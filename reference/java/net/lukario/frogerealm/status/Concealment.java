package net.lukario.frogerealm.status;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.network.CConcealPacket;
import net.lukario.frogerealm.network.PacketHandler;
import net.lukario.frogerealm.particles.fx.ParticleFx;
import net.lukario.frogerealm.particles.fx.ParticleShapes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Concealment: an entity steps out of the world for a while. Works from any class/ability, on players and mobs:
 *
 *   Concealment.hide(player, 80);       // 4 seconds
 *   Concealment.isHidden(entity)
 *   Concealment.ticksLeft(entity)
 *   Concealment.reveal(player);         // step back out early
 *
 * While hidden:
 *   - nobody sees it: not its body, armor, held items or name, nor the 3D models stuck to it
 *     (the players' games simply don't draw it)
 *   - nothing can hurt it (except the void and /kill)
 *   - mobs forget it and cannot pick it as a target; spells that use Spells.isEnemy pass over it
 *   - it can still walk around. A hidden player who attacks something steps back out (BREAKS_ON_ATTACK);
 *     abilities should call Concealment.reveal(player) when they are cast, if casting should end it.
 *
 * Saved with the entity, so someone hidden still comes back properly after a reload.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Concealment {

    /** A puff of fog where it vanishes and where it returns. Set to false to handle visuals yourself. */
    public static boolean SHOW_PARTICLES = true;
    /** A hidden player who hits something with a normal attack is revealed. */
    public static boolean BREAKS_ON_ATTACK = true;

    private static final String TAG = "forgerealm_concealed";
    private static final String TICKS = "Ticks";
    private static final String HAD_INVISIBILITY = "HadInvisibility";
    private static final double FORGET_RANGE = 48.0;    // mobs this close that were hunting it lose track of it
    private static final int REFRESH = 10;              // ticks between reminders to mobs and to the players' games

    private static final ParticleFx FOG = ParticleFx.of("fx/smoke")
            .color(0xB0C8CCD4).endColor(0x00888C98)
            .size(0.45f).endSize(1.1f).sizeRandom(0.35f)
            .lifetime(26, 14).gravity(-0.01f).friction(0.9f)
            .spin(2f).randomRotation();

    private Concealment() {}

    // =========================
    // API
    // =========================

    /** Hides the entity for 'ticks' ticks. Hiding something already hidden only makes it last longer. */
    public static void hide(LivingEntity entity, int ticks) {
        if (ticks <= 0 || !entity.isAlive() || !(entity.level() instanceof ServerLevel sl)) return;

        CompoundTag tag = concealTag(entity);
        if (tag == null) {
            tag = new CompoundTag();
            tag.putBoolean(HAD_INVISIBILITY, entity.hasEffect(MobEffects.INVISIBILITY));
            entity.getPersistentData().put(TAG, tag);
            fog(sl, entity);
        } else if (ticks <= tag.getInt(TICKS)) {
            return; // already hidden for at least that long
        }
        tag.putInt(TICKS, ticks);
        keepHidden(entity, sl, ticks);
    }

    public static boolean isHidden(Entity entity) {
        return entity != null && entity.getPersistentData().contains(TAG);
    }

    public static int ticksLeft(Entity entity) {
        CompoundTag tag = entity == null ? null : concealTag(entity);
        return tag == null ? 0 : tag.getInt(TICKS);
    }

    /** Brings it back now. Does nothing if it is not hidden. */
    public static void reveal(LivingEntity entity) {
        CompoundTag tag = concealTag(entity);
        if (tag == null) return;
        entity.getPersistentData().remove(TAG);
        if (!tag.getBoolean(HAD_INVISIBILITY)) entity.removeEffect(MobEffects.INVISIBILITY);
        if (entity.level() instanceof ServerLevel sl) {
            PacketHandler.sendToAllClients(new CConcealPacket(entity.getId(), 0));
            if (entity.isAlive()) fog(sl, entity);
        }
    }

    // =========================
    // Internals
    // =========================

    private static CompoundTag concealTag(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.contains(TAG) ? data.getCompound(TAG) : null;
    }

    /** Everything that has to be repeated now and then: the invisibility, the mobs' memory, the players' games. */
    private static void keepHidden(LivingEntity entity, ServerLevel sl, int ticksLeft) {
        // vanilla invisibility too: no shadow, no particles, and mobs notice it from much closer only
        entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ticksLeft + REFRESH, 0, false, false));

        for (Mob mob : sl.getEntitiesOfClass(Mob.class, entity.getBoundingBox().inflate(FORGET_RANGE),
                hunter -> hunter.getTarget() == entity)) {
            mob.setTarget(null);
            // mobs with a brain (piglins, wardens...) remember their target there
            if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
                mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            }
        }
        // told again every REFRESH ticks, so players who only just came near stop drawing it too
        PacketHandler.sendToAllClients(new CConcealPacket(entity.getId(), ticksLeft));
    }

    private static void fog(ServerLevel sl, LivingEntity entity) {
        if (!SHOW_PARTICLES) return;
        ParticleShapes.burst(sl, FOG, entity.getBoundingBox().getCenter(), 22, 0.02, 0.12);
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onConcealmentLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel sl)) return;
        CompoundTag tag = concealTag(entity);
        if (tag == null) return;

        int left = tag.getInt(TICKS) - 1;
        if (left <= 0) {
            reveal(entity);
            return;
        }
        tag.putInt(TICKS, left);
        if (left % REFRESH == 0) keepHidden(entity, sl, left);
    }

    // HIGH: runs before Substitute and the like, so a hit on someone hidden never uses those up
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onConcealmentAttacked(LivingAttackEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !isHidden(victim)) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;   // the void and /kill
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onConcealmentTargeted(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        if (target != null && isHidden(target)) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onConcealmentPlayerAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (BREAKS_ON_ATTACK && !player.level().isClientSide() && isHidden(player)) reveal(player);
    }

    @SubscribeEvent
    public static void onConcealmentDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide() && isHidden(entity)) reveal(entity);
    }
}
