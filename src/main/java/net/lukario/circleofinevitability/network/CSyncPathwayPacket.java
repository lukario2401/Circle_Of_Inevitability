package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.client.ClientPathwayData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Server -> Client: tells the client its player's Pathway, Sequence and digestion (whole percent).
 * The pathway only lives in server-side persistent data, so the client needs this to know it.
 * Sent automatically by PathwaySync whenever one of them changes.
 */
public class CSyncPathwayPacket {

    private final String pathway;
    private final int sequence;
    private final int digestion; // whole percent

    public CSyncPathwayPacket(String pathway, int sequence, int digestion) {
        this.pathway = pathway;
        this.sequence = sequence;
        this.digestion = digestion;
    }

    public CSyncPathwayPacket(FriendlyByteBuf buffer) {
        this.pathway = buffer.readUtf();
        this.sequence = buffer.readVarInt();
        this.digestion = buffer.readVarInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(pathway);
        buffer.writeVarInt(sequence);
        buffer.writeVarInt(digestion);
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist.isClient()) {
            ClientPathwayData.setPathway(pathway);
            ClientPathwayData.setProgress(sequence, digestion);
        }
    }
}
