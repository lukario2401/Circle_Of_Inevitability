package net.lukario.circleofinevitability.item.artifacts.uniqueness;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * One downside of a Uniqueness, reusable by any uniqueness. It applies from minLevel() (a corruption level index,
 * 0 = the first) and reads its strength for the wielder's level (Wielder.at). Every hook runs on the server, and only
 * while it applies and the wielder suffers (carried or bound, not creative), except second(), which is also called
 * when it is put away so lingering effects can wear off.
 *
 * A new one implements only the hooks it needs, and keeps its saved values in Wielder.state() under its own keys.
 */
public interface UniquenessDrawback {

    /** The lowest corruption level it applies at (0 = from the start). */
    default int minLevel() { return 0; }

    default boolean appliesAt(int level) {
        return level >= minLevel();
    }

    /** One tooltip / guide line (the strongest form, read by the item's tooltip). */
    Component describe();

    /** Once per second for everyone who ever carried it, carried or not. Check Wielder.suffering() yourself. */
    default void second(Wielder wielder) {}

    /** An ability was cast through it. */
    default void cast(Wielder wielder, UniquePower power) {}

    /** Non-null refuses the cast, with this message. */
    default Component blocksCast(Wielder wielder, UniquePower power) { return null; }

    /** The wielder killed something. */
    default void killed(Wielder wielder, LivingEntity victim) {}

    /** The wielder is healed by 'amount' (not by a spell: Healing.apply calls are let through). Return the new amount. */
    default float healed(Wielder wielder, float amount) { return amount; }

    /** Non-null keeps them out of bed, with this message. */
    default Component refusesSleep(Wielder wielder) { return null; }

    /** The wielder opened a menu (a chest, a villager's trades...). True closes it at once. */
    default boolean refusesMenu(Wielder wielder, AbstractContainerMenu menu) { return false; }

    /** The wielder tries to act: attack 'target' (null for using an item or a block). True swallows the input. */
    default boolean swallows(Wielder wielder, Entity target) { return false; }

    /** The wielder died (while it applied). Called before the uniqueness forgets their corruption. */
    default void died(Wielder wielder) {}
}
