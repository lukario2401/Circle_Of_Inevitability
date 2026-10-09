package net.lukario.frogerealm.handler;


import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.client.Keybindings;
import net.lukario.frogerealm.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeHandler {
    private static final Component EXAMPLE_KEY_PRESSED =
            Component.translatable("message." + ForgeRealm.MOD_ID + ".example_key_pressed");

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if(Keybindings.INSTANCE.exampleKey.consumeClick() && minecraft.player != null) {
            minecraft.player.displayClientMessage(EXAMPLE_KEY_PRESSED, false);
        }

        if(Keybindings.INSTANCE.examplePacketKey.consumeClick() && minecraft.player != null) {
            minecraft.player.displayClientMessage(EXAMPLE_KEY_PRESSED, true);
            PacketHandler.sendToServer(new SKeyPressSpawnEntityPacket());
        }
        if(Keybindings.INSTANCE.abilityOnePacketKey.consumeClick() && minecraft.player != null) {
//            minecraft.player.displayClientMessage(EXAMPLE_KEY_PRESSED, true);
            PacketHandler.sendToServer(new SKeyPressAbilityOneUsed());
        }
        if(Keybindings.INSTANCE.abilityTwoPacketKey.consumeClick() && minecraft.player != null) {
//            minecraft.player.displayClientMessage(EXAMPLE_KEY_PRESSED, true);
            PacketHandler.sendToServer(new SKeyPressAbilityTwoUsed());
        }
        if(Keybindings.INSTANCE.abilityThreePacketKey.consumeClick() && minecraft.player != null) {
//            minecraft.player.displayClientMessage(EXAMPLE_KEY_PRESSED, true);
            PacketHandler.sendToServer(new SKeyPressAbilityThreeUsed());
        }
        if(Keybindings.INSTANCE.abilityFourPacketKey.consumeClick() && minecraft.player != null) {
//            minecraft.player.displayClientMessage(EXAMPLE_KEY_PRESSED, true);
            PacketHandler.sendToServer(new SKeyPressAbilityFourUsed());
        }
        if(Keybindings.INSTANCE.abilityFivePacketKey.consumeClick() && minecraft.player != null) {
//            minecraft.player.displayClientMessage(EXAMPLE_KEY_PRESSED, true);
            PacketHandler.sendToServer(new SKeyPressAbilityFiveUsed());
        }
        if(Keybindings.INSTANCE.abilitySixPacketKey.consumeClick() && minecraft.player != null) {
//            minecraft.player.displayClientMessage(EXAMPLE_KEY_PRESSED, true);
            PacketHandler.sendToServer(new SKeyPressAbilitySixUsed());
        }
        if(Keybindings.INSTANCE.abilitySevenPacketKey.consumeClick() && minecraft.player != null) {
//            minecraft.player.displayClientMessage(EXAMPLE_KEY_PRESSED, true);
            PacketHandler.sendToServer(new SKeyPressAbilitySevenUsed());
        }
    }
}