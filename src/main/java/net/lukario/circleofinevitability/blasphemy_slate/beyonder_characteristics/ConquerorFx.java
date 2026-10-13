package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Seeker;
import net.lukario.circleofinevitability.combat.SpellFx;
import net.lukario.circleofinevitability.particles.fx.ModelFx;
import net.lukario.circleofinevitability.particles.fx.ModelFx.Ease;
import net.lukario.circleofinevitability.particles.fx.ParticleFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.particles.fx.SlashFx;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * The Conqueror's looks: war, blood, iron and fire. Each display is one call, with its size, timing and animation
 * already chosen, used by the Conqueror abilities (see Conqueror) and free for anything else.
 * The models are in models/model_fx/conqueror/ and are made by tools/model_gen/conqueror_models.py.
 *
 * ---------- See them in game ----------
 *   /conquerorfx all          every display in a ring around you, three times over (/conquerorfx all 5 = five times)
 *   /conquerorfx cull         just that one, 7 blocks in front of you (press TAB after /conquerorfx for the names)
 * Stand on flat ground: they are placed at the height of your feet.
 *
 * ---------- Use them in an ability ----------
 *   ConquerorFx.cull(sl, enemy.position(), player.getYRot(), enemy.getBbHeight());
 *   Later.run(sl, ConquerorFx.CULL_HITS, () -> ...the damage...);
 * Numbers named ..._TICKS say how long a display lasts; the other numbers are the tick something happens (the scythe
 * cuts, the trap snaps), for Later.run. Displays that last as long as you want take a 'ticks' argument.
 * They only show things: none of them hurts, roots or moves anyone.
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class ConquerorFx {

    private ConquerorFx() {}

    private static ModelFx.Pose pose() {
        return ModelFx.pose();
    }

    // =====================================================================================
    // Particles: blood, iron, fog
    // =====================================================================================

    /** Drops of blood: thrown out, they fall. */
    public static final ParticleFx BLOOD = ParticleFx.of("fx/glow")
            .color(0xFFB0080C).endColor(0x80400004)
            .size(0.09f).endSize(0.05f).sizeRandom(0.5f)
            .lifetime(26, 14).gravity(0.07f).friction(0.96f);
    /** A red mist that hangs and spreads. */
    public static final ParticleFx BLOOD_MIST = ParticleFx.of("fx/smoke")
            .color(0x90801010).endColor(0x00300404)
            .size(0.4f).endSize(1.1f).sizeRandom(0.3f)
            .lifetime(30, 14).gravity(-0.01f).friction(0.9f)
            .spin(2f).randomRotation();
    /** Bright red sparks (a blade's edge, a wound opening). */
    public static final ParticleFx BLOOD_SPARK = ParticleFx.of("fx/spark")
            .color(0xFFFF3A2A).endColor(0x00800808)
            .size(0.12f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(14, 8).friction(0.88f)
            .glow();
    /** Sparks of molten iron: white-hot, cooling to red as they fall. */
    public static final ParticleFx IRON_SPARK = ParticleFx.of("fx/spark")
            .color(0xFFFFF0C0).endColor(0x00C02808)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(18, 10).gravity(0.04f).friction(0.92f)
            .glow();
    /** Flakes of iron that break off. */
    public static final ParticleFx IRON_FLAKE = ParticleFx.of("fx/flake")
            .color(0xFF5A585E).endColor(0x00302E32)
            .size(0.14f).endSize(0.08f).sizeRandom(0.4f)
            .lifetime(24, 10).gravity(0.05f).friction(0.92f)
            .randomRotation();
    /** The fog of war: heavy, grey with a red cast, slow. */
    public static final ParticleFx WAR_FOG = ParticleFx.of("fx/smoke")
            .color(0x9C5A4A4A).endColor(0x00281A1A)
            .size(1.4f).endSize(2.6f).sizeRandom(0.35f)
            .lifetime(56, 20).friction(0.96f)
            .spin(0.6f).randomRotation();
    /** Embers drifting in the fog. */
    public static final ParticleFx WAR_EMBER = ParticleFx.of("fx/glow")
            .color(0xFFFF5A2A).endColor(0x00801010)
            .size(0.05f).endSize(0.02f).sizeRandom(0.5f)
            .lifetime(40, 20).gravity(-0.01f).friction(0.97f)
            .glow();

    // =====================================================================================
    // Hunter's Trap: the bear trap
    // =====================================================================================

    /** A steel bear trap, 2 blocks wide. Frames: 0 = open, 2 = shut. */
    public static final ModelFx BEAR_TRAP = ModelFx.of("conqueror/bear_trap").frames(3).smooth().scale(2f);

    /** How long the trap stays in sight when it is set before it fades into hiding. */
    public static final int TRAP_SET_TICKS = 30;

    /**
     * Setting the trap: it drops onto the ground shut, its jaws spring open, and it fades into hiding over the
     * last ticks of TRAP_SET_TICKS (it is still there, only unseen).
     */
    public static void trapSet(ServerLevel sl, Vec3 ground, float yaw) {
        ModelFx trap = BEAR_TRAP.lifetime(TRAP_SET_TICKS).fade(0, 12)
                .key(0, pose().frame(2).up(0.6f).scale(0.6f))
                .key(5, pose().frame(2).up(0f).scale(1f), Ease.OUT_BOUNCE)
                .key(11, pose().frame(0), Ease.OUT_BACK);                 // the jaws are pulled open
        ParticleShapes.model(sl, trap, ground.add(0, 0.02, 0), yaw, 0f, 0f);
        Later.run(sl, 9, () -> ParticleShapes.burst(sl, IRON_SPARK, ground.add(0, 0.2, 0), 6, 0.03, 0.12));
    }

    /** The tick the jaws are shut, for trapSnap. */
    public static final int TRAP_SNAPS = 2;

    /**
     * The trap springs: it shows open, the jaws slam shut in two ticks, it jolts, holds 'ticks' ticks (keep it as
     * long as the one caught is held) and fades. Sparks fly and blood sprays when it shuts.
     */
    public static void trapSnap(ServerLevel sl, Vec3 ground, float yaw, int ticks) {
        int holds = Math.max(TRAP_SNAPS + 10, ticks);
        ModelFx trap = BEAR_TRAP.lifetime(holds).fade(0, 10)
                .key(0, pose().frame(0))
                .key(TRAP_SNAPS, pose().frame(2), Ease.IN)                // slams shut
                .during(TRAP_SNAPS, TRAP_SNAPS + 2, pose().up(0.15f).roll(4f), Ease.OUT)
                .during(TRAP_SNAPS + 2, TRAP_SNAPS + 6, pose().up(0f).roll(0f), Ease.OUT_BOUNCE);
        ParticleShapes.model(sl, trap, ground.add(0, 0.02, 0), yaw, 0f, 0f);
        Later.run(sl, TRAP_SNAPS, () -> {
            Vec3 jaws = ground.add(0, 0.5, 0);
            ParticleShapes.burst(sl, IRON_SPARK, jaws, 16, 0.08, 0.3);
            ParticleShapes.cone(sl, BLOOD, jaws, new Vec3(0, 1, 0), 120, 18, 0.1, 0.3);
            ParticleShapes.burst(sl, BLOOD_MIST, jaws, 4, 0.01, 0.04);
            SpellFx.shockRing(sl, ground, 1.8, 0xC0D8DCE4, 6, 0);
        });
    }

    // =====================================================================================
    // Explosive Charge: the powder keg
    // =====================================================================================

    /** A powder keg, 0.8 blocks wide, its fuse burning. */
    public static final ModelFx POWDER_KEG = ModelFx.of("conqueror/powder_keg").scale(1.3f);

    /** Where the fuse burns on a keg played at 'ground': for the sparks. */
    public static Vec3 kegFuse(Vec3 ground) {
        return ground.add(0, 1.25, 0);
    }

    /**
     * The keg drops onto the ground with a bounce and sits there for 'fuse' ticks, shaking harder and harder,
     * swelling a little at the very end; it is gone when the fuse is out (the blast takes its place).
     */
    public static void powderKeg(ServerLevel sl, Vec3 ground, float yaw, int fuse) {
        int ticks = Math.max(10, fuse);
        ModelFx keg = POWDER_KEG.lifetime(ticks)
                .key(0, pose().up(1.4f))
                .key(8, pose().up(0f), Ease.OUT_BOUNCE);
        // a shake that gets quicker and wider as the fuse burns down (a few .during each way)
        int t = 8;
        int step = 6;
        float swing = 1.5f;
        boolean left = true;
        while (t + step < ticks - 3 && keg.keys().size() < 56) {
            keg = keg.during(t, t + step, pose().roll(left ? swing : -swing), Ease.IN_OUT);
            t += step;
            left = !left;
            swing = Math.min(8f, swing + 0.6f);
            step = Math.max(2, step - (t > ticks / 2 ? 1 : 0));
        }
        keg = keg.during(ticks - 3, ticks, pose().scale(1.25f).roll(0f), Ease.IN);
        ParticleShapes.model(sl, keg, ground, yaw, 0f, 0f);
    }

    // =====================================================================================
    // Flame Transformation: the flame spear, and the figure of flame (Misdirection)
    // =====================================================================================

    /**
     * The spear of fire, 2.4 blocks long, its top (the point) its front: for a Shot or for riding it (Transit).
     * Its pivot is near the back, so held in front of the eyes it reaches forward and stays out of the face.
     */
    public static final ModelFx FLAME_SPEAR = ModelFx.of("conqueror/flame_spear")
            .scale(0.8f).pivot(8, -6, 8).glow().unshaded().seeThrough()
            .aura(0xFFFF6A1A, 0.12f, 3).spin(14f);

    /** A figure of flame in your shape, 2 blocks tall: the decoy. */
    public static final ModelFx FLAME_FIGURE = ModelFx.of("conqueror/flame_figure")
            .scale(0.95f).glow().unshaded().seeThrough().aura(0xC0FF5A1A, 0.1f, 2);

    /**
     * The figure of flame rises out of a burst of fire where 'spot' is, looks the way 'yaw' looks, stands burning
     * and swaying for 'ticks' ticks, and swells in its last ticks (the blast comes then).
     */
    public static void flameFigure(ServerLevel sl, Vec3 spot, float yaw, int ticks) {
        int lasts = Math.max(12, ticks);
        ModelFx figure = FLAME_FIGURE.lifetime(lasts).fade(0, 3)
                .key(0, pose().scale(0.4f).alpha(0f))
                .key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK)
                .during(lasts - 6, lasts, pose().scale(1.3f).alpha(0.6f), Ease.IN);
        for (int t = 6; t + 10 < lasts - 6 && figure.keys().size() < 56; t += 10) {
            figure = figure.during(t, t + 10, pose().roll(((t / 10) % 2 == 0) ? 3f : -3f), Ease.IN_OUT);
        }
        ParticleShapes.model(sl, figure, spot, yaw, 0f, 0f);
        ModelFx base = SpellFx.FLAMES.scale(1.1f, 0.35f, 1.1f).lifetime(lasts).fade(4, 4).spin(4f);
        ParticleShapes.model(sl, base, spot, yaw, 0f, 0f);
        ParticleShapes.burst(sl, SpellFx.FIRE_EMBER, spot.add(0, 1, 0), 18, 0.05, 0.2);
    }

    // =====================================================================================
    // Provocation and Fog of War: the war banner
    // =====================================================================================

    /** A tattered war banner on a pole, 3.6 blocks tall. Frames: blown back and forth. Cloth = front. */
    public static final ModelFx WAR_BANNER = ModelFx.of("conqueror/war_banner").frames(3).smooth()
            .scale(1.2f).pivot(8, -16, 8);

    /** Shortest time a banner can be shown (it needs this long to rise and sink). */
    public static final int BANNER_SHORTEST = 24;

    /**
     * A war banner rises out of the ground at 'ground', its cloth toward where 'yaw' looks, waves in the wind for
     * 'ticks' ticks and sinks in the last of them. 'tag' names it so it can be taken away early (null = none).
     */
    public static void warBanner(ServerLevel sl, Vec3 ground, float yaw, int ticks, String tag) {
        ParticleShapes.model(sl, banner(ticks, 0, tag), ground, yaw, 0f, 0f);
        Later.run(sl, 4, () -> ParticleShapes.burst(sl, IRON_FLAKE, ground.add(0, 0.2, 0), 8, 0.05, 0.15));
    }

    /** The banner as a ModelFx: rises, waves, sinks. 'delay' ticks before it shows. */
    private static ModelFx banner(int ticks, int delay, String tag) {
        int lasts = Math.max(BANNER_SHORTEST, ticks);
        float height = 3.6f;
        ModelFx flag = WAR_BANNER.lifetime(lasts).delay(delay).fade(0, 6)
                .key(0, pose().up(-height).frame(1))
                .key(8, pose().up(0f), Ease.OUT_BACK)                     // thrust up out of the ground
                .during(lasts - 10, lasts, pose().up(-height), Ease.IN);   // and sinks back
        // waving: 0 -> 2 -> 0, a little irregular so a row of them does not move as one
        int t = 4;
        boolean out = true;
        while (t < lasts - 10 && flag.keys().size() < 56) {
            int beat = 9 + (t * 7) % 5;
            flag = flag.during(t, Math.min(lasts - 10, t + beat), pose().frame(out ? 2f : 0f), Ease.IN_OUT);
            t += beat;
            out = !out;
        }
        if (tag != null) flag = flag.tag(tag);
        return flag;
    }

    /** A battle cry: a banner rises behind 'who' (as long as 'ticks'), a red ring rolls out, angry sparks. */
    public static void battleCry(ServerLevel sl, Entity who, int ticks) {
        float yaw = who.getYRot();
        Vec3 behind = who.position().add(ahead(yaw).scale(-1.2));
        warBanner(sl, behind, yaw + 180f, ticks, null);
        SpellFx.shockRing(sl, who.position(), 6.0, 0xC0FF3A2A, 10, 0);
        SpellFx.shockRing(sl, who.position(), 3.5, 0xA0602A1E, 8, 3);
        ParticleShapes.burst(sl, BLOOD_SPARK, who.position().add(0, 1.4, 0), 12, 0.05, 0.25);
    }

    // =====================================================================================
    // Cull (Reaper): the scythe that cuts down diagonally through its prey
    // =====================================================================================

    /** The Reaper's scythe, 3.6 blocks tall: a black blade with a blood-red edge. Pivot at the lower grip. */
    public static final ModelFx REAPER_SCYTHE = ModelFx.of("conqueror/reaper_scythe")
            .scale(1.6f).pivot(4, -12, 8).glow().aura(0xC0B00A0A, 0.08f, 3);

    public static final int CULL_TICKS = 40;
    /** The tick the blade goes through the prey: put the damage here. */
    public static final int CULL_HITS = 16;
    private static final int CULL_RAISED = 11;                       // the scythe is drawn back, ready
    private static final float CULL_TILT = 38f;                      // how far the swing leans from upright: a diagonal cut

    /*
     * How the swing works. The scythe is turned side-on (played at yaw - 90), so its blade, which reaches out to the
     * model's +X, points the way the cut goes and the whole scythe lies in one upright plane through the prey. That
     * plane leans CULL_TILT degrees to one side (the model's pitch). The swing is a ROLL inside that plane, around the
     * grip: from drawn back (CULL_BACK) over the top and down through the prey (CULL_THROUGH) to the follow-through.
     * The blade's hook leads all the way and its edge goes through the prey, not the shaft.
     */
    private static final float CULL_BACK = -80f;                     // drawn back: the blade high behind the grip
    private static final float CULL_THROUGH = 70f;                   // at CULL_HITS: the middle of the blade on the prey
    private static final float CULL_AFTER = 140f;                    // where the swing runs out
    /** The middle of the blade lies this many degrees ahead of the shaft (seen from the grip). */
    private static final float CULL_BLADE_AHEAD = 20.7f;
    /** Blocks from the grip to the middle of the blade (it is all about this far: the crescent's radius). */
    private static final double CULL_REACH = 3.86;

    // the cut itself: a wide crimson crescent along the blade's path, its dotted lead, a white-hot core, the pieces
    private static final SlashFx CULL_LEAD = SlashFx.arc("slash/dashed")
            .color(0xC0601010).radius(3.95f).arc(150f).width(0.12f).taper(SlashFx.Taper.UNIFORM)
            .lifetime(12).sweep(5);
    private static final SlashFx CULL_CUT = SlashFx.arc("slash/edge")
            .color(0xF0A00C0C).tailColor(0x90300000).headColor(0xFFFF4A3A).core(0xE0FFE0D0)
            .radius(3.86f).arc(170f).width(1.4f).taper(SlashFx.Taper.CRESCENT)
            .layers(3).spread(0.22f)
            .lifetime(14).sweep(3);
    private static final SlashFx CULL_CORE = SlashFx.arc("slash/smooth")
            .color(0xFFFFF0E0).core(0xFFFFFFFF)
            .radius(3.86f).arc(160f).width(0.18f).taper(SlashFx.Taper.CRESCENT)
            .lifetime(10).sweep(3);
    private static final SlashFx CULL_BREAK = SlashFx.arc("slash/shatter")
            .color(0xC0C01010).tailColor(0x40400000).headColor(0xE0FF6040)
            .radius(3.95f).arc(150f).width(1.6f).taper(SlashFx.Taper.CRESCENT)
            .layers(2).spread(0.3f)
            .lifetime(10).sweep(1).delay(6);
    private static final SlashFx CULL_RAY = SlashFx.line("slash/smooth")
            .color(0xFFFF3A2A).headColor(0xFFFFE0D0).core(0xC0FFFFFF)
            .width(0.09f).taper(SlashFx.Taper.CRESCENT)
            .lifetime(8).sweep(2);

    /** Cull on an entity: the same as below, sized and placed for it. */
    public static void cull(ServerLevel sl, Entity prey, float yaw) {
        cull(sl, prey.position(), yaw, prey.getBbHeight());
    }

    /**
     * Cull: red mist is drawn in to the prey, a blood-red circle opens under it and a dotted crescent marks the cut
     * to come. A giant scythe fades in, side-on, drawn back high behind the prey; at CULL_HITS it sweeps over and
     * down diagonally, hook first, its blade through the prey: a crimson crescent along its path, blood thrown the way
     * the blade goes, red rays and sparks, the ground splashed; the scythe sweeps on and crumbles into red embers.
     * 'feet' = where the prey stands, 'yaw' = the way the cut goes (the caster's look), 'height' = how tall it is.
     */
    public static void cull(ServerLevel sl, Vec3 feet, float yaw, double height) {
        RandomSource random = sl.getRandom();
        Vec3 middle = feet.add(0, Math.max(0.5, height * 0.55), 0);
        Vec3 forward = ahead(yaw);
        boolean fromLeft = random.nextBoolean();
        float tilt = fromLeft ? CULL_TILT : -CULL_TILT;
        // the swing's plane: 'forward' and 'upright' (straight up, leaning 'tilt' degrees to the left; - = right)
        Vec3 left = ahead(yaw - 90f);
        double lean = Math.toRadians(tilt);
        Vec3 upright = new Vec3(0, Math.cos(lean), 0).add(left.scale(Math.sin(lean)));
        // the grip: the middle of the blade, CULL_REACH from it, reaches the prey's middle at CULL_THROUGH
        Vec3 grip = middle.subtract(onSwing(forward, upright, CULL_THROUGH + CULL_BLADE_AHEAD, CULL_REACH));
        // the crescents lie in the same plane, centered on the grip: they trace the blade (SlashFx roll -90 + tilt)
        float roll = -90f + tilt;

        // 1. the mark: a blood circle, mist drawn in, the dotted crescent of the cut
        SpellFx.runeCircle(sl, feet, 2.2, 0xD0A00A0A, CULL_TICKS, fromLeft ? -6f : 6f);
        SpellFx.closingRing(sl, feet, 5.0, 0xA0601010, CULL_HITS - 2, 0);
        for (int i = 0; i < 5; i++) {
            Later.run(sl, i * 2, () -> ParticleShapes.ring(sl, BLOOD_MIST, middle.add(0, -0.4, 0), 4.0, 14, -0.18));
        }
        ParticleShapes.slash(sl, CULL_LEAD, grip, yaw, 0f, roll);

        // 2. the scythe, side-on (yaw - 90), leaning 'tilt' (pitch), swung by its roll around the grip
        ModelFx scythe = REAPER_SCYTHE.lifetime(CULL_TICKS).fade(0, 8)
                .key(0, pose().roll(CULL_BACK + 40f).scale(0.7f).alpha(0f))
                .key(CULL_RAISED, pose().roll(CULL_BACK).scale(1f).alpha(1f), Ease.OUT)    // rises and is drawn back
                .key(CULL_HITS, pose().roll(CULL_THROUGH), Ease.IN)                        // the swing: slow, then all at once
                .key(CULL_HITS + 8, pose().roll(CULL_AFTER), Ease.OUT)                     // on through, slowing
                .during(CULL_HITS + 6, CULL_TICKS, pose().alpha(0f).scale(1.1f), Ease.IN);
        ParticleShapes.model(sl, scythe, grip, yaw - 90f, tilt, 0f);

        // 3. the cut
        Later.run(sl, CULL_HITS - 2, () -> {
            ParticleShapes.slash(sl, CULL_CUT, grip, yaw, 0f, roll);
            ParticleShapes.slash(sl, CULL_CORE, grip, yaw, 0f, roll);
            ParticleShapes.slash(sl, CULL_BREAK, grip, yaw, 0f, roll);
        });
        // which way the blade is going as it passes through the prey: on along its path (down and to the side)
        Vec3 cutsToward = onSwing(forward, upright, CULL_THROUGH + CULL_BLADE_AHEAD + 90f, 1.0).add(forward.scale(0.4)).normalize();
        Later.run(sl, CULL_HITS, () -> cullBleeds(sl, feet, middle, cutsToward, random));
        Later.run(sl, CULL_HITS + 6, () -> {
            ParticleShapes.alongSlash(sl, BLOOD_SPARK, CULL_CUT, grip, yaw, 0f, roll, 6f, 18, 0.05, 0.05);
            ParticleShapes.alongSlash(sl, BLOOD_MIST, CULL_CUT, grip, yaw, 0f, roll, 6f, 8, 0.02, 0.02);
        });
        // the scythe crumbling: embers where its blade is as the swing runs out
        Later.run(sl, CULL_HITS + 9, () -> {
            Vec3 blade = grip.add(onSwing(forward, upright, CULL_AFTER + CULL_BLADE_AHEAD, CULL_REACH));
            ParticleShapes.burst(sl, WAR_EMBER, blade, 20, 0.03, 0.15);
            ParticleShapes.burst(sl, BLOOD_SPARK, blade, 10, 0.03, 0.12);
        });
    }

    /** A point of the swing, seen from the grip: 'degrees' from upright toward forward, 'distance' blocks out. */
    private static Vec3 onSwing(Vec3 forward, Vec3 upright, double degrees, double distance) {
        double a = Math.toRadians(degrees);
        return upright.scale(Math.cos(a) * distance).add(forward.scale(Math.sin(a) * distance));
    }

    /** The blood when the scythe goes through: out along the cut, up in a fountain, down on the ground. */
    private static void cullBleeds(ServerLevel sl, Vec3 feet, Vec3 middle, Vec3 forward, RandomSource random) {
        ParticleShapes.cone(sl, BLOOD, middle, forward, 70, 46, 0.15, 0.5);              // thrown the way the blade goes
        ParticleShapes.cone(sl, BLOOD, middle, new Vec3(0, 1, 0), 50, 24, 0.2, 0.45);    // a fountain
        ParticleShapes.burst(sl, BLOOD_MIST, middle, 14, 0.02, 0.1);
        ParticleShapes.burst(sl, BLOOD_SPARK, middle, 22, 0.1, 0.4);
        for (int i = 0; i < 9; i++) {
            Vec3 dir = ParticleShapes.randomDirectionInCone(random, forward, 220);
            double inner = 0.6 + random.nextDouble() * 0.3;
            double outer = inner + 0.8 + random.nextDouble() * 0.6;
            ParticleShapes.slashBetween(sl, CULL_RAY, middle.add(dir.scale(inner)), middle.add(dir.scale(outer)));
        }
        SpellFx.shockRing(sl, feet, 4.5, 0xE0B00A0A, 9, 0);
        SpellFx.shockRing(sl, feet, 2.5, 0xC0FFE0D0, 6, 1);
        // the ground splashed: a dark red circle that stays a while, and drops falling on it
        ModelFx pool = SpellFx.RUNE_CIRCLE.color(0xB0500004).scale(2.6f).lifetime(80).fade(0, 30)
                .key(0, pose().scale(0.2f))
                .key(6, pose().scale(1f), Ease.OUT);
        ParticleShapes.model(sl, pool, feet.add(0, 0.04, 0), random.nextFloat() * 360f, 0f, 0f);
        for (int i = 1; i <= 3; i++) {
            Later.run(sl, i * 4, () -> ParticleShapes.cone(sl, BLOOD, middle.add(0, 0.6, 0), new Vec3(0, 1, 0), 140, 8, 0.05, 0.15));
        }
    }

    // =====================================================================================
    // Galvanization (Iron-blooded Knight): a mantle of iron over the body
    // =====================================================================================

    /** Dark iron with red-hot veins, worn over a player-sized body. */
    public static final ModelFx IRON_MANTLE = ModelFx.of("conqueror/iron_mantle").aura(0x90FF3A1A, 0.05f, 2);

    /** Shortest time the mantle can be worn (it needs this long to form and break). */
    public static final int MANTLE_SHORTEST = 20;
    /** The tick the iron has fully closed over the body. */
    public static final int MANTLE_FORMED = 8;

    /**
     * Galvanization on an entity: molten iron rises around it in a spiral, a rune circle of iron and blood turns
     * under it, and a mantle of iron closes over its body (it turns with it), glowing as it cools; it is worn for
     * 'ticks' ticks, red-hot sparks dropping off it now and then, and at the end it cracks and falls away in flakes.
     * 'tag' names it: wearing it again with the same tag replaces it, galvanizeEnds(...) takes it away early.
     */
    public static void galvanize(ServerLevel sl, Entity wearer, int ticks, String tag) {
        int lasts = Math.max(MANTLE_SHORTEST, ticks);
        float size = (float) Math.max(0.5, wearer.getBbHeight() / 1.8);
        ParticleShapes.clearModels(sl, wearer, tag);
        ModelFx mantle = IRON_MANTLE.scale(size).lifetime(lasts).fade(0, 6).tag(tag)
                .key(0, pose().scale(1.35f).alpha(0f))
                .key(MANTLE_FORMED, pose().scale(1f).alpha(1f), Ease.OUT)
                .during(lasts - 6, lasts, pose().scale(1.08f), Ease.IN);
        ParticleShapes.modelOnTurning(sl, mantle, wearer, Vec3.ZERO, 0f, 0f, 0f);
        SpellFx.circleUnder(sl, wearer, Math.max(0.8, wearer.getBbWidth() * 1.6), 0xC08A8A92, lasts, tag + "_circle");

        Vec3 feet = wearer.position();
        double height = wearer.getBbHeight();
        // molten iron spirals up the body while it forms
        for (int t = 0; t < MANTLE_FORMED; t++) {
            int tick = t;
            Later.run(sl, tick, () -> {
                double a = tick * 0.9;
                for (int k = 0; k < 2; k++) {
                    double angle = a + k * Math.PI;
                    Vec3 at = wearer.position().add(Math.cos(angle) * 0.7, height * tick / MANTLE_FORMED, Math.sin(angle) * 0.7);
                    ParticleShapes.spawn(sl, IRON_SPARK, at, new Vec3(0, 0.05, 0));
                }
            });
        }
        Later.run(sl, MANTLE_FORMED, () -> {
            Vec3 chest = wearer.position().add(0, height * 0.65, 0);
            ParticleShapes.burst(sl, IRON_SPARK, chest, 20, 0.08, 0.3);       // the iron closes: an anvil strike
            SpellFx.shockRing(sl, wearer.position(), 2.5, 0xD0B8BCC8, 8, 0);
        });
        // red-hot sparks dropping off it while it is worn
        for (int t = MANTLE_FORMED + 10; t < lasts - 6; t += 10) {
            Later.run(sl, t, () -> {
                if (!wearer.isAlive()) return;
                ParticleShapes.burst(sl, IRON_SPARK, wearer.position().add(0, height * 0.7, 0), 2, 0.0, 0.06);
            });
        }
        Later.run(sl, lasts - 4, () -> {
            if (wearer.isAlive()) galvanizeBreaks(sl, wearer);
        });
    }

    /** The mantle taken away early: it cracks and falls in flakes. */
    public static void galvanizeEnds(ServerLevel sl, Entity wearer, String tag) {
        ParticleShapes.clearModels(sl, wearer, tag);
        ParticleShapes.clearModels(sl, wearer, tag + "_circle");
        galvanizeBreaks(sl, wearer);
    }

    private static void galvanizeBreaks(ServerLevel sl, Entity wearer) {
        Vec3 chest = wearer.position().add(0, wearer.getBbHeight() * 0.6, 0);
        ParticleShapes.burst(sl, IRON_FLAKE, chest, 22, 0.06, 0.2);
        ParticleShapes.burst(sl, IRON_SPARK, chest, 8, 0.04, 0.15);
    }

    /** A blow glancing off iron skin: a spray of sparks where it struck (for hits taken while galvanized). */
    public static void ironClang(ServerLevel sl, Vec3 at) {
        ParticleShapes.burst(sl, IRON_SPARK, at, 10, 0.08, 0.3);
        ParticleShapes.burst(sl, IRON_FLAKE, at, 3, 0.04, 0.1);
    }

    // =====================================================================================
    // Fog of War (War Bishop): a dome of fog, banners at its edge, the seal on the ground
    // =====================================================================================

    /** The War Bishop's seal, flat on the ground. .scale(n) = n blocks wide. */
    public static final ModelFx WAR_SIGIL = ModelFx.of("conqueror/war_sigil").glow().unshaded().seeThrough();

    /** Shortest Fog of War (it needs this long to roll out and clear). */
    public static final int FOG_SHORTEST = 60;
    /** The tick the fog has rolled out to its edge. */
    public static final int FOG_ROLLED_OUT = 24;
    private static final int FOG_PULSE = 4;                          // ticks between two puffs of the dome
    private static final int FOG_BANNERS = 6;

    /**
     * Fog of War: the War Bishop's seal opens on the ground at 'center' and turns; fog rolls out from it in rings
     * until it is a dome 'radius' blocks wide, which stays for 'ticks' ticks, thickest at its edge, embers drifting
     * in it; war banners rise around the edge, waving, their cloth toward the middle; at the end it all thins away.
     * 'tag' names the seal and the banners, so fogOfWarEnds(...) can take them away early (the fog then thins out
     * by itself).
     */
    public static void fogOfWar(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        int lasts = Math.max(FOG_SHORTEST, ticks);
        RandomSource random = sl.getRandom();
        float sigilWide = (float) Math.min(24.0, radius * 1.1);
        ModelFx sigil = WAR_SIGIL.scale(sigilWide).lifetime(lasts).fade(0, 20).spin(0.6f).tag(tag)
                .key(0, pose().scale(0.1f).alpha(0f))
                .key(16, pose().scale(1f).alpha(0.9f), Ease.OUT);
        ParticleShapes.model(sl, sigil, center.add(0, 0.06, 0), random.nextFloat() * 360f, 0f, 0f);
        SpellFx.shockRing(sl, center, radius, 0xC0802020, FOG_ROLLED_OUT, 0);

        // rolling out: a ring of fog every two ticks, each one further out
        for (int i = 0; i <= FOG_ROLLED_OUT / 2; i++) {
            double r = radius * i / (FOG_ROLLED_OUT / 2.0);
            Later.run(sl, i * 2, () -> ParticleShapes.ring(sl, WAR_FOG, center.add(0, 0.6, 0), Math.max(0.5, r),
                    (int) Math.max(6, r * 2.2), 0.05));
        }
        // the banners, one after another around the edge
        for (int i = 0; i < FOG_BANNERS; i++) {
            float around = i * 360f / FOG_BANNERS + random.nextFloat() * 20f;
            Vec3 at = center.add(ahead(around).scale(radius * 0.92));
            int delay = FOG_ROLLED_OUT / 2 + i * 3;
            ParticleShapes.model(sl, banner(lasts - delay, delay, tag), at, around + 180f, 0f, 0f);
        }
        fogPulse(sl, center, radius, lasts - FOG_ROLLED_OUT, FOG_ROLLED_OUT);
    }

    /** The dome kept up: puffs over its surface and a thicker wall at its foot, every FOG_PULSE ticks. */
    private static void fogPulse(ServerLevel sl, Vec3 center, double radius, int ticksLeft, int delay) {
        Later.run(sl, delay, () -> {
            if (ticksLeft <= 0) return;
            RandomSource random = sl.getRandom();
            boolean thinning = ticksLeft < 30;
            int puffs = (int) Math.max(8, radius * (thinning ? 1.0 : 2.4));
            for (int i = 0; i < puffs; i++) {
                // a point on the dome (the upper half of a sphere), most of them low
                double a = random.nextDouble() * Math.PI * 2;
                double up = Math.pow(random.nextDouble(), 1.8) * Math.PI / 2;
                Vec3 on = center.add(Math.cos(a) * Math.cos(up) * radius, Math.sin(up) * radius * 0.7 + 0.5,
                        Math.sin(a) * Math.cos(up) * radius);
                ParticleShapes.spawn(sl, WAR_FOG, on, new Vec3(-Math.sin(a) * 0.02, 0, Math.cos(a) * 0.02));
            }
            if (!thinning) {
                ParticleShapes.ring(sl, WAR_FOG, center.add(0, 0.8, 0), radius, (int) Math.max(8, radius * 1.4), 0.0);
                for (int i = 0; i < 4; i++) {
                    Vec3 inside = center.add((random.nextDouble() - 0.5) * radius * 1.4, 0.5 + random.nextDouble() * 2.5,
                            (random.nextDouble() - 0.5) * radius * 1.4);
                    ParticleShapes.spawn(sl, WAR_EMBER, inside, new Vec3(0, 0.01, 0));
                }
            }
            fogPulse(sl, center, radius, ticksLeft - FOG_PULSE, FOG_PULSE);
        });
    }

    /** Takes the seal and the banners of a Fog of War away early; the fog thins out by itself. */
    public static void fogOfWarEnds(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
    }

    // =====================================================================================
    // Fire Ravens: the flock leaving (the ravens themselves fly with combat/Seeker, see Conqueror)
    // =====================================================================================

    /** A raven of fire, 1.2 blocks wing to wing (frames: wings up, level, down). Beak = front: for combat/Seeker. */
    public static final ModelFx FIRE_RAVEN = ModelFx.of("conqueror/fire_raven").frames(3).smooth()
            .scale(0.8f).pivot(8, 2.5f, 9).glow().unshaded()
            .aura(0xFFFF6A1A, 0.07f, 3);

    /** A ring of fire flaring round 'who' and a burst of embers at 'from': the flock bursting out. */
    public static void ravensErupt(ServerLevel sl, Entity who, Vec3 from) {
        SpellFx.risingRings(sl, who.position(), 1.4, 1.8, 3, 0xD0FF6A1A);
        ParticleShapes.burst(sl, SpellFx.FIRE_EMBER, from, 24, 0.08, 0.3);
        ParticleShapes.burst(sl, SpellFx.FIRE_SMOKE, from, 6, 0.02, 0.08);
    }

    // =====================================================================================
    // The display cases (/conquerorfx)
    // =====================================================================================

    private interface Show {
        void play(ServerLevel sl, Player viewer, Vec3 spot, float yaw);
    }

    /** worn = it goes on the viewer, not on a spot. away = it is shown looking away from the viewer. */
    private record Case(String name, int ticks, boolean worn, boolean away, Show show) {}

    private static final List<Case> CASES = List.of(
            new Case("trap_set", TRAP_SET_TICKS, false, false, (sl, viewer, spot, yaw) -> trapSet(sl, spot, yaw)),
            new Case("trap_snap", 60, false, false, (sl, viewer, spot, yaw) -> trapSnap(sl, spot, yaw, 60)),
            new Case("keg", 60, false, false, (sl, viewer, spot, yaw) -> powderKeg(sl, spot, yaw, 60)),
            new Case("spear", 30, false, true, (sl, viewer, spot, yaw) -> spearFlies(sl, spot, yaw)),
            new Case("figure", 60, false, false, (sl, viewer, spot, yaw) -> flameFigure(sl, spot, yaw, 60)),
            new Case("banner", 80, false, false, (sl, viewer, spot, yaw) -> warBanner(sl, spot, yaw, 80, null)),
            new Case("cull", CULL_TICKS, false, true, (sl, viewer, spot, yaw) -> cull(sl, spot, yaw, 1.9)),
            new Case("fog", 160, false, false, (sl, viewer, spot, yaw) -> fogOfWar(sl, spot, 8.0, 160, "conquerorfx_fog")),
            new Case("battle_cry", 40, true, false, (sl, viewer, spot, yaw) -> battleCry(sl, viewer, 40)),
            new Case("galvanize", 100, true, false, (sl, viewer, spot, yaw) -> galvanize(sl, viewer, 100, "conquerorfx_mantle")),
            new Case("ravens", 40, true, false, (sl, viewer, spot, yaw) -> ravensShow(sl, viewer)));

    /** The spear thrown from a spot: 20 blocks in 16 ticks. */
    private static void spearFlies(ServerLevel sl, Vec3 spot, float yaw) {
        Vec3 dir = ahead(yaw);
        ModelFx flying = FLAME_SPEAR.lifetime(20).fade(2, 4)
                .key(0, pose().scale(0.3f))
                .key(3, pose().scale(1f), Ease.OUT_BACK)
                .key(18, pose().up(20f), Ease.IN);
        ParticleShapes.modelAlong(sl, flying, spot.add(0, 1.4, 0), dir);
    }

    /** The flock let loose from the viewer, with no prey: they fly out, look for some, and burn out. */
    private static void ravensShow(ServerLevel sl, Player viewer) {
        Vec3 from = viewer.getEyePosition().add(0, 0.2, 0);
        ravensErupt(sl, viewer, from);
        Seeker.of(FIRE_RAVEN)
                .speed(0.6).life(30).flap(6).trail(SpellFx.FIRE_EMBER, 1).burst(SpellFx.FIRE_EMBER, 12)
                .launchAt(viewer, sl, from, List.of(), 6, 2);
    }

    /** The names /conquerorfx knows. */
    public static List<String> names() {
        List<String> names = new ArrayList<>();
        for (Case c : CASES) names.add(c.name());
        return names;
    }

    /** One display, 7 blocks in front of the viewer and turned toward them. False if there is none of that name. */
    public static boolean show(ServerLevel sl, Player viewer, String name) {
        for (Case c : CASES) {
            if (!c.name().equals(name)) continue;
            float look = viewer.getYRot();
            Vec3 spot = viewer.position().add(ahead(look).scale(7));
            c.show().play(sl, viewer, spot, c.away() ? look : look + 180f);
            return true;
        }
        return false;
    }

    /** Every display at once, in a ring 16 blocks out from the viewer (the worn ones on the viewer), 'rounds' times. */
    public static void gallery(ServerLevel sl, Player viewer, int rounds) {
        List<Case> standing = new ArrayList<>();
        for (Case c : CASES) if (!c.worn()) standing.add(c);
        Vec3 middle = viewer.position();
        for (Case c : CASES) {
            int place = Math.max(0, standing.indexOf(c));
            float out = viewer.getYRot() + place * 360f / standing.size();
            Vec3 spot = middle.add(ahead(out).scale(16));
            float yaw = c.away() ? out : out + 180f;
            for (int round = 0; round < rounds; round++) {
                Later.run(sl, round * (c.ticks() + 10), () -> {
                    if (viewer.isAlive()) c.show().play(sl, viewer, spot, yaw);
                });
            }
        }
    }

    /** One block the way a yaw looks, level with the ground. */
    private static Vec3 ahead(float yaw) {
        double angle = Math.toRadians(yaw);
        return new Vec3(-Math.sin(angle), 0, Math.cos(angle));
    }

    @SubscribeEvent
    public static void onConquerorFxCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("conquerorfx")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("all")
                        .executes(ctx -> showAll(ctx.getSource(), 3))
                        .then(Commands.argument("rounds", IntegerArgumentType.integer(1, 20))
                                .executes(ctx -> showAll(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "rounds")))))
                .then(Commands.argument("name", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(names(), builder))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String name = StringArgumentType.getString(ctx, "name");
                            if (!show(ctx.getSource().getLevel(), player, name)) {
                                ctx.getSource().sendFailure(Component.literal("No display called " + name + ". There are: " + String.join(", ", names())));
                                return 0;
                            }
                            return 1;
                        })));
    }

    private static int showAll(CommandSourceStack source, int rounds) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Player player = source.getPlayerOrException();
        gallery(source.getLevel(), player, rounds);
        return 1;
    }
}
