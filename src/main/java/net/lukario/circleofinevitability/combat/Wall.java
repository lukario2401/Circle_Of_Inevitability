package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A wall: a flat barrier standing in the air that enemies cannot cross, either way, and their projectiles end at.
 * A gate slammed shut, a curtain of fire, a line of shields, a ward across a corridor. The flat cousin of Dome:
 *
 *   Wall.at(middle, yaw, 10, 5)             // the middle of its bottom edge, the way its face looks, width, height
 *           .lasts(200)                     // ticks (default 200)
 *           .stopsProjectiles()             // projectiles of enemies (or of nobody) end at it
 *           .onBlock(at -> ...)             // something was stopped at this spot on the wall: a ripple, a sound
 *           .onEnd(() -> ...)
 *           .raise(player, sl, "my_pathway_wall");
 *
 * 'yaw' is a direction like a player's: Wall.at(spot, player.getYRot(), ...) stands across the player's view, face
 * toward them. It has no thickness of its own; crossing its plane is what counts, checked every tick from where each
 * one is, so walking, being knocked through or a teleport across all count. One put back stands on its own side,
 * pushed off the wall. Its ends and top are open: going round or over it is allowed.
 *
 * WHO IS WHO. Outsiders are the owner's enemies (Spells.isEnemy) unless .outsiders(...) says otherwise; everyone
 * else, and the owner, crosses freely, and so do their projectiles.
 *
 *   Wall.isRaised(player, name)    Wall.ticksLeft(player, name)    Wall.close(player, name)    Wall.renew(player, name)
 *
 * Raising one with a name that player already has closes the old one first. It ends by itself when its time is up
 * or its owner dies, logs out or changes dimension. It has no look of its own: draw it when you raise it and clear
 * the look in onEnd. Not saved: a server restart drops it.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Wall {

    /** How far in front of and behind the wall things are looked at (blocks). */
    private static final double MARGIN = 4.0;
    /** Ticks between two onBlock calls for the same entity being put back. */
    private static final int BLOCK_SHOW_EVERY = 8;

    private static final List<Wall> RAISED = new ArrayList<>();

    // ---------- what it is ----------

    private final Vec3 middle;              // the middle of its bottom edge
    private final Vec3 normal;              // the way its face looks (level)
    private final Vec3 side;                // along it (level)
    private final double halfWidth;
    private final double height;
    private int lasts = 200;
    private boolean stopsProjectiles;
    private Predicate<LivingEntity> outsiders;
    private Consumer<Vec3> onBlock;
    private Runnable onEnd;

    // ---------- while it stands ----------

    private LivingEntity owner;
    private ServerLevel level;
    private String name;
    private int age;
    private boolean closed = true;
    private final Map<UUID, Boolean> wasInFront = new HashMap<>();
    private final Map<UUID, Integer> lastShown = new HashMap<>();

    private Wall(Vec3 middle, float yaw, double width, double height) {
        this.middle = middle;
        double angle = Math.toRadians(yaw);
        this.normal = new Vec3(-Math.sin(angle), 0, Math.cos(angle));
        this.side = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        this.halfWidth = Math.max(0.5, width / 2.0);
        this.height = Math.max(1.0, height);
    }

    /** A wall whose bottom edge's middle is 'middle', facing 'yaw', 'width' across and 'height' tall. */
    public static Wall at(Vec3 middle, float yaw, double width, double height) {
        return new Wall(middle, yaw, width, height);
    }

    // ---------- setting it up ----------

    public Wall lasts(int ticks) {
        this.lasts = Math.max(1, ticks);
        return this;
    }

    /** Projectiles of outsiders (or of nobody) end at it, both ways. */
    public Wall stopsProjectiles() {
        this.stopsProjectiles = true;
        return this;
    }

    /** Who cannot cross, instead of the owner's enemies. The owner always can. */
    public Wall outsiders(Predicate<LivingEntity> who) {
        this.outsiders = who;
        return this;
    }

    /** Runs with the spot on the wall where something was stopped. */
    public Wall onBlock(Consumer<Vec3> action) {
        this.onBlock = action;
        return this;
    }

    /** Runs when it falls, however it ended. */
    public Wall onEnd(Runnable action) {
        this.onEnd = action;
        return this;
    }

    /** Raises it. If this player already has one with this name, that one falls first. */
    public void raise(LivingEntity owner, ServerLevel level, String name) {
        close(owner, name);
        this.owner = owner;
        this.level = level;
        this.name = name;
        this.age = 0;
        this.closed = false;
        this.wasInFront.clear();
        RAISED.add(this);
        tickSafely();
    }

    // ---------- asking / changing ----------

    public static boolean isRaised(LivingEntity owner, String name) {
        return find(owner, name) != null;
    }

    public static int ticksLeft(LivingEntity owner, String name) {
        Wall wall = find(owner, name);
        return wall == null ? 0 : Math.max(1, wall.lasts - wall.age);
    }

    /** It falls now (its onEnd runs). */
    public static void close(LivingEntity owner, String name) {
        Wall wall = find(owner, name);
        if (wall != null) wall.end();
    }

    /** Its whole time again, from now. */
    public static void renew(LivingEntity owner, String name) {
        Wall wall = find(owner, name);
        if (wall != null) wall.age = 0;
    }

    private static Wall find(LivingEntity owner, String name) {
        for (Wall wall : RAISED) {
            if (!wall.closed && wall.name.equals(name) && wall.owner.getUUID().equals(owner.getUUID())) return wall;
        }
        return null;
    }

    // ---------- internals ----------

    private boolean isOwner(Entity entity) {
        return entity == owner || entity.getUUID().equals(owner.getUUID());
    }

    private boolean isOutsider(LivingEntity entity) {
        if (isOwner(entity)) return false;
        return outsiders != null ? outsiders.test(entity) : Spells.isEnemy(owner, entity);
    }

    /** How far in front of the wall a point is (negative: behind it). */
    private double front(Vec3 at) {
        return at.subtract(middle).dot(normal);
    }

    /** Whether a point (with 'grow' blocks to spare sideways and up) is level with the wall's face. */
    private boolean beside(Vec3 at, double grow) {
        Vec3 d = at.subtract(middle);
        double along = d.dot(side);
        double up = at.y - middle.y;
        return Math.abs(along) <= halfWidth + grow && up >= -1.0 - grow && up <= height + grow;
    }

    private AABB box() {
        Vec3 a = middle.add(side.scale(halfWidth));
        Vec3 b = middle.add(side.scale(-halfWidth)).add(0, height, 0);
        return new AABB(a, b).inflate(MARGIN);
    }

    private void tick() {
        if (!Spells.casterStillHere(owner, level) || age >= lasts) {
            end();
            return;
        }
        age++;
        AABB box = box();
        Set<UUID> seen = new HashSet<>();
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && !e.isSpectator() && !(e instanceof ArmorStand) && !e.isPassenger())) {
            if (!isOutsider(entity) || (entity instanceof Player player && player.isCreative())) continue;
            UUID id = entity.getUUID();
            seen.add(id);
            Vec3 body = entity.getBoundingBox().getCenter();
            boolean inFront = front(body) >= 0;
            Boolean before = wasInFront.get(id);
            if (before != null && before != inFront && beside(body, entity.getBbWidth() * 0.5 + entity.getBbHeight() * 0.5)) {
                putBack(entity, before);
                continue;
            }
            wasInFront.put(id, inFront);
        }
        wasInFront.keySet().retainAll(seen);
        if (stopsProjectiles) stopProjectiles(box);
    }

    /** Puts it back on its own side: just in front of the wall, or just behind it. */
    private void putBack(LivingEntity entity, boolean inFront) {
        Vec3 body = entity.getBoundingBox().getCenter();
        double out = (entity.getBbWidth() * 0.5 + 0.6) * (inFront ? 1 : -1);
        Vec3 onWall = body.subtract(normal.scale(front(body)));
        Vec3 to = onWall.add(normal.scale(out));
        entity.teleportTo(to.x, entity.getY(), to.z);
        entity.setDeltaMovement(normal.x * (inFront ? 0.35 : -0.35), 0.1, normal.z * (inFront ? 0.35 : -0.35));
        entity.hurtMarked = true;
        wasInFront.put(entity.getUUID(), inFront);
        if (onBlock == null) return;
        Integer last = lastShown.get(entity.getUUID());
        if (last != null && age - last < BLOCK_SHOW_EVERY) return;
        lastShown.put(entity.getUUID(), age);
        onBlock.accept(onWall);
    }

    /** Projectiles of outsiders whose next step goes through the wall end there. */
    private void stopProjectiles(AABB box) {
        for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, box, Entity::isAlive)) {
            Entity shooter = projectile.getOwner();
            if (shooter != null && (isOwner(shooter) || (shooter instanceof LivingEntity living && !isOutsider(living)))) continue;
            Vec3 now = projectile.position();
            Vec3 next = now.add(projectile.getDeltaMovement());
            double a = front(now), b = front(next);
            if ((a >= 0) == (b >= 0)) continue;
            Vec3 through = now.add(next.subtract(now).scale(a / (a - b)));
            if (!beside(through, 0.3)) continue;
            projectile.discard();
            if (onBlock != null) onBlock.accept(through);
        }
    }

    private void end() {
        if (closed) return;
        closed = true;
        RAISED.remove(this);
        wasInFront.clear();
        lastShown.clear();
        if (onEnd != null) {
            try {
                onEnd.run();
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A wall failed while falling", e);
            }
        }
    }

    private void tickSafely() {
        try {
            tick();
        } catch (Exception e) {
            CircleOfInevitability.LOGGER.error("A wall failed", e);
            end();
        }
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onWallLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel level) || RAISED.isEmpty()) return;
        for (Wall wall : new ArrayList<>(RAISED)) {
            if (wall.level == level && !wall.closed) wall.tickSafely();
        }
    }

    @SubscribeEvent
    public static void onWallLevelUnload(LevelEvent.Unload event) {
        RAISED.removeIf(wall -> wall.level == event.getLevel());
    }
}
