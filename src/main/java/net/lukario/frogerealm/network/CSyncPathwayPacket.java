package net.lukario.frogerealm.network;

import net.lukario.frogerealm.client.ClientPathwayData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Server -> Client: tells the client which pathway its player currently has.
 * The pathway only lives in server-side persistent data, so the client needs this to know it.
 * Sent automatically by PathwaySync whenever the pathway changes.
 */
public class CSyncPathwayPacket {

    private final String pathway;

    public CSyncPathwayPacket(String pathway) {
        this.pathway = pathway;
    }

    public CSyncPathwayPacket(FriendlyByteBuf buffer) {
        this.pathway = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(pathway);
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist.isClient()) {
            ClientPathwayData.setPathway(pathway);
        }
    }
}
