package net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.minecraft.world.entity.LivingEntity;

/**
 * A Beyonder mob's spirituality: what its abilities spend (MobCasting). Its bar is BeyonderMobRules.SPIRITUALITY_SHARE
 * of a player's of the same Sequence, and it comes back at REGEN_SHARE of a player's speed (the table in Beyonder);
 * but the bar is never smaller than SPIRITUALITY_FLOOR_CASTS casts of its cheapest ability, and such a bar fills in
 * FLOOR_REFILL_TICKS at most.
 * A new Beyonder mob starts full. Saved with the mob (persistent data, circle_of_inevitability_mob_spirituality).
 *
 *   MobSpirituality.get(mob) / max(mob)
 *   MobSpirituality.spend(mob, 250)       // false (and nothing taken) when it has less
 *   MobSpirituality.add(mob, 100)         // an ability that gives some back
 */
public final class MobSpirituality {

    private static final String KEY = "circle_of_inevitability_mob_spirituality";

    private MobSpirituality() {}

    /**
     * Its whole bar (0 when it is no Beyonder): SPIRITUALITY_SHARE of a player's, but never less than
     * SPIRITUALITY_FLOOR_CASTS casts of the cheapest ability it knows.
     */
    public static float max(LivingEntity mob) {
        int sequence = BeyonderMobs.sequence(mob);
        if (sequence < 0) return 0f;
        return Math.max(shareOfPlayers(sequence), floor(mob, sequence));
    }

    /** What it gets back each tick: REGEN_SHARE of a player's, and fast enough to fill a floor-raised bar in FLOOR_REFILL_TICKS. */
    public static float regenPerTick(LivingEntity mob) {
        int sequence = BeyonderMobs.sequence(mob);
        if (sequence < 0) return 0f;
        float regen = Beyonder.spiritualityRegenPerTick(sequence) * Math.max(0f, BeyonderMobRules.REGEN_SHARE);
        float floor = floor(mob, sequence);
        if (floor > shareOfPlayers(sequence) && BeyonderMobRules.FLOOR_REFILL_TICKS > 0) {
            regen = Math.max(regen, floor / BeyonderMobRules.FLOOR_REFILL_TICKS);
        }
        return regen;
    }

    private static float shareOfPlayers(int sequence) {
        return Beyonder.maxSpirituality(sequence) * Math.max(0f, BeyonderMobRules.SPIRITUALITY_SHARE);
    }

    /** SPIRITUALITY_FLOOR_CASTS times the cheapest ability its Pathway and Sequence know (0 if it knows none). */
    private static float floor(LivingEntity mob, int sequence) {
        if (BeyonderMobRules.SPIRITUALITY_FLOOR_CASTS <= 0f) return 0f;
        return MobAbilities.cheapestCost(BeyonderMobs.pathway(mob), sequence) * BeyonderMobRules.SPIRITUALITY_FLOOR_CASTS;
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

    // ---------- what a cast has paid for already

    /** mob -> what its running cast still covers (MobCasting pays an ability's cost() itself). Not saved. */
    private static final java.util.Map<java.util.UUID, Float> PREPAID = new java.util.HashMap<>();

    /**
     * While its ability runs (MobCasting): the player code it runs may pay for itself (Spells.paySpirituality, which
     * comes here for a mob); up to 'amount' of that is the ability's cost(), which MobCasting takes, so it is free.
     */
    public static void prepaid(LivingEntity mob, float amount) {
        PREPAID.put(mob.getUUID(), Math.max(0f, amount));
    }

    /** What its running cast still covers (0 when none runs). */
    public static float prepaidLeft(LivingEntity mob) {
        Float covered = PREPAID.get(mob.getUUID());
        return covered == null ? 0f : covered;
    }

    /** The cast ended: what it covered is gone. */
    public static void endPrepaid(LivingEntity mob) {
        PREPAID.remove(mob.getUUID());
    }

    /**
     * What the player code of an ability pays (Spells.paySpirituality for a mob): first out of what its running cast
     * covers, the rest from its bar (an upkeep, a dearer form). False, and nothing taken, when it cannot.
     */
    public static boolean pay(LivingEntity mob, float cost) {
        if (cost <= 0f) return true;
        Float covered = PREPAID.get(mob.getUUID());
        float free = covered == null ? 0f : covered;
        if (cost <= free) {
            PREPAID.put(mob.getUUID(), free - cost);
            return true;
        }
        if (!spend(mob, cost - free)) return false;
        if (covered != null) PREPAID.put(mob.getUUID(), 0f);
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
