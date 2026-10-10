package net.lukario.frogerealm.client.animation;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * Decides which swing animations a player gets: a condition on the player and the item in their main hand,
 * plus the swing ids played one after another (they cycle, and start over after a pause).
 * Register rules in SwingRules; the first rule that matches wins. No match = vanilla swing.
 *
 *   SwingRule.of((player, held) -> SwingRules.hasAspect(player, "Hand Of Order") && held.is(ItemTags.AXES),
 *           SwingRules.DEFAULT_RESET_TICKS, SwingAnimations.VERTICAL_DOWN, SwingAnimations.HORIZONTAL);
 *
 * Client-side only: it runs for the local player and for every other player this client sees.
 */
public interface SwingRule {

    /** True if this rule picks the swing for this player holding this stack. A throw counts as no match. */
    boolean matches(Player player, ItemStack held);

    /** The swing ids played one after another: 1st swing, 2nd swing... then from the start again. */
    List<String> sequence();

    /** Ticks without a new swing before the sequence starts over at its first id (20 ticks = 1 second). */
    default int resetTicks() {
        return SwingRules.DEFAULT_RESET_TICKS;
    }

    /** A rule from a condition, a reset time in ticks and the ids to cycle through. */
    static SwingRule of(BiPredicate<Player, ItemStack> when, int resetTicks, String... sequence) {
        List<String> ids = List.of(sequence);
        return new SwingRule() {
            @Override
            public boolean matches(Player player, ItemStack held) {
                return when.test(player, held);
            }

            @Override
            public List<String> sequence() {
                return ids;
            }

            @Override
            public int resetTicks() {
                return resetTicks;
            }
        };
    }
}
