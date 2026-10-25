package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragonFx.Status;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragonMarks.Mark;
import net.lukario.circleofinevitability.combat.Cooldowns;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Freeze;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Form;
import net.lukario.circleofinevitability.status.Marks;
import net.lukario.circleofinevitability.status.Unnoticed;
import net.lukario.circleofinevitability.status.Weight;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Star Dragon: the Condenser Pathway of Lord of the Mysteries (docs/STAR_DRAGON.md). Seven keys in the Attendant Of
 * Mysteries pattern, from Astronomy Aficionado (Sequence 9) up to Condenser (Sequence 0); every key has a normal and a
 * sneak cast:
 *
 *   1 Astronomy       (Astronomy Aficionado, Star Worshipper)  Star Chart: the enemy looked at is Charted
 *                                                              | sneak: Fate Glimpse, the next two aimed attacks dodged
 *   2 Star Sacrifice  (Star Sacrificer)    the wheel: Gravity Abnormality, Electromagnetic Attraction, Cosmic Void,
 *                                          Weakening Ray (7), Sunlight Ray (5)
 *   3 Navigator       (Navigator)          Void Passage: through the void to where you aim | sneak: Piercing Ray
 *   4 Tidal Scholar   (Tidal Scholar)      Tidal Surge: a crest that rolls forward          | sneak: Scaled Form
 *   5 Heavybringing   (Heavybringer)       the wheel: Heavybringing, Fate Convergence, Heavy Sphere, Series of Rays,
 *                                          Remote Sight
 *   6 Star Shepherd   (Star Shepherd)      Star Grazing: an asteroid out of the sky        | sneak: Fission Pulse
 *   7 Cosmos          (Radiant Angel)      the wheel: Fusion Cluster, Radiant Angel Form (2), Gravitational Time
 *                                          Dilation, Planetfall, Star Dragon Form (1), Neutron Star, Singularity,
 *                                          Quantum Uncertainty (0)
 *
 * Two rules carry it: Heaviness slows (status/Weight, combat/GravityField) and Starlight disorders (rays, voids, Fate:
 * StarDragonMarks). Every Star Dragon hit goes through hit(...) or rayHit(...), which add the Star Chart's calculated
 * shot and the void's manifestation by themselves. Every cast's cost goes through costFor / pay / afterCast (addCostRule
 * and addAfterCast take more: the Condenser's cheaper Fusion Cluster is one, a Starlight meter would be another). The
 * abilities live in StarDragonGravity, StarDragonRays, StarDragonVoid, StarDragonTides, StarDragonSky and
 * StarDragonForms beside this class; this one holds the keys, the wheels, the passives and what they share. Every look
 * and sound is in StarDragonFx (see /stardragonfx). Nothing here breaks blocks or lights real fire.
 */
public class StarDragon {

    public static final String PATHWAY = "Star Dragon";

    // =========================
    // Ability settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Ability 1 - Star Chart (Sequence 9)
    static final int CHART_SEQUENCE = 9;
    static final int CHART_COST = 250;
    static final int CHART_COOLDOWN = 1;
    static final double CHART_RANGE = 24.0;
    static final int CHART_TICKS = 200;                     // Charted for 10 seconds
    // sneak: Fate Glimpse (Sequence 8)
    static final int GLIMPSE_SEQUENCE = 8;
    static final int GLIMPSE_COST = 350;                    // the plan's 600: Sequence 8 holds only 400
    static final int GLIMPSE_COOLDOWN = 1;
    static final int GLIMPSE_TICKS = 200;
    static final int GLIMPSE_DODGES = 2;                    // the first two attacks aimed at you go wide
    static final double GLIMPSE_STEP = 1.2;                 // and you slip this far aside

    // Ability 2 - Star Sacrifice (the wheel, Sequence 7)
    static final int SACRIFICE_SEQUENCE = 7;

    // Ability 3 - Void Passage / Piercing Ray (Sequence 6), Ability 4 - Tidal Surge / Scaled Form (Sequence 5):
    // their numbers are in StarDragonVoid, StarDragonRays and StarDragonTides
    static final int NAVIGATOR_SEQUENCE = 6;
    static final int TIDAL_SEQUENCE = 5;

    // Ability 5 - Heavybringing (the wheel, Sequence 4)
    static final int HEAVYBRINGER_SEQUENCE = 4;

    // Ability 6 - Star Grazing / Fission Pulse (Sequence 3): their numbers are in StarDragonSky
    static final int SHEPHERD_SEQUENCE = 3;

    // Ability 7 - Cosmos (the wheel, Sequence 2): every entry's numbers are in the class it lives in
    static final int COSMOS_SEQUENCE = 2;

    // Passives (docs/STAR_DRAGON.md, "Passives"); the once-a-second ones run from SequencePassives
    static final int STARGAZING_SEQUENCE = 9;               // Stargazing: still this long under open sky...
    static final int STARGAZING_STILL = 60;
    static final float STARGAZING_REGEN = 0.3f;             // ...and spirituality comes back this much faster...
    static final float STARGAZING_NIGHT = 2f;               // ...twice that at night
    static final int INTUITION_SEQUENCE = 8;                // Spiritual Intuition: hunted unseen, a chime...
    static final int INTUITION_REST = 60;                   // ...at most this often
    static final int EMBRACE_SEQUENCE = 7;                  // Star's Embrace: two more hearts...
    static final double EMBRACE_HEARTS = 4.0;
    static final float EMBRACE_FALL = 0.5f;                 // ...and half fall damage
    static final double PASSAGES_RADIUS = 12.0;             // Sense of Passages (6): the hidden this close
    static final int SCALES_SEQUENCE = 5;                   // Starlight Scales: half knockback, fast swimming
    static final double SCALES_FOOTING = 0.5;
    static final int DENSE_SEQUENCE = 4;                    // Dense Body: nothing moves you, no fall damage
    static final double DENSE_FOOTING = 1.0;
    static final int TOUCH_SEQUENCE = 3;                    // Fission Touch: a blow arcs on to the nearest enemy...
    static final double TOUCH_REACH = 4.0;                  // ...this close to the one struck...
    static final float TOUCH_SHARE = 0.25f;                 // ...for this share of it
    static final int RADIANCE_SEQUENCE = 2;                 // Radiance: enemies this close are burned...
    static final double RADIANCE_RADIUS = 8.0;
    static final float RADIANCE_BURN = 4f;                  // ...this much a second (twice on the undead)
    static final int AUTHORITY_SEQUENCE = 1;                // Gravity Authority: no Heaviness takes on you...
    static final double AUTHORITY_RADIUS = 6.0;             // ...and projectiles this close fly at...
    static final double AUTHORITY_SLOWS = 0.5;              // ...half speed
    static final int CONDENSER_SEQUENCE = 0;                // Weight and Density: nothing roots, freezes or moves you...
    static final double CONDENSER_STEP = 1.5;               // ...and every this many blocks walked the ground shakes

    // =========================
    // Saved on the player (game times are "until" this tick)
    // =========================
    static final String GLIMPSE_UNTIL = "star_dragon_glimpse_until";
    static final String GLIMPSE_LEFT = "star_dragon_glimpse_left";
    static final String STILL_SINCE = "star_dragon_still_since";
    static final String STILL_AT = "star_dragon_still_at";
    static final String INTUITION_READY = "star_dragon_intuition_ready";
    static final String LUCK_WARD = "star_dragon_luck_ward";            // status/Marks: Fate Convergence's halved hit
    static final String SWING_AT = "star_dragon_swing_at";              // the last real blow: its target...
    static final String SWING_TIME = "star_dragon_swing_time";          // ...and its game time (Fission Touch)
    static final String STEP_AT = "star_dragon_step_at";                // where the last shaking step was
    static final String AUTHORITY_SLOWED = "star_dragon_authority";     // on a projectile Gravity Authority slowed

    private static final Identifier EMBRACE_ID = Physique.id("star_dragon_embrace_hearts");
    private static final Identifier SCALES_ID = Physique.id("star_dragon_scales_footing");
    private static final Identifier DENSE_ID = Physique.id("star_dragon_dense_footing");

    // Cooldown keys (combat/Cooldowns)
    /** false: this Pathway's single keys have no cooldown (combat/Cooldowns.addFree): spirituality is their only limit. */
    public static final boolean KEY_COOLDOWNS = false;
    /** A sneak recast from any of this Pathway's wheels: no cooldown, this many times the cost. */
    public static final float QUICK_CAST_COST = 3f;

    static final String CD_CHART = "star_dragon_chart";
    static final String CD_GLIMPSE = "star_dragon_glimpse";

    // =========================
    // Costs: every key and every wheel entry goes through costFor
    // =========================

    /** A rule that changes what a cast costs: (player, cost, ability key) -> new cost. Once, in a static block. */
    @FunctionalInterface
    public interface CostRule {
        int cost(Player player, int cost, String ability);
    }

    private static final List<CostRule> COST_RULES = new ArrayList<>();
    private static final List<BiConsumer<Player, String>> AFTER_CAST = new ArrayList<>();

    /** One more rule for what a Star Dragon cast costs (the planned Starlight meter, a form that makes rays cheaper). */
    public static void addCostRule(CostRule rule) {
        COST_RULES.add(rule);
    }

    /** Runs after every Star Dragon cast that went off, with the ability's key. Once, in a static block. */
    public static void addAfterCast(BiConsumer<Player, String> action) {
        AFTER_CAST.add(action);
    }

    /** What a cast of 'cost' costs this player now: every addCostRule in turn. The one place for them. */
    public static int costFor(Player player, int cost, String ability) {
        if (cost <= 0 || Beyonder.isCastingUnbound(player) || !SoulCore.getPathway(player).equals(PATHWAY)) return cost;
        int result = cost;
        for (CostRule rule : COST_RULES) result = Math.max(0, rule.cost(player, result, ability));
        return result;
    }

    /** Pays for a key's cast (after costFor) and runs afterCast. False, with the usual message, if it cannot. */
    static boolean pay(Player player, int cost, String ability) {
        int due = costFor(player, cost, ability);
        if (due > 0 && !Spells.paySpirituality(player, due)) return false;
        afterCast(player, ability);
        return true;
    }

    /** Every cast that went off (a key through pay, a wheel entry through .onCast). */
    public static void afterCast(Player player, String ability) {
        if (Beyonder.isCastingUnbound(player) || !SoulCore.getPathway(player).equals(PATHWAY)) return;
        for (BiConsumer<Player, String> action : AFTER_CAST) action.accept(player, ability);
    }

    // =========================
    // The wheels
    // =========================

    private static final AbilityWheel SACRIFICE = AbilityWheel.create("star_dragon_sacrifice", "Star Sacrifice")
            .radius(72)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .entry("gravity_abnormality", StarDragonFx.WHEEL_GRAVITY_ABNORMALITY, "Gravity Abnormality",
                    StarDragonGravity.ABNORMALITY_SEQUENCE, StarDragonGravity.ABNORMALITY_COST,
                    StarDragonGravity.ABNORMALITY_COOLDOWN, StarDragonGravity::gravityAbnormality)
            .costBy((player, cost) -> costFor(player, cost, "gravity_abnormality"))
            .entry("electromagnetic_attraction", StarDragonFx.WHEEL_ELECTROMAGNETIC_ATTRACTION, "Electromagnetic Attraction",
                    StarDragonGravity.MAGNET_SEQUENCE, StarDragonGravity.MAGNET_COST, StarDragonGravity.MAGNET_COOLDOWN,
                    StarDragonGravity::electromagneticAttraction)
            .costBy((player, cost) -> costFor(player, cost, "electromagnetic_attraction"))
            .entry("cosmic_void", StarDragonFx.WHEEL_COSMIC_VOID, "Cosmic Void",
                    StarDragonVoid.VOID_SEQUENCE, StarDragonVoid.VOID_COST, StarDragonVoid.VOID_COOLDOWN, StarDragonVoid::cosmicVoid)
            .costBy((player, cost) -> costFor(player, cost, "cosmic_void"))
            .entry("weakening_ray", StarDragonFx.WHEEL_WEAKENING_RAY, "Weakening Ray",
                    StarDragonRays.WEAKENING_SEQUENCE, StarDragonRays.WEAKENING_COST, StarDragonRays.WEAKENING_COOLDOWN,
                    StarDragonRays::weakeningRay)
            .costBy((player, cost) -> costFor(player, cost, "weakening_ray"))
            .entry("sunlight_ray", StarDragonFx.WHEEL_SUNLIGHT_RAY, "Sunlight Ray",
                    StarDragonRays.SUNLIGHT_SEQUENCE, StarDragonRays.SUNLIGHT_COST, StarDragonRays.SUNLIGHT_COOLDOWN,
                    StarDragonRays::sunlightRay)
            .costBy((player, cost) -> costFor(player, StarDragonRays.sunlightCost(player, cost), "sunlight_ray"))
            .onCast((player, sl, entry) -> afterCast(player, entry.key()));

    private static final AbilityWheel HEAVYBRINGER = AbilityWheel.create("star_dragon_heavybringer", "Heavybringer")
            .radius(72)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .entry("heavybringing", StarDragonFx.WHEEL_HEAVYBRINGING, "Heavybringing",
                    StarDragonGravity.HEAVYBRINGING_SEQUENCE, StarDragonGravity.HEAVYBRINGING_COST,
                    StarDragonGravity.HEAVYBRINGING_COOLDOWN, StarDragonGravity::heavybringing)
            .costBy((player, cost) -> costFor(player, cost, "heavybringing"))
            .entry("fate_convergence", StarDragonFx.WHEEL_FATE_CONVERGENCE, "Fate Convergence",
                    StarDragonVoid.FATE_SEQUENCE, StarDragonVoid.FATE_COST, StarDragonVoid.FATE_COOLDOWN,
                    StarDragonVoid::fateConvergence)
            .costBy((player, cost) -> costFor(player, cost, "fate_convergence"))
            .entry("heavy_sphere", StarDragonFx.WHEEL_HEAVY_SPHERE, "Heavy Sphere",
                    StarDragonGravity.SPHERE_SEQUENCE, StarDragonGravity.SPHERE_COST, StarDragonGravity.SPHERE_COOLDOWN,
                    StarDragonGravity::heavySphere)
            .costBy((player, cost) -> costFor(player, cost, "heavy_sphere"))
            .entry("series_of_rays", StarDragonFx.WHEEL_SERIES_OF_RAYS, "Series of Rays",
                    StarDragonRays.SERIES_SEQUENCE, StarDragonRays.SERIES_COST, StarDragonRays.SERIES_COOLDOWN,
                    StarDragonRays::seriesOfRays)
            .costBy((player, cost) -> costFor(player, cost, "series_of_rays"))
            .entry("remote_sight", StarDragonFx.WHEEL_REMOTE_SIGHT, "Remote Sight",
                    StarDragonVoid.SIGHT_SEQUENCE, StarDragonVoid.SIGHT_COST, StarDragonVoid.SIGHT_COOLDOWN,
                    StarDragonVoid::remoteSight)
            .costBy((player, cost) -> costFor(player, cost, "remote_sight"))
            .onCast((player, sl, entry) -> afterCast(player, entry.key()));

    private static final AbilityWheel COSMOS = AbilityWheel.create("star_dragon_cosmos", "Cosmos")
            .radius(84)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .entry("fusion_cluster", StarDragonFx.WHEEL_FUSION_CLUSTER, "Fusion Cluster",
                    StarDragonSky.FUSION_SEQUENCE, StarDragonSky.FUSION_COST, StarDragonSky.FUSION_COOLDOWN,
                    StarDragonSky::fusionCluster)
            .costBy((player, cost) -> costFor(player, cost, "fusion_cluster"))
            .entry("radiant_angel_form", StarDragonFx.WHEEL_RADIANT_ANGEL_FORM, "Radiant Angel Form",
                    StarDragonForms.ANGEL_SEQUENCE, StarDragonForms.ANGEL_COST, StarDragonForms.ANGEL_COOLDOWN,
                    StarDragonForms::radiantAngelForm)
            .free(StarDragonForms::isRadiantAngel)                                // turning back
            .costBy((player, cost) -> costFor(player, cost, "radiant_angel_form"))
            .entry("time_dilation", StarDragonFx.WHEEL_TIME_DILATION, "Gravitational Time Dilation",
                    StarDragonGravity.DILATION_SEQUENCE, StarDragonGravity.DILATION_COST, StarDragonGravity.DILATION_COOLDOWN,
                    StarDragonGravity::timeDilation)
            .costBy((player, cost) -> costFor(player, cost, "time_dilation"))
            .entry("planetfall", StarDragonFx.WHEEL_PLANETFALL, "Planetfall",
                    StarDragonSky.PLANET_SEQUENCE, StarDragonSky.PLANET_COST, StarDragonSky.PLANET_COOLDOWN,
                    StarDragonSky::planetfall)
            .costBy((player, cost) -> costFor(player, cost, "planetfall"))
            .entry("star_dragon_form", StarDragonFx.WHEEL_STAR_DRAGON_FORM, "Star Dragon Form",
                    StarDragonForms.DRAGON_SEQUENCE, StarDragonForms.DRAGON_COST, StarDragonForms.DRAGON_COOLDOWN,
                    StarDragonForms::starDragonForm)
            .free(StarDragonForms::isDragon)                                      // turning back
            .costBy((player, cost) -> costFor(player, cost, "star_dragon_form"))
            .entry("neutron_star", StarDragonFx.WHEEL_NEUTRON_STAR, "Neutron Star",
                    StarDragonGravity.NEUTRON_SEQUENCE, StarDragonGravity.NEUTRON_COST, StarDragonGravity.NEUTRON_COOLDOWN,
                    StarDragonGravity::neutronStar)
            .costBy((player, cost) -> costFor(player, cost, "neutron_star"))
            .entry("singularity", StarDragonFx.WHEEL_SINGULARITY, "Singularity",
                    StarDragonGravity.SINGULARITY_SEQUENCE, StarDragonGravity.SINGULARITY_COST,
                    StarDragonGravity.SINGULARITY_COOLDOWN, StarDragonGravity::singularity)
            .costBy((player, cost) -> costFor(player, cost, "singularity"))
            .entry("quantum_uncertainty", StarDragonFx.WHEEL_QUANTUM_UNCERTAINTY, "Quantum Uncertainty",
                    StarDragonVoid.QUANTUM_SEQUENCE, StarDragonVoid.QUANTUM_COST, StarDragonVoid.QUANTUM_COOLDOWN,
                    StarDragonVoid::quantumUncertainty)
            .costBy((player, cost) -> costFor(player, cost, "quantum_uncertainty"))
            .onCast((player, sl, entry) -> afterCast(player, entry.key()));

    static {
        // Condenser: no confinement needed, the Fusion Cluster costs half
        addCostRule((player, cost, ability) -> "fusion_cluster".equals(ability) && grown(player, CONDENSER_SEQUENCE)
                ? Math.round(cost * StarDragonSky.FUSION_CONDENSER_COST) : cost);
    }

    // =========================
    // Events
    // =========================

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class StarDragonEvents {

        static {
            // the single keys have no cooldown (KEY_COOLDOWNS)
            if (!KEY_COOLDOWNS) Cooldowns.addFree(Cooldowns.keysStartingWith("star_dragon_"));
            // Dense Body (4): nothing pulls, pushes or lifts you; Heavybringing and the Neutron Star: nothing moves you
            Spells.addMoveImmunity(one -> one instanceof Player dense && (isStarDragon(dense, DENSE_SEQUENCE)
                    || StarDragonGravity.isHeavybringing(dense) || StarDragonGravity.isNeutronStar(dense)));
            // Gravity Authority (1) and the Neutron Star: no Heaviness takes on you
            Weight.addImmunity(one -> one instanceof Player heavy
                    && (isStarDragon(heavy, AUTHORITY_SEQUENCE) || StarDragonGravity.isNeutronStar(heavy)));
            // Weight and Density (0) and the Neutron Star: nothing roots you in place or freezes you (your own gathering
            // for the Singularity is the one hold you put on yourself)
            Root.addImmunity((one, restriction) -> one instanceof Player dense && heldBody(restriction)
                    && !StarDragonGravity.isGathering(dense)
                    && (isStarDragon(dense, CONDENSER_SEQUENCE) || StarDragonGravity.isNeutronStar(dense)));
            Freeze.addImmunity(one -> one instanceof Player dense
                    && (isStarDragon(dense, CONDENSER_SEQUENCE) || StarDragonGravity.isNeutronStar(dense)));
        }

        @SubscribeEvent
        public static void onStarDragonTick(TickEvent.PlayerTickEvent.Post event) {
            Player player = event.player();
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (player.tickCount % 20 == 0) bodyTick(player);
            if (player.tickCount % 2 == 0 && isStarDragon(player, AUTHORITY_SEQUENCE)) gravityAuthority(player, sl);
            if (player.tickCount % 4 == 0 && isStarDragon(player, CONDENSER_SEQUENCE)) condenserSteps(player, sl);
        }

        /** A real blow (not an ability's hit): remembered for Fission Touch, and the Star Dragon's asteroid. */
        @SubscribeEvent(priority = Priority.LOW)
        public static void onStarDragonSwing(AttackEntityEvent event) {
            Player player = event.getEntity();
            if (!(player.level() instanceof ServerLevel sl) || !(event.getTarget() instanceof LivingEntity target)) return;
            player.getPersistentData().putString(SWING_AT, target.getUUID().toString());
            player.getPersistentData().putLong(SWING_TIME, sl.getGameTime());
            StarDragonForms.swing(player, target);
        }

        /** Quantum Uncertainty: a teleport into the field slips aside. */
        @SubscribeEvent
        public static boolean onStarDragonTeleport(EntityTeleportEvent event) {
            StarDragonVoid.teleport(event);
            return false;
        }

        /**
         * Before a blow lands on a Star Dragon: Fate Glimpse dodges it, Tidal Surge's crest parries it. A cancelled
         * attack does nothing at all.
         */
        @SubscribeEvent
        public static boolean onStarDragonAttacked(LivingAttackEvent event) {
            if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel sl)) return false;
            DamageSource source = event.getSource();
            Entity attacker = source.getEntity();
            if (attacker == null || attacker == player || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
            if (StarDragonTides.parries(player, sl, source)) return true;
            return glimpseDodges(player, sl, source);
        }

        /** Hurt events: Star's Embrace, Dense Body, the luck of Fate Convergence, the void's limit, Scaled Form's blows. */
        @SubscribeEvent
        public static void onStarDragonHurt(LivingHurtEvent event) {
            LivingEntity hurt = event.getEntity();
            if (hurt.level().isClientSide()) return;
            if (Marks.count(hurt, LUCK_WARD) > 0 && event.getSource().getEntity() != null) {
                event.setAmount(event.getAmount() * 0.5f);                      // Fate Convergence's luck: the first hit halved
                Marks.take(hurt, LUCK_WARD);
            }
            StarDragonVoid.hurt(event);
            StarDragonTides.hurt(event);
            fissionTouch(event);
            if (!(hurt instanceof Player player) || !event.getSource().is(DamageTypes.FALL)) return;
            if (isStarDragon(player, DENSE_SEQUENCE)) event.setAmount(0f);
            else if (isStarDragon(player, EMBRACE_SEQUENCE)) event.setAmount(event.getAmount() * EMBRACE_FALL);
        }

        /** Spiritual Intuition (8): a mob starts hunting you from where you cannot see it. */
        @SubscribeEvent
        public static void onStarDragonTarget(LivingChangeTargetEvent event) {
            if (!(event.getNewTarget() instanceof ServerPlayer hunted) || event.getEntity().level().isClientSide()) return;
            if (!isStarDragon(hunted, INTUITION_SEQUENCE)) return;
            LivingEntity mob = event.getEntity();
            long now = hunted.level().getGameTime();
            if (hunted.getPersistentData().getLongOr(INTUITION_READY, 0L) > now) return;
            Vec3 toMob = mob.position().subtract(hunted.position());
            boolean behind = toMob.horizontalDistanceSqr() > 0.01
                    && Spells.flatLook(hunted).dot(new Vec3(toMob.x, 0, toMob.z).normalize()) < 0.2;
            if (!behind && hunted.hasLineOfSight(mob)) return;                     // seen coming: no warning needed
            hunted.getPersistentData().putLong(INTUITION_READY, now + INTUITION_REST);
            StarDragonFx.intuition(hunted, mob.position());
        }

        // what was on the screen does not survive relogging or dying: start clean
        @SubscribeEvent
        public static void onStarDragonLogin(PlayerEvent.PlayerLoggedInEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onStarDragonRespawn(PlayerEvent.PlayerRespawnEvent event) {
            clearState(event.getEntity());
        }
    }

    // =========================
    // Ability 1 - Star Chart (Astronomy Aficionado) / Fate Glimpse (Star Worshipper)
    // Normal: the enemy you look at (CHART_RANGE) is Charted for CHART_TICKS (StarDragonMarks): its constellation is
    //         drawn for you through walls, it cannot hide (status/Exposed), and the first ability of yours that lands on
    //         it does 20% more (hit / rayHit spend it). No enemy in sight: nothing spent.
    // Sneak:  Fate Glimpse (Sequence 8). For GLIMPSE_TICKS the first GLIMPSE_DODGES attacks that have an attacker
    //         (melee, projectiles, abilities) do nothing, and you slip a step aside. Falls, poison, the void do not count.
    // =========================
    public static void starDragonAstronomy(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (player.isShiftKeyDown()) {
            if (!ready(player, bypassClassCheck, GLIMPSE_SEQUENCE)) return;
            fateGlimpse(player, sl);
            return;
        }
        if (!ready(player, bypassClassCheck, CHART_SEQUENCE)) return;
        LivingEntity target = Spells.aimEnemy(player, sl, CHART_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one under these stars to chart."));
            return; // nothing spent
        }
        if (!Cooldowns.ready(player, CD_CHART, "Star Chart")) return;
        if (!pay(player, CHART_COST, CD_CHART)) return;
        Cooldowns.start(player, CD_CHART, CHART_COOLDOWN);
        starChart(player, sl, target);
    }

    /** Star Chart once it is paid for: 'target' is Charted at CHART_LANDS (a Beyonder mob casts it this way too). */
    static void starChart(LivingEntity player, ServerLevel sl, LivingEntity target) {
        StarDragonFx.starChart(sl, player, target);
        Later.run(sl, StarDragonFx.CHART_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive()) return;
            StarDragonMarks.put(target, Mark.CHARTED, player, CHART_TICKS);
        });
    }

    private static void fateGlimpse(Player player, ServerLevel sl) {
        if (!Cooldowns.ready(player, CD_GLIMPSE, "Fate Glimpse")) return;
        if (!pay(player, GLIMPSE_COST, CD_GLIMPSE)) return;
        Cooldowns.start(player, CD_GLIMPSE, GLIMPSE_COOLDOWN);
        player.getPersistentData().putLong(GLIMPSE_UNTIL, sl.getGameTime() + GLIMPSE_TICKS);
        player.getPersistentData().putInt(GLIMPSE_LEFT, GLIMPSE_DODGES);
        String tag = glimpseTag(player);
        StarDragonFx.fateGlimpse(sl, player, GLIMPSE_TICKS, tag);
        StarDragonFx.status(player, Status.FATE_GLIMPSE, GLIMPSE_TICKS);
        Later.run(sl, GLIMPSE_TICKS, () -> {
            if (player.getPersistentData().getLongOr(GLIMPSE_UNTIL, 0L) <= sl.getGameTime()) StarDragonFx.fateGlimpseEnd(sl, player, tag);
        });
    }

    /** One attack aimed at a Star Worshipper under Fate Glimpse: dodged while dodges are left. */
    private static boolean glimpseDodges(LivingEntity player, ServerLevel sl, DamageSource source) {
        if (!isActive(player, GLIMPSE_UNTIL)) return false;
        int left = player.getPersistentData().getIntOr(GLIMPSE_LEFT, 0);
        if (left <= 0) return false;
        player.getPersistentData().putInt(GLIMPSE_LEFT, left - 1);
        Vec3 from = player.position();
        Vec3 side = Spells.rightOf(player).scale(sl.getRandom().nextBoolean() ? GLIMPSE_STEP : -GLIMPSE_STEP);
        Vec3 to = from.add(side);
        if (!Spells.fits(player, sl, to)) to = from.subtract(side);
        if (Spells.fits(player, sl, to) && !Root.has(player, RootRestriction.MOVE)) player.teleportTo(to.x, to.y, to.z);
        else to = from;
        StarDragonFx.glimpseDodge(sl, player, from, to);
        if (left - 1 <= 0) {
            player.getPersistentData().remove(GLIMPSE_UNTIL);
            StarDragonFx.fateGlimpseEnd(sl, player, glimpseTag(player));
            StarDragonFx.statusEnd(player, Status.FATE_GLIMPSE);
        }
        return true;
    }

    private static String glimpseTag(LivingEntity player) {
        return "sd_glimpse_" + player.getId();
    }

    // =========================
    // Ability 2 - Star Sacrifice: the wheel (StarDragonGravity, StarDragonVoid, StarDragonRays)
    // Normal: the wheel. Sneak: the last pick again, without the wheel (no cooldown, QUICK_CAST_COST times the cost).
    // =========================
    public static void starDragonStarSacrifice(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, SACRIFICE_SEQUENCE)) return;
        if (player.isShiftKeyDown()) SACRIFICE.quickCast(player);
        else SACRIFICE.open(player);
    }

    // =========================
    // Ability 3 - Navigator: Void Passage (StarDragonVoid) / Piercing Ray (StarDragonRays)
    // =========================
    public static void starDragonNavigator(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, NAVIGATOR_SEQUENCE)) return;
        if (player.isShiftKeyDown()) StarDragonRays.piercingRay(player, sl);
        else StarDragonVoid.voidPassage(player, sl);
    }

    // =========================
    // Ability 4 - Tidal Scholar: Tidal Surge / Scaled Form (StarDragonTides)
    // =========================
    public static void starDragonTidalScholar(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, TIDAL_SEQUENCE)) return;
        if (player.isShiftKeyDown()) StarDragonTides.scaledForm(player, sl);
        else StarDragonTides.tidalSurge(player, sl);
    }

    // =========================
    // Ability 5 - Heavybringer: the wheel (StarDragonGravity, StarDragonVoid, StarDragonRays)
    // =========================
    public static void starDragonHeavybringer(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, HEAVYBRINGER_SEQUENCE)) return;
        if (player.isShiftKeyDown()) HEAVYBRINGER.quickCast(player);
        else HEAVYBRINGER.open(player);
    }

    // =========================
    // Ability 6 - Star Shepherd: Star Grazing / Fission Pulse (StarDragonSky)
    // =========================
    public static void starDragonStarShepherd(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, SHEPHERD_SEQUENCE)) return;
        if (player.isShiftKeyDown()) StarDragonSky.fissionPulse(player, sl);
        else StarDragonSky.starGrazing(player, sl);
    }

    // =========================
    // Ability 7 - Cosmos: the wheel (StarDragonSky, StarDragonForms, StarDragonGravity, StarDragonVoid)
    // Normal: the wheel. Sneak: the last pick again, without the wheel (no cooldown, QUICK_CAST_COST times the cost).
    // =========================
    public static void starDragonCosmos(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, COSMOS_SEQUENCE)) return;
        if (player.isShiftKeyDown()) COSMOS.quickCast(player);
        else COSMOS.open(player);
    }

    // =========================
    // Passives (the once-a-second ones run from SequencePassives)
    // =========================

    /**
     * Astronomy Aficionado (9), once a second: Stargazing. Standing still STARGAZING_STILL under the open sky brings
     * spirituality back STARGAZING_REGEN faster (twice that at night).
     */
    public static void stargazing(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        Vec3 at = player.position();
        String last = player.getPersistentData().getStringOr(STILL_AT, "");
        String here = Math.round(at.x * 4) + "," + Math.round(at.y * 4) + "," + Math.round(at.z * 4);
        long now = sl.getGameTime();
        if (!here.equals(last)) {
            player.getPersistentData().putString(STILL_AT, here);
            player.getPersistentData().putLong(STILL_SINCE, now);
            return;
        }
        if (now - player.getPersistentData().getLongOr(STILL_SINCE, now) < STARGAZING_STILL) return;
        if (!sl.canSeeSky(player.blockPosition().above())) return;
        float share = STARGAZING_REGEN * (sl.isDarkOutside() ? STARGAZING_NIGHT : 1f);
        float extra = Beyonder.spiritualityRegenPerTick(player) * 20f * share;
        SoulCore.setSpiritualityQuietly(player, Math.min(Beyonder.maxSpirituality(player), SoulCore.getSpirituality(player) + extra));
        StarDragonFx.stargazing(sl, player);
    }

    /** Navigator (6), once a second: Sense of Passages. The hidden near you show for you and cannot stay unnoticed. */
    public static void senseOfPassages(LivingEntity player) {
        if (!(player instanceof ServerPlayer viewer) || !(player.level() instanceof ServerLevel sl)) return;
        for (LivingEntity one : Spells.livingAround(player, sl, player.position(), PASSAGES_RADIUS)) {
            if (Spells.isFriend(player, one)) continue;
            boolean hiding = Concealment.isHidden(one) || Unnoticed.isUnnoticed(one) || one.hasEffect(MobEffects.INVISIBILITY);
            if (!hiding) continue;
            Unnoticed.end(one);
            StarDragonFx.senseOfPassages(viewer, one);
        }
    }

    /** Tidal Scholar (5), once a second: Starlight Scales. Fast swimming in water (the half knockback is in bodyTick). */
    public static void starlightScales(LivingEntity player) {
        if (!player.isInWater()) return;
        Spells.keepEffect(player, MobEffects.DOLPHINS_GRACE, 0, 60);
        if (player.level() instanceof ServerLevel sl && player.tickCount % 60 == 0) StarDragonFx.starlightScales(sl, player);
    }

    /**
     * Star Shepherd (3), on a real blow (the swing remembered this tick): Fission Touch. It arcs on to the nearest other
     * enemy within TOUCH_REACH of the one struck, for TOUCH_SHARE of it.
     */
    private static void fissionTouch(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof Player player) || source.getDirectEntity() != player) return;
        if (!isStarDragon(player, TOUCH_SEQUENCE) || !(player.level() instanceof ServerLevel sl)) return;
        LivingEntity struck = event.getEntity();
        if (player.getPersistentData().getLongOr(SWING_TIME, -1L) != sl.getGameTime()
                || !struck.getUUID().toString().equals(player.getPersistentData().getStringOr(SWING_AT, ""))) return;
        player.getPersistentData().remove(SWING_AT);                             // once a blow: the arc's own hit is not one
        LivingEntity next = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, struck.position(), TOUCH_REACH)) {
            if (enemy == struck) continue;
            double d = enemy.distanceToSqr(struck);
            if (d < best) {
                best = d;
                next = enemy;
            }
        }
        if (next == null) return;
        StarDragonFx.fissionTouch(sl, struck.getBoundingBox().getCenter(), next.getBoundingBox().getCenter());
        hit(player, next, event.getAmount() * TOUCH_SHARE);
    }

    /** Radiant Angel (2), once a second: Radiance. Enemies near you are burned (only a glow is drawn, no light blocks). */
    public static void radiance(LivingEntity player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        StarDragonFx.radianceAura(sl, player, RADIANCE_RADIUS);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), RADIANCE_RADIUS)) {
            hit(player, enemy, RADIANCE_BURN * (enemy.is(EntityTypeTags.UNDEAD) ? 2f : 1f));
        }
    }

    /** Star Dragon (1), every other tick: Gravity Authority. Projectiles of the other side near you fly at half speed. */
    private static void gravityAuthority(LivingEntity player, ServerLevel sl) {
        for (Projectile shot : Spells.hostileShotsAround(player, sl, player.getBoundingBox().getCenter(), AUTHORITY_RADIUS)) {
            if (shot.getPersistentData().getBooleanOr(AUTHORITY_SLOWED, false)) continue;   // once a projectile
            shot.getPersistentData().putBoolean(AUTHORITY_SLOWED, true);
            shot.setDeltaMovement(shot.getDeltaMovement().scale(AUTHORITY_SLOWS));
            shot.hurtMarked = true;
            StarDragonFx.gravityAuthority(sl, shot.position());
        }
    }

    /** Condenser (0), every 4 ticks: Weight and Density's steps. Every CONDENSER_STEP blocks walked, the ground shakes. */
    private static void condenserSteps(LivingEntity player, ServerLevel sl) {
        if (!player.onGround() || player.isSpectator()) return;
        Vec3 at = player.position();
        String last = player.getPersistentData().getStringOr(STEP_AT, "");
        String[] parts = last.split(",");
        if (parts.length == 2) {
            try {
                double dx = at.x - Double.parseDouble(parts[0]);
                double dz = at.z - Double.parseDouble(parts[1]);
                if (dx * dx + dz * dz < CONDENSER_STEP * CONDENSER_STEP) return;
                StarDragonFx.condenserStep(sl, player);
            } catch (NumberFormatException ignored) {
                // a broken value: start again from here
            }
        }
        player.getPersistentData().putString(STEP_AT, at.x + "," + at.z);
    }

    /** The holds Weight and Density (and the Neutron Star) shrug off: held in place, no jumping, no teleporting. */
    private static boolean heldBody(RootRestriction restriction) {
        return restriction == RootRestriction.MOVE || restriction == RootRestriction.JUMP || restriction == RootRestriction.TELEPORT;
    }

    /** Once a second, for every player: the hearts and footing of Star's Embrace, Starlight Scales and Dense Body. */
    private static void bodyTick(LivingEntity player) {
        Physique.keep(player, Attributes.MAX_HEALTH, EMBRACE_ID, EMBRACE_HEARTS, AttributeModifier.Operation.ADD_VALUE,
                isStarDragon(player, EMBRACE_SEQUENCE));
        Physique.keep(player, Attributes.KNOCKBACK_RESISTANCE, SCALES_ID, SCALES_FOOTING, AttributeModifier.Operation.ADD_VALUE,
                isStarDragon(player, SCALES_SEQUENCE) && !isStarDragon(player, DENSE_SEQUENCE));
        Physique.keep(player, Attributes.KNOCKBACK_RESISTANCE, DENSE_ID, DENSE_FOOTING, AttributeModifier.Operation.ADD_VALUE,
                isStarDragon(player, DENSE_SEQUENCE) || StarDragonGravity.isHeavybringing(player));
    }

    // =========================
    // Helpers shared by the abilities
    // =========================

    /**
     * A Star Dragon ability hit: Spells.strike, with the Star Chart's calculated shot (the first hit on what you
     * Charted, 20% more) and, from the Heavybringer, the true pocket's fragility (in status/Vulnerable already).
     */
    static void hit(LivingEntity player, LivingEntity target, float damage) {
        float dealt = damage;
        if (StarDragonMarks.spendCharted(target, player)) dealt *= 1f + StarDragonMarks.CHARTED_BONUS;
        Spells.strike(player, target, dealt);
    }

    /**
     * A ray's hit: as hit(...), and on one in your Cosmic Void from the Tidal Scholar the ray manifests there, VOID_RAY
     * more, and nothing in the way softens or stops it (Spells.strikeThrough).
     */
    static void rayHit(LivingEntity player, LivingEntity target, float damage) {
        boolean manifests = isStarDragon(player, StarDragonVoid.MANIFEST_SEQUENCE)
                && StarDragonMarks.fromCaster(target, Mark.VOIDED, player);
        if (!manifests) {
            hit(player, target, damage);
            return;
        }
        float dealt = damage * (1f + StarDragonVoid.VOID_RAY);
        if (StarDragonMarks.spendCharted(target, player)) dealt *= 1f + StarDragonMarks.CHARTED_BONUS;
        Spells.strikeThrough(player, target, dealt);
        if (target.level() instanceof ServerLevel sl) StarDragonFx.voidManifest(sl, target);
    }

    /** The luck of Fate Convergence on an ally: the next hit with an attacker is halved (status/Marks, for 'ticks'). */
    static void luckWard(LivingEntity ally, int ticks) {
        Marks.add(ally, LUCK_WARD, 1, ticks);
    }

    /** True for this Pathway at this Sequence or stronger. */
    public static boolean isStarDragon(Player player, int sequence) {
        return SoulCore.getPathway(player).equals(PATHWAY) && Beyonder.hasSequence(player, sequence);
    }

    /** The same for anyone: a player as above, a Beyonder mob of this Pathway by its own Sequence. */
    public static boolean isStarDragon(LivingEntity entity, int sequence) {
        if (entity instanceof Player player) return isStarDragon(player, sequence);
        return PATHWAY.equals(Beyonders.pathwayOf(entity)) && Beyonder.hasSequence(entity, sequence);
    }

    /** True while casting as a Star Dragon at that Sequence: their own Pathway, or a stolen cast (no Pathway check). */
    static boolean grown(LivingEntity player, int sequence) {
        return Beyonder.hasSequence(player, sequence);
    }

    /** Has this pathway (or the check is skipped) and has reached the Sequence the ability needs. */
    static boolean ready(Player player, boolean bypassClassCheck, int sequence) {
        if (!bypassClassCheck && !SoulCore.getPathway(player).equals(PATHWAY)) return false;
        return Beyonder.hasSequence(player, sequence);
    }

    /** True while the game time saved under 'key' is still ahead. */
    static boolean isActive(Entity entity, String key) {
        return entity.getPersistentData().getLongOr(key, 0L) > entity.level().getGameTime();
    }

    /** After a relog or a death: nothing of the timed casts is left over. */
    private static void clearState(Player player) {
        player.getPersistentData().remove(GLIMPSE_UNTIL);
        player.getPersistentData().remove(GLIMPSE_LEFT);
        StarDragonGravity.clearState(player);
        StarDragonVoid.clearState(player);
        StarDragonTides.clearState(player);
        StarDragonSky.clearState(player);
        StarDragonForms.clearState(player);
        player.getPersistentData().remove(SWING_AT);
        player.getPersistentData().remove(STEP_AT);
        if (Form.current(player) != null && Form.current(player).startsWith("star_dragon")) Form.end(player);
        if (!SoulCore.getPathway(player).equals(PATHWAY)) return;
        for (Status status : Status.values()) StarDragonFx.statusEnd(player, status);   // a clock left running before death
    }
}
