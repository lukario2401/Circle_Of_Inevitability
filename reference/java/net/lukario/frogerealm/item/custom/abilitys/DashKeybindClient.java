package net.lukario.frogerealm.item.custom.abilitys;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(
        modid = "forgerealmmod",
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public class DashKeybindClient {

    public static KeyMapping DASH_KEY;

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        DASH_KEY = new KeyMapping(
                "key.forgerealmmod.dash",
                GLFW.GLFW_KEY_Z,
                "key.categories.forgerealmmod"
        );
        event.register(DASH_KEY);
    }
}
