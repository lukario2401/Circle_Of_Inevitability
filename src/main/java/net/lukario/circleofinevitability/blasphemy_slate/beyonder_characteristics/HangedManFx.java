package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.lukario.circleofinevitability.combat.HudTimer;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Shot;
import net.lukario.circleofinevitability.combat.SpellFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.particles.fx.ModelFx;
import net.lukario.circleofinevitability.particles.fx.ModelFx.Ease;
import net.lukario.circleofinevitability.particles.fx.ParticleFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.particles.fx.SlashFx;
import net.lukario.circleofinevitability.screen.ScreenAnchor;
import net.lukario.circleofinevitability.screen.ScreenImages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * How the Hanged Man pathway looks and sounds: every key, both casts, and the Grazing soul theft.
 * Visual only: nothing here hurts, roots or heals anyone. The ability code (HangedMan) does that, at the ticks named by the
 * constants below (..._GRIPS, ..._ARRIVES...), with Later.run.
 *
 * ---------- See them in game ----------
 *   /hangedfx all              every display in a ring around you (/hangedfx all 3 = three times over)
 *   /hangedfx binding          just that one (press TAB after /hangedfx for the names)
 * Displays that need a target use the nearest mob within 12 blocks, or you when there is none.
 * The models are made by tools/model_gen/hanged_man/models.py (render.py there draws one without the game).
 *
 * ---------- Use them in an ability ----------
 *   Key 1  divination(sl, player)            then divinationReveal(sl, mob, ticks) / divinationMark(sl, strongest, ticks)
 *          ritualCircle(sl, ground, radius, ticks), ritualPulse(sl, ground, radius), ritualPrice(sl, player)
 *   Key 2  whispers(sl, player, target, ticks), whispersEnd(sl, target)
 *          listenPulse(sl, player, radius), listenWarning(sl, attacker, player)
 *   Key 3  bindingWarning(sl, ground, radius), bind(sl, enemy, ticks)            (grips at BIND_GRIPS)
 *          lurkEnter(sl, player, ticks), lurkTick(sl, player), lurkExit(sl, player)
 *   Key 4  FLESH_BOMB.onHit(...).fire(...), fleshBombStick(sl, target, ticks), fleshBombBurst(sl, center, radius, target)
 *          fleshCloak(sl, player, ticks), fleshCloakAbsorb(sl, player), fleshCloakRegen(sl, player), fleshCloakEnd(sl, player)
 *   Key 5  String soul = graveSoul(sl, corpseSpot, color, ticks), graveSoulTick(sl, spot), graveSoulFades(sl, spot, soul)
 *          soulStolen(sl, spot, soul, player, color) (in at STEAL_ARRIVES), stolenCast(sl, player), emptySlot(sl, player)
 *   Key 6  cull(sl, player), cullHit(sl, enemy, ticks)
 *          blackArmor(sl, player, ticks, size), blackArmorHit(sl, player), blackArmorEnd(sl, player)
 *   Key 7  degeneration(sl, player, target) (lands at DEGENERATION_ARRIVES), degenerationMark(sl, target, ticks),
 *          degenerationStrip(sl, target, removedEffects), degenerationBetray(sl, target, ally, ticks), degenerationTick(sl, target)
 *          trinity(sl, player, ticks), trinityEcho(sl, player), trinityEnd(sl, player)
 *   Every timed cast: status(player, Status.X, ticks) shows its picture and its clock; statusEnd(player, Status.X) takes them away.
 *
 * The models are in models/model_fx/hanged_man/ (made by a generator; see each JSON's "credit" for its size and pivot),
 * the pictures in textures/gui/hanged_man/.
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class HangedManFx {

    private HangedManFx() {}

    private static ModelFx.Pose pose() {
        return ModelFx.pose();
    }

    // =====================================================================================
    // Colors (ARGB) and the small particles everything shares
    // =====================================================================================

    public static final int CRIMSON = 0xFF7A0F1E;
    public static final int CRIMSON_GLOW = 0xE0D0283C;
    public static final int SHADOW = 0xE0100810;
    public static final int FLESH = 0xFFC9727A;
    public static final int CORROSION = 0xFF3A4A1E;
    public static final int SOUL = 0xB0A8C8E0;

    private static final ParticleFx CRIMSON_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFE0283C).endColor(0x007A0F1E)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(18, 10).gravity(-0.02f).friction(0.92f).glow();
    private static final ParticleFx BLOOD = ParticleFx.of("fx/spark")
            .color(0xFFA01020).endColor(0xC0400810)
            .size(0.06f).endSize(0.04f).sizeRandom(0.4f)
            .lifetime(24, 10).gravity(0.06f).friction(0.98f).collide();
    private static final ParticleFx SHADOW_WISP = ParticleFx.of("fx/smoke")
            .color(0xC0100810).endColor(0x00080408)
            .size(0.25f).endSize(0.6f).sizeRandom(0.3f)
            .lifetime(24, 12).gravity(-0.015f).friction(0.9f)
            .spin(2f).randomRotation();
    private static final ParticleFx FLESH_CHUNK = ParticleFx.of("fx/splinter")
            .color(0xFFC9727A).endColor(0xFF7A2632)
            .size(0.08f).endSize(0.06f).sizeRandom(0.5f)
            .lifetime(26, 10).gravity(0.07f).friction(0.96f).collide()
            .spin(12f).randomRotation();
    private static final ParticleFx FLESH_MOTE = ParticleFx.of("fx/glow")
            .color(0xC0E08A90).endColor(0x00C9727A)
            .size(0.07f).endSize(0.02f)
            .lifetime(20, 8).gravity(-0.03f).friction(0.9f);
    private static final ParticleFx CORRODE = ParticleFx.of("fx/glow")
            .color(0xE05A6A20).endColor(0x00202A08)
            .size(0.09f).endSize(0.03f).sizeRandom(0.4f)
            .lifetime(20, 10).gravity(0.01f).friction(0.9f);
    private static final ParticleFx ASH = ParticleFx.of("fx/flake")
            .color(0xE0302830).endColor(0x00201820)
            .size(0.05f).endSize(0.04f).sizeRandom(0.5f)
            .lifetime(34, 16).gravity(0.012f).friction(0.95f)
            .spin(6f).randomRotation();
    private static final ParticleFx ARMOR_SHARD = ParticleFx.of("fx/splinter")
            .color(0xFFB8B8C4).endColor(0xC0505060)
            .size(0.07f).endSize(0.05f).sizeRandom(0.5f)
            .lifetime(22, 8).gravity(0.07f).friction(0.95f).collide()
            .spin(18f).randomRotation();
    private static final ParticleFx STEEL_SPARK = ParticleFx.of("fx/spark")
            .color(0xFFFFE0B0).endColor(0x00FF6020)
            .size(0.05f).endSize(0.01f)
            .lifetime(8, 4).gravity(0.04f).friction(0.9f).glow();
    private static final ParticleFx WHISPER = ParticleFx.of("fx/sliver")
            .color(0xA0E8A8AC).endColor(0x007A0F1E)
            .size(0.1f).endSize(0.03f).sizeRandom(0.3f)
            .lifetime(22, 8).gravity(-0.03f).friction(0.9f)
            .spin(8f).randomRotation();
    private static final ParticleFx SOUL_MOTE = ParticleFx.of("fx/glow")
            .color(0xE0DCECF8).endColor(0x0078A0C8)
            .size(0.07f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(26, 10).gravity(-0.025f).friction(0.92f).glow();

    // Some SoundEvents constants are a SoundEvent, others a Holder of one: both overloads take either.
    private static void play(ServerLevel sl, Vec3 at, SoundEvent sound, float volume, float pitch) {
        Spells.sound(sl, at, sound, volume, pitch);
    }

    private static void play(ServerLevel sl, Vec3 at, Holder<SoundEvent> sound, float volume, float pitch) {
        Spells.sound(sl, at, sound.value(), volume, pitch);
    }

    /** A sound only this player hears (what 1.21's playNotifySound did; see KeyOfStars.playNotifySound). */
    private static void playTo(Player player, Holder<SoundEvent> sound, float volume, float pitch) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        serverPlayer.connection.send(new ClientboundSoundPacket(sound, SoundSource.PLAYERS,
                player.getX(), player.getY(), player.getZ(), volume, pitch, player.getRandom().nextLong()));
    }

    private static void playTo(Player player, SoundEvent sound, float volume, float pitch) {
        playTo(player, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), volume, pitch);
    }

    private static Vec3 chestOf(Entity entity) {
        return entity.position().add(0, entity.getBbHeight() * 0.6, 0);
    }

    /** 'model' swinging between two poses, 'period' ticks each way, from tick 'from' to tick 'to'. */
    private static ModelFx swinging(ModelFx model, int from, int to, int period, ModelFx.Pose a, ModelFx.Pose b) {
        ModelFx out = model;
        boolean toA = true;
        for (int t = from; t < to; t += period) {
            out = out.during(t, Math.min(to, t + period), toA ? a : b, Ease.IN_OUT);
            toA = !toA;
        }
        return out;
    }

    // =====================================================================================
    // Statuses: a picture in the row above the hotbar stacks, and a clock beside the hotbar
    // =====================================================================================

    public enum Status {
        RITUAL("ritual", "Ritual", 0xE0283C),
        LISTEN("listen", "Listen", 0xE8A8AC),
        LURKING("shadow_lurking", "Lurking", 0x9A7A98),
        FLESH_CLOAK("flesh_cloak", "Flesh Cloak", 0xC9727A),
        BLACK_ARMOR("black_armor", "Black Armor", 0xC02A3A),
        TRINITY("trinity", "Trinity", 0xB08AB0),
        WHISPERS("whispers", "Whispers", 0xE8A8AC),
        DIVINATION("divination", "Divination", 0xE0283C);

        final String image;
        final String label;
        final int rgb;

        Status(String image, String label, int rgb) {
            this.image = image;
            this.label = label;
            this.rgb = rgb;
        }

        String id() {
            return "hm_status_" + name().toLowerCase();
        }
    }

    /** Shows the status's picture (the same row and spacing as the Attendant's) and a clock beside the hotbar. */
    public static void status(Player player, Status status, int ticks) {
        ScreenImages.show(player, status.id(), "hanged_man/" + status.image,
                ScreenAnchor.BOTTOM, 103 + status.ordinal() * 18, -21, 16, ticks);
        HudTimer.show(player, status.id(), status.label, ticks, status.rgb);
    }

    /** It ended early. */
    public static void statusEnd(Player player, Status status) {
        ScreenImages.hide(player, status.id());
        HudTimer.hide(player, status.id());
    }

    // =====================================================================================
    // Key 1: Divination (Secrets Suppliant)
    // =====================================================================================

    public static final int DIVINATION_TICKS = 44;
    /** The tick the pendulum stops on the strongest one and the pulse goes out. */
    public static final int DIVINATION_PULSE = 22;
    private static final String DIVINATION_MARK = "hm_divination_mark";

    /** The pendulum, 1.4 blocks long at scale 1. Held from its top. */
    public static final ModelFx PENDULUM = ModelFx.of("hanged_man/pendulum")
            .pivot(8, 24, 8).glow().aura(0x40D0283C, 0.06f, 3);

    private static final ModelFx PENDULUM_HELD = swinging(PENDULUM.scale(0.6f).lifetime(DIVINATION_TICKS).fade(0, 8)
                    .key(0, pose().forward(0.75f).right(0.25f).alpha(0f).roll(-35))
                    .key(4, pose().alpha(1f)),
            0, DIVINATION_PULSE, 7, pose().roll(35), pose().roll(-35))
            .during(DIVINATION_PULSE, DIVINATION_PULSE + 4, pose().roll(0), Ease.OUT);

    /** The pendulum swings in front of the caster, stops, and a crimson pulse races out 24 blocks. */
    public static void divination(ServerLevel sl, Player caster) {
        ParticleShapes.modelOnTurning(sl, PENDULUM_HELD, caster, new Vec3(0, caster.getBbHeight() * 0.82, 0), 0f, 0f, 0f);
        play(sl, caster.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.6f);
        Later.run(sl, DIVINATION_PULSE, () -> {
            if (!Spells.casterStillHere(caster, sl)) return;
            SpellFx.shockRing(sl, caster.position(), 24, 0xD0D0283C, 16, 0);
            SpellFx.shockRing(sl, caster.position(), 12, 0x90FF8A90, 12, 2);
            ParticleShapes.ring(sl, CRIMSON_MOTE, caster.position().add(0, 0.3, 0), 1.2, 24, 0.35);
            play(sl, caster.position(), SoundEvents.BELL_RESONATE, 0.8f, 1.4f);
        });
    }

    /** Something the pulse found: it glows (seen through walls) for 'ticks'. */
    public static void divinationReveal(ServerLevel sl, LivingEntity found, int ticks) {
        found.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0, false, false));
        ParticleShapes.burst(sl, CRIMSON_MOTE, chestOf(found), 6, 0.02, 0.06);
    }

    /** The strongest one: a crimson circle under it and a small pendulum swinging over its head for 'ticks'. */
    public static void divinationMark(ServerLevel sl, LivingEntity strongest, int ticks) {
        SpellFx.circleUnder(sl, strongest, Math.max(0.8, strongest.getBbWidth()), 0xC0D0283C, ticks, DIVINATION_MARK);
        ModelFx over = swinging(PENDULUM.scale(0.4f).lifetime(ticks).fade(4, 8).tag(DIVINATION_MARK + "_pendulum"),
                0, ticks, 10, pose().roll(25), pose().roll(-25));
        ParticleShapes.clearModels(sl, strongest, DIVINATION_MARK + "_pendulum");
        ParticleShapes.modelOn(sl, over, strongest, new Vec3(0, strongest.getBbHeight() + 1.0, 0), 0f, 0f, 0f);
        play(sl, strongest.position(), SoundEvents.BELL_RESONATE, 0.6f, 0.7f);
    }

    // ---------- sneak: Ritual Circle

    /** A slow crimson rune circle with four candle flames on its edge, for 'ticks'. */
    public static void ritualCircle(ServerLevel sl, Vec3 ground, double radius, int ticks) {
        SpellFx.runeCircle(sl, ground, radius, 0xC0D0283C, ticks, 0.8f);
        SpellFx.runeCircle(sl, ground.add(0, 0.01, 0), radius * 0.55, 0x907A0F1E, ticks, -1.6f);
        ModelFx candle = SpellFx.FLAMES.scale(0.16f, 0.22f, 0.16f).lifetime(ticks).fade(6, 10)
                .key(0, pose().scale(0f)).key(8, pose().scale(1f), Ease.OUT_BACK);
        for (int i = 0; i < 4; i++) {
            Vec3 at = Spells.spotAround(ground, 45f + i * 90f, radius * 0.92);
            ParticleShapes.model(sl, candle.delay(i * 3), at.add(0, 0.05, 0), 0f, 0f, 0f);
        }
        play(sl, ground, SoundEvents.ENCHANTMENT_TABLE_USE, 1.0f, 0.7f);
    }

    /** One beat of the circle (every 2 seconds while it lasts): faint rings climb out of it. */
    public static void ritualPulse(ServerLevel sl, Vec3 ground, double radius) {
        SpellFx.risingRings(sl, ground, radius * 0.6, 1.6, 3, 0x60D0283C);
        ParticleShapes.ring(sl, CRIMSON_MOTE, ground.add(0, 0.15, 0), radius * 0.9, 16, 0);
    }

    /** The price in blood: drops fall from the caster. */
    public static void ritualPrice(ServerLevel sl, Player caster) {
        ParticleShapes.burst(sl, BLOOD, chestOf(caster), 14, 0.03, 0.1);
        play(sl, caster.position(), SoundEvents.PLAYER_HURT, 0.5f, 0.7f);
    }

    // =====================================================================================
    // Key 2: Whispers (Listener)
    // =====================================================================================

    private static final String WHISPERS_MOUTHS = "hm_whispers_mouths";

    /** Four mouths in a ring, 0.9 blocks across at scale 1. */
    public static final ModelFx WHISPER_MOUTHS = ModelFx.of("hanged_man/whisper_mouths").frames(2).smooth()
            .pivot(8, 8, 8).glow();

    private static final SlashFx WHISPER_LINE = SlashFx.line("slash/smooth")
            .color(0x90100810).core(0x60D0283C).width(0.06f).taper(SlashFx.Taper.UNIFORM).translucent();

    /** A dark thread runs from the caster's head to the target, and whispering mouths circle the target's head. */
    public static void whispers(ServerLevel sl, Player caster, LivingEntity target, int ticks) {
        SpellFx.tether(sl, WHISPER_LINE, () -> SpellFx.topOf(caster, sl), () -> SpellFx.topOf(target, sl), 10);
        float scale = (float) Math.max(0.9, target.getBbWidth() * 1.5);
        ModelFx mouths = swinging(WHISPER_MOUTHS.scale(scale).lifetime(ticks).fade(6, 10).spin(4f).tag(WHISPERS_MOUTHS)
                        .key(0, pose().scale(0.3f).alpha(0f))
                        .key(8, pose().scale(1f).alpha(1f), Ease.OUT_BACK),
                0, ticks, 4, pose().frame(1), pose().frame(0));
        ParticleShapes.clearModels(sl, target, WHISPERS_MOUTHS);
        ParticleShapes.modelOn(sl, mouths, target, new Vec3(0, target.getBbHeight() - 0.25, 0), 0f, 0f, 0f);
        ParticleShapes.burst(sl, WHISPER, SpellFx.topOf(target, sl), 10, 0.02, 0.06);
        for (int i = 0; i < 3; i++) {
            float pitch = 0.6f + i * 0.35f;
            Later.run(sl, i * 3, () -> play(sl, target.position(), SoundEvents.SOUL_ESCAPE, 0.9f, pitch));
        }
    }

    /** The mark ended early. */
    public static void whispersEnd(ServerLevel sl, LivingEntity target) {
        ParticleShapes.clearModels(sl, target, WHISPERS_MOUTHS);
    }

    // ---------- sneak: Listen

    /** One sonar beat (every 2 seconds while Listen lasts). */
    public static void listenPulse(ServerLevel sl, Player caster, double radius) {
        SpellFx.steadyRing(sl, caster.position(), radius, 0x90C8C6D0, 20, 0);
        play(sl, caster.position(), SoundEvents.SCULK_CLICKING, 0.7f, 1.2f);
    }

    /** "That one is about to hit you": a line of motes runs from the attacker to the caster, who alone hears the bell. */
    public static void listenWarning(ServerLevel sl, LivingEntity attacker, Player caster) {
        ParticleShapes.line(sl, CRIMSON_MOTE, chestOf(attacker), chestOf(caster), 3);
        SpellFx.circleUnder(sl, attacker, Math.max(0.6, attacker.getBbWidth() * 0.8), 0xF0FF2A3A, 12, "hm_listen_warning");
        playTo(caster, SoundEvents.NOTE_BLOCK_BELL, 1.0f, 1.8f);
    }

    // =====================================================================================
    // Key 3: Shadow Binding (Shadow Ascetic)
    // =====================================================================================

    /** The tick the tendrils have closed on the one they bind: root and hurt it then. */
    public static final int BIND_GRIPS = 14;
    private static final String BIND_TAG = "hm_bind";

    /** Four tendrils from a pool of shadow, 1.4 blocks tall and closing round a 1-block middle at scale 1. */
    public static final ModelFx SHADOW_TENDRILS = ModelFx.of("hanged_man/shadow_tendrils").frames(5).smooth()
            .pivot(8, -16, 8).glow();

    /** Where the shadows will rise: a dark warning circle with a crimson ring closing on it. */
    public static void bindingWarning(ServerLevel sl, Vec3 ground, double radius) {
        SpellFx.warningCircle(sl, ground, radius, SHADOW, 12);
        SpellFx.closingRing(sl, ground, radius, 0xC0D0283C, 10, 0);
        play(sl, ground, SoundEvents.WARDEN_DIG, 0.6f, 1.4f);
    }

    /**
     * The shadow of one enemy rises and closes on it (shut at BIND_GRIPS), holds it until 'ticks' and sinks back.
     * Stuck to it, so it goes where the enemy is pushed.
     */
    public static void bind(ServerLevel sl, LivingEntity enemy, int ticks) {
        float scale = (float) Math.max(0.8, enemy.getBbWidth() * 1.3 + 0.2);
        float under = -1.5f * scale;
        int life = Math.max(ticks, BIND_GRIPS + 10);
        ModelFx tendrils = SHADOW_TENDRILS.scale(scale).lifetime(life).fade(0, 6).tag(BIND_TAG)
                .key(0, pose().frame(0).up(under))
                .key(8, pose().up(0f), Ease.OUT)
                .during(6, 10, pose().frame(2), Ease.OUT)
                .during(10, BIND_GRIPS, pose().frame(4), Ease.IN)
                .during(life - 10, life, pose().frame(1).up(under), Ease.IN);
        ParticleShapes.clearModels(sl, enemy, BIND_TAG);
        ParticleShapes.modelOn(sl, tendrils, enemy, new Vec3(0, 0.02, 0), sl.getRandom().nextFloat() * 90f, 0f, 0f);
        ParticleShapes.burst(sl, SHADOW_WISP, enemy.position().add(0, 0.2, 0), 8, 0.02, 0.06);
        Later.run(sl, BIND_GRIPS, () -> {
            if (!enemy.isAlive()) return;
            play(sl, enemy.position(), SoundEvents.CHAIN_PLACE, 1.0f, 0.6f);
            ParticleShapes.burst(sl, SHADOW_WISP, chestOf(enemy), 6, 0.02, 0.05);
        });
    }

    /** The binding broke early. */
    public static void bindEnd(ServerLevel sl, LivingEntity enemy) {
        ParticleShapes.clearModels(sl, enemy, BIND_TAG);
        ParticleShapes.burst(sl, SHADOW_WISP, enemy.position().add(0, 0.3, 0), 10, 0.03, 0.08);
    }

    // ---------- sneak: Shadow Lurking

    private static final String LURK_TAG = "hm_lurk";

    /** The caster sinks into their shadow: a ring closes on them and a pool of shadow stays under their feet. */
    public static void lurkEnter(ServerLevel sl, Player caster, int ticks) {
        SpellFx.closingRing(sl, caster.position(), 1.8, SHADOW, 10, 0);
        ParticleShapes.burst(sl, SHADOW_WISP, chestOf(caster), 16, 0.03, 0.1);
        SpellFx.circleUnder(sl, caster, 0.9, 0xD0080408, ticks, LURK_TAG);
        play(sl, caster.position(), SoundEvents.ENDERMAN_TELEPORT, 0.8f, 0.5f);
    }

    /** Every few ticks while lurking: the pool smokes a little. */
    public static void lurkTick(ServerLevel sl, Player caster) {
        ParticleShapes.burst(sl, SHADOW_WISP, caster.position().add(0, 0.1, 0), 2, 0.005, 0.02);
    }

    /** They rise out of it again. */
    public static void lurkExit(ServerLevel sl, Player caster) {
        ParticleShapes.clearModels(sl, caster, LURK_TAG);
        SpellFx.risingRings(sl, caster.position(), 1.0, 2.0, 3, SHADOW);
        ParticleShapes.burst(sl, SHADOW_WISP, chestOf(caster), 18, 0.04, 0.12);
        play(sl, caster.position(), SoundEvents.ENDERMAN_TELEPORT, 0.8f, 0.7f);
    }

    // =====================================================================================
    // Key 4: Flesh Bomb (Rose Bishop)
    // =====================================================================================

    private static final String FLESH_BOMB_TAG = "hm_flesh_bomb";

    /** The lump of flesh, 0.6 blocks wide at scale 1, in two shapes: resting (0) and swollen (1). */
    public static final ModelFx FLESH_LUMP = ModelFx.of("hanged_man/flesh_lump").frames(2).smooth()
            .pivot(8, 8, 8).glow();

    private static final SlashFx FLESH_TRAIL = SlashFx.line("slash/smooth")
            .color(0xC07A0F1E).core(0xA0D0283C).width(0.16f).taper(SlashFx.Taper.COMET);

    /**
     * The thrown lump, tumbling, with a crimson streak behind it. Give it onHit / onEnd and fire it:
     *   HangedManFx.FLESH_BOMB.onHit((target, at) -> { HangedManFx.fleshBombStick(sl, target, 40); ... }).fire(player, sl, hand, dir);
     */
    public static final Shot FLESH_BOMB = Shot.of(FLESH_LUMP.scale(0.7f).spin(25f))
            .speed(2.2).range(24).width(0.5).tip(0.3)
            .trail(FLESH_TRAIL);

    /** Where FLESH_BOMB leaves from: just off the caster's right hand. */
    public static Vec3 fleshBombHand(Player caster, ServerLevel sl) {
        return Spells.clearStart(caster, sl,
                caster.getEyePosition().add(Spells.rightOf(caster).scale(0.35)).add(0, -0.25, 0));
    }

    /** The lump stuck on the target, swelling and throbbing faster and faster for 'ticks', with a quickening heartbeat. */
    public static void fleshBombStick(ServerLevel sl, LivingEntity target, int ticks) {
        ModelFx lump = FLESH_LUMP.scale(0.7f).lifetime(ticks + 2).tag(FLESH_BOMB_TAG)
                .key(0, pose().scale(0.8f))
                .key(ticks, pose().scale(1.6f), Ease.IN);
        int t = 0;
        int beat = 10;
        boolean swell = true;
        while (t < ticks) {                                   // the throbbing gets faster as it swells
            int end = Math.min(ticks, t + beat);
            lump = lump.during(t, end, pose().frame(swell ? 1 : 0), Ease.IN_OUT);
            if (swell) {
                Later.run(sl, t, () -> {
                    if (target.isAlive()) play(sl, target.position(), SoundEvents.WARDEN_HEARTBEAT, 0.9f, 1.3f);
                });
            }
            swell = !swell;
            t = end;
            beat = Math.max(3, beat - 1);
        }
        ParticleShapes.clearModels(sl, target, FLESH_BOMB_TAG);
        ParticleShapes.modelOn(sl, lump, target, new Vec3(0, target.getBbHeight() * 0.6, -target.getBbWidth() * 0.5), 0f, 0f, 0f);
        ParticleShapes.burst(sl, BLOOD, chestOf(target), 8, 0.03, 0.08);
        play(sl, target.position(), SoundEvents.SLIME_SQUISH, 1.0f, 0.7f);
    }

    /** It bursts: a crimson blast, then a second of blood raining on the area. 'stuckOn' (or null) loses the lump. */
    public static void fleshBombBurst(ServerLevel sl, Vec3 center, double radius, LivingEntity stuckOn) {
        if (stuckOn != null) ParticleShapes.clearModels(sl, stuckOn, FLESH_BOMB_TAG);
        SpellFx.blast(sl, center, radius * 0.7, 0xFFB01828, FLESH_CHUNK);
        SpellFx.shockRing(sl, center.add(0, -0.5, 0), radius, 0xA05A6A20, 12, 2);
        play(sl, center, SoundEvents.SLIME_SQUISH, 1.2f, 0.5f);
        play(sl, center, SoundEvents.GENERIC_EXPLODE, 0.8f, 1.4f);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int wave = 0; wave < 5; wave++) {
            Later.run(sl, 2 + wave * 4, () -> {
                for (int i = 0; i < 14; i++) {
                    double angle = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
                    Vec3 at = center.add(Math.cos(angle) * far, 2.5 + random.nextDouble(), Math.sin(angle) * far);
                    ParticleShapes.spawn(sl, BLOOD, at, new Vec3(0, -0.15, 0));
                    if (i % 3 == 0) ParticleShapes.spawn(sl, CORRODE, at.add(0, -2.6, 0), Vec3.ZERO);
                }
            });
        }
        play(sl, center, SoundEvents.HONEY_BLOCK_BREAK, 1.0f, 0.6f);
    }

    // ---------- sneak: Flesh Cloak

    private static final String CLOAK_TAG = "hm_flesh_cloak";

    /** See-through veined shells round a standing player (2 blocks tall at scale 1). Worn: it turns with the body. */
    public static final ModelFx FLESH_CLOAK = ModelFx.of("hanged_man/flesh_cloak").seeThrough();

    /** The caster's body goes soft: the cloak wraps them and beats gently for 'ticks'. */
    public static void fleshCloak(ServerLevel sl, Player caster, int ticks) {
        float scale = caster.getBbHeight() / 1.9f;
        ModelFx cloak = swinging(FLESH_CLOAK.scale(scale).lifetime(ticks).fade(0, 10).tag(CLOAK_TAG)
                        .key(0, pose().scale(1.25f).alpha(0f))
                        .key(8, pose().scale(1f).alpha(1f), Ease.OUT),
                8, ticks, 10, pose().scale(1.04f), pose().scale(1f));
        ParticleShapes.clearModels(sl, caster, CLOAK_TAG);
        ParticleShapes.modelOnTurning(sl, cloak, caster, Vec3.ZERO, 0f, 0f, 0f);
        ParticleShapes.burst(sl, FLESH_CHUNK, chestOf(caster), 10, 0.04, 0.1);
        play(sl, caster.position(), SoundEvents.SLIME_SQUISH, 1.0f, 0.5f);
        play(sl, caster.position(), SoundEvents.WARDEN_HEARTBEAT, 0.8f, 1.0f);
    }

    /** The cloak swallows a hit. */
    public static void fleshCloakAbsorb(ServerLevel sl, Player caster) {
        ParticleShapes.burst(sl, BLOOD, chestOf(caster), 10, 0.05, 0.15);
        ParticleShapes.burst(sl, FLESH_CHUNK, chestOf(caster), 4, 0.05, 0.12);
        play(sl, caster.position(), SoundEvents.SLIME_SQUISH, 0.9f, 1.1f);
    }

    /** Every half second while it lasts: it mends them. */
    public static void fleshCloakRegen(ServerLevel sl, Player caster) {
        ParticleShapes.burst(sl, FLESH_MOTE, chestOf(caster), 3, 0.01, 0.04);
    }

    /** Taken off (early or at the end). */
    public static void fleshCloakEnd(ServerLevel sl, Player caster) {
        ParticleShapes.clearModels(sl, caster, CLOAK_TAG);
        ParticleShapes.burst(sl, FLESH_CHUNK, chestOf(caster), 8, 0.04, 0.1);
    }

    // =====================================================================================
    // Key 5: Grazing (Shepherd): the stolen souls
    // =====================================================================================

    /** The tick a stolen soul reaches the caster. */
    public static final int STEAL_ARRIVES = 15;
    private static final String GRAVE_SOUL_TAG = "hm_grave_soul";

    /** The wheel's pictures, for the Grazing wheel screen (textures/gui/...). */
    public static final String WHEEL_SOUL = "hanged_man/soul";
    public static final String WHEEL_EMPTY = "hanged_man/slot_empty";
    public static final String WHEEL_SNEAK_BADGE = "hanged_man/sneak_badge";
    public static final String WHEEL_REPLACE = "hanged_man/replace";

    /** A soul: the shared soft orb, tinted. */
    public static final ModelFx SOUL_ORB = ModelFx.of("orb").pivot(8, 8, 8).glow().unshaded().seeThrough();

    /**
     * The soul of something just killed, floating over where it fell for 'ticks', bobbing and turning.
     * 'color' is SOUL for a mob; for a player, the color of their pathway.
     * Returns this soul's own name: keep it with the stolen ability and hand it to soulStolen or graveSoulFades,
     * which take away this soul and no other.
     */
    public static String graveSoul(ServerLevel sl, Vec3 corpse, int color, int ticks) {
        String tag = GRAVE_SOUL_TAG + "_" + UUID.randomUUID();
        ModelFx soul = swinging(SOUL_ORB.color(color).scale(0.6f).lifetime(ticks).fade(8, 60).spin(2f).tag(tag)
                        .key(0, pose().scale(0.2f).alpha(0f))
                        .key(10, pose().scale(1f).alpha(1f), Ease.OUT_BACK),
                10, ticks, 20, pose().up(0.18f), pose().up(-0.05f));
        ParticleShapes.model(sl, soul, corpse.add(0, 1.2, 0), 0f, 0f, 0f);
        ModelFx circle = SpellFx.RUNE_CIRCLE.color(color & 0x80FFFFFF).scale(1.4f).lifetime(ticks).fade(8, 60).spin(1f)
                .tag(tag);
        ParticleShapes.model(sl, circle, corpse.add(0, 0.06, 0), 0f, 0f, 0f);
        ParticleShapes.burst(sl, SOUL_MOTE, corpse.add(0, 1.0, 0), 10, 0.02, 0.06);
        play(sl, corpse, SoundEvents.SOUL_ESCAPE, 1.0f, 1.0f);
        return tag;
    }

    /** Every half second while a soul waits on its corpse: a few motes drift off it. */
    public static void graveSoulTick(ServerLevel sl, Vec3 corpse) {
        ParticleShapes.burst(sl, SOUL_MOTE, corpse.add(0, 1.2, 0), 2, 0.005, 0.02);
    }

    /** Nobody took it: it fades now. 'soul' = what graveSoul returned. */
    public static void graveSoulFades(ServerLevel sl, Vec3 corpse, String soul) {
        ParticleShapes.clearModels(sl, corpse, soul);
        ParticleShapes.burst(sl, SOUL_MOTE, corpse.add(0, 1.2, 0), 8, 0.03, 0.08);
    }

    /** The caster takes the soul: it leaves the corpse and flies into their chest (arrives at STEAL_ARRIVES). */
    public static void soulStolen(ServerLevel sl, Vec3 corpse, String soul, Player caster, int color) {
        graveSoulFades(sl, corpse, soul);
        ModelFx flying = SOUL_ORB.color(color).scale(0.5f).lifetime(STEAL_ARRIVES + 4).fade(0, 4)
                .during(STEAL_ARRIVES - 2, STEAL_ARRIVES + 2, pose().scale(0.2f), Ease.IN);
        SpellFx.homing(sl, flying, corpse.add(0, 1.2, 0), caster, STEAL_ARRIVES);
        Later.run(sl, STEAL_ARRIVES, () -> {
            if (!Spells.casterStillHere(caster, sl)) return;
            SpellFx.risingRings(sl, caster.position(), 1.0, 2.0, 3, color);
            ParticleShapes.burst(sl, SOUL_MOTE, chestOf(caster), 16, 0.03, 0.1);
            play(sl, caster.position(), SoundEvents.SOUL_ESCAPE, 1.0f, 0.6f);
            play(sl, caster.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 0.8f, 1.2f);
        });
    }

    /** Before a stolen ability's own look: a quick soul ring under the caster, so it reads as stolen. */
    public static void stolenCast(ServerLevel sl, Player caster) {
        SpellFx.shockRing(sl, caster.position(), 1.6, SOUL, 6, 0);
        ParticleShapes.burst(sl, SOUL_MOTE, chestOf(caster), 5, 0.02, 0.06);
        play(sl, caster.position(), SoundEvents.SOUL_ESCAPE, 0.5f, 1.4f);
    }

    /** The selected slot holds nothing. */
    public static void emptySlot(ServerLevel sl, Player caster) {
        ParticleShapes.burst(sl, ASH, chestOf(caster), 5, 0.01, 0.04);
        playTo(caster, SoundEvents.SOUL_SAND_BREAK, 1.0f, 0.6f);
    }

    // =====================================================================================
    // Key 6: Cull of Spiritual Flesh (Black Knight)
    // =====================================================================================

    private static final String CULL_TAG = "hm_cull";

    /** The black body of the cut, drawn normally (a dark color would vanish if it were added as light). */
    private static final SlashFx CULL_BLACK = SlashFx.arc("slash/streaks")
            .color(0xE00A0408).core(0xF0000000).translucent()
            .radius(3.0f).arc(150f).width(1.6f)
            .layers(3).spread(0.3f)
            .lifetime(10).sweep(3);
    /** Its crimson edge, a little further out, glowing. */
    private static final SlashFx CULL_EDGE = SlashFx.arc("slash/streaks")
            .color(0xE0D0283C).core(0xFFFF8A90)
            .radius(3.9f).arc(150f).width(0.5f)
            .layers(2).spread(0.15f)
            .lifetime(10).sweep(3);

    /** A wide black arc with a crimson edge sweeps in front of the caster. */
    public static void cull(ServerLevel sl, Player caster) {
        Vec3 center = caster.position().add(0, caster.getBbHeight() * 0.6, 0).add(Spells.flatLook(caster).scale(0.4));
        float yaw = caster.getYRot();
        ParticleShapes.slash(sl, CULL_BLACK, center, yaw, 0f, -8f);
        ParticleShapes.slash(sl, CULL_EDGE.delay(1), center, yaw, 0f, -8f);
        play(sl, caster.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.6f);
        play(sl, caster.position(), SoundEvents.ANVIL_LAND, 0.3f, 0.5f);
    }

    /** One enemy it cut: black dust, shards of its stripped defense, and a dark mark under it while that lasts. */
    public static void cullHit(ServerLevel sl, LivingEntity enemy, int ticks) {
        ParticleShapes.burst(sl, SHADOW_WISP, chestOf(enemy), 8, 0.04, 0.1);
        ParticleShapes.burst(sl, ARMOR_SHARD, chestOf(enemy), 10, 0.08, 0.2);
        SpellFx.circleUnder(sl, enemy, Math.max(0.7, enemy.getBbWidth()), 0xC0400810, ticks, CULL_TAG);
        play(sl, enemy.position(), SoundEvents.SHIELD_BREAK, 0.6f, 0.7f);
    }

    // ---------- sneak: Black Armor

    private static final String ARMOR_TAG = "hm_black_armor";
    /** The tick the plates have locked on. */
    public static final int ARMOR_LOCKS = 12;

    /** Plates round a standing player's body (not the head), in two shapes: flung apart (0), locked on (1). Worn. */
    public static final ModelFx BLACK_ARMOR = ModelFx.of("hanged_man/black_armor").frames(2).smooth().glow();
    /** The helm, built round a head whose top is the pivot: modelOnHead(..., 0, ...) puts it on. */
    public static final ModelFx BLACK_HELM = ModelFx.of("hanged_man/black_armor_helm").glow();

    /**
     * The plates fly in and lock onto the caster, and the helm comes down onto their head, for 'ticks'.
     * 'size' = how big the caster is now (their scale attribute; 1 = normal), so the armor grows with them.
     */
    public static void blackArmor(ServerLevel sl, Player caster, int ticks, float size) {
        int life = Math.max(ticks, ARMOR_LOCKS + 20);
        ModelFx plates = BLACK_ARMOR.scale(size).lifetime(life).fade(3, 0).tag(ARMOR_TAG)
                .key(0, pose().frame(0).alpha(0f))
                .key(ARMOR_LOCKS, pose().frame(1).alpha(1f), Ease.IN)
                .during(life - 10, life, pose().frame(0).alpha(0f), Ease.OUT);
        ModelFx helm = BLACK_HELM.scale(size).lifetime(life).tag(ARMOR_TAG)
                .key(0, pose().up(1.4f).alpha(0f))
                .key(ARMOR_LOCKS, pose().up(0f).alpha(1f), Ease.IN)
                .during(life - 10, life, pose().up(1.0f).alpha(0f), Ease.OUT);
        ParticleShapes.clearModels(sl, caster, ARMOR_TAG);
        ParticleShapes.modelOnTurning(sl, plates, caster, Vec3.ZERO, 0f, 0f, 0f);
        ParticleShapes.modelOnHead(sl, helm, caster, 0, 0f, 0f, 0f);
        ParticleShapes.burst(sl, SHADOW_WISP, chestOf(caster), 20, 0.05, 0.15);
        play(sl, caster.position(), SoundEvents.ARMOR_EQUIP_NETHERITE, 1.0f, 0.6f);
        Later.run(sl, ARMOR_LOCKS, () -> {
            if (!Spells.casterStillHere(caster, sl)) return;
            play(sl, caster.position(), SoundEvents.ANVIL_PLACE, 0.6f, 0.5f);
            SpellFx.shockRing(sl, caster.position(), 2.2, 0xC0D0283C, 8, 0);
        });
    }

    /** A blow glances off the plates. */
    public static void blackArmorHit(ServerLevel sl, Player caster) {
        ParticleShapes.burst(sl, STEEL_SPARK, chestOf(caster), 8, 0.08, 0.2);
        play(sl, caster.position(), SoundEvents.ANVIL_PLACE, 0.3f, 1.6f);
    }

    /** The armor is gone (early, or at the end if it should leave faster than its fade). */
    public static void blackArmorEnd(ServerLevel sl, Player caster) {
        ParticleShapes.clearModels(sl, caster, ARMOR_TAG);
        ParticleShapes.burst(sl, SHADOW_WISP, chestOf(caster), 14, 0.04, 0.12);
        ParticleShapes.burst(sl, ARMOR_SHARD, chestOf(caster), 8, 0.06, 0.15);
    }

    // =====================================================================================
    // Key 7: Degeneration (Trinity Templar)
    // =====================================================================================

    /** The tick the corruption reaches its target. */
    public static final int DEGENERATION_ARRIVES = 10;
    private static final String DEGENERATION_TAG = "hm_degeneration";

    private static final SlashFx BETRAYAL_LINE = SlashFx.line("slash/smooth")
            .color(0xC0D0283C).core(0x80FF8A90).width(0.08f).taper(SlashFx.Taper.UNIFORM);

    /** A crimson orb with a rotten core flies from the caster's hand into the target. */
    public static void degeneration(ServerLevel sl, Player caster, LivingEntity target) {
        Vec3 hand = SpellFx.handOf(caster, sl);
        if (hand == null) return;
        ModelFx outer = SOUL_ORB.color(0xF0B01828).scale(0.55f).lifetime(DEGENERATION_ARRIVES + 4).fade(0, 4);
        ModelFx inner = SOUL_ORB.color(0xFF3A4A1E).scale(0.3f).lifetime(DEGENERATION_ARRIVES + 4).fade(0, 4).noAura();
        SpellFx.homing(sl, outer, hand, target, DEGENERATION_ARRIVES);
        SpellFx.homing(sl, inner, hand, target, DEGENERATION_ARRIVES);
        play(sl, caster.position(), SoundEvents.WITHER_SHOOT, 0.4f, 1.6f);
        Later.run(sl, DEGENERATION_ARRIVES, () -> {
            if (!target.isAlive()) return;
            ParticleShapes.burst(sl, CORRODE, chestOf(target), 14, 0.03, 0.1);
            play(sl, target.position(), SoundEvents.WITHER_AMBIENT, 0.7f, 1.5f);
        });
    }

    /** The rot under the target for as long as it corrodes. */
    public static void degenerationMark(ServerLevel sl, LivingEntity target, int ticks) {
        SpellFx.circleUnder(sl, target, Math.max(0.8, target.getBbWidth()), 0xC03A4A1E, ticks, DEGENERATION_TAG);
    }

    /** Each buff it eats pops off as a puff in that effect's own color. */
    public static void degenerationStrip(ServerLevel sl, LivingEntity target, List<MobEffectInstance> removed) {
        int i = 0;
        for (MobEffectInstance effect : removed) {
            int rgb = effect.getEffect().value().getColor() & 0xFFFFFF;
            ParticleFx puff = ParticleFx.of("fx/glow").color(0xF0000000 | rgb).endColor(rgb)
                    .size(0.12f).endSize(0.02f).lifetime(18, 6).gravity(-0.03f).friction(0.9f).glow();
            Vec3 at = chestOf(target).add(0, 0.2 * i++, 0);
            Later.run(sl, i * 2, () -> {
                ParticleShapes.burst(sl, puff, at, 10, 0.04, 0.1);
                play(sl, at, SoundEvents.FIRE_EXTINGUISH, 0.4f, 1.6f);
            });
        }
    }

    /** It turns on one of its own: a crimson line from it to the one it attacks, for 'ticks'. */
    public static void degenerationBetray(ServerLevel sl, LivingEntity target, LivingEntity ally, int ticks) {
        SpellFx.tether(sl, BETRAYAL_LINE, target, ally, ticks);
        play(sl, target.position(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 0.5f, 1.6f);
    }

    /** Every half second while it corrodes: ash falls off it. */
    public static void degenerationTick(ServerLevel sl, LivingEntity target) {
        ParticleShapes.burst(sl, ASH, chestOf(target), 3, 0.01, 0.03);
        ParticleShapes.burst(sl, CORRODE, target.position().add(0, 0.2, 0), 2, 0.01, 0.02);
    }

    // ---------- sneak: Trinity Body

    private static final String TRINITY_TAG = "hm_trinity";
    /** How far each extra body stands to the side, in blocks. */
    public static final double TRINITY_SIDE = 0.95;

    /** The historical projection (Attendant of Mysteries), tinted dark crimson: the two extra bodies. */
    public static final ModelFx TRINITY_BODY = ModelFx.of("attendant_of_mysteries/projection")
            .pivot(8, -6, 8).color(0xB0401020).glow().seeThrough();
    /** A head of shadow, 0.45 blocks wide, face = front. Pivot = the bottom of its neck. */
    public static final ModelFx SHADOW_HEAD = ModelFx.of("hanged_man/shadow_head").glow();

    /** Two shadow heads grow from the caster's shoulders and two dark copies step out to their sides, for 'ticks'. */
    public static void trinity(ServerLevel sl, Player caster, int ticks) {
        ParticleShapes.clearModels(sl, caster, TRINITY_TAG);
        float body = caster.getBbHeight() / 2.35f;
        for (int side = -1; side <= 1; side += 2) {
            float right = (float) (TRINITY_SIDE * side);
            ModelFx copy = swinging(TRINITY_BODY.scale(body).lifetime(ticks).fade(0, 10).tag(TRINITY_TAG)
                            .key(0, pose().alpha(0f))
                            .key(10, pose().right(right).alpha(0.85f), Ease.OUT_BACK),
                    10, ticks, 7 + side, pose().alpha(0.55f), pose().alpha(0.85f));      // they flicker, out of step
            ParticleShapes.modelOnTurning(sl, copy, caster, new Vec3(0, 0.02, 0), 0f, 0f, 0f);

            ModelFx head = SHADOW_HEAD.scale(0.8f).lifetime(ticks).fade(0, 8).tag(TRINITY_TAG)
                    .key(0, pose().right(0.2f * side).up(-0.55f).scale(0f))
                    .key(8, pose().right(0.42f * side).up(-0.4f).scale(1f).roll(14f * side), Ease.OUT_BACK);
            ParticleShapes.modelOnHead(sl, head, caster, 0, 0f, 0f, 0f);
        }
        SpellFx.risingRings(sl, caster.position(), 1.4, 2.2, 3, 0xC0400818);
        ParticleShapes.burst(sl, SHADOW_WISP, chestOf(caster), 16, 0.04, 0.12);
        play(sl, caster.position(), SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.0f, 0.8f);
    }

    /** The copies repeat a spell: a flash where each one stands. */
    public static void trinityEcho(ServerLevel sl, Player caster) {
        Vec3 right = Spells.rightOf(caster).scale(TRINITY_SIDE);
        for (Vec3 at : new Vec3[]{caster.position().add(right), caster.position().subtract(right)}) {
            SpellFx.shockRing(sl, at, 1.2, 0xC0D0283C, 6, 0);
            ParticleShapes.burst(sl, SHADOW_WISP, at.add(0, 1.0, 0), 5, 0.02, 0.06);
        }
        play(sl, caster.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8f, 1.0f);
    }

    /** The extra bodies and heads are gone. */
    public static void trinityEnd(ServerLevel sl, Player caster) {
        ParticleShapes.clearModels(sl, caster, TRINITY_TAG);
        Vec3 right = Spells.rightOf(caster).scale(TRINITY_SIDE);
        for (Vec3 at : new Vec3[]{caster.position().add(right), caster.position().subtract(right)}) {
            ParticleShapes.burst(sl, SHADOW_WISP, at.add(0, 1.0, 0), 14, 0.03, 0.1);
        }
        play(sl, caster.position(), SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8f, 0.6f);
    }

    // =====================================================================================
    // The display cases (/hangedfx)
    // =====================================================================================

    private interface Show {
        /** 'target' = the nearest mob, or the viewer when there is none. */
        void play(ServerLevel sl, Player viewer, LivingEntity target, Vec3 spot);
    }

    /** onViewer = it is shown on the viewer (worn), not on a spot or a target. */
    private record Case(String name, int ticks, boolean onViewer, Show show) {}

    private static final List<Case> CASES = List.of(
            new Case("divination", DIVINATION_TICKS + 10, true, (sl, v, t, spot) -> {
                divination(sl, v);
                Later.run(sl, DIVINATION_PULSE, () -> { if (t != v) divinationMark(sl, t, 100); });
            }),
            new Case("ritual", 120, false, (sl, v, t, spot) -> {
                ritualCircle(sl, spot, 4, 120);
                for (int i = 0; i < 3; i++) Later.run(sl, 20 + i * 40, () -> ritualPulse(sl, spot, 4));
            }),
            new Case("whispers", 100, false, (sl, v, t, spot) -> whispers(sl, v, t, 100)),
            new Case("listen", 80, true, (sl, v, t, spot) -> {
                for (int i = 0; i < 3; i++) Later.run(sl, i * 30, () -> listenPulse(sl, v, 16));
                if (t != v) Later.run(sl, 40, () -> listenWarning(sl, t, v));
            }),
            new Case("binding", 60, false, (sl, v, t, spot) -> {
                bindingWarning(sl, t.position(), 2.5);
                Later.run(sl, 10, () -> bind(sl, t, 60));
            }),
            new Case("lurking", 80, true, (sl, v, t, spot) -> {
                lurkEnter(sl, v, 80);
                for (int i = 1; i < 16; i++) Later.run(sl, i * 5, () -> lurkTick(sl, v));
                Later.run(sl, 80, () -> lurkExit(sl, v));
            }),
            new Case("flesh_bomb", 60, false, (sl, v, t, spot) -> {
                if (t == v) {
                    fleshBombBurst(sl, spot.add(0, 1, 0), 4, null);
                    return;
                }
                fleshBombStick(sl, t, 40);
                Later.run(sl, 40, () -> fleshBombBurst(sl, chestOf(t), 4, t));
            }),
            new Case("flesh_cloak", 120, true, (sl, v, t, spot) -> {
                fleshCloak(sl, v, 120);
                for (int i = 1; i < 12; i++) Later.run(sl, i * 10, () -> fleshCloakRegen(sl, v));
                Later.run(sl, 40, () -> fleshCloakAbsorb(sl, v));
            }),
            new Case("grazing", 90, false, (sl, v, t, spot) -> {
                String soul = graveSoul(sl, spot, SOUL, 60);
                for (int i = 1; i < 6; i++) Later.run(sl, i * 10, () -> graveSoulTick(sl, spot));
                Later.run(sl, 60, () -> soulStolen(sl, spot, soul, v, SOUL));
                Later.run(sl, 60 + STEAL_ARRIVES + 10, () -> stolenCast(sl, v));
            }),
            new Case("cull", 40, true, (sl, v, t, spot) -> {
                cull(sl, v);
                if (t != v) Later.run(sl, 3, () -> cullHit(sl, t, 60));
            }),
            new Case("black_armor", 140, true, (sl, v, t, spot) -> {
                blackArmor(sl, v, 140, 1f);
                Later.run(sl, 50, () -> blackArmorHit(sl, v));
            }),
            new Case("degeneration", 100, false, (sl, v, t, spot) -> {
                degeneration(sl, v, t);
                Later.run(sl, DEGENERATION_ARRIVES, () -> {
                    degenerationMark(sl, t, 90);
                    degenerationStrip(sl, t, List.of(
                            new MobEffectInstance(MobEffects.SPEED, 20),
                            new MobEffectInstance(MobEffects.STRENGTH, 20),
                            new MobEffectInstance(MobEffects.REGENERATION, 20)));
                    for (int i = 1; i < 9; i++) Later.run(sl, i * 10, () -> degenerationTick(sl, t));
                });
            }),
            new Case("trinity", 140, true, (sl, v, t, spot) -> {
                trinity(sl, v, 140);
                Later.run(sl, 50, () -> trinityEcho(sl, v));
                Later.run(sl, 140, () -> trinityEnd(sl, v));
            }));

    /** The names /hangedfx knows. */
    public static List<String> names() {
        List<String> names = new ArrayList<>();
        for (Case c : CASES) names.add(c.name());
        return names;
    }

    /** The nearest living thing within 12 blocks that is not the viewer, or the viewer. */
    private static LivingEntity nearestTarget(ServerLevel sl, Player viewer) {
        LivingEntity best = viewer;
        double bestDistance = 12 * 12;
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, viewer.getBoundingBox().inflate(12))) {
            if (e == viewer || !e.isAlive()) continue;
            double d = e.distanceToSqr(viewer);
            if (d < bestDistance) {
                bestDistance = d;
                best = e;
            }
        }
        return best;
    }

    /** One display, 5 blocks in front of the viewer. False if there is none of that name. */
    public static boolean show(ServerLevel sl, Player viewer, String name) {
        for (Case c : CASES) {
            if (!c.name().equals(name)) continue;
            Vec3 spot = viewer.position().add(ahead(viewer.getYRot()).scale(5));
            c.show().play(sl, viewer, nearestTarget(sl, viewer), spot);
            return true;
        }
        return false;
    }

    /** Every display one after the other (those on a spot in a ring 8 blocks out), 'rounds' times over. */
    public static void gallery(ServerLevel sl, Player viewer, int rounds) {
        List<Case> placed = new ArrayList<>();
        for (Case c : CASES) if (!c.onViewer()) placed.add(c);
        int at = 0;
        for (int round = 0; round < rounds; round++) {
            for (Case c : CASES) {
                int place = placed.indexOf(c);
                Later.run(sl, at, () -> {
                    if (!viewer.isAlive() || viewer.level() != sl) return;
                    float out = viewer.getYRot() + Math.max(0, place) * 360f / Math.max(1, placed.size());
                    Vec3 spot = viewer.position().add(ahead(out).scale(8));
                    c.show().play(sl, viewer, nearestTarget(sl, viewer), spot);
                });
                at += c.ticks() + 10;
            }
        }
    }

    /** One block the way a yaw looks, level with the ground. */
    private static Vec3 ahead(float yaw) {
        double angle = Math.toRadians(yaw);
        return new Vec3(-Math.sin(angle), 0, Math.cos(angle));
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("hangedfx")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("all")
                        .executes(ctx -> showAll(ctx.getSource(), 1))
                        .then(Commands.argument("rounds", IntegerArgumentType.integer(1, 10))
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
        gallery(source.getLevel(), source.getPlayerOrException(), rounds);
        return 1;
    }
}
