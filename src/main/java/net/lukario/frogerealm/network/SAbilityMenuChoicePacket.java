package net.lukario.frogerealm.network;

import net.lukario.frogerealm.menu.AbilityMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/**
 * Client -> Server: the player clicked an icon in an AbilityMenu (choice = -1: closed without picking).
 */
public class SAbilityMenuChoicePacket {

    private final String menuId;
    private final int choice;

    public SAbilityMenuChoicePacket(String menuId, int choice) {
        this.menuId = menuId;
        this.choice = choice;
    }

    public SAbilityMenuChoicePacket(FriendlyByteBuf buffer) {
        this.menuId = buffer.readUtf();
        this.choice = buffer.readInt();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(menuId);
        buffer.writeInt(choice);
    }

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null) return;
        AbilityMenu.handleChoice(player, menuId, choice);
    }
}
