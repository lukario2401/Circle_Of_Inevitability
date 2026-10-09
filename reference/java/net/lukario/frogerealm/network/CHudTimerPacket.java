package net.lukario.frogerealm.network;

import net.lukario.frogerealm.client.HudTimerOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Server -> Client: show (or take away) a small timer next to the hotbar.
 * Don't send it yourself: use combat/HudTimer.
 */
public class CHudTimerPacket {

    private final String name;
    private final String label;
    private final int ticks;        // 0 = take it away
    private final int color;

    public CHudTimerPacket(String name, String label, int ticks, int color) {
        this.name = name;
        this.label = label;
        this.ticks = ticks;
        this.color = color;
    }

    public CHudTimerPacket(FriendlyByteBuf buffer) {
        this.name = buffer.readUtf();
        this.label = buffer.readUtf();
        this.ticks = buffer.readInt();
        this.color = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(name);
        buffer.writeUtf(label);
        buffer.writeInt(ticks);
        buffer.writeInt(color);
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist.isClient()) {
            HudTimerOverlay.show(name, label, ticks, color);
        }
    }
}
