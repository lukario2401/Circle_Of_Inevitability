package net.lukario.frogerealm.network;

import net.lukario.frogerealm.client.TextPromptScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Server -> Client: open a text prompt window. Don't use directly — use AbilityTextPrompt.open(player).
 */
public class COpenTextPromptPacket {

    private final String promptId;
    private final String title;
    private final String hint;

    public COpenTextPromptPacket(String promptId, String title, String hint) {
        this.promptId = promptId;
        this.title = title;
        this.hint = hint;
    }

    public COpenTextPromptPacket(FriendlyByteBuf buffer) {
        this.promptId = buffer.readUtf();
        this.title = buffer.readUtf();
        this.hint = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(promptId);
        buffer.writeUtf(title);
        buffer.writeUtf(hint);
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist.isClient()) {
            TextPromptScreen.open(promptId, title, hint);
        }
    }
}
