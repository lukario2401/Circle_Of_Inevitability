package net.lukario.frogerealm.network;

import net.lukario.frogerealm.client.AbilityHudOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Server -> Client: show a big number in the middle of the screen (countdowns).
 * Usage: PacketHandler.sendToPlayer(CShowHudOverlayPacket.number(3, 0xFFFFFF, 20), serverPlayer);
 * For pictures use ScreenImages instead.
 */
public class CShowHudOverlayPacket {

    private final int number;
    private final int color;
    private final int durationTicks;

    private CShowHudOverlayPacket(int number, int color, int durationTicks) {
        this.number = number;
        this.color = color;
        this.durationTicks = durationTicks;
    }

    /** Big number in the middle of the screen. color is 0xRRGGBB. */
    public static CShowHudOverlayPacket number(int number, int color, int durationTicks) {
        return new CShowHudOverlayPacket(number, color, durationTicks);
    }

    public CShowHudOverlayPacket(FriendlyByteBuf buffer) {
        this.number = buffer.readInt();
        this.color = buffer.readInt();
        this.durationTicks = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeInt(number);
        buffer.writeInt(color);
        buffer.writeInt(durationTicks);
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist.isClient()) {
            AbilityHudOverlay.showNumber(number, color, durationTicks);
        }
    }
}
