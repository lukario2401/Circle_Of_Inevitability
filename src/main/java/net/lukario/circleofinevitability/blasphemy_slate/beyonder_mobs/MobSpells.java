package net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs;

import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.status.Concealment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The mob side of the combat kit (combat/Spells is written for player casters): who a casting mob fights, where it
 * aims, and how its abilities hurt. For the mob versions of abilities (MobAbility, MobAbilities).
 *
 *   MobSpells.face(mob, target);                                  // turn to it (abilities' looks use the facing)
 *   Vec3 eye = mob.getEyePosition();
 *   Vec3 to = MobSpells.lineEnd(mob, sl, eye, MobSpells.toward(mob, target), 28);
 *   for (LivingEntity hit : MobSpells.enemiesOnLine(mob, sl, eye, to, 0.5)) MobSpells.strike(mob, hit, 40f);
 *
 * Its enemies: what it is fighting (its target), whoever is hunting it, and, for a hostile mob, every player who is
 * not in creative or spectator. Never its own team, and never someone concealed.
 */
public final class MobSpells {

    private MobSpells() {}

    // =========================
    // Who
    // =========================

    /**
     * What it is fighting now, or null: its target, else whoever hurt it in the last 5 seconds (the Ender Dragon and
     * the Wither have no target of the usual kind). Alive, in its level, within BeyonderMobRules.FIGHT_RANGE, and
     * not in creative or spectator, nor concealed.
     */
    public static LivingEntity fightTarget(Mob mob) {
        LivingEntity target = mob.getTarget();
        if (target == null && mob.getLastHurtByMob() != null && mob.tickCount - mob.getLastHurtByMobTimestamp() < 100) {
            target = mob.getLastHurtByMob();
        }
        if (target == null || target == mob || !target.isAlive() || target.isSpectator() || target.level() != mob.level()) return null;
        if (target instanceof Player player && player.isCreative()) return null;
        if (Concealment.isHidden(target)) return null;
        if (mob.distanceTo(target) > BeyonderMobRules.FIGHT_RANGE) return null;
        return target;
    }

    public static boolean isEnemy(Mob caster, LivingEntity other) {
        if (other == null || other == caster || !other.isAlive() || other.isSpectator() || other instanceof ArmorStand) return false;
        if (Concealment.isHidden(other)) return false;
        if (caster.isAlliedTo(other)) return false;                               // same team
        if (other == caster.getTarget() || other == caster.getLastHurtByMob()) return true;
        if (other instanceof Mob hunter && hunter.getTarget() == caster) return true;
        return other instanceof Player player && !player.isCreative() && caster instanceof Enemy;
    }

    /** Every enemy whose body is within 'radius' of a point. */
    public static List<LivingEntity> enemiesAround(Mob caster, ServerLevel sl, Vec3 center, double radius) {
        List<LivingEntity> result = new ArrayList<>();
        AABB area = new AABB(center, center).inflate(radius + 2.0);
        for (LivingEntity candidate : sl.getEntitiesOfClass(LivingEntity.class, area, other -> isEnemy(caster, other))) {
            if (Spells.distanceToBody(candidate, center) <= radius) result.add(candidate);
        }
        return result;
    }

    /** Every enemy whose body (grown by 'grow' blocks) the line from..to passes through, nearest to 'from' first. */
    public static List<LivingEntity> enemiesOnLine(Mob caster, ServerLevel sl, Vec3 from, Vec3 to, double grow) {
        List<LivingEntity> result = new ArrayList<>();
        if (from.distanceToSqr(to) < 1.0E-6) return result;
        AABB area = new AABB(from, to).inflate(grow + 0.5);
        for (LivingEntity candidate : sl.getEntitiesOfClass(LivingEntity.class, area, other -> isEnemy(caster, other))) {
            AABB body = candidate.getBoundingBox().inflate(grow);
            if (body.contains(from) || body.clip(from, to).isPresent()) result.add(candidate);
        }
        result.sort(Comparator.comparingDouble(candidate -> candidate.getBoundingBox().getCenter().distanceToSqr(from)));
        return result;
    }

    /** True for that Sequence or a stronger one (a lower number), like Beyonder.hasSequence for players. */
    public static boolean hasSequence(LivingEntity mob, int sequence) {
        int own = BeyonderMobs.sequence(mob);
        return own >= 0 && own <= sequence;
    }

    // =========================
    // Where
    // =========================

    /** The middle of its body: where an ability aims at it. */
    public static Vec3 chestOf(LivingEntity entity) {
        return entity.getBoundingBox().getCenter();
    }

    /** From its eyes toward the target's chest (a unit vector). */
    public static Vec3 toward(LivingEntity caster, LivingEntity target) {
        Vec3 way = chestOf(target).subtract(caster.getEyePosition());
        return way.lengthSqr() < 1.0E-8 ? caster.getLookAngle() : way.normalize();
    }

    /** Turns it to face the target at once (body, head and look), so the looks of its ability point the right way. */
    public static void face(Mob caster, LivingEntity target) {
        Vec3 way = chestOf(target).subtract(caster.getEyePosition());
        float yaw = (float) (Mth.atan2(way.z, way.x) * (180.0 / Math.PI)) - 90f;
        float pitch = (float) -(Mth.atan2(way.y, Math.sqrt(way.x * way.x + way.z * way.z)) * (180.0 / Math.PI));
        caster.setYRot(yaw);
        caster.setYHeadRot(yaw);
        caster.yBodyRot = yaw;
        caster.setXRot(pitch);
        caster.getLookControl().setLookAt(target, 30f, 30f);
    }

    /** Where a line from 'from' along 'direction' stops: the first block, or 'range'. */
    public static Vec3 lineEnd(Mob caster, ServerLevel sl, Vec3 from, Vec3 direction, double range) {
        Vec3 far = from.add(direction.normalize().scale(range));
        return sl.clip(new ClipContext(from, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getLocation();
    }

    /** False once it died or went elsewhere: a delayed part of its ability (Later.run) stops then. */
    public static boolean casterStillHere(LivingEntity caster, ServerLevel sl) {
        return caster.isAlive() && !caster.isRemoved() && caster.level() == sl;
    }

    // =========================
    // Doing
    // =========================

    /**
     * Damage from the mob's ability. A player takes BeyonderMobRules.DAMAGE_TO_PLAYERS of it (the numbers are the
     * player abilities', which were made against mobs). The short invulnerability after a hit is cleared first, as
     * Spells.strike does, so quick hits in a row all count.
     */
    public static void strike(Mob caster, LivingEntity target, float damage) {
        if (!(caster.level() instanceof ServerLevel sl)) return;
        float dealt = target instanceof Player ? damage * BeyonderMobRules.DAMAGE_TO_PLAYERS : damage;
        if (dealt <= 0f) return;
        target.invulnerableTime = 0;
        target.hurt(sl.damageSources().mobAttack(caster), dealt);
        caster.setLastHurtMob(target);
    }
}
