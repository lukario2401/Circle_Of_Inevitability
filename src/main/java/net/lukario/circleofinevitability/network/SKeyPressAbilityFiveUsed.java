package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.network.chat.Component;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import static net.lukario.circleofinevitability.shadow_slave.soul_abilities.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesCountdown;
import static net.lukario.circleofinevitability.shadow_slave.soul_abilities.beyonder_characteristics.HandOfOrder.handOfOrderAbility5;
import static net.lukario.circleofinevitability.shadow_slave.soul_abilities.beyonder_characteristics.KeyOfStars.keyOfStarsTransport;
import static net.lukario.circleofinevitability.shadow_slave.soul_abilities.beyonder_characteristics.PaleEmperor.paleEmperorSpikes;
import static net.lukario.circleofinevitability.shadow_slave.soul_abilities.beyonder_characteristics.PrinceOfAbolition.princeOfAbolitionMeteor;

public class SKeyPressAbilityFiveUsed {

    public SKeyPressAbilityFiveUsed() {}
    public SKeyPressAbilityFiveUsed(FriendlyByteBuf buffer) {}
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

        keyOfStarsTransport(player,level,serverLevel, false);
        attendantOfMysteriesCountdown(player,level,serverLevel, false);
        handOfOrderAbility5(player,level,serverLevel, false);
        princeOfAbolitionMeteor(player,level,serverLevel,false);
        paleEmperorSpikes(player,level,serverLevel,false);


    }
}
