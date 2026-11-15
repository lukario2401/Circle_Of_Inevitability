package net.lukario.circleofinevitability.status;

import net.minecraft.world.entity.LivingEntity;

/** Caster-owned Abyss Seeds; lazy decay is saved with the target by Marks. */
public final class Depravity {
    public static final String KIND = "filthy_monarch_depravity";
    private Depravity() {}
    public static int plant(LivingEntity caster, LivingEntity target, int amount, int dropTicks) {
        if (!target.isAlive()) return 0;
        return Marks.add(target, caster, KIND, Math.max(0, amount), 5, dropTicks);
    }
    public static int count(LivingEntity caster, LivingEntity target) { return Marks.count(target, caster, KIND); }
    public static int spend(LivingEntity caster, LivingEntity target) { return Marks.take(target, caster, KIND); }
    public static void clear(LivingEntity caster, LivingEntity target) { Marks.clear(target, caster, KIND); }
    public static float bonus(LivingEntity caster, LivingEntity target) { return 1 + .04f * count(caster, target); }
}
