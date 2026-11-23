package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.client.CorruptionTint;
import net.lukario.circleofinevitability.client.SpiritualityHud;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Updates the local player's spirituality bar and corruption tint. */
public record CSyncSoulStatsPacket(float spirituality, float maxSpirituality, int corruption) {

    public CSyncSoulStatsPacket(FriendlyByteBuf buffer) {
        this(buffer.readFloat(), buffer.readFloat(), buffer.readUnsignedByte());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(spirituality);
        buffer.writeFloat(maxSpirituality);
        buffer.writeByte(corruption);
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist.isClient()) {
            SpiritualityHud.update(spirituality, maxSpirituality);
            CorruptionTint.update(corruption);
        }
    }
}
