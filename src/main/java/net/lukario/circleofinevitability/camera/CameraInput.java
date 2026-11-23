package net.lukario.circleofinevitability.camera;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Input;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.UUID;

/** Only keys and look angles cross from client to server. The server computes the viewpoint. */
public record CameraInput(UUID session, Input keys, float yaw, float pitch, boolean use, boolean exit, boolean lostView, boolean stepHeld) {
    public CameraInput(FriendlyByteBuf buf) {
        this(buf.readUUID(), Input.STREAM_CODEC.decode(buf), buf.readFloat(), buf.readFloat(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(session);
        Input.STREAM_CODEC.encode(buf, keys);
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        buf.writeBoolean(use);
        buf.writeBoolean(exit);
        buf.writeBoolean(lostView);
        buf.writeBoolean(stepHeld);
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (context.getSender() != null) DetachedCamera.input(context.getSender(), this);
    }
}
