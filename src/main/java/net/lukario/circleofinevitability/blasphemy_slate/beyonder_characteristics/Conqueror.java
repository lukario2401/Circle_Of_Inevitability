package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.pathway_fx.ConquerorFx;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.particles.fx.ModelFx;
import net.lukario.circleofinevitability.particles.fx.ParticleFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.particles.fx.SlashFx;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Conqueror: the Hunter pathway (Red Priest) of Lord of the Mysteries. Each key is one step up the pathway,
 * and every ability has a normal cast and a sneak cast:
 *
 *   1 Hunter's Trap        (Hunter)               a hidden trap that holds and hurts whoever steps on it       | sneak: Explosive Charge, a fuse and a blast
 *   2 Provocation          (Provoker)             a mob drops everything to come for you, and takes more damage | sneak: Repugnant Aura, the mobs around you fight each other
 *   3 Fire Ravens          (Pyromaniac)           a flock of fire ravens hunts the enemies around you            | sneak: Dash, flung the way you look in a streak of fire
 *   4 Flame Transformation (Conspirer)            ride a flame lance, steer it with your look, step out at the end | sneak: Misdirection, vanish and leave a flame figure that blows up
 *   5 Cull                 (Reaper)               a fireball that bursts on what it hits                        | sneak: Cull, a giant scythe reaps the enemy you aim at
 *   6 Galvanization        (Iron-blooded Knight)  a mantle of iron: half damage, blows rebound, strength       | sneak: Iron Charge, a charge that runs through enemies
 *   7 Fog of War           (War Bishop)           a dome of fog: enemies in it are blinded, slowed, weakened    | sneak: War Banner, a banner that blesses allies near it
 *
 * ADDING THE NEXT ABILITY (four places, nothing else):
 *   1. Here: its numbers in "Ability settings", then a method like conquerorHuntersTrap under its own heading.
 *      Start it with ready(player, bypassClassCheck, SEQUENCE), check that the cast makes sense, pay, then do it.
 *   2. network/SKeyPressAbility<Two..Seven>Used: one import static and one line next to the other Pathways'.
 *   3. PathwayAbilities.ALL: one line under "---- Conqueror ----" (the same name and Sequence as here).
 *   4. docs/SEQUENCES.md: its place in the Conqueror row of "Required Sequence of every ability key".
 * Later, if they are wanted: a melee combo (MeleeCombo.forPathway(PATHWAY) here + one register line in
 * combat/MeleeCombos, and this class in SlashFxCommand.SOURCES once it has SlashFx constants), passives
 * (one line in SequencePassives) and a HUD icon (PathwayHudIcon). State on the player goes into
 * player.getPersistentData() under keys that start with "conqueror_"; an event subscriber class in here needs
 * a name nobody else has (ConquerorEvents).
 *
 * What is reusable lives outside this class (see docs/SPELL_KIT.md): combat/Trap (the trap itself),
 * status/Taunt (a mob made to go after one entity: you, or another mob), combat/Transit (travel inside a Shot),
 * combat/Seeker (a model that hunts an enemy: the ravens), status/Vulnerable, status/Concealment, combat/Spells,
 * combat/SpellFx, combat/Later, root/Root. fireBlast (where, how wide, how hard) and fuseBlast (the same
 * after a fuse) below are written so the fire abilities to come can call them too.
 *
 * Nothing here breaks blocks or lights real fire. The numbers for every ability are right below.
 */
public class Conqueror {

    private static final String PATHWAY = "Conqueror";

    // =========================
    // Ability settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Ability 1 - Hunter's Trap (Sequence 9)
    private static final int TRAP_SEQUENCE = 9;
    private static final int TRAP_COST = 250;
    private static final double TRAP_RANGE = 16.0;               // how far away a trap can be set
    private static final double TRAP_RADIUS = 1.5;               // how close to its middle someone must step
    private static final int TRAP_MAX = 3;                       // traps lying at once: one more replaces the oldest
    private static final int TRAP_ARM_TICKS = 20;                // it cannot spring for this long after it was set
    private static final int TRAP_TICKS = 1200;                  // how long it waits before it is gone
    private static final float TRAP_DAMAGE = 40f;                // on everything standing on it when it springs
    private static final int TRAP_HOLD_TICKS = 60;               // how long they are held in place
    private static final int TRAP_GLINT_EVERY = 40;              // ticks between the glints that show where it lies
    private static final int TRAP_COLOR = 0xC0D8DCE4;            // pale steel
    private static final String TRAP_NAME = "conqueror_trap";    // (combat/Trap: the kind of trap)
    private static final int CHARGE_COST = 250;                  // sneak: Explosive Charge
    private static final double CHARGE_RANGE = 16.0;
    private static final int CHARGE_FUSE = 40;                   // ticks from planting it to the blast
    private static final int CHARGE_TICK_EVERY = 10;             // ticks between the sparks and clicks of the fuse
    private static final double CHARGE_RADIUS = 4.0;
    private static final float CHARGE_DAMAGE = 48f;              // in the middle of the blast
    private static final int CHARGE_COLOR = 0xE0FF4A1A;          // the warning circle: red-orange

    // Ability 2 - Provocation (Sequence 8)
    private static final int PROVOKE_SEQUENCE = 8;
    private static final int PROVOKE_COST = 300;                 // Sequence 8 holds 400, so it can be paid from there on
    private static final double PROVOKE_RANGE = 24.0;
    private static final int PROVOKE_TICKS = 200;                // how long the target stays provoked
    private static final float PROVOKE_EXTRA = 0.25f;            // it takes this much more damage meanwhile (0.25 = 25%)
    private static final int PROVOKE_COLOR = 0xD0FF3A2A;         // angry red
    private static final String PROVOKED_MARK = "conqueror_provoked";   // the tag of the circle under a provoked one
    private static final int AURA_COST = 300;                    // sneak: Repugnant Aura
    private static final double AURA_RADIUS = 10.0;              // the mobs this close turn on each other
    private static final int AURA_MAX_TARGETS = 12;              // the nearest this many
    private static final int AURA_TICKS = 200;                   // how long they fight each other
    private static final int AURA_COLOR = 0xC0602A1E;            // murky red-brown

    // Ability 3 - Fire Ravens (Sequence 7)
    private static final int RAVENS_SEQUENCE = 7;
    private static final int RAVENS_COST = 700;                  // Sequence 7 holds 1980
    private static final double RAVENS_RANGE = 24.0;             // the enemies this close (and in sight) are hunted
    private static final int RAVENS_COUNT = 6;                   // ravens in one flock, shared out over the enemies
    private static final int RAVENS_EVERY = 2;                   // ticks between one raven leaving and the next
    private static final float RAVEN_DAMAGE = 10f;               // each raven that reaches its prey
    private static final int RAVEN_BURN_TICKS = 80;              // ...which then burns this long
    private static final double RAVEN_SPEED = 0.9;               // blocks per tick
    private static final double RAVEN_TURN = 14.0;               // degrees a tick it turns after its prey (less = easier to dodge)
    private static final int RAVEN_LIFE = 80;                    // ticks a raven hunts before it burns out
    private static final int DASH_COST = 250;                    // sneak: Dash
    private static final double DASH_SPEED = 4.0;                // blocks per tick along your look, at the start
    private static final double DASH_UP_SHARE = 0.5;             // how much of the up/down part of your look is kept
    private static final double DASH_LIFT = 0.2;                 // a small hop on top, so it leaves the ground
    private static final int DASH_FALL_GUARD_TICKS = 60;         // no fall damage on the next landing within this long
    private static final int DASH_TRAIL_TICKS = 10;              // ticks of fire trailing behind you

    // Ability 4 - Flame Transformation (Sequence 6)
    private static final int FLAME_SEQUENCE = 6;
    private static final int FLAME_COST = 1500;
    // How far the lance flies (blocks of path). Within a Sequence it grows with digestion, from the most of the
    // Sequence before up to the most of this one. From Sequence 3 there is no limit: it flies until a block, an
    // enemy or sneaking stops it (Transit.UNLIMITED).
    private static final double FLAME_RANGE_MIN = 10.0;          // Sequence 6, not digested at all
    private static final double FLAME_RANGE_SEQ6 = 120.0;        // Sequence 6, fully digested
    private static final double FLAME_RANGE_SEQ5 = 360.0;        // Sequence 5, fully digested
    private static final double FLAME_RANGE_SEQ4 = 3600.0;       // Sequence 4, fully digested
    private static final double LANCE_SPEED = 1.2;               // blocks per tick (you fly this fast: slower = easier to steer)
    private static final double LANCE_TURN = 15.0;               // degrees a tick it turns toward where you look
    private static final float LANCE_DAMAGE = 30f;               // on the enemy it flies into (it stops there)
    private static final double FLAME_BURST_RADIUS = 2.5;        // the bursts where you leave and where you arrive
    private static final float FLAME_BURST_DAMAGE = 16f;
    private static final int DECOY_COST = 1500;                  // sneak: Misdirection
    private static final int DECOY_FUSE = 40;                    // ticks until the figure of flame goes off
    private static final double DECOY_RADIUS = 3.5;
    private static final float DECOY_DAMAGE = 36f;
    private static final int DECOY_HIDE_TICKS = 100;              // how long you stay hidden
    private static final int DECOY_SPEED_LEVEL = 2;              // 0 = Speed I, 1 = Speed II while hidden

    // Ability 5 - Cull (Sequence 5)
    private static final int CULL_SEQUENCE = 5;
    private static final int FIREBALL_COST = 400;
    private static final double FIREBALL_RANGE = 48.0;
    private static final double FIREBALL_SPEED = 1.4;            // blocks per tick
    private static final double FIREBALL_RADIUS = 3.5;           // the burst where it lands
    private static final float FIREBALL_DAMAGE = 40f;            // in the middle of the burst (BLAST_EDGE of it at the rim)
    private static final int CULL_COST = 1800;                   // sneak: Cull
    private static final double CULL_RANGE = 20.0;               // how far away the prey can be
    private static final float CULL_DAMAGE = 45f;
    private static final float CULL_EXECUTE_SHARE = 0.3f;        // prey at or under this share of its health is killed outright
    private static final int CULL_BLEED_TICKS = 100;             // Wither afterwards: it bleeds

    // Ability 6 - Galvanization (Sequence 4)
    private static final int GALV_SEQUENCE = 4;
    private static final int GALV_COST = 6000;                   // Sequence 4 holds 32400
    private static final int GALV_TICKS = 400;                   // how long the iron is worn (cast again to take it off)
    private static final float GALV_GUARD = 0.5f;                // share of every blow the iron takes (0.5 = half damage)
    private static final float GALV_REBOUND = 0.3f;              // share of a close blow that goes back to who struck it
    private static final double GALV_REBOUND_REACH = 6.0;        // ...if they are this close (melee, not arrows from afar)
    private static final int GALV_STRENGTH_LEVEL = 1;            // 0 = Strength I, 1 = Strength II
    private static final int IRON_CHARGE_COST = 2000;            // sneak: Iron Charge
    private static final int IRON_CHARGE_TICKS = 8;              // how long you charge
    private static final double IRON_CHARGE_SPEED = 1.8;         // blocks per tick (8 ticks = about 14 blocks)
    private static final double IRON_CHARGE_REACH = 1.8;         // the enemies this close to you are run through
    private static final float IRON_CHARGE_DAMAGE = 80f;         // once per enemy and charge
    private static final double IRON_CHARGE_PUSH = 1.4;
    private static final double IRON_CHARGE_LIFT = 0.5;

    // Ability 7 - Fog of War (Sequence 3)
    private static final int FOG_SEQUENCE = 3;
    private static final int FOG_COST = 12000;                   // Sequence 3 holds 59780
    private static final double FOG_RADIUS = 16.0;
    private static final int FOG_TICKS = 400;                    // cast again to lift it early
    private static final double FOG_MOB_SEES = 4.0;              // a mob in the fog only sees this far (status/Blind)
    private static final float FOG_EXTRA = 0.25f;                // enemies in the fog take this much more damage
    private static final int BANNER_COST = 8000;                 // sneak: War Banner
    private static final double BANNER_RADIUS = 10.0;
    private static final int BANNER_TICKS = 600;                 // one banner at a time: a new one takes the old one's place

    // Fire blasts (the Explosive Charge and Flame Transformation, the fire abilities later)
    private static final float BLAST_EDGE = 0.5f;                // part of the damage that is left at the rim
    private static final double BLAST_PUSH = 1.1;                // how hard it throws back, blocks per tick
    private static final double BLAST_LIFT = 0.45;
    private static final int BLAST_BURN_TICKS = 80;              // how long what it hit keeps burning

    // =========================
    // Small particles that go with them
    // =========================
    private static final ParticleFx TRAP_GLINT = ParticleFx.of("fx/glow")
            .color(0xFFE8ECF4).endColor(0x00707888)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(16, 8).friction(0.9f)
            .glow();
    private static final ParticleFx ANGER_SPARK = ParticleFx.of("fx/spark")
            .color(0xFFFF4A3A).endColor(0x00801010)
            .size(0.12f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(18, 10).friction(0.9f)
            .glow();
    private static final SlashFx PROVOKE_LINE = SlashFx.line("slash/smooth")     // caster to the provoked, for a moment
            .color(0xC0FF3A2A).core(0xE0FFE0D0)
            .width(0.08f).taper(SlashFx.Taper.UNIFORM);

    /** The spear of fire (models/model_fx/conqueror/flame_spear, see ConquerorFx). */
    private static final ModelFx LANCE_MODEL = ConquerorFx.FLAME_SPEAR;
    private static final SlashFx LANCE_STREAK = SlashFx.line("slash/smooth")
            .color(0xC0FF7A1A).core(0xE0FFE8B0)
            .width(0.35f).taper(SlashFx.Taper.COMET);
    /** The flame lance of Flame Transformation. Its range is set per cast (flameRange). */
    private static final Shot FLAME_LANCE = Shot.of(LANCE_MODEL)
            .speed(LANCE_SPEED).width(0.5).trail(LANCE_STREAK);

    /** The fireball of ability 5 (models/model_fx/fireball): blood-red fire that whirls as it flies. */
    private static final ModelFx FIREBALL_MODEL = ModelFx.of("fireball")
            .pivot(8, 8, 8).glow().unshaded().seeThrough()
            .aura(0xFFC01A0A, 0.14f, 3).spin(26f);
    private static final SlashFx FIREBALL_STREAK = SlashFx.line("slash/smooth")
            .color(0xA0E0301A).core(0xD0FFD0A0)
            .width(0.3f).taper(SlashFx.Taper.COMET);
    /** It flies straight and stops at the first enemy or block; what it does there is set per cast. */
    private static final Shot FIREBALL = Shot.of(FIREBALL_MODEL)
            .speed(FIREBALL_SPEED).range(FIREBALL_RANGE).width(0.5).trail(FIREBALL_STREAK);

    /** A raven of fire (models/model_fx/conqueror/fire_raven_0..2: wings up, level, down). */
    private static final ModelFx RAVEN_MODEL = ConquerorFx.FIRE_RAVEN;
    /** One raven of Fire Ravens: hunts its prey (combat/Seeker). What it does on a hit is set per cast. */
    private static final Seeker FIRE_RAVEN = Seeker.of(RAVEN_MODEL)
            .speed(RAVEN_SPEED).turn(RAVEN_TURN).life(RAVEN_LIFE).width(0.5).seek(RAVENS_RANGE)
            .flap(6)
            .trail(SpellFx.FIRE_EMBER, 1)
            .burst(SpellFx.FIRE_EMBER, 14);


    //--------------------------------------------------------------------------------------------------------------------------------------------------------------------------

    private static final int EDGE_HEAD = 0xFF4A0E0E;   // pure front (dark blood red)[cite: 2]
    private static final int EDGE_LEAD = 0xD839150C;   // lead-in lines (dark warm red-copper)[cite: 2]
    private static final int EDGE_GLOW = 0xF0801111;   // vibrant body (dark crimson-red)[cite: 2]
    private static final int EDGE_TAIL = 0x902A0000;   // dimmer tail (darkest blood-black)[cite: 2]


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

    public static final MeleeCombo MELEE_COMBO = MeleeCombo.forPathway("Conqueror")
            .step(Conqueror::edgeCrossCut)
            .step(Conqueror::edgeGroundRend)
            .step(Conqueror::edgeWideSweep)
            .step(Conqueror::edgeCleave)
            .step(Conqueror::edgeRing)
            .resetAfter(40)
            .minCharge(0.8f);

    //---------------------------------------------------------------------------------------------------------------------------------------------------------


    // =========================
    // Ability 1 - Hunter's Trap
    // Normal: a hidden trap on the ground you aim at. After a second it is armed; the first enemy to step on it
    //         springs it, and everything standing on it is hurt and held in place. Up to TRAP_MAX lie at once.
    // Sneak:  Explosive Charge: a charge on the ground you aim at that goes off when its fuse has burnt down.
    // =========================
    public static void conquerorHuntersTrap(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, TRAP_SEQUENCE)) return;

        boolean sneak = player.isShiftKeyDown();
        Vec3 ground = Spells.aimGround(player, sl, sneak ? CHARGE_RANGE : TRAP_RANGE);
        if (!onSolidGround(sl, ground)) {
            player.sendSystemMessage(Component.literal("There is no ground there to set it on."));
            return;                                                  // nothing spent
        }
        if (!Spells.paySpirituality(player, sneak ? CHARGE_COST : TRAP_COST)) return;

        if (sneak) plantCharge(player, sl, ground);
        else setTrap(player, sl, ground);
    }

    /** A circle shows where it is being set, then it lies hidden: only a glint now and then gives it away. */
    private static void setTrap(Player player, ServerLevel sl, Vec3 ground) {
        Trap.at(ground, TRAP_RADIUS)
                .armsAfter(TRAP_ARM_TICKS)
                .lasts(TRAP_TICKS)
                .showEvery(TRAP_GLINT_EVERY)
                .onWait(ticksLeft -> ParticleShapes.ring(sl, TRAP_GLINT, ground.add(0, 0.1, 0), TRAP_RADIUS * 0.6, 6, 0))
                .onSpring(victim -> springTrap(player, sl, ground, victim))
                .set(player, sl, TRAP_NAME, TRAP_MAX);

        SpellFx.warningCircle(sl, ground, TRAP_RADIUS, TRAP_COLOR, TRAP_ARM_TICKS);
        ConquerorFx.trapSet(sl, ground, player.getYRot());           // the bear trap opens, then fades into hiding
        Spells.sound(sl, ground, SoundEvents.CHAIN_PLACE, 0.8f, 0.8f);
    }

    /** The trap closes: the one who stepped on it and everything else standing on it is hurt and held in place. */
    private static void springTrap(Player player, ServerLevel sl, Vec3 ground, LivingEntity victim) {
        List<LivingEntity> caught = Spells.enemiesAround(player, sl, ground.add(0, 0.5, 0), TRAP_RADIUS);
        if (!caught.contains(victim)) caught.add(victim);            // (it may have been in the air above the trap)
        for (LivingEntity target : caught) {
            Spells.strike(player, target, TRAP_DAMAGE);
            Root.apply(target, TRAP_HOLD_TICKS, RootRestriction.MOVEMENT);
        }

        SpellFx.closingRing(sl, ground, TRAP_RADIUS * 1.6, TRAP_COLOR, 6, 0);
        ConquerorFx.trapSnap(sl, ground, victim.getYRot(), TRAP_HOLD_TICKS);   // the jaws slam shut and hold
        ParticleShapes.burst(sl, TRAP_GLINT, ground.add(0, 0.4, 0), 24, 0.08, 0.3);
        Spells.sound(sl, ground, SoundEvents.ANVIL_LAND, 0.5f, 1.8f);
        Spells.sound(sl, ground, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.8f);
        player.sendSystemMessage(Component.literal("Your trap has sprung."));
    }

    /** Sneak: the charge lies in a red circle while its fuse burns, then it goes off. */
    private static void plantCharge(Player player, ServerLevel sl, Vec3 ground) {
        ConquerorFx.powderKeg(sl, ground, player.getYRot() + 180f, CHARGE_FUSE);   // a powder keg, shaking as it burns down
        fuseBlast(player, sl, ground, ConquerorFx.kegFuse(ground), CHARGE_FUSE, CHARGE_RADIUS, CHARGE_DAMAGE);
    }

    // =========================
    // Ability 2 - Provocation
    // Normal: the enemy you look at is provoked for PROVOKE_TICKS. A mob drops everything to come for you
    //         (status/Taunt), and anything provoked, players too, takes PROVOKE_EXTRA more damage meanwhile.
    // Sneak:  Repugnant Aura: the mobs around you turn on each other.
    // =========================
    public static void conquerorProvocation(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, PROVOKE_SEQUENCE)) return;

        if (player.isShiftKeyDown()) repugnantAura(player, sl);
        else provoke(player, sl);
    }

    private static void provoke(Player player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, PROVOKE_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("No one stands before you."));
            return;                                                  // nothing spent
        }
        if (!Spells.paySpirituality(player, PROVOKE_COST)) return;

        Vulnerable.add(target, PROVOKE_EXTRA, PROVOKE_EXTRA, PROVOKE_TICKS);    // a fixed share: casting again does not stack it
        Taunt.apply(target, player, PROVOKE_TICKS);                             // (does nothing to a player: they have no AI)
        if (target instanceof Player other) {
            other.sendSystemMessage(Component.literal("You have been provoked. Every blow lands harder on you for a while."));
        }
        provokeShows(player, sl, target);
    }

    private static void repugnantAura(Player player, ServerLevel sl) {
        List<LivingEntity> near = new ArrayList<>();
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position().add(0, 1, 0), AURA_RADIUS)) {
            if (enemy instanceof Mob && !Marionette.isMarionette(enemy)) near.add(enemy);   // only mobs can be steered
        }
        near.sort(Comparator.comparingDouble(enemy -> enemy.distanceToSqr(player)));
        if (near.size() > AURA_MAX_TARGETS) near = new ArrayList<>(near.subList(0, AURA_MAX_TARGETS));
        if (near.size() < 2) {
            player.sendSystemMessage(Component.literal("There is nothing here to set against each other."));
            return;                                                  // nothing spent
        }
        if (!Spells.paySpirituality(player, AURA_COST)) return;

        Taunt.brawl(near, AURA_TICKS, player.getRandom());
        auraShows(player, sl, near);
    }

    // ---------- what is seen and heard (nothing below changes what happens) ----------

    /** A red line flashes from the caster to the target, sparks fly over its head and a red circle stays under it. */
    private static void provokeShows(Player player, ServerLevel sl, LivingEntity target) {
        SpellFx.tether(sl, PROVOKE_LINE, player, target, 10);
        ConquerorFx.battleCry(sl, player, 30);                       // a war banner rises behind you, a red ring rolls out
        SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth()), PROVOKE_COLOR, PROVOKE_TICKS, PROVOKED_MARK);
        ParticleShapes.burst(sl, ANGER_SPARK, target.position().add(0, target.getBbHeight() + 0.3, 0), 14, 0.05, 0.2);
        Spells.sound(sl, target.position(), SoundEvents.ENDER_DRAGON_GROWL, 0.4f, 1.8f);
        Spells.sound(sl, player.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8f, 0.7f);
    }

    /** A murky wave rolls out from the caster, and every mob that was turned gets a red circle and angry sparks. */
    private static void auraShows(Player player, ServerLevel sl, List<LivingEntity> turned) {
        SpellFx.shockRing(sl, player.position(), AURA_RADIUS, AURA_COLOR, 12, 0);
        SpellFx.shockRing(sl, player.position(), AURA_RADIUS * 0.6, PROVOKE_COLOR, 10, 3);
        for (LivingEntity mob : turned) {
            if (!Taunt.isTaunted(mob)) continue;
            SpellFx.circleUnder(sl, mob, Math.max(0.6, mob.getBbWidth()), PROVOKE_COLOR, AURA_TICKS, PROVOKED_MARK);
            ParticleShapes.burst(sl, ANGER_SPARK, mob.position().add(0, mob.getBbHeight() + 0.3, 0), 8, 0.04, 0.16);
        }
        Spells.sound(sl, player.position(), SoundEvents.WITHER_AMBIENT, 0.6f, 0.6f);
    }


    // =========================
    // Ability 3 - Fire Ravens
    // Normal: a flock of RAVENS_COUNT ravens of fire bursts from you and is shared out over the enemies within
    //         RAVENS_RANGE that you can see. Each one hunts its enemy (combat/Seeker): it turns after it when it runs,
    //         hits it on contact (RAVEN_DAMAGE, set on fire), takes the nearest enemy left if its own dies first,
    //         and bursts on a block in the way.
    // Sneak:  Dash: you are flung the way you look in a streak of fire, and land without fall damage.
    // =========================
    public static void conquerorFireRavens(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, RAVENS_SEQUENCE)) return;

        if (player.isShiftKeyDown()) dash(player, sl);
        else fireRavens(player, sl);
    }

    private static void fireRavens(Player player, ServerLevel sl) {
        Vec3 eye = player.getEyePosition();
        List<LivingEntity> prey = Spells.enemiesAround(player, sl, player.getBoundingBox().getCenter(), RAVENS_RANGE);
        prey.removeIf(enemy -> !Spells.clearLine(player, sl, eye, enemy.getBoundingBox().getCenter()));   // only what you can see
        if (prey.isEmpty()) {
            player.sendSystemMessage(Component.literal("There is no prey in sight."));
            return;                                                  // nothing spent
        }
        if (!Spells.paySpirituality(player, RAVENS_COST)) return;

        prey.sort(Comparator.comparingDouble(enemy -> enemy.distanceToSqr(player)));   // the nearest get theirs first
        Vec3 from = Spells.clearStart(player, sl, eye.add(0, 0.2, 0));
        FIRE_RAVEN.onHit((target, at) -> ravenStrikes(player, sl, target, at))
                .launchAt(player, sl, from, prey, RAVENS_COUNT, RAVENS_EVERY);

        ConquerorFx.ravensErupt(sl, player, from);                   // rings of fire round you, embers where they leave
        Spells.sound(sl, from, SoundEvents.BLAZE_SHOOT, 1f, 1.2f);
        Spells.sound(sl, from, SoundEvents.ENDER_DRAGON_FLAP, 0.8f, 1.8f);
    }

    /** A raven reaches its prey: it hits, sets it alight and bursts. */
    private static void ravenStrikes(Player player, ServerLevel sl, LivingEntity target, Vec3 at) {
        Spells.strike(player, target, RAVEN_DAMAGE);
        target.setRemainingFireTicks(RAVEN_BURN_TICKS);
        ParticleShapes.burst(sl, SpellFx.FIRE_SMOKE, at, 3, 0.02, 0.06);
        Spells.sound(sl, at, SoundEvents.FIRECHARGE_USE, 0.5f, 1.5f);
    }

    /** Sneak: flung the way you look, fire trailing behind, and no fall damage when you come down. */
    private static void dash(Player player, ServerLevel sl) {
        if (!Spells.paySpirituality(player, DASH_COST)) return;

        Vec3 look = player.getLookAngle().normalize();
        player.setDeltaMovement(look.x * DASH_SPEED, look.y * DASH_SPEED * DASH_UP_SHARE + DASH_LIFT, look.z * DASH_SPEED);
        player.hurtMarked = true;                                    // (the client is told about the new speed)
        FallGuard.protect(player, DASH_FALL_GUARD_TICKS);

        ParticleShapes.burst(sl, SpellFx.FIRE_EMBER, player.position().add(0, 0.5, 0), 16, 0.05, 0.25);
        Spells.sound(sl, player.position(), SoundEvents.FIRECHARGE_USE, 0.9f, 0.9f);
        dashTrail(player, sl, DASH_TRAIL_TICKS);
    }

    /** Embers and smoke where you are, every tick for 'ticksLeft' ticks. */
    private static void dashTrail(Player player, ServerLevel sl, int ticksLeft) {
        if (ticksLeft <= 0 || !Spells.casterStillHere(player, sl)) return;
        Vec3 middle = player.getBoundingBox().getCenter();
        ParticleShapes.burst(sl, SpellFx.FIRE_EMBER, middle, 4, 0.0, 0.05);
        if (ticksLeft % 2 == 0) ParticleShapes.spawn(sl, SpellFx.FIRE_SMOKE, middle);
        Later.run(sl, 1, () -> dashTrail(player, sl, ticksLeft - 1));
    }

    // =========================
    // Ability 4 - Flame Transformation
    // Normal: become a flame lance and ride it (combat/Transit, steered): your body and camera fly with it and it
    //         turns toward wherever you look, LANCE_TURN degrees a tick. It ends at a block, at the first enemy it
    //         runs into (LANCE_DAMAGE, set on fire), at the end of its range, or when you sneak; a burst of fire
    //         where you left and where you come out. Its range grows with digestion: Sequence 6 10 -> 120 blocks,
    //         Sequence 5 -> 360, Sequence 4 -> 3600, Sequence 3 and above no limit (flameRange).
    // Sneak:  Misdirection: you vanish, quick and unseen, and leave a figure of flame that blows up behind you.
    // =========================
    public static void conquerorFlameTransformation(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, FLAME_SEQUENCE)) return;

        if (player.isShiftKeyDown()) misdirection(player, sl);
        else flameTransformation(player, sl);
    }

    private static void flameTransformation(Player player, ServerLevel sl) {
        if (!Transit.canGo(player)) {
            player.sendSystemMessage(Component.literal("You cannot leave this place."));
            return;                                                  // nothing spent
        }
        if (!Spells.paySpirituality(player, FLAME_COST)) return;

        double range = flameRange(player);
        Vec3 hand = Spells.clearStart(player, sl, player.getEyePosition().add(Spells.rightOf(player).scale(0.35)).add(0, -0.25, 0));
        Shot lance = FLAME_LANCE.range(range).onHit((target, at) -> {
            Spells.strike(player, target, LANCE_DAMAGE);
            target.setRemainingFireTicks(BLAST_BURN_TICKS);
        });
        Transit.by(lance)
                .steered(LANCE_TURN)
                .letGoOnSneak()
                .onDepart(from -> fireBlast(player, sl, from.add(0, 0.6, 0), FLAME_BURST_RADIUS, FLAME_BURST_DAMAGE))
                .onArrive((at, hitBlock) -> fireBlast(player, sl, at.add(0, 0.6, 0), FLAME_BURST_RADIUS, FLAME_BURST_DAMAGE))
                .go(player, sl, hand, player.getLookAngle());       // steered: it sets off where you look
        Spells.sound(sl, player.position(), SoundEvents.BLAZE_SHOOT, 1f, 0.8f);
    }

    /**
     * How far the lance flies: Sequence 6 goes from FLAME_RANGE_MIN to FLAME_RANGE_SEQ6 as it is digested,
     * Sequence 5 from there to FLAME_RANGE_SEQ5, Sequence 4 on to FLAME_RANGE_SEQ4; Sequence 3 and above: no limit.
     */
    private static double flameRange(Player player) {
        if (Beyonder.hasSequence(player, 3)) return Transit.UNLIMITED;
        float digested = Math.max(0f, Math.min(100f, SoulCore.getDigestion(player))) / 100f;
        if (Beyonder.hasSequence(player, 4)) return FLAME_RANGE_SEQ5 + (FLAME_RANGE_SEQ4 - FLAME_RANGE_SEQ5) * digested;
        if (Beyonder.hasSequence(player, 5)) return FLAME_RANGE_SEQ6 + (FLAME_RANGE_SEQ5 - FLAME_RANGE_SEQ6) * digested;
        return FLAME_RANGE_MIN + (FLAME_RANGE_SEQ6 - FLAME_RANGE_MIN) * digested;
    }

    /** Sneak: a figure of flame stands where you stood and goes off after its fuse; you are hidden and quick meanwhile. */
    private static void misdirection(Player player, ServerLevel sl) {
        if (!Spells.paySpirituality(player, DECOY_COST)) return;

        Vec3 spot = player.position();
        ConquerorFx.flameFigure(sl, spot, player.getYRot(), DECOY_FUSE);   // a figure of flame in your shape
        fuseBlast(player, sl, spot, DECOY_FUSE, DECOY_RADIUS, DECOY_DAMAGE);

        Concealment.hide(player, DECOY_HIDE_TICKS);                 // mobs forget you and go for nothing
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, DECOY_HIDE_TICKS, DECOY_SPEED_LEVEL, false, false, true));
    }

    // =========================
    // Ability 5 - Cull
    // Normal: a fireball flies where you look and bursts on the first enemy or block (fireBlast: FIREBALL_DAMAGE,
    //         thrown back, set on fire).
    // Sneak:  Cull: a giant scythe rises behind the enemy you aim at (or the nearest one in sight) and reaps it
    //         at ConquerorFx.CULL_HITS. It is held in place until then. CULL_DAMAGE, or death outright if it is down
    //         to CULL_EXECUTE_SHARE of its health; then it bleeds (Wither).
    // =========================
    public static void conquerorCull(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, CULL_SEQUENCE)) return;

        if (player.isShiftKeyDown()) fireball(player, sl);
        else cull(player, sl);
    }

    private static void fireball(Player player, ServerLevel sl) {
        if (!Spells.paySpirituality(player, FIREBALL_COST)) return;

        Vec3 look = player.getLookAngle().normalize();
        Vec3 from = Spells.clearStart(player, sl, player.getEyePosition().add(look.scale(0.9)).add(0, -0.2, 0));
        FIREBALL.onEnd((at, hitBlock) -> fireBlast(player, sl, at, FIREBALL_RADIUS, FIREBALL_DAMAGE))
                .fire(player, sl, from, look);
        Spells.sound(sl, from, SoundEvents.BLAZE_SHOOT, 1f, 0.7f);
    }

    private static void cull(Player player, ServerLevel sl) {
        LivingEntity prey = Spells.aimEnemy(player, sl, CULL_RANGE);
        if (prey == null) {
            List<LivingEntity> inSight = Spells.enemiesInSight(player, sl, CULL_RANGE, 30.0);
            if (!inSight.isEmpty()) prey = inSight.get(0);
        }
        if (prey == null) {
            player.sendSystemMessage(Component.literal("There is nothing in sight to reap."));
            return;                                                  // nothing spent
        }
        if (!Spells.paySpirituality(player, CULL_COST)) return;

        LivingEntity target = prey;
        float yaw = Spells.yawOf(target.position().subtract(player.position()));   // the cut faces away from you
        Root.apply(target, ConquerorFx.CULL_HITS, RootRestriction.MOVEMENT);       // it cannot walk out of the swing
        ConquerorFx.cull(sl, target, yaw);
        Spells.sound(sl, target.position(), SoundEvents.WITHER_AMBIENT, 0.8f, 0.6f);

        Later.run(sl, ConquerorFx.CULL_HITS, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            boolean weak = target.getHealth() <= target.getMaxHealth() * CULL_EXECUTE_SHARE;

            Marks.add(target,player, "conqueror_cull",5*Beyonder.powerLevel(player),20*20);
            int marks = Marks.count(target, player, "conqueror_cull");
            Spells.strike(player, target, weak ? target.getMaxHealth() * 10f + 1000f : CULL_DAMAGE*marks);   // through any armour


            if (target.isAlive()) target.addEffect(new MobEffectInstance(MobEffects.WITHER, CULL_BLEED_TICKS, 1));
            Spells.sound(sl, target.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.5f);
            Spells.sound(sl, target.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.6f);
        });
    }

    // =========================
    // Ability 6 - Galvanization
    // Normal: iron closes over your body for GALV_TICKS (cast again to take it off, which costs nothing): it takes
    //         GALV_GUARD of every blow, a blow struck from close by rebounds GALV_REBOUND of itself onto who struck
    //         it, and you have Strength and Fire Resistance meanwhile (ConquerorEvents does the blows).
    // Sneak:  Iron Charge: you charge the way you face for IRON_CHARGE_TICKS; every enemy you run into is hit
    //         once (IRON_CHARGE_DAMAGE) and thrown aside.
    // =========================
    public static void conquerorGalvanization(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, GALV_SEQUENCE)) return;

        if (player.isShiftKeyDown()) ironCharge(player, sl);
        else galvanization(player, sl);
    }

    private static final String GALVANIZED_UNTIL = "conqueror_galvanized_until";   // game time
    private static final String MANTLE_TAG = "conqueror_mantle";

    private static void galvanization(Player player, ServerLevel sl) {
        if (isGalvanized(player)) {                                  // cast again: it comes off
            player.getPersistentData().remove(GALVANIZED_UNTIL);
            ConquerorFx.galvanizeEnds(sl, player, MANTLE_TAG);
            Spells.sound(sl, player.position(), SoundEvents.CHAIN_PLACE, 1f, 0.6f);
            return;
        }
        if (!Spells.paySpirituality(player, GALV_COST)) return;

        long until = sl.getGameTime() + GALV_TICKS;
        player.getPersistentData().putLong(GALVANIZED_UNTIL, until);
        ConquerorFx.galvanize(sl, player, GALV_TICKS, MANTLE_TAG);
        Spells.sound(sl, player.position(), SoundEvents.ANVIL_LAND, 1f, 0.6f);
        Spells.sound(sl, player.position(), SoundEvents.FIRECHARGE_USE, 0.7f, 0.5f);
        galvanizedTick(player, sl, until);
    }

    /** True while the iron is worn. */
    public static boolean isGalvanized(Entity entity) {
        return entity.getPersistentData().getLongOr(GALVANIZED_UNTIL, 0L) > entity.level().getGameTime();
    }

    /** Keeps the strength of the iron up, once a second, until it ends, comes off or a new one replaces it. */
    private static void galvanizedTick(Player player, ServerLevel sl, long until) {
        if (!Spells.casterStillHere(player, sl)) return;
        if (player.getPersistentData().getLongOr(GALVANIZED_UNTIL, 0L) != until || !isGalvanized(player)) return;
        Spells.keepEffect(player, MobEffects.STRENGTH, GALV_STRENGTH_LEVEL, 40);
        Spells.keepEffect(player, MobEffects.FIRE_RESISTANCE, 0, 40);
        Later.run(sl, 20, () -> galvanizedTick(player, sl, until));
    }

    /** A blow on someone galvanized: the iron takes its share, and a close attacker gets some of it back. */
    private static boolean rebounding = false;                       // the blow sent back must not rebound again

    private static void ironTakesBlow(LivingHurtEvent event, LivingEntity victim, ServerLevel sl) {
        float amount = event.getAmount();
        if (amount <= 0f) return;
        event.setAmount(amount * (1f - GALV_GUARD));

        Entity direct = event.getSource().getDirectEntity();
        Vec3 middle = victim.getBoundingBox().getCenter();
        Vec3 struck = direct == null ? middle
                : middle.add(direct.getBoundingBox().getCenter().subtract(middle).normalize().scale(victim.getBbWidth() * 0.6));
        ConquerorFx.ironClang(sl, struck);
        Spells.sound(sl, struck, SoundEvents.ANVIL_LAND, 0.4f, 1.6f);

        if (direct instanceof LivingEntity attacker && attacker != victim && attacker.isAlive()
                && attacker.distanceTo(victim) <= GALV_REBOUND_REACH) {
            rebounding = true;
            try {
                attacker.invulnerableTime = 0;
                attacker.hurt(victim.damageSources().thorns(victim), amount * GALV_REBOUND);
            } finally {
                rebounding = false;
            }
        }
    }

    /** Sneak: a charge along the ground; everyone in the way is hit once and thrown aside. */
    private static void ironCharge(Player player, ServerLevel sl) {
        if (!Spells.paySpirituality(player, IRON_CHARGE_COST)) return;

        Vec3 way = Spells.flatLook(player).normalize();
        FallGuard.protect(player, IRON_CHARGE_TICKS + 40);
        ParticleShapes.burst(sl, ConquerorFx.IRON_SPARK, player.position().add(0, 0.5, 0), 16, 0.06, 0.25);
        Spells.sound(sl, player.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.6f);
        Spells.sound(sl, player.position(), SoundEvents.ENDER_DRAGON_GROWL, 0.5f, 1.6f);
        ironChargeStep(player, sl, way, IRON_CHARGE_TICKS, new HashSet<>());
    }

    private static void ironChargeStep(Player player, ServerLevel sl, Vec3 way, int ticksLeft, Set<UUID> struck) {
        if (!Spells.casterStillHere(player, sl)) return;
        if (ticksLeft <= 0) {                                        // the charge runs out: dust and a ring
            SpellFx.shockRing(sl, player.position(), 3.0, 0xD0B8BCC8, 8, 0);
            ParticleShapes.burst(sl, ConquerorFx.IRON_FLAKE, player.position().add(0, 0.3, 0), 10, 0.05, 0.15);
            return;
        }
        player.setDeltaMovement(way.x * IRON_CHARGE_SPEED, player.getDeltaMovement().y, way.z * IRON_CHARGE_SPEED);
        player.hurtMarked = true;

        Vec3 middle = player.getBoundingBox().getCenter();
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, middle, IRON_CHARGE_REACH)) {
            if (!struck.add(enemy.getUUID())) continue;              // once per enemy
            Spells.strike(player, enemy, IRON_CHARGE_DAMAGE);
            Spells.push(enemy, middle.subtract(way), IRON_CHARGE_PUSH, IRON_CHARGE_LIFT);
            ConquerorFx.ironClang(sl, enemy.getBoundingBox().getCenter());
            Spells.sound(sl, enemy.position(), SoundEvents.ANVIL_LAND, 0.8f, 0.8f);
        }
        ParticleShapes.burst(sl, ConquerorFx.IRON_SPARK, player.position().add(0, 0.3, 0), 4, 0.05, 0.1);
        Later.run(sl, 1, () -> ironChargeStep(player, sl, way, ticksLeft - 1, struck));
    }

    // =========================
    // Ability 7 - Fog of War
    // Normal: a dome of fog FOG_RADIUS wide rolls out from where you stand for FOG_TICKS (cast again to lift it):
    //         enemies in it are blinded (a mob only sees FOG_MOB_SEES blocks and loses what it hunted), slowed,
    //         weakened and take FOG_EXTRA more damage; you are quicker in it.
    // Sneak:  War Banner: a banner rises where you stand for BANNER_TICKS. You and your allies within BANNER_RADIUS
    //         of it get Strength II, Resistance and Regeneration; enemies near it are weakened. One at a time.
    // =========================
    public static void conquerorFogOfWar(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!ready(player, bypassClassCheck, FOG_SEQUENCE)) return;

        if (player.isShiftKeyDown()) warBanner(player, sl);
        else fogOfWar(player, sl);
    }

    private static final String FOG_ZONE = "conqueror_fog_of_war";
    private static final String BANNER_ZONE = "conqueror_war_banner";

    private static void fogOfWar(Player player, ServerLevel sl) {
        if (Zone.isOpen(player, FOG_ZONE)) {                         // cast again: it lifts
            Zone.close(player, FOG_ZONE);
            return;
        }
        if (!Spells.paySpirituality(player, FOG_COST)) return;

        Vec3 center = player.position();
        ConquerorFx.fogOfWar(sl, center, FOG_RADIUS, FOG_TICKS, FOG_ZONE);
        Spells.sound(sl, center, SoundEvents.WITHER_SPAWN, 0.6f, 0.5f);
        Spells.sound(sl, center, SoundEvents.BELL_BLOCK, 1f, 0.5f);

        Zone.at(center, FOG_RADIUS)
                .height(FOG_RADIUS * 0.7 + 2)
                .lasts(FOG_TICKS)
                .every(10)
                .onInside((enemy, ticksInside) -> {
                    Blind.apply(enemy, 30, FOG_MOB_SEES);
                    Spells.keepEffect(enemy, MobEffects.SLOWNESS, 0, 40);
                    Spells.keepEffect(enemy, MobEffects.WEAKNESS, 0, 40);
                    Vulnerable.add(enemy, FOG_EXTRA, FOG_EXTRA, 30);
                })
                .onPulse(ticksLeft -> {
                    if (player.position().distanceTo(center) <= FOG_RADIUS) Spells.keepEffect(player, MobEffects.SPEED, 0, 40);
                })
                .onEnd(() -> ConquerorFx.fogOfWarEnds(sl, center, FOG_ZONE))
                .open(player, sl, FOG_ZONE);
    }

    /** Sneak: a war banner that blesses those who fight with you and weakens those who fight against you. */
    private static void warBanner(Player player, ServerLevel sl) {
        if (!Spells.paySpirituality(player, BANNER_COST)) return;
        if (Zone.isOpen(player, BANNER_ZONE)) Zone.close(player, BANNER_ZONE);   // the old banner goes

        Vec3 ground = player.position();
        ConquerorFx.warBanner(sl, ground, player.getYRot(), BANNER_TICKS, BANNER_ZONE);
        ConquerorFx.battleCry(sl, player, 40);
        SpellFx.steadyRing(sl, ground, BANNER_RADIUS, 0x90FF3A2A, BANNER_TICKS, 0);
        Spells.sound(sl, ground, SoundEvents.BELL_BLOCK, 1.2f, 0.7f);
        Spells.sound(sl, ground, SoundEvents.ENDER_DRAGON_GROWL, 0.6f, 0.8f);

        Zone.at(ground, BANNER_RADIUS)
                .lasts(BANNER_TICKS)
                .every(10)
                .affects(who -> true)                                // enemies and allies: told apart below
                .onInside((who, ticksInside) -> {
                    if (Spells.isEnemy(player, who)) Spells.keepEffect(who, MobEffects.WEAKNESS, 1, 40);
                    else blessedByBanner(who);
                })
                .onPulse(ticksLeft -> {
                    if (player.position().distanceTo(ground) <= BANNER_RADIUS) blessedByBanner(player);
                })
                .onEnd(() -> ParticleShapes.clearModels(sl, ground, BANNER_ZONE))
                .open(player, sl, BANNER_ZONE);
    }

    private static void blessedByBanner(LivingEntity who) {
        Spells.keepEffect(who, MobEffects.STRENGTH, 1, 60);
        Spells.keepEffect(who, MobEffects.RESISTANCE, 0, 60);
        Spells.keepEffect(who, MobEffects.REGENERATION, 0, 60);
    }

    // =========================
    // Helpers shared by the abilities
    // =========================

    /**
     * A blast with a fuse at a spot on the ground: a red warning circle 'radius' wide, sparks and rising clicks
     * for 'fuse' ticks, then fireBlast. Nothing goes off if the caster died or left meanwhile.
     */
    private static void fuseBlast(Player player, ServerLevel sl, Vec3 ground, int fuse, double radius, float damage) {
        fuseBlast(player, sl, ground, ground.add(0, 0.2, 0), fuse, radius, damage);
    }

    /** The same, with the fuse's sparks at 'sparks' (the top of a keg) instead of just above the ground. */
    private static void fuseBlast(Player player, ServerLevel sl, Vec3 ground, Vec3 sparks, int fuse, double radius, float damage) {
        SpellFx.warningCircle(sl, ground, radius, CHARGE_COLOR, fuse);
        Spells.sound(sl, ground, SoundEvents.FIRECHARGE_USE, 0.8f, 0.7f);
        burnFuse(sl, ground, sparks, fuse, fuse);
        Later.run(sl, fuse, () -> {
            if (!Spells.casterStillHere(player, sl)) return;         // nobody left to set it off
            fireBlast(player, sl, ground.add(0, 0.6, 0), radius, damage);
        });
    }

    /** One spark and click of a fuse, then the next one CHARGE_TICK_EVERY ticks later: higher as it gets close. */
    private static void burnFuse(ServerLevel sl, Vec3 ground, Vec3 sparks, int ticksLeft, int fuse) {
        if (ticksLeft <= 0) return;
        ParticleShapes.cone(sl, SpellFx.FIRE_EMBER, sparks, Spells.UP, 40, 6, 0.05, 0.18);
        Spells.sound(sl, ground, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f, 2.0f - (float) ticksLeft / fuse);
        Later.run(sl, CHARGE_TICK_EVERY, () -> burnFuse(sl, ground, sparks, ticksLeft - CHARGE_TICK_EVERY, fuse));
    }

    /**
     * An explosion of fire at a spot: 'damage' in the middle, BLAST_EDGE of it at the rim. What it hits is
     * thrown back and set alight. Breaks no blocks.
     */
    private static void fireBlast(Player player, ServerLevel sl, Vec3 center, double radius, float damage) {
        for (LivingEntity target : Spells.enemiesAround(player, sl, center, radius)) {
            double near = 1.0 - Math.min(1.0, Spells.distanceToBody(target, center) / radius);   // 1 = in the middle, 0 = at the rim
            Spells.strike(player, target, (float) (damage * (BLAST_EDGE + (1.0 - BLAST_EDGE) * near)));
            Spells.push(target, center, BLAST_PUSH, BLAST_LIFT);
            target.setRemainingFireTicks(BLAST_BURN_TICKS);
        }
        SpellFx.fireBurst(sl, center, radius);
        Spells.sound(sl, center, SoundEvents.GENERIC_EXPLODE.value(), 1f, 0.9f);
    }

    /** Has this pathway (or the check is skipped) and has reached the Sequence the ability needs. */
    private static boolean ready(Player player, boolean bypassClassCheck, int sequence) {
        if (!bypassClassCheck && !SoulCore.getPathway(player).equals(PATHWAY)) return false;
        return Beyonder.hasSequence(player, sequence);
    }

    /** True if there is a block right under the spot (so what is set there lies on something). */
    private static boolean onSolidGround(ServerLevel sl, Vec3 spot) {
        return !sl.getBlockState(BlockPos.containing(spot.x, spot.y - 0.2, spot.z)).isAir();
    }

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

        ParticleShapes.slashBetween(sl, EDGE_REND_LEAD, start.add(0, 1.12, 0), end.add(0, 1.12, 0));
        ParticleShapes.slashBetween(sl, EDGE_REND, start.add(0, 1.3, 0), end.add(0, 1.3, 0));
        ParticleShapes.slashBetween(sl, EDGE_REND_CORE, start.add(0, 1.3, 0), end.add(0, 1.3, 0));

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

    // =========================
    // Events
    // =========================

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ConquerorEvents {

        /** Blows on someone galvanized (ability 6) glance off the iron. */
        @SubscribeEvent
        public static void onConquerorHurt(LivingHurtEvent event) {
            LivingEntity victim = event.getEntity();
            if (rebounding || !(victim.level() instanceof ServerLevel sl)) return;
            if (isGalvanized(victim)) ironTakesBlow(event, victim, sl);
        }
    }
}
