package net.lukario.circleofinevitability.combat;

import net.minecraft.world.entity.LivingEntity;

/** A shared heal entry point. LivingEntity.heal posts Forge's heal event, preserving NoHeal,
 * Decree, Inversion and other healing rules. Never replace this with setHealth. */
public final class Healing {
    @FunctionalInterface public interface Scale { double factor(LivingEntity source,LivingEntity target); }
    private static final java.util.List<Scale> SCALES=new java.util.ArrayList<>();
    /** Register once at setup. Return 1 for unaffected targets; finite nonnegative values multiply. */
    public static void addScale(Scale rule) { SCALES.add(java.util.Objects.requireNonNull(rule)); }
    private Healing() {}
    public static float apply(LivingEntity target, float amount) {
        return apply(target,target,amount);
    }
    public static float apply(LivingEntity source,LivingEntity target,float amount) {
        for(Scale rule:java.util.List.copyOf(SCALES)) {
            double factor=rule.factor(source,target);
            if(!Double.isFinite(factor)||factor<0)return 0;
            amount*=(float)factor;
        }
        if (!target.isAlive() || target.level().isClientSide() || !Float.isFinite(amount) || amount <= 0) return 0;
        float before = target.getHealth();
        target.heal(amount);
        return Math.max(0, target.getHealth() - before);
    }
}
