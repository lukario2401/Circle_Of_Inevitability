package net.lukario.circleofinevitability.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record SAbilityKeyPacket(int key, Action action) {
    public enum Action { PRESS, REPEAT, RELEASE, CANCEL }

    public SAbilityKeyPacket(FriendlyByteBuf buffer) {
        this(buffer.readUnsignedByte(), buffer.readEnum(Action.class));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeByte(key);
        buffer.writeEnum(action);
    }

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player != null) AbilityKeys.input(player, key, action);
    }
}
