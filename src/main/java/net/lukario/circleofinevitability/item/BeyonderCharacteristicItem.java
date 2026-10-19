package net.lukario.circleofinevitability.item;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.CharacteristicConsumption;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A Beyonder Characteristic: right-click to consume it. Which Pathway and Sequence it is lives in its data
 * component (BeyonderCharacteristic); what consuming it does, and every chance, is in
 * blasphemy_slate/beyonder/CharacteristicConsumption.
 *
 * Make stacks with of(pathway, sequence): it sets the component, the name ("Key Of Stars Characteristic,
 * Sequence 5") and the description lines (Pathway, Sequence, "Right-click to consume").
 */
public class BeyonderCharacteristicItem extends Item {

    /** How many fit in one slot (only characteristics of the same Pathway and Sequence stack). */
    public static final int STACK_SIZE = 16;

    public BeyonderCharacteristicItem(Properties properties) {
        super(properties);
    }

    /** A stack of one characteristic of that Pathway and Sequence, named for it. */
    public static ItemStack of(String pathway, int sequence) {
        ItemStack stack = new ItemStack(ModItems.BEYONDER_CHARACTERISTIC.get());
        stack.set(ModDataComponents.CHARACTERISTIC.get(), new BeyonderCharacteristic(pathway, sequence));
        stack.set(DataComponents.ITEM_NAME,
                Component.translatable("item.circle_of_inevitability.beyonder_characteristic.named", pathway, sequence));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.translatable("tooltip.circle_of_inevitability.beyonder_characteristic.pathway", pathway)
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable("tooltip.circle_of_inevitability.beyonder_characteristic.sequence", sequence)
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable("tooltip.circle_of_inevitability.beyonder_characteristic.use")
                        .withStyle(ChatFormatting.DARK_PURPLE))));
        return stack;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;                      // the client only swings; the server decides
        }
        ItemStack stack = player.getItemInHand(hand);
        BeyonderCharacteristic characteristic = stack.get(ModDataComponents.CHARACTERISTIC.get());
        if (characteristic == null) {
            player.sendSystemMessage(Component.translatable("message.circle_of_inevitability.characteristic.empty"));
            return InteractionResult.FAIL;
        }
        // the stack shrinks inside consume, before a failed consumption kills the player
        return CharacteristicConsumption.consume(serverPlayer, sl, stack, characteristic)
                ? InteractionResult.SUCCESS
                : InteractionResult.FAIL;
    }
}
