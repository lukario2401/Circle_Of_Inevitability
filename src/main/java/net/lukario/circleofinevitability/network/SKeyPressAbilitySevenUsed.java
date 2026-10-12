package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.StolenAbilities;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Parasite;
import net.minecraft.network.chat.Component;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.KeyOfStars.keyOfStarsCosmicPlague;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PaleEmperor.paleEmperorWraiths;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.HandOfOrder.handOfOrderBind;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PrinceOfAbolition.princeOfAbolitionAbolition;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesGrafting;

public class SKeyPressAbilitySevenUsed {

    public SKeyPressAbilitySevenUsed() {}
    public SKeyPressAbilitySevenUsed(FriendlyByteBuf buffer) {}
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
        // an ability that was stolen from this player (Worm Of Time) is not theirs to use meanwhile
        if (StolenAbilities.blocksKey(player, 7)) return;
        // a player who lives inside a host (status/Parasite) has no body to use most abilities with
        if (Parasite.blocksKey(player, 7)) return;

        keyOfStarsCosmicPlague(player,level,serverLevel,false);
        handOfOrderBind(player,level,serverLevel,false);
        princeOfAbolitionAbolition(player,level,serverLevel,false);
        attendantOfMysteriesGrafting(player,level,serverLevel,false);
        paleEmperorWraiths(player,level,serverLevel,false);


    }

}
