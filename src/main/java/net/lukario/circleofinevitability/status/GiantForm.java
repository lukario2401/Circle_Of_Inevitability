package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A giant (or any grown) form: an entity swells to a bigger body for a while, with longer reach and a longer step,
 * and, if you want, a madness that rises while it stays. A Mythical Creature Form, a titan's shape, a curse of growth.
 * Works from any class/ability, on players and mobs:
 *
 *   GiantForm.of(1.5)                          // 1.5 times the size it has now
 *           .reach(1.5)                        // blocks more reach (players)            (optional)
 *           .stepHeight(1.0)                   // blocks more it steps up without jumping (optional)
 *           .lasts(800)                        // ticks; 0 = until GiantForm.end(...)    (default 0)
 *           .madness(1f / 30f)                 // madness gained each second, 0..1; 0 = none (default)
 *           .everySecond((giant, madness) -> ...)   // a stomp, an aura, the madness shown  (optional)
 *           .onMad(giant -> ...)               // madness reached 1: the form ends right after (optional)
 *           .onEnd((giant, why) -> ...)        // it ended, and why (Ending)            (optional)
 *           .grow(player);
 *
 *   GiantForm.isGiant(entity)    GiantForm.madness(entity)    GiantForm.ticksLeft(entity)    GiantForm.end(entity)
 *
 * The body is attribute modifiers with fixed ids (scale, reach, step height), so one form at a time: growing again
 * replaces the old form (its onEnd runs with REPLACED). It ends, and the body shrinks back, on death, logout and a
 * change of dimension. Not saved, and the modifiers are not either: after a server restart nobody is left giant.
 * The size is on top of what the entity already has (a Pathway's own Physique stays).
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class GiantForm {

    public enum Ending { TIME, MAD, ENDED, REPLACED, DIED, LEFT }

    @FunctionalInterface
    public interface Second {
        void run(LivingEntity giant, float madness);
    }

    @FunctionalInterface
    public interface End {
        void run(LivingEntity giant, Ending why);
    }

    private static final Identifier SCALE_ID = Physique.id("giant_form_scale");
    private static final Identifier REACH_ID = Physique.id("giant_form_reach");
    private static final Identifier BLOCK_REACH_ID = Physique.id("giant_form_block_reach");
    private static final Identifier STEP_ID = Physique.id("giant_form_step");

    private final double scale;
    private double reach;
    private double stepHeight;
    private int lasts;
    private float madnessPerSecond;
    private Second everySecond;
    private Consumer<LivingEntity> onMad;
    private End onEnd;

    /** One giant: its form and where it is. */
    private static final class Giant {
        final GiantForm form;
        final long started;
        float madness;

        Giant(GiantForm form, long started) {
            this.form = form;
            this.started = started;
        }
    }

    private static final Map<UUID, Giant> GIANTS = new HashMap<>();

    private GiantForm(double scale) {
        this.scale = Math.max(0.1, scale);
    }

    /** A form 'scale' times the entity's size now (2 = twice as tall and wide). */
    public static GiantForm of(double scale) {
        return new GiantForm(scale);
    }

    public GiantForm reach(double blocks) { this.reach = blocks; return this; }
    public GiantForm stepHeight(double blocks) { this.stepHeight = blocks; return this; }
    public GiantForm lasts(int ticks) { this.lasts = Math.max(0, ticks); return this; }
    public GiantForm madness(float perSecond) { this.madnessPerSecond = Math.max(0f, perSecond); return this; }
    public GiantForm everySecond(Second action) { this.everySecond = action; return this; }
    public GiantForm onMad(Consumer<LivingEntity> action) { this.onMad = action; return this; }
    public GiantForm onEnd(End action) { this.onEnd = action; return this; }

    /** The entity takes this form now (any form it had ends first, as REPLACED). */
    public void grow(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel sl) || !entity.isAlive()) return;
        if (GIANTS.containsKey(entity.getUUID())) finish(entity, Ending.REPLACED);
        GIANTS.put(entity.getUUID(), new Giant(this, sl.getGameTime()));
        body(entity, this);
    }

    // =========================
    // API
    // =========================

    public static boolean isGiant(Entity entity) {
        return entity != null && GIANTS.containsKey(entity.getUUID());
    }

    /** 0..1: how mad the giant is now (0 for a form without madness). */
    public static float madness(Entity entity) {
        Giant giant = entity == null ? null : GIANTS.get(entity.getUUID());
        return giant == null ? 0f : giant.madness;
    }

    /** Ticks until the form ends by itself (0 = not giant; Integer.MAX_VALUE = until ended). */
    public static int ticksLeft(Entity entity) {
        Giant giant = entity == null ? null : GIANTS.get(entity.getUUID());
        if (giant == null) return 0;
        if (giant.form.lasts <= 0) return Integer.MAX_VALUE;
        return (int) Math.max(0, giant.started + giant.form.lasts - entity.level().getGameTime());
    }

    /** How many times its size the form makes it (1 = not giant). */
    public static double scale(Entity entity) {
        Giant giant = entity == null ? null : GIANTS.get(entity.getUUID());
        return giant == null ? 1.0 : giant.form.scale;
    }

    /** Ends it now (onEnd runs with ENDED). */
    public static void end(LivingEntity entity) {
        if (isGiant(entity)) finish(entity, Ending.ENDED);
    }

    // =========================
    // Internals
    // =========================

    private static void body(LivingEntity entity, GiantForm form) {
        boolean on = form != null;
        Physique.keep(entity, Attributes.SCALE, SCALE_ID, on ? form.scale - 1.0 : 0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, on && form.scale != 1.0);
        Physique.keep(entity, Attributes.ENTITY_INTERACTION_RANGE, REACH_ID, on ? form.reach : 0, AttributeModifier.Operation.ADD_VALUE, on && form.reach != 0);
        Physique.keep(entity, Attributes.BLOCK_INTERACTION_RANGE, BLOCK_REACH_ID, on ? form.reach : 0, AttributeModifier.Operation.ADD_VALUE, on && form.reach != 0);
        Physique.keep(entity, Attributes.STEP_HEIGHT, STEP_ID, on ? form.stepHeight : 0, AttributeModifier.Operation.ADD_VALUE, on && form.stepHeight != 0);
    }

    private static void finish(LivingEntity entity, Ending why) {
        Giant giant = GIANTS.remove(entity.getUUID());
        body(entity, null);
        if (giant == null || giant.form.onEnd == null) return;
        try {
            giant.form.onEnd.run(entity, why);
        } catch (Exception e) {
            CircleOfInevitability.LOGGER.error("A giant form's end failed", e);
        }
    }

    private static void tick(LivingEntity entity, ServerLevel sl, Giant giant) {
        GiantForm form = giant.form;
        long age = sl.getGameTime() - giant.started;
        if (form.lasts > 0 && age >= form.lasts) {
            finish(entity, Ending.TIME);
            return;
        }
        if (age == 0 || age % 20 != 0) return;
        body(entity, form);                                         // only changes what was changed by someone else
        if (form.madnessPerSecond > 0f) giant.madness = Math.min(1f, giant.madness + form.madnessPerSecond);
        try {
            if (form.everySecond != null) form.everySecond.run(entity, giant.madness);
            if (giant.madness >= 1f) {
                if (form.onMad != null) form.onMad.accept(entity);
                if (GIANTS.get(entity.getUUID()) == giant) finish(entity, Ending.MAD);
            }
        } catch (Exception e) {
            CircleOfInevitability.LOGGER.error("A giant form's second failed", e);
        }
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onGiantFormLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (GIANTS.isEmpty() || !(entity.level() instanceof ServerLevel sl)) return;
        Giant giant = GIANTS.get(entity.getUUID());
        if (giant != null) tick(entity, sl, giant);
    }

    @SubscribeEvent
    public static void onGiantFormDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide() && isGiant(entity)) finish(entity, Ending.DIED);
    }

    @SubscribeEvent
    public static void onGiantFormLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (isGiant(event.getEntity())) finish(event.getEntity(), Ending.LEFT);
    }

    @SubscribeEvent
    public static void onGiantFormChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (isGiant(event.getEntity())) finish(event.getEntity(), Ending.LEFT);
    }

    // a mob that was unloaded while giant: forget it once its time is long over (it comes back at its own size)
    @SubscribeEvent
    public static void onGiantFormLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (GIANTS.isEmpty() || !(event.level() instanceof ServerLevel sl) || sl.getGameTime() % 600 != 0) return;
        long now = sl.getGameTime();
        GIANTS.values().removeIf(giant -> giant.form.lasts > 0 && now - giant.started > giant.form.lasts + 600L);
    }
}
