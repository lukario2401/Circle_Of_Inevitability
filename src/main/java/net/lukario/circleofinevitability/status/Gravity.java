package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;

/**
 * Gravity: an entity is heavier or lighter for a while. Starlight that presses enemies down, ground that bears no
 * weight, a heaven where nothing falls. Works from any class/ability, on players and mobs, with no potion swirls:
 *
 *   Gravity.set(enemy, "my_pathway_crush", 3.0, 40);    // three times as heavy for 2 seconds: it drops like a stone
 *   Gravity.set(ally, "my_pathway_float", 0.3, 40);     // a third: long, slow jumps and falls
 *   Gravity.set(enemy, "my_pathway_lift", -0.15, 40);   // below 0 it falls upward, slowly: lifted off the ground
 *   Gravity.factor(entity)                              // what it is now (1 = normal)
 *   Gravity.clear(entity, "my_pathway_lift")            // this reason ends; the others go on
 *
 * Every reason has a name (start it with your Pathway). Several at once multiply together. Setting a name again
 * replaces its factor and makes it last at least as long. It is the game's own gravity attribute underneath, so
 * a player's movement on their screen agrees. Nothing here stops fall damage: lift someone with FallGuard too.
 *
 * Saved with the entity (the attribute itself is put on again every tick while a reason lasts).
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Gravity {

    private static final String TAG = "circle_of_inevitability_gravity";        // {name: {Factor, Until}}
    private static final String FACTOR = "Factor";
    private static final String UNTIL = "Until";
    private static final Identifier MODIFIER = Physique.id("gravity_status");

    private Gravity() {}

    // =========================
    // API
    // =========================

    /** Its gravity is 'factor' times its own (1 = normal, 0 = none, below 0 = falls upward) for 'ticks', for 'name'. */
    public static void set(LivingEntity target, String name, double factor, int ticks) {
        if (target.level().isClientSide() || ticks <= 0) return;
        CompoundTag all = target.getPersistentData().getCompoundOrEmpty(TAG);
        long until = Math.max(target.level().getGameTime() + ticks, all.getCompoundOrEmpty(name).getLongOr(UNTIL, 0L));
        CompoundTag one = new CompoundTag();
        one.putDouble(FACTOR, factor);
        one.putLong(UNTIL, until);
        all.put(name, one);
        target.getPersistentData().put(TAG, all);
        update(target);
    }

    /** What its gravity is multiplied by right now (1 = normal). */
    public static double factor(Entity entity) {
        if (entity == null || !entity.getPersistentData().contains(TAG)) return 1.0;
        CompoundTag all = entity.getPersistentData().getCompoundOrEmpty(TAG);
        long now = entity.level().getGameTime();
        double factor = 1.0;
        for (String key : all.keySet()) {
            CompoundTag one = all.getCompoundOrEmpty(key);
            if (one.getLongOr(UNTIL, 0L) > now) factor *= one.getDoubleOr(FACTOR, 1.0);
        }
        return factor;
    }

    public static int ticksLeft(Entity entity, String name) {
        if (entity == null) return 0;
        long until = entity.getPersistentData().getCompoundOrEmpty(TAG).getCompoundOrEmpty(name).getLongOr(UNTIL, 0L);
        return (int) Math.max(0, until - entity.level().getGameTime());
    }

    public static void clear(LivingEntity target, String name) {
        if (!target.getPersistentData().contains(TAG)) return;
        CompoundTag all = target.getPersistentData().getCompoundOrEmpty(TAG);
        all.remove(name);
        if (all.isEmpty()) target.getPersistentData().remove(TAG);
        else target.getPersistentData().put(TAG, all);
        update(target);
    }

    public static void clearAll(LivingEntity target) {
        target.getPersistentData().remove(TAG);
        update(target);
    }

    // =========================
    // Internals
    // =========================

    /** Drops the reasons that ran out and puts the attribute where they say. */
    private static void update(LivingEntity entity) {
        if (entity.getPersistentData().contains(TAG)) {
            CompoundTag all = entity.getPersistentData().getCompoundOrEmpty(TAG);
            long now = entity.level().getGameTime();
            for (String key : new ArrayList<>(all.keySet())) {
                if (all.getCompoundOrEmpty(key).getLongOr(UNTIL, 0L) <= now) all.remove(key);
            }
            if (all.isEmpty()) entity.getPersistentData().remove(TAG);
            else entity.getPersistentData().put(TAG, all);
        }
        double factor = factor(entity);
        // ADD_MULTIPLIED_TOTAL: the gravity is multiplied by (1 + amount), so amount = factor - 1
        Physique.keep(entity, Attributes.GRAVITY, MODIFIER, factor - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,
                Math.abs(factor - 1.0) > 1.0E-4);
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onGravityLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (entity.getPersistentData().contains(TAG) || Physique.has(entity, Attributes.GRAVITY, MODIFIER)) update(entity);
    }

    @SubscribeEvent
    public static void onGravityDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) clearAll(event.getEntity());
    }
}
