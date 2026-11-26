package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.camera.DetachedCamera;
import net.lukario.circleofinevitability.item.artifacts.MonocleItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** The Monocle's Gaze key. The server checks that the Monocle is worn and decides everything else. */
public class SMonocleGazePacket {
    public SMonocleGazePacket() {}
    public SMonocleGazePacket(FriendlyByteBuf buffer) {}
    public void encode(FriendlyByteBuf buffer) {}

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || DetachedCamera.active(player)) return;
        MonocleItem.gaze(player);
    }
}
