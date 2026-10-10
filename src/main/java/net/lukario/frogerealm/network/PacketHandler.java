package net.lukario.frogerealm.network;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public class PacketHandler {

    private static final SimpleChannel INSTANCE = ChannelBuilder.named(
                    Identifier.fromNamespaceAndPath(ForgeRealm.MOD_ID, "main"))
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

        // TODO(stage5): INSTANCE.messageBuilder(SKeyPressAbilityOneUsed.class, NetworkDirection.PLAY_TO_SERVER)
                // TODO(stage5): .encoder(SKeyPressAbilityOneUsed::encode)
                // TODO(stage5): .decoder(SKeyPressAbilityOneUsed::new)
                // TODO(stage5): .consumerMainThread(SKeyPressAbilityOneUsed::handle)
                // TODO(stage5): .add();

        // TODO(stage5): INSTANCE.messageBuilder(SKeyPressAbilityTwoUsed.class, NetworkDirection.PLAY_TO_SERVER)
                // TODO(stage5): .encoder(SKeyPressAbilityTwoUsed::encode)
                // TODO(stage5): .decoder(SKeyPressAbilityTwoUsed::new)
                // TODO(stage5): .consumerMainThread(SKeyPressAbilityTwoUsed::handle)
                // TODO(stage5): .add();

        // TODO(stage5): INSTANCE.messageBuilder(SKeyPressAbilityThreeUsed.class, NetworkDirection.PLAY_TO_SERVER)
                // TODO(stage5): .encoder(SKeyPressAbilityThreeUsed::encode)
                // TODO(stage5): .decoder(SKeyPressAbilityThreeUsed::new)
                // TODO(stage5): .consumerMainThread(SKeyPressAbilityThreeUsed::handle)
                // TODO(stage5): .add();

        // TODO(stage5): INSTANCE.messageBuilder(SKeyPressAbilityFourUsed.class, NetworkDirection.PLAY_TO_SERVER)
                // TODO(stage5): .encoder(SKeyPressAbilityFourUsed::encode)
                // TODO(stage5): .decoder(SKeyPressAbilityFourUsed::new)
                // TODO(stage5): .consumerMainThread(SKeyPressAbilityFourUsed::handle)
                // TODO(stage5): .add();

        // TODO(stage5): INSTANCE.messageBuilder(SKeyPressAbilityFiveUsed.class, NetworkDirection.PLAY_TO_SERVER)
                // TODO(stage5): .encoder(SKeyPressAbilityFiveUsed::encode)
                // TODO(stage5): .decoder(SKeyPressAbilityFiveUsed::new)
                // TODO(stage5): .consumerMainThread(SKeyPressAbilityFiveUsed::handle)
                // TODO(stage5): .add();


        // TODO(stage5): INSTANCE.messageBuilder(SKeyPressAbilitySixUsed.class, NetworkDirection.PLAY_TO_SERVER)
                // TODO(stage5): .encoder(SKeyPressAbilitySixUsed::encode)
                // TODO(stage5): .decoder(SKeyPressAbilitySixUsed::new)
                // TODO(stage5): .consumerMainThread(SKeyPressAbilitySixUsed::handle)
                // TODO(stage5): .add();

        // TODO(stage5): INSTANCE.messageBuilder(SKeyPressAbilitySevenUsed.class, NetworkDirection.PLAY_TO_SERVER)
                // TODO(stage5): .encoder(SKeyPressAbilitySevenUsed::encode)
                // TODO(stage5): .decoder(SKeyPressAbilitySevenUsed::new)
                // TODO(stage5): .consumerMainThread(SKeyPressAbilitySevenUsed::handle)
                // TODO(stage5): .add();

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

        // TODO(stage4): INSTANCE.messageBuilder(CScreenImagePacket.class, NetworkDirection.PLAY_TO_CLIENT)
                // TODO(stage4): .encoder(CScreenImagePacket::encode)
                // TODO(stage4): .decoder(CScreenImagePacket::new)
                // TODO(stage4): .consumerMainThread(CScreenImagePacket::handle)
                // TODO(stage4): .add();

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

        // TODO(stage4): INSTANCE.messageBuilder(STextPromptAnswerPacket.class, NetworkDirection.PLAY_TO_SERVER)
                // TODO(stage4): .encoder(STextPromptAnswerPacket::encode)
                // TODO(stage4): .decoder(STextPromptAnswerPacket::new)
                // TODO(stage4): .consumerMainThread(STextPromptAnswerPacket::handle)
                // TODO(stage4): .add();

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
