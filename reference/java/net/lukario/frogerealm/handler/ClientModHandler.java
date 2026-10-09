package net.lukario.frogerealm.handler;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.client.Keybindings;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModHandler {

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(Keybindings.INSTANCE.exampleKey);
        event.register(Keybindings.INSTANCE.examplePacketKey);
        event.register(Keybindings.INSTANCE.abilityOnePacketKey);
        event.register(Keybindings.INSTANCE.abilityTwoPacketKey);
        event.register(Keybindings.INSTANCE.abilityThreePacketKey);
        event.register(Keybindings.INSTANCE.abilityFourPacketKey);
        event.register(Keybindings.INSTANCE.abilityFivePacketKey);
        event.register(Keybindings.INSTANCE.abilitySixPacketKey);
        event.register(Keybindings.INSTANCE.abilitySevenPacketKey);
    }

}