package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.network.chat.Component;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import static net.lukario.circleofinevitability.blasphemy_slate.soul_abilities.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesFlamingJump;
import static net.lukario.circleofinevitability.blasphemy_slate.soul_abilities.beyonder_characteristics.HandOfOrder.handOfOrderDecree;
import static net.lukario.circleofinevitability.blasphemy_slate.soul_abilities.beyonder_characteristics.KeyOfStars.keyOfStarsDash;
import static net.lukario.circleofinevitability.blasphemy_slate.soul_abilities.beyonder_characteristics.PaleEmperor.paleEmperorDoor;
import static net.lukario.circleofinevitability.blasphemy_slate.soul_abilities.beyonder_characteristics.PrinceOfAbolition.princeOfAbolitionFlame;

public class SKeyPressAbilityTwoUsed {

    public SKeyPressAbilityTwoUsed() {}
    public SKeyPressAbilityTwoUsed(FriendlyByteBuf buffer) {}
    public void encode(FriendlyByteBuf buffer) {}

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if(player == null)
            return;

        Level level = player.level();

        if (!(level instanceof ServerLevel serverLevel)) return;

        // rooted with ABILITIES = can't use any ability
        if (Root.has(player, RootRestriction.ABILITIES)) {
            player.sendOverlayMessage(Component.literal("You are bound by Order."));
            return;
        }

        keyOfStarsDash(player,level,serverLevel, false);
        attendantOfMysteriesFlamingJump(player,level,serverLevel, false);
        handOfOrderDecree(player,level,serverLevel, false);
        princeOfAbolitionFlame(player,level,serverLevel,false);
        paleEmperorDoor(player,serverLevel,false);

    }
}
