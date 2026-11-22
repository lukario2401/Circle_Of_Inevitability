package net.lukario.circleofinevitability.camera;

import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Sweep a small camera volume against actual block shapes, including thin panes at sprint speed. */
final class CameraTravel {
    private static final double RADIUS = 0.16;

    private CameraTravel() {}

    private static BlockCollisions<VoxelShape> obstacles(Level level, AABB bounds, CameraPreset.Collision rule) {
        return new BlockCollisions<>(level, CollisionContext.empty(), bounds, false, (pos, shape) -> {
            BlockState state = level.getBlockState(pos);
            boolean blocks = switch (rule.mode()) {
                case SOLID -> true;
                case PHASE -> false;
                case ALLOWLIST -> !state.is(rule.tag().orElseThrow());
                case DENYLIST -> state.is(rule.tag().orElseThrow());
            };
            return blocks ? shape : Shapes.empty();
        });
    }

    static boolean fits(Level level, Vec3 point, CameraPreset.Collision rule) {
        if (rule.mode() == CameraPreset.Walls.PHASE) return true;
        AABB box = new AABB(point, point).inflate(RADIUS);
        for (var shapes = obstacles(level, box, rule); shapes.hasNext(); ) {
            if (!shapes.next().isEmpty()) return false;
        }
        return true;
    }

    static Vec3 sweep(Level level, Vec3 from, Vec3 to, CameraPreset.Collision rule) {
        double distance = from.distanceTo(to);
        if (distance < 1e-7 || rule.mode() == CameraPreset.Walls.PHASE) return to;
        // Split long paths so an orbit turn or return never scans a large diagonal cuboid.
        if (distance > 4) {
            int steps = (int)Math.ceil(distance / 4);
            Vec3 previous = from;
            for (int step = 1; step <= steps; step++) {
                Vec3 next = from.lerp(to, (double)step / steps);
                Vec3 result = sweep(level, previous, next, rule);
                if (!result.equals(next)) return result;
                previous = next;
            }
            return to;
        }
        // AABB.clip ignores a face at t=0. Start the ray just behind the camera so a
        // move beginning exactly on a face cannot enter the block on its next tick.
        Vec3 rayStart = from.subtract(to.subtract(from).scale(1e-6 / distance));
        double allowed = distance;
        for (var shapes = obstacles(level, new AABB(from, to).inflate(RADIUS), rule); shapes.hasNext(); ) {
            for (AABB shape : shapes.next().toAabbs()) {
                AABB expanded = shape.inflate(RADIUS);
                if (expanded.deflate(1e-7).contains(from)) return from;
                var hit = expanded.clip(rayStart, to);
                if (hit.isPresent()) allowed = Math.min(allowed, from.distanceTo(hit.get()));
            }
        }
        if (allowed == distance) return to;
        return from.lerp(to, Math.max(0, allowed - 1e-5) / distance);
    }
}
