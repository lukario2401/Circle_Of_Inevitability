package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.StolenAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.snake_of_mercury.SnakeOfMercuryPowers;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time.WormOfTimePowers;
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

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.AbominationWraith.abominationWraith;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Apocalypse.apocalypsePlague;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesCountdown;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Conqueror.conquerorCull;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.DarkAngel.hangedManGrazing;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.BeautyGoddess.beautyGoddessLunarBattlefield;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGod.handOfGodProtection;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortuneSpirits.knightOfMisfortuneSpiritCommanding;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrderDecrees.handOfOrderDecree;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Wanderer.wandererTransport;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStarsTravel.keyOfStarsTravel;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PaleEmperor.paleEmperorSpikes;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.FrenziedMage.frenziedMageMeteor;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolition.princeOfAbolitionDisorder;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperorStars.knowledgeEmperorStars;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorAstronomy.illuminatorAstronomy;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragon.starDragonHeavybringer;

public class SKeyPressAbilityFiveUsed {

    public SKeyPressAbilityFiveUsed() {}
    public SKeyPressAbilityFiveUsed(FriendlyByteBuf buffer) {}
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
        if (Decree.blocksKey(player, 5)) return;
        // rooted with ABILITIES = can't use any ability (status/Silence lets a power that needs no voice through)
        if (Silence.blocksKey(player, 5)) {
            player.sendOverlayMessage(Component.literal("You are bound by Order."));
            return;
        }
        // an ability that was stolen from this player (Worm Of Time) is not theirs to use meanwhile
        if (StolenAbilities.blocksKey(player, 5)) return;
        // a player who lives inside a host (status/Parasite) has no body to use most abilities with
        if (Parasite.blocksKey(player, 5)) return;
        // one power taken away for a while (status/KeySeal, the Hand Of Order's Deprivation)
        if (KeySeal.blocksKey(player, 5)) return;
        if (net.lukario.circleofinevitability.combat.HeldActions.defer(player, () -> cast(player))) return;
        KeySeal.pressed(player, 5);
        Casts.keyPressed(player, 5);                 // combat/Casts: told if the press goes off
        try {
            net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay.GodOfDecay.key(player, level, serverLevel, 5, false);
            SnakeOfMercuryPowers.misfortuneWheel(player, level, serverLevel, false);
            WormOfTimePowers.deceit(player, level, serverLevel, false);

            wandererTransport(player,level,serverLevel, false);

            keyOfStarsTravel(player, level, serverLevel, false);
            attendantOfMysteriesCountdown(player,level,serverLevel, false);
            handOfOrderDecree(player,level,serverLevel, false);
            frenziedMageMeteor(player,level,serverLevel,false);
            paleEmperorSpikes(player,level,serverLevel,false);
            conquerorCull(player,level,serverLevel,false);
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
        } finally {
            Casts.keyDone(player);
        }
    }
}
