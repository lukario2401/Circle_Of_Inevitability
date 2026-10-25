package net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs;

import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Teleport;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.Concealment;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

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

    /**
     * On its side, so its abilities that help (a blessing, a heal, a shield) may reach it: itself, what it rides or
     * carries, its own pets and team, and a mob of its kind that is not fighting it: another monster for a monster,
     * else one of the same type. (Spells.isFriend asks this for a mob caster.)
     */
    public static boolean isFriend(Mob caster, LivingEntity other) {
        if (other == null || !other.isAlive() || other.isSpectator() || other instanceof ArmorStand) return false;
        if (other == caster || other == caster.getVehicle() || other.getVehicle() == caster) return true;
        if (caster.isAlliedTo(other)) return true;
        if (other instanceof OwnableEntity owned && owned.getOwnerReference() != null
                && caster.getUUID().equals(owned.getOwnerReference().getUUID())) return true;
        if (!(other instanceof Mob mob) || other == caster.getTarget() || other == caster.getLastHurtByMob()) return false;
        if (mob.getTarget() == caster || mob.getLastHurtByMob() == caster) return false;
        return caster instanceof Enemy ? other instanceof Enemy : other.getType() == caster.getType();
    }

    /** Every enemy whose body is within 'radius' of a point (Spells.enemiesAround, which asks isEnemy for a mob). */
    public static List<LivingEntity> enemiesAround(Mob caster, ServerLevel sl, Vec3 center, double radius) {
        return Spells.enemiesAround(caster, sl, center, radius);
    }

    /** Every enemy whose body (grown by 'grow' blocks) the line from..to passes through, nearest to 'from' first. */
    public static List<LivingEntity> enemiesOnLine(Mob caster, ServerLevel sl, Vec3 from, Vec3 to, double grow) {
        return Spells.enemiesOnLine(caster, sl, from, to, grow);
    }

    /** Its friends (isFriend) within 'radius' of a point, itself among them. */
    public static List<LivingEntity> friendsAround(Mob caster, ServerLevel sl, Vec3 center, double radius) {
        List<LivingEntity> result = new ArrayList<>();
        AABB area = new AABB(center, center).inflate(radius + 2.0);
        for (LivingEntity candidate : sl.getEntitiesOfClass(LivingEntity.class, area, other -> isFriend(caster, other))) {
            if (Spells.distanceToBody(candidate, center) <= radius) result.add(candidate);
        }
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

    /**
     * True if its ability may break, place or change blocks now: BeyonderMobRules.MOBS_CHANGE_BLOCKS, and the
     * mobGriefing game rule (asked through Forge, so mods that decide it per mob are heard). When false, an ability
     * still does everything else.
     */
    public static boolean mayChangeBlocks(LivingEntity caster, ServerLevel sl) {
        return BeyonderMobRules.MOBS_CHANGE_BLOCKS && ForgeEventFactory.getMobGriefingEvent(sl, caster);
    }

    /** The spot on the ground under its target (or under the target's feet in the air): where ground abilities land. */
    public static Vec3 groundUnder(Mob caster, ServerLevel sl, LivingEntity target) {
        return Spells.groundUnder(caster, sl, target);
    }

    /** Where to stand to strike its target: just before it, on its own side. Null: no room. */
    public static Vec3 spotBefore(Mob caster, ServerLevel sl, LivingEntity target) {
        Vec3 back = caster.position().subtract(target.position());
        Vec3 flat = new Vec3(back.x, 0, back.z);
        if (flat.lengthSqr() < 1.0E-4) return caster.position();
        return Spells.freeSpotNear(caster, sl, target.position(), flat.normalize(), target.getBbWidth() * 0.5 + 1.2);
    }

    /** It steps to 'feet' at once (a stride, a blink), safe from the fall after it. False: a root kept it there. */
    public static boolean stepTo(Mob caster, ServerLevel sl, Vec3 feet) {
        return Teleport.of(caster).fallGuard(40).go(sl, feet);
    }

    /** It is within melee reach of its target (its blows can land). */
    public static boolean inMelee(Mob caster, LivingEntity target) {
        return target != null && caster.distanceTo(target) <= caster.getBbWidth() * 0.5 + target.getBbWidth() * 0.5 + 2.5;
    }

    /** The weakest of its friends nearby (itself included) that is hurt: under 'share' of its health. Null: none. */
    public static LivingEntity hurtFriend(Mob caster, ServerLevel sl, double radius, float share) {
        LivingEntity worst = null;
        float lowest = share;
        for (LivingEntity friend : friendsAround(caster, sl, caster.position(), radius)) {
            float part = friend.getHealth() / Math.max(1f, friend.getMaxHealth());
            if (part < lowest) {
                lowest = part;
                worst = friend;
            }
        }
        return worst;
    }

    /** Its health is under this share of its max. */
    public static boolean hurt(LivingEntity caster, float share) {
        return caster.getHealth() < caster.getMaxHealth() * share;
    }

    // =========================
    // Player abilities written for any caster
    // =========================

    /** A wheel entry written for any caster (its aim is where the caster faces). */
    @FunctionalInterface
    public interface Entry {
        AbilityWheel.Result run(LivingEntity caster, ServerLevel sl);
    }

    /** A paid-for ability written for any caster (its aim is where the caster faces). */
    @FunctionalInterface
    public interface Core {
        void run(LivingEntity caster, ServerLevel sl);
    }

    /**
     * The mob version of a wheel entry that is written for any caster: the mob turns to its target, then the entry runs
     * as it does for a player (its aim is the mob's facing). Refused (REFUSED) = nothing spent.
     */
    public static MobAbility.Cast aimedEntry(Entry entry) {
        return (caster, target, sl) -> {
            if (target != null) face(caster, target);
            return entry.run(caster, sl) != AbilityWheel.Result.REFUSED;
        };
    }

    /** The same for an ability that cannot refuse once it is paid for. */
    public static MobAbility.Cast aimedCore(Core core) {
        return (caster, target, sl) -> {
            if (target != null) face(caster, target);
            core.run(caster, sl);
            return true;
        };
    }

    /**
     * The same for a player ability that pays for itself inside (Spells.paySpirituality) and returns nothing: it went
     * off if it paid, and it refused (nothing spent) if it did not. Its MobAbility.cost() must be what it pays.
     */
    public static MobAbility.Cast paidCore(Core core) {
        return (caster, target, sl) -> {
            if (target != null) face(caster, target);
            float before = MobSpirituality.prepaidLeft(caster);
            core.run(caster, sl);
            return MobSpirituality.prepaidLeft(caster) < before;
        };
    }

    /** A player's whole key, written for any caster: (caster, level, sl) as PathwayAbilities casts it. */
    @FunctionalInterface
    public interface Key {
        void run(LivingEntity caster, ServerLevel sl);
    }

    /**
     * A player's whole key run by the mob, its normal or its sneak form: the mob turns to its target, crouches for the
     * sneak form while the key runs (the key reads isShiftKeyDown, as for a player), and the key checks, pays (out of
     * what the mob's cast covers) and goes off, or refuses and nothing is spent. Its MobAbility.cost() must be what
     * that form pays.
     */
    public static MobAbility.Cast key(boolean sneak, Key key) {
        return (caster, target, sl) -> {
            if (target != null) face(caster, target);
            boolean crouched = caster.isShiftKeyDown();
            float before = MobSpirituality.prepaidLeft(caster);
            caster.setShiftKeyDown(sneak);
            try {
                key.run(caster, sl);
            } finally {
                caster.setShiftKeyDown(crouched);
            }
            return MobSpirituality.prepaidLeft(caster) < before;
        };
    }

    /** False once it died or went elsewhere: a delayed part of its ability (Later.run) stops then. */
    public static boolean casterStillHere(LivingEntity caster, ServerLevel sl) {
        return caster.isAlive() && !caster.isRemoved() && caster.level() == sl;
    }

    // =========================
    // Doing
    // =========================

    /**
     * Damage from the mob's ability (Spells.strike: a player takes BeyonderMobRules.DAMAGE_TO_PLAYERS of it, the
     * numbers being the player abilities', which were made against mobs). The short invulnerability after a hit is
     * cleared first, so quick hits in a row all count.
     */
    public static void strike(Mob caster, LivingEntity target, float damage) {
        Spells.strike(caster, target, damage);
    }
}
