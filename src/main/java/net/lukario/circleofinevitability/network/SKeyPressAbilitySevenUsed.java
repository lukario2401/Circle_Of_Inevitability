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

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.AbominationCurses.abominationCurses;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Apocalypse.apocalypseCatastrophe;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.KeyOfStars.keyOfStarsCosmicPlague;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Conqueror.conquerorFogOfWar;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PaleEmperor.paleEmperorWraiths;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.DarkAngel.hangedManDegeneration;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.BeautyGoddess.beautyGoddessMoonWheel;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.HandOfOrder.handOfOrderBind;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.FrenziedMage.frenziedMageAbolition;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesGrafting;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGodTwilight.handOfGodTwilight;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortuneDarkness.knightOfMisfortuneDarkness;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionSovereign.princeOfAbolitionSovereign;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperorKnowledge.knowledgeEmperorKnowledge;

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

        // rooted with ABILITIES = can't use any ability (status/Silence lets a power that needs no voice through)
        if (Silence.blocksKey(player, 7)) {
            player.sendOverlayMessage(Component.literal("You are bound by Order."));
            return;
        }
        // an ability that was stolen from this player (Worm Of Time) is not theirs to use meanwhile
        if (StolenAbilities.blocksKey(player, 7)) return;
        // a player who lives inside a host (status/Parasite) has no body to use most abilities with
        if (Parasite.blocksKey(player, 7)) return;

        keyOfStarsCosmicPlague(player,level,serverLevel,false);
        handOfOrderBind(player,level,serverLevel,false);
        frenziedMageAbolition(player,level,serverLevel,false);
        attendantOfMysteriesGrafting(player,level,serverLevel,false);
        paleEmperorWraiths(player,level,serverLevel,false);
        conquerorFogOfWar(player,level,serverLevel,false);
        apocalypseCatastrophe(player,level,serverLevel,false);
        hangedManDegeneration(player,level,serverLevel,false);
        beautyGoddessMoonWheel(player,level,serverLevel,false);
        handOfGodTwilight(player,level,serverLevel,false);
        knightOfMisfortuneDarkness(player,level,serverLevel,false);
        abominationCurses(player,level,serverLevel,false);
        princeOfAbolitionSovereign(player,level,serverLevel,false);
        knowledgeEmperorKnowledge(player,level,serverLevel,false);


    }

}
