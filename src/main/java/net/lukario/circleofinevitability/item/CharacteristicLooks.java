package net.lukario.circleofinevitability.item;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Every Beyonder Characteristic a player carries shows its Pathway and Sequence, however it was made: once a second
 * the server looks through each player's inventory and gives any characteristic that lacks them its name and
 * description (BeyonderCharacteristicItem.applyLooks). Stacks made with BeyonderCharacteristicItem.of already
 * have them; this is for /give and anything else that sets only the component.
 * A stack on the ground or in a chest gets them when a player picks it up.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID)
public final class CharacteristicLooks {

    private static final int CHECK_EVERY_TICKS = 20;

    private CharacteristicLooks() {}

    @SubscribeEvent
    public static void onCharacteristicLooksPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        Player player = event.player();
        if (player.level().isClientSide()) return;
        if (player.tickCount % CHECK_EVERY_TICKS != 0) return;

        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;
            BeyonderCharacteristic characteristic = stack.get(ModDataComponents.CHARACTERISTIC.get());
            if (characteristic != null) BeyonderCharacteristicItem.applyLooks(stack, characteristic);
        }
    }
}
