package net.lukario.circleofinevitability.combat;

import net.minecraft.world.entity.LivingEntity;

/** A shared heal entry point. LivingEntity.heal posts Forge's heal event, preserving NoHeal,
 * Decree, Inversion and other healing rules. Never replace this with setHealth. */
public final class Healing {
    private Healing() {}
    public static float apply(LivingEntity target, float amount) {
        if (!target.isAlive() || target.level().isClientSide() || !Float.isFinite(amount) || amount <= 0) return 0;
        float before = target.getHealth();
        target.heal(amount);
        return Math.max(0, target.getHealth() - before);
    }
}
