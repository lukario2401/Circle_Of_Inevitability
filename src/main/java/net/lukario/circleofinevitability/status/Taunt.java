package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Taunt: a mob is made to go after one entity you choose, and nothing else, for a while. Works from any
 * class/ability:
 *
 *   Taunt.apply(mob, player, 200);           // 10 seconds: it attacks the caster (a provocation, a decoy)
 *   Taunt.apply(mob, otherMob, 200);         // ...or another mob: they fight each other
 *   Taunt.brawl(mobs, 200, random);          // everyone in the list goes after a random other one of them
 *   Taunt.isTaunted(entity)   Taunt.targetOf(entity)   Taunt.ticksLeft(entity)   Taunt.remove(mob)
 *
 * Only mobs can be taunted (a player has no AI to steer); apply returns false for anything else, and for
 * marionettes, which only fight what their master hands them.
 *
 * While it lasts the mob ignores every other target it would pick, and is pointed at its target again every
 * REAIM_EVERY ticks (also mobs that keep their target in a brain, like piglins). It ends early when its target
 * dies, leaves the dimension or gets further away than GIVE_UP_DISTANCE. A mob that is blind (status/Blind)
 * is left alone until it sees again, and nothing is ever made to go after someone concealed. Monsters and
 * neutral mobs know how to fight what they target; a cow that is taunted only looks at it.
 *
 * Saved with the entity.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Taunt {

    /** Ticks between the moments a taunted mob is pointed at its target again. */
    public static int REAIM_EVERY = 10;
    /** A target further away than this (blocks) ends the taunt. */
    public static double GIVE_UP_DISTANCE = 48.0;

    private static final String TAG = "circle_of_inevitability_taunt";   // {Target: uuid, Until: game time}
    private static final String TARGET = "Target";
    private static final String UNTIL = "Until";

    private Taunt() {}

    // =========================
    // API
    // =========================

    /**
     * Makes 'victim' go after 'at' for 'ticks' ticks. A new taunt replaces the one it had. False (and nothing
     * happens) if it cannot be taunted: not a mob, a marionette, or 'at' is itself, dead or concealed.
     */
    public static boolean apply(LivingEntity victim, LivingEntity at, int ticks) {
        if (victim.level().isClientSide() || ticks <= 0 || at == null || at == victim) return false;
        if (!(victim instanceof Mob mob) || !mob.isAlive() || Marionette.isMarionette(mob)) return false;
        if (!at.isAlive() || at.level() != mob.level() || Concealment.isHidden(at)) return false;

        CompoundTag tag = new CompoundTag();
        tag.store(TARGET, UUIDUtil.CODEC, at.getUUID());
        tag.putLong(UNTIL, mob.level().getGameTime() + ticks);
        mob.getPersistentData().put(TAG, tag);
        aim(mob, at);
        return true;
    }

    /**
     * Sets a group against itself: every mob in it is taunted at a random OTHER member, for 'ticks' ticks.
     * Returns how many were taunted (0 if there are fewer than two that can be).
     */
    public static int brawl(List<? extends LivingEntity> group, int ticks, RandomSource random) {
        List<LivingEntity> fighters = new ArrayList<>();
        for (LivingEntity entity : group) {
            if (entity instanceof Mob mob && mob.isAlive() && !Marionette.isMarionette(mob)) fighters.add(entity);
        }
        if (fighters.size() < 2) return 0;
        int taunted = 0;
        for (int i = 0; i < fighters.size(); i++) {
            int pick = random.nextInt(fighters.size() - 1);
            if (pick >= i) pick++;                                    // anyone but itself
            if (apply(fighters.get(i), fighters.get(pick), ticks)) taunted++;
        }
        return taunted;
    }

    public static boolean isTaunted(Entity entity) {
        return ticksLeft(entity) > 0;
    }

    /** Who it is made to go after right now, or null (not taunted, or that one is gone). */
    public static LivingEntity targetOf(Entity entity) {
        if (!isTaunted(entity) || !(entity.level() instanceof ServerLevel sl)) return null;
        CompoundTag tag = entity.getPersistentData().getCompoundOrEmpty(TAG);
        if (!tag.read(TARGET, UUIDUtil.CODEC).isPresent()) return null;
        UUID id = tag.read(TARGET, UUIDUtil.CODEC).orElseThrow();
        return sl.getEntity(id) instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    public static int ticksLeft(Entity entity) {
        if (entity == null || !entity.getPersistentData().contains(TAG)) return 0;
        long until = entity.getPersistentData().getCompoundOrEmpty(TAG).getLongOr(UNTIL, 0L);
        return (int) Math.max(0, until - entity.level().getGameTime());
    }

    /** Ends the taunt. The mob keeps fighting what it fights now, but may choose again from here on. */
    public static void remove(LivingEntity entity) {
        entity.getPersistentData().remove(TAG);
    }

    // =========================
    // Internals
    // =========================

    /** Can it still be made to go after this target? */
    private static boolean stillValid(Mob mob, LivingEntity target) {
        if (target == null || !target.isAlive() || target.level() != mob.level() || target.isSpectator()) return false;
        if (target instanceof Player player && player.getAbilities().invulnerable) return false;   // creative
        return mob.distanceToSqr(target) <= GIVE_UP_DISTANCE * GIVE_UP_DISTANCE;
    }

    /** Points the mob at its target, also for mobs that keep their target in a brain (as Marionette does). */
    private static void aim(Mob mob, LivingEntity target) {
        if (mob.getTarget() != target) mob.setTarget(target);
        mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onTauntLivingTick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || mob.level().isClientSide()) return;
        if (!mob.getPersistentData().contains(TAG)) return;
        if (!isTaunted(mob)) {
            remove(mob);
            return;
        }
        if (mob.tickCount % REAIM_EVERY != 0) return;
        LivingEntity target = targetOf(mob);
        if (!stillValid(mob, target)) {
            remove(mob);
            return;
        }
        if (Blind.isBlind(mob) || Concealment.isHidden(target)) return;   // those win for as long as they last
        aim(mob, target);
    }

    // While taunted, a mob may not pick anyone else. (Letting go of a target, null, is allowed: blindness and
    // concealment do that; the next re-aim brings the taunt back once they are over.)
    @SubscribeEvent
    public static boolean onTauntTarget(LivingChangeTargetEvent event) {
        LivingEntity mob = event.getEntity();
        LivingEntity wanted = event.getNewTarget();
        if (wanted == null || mob.level().isClientSide() || !isTaunted(mob)) return false;
        LivingEntity target = targetOf(mob);
        if (!(mob instanceof Mob m) || !stillValid(m, target)) return false;
        return wanted != target;
    }
}
