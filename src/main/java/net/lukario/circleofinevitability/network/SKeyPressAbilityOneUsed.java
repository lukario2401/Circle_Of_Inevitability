package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.blasphemy_slate.soul_abilities.beyonder_characteristics.StolenAbilities;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Parasite;
import net.minecraft.network.chat.Component;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import static net.lukario.circleofinevitability.blasphemy_slate.soul_abilities.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesPaperDagger;
import static net.lukario.circleofinevitability.blasphemy_slate.soul_abilities.beyonder_characteristics.HandOfOrder.handOfOrderBuff;
import static net.lukario.circleofinevitability.blasphemy_slate.soul_abilities.beyonder_characteristics.KeyOfStars.keyOfStarsStarBurst;
import static net.lukario.circleofinevitability.blasphemy_slate.soul_abilities.beyonder_characteristics.PaleEmperor.paleEmperorRoot;
import static net.lukario.circleofinevitability.blasphemy_slate.soul_abilities.beyonder_characteristics.PrinceOfAbolition.princeOfAbolitionSpear;
import static net.lukario.circleofinevitability.blasphemy_slate.soul_abilities.beyonder_characteristics.WormOfTime.wormOfTimeAbility1;

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

        // rooted with ABILITIES = can't use any ability
        if (Root.has(player, RootRestriction.ABILITIES)) {
            player.sendOverlayMessage(Component.literal("You are bound by Order."));
            return;
        }
        // an ability that was stolen from this player (Worm Of Time) is not theirs to use meanwhile
        if (StolenAbilities.blocksKey(player, 1)) return;
        // a player who lives inside a host (status/Parasite) has no body to use most abilities with
        if (Parasite.blocksKey(player, 1)) return;

        keyOfStarsStarBurst(player,level,serverLevel,false);
        attendantOfMysteriesPaperDagger(player,level,serverLevel,false);
        princeOfAbolitionSpear(player,level,serverLevel,false);
        handOfOrderBuff(player,level,serverLevel,false);
        paleEmperorRoot(player,serverLevel,false);
        wormOfTimeAbility1(player,level,serverLevel,false);
    }
}
