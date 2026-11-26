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
import java.util.List;

/**
 * Shared item activation, player-owned cooldowns, saved drawback state and inventory hooks.
 *
 * Items that share an artifactId are one artifact "family" (the tiers of Creeping Hunger): they share saved state and
 * cooldown on each player, and only the highest-ranked carried one gets the once-per-second carrying tick.
 */
public abstract class SealedArtifactItem extends Item {
    private final String artifactId;
    private final int cooldownTicks;

    protected SealedArtifactItem(Properties properties, String artifactId, int cooldownTicks, String... tooltipKeys) {
        this(properties, artifactId, cooldownTicks,
                Arrays.stream(tooltipKeys).map(key -> (Component) Component.translatable(key)).toList());
    }

    /** Lore lines without their own colour are shown grey. */
    protected SealedArtifactItem(Properties properties, String artifactId, int cooldownTicks, List<Component> lore) {
        super(properties.stacksTo(1).fireResistant().component(DataComponents.LORE, new ItemLore(
                lore.stream().map(line -> line.getStyle().getColor() != null ? line
                        : (Component) line.copy().withStyle(ChatFormatting.GRAY)).toList())));
        this.artifactId = artifactId;
        this.cooldownTicks = cooldownTicks;
    }

    /** The family id: saved state, cooldowns and carrying ticks are shared by every item with this id. */
    public final String artifactId() { return artifactId; }

    /** Within a family, the carried item with the highest rank (its tier) receives carriedTick. */
    public int rank() { return 0; }

    /** The longest cooldown this artifact can start (cooldownFor is clamped to it). */
    public final int maxCooldownTicks() { return cooldownTicks; }

    /** Survives relog/death; multiple copies (and every item of the family) share the same state on each player. */
    public final CompoundTag state(ServerPlayer player) {
        return state(player, artifactId);
    }

    /** The saved state of any artifact family, for code that has no item at hand (cleansing, commands). */
    public static CompoundTag state(ServerPlayer player, String artifactId) {
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
        if (net.lukario.circleofinevitability.status.ArtifactLock.has(player) || AbilityLock.isLocked(player) || Root.has(player, RootRestriction.ABILITIES) || Root.has(player, RootRestriction.USE_ITEMS)) {
            player.sendSystemMessage(Component.translatable("message.circle_of_inevitability.artifact.locked"));
            return InteractionResult.FAIL;
        }
        ItemStack stack = player.getItemInHand(hand);
        // An alternate action (a menu, a selection) that ignores and never starts the cooldown.
        if (player.isShiftKeyDown() && sneakUse(server, user, stack)) return InteractionResult.SUCCESS;
        int left = cooldownLeft(user);
        if (left > 0) {
            player.sendSystemMessage(Component.translatable("message.circle_of_inevitability.artifact.cooldown", (left + 19) / 20));
            player.getCooldowns().addCooldown(stack, left);
            return InteractionResult.FAIL;
        }
        int cooldown = Math.clamp(cooldownFor(user, stack), 0, cooldownTicks);
        if (!activate(server, user, stack)) return InteractionResult.FAIL;
        if (cooldown > 0) {
            state(user).putLong("ready_at", server.getGameTime() + cooldown);
            player.getCooldowns().addCooldown(stack, cooldown);
        }
        return InteractionResult.SUCCESS;
    }

    /** Return false to refuse activation without consuming cooldown. Called on the server only. */
    protected abstract boolean activate(ServerLevel level, ServerPlayer player, ItemStack stack);

    /**
     * Sneak + right-click. Return true when it was handled: then it costs nothing and activate is not called.
     * The default (false) lets sneaking use the normal activation.
     */
    protected boolean sneakUse(ServerLevel level, ServerPlayer player, ItemStack stack) { return false; }

    /** The cooldown the next successful activation starts, decided before it runs. At most maxCooldownTicks. */
    protected int cooldownFor(ServerPlayer player, ItemStack stack) { return cooldownTicks; }

    /** Once per second per player, regardless of copy count. Also called when absent to decay lingering drawbacks. */
    public void carriedTick(ServerLevel level, ServerPlayer player, boolean carried) {}

    /**
     * The same tick, with the carried stack of this family (the highest-ranked one), or ItemStack.EMPTY when none is
     * carried. Override this one when the stack or its tier matters; the default calls carriedTick(boolean).
     */
    public void carriedTick(ServerLevel level, ServerPlayer player, ItemStack carried) {
        carriedTick(level, player, !carried.isEmpty());
    }
}
