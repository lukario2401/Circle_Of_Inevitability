package net.lukario.frogerealm.network;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public class PacketHandler {

    private static final SimpleChannel INSTANCE = ChannelBuilder.named(
                    ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "main"))
            .serverAcceptedVersions((status, version) -> true)
            .clientAcceptedVersions((status, version) -> true)
            .networkProtocolVersion(1)
            .simpleChannel();

    public static void register() {
        INSTANCE.messageBuilder(SKeyPressSpawnEntityPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SKeyPressSpawnEntityPacket::encode)
                .decoder(SKeyPressSpawnEntityPacket::new)
                .consumerMainThread(SKeyPressSpawnEntityPacket::handle)
                .add();

        INSTANCE.messageBuilder(SKeyPressAbilityOneUsed.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SKeyPressAbilityOneUsed::encode)
                .decoder(SKeyPressAbilityOneUsed::new)
                .consumerMainThread(SKeyPressAbilityOneUsed::handle)
                .add();

        INSTANCE.messageBuilder(SKeyPressAbilityTwoUsed.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SKeyPressAbilityTwoUsed::encode)
                .decoder(SKeyPressAbilityTwoUsed::new)
                .consumerMainThread(SKeyPressAbilityTwoUsed::handle)
                .add();

        INSTANCE.messageBuilder(SKeyPressAbilityThreeUsed.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SKeyPressAbilityThreeUsed::encode)
                .decoder(SKeyPressAbilityThreeUsed::new)
                .consumerMainThread(SKeyPressAbilityThreeUsed::handle)
                .add();

        INSTANCE.messageBuilder(SKeyPressAbilityFourUsed.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SKeyPressAbilityFourUsed::encode)
                .decoder(SKeyPressAbilityFourUsed::new)
                .consumerMainThread(SKeyPressAbilityFourUsed::handle)
                .add();

        INSTANCE.messageBuilder(SKeyPressAbilityFiveUsed.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SKeyPressAbilityFiveUsed::encode)
                .decoder(SKeyPressAbilityFiveUsed::new)
                .consumerMainThread(SKeyPressAbilityFiveUsed::handle)
                .add();


        INSTANCE.messageBuilder(SKeyPressAbilitySixUsed.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SKeyPressAbilitySixUsed::encode)
                .decoder(SKeyPressAbilitySixUsed::new)
                .consumerMainThread(SKeyPressAbilitySixUsed::handle)
                .add();

        INSTANCE.messageBuilder(SKeyPressAbilitySevenUsed.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SKeyPressAbilitySevenUsed::encode)
                .decoder(SKeyPressAbilitySevenUsed::new)
                .consumerMainThread(SKeyPressAbilitySevenUsed::handle)
                .add();

        // Server -> Client
        INSTANCE.messageBuilder(CShowHudOverlayPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CShowHudOverlayPacket::encode)
                .decoder(CShowHudOverlayPacket::new)
                .consumerMainThread(CShowHudOverlayPacket::handle)
                .add();

        // Server -> Client: a small timer next to the hotbar (see combat/HudTimer)
        INSTANCE.messageBuilder(CHudTimerPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CHudTimerPacket::encode)
                .decoder(CHudTimerPacket::new)
                .consumerMainThread(CHudTimerPacket::handle)
                .add();

        INSTANCE.messageBuilder(CSyncAspectPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CSyncAspectPacket::encode)
                .decoder(CSyncAspectPacket::new)
                .consumerMainThread(CSyncAspectPacket::handle)
                .add();

        INSTANCE.messageBuilder(CScreenImagePacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CScreenImagePacket::encode)
                .decoder(CScreenImagePacket::new)
                .consumerMainThread(CScreenImagePacket::handle)
                .add();

        INSTANCE.messageBuilder(COpenAbilityMenuPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(COpenAbilityMenuPacket::encode)
                .decoder(COpenAbilityMenuPacket::new)
                .consumerMainThread(COpenAbilityMenuPacket::handle)
                .add();

        // Client -> Server
        INSTANCE.messageBuilder(SAbilityMenuChoicePacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SAbilityMenuChoicePacket::encode)
                .decoder(SAbilityMenuChoicePacket::new)
                .consumerMainThread(SAbilityMenuChoicePacket::handle)
                .add();

        INSTANCE.messageBuilder(COpenTextPromptPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(COpenTextPromptPacket::encode)
                .decoder(COpenTextPromptPacket::new)
                .consumerMainThread(COpenTextPromptPacket::handle)
                .add();

        INSTANCE.messageBuilder(STextPromptAnswerPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(STextPromptAnswerPacket::encode)
                .decoder(STextPromptAnswerPacket::new)
                .consumerMainThread(STextPromptAnswerPacket::handle)
                .add();

        // Server -> Client: who is hidden (see status/Concealment)
        INSTANCE.messageBuilder(CConcealPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CConcealPacket::encode)
                .decoder(CConcealPacket::new)
                .consumerMainThread(CConcealPacket::handle)
                .add();
    }

    public static void sendToServer(Object msg) {
        INSTANCE.send(msg, PacketDistributor.SERVER.noArg());
    }

    public static void sendToPlayer(Object msg, ServerPlayer player) {
        INSTANCE.send(msg, PacketDistributor.PLAYER.with(player));
    }

    public static void sendToAllClients(Object msg) {
        INSTANCE.send(msg, PacketDistributor.ALL.noArg());
    }
}
