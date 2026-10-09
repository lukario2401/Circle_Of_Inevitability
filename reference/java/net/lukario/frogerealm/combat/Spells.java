package net.lukario.frogerealm.combat;

import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.lukario.frogerealm.status.Concealment;
import net.lukario.frogerealm.status.Marionette;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * The questions every ability asks, answered once: who is an enemy, what am I aiming at, where is the ground,
 * who stands in this circle or on this line. Any class can call these from its abilities:
 *
 *   LivingEntity target = Spells.aimEnemy(player, sl, 24);          // the enemy under the crosshair, or null
 *   Vec3 spot = Spells.aimGround(player, sl, 24);                   // the ground the player is aiming at
 *   Vec3 direction = Spells.aimFrom(player, sl, hand, 24);          // from the hand toward what the crosshair is on
 *   for (LivingEntity e : Spells.enemiesAround(player, sl, spot, 4)) Spells.strike(player, e, 12f);
 *   if (!Spells.payEssence(player, 1000)) return;                   // takes the soul essence, or says there is not enough
 *
 * "Enemy" means: anything alive except the caster, what the caster rides or owns (pets, tamed horses), the
 * caster's marionettes, team mates, armor stands, spectators and whoever is concealed. Add your own rule with
 * Spells.addAllyCheck(...), e.g. for summons of a new class.
 *
 * (PrinceOfAbolition has private copies of several of these: it was written before this class existed.)
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class Spells {

    public static final Vec3 UP = new Vec3(0, 1, 0);

    /** Says whether 'other' is on the caster's side and must be left alone by the caster's spells. */
    @FunctionalInterface
    public interface AllyCheck {
        boolean isAlly(Player caster, LivingEntity other);
    }

    private static final List<AllyCheck> ALLY_CHECKS = new ArrayList<>();

    private Spells() {}

    /** One more kind of ally that spells leave alone, e.g. (caster, other) -> MySummons.belongsTo(other, caster). */
    public static void addAllyCheck(AllyCheck check) {
        ALLY_CHECKS.add(check);
    }

    // =========================
    // Who is an enemy
    // =========================

    public static boolean isEnemy(Player caster, LivingEntity other) {
        // the UUID check also covers the caster's new body after dying and respawning while a spell is still running
        if (other == caster || other.getUUID().equals(caster.getUUID())) return false;
        if (!other.isAlive() || other.isSpectator() || other instanceof ArmorStand) return false;
        if (other == caster.getVehicle()) return false;
        if (other instanceof OwnableEntity owned && caster.getUUID().equals(owned.getOwnerUUID())) return false;
        if (Marionette.isMarionetteOf(other, caster)) return false;
        if (Concealment.isHidden(other)) return false;
        if (caster.isAlliedTo(other)) return false;               // same scoreboard team
        for (AllyCheck check : ALLY_CHECKS) {
            if (check.isAlly(caster, other)) return false;
        }
        return true;
    }

    /** Every enemy whose body is within 'radius' of a point. */
    public static List<LivingEntity> enemiesAround(Player caster, ServerLevel sl, Vec3 center, double radius) {
        List<LivingEntity> result = new ArrayList<>();
        AABB area = new AABB(center, center).inflate(radius + 2.0);
        for (LivingEntity candidate : sl.getEntitiesOfClass(LivingEntity.class, area, other -> isEnemy(caster, other))) {
            if (distanceToBody(candidate, center) <= radius) result.add(candidate);
        }
        return result;
    }

    /** Every enemy whose body (grown by 'grow' blocks) the line from..to passes through, nearest to 'from' first. */
    public static List<LivingEntity> enemiesOnLine(Player caster, ServerLevel sl, Vec3 from, Vec3 to, double grow) {
        List<LivingEntity> result = new ArrayList<>();
        if (from.distanceToSqr(to) < 1.0E-6) return result;
        AABB area = new AABB(from, to).inflate(grow + 0.5);
        for (LivingEntity candidate : sl.getEntitiesOfClass(LivingEntity.class, area, other -> isEnemy(caster, other))) {
            AABB body = candidate.getBoundingBox().inflate(grow);
            // clip() finds nothing when the line starts inside the box, which is exactly an enemy right on top of you
            if (body.contains(from) || body.clip(from, to).isPresent()) result.add(candidate);
        }
        result.sort(Comparator.comparingDouble(candidate -> candidate.getBoundingBox().getCenter().distanceToSqr(from)));
        return result;
    }

    /**
     * Every enemy the caster can see: within 'range' blocks, no more than 'halfAngle' degrees away from the crosshair
     * (45 is about what is on the screen) and not behind blocks. Nearest first.
     */
    public static List<LivingEntity> enemiesInSight(Player caster, ServerLevel sl, double range, double halfAngle) {
        Vec3 eye = caster.getEyePosition();
        List<LivingEntity> result = enemiesInCone(caster, sl, eye, caster.getLookAngle(), range, halfAngle);
        // its body or its head showing is enough
        result.removeIf(candidate -> !clearLine(caster, sl, eye, candidate.getBoundingBox().getCenter())
                && !clearLine(caster, sl, eye, candidate.getEyePosition()));
        return result;
    }

    /**
     * Every enemy in a cone: within 'range' blocks of 'from' and no more than 'halfAngle' degrees away from
     * 'direction'. Walls do not matter here (enemiesInSight is the one that looks). Nearest first.
     * For a breath, a roar, a blast out of a summon's mouth.
     */
    public static List<LivingEntity> enemiesInCone(Player caster, ServerLevel sl, Vec3 from, Vec3 direction,
                                                   double range, double halfAngle) {
        Vec3 along = direction.normalize();
        double narrowest = Math.cos(Math.toRadians(halfAngle));

        List<LivingEntity> result = new ArrayList<>();
        for (LivingEntity candidate : enemiesAround(caster, sl, from, range)) {
            Vec3 toIt = candidate.getBoundingBox().getCenter().subtract(from);
            // one that is right on the point has no direction: it counts as inside
            if (toIt.lengthSqr() > 1.0 && toIt.normalize().dot(along) < narrowest) continue;
            result.add(candidate);
        }
        result.sort(Comparator.comparingDouble(candidate -> candidate.getBoundingBox().getCenter().distanceToSqr(from)));
        return result;
    }

    /**
     * A wave that spreads out from a spot along the ground and reaches 'radius' blocks after 'ticks' ticks.
     * onReach runs once for each enemy, at the moment the wave gets to it: enemies near the middle first.
     * 'height' = how far above and below the spot it still catches someone.
     *
     *   SpellFx.steadyRing(sl, ground, 16, 0xF09CFFD2, 20, 0);                       // what it looks like
     *   Spells.wave(player, sl, ground, 16, 20, 6, enemy -> Spells.strike(player, enemy, 45f));
     */
    public static void wave(Player caster, ServerLevel sl, Vec3 ground, double radius, int ticks, double height,
                            Consumer<LivingEntity> onReach) {
        Set<UUID> reached = new HashSet<>();
        int steps = Math.max(1, ticks);
        for (int tick = 1; tick <= steps; tick++) {
            double front = radius * tick / steps;
            Later.run(sl, tick, () -> {
                if (!casterStillHere(caster, sl)) return;
                for (LivingEntity enemy : enemiesAround(caster, sl, ground, radius + height)) {
                    if (Math.abs(enemy.getY() - ground.y) > height) continue;
                    double dx = enemy.getX() - ground.x;
                    double dz = enemy.getZ() - ground.z;
                    // measured to the edge of its body, so it is caught when the ring touches it
                    if (Math.sqrt(dx * dx + dz * dz) - enemy.getBbWidth() / 2.0 > front) continue;
                    if (reached.add(enemy.getUUID())) onReach.accept(enemy);
                }
            });
        }
    }

    /** The enemy on the line that is closest to 'from', or null. */
    public static LivingEntity firstEnemyOnLine(Player caster, ServerLevel sl, Vec3 from, Vec3 to, double grow) {
        List<LivingEntity> onLine = enemiesOnLine(caster, sl, from, to, grow);
        return onLine.isEmpty() ? null : onLine.get(0);
    }

    /** Distance from a point to the nearest part of an entity's body (0 when the point is inside it). */
    public static double distanceToBody(LivingEntity entity, Vec3 point) {
        AABB body = entity.getBoundingBox();
        double dx = Mth.clamp(point.x, body.minX, body.maxX) - point.x;
        double dy = Mth.clamp(point.y, body.minY, body.maxY) - point.y;
        double dz = Mth.clamp(point.z, body.minZ, body.maxZ) - point.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // =========================
    // What the caster is aiming at
    // =========================

    /** The enemy the caster is looking at within range (not through walls), or null. */
    public static LivingEntity aimEnemy(Player caster, ServerLevel sl, double range) {
        Vec3 eye = caster.getEyePosition();
        return firstEnemyOnLine(caster, sl, eye, aimPoint(caster, sl, range), 0.4);
    }

    /** Where the crosshair ends: on the first block in the way, or 'range' blocks out if there is none. */
    public static Vec3 aimPoint(Player caster, ServerLevel sl, double range) {
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(range));
        return sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getLocation();
    }

    /**
     * The direction from 'from' to what the crosshair is on (the first enemy in the way, else the block, else the
     * far end of the range). Throw things that start BESIDE the caster along this (from a hand, a shoulder, a summon
     * standing next to them) and they land where the caster is aiming instead of a step to the side of it.
     */
    public static Vec3 aimFrom(Player caster, ServerLevel sl, Vec3 from, double range) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 point = aimPoint(caster, sl, range);
        LivingEntity target = firstEnemyOnLine(caster, sl, eye, point, 0.4);
        if (target != null) {
            // the spot on the crosshair's line that is level with that enemy
            double along = Mth.clamp(target.getBoundingBox().getCenter().subtract(eye).dot(look), 0.0, eye.distanceTo(point));
            point = eye.add(look.scale(along));
        }
        Vec3 aim = point.subtract(from);
        return aim.lengthSqr() < 1.0 ? look : aim.normalize();     // too close to tell: straight ahead
    }

    /**
     * The spot on the ground the caster is aiming at: under the first enemy in the way,
     * else where the look hits a block, else under the far end of the range.
     */
    public static Vec3 aimGround(Player caster, ServerLevel sl, double range) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 stop = aimPoint(caster, sl, range);
        LivingEntity target = firstEnemyOnLine(caster, sl, eye, stop, 0.4);
        if (target != null) return groundAt(caster, sl, target.position().add(0, 0.5, 0));
        return groundAt(caster, sl, stop.subtract(look.scale(0.3)));           // a step back out of the block that was hit
    }

    /**
     * Top of the blocks under a point. The point itself if there is nothing within 24 blocks below it.
     * It looks down from a little above the point, so a point lying on the ground still finds it. A look that
     * starts inside a block "hits" at once, so when that happens it tries again from other heights:
     * from the point itself (the point is just under a ceiling), then from higher up (the point is inside a hill).
     */
    public static Vec3 groundAt(Player caster, ServerLevel sl, Vec3 point) {
        Vec3 to = new Vec3(point.x, point.y - 24.0, point.z);
        for (double lift : new double[]{0.5, 0.0, 1.5, 2.5, 3.5}) {
            Vec3 from = new Vec3(point.x, point.y + lift, point.z);
            BlockHitResult hit = sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
            if (hit.getType() == HitResult.Type.MISS) return point;
            if (hit.getLocation().distanceToSqr(from) > 0.0025) return hit.getLocation();   // further than 0.05: a real hit
        }
        return point;
    }

    /**
     * The ground at a spot whose height you only roughly know, for putting a ring or a field of things on
     * uneven land: the top of the blocks no more than 'reach' above or below 'spot', or null if there is none
     * there (a cliff, a pit, open air, the inside of a hill).
     *
     *   Vec3 stands = Spells.groundNear(player, sl, Spells.spotAround(ground, yaw, 8), 3);
     *   if (stands != null) ParticleShapes.model(sl, STONE, stands, yaw, 0f, 0f);
     */
    public static Vec3 groundNear(Player caster, ServerLevel sl, Vec3 spot, double reach) {
        Vec3 top = groundAt(caster, sl, spot.add(0, reach, 0));
        // (groundAt hands back the point it was given when it finds nothing)
        if (top.y > spot.y + reach - 0.01 || spot.y - top.y > reach) return null;
        return top;
    }

    /**
     * Where something that should appear at 'wanted' can really appear: 'wanted' itself if nothing is between it
     * and the caster's eyes, else just in front of the block in the way. Without this, a spear that starts beside
     * you would start inside the wall whenever you stand next to one, and go nowhere.
     */
    public static Vec3 clearStart(Player caster, ServerLevel sl, Vec3 wanted) {
        Vec3 eye = caster.getEyePosition();
        BlockHitResult hit = sl.clip(new ClipContext(eye, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (hit.getType() == HitResult.Type.MISS) return wanted;
        Vec3 back = eye.subtract(hit.getLocation());
        return back.lengthSqr() < 0.04 ? eye : hit.getLocation().add(back.normalize().scale(0.15));
    }

    /** True when nothing solid is between the two points. */
    public static boolean clearLine(Player caster, ServerLevel sl, Vec3 from, Vec3 to) {
        return sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getType()
                == HitResult.Type.MISS;
    }

    /**
     * A place within 'distance' blocks of 'from', in (or near) the horizontal direction 'preferred', where the
     * player fits and that can be seen from 'from': for blinks, dodges and swaps. It tries the preferred direction
     * first, then turns away from it step by step. Null if every direction is walled in.
     */
    public static Vec3 freeSpotNear(Player player, ServerLevel sl, Vec3 from, Vec3 preferred, double distance) {
        Vec3 flat = new Vec3(preferred.x, 0, preferred.z);
        if (flat.lengthSqr() < 1.0E-6) flat = flatLook(player).scale(-1);
        float base = yawOf(flat.normalize());
        Vec3 chest = from.add(0, 1.0, 0);
        for (float turn : new float[]{0f, 35f, -35f, 70f, -70f, 110f, -110f, 150f, -150f, 180f}) {
            Vec3 direction = Vec3.directionFromRotation(0f, base + turn);
            for (double reach : new double[]{distance, distance * 0.6}) {
                Vec3 there = chest.add(direction.scale(reach));
                if (!clearLine(player, sl, chest, there)) continue;
                Vec3 ground = groundAt(player, sl, there);
                if (Math.abs(ground.y - from.y) > 3.5) continue;                // not off a cliff or up a tower
                if (fits(player, sl, ground)) return ground;
            }
        }
        return null;
    }

    /**
     * Where the player's feet go to be at what the crosshair is on, as near to it as they fit: for a blink.
     * Looking at the ground they stand on that spot; looking at a wall they stand in front of it (or hang in the
     * air in front of it, if it is a cliff: FallGuard.protect(...) is kind then); looking at the open sky they
     * end up 'range' blocks out. Never inside a block. Null when there is no room anywhere near.
     *
     *   Vec3 there = Spells.blinkSpot(player, sl, 32);
     *   if (there != null) player.teleportTo(there.x, there.y, there.z);
     */
    public static Vec3 blinkSpot(Player player, ServerLevel sl, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 point = aimPoint(player, sl, range);
        double far = eye.distanceTo(point);

        // a little back from what was hit, then further back until there is room
        for (double back : new double[]{0.4, 0.8, 1.2, 1.8, 2.6, 3.6}) {
            if (back >= far) break;
            Vec3 near = point.subtract(look.scale(back));
            Vec3 ground = groundAt(player, sl, near);

            // on the ground under it if that is close; else with the spot at the chest, the head or the feet
            if (near.y - ground.y <= 3.0 && fits(player, sl, ground)) return ground;
            for (double drop : new double[]{1.0, 1.85, 0.0}) {
                Vec3 feet = near.add(0, -drop, 0);
                if (fits(player, sl, feet)) return feet;
            }
        }
        return null;
    }

    /**
     * The ground an entity stands on or hangs just above (no more than 4 blocks): where spikes, hands or a
     * circle for it should appear. For one higher in the air it is the spot at its feet.
     */
    public static Vec3 groundUnder(Player caster, ServerLevel sl, LivingEntity entity) {
        if (entity.onGround()) return entity.position();
        Vec3 ground = groundAt(caster, sl, entity.position());
        return entity.getY() - ground.y <= 4.0 ? ground : entity.position();
    }

    /** The spot 'distance' blocks from 'center' the way 'yaw' looks, at the same height: for rings of things around a point. */
    public static Vec3 spotAround(Vec3 center, float yaw, double distance) {
        return center.add(Vec3.directionFromRotation(0f, yaw).scale(distance));
    }

    /** True if the player's body fits with their feet on this spot (for teleports: never into a wall). */
    public static boolean fits(Player player, ServerLevel sl, Vec3 feet) {
        return sl.noCollision(player, player.getBoundingBox().move(feet.subtract(player.position())));
    }

    // =========================
    // Directions
    // =========================

    /** The yaw that faces along a direction. */
    public static float yawOf(Vec3 direction) {
        return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
    }

    /** Horizontal direction the player faces. */
    public static Vec3 flatLook(Player player) {
        return Vec3.directionFromRotation(0f, player.getYRot());
    }

    /** Horizontal direction to the player's right. */
    public static Vec3 rightOf(Player player) {
        return flatLook(player).cross(UP).normalize();
    }

    /** 'direction' turned left/right by 'degrees' around the world's up (for fans of projectiles). */
    public static Vec3 turned(Vec3 direction, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(direction.x * cos - direction.z * sin, direction.y, direction.x * sin + direction.z * cos);
    }

    // =========================
    // Doing things
    // =========================

    /**
     * Damage from the caster. Clears the short invulnerability after a hit first, so quick hits in a row all count.
     * The target also becomes "what the caster last hit", which is what marionettes and tamed wolves go after.
     */
    public static void strike(Player caster, LivingEntity target, float damage) {
        target.invulnerableTime = 0;
        target.hurt(caster.damageSources().playerAttack(caster), damage);
        caster.setLastHurtMob(target);
    }

    /** Throws the target away from a point: 'strength' blocks per tick sideways, 'lift' upward. */
    public static void push(LivingEntity target, Vec3 from, double strength, double lift) {
        double dx = target.getX() - from.x;
        double dz = target.getZ() - from.z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 0.05) {
            target.setDeltaMovement(target.getDeltaMovement().add(0, lift, 0));
        } else {
            target.setDeltaMovement(dx / distance * strength, lift, dz / distance * strength);
        }
        target.hurtMarked = true;                                   // makes players' clients accept the push
    }

    /**
     * Drags the target toward a point (the opposite of push): faster the further away it is, 'maxSpeed' blocks per
     * tick at most, so it slows down as it gets there instead of flying past. Within a block of the point it is left alone.
     * Call it every couple of ticks for a steady pull.
     */
    public static void pull(LivingEntity target, Vec3 to, double maxSpeed, double lift) {
        double dx = to.x - target.getX();
        double dz = to.z - target.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 1.3) return;                                 // close enough: no piling up on one block
        double speed = Math.min(maxSpeed, distance * 0.4);
        target.setDeltaMovement(dx / distance * speed, lift, dz / distance * speed);
        target.hurtMarked = true;
    }

    /** The harmful potion effects on an entity right now (a copy of the list, so they can be removed while going through it). */
    public static List<MobEffectInstance> harmfulEffects(LivingEntity entity) {
        List<MobEffectInstance> harmful = new ArrayList<>();
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (!effect.getEffect().value().isBeneficial()) harmful.add(effect);
        }
        return harmful;
    }

    /**
     * A potion effect that is put on again and again while something lasts (a zone, an aura, a held beam):
     *   Spells.keepEffect(enemy, MobEffects.WITHER, 0, 60);      // call it every few ticks: withered while it lasts,
     *                                                           // and for up to 3 seconds after the last call
     * Unlike a plain addEffect it leaves the effect alone while it still has more than half of 'ticks' to run,
     * so wither and poison keep their beat (started afresh every few ticks they would never hurt at all), and it
     * never replaces a stronger effect by a weaker one. A higher 'amplifier' than the one it has takes over at once.
     */
    public static void keepEffect(LivingEntity entity, Holder<MobEffect> effect, int amplifier, int ticks) {
        MobEffectInstance has = entity.getEffect(effect);
        if (has != null && has.getAmplifier() >= amplifier
                && (has.getDuration() > ticks / 2 || has.isInfiniteDuration())) return;
        entity.addEffect(new MobEffectInstance(effect, ticks, amplifier));
    }

    /** Takes every harmful potion effect off the entity and puts out the fire on it. */
    public static void cleanse(LivingEntity entity) {
        for (MobEffectInstance effect : harmfulEffects(entity)) entity.removeEffect(effect.getEffect());
        entity.clearFire();
    }

    public static void sound(ServerLevel sl, Vec3 at, SoundEvent event, float volume, float pitch) {
        sl.playSound(null, at.x, at.y, at.z, event, SoundSource.PLAYERS, volume, pitch);
    }

    /** Takes 'cost' soul essence. If the player has too little it takes nothing, tells them and returns false. */
    public static boolean payEssence(Player player, float cost) {
        if (SoulCore.getSoulEssence(player) < cost) {
            player.sendSystemMessage(Component.literal("Not enough soul essence."));
            return false;
        }
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - cost);
        return true;
    }

    /** False once the caster died, logged out or went to another dimension: spells that stay around them stop then. */
    public static boolean casterStillHere(Player player, ServerLevel sl) {
        return player.isAlive() && !player.isRemoved() && player.level() == sl;
    }
}
