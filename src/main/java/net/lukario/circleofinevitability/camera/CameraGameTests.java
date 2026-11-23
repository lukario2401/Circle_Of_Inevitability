package net.lukario.circleofinevitability.camera;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStars;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Consumer;

public final class CameraGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, CircleOfInevitability.MOD_ID);
    static {
        FUNCTIONS.register("camera_walls", () -> CameraGameTests::walls);
        FUNCTIONS.register("camera_sessions", () -> CameraGameTests::sessions);
        FUNCTIONS.register("camera_star_steps", () -> CameraGameTests::starSteps);
        FUNCTIONS.register("camera_scripted", () -> CameraGameTests::scripted);
    }

    private CameraGameTests() {}

    private static CameraPreset preset(String json) {
        return CameraPreset.CODEC.parse(JsonOps.INSTANCE, StrictJsonParser.parse(json)).getOrThrow();
    }

    private static void walls(GameTestHelper h) {
        Vec3 from = h.absoluteVec(new Vec3(0.2, 1.4, 1.5));
        Vec3 to = from.add(2.6, 0, 0);
        BlockPos block = BlockPos.containing(from.add(1, 0, 0));
        var level = h.getLevel();
        var solid = preset("{}").collision();
        var phase = preset("{\"collision\":{\"mode\":\"phase\"}}").collision();
        var allow = preset("{\"collision\":{\"mode\":\"allowlist\",\"tag\":\"circle_of_inevitability:camera_wards\"}}").collision();
        var wards = CameraPresets.loaded.get(Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "spirit_scouting")).collision();
        level.setBlockAndUpdate(block, Blocks.GLASS_PANE.defaultBlockState());
        h.assertTrue(CameraTravel.sweep(level, from, to, solid).x < block.getX() + 0.5, "A fast camera must stop at a pane");
        h.assertTrue(CameraTravel.sweep(level, from, to, phase).equals(to), "Phase passes through a pane");
        h.assertTrue(CameraTravel.sweep(level, from, to, wards).equals(to), "The ward preset passes ordinary walls");
        level.setBlockAndUpdate(block, Blocks.OBSIDIAN.defaultBlockState());
        Vec3 stopped = CameraTravel.sweep(level, from, to, wards);
        h.assertTrue(stopped.x < block.getX() && stopped.x > from.x, "Ward stops the camera just outside its face");
        Vec3 touching = new Vec3(block.getX() - 0.16, from.y, from.z);
        h.assertTrue(CameraTravel.sweep(level, touching, to, wards).distanceTo(touching) < 1e-5,
                "Starting exactly on a face cannot bypass its ward");
        h.assertTrue(CameraTravel.sweep(level, touching, from, wards).equals(from), "A touching camera can leave the face");
        h.assertTrue(CameraTravel.sweep(level, stopped, from, wards).equals(from), "Camera can back away from the ward");
        h.assertTrue(CameraTravel.sweep(level, from, to, allow).equals(to), "Allowlist inverts the ward filter");
        h.assertTrue(!CameraTravel.fits(level, Vec3.atCenterOf(block), wards), "Starting inside a ward is rejected");
        h.assertTrue(CameraPreset.CODEC.parse(JsonOps.INSTANCE, StrictJsonParser.parse("{\"collision\":{\"mode\":\"denylist\"}}")).error().isPresent(),
                "Tagged policies require a tag");
        h.succeed();
    }

    private static ServerPlayer createPlayer(GameTestHelper h, String name) {
        var level = h.getLevel();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        h.addCleanup(ok -> level.getServer().getPlayerList().remove(player));
        Vec3 body = h.absoluteVec(new Vec3(1.5, 7, 1.5));
        player.snapTo(body, 23, 12);
        player.setChunkTrackingView(ChunkTrackingView.of(ChunkPos.containing(BlockPos.containing(body)), 5));
        player.setGameMode(GameType.SURVIVAL);
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        return player;
    }

    private static void sessions(GameTestHelper h) {
        var level = h.getLevel();
        var player = createPlayer(h, "CameraTester");
        Vec3 body = player.position();
        int[] actions = {0};
        h.assertTrue(!DetachedCamera.start(player, "missing_preset"), "Unknown preset fails cleanly");
        h.assertTrue(DetachedCamera.start(player, "spirit_scouting", (p, view) -> actions[0]++), "Scout opens");
        var scout = DetachedCamera.handle(player).orElseThrow();
        h.assertTrue(!DetachedCamera.start(player, "orbit"), "Two abilities cannot own the same view");
        var forward = new Input(true, false, false, false, false, false, true);
        h.assertTrue(!scout.setPose(body, 90, 15), "Free flight belongs to the player");
        var move = new CameraInput(scout.id(), forward, 90, 0, true, false, false, false);
        DetachedCamera.input(player, move);
        DetachedCamera.input(player, move);
        DetachedCamera.tick(new TickEvent.PlayerTickEvent.Post(player));
        Vec3 eye = player.getEyePosition();
        h.assertTrue(scout.view().orElseThrow().position().x < eye.x && actions[0] == 1, "Input moves the view and coalesces actions");
        h.assertTrue(player.position().equals(body) && player.getYRot() == 23 && player.getXRot() == 12
                && player.getCamera() == player && !player.isSpectator(), "Body, facing and game mode survive detached flight");
        for (int i = 0; i < 160; i++) {
            DetachedCamera.input(player, new CameraInput(scout.id(), forward, 90, -90, false, false, false, false));
            DetachedCamera.tick(new TickEvent.PlayerTickEvent.Post(player));
        }
        h.assertTrue(Math.abs(scout.view().orElseThrow().position().distanceTo(eye) - 48) < 1e-4, "Flight stops at its tether");
        player.hurtServer(level, player.damageSources().generic(), 2);
        h.assertTrue(!DetachedCamera.active(player), "Actual damage breaks scouting");

        Vec3 doorway = eye.add(0, 2, 0);
        h.assertTrue(DetachedCamera.at(player, "clairvoyance", doorway), "Fixed view opens at a point");
        var fixed = DetachedCamera.handle(player).orElseThrow();
        DetachedCamera.input(player, move);
        scout.stop();
        DetachedCamera.tick(new TickEvent.PlayerTickEvent.Post(player));
        h.assertTrue(fixed.view().orElseThrow().position().equals(doorway) && actions[0] == 1, "Stale handles and inputs leave the new view alone");
        for (int i = 0; i < 170; i++) DetachedCamera.tick(new TickEvent.PlayerTickEvent.Post(player));
        h.assertTrue(!DetachedCamera.active(player), "Timed view completes its smooth return");

        h.assertTrue(DetachedCamera.follow(player, "orbit", player), "Orbit accepts a moving entity anchor");
        var orbit = DetachedCamera.handle(player).orElseThrow();
        Vec3 offset = orbit.view().orElseThrow().position().subtract(eye);
        h.assertTrue(offset.length() > 0.5 && offset.length() <= 4.0001, "Orbit starts outside the body");
        player.setPos(body.add(0, 1, 0));
        DetachedCamera.tick(new TickEvent.PlayerTickEvent.Post(player));
        h.assertTrue(orbit.view().orElseThrow().position().distanceTo(player.getEyePosition().add(offset)) < 1e-4, "Orbit follows its moving anchor");
        h.succeed();
    }

    private static void starSteps(GameTestHelper h) {
        var player = createPlayer(h, "StarCamera");
        var tick = new TickEvent.PlayerTickEvent.Post(player);
        var body = player.position();
        var eye = player.getEyePosition();
        h.assertTrue(!KeyOfStars.openScoutingCamera(player), "Other pathways cannot open the star camera");
        SoulCore.setPathway(player, KeyOfStars.PATHWAY);
        h.assertTrue(KeyOfStars.openScoutingCamera(player), "A Key of Stars can open the scouting view");
        var camera = DetachedCamera.handle(player).orElseThrow();
        var held = new CameraInput(camera.id(), Input.EMPTY, 0, 0, false, false, false, true);
        var release = new CameraInput(camera.id(), Input.EMPTY, 0, 0, false, false, false, false);
        for (int i = 1; i <= 29; i++) {
            DetachedCamera.input(player, held);
            DetachedCamera.input(player, held);
            DetachedCamera.tick(tick);
            h.assertTrue(camera.view().orElseThrow().position().distanceTo(eye.add(0, 0, i / 10)) < 1e-5,
                    "Advance one block per ten server ticks regardless of packet count");
        }
        DetachedCamera.input(player, release);
        var lookUp = new CameraInput(camera.id(), Input.EMPTY, 0, -90, false, false, false, true);
        Vec3 before = camera.view().orElseThrow().position();
        for (int i = 0; i < 9; i++) {
            DetachedCamera.input(player, lookUp);
            DetachedCamera.tick(tick);
        }
        h.assertTrue(camera.view().orElseThrow().position().equals(before), "Release discards a partial interval");
        DetachedCamera.input(player, lookUp);
        DetachedCamera.tick(tick);
        Vec3 above = camera.view().orElseThrow().position();
        h.assertTrue(above.distanceTo(before.add(0, 1, 0)) < 1e-5, "Step follows the current camera pitch");
        BlockPos ward = BlockPos.containing(above.add(0, 1, 0));
        h.getLevel().setBlockAndUpdate(ward, Blocks.OBSIDIAN.defaultBlockState());
        for (int i = 0; i < 20; i++) {
            DetachedCamera.input(player, lookUp);
            DetachedCamera.tick(tick);
        }
        h.assertTrue(camera.view().orElseThrow().position().y < ward.getY(), "Forward steps respect wards");
        h.assertTrue(player.position().equals(body) && player.getYRot() == 23 && player.getXRot() == 12,
                "Camera jumps leave the body and its facing in place");
        camera.stop();
        h.assertTrue(DetachedCamera.start(player, "spirit_scouting"), "Ordinary scouting opens after a return");
        var ordinary = DetachedCamera.handle(player).orElseThrow();
        for (int i = 0; i < 20; i++) {
            DetachedCamera.input(player, new CameraInput(ordinary.id(), Input.EMPTY, 0, 0, false, false, false, true));
            DetachedCamera.tick(tick);
        }
        h.assertTrue(ordinary.view().orElseThrow().position().equals(eye), "Presets opt in to forward stepping");
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            held.encode(buf);
            var snap = CameraFrame.move(camera.id(), above, true);
            snap.encode(buf);
            h.assertTrue(held.equals(new CameraInput(buf)) && snap.equals(CameraFrame.decode(buf)) && !buf.isReadable(),
                    "Held input and instant camera position survive the wire");
        } finally {
            buf.release();
        }
        h.succeed();
    }

    private static void scripted(GameTestHelper h) {
        var player = createPlayer(h, "CameraDirector");
        var body = player.position();
        var eye = player.getEyePosition();
        var tick = new TickEvent.PlayerTickEvent.Post(player);
        var camera = DetachedCamera.startScripted(player).orElseThrow();
        var destination = eye.add(4, 2, 0);
        h.assertTrue(camera.moveTo(destination, -90, 30, 4), "Ability can queue a four-tick movement");
        var input = new CameraInput(camera.id(), new Input(true, false, false, false, true, false, true),
                160, -70, false, false, false, false);
        for (int step = 1; step <= 4; step++) {
            DetachedCamera.input(player, input);
            DetachedCamera.tick(tick);
            var view = camera.view().orElseThrow();
            double fraction = step / 4.0;
            h.assertTrue(view.position().distanceTo(eye.lerp(destination, fraction)) < 1e-6,
                    "Only the ability moves the camera");
            Vec3 expectedAim = Vec3.directionFromRotation((float)(12 + 18 * fraction), (float)(23 - 113 * fraction));
            h.assertTrue(view.direction().distanceTo(expectedAim) < 1e-5, "Only the ability turns the camera");
        }
        h.assertTrue(camera.setPose(destination, 170, -10), "Ability can turn in place");
        DetachedCamera.tick(tick);
        h.assertTrue(camera.view().orElseThrow().direction().distanceTo(Vec3.directionFromRotation(-10, 170)) < 1e-5,
                "Stationary look change applied");
        h.assertTrue(!camera.setPose(new Vec3(Double.NaN, 0, 0), 0, 0), "Invalid pose rejected");
        h.assertTrue(player.position().equals(body) && player.getYRot() == 23 && player.getXRot() == 12,
                "Camera travel leaves the real body and aim alone");

        Vec3 high = destination.add(0, 90, 0);
        h.assertTrue(camera.setPose(high, 0, 90), "Can request a distant point in a tracked chunk");
        DetachedCamera.tick(tick);
        h.assertTrue(camera.view().orElseThrow().position().equals(high), "Zero range allows untethered travel");
        camera.setPose(destination, 0, -90);
        DetachedCamera.tick(tick);
        BlockPos wall = BlockPos.containing(destination.add(0, 2, 0));
        h.getLevel().setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
        camera.setPose(destination.add(0, 4, 0), 0, -90);
        DetachedCamera.tick(tick);
        h.assertTrue(camera.view().orElseThrow().position().y < wall.getY(), "Scripted poses respect solid walls");
        camera.stop();
        h.assertTrue(!camera.setPose(eye, 0, 0), "Stops take priority over queued camera moves");
        for (int i = 0; i < 6; i++) DetachedCamera.tick(tick);
        var spirit = DetachedCamera.startScripted(player, "spirit_scouting").orElseThrow();
        h.assertTrue(!camera.setPose(eye, 0, 0), "An expired handle cannot move the next ability's camera");
        Vec3 beyond = destination.add(0, 4, 0);
        spirit.setPose(beyond, 0, 0);
        DetachedCamera.tick(tick);
        h.assertTrue(spirit.view().orElseThrow().position().distanceTo(beyond) < 1e-6,
                "An ability can drive the ward preset straight through stone");
        var frame = CameraFrame.pose(spirit.id(), beyond, -175, 28);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            frame.encode(buffer);
            h.assertTrue(frame.equals(CameraFrame.decode(buffer)) && !buffer.isReadable(), "Pose packet preserves position and aim");
        } finally {
            buffer.release();
        }
        h.succeed();
    }
}
