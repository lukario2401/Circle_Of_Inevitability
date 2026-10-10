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

    private static final Map<String, SwingAnimation> BY_ID = new LinkedHashMap<>();

    /** Flat cut from right to left at chest height. */
    public static final SwingAnimation HORIZONTAL_CUT = SwingAnimation.of(HORIZONTAL, 8)    // 8 ticks = 0.4 s
            .key(0.00f, Easing.LINEAR, Pose.REST)
            // wind-up: blade laid forward (pitch -60) and drawn back to the right (yaw -35), hand slightly up;
            // arm low in front (pitch -40 keeps the held blade pointing forward) and swung outward, chest turned to the sword
            .key(0.25f, Easing.OUT, Pose.REST.item(0.02f, 0.06f, 0.0f).itemRot(-60, -35, 0)
                    .arm(-40, 65, 0).body(20))
            // the cut: tip sweeps across to the left (yaw +70), hand follows left and slightly forward;
            // arm sweeps across the body, chest turns away
            .key(0.60f, Easing.OUT_EXPO, Pose.REST.item(-0.25f, 0.02f, -0.10f).itemRot(-75, 70, 0)
                    .arm(-45, -65, 0).body(-25))
            // follow-through: settle back to vanilla's resting pose
            .key(1.00f, Easing.IN_OUT, Pose.REST);

    /** The same cut from left to right (the mirror flag flips every yaw, roll and sideways offset). */
    public static final SwingAnimation HORIZONTAL_REVERSE_CUT = HORIZONTAL_CUT.mirrored(HORIZONTAL_REVERSE);

    /** From high on the right down to low on the left. The roll (+35) tilts the horizontal sweep into a diagonal. */
    public static final SwingAnimation DIAGONAL_DOWN_CUT = SwingAnimation.of(DIAGONAL_DOWN, 8)
            .key(0.00f, Easing.LINEAR, Pose.REST)
            // wind-up: hand up and right, blade drawn back to the right; arm high, chest turned to the sword
            .key(0.25f, Easing.OUT, Pose.REST.item(0.05f, 0.15f, 0.0f).itemRot(-50, -35, 35)
                    .arm(-140, 35, 0).body(15))
            // the cut: down and across to the left; arm ends low and across the body
            .key(0.60f, Easing.OUT_EXPO, Pose.REST.item(-0.22f, -0.02f, -0.08f).itemRot(-70, 65, 35)
                    .arm(-40, -45, 0).body(-20))
            .key(1.00f, Easing.IN_OUT, Pose.REST);

    /** Rising cut from low on the right up to high on the left (negative roll tilts the sweep the other way). */
    public static final SwingAnimation DIAGONAL_UP_CUT = SwingAnimation.of(DIAGONAL_UP, 8)
            .key(0.00f, Easing.LINEAR, Pose.REST)
            // wind-up: hand low and right, blade drawn back; arm low and out
            .key(0.25f, Easing.OUT, Pose.REST.item(0.05f, -0.10f, 0.0f).itemRot(-60, -35, -35)
                    .arm(-25, 40, 0).body(15))
            // the cut: up and across to the left; arm ends high and across
            .key(0.60f, Easing.OUT_EXPO, Pose.REST.item(-0.22f, 0.12f, -0.08f).itemRot(-70, 65, -35)
                    .arm(-145, -40, 0).body(-20))
            .key(1.00f, Easing.IN_OUT, Pose.REST);

    /** Overhead chop straight down. */
    public static final SwingAnimation VERTICAL_DOWN_CUT = SwingAnimation.of(VERTICAL_DOWN, 8)
            .key(0.00f, Easing.LINEAR, Pose.REST)
            // wind-up: hand raised and moved toward the middle of the screen, blade upright (pitch +10); arm overhead
            .key(0.25f, Easing.OUT, Pose.REST.item(-0.18f, 0.22f, 0.0f).itemRot(10, 25, 0)
                    .arm(-165, 10, 0).body(5))
            // the cut: blade chops forward and down (pitch -75) through the middle of the view, hand drops; arm ends low in front
            .key(0.55f, Easing.OUT_EXPO, Pose.REST.item(-0.20f, -0.12f, -0.10f).itemRot(-75, 25, 0)
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
