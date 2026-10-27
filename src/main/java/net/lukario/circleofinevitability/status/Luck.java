package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.fate.LuckBalance;
import net.minecraft.world.entity.LivingEntity;

/** Persistent, server-owned signed reserve, independent of vanilla Luck and Misfortune buildup. */
public final class Luck {
    private static final String BALANCE = "snake_luck_balance";
    private Luck() {}
    public static double balance(LivingEntity entity) {
        double saved = entity.getPersistentData().getDoubleOr(BALANCE, 0d);
        return Double.isFinite(saved) ? LuckBalance.bounded(saved) : 0;
    }
    public static void set(LivingEntity entity, double value) {
        double bounded = LuckBalance.bounded(value);
        if (!entity.level().isClientSide()) entity.getPersistentData().putDouble(BALANCE, bounded);
    }
    /** Add either sign to any living target, including enemies; opposite signs cancel. */
    public static void add(LivingEntity entity, double amount) {
        if (!Double.isFinite(amount)) throw new IllegalArgumentException("Luck must be finite");
        set(entity, balance(entity) + amount);
    }
    public static void spendRoll(LivingEntity entity, int tier) {
        set(entity, LuckBalance.afterRoll(balance(entity), tier));
    }
}
