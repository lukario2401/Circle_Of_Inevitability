package net.lukario.frogerealm.network;

import net.lukario.frogerealm.root.Root;
import net.lukario.frogerealm.root.RootRestriction;
import net.minecraft.network.chat.Component;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.KeyOfStars.keyOfStarsOrbital;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PaleEmperor.paleEmperorWings;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.HandOfOrder.handOfOrderWords;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PrinceOfAbolition.princeOfAbolitionEntropy;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesHistoricalVoid;

public class SKeyPressAbilitySixUsed {

    public SKeyPressAbilitySixUsed() {}
    public SKeyPressAbilitySixUsed(FriendlyByteBuf buffer) {}
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

        keyOfStarsOrbital(player,level,serverLevel,false);
        handOfOrderWords(player,level,serverLevel,false);
        princeOfAbolitionEntropy(player,level,serverLevel,false);
        attendantOfMysteriesHistoricalVoid(player,level,serverLevel,false);
        paleEmperorWings(player,level,serverLevel,false);

    }
}
