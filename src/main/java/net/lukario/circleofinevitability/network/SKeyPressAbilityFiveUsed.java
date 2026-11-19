package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.snake_of_mercury.SnakeOfMercuryPowers;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time.WormOfTimePowers;

import net.lukario.circleofinevitability.combat.AbilityCharge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.AbominationWraith.abominationWraith;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.apocalypse.Apocalypse.apocalypsePlague;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.attendant_of_mysteries.AttendantOfMysteries.attendantOfMysteriesCountdown;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.conqueror.Conqueror.conquerorCull;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.dark_angel.DarkAngel.hangedManGrazing;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.beauty_goddess.BeautyGoddess.beautyGoddessLunarBattlefield;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGod.handOfGodProtection;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortuneSpirits.knightOfMisfortuneSpiritCommanding;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrderDecrees.handOfOrderDecree;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.wanderer.Wanderer.wandererTransport;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStarsTravel.keyOfStarsTravel;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.pale_emperor.PaleEmperor.paleEmperorSpikes;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.frenzied_mage.FrenziedMage.frenziedMageMeteor;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolition.princeOfAbolitionDisorder;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperorStars.knowledgeEmperorStars;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorAstronomy.illuminatorAstronomy;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragon.starDragonHeavybringer;

final class SKeyPressAbilityFiveUsed {
    private SKeyPressAbilityFiveUsed() {}

    static void cast(ServerPlayer player, Level level, ServerLevel serverLevel, AbilityCharge charge) {
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god.ThunderGod.key(player, level, serverLevel, 5, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye.OmniscientEye.key(player, level, serverLevel, 5, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.white_angel.WhiteAngel.key(player, level, serverLevel, 5, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.author.Author.key(player, level, serverLevel, 5, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.filthy_monarch.FilthyMonarch.key(player, level, serverLevel, 5, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker.NatureWalker.key(player, level, serverLevel, 5, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay.GodOfDecay.key(player, level, serverLevel, 5, false);
        SnakeOfMercuryPowers.misfortuneWheel(player, level, serverLevel, false);
        WormOfTimePowers.deceit(player, level, serverLevel, false);

        wandererTransport(player,level,serverLevel, false);

        keyOfStarsTravel(player, level, serverLevel, false);
        attendantOfMysteriesCountdown(player,level,serverLevel, false);
        handOfOrderDecree(player,level,serverLevel, false);
        frenziedMageMeteor(player,level,serverLevel,false);
        paleEmperorSpikes(player,level,serverLevel,false);
        conquerorCull(player, level, serverLevel, false, charge);
        apocalypsePlague(player,level,serverLevel,false);
        hangedManGrazing(player,level,serverLevel,false);
        beautyGoddessLunarBattlefield(player,level,serverLevel,false);
        handOfGodProtection(player,level,serverLevel,false);
        knightOfMisfortuneSpiritCommanding(player,level,serverLevel,false);
        abominationWraith(player,level,serverLevel,false);
        princeOfAbolitionDisorder(player,level,serverLevel,false);
        knowledgeEmperorStars(player,level,serverLevel,false);
        illuminatorAstronomy(player,level,serverLevel,false);
        starDragonHeavybringer(player,level,serverLevel,false);
    }
}
