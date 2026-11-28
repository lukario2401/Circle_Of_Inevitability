package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.snake_of_mercury.SnakeOfMercury;

import net.lukario.circleofinevitability.combat.AbilityCharge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.Abomination.abominationBloodChain;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.apocalypse.Apocalypse.apocalypseFatalStep;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.attendant_of_mysteries.AttendantOfMysteries.attendantOfMysteriesPaperDagger;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.conqueror.Conqueror.conquerorHuntersTrap;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.dark_angel.DarkAngel.hangedManDivination;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.beauty_goddess.BeautyGoddess.beautyGoddessMedicine;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGod.handOfGodWarriorsCharge;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortune.knightOfMisfortuneNightSight;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrder.handOfOrderAuthority;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.wanderer.Wanderer.wandererStarBurst;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStars.keyOfStarsDoorOpening;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.pale_emperor.PaleEmperor.paleEmperorRoot;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.frenzied_mage.FrenziedMage.frenziedMageSpear;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time.WormOfTime.wormOfTimeAbility1;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolition.princeOfAbolitionVerdict;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperor.knowledgeEmperorMysteryPrying;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.Illuminator.illuminatorCompoundFlask;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragon.starDragonAstronomy;

final class SKeyPressAbilityOneUsed {
    private SKeyPressAbilityOneUsed() {}

    static void cast(ServerPlayer player, Level level, ServerLevel serverLevel, AbilityCharge charge) {
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god.ThunderGod.key(player, level, serverLevel, 1, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye.OmniscientEye.key(player, level, serverLevel, 1, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.white_angel.WhiteAngel.key(player, level, serverLevel, 1, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.author.Author.key(player, level, serverLevel, 1, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.filthy_monarch.FilthyMonarch.key(player, level, serverLevel, 1, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker.NatureWalker.key(player, level, serverLevel, 1, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay.GodOfDecay.key(player, level, serverLevel, 1, false);

        wandererStarBurst(player,level,serverLevel,false);

        keyOfStarsDoorOpening(player, level, serverLevel, false);
        attendantOfMysteriesPaperDagger(player,level,serverLevel,false);
        frenziedMageSpear(player,level,serverLevel,false);
        handOfOrderAuthority(player,level,serverLevel,false);
        paleEmperorRoot(player,serverLevel,false);
        wormOfTimeAbility1(player,level,serverLevel,false);
        conquerorHuntersTrap(player,level,serverLevel,false);
        apocalypseFatalStep(player,level,serverLevel,false);
        hangedManDivination(player,level,serverLevel,false);
        beautyGoddessMedicine(player,level,serverLevel,false);
        handOfGodWarriorsCharge(player,level,serverLevel,false);
        knightOfMisfortuneNightSight(player,level,serverLevel,false);
        abominationBloodChain(player,level,serverLevel,false);
        princeOfAbolitionVerdict(player,level,serverLevel,false);
        knowledgeEmperorMysteryPrying(player,level,serverLevel,false);
        illuminatorCompoundFlask(player,level,serverLevel,false);
        starDragonAstronomy(player,level,serverLevel,false);
        SnakeOfMercury.premonition(player, level, serverLevel, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer.Observer.observerTerritory(player, level, serverLevel, false);
    }
}
