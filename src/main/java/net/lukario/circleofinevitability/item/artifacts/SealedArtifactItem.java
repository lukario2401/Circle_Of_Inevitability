package net.lukario.circleofinevitability.item.artifacts;

import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.AbilityLock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
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

import java.util.Arrays;

/** Shared item activation, player-owned cooldowns, saved drawback state and inventory hooks. */
public abstract class SealedArtifactItem extends Item {
    private final String artifactId;
    private final int cooldownTicks;

    protected SealedArtifactItem(Properties properties, String artifactId, int cooldownTicks, String... tooltipKeys) {
        super(properties.stacksTo(1).fireResistant().component(DataComponents.LORE, new ItemLore(
                Arrays.stream(tooltipKeys).map(key -> Component.translatable(key).withStyle(ChatFormatting.GRAY))
                        .map(line -> (Component) line).toList())));
        this.artifactId = artifactId;
        this.cooldownTicks = cooldownTicks;
    }

    /** Survives relog/death; multiple copies share the same state on each player. */
    public final CompoundTag state(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG);
        String key = "circle_of_inevitability_artifact_" + artifactId;
        CompoundTag state = persisted.getCompoundOrEmpty(key);
        persisted.put(key, state);
        data.put(ServerPlayer.PERSISTED_NBT_TAG, persisted);
        return state;
    }

    public final int cooldownLeft(ServerPlayer player) {
        long remaining = state(player).getLongOr("ready_at", 0L) - player.level().getGameTime();
        return (int) Math.clamp(remaining, 0L, cooldownTicks);
    }

    @Override
    public final InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer user)) return InteractionResult.SUCCESS;
        if (player.isSpectator() || !player.isAlive()) return InteractionResult.FAIL;
        if (AbilityLock.isLocked(player) || Root.has(player, RootRestriction.ABILITIES) || Root.has(player, RootRestriction.USE_ITEMS)) {
            player.sendSystemMessage(Component.translatable("message.circle_of_inevitability.artifact.locked"));
            return InteractionResult.FAIL;
        }
        int left = cooldownLeft(user);
        if (left > 0) {
            player.sendSystemMessage(Component.translatable("message.circle_of_inevitability.artifact.cooldown", (left + 19) / 20));
            player.getCooldowns().addCooldown(player.getItemInHand(hand), left);
            return InteractionResult.FAIL;
        }
        if (!activate(server, user, player.getItemInHand(hand))) return InteractionResult.FAIL;
        state(user).putLong("ready_at", server.getGameTime() + cooldownTicks);
        player.getCooldowns().addCooldown(player.getItemInHand(hand), cooldownTicks);
        return InteractionResult.SUCCESS;
    }

    /** Return false to refuse activation without consuming cooldown. Called on the server only. */
    protected abstract boolean activate(ServerLevel level, ServerPlayer player, ItemStack stack);

    /** Once per second per player, regardless of copy count. Also called when absent to decay lingering drawbacks. */
    public void carriedTick(ServerLevel level, ServerPlayer player, boolean carried) {}
}
