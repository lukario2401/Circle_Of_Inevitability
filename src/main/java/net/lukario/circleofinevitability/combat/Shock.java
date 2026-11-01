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
    private static final java.util.List<java.util.function.Predicate<LivingEntity>> IMMUNITIES=new java.util.ArrayList<>();
    private static final java.util.List<java.util.function.BiConsumer<ServerLevel,net.minecraft.world.phys.Vec3>> LANDINGS=new java.util.ArrayList<>();
    public static void addImmunity(java.util.function.Predicate<LivingEntity> rule){IMMUNITIES.add(rule);}
    public static boolean immune(LivingEntity e){return IMMUNITIES.stream().anyMatch(rule->rule.test(e));}
    public static void addLandingListener(java.util.function.BiConsumer<ServerLevel,net.minecraft.world.phys.Vec3> rule){LANDINGS.add(rule);}
    public static void boltLanded(ServerLevel sl,net.minecraft.world.phys.Vec3 at){for(var rule:LANDINGS)rule.accept(sl,at);}
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
        strike(caster,target,damage,paralysis,cast,false);
    }
    /** Friendly fire is explicit, for uncontrolled storms and Source of Wrath. */
    public static void strike(LivingEntity caster, LivingEntity target, float damage, int paralysis, Rage.Cast cast, boolean friendlyFire) {
        if (!(caster.level() instanceof ServerLevel sl) || target.level() != sl || target==caster || !target.isAlive()
                || target.isSpectator() || !friendlyFire&&!Spells.isEnemy(caster,target)) return;
        boltLanded(sl,target.position());
        boolean conducts = Soaked.isSoaked(target) || metal(target);
        hit(caster, target, damage, paralysis, cast);
        if (conducts) {
            for (LivingEntity next : (friendlyFire?Spells.livingAround(caster,sl,target.getBoundingBox().getCenter(),5):Spells.enemiesAround(caster, sl, target.getBoundingBox().getCenter(), 5)).stream()
                    .filter(e -> e != target && Soaked.isSoaked(e))
                    .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(target))).limit(3).toList()) {
                ThunderGodFx.bolt(sl, target.getBoundingBox().getCenter(), next.getBoundingBox().getCenter(), .05f, 5);
                hit(caster, next, damage * .5f, paralysis, cast);
            }
        }
    }
    private static void hit(LivingEntity caster, LivingEntity target, float damage, int paralysis, Rage.Cast cast) {
        if(immune(target))return;
        float actual = damage * (Soaked.isSoaked(target) ? 1.4f : 1f) * (Spells.isEvil(target) ? 1.5f : 1f);
        float before = target.getHealth() + target.getAbsorptionAmount();
        cast.strike(target, actual);
        if(net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god.ThunderGodHigher.presence(caster))
            net.lukario.circleofinevitability.status.Fear.apply(target,caster,40);
        if (target.getHealth() + target.getAbsorptionAmount() >= before || !target.isAlive()) return;
        if (paralysis > 0 && Marks.count(target, IMMUNITY) == 0) {
            Root.apply(target, Math.max(1, Math.min(20, paralysis)), RootRestriction.MOVE, RootRestriction.JUMP);
            Marks.add(target, IMMUNITY, 1, 60);
        }
    }
}
