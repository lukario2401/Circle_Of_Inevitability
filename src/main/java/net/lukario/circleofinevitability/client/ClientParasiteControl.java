package net.lukario.circleofinevitability.client;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.network.PacketHandler;
import net.lukario.circleofinevitability.network.SParasiteControlPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client side of steering a host (status/ParasiteControl): while this player is inside a host they may steer,
 * their walk keys, jump, sprint, mouse and clicks are sent to the server every tick instead of doing what they
 * normally do, and the view turns with the mouse at once (the host's head follows a moment later).
 * With no input for a while the host acts on its own, and the view is its eyes again.
 * You shouldn't need to touch this: the server says when it starts and stops (CParasiteControlPacket).
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientParasiteControl {

    /** A turn of the mouse smaller than this (degrees per tick) is not input: a hand resting on it. */
    private static final float TURN = 0.05f;

    private static int hostId = -1;                 // the host that may be steered, or -1
    private static int idleTicks;
    private static long ticks;                      // client ticks while steering is possible
    private static long lastInput = Long.MIN_VALUE / 2;
    private static float lastYaw, lastPitch;

    private ClientParasiteControl() {}

    /** From CParasiteControlPacket: hostId = -1 means it is over. */
    public static void set(int id, int idle) {
        hostId = id;
        idleTicks = idle;
        lastInput = Long.MIN_VALUE / 2;             // it starts with the host acting on its own
        lastYaw = Float.NaN;                        // nothing to compare the first look with: not a turn
        lastPitch = Float.NaN;
    }

    /** The host being steered, while the view is in it (holding sneak takes it out), or null. */
    private static Entity host(Minecraft minecraft) {
        if (hostId < 0 || minecraft.player == null || minecraft.level == null) return null;
        Entity camera = minecraft.getCameraEntity();
        return camera != null && camera != minecraft.player && camera.getId() == hostId ? camera : null;
    }

    private static boolean steering() {
        return ticks - lastInput < idleTicks;
    }

    // Pre: before the game handles the clicks itself (a spectator's click would jump the view into what it hits)
    @SubscribeEvent
    public static void onClientParasiteControlTick(TickEvent.ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {             // left the world
            hostId = -1;
            return;
        }
        Entity host = host(minecraft);
        if (host == null || minecraft.isPaused()) return;
        ticks++;

        LocalPlayer player = minecraft.player;
        Options keys = minecraft.options;
        boolean strike = false, pick = false;
        while (keys.keyAttack.consumeClick()) strike = true;
        while (keys.keyUse.consumeClick()) pick = true;
        int forward = (keys.keyUp.isDown() ? 1 : 0) - (keys.keyDown.isDown() ? 1 : 0);
        int strafe = (keys.keyLeft.isDown() ? 1 : 0) - (keys.keyRight.isDown() ? 1 : 0);
        boolean jump = keys.keyJump.isDown();
        boolean sprint = keys.keySprint.isDown();
        boolean turned = Math.abs(Mth.wrapDegrees(player.getYRot() - lastYaw)) > TURN
                || Math.abs(player.getXRot() - lastPitch) > TURN;

        if (forward != 0 || strafe != 0 || jump || strike || pick || turned) lastInput = ticks;
        if (steering()) {
            PacketHandler.sendToServer(new SParasiteControlPacket(forward, strafe, jump, sprint, strike, pick,
                    player.getYRot(), player.getXRot()));
        } else {
            // the host's own will: the view is its eyes, and the mouse starts from there when it is moved again
            float yaw = host.getViewYRot(1f);
            float pitch = host.getViewXRot(1f);
            player.setYRot(yaw);
            player.setXRot(pitch);
            player.yRotO = yaw;
            player.xRotO = pitch;
        }
        lastYaw = player.getYRot();
        lastPitch = player.getXRot();
    }

    // While steering, the view turns with the mouse at once instead of waiting for the host's head
    @SubscribeEvent
    public static void onClientParasiteControlCamera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (host(minecraft) == null || !steering()) return;
        // the player's own turn (what the mouse moves), not getViewYRot: for a living thing that may be its head
        LocalPlayer player = minecraft.player;
        float partialTick = event.getPartialTick();
        event.setYaw(Mth.rotLerp(partialTick, player.yRotO, player.getYRot()));
        event.setPitch(Mth.lerp(partialTick, player.xRotO, player.getXRot()));
    }
}
