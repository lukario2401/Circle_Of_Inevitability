package net.lukario.circleofinevitability.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStars;
import net.lukario.circleofinevitability.network.PacketHandler;
import net.lukario.circleofinevitability.network.SOpenStarCameraPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

/** Resolve overlapping physical presses before any of the mod's standalone bindings can dispatch them. */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class CameraChordInput {
    private static final Set<InputConstants.Key> held = new HashSet<>();
    private static final Set<InputConstants.Key> claimed = new HashSet<>();
    // Forge may release a keyboard modifier before the next poll. Remember which queued clicks we intercepted.
    private static final Set<InputConstants.Key> intercepted = new HashSet<>();
    private static final Set<InputConstants.Key> soloReleases = new HashSet<>();
    private static LocalPlayer owner;
    private static ClientLevel level;
    private static boolean listening;

    private CameraChordInput() {}

    public static boolean updateContext(Minecraft mc) {
        boolean gameplay = mc.player != null && mc.player.isAlive() && mc.isWindowActive()
                && mc.gui.screen() == null && mc.gui.overlay() == null;
        boolean enabled = gameplay && (ClientPathwayData.is(KeyOfStars.PATHWAY) || ClientDetachedCamera.viewpoint() != null);
        if (owner != mc.player || level != mc.level || listening != enabled) {
            held.clear();
            claimed.clear();
            intercepted.clear();
            soloReleases.clear();
        }
        owner = mc.player;
        level = mc.level;
        listening = enabled;
        return gameplay;
    }

    @SubscribeEvent
    public static void keyboard(InputEvent.Key event) {
        input(InputConstants.getKey(event.getInfo()), event.getAction());
    }

    @SubscribeEvent
    public static void mouse(InputEvent.MouseButton.Post event) {
        input(InputConstants.Type.MOUSE.getOrCreate(event.getButton()), event.getAction());
    }

    private static boolean shared(InputConstants.Key key) {
        var keys = Keybindings.INSTANCE;
        return listening && (keys.cameraModifier.isActiveAndMatches(key) || keys.cameraOpen.isActiveAndMatches(key)
                || keys.cameraStep.isActiveAndMatches(key));
    }

    private static void input(InputConstants.Key key, int action) {
        updateContext(Minecraft.getInstance());
        if (action == GLFW.GLFW_RELEASE) {
            boolean used = claimed.remove(key);
            if (held.remove(key)) {
                intercepted.add(key);
                if (!used && ClientDetachedCamera.viewpoint() == null) soloReleases.add(key);
            }
            return;
        }
        if (action != GLFW.GLFW_PRESS || !shared(key) || !held.add(key)) return;

        intercepted.add(key);
        boolean detached = ClientDetachedCamera.viewpoint() != null;
        if (detached) claimed.add(key);
        var keys = Keybindings.INSTANCE;
        if (complete(key, keys.cameraOpen) && !detached && ClientPathwayData.is(KeyOfStars.PATHWAY)) {
            PacketHandler.sendToServer(new SOpenStarCameraPacket());
        }
        complete(key, keys.cameraStep);
    }

    private static boolean complete(InputConstants.Key pressed, KeyMapping action) {
        var modifier = Keybindings.INSTANCE.cameraModifier;
        if ((!modifier.matches(pressed) && !action.matches(pressed)) || modifier.getKey().equals(action.getKey())
                || !holding(modifier) || !holding(action)) return false;
        claimed.add(modifier.getKey());
        claimed.add(action.getKey());
        return true;
    }

    private static boolean holding(KeyMapping mapping) {
        return mapping.isActive() && held.contains(mapping.getKey());
    }

    public static boolean stepHeld() {
        var keys = Keybindings.INSTANCE;
        return holding(keys.cameraModifier) && holding(keys.cameraStep)
                && !keys.cameraModifier.getKey().equals(keys.cameraStep.getKey());
    }

    private static boolean reserved(KeyMapping mapping) {
        return held.contains(mapping.getKey()) || intercepted.contains(mapping.getKey()) || shared(mapping.getKey());
    }

    public static boolean click(KeyMapping mapping) {
        if (!reserved(mapping)) return mapping.consumeClick();
        // Each mapping has its own native click queue, including mappings rebound to the same physical key.
        while (mapping.consumeClick()) {
            // Drop the native press and any OS repeats, even after the chord's first key is released.
        }
        return mapping.isActive() && soloReleases.contains(mapping.getKey());
    }

    public static boolean down(KeyMapping mapping) {
        return !reserved(mapping) && mapping.isDown();
    }

    public static void finishTick() {
        intercepted.clear();
        soloReleases.clear();
    }
}
