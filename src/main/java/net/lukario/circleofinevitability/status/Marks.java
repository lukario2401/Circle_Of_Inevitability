package net.lukario.circleofinevitability.status;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Set;

/**
 * Marks: a number an entity carries for a while. "How often was it hit by my spikes", "how many of my seeds
 * are in it", "how many charges did this player build up". Works from any class/ability, on players and mobs:
 *
 *   int hits = Marks.add(target, "my_class_hits", 10, 300);   // one more, 10 at most. All of them last 15 seconds again
 *   Marks.add(target, "my_class_hits", 3, 10, 300);           // three more at once
 *   Marks.count(target, "my_class_hits")                      // how many it carries now (0 once they are forgotten)
 *   Marks.ticksLeft(target, "my_class_hits")
 *   int paid = Marks.take(target, "my_class_hits");           // how many it carried, and they are gone: for finishers
 *   Marks.clear(target, "my_class_hits");
 *
 * Every name is its own counter, so start the name with your class. Each add makes ALL the marks of that name
 * last their full time again; when the time runs out without a new one, they are all forgotten together.
 *
 * ---------- Marks one player puts on an enemy, that drop one at a time ----------
 * Give the owner (the player who marks) and how many ticks it takes for ONE mark to wear off. Each player has his own
 * count on the same mob, and other players' marks never count for you. Nothing marks by itself: your ability calls add.
 *
 *   Marks.add(target, player, "conqueror_cull", 5, 60);       // one more of yours, 5 at most; one wears off every 3 s
 *   Marks.add(target, player, "conqueror_cull", 2, 5, 60);    // two more at once
 *   int marks = Marks.count(target, player, "conqueror_cull");
 *   int marks = Marks.take(target, player, "conqueror_cull"); // how many of yours, and they are gone: for finishers
 *   Marks.clear(target, player, "conqueror_cull");
 *   Marks.ticksToNextDrop(target, player, "conqueror_cull")   // until it loses one;  ticksLeft(...) until all are gone
 *
 * Every add starts the wear-off clock again, so marks only drop while you are not marking it. A finisher that does
 * more the more marks there are:
 *
 *   switch (Marks.take(target, player, "conqueror_cull")) {
 *       case 0 -> ...;              // not marked
 *       case 1, 2 -> ...;
 *       default -> ...;             // 3 or more
 *   }
 *
 * To show them, see SpellFx.overHead(...): a ring of small models over the entity's head, one for each mark.
 *
 * Saved with the entity. Nothing has to tick for this: the time a mark runs out is written down, not counted.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class Marks {

    private static final String TAG = "circle_of_inevitability_marks";       // name -> {Count, Until}
    private static final String COUNT = "Count";
    private static final String UNTIL = "Until";                // game time
    private static final String SINCE = "Since";                // game time of the last add (marks that drop one at a time)
    private static final String EVERY = "Every";                // ticks for one of them to wear off

    /** Advances typed mark timers without changing their count, source or interval. */
    public static void age(Entity entity, int ticks) {
        if (ticks <= 0) return;
        CompoundTag all = entity.getPersistentData().getCompoundOrEmpty(TAG);
        for (String key : java.util.Set.copyOf(all.keySet())) {
            CompoundTag mark = all.getCompoundOrEmpty(key);
            if (mark.contains(UNTIL)) mark.putLong(UNTIL, mark.getLongOr(UNTIL, 0L) - ticks);
            if (mark.contains(SINCE)) mark.putLong(SINCE, mark.getLongOr(SINCE, 0L) - ticks);
        }
        forgetWornOff(entity, all);
    }

    private Marks() {}

    // =========================
    // API
    // =========================

    /** One more mark, 'max' at most. All marks of this name last 'ticks' ticks from now. Returns how many it carries now. */
    public static int add(LivingEntity entity, String name, int max, int ticks) {
        return add(entity, name, 1, max, ticks);
    }

    /** 'amount' more marks (a negative amount takes some away), 'max' at most. Returns how many it carries now. */
    public static int add(LivingEntity entity, String name, int amount, int max, int ticks) {
        if (entity.level().isClientSide()) return count(entity, name);

        int now = Math.min(max, count(entity, name) + amount);
        if (now <= 0 || ticks <= 0) {
            clear(entity, name);
            return 0;
        }

        CompoundTag data = entity.getPersistentData();
        if (!data.contains(TAG)) data.put(TAG, new CompoundTag());

        CompoundTag mark = new CompoundTag();
        mark.putInt(COUNT, now);
        mark.putLong(UNTIL, entity.level().getGameTime() + ticks);
        data.getCompoundOrEmpty(TAG).put(name, mark);
        return now;
    }

    /** How many marks of this name the entity carries right now. */
    public static int count(Entity entity, String name) {
        CompoundTag mark = markTag(entity, name);
        if (mark == null) return 0;
        long now = entity.level().getGameTime();
        int every = mark.getIntOr(EVERY, 0);
        if (every > 0) {                                          // one wears off every 'every' ticks
            long dropped = Math.max(0L, now - mark.getLongOr(SINCE, now)) / every;
            return (int) Math.max(0L, mark.getIntOr(COUNT, 0) - dropped);
        }
        if (now >= mark.getLongOr(UNTIL, 0L)) return 0;
        return mark.getIntOr(COUNT, 0);
    }

    /** Ticks until the marks of this name are forgotten (0 = it carries none). */
    public static int ticksLeft(Entity entity, String name) {
        CompoundTag mark = markTag(entity, name);
        if (mark == null) return 0;
        int every = mark.getIntOr(EVERY, 0);
        long end = every > 0 ? mark.getLongOr(SINCE, 0L) + (long) mark.getIntOr(COUNT, 0) * every : mark.getLongOr(UNTIL, 0L);
        return (int) Math.max(0, end - entity.level().getGameTime());
    }

    /** Takes all marks of this name off the entity and returns how many there were. */
    public static int take(LivingEntity entity, String name) {
        int carried = count(entity, name);
        clear(entity, name);
        return carried;
    }

    public static void clear(LivingEntity entity, String name) {
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(TAG)) return;
        CompoundTag all = data.getCompoundOrEmpty(TAG);
        all.remove(name);
        if (all.isEmpty()) data.remove(TAG);
    }

    // =========================
    // API: one player's marks, dropping one at a time
    // =========================

    /** One more of 'owner's marks, 'max' at most. One wears off every 'dropEvery' ticks without a new add. Returns his count now. */
    public static int add(LivingEntity entity, Entity owner, String name, int max, int dropEvery) {
        return add(entity, owner, name, 1, max, dropEvery);
    }

    /** 'amount' more of 'owner's marks (negative takes some away), 'max' at most. Returns his count now. */
    public static int add(LivingEntity entity, Entity owner, String name, int amount, int max, int dropEvery) {
        String key = keyOf(owner, name);
        if (entity.level().isClientSide()) return count(entity, key);

        int now = Math.min(max, count(entity, key) + amount);
        if (now <= 0 || dropEvery <= 0) {
            clear(entity, key);
            return 0;
        }

        CompoundTag data = entity.getPersistentData();
        if (!data.contains(TAG)) data.put(TAG, new CompoundTag());
        CompoundTag all = data.getCompoundOrEmpty(TAG);
        forgetWornOff(entity, all);                              // so marks of players long gone do not pile up

        CompoundTag mark = new CompoundTag();
        mark.putInt(COUNT, now);
        mark.putLong(SINCE, entity.level().getGameTime());
        mark.putInt(EVERY, dropEvery);
        all.put(key, mark);
        return now;
    }

    /** How many of 'owner's marks of this name the entity carries right now. */
    public static int count(Entity entity, Entity owner, String name) {
        return count(entity, keyOf(owner, name));
    }

    /** Ticks until all of 'owner's marks are gone, if nobody adds one (0 = none left). */
    public static int ticksLeft(Entity entity, Entity owner, String name) {
        return ticksLeft(entity, keyOf(owner, name));
    }

    /** Ticks until 'owner's marks go one down (0 = none left). */
    public static int ticksToNextDrop(Entity entity, Entity owner, String name) {
        CompoundTag mark = markTag(entity, keyOf(owner, name));
        if (mark == null || count(entity, keyOf(owner, name)) <= 0) return 0;
        int every = mark.getIntOr(EVERY, 0);
        if (every <= 0) return ticksLeft(entity, keyOf(owner, name));
        long passed = Math.max(0L, entity.level().getGameTime() - mark.getLongOr(SINCE, 0L));
        return (int) (every - passed % every);
    }

    /** Takes all of 'owner's marks of this name off the entity and returns how many there were. */
    public static int take(LivingEntity entity, Entity owner, String name) {
        return take(entity, keyOf(owner, name));
    }

    public static void clear(LivingEntity entity, Entity owner, String name) {
        clear(entity, keyOf(owner, name));
    }

    /** The counter of 'name' that belongs to 'owner' (null owner: the shared one). */
    private static String keyOf(Entity owner, String name) {
        return owner == null ? name : name + "@" + owner.getUUID();
    }

    // =========================
    // Internals
    // =========================

    /** Drops the counters that have nothing left in them. */
    private static void forgetWornOff(Entity entity, CompoundTag all) {
        for (String key : Set.copyOf(all.keySet())) {
            if (count(entity, key) <= 0) all.remove(key);
        }
    }

    private static CompoundTag markTag(Entity entity, String name) {
        if (entity == null) return null;
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(TAG)) return null;
        CompoundTag all = data.getCompoundOrEmpty(TAG);
        return all.contains(name) ? all.getCompoundOrEmpty(name) : null;
    }
}
