package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.camera.CameraFrame;
import net.lukario.circleofinevitability.camera.CameraInput;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;

public class PacketHandler {

    private static final SimpleChannel INSTANCE = ChannelBuilder.named(
                    Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "main"))
            .networkProtocolVersion(6)
            .simpleChannel();

    public static void register() {
        INSTANCE.messageBuilder(SKeyPressSpawnEntityPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SKeyPressSpawnEntityPacket::encode)
                .decoder(SKeyPressSpawnEntityPacket::new)
                .consumerMainThread(SKeyPressSpawnEntityPacket::handle)
                .add();

        INSTANCE.messageBuilder(SAbilityKeyPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SAbilityKeyPacket::encode)
                .decoder(SAbilityKeyPacket::new)
                .consumerMainThread(SAbilityKeyPacket::handle)
                .add();

        INSTANCE.messageBuilder(CWhiteAngelLabelPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CWhiteAngelLabelPacket::encode).decoder(CWhiteAngelLabelPacket::new)
                .consumerMainThread(CWhiteAngelLabelPacket::handle).add();
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

        INSTANCE.messageBuilder(CSyncPathwayPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CSyncPathwayPacket::encode)
                .decoder(CSyncPathwayPacket::new)
                .consumerMainThread(CSyncPathwayPacket::handle)
                .add();

        // Server -> Client: another player's pathway, for their swing animations (see client/animation/SwingRules)
        INSTANCE.messageBuilder(CPlayerPathwayPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CPlayerPathwayPacket::encode)
                .decoder(CPlayerPathwayPacket::new)
                .consumerMainThread(CPlayerPathwayPacket::handle)
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

        // Steering a host from inside it (see status/ParasiteControl)
        INSTANCE.messageBuilder(CParasiteControlPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CParasiteControlPacket::encode)
                .decoder(CParasiteControlPacket::new)
                .consumerMainThread(CParasiteControlPacket::handle)
                .add();

        INSTANCE.messageBuilder(SParasiteControlPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SParasiteControlPacket::encode)
                .decoder(SParasiteControlPacket::new)
                .consumerMainThread(SParasiteControlPacket::handle)
                .add();

        // Server -> Client: who is hidden (see status/Concealment)
        INSTANCE.messageBuilder(CConcealPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CConcealPacket::encode)
                .decoder(CConcealPacket::new)
                .consumerMainThread(CConcealPacket::handle)
                .add();

        // Server -> Client: a small counter next to the hotbar (see combat/HudCounter). Kept last so the ids of the
        // packets above stay as they were.
        INSTANCE.messageBuilder(CHudCounterPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CHudCounterPacket::encode)
                .decoder(CHudCounterPacket::new)
                .consumerMainThread(CHudCounterPacket::handle)
                .add();
        INSTANCE.messageBuilder(COpenCodexPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(COpenCodexPacket::encode).decoder(COpenCodexPacket::new)
                .consumerMainThread(COpenCodexPacket::handle).add();
        INSTANCE.messageBuilder(SCodexChoicePacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SCodexChoicePacket::encode).decoder(SCodexChoicePacket::new)
                .consumerMainThread(SCodexChoicePacket::handle).add();
        INSTANCE.messageBuilder(CSyncSpiritualityPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CSyncSpiritualityPacket::encode)
                .decoder(CSyncSpiritualityPacket::new)
                .consumerMainThread(CSyncSpiritualityPacket::handle)
                .add();
        INSTANCE.messageBuilder(COpenGuidePacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(COpenGuidePacket::encode).decoder(COpenGuidePacket::new)
                .consumerMainThread(COpenGuidePacket::handle).add();
        INSTANCE.messageBuilder(CameraFrame.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(CameraFrame::encode).decoder(CameraFrame::decode)
                .consumerMainThread(CameraFrame::handle).add();
        INSTANCE.messageBuilder(CameraInput.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CameraInput::encode).decoder(CameraInput::new)
                .consumerMainThread(CameraInput::handle).add();
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
