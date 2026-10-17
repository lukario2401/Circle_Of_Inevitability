package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.DamageFx;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Bleeding: an open wound. The entity loses health once a second for a while, and the damage is counted as the
 * one who opened it (DamageFx.credit). Works from any class/ability, on players and mobs:
 *
 *   Bleeding.apply(target, player, 80, 2f);          // 4 seconds, 2 damage a second
 *   Bleeding.isBleeding(entity)    Bleeding.ticksLeft(entity)    Bleeding.perSecond(entity)    Bleeding.remove(target)
 *   Bleeding.addImmunity(entity -> ...)              // some cannot bleed (a body that feels no pain, a golem)
 *   Bleeding.addListener((victim, amount, source) -> ...)   // the look and sound of each drip
 *
 * Applying again makes it last 'ticks' from now if that is longer, and keeps the bigger of the two numbers, so a
 * weak cut never softens a deep one. The newest source gets the credit. It ends when the target dies.
 * Magic damage: armor does not soften it. Saved with the entity.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Bleeding {

    /** Runs for every drip. 'source' = who opened the wound (may be null: gone, or nobody). */
    @FunctionalInterface
    public interface Listener {
        void dripped(LivingEntity victim, float amount, Player source);
    }

    /** Ticks between two drips. */
    public static int EVERY = 20;

    private static final String TAG = "circle_of_inevitability_bleeding";
    private static final String UNTIL = "until";               // game time
    private static final String PER_SECOND = "per_second";
    private static final String SOURCE = "source";

    private static final List<Predicate<LivingEntity>> IMMUNE = new ArrayList<>();
    private static final List<Listener> LISTENERS = new ArrayList<>();

    private Bleeding() {}

    // =========================
    // API
    // =========================

    /** Bleeds 'perSecond' a second for 'ticks'. False if it cannot bleed (immune, dead, creative). */
    public static boolean apply(LivingEntity target, Player source, int ticks, float perSecond) {
        if (target.level().isClientSide() || ticks <= 0 || perSecond <= 0f || !target.isAlive()) return false;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
        if (isImmune(target)) return false;
        long now = target.level().getGameTime();
        CompoundTag tag = target.getPersistentData().getCompoundOrEmpty(TAG);
        boolean still = tag.getLongOr(UNTIL, 0L) > now;
        tag.putLong(UNTIL, Math.max(now + ticks, still ? tag.getLongOr(UNTIL, 0L) : 0L));
        tag.putFloat(PER_SECOND, Math.max(perSecond, still ? tag.getFloatOr(PER_SECOND, 0f) : 0f));
        if (source != null) tag.store(SOURCE, UUIDUtil.CODEC, source.getUUID());
        target.getPersistentData().put(TAG, tag);
        return true;
    }

    public static boolean isBleeding(Entity entity) {
        return ticksLeft(entity) > 0;
    }

    public static int ticksLeft(Entity entity) {
        if (entity == null || !entity.getPersistentData().contains(TAG)) return 0;
        long until = entity.getPersistentData().getCompoundOrEmpty(TAG).getLongOr(UNTIL, 0L);
        return (int) Math.max(0, until - entity.level().getGameTime());
    }

    /** Damage a second right now (0 when it does not bleed). */
    public static float perSecond(Entity entity) {
        if (!isBleeding(entity)) return 0f;
        return entity.getPersistentData().getCompoundOrEmpty(TAG).getFloatOr(PER_SECOND, 0f);
    }

    public static void remove(LivingEntity target) {
        target.getPersistentData().remove(TAG);
    }

    public static boolean isImmune(LivingEntity entity) {
        for (Predicate<LivingEntity> immune : IMMUNE) if (immune.test(entity)) return true;
        return false;
    }

    public static void addImmunity(Predicate<LivingEntity> immune) {
        IMMUNE.add(immune);
    }

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onBleedingLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel sl) || !entity.getPersistentData().contains(TAG)) return;
        if (!isBleeding(entity) || isImmune(entity)) {               // its time is up, or it became immune meanwhile
            remove(entity);
            return;
        }
        if (entity.tickCount % EVERY != 0) return;
        CompoundTag tag = entity.getPersistentData().getCompoundOrEmpty(TAG);
        float amount = tag.getFloatOr(PER_SECOND, 0f) * EVERY / 20f;
        UUID id = tag.read(SOURCE, UUIDUtil.CODEC).orElse(null);
        Player source = id == null ? null : sl.getPlayerByUUID(id);
        if (source != null) DamageFx.credit(source, entity);
        entity.invulnerableTime = 0;
        entity.hurt(sl.damageSources().magic(), amount);
        for (Listener listener : LISTENERS) {
            try {
                listener.dripped(entity, amount, source);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A bleeding listener failed", e);
            }
        }
    }

    @SubscribeEvent
    public static void onBleedingDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) remove(event.getEntity());
    }
}
