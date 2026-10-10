package net.lukario.frogerealm.network;

import net.lukario.frogerealm.root.Root;
import net.lukario.frogerealm.root.RootRestriction;
import net.minecraft.network.chat.Component;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesFlamingJump;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.HandOfOrder.handOfOrderDecree;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.KeyOfStars.keyOfStarsDash;
// TODO(stage5): PaleEmperor: import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PaleEmperor.paleEmperorDoor;
// TODO(stage5): PrinceOfAbolition: import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PrinceOfAbolition.princeOfAbolitionFlame;

public class SKeyPressAbilityTwoUsed {

    public SKeyPressAbilityTwoUsed() {}
    public SKeyPressAbilityTwoUsed(FriendlyByteBuf buffer) {}
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

        keyOfStarsDash(player,level,serverLevel, false);
        attendantOfMysteriesFlamingJump(player,level,serverLevel, false);
        handOfOrderDecree(player,level,serverLevel, false);
        // TODO(stage5): PrinceOfAbolition: princeOfAbolitionFlame(player,level,serverLevel,false);
        // TODO(stage5): PaleEmperor: paleEmperorDoor(player,serverLevel,false);

    }
}
