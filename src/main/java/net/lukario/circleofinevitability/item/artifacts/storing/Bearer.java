package net.lukario.circleofinevitability.item.artifacts.storing;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Who a drawback works on, right now.
 * stack: the carried stack of the family (ItemStack.EMPTY when it is put away). tier: its tier (0 when put away).
 * state: the artifact family's saved per-player state (SealedArtifactItem.state), shared by its drawbacks.
 */
public record Bearer(StoringArtifact artifact, ServerLevel level, ServerPlayer player, ItemStack stack, int tier,
                     CompoundTag state) {

    public boolean carried() {
        return !stack.isEmpty();
    }

    /** Creative and spectator players are spared the drawbacks (recovery still happens). */
    public boolean exempt() {
        return player.isCreative() || player.isSpectator();
    }

    /** values[tier - 1], the last value for higher tiers and the first for tier 0. */
    public static int at(int[] values, int tier) {
        return values[Math.clamp(tier - 1, 0, values.length - 1)];
    }

    /** values[tier - 1], the last value for higher tiers and the first for tier 0. */
    public static float at(float[] values, int tier) {
        return values[Math.clamp(tier - 1, 0, values.length - 1)];
    }

    public int at(int[] values) {
        return at(values, tier);
    }

    /** A per-artifact name for HUD timers and screen images: "creeping_hunger_thirst". */
    public String key(String suffix) {
        return artifact.id() + "_" + suffix;
    }
}
