package net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics;

import net.lukario.frogerealm.combat.Later;
import net.lukario.frogerealm.combat.MeleeCombo;
import net.lukario.frogerealm.particles.fx.ModelFx;
import net.lukario.frogerealm.particles.fx.ParticleFx;
import net.lukario.frogerealm.particles.fx.ParticleShapes;
import net.lukario.frogerealm.particles.fx.SlashFx;
import net.lukario.frogerealm.root.Freeze;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Prince Of Abolition: a frenzied caster who throws a different conjured weapon or element with every key.
 * Every ability has a normal cast and a sneak cast:
 *
 *   1 Spear of Abolition : a spear thrown through everything in its way    | sneak: spears rain on the spot you aim at
 *   2 Corroding Flame    : a fireball that bursts, burns and corrodes      | sneak: a pyre that burns and corrupts
 *   3 Frost              : ice spikes erupt in a line and freeze           | sneak: one target sealed in a glacier
 *   4 Gavel of Ruin      : a giant hammer drops where you aim and weakens  | sneak: the hammer whirls around you
 *   5 Meteor             : a meteor crashes where you aim                  | sneak: a meteor shower
 *   6 Entropy            : a sphere drags everything in, then bursts       | sneak: the sphere forms around you
 *   7 Abolition          : a colossal blade falls and strips all buffs     | sneak: Frenzy, random spells hit everything near you
 *
 * What the old abilities did lives on as side effects of these (the helpers are near the bottom):
 *   Bribe            slowness, weakness, mining fatigue  -> bribeWeaken  : the hammer drop and the blade
 *   sneak Bribe      the buffs on yourself               -> bribeEmpower : Frenzy
 *   Corrosion        wither and poison                   -> corrode      : the fireball, the Entropy burst and the blade
 *   sneak Corrosion  corruption                          -> corrupt      : the pyre, Entropy's pulses and the blade
 *
 * Nothing here breaks blocks or lights fires. The numbers for every ability are right below;
 * the 3D models are in models/model_fx/ (see docs/MODEL_FX.md).
 */
public class PrinceOfAbolition {

    // =========================
    // Ability settings (cost = soul essence, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Side effects kept from the old abilities
    private static final int BRIBE_TICKS = 120;                  // old Bribe: slowness, weakness, mining fatigue
    private static final int CORROSION_TICKS = 180;              // old Corrosion: wither + poison
    private static final int CORROSION_CORRUPTION = 10;          // old sneak Corrosion: corruption added (100 kills)

    // Ability 1 - Spear of Abolition (ascension stage 0)
    private static final int SPEAR_COST = 250;
    private static final double SPEAR_RANGE = 28.0;
    private static final double SPEAR_SPEED = 3.0;               // blocks per tick
    private static final int SPEAR_WINDUP = 4;                   // ticks it hangs beside you before it flies
    private static final float SPEAR_DAMAGE = 12f;
    private static final double SPEAR_RAIN_RANGE = 24.0;
    private static final int SPEAR_RAIN_COUNT = 7;
    private static final double SPEAR_RAIN_SPREAD = 4.0;         // how far from the aim point they can land
    private static final float SPEAR_RAIN_DAMAGE = 9f;
    private static final double SPEAR_RAIN_HIT = 1.8;            // radius hurt around each fallen spear
    private static final int SPEAR_FALL = 5;                     // ticks a falling spear takes to land

    // Ability 2 - Corroding Flame (stage 1)
    private static final int FLAME_COST = 1000;
    private static final double FIREBALL_RANGE = 26.0;
    private static final double FIREBALL_SPEED = 1.3;            // blocks per tick
    private static final double FIREBALL_RADIUS = 3.5;
    private static final float FIREBALL_DAMAGE = 14f;
    private static final int FIREBALL_BURN_TICKS = 100;
    private static final double PYRE_RANGE = 20.0;
    private static final double PYRE_RADIUS = 3.0;
    private static final float PYRE_HEIGHT = 5f;
    private static final int PYRE_TICKS = 60;
    private static final int PYRE_PULSE = 10;                    // ticks between burns
    private static final float PYRE_DAMAGE = 4f;                 // per burn

    // Ability 3 - Frost (stage 2)
    private static final int FROST_COST = 1250;
    private static final int SPIKE_COUNT = 10;
    private static final double SPIKE_FIRST = 1.2;               // how far in front of you the first one comes up
    private static final double SPIKE_SPACING = 1.1;
    private static final float SPIKE_DAMAGE = 16f;               // once per enemy, however many spikes reach it
    private static final int SPIKE_FREEZE_TICKS = 60;
    private static final double SPIKE_HIT = 1.4;                 // radius hurt around each spike
    private static final int SPIKE_LIFE = 34;                    // ticks a spike stays
    private static final double GLACIER_RANGE = 24.0;
    private static final int GLACIER_FREEZE_TICKS = 160;
    private static final float GLACIER_DAMAGE = 8f;

    // Ability 4 - Gavel of Ruin (stage 3)
    private static final int HAMMER_COST = 2500;
    private static final double HAMMERFALL_RANGE = 24.0;
    private static final double HAMMERFALL_RADIUS = 4.5;
    private static final float HAMMERFALL_DAMAGE = 24f;
    private static final float HAMMERFALL_SIZE = 2.6f;           // 1 = the size it has in Blockbench (3 blocks tall)
    private static final int HAMMERFALL_WARNING = 10;            // ticks the circle shows before the hammer appears
    private static final int HAMMER_FALL = 7;                    // ticks the fall takes
    private static final float WHIRL_SIZE = 1.5f;
    private static final double WHIRL_RADIUS = 4.5;
    private static final float WHIRL_DAMAGE = 12f;               // per turn, two turns

    // Ability 5 - Meteor (stage 4)
    private static final int METEOR_COST = 5000;
    private static final double METEOR_RANGE = 28.0;
    private static final float METEOR_SIZE = 2.6f;
    private static final double METEOR_RADIUS = 6.0;
    private static final float METEOR_DAMAGE = 40f;              // in the middle, half at the edge
    private static final int METEOR_BURN_TICKS = 160;
    private static final int METEOR_FALL = 24;                   // ticks from the sky to the ground
    private static final double METEOR_START_HEIGHT = 26.0;
    private static final double METEOR_START_BACK = 14.0;        // it comes in from this far behind the landing spot
    private static final int SHOWER_COUNT = 6;
    private static final double SHOWER_SPREAD = 7.0;
    private static final float SHOWER_SIZE = 1.2f;
    private static final double SHOWER_RADIUS = 3.0;
    private static final float SHOWER_DAMAGE = 16f;

    // Ability 6 - Entropy (stage 5)
    private static final int ENTROPY_COST = 6000;
    private static final double ENTROPY_RANGE = 20.0;
    private static final float ENTROPY_SIZE = 2.2f;
    private static final int ENTROPY_ARRIVE = 8;                 // ticks until the sphere is in place
    private static final int ENTROPY_TICKS = 100;                // how long it pulls after that
    private static final int ENTROPY_PULSE = 10;                 // ticks between its damage pulses
    private static final double ENTROPY_PULL_RADIUS = 8.0;
    private static final double ENTROPY_PULL = 0.3;              // how hard it drags, blocks per tick
    private static final double ENTROPY_HURT_RADIUS = 3.5;
    private static final float ENTROPY_PULSE_DAMAGE = 4f;
    private static final int ENTROPY_PULSE_CORRUPTION = 2;
    private static final double ENTROPY_BURST_RADIUS = 5.5;
    private static final float ENTROPY_BURST_DAMAGE = 22f;

    // Ability 7 - Abolition (stage 6)
    private static final int ABOLITION_COST = 12000;
    private static final double ABOLITION_RANGE = 28.0;
    private static final double ABOLITION_RADIUS = 8.0;
    private static final float ABOLITION_DAMAGE = 60f;
    private static final int ABOLITION_WARNING = 20;             // ticks the blade hangs in the air first
    private static final float BLADE_HANG = 9f;                  // how high its point hangs above the spot (low enough to see)
    private static final int BLADE_FALL = 5;                     // ticks the fall takes
    private static final int ABOLITION_CORRUPTION = 15;
    private static final int FRENZY_TICKS = 120;
    private static final int FRENZY_INTERVAL = 6;                // ticks between its random spells
    private static final double FRENZY_RANGE = 16.0;
    private static final int FRENZY_BUFF_TICKS = 520;            // how long the old sneak Bribe's buffs last

    // =========================
    // 3D models (models/model_fx/..., textures in textures/model_fx/...)
    // =========================
    private static final float SPEAR_SIZE = 1.25f;
    private static final double SPEAR_TIP = (32 - 8) / 16.0 * SPEAR_SIZE;   // middle of the shaft -> tip, in blocks
    private static final ModelFx SPEAR = ModelFx.of("prince_of_abolition/spear")
            .scale(SPEAR_SIZE).pivot(8, 8, 8)                     // turns around the middle of the shaft
            .glow().aura(0xFFFF5A2A, 0.07f, 3);
    // .unshaded() = every face equally bright: fire and energy should not have dark sides like a solid thing
    private static final ModelFx FIREBALL = ModelFx.of("fireball")
            .pivot(8, 8, 8).glow().unshaded().seeThrough()
            .aura(0xFFFF7A1A, 0.14f, 3).spin(26f);
    private static final ModelFx FIRE_BLAST = ModelFx.of("fire_blast")     // the flash of an explosion, about 1 block wide
            .pivot(8, 8, 8).glow().unshaded().seeThrough();
    private static final ModelFx FLAMES = ModelFx.of("flame_pillar")  // 1 block wide, 2 tall, animated texture
            .glow().unshaded().seeThrough();
    private static final ModelFx ICE_SPIKE = ModelFx.of("ice_spike")
            .glow().aura(0x5560D8FF, 0.05f, 2);
    private static final ModelFx HAMMER = ModelFx.of("prince_of_abolition/hammer")
            .glow().aura(0xFFFF2A1A, 0.12f, 3);
    private static final ModelFx METEOR = ModelFx.of("meteor")
            .pivot(8, 6, 8).glow().aura(0xFFFF6A1A, 0.2f, 3);
    private static final ModelFx METEOR_TAIL = ModelFx.of("meteor_tail")   // the flames behind the rock, same pivot
            .pivot(8, 6, 8).glow().unshaded().seeThrough();
    private static final ModelFx ENTROPY_ORB = ModelFx.of("prince_of_abolition/entropy_orb")
            .pivot(8, 8, 8).glow().unshaded().seeThrough()
            .aura(0x40C0102A, 0.16f, 3);                          // weak on purpose: a strong aura turns the dark core pink
    private static final ModelFx ENTROPY_DISC = ModelFx.of("prince_of_abolition/entropy_disc")   // the ring around it
            .pivot(8, 8, 8).glow().unshaded().seeThrough();
    private static final ModelFx BLADE = ModelFx.of("prince_of_abolition/blade")
            .scale(4f).pivot(8, -16, 8)                           // hangs point down, turns around its tip
            .glow().aura(0xFFFF3A1A, 0.22f, 3);
    // flat plates, one block wide and white: .scale(...) is their width in blocks, .color(...) tints them
    private static final ModelFx RUNE_CIRCLE = ModelFx.of("rune_circle").glow().unshaded().seeThrough();
    private static final ModelFx SHOCK_RING = ModelFx.of("shock_ring").glow().unshaded().seeThrough();

    // =========================
    // Small particles and trails that go with them
    // =========================
    private static final ParticleFx FIRE_EMBER = ParticleFx.of("fx/glow")
            .color(0xFFFFB04A).endColor(0x00C01810)
            .size(0.11f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(14, 10).gravity(-0.03f).friction(0.9f)
            .glow();
    private static final ParticleFx FIRE_SMOKE = ParticleFx.of("fx/smoke")
            .color(0xA0241818).endColor(0x00100808)
            .size(0.35f).endSize(1.0f).sizeRandom(0.3f)
            .lifetime(28, 14).gravity(-0.03f).friction(0.9f)
            .spin(2f).randomRotation();
    private static final ParticleFx DEBRIS = ParticleFx.of("fx/shard")
            .color(0xFF6A5648).endColor(0x00403028)
            .size(0.13f).endSize(0.06f).sizeRandom(0.4f)
            .lifetime(22, 10).gravity(0.9f).friction(0.96f)
            .collide().spin(20f).randomRotation();
    private static final ParticleFx FROST = ParticleFx.of("fx/flake")
            .color(0xFFE8FBFF).fadeOut()
            .size(0.08f).endSize(0.03f)
            .lifetime(16, 8).friction(0.86f).gravity(0.02f)
            .glow().spin(8f).randomRotation();
    private static final ParticleFx ICE_SHARD = ParticleFx.of("fx/shard")
            .color(0xFFF2FCFF).endColor(0x0060D8FF)
            .size(0.12f).endSize(0.05f).sizeRandom(0.4f)
            .lifetime(18, 10).gravity(0.9f).friction(0.96f)
            .glow().collide().spin(25f).randomRotation();
    private static final ParticleFx VOID_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFD01838).endColor(0x00300010)
            .size(0.09f).endSize(0.02f)
            .lifetime(16, 8).friction(0.92f)
            .glow();

    private static final SlashFx SPEAR_TRAIL = SlashFx.line("slash/smooth")       // streak behind the thrown spear
            .color(0xE0FF8A3A).core(0xFFFFF1D6)
            .width(0.22f).taper(SlashFx.Taper.COMET);
    private static final SlashFx WHIRL_TRAIL = SlashFx.arc("slash/edge")           // the whirling hammer's wake
            .color(0xF0FF5A2A).tailColor(0x90A01010).headColor(0xFFFFE0B0)
            .radius(3.0f).arc(330f).width(1.0f).taper(SlashFx.Taper.COMET)
            .layers(2).spread(0.25f)
            .lifetime(18).sweep(6).delay(4).spin(40f);
    private static final SlashFx GROUND_CRACK = SlashFx.line("slash/shatter")      // glowing cracks around an impact
            .color(0xF0FF7A3A).tailColor(0x90FF3A1A).headColor(0xFFFFE8C0)
            .width(0.9f).taper(SlashFx.Taper.COMET)
            .lifetime(14).sweep(3);

    // =========================
    // Melee combo "Edgeflow" (hitting mobs as Prince Of Abolition), registered in combat/MeleeCombos
    // 1 = crossing cut on the target, 2 = rend along the ground, 3 = wide sweep in front,
    // 4 = tall cleave down onto the target, 5 = ring around you. Then it starts over.
    // Every slash starts as a dotted line, turns into a solid blade of light, then shatters into flakes.
    // =========================

    private static final int EDGE_GLOW = 0xF0FFF1D6;   // warm white body
    private static final int EDGE_TAIL = 0x90FFCF94;   // dimmer, warmer tail
    private static final int EDGE_HEAD = 0xFFFFFFFF;   // pure white front
    private static final int EDGE_LEAD = 0xD8FFE2AE;   // dotted lead-in lines

    /** The yellow/orange spikes around whatever gets hit. Set to false to hide them. */
    private static final boolean SHOW_HIT_RAYS = true;

    // Hit 1: two thin cuts crossing on the target (dotted first, then solid)
    private static final SlashFx EDGE_X_LEAD = SlashFx.line("slash/dashed")
            .color(EDGE_LEAD).width(0.11f).taper(SlashFx.Taper.UNIFORM)
            .lifetime(4).sweep(1);
    private static final SlashFx EDGE_X_CUT = SlashFx.line("slash/smooth")
            .color(0xC8FFC878).core(0xFFFFFFFF)
            .width(0.1f).taper(SlashFx.Taper.CRESCENT)
            .lifetime(20).sweep(2).delay(2);

    // Hit 2: a dotted line runs along the ground, then a jagged blade of light tears along it
    private static final float REND_LENGTH = 10f;
    private static final SlashFx EDGE_REND_LEAD = SlashFx.line("slash/dashed")
            .color(EDGE_LEAD).width(0.14f).taper(SlashFx.Taper.UNIFORM)
            .lifetime(12).sweep(2);
    private static final SlashFx EDGE_REND = SlashFx.line("slash/shatter")
            .color(EDGE_GLOW).tailColor(EDGE_TAIL).headColor(EDGE_HEAD)
            .width(1.4f).taper(SlashFx.Taper.COMET)
            .layers(2).spread(0.3f)
            .lifetime(28).sweep(4).delay(3);
    private static final SlashFx EDGE_REND_CORE = SlashFx.line("slash/smooth")
            .color(0xE0FFF4E0).core(0xFFFFFFFF)
            .width(0.16f).taper(SlashFx.Taper.UNIFORM)
            .lifetime(24).sweep(3).delay(3);



    // Hit 3: wide crescent sweeping around the front
    private static final SlashFx EDGE_SWEEP_LEAD = SlashFx.arc("slash/dashed_fine")
            .color(EDGE_LEAD).radius(2.75f).arc(250f).width(0.12f).taper(SlashFx.Taper.UNIFORM)
            .lifetime(6).sweep(3);
    private static final SlashFx EDGE_SWEEP = SlashFx.arc("slash/edge")
            .color(EDGE_GLOW).tailColor(EDGE_TAIL).headColor(EDGE_HEAD)
            .radius(2.6f).arc(225f).width(0.85f).taper(SlashFx.Taper.CRESCENT)
            .layers(2).spread(0.18f)
            .lifetime(10).sweep(3).delay(2);

    // Hit 4: tall slanted cleave from high above down onto the target, then it shatters
    private static final SlashFx EDGE_CLEAVE_LEAD = SlashFx.arc("slash/smooth")
            .color(0xE8FFF0D0).core(0xFFFFFFFF)
            .radius(2.55f).arc(135f).width(0.12f).taper(SlashFx.Taper.CRESCENT)
            .lifetime(6).sweep(2)
            .rotation(0f, -30f, -72f);                  // standing up, slanted, coming down in front
    private static final SlashFx EDGE_CLEAVE = SlashFx.arc("slash/edge")
            .color(EDGE_GLOW).tailColor(EDGE_TAIL).headColor(EDGE_HEAD)
            .radius(2.45f).arc(130f).width(0.8f).taper(SlashFx.Taper.CRESCENT)
            .layers(2).spread(0.18f)
            .lifetime(9).sweep(2).delay(2)
            .rotation(0f, -30f, -72f);
    private static final SlashFx EDGE_CLEAVE_BREAK = SlashFx.arc("slash/shatter")
            .color(0xB8FFF1D6).tailColor(0x50FFCF94).headColor(0xC8FFFFFF)
            .radius(2.45f).arc(120f).width(1.0f).taper(SlashFx.Taper.CRESCENT)
            .layers(2).spread(0.26f)
            .lifetime(6).sweep(1).delay(7)
            .rotation(0f, -30f, -72f);

    // Hit 5: dotted circle -> thin ring of light -> thick spinning blade around you
    private static final SlashFx EDGE_RING_LEAD = SlashFx.arc("slash/dashed_fine")
            .color(EDGE_LEAD).radius(2.7f).arc(360f).width(0.11f).taper(SlashFx.Taper.UNIFORM)
            .lifetime(6).sweep(3);
    private static final SlashFx EDGE_RING_LINE = SlashFx.arc("slash/smooth")
            .color(0xF0FFF1D6).core(0xFFFFFFFF)
            .radius(2.6f).arc(345f).width(0.13f).taper(SlashFx.Taper.UNIFORM)
            .lifetime(8).sweep(2).delay(2);
    private static final SlashFx EDGE_RING = SlashFx.arc("slash/edge")
            .color(EDGE_GLOW).tailColor(EDGE_TAIL).headColor(EDGE_HEAD)
            .radius(2.5f).arc(300f).width(1.1f).taper(SlashFx.Taper.COMET)
            .layers(2).spread(0.25f)
            .lifetime(11).sweep(3).delay(4).spin(14f);

    // Hit sparks (yellow -> orange-red spikes) around whatever gets hit
    private static final SlashFx EDGE_HIT_RAY = SlashFx.line("slash/smooth")
            .color(0xFFFFC23A).tailColor(0xFFFFF27A).headColor(0xFFFF6A1E).core(0xC0FFFFE0)
            .width(0.1f).taper(SlashFx.Taper.CRESCENT)
            .lifetime(7).sweep(2);

    // Broken pieces of the blades, red embers and red mist
    private static final ParticleFx EDGE_FLAKE = ParticleFx.of("fx/flake")
            .color(0xFFFFF4DE).endColor(0x00FFE2B0)
            .size(0.17f).endSize(0.06f).sizeRandom(0.45f)
            .lifetime(12, 10).gravity(-0.05f).friction(0.88f)
            .glow().randomRotation();
    private static final ParticleFx EDGE_SLIVER = ParticleFx.of("fx/sliver")
            .color(0xFFFFF4DE).endColor(0x00FFE2B0)
            .size(0.22f).endSize(0.08f).sizeRandom(0.4f)
            .lifetime(10, 8).gravity(-0.05f).friction(0.88f)
            .glow().randomRotation();
    private static final ParticleFx EDGE_EMBER = ParticleFx.of("fx/glow")
            .color(0xFFFF3A24).endColor(0x00A01010)
            .size(0.035f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(22, 16).friction(0.93f)
            .glow();
    private static final ParticleFx EDGE_MIST = ParticleFx.of("fx/smoke")
            .color(0x88A01C1C).endColor(0x00501010)
            .size(0.3f).endSize(0.75f).sizeRandom(0.3f)
            .lifetime(26, 12).gravity(-0.02f).friction(0.92f)
            .spin(2f).randomRotation();
    private static final ParticleFx EDGE_GROUND_MIST = ParticleFx.of("fx/smoke")
            .color(0x806A2A1C).endColor(0x00402010)
            .size(0.28f).endSize(0.55f).sizeRandom(0.3f)
            .lifetime(30, 10).friction(0.85f)
            .randomRotation();

    public static final MeleeCombo MELEE_COMBO = MeleeCombo.forAspect("Prince Of Abolition")
            .step(PrinceOfAbolition::edgeCrossCut)
            .step(PrinceOfAbolition::edgeGroundRend)
            .step(PrinceOfAbolition::edgeWideSweep)
            .step(PrinceOfAbolition::edgeCleave)
            .step(PrinceOfAbolition::edgeRing)
            .resetAfter(40)
            .minCharge(0.8f);

    // =========================
    // Ability 1 - Spear of Abolition
    // =========================
    public static void princeOfAbolitionSpear(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClass(player, bypassClassCheck)) return;
        if (SoulCore.getAscensionStage(player) < 0) return;
        if (!payEssence(player, SPEAR_COST)) return;

        if (player.isShiftKeyDown()) spearRain(player, sl);
        else spearThrow(player, sl);
    }

    // Normal: one spear appears beside you and flies to where you aim, through everything, until it hits a block
    private static void spearThrow(Player player, ServerLevel sl) {
        Vec3 eye = player.getEyePosition();
        Vec3 from = clearStart(player, sl, eye.add(rightOf(player).scale(0.5)).add(0, -0.3, 0));   // beside the right shoulder
        // It starts beside you, so it is aimed at the point under your crosshair. Flying straight along
        // your look instead would make it land half a block to the right of where you aim.
        Vec3 crosshair = sl.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(SPEAR_RANGE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
        Vec3 look = crosshair.distanceToSqr(from) < 0.25 ? player.getLookAngle().normalize() : crosshair.subtract(from).normalize();
        Vec3 end = from.add(look.scale(SPEAR_RANGE));
        BlockHitResult wall = sl.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        boolean hitWall = wall.getType() != HitResult.Type.MISS;
        Vec3 stop = wall.getLocation();
        double distance = Math.max(0.5, from.distanceTo(stop));
        int flight = Math.max(2, (int) Math.ceil(distance / SPEAR_SPEED));
        int arrive = SPEAR_WINDUP + flight;

        // modelAlong points the spear's own up axis along the flight, so up(...) in the keys moves it along it.
        // It turns around the middle of the shaft, so it stops when the tip is half a block into the wall.
        float travel = (float) (distance - SPEAR_TIP + (hitWall ? 0.5 : 0.0));
        ModelFx spear = SPEAR.lifetime(arrive + (hitWall ? 18 : 3)).fade(0, hitWall ? 8 : 2)
                .key(0, ModelFx.pose().up(-0.3f).scale(0.3f).alpha(0f))
                .key(SPEAR_WINDUP, ModelFx.pose().up(-1.0f).scale(1f).alpha(1f), ModelFx.Ease.OUT_BACK)   // appears, drawn back
                .key(arrive, ModelFx.pose().up(travel));                                                 // flies
        ParticleShapes.modelAlong(sl, spear, from, look);
        ParticleShapes.slashBetween(sl, SPEAR_TRAIL.delay(SPEAR_WINDUP).sweep(flight).lifetime(flight * 2 + 4), from, stop);
        sound(sl, from, SoundEvents.ARROW_SHOOT, 1f, 0.5f);

        for (LivingEntity target : enemiesOnLine(player, sl, from, stop, 0.5)) {
            double along = from.distanceTo(target.getBoundingBox().getCenter());
            int when = SPEAR_WINDUP + (int) Math.round(flight * Math.min(1.0, along / distance));   // when the spear gets there
            Later.run(sl, when, () -> {
                if (!target.isAlive()) return;
                strike(player, target, SPEAR_DAMAGE);
                Vec3 hit = target.getBoundingBox().getCenter();
                edgeHitRays(sl, hit, sl.getRandom(), 0);
                sound(sl, hit, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 1.2f);
            });
        }
        if (hitWall) {
            Later.run(sl, arrive, () -> {
                ParticleShapes.burst(sl, DEBRIS, stop, 12, 0.08, 0.25);
                sound(sl, stop, SoundEvents.ANVIL_LAND, 0.5f, 1.6f);
            });
        }
    }

    // Sneak: spears drop out of the sky around the spot you aim at
    private static void spearRain(Player player, ServerLevel sl) {
        Vec3 center = aimGround(player, sl, SPEAR_RAIN_RANGE);
        RandomSource random = sl.getRandom();
        warningCircle(sl, center, SPEAR_RAIN_SPREAD + 1.0, 0xE0FF5A2A, SPEAR_RAIN_COUNT * 3 + SPEAR_FALL + 8);

        List<LivingEntity> targets = enemiesAround(player, sl, center, SPEAR_RAIN_SPREAD + 1.0);
        for (int i = 0; i < SPEAR_RAIN_COUNT; i++) {
            Vec3 spot;
            if (i < targets.size()) {
                spot = targets.get(i).position();                 // the first spears go for whoever is standing there
            } else {
                double angle = random.nextDouble() * Math.PI * 2;
                double away = Math.sqrt(random.nextDouble()) * SPEAR_RAIN_SPREAD;
                spot = groundAt(player, sl, center.add(Math.cos(angle) * away, 1.0, Math.sin(angle) * away));
            }
            spearFromSky(player, sl, spot, 4 + i * 3, SPEAR_RAIN_DAMAGE, SPEAR_RAIN_HIT);
        }
        sound(sl, center, SoundEvents.ILLUSIONER_CAST_SPELL, 1.2f, 0.7f);
    }

    /** One spear dropping out of the sky onto a spot, 'delay' ticks from now (also used by Frenzy). */
    private static void spearFromSky(Player player, ServerLevel sl, Vec3 ground, int delay, float damage, double radius) {
        RandomSource random = sl.getRandom();
        // Upside down (pitch 180) and leaning a little. Its up axis now points DOWN, so up(-14) is 14 blocks above.
        ModelFx spear = SPEAR.pivot(8, 32, 8)                    // turn around the tip, which lands on the spot
                .delay(delay).lifetime(SPEAR_FALL + 20).fade(0, 8)
                .key(0, ModelFx.pose().up(-14f).alpha(0f))
                .key(2, ModelFx.pose().alpha(1f))
                .key(SPEAR_FALL, ModelFx.pose().up(0.5f), ModelFx.Ease.IN);   // lands with the tip half a block in the ground
        ParticleShapes.model(sl, spear, ground, random.nextFloat() * 360f, 180f + (random.nextFloat() - 0.5f) * 30f, 0f);

        Later.run(sl, delay + SPEAR_FALL, () -> {
            for (LivingEntity target : enemiesAround(player, sl, ground, radius)) strike(player, target, damage);
            edgeHitRays(sl, ground.add(0, 0.4, 0), sl.getRandom(), 0);
            ParticleShapes.burst(sl, DEBRIS, ground.add(0, 0.2, 0), 8, 0.08, 0.22);
            shockRing(sl, ground, radius + 0.4, 0xD0FF8A3A, 7, 0);
            sound(sl, ground, SoundEvents.PLAYER_ATTACK_CRIT, 0.9f, 0.8f);
        });
    }

    // =========================
    // Ability 2 - Corroding Flame
    // =========================
    public static void princeOfAbolitionFlame(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClass(player, bypassClassCheck)) return;
        if (SoulCore.getAscensionStage(player) < 1) return;
        if (!payEssence(player, FLAME_COST)) return;

        if (player.isShiftKeyDown()) {
            pyre(player, sl, aimGround(player, sl, PYRE_RANGE));
        } else {
            Vec3 look = player.getLookAngle().normalize();
            Vec3 from = clearStart(player, sl, player.getEyePosition().add(look.scale(0.9)).add(0, -0.2, 0));
            launchFireball(player, sl, from, look, FIREBALL_RANGE, 1f, FIREBALL_RADIUS, FIREBALL_DAMAGE);
            sound(sl, from, SoundEvents.FIRECHARGE_USE, 1f, 0.8f);
        }
    }

    /**
     * Normal: a fireball from 'from' along 'direction' (length 1) that bursts on the first enemy or block in its way.
     * Whatever is in the burst burns and gets the old Corrosion: wither and poison. Also used by Frenzy.
     */
    private static void launchFireball(Player player, ServerLevel sl, Vec3 from, Vec3 direction, double range,
                                       float size, double radius, float damage) {
        Vec3 end = from.add(direction.scale(range));
        Vec3 wall = sl.clip(new ClipContext(from, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
        LivingEntity first = firstEnemyOnLine(player, sl, from, wall, 0.5);
        double distance = first == null ? from.distanceTo(wall)
                : Math.min(from.distanceTo(wall), from.distanceTo(first.getBoundingBox().getCenter()));
        Vec3 burst = from.add(direction.scale(Math.max(0.0, distance - 0.1)));   // just in front of what it hit, not inside it
        int flight = Math.max(2, (int) Math.ceil(distance / FIREBALL_SPEED));

        ModelFx ball = FIREBALL.scale(size).lifetime(flight)
                .key(0, ModelFx.pose().scale(0.3f))
                .key(3, ModelFx.pose().scale(1f), ModelFx.Ease.OUT_BACK)          // swells as it leaves your hand
                .during(0, flight, ModelFx.pose().up((float) distance));          // flies the whole way at one speed
        ParticleShapes.modelAlong(sl, ball, from, direction);

        for (int tick = 1; tick < flight; tick++) {                             // embers left behind it
            Vec3 at = from.add(direction.scale(distance * tick / flight));
            Later.run(sl, tick, () -> ParticleShapes.burst(sl, FIRE_EMBER, at, 3, 0.01, 0.05));
        }
        Later.run(sl, flight, () -> {
            for (LivingEntity target : enemiesAround(player, sl, burst, radius)) {
                strike(player, target, damage);
                target.setRemainingFireTicks(FIREBALL_BURN_TICKS);
                corrode(target, CORROSION_TICKS);
            }
            fireBurst(sl, burst, radius);
            sound(sl, burst, SoundEvents.GENERIC_EXPLODE.value(), 1f, 1.3f);
        });
    }

    // Sneak: a pillar of fire on the spot you aim at. It burns whoever stands in it and corrupts them (the old sneak Corrosion).
    private static void pyre(Player player, ServerLevel sl, Vec3 ground) {
        warningCircle(sl, ground, PYRE_RADIUS, 0xE0C01818, PYRE_TICKS + 6);
        float width = (float) (PYRE_RADIUS * 1.7);
        ModelFx flames = FLAMES.scale(width, PYRE_HEIGHT / 2f, width)          // the model is 1 block wide and 2 tall
                .lifetime(PYRE_TICKS).fade(0, 10).spin(3f)
                .key(0, ModelFx.pose().scale(0.15f))
                .key(7, ModelFx.pose().scale(1f), ModelFx.Ease.OUT_BACK);      // roars up out of the ground
        ParticleShapes.model(sl, flames, ground, sl.getRandom().nextFloat() * 360f, 0f, 0f);
        sound(sl, ground, SoundEvents.FIRECHARGE_USE, 1.2f, 0.5f);

        Vec3 middle = ground.add(0, 1.0, 0);
        Set<LivingEntity> corrupted = new HashSet<>();            // each enemy is corrupted once, the first time it burns
        for (int tick = 4; tick < PYRE_TICKS; tick += PYRE_PULSE) {
            Later.run(sl, tick, () -> {
                for (LivingEntity target : enemiesAround(player, sl, middle, PYRE_RADIUS)) {
                    strike(player, target, PYRE_DAMAGE);
                    target.setRemainingFireTicks(60);
                    if (corrupted.add(target)) corrupt(target, CORROSION_CORRUPTION);
                }
                ParticleShapes.cone(sl, FIRE_EMBER, ground.add(0, 0.3, 0), UP, 50, 14, 0.08, 0.3);
                ParticleShapes.burst(sl, FIRE_SMOKE, ground.add(0, PYRE_HEIGHT * 0.8, 0), 3, 0.01, 0.04);
                sound(sl, ground, SoundEvents.BLAZE_SHOOT, 0.5f, 0.6f);
            });
        }
    }

    // =========================
    // Ability 3 - Frost
    // =========================
    public static void princeOfAbolitionFrost(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClass(player, bypassClassCheck)) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        if (player.isShiftKeyDown()) {
            LivingEntity target = aimEnemy(player, sl, GLACIER_RANGE);
            if (target == null) {
                player.sendSystemMessage(Component.literal("No one stands before you."));
                return; // nothing spent
            }
            if (!payEssence(player, FROST_COST)) return;
            glacier(player, sl, target);
        } else {
            List<Vec3> spots = spikeSpots(player, sl);
            if (spots.isEmpty()) {
                player.sendSystemMessage(Component.literal("There is no room for the ice."));
                return; // nothing spent
            }
            if (!payEssence(player, FROST_COST)) return;
            iceSpikes(player, sl, spots);
        }
    }

    /** Where the row of spikes comes up: along the ground in front of the caster, over slopes and steps, up to the first wall. */
    private static List<Vec3> spikeSpots(Player player, ServerLevel sl) {
        List<Vec3> spots = new ArrayList<>();
        Vec3 forward = flatLook(player);
        Vec3 walker = player.position().add(0, 1.2, 0);           // runs ahead at chest height to find walls and steps
        for (int i = 0; i < SPIKE_COUNT; i++) {
            Vec3 next = walker.add(forward.scale(i == 0 ? SPIKE_FIRST : SPIKE_SPACING));
            BlockHitResult wall = sl.clip(new ClipContext(walker, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (wall.getType() != HitResult.Type.MISS) break;     // the row stops at walls
            Vec3 ground = groundAt(player, sl, next);
            walker = new Vec3(next.x, ground.y + 1.2, next.z);    // follow slopes and steps
            spots.add(ground);
        }
        return spots;
    }

    // Normal: ice spikes burst out of the ground one after another in a row in front of you
    private static void iceSpikes(Player player, ServerLevel sl, List<Vec3> spots) {
        Set<LivingEntity> alreadyHit = new HashSet<>();           // shared by the whole row: each enemy is hurt once
        for (int i = 0; i < spots.size(); i++) {
            iceSpikeAt(player, sl, spots.get(i), i, 1.15f + i * 0.07f, SPIKE_DAMAGE, SPIKE_FREEZE_TICKS, alreadyHit);
        }
        sound(sl, player.position(), SoundEvents.GLASS_BREAK, 1f, 0.6f);
    }

    /**
     * One ice spike bursting out of the ground: hurts and freezes whatever stands on it (also used by Frenzy).
     * Whoever it hurts is added to 'alreadyHit' and whoever is already in there is left alone,
     * so spikes that share one set hurt each enemy only once.
     */
    private static void iceSpikeAt(Player player, ServerLevel sl, Vec3 ground, int delay, float size, float damage,
                                   int freezeTicks, Set<LivingEntity> alreadyHit) {
        RandomSource random = sl.getRandom();
        float hidden = -2.3f * size;                                             // far enough down to be fully underground
        ModelFx spike = ICE_SPIKE.scale(size).delay(delay).lifetime(SPIKE_LIFE).fade(0, 6)
                .key(0, ModelFx.pose().up(hidden))
                .key(4, ModelFx.pose().up(0f), ModelFx.Ease.OUT_BACK)            // bursts out
                .key(SPIKE_LIFE - 8, ModelFx.pose().up(0f))                      // stands
                .key(SPIKE_LIFE, ModelFx.pose().up(hidden), ModelFx.Ease.IN);    // sinks back
        // It leans a random way. up(...) follows the lean, so it grows along its own length.
        ParticleShapes.model(sl, spike, ground, random.nextFloat() * 360f, (random.nextFloat() - 0.5f) * 26f, 0f);

        Later.run(sl, delay + 2, () -> {
            for (LivingEntity target : enemiesAround(player, sl, ground.add(0, 0.6, 0), SPIKE_HIT)) {
                if (!alreadyHit.add(target)) continue;                           // a spike before this one got it
                strike(player, target, damage);
                Freeze.apply(target, freezeTicks);
            }
            ParticleShapes.burst(sl, FROST, ground.add(0, 0.5, 0), 8, 0.04, 0.16);
            ParticleShapes.cone(sl, ICE_SHARD, ground.add(0, 0.2, 0), UP, 70, 6, 0.15, 0.35);
            sound(sl, ground, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.7f, 0.9f + sl.getRandom().nextFloat() * 0.5f);
        });
    }

    // Sneak: the target you look at is sealed in ice for a long time, with a crown of spikes around it
    private static void glacier(Player player, ServerLevel sl, LivingEntity target) {
        strike(player, target, GLACIER_DAMAGE);
        Freeze.apply(target, GLACIER_FREEZE_TICKS);

        Vec3 feet = target.position();
        double ringRadius = target.getBbWidth() * 0.5 + 0.9;
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2 * i / 6;
            Vec3 out = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            Vec3 spot = groundAt(player, sl, feet.add(out.scale(ringRadius)).add(0, 0.5, 0));
            ModelFx spike = ICE_SPIKE.scale(1.0f + (i % 2) * 0.35f).delay(i).lifetime(GLACIER_FREEZE_TICKS).fade(0, 10)
                    .key(0, ModelFx.pose().up(-3.2f))
                    .key(5, ModelFx.pose().up(0f), ModelFx.Ease.OUT_BACK);
            ParticleShapes.model(sl, spike, spot, yawOf(out), 22f, 0f);          // leaning away from the target
        }
        shockRing(sl, feet, ringRadius + 2.5, 0xE09FE8FF, 10, 0);
        ParticleShapes.burst(sl, FROST, target.getBoundingBox().getCenter(), 24, 0.05, 0.25);
        sound(sl, feet, SoundEvents.GLASS_BREAK, 1.2f, 0.5f);
        sound(sl, feet, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 0.5f);
    }

    // =========================
    // Ability 4 - Gavel of Ruin
    // =========================
    public static void princeOfAbolitionHammer(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClass(player, bypassClassCheck)) return;
        if (SoulCore.getAscensionStage(player) < 3) return;
        if (!payEssence(player, HAMMER_COST)) return;

        if (player.isShiftKeyDown()) {
            hammerWhirl(player, sl);
        } else {
            Vec3 ground = aimGround(player, sl, HAMMERFALL_RANGE);
            hammerFall(player, sl, ground, HAMMERFALL_SIZE, HAMMERFALL_RADIUS, HAMMERFALL_DAMAGE, HAMMERFALL_WARNING);
            sound(sl, player.position(), SoundEvents.BEACON_POWER_SELECT, 1f, 0.5f);
        }
    }

    /**
     * Normal: a hammer drops head first out of the sky onto a spot, 'delay' ticks from now.
     * Whatever it lands on is thrown up and gets the old Bribe: slowness, weakness, mining fatigue. Also used by Frenzy.
     */
    private static void hammerFall(Player player, ServerLevel sl, Vec3 ground, float size, double radius, float damage, int delay) {
        int land = delay + HAMMER_FALL;
        warningCircle(sl, ground, radius, 0xE0FF2A1A, land);

        // Upside down (pitch 180) around the flat top of its head. Its up axis points DOWN, so up(-22) is 22 blocks above.
        ModelFx hammer = HAMMER.scale(size).pivot(8, 30, 8)
                .delay(delay).lifetime(HAMMER_FALL + 26).fade(0, 10)
                .key(0, ModelFx.pose().up(-22f).yaw(-270f).alpha(0f))
                .key(2, ModelFx.pose().alpha(1f))
                .key(HAMMER_FALL, ModelFx.pose().up(0f).yaw(0f), ModelFx.Ease.IN)         // drops, turning as it falls
                .key(HAMMER_FALL + 2, ModelFx.pose().up(-0.3f * size), ModelFx.Ease.OUT)  // small bounce
                .key(HAMMER_FALL + 5, ModelFx.pose().up(0f), ModelFx.Ease.IN);
        ParticleShapes.model(sl, hammer, ground, sl.getRandom().nextFloat() * 360f, 180f, 0f);

        Later.run(sl, land, () -> {
            for (LivingEntity target : enemiesAround(player, sl, ground, radius)) {
                strike(player, target, damage);
                bribeWeaken(target, BRIBE_TICKS);
                target.setDeltaMovement(target.getDeltaMovement().add(0, 0.5, 0));
                target.hurtMarked = true;
            }
            groundSlam(sl, ground, radius);
            sound(sl, ground, SoundEvents.ANVIL_LAND, 1.6f, 0.5f);
            sound(sl, ground, SoundEvents.GENERIC_EXPLODE.value(), 0.9f, 1.1f);
        });
    }

    // Sneak: the hammer appears in your hands and whirls around you twice, knocking everything away
    private static void hammerWhirl(Player player, ServerLevel sl) {
        ModelFx hammer = HAMMER.scale(WHIRL_SIZE).pivot(8, -10, 8)                   // turns around the grip
                .lifetime(26).fade(0, 6)
                .key(0, ModelFx.pose().pitch(85f).scale(0.4f).alpha(0f))
                .key(4, ModelFx.pose().scale(1f).alpha(1f), ModelFx.Ease.OUT_BACK)   // appears held out level in front of you
                .during(4, 20, ModelFx.pose().yaw(720f), ModelFx.Ease.IN_OUT);       // two full turns around you
        ParticleShapes.modelOn(sl, hammer, player, new Vec3(0, 1.0, 0), player.getYRot(), 0f, 0f);
        ParticleShapes.slash(sl, WHIRL_TRAIL, player.position().add(0, 1.0, 0), player.getYRot(), 0f, 0f);
        sound(sl, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.5f);

        for (int tick : new int[]{9, 16}) {                                       // each turn hits once
            Later.run(sl, tick, () -> {
                if (!casterStillHere(player, sl)) return;
                for (LivingEntity target : enemiesAround(player, sl, player.position().add(0, 1.0, 0), WHIRL_RADIUS)) {
                    strike(player, target, WHIRL_DAMAGE);
                    target.knockback(0.9, player.getX() - target.getX(), player.getZ() - target.getZ());
                }
                sound(sl, player.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.7f);
            });
        }
    }

    // =========================
    // Ability 5 - Meteor
    // =========================
    public static void princeOfAbolitionMeteor(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClass(player, bypassClassCheck)) return;
        if (SoulCore.getAscensionStage(player) < 4) return;
        if (!payEssence(player, METEOR_COST)) return;

        Vec3 ground = aimGround(player, sl, METEOR_RANGE);
        if (player.isShiftKeyDown()) {
            // Sneak: a shower of small meteors. The first lands on the spot itself, the rest around it.
            RandomSource random = sl.getRandom();
            for (int i = 0; i < SHOWER_COUNT; i++) {
                Vec3 spot = ground;
                if (i > 0) {
                    double angle = random.nextDouble() * Math.PI * 2;
                    double away = Math.sqrt(random.nextDouble()) * SHOWER_SPREAD;
                    spot = groundAt(player, sl, ground.add(Math.cos(angle) * away, 1.0, Math.sin(angle) * away));
                }
                meteorAt(player, sl, spot, SHOWER_SIZE, SHOWER_RADIUS, SHOWER_DAMAGE, i * 5);
            }
        } else {
            meteorAt(player, sl, ground, METEOR_SIZE, METEOR_RADIUS, METEOR_DAMAGE, 0);
        }
        sound(sl, player.position(), SoundEvents.ENDER_DRAGON_GROWL, 0.8f, 0.5f);
    }

    /** A meteor crashing onto a spot, coming in over the caster's head, 'delay' ticks from now (also used by Frenzy). */
    private static void meteorAt(Player player, ServerLevel sl, Vec3 ground, float size, double radius, float damage, int delay) {
        Vec3 toCaster = new Vec3(player.getX() - ground.x, 0, player.getZ() - ground.z);
        Vec3 back = toCaster.lengthSqr() < 1.0 ? flatLook(player).scale(-1) : toCaster.normalize();
        Vec3 path = back.scale(METEOR_START_BACK).add(0, METEOR_START_HEIGHT, 0);   // from the landing spot up to where it starts
        float length = (float) path.length();
        Vec3 along = path.normalize();
        int land = delay + METEOR_FALL;

        warningCircle(sl, ground, radius, 0xE0FF4A1A, land);
        // Rock and flames share one flight. Their up axis points back up the path, so the flames trail behind.
        ParticleShapes.modelAlong(sl, meteorFlight(METEOR.spin(16f), size, length, delay), ground, along);
        ParticleShapes.modelAlong(sl, meteorFlight(METEOR_TAIL, size, length, delay), ground, along);

        for (int tick = 2; tick < METEOR_FALL; tick += 2) {                     // smoke and embers left along the way
            Vec3 at = ground.add(along.scale(length * (1.0 - (double) tick / METEOR_FALL)));
            Later.run(sl, delay + tick, () -> {
                ParticleShapes.burst(sl, FIRE_EMBER, at, 4, 0.02, 0.1);
                ParticleShapes.burst(sl, FIRE_SMOKE, at, 2, 0.01, 0.04);
            });
        }
        Later.run(sl, land, () -> {
            for (LivingEntity target : enemiesAround(player, sl, ground, radius)) {
                double dx = target.getX() - ground.x;
                double dz = target.getZ() - ground.z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                strike(player, target, damage * (float) Mth.clamp(1.0 - 0.5 * distance / radius, 0.5, 1.0));   // half damage at the edge
                target.setRemainingFireTicks(METEOR_BURN_TICKS);
                if (distance > 0.05) {                                          // thrown away from the crater
                    target.setDeltaMovement(dx / distance * 0.9, 0.5, dz / distance * 0.9);
                    target.hurtMarked = true;
                }
            }
            fireBurst(sl, ground.add(0, 0.6, 0), radius);
            groundSlam(sl, ground, radius);
            ModelFx glow = RUNE_CIRCLE.color(0xB0FF3A14).scale((float) (radius * 1.5)).lifetime(50).fade(0, 40).spin(-1.5f);
            ParticleShapes.model(sl, glow, ground.add(0, 0.05, 0), 0f, 0f, 0f);   // embers left glowing on the ground
            sound(sl, ground, SoundEvents.GENERIC_EXPLODE.value(), 2.5f, 0.6f);
            sound(sl, ground, SoundEvents.LIGHTNING_BOLT_IMPACT, 1.2f, 0.5f);
        });
    }

    /** The meteor's flight: starts 'length' blocks up its path, fades in, reaches the spot after METEOR_FALL ticks. */
    private static ModelFx meteorFlight(ModelFx model, float size, float length, int delay) {
        return model.scale(size).delay(delay).lifetime(METEOR_FALL)
                .key(0, ModelFx.pose().up(length).alpha(0f))
                .during(0, 3, ModelFx.pose().alpha(1f))
                .during(0, METEOR_FALL, ModelFx.pose().up(0f));       // one steady speed all the way down
    }

    // =========================
    // Ability 6 - Entropy
    // =========================
    public static void princeOfAbolitionEntropy(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClass(player, bypassClassCheck)) return;
        if (SoulCore.getAscensionStage(player) < 5) return;
        if (!payEssence(player, ENTROPY_COST)) return;

        if (player.isShiftKeyDown()) {
            // Sneak: the sphere forms over your head and moves with you
            Vec3 above = new Vec3(0, 3.0, 0);
            for (ModelFx part : new ModelFx[]{ENTROPY_ORB.spin(9f), ENTROPY_DISC.spin(-14f)}) {
                ModelFx model = entropyLife(part, ENTROPY_ARRIVE)
                        .key(0, ModelFx.pose().scale(0.15f))
                        .key(ENTROPY_ARRIVE, ModelFx.pose().scale(1f), ModelFx.Ease.OUT_BACK);
                ParticleShapes.modelOn(sl, model, player, above, 0f, 0f, 0f);
            }
            runEntropy(player, sl, () -> player.position().add(0, 1.0, 0));
        } else {
            // Normal: the sphere flies from you to the spot you aim at and stays there
            Vec3 center = aimGround(player, sl, ENTROPY_RANGE).add(0, 1.8, 0);
            Vec3 back = player.getEyePosition().subtract(center);          // from the spot back to the caster
            float flat = (float) Math.sqrt(back.x * back.x + back.z * back.z);
            for (ModelFx part : new ModelFx[]{ENTROPY_ORB.spin(9f), ENTROPY_DISC.spin(-14f)}) {
                // forward/up are measured from the spot, facing the caster: it starts at the caster and ends on the spot
                ModelFx model = entropyLife(part, ENTROPY_ARRIVE)
                        .key(0, ModelFx.pose().forward(flat).up((float) back.y).scale(0.25f))
                        .key(ENTROPY_ARRIVE, ModelFx.pose().forward(0f).up(0f).scale(1f), ModelFx.Ease.OUT);
                ParticleShapes.model(sl, model, center, yawOf(back), 0f, 0f);
            }
            runEntropy(player, sl, () -> center);
        }
        sound(sl, player.position(), SoundEvents.WARDEN_SONIC_BOOM, 0.8f, 0.5f);
    }

    /** The sphere's life once it is in place at tick 'start': it swells with every pulse and collapses at the end. */
    private static ModelFx entropyLife(ModelFx part, int start) {
        int end = start + ENTROPY_TICKS;
        ModelFx model = part.scale(ENTROPY_SIZE).lifetime(end + 5);
        for (int tick = start; tick < end; tick += ENTROPY_PULSE) {
            model = model.during(tick, tick + 3, ModelFx.pose().scale(1.25f), ModelFx.Ease.OUT)
                    .during(tick + 3, tick + ENTROPY_PULSE, ModelFx.pose().scale(1f), ModelFx.Ease.IN_OUT);
        }
        return model.during(end, end + 5, ModelFx.pose().scale(0.05f), ModelFx.Ease.IN);
    }

    /** Pulls, hurts and finally bursts around 'center', which is asked again every time so it can follow the caster. */
    private static void runEntropy(Player player, ServerLevel sl, Supplier<Vec3> center) {
        int end = ENTROPY_ARRIVE + ENTROPY_TICKS;
        for (int tick = ENTROPY_ARRIVE; tick < end; tick += 2) {
            boolean pulse = (tick - ENTROPY_ARRIVE) % ENTROPY_PULSE == 0;
            Later.run(sl, tick, () -> {
                if (casterStillHere(player, sl)) entropyTick(player, sl, center.get(), pulse);   // it dies with its caster
            });
        }
        Later.run(sl, end + 4, () -> {                                           // once it has collapsed
            if (casterStillHere(player, sl)) entropyBurst(player, sl, center.get());
        });
    }

    private static void entropyTick(Player player, ServerLevel sl, Vec3 center, boolean pulse) {
        for (LivingEntity target : enemiesAround(player, sl, center, ENTROPY_PULL_RADIUS)) {
            Vec3 to = center.subtract(target.getBoundingBox().getCenter());
            double distance = to.length();
            if (distance > 1.0) {                                             // dragged toward the middle
                Vec3 motion = target.getDeltaMovement();
                double pull = ENTROPY_PULL / distance;
                target.setDeltaMovement(motion.x * 0.5 + to.x * pull, motion.y * 0.7 + to.y * pull * 0.5, motion.z * 0.5 + to.z * pull);
                target.hurtMarked = true;
            }
            if (pulse && distance <= ENTROPY_HURT_RADIUS) {
                strike(player, target, ENTROPY_PULSE_DAMAGE);
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
                corrupt(target, ENTROPY_PULSE_CORRUPTION);
            }
        }
        if (pulse) {
            ParticleShapes.ring(sl, VOID_MOTE, center, ENTROPY_PULL_RADIUS * 0.8, 20, -0.3);   // motes rushing inward
            ModelFx ring = SHOCK_RING.color(0xD0D01838).lifetime(8)
                    .key(0, ModelFx.pose().scale((float) (ENTROPY_PULL_RADIUS * 1.6)).alpha(0f))
                    .key(8, ModelFx.pose().scale(1f).alpha(1f), ModelFx.Ease.IN);            // a ring closing in on it
            ParticleShapes.model(sl, ring, center.add(0, -0.3, 0), 0f, 0f, 0f);
            sound(sl, center, SoundEvents.WITHER_HURT, 0.4f, 0.5f);
        }
    }

    private static void entropyBurst(Player player, ServerLevel sl, Vec3 center) {
        for (LivingEntity target : enemiesAround(player, sl, center, ENTROPY_BURST_RADIUS)) {
            strike(player, target, ENTROPY_BURST_DAMAGE);
            corrode(target, CORROSION_TICKS);
            Vec3 away = target.getBoundingBox().getCenter().subtract(center);
            double distance = Math.sqrt(away.x * away.x + away.z * away.z);
            if (distance > 0.05) {
                target.setDeltaMovement(away.x / distance * 1.1, 0.45, away.z / distance * 1.1);
                target.hurtMarked = true;
            }
        }
        shockRing(sl, center.add(0, -0.4, 0), ENTROPY_BURST_RADIUS * 1.3, 0xF0FF2A44, 10, 0);
        shockRing(sl, center.add(0, -0.4, 0), ENTROPY_BURST_RADIUS, 0xD0FFD0C0, 7, 0);
        ParticleShapes.burst(sl, VOID_MOTE, center, 60, 0.15, 0.7);
        sound(sl, center, SoundEvents.GENERIC_EXPLODE.value(), 1.4f, 0.5f);
    }

    // =========================
    // Ability 7 - Abolition
    // =========================
    public static void princeOfAbolitionAbolition(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClass(player, bypassClassCheck)) return;
        if (SoulCore.getAscensionStage(player) < 6) return;
        if (!payEssence(player, ABOLITION_COST)) return;

        if (player.isShiftKeyDown()) frenzy(player, sl);
        else bladeOfAbolition(player, sl, aimGround(player, sl, ABOLITION_RANGE));
    }

    // Normal: a colossal blade appears in the air over the spot you aim at, then falls.
    // Everything under it loses its good effects and gets the old Bribe and Corrosion on top of the damage.
    private static void bladeOfAbolition(Player player, ServerLevel sl, Vec3 ground) {
        int land = ABOLITION_WARNING + BLADE_FALL;
        warningCircle(sl, ground, ABOLITION_RADIUS, 0xF0FF2A1A, land);

        // The model hangs point down and its tip sits on the spot, so up(9) lifts the point 9 blocks above it.
        ModelFx blade = BLADE.lifetime(land + 55).fade(0, 16)
                .key(0, ModelFx.pose().up(BLADE_HANG + 4f).scale(0.6f).alpha(0f))
                .key(8, ModelFx.pose().scale(1f).alpha(1f), ModelFx.Ease.OUT)
                .key(ABOLITION_WARNING, ModelFx.pose().up(BLADE_HANG), ModelFx.Ease.IN_OUT)   // hangs over the spot, sinking slowly
                .key(land, ModelFx.pose().up(-1.6f), ModelFx.Ease.IN)                         // falls, point deep into the ground
                .key(land + 3, ModelFx.pose().up(-1.2f), ModelFx.Ease.OUT)
                .during(0, ABOLITION_WARNING, ModelFx.pose().spin(360f), ModelFx.Ease.OUT);   // one turn while it hangs, ends flat side to you
        ParticleShapes.model(sl, blade, ground, player.getYRot(), 0f, 0f);
        sound(sl, ground, SoundEvents.WITHER_SPAWN, 1.5f, 0.5f);

        Later.run(sl, land, () -> {
            for (LivingEntity target : enemiesAround(player, sl, ground, ABOLITION_RADIUS)) {
                abolishBuffs(target);
                strike(player, target, ABOLITION_DAMAGE);
                bribeWeaken(target, BRIBE_TICKS * 2);
                corrode(target, CORROSION_TICKS);
                corrupt(target, ABOLITION_CORRUPTION);
                double dx = target.getX() - ground.x;
                double dz = target.getZ() - ground.z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > 0.05) {
                    target.setDeltaMovement(dx / distance * 1.2, 0.6, dz / distance * 1.2);
                    target.hurtMarked = true;
                }
            }
            groundSlam(sl, ground, ABOLITION_RADIUS);
            shockRing(sl, ground, ABOLITION_RADIUS * 1.5, 0xF0FF3A1A, 14, 3);
            shockRing(sl, ground, ABOLITION_RADIUS * 1.8, 0xC0FFE0B0, 18, 6);
            ModelFx column = FLAMES.scale(4.5f, 7f, 4.5f).lifetime(26).fade(0, 12).spin(6f)   // fire roaring up the blade
                    .key(0, ModelFx.pose().scale(0.3f))
                    .key(6, ModelFx.pose().scale(1f), ModelFx.Ease.OUT);
            ParticleShapes.model(sl, column, ground, 0f, 0f, 0f);
            ParticleShapes.burst(sl, FIRE_EMBER, ground.add(0, 1.0, 0), 80, 0.2, 0.9);
            sound(sl, ground, SoundEvents.LIGHTNING_BOLT_THUNDER, 3f, 0.6f);
            sound(sl, ground, SoundEvents.GENERIC_EXPLODE.value(), 3f, 0.5f);
        });
    }

    /** Strips every beneficial effect from the target. */
    private static void abolishBuffs(LivingEntity target) {
        List<MobEffectInstance> buffs = new ArrayList<>();
        for (MobEffectInstance effect : target.getActiveEffects()) {
            if (effect.getEffect().value().isBeneficial()) buffs.add(effect);
        }
        for (MobEffectInstance effect : buffs) target.removeEffect(effect.getEffect());
    }

    // Sneak: Frenzy. You get all the buffs of the old sneak Bribe, and for a while a random spell
    // hits a random enemy near you every few ticks.
    private static void frenzy(Player player, ServerLevel sl) {
        bribeEmpower(player, FRENZY_BUFF_TICKS);

        ModelFx circle = RUNE_CIRCLE.color(0xE0FF3A24).scale(3.4f).lifetime(FRENZY_TICKS).fade(5, 10).spin(9f);
        ParticleShapes.modelOn(sl, circle, player, new Vec3(0, 0.08, 0), 0f, 0f, 0f);   // burning circle under your feet
        sound(sl, player.position(), SoundEvents.WITHER_SPAWN, 0.8f, 1.4f);

        for (int tick = 4; tick < FRENZY_TICKS; tick += FRENZY_INTERVAL) {
            Later.run(sl, tick, () -> frenzyCast(player, sl));
        }
    }

    private static void frenzyCast(Player player, ServerLevel sl) {
        if (!casterStillHere(player, sl)) return;
        List<LivingEntity> targets = enemiesAround(player, sl, player.position().add(0, 1.0, 0), FRENZY_RANGE);
        if (targets.isEmpty()) return;

        RandomSource random = sl.getRandom();
        LivingEntity target = targets.get(random.nextInt(targets.size()));
        Vec3 spot = target.position();
        switch (random.nextInt(5)) {
            case 0 -> spearFromSky(player, sl, spot, 0, SPEAR_RAIN_DAMAGE, SPEAR_RAIN_HIT);
            case 1 -> {
                Vec3 from = clearStart(player, sl, player.getEyePosition().add(0, 1.2, 0));   // from above your head
                Vec3 aim = target.getBoundingBox().getCenter().subtract(from);
                launchFireball(player, sl, from, aim.normalize(), aim.length() + 2.0, 0.7f, 2.5, 8f);
            }
            case 2 -> iceSpikeAt(player, sl, spot, 0, 1.3f, 8f, 40, new HashSet<>());
            case 3 -> hammerFall(player, sl, spot, 1.3f, 2.5, 12f, 0);
            default -> meteorAt(player, sl, spot, SHOWER_SIZE, SHOWER_RADIUS, SHOWER_DAMAGE, 0);
        }
    }

    // =========================
    // Helpers shared by the abilities
    // =========================

    private static boolean canUseClass(Player player, boolean dontCheck) {
        if (dontCheck) return true;
        return SoulCore.getAspect(player).equals("Prince Of Abolition");
    }

    /** False once the caster died, logged out or went to another dimension: spells that stay around them stop then. */
    private static boolean casterStillHere(Player player, ServerLevel sl) {
        return player.isAlive() && player.level() == sl;
    }

    private static boolean payEssence(Player player, float cost) {
        if (SoulCore.getSoulEssence(player) < cost) {
            player.sendSystemMessage(Component.literal("Not enough soul essence."));
            return false;
        }
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - cost);
        return true;
    }

    /**
     * What the spells hit: anything alive except the caster, what the caster rides or owns (pets, tamed horses),
     * armor stands (decoration, and the invisible ones other aspects use as markers) and spectators.
     */
    private static boolean isEnemy(Player player, LivingEntity other) {
        // the UUID check also covers the caster's new body after dying and respawning while a spell is still running
        if (other == player || other.getUUID().equals(player.getUUID())) return false;
        if (!other.isAlive() || other.isSpectator() || other instanceof ArmorStand) return false;
        if (other == player.getVehicle()) return false;
        return !(other instanceof OwnableEntity owned && player.getUUID().equals(owned.getOwnerUUID()));
    }

    /** Every enemy whose body is within 'radius' of a point. */
    private static List<LivingEntity> enemiesAround(Player player, ServerLevel sl, Vec3 center, double radius) {
        List<LivingEntity> result = new ArrayList<>();
        AABB area = new AABB(center, center).inflate(radius + 2.0);
        for (LivingEntity candidate : sl.getEntitiesOfClass(LivingEntity.class, area, other -> isEnemy(player, other))) {
            AABB body = candidate.getBoundingBox();
            double dx = Mth.clamp(center.x, body.minX, body.maxX) - center.x;   // nearest point of its body to the center
            double dy = Mth.clamp(center.y, body.minY, body.maxY) - center.y;
            double dz = Mth.clamp(center.z, body.minZ, body.maxZ) - center.z;
            if (dx * dx + dy * dy + dz * dz <= radius * radius) result.add(candidate);
        }
        return result;
    }

    /** Every enemy whose body (grown by 'grow' blocks) the line from..to passes through. */
    private static List<LivingEntity> enemiesOnLine(Player player, ServerLevel sl, Vec3 from, Vec3 to, double grow) {
        List<LivingEntity> result = new ArrayList<>();
        if (from.distanceToSqr(to) < 1.0E-6) return result;
        AABB area = new AABB(from, to).inflate(grow + 0.5);
        for (LivingEntity candidate : sl.getEntitiesOfClass(LivingEntity.class, area, other -> isEnemy(player, other))) {
            AABB body = candidate.getBoundingBox().inflate(grow);
            // clip() finds nothing when the line starts inside the box, which is exactly an enemy right on top of you
            if (body.contains(from) || body.clip(from, to).isPresent()) result.add(candidate);
        }
        return result;
    }

    /** The enemy on the line that is closest to 'from', or null. */
    private static LivingEntity firstEnemyOnLine(Player player, ServerLevel sl, Vec3 from, Vec3 to, double grow) {
        LivingEntity closest = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity candidate : enemiesOnLine(player, sl, from, to, grow)) {
            double distance = candidate.getBoundingBox().getCenter().distanceToSqr(from);
            if (distance < best) {
                best = distance;
                closest = candidate;
            }
        }
        return closest;
    }

    /** The enemy the caster is looking at within range (not through walls), or null. */
    private static LivingEntity aimEnemy(Player player, ServerLevel sl, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        Vec3 stop = sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
        return firstEnemyOnLine(player, sl, eye, stop, 0.4);
    }

    /**
     * The spot on the ground the caster is aiming at: under the first enemy in the way,
     * else where the look hits a block, else under the far end of the range.
     */
    private static Vec3 aimGround(Player player, ServerLevel sl, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));
        Vec3 stop = sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
        LivingEntity target = firstEnemyOnLine(player, sl, eye, stop, 0.4);
        if (target != null) return groundAt(player, sl, target.position().add(0, 0.5, 0));
        return groundAt(player, sl, stop.subtract(look.scale(0.3)));           // a step back out of the block that was hit
    }

    /**
     * Top of the blocks under a point. The point itself if there is nothing within 24 blocks below it.
     * It looks down from a little above the point, so a point lying on the ground still finds it. A look that
     * starts inside a block "hits" at once, so when that happens it tries again from other heights:
     * from the point itself (the point is just under a ceiling), then from higher up (the point is inside a hill).
     */
    private static Vec3 groundAt(Player player, ServerLevel sl, Vec3 point) {
        Vec3 to = new Vec3(point.x, point.y - 24.0, point.z);
        for (double lift : new double[]{0.5, 0.0, 1.5, 2.5, 3.5}) {
            Vec3 from = new Vec3(point.x, point.y + lift, point.z);
            BlockHitResult hit = sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) return point;
            if (hit.getLocation().distanceToSqr(from) > 0.0025) return hit.getLocation();   // further than 0.05: a real hit
        }
        return point;
    }

    /**
     * Where something that should appear at 'wanted' can really appear: 'wanted' itself if nothing is between it
     * and the caster's eyes, else just in front of the block in the way. Without this, a spear that starts beside
     * you would start inside the wall whenever you stand next to one, and go nowhere.
     */
    private static Vec3 clearStart(Player player, ServerLevel sl, Vec3 wanted) {
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = sl.clip(new ClipContext(eye, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) return wanted;
        Vec3 back = eye.subtract(hit.getLocation());
        return back.lengthSqr() < 0.04 ? eye : hit.getLocation().add(back.normalize().scale(0.15));
    }

    /** The yaw that faces along a direction. */
    private static float yawOf(Vec3 direction) {
        return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
    }

    /** Horizontal direction to the caster's right. */
    private static Vec3 rightOf(Player player) {
        return flatLook(player).cross(UP).normalize();
    }

    /** Damage from the caster. Clears the short invulnerability after a hit first, so quick hits in a row all count. */
    private static void strike(Player player, LivingEntity target, float damage) {
        target.invulnerableTime = 0;
        target.hurt(player.damageSources().playerAttack(player), damage);
    }

    /** The old Bribe: the target is slowed, weakened and works slower. */
    private static void bribeWeaken(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 1));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ticks, 1));
    }

    /** The old sneak Bribe: every buff it gave the caster, unchanged. */
    private static void bribeEmpower(Player player, int ticks) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ticks, 1));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ticks, 1));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, ticks, 1));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, ticks, 1));
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, ticks, 1));
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, ticks, 1));
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, ticks, 1));
        player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, ticks, 1));
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ticks, 1));
    }

    /** The old Corrosion: wither and poison. */
    private static void corrode(LivingEntity target, int ticks) {
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, ticks, 2));
        target.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, 2));
    }

    /** The old sneak Corrosion: adds corruption (see SoulCore: it weakens more and more, and kills at 100). */
    private static void corrupt(LivingEntity target, int amount) {
        SoulCore.setCorruption(target, SoulCore.getCorruption(target) + amount);
    }

    private static void sound(ServerLevel sl, Vec3 at, SoundEvent event, float volume, float pitch) {
        sl.playSound(null, at.x, at.y, at.z, event, SoundSource.PLAYERS, volume, pitch);
    }

    // =========================
    // Looks shared by the abilities (visual only)
    // =========================

    /** A ring racing outward along the ground until it is 'radius' wide. */
    private static void shockRing(ServerLevel sl, Vec3 ground, double radius, int color, int ticks, int delay) {
        ModelFx ring = SHOCK_RING.color(color).delay(delay).lifetime(ticks)
                .key(0, ModelFx.pose().scale(0.6f))
                .key(ticks, ModelFx.pose().scale((float) (radius * 2.0)), ModelFx.Ease.OUT)
                .during(0, ticks, ModelFx.pose().alpha(0f), ModelFx.Ease.IN);        // stays bright, fades at the end
        ParticleShapes.model(sl, ring, ground.add(0, 0.07, 0), sl.getRandom().nextFloat() * 360f, 0f, 0f);
    }

    /** The turning rune circle that marks where something is about to land. */
    private static void warningCircle(ServerLevel sl, Vec3 ground, double radius, int color, int ticks) {
        ModelFx circle = RUNE_CIRCLE.color(color).scale((float) (radius * 2.0)).lifetime(ticks).fade(0, 4).spin(5f)
                .key(0, ModelFx.pose().scale(0.3f).alpha(0f))
                .key(5, ModelFx.pose().scale(1f).alpha(1f), ModelFx.Ease.OUT_BACK);
        ParticleShapes.model(sl, circle, ground.add(0, 0.05, 0), sl.getRandom().nextFloat() * 360f, 0f, 0f);
    }

    /** A burst of fire: a ball of flame swelling and fading, a ring, embers and smoke. */
    private static void fireBurst(ServerLevel sl, Vec3 center, double radius) {
        ModelFx flash = FIRE_BLAST.spin(25f).lifetime(8)
                .key(0, ModelFx.pose().scale(0.8f))
                .key(8, ModelFx.pose().scale((float) (radius * 1.8)), ModelFx.Ease.OUT)   // ends about as wide as the burst
                .during(2, 8, ModelFx.pose().alpha(0f));
        ParticleShapes.model(sl, flash, center, sl.getRandom().nextFloat() * 360f, 0f, 0f);
        shockRing(sl, center.add(0, -0.3, 0), radius * 1.1, 0xE0FFB060, 8, 0);
        ParticleShapes.burst(sl, FIRE_EMBER, center, (int) (radius * 10), 0.1, 0.12 + radius * 0.08);
        ParticleShapes.burst(sl, FIRE_SMOKE, center, (int) (radius * 3), 0.03, 0.12);
    }

    /** Something heavy hitting the ground: rings, glowing cracks, flying debris, dust. */
    private static void groundSlam(ServerLevel sl, Vec3 ground, double radius) {
        RandomSource random = sl.getRandom();
        shockRing(sl, ground, radius * 1.15, 0xF0FFE0B0, 9, 0);
        shockRing(sl, ground, radius * 0.8, 0xD0FF5A2A, 12, 2);
        int cracks = 5 + (int) radius;
        for (int i = 0; i < cracks; i++) {
            double angle = Math.PI * 2 * (i + random.nextDouble() * 0.6) / cracks;
            Vec3 out = new Vec3(Math.cos(angle), 0, Math.sin(angle));
            Vec3 from = ground.add(out.scale(0.4)).add(0, 0.12, 0);
            ParticleShapes.slashBetween(sl, GROUND_CRACK, from, from.add(out.scale(radius * (0.6 + random.nextDouble() * 0.4))));
        }
        ParticleShapes.cone(sl, DEBRIS, ground.add(0, 0.2, 0), UP, 120, (int) (radius * 8), 0.2, 0.55);
        ParticleShapes.burst(sl, FIRE_SMOKE, ground.add(0, 0.4, 0), (int) (radius * 3), 0.03, 0.12);
    }

    // =========================
    // Melee combo hits
    // =========================

    private static final Vec3 UP = new Vec3(0, 1, 0);

    private static Vec3 flatLook(Player player) {
        return Vec3.directionFromRotation(0f, player.getYRot());
    }

    // Hit 1: two thin cuts cross on the target, red mist bursts out of it
    private static void edgeCrossCut(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        RandomSource random = player.getRandom();
        Vec3 hit = target.getBoundingBox().getCenter();
        Vec3 look = flatLook(player);
        Vec3 side = look.cross(UP).normalize();

        // one long cut rising one way, a shorter one rising the other way, both through the target
        double[][] cuts = {
                {Math.toRadians(38 + random.nextFloat() * 17), 2.3 + random.nextFloat() * 0.3},
                {Math.toRadians(180 - (28 + random.nextFloat() * 17)), 1.7 + random.nextFloat() * 0.3}};
        for (int i = 0; i < cuts.length; i++) {
            double angle = cuts[i][0];
            double half = cuts[i][1];
            Vec3 dir = side.scale(Math.cos(angle)).add(UP.scale(Math.sin(angle)))
                    .add(look.scale((random.nextFloat() - 0.5f) * 0.5f)).normalize();
            Vec3 center = hit.add(ParticleShapes.randomDirectionInCone(random, UP, 360).scale(0.12));
            Vec3 from = center.subtract(dir.scale(half));
            Vec3 to = center.add(dir.scale(half));
            ParticleShapes.slashBetween(sl, EDGE_X_LEAD.delay(i), from, to);
            ParticleShapes.slashBetween(sl, EDGE_X_CUT.delay(2 + i), from, to);
        }

        Vec3 feet = target.position();
        Later.run(sl, 3, () -> {
            for (int i = 0; i < 7; i++) {
                Vec3 velocity = UP.scale(0.05 + random.nextDouble() * 0.06)
                        .add(ParticleShapes.randomDirectionInCone(random, UP, 360).scale(0.01 + random.nextDouble() * 0.04));
                ParticleShapes.spawn(sl, EDGE_MIST, hit.add(ParticleShapes.randomDirectionInCone(random, UP, 360).scale(0.25)), velocity);
            }
            for (int i = 0; i < 4; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                Vec3 out = new Vec3(Math.cos(a), 0, Math.sin(a));
                ParticleShapes.spawn(sl, EDGE_GROUND_MIST, feet.add(0, 0.2, 0).add(out.scale(0.3)), out.scale(0.05).add(0, 0.005, 0));
            }
        });
        edgeHitRays(sl, hit, random, 3);
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9f, 1.5f);
        Later.run(sl, 2, () -> sl.playSound(null, BlockPos.containing(hit), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 1.3f));
    }

    // Hit 2: a dotted line shoots along the ground, then a jagged blade of light tears along it
    private static void edgeGroundRend(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        RandomSource random = player.getRandom();
        Vec3 look = flatLook(player);
        Vec3 side = look.cross(UP).normalize();
        Vec3 feet = player.position().add(0, 0.06, 0);
        Vec3 start = feet.add(look.scale(0.7));
        Vec3 end = feet.add(look.scale(0.7 + REND_LENGTH));

        ParticleShapes.slashBetween(sl, EDGE_REND_LEAD, start.add(0, 0.12, 0), end.add(0, 0.12, 0));
        ParticleShapes.slashBetween(sl, EDGE_REND, start.add(0, 0.3, 0), end.add(0, 0.3, 0));
        ParticleShapes.slashBetween(sl, EDGE_REND_CORE, start.add(0, 0.3, 0), end.add(0, 0.3, 0));

        // pieces break off along the blade, a moment after it passes each spot
        for (int wave = 0; wave < 4; wave++) {
            int w = wave;
            Later.run(sl, 6 + wave, () -> {
                for (int i = 0; i < 7; i++) {
                    double t = (w + random.nextDouble()) / 4.0;
                    Vec3 position = start.add(end.subtract(start).scale(t))
                            .add(side.scale((random.nextDouble() - 0.5) * 0.9)).add(0, 0.1, 0);
                    Vec3 velocity = UP.scale(0.03 + random.nextDouble() * 0.07)
                            .add(side.scale((random.nextDouble() - 0.5) * 0.08));
                    ParticleShapes.spawn(sl, random.nextFloat() < 0.6f ? EDGE_FLAKE : EDGE_SLIVER, position, velocity);
                }
            });
        }
        edgeHitRays(sl, target.getBoundingBox().getCenter(), random, 5);
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.7f);
        Later.run(sl, 3, () -> sl.playSound(null, BlockPos.containing(start), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.2f, 0.7f));
    }

    // Hit 3: a wide crescent sweeps around the front and breaks into flakes
    private static void edgeWideSweep(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        RandomSource random = player.getRandom();
        Vec3 center = player.position().add(0, 1.0, 0);
        float yaw = player.getYRot();
        SlashFx sweep = EDGE_SWEEP.varied(random, 20f, 6f, 12f);       // left or right, a little tilted

        ParticleShapes.slash(sl, EDGE_SWEEP_LEAD.alignedWith(sweep), center, yaw, 0f, 0f);
        ParticleShapes.slash(sl, sweep, center, yaw, 0f, 0f);
        Later.run(sl, 8, () -> {
            ParticleShapes.alongSlash(sl, EDGE_FLAKE, sweep, center, yaw, 0f, 0f, 6f, 12, 0.06, 0.03);
            ParticleShapes.alongSlash(sl, EDGE_SLIVER, sweep, center, yaw, 0f, 0f, 6f, 6, 0.06, 0.03);
            sl.playSound(null, BlockPos.containing(center), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.4f);
        });
        edgeHitRays(sl, target.getBoundingBox().getCenter(), random, 3);
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.0f);
    }

    // Hit 4: a tall slanted cleave comes down from high above onto the target, then shatters
    private static void edgeCleave(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        RandomSource random = player.getRandom();
        float yaw = player.getYRot();
        // the arc's center sits between you and the target, so the blade comes down right through it
        Vec3 center = target.position().subtract(flatLook(player).scale(2.0)).add(0, 1.1, 0);
        SlashFx cleave = EDGE_CLEAVE.varied(random, 15f, 8f, 12f);     // slanted to either side

        ParticleShapes.slash(sl, EDGE_CLEAVE_LEAD.alignedWith(cleave), center, yaw, 0f, 0f);
        ParticleShapes.slash(sl, cleave, center, yaw, 0f, 0f);
        ParticleShapes.slash(sl, EDGE_CLEAVE_BREAK.alignedWith(cleave), center, yaw, 0f, 0f);
        Later.run(sl, 7, () -> {
            ParticleShapes.alongSlash(sl, EDGE_FLAKE, cleave, center, yaw, 0f, 0f, 5f, 14, 0.05, 0.04);
            ParticleShapes.alongSlash(sl, EDGE_SLIVER, cleave, center, yaw, 0f, 0f, 5f, 8, 0.05, 0.04);
            sl.playSound(null, BlockPos.containing(center), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1f, 1.3f);
        });
        edgeHitRays(sl, target.getBoundingBox().getCenter(), random, 3);
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.8f);
        Later.run(sl, 2, () -> sl.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 0.9f));
    }

    // Hit 5: dotted circle -> thin ring of light -> thick blade spinning around you, then flakes
    private static void edgeRing(ServerPlayer player, LivingEntity target, ServerLevel sl) {
        RandomSource random = player.getRandom();
        Vec3 center = player.position().add(0, 0.95, 0);
        float yaw = player.getYRot();
        SlashFx ring = EDGE_RING.varied(random, 180f, 5f, 8f);         // starts anywhere, spins either way

        ParticleShapes.slash(sl, EDGE_RING_LEAD.alignedWith(ring), center, yaw, 0f, 0f);
        ParticleShapes.slash(sl, EDGE_RING_LINE.alignedWith(ring), center, yaw, 0f, 0f);
        ParticleShapes.slash(sl, ring, center, yaw, 0f, 0f);
        Later.run(sl, 11, () -> {
            ParticleShapes.alongSlash(sl, EDGE_FLAKE, ring, center, yaw, 0f, 0f, 7f, 16, 0.07, 0.03);
            ParticleShapes.alongSlash(sl, EDGE_SLIVER, ring, center, yaw, 0f, 0f, 7f, 8, 0.07, 0.03);
            sl.playSound(null, BlockPos.containing(center), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.1f);
        });
        edgeHitRays(sl, target.getBoundingBox().getCenter(), random, 5);
        sl.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.9f);
        Later.run(sl, 4, () -> sl.playSound(null, player.blockPosition(), SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.6f, 1.4f));
    }

    // Yellow/orange spikes shooting out around the hit, plus a few red embers
    private static void edgeHitRays(ServerLevel sl, Vec3 center, RandomSource random, int delay) {
        if (SHOW_HIT_RAYS) {
            for (int i = 0; i < 7; i++) {
                Vec3 dir = ParticleShapes.randomDirectionInCone(random, UP, 360);
                double inner = 0.55 + random.nextDouble() * 0.25;
                double outer = inner + 0.55 + random.nextDouble() * 0.3;
                ParticleShapes.slashBetween(sl, EDGE_HIT_RAY.delay(delay), center.add(dir.scale(inner)), center.add(dir.scale(outer)));
            }
        }
        Later.run(sl, delay, () -> {
            for (int i = 0; i < 6; i++) {
                Vec3 position = center.add(ParticleShapes.randomDirectionInCone(random, UP, 360).scale(0.3));
                Vec3 velocity = ParticleShapes.randomDirectionInCone(random, UP, 360).scale(0.02 + random.nextDouble() * 0.05);
                ParticleShapes.spawn(sl, EDGE_EMBER, position, velocity);
            }
        });
    }
}
