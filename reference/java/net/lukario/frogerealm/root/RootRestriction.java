package net.lukario.frogerealm.root;

import java.util.EnumSet;
import java.util.Set;

/**
 * The things a root can take away. Mix them however you like:
 *
 *   Root.apply(target, 100, RootRestriction.MOVE);                          // can't walk, can still jump/fight
 *   Root.apply(target, 100, RootRestriction.MOVE, RootRestriction.JUMP);    // fully held in place
 *   Root.apply(target, 100, RootRestriction.MOVEMENT);                      // same + no teleporting (preset)
 *   Root.apply(target, 100, RootRestriction.EVERYTHING);                    // can't do anything
 */
public enum RootRestriction {
    /** Can't walk, swim, fly with elytra or be pushed around (knockback still lands, then stops). */
    MOVE,
    /** Can't jump. */
    JUMP,
    /** Can't deal damage: melee, projectiles, ability damage. */
    ATTACK,
    /** Can't right-click items: eating, drinking, bows, shields, throwing pearls... */
    USE_ITEMS,
    /** Can't break, place or right-click blocks (doors, chests, buttons...). */
    BLOCKS,
    /** Can't use soul abilities (the 1-7 ability keys). */
    ABILITIES,
    /** Can't teleport: ender pearls, chorus fruit, endermen/shulker teleports. */
    TELEPORT;

    // ---------- presets ----------

    /** Held in place: MOVE + JUMP + TELEPORT. */
    public static final Set<RootRestriction> MOVEMENT = EnumSet.of(MOVE, JUMP, TELEPORT);

    /** Can't hurt anyone or use anything, but can still move: ATTACK + USE_ITEMS + ABILITIES. */
    public static final Set<RootRestriction> DISARM = EnumSet.of(ATTACK, USE_ITEMS, ABILITIES);

    /** Can still move and fight, but no abilities or teleports. */
    public static final Set<RootRestriction> SILENCE = EnumSet.of(ABILITIES, TELEPORT);

    /** Nothing at all. */
    public static final Set<RootRestriction> EVERYTHING = EnumSet.allOf(RootRestriction.class);
}
