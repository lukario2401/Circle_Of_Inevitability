package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.client.AbilityListScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.ArrayList;
import java.util.List;

/** Server -> Client: open a scrollable pick list. Don't use directly: use menu/AbilityList.open(player). */
public record COpenAbilityListPacket(String listId, String title, String hint, String confirmLabel, int maxPicks,
                                     List<Row> rows) {

    /** icon: a texture id, "" for none. */
    public record Row(String icon, String name, String detail, String description, boolean locked, boolean marked) {}

    public COpenAbilityListPacket {
        rows = List.copyOf(rows);
    }

    public COpenAbilityListPacket(FriendlyByteBuf buffer) {
        this(buffer.readUtf(), buffer.readUtf(), buffer.readUtf(), buffer.readUtf(), buffer.readVarInt(), readRows(buffer));
    }

    private static List<Row> readRows(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > 4096) throw new IllegalArgumentException("Ability list size");
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < count; i++)
            rows.add(new Row(buffer.readUtf(), buffer.readUtf(), buffer.readUtf(), buffer.readUtf(), buffer.readBoolean(),
                    buffer.readBoolean()));
        return rows;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(listId);
        buffer.writeUtf(title);
        buffer.writeUtf(hint);
        buffer.writeUtf(confirmLabel);
        buffer.writeVarInt(maxPicks);
        buffer.writeVarInt(rows.size());
        for (Row row : rows) {
            buffer.writeUtf(row.icon());
            buffer.writeUtf(row.name());
            buffer.writeUtf(row.detail());
            buffer.writeUtf(row.description());
            buffer.writeBoolean(row.locked());
            buffer.writeBoolean(row.marked());
        }
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist.isClient()) AbilityListScreen.open(this);
    }
}
