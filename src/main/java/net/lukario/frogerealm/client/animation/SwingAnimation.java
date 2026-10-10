package net.lukario.frogerealm.client.animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * One custom swing: a list of keyframes (poses at points in time), how long it lasts and whether it is mirrored.
 * Pure data, no rendering: FirstPersonSwing and ThirdPersonSwing read the pose this returns for the current moment.
 * Define new ones in SwingAnimations (the one file to edit), see docs/ANIMATIONS.md.
 *
 *   SwingAnimation.of("my_cut", 8)                                     // id, length in ticks (20 ticks = 1 second)
 *           .key(0.00f, Easing.LINEAR, Pose.REST)                      // starts where vanilla holds the item
 *           .key(0.30f, Easing.OUT, Pose.REST.itemRot(-60, -50, 0))    // wind-up, reached 30% of the way in
 *           .key(0.60f, Easing.OUT_EXPO, Pose.REST.itemRot(-60, 70, 0))// the cut
 *           .key(1.00f, Easing.IN_OUT, Pose.REST);                     // back to rest
 *
 * The easing on a keyframe shapes the movement that ENDS at that keyframe.
 */
public final class SwingAnimation {

    /** How a movement between two keyframes speeds up or slows down. */
    public enum Easing {
        /** constant speed */
        LINEAR,
        /** starts slow, ends fast */
        IN,
        /** starts fast, ends slow (good for wind-ups) */
        OUT,
        /** slow, fast, slow (good for settling back to rest) */
        IN_OUT,
        /** starts very fast, then glides in: the snappy "cut" */
        OUT_EXPO,
        /** fast, overshoots a little, then settles */
        OUT_BACK;

        /** Maps 0..1 to 0..1 (OUT_BACK goes slightly past 1 on the way). */
        public float apply(float t) {
            if (t <= 0f) return 0f;
            if (t >= 1f) return 1f;
            return switch (this) {
                case LINEAR -> t;
                case IN -> t * t * t;
                case OUT -> 1f - (1f - t) * (1f - t) * (1f - t);
                case IN_OUT -> t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
                case OUT_EXPO -> 1f - (float) Math.pow(2, -10 * t);
                case OUT_BACK -> {
                    float c1 = 1.70158f, c3 = c1 + 1f, u = t - 1f;
                    yield 1f + c3 * u * u * u + c1 * u * u;
                }
            };
        }
    }

    /**
     * Where everything is at one moment, as offsets from vanilla's resting pose (REST = all zero, scale 1 = vanilla).
     * Written for the right hand; the left hand and mirrored swings flip the signs that need flipping.
     *
     * First person (the item in front of the camera, pivot = the hand):
     *   item x/y/z      move it, in blocks: +x right, +y up, +z toward the camera
     *   item pitch      tips the blade: negative = forward/down (vanilla's chop goes to about -80)
     *   item yaw        sweeps around the vertical: positive = the tip moves to the left
     *   item roll       tilts the whole sweep: positive = counter-clockwise on screen (makes a sweep diagonal)
     *   item scale      1 = normal size
     * Third person (the player model, seen in F5 and by other players):
     *   arm pitch       raises the arm: -90 = straight forward, -170 = overhead (added to half the walking swing).
     *                   The held item sticks out of the fist at a right angle: with the arm at -30..-45 the blade
     *                   points forward, at -90 it points up. Flat cuts and stabs keep the arm low.
     *   arm yaw         positive = the arm swings outward (right arm to the right), negative = across the body
     *   arm roll        rolls the arm around the forward axis, which turns the held blade
     *   body yaw        twists the upper body: positive = chest turns toward the sword arm
     * Rotations are degrees. In first person they are applied roll, then yaw, then pitch.
     */
    public static final class Pose {
        public static final Pose REST = new Pose(0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0);

        public final float itemX, itemY, itemZ, itemPitch, itemYaw, itemRoll, itemScale;
        public final float armPitch, armYaw, armRoll, bodyYaw;

        private Pose(float itemX, float itemY, float itemZ, float itemPitch, float itemYaw, float itemRoll, float itemScale,
                     float armPitch, float armYaw, float armRoll, float bodyYaw) {
            this.itemX = itemX; this.itemY = itemY; this.itemZ = itemZ;
            this.itemPitch = itemPitch; this.itemYaw = itemYaw; this.itemRoll = itemRoll; this.itemScale = itemScale;
            this.armPitch = armPitch; this.armYaw = armYaw; this.armRoll = armRoll; this.bodyYaw = bodyYaw;
        }

        /** Same pose, first-person item moved (blocks). */
        public Pose item(float x, float y, float z) {
            return new Pose(x, y, z, itemPitch, itemYaw, itemRoll, itemScale, armPitch, armYaw, armRoll, bodyYaw);
        }

        /** Same pose, first-person item turned (degrees). */
        public Pose itemRot(float pitch, float yaw, float roll) {
            return new Pose(itemX, itemY, itemZ, pitch, yaw, roll, itemScale, armPitch, armYaw, armRoll, bodyYaw);
        }

        /** Same pose, first-person item scaled (1 = normal). */
        public Pose scale(float scale) {
            return new Pose(itemX, itemY, itemZ, itemPitch, itemYaw, itemRoll, scale, armPitch, armYaw, armRoll, bodyYaw);
        }

        /** Same pose, third-person sword arm turned (degrees). */
        public Pose arm(float pitch, float yaw, float roll) {
            return new Pose(itemX, itemY, itemZ, itemPitch, itemYaw, itemRoll, itemScale, pitch, yaw, roll, bodyYaw);
        }

        /** Same pose, third-person upper body twisted (degrees). */
        public Pose body(float yaw) {
            return new Pose(itemX, itemY, itemZ, itemPitch, itemYaw, itemRoll, itemScale, armPitch, armYaw, armRoll, yaw);
        }

        /** The same pose for the other side: left/right offsets, yaws and rolls change sign. */
        public Pose mirrored() {
            return new Pose(-itemX, itemY, itemZ, itemPitch, -itemYaw, -itemRoll, itemScale, armPitch, -armYaw, -armRoll, -bodyYaw);
        }

        /** Between a (t = 0) and b (t = 1); t may go a little past 1 (OUT_BACK). */
        public static Pose lerp(Pose a, Pose b, float t) {
            return new Pose(
                    a.itemX + (b.itemX - a.itemX) * t, a.itemY + (b.itemY - a.itemY) * t, a.itemZ + (b.itemZ - a.itemZ) * t,
                    a.itemPitch + (b.itemPitch - a.itemPitch) * t, a.itemYaw + (b.itemYaw - a.itemYaw) * t,
                    a.itemRoll + (b.itemRoll - a.itemRoll) * t, a.itemScale + (b.itemScale - a.itemScale) * t,
                    a.armPitch + (b.armPitch - a.armPitch) * t, a.armYaw + (b.armYaw - a.armYaw) * t,
                    a.armRoll + (b.armRoll - a.armRoll) * t, a.bodyYaw + (b.bodyYaw - a.bodyYaw) * t);
        }
    }

    /** A pose reached at 'at' (0 = start, 1 = end of the swing), moving into it with 'easing'. */
    public record Keyframe(float at, Easing easing, Pose pose) {}

    private final String id;
    private final int ticks;
    private final boolean mirror;
    private final List<Keyframe> keyframes;

    private SwingAnimation(String id, int ticks, boolean mirror, List<Keyframe> keyframes) {
        this.id = id;
        this.ticks = Math.max(1, ticks);
        this.mirror = mirror;
        this.keyframes = keyframes;
    }

    /** A new, empty swing. ticks = how long it lasts (vanilla's swing is 6). */
    public static SwingAnimation of(String id, int ticks) {
        return new SwingAnimation(id, ticks, false, List.of());
    }

    /** A copy with one more keyframe. Keyframes may be added in any order; they are sorted by time. */
    public SwingAnimation key(float at, Easing easing, Pose pose) {
        List<Keyframe> list = new ArrayList<>(keyframes);
        list.add(new Keyframe(Math.max(0f, Math.min(1f, at)), easing == null ? Easing.LINEAR : easing, pose == null ? Pose.REST : pose));
        list.sort((a, b) -> Float.compare(a.at(), b.at()));
        return new SwingAnimation(id, ticks, mirror, Collections.unmodifiableList(list));
    }

    /** A copy under another id that plays the other way round (right-to-left becomes left-to-right). */
    public SwingAnimation mirrored(String newId) {
        return new SwingAnimation(newId, ticks, !mirror, keyframes);
    }

    /** A copy with another length in ticks. */
    public SwingAnimation ticks(int newTicks) {
        return new SwingAnimation(id, newTicks, mirror, keyframes);
    }

    public String id() {
        return id;
    }

    /** Length in ticks (at least 1). */
    public int ticks() {
        return ticks;
    }

    /** True if every pose is flipped left/right when played. */
    public boolean mirror() {
        return mirror;
    }

    public List<Keyframe> keyframes() {
        return keyframes;
    }

    /**
     * The pose at 'progress' (0 = start, 1 = end), mirror already applied. REST before the first keyframe,
     * after the last one, or if there are no keyframes; never null, never NaN for a finite progress.
     */
    public Pose sample(float progress) {
        Pose pose = sampleUnmirrored(progress);
        return mirror ? pose.mirrored() : pose;
    }

    private Pose sampleUnmirrored(float progress) {
        if (keyframes.isEmpty() || Float.isNaN(progress)) return Pose.REST;
        Keyframe first = keyframes.get(0);
        if (progress <= first.at()) return first.at() <= 0f ? first.pose() : Pose.lerp(Pose.REST, first.pose(), first.easing().apply(progress / first.at()));
        for (int i = 1; i < keyframes.size(); i++) {
            Keyframe from = keyframes.get(i - 1), to = keyframes.get(i);
            if (progress <= to.at()) {
                float span = to.at() - from.at();
                float t = span <= 0f ? 1f : (progress - from.at()) / span;
                return Pose.lerp(from.pose(), to.pose(), to.easing().apply(t));
            }
        }
        return keyframes.get(keyframes.size() - 1).pose();
    }
}
