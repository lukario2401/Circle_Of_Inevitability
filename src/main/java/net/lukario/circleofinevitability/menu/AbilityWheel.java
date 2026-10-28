package net.lukario.circleofinevitability.menu;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.combat.Cooldowns;
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
import java.util.function.ToDoubleFunction;

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
 * - .costBy((player, cost) -> ...) after an entry: its cost worked out at the moment of casting (more for every
 *   mausoleum already standing, half against a marked enemy). 'cost' is what it would cost otherwise.
 * - .quickCastCostBy(player -> ...): the recast price read from the player (3x at Sequence 9, 1x at Sequence 5...).
 * - .noCooldownsWhen(player -> ...): while it says true for a player, none of the wheel's entries has a cooldown.
 * - .cooldownBy((player, ticks) -> ...): every entry's cooldown worked out when it starts (30% shorter while blessed).
 * - .notCopyable() after an entry: Record, Replication and the echoes (combat/Casts) never copy it.
 *
 * Cast through Beyonder.castUnbound (a stolen ability) there is no screen: the last pick is cast at once,
 * or a random entry if there is none.
 * Saved on the player: the last pick and the cooldowns (in game time), so both survive a relog.
 * Every pick that went off is told to combat/Casts (Record, Replication and the echoes copy it from there).
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

    /** A cooldown worked out when it starts: 'ticks' is the entry's own cooldown. */
    @FunctionalInterface
    public interface CooldownRule {
        int cooldown(ServerPlayer player, int ticks);
    }

    /** An entry's cost worked out when it is cast: 'cost' is what it costs otherwise (already times a recast price). */
    @FunctionalInterface
    public interface CostRule {
        int cost(ServerPlayer player, int cost);
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
        private CostRule costBy;
        private boolean copyable = true;
        private String description = "";

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
        /** False for an entry that only works for its own Pathway: combat/Casts never copies it (see notCopyable). */
        public boolean copyable() { return copyable; }

        private boolean isFree(ServerPlayer player) {
            return free != null && free.test(player);
        }

        /** What it costs this player now, at 'multiplier' times its own cost (a recast's price). */
        private int costFor(ServerPlayer player, float multiplier) {
            int base = Math.round(cost * Math.max(0f, multiplier));
            return costBy == null ? base : Math.max(0, costBy.cost(player, base));
        }
    }

    private final String id;
    private final String title;
    private int radius = 72;
    private final List<Entry> entries = new ArrayList<>();
    private CastDone onCast;
    private boolean quickCastCooldown = true;        // false: a sneak recast neither waits for nor starts a cooldown
    private float quickCastCost = 1f;                // a sneak recast costs this many times the entry's cost
    private ToDoubleFunction<Player> quickCastCostBy; // ...or this, read from the player (null: quickCastCost)
    private Predicate<Player> noCooldowns;           // true: no entry has a cooldown for this player
    private CooldownRule cooldownBy;                 // null: every entry's own cooldown
    private Predicate<ServerPlayer> castsWhileSilenced; // true: picks from the wheel go through a root's ABILITIES
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

    /** Hover text for the entry added last. */
    public AbilityWheel description(String text) {
        if (!entries.isEmpty()) entries.get(entries.size() - 1).description = text;
        return this;
    }

    /** While this says true, the entry added last skips the cost and cooldown checks. */
    public AbilityWheel free(Predicate<ServerPlayer> when) {
        if (!entries.isEmpty()) entries.get(entries.size() - 1).free = when;
        return this;
    }

    /**
     * The entry added last is never copied by another (combat/Casts: Record, Replication, the echoes): it needs its own
     * Pathway's state or tick, or copying it makes no sense (a copier copying).
     */
    public AbilityWheel notCopyable() {
        if (!entries.isEmpty()) entries.get(entries.size() - 1).copyable = false;
        return this;
    }

    /**
     * The entry added last costs what this says when it is cast: (player, cost) -> new cost. 'cost' is what it would
     * cost otherwise (already times a recast's price). Shown under its icon too.
     */
    public AbilityWheel costBy(CostRule rule) {
        if (!entries.isEmpty()) entries.get(entries.size() - 1).costBy = rule;
        return this;
    }

    /**
     * The price of a recast read from the player, instead of one fixed quickCastCost: player -> 3.0 at Sequence 9,
     * 1.0 from Sequence 5 (the Prince Of Abolition's wheels).
     */
    public AbilityWheel quickCastCostBy(ToDoubleFunction<Player> multiplier) {
        this.quickCastCostBy = multiplier;
        return this;
    }

    /** While this says true for a player, none of the entries has a cooldown for them, from the wheel or recast. */
    public AbilityWheel noCooldownsWhen(Predicate<Player> when) {
        this.noCooldowns = when;
        return this;
    }

    /**
     * Every entry's cooldown worked out when it starts: (player, ticks) -> new ticks, 'ticks' being the entry's own.
     * Shown under the icons too. For "your cooldowns are 30% shorter while this lasts".
     */
    public AbilityWheel cooldownBy(CooldownRule rule) {
        this.cooldownBy = rule;
        return this;
    }

    /** The cooldown this entry starts for this player now. */
    public int cooldownFor(ServerPlayer player, Entry entry) {
        int own = cooldownBy == null ? entry.cooldown : Math.max(0, Math.min(entry.cooldown, cooldownBy.cooldown(player, entry.cooldown)));
        return Cooldowns.scaled(player, own);                    // every Cooldowns.addScale rule (a Divine Kingdom's enemies)
    }

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

    /**
     * While this says true for a player, a pick from the wheel is not stopped by a root with ABILITIES (status/Silence):
     * spells that need no voice. Let the key through too: Silence.addCastsThrough((player, key) -> ...).
     */
    public AbilityWheel castsWhileSilenced(Predicate<ServerPlayer> when) {
        this.castsWhileSilenced = when;
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
        float multiplier = quickCastCostBy != null ? (float) quickCastCostBy.applyAsDouble(player) : quickCastCost;
        cast(serverPlayer, entry, !quickCastCooldown, multiplier);
    }

    /** True while none of the entries has a cooldown for this player (noCooldownsWhen). */
    public boolean noCooldowns(Player player) {
        return noCooldowns != null && noCooldowns.test(player);
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
        int cost = entry.costFor(player, costMultiplier);
        if (noCooldowns(player)) ignoreCooldown = true;
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
            if (SoulCore.getSpirituality(player) < Spells.costFor(player, cost)) {
                Spells.paySpirituality(player, cost);            // takes nothing, only says so
                return Result.REFUSED;
            }
        }

        boolean skipCooldown = ignoreCooldown;
        if (net.lukario.circleofinevitability.combat.HeldActions.defer(player, () -> {
            if (!net.lukario.circleofinevitability.root.Root.has(player, net.lukario.circleofinevitability.root.RootRestriction.ABILITIES))
                cast(player, entry, skipCooldown, costMultiplier);
        })) return Result.REFUSED;

        Result result = entry.cast.cast(player, sl);
        if (result == null || result == Result.REFUSED) return Result.REFUSED;

        player.getPersistentData().putString(id + "_last", entry.key);
        Casts.wheelCast(player, this, entry);                            // who cast what (combat/Casts: Record, echoes)
        if (result == Result.DONE) {
            if (!free) {
                Spells.paySpirituality(player, cost);
                int cooldown = cooldownFor(player, entry);
                if (cooldown > 0 && !ignoreCooldown) player.getPersistentData().putLong(id + "_ready_" + entry.key, sl.getGameTime() + cooldown);
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
        if (left > entry.cooldown * 4L + 20) {                // a save from another world's clock: forget it
            player.getPersistentData().remove(id + "_ready_" + entry.key);
            return 0;
        }
        return (int) Math.max(0, left);
    }

    /** That one entry is ready again now (a form that lets it fire without a pause, a refund). */
    public void resetCooldown(Player player, String key) {
        player.getPersistentData().remove(id + "_ready_" + key);
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
        if (castsWhileSilenced != null) built.castsWhileSilenced(castsWhileSilenced);
        for (Entry entry : entries) {
            built.option(entry.icon, entry.name, 0, 0, (player, sl) -> cast(player, entry))
                    .details(player -> details(player, entry))
                    .description(player -> entry.description)
                    .lockedWhen(player -> !unlocked(player, entry));
        }
        menu = built;
        return menu;
    }

    /** "Sequence 5" while locked, "1250 · 2s" (cost · cooldown) when ready, "ready in 3s" while cooling down. */
    private String details(ServerPlayer player, Entry entry) {
        if (!unlocked(player, entry)) return "Sequence " + entry.sequence;
        if (entry.isFree(player)) return "free now";
        int cost = Math.round(Spells.costFor(player, entry.costFor(player, 1f)));
        if (noCooldowns(player)) return String.valueOf(cost);
        int wait = cooldownLeft(player, entry);
        if (wait > 0) return "ready in " + seconds(wait);
        int cooldown = cooldownFor(player, entry);
        return cooldown > 0 ? cost + " · " + seconds(cooldown) : String.valueOf(cost);
    }

    private static String seconds(int ticks) {
        int tenths = (int) Math.ceil(ticks / 2.0);                // 20 ticks = 1 s, rounded up to a tenth
        return tenths % 10 == 0 ? (tenths / 10) + "s" : (tenths / 10) + "." + (tenths % 10) + "s";
    }
}
