package net.lukario.circleofinevitability.camera;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.Locale;
import java.util.Optional;

/** Server-side data from data/<namespace>/camera_presets/<name>.json. */
public record CameraPreset(Motion motion, Collision collision, double range, boolean returnAtBoundary,
                           int duration, boolean cancelOnDamage, boolean manualExit, Display display) {
    public enum Mode { FREE, FIXED, ORBIT, SCRIPTED }
    public enum Walls { SOLID, PHASE, ALLOWLIST, DENYLIST }

    private static <E extends Enum<E>> Codec<E> names(Class<E> type) {
        return Codec.STRING.comapFlatMap(name -> {
            try {
                return DataResult.success(Enum.valueOf(type, name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                return DataResult.error(() -> "Unknown " + type.getSimpleName() + " '" + name + "'");
            }
        }, value -> value.name().toLowerCase(Locale.ROOT));
    }

    /** An optional jump along the current free-flight aim after each full interval of held input. */
    public record ForwardStep(double blocks, int intervalTicks) {
        static final Codec<ForwardStep> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(0.01, 64).fieldOf("blocks").forGetter(ForwardStep::blocks),
                Codec.intRange(1, 72000).fieldOf("interval_ticks").forGetter(ForwardStep::intervalTicks)
        ).apply(i, ForwardStep::new));
    }

    public record Motion(Mode mode, double speed, double sprintMultiplier, double acceleration,
                         double braking, double orbitRadius, Optional<ForwardStep> forwardStep) {
        static final Motion DEFAULT = new Motion(Mode.FREE, 0.35, 2, 0.5, 0.7, 4, Optional.empty());
        static final Codec<Motion> CODEC = RecordCodecBuilder.<Motion>create(i -> i.group(
                names(Mode.class).optionalFieldOf("mode", DEFAULT.mode).forGetter(Motion::mode),
                Codec.doubleRange(0.01, 4).optionalFieldOf("speed", DEFAULT.speed).forGetter(Motion::speed),
                Codec.doubleRange(1, 4).optionalFieldOf("sprint_multiplier", DEFAULT.sprintMultiplier).forGetter(Motion::sprintMultiplier),
                Codec.doubleRange(0.01, 1).optionalFieldOf("acceleration", DEFAULT.acceleration).forGetter(Motion::acceleration),
                Codec.doubleRange(0.01, 1).optionalFieldOf("braking", DEFAULT.braking).forGetter(Motion::braking),
                Codec.doubleRange(0.5, 32).optionalFieldOf("orbit_radius", DEFAULT.orbitRadius).forGetter(Motion::orbitRadius),
                ForwardStep.CODEC.optionalFieldOf("forward_step").forGetter(Motion::forwardStep)
        ).apply(i, Motion::new)).validate(m -> m.forwardStep.isEmpty() || m.mode == Mode.FREE
                ? DataResult.success(m) : DataResult.error(() -> "forward_step requires free motion"));
    }

    public record Collision(Walls mode, Optional<TagKey<Block>> tag) {
        static final Collision DEFAULT = new Collision(Walls.SOLID, Optional.empty());
        static final Codec<Collision> CODEC = RecordCodecBuilder.<Collision>create(i -> i.group(
                names(Walls.class).fieldOf("mode").forGetter(Collision::mode),
                TagKey.codec(Registries.BLOCK).optionalFieldOf("tag").forGetter(Collision::tag)
        ).apply(i, Collision::new)).validate(c ->
                (c.mode == Walls.ALLOWLIST || c.mode == Walls.DENYLIST) == c.tag.isPresent()
                        ? DataResult.success(c) : DataResult.error(() -> "Set 'tag' only for allowlist or denylist"));
    }

    public record Display(int fov, boolean hideHud, int returnTicks) {
        static final Display DEFAULT = new Display(0, false, 0);
        static final Codec<Display> CODEC = RecordCodecBuilder.<Display>create(i -> i.group(
                Codec.intRange(0, 150).optionalFieldOf("fov", 0).forGetter(Display::fov),
                Codec.BOOL.optionalFieldOf("hide_hud", false).forGetter(Display::hideHud),
                Codec.intRange(0, 60).optionalFieldOf("return_ticks", 0).forGetter(Display::returnTicks)
        ).apply(i, Display::new)).validate(d -> d.fov == 0 || d.fov >= 30 ? DataResult.success(d)
                : DataResult.error(() -> "fov must be 0 (player setting) or 30 to 150"));
    }

    public static final Codec<CameraPreset> CODEC = RecordCodecBuilder.create(i -> i.group(
            Motion.CODEC.optionalFieldOf("motion", Motion.DEFAULT).forGetter(CameraPreset::motion),
            Collision.CODEC.optionalFieldOf("collision", Collision.DEFAULT).forGetter(CameraPreset::collision),
            Codec.doubleRange(0, 512).optionalFieldOf("range", 48.0).forGetter(CameraPreset::range),
            Codec.BOOL.optionalFieldOf("return_at_boundary", false).forGetter(CameraPreset::returnAtBoundary),
            Codec.intRange(0, 72000).optionalFieldOf("duration", 200).forGetter(CameraPreset::duration),
            Codec.BOOL.optionalFieldOf("cancel_on_damage", true).forGetter(CameraPreset::cancelOnDamage),
            Codec.BOOL.optionalFieldOf("manual_exit", true).forGetter(CameraPreset::manualExit),
            Display.CODEC.optionalFieldOf("display", Display.DEFAULT).forGetter(CameraPreset::display)
    ).apply(i, CameraPreset::new));
}
