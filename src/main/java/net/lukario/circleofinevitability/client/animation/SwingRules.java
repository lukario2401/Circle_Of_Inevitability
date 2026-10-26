package net.lukario.circleofinevitability.client.animation;

import net.lukario.circleofinevitability.client.ClientPathwayData;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Every swing rule, checked in order (the first that matches decides). To give another class or weapon its own
 * swings, add one SwingRule constant here and one register(...) line. See docs/ANIMATIONS.md.
 */
public final class SwingRules {

    /** 30 ticks = 1.5 seconds without swinging, then the sequence starts over. */
    public static final int DEFAULT_RESET_TICKS = 30;

    /**
     * Worm Of Time with any sword (the minecraft:swords item tag): all eight swings, ordered so each one travels a
     * different way from the one before (right-to-left, forward, down-left, up-right, left-to-right, down, up-left,
     * the wide sweep) and no swing follows itself, also when the cycle wraps around. Starts over after the pause.
     */
    public static final SwingRule WORM_OF_TIME_SWORD = SwingRule.of(
            (player, held) -> hasPathway(player, "Worm Of Time") && held.is(ItemTags.SWORDS),
            DEFAULT_RESET_TICKS,
            SwingAnimations.HORIZONTAL, SwingAnimations.STAB, SwingAnimations.DIAGONAL_DOWN, SwingAnimations.UPPERCUT,
            SwingAnimations.HORIZONTAL_REVERSE, SwingAnimations.VERTICAL_DOWN, SwingAnimations.DIAGONAL_UP,
            SwingAnimations.SPIN_SLASH);

    /** A twelve-swing imperial sword chain; resets after two seconds. */
    public static final SwingRule PRINCE_OF_ABOLITION_SWORD = SwingRule.of(
            (player, held) -> hasPathway(player, "Prince Of Abolition") && held.is(ItemTags.SWORDS),
            40,
            SwingAnimations.HORIZONTAL, SwingAnimations.HORIZONTAL_REVERSE, SwingAnimations.STAB,
            SwingAnimations.DIAGONAL_DOWN, SwingAnimations.UPPERCUT, SwingAnimations.SPIN_SLASH,
            SwingAnimations.DIAGONAL_UP, SwingAnimations.VERTICAL_DOWN, SwingAnimations.STAB,
            SwingAnimations.HORIZONTAL_REVERSE, SwingAnimations.UPPERCUT, SwingAnimations.SPIN_SLASH);

    private static final List<SwingRule> RULES = new ArrayList<>();

    static {
        register(WORM_OF_TIME_SWORD);
        register(PRINCE_OF_ABOLITION_SWORD);
    }

    private SwingRules() {}

    /** Adds a rule after the existing ones. Null is ignored. */
    public static void register(SwingRule rule) {
        if (rule != null) RULES.add(rule);
    }

    /** The first rule that matches this player and main-hand stack, or null (null = vanilla swing). Never throws. */
    public static SwingRule find(Player player, ItemStack held) {
        if (player == null || held == null) return null;
        for (SwingRule rule : RULES) {
            try {
                if (rule.matches(player, held) && !rule.sequence().isEmpty()) return rule;
            } catch (RuntimeException e) {
                // a broken rule never stops the others (or the game); it just doesn't match
            }
        }
        return null;
    }

    /** True if this client believes the player has that pathway (exact name, as used by /soul setPathway). */
    public static boolean hasPathway(Player player, String pathway) {
        return pathway.equals(pathwayOf(player));
    }

    /**
     * The player's pathway as this client knows it: the local player's from ClientPathwayData, other players'
     * from PlayerPathwayCache (both synced by PathwaySync). "none" if unknown.
     */
    public static String pathwayOf(Player player) {
        if (player == Minecraft.getInstance().player) return ClientPathwayData.getPathway();
        return PlayerPathwayCache.get(player.getUUID());
    }
}
