package net.lukario.circleofinevitability.item.artifacts;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * New SealedArtifactItem subclasses automatically receive inventory/equipment carrying ticks: one per artifact family
 * (artifactId) per second, given to the highest-ranked carried item of that family.
 */
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
        Map<String, ItemStack> carried = carriedByFamily(player);
        Set<String> ticked = new HashSet<>();
        for (var entry : ModItems.ITEMS.getEntries()) {
            if (!(entry.get() instanceof SealedArtifactItem artifact) || !ticked.add(artifact.artifactId())) continue;
            ItemStack stack = carried.getOrDefault(artifact.artifactId(), ItemStack.EMPTY);
            SealedArtifactItem owner = stack.isEmpty() ? artifact : (SealedArtifactItem) stack.getItem();
            owner.carriedTick(level, player, stack);
        }
    }

    /**
     * The carried stack of each artifact family: the highest rank wins, and on a tie the main hand, then the off hand,
     * then the inventory, then equipment. Items inside container items are not searched.
     */
    public static Map<String, ItemStack> carriedByFamily(ServerPlayer player) {
        Map<String, ItemStack> best = new LinkedHashMap<>();
        consider(best, player.getItemInHand(InteractionHand.MAIN_HAND));
        consider(best, player.getItemInHand(InteractionHand.OFF_HAND));
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++)
            consider(best, player.getInventory().getItem(slot));
        for (EquipmentSlot slot : EquipmentSlot.values()) consider(best, player.getItemBySlot(slot));
        return best;
    }

    /** The carried stack of one family, or ItemStack.EMPTY. */
    public static ItemStack carried(ServerPlayer player, String artifactId) {
        return carriedByFamily(player).getOrDefault(artifactId, ItemStack.EMPTY);
    }

    private static void consider(Map<String, ItemStack> best, ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof SealedArtifactItem artifact)) return;
        ItemStack before = best.get(artifact.artifactId());
        if (before == null || ((SealedArtifactItem) before.getItem()).rank() < artifact.rank())
            best.put(artifact.artifactId(), stack);
    }
}
