package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.client.ClientParasiteControl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Server -> Client: you can steer this host now (hostId), or no longer (hostId = -1). See status/ParasiteControl.
 * Don't send this directly: Parasite does it when a parasitism that steers starts and ends.
 */
public class CParasiteControlPacket {

    private final int hostId;
    private final int idleTicks;    // after this long without input the host acts on its own again

    public CParasiteControlPacket(int hostId, int idleTicks) {
        this.hostId = hostId;
        this.idleTicks = idleTicks;
    }

    public CParasiteControlPacket(FriendlyByteBuf buffer) {
        this.hostId = buffer.readInt();
        this.idleTicks = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(hostId);
        buffer.writeInt(idleTicks);
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist.isClient()) {
            ClientParasiteControl.set(hostId, idleTicks);
        }
    }
}
