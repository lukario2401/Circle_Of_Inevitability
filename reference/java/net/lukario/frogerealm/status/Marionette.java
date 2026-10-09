package net.lukario.frogerealm.status;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.particles.fx.ModelFx;
import net.lukario.frogerealm.particles.fx.ParticleFx;
import net.lukario.frogerealm.particles.fx.ParticleShapes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Marionette: a mob whose strings a player holds. It stops hunting its master and fights for them instead.
 * Works from any class/ability:
 *
 *   Marionette.control(mob, player, 600);       // 30 seconds. false if it could not be taken
 *   Marionette.isMarionette(entity)
 *   Marionette.isMarionetteOf(entity, player)
 *   Marionette.of(player)                       // the marionettes that player holds right now
 *   Marionette.ticksLeft(entity)
 *   Marionette.release(mob)                     // lets go: it is its old self again
 *   Marionette.collapse(mob)                    // cuts the strings the way running out of time does
 *
 * What a marionette does:
 *   - it attacks, in this order: what its master last hit, what last hit its master, the fight it is already in,
 *     what last hit the marionette itself, the nearest monster. It never picks a target of its own.
 *   - with nothing to fight it walks back to its master
 *   - it can never target or hurt its master or the master's other marionettes
 *   - spells that use Spells.isEnemy leave the caster's own marionettes alone
 *   - mobs that cannot fight (a cow, a villager) are walked up to the enemy and made to hit it
 * It ends when the time is up, when the puppet dies, or when its master dies, logs out or leaves the dimension.
 * When the time runs out the puppet drops dead (DIES_WHEN_TIME_RUNS_OUT); in the other cases it is just let go.
 *
 * Looks: a wooden control bar turning above it with strings down to its body (models/model_fx/marionette_cross).
 * Saved with the mob, so a marionette is still one after a reload (the model is not shown again, though).
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Marionette {

    /** A marionette whose time runs out falls dead. false = it simply becomes itself again. */
    public static boolean DIES_WHEN_TIME_RUNS_OUT = true;
    /** How many marionettes one master can hold. Taking another lets go of the one with the least time left. */
    public static int MAX_PER_MASTER = 3;

    private static final String TAG = "forgerealm_marionette";
    private static final String MASTER = "Master";
    private static final String TICKS = "Ticks";
    private static final String STRIKE = "Strike";          // ticks until a mob that cannot fight may be made to hit again
    /** The control bar carries this tag, so cutting the strings removes it and nothing else stuck to the mob. */
    private static final String MODEL_TAG = "marionette";

    private static final int THINK = 10;                    // ticks between decisions
    private static final double SEEK_RANGE = 16.0;          // how far from itself it looks for something to fight
    private static final double FOLLOW_DISTANCE = 6.0;      // idle and further from its master than this: it walks back
    private static final float PUPPET_STRIKE_DAMAGE = 4f;   // for mobs that cannot fight on their own
    private static final int PUPPET_STRIKE_COOLDOWN = 20;

    // The model is 28 px (1.75 blocks) from the ends of the strings (its pivot) up to the bar.
    private static final float CROSS_HEIGHT = 28f / 16f;
    private static final ModelFx CONTROL_CROSS = ModelFx.of("marionette_cross")
            .glow().aura(0x50E8D8FF, 0.04f, 2).fade(4, 8).spin(1.5f).tag(MODEL_TAG)
            .key(0, ModelFx.pose().up(1.2f).alpha(0f))
            .key(8, ModelFx.pose().up(0f).alpha(1f), ModelFx.Ease.OUT_BACK);      // drops into place

    private static final ParticleFx STRING_SNAP = ParticleFx.of("fx/sliver")
            .color(0xFFE8E0F8).endColor(0x00786890)
            .size(0.14f).endSize(0.05f).sizeRandom(0.4f)
            .lifetime(14, 8).gravity(0.25f).friction(0.9f)
            .glow().randomRotation();

    private static final Map<UUID, Set<UUID>> HELD = new HashMap<>();   // master -> its marionettes
    private static boolean aiming;                                      // true while WE are setting a marionette's target

    private Marionette() {}

    // =========================
    // API
    // =========================

    /**
     * Hands the mob's strings to the master for 'ticks' ticks.
     * @return false if it cannot be taken: dead, in another dimension, or another player's marionette
     */
    public static boolean control(Mob mob, Player master, int ticks) {
        if (ticks <= 0 || !mob.isAlive() || !(mob.level() instanceof ServerLevel sl) || master.level() != sl) return false;
        UUID holder = masterId(mob);
        if (holder != null && !holder.equals(master.getUUID())) return false;

        if (holder == null) {
            List<Mob> held = of(master);
            while (held.size() >= Math.max(1, MAX_PER_MASTER)) {        // make room: the one closest to its end goes
                Mob oldest = held.get(0);
                for (Mob other : held) {
                    if (ticksLeft(other) < ticksLeft(oldest)) oldest = other;
                }
                release(oldest);
                held.remove(oldest);
            }
        }

        CompoundTag tag = new CompoundTag();
        tag.putUUID(MASTER, master.getUUID());
        tag.putInt(TICKS, ticks);
        mob.getPersistentData().put(TAG, tag);
        HELD.computeIfAbsent(master.getUUID(), id -> new HashSet<>()).add(mob.getUUID());

        aim(mob, null);                                     // it forgets whoever it was hunting (often its new master)
        ParticleShapes.clearModels(sl, mob, MODEL_TAG);     // taken again: one control bar, not two
        hangStrings(sl, mob, ticks);
        return true;
    }

    public static boolean isMarionette(Entity entity) {
        return entity != null && entity.getPersistentData().contains(TAG);
    }

    public static boolean isMarionetteOf(Entity entity, Entity master) {
        UUID holder = masterId(entity);
        return holder != null && master != null && holder.equals(master.getUUID());
    }

    /** The UUID of the player holding this entity's strings, or null if it is not a marionette. */
    public static UUID masterId(Entity entity) {
        CompoundTag tag = entity == null ? null : marionetteTag(entity);
        return tag == null || !tag.hasUUID(MASTER) ? null : tag.getUUID(MASTER);
    }

    public static int ticksLeft(Entity entity) {
        CompoundTag tag = entity == null ? null : marionetteTag(entity);
        return tag == null ? 0 : tag.getInt(TICKS);
    }

    /** The marionettes this player holds that are alive and loaded in the player's dimension. */
    public static List<Mob> of(Player master) {
        List<Mob> result = new ArrayList<>();
        Set<UUID> ids = HELD.get(master.getUUID());
        if (ids == null || !(master.level() instanceof ServerLevel sl)) return result;
        for (UUID id : ids) {
            if (sl.getEntity(id) instanceof Mob mob && mob.isAlive() && isMarionetteOf(mob, master)) result.add(mob);
        }
        return result;
    }

    /** Lets go of it: it becomes its old self again. */
    public static void release(Mob mob) {
        end(mob, false);
    }

    /** Cuts the strings the way running out of time does (it drops dead if DIES_WHEN_TIME_RUNS_OUT). */
    public static void collapse(Mob mob) {
        end(mob, DIES_WHEN_TIME_RUNS_OUT);
    }

    // =========================
    // Internals
    // =========================

    private static CompoundTag marionetteTag(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.contains(TAG) ? data.getCompound(TAG) : null;
    }

    /** The control bar above the mob, with its strings reaching down to the middle of the body. */
    private static void hangStrings(ServerLevel sl, Mob mob, int ticks) {
        float width = Mth.clamp(mob.getBbWidth() * 1.5f, 0.9f, 3.2f);
        double waist = mob.getBbHeight() * 0.55;
        float height = (float) ((mob.getBbHeight() - waist + 0.9) / CROSS_HEIGHT);   // the bar hangs 0.9 blocks above the head
        ModelFx cross = CONTROL_CROSS.scale(width, height, width).lifetime(ticks);
        ParticleShapes.modelOn(sl, cross, mob, new Vec3(0, waist, 0), mob.yBodyRot, 0f, 0f);
        sl.playSound(null, mob.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.9f, 1.6f);
    }

    private static void end(Mob mob, boolean dies) {
        CompoundTag tag = marionetteTag(mob);
        if (tag == null) return;
        UUID holder = tag.hasUUID(MASTER) ? tag.getUUID(MASTER) : null;
        mob.getPersistentData().remove(TAG);
        if (holder != null) {
            Set<UUID> ids = HELD.get(holder);
            if (ids != null) {
                ids.remove(mob.getUUID());
                if (ids.isEmpty()) HELD.remove(holder);
            }
        }
        if (!(mob.level() instanceof ServerLevel sl)) return;
        ParticleShapes.clearModels(sl, mob, MODEL_TAG);
        if (!mob.isAlive()) return;

        aim(mob, null);
        ParticleShapes.burst(sl, STRING_SNAP, mob.position().add(0, mob.getBbHeight() + 0.6, 0), 12, 0.03, 0.14);
        sl.playSound(null, mob.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.8f, 0.6f);
        if (dies) {
            Player master = holder == null ? null : sl.getPlayerByUUID(holder);
            // the master gets the kill (and the drops) if they are still around
            if (master != null) mob.hurt(sl.damageSources().indirectMagic(master, master), mob.getMaxHealth() * 100f);
            if (mob.isAlive()) mob.kill();
        }
    }

    /** Points the mob at a target (or at nothing), also for mobs that keep their target in a brain. */
    private static void aim(Mob mob, LivingEntity target) {
        if (mob.getTarget() != target) {
            aiming = true;                  // tells onMarionetteTarget that this choice is ours, not the mob's
            try {
                mob.setTarget(target);
            } finally {
                aiming = false;
            }
        }
        if (target != null) {
            mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        } else if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
    }

    private static boolean canFight(Mob mob, Player master, LivingEntity target) {
        if (target == null || target == mob || target == master || !target.isAlive() || target.isSpectator()) return false;
        if (target.level() != mob.level() || isMarionetteOf(target, master) || Concealment.isHidden(target)) return false;
        if (target instanceof Player player && player.getAbilities().invulnerable) return false;   // creative
        if (master.isAlliedTo(target)) return false;                                               // same team
        return mob.distanceToSqr(target) <= (SEEK_RANGE * 1.5) * (SEEK_RANGE * 1.5);
    }

    private static LivingEntity pickTarget(Mob mob, Player master, ServerLevel sl) {
        LivingEntity struck = master.getLastHurtMob();          // what its master hit last
        if (canFight(mob, master, struck)) return struck;
        LivingEntity attacker = master.getLastHurtByMob();      // what hit its master last
        if (canFight(mob, master, attacker)) return attacker;
        LivingEntity current = mob.getTarget();                 // else it finishes the fight it is in
        if (canFight(mob, master, current)) return current;
        LivingEntity ownAttacker = mob.getLastHurtByMob();      // else whatever hit the marionette itself
        if (canFight(mob, master, ownAttacker)) return ownAttacker;

        LivingEntity nearest = null;                            // else the nearest monster
        double best = SEEK_RANGE * SEEK_RANGE;
        for (LivingEntity other : sl.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(SEEK_RANGE),
                candidate -> candidate instanceof Enemy)) {
            double distance = mob.distanceToSqr(other);
            if (distance <= best && canFight(mob, master, other)) {
                best = distance;
                nearest = other;
            }
        }
        return nearest;
    }

    private static void think(Mob mob, Player master, ServerLevel sl, CompoundTag tag) {
        LivingEntity target = pickTarget(mob, master, sl);
        aim(mob, target);

        if (target == null) {
            if (mob.distanceToSqr(master) > FOLLOW_DISTANCE * FOLLOW_DISTANCE) mob.getNavigation().moveTo(master, 1.2);
            return;
        }
        // Monsters and neutral mobs (wolves, golems...) know how to fight whatever they target.
        // Everything else has to be walked there and made to hit.
        if (mob instanceof Enemy || mob instanceof NeutralMob) return;
        double reach = (mob.getBbWidth() + target.getBbWidth()) * 0.5 + 1.3;
        if (mob.distanceToSqr(target) > reach * reach) {
            mob.getNavigation().moveTo(target, 1.3);
            return;
        }
        mob.getLookControl().setLookAt(target, 30f, 30f);
        if (tag.getInt(STRIKE) > 0) return;
        tag.putInt(STRIKE, PUPPET_STRIKE_COOLDOWN);
        mob.swing(InteractionHand.MAIN_HAND);
        target.hurt(sl.damageSources().mobAttack(mob), PUPPET_STRIKE_DAMAGE);
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onMarionetteLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel sl)) return;
        CompoundTag tag = marionetteTag(mob);
        if (tag == null) return;
        if (!tag.hasUUID(MASTER)) {
            end(mob, false);
            return;
        }
        UUID holder = tag.getUUID(MASTER);
        HELD.computeIfAbsent(holder, id -> new HashSet<>()).add(mob.getUUID());   // (again after a reload)

        int left = tag.getInt(TICKS) - 1;
        if (left <= 0) {
            end(mob, DIES_WHEN_TIME_RUNS_OUT);
            return;
        }
        Player master = sl.getPlayerByUUID(holder);         // null when the master is gone or in another dimension
        if (master == null || !master.isAlive()) {
            end(mob, false);
            return;
        }
        tag.putInt(TICKS, left);
        int strike = tag.getInt(STRIKE);
        if (strike > 0) tag.putInt(STRIKE, strike - 1);
        if (mob.tickCount % THINK == 0) think(mob, master, sl, tag);
    }

    // A marionette does not choose who it fights: only the targets we hand it (in aim) go through.
    // That also means it can never turn on its master or on a fellow marionette.
    @SubscribeEvent
    public static void onMarionetteTarget(LivingChangeTargetEvent event) {
        if (aiming || event.getNewTarget() == null) return;
        if (isMarionette(event.getEntity())) event.setCanceled(true);
    }

    // ...and cannot hurt them by accident either (a stray arrow, a creeper marionette going off next to its master).
    @SubscribeEvent
    public static void onMarionetteHurtsFriend(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        UUID holder = masterId(event.getSource().getEntity());
        if (holder == null) return;
        if (holder.equals(victim.getUUID()) || holder.equals(masterId(victim))) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMarionetteDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Mob mob && !mob.level().isClientSide() && isMarionette(mob)) end(mob, false);
    }
}
