package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.StolenAbilities;
import net.lukario.circleofinevitability.status.KeySeal;
import net.lukario.circleofinevitability.status.Parasite;
import net.lukario.circleofinevitability.status.Silence;
import net.minecraft.network.chat.Component;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Apocalypse.apocalypseUnaging;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.KeyOfStars.keyOfStarsOrbital;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Conqueror.conquerorGalvanization;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PaleEmperor.paleEmperorWings;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.DarkAngel.hangedManCull;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.BeautyGoddess.beautyGoddessGaze;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrder.handOfOrderJudgment;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.FrenziedMage.frenziedMageEntropy;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesHistoricalVoid;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.AbominationChains.abominationChains;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGodDemonHunting.handOfGodDemonHunting;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortuneNightwatcher.knightOfMisfortuneNightwatcher;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionFallen.princeOfAbolitionFallen;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperorRetellings.knowledgeEmperorRetellings;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorAlchemy.illuminatorAlchemy;

public class SKeyPressAbilitySixUsed {

    public SKeyPressAbilitySixUsed() {}
    public SKeyPressAbilitySixUsed(FriendlyByteBuf buffer) {}
    public void encode(FriendlyByteBuf buffer) {}

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if(player == null)
            return;

        Level level = player.level();

        if (!(level instanceof ServerLevel serverLevel)) return;

        // rooted with ABILITIES = can't use any ability (status/Silence lets a power that needs no voice through)
        if (Silence.blocksKey(player, 6)) {
            player.sendOverlayMessage(Component.literal("You are bound by Order."));
            return;
        }
        // an ability that was stolen from this player (Worm Of Time) is not theirs to use meanwhile
        if (StolenAbilities.blocksKey(player, 6)) return;
        // a player who lives inside a host (status/Parasite) has no body to use most abilities with
        if (Parasite.blocksKey(player, 6)) return;
        // one power taken away for a while (status/KeySeal, the Hand Of Order's Deprivation)
        if (KeySeal.blocksKey(player, 6)) return;
        KeySeal.pressed(player, 6);

        keyOfStarsOrbital(player,level,serverLevel,false);
        handOfOrderJudgment(player,level,serverLevel,false);
        frenziedMageEntropy(player,level,serverLevel,false);
        attendantOfMysteriesHistoricalVoid(player,level,serverLevel,false);
        paleEmperorWings(player,level,serverLevel,false);
        conquerorGalvanization(player,level,serverLevel,false);
        apocalypseUnaging(player,level,serverLevel,false);
        hangedManCull(player,level,serverLevel,false);
        beautyGoddessGaze(player,level,serverLevel,false);
        handOfGodDemonHunting(player,level,serverLevel,false);
        knightOfMisfortuneNightwatcher(player,level,serverLevel,false);
        abominationChains(player,level,serverLevel,false);
        princeOfAbolitionFallen(player,level,serverLevel,false);
        knowledgeEmperorRetellings(player,level,serverLevel,false);
        illuminatorAlchemy(player,level,serverLevel,false);

    }
}
