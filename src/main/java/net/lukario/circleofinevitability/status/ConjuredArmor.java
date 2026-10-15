package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Conjured armor: armor that is not an item. Plates of light, a coat of liquid silver, a shell of bone. It weighs
 * nothing and cannot be taken off or dropped. Works from any class/ability, on players and mobs:
 *
 *   ConjuredArmor.of("my_class_bone_shell")
 *           .lasts(900)                        // ticks; 0 = until ConjuredArmor.takeOff(...)   (default 0)
 *           .cut(0.4f)                         // 40% less damage from everything            (optional)
 *           .absorption(8f)                    // a layer of absorption health on top        (optional)
 *           .regrow(40)                        // one point of that layer back every 40 ticks without a hit (optional)
 *           .steady()                          // no knockback, and slowness does not take    (optional)
 *           .onHit((wearer, amount) -> ...)    // a blow landed on it ('amount' after the cut) (optional)
 *           .onEnd(wearer -> ...)              // it came off: time, takeOff, death, logout   (optional)
 *           .wear(entity);
 *
 *   ConjuredArmor.isWorn(entity, "my_class_bone_shell")     ticksLeft(...)     takeOff(entity, "my_class_bone_shell")
 *
 * The look is yours (a model on the body, see ParticleShapes.modelOnTurning). The name keeps different armors apart
 * (start it with your class): wearing one again under the same name replaces it (the old one's onEnd does not run).
 * An entity can wear several at once: the biggest cut counts, and the layers add up.
 * Not saved: gone after a server restart, on death and on logout.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ConjuredArmor {

    private static final Identifier LAYER_ID = Physique.id("conjured_armor_layer");
    private static final Identifier STEADY_ID = Physique.id("conjured_armor_steady");

    private final String name;
    private int lasts;
    private float cut;
    private float absorption;
    private int regrow;
    private boolean steady;
    private BiConsumer<LivingEntity, Float> onHit;
    private Consumer<LivingEntity> onEnd;

    private static final class Worn {
        final ConjuredArmor armor;
        final long until;                          // Long.MAX_VALUE = until taken off
        long lastHit;

        Worn(ConjuredArmor armor, long until, long now) {
            this.armor = armor;
            this.until = until;
            this.lastHit = now;
        }
    }

    private static final Map<UUID, Map<String, Worn>> WORN = new HashMap<>();

    private ConjuredArmor(String name) {
        this.name = name;
    }

    public static ConjuredArmor of(String name) {
        return new ConjuredArmor(name);
    }

    public ConjuredArmor lasts(int ticks) { this.lasts = Math.max(0, ticks); return this; }
    public ConjuredArmor cut(float fraction) { this.cut = Math.max(0f, Math.min(1f, fraction)); return this; }
    public ConjuredArmor absorption(float health) { this.absorption = Math.max(0f, health); return this; }
    public ConjuredArmor regrow(int ticksPerPoint) { this.regrow = Math.max(0, ticksPerPoint); return this; }
    public ConjuredArmor steady() { this.steady = true; return this; }
    public ConjuredArmor onHit(BiConsumer<LivingEntity, Float> action) { this.onHit = action; return this; }
    public ConjuredArmor onEnd(Consumer<LivingEntity> action) { this.onEnd = action; return this; }

    /** Puts it on now, with its layer of absorption full. */
    public void wear(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel sl) || !entity.isAlive()) return;
        long now = sl.getGameTime();
        Map<String, Worn> mine = WORN.computeIfAbsent(entity.getUUID(), id -> new LinkedHashMap<>());
        mine.put(name, new Worn(this, lasts > 0 ? now + lasts : Long.MAX_VALUE, now));
        body(entity);
        if (absorption > 0f) entity.setAbsorptionAmount(Math.min(entity.getMaxAbsorption(), entity.getAbsorptionAmount() + absorption));
        if (steady) entity.removeEffect(MobEffects.SLOWNESS);
    }

    // =========================
    // API
    // =========================

    public static boolean isWorn(Entity entity, String name) {
        Map<String, Worn> mine = entity == null ? null : WORN.get(entity.getUUID());
        return mine != null && mine.containsKey(name);
    }

    /** Ticks until it comes off by itself (0 = not worn; Integer.MAX_VALUE = until taken off). */
    public static int ticksLeft(Entity entity, String name) {
        Map<String, Worn> mine = entity == null ? null : WORN.get(entity.getUUID());
        Worn worn = mine == null ? null : mine.get(name);
        if (worn == null) return 0;
        if (worn.until == Long.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) Math.max(0, worn.until - entity.level().getGameTime());
    }

    /** Takes it off now (its onEnd runs). */
    public static void takeOff(LivingEntity entity, String name) {
        Map<String, Worn> mine = WORN.get(entity.getUUID());
        Worn worn = mine == null ? null : mine.remove(name);
        if (mine != null && mine.isEmpty()) WORN.remove(entity.getUUID());
        if (worn == null) return;
        body(entity);
        ended(entity, worn);
    }

    // =========================
    // Internals
    // =========================

    /** The absorption all its armors give together. */
    private static float layer(LivingEntity entity) {
        float total = 0f;
        Map<String, Worn> mine = WORN.get(entity.getUUID());
        if (mine != null) for (Worn worn : mine.values()) total += worn.armor.absorption;
        return total;
    }

    private static boolean steady(LivingEntity entity) {
        Map<String, Worn> mine = WORN.get(entity.getUUID());
        if (mine != null) for (Worn worn : mine.values()) if (worn.armor.steady) return true;
        return false;
    }

    /** The layer's room (max absorption) and the steadiness, as the worn armors say. */
    private static void body(LivingEntity entity) {
        float layer = layer(entity);
        Physique.keep(entity, Attributes.MAX_ABSORPTION, LAYER_ID, layer, AttributeModifier.Operation.ADD_VALUE, layer > 0f);
        Physique.keep(entity, Attributes.KNOCKBACK_RESISTANCE, STEADY_ID, 1.0, AttributeModifier.Operation.ADD_VALUE, steady(entity));
        if (entity.getAbsorptionAmount() > entity.getMaxAbsorption()) entity.setAbsorptionAmount(entity.getMaxAbsorption());
    }

    private static void ended(LivingEntity entity, Worn worn) {
        if (worn.armor.onEnd == null) return;
        try {
            worn.armor.onEnd.accept(entity);
        } catch (Exception e) {
            CircleOfInevitability.LOGGER.error("A conjured armor's end failed", e);
        }
    }

    private static void clear(LivingEntity entity) {
        Map<String, Worn> mine = WORN.remove(entity.getUUID());
        if (mine == null) return;
        body(entity);
        for (Worn worn : mine.values()) ended(entity, worn);
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onConjuredArmorTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (WORN.isEmpty() || !(entity.level() instanceof ServerLevel sl)) return;
        Map<String, Worn> mine = WORN.get(entity.getUUID());
        if (mine == null) return;
        long now = sl.getGameTime();
        for (Map.Entry<String, Worn> entry : new ArrayList<>(mine.entrySet())) {
            Worn worn = entry.getValue();
            if (worn.until <= now) {
                mine.remove(entry.getKey());
                if (mine.isEmpty()) WORN.remove(entity.getUUID());
                body(entity);
                ended(entity, worn);
                continue;
            }
            int regrow = worn.armor.regrow;
            if (regrow > 0 && now - worn.lastHit >= regrow && (now - worn.lastHit) % regrow == 0
                    && entity.getAbsorptionAmount() < entity.getMaxAbsorption()) {
                entity.setAbsorptionAmount(Math.min(entity.getMaxAbsorption(), entity.getAbsorptionAmount() + 1f));
            }
        }
        if (now % 20 == 0 && WORN.containsKey(entity.getUUID())) body(entity);       // the modifiers are not saved
    }

    @SubscribeEvent
    public static void onConjuredArmorHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !WORN.containsKey(entity.getUUID())) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        Map<String, Worn> mine = WORN.get(entity.getUUID());
        float best = 0f;
        long now = entity.level().getGameTime();
        for (Worn worn : mine.values()) {
            best = Math.max(best, worn.armor.cut);
            worn.lastHit = now;
        }
        float amount = event.getAmount() * (1f - best);
        event.setAmount(amount);
        for (Worn worn : new ArrayList<>(mine.values())) {
            if (worn.armor.onHit == null) continue;
            try {
                worn.armor.onHit.accept(entity, amount);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A conjured armor's hit failed", e);
            }
        }
    }

    @SubscribeEvent
    public static void onConjuredArmorEffect(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !WORN.containsKey(entity.getUUID())) return;
        if (event.getEffectInstance().getEffect() == MobEffects.SLOWNESS && steady(entity)) event.setResult(Result.DENY);
    }

    @SubscribeEvent
    public static void onConjuredArmorDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onConjuredArmorLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity());
    }
}
