package net.lukario.frogerealm.network;

import net.lukario.frogerealm.root.Root;
import net.lukario.frogerealm.root.RootRestriction;
import net.minecraft.network.chat.Component;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

// TODO(stage5): AttendantOfMysteries: import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesDamageTransfer;
// TODO(stage5): HandOfOrder: import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.HandOfOrder.handOfOrderEdict;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.KeyOfStars.keyOfStarsBuff;
// TODO(stage5): PaleEmperor: import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PaleEmperor.paleEmperorSerpent;
// TODO(stage5): PrinceOfAbolition: import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PrinceOfAbolition.princeOfAbolitionFrost;

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

        // rooted with ABILITIES = can't use any ability
        if (Root.has(player, RootRestriction.ABILITIES)) {
            player.sendOverlayMessage(Component.literal("You are bound by Order."));
            return;
        }

        keyOfStarsBuff(player,level,serverLevel,false);
        // TODO(stage5): AttendantOfMysteries: attendantOfMysteriesDamageTransfer(player,level,serverLevel,false);
        // TODO(stage5): HandOfOrder: handOfOrderEdict(player,level,serverLevel,false);
        // TODO(stage5): PrinceOfAbolition: princeOfAbolitionFrost(player,level,serverLevel,false);
        // TODO(stage5): PaleEmperor: paleEmperorSerpent(player,level,serverLevel,false);


    }
}
