package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order;

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
 * How the Hand Of Order (Justiciar) pathway looks and sounds: every key, both casts, every wheel entry, the growth at
 * higher Sequences (Intimidation, Confinement, the void Exile and its hurricane, Prohibition layers, the Brass Pillar),
 * the spoken words of the Verdicts and Decrees, the Convict, the Decree zones and their Violations, the Court of
 * Judgment and its clauses, the passives that show and the melee combo. Visual only: nothing here hurts, holds, hides,
 * heals or moves anyone. The ability code does that, at the ticks named by the constants below (..._LANDS, ..._RISEN,
 * ...), with Later.run. See docs/HAND_OF_ORDER.md.
 *
 * ---------- See them in game ----------
 *   /orderfx all              every display one after another (/orderfx all 3 = three times over)
 *   /orderfx court            just that one (press TAB after /orderfx for the names)
 * Displays that need a target use the nearest mob within 12 blocks, or you when there is none.
 *
 * ---------- Use them in an ability ----------
 *   Words   speak(sl, who, Word.X) shows a spoken word over 'who' (HALT, EXILE, DEATH, RELEASE ...) and says it
 *   Key 1   halt(sl, player, target) (lands at HALT_LANDS), halted(sl, target, ticks, tag), forgets(sl, mob)
 *           sneak: presence(sl, player, radius, intimidation), cowed(sl, mob, ticks, tag)
 *   Key 2   jurisdiction(sl, player, center, radius, ticks, tag) (claimed at JURISDICTION_CLAIMED),
 *               jurisdictionTick(sl, center, radius) every few seconds, jurisdictionEnter(owner, who) (only the owner
 *               sees and hears it), jurisdictionEnd(sl, center, radius, tag)
 *           sneak: recognition(owner, target, ticks, tag), recognitionTrack(owner, target) every second (footprints
 *               and the distance mark only the owner sees), recognitionEnd(owner, target, tag)
 *   Key 3   the wheel's pictures: WHEEL_PSYCHIC_LASHING, WHEEL_PSYCHIC_PIERCING, WHEEL_WHIP_OF_PAIN
 *           psychicLashing(sl, player, ticks, tag), lashingTick(sl, player) every few ticks, lashHit(sl, player,
 *               target), lashingEnd(sl, player, tag)
 *           psychicPiercing(sl, player, end, hits) (it strikes at once), piercingHit(sl, target, spirit)
 *           whipOfPain(sl, player, target) (lands at WHIP_LANDS), trembling(sl, target, ticks, tag), truthOut(sl,
 *               target) when its Concealment ends
 *   Key 4   the wheel's pictures: WHEEL_IMPRISON ... WHEEL_EXECUTION
 *           imprison(sl, player, target, ticks, tag) (walls closed at IMPRISON_LANDS), release(sl, target, tag),
 *               imprisonEnd(sl, target, tag); Confinement (4): confinement(sl, player, target, ticks, tag)
 *           exile(sl, player, target, to) (thrown at EXILE_LANDS); the void (4): exileVoid(sl, target, ticks, tag),
 *               exileReturn(sl, at, tag); the hurricane (3): exileHurricane(sl, center, radius)
 *           death(sl, player, from, target, wound) (the blow at DEATH_LANDS; wound = the Paladin's form)
 *           maintainSecrecy(sl, player, target, ticks, tag), secrecyEnd(sl, target, tag)
 *           deprivation(sl, player, target, count) (taken at DEPRIVED), deprived(sl, target, ticks, tag), deprivedEnd
 *           execution(sl, player, target, tag) (the fist falls at EXECUTION_LANDS), executionStrike(sl, player,
 *               target, slain), executionBroken(sl, player, target, tag)
 *   Key 5   the wheel's pictures: WHEEL_PROHIBITION_PASSAGE ... WHEEL_CONTRACT
 *           decree(sl, player, center, radius, ticks, Rule.X, tag) (in force at DECREE_RISEN), decreeLayer(sl,
 *               center, radius, layer, tag), decreeTick(sl, center, radius, rule), decreeEnd(sl, center, radius, tag)
 *           refused(sl, who) when a teleport, flight, dash, hiding or stand-in is refused; revealed(sl, who) when a
 *               Concealment is broken; violation(sl, who, count) for each Rule broken (count = how many so far)
 *           reality(sl, player, center, radius, ticks, tag) (Imperative: Reality), realityEnd
 *           contract(sl, player, target, ticks, tag) (signed at CONTRACT_SIGNED), contractViolation(sl, target,
 *               step), contractTear(sl, target, tag)
 *   Key 6   punishment(sl, player, target, ticks, tag), convictHit(sl, target), convictPunished(sl, target) (the
 *               Chaos Hunter's sure hit), punishmentEnd(sl, player, target, tag)
 *           sneak: swordOfJudgment(sl, player, from, target) (the stride at once, the cut at SWORD_LANDS),
 *               judgmentCleanse(sl, player)
 *   Key 7   the wheel's pictures: WHEEL_BALANCE ... WHEEL_COURT_OF_JUDGMENT
 *           balance(sl, player, target, ticks, tag) (settled at BALANCE_SETTLED), balanceEnd(sl, player, target, tag)
 *           divideBattlefield(sl, player, center, radius, ticks, tag) (risen at DIVIDE_RISEN), divideBlocked(sl, at),
 *               thrownOut(sl, who), divideEnd(sl, center, radius, tag)
 *           lawNoReturn(sl, player, center, radius, ticks, tag), revivalVoided(sl, who), puppetFrozen(sl, puppet)
 *           orderProxy(sl, player, ticks, tag), proxyTick(sl, player), proxyEnd(sl, player, tag)
 *           quellDisorder(sl, player, radius), quelled(sl, who), cleansed(sl, ally)
 *           court(sl, player, center, radius, ticks, tag) (in session at COURT_RISEN), clause(sl, center, radius,
 *               index) every 10 seconds, judged(sl, who, breach) for the 1st to 4th breach, adjourn(sl, center,
 *               radius, tag); on the screen courtClauses(player, written, ticks), courtClausesHide(player)
 *   Form    brassPillar(sl, player, ticks, tag) (the Imperative Mage's Mythical Creature form, a visual only)
 *   Passives: orderPresence(sl, player), intuition(owner), senseOrder(owner, at), disorderSense(owner, who),
 *             beyonderSense(owner, who), selfRestraint(sl, player)
 *   Melee: combo(sl, player, target, step) for hits 0, 1 and 2
 *   Every timed cast: status(player, Status.X, ticks) shows its picture and its clock; statusEnd(player, Status.X).
 *
 * The models are in models/model_fx/hand_of_order/ (made by tools/model_gen/hand_of_order/models.py; each JSON's
 * "credit" gives its size and pivot), the pictures in textures/gui/hand_of_order/ and
 * textures/gui/ability_menus/hand_of_order/ (icons.py), the particles in textures/particle/fx/ (particles.py), the
 * sounds in sounds/hand_of_order/ (sounds.py) and sounds/ModSounds (ORDER_...).
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class HandOfOrderFx {

    private HandOfOrderFx() {}

    private static ModelFx.Pose pose() {
        return ModelFx.pose();
    }

    // =====================================================================================
    // Colors (ARGB)
    // =====================================================================================

    /** The Pathway's own color: brass. */
    public static final int PATHWAY_COLOR = 0xFFC9A24B;
    public static final int BRASS = 0xFFC9A24B;
    public static final int BRASS_LIGHT = 0xFFF0D68C;
    public static final int WHITE_GOLD = 0xFFFFF3D0;
    public static final int LIGHTNING = 0xFFA8C8FF;
    public static final int IRON = 0xFF3E434D;
    public static final int IRON_LIGHT = 0xFF8A909C;
    public static final int VERDICT_RED = 0xFFB03030;
    public static final int DISORDER = 0xFF5A3A7A;
    public static final int GLASS = 0xFFDCECFF;

    /** The Rules a Decree can write, each with its color and the word it is spoken with (docs, "Decree"). */
    public enum Rule {
        PASSAGE(0xFFB0B8C8, Word.PROHIBITED, 1),
        CONCEALMENT(0xFFF0D68C, Word.PROHIBITED, 2),
        RESURRECTION(0xFFB03030, Word.NO_RETURN, 3),
        STAND_INS(0xFFC9A24B, Word.PROHIBITED, 4),
        HEALING(0xFF6AC07A, Word.PROHIBITED, 5),
        PROJECTILES(0xFF8A909C, Word.PROHIBITED, 6),
        ABILITIES(0xFF9A72C8, Word.PROHIBITED, 7),
        MARIONETTES(0xFF8A6AA8, Word.PROHIBITED, 0),
        SEALED(0xFFFFF3D0, Word.ORDER, 0),
        REALITY(0xFF8A909C, Word.REALITY, 0);

        public final int color;
        public final Word word;
        /** The Court's clause picture (textures/gui/hand_of_order/clause_N.png), 0 = none. */
        public final int clause;

        Rule(int color, Word word, int clause) {
            this.color = color;
            this.word = word;
            this.clause = clause;
        }
    }

    // =====================================================================================
    // The small particles everything shares
    // =====================================================================================

    private static final ParticleFx BRASS_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFFFE6A0).endColor(0x00A07A2A)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(24, 12).gravity(-0.02f).friction(0.92f).glow();
    private static final ParticleFx WHITE_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFFFFFFF).endColor(0x00FFF3D0)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(20, 10).gravity(-0.015f).friction(0.92f).glow();
    private static final ParticleFx BRASS_SPARK = ParticleFx.of("fx/spark")
            .color(0xFFFFF3D0).endColor(0x00C9A24B)
            .size(0.06f).endSize(0.02f)
            .lifetime(10, 6).gravity(0.03f).friction(0.88f).glow();
    private static final ParticleFx LIGHTNING_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFE8F2FF).endColor(0x005A7AD8)
            .size(0.07f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(12, 6).gravity(0f).friction(0.86f).glow();
    private static final ParticleFx LIGHTNING_FORK = ParticleFx.of("fx/bolt")
            .color(0xFFFFFFFF).endColor(0x00A8C8FF)
            .size(0.1f).endSize(0.06f).sizeRandom(0.4f)
            .lifetime(6, 4).gravity(0f).friction(0.8f).glow().randomRotation();
    private static final ParticleFx BRAND = ParticleFx.of("fx/brand")
            .color(0xFFE8F2FF).endColor(0x005A7AD8)
            .size(0.14f).endSize(0.08f)
            .lifetime(12, 4).gravity(0f).friction(0.9f).glow().randomRotation();
    private static final ParticleFx[] LAWS = {law("fx/law_0"), law("fx/law_1"), law("fx/law_2")};
    private static final ParticleFx PROHIBIT_MARK = ParticleFx.of("fx/prohibit")
            .color(0xFFFF8A80).endColor(0x00B03030)
            .size(0.16f).endSize(0.12f)
            .lifetime(20, 4).gravity(-0.01f).friction(0.9f).glow();
    private static final ParticleFx SHACKLE = ParticleFx.of("fx/shackle")
            .color(0xFFB0B8C8).endColor(0x003E434D)
            .size(0.12f).endSize(0.1f).sizeRandom(0.3f)
            .lifetime(22, 8).gravity(0.02f).friction(0.9f).spin(6f).randomRotation();
    private static final ParticleFx STAMP = ParticleFx.of("fx/verdict")
            .color(0xFFFFE6A0).endColor(0x00C9A24B)
            .size(0.16f).endSize(0.12f)
            .lifetime(20, 4).gravity(-0.01f).friction(0.9f).glow();
    private static final ParticleFx CHAIN = ParticleFx.of("fx/chain_link")
            .color(0xFF9AA0AC).endColor(0x003E434D)
            .size(0.1f).endSize(0.08f).sizeRandom(0.3f)
            .lifetime(24, 8).gravity(0.04f).friction(0.94f).spin(8f).randomRotation().collide();
    private static final ParticleFx GAVEL_MARK = ParticleFx.of("fx/gavel")
            .color(0xFFFFE6A0).endColor(0x00C9A24B)
            .size(0.14f).endSize(0.12f)
            .lifetime(24, 4).gravity(-0.01f).friction(0.9f).glow();
    private static final ParticleFx RED_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFFF8A80).endColor(0x00600808)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(20, 10).gravity(-0.02f).friction(0.9f).glow();
    private static final ParticleFx DISORDER_SMOKE = ParticleFx.of("fx/smoke")
            .color(0xA05A3A7A).endColor(0x00201028)
            .size(0.3f).endSize(0.8f).sizeRandom(0.3f)
            .lifetime(26, 12).gravity(-0.01f).friction(0.9f).spin(2f).randomRotation();
    private static final ParticleFx VIOLET_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFC0A0E8).endColor(0x002A1840)
            .size(0.09f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(22, 10).gravity(0f).friction(0.9f).glow();
    private static final ParticleFx DUST = ParticleFx.of("fx/smoke")
            .color(0x90B8B0A0).endColor(0x00605848)
            .size(0.3f).endSize(0.9f).sizeRandom(0.4f)
            .lifetime(24, 10).gravity(-0.005f).friction(0.9f).spin(2f).randomRotation();
    private static final ParticleFx GLASS_SHARD = ParticleFx.of("fx/splinter")
            .color(0xE0E8F4FF).endColor(0x00A0C0E0)
            .size(0.08f).endSize(0.05f).sizeRandom(0.5f)
            .lifetime(22, 10).gravity(0.05f).friction(0.95f).glow().spin(12f).randomRotation().collide();
    private static final ParticleFx PAPER = ParticleFx.of("fx/paper")
            .color(0xFFF2E4BC).endColor(0x00CDB27C)
            .size(0.1f).endSize(0.08f).sizeRandom(0.3f)
            .lifetime(30, 10).gravity(0.02f).friction(0.94f).spin(10f).randomRotation();
    private static final ParticleFx FOOTPRINT = ParticleFx.of("fx/footprint")
            .color(0xC0F0D68C).endColor(0x00C9A24B)
            .size(0.18f).endSize(0.18f)
            .lifetime(60, 0).gravity(0f).friction(0f).glow();
    private static final ParticleFx EYE_MARK = ParticleFx.of("fx/eye")
            .color(0xFFF0D68C).endColor(0x00C9A24B)
            .size(0.12f).endSize(0.1f)
            .lifetime(24, 0).gravity(0f).friction(0f).glow();

    private static ParticleFx law(String texture) {
        return ParticleFx.of(texture)
                .color(0xFFFFF3D0).endColor(0x00C9A24B)
                .size(0.09f).endSize(0.06f).sizeRandom(0.3f)
                .lifetime(26, 10).gravity(-0.015f).friction(0.9f).glow();
    }

    private static ParticleFx anyLaw() {
        return LAWS[ThreadLocalRandom.current().nextInt(LAWS.length)];
    }

    private static void lawBurst(ServerLevel sl, Vec3 at, int count, double minSpeed, double maxSpeed) {
        for (int i = 0; i < count; i++) ParticleShapes.burst(sl, anyLaw(), at, 1, minSpeed, maxSpeed);
    }

    // =====================================================================================
    // Lines and arcs
    // =====================================================================================

    private static final SlashFx BOLT_LINE = SlashFx.line("slash/smooth")
            .color(0xE0A8C8FF).core(0xFFFFFFFF).width(0.12f).taper(SlashFx.Taper.UNIFORM).lifetime(5).sweep(1);
    private static final SlashFx BOLT_ARC = SlashFx.arc("slash/streaks")
            .color(0xD0A8C8FF).headColor(0xFFE8F2FF).core(0xFFFFFFFF)
            .radius(1.3f).arc(120f).width(0.35f).layers(2).spread(0.12f).lifetime(6).sweep(2);
    private static final SlashFx RUSH = SlashFx.line("slash/streaks")
            .color(0x90C9A24B).core(0x80FFF3D0).width(0.45f).taper(SlashFx.Taper.COMET).lifetime(8).sweep(2);
    private static final SlashFx BEAM = SlashFx.line("slash/smooth")
            .color(0xF0A8C8FF).core(0xFFFFFFFF).width(0.3f).taper(SlashFx.Taper.COMET).lifetime(6).sweep(1);
    private static final SlashFx GOLD_THREAD = SlashFx.line("slash/smooth")
            .color(0xC0FFF3D0).core(0xFFFFFFFF).width(0.06f).taper(SlashFx.Taper.UNIFORM);
    private static final SlashFx CHAIN_LINE = SlashFx.line("slash/dashed")
            .color(0xD08A909C).core(0xA0E0E4EC).width(0.1f).taper(SlashFx.Taper.UNIFORM);
    private static final SlashFx RED_LINE = SlashFx.line("slash/dashed_fine")
            .color(0xC0B03030).core(0x80FFD0D0).width(0.08f).taper(SlashFx.Taper.UNIFORM);
    private static final SlashFx JUDGMENT_ARC = SlashFx.arc("slash/edge")
            .color(0xF0FFF3D0).headColor(0xFFFFFFFF).tailColor(0xA0C9A24B).core(0xFFFFFFFF)
            .radius(2.6f).arc(150f).width(0.8f).layers(3).spread(0.15f).lifetime(10).sweep(3);
    private static final SlashFx DEATH_ARC = SlashFx.arc("slash/streaks")
            .color(0xE0FFF3D0).headColor(0xFFFFFFFF).tailColor(0xA0B03030).core(0xFFFFFFFF)
            .radius(1.2f).arc(90f).width(0.5f).layers(2).spread(0.1f).lifetime(7).sweep(2);

    /** Lightning between two points: a few jagged straight pieces, drawn again a little differently each call. */
    private static void lightningBetween(ServerLevel sl, SlashFx line, Vec3 from, Vec3 to, double jag) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int pieces = Math.max(2, (int) (from.distanceTo(to) / 1.5));
        Vec3 last = from;
        for (int i = 1; i <= pieces; i++) {
            Vec3 next = from.lerp(to, i / (double) pieces);
            if (i < pieces) next = next.add(r.nextGaussian() * jag, r.nextGaussian() * jag, r.nextGaussian() * jag);
            ParticleShapes.slashBetween(sl, line, last, next);
            last = next;
        }
    }

    // =====================================================================================
    // Sounds
    // =====================================================================================

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

    private static Vec3 ahead(float yaw) {
        double angle = Math.toRadians(yaw);
        return new Vec3(-Math.sin(angle), 0, Math.cos(angle));
    }

    private static float yawToward(Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from);
        return (float) Math.toDegrees(Math.atan2(-d.x, d.z));
    }

    private static float randomYaw(ServerLevel sl) {
        return sl.getRandom().nextFloat() * 360f;
    }

    // =====================================================================================
    // Statuses: a picture in the row above the hotbar stacks, and a clock beside the hotbar
    // =====================================================================================

    public enum Status {
        // what the Justiciar holds
        JURISDICTION("jurisdiction", "Jurisdiction", 0xC9A24B),
        RECOGNITION("recognition", "Recognition", 0xF0D68C),
        LASHING("lashing", "Torture Device", 0xA8C8FF),
        CONVICT("convict", "Convict", 0xB03030),
        DECREE("decree", "Decree", 0x8A909C),
        PROXY("proxy", "Order Proxy", 0xFFF3D0),
        SPENT("spent", "Spent", 0x7A5A24),
        COURT("court", "Court of Judgment", 0xC9A24B),
        BALANCE("balance", "Balance", 0xF0D68C),
        DIVIDED("divided", "Divided", 0xC9A24B),
        // what is done to others (when they are players)
        HALTED("halted", "Halted", 0xB03030),
        COWED("cowed", "Cowed", 0x8A909C),
        TREMBLING("trembling", "Trembling", 0xA8C8FF),
        IMPRISONED("imprisoned", "Imprisoned", 0xDCECFF),
        EXILED("exiled", "Exiled", 0x5A3A7A),
        STUNNED("stunned", "Stunned", 0xFFF3D0),
        SILENCED("silenced", "Silenced", 0x8A909C),
        DEPRIVED("deprived", "Deprived", 0x9A72C8),
        SENTENCED("sentenced", "Sentenced", 0xB03030),
        PROHIBITED("prohibited", "Prohibited", 0xB03030),
        REVEALED("revealed", "Revealed", 0xC9A24B),
        REALITY("reality", "Reality", 0x8A909C),
        CONTRACT("contract", "Contract", 0xF2E4BC),
        NO_RETURN("no_return", "No Return", 0xB03030),
        JUDGED("judged", "Judged", 0xC9A24B);

        final String image;
        final String label;
        final int rgb;

        Status(String image, String label, int rgb) {
            this.image = image;
            this.label = label;
            this.rgb = rgb;
        }

        String id() {
            return "ho_status_" + name().toLowerCase();
        }
    }

    /** Shows the status's picture (in the row above the hotbar stacks, rows of nine) and a clock beside the hotbar. */
    public static void status(Player player, Status status, int ticks) {
        int slot = status.ordinal();
        ScreenImages.show(player, status.id(), "hand_of_order/" + status.image,
                ScreenAnchor.BOTTOM, 103 + (slot % 9) * 18, -21 - (slot / 9) * 18, 16, ticks);
        HudTimer.show(player, status.id(), status.label, ticks, status.rgb);
    }

    /** It ended early. */
    public static void statusEnd(Player player, Status status) {
        ScreenImages.hide(player, status.id());
        HudTimer.hide(player, status.id());
    }

    // ---------- the Court's clauses on the screen

    private static String clauseId(int index) {
        return "ho_clause_" + index;
    }

    /**
     * The clauses written so far (1 to 7) as pictures in the row beside the hotbar, where the Attendant's stacks
     * are, and the whole tablet in the top right corner for 'ticks'. Call it again each time a clause is written.
     */
    public static void courtClauses(Player player, int written, int ticks) {
        for (int i = 1; i <= 7; i++) {
            if (i <= written) {
                ScreenImages.show(player, clauseId(i), "hand_of_order/clause_" + i,
                        ScreenAnchor.BOTTOM, 103 + (i - 1) * 18, -3, 16, ticks);
            } else {
                ScreenImages.hide(player, clauseId(i));
            }
        }
        ScreenImages.show(player, "ho_court_tablet", "hand_of_order/court_clauses", ScreenAnchor.TOP_RIGHT, -8, 8, 64, ticks);
    }

    public static void courtClausesHide(Player player) {
        for (int i = 1; i <= 7; i++) ScreenImages.hide(player, clauseId(i));
        ScreenImages.hide(player, "ho_court_tablet");
    }

    // =====================================================================================
    // The wheels' pictures (textures/gui/ability_menus/hand_of_order/, for AbilityWheel.entry's icon)
    // =====================================================================================

    public static final String WHEEL_PSYCHIC_LASHING = "ability_menus/hand_of_order/psychic_lashing";
    public static final String WHEEL_PSYCHIC_PIERCING = "ability_menus/hand_of_order/psychic_piercing";
    public static final String WHEEL_WHIP_OF_PAIN = "ability_menus/hand_of_order/whip_of_pain";
    public static final String WHEEL_IMPRISON = "ability_menus/hand_of_order/imprison";
    public static final String WHEEL_EXILE = "ability_menus/hand_of_order/exile";
    public static final String WHEEL_DEATH = "ability_menus/hand_of_order/death";
    public static final String WHEEL_MAINTAIN_SECRECY = "ability_menus/hand_of_order/maintain_secrecy";
    public static final String WHEEL_DEPRIVATION = "ability_menus/hand_of_order/deprivation";
    public static final String WHEEL_EXECUTION = "ability_menus/hand_of_order/execution";
    public static final String WHEEL_PROHIBITION_PASSAGE = "ability_menus/hand_of_order/prohibition_passage";
    public static final String WHEEL_PROHIBITION_CONCEALMENT = "ability_menus/hand_of_order/prohibition_concealment";
    public static final String WHEEL_IMPERATIVE_REALITY = "ability_menus/hand_of_order/imperative_reality";
    public static final String WHEEL_CONTRACT = "ability_menus/hand_of_order/contract";
    public static final String WHEEL_BALANCE = "ability_menus/hand_of_order/balance";
    public static final String WHEEL_DIVIDE_BATTLEFIELD = "ability_menus/hand_of_order/divide_battlefield";
    public static final String WHEEL_LAW_NO_RETURN = "ability_menus/hand_of_order/law_no_return";
    public static final String WHEEL_ORDER_PROXY = "ability_menus/hand_of_order/order_proxy";
    public static final String WHEEL_QUELL_DISORDER = "ability_menus/hand_of_order/quell_disorder";
    public static final String WHEEL_COURT_OF_JUDGMENT = "ability_menus/hand_of_order/court_of_judgment";

    // =====================================================================================
    // The models (models/model_fx/hand_of_order/, see each JSON's "credit")
    // =====================================================================================

    /** The Brass Pillar, 2 blocks tall at scale 1; pivot the middle of its base. */
    public static final ModelFx BRASS_PILLAR = ModelFx.of("hand_of_order/brass_pillar").glow().aura(0x80FFF3D0, 0.12f, 3);
    /** The scales: the stand (pivot its foot) and the beam (pivot its hinge, 1.5 blocks up the stand). */
    public static final ModelFx SCALES_STAND = ModelFx.of("hand_of_order/scales_stand").glow();
    public static final ModelFx SCALES_BEAM = ModelFx.of("hand_of_order/scales_beam").pivot(8, 24, 8).glow();
    /** An Illusory Torture Device, 1.9 blocks, point up; pivot its grip. Tint it lightning blue. */
    public static final ModelFx BRANDING_IRON = ModelFx.of("hand_of_order/branding_iron").pivot(8, 4, 8).glow()
            .aura(0x80A8C8FF, 0.06f, 2);
    /** A spectral right fist, knuckles north, 1.5 blocks; pivot the wrist. */
    public static final ModelFx SPECTRAL_FIST = ModelFx.of("hand_of_order/spectral_fist").pivot(8, 8, 14).glow().unshaded()
            .seeThrough().aura(0x80FFF3D0, 0.1f, 3);
    /** One glass wall, 1 x 2 blocks, standing; pivot the middle of its bottom edge. */
    public static final ModelFx GLASS_PANE = ModelFx.of("hand_of_order/glass_pane").glow().seeThrough();
    /** Confinement's cell, 1 x 2 x 1 blocks; pivot the middle of its floor. */
    public static final ModelFx CONFINEMENT_CELL = ModelFx.of("hand_of_order/confinement_cell").glow().seeThrough();
    /** A gavel, 1.25 blocks, handle south; pivot the end of the handle. */
    public static final ModelFx GAVEL = ModelFx.of("hand_of_order/gavel").pivot(8, 8, 19).glow();
    /** The Sword of Judgment, 3 blocks, point up; pivot the grip. */
    public static final ModelFx JUDGMENT_SWORD = ModelFx.of("hand_of_order/judgment_sword").pivot(8, -10, 8).glow().unshaded()
            .aura(0xA0FFF3D0, 0.14f, 3);
    /** Flat pictures, 1 block, white (tint them): the seals and circles on the ground. */
    public static final ModelFx ORDER_SEAL = ModelFx.of("hand_of_order/order_seal").glow().unshaded().seeThrough();
    public static final ModelFx DECREE_CIRCLE = ModelFx.of("hand_of_order/decree_circle").glow().unshaded().seeThrough();
    public static final ModelFx COURT_FLOOR = ModelFx.of("hand_of_order/court_floor").glow().unshaded().seeThrough();
    public static final ModelFx REALITY_SEAL = ModelFx.of("hand_of_order/reality_seal").glow().unshaded().seeThrough();
    public static final ModelFx NO_RETURN_SEAL = ModelFx.of("hand_of_order/no_return_sigil").glow().unshaded().seeThrough();
    public static final ModelFx QUELL_SEAL = ModelFx.of("hand_of_order/quell_seal").glow().unshaded().seeThrough();
    /** Standing pictures, 1 block, white (tint them); pivot their middle. */
    public static final ModelFx PROHIBITION = ModelFx.of("hand_of_order/prohibition_sigil").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx CONVICT_BRAND = ModelFx.of("hand_of_order/convict_brand").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx RECOGNITION_MARK = ModelFx.of("hand_of_order/recognition_mark").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx EXECUTION_RING = ModelFx.of("hand_of_order/execution_ring").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx EXILE_VOID = ModelFx.of("hand_of_order/exile_void").pivot(8, 8, 8).glow().unshaded().seeThrough();
    public static final ModelFx DAWN_HALO = ModelFx.of("hand_of_order/dawn_halo").pivot(8, 8, 8).glow().unshaded().seeThrough();
    /** The Contract, 0.75 x 1 blocks, its own colors; pivot its middle. */
    public static final ModelFx CONTRACT_PAPER = ModelFx.of("hand_of_order/contract_paper").pivot(8, 8, 8).glow().seeThrough();
    /** A panel of the wall of Order, 1 block, standing, animated; pivot the middle of its bottom edge. */
    public static final ModelFx ORDER_WALL = ModelFx.of("hand_of_order/order_wall").glow().unshaded().seeThrough();

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

    /** A ring of standing panels round a center: a wall. 'height' in blocks; each panel about 'width' blocks wide. */
    private static void wallRing(ServerLevel sl, ModelFx panel, Vec3 center, double radius, double height, int color,
                                 int ticks, int risen, String tag) {
        int count = Math.max(8, Math.min(40, (int) (Math.PI * 2 * radius / 2.0)));
        float width = (float) (Math.PI * 2 * radius / count) * 1.05f;
        for (int i = 0; i < count; i++) {
            double a = Math.PI * 2 * i / count;
            Vec3 at = center.add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
            float yaw = yawToward(at, center);
            ModelFx one = panel.color(color).scale(width, (float) height, 1f).lifetime(ticks)
                    .fade(0, Math.min(16, ticks / 3)).tag(tag)
                    .key(0, pose().up((float) -height).alpha(0f))
                    .key(risen, pose().up(0f).alpha(1f), Ease.OUT);
            ParticleShapes.model(sl, one, at, yaw, 0f, 0f);
        }
    }

    // =====================================================================================
    // The spoken words (docs: "Verdicts are one spoken word. Each shows its word above you when cast.")
    // =====================================================================================

    /** The words, each a model (models/model_fx/hand_of_order/word_*.json) and the color it is spoken in. */
    public enum Word {
        HALT("word_halt", 0xFFFFE6A0),
        IMPRISON("word_imprison", 0xFFDCECFF),
        RELEASE("word_release", 0xFFDCECFF),
        EXILE("word_exile", 0xFFC0A0E8),
        DEATH("word_death", 0xFFFF8A80),
        SILENCE("word_silence", 0xFFB0B8C8),
        DEPRIVE("word_deprive", 0xFFC0A0E8),
        EXECUTE("word_execute", 0xFFFF6A60),
        GUILTY("word_guilty", 0xFFFF8A80),
        SIGN("word_sign", 0xFFF2E4BC),
        PROHIBITED("word_prohibited", 0xFFB0B8C8),
        REALITY("word_reality", 0xFFB0B8C8),
        NO_RETURN("word_no_return", 0xFFFF8A80),
        BALANCE("word_balance", 0xFFF0D68C),
        ORDER("word_order", 0xFFFFF3D0),
        VIOLATION("word_violation", 0xFFFF6A60),
        JUDGMENT("word_judgment", 0xFFFFF3D0);

        public final ModelFx model;
        public final int color;

        Word(String model, int color) {
            this.model = ModelFx.of("hand_of_order/" + model).glow().unshaded().seeThrough();
            this.color = color;
        }
    }

    /**
     * 'who' speaks a word: it rises over their head in its own color, facing the way they face (readable from in
     * front and from behind), holds for a second and fades; a low voice says it.
     */
    public static void speak(ServerLevel sl, Entity who, Word word) {
        speak(sl, who, word, 0.55f, true);
    }

    /** speak, sized (0.55 = the usual), with or without the voice (a Decree's proclamation has its own sound). */
    public static void speak(ServerLevel sl, Entity who, Word word, float size, boolean voice) {
        ModelFx shown = word.model.color(word.color).scale(size).lifetime(34).fade(0, 12)
                .key(0, pose().up(-0.2f).scale(0.6f).alpha(0f))
                .key(5, pose().up(0.1f).scale(1.05f).alpha(1f), Ease.OUT_BACK)
                .key(34, pose().up(0.5f).scale(1f).alpha(0f), Ease.IN);
        ParticleShapes.modelOn(sl, shown, who, new Vec3(0, who.getBbHeight() + 0.45, 0), who.getYRot(), 0f, 0f);
        ParticleShapes.burst(sl, BRASS_MOTE, headOf(who).add(0, 0.6, 0), 5, 0.02, 0.06);
        if (voice) sound(sl, headOf(who), ModSounds.ORDER_VERDICT, 1.0f, wobble(1.0f));
    }

    // =====================================================================================
    // Key 1: Halt / Presence (Arbiter)
    // =====================================================================================

    /** The tick Halt holds its target: the hold goes there. */
    public static final int HALT_LANDS = 4;

    /** One word at the enemy looked at: HALT over the Arbiter, a brass ring closing on the target. */
    public static void halt(ServerLevel sl, Player player, LivingEntity target) {
        speak(sl, player, Word.HALT, 0.55f, false);
        sound(sl, player.position(), ModSounds.ORDER_HALT, 1.0f, wobble(1.0f));
        if (target == null) return;
        SpellFx.closingRing(sl, target.position(), 2.4, 0xE0F0D68C, HALT_LANDS, 0);
        Later.run(sl, HALT_LANDS, () -> {
            if (!target.isAlive()) return;
            ParticleShapes.ring(sl, SHACKLE, chestOf(target), Math.max(0.5, target.getBbWidth() * 0.8), 6, 0.0);
            ParticleShapes.burst(sl, BRASS_SPARK, chestOf(target), 10, 0.05, 0.15);
        });
    }

    /** Held still by the word: a ring of iron under it for 'ticks'. */
    public static void halted(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth()), 0xC0B0B8C8, ticks, tag);
        if (target instanceof Player p) status(p, Status.HALTED, ticks);
    }

    /** A mob that was hunting the Arbiter forgets them: a puff of dust over its head. */
    public static void forgets(ServerLevel sl, LivingEntity mob) {
        ParticleShapes.burst(sl, DUST, headOf(mob).add(0, 0.3, 0), 3, 0.01, 0.03);
        ParticleShapes.spawn(sl, EYE_MARK.color(0xFF8A909C), headOf(mob).add(0, 0.5, 0), Vec3.ZERO);
    }

    // ---------- sneak: Presence

    /**
     * A wave of Authority: a brass ring races out 'radius' blocks behind a horn. intimidation = the Imperative Mage's
     * form: the Brass Pillar flashes up behind and iron shackles drop on the ring.
     */
    public static void presence(ServerLevel sl, Player player, double radius, boolean intimidation) {
        SpellFx.shockRing(sl, player.position(), radius, 0xE0F0D68C, 12, 0);
        SpellFx.shockRing(sl, player.position(), radius * 0.7, 0xA0FFF3D0, 10, 3);
        ParticleShapes.ring(sl, BRASS_MOTE, player.position().add(0, 0.3, 0), 1.0, 16, 0.4);
        lawBurst(sl, chestOf(player), 6, 0.05, 0.15);
        if (intimidation) {
            ModelFx pillar = BRASS_PILLAR.scale(1.6f).lifetime(24).fade(2, 10)
                    .key(0, pose().forward(-1.2f).up(-1f).alpha(0f))
                    .key(6, pose().up(0f).alpha(1f), Ease.OUT_BACK);
            ParticleShapes.modelOnTurning(sl, pillar, player, Vec3.ZERO, 0f, 0f, 0f);
            ParticleShapes.ring(sl, SHACKLE, player.position().add(0, 0.5, 0), radius * 0.6, 18, 0.1);
        }
        sound(sl, player.position(), ModSounds.ORDER_PRESENCE, 1.2f, intimidation ? 0.85f : 1.0f);
    }

    /** Cowed: it cannot bring itself to attack. Brass bows over its head. */
    public static void cowed(ServerLevel sl, LivingEntity mob, int ticks, String tag) {
        ModelFx gavel = GAVEL.color(0xFFF0D68C).scale(0.35f).lifetime(ticks).fade(3, 6).tag(tag)
                .key(0, pose().pitch(-40f))
                .key(8, pose().pitch(10f), Ease.IN)
                .key(12, pose().pitch(0f), Ease.OUT);
        ParticleShapes.modelOn(sl, gavel, mob, new Vec3(0, mob.getBbHeight() + 0.5, 0), randomYaw(sl), 0f, 0f);
        ParticleShapes.burst(sl, BRASS_MOTE, headOf(mob), 4, 0.01, 0.04);
        if (mob instanceof Player p) status(p, Status.COWED, ticks);
    }

    // =====================================================================================
    // Key 2: Jurisdiction / Recognition (Sheriff)
    // =====================================================================================

    /** The tick the ground is claimed (the seal has opened). */
    public static final int JURISDICTION_CLAIMED = 12;

    /**
     * The Sheriff claims a circle of ground: a seal of Order opens underfoot, a brass ring races out to the edge and a
     * ring of law stays there for 'ticks' (cast again: end the old one with jurisdictionEnd first).
     */
    public static void jurisdiction(ServerLevel sl, Player player, Vec3 center, double radius, int ticks, String tag) {
        floor(sl, ORDER_SEAL, center, 3.0, 0xE0C9A24B, ticks, 0.4f, tag, 0);
        SpellFx.steadyRing(sl, center, radius, 0xC0F0D68C, JURISDICTION_CLAIMED, 0);
        ModelFx edge = SpellFx.RUNE_CIRCLE.color(0x60C9A24B).scale((float) (radius * 2.0)).lifetime(ticks)
                .fade(10, 20).spin(0.2f).delay(JURISDICTION_CLAIMED).tag(tag);
        ParticleShapes.model(sl, edge, center.add(0, 0.06, 0), randomYaw(sl), 0f, 0f);
        SpellFx.risingRings(sl, center, 1.6, 2.4, 3, 0xC0F0D68C);
        ParticleShapes.burst(sl, BRASS_MOTE, center.add(0, 0.5, 0), 16, 0.04, 0.14);
        sound(sl, center, ModSounds.ORDER_JURISDICTION, 1.6f, 1.0f);
        if (player instanceof ServerPlayer sp) status(sp, Status.JURISDICTION, ticks);
    }

    /** Every few seconds while it lasts: motes of brass drift up along the edge, so the boundary can be seen. */
    public static void jurisdictionTick(ServerLevel sl, Vec3 center, double radius) {
        int count = (int) Math.min(48, radius * 1.5);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            Vec3 at = center.add(Math.cos(a) * radius, 0.2 + r.nextDouble() * 0.6, Math.sin(a) * radius);
            ParticleShapes.spawn(sl, BRASS_MOTE, at, new Vec3(0, 0.03, 0));
        }
    }

    /** An enemy steps into the Jurisdiction: a soft chime and a brass outline only the owner sees and hears. */
    public static void jurisdictionEnter(ServerPlayer owner, LivingEntity who) {
        Vec3 at = who.position();
        double r = Math.max(0.5, who.getBbWidth() * 0.8);
        for (int i = 0; i < 10; i++) {
            double a = Math.PI * 2 * i / 10;
            ParticleShapes.spawnFor(owner, BRASS_MOTE, at.add(Math.cos(a) * r, 0.1, Math.sin(a) * r), new Vec3(0, 0.06, 0));
        }
        ParticleShapes.spawnFor(owner, EYE_MARK, headOf(who).add(0, 0.4, 0), Vec3.ZERO);
        soundFor(owner, at, ModSounds.ORDER_JURISDICTION_ENTER, 0.7f, 1.0f);
    }

    public static void jurisdictionEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        SpellFx.closingRing(sl, center, radius, 0x80C9A24B, 12, 0);
        ParticleShapes.burst(sl, BRASS_MOTE, center.add(0, 0.4, 0), 10, 0.02, 0.06);
    }

    // ---------- sneak: Recognition

    /** The enemy looked at is memorized: the frame of the law closes on it, only the owner sees. */
    public static void recognition(ServerPlayer owner, LivingEntity target, int ticks, String tag) {
        ModelFx mark = RECOGNITION_MARK.color(0xF0F0D68C).scale(Math.max(1.0f, target.getBbHeight() * 0.9f))
                .lifetime(24).fade(0, 10)
                .key(0, pose().scale(2.2f).alpha(0f))
                .key(8, pose().scale(1f).alpha(1f), Ease.OUT);
        float yaw = yawToward(chestOf(target), owner.getEyePosition());
        ParticleShapes.spawnFor(owner, mark.rotated(yaw, 0f, 0f).following(target, new Vec3(0, target.getBbHeight() * 0.5, 0)),
                chestOf(target), Vec3.ZERO);
        soundFor(owner, chestOf(target), ModSounds.ORDER_RECOGNITION, 0.9f, 1.0f);
        status(owner, Status.RECOGNITION, ticks);
    }

    /**
     * Each second while it lasts: a glowing footprint where it stands and a small mark over it, both only for the
     * owner (the glowing through walls is the ability's).
     */
    public static void recognitionTrack(ServerPlayer owner, LivingEntity target) {
        ParticleShapes.spawnFor(owner, FOOTPRINT, target.position().add(0, 0.02, 0), Vec3.ZERO);
        ParticleShapes.spawnFor(owner, EYE_MARK, headOf(target).add(0, 0.5, 0), Vec3.ZERO);
    }

    public static void recognitionEnd(ServerPlayer owner, LivingEntity target, String tag) {
        statusEnd(owner, Status.RECOGNITION);
    }

    // =====================================================================================
    // Key 3: Interrogation (Interrogator)
    // =====================================================================================

    /** Psychic Lashing: an illusory branding iron flickers along the weapon in hand for 'ticks'. */
    public static void psychicLashing(ServerLevel sl, Player player, int ticks, String tag) {
        ModelFx iron = BRANDING_IRON.color(0xFFA8C8FF).scale(0.55f).lifetime(ticks).fade(4, 8).tag(tag)
                .key(0, pose().forward(0.35f).right(0.38f).up(0.2f).pitch(-60f).alpha(0f))
                .key(6, pose().alpha(0.85f), Ease.OUT);
        ParticleShapes.modelOnTurning(sl, iron, player, new Vec3(0, player.getEyeHeight() - 0.7, 0), 0f, 0f, 0f);
        lightningBetween(sl, BOLT_LINE, SpellFx.handOf(player, sl), SpellFx.handOf(player, sl).add(Spells.flatLook(player)), 0.15);
        ParticleShapes.burst(sl, LIGHTNING_FORK, SpellFx.handOf(player, sl), 6, 0.04, 0.12);
        sound(sl, player.position(), ModSounds.ORDER_LASHING, 1.0f, 1.0f);
        status(player, Status.LASHING, ticks);
    }

    /** Every few ticks while the iron is coated: sparks crawl along the hand. */
    public static void lashingTick(ServerLevel sl, Player player) {
        Vec3 hand = SpellFx.handOf(player, sl);
        if (hand == null) return;
        ParticleShapes.spawn(sl, LIGHTNING_MOTE, hand.add(Spells.flatLook(player).scale(0.3)), new Vec3(0, 0.02, 0));
    }

    /** A hit with the coated weapon: a branding arc of lightning and the brand's mark on the target. */
    public static void lashHit(ServerLevel sl, Player player, LivingEntity target) {
        Vec3 at = chestOf(target);
        ParticleShapes.slash(sl, BOLT_ARC.varied(sl.getRandom(), 0f, 0f, 40f), at, player.getYRot(), 0f, 0f);
        ParticleShapes.spawn(sl, BRAND, at, Vec3.ZERO);
        ParticleShapes.burst(sl, LIGHTNING_FORK, at, 6, 0.06, 0.18);
        sound(sl, at, ModSounds.ORDER_LASH_HIT, 0.9f, wobble(1.0f));
    }

    public static void lashingEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        statusEnd(player, Status.LASHING);
    }

    /**
     * Psychic Piercing: bolts of lightning leap from the eyes to 'end' (12 blocks, or what stopped them). 'hits' are
     * the enemies on the line; piercingHit each one at once.
     */
    public static void psychicPiercing(ServerLevel sl, Player player, Vec3 end, List<LivingEntity> hits) {
        Vec3 eyes = player.getEyePosition().add(Spells.flatLook(player).scale(0.3));
        ParticleShapes.slashBetween(sl, BEAM, eyes, end);
        for (int i = 0; i < 3; i++) lightningBetween(sl, BOLT_LINE, eyes, end, 0.25);
        ParticleShapes.burst(sl, LIGHTNING_MOTE, eyes, 6, 0.02, 0.06);
        ParticleShapes.line(sl, LIGHTNING_MOTE, eyes, end, 1.5);
        ParticleShapes.burst(sl, LIGHTNING_FORK, end, 8, 0.05, 0.15);
        sound(sl, player.position(), ModSounds.ORDER_PIERCING, 1.0f, wobble(1.0f));
    }

    /** One caught on the line; spirit = a spirit or marionette (it takes double): a brighter burst. */
    public static void piercingHit(ServerLevel sl, LivingEntity target, boolean spirit) {
        Vec3 at = chestOf(target);
        ParticleShapes.burst(sl, LIGHTNING_FORK, at, spirit ? 14 : 8, 0.06, 0.2);
        ParticleShapes.spawn(sl, BRAND.color(spirit ? 0xFFFFFFFF : 0xFFE8F2FF), at, Vec3.ZERO);
    }

    /** The tick the Whip of Pain lands. */
    public static final int WHIP_LANDS = 5;

    /** A barbed whip of lightning lashes out from the hand to the target. */
    public static void whipOfPain(ServerLevel sl, Player player, LivingEntity target) {
        Vec3 hand = SpellFx.handOf(player, sl);
        if (hand == null) return;
        sound(sl, player.position(), ModSounds.ORDER_WHIP, 1.0f, wobble(1.0f));
        Vec3 to0 = chestOf(target);
        Vec3 side = Spells.rightOf(player).scale(1.6).add(0, 1.2, 0);
        // the lash unrolls in a curve over the ticks before it lands
        for (int step = 0; step < WHIP_LANDS; step++) {
            int s = step;
            Later.run(sl, step, () -> {
                if (!target.isAlive()) return;
                Vec3 to = chestOf(target);
                double t = (s + 1) / (double) WHIP_LANDS;
                Vec3 mid = hand.lerp(to, 0.5).add(side.scale(1.0 - t));
                Vec3 tip = hand.lerp(mid, t).lerp(mid.lerp(to, t), t);
                lightningBetween(sl, BOLT_LINE.width(0.08f), hand, hand.lerp(mid, t), 0.12);
                lightningBetween(sl, BOLT_LINE.width(0.08f), hand.lerp(mid, t), tip, 0.12);
            });
        }
        Later.run(sl, WHIP_LANDS, () -> {
            if (!target.isAlive()) return;
            Vec3 at = chestOf(target);
            lightningBetween(sl, BOLT_LINE.width(0.14f), hand, at, 0.2);
            ParticleShapes.burst(sl, LIGHTNING_FORK, at, 12, 0.08, 0.22);
            ParticleShapes.spawn(sl, BRAND, at, Vec3.ZERO);
        });
    }

    /** It trembles: lightning crawls over it for 'ticks'. */
    public static void trembling(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth()), 0xA0A8C8FF, ticks, tag);
        for (int t = 0; t < ticks; t += 5) {
            Later.run(sl, t, () -> {
                if (!target.isAlive()) return;
                Vec3 c = chestOf(target);
                ParticleShapes.burst(sl, LIGHTNING_FORK, c, 2, 0.04, 0.1);
                ParticleShapes.spawn(sl, LIGHTNING_MOTE, c.add((sl.getRandom().nextDouble() - 0.5) * target.getBbWidth(),
                        (sl.getRandom().nextDouble() - 0.5) * target.getBbHeight() * 0.6, 0), Vec3.ZERO);
            });
        }
        if (target instanceof Player p) status(p, Status.TREMBLING, ticks);
    }

    /** It cannot lie: its Concealment breaks and the frame of the law flashes on it. */
    public static void truthOut(ServerLevel sl, LivingEntity target) {
        revealed(sl, target);
    }

    // =====================================================================================
    // Key 4: Verdict (Judge and up)
    // =====================================================================================

    /** The tick Imprison's walls have closed: the hold goes there. */
    public static final int IMPRISON_LANDS = 6;

    /** IMPRISON: four glass walls slide in round the target and stay for 'ticks'. */
    public static void imprison(ServerLevel sl, Player player, LivingEntity target, int ticks, String tag) {
        speak(sl, player, Word.IMPRISON);
        float w = Math.max(1.0f, target.getBbWidth() * 1.6f);
        float h = Math.max(1.0f, target.getBbHeight() * 0.65f);
        double r = w * 0.5;
        for (int i = 0; i < 4; i++) {
            float yaw = target.getYRot() + i * 90f;
            Vec3 out = ahead(yaw);
            ModelFx pane = GLASS_PANE.color(0xC0E0F0FF).scale(w, h, 1f).lifetime(ticks + IMPRISON_LANDS).fade(2, 8).tag(tag)
                    .key(0, pose().forward((float) -(r + 2.5)).alpha(0f))
                    .key(IMPRISON_LANDS, pose().forward(0f).alpha(1f), Ease.IN);
            ParticleShapes.modelOn(sl, pane, target, out.scale(r), yaw + 180f, 0f, 0f);
        }
        Later.run(sl, IMPRISON_LANDS, () -> {
            if (!target.isAlive()) return;
            ParticleShapes.burst(sl, WHITE_MOTE, chestOf(target), 10, 0.04, 0.12);
            ParticleShapes.ring(sl, SHACKLE, target.position().add(0, 0.2, 0), r, 6, 0.0);
            sound(sl, target.position(), ModSounds.ORDER_IMPRISON, 1.0f, 1.0f);
        });
        if (target instanceof Player p) status(p, Status.IMPRISONED, ticks + IMPRISON_LANDS);
    }

    /** "Release": the walls lift away, freeing it (for free, or an ally held by mistake). */
    public static void release(ServerLevel sl, LivingEntity target, String tag) {
        speak(sl, target, Word.RELEASE, 0.45f, false);
        imprisonEnd(sl, target, tag);
        sound(sl, target.position(), ModSounds.ORDER_RELEASE, 1.0f, 1.0f);
    }

    /** The walls break when the hold ends: glass shards and a clear chime. */
    public static void imprisonEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.burst(sl, GLASS_SHARD, chestOf(target), 14, 0.08, 0.2);
        if (target instanceof Player p) statusEnd(p, Status.IMPRISONED);
    }

    /** Confinement (Imperative Mage): a transparent cell drops over the target and seals for 'ticks'. */
    public static void confinement(ServerLevel sl, Player player, LivingEntity target, int ticks, String tag) {
        speak(sl, player, Word.IMPRISON);
        float w = Math.max(1.0f, target.getBbWidth() * 1.6f);
        float h = Math.max(1.0f, (target.getBbHeight() + 0.4f) / 2f);
        ModelFx cell = CONFINEMENT_CELL.color(0xD0E0F0FF).scale(w, h, w).lifetime(ticks + IMPRISON_LANDS).fade(2, 8).tag(tag)
                .key(0, pose().up(4f).alpha(0f))
                .key(IMPRISON_LANDS, pose().up(0f).alpha(1f), Ease.OUT_BOUNCE);
        ParticleShapes.modelOn(sl, cell, target, Vec3.ZERO, target.getYRot(), 0f, 0f);
        Later.run(sl, IMPRISON_LANDS, () -> {
            if (!target.isAlive()) return;
            SpellFx.shockRing(sl, target.position(), w * 1.4, 0xC0DCECFF, 8, 0);
            ParticleShapes.burst(sl, WHITE_MOTE, chestOf(target), 12, 0.04, 0.12);
            sound(sl, target.position(), ModSounds.ORDER_IMPRISON, 1.0f, 0.85f);
        });
        if (target instanceof Player p) status(p, Status.IMPRISONED, ticks + IMPRISON_LANDS);
    }

    /** The tick Exile throws: the push goes there. */
    public static final int EXILE_LANDS = 3;

    /** EXILE: an invisible force throws the target away; 'to' = about where it lands (for the streak). */
    public static void exile(ServerLevel sl, Player player, LivingEntity target, Vec3 to) {
        speak(sl, player, Word.EXILE);
        sound(sl, target.position(), ModSounds.ORDER_EXILE, 1.0f, wobble(1.0f));
        Later.run(sl, EXILE_LANDS, () -> {
            Vec3 from = chestOf(target);
            SpellFx.shockRing(sl, target.position(), 2.0, 0xC0C0A0E8, 6, 0);
            ParticleShapes.burst(sl, VIOLET_MOTE, from, 10, 0.08, 0.2);
            ParticleShapes.slashBetween(sl, RUSH.color(0x90C0A0E8), from, to.add(0, from.y - target.getY(), 0));
        });
    }

    /** The void (Imperative Mage): a hole opens and swallows the target for 'ticks', then gives it back. */
    public static void exileVoid(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        Vec3 at = chestOf(target);
        float size = Math.max(1.4f, target.getBbHeight() * 1.2f);
        ModelFx hole = EXILE_VOID.color(0xE02A1840).scale(size).lifetime(ticks + 10).fade(2, 8).tag(tag)
                .key(0, pose().scale(0.1f))
                .key(6, pose().scale(1f), Ease.OUT_BACK)
                .key(ticks + 4, pose().scale(1f))
                .key(ticks + 10, pose().scale(0.1f), Ease.IN);
        ParticleShapes.model(sl, hole, at, yawToward(at, at.add(ahead(target.getYRot()))), 0f, 0f);
        ParticleShapes.model(sl, hole.rotated(90f, 0f, 0f), at, yawToward(at, at.add(ahead(target.getYRot()))), 0f, 0f);
        ParticleShapes.burst(sl, DISORDER_SMOKE, at, 8, 0.02, 0.06);
        sound(sl, at, ModSounds.ORDER_EXILE_VOID, 1.0f, 1.0f);
        if (target instanceof Player p) status(p, Status.EXILED, ticks);
    }

    /** It comes back on the same spot. */
    public static void exileReturn(ServerLevel sl, Vec3 at, String tag) {
        ParticleShapes.clearModels(sl, at, tag);
        ParticleShapes.burst(sl, VIOLET_MOTE, at, 14, 0.06, 0.16);
        ParticleShapes.burst(sl, DISORDER_SMOKE, at, 5, 0.02, 0.06);
        sound(sl, at, ModSounds.ORDER_EXILE_VOID, 0.6f, 1.4f);
    }

    /** The hurricane (Chaos Hunter): a wind forms at the target and throws away everything within 'radius'. */
    public static void exileHurricane(ServerLevel sl, Vec3 center, double radius) {
        ParticleShapes.spiral(sl, DUST, center.add(0, 0.2, 0), 4, 40, 0.4, radius, 1.5, 0.35);
        ParticleShapes.spiral(sl, VIOLET_MOTE, center.add(0, 1.2, 0), 3, 30, 0.3, radius * 0.8, 1.2, 0.3);
        SpellFx.shockRing(sl, center, radius, 0xA0C0A0E8, 12, 0);
        SpellFx.risingRings(sl, center, radius * 0.6, 3.0, 4, 0x80C0A0E8);
    }

    /** The tick Death's blow lands. */
    public static final int DEATH_LANDS = 6;

    /**
     * DEATH: the Judge blurs into a rush from 'from' (afterimages of brass) and a spectral fist strikes the head.
     * wound = the Disciplinary Paladin's form, aimed at its latest wound: the blow is red and the wound opens.
     */
    public static void death(ServerLevel sl, Player player, Vec3 from, LivingEntity target, boolean wound) {
        speak(sl, player, Word.DEATH);
        ParticleShapes.slashBetween(sl, RUSH, from.add(0, 1.0, 0), chestOf(player));
        for (int i = 0; i < 4; i++) {
            Vec3 at = from.lerp(player.position(), i / 4.0).add(0, 1.0, 0);
            ParticleShapes.burst(sl, BRASS_MOTE, at, 3, 0.01, 0.03);
        }
        sound(sl, player.position(), ModSounds.ORDER_DEATH, 1.0f, wobble(1.0f));
        if (target == null) return;
        float yaw = yawToward(player.position(), target.position());
        ModelFx fist = SPECTRAL_FIST.color(wound ? 0xE0FFB0A0 : 0xE0FFF3D0).scale(0.8f).lifetime(DEATH_LANDS + 6).fade(1, 5)
                .key(0, pose().forward(-1.2f).alpha(0f))
                .key(DEATH_LANDS, pose().forward(0.5f).alpha(1f), Ease.IN);
        ParticleShapes.model(sl, fist, headOf(target).subtract(ahead(yaw).scale(0.9)).add(0, -0.2, 0), yaw, 0f, 0f);
        Later.run(sl, DEATH_LANDS, () -> {
            if (!target.isAlive()) return;
            Vec3 head = headOf(target).add(0, -0.2, 0);
            ParticleShapes.slash(sl, wound ? DEATH_ARC.headColor(0xFFFF8A80) : DEATH_ARC, head, yaw, 0f, 20f);
            ParticleShapes.burst(sl, wound ? RED_MOTE : BRASS_SPARK, head, 12, 0.08, 0.22);
            ParticleShapes.spawn(sl, STAMP, head.add(0, 0.4, 0), Vec3.ZERO);
            if (wound) ParticleShapes.burst(sl, RED_MOTE, chestOf(target), 8, 0.02, 0.06);
        });
    }

    /** Maintain Secrecy: a strange light flashes in the eyes and a seal falls over the target's mouth for 'ticks'. */
    public static void maintainSecrecy(ServerLevel sl, Player player, LivingEntity target, int ticks, String tag) {
        speak(sl, player, Word.SILENCE, 0.5f, false);
        Vec3 eyes = player.getEyePosition().add(Spells.flatLook(player).scale(0.3));
        ParticleShapes.burst(sl, WHITE_MOTE, eyes, 8, 0.02, 0.06);
        ParticleShapes.slashBetween(sl, GOLD_THREAD.lifetime(8).sweep(2), eyes, headOf(target).add(0, -0.25, 0));
        SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth()), 0xA0B0B8C8, ticks, tag);
        ParticleShapes.spawn(sl, STAMP.color(0xFFB0B8C8), headOf(target).add(0, -0.15, 0), Vec3.ZERO);
        sound(sl, target.position(), ModSounds.ORDER_SECRECY, 1.0f, 1.0f);
        if (target instanceof Player p) status(p, Status.SILENCED, ticks);
    }

    public static void secrecyEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        if (target instanceof Player p) statusEnd(p, Status.SILENCED);
    }

    /** The tick the power is taken (it is locked away there). */
    public static final int DEPRIVED = 10;

    /** DEPRIVE: 'count' orbs of violet power are drawn out of the target into the Mage's hand and locked away. */
    public static void deprivation(ServerLevel sl, Player player, LivingEntity target, int count) {
        speak(sl, player, Word.DEPRIVE);
        sound(sl, target.position(), ModSounds.ORDER_DEPRIVATION, 1.0f, 1.0f);
        for (int i = 0; i < Math.max(1, count); i++) {
            int k = i;
            for (int t = 0; t < DEPRIVED; t += 2) {
                int tick = t;
                Later.run(sl, t + k * 2, () -> {
                    Vec3 hand = SpellFx.handOf(player, sl);
                    if (hand == null || !target.isAlive()) return;
                    Vec3 at = chestOf(target).lerp(hand, tick / (double) DEPRIVED)
                            .add(0, Math.sin(Math.PI * tick / DEPRIVED) * 0.6 + k * 0.2, 0);
                    ParticleShapes.spawn(sl, VIOLET_MOTE.size(0.16f), at, Vec3.ZERO);
                });
            }
        }
        Later.run(sl, DEPRIVED + count * 2, () -> {
            Vec3 hand = SpellFx.handOf(player, sl);
            if (hand == null) return;
            ParticleShapes.spawn(sl, SHACKLE, hand, Vec3.ZERO);
            ParticleShapes.burst(sl, DISORDER_SMOKE, hand, 3, 0.01, 0.04);
        });
    }

    /** The power stays dead in it for 'ticks': a violet ring underfoot. */
    public static void deprived(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        SpellFx.circleUnder(sl, target, Math.max(0.6, target.getBbWidth()), 0x909A72C8, ticks, tag);
        if (target instanceof Player p) status(p, Status.DEPRIVED, ticks);
    }

    public static void deprivedEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.burst(sl, VIOLET_MOTE, chestOf(target), 6, 0.02, 0.06);
        if (target instanceof Player p) statusEnd(p, Status.DEPRIVED);
    }

    /** The wind-up of Execution: the fist is raised this long; it falls at this tick. */
    public static final int EXECUTION_LANDS = 30;

    /**
     * EXECUTE: the right fist is raised high over the Mage and a ring closes on the target for EXECUTION_LANDS ticks.
     * Then executionStrike (or executionBroken if a blow breaks it, with the same tag).
     */
    public static void execution(ServerLevel sl, Player player, LivingEntity target, String tag) {
        speak(sl, player, Word.EXECUTE);
        ModelFx fist = SPECTRAL_FIST.color(0xE0FFF3D0).scale(1.4f).lifetime(EXECUTION_LANDS + 4).fade(4, 4).tag(tag)
                .key(0, pose().up(0f).pitch(-90f).alpha(0f))
                .key(12, pose().up(1.2f).pitch(-90f).alpha(1f), Ease.OUT)
                .key(EXECUTION_LANDS - 4, pose().up(1.5f).pitch(-100f), Ease.IN_OUT)
                .key(EXECUTION_LANDS, pose().up(0.6f).pitch(-10f), Ease.IN);
        ParticleShapes.modelOnTurning(sl, fist, player, new Vec3(0, player.getBbHeight() + 0.4, 0), 0f, 0f, 0f);
        if (target != null) {
            float size = Math.max(1.4f, target.getBbHeight() * 1.2f);
            ModelFx ring = EXECUTION_RING.color(0xE0FF6A60).lifetime(EXECUTION_LANDS + 2).fade(4, 2).spin(-4f).tag(tag)
                    .key(0, pose().scale(size * 2.2f).alpha(0f))
                    .key(EXECUTION_LANDS, pose().scale(size * 0.7f).alpha(1f), Ease.IN);
            ParticleShapes.modelOn(sl, ring, target, new Vec3(0, target.getBbHeight() * 0.55, 0),
                    yawToward(target.position(), player.position()), 0f, 0f);
            SpellFx.tether(sl, RED_LINE, () -> SpellFx.handOf(player, sl), () -> SpellFx.middleOf(target, sl), EXECUTION_LANDS);
        }
        sound(sl, player.position(), ModSounds.ORDER_EXECUTION_RAISE, 1.0f, 1.0f);
    }

    /** The fist falls on the target from any distance; slain = it was under the threshold and simply dies. */
    public static void executionStrike(ServerLevel sl, Player player, LivingEntity target, boolean slain) {
        Vec3 head = headOf(target);
        float yaw = yawToward(player.position(), target.position());
        ModelFx fist = SPECTRAL_FIST.color(slain ? 0xF0FF8A80 : 0xF0FFF3D0).scale(Math.max(1.6f, target.getBbHeight()))
                .lifetime(12).fade(0, 6)
                .key(0, pose().up(3f).pitch(-80f))
                .key(4, pose().up(0.2f).pitch(-80f), Ease.IN);
        ParticleShapes.model(sl, fist, head, yaw, 0f, 0f);
        Later.run(sl, 4, () -> {
            SpellFx.shockRing(sl, target.position(), 3.5, 0xE0FFF3D0, 10, 0);
            SpellFx.shockRing(sl, target.position(), 2.2, slain ? 0xE0B03030 : 0xC0C9A24B, 8, 2);
            ParticleShapes.burst(sl, slain ? RED_MOTE : BRASS_SPARK, head, 20, 0.1, 0.3);
            ParticleShapes.burst(sl, DUST, target.position().add(0, 0.2, 0), 8, 0.06, 0.15);
            ParticleShapes.spawn(sl, STAMP.size(0.3f), head.add(0, 0.6, 0), Vec3.ZERO);
            sound(sl, head, ModSounds.ORDER_EXECUTION, 1.5f, slain ? 0.85f : 1.0f);
        });
    }

    /** A blow broke the wind-up: the fist and the ring shatter. */
    public static void executionBroken(ServerLevel sl, Player player, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.burst(sl, WHITE_MOTE, headOf(player).add(0, 1.2, 0), 14, 0.06, 0.18);
        if (target != null) {
            ParticleShapes.clearModels(sl, target, tag);
            ParticleShapes.burst(sl, RED_MOTE, chestOf(target), 8, 0.05, 0.12);
        }
        sound(sl, player.position(), ModSounds.ORDER_REFUSED, 0.8f, 1.2f);
    }

    // =====================================================================================
    // Key 5: Decree (Judge and up)
    // =====================================================================================

    /** The tick a Decree is in force. */
    public static final int DECREE_RISEN = 12;

    /**
     * A Decree is written round 'center': a ring bound with chain opens, the edge is drawn, the word is proclaimed
     * over the caster, and the circle stays for 'ticks'. The Rule picks the color (and the word).
     */
    public static void decree(ServerLevel sl, Player player, Vec3 center, double radius, int ticks, Rule rule, String tag) {
        speak(sl, player, rule.word, 0.6f, false);
        floor(sl, rule == Rule.REALITY ? REALITY_SEAL : rule == Rule.RESURRECTION ? NO_RETURN_SEAL : DECREE_CIRCLE,
                center, radius, (rule.color & 0x00FFFFFF) | 0x90000000, ticks, 0.15f, tag, 0);
        ModelFx edge = SpellFx.RUNE_CIRCLE.color((rule.color & 0x00FFFFFF) | 0xC0000000).scale((float) (radius * 2.0))
                .lifetime(ticks).fade(6, 14).spin(-0.4f).tag(tag)
                .key(0, pose().scale(0.1f).alpha(0f))
                .key(DECREE_RISEN, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.model(sl, edge, center.add(0, 0.08, 0), randomYaw(sl), 0f, 0f);
        int links = (int) Math.min(40, radius * 2.4);
        ParticleShapes.ring(sl, CHAIN, center.add(0, 1.2, 0), radius, links, 0.0);
        lawBurst(sl, center.add(0, 0.8, 0), 10, 0.04, 0.12);
        Later.run(sl, DECREE_RISEN, () -> SpellFx.risingRings(sl, center.add(0, 0.05, 0), radius, 2.0, 2,
                (rule.color & 0x00FFFFFF) | 0xA0000000));
        sound(sl, center, rule == Rule.REALITY ? ModSounds.ORDER_REALITY
                : rule == Rule.RESURRECTION ? ModSounds.ORDER_NO_RETURN : ModSounds.ORDER_PROHIBITION, 1.4f, 1.0f);
        if (player instanceof ServerPlayer sp) status(sp, Status.DECREE, ticks);
    }

    /** Prohibition layers (Disciplinary Paladin): another chain ring a little higher for each layer (2 or 3). */
    public static void decreeLayer(ServerLevel sl, Vec3 center, double radius, int layer, String tag) {
        ModelFx ring = SpellFx.RUNE_CIRCLE.color(0xA0B0B8C8).scale((float) (radius * 2.0 * (1 - layer * 0.08)))
                .lifetime(200).fade(4, 14).spin(layer % 2 == 0 ? 0.5f : -0.5f).tag(tag)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(8, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.model(sl, ring, center.add(0, 0.1 + layer * 0.4, 0), randomYaw(sl), 0f, 0f);
        ParticleShapes.ring(sl, CHAIN, center.add(0, 1.2 + layer * 0.4, 0), radius * 0.9, 24, 0.0);
        sound(sl, center, ModSounds.ORDER_PROHIBITION, 0.8f, 1.0f + layer * 0.12f);
    }

    /** Each second or two while it lasts: Rule letters drift up along its edge. */
    public static void decreeTick(ServerLevel sl, Vec3 center, double radius, Rule rule) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int count = (int) Math.min(24, radius * 1.2);
        for (int i = 0; i < count; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            Vec3 at = center.add(Math.cos(a) * radius, 0.3 + r.nextDouble() * 0.8, Math.sin(a) * radius);
            ParticleShapes.spawn(sl, anyLaw().color(rule.color), at, new Vec3(0, 0.02, 0));
        }
    }

    public static void decreeEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        SpellFx.closingRing(sl, center, radius, 0x808A909C, 10, 0);
        ParticleShapes.ring(sl, CHAIN, center.add(0, 1.0, 0), radius, 16, 0.0);
    }

    /** "You cannot leave this place": a teleport, flight, dash, hiding or stand-in is refused. */
    public static void refused(ServerLevel sl, LivingEntity who) {
        Vec3 at = chestOf(who);
        ParticleShapes.model(sl, PROHIBITION.color(0xE0FF8A80).scale(0.8f).lifetime(14).fade(0, 6)
                .key(0, pose().scale(1.6f).alpha(0f))
                .key(4, pose().scale(1f).alpha(1f), Ease.OUT), at, randomYaw(sl), 0f, 0f);
        ParticleShapes.burst(sl, SHACKLE, at, 4, 0.04, 0.1);
        sound(sl, at, ModSounds.ORDER_REFUSED, 0.9f, wobble(1.0f));
        if (who instanceof Player p) status(p, Status.PROHIBITED, 30);
    }

    /** Concealment broken (Prohibition: Concealment, Whip of Pain, the Court): the frame of the law flashes on it. */
    public static void revealed(ServerLevel sl, LivingEntity who) {
        Vec3 at = chestOf(who);
        flash(sl, RECOGNITION_MARK, at, randomYaw(sl), 0xF0F0D68C, Math.max(1.0f, who.getBbHeight() * 0.9f), 18);
        ParticleShapes.burst(sl, GLASS_SHARD.color(0xE0FFF3D0), at, 10, 0.06, 0.16);
        ParticleShapes.ring(sl, BRASS_MOTE, who.position().add(0, 0.1, 0), Math.max(0.5, who.getBbWidth()), 10, 0.02);
        if (who instanceof Player p) status(p, Status.REVEALED, 40);
    }

    /**
     * A Rule broken inside a Decree: a Violation. count = how many it has broken so far (1, 2, 3...): each one
     * louder and redder; from the second the word VIOLATION is spoken over it.
     */
    public static void violation(ServerLevel sl, LivingEntity who, int count) {
        Vec3 at = chestOf(who);
        float size = Math.min(2.0f, 0.8f + count * 0.25f);
        flash(sl, PROHIBITION, at, randomYaw(sl), count >= 3 ? 0xF0FF4040 : 0xE0FF8A80, size, 16);
        ParticleShapes.burst(sl, RED_MOTE, at, 6 + count * 4, 0.05, 0.12 + count * 0.03);
        if (count >= 2) speak(sl, who, Word.VIOLATION, 0.45f, false);
        sound(sl, at, ModSounds.ORDER_VIOLATION, 0.8f + count * 0.15f, Math.max(0.7f, 1.1f - count * 0.1f));
    }

    /** Imperative: Reality: the plain grid of Reality covers 'radius' for 'ticks'. */
    public static void reality(ServerLevel sl, Player player, Vec3 center, double radius, int ticks, String tag) {
        decree(sl, player, center, radius, ticks, Rule.REALITY, tag);
        ParticleShapes.burst(sl, DUST, center.add(0, 0.5, 0), 14, 0.06, 0.2);
        if (player instanceof ServerPlayer sp) status(sp, Status.REALITY, ticks);
    }

    public static void realityEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        decreeEnd(sl, center, radius, tag);
    }

    /** The tick the Contract is signed (it holds from there). */
    public static final int CONTRACT_SIGNED = 22;

    /**
     * A paper contract appears before the target and is signed, with no say in the matter; it then floats over
     * its head for 'ticks'.
     */
    public static void contract(ServerLevel sl, Player player, LivingEntity target, int ticks, String tag) {
        speak(sl, player, Word.SIGN, 0.45f, false);
        float yaw = yawToward(target.position(), player.position());
        Vec3 before = chestOf(target).add(ahead(yaw).scale(0.9));
        ModelFx paper = CONTRACT_PAPER.scale(0.9f).lifetime(CONTRACT_SIGNED + 4).fade(3, 4)
                .key(0, pose().scale(0.2f).alpha(0f).roll(-30f))
                .key(8, pose().scale(1f).alpha(1f).roll(0f), Ease.OUT_BACK)
                .key(CONTRACT_SIGNED, pose().up(0.6f).scale(0.6f), Ease.IN_OUT);
        ParticleShapes.model(sl, paper, before, yaw, 0f, 0f);
        ParticleShapes.burst(sl, PAPER, before, 4, 0.02, 0.06);
        Later.run(sl, CONTRACT_SIGNED, () -> {
            if (!target.isAlive()) return;
            SpellFx.overHead(sl, target, CONTRACT_PAPER.scale(0.35f).lifetime(ticks).fade(4, 8).spin(3f), 1, tag);
            ParticleShapes.spawn(sl, STAMP.color(0xFFFF8A80), headOf(target).add(0, 0.6, 0), Vec3.ZERO);
            ParticleShapes.burst(sl, BRASS_MOTE, headOf(target), 6, 0.02, 0.06);
        });
        sound(sl, target.position(), ModSounds.ORDER_CONTRACT, 1.0f, 1.0f);
        if (target instanceof Player p) status(p, Status.CONTRACT, ticks);
    }

    /** It tried to harm the Mage: step 1 staggered, 2 struck, 3 held. The seal flares, redder each time. */
    public static void contractViolation(ServerLevel sl, LivingEntity target, int step) {
        violation(sl, target, step);
        ParticleShapes.burst(sl, PAPER, headOf(target).add(0, 0.5, 0), 3 + step * 2, 0.04, 0.1);
        if (step >= 3) ParticleShapes.ring(sl, SHACKLE, chestOf(target), Math.max(0.5, target.getBbWidth()), 6, 0.0);
    }

    /** The Mage struck it: the paper tears and the Contract ends. */
    public static void contractTear(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tag);
        ParticleShapes.burst(sl, PAPER, headOf(target).add(0, 0.5, 0), 10, 0.06, 0.16);
        sound(sl, headOf(target), ModSounds.ORDER_CONTRACT_TEAR, 1.0f, wobble(1.0f));
        if (target instanceof Player p) statusEnd(p, Status.CONTRACT);
    }

    // =====================================================================================
    // Key 6: Punishment / Sword of Judgment (Disciplinary Paladin, Chaos Hunter)
    // =====================================================================================

    /**
     * GUILTY: the Convict is named. A dawn-like halo of brass rises behind the Paladin's head, the Convict's brand
     * hangs over the target and a red ring follows it, all for 'ticks'.
     */
    public static void punishment(ServerLevel sl, Player player, LivingEntity target, int ticks, String tag) {
        speak(sl, player, Word.GUILTY);
        ModelFx halo = DAWN_HALO.color(0xC0F0D68C).scale(1.5f).lifetime(ticks).fade(10, 12).spin(1f).tag(tag)
                .key(0, pose().forward(-0.35f).scale(0.3f).alpha(0f))
                .key(10, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOnHead(sl, halo, player, -0.3, 180f, 0f, 0f);
        SpellFx.risingRings(sl, player.position(), 1.2, 2.2, 3, 0xC0F0D68C);
        if (target != null) {
            SpellFx.overHead(sl, target, CONVICT_BRAND.color(0xE0FF6A60).scale(0.55f).lifetime(ticks).fade(4, 8).spin(4f), 1, tag);
            SpellFx.circleUnder(sl, target, Math.max(0.7, target.getBbWidth()), 0xB0B03030, ticks, tag + "_ring");
            SpellFx.tether(sl, RED_LINE.lifetime(6), () -> SpellFx.middleOf(player, sl), () -> SpellFx.middleOf(target, sl), 16);
            if (target instanceof Player p) status(p, Status.SENTENCED, ticks);
        }
        sound(sl, player.position(), ModSounds.ORDER_PUNISHMENT, 1.2f, 1.0f);
        status(player, Status.CONVICT, ticks);
    }

    /** A hit on the Convict: red sparks off its brand. */
    public static void convictHit(ServerLevel sl, LivingEntity target) {
        ParticleShapes.burst(sl, RED_MOTE, chestOf(target), 6, 0.05, 0.14);
        ParticleShapes.burst(sl, BRASS_SPARK, chestOf(target), 4, 0.06, 0.16);
    }

    /** "Violators will be punished!" (Chaos Hunter): the next hit is sure; the brand blazes. */
    public static void convictPunished(ServerLevel sl, LivingEntity target) {
        flash(sl, CONVICT_BRAND, chestOf(target), randomYaw(sl), 0xF0FF4040, 1.4f, 14);
        ParticleShapes.burst(sl, RED_MOTE, chestOf(target), 14, 0.08, 0.2);
        sound(sl, chestOf(target), ModSounds.ORDER_VIOLATION, 0.9f, 0.8f);
    }

    public static void punishmentEnd(ServerLevel sl, Player player, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        if (target != null) {
            ParticleShapes.clearModels(sl, target, tag);
            ParticleShapes.clearModels(sl, target, tag + "_ring");
            if (target instanceof Player p) statusEnd(p, Status.SENTENCED);
        }
        statusEnd(player, Status.CONVICT);
    }

    // ---------- sneak: Sword of Judgment

    /** The tick the Judgment falls (after the stride). */
    public static final int SWORD_LANDS = 8;

    /**
     * JUDGMENT: one stride from 'from' to before the target (the moving is the ability's), the Sword of Judgment rises
     * over the Chaos Hunter and cuts down; it cannot be dodged.
     */
    public static void swordOfJudgment(ServerLevel sl, Player player, Vec3 from, LivingEntity target) {
        speak(sl, player, Word.JUDGMENT, 0.6f, false);
        ParticleShapes.slashBetween(sl, RUSH.width(0.7f), from.add(0, 1.0, 0), chestOf(player));
        for (int i = 0; i < 6; i++) {
            ParticleShapes.burst(sl, BRASS_MOTE, from.lerp(player.position(), i / 6.0).add(0, 1.0, 0), 3, 0.01, 0.04);
        }
        ModelFx sword = JUDGMENT_SWORD.scale(1.1f).lifetime(SWORD_LANDS + 8).fade(2, 6)
                .key(0, pose().up(1.5f).pitch(-20f).alpha(0f))
                .key(4, pose().up(1.8f).pitch(-30f).alpha(1f), Ease.OUT)
                .key(SWORD_LANDS, pose().up(0.6f).pitch(95f), Ease.IN);
        ParticleShapes.modelOnTurning(sl, sword, player, new Vec3(0, player.getBbHeight() * 0.7, 0), 0f, 0f, 0f);
        sound(sl, player.position(), ModSounds.ORDER_SWORD_OF_JUDGMENT, 1.4f, 1.0f);
        Later.run(sl, SWORD_LANDS, () -> {
            Vec3 at = target != null && target.isAlive() ? chestOf(target)
                    : chestOf(player).add(Spells.flatLook(player).scale(2.0));
            ParticleShapes.slash(sl, JUDGMENT_ARC, at, player.getYRot(), 90f, 0f);
            SpellFx.shockRing(sl, at.add(0, -1.0, 0), 4.0, 0xE0FFF3D0, 10, 0);
            ParticleShapes.burst(sl, BRASS_SPARK, at, 24, 0.12, 0.3);
            lawBurst(sl, at, 8, 0.06, 0.16);
        });
    }

    /** The Judgment strips harmful effects off the one who gives it: gold rings rise round them. */
    public static void judgmentCleanse(ServerLevel sl, Player player) {
        SpellFx.risingRings(sl, player.position(), 0.9, 2.0, 3, 0xC0FFF3D0);
        ParticleShapes.burst(sl, WHITE_MOTE, chestOf(player), 10, 0.03, 0.08);
    }

    // =====================================================================================
    // Key 7: Order (Balancer and up)
    // =====================================================================================

    /** The tick the scales settle level. */
    public static final int BALANCE_SETTLED = 24;

    /**
     * BALANCE: the scales stand between the two, the heavier side (the target's) tips and then settles level; a
     * thread of gold joins them for 'ticks'.
     */
    public static void balance(ServerLevel sl, Player player, LivingEntity target, int ticks, String tag) {
        speak(sl, player, Word.BALANCE);
        Vec3 mid = player.position().lerp(target.position(), 0.5);
        float yaw = yawToward(player.position(), target.position()) + 90f;
        ModelFx stand = SCALES_STAND.scale(1.3f).lifetime(BALANCE_SETTLED + 30).fade(6, 12)
                .key(0, pose().up(-1f).alpha(0f))
                .key(8, pose().up(0f).alpha(1f), Ease.OUT_BACK);
        ModelFx beam = SCALES_BEAM.scale(1.3f).lifetime(BALANCE_SETTLED + 30).fade(6, 12)
                .key(0, pose().up(-1f).roll(-30f).alpha(0f))
                .key(8, pose().up(0f).roll(-30f).alpha(1f), Ease.OUT_BACK)
                .key(16, pose().roll(-18f), Ease.IN_OUT)
                .key(BALANCE_SETTLED, pose().roll(0f), Ease.OUT_BOUNCE);
        ParticleShapes.model(sl, stand, mid, yaw, 0f, 0f);
        ParticleShapes.model(sl, beam, mid.add(0, 1.95, 0), yaw, 0f, 0f);
        SpellFx.tether(sl, GOLD_THREAD, player, target, ticks);
        Later.run(sl, BALANCE_SETTLED, () -> {
            ParticleShapes.burst(sl, BRASS_MOTE, mid.add(0, 2.0, 0), 12, 0.03, 0.1);
            SpellFx.shockRing(sl, mid, 2.0, 0xC0F0D68C, 8, 0);
        });
        sound(sl, mid, ModSounds.ORDER_BALANCE, 1.0f, 1.0f);
        status(player, Status.BALANCE, ticks);
    }

    public static void balanceEnd(ServerLevel sl, Player player, LivingEntity target, String tag) {
        if (target != null) ParticleShapes.burst(sl, BRASS_MOTE, chestOf(target), 6, 0.02, 0.06);
        statusEnd(player, Status.BALANCE);
    }

    /** The tick the ring of Order has risen. */
    public static final int DIVIDE_RISEN = 14;

    /** A ring of Order 'radius' blocks round 'center': a wall of brass bars rises and stays for 'ticks'. */
    public static void divideBattlefield(ServerLevel sl, Player player, Vec3 center, double radius, int ticks, String tag) {
        wallRing(sl, ORDER_WALL, center, radius, 3.0, 0xB0C9A24B, ticks, DIVIDE_RISEN, tag);
        floor(sl, ORDER_SEAL, center, 2.5, 0x90C9A24B, ticks, 0.3f, tag, 0);
        SpellFx.steadyRing(sl, center, radius, 0xE0FFF3D0, DIVIDE_RISEN, 0);
        Later.run(sl, DIVIDE_RISEN, () -> SpellFx.shockRing(sl, center, radius + 1.0, 0xC0F0D68C, 8, 0));
        sound(sl, center, ModSounds.ORDER_DIVIDE, 1.5f, 1.0f);
        if (player instanceof ServerPlayer sp) status(sp, Status.DIVIDED, ticks);
    }

    /** A projectile or teleport stopped at the edge. */
    public static void divideBlocked(ServerLevel sl, Vec3 at) {
        ParticleShapes.burst(sl, BRASS_SPARK, at, 10, 0.06, 0.18);
        ParticleShapes.spawn(sl, PROHIBIT_MARK, at, Vec3.ZERO);
        sound(sl, at, ModSounds.ORDER_REFUSED, 0.6f, 1.3f);
    }

    /** One too strong for the scales is thrown out of the ring. */
    public static void thrownOut(ServerLevel sl, LivingEntity who) {
        ParticleShapes.burst(sl, BRASS_SPARK, chestOf(who), 14, 0.1, 0.25);
        flash(sl, PROHIBITION, chestOf(who), randomYaw(sl), 0xE0F0D68C, 1.2f, 14);
        sound(sl, who.position(), ModSounds.ORDER_EXILE, 0.8f, 1.2f);
    }

    public static void divideEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        SpellFx.closingRing(sl, center, radius, 0x80C9A24B, 12, 0);
        jurisdictionTick(sl, center, radius);
    }

    /** NO RETURN: "Resurrection of the dead is prohibited here!" A shut door is laid on the ground for 'ticks'. */
    public static void lawNoReturn(ServerLevel sl, Player player, Vec3 center, double radius, int ticks, String tag) {
        decree(sl, player, center, radius, ticks, Rule.RESURRECTION, tag);
    }

    /** A death ward, totem or revival on an enemy inside is voided: the door flashes shut on it. */
    public static void revivalVoided(ServerLevel sl, LivingEntity who) {
        Vec3 at = chestOf(who);
        ModelFx door = NO_RETURN_SEAL.color(0xF0FF6A60).scale(1.4f).lifetime(18).fade(0, 8)
                .key(0, pose().scale(0.4f).alpha(0f))
                .key(5, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.model(sl, door, at, randomYaw(sl), 90f, 0f);
        ParticleShapes.burst(sl, RED_MOTE, at, 10, 0.04, 0.1);
        sound(sl, at, ModSounds.ORDER_NO_RETURN, 0.6f, 1.3f);
        if (who instanceof Player p) status(p, Status.NO_RETURN, 40);
    }

    /** A marionette inside stops moving: chains drop on it. */
    public static void puppetFrozen(ServerLevel sl, LivingEntity puppet) {
        ParticleShapes.ring(sl, CHAIN, chestOf(puppet), Math.max(0.5, puppet.getBbWidth()), 8, 0.0);
        ParticleShapes.spawn(sl, SHACKLE, headOf(puppet), Vec3.ZERO);
    }

    /**
     * Order Proxy: the Brass Pillar rises huge behind the Hand of Order and white-gold light streams down it for
     * 'ticks' (proxyTick every few ticks). When it ends: proxyEnd.
     */
    public static void orderProxy(ServerLevel sl, Player player, int ticks, String tag) {
        ModelFx pillar = BRASS_PILLAR.scale(3.2f).lifetime(ticks + 10).fade(4, 14).tag(tag)
                .key(0, pose().forward(-1.6f).up(-6f).alpha(0f))
                .key(12, pose().up(0f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOnTurning(sl, pillar, player, Vec3.ZERO, 0f, 0f, 0f);
        ModelFx halo = DAWN_HALO.color(0xB0FFF3D0).scale(2.4f).lifetime(ticks).fade(8, 10).spin(2f).tag(tag);
        ParticleShapes.modelOnHead(sl, halo, player, -0.2, 180f, 0f, 0f);
        SpellFx.shockRing(sl, player.position(), 8.0, 0xE0FFF3D0, 14, 0);
        SpellFx.risingRings(sl, player.position(), 1.4, 3.0, 4, 0xC0F0D68C);
        ParticleShapes.burst(sl, BRASS_SPARK, chestOf(player), 24, 0.1, 0.3);
        sound(sl, player.position(), ModSounds.ORDER_PROXY, 1.6f, 1.0f);
        status(player, Status.PROXY, ticks);
    }

    public static void proxyTick(ServerLevel sl, Player player) {
        ParticleShapes.ring(sl, BRASS_MOTE, player.position().add(0, 0.1, 0), 0.9, 6, 0.0);
        ParticleShapes.spawn(sl, anyLaw(), chestOf(player).add(Spells.rightOf(player).scale(0.6)), new Vec3(0, 0.05, 0));
    }

    /** It ends and the proxy is spent: the Pillar sinks, dust, a long weariness. */
    public static void proxyEnd(ServerLevel sl, Player player, String tag) {
        ParticleShapes.clearModels(sl, player, tag);
        ParticleShapes.burst(sl, DUST, player.position().add(0, 0.5, 0), 10, 0.03, 0.08);
        sound(sl, player.position(), ModSounds.ORDER_PROXY_END, 1.0f, 1.0f);
        statusEnd(player, Status.PROXY);
        status(player, Status.SPENT, 600);
    }

    /** Order restores itself within 'radius': nested squares spread out and the violet of Disorder is snuffed. */
    public static void quellDisorder(ServerLevel sl, Player player, double radius) {
        ModelFx seal = QUELL_SEAL.color(0xE0FFF3D0).lifetime(24).fade(0, 10).spin(4f)
                .key(0, pose().scale(1f))
                .key(20, pose().scale((float) (radius * 2.0)), Ease.OUT);
        ParticleShapes.model(sl, seal, player.position().add(0, 0.06, 0), randomYaw(sl), 0f, 0f);
        SpellFx.steadyRing(sl, player.position(), radius, 0xC0FFF3D0, 20, 0);
        lawBurst(sl, chestOf(player), 12, 0.06, 0.2);
        sound(sl, player.position(), ModSounds.ORDER_QUELL, 1.5f, 1.0f);
    }

    /** What an enemy made with abilities is quelled on it: Disorder smoke pulled out and snuffed. */
    public static void quelled(ServerLevel sl, LivingEntity who) {
        Vec3 at = chestOf(who);
        ParticleShapes.burst(sl, DISORDER_SMOKE, at, 6, 0.03, 0.08);
        ParticleShapes.burst(sl, VIOLET_MOTE, at, 8, 0.05, 0.12);
        ParticleShapes.spawn(sl, STAMP, headOf(who).add(0, 0.4, 0), Vec3.ZERO);
    }

    /** An ally cleansed of harmful effects. */
    public static void cleansed(ServerLevel sl, LivingEntity ally) {
        SpellFx.risingRings(sl, ally.position(), Math.max(0.6, ally.getBbWidth()), 2.0, 2, 0xC0FFF3D0);
        ParticleShapes.burst(sl, WHITE_MOTE, chestOf(ally), 6, 0.02, 0.06);
    }

    // ---------- the Ancient Court of Judgment (Justiciar)

    /** The tick the Court is in session. */
    public static final int COURT_RISEN = 24;

    /**
     * ORDER: the Ancient Court of Judgment. A floor of Order 'radius' wide, a wall of brass bars round its edge in
     * two tiers (a dome), the Brass Pillar behind the Justiciar, all for 'ticks'. clause(...) every 10 seconds.
     */
    public static void court(ServerLevel sl, Player player, Vec3 center, double radius, int ticks, String tag) {
        speak(sl, player, Word.ORDER, 0.8f, false);
        floor(sl, COURT_FLOOR, center, Math.min(radius, 40.0), 0xA0C9A24B, ticks, 0.05f, tag, 0);
        floor(sl, ORDER_SEAL, center, 3.5, 0xE0FFF3D0, ticks, -0.3f, tag, 6);
        wallRing(sl, ORDER_WALL, center, radius, 6.0, 0x90C9A24B, ticks, COURT_RISEN, tag);
        wallRing(sl, ORDER_WALL, center.add(0, 6.0, 0), radius * 0.8, 5.0, 0x70FFF3D0, ticks, COURT_RISEN + 8, tag);
        wallRing(sl, ORDER_WALL, center.add(0, 11.0, 0), radius * 0.45, 4.0, 0x50FFF3D0, ticks, COURT_RISEN + 16, tag);
        ModelFx pillar = BRASS_PILLAR.scale(4.5f).lifetime(ticks).fade(10, 20).tag(tag)
                .key(0, pose().up(-9f).alpha(0f))
                .key(COURT_RISEN, pose().up(0f).alpha(1f), Ease.OUT);
        float yaw = player.getYRot();
        ParticleShapes.model(sl, pillar, player.position().subtract(ahead(yaw).scale(3.0)), yaw, 0f, 0f);
        SpellFx.shockRing(sl, center, radius, 0xE0FFF3D0, COURT_RISEN, 0);
        SpellFx.risingRings(sl, center, 3.0, 5.0, 4, 0xC0F0D68C);
        sound(sl, center, ModSounds.ORDER_COURT, 2.5f, 1.0f);
        status(player, Status.COURT, ticks);
    }

    /** Clause 'index' (1 to 7, in the order of docs/HAND_OF_ORDER.md) is written: a gavel strikes, law rains down. */
    public static void clause(ServerLevel sl, Vec3 center, double radius, int index) {
        ModelFx gavel = GAVEL.color(0xFFF0D68C).scale(2.5f).lifetime(18).fade(2, 8)
                .key(0, pose().pitch(-60f).alpha(0f))
                .key(6, pose().pitch(-70f).alpha(1f), Ease.OUT)
                .key(10, pose().pitch(10f), Ease.IN)
                .key(13, pose().pitch(0f), Ease.OUT);
        ParticleShapes.model(sl, gavel, center.add(0, 4.0, 0), randomYaw(sl), 0f, 0f);
        Later.run(sl, 10, () -> {
            SpellFx.shockRing(sl, center, radius * 0.7, 0xC0F0D68C, 14, 0);
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (int i = 0; i < 24; i++) {
                double a = r.nextDouble() * Math.PI * 2;
                double d = r.nextDouble() * radius * 0.8;
                ParticleShapes.spawn(sl, anyLaw(), center.add(Math.cos(a) * d, 6 + r.nextDouble() * 4, Math.sin(a) * d),
                        new Vec3(0, -0.15, 0));
            }
            sound(sl, center, ModSounds.ORDER_CLAUSE, 2.0f, 1.0f + index * 0.02f);
        });
    }

    /**
     * An enemy breaks a clause and is Judged; breach = 1 (a warning), 2, 3 or 4 (Death). The sentence grows with
     * each: a stamp, iron, a falling gavel, and at the last a great gavel and the word DEATH.
     */
    public static void judged(ServerLevel sl, LivingEntity who, int breach) {
        Vec3 at = chestOf(who);
        ParticleShapes.spawn(sl, STAMP, headOf(who).add(0, 0.5, 0), Vec3.ZERO);
        if (who instanceof Player p) status(p, Status.JUDGED, 60);
        if (breach <= 1) {
            flash(sl, PROHIBITION, at, randomYaw(sl), 0xE0F0D68C, 1.0f, 14);
            sound(sl, at, ModSounds.ORDER_VIOLATION, 0.8f, 1.1f);
            return;
        }
        ParticleShapes.ring(sl, SHACKLE, at, Math.max(0.6, who.getBbWidth()), 6 + breach * 2, 0.0);
        float size = breach >= 4 ? 3.0f : breach == 3 ? 1.8f : 1.1f;
        ModelFx gavel = GAVEL.color(breach >= 4 ? 0xFFFF8A80 : 0xFFF0D68C).scale(size).lifetime(16).fade(1, 6)
                .key(0, pose().pitch(-80f).alpha(0f))
                .key(4, pose().pitch(-80f).alpha(1f))
                .key(8, pose().pitch(15f), Ease.IN)
                .key(10, pose().pitch(5f), Ease.OUT);
        float yaw = randomYaw(sl);
        ParticleShapes.model(sl, gavel, headOf(who).add(0, size * 0.4, 0).subtract(ahead(yaw).scale(size * 0.9)), yaw, 0f, 0f);
        Later.run(sl, 8, () -> {
            SpellFx.shockRing(sl, who.position(), 1.5 + breach, breach >= 4 ? 0xE0B03030 : 0xC0F0D68C, 8, 0);
            ParticleShapes.burst(sl, breach >= 4 ? RED_MOTE : BRASS_SPARK, at, 8 + breach * 5, 0.08, 0.2 + breach * 0.04);
            sound(sl, at, ModSounds.ORDER_SENTENCE, 0.8f + breach * 0.2f, Math.max(0.7f, 1.2f - breach * 0.1f));
        });
        if (breach >= 4) speak(sl, who, Word.DEATH, 0.6f, false);
    }

    /** The Court adjourns (cast again, or its time is up). */
    public static void adjourn(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tag);
        SpellFx.closingRing(sl, center, radius, 0xA0C9A24B, 20, 0);
        ParticleShapes.burst(sl, BRASS_MOTE, center.add(0, 1.0, 0), 20, 0.04, 0.12);
        sound(sl, center, ModSounds.ORDER_ADJOURN, 2.0f, 1.0f);
    }

    // =====================================================================================
    // The Brass Pillar form (Imperative Mage): only a visual
    // =====================================================================================

    /** The incomplete Mythical Creature form: the Brass Pillar rises behind the Mage and stays for 'ticks'. */
    public static void brassPillar(ServerLevel sl, Player player, int ticks, String tag) {
        ModelFx pillar = BRASS_PILLAR.scale(1.8f).lifetime(ticks).fade(4, 12).tag(tag)
                .key(0, pose().forward(-1.0f).up(-3.6f).alpha(0f))
                .key(14, pose().up(0f).alpha(1f), Ease.OUT);
        ParticleShapes.modelOnTurning(sl, pillar, player, Vec3.ZERO, 0f, 0f, 0f);
        ParticleShapes.burst(sl, DUST, player.position().subtract(Spells.flatLook(player)).add(0, 0.2, 0), 8, 0.03, 0.08);
        sound(sl, player.position(), ModSounds.ORDER_PILLAR, 1.2f, 1.0f);
    }

    // =====================================================================================
    // Passives that show
    // =====================================================================================

    /** Order (Arbiter): now and then a mote of brass circles the Arbiter, enough to keep weak things off. */
    public static void orderPresence(ServerLevel sl, Player player) {
        double a = sl.getRandom().nextDouble() * Math.PI * 2;
        ParticleShapes.spawn(sl, BRASS_MOTE, player.position().add(Math.cos(a) * 0.7, 0.3, Math.sin(a) * 0.7), new Vec3(0, 0.03, 0));
    }

    /** Sheriff's Intuition: something starts hunting from out of sight. Only the Sheriff hears it. */
    public static void intuition(ServerPlayer owner) {
        soundFor(owner, owner.getEyePosition(), ModSounds.ORDER_INTUITION, 0.7f, 1.0f);
    }

    /** Sense of Order (Hand of Order): someone cast an ability or wrote a Rule at 'at'. Only the owner sees and hears. */
    public static void senseOrder(ServerPlayer owner, Vec3 at) {
        ParticleShapes.spawnFor(owner, GAVEL_MARK, at.add(0, 1.0, 0), Vec3.ZERO);
        soundFor(owner, at, ModSounds.ORDER_SENSE_ORDER, 0.6f, 1.0f);
    }

    /** Disorder Sense (Chaos Hunter): an enemy of high Disorder smoulders violet, for the owner alone. */
    public static void disorderSense(ServerPlayer owner, LivingEntity who) {
        Vec3 at = chestOf(who);
        for (int i = 0; i < 3; i++) {
            ParticleShapes.spawnFor(owner, VIOLET_MOTE, at.add((owner.getRandom().nextDouble() - 0.5) * who.getBbWidth(),
                    (owner.getRandom().nextDouble() - 0.5) * who.getBbHeight() * 0.6, 0), new Vec3(0, 0.03, 0));
        }
    }

    /** Beyonder Sense (Imperative Mage): a Beyonder is told from an ordinary thing on sight. A brass mark over it. */
    public static void beyonderSense(ServerPlayer owner, LivingEntity who) {
        ParticleShapes.spawnFor(owner, STAMP.size(0.1f), headOf(who).add(0, 0.5, 0), Vec3.ZERO);
    }

    /** Self-Restraint (Disciplinary Paladin): a mind effect on the Paladin is shrugged off faster. */
    public static void selfRestraint(ServerLevel sl, Player player) {
        ParticleShapes.ring(sl, CHAIN, chestOf(player), 0.6, 6, 0.04);
    }

    // =====================================================================================
    // Melee combo (docs/HAND_OF_ORDER.md, "Melee combo")
    // =====================================================================================

    private static final SlashFx COMBO_RING = SlashFx.arc("slash/streaks")
            .color(0xEEC9A24B).headColor(0xFFFFF3D0).tailColor(0xA03E434D).core(0xFFFFFFFF)
            .radius(1.6f).arc(300f).width(0.5f).layers(2).spread(0.1f).lifetime(8).sweep(3).flat();
    private static final SlashFx COMBO_CROSS = SlashFx.arc("slash/streaks")
            .color(0xE0A8C8FF).headColor(0xFFE8F2FF).core(0xFFFFFFFF)
            .radius(1.1f).arc(130f).width(0.4f).layers(2).spread(0.1f).lifetime(7).sweep(2);
    private static final SlashFx COMBO_GAVEL = SlashFx.line("slash/edge")
            .color(0xF0FFF3D0).core(0xFFFFFFFF).width(0.5f).taper(SlashFx.Taper.CRESCENT).lifetime(8).sweep(2);

    /**
     * Hit 'step' (0, 1, 2) of the melee combo on 'target'. 0: a brass ring sweeps round with branding-iron sparks.
     * 1: a crossed pair of lightning slashes on the target. 2: either a ring of Rule glyphs spreading on the ground,
     * or a straight slash slamming down on the target like a gavel.
     */
    public static void combo(ServerLevel sl, Player player, LivingEntity target, int step) {
        Vec3 chest = chestOf(player);
        switch (step) {
            case 0 -> {
                ParticleShapes.slash(sl, COMBO_RING, chest, player.getYRot(), 0f, 0f);
                ParticleShapes.ring(sl, BRAND, chest, 1.5, 6, 0.05);
                ParticleShapes.burst(sl, BRASS_SPARK, chestOf(target), 6, 0.05, 0.15);
                sound(sl, chest, ModSounds.ORDER_ORDER_SWING, 0.8f, wobble(1.0f));
            }
            case 1 -> {
                Vec3 at = chestOf(target);
                float yaw = yawToward(player.position(), target.position());
                ParticleShapes.slash(sl, COMBO_CROSS, at, yaw, 0f, 45f);
                ParticleShapes.slash(sl, COMBO_CROSS, at, yaw, 0f, -45f);
                ParticleShapes.burst(sl, LIGHTNING_FORK, at, 6, 0.05, 0.15);
                sound(sl, at, ModSounds.ORDER_LASH_HIT, 0.7f, wobble(1.2f));
            }
            default -> {
                if (sl.getRandom().nextBoolean()) {
                    floor(sl, DECREE_CIRCLE, player.position(), 2.2, 0xD0F0D68C, 16, 6f, null, 0);
                    ParticleShapes.ring(sl, anyLaw(), player.position().add(0, 0.2, 0), 1.0, 12, 0.18);
                    sound(sl, chest, ModSounds.ORDER_ORDER_SWING, 0.8f, wobble(0.85f));
                } else {
                    Vec3 at = chestOf(target);
                    ParticleShapes.slashBetween(sl, COMBO_GAVEL, at.add(0, 1.8, 0), target.position());
                    SpellFx.shockRing(sl, target.position(), 1.6, 0xC0F0D68C, 6, 0);
                    ParticleShapes.spawn(sl, GAVEL_MARK, headOf(target).add(0, 0.4, 0), Vec3.ZERO);
                    sound(sl, at, ModSounds.ORDER_GAVEL_SLAM, 0.8f, wobble(1.0f));
                }
                ParticleShapes.burst(sl, BRASS_SPARK, chestOf(target), 6, 0.08, 0.18);
            }
        }
    }

    // =====================================================================================
    // /orderfx: every display, to look at in game
    // =====================================================================================

    @FunctionalInterface
    private interface Show {
        void play(ServerLevel sl, Player viewer, LivingEntity target, Vec3 spot);
    }

    /** onViewer = it is shown on the viewer (worn), not on a spot or a target. */
    private record Case(String name, int ticks, boolean onViewer, Show show) {}

    private static String tagFor(String name) {
        return "ho_fx_" + name + "_" + UUID.randomUUID();
    }

    private static final List<Case> CASES = List.of(
            new Case("words", 140, true, (sl, v, t, spot) -> {
                Word[] all = Word.values();
                for (int i = 0; i < all.length; i++) {
                    Word w = all[i];
                    Later.run(sl, i * 8, () -> speak(sl, v, w, 0.55f, false));
                }
            }),
            new Case("halt", 50, false, (sl, v, t, spot) -> {
                halt(sl, v, t == v ? null : t);
                if (t != v) Later.run(sl, HALT_LANDS, () -> halted(sl, t, 40, tagFor("halt")));
                if (t != v) Later.run(sl, HALT_LANDS + 6, () -> forgets(sl, t));
            }),
            new Case("presence", 40, true, (sl, v, t, spot) -> {
                presence(sl, v, 8, false);
                if (t != v) Later.run(sl, 6, () -> cowed(sl, t, 40, tagFor("cowed")));
            }),
            new Case("intimidation", 40, true, (sl, v, t, spot) -> presence(sl, v, 12, true)),
            new Case("jurisdiction", 140, true, (sl, v, t, spot) -> {
                String tag = tagFor("jur");
                Vec3 c = v.position();
                jurisdiction(sl, v, c, 12, 130, tag);
                for (int i = 1; i < 6; i++) Later.run(sl, i * 20, () -> jurisdictionTick(sl, c, 12));
                if (v instanceof ServerPlayer sp && t != v) Later.run(sl, 30, () -> jurisdictionEnter(sp, t));
                Later.run(sl, 130, () -> jurisdictionEnd(sl, c, 12, tag));
            }),
            new Case("recognition", 100, false, (sl, v, t, spot) -> {
                if (!(v instanceof ServerPlayer sp) || t == v) return;
                String tag = tagFor("rec");
                recognition(sp, t, 90, tag);
                for (int i = 1; i < 5; i++) Later.run(sl, i * 20, () -> recognitionTrack(sp, t));
                Later.run(sl, 90, () -> recognitionEnd(sp, t, tag));
            }),
            new Case("psychic_lashing", 90, true, (sl, v, t, spot) -> {
                String tag = tagFor("lash");
                psychicLashing(sl, v, 80, tag);
                for (int i = 4; i < 80; i += 4) Later.run(sl, i, () -> lashingTick(sl, v));
                if (t != v) for (int i = 0; i < 3; i++) Later.run(sl, 20 + i * 15, () -> lashHit(sl, v, t));
                Later.run(sl, 80, () -> lashingEnd(sl, v, tag));
            }),
            new Case("psychic_piercing", 30, false, (sl, v, t, spot) -> {
                Vec3 end = t == v ? v.getEyePosition().add(v.getLookAngle().scale(12)) : chestOf(t);
                psychicPiercing(sl, v, end, t == v ? List.of() : List.of(t));
                if (t != v) piercingHit(sl, t, false);
            }),
            new Case("whip_of_pain", 100, false, (sl, v, t, spot) -> {
                if (t == v) return;
                whipOfPain(sl, v, t);
                Later.run(sl, WHIP_LANDS, () -> {
                    trembling(sl, t, 80, tagFor("tremble"));
                    truthOut(sl, t);
                });
            }),
            new Case("imprison", 130, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("imprison");
                imprison(sl, v, t, 60, tag);
                Later.run(sl, 70, () -> release(sl, t, tag));
            }),
            new Case("confinement", 100, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("cell");
                confinement(sl, v, t, 80, tag);
                Later.run(sl, 86, () -> imprisonEnd(sl, t, tag));
            }),
            new Case("exile", 40, false, (sl, v, t, spot) -> {
                if (t == v) return;
                exile(sl, v, t, t.position().add(Spells.flatLook(v).scale(12)));
            }),
            new Case("exile_void", 110, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("void");
                Vec3 at = chestOf(t);
                speak(sl, v, Word.EXILE);
                exileVoid(sl, t, 80, tag);
                Later.run(sl, 90, () -> exileReturn(sl, at, tag));
            }),
            new Case("exile_hurricane", 40, false, (sl, v, t, spot) -> exileHurricane(sl, t == v ? spot : t.position(), 5)),
            new Case("death", 40, false, (sl, v, t, spot) ->
                    death(sl, v, v.position().subtract(Spells.flatLook(v).scale(6)), t == v ? null : t, false)),
            new Case("death_wound", 40, false, (sl, v, t, spot) ->
                    death(sl, v, v.position().subtract(Spells.flatLook(v).scale(6)), t == v ? null : t, true)),
            new Case("maintain_secrecy", 100, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("secrecy");
                maintainSecrecy(sl, v, t, 90, tag);
                Later.run(sl, 90, () -> secrecyEnd(sl, t, tag));
            }),
            new Case("deprivation", 80, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("deprive");
                deprivation(sl, v, t, 3);
                Later.run(sl, DEPRIVED + 6, () -> deprived(sl, t, 50, tag));
                Later.run(sl, DEPRIVED + 56, () -> deprivedEnd(sl, t, tag));
            }),
            new Case("execution", 60, false, (sl, v, t, spot) -> {
                if (t == v) return;
                execution(sl, v, t, tagFor("exec"));
                Later.run(sl, EXECUTION_LANDS, () -> executionStrike(sl, v, t, false));
            }),
            new Case("execution_slain", 60, false, (sl, v, t, spot) -> {
                if (t == v) return;
                execution(sl, v, t, tagFor("exec"));
                Later.run(sl, EXECUTION_LANDS, () -> executionStrike(sl, v, t, true));
            }),
            new Case("execution_broken", 40, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("exec");
                execution(sl, v, t, tag);
                Later.run(sl, 15, () -> executionBroken(sl, v, t, tag));
            }),
            new Case("prohibition_passage", 130, false, (sl, v, t, spot) -> {
                String tag = tagFor("decree");
                decree(sl, v, spot, 6, 120, Rule.PASSAGE, tag);
                for (int i = 1; i < 6; i++) Later.run(sl, i * 20, () -> decreeTick(sl, spot, 6, Rule.PASSAGE));
                if (t != v) Later.run(sl, 30, () -> refused(sl, t));
                Later.run(sl, 120, () -> decreeEnd(sl, spot, 6, tag));
            }),
            new Case("prohibition_concealment", 130, false, (sl, v, t, spot) -> {
                String tag = tagFor("decree");
                decree(sl, v, spot, 6, 120, Rule.CONCEALMENT, tag);
                if (t != v) Later.run(sl, DECREE_RISEN, () -> revealed(sl, t));
                Later.run(sl, 120, () -> decreeEnd(sl, spot, 6, tag));
            }),
            new Case("prohibition_layers", 130, false, (sl, v, t, spot) -> {
                String tag = tagFor("decree");
                decree(sl, v, spot, 6, 120, Rule.PASSAGE, tag);
                Later.run(sl, 20, () -> decreeLayer(sl, spot, 6, 2, tag));
                Later.run(sl, 40, () -> decreeLayer(sl, spot, 6, 3, tag));
                Later.run(sl, 120, () -> decreeEnd(sl, spot, 6, tag));
            }),
            new Case("violations", 90, false, (sl, v, t, spot) -> {
                if (t == v) return;
                for (int i = 1; i <= 4; i++) {
                    int n = i;
                    Later.run(sl, (i - 1) * 20, () -> violation(sl, t, n));
                }
            }),
            new Case("imperative_reality", 130, false, (sl, v, t, spot) -> {
                String tag = tagFor("reality");
                reality(sl, v, spot, 8, 120, tag);
                Later.run(sl, 120, () -> realityEnd(sl, spot, 8, tag));
            }),
            new Case("contract", 140, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("contract");
                contract(sl, v, t, 110, tag);
                for (int i = 1; i <= 3; i++) {
                    int step = i;
                    Later.run(sl, CONTRACT_SIGNED + i * 20, () -> contractViolation(sl, t, step));
                }
                Later.run(sl, CONTRACT_SIGNED + 100, () -> contractTear(sl, t, tag));
            }),
            new Case("punishment", 130, false, (sl, v, t, spot) -> {
                String tag = tagFor("convict");
                LivingEntity convict = t == v ? null : t;
                punishment(sl, v, convict, 120, tag);
                if (convict != null) {
                    for (int i = 0; i < 3; i++) Later.run(sl, 20 + i * 15, () -> convictHit(sl, convict));
                    Later.run(sl, 80, () -> convictPunished(sl, convict));
                }
                Later.run(sl, 120, () -> punishmentEnd(sl, v, convict, tag));
            }),
            new Case("sword_of_judgment", 40, false, (sl, v, t, spot) -> {
                swordOfJudgment(sl, v, v.position().subtract(Spells.flatLook(v).scale(8)), t == v ? null : t);
                Later.run(sl, SWORD_LANDS + 2, () -> judgmentCleanse(sl, v));
            }),
            new Case("balance", 110, false, (sl, v, t, spot) -> {
                if (t == v) return;
                String tag = tagFor("balance");
                balance(sl, v, t, 100, tag);
                Later.run(sl, 100, () -> balanceEnd(sl, v, t, tag));
            }),
            new Case("divide_battlefield", 160, true, (sl, v, t, spot) -> {
                String tag = tagFor("divide");
                Vec3 c = v.position();
                divideBattlefield(sl, v, c, 10, 150, tag);
                Later.run(sl, 40, () -> divideBlocked(sl, c.add(ahead(v.getYRot()).scale(10)).add(0, 1.5, 0)));
                if (t != v) Later.run(sl, 60, () -> thrownOut(sl, t));
                Later.run(sl, 150, () -> divideEnd(sl, c, 10, tag));
            }),
            new Case("law_no_return", 130, false, (sl, v, t, spot) -> {
                String tag = tagFor("noreturn");
                lawNoReturn(sl, v, spot, 8, 120, tag);
                if (t != v) {
                    Later.run(sl, 30, () -> revivalVoided(sl, t));
                    Later.run(sl, 50, () -> puppetFrozen(sl, t));
                }
                Later.run(sl, 120, () -> decreeEnd(sl, spot, 8, tag));
            }),
            new Case("order_proxy", 140, true, (sl, v, t, spot) -> {
                String tag = tagFor("proxy");
                orderProxy(sl, v, 120, tag);
                for (int i = 4; i < 120; i += 4) Later.run(sl, i, () -> proxyTick(sl, v));
                Later.run(sl, 120, () -> {
                    proxyEnd(sl, v, tag);
                    statusEnd(v, Status.SPENT);
                });
            }),
            new Case("quell_disorder", 50, true, (sl, v, t, spot) -> {
                quellDisorder(sl, v, 12);
                if (t != v) Later.run(sl, 10, () -> quelled(sl, t));
                Later.run(sl, 12, () -> cleansed(sl, v));
            }),
            new Case("court", 400, true, (sl, v, t, spot) -> {
                String tag = tagFor("court");
                Vec3 c = v.position();
                court(sl, v, c, 20, 380, tag);
                for (int i = 1; i <= 7; i++) {
                    int n = i;
                    Later.run(sl, COURT_RISEN + (i - 1) * 40, () -> {
                        clause(sl, c, 20, n);
                        courtClauses(v, n, 380 - (COURT_RISEN + (n - 1) * 40));
                    });
                }
                if (t != v) for (int b = 1; b <= 4; b++) {
                    int breach = b;
                    Later.run(sl, COURT_RISEN + 60 + b * 50, () -> {
                        if (t.isAlive()) judged(sl, t, breach);
                    });
                }
                Later.run(sl, 380, () -> {
                    adjourn(sl, c, 20, tag);
                    courtClausesHide(v);
                });
            }),
            new Case("brass_pillar", 110, true, (sl, v, t, spot) -> brassPillar(sl, v, 100, tagFor("pillar"))),
            new Case("passives", 80, true, (sl, v, t, spot) -> {
                for (int i = 0; i < 8; i++) Later.run(sl, i * 5, () -> orderPresence(sl, v));
                if (v instanceof ServerPlayer sp) {
                    intuition(sp);
                    Later.run(sl, 20, () -> senseOrder(sp, spot));
                    if (t != v) {
                        for (int i = 0; i < 6; i++) Later.run(sl, 30 + i * 4, () -> disorderSense(sp, t));
                        Later.run(sl, 50, () -> beyonderSense(sp, t));
                    }
                }
                Later.run(sl, 60, () -> selfRestraint(sl, v));
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
                courtClauses(v, 7, 90);
                Later.run(sl, 90, () -> courtClausesHide(v));
            }));

    /** The names /orderfx knows. */
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
        event.getDispatcher().register(Commands.literal("orderfx")
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
