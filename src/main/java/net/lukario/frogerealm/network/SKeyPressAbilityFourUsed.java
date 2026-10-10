package net.lukario.frogerealm.network;

import net.lukario.frogerealm.root.Root;
import net.lukario.frogerealm.root.RootRestriction;
import net.minecraft.network.chat.Component;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.network.CustomPayloadEvent;

// TODO(stage5): AttendantOfMysteries: import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.AttendantOfMysteries.attendantOfMysteriesAirCannon;
// TODO(stage5): HandOfOrder: import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.HandOfOrder.handOfOrderAbility4;
import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.KeyOfStars.keyOfStarsSeal;
// TODO(stage5): PaleEmperor: import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PaleEmperor.paleEmperorSpearSkull;
// TODO(stage5): PrinceOfAbolition: import static net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics.PrinceOfAbolition.princeOfAbolitionHammer;

public class SKeyPressAbilityFourUsed {

    public SKeyPressAbilityFourUsed() {}
    public SKeyPressAbilityFourUsed(FriendlyByteBuf buffer) {}
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

        keyOfStarsSeal(player,level,serverLevel,false);
        // TODO(stage5): AttendantOfMysteries: attendantOfMysteriesAirCannon(player,level,serverLevel,false);
        // TODO(stage5): PrinceOfAbolition: princeOfAbolitionHammer(player,level,serverLevel,false);
        // TODO(stage5): HandOfOrder: handOfOrderAbility4(player,level,serverLevel,false);
        // TODO(stage5): PaleEmperor: paleEmperorSpearSkull(player,level,serverLevel,false);
    }

}
