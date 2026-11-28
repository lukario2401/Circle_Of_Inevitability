package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.combat.Countdown;
import net.lukario.circleofinevitability.combat.FollowerSummon;
import net.lukario.circleofinevitability.combat.Gaze;
import net.lukario.circleofinevitability.combat.Painting;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Territory;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.ConjuredArmor;
import net.lukario.circleofinevitability.status.Construct;
import net.lukario.circleofinevitability.status.DeathWard;
import net.lukario.circleofinevitability.status.Dispel;
import net.lukario.circleofinevitability.status.Exile;
import net.lukario.circleofinevitability.status.Falter;
import net.lukario.circleofinevitability.status.Flight;
import net.lukario.circleofinevitability.status.Form;
import net.lukario.circleofinevitability.status.Hostel;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.NoHeal;
import net.lukario.circleofinevitability.status.Observation;
import net.lukario.circleofinevitability.status.Phase;
import net.lukario.circleofinevitability.status.Silence;
import net.lukario.circleofinevitability.status.Unnoticed;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Consumer;

/** The Observer's rules and wheels (docs/pathways/observer/OBSERVER.md). Run: ./gradlew runGameTestServer -PobserverTests */
public final class ObserverGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, "circle_of_inevitability");

    static {
        FUNCTIONS.register("observer_observation", () -> ObserverGameTests::observation);
        FUNCTIONS.register("observer_gaze", () -> ObserverGameTests::gaze);
        FUNCTIONS.register("observer_painting_limits", () -> ObserverGameTests::paintingLimits);
        FUNCTIONS.register("observer_painting_stare", () -> ObserverGameTests::paintingStare);
        FUNCTIONS.register("observer_territory", () -> ObserverGameTests::territory);
        FUNCTIONS.register("observer_wheels", () -> ObserverGameTests::wheels);
        FUNCTIONS.register("observer_phase", () -> ObserverGameTests::phase);
        FUNCTIONS.register("observer_hostel", () -> ObserverGameTests::hostel);
        FUNCTIONS.register("observer_sealed_ward", () -> ObserverGameTests::sealedWard);
        FUNCTIONS.register("observer_dimension", () -> ObserverGameTests::dimension);
        FUNCTIONS.register("observer_strings", () -> ObserverGameTests::strings);
        FUNCTIONS.register("observer_sublunary", () -> ObserverGameTests::sublunary);
        FUNCTIONS.register("observer_painting_world", () -> ObserverGameTests::paintingWorld);
        FUNCTIONS.register("observer_passives", () -> ObserverGameTests::passives);
    }

    private ObserverGameTests() {}

    static ServerPlayer player(GameTestHelper h, int sequence) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        Vec3 at = h.absoluteVec(new Vec3(1.5, 1, 1.5));
        p.setPos(at.x, at.y, at.z);
        SoulCore.setPathway(p, Observer.PATHWAY);
        SoulCore.setSequence(p, sequence);
        SoulCore.setSpiritualityQuietly(p, Float.MAX_VALUE);
        h.addCleanup(ok -> {
            Territory.close(p);
            Painting.clearAll(p);
            Observation.forget(p);
            Countdown.cancel(p);
            for (var mob : Construct.ofOwner(p, ObserverFiction.KNIGHTS)) Construct.end(mob);
            Phase.leave(p);
            if (Form.isIn(p, ObserverDimension.SHADOW_FORM)) Form.end(p);
            Hostel.empty(p);
            ObserverTerritory.closeWorld(p);
            for (int i = 0; i < 3; i++) FollowerSummon.dismiss(p, ObserverTerritory.SERVANT + i);
            ObserverSublunary.clearState(p);
            DeathWard.disarm(p);
            p.discard();
        });
        return p;
    }

    /** Faces 'p' toward 'at' (the crosshair on it). */
    private static void face(ServerPlayer p, Vec3 at) {
        p.lookAt(EntityAnchorArgument.Anchor.EYES, at);
    }

    private static void full(ServerPlayer p) {
        SoulCore.setSpiritualityQuietly(p, Float.MAX_VALUE);
    }

    private static Zombie zombie(GameTestHelper h, double x, double z) {
        Zombie zombie = h.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new Vec3(x, 1, z));
        return zombie;
    }

    /** Levels build, stop at 3, hold teleports at 3 and are spent. Two watchers keep their own. */
    private static void observation(GameTestHelper h) {
        ServerPlayer p = player(h, 9);
        Zombie z = zombie(h, 1.5, 1.5);
        h.assertTrue(Observation.level(p, z) == 0, "Nothing watched yet");
        Observation.watch(p, z, Observation.BUILD_TICKS - 5);
        h.assertTrue(Observation.level(p, z) == 0, "Not long enough for a level");
        Observation.watch(p, z, 5);
        h.assertTrue(Observation.level(p, z) == 1, "Two seconds in sight: Watched");
        Observation.raise(p, z, 5);
        h.assertTrue(Observation.level(p, z) == Observation.MAX, "It stops at 3");
        h.assertTrue(Root.has(z, RootRestriction.TELEPORT), "Seen through: it cannot teleport near the watcher");
        ServerPlayer other = player(h, 9);
        h.assertTrue(Observation.level(other, z) == 0, "Another watcher has its own levels");
        h.assertTrue(Observation.spend(p, z) == Observation.MAX && Observation.level(p, z) == 0, "Spent: all of them");
        h.assertTrue(!Root.has(z, RootRestriction.TELEPORT), "Spent: the hold is gone");
        h.succeed();
    }

    /** Looking means within the angle, within range and with a clear line. */
    private static void gaze(GameTestHelper h) {
        Zombie z = zombie(h, 1.5, 1.5);
        z.setYRot(0f);                                                          // looking toward +z
        z.setXRot(0f);
        Vec3 eye = z.getEyePosition();
        h.assertTrue(Gaze.looking(z, eye.add(0, 0, 1.2), 55, 20), "Straight ahead is seen: " + z.getLookAngle());
        h.assertTrue(!Gaze.looking(z, eye.add(0, 0, -1.2), 55, 20), "Behind is not");
        h.assertTrue(!Gaze.looking(z, eye.add(0, 0, 30), 55, 20), "Too far is not");
        h.assertTrue(Gaze.stare(z, "test", true, 5, 10) < 1f && Gaze.stare(z, "test", true, 5, 10) >= 1f, "A stare fills");
        h.assertTrue(Gaze.stare(z, "test", false, 5, 10) == 0f && Gaze.held(z, "test") == 0, "Looking away forgets it");
        h.succeed();
    }

    /** A fourth of a kind replaces the oldest; power fades to two thirds. */
    private static void paintingLimits(GameTestHelper h) {
        ServerPlayer p = player(h, 7);
        int[] faded = {0};
        Vec3 spot = h.absoluteVec(new Vec3(1.5, 2, 1.5));
        Painting.Hung first = null;
        for (int i = 0; i < 4; i++) {
            Painting.Hung hung = Painting.of("observer_test").lasts(40).stare(9999)
                    .onEnd((one, spent) -> { if (!spent) faded[0]++; })
                    .hang(p, h.getLevel(), spot, new Vec3(0, 0, 1), 3);
            if (i == 0) first = hung;
        }
        h.assertTrue(Painting.of(p, "observer_test").size() == 3, "Three of a kind at most");
        h.assertTrue(first != null && first.isOver() && faded[0] == 1, "The oldest faded");
        h.assertTrue(Math.abs(Painting.of(p, "observer_test").getFirst().power() - 1f) < 0.01f, "Fresh: full power");
        h.runAfterDelay(30, () -> {
            float power = Painting.of(p, "observer_test").isEmpty() ? 0f : Painting.of(p, "observer_test").getFirst().power();
            h.assertTrue(power < 0.9f && power >= 1f - Painting.FADE - 0.01f, "Power fades toward two thirds: " + power);
        });
        h.runAfterDelay(50, () -> {
            h.assertTrue(Painting.of(p, "observer_test").isEmpty() && faded[0] == 4, "Their time ends them");
            h.succeed();
        });
    }

    /** An enemy that keeps a canvas in view for the stare takes it, and the canvas is spent. */
    private static void paintingStare(GameTestHelper h) {
        ServerPlayer p = player(h, 7);
        Zombie z = zombie(h, 1.5, 0.5);
        z.setYRot(0f);
        z.setYHeadRot(0f);
        z.setXRot(0f);
        Vec3 spot = z.getEyePosition().add(0, 0, 1.5);
        boolean[] seen = {false};
        boolean[] spent = {false};
        Painting.of("observer_test_stare").lasts(200).stare(20)
                .onSeen((hung, viewer) -> seen[0] = viewer == z)
                .onEnd((hung, wasSpent) -> spent[0] = wasSpent)
                .hang(p, h.getLevel(), spot, new Vec3(0, 0, -1), 3);
        h.succeedWhen(() -> {
            h.assertTrue(seen[0], "The zombie took the Painting");
            h.assertTrue(spent[0], "The canvas is spent");
        });
    }

    /** The Territory covers its ground and makes its owner's casts cheaper. */
    private static void territory(GameTestHelper h) {
        ServerPlayer p = player(h, 9);
        float before = Spells.costFor(p, 100f);
        ObserverTerritory.plant(p, h.getLevel(), p.position());
        h.assertTrue(Territory.isOpen(p) && Territory.inside(p, p), "The Territory opens round the totem");
        h.assertTrue(Math.abs(Spells.costFor(p, 100f) - before * Observer.TERRITORY_COST) < 0.01f, "Casts are 15% cheaper inside");
        h.assertTrue(Territory.radius(p) == ObserverTerritory.RADIUS, "Sixteen blocks at Shaman");
        Territory.close(p);
        h.assertTrue(!Territory.isOpen(p) && Math.abs(Spells.costFor(p, 100f) - before) < 0.01f, "Closed: the bonus is gone");
        h.succeed();
    }

    /** Every wheel entry that needs no target goes off; toggles end free; the ritual waits before paying. */
    private static void wheels(GameTestHelper h) {
        ServerPlayer p = player(h, 6);
        p.setYRot(0f);
        p.setXRot(0f);
        h.assertTrue(Observer.TERRITORY.cast(p, Observer.TERRITORY.entry("totem_territory")) == AbilityWheel.Result.FREE
                && Countdown.isRunning(p), "The ritual starts and nothing is paid yet");
        Countdown.cancel(p);
        h.assertTrue(Observer.SIGHT.cast(p, Observer.SIGHT.entry("true_sight")) == AbilityWheel.Result.DONE
                && ObserverSight.isTrueSight(p), "True Sight opens");
        h.assertTrue(Observer.SIGHT.cast(p, Observer.SIGHT.entry("true_sight")) == AbilityWheel.Result.FREE
                && !ObserverSight.isTrueSight(p), "Pressed again it ends, free");
        SoulCore.setSpiritualityQuietly(p, Float.MAX_VALUE);
        h.assertTrue(Observer.SIGHT.cast(p, Observer.SIGHT.entry("many_angles")) == AbilityWheel.Result.DONE, "Many Angles opens");
        h.assertTrue(Observer.CANVAS.cast(p, Observer.CANVAS.entry("riot_of_colours")) == AbilityWheel.Result.DONE, "A Painting is painted");
        SoulCore.setSpiritualityQuietly(p, Float.MAX_VALUE);
        h.assertTrue(Observer.FICTION.cast(p, Observer.FICTION.entry("painted_face")) == AbilityWheel.Result.DONE
                && Unnoticed.isUnnoticed(p), "Painted Face: unnoticed");
        SoulCore.setSpiritualityQuietly(p, Float.MAX_VALUE);
        h.assertTrue(Observer.FICTION.cast(p, Observer.FICTION.entry("manifest_knight")) == AbilityWheel.Result.DONE
                && Construct.ofOwner(p, ObserverFiction.KNIGHTS).size() == 1, "A Knight steps out of the page");
        SoulCore.setSpiritualityQuietly(p, Float.MAX_VALUE);                    // a full bar again
        h.assertTrue(Observer.FICTION.cast(p, Observer.FICTION.entry("manifest_knight")) == AbilityWheel.Result.DONE,
                "A second Knight");
        h.runAfterDelay(ObserverFx.PAINT_TICKS + 2, () -> {
            h.assertTrue(Construct.ofOwner(p, ObserverFiction.KNIGHTS).size() == 1,
                    "One Knight at a time: " + Construct.ofOwner(p, ObserverFiction.KNIGHTS).size());
            h.assertTrue(Painting.of(p, "observer_riot_of_colours").size() == 1, "The Riot hangs once painted");
            h.succeed();
        });
    }

    /** Phase hides and lets you fly; another cast brings you back; pressed again it ends, free. */
    private static void phase(GameTestHelper h) {
        ServerPlayer p = player(h, 5);
        h.assertTrue(Observer.DIMENSION.cast(p, Observer.DIMENSION.entry("phase")) == AbilityWheel.Result.DONE
                && Phase.isPhased(p) && Concealment.isHidden(p) && Flight.has(p), "Into the Phase: hidden and flying");
        full(p);
        Casts.wheelCast(p, Observer.SIGHT, Observer.SIGHT.entry("true_sight"));
        h.assertTrue(!Phase.isPhased(p) && !Concealment.isHidden(p), "A cast brings you back");
        full(p);
        h.assertTrue(Observer.DIMENSION.cast(p, Observer.DIMENSION.entry("phase")) == AbilityWheel.Result.DONE, "In again");
        h.assertTrue(Observer.DIMENSION.cast(p, Observer.DIMENSION.entry("phase")) == AbilityWheel.Result.FREE
                && !Phase.isPhased(p), "Pressed again: back, free");
        h.succeed();
    }

    /** A weakened mob is lodged and let out again as a summon. */
    private static void hostel(GameTestHelper h) {
        ServerPlayer p = player(h, 4);
        Zombie z = zombie(h, 1.5, 2.5);
        z.setHealth(4f);
        face(p, z.getBoundingBox().getCenter());
        h.assertTrue(Observer.DIMENSION.cast(p, Observer.DIMENSION.entry("hostel")) == AbilityWheel.Result.DONE
                && z.isRemoved() && Hostel.count(p) == 1, "Lodged: " + Hostel.count(p));
        p.setXRot(-90f);                                                        // nothing in sight
        full(p);
        h.assertTrue(Observer.DIMENSION.cast(p, Observer.DIMENSION.entry("hostel")) == AbilityWheel.Result.DONE
                && Hostel.count(p) == 0, "Let out");
        h.assertTrue(!h.getLevel().getEntitiesOfClass(Zombie.class, p.getBoundingBox().inflate(6), Marionette::isSummoned).isEmpty(),
                "It fights for you as a summon");
        h.succeed();
    }

    /** A sealed ward is past dispelling; Foregone Survival will not replace it. */
    private static void sealedWard(GameTestHelper h) {
        ServerPlayer p = player(h, 1);
        DeathWard.armSealed(p, "test", 200, 0.8f, 20, null);
        Dispel.statuses(p);
        h.assertTrue(DeathWard.isArmed(p) && DeathWard.isSealed(p), "Dispel does not take it off");
        p.setXRot(-90f);
        h.assertTrue(Observer.SUBLUNARY.cast(p, Observer.SUBLUNARY.entry("foregone_survival")) == AbilityWheel.Result.REFUSED,
                "A higher ending is already written");
        DeathWard.disarm(p);
        h.assertTrue(Observer.SUBLUNARY.cast(p, Observer.SUBLUNARY.entry("foregone_survival")) == AbilityWheel.Result.DONE
                && DeathWard.isArmedBy(p, ObserverSublunary.SURVIVAL), "Foregone Survival on yourself");
        Dispel.statuses(p);
        h.assertTrue(!DeathWard.isArmed(p), "That one can be dispelled");
        h.succeed();
    }

    /** Wings fold free, the Shadow Form ends free, Painted Lightning strikes. */
    private static void dimension(GameTestHelper h) {
        ServerPlayer p = player(h, 4);
        Zombie z = zombie(h, 1.5, 2.5);
        h.assertTrue(Observer.DIMENSION.cast(p, Observer.DIMENSION.entry("pixie_wings")) == AbilityWheel.Result.DONE
                && Flight.has(p), "Wings: flying");
        h.assertTrue(Observer.DIMENSION.cast(p, Observer.DIMENSION.entry("pixie_wings")) == AbilityWheel.Result.FREE
                && !Flight.has(p), "Folded, free");
        full(p);
        h.assertTrue(Observer.DIMENSION.cast(p, Observer.DIMENSION.entry("shadow_form")) == AbilityWheel.Result.DONE
                && ObserverDimension.inShadowForm(p), "Shadow Form");
        h.assertTrue(Observer.DIMENSION.cast(p, Observer.DIMENSION.entry("shadow_form")) == AbilityWheel.Result.FREE
                && !ObserverDimension.inShadowForm(p), "Ended, free");
        full(p);
        face(p, z.getBoundingBox().getCenter());
        String why = "aimed at " + Spells.aimEnemy(p, h.getLevel(), 32) + ", max " + z.getMaxHealth();
        h.assertTrue(Observer.DIMENSION.cast(p, Observer.DIMENSION.entry("painted_lightning")) == AbilityWheel.Result.DONE, "Bolts");
        h.runAfterDelay(2, () -> {
            h.assertTrue(z.getHealth() < z.getMaxHealth() || !z.isAlive(), "The bolt struck: " + z.getHealth() + " " + why);
            h.succeed();
        });
    }

    /** Resonance gathers and bursts; Taut Chord plays; Dissonance shakes; Harmony goes off. */
    private static void strings(GameTestHelper h) {
        ServerPlayer p = player(h, 3);
        Zombie z = zombie(h, 1.5, 2.5);
        z.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
        z.setHealth(1000f);
        face(p, z.getBoundingBox().getCenter());
        h.assertTrue(Observer.STRINGS.cast(p, Observer.STRINGS.entry("resonance")) == AbilityWheel.Result.DONE, "Plucked");
        for (int i = 0; i < ObserverStrings.RESONANCE_MAX; i++) {
            z.invulnerableTime = 0;
            Spells.strike(p, z, 1f);
        }
        float afterHits = z.getHealth();
        full(p);
        h.assertTrue(Observer.STRINGS.cast(p, Observer.STRINGS.entry("dissonance")) == AbilityWheel.Result.DONE
                && Falter.chance(z) > 0f, "Dissonance: its blows go wide");
        full(p);
        h.assertTrue(Observer.STRINGS.cast(p, Observer.STRINGS.entry("harmony")) == AbilityWheel.Result.DONE, "Harmony");
        full(p);
        h.assertTrue(Observer.STRINGS.cast(p, Observer.STRINGS.entry("taut_chord")) == AbilityWheel.Result.DONE, "Strings drawn");
        h.runAfterDelay(3, () -> h.assertTrue(z.getHealth() <= afterHits - ObserverStrings.RESONANCE_DAMAGE * 4,
                "Five Resonances burst: " + afterHits + " -> " + z.getHealth()));
        h.runAfterDelay(ObserverFx.TAUT_PLAYS + 3, () -> {
            h.assertTrue(z.getHealth() < afterHits - ObserverStrings.RESONANCE_DAMAGE * 4 - 1, "The chord was played: " + z.getHealth());
            h.succeed();
        });
    }

    /** The endings need Observation 3; Erase takes it out of the world; Bloom shields you; Fall Silent hushes what is behind you. */
    private static void sublunary(GameTestHelper h) {
        ServerPlayer p = player(h, 0);
        Zombie z = zombie(h, 1.5, 2.5);
        face(p, z.getBoundingBox().getCenter());
        h.assertTrue(Observer.SUBLUNARY.cast(p, Observer.SUBLUNARY.entry("foregone_loss")) == AbilityWheel.Result.REFUSED,
                "Not watched long enough: nothing is spent");
        Observation.raise(p, z, 3);
        h.assertTrue(Observer.SUBLUNARY.cast(p, Observer.SUBLUNARY.entry("foregone_loss")) == AbilityWheel.Result.DONE
                && NoHeal.isApplied(z), "Its ending is written");
        full(p);
        h.assertTrue(Observer.SUBLUNARY.cast(p, Observer.SUBLUNARY.entry("erase")) == AbilityWheel.Result.DONE
                && Exile.isExiled(z), "Rubbed out");
        Exile.bringBack(z);
        full(p);
        h.assertTrue(Observer.SUBLUNARY.cast(p, Observer.SUBLUNARY.entry("bloom")) == AbilityWheel.Result.DONE, "Bloom");
        full(p);
        face(p, p.getEyePosition().add(z.getEyePosition().subtract(p.getEyePosition()).scale(-1)));   // the zombie behind you
        h.assertTrue(Observer.SUBLUNARY.cast(p, Observer.SUBLUNARY.entry("fall_silent")) == AbilityWheel.Result.DONE
                && Silence.isSilenced(z), "Out of sight: silent");
        h.runAfterDelay(ObserverFx.EYE_OPEN + 2, () -> {
            h.assertTrue(ConjuredArmor.isWorn(p, ObserverSublunary.BLOOM_ARMOR), "The Bloom's shield is on you");
            h.succeed();
        });
    }

    /** The pocket world holds an enemy; cast again from inside it ends, free. Ink Servants come. */
    private static void paintingWorld(GameTestHelper h) {
        ServerPlayer p = player(h, 5);
        Zombie z = zombie(h, 2.5, 2.5);
        p.setXRot(90f);                                                         // at your feet
        h.assertTrue(Observer.TERRITORY.cast(p, Observer.TERRITORY.entry("painting_world")) == AbilityWheel.Result.DONE
                && ObserverTerritory.inWorld(p, z) && ObserverTerritory.inWorld(p, p), "The world stands round both");
        h.assertTrue(Observer.TERRITORY.cast(p, Observer.TERRITORY.entry("painting_world")) == AbilityWheel.Result.FREE
                && !ObserverTerritory.inWorld(p, z), "Ended from inside, free");
        full(p);
        h.assertTrue(Observer.TERRITORY.cast(p, Observer.TERRITORY.entry("ink_servants")) == AbilityWheel.Result.DONE
                && FollowerSummon.isActive(p, ObserverTerritory.SERVANT + 0), "The servants circle you");
        h.succeed();
    }

    /** Passives 5 to 0: hearts, no holds, Observation twice as fast. */
    private static void passives(GameTestHelper h) {
        ServerPlayer p = player(h, 0);
        Observer.bodyTick(p);
        h.assertTrue(Math.abs(p.getMaxHealth() - 24f) < 0.01f, "Demigod Body: two more hearts: " + p.getMaxHealth());
        h.assertTrue(Root.isImmune(p, RootRestriction.MOVE), "Shadow Body: nothing holds you");
        Zombie z = zombie(h, 1.5, 2.5);
        Observation.watch(p, z, Observation.BUILD_TICKS / 2);
        h.assertTrue(Observation.level(p, z) == 1, "Eyes Everywhere: twice as fast");
        h.succeed();
    }
}
