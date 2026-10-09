package net.lukario.frogerealm.network;

import net.lukario.frogerealm.client.ClientConcealment;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Server -> Client: stop drawing this entity for a while (or draw it again).
 * Don't use this directly — use Concealment.hide / Concealment.reveal.
 */
public class CConcealPacket {

    private final int entityId;
    private final int ticks; // how long it stays hidden; 0 = show it again now

    public CConcealPacket(int entityId, int ticks) {
        this.entityId = entityId;
        this.ticks = ticks;
    }

    public CConcealPacket(FriendlyByteBuf buffer) {
        this.entityId = buffer.readInt();
        this.ticks = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeInt(ticks);
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist.isClient()) {
            ClientConcealment.set(entityId, ticks);
        }
    }
}
