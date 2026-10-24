package net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.minecraft.world.entity.LivingEntity;

/**
 * A Beyonder mob's spirituality: what its abilities spend (MobCasting). Its bar is BeyonderMobRules.SPIRITUALITY_SHARE
 * of a player's of the same Sequence, and it comes back at REGEN_SHARE of a player's speed (the table in Beyonder).
 * A new Beyonder mob starts full. Saved with the mob (persistent data, circle_of_inevitability_mob_spirituality).
 *
 *   MobSpirituality.get(mob) / max(mob)
 *   MobSpirituality.spend(mob, 250)       // false (and nothing taken) when it has less
 *   MobSpirituality.add(mob, 100)         // an ability that gives some back
 */
public final class MobSpirituality {

    private static final String KEY = "circle_of_inevitability_mob_spirituality";

    private MobSpirituality() {}

    /** Its whole bar (0 when it is no Beyonder). */
    public static float max(LivingEntity mob) {
        int sequence = BeyonderMobs.sequence(mob);
        if (sequence < 0) return 0f;
        return Beyonder.maxSpirituality(sequence) * Math.max(0f, BeyonderMobRules.SPIRITUALITY_SHARE);
    }

    /** What it gets back each tick. */
    public static float regenPerTick(LivingEntity mob) {
        int sequence = BeyonderMobs.sequence(mob);
        if (sequence < 0) return 0f;
        return Beyonder.spiritualityRegenPerTick(sequence) * Math.max(0f, BeyonderMobRules.REGEN_SHARE);
    }

    /** What it has now. A Beyonder mob never given any yet (made before this existed) has a full bar. */
    public static float get(LivingEntity mob) {
        if (!BeyonderMobs.isBeyonder(mob)) return 0f;
        float max = max(mob);
        if (!mob.getPersistentData().contains(KEY)) return max;
        return Math.min(max, mob.getPersistentData().getFloatOr(KEY, max));
    }

    /** Sets it (kept within 0..max). */
    public static void set(LivingEntity mob, float value) {
        if (!BeyonderMobs.isBeyonder(mob)) return;
        mob.getPersistentData().putFloat(KEY, Math.max(0f, Math.min(max(mob), value)));
    }

    public static boolean has(LivingEntity mob, float cost) {
        return cost <= 0f || get(mob) >= cost;
    }

    /** Takes 'cost'. False, and nothing taken, when it has less. */
    public static boolean spend(LivingEntity mob, float cost) {
        if (cost <= 0f) return true;
        float now = get(mob);
        if (now < cost) return false;
        set(mob, now - cost);
        return true;
    }

    public static void add(LivingEntity mob, float amount) {
        if (amount > 0f) set(mob, get(mob) + amount);
    }

    /** A full bar (a new Beyonder mob, a new Sequence). */
    public static void fill(LivingEntity mob) {
        set(mob, max(mob));
    }

    /** 'ticks' worth of getting it back (MobCasting calls this each time the mob thinks). */
    public static void regen(LivingEntity mob, int ticks) {
        float max = max(mob);
        float now = get(mob);
        if (now >= max) return;
        set(mob, now + regenPerTick(mob) * ticks);
    }

    /** It is no Beyonder any more: forgotten. */
    public static void clear(LivingEntity mob) {
        mob.getPersistentData().remove(KEY);
    }
}
