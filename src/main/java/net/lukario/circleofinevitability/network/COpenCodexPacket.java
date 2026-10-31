package net.lukario.circleofinevitability.network;
import java.util.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.lukario.circleofinevitability.client.CodexScreen;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye.Codex.Row;

public record COpenCodexPacket(List<Row> rows) {
    public COpenCodexPacket(FriendlyByteBuf b) { this(read(b)); }
    private static List<Row> read(FriendlyByteBuf b) {
        int count=b.readVarInt();
        if (count<0 || count>4096) throw new IllegalArgumentException("Codex size");
        List<Row> rows=new ArrayList<>();
        for(int i=0;i<count;i++) rows.add(new Row(b.readUtf(),b.readUtf(),b.readUtf(),b.readVarInt(),b.readDouble(),b.readBoolean(),b.readBoolean()));
        return List.copyOf(rows);
    }
    public void encode(FriendlyByteBuf b) {
        b.writeVarInt(rows.size());
        for(Row r:rows) { b.writeUtf(r.id());b.writeUtf(r.name());b.writeUtf(r.icon());b.writeVarInt(r.sequence());b.writeDouble(r.study());b.writeBoolean(r.favorite());b.writeBoolean(r.selected()); }
    }
    public void handle(CustomPayloadEvent.Context ctx) {
        if(FMLEnvironment.dist.isClient()) CodexScreen.open(rows);
    }
}
