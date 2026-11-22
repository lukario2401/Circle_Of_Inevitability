package net.lukario.circleofinevitability.client;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.camera.CameraFrame;
import net.lukario.circleofinevitability.camera.CameraInput;
import net.lukario.circleofinevitability.network.PacketHandler;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ClientDetachedCamera {
    // This marker exists only as a pose. It never enters the world or takes ownership of the player.
    private static Marker marker;
    private static LocalPlayer owner;
    private static UUID session;
    private static Vec3 target;
    private static int fov, returnTicks;
    private static CameraType originalPerspective;
    private static boolean originalHud, hideHud;

    private ClientDetachedCamera() {}

    public static Marker viewpoint() {
        if (marker == null) return null;
        Minecraft mc = Minecraft.getInstance();
        return mc.player == owner && owner.isAlive() && mc.level == marker.level() && mc.getCameraEntity() == owner ? marker : null;
    }

    public static void turn(LocalPlayer player, double mouseX, double mouseY) {
        Marker view = viewpoint();
        if (view == null) player.turn(mouseX, mouseY);
        else if (returnTicks == 0) view.turn(mouseX, mouseY);
    }

    public static void receive(CameraFrame frame) {
        Minecraft mc = Minecraft.getInstance();
        if (frame.kind() == CameraFrame.Kind.OPEN) {
            clear();
            if (mc.player == null || mc.level == null || mc.getCameraEntity() != mc.player) {
                PacketHandler.sendToServer(new CameraInput(frame.session(), Input.EMPTY, 0, 0, false, false, true));
                return;
            }
            session = frame.session();
            owner = mc.player;
            owner.setSprinting(false);
            Vec3 motion = owner.getDeltaMovement();
            owner.setDeltaMovement(0, motion.y, 0);
            target = frame.position();
            marker = new Marker(EntityTypes.MARKER, mc.level);
            marker.snapTo(target, frame.yaw(), frame.pitch());
            fov = frame.fov();
            originalPerspective = mc.options.getCameraType();
            originalHud = mc.gui.hud.isHidden();
            hideHud = frame.hideHud();
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            if (hideHud && !originalHud) mc.gui.hud.toggle();
            if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
        } else if (frame.session().equals(session)) {
            switch (frame.kind()) {
                case CLOSE -> clear();
                case RETURN -> returnTicks = frame.returnTicks();
                case MOVE -> target = frame.position();
            }
        }
    }

    private static void clear() {
        if (marker == null) return;
        Minecraft mc = Minecraft.getInstance();
        mc.options.setCameraType(originalPerspective);
        if (hideHud && originalHud != mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        returnTicks = 0;
        marker = null;
        owner = null;
        session = null;
    }

    @SubscribeEvent(priority = Priority.HIGHEST)
    public static void tick(TickEvent.ClientTickEvent.Pre event) {
        if (marker == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (viewpoint() == null) {
            if (mc.getConnection() != null)
                PacketHandler.sendToServer(new CameraInput(session, Input.EMPTY, 0, 0, false, false, true));
            clear();
            return;
        }
        marker.setOldPosAndRot();
        marker.setPos(target);
        if (returnTicks > 0) {
            float fraction = 1f / returnTicks--;
            marker.setYRot(Mth.rotLerp(fraction, marker.getYRot(), owner.getYRot()));
            marker.setXRot(Mth.lerp(fraction, marker.getXRot(), owner.getXRot()));
        }
        var keys = mc.options;
        boolean gameplay = mc.isWindowActive() && mc.gui.screen() == null && mc.gui.overlay() == null;
        Input input = gameplay ? new Input(keys.keyUp.isDown(), keys.keyDown.isDown(), keys.keyLeft.isDown(),
                keys.keyRight.isDown(), keys.keyJump.isDown(), keys.keyShift.isDown(), keys.keySprint.isDown()) : Input.EMPTY;
        boolean use = consume(keys.keyUse);
        boolean exit = consume(Keybindings.INSTANCE.exitDetachedCamera);
        consume(keys.keyAttack);
        consume(keys.keyPickItem);
        consume(keys.keyDrop);
        consume(keys.keySwapOffhand);
        consume(keys.keyTogglePerspective);
        if (hideHud && !mc.gui.hud.isHidden()) mc.gui.hud.toggle();
        PacketHandler.sendToServer(new CameraInput(session, input, marker.getYRot(), marker.getXRot(),
                gameplay && use, gameplay && exit, false));
    }

    private static boolean consume(KeyMapping key) {
        boolean clicked = false;
        while (key.consumeClick()) clicked = true;
        return clicked;
    }

    @SubscribeEvent
    public static void movement(MovementInputUpdateEvent event) {
        if (viewpoint() == null) return;
        event.getInput().keyPresses = Input.EMPTY;
        event.getInput().moveVector = Vec2.ZERO;
    }

    @SubscribeEvent(priority = Priority.HIGHEST)
    public static boolean interact(InputEvent.InteractionKeyMappingTriggered event) {
        if (viewpoint() == null) return false;
        event.setSwingHand(false);
        return true;
    }

    @SubscribeEvent
    public static boolean hideHand(RenderHandEvent event) { return viewpoint() != null; }

    @SubscribeEvent
    public static void fieldOfView(ViewportEvent.ComputeFov event) {
        if (viewpoint() != null && fov > 0 && event.usedConfiguredFov()) event.setFOV(fov);
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
}
