package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.StolenAbilities;
import net.lukario.circleofinevitability.status.Parasite;
import net.lukario.circleofinevitability.status.Silence;
import net.minecraft.network.chat.Component;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Apocalypse.apocalypseBlackFlame;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesFlamingJump;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Conqueror.conquerorProvocation;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.DarkAngel.hangedManWhispers;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.BeautyGoddess.beautyGoddessTaming;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGod.handOfGodGladiatorsFlurry;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortunePoems.knightOfMisfortuneMidnightPoem;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.HandOfOrder.handOfOrderDecree;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.KeyOfStars.keyOfStarsDash;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PaleEmperor.paleEmperorDoor;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PrinceOfAbolition.princeOfAbolitionFlame;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.WormOfTime.wormOfTimeAbility2;

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

        // rooted with ABILITIES = can't use any ability (status/Silence lets a power that needs no voice through)
        if (Silence.blocksKey(player, 2)) {
            player.sendOverlayMessage(Component.literal("You are bound by Order."));
            return;
        }
        // an ability that was stolen from this player (Worm Of Time) is not theirs to use meanwhile
        if (StolenAbilities.blocksKey(player, 2)) return;
        // a player who lives inside a host (status/Parasite) has no body to use most abilities with
        if (Parasite.blocksKey(player, 2)) return;

        keyOfStarsDash(player,level,serverLevel, false);
        attendantOfMysteriesFlamingJump(player,level,serverLevel, false);
        handOfOrderDecree(player,level,serverLevel, false);
        princeOfAbolitionFlame(player,level,serverLevel,false);
        paleEmperorDoor(player,serverLevel,false);
        wormOfTimeAbility2(player,level,serverLevel,false);
        conquerorProvocation(player,level,serverLevel,false);
        apocalypseBlackFlame(player,level,serverLevel,false);
        hangedManWhispers(player,level,serverLevel,false);
        beautyGoddessTaming(player,level,serverLevel,false);
        handOfGodGladiatorsFlurry(player,level,serverLevel,false);
        knightOfMisfortuneMidnightPoem(player,level,serverLevel,false);

    }
}
