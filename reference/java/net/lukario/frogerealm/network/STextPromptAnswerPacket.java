package net.lukario.frogerealm.network;

import net.lukario.frogerealm.menu.AbilityTextPrompt;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client -> Server: what the player typed into an AbilityTextPrompt ("" = closed without answering).
 */
public class STextPromptAnswerPacket {

    private final String promptId;
    private final String text;

    public STextPromptAnswerPacket(String promptId, String text) {
        this.promptId = promptId;
        this.text = text;
    }

    public STextPromptAnswerPacket(FriendlyByteBuf buffer) {
        this.promptId = buffer.readUtf(64);
        this.text = buffer.readUtf(AbilityTextPrompt.MAX_LENGTH * 4);
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(promptId, 64);
        buffer.writeUtf(text, AbilityTextPrompt.MAX_LENGTH * 4);
    }

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        AbilityTextPrompt.handleAnswer(player, promptId, text);
    }
}
