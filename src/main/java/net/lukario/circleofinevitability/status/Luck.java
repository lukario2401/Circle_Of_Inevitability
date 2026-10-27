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
    public static double resistance(LivingEntity target) {
        return net.lukario.circleofinevitability.fate.Fate.applies(target)
                && net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore.getSequence((net.minecraft.world.entity.player.Player) target) <= 1 ? .5 : 1;
    }
    public static void bestow(LivingEntity source, LivingEntity target, double amount) {
        if (source.level() != target.level() || source.level().isClientSide()) return;
        add(target, amount * (source == target ? 1 : resistance(target)));
    }
    public static double transferable(LivingEntity source, LivingEntity target, double amount) {
        if (source == target || source.level() != target.level() || source.level().isClientSide()) return 0;
        return LuckBalance.transfer(balance(source), balance(target), amount, resistance(target));
    }
    public static double transfer(LivingEntity source, LivingEntity target, double amount) {
        double accepted = transferable(source, target, amount);
        if (accepted != 0) { add(source, -accepted); add(target, accepted); }
        return accepted;
    }
    public static void spendRoll(LivingEntity entity, int tier) {
        set(entity, LuckBalance.afterRoll(balance(entity), tier));
    }
}
