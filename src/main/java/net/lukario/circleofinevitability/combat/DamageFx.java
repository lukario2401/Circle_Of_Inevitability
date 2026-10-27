package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.particles.fx.DamageNumber;
import net.lukario.circleofinevitability.status.Marionette;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Damage fx: the damage a player deals, as a red number above the target's head.
 * Every player turns it on or off for themself, no op needed:
 *
 *   /damagefx          switches it
 *   /damagefx on
 *   /damagefx off
 *
 * Off until a player turns it on. Saved with the player: it stays through death, relog and a server restart.
 *
 * A number is shown when the player who DEALT the damage has it on. It is shown to that player and to every
 * other player within VIEW_RANGE blocks of the target who has it on too (never to the one who was hurt).
 *
 * What counts as a player's damage:
 *   - everything that names them as the attacker: melee, arrows and other projectiles, Spells.strike,
 *     explosions they set off, damage passed on by DamageLink
 *   - what their marionettes deal
 *   - burning, poison, wither and other magic that names no attacker, for CREDIT_TICKS after the player last
 *     hurt that entity (so a mob you set on fire keeps showing numbers while it burns)
 *
 * An ability that hurts with a source that names nobody (sl.damageSources().magic(), an explosion or a
 * lightning bolt with no owner) says whose damage it is just before:
 *
 *   DamageFx.credit(player, target);
 *   target.hurt(sl.damageSources().magic(), damage);
 *
 * credit(...) changes nothing about the damage, it is only remembered for the numbers.
 *
 * The number is what the target really lost: after armor, resistance and every other listener, with the
 * yellow absorption hearts counted. Nothing is shown for damage that was cancelled or came to 0.
 *
 * How it is drawn (size, colour, how long it stays): particles/fx/DamageNumberRenderer. Guide: docs/DAMAGE_FX.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DamageFx {

    /** Other players see a number when they are within this many blocks of the target. */
    public static double VIEW_RANGE = 48.0;
    /** The player who dealt the damage sees it from this far (a long bow shot). */
    public static double OWN_RANGE = 128.0;
    /** How long burning, poison, wither and magic with no attacker count as the player's, after their last hit. */
    public static int CREDIT_TICKS = 300;
    /** After DamageFx.credit(...): how long ANY damage with no attacker counts as the player's (a blast, a bolt on its way). */
    public static int ABILITY_CREDIT_TICKS = 40;

    /** Less than this is not shown (it would read "0"). */
    private static final float SMALLEST_SHOWN = 0.05f;

    /** Inside the part of getPersistentData() that Forge itself carries over to the new player after a death. */
    private static final String KEY = "circle_of_inevitability_damage_fx";

    /** Whose damage the attacker-less damage on an entity is: lingering kinds until 'until', any kind until 'anyUntil' (game time). */
    private record Credit(UUID player, long until, long anyUntil) {}

    // server thread only; an entity that is unloaded or dead drops out by itself
    private static final Map<LivingEntity, Credit> CREDITS = new WeakHashMap<>();
    private static final Map<LivingEntity, Float> ABSORPTION_BEFORE = new WeakHashMap<>();

    private DamageFx() {}

    // =========================
    // API
    // =========================

    public static boolean isOn(LivingEntity player) {
        return player.getPersistentData().getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG).getBooleanOr(KEY, false);
    }

    public static void set(LivingEntity player, boolean on) {
        CompoundTag data = player.getPersistentData();
        CompoundTag kept = data.getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG);
        kept.putBoolean(KEY, on);
        data.put(ServerPlayer.PERSISTED_NBT_TAG, kept);
    }

    /**
     * Says that what hurts 'target' next is this player's doing, for damage whose source names no attacker.
     * Call it right before target.hurt(...) (or before the explosion / the lightning bolt). It only matters for
     * the numbers: the damage, who gets the kill and who the mob gets angry at stay exactly as they were.
     */
    public static void credit(LivingEntity player, LivingEntity target) {
        if (player == null || target == null || target.level().isClientSide()) return;
        long now = target.level().getGameTime();
        CREDITS.put(target, new Credit(player.getUUID(), now + CREDIT_TICKS, now + ABILITY_CREDIT_TICKS));
    }

    /** Explicit ability credit only, for gameplay modifiers. Does not borrow the long-lived visual DoT credit. */
    public static ServerPlayer abilityDealerOf(LivingEntity victim, DamageSource source, ServerLevel sl) {
        if (source.getEntity() != null) {
            Entity attacker = source.getEntity();
            return attacker instanceof ServerPlayer player ? player : online(sl, Marionette.masterId(attacker));
        }
        if (source.getDirectEntity() != null) return null;
        Credit credit = CREDITS.get(victim);
        return credit != null && credit.anyUntil() >= sl.getGameTime() ? online(sl, credit.player()) : null;
    }

    // =========================
    // Command
    // =========================

    @SubscribeEvent
    public static void onDamageFxRegisterCommands(RegisterCommandsEvent event) {
        // no .requires(...): every player may use it
        event.getDispatcher().register(Commands.literal("damagefx")
                .executes(ctx -> {
                    Player player = ctx.getSource().getPlayerOrException();
                    return turn(ctx.getSource(), player, !isOn(player));
                })
                .then(Commands.literal("on")
                        .executes(ctx -> turn(ctx.getSource(), ctx.getSource().getPlayerOrException(), true)))
                .then(Commands.literal("off")
                        .executes(ctx -> turn(ctx.getSource(), ctx.getSource().getPlayerOrException(), false))));
    }

    private static int turn(CommandSourceStack source, LivingEntity player, boolean on) {
        set(player, on);
        source.sendSuccess(() -> Component.translatable(
                on ? "message.circle_of_inevitability.damage_fx.on" : "message.circle_of_inevitability.damage_fx.off"), false);
        return 1;
    }

    // =========================
    // Events
    // =========================

    // Before armor is counted: remembers the yellow hearts, to see afterwards how much of the hit they took.
    @SubscribeEvent
    public static void onDamageFxHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) return;
        float absorption = victim.getAbsorptionAmount();
        if (absorption > 0f) ABSORPTION_BEFORE.put(victim, absorption);
        else ABSORPTION_BEFORE.remove(victim);
    }

    // Last of all listeners, so the amount is what the target really loses. A listener that cancels the damage
    // stops the event before it gets here, so cancelled damage shows no number.
    @SubscribeEvent(priority = Priority.LOWEST)
    public static void onDamageFxDamage(LivingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel sl)) return;

        Float before = ABSORPTION_BEFORE.remove(victim);
        float absorbed = before == null ? 0f : Math.max(0f, before - victim.getAbsorptionAmount());
        float amount = event.getAmount() + absorbed;

        ServerPlayer dealer = dealerOf(victim, event.getSource(), sl);
        if (dealer == null || dealer == victim) return;
        if (!(amount >= SMALLEST_SHOWN) || !isOn(dealer)) return;
        show(sl, dealer, victim, amount);
    }

    // =========================
    // Internals
    // =========================

    /** The player this damage belongs to, or null. A hit that names a player also makes them the one later burning counts for. */
    private static ServerPlayer dealerOf(LivingEntity victim, DamageSource source, ServerLevel sl) {
        long now = sl.getGameTime();
        Entity attacker = source.getEntity();

        if (attacker == null) {
            if (source.getDirectEntity() != null) return null;          // an arrow from a dispenser, a falling anvil
            Credit credit = CREDITS.get(victim);
            if (credit == null) return null;
            if (now > credit.anyUntil() && !(now <= credit.until() && lingers(source))) return null;
            return online(sl, credit.player());
        }

        ServerPlayer dealer = attacker instanceof ServerPlayer player ? player : online(sl, Marionette.masterId(attacker));
        if (dealer != null && dealer != victim) {
            Credit old = CREDITS.get(victim);
            long anyUntil = old != null && old.player().equals(dealer.getUUID()) ? old.anyUntil() : 0L;
            CREDITS.put(victim, new Credit(dealer.getUUID(), now + CREDIT_TICKS, anyUntil));
        }
        return dealer;
    }

    /** Damage that keeps coming after the hit that caused it: burning, wither, and magic (poison is magic). */
    private static boolean lingers(DamageSource source) {
        return source.is(DamageTypes.ON_FIRE) || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.WITHER);
    }

    private static ServerPlayer online(ServerLevel sl, UUID id) {
        if (id == null) return null;
        Player found = sl.getPlayerByUUID(id);          // null when they left or are in another dimension
        return found instanceof ServerPlayer player ? player : null;
    }

    private static void show(ServerLevel sl, ServerPlayer dealer, LivingEntity victim, float amount) {
        DamageNumber number = new DamageNumber(amount, victim.getId());
        double x = victim.getX();
        double y = victim.getY() + victim.getBbHeight();    // the top of the head; the client lifts it above that
        double z = victim.getZ();
        for (ServerPlayer viewer : sl.players()) {
            if (viewer == victim) continue;                 // a number above your own head is no use to you
            if (viewer != dealer && !isOn(viewer)) continue;
            double range = viewer == dealer ? OWN_RANGE : VIEW_RANGE;
            if (viewer.distanceToSqr(x, y, z) > range * range) continue;
            // (as ParticleShapes.spawnFar: sent whatever the viewer's particle setting is; count 0 = exactly here)
            sl.sendParticles(viewer, number, true, true, x, y, z, 0, 0.0, 0.0, 0.0, 1.0);
        }
    }
}
