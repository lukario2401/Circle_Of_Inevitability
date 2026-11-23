package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStars;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record SOpenStarCameraPacket() {
    public void handle(CustomPayloadEvent.Context context) {
        if (context.getSender() != null) KeyOfStars.openScoutingCamera(context.getSender());
    }
}
