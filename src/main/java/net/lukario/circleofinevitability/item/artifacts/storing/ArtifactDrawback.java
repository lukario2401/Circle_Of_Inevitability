package net.lukario.circleofinevitability.item.artifacts.storing;

import net.lukario.circleofinevitability.combat.AbilityCatalog;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

/**
 * One downside of a StoringArtifact, reusable by any artifact. A drawback applies from minTier() up and reads its
 * strength for the bearer's tier (Bearer.tier, see Bearer.at). Every hook runs on the server.
 *
 * The ready-made ones: FeedOrDieDrawback, CorruptionDrawback, AggroDrawback, BloodThirstDrawback,
 * DisorientationDrawback. A new one implements only the hooks it needs and keeps its saved values in Bearer.state()
 * under its own key names (the state is shared by every drawback of the artifact, so prefix them).
 */
public interface ArtifactDrawback {

    /** The lowest tier it applies at. */
    int minTier();

    default boolean appliesAt(int tier) {
        return tier >= minTier();
    }

    /** One tooltip line for this tier (only asked when appliesAt(tier)). */
    Component describe(int tier);

    /**
     * Once per second for every bearer who ever carried the family, carried or not (Bearer.carried(); the tier is 0
     * when it is put away). Recover lingering effects here when it is not carried.
     */
    default void second(Bearer bearer) {}

    /** The bearer killed something while carrying it (only when it applies at the carried tier). */
    default void killed(Bearer bearer, LivingEntity victim) {}

    /** A stored ability was cast from it (only when it applies at that tier). */
    default void cast(Bearer bearer, AbilityCatalog.Power power) {}

    /** Non-null refuses a cast, with this message (only asked when it applies at that tier). */
    default Component blocksCast(Bearer bearer) {
        return null;
    }

    /** The bearer was cleansed (status/Cleansing: cleanse abilities, milk). Carried or not. */
    default void cleansed(Bearer bearer) {}

    /** The bearer died. Clear what should not follow them into their next life. */
    default void bearerDied(Bearer bearer) {}
}
