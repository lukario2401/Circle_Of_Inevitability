package net.lukario.circleofinevitability.camera;

import net.lukario.circleofinevitability.client.ClientDetachedCamera;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.UUID;

public record CameraFrame(Kind kind, UUID session, Vec3 position, float yaw, float pitch, int fov, boolean hideHud, int returnTicks) {
    public enum Kind { OPEN, MOVE, RETURN, CLOSE, OPEN_SCRIPTED, POSE, SNAP }

    static CameraFrame open(UUID id, Vec3 position, float yaw, float pitch, CameraPreset.Display display, boolean scripted) {
        return new CameraFrame(scripted ? Kind.OPEN_SCRIPTED : Kind.OPEN, id, position, yaw, pitch, display.fov(), display.hideHud(), 0);
    }
    static CameraFrame move(UUID id, Vec3 position, boolean snap) {
        return new CameraFrame(snap ? Kind.SNAP : Kind.MOVE, id, position, 0, 0, 0, false, 0);
    }
    static CameraFrame pose(UUID id, Vec3 position, float yaw, float pitch) {
        return new CameraFrame(Kind.POSE, id, position, yaw, pitch, 0, false, 0);
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
        if (kind == Kind.MOVE || kind == Kind.SNAP) return move(id, point, kind == Kind.SNAP);
        float yaw = buf.readFloat();
        float pitch = buf.readFloat();
        if (kind == Kind.POSE) return pose(id, point, yaw, pitch);
        return new CameraFrame(kind, id, point, yaw, pitch, buf.readVarInt(), buf.readBoolean(), 0);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(kind);
        buf.writeUUID(session);
        if (kind == Kind.CLOSE) return;
        if (kind == Kind.RETURN) {
            buf.writeVarInt(returnTicks);
            return;
        }
        Vec3.STREAM_CODEC.encode(buf, position);
        if (kind == Kind.MOVE || kind == Kind.SNAP) return;
        buf.writeFloat(yaw);
        buf.writeFloat(pitch);
        if (kind == Kind.POSE) return;
        buf.writeVarInt(fov);
        buf.writeBoolean(hideHud);
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist.isClient()) ClientDetachedCamera.receive(this);
    }
}
