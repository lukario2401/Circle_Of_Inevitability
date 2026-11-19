package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.client.AbilityGuideScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

public record COpenGuidePacket(String pathway, int sequence) {
    public COpenGuidePacket(FriendlyByteBuf buf) {
        this(buf.readUtf(), buf.readVarInt());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(pathway);
        buf.writeVarInt(sequence);
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist.isClient()) AbilityGuideScreen.open(pathway, sequence);
    }
}
