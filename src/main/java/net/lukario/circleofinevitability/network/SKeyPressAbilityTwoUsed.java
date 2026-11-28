package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.snake_of_mercury.SnakeOfMercuryPowers;

import net.lukario.circleofinevitability.combat.AbilityCharge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.Abomination.abominationMoonstruck;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.apocalypse.Apocalypse.apocalypseBlackFlame;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.attendant_of_mysteries.AttendantOfMysteries.attendantOfMysteriesFlamingJump;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.conqueror.Conqueror.conquerorProvocation;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.dark_angel.DarkAngel.hangedManWhispers;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.beauty_goddess.BeautyGoddess.beautyGoddessTaming;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGod.handOfGodGladiatorsFlurry;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortunePoems.knightOfMisfortuneMidnightPoem;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrder.handOfOrderJurisdiction;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.wanderer.Wanderer.wandererDash;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStarsTricks.keyOfStarsTricks;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.pale_emperor.PaleEmperor.paleEmperorDoor;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.frenzied_mage.FrenziedMage.frenziedMageFlame;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time.WormOfTime.wormOfTimeAbility2;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolition.princeOfAbolitionBruteForce;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperor.knowledgeEmperorWeakPointStrike;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.Illuminator.illuminatorSurvey;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragon.starDragonStarSacrifice;

final class SKeyPressAbilityTwoUsed {
    private SKeyPressAbilityTwoUsed() {}

    static void cast(ServerPlayer player, Level level, ServerLevel serverLevel, AbilityCharge charge) {
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god.ThunderGod.key(player, level, serverLevel, 2, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye.OmniscientEye.key(player, level, serverLevel, 2, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.white_angel.WhiteAngel.key(player, level, serverLevel, 2, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.author.Author.key(player, level, serverLevel, 2, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.filthy_monarch.FilthyMonarch.key(player, level, serverLevel, 2, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker.NatureWalker.key(player, level, serverLevel, 2, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay.GodOfDecay.key(player, level, serverLevel, 2, false);
        SnakeOfMercuryPowers.calculation(player, level, serverLevel, false);

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
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer.Observer.observerSight(player, level, serverLevel, false);
    }
}
