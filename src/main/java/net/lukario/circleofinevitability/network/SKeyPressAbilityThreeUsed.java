package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.StolenAbilities;
import net.lukario.circleofinevitability.status.Parasite;
import net.lukario.circleofinevitability.status.Silence;
import net.minecraft.network.chat.Component;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.Abomination.abominationRendingPounce;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Apocalypse.apocalypseMirrorWorld;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesDamageTransfer;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.Conqueror.conquerorFireRavens;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.DarkAngel.hangedManShadowBinding;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.BeautyGoddess.beautyGoddessWings;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGod.handOfGodGrandmastersEdge;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortune.knightOfMisfortuneNightmare;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.HandOfOrder.handOfOrderEdict;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.KeyOfStars.keyOfStarsBuff;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PaleEmperor.paleEmperorSerpent;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.FrenziedMage.frenziedMageFrost;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.WormOfTime.wormOfTimeAbility3;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionBribes.princeOfAbolitionBribe;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperor.knowledgeEmperorSpells;

public class SKeyPressAbilityThreeUsed {

    public SKeyPressAbilityThreeUsed() {}
    public SKeyPressAbilityThreeUsed(FriendlyByteBuf buffer) {}
    public void encode(FriendlyByteBuf buffer) {}

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if(player == null)
            return;

        Level level = player.level();

        if (!(level instanceof ServerLevel serverLevel)) return;

        // rooted with ABILITIES = can't use any ability (status/Silence lets a power that needs no voice through)
        if (Silence.blocksKey(player, 3)) {
            player.sendOverlayMessage(Component.literal("You are bound by Order."));
            return;
        }
        // an ability that was stolen from this player (Worm Of Time) is not theirs to use meanwhile
        if (StolenAbilities.blocksKey(player, 3)) return;
        // a player who lives inside a host (status/Parasite) has no body to use most abilities with
        if (Parasite.blocksKey(player, 3)) return;

        keyOfStarsBuff(player,level,serverLevel,false);
        attendantOfMysteriesDamageTransfer(player,level,serverLevel,false);
        handOfOrderEdict(player,level,serverLevel,false);
        frenziedMageFrost(player,level,serverLevel,false);
        paleEmperorSerpent(player,level,serverLevel,false);
        wormOfTimeAbility3(player,level,serverLevel,false);
        conquerorFireRavens(player,level,serverLevel,false);
        apocalypseMirrorWorld(player,level,serverLevel,false);
        hangedManShadowBinding(player,level,serverLevel,false);
        beautyGoddessWings(player,level,serverLevel,false);
        handOfGodGrandmastersEdge(player,level,serverLevel,false);
        knightOfMisfortuneNightmare(player,level,serverLevel,false);
        abominationRendingPounce(player,level,serverLevel,false);
        princeOfAbolitionBribe(player,level,serverLevel,false);
        knowledgeEmperorSpells(player,level,serverLevel,false);


    }
}
