package net.lukario.frogerealm.network;

import net.lukario.frogerealm.client.ClientAspectData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Server -> Client: tells the client which aspect its player currently has.
 * The aspect only lives in server-side persistent data, so the client needs this to know it.
 * Sent automatically by AspectSync whenever the aspect changes.
 */
public class CSyncAspectPacket {

    private final String aspect;

    public CSyncAspectPacket(String aspect) {
        this.aspect = aspect;
    }

    public CSyncAspectPacket(FriendlyByteBuf buffer) {
        this.aspect = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(aspect);
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist.isClient()) {
            ClientAspectData.setAspect(aspect);
        }
    }
}
