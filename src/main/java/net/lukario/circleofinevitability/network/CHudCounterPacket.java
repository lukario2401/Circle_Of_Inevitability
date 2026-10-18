package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.client.HudCounterOverlay;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Server -> Client: show, update (or take away) a small counter next to the hotbar.
 * Don't send it yourself: use combat/HudCounter.
 */
public class CHudCounterPacket {

    private final String name;
    private final String image;
    private final String label;
    private final int value;
    private final int max;
    private final int color;
    private final boolean shown;    // false = take it away

    public CHudCounterPacket(String name, String image, String label, int value, int max, int color, boolean shown) {
        this.name = name;
        this.image = image;
        this.label = label;
        this.value = value;
        this.max = max;
        this.color = color;
        this.shown = shown;
    }

    public CHudCounterPacket(FriendlyByteBuf buffer) {
        this.name = buffer.readUtf();
        this.image = buffer.readUtf();
        this.label = buffer.readUtf();
        this.value = buffer.readInt();
        this.max = buffer.readInt();
        this.color = buffer.readInt();
        this.shown = buffer.readBoolean();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(name);
        buffer.writeUtf(image);
        buffer.writeUtf(label);
        buffer.writeInt(value);
        buffer.writeInt(max);
        buffer.writeInt(color);
        buffer.writeBoolean(shown);
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist.isClient()) {
            if (shown) HudCounterOverlay.show(name, image, label, value, max, color);
            else HudCounterOverlay.hide(name);
        }
    }
}
