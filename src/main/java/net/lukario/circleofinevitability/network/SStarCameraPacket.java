package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStars;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record SStarCameraPacket(Action action) {
    public enum Action { SCOUT, BEGIN_CHARGE, RELEASE, CANCEL }

    public SStarCameraPacket(FriendlyByteBuf buf) {
        this(buf.readEnum(Action.class));
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeEnum(action);
    }

    public void handle(CustomPayloadEvent.Context context) {
        var player = context.getSender();
        if (player == null) return;
        switch (action) {
            case SCOUT -> KeyOfStars.openScoutingCamera(player);
            case BEGIN_CHARGE -> KeyOfStars.beginCameraCharge(player);
            case RELEASE -> KeyOfStars.releaseCameraCharge(player);
            case CANCEL -> KeyOfStars.cancelCameraCharge(player);
        }
    }
}
