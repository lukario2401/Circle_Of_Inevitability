package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.snake_of_mercury;

import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.fate.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.snake_of_mercury.SnakeOfMercuryPowers.*;

/** Dimension-bound cycle state; unloaded levels, dead casters and logouts release every anchor/medium. */
public final class SnakeOfMercuryCycle {
    private record Anchor(UUID target, Vec3 at, long expires) {
        Vec3 position(ServerLevel sl) { return target == null ? at : sl.getEntity(target) instanceof LivingEntity e && e.isAlive() ? e.position() : null; }
    }
    private record Loop(UUID owner, UUID medium, Vec3 at, double radius, long created, long expires, boolean bosses) {}
    private static final Map<ServerLevel, Map<UUID, List<Anchor>>> ANCHORS = new IdentityHashMap<>();
    private static final Map<ServerLevel, List<Loop>> LOOPS = new IdentityHashMap<>();
    static {
        Casts.addListener((p, sl, cast) -> {
            if (!Fate.applies(p) || Casts.isCopying(p) || !cast.pathway().equals(Fate.PATHWAY) || !cast.copyable()) return;
            // Self-only buffs have no transferable origin. Never overwrite them from an anchor.
            boolean targetedRead = cast.key() == 2 && !cast.sneak();
            boolean targetedEntry = cast.entry() != null && (cast.entry().key().equals("chain") || cast.entry().key().equals("conceal"));
            if (!targetedRead && !targetedEntry) return;
            List<Vec3> spots = anchors(p, sl).stream().map(a -> a.position(sl)).filter(Objects::nonNull).toList();
            if (!spots.isEmpty()) Echo.repeat(p, cast, spots, .5f, index -> ring(sl, spots.get(index), .7, SILVER, 15));
        });
    }
    private SnakeOfMercuryCycle() {}
    static void initialize() {} // force listener registration without waiting for the first cycle cast
    private static List<Anchor> anchors(ServerPlayer p, ServerLevel sl) {
        List<Anchor> list = ANCHORS.computeIfAbsent(sl, key -> new HashMap<>()).computeIfAbsent(p.getUUID(), key -> new ArrayList<>());
        list.removeIf(a -> a.expires <= sl.getGameTime() || a.position(sl) == null);
        return list;
    }
    static AbilityWheel.Result reboot(ServerPlayer p, ServerLevel sl) {
        int ago = sequence(p) == 0 ? 400 : 160;
        double radius = (sequence(p) == 0 ? 48 : 24) * material(p).aoe;
        Vec3 center = p.position();
        List<Entity> candidates = new ArrayList<>(sl.getEntities(p, p.getBoundingBox().inflate(radius), Entity::isAlive));
        candidates.add(p);
        if (candidates.stream().noneMatch(e -> FateTimeline.has(e, ago))) return refuse(p, "The cycle has not recorded enough history yet.");
        int restored = 0;
        for (Entity entity : candidates) if (entity.position().distanceToSqr(center) <= radius * radius && FateTimeline.rewind(entity, ago, false)) restored++;
        if (restored == 0) return refuse(p, "No recorded entity can safely return.");
        ring(sl, center, radius, SILVER, 40);
        return AbilityWheel.Result.DONE;
    }
    static AbilityWheel.Result loop(ServerPlayer p, ServerLevel sl) {
        Vec3 at = Spells.aimPoint(p, sl, 32 * material(p).range);
        List<Loop> loops = LOOPS.computeIfAbsent(sl, key -> new ArrayList<>());
        // Recasting replaces this caster's medium instead of accumulating unbounded areas.
        loops.removeIf(old -> { if (!old.owner.equals(p.getUUID())) return false; discard(sl, old); return true; });
        ArmorStand medium = new ArmorStand(EntityTypes.ARMOR_STAND, sl);
        medium.setPos(at.x, at.y, at.z);
        medium.setNoGravity(true);
        medium.setInvulnerable(true);
        medium.setCustomName(Component.literal("Broken Wheel · Fate Loop"));
        medium.setCustomNameVisible(true);
        medium.getPersistentData().putBoolean(PREFIX + "medium", true);
        // Custom hit-point tracking handles vanilla armor stand's special damage behavior.
        medium.getPersistentData().putFloat(PREFIX + "medium_health", sequence(p) == 0 ? 80 : 40);
        if (!sl.addFreshEntity(medium)) return refuse(p, "The loop medium could not appear.");
        int ticks = duration(p, 300); double radius = 10 * material(p).aoe;
        loops.add(new Loop(p.getUUID(), medium.getUUID(), at, radius, sl.getGameTime(), sl.getGameTime() + ticks, sequence(p) == 0));
        FateTimeline.record(sl, at, radius);
        ring(sl, at, radius, SILVER, ticks);
        return AbilityWheel.Result.DONE;
    }
    private static Anchor aimed(ServerPlayer p, ServerLevel sl) {
        Vec3 from = p.getEyePosition(), direction = p.getLookAngle();
        return anchors(p, sl).stream().filter(a -> {
            Vec3 delta = a.position(sl).add(0, .5, 0).subtract(from);
            double along = delta.dot(direction);
            return along > 0 && along <= 32 * material(p).range && delta.subtract(direction.scale(along)).lengthSqr() <= 1.5
                    && Spells.clearLine(p, sl, from, a.position(sl).add(0, .5, 0));
        }).min(Comparator.comparingDouble(a -> a.position(sl).distanceToSqr(from))).orElse(null);
    }
    static boolean aimingAtAnchor(ServerPlayer p) { return aimed(p, p.level()) != null; }
    static AbilityWheel.Result connection(ServerPlayer p, ServerLevel sl) {
        Anchor existing = aimed(p, sl);
        if (existing != null) {
            if (Root.has(p, RootRestriction.TELEPORT)) return refuse(p, "Teleportation is blocked.");
            Vec3 there = existing.position(sl), home = p.position();
            Vec3 safe = Spells.freeSpotNear(p, sl, there, Spells.flatLook(p), 0);
            if (safe == null) return refuse(p, "There is no safe space at this coin.");
            Entity target = existing.target == null ? null : sl.getEntity(existing.target);
            if (target != null && (Root.has(target, RootRestriction.TELEPORT) || !sl.noCollision(target, target.getBoundingBox().move(home.subtract(target.position())))))
                return refuse(p, "The anchored target cannot safely swap.");
            p.teleportTo(safe.x, safe.y, safe.z); p.fallDistance = 0;
            if (target != null) target.teleportTo(home.x, home.y, home.z);
            else { List<Anchor> list = anchors(p, sl); list.remove(existing); list.add(new Anchor(null, home, existing.expires)); }
            return AbilityWheel.Result.FREE;
        }
        List<Anchor> list = anchors(p, sl);
        if (list.size() >= 3) return refuse(p, "Three connections already exist. Wait for a coin to expire.");
        LivingEntity target = aim(p, sl, 32 * material(p).range);
        Vec3 at = target == null ? Spells.aimPoint(p, sl, 32 * material(p).range) : target.position();
        list.add(new Anchor(target == null ? null : target.getUUID(), at, sl.getGameTime() + duration(p, 1200)));
        ring(sl, at, .6, GOLD, 40);
        return AbilityWheel.Result.DONE;
    }
    static AbilityWheel.Result reincarnation(ServerPlayer p, ServerLevel sl) {
        if (DeathWard.isArmed(p)) return refuse(p, "A death ward is already armed.");
        if (Decree.violate(p, Decree.Rule.RESURRECTION)) return refuse(p, "Resurrection is forbidden here.");
        DeathWard.arm(p, 12000, .5f, (saved, source, world) -> {
            FateTimeline.rewind(saved, 200, true);
            saved.removeAllEffects(); saved.clearFire();
            Concealment.hide(saved, 60);
            if (saved instanceof ServerPlayer player) { Luck.add(player, 10); Fate.spin(player); }
            ring(world, saved.position(), 3, GOLD, 40);
        });
        return AbilityWheel.Result.DONE;
    }
    static AbilityWheel.Result wheel(ServerPlayer p, ServerLevel sl) {
        if (active(p, "sky")) { p.getPersistentData().remove(PREFIX + "sky"); return AbilityWheel.Result.FREE; }
        arm(p, "sky", duration(p, 600), "The Wheel of Fortune");
        p.getPersistentData().putDouble(PREFIX + "sky_radius", 30 * material(p).aoe);
        p.getPersistentData().putLong(PREFIX + "sky_pulse", sl.getGameTime() + 80);
        return AbilityWheel.Result.DONE;
    }

    static void playerTick(ServerPlayer p, ServerLevel sl) {
        for (Anchor anchor : anchors(p, sl)) if (p.tickCount % 20 == 0) ring(sl, anchor.position(sl).add(0, .3, 0), .45, GOLD, 20);
        if (sequence(p) <= 1 && p.tickCount % 5 == 0) FateTimeline.record(sl, p.position(), sequence(p) == 0 ? 100 : 64);
        if (active(p, "sky")) {
            if (p.tickCount % 20 == 0) ring(sl, p.position().add(0, 12, 0), 12, SILVER, 25);
            if (sl.getGameTime() >= p.getPersistentData().getLongOr(PREFIX + "sky_pulse", 0L)) {
                p.getPersistentData().putLong(PREFIX + "sky_pulse", sl.getGameTime() + 80);
                List<LivingEntity> recipients = Spells.livingAround(p, sl, p.position(), p.getPersistentData().getDoubleOr(PREFIX + "sky_radius", 30d));
                recipients.add(p);
                for (LivingEntity target : recipients) {
                if (target == p || Spells.isFriend(p, target)) { Luck.bestow(p, target, 10); target.heal(target.getMaxHealth() * .15f); }
                else { Luck.bestow(p, target, -10); Misfortune.add(p, target, 10); SnakeOfMercuryEvents.mishap(target, true); }
                }
            }
        }
    }
    static void levelTick(ServerLevel sl) {
        List<Loop> loops = LOOPS.get(sl); if (loops == null) return;
        loops.removeIf(loop -> {
            Entity owner = sl.getEntity(loop.owner), medium = sl.getEntity(loop.medium);
            if (!(owner instanceof ServerPlayer p) || !p.isAlive() || !Fate.applies(p) || sequence(p) > 1
                    || medium == null || !medium.isAlive() || loop.expires <= sl.getGameTime()) { discard(sl, loop); return true; }
            if (sl.getGameTime() % 5 == 0) FateTimeline.record(sl, loop.at, loop.radius);
            if (sl.getGameTime() > loop.created && (sl.getGameTime() - loop.created) % 60 == 0) for (LivingEntity target : Spells.enemiesAround(p, sl, loop.at, loop.radius))
                if (target != medium && (loop.bosses || !Spells.isBoss(target))) FateTimeline.rewind(target, 60, false);
            return false;
        });
        if (loops.isEmpty()) LOOPS.remove(sl);
    }
    private static void discard(ServerLevel sl, Loop loop) { Entity medium = sl.getEntity(loop.medium); if (medium != null) medium.discard(); }
    static void clear(ServerLevel sl) {
        for (Loop loop : LOOPS.getOrDefault(sl, List.of())) discard(sl, loop);
        LOOPS.remove(sl); ANCHORS.remove(sl); FateTimeline.clear(sl);
    }
    static void forget(ServerPlayer p) {
        Map<UUID, List<Anchor>> map = ANCHORS.get(p.level()); if (map != null) map.remove(p.getUUID());
        List<Loop> loops = LOOPS.get(p.level());
        if (loops != null) loops.removeIf(loop -> { if (!loop.owner.equals(p.getUUID())) return false; discard(p.level(), loop); return true; });
        FateTimeline.forget(p);
    }
}
