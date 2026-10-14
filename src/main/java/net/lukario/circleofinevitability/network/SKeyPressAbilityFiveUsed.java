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

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Apocalypse.apocalypsePlague;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesCountdown;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Conqueror.conquerorCull;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.DarkAngel.hangedManGrazing;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.BeautyGoddess.beautyGoddessLunarBattlefield;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.HandOfOrder.handOfOrderAbility5;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.KeyOfStars.keyOfStarsTransport;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PaleEmperor.paleEmperorSpikes;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PrinceOfAbolition.princeOfAbolitionMeteor;

public class SKeyPressAbilityFiveUsed {

    public SKeyPressAbilityFiveUsed() {}
    public SKeyPressAbilityFiveUsed(FriendlyByteBuf buffer) {}
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
        if (StolenAbilities.blocksKey(player, 5)) return;
        // a player who lives inside a host (status/Parasite) has no body to use most abilities with
        if (Parasite.blocksKey(player, 5)) return;

        keyOfStarsTransport(player,level,serverLevel, false);
        attendantOfMysteriesCountdown(player,level,serverLevel, false);
        handOfOrderAbility5(player,level,serverLevel, false);
        princeOfAbolitionMeteor(player,level,serverLevel,false);
        paleEmperorSpikes(player,level,serverLevel,false);
        conquerorCull(player,level,serverLevel,false);
        apocalypsePlague(player,level,serverLevel,false);
        hangedManGrazing(player,level,serverLevel,false);
        beautyGoddessLunarBattlefield(player,level,serverLevel,false);



    }
}
