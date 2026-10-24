package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.StolenAbilities;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.status.Decree;
import net.lukario.circleofinevitability.status.KeySeal;
import net.lukario.circleofinevitability.status.Parasite;
import net.lukario.circleofinevitability.status.Silence;
import net.minecraft.network.chat.Component;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.Abomination.abominationMoonstruck;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Apocalypse.apocalypseBlackFlame;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesFlamingJump;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Conqueror.conquerorProvocation;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.DarkAngel.hangedManWhispers;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.BeautyGoddess.beautyGoddessTaming;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGod.handOfGodGladiatorsFlurry;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortunePoems.knightOfMisfortuneMidnightPoem;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrder.handOfOrderJurisdiction;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Wanderer.wandererDash;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStarsTricks.keyOfStarsTricks;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PaleEmperor.paleEmperorDoor;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.FrenziedMage.frenziedMageFlame;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.WormOfTime.wormOfTimeAbility2;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolition.princeOfAbolitionBruteForce;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperor.knowledgeEmperorWeakPointStrike;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.Illuminator.illuminatorSurvey;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragon.starDragonStarSacrifice;

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

        // a Decree that prohibits abilities here (status/Decree, ABILITIES): refused, and a Violation
        if (Decree.blocksKey(player, 2)) return;
        // rooted with ABILITIES = can't use any ability (status/Silence lets a power that needs no voice through)
        if (Silence.blocksKey(player, 2)) {
            player.sendOverlayMessage(Component.literal("You are bound by Order."));
            return;
        }
        // an ability that was stolen from this player (Worm Of Time) is not theirs to use meanwhile
        if (StolenAbilities.blocksKey(player, 2)) return;
        // a player who lives inside a host (status/Parasite) has no body to use most abilities with
        if (Parasite.blocksKey(player, 2)) return;
        // one power taken away for a while (status/KeySeal, the Hand Of Order's Deprivation)
        if (KeySeal.blocksKey(player, 2)) return;
        KeySeal.pressed(player, 2);
        Casts.keyPressed(player, 2);                 // combat/Casts: told if the press goes off
        try {

            wandererDash(player,level,serverLevel, false);

            keyOfStarsTricks(player, level, serverLevel, false);
            attendantOfMysteriesFlamingJump(player,level,serverLevel, false);
            handOfOrderJurisdiction(player,level,serverLevel, false);
            frenziedMageFlame(player,level,serverLevel,false);
            paleEmperorDoor(player,serverLevel,false);
            wormOfTimeAbility2(player,level,serverLevel,false);
            conquerorProvocation(player,level,serverLevel,false);
            apocalypseBlackFlame(player,level,serverLevel,false);
            hangedManWhispers(player,level,serverLevel,false);
            beautyGoddessTaming(player,level,serverLevel,false);
            handOfGodGladiatorsFlurry(player,level,serverLevel,false);
            knightOfMisfortuneMidnightPoem(player,level,serverLevel,false);
            abominationMoonstruck(player,level,serverLevel,false);
            princeOfAbolitionBruteForce(player,level,serverLevel,false);
            knowledgeEmperorWeakPointStrike(player,level,serverLevel,false);
            illuminatorSurvey(player,level,serverLevel,false);
            starDragonStarSacrifice(player,level,serverLevel,false);
        } finally {
            Casts.keyDone(player);
        }
    }
}
