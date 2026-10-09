package net.lukario.frogerealm.status;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

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
 * To show them, see SpellFx.overHead(...): a ring of small models over the entity's head, one for each mark.
 *
 * Saved with the entity. Nothing has to tick for this: the time a mark runs out is written down, not counted.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class Marks {

    private static final String TAG = "forgerealm_marks";       // name -> {Count, Until}
    private static final String COUNT = "Count";
    private static final String UNTIL = "Until";                // game time

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
        data.getCompound(TAG).put(name, mark);
        return now;
    }

    /** How many marks of this name the entity carries right now. */
    public static int count(Entity entity, String name) {
        CompoundTag mark = markTag(entity, name);
        if (mark == null || entity.level().getGameTime() >= mark.getLong(UNTIL)) return 0;
        return mark.getInt(COUNT);
    }

    /** Ticks until the marks of this name are forgotten (0 = it carries none). */
    public static int ticksLeft(Entity entity, String name) {
        CompoundTag mark = markTag(entity, name);
        if (mark == null) return 0;
        return (int) Math.max(0, mark.getLong(UNTIL) - entity.level().getGameTime());
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
        CompoundTag all = data.getCompound(TAG);
        all.remove(name);
        if (all.isEmpty()) data.remove(TAG);
    }

    // =========================
    // Internals
    // =========================

    private static CompoundTag markTag(Entity entity, String name) {
        if (entity == null) return null;
        CompoundTag data = entity.getPersistentData();
        if (!data.contains(TAG)) return null;
        CompoundTag all = data.getCompound(TAG);
        return all.contains(name) ? all.getCompound(name) : null;
    }
}
