package net.lukario.circleofinevitability.item.custom.abilitys.star_burst;

import net.lukario.circleofinevitability.client.Keybindings;
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
public class StarBurstKeybindClient {

    public static KeyMapping STAR_BURST_KEY;

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        STAR_BURST_KEY = new KeyMapping(
                "key.circle_of_inevitability.star_burst",
                GLFW.GLFW_KEY_C,
                Keybindings.CATEGORY
        );
        event.register(STAR_BURST_KEY);
    }
}
