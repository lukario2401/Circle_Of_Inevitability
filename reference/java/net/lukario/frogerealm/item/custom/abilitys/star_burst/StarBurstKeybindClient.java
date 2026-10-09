package net.lukario.frogerealm.item.custom.abilitys.star_burst;

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
public class StarBurstKeybindClient {

    public static KeyMapping STAR_BURST_KEY;

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        STAR_BURST_KEY = new KeyMapping(
                "key.forgerealmmod.star_burst",
                GLFW.GLFW_KEY_C,
                "key.categories.forgerealmmod"
        );
        event.register(STAR_BURST_KEY);
    }
}
