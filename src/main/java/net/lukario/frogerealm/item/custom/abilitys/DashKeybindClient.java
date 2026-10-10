package net.lukario.frogerealm.item.custom.abilitys;

import net.lukario.frogerealm.client.Keybindings;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = "circle_of_inevitability",
        value = Dist.CLIENT
)
public class DashKeybindClient {

    public static KeyMapping DASH_KEY;

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        DASH_KEY = new KeyMapping(
                "key.circle_of_inevitability.dash",
                GLFW.GLFW_KEY_Z,
                Keybindings.CATEGORY
        );
        event.register(DASH_KEY);
    }
}
