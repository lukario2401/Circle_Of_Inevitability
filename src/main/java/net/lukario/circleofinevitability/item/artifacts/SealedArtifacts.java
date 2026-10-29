package net.lukario.circleofinevitability.item.artifacts;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/** New SealedArtifactItem subclasses automatically receive inventory/equipment carrying ticks. */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SealedArtifacts {
    private SealedArtifacts() {}

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)
                || player.tickCount % 20 != 0 || !player.isAlive()) return;
        tickCarried(level, player);
    }

    public static void tickCarried(ServerLevel level, ServerPlayer player) {
        Set<SealedArtifactItem> carried = new HashSet<>();
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            add(carried, player.getInventory().getItem(slot));
        for (EquipmentSlot slot : EquipmentSlot.values()) add(carried, player.getItemBySlot(slot));
        for (var entry : ModItems.ITEMS.getEntries())
            if (entry.get() instanceof SealedArtifactItem artifact) artifact.carriedTick(level, player, carried.contains(artifact));
    }

    private static void add(Set<SealedArtifactItem> items, ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof SealedArtifactItem artifact) items.add(artifact);
    }
}
