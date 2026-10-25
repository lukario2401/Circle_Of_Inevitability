package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobs;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.combat.Cooldowns;
import net.lukario.circleofinevitability.combat.Echo;
import net.lukario.circleofinevitability.combat.HudTimer;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Key Of Stars, key 4: Record (Scribe, Sequence 6). docs/KEY_OF_STARS.md, "Keys" and "Pathway systems".
 *
 *   Record   for a while you watch: the first Beyonder ability another caster uses near you and in your sight is
 *            copied into a free Recorded slot, if the roll succeeds ("I came, I saw, I recorded."). A Beyonder mob
 *            you look at when you press it is read at once (BeyonderMobs.castOf: mobs cast nothing themselves)
 *   Replay   (sneak) casts the oldest Recorded ability at once, free, at the strength it was written down with, and
 *            empties its slot
 *
 * Quick Study (the Scribe's passive) is here too: a glint on every ability cast near you that Record could copy, and
 * the chance shown beside the hotbar.
 *
 * What was cast comes from combat/Casts (every Pathway's keys and wheels are told there); the copy is Casts.recast
 * (free, no Sequence gate, no cooldown) inside Echo.weaker (its hits at the recorded strength). The slots live in
 * memory: they survive a relog and a death, not a server restart. Only what combat/Casts calls copyable can be
 * written down (not an ability that works only for its own Pathway).
 */
public final class KeyOfStarsRecord {

    private KeyOfStarsRecord() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int RECORD_SEQUENCE = 6;
    static final int RECORD_COST = 2450;
    static final int RECORD_COOLDOWN = 300;
    static final int RECORD_TICKS = 160;                // how long you watch
    static final double RECORD_RANGE = 32.0;            // a cast this close, in your sight
    static final int SLOTS = 3;
    static final int MORE_SLOTS_SEQUENCE = 5;           // Traveler: 4 slots
    static final int MORE_SLOTS = 4;
    static final int MOST_SLOTS_SEQUENCE = 4;           // Secrets Sorcerer: 5 slots, and Angel-level powers
    static final int MOST_SLOTS = 5;
    static final int ANGEL_LEVEL = 2;                   // an ability of this Sequence or stronger is Angel-level
    static final float ANGEL_CHANCE = 0.15f;            // what a Secrets Sorcerer or Wanderer gets at one
    static final int SURE_SEQUENCE = 2;                 // Planeswalker: no roll at your Sequence or weaker
    // the roll and the strength (docs: "Chances and strength of a record"); gap = its Sequence minus yours
    static final float CHANCE = 0.95f;                  // gap 0 or more
    static final float CHANCE_PER_STEP = 0.30f;         // less for every Sequence stronger than you
    static final int NEVER_GAP = -4;                    // this much stronger: never
    static final float STRENGTH = 0.5f;                 // replayed at STRENGTH + STRENGTH_PER_GAP * gap...
    static final float STRENGTH_PER_GAP = 0.1f;
    static final float STRENGTH_MIN = 0.3f;             // ...never less, never more than 1
    // sneak: Replay
    static final int REPLAY_COOLDOWN = 20;
    // Quick Study (passive)
    static final int QUICK_STUDY_REST = 20;             // one glint at most this often
    static final int QUICK_STUDY_SHOWN = 40;

    // =========================
    // Names and what is kept
    // =========================
    static final String WATCH_UNTIL = "key_of_stars_record_until";
    static final String STUDY_READY = "key_of_stars_quick_study_ready";
    static final String RECORD_KEY = "key_of_stars_record";               // combat/Cooldowns
    static final String REPLAY_KEY = "key_of_stars_replay";
    static final String STUDY_CLOCK = "ks_quick_study";

    /** One Recorded Power: what was cast, how strongly it comes back, and whether it was an Angel's. */
    public record Recorded(Casts.Cast cast, float strength, boolean angel) {}

    private static final Map<UUID, List<Recorded>> PAGES = new HashMap<>();

    // =========================
    // Ability 4 - Record
    // Normal: Record. For RECORD_TICKS you watch: the first ability another Beyonder casts within RECORD_RANGE and in
    //         your sight is rolled for (chance) and, if it succeeds, written into a free slot (SLOTS; Traveler
    //         MORE_SLOTS; Secrets Sorcerer MOST_SLOTS). Watching ends with that first one, either way. Cast while
    //         looking at a Beyonder mob, one of its abilities (up to its Sequence) is rolled for at once.
    // Sneak:  Replay. The oldest Recorded Power is cast at once, toward your crosshair, free, at its strength; its
    //         slot empties. One that does not go off (no target) stays.
    // =========================
    public static void keyOfStarsRecord(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!KeyOfStars.ready(player, bypassClassCheck, RECORD_SEQUENCE) || !(player instanceof ServerPlayer sp)) return;
        if (player.isShiftKeyDown()) replay(sp, sl);
        else record(sp, sl);
    }

    private static void record(ServerPlayer player, ServerLevel sl) {
        if (isWatching(player)) {
            player.sendSystemMessage(Component.literal("You are already watching."));
            return;
        }
        if (!Cooldowns.ready(player, RECORD_KEY, "Record")) return;
        if (pages(player).size() >= slots(player)) {
            player.sendSystemMessage(Component.literal("Every page is written. Replay one first (sneak)."));
            return; // nothing spent
        }
        if (!KeyOfStars.pay(player, RECORD_COST)) return;
        Cooldowns.start(player, RECORD_KEY, RECORD_COOLDOWN);
        player.getPersistentData().putLong(WATCH_UNTIL, sl.getGameTime() + RECORD_TICKS);
        KeyOfStarsFx.record(sl, player, RECORD_TICKS, recordTag(player));

        // mobs cast nothing: a Beyonder mob you look at (blasphemy_slate/beyonder_mobs) is written down at once, one of
        // its Pathway's abilities up to its Sequence, with the same roll
        LivingEntity aimed = Spells.aimEnemy(player, sl, RECORD_RANGE);
        Casts.Cast fromMob = BeyonderMobs.castOf(aimed, sl, player.getRandom());
        if (fromMob != null) write(player, sl, aimed, fromMob, chance(player, fromMob.sequence()));
    }

    private static void replay(ServerPlayer player, ServerLevel sl) {
        List<Recorded> pages = pages(player);
        if (pages.isEmpty()) {
            player.sendSystemMessage(Component.literal("Nothing is written in your book."));
            return;
        }
        if (!Cooldowns.ready(player, REPLAY_KEY, "Replay")) return;
        Recorded oldest = pages.get(0);
        KeyOfStarsFx.replay(sl, player);
        boolean went = Echo.weaker(player, oldest.strength(), () -> Casts.recast(player, oldest.cast()));
        if (!went) {
            player.sendSystemMessage(Component.literal(oldest.cast().name() + " did not go off. It stays written."));
            return;
        }
        pages.remove(0);
        if (pages.isEmpty()) PAGES.remove(player.getUUID());
        Cooldowns.start(player, REPLAY_KEY, REPLAY_COOLDOWN);
        showPages(player);
        Recorded next = pages.isEmpty() ? null : pages.get(0);
        player.sendSystemMessage(Component.literal("Replayed " + oldest.cast().name() + "."
                + (next == null ? "" : " Next: " + next.cast().name() + ".")));
    }

    // =========================
    // Seeing a cast (combat/Casts tells every one)
    // =========================

    /** Every cast that went off: a watching Scribe near it records it; one not watching gets Quick Study's glint. */
    static void seen(LivingEntity caster, ServerLevel sl, Casts.Cast cast) {
        if (!cast.copyable()) return;
        for (ServerPlayer watcher : sl.players()) {
            if (watcher == caster || !KeyOfStars.isKeyOfStars(watcher, RECORD_SEQUENCE)) continue;
            if (watcher.distanceTo(caster) > RECORD_RANGE || !watcher.hasLineOfSight(caster)) continue;
            float chance = chance(watcher, cast.sequence());
            if (isWatching(watcher)) {
                write(watcher, sl, caster, cast, chance);
                continue;
            }
            long now = sl.getGameTime();
            if (watcher.getPersistentData().getLongOr(STUDY_READY, 0L) > now) continue;
            watcher.getPersistentData().putLong(STUDY_READY, now + QUICK_STUDY_REST);
            KeyOfStarsFx.quickStudy(watcher, caster);
            HudTimer.show(watcher, STUDY_CLOCK, cast.name() + ": " + Math.round(chance * 100) + "%", QUICK_STUDY_SHOWN, 0xE8ECFC);
        }
    }

    /** The first cast seen while watching: rolled for, written down if it succeeds. Watching ends either way. */
    private static void write(ServerPlayer player, ServerLevel sl, LivingEntity caster, Casts.Cast cast, float chance) {
        endWatching(player, sl);
        if (chance <= 0f || player.getRandom().nextFloat() >= chance) {
            KeyOfStarsFx.recordFailed(sl, player);
            player.sendSystemMessage(Component.literal(cast.name() + " was too much to write down."));
            return;
        }
        List<Recorded> pages = PAGES.computeIfAbsent(player.getUUID(), id -> new ArrayList<>());
        if (pages.size() >= slots(player)) return;                    // (filled while watching: checked when it began)
        boolean angel = cast.sequence() <= ANGEL_LEVEL;
        pages.add(new Recorded(cast, strength(player, cast.sequence()), angel));
        KeyOfStarsFx.recorded(sl, player, caster.getEyePosition(), angel);
        showPages(player);
        player.sendSystemMessage(Component.literal("I came, I saw, I recorded: " + cast.name() + "."));
    }

    /** The roll for an ability of 'sequence' (docs: "Chances and strength of a record"). */
    static float chance(Player recorder, int sequence) {
        int mine = SoulCore.getSequence(recorder);
        if (sequence <= ANGEL_LEVEL && mine > ANGEL_LEVEL) {
            return Beyonder.hasSequence(recorder, MOST_SLOTS_SEQUENCE) ? ANGEL_CHANCE : 0f;
        }
        int gap = sequence - mine;
        if (gap >= 0) return Beyonder.hasSequence(recorder, SURE_SEQUENCE) ? 1f : CHANCE;
        if (gap <= NEVER_GAP) return 0f;
        return Math.max(0f, CHANCE + CHANCE_PER_STEP * gap);
    }

    /** How strongly an ability of 'sequence' comes back when it is replayed. */
    static float strength(Player recorder, int sequence) {
        int gap = sequence - SoulCore.getSequence(recorder);
        return Math.max(STRENGTH_MIN, Math.min(1f, STRENGTH + STRENGTH_PER_GAP * gap));
    }

    // =========================
    // State
    // =========================

    static boolean isWatching(Player player) {
        return player.getPersistentData().getLongOr(WATCH_UNTIL, 0L) > player.level().getGameTime();
    }

    private static void endWatching(Player player, ServerLevel sl) {
        player.getPersistentData().remove(WATCH_UNTIL);
        KeyOfStarsFx.recordEnd(sl, player, recordTag(player));
    }

    /** Once a second for a Key Of Stars: a watch that saw nothing ends. */
    static void tick(Player player, ServerLevel sl) {
        long until = player.getPersistentData().getLongOr(WATCH_UNTIL, 0L);
        if (until == 0L || until > sl.getGameTime()) return;
        endWatching(player, sl);
        player.sendSystemMessage(Component.literal("Nothing was cast for you to record."));
    }

    static int slots(Player player) {
        if (Beyonder.hasSequence(player, MOST_SLOTS_SEQUENCE)) return MOST_SLOTS;
        if (Beyonder.hasSequence(player, MORE_SLOTS_SEQUENCE)) return MORE_SLOTS;
        return SLOTS;
    }

    /** The Recorded Powers, oldest first (a live list). */
    static List<Recorded> pages(Player player) {
        return PAGES.getOrDefault(player.getUUID(), new ArrayList<>());
    }

    /** The pages right of the hotbar, drawn again (after every change and a relog). */
    static void showPages(Player player) {
        List<Recorded> pages = pages(player);
        boolean[] angel = new boolean[pages.size()];
        for (int i = 0; i < angel.length; i++) angel[i] = pages.get(i).angel();
        KeyOfStarsFx.recordedPages(player, angel);
    }

    /** A relog or a respawn: the watch is over; the pages are drawn again. */
    static void clearState(Player player) {
        player.getPersistentData().remove(WATCH_UNTIL);
        player.getPersistentData().remove(STUDY_READY);
        if (!SoulCore.getPathway(player).equals(KeyOfStars.PATHWAY)) {
            if (PAGES.remove(player.getUUID()) != null) KeyOfStarsFx.recordedPagesHide(player);   // a Pathway left behind
            return;
        }
        showPages(player);
    }

    private static String recordTag(Player player) {
        return "ks_record_" + player.getId();
    }
}
