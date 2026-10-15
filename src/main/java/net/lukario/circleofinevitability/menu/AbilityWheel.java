package net.lukario.circleofinevitability.menu;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * A wheel of spells on one key: a round menu of entries, each with its own Sequence, spirituality cost and
 * cooldown. Entries above the player's Sequence are greyed out, the cost and cooldown are written under each one,
 * and the last pick is saved on the player so a sneak press can cast it again without the screen:
 *
 *   private static final AbilityWheel SPELLS = AbilityWheel.create("my_pathway_spells", "Spells")
 *           .entry("fire",  "my_pathway/fire",  "Fireball", 6, 1250, 40,  MyPathway::fireball)
 *           .entry("storm", "my_pathway/storm", "Storm",    4, 5000, 200, MyPathway::storm)
 *           .onCast((player, sl, entry) -> addStack(player, sl));        // after every cast that went off
 *
 *   // in the key's ability, once the Pathway and the Sequence of the key are checked:
 *   if (player.isShiftKeyDown()) SPELLS.quickCast(player);              // the last pick (no last pick: the wheel)
 *   else SPELLS.open(player);
 *
 * - key:      the entry's name in the save data. Keep it once players have used it (the last pick is saved by it).
 * - icon:     PNG in assets/circle_of_inevitability/textures/gui/ without ".png", as for AbilityMenu.
 * - sequence: the Sequence it unlocks at. cost: spirituality. cooldown: ticks before it can be cast again.
 * - the cast returns what happened:
 *     DONE     it went off: the cost is taken, the cooldown starts, onCast runs.
 *     FREE     it went off, but costs nothing and starts no cooldown (the second press of a two-part spell).
 *     REFUSED  it could not (no target, no room): nothing is taken. Tell the player why in the cast.
 *   The wheel checks the Sequence, the cooldown and that the player has the spirituality BEFORE the cast, and
 *   takes it after, so a cast only has to check what is its own (a target, room).
 * - .quickCastCooldown(false).quickCastCost(2f): a sneak recast ignores the cooldown and costs double.
 * - .free(player -> ...) after an entry: while it says true, that entry skips the cost and cooldown checks
 *   (pair it with FREE: the cast that ties off a spell already paid for).
 *
 * Cast through Beyonder.castUnbound (a stolen ability) there is no screen: the last pick is cast at once,
 * or a random entry if there is none.
 * Saved on the player: the last pick and the cooldowns (in game time), so both survive a relog.
 */
public final class AbilityWheel {

    public enum Result { DONE, FREE, REFUSED }

    @FunctionalInterface
    public interface Cast {
        Result cast(ServerPlayer player, ServerLevel sl);
    }

    /** Runs after an entry went off with DONE. */
    @FunctionalInterface
    public interface CastDone {
        void run(ServerPlayer player, ServerLevel sl, Entry entry);
    }

    public static final class Entry {
        private final String key;
        private final String icon;
        private final String name;
        private final int sequence;
        private final int cost;
        private final int cooldown;
        private final Cast cast;
        private Predicate<ServerPlayer> free;

        private Entry(String key, String icon, String name, int sequence, int cost, int cooldown, Cast cast) {
            this.key = key;
            this.icon = icon;
            this.name = name;
            this.sequence = sequence;
            this.cost = cost;
            this.cooldown = cooldown;
            this.cast = cast;
        }

        public String key() { return key; }
        public String name() { return name; }
        public int sequence() { return sequence; }
        public int cost() { return cost; }
        public int cooldown() { return cooldown; }

        private boolean isFree(ServerPlayer player) {
            return free != null && free.test(player);
        }
    }

    private final String id;
    private final String title;
    private int radius = 72;
    private final List<Entry> entries = new ArrayList<>();
    private CastDone onCast;
    private boolean quickCastCooldown = true;        // false: a sneak recast neither waits for nor starts a cooldown
    private float quickCastCost = 1f;                // a sneak recast costs this many times the entry's cost
    private AbilityMenu menu;                        // built the first time it is opened

    private AbilityWheel(String id, String title) {
        this.id = id;
        this.title = title;
    }

    public static AbilityWheel create(String id, String title) {
        return new AbilityWheel(id, title);
    }

    /** How far from the middle of the screen the icons sit, in GUI pixels (default 72). */
    public AbilityWheel radius(int pixels) {
        this.radius = pixels;
        return this;
    }

    public AbilityWheel entry(String key, String icon, String name, int sequence, int cost, int cooldownTicks, Cast cast) {
        entries.add(new Entry(key, icon, name, sequence, cost, Math.max(0, cooldownTicks), cast));
        return this;
    }

    /** While this says true, the entry added last skips the cost and cooldown checks. */
    public AbilityWheel free(Predicate<ServerPlayer> when) {
        if (!entries.isEmpty()) entries.get(entries.size() - 1).free = when;
        return this;
    }

    /** Runs after every cast that went off with DONE (a stack, a sound, leaving a form...). */
    /**
     * false: quickCast ignores cooldowns. It casts the last pick even while that pick is cooling down, and starts
     * no cooldown of its own (the cost is still paid). Picking from the wheel keeps its cooldowns as before.
     */
    public AbilityWheel quickCastCooldown(boolean on) {
        this.quickCastCooldown = on;
        return this;
    }

    /**
     * A sneak recast (quickCast) costs this many times the entry's own cost: quickCastCost(2f) = twice as much.
     * Pair it with quickCastCooldown(false) for "no cooldown, but double the price". Picking from the wheel pays
     * the normal cost.
     */
    public AbilityWheel quickCastCost(float multiplier) {
        this.quickCastCost = Math.max(0f, multiplier);
        return this;
    }

    public AbilityWheel onCast(CastDone action) {
        this.onCast = action;
        return this;
    }

    // =========================
    // Using it
    // =========================

    /** Opens the wheel. Cast unbound (stolen), it casts the last pick (or a random entry) at once instead. */
    public void open(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || entries.isEmpty()) return;
        if (Beyonder.isCastingUnbound(player)) {
            Entry entry = lastPick(player);
            if (entry == null) entry = entries.get(player.getRandom().nextInt(entries.size()));
            cast(serverPlayer, entry);
            return;
        }
        menu().open(serverPlayer);
    }

    /**
     * Casts the last pick again (with no cooldown when quickCastCooldown(false)). With no last pick the wheel
     * opens; a last pick above the player's Sequence is refused with a message.
     */
    public void quickCast(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        Entry entry = lastPick(player);
        if (entry == null || !unlocked(player, entry)) {
            open(player);
            return;
        }
        cast(serverPlayer, entry, !quickCastCooldown, quickCastCost);
    }

    /** Casts one entry with every check, its cooldown included. */
    public Result cast(ServerPlayer player, Entry entry) {
        return cast(player, entry, false);
    }

    /**
     * Casts one entry: checks its Sequence, cooldown and cost, runs it, and on DONE takes the cost, starts the
     * cooldown and runs onCast. The entry becomes the last pick if it went off.
     * ignoreCooldown: the cooldown is neither checked nor started (the cost is still checked and taken).
     */
    public Result cast(ServerPlayer player, Entry entry, boolean ignoreCooldown) {
        return cast(player, entry, ignoreCooldown, 1f);
    }

    /** The same, paying 'costMultiplier' times the entry's cost (a sneak recast with quickCastCost). */
    public Result cast(ServerPlayer player, Entry entry, boolean ignoreCooldown, float costMultiplier) {
        if (!(player.level() instanceof ServerLevel sl)) return Result.REFUSED;
        int cost = Math.round(entry.cost * Math.max(0f, costMultiplier));
        if (!unlocked(player, entry)) {
            player.sendSystemMessage(Component.literal(entry.name + " needs Sequence " + entry.sequence + "."));
            return Result.REFUSED;
        }
        boolean free = entry.isFree(player);
        if (!free) {
            int wait = ignoreCooldown ? 0 : cooldownLeft(player, entry);
            if (wait > 0) {
                player.sendSystemMessage(Component.literal(entry.name + " is ready again in " + seconds(wait) + "."));
                return Result.REFUSED;
            }
            if (SoulCore.getSpirituality(player) < cost) {
                Spells.paySpirituality(player, cost);            // takes nothing, only says so
                return Result.REFUSED;
            }
        }

        Result result = entry.cast.cast(player, sl);
        if (result == null || result == Result.REFUSED) return Result.REFUSED;

        player.getPersistentData().putString(id + "_last", entry.key);
        if (result == Result.DONE) {
            if (!free) {
                Spells.paySpirituality(player, cost);
                if (entry.cooldown > 0 && !ignoreCooldown) player.getPersistentData().putLong(id + "_ready_" + entry.key, sl.getGameTime() + entry.cooldown);
            }
            if (onCast != null) onCast.run(player, sl, entry);
        }
        return result;
    }

    // =========================
    // Asking about it
    // =========================

    /** The entry the player last cast from this wheel, or null. */
    public Entry lastPick(Player player) {
        return entry(player.getPersistentData().getStringOr(id + "_last", ""));
    }

    /** The entry with this key, or null. */
    public Entry entry(String key) {
        for (Entry entry : entries) {
            if (entry.key.equals(key)) return entry;
        }
        return null;
    }

    public List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    /** The entries the player's Sequence has unlocked. */
    public List<Entry> unlocked(Player player) {
        List<Entry> open = new ArrayList<>();
        for (Entry entry : entries) {
            if (unlocked(player, entry)) open.add(entry);
        }
        return open;
    }

    public boolean unlocked(Player player, Entry entry) {
        return Beyonder.hasSequence(player, entry.sequence);
    }

    /** Ticks until the entry can be cast again (0 = ready). */
    public int cooldownLeft(Player player, Entry entry) {
        long ready = player.getPersistentData().getLongOr(id + "_ready_" + entry.key, 0L);
        long left = ready - player.level().getGameTime();
        if (left > entry.cooldown) {                          // a save from another world's clock: forget it
            player.getPersistentData().remove(id + "_ready_" + entry.key);
            return 0;
        }
        return (int) Math.max(0, left);
    }

    /** Forgets the cooldowns and the last pick (a respec, a new Pathway). */
    public void reset(Player player) {
        CompoundTag data = player.getPersistentData();
        data.remove(id + "_last");
        for (Entry entry : entries) data.remove(id + "_ready_" + entry.key);
    }

    // =========================
    // Inside
    // =========================

    private AbilityMenu menu() {
        if (menu != null) return menu;
        AbilityMenu built = AbilityMenu.create(id).title(title).ring(radius);
        for (Entry entry : entries) {
            built.option(entry.icon, entry.name, 0, 0, (player, sl) -> cast(player, entry))
                    .details(player -> details(player, entry))
                    .lockedWhen(player -> !unlocked(player, entry));
        }
        menu = built;
        return menu;
    }

    /** "Sequence 5" while locked, "1250 · 2s" (cost · cooldown) when ready, "ready in 3s" while cooling down. */
    private String details(ServerPlayer player, Entry entry) {
        if (!unlocked(player, entry)) return "Sequence " + entry.sequence;
        if (entry.isFree(player)) return "free now";
        int wait = cooldownLeft(player, entry);
        if (wait > 0) return "ready in " + seconds(wait);
        return entry.cooldown > 0 ? entry.cost + " · " + seconds(entry.cooldown) : String.valueOf(entry.cost);
    }

    private static String seconds(int ticks) {
        int tenths = (int) Math.ceil(ticks / 2.0);                // 20 ticks = 1 s, rounded up to a tenth
        return tenths % 10 == 0 ? (tenths / 10) + "s" : (tenths / 10) + "." + (tenths % 10) + "s";
    }
}






















