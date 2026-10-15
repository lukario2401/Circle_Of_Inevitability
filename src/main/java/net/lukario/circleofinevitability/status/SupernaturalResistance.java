package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.root.Freeze;
import net.lukario.circleofinevitability.root.Root;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Supernatural resistance: for a while the supernatural has less hold on an entity. Damage that is supernatural
 * (magic, wither, explosions, lightning, freezing, sonic booms, thorns: see isSupernatural) is cut, and blindness
 * and nausea cannot be put on it. A grant that "shakes off" also ends roots, freezing, Blind and taunts at once,
 * every tick, and darkness with them. Works on mobs too. Several grants can be on one entity under different
 * names (an always-on passive and a stronger cast): the biggest cut counts, and any grant that shakes off does.
 *
 *   SupernaturalResistance.grant(player, "my_pathway_resist", 160, 0.4f, true, shaken -> ...);   // 8 seconds, 40% less
 *   SupernaturalResistance.grant(player, "my_pathway_innate", 40, 0.25f, false, null);           // a passive, renewed each second
 *
 *   SupernaturalResistance.has(entity)     cut(entity)     ticksLeft(entity, name)     end(entity, name)
 *
 * The last part (may be null) runs when something was shaken off: a flash, a sound.
 * Not saved: gone on death, logout and after a restart (a passive renews itself, so that does not matter).
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SupernaturalResistance {

    /** The potion effects that never take while any grant lasts. */
    public static final List<Holder<MobEffect>> NEVER_TAKE = List.of(MobEffects.BLINDNESS, MobEffects.NAUSEA);
    /** And these only while a grant that shakes off lasts. */
    public static final List<Holder<MobEffect>> SHAKEN_OFF = List.of(MobEffects.DARKNESS);

    private record Grant(long expires, float cut, boolean shakesOff, Consumer<LivingEntity> onShrug) {}

    private static final Map<UUID, Map<String, Grant>> GRANTS = new HashMap<>();

    private SupernaturalResistance() {}

    // =========================
    // API
    // =========================

    /**
     * For 'ticks', supernatural damage is cut by 'cut' (0.4 = 40% less). shakesOff: roots, freezing, Blind,
     * taunts and darkness also end at once. onShrug (may be null) runs each time something was shaken off.
     * Granting the same name again replaces it.
     */
    public static void grant(LivingEntity entity, String name, int ticks, float cut, boolean shakesOff,
                             Consumer<LivingEntity> onShrug) {
        if (!(entity.level() instanceof ServerLevel sl) || ticks <= 0) return;
        Map<String, Grant> mine = GRANTS.computeIfAbsent(entity.getUUID(), id -> new HashMap<>());
        mine.put(name, new Grant(sl.getGameTime() + ticks, Math.max(0f, Math.min(1f, cut)), shakesOff, onShrug));
        for (Holder<MobEffect> effect : NEVER_TAKE) entity.removeEffect(effect);       // what is on it already ends too
    }

    public static boolean has(Entity entity) {
        return entity != null && !live(entity).isEmpty();
    }

    /** How much supernatural damage is cut right now (0 = none). */
    public static float cut(Entity entity) {
        float best = 0f;
        for (Grant grant : live(entity).values()) best = Math.max(best, grant.cut());
        return best;
    }

    public static int ticksLeft(Entity entity, String name) {
        Grant grant = live(entity).get(name);
        return grant == null ? 0 : (int) (grant.expires() - entity.level().getGameTime());
    }

    public static void end(Entity entity, String name) {
        Map<String, Grant> mine = GRANTS.get(entity.getUUID());
        if (mine == null) return;
        mine.remove(name);
        if (mine.isEmpty()) GRANTS.remove(entity.getUUID());
    }

    /**
     * Supernatural damage: magic (also what abilities deal with no attacker), wither, explosions, lightning,
     * freezing, sonic booms, thorns and dragon's breath. Melee, arrows, falling, fire and drowning are not.
     */
    public static boolean isSupernatural(DamageSource source) {
        return source.is(DamageTypeTags.WITCH_RESISTANT_TO)               // magic, indirect magic, sonic boom, thorns
                || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypeTags.IS_LIGHTNING)
                || source.is(DamageTypeTags.IS_FREEZING)
                || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.DRAGON_BREATH);
    }

    /** The grants that have not run out (forgets the ones that have). */
    private static Map<String, Grant> live(Entity entity) {
        Map<String, Grant> mine = GRANTS.get(entity.getUUID());
        if (mine == null) return Map.of();
        long now = entity.level().getGameTime();
        mine.values().removeIf(grant -> grant.expires() < now);
        if (mine.isEmpty()) {
            GRANTS.remove(entity.getUUID());
            return Map.of();
        }
        return mine;
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onSupernaturalResistanceHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !GRANTS.containsKey(entity.getUUID())) return;
        if (!isSupernatural(event.getSource()) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        float cut = cut(entity);
        if (cut > 0f) event.setAmount(event.getAmount() * (1f - cut));
    }

    @SubscribeEvent
    public static void onSupernaturalResistanceEffect(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !GRANTS.containsKey(entity.getUUID())) return;
        Map<String, Grant> mine = live(entity);
        if (mine.isEmpty()) return;
        Holder<MobEffect> effect = event.getEffectInstance().getEffect();
        boolean shakesOff = mine.values().stream().anyMatch(Grant::shakesOff);
        if (NEVER_TAKE.contains(effect) || (shakesOff && SHAKEN_OFF.contains(effect))) event.setResult(Result.DENY);
    }

    @SubscribeEvent
    public static void onSupernaturalResistanceTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !GRANTS.containsKey(entity.getUUID())) return;
        Map<String, Grant> mine = live(entity);
        Grant shaker = null;
        for (Grant grant : mine.values()) if (grant.shakesOff()) shaker = grant;
        if (shaker == null) return;

        boolean shook = false;
        if (Root.isRooted(entity)) { Root.remove(entity); shook = true; }
        if (Freeze.isFrozen(entity)) { Freeze.thaw(entity); shook = true; }
        if (Blind.isBlind(entity)) { Blind.remove(entity); shook = true; }
        if (Taunt.isTaunted(entity)) { Taunt.remove(entity); shook = true; }
        for (Holder<MobEffect> effect : SHAKEN_OFF) {
            if (entity.hasEffect(effect)) { entity.removeEffect(effect); shook = true; }
        }
        if (shook && shaker.onShrug() != null) {
            try {
                shaker.onShrug().accept(entity);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A supernatural resistance's shrug failed", e);
            }
        }
    }

    @SubscribeEvent
    public static void onSupernaturalResistanceDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) GRANTS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onSupernaturalResistanceLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        GRANTS.remove(event.getEntity().getUUID());
    }
}
