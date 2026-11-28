package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer.ObserverFx.Status;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.combat.Painting;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Territory;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.EffectDuration;
import net.lukario.circleofinevitability.status.Madness;
import net.lukario.circleofinevitability.status.Nocturnality;
import net.lukario.circleofinevitability.status.Observation;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Observer: the Sublunary Eye Pathway of Lord of the Mysteries (docs/pathways/observer/OBSERVER.md). Every key is a
 * wheel in the Star Dragon pattern: sneak + key opens the wheel to choose an entry, the key alone fires the last pick.
 * No entry has a cooldown; spirituality and the limits in the plan are the only bounds.
 *
 *   1 Territory  Totem Territory, Borrowed Power (Shaman 9)                         ObserverTerritory
 *   2 Sight      True Sight, Many Angles (Reporter 8), Read Intent (Painter 7)      ObserverSight
 *   3 Canvas     Riot of Colours, Itching House, Golden Sun (Painter 7),            ObserverCanvas
 *                Boat in a Storm, Self Portrait, Giant's Fist (Literature Enthusiast 6)
 *   4 Fiction    Painted Mark, Hide in Painting (Painter 7),                        ObserverFiction
 *                Painted Face, Manifest Knight, Manifest Archer (Literature Enthusiast 6)
 *   5 Dimension, 6 Strings, 7 Sublunary, and Ink Servants, Painting World, Fantasy Double: the second half (not built)
 *
 * Two rules carry it. Observation (status/Observation): whoever you keep in your crosshair for 2 seconds gains a level,
 * up to 3, and each level does more (it cannot hide, your hits lead it and do more, it cannot teleport). Paintings
 * (combat/Painting): a canvas that takes hold of the enemy who keeps it in view. Both are reusable on their own. The
 * Territory (combat/Territory) is the Shaman's home ground. Every cast's cost goes through costFor (addCostRule for more).
 * Every look and sound is in ObserverFx (see /observerfx).
 */
public class Observer {

    public static final String PATHWAY = "Observer";

    // =========================
    // The Sequences
    // =========================
    static final int SHAMAN = 9;
    static final int REPORTER = 8;
    static final int PAINTER = 7;
    static final int LITERATURE_ENTHUSIAST = 6;
    static final int PIXIE = 5;
    static final int VISITOR = 4;
    static final int STRING_PLAYER = 3;
    static final int DIMENSIONAL_SHADOW = 2;
    static final int OBSERVER = 1;
    static final int SUBLUNARY_EYE = 0;

    // =========================
    // Observation (docs/pathways/observer/OBSERVER.md, "Observation"); the levels themselves are in status/Observation
    // =========================
    static final double OBSERVATION_RANGE = 32.0;            // the crosshair reaches this far
    static final int OBSERVATION_CHECK = 5;                  // ticks between looks
    static final int OBSERVATION_STARE_CUT = 10;             // each level cuts this many ticks off a Painting's stare
    static final int EYES_TICKS = 6000;                      // the eyes over a watched enemy, at most

    // =========================
    // Passives (docs/pathways/observer/OBSERVER.md, "Passives"); the once-a-second ones run from SequencePassives
    // =========================
    static final float SPIRIT_SENSE_DARK = 0.5f;            // Spirit Sense (9): night vision where it is this dark...
    static final int SPIRIT_SENSE_REST = 60;                // ...and the chime of something hunting you, at most this often
    static final double HANDS_ATTACK_SPEED = 0.2;           // Reporter's Hands (8): 20% faster attacks (and Speed I)
    static final double PAINTER_EYE_RADIUS = 16.0;          // Painter's Eye (7): others' Paintings and Traps this close
    static final float FICTION_SHORTER = 0.6f;              // Fiction and Reality (6): these effects last 40% shorter

    // Territory bonus for the owner standing in it (combat/Territory)
    static final float TERRITORY_COST = 0.85f;              // casts 15% cheaper...
    static final float TERRITORY_COST_EYE = 0.7f;           // ...30% from Sublunary Eye (0)
    static final float TERRITORY_REGEN = 0.3f;              // spirituality back 30% faster

    // =========================
    // Saved on the player
    // =========================
    static final String SENSE_READY = "observer_sense_ready";
    static final String TOLD_PAINTINGS = "observer_painter_eye_told";

    private static final Identifier HANDS_ID = Physique.id("observer_reporter_hands");

    // =========================
    // Costs: every wheel entry goes through costFor
    // =========================

    /** A rule that changes what a cast costs: (player, cost, ability key) -> new cost. Once, in a static block. */
    @FunctionalInterface
    public interface CostRule {
        int cost(Player player, int cost, String ability);
    }

    private static final List<CostRule> COST_RULES = new ArrayList<>();
    private static final List<BiConsumer<Player, String>> AFTER_CAST = new ArrayList<>();

    public static void addCostRule(CostRule rule) {
        COST_RULES.add(rule);
    }

    /** Runs after every Observer cast that went off, with the entry's key. Once, in a static block. */
    public static void addAfterCast(BiConsumer<Player, String> action) {
        AFTER_CAST.add(action);
    }

    /** What a cast of 'cost' costs this player now: every addCostRule in turn. */
    public static int costFor(Player player, int cost, String ability) {
        if (cost <= 0 || Beyonder.isCastingUnbound(player) || !SoulCore.getPathway(player).equals(PATHWAY)) return cost;
        int result = cost;
        for (CostRule rule : COST_RULES) result = Math.max(0, rule.cost(player, result, ability));
        return result;
    }

    /** Pays for a cast that is paid later than the wheel pays (a ritual that finishes). False, with the message, if not. */
    static boolean pay(Player player, int cost, String ability) {
        int due = costFor(player, cost, ability);
        if (due > 0 && !Spells.paySpirituality(player, due)) return false;
        afterCast(player, ability);
        return true;
    }

    public static void afterCast(Player player, String ability) {
        if (Beyonder.isCastingUnbound(player) || !SoulCore.getPathway(player).equals(PATHWAY)) return;
        for (BiConsumer<Player, String> action : AFTER_CAST) action.accept(player, ability);
    }

    // =========================
    // The wheels
    // =========================

    static final AbilityWheel TERRITORY = AbilityWheel.create("observer_territory", "Territory")
            .pathway(PATHWAY).radius(72)
            .selectThenFire().noCooldowns()
            .entry("totem_territory", ObserverFx.WHEEL_TOTEM_TERRITORY, "Totem Territory",
                    ObserverTerritory.TOTEM_SEQUENCE, ObserverTerritory.TOTEM_COST, 0, ObserverTerritory::totemTerritory)
            .notCopyable()
            .costBy((player, cost) -> costFor(player, cost, "totem_territory"))
            .entry("borrowed_power", ObserverFx.WHEEL_BORROWED_POWER, "Borrowed Power",
                    ObserverTerritory.BORROW_SEQUENCE, ObserverTerritory.BORROW_COST, 0, ObserverTerritory::borrowedPower)
            .costBy((player, cost) -> costFor(player, cost, "borrowed_power"))
            .onCast((player, sl, entry) -> afterCast(player, entry.key()));

    static final AbilityWheel SIGHT = AbilityWheel.create("observer_sight", "Sight")
            .pathway(PATHWAY).radius(72)
            .selectThenFire().noCooldowns()
            .entry("true_sight", ObserverFx.WHEEL_TRUE_SIGHT, "True Sight",
                    ObserverSight.TRUE_SIGHT_SEQUENCE, ObserverSight.TRUE_SIGHT_COST, 0, ObserverSight::trueSight)
            .free(ObserverSight::isTrueSight)                                     // ending it early
            .costBy((player, cost) -> costFor(player, cost, "true_sight"))
            .entry("many_angles", ObserverFx.WHEEL_MANY_ANGLES, "Many Angles",
                    ObserverSight.ANGLES_SEQUENCE, ObserverSight.ANGLES_COST, 0, ObserverSight::manyAngles)
            .costBy((player, cost) -> costFor(player, cost, "many_angles"))
            .entry("read_intent", ObserverFx.WHEEL_READ_INTENT, "Read Intent",
                    ObserverSight.READ_SEQUENCE, ObserverSight.READ_COST, 0, ObserverSight::readIntent)
            .costBy((player, cost) -> costFor(player, cost, "read_intent"))
            .onCast((player, sl, entry) -> afterCast(player, entry.key()));

    static final AbilityWheel CANVAS = AbilityWheel.create("observer_canvas", "Canvas")
            .pathway(PATHWAY).radius(72)
            .selectThenFire().noCooldowns()
            .entry("riot_of_colours", ObserverFx.WHEEL_RIOT_OF_COLOURS, "Riot of Colours",
                    ObserverCanvas.RIOT_SEQUENCE, ObserverCanvas.RIOT_COST, 0, ObserverCanvas::riotOfColours)
            .costBy((player, cost) -> costFor(player, cost, "riot_of_colours"))
            .entry("itching_house", ObserverFx.WHEEL_ITCHING_HOUSE, "Itching House",
                    ObserverCanvas.ITCHING_SEQUENCE, ObserverCanvas.ITCHING_COST, 0, ObserverCanvas::itchingHouse)
            .costBy((player, cost) -> costFor(player, cost, "itching_house"))
            .entry("golden_sun", ObserverFx.WHEEL_GOLDEN_SUN, "Golden Sun",
                    ObserverCanvas.SUN_SEQUENCE, ObserverCanvas.SUN_COST, 0, ObserverCanvas::goldenSun)
            .costBy((player, cost) -> costFor(player, cost, "golden_sun"))
            .entry("boat_in_a_storm", ObserverFx.WHEEL_BOAT_IN_A_STORM, "Boat in a Storm",
                    ObserverCanvas.STORM_SEQUENCE, ObserverCanvas.STORM_COST, 0, ObserverCanvas::boatInAStorm)
            .costBy((player, cost) -> costFor(player, cost, "boat_in_a_storm"))
            .entry("self_portrait", ObserverFx.WHEEL_SELF_PORTRAIT, "Self Portrait",
                    ObserverCanvas.PORTRAIT_SEQUENCE, ObserverCanvas.PORTRAIT_COST, 0, ObserverCanvas::selfPortrait)
            .free(ObserverCanvas::hasPortrait)                                    // looking through the one that hangs
            .notCopyable()
            .costBy((player, cost) -> costFor(player, cost, "self_portrait"))
            .entry("giants_fist", ObserverFx.WHEEL_GIANTS_FIST, "Giant's Fist",
                    ObserverCanvas.FIST_SEQUENCE, ObserverCanvas.FIST_COST, 0, ObserverCanvas::giantsFist)
            .costBy((player, cost) -> costFor(player, cost, "giants_fist"))
            .onCast((player, sl, entry) -> afterCast(player, entry.key()));

    static final AbilityWheel FICTION = AbilityWheel.create("observer_fiction", "Fiction")
            .pathway(PATHWAY).radius(72)
            .selectThenFire().noCooldowns()
            .entry("painted_mark", ObserverFx.WHEEL_PAINTED_MARK, "Painted Mark",
                    ObserverFiction.MARK_SEQUENCE, ObserverFiction.MARK_COST, 0, ObserverFiction::paintedMark)
            .notCopyable()
            .costBy((player, cost) -> costFor(player, ObserverFiction.markCost(player, cost), "painted_mark"))
            .entry("hide_in_painting", ObserverFx.WHEEL_HIDE_IN_PAINTING, "Hide in Painting",
                    ObserverFiction.HIDE_SEQUENCE, ObserverFiction.HIDE_COST, 0, ObserverFiction::hideInPainting)
            .free(ObserverFiction::isHidden)                                      // stepping out
            .notCopyable()
            .costBy((player, cost) -> costFor(player, cost, "hide_in_painting"))
            .entry("painted_face", ObserverFx.WHEEL_PAINTED_FACE, "Painted Face",
                    ObserverFiction.FACE_SEQUENCE, ObserverFiction.FACE_COST, 0, ObserverFiction::paintedFace)
            .costBy((player, cost) -> costFor(player, cost, "painted_face"))
            .entry("manifest_knight", ObserverFx.WHEEL_MANIFEST_KNIGHT, "Manifest Knight",
                    ObserverFiction.KNIGHT_SEQUENCE, ObserverFiction.KNIGHT_COST, 0, ObserverFiction::manifestKnight)
            .costBy((player, cost) -> costFor(player, cost, "manifest_knight"))
            .entry("manifest_archer", ObserverFx.WHEEL_MANIFEST_ARCHER, "Manifest Archer",
                    ObserverFiction.ARCHER_SEQUENCE, ObserverFiction.ARCHER_COST, 0, ObserverFiction::manifestArcher)
            .costBy((player, cost) -> costFor(player, cost, "manifest_archer"))
            .onCast((player, sl, entry) -> afterCast(player, entry.key()));

    static {
        // the Shaman's Territory: cheaper casts and faster spirituality for its owner standing in it
        Territory.addCostScale(player -> !isObserver(player, SHAMAN) ? 1.0
                : isObserver(player, SUBLUNARY_EYE) ? TERRITORY_COST_EYE : TERRITORY_COST);
        Territory.addRegen(player -> isObserver(player, SHAMAN) ? TERRITORY_REGEN : 0.0);
        // Observation shortens the stare at an Observer's Painting
        Painting.addStareRule((viewer, hung, ticks) -> hung.kind().startsWith("observer_")
                ? Math.max(0, ticks - OBSERVATION_STARE_CUT * Observation.level(hung.owner(), viewer)) : ticks);
        // Fiction and Reality (6): illusions on you are short
        EffectDuration.addRule((entity, effect) -> entity instanceof Player player && isObserver(player, LITERATURE_ENTHUSIAST)
                && (effect.is(MobEffects.BLINDNESS) || effect.is(MobEffects.NAUSEA) || effect.is(MobEffects.DARKNESS))
                ? FICTION_SHORTER : 1.0);
        // the eyes over what you watch, and the picture on a watched player's screen
        Observation.addListener(Observer::observationChanged);
        // what the Self Portrait repeats, and casting ends Hide in Painting
        Casts.addListener((caster, sl, cast) -> {
            ObserverFiction.castWhileHidden(caster, cast);
            ObserverCanvas.portraitEcho(caster, sl, cast);
        });
        // Riot of Colours: madness at its height ends in slumber
        Madness.onBreak(ObserverCanvas::riotBreaks);
    }

    // =========================
    // The keys
    // =========================

    /** Key 1: the Territory wheel. */
    public static void observerTerritory(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (ready(player, bypassClassCheck, SHAMAN)) TERRITORY.press(player);
    }

    /** Key 2: the Sight wheel. */
    public static void observerSight(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (ready(player, bypassClassCheck, REPORTER)) SIGHT.press(player);
    }

    /** Key 3: the Canvas wheel. */
    public static void observerCanvas(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (ready(player, bypassClassCheck, PAINTER)) CANVAS.press(player);
    }

    /** Key 4: the Fiction wheel. */
    public static void observerFiction(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (ready(player, bypassClassCheck, PAINTER)) FICTION.press(player);
    }

    // =========================
    // Events
    // =========================

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ObserverEvents {

        @SubscribeEvent
        public static void onObserverTick(TickEvent.PlayerTickEvent.Post event) {
            Player player = event.player();
            if (!(player.level() instanceof ServerLevel sl)) return;
            ObserverCanvas.itchTick(player);                                     // anyone itching cannot sprint
            if (player.tickCount % 20 == 0) bodyTick(player);
            if (!(player instanceof ServerPlayer viewer) || !SoulCore.getPathway(player).equals(PATHWAY)) return;
            if (player.tickCount % OBSERVATION_CHECK == 0 && grown(player, SHAMAN)) watch(viewer, sl);
            if (player.tickCount % 10 == 0) ObserverSight.tick(viewer, sl);
            if (player.tickCount % 20 == 0) ObserverFiction.tick(viewer, sl);
            if (player.tickCount % 10 == 0) ObserverFiction.archers(viewer, sl);
        }

        @SubscribeEvent
        public static void onObserverHurt(LivingHurtEvent event) {
            LivingEntity hurt = event.getEntity();
            if (hurt.level().isClientSide()) return;
            ObserverTerritory.hurt(event);
            ObserverFiction.hurt(event);
        }

        @SubscribeEvent
        public static void onObserverSwing(AttackEntityEvent event) {
            if (!event.getEntity().level().isClientSide()) ObserverFiction.swing(event.getEntity());
        }

        /** True Sight: no illusion takes on you. */
        @SubscribeEvent
        public static void onObserverEffect(MobEffectEvent.Applicable event) {
            ObserverSight.effect(event);
        }

        /** Spirit Sense (9): a mob starts hunting you from where you cannot see it. */
        @SubscribeEvent
        public static void onObserverTarget(LivingChangeTargetEvent event) {
            if (!(event.getNewTarget() instanceof ServerPlayer hunted) || event.getEntity().level().isClientSide()) return;
            if (!isObserver(hunted, SHAMAN)) return;
            LivingEntity mob = event.getEntity();
            long now = hunted.level().getGameTime();
            if (hunted.getPersistentData().getLongOr(SENSE_READY, 0L) > now) return;
            Vec3 toMob = mob.position().subtract(hunted.position());
            boolean behind = toMob.horizontalDistanceSqr() > 0.01
                    && Spells.flatLook(hunted).dot(new Vec3(toMob.x, 0, toMob.z).normalize()) < 0.2;
            if (!behind && hunted.hasLineOfSight(mob)) return;
            hunted.getPersistentData().putLong(SENSE_READY, now + SPIRIT_SENSE_REST);
            ObserverFx.spiritChime(hunted, mob.position());
        }

        @SubscribeEvent
        public static void onObserverLogin(PlayerEvent.PlayerLoggedInEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onObserverRespawn(PlayerEvent.PlayerRespawnEvent event) {
            clearState(event.getEntity());
        }

        @SubscribeEvent
        public static void onObserverLogout(PlayerEvent.PlayerLoggedOutEvent event) {
            Observation.forget(event.getEntity());
            ObserverFiction.clearState(event.getEntity());
        }
    }

    // =========================
    // Observation
    // =========================

    /** Every OBSERVATION_CHECK ticks: whoever is in the crosshair is being watched. */
    private static void watch(ServerPlayer viewer, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(viewer, sl, OBSERVATION_RANGE);
        if (target == null) {
            Observation.lookAway(viewer);
            return;
        }
        int level = Observation.watch(viewer, target, OBSERVATION_CHECK);
        if (level < Observation.MAX && viewer.tickCount % 10 == 0) ObserverFx.staring(viewer, target);
    }

    /** A level changed: the eyes over it (for the watcher), the picture on a watched player's screen. */
    private static void observationChanged(LivingEntity viewer, LivingEntity target, int level) {
        if (viewer instanceof ServerPlayer watcher && SoulCore.getPathway(watcher).equals(PATHWAY)) {
            // the eyes stay until the level changes again (a drop is told within a second): no clock of their own
            ObserverFx.observed(watcher, target, level, EYES_TICKS);
            Integer was = LAST_SHOWN.put(watcher.getUUID() + "|" + target.getUUID(), level);
            if (level > 0 && (was == null || level > was)) ObserverFx.observationRises(watcher, target, level);
            if (level <= 0) LAST_SHOWN.remove(watcher.getUUID() + "|" + target.getUUID());
        }
        if (target instanceof Player) {
            ObserverFx.statusEnd(target, Status.WATCHED);
            ObserverFx.statusEnd(target, Status.STUDIED);
            ObserverFx.statusEnd(target, Status.SEEN_THROUGH);
            Status status = switch (level) {
                case 1 -> Status.WATCHED;
                case 2 -> Status.STUDIED;
                case 3 -> Status.SEEN_THROUGH;
                default -> null;
            };
            if (status != null) ObserverFx.status(target, status, Observation.DROP_TICKS);
        }
    }

    private static final java.util.Map<String, Integer> LAST_SHOWN = new java.util.HashMap<>();

    // =========================
    // Passives
    // =========================

    /** Shaman (9), once a second: Spirit Sense. Night vision wherever it is dark. */
    public static void spiritSense(Player player) {
        if (Nocturnality.darkness(player) >= SPIRIT_SENSE_DARK || player.level().getMaxLocalRawBrightness(player.blockPosition()) <= 6) {
            Spells.keepEffect(player, MobEffects.NIGHT_VISION, 0, 400);
        }
    }

    /**
     * Painter (7), once a second: Painter's Eye. Hung Paintings and hidden Traps of others within PAINTER_EYE_RADIUS show
     * for you, and a Painting new to you says who made it and what it does.
     */
    public static void paintersEye(Player player) {
        if (!(player instanceof ServerPlayer viewer) || !(player.level() instanceof ServerLevel sl)) return;
        for (Vec3 trap : net.lukario.circleofinevitability.combat.Trap.hiddenNear(sl, player.position(), PAINTER_EYE_RADIUS)) {
            ObserverFx.painterEye(viewer, trap);
        }
        String told = player.getPersistentData().getStringOr(TOLD_PAINTINGS, "");
        StringBuilder now = new StringBuilder();
        for (Painting.Hung hung : Painting.near(sl, player.position(), PAINTER_EYE_RADIUS)) {
            if (hung.owner().getUUID().equals(player.getUUID())) continue;
            ObserverFx.painterEye(viewer, hung.spot());
            now.append(hung.id(), 0, 8).append(',');
            if (!told.contains(hung.id().substring(0, 8))) {
                viewer.sendOverlayMessage(net.minecraft.network.chat.Component.literal(ObserverCanvas.describe(hung)));
            }
        }
        player.getPersistentData().putString(TOLD_PAINTINGS, now.toString());
    }

    /** Once a second, for every player: Reporter's Hands' faster attacks. */
    static void bodyTick(Player player) {
        Physique.keep(player, Attributes.ATTACK_SPEED, HANDS_ID, HANDS_ATTACK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE,
                isObserver(player, REPORTER));
    }

    // =========================
    // Helpers shared by the abilities
    // =========================

    /** True for this Pathway at this Sequence or stronger. */
    public static boolean isObserver(Player player, int sequence) {
        return SoulCore.getPathway(player).equals(PATHWAY) && Beyonder.hasSequence(player, sequence);
    }

    /** The same for anyone: a player as above, a Beyonder mob of this Pathway by its own Sequence. */
    public static boolean isObserver(LivingEntity entity, int sequence) {
        if (entity instanceof Player player) return isObserver(player, sequence);
        return PATHWAY.equals(Beyonders.pathwayOf(entity)) && Beyonder.hasSequence(entity, sequence);
    }

    /** True while casting at that Sequence: their own Pathway, or a stolen cast (no Pathway check). */
    static boolean grown(LivingEntity player, int sequence) {
        return Beyonder.hasSequence(player, sequence);
    }

    /** Has this Pathway (or the check is skipped) and has reached the Sequence the key needs. */
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
        player.getPersistentData().remove(SENSE_READY);
        player.getPersistentData().remove(TOLD_PAINTINGS);
        ObserverTerritory.clearState(player);
        ObserverSight.clearState(player);
        ObserverFiction.clearState(player);
        if (!SoulCore.getPathway(player).equals(PATHWAY)) return;
        for (Status status : Status.values()) ObserverFx.statusEnd(player, status);   // a clock left running before death
    }
}
