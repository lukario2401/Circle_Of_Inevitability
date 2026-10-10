package net.lukario.frogerealm.client.animation;

import net.lukario.frogerealm.client.animation.SwingAnimation.Easing;
import net.lukario.frogerealm.client.animation.SwingAnimation.Pose;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Every swing animation, by id. THE file to edit: to add a swing, write one definition below and register it;
 * to tune one, change its numbers. Nothing in the rendering code needs to change. Preview with /swinganim <id>
 * (or /swinganim <id> 4 to watch it four times slower). See docs/ANIMATIONS.md.
 *
 * All swings follow the same rhythm, in the style of the "punchy" combat mods:
 *   wind-up (fast, OUT) -> the cut (very fast, OUT_EXPO) -> follow-through back to rest (IN_OUT).
 * Keyframe times are fractions of the swing (0 = start, 1 = end), so changing the length keeps the rhythm.
 * Pose numbers: see SwingAnimation.Pose for what each one does and which sign goes where.
 */
public final class SwingAnimations {

    public static final String HORIZONTAL = "horizontal";
    public static final String HORIZONTAL_REVERSE = "horizontal_reverse";
    public static final String DIAGONAL_DOWN = "diagonal_down";
    public static final String DIAGONAL_UP = "diagonal_up";
    public static final String VERTICAL_DOWN = "vertical_down";
    public static final String STAB = "stab";

    /**
     * First person: where the sword sits while a swing plays, added to vanilla's resting spot (blocks, the camera
     * looks along -z). x: + = right (mirrored for left-handed players), so negative moves it toward the screen
     * centre; y: + = up; z: negative = further from the camera. Faded in during the wind-up and out during the
     * recovery, so a swing still starts and ends exactly where vanilla holds the item.
     */
    public static final float FIRST_PERSON_OFFSET_X = -0.18f;
    public static final float FIRST_PERSON_OFFSET_Y = 0.24f;
    public static final float FIRST_PERSON_OFFSET_Z = -0.25f;

    private static final Map<String, SwingAnimation> BY_ID = new LinkedHashMap<>();

    // How the first-person numbers work (see SwingAnimation.Pose for every sign):
    //  - itemRot(-110, yaw, roll) lays the blade level and pointing straight ahead; yaw then swings it around:
    //    yaw +80 points it to the left, -70 to the right (at +-70..90 its flat faces you, so it reads as a sword),
    //    and roll tilts that sweep on screen (+ = left end lower, so a right-to-left cut goes downhill).
    //  - item(x, y, z) is the hand's path on screen, on top of the global FIRST_PERSON_OFFSET above.
    //  - Third person: arm(pitch, yaw, roll) and body(yaw) move the player model; the held sword follows the arm.

    /** Flat cut from right to left at chest height. */
    public static final SwingAnimation HORIZONTAL_CUT = SwingAnimation.of(HORIZONTAL, 8)    // 8 ticks = 0.4 s
            .key(0.00f, Easing.LINEAR, Pose.REST)
            // wind-up: hand drawn to the right, blade level and pointing right;
            // arm low in front and swung outward, chest turned to the sword
            .key(0.25f, Easing.OUT, Pose.REST.item(0.10f, 0.02f, 0.05f).itemRot(-110, -70, -5)
                    .arm(-40, 65, 0).body(20))
            // the cut: the hand crosses to the left of the centre, the blade sweeps through "straight ahead" and
            // ends level, pointing left; arm across the body, chest turned away
            .key(0.60f, Easing.OUT_EXPO, Pose.REST.item(-0.30f, 0.0f, -0.06f).itemRot(-110, 80, 5)
                    .arm(-45, -65, 0).body(-25))
            // follow-through: settle back to vanilla's resting pose
            .key(1.00f, Easing.IN_OUT, Pose.REST);

    /**
     * Backhand: flat cut from left to right. Its own keyframes, not HORIZONTAL_CUT.mirrored(...): a mirrored copy
     * mirrors around the hand, which sits right of the centre, so its cut would end with the tip off the screen.
     */
    public static final SwingAnimation HORIZONTAL_REVERSE_CUT = SwingAnimation.of(HORIZONTAL_REVERSE, 8)
            .key(0.00f, Easing.LINEAR, Pose.REST)
            // wind-up: hand carried across to the left, blade level and pointing left (you see its other side);
            // arm across the body, chest turned away from the sword
            .key(0.25f, Easing.OUT, Pose.REST.item(-0.40f, 0.03f, 0.04f).itemRot(-110, 75, 8)
                    .arm(-40, -65, 0).body(-20))
            // the cut: hand back to the right, blade ends level, pointing right; arm swings outward
            .key(0.60f, Easing.OUT_EXPO, Pose.REST.item(0.04f, -0.01f, -0.06f).itemRot(-110, -70, -5)
                    .arm(-45, 65, 0).body(25))
            .key(1.00f, Easing.IN_OUT, Pose.REST);

    /** From high on the right down to low on the left (the sweep tilted by roll +32). */
    public static final SwingAnimation DIAGONAL_DOWN_CUT = SwingAnimation.of(DIAGONAL_DOWN, 8)
            .key(0.00f, Easing.LINEAR, Pose.REST)
            // wind-up: hand up and right, blade pointing up-right; arm high, chest turned to the sword
            .key(0.25f, Easing.OUT, Pose.REST.item(0.12f, 0.16f, 0.04f).itemRot(-110, -70, 32)
                    .arm(-140, 35, 0).body(15))
            // the cut: hand drops to the lower left, blade ends pointing down-left; arm low and across
            .key(0.60f, Easing.OUT_EXPO, Pose.REST.item(-0.26f, -0.04f, -0.06f).itemRot(-110, 80, 32)
                    .arm(-40, -45, 0).body(-20))
            .key(1.00f, Easing.IN_OUT, Pose.REST);

    /** Rising cut from low on the right up to high on the left (roll -20 -> -40: the sweep climbs as it goes). */
    public static final SwingAnimation DIAGONAL_UP_CUT = SwingAnimation.of(DIAGONAL_UP, 8)
            .key(0.00f, Easing.LINEAR, Pose.REST)
            // wind-up: hand low on the right, blade pointing down-right; arm low and out
            .key(0.25f, Easing.OUT, Pose.REST.item(0.12f, -0.02f, 0.04f).itemRot(-110, -70, -20)
                    .arm(-25, 40, 0).body(15))
            // the cut: hand rises to the upper left, blade ends pointing up-left; arm high and across
            .key(0.60f, Easing.OUT_EXPO, Pose.REST.item(-0.26f, 0.18f, -0.06f).itemRot(-110, 80, -40)
                    .arm(-145, -40, 0).body(-20))
            .key(1.00f, Easing.IN_OUT, Pose.REST);

    /** Overhead chop straight down the middle (roll 85 turns the sweep upright). */
    public static final SwingAnimation VERTICAL_DOWN_CUT = SwingAnimation.of(VERTICAL_DOWN, 8)
            .key(0.00f, Easing.LINEAR, Pose.REST)
            // wind-up: hand near the middle, blade raised upright (leaning a little forward so its flat stays lit);
            // arm overhead
            .key(0.25f, Easing.OUT, Pose.REST.item(-0.06f, 0.02f, 0.0f).itemRot(-110, -75, 85)
                    .arm(-165, 10, 0).body(5))
            // the cut: the blade falls forward until it points straight ahead, hand drops; arm ends low in front
            .key(0.55f, Easing.OUT_EXPO, Pose.REST.item(-0.14f, -0.08f, -0.08f).itemRot(-110, -12, 85)
                    .arm(-20, -10, 0).body(-5))
            .key(1.00f, Easing.IN_OUT, Pose.REST);

    /** Short pull back, then a straight thrust forward. */
    public static final SwingAnimation STAB_THRUST = SwingAnimation.of(STAB, 7)            // a bit quicker: 0.35 s
            .key(0.00f, Easing.LINEAR, Pose.REST)
            // pull back: blade levelled forward (pitch -80), hand drawn toward the camera; arm pulled back low
            .key(0.30f, Easing.OUT, Pose.REST.item(0.03f, -0.05f, 0.15f).itemRot(-80, 10, 0)
                    .arm(-5, 20, 0).body(10))
            // thrust: hand shoots forward (z -0.45); arm forward but low (-30), so the held blade points ahead
            .key(0.55f, Easing.OUT_EXPO, Pose.REST.item(-0.05f, 0.02f, -0.45f).itemRot(-85, 5, 0)
                    .arm(-30, -5, 0).body(-10))
            .key(1.00f, Easing.IN_OUT, Pose.REST);

    static {
        register(HORIZONTAL_CUT);
        register(HORIZONTAL_REVERSE_CUT);
        register(DIAGONAL_DOWN_CUT);
        register(DIAGONAL_UP_CUT);
        register(VERTICAL_DOWN_CUT);
        register(STAB_THRUST);
    }

    private SwingAnimations() {}

    /** Adds (or replaces) a swing under its id. Null is ignored. */
    public static void register(SwingAnimation animation) {
        if (animation != null && animation.id() != null) BY_ID.put(animation.id(), animation);
    }

    /** The swing with this id, or null if there is none (null = the caller plays vanilla). */
    public static SwingAnimation get(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    /** Every registered id, in registration order. */
    public static Set<String> ids() {
        return Collections.unmodifiableSet(BY_ID.keySet());
    }
}
