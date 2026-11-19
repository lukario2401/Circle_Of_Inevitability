package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.client.SpiritualityHud;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Updates the local player's mana bar with the server's current and maximum spirituality. */
public record CSyncSpiritualityPacket(float current, float maximum) {

    public CSyncSpiritualityPacket(FriendlyByteBuf buffer) {
        this(buffer.readFloat(), buffer.readFloat());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(current);
        buffer.writeFloat(maximum);
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist.isClient()) {
            SpiritualityHud.update(current, maximum);
        }
    }
}
