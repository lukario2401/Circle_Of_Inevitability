package net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A Beyonder mob's form (a Mythical Creature Form, scales, a giant's body): status/Form for mobs. status/Form is a
 * player's (its parts are written for players); a mob's abilities take their forms here, with the same pieces:
 *
 *   MobForm.of("star_dragon_scales").lasts(400)
 *           .attribute(Attributes.SCALE, 1.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
 *           .effect(MobEffects.STRENGTH, 1)          // kept up while it lasts
 *           .cutsDamage(0.4f)                        // 40% less damage taken while it lasts
 *           .aftermath(200)                          // Weakness I and Slowness I after it
 *           .every(10, mob -> MyFx.glow(...))        // every 10 ticks while it lasts
 *           .onEnd(mob -> MyFx.formEnd(...))         // its look ending, however it ends
 *           .take(mob);
 *
 *   MobForm.isIn(mob, name)   MobForm.current(mob)   MobForm.end(mob)
 *
 * One form at a time (a new one ends the old). Not saved: after a restart no mob is left transformed.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class MobForm {

    private record Change(Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation operation) {}
    private record Kept(Holder<MobEffect> effect, int amplifier) {}
    private record Active(MobForm form, long until, long since) {}

    private static final Map<UUID, Active> ACTIVE = new HashMap<>();

    private final String name;
    private int lasts = 200;
    private int aftermath;
    private float cut;
    private int everyTicks = 20;
    private Consumer<Mob> every;
    private Consumer<Mob> onEnd;
    private final List<Change> changes = new ArrayList<>();
    private final List<Kept> effects = new ArrayList<>();

    private MobForm(String name) {
        this.name = name;
    }

    public static MobForm of(String name) {
        return new MobForm(name);
    }

    public MobForm lasts(int ticks) { lasts = Math.max(1, ticks); return this; }

    public MobForm attribute(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
        changes.add(new Change(attribute, Physique.id("mob_form_" + name + "_" + changes.size()), amount, operation));
        return this;
    }

    public MobForm effect(Holder<MobEffect> effect, int amplifier) { effects.add(new Kept(effect, amplifier)); return this; }

    public MobForm cutsDamage(float fraction) { cut = Math.max(0f, Math.min(1f, fraction)); return this; }

    public MobForm aftermath(int ticks) { aftermath = Math.max(0, ticks); return this; }

    public MobForm every(int ticks, Consumer<Mob> action) { everyTicks = Math.max(1, ticks); every = action; return this; }

    public MobForm onEnd(Consumer<Mob> action) { onEnd = action; return this; }

    /** The mob takes this form now (one it was in ends first). */
    public void take(Mob mob) {
        if (!(mob.level() instanceof ServerLevel sl)) return;
        Active old = ACTIVE.get(mob.getUUID());
        if (old != null) finish(mob, old, false);
        long now = sl.getGameTime();
        Active active = new Active(this, now + lasts, now);
        ACTIVE.put(mob.getUUID(), active);
        for (Change change : changes) {
            Physique.keep(mob, change.attribute(), change.id(), change.amount(), change.operation(), true);
        }
        beat(mob, sl, active, 0);
    }

    /** Every everyTicks while it lasts: its effects kept up and its 'every'; at the end, it ends. */
    private void beat(Mob mob, ServerLevel sl, Active active, int age) {
        if (ACTIVE.get(mob.getUUID()) != active) return;                       // ended, or replaced
        if (!MobSpells.casterStillHere(mob, sl)) {
            finish(mob, active, false);
            return;
        }
        if (sl.getGameTime() >= active.until()) {
            finish(mob, active, true);
            return;
        }
        for (Kept kept : effects) Spells.keepEffect(mob, kept.effect(), kept.amplifier(), Math.max(60, everyTicks * 3));
        if (every != null && age > 0) every.accept(mob);
        int next = (int) Math.min(everyTicks, Math.max(1, active.until() - sl.getGameTime()));
        Later.run(sl, next, () -> beat(mob, sl, active, age + next));
    }

    private static void finish(Mob mob, Active active, boolean ranOut) {
        ACTIVE.remove(mob.getUUID(), active);
        MobForm form = active.form();
        for (Change change : form.changes) Physique.remove(mob, change.attribute(), change.id());
        for (Kept kept : form.effects) {
            MobEffectInstance on = mob.getEffect(kept.effect());
            if (on != null && on.getAmplifier() == kept.amplifier()) mob.removeEffect(kept.effect());
        }
        if (ranOut && form.aftermath > 0 && mob.isAlive()) {
            mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, form.aftermath, 0));
            mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, form.aftermath, 0));
        }
        if (form.onEnd != null) form.onEnd.accept(mob);
    }

    public String name() { return name; }

    // =========================
    // Asking
    // =========================

    public static boolean isIn(LivingEntity mob, String name) {
        Active active = ACTIVE.get(mob.getUUID());
        return active != null && active.form().name.equals(name) && active.until() > mob.level().getGameTime();
    }

    /** The name of the form it is in, or null. */
    public static String current(LivingEntity mob) {
        Active active = ACTIVE.get(mob.getUUID());
        return active == null || active.until() <= mob.level().getGameTime() ? null : active.form().name;
    }

    /** It turns back now (with the aftermath). */
    public static void end(Mob mob) {
        Active active = ACTIVE.get(mob.getUUID());
        if (active != null) finish(mob, active, true);
    }

    // =========================
    // Events
    // =========================

    /** A form that cuts damage. */
    @SubscribeEvent
    public static void onMobFormHurt(LivingHurtEvent event) {
        if (ACTIVE.isEmpty()) return;
        Active active = ACTIVE.get(event.getEntity().getUUID());
        if (active == null || active.form().cut <= 0f || active.until() <= event.getEntity().level().getGameTime()) return;
        event.setAmount(event.getAmount() * (1f - active.form().cut));
    }

    @SubscribeEvent
    public static void onMobFormDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob)) return;
        Active active = ACTIVE.get(mob.getUUID());
        if (active != null) finish(mob, active, false);
    }
}
