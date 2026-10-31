package net.lukario.circleofinevitability.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye.Codex;
public record SCodexChoicePacket(String id,boolean favorite) {
    public SCodexChoicePacket(FriendlyByteBuf b) { this(b.readUtf(512),b.readBoolean()); }
    public void encode(FriendlyByteBuf b) { b.writeUtf(id,512);b.writeBoolean(favorite); }
    public void handle(CustomPayloadEvent.Context ctx) {
        if(ctx.getSender()!=null) Codex.choice(ctx.getSender(),id,favorite);
    }
}
