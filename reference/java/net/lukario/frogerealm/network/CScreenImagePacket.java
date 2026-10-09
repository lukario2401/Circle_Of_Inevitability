package net.lukario.frogerealm.network;

import net.lukario.frogerealm.client.ScreenImageRenderer;
import net.lukario.frogerealm.screen.ScreenAnchor;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Server -> Client: show or hide a picture on screen.
 * Don't use this directly — use ScreenImages.show / ScreenImages.hide.
 */
public class CScreenImagePacket {

    private final boolean show; // false = hide
    private final String id;
    private final ResourceLocation texture;
    private final ScreenAnchor anchor;
    private final int x, y, width, height;
    private final int ticks; // show: duration (-1 = forever), hide: delay

    private CScreenImagePacket(boolean show, String id, ResourceLocation texture, ScreenAnchor anchor,
                               int x, int y, int width, int height, int ticks) {
        this.show = show;
        this.id = id;
        this.texture = texture;
        this.anchor = anchor;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.ticks = ticks;
    }

    public static CScreenImagePacket show(String id, ResourceLocation texture, ScreenAnchor anchor,
                                          int x, int y, int width, int height, int durationTicks) {
        return new CScreenImagePacket(true, id, texture, anchor, x, y, width, height, durationTicks);
    }

    public static CScreenImagePacket hide(String id, int delayTicks) {
        return new CScreenImagePacket(false, id, null, ScreenAnchor.CENTER, 0, 0, 0, 0, delayTicks);
    }

    public CScreenImagePacket(FriendlyByteBuf buffer) {
        this.show = buffer.readBoolean();
        this.id = buffer.readUtf();
        this.texture = show ? buffer.readResourceLocation() : null;
        this.anchor = buffer.readEnum(ScreenAnchor.class);
        this.x = buffer.readInt();
        this.y = buffer.readInt();
        this.width = buffer.readInt();
        this.height = buffer.readInt();
        this.ticks = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(show);
        buffer.writeUtf(id);
        if (show) {
            buffer.writeResourceLocation(texture);
        }
        buffer.writeEnum(anchor);
        buffer.writeInt(x);
        buffer.writeInt(y);
        buffer.writeInt(width);
        buffer.writeInt(height);
        buffer.writeInt(ticks);
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (!FMLEnvironment.dist.isClient()) return;
        if (show) {
            ScreenImageRenderer.show(id, texture, anchor, x, y, width, height, ticks);
        } else {
            ScreenImageRenderer.hide(id, ticks);
        }
    }
}
