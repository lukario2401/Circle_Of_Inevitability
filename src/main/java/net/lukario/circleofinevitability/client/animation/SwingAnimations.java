package net.lukario.circleofinevitability.client.animation;

import net.lukario.circleofinevitability.client.animation.SwingAnimation.Easing;
import net.lukario.circleofinevitability.client.animation.SwingAnimation.Pose;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Every swing animation, by id. THE file to edit: to add a swing, write one definition below and register it;
 * to tune one, change its numbers. Nothing in the rendering code needs to change. Preview with /swinganim <id>
 * (or /swinganim <id> 4 to watch it four times slower). See docs/ANIMATIONS.md.
 *
 * Every swing has the same four phases (keyframe times are fractions of the swing, so a new length keeps them):
 *   wind-up   0 -> ~0.28, easing OUT       the anticipation: the blade is drawn back and slows into the coil
 *   cut       -> ~0.46,   easing OUT_EXPO  very fast, most of the travel in the first tick
 *   hold      -> ~0.64,   easing OUT       a short stop at the impact pose (2 to 3 ticks) with a little drift
 *   recovery  -> 1.00,    easing IN_OUT    smooth return to vanilla's resting pose
 * Pose numbers: see SwingAnimation.Pose for what each one does and which sign goes where.
 */
public final class SwingAnimations {

    // ---------------------------------------------------------------------------------------------------------------
    // GLOBAL TUNING: these apply to every swing. Nudge them here; no definition below needs to change.
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * First person: where the sword sits while a swing plays, added to vanilla's resting spot (blocks, the camera
     * looks along -z). x: + = right (mirrored for left-handed players), so negative moves it toward the screen
     * centre; y: + = up; z: negative = further from the camera. Faded in during the wind-up and out during the
     * recovery, so a swing still starts and ends exactly where vanilla holds the item.
     * Typical: x -0.10 .. -0.25, y 0.10 .. 0.25, z -0.10 .. -0.30.
     */
    public static final float FIRST_PERSON_OFFSET_X = -0.18f;
    public static final float FIRST_PERSON_OFFSET_Y = 0.24f;
    public static final float FIRST_PERSON_OFFSET_Z = -0.25f;

    /** First person: the sword's size during a swing (1 = vanilla), faded in and out like the offset. Typical 1.1 .. 1.4. */
    public static final float FIRST_PERSON_SCALE = 1.2f;

    /**
     * Playback speed of every swing, first and third person: 1 = the lengths written below, 0.8 = 25% slower,
     * 1.25 = 25% faster. Typical 0.75 .. 1.25. Visual only: attack speed, cooldown and damage never change.
     */
    public static final float SPEED = 1.0f;

    // ---------------------------------------------------------------------------------------------------------------

    public static final String HORIZONTAL = "horizontal";
    public static final String HORIZONTAL_REVERSE = "horizontal_reverse";
    public static final String DIAGONAL_DOWN = "diagonal_down";
    public static final String DIAGONAL_UP = "diagonal_up";
    public static final String VERTICAL_DOWN = "vertical_down";
    public static final String STAB = "stab";
    public static final String UPPERCUT = "uppercut";
    public static final String SPIN_SLASH = "spin_slash";

    private static final Map<String, SwingAnimation> BY_ID = new LinkedHashMap<>();

    // How the first-person numbers work (see SwingAnimation.Pose for every sign):
    //  - itemRot(-110, yaw, roll) lays the blade level and pointing straight ahead; yaw then swings it around:
    //    yaw +80 points it to the left, -70 to the right (at +-70..90 its flat faces you, so it reads as a sword),
    //    and roll tilts that sweep on screen (+ = left end lower, so a right-to-left cut goes downhill).
    //  - item(x, y, z) is the hand's path on screen, on top of the global FIRST_PERSON_OFFSET above.
    //  - Third person: arm(pitch, yaw, roll) and body(yaw) move the player model; the held sword follows the arm.

    /** Flat cut from right to left at chest height. */
    public static final SwingAnimation HORIZONTAL_CUT = SwingAnimation.of(HORIZONTAL, 14)       // 0.7 s
            .key(0.00f, Easing.LINEAR, Pose.REST)
            // wind-up: hand drawn to the right, blade level and pointing right;
            // arm low in front and swung outward, chest turned to the sword
            .key(0.28f, Easing.IN_OUT, Pose.REST.item(0.10f, 0.02f, 0.05f).itemRot(-110, -70, -5)
                    .arm(-40, 65, 0).body(20))
            // the cut: the hand crosses to the left of the centre, the blade sweeps through "straight ahead" and
            // ends level, pointing left; arm across the body, chest turned away
            .key(0.46f, Easing.OUT_EXPO, Pose.REST.item(-0.30f, 0.0f, -0.06f).itemRot(-110, 80, 5)
                    .arm(-45, -65, 0).body(-25))
            // hold: a few degrees of follow-through, then back to rest
            .key(0.64f, Easing.OUT, Pose.REST.item(-0.34f, -0.01f, -0.07f).itemRot(-110, 86, 6)
                    .arm(-45, -72, 0).body(-28))
            .key(1.00f, Easing.IN_OUT, Pose.REST);

    /**
     * Backhand: flat cut from left to right. Its own keyframes, not HORIZONTAL_CUT.mirrored(...): a mirrored copy
     * mirrors around the hand, which sits right of the centre, so its cut would end with the tip off the screen.
     */
    public static final SwingAnimation HORIZONTAL_REVERSE_CUT = SwingAnimation.of(HORIZONTAL_REVERSE, 14)
            .key(0.00f, Easing.LINEAR, Pose.REST)
            // wind-up: hand carried across to the left, blade level and pointing left (you see its other side);
            // arm across the body, chest turned away from the sword
            .key(0.28f, Easing.IN_OUT, Pose.REST.item(-0.40f, 0.03f, 0.04f).itemRot(-110, 75, 8)
                    .arm(-40, -65, 0).body(-20))
            // the cut: hand back to the right, blade ends level, pointing right; arm swings outward
            .key(0.46f, Easing.OUT_EXPO, Pose.REST.item(0.04f, -0.01f, -0.06f).itemRot(-110, -70, -5)
                    .arm(-45, 65, 0).body(25))
            .key(0.64f, Easing.OUT, Pose.REST.item(0.07f, -0.02f, -0.07f).itemRot(-110, -76, -6)
                    .arm(-45, 72, 0).body(28))
            .key(1.00f, Easing.IN_OUT, Pose.REST);

    /** From high on the right down to low on the left (the sweep tilted by roll +32). */
    public static final SwingAnimation DIAGONAL_DOWN_CUT = SwingAnimation.of(DIAGONAL_DOWN, 14)
            .key(0.00f, Easing.LINEAR, Pose.REST)
            // wind-up: hand up and right, blade pointing up-right; arm high, chest turned to the sword
            .key(0.28f, Easing.IN_OUT, Pose.REST.item(0.12f, 0.16f, 0.04f).itemRot(-110, -70, 32)
                    .arm(-140, 35, 0).body(15))
            // the cut: hand drops to the lower left, blade ends pointing down-left; arm low and across
            .key(0.46f, Easing.OUT_EXPO, Pose.REST.item(-0.26f, -0.04f, -0.06f).itemRot(-110, 80, 32)
                    .arm(-40, -45, 0).body(-20))
            .key(0.64f, Easing.OUT, Pose.REST.item(-0.29f, -0.06f, -0.07f).itemRot(-110, 86, 34)
                    .arm(-35, -50, 0).body(-22))
            .key(1.00f, Easing.IN_OUT, Pose.REST);

    /** Rising cut from low on the right up to high on the left (roll -20 -> -40: the sweep climbs as it goes). */
    public static final SwingAnimation DIAGONAL_UP_CUT = SwingAnimation.of(DIAGONAL_UP, 14)
            .key(0.00f, Easing.LINEAR, Pose.REST)
            // wind-up: hand low on the right, blade pointing down-right; arm low and out
            .key(0.28f, Easing.IN_OUT, Pose.REST.item(0.12f, -0.02f, 0.04f).itemRot(-110, -70, -20)
                    .arm(-25, 40, 0).body(15))
            // the cut: hand rises to the upper left, blade ends pointing up-left; arm high and across
            .key(0.46f, Easing.OUT_EXPO, Pose.REST.item(-0.26f, 0.18f, -0.06f).itemRot(-110, 80, -40)
                    .arm(-145, -40, 0).body(-20))
            .key(0.64f, Easing.OUT, Pose.REST.item(-0.29f, 0.20f, -0.07f).itemRot(-110, 86, -42)
                    .arm(-150, -45, 0).body(-22))
            .key(1.00f, Easing.IN_OUT, Pose.REST);

    /** Overhead chop straight down the middle (roll 85 turns the sweep upright). */
    public static final SwingAnimation VERTICAL_DOWN_CUT = SwingAnimation.of(VERTICAL_DOWN, 14)
            .key(0.00f, Easing.LINEAR, Pose.REST)
            // wind-up: hand near the middle, blade raised upright (leaning a little forward so its flat stays lit);
            // arm overhead
            .key(0.28f, Easing.IN_OUT, Pose.REST.item(-0.06f, 0.02f, 0.0f).itemRot(-110, -75, 85)
                    .arm(-165, 10, 0).body(5))
            // the cut: the blade falls forward until it points straight ahead, hand drops; arm ends low in front
            .key(0.46f, Easing.OUT_EXPO, Pose.REST.item(-0.14f, -0.08f, -0.08f).itemRot(-110, -12, 85)
                    .arm(-20, -10, 0).body(-5))
            .key(0.58f, Easing.OUT, Pose.REST.item(-0.15f, -0.10f, -0.09f).itemRot(-110, -6, 85)
                    .arm(-15, -12, 0).body(-6))
            .key(1.00f, Easing.IN_OUT, Pose.REST);

    /**
     * Pulled back to the hip, a fast thrust along the view, a small recoil. The blade points at the middle of the
     * screen the whole time (itemRot(-130, 29, -90): ahead, a little up and left, flat facing up) and only the
     * hand moves, mostly in depth (z).
     */
    public static final SwingAnimation STAB_THRUST = SwingAnimation.of(STAB, 12)                // 0.6 s
            .key(0.00f, Easing.LINEAR, Pose.REST)
            // pull back: hand low, right and close to you; arm drawn back behind the hip, chest turned to the sword
            .key(0.30f, Easing.IN_OUT, Pose.REST.item(0.08f, -0.10f, 0.14f).itemRot(-130, 29, -90)
                    .arm(20, 15, 0).body(20))
            // thrust: hand shoots forward toward the middle (z -0.30); arm forward and low so the blade points ahead
            .key(0.44f, Easing.OUT_EXPO, Pose.REST.item(-0.12f, 0.08f, -0.30f).itemRot(-130, 29, -90)
                    .arm(-25, -5, 0).body(-15))
            // recoil: back by 0.06, then hold
            .key(0.56f, Easing.OUT, Pose.REST.item(-0.11f, 0.07f, -0.24f).itemRot(-130, 29, -90)
                    .arm(-20, -5, 0).body(-12))
            .key(0.66f, Easing.LINEAR, Pose.REST.item(-0.11f, 0.07f, -0.23f).itemRot(-130, 29, -90)
                    .arm(-20, -5, 0).body(-12))
            .key(1.00f, Easing.IN_OUT, Pose.REST);

    /** From low on the left (the side away from the sword), rising up and across to finish high on the right. */
    public static final SwingAnimation UPPERCUT_CUT = SwingAnimation.of(UPPERCUT, 14)
            .key(0.00f, Easing.LINEAR, Pose.REST)
            // wind-up: hand low on the left, blade pointing down-left; arm low and across, chest turned away
            .key(0.30f, Easing.IN_OUT, Pose.REST.item(-0.30f, -0.04f, 0.04f).itemRot(-110, 75, 30)
                    .arm(-15, -45, 0).body(-20))
            // the cut: a steep climb (roll 30 -> 60) to the upper right, blade ends pointing up; arm raised and out
            .key(0.48f, Easing.OUT_EXPO, Pose.REST.item(0.02f, 0.22f, -0.06f).itemRot(-110, -75, 60)
                    .arm(-130, 35, 0).body(20))
            .key(0.64f, Easing.OUT, Pose.REST.item(0.03f, 0.25f, -0.07f).itemRot(-110, -82, 62)
                    .arm(-135, 38, 0).body(22))
            .key(1.00f, Easing.IN_OUT, Pose.REST);

    /**
     * A wide arc through nearly the whole width of the view: up from the right, over the middle, down to the left,
     * with the wrist rolling all the way (roll -22 -> +21), then a follow-through.
     */
    public static final SwingAnimation SPIN_SLASH_CUT = SwingAnimation.of(SPIN_SLASH, 16)       // 0.8 s
            .key(0.00f, Easing.LINEAR, Pose.REST)
            // wind-up: hand low on the right, blade pointing down-right; arm far out, chest turned to it
            .key(0.25f, Easing.IN_OUT, Pose.REST.item(0.10f, -0.04f, 0.06f).itemRot(-110, -62, -22)
                    .arm(-60, 85, -20).body(35))
            // top of the arc (speeding up): hand high in the middle, blade pointing ahead and up
            .key(0.36f, Easing.IN, Pose.REST.item(-0.08f, 0.18f, -0.10f).itemRot(-90, 0, 0)
                    .arm(-80, 0, 10).body(0))
            // end of the arc (slowing down): hand far left, blade pointing down-left; arm across, chest turned away
            .key(0.47f, Easing.OUT, Pose.REST.item(-0.44f, 0.02f, -0.04f).itemRot(-110, 84, 14)
                    .arm(-70, -70, 30).body(-35))
            // follow-through: the blade keeps turning back past the left side, then holds
            .key(0.56f, Easing.OUT, Pose.REST.item(-0.48f, 0.0f, -0.02f).itemRot(-110, 96, 20)
                    .arm(-60, -78, 36).body(-38))
            .key(0.70f, Easing.LINEAR, Pose.REST.item(-0.49f, -0.01f, -0.02f).itemRot(-110, 98, 21)
                    .arm(-58, -79, 36).body(-38))
            .key(1.00f, Easing.IN_OUT, Pose.REST);

    static {
        register(HORIZONTAL_CUT);
        register(HORIZONTAL_REVERSE_CUT);
        register(DIAGONAL_DOWN_CUT);
        register(DIAGONAL_UP_CUT);
        register(VERTICAL_DOWN_CUT);
        register(STAB_THRUST);
        register(UPPERCUT_CUT);
        register(SPIN_SLASH_CUT);
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
