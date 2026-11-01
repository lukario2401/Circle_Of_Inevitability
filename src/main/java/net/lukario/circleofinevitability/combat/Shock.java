package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god.ThunderGodFx;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Marks;
import net.lukario.circleofinevitability.status.Rage;
import net.lukario.circleofinevitability.status.Soaked;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import java.util.Comparator;

/** One shared lightning rule; secondary arcs never recursively create more arcs. */
public final class Shock {
    private static final String IMMUNITY = "shock_paralysis_immunity";
    private Shock() {}
    public static boolean metal(LivingEntity target) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var item = target.getItemBySlot(slot).getItem();
            if (item == Items.COPPER_HELMET || item == Items.COPPER_CHESTPLATE || item == Items.COPPER_LEGGINGS || item == Items.COPPER_BOOTS
                    || item == Items.IRON_HELMET || item == Items.IRON_CHESTPLATE || item == Items.IRON_LEGGINGS || item == Items.IRON_BOOTS
                    || item == Items.GOLDEN_HELMET || item == Items.GOLDEN_CHESTPLATE || item == Items.GOLDEN_LEGGINGS || item == Items.GOLDEN_BOOTS
                    || item == Items.CHAINMAIL_HELMET || item == Items.CHAINMAIL_CHESTPLATE || item == Items.CHAINMAIL_LEGGINGS || item == Items.CHAINMAIL_BOOTS
                    || item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS) return true;
        }
        return false;
    }
    public static void strike(LivingEntity caster, LivingEntity target, float damage) {
        strike(caster, target, damage, 10, Rage.cast(caster));
    }
    public static void strike(LivingEntity caster, LivingEntity target, float damage, int paralysis, Rage.Cast cast) {
        if (!(caster.level() instanceof ServerLevel sl) || target.level() != sl || !Spells.isEnemy(caster, target)) return;
        boolean conducts = Soaked.isSoaked(target) || metal(target);
        hit(caster, target, damage, paralysis, cast);
        if (conducts) {
            for (LivingEntity next : Spells.enemiesAround(caster, sl, target.getBoundingBox().getCenter(), 5).stream()
                    .filter(e -> e != target && Soaked.isSoaked(e))
                    .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(target))).limit(3).toList()) {
                ThunderGodFx.bolt(sl, target.getBoundingBox().getCenter(), next.getBoundingBox().getCenter(), .05f, 5);
                hit(caster, next, damage * .5f, paralysis, cast);
            }
        }
    }
    private static void hit(LivingEntity caster, LivingEntity target, float damage, int paralysis, Rage.Cast cast) {
        float actual = damage * (Soaked.isSoaked(target) ? 1.4f : 1f) * (Spells.isEvil(target) ? 1.5f : 1f);
        float before = target.getHealth() + target.getAbsorptionAmount();
        cast.strike(target, actual);
        if (target.getHealth() + target.getAbsorptionAmount() >= before || !target.isAlive()) return;
        if (Marks.count(target, IMMUNITY) == 0) {
            Root.apply(target, Math.max(1, Math.min(20, paralysis)), RootRestriction.MOVE, RootRestriction.JUMP);
            Marks.add(target, IMMUNITY, 1, 60);
        }
    }
}
