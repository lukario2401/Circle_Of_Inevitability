package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragonMarks.Mark;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobForm;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Teleport;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Substitute;
import net.lukario.circleofinevitability.status.WeaponCoat;
import net.lukario.circleofinevitability.status.Weight;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Star Dragon abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 *
 * Most Star Dragon abilities are written for any caster (their caster is a LivingEntity, their aim the caster's
 * facing), so a mob casts the very same code a player does: MobSpells.aimedEntry turns it to its target and runs the
 * wheel entry, MobSpells.aimedCore runs a key's ability once it is paid for (StarDragon.starChart, StarDragonVoid.
 * passThroughVoid, StarDragonRays.firePiercingRay, StarDragonTides.surge, StarDragonSky.grazeStar and loosenNuclei).
 * Only the forms are a mob's own (beyonder_mobs/MobForm: status/Form is a player's), and Fate Glimpse (the dodges
 * through status/Substitute). The cooldowns, and when a mob thinks a cast worth it, are the mob's.
 *
 *   1  Star Chart (9), Fate Glimpse (8, sneak)
 *   2  Star Sacrifice (7): Gravity Abnormality, Electromagnetic Attraction, Cosmic Void, Weakening Ray, Sunlight Ray (5)
 *   3  Void Passage (6: to its target from afar), Piercing Ray (6, sneak)
 *   4  Tidal Surge (5), Scaled Form (5, sneak; the Heavy Body from 4)
 *   5  Heavybringer (4): Heavybringing, Fate Convergence, Heavy Sphere, Series of Rays
 *   6  Star Grazing (3), Fission Pulse (3, sneak)
 *   7  Cosmos (2): Fusion Cluster, Radiant Angel Form, Gravitational Time Dilation (1), Planetfall (1), Star Dragon Form
 *      (1: no asteroid circles it), Neutron Star (0), Singularity (0), Quantum Uncertainty (0)
 *
 * Left out: Remote Sight (what it shows is for a player's eyes).
 */
public final class StarDragonMobs {

    // ---------- mob cooldowns (ticks)
    private static final int CHART_MOB_COOLDOWN = 400;
    private static final int GLIMPSE_MOB_COOLDOWN = 500;
    private static final int PASSAGE_MOB_COOLDOWN = 240;
    private static final int PIERCING_MOB_COOLDOWN = 160;
    private static final int SURGE_MOB_COOLDOWN = 240;
    private static final int SCALES_MOB_COOLDOWN = 1200;
    private static final int SKY_MOB_COOLDOWN = 400;
    private static final double PASSAGE_MOB_FAR = 8.0;          // it takes the void to a target further than this
    private static final String SCALES_COAT = "star_dragon_mob_scales";

    private StarDragonMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        String P = StarDragon.PATHWAY;

        // ---- key 1: Astronomy
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.NORMAL)                // Star Chart
                .cost(StarDragon.CHART_COST).cooldown(CHART_MOB_COOLDOWN).weight(6)
                .range(StarDragon.CHART_RANGE).sight()
                .when((mob, target, sl) -> !StarDragonMarks.fromCaster(target, Mark.CHARTED, mob))
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    StarDragon.starChart(mob, sl, target);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.SNEAK).named("Fate Glimpse")
                .sequence(StarDragon.GLIMPSE_SEQUENCE)
                .cost(StarDragon.GLIMPSE_COST).cooldown(GLIMPSE_MOB_COOLDOWN)
                .range(6.0)
                .when((mob, target, sl) -> !Substitute.isArmed(mob))
                .cast(StarDragonMobs::fateGlimpse));

        // ---- key 2: the Star Sacrifice wheel
        entry(2, "gravity_abnormality", "Gravity Abnormality", StarDragonGravity.ABNORMALITY_SEQUENCE,
                StarDragonGravity.ABNORMALITY_COST, StarDragonGravity.ABNORMALITY_COOLDOWN, StarDragonGravity.ABNORMALITY_RANGE,
                StarDragonGravity::gravityAbnormality);
        entry(2, "electromagnetic_attraction", "Electromagnetic Attraction", StarDragonGravity.MAGNET_SEQUENCE,
                StarDragonGravity.MAGNET_COST, StarDragonGravity.MAGNET_COOLDOWN, StarDragonGravity.MAGNET_RANGE,
                StarDragonGravity::electromagneticAttraction);
        entry(2, "cosmic_void", "Cosmic Void", StarDragonVoid.VOID_SEQUENCE,
                StarDragonVoid.VOID_COST, StarDragonVoid.VOID_COOLDOWN, StarDragonVoid.VOID_RANGE,
                StarDragonVoid::cosmicVoid);
        entry(2, "weakening_ray", "Weakening Ray", StarDragonRays.WEAKENING_SEQUENCE,
                StarDragonRays.WEAKENING_COST, StarDragonRays.WEAKENING_COOLDOWN, StarDragonRays.WEAKENING_RANGE,
                StarDragonRays::weakeningRay);
        entry(2, "sunlight_ray", "Sunlight Ray", StarDragonRays.SUNLIGHT_SEQUENCE,
                StarDragonRays.SUNLIGHT_COST, StarDragonRays.SUNLIGHT_COOLDOWN, StarDragonRays.SUNLIGHT_RANGE,
                StarDragonRays::sunlightRay);

        // ---- key 3: Navigator
        MobAbilities.register(MobAbility.of(P, 3, MobAbility.NORMAL)                // Void Passage
                .cost(StarDragonVoid.PASSAGE_COST).cooldown(PASSAGE_MOB_COOLDOWN)
                .range(StarDragonVoid.PASSAGE_RANGE)
                .when((mob, target, sl) -> mob.distanceTo(target) > PASSAGE_MOB_FAR
                        && !Root.has(mob, RootRestriction.TELEPORT) && !Root.has(mob, RootRestriction.MOVE))
                .cast(StarDragonMobs::voidPassage));
        MobAbilities.register(MobAbility.of(P, 3, MobAbility.SNEAK).named("Piercing Ray")
                .cost(StarDragonRays.PIERCING_COST).cooldown(PIERCING_MOB_COOLDOWN)
                .range(StarDragonRays.PIERCING_RANGE).sight()
                .cast(MobSpells.aimedCore(StarDragonRays::firePiercingRay)));

        // ---- key 4: Tidal Scholar
        MobAbilities.register(MobAbility.of(P, 4, MobAbility.NORMAL)                // Tidal Surge
                .cost(StarDragonTides.SURGE_COST).cooldown(SURGE_MOB_COOLDOWN)
                .range(8.0)
                .cast(MobSpells.aimedCore(StarDragonTides::surge)));
        MobAbilities.register(MobAbility.of(P, 4, MobAbility.SNEAK).named("Scaled Form")
                .cost(StarDragonTides.SCALES_COST).cooldown(SCALES_MOB_COOLDOWN)
                .range(8.0)
                .when((mob, target, sl) -> MobForm.current(mob) == null)
                .cast(StarDragonMobs::scaledForm));

        // ---- key 5: the Heavybringer wheel
        entry(5, "heavybringing", "Heavybringing", StarDragonGravity.HEAVYBRINGING_SEQUENCE,
                StarDragonGravity.HEAVYBRINGING_COST, StarDragonGravity.HEAVYBRINGING_COOLDOWN, StarDragonGravity.HEAVYBRINGING_RADIUS,
                StarDragonGravity::heavybringing);
        entry(5, "fate_convergence", "Fate Convergence", StarDragonVoid.FATE_SEQUENCE,
                StarDragonVoid.FATE_COST, StarDragonVoid.FATE_COOLDOWN, StarDragonVoid.FATE_RADIUS,
                StarDragonVoid::fateConvergence);
        entry(5, "heavy_sphere", "Heavy Sphere", StarDragonGravity.SPHERE_SEQUENCE,
                StarDragonGravity.SPHERE_COST, StarDragonGravity.SPHERE_COOLDOWN, StarDragonGravity.SPHERE_RANGE,
                StarDragonGravity::heavySphere);
        entry(5, "series_of_rays", "Series of Rays", StarDragonRays.SERIES_SEQUENCE,
                StarDragonRays.SERIES_COST, StarDragonRays.SERIES_COOLDOWN, StarDragonRays.SERIES_RANGE,
                StarDragonRays::seriesOfRays);

        // ---- key 6: Star Shepherd
        MobAbilities.register(MobAbility.of(P, 6, MobAbility.NORMAL)                // Star Grazing
                .cost(StarDragonSky.GRAZE_COST).cooldown(Math.max(SKY_MOB_COOLDOWN, StarDragonSky.GRAZE_COOLDOWN))
                .range(StarDragonSky.GRAZE_RANGE).sight()
                .cast(MobSpells.aimedCore(StarDragonSky::grazeStar)));
        MobAbilities.register(MobAbility.of(P, 6, MobAbility.SNEAK).named("Fission Pulse")
                .cost(StarDragonSky.FISSION_COST).cooldown(Math.max(SKY_MOB_COOLDOWN, StarDragonSky.FISSION_COOLDOWN))
                .range(StarDragonSky.FISSION_RANGE).sight()
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    StarDragonSky.loosenNuclei(mob, sl, target);
                    return true;
                }));

        // ---- key 7: the Cosmos wheel
        entry(7, "fusion_cluster", "Fusion Cluster", StarDragonSky.FUSION_SEQUENCE,
                StarDragonSky.FUSION_COST, StarDragonSky.FUSION_COOLDOWN, StarDragonSky.FUSION_RANGE,
                StarDragonSky::fusionCluster);
        MobAbilities.register(MobAbility.of(P, 7, MobAbility.NORMAL).entry("radiant_angel_form").named("Radiant Angel Form")
                .sequence(StarDragonForms.ANGEL_SEQUENCE)
                .cost(StarDragonForms.ANGEL_COST).cooldown(StarDragonForms.ANGEL_COOLDOWN)
                .range(StarDragonForms.ANGEL_GLOW * 1.5)
                .when((mob, target, sl) -> MobForm.current(mob) == null)
                .cast(StarDragonMobs::radiantAngelForm));
        entry(7, "time_dilation", "Gravitational Time Dilation", StarDragonGravity.DILATION_SEQUENCE,
                StarDragonGravity.DILATION_COST, StarDragonGravity.DILATION_COOLDOWN, StarDragonGravity.DILATION_RANGE,
                StarDragonGravity::timeDilation);
        entry(7, "planetfall", "Planetfall", StarDragonSky.PLANET_SEQUENCE,
                StarDragonSky.PLANET_COST, StarDragonSky.PLANET_COOLDOWN, StarDragonSky.PLANET_RANGE,
                StarDragonSky::planetfall);
        MobAbilities.register(MobAbility.of(P, 7, MobAbility.NORMAL).entry("star_dragon_form").named("Star Dragon Form")
                .sequence(StarDragonForms.DRAGON_SEQUENCE)
                .cost(StarDragonForms.DRAGON_COST).cooldown(StarDragonForms.DRAGON_COOLDOWN)
                .range(24.0)
                .when((mob, target, sl) -> !StarDragonForms.isDragon(mob))
                .cast(StarDragonMobs::starDragonForm));
        entry(7, "neutron_star", "Neutron Star", StarDragonGravity.NEUTRON_SEQUENCE,
                StarDragonGravity.NEUTRON_COST, StarDragonGravity.NEUTRON_COOLDOWN, StarDragonGravity.NEUTRON_RADIUS * 2,
                StarDragonGravity::neutronStar);
        MobAbilities.register(MobAbility.of(P, 7, MobAbility.NORMAL).entry("singularity").named("Singularity")
                .sequence(StarDragonGravity.SINGULARITY_SEQUENCE)
                .cost(StarDragonGravity.SINGULARITY_COST).cooldown(StarDragonGravity.SINGULARITY_COOLDOWN).weight(4)
                .range(StarDragonGravity.SINGULARITY_RANGE).sight()
                .when((mob, target, sl) -> MobSpells.hurt(mob, 0.35f))                 // the price is everything: a last resort
                .cast(MobSpells.aimedEntry(StarDragonGravity::singularity)));
        entry(7, "quantum_uncertainty", "Quantum Uncertainty", StarDragonVoid.QUANTUM_SEQUENCE,
                StarDragonVoid.QUANTUM_COST, StarDragonVoid.QUANTUM_COOLDOWN, StarDragonVoid.QUANTUM_RADIUS,
                StarDragonVoid::quantumUncertainty);
    }

    /** One wheel entry written for any caster: the mob faces its target and casts it as a player does. */
    private static void entry(int key, String entry, String name, int sequence, int cost, int cooldown, double range,
                              MobSpells.Entry cast) {
        MobAbilities.register(MobAbility.of(StarDragon.PATHWAY, key, MobAbility.NORMAL).entry(entry).named(name)
                .sequence(sequence).cost(cost).cooldown(Math.max(100, cooldown)).range(range).sight()
                .cast(MobSpells.aimedEntry(cast)));
    }

    // =========================
    // Fate Glimpse: the first GLIMPSE_DODGES attacks aimed at it within GLIMPSE_TICKS go wide, and it slips aside
    // =========================

    private static boolean fateGlimpse(Mob mob, LivingEntity target, ServerLevel sl) {
        String tag = "sd_mob_glimpse_" + mob.getId();
        StarDragonFx.fateGlimpse(sl, mob, StarDragon.GLIMPSE_TICKS, tag);
        Substitute.arm(mob, StarDragon.GLIMPSE_TICKS, StarDragon.GLIMPSE_DODGES, (saved, source, amount, level) -> {
            Vec3 from = mob.position();
            Vec3 side = Spells.rightOf(mob).scale(level.getRandom().nextBoolean() ? StarDragon.GLIMPSE_STEP : -StarDragon.GLIMPSE_STEP);
            Vec3 to = from.add(side);
            if (!Spells.fits(mob, level, to)) to = from.subtract(side);
            if (Spells.fits(mob, level, to) && !Root.has(mob, RootRestriction.MOVE)) mob.teleportTo(to.x, to.y, to.z);
            else to = from;
            StarDragonFx.glimpseDodge(level, mob, from, to);
            if (Substitute.charges(mob) <= 0) StarDragonFx.fateGlimpseEnd(level, mob, tag);
        });
        Later.run(sl, StarDragon.GLIMPSE_TICKS, () -> StarDragonFx.fateGlimpseEnd(sl, mob, tag));
        return true;
    }

    // =========================
    // Void Passage: through the void to its target
    // =========================

    private static boolean voidPassage(Mob mob, LivingEntity target, ServerLevel sl) {
        Vec3 to = Teleport.standNear(mob, sl, target.position(), 2.5);
        if (to == null || to.distanceToSqr(mob.position()) < 4.0) return false;
        MobSpells.face(mob, target);
        StarDragonVoid.passThroughVoid(mob, sl, to);
        return true;
    }

    // =========================
    // The forms (beyonder_mobs/MobForm)
    // =========================

    /** Scaled Form: Strength II, Resistance II (III), more reach, and its blows make what they hit Heavy. The Heavy Body from 4. */
    private static boolean scaledForm(Mob mob, LivingEntity target, ServerLevel sl) {
        boolean heavy = MobSpells.hasSequence(mob, StarDragonTides.HEAVY_BODY_SEQUENCE);
        int ticks = heavy ? StarDragonTides.HEAVY_BODY_TICKS : StarDragonTides.SCALES_TICKS;
        String tag = "sd_mob_scales_" + mob.getId();
        MobForm form = MobForm.of(heavy ? StarDragonTides.HEAVY_BODY : StarDragonTides.SCALED)
                .lasts(ticks)
                .effect(MobEffects.STRENGTH, 1)
                .effect(MobEffects.RESISTANCE, heavy ? 2 : 1)
                .attribute(Attributes.ENTITY_INTERACTION_RANGE, heavy ? StarDragonTides.HEAVY_BODY_REACH : StarDragonTides.SCALES_REACH,
                        AttributeModifier.Operation.ADD_VALUE)
                .onEnd(one -> {
                    WeaponCoat.disarm(one, SCALES_COAT);
                    StarDragonFx.scaledFormEnd(sl, one, tag);
                });
        if (heavy) {
            form = form.attribute(Attributes.SCALE, StarDragonTides.HEAVY_BODY_SIZE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                    .every(20, StarDragonTides::heavyBodyTick);
        }
        form.take(mob);
        WeaponCoat.armAny(mob, SCALES_COAT, 10000, ticks, (attacker, struck, amount, level) -> {
            Weight.put(struck, 1, StarDragonTides.SCALED_HEAVY);
            return amount;
        });
        StarDragonFx.scaledForm(sl, mob, ticks, heavy, tag);
        return true;
    }

    /** Radiant Angel Form: Strength II, Resistance II, and its glow burns every enemy near it. */
    private static boolean radiantAngelForm(Mob mob, LivingEntity target, ServerLevel sl) {
        int ticks = StarDragonForms.ANGEL_TICKS;
        String tag = "sd_mob_radiant_angel_" + mob.getId();
        MobForm.of(StarDragonForms.ANGEL)
                .lasts(ticks)
                .effect(MobEffects.STRENGTH, 1)
                .effect(MobEffects.RESISTANCE, 1)
                .aftermath(StarDragonForms.ANGEL_AFTERMATH)
                .every(10, one -> StarDragonForms.glow(one, 1f))
                .onEnd(one -> StarDragonFx.radiantAngelEnd(sl, one, tag))
                .take(mob);
        StarDragonFx.radiantAngel(sl, mob, ticks, tag);
        return true;
    }

    /** Star Dragon Form: bigger, DRAGON_TOUGH less damage taken, thicker rays, the glow at half strength. */
    private static boolean starDragonForm(Mob mob, LivingEntity target, ServerLevel sl) {
        int ticks = StarDragonForms.DRAGON_TICKS;
        String tag = "sd_mob_star_dragon_" + mob.getId();
        MobForm.of(StarDragonForms.DRAGON)
                .lasts(ticks)
                .attribute(Attributes.SCALE, StarDragonForms.DRAGON_SIZE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                .cutsDamage(StarDragonForms.DRAGON_TOUGH)
                .aftermath(StarDragonForms.DRAGON_AFTERMATH)
                .every(10, one -> {
                    StarDragonForms.glow(one, StarDragonForms.DRAGON_GLOW);
                    if (one.tickCount % StarDragonForms.DRAGON_BEAT_EVERY < 10 && !one.onGround()) {
                        StarDragonFx.dragonBeat(sl, one, StarDragonForms.DRAGON_BEAT_EVERY, tag);
                    }
                })
                .onEnd(one -> StarDragonFx.starDragonFormEnd(sl, one, tag))
                .take(mob);
        StarDragonFx.starDragonForm(sl, mob, ticks, tag);
        return true;
    }
}
