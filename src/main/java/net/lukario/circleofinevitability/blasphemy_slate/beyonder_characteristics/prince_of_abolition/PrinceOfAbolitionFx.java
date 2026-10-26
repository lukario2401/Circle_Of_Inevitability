package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.lukario.circleofinevitability.combat.HudCounter;
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
 * How the Prince Of Abolition (the Black Emperor pathway) looks and sounds: every key, both casts, every wheel entry,
 * the mausoleums, the Order counter, what grows at higher Sequences and the passives that show. Visual only: nothing
 * here hurts, holds, seals, hides, revives or moves anyone. The ability code does that, at the ticks named by the
 * constants below (..._LANDS, ..._SLAMS, ..._RISEN...), with Later.run. See docs/PRINCE_OF_ABOLITION.md.
 *
 * ---------- See them in game ----------
 *   /abolitionfx all            every display one after another (/abolitionfx all 3 = three times over)
 *   /abolitionfx seal           just that one (press TAB after /abolitionfx for the names)
 * Displays that need a target use the nearest mob within 12 blocks, or you when there is none.
 *
 * ---------- Use them in an ability ----------
 *   Key 1  verdict(sl, player, target) (it strikes at VERDICT_LANDS), compromised(sl, target, ticks, tag),
 *              compromisedEnd(sl, target, tag)
 *          sneak: brainwash(sl, player, target, ticks, tag) (it lands at WORDS_LAND), brainwashEnd(sl, target, tag)
 *   Key 2  bruteForce(sl, player, target) (it lands at BRUTE_LANDS)
 *          sneak: ironWill(sl, player, ticks, tag), mindShrug(sl, player), ironWillEnd(sl, player, tag)
 *   Key 3  the wheel's pictures: WHEEL_WEAKEN, WHEEL_CHARM, WHEEL_ARROGANCE, WHEEL_CONNECT, WHEEL_COIN_SHOT
 *          bribe(sl, player, target, Bribe.X, actual) (it lands at BRIBE_LANDS), bribed(sl, target, Bribe.X, ticks, tag),
 *              bribedEnd(sl, target, tag), bribeSpread(sl, center, radius, Bribe.X), charmTurn(sl, turned, ally),
 *              connect(sl, player, target, ticks, tag) (the thread), connectEnd(sl, target, tag)
 *          coinShot(sl, player, hitAt, actual) (it hits at once), ricochet(sl, from, to)
 *   Key 4  distortShot(sl, player, at, backToward), distortIntent(sl, player, target, ticks, tag), distortedStrike(sl, attacker,
 *              victim), distortIntentEnd(sl, target, tag); shellFlip(sl, center, radius) (Duke of Entropy)
 *          sneak: seal(sl, player, center, radius, ticks, tag) (shut at SEAL_SLAMS), sealEnd(sl, center, radius, tag);
 *              gate(sl, player, middle, yaw, ticks, tag) (Earl of the Fallen: a wall, shut at GATE_SLAMS),
 *              gateEnd(sl, middle, yaw, tag)
 *   Key 5  disorder(sl, player, target, ticks, tag), disorderEnd(sl, target, tag), unbound(sl, player) (holds broken)
 *          sneak: paces(sl, player, from, to, ticks, tag) (the fake self stays at 'from'), decoyBurst(sl, at, tag),
 *              untouchable(sl, player) (Earl of the Fallen: everything misses)
 *   Key 6  the wheel's pictures: WHEEL_RAISE_MAUSOLEUM, WHEEL_CORROSION, WHEEL_STEP, WHEEL_MAJESTY, WHEEL_BESTOWMENT,
 *              WHEEL_EXPLOIT, WHEEL_MAGNIFY, WHEEL_MAUSOLEUM_SALVO, WHEEL_MAUSOLEUM_BLAST
 *          raiseMausoleum(sl, ground, yaw, ticks, tag) (standing at MAUSOLEUM_RISEN), mausoleumHit(sl, ground),
 *              mausoleumCrumble(sl, ground, yaw, tag), sanctuaryPulse(sl, ground, radius), sanctuaryHum(sl, ground),
 *              sanctuaryMend(sl, ally), mausoleumAgain(sl, ground, yaw, ticks, tag) (shown again, already standing)
 *          corrosion(sl, player, radius), corroded(sl, enemy, ticks, tag), corrosionBite(sl, victim, perSecond)
 *          step(sl, player, from, to) (out at STEP_OUT)
 *          majesty(sl, player, radius), kneel(sl, victim, ticks, tag)
 *          bestowment(sl, player, target, Feeling.X, ticks, tag) (it lands at BESTOW_LANDS)
 *          exploitGlide(sl, player, ticks, tag), exploitGlideEnd(sl, player, tag), exploitStrip(sl, player, target),
 *              exploitExtend(sl, player), moonLeap(sl, player) (Duke of Entropy)
 *          magnify(sl, player, ticks, tag), magnifyEnd(sl, player, tag), magnifiedHit(sl, at),
 *              magnifiedFlower(sl, player, to) which returns its flight ticks, quake(sl, at, radius), seize(sl, player, target)
 *          salvo(sl, tops, target) (beam i hits at SALVO_STEP * i), salvoHit(sl, victim)
 *          mausoleumBlast(sl, ground, yaw, radius, tag) (it goes off at BLAST_LANDS; the standing one is cleared then)
 *   Key 7  the wheel's pictures: WHEEL_RUIN, WHEEL_FRENZY, WHEEL_ENTROPY, WHEEL_ABOLITION, WHEEL_EXILE, WHEEL_INVERSION,
 *              WHEEL_CORONATION, WHEEL_DECREE
 *          ruin(sl, player, to) (it fires at RUIN_FIRES), ruinHit(sl, victim)
 *          frenzy(sl, player, targets) (bolt i hits at FRENZY_FIRST + i * FRENZY_STEP), frenzyHit(sl, victim)
 *          entropy(sl, center, radius, ticks, tag), entropyPulse(sl, center, radius, strength), entropyEnd(sl, center, tag)
 *          abolition(sl, center, radius, ticks, tag) (shut at ABOLITION_CLOSED), abolitionEnd(sl, center, radius, tag)
 *          exile(sl, who), exileReturn(sl, who)
 *          inversion(sl, player, radius, ticks, tag), inversionPulse(sl, player, radius), inverted(sl, enemy)
 *          coronation(sl, player, ticks, tag) (crowned at CROWNED), coronationPulse(sl, player, radius),
 *              coronationEnd(sl, player, tag)
 *          decree(sl, player, target, mausoleums) (it falls at DECREE_LANDS; brighter per standing mausoleum)
 *   Death: rebirthFall(sl, player) (out of the world), rebirthRise(sl, player, ground), lastRebirth(sl, player)
 *   Order: order(player, count) shows "[sigil] Order n/9" beside the hotbar; orderRaised(sl, player) for a new one;
 *          orderEnd(player) hides it.
 *   Passives: weaknessDetection(viewer, target) (only the viewer sees it), presence(sl, player, radius),
 *          looseOrder(sl, at), beyondTheWorld(sl, player)
 *   Melee: combo(sl, player, target, step) for the twelve hits of docs/PRINCE_OF_ABOLITION.md, "Melee combo".
 *   Shells: shell(sl, center, radius, Shell.X, color, ticks, tag), a sphere of tiles (Seal, Abolition).
 *   Every timed cast: status(player, Status.X, ticks) shows its picture and clock; statusEnd(player, Status.X) takes them away.
 *
 * The models are in models/model_fx/prince_of_abolition/ (made by tools/model_gen/prince_of_abolition/models.py; each
 * JSON's "credit" gives its size and pivot; the five older models in that folder are the Frenzied Mage's), the pictures
 * in textures/gui/prince_of_abolition/ and textures/gui/ability_menus/prince_of_abolition/ (icons.py), the particles
 * in textures/particle/fx/ (particles.py), the sounds in sounds/prince_of_abolition/ (sounds.py) and sounds/ModSounds
 * (ABOLITION_...).
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class PrinceOfAbolitionFx {

    private PrinceOfAbolitionFx() {}

    private static ModelFx.Pose pose() {
        return ModelFx.pose();
    }

    // =====================================================================================
    // Colors (ARGB)
    // =====================================================================================

    /** The Pathway's own color: jet black. */
    public static final int PATHWAY_COLOR = 0xFF16121F;
    public static final int JET = 0xFF16121F;
    /** Imperial gold: orders, seals, mausoleums and the crown. */
    public static final int GOLD = 0xFFE0B04A;
    public static final int GOLD_LIGHT = 0xFFFFE08A;
    public static final int VIOLET = 0xFF2A1F3D;
    public static final int VIOLET_LIGHT = 0xFFC8AAFF;
    /** Ash grey: Disorder and Entropy. */
    public static final int ASH = 0xFFA0A0AA;
    /** The colorless fog of a world that is not the real one: Abolition and Exile. */
    public static final int FOG = 0xFFC4C4C8;
    public static final int ROSE = 0xFFF096AA;

    /** The four Bribes, each with its color (the coin's mark over the head, the motes). */
    public enum Bribe {
        WEAKEN(0xFFB8A880),
        CHARM(0xFFF0A0B4),
        ARROGANCE(0xFFFFD86A),
        CONNECT(0xFF3A3050);

        public final int color;

        Bribe(int color) {
            this.color = color;
        }
    }

    /** The feelings Bestowment can give (docs/PRINCE_OF_ABOLITION.md, the Fallen wheel). */
    public enum Feeling {
        SLUGGISH(0xFF7896D2),
        ANXIOUS(0xFFC8AAFF),
        DEFEATED(0xFF8A8894),
        RASH(0xFFFF6A5A),
        AVARICIOUS(0xFFE0B04A);

        public final int color;

        Feeling(int color) {
            this.color = color;
        }
    }

    /** What a shell is made of. */
    public enum Shell {
        /** gold bars with keyholes: Seal */
        SEAL,
        /** grey fog with no light in it: Abolition */
        ABOLITION
    }

    // =====================================================================================
    // The small particles everything shares
    // =====================================================================================

    private static final ParticleFx GOLD_MOTE = ParticleFx.of("fx/glow")
            .color(0xFFFFE08A).endColor(0x008A6420)
            .size(0.08f).endSize(0.02f).sizeRandom(0.4f)
            .lifetime(22, 10).gravity(-0.015f).friction(0.92f).glow();
    private static final ParticleFx VIOLET_MOTE = GOLD_MOTE.color(0xFFC8AAFF).endColor(0x002A1F3D);
    private static final ParticleFx ASH_MOTE = GOLD_MOTE.color(0xFFC0C0CA).endColor(0x00505058);
    private static final ParticleFx JET_SMOKE = ParticleFx.of("fx/smoke")
            .color(0xD016121F).endColor(0x00080610)
            .size(0.35f).endSize(0.9f).sizeRandom(0.3f)
            .lifetime(28, 12).gravity(-0.008f).friction(0.9f).spin(1.5f).randomRotation();
    private static final ParticleFx ASH_DUST = ParticleFx.of("fx/smoke")
            .color(0xA08A8894).endColor(0x00404048)
            .size(0.3f).endSize(0.8f).sizeRandom(0.3f)
            .lifetime(24, 10).gravity(-0.004f).friction(0.88f).spin(2f).randomRotation();
    private static final ParticleFx FOG_PUFF = ParticleFx.of("fx/smoke")
            .color(0x90C4C4C8).endColor(0x00808084)
            .size(0.5f).endSize(1.3f).sizeRandom(0.3f)
            .lifetime(40, 16).gravity(-0.002f).friction(0.9f).spin(1f).randomRotation();
    private static final ParticleFx COIN = ParticleFx.of("fx/coin")
            .color(0xFFFFE08A).endColor(0xC0B8862E)
            .size(0.08f).endSize(0.07f).sizeRandom(0.3f)
            .lifetime(24, 10).gravity(0.04f).friction(0.94f).spin(9f).randomRotation().glow();
    private static final ParticleFx SECTION = ParticleFx.of("fx/section")
            .color(0xFFE0B04A).endColor(0x008A6420)
            .size(0.09f).endSize(0.1f).sizeRandom(0.3f)
            .lifetime(26, 10).gravity(-0.015f).friction(0.94f).glow();
    private static final ParticleFx CROWN = ParticleFx.of("fx/crown")
            .color(0xFFFFE08A).endColor(0x00B8862E)
            .size(0.1f).endSize(0.08f).sizeRandom(0.2f)
            .lifetime(26, 10).gravity(-0.01f).friction(0.94f).glow();
    private static final ParticleFx COG = ParticleFx.of("fx/cog")
            .color(0xFFC0C0CA).endColor(0x40606068)
            .size(0.09f).endSize(0.07f).sizeRandom(0.4f)
            .lifetime(24, 10).gravity(0.01f).friction(0.92f).spin(-8f).randomRotation();
    private static final ParticleFx KEYHOLE = ParticleFx.of("fx/keyhole")
            .color(0xFFE0B04A).endColor(0x002A1F3D)
            .size(0.09f).endSize(0.1f).sizeRandom(0.2f)
            .lifetime(22, 8).gravity(-0.01f).friction(0.94f).glow();
    private static final ParticleFx STELE = ParticleFx.of("fx/stele")
            .color(0xFFE0B04A).endColor(0x0016121F)
            .size(0.1f).endSize(0.08f).sizeRandom(0.2f)
            .lifetime(26, 10).gravity(-0.02f).friction(0.94f).glow();
    private static final ParticleFx TWIST = ParticleFx.of("fx/twist")
            .color(0xFFC8AAFF).endColor(0x002A1F3D)
            .size(0.1f).endSize(0.12f).sizeRandom(0.3f)
            .lifetime(20, 8).gravity(-0.01f).friction(0.92f).spin(-12f).glow();
    private static final ParticleFx GAVEL_BIT = ParticleFx.of("fx/gavel")
            .color(0xFFE0B04A).endColor(0x0016121F)
            .size(0.1f).endSize(0.08f)
            .lifetime(16, 6).gravity(0.03f).friction(0.92f).spin(10f).randomRotation();
    private static final ParticleFx STONE_CHIP = ParticleFx.of("fx/splinter")
            .color(0xFF2A2438).endColor(0x80504868)
            .size(0.08f).endSize(0.06f).sizeRandom(0.4f)
            .lifetime(22, 8).gravity(0.06f).friction(0.92f).spin(10f).randomRotation().collide();
    private static final ParticleFx SPARK = ParticleFx.of("fx/spark")
            .color(0xFFFFFFFF).endColor(0x00E0B04A)
            .size(0.06f).endSize(0.02f)
            .lifetime(10, 6).gravity(0.04f).friction(0.88f).glow();
    private static final ParticleFx BOLT_SPARK = SPARK.color(0xFFE8D8FF).endColor(0x002A1F3D);
    private static final ParticleFx ROSE_MOTE = GOLD_MOTE.color(0xFFF0A0B4).endColor(0x00801830);

    // =====================================================================================
    // Sounds
    // =====================================================================================

    /** One of the pathway's own sounds (sounds/ModSounds, ABOLITION_...). */
    private static void sound(ServerLevel sl, Vec3 at, RegistryObject<SoundEvent> sound, float volume, float pitch) {
        Spells.sound(sl, at, sound.get(), volume, pitch);
    }

    /** One of the pathway's own sounds that only this player hears. */
    private static void soundFor(ServerPlayer player, RegistryObject<SoundEvent> sound, float volume, float pitch) {
        player.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound.get()),
                SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), volume, pitch, player.getRandom().nextLong()));
    }

    private static float wobble(float pitch) {
        return pitch * (0.95f + ThreadLocalRandom.current().nextFloat() * 0.1f);
    }

    // =====================================================================================
    // Small helpers
    // =====================================================================================

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
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }

    private static double widthOf(Entity entity) {
        return Math.max(0.6, entity.getBbWidth() * 0.8);
    }

    private static String tagged(String tag) {
        return tag == null ? "" : tag;
    }

    // =====================================================================================
    // Statuses: a picture in the row above the hotbar, and a clock beside the hotbar
    // =====================================================================================

    public enum Status {
        // what the Beyonder holds
        IRON_WILL("iron_will", "Iron Will", 0xA0A0AA),
        SEAL("seal", "Seal", 0xE0B04A),
        PACES("paces", "Paces", 0xC0C0CA),
        CONNECT("connect", "Connection", 0xC8AAFF),
        CORROSION("corrosion", "Corrosion", 0x9A6AC0),
        EXPLOIT("exploit", "Exploit", 0xFFE08A),
        MAGNIFY("magnify", "Magnify", 0xE0B04A),
        ENTROPY("entropy", "Entropy", 0xA0A0AA),
        ABOLITION("abolition", "Abolition", 0xC4C4C8),
        EXILE("exile", "Exiled", 0xC4C4C8),
        INVERSION("inversion", "Inversion", 0xE0B04A),
        CORONATION("coronation", "Coronation", 0xFFE08A),
        SANCTUARY("sanctuary", "Sanctuary", 0xE0B04A),
        REBIRTH("rebirth", "Rebirth", 0xFFE08A),
        // what is done to others (shown when they are players)
        COMPROMISED("compromised", "Compromised", 0xE0B04A),
        BRAINWASHED("brainwashed", "Brainwashed", 0xC8AAFF),
        WEAKENED("weakened", "Weakened", 0xB8A880),
        CHARMED("charmed", "Charmed", 0xF0A0B4),
        ARROGANT("arrogant", "Arrogant", 0xFFD86A),
        DISTORTED("distorted", "Distorted", 0xC8AAFF),
        DISORDERED("disordered", "Disordered", 0xA0A0AA),
        CORRODED("corroded", "Corroded", 0x9A6AC0),
        KNEELING("kneeling", "Kneeling", 0xE0B04A),
        SLUGGISH("sluggish", "Sluggish", 0x7896D2),
        ANXIOUS("anxious", "Anxious", 0xC8AAFF),
        DEFEATED("defeated", "Defeated", 0x8A8894),
        RASH("rash", "Rash", 0xFF6A5A),
        AVARICIOUS("avaricious", "Avaricious", 0xE0B04A);

        final String image;
        final String label;
        final int rgb;

        Status(String image, String label, int rgb) {
            this.image = image;
            this.label = label;
            this.rgb = rgb;
        }

        String id() {
            return "poa_status_" + name().toLowerCase();
        }
    }

    /** Shows the status's picture (in the row above the hotbar, rows of nine) and a clock beside the hotbar. */
    public static void status(LivingEntity player, Status status, int ticks) {
        int slot = status.ordinal();
        if (player instanceof Player viewerOfHud) ScreenImages.show(viewerOfHud, status.id(), "prince_of_abolition/" + status.image,
                ScreenAnchor.BOTTOM, 103 + (slot % 9) * 18, -21 - (slot / 9) * 18, 16, ticks);
        if (player instanceof Player viewerOfHud) HudTimer.show(viewerOfHud, status.id(), status.label, ticks, status.rgb);
    }

    /** Shows the status's picture with no clock, until statusEnd (for what lasts as long as something else does). */
    public static void statusForever(LivingEntity player, Status status) {
        int slot = status.ordinal();
        if (player instanceof Player viewerOfHud) ScreenImages.show(viewerOfHud, status.id(), "prince_of_abolition/" + status.image,
                ScreenAnchor.BOTTOM, 103 + (slot % 9) * 18, -21 - (slot / 9) * 18, 16, FOREVER);
        if (player instanceof Player viewerOfHud) HudTimer.hide(viewerOfHud, status.id());
    }

    /** A picture shown with statusForever stays this long (until statusEnd takes it away). */
    public static final int FOREVER = 72000;

    /** It ended early. */
    public static void statusEnd(LivingEntity player, Status status) {
        if (player instanceof Player viewerOfHud) ScreenImages.hide(viewerOfHud, status.id());
        if (player instanceof Player viewerOfHud) HudTimer.hide(viewerOfHud, status.id());
    }

    /** The status a Bestowment feeling shows on a player. */
    public static Status statusOf(Feeling feeling) {
        return switch (feeling) {
            case SLUGGISH -> Status.SLUGGISH;
            case ANXIOUS -> Status.ANXIOUS;
            case DEFEATED -> Status.DEFEATED;
            case RASH -> Status.RASH;
            case AVARICIOUS -> Status.AVARICIOUS;
        };
    }

    /** The status a Bribe shows on a player (Connect shows on the briber, as CONNECT). */
    public static Status statusOf(Bribe bribe) {
        return switch (bribe) {
            case WEAKEN -> Status.WEAKENED;
            case CHARM -> Status.CHARMED;
            case ARROGANCE -> Status.ARROGANT;
            case CONNECT -> Status.CONNECT;
        };
    }

    // =====================================================================================
    // Order: "[sigil] Order 4/9" to the left of the hotbar (combat/HudCounter)
    // =====================================================================================

    public static final int ORDER_CAP = 9;
    private static final String ORDER_ID = "poa_order";

    /** Shows (or updates) the Order counter. count 0 still shows "Order 0/9"; orderEnd hides it. */
    public static void order(LivingEntity player, int count) {
        if (player instanceof Player viewerOfHud) HudCounter.show(viewerOfHud, ORDER_ID, "prince_of_abolition/order", "Order", Math.max(0, count), ORDER_CAP, 0xE0B04A);
    }

    /** Takes the Order counter away (the Pathway was lost, or the player left it). */
    public static void orderEnd(LivingEntity player) {
        if (player instanceof Player viewerOfHud) HudCounter.hide(viewerOfHud, ORDER_ID);
    }

    /** A new standing order: a small crown rises from the Beyonder and a soft chime only they hear. */
    public static void orderRaised(ServerLevel sl, LivingEntity player) {
        ParticleShapes.spawn(sl, CROWN, headOf(player).add(0, 0.2, 0), new Vec3(0, 0.05, 0));
        if (player instanceof ServerPlayer sp) soundFor(sp, ModSounds.ABOLITION_ORDER, 0.6f, wobble(1.0f));
    }

    // =====================================================================================
    // The wheels' pictures (textures/gui/ability_menus/prince_of_abolition/, for AbilityWheel.entry's icon)
    // =====================================================================================

    public static final String WHEEL_WEAKEN = "ability_menus/prince_of_abolition/weaken";
    public static final String WHEEL_CHARM = "ability_menus/prince_of_abolition/charm";
    public static final String WHEEL_ARROGANCE = "ability_menus/prince_of_abolition/arrogance";
    public static final String WHEEL_CONNECT = "ability_menus/prince_of_abolition/connect";
    public static final String WHEEL_COIN_SHOT = "ability_menus/prince_of_abolition/coin_shot";
    public static final String WHEEL_RAISE_MAUSOLEUM = "ability_menus/prince_of_abolition/raise_mausoleum";
    public static final String WHEEL_CORROSION = "ability_menus/prince_of_abolition/corrosion";
    public static final String WHEEL_STEP = "ability_menus/prince_of_abolition/step";
    public static final String WHEEL_MAJESTY = "ability_menus/prince_of_abolition/majesty";
    public static final String WHEEL_BESTOWMENT = "ability_menus/prince_of_abolition/bestowment";
    public static final String WHEEL_EXPLOIT = "ability_menus/prince_of_abolition/exploit";
    public static final String WHEEL_MAGNIFY = "ability_menus/prince_of_abolition/magnify";
    public static final String WHEEL_MAUSOLEUM_SALVO = "ability_menus/prince_of_abolition/mausoleum_salvo";
    public static final String WHEEL_MAUSOLEUM_BLAST = "ability_menus/prince_of_abolition/mausoleum_blast";
    public static final String WHEEL_RUIN = "ability_menus/prince_of_abolition/ruin";
    public static final String WHEEL_FRENZY = "ability_menus/prince_of_abolition/frenzy";
    public static final String WHEEL_ENTROPY = "ability_menus/prince_of_abolition/entropy";
    public static final String WHEEL_ABOLITION = "ability_menus/prince_of_abolition/abolition";
    public static final String WHEEL_EXILE = "ability_menus/prince_of_abolition/exile";
    public static final String WHEEL_INVERSION = "ability_menus/prince_of_abolition/inversion";
    public static final String WHEEL_CORONATION = "ability_menus/prince_of_abolition/coronation";
    public static final String WHEEL_DECREE = "ability_menus/prince_of_abolition/decree";

    // =====================================================================================
    // The models (models/model_fx/prince_of_abolition/, see each JSON's "credit")
    // =====================================================================================

    /** The gavel of Verdict, 1.6 blocks tall at scale 1, head down, pivot under the head. */
    public static final ModelFx GAVEL = ModelFx.of("prince_of_abolition/gavel").pivot(8, 0.5f, 8)
            .aura(0x80E0B04A, 0.08f, 2);
    /** A mausoleum, 3 blocks tall at scale 1: frame 0 whole, frame 1 cracked. Pivot = the middle of its base. */
    public static final ModelFx MAUSOLEUM = ModelFx.of("prince_of_abolition/mausoleum").frames(2).pivot(8, -16, 8);
    /** The illusory door of Seal, 1 wide and 2 tall at scale 1, frames 0 (closed) to 3 (wide open), opening north. */
    public static final ModelFx DOOR = ModelFx.of("prince_of_abolition/illusory_door").frames(4).smooth();
    /** The crown, 0.75 block wide at scale 1, pivot = the underside of its band. */
    public static final ModelFx CROWN_MODEL = ModelFx.of("prince_of_abolition/crown").glow().aura(0x80E0B04A, 0.06f, 2);
    /** The thrown flower of a magnified Bribe, 0.6 block, its stem's end is the pivot. */
    public static final ModelFx FLOWER = ModelFx.of("prince_of_abolition/flower");
    /** A black bolt of Frenzy, 2 blocks at scale 1, pivot = its tail, top = its head (modelAlong). */
    public static final ModelFx BLACK_BOLT = ModelFx.of("prince_of_abolition/black_bolt").glow().unshaded();
    /** The fake self of Paces, 2 blocks tall, face = front, pivot between its feet. */
    public static final ModelFx DOUBLE = ModelFx.of("prince_of_abolition/shadow_double").pivot(8, -16, 8).seeThrough();
    /** A gold coin, 1 block at scale 1 (play it at 0.3), standing, pivot = its middle. */
    public static final ModelFx COIN_MODEL = ModelFx.of("prince_of_abolition/coin").pivot(8, 8, 8).glow();
    /** A beam 1 block long along +Y, its own colors; modelAlong + scale(width, length, width). */
    public static final ModelFx BEAM = ModelFx.of("prince_of_abolition/beam").glow().unshaded().seeThrough();
    /** The seal of the Black Emperor, 1 block, flat, white. */
    public static final ModelFx EMPEROR_SEAL = ModelFx.of("prince_of_abolition/emperor_seal").glow().unshaded().seeThrough();
    /** Compromised: the tipped scales, 1 block, standing, white. */
    public static final ModelFx VERDICT_MARK = ModelFx.of("prince_of_abolition/verdict_mark").pivot(8, 8, 8).glow()
            .unshaded().seeThrough();
    /** Lawyer's words: a ring of legal script, 1 block, flat, white. */
    public static final ModelFx LAW_SCRIPT = ModelFx.of("prince_of_abolition/law_script").glow().unshaded().seeThrough();
    /** A Bribe's coin mark, 1 block, standing, white. */
    public static final ModelFx BRIBE_MARK = ModelFx.of("prince_of_abolition/bribe_mark").pivot(8, 8, 8).glow().unshaded()
            .seeThrough();
    /** Disorder: a clock all wrong, 1 block, standing, white. */
    public static final ModelFx DISORDER_GLYPH = ModelFx.of("prince_of_abolition/disorder_glyph").pivot(8, 8, 8).glow()
            .unshaded().seeThrough();
    /** Inversion: a crown and its reflection, 1 block, flat, white. */
    public static final ModelFx INVERSION_SIGIL = ModelFx.of("prince_of_abolition/inversion_sigil").glow().unshaded()
            .seeThrough();
    /** Decree: the one word, 1 block, standing, white. */
    public static final ModelFx DECREE_WORD = ModelFx.of("prince_of_abolition/decree_word").pivot(8, 8, 8).glow().unshaded()
            .seeThrough();
    /** The sigil over a mausoleum, 1 block, standing, white. */
    public static final ModelFx MAUSOLEUM_SIGIL = ModelFx.of("prince_of_abolition/mausoleum_sigil").pivot(8, 8, 8).glow()
            .unshaded().seeThrough();
    /** Bestowment's mask, 1 block, standing, white. */
    public static final ModelFx FEELING_MASK = ModelFx.of("prince_of_abolition/feeling_mask").pivot(8, 8, 8).glow()
            .unshaded().seeThrough();
    /** A tile of a Seal's shell, 1 block, standing, white; pivot = the middle of its bottom edge. */
    public static final ModelFx SEAL_LATTICE = ModelFx.of("prince_of_abolition/seal_lattice").glow().unshaded().seeThrough();
    /** A tile of Abolition's fog, 1 block, standing, its own colors: no glow. */
    public static final ModelFx ABOLITION_FOG = ModelFx.of("prince_of_abolition/abolition_fog").unshaded().seeThrough();
    /** The ground of an Entropy field, 1 block, flat, its own colors. */
    public static final ModelFx ENTROPY_FIELD = ModelFx.of("prince_of_abolition/entropy_field").unshaded().seeThrough();

    /** The seal of the Black Emperor on the ground, 'radius' wide, turning, for 'ticks'. */
    public static void seal(ServerLevel sl, Vec3 ground, double radius, int color, int ticks, String tag) {
        ModelFx seal = EMPEROR_SEAL.color(color).scale((float) (radius * 2.0)).lifetime(ticks).fade(0, 14).spin(0.6f)
                .key(0, pose().scale(0.2f).alpha(0f))
                .key(12, pose().scale(1f).alpha(1f), Ease.OUT_BACK).tag(tagged(tag));
        ParticleShapes.model(sl, seal, ground.add(0, 0.05, 0), sl.getRandom().nextFloat() * 360f, 0f, 0f);
    }

    // =====================================================================================
    // Shells: tiles laid into a sphere, row by row (Seal, Abolition).
    // Each tile is one model, so a sphere 12 blocks wide is about 100 of them: keep the radius sensible.
    // =====================================================================================

    /** A whole sphere round 'center' (its middle), closing from the equator to both poles. */
    public static void shell(ServerLevel sl, Vec3 center, double radius, Shell kind, int color, int ticks, String tag) {
        ModelFx base = kind == Shell.SEAL ? SEAL_LATTICE.color(color) : ABOLITION_FOG.color(color);
        double[] edges = {-80, -55, -28, 0, 28, 55, 80};
        float turn = sl.getRandom().nextFloat() * 360f;
        int equator = 3;
        for (int row = 0; row < edges.length - 1; row++) {
            double lo = Math.toRadians(edges[row]), hi = Math.toRadians(edges[row + 1]);
            double mid = (lo + hi) / 2;
            double wide = radius * Math.cos(Math.min(Math.abs(lo), Math.abs(hi)));
            int count = Math.max(6, Math.min(24, (int) Math.round(2 * Math.PI * radius * Math.cos(mid) / 1.8)));
            float width = (float) (2 * wide * Math.sin(Math.PI / count) * 1.1);
            float height = (float) (2 * radius * Math.sin((hi - lo) / 2) * 1.06);
            int delay = Math.abs(row - (row < equator ? equator - 1 : equator)) * 4;
            for (int k = 0; k < count; k++) {
                float azimuth = turn + k * 360f / count;
                Vec3 at = center.add(ahead(azimuth).scale(radius * Math.cos(lo))).add(0, radius * Math.sin(lo), 0);
                ModelFx tile = base.scale(width, height, 1f).delay(delay).lifetime(ticks)
                        .fade(6, 14).key(0, pose().alpha(0f)).key(8, pose().alpha(1f), Ease.OUT).tag(tagged(tag));
                ParticleShapes.model(sl, tile, at, azimuth + 180f, (float) Math.toDegrees(mid), 0f);
            }
        }
        double cap = radius * Math.cos(Math.toRadians(80)) * 2.4;
        double up = radius * Math.sin(Math.toRadians(80));
        for (int side = -1; side <= 1; side += 2) {
            ModelFx lid = EMPEROR_SEAL.color(withAlpha(kind == Shell.SEAL ? color : FOG, 0xB0)).scale((float) cap)
                    .delay(12).lifetime(ticks).fade(4, 12).spin(0.5f).tag(tagged(tag));
            ParticleShapes.model(sl, lid, center.add(0, up * side, 0), 0f, 0f, 0f);
        }
    }

    // =====================================================================================
    // Key 1: Verdict (Lawyer) / Brainwash
    // =====================================================================================

    /** The tick Verdict's gavel strikes (the damage and the stagger go here). */
    public static final int VERDICT_LANDS = 10;

    /** A black gavel appears high over the target, swings back and strikes: a gold ring, a court's echo. */
    public static void verdict(ServerLevel sl, LivingEntity player, LivingEntity target) {
        Vec3 top = headOf(target);
        float yaw = yawToward(target.position(), player.position());
        ModelFx gavel = GAVEL.scale(2.2f).lifetime(VERDICT_LANDS + 14).fade(3, 10)
                .key(0, pose().up(3.5f).roll(-50f).alpha(0f))
                .key(5, pose().up(3.2f).roll(-70f).alpha(1f), Ease.OUT)                // raised...
                .key(VERDICT_LANDS, pose().up(0f).roll(0f), Ease.IN)                    // ...and brought down
                .key(VERDICT_LANDS + 2, pose().up(0.15f).roll(-4f), Ease.OUT)
                .key(VERDICT_LANDS + 5, pose().up(0f).roll(0f), Ease.OUT_BOUNCE);
        ParticleShapes.model(sl, gavel, top, yaw + 90f, 0f, 0f);
        sound(sl, target.position(), ModSounds.ABOLITION_VERDICT, 1.2f, wobble(1.0f));
        Later.run(sl, VERDICT_LANDS, () -> {
            if (!target.isAlive()) return;
            SpellFx.shockRing(sl, target.position(), 2.2, 0xD0E0B04A, 10, 0);
            ParticleShapes.burst(sl, GAVEL_BIT, headOf(target), 4, 0.05, 0.12);
            ParticleShapes.burst(sl, SPARK, headOf(target), 10, 0.1, 0.25);
            ParticleShapes.ring(sl, SECTION, target.position().add(0, 0.2, 0), widthOf(target), 8, 0.06);
        });
    }

    /** Compromised: the tipped scales hang over its head and keyholes drift off it for 'ticks'. */
    public static void compromised(ServerLevel sl, LivingEntity target, int ticks, String tag) {
        ModelFx mark = VERDICT_MARK.color(0xE0E0B04A).scale(0.55f).lifetime(ticks).fade(4, 10).tag(tagged(tag))
                .key(0, pose().scale(0.2f).alpha(0f)).key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK)
                .during(6, Math.max(7, ticks), pose().yaw(Math.min(720f, ticks * 2f)));
        ParticleShapes.modelOn(sl, mark, target, new Vec3(0, target.getBbHeight() + 0.6, 0), 0f, 0f, 0f);
        ParticleShapes.burst(sl, KEYHOLE, chestOf(target), 3, 0.02, 0.05);
    }

    public static void compromisedEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tagged(tag));
    }

    /** The tick Brainwash's words reach the target. */
    public static final int WORDS_LAND = 12;

    /** A few quiet words: legal script turns round the speaker's head, a stream of sections drifts to the target. */
    public static void brainwash(ServerLevel sl, LivingEntity player, LivingEntity target, int ticks, String tag) {
        ModelFx script = LAW_SCRIPT.color(0xD0C8AAFF).scale(1.3f).lifetime(30).fade(2, 10).spin(4f)
                .key(0, pose().scale(0.4f).alpha(0f)).key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOnHead(sl, script, player, -0.25, 0f, 0f, 0f);
        Vec3 from = player.getEyePosition();
        Vec3 to = headOf(target);
        Vec3 step = to.subtract(from).scale(1.0 / WORDS_LAND);
        for (int i = 0; i < 6; i++) ParticleShapes.spawn(sl, SECTION.color(0xFFC8AAFF), from, step.scale(0.9 + i * 0.04));
        sound(sl, player.position(), ModSounds.ABOLITION_BRAINWASH, 0.9f, wobble(1.0f));
        Later.run(sl, WORDS_LAND, () -> {
            if (!target.isAlive()) return;
            ModelFx ring = LAW_SCRIPT.color(0xA0C8AAFF).scale(0.9f).lifetime(Math.max(10, ticks)).fade(4, 10).spin(-3f)
                    .tag(tagged(tag));
            ParticleShapes.modelOnHead(sl, ring, target, 0.15, 0f, 0f, 0f);
            ParticleShapes.spiral(sl, VIOLET_MOTE, headOf(target), 3, 18, 0.2, 0.8, 1.0, 0.05);
        });
    }

    public static void brainwashEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tagged(tag));
    }

    // =====================================================================================
    // Key 2: Brute Force (Barbarian) / Iron Will
    // =====================================================================================

    /** The tick Brute Force connects. */
    public static final int BRUTE_LANDS = 4;

    private static final SlashFx BRUTE_ARC = SlashFx.arc("slash/streaks")
            .color(0xE0E0B04A).core(0xFFFFF0C0).radius(1.8f).arc(120f).width(0.9f).layers(2).spread(0.2f)
            .lifetime(8).sweep(3);

    /** One blow with the whole body: a heavy arc, a gold shock where it lands and the ground thrown up. */
    public static void bruteForce(ServerLevel sl, LivingEntity player, LivingEntity target) {
        Vec3 center = chestOf(player).add(Spells.flatLook(player).scale(0.8));
        ParticleShapes.slash(sl, BRUTE_ARC, center, player.getYRot(), 0f, 20f);
        sound(sl, player.position(), ModSounds.ABOLITION_BRUTE_FORCE, 1.1f, wobble(0.95f));
        Later.run(sl, BRUTE_LANDS, () -> {
            Vec3 at = target.isAlive() ? chestOf(target) : center.add(Spells.flatLook(player).scale(2));
            SpellFx.blast(sl, at, 1.4, 0xFFE0B04A, SPARK);
            ParticleShapes.burst(sl, ASH_DUST, target.position().add(0, 0.2, 0), 10, 0.05, 0.15);
            ParticleShapes.cone(sl, STONE_CHIP, target.position().add(0, 0.2, 0), Spells.flatLook(player), 80, 10, 0.2, 0.45);
        });
    }

    /** Iron Will: grey rings close round the Beyonder and a cold aura stays on them for 'ticks'. */
    public static void ironWill(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        SpellFx.closingRing(sl, player.position(), 3.0, 0xC0A0A0AA, 8, 0);
        SpellFx.risingRings(sl, player.position(), 0.9, player.getBbHeight(), 3, 0xA0C0C0CA);
        ModelFx halo = LAW_SCRIPT.color(0x80A0A0AA).scale(1.4f).lifetime(ticks).fade(6, 10).spin(1.5f).tag(tagged(tag));
        ParticleShapes.modelOnHead(sl, halo, player, 0.25, 0f, 0f, 0f);
        sound(sl, player.position(), ModSounds.ABOLITION_IRON_WILL, 1.0f, 1.0f);
    }

    /** A mind effect slides off: a grey flash at the temple and motes thrown away. */
    public static void mindShrug(ServerLevel sl, LivingEntity player) {
        ParticleShapes.burst(sl, ASH_MOTE, player.getEyePosition(), 8, 0.08, 0.18);
        ParticleShapes.burst(sl, SPARK.color(0xFFC0C0CA), player.getEyePosition(), 5, 0.1, 0.2);
    }

    public static void ironWillEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tagged(tag));
    }

    // =====================================================================================
    // Key 3: the Bribe wheel (Briber)
    // =====================================================================================

    /** The tick a Bribe's coin reaches the target. */
    public static final int BRIBE_LANDS = 8;

    /**
     * A coin is flicked from the briber's hand onto the target, spinning; 'actual' (a real ingot or emerald was spent)
     * makes it bigger and leaves a little shower of coins where it lands.
     */
    public static void bribe(ServerLevel sl, LivingEntity player, LivingEntity target, Bribe bribe, boolean actual) {
        Vec3 hand = SpellFx.handOf(player, sl);
        if (hand == null) hand = chestOf(player);
        ModelFx coin = COIN_MODEL.scale(actual ? 0.42f : 0.3f).lifetime(BRIBE_LANDS + 4).fade(1, 3)
                .key(0, pose().roll(0f)).key(BRIBE_LANDS, pose().roll(1080f));
        SpellFx.homing(sl, coin, hand, target, BRIBE_LANDS);
        sound(sl, player.position(), ModSounds.ABOLITION_BRIBE, 0.9f, wobble(1.0f));
        Later.run(sl, BRIBE_LANDS, () -> {
            if (!target.isAlive()) return;
            ParticleShapes.burst(sl, COIN, chestOf(target), actual ? 10 : 4, 0.06, 0.16);
            ParticleShapes.burst(sl, GOLD_MOTE.color(bribe.color), chestOf(target), 8, 0.03, 0.08);
            RegistryObject<SoundEvent> s = switch (bribe) {
                case WEAKEN -> ModSounds.ABOLITION_WEAKEN;
                case CHARM -> ModSounds.ABOLITION_CHARM;
                case ARROGANCE -> ModSounds.ABOLITION_ARROGANCE;
                case CONNECT -> ModSounds.ABOLITION_CONNECT;
            };
            sound(sl, target.position(), s, 0.9f, wobble(1.0f));
        });
    }

    /** The bribed: the coin mark, tinted for the Bribe, hangs over its head for 'ticks'; Charm adds rose motes. */
    public static void bribed(ServerLevel sl, LivingEntity target, Bribe bribe, int ticks, String tag) {
        ModelFx mark = BRIBE_MARK.color(withAlpha(bribe.color, 0xE0)).scale(0.5f).lifetime(ticks).fade(4, 10)
                .tag(tagged(tag)).key(0, pose().scale(0.2f).alpha(0f)).key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.modelOn(sl, mark, target, new Vec3(0, target.getBbHeight() + 0.55, 0), 0f, 0f, 0f);
        if (bribe == Bribe.CHARM) ParticleShapes.burst(sl, ROSE_MOTE, chestOf(target), 8, 0.02, 0.06);
        if (bribe == Bribe.WEAKEN) ParticleShapes.burst(sl, ASH_DUST, chestOf(target), 5, 0.01, 0.03);
        if (bribe == Bribe.ARROGANCE) ParticleShapes.ring(sl, CROWN, headOf(target), 0.5, 5, 0.02);
    }

    public static void bribedEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tagged(tag));
    }

    /** An actual Bribe (or one from the Earl of the Fallen on) spreads: coins ring out across 'radius'. */
    public static void bribeSpread(ServerLevel sl, Vec3 center, double radius, Bribe bribe) {
        SpellFx.shockRing(sl, center, radius, withAlpha(bribe.color, 0xC0), 10, 0);
        ParticleShapes.ring(sl, COIN, center.add(0, 0.6, 0), 0.4, 14, radius / 10.0);
    }

    /** Charm: someone turns on its own side: a rose flash and a thin line to whoever it turns on. */
    public static void charmTurn(ServerLevel sl, LivingEntity turned, LivingEntity ally) {
        ParticleShapes.burst(sl, ROSE_MOTE, headOf(turned), 10, 0.04, 0.12);
        if (ally != null) {
            SlashFx line = SlashFx.line("slash/smooth").color(0xC0F0A0B4).core(0xE0FFFFFF).width(0.06f)
                    .taper(SlashFx.Taper.COMET).lifetime(10);
            ParticleShapes.slashBetween(sl, line, chestOf(turned), chestOf(ally));
        }
    }

    private static final SlashFx THREAD = SlashFx.line("slash/smooth").color(0xE016121F).core(0xC0C8AAFF)
            .width(0.04f).taper(SlashFx.Taper.UNIFORM).translucent();

    /** Connect: a thin black thread from the briber to the target for 'ticks', gold motes along it at the start. */
    public static void connect(ServerLevel sl, LivingEntity player, LivingEntity target, int ticks, String tag) {
        SpellFx.tether(sl, THREAD, player, target, ticks);
        ParticleShapes.line(sl, GOLD_MOTE, chestOf(player), chestOf(target), 1.2);
        SpellFx.circleUnder(sl, target, 0.7, 0x90E0B04A, ticks, tagged(tag));
        sound(sl, player.position(), ModSounds.ABOLITION_CONNECT, 0.8f, 1.0f);
    }

    public static void connectEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tagged(tag));
    }

    private static final SlashFx COIN_TRAIL = SlashFx.line("slash/streaks").color(0xE0FFE08A).core(0xFFFFFFFF)
            .width(0.07f).taper(SlashFx.Taper.COMET).lifetime(6);

    /** Coin Shot: the flick, a gold streak along the line to where it hits, and a ping. Hitscan: it is there at once. */
    public static void coinShot(ServerLevel sl, LivingEntity player, Vec3 hitAt, boolean actual) {
        Vec3 hand = SpellFx.handOf(player, sl);
        if (hand == null) hand = player.getEyePosition();
        ParticleShapes.slashBetween(sl, actual ? COIN_TRAIL.width(0.12f) : COIN_TRAIL, hand, hitAt);
        ParticleShapes.burst(sl, COIN, hitAt, actual ? 6 : 3, 0.05, 0.15);
        ParticleShapes.burst(sl, SPARK, hitAt, 8, 0.1, 0.25);
        sound(sl, player.position(), ModSounds.ABOLITION_COIN_SHOT, 1.0f, wobble(1.0f));
    }

    /** The coin bounces on to the next one. */
    public static void ricochet(ServerLevel sl, Vec3 from, Vec3 to) {
        ParticleShapes.slashBetween(sl, COIN_TRAIL.width(0.05f), from, to);
        ParticleShapes.burst(sl, SPARK, to, 5, 0.08, 0.2);
        sound(sl, from, ModSounds.ABOLITION_RICOCHET, 0.8f, wobble(1.0f));
    }

    // =====================================================================================
    // Key 4: Distort (Baron of Corruption) / Seal (the illusory door)
    // =====================================================================================

    /** A projectile or spell is distorted where it is: twisting motes, a reversed ring, a streak back to its sender. */
    public static void distortShot(ServerLevel sl, LivingEntity player, Vec3 at, Vec3 backToward) {
        SpellFx.closingRing(sl, at.add(0, -0.3, 0), 1.6, 0xC0C8AAFF, 6, 0);
        ParticleShapes.burst(sl, TWIST, at, 6, 0.03, 0.08);
        if (backToward != null) {
            SlashFx back = SlashFx.line("slash/streaks").color(0xC0C8AAFF).core(0xFFFFFFFF).width(0.1f)
                    .taper(SlashFx.Taper.COMET).lifetime(8);
            ParticleShapes.slashBetween(sl, back, at, at.add(backToward.subtract(at).normalize().scale(4)));
        }
        sound(sl, at, ModSounds.ABOLITION_REFLECT, 0.9f, wobble(1.0f));
    }

    /** An enemy's intent is distorted for 'ticks': a twisting swirl over its head. */
    public static void distortIntent(ServerLevel sl, LivingEntity player, LivingEntity target, int ticks, String tag) {
        ParticleShapes.line(sl, TWIST, player.getEyePosition(), headOf(target), 1.0);
        ModelFx swirl = DISORDER_GLYPH.color(0xB0C8AAFF).scale(0.45f).lifetime(ticks).fade(4, 8).spin(-9f).tag(tagged(tag));
        ParticleShapes.modelOn(sl, swirl, target, new Vec3(0, target.getBbHeight() + 0.5, 0), 0f, 0f, 0f);
        sound(sl, target.position(), ModSounds.ABOLITION_DISTORT, 0.9f, wobble(1.0f));
    }

    /** A Distorted attack lands on the wrong one: a bent violet streak from the attacker to its own ally. */
    public static void distortedStrike(ServerLevel sl, LivingEntity attacker, LivingEntity victim) {
        SlashFx bend = SlashFx.line("slash/smooth").color(0xD0C8AAFF).core(0xFFFFFFFF).width(0.12f)
                .taper(SlashFx.Taper.COMET).lifetime(8);
        ParticleShapes.slashBetween(sl, bend, chestOf(attacker), chestOf(victim));
        ParticleShapes.burst(sl, TWIST, chestOf(victim), 4, 0.04, 0.1);
    }

    public static void distortIntentEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tagged(tag));
    }

    /** Duke of Entropy: an enemy shell is turned inside out: a ring closing on its middle, then bursting. */
    public static void shellFlip(ServerLevel sl, Vec3 center, double radius) {
        SpellFx.closingRing(sl, center, radius, 0xD0C8AAFF, 10, 0);
        Later.run(sl, 10, () -> {
            SpellFx.shockRing(sl, center, radius, 0xD0E0B04A, 10, 0);
            ParticleShapes.sphere(sl, TWIST, center, radius * 0.8, 30);
        });
        sound(sl, center, ModSounds.ABOLITION_DISTORT, 1.2f, 0.7f);
    }

    /** The tick the illusory door slams and the space is Sealed. */
    public static final int SEAL_SLAMS = 16;

    /** The illusory door, 'scale' times its size, standing at 'at' facing 'yaw': it rises, swings open, slams shut. */
    private static void door(ServerLevel sl, Vec3 at, float yaw, float scale, int slams, String tag) {
        ModelFx door = DOOR.scale(scale).lifetime(slams + 14).fade(4, 10).tag(tagged(tag))
                .key(0, pose().frame(0f).up(-1.2f * scale).alpha(0f))
                .key(5, pose().frame(0f).up(0f).alpha(1f), Ease.OUT)
                .key(slams - 6, pose().frame(3f), Ease.OUT)                    // a heartbeat open...
                .key(slams - 2, pose().frame(3f))
                .key(slams, pose().frame(0f), Ease.IN)                          // ...and slammed shut
                .key(slams + 2, pose().scale(1.04f), Ease.OUT)
                .key(slams + 4, pose().scale(1f));
        ParticleShapes.model(sl, door, at, yaw, 0f, 0f);
    }

    /**
     * Seal: an illusory door rises in front of the Beyonder, swings open, slams shut, and a shell of gold lattice closes
     * round 'center' (radius blocks) for 'ticks'.
     */
    public static void seal(ServerLevel sl, LivingEntity player, Vec3 center, double radius, int ticks, String tag) {
        Vec3 front = player.position().add(Spells.flatLook(player).scale(1.6));
        door(sl, front, player.getYRot() + 180f, 1.4f, SEAL_SLAMS, tag);
        sound(sl, front, ModSounds.ABOLITION_DOOR_RISE, 1.0f, 1.0f);
        Later.run(sl, SEAL_SLAMS, () -> {
            sound(sl, front, ModSounds.ABOLITION_SEAL, 1.3f, 1.0f);
            ParticleShapes.burst(sl, KEYHOLE, front.add(0, 1.3, 0), 10, 0.05, 0.15);
            SpellFx.shockRing(sl, center, radius, 0xD0E0B04A, 10, 0);
            shell(sl, center.add(0, 0.5, 0), radius, Shell.SEAL, 0xC0E0B04A, Math.max(20, ticks - SEAL_SLAMS), tag);
            seal(sl, center, radius * 0.8, 0x80E0B04A, Math.max(20, ticks - SEAL_SLAMS), tag);
        });
    }

    /** The Seal breaks (or runs out): the lattice is cleared and gold shards fall from where it stood. */
    public static void sealEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tagged(tag));
        ParticleShapes.sphere(sl, SPARK, center.add(0, 0.5, 0), radius, 40);
        ParticleShapes.sphere(sl, KEYHOLE, center.add(0, 0.5, 0), radius * 0.9, 12);
        sound(sl, center, ModSounds.ABOLITION_SEAL_BREAK, 1.2f, 1.0f);
    }

    /** The tick the gate's door slams (Earl of the Fallen). */
    public static final int GATE_SLAMS = 8;

    /**
     * Earl of the Fallen: the door is summoned anywhere and slams at once, and a wall of lattice 10 wide and 5 tall
     * stands across 'yaw' at 'middle' (the middle of its bottom edge) for 'ticks'.
     */
    public static void gate(ServerLevel sl, LivingEntity player, Vec3 middle, float yaw, int ticks, String tag) {
        door(sl, middle, yaw, 1.6f, GATE_SLAMS, tag);
        sound(sl, middle, ModSounds.ABOLITION_DOOR_RISE, 1.0f, 1.3f);
        Later.run(sl, GATE_SLAMS, () -> {
            sound(sl, middle, ModSounds.ABOLITION_SEAL, 1.4f, 0.9f);
            Vec3 side = ahead(yaw + 90f);
            for (int across = -5; across < 5; across++) {
                for (int up = 0; up < 5; up++) {
                    Vec3 at = middle.add(side.scale(across + 0.5)).add(0, up, 0);
                    int delay = Math.abs(across) + up;
                    ModelFx tile = SEAL_LATTICE.color(0xC0E0B04A).scale(1.02f).delay(delay)
                            .lifetime(Math.max(10, ticks - GATE_SLAMS)).fade(4, 10)
                            .key(0, pose().alpha(0f)).key(5, pose().alpha(1f), Ease.OUT).tag(tagged(tag));
                    ParticleShapes.model(sl, tile, at, yaw, 0f, 0f);
                }
            }
            ParticleShapes.line(sl, SPARK, middle.add(side.scale(-5)), middle.add(side.scale(5)), 3);
        });
    }

    /** The gate falls (or runs out): its lattice is cleared and gold sparks run along where it stood. */
    public static void gateEnd(ServerLevel sl, Vec3 middle, float yaw, String tag) {
        ParticleShapes.clearModels(sl, middle, tagged(tag));
        Vec3 side = ahead(yaw + 90f);
        for (int up = 0; up < 5; up += 2) {
            ParticleShapes.line(sl, SPARK, middle.add(side.scale(-5)).add(0, up + 0.5, 0), middle.add(side.scale(5)).add(0, up + 0.5, 0), 2);
        }
        ParticleShapes.burst(sl, KEYHOLE, middle.add(0, 2.5, 0), 8, 0.08, 0.2);
        sound(sl, middle, ModSounds.ABOLITION_SEAL_BREAK, 1.2f, 1.1f);
    }

    // =====================================================================================
    // Key 5: Disorder (Mentor of Disorder) / Paces
    // =====================================================================================

    /** Disorder: a clock all wrong spins over the enemy's head, cogs fall off it, the air slips out of tune. */
    public static void disorder(ServerLevel sl, LivingEntity player, LivingEntity target, int ticks, String tag) {
        ParticleShapes.line(sl, ASH_MOTE, player.getEyePosition(), headOf(target), 1.5);
        ModelFx glyph = DISORDER_GLYPH.color(0xD0A0A0AA).scale(0.6f).lifetime(ticks).fade(4, 10).tag(tagged(tag))
                .key(0, pose().scale(0.2f).alpha(0f)).key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK)
                .during(6, Math.max(7, ticks), pose().spin(-ticks * 9f));
        ParticleShapes.modelOn(sl, glyph, target, new Vec3(0, target.getBbHeight() + 0.65, 0), 0f, 0f, 0f);
        ParticleShapes.burst(sl, COG, headOf(target), 6, 0.04, 0.1);
        sound(sl, target.position(), ModSounds.ABOLITION_DISORDER, 1.0f, wobble(1.0f));
    }

    public static void disorderEnd(ServerLevel sl, LivingEntity target, String tag) {
        ParticleShapes.clearModels(sl, target, tagged(tag));
    }

    /** Disorder cast while held: everything holding the Beyonder comes apart in cogs and grey dust. */
    public static void unbound(ServerLevel sl, LivingEntity player) {
        ParticleShapes.burst(sl, COG, chestOf(player), 14, 0.08, 0.2);
        ParticleShapes.burst(sl, ASH_DUST, player.position().add(0, 0.5, 0), 8, 0.04, 0.1);
        SpellFx.shockRing(sl, player.position(), 2.5, 0xC0A0A0AA, 8, 0);
        sound(sl, player.position(), ModSounds.ABOLITION_DISORDER, 1.0f, 1.3f);
    }

    /**
     * Paces: a few long strides of light from 'from' to 'to', and the fake self left standing at 'from' for 'ticks'
     * (play it before moving the Beyonder; decoyBurst when it is struck).
     */
    public static void paces(ServerLevel sl, LivingEntity player, Vec3 from, Vec3 to, int ticks, String tag) {
        Vec3 way = to.subtract(from);
        for (int i = 1; i <= 4; i++) {
            Vec3 foot = from.add(way.scale(i / 4.0)).add(0, 0.1, 0);
            int at = i * 2;
            Later.run(sl, at, () -> {
                ParticleShapes.burst(sl, ASH_MOTE, foot, 5, 0.02, 0.06);
                SpellFx.shockRing(sl, foot, 0.8, 0x90C0C0CA, 6, 0);
            });
        }
        ModelFx self = DOUBLE.color(0xD0FFFFFF).lifetime(ticks).fade(2, 10).tag(tagged(tag))
                .during(0, ticks, pose().up(0.05f));
        ParticleShapes.model(sl, self, from, player.getYRot() + 180f, 0f, 0f);
        ParticleShapes.burst(sl, ASH_DUST, from.add(0, 1, 0), 6, 0.02, 0.06);
        sound(sl, from, ModSounds.ABOLITION_PACES, 1.0f, wobble(1.0f));
    }

    /** The fake self is struck and bursts: ash and cogs everywhere, a grey ring. */
    public static void decoyBurst(ServerLevel sl, Vec3 at, String tag) {
        ParticleShapes.clearModels(sl, at, tagged(tag));
        ParticleShapes.burst(sl, ASH_DUST, at.add(0, 1, 0), 14, 0.06, 0.18);
        ParticleShapes.burst(sl, COG, at.add(0, 1, 0), 10, 0.1, 0.25);
        SpellFx.shockRing(sl, at, 3.0, 0xC0A0A0AA, 10, 0);
        sound(sl, at, ModSounds.ABOLITION_DECOY_BURST, 1.0f, 1.0f);
    }

    /** Earl of the Fallen: for a moment everything aimed at the Beyonder misses: a grey shimmer round them. */
    public static void untouchable(ServerLevel sl, LivingEntity player) {
        ParticleShapes.sphere(sl, ASH_MOTE, chestOf(player), 1.1, 24);
        ParticleShapes.burst(sl, TWIST.color(0xFFC0C0CA), chestOf(player), 4, 0.03, 0.06);
    }

    // =====================================================================================
    // Key 6: the Fallen wheel, and the mausoleums
    // =====================================================================================

    /** The tick a raised mausoleum stands complete (count it toward Order and its bonus from here). */
    public static final int MAUSOLEUM_RISEN = 26;

    /**
     * A mausoleum rises out of the ground at 'ground', its door looking along 'yaw', and stands for 'ticks' (its whole
     * life: 2 minutes at base) with its gold sigil turning over it. 'tag' names this one: mausoleumCrumble with the same
     * tag breaks it.
     */
    public static void raiseMausoleum(ServerLevel sl, Vec3 ground, float yaw, int ticks, String tag) {
        int life = Math.max(MAUSOLEUM_RISEN + 1, ticks);
        ModelFx stele = MAUSOLEUM.lifetime(life).fade(0, 8).tag(tagged(tag))
                .key(0, pose().frame(0f).up(-3.2f))
                .key(MAUSOLEUM_RISEN, pose().up(0f), Ease.OUT);
        ParticleShapes.model(sl, stele, ground, yaw + 180f, 0f, 0f);
        ModelFx sigil = MAUSOLEUM_SIGIL.color(0xE0E0B04A).scale(0.55f).delay(MAUSOLEUM_RISEN).lifetime(life - MAUSOLEUM_RISEN)
                .fade(8, 8).spin(2f).tag(tagged(tag))
                .during(0, life - MAUSOLEUM_RISEN, pose().up(0.2f));
        ParticleShapes.model(sl, sigil, ground.add(0, 3.6, 0), yaw, 0f, 0f);
        SpellFx.runeCircle(sl, ground, 1.6, 0xA0E0B04A, MAUSOLEUM_RISEN + 10, 3f);
        for (int t = 0; t < MAUSOLEUM_RISEN; t += 3) {
            Later.run(sl, t, () -> {
                ParticleShapes.ring(sl, STONE_CHIP, ground.add(0, 0.1, 0), 0.7, 5, 0.08);
                ParticleShapes.ring(sl, ASH_DUST, ground.add(0, 0.1, 0), 0.8, 3, 0.04);
            });
        }
        Later.run(sl, MAUSOLEUM_RISEN, () -> {
            SpellFx.shockRing(sl, ground, 3.0, 0xD0E0B04A, 10, 0);
            ParticleShapes.burst(sl, STELE, ground.add(0, 3.2, 0), 6, 0.03, 0.08);
        });
        sound(sl, ground, ModSounds.ABOLITION_MAUSOLEUM_RAISE, 1.2f, 1.0f);
    }

    /**
     * A mausoleum that already stands is drawn again for 'ticks' more (its time was stretched: Exploit), with no
     * rising. Clear the old one first: ParticleShapes.clearModels(sl, ground, tag).
     */
    public static void mausoleumAgain(ServerLevel sl, Vec3 ground, float yaw, int ticks, String tag) {
        int life = Math.max(10, ticks);
        ModelFx stele = MAUSOLEUM.lifetime(life).fade(0, 8).tag(tagged(tag)).key(0, pose().frame(0f));
        ParticleShapes.model(sl, stele, ground, yaw + 180f, 0f, 0f);
        ModelFx sigil = MAUSOLEUM_SIGIL.color(0xE0E0B04A).scale(0.55f).lifetime(life).fade(0, 8).spin(2f).tag(tagged(tag))
                .during(0, life, pose().up(0.2f));
        ParticleShapes.model(sl, sigil, ground.add(0, 3.6, 0), yaw, 0f, 0f);
        ParticleShapes.burst(sl, GOLD_MOTE, ground.add(0, 3.2, 0), 6, 0.03, 0.08);
    }

    /** The tick a detonated mausoleum goes off (Mausoleum Blast); the standing one is cleared at that tick. */
    public static final int BLAST_LANDS = 12;

    /**
     * Mausoleum Blast: the stele's seams glow gold, its sigil spins faster and sinks into it, then it bursts: a ring
     * of gold fire 'radius' wide, black stone and gold sparks flung out, the ground shaking.
     */
    public static void mausoleumBlast(ServerLevel sl, Vec3 ground, float yaw, double radius, String tag) {
        Vec3 middle = ground.add(0, 1.5, 0);
        SpellFx.warningCircle(sl, ground, radius, 0xA0E0B04A, BLAST_LANDS);
        ModelFx sigil = MAUSOLEUM_SIGIL.color(0xFFFFE08A).scale(0.55f).lifetime(BLAST_LANDS).fade(0, 2).spin(14f)
                .key(0, pose().up(0f)).key(BLAST_LANDS, pose().up(-2.4f).scale(0.4f), Ease.IN);
        ParticleShapes.model(sl, sigil, ground.add(0, 3.6, 0), yaw, 0f, 0f);
        for (int t = 0; t < BLAST_LANDS; t += 3) {
            int h = t;
            Later.run(sl, t, () -> ParticleShapes.burst(sl, SPARK, ground.add(0, 0.4 + h * 0.2, 0), 3, 0.04, 0.1));
        }
        sound(sl, ground, ModSounds.ABOLITION_MAUSOLEUM_RAISE, 1.0f, 1.6f);
        Later.run(sl, BLAST_LANDS, () -> {
            ParticleShapes.clearModels(sl, ground, tagged(tag));
            SpellFx.blast(sl, middle, radius * 0.6, 0xFFFFE08A, SPARK);
            SpellFx.shockRing(sl, ground, radius, 0xF0E0B04A, 12, 0);
            SpellFx.shockRing(sl, ground, radius * 0.7, 0xC016121F, 10, 2);
            ParticleShapes.burst(sl, STONE_CHIP, middle, 24, 0.25, 0.6);
            ParticleShapes.burst(sl, JET_SMOKE, middle, 12, 0.08, 0.2);
            ParticleShapes.burst(sl, STELE, middle.add(0, 1, 0), 6, 0.1, 0.25);
            ParticleShapes.ring(sl, ASH_DUST, ground.add(0, 0.2, 0), 1.0, 20, radius / 10.0);
            sound(sl, ground, ModSounds.ABOLITION_QUAKE, 1.6f, 0.7f);
            sound(sl, ground, ModSounds.ABOLITION_MAUSOLEUM_CRUMBLE, 1.3f, 0.8f);
        });
    }

    /** A mausoleum is struck: chips of jet fly off it. */
    public static void mausoleumHit(ServerLevel sl, Vec3 ground) {
        ParticleShapes.burst(sl, STONE_CHIP, ground.add(0, 1.4, 0), 6, 0.08, 0.18);
        ParticleShapes.burst(sl, SPARK, ground.add(0, 1.4, 0), 3, 0.06, 0.14);
    }

    /**
     * A mausoleum crumbles (destroyed, used for Rebirth, run out, or the tenth pushed the oldest out): the standing one
     * is cleared and a cracked one shudders, sinks and falls apart in its place.
     */
    public static void mausoleumCrumble(ServerLevel sl, Vec3 ground, float yaw, String tag) {
        ParticleShapes.clearModels(sl, ground, tagged(tag));
        ModelFx broken = MAUSOLEUM.lifetime(30).fade(0, 14)
                .key(0, pose().frame(1f))
                .key(3, pose().roll(2f)).key(6, pose().roll(-2f)).key(9, pose().roll(1f))
                .key(30, pose().up(-1.6f).roll(4f), Ease.IN);
        ParticleShapes.model(sl, broken, ground, yaw + 180f, 0f, 0f);
        for (int t = 0; t < 24; t += 4) {
            int h = t;
            Later.run(sl, t, () -> {
                ParticleShapes.burst(sl, STONE_CHIP, ground.add(0, 2.4 - h * 0.06, 0), 6, 0.05, 0.15);
                ParticleShapes.ring(sl, ASH_DUST, ground.add(0, 0.2, 0), 0.9, 4, 0.06);
            });
        }
        sound(sl, ground, ModSounds.ABOLITION_MAUSOLEUM_CRUMBLE, 1.1f, wobble(1.0f));
    }

    /** Sanctuary (from Briber): a soft gold ring pulses round a mausoleum (call it every second or two). */
    public static void sanctuaryPulse(ServerLevel sl, Vec3 ground, double radius) {
        SpellFx.steadyRing(sl, ground, radius, 0x70E0B04A, 20, 0);
        ParticleShapes.ring(sl, GOLD_MOTE, ground.add(0, 0.3, 0), radius * 0.6, 10, 0.02);
    }

    /** The low hum of a Sanctuary (about 4 seconds long: play it every 4 seconds or so while someone is in it). */
    public static void sanctuaryHum(ServerLevel sl, Vec3 ground) {
        sound(sl, ground, ModSounds.ABOLITION_SANCTUARY, 0.7f, wobble(1.0f));
    }

    /** An ally (or the Beyonder) mends in a Sanctuary: gold motes rise from them. */
    public static void sanctuaryMend(ServerLevel sl, LivingEntity ally) {
        ParticleShapes.burst(sl, GOLD_MOTE, chestOf(ally), 3, 0.01, 0.04);
    }

    /** The tick Step lets the Beyonder out of the far mausoleum. */
    public static final int STEP_OUT = 6;

    /** Step: a gold door at the near end swallows the Beyonder and one at the mausoleum lets them out. */
    public static void step(ServerLevel sl, LivingEntity player, Vec3 from, Vec3 to) {
        float yaw = yawToward(from, to);
        door(sl, from, yaw + 180f, 1.0f, STEP_OUT + 2, null);
        ParticleShapes.burst(sl, GOLD_MOTE, from.add(0, 1, 0), 12, 0.03, 0.1);
        sound(sl, from, ModSounds.ABOLITION_STEP, 1.0f, 1.0f);
        Later.run(sl, STEP_OUT, () -> {
            ParticleShapes.burst(sl, GOLD_MOTE, to.add(0, 1, 0), 14, 0.04, 0.12);
            SpellFx.shockRing(sl, to, 2.0, 0xC0E0B04A, 8, 0);
            ParticleShapes.burst(sl, KEYHOLE, to.add(0, 1.6, 0), 4, 0.03, 0.06);
        });
    }

    /** Corrosion: a dark, greedy tide rolls out from the Beyonder across 'radius'; coins glint in it. */
    public static void corrosion(ServerLevel sl, LivingEntity player, double radius) {
        SpellFx.shockRing(sl, player.position(), radius, 0xC02A1F3D, 14, 0);
        SpellFx.shockRing(sl, player.position(), radius * 0.7, 0x909A6AC0, 12, 3);
        ParticleShapes.ring(sl, JET_SMOKE, player.position().add(0, 0.3, 0), 1.0, 24, radius / 16.0);
        ParticleShapes.ring(sl, COIN, player.position().add(0, 0.5, 0), 0.8, 10, radius / 14.0);
        sound(sl, player.position(), ModSounds.ABOLITION_CORROSION, 1.1f, 1.0f);
    }

    /** One corroded: dark smoke hangs round it and its eyes catch a glint of gold, for 'ticks'. */
    public static void corroded(ServerLevel sl, LivingEntity enemy, int ticks, String tag) {
        SpellFx.circleUnder(sl, enemy, widthOf(enemy) * 0.8, 0x909A6AC0, ticks, tagged(tag));
        ParticleShapes.burst(sl, JET_SMOKE, chestOf(enemy), 4, 0.01, 0.03);
        ParticleShapes.spawn(sl, COIN, headOf(enemy), new Vec3(0, 0.04, 0));
    }

    /** One bite of Corrosion (once a second): dark smoke and violet motes drip off it, thicker as the rot deepens. */
    public static void corrosionBite(ServerLevel sl, LivingEntity victim, float perSecond) {
        int count = 2 + Math.min(10, (int) (perSecond / 3f));
        ParticleShapes.burst(sl, JET_SMOKE, chestOf(victim), count, 0.02, 0.05);
        ParticleShapes.burst(sl, VIOLET_MOTE, chestOf(victim), count, 0.03, 0.06);
    }

    /** Majesty: a crown of light over the Beyonder, the seal of the Black Emperor slams down under them. */
    public static void majesty(ServerLevel sl, LivingEntity player, double radius) {
        ModelFx crown = CROWN_MODEL.scale(1.6f).lifetime(26).fade(3, 10)
                .key(0, pose().up(1.2f).alpha(0f)).key(6, pose().up(0.4f).alpha(1f), Ease.OUT)
                .key(12, pose().up(0.2f).scale(1.2f), Ease.IN);
        ParticleShapes.modelOnHead(sl, crown, player, 0.3, 0f, 0f, 0f);
        Later.run(sl, 10, () -> {
            seal(sl, player.position(), radius, 0xC0E0B04A, 30, null);
            SpellFx.shockRing(sl, player.position(), radius, 0xE0E0B04A, 10, 0);
            ParticleShapes.ring(sl, CROWN, player.position().add(0, 0.4, 0), 1.0, 12, radius / 12.0);
        });
        sound(sl, player.position(), ModSounds.ABOLITION_MAJESTY, 1.3f, 1.0f);
    }

    /** One forced to kneel for 'ticks': a gold weight presses on its head, a ring under it. */
    public static void kneel(ServerLevel sl, LivingEntity victim, int ticks, String tag) {
        SpellFx.circleUnder(sl, victim, widthOf(victim), 0xA0E0B04A, ticks, tagged(tag));
        ParticleShapes.spawn(sl, CROWN, headOf(victim).add(0, 0.6, 0), new Vec3(0, -0.06, 0));
        ParticleShapes.burst(sl, ASH_DUST, victim.position().add(0, 0.1, 0), 4, 0.02, 0.05);
    }

    /** The tick a bestowed feeling settles on the target. */
    public static final int BESTOW_LANDS = 8;

    /** Bestowment: a mask in the feeling's color drifts from the Beyonder onto the target's face, and fades into it. */
    public static void bestowment(ServerLevel sl, LivingEntity player, LivingEntity target, Feeling feeling, int ticks, String tag) {
        ModelFx mask = FEELING_MASK.color(withAlpha(feeling.color, 0xE0)).scale(0.6f).lifetime(BESTOW_LANDS + 12).fade(2, 10)
                .key(BESTOW_LANDS, pose().scale(1f)).key(BESTOW_LANDS + 10, pose().scale(1.4f), Ease.OUT);
        SpellFx.homing(sl, mask, player.getEyePosition(), target, BESTOW_LANDS);
        sound(sl, player.position(), ModSounds.ABOLITION_BESTOWMENT, 0.9f, wobble(1.0f));
        Later.run(sl, BESTOW_LANDS, () -> {
            if (!target.isAlive()) return;
            ParticleFx mote = GOLD_MOTE.color(feeling.color);
            ParticleShapes.burst(sl, mote, headOf(target), 8, 0.02, 0.06);
            SpellFx.circleUnder(sl, target, widthOf(target) * 0.7, withAlpha(feeling.color, 0x90), ticks, tagged(tag));
            if (feeling == Feeling.AVARICIOUS) ParticleShapes.burst(sl, COIN, headOf(target), 5, 0.03, 0.08);
            if (feeling == Feeling.RASH) ParticleShapes.burst(sl, SPARK.color(0xFFFF6A5A), chestOf(target), 6, 0.08, 0.16);
        });
    }

    /** Exploit, in the air: the Beyonder hangs, gold motes trailing off their shoulders like wings, for 'ticks'. */
    public static void exploitGlide(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        SpellFx.risingRings(sl, player.position(), 1.0, 1.0, 2, 0xA0FFE08A);
        ModelFx halo = EMPEROR_SEAL.color(0x70FFE08A).scale(1.4f).lifetime(ticks).fade(4, 10).spin(1.2f).tag(tagged(tag));
        ParticleShapes.modelOn(sl, halo, player, new Vec3(0, -0.15, 0), 0f, 0f, 0f);
        sound(sl, player.position(), ModSounds.ABOLITION_EXPLOIT, 0.9f, 1.0f);
    }

    public static void exploitGlideEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tagged(tag));
    }

    /** Exploit on an enemy: its states end early: what it carried is pulled off it in a spray of keyholes. */
    public static void exploitStrip(ServerLevel sl, LivingEntity player, LivingEntity target) {
        ParticleShapes.line(sl, GOLD_MOTE, player.getEyePosition(), chestOf(target), 1.5);
        ParticleShapes.burst(sl, KEYHOLE, chestOf(target), 8, 0.06, 0.16);
        SpellFx.closingRing(sl, target.position(), 2.0, 0xC0FFE08A, 6, 0);
        sound(sl, target.position(), ModSounds.ABOLITION_EXPLOIT, 1.0f, 0.8f);
    }

    /** Exploit on oneself: everything running is stretched: rings rise slowly and widen. */
    public static void exploitExtend(ServerLevel sl, LivingEntity player) {
        SpellFx.risingRings(sl, player.position(), 1.4, 2.4, 4, 0xA0FFE08A);
        ParticleShapes.spiral(sl, GOLD_MOTE, player.position().add(0, 0.2, 0), 3, 24, 0.3, 1.6, 1.2, 0.06);
        sound(sl, player.position(), ModSounds.ABOLITION_EXPLOIT, 1.0f, 1.2f);
    }

    /** Duke of Entropy, inside an Entropy field: the jump becomes a leap to the Moon: a burst of grey and gold below. */
    public static void moonLeap(ServerLevel sl, LivingEntity player) {
        SpellFx.shockRing(sl, player.position(), 4.0, 0xC0A0A0AA, 10, 0);
        ParticleShapes.cone(sl, ASH_DUST, player.position(), new Vec3(0, -1, 0), 160, 14, 0.1, 0.3);
        ParticleShapes.cone(sl, GOLD_MOTE, player.position(), new Vec3(0, -1, 0), 120, 12, 0.1, 0.3);
        sound(sl, player.position(), ModSounds.ABOLITION_EXPLOIT, 1.2f, 0.6f);
    }

    /** Magnify: the next blow gathers: a gold glow round the Beyonder's hand for 'ticks' (cleared when used). */
    public static void magnify(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        ModelFx ring = EMPEROR_SEAL.color(0xB0E0B04A).scale(0.7f).lifetime(ticks).fade(6, 6).spin(6f).tag(tagged(tag));
        ParticleShapes.modelOnTurning(sl, ring, player, new Vec3(0, 1.0, 0), 0f, 90f, 0f);
        SpellFx.closingRing(sl, player.position(), 3.0, 0xC0E0B04A, 10, 0);
        sound(sl, player.position(), ModSounds.ABOLITION_MAGNIFY, 1.0f, 1.0f);
    }

    public static void magnifyEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tagged(tag));
    }

    /** A magnified blow lands: a big gold flash and a ring. */
    public static void magnifiedHit(ServerLevel sl, Vec3 at) {
        SpellFx.blast(sl, at, 2.0, 0xFFFFE08A, SPARK);
        ParticleShapes.burst(sl, CROWN, at, 4, 0.06, 0.14);
    }

    /**
     * A magnified Bribe travels as a thrown flower, spinning end over end from the Beyonder's hand to 'to'. Returns
     * the ticks until it lands (play quake there then).
     */
    public static int magnifiedFlower(ServerLevel sl, LivingEntity player, Vec3 to) {
        Vec3 hand = SpellFx.handOf(player, sl);
        Vec3 from = hand == null ? player.getEyePosition() : hand;
        Vec3 way = to.subtract(from);
        int ticks = Math.max(6, (int) Math.round(way.length() / 1.1));
        ModelFx flower = FLOWER.scale(1.6f).lifetime(ticks + 2).fade(1, 2)
                .key(0, pose().up(0f).roll(0f))
                .key(ticks, pose().up((float) way.length()).roll(-720f));
        ParticleShapes.modelAlong(sl, flower, from, way);
        sound(sl, from, ModSounds.ABOLITION_BRIBE, 0.9f, 0.8f);
        return ticks;
    }

    /** The flower lands as a quake: the ground rings out across 'radius', dust, coins and petals of gold. */
    public static void quake(ServerLevel sl, Vec3 at, double radius) {
        SpellFx.shockRing(sl, at, radius, 0xE0E0B04A, 12, 0);
        SpellFx.shockRing(sl, at, radius * 0.6, 0xB02A1F3D, 10, 3);
        ParticleShapes.ring(sl, ASH_DUST, at.add(0, 0.2, 0), 0.8, 20, radius / 14.0);
        ParticleShapes.burst(sl, COIN, at.add(0, 0.4, 0), 10, 0.1, 0.25);
        ParticleShapes.burst(sl, STONE_CHIP, at.add(0, 0.3, 0), 12, 0.15, 0.35);
        sound(sl, at, ModSounds.ABOLITION_QUAKE, 1.4f, 1.0f);
    }

    /** Magnify through a Connection: the thread snaps taut, gold, and the target is hauled in along it. */
    public static void seize(ServerLevel sl, LivingEntity player, LivingEntity target) {
        SlashFx taut = THREAD.color(0xE0E0B04A).core(0xFFFFFFFF).width(0.08f);
        SpellFx.tether(sl, taut, player, target, 14);
        ParticleShapes.burst(sl, GOLD_MOTE, chestOf(target), 10, 0.05, 0.12);
        sound(sl, target.position(), ModSounds.ABOLITION_CONNECT, 1.2f, 0.7f);
    }

    /** Gap between the beams of Mausoleum Salvo (beam i hits at SALVO_STEP * i). */
    public static final int SALVO_STEP = 3;

    /** Mausoleum Salvo: every mausoleum (its top in 'tops') fires a gold beam at 'target', one after the other. */
    public static void salvo(ServerLevel sl, List<Vec3> tops, LivingEntity target) {
        for (int i = 0; i < tops.size(); i++) {
            Vec3 top = tops.get(i);
            Later.run(sl, i * SALVO_STEP, () -> {
                if (!target.isAlive()) return;
                Vec3 to = chestOf(target);
                beam(sl, top, to, 0.35f, 8);
                ParticleShapes.burst(sl, SPARK, top, 4, 0.05, 0.12);
                sound(sl, top, ModSounds.ABOLITION_SALVO, 0.9f, wobble(1.0f));
            });
        }
    }

    /** A Salvo beam strikes. */
    public static void salvoHit(ServerLevel sl, LivingEntity victim) {
        ParticleShapes.burst(sl, SPARK, chestOf(victim), 6, 0.08, 0.2);
        ParticleShapes.burst(sl, GOLD_MOTE, chestOf(victim), 4, 0.03, 0.08);
    }

    /** A beam from 'from' to 'to', 'width' blocks wide, flaring up and fading over 'ticks'. */
    public static void beam(ServerLevel sl, Vec3 from, Vec3 to, float width, int ticks) {
        Vec3 way = to.subtract(from);
        float length = (float) way.length();
        if (length < 0.1f) return;
        ModelFx beam = BEAM.scale(width, length, width).lifetime(ticks).fade(0, Math.max(2, ticks / 2))
                .key(0, pose().scale(0.3f)).key(2, pose().scale(1.2f), Ease.OUT).key(ticks, pose().scale(0.6f));
        ParticleShapes.modelAlong(sl, beam, from, way);
    }

    // =====================================================================================
    // Key 7: the Sovereign wheel
    // =====================================================================================

    /** The tick Ruin fires (it charges a moment in the hand first). */
    public static final int RUIN_FIRES = 6;

    /** Ruin: black and gold gather in the hand, then a beam the full length of the line. */
    public static void ruin(ServerLevel sl, LivingEntity player, Vec3 to) {
        Vec3 hand = SpellFx.handOf(player, sl);
        Vec3 from = hand == null ? player.getEyePosition() : hand;
        SpellFx.closingRing(sl, player.position(), 2.0, 0xC02A1F3D, RUIN_FIRES, 0);
        ParticleShapes.burst(sl, JET_SMOKE, from, 4, 0.01, 0.03);
        sound(sl, player.position(), ModSounds.ABOLITION_RUIN, 1.2f, 1.0f);
        Later.run(sl, RUIN_FIRES, () -> {
            beam(sl, from, to, 0.9f, 12);
            beam(sl, from, to, 1.6f, 8);
            ParticleShapes.line(sl, JET_SMOKE, from, to, 0.6);
            ParticleShapes.burst(sl, SPARK, to, 10, 0.1, 0.25);
        });
    }

    /** Ruin passes through someone: it is left smoking dark. */
    public static void ruinHit(ServerLevel sl, LivingEntity victim) {
        ParticleShapes.burst(sl, JET_SMOKE, chestOf(victim), 6, 0.02, 0.06);
        ParticleShapes.burst(sl, SPARK, chestOf(victim), 5, 0.08, 0.18);
    }

    /** The tick the first bolt of Frenzy hits, and the gap after it (bolt i hits at FRENZY_FIRST + i * FRENZY_STEP). */
    public static final int FRENZY_FIRST = 8;
    public static final int FRENZY_STEP = 1;

    /**
     * Frenzy: the Beyonder throws their arms open and black bolts leave a ring round them, each flying onto one of
     * 'targets' (the same ones again if there are fewer than twelve). With no targets they fly out in a ring and fade.
     */
    public static void frenzy(ServerLevel sl, LivingEntity player, List<LivingEntity> targets) {
        Vec3 chest = chestOf(player);
        SpellFx.shockRing(sl, player.position(), 3.0, 0xC0C8AAFF, 8, 0);
        sound(sl, player.position(), ModSounds.ABOLITION_FRENZY, 1.3f, 1.0f);
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            Vec3 start = chest.add(Math.cos(a) * 1.2, 0.4 + Math.sin(a * 3) * 0.3, Math.sin(a) * 1.2);
            LivingEntity target = targets.isEmpty() ? null : targets.get(i % targets.size());
            Vec3 end = target != null ? chestOf(target) : chest.add(Math.cos(a) * 14, 1.0, Math.sin(a) * 14);
            int flight = FRENZY_FIRST + i * FRENZY_STEP;
            Vec3 way = end.subtract(start);
            ModelFx bolt = BLACK_BOLT.scale(0.55f).lifetime(flight + 3).fade(1, 3)
                    .key(0, pose().up(0f))
                    .key(flight, pose().up((float) way.length()), Ease.IN);
            ParticleShapes.modelAlong(sl, bolt, start, way);
            ParticleShapes.burst(sl, BOLT_SPARK, start, 2, 0.02, 0.06);
        }
    }

    /** A Frenzy bolt strikes: a crack of pale light and a cog or two of Disorder. */
    public static void frenzyHit(ServerLevel sl, LivingEntity victim) {
        ParticleShapes.burst(sl, BOLT_SPARK, chestOf(victim), 8, 0.1, 0.25);
        ParticleShapes.burst(sl, COG, headOf(victim), 2, 0.03, 0.08);
    }

    /**
     * Entropy: the ground of a field 'radius' wide unravels round 'center' for 'ticks', growing stronger (call
     * entropyPulse with strength 0..1 every second or so; it is full after 8 seconds).
     */
    public static void entropy(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        ModelFx field = ENTROPY_FIELD.scale((float) (radius * 2.0)).lifetime(ticks).fade(30, 20).spin(0.8f).tag(tagged(tag))
                .key(0, pose().scale(0.2f)).key(160, pose().scale(1f), Ease.OUT);
        ParticleShapes.model(sl, field, center.add(0, 0.06, 0), 0f, 0f, 0f);
        SpellFx.shockRing(sl, center, radius, 0xB0A0A0AA, 16, 0);
        sound(sl, center, ModSounds.ABOLITION_ENTROPY, 1.4f, 1.0f);
    }

    /** One beat of an Entropy field: cogs and grey dust tumble up, thicker as it grows. */
    public static void entropyPulse(ServerLevel sl, Vec3 center, double radius, float strength) {
        int count = 6 + (int) (strength * 18);
        RandomCount r = new RandomCount(sl);
        for (int i = 0; i < count; i++) {
            Vec3 at = center.add(r.spread(radius), 0.2, r.spread(radius));
            ParticleShapes.spawn(sl, i % 3 == 0 ? COG : ASH_MOTE, at, new Vec3(r.spread(0.03), 0.03 + strength * 0.04, r.spread(0.03)));
        }
        if (strength > 0.5f) SpellFx.steadyRing(sl, center, radius, withAlpha(ASH, (int) (strength * 120)), 20, 0);
    }

    public static void entropyEnd(ServerLevel sl, Vec3 center, String tag) {
        ParticleShapes.clearModels(sl, center, tagged(tag));
    }

    /** The tick the pocket of Abolition is closed. */
    public static final int ABOLITION_CLOSED = 22;

    /** Abolition: a sphere 'radius' wide round 'center' slips out of the world: grey fog closes over it, no light. */
    public static void abolition(ServerLevel sl, Vec3 center, double radius, int ticks, String tag) {
        SpellFx.closingRing(sl, center.add(0, -radius * 0.9, 0), radius, 0xB0C4C4C8, ABOLITION_CLOSED, 0);
        shell(sl, center, radius, Shell.ABOLITION, 0xFFFFFFFF, ticks, tag);
        ParticleShapes.sphere(sl, FOG_PUFF, center, radius * 0.95, 60);
        sound(sl, center, ModSounds.ABOLITION_ABOLITION, 1.5f, 1.0f);
    }

    /** The pocket returns to the world: the fog is cleared and the world's sound comes back. */
    public static void abolitionEnd(ServerLevel sl, Vec3 center, double radius, String tag) {
        ParticleShapes.clearModels(sl, center, tagged(tag));
        ParticleShapes.sphere(sl, FOG_PUFF, center, radius, 40);
        SpellFx.shockRing(sl, center.add(0, -radius * 0.9, 0), radius * 1.2, 0x90C4C4C8, 12, 0);
        sound(sl, center, ModSounds.ABOLITION_EXILE_RETURN, 1.2f, 0.8f);
    }

    /** Exile: someone (or the Beyonder) folds out of the world: grey fog wraps them, they are gone. */
    public static void exile(ServerLevel sl, Entity who) {
        Vec3 c = chestOf(who);
        ParticleShapes.sphere(sl, FOG_PUFF, c, widthOf(who), 16);
        SpellFx.closingRing(sl, who.position(), 2.0, 0xC0C4C4C8, 8, 0);
        ModelFx fold = ABOLITION_FOG.scale((float) widthOf(who) * 1.6f, who.getBbHeight() * 1.3f, 1f).lifetime(14).fade(2, 8)
                .key(0, pose().scale(1f)).key(12, pose().scale(0.05f), Ease.IN);
        ParticleShapes.model(sl, fold, who.position(), who.getYRot(), 0f, 0f);
        ParticleShapes.model(sl, fold, who.position(), who.getYRot() + 90f, 0f, 0f);
        sound(sl, who.position(), ModSounds.ABOLITION_EXILE, 1.0f, 1.0f);
    }

    /** Back from Exile: fog unfolds where they return. */
    public static void exileReturn(ServerLevel sl, Entity who) {
        ParticleShapes.burst(sl, FOG_PUFF, chestOf(who), 10, 0.03, 0.08);
        SpellFx.shockRing(sl, who.position(), 2.0, 0xC0C4C4C8, 8, 0);
        sound(sl, who.position(), ModSounds.ABOLITION_EXILE_RETURN, 1.0f, 1.0f);
    }

    /**
     * Inversion: the sigil of opposites lies under the Beyonder across 'radius', turning backward, and the light goes
     * dark round them for 'ticks'.
     */
    public static void inversion(ServerLevel sl, LivingEntity player, double radius, int ticks, String tag) {
        ModelFx sigil = INVERSION_SIGIL.color(0xC0E0B04A).scale((float) (radius * 2.0)).lifetime(ticks).fade(10, 14)
                .spin(-0.8f).tag(tagged(tag))
                .key(0, pose().scale(0.1f).alpha(0f)).key(14, pose().scale(1f).alpha(1f), Ease.OUT);
        ParticleShapes.model(sl, sigil, player.position().add(0, 0.06, 0), 0f, 0f, 0f);
        SpellFx.closingRing(sl, player.position(), radius, 0xD016121F, 14, 0);
        ParticleShapes.ring(sl, JET_SMOKE, player.position().add(0, 0.5, 0), radius, 30, -radius / 20.0);
        sound(sl, player.position(), ModSounds.ABOLITION_INVERSION, 1.4f, 1.0f);
    }

    /** One beat of Inversion: dark motes are drawn in toward the middle, gold ones thrown out. */
    public static void inversionPulse(ServerLevel sl, LivingEntity player, double radius) {
        ParticleShapes.ring(sl, JET_SMOKE, player.position().add(0, 0.6, 0), radius * 0.9, 16, -0.15);
        ParticleShapes.ring(sl, GOLD_MOTE, player.position().add(0, 0.4, 0), 0.6, 10, 0.25);
    }

    /** Someone caught by Inversion: what was good on it turns: a gold ring above it flips to black below. */
    public static void inverted(ServerLevel sl, LivingEntity enemy) {
        ParticleShapes.ring(sl, GOLD_MOTE, headOf(enemy), widthOf(enemy), 6, 0.0);
        ParticleShapes.ring(sl, JET_SMOKE, enemy.position().add(0, 0.1, 0), widthOf(enemy), 6, 0.0);
    }

    /** The tick the crown of Coronation settles on the head. */
    public static final int CROWNED = 14;

    /**
     * Coronation: the mausoleums crown the Beyonder: a crown descends onto their head and stays there for 'ticks'
     * (call coronationPulse every 5 seconds), the seal of the Black Emperor under them.
     */
    public static void coronation(ServerLevel sl, LivingEntity player, int ticks, String tag) {
        ModelFx crown = CROWN_MODEL.scale(0.9f).lifetime(ticks).fade(4, 12).tag(tagged(tag))
                .key(0, pose().up(2.4f).alpha(0f).spin(-180f))
                .key(CROWNED, pose().up(0f).alpha(1f).spin(0f), Ease.OUT);
        ParticleShapes.modelOnHead(sl, crown, player, 0.0, 0f, 0f, 0f);
        seal(sl, player.position(), 2.5, 0xC0E0B04A, CROWNED + 30, null);
        ParticleShapes.spiral(sl, GOLD_MOTE, player.position().add(0, 0.2, 0), 4, 40, 0.4, 2.5, 1.0, 0.08);
        sound(sl, player.position(), ModSounds.ABOLITION_CORONATION, 1.4f, 1.0f);
        Later.run(sl, CROWNED, () -> {
            ParticleShapes.burst(sl, CROWN, headOf(player).add(0, 0.4, 0), 8, 0.05, 0.12);
            SpellFx.risingRings(sl, player.position(), 1.2, player.getBbHeight() + 0.4, 3, 0xD0FFE08A);
        });
    }

    /** One of Coronation's Majesty pulses: a gold ring out of the crowned Beyonder. */
    public static void coronationPulse(ServerLevel sl, LivingEntity player, double radius) {
        SpellFx.shockRing(sl, player.position(), radius, 0xB0E0B04A, 12, 0);
        ParticleShapes.ring(sl, CROWN, player.position().add(0, 0.4, 0), 1.0, 8, radius / 14.0);
        sound(sl, player.position(), ModSounds.ABOLITION_MAJESTY, 0.8f, 1.2f);
    }

    public static void coronationEnd(ServerLevel sl, LivingEntity player, String tag) {
        ParticleShapes.clearModels(sl, player, tagged(tag));
        ParticleShapes.burst(sl, GOLD_MOTE, headOf(player), 10, 0.03, 0.08);
    }

    /** The tick the Decree takes effect. */
    public static final int DECREE_LANDS = 18;

    /**
     * Decree: the one word hangs before the speaker, then over the target, and a pillar of gold-white light falls on
     * it. The glyph burns brighter for every standing mausoleum ('mausoleums').
     */
    public static void decree(ServerLevel sl, LivingEntity player, LivingEntity target, int mausoleums) {
        int glow = Math.min(0xFF, 0xA0 + mausoleums * 10);
        ModelFx word = DECREE_WORD.color(withAlpha(GOLD_LIGHT, glow)).scale(1.0f).lifetime(14).fade(3, 6)
                .key(0, pose().scale(0.3f).alpha(0f)).key(6, pose().scale(1f).alpha(1f), Ease.OUT_BACK);
        ParticleShapes.model(sl, word, player.getEyePosition().add(Spells.flatLook(player).scale(1.4)),
                player.getYRot() + 180f, 0f, 0f);
        sound(sl, player.position(), ModSounds.ABOLITION_DECREE, 1.6f, 1.0f);
        Later.run(sl, 10, () -> {
            if (!target.isAlive()) return;
            ModelFx over = DECREE_WORD.color(withAlpha(GOLD_LIGHT, glow)).scale(1.8f + mausoleums * 0.1f).lifetime(20)
                    .fade(2, 10).key(0, pose().scale(0.4f)).key(8, pose().scale(1f), Ease.OUT);
            ParticleShapes.model(sl, over, headOf(target).add(0, 1.8, 0), yawToward(target.position(), player.position()), 0f, 0f);
        });
        Later.run(sl, DECREE_LANDS, () -> {
            if (!target.isAlive()) return;
            Vec3 ground = target.position();
            beam(sl, ground.add(0, 12, 0), ground, 1.4f, 12);
            beam(sl, ground.add(0, 12, 0), ground, 2.6f, 8);
            SpellFx.blast(sl, chestOf(target), 2.2, 0xFFFFF0C0, SPARK);
            seal(sl, ground, 3.0, 0xD0E0B04A, 30, null);
        });
    }

    // =====================================================================================
    // Death: Rebirth from a mausoleum, Last Rebirth
    // =====================================================================================

    /** A killing blow does not take: the Beyonder slips out of the real world (fog closes on them). */
    public static void rebirthFall(ServerLevel sl, LivingEntity player) {
        exile(sl, player);
        ParticleShapes.burst(sl, STELE, headOf(player), 5, 0.04, 0.1);
    }

    /**
     * The Beyonder rises from the mausoleum at 'ground' (its middle): light pours up through the stele, the Beyonder
     * steps out, a Majesty ring goes out. Play mausoleumCrumble on that mausoleum just after.
     */
    public static void rebirthRise(ServerLevel sl, LivingEntity player, Vec3 ground) {
        beam(sl, ground, ground.add(0, 6, 0), 1.2f, 16);
        SpellFx.risingRings(sl, ground, 1.6, 3.4, 4, 0xD0FFE08A);
        seal(sl, ground, 4.0, 0xD0E0B04A, 40, null);
        Later.run(sl, 6, () -> {
            SpellFx.shockRing(sl, ground, 10.0, 0xE0E0B04A, 14, 0);
            ParticleShapes.burst(sl, CROWN, ground.add(0, 2.2, 0), 10, 0.06, 0.16);
        });
        sound(sl, ground, ModSounds.ABOLITION_REBIRTH, 1.6f, 1.0f);
    }

    /** Last Rebirth: the Beyonder rises where they fell, held up by the orders still standing: a smaller rising. */
    public static void lastRebirth(ServerLevel sl, LivingEntity player) {
        Vec3 ground = player.position();
        SpellFx.risingRings(sl, ground, 1.2, 2.4, 3, 0xC0E0B04A);
        seal(sl, ground, 2.5, 0xA0E0B04A, 30, null);
        ParticleShapes.burst(sl, GOLD_MOTE, chestOf(player), 16, 0.04, 0.12);
        sound(sl, ground, ModSounds.ABOLITION_REBIRTH, 1.2f, 1.2f);
    }

    // =====================================================================================
    // Passives
    // =====================================================================================

    /** Weakness Detection: a gold keyhole over what the Beyonder looks at, only they see it. */
    public static void weaknessDetection(ServerPlayer viewer, LivingEntity target) {
        ParticleShapes.spawnFor(viewer, KEYHOLE, headOf(target).add(0, 0.4, 0), Vec3.ZERO);
    }

    /** Presence: weak things nearby hesitate: a faint gold ring passes over the ground now and then. */
    public static void presence(ServerLevel sl, LivingEntity player, double radius) {
        SpellFx.steadyRing(sl, player.position(), radius, 0x40E0B04A, 20, 0);
    }

    /** Loose Order: something aimed at the Beyonder drifts off: a little twist where it bends. */
    public static void looseOrder(ServerLevel sl, Vec3 at) {
        ParticleShapes.spawn(sl, TWIST.color(0xFFC0C0CA), at, Vec3.ZERO);
    }

    /** Beyond the World: harm with no hand behind it is half lost: grey fog puffs from the Beyonder. */
    public static void beyondTheWorld(ServerLevel sl, LivingEntity player) {
        ParticleShapes.burst(sl, FOG_PUFF, chestOf(player), 3, 0.02, 0.05);
    }

    // =====================================================================================
    // Melee combo (docs/PRINCE_OF_ABOLITION.md, "Melee combo")
    // =====================================================================================

    private static final SlashFx COMBO_EDGE = SlashFx.arc("slash/streaks")
            .color(0xD070488C).tailColor(0x30502068).headColor(0xFFFFE08A).core(0xFFFFF2BE)
            .radius(1.8f).arc(190f).width(0.30f).layers(2).spread(0.08f).lifetime(8).sweep(3);
    // A translucent silhouette keeps the black blade visible even in bright daylight.
    private static final SlashFx COMBO_SHADOW = SlashFx.arc("slash/smooth")
            .color(0xD016121F).translucent().radius(1.78f).arc(190f).width(0.48f).lifetime(9).sweep(3);
    private static final SlashFx COMBO_THRUST = SlashFx.line("slash/crystal")
            .color(0xC0C8AAFF).headColor(0xFFFFE08A).core(0xFFFFF2BE)
            .radius(1.3f).width(0.16f).layers(2).spread(0.07f).lifetime(8).sweep(2).fromCenter();

    /** Twelve visual hits. Geometry varies within each move without changing damage or attack speed. */
    public static void combo(ServerLevel sl, LivingEntity player, LivingEntity target, int step) {
        int move = Math.floorMod(step, 12);
        Vec3 chest = chestOf(player);
        Vec3 impact = chestOf(target);
        float yaw = yawToward(player.position(), target.position());
        float tilt = switch (move) {
            case 3 -> 48f;
            case 4 -> 54f;
            case 10 -> -52f;
            case 6 -> -36f;
            case 7 -> 70f;
            case 9 -> -18f;
            case 11 -> 50f;
            default -> 0f;
        };
        float turn = (move == 1 || move == 4 || move == 9) ? -1f : 1f;
        if (move == 2 || move == 8) {
            ParticleShapes.slashBetween(sl, COMBO_THRUST, chest, impact);
            ParticleShapes.slash(sl, COMBO_EDGE.radius(0.75f).arc(320f).width(0.16f)
                    .delay(2).rotation(0, -90, 0), impact, yaw, 0, 0);
        } else {
            boolean flourish = move == 5 || move == 11;
            SlashFx edge = COMBO_EDGE.arc(turn * (flourish ? 270f : 165f))
                    .radius(flourish ? 2.0f : 1.65f).jittered(sl.getRandom(), 3f, 2f, 3f);
            ParticleShapes.slash(sl, COMBO_SHADOW.arc(turn * (flourish ? 270f : 165f))
                    .radius(flourish ? 1.98f : 1.63f).alignedWith(edge), chest, yaw, 0f, tilt);
            ParticleShapes.slash(sl, edge, chest, yaw, 0f, tilt);
            ParticleShapes.slash(sl, edge.radius(1.45f).width(0.10f).layers(1).delay(2)
                    .color(0xC0E0B04A), chest.add(0, 0.12, 0), yaw, 0f, tilt);
        }
        ParticleShapes.burst(sl, SPARK, impact, move == 11 ? 8 : 3, 0.06, 0.14);
        if (move == 5 || move == 11) {
            int variant = sl.getRandom().nextInt(3);
            if (variant == 0) {
                SpellFx.runeCircle(sl, target.position(), 1.7, 0xE0E0B04A, 18, 6f);
                ParticleShapes.slash(sl, COMBO_EDGE.radius(0.6f).endRadius(2.3f).arc(400f)
                        .spin(5f).width(0.16f).layers(2).delay(3), impact, yaw, 0f, 0f);
            } else if (variant == 1) {
                for (int i = 0; i < 3; i++) {
                    ParticleShapes.slash(sl, COMBO_THRUST.radius(1.6f).delay(i * 2)
                            .rotation(0, -90, -60 + i * 60), impact, yaw, 0, 0);
                }
            } else {
                for (int i = 0; i < 2; i++) {
                    ParticleShapes.slash(sl, COMBO_EDGE.radius(1.3f).width(0.22f).layers(1).arc(i == 0 ? 190 : -190)
                            .delay(2 + i * 2), impact, yaw, 0, i == 0 ? 45 : -45);
                }
                ParticleShapes.burst(sl, SECTION, impact, 4, 0.04, 0.1);
            }
        }
    }

    // =====================================================================================
    // A little randomness for scattered effects
    // =====================================================================================

    private static final class RandomCount {
        private final ServerLevel sl;

        RandomCount(ServerLevel sl) {
            this.sl = sl;
        }

        double spread(double half) {
            return (sl.getRandom().nextDouble() * 2 - 1) * half;
        }
    }

    // =====================================================================================
    // /abolitionfx: every display, to look at in game
    // =====================================================================================

    @FunctionalInterface
    private interface Show {
        void play(ServerLevel sl, Player viewer, LivingEntity target, Vec3 spot);
    }

    /** onViewer = it is shown on the viewer (worn), not on a spot or a target. */
    private record Case(String name, int ticks, boolean onViewer, Show show) {}

    private static String tagFor(String name) {
        return "poa_fx_" + name + "_" + UUID.randomUUID();
    }

    private static final List<Case> CASES = List.of(
            new Case("verdict", 70, false, (sl, v, t, spot) -> {
                if (t == v) return;
                verdict(sl, v, t);
                String tag = tagFor("compromised");
                Later.run(sl, VERDICT_LANDS, () -> compromised(sl, t, 50, tag));
            }),
            new Case("brainwash", 70, false, (sl, v, t, spot) -> {
                if (t == v) return;
                brainwash(sl, v, t, 50, tagFor("brainwash"));
            }),
            new Case("brute_force", 30, false, (sl, v, t, spot) -> bruteForce(sl, v, t)),
            new Case("iron_will", 70, true, (sl, v, t, spot) -> {
                String tag = tagFor("iron");
                ironWill(sl, v, 60, tag);
                Later.run(sl, 25, () -> mindShrug(sl, v));
            }),
            new Case("bribes", 130, false, (sl, v, t, spot) -> {
                if (t == v) return;
                Bribe[] all = Bribe.values();
                for (int i = 0; i < all.length; i++) {
                    Bribe bribe = all[i];
                    Later.run(sl, i * 30, () -> {
                        bribe(sl, v, t, bribe, bribe == Bribe.ARROGANCE);
                        String tag = tagFor("bribed");
                        Later.run(sl, BRIBE_LANDS, () -> bribed(sl, t, bribe, 22, tag));
                        if (bribe == Bribe.ARROGANCE) Later.run(sl, BRIBE_LANDS, () -> bribeSpread(sl, t.position(), 3, bribe));
                        if (bribe == Bribe.CONNECT) Later.run(sl, BRIBE_LANDS, () -> connect(sl, v, t, 40, tagFor("connect")));
                    });
                }
            }),
            new Case("coin_shot", 40, false, (sl, v, t, spot) -> {
                Vec3 hit = t == v ? v.getEyePosition().add(v.getLookAngle().scale(12)) : chestOf(t);
                coinShot(sl, v, hit, false);
                Later.run(sl, 4, () -> ricochet(sl, hit, hit.add(3, 0, 2)));
                Later.run(sl, 20, () -> coinShot(sl, v, hit, true));
            }),
            new Case("distort", 60, false, (sl, v, t, spot) -> {
                Vec3 at = v.getEyePosition().add(v.getLookAngle().scale(6));
                distortShot(sl, v, at, v.getEyePosition().add(v.getLookAngle().scale(20)));
                if (t != v) {
                    String tag = tagFor("intent");
                    Later.run(sl, 15, () -> distortIntent(sl, v, t, 40, tag));
                    Later.run(sl, 35, () -> distortedStrike(sl, t, v));
                }
            }),
            new Case("seal", 120, true, (sl, v, t, spot) -> {
                String tag = tagFor("seal");
                Vec3 center = v.position();
                seal(sl, v, center, 6, 100, tag);
                Later.run(sl, 110, () -> sealEnd(sl, center, 6, tag));
            }),
            new Case("gate", 90, false, (sl, v, t, spot) -> {
                String tag = tagFor("gate");
                gate(sl, v, spot, v.getYRot(), 80, tag);
            }),
            new Case("shell_flip", 40, false, (sl, v, t, spot) -> shellFlip(sl, spot.add(0, 2, 0), 4)),
            new Case("disorder", 80, false, (sl, v, t, spot) -> {
                if (t == v) {
                    unbound(sl, v);
                    return;
                }
                disorder(sl, v, t, 60, tagFor("disorder"));
                Later.run(sl, 66, () -> unbound(sl, v));
            }),
            new Case("paces", 90, true, (sl, v, t, spot) -> {
                String tag = tagFor("decoy");
                Vec3 from = v.position();
                paces(sl, v, from, spot, 80, tag);
                Later.run(sl, 50, () -> decoyBurst(sl, from, tag));
                Later.run(sl, 70, () -> untouchable(sl, v));
            }),
            new Case("mausoleum", 140, false, (sl, v, t, spot) -> {
                String tag = tagFor("mausoleum");
                float yaw = yawToward(spot, v.position());
                raiseMausoleum(sl, spot, yaw, 200, tag);
                for (int i = 0; i < 3; i++) Later.run(sl, MAUSOLEUM_RISEN + 10 + i * 20, () -> sanctuaryPulse(sl, spot, 8));
                Later.run(sl, MAUSOLEUM_RISEN + 10, () -> sanctuaryHum(sl, spot));
                Later.run(sl, MAUSOLEUM_RISEN + 30, () -> sanctuaryMend(sl, v));
                Later.run(sl, 90, () -> mausoleumHit(sl, spot));
                Later.run(sl, 110, () -> mausoleumCrumble(sl, spot, yaw, tag));
            }),
            new Case("step", 60, false, (sl, v, t, spot) -> {
                String tag = tagFor("step_m");
                raiseMausoleum(sl, spot, v.getYRot() + 180f, 60, tag);
                Later.run(sl, MAUSOLEUM_RISEN + 4, () -> step(sl, v, v.position(), spot.add(Spells.flatLook(v).scale(-1.2))));
            }),
            new Case("corrosion", 60, true, (sl, v, t, spot) -> {
                corrosion(sl, v, 10);
                if (t != v) {
                    Later.run(sl, 10, () -> corroded(sl, t, 40, tagFor("corroded")));
                    for (int i = 1; i < 3; i++) {
                        float bite = i * 6f;
                        Later.run(sl, 10 + i * 20, () -> corrosionBite(sl, t, bite));
                    }
                }
            }),
            new Case("majesty", 60, true, (sl, v, t, spot) -> {
                majesty(sl, v, 10);
                if (t != v) Later.run(sl, 10, () -> kneel(sl, t, 40, tagFor("kneel")));
            }),
            new Case("bestowment", 160, false, (sl, v, t, spot) -> {
                if (t == v) return;
                Feeling[] all = Feeling.values();
                for (int i = 0; i < all.length; i++) {
                    Feeling feeling = all[i];
                    Later.run(sl, i * 30, () -> bestowment(sl, v, t, feeling, 25, tagFor("feeling")));
                }
            }),
            new Case("exploit", 110, true, (sl, v, t, spot) -> {
                String tag = tagFor("glide");
                exploitGlide(sl, v, 40, tag);
                Later.run(sl, 40, () -> exploitGlideEnd(sl, v, tag));
                if (t != v) Later.run(sl, 50, () -> exploitStrip(sl, v, t));
                Later.run(sl, 75, () -> exploitExtend(sl, v));
                Later.run(sl, 100, () -> moonLeap(sl, v));
            }),
            new Case("magnify", 100, false, (sl, v, t, spot) -> {
                String tag = tagFor("magnify");
                magnify(sl, v, 30, tag);
                Later.run(sl, 30, () -> {
                    magnifyEnd(sl, v, tag);
                    int flight = magnifiedFlower(sl, v, spot);
                    Later.run(sl, flight, () -> quake(sl, spot, 6));
                });
                if (t != v) Later.run(sl, 70, () -> {
                    seize(sl, v, t);
                    magnifiedHit(sl, chestOf(t));
                });
            }),
            new Case("salvo", 120, false, (sl, v, t, spot) -> {
                List<Vec3> tops = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                    float yaw = v.getYRot() + 90f + i * 45f;
                    Vec3 ground = v.position().add(ahead(yaw).scale(5));
                    raiseMausoleum(sl, ground, yaw + 180f, 110, tagFor("salvo_m"));
                    tops.add(ground.add(0, 3.6, 0));
                }
                if (t != v) Later.run(sl, MAUSOLEUM_RISEN + 10, () -> {
                    salvo(sl, tops, t);
                    for (int i = 0; i < tops.size(); i++) Later.run(sl, i * SALVO_STEP, () -> salvoHit(sl, t));
                });
            }),
            new Case("mausoleum_blast", 80, false, (sl, v, t, spot) -> {
                String tag = tagFor("blast_m");
                float yaw = yawToward(spot, v.position());
                raiseMausoleum(sl, spot, yaw, 200, tag);
                Later.run(sl, MAUSOLEUM_RISEN + 10, () -> mausoleumBlast(sl, spot, yaw, 5, tag));
            }),
            new Case("ruin", 40, false, (sl, v, t, spot) -> {
                Vec3 to = t == v ? v.getEyePosition().add(v.getLookAngle().scale(24)) : chestOf(t);
                ruin(sl, v, to);
                if (t != v) Later.run(sl, RUIN_FIRES, () -> ruinHit(sl, t));
            }),
            new Case("frenzy", 50, true, (sl, v, t, spot) -> {
                List<LivingEntity> targets = t == v ? List.of() : List.of(t);
                frenzy(sl, v, targets);
                if (t != v) for (int i = 0; i < 12; i += 3) Later.run(sl, FRENZY_FIRST + i * FRENZY_STEP, () -> frenzyHit(sl, t));
            }),
            new Case("entropy", 170, false, (sl, v, t, spot) -> {
                String tag = tagFor("entropy");
                entropy(sl, spot, 8, 160, tag);
                for (int i = 1; i < 8; i++) {
                    float strength = Math.min(1f, i / 7f);
                    Later.run(sl, i * 20, () -> entropyPulse(sl, spot, 8, strength));
                }
            }),
            new Case("abolition", 150, false, (sl, v, t, spot) -> {
                String tag = tagFor("abolition");
                Vec3 center = spot.add(0, 2, 0);
                abolition(sl, center, 6, 200, tag);
                Later.run(sl, 140, () -> abolitionEnd(sl, center, 6, tag));
            }),
            new Case("exile", 90, false, (sl, v, t, spot) -> {
                Entity who = t == v ? v : t;
                exile(sl, who);
                Later.run(sl, 60, () -> exileReturn(sl, who));
            }),
            new Case("inversion", 110, true, (sl, v, t, spot) -> {
                String tag = tagFor("inversion");
                inversion(sl, v, 10, 100, tag);
                for (int i = 1; i < 5; i++) Later.run(sl, i * 20, () -> inversionPulse(sl, v, 10));
                if (t != v) for (int i = 1; i < 4; i++) Later.run(sl, i * 25, () -> inverted(sl, t));
            }),
            new Case("coronation", 130, true, (sl, v, t, spot) -> {
                String tag = tagFor("crown");
                coronation(sl, v, 120, tag);
                Later.run(sl, CROWNED + 40, () -> coronationPulse(sl, v, 10));
                Later.run(sl, CROWNED + 80, () -> coronationPulse(sl, v, 10));
                Later.run(sl, 120, () -> coronationEnd(sl, v, tag));
            }),
            new Case("decree", 60, false, (sl, v, t, spot) -> {
                if (t != v) decree(sl, v, t, 9);
            }),
            new Case("rebirth", 160, true, (sl, v, t, spot) -> {
                String tag = tagFor("rebirth_m");
                float yaw = yawToward(spot, v.position());
                raiseMausoleum(sl, spot, yaw, 200, tag);
                Later.run(sl, MAUSOLEUM_RISEN + 10, () -> rebirthFall(sl, v));
                Later.run(sl, MAUSOLEUM_RISEN + 60, () -> {
                    rebirthRise(sl, v, spot);
                    Later.run(sl, 8, () -> mausoleumCrumble(sl, spot, yaw, tag));
                });
                Later.run(sl, 150, () -> lastRebirth(sl, v));
            }),
            new Case("order", 100, true, (sl, v, t, spot) -> {
                for (int i = 0; i <= ORDER_CAP; i++) {
                    int count = i;
                    Later.run(sl, i * 8, () -> {
                        order(v, count);
                        if (count > 0) orderRaised(sl, v);
                    });
                }
                Later.run(sl, 95, () -> orderEnd(v));
            }),
            new Case("statuses", 80, true, (sl, v, t, spot) -> {
                for (Status s : Status.values()) status(v, s, 60 + s.ordinal());
            }),
            new Case("passives", 60, true, (sl, v, t, spot) -> {
                for (int i = 0; i < 3; i++) Later.run(sl, i * 20, () -> {
                    presence(sl, v, 6);
                    beyondTheWorld(sl, v);
                });
                if (v instanceof ServerPlayer sp && t != v) {
                    for (int i = 0; i < 6; i++) Later.run(sl, i * 8, () -> weaknessDetection(sp, t));
                }
                Later.run(sl, 30, () -> looseOrder(sl, chestOf(v).add(Spells.flatLook(v).scale(1.5))));
            }),
            new Case("combo", 170, false, (sl, v, t, spot) -> {
                for (int i = 0; i < 12; i++) {
                    int step = i;
                    Later.run(sl, i * 13, () -> combo(sl, v, t, step));
                }
            }));

    /** The names /abolitionfx knows. */
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
        event.getDispatcher().register(Commands.literal("abolitionfx")
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
