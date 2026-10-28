package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.StolenAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.snake_of_mercury.SnakeOfMercuryPowers;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time.WormOfTimeAuthority;
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

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.AbominationCurses.abominationCurses;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Apocalypse.apocalypseCatastrophe;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Wanderer.wandererCosmicPlague;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStars.keyOfStarsStarlight;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Conqueror.conquerorFogOfWar;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PaleEmperor.paleEmperorWraiths;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.DarkAngel.hangedManDegeneration;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.BeautyGoddess.beautyGoddessMoonWheel;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrderOrder.handOfOrderOrder;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.FrenziedMage.frenziedMageAbolition;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesGrafting;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGodTwilight.handOfGodTwilight;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortuneDarkness.knightOfMisfortuneDarkness;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionSovereign.princeOfAbolitionSovereign;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperorKnowledge.knowledgeEmperorKnowledge;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorCivilization.illuminatorCivilization;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragon.starDragonCosmos;

public class SKeyPressAbilitySevenUsed {

    public SKeyPressAbilitySevenUsed() {}
    public SKeyPressAbilitySevenUsed(FriendlyByteBuf buffer) {}
    public void encode(FriendlyByteBuf buffer) {}

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if(player == null)
            return;

        cast(player);
    }

    public static void cast(ServerPlayer player) {
        Level level = player.level();

        if (!(level instanceof ServerLevel serverLevel)) return;

        // a Decree that prohibits abilities here (status/Decree, ABILITIES): refused, and a Violation
        if (Decree.blocksKey(player, 7)) return;
        // rooted with ABILITIES = can't use any ability (status/Silence lets a power that needs no voice through)
        if (Silence.blocksKey(player, 7)) {
            player.sendOverlayMessage(Component.literal("You are bound by Order."));
            return;
        }
        // an ability that was stolen from this player (Worm Of Time) is not theirs to use meanwhile
        if (StolenAbilities.blocksKey(player, 7)) return;
        // a player who lives inside a host (status/Parasite) has no body to use most abilities with
        if (Parasite.blocksKey(player, 7)) return;
        // one power taken away for a while (status/KeySeal, the Hand Of Order's Deprivation)
        if (KeySeal.blocksKey(player, 7)) return;
        if (net.lukario.circleofinevitability.combat.HeldActions.defer(player, () -> cast(player))) return;
        KeySeal.pressed(player, 7);
        Casts.keyPressed(player, 7);                 // combat/Casts: told if the press goes off
        try {
            net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay.GodOfDecay.key(player, level, serverLevel, 7, false);
            SnakeOfMercuryPowers.cycleWheel(player, level, serverLevel, false);
            WormOfTimeAuthority.authority(player, level, serverLevel, false);

            wandererCosmicPlague(player,level,serverLevel,false);

            keyOfStarsStarlight(player, level, serverLevel, false);
            handOfOrderOrder(player,level,serverLevel,false);
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
            illuminatorCivilization(player,level,serverLevel,false);
            starDragonCosmos(player,level,serverLevel,false);
        } finally {
            Casts.keyDone(player);
        }
    }

}
