package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.snake_of_mercury.SnakeOfMercuryPowers;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time.WormOfTimePowers;

import net.lukario.circleofinevitability.combat.AbilityCharge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.AbominationZombie.abominationZombie;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.apocalypse.Apocalypse.apocalypseSilkWeaver;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.attendant_of_mysteries.AttendantOfMysteries.attendantOfMysteriesAirCannon;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.conqueror.Conqueror.conquerorFlameTransformation;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.dark_angel.DarkAngel.hangedManFleshBomb;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.beauty_goddess.BeautyGoddess.beautyGoddessPotions;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrderVerdicts.handOfOrderVerdict;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.wanderer.Wanderer.wandererSeal;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStars.keyOfStarsRecord;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.pale_emperor.PaleEmperor.paleEmperorSpearSkull;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.frenzied_mage.FrenziedMage.frenziedMageHammer;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGodDawn.handOfGodDawn;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortune.knightOfMisfortuneRequiem;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolition.princeOfAbolitionDistort;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperor.knowledgeEmperorScrolls;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorWorkshop.illuminatorWorkshop;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragon.starDragonTidalScholar;

final class SKeyPressAbilityFourUsed {
    private SKeyPressAbilityFourUsed() {}

    static void cast(ServerPlayer player, Level level, ServerLevel serverLevel, AbilityCharge charge) {
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god.ThunderGod.key(player, level, serverLevel, 4, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye.OmniscientEye.key(player, level, serverLevel, 4, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.white_angel.WhiteAngel.key(player, level, serverLevel, 4, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.author.Author.key(player, level, serverLevel, 4, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.filthy_monarch.FilthyMonarch.key(player, level, serverLevel, 4, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker.NatureWalker.key(player, level, serverLevel, 4, false);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay.GodOfDecay.key(player, level, serverLevel, 4, false);
        SnakeOfMercuryPowers.calamity(player, level, serverLevel, false);
        WormOfTimePowers.insight(player, level, serverLevel, false);

        wandererSeal(player,level,serverLevel,false);

        keyOfStarsRecord(player, level, serverLevel, false);
        attendantOfMysteriesAirCannon(player,level,serverLevel,false);
        frenziedMageHammer(player,level,serverLevel,false);
        handOfOrderVerdict(player,level,serverLevel,false);
        paleEmperorSpearSkull(player,level,serverLevel,false);
        conquerorFlameTransformation(player,level,serverLevel,false);
        apocalypseSilkWeaver(player,level,serverLevel,false);
        hangedManFleshBomb(player, level, serverLevel, false, charge);
        beautyGoddessPotions(player,level,serverLevel,false);
        handOfGodDawn(player,level,serverLevel,false);
        knightOfMisfortuneRequiem(player,level,serverLevel,false);
        abominationZombie(player,level,serverLevel,false);
        princeOfAbolitionDistort(player,level,serverLevel,false);
        knowledgeEmperorScrolls(player,level,serverLevel,false);
        illuminatorWorkshop(player,level,serverLevel,false);
        starDragonTidalScholar(player,level,serverLevel,false);
    }
}
