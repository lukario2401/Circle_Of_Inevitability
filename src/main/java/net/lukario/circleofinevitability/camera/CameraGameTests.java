package net.lukario.circleofinevitability.camera;

import com.mojang.authlib.GameProfile;
import com.mojang.serialization.JsonOps;
import io.netty.channel.embedded.EmbeddedChannel;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
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

    private static void sessions(GameTestHelper h) {
        var level = h.getLevel();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "CameraTester"), false);
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
        int[] actions = {0};
        h.assertTrue(!DetachedCamera.start(player, "missing_preset"), "Unknown preset fails cleanly");
        h.assertTrue(DetachedCamera.start(player, "spirit_scouting", (p, view) -> actions[0]++), "Scout opens");
        var scout = DetachedCamera.handle(player).orElseThrow();
        h.assertTrue(!DetachedCamera.start(player, "orbit"), "Two abilities cannot own the same view");
        var forward = new Input(true, false, false, false, false, false, true);
        var move = new CameraInput(scout.id(), forward, 90, 0, true, false, false);
        DetachedCamera.input(player, move);
        DetachedCamera.input(player, move);
        DetachedCamera.tick(new TickEvent.PlayerTickEvent.Post(player));
        Vec3 eye = player.getEyePosition();
        h.assertTrue(scout.view().orElseThrow().position().x < eye.x && actions[0] == 1, "Input moves the view and coalesces actions");
        h.assertTrue(player.position().equals(body) && player.getYRot() == 23 && player.getXRot() == 12
                && player.getCamera() == player && !player.isSpectator(), "Body, facing and game mode survive detached flight");
        for (int i = 0; i < 160; i++) {
            DetachedCamera.input(player, new CameraInput(scout.id(), forward, 90, -90, false, false, false));
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
}
