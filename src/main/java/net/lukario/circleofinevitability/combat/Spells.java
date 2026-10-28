package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpirituality;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobRules;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.lukario.circleofinevitability.status.Cleansing;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Unnoticed;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The questions every ability asks, answered once: who is an enemy, what am I aiming at, where is the ground,
 * who stands in this circle or on this line. Any class can call these from its abilities:
 *
 *   LivingEntity target = Spells.aimEnemy(player, sl, 24);          // the enemy under the crosshair, or null
 *   Vec3 spot = Spells.aimGround(player, sl, 24);                   // the ground the player is aiming at
 *   Vec3 direction = Spells.aimFrom(player, sl, hand, 24);          // from the hand toward what the crosshair is on
 *   for (LivingEntity e : Spells.enemiesAround(player, sl, spot, 4)) Spells.strike(player, e, 12f);
 *   if (!Spells.paySpirituality(player, 1000)) return;                   // takes the spirituality, or says there is not enough
 *   Spells.alliesAround(player, sl, spot, 6)                        // the caster's side there (livingAround: everyone)
 *   Spells.strikeThrough(player, target, 90f)                       // a hit no shield, substitute or death ward stops
 *   BlockPos light = Spells.lightAimed(player, sl, 32, 4, 10);      // the light source nearest the crosshair, or null
 *   BlockPos mirror = Spells.reflectionAimed(player, sl, 32, 3);    // the reflection nearest the crosshair, or null
 *   if (Spells.isFullMoonNight(sl)) ...                             // night, and the moon is full
 *   Spells.looseThingsAround(player, sl, spot, 16)                  // dropped items, arrows, boats... to throw (combat/Hurled)
 *
 * "Enemy" means: anything alive except the caster, what the caster rides or owns (pets, tamed horses), the
 * caster's marionettes, team mates, armor stands, spectators and whoever is concealed. Add your own rule with
 * Spells.addAllyCheck(...), e.g. for summons of a new class.
 *
 * Every caster here may also be a mob (a Beyonder mob's ability: blasphemy_slate/beyonder_mobs). Then its enemies
 * are only what it is really fighting (MobSpells.isEnemy), its friends its own kind (MobSpells.isFriend), its aim
 * is where it faces (MobSpells.face turns it first), and a player takes BeyonderMobRules.DAMAGE_TO_PLAYERS of its
 * strikes. paySpirituality, costFor and the rules added with addAllyCheck and addCostScale stay players' only.
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

    /**
     * For a player caster: everyone not on their side (isFriend). For a mob caster (a Beyonder mob's ability): only
     * what it is really fighting, as blasphemy_slate/beyonder_mobs/MobSpells.isEnemy says, so a zombie's wave does
     * not hurt the other zombies, nor the cows standing by.
     */
    public static boolean isEnemy(LivingEntity caster, LivingEntity other) {
        if (!other.isAlive() || other.isSpectator() || other instanceof ArmorStand) return false;
        if (Concealment.isHidden(other)) return false;
        if (!(caster instanceof Player)) return caster instanceof Mob mob && MobSpells.isEnemy(mob, other);
        return !isFriend(caster, other);
    }

    /**
     * On the caster's side: the caster themself (also their new body after respawning), what they ride or own, their
     * marionettes, team mates and every addAllyCheck rule. Unlike !isEnemy, a concealed body is judged as if it were
     * seen: for a wall that must keep hidden enemies out too (Hand Of God's Protection from Sequence 1).
     */
    public static boolean isFriend(LivingEntity caster, LivingEntity other) {
        // the UUID check also covers the caster's new body after dying and respawning while a spell is still running
        if (other == caster || other.getUUID().equals(caster.getUUID())) return true;
        if (other == caster.getVehicle()) return true;
        if (other instanceof OwnableEntity owned && owned.getOwnerReference() != null && caster.getUUID().equals(owned.getOwnerReference().getUUID())) return true;
        if (Marionette.isMarionetteOf(other, caster)) return true;
        if (caster.isAlliedTo(other)) return true;                // same scoreboard team
        if (!(caster instanceof Player player)) return caster instanceof Mob mob && MobSpells.isFriend(mob, other);
        for (AllyCheck check : ALLY_CHECKS) {
            if (check.isAlly(player, other)) return true;
        }
        return false;
    }

    /** Says whether something is evil, beyond what isEvil already knows. */
    @FunctionalInterface
    public interface EvilCheck {
        boolean isEvil(LivingEntity entity);
    }

    private static final List<EvilCheck> EVIL_CHECKS = new ArrayList<>();

    /** One more kind of evil thing (a Pathway's corrupted, its demons): Spells.addEvilCheck(entity -> ...). */
    public static void addEvilCheck(EvilCheck check) {
        EVIL_CHECKS.add(check);
    }

    /**
     * Evil, for "burns evil and undead" in one call: the undead, every monster (anything the game counts as an
     * enemy of players: zombies, spiders, slimes, illagers...) and whatever an addEvilCheck rule names.
     */
    public static boolean isEvil(LivingEntity entity) {
        if (entity.is(EntityTypeTags.UNDEAD) || entity instanceof Enemy) return true;
        for (EvilCheck check : EVIL_CHECKS) {
            if (check.isEvil(entity)) return true;
        }
        return false;
    }

    /** Every enemy whose body is within 'radius' of a point. */
    public static List<LivingEntity> enemiesAround(LivingEntity caster, ServerLevel sl, Vec3 center, double radius) {
        List<LivingEntity> result = new ArrayList<>();
        AABB area = new AABB(center, center).inflate(radius + 2.0);
        for (LivingEntity candidate : sl.getEntitiesOfClass(LivingEntity.class, area, other -> isEnemy(caster, other))) {
            if (distanceToBody(candidate, center) <= radius) result.add(candidate);
        }
        return result;
    }

    /**
     * Every living body whose body is within 'radius' of a point, enemies and allies alike: everything alive except
     * the caster, spectators and armor stands (a sight that sees all, a splash that hurts some and helps others).
     */
    public static List<LivingEntity> livingAround(LivingEntity caster, ServerLevel sl, Vec3 center, double radius) {
        List<LivingEntity> result = new ArrayList<>();
        AABB area = new AABB(center, center).inflate(radius + 2.0);
        for (LivingEntity candidate : sl.getEntitiesOfClass(LivingEntity.class, area, other -> other != caster
                && !other.getUUID().equals(caster.getUUID()) && other.isAlive() && !other.isSpectator() && !(other instanceof ArmorStand))) {
            if (distanceToBody(candidate, center) <= radius) result.add(candidate);
        }
        return result;
    }

    /**
     * The caster's side within 'radius' of a point: everyone livingAround finds that is not an enemy (team mates,
     * pets, marionettes and summons, other players' concealed bodies). The caster is not in it; add them yourself.
     */
    public static List<LivingEntity> alliesAround(LivingEntity caster, ServerLevel sl, Vec3 center, double radius) {
        List<LivingEntity> result = livingAround(caster, sl, center, radius);
        result.removeIf(other -> isEnemy(caster, other));
        return result;
    }

    /**
     * Projectiles in flight within 'radius' of a point that are not the caster's side's: arrows, fireballs, tridents,
     * pearls (one with no owner counts too), but not one lying still or stuck in a wall. For fields that slow, stop,
     * hang or bend what is shot through them (Star Dragon's Time Dilation, the storm, Gravity Authority, the black hole).
     */
    public static List<Projectile> hostileShotsAround(LivingEntity caster, ServerLevel sl, Vec3 center, double radius) {
        AABB area = new AABB(center, center).inflate(radius);
        List<Projectile> result = new ArrayList<>();
        for (Projectile shot : sl.getEntitiesOfClass(Projectile.class, area, Entity::isAlive)) {
            if (shot.position().distanceToSqr(center) > radius * radius) continue;
            if (shot.getDeltaMovement().lengthSqr() < 0.0004) continue;                   // lying still, stuck
            if (shot.getOwner() == caster || shot.getOwner() instanceof LivingEntity owner && isFriend(caster, owner)) continue;
            result.add(shot);
        }
        return result;
    }

    /**
     * The loose things within 'radius' of a point that a power could lift and throw (combat/Hurled), nearest first:
     * dropped items, projectiles (flying or stuck in a wall), armor stands, and boats, minecarts and the like that
     * nobody rides. Never anything alive and never blocks; things already being thrown are left out.
     */
    public static List<Entity> looseThingsAround(LivingEntity caster, ServerLevel sl, Vec3 center, double radius) {
        AABB area = new AABB(center, center).inflate(radius + 1.0);
        List<Entity> result = new ArrayList<>(sl.getEntities((Entity) null, area,
                entity -> isLoose(caster, entity) && entity.position().distanceToSqr(center) <= radius * radius));
        result.sort(Comparator.comparingDouble(entity -> entity.position().distanceToSqr(center)));
        return result;
    }

    /** True for a loose thing (see looseThingsAround). */
    public static boolean isLoose(LivingEntity caster, Entity entity) {
        if (!entity.isAlive() || entity == caster || entity.isPassenger() || entity.isVehicle() || Hurled.isFlying(entity)) return false;
        if (entity instanceof ItemEntity || entity instanceof Projectile || entity instanceof ArmorStand) return true;
        return !(entity instanceof LivingEntity) && entity.isPushable();                // boats, minecarts
    }

    /** Every enemy whose body (grown by 'grow' blocks) the line from..to passes through, nearest to 'from' first. */
    public static List<LivingEntity> enemiesOnLine(LivingEntity caster, ServerLevel sl, Vec3 from, Vec3 to, double grow) {
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
    public static List<LivingEntity> enemiesInSight(LivingEntity caster, ServerLevel sl, double range, double halfAngle) {
        Vec3 eye = caster.getEyePosition();
        List<LivingEntity> result = enemiesInCone(caster, sl, eye, caster.getLookAngle(), range, halfAngle);
        result.removeIf(candidate -> !canBeChosen(caster, candidate));     // seen, but no mind takes it in (status/Unnoticed)
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
    public static List<LivingEntity> enemiesInCone(LivingEntity caster, ServerLevel sl, Vec3 from, Vec3 direction,
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
    public static void wave(LivingEntity caster, ServerLevel sl, Vec3 ground, double radius, int ticks, double height,
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
    public static LivingEntity firstEnemyOnLine(LivingEntity caster, ServerLevel sl, Vec3 from, Vec3 to, double grow) {
        List<LivingEntity> onLine = enemiesOnLine(caster, sl, from, to, grow);
        return onLine.isEmpty() ? null : onLine.get(0);
    }

    /** Says that 'chooser' cannot pick 'target' as the target of an ability right now. 'chooser' may be null (unknown). */
    @FunctionalInterface
    public interface UnchoosableCheck {
        boolean cannotChoose(Player chooser, LivingEntity target);
    }

    private static final List<UnchoosableCheck> UNCHOOSABLE_CHECKS = new ArrayList<>();

    /**
     * One more reason an ability cannot pick someone, beyond Unnoticed: a veiled area, a sealed world, a Pathway that
     * divination slips off. E.g. (chooser, target) -> MyVeils.covers(target) && !MyVeils.covers(chooser).
     */
    public static void addUnchoosableCheck(UnchoosableCheck check) {
        UNCHOOSABLE_CHECKS.add(check);
    }

    /**
     * False for an enemy an ability cannot CHOOSE as its target, though it is there and can be caught by what hits
     * an area: one whose mind is concealed (status/Unnoticed), or one an addUnchoosableCheck rule hides. The aiming
     * here (aimEnemy, aimFrom, aimGround, enemiesInSight) already passes over it; ask this yourself when you pick a
     * target some other way.
     */
    public static boolean canBeChosen(LivingEntity entity) {
        return canBeChosen(null, entity);
    }

    /** The same, when you know who is choosing (some rules only hide it from those outside, or from enemies). */
    public static boolean canBeChosen(LivingEntity chooser, LivingEntity entity) {
        if (!Unnoticed.canBeChosen(entity)) return false;
        Player asking = chooser instanceof Player player ? player : null;      // a mob choosing counts as unknown
        for (UnchoosableCheck check : UNCHOOSABLE_CHECKS) {
            if (check.cannotChoose(asking, entity)) return false;
        }
        return true;
    }

    /** The enemy on the line closest to 'from' that can be chosen as a target (canBeChosen), or null. */
    private static LivingEntity firstChosenOnLine(LivingEntity caster, ServerLevel sl, Vec3 from, Vec3 to, double grow) {
        for (LivingEntity candidate : enemiesOnLine(caster, sl, from, to, grow)) {
            if (canBeChosen(caster, candidate)) return candidate;
        }
        return null;
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
    public static LivingEntity aimEnemy(LivingEntity caster, ServerLevel sl, double range) {
        Vec3 eye = caster.getEyePosition();
        return firstChosenOnLine(caster, sl, eye, aimPoint(caster, sl, range), 0.4);
    }

    /**
     * The ally the caster is looking at within range (not through walls), or null: the friendly half of aimEnemy, for
     * a ward pinned on a friend, a blessing, a step back sent to someone else. Allies are the caster's side
     * (isFriend: team mates, pets, marionettes, summons); the caster themself is never it.
     */
    public static LivingEntity aimAlly(LivingEntity caster, ServerLevel sl, double range) {
        Vec3 eye = caster.getEyePosition();
        Vec3 to = aimPoint(caster, sl, range);
        if (eye.distanceToSqr(to) < 1.0E-6) return null;
        LivingEntity nearest = null;
        double best = Double.MAX_VALUE;
        AABB area = new AABB(eye, to).inflate(0.9);
        for (LivingEntity candidate : sl.getEntitiesOfClass(LivingEntity.class, area, other -> other != caster
                && !other.getUUID().equals(caster.getUUID()) && other.isAlive() && !other.isSpectator()
                && !(other instanceof ArmorStand) && isFriend(caster, other))) {
            AABB body = candidate.getBoundingBox().inflate(0.4);
            if (!body.contains(eye) && body.clip(eye, to).isEmpty()) continue;
            double distance = candidate.getBoundingBox().getCenter().distanceToSqr(eye);
            if (distance < best) {
                best = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    /** Where the crosshair ends: on the first block in the way, or 'range' blocks out if there is none. */
    public static Vec3 aimPoint(LivingEntity caster, ServerLevel sl, double range) {
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(range));
        return sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getLocation();
    }

    /**
     * The direction from 'from' to what the crosshair is on (the first enemy in the way, else the block, else the
     * far end of the range). Throw things that start BESIDE the caster along this (from a hand, a shoulder, a summon
     * standing next to them) and they land where the caster is aiming instead of a step to the side of it.
     */
    public static Vec3 aimFrom(LivingEntity caster, ServerLevel sl, Vec3 from, double range) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 point = aimPoint(caster, sl, range);
        LivingEntity target = firstChosenOnLine(caster, sl, eye, point, 0.4);
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
    public static Vec3 aimGround(LivingEntity caster, ServerLevel sl, double range) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 stop = aimPoint(caster, sl, range);
        LivingEntity target = firstChosenOnLine(caster, sl, eye, stop, 0.4);
        if (target != null) return groundAt(caster, sl, target.position().add(0, 0.5, 0));
        return groundAt(caster, sl, stop.subtract(look.scale(0.3)));           // a step back out of the block that was hit
    }

    /**
     * Top of the blocks under a point. The point itself if there is nothing within 24 blocks below it.
     * It looks down from a little above the point, so a point lying on the ground still finds it. A look that
     * starts inside a block "hits" at once, so when that happens it tries again from other heights:
     * from the point itself (the point is just under a ceiling), then from higher up (the point is inside a hill).
     */
    public static Vec3 groundAt(LivingEntity caster, ServerLevel sl, Vec3 point) {
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
    public static Vec3 groundNear(LivingEntity caster, ServerLevel sl, Vec3 spot, double reach) {
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
    public static Vec3 clearStart(LivingEntity caster, ServerLevel sl, Vec3 wanted) {
        Vec3 eye = caster.getEyePosition();
        BlockHitResult hit = sl.clip(new ClipContext(eye, wanted, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (hit.getType() == HitResult.Type.MISS) return wanted;
        Vec3 back = eye.subtract(hit.getLocation());
        return back.lengthSqr() < 0.04 ? eye : hit.getLocation().add(back.normalize().scale(0.15));
    }

    /** True when nothing solid is between the two points. */
    public static boolean clearLine(LivingEntity caster, ServerLevel sl, Vec3 from, Vec3 to) {
        return sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster)).getType()
                == HitResult.Type.MISS;
    }

    /**
     * A place within 'distance' blocks of 'from', in (or near) the horizontal direction 'preferred', where the
     * player fits and that can be seen from 'from': for blinks, dodges and swaps. It tries the preferred direction
     * first, then turns away from it step by step. Null if every direction is walled in.
     */
    public static Vec3 freeSpotNear(LivingEntity player, ServerLevel sl, Vec3 from, Vec3 preferred, double distance) {
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
    public static Vec3 blinkSpot(LivingEntity player, ServerLevel sl, double range) {
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
    public static Vec3 groundUnder(LivingEntity caster, ServerLevel sl, LivingEntity entity) {
        if (entity.onGround()) return entity.position();
        Vec3 ground = groundAt(caster, sl, entity.position());
        return entity.getY() - ground.y <= 4.0 ? ground : entity.position();
    }

    /** The spot 'distance' blocks from 'center' the way 'yaw' looks, at the same height: for rings of things around a point. */
    public static Vec3 spotAround(Vec3 center, float yaw, double distance) {
        return center.add(Vec3.directionFromRotation(0f, yaw).scale(distance));
    }

    /** True if the player's body fits with their feet on this spot (for teleports: never into a wall). */
    public static boolean fits(LivingEntity player, ServerLevel sl, Vec3 feet) {
        return sl.noCollision(player, player.getBoundingBox().move(feet.subtract(player.position())));
    }

    // =========================
    // Light
    // =========================

    /** Furthest lightNear looks (blocks): every block in the cube is checked, so this stays small. */
    public static final double LIGHT_SEARCH_LIMIT = 8.0;

    /**
     * The blocks that give off at least 'minLight' light (torch 14, lantern 15, glowstone 15, lava 15, a candle 3 to
     * 12, redstone torch 7) within 'radius' blocks of a point, nearest first. Empty if there is none. 'radius' is
     * capped at LIGHT_SEARCH_LIMIT.
     *
     *   List<BlockPos> lights = Spells.lightNear(sl, player.position(), 6, 10);
     */
    public static List<BlockPos> lightNear(ServerLevel sl, Vec3 center, double radius, int minLight) {
        double reach = Math.min(radius, LIGHT_SEARCH_LIMIT);
        int r = (int) Math.ceil(reach);
        BlockPos middle = BlockPos.containing(center);
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(middle.offset(-r, -r, -r), middle.offset(r, r, r))) {
            if (new Vec3(
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5
            ).distanceTo(center) > reach) continue;
            BlockState state = sl.getBlockState(pos);
            if (state.isAir()) continue;
            if (state.getLightEmission(sl, pos) >= minLight) found.add(pos.immutable());
        }
        found.sort(Comparator.comparingDouble(pos ->
                new Vec3(
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5
                ).distanceToSqr(center)
        ));
        return found;
    }

    /**
     * The light source (at least 'minLight', see lightNear) nearest to what the crosshair is on, within 'around'
     * blocks of it, as far as 'range' away. Null if there is none.
     */
    public static BlockPos lightAimed(LivingEntity caster, ServerLevel sl, double range, double around, int minLight) {
        Vec3 point = aimPoint(caster, sl, range);
        List<BlockPos> lights = lightNear(sl, point, around, minLight);
        return lights.isEmpty() ? null : lights.get(0);
    }

    /** True by day for a spot under the open sky (sunlight counts as a light source). */
    public static boolean inDaylight(ServerLevel sl, BlockPos pos) {
        return !sl.isDarkOutside() && sl.canSeeSky(pos);
    }

    // =========================
    // The moon
    // =========================

    /** Days in one round of the moon: full on the first night, then every eighth. */
    public static final int MOON_DAYS = 8;

    /**
     * True at night when the moon is full (the first night of the world, and every eighth after it). The day is
     * counted from the world's age (game time), so a /time set or a night slept through moves the moon in the sky
     * but not this: it stays the same for everyone and cannot be skipped by sleeping.
     */
    public static boolean isFullMoonNight(ServerLevel sl) {
        return sl.isDarkOutside() && Math.floorMod(sl.getGameTime() / 24000L, (long) MOON_DAYS) == 0;
    }

    // =========================
    // Reflections
    // =========================

    /** Polished metal that shows a reflection (by block name). */
    private static final Set<String> MIRROR_METALS = Set.of("iron_block", "gold_block", "netherite_block",
            "copper_block", "waxed_copper_block", "diamond_block");

    /**
     * True for a block that shows a reflection: still water at the surface (a source block with no water on top),
     * ice of every kind, glass and glass panes (stained and tinted too), and blocks of polished metal (iron, gold,
     * copper that has not weathered, netherite, diamond). For walking through mirrors.
     */
    public static boolean isReflective(ServerLevel sl, BlockPos pos) {
        FluidState fluid = sl.getFluidState(pos);
        if (fluid.is(FluidTags.WATER)) return fluid.isSource() && !sl.getFluidState(pos.above()).is(FluidTags.WATER);
        BlockState state = sl.getBlockState(pos);
        if (state.isAir()) return false;
        if (state.is(BlockTags.ICE)) return true;
        String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return name.contains("glass") || MIRROR_METALS.contains(name);
    }

    /**
     * The reflective blocks (see isReflective) within 'radius' blocks of a point, nearest first. Empty if there is
     * none. 'radius' is capped at LIGHT_SEARCH_LIMIT, as for lightNear.
     *
     *   List<BlockPos> mirrors = Spells.reflectionNear(sl, player.position(), 6);
     */
    public static List<BlockPos> reflectionNear(ServerLevel sl, Vec3 center, double radius) {
        double reach = Math.min(radius, LIGHT_SEARCH_LIMIT);
        int r = (int) Math.ceil(reach);
        BlockPos middle = BlockPos.containing(center);
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(middle.offset(-r, -r, -r), middle.offset(r, r, r))) {
            if (Vec3.atCenterOf(pos).distanceTo(center) > reach) continue;
            if (isReflective(sl, pos)) found.add(pos.immutable());
        }
        found.sort(Comparator.comparingDouble(pos -> Vec3.atCenterOf(pos).distanceToSqr(center)));
        return found;
    }

    /**
     * The reflection nearest to what the crosshair is on, within 'around' blocks of it, as far as 'range' away. The
     * crosshair stops on the surface of still water here (unlike aimPoint), so looking at a lake finds the lake.
     * Null if there is none.
     */
    public static BlockPos reflectionAimed(LivingEntity caster, ServerLevel sl, double range, double around) {
        Vec3 eye = caster.getEyePosition();
        Vec3 end = eye.add(caster.getLookAngle().scale(range));
        BlockHitResult hit = sl.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY, caster));
        Vec3 point = hit.getLocation();
        if (hit.getType() == HitResult.Type.BLOCK && isReflective(sl, hit.getBlockPos())) return hit.getBlockPos().immutable();
        List<BlockPos> mirrors = reflectionNear(sl, point, around);
        return mirrors.isEmpty() ? null : mirrors.get(0);
    }

    // =========================
    // Directions
    // =========================

    /** The yaw that faces along a direction. */
    public static float yawOf(Vec3 direction) {
        return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
    }

    /** Horizontal direction the player (or any caster) faces. */
    public static Vec3 flatLook(Entity player) {
        return Vec3.directionFromRotation(0f, player.getYRot());
    }

    /** Horizontal direction to the player's right. */
    public static Vec3 rightOf(Entity player) {
        return flatLook(player).cross(UP).normalize();
    }

    /** 'direction' turned left/right by 'degrees' around the world's up (for fans of projectiles). */
    public static Vec3 turned(Vec3 direction, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(direction.x * cos - direction.z * sin, direction.y, direction.x * sin + direction.z * cos);
    }

    /**
     * 'from' turned toward 'to' by at most 'maxDegrees' (in any direction, up and down too): steering something
     * that cannot turn on the spot, a tick at a time. Both are directions; the result is one too (length 1).
     *   heading = Spells.turnToward(heading, player.getLookAngle(), 12);   // follows the crosshair, 12 degrees a tick
     */
    public static Vec3 turnToward(Vec3 from, Vec3 to, double maxDegrees) {
        Vec3 a = from.normalize();
        Vec3 b = to.normalize();
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, a.dot(b))));
        double max = Math.toRadians(Math.max(0.0, maxDegrees));
        if (angle <= max) return b;
        double sin = Math.sin(angle);
        if (sin < 1.0E-4) {
            // turning right round: any way is as good, so go over the side
            Vec3 side = Math.abs(a.y) > 0.99 ? new Vec3(1, 0, 0) : a.cross(UP).normalize();
            return a.scale(Math.cos(max)).add(side.scale(Math.sin(max))).normalize();
        }
        double t = max / angle;                                       // the part of the way it gets this time
        return a.scale(Math.sin((1 - t) * angle) / sin).add(b.scale(Math.sin(t * angle) / sin)).normalize();
    }

    // =========================
    // Doing things
    // =========================

    /**
     * Damage from the caster. Clears the short invulnerability after a hit first, so quick hits in a row all count.
     * The target also becomes "what the caster last hit", which is what marionettes and tamed wolves go after.
     */
    public static void strike(LivingEntity caster, LivingEntity target, float damage) {
        // a mob's ability numbers are the players', which were made against mobs: a player takes a share of them
        if (!(caster instanceof Player) && target instanceof Player) damage *= BeyonderMobRules.DAMAGE_TO_PLAYERS;
        if (damage <= 0f) return;
        target.invulnerableTime = 0;
        SPELL_HITS.add(target.getUUID());
        try {
            target.hurt(caster instanceof Player player ? caster.damageSources().playerAttack(player)
                    : caster.damageSources().mobAttack(caster), damage);
        } finally {
            SPELL_HITS.remove(target.getUUID());
        }
        caster.setLastHurtMob(target);
    }

    /** The entities a Spells.strike is landing on right now (only during the strike itself). */
    private static final Set<UUID> SPELL_HITS = new HashSet<>();

    /**
     * True while an ability's strike (strike, strikeThrough, strikeTrue) is landing on this entity. Such a hit looks
     * like a player's attack to the game; this tells it from a swing (an Imperative: Reality weakens only abilities).
     */
    public static boolean isSpellHit(LivingEntity entity) {
        return !SPELL_HITS.isEmpty() && SPELL_HITS.contains(entity.getUUID());
    }

    /** The entities a strikeThrough is landing on right now (only during the strike itself). */
    private static final Set<UUID> PIERCING = new HashSet<>();

    /**
     * A strike that nothing stops: a Dome's shield does not soften it, a Substitute does not take it and a DeathWard
     * does not save from it (none of them is used up either). For a sword of Glory, a divine judgement.
     */
    public static void strikeThrough(LivingEntity caster, LivingEntity target, float damage) {
        PIERCING.add(target.getUUID());
        try {
            strike(caster, target, damage);
        } finally {
            PIERCING.remove(target.getUUID());
        }
    }

    /** What a strikeTrue lands with, by the entity it is landing on (only during the strike itself; -1 = not yet known). */
    private static final Map<UUID, Float> TRUE_DAMAGE = new HashMap<>();

    /**
     * A strikeThrough that nothing softens either: a raised shield or block stance is dropped first, and the target
     * loses what the hit comes to after every damage event has had its say (bonuses such as Vulnerable or a Pathway's
     * might, and the target's own Pathway defenses) but before armor, Protection, Resistance and absorption hearts
     * take anything off. Something that cancels the hit outright (Concealment, an Exile, a miss) still stops it.
     * For a decree, an execution, a word that cannot be argued with.
     */
    public static void strikeTrue(LivingEntity caster, LivingEntity target, float damage) {
        target.stopUsingItem();
        TRUE_DAMAGE.put(target.getUUID(), -1f);
        try {
            strikeThrough(caster, target, damage);
        } finally {
            TRUE_DAMAGE.remove(target.getUUID());
        }
    }

    /** The two halves of Spells.strikeTrue: what the hit comes to before armor, and that amount landing whole. */
    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class TrueDamageEvents {
        private TrueDamageEvents() {}

        @SubscribeEvent(priority = Priority.LOWEST)
        public static void onSpellsTrueHurt(LivingHurtEvent event) {
            if (TRUE_DAMAGE.isEmpty() || event.getEntity().level().isClientSide()) return;
            if (TRUE_DAMAGE.containsKey(event.getEntity().getUUID())) TRUE_DAMAGE.put(event.getEntity().getUUID(), event.getAmount());
        }

        @SubscribeEvent(priority = Priority.LOW)
        public static void onSpellsTrueDamage(LivingDamageEvent event) {
            if (TRUE_DAMAGE.isEmpty() || event.getEntity().level().isClientSide()) return;
            Float exact = TRUE_DAMAGE.get(event.getEntity().getUUID());
            if (exact != null && exact >= 0f) event.setAmount(exact);
        }
    }

    /** True while a strikeThrough is landing on this entity: shields and wards must let it through. */
    public static boolean isPiercing(LivingEntity entity) {
        return !PIERCING.isEmpty() && PIERCING.contains(entity.getUUID());
    }

    /** Throws the target away from a point: 'strength' blocks per tick sideways, 'lift' upward. */
    public static void push(LivingEntity target, Vec3 from, double strength, double lift) {
        if (cannotBeMoved(target)) return;
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
        if (cannotBeMoved(target)) return;
        double dx = to.x - target.getX();
        double dz = to.z - target.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance < 1.3) return;                                 // close enough: no piling up on one block
        double speed = Math.min(maxSpeed, distance * 0.4);
        target.setDeltaMovement(dx / distance * speed, lift, dz / distance * speed);
        target.hurtMarked = true;
    }

    private static final List<Predicate<LivingEntity>> UNMOVABLE = new ArrayList<>();

    /**
     * Once, in a static block: while it says true for an entity, push and pull leave it where it is (and so does every
     * field, wave and shove built on them). A body too dense to move, an anchor, a giant's footing. Star Dragon's Dense
     * Body and Heavybringing.
     */
    public static void addMoveImmunity(Predicate<LivingEntity> unmovable) {
        UNMOVABLE.add(unmovable);
    }

    /** True while an addMoveImmunity rule keeps it in place. */
    public static boolean cannotBeMoved(LivingEntity entity) {
        for (Predicate<LivingEntity> rule : UNMOVABLE) if (rule.test(entity)) return true;
        return false;
    }

    /** A mob with more than this maximum health counts as a boss (isBoss). */
    public static float BOSS_HEALTH = 150f;
    /** Holds (lifted, caged, slowed in time...) last this share on a boss, and this share on a player (holdLength). */
    public static float BOSS_HOLD = 0.5f;
    public static float PLAYER_HOLD = 0.7f;

    /** A boss: the dragon, the wither, the warden, an elder guardian, or anything with more than BOSS_HEALTH. */
    public static boolean isBoss(LivingEntity one) {
        return isNaturalBoss(one) || one.getMaxHealth() > BOSS_HEALTH;
    }

    /** Named vanilla bosses, independent of a spell's configurable execution-health threshold. */
    public static boolean isNaturalBoss(LivingEntity one) {
        return one.getType() == EntityTypes.ENDER_DRAGON || one.getType() == EntityTypes.WITHER
                || one.getType() == EntityTypes.WARDEN || one.getType() == EntityTypes.ELDER_GUARDIAN;
    }

    /**
     * How long a hold of 'ticks' (lifted, caged, voided, frozen in time) lasts on this one: half on a boss, 70% on a
     * player, all of it on anything else. The one place for that rule, so every Pathway that wants it agrees.
     */
    public static int holdLength(LivingEntity target, int ticks) {
        float share = isBoss(target) ? BOSS_HOLD : target instanceof Player ? PLAYER_HOLD : 1f;
        return Math.max(1, Math.round(ticks * share));
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

    /**
     * 'spots' random places on the ground within 'radius' of 'ground' grow as if bone meal were used on them:
     * crops ripen, saplings grow, grass sprouts flowers. Call it every second for a garden that grows while it lasts.
     * Returns how many of them grew. (It changes blocks: only plants, the way bone meal does.)
     */
    public static int growPlants(LivingEntity caster, ServerLevel sl, Vec3 ground, double radius, int spots) {
        ItemStack meal = new ItemStack(Items.BONE_MEAL, 64);
        int grew = 0;
        for (int i = 0; i < spots; i++) {
            double angle = sl.getRandom().nextDouble() * Math.PI * 2;
            double far = Math.sqrt(sl.getRandom().nextDouble()) * radius;
            Vec3 spot = groundNear(caster, sl, ground.add(Math.cos(angle) * far, 0, Math.sin(angle) * far), 3);
            if (spot == null) continue;
            BlockPos above = BlockPos.containing(spot.x, spot.y + 0.1, spot.z);       // a crop or sapling standing there
            if (BoneMealItem.growCrop(meal, sl, above) || BoneMealItem.growCrop(meal, sl, above.below())) grew++;   // or the grass itself
            if (meal.isEmpty()) meal = new ItemStack(Items.BONE_MEAL, 64);
        }
        return grew;
    }

    /**
     * Takes every harmful potion effect off the entity, puts out the fire on it, and takes off every status that
     * listens to status/Cleansing (Corrosion, and any status of your own that calls Cleansing.addListener).
     */
    public static void cleanse(LivingEntity entity) {
        for (MobEffectInstance effect : harmfulEffects(entity)) entity.removeEffect(effect.getEffect());
        entity.clearFire();
        Cleansing.cleanseStatuses(entity);
    }

    public static void sound(ServerLevel sl, Vec3 at, SoundEvent event, float volume, float pitch) {
        sl.playSound(null, at.x, at.y, at.z, event, SoundSource.PLAYERS, volume, pitch);
    }

    /** A rule that makes every ability cost more or less for a player: 1.5f = half again as much, 0.8f = 20% less. */
    @FunctionalInterface
    public interface CostScale {
        float scale(Player player);
    }

    private static final List<CostScale> COST_SCALES = new ArrayList<>();

    /**
     * One more rule for what every ability of every Pathway costs this player (an eroded mind pays half again, an
     * enlightened one 20% less, allies in a Divine Kingdom 30% less). Once, in a static block. paySpirituality and the
     * wheels ask it; a stolen cast (Beyonder.castUnbound) is never scaled.
     */
    public static void addCostScale(CostScale rule) {
        COST_SCALES.add(rule);
    }

    /** What 'cost' really costs this player now, after every addCostScale rule. */
    public static float costFor(Player player, float cost) {
        if (cost <= 0f || COST_SCALES.isEmpty() || Beyonder.isCastingUnbound(player)) return cost;
        float scale = 1f;
        for (CostScale rule : COST_SCALES) scale *= Math.max(0f, rule.scale(player));
        return cost * scale;
    }

    /**
     * Takes 'cost' spirituality (after every addCostScale rule). If the player has too little it takes nothing, tells
     * them and returns false.
     */
    public static boolean paySpirituality(Player player, float cost) {
        float due = costFor(player, cost);
        if (SoulCore.getSpirituality(player) < due) {
            player.sendSystemMessage(Component.translatable("message.circle_of_inevitability.not_enough_spirituality"));
            return false;
        }
        SoulCore.setSpirituality(player, SoulCore.getSpirituality(player) - due);
        return true;
    }

    /**
     * The same for any caster: a player as above; a Beyonder mob out of what its running cast covers (MobCasting pays
     * the ability's cost), the rest from its own bar (an upkeep later, a dearer form). False when it cannot pay.
     */
    public static boolean paySpirituality(LivingEntity caster, float cost) {
        if (caster instanceof Player player) return paySpirituality(player, cost);
        return MobSpirituality.pay(caster, cost);                              // what its cast covers first
    }

    /** Spirituality given back (never past the top): a player's, a Beyonder mob's own bar. */
    public static void giveSpirituality(LivingEntity caster, float amount) {
        if (amount <= 0f) return;
        if (caster instanceof Player player) {
            SoulCore.setSpiritualityQuietly(player, SoulCore.getSpirituality(player) + amount);      // it keeps within 0..max
        } else {
            MobSpirituality.add(caster, amount);
        }
    }

    /** The most spirituality a caster holds: a player's by their Sequence, a Beyonder mob's own bar. */
    public static float maxSpiritualityOf(LivingEntity caster) {
        return caster instanceof Player player ? Beyonder.maxSpirituality(player) : MobSpirituality.max(caster);
    }

    /** What comes back to a caster each tick by itself. */
    public static float spiritualityRegenOf(LivingEntity caster) {
        return caster instanceof Player player ? Beyonder.spiritualityRegenPerTick(player) : MobSpirituality.regenPerTick(caster);
    }

    /** What a caster has left: a player's spirituality, a Beyonder mob's own bar. */
    public static float spiritualityOf(LivingEntity caster) {
        return caster instanceof Player player ? SoulCore.getSpirituality(player) : MobSpirituality.get(caster);
    }

    /**
     * A message in chat for the caster, when it is a player (a Beyonder mob casting the same ability reads nothing).
     * For code written for any caster: a LivingEntity has no sendSystemMessage of its own.
     */
    public static void tell(LivingEntity caster, Component message) {
        if (caster instanceof Player player) player.sendSystemMessage(message);
    }

    /** False once the caster died, logged out or went to another dimension: spells that stay around them stop then. */
    public static boolean casterStillHere(LivingEntity player, ServerLevel sl) {
        return player.isAlive() && !player.isRemoved() && player.level() == sl;
    }
}
