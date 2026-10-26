package net.lukario.circleofinevitability.client.animation;

import net.lukario.circleofinevitability.client.animation.SwingAnimation.Easing;
import net.lukario.circleofinevitability.client.animation.SwingAnimation.Pose;

/** Prince-only choreography. Compact screen paths, restrained torso turns and a readable impact hold.
 * Shared Worm Of Time presets remain available independently. Preview with /swinganim abolition_<name>.
 */
public final class PrinceSwordSwings {
    private PrinceSwordSwings() {}

    public static final String IMPERIAL_DRAW = "abolition_imperial_draw";
    public static final String SILK_RETURN = "abolition_silk_return";
    public static final String ROYAL_LUNGE = "abolition_royal_lunge";
    public static final String GUILLOTINE = "abolition_guillotine";
    public static final String CROWN_LIFT = "abolition_crown_lift";
    public static final String ECLIPSE_SWEEP = "abolition_eclipse_sweep";
    public static final String OBLIQUE_RISE = "abolition_oblique_rise";
    public static final String SOVEREIGN_FALL = "abolition_sovereign_fall";
    public static final String NEEDLE_DECREE = "abolition_needle_decree";
    public static final String LOW_REPRISAL = "abolition_low_reprisal";
    public static final String ASCENDANT_REAP = "abolition_ascendant_reap";
    public static final String ABOLITION_FINISH = "abolition_final_decree";

    /** Measured chest-height draw cut. */
    public static final SwingAnimation IMPERIAL_DRAW_CUT = SwingAnimation.of(IMPERIAL_DRAW, 14)
            .key(0f, Easing.LINEAR, Pose.REST)
            .key(0.28f, Easing.IN_OUT, Pose.REST.item(0.06f, 0.03f, 0.05f).itemRot(-110f, -65f, -8f)
                    .arm(-42f, 52f, -4f).body(16f))
            .key(0.48f, Easing.OUT, Pose.REST.item(-0.25f, 0.02f, -0.09f).itemRot(-110f, 72f, 2f)
                    .arm(-48f, -52f, 6f).body(-18f))
            .key(0.66f, Easing.OUT, Pose.REST.item(-0.264f, 0.012f, -0.09f).itemRot(-110f, 75f, 2f)
                    .arm(-48f, -52f, 6f).body(-20f))
            .key(1f, Easing.IN_OUT, Pose.REST);

    /** Compact backhand returning across the guard. */
    public static final SwingAnimation SILK_RETURN_CUT = SwingAnimation.of(SILK_RETURN, 14)
            .key(0f, Easing.LINEAR, Pose.REST)
            .key(0.28f, Easing.IN_OUT, Pose.REST.item(-0.29f, 0.04f, 0.04f).itemRot(-108f, 70f, 6f)
                    .arm(-45f, -52f, 5f).body(-16f))
            .key(0.48f, Easing.OUT, Pose.REST.item(0.01f, 0.05f, -0.08f).itemRot(-112f, -66f, -4f)
                    .arm(-50f, 52f, -5f).body(18f))
            .key(0.66f, Easing.OUT, Pose.REST.item(0.016f, 0.042f, -0.08f).itemRot(-112f, -69f, -4f)
                    .arm(-50f, 52f, -5f).body(20f))
            .key(1f, Easing.IN_OUT, Pose.REST);

    /** Level thrust with a short recoil. */
    public static final SwingAnimation ROYAL_LUNGE_CUT = SwingAnimation.of(ROYAL_LUNGE, 13)
            .key(0f, Easing.LINEAR, Pose.REST)
            .key(0.28f, Easing.IN_OUT, Pose.REST.item(0.03f, -0.06f, 0.12f).itemRot(-130f, 29f, -90f)
                    .arm(12f, 12f, -4f).body(14f))
            .key(0.48f, Easing.OUT, Pose.REST.item(-0.13f, 0.05f, -0.28f).itemRot(-130f, 29f, -90f)
                    .arm(-28f, -8f, 2f).body(-12f))
            .key(0.57f, Easing.OUT, Pose.REST.item(-0.13f, 0.05f, -0.23f).itemRot(-130f, 29f, -90f)
                    .arm(-28f, -8f, 2f).body(-12f))
            .key(0.66f, Easing.OUT, Pose.REST.item(-0.124f, 0.042f, -0.23f).itemRot(-130f, 32f, -90f)
                    .arm(-28f, -8f, 2f).body(-14f))
            .key(1f, Easing.IN_OUT, Pose.REST);

    /** High diagonal descending into a low guard. */
    public static final SwingAnimation GUILLOTINE_CUT = SwingAnimation.of(GUILLOTINE, 16)
            .key(0f, Easing.LINEAR, Pose.REST)
            .key(0.28f, Easing.IN_OUT, Pose.REST.item(0.04f, 0.14f, 0.04f).itemRot(-110f, -66f, 48f)
                    .arm(-145f, 28f, -8f).body(17f))
            .key(0.48f, Easing.OUT, Pose.REST.item(-0.25f, -0.08f, -0.1f).itemRot(-112f, 72f, 48f)
                    .arm(-30f, -42f, 10f).body(-20f))
            .key(0.66f, Easing.OUT, Pose.REST.item(-0.264f, -0.088f, -0.1f).itemRot(-112f, 75f, 48f)
                    .arm(-30f, -42f, 10f).body(-22f))
            .key(1f, Easing.IN_OUT, Pose.REST);

    /** Backhand lift from the low guard. */
    public static final SwingAnimation CROWN_LIFT_CUT = SwingAnimation.of(CROWN_LIFT, 15)
            .key(0f, Easing.LINEAR, Pose.REST)
            .key(0.28f, Easing.IN_OUT, Pose.REST.item(-0.27f, -0.07f, 0.04f).itemRot(-112f, 68f, 30f)
                    .arm(-22f, -42f, 6f).body(-16f))
            .key(0.48f, Easing.OUT, Pose.REST.item(-0.01f, 0.17f, -0.1f).itemRot(-108f, -68f, 54f)
                    .arm(-125f, 32f, -8f).body(18f))
            .key(0.66f, Easing.OUT, Pose.REST.item(-0.004f, 0.182f, -0.1f).itemRot(-108f, -71f, 54f)
                    .arm(-125f, 32f, -8f).body(20f))
            .key(1f, Easing.IN_OUT, Pose.REST);

    /** Rounded sweep with a controlled overhead passing pose. */
    public static final SwingAnimation ECLIPSE_SWEEP_CUT = SwingAnimation.of(ECLIPSE_SWEEP, 17)
            .key(0f, Easing.LINEAR, Pose.REST)
            .key(0.28f, Easing.IN_OUT, Pose.REST.item(0.04f, -0.04f, 0.05f).itemRot(-112f, -64f, -18f)
                    .arm(-55f, 60f, -12f).body(23f))
            .key(0.37f, Easing.IN, Pose.REST.item(-0.11f, 0.16f, -0.12f).itemRot(-105f, 12f, 8f)
                    .arm(-100f, 0f, 4f).body(0f))
            .key(0.48f, Easing.OUT, Pose.REST.item(-0.31f, 0.01f, -0.08f).itemRot(-110f, 78f, 16f)
                    .arm(-65f, -55f, 18f).body(-25f))
            .key(0.66f, Easing.OUT, Pose.REST.item(-0.324f, 0.002f, -0.08f).itemRot(-110f, 81f, 16f)
                    .arm(-65f, -55f, 18f).body(-27f))
            .key(1f, Easing.IN_OUT, Pose.REST);

    /** Shallow forehand rising diagonally. */
    public static final SwingAnimation OBLIQUE_RISE_CUT = SwingAnimation.of(OBLIQUE_RISE, 15)
            .key(0f, Easing.LINEAR, Pose.REST)
            .key(0.28f, Easing.IN_OUT, Pose.REST.item(0.05f, -0.08f, 0.05f).itemRot(-112f, -66f, -25f)
                    .arm(-25f, 42f, -6f).body(16f))
            .key(0.48f, Easing.OUT, Pose.REST.item(-0.25f, 0.13f, -0.1f).itemRot(-108f, 72f, -36f)
                    .arm(-125f, -35f, 8f).body(-18f))
            .key(0.66f, Easing.OUT, Pose.REST.item(-0.264f, 0.142f, -0.1f).itemRot(-108f, 75f, -36f)
                    .arm(-125f, -35f, 8f).body(-20f))
            .key(1f, Easing.IN_OUT, Pose.REST);

    /** Overhead cut angled across the center to keep the blade visible. */
    public static final SwingAnimation SOVEREIGN_FALL_CUT = SwingAnimation.of(SOVEREIGN_FALL, 16)
            .key(0f, Easing.LINEAR, Pose.REST)
            .key(0.28f, Easing.IN_OUT, Pose.REST.item(-0.04f, 0.14f, 0.04f).itemRot(-108f, -62f, 76f)
                    .arm(-155f, 18f, -4f).body(12f))
            .key(0.48f, Easing.OUT, Pose.REST.item(-0.21f, -0.09f, -0.12f).itemRot(-112f, 62f, 70f)
                    .arm(-30f, -28f, 8f).body(-14f))
            .key(0.66f, Easing.OUT, Pose.REST.item(-0.224f, -0.098f, -0.12f).itemRot(-112f, 65f, 70f)
                    .arm(-30f, -28f, 8f).body(-16f))
            .key(1f, Easing.IN_OUT, Pose.REST);

    /** High-guard thrust, distinct from the low lunge. */
    public static final SwingAnimation NEEDLE_DECREE_CUT = SwingAnimation.of(NEEDLE_DECREE, 14)
            .key(0f, Easing.LINEAR, Pose.REST)
            .key(0.28f, Easing.IN_OUT, Pose.REST.item(-0.13f, 0.13f, 0.09f).itemRot(-126f, 24f, -72f)
                    .arm(-105f, -18f, 8f).body(-12f))
            .key(0.48f, Easing.OUT, Pose.REST.item(-0.16f, 0.07f, -0.3f).itemRot(-130f, 26f, -82f)
                    .arm(-38f, -4f, 0f).body(10f))
            .key(0.57f, Easing.OUT, Pose.REST.item(-0.16f, 0.07f, -0.25f).itemRot(-130f, 26f, -82f)
                    .arm(-38f, -4f, 0f).body(10f))
            .key(0.66f, Easing.OUT, Pose.REST.item(-0.154f, 0.062f, -0.25f).itemRot(-130f, 29f, -82f)
                    .arm(-38f, -4f, 0f).body(12f))
            .key(1f, Easing.IN_OUT, Pose.REST);

    /** Low backhand cutting out of the thrust recovery. */
    public static final SwingAnimation LOW_REPRISAL_CUT = SwingAnimation.of(LOW_REPRISAL, 14)
            .key(0f, Easing.LINEAR, Pose.REST)
            .key(0.28f, Easing.IN_OUT, Pose.REST.item(-0.28f, -0.06f, 0.04f).itemRot(-112f, 68f, -12f)
                    .arm(-30f, -48f, 8f).body(-16f))
            .key(0.48f, Easing.OUT, Pose.REST.item(0.01f, -0.03f, -0.1f).itemRot(-110f, -66f, -18f)
                    .arm(-35f, 48f, -6f).body(18f))
            .key(0.66f, Easing.OUT, Pose.REST.item(0.016f, -0.038f, -0.1f).itemRot(-110f, -69f, -18f)
                    .arm(-35f, 48f, -6f).body(20f))
            .key(1f, Easing.IN_OUT, Pose.REST);

    /** Steep forehand lift with a long curved follow-through. */
    public static final SwingAnimation ASCENDANT_REAP_CUT = SwingAnimation.of(ASCENDANT_REAP, 16)
            .key(0f, Easing.LINEAR, Pose.REST)
            .key(0.28f, Easing.IN_OUT, Pose.REST.item(0.04f, -0.1f, 0.05f).itemRot(-112f, -68f, -32f)
                    .arm(-22f, 50f, -10f).body(20f))
            .key(0.48f, Easing.OUT, Pose.REST.item(-0.26f, 0.16f, -0.11f).itemRot(-108f, 74f, -52f)
                    .arm(-140f, -40f, 12f).body(-22f))
            .key(0.66f, Easing.OUT, Pose.REST.item(-0.274f, 0.172f, -0.11f).itemRot(-108f, 77f, -52f)
                    .arm(-140f, -40f, 12f).body(-24f))
            .key(1f, Easing.IN_OUT, Pose.REST);

    /** Broad diagonal execution with a deliberate impact hold. */
    public static final SwingAnimation ABOLITION_FINISH_CUT = SwingAnimation.of(ABOLITION_FINISH, 18)
            .key(0f, Easing.LINEAR, Pose.REST)
            .key(0.28f, Easing.IN_OUT, Pose.REST.item(0.04f, 0.15f, 0.06f).itemRot(-110f, -68f, 58f)
                    .arm(-150f, 32f, -10f).body(24f))
            .key(0.37f, Easing.IN, Pose.REST.item(-0.11f, 0.16f, -0.12f).itemRot(-105f, 12f, 8f)
                    .arm(-100f, 0f, 4f).body(0f))
            .key(0.48f, Easing.OUT, Pose.REST.item(-0.29f, -0.08f, -0.12f).itemRot(-112f, 76f, 50f)
                    .arm(-35f, -48f, 14f).body(-25f))
            .key(0.66f, Easing.OUT, Pose.REST.item(-0.304f, -0.088f, -0.12f).itemRot(-112f, 79f, 50f)
                    .arm(-35f, -48f, 14f).body(-27f))
            .key(1f, Easing.IN_OUT, Pose.REST);

    static void registerAll() {
        SwingAnimations.register(IMPERIAL_DRAW_CUT);
        SwingAnimations.register(SILK_RETURN_CUT);
        SwingAnimations.register(ROYAL_LUNGE_CUT);
        SwingAnimations.register(GUILLOTINE_CUT);
        SwingAnimations.register(CROWN_LIFT_CUT);
        SwingAnimations.register(ECLIPSE_SWEEP_CUT);
        SwingAnimations.register(OBLIQUE_RISE_CUT);
        SwingAnimations.register(SOVEREIGN_FALL_CUT);
        SwingAnimations.register(NEEDLE_DECREE_CUT);
        SwingAnimations.register(LOW_REPRISAL_CUT);
        SwingAnimations.register(ASCENDANT_REAP_CUT);
        SwingAnimations.register(ABOLITION_FINISH_CUT);
    }
}
