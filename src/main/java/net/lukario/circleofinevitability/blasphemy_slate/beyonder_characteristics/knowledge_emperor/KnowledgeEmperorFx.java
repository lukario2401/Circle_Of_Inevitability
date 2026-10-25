package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.lukario.circleofinevitability.combat.HudTimer;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.SpellFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.particles.fx.ModelFx;
import net.lukario.circleofinevitability.particles.fx.ModelFx.Ease;
import net.lukario.circleofinevitability.particles.fx.ParticleFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.particles.fx.SlashFx;
import net.lukario.circleofinevitability.screen.ScreenAnchor;
import net.lukario.circleofinevitability.screen.ScreenImages;
import net.lukario.circleofinevitability.sounds.ModSounds;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * How the Knowledge Emperor (Hermit) pathway looks and sounds: every key, both casts, every wheel entry, the growth at
 * higher Sequences (Eyes of Secret Inquiry, the Knowledge Orbs, three lances), the shared Study eye and scroll pouch,
 * the passives that show and the melee combo. Visual only: nothing here hurts, holds, hides, heals or moves anyone.
 * The ability code does that, at the ticks named by the constants below (..._LANDS, ..._OPENS, ..._RISEN...), with
 * Later.run. See docs/KNOWLEDGE_EMPEROR.md.
 *
 * ---------- See them in game ----------
 *   /knowledgefx all              every display one after another (/knowledgefx all 3 = three times over)
 *   /knowledgefx star_fall        just that one (press TAB after /knowledgefx for the names)
 * Displays that need a target use the nearest mob within 12 blocks, or you when there is none.
 *
 * ---------- Use them in an ability ----------
 *   Study   studyEye(viewer, target, percent, ticks) shows the eye only the Hermit sees (shut, half, wide);
 *           studyGain(viewer, target), understood(viewer, target), studyEyeHide(viewer, target)
 *   Key 1   pry(sl, player, ticks), pryRead(viewer, target) on the one looked at, pryGlimpse(sl, hidden) on each
 *           hidden thing; Eyes of Secret Inquiry (Sequence 5): pry(...) with secretInquiry = true
 *           sneak: dowse(sl, player, target, ticks, tag) (target null = it hangs still), dowseEnd(sl, player, tag)
 *   Key 2   weakPointStrike(sl, player, from, target) (lands at STRIKE_LANDS)
 *           sneak: sidestep(sl, player, ticks, tag), sidestepSlip(sl, player, from, to), sidestepEnd
 *   Key 3   hex(sl, player, target, ticks, tag) (lands at HEX_LANDS), hexJump(sl, from, to, ticks, tag), hexEnd
 *           sneak: spellbolts(sl, player, targets, element) (bolt i lands at BOLT_FLIGHT + i * BOLT_STEP),
 *               boltHit(sl, target, element); with nothing in sight BOLT(element) for a Shot and boltTrail/boltBurst;
 *               Sage: knowledgeOrbs(sl, player, targets) (orb i lands at ORB_FLIGHT + i * BOLT_STEP), orbHit
 *   Key 4   pouch(player, scrolls) whenever the pouch changes, pouchHide(player); scrollReady(sl, player, scroll)
 *           readScroll(sl, player, scroll) (the spell leaves at READ_FIRES), freezingRay(sl, target, ticks, tag)
 *               (it falls at RAY_FALLS), paralysisRay(sl, player, target, ticks, tag) (it hits at RAY_HITS)
 *           sneak: inscribe(sl, player) (written at INSCRIBED), scrollDropped(sl, player)
 *   Key 5   the Stars wheel's pictures: WHEEL_STARLIGHT_CAGE, WHEEL_STELLAR_SELF, WHEEL_STAR_FALL, WHEEL_STAR_GUARD
 *           starlightCage(sl, player, target) (lands at CAGE_LANDS), then caged(sl, each, ticks, tag), cageCrack,
 *               cageBreak(sl, each, tag), cageEnd(sl, each, tag)
 *           stellarSelf(sl, player, from, to) (re-formed at STELLAR_ARRIVES), stellarCarry(sl, ally, from, to)
 *           starFallSky(sl, center, ticks, tag), starFall(sl, ground) (it lands at STAR_LANDS: its own warning ring
 *               shows the second before), starFallEnd(sl, center, tag)
 *           starGuard(sl, player, ticks, tag), then starGuardTick(sl, player, tag, age) EVERY tick (it moves the
 *               four stars round), starGuardBlock(sl, player, tag, index, at), starGuardBack(sl, player, tag, index,
 *               ticksLeft), starGuardStrike(sl, player, target), starGuardEnd(sl, player, tag)
 *   Key 6   the wheel's pictures: WHEEL_SLEEPING_BEAUTY ... WHEEL_PROPHECY_OF_RUIN
 *           every Retelling: tale(sl, player, Tale.X, strength) (strength = Obscurity's multiplier, 0.4 to 1)
 *           sleepingBeauty(sl, player, center, radius) (sleep at SPINDLE_SLEEPS), asleep(sl, sleeper, ticks, tag),
 *               awaken(sl, sleeper, tag)
 *           avalon(sl, center, radius, ticks, tag) (risen at AVALON_RISEN), avalonTick, avalonBlessed, avalonEnd
 *           achillesHeel(sl, player, target, ticks, tag) (the arrow lands at ARROW_LANDS), achillesEnd
 *           pandorasBox(sl, player, radius) (open at BOX_OPENS, hope at HOPE_RISES), misfortune(sl, enemy, Misfortune.X,
 *               ticks, tag), hope(sl, ally)
 *           foresight(sl, player, ticks, tag), foresightGhost(sl, attacker, at), foresightDodge(sl, player, from, to),
 *               foresightEnd
 *           prophecy(sl, player, target, seconds, tag), prophecyTick(sl, target, secondsLeft) every second,
 *               prophecyFulfilled(sl, target, tag), prophecyFails(sl, target, tag), prophecyRebound(sl, player)
 *   Key 7   the wheel's pictures: WHEEL_INFORMATIZATION ... WHEEL_DIVINE_KINGDOM
 *           informatization(sl, player, ticks, tag), informatizationTick(sl, player), decoy(sl, at, yaw, ticks, tag),
 *               decoyBurst(sl, at, tag), informatizationEnd(sl, player, tag, released)
 *           erase(sl, player, target) (strikes at ERASE_LANDS), deleted(sl, thing)
 *           knowledgeOverload(sl, player, target, ticks, tag) (it reaches the mind at OVERLOAD_REACHES),
 *               overloadHit(sl, target, tag), mindBurst(sl, target, radius); Hermit: overloadAll(sl, player, targets,
 *               ticks, tag)
 *           lanceForm(sl, player, count) (thrown at LANCE_THROWN), LANCE for a Shot, lanceTrail(sl, at, direction),
 *               lanceImpact(sl, at, radius)
 *           condenseKnowledge(sl, player, sources) (gathered at CONDENSED), condenseInverse(sl, player, ticks, tag)
 *           embodiment(sl, player), embodimentEnd(sl, player)
 *           indoctrination(sl, player, radius), enlightened(sl, ally, ticks, tag), indoctrinated(sl, enemy, ticks, tag)
 *           divineKingdom(sl, player, radius, ticks, tag) (risen at KINGDOM_RISEN), kingdomTick(sl, center, radius),
 *               kingdomForcedOut(sl, hidden), kingdomGhost(sl, at), kingdomEnd(sl, center, tag)
 *   Passives: intuition(hunted), intuitionOutline(viewer, hidden), whisper(player), clairaudience(viewer, at),
 *             informationSkin(sl, player), starlight(sl, player), lawOfThree(sl, player)
 *   Melee: combo(sl, player, target, step) for hits 0, 1 and 2
 *   Every timed cast: status(player, Status.X, ticks) shows its picture and its clock; statusEnd(player, Status.X).
 *
 * The models are in models/model_fx/knowledge_emperor/ (made by tools/model_gen/knowledge_emperor/models.py; each
 * JSON's "credit" gives its size and pivot), the pictures in textures/gui/knowledge_emperor/ and
 * textures/gui/ability_menus/knowledge_emperor/ (icons.py), the particles in textures/particle/fx/ (particles.py), the
 * sounds in sounds/knowledge_emperor/ (sounds.py) and sounds/ModSounds (KNOWLEDGE_...).
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class KnowledgeEmperorFx {

    private KnowledgeEmperorFx() {}

    private static ModelFx.Pose pose() {
        return ModelFx.pose();
    }

    // =====================================================================================
    // Colors (ARGB)
    // =====================================================================================

    /** The Pathway's own color: orchid purple. */
    public static final int PATHWAY_COLOR = 0xFFC070E8;
    public static final int ORCHID = 0xFFC070E8;
    public static final int ORCHID_LIGHT = 0xFFE8C4FF;
    public static final int STAR_WHITE = 0xFFF6F4FF;
    public static final int INK = 0xFF0E0818;
    public static final int INDIGO = 0xFF2A1A5A;
    public static final int PALE_GOLD = 0xFFF0DC96;
    public static final int HEX_VIOLET = 0xFFAA5ADC;
    public static final int AMBER = 0xFFF0962A;
    public static final int MIST_GREEN = 0xFFCCF0C0;
    public static final int RUIN_RED = 0xFFE04050;
    public static final int ICE = 0xFFBEECFF;
    public static final int PARALYSIS_GREEN = 0xFFAAFFAA;

    /** Spellbolts' three elements, each with its color and sound. */
    public enum Element {
        FROST(0xFF9AD8FF),
        LIGHTNING(0xFFC8B0FF),
        FIRE(0xFFFF8A2A);

        public final int color;

        Element(int color) {
            this.color = color;
        }

        RegistryObject<SoundEvent> sound() {
            return switch (this) {
                case FROST -> ModSounds.KNOWLEDGE_BOLT_FROST;
                case LIGHTNING -> ModSounds.KNOWLEDGE_BOLT_LIGHTNING;
                case FIRE -> ModSounds.KNOWLEDGE_BOLT_FIRE;
            };
        }
    }

    /** The scrolls the pouch can hold, each with its picture (textures/gui/knowledge_emperor/). */
    public enum Scroll {
        FREEZING("scroll_freezing", 0xFFBEECFF),
        PARALYSIS("scroll_paralysis", 0xFFAAFFAA),
        INSCRIBED("scroll_inscribed", 0xFFF0DC96);

        public final String image;
        public final int color;

        Scroll(String image, int color) {
            this.image = image;
            this.color = color;
        }
    }

    /** The Mysticologist's tales (the four that Obscurity weakens), each with its color. */
    public enum Tale {
        SLEEPING_BEAUTY(0xFFFFD86A),
        AVALON(0xFFCCF0C0),
        ACHILLES_HEEL(0xFFF0DC96),
        PANDORAS_BOX(0xFFC070E8);

        public final int color;

        Tale(int color) {
            this.color = color;
        }
    }

    /** Pandora's Box: the six misfortunes, each with its color. */
    public enum Misfortune {
        BLINDNESS(0xFF20202A),
        SLOWNESS(0xFF7A8AB0),
        WEAKNESS(0xFF8A6A8A),
        POISON(0xFF5AB040),
        NAUSEA(0xFF9AB060),
        NO_HEALING(0xFFB04050);

        public final int color;

        Misfortune(int color) {
            this.color = color;
        }
    }

    // =====================================================================================
    // The small particles everything shares
    // =====================================================================================

    private static final ParticleFx STAR_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFF6F4FF).endColor(0x00C070E8)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(22, 10).gravity(-0.015f).friction(0.92f).glow();
    private static final ParticleFx ORCHID_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFE8C4FF).endColor(0x006A2A9A)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(24, 12).gravity(-0.02f).friction(0.92f).glow();
    private static final ParticleFx STAR_SPARK = ParticleFx.of("fx/star5")
            .color(0xFFFFFFFF).endColor(0x00C070E8)
            .size(0.08f).endSize(0.03f).sizeRandom(0.5f)
            .lifetime(18, 10).gravity(0.01f).friction(0.9f).glow().spin(8f).randomRotation();
    private static final ParticleFx SPARKLE = ParticleFx.of("fx/sparkle")
            .color(0xFFFFFFFF).endColor(0x00E8C4FF)
            .size(0.07f).endSize(0.02f).sizeRandom(0.5f)
            .lifetime(16, 8).gravity(-0.005f).friction(0.9f).glow().randomRotation();
    private static final ParticleFx[] GLYPHS = {
            glyph("fx/glyph_0"), glyph("fx/glyph_1"), glyph("fx/glyph_2"), glyph("fx/glyph_3")};
    private static final ParticleFx INK_DROP = ParticleFx.of("fx/ink")
            .color(0xFF1A0E2A).endColor(0xA0100818)
            .size(0.05f).endSize(0.04f).sizeRandom(0.4f)
            .lifetime(20, 8).gravity(0.06f).friction(0.98f).collide();
    private static final ParticleFx PAGE = ParticleFx.of("fx/page")
            .color(0xFFF2E4BC).endColor(0x00CDB27C)
            .size(0.1f).endSize(0.08f).sizeRandom(0.3f)
            .lifetime(36, 14).gravity(0.008f).friction(0.94f).spin(10f).randomRotation();
    private static final ParticleFx BURNING_PAGE = PAGE.color(0xFFFFE0A0).endColor(0x00602010).glow().gravity(-0.02f);
    private static final ParticleFx QUILL = ParticleFx.of("fx/quill")
            .color(0xFFF6F4FF).endColor(0x00C070E8)
            .size(0.14f).endSize(0.14f)
            .lifetime(30, 0).gravity(0f).friction(0.9f).glow();
    private static final ParticleFx TEXT = ParticleFx.of("fx/text_line")
            .color(0xFFE8C4FF).endColor(0x002A1A5A)
            .size(0.09f).endSize(0.06f).sizeRandom(0.3f)
            .lifetime(22, 10).gravity(-0.01f).friction(0.9f).glow();
    private static final ParticleFx INK_SMOKE = ParticleFx.of("fx/smoke")
            .color(0xB01A0E2A).endColor(0x00080410)
            .size(0.3f).endSize(0.8f).sizeRandom(0.3f)
            .lifetime(28, 12).gravity(-0.01f).friction(0.9f).spin(2f).randomRotation();
    private static final ParticleFx MIST = ParticleFx.of("fx/smoke")
            .color(0x70E0F4E0).endColor(0x00A0C0A0)
            .size(0.5f).endSize(1.3f).sizeRandom(0.4f)
            .lifetime(50, 20).gravity(-0.002f).friction(0.95f).spin(1f).randomRotation();
    private static final ParticleFx PETAL = ParticleFx.of("fx/petal")
            .color(0xFFFFE8F0).endColor(0x00FFB0C8)
            .size(0.07f).endSize(0.06f).sizeRandom(0.4f)
            .lifetime(50, 20).gravity(0.01f).friction(0.96f).spin(6f).randomRotation();
    private static final ParticleFx GOLD_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFFFF0B0).endColor(0x00C09040)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(24, 12).gravity(-0.02f).friction(0.92f).glow();
    private static final ParticleFx AMBER_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFFFD27A).endColor(0x00B85A10)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(20, 10).gravity(0.01f).friction(0.92f).glow();
    private static final ParticleFx AMBER_SHARD = ParticleFx.of("fx/splinter")
            .color(0xE0FFC060).endColor(0x00B85A10)
            .size(0.08f).endSize(0.05f).sizeRandom(0.5f)
            .lifetime(24, 10).gravity(0.05f).friction(0.95f).glow().spin(12f).randomRotation().collide();
    private static final ParticleFx FROST = ParticleFx.of("fx/flake")
            .color(0xFFE8F8FF).endColor(0x0060B0E0)
            .size(0.08f).endSize(0.05f).sizeRandom(0.5f)
            .lifetime(26, 10).gravity(0.012f).friction(0.94f).glow().spin(6f).randomRotation();
    private static final ParticleFx SPARK = ParticleFx.of("fx/spark")
            .color(0xFFFFFFFF).endColor(0x00C8B0FF)
            .size(0.06f).endSize(0.02f)
            .lifetime(8, 6).gravity(0.02f).friction(0.86f).glow();
    private static final ParticleFx GREEN_SPARK = SPARK.color(0xFFE0FFE0).endColor(0x0040C060);
    private static final ParticleFx SLEEP_Z = ParticleFx.of("fx/sleep_z")
            .color(0xFFFFE8A0).endColor(0x00FFC060)
            .size(0.12f).endSize(0.16f).sizeRandom(0.2f)
            .lifetime(34, 10).gravity(-0.03f).friction(0.94f).glow();
    private static final ParticleFx HEX_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFD08AFF).endColor(0x00400860)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(22, 10).gravity(0.01f).friction(0.9f).glow();
    private static final ParticleFx RUIN_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFFF8A90).endColor(0x00600810)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(20, 10).gravity(-0.02f).friction(0.9f).glow();
    private static final ParticleFx EYE_MARK = ParticleFx.of("fx/eye")
            .color(0xFFE8C4FF).endColor(0x00C070E8)
            .size(0.12f).endSize(0.1f)
            .lifetime(30, 0).gravity(0f).friction(0f).glow();

    private static ParticleFx glyph(String texture) {
        return ParticleFx.of(texture)
                .color(0xFFF2E8FF).endColor(0x00603090)
                .size(0.08f).endSize(0.06f).sizeRandom(0.3f)
                .lifetime(26, 10).gravity(-0.01f).friction(0.9f).glow();
    }

    private static ParticleFx anyGlyph() {
        return GLYPHS[ThreadLocalRandom.current().nextInt(GLYPHS.length)];
    }

    /** A few random glyphs bursting out of a point. */
    private static void glyphBurst(ServerLevel sl, Vec3 at, int count, double minSpeed, double maxSpeed) {
        for (int i = 0; i < count; i++) ParticleShapes.burst(sl, anyGlyph(), at, 1, minSpeed, maxSpeed);
    }

    // =====================================================================================
    // Lines and arcs
    // =====================================================================================

    private static final SlashFx THREAD = SlashFx.line("slash/smooth")
            .color(0xC0F6F4FF).core(0xFFFFFFFF).width(0.05f).taper(SlashFx.Taper.UNIFORM);
    private static final SlashFx HEX_LINE = SlashFx.line("slash/dashed_fine")
            .color(0xE0AA5ADC).core(0xFFE8C4FF).width(0.12f).taper(SlashFx.Taper.COMET).lifetime(10).sweep(3);
    private static final SlashFx RAY_LINE = SlashFx.line("slash/smooth")
            .color(0xE0AAFFAA).core(0xFFFFFFFF).width(0.22f).taper(SlashFx.Taper.COMET).lifetime(8).sweep(3);
    private static final SlashFx STRIKE_ARC = SlashFx.arc("slash/streaks")
            .color(0xD0E8C4FF).headColor(0xF2F0E6FF).core(0xFFFFFFFF)
            .radius(1.4f).arc(110f).width(0.45f).layers(2).spread(0.12f).lifetime(7).sweep(2);
    private static final SlashFx DASH = SlashFx.line("slash/streaks")
            .color(0x90C070E8).core(0x80FFFFFF).width(0.3f).taper(SlashFx.Taper.COMET);
    private static final SlashFx STAR_TRAIL = SlashFx.line("slash/smooth")
            .color(0xC0F6F4FF).core(0xFFFFFFFF).width(0.12f).taper(SlashFx.Taper.COMET).lifetime(10).sweep(2);
    private static final SlashFx RUIN_LINE = SlashFx.line("slash/dashed")
            .color(0xC0E04050).core(0x80FFD0D0).width(0.08f).taper(SlashFx.Taper.UNIFORM);
    private static final SlashFx GHOST_ARC = SlashFx.arc("slash/smooth")
            .color(0x60E8E8FF).core(0x80FFFFFF)
            .radius(1.2f).arc(120f).width(0.35f).lifetime(18).sweep(4).translucent();
    private static final SlashFx ERASE_LINE = SlashFx.line("slash/edge")
            .color(0xF0FFFFFF).core(0xFFFFFFFF).width(0.25f).taper(SlashFx.Taper.CRESCENT).lifetime(8).sweep(2);

    // =====================================================================================
    // Sounds
    // =====================================================================================

    /** One of the pathway's own sounds (sounds/ModSounds), heard by everyone near. */
    private static void sound(ServerLevel sl, Vec3 at, RegistryObject<SoundEvent> sound, float volume, float pitch) {
        Spells.sound(sl, at, sound.get(), volume, pitch);
    }

    /** A sound only this player hears, coming from 'at'. */
    private static void soundFor(ServerPlayer player, Vec3 at, RegistryObject<SoundEvent> sound, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound.get()),
                SoundSource.PLAYERS, at.x, at.y, at.z, volume, pitch, player.getRandom().nextLong()));
    }

    private static float wobble(float pitch) {
        return pitch * (0.95f + ThreadLocalRandom.current().nextFloat() * 0.1f);
    }

    private static Vec3 chestOf(Entity entity) {
        return entity.position().add(0, entity.getBbHeight() * 0.6, 0);
    }

    private static Vec3 headOf(Entity entity) {
        return entity.position().add(0, entity.getBbHeight() + 0.1, 0);
    }

    /** One block the way a yaw looks, level with the ground. */
    private static Vec3 ahead(float yaw) {
        double angle = Math.toRadians(yaw);
        return new Vec3(-Math.sin(angle), 0, Math.cos(angle));
    }

    private static float yawToward(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
    }

    private static int withAlpha(int argb, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0x00FFFFFF);
    }

    /** The color with its alpha scaled (strength 0..1). */
    private static int dimmed(int argb, float strength) {
        return withAlpha(argb, (int) (((argb >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, strength))));
    }

    private static float randomYaw(ServerLevel sl) {
        return sl.getRandom().nextFloat() * 360f;
    }

    // =====================================================================================
    // Statuses: a picture in the row above the hotbar stacks, and a clock beside the hotbar
    // =====================================================================================

    public enum Status {
        // what the Hermit holds
        PRYING("prying", "Prying", 0xC070E8),
        DOWSING("dowsing", "Dowsing", 0xE8C4FF),
        SIDESTEP("sidestep", "Sidestep", 0xF6F4FF),
        FORESIGHT("foresight", "Foresight", 0xE8C4FF),
        STAR_GUARD("star_guard", "Star Guard", 0xF6F4FF),
        AVALON("avalon", "Avalon", 0xCCF0C0),
        INFORMATIZATION("informatization", "Information", 0xC070E8),
        EMBODIMENT("embodiment", "Embodiment", 0x8A70D0),
        KINGDOM("kingdom", "Knowledge Moor", 0xC070E8),
        LAW_OF_THREE("law_of_three", "Law of Three", 0xF0DC96),
        CONDENSED("condensed", "Condensed", 0xF0DC96),
        ENLIGHTENED("enlightened", "Enlightened", 0xF0DC96),
        WHISPER("whisper", "Whisper", 0xE8C4FF),
        // what is done to others (when they are players)
        HEXED("hexed", "Hexed", 0xAA5ADC),
        ASLEEP("asleep", "Asleep", 0xFFD86A),
        MISFORTUNE("misfortune", "Misfortune", 0x8A6A8A),
        DOOMED("doomed", "Doomed", 0xE04050),
        ACHILLES("achilles", "Weak Spot", 0xF0DC96),
        UNDERSTOOD("understood", "Understood", 0xC070E8),
        OVERLOADED("overloaded", "Overloaded", 0xC070E8),
        INDOCTRINATED("indoctrinated", "Indoctrinated", 0x8A70D0),
        FROZEN("frozen", "Frozen", 0xBEECFF),
        PARALYZED("paralyzed", "Paralyzed", 0xAAFFAA),
        CAGED("caged", "Caged", 0xF0962A);

        final String image;
        final String label;
        final int rgb;

        Status(String image, String label, int rgb) {
            this.image = image;
            this.label = label;
            this.rgb = rgb;
        }

        String id() {
            return "ke_status_" + name().toLowerCase();
        }
    }

    /** Shows the status's picture (in the row above the hotbar stacks, rows of nine) and a clock beside the hotbar. */
    public static void status(LivingEntity player, Status status, int ticks) {
        int slot = status.ordinal();
        if (player instanceof Player viewerOfHud) ScreenImages.show(viewerOfHud, status.id(), "knowledge_emperor/" + status.image,
                ScreenAnchor.BOTTOM, 103 + (slot % 9) * 18, -21 - (slot / 9) * 18, 16, ticks);
        if (player instanceof Player viewerOfHud) HudTimer.show(viewerOfHud, status.id(), status.label, ticks, status.rgb);
    }

    /** How long something "until ended" is shown at most (an hour): show it again after that. */
    public static final int FOREVER = 72000;

    /** Only the status's picture, no clock: for what lasts until it is ended (the Whisper's free cast). */
    public static void statusForever(LivingEntity player, Status status) {
        int slot = status.ordinal();
        if (player instanceof Player viewerOfHud) ScreenImages.show(viewerOfHud, status.id(), "knowledge_emperor/" + status.image,
                ScreenAnchor.BOTTOM, 103 + (slot % 9) * 18, -21 - (slot / 9) * 18, 16, FOREVER);
        if (player instanceof Player viewerOfHud) HudTimer.hide(viewerOfHud, status.id());
    }

    /** It ended early. */
    public static void statusEnd(LivingEntity player, Status status) {
        if (player instanceof Player viewerOfHud) ScreenImages.hide(viewerOfHud, status.id());
        if (player instanceof Player viewerOfHud) HudTimer.hide(viewerOfHud, status.id());
    }

    // =====================================================================================
    // The scroll pouch: one picture per scroll, in the row beside the hotbar (where the Attendant's stacks are)
    // =====================================================================================

    /** How many slots the pouch can show (5 from Mysticologist; 3 before). */
    public static final int POUCH_SLOTS = 5;

    private static String pouchId(int slot) {
        return "ke_pouch_" + slot;
    }

    /** Shows the scrolls in the pouch, oldest first (the one Read Scroll burns next is on the left). */
    public static void pouch(LivingEntity player, List<Scroll> scrolls) {
        for (int slot = 0; slot < POUCH_SLOTS; slot++) {
            if (slot < scrolls.size()) {
                if (player instanceof Player viewerOfHud) ScreenImages.show(viewerOfHud, pouchId(slot), "knowledge_emperor/" + scrolls.get(slot).image,
                        ScreenAnchor.BOTTOM, 103 + slot * 18, -3, 16, ScreenImages.FOREVER);
            } else {
                if (player instanceof Player viewerOfHud) ScreenImages.hide(viewerOfHud, pouchId(slot));
            }
        }
    }

    public static void pouchHide(Player player) {
        for (int slot = 0; slot < POUCH_SLOTS; slot++) ScreenImages.hide(player, pouchId(slot));
    }

    // =====================================================================================
    // The wheels' pictures (textures/gui/ability_menus/knowledge_emperor/, for AbilityWheel.entry's icon)
    // =====================================================================================

    public static final String WHEEL_STARLIGHT_CAGE = "ability_menus/knowledge_emperor/starlight_cage";
    public static final String WHEEL_STELLAR_SELF = "ability_menus/knowledge_emperor/stellar_self";
    public static final String WHEEL_STAR_FALL = "ability_menus/knowledge_emperor/star_fall";
    public static final String WHEEL_STAR_GUARD = "ability_menus/knowledge_emperor/star_guard";
    public static final String WHEEL_SLEEPING_BEAUTY = "ability_menus/knowledge_emperor/sleeping_beauty";
    public static final String WHEEL_AVALON = "ability_menus/knowledge_emperor/avalon";
    public static final String WHEEL_ACHILLES_HEEL = "ability_menus/knowledge_emperor/achilles_heel";
    public static final String WHEEL_PANDORAS_BOX = "ability_menus/knowledge_emperor/pandoras_box";
    public static final String WHEEL_FORESIGHT = "ability_menus/knowledge_emperor/foresight";
    public static final String WHEEL_PROPHECY_OF_RUIN = "ability_menus/knowledge_emperor/prophecy_of_ruin";
    public static final String WHEEL_INFORMATIZATION = "ability_menus/knowledge_emperor/informatization";
    public static final String WHEEL_ERASE = "ability_menus/knowledge_emperor/erase";
    public static final String WHEEL_KNOWLEDGE_OVERLOAD = "ability_menus/knowledge_emperor/knowledge_overload";
    public static final String WHEEL_LANCE_OF_KNOWLEDGE = "ability_menus/knowledge_emperor/lance_of_knowledge";
    public static final String WHEEL_CONDENSE_KNOWLEDGE = "ability_menus/knowledge_emperor/condense_knowledge";
    public static final String WHEEL_EMBODIMENT = "ability_menus/knowledge_emperor/embodiment_of_knowledge";
    public static final String WHEEL_INDOCTRINATION = "ability_menus/knowledge_emperor/indoctrination";
    public static final String WHEEL_DIVINE_KINGDOM = "ability_menus/knowledge_emperor/divine_kingdom";

    // =====================================================================================
    // The models (models/model_fx/knowledge_emperor/, see each JSON's "credit")
    // =====================================================================================

    /** The Study eye, 1 block, standing, its own colors: shut, half open, wide open (frames of one model too). */
    public static final ModelFx[] STUDY_EYES = {
            ModelFx.of("knowledge_emperor/study_eye_0").pivot(8, 8, 8).glow().unshaded().seeThrough(),
            ModelFx.of("knowledge_emperor/study_eye_1").pivot(8, 8, 8).glow().unshaded().seeThrough(),
            ModelFx.of("knowledge_emperor/study_eye_2").pivot(8, 8, 8).glow().unshaded().seeThrough()};
    /** The same three as one model that opens: .frames(3).smooth() glides from shut to wide over its lifetime. */
    public static final ModelFx STUDY_EYE_OPENING = ModelFx.of("knowledge_emperor/study_eye").frames(3).smooth()
            .pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** The Hermit's symbol, a pair of cold eyes full of stars, 1 block, standing; the rings take a tint. */
    public static final ModelFx HERMIT_EYES = ModelFx.of("knowledge_emperor/hermit_eyes").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** The dowsing pendulum, 1.2 blocks; pivot at the ring on top, so it swings from there. */
    public static final ModelFx PENDULUM = ModelFx.of("knowledge_emperor/pendulum").pivot(8, 16, 8).glow();
    /** A rolled scroll, 1 block long, lying east-west; pivot its middle. */
    public static final ModelFx SCROLL = ModelFx.of("knowledge_emperor/scroll").pivot(8, 8, 8).glow();
    /** An unrolled scroll, 1 block, standing, its own colors. */
    public static final ModelFx SCROLL_SHEET = ModelFx.of("knowledge_emperor/scroll_sheet").pivot(8, 8, 8).glow().seeThrough();
    /** A small star, 0.9 blocks across; pivot its heart. */
    public static final ModelFx STAR = ModelFx.of("knowledge_emperor/star").pivot(8, 8, 8).glow().unshaded()
            .aura(0x60C070E8, 0.1f, 3);
    /** The Starlight Cage's amber, 1 block; pivot the middle of its bottom. Scale it to the one inside. */
    public static final ModelFx AMBER_BLOCK = ModelFx.of("knowledge_emperor/amber").glow().seeThrough();
    /** Sleeping Beauty's spindle, 1.6 blocks; pivot its middle. */
    public static final ModelFx SPINDLE = ModelFx.of("knowledge_emperor/spindle").pivot(8, 10, 8).glow()
            .aura(0x80FFD86A, 0.08f, 3);
    /** Pandora's box and its lid, 0.75 blocks; the lid's pivot is its hinge (pitch(-105) opens it). */
    public static final ModelFx BLACK_BOX = ModelFx.of("knowledge_emperor/black_box").glow();
    public static final ModelFx BLACK_BOX_LID = ModelFx.of("knowledge_emperor/black_box_lid").pivot(8, 7, 13).glow();
    /** The Lance of Knowledge, 2.8 blocks; top = the point, pivot its middle. Fly it with modelAlong or a Shot. */
    public static final ModelFx LANCE = ModelFx.of("knowledge_emperor/white_spear").pivot(8, 8, 8).glow().unshaded()
            .aura(0xA0F2F0FF, 0.12f, 3);
    /** A ball of illusory light, 0.75 blocks; pivot its middle; white, so tint it. */
    public static final ModelFx LIGHT_BALL = ModelFx.of("knowledge_emperor/light_ball").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** A spell's bolt, 0.9 blocks; top = its front; white, so tint it. */
    public static final ModelFx SPELL_BOLT = ModelFx.of("knowledge_emperor/spell_bolt").pivot(8, 8, 8).glow().unshaded();
    /** The book of tales lying open, 0.9 blocks; pivot the bottom of the spine. */
    public static final ModelFx TALE_BOOK = ModelFx.of("knowledge_emperor/tale_book").glow();
    /** Flat pictures, 1 block, white (tint them): a ring of glyphs, a circle of constellations, the Moor's floor, Avalon. */
    public static final ModelFx GLYPH_RING = ModelFx.of("knowledge_emperor/glyph_ring").glow().unshaded().seeThrough();
    public static final ModelFx CONSTELLATIONS = ModelFx.of("knowledge_emperor/constellation_circle").glow().unshaded().seeThrough();
    public static final ModelFx KNOWLEDGE_MOOR = ModelFx.of("knowledge_emperor/knowledge_moor").glow().unshaded().seeThrough();
    public static final ModelFx AVALON_SEAL = ModelFx.of("knowledge_emperor/avalon_seal").glow().unshaded().seeThrough();
    /** Standing pictures, 1 block, white (tint them); pivot their middle. */
    public static final ModelFx HEX_MARK = ModelFx.of("knowledge_emperor/hex_mark").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx FATE_WHEEL = ModelFx.of("knowledge_emperor/fate_wheel").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx ERASE_SIGIL = ModelFx.of("knowledge_emperor/erase_sigil").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx HEEL_MARK = ModelFx.of("knowledge_emperor/heel_mark").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx WEAK_POINT = ModelFx.of("knowledge_emperor/weak_point").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** Standing panels, 1 block, white (tint them); pivot the middle of their bottom edge. */
    public static final ModelFx LIBRARY_SHELF = ModelFx.of("knowledge_emperor/library_shelf").glow().unshaded().seeThrough();
    public static final ModelFx DATA_STREAM = ModelFx.of("knowledge_emperor/data_stream").glow().unshaded().seeThrough();
    /** A column of light, two crossed planes, 1 block; pivot the middle of its bottom. */
    public static final ModelFx LIGHT_COLUMN = ModelFx.of("knowledge_emperor/light_column").glow().unshaded().seeThrough();

    /** A flash of a standing picture at a spot, facing a yaw: pops up, then fades. */
    private static void flash(ServerLevel sl, ModelFx picture, Vec3 at, float yaw, int color, float size, int ticks) {
        ModelFx one = picture.color(color).scale(size).lifetime(ticks).fade(0, Math.max(2, ticks / 2))
                .key(0, pose().scale(0.3f).alpha(0f))
                .key(4, pose().scale(1.1f).alpha(1f), Ease.OUT_BACK)
                .key(ticks, pose().scale(0.9f).alpha(0f), Ease.IN);
        ParticleShapes.model(sl, one, at, yaw, 0f, 0f);
    }

    /** A flat picture lying on the ground, 'radius' wide, popping open and turning, for 'ticks'. */
    private static void floor(ServerLevel sl, ModelFx picture, Vec3 ground, double radius, int color, int ticks, float spin,
                              String tag, int delay) {
        ModelFx seal = picture.color(color).scale((float) (radius * 2.0)).delay(delay).lifetime(ticks)
                .fade(0, Math.min(14, ticks / 2)).spin(spin)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(10, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        if (tag != null) seal = seal.tag(tag);
        ParticleShapes.model(sl, seal, ground.add(0, 0.05, 0), randomYaw(sl), 0f, 0f);
    }

    // =====================================================================================
    // Study: the eye over an enemy that only the Hermit sees (docs/KNOWLEDGE_EMPEROR.md, "Study")
    // =====================================================================================

    private static String studyTag(LivingEntity viewer) {
        return "ke_study_" + viewer.getUUID();
    }

    /**
     * The Study eye over 'target', seen only by 'viewer': shut below 34%, half open below 67%, wide open from there
     * (and at 100%, Understood). Call it again whenever the Study changes; it replaces the one before. It is gone
     * by itself after 'ticks' (give it a few seconds and call it again while the Study lasts).
     */
    public static void studyEye(ServerPlayer viewer, LivingEntity target, int percent, int ticks) {
        String tag = studyTag(viewer);
        ParticleShapes.spawnFor(viewer, ModelFx.clearing(target, tag), target.position(), Vec3.ZERO);
        if (percent <= 0) return;
        int stage = percent < 34 ? 0 : percent < 67 ? 1 : 2;
        float size = 0.38f + stage * 0.06f;
        Vec3 offset = new Vec3(0, target.getBbHeight() + 0.55, 0);
        ModelFx eye = STUDY_EYES[stage].scale(size).lifetime(ticks).fade(4, 10).spin(3f).tag(tag);
        if (percent >= 100) eye = eye.aura(0x80C070E8, 0.05f, 3);
        ParticleShapes.spawnFor(viewer, eye.following(target, offset), target.position().add(offset), Vec3.ZERO);
    }

    /** Takes the viewer's Study eye off this target. */
    public static void studyEyeHide(ServerPlayer viewer, LivingEntity target) {
        ParticleShapes.spawnFor(viewer, ModelFx.clearing(target, studyTag(viewer)), target.position(), Vec3.ZERO);
    }

    /** Study rose: a small orchid glimmer above the head, only the viewer sees it. */
    public static void studyGain(ServerPlayer viewer, LivingEntity target) {
        Vec3 at = headOf(target).add(0, 0.3, 0);
        for (int i = 0; i < 3; i++) {
            double a = Math.PI * 2 * i / 3;
            ParticleShapes.spawnFor(viewer, ORCHID_MOTE, at.add(Math.cos(a) * 0.25, 0, Math.sin(a) * 0.25), new Vec3(0, 0.03, 0));
        }
    }

    /** The target is Understood (100%): stars burst from the eye and a soft chime, both only for the viewer. */
    public static void understood(ServerPlayer viewer, LivingEntity target) {
        Vec3 at = headOf(target).add(0, 0.45, 0);
        for (int i = 0; i < 10; i++) {
            double a = Math.PI * 2 * i / 10;
            ParticleShapes.spawnFor(viewer, STAR_SPARK, at, new Vec3(Math.cos(a) * 0.12, 0.05, Math.sin(a) * 0.12));
        }
        soundFor(viewer, at, ModSounds.KNOWLEDGE_CHIME, 0.6f, 1.3f);
    }

    // =====================================================================================
    // Key 1: Mystery Prying / Dowsing (Mystery Pryer)
    // =====================================================================================

    /**
     * The eyes open: the Hermit's symbol flares before the face and a ring of sight races out 'reach' blocks.
     * secretInquiry = the Eyes of Secret Inquiry (Constellations Master): the ring is wider and stars wheel round.
     */
    public static void pry(ServerLevel sl, LivingEntity player, double reach, boolean secretInquiry) {
        ModelFx eyes = HERMIT_EYES.color(secretInquiry ? 0xFFF6F4FF : ORCHID).scale(0.7f).lifetime(24).fade(2, 12)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(5, pose().scale(1f).alpha(1f), Ease.OUT_BACK)
                .key(24, pose().forward(0.6f).scale(1.6f).alpha(0f), Ease.IN);
        ParticleShapes.modelOnHead(sl, eyes, player, -0.25, 0f, 0f, 0f);
        SpellFx.steadyRing(sl, player.position(), Math.min(reach, 24.0), 0xA0C070E8, 14, 0);
        if (secretInquiry) {
            SpellFx.steadyRing(sl, player.position(), Math.min(reach, 32.0), 0x80F6F4FF, 18, 3);
            ParticleShapes.spiral(sl, STAR_SPARK, player.position().add(0, 0.2, 0), 4, 24, 0.6, 2.4, 0.8, 0.08);
        }
        ParticleShapes.burst(sl, STAR_MOTE, player.getEyePosition(), 10, 0.03, 0.1);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_PRY, 1.0f, secretInquiry ? 0.9f : 1.0f);
    }

    /** The one looked at is read: a weak point flashes on it and its Study eye opens (only the viewer sees). */
    public static void pryRead(ServerPlayer viewer, LivingEntity target) {
        ModelFx mark = WEAK_POINT.color(0xC0E8C4FF).scale(0.9f).lifetime(20).fade(0, 10)
                .key(0, pose().scale(1.6f).alpha(0f))
                .key(6, pose().scale(1f).alpha(1f), Ease.OUT);
        float yaw = yawToward(chestOf(target), viewer.getEyePosition());
        ParticleShapes.spawnFor(viewer, mark.rotated(yaw, 0f, 0f), chestOf(target), Vec3.ZERO);
        studyGain(viewer, target);
    }

    /** Something hidden within reach is glimpsed: faint stars trace round it (the glowing is the ability's). */
    public static void pryGlimpse(ServerLevel sl, Entity hidden) {
        ParticleShapes.ring(sl, STAR_MOTE, hidden.position().add(0, 0.2, 0), Math.max(0.4, hidden.getBbWidth() * 0.7), 8, 0);
        ParticleShapes.spawn(sl, EYE_MARK, headOf(hidden).add(0, 0.4, 0), Vec3.ZERO);
    }

    // ---------- sneak: Dowsing

    /**
     * A silver pendulum hangs from the raised hand. With a target (someone hidden), it swings toward them and a thread
     * of light points the way for 'ticks'; with none (null), it hangs still and fades.
     */
    public static void dowse(ServerLevel sl, LivingEntity player, Entity target, int ticks, String tag) {
        Vec3 hand = player.getEyePosition().add(Spells.flatLook(player).scale(0.55)).add(Spells.rightOf(player).scale(0.3));
        int life = target == null ? 40 : Math.min(ticks, 80);
        float toward = target == null ? 0f : yawToward(player.position(), target.position()) - player.getYRot();
        ModelFx pendulum = PENDULUM.scale(0.55f).lifetime(life).fade(4, 10).tag(tag)
                .key(0, pose().forward(0.5f).right(0.3f).roll(-25f).alpha(0f))
                .key(8, pose().roll(22f).alpha(1f), Ease.IN_OUT)
                .key(16, pose().roll(-16f), Ease.IN_OUT)
                .key(24, pose().roll(10f), Ease.IN_OUT);
        if (target != null) {
            pendulum = pendulum.key(34, pose().roll(0f).yaw(toward).pitch(-35f), Ease.OUT);   // it leans toward them
        } else {
            pendulum = pendulum.key(34, pose().roll(0f), Ease.OUT);
        }
        ParticleShapes.modelOnTurning(sl, pendulum, player, new Vec3(0, player.getEyeHeight() - 0.15, 0), 0f, 0f, 0f);
        sound(sl, hand, ModSounds.KNOWLEDGE_DOWSE, target == null ? 0.5f : 0.9f, target == null ? 0.8f : 1.0f);
        if (target == null) return;
        Later.run(sl, 34, () -> {
            if (!player.isAlive()) return;
            SpellFx.tether(sl, THREAD, () -> SpellFx.handOf(player, sl), () -> SpellFx.middleOf(target, sl), Math.max(1, ticks - 34));
            ParticleShapes.burst(sl, STAR_MOTE, chestOf(player), 6, 0.02, 0.06);
        });
    }

    public static void dowseEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
    }

    // =====================================================================================
    // Key 2: Weak-Point Strike / Sidestep (Melee Scholar)
    // =====================================================================================

    /** The tick the strike lands: the damage goes there. */
    public static final int STRIKE_LANDS = 3;

    /** A step forward ('from' = where the Scholar stood) and a precise cut where the target is weakest. */
    public static void weakPointStrike(ServerLevel sl, LivingEntity player, Vec3 from, LivingEntity target) {
        ParticleShapes.slashBetween(sl, DASH, from.add(0, 1.0, 0), chestOf(player));
        ParticleShapes.burst(sl, PAGE, from.add(0, 0.6, 0), 3, 0.03, 0.08);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_WEAK_POINT, 1.0f, wobble(1.0f));
        if (target == null) return;
        Later.run(sl, STRIKE_LANDS, () -> {
            if (!target.isAlive()) return;
            Vec3 at = chestOf(target);
            float yaw = yawToward(player.position(), target.position());
            ParticleShapes.slash(sl, STRIKE_ARC, chestOf(player).add(Spells.flatLook(player).scale(0.5)), player.getYRot(), 0f, -30f);
            flash(sl, WEAK_POINT, at, yaw + 180f, 0xF0F6F4FF, 1.0f, 12);
            ParticleShapes.burst(sl, SPARKLE, at, 10, 0.06, 0.2);
            ParticleShapes.burst(sl, ORCHID_MOTE, at, 6, 0.03, 0.1);
        });
    }

    // ---------- sneak: Sidestep

    /** Ready to slip aside: a ring of pages turns at the feet for 'ticks'. */
    public static void sidestep(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        SpellFx.circleUnder(sl, player, 0.9, 0x80E8C4FF, ticks, tag);
        ParticleShapes.ring(sl, PAGE, player.position().add(0, 0.3, 0), 0.8, 8, 0.03);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_SIDESTEP, 0.5f, 1.3f);
    }

    /** The blow is slipped: an afterimage of pages stays where the Scholar was, a streak to where they went. */
    public static void sidestepSlip(ServerLevel sl, LivingEntity player, Vec3 from, Vec3 to) {
        ParticleShapes.slashBetween(sl, DASH.width(0.4f), from.add(0, 1.0, 0), to.add(0, 1.0, 0));
        ParticleShapes.burst(sl, PAGE, from.add(0, 1.0, 0), 8, 0.04, 0.12);
        ParticleShapes.burst(sl, TEXT, from.add(0, 1.0, 0), 5, 0.02, 0.06);
        sound(sl, to, ModSounds.KNOWLEDGE_SIDESTEP, 1.0f, wobble(1.0f));
    }

    public static void sidestepEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
    }

    // =====================================================================================
    // Key 3: Hex / Spellbolts (Warlock)
    // =====================================================================================

    /** The tick the hex lands on its target. */
    public static final int HEX_LANDS = 6;

    /** A dashed violet line from the hand, then the hex mark settles over the target for 'ticks'. */
    public static void hex(ServerLevel sl, LivingEntity player, LivingEntity target, int ticks, String tag) {
        Vec3 hand = SpellFx.handOf(player, sl);
        if (hand == null) hand = chestOf(player);
        ParticleShapes.slashBetween(sl, HEX_LINE, hand, chestOf(target));
        ParticleShapes.burst(sl, HEX_MOTE, hand, 6, 0.02, 0.08);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_HEX, 1.0f, wobble(1.0f));
        Later.run(sl, HEX_LANDS, () -> {
            if (!target.isAlive()) return;
            hexed(sl, target, ticks - HEX_LANDS, tag);
            ParticleShapes.burst(sl, HEX_MOTE, chestOf(target), 12, 0.04, 0.12);
        });
    }

    /** The mark itself, on its own: over the head and a violet circle underfoot. */
    public static void hexed(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        if (ticks <= 0) return;
        ParticleShapes.clearModels(sl, target, tag);
        ModelFx mark = HEX_MARK.color(0xE0AA5ADC).scale(0.5f).lifetime(ticks).fade(4, 10).spin(2.5f).tag(tag)
                .key(0, pose().scale(1.8f).alpha(0f))
                .key(6, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOn(sl, mark, target, new Vec3(0, target.getBbHeight() + 0.5, 0), 0f, 0f, 0f);
        SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth()), 0x90AA5ADC, ticks, tag + "_circle");
    }

    /** The hexed died: the mark leaps to the next one for the time left. */
    public static void hexJump(ServerLevel sl, LivingEntity from, LivingEntity to, int ticks, String tag) {
        hexEnd(sl, from, tag);
        ModelFx leaping = HEX_MARK.color(0xE0AA5ADC).scale(0.5f).lifetime(10).fade(0, 3).spin(12f);
        SpellFx.homing(sl, leaping, headOf(from), to, 8);
        sound(sl, chestOf(from), ModSounds.KNOWLEDGE_HEX_JUMP, 0.9f, wobble(1.0f));
        Later.run(sl, 8, () -> {
            if (to.isAlive()) hexed(sl, to, ticks - 8, tag);
        });
    }

    public static void hexEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.clearModels(sl, target, tag + "_circle");
        ParticleShapes.burst(sl, HEX_MOTE, chestOf(target), 8, 0.02, 0.06);
    }

    // ---------- sneak: Spellbolts (and the Sage's Knowledge Orbs)

    /** How long a bolt flies before it lands, and how many ticks apart the bolts leave. */
    public static final int BOLT_FLIGHT = 8;
    public static final int BOLT_STEP = 3;

    /** A spell's bolt of one element, white tinted: for a Shot when nothing is in sight (it leads with its top). */
    public static ModelFx BOLT(Element element) {
        return SPELL_BOLT.color(element.color).scale(0.8f).aura(withAlpha(element.color, 0x90), 0.06f, 3);
    }

    private static ParticleFx boltParticle(Element element) {
        return switch (element) {
            case FROST -> FROST;
            case LIGHTNING -> SPARK;
            case FIRE -> SpellFx.FIRE_EMBER;
        };
    }

    /**
     * The bolts leave the hand one after another (bolt i at i * BOLT_STEP) and fly into their targets, which they
     * cannot miss (bolt i lands at BOLT_FLIGHT + i * BOLT_STEP; do boltHit then). One target may be in the list
     * several times.
     */
    public static void spellbolts(ServerLevel sl, LivingEntity player, List<LivingEntity> targets, Element element) {
        for (int i = 0; i < targets.size(); i++) {
            LivingEntity target = targets.get(i);
            int index = i;
            Later.run(sl, i * BOLT_STEP, () -> {
                if (!player.isAlive() || !target.isAlive()) return;
                Vec3 hand = SpellFx.handOf(player, sl);
                if (hand == null) hand = chestOf(player);
                Vec3 from = hand.add(Spells.rightOf(player).scale((index - 1) * 0.35)).add(0, (index % 2) * 0.2, 0);
                ModelFx bolt = BOLT(element).lifetime(BOLT_FLIGHT + 4).fade(0, 4)
                        .during(BOLT_FLIGHT, BOLT_FLIGHT + 4, pose().forward(1f));
                SpellFx.homing(sl, bolt.pivot(8, 8, 8).rotation(0f, 90f, 0f), from, target, BOLT_FLIGHT);
                ParticleShapes.burst(sl, boltParticle(element), from, 4, 0.02, 0.08);
                ParticleShapes.burst(sl, GLYPHS[index % GLYPHS.length], from, 2, 0.02, 0.05);
                if (index == 0) sound(sl, player.position(), element.sound(), 1.0f, wobble(1.0f));
            });
        }
    }

    /** A bolt lands: a small burst of its element. */
    public static void boltHit(ServerLevel sl, LivingEntity target, Element element) {
        boltBurst(sl, chestOf(target), element);
    }

    /** A burst of the element at a spot (a bolt that hit nothing, a Shot's end). */
    public static void boltBurst(ServerLevel sl, Vec3 at, Element element) {
        switch (element) {
            case FROST -> {
                ParticleShapes.burst(sl, FROST, at, 12, 0.05, 0.18);
                SpellFx.shockRing(sl, at.add(0, -0.6, 0), 1.2, 0xC0BEECFF, 6, 0);
            }
            case LIGHTNING -> {
                ParticleShapes.burst(sl, SPARK, at, 16, 0.08, 0.3);
                for (int i = 0; i < 3; i++) {
                    Vec3 end = at.add(ParticleShapes.randomDirectionInCone(sl.getRandom(), Spells.UP, 360).scale(0.9));
                    ParticleShapes.slashBetween(sl, RAY_LINE.color(0xE0C8B0FF).width(0.08f).lifetime(4), at, end);
                }
            }
            case FIRE -> SpellFx.blast(sl, at, 1.2, 0xFFFF8A2A, SpellFx.FIRE_EMBER);
        }
        sound(sl, at, element.sound(), 0.6f, wobble(1.3f));
    }

    /** Every tick a bolt Shot flies: a little of its element behind it. */
    public static void boltTrail(ServerLevel sl, Vec3 at, Element element) {
        ParticleShapes.spawn(sl, boltParticle(element), at, Vec3.ZERO);
    }

    /** The Knowledge Orbs (Sage): how long one flies. */
    public static final int ORB_FLIGHT = 12;

    /** Five homing orbs of orchid light (orb i leaves at i * BOLT_STEP, lands at ORB_FLIGHT + i * BOLT_STEP). */
    public static void knowledgeOrbs(ServerLevel sl, LivingEntity player, List<LivingEntity> targets) {
        for (int i = 0; i < targets.size(); i++) {
            LivingEntity target = targets.get(i);
            int index = i;
            Later.run(sl, i * BOLT_STEP, () -> {
                if (!player.isAlive() || !target.isAlive()) return;
                double angle = Math.PI * 2 * index / Math.max(1, targets.size());
                Vec3 from = chestOf(player).add(0, 0.6, 0)
                        .add(Spells.rightOf(player).scale(Math.cos(angle) * 0.8)).add(0, Math.sin(angle) * 0.5, 0);
                ModelFx orb = LIGHT_BALL.color(0xE0C070E8).scale(0.45f).lifetime(ORB_FLIGHT + 3).fade(2, 3).spin(10f);
                SpellFx.homing(sl, orb, from, target, ORB_FLIGHT);
                ParticleShapes.burst(sl, ORCHID_MOTE, from, 4, 0.02, 0.06);
                if (index == 0) sound(sl, player.position(), ModSounds.KNOWLEDGE_BOLT_LIGHTNING, 0.8f, 1.4f);
            });
        }
    }

    /** An orb lands: a ring of glyphs and a stun-spark. */
    public static void orbHit(ServerLevel sl, LivingEntity target) {
        Vec3 at = chestOf(target);
        glyphBurst(sl, at, 8, 0.05, 0.15);
        ParticleShapes.burst(sl, ORCHID_MOTE, at, 8, 0.04, 0.12);
        SpellFx.shockRing(sl, target.position(), 1.4, 0xC0C070E8, 6, 0);
        sound(sl, at, ModSounds.KNOWLEDGE_CHIME, 0.5f, wobble(1.6f));
    }

    // =====================================================================================
    // Key 4: Read Scroll / Inscribe Scroll (Scrolls Professor), the free scrolls
    // =====================================================================================

    /** A free scroll arrives in the pouch: a small scroll appears beside the Professor and sinks into them. */
    public static void scrollReady(ServerLevel sl, LivingEntity player, Scroll scroll) {
        ModelFx arriving = SCROLL.scale(0.35f).lifetime(20).fade(4, 6)
                .key(0, pose().right(0.6f).up(0.6f).alpha(0f).spin(0f))
                .key(8, pose().right(0.6f).up(0.4f).alpha(1f).spin(180f), Ease.OUT)
                .key(20, pose().right(0.1f).up(0f).scale(0.2f).spin(360f), Ease.IN);
        ParticleShapes.modelOnTurning(sl, arriving, player, new Vec3(0, 1.0, 0), 0f, 0f, 0f);
        ParticleShapes.burst(sl, scroll == Scroll.FREEZING ? FROST : scroll == Scroll.PARALYSIS ? GREEN_SPARK : GOLD_MOTE,
                chestOf(player).add(Spells.rightOf(player).scale(0.5)), 4, 0.01, 0.04);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_SCROLL_READY, 0.5f, wobble(1.0f));
    }

    /** The tick the read spell leaves the page: cast its effect there. */
    public static final int READ_FIRES = 6;

    /** The oldest scroll unrolls before the Professor, its spell leaps off and the page burns away. */
    public static void readScroll(ServerLevel sl, LivingEntity player, Scroll scroll) {
        ModelFx sheet = SCROLL_SHEET.scale(0.8f).lifetime(18).fade(0, 8)
                .key(0, pose().forward(0.7f).scale(0.2f).alpha(0f))
                .key(READ_FIRES, pose().forward(0.9f).scale(1f).alpha(1f), Ease.OUT_BACK)
                .key(18, pose().forward(1.1f).up(0.4f).scale(0.6f).alpha(0f), Ease.IN);
        ParticleShapes.modelOnTurning(sl, sheet, player, new Vec3(0, 1.3, 0), 0f, 0f, 0f);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_SCROLL_READ, 1.0f, wobble(1.0f));
        Later.run(sl, READ_FIRES, () -> {
            if (!player.isAlive()) return;
            Vec3 page = chestOf(player).add(0, 0.5, 0).add(Spells.flatLook(player).scale(0.9));
            ParticleShapes.burst(sl, BURNING_PAGE, page, 10, 0.03, 0.1);
            ParticleShapes.burst(sl, SpellFx.FIRE_EMBER, page, 6, 0.02, 0.08);
            ParticleShapes.burst(sl, scroll == Scroll.FREEZING ? FROST : scroll == Scroll.PARALYSIS ? GREEN_SPARK : GOLD_MOTE,
                    page, 8, 0.04, 0.12);
        });
    }

    /** The Scroll of Freezing's ray: the tick it reaches the target. */
    public static final int RAY_FALLS = 8;

    /** A clear ray falls on the target from above and frost grows on it for 'ticks'. */
    public static void freezingRay(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        float width = Math.max(0.8f, target.getBbWidth() * 1.4f);
        ModelFx column = LIGHT_COLUMN.color(0xC0BEECFF).scale(width, 5f, width).lifetime(RAY_FALLS + 12).fade(0, 10)
                .key(0, pose().up(6f).scale(0.3f).alpha(0f))
                .key(RAY_FALLS, pose().up(0f).scale(1f).alpha(1f), Ease.IN);
        ParticleShapes.model(sl, column, target.position(), 0f, 0f, 0f);
        sound(sl, target.position(), ModSounds.KNOWLEDGE_FREEZE_RAY, 1.0f, wobble(1.0f));
        Later.run(sl, RAY_FALLS, () -> {
            if (!target.isAlive()) return;
            frozen(sl, target, ticks, tag);
            SpellFx.shockRing(sl, target.position(), 2.0, 0xD0BEECFF, 8, 0);
        });
    }

    /** Frost on someone held by the Scroll of Freezing (slow, cannot jump): flakes and a pale circle. */
    public static void frozen(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth()), 0xA0BEECFF, ticks, tag);
        ParticleShapes.burst(sl, FROST, chestOf(target), 14, 0.03, 0.1);
        for (int t = 10; t < ticks; t += 10) {
            Later.run(sl, t, () -> {
                if (target.isAlive()) ParticleShapes.burst(sl, FROST, chestOf(target), 3, 0.01, 0.04);
            });
        }
    }

    /** The Scroll of Paralysis's ray: the tick it hits. */
    public static final int RAY_HITS = 4;

    /** A pale green ray flies from the Professor into the target, then holds it for 'ticks'. */
    public static void paralysisRay(ServerLevel sl, LivingEntity player, LivingEntity target, int ticks, String tag) {
        Vec3 hand = SpellFx.handOf(player, sl);
        if (hand == null) hand = chestOf(player);
        ParticleShapes.slashBetween(sl, RAY_LINE, hand, chestOf(target));
        sound(sl, player.position(), ModSounds.KNOWLEDGE_PARALYSIS_RAY, 1.0f, wobble(1.0f));
        Later.run(sl, RAY_HITS, () -> {
            if (!target.isAlive()) return;
            paralyzed(sl, target, ticks, tag);
        });
    }

    /** Held by the Scroll of Paralysis: green sparks crawl over the body, a green circle underfoot. */
    public static void paralyzed(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth()), 0xA0AAFFAA, ticks, tag);
        ParticleShapes.burst(sl, GREEN_SPARK, chestOf(target), 12, 0.05, 0.15);
        for (int t = 4; t < ticks; t += 4) {
            Later.run(sl, t, () -> {
                if (target.isAlive()) ParticleShapes.burst(sl, GREEN_SPARK, chestOf(target), 2, 0.04, 0.1);
            });
        }
    }

    // ---------- sneak: Inscribe Scroll

    /** The tick the scroll is written and rolled up. */
    public static final int INSCRIBED = 30;

    /** A blank sheet hangs before the Professor; a ring of glyphs turns round it, ink flies, a quill writes; it rolls up. */
    public static void inscribe(ServerLevel sl, LivingEntity player) {
        ModelFx sheet = SCROLL_SHEET.scale(0.8f).lifetime(INSCRIBED + 6).fade(4, 4)
                .key(0, pose().forward(0.9f).alpha(0f).scale(0.6f))
                .key(6, pose().forward(0.9f).alpha(1f).scale(1f), Ease.OUT)
                .key(INSCRIBED, pose().forward(0.9f).scale(1f))
                .key(INSCRIBED + 6, pose().forward(0.6f).scale(0.1f).alpha(0f), Ease.IN);
        ParticleShapes.modelOnTurning(sl, sheet, player, new Vec3(0, 1.3, 0), 0f, 0f, 0f);
        ModelFx ring = GLYPH_RING.color(0xC0E8C4FF).scale(1.4f).lifetime(INSCRIBED).fade(4, 6).spin(6f)
                .key(0, pose().forward(0.9f).pitch(90f).scale(0.3f).alpha(0f))
                .key(6, pose().forward(0.9f).pitch(90f).scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOnTurning(sl, ring, player, new Vec3(0, 1.3, 0), 0f, 0f, 0f);
        for (int t = 4; t < INSCRIBED; t += 3) {
            int step = t;
            Later.run(sl, t, () -> {
                if (!player.isAlive()) return;
                Vec3 page = chestOf(player).add(0, 0.5, 0).add(Spells.flatLook(player).scale(0.9));
                double across = Math.sin(step * 0.6) * 0.3;
                Vec3 nib = page.add(Spells.rightOf(player).scale(across)).add(0, 0.25 - step * 0.015, 0);
                ParticleShapes.spawn(sl, QUILL.lifetime(4, 0), nib.add(0, 0.12, 0), Vec3.ZERO);
                ParticleShapes.spawn(sl, INK_DROP, nib, new Vec3(0, 0.02, 0));
                ParticleShapes.spawn(sl, anyGlyph(), nib, Spells.flatLook(player).scale(-0.02));
            });
        }
        sound(sl, player.position(), ModSounds.KNOWLEDGE_SCROLL_INSCRIBE, 1.0f, wobble(1.0f));
        Later.run(sl, INSCRIBED, () -> {
            if (!player.isAlive()) return;
            ParticleShapes.burst(sl, GOLD_MOTE, chestOf(player).add(Spells.flatLook(player).scale(0.8)), 10, 0.03, 0.08);
            scrollReady(sl, player, Scroll.INSCRIBED);
        });
    }

    /** The pouch was full: its oldest scroll crumbles to dust beside the Professor. */
    public static void scrollDropped(ServerLevel sl, LivingEntity player) {
        Vec3 at = chestOf(player).add(Spells.rightOf(player).scale(-0.6));
        ParticleShapes.burst(sl, PAGE.color(0xFFB8A070).endColor(0x00604020), at, 8, 0.02, 0.06);
        ParticleShapes.burst(sl, INK_SMOKE, at, 3, 0.01, 0.03);
    }

    // =====================================================================================
    // Key 5: the Stars wheel (Constellations Master)
    // =====================================================================================

    /** The tick the starlight reaches the target and the amber sets (hold everyone from here). */
    public static final int CAGE_LANDS = 14;

    /** Starlight gathers round the caster, then flies to the target as a small star. */
    public static void starlightCage(ServerLevel sl, LivingEntity player, LivingEntity target) {
        SpellFx.closingRing(sl, player.position(), 2.6, 0xC0F6F4FF, 8, 0);
        ParticleShapes.spiral(sl, STAR_MOTE, player.position().add(0, 0.8, 0), 3, 30, 0.4, 2.2, 1.0, -0.08);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_STARLIGHT_GATHER, 1.0f, 1.0f);
        Later.run(sl, 6, () -> {
            if (!player.isAlive() || !target.isAlive()) return;
            ModelFx star = STAR.scale(0.6f).lifetime(CAGE_LANDS - 6 + 2).fade(0, 2).spin(20f);
            SpellFx.homing(sl, star, chestOf(player).add(0, 0.6, 0), target, CAGE_LANDS - 6);
        });
    }

    /** Someone held in the amber for 'ticks': it grows round their body and starlight is frozen in it. */
    public static void caged(ServerLevel sl, LivingEntity held, int ticks, String tag) {
        float w = held.getBbWidth() * 1.4f + 0.2f;
        float h = held.getBbHeight() + 0.25f;
        ModelFx amber = AMBER_BLOCK.scale(w, h, w).lifetime(ticks).fade(0, 6).tag(tag)
                .key(0, pose().scale(0.1f).alpha(0f))
                .key(5, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOn(sl, amber, held, Vec3.ZERO, randomYaw(sl), 0f, 0f);
        ParticleShapes.burst(sl, AMBER_MOTE, chestOf(held), 12, 0.04, 0.12);
        ParticleShapes.burst(sl, STAR_SPARK, chestOf(held), 6, 0.03, 0.1);
        sound(sl, held.position(), ModSounds.KNOWLEDGE_AMBER_SET, 1.0f, wobble(1.0f));
    }

    /** The amber takes a blow: chips fly. */
    public static void cageCrack(ServerLevel sl, LivingEntity held) {
        ParticleShapes.burst(sl, AMBER_SHARD, chestOf(held), 5, 0.06, 0.15);
    }

    /** The amber broke (it took enough damage): it shatters. */
    public static void cageBreak(ServerLevel sl, LivingEntity held, String tag) {
        ParticleShapes.clearModels(sl, held, tag);
        ParticleShapes.burst(sl, AMBER_SHARD, chestOf(held), 22, 0.08, 0.25);
        ParticleShapes.burst(sl, STAR_SPARK, chestOf(held), 8, 0.05, 0.15);
        sound(sl, held.position(), ModSounds.KNOWLEDGE_AMBER_BREAK, 1.0f, wobble(1.0f));
    }

    /** The hold ran out: the amber melts away as motes. */
    public static void cageEnd(ServerLevel sl, LivingEntity held, String tag) {
        ParticleShapes.clearModels(sl, held, tag);
        ParticleShapes.burst(sl, AMBER_MOTE, chestOf(held), 14, 0.02, 0.08);
    }

    // ---------- Stellar Self

    /** The tick the body is whole again at the far end (the ability moves it there). */
    public static final int STELLAR_ARRIVES = 20;

    /** The body breaks into starlight at 'from' (a blinding flash there), streams to 'to' and re-forms. */
    public static void stellarSelf(ServerLevel sl, LivingEntity player, Vec3 from, Vec3 to) {
        Vec3 chestFrom = from.add(0, 1.0, 0);
        ParticleShapes.burst(sl, STAR_MOTE, chestFrom, 30, 0.05, 0.22);
        ParticleShapes.burst(sl, SPARKLE, chestFrom, 16, 0.08, 0.3);
        SpellFx.blast(sl, chestFrom, 1.6, 0xFFF6F4FF, STAR_SPARK);
        sound(sl, from, ModSounds.KNOWLEDGE_STELLAR_SELF, 1.0f, wobble(1.0f));
        streamStars(sl, chestFrom, to.add(0, 1.0, 0), STELLAR_ARRIVES, 5);
        Later.run(sl, STELLAR_ARRIVES, () -> {
            SpellFx.closingRing(sl, to, 2.0, 0xD0F6F4FF, 6, 0);
            ParticleShapes.burst(sl, STAR_MOTE, to.add(0, 1.0, 0), 16, 0.02, 0.08);
        });
    }

    /** An ally carried along (Clairvoyant): their own smaller stream. */
    public static void stellarCarry(ServerLevel sl, LivingEntity ally, Vec3 from, Vec3 to) {
        ParticleShapes.burst(sl, STAR_MOTE, from.add(0, 1.0, 0), 14, 0.04, 0.15);
        streamStars(sl, from.add(0, 1.0, 0), to.add(0, 1.0, 0), STELLAR_ARRIVES, 3);
    }

    /** Small stars flying along a path over 'ticks', and a trail of motes on the way. */
    private static void streamStars(ServerLevel sl, Vec3 from, Vec3 to, int ticks, int count) {
        Vec3 way = to.subtract(from);
        for (int i = 0; i < count; i++) {
            int delay = i * 2;
            float spread = (i - count / 2f) * 0.25f;
            ModelFx star = STAR.scale(0.3f).delay(delay).lifetime(ticks - delay + 2).fade(2, 3).spin(25f)
                    .key(0, pose().right(spread))
                    .key(ticks - delay, pose().up((float) way.length()).right(0f), Ease.IN_OUT);
            ParticleShapes.modelAlong(sl, star, from, way);
        }
        ParticleShapes.line(sl, STAR_MOTE, from, to, 2.0);
        ParticleShapes.slashBetween(sl, STAR_TRAIL.lifetime(ticks).sweep(Math.max(1, ticks - 4)), from, to);
    }

    // ---------- Star Fall

    /** How long a star falls (its warning ring shows for that long) before it lands: burst there. */
    public static final int STAR_LANDS = 20;

    /** A circle of constellations in the sky over the area while the stars fall. */
    public static void starFallSky(ServerLevel sl, Vec3 center, int ticks, String tag) {
        ModelFx sky = CONSTELLATIONS.color(0xA0F6F4FF).scale(7f).lifetime(ticks).fade(8, 12).spin(1.2f).tag(tag)
                .key(0, pose().scale(0.3f).alpha(0f))
                .key(12, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.model(sl, sky, center.add(0, 9, 0), randomYaw(sl), 0f, 0f);
    }

    /** One star: a faint ring marks where it will land, it falls from the sky and lands at STAR_LANDS. */
    public static void starFall(ServerLevel sl, Vec3 ground, double radius) {
        SpellFx.warningCircle(sl, ground, radius, 0x80E8C4FF, STAR_LANDS + 4);
        Vec3 sky = ground.add(sl.getRandom().nextDouble() * 4 - 2, 14, sl.getRandom().nextDouble() * 4 - 2);
        Vec3 down = ground.subtract(sky);
        ModelFx star = STAR.scale(0.9f).lifetime(STAR_LANDS + 1).fade(3, 1).spin(18f)
                .key(STAR_LANDS, pose().up((float) down.length()), Ease.IN);
        ParticleShapes.modelAlong(sl, star, sky, down);
        ParticleShapes.slashBetween(sl, STAR_TRAIL.width(0.25f).lifetime(STAR_LANDS).sweep(STAR_LANDS), sky, ground);
        sound(sl, ground, ModSounds.KNOWLEDGE_STAR_FALL, 0.9f, wobble(1.0f));
        Later.run(sl, STAR_LANDS, () -> starBurst(sl, ground, radius));
    }

    /** Where a star lands: a white burst, a ring and a spray of star sparks. */
    public static void starBurst(ServerLevel sl, Vec3 ground, double radius) {
        SpellFx.blast(sl, ground.add(0, 0.4, 0), radius * 0.8, 0xFFF6F4FF, STAR_SPARK);
        SpellFx.shockRing(sl, ground, radius * 1.2, 0xD0E8C4FF, 8, 0);
        ParticleShapes.burst(sl, SPARKLE, ground.add(0, 0.5, 0), 14, 0.08, 0.3);
        sound(sl, ground, ModSounds.KNOWLEDGE_STAR_BURST, 1.0f, wobble(1.0f));
    }

    public static void starFallEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center.add(0, 9, 0), tag);
    }

    // ---------- Star Guard

    /** The four guarding stars: how far out they circle and how fast (degrees per tick). */
    private static final double GUARD_RADIUS = 1.2;
    private static final float GUARD_SPEED = 6f;

    private static String guardTag(String tag, int index) {
        return tag + "_" + index;
    }

    private static Vec3 guardSpot(LivingEntity player, int index, int age) {
        double angle = Math.toRadians(age * GUARD_SPEED + index * 90.0);
        return player.position().add(Math.cos(angle) * GUARD_RADIUS, 1.1 + Math.sin(age * 0.15 + index) * 0.15,
                Math.sin(angle) * GUARD_RADIUS);
    }

    /**
     * Four small stars take their places round the caster for 'ticks'. They do not follow by themselves: call
     * starGuardTick every tick, which moves them round the caster (Star Guard does it from combat/Orbit's onTick).
     */
    public static void starGuard(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        for (int i = 0; i < 4; i++) spawnGuardStar(sl, player, tag, i, ticks, 0);
        ParticleShapes.ring(sl, STAR_MOTE, player.position().add(0, 1.1, 0), GUARD_RADIUS, 16, 0.02);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_STAR_GUARD, 1.0f, 1.0f);
    }

    private static void spawnGuardStar(ServerLevel sl, LivingEntity player, String tag, int index, int ticks, int age) {
        ModelFx star = STAR.scale(0.35f).lifetime(ticks).fade(4, 8).spin(12f).tag(guardTag(tag, index))
                .key(0, pose().scale(0.1f).alpha(0f))
                .key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.model(sl, star, guardSpot(player, index, age), 0f, 0f, 0f);
    }

    /** Every tick while the guard holds: the stars move round the caster ('age' = ticks since it started). */
    public static void starGuardTick(ServerLevel sl, LivingEntity player, String tag, int age) {
        for (int i = 0; i < 4; i++) {
            ParticleShapes.moveModel(sl, guardTag(tag, i), guardSpot(player, i, age), 0f, 0f, 0f);
        }
        if (age % 10 == 0) ParticleShapes.spawn(sl, STAR_MOTE, guardSpot(player, age / 10 % 4, age), Vec3.ZERO);
    }

    /** Star 'index' stopped a projectile at 'at': it bursts and is gone until starGuardBack. */
    public static void starGuardBlock(ServerLevel sl, LivingEntity player, String tag, int index, Vec3 at) {
        ParticleShapes.clearModels(sl, at, guardTag(tag, index));
        ParticleShapes.burst(sl, STAR_SPARK, at, 10, 0.05, 0.18);
        sound(sl, at, ModSounds.KNOWLEDGE_STAR_BLOCK, 1.0f, wobble(1.0f));
    }

    /** Star 'index' comes back (5 seconds later), for the 'ticksLeft' the guard still lasts. */
    public static void starGuardBack(ServerLevel sl, LivingEntity player, String tag, int index, int ticksLeft, int age) {
        if (ticksLeft <= 0) return;
        spawnGuardStar(sl, player, tag, index, ticksLeft, age);
        ParticleShapes.spawn(sl, SPARKLE, guardSpot(player, index, age), Vec3.ZERO);
    }

    /** A star strikes an enemy that came too close: a short bright line from the caster's side. */
    public static void starGuardStrike(ServerLevel sl, LivingEntity player, LivingEntity target) {
        Vec3 from = chestOf(player).add(chestOf(target).subtract(chestOf(player)).normalize().scale(GUARD_RADIUS));
        ParticleShapes.slashBetween(sl, STAR_TRAIL.lifetime(6).sweep(2), from, chestOf(target));
        ParticleShapes.burst(sl, STAR_SPARK, chestOf(target), 8, 0.05, 0.15);
        sound(sl, chestOf(target), ModSounds.KNOWLEDGE_STAR_BURST, 0.5f, wobble(1.5f));
    }

    public static void starGuardEnd(ServerLevel sl, LivingEntity player, String tag) {
        for (int i = 0; i < 4; i++) ParticleShapes.clearModels(sl, player.position(), guardTag(tag, i));
        ParticleShapes.ring(sl, STAR_MOTE, player.position().add(0, 1.1, 0), GUARD_RADIUS, 12, 0.04);
    }

    // =====================================================================================
    // Key 6: Retellings and Prophecy (Mysticologist, Clairvoyant)
    // =====================================================================================

    /**
     * Every Retelling begins the same way: the book of tales falls open before the teller and pages lift off it.
     * 'strength' is Obscurity's multiplier (1 = untold, down to 0.4): a story told too often looks paler.
     */
    public static void tale(ServerLevel sl, LivingEntity player, Tale tale, float strength) {
        int color = dimmed(tale.color, 0.4f + 0.6f * strength);
        ModelFx book = TALE_BOOK.scale(0.8f).lifetime(30).fade(4, 10)
                .key(0, pose().forward(0.7f).up(-0.2f).scale(0.3f).alpha(0f))
                .key(6, pose().forward(0.7f).up(0f).scale(1f).alpha(1f), Ease.OUT_BACK)
                .key(30, pose().forward(0.8f).up(0.3f).alpha(0f));
        ParticleShapes.modelOnTurning(sl, book, player, new Vec3(0, 1.0, 0), 0f, -20f, 0f);
        Vec3 over = chestOf(player).add(Spells.flatLook(player).scale(0.7)).add(0, 0.4, 0);
        ParticleShapes.cone(sl, PAGE, over, Spells.UP, 70, (int) (4 + 6 * strength), 0.04, 0.1);
        ParticleShapes.burst(sl, ParticleFx.of("fx/glow").color(color).endColor(color & 0x00FFFFFF).size(0.08f)
                .endSize(0.02f).lifetime(20, 10).gravity(-0.02f).friction(0.92f).glow(), over, 10, 0.02, 0.08);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_TALE, 1.0f, 0.9f + 0.1f * strength);
    }

    // ---------- Sleeping Beauty

    /** The tick the spindle has turned once and everyone near falls asleep. */
    public static final int SPINDLE_SLEEPS = 20;

    /** A golden spindle rises over the teller and turns; a ring of gold spreads 'radius' blocks. */
    public static void sleepingBeauty(ServerLevel sl, LivingEntity player, Vec3 center, double radius) {
        ModelFx spindle = SPINDLE.scale(0.8f).lifetime(SPINDLE_SLEEPS + 20).fade(4, 14).spin(12f)
                .key(0, pose().up(0f).alpha(0f))
                .key(10, pose().up(1.2f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOn(sl, spindle, player, new Vec3(0, player.getBbHeight() + 0.2, 0), 0f, 0f, 0f);
        Later.run(sl, SPINDLE_SLEEPS, () -> {
            SpellFx.steadyRing(sl, center, radius, 0xC0FFD86A, 12, 0);
            ParticleShapes.ring(sl, GOLD_MOTE, center.add(0, 0.6, 0), radius * 0.6, 24, 0.12);
        });
        sound(sl, center, ModSounds.KNOWLEDGE_SLEEPING_BEAUTY, 1.2f, 1.0f);
    }

    /** Someone asleep for 'ticks' (or only slowed, if a boss or a player): Zs drift up from them. */
    public static void asleep(ServerLevel sl, LivingEntity sleeper, int ticks, String tag) {
        SpellFx.circleUnder(sl, sleeper, Math.max(0.6, sleeper.getBbWidth()), 0x80FFD86A, ticks, tag);
        for (int t = 0; t < ticks; t += 14) {
            Later.run(sl, t, () -> {
                if (sleeper.isAlive()) ParticleShapes.spawn(sl, SLEEP_Z, headOf(sleeper), new Vec3(0.01, 0.03, 0));
            });
        }
    }

    /** The sleeper woke (it was hurt): a jolt of gold. */
    public static void awaken(ServerLevel sl, LivingEntity sleeper, String tag) {
        ParticleShapes.clearModels(sl, sleeper, tag);
        ParticleShapes.burst(sl, GOLD_MOTE, headOf(sleeper), 10, 0.05, 0.15);
        sound(sl, sleeper.position(), ModSounds.KNOWLEDGE_AWAKEN, 0.8f, wobble(1.0f));
    }

    // ---------- Avalon

    /** The tick the isle has risen out of the mist. */
    public static final int AVALON_RISEN = 20;

    /** The Isle of the Blessed rises: its seal spreads 'radius' wide, a ring of mist stands at the edge. */
    public static void avalon(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        floor(sl, AVALON_SEAL, center, radius, 0xB0CCF0C0, ticks, 0.4f, tag, 0);
        SpellFx.risingRings(sl, center, radius * 0.4, 2.0, 3, 0xA0CCF0C0);
        Later.run(sl, AVALON_RISEN, () -> avalonTick(sl, center, radius));
        sound(sl, center, ModSounds.KNOWLEDGE_AVALON, 1.2f, 1.0f);
    }

    /** Every second while the isle stands: mist at the edge, blossoms drifting inside. */
    public static void avalonTick(ServerLevel sl, Vec3 center, double radius) {
        ParticleShapes.ring(sl, MIST, center.add(0, 0.4, 0), radius, (int) Math.max(12, radius * 3), 0);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 6; i++) {
            double angle = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
            ParticleShapes.spawn(sl, PETAL, center.add(Math.cos(angle) * far, 2.0 + random.nextDouble(), Math.sin(angle) * far),
                    new Vec3(0.01, -0.01, 0.005));
        }
    }

    /** An ally the isle heals and cleanses: soft green-gold motes round them. */
    public static void avalonBlessed(ServerLevel sl, LivingEntity ally) {
        ParticleShapes.ring(sl, GOLD_MOTE.color(0xFFE8FFD8), ally.position().add(0, 0.3, 0), Math.max(0.4, ally.getBbWidth() * 0.6), 6, 0);
    }

    public static void avalonEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        ParticleShapes.ring(sl, MIST, center.add(0, 0.5, 0), radius * 0.8, 16, -0.03);
    }

    // ---------- Achilles' Heel

    /** The tick the arrow of the tale finds the heel. */
    public static final int ARROW_LANDS = 10;

    /** An arrow of light flies to the target's heel; the heel mark hangs over them for 'ticks'. */
    public static void achillesHeel(ServerLevel sl, LivingEntity player, LivingEntity target, int ticks, String tag) {
        ModelFx arrow = SPELL_BOLT.color(0xFFF0DC96).scale(0.7f).rotation(0f, 90f, 0f).lifetime(ARROW_LANDS + 2).fade(0, 2);
        SpellFx.homing(sl, arrow, chestOf(player), target, ARROW_LANDS);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_ACHILLES, 1.0f, wobble(1.0f));
        Later.run(sl, ARROW_LANDS, () -> {
            if (!target.isAlive()) return;
            ParticleShapes.burst(sl, GOLD_MOTE, target.position().add(0, 0.2, 0), 10, 0.04, 0.12);
            ModelFx mark = HEEL_MARK.color(0xE0F0DC96).scale(0.55f).lifetime(ticks - ARROW_LANDS).fade(4, 10).spin(3f).tag(tag)
                    .key(0, pose().scale(1.8f).alpha(0f))
                    .key(6, pose().scale(1f).alpha(1f), Ease.OUT);
            ParticleShapes.modelOn(sl, mark, target, new Vec3(0, target.getBbHeight() + 0.55, 0), 0f, 0f, 0f);
            SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth()), 0x90F0DC96, ticks - ARROW_LANDS, tag + "_circle");
        });
    }

    public static void achillesEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.clearModels(sl, target, tag + "_circle");
    }

    // ---------- Pandora's Box

    /** The tick the lid flies open (the misfortunes go out), and the tick hope comes out last. */
    public static final int BOX_OPENS = 14;
    public static final int HOPE_RISES = 40;

    /** A black box appears before the teller, its lid flies open and darkness spills out 'radius' blocks. */
    public static void pandorasBox(ServerLevel sl, LivingEntity player, double radius) {
        Vec3 at = player.position().add(Spells.flatLook(player).scale(1.4));
        float yaw = player.getYRot() + 180f;
        ModelFx box = BLACK_BOX.scale(0.9f).lifetime(HOPE_RISES + 16).fade(4, 10)
                .key(0, pose().up(0.6f).scale(0.2f).alpha(0f))
                .key(8, pose().up(0f).scale(1f).alpha(1f), Ease.OUT_BOUNCE);
        ModelFx lid = BLACK_BOX_LID.scale(0.9f).lifetime(HOPE_RISES + 16).fade(4, 10)
                .key(0, pose().up(0.6f).scale(0.2f).alpha(0f))
                .key(8, pose().up(0f).scale(1f).alpha(1f), Ease.OUT_BOUNCE)
                .key(BOX_OPENS - 2, pose().pitch(0f))
                .key(BOX_OPENS, pose().pitch(-105f), Ease.OUT_BACK);
        ParticleShapes.model(sl, box, at, yaw, 0f, 0f);
        ParticleShapes.model(sl, lid, at, yaw, 0f, 0f);
        sound(sl, at, ModSounds.KNOWLEDGE_PANDORA, 1.2f, 1.0f);
        Later.run(sl, BOX_OPENS, () -> {
            Vec3 mouth = at.add(0, 0.6, 0);
            ParticleShapes.cone(sl, INK_SMOKE, mouth, Spells.UP, 90, 24, 0.08, 0.25);
            for (Misfortune m : Misfortune.values()) {
                ParticleShapes.cone(sl, ParticleFx.of("fx/glow").color(m.color | 0xFF000000).endColor(m.color & 0x00FFFFFF)
                        .size(0.1f).endSize(0.03f).lifetime(30, 10).gravity(-0.01f).friction(0.95f).glow(),
                        mouth, Spells.UP, 120, 4, 0.1, 0.3);
            }
            SpellFx.shockRing(sl, at, radius, 0xB02A1A5A, 14, 0);
        });
        Later.run(sl, HOPE_RISES, () -> {
            Vec3 mouth = at.add(0, 0.7, 0);
            ModelFx light = LIGHT_BALL.color(0xFFFFF0B0).scale(0.4f).lifetime(30).fade(4, 14).spin(6f)
                    .key(0, pose().scale(0.2f))
                    .key(14, pose().up(1.2f).scale(1f), Ease.OUT);
            ParticleShapes.model(sl, light, mouth, 0f, 0f, 0f);
            ParticleShapes.burst(sl, GOLD_MOTE, mouth.add(0, 1.0, 0), 16, 0.03, 0.12);
        });
    }

    /** One misfortune settling on an enemy for 'ticks': a smudge of its color round them. */
    public static void misfortune(ServerLevel sl, LivingEntity enemy, Misfortune kind, int ticks, String tag) {
        SpellFx.circleUnder(sl, enemy, Math.max(0.6, enemy.getBbWidth()), withAlpha(kind.color, 0xA0), ticks, tag);
        ParticleShapes.burst(sl, INK_SMOKE.color(withAlpha(kind.color, 0xB0)), chestOf(enemy), 6, 0.02, 0.06);
    }

    /** Hope reaches an ally (Regeneration): warm motes round them. */
    public static void hope(ServerLevel sl, LivingEntity ally) {
        ParticleShapes.ring(sl, GOLD_MOTE, ally.position().add(0, 0.3, 0), Math.max(0.4, ally.getBbWidth() * 0.6), 8, 0);
        ParticleShapes.burst(sl, SPARKLE, chestOf(ally), 4, 0.02, 0.06);
    }

    // ---------- Foresight

    /** The future opens before the seer's eyes for 'ticks': the Hermit's eyes turn pale before the face. */
    public static void foresight(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        ModelFx eyes = HERMIT_EYES.color(0xC0F6F4FF).scale(0.5f).lifetime(Math.min(ticks, 30)).fade(4, 12).tag(tag)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOnHead(sl, eyes, player, -0.25, 0f, 0f, 0f);
        SpellFx.circleUnder(sl, player, 1.0, 0x70E8E8FF, ticks, tag + "_circle");
        SpellFx.risingRings(sl, player.position(), 1.0, 2.0, 3, 0x90F6F4FF);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_FORESIGHT, 1.0f, 1.0f);
    }

    /** A pale ghost of an attack that is coming, where it will land (Foresight, and the Divine Kingdom). */
    public static void foresightGhost(ServerLevel sl, Entity attacker, Vec3 at) {
        float yaw = attacker == null ? randomYaw(sl) : yawToward(attacker.position(), at);
        ParticleShapes.slash(sl, GHOST_ARC, at, yaw, 0f, 20f);
        ParticleShapes.spawn(sl, STAR_MOTE.color(0x80F6F4FF), at, Vec3.ZERO);
    }

    /** A blow foreseen and dodged: the seer slides two blocks aside, a pale afterimage stays. */
    public static void foresightDodge(ServerLevel sl, LivingEntity player, Vec3 from, Vec3 to) {
        ParticleShapes.slashBetween(sl, DASH.color(0x80F6F4FF).width(0.35f), from.add(0, 1.0, 0), to.add(0, 1.0, 0));
        ParticleShapes.burst(sl, STAR_MOTE, from.add(0, 1.0, 0), 8, 0.02, 0.08);
        sound(sl, to, ModSounds.KNOWLEDGE_DODGE, 1.0f, wobble(1.0f));
    }

    public static void foresightEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.clearModels(sl, player, tag + "_circle");
    }

    // ---------- Prophecy of Ruin

    /** The fate is pronounced: a wheel of fate hangs over the target and a red thread binds them to the seer. */
    public static void prophecy(ServerLevel sl, LivingEntity player, LivingEntity target, int seconds, String tag) {
        int ticks = seconds * 20;
        ModelFx wheel = FATE_WHEEL.color(0xE0E04050).scale(0.7f).lifetime(ticks + 6).fade(4, 6).tag(tag)
                .key(0, pose().scale(2.4f).alpha(0f))
                .key(8, pose().scale(1f).alpha(1f), Ease.OUT)
                .during(0, ticks, pose().spin(720f), Ease.IN);                   // turning faster as it nears zero
        ParticleShapes.modelOn(sl, wheel, target, new Vec3(0, target.getBbHeight() + 0.7, 0), 0f, 0f, 0f);
        SpellFx.circleUnder(sl, target, Math.max(0.7, target.getBbWidth() * 1.2), 0xA0E04050, ticks, tag + "_circle");
        SpellFx.tether(sl, RUIN_LINE, player, target, ticks);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_PROPHECY, 1.2f, 1.0f);
    }

    /** Each second of the countdown ('secondsLeft' = 3, 2, 1): a toll and a red flash on the doomed. */
    public static void prophecyTick(ServerLevel sl, LivingEntity target, int secondsLeft) {
        SpellFx.closingRing(sl, target.position(), 2.0 + secondsLeft * 0.6, 0xC0E04050, 10, 0);
        ParticleShapes.burst(sl, RUIN_MOTE, headOf(target), 4 + (4 - secondsLeft) * 3, 0.03, 0.1);
        sound(sl, target.position(), ModSounds.KNOWLEDGE_PROPHECY_TICK, 1.0f, 0.8f + (4 - secondsLeft) * 0.12f);
    }

    /** It comes true: a column of red-white ruin falls on the target. */
    public static void prophecyFulfilled(ServerLevel sl, LivingEntity target, String tag) {
        prophecyClear(sl, target, tag);
        float width = Math.max(1.2f, target.getBbWidth() * 2f);
        ModelFx column = LIGHT_COLUMN.color(0xF0FF6070).scale(width, 8f, width).lifetime(14).fade(0, 10)
                .key(0, pose().up(8f).alpha(0f))
                .key(4, pose().up(0f).alpha(1f), Ease.IN);
        ParticleShapes.model(sl, column, target.position(), 0f, 0f, 0f);
        Later.run(sl, 4, () -> {
            SpellFx.blast(sl, chestOf(target), 2.4, 0xFFE04050, RUIN_MOTE);
            flash(sl, FATE_WHEEL, chestOf(target), randomYaw(sl), 0xFFFFFFFF, 2.2f, 10);
        });
        sound(sl, target.position(), ModSounds.KNOWLEDGE_RUIN, 1.5f, 1.0f);
    }

    /** It failed (the target died or got away): the wheel cracks and the thread snaps. */
    public static void prophecyFails(ServerLevel sl, LivingEntity target, String tag) {
        prophecyClear(sl, target, tag);
        ParticleShapes.burst(sl, RUIN_MOTE.color(0xA0808080), headOf(target), 10, 0.03, 0.1);
    }

    /** Against the stronger, a tenth of the ruin comes back on the seer: a red flash on them. */
    public static void prophecyRebound(ServerLevel sl, LivingEntity player) {
        ParticleShapes.burst(sl, RUIN_MOTE, chestOf(player), 10, 0.04, 0.12);
        SpellFx.shockRing(sl, player.position(), 1.5, 0xA0E04050, 6, 0);
    }

    private static void prophecyClear(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.clearModels(sl, target, tag + "_circle");
    }

    // =====================================================================================
    // Key 7: the Knowledge wheel (Sage, Knowledge Emperor, Hermit)
    // =====================================================================================

    // ---------- Informatization

    /** The body becomes a stream of information for 'ticks': panels of streaming glyphs wrap it. */
    public static void informatization(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        for (int i = 0; i < 4; i++) {
            ModelFx panel = DATA_STREAM.color(0xC0C070E8).scale(0.7f, 2.1f, 1f).lifetime(ticks).fade(4, 8).tag(tag)
                    .key(0, pose().forward(0.36f).scale(0.2f).alpha(0f))
                    .key(6, pose().forward(0.36f).scale(1f).alpha(1f), Ease.OUT);
            ParticleShapes.modelOnTurning(sl, panel, player, Vec3.ZERO, i * 90f, 0f, 0f);
        }
        glyphBurst(sl, chestOf(player), 14, 0.04, 0.14);
        ParticleShapes.burst(sl, TEXT, chestOf(player), 10, 0.03, 0.1);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_INFORMATIZE, 1.0f, 1.0f);
    }

    /** Every few ticks while it lasts: text trails off the moving stream. */
    public static void informatizationTick(ServerLevel sl, LivingEntity player) {
        ParticleShapes.spawn(sl, TEXT, player.position().add(0, 0.3 + sl.getRandom().nextDouble() * 1.4, 0),
                Spells.flatLook(player).scale(-0.05));
        ParticleShapes.spawn(sl, anyGlyph(), player.position().add(0, 0.2, 0), new Vec3(0, 0.02, 0));
    }

    /** The false copy left behind: a standing figure of streaming glyphs at 'at', facing 'yaw', for 'ticks'. */
    public static void decoy(ServerLevel sl, Vec3 at, float yaw, int ticks, String tag) {
        for (int i = 0; i < 4; i++) {
            ModelFx panel = DATA_STREAM.color(0xA0E8C4FF).scale(0.6f, 1.9f, 1f).lifetime(ticks).fade(0, 6).tag(tag)
                    .key(0, pose().forward(0.3f));
            ParticleShapes.model(sl, panel, at, yaw + i * 90f, 0f, 0f);
        }
        ModelFx eyes = HERMIT_EYES.color(0xA0C070E8).scale(0.4f).lifetime(ticks).fade(0, 6).tag(tag);
        ParticleShapes.model(sl, eyes, at.add(0, 1.6, 0), yaw, 0f, 0f);
    }

    /** The copy is struck and bursts into text (the ability does the 40 damage). */
    public static void decoyBurst(ServerLevel sl, Vec3 at, String tag) {
        ParticleShapes.clearModels(sl, at, tag);
        Vec3 chest = at.add(0, 1.0, 0);
        glyphBurst(sl, chest, 24, 0.08, 0.3);
        ParticleShapes.burst(sl, PAGE, chest, 14, 0.06, 0.2);
        SpellFx.blast(sl, chest, 3.0, 0xFFC070E8, null);
        sound(sl, at, ModSounds.KNOWLEDGE_DECOY_BURST, 1.2f, wobble(1.0f));
    }

    /** The stream takes shape again; 'released' = the absorbed harm is let out round it (a ring of text). */
    public static void informatizationEnd(ServerLevel sl, LivingEntity player, String tag, boolean released) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.burst(sl, TEXT, chestOf(player), 12, 0.04, 0.12);
        if (released) {
            SpellFx.shockRing(sl, player.position(), 4.0, 0xD0C070E8, 10, 0);
            glyphBurst(sl, chestOf(player), 16, 0.1, 0.3);
        }
        sound(sl, player.position(), ModSounds.KNOWLEDGE_REFORM, 1.0f, 1.0f);
    }

    // ---------- Erase

    /** The tick the erasing stroke reaches the target. */
    public static final int ERASE_LANDS = 3;

    /** A white stroke from the Sage strikes the target out: its symbols break apart and fall. */
    public static void erase(ServerLevel sl, LivingEntity player, LivingEntity target) {
        Vec3 hand = SpellFx.handOf(player, sl);
        if (hand == null) hand = chestOf(player);
        ParticleShapes.slashBetween(sl, ERASE_LINE, hand, chestOf(target));
        sound(sl, target.position(), ModSounds.KNOWLEDGE_ERASE, 1.2f, 1.0f);
        Later.run(sl, ERASE_LANDS, () -> {
            if (!target.isAlive()) return;
            Vec3 at = chestOf(target);
            flash(sl, ERASE_SIGIL, at, yawToward(at, player.position()), 0xF0FFFFFF, 1.6f, 12);
            for (int i = 0; i < 12; i++) {
                ParticleShapes.spawn(sl, anyGlyph().gravity(0.04f), at.add((i % 4 - 1.5) * 0.25, 0.4, 0), new Vec3(0, 0.02, 0));
            }
            ParticleShapes.burst(sl, STAR_MOTE.color(0xFFFFFFFF), at, 8, 0.05, 0.15);
        });
    }

    /** Something summoned (a marionette, a projection, a decoy) is deleted outright: it folds into nothing. */
    public static void deleted(ServerLevel sl, Entity thing) {
        Vec3 at = thing.position().add(0, thing.getBbHeight() * 0.5, 0);
        SpellFx.closingRing(sl, thing.position(), 2.0, 0xF0FFFFFF, 6, 0);
        flash(sl, ERASE_SIGIL, at, randomYaw(sl), 0xF0FFFFFF, Math.max(1.2f, thing.getBbHeight()), 10);
        glyphBurst(sl, at, 10, 0.02, 0.06);
    }

    // ---------- Knowledge Overload

    /** The tick the flood reaches the mind: its thoughts stop from here. */
    public static final int OVERLOAD_REACHES = 12;

    /**
     * Ancient symbols fill the Emperor's eyes (a ring of glyphs before them) and pour into the target's head; at
     * OVERLOAD_REACHES a ring of glyphs locks round its head for 'ticks' (the hold). Do overloadHit when it takes the
     * damage, mindBurst if the mind detonates.
     */
    public static void knowledgeOverload(ServerLevel sl, LivingEntity player, LivingEntity target, int ticks, String tag) {
        ModelFx eyes = GLYPH_RING.color(0xE0E8C4FF).scale(0.45f).lifetime(OVERLOAD_REACHES + 10).fade(2, 8).spin(18f)
                .key(0, pose().pitch(90f).scale(0.2f).alpha(0f))
                .key(4, pose().pitch(90f).scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOnHead(sl, eyes, player, -0.25, 0f, 0f, 0f);
        flood(sl, player.getEyePosition(), target);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_OVERLOAD, 1.2f, 1.0f);
        Later.run(sl, OVERLOAD_REACHES, () -> {
            if (!target.isAlive()) return;
            overloaded(sl, target, ticks, tag);
        });
    }

    /** The Hermit's form: every enemy within reach at once (one flood each, one sound). */
    public static void overloadAll(ServerLevel sl, LivingEntity player, List<LivingEntity> targets, int ticks, String tag) {
        ModelFx crown = GLYPH_RING.color(0xE0E8C4FF).scale(3f).lifetime(OVERLOAD_REACHES + 10).fade(2, 8).spin(10f);
        ParticleShapes.modelOn(sl, crown, player, new Vec3(0, player.getBbHeight() + 0.6, 0), 0f, 0f, 0f);
        SpellFx.steadyRing(sl, player.position(), 16.0, 0xC0C070E8, OVERLOAD_REACHES, 0);
        for (LivingEntity target : targets) {
            flood(sl, headOf(player).add(0, 0.6, 0), target);
            Later.run(sl, OVERLOAD_REACHES, () -> {
                if (target.isAlive()) overloaded(sl, target, ticks, tag);
            });
        }
        sound(sl, player.position(), ModSounds.KNOWLEDGE_OVERLOAD, 1.5f, 0.9f);
    }

    /** A stream of glyphs from 'from' into the target's head over OVERLOAD_REACHES ticks. */
    private static void flood(ServerLevel sl, Vec3 from, LivingEntity target) {
        for (int t = 0; t < OVERLOAD_REACHES; t += 2) {
            int step = t;
            Later.run(sl, t, () -> {
                if (!target.isAlive()) return;
                Vec3 head = headOf(target).add(0, -0.2, 0);
                Vec3 way = head.subtract(from);
                double part = step / (double) OVERLOAD_REACHES;
                for (int i = 0; i < 3; i++) {
                    Vec3 at = from.add(way.scale(Math.min(1.0, part + i * 0.08)))
                            .add(ParticleShapes.randomDirectionInCone(sl.getRandom(), Spells.UP, 360).scale(0.2));
                    ParticleShapes.spawn(sl, anyGlyph(), at, way.normalize().scale(0.25));
                }
            });
        }
    }

    /** The mind is held for 'ticks': a ring of glyphs locks round the head, symbols swirl into it. */
    private static void overloaded(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        ModelFx ring = GLYPH_RING.color(0xE0C070E8).scale(Math.max(0.8f, target.getBbWidth() * 1.3f)).lifetime(ticks)
                .fade(2, 6).spin(14f).tag(tag)
                .key(0, pose().scale(2f).alpha(0f))
                .key(4, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOn(sl, ring, target, new Vec3(0, target.getBbHeight() - 0.2, 0), 0f, 0f, 0f);
        ParticleShapes.spiral(sl, GLYPHS[0], headOf(target), 3, 12, 0.2, 0.8, 1.0, -0.06);
    }

    /** The flood strikes (the damage): a white flash in the mind. */
    public static void overloadHit(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        Vec3 at = headOf(target).add(0, -0.3, 0);
        flash(sl, HERMIT_EYES, at, randomYaw(sl), 0xF0F6F4FF, 1.2f, 10);
        glyphBurst(sl, at, 14, 0.06, 0.2);
        ParticleShapes.burst(sl, SPARKLE, at, 8, 0.05, 0.15);
    }

    /** At 100% Study a mind detonates: a burst of light and pages 'radius' blocks round it. */
    public static void mindBurst(ServerLevel sl, LivingEntity target, double radius) {
        Vec3 at = headOf(target).add(0, -0.3, 0);
        SpellFx.blast(sl, at, radius, 0xFFE8C4FF, STAR_SPARK);
        ParticleShapes.burst(sl, PAGE, at, 20, 0.08, 0.3);
        glyphBurst(sl, at, 20, 0.1, 0.35);
        sound(sl, at, ModSounds.KNOWLEDGE_MIND_BURST, 1.3f, wobble(1.0f));
    }

    // ---------- Lance of Knowledge

    /** The tick the lance leaves the hand (start the Shot then). */
    public static final int LANCE_THROWN = 16;

    /**
     * Knowledge condenses into a white spear above the Emperor's shoulder (count 3 = the Hermit's fan), pointing
     * the way they face, and is thrown at LANCE_THROWN. Fly LANCE in a Shot after that.
     */
    public static void lanceForm(ServerLevel sl, LivingEntity player, int count) {
        for (int i = 0; i < count; i++) {
            float side = count <= 1 ? 0f : (i - (count - 1) / 2f) * 0.7f;
            // tipped forward 90 degrees, so its point looks the way the body faces; in this frame forward(...) is
            // down, so forward(-0.3) lifts it over the shoulder
            ModelFx forming = LANCE.scale(0.7f).lifetime(LANCE_THROWN).fade(0, 1)
                    .key(0, pose().right(0.5f + side).forward(-0.3f).scale(0.1f).alpha(0f))
                    .key(LANCE_THROWN - 4, pose().right(0.5f + side).forward(-0.3f).scale(1f).alpha(1f), Ease.OUT);
            ParticleShapes.modelOnTurning(sl, forming, player, new Vec3(0, 1.5, 0), 0f, 90f, 0f);
        }
        Vec3 shoulder = player.getEyePosition().add(Spells.rightOf(player).scale(0.5)).add(0, 0.3, 0);
        ParticleShapes.spiral(sl, GLYPHS[1], shoulder, 3, 18, 0.2, 1.4, 1.0, -0.06);
        ParticleShapes.burst(sl, STAR_MOTE, shoulder, 10, 0.02, 0.06);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_LANCE_FORM, 1.0f, wobble(1.0f));
    }

    /** Every tick the lance flies: white motes and glyphs fall off it. */
    public static void lanceTrail(ServerLevel sl, Vec3 at, Vec3 direction) {
        ParticleShapes.spawn(sl, STAR_MOTE, at, direction.normalize().scale(-0.05));
        if (sl.getRandom().nextInt(2) == 0) ParticleShapes.spawn(sl, anyGlyph(), at, Vec3.ZERO);
    }

    /** It bursts: white light and a storm of glyphs 'radius' blocks round (knowledge forced into minds). */
    public static void lanceImpact(ServerLevel sl, Vec3 at, double radius) {
        SpellFx.blast(sl, at, radius * 0.8, 0xFFF2F0FF, STAR_SPARK);
        SpellFx.shockRing(sl, at.add(0, -0.4, 0), radius * 1.1, 0xD0E8C4FF, 10, 0);
        glyphBurst(sl, at, 30, 0.1, 0.4);
        flash(sl, HERMIT_EYES, at.add(0, 1.0, 0), randomYaw(sl), 0xE0F6F4FF, 2.4f, 14);
        sound(sl, at, ModSounds.KNOWLEDGE_LANCE_IMPACT, 1.5f, wobble(1.0f));
    }

    // ---------- Condense Knowledge

    /** The tick the gathered light reaches the Emperor's hands and is taken in. */
    public static final int CONDENSED = 30;

    /** Light streams from every enemy whose Study is taken ('sources') into a ball between the clasped hands. */
    public static void condenseKnowledge(ServerLevel sl, LivingEntity player, List<LivingEntity> sources) {
        ModelFx ball = LIGHT_BALL.color(0xFFFFF0B0).scale(0.5f).lifetime(CONDENSED + 10).fade(4, 8).spin(8f)
                .key(0, pose().forward(0.6f).scale(0.1f))
                .key(CONDENSED, pose().forward(0.6f).scale(1f), Ease.OUT)
                .key(CONDENSED + 10, pose().forward(0.2f).scale(0.2f).alpha(0f), Ease.IN);
        ParticleShapes.modelOnTurning(sl, ball, player, new Vec3(0, 1.1, 0), 0f, 0f, 0f);
        for (LivingEntity source : sources) {
            for (int t = 0; t < CONDENSED - 4; t += 4) {
                Later.run(sl, t, () -> {
                    if (!source.isAlive() || !player.isAlive()) return;
                    Vec3 from = chestOf(source);
                    Vec3 to = player.position().add(0, 1.1, 0).add(Spells.flatLook(player).scale(0.6));
                    ParticleShapes.spawn(sl, GOLD_MOTE, from, to.subtract(from).scale(0.06));
                });
            }
            ParticleShapes.burst(sl, ORCHID_MOTE, chestOf(source), 6, 0.02, 0.06);
        }
        sound(sl, player.position(), ModSounds.KNOWLEDGE_CONDENSE, 1.0f, 1.0f);
        Later.run(sl, CONDENSED, () -> {
            if (!player.isAlive()) return;
            SpellFx.risingRings(sl, player.position(), 1.0, 2.0, 3, 0xC0F0DC96);
            ParticleShapes.burst(sl, GOLD_MOTE, chestOf(player), 14, 0.03, 0.1);
        });
    }

    /** With no Study around it works the other way: the hand glows until the next hit lands ('ticks' at most). */
    public static void condenseInverse(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        SpellFx.circleUnder(sl, player, 0.8, 0x90F0DC96, ticks, tag);
        Vec3 hand = SpellFx.handOf(player, sl);
        if (hand != null) ParticleShapes.burst(sl, GOLD_MOTE, hand, 8, 0.02, 0.06);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_CONDENSE, 0.6f, 1.4f);
    }

    // ---------- Embodiment of Knowledge

    /** The Hermit unravels into their own information and is gone (the hiding is the ability's). */
    public static void embodiment(ServerLevel sl, LivingEntity player) {
        for (int i = 0; i < 4; i++) {
            ModelFx panel = DATA_STREAM.color(0xA08A70D0).scale(0.7f, 2.1f, 1f).lifetime(16).fade(0, 12)
                    .key(0, pose().forward(0.36f))
                    .key(16, pose().forward(0.9f).up(0.6f).alpha(0f), Ease.IN);
            ParticleShapes.model(sl, panel, player.position(), player.getYRot() + i * 90f, 0f, 0f);
        }
        ParticleShapes.burst(sl, TEXT, chestOf(player), 16, 0.03, 0.12);
        ParticleShapes.burst(sl, INK_SMOKE, chestOf(player), 6, 0.01, 0.04);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_EMBODIMENT, 1.0f, 1.0f);
    }

    /** The Hermit is seen again: lines of text fold back into a body. */
    public static void embodimentEnd(ServerLevel sl, LivingEntity player) {
        SpellFx.closingRing(sl, player.position(), 1.8, 0xC08A70D0, 8, 0);
        ParticleShapes.burst(sl, TEXT, chestOf(player), 10, 0.02, 0.06);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_REFORM, 0.7f, 0.9f);
    }

    // ---------- Indoctrination

    /** A pulse of knowledge over 'radius': an open book over the Hermit, a ring of orchid light racing out. */
    public static void indoctrination(ServerLevel sl, LivingEntity player, double radius) {
        ModelFx book = TALE_BOOK.scale(1.2f).lifetime(30).fade(4, 12)
                .key(0, pose().up(0f).alpha(0f))
                .key(8, pose().up(0.8f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOn(sl, book, player, new Vec3(0, player.getBbHeight() + 0.2, 0), player.getYRot(), 0f, 0f);
        ModelFx column = LIGHT_COLUMN.color(0xA0C070E8).scale(1.6f, 6f, 1.6f).lifetime(24).fade(0, 14);
        ParticleShapes.model(sl, column, player.position(), 0f, 0f, 0f);
        SpellFx.steadyRing(sl, player.position(), radius, 0xD0C070E8, 16, 0);
        SpellFx.steadyRing(sl, player.position(), radius * 0.7, 0x90F6F4FF, 16, 3);
        glyphBurst(sl, headOf(player).add(0, 0.8, 0), 20, 0.1, 0.3);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_INDOCTRINATION, 1.5f, 1.0f);
    }

    /** An ally Hermit receives the good kind for 'ticks': a golden circle and light rising. */
    public static void enlightened(ServerLevel sl, LivingEntity ally, int ticks, String tag) {
        SpellFx.circleUnder(sl, ally, Math.max(0.6, ally.getBbWidth()), 0x90F0DC96, ticks, tag);
        ParticleShapes.burst(sl, GOLD_MOTE, chestOf(ally), 10, 0.03, 0.08);
    }

    /** An enemy receives the bad kind for 'ticks': glyphs swirl round its head (its Study is set to 100%). */
    public static void indoctrinated(ServerLevel sl, LivingEntity enemy, int ticks, String tag) {
        ModelFx ring = GLYPH_RING.color(0xB08A70D0).scale(Math.max(0.7f, enemy.getBbWidth() * 1.2f)).lifetime(ticks)
                .fade(4, 8).spin(-8f).tag(tag);
        ParticleShapes.modelOn(sl, ring, enemy, new Vec3(0, enemy.getBbHeight() + 0.15, 0), 0f, 0f, 0f);
        glyphBurst(sl, headOf(enemy), 6, 0.02, 0.06);
    }

    // ---------- Divine Kingdom: the Knowledge Moor

    /** The tick the library has risen round the Hermit. */
    public static final int KINGDOM_RISEN = 30;

    /**
     * The Knowledge Moor opens round the Hermit for 'ticks': its floor spreads 'radius' wide, a ring of shelves of
     * orchid light rises at its edge (two rows high), the Hermit's eyes open in the sky.
     */
    public static void divineKingdom(ServerLevel sl, LivingEntity player, double radius, int ticks, String tag) {
        Vec3 center = player.position();
        floor(sl, KNOWLEDGE_MOOR, center, radius, 0xB0C070E8, ticks, 0.3f, tag, 0);
        floor(sl, GLYPH_RING, center, radius * 0.45, 0x90E8C4FF, ticks, -0.8f, tag, 10);
        double wall = Math.min(radius, 20.0);                                   // the shelves stand inside 20 blocks
        int count = Math.max(12, Math.min(40, (int) Math.round(2 * Math.PI * wall / 2.0)));
        float width = (float) (2 * wall * Math.sin(Math.PI / count) * 1.05);
        float start = randomYaw(sl);
        for (int row = 0; row < 2; row++) {
            for (int k = 0; k < count; k++) {
                float azimuth = start + k * 360f / count;
                Vec3 at = center.add(ahead(azimuth).scale(wall)).add(0, row * 2.0, 0);
                int delay = 4 + row * 8 + (k % 4);
                ModelFx shelf = LIBRARY_SHELF.color(0xB0C070E8).scale(width, 2f, 1f).delay(delay).lifetime(ticks)
                        .fade(4, 16).tag(tag)
                        .key(0, pose().up(-2f).alpha(0f))
                        .key(10, pose().up(0f).alpha(1f), Ease.OUT);
                ParticleShapes.model(sl, shelf, at, azimuth + 180f, 0f, 0f);
            }
        }
        ModelFx eyes = HERMIT_EYES.color(0xC0C070E8).scale((float) Math.min(10.0, radius * 0.4)).delay(KINGDOM_RISEN - 10)
                .lifetime(ticks).fade(16, 16).spin(0.4f).tag(tag);
        ParticleShapes.model(sl, eyes, center.add(0, Math.min(radius, 20.0) * 0.6 + 6, 0), 0f, -90f, 0f);
        SpellFx.steadyRing(sl, center, radius, 0xD0C070E8, KINGDOM_RISEN, 0);
        sound(sl, center, ModSounds.KNOWLEDGE_KINGDOM, 2.0f, 1.0f);
        Later.run(sl, KINGDOM_RISEN, () -> kingdomTick(sl, center, radius));
    }

    /** Every second while the Moor stands: pages drift down, glyphs rise. */
    public static void kingdomTick(ServerLevel sl, Vec3 center, double radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 14; i++) {
            double angle = random.nextDouble() * Math.PI * 2, far = Math.sqrt(random.nextDouble()) * radius;
            Vec3 at = center.add(Math.cos(angle) * far, 0, Math.sin(angle) * far);
            if (i % 2 == 0) ParticleShapes.spawn(sl, PAGE, at.add(0, 4 + random.nextDouble() * 3, 0), new Vec3(0.01, -0.02, 0.01));
            else ParticleShapes.spawn(sl, anyGlyph(), at.add(0, 0.3, 0), new Vec3(0, 0.04, 0));
        }
    }

    /** Someone hidden is forced out into the open by the Moor: a flash of the eyes over them. */
    public static void kingdomForcedOut(ServerLevel sl, LivingEntity hidden) {
        flash(sl, HERMIT_EYES, headOf(hidden).add(0, 0.6, 0), randomYaw(sl), 0xF0C070E8, 1.0f, 16);
        SpellFx.closingRing(sl, hidden.position(), 2.0, 0xE0C070E8, 8, 0);
        ParticleShapes.burst(sl, STAR_MOTE, chestOf(hidden), 12, 0.04, 0.12);
        sound(sl, hidden.position(), ModSounds.KNOWLEDGE_PRY, 0.8f, 1.3f);
    }

    /** An enemy ability about to land inside the Moor: its ghost a second before (the same as Foresight's). */
    public static void kingdomGhost(ServerLevel sl, Entity caster, Vec3 at) {
        foresightGhost(sl, caster, at);
    }

    public static void kingdomEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        SpellFx.closingRing(sl, center, Math.min(radius, 20.0), 0xC0C070E8, 14, 0);
        ParticleShapes.burst(sl, PAGE, center.add(0, 1.5, 0), 20, 0.05, 0.2);
    }

    // =====================================================================================
    // Passives
    // =====================================================================================

    /** Spiritual Intuition: something started hunting the Hermit from out of sight. A chime only they hear. */
    public static void intuition(ServerPlayer hunted) {
        soundFor(hunted, hunted.getEyePosition(), ModSounds.KNOWLEDGE_CHIME, 0.6f, 1.0f);
    }

    /** Spiritual Intuition: a hidden thing within 6 blocks shows as a faint outline, only to the Hermit. */
    public static void intuitionOutline(ServerPlayer viewer, Entity hidden) {
        double w = Math.max(0.3, hidden.getBbWidth() * 0.55), h = hidden.getBbHeight();
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6;
            Vec3 foot = hidden.position().add(Math.cos(a) * w, 0.1, Math.sin(a) * w);
            ParticleShapes.spawnFor(viewer, STAR_MOTE.color(0x90E8C4FF), foot.add(0, h * (i % 3) / 2.5, 0), Vec3.ZERO);
        }
    }

    /** Whisper of the Hidden Sage: the next ability is free. Only the Hermit hears it and sees motes round the head. */
    public static void whisper(ServerPlayer player) {
        soundFor(player, player.getEyePosition(), ModSounds.KNOWLEDGE_WHISPER, 0.7f, 1.0f);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            ParticleShapes.spawnFor(player, ORCHID_MOTE, player.getEyePosition().add(Math.cos(a) * 0.6, 0.2, Math.sin(a) * 0.6),
                    new Vec3(-Math.cos(a) * 0.02, 0.01, -Math.sin(a) * 0.02));
        }
        statusForever(player, Status.WHISPER);
    }

    /** Clairaudience: a concealed enemy acted; a chime from its direction, only the Hermit hears it. */
    public static void clairaudience(ServerPlayer viewer, Vec3 at) {
        soundFor(viewer, at, ModSounds.KNOWLEDGE_CHIME, 0.8f, 1.6f);
    }

    /** Information Skin: harm without an attacker slides off: a flicker of text over the body. */
    public static void informationSkin(ServerLevel sl, LivingEntity player) {
        ParticleShapes.burst(sl, TEXT, chestOf(player), 3, 0.02, 0.05);
    }

    /** Starlight: at night under open sky, faint stars drift down onto the Constellations Master now and then. */
    public static void starlight(ServerLevel sl, LivingEntity player) {
        Vec3 above = player.position().add(sl.getRandom().nextDouble() - 0.5, 3.0, sl.getRandom().nextDouble() - 0.5);
        ParticleShapes.spawn(sl, STAR_MOTE.gravity(0.01f).lifetime(40, 10), above, new Vec3(0, -0.04, 0));
    }

    /** Law of Three: the third cast in a row is empowered: three points of gold join over the head. */
    public static void lawOfThree(ServerLevel sl, LivingEntity player) {
        Vec3 top = headOf(player).add(0, 0.5, 0);
        for (int i = 0; i < 3; i++) {
            double a = Math.PI * 2 * i / 3 + Math.PI / 2;
            Vec3 corner = top.add(Math.cos(a) * 0.45, 0, Math.sin(a) * 0.45);
            double b = Math.PI * 2 * (i + 1) / 3 + Math.PI / 2;
            Vec3 next = top.add(Math.cos(b) * 0.45, 0, Math.sin(b) * 0.45);
            ParticleShapes.line(sl, GOLD_MOTE, corner, next, 8);
        }
        SpellFx.risingRings(sl, player.position(), 0.9, 2.2, 3, 0xC0F0DC96);
        sound(sl, player.position(), ModSounds.KNOWLEDGE_LAW_OF_THREE, 0.9f, 1.0f);
    }

    // =====================================================================================
    // Melee combo (docs/KNOWLEDGE_EMPEROR.md, "Melee combo")
    // =====================================================================================

    private static final SlashFx COMBO_RING = SlashFx.arc("slash/streaks")
            .color(0xEEC070E8).headColor(0xF2F0E6FF).tailColor(0xA02A1A5A).core(0xFFF6F4FF)
            .radius(1.6f).arc(300f).width(0.5f).layers(2).spread(0.1f).lifetime(8).sweep(3).flat();
    private static final SlashFx COMBO_TILTED = SlashFx.arc("slash/dashed")
            .color(0xEEC070E8).headColor(0xF2F0E6FF).tailColor(0xA02A1A5A).core(0xFFF6F4FF)
            .radius(1.5f).arc(260f).width(0.4f).layers(2).spread(0.1f).lifetime(8).sweep(3).flat()
            .rotation(0f, 0f, 28f);
    private static final SlashFx COMBO_PAGE = SlashFx.arc("slash/edge")
            .color(0xEEC070E8).headColor(0xF2F0E6FF).tailColor(0xA02A1A5A).core(0xFFF6F4FF)
            .radius(1.0f).arc(150f).width(0.35f).lifetime(8).sweep(2);

    /** Which way the tilted ring leans next (it mirrors every time). */
    private static boolean comboMirror = false;

    /**
     * Hit 'step' (0, 1, 2) of the melee combo on 'target'. 0: a ring of drifting glyphs sweeps round the Scholar.
     * 1: a tilted ring of star points, mirrored each time. 2: either a circle of constellations winding out on the
     * ground, or crossed page-edge cuts on the target with an eye opening over it.
     */
    public static void combo(ServerLevel sl, LivingEntity player, LivingEntity target, int step) {
        Vec3 chest = chestOf(player);
        switch (step) {
            case 0 -> {
                ParticleShapes.slash(sl, COMBO_RING, chest, player.getYRot(), 0f, 0f);
                ParticleShapes.ring(sl, anyGlyph(), chest, 1.5, 8, 0.05);
                sound(sl, chest, ModSounds.KNOWLEDGE_GLYPH_SWING, 0.8f, wobble(1.0f));
            }
            case 1 -> {
                comboMirror = !comboMirror;
                SlashFx ring = comboMirror ? COMBO_TILTED.mirrored() : COMBO_TILTED;
                ParticleShapes.slash(sl, ring, chest, player.getYRot(), 0f, 0f);
                ParticleShapes.burst(sl, STAR_SPARK, chestOf(target), 6, 0.05, 0.15);
                sound(sl, chest, ModSounds.KNOWLEDGE_GLYPH_SWING, 0.8f, wobble(1.15f));
            }
            default -> {
                if (sl.getRandom().nextBoolean()) {
                    floor(sl, CONSTELLATIONS, player.position(), 2.2, 0xD0E8C4FF, 16, 6f, null, 0);
                    ParticleShapes.spiral(sl, STAR_MOTE, player.position().add(0, 0.2, 0), 6, 18, 0.4, 2.2, 0.7, 0.08);
                } else {
                    Vec3 at = chestOf(target);
                    float yaw = yawToward(player.position(), target.position());
                    ParticleShapes.slash(sl, COMBO_PAGE, at, yaw, 0f, 45f);
                    ParticleShapes.slash(sl, COMBO_PAGE, at, yaw, 0f, -45f);
                    ModelFx eye = STUDY_EYE_OPENING.scale(0.45f).lifetime(18).fade(0, 6)
                            .key(0, pose().frame(0f))
                            .key(10, pose().frame(2f), Ease.OUT);
                    ParticleShapes.model(sl, eye, headOf(target).add(0, 0.5, 0), yaw + 180f, 0f, 0f);
                }
                ParticleShapes.burst(sl, SPARKLE, chestOf(target), 6, 0.08, 0.18);
                sound(sl, chest, ModSounds.KNOWLEDGE_WEAK_POINT, 0.7f, wobble(1.2f));
            }
        }
    }

    // =====================================================================================
    // /knowledgefx: every display, to look at in game
    // =====================================================================================

    @FunctionalInterface
    private interface Show {
        void play(ServerLevel sl, Player viewer, LivingEntity target, Vec3 spot);
    }

    /** onViewer = it is shown on the viewer (worn), not on a spot or a target. */
    private record Case(String name, int ticks, boolean onViewer, Show show) {}

    private static String tagFor(String name) {
        return "ke_fx_" + name + "_" + UUID.randomUUID();
    }

    private static final List<Case> CASES = List.of(
            new Case("study", 80, false, (sl, v, t, spot) -> {
                if (!(v instanceof ServerPlayer viewer) || t == v) return;
                for (int i = 0; i < 4; i++) {
                    int percent = 15 + i * 30;
                    Later.run(sl, i * 18, () -> {
                        if (!t.isAlive()) return;
                        studyEye(viewer, t, Math.min(100, percent), 30);
                        studyGain(viewer, t);
                        if (percent >= 100) understood(viewer, t);
                    });
                }
                Later.run(sl, 78, () -> studyEyeHide(viewer, t));
            }),
            new Case("pry", 40, true, (sl, v, t, spot) -> {
                pry(sl, v, 30, false);
                if (v instanceof ServerPlayer viewer && t != v) Later.run(sl, 6, () -> pryRead(viewer, t));
                if (t != v) Later.run(sl, 10, () -> pryGlimpse(sl, t));
            }),
            new Case("secret_inquiry", 40, true, (sl, v, t, spot) -> pry(sl, v, 60, true)),
            new Case("dowse", 90, true, (sl, v, t, spot) -> {
                String tag = tagFor("dowse");
                dowse(sl, v, t == v ? null : t, 80, tag);
            }),
            new Case("dowse_still", 50, true, (sl, v, t, spot) -> dowse(sl, v, null, 40, tagFor("dowse"))),
            new Case("weak_point_strike", 30, false, (sl, v, t, spot) -> {
                weakPointStrike(sl, v, v.position().subtract(Spells.flatLook(v).scale(3)), t == v ? null : t);
            }),
            new Case("sidestep", 50, true, (sl, v, t, spot) -> {
                String tag = tagFor("side");
                sidestep(sl, v, 40, tag);
                Vec3 from = v.position();
                Later.run(sl, 20, () -> sidestepSlip(sl, v, from, from.add(Spells.rightOf(v).scale(4))));
            }),
            new Case("hex", 120, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("hex");
                hex(sl, v, t, 110, tag);
                Later.run(sl, 110, () -> hexEnd(sl, t, tag));
            }),
            new Case("spellbolts", 50, false, (sl, v, t, spot) -> {
                if (t == v) return;
                Element[] all = Element.values();
                Element element = all[ThreadLocalRandom.current().nextInt(all.length)];
                spellbolts(sl, v, List.of(t, t, t), element);
                for (int i = 0; i < 3; i++) Later.run(sl, BOLT_FLIGHT + i * BOLT_STEP, () -> boltHit(sl, t, element));
            }),
            new Case("knowledge_orbs", 50, false, (sl, v, t, spot) -> {
                if (t == v) return;
                knowledgeOrbs(sl, v, List.of(t, t, t, t, t));
                for (int i = 0; i < 5; i++) Later.run(sl, ORB_FLIGHT + i * BOLT_STEP, () -> orbHit(sl, t));
            }),
            new Case("pouch", 100, true, (sl, v, t, spot) -> {
                pouch(v, List.of(Scroll.FREEZING));
                scrollReady(sl, v, Scroll.FREEZING);
                Later.run(sl, 25, () -> {
                    pouch(v, List.of(Scroll.FREEZING, Scroll.PARALYSIS));
                    scrollReady(sl, v, Scroll.PARALYSIS);
                });
                Later.run(sl, 50, () -> pouch(v, List.of(Scroll.FREEZING, Scroll.PARALYSIS, Scroll.INSCRIBED)));
                Later.run(sl, 95, () -> pouchHide(v));
            }),
            new Case("read_freezing", 70, false, (sl, v, t, spot) -> {
                readScroll(sl, v, Scroll.FREEZING);
                if (t != v) Later.run(sl, READ_FIRES, () -> freezingRay(sl, t, 60, tagFor("frozen")));
            }),
            new Case("read_paralysis", 50, false, (sl, v, t, spot) -> {
                readScroll(sl, v, Scroll.PARALYSIS);
                if (t != v) Later.run(sl, READ_FIRES, () -> paralysisRay(sl, v, t, 30, tagFor("paralyzed")));
            }),
            new Case("inscribe", 50, true, (sl, v, t, spot) -> {
                inscribe(sl, v);
                Later.run(sl, INSCRIBED + 8, () -> scrollDropped(sl, v));
            }),
            new Case("starlight_cage", 120, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("cage");
                starlightCage(sl, v, t);
                Later.run(sl, CAGE_LANDS, () -> caged(sl, t, 90, tag));
                for (int i = 0; i < 3; i++) Later.run(sl, CAGE_LANDS + 20 + i * 15, () -> cageCrack(sl, t));
                Later.run(sl, CAGE_LANDS + 90, () -> cageBreak(sl, t, tag));
            }),
            new Case("stellar_self", 40, true, (sl, v, t, spot) -> stellarSelf(sl, v, v.position(), spot)),
            new Case("star_fall", 90, false, (sl, v, t, spot) -> {
                String tag = tagFor("sky");
                starFallSky(sl, spot, 80, tag);
                for (int i = 0; i < 5; i++) {
                    Vec3 ground = spot.add((i % 3 - 1) * 2.5, 0, (i / 3 - 0.5) * 3);
                    Later.run(sl, i * 8, () -> starFall(sl, ground, 3.0));
                }
            }),
            new Case("star_guard", 160, true, (sl, v, t, spot) -> {
                String tag = tagFor("guard");
                starGuard(sl, v, 150, tag);
                for (int age = 1; age < 150; age++) {
                    int a = age;
                    Later.run(sl, age, () -> {
                        if (v.isAlive()) starGuardTick(sl, v, tag, a);
                    });
                }
                Later.run(sl, 40, () -> starGuardBlock(sl, v, tag, 1, v.position().add(0, 1.1, 1.2)));
                Later.run(sl, 100, () -> starGuardBack(sl, v, tag, 1, 50, 100));
                if (t != v) Later.run(sl, 70, () -> starGuardStrike(sl, v, t));
                Later.run(sl, 150, () -> starGuardEnd(sl, v, tag));
            }),
            new Case("tale", 40, true, (sl, v, t, spot) -> tale(sl, v, Tale.AVALON, 1f)),
            new Case("tale_retold", 40, true, (sl, v, t, spot) -> tale(sl, v, Tale.AVALON, 0.4f)),
            new Case("sleeping_beauty", 140, false, (sl, v, t, spot) -> {
                String tag = tagFor("sleep");
                tale(sl, v, Tale.SLEEPING_BEAUTY, 1f);
                sleepingBeauty(sl, v, v.position(), 8);
                if (t != v) {
                    Later.run(sl, SPINDLE_SLEEPS, () -> asleep(sl, t, 100, tag));
                    Later.run(sl, SPINDLE_SLEEPS + 100, () -> awaken(sl, t, tag));
                }
            }),
            new Case("avalon", 160, false, (sl, v, t, spot) -> {
                String tag = tagFor("avalon");
                tale(sl, v, Tale.AVALON, 1f);
                avalon(sl, spot, 6, 150, tag);
                for (int i = 2; i < 7; i++) Later.run(sl, i * 20, () -> avalonTick(sl, spot, 6));
                for (int i = 1; i < 6; i++) Later.run(sl, AVALON_RISEN + i * 20, () -> avalonBlessed(sl, v));
                Later.run(sl, 150, () -> avalonEnd(sl, spot, 6, tag));
            }),
            new Case("achilles_heel", 110, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("heel");
                tale(sl, v, Tale.ACHILLES_HEEL, 1f);
                achillesHeel(sl, v, t, 100, tag);
                Later.run(sl, 100, () -> achillesEnd(sl, t, tag));
            }),
            new Case("pandoras_box", 80, false, (sl, v, t, spot) -> {
                tale(sl, v, Tale.PANDORAS_BOX, 1f);
                pandorasBox(sl, v, 10);
                if (t != v) {
                    Misfortune[] all = Misfortune.values();
                    Misfortune m = all[ThreadLocalRandom.current().nextInt(all.length)];
                    Later.run(sl, BOX_OPENS + 6, () -> misfortune(sl, t, m, 50, tagFor("misfortune")));
                }
                Later.run(sl, HOPE_RISES + 4, () -> hope(sl, v));
            }),
            new Case("foresight", 100, true, (sl, v, t, spot) -> {
                String tag = tagFor("foresight");
                foresight(sl, v, 90, tag);
                for (int i = 0; i < 3; i++) {
                    int n = i;
                    Later.run(sl, 15 + i * 22, () -> {
                        Vec3 at = chestOf(v).add(Spells.flatLook(v).scale(0.8));
                        foresightGhost(sl, t == v ? null : t, at);
                        Vec3 from = v.position();
                        Later.run(sl, 10, () -> foresightDodge(sl, v, from, from.add(Spells.rightOf(v).scale(n % 2 == 0 ? 2 : -2))));
                    });
                }
                Later.run(sl, 90, () -> foresightEnd(sl, v, tag));
            }),
            new Case("prophecy", 110, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("prophecy");
                prophecy(sl, v, t, 4, tag);
                for (int s = 3; s >= 1; s--) {
                    int left = s;
                    Later.run(sl, (4 - s) * 20, () -> {
                        if (t.isAlive()) prophecyTick(sl, t, left);
                    });
                }
                Later.run(sl, 80, () -> {
                    if (t.isAlive()) prophecyFulfilled(sl, t, tag);
                    else prophecyFails(sl, t, tag);
                });
            }),
            new Case("informatization", 140, true, (sl, v, t, spot) -> {
                String tag = tagFor("info");
                String decoyTag = tagFor("decoy");
                Vec3 at = v.position();
                float yaw = v.getYRot();
                informatization(sl, v, 120, tag);
                decoy(sl, at, yaw, 100, decoyTag);
                for (int i = 4; i < 120; i += 4) Later.run(sl, i, () -> informatizationTick(sl, v));
                Later.run(sl, 60, () -> decoyBurst(sl, at, decoyTag));
                Later.run(sl, 120, () -> informatizationEnd(sl, v, tag, true));
            }),
            new Case("erase", 40, false, (sl, v, t, spot) -> {
                if (t == v) return;
                erase(sl, v, t);
            }),
            new Case("deleted", 30, false, (sl, v, t, spot) -> {
                if (t == v) return;
                deleted(sl, t);
            }),
            new Case("knowledge_overload", 80, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("overload");
                knowledgeOverload(sl, v, t, 40, tag);
                Later.run(sl, OVERLOAD_REACHES + 40, () -> overloadHit(sl, t, tag));
            }),
            new Case("mind_burst", 40, false, (sl, v, t, spot) -> {
                if (t == v) return;
                mindBurst(sl, t, 4);
            }),
            new Case("overload_all", 80, true, (sl, v, t, spot) -> {
                String tag = tagFor("overload");
                List<LivingEntity> all = t == v ? List.of() : List.of(t);
                overloadAll(sl, v, all, 40, tag);
                for (LivingEntity one : all) Later.run(sl, OVERLOAD_REACHES + 40, () -> overloadHit(sl, one, tag));
            }),
            new Case("lance_of_knowledge", 50, false, (sl, v, t, spot) -> {
                lanceForm(sl, v, 1);
                Later.run(sl, LANCE_THROWN, () -> {
                    Vec3 from = v.getEyePosition().add(Spells.rightOf(v).scale(0.5));
                    Vec3 to = t == v ? spot.add(0, 1, 0) : chestOf(t);
                    Vec3 way = to.subtract(from);
                    int flight = Math.max(4, (int) (way.length() / 2.0));
                    ModelFx flying = LANCE.lifetime(flight + 1).fade(0, 1).key(flight, pose().up((float) way.length()));
                    ParticleShapes.modelAlong(sl, flying, from, way);
                    for (int i = 0; i < flight; i++) {
                        int step = i;
                        Later.run(sl, i, () -> lanceTrail(sl, from.add(way.scale(step / (double) flight)), way));
                    }
                    Later.run(sl, flight, () -> lanceImpact(sl, to, 5));
                });
            }),
            new Case("three_lances", 30, true, (sl, v, t, spot) -> lanceForm(sl, v, 3)),
            new Case("condense_knowledge", 50, true, (sl, v, t, spot) -> {
                condenseKnowledge(sl, v, t == v ? List.of() : List.of(t));
            }),
            new Case("condense_inverse", 40, true, (sl, v, t, spot) -> condenseInverse(sl, v, 40, tagFor("condense"))),
            new Case("embodiment", 70, true, (sl, v, t, spot) -> {
                embodiment(sl, v);
                Later.run(sl, 60, () -> embodimentEnd(sl, v));
            }),
            new Case("indoctrination", 80, true, (sl, v, t, spot) -> {
                indoctrination(sl, v, 24);
                Later.run(sl, 10, () -> enlightened(sl, v, 60, tagFor("enlightened")));
                if (t != v) Later.run(sl, 12, () -> indoctrinated(sl, t, 60, tagFor("indoctrinated")));
            }),
            new Case("divine_kingdom", 220, true, (sl, v, t, spot) -> {
                String tag = tagFor("kingdom");
                Vec3 center = v.position();
                divineKingdom(sl, v, 16, 210, tag);
                for (int i = 1; i < 9; i++) Later.run(sl, KINGDOM_RISEN + i * 20, () -> kingdomTick(sl, center, 16));
                if (t != v) Later.run(sl, KINGDOM_RISEN + 10, () -> kingdomForcedOut(sl, t));
                Later.run(sl, KINGDOM_RISEN + 40, () -> kingdomGhost(sl, t == v ? null : t, chestOf(v).add(Spells.flatLook(v))));
                Later.run(sl, 210, () -> kingdomEnd(sl, center, 16, tag));
            }),
            new Case("passives", 80, true, (sl, v, t, spot) -> {
                if (v instanceof ServerPlayer viewer) {
                    intuition(viewer);
                    if (t != v) intuitionOutline(viewer, t);
                    Later.run(sl, 20, () -> whisper(viewer));
                    Later.run(sl, 70, () -> statusEnd(viewer, Status.WHISPER));
                    Later.run(sl, 30, () -> clairaudience(viewer, spot));
                }
                Later.run(sl, 40, () -> informationSkin(sl, v));
                for (int i = 0; i < 6; i++) Later.run(sl, i * 6, () -> starlight(sl, v));
                Later.run(sl, 50, () -> lawOfThree(sl, v));
            }),
            new Case("combo", 50, false, (sl, v, t, spot) -> {
                if (t == v) return;
                for (int i = 0; i < 4; i++) {
                    int step = Math.min(2, i);
                    Later.run(sl, i * 12, () -> combo(sl, v, t, step));
                }
            }),
            new Case("statuses", 100, true, (sl, v, t, spot) -> {
                for (Status s : Status.values()) status(v, s, 90);
            }));

    /** The names /knowledgefx knows. */
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

    /** One display, 6 blocks in front of the viewer. False if there is none of that name. */
    public static boolean show(ServerLevel sl, Player viewer, String name) {
        for (Case c : CASES) {
            if (!c.name().equals(name)) continue;
            Vec3 spot = viewer.position().add(ahead(viewer.getYRot()).scale(6));
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

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("knowledgefx")
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
