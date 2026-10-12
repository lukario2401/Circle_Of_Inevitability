package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.status.ParasiteControl;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client -> Server: what a player inside a host pressed this tick (see status/ParasiteControl).
 * Sent by client/ClientParasiteControl every tick while they steer it, nothing while they let it be.
 */
public class SParasiteControlPacket {

    private static final int JUMP = 1, SPRINT = 2, STRIKE = 4, PICK = 8;

    private final int forward;      // 1 forward, -1 back, 0 neither
    private final int strafe;       // 1 left, -1 right, 0 neither
    private final int keys;         // JUMP | SPRINT | STRIKE (left click) | PICK (right click)
    private final float yaw;
    private final float pitch;

    public SParasiteControlPacket(int forward, int strafe, boolean jump, boolean sprint, boolean strike, boolean pick,
                                  float yaw, float pitch) {
        this.forward = forward;
        this.strafe = strafe;
        this.keys = (jump ? JUMP : 0) | (sprint ? SPRINT : 0) | (strike ? STRIKE : 0) | (pick ? PICK : 0);
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public SParasiteControlPacket(FriendlyByteBuf buffer) {
        this.forward = buffer.readByte();
        this.strafe = buffer.readByte();
        this.keys = buffer.readByte();
        this.yaw = buffer.readFloat();
        this.pitch = buffer.readFloat();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeByte(forward);
        buffer.writeByte(strafe);
        buffer.writeByte(keys);
        buffer.writeFloat(yaw);
        buffer.writeFloat(pitch);
    }

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        ParasiteControl.input(player, forward, strafe, (keys & JUMP) != 0, (keys & SPRINT) != 0,
                (keys & STRIKE) != 0, (keys & PICK) != 0, yaw, pitch);
    }
}
