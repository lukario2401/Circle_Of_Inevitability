package net.lukario.circleofinevitability.camera;

import net.lukario.circleofinevitability.client.ClientDetachedCamera;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.UUID;

public record CameraFrame(Kind kind, UUID session, Vec3 position, float yaw, float pitch, int fov, boolean hideHud, int returnTicks) {
    public enum Kind { OPEN, MOVE, RETURN, CLOSE }

    static CameraFrame open(UUID id, Vec3 position, float yaw, float pitch, CameraPreset.Display display) {
        return new CameraFrame(Kind.OPEN, id, position, yaw, pitch, display.fov(), display.hideHud(), 0);
    }
    static CameraFrame move(UUID id, Vec3 position) {
        return new CameraFrame(Kind.MOVE, id, position, 0, 0, 0, false, 0);
    }
    static CameraFrame returning(UUID id, int ticks) {
        return new CameraFrame(Kind.RETURN, id, Vec3.ZERO, 0, 0, 0, false, ticks);
    }
    static CameraFrame close(UUID id) {
        return new CameraFrame(Kind.CLOSE, id, Vec3.ZERO, 0, 0, 0, false, 0);
    }

    public static CameraFrame decode(FriendlyByteBuf buf) {
        Kind kind = buf.readEnum(Kind.class);
        UUID id = buf.readUUID();
        if (kind == Kind.CLOSE) return close(id);
        if (kind == Kind.RETURN) return returning(id, buf.readVarInt());
        Vec3 point = Vec3.STREAM_CODEC.decode(buf);
        if (kind == Kind.MOVE) return move(id, point);
        return new CameraFrame(kind, id, point, buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readBoolean(), 0);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(kind);
        buf.writeUUID(session);
        if (kind == Kind.RETURN) buf.writeVarInt(returnTicks);
        if (kind == Kind.OPEN || kind == Kind.MOVE) Vec3.STREAM_CODEC.encode(buf, position);
        if (kind == Kind.OPEN) {
            buf.writeFloat(yaw);
            buf.writeFloat(pitch);
            buf.writeVarInt(fov);
            buf.writeBoolean(hideHud);
        }
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist.isClient()) ClientDetachedCamera.receive(this);
    }
}
