package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.Countdown;
import net.lukario.circleofinevitability.combat.Gaze;
import net.lukario.circleofinevitability.combat.Painting;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Territory;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Construct;
import net.lukario.circleofinevitability.status.Observation;
import net.lukario.circleofinevitability.status.Unnoticed;
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
            p.discard();
        });
        return p;
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
}
