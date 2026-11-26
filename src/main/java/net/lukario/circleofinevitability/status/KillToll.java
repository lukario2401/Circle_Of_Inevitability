package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.HudTimer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * KillToll: something that must be fed a kill every so often, or it punishes whoever carries it. Cursed armor, a
 * blade that thirsts, a pact with something hungry. Define it once, then tick it while it binds someone:
 *
 *   static final KillToll THIRST = KillToll.of("my_class_thirst", 6000)            // a kill every 5 minutes
 *           .warnAt(1200, 600, 200)                                              // warn with 60, 30 and 10 s left
 *           .hud("Thirst", 0xC03030)                                             // a clock beside the hotbar
 *           .onWarning((bearer, ticksLeft) -> ...)                               // a message, a sound
 *           .onFed(bearer -> ...)
 *           .onExpired(bearer -> ...);                                          // the punishment
 *
 *   THIRST.tick(player);          // every server tick while it binds them (worn, held, sworn): the clock runs
 *   THIRST.ticksLeft(player)
 *   THIRST.feed(player);          // fed by hand (kills feed it by themselves)
 *   THIRST.pause(player);         // it no longer binds them (the armor came off): the clock leaves the screen
 *   THIRST.forget(player);        // the pact is over: the clock is gone (taking the armor off does NOT need this)
 *
 * Any living thing the bearer kills while bound feeds it, however it died (a sword, an ability, a summon that
 * strikes in their name). The clock only runs while tick is called: stop calling it (the armor came off) and it
 * waits where it was; calling it again carries on from there, so taking it off cannot reset it. After it expires
 * it starts again full, so a punishment that fails (a creative player) does not repeat every tick. Dying clears it.
 *
 * Saved with the entity. Start the name with your class.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KillToll {

    public interface Warning { void warn(LivingEntity bearer, int ticksLeft); }
    public interface Event { void on(LivingEntity bearer); }

    private static final String TAG = "circle_of_inevitability_kill_toll";    // name -> {Left, Seen}
    private static final String LEFT = "Left";
    private static final String SEEN = "Seen";
    private static final String SHOWN = "Shown";                // its clock is on the bearer's screen
    private static final List<KillToll> ALL = new ArrayList<>();

    private final String name;
    private final int ticks;
    private int[] warnAt = new int[0];
    private String hudLabel;
    private int hudColor;
    private Warning onWarning = (bearer, left) -> {};
    private Event onFed = bearer -> {}, onExpired = bearer -> {};

    private KillToll(String name, int ticks) {
        this.name = Objects.requireNonNull(name);
        if (ticks < 1) throw new IllegalArgumentException("A toll needs at least one tick");
        this.ticks = ticks;
    }

    /** A toll named 'name' (start it with your class) that wants a kill every 'ticks' ticks. */
    public static KillToll of(String name, int ticks) {
        KillToll toll = new KillToll(name, ticks);
        synchronized (ALL) {
            ALL.removeIf(old -> old.name.equals(name));
            ALL.add(toll);
        }
        return toll;
    }

    public KillToll warnAt(int... ticksLeft) { warnAt = Arrays.stream(ticksLeft).filter(t -> t > 0).toArray(); return this; }
    public KillToll hud(String label, int rgb) { hudLabel = label; hudColor = rgb; return this; }
    public KillToll onWarning(Warning warning) { onWarning = Objects.requireNonNull(warning); return this; }
    public KillToll onFed(Event event) { onFed = Objects.requireNonNull(event); return this; }
    public KillToll onExpired(Event event) { onExpired = Objects.requireNonNull(event); return this; }

    public String name() { return name; }
    public int ticks() { return ticks; }

    // =========================
    // API
    // =========================

    /** Call every server tick while it binds 'bearer'. Counts down, warns, and punishes at 0. */
    public void tick(LivingEntity bearer) {
        if (bearer.level().isClientSide()) return;
        long now = bearer.level().getGameTime();
        CompoundTag tag = tag(bearer, true);
        boolean resumed = !bound(tag, now);
        int left = tag.getIntOr(LEFT, ticks) - 1;
        tag.putLong(SEEN, now);
        if (left <= 0) {
            tag.putInt(LEFT, ticks);
            showHud(bearer, ticks);
            onExpired.on(bearer);
            return;
        }
        tag.putInt(LEFT, left);
        if (resumed || left % 200 == 0) showHud(bearer, left);
        for (int warning : warnAt) if (left == warning) onWarning.warn(bearer, left);
    }

    /** Ticks left before it punishes; the full time if it is not running. */
    public int ticksLeft(LivingEntity bearer) {
        CompoundTag tag = tag(bearer, false);
        return tag == null ? ticks : tag.getIntOr(LEFT, ticks);
    }

    /** True while tick is being called for this bearer (it binds them right now). */
    public boolean binds(LivingEntity bearer) {
        CompoundTag tag = tag(bearer, false);
        return tag != null && bound(tag, bearer.level().getGameTime());
    }

    /** A kill: the clock starts again full. */
    public void feed(LivingEntity bearer) {
        CompoundTag tag = tag(bearer, true);
        tag.putInt(LEFT, ticks);
        if (bound(tag, bearer.level().getGameTime())) showHud(bearer, ticks);
        onFed.on(bearer);
    }

    /** Forgets the clock entirely (and takes the HUD clock away). */
    public void forget(LivingEntity bearer) {
        CompoundTag all = bearer.getPersistentData().getCompoundOrEmpty(TAG);
        all.remove(name);
        if (all.isEmpty()) bearer.getPersistentData().remove(TAG);
        hideHud(bearer);
    }

    /** It no longer binds them for now: the clock waits where it is and leaves the screen. Cheap to call every tick. */
    public void pause(LivingEntity bearer) {
        CompoundTag tag = tag(bearer, false);
        if (tag == null || !tag.getBooleanOr(SHOWN, false)) return;
        tag.remove(SHOWN);
        hideHud(bearer);
    }

    private void hideHud(LivingEntity bearer) {
        if (hudLabel != null && bearer instanceof Player player) HudTimer.hide(player, name);
    }

    // =========================
    // Internals
    // =========================

    private static boolean bound(CompoundTag tag, long now) {
        return tag.contains(SEEN) && now - tag.getLongOr(SEEN, Long.MIN_VALUE / 2) <= 2;
    }

    private void showHud(LivingEntity bearer, int left) {
        if (hudLabel == null || !(bearer instanceof Player player)) return;
        HudTimer.show(player, name, hudLabel, left, hudColor);
        tag(bearer, true).putBoolean(SHOWN, true);
    }

    private CompoundTag tag(LivingEntity bearer, boolean create) {
        CompoundTag data = bearer.getPersistentData();
        if (!data.contains(TAG)) {
            if (!create) return null;
            data.put(TAG, new CompoundTag());
        }
        CompoundTag all = data.getCompoundOrEmpty(TAG);
        if (!all.contains(name)) {
            if (!create) return null;
            all.put(name, new CompoundTag());
        }
        return all.getCompoundOrEmpty(name);
    }

    @SubscribeEvent
    public static void onKillTollDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide()) return;
        List<KillToll> tolls;
        synchronized (ALL) { tolls = List.copyOf(ALL); }
        // The dead bearer's clocks are over; whoever killed something while bound is fed.
        for (KillToll toll : tolls) if (toll.tag(dead, false) != null) toll.forget(dead);
        if (dead instanceof ArmorStand || !(event.getSource().getEntity() instanceof LivingEntity killer) || killer == dead) return;
        for (KillToll toll : tolls) if (toll.binds(killer)) toll.feed(killer);
    }
}
