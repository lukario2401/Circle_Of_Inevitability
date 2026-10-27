package net.lukario.circleofinevitability.fate;

import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Bounded, dimension-local history. Only position, motion, health and effects are ever restored. */
public final class FateTimeline {
    private record Snapshot(long time, Vec3 position, Vec3 motion, float health, List<MobEffectInstance> effects) {}
    private static final Map<ServerLevel, Map<UUID, Deque<Snapshot>>> HISTORY = new IdentityHashMap<>();
    private static final int HISTORY_TICKS = 440, MAX_ENTITIES = 512;
    private FateTimeline() {}

    public static void record(ServerLevel sl, Vec3 center, double radius) {
        Map<UUID, Deque<Snapshot>> records = HISTORY.computeIfAbsent(sl, key -> new HashMap<>());
        long now = sl.getGameTime();
        records.values().forEach(q -> { while (!q.isEmpty() && q.peekFirst().time < now - HISTORY_TICKS) q.removeFirst(); });
        records.entrySet().removeIf(e -> e.getValue().isEmpty() || sl.getEntity(e.getKey()) == null);
        for (Entity entity : sl.getEntities((Entity) null, new AABB(center, center).inflate(radius), e -> e.isAlive()
                && (e instanceof LivingEntity || e instanceof net.minecraft.world.entity.projectile.Projectile))) {
            if (!records.containsKey(entity.getUUID()) && records.size() >= MAX_ENTITIES) continue;
            Deque<Snapshot> q = records.computeIfAbsent(entity.getUUID(), key -> new ArrayDeque<>());
            if (!q.isEmpty() && q.peekLast().time == now) continue;
            LivingEntity living = entity instanceof LivingEntity l ? l : null;
            List<MobEffectInstance> effects = living == null ? List.of() : living.getActiveEffects().stream().map(MobEffectInstance::new).toList();
            q.addLast(new Snapshot(now, entity.position(), entity.getDeltaMovement(), living == null ? 0 : living.getHealth(), effects));
        }
    }

    private static Snapshot at(Entity entity, int ticksAgo) {
        if (!(entity.level() instanceof ServerLevel sl)) return null;
        Deque<Snapshot> q = HISTORY.getOrDefault(sl, Map.of()).get(entity.getUUID());
        if (q == null) return null;
        long time = sl.getGameTime() - ticksAgo;
        Snapshot selected = null;
        for (Snapshot frame : q) {
            if (frame.time > time) break;
            selected = frame;
        }
        // Require real history; a freshly joined entity is never rewound to a fabricated frame.
        return selected != null && time - selected.time <= 5 ? selected : null;
    }

    public static boolean has(Entity entity, int ticksAgo) { return at(entity, ticksAgo) != null; }
    public static boolean rewind(Entity entity, int ticksAgo, boolean positionOnly) {
        Snapshot frame = at(entity, ticksAgo);
        if (frame == null || !entity.isAlive() || !(entity.level() instanceof ServerLevel sl)
                || Root.has(entity, RootRestriction.TELEPORT) || entity.isPassenger() || entity.isVehicle()) return false;
        Vec3 destination = frame.position;
        if (!sl.hasChunkAt(BlockPos.containing(destination)) || !sl.getWorldBorder().isWithinBounds(BlockPos.containing(destination))
                || !sl.noCollision(entity, entity.getBoundingBox().move(destination.subtract(entity.position())))) return false;
        entity.teleportTo(destination.x, destination.y, destination.z);
        entity.setDeltaMovement(frame.motion);
        entity.fallDistance = 0;
        entity.hurtMarked = true;
        if (!positionOnly && entity instanceof LivingEntity living) {
            living.setHealth(Math.max(1, Math.min(living.getMaxHealth(), frame.health)));
            living.removeAllEffects();
            frame.effects.forEach(effect -> living.addEffect(new MobEffectInstance(effect)));
        }
        return true;
    }
    public static void clear(ServerLevel sl) { HISTORY.remove(sl); }
    public static void forget(Entity entity) {
        if (entity.level() instanceof ServerLevel sl) {
            Map<UUID, Deque<Snapshot>> records = HISTORY.get(sl);
            if (records != null) records.remove(entity.getUUID());
        }
    }
}
