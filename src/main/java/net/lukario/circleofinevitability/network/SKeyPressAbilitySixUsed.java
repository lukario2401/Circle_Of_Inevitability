package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.snake_of_mercury.SnakeOfMercuryPowers;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time.WormOfTimePowers;

import net.lukario.circleofinevitability.combat.AbilityCharge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.apocalypse.Apocalypse.apocalypseUnaging;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.wanderer.Wanderer.wandererOrbital;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStars.keyOfStarsSecrets;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.conqueror.Conqueror.conquerorGalvanization;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.pale_emperor.PaleEmperor.paleEmperorWings;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.dark_angel.DarkAngel.hangedManCull;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.beauty_goddess.BeautyGoddess.beautyGoddessGaze;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrder.handOfOrderJudgment;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.frenzied_mage.FrenziedMage.frenziedMageEntropy;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.attendant_of_mysteries.AttendantOfMysteries.attendantOfMysteriesHistoricalVoid;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.AbominationChains.abominationChains;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGodDemonHunting.handOfGodDemonHunting;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortuneNightwatcher.knightOfMisfortuneNightwatcher;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionFallen.princeOfAbolitionFallen;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperorRetellings.knowledgeEmperorRetellings;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorAlchemy.illuminatorAlchemy;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragon.starDragonStarShepherd;

final class SKeyPressAbilitySixUsed {
    private SKeyPressAbilitySixUsed() {}

    static void cast(ServerPlayer player, Level level, ServerLevel serverLevel, AbilityCharge charge) {
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.white_angel.WhiteAngel.key(player, level, serverLevel, 6, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.author.Author.key(player, level, serverLevel, 6, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.filthy_monarch.FilthyMonarch.key(player, level, serverLevel, 6, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker.NatureWalker.key(player, level, serverLevel, 6, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god.ThunderGod.key(player, level, serverLevel, 6, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye.OmniscientEye.key(player, level, serverLevel, 6, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay.GodOfDecay.key(player, level, serverLevel, 6, false);
        SnakeOfMercuryPowers.chaosWheel(player, level, serverLevel, false);
        WormOfTimePowers.destiny(player, level, serverLevel, false);

        wandererOrbital(player,level,serverLevel,false);

        keyOfStarsSecrets(player, level, serverLevel, false);
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
        starDragonStarShepherd(player,level,serverLevel,false);
    }
}
