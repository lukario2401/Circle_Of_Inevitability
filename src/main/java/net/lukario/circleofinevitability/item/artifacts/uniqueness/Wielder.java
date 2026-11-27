package net.lukario.circleofinevitability.item.artifacts.uniqueness;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Who a uniqueness's downside works on, right now.
 * stack: the carried uniqueness (ItemStack.EMPTY when it is not on them). corruption: 0..100. level: the index of the
 * corruption level (0 = the first, e.g. Low). state: the uniqueness's saved per-player state, shared by its downsides
 * (prefix your keys).
 */
public record Wielder(Uniqueness uniqueness, ServerLevel level, ServerPlayer player, ItemStack stack,
                      double corruption, int corruptionLevel, CompoundTag state) {

    public boolean carried() {
        return !stack.isEmpty();
    }

    /** Bound: it reached the level of no return and cannot be put away any more. */
    public boolean bound() {
        return state.getBooleanOr(Uniqueness.BOUND, false);
    }

    /** Creative and spectator players are spared the downsides (corruption still grows so the gates can be tried). */
    public boolean exempt() {
        return player.isCreative() || player.isSpectator();
    }

    /** True when the downsides act on them: carried (or bound to them) and not exempt. */
    public boolean suffering() {
        return (carried() || bound()) && !exempt();
    }

    public String levelName() {
        return uniqueness.level(corruptionLevel).name();
    }

    /** perLevel[corruption level], the last value for higher levels. */
    public int at(int[] perLevel) {
        return perLevel[Math.clamp(corruptionLevel, 0, perLevel.length - 1)];
    }

    public float at(float[] perLevel) {
        return perLevel[Math.clamp(corruptionLevel, 0, perLevel.length - 1)];
    }

    public double at(double[] perLevel) {
        return perLevel[Math.clamp(corruptionLevel, 0, perLevel.length - 1)];
    }

    /** A per-uniqueness name for HUD counters, timers and screen images: "red_priest_storm". */
    public String key(String suffix) {
        return uniqueness.id() + "_" + suffix;
    }
}
