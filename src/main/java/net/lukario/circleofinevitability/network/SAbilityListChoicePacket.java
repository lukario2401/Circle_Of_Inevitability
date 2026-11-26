package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.menu.AbilityList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.ArrayList;
import java.util.List;

/** Client -> Server: the rows picked in an AbilityList (none: closed without picking). */
public record SAbilityListChoicePacket(String listId, List<Integer> picks) {

    public SAbilityListChoicePacket {
        picks = List.copyOf(picks);
    }

    public SAbilityListChoicePacket(FriendlyByteBuf buffer) {
        this(buffer.readUtf(256), readPicks(buffer));
    }

    private static List<Integer> readPicks(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > 64) throw new IllegalArgumentException("Ability list picks");
        List<Integer> picks = new ArrayList<>();
        for (int i = 0; i < count; i++) picks.add(buffer.readVarInt());
        return picks;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(listId, 256);
        buffer.writeVarInt(picks.size());
        for (int pick : picks) buffer.writeVarInt(pick);
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (context.getSender() != null) AbilityList.handleChoice(context.getSender(), listId, picks);
    }
}
