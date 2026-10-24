package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.StolenAbilities;
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

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.Abomination.abominationBloodChain;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Apocalypse.apocalypseFatalStep;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesPaperDagger;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Conqueror.conquerorHuntersTrap;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.DarkAngel.hangedManDivination;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.BeautyGoddess.beautyGoddessMedicine;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGod.handOfGodWarriorsCharge;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortune.knightOfMisfortuneNightSight;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrder.handOfOrderAuthority;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Wanderer.wandererStarBurst;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PaleEmperor.paleEmperorRoot;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.FrenziedMage.frenziedMageSpear;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.WormOfTime.wormOfTimeAbility1;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolition.princeOfAbolitionVerdict;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperor.knowledgeEmperorMysteryPrying;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.Illuminator.illuminatorCompoundFlask;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragon.starDragonAstronomy;

public class SKeyPressAbilityOneUsed {

    public SKeyPressAbilityOneUsed() {}
    public SKeyPressAbilityOneUsed(FriendlyByteBuf buffer) {}
    public void encode(FriendlyByteBuf buffer) {}

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if(player == null)
            return;

        Level level = player.level();

        if (!(level instanceof ServerLevel serverLevel)) return;

        // a Decree that prohibits abilities here (status/Decree, ABILITIES): refused, and a Violation
        if (Decree.blocksKey(player, 1)) return;
        // rooted with ABILITIES = can't use any ability (status/Silence lets a power that needs no voice through)
        if (Silence.blocksKey(player, 1)) {
            player.sendOverlayMessage(Component.literal("You are bound by Order."));
            return;
        }
        // an ability that was stolen from this player (Worm Of Time) is not theirs to use meanwhile
        if (StolenAbilities.blocksKey(player, 1)) return;
        // a player who lives inside a host (status/Parasite) has no body to use most abilities with
        if (Parasite.blocksKey(player, 1)) return;
        // one power taken away for a while (status/KeySeal, the Hand Of Order's Deprivation)
        if (KeySeal.blocksKey(player, 1)) return;
        KeySeal.pressed(player, 1);

        wandererStarBurst(player,level,serverLevel,false);
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
    }
}
