package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.lukario.circleofinevitability.combat.Later;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * The Demoness (Assassin) look: every ability of the class shown the way it looks best, size, pivot, timing and
 * animation already chosen. Visual only: nothing here hurts, roots or heals anyone. The ability does that, at the
 * ticks named below, with Later.run.
 *
 * ---------- See them in game ----------
 *   /demonessfx all           every display in a ring around you, three times over (/demonessfx all 5 = five times)
 *   /demonessfx tornado       just that one, in front of you (press TAB after /demonessfx for the names)
 * Stand on flat ground: they are placed at the height of your feet.
 *
 * ---------- Use them in an ability ----------
 *   int lands = DemonessFx.blackFlameBolt(sl, SpellFx.handOf(player, sl), SpellFx.middleOf(target, sl));
 *   Later.run(sl, lands, () -> { ...hurt and slow the target...; DemonessFx.blackFlameHit(sl, target); });
 * Most displays are one call. The numbers named ..._TICKS say how long one lasts; the other public numbers are
 * the tick something happens (the cocoon is shut, the bolt lands), for Later.run. Methods that return an int
 * return that tick for the case they were given (a flight that depends on the distance).
 *
 * Displays that stay on an entity or a spot take a 'tag': calling them again with the same tag replaces them,
 * and ParticleShapes.clearModels(sl, entity, tag) (or clearModels(sl, spot, tag)) takes them away early.
 *
 * The models are in models/model_fx/demoness/ and are made by tools/model_fx/demoness_models.py.
 * Key by key, as in the design doc:
 *   1 Fatal Step          vanish, appear, backstab            | sneak: a mirror shard that charges, shardExplode
 *   2 Black Flame         blackFlameBolt, blackFlameHit, burn, creep  | sneak: mirrorSubstitute, mirrorShatter
 *   3 Mirror World        mirrorEnter, mirrorExit             | sneak: mirrorReturn
 *   4 Silk Weaver         silkSnare/snareWeb/snareCatch, webSwing/webPull, tripWeb/tripSnap, cocoon,
 *                         threadsOfLife/servantRise, webOfFate/fateTangle/fateLink/fatePull/fateSnap
 *   5 Plague              plagueMark, plagueJump, plagueBloom | sneak: curseMark, curseCharge, curseRelease
 *   6 Unaging             unagingWard, unagingRevive          | sneak: shardExplode on every placed shard
 *   7 Catastrophe         earthquake, hailstorm, tornado, iceAge, blackDeath, blackFlameRain, doomsdaySky
 *                         (rainCloud and bloodSky: only the sky of the last two, to drop what falls yourself)
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class DemonessFx {

    private DemonessFx() {}

    private static ModelFx.Pose pose() {
        return ModelFx.pose();
    }

    // =====================================================================================
    // Colours, particles and lines shared by the whole class
    // =====================================================================================

    public static final int VIOLET = 0xFFB050FF;
    public static final int VIOLET_SOFT = 0xC0D27BFF;
    public static final int SILK_WHITE = 0xFFF0ECFF;
    public static final int CRIMSON = 0xFFE0203A;
    public static final int PLAGUE_GREEN = 0xFF9CFF4A;
    public static final int ICE_BLUE = 0xFFB8E4FF;

    public static final ParticleFx SHADOW_SMOKE = ParticleFx.of("fx/smoke")
            .color(0xC0140A1C).endColor(0x00060208)
            .size(0.4f).endSize(1.1f).sizeRandom(0.3f)
            .lifetime(18, 10).gravity(-0.01f).friction(0.88f)
            .spin(2f).randomRotation();
    public static final ParticleFx VIOLET_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFD27BFF).endColor(0x002A0F3A)
            .size(0.1f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(16, 10).friction(0.9f)
            .glow();
    public static final ParticleFx GLASS_GLINT = ParticleFx.of("fx/sliver")
            .color(0xFFF4F0FF).endColor(0x009A90C0)
            .size(0.13f).endSize(0.06f).sizeRandom(0.4f)
            .lifetime(22, 12).gravity(0.4f).friction(0.92f)
            .spin(14f).randomRotation().glow();
    public static final ParticleFx SILK_WISP = ParticleFx.of("fx/glow")
            .color(0xE0FFFFFF).endColor(0x00C8C0E0)
            .size(0.06f).endSize(0.02f)
            .lifetime(14, 8).friction(0.9f)
            .glow();
    public static final ParticleFx BLOOD_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFFF4A5A).endColor(0x00600010)
            .size(0.1f).endSize(0.03f).sizeRandom(0.3f)
            .lifetime(18, 6).friction(0.97f)
            .glow();
    public static final ParticleFx PLAGUE_SMOKE = ParticleFx.of("fx/smoke")
            .color(0xA04A6A18).endColor(0x00101804)
            .size(0.45f).endSize(1.3f).sizeRandom(0.35f)
            .lifetime(34, 16).gravity(-0.005f).friction(0.9f)
            .spin(1.5f).randomRotation();
    public static final ParticleFx SPORE = ParticleFx.of("fx/glow")
            .color(0xFFB8FF5A).endColor(0x00305010)
            .size(0.07f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(26, 12).gravity(-0.01f).friction(0.94f)
            .glow();
    public static final ParticleFx PETAL = ParticleFx.of("fx/flake")
            .color(0xFFA0102A).endColor(0x00300008)
            .size(0.14f).endSize(0.1f).sizeRandom(0.4f)
            .lifetime(40, 20).gravity(0.06f).friction(0.94f)
            .spin(8f).randomRotation();
    public static final ParticleFx DUST = ParticleFx.of("fx/smoke")
            .color(0xB0605044).endColor(0x00302820)
            .size(0.5f).endSize(1.4f).sizeRandom(0.35f)
            .lifetime(30, 14).gravity(-0.01f).friction(0.88f)
            .spin(2f).randomRotation();
    public static final ParticleFx SNOW = ParticleFx.of("fx/flake")
            .color(0xFFF4FAFF).endColor(0x00C8E4FF)
            .size(0.08f).endSize(0.06f).sizeRandom(0.5f)
            .lifetime(50, 20).gravity(0.04f).friction(0.98f)
            .spin(5f).randomRotation().glow();
    public static final ParticleFx EMBER_BLACK = ParticleFx.of("fx/spark")
            .color(0xFFC060FF).endColor(0x00200030)
            .size(0.1f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(18, 10).gravity(-0.03f).friction(0.9f)
            .glow();

    /** A strand of silk: thin, white, see-through. */
    public static final SlashFx SILK = SlashFx.line("slash/smooth")
            .translucent().color(0xC0E8E4F4).core(0xE0FFFFFF)
            .width(0.05f).taper(SlashFx.Taper.UNIFORM);
    /** Silk shot from the hand: a little thicker, with a bright head. */
    public static final SlashFx SILK_SHOT = SlashFx.line("slash/smooth")
            .color(0xB0F0ECFF).core(0xF0FFFFFF)
            .width(0.09f).taper(SlashFx.Taper.COMET);
    /** The trip-web: barely there. */
    public static final SlashFx TRIP_SILK = SlashFx.line("slash/smooth")
            .translucent().color(0x40E8E4F4).core(0x70FFFFFF)
            .width(0.03f).taper(SlashFx.Taper.UNIFORM);
    /** The threads of the Web of Fate and the links between those it holds. */
    public static final SlashFx FATE_THREAD = SlashFx.line("slash/smooth")
            .color(0xB0B050FF).core(0xE0F0D8FF)
            .width(0.06f).taper(SlashFx.Taper.UNIFORM);
    /** Threads of Life: red, life running along them. */
    public static final SlashFx LIFE_THREAD = SlashFx.line("slash/smooth")
            .color(0xC0FF3048).core(0xE0FFD0D8)
            .width(0.05f).taper(SlashFx.Taper.UNIFORM);
    /** The streak a black flame leaves. */
    public static final SlashFx FLAME_STREAK = SlashFx.line("slash/smooth")
            .translucent().color(0xC0200830).core(0xE0B050FF)
            .width(0.35f).taper(SlashFx.Taper.COMET);
    /** The dagger's stab. */
    public static final SlashFx STAB = SlashFx.line("slash/smooth")
            .color(0xC0B050FF).core(0xF0FFE8FF)
            .width(0.22f).taper(SlashFx.Taper.COMET);
    /** A spore drifting from one victim to the next. */
    public static final SlashFx SPORE_TRAIL = SlashFx.line("slash/smooth")
            .translucent().color(0x904A6A18).core(0xC0B8FF5A)
            .width(0.12f).taper(SlashFx.Taper.COMET);

    // =====================================================================================
    // Models
    // =====================================================================================

    /** The Assassin's dagger, 1.4 blocks long, tip up. */
    public static final ModelFx DAGGER = ModelFx.of("demoness/dagger")
            .scale(0.8f).pivot(8, 0, 8).glow().aura(0x50B050FF, 0.04f, 2);
    /** A tall mirror on the ground, 2.3 blocks. Front = the way the yaw looks. */
    public static final ModelFx MIRROR = ModelFx.of("demoness/mirror")
            .scale(1.15f).pivot(8, 0, 8).glow();
    /** A shard of dark mirror stuck in the ground, 1.8 blocks. */
    public static final ModelFx SHARD = ModelFx.of("demoness/mirror_shard")
            .pivot(8, 0, 8).glow();
    /** Its glowing cracks, played over it. */
    public static final ModelFx SHARD_GLOW = ModelFx.of("demoness/mirror_shard_glow")
            .pivot(8, 0, 8).glow().unshaded().seeThrough();
    /** One sliver of glass, 0.7 blocks. */
    public static final ModelFx SLIVER = ModelFx.of("demoness/glass_shard")
            .pivot(8, 5, 8).glow().seeThrough();
    public static final ModelFx BLACK_FLAME = ModelFx.of("demoness/black_flame")
            .pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** Black flames 1 block wide and 2 tall. Stretch with .scale(w, h / 2, w). */
    public static final ModelFx BLACK_FLAMES = ModelFx.of("demoness/black_flame_pillar")
            .glow().unshaded().seeThrough();
    /** A round web, 1 block wide, lying flat: .scale(width). */
    public static final ModelFx WEB = ModelFx.of("demoness/web")
            .glow().unshaded().seeThrough();
    /** Silk wrapping round something 2 blocks tall: frame 0 = loose rings, 2 = shut. */
    public static final ModelFx COCOON = ModelFx.of("demoness/cocoon").frames(3).smooth()
            .pivot(8, 0, 8).glow();
    public static final ModelFx PLAGUE_SIGIL = ModelFx.of("demoness/plague_sigil")
            .glow().unshaded().seeThrough();
    public static final ModelFx CURSE_SIGIL = ModelFx.of("demoness/curse_sigil")
            .glow().unshaded().seeThrough();
    /** A black rose, 1 block tall: frame 0 = a bud, 2 = open. */
    public static final ModelFx ROSE = ModelFx.of("demoness/black_rose").frames(3).smooth()
            .pivot(8, 0, 8).glow();
    public static final ModelFx FISSURE = ModelFx.of("demoness/fissure")
            .glow().unshaded().seeThrough();
    public static final ModelFx FROST = ModelFx.of("demoness/frost")
            .glow().unshaded().seeThrough();
    public static final ModelFx ROCK = ModelFx.of("demoness/rock")
            .pivot(8, 8, 8).glow();
    public static final ModelFx HAILSTONE = ModelFx.of("demoness/hailstone")
            .pivot(8, 8, 8).glow().seeThrough().aura(0x40C0E8FF, 0.04f, 2);
    /** A funnel of wind, 2 blocks tall and wide at the top. */
    public static final ModelFx TORNADO = ModelFx.of("demoness/tornado")
            .glow().unshaded().seeThrough();
    public static final ModelFx PLAGUE_CLOUD = ModelFx.of("demoness/plague_cloud")
            .pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** A red bolt, 2 blocks tall, its pivot where it strikes. */
    public static final ModelFx LIGHTNING = ModelFx.of("demoness/lightning")
            .glow().unshaded().seeThrough();
    /** A swirl of red cloud seen from below, 1 block wide: scale it up a lot and hang it high. */
    public static final ModelFx SKY = ModelFx.of("demoness/blood_sky")
            .glow().unshaded().seeThrough();
    private static final ModelFx ICE_SPIKE = ModelFx.of("ice_spike")
            .glow().aura(0x5560D8FF, 0.05f, 2);
    private static final ModelFx METEOR = ModelFx.of("meteor")
            .pivot(8, 6, 8).glow().color(0xFFFF8080).aura(0xFFC01020, 0.2f, 3);
    private static final ModelFx METEOR_TAIL = ModelFx.of("meteor_tail")
            .pivot(8, 6, 8).glow().unshaded().seeThrough().color(0xFFFF6070);

    // =====================================================================================
    // Key 1: Fatal Step
    // =====================================================================================

    /** Someone melts into shadow: a puff of dark smoke and violet motes where they stood. */
    public static void vanish(ServerLevel sl, Entity who) {
        Vec3 middle = who.position().add(0, who.getBbHeight() * 0.5, 0);
        ParticleShapes.burst(sl, SHADOW_SMOKE, middle, 14, 0.02, 0.1);
        ParticleShapes.burst(sl, VIOLET_MOTE, middle, 16, 0.05, 0.18);
        SpellFx.closingRing(sl, who.position(), 1.4, 0xC0B050FF, 6, 0);
    }

    /** Someone steps out of shadow: smoke drawn in, a small ring going out. */
    public static void appear(ServerLevel sl, Entity who) {
        Vec3 middle = who.position().add(0, who.getBbHeight() * 0.5, 0);
        ParticleShapes.burst(sl, SHADOW_SMOKE, middle, 10, 0.01, 0.05);
        ParticleShapes.burst(sl, VIOLET_MOTE, middle, 10, 0.02, 0.1);
        SpellFx.shockRing(sl, who.position(), 1.2, 0xC0B050FF, 7, 0);
    }

    public static final int BACKSTAB_TICKS = 18;
    /** The tick the dagger goes in. */
    public static final int BACKSTAB_HITS = 8;

    /**
     * A dagger appears in the attacker's hand, draws back and drives into the target's back. Play it right after
     * the attacker appears behind the target; do the hit at BACKSTAB_HITS.
     */
    public static void backstab(ServerLevel sl, Entity attacker, Entity target) {
        Vec3 into = target.position().add(0, target.getBbHeight() * 0.55, 0);
        Vec3 hand = attacker.position().add(0, attacker.getBbHeight() * 0.6, 0);
        Vec3 toward = into.subtract(hand);
        Vec3 along = (toward.lengthSqr() < 1.0E-4 ? attacker.getLookAngle() : toward).normalize();
        float reach = (float) Math.max(0.4, into.distanceTo(hand) - 0.6);
        ModelFx dagger = DAGGER.lifetime(BACKSTAB_TICKS).fade(0, 6)
                .key(0, pose().up(-0.3f).scale(0.4f).alpha(0f))
                .key(4, pose().up(-0.8f).scale(1f).alpha(1f), Ease.OUT_BACK)        // appears and is drawn back
                .key(BACKSTAB_HITS, pose().up(reach), Ease.IN)                       // drives in
                .during(BACKSTAB_HITS, BACKSTAB_HITS + 3, pose().spin(25f));        // and twists
        ParticleShapes.modelAlong(sl, dagger, hand, along);
        Later.run(sl, BACKSTAB_HITS, () -> {
            ParticleShapes.slashBetween(sl, STAB.lifetime(6).sweep(2), hand, into.add(along.scale(0.4)));
            ParticleShapes.cone(sl, VIOLET_MOTE, into, along, 70, 14, 0.08, 0.3);
            ParticleShapes.cone(sl, BLOOD_MOTE, into, along, 50, 8, 0.05, 0.2);
        });
    }

    /** The whole of Fatal Step: gone from 'from', out of shadow behind 'target', dagger in its back. */
    public static void fatalStep(ServerLevel sl, Entity who, Vec3 from, Entity target) {
        Vec3 middle = from.add(0, who.getBbHeight() * 0.5, 0);
        ParticleShapes.burst(sl, SHADOW_SMOKE, middle, 14, 0.02, 0.1);
        ParticleShapes.burst(sl, VIOLET_MOTE, middle, 16, 0.05, 0.18);
        appear(sl, who);
        backstab(sl, who, target);
    }

    // ---------- the mirror shard (key 1 sneak) and its explosion (key 6 sneak) ----------

    /** The tick a placed shard is fully out of the ground. */
    public static final int SHARD_RISES = 6;

    /**
     * A shard of dark mirror breaks out of the ground and stands there for 'ticks' ticks, its cracks glowing
     * faintly. Its front (where enemies charge it by standing) looks the way 'yaw' looks. 'tag' names it: one
     * tag per shard, so shardCharge and shardExplode reach just that one.
     */
    public static void placeShard(ServerLevel sl, Vec3 ground, float yaw, int ticks, String tag) {
        ParticleShapes.clearModels(sl, ground, tag);
        int all = Math.max(30, ticks);
        ModelFx shard = SHARD.lifetime(all).fade(0, 4).tag(tag)
                .key(0, pose().up(-2f))
                .key(SHARD_RISES, pose().up(0f), Ease.OUT_BACK)
                .during(all - 10, all, pose().up(-2f), Ease.IN);
        ModelFx glow = SHARD_GLOW.lifetime(all).fade(SHARD_RISES, 10).tag(tag).color(0x40FFFFFF)
                .key(0, pose().up(-2f))
                .key(SHARD_RISES, pose().up(0f), Ease.OUT_BACK)
                .during(all - 10, all, pose().up(-2f), Ease.IN);
        ParticleShapes.model(sl, shard, ground, yaw, 0f, 0f);
        ParticleShapes.model(sl, glow, ground, yaw, 0f, 0f);
        Later.run(sl, 2, () -> ParticleShapes.burst(sl, GLASS_GLINT, ground.add(0, 0.4, 0), 10, 0.08, 0.22));
    }

    /**
     * One pulse of a shard's charge: its cracks flare as bright as 'charge' (0 = barely, 1 = full) and violet motes
     * stream off its front. Call it every second or so while enemies stand in front of it.
     */
    public static void shardCharge(ServerLevel sl, Vec3 ground, float yaw, float charge, String tag) {
        float c = Math.max(0f, Math.min(1f, charge));
        int alpha = Math.round(70 + 185 * c);
        ModelFx flare = SHARD_GLOW.color((alpha << 24) | 0xFFFFFF).lifetime(16).fade(2, 10).tag(tag)
                .key(0, pose().scale(1f))
                .key(4, pose().scale(1f + 0.06f * c), Ease.OUT)
                .key(16, pose().scale(1f), Ease.IN);
        ParticleShapes.model(sl, flare, ground, yaw, 0f, 0f);
        Vec3 front = ahead(yaw);
        ParticleShapes.cone(sl, VIOLET_MOTE, ground.add(0, 0.9, 0), front, 40, 2 + Math.round(8 * c), 0.05, 0.12 + 0.1 * c);
    }

    public static final int SHARD_EXPLODE_TICKS = 16;

    /**
     * The shard bursts: slivers of glass fly out in every direction, a violet flash and a ring. 'power' 0..1 is
     * how charged it was: more glass, a bigger blast. Takes the shard with this tag away.
     */
    public static void shardExplode(ServerLevel sl, Vec3 ground, float power, String tag) {
        ParticleShapes.clearModels(sl, ground, tag);
        float p = Math.max(0f, Math.min(1f, power));
        Vec3 center = ground.add(0, 0.9, 0);
        double radius = 1.5 + 2.5 * p;
        slivers(sl, center, null, 0, 8 + Math.round(16 * p), (float) radius + 1f);
        SpellFx.blast(sl, center, radius, 0xFFD27BFF, GLASS_GLINT);
        SpellFx.shockRing(sl, ground, radius, 0xF0E8D8FF, 9, 0);
    }

    /**
     * 'count' slivers of glass flying out of 'center' 'distance' blocks: in every direction (toward = null) or
     * in a cone of 'cone' degrees toward a point. They arrive after SLIVERS_FLY ticks.
     */
    public static final int SLIVERS_FLY = 8;

    public static void slivers(ServerLevel sl, Vec3 center, Vec3 toward, float cone, int count, float distance) {
        RandomSource random = sl.getRandom();
        Vec3 aim = toward == null ? null : toward.subtract(center).normalize();
        for (int i = 0; i < count; i++) {
            Vec3 dir = aim == null
                    ? ParticleShapes.randomDirectionInCone(random, new Vec3(0, 1, 0), 360)
                    : ParticleShapes.randomDirectionInCone(random, aim, cone);
            float far = distance * (0.7f + random.nextFloat() * 0.5f);
            ModelFx sliver = SLIVER.scale(0.6f + random.nextFloat() * 0.6f).lifetime(SLIVERS_FLY + 6).fade(0, 6)
                    .key(0, pose().scale(0.4f))
                    .key(SLIVERS_FLY, pose().up(far).scale(1f), Ease.OUT)
                    .during(0, SLIVERS_FLY + 6, pose().pitch(random.nextBoolean() ? 540f : -540f));   // tumbling
            ParticleShapes.modelAlong(sl, sliver, center, dir);
        }
    }

    // =====================================================================================
    // Key 2: Black Flame
    // =====================================================================================

    /** How fast the bolt flies, blocks per tick. */
    private static final double BOLT_SPEED = 1.6;

    /**
     * A ball of black flame flies from 'from' to 'to', leaving a dark streak. Returns the tick it arrives:
     * do the hit then (Later.run), with blackFlameHit.
     */
    public static int blackFlameBolt(ServerLevel sl, Vec3 from, Vec3 to) {
        Vec3 path = to.subtract(from);
        float length = (float) path.length();
        int ticks = Math.max(3, (int) Math.ceil(length / BOLT_SPEED));
        if (length < 1.0E-3) return 0;
        ModelFx bolt = BLACK_FLAME.lifetime(ticks + 2).fade(0, 2).spin(24f)
                .key(0, pose().scale(0.4f))
                .key(3, pose().scale(1.1f), Ease.OUT_BACK)
                .during(0, ticks, pose().up(length));
        ParticleShapes.modelAlong(sl, bolt, from, path);
        ParticleShapes.slashBetween(sl, FLAME_STREAK.lifetime(ticks + 6).sweep(ticks), from, to);
        for (int tick = 2; tick < ticks; tick += 2) {
            Vec3 at = from.add(path.scale((double) tick / ticks));
            Later.run(sl, tick, () -> ParticleShapes.burst(sl, EMBER_BLACK, at, 3, 0.01, 0.06));
        }
        return ticks;
    }

    /** The black flame bursts on something: a dark flash, violet sparks and a ring. */
    public static void blackFlameHit(ServerLevel sl, Vec3 at) {
        ModelFx flash = BLACK_FLAME.spin(30f).lifetime(10)
                .key(0, pose().scale(1f))
                .key(10, pose().scale(3.2f), Ease.OUT)
                .during(3, 10, pose().alpha(0f));
        ParticleShapes.model(sl, flash, at, sl.getRandom().nextFloat() * 360f, 0f, 0f);
        ParticleShapes.burst(sl, EMBER_BLACK, at, 18, 0.08, 0.25);
        ParticleShapes.burst(sl, SHADOW_SMOKE, at, 6, 0.02, 0.08);
        SpellFx.shockRing(sl, at.add(0, -0.4, 0), 1.6, 0xD07A2CB8, 8, 0);
    }

    /**
     * Black flames burning on an entity for 'ticks' ticks, sized to it, going wherever it goes. Cold fire: the
     * flames are only a picture, nothing is lit. Calling it again with the same tag replaces them.
     */
    public static void burn(ServerLevel sl, Entity who, int ticks, String tag) {
        ParticleShapes.clearModels(sl, who, tag);
        float wide = who.getBbWidth() + 0.5f;
        float tall = who.getBbHeight() * 0.85f;
        ModelFx flames = BLACK_FLAMES.scale(wide, tall / 2f, wide).lifetime(ticks).fade(4, 10).tag(tag)
                .key(0, pose().scale(0.3f))
                .key(5, pose().scale(1f), Ease.OUT_BACK);
        ParticleShapes.modelOn(sl, flames, who, Vec3.ZERO, sl.getRandom().nextFloat() * 360f, 0f, 0f);
    }

    public static final int CREEP_TICKS = 6;

    /** A small tongue of black flame leaping from one burning entity to the next. It arrives at CREEP_TICKS. */
    public static void creep(ServerLevel sl, Entity from, Entity to) {
        Vec3 a = from.position().add(0, from.getBbHeight() * 0.5, 0);
        ModelFx tongue = BLACK_FLAME.scale(0.5f).lifetime(CREEP_TICKS + 4).fade(0, 4).spin(30f);
        SpellFx.homing(sl, tongue, a, to, CREEP_TICKS);
        ParticleShapes.slashBetween(sl, FLAME_STREAK.width(0.18f).lifetime(CREEP_TICKS + 4).sweep(CREEP_TICKS),
                a, to.position().add(0, to.getBbHeight() * 0.5, 0));
    }

    // ---------- Mirror Substitute (key 2 sneak) ----------

    /** The tick the mirror stands. */
    public static final int MIRROR_RISES = 8;

    /**
     * A mirror rises where someone stands and keeps standing there for 'ticks' ticks, holding their place: the
     * copy that takes the next hit. Its glass looks the way they look. Break it with mirrorShatter (same tag).
     */
    public static void mirrorSubstitute(ServerLevel sl, Entity who, int ticks, String tag) {
        Vec3 ground = who.position();
        ParticleShapes.clearModels(sl, ground, tag);
        int all = Math.max(MIRROR_RISES + 10, ticks);
        ModelFx mirror = MIRROR.lifetime(all).fade(0, 8).tag(tag)
                .key(0, pose().up(-2.4f).alpha(0f))
                .key(MIRROR_RISES, pose().up(0f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.model(sl, mirror, ground, who.getYRot() + 180f, 0f, 0f);
        ParticleShapes.burst(sl, VIOLET_MOTE, ground.add(0, 1.1, 0), 12, 0.03, 0.12);
    }

    /**
     * The mirror breaks: its glass flies at whoever struck it ('toward'), the frame is gone. Shards arrive at
     * SLIVERS_FLY.
     */
    public static void mirrorShatter(ServerLevel sl, Vec3 ground, Vec3 toward, String tag) {
        ParticleShapes.clearModels(sl, ground, tag);
        Vec3 center = ground.add(0, 1.2, 0);
        float far = (float) Math.max(2.0, toward.distanceTo(center) + 0.5);
        slivers(sl, center, toward, 35f, 16, far);
        ParticleShapes.burst(sl, GLASS_GLINT, center, 24, 0.1, 0.3);
        ModelFx flash = SpellFx.SHOCK_RING.color(0xF0E8D8FF).lifetime(8)
                .key(0, pose().scale(0.8f))
                .key(8, pose().scale(4f), Ease.OUT)
                .during(2, 8, pose().alpha(0f));
        ParticleShapes.model(sl, flash, center, yawToward(center, toward), 90f, 0f);    // stands up, facing them
    }

    // =====================================================================================
    // Key 3: Mirror World
    // =====================================================================================

    public static final int MIRROR_DOOR_TICKS = 34;
    /** The tick someone has stepped through. */
    public static final int MIRROR_ENTERED = 12;

    /** A mirror rises, ripples like water, takes someone in, and sinks. Its glass looks the way 'yaw' looks. */
    public static void mirrorDoor(ServerLevel sl, Vec3 ground, float yaw) {
        ModelFx mirror = MIRROR.lifetime(MIRROR_DOOR_TICKS).fade(0, 6)
                .key(0, pose().up(-2.4f).alpha(0f))
                .key(MIRROR_RISES, pose().up(0f).alpha(1f), Ease.OUT_BACK)
                .during(MIRROR_DOOR_TICKS - 12, MIRROR_DOOR_TICKS, pose().up(-2.4f), Ease.IN);
        ParticleShapes.model(sl, mirror, ground, yaw, 0f, 0f);
        Vec3 glass = ground.add(0, 1.2, 0);
        for (int i = 0; i < 3; i++) {                                              // ripples on the glass
            ModelFx ripple = SpellFx.SHOCK_RING.color(0xC0D8C8FF).delay(MIRROR_RISES + i * 3).lifetime(10)
                    .key(0, pose().scale(0.3f))
                    .key(10, pose().scale(1.6f), Ease.OUT)
                    .during(0, 10, pose().alpha(0f), Ease.IN);
            ParticleShapes.model(sl, ripple, glass.add(ahead(yaw).scale(-0.12)), yaw, 90f, 0f);
        }
        Later.run(sl, MIRROR_ENTERED, () -> ParticleShapes.burst(sl, VIOLET_MOTE, glass, 14, 0.02, 0.1));
    }

    /** Someone steps into a mirror that rises in front of them. */
    public static void mirrorEnter(ServerLevel sl, Entity who) {
        mirrorDoor(sl, who.position().add(ahead(who.getYRot()).scale(0.6)), who.getYRot() + 180f);
    }

    /** A mirror rises at 'ground' and someone steps out of it, toward where 'yaw' looks. */
    public static void mirrorExit(ServerLevel sl, Vec3 ground, float yaw) {
        mirrorDoor(sl, ground, yaw);
    }

    /** The sneak exit: back out where they went in, the mirror bursting into glass behind them. */
    public static void mirrorReturn(ServerLevel sl, Vec3 ground, float yaw) {
        Vec3 center = ground.add(0, 1.2, 0);
        slivers(sl, center, null, 0, 14, 3.5f);
        ParticleShapes.burst(sl, GLASS_GLINT, center, 26, 0.1, 0.32);
        SpellFx.shockRing(sl, ground, 3.0, 0xF0E8D8FF, 8, 0);
        appearAt(sl, ground);
    }

    // =====================================================================================
    // Key 4: Silk Weaver
    // =====================================================================================

    // ---------- Silk Snare ----------

    /** How fast a thrown net flies, blocks per tick. */
    private static final double NET_SPEED = 1.4;

    /**
     * A wad of silk is thrown from 'from' and opens into a web on the ground at 'ground', 'radius' wide... and
     * stays for 'ticks' ticks (see snareWeb). Returns the tick it lands.
     */
    public static int silkSnare(ServerLevel sl, Vec3 from, Vec3 ground, double radius, int ticks, String tag) {
        Vec3 path = ground.add(0, 0.1, 0).subtract(from);
        float length = (float) path.length();
        int lands = Math.max(3, (int) Math.ceil(length / NET_SPEED));
        ModelFx net = WEB.color(0xE0FFFFFF).lifetime(lands + 1).fade(0, 1)
                .key(0, pose().scale(0.25f))
                .key(lands, pose().up(length).scale(0.9f), Ease.LINEAR)
                .during(0, lands, pose().yaw(720f));
        // flies with its flat side forward: the up axis is the path, so the web lies across it
        ParticleShapes.modelAlong(sl, net, from, path);
        ParticleShapes.slashBetween(sl, SILK_SHOT.lifetime(lands + 4).sweep(lands), from, ground.add(0, 0.1, 0));
        Later.run(sl, lands, () -> snareWeb(sl, ground, radius, ticks, tag));
        return lands;
    }

    /**
     * A web on the ground, 'radius' wide, for 'ticks' ticks: it springs open, then lies there nearly unseen.
     * Its own tag, so it can be cleared when it catches something or runs out.
     */
    public static void snareWeb(ServerLevel sl, Vec3 ground, double radius, int ticks, String tag) {
        ParticleShapes.clearModels(sl, ground, tag);
        int all = Math.max(20, ticks);
        ModelFx web = WEB.color(0x90FFFFFF).scale((float) (radius * 2.0)).lifetime(all).fade(0, 20).tag(tag)
                .key(0, pose().scale(0.15f).alpha(1f))
                .key(6, pose().scale(1f), Ease.OUT_BACK)
                .key(26, pose().alpha(0.45f));                       // then settles, almost hidden
        ParticleShapes.model(sl, web, ground.add(0, 0.04, 0), sl.getRandom().nextFloat() * 360f, 0f, 0f);
        ParticleShapes.ring(sl, SILK_WISP, ground.add(0, 0.1, 0), radius, 18, 0.02);
    }

    public static final int SNARE_CATCH_TICKS = 40;

    /** Something walked into the snare: strands spring up round its legs and hold it. */
    public static void snareCatch(ServerLevel sl, Entity victim, String tag) {
        float s = Math.max(0.6f, (victim.getBbWidth() + 0.5f) / 0.875f);
        ModelFx strands = COCOON.scale(s, 0.45f, s).lifetime(SNARE_CATCH_TICKS).fade(0, 10).tag(tag)
                .key(0, pose().frame(0).scale(1.4f).alpha(0f))
                .key(6, pose().frame(1).scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOn(sl, strands, victim, Vec3.ZERO, 0f, 0f, 0f);
        ParticleShapes.burst(sl, SILK_WISP, victim.position().add(0, 0.3, 0), 14, 0.04, 0.14);
    }

    // ---------- Web Swing ----------

    public static final int SWING_SHOT = 3;

    /**
     * Silk shoots from the player's hand to 'anchor' (a block they looked at) and stays taut between them for
     * 'ticks' ticks while they are pulled along it. A little web is stuck where it hit.
     */
    public static void webSwing(ServerLevel sl, LivingEntity player, Vec3 anchor, int ticks) {
        Vec3 hand = SpellFx.handOf(player, sl);
        if (hand == null) return;
        ParticleShapes.slashBetween(sl, SILK_SHOT.lifetime(SWING_SHOT + 3).sweep(SWING_SHOT), hand, anchor);
        ModelFx stuck = WEB.scale(1.1f).delay(SWING_SHOT).lifetime(ticks + 10).fade(0, 10)
                .key(0, pose().scale(0.2f))
                .key(4, pose().scale(1f), Ease.OUT_BACK);
        Vec3 back = hand.subtract(anchor).normalize();
        ParticleShapes.modelAlong(sl, stuck, anchor.add(back.scale(0.05)), back);      // flat against what it hit
        Later.run(sl, SWING_SHOT, () -> {
            ParticleShapes.burst(sl, SILK_WISP, anchor, 10, 0.03, 0.12);
            SpellFx.tether(sl, SILK, () -> SpellFx.handOf(player, sl), () -> anchor, ticks);
        });
    }

    /** Silk shoots out and catches a mob, then stays between them for 'ticks' ticks while it is pulled in. */
    public static void webPull(ServerLevel sl, LivingEntity player, Entity mob, int ticks) {
        Vec3 hand = SpellFx.handOf(player, sl);
        Vec3 body = SpellFx.middleOf(mob, sl);
        if (hand == null || body == null) return;
        ParticleShapes.slashBetween(sl, SILK_SHOT.lifetime(SWING_SHOT + 3).sweep(SWING_SHOT), hand, body);
        Later.run(sl, SWING_SHOT, () -> {
            ParticleShapes.burst(sl, SILK_WISP, body, 10, 0.03, 0.12);
            SpellFx.tether(sl, SILK, () -> SpellFx.handOf(player, sl), () -> SpellFx.middleOf(mob, sl), ticks);
        });
    }

    // ---------- Trip-web ----------

    /**
     * A barely-seen line of silk between two points, tied with a tiny web at each end, for 'ticks' ticks or
     * until 'intact' says false (it was tripped: play tripSnap). 'tag' names the little webs.
     */
    public static void tripWeb(ServerLevel sl, Vec3 a, Vec3 b, int ticks, BooleanSupplier intact, String tag) {
        Supplier<Vec3> end1 = () -> intact.getAsBoolean() ? a : null;
        Supplier<Vec3> end2 = () -> intact.getAsBoolean() ? b : null;
        SpellFx.tether(sl, TRIP_SILK, end1, end2, ticks);
        Vec3 across = b.subtract(a);
        if (across.lengthSqr() < 1.0E-4) return;
        ModelFx knot = WEB.color(0xA0FFFFFF).scale(0.5f).lifetime(ticks).fade(4, 10).tag(tag);
        ParticleShapes.modelAlong(sl, knot, a, across);
        ParticleShapes.modelAlong(sl, knot, b, across.scale(-1));
    }

    /** The trip-web snaps: both halves whip back to their knots, glints in the middle. */
    public static void tripSnap(ServerLevel sl, Vec3 a, Vec3 b, String tag) {
        ParticleShapes.clearModels(sl, a, tag);
        Vec3 middle = a.add(b).scale(0.5);
        ParticleShapes.slashBetween(sl, SILK_SHOT.lifetime(5).sweep(1), middle, a);
        ParticleShapes.slashBetween(sl, SILK_SHOT.lifetime(5).sweep(1), middle, b);
        ParticleShapes.burst(sl, SILK_WISP, middle, 16, 0.06, 0.2);
    }

    // ---------- Cocoon ----------

    /** The tick the cocoon is wrapped shut. */
    public static final int COCOON_SHUT = 14;
    private static final int COCOON_UNWRAP = 12;

    /**
     * Silk winds round an entity and wraps it shut, holds for 'ticks' ticks and unravels in its last moments.
     * Sized to the entity and stuck to it. mending = true (a cocoon round yourself): silver motes rise out of it
     * while it holds.
     */
    public static void cocoon(ServerLevel sl, Entity who, int ticks, boolean mending, String tag) {
        ParticleShapes.clearModels(sl, who, tag);
        int all = Math.max(COCOON_SHUT + COCOON_UNWRAP + 4, ticks);
        // at scale 1 the shut cocoon is 2 blocks tall and 0.875 wide at its widest
        float s = Math.max((who.getBbHeight() + 0.3f) / 2f, (who.getBbWidth() + 0.35f) / 0.875f);
        ModelFx wrap = COCOON.scale(s).lifetime(all).fade(0, 6).tag(tag)
                .key(0, pose().frame(0).alpha(0f).spin(-90f))
                .key(4, pose().alpha(1f))
                .during(2, COCOON_SHUT, pose().frame(2).spin(0f), Ease.IN_OUT)      // winds round as it closes
                .during(all - COCOON_UNWRAP, all, pose().frame(0).spin(120f), Ease.IN);
        ParticleShapes.modelOn(sl, wrap, who, Vec3.ZERO, 0f, 0f, 0f);
        Vec3 body = who.position().add(0, who.getBbHeight() * 0.5, 0);
        Later.run(sl, COCOON_SHUT, () -> ParticleShapes.burst(sl, SILK_WISP, body, 16, 0.04, 0.12));
        if (mending) {
            for (int t = COCOON_SHUT; t < all - COCOON_UNWRAP; t += 10) {
                Later.run(sl, t, () -> {
                    if (!who.isAlive()) return;
                    ParticleShapes.cone(sl, SILK_WISP, who.position().add(0, who.getBbHeight(), 0),
                            new Vec3(0, 1, 0), 50, 4, 0.03, 0.08);
                });
            }
        }
    }

    // ---------- Threads of Life ----------

    /**
     * Red threads run from a mob into the caster for 'ticks' ticks, with drops of life travelling along them.
     * The threads break by themselves when either dies.
     */
    public static void threadsOfLife(ServerLevel sl, Entity caster, Entity mob, int ticks) {
        for (int i = 0; i < 3; i++) {
            double lift = (i - 1) * 0.3;
            SpellFx.tether(sl, LIFE_THREAD,
                    () -> {
                        Vec3 m = SpellFx.middleOf(mob, sl);
                        return m == null ? null : m.add(0, lift, 0);
                    },
                    () -> SpellFx.middleOf(caster, sl), ticks);
        }
        for (int t = 0; t < ticks; t += 6) {
            Later.run(sl, t, () -> {
                Vec3 from = SpellFx.middleOf(mob, sl);
                Vec3 to = SpellFx.middleOf(caster, sl);
                if (from == null || to == null) return;
                Vec3 path = to.subtract(from);
                // drops that reach the caster in about their own lifetime
                ParticleShapes.cone(sl, BLOOD_MOTE, from, path, 8, 2, path.length() / 22, path.length() / 16);
            });
        }
    }

    /** A drained mob rises again, stone-skinned: rock bursts off it, dust, grey rings climbing it. */
    public static void servantRise(ServerLevel sl, Entity servant) {
        Vec3 ground = servant.position();
        rocks(sl, ground, servant.getBbWidth() + 0.5, 5, 1.5f, 0.6f);
        ParticleShapes.burst(sl, DUST, ground.add(0, 0.4, 0), 10, 0.03, 0.1);
        SpellFx.risingRings(sl, ground, servant.getBbWidth() + 0.4, servant.getBbHeight(), 4, 0xE0A09890);
        ModelFx crack = FISSURE.scale(2.4f).lifetime(40).fade(0, 20)
                .key(0, pose().scale(0.2f))
                .key(6, pose().scale(1f), Ease.OUT);
        ParticleShapes.model(sl, crack, ground.add(0, 0.05, 0), sl.getRandom().nextFloat() * 360f, 0f, 0f);
    }

    // ---------- Web of Fate ----------

    /** The tick the web has opened. */
    public static final int FATE_OPENS = 10;

    /**
     * A great web hangs in the air over 'center', 'radius' wide, for 'ticks' ticks: it opens, turns slowly,
     * and threads run from its rim down to the ground. 'tag' names it, for fateSnap.
     */
    public static void webOfFate(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        int all = Math.max(FATE_OPENS + 10, ticks);
        ModelFx web = WEB.color(0xF0E8D0FF).scale((float) (radius * 2.0)).lifetime(all).fade(0, 12).spin(0.5f).tag(tag)
                .key(0, pose().scale(0.1f).yaw(-180f))
                .key(FATE_OPENS, pose().scale(1f).yaw(0f), Ease.OUT_BACK);
        ParticleShapes.model(sl, web, center, sl.getRandom().nextFloat() * 360f, 0f, 0f);
        ModelFx under = SpellFx.RUNE_CIRCLE.color(0x90B050FF).scale((float) (radius * 1.6)).lifetime(all).fade(6, 12)
                .spin(-1f).tag(tag);
        double drop = center.y - Math.floor(center.y) + 6.0;
        Vec3 ground = center.add(0, -Math.min(drop, 8.0), 0);
        ParticleShapes.model(sl, under, ground.add(0, 0.05, 0), 0f, 0f, 0f);
        for (int i = 0; i < 6; i++) {                                              // anchor threads
            double angle = Math.PI * 2 * i / 6 + 0.3;
            Vec3 rim = center.add(Math.cos(angle) * radius * 0.95, 0, Math.sin(angle) * radius * 0.95);
            Vec3 foot = ground.add(Math.cos(angle) * radius * 1.15, 0, Math.sin(angle) * radius * 1.15);
            Later.run(sl, FATE_OPENS, () -> SpellFx.tether(sl, FATE_THREAD.width(0.04f), () -> rim, () -> foot, all - FATE_OPENS));
        }
    }

    /** Someone caught in the Web of Fate: a small violet web turning over their head for 'ticks' ticks. */
    public static void fateTangle(ServerLevel sl, Entity who, int ticks, String tag) {
        ParticleShapes.clearModels(sl, who, tag);
        ModelFx mark = WEB.color(0xF0C890FF).scale(0.9f).lifetime(ticks).fade(4, 8).spin(3f).tag(tag)
                .key(0, pose().scale(0.2f))
                .key(5, pose().scale(1f), Ease.OUT_BACK);
        ParticleShapes.modelOn(sl, mark, who, new Vec3(0, who.getBbHeight() + 0.45, 0), 0f, 0f, 0f);
        ParticleShapes.burst(sl, VIOLET_MOTE, who.position().add(0, who.getBbHeight() * 0.5, 0), 8, 0.03, 0.1);
    }

    /** A violet thread between two who are tangled together, for 'ticks' ticks. Damage shared runs along it. */
    public static void fateLink(ServerLevel sl, Entity a, Entity b, int ticks) {
        SpellFx.tether(sl, FATE_THREAD, a, b, ticks);
    }

    public static final int FATE_PULL_TICKS = 10;

    /** The recast: threads draw everyone tangled in to the middle. The pull lasts FATE_PULL_TICKS. */
    public static void fatePull(ServerLevel sl, Vec3 center, List<? extends Entity> tangled) {
        for (Entity e : tangled) {
            SpellFx.tether(sl, FATE_THREAD.width(0.09f), () -> SpellFx.middleOf(e, sl), () -> center, FATE_PULL_TICKS);
        }
        SpellFx.closingRing(sl, center.add(0, -0.5, 0), 5.0, 0xE0B050FF, FATE_PULL_TICKS, 0);
    }

    /** The Web of Fate snaps: its strands lash outward, a violet flash. Takes the web with this tag away. */
    public static void fateSnap(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        RandomSource random = sl.getRandom();
        for (int i = 0; i < 14; i++) {
            double angle = Math.PI * 2 * i / 14 + random.nextDouble() * 0.3;
            Vec3 out = center.add(Math.cos(angle) * radius, (random.nextDouble() - 0.5) * 1.5, Math.sin(angle) * radius);
            ParticleShapes.slashBetween(sl, FATE_THREAD.width(0.07f).lifetime(7).sweep(3), center, out);
        }
        SpellFx.blast(sl, center, Math.max(2.5, radius * 0.4), 0xFFD27BFF, VIOLET_MOTE);
        ParticleShapes.burst(sl, SILK_WISP, center, 30, 0.1, 0.4);
    }

    // =====================================================================================
    // Key 5: Plague and the Curse
    // =====================================================================================

    /**
     * The mark of Plague over an entity for 'ticks' ticks: a sickly seal turning over its head, green smoke and
     * spores seeping off it. 'stage' 1, 2, 3... (each jump makes it stronger) makes the seal bigger and the smoke
     * thicker. Calling it again with the same tag replaces it.
     */
    public static void plagueMark(ServerLevel sl, Entity who, int ticks, int stage, String tag) {
        ParticleShapes.clearModels(sl, who, tag);
        float grow = 1f + 0.18f * Math.max(0, stage - 1);
        ModelFx seal = PLAGUE_SIGIL.scale(0.75f * grow).lifetime(ticks).fade(4, 10).spin(3f).tag(tag)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOn(sl, seal, who, new Vec3(0, who.getBbHeight() + 0.4, 0), 0f, 0f, 0f);
        int puffs = 1 + Math.max(0, stage - 1);
        for (int t = 4; t < ticks; t += 10) {
            Later.run(sl, t, () -> {
                if (!who.isAlive()) return;
                Vec3 body = who.position().add(0, who.getBbHeight() * 0.5, 0);
                ParticleShapes.burst(sl, PLAGUE_SMOKE, body, puffs, 0.01, 0.03);
                ParticleShapes.burst(sl, SPORE, body, 2 * puffs, 0.02, 0.06);
            });
        }
    }

    public static final int PLAGUE_JUMP_TICKS = 10;

    /** The disease leaps to the next victim: a ball of plague drifts over and sinks into it at PLAGUE_JUMP_TICKS. */
    public static void plagueJump(ServerLevel sl, Entity from, Entity to) {
        Vec3 a = from.position().add(0, from.getBbHeight() * 0.6, 0);
        ModelFx spore = PLAGUE_CLOUD.scale(0.45f).lifetime(PLAGUE_JUMP_TICKS + 4).fade(2, 4).spin(12f);
        SpellFx.homing(sl, spore, a, to, PLAGUE_JUMP_TICKS);
        ParticleShapes.slashBetween(sl, SPORE_TRAIL.lifetime(PLAGUE_JUMP_TICKS + 4).sweep(PLAGUE_JUMP_TICKS),
                a, to.position().add(0, to.getBbHeight() * 0.5, 0));
    }

    /** The mark runs its course on someone: a burst of plague off them (just before it jumps on). */
    public static void plagueBloom(ServerLevel sl, Entity who) {
        Vec3 body = who.position().add(0, who.getBbHeight() * 0.5, 0);
        ModelFx cloud = PLAGUE_CLOUD.lifetime(16).fade(0, 10)
                .key(0, pose().scale(0.5f))
                .key(16, pose().scale(2.2f), Ease.OUT);
        ParticleShapes.model(sl, cloud, body, sl.getRandom().nextFloat() * 360f, 0f, 0f);
        ParticleShapes.burst(sl, PLAGUE_SMOKE, body, 8, 0.03, 0.1);
        ParticleShapes.burst(sl, SPORE, body, 16, 0.05, 0.18);
    }

    /**
     * The Curse on an entity for 'ticks' ticks: a crimson eye in a ring of thorns turning over its head, and a
     * circle under its feet. Calling it again with the same tag replaces it.
     */
    public static void curseMark(ServerLevel sl, Entity who, int ticks, String tag) {
        ParticleShapes.clearModels(sl, who, tag);
        ModelFx eye = CURSE_SIGIL.scale(0.8f).lifetime(ticks).fade(4, 8).spin(-2f).tag(tag)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOn(sl, eye, who, new Vec3(0, who.getBbHeight() + 0.45, 0), 0f, 0f, 0f);
        SpellFx.circleUnder(sl, who, Math.max(0.8, who.getBbWidth()), 0xA0E0203A, ticks, tag);
    }

    /**
     * The Curse drinks in a hit: the eye throbs, the bigger 'stored' (0..1 of what it can hold, your scale) the
     * harder. Call it each time the cursed one is hurt.
     */
    public static void curseCharge(ServerLevel sl, Entity who, float stored) {
        float s = Math.max(0f, Math.min(1f, stored));
        ModelFx throb = CURSE_SIGIL.scale(0.8f).lifetime(8).fade(0, 6)
                .key(0, pose().scale(1f))
                .key(8, pose().scale(1.3f + 0.6f * s), Ease.OUT);
        ParticleShapes.modelOn(sl, throb, who, new Vec3(0, who.getBbHeight() + 0.45, 0), 0f, 0f, 0f);
        ParticleShapes.burst(sl, BLOOD_MOTE, who.position().add(0, who.getBbHeight() * 0.5, 0),
                3 + Math.round(9 * s), 0.03, 0.1);
    }

    public static final int CURSE_RELEASE_HITS = 4;

    /**
     * The Curse comes due: a red bolt drops onto the cursed one and the eye bursts. The stored damage lands at
     * CURSE_RELEASE_HITS.
     */
    public static void curseRelease(ServerLevel sl, Entity who, String tag) {
        ParticleShapes.clearModels(sl, who, tag);
        Vec3 ground = who.position();
        ModelFx bolt = LIGHTNING.scale(2.4f).lifetime(10).fade(0, 6)
                .key(0, pose().up(8f).alpha(0f))
                .key(CURSE_RELEASE_HITS, pose().up(0f).alpha(1f), Ease.IN);
        ParticleShapes.model(sl, bolt, ground, sl.getRandom().nextFloat() * 360f, 0f, 0f);
        Later.run(sl, CURSE_RELEASE_HITS, () -> {
            Vec3 body = ground.add(0, who.getBbHeight() * 0.5, 0);
            ModelFx eye = CURSE_SIGIL.lifetime(10).fade(0, 8)
                    .key(0, pose().scale(0.8f))
                    .key(10, pose().scale(3.5f), Ease.OUT);
            ParticleShapes.model(sl, eye, ground.add(0, who.getBbHeight() + 0.45, 0), 0f, 0f, 0f);
            SpellFx.blast(sl, body, 2.2, 0xFFE0203A, BLOOD_MOTE);
        });
    }

    // =====================================================================================
    // Key 6: Unaging
    // =====================================================================================

    /**
     * The ward: a black rose bud turning slowly over someone's head for 'ticks' ticks, seen by everyone but the
     * wearer (in first person). Take it off with unagingRevive (same tag) or let it run out.
     */
    public static void unagingWard(ServerLevel sl, Entity who, int ticks, String tag) {
        ParticleShapes.clearModels(sl, who, tag);
        ModelFx bud = ROSE.scale(0.45f).lifetime(ticks).fade(6, 20).spin(1.5f).tag(tag)
                .key(0, pose().frame(0).up(0.6f).alpha(0f))
                .key(10, pose().up(0f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOnHead(sl, bud, who, 0.35, 0f, 0f, 0f);
        ParticleShapes.burst(sl, PETAL, who.position().add(0, who.getBbHeight() + 0.4, 0), 6, 0.02, 0.06);
    }

    public static final int REVIVE_TICKS = 70;
    /** The tick the rose is fully open: the moment of coming back. */
    public static final int REVIVE_BLOOMS = 22;

    /**
     * Death is cancelled: the ward's bud is gone, a great black rose grows out of the ground at their feet,
     * opens, and scatters its petals as they rise again.
     */
    public static void unagingRevive(ServerLevel sl, Entity who, String tag) {
        ParticleShapes.clearModels(sl, who, tag);
        Vec3 ground = who.position();
        ModelFx rose = ROSE.scale(2.6f).lifetime(REVIVE_TICKS).fade(0, 16)
                .key(0, pose().frame(0).up(-2.8f))
                .key(12, pose().up(0f), Ease.OUT)
                .during(10, REVIVE_BLOOMS, pose().frame(2), Ease.OUT_BACK)
                .during(REVIVE_BLOOMS, REVIVE_TICKS, pose().spin(40f).scale(1.15f), Ease.OUT);
        ParticleShapes.model(sl, rose, ground.add(ahead(who.getYRot()).scale(-0.9)), who.getYRot(), 0f, 0f);
        ParticleShapes.burst(sl, SHADOW_SMOKE, ground.add(0, 0.8, 0), 18, 0.03, 0.12);
        Later.run(sl, REVIVE_BLOOMS, () -> {
            ParticleShapes.burst(sl, PETAL, ground.add(0, 2.4, 0), 40, 0.08, 0.26);
            SpellFx.risingRings(sl, ground, 1.6, 2.2, 4, 0xE0E0203A);
            SpellFx.shockRing(sl, ground, 4.0, 0xC0A0102A, 10, 0);
        });
    }

    // =====================================================================================
    // Key 7: Catastrophes
    // =====================================================================================

    // ---------- Earthquake ----------

    public static final int QUAKE_TICKS = 70;
    /** The tick the ground splits open (the knock-up). */
    public static final int QUAKE_SPLITS = 8;

    /** The ground cracks open round 'ground', 'radius' out: rings of dust, rock flung up, cracks glowing red. */
    public static void earthquake(ServerLevel sl, Vec3 ground, double radius) {
        ModelFx cracks = FISSURE.scale((float) (radius * 2.0)).lifetime(QUAKE_TICKS).fade(0, 30)
                .key(0, pose().scale(0.15f))
                .key(QUAKE_SPLITS, pose().scale(1f), Ease.OUT);
        ParticleShapes.model(sl, cracks, ground.add(0, 0.05, 0), sl.getRandom().nextFloat() * 360f, 0f, 0f);
        SpellFx.warningCircle(sl, ground, radius, 0x80C0301A, QUAKE_SPLITS);
        for (int i = 0; i < 3; i++) {
            SpellFx.shockRing(sl, ground, radius * (0.6 + 0.25 * i), 0xD0806050, 12, QUAKE_SPLITS + i * 6);
        }
        Later.run(sl, QUAKE_SPLITS, () -> {
            rocks(sl, ground, radius, (int) Math.round(radius * 2.5), 2.8f, 1f);
            RandomSource random = sl.getRandom();
            for (int i = 0; i < radius * 3; i++) {
                double angle = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
                ParticleShapes.burst(sl, DUST, ground.add(Math.cos(angle) * far, 0.3, Math.sin(angle) * far), 2, 0.02, 0.08);
            }
        });
    }

    /** 'count' lumps of rock flung up out of a round patch, tumbling, falling back. 'height' in blocks. */
    public static void rocks(ServerLevel sl, Vec3 ground, double radius, int count, float height, float size) {
        RandomSource random = sl.getRandom();
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
            float up = height * (0.6f + random.nextFloat() * 0.6f);
            int air = 14 + random.nextInt(8);
            ModelFx rock = ROCK.scale(size * (0.6f + random.nextFloat() * 0.8f)).delay(random.nextInt(4)).lifetime(air + 4).fade(0, 4)
                    .key(0, pose().up(-0.3f))
                    .key(air / 2, pose().up(up), Ease.OUT)
                    .key(air, pose().up(-0.2f), Ease.IN)
                    .during(0, air, pose().pitch(random.nextBoolean() ? 360f : -360f).spin(180f));
            Vec3 at = ground.add(Math.cos(angle) * far, 0.3, Math.sin(angle) * far);
            ParticleShapes.model(sl, rock, at, random.nextFloat() * 360f, 0f, 0f);
        }
    }

    // ---------- Hailstorm ----------

    /** Ticks from a hailstone appearing to it hitting the ground. */
    public static final int HAIL_FALLS = 10;

    /** One hailstone dropping onto 'spot' from high up; it shatters into flakes at HAIL_FALLS. */
    public static void hailstone(ServerLevel sl, Vec3 spot) {
        RandomSource random = sl.getRandom();
        float size = 0.8f + random.nextFloat() * 0.8f;
        ModelFx stone = HAILSTONE.scale(size).lifetime(HAIL_FALLS + 1).fade(2, 1)
                .key(0, pose().up(16f))
                .key(HAIL_FALLS, pose().up(0.15f), Ease.IN)
                .during(0, HAIL_FALLS, pose().pitch(300f).spin(200f));
        ParticleShapes.model(sl, stone, spot, random.nextFloat() * 360f, 0f, 0f);
        Later.run(sl, HAIL_FALLS, () -> ParticleShapes.burst(sl, SNOW, spot.add(0, 0.2, 0), 6, 0.05, 0.15));
    }

    /**
     * A grey-blue storm cloud over 'ground' for 'ticks' ticks and hail falling out of it over 'radius': about
     * 'perSecond' stones a second, each landing HAIL_FALLS after it was dropped.
     */
    public static void hailstorm(ServerLevel sl, Vec3 ground, double radius, int ticks, int perSecond) {
        cloudOver(sl, ground, radius * 2.6, 16.5, ticks + HAIL_FALLS, 0xFF8898B8);
        RandomSource random = sl.getRandom();
        int every = Math.max(1, 20 / Math.max(1, perSecond));
        for (int t = 0; t < ticks; t += every) {
            Later.run(sl, t, () -> hailstone(sl, randomSpot(random, ground, radius)));
        }
    }

    // ---------- Tornado ----------

    /** The tornado reaches full size at this tick. */
    public static final int TORNADO_FORMS = 14;
    private static final float TORNADO_SCALE = 3.2f;

    /**
     * A tornado forms at 'ground' and travels 'speed' blocks per tick the way 'yaw' looks for 'ticks' ticks, then
     * breaks up. tornadoAt(...) says where it is at any tick.
     */
    public static void tornado(ServerLevel sl, Vec3 ground, float yaw, double speed, int ticks) {
        int all = Math.max(TORNADO_FORMS + 10, ticks);
        float travel = (float) (speed * all);
        ModelFx outer = TORNADO.scale(TORNADO_SCALE).lifetime(all).fade(4, 12).spin(28f)
                .key(0, pose().scale(0.15f))
                .key(TORNADO_FORMS, pose().scale(1f), Ease.OUT)
                .during(0, all, pose().forward(travel))
                .during(all - 12, all, pose().scale(1.3f).alpha(0f), Ease.IN);
        ModelFx inner = TORNADO.scale(TORNADO_SCALE * 0.7f, TORNADO_SCALE * 1.05f, TORNADO_SCALE * 0.7f)
                .color(0xFF6A5A78).lifetime(all).fade(4, 12).spin(-36f)
                .key(0, pose().scale(0.15f))
                .key(TORNADO_FORMS, pose().scale(1f), Ease.OUT)
                .during(0, all, pose().forward(travel));
        ParticleShapes.model(sl, outer, ground, yaw, 0f, 0f);
        ParticleShapes.model(sl, inner, ground, yaw, 0f, 0f);
        for (int t = 2; t < all; t += 3) {                                         // dust whirled up at its foot
            int tick = t;
            Later.run(sl, t, () -> {
                Vec3 foot = tornadoAt(ground, yaw, speed, tick);
                ParticleShapes.spiral(sl, DUST, foot.add(0, 0.3, 0), 3, 6, 0.6, 2.2, 0.6, 0.35);
            });
        }
    }

    /** Where the foot of a tornado started with tornado(...) is, 'tick' ticks after it was started. */
    public static Vec3 tornadoAt(Vec3 ground, float yaw, double speed, int tick) {
        return ground.add(ahead(yaw).scale(speed * tick));
    }

    // ---------- Hopeless Ice Age ----------

    /** The tick the frost has spread to its full width. */
    public static final int ICE_SPREADS = 40;

    /**
     * The ground round 'ground' freezes over, 'radius' wide, for 'ticks' ticks: frost spreading out, spikes of ice
     * breaking through one after another, snow falling. Freezing the enemies themselves is Freeze's job.
     */
    public static void iceAge(ServerLevel sl, Vec3 ground, double radius, int ticks) {
        int all = Math.max(ICE_SPREADS + 20, ticks);
        ModelFx frost = FROST.scale((float) (radius * 2.0)).lifetime(all).fade(0, 30)
                .key(0, pose().scale(0.1f))
                .key(ICE_SPREADS, pose().scale(1f), Ease.OUT);
        ParticleShapes.model(sl, frost, ground.add(0, 0.05, 0), sl.getRandom().nextFloat() * 360f, 0f, 0f);
        ModelFx rim = SpellFx.RUNE_CIRCLE.color(0xA0B8E4FF).scale((float) (radius * 2.1)).lifetime(all).fade(ICE_SPREADS, 20).spin(0.6f);
        ParticleShapes.model(sl, rim, ground.add(0, 0.06, 0), 0f, 0f, 0f);
        cloudOver(sl, ground, radius * 2.4, 15.0, all, 0xFFB0C8E8);
        RandomSource random = sl.getRandom();
        int spikes = (int) Math.round(radius * 1.6);
        for (int i = 0; i < spikes; i++) {
            int delay = 6 + random.nextInt(ICE_SPREADS + 20);
            float size = 0.7f + random.nextFloat() * 0.8f;
            float hidden = -2.3f * size;
            int life = Math.max(20, all - delay);
            ModelFx spike = ICE_SPIKE.scale(size).delay(delay).lifetime(life).fade(0, 8)
                    .key(0, pose().up(hidden))
                    .key(5, pose().up(0f), Ease.OUT_BACK)
                    .during(life - 10, life, pose().up(hidden), Ease.IN);
            Vec3 at = randomSpot(random, ground, radius * 0.9);
            ParticleShapes.model(sl, spike, at, random.nextFloat() * 360f, (random.nextFloat() - 0.5f) * 24f, 0f);
        }
        for (int t = 0; t < all; t += 4) {
            Later.run(sl, t, () -> {
                for (int i = 0; i < 4; i++) {
                    ParticleShapes.spawn(sl, SNOW, randomSpot(random, ground, radius).add(0, 6 + random.nextDouble() * 3, 0),
                            new Vec3(0, -0.08, 0));
                }
            });
        }
    }

    // ---------- Black Death ----------

    /** A plague cloud that can grow while it lasts. */
    private static final class Cloud {
        Vec3 center;
        double radius;
        int left;
        String tag;
    }

    private static final Map<String, Cloud> CLOUDS = new HashMap<>();

    /**
     * A cloud of plague settles over 'center', 'radius' wide, for 'ticks' ticks, smoke and spores rolling in it.
     * Make it bigger while it lasts with blackDeathGrow (same tag); blackDeathRadius says how big it is now.
     */
    public static void blackDeath(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        Cloud cloud = new Cloud();
        cloud.center = center;
        cloud.radius = radius;
        cloud.left = ticks;
        cloud.tag = tag;
        CLOUDS.put(tag, cloud);
        cloudModels(sl, cloud, radius * 0.3, 14);
        rollCloud(sl, cloud);
    }

    /** The cloud with this tag swells to 'radius'. */
    public static void blackDeathGrow(ServerLevel sl, String tag, double radius) {
        Cloud cloud = CLOUDS.get(tag);
        if (cloud == null || cloud.left <= 0) return;
        double was = cloud.radius;
        cloud.radius = radius;
        cloudModels(sl, cloud, was, 8);
        ParticleShapes.burst(sl, SPORE, cloud.center, 20, 0.05, 0.2);
    }

    /** How wide the cloud with this tag is now (0 = it is gone). */
    public static double blackDeathRadius(String tag) {
        Cloud cloud = CLOUDS.get(tag);
        return cloud == null || cloud.left <= 0 ? 0 : cloud.radius;
    }

    private static void cloudModels(ServerLevel sl, Cloud cloud, double from, int swell) {
        ParticleShapes.clearModels(sl, cloud.center, cloud.tag);
        RandomSource random = sl.getRandom();
        int life = Math.max(swell + 4, cloud.left);
        float grow = (float) (from / Math.max(0.1, cloud.radius));
        for (int i = 0; i < 6; i++) {
            double angle = Math.PI * 2 * i / 6;
            double out = i == 0 ? 0 : cloud.radius * 0.45;
            Vec3 at = cloud.center.add(Math.cos(angle) * out, (i == 0 ? 0.6 : 0.2) + random.nextDouble() * 0.5, Math.sin(angle) * out);
            float size = (float) (cloud.radius * (i == 0 ? 1.2 : 0.8));
            ModelFx puff = PLAGUE_CLOUD.color(0xC0FFFFFF).scale(size, size * 0.55f, size).lifetime(life).fade(6, 20)
                    .spin(i % 2 == 0 ? 0.8f : -0.8f).tag(cloud.tag)
                    .key(0, pose().scale(Math.max(0.1f, grow)))
                    .key(swell, pose().scale(1f), Ease.OUT);
            ParticleShapes.model(sl, puff, at, random.nextFloat() * 360f, 0f, 0f);
        }
    }

    private static void rollCloud(ServerLevel sl, Cloud cloud) {
        if (cloud.left <= 0 || CLOUDS.get(cloud.tag) != cloud) {
            if (CLOUDS.get(cloud.tag) == cloud) CLOUDS.remove(cloud.tag);
            return;
        }
        RandomSource random = sl.getRandom();
        int count = (int) Math.ceil(cloud.radius * 1.5);
        for (int i = 0; i < count; i++) {
            Vec3 at = randomSpot(random, cloud.center, cloud.radius).add(0, random.nextDouble() * 1.8, 0);
            ParticleShapes.spawn(sl, PLAGUE_SMOKE, at, new Vec3(0, 0.005, 0));
            ParticleShapes.spawn(sl, SPORE, at, new Vec3((random.nextDouble() - 0.5) * 0.04, 0.01, (random.nextDouble() - 0.5) * 0.04));
        }
        cloud.left -= 5;
        Later.run(sl, 5, () -> rollCloud(sl, cloud));
    }

    // ---------- Black Flame Rain ----------

    /** Ticks from a falling black flame appearing to it hitting the ground. */
    public static final int FLAME_FALLS = 12;

    /** One ball of black flame streaking down onto 'spot' at a slant; it bursts into black fire at FLAME_FALLS. */
    public static void blackFlameDrop(ServerLevel sl, Vec3 spot, float yaw) {
        Vec3 back = ahead(yaw).scale(-6).add(0, 18, 0);              // comes down from behind, high up
        float length = (float) back.length();
        Vec3 along = back.normalize();
        ModelFx ball = BLACK_FLAME.scale(0.9f).lifetime(FLAME_FALLS).spin(30f)
                .key(0, pose().up(length).alpha(0f))
                .during(0, 3, pose().alpha(1f))
                .during(0, FLAME_FALLS, pose().up(0.3f));
        ParticleShapes.modelAlong(sl, ball, spot, along);
        ParticleShapes.slashBetween(sl, FLAME_STREAK.width(0.4f).lifetime(FLAME_FALLS + 4).sweep(FLAME_FALLS),
                spot.add(back), spot.add(0, 0.3, 0));
        Later.run(sl, FLAME_FALLS, () -> {
            ModelFx fire = BLACK_FLAMES.scale(0.9f, 0.45f, 0.9f).lifetime(24).fade(0, 12)
                    .key(0, pose().scale(0.3f))
                    .key(4, pose().scale(1.2f), Ease.OUT_BACK)
                    .key(24, pose().scale(0.5f), Ease.IN);
            ParticleShapes.model(sl, fire, spot, sl.getRandom().nextFloat() * 360f, 0f, 0f);
            ParticleShapes.burst(sl, EMBER_BLACK, spot.add(0, 0.3, 0), 8, 0.05, 0.18);
        });
    }

    /**
     * Black fire rains over 'radius' round 'ground' for 'ticks' ticks under a violet-black sky: about 'perSecond'
     * drops a second, each landing FLAME_FALLS after it appears.
     */
    public static void blackFlameRain(ServerLevel sl, Vec3 ground, double radius, int ticks, int perSecond) {
        cloudOver(sl, ground, radius * 2.8, 19.0, ticks + FLAME_FALLS, 0xFF6A3090);
        RandomSource random = sl.getRandom();
        float yaw = random.nextFloat() * 360f;
        int every = Math.max(1, 20 / Math.max(1, perSecond));
        for (int t = 0; t < ticks; t += every) {
            Later.run(sl, t, () -> blackFlameDrop(sl, randomSpot(random, ground, radius), yaw + (random.nextFloat() - 0.5f) * 30f));
        }
    }

    // ---------- Doomsday Sky ----------

    public static final int METEOR_FALLS = 22;
    public static final int LIGHTNING_HITS = 3;

    /** A meteor in red fire coming down on 'spot' from far off: it lands at METEOR_FALLS. */
    public static void meteorDrop(ServerLevel sl, Vec3 spot, float size) {
        RandomSource random = sl.getRandom();
        Vec3 path = ahead(random.nextFloat() * 360f).scale(12).add(0, 24, 0);
        float length = (float) path.length();
        Vec3 along = path.normalize();
        for (ModelFx m : new ModelFx[]{METEOR.spin(16f), METEOR_TAIL}) {
            ModelFx flight = m.scale(size).lifetime(METEOR_FALLS)
                    .key(0, pose().up(length).alpha(0f))
                    .during(0, 3, pose().alpha(1f))
                    .during(0, METEOR_FALLS, pose().up(0f));
            ParticleShapes.modelAlong(sl, flight, spot, along);
        }
        SpellFx.warningCircle(sl, spot, 1.5 * size, 0xD0E0203A, METEOR_FALLS);
        Later.run(sl, METEOR_FALLS, () -> {
            SpellFx.blast(sl, spot.add(0, 0.6, 0), 2.2 * size, 0xFFFF3A2A, EMBER_BLACK);
            rocks(sl, spot, 1.5 * size, 4, 2f, 0.7f);
        });
    }

    /** A red bolt strikes 'spot'. It hits at LIGHTNING_HITS. */
    public static void lightningStrike(ServerLevel sl, Vec3 spot) {
        RandomSource random = sl.getRandom();
        ModelFx bolt = LIGHTNING.scale(3f, 4f, 3f).lifetime(10).fade(0, 6)
                .key(0, pose().up(10f).alpha(0f))
                .key(LIGHTNING_HITS, pose().up(0f).alpha(1f), Ease.IN);
        ParticleShapes.model(sl, bolt, spot, random.nextFloat() * 360f, 0f, 0f);
        Later.run(sl, LIGHTNING_HITS, () -> {
            ParticleShapes.burst(sl, BLOOD_MOTE, spot.add(0, 0.3, 0), 14, 0.08, 0.3);
            SpellFx.shockRing(sl, spot, 2.0, 0xF0FF3048, 6, 0);
        });
    }

    /**
     * The sky over 'ground' turns a swirling dark red for 'ticks' ticks and doom falls out of it over 'radius':
     * meteors, black flame and red lightning, about 'perSecond' a second, at random. Use the single drops
     * (meteorDrop, blackFlameDrop, lightningStrike) yourself to aim them at enemies.
     */
    public static void doomsdaySky(ServerLevel sl, Vec3 ground, double radius, int ticks, int perSecond) {
        int all = Math.max(40, ticks);
        bloodSky(sl, ground, radius, all);
        RandomSource random = sl.getRandom();
        int every = Math.max(1, 20 / Math.max(1, perSecond));
        for (int t = 20; t < all - 20; t += every) {
            Later.run(sl, t, () -> {
                Vec3 spot = randomSpot(random, ground, radius);
                int pick = random.nextInt(3);
                if (pick == 0) meteorDrop(sl, spot, 0.8f + random.nextFloat() * 0.6f);
                else if (pick == 1) blackFlameDrop(sl, spot, random.nextFloat() * 360f);
                else lightningStrike(sl, spot);
            });
        }
    }

    /**
     * Only the sky of the Doomsday: a swirling dark red sky over 'ground' and a red circle on the ground under it,
     * 'radius' wide, for 'ticks' ticks, with nothing falling. Drop what falls yourself (meteorDrop,
     * blackFlameDrop, lightningStrike) where you want it to hit.
     */
    public static void bloodSky(ServerLevel sl, Vec3 ground, double radius, int ticks) {
        int all = Math.max(40, ticks);
        ModelFx sky = SKY.scale((float) (radius * 3.5)).lifetime(all).fade(30, 30).spin(0.4f);
        ParticleShapes.model(sl, sky, ground.add(0, 22, 0), 0f, 0f, 0f);
        ModelFx glow = SpellFx.RUNE_CIRCLE.color(0x70E0203A).scale((float) (radius * 2.0)).lifetime(all).fade(20, 20).spin(-0.5f);
        ParticleShapes.model(sl, glow, ground.add(0, 0.05, 0), 0f, 0f, 0f);
    }

    /**
     * Only the cloud of the Black Flame Rain: a violet-black cloud over 'ground' for 'ticks' ticks, 'radius' being
     * the width of the rain under it. Drop the flames yourself with blackFlameDrop where they should hit.
     */
    public static void rainCloud(ServerLevel sl, Vec3 ground, double radius, int ticks) {
        cloudOver(sl, ground, radius * 2.8, 19.0, ticks, 0xFF6A3090);
    }

    // =====================================================================================
    // Small helpers
    // =====================================================================================

    /** A cloud hanging 'height' blocks over 'ground', 'wide' blocks across, tinted 'color', for 'ticks' ticks. */
    private static void cloudOver(ServerLevel sl, Vec3 ground, double wide, double height, int ticks, int color) {
        ModelFx cloud = SKY.color(color).scale((float) wide).lifetime(Math.max(20, ticks)).fade(20, 20).spin(0.6f);
        ParticleShapes.model(sl, cloud, ground.add(0, height, 0), 0f, 0f, 0f);
    }

    /** Smoke drawn in and a ring going out at a spot: someone steps out of nowhere. */
    private static void appearAt(ServerLevel sl, Vec3 ground) {
        ParticleShapes.burst(sl, SHADOW_SMOKE, ground.add(0, 0.9, 0), 10, 0.01, 0.05);
        SpellFx.shockRing(sl, ground, 1.2, 0xC0B050FF, 7, 0);
    }

    /** A random spot on a round patch of ground, evenly spread. */
    private static Vec3 randomSpot(RandomSource random, Vec3 ground, double radius) {
        double angle = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
        return ground.add(Math.cos(angle) * far, 0, Math.sin(angle) * far);
    }

    /** One block the way a yaw looks, level with the ground. */
    private static Vec3 ahead(float yaw) {
        double angle = Math.toRadians(yaw);
        return new Vec3(-Math.sin(angle), 0, Math.cos(angle));
    }

    /** The yaw that looks from one point to another. */
    private static float yawToward(Vec3 from, Vec3 to) {
        return (float) Math.toDegrees(Math.atan2(-(to.x - from.x), to.z - from.z));
    }

    // =====================================================================================
    // The display cases
    // =====================================================================================

    private interface Show {
        void play(ServerLevel sl, Player viewer, Vec3 spot, float yaw);
    }

    /** onViewer = it is played on the viewer, not on a spot; ticks = how long it lasts. */
    private record Case(String name, int ticks, boolean onViewer, Show show) {}

    private static final List<Case> CASES = List.of(
            new Case("fatal_step", 30, false, (sl, viewer, spot, yaw) -> {
                Vec3 a = spot.add(ahead(yaw).scale(-2.5));
                standIn(sl, a);
                Vec3 behind = spot.add(ahead(yaw + 180f).scale(-1.2));
                ParticleShapes.burst(sl, SHADOW_SMOKE, behind.add(0, 0.9, 0), 10, 0.01, 0.05);
                showBackstab(sl, behind, spot);
            }),
            new Case("mirror_shard", 120, false, (sl, viewer, spot, yaw) -> {
                String tag = "demonessfx_shard_" + Math.round(spot.x) + "_" + Math.round(spot.z);
                placeShard(sl, spot, yaw, 120, tag);
                for (int i = 0; i < 5; i++) {
                    float charge = (i + 1) / 5f;
                    Later.run(sl, 15 + i * 16, () -> shardCharge(sl, spot, yaw, charge, tag));
                }
                Later.run(sl, 100, () -> shardExplode(sl, spot, 1f, tag));
            }),
            new Case("black_flame", 50, false, (sl, viewer, spot, yaw) -> {
                Vec3 from = spot.add(ahead(yaw).scale(-6)).add(0, 1.4, 0);
                Vec3 to = spot.add(0, 1.0, 0);
                int lands = blackFlameBolt(sl, from, to);
                Later.run(sl, lands, () -> {
                    blackFlameHit(sl, to);
                    ModelFx fire = BLACK_FLAMES.scale(1.1f, 0.9f, 1.1f).lifetime(40).fade(4, 10);
                    ParticleShapes.model(sl, fire, spot, 0f, 0f, 0f);
                });
            }),
            new Case("mirror_substitute", 60, false, (sl, viewer, spot, yaw) -> {
                String tag = "demonessfx_mirror_" + Math.round(spot.x) + "_" + Math.round(spot.z);
                ModelFx mirror = MIRROR.lifetime(60).fade(0, 8).tag(tag)
                        .key(0, pose().up(-2.4f).alpha(0f))
                        .key(MIRROR_RISES, pose().up(0f).alpha(1f), Ease.OUT_BACK);
                ParticleShapes.model(sl, mirror, spot, yaw, 0f, 0f);
                Later.run(sl, 36, () -> mirrorShatter(sl, spot, spot.add(ahead(yaw).scale(5)).add(0, 1, 0), tag));
            }),
            new Case("mirror_world", MIRROR_DOOR_TICKS, false, (sl, viewer, spot, yaw) -> mirrorDoor(sl, spot, yaw)),
            new Case("mirror_return", 30, false, (sl, viewer, spot, yaw) -> mirrorReturn(sl, spot, yaw)),
            new Case("silk_snare", 80, false, (sl, viewer, spot, yaw) -> {
                Vec3 from = spot.add(ahead(yaw).scale(-6)).add(0, 1.5, 0);
                silkSnare(sl, from, spot, 2.5, 70, "demonessfx_snare_" + Math.round(spot.x) + "_" + Math.round(spot.z));
            }),
            new Case("web_swing", 30, false, (sl, viewer, spot, yaw) -> {
                Vec3 from = spot.add(0, 1.4, 0);
                Vec3 anchor = spot.add(ahead(yaw).scale(4)).add(0, 5, 0);
                ParticleShapes.slashBetween(sl, SILK_SHOT.lifetime(SWING_SHOT + 3).sweep(SWING_SHOT), from, anchor);
                Later.run(sl, SWING_SHOT, () -> SpellFx.tether(sl, SILK, () -> from, () -> anchor, 24));
                ModelFx stuck = WEB.scale(1.1f).delay(SWING_SHOT).lifetime(34).fade(0, 10)
                        .key(0, pose().scale(0.2f))
                        .key(4, pose().scale(1f), Ease.OUT_BACK);
                ParticleShapes.modelAlong(sl, stuck, anchor, from.subtract(anchor));
            }),
            new Case("trip_web", 60, false, (sl, viewer, spot, yaw) -> {
                Vec3 side = ahead(yaw + 90f);
                Vec3 a = spot.add(side.scale(-2.5)).add(0, 0.4, 0);
                Vec3 b = spot.add(side.scale(2.5)).add(0, 0.4, 0);
                String tag = "demonessfx_trip_" + Math.round(spot.x) + "_" + Math.round(spot.z);
                boolean[] snapped = {false};
                tripWeb(sl, a, b, 60, () -> !snapped[0], tag);
                Later.run(sl, 40, () -> {
                    snapped[0] = true;
                    tripSnap(sl, a, b, tag);
                });
            }),
            new Case("cocoon", 80, false, (sl, viewer, spot, yaw) -> {
                ModelFx wrap = COCOON.lifetime(80).fade(0, 6)
                        .key(0, pose().frame(0).alpha(0f).spin(-90f))
                        .key(4, pose().alpha(1f))
                        .during(2, COCOON_SHUT, pose().frame(2).spin(0f), Ease.IN_OUT)
                        .during(80 - COCOON_UNWRAP, 80, pose().frame(0).spin(120f), Ease.IN);
                ParticleShapes.model(sl, wrap, spot, yaw, 0f, 0f);
            }),
            new Case("threads_of_life", 60, false, (sl, viewer, spot, yaw) -> {
                Vec3 mob = spot.add(0, 1.0, 0);
                Vec3 you = spot.add(ahead(yaw).scale(-4)).add(0, 1.0, 0);
                for (int i = 0; i < 3; i++) {
                    double lift = (i - 1) * 0.3;
                    SpellFx.tether(sl, LIFE_THREAD, () -> mob.add(0, lift, 0), () -> you, 40);
                }
                for (int t = 0; t < 40; t += 6) {
                    Later.run(sl, t, () -> ParticleShapes.cone(sl, BLOOD_MOTE, mob, you.subtract(mob), 8, 2, 0.2, 0.26));
                }
                Later.run(sl, 44, () -> {
                    rocks(sl, spot, 0.9, 5, 1.5f, 0.6f);
                    SpellFx.risingRings(sl, spot, 0.9, 1.8, 4, 0xE0A09890);
                });
            }),
            new Case("web_of_fate", 100, false, (sl, viewer, spot, yaw) -> {
                Vec3 center = spot.add(0, 4, 0);
                String tag = "demonessfx_fate_" + Math.round(spot.x) + "_" + Math.round(spot.z);
                webOfFate(sl, center, 6, 100, tag);
                Later.run(sl, 80, () -> fateSnap(sl, center, 6, tag));
            }),
            new Case("plague", 80, false, (sl, viewer, spot, yaw) -> {
                ModelFx seal = PLAGUE_SIGIL.scale(0.75f).lifetime(60).fade(4, 10).spin(3f)
                        .key(0, pose().scale(0.2f).alpha(0f))
                        .key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
                ParticleShapes.model(sl, seal, spot.add(0, 2.2, 0), 0f, 0f, 0f);
                for (int t = 4; t < 60; t += 10) {
                    Later.run(sl, t, () -> ParticleShapes.burst(sl, PLAGUE_SMOKE, spot.add(0, 1, 0), 2, 0.01, 0.03));
                }
                Later.run(sl, 60, () -> {
                    ModelFx cloud = PLAGUE_CLOUD.lifetime(16).fade(0, 10)
                            .key(0, pose().scale(0.5f))
                            .key(16, pose().scale(2.2f), Ease.OUT);
                    ParticleShapes.model(sl, cloud, spot.add(0, 1, 0), 0f, 0f, 0f);
                    ParticleShapes.burst(sl, SPORE, spot.add(0, 1, 0), 16, 0.05, 0.18);
                });
            }),
            new Case("curse", 70, false, (sl, viewer, spot, yaw) -> {
                ModelFx eye = CURSE_SIGIL.scale(0.8f).lifetime(50).fade(4, 4).spin(-2f)
                        .key(0, pose().scale(0.2f).alpha(0f))
                        .key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
                ParticleShapes.model(sl, eye, spot.add(0, 2.3, 0), 0f, 0f, 0f);
                ModelFx bolt = LIGHTNING.scale(2.4f).delay(50).lifetime(10).fade(0, 6)
                        .key(0, pose().up(8f).alpha(0f))
                        .key(CURSE_RELEASE_HITS, pose().up(0f).alpha(1f), Ease.IN);
                ParticleShapes.model(sl, bolt, spot, 0f, 0f, 0f);
                Later.run(sl, 50 + CURSE_RELEASE_HITS, () -> SpellFx.blast(sl, spot.add(0, 1, 0), 2.2, 0xFFE0203A, BLOOD_MOTE));
            }),
            new Case("unaging", REVIVE_TICKS, true, (sl, viewer, spot, yaw) -> {
                String tag = "demonessfx_ward";
                unagingWard(sl, viewer, 40, tag);
                Later.run(sl, 40, () -> unagingRevive(sl, viewer, tag));
            }),
            new Case("earthquake", QUAKE_TICKS, false, (sl, viewer, spot, yaw) -> earthquake(sl, spot, 5)),
            new Case("hailstorm", 120, false, (sl, viewer, spot, yaw) -> hailstorm(sl, spot, 4, 100, 10)),
            new Case("tornado", 100, false, (sl, viewer, spot, yaw) -> tornado(sl, spot.add(ahead(yaw).scale(-3)), yaw, 0.06, 100)),
            new Case("ice_age", 120, false, (sl, viewer, spot, yaw) -> iceAge(sl, spot, 5, 120)),
            new Case("black_death", 120, false, (sl, viewer, spot, yaw) -> {
                String tag = "demonessfx_cloud_" + Math.round(spot.x) + "_" + Math.round(spot.z);
                blackDeath(sl, spot.add(0, 0.8, 0), 2, 110, tag);
                Later.run(sl, 35, () -> blackDeathGrow(sl, tag, 3));
                Later.run(sl, 70, () -> blackDeathGrow(sl, tag, 4.2));
            }),
            new Case("black_flame_rain", 110, false, (sl, viewer, spot, yaw) -> blackFlameRain(sl, spot, 4, 90, 8)),
            new Case("doomsday_sky", 160, false, (sl, viewer, spot, yaw) -> doomsdaySky(sl, spot, 6, 160, 4)));

    /** A dark figure-shaped puff of smoke where the caster would stand (for the display, which has no caster). */
    private static void standIn(ServerLevel sl, Vec3 ground) {
        ParticleShapes.burst(sl, SHADOW_SMOKE, ground.add(0, 0.9, 0), 12, 0.01, 0.06);
        SpellFx.closingRing(sl, ground, 1.4, 0xC0B050FF, 6, 0);
    }

    /** The backstab without entities: a dagger stabbing from 'behind' into 'spot'. */
    private static void showBackstab(ServerLevel sl, Vec3 behind, Vec3 spot) {
        Vec3 hand = behind.add(0, 1.1, 0);
        Vec3 into = spot.add(0, 1.0, 0);
        Vec3 along = into.subtract(hand).normalize();
        float reach = (float) Math.max(0.4, into.distanceTo(hand) - 0.2);
        ModelFx dagger = DAGGER.lifetime(BACKSTAB_TICKS).fade(0, 6)
                .key(0, pose().up(-0.3f).scale(0.4f).alpha(0f))
                .key(4, pose().up(-0.8f).scale(1f).alpha(1f), Ease.OUT_BACK)
                .key(BACKSTAB_HITS, pose().up(reach), Ease.IN)
                .during(BACKSTAB_HITS, BACKSTAB_HITS + 3, pose().spin(25f));
        ParticleShapes.modelAlong(sl, dagger, hand, along);
        Later.run(sl, BACKSTAB_HITS, () -> {
            ParticleShapes.slashBetween(sl, STAB.lifetime(6).sweep(2), hand, into.add(along.scale(0.4)));
            ParticleShapes.cone(sl, VIOLET_MOTE, into, along, 70, 14, 0.08, 0.3);
        });
    }

    /** The names /demonessfx knows. */
    public static List<String> names() {
        List<String> names = new ArrayList<>();
        for (Case c : CASES) names.add(c.name());
        return names;
    }

    /** One display, 9 blocks in front of the viewer and turned toward them. False if there is none of that name. */
    public static boolean show(ServerLevel sl, Player viewer, String name) {
        for (Case c : CASES) {
            if (!c.name().equals(name)) continue;
            float look = viewer.getYRot();
            Vec3 spot = viewer.position().add(ahead(look).scale(9));
            c.show().play(sl, viewer, spot, look + 180f);
            return true;
        }
        return false;
    }

    /** Every display at once, in a ring 32 blocks out from the viewer, each played 'rounds' times. */
    public static void gallery(ServerLevel sl, Player viewer, int rounds) {
        List<Case> standing = new ArrayList<>();
        for (Case c : CASES) if (!c.onViewer()) standing.add(c);
        Vec3 middle = viewer.position();
        for (Case c : CASES) {
            int place = Math.max(0, standing.indexOf(c));
            float out = viewer.getYRot() + place * 360f / standing.size();     // the first one is straight ahead
            Vec3 spot = middle.add(ahead(out).scale(32));
            float yaw = out + 180f;
            for (int round = 0; round < rounds; round++) {
                Later.run(sl, round * (c.ticks() + 20), () -> {
                    if (viewer.isAlive()) c.show().play(sl, viewer, spot, yaw);
                });
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("demonessfx")
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
