package net.lukario.circleofinevitability.client;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** The Monocle's Gaze key (default B), polled in ClientForgeHandler. */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability", value = Dist.CLIENT)
public final class MonocleKeybindClient {
    public static KeyMapping GAZE_KEY;

    private MonocleKeybindClient() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        GAZE_KEY = new KeyMapping("key.circle_of_inevitability.monocle_gaze", GLFW.GLFW_KEY_B, Keybindings.CATEGORY);
        event.register(GAZE_KEY);
    }
}
