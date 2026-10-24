package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A form: a player takes another shape for a while, with the body of that shape (attribute changes and potion effects
 * kept up while it lasts) and, if you want, a weakness afterwards. A Mythical Creature Form, scales over the skin, a
 * body of light. One manager for all of them, so each one is a few lines and they all end cleanly:
 *
 *   Form.of("my_pathway_scales")                                   // its name (start it with your Pathway)
 *           .lasts(600)                                           // ticks; 0 = until Form.end(...)  (default 0)
 *           .attribute(Attributes.ENTITY_INTERACTION_RANGE, 1.0, AttributeModifier.Operation.ADD_VALUE)
 *           .attribute(Attributes.SCALE, 1.5, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)   // as many as wanted
 *           .effect(MobEffects.STRENGTH, 1)                       // Strength II, kept up while it lasts
 *           .aftermath(200)                                       // 10 seconds of Weakness and Slowness after it
 *           .keepWhile(player -> SoulCore.getPathway(player).equals(PATHWAY))   // ends at once when this is false
 *           .everySecond(player -> MyFx.aura(...))                // an aura, a look, a drain
 *           .onEnd((player, why) -> MyFx.formEnd(...))            // its look ending, whatever the reason
 *           .take(player);
 *
 *   Form.isIn(entity, name)   Form.current(entity)   Form.ticksLeft(entity)   Form.end(player)   Form.inAftermath(entity)
 *
 * One form at a time: taking another ends the one there (REPLACED). It ends, and every change is taken off, when its
 * time is up (TIME), when Form.end is called (ENDED), when keepWhile says no (LOST: the Pathway changed), on death
 * (DIED) and on logout (LEFT). Not saved: after a server restart nobody is left transformed. The aftermath runs for
 * TIME and ENDED only (a death or a lost Pathway leaves nothing to recover from). Star Dragon's Scaled Form; the
 * Radiant Angel and the Star Dragon are planned on it, and Hand Of God's Giant Form could move onto it
 * (docs/STAR_DRAGON.md).
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Form {

    public enum Ending { TIME, ENDED, REPLACED, LOST, DIED, LEFT }

    private record Change(Holder<Attribute> attribute, Identifier id, double amount, AttributeModifier.Operation operation) {}

    private record Kept(Holder<MobEffect> effect, int amplifier) {}

    private final String name;
    private int lasts;
    private int aftermath;
    private final List<Change> changes = new ArrayList<>();
    private final List<Kept> effects = new ArrayList<>();
    private Predicate<Player> keepWhile = player -> true;
    private Consumer<Player> everySecond;
    private BiConsumer<Player, Ending> onEnd;

    /** One player in a form: which, and until when (0 = until ended). */
    private record Active(Form form, long until) {}

    private static final Map<UUID, Active> ACTIVE = new HashMap<>();
    private static final String AFTERMATH = "circle_of_inevitability_form_aftermath";   // game time it lasts until

    private Form(String name) {
        this.name = name;
    }

    public static Form of(String name) {
        return new Form(name);
    }

    public Form lasts(int ticks) { lasts = Math.max(0, ticks); return this; }

    /** An attribute change while it lasts (taken off when it ends). */
    public Form attribute(Holder<Attribute> attribute, double amount, AttributeModifier.Operation operation) {
        changes.add(new Change(attribute, Physique.id("form_" + name + "_" + changes.size()), amount, operation));
        return this;
    }

    /** A potion effect kept up while it lasts (amplifier 0 = level I). */
    public Form effect(Holder<MobEffect> effect, int amplifier) {
        effects.add(new Kept(effect, amplifier));
        return this;
    }

    /** After it ends by itself or is ended: Weakness I and Slowness I for this long. */
    public Form aftermath(int ticks) { aftermath = Math.max(0, ticks); return this; }

    /** Checked every second: it ends (LOST) the moment this is false. */
    public Form keepWhile(Predicate<Player> rule) { keepWhile = rule; return this; }

    public Form everySecond(Consumer<Player> action) { everySecond = action; return this; }

    public Form onEnd(BiConsumer<Player, Ending> action) { onEnd = action; return this; }

    public String name() { return name; }

    /** The player takes this form now (another form they were in ends first, REPLACED). */
    public void take(Player player) {
        if (player.level().isClientSide()) return;
        Active old = ACTIVE.get(player.getUUID());
        if (old != null) finish(player, old, Ending.REPLACED);
        long until = lasts > 0 ? player.level().getGameTime() + lasts : 0L;
        ACTIVE.put(player.getUUID(), new Active(this, until));
        apply(player);
    }

    // =========================
    // Asking about it
    // =========================

    public static boolean isIn(Player player, String name) {
        Active active = ACTIVE.get(player.getUUID());
        return active != null && active.form().name.equals(name);
    }

    /** The name of the form the player is in, or null. */
    public static String current(Player player) {
        Active active = ACTIVE.get(player.getUUID());
        return active == null ? null : active.form().name;
    }

    /** Ticks left of the form the player is in (-1 = until ended, 0 = none). */
    public static int ticksLeft(Player player) {
        Active active = ACTIVE.get(player.getUUID());
        if (active == null) return 0;
        if (active.until() == 0L) return -1;
        return (int) Math.max(0, active.until() - player.level().getGameTime());
    }

    /** It ends now (ENDED), with its aftermath. */
    public static void end(Player player) {
        Active active = ACTIVE.get(player.getUUID());
        if (active != null) finish(player, active, Ending.ENDED);
    }

    /** True while the weakness after a form lasts. */
    public static boolean inAftermath(Player player) {
        return player.getPersistentData().getLongOr(AFTERMATH, 0L) > player.level().getGameTime();
    }

    // =========================
    // Internals
    // =========================

    private void apply(Player player) {
        for (Change change : changes) {
            Physique.keep(player, change.attribute(), change.id(), change.amount(), change.operation(), true);
        }
        for (Kept kept : effects) Spells.keepEffect(player, kept.effect(), kept.amplifier(), 60);
    }

    private void takeOff(Player player) {
        for (Change change : changes) Physique.remove(player, change.attribute(), change.id());
        for (Kept kept : effects) {
            MobEffectInstance on = player.getEffect(kept.effect());
            if (on != null && on.getAmplifier() == kept.amplifier() && on.getDuration() <= 60) player.removeEffect(kept.effect());
        }
    }

    private static void finish(Player player, Active active, Ending why) {
        ACTIVE.remove(player.getUUID());
        Form form = active.form();
        form.takeOff(player);
        if (form.aftermath > 0 && (why == Ending.TIME || why == Ending.ENDED) && player.isAlive()) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, form.aftermath, 0));
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, form.aftermath, 0));
            player.getPersistentData().putLong(AFTERMATH, player.level().getGameTime() + form.aftermath);
        }
        if (form.onEnd != null) form.onEnd.accept(player, why);
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onFormPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        Player player = event.player();
        if (player.level().isClientSide()) return;
        Active active = ACTIVE.get(player.getUUID());
        if (active == null) return;
        if (active.until() != 0L && player.level().getGameTime() >= active.until()) {
            finish(player, active, Ending.TIME);
            return;
        }
        if (player.tickCount % 20 != 0) return;
        if (!active.form().keepWhile.test(player)) {
            finish(player, active, Ending.LOST);
            return;
        }
        active.form().apply(player);                                             // again after milk, a relog of effects...
        if (active.form().everySecond != null) active.form().everySecond.accept(player);
    }

    @SubscribeEvent
    public static void onFormDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
        Active active = ACTIVE.get(player.getUUID());
        if (active != null) finish(player, active, Ending.DIED);
    }

    @SubscribeEvent
    public static void onFormLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        Active active = ACTIVE.get(player.getUUID());
        if (active != null) finish(player, active, Ending.LEFT);
    }
}
