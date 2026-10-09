package net.lukario.frogerealm.combat;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.network.CShowHudOverlayPacket;
import net.lukario.frogerealm.network.PacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/**
 * A countdown on a player's screen: big numbers 5, 4, 3, 2, 1 in the middle of it, then something happens.
 * For abilities that need time to take hold, charge up or go off:
 *
 *   Countdown.seconds(5)
 *           .colors(0xE8C872, 0xD83A4A)              // the numbers, and the last one (the "1"), 0xRRGGBB
 *           .onSecond(left -> ...)                   // each time a number shows: 5, 4, 3, 2, 1
 *           .onTick(ticksLeft -> ...)                // every tick while it runs (optional)
 *           .keepWhile(() -> target.isAlive())       // checked every tick; false = the countdown is cancelled
 *           .onFinish(() -> ...)                     // it reached 0
 *           .onCancel(() -> ...)                     // keepWhile failed, the player died or left, or Countdown.cancel(player)
 *           .start(player);
 *
 * Every part is optional. .silent() counts without showing the numbers (when the ability shows a picture instead).
 * A player has one countdown at a time: starting another one cancels the one that is running.
 * Build a new one for every cast (it keeps its own count), so don't store one in a constant.
 *
 *   Countdown.isRunning(player)     Countdown.ticksLeft(player)     Countdown.cancel(player)
 *
 * It is not saved: if the server stops in the middle of a countdown, it is simply gone.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Countdown {

    private static final Map<UUID, Countdown> RUNNING = new HashMap<>();

    private final int seconds;
    private int color = 0xFFFFFF;
    private int lastColor = 0xFFFFFF;
    private boolean silent = false;
    private IntConsumer onSecond = null;
    private IntConsumer onTick = null;
    private BooleanSupplier keepWhile = null;
    private Runnable onFinish = null;
    private Runnable onCancel = null;

    private int ticksLeft;

    private Countdown(int seconds) {
        this.seconds = Math.max(1, seconds);
    }

    /** A countdown from this many seconds. Nothing happens until .start(player). */
    public static Countdown seconds(int seconds) {
        return new Countdown(seconds);
    }

    // ---------- setting it up ----------

    /** Color of the numbers, 0xRRGGBB. */
    public Countdown color(int rgb) {
        this.color = rgb;
        this.lastColor = rgb;
        return this;
    }

    /** Color of the numbers, and a different one for the last number (the "1"). */
    public Countdown colors(int rgb, int lastRgb) {
        this.color = rgb;
        this.lastColor = lastRgb;
        return this;
    }

    /** Count without showing the numbers. */
    public Countdown silent() {
        this.silent = true;
        return this;
    }

    /** Runs each time a new number shows, with that number: first 'seconds', then one less every second, down to 1. */
    public Countdown onSecond(IntConsumer action) {
        this.onSecond = action;
        return this;
    }

    /** Runs every tick with the ticks that are left (starts at seconds * 20 - 1, never reaches 0). */
    public Countdown onTick(IntConsumer action) {
        this.onTick = action;
        return this;
    }

    /** Checked every tick. As soon as it is false the countdown stops and onCancel runs. */
    public Countdown keepWhile(BooleanSupplier condition) {
        this.keepWhile = condition;
        return this;
    }

    /** Runs when the countdown reaches 0. */
    public Countdown onFinish(Runnable action) {
        this.onFinish = action;
        return this;
    }

    /** Runs when the countdown stops early. */
    public Countdown onCancel(Runnable action) {
        this.onCancel = action;
        return this;
    }

    /** Starts counting for this player (cancelling the countdown they already had, if any). */
    public void start(Player player) {
        cancel(player);
        this.ticksLeft = seconds * 20;
        RUNNING.put(player.getUUID(), this);
        announce(player, seconds);
    }

    // ---------- asking / stopping ----------

    public static boolean isRunning(Player player) {
        return RUNNING.containsKey(player.getUUID());
    }

    /** Ticks until the player's countdown reaches 0 (0 = none is running). */
    public static int ticksLeft(Player player) {
        Countdown running = RUNNING.get(player.getUUID());
        return running == null ? 0 : running.ticksLeft;
    }

    /** Stops the player's countdown early (its onCancel runs). Does nothing if none is running. */
    public static void cancel(Player player) {
        Countdown running = RUNNING.remove(player.getUUID());
        if (running != null && running.onCancel != null) running.onCancel.run();
    }

    // ---------- internals ----------

    private void announce(Player player, int number) {
        if (!silent && player instanceof ServerPlayer serverPlayer) {
            int rgb = number == 1 ? lastColor : color;
            PacketHandler.sendToPlayer(CShowHudOverlayPacket.number(number, rgb, 20), serverPlayer);
        }
        if (onSecond != null) onSecond.accept(number);
    }

    private void tick(Player player) {
        if (!player.isAlive() || (keepWhile != null && !keepWhile.getAsBoolean())) {
            cancel(player);
            return;
        }
        ticksLeft--;
        if (ticksLeft <= 0) {
            RUNNING.remove(player.getUUID());
            if (onFinish != null) onFinish.run();
            return;
        }
        if (onTick != null) onTick.accept(ticksLeft);
        // the countdown may have been cancelled or replaced by what just ran
        if (RUNNING.get(player.getUUID()) == this && ticksLeft % 20 == 0) announce(player, ticksLeft / 20);
    }

    /** One tick of this countdown. If one of its parts fails, it is logged and the countdown ends (the game goes on). */
    private void tickSafely(Player player) {
        try {
            tick(player);
        } catch (Exception e) {
            ForgeRealm.LOGGER.error("A countdown failed", e);
            RUNNING.remove(player.getUUID(), this);
        }
    }

    @SubscribeEvent
    public static void onCountdownPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;
        Countdown running = RUNNING.get(event.player.getUUID());
        if (running != null) running.tickSafely(event.player);
    }

    @SubscribeEvent
    public static void onCountdownPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        cancel(event.getEntity());
    }

    // a countdown never carries over to the body a player respawns in
    @SubscribeEvent
    public static void onCountdownPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        cancel(event.getEntity());
    }
}
