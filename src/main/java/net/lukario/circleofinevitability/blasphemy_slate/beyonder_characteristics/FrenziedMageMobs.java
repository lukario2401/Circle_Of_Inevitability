package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Frenzied Mage abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 *
 * The Frenzied Mage's abilities are written for any caster (its caster a LivingEntity, its aim the caster's facing),
 * so a mob turns to its target and runs, once it has paid, the code a player's key runs after paying (spearThrow,
 * flameCast, iceSpikes, glacier, hammerFall, hammerWhirl, meteorCast, entropyCast, bladeOfAbolition, frenzy).
 *
 *   1  Spear (9), Spear Rain (9, sneak)        2  Fireball (8), Pyre (8, sneak)
 *   3  Ice Spikes (7), Glacier (7, sneak)       4  Hammerfall (6), Hammer Whirl (6, sneak: in melee)
 *   5  Meteor (5), Meteor Shower (5, sneak)     6  Entropy (4: where it aims), Entropy Round It (4, sneak)
 *   7  Blade of Abolition (3), Frenzy (3, sneak)
 */
public final class FrenziedMageMobs {

    private static final String P = "Frenzied Mage";

    private FrenziedMageMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        // ---- key 1: Spear / Spear Rain
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.NORMAL)
                .cost(FrenziedMage.SPEAR_COST).cooldown(40).weight(14).range(FrenziedMage.SPEAR_RANGE).sight()
                .cast(MobSpells.aimedCore(FrenziedMage::spearThrow)));
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.SNEAK).named("Spear Rain")
                .cost(FrenziedMage.SPEAR_COST).cooldown(120).range(FrenziedMage.SPEAR_RAIN_RANGE).sight()
                .cast(MobSpells.aimedCore(FrenziedMage::spearRain)));

        // ---- key 2: Fireball / Pyre
        MobAbilities.register(MobAbility.of(P, 2, MobAbility.NORMAL).named("Fireball")
                .cost(FrenziedMage.FLAME_COST).cooldown(80).weight(14).range(FrenziedMage.FIREBALL_RANGE).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > FrenziedMage.FIREBALL_RADIUS + 1.0)
                .cast(MobSpells.aimedCore((mob, sl) -> FrenziedMage.flameCast(mob, sl, false))));
        MobAbilities.register(MobAbility.of(P, 2, MobAbility.SNEAK).named("Pyre")
                .cost(FrenziedMage.FLAME_COST).cooldown(FrenziedMage.PYRE_TICKS + 100).range(FrenziedMage.PYRE_RANGE).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > FrenziedMage.PYRE_RADIUS + 1.0)
                .cast(MobSpells.aimedCore((mob, sl) -> FrenziedMage.flameCast(mob, sl, true))));

        // ---- key 3: Ice Spikes / Glacier
        MobAbilities.register(MobAbility.of(P, 3, MobAbility.NORMAL).named("Ice Spikes")
                .cost(FrenziedMage.FROST_COST).cooldown(120).range(12.0).sight()
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    List<Vec3> spots = FrenziedMage.spikeSpots(mob, sl);
                    if (spots.isEmpty()) return false;
                    FrenziedMage.iceSpikes(mob, sl, spots);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 3, MobAbility.SNEAK).named("Glacier")
                .cost(FrenziedMage.FROST_COST).cooldown(FrenziedMage.GLACIER_FREEZE_TICKS + 60).range(FrenziedMage.GLACIER_RANGE).sight()
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    FrenziedMage.glacier(mob, sl, target);
                    return true;
                }));

        // ---- key 4: Hammerfall / Hammer Whirl
        MobAbilities.register(MobAbility.of(P, 4, MobAbility.NORMAL).named("Hammerfall")
                .cost(FrenziedMage.HAMMER_COST).cooldown(140).range(FrenziedMage.HAMMERFALL_RANGE).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > FrenziedMage.HAMMERFALL_RADIUS)
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    Vec3 ground = FrenziedMage.aimGround(mob, sl, FrenziedMage.HAMMERFALL_RANGE);
                    FrenziedMage.hammerFall(mob, sl, ground, FrenziedMage.HAMMERFALL_SIZE, FrenziedMage.HAMMERFALL_RADIUS,
                            FrenziedMage.HAMMERFALL_DAMAGE, FrenziedMage.HAMMERFALL_WARNING);
                    FrenziedMage.sound(sl, mob.position(), SoundEvents.BEACON_POWER_SELECT, 1f, 0.5f);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 4, MobAbility.SNEAK).named("Hammer Whirl")
                .cost(FrenziedMage.HAMMER_COST).cooldown(140).weight(14).range(FrenziedMage.WHIRL_RADIUS)
                .cast(MobSpells.aimedCore(FrenziedMage::hammerWhirl)));

        // ---- key 5: Meteor / Meteor Shower
        MobAbilities.register(MobAbility.of(P, 5, MobAbility.NORMAL).named("Meteor")
                .cost(FrenziedMage.METEOR_COST).cooldown(300).range(FrenziedMage.METEOR_RANGE).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > FrenziedMage.METEOR_RADIUS + 1.0)
                .cast(MobSpells.aimedCore((mob, sl) -> FrenziedMage.meteorCast(mob, sl, false))));
        MobAbilities.register(MobAbility.of(P, 5, MobAbility.SNEAK).named("Meteor Shower")
                .cost(FrenziedMage.METEOR_COST).cooldown(300).range(FrenziedMage.METEOR_RANGE).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > FrenziedMage.METEOR_RADIUS + 1.0)
                .cast(MobSpells.aimedCore((mob, sl) -> FrenziedMage.meteorCast(mob, sl, true))));

        // ---- key 6: Entropy (where it aims) / round itself
        MobAbilities.register(MobAbility.of(P, 6, MobAbility.NORMAL).named("Entropy")
                .cost(FrenziedMage.ENTROPY_COST).cooldown(FrenziedMage.ENTROPY_TICKS + 200).range(FrenziedMage.ENTROPY_RANGE).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > FrenziedMage.ENTROPY_PULL_RADIUS)
                .cast(MobSpells.aimedCore((mob, sl) -> FrenziedMage.entropyCast(mob, sl, false))));
        MobAbilities.register(MobAbility.of(P, 6, MobAbility.SNEAK).named("Entropy Round It")
                .cost(FrenziedMage.ENTROPY_COST).cooldown(FrenziedMage.ENTROPY_TICKS + 200).range(FrenziedMage.ENTROPY_PULL_RADIUS)
                .cast(MobSpells.aimedCore((mob, sl) -> FrenziedMage.entropyCast(mob, sl, true))));

        // ---- key 7: Blade of Abolition / Frenzy
        MobAbilities.register(MobAbility.of(P, 7, MobAbility.NORMAL).named("Blade of Abolition")
                .cost(FrenziedMage.ABOLITION_COST).cooldown(400).range(FrenziedMage.ABOLITION_RANGE).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > FrenziedMage.ABOLITION_RADIUS)
                .cast(MobSpells.aimedCore((mob, sl) ->
                        FrenziedMage.bladeOfAbolition(mob, sl, FrenziedMage.aimGround(mob, sl, FrenziedMage.ABOLITION_RANGE)))));
        MobAbilities.register(MobAbility.of(P, 7, MobAbility.SNEAK).named("Frenzy")
                .cost(FrenziedMage.ABOLITION_COST).cooldown(FrenziedMage.FRENZY_TICKS + 400).range(FrenziedMage.FRENZY_RANGE)
                .cast(MobSpells.aimedCore(FrenziedMage::frenzy)));
    }
}
