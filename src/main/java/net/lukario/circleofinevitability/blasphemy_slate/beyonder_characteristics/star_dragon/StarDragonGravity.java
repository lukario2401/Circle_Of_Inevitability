package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragonFx.Status;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.GravityField;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Exposed;
import net.lukario.circleofinevitability.status.Flight;
import net.lukario.circleofinevitability.status.Unnoticed;
import net.lukario.circleofinevitability.status.Weight;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Star Dragon, the gravity half of the wheels (docs/STAR_DRAGON.md): Gravity Abnormality and Electromagnetic Attraction
 * (Star Sacrifice, key 2), Heavybringing and the Heavy Sphere (Heavybringer, key 5). Time Dilation, the Neutron Star and
 * the Singularity of the second half belong here too, built the same way on combat/GravityField and status/Weight.
 * Every number is right below; the looks are StarDragonFx's.
 */
public final class StarDragonGravity {

    private StarDragonGravity() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Gravity Abnormality (Star Sacrificer)
    static final int ABNORMALITY_SEQUENCE = 7;
    static final int ABNORMALITY_COST = 1250;
    static final int ABNORMALITY_COOLDOWN = 240;
    private static final double ABNORMALITY_RANGE = 20.0;
    private static final double ABNORMALITY_RADIUS = 4.0;       // an 8 block area...
    private static final double ABNORMALITY_WIDE = 6.0;         // ...12 from the Tidal Scholar...
    private static final double ABNORMALITY_WIDEST = 8.0;       // ...16 from the Heavybringer
    private static final int ABNORMALITY_TICKS = 100;
    private static final float LIFT_PITCH = -45f;               // aimed more than this far up: Lifted, not Pressed
    private static final int PRESSED_WEIGHT = 2;                // Pressed: Heavy at this level (status/Weight)
    private static final double LIFT_HEIGHT = 3.0;              // Lifted: held this high...
    private static final double LIFT_HIGHER = 6.0;              // ...this high from the Heavybringer, and slammed down
    private static final double SLAM_SPEED = 1.4;
    private static final int SLAM_WEIGHT = 3;                   // a slammed one's fall hurts as if this Heavy (about double)
    private static final float COLLAPSE_BELOW = 0.5f;           // Heavybringer: a Pressed enemy under half health...
    private static final float COLLAPSE_DAMAGE = 80f;           // ...collapses inward, once a cast
    private static final int LIGHTEN_SEQUENCE = 5;              // Tidal Scholar: aimed at your own feet...
    private static final float LIGHTEN_PITCH = 75f;
    private static final int LIGHTEN_TICKS = 120;               // ...you fly this long...
    private static final double AIR_WAVE_RADIUS = 4.0;          // ...and shed a wave of air
    private static final float AIR_WAVE_DAMAGE = 20f;

    // Electromagnetic Attraction (Star Sacrificer)
    static final int MAGNET_SEQUENCE = 7;
    static final int MAGNET_COST = 1250;
    static final int MAGNET_COOLDOWN = 200;
    private static final double MAGNET_RANGE = 24.0;
    private static final double MAGNET_PARTNER = 12.0;          // a second enemy this close to the first...
    private static final int MAGNET_TICKS = 14;                 // ...and they are drawn together over this long
    private static final double MAGNET_SPEED = 0.9;
    private static final float MAGNET_DAMAGE = 40f;
    private static final int MAGNET_STAGGER = 20;

    // Heavybringing (Heavybringer)
    static final int HEAVYBRINGING_SEQUENCE = 4;
    static final int HEAVYBRINGING_COST = 6000;
    static final int HEAVYBRINGING_COOLDOWN = 500;
    private static final double HEAVYBRINGING_RADIUS = 16.0;
    private static final double HEAVYBRINGING_WIDE = 24.0;      // Star Shepherd
    private static final int HEAVYBRINGING_WIDE_SEQUENCE = 3;
    private static final int HEAVYBRINGING_TICKS = 160;
    private static final double HEAVYBRINGING_PULL = 0.12;      // blocks a tick at most
    private static final double INVERSION_RADIUS = 4.0;         // within this: space inverts, Heavy, no teleports
    private static final int INVERSION_WEIGHT = 2;
    private static final double BEND = 0.08;                    // projectiles are bent toward you this much a tick

    // Heavy Sphere (Heavybringer)
    static final int SPHERE_SEQUENCE = 4;
    static final int SPHERE_COST = 6000;
    static final int SPHERE_COOLDOWN = 600;
    private static final double SPHERE_RANGE = 24.0;
    private static final double SPHERE_RADIUS = 3.0;
    private static final int SPHERE_TICKS = 120;

    // =========================
    // Names
    // =========================
    private static final String ABNORMALITY = "star_dragon_abnormality";          // its GravityField
    private static final String HEAVYBRINGING = "star_dragon_heavybringing";      // its GravityField
    private static final String SPHERE = "star_dragon_heavy_sphere";              // its Dome
    private static final String SPHERE_SEAL = "star_dragon_heavy_sphere_seal";    // its Zone (no teleports, no hiding)

    // =========================
    // Gravity Abnormality: where you aim (ABNORMALITY_RANGE) the gravity of an area changes for ABNORMALITY_TICKS. Aimed
    // at the ground, enemies are Pressed (Heavy, status/Weight: slowed, no jumping, their shots drop, falls hurt more).
    // Aimed up (more than 45 degrees), they are Lifted (held LIFT_HEIGHT up, unable to walk). Bosses are only slowed;
    // players are held up 70% as long (Spells.holdLength). Tidal Scholar: a wider area; aimed straight down at your own
    // feet you are lightened instead: you fly LIGHTEN_TICKS and shed a wave of air. Heavybringer: wider still, Lifted
    // ones rise LIFT_HIGHER and are slammed down, and a Pressed one under half health collapses inward (once a cast).
    // =========================
    static AbilityWheel.Result gravityAbnormality(ServerPlayer player, ServerLevel sl) {
        boolean lifted = player.getXRot() < LIFT_PITCH;
        Vec3 center = Spells.aimGround(player, sl, ABNORMALITY_RANGE);
        if (StarDragon.grown(player, LIGHTEN_SEQUENCE) && player.getXRot() > LIGHTEN_PITCH
                && center.distanceToSqr(player.position()) < 6.25) {
            lighten(player, sl);
            return AbilityWheel.Result.DONE;
        }
        boolean heavybringer = StarDragon.grown(player, StarDragon.HEAVYBRINGER_SEQUENCE);
        double radius = heavybringer ? ABNORMALITY_WIDEST
                : StarDragon.grown(player, LIGHTEN_SEQUENCE) ? ABNORMALITY_WIDE : ABNORMALITY_RADIUS;
        String tag = "sd_abnormality_" + player.getId() + "_" + sl.getGameTime();
        GravityField.close(player, ABNORMALITY);
        StarDragonFx.gravityAbnormality(sl, center, radius, ABNORMALITY_TICKS, lifted, tag);
        Set<UUID> collapsed = new HashSet<>();
        Later.run(sl, StarDragonFx.GRAVITY_RISEN, () -> {
            if (!Spells.casterStillHere(player, sl)) {
                StarDragonFx.gravityEnd(sl, center, radius, lifted, tag);
                return;
            }
            GravityField field = GravityField.at(center, radius)
                    .height(lifted ? LIFT_HIGHER + 3 : 4)
                    .lasts(ABNORMALITY_TICKS - StarDragonFx.GRAVITY_RISEN)
                    .every(4)
                    .bossesOnlySlowed(Spells::isBoss)
                    .onEnter(one -> {
                        if (one instanceof Player shown) {
                            StarDragonFx.status(shown, lifted ? Status.LIFTED : Status.PRESSED, GravityField.isRaised(player, ABNORMALITY)
                                    ? Zone.ticksLeft(player, ABNORMALITY) : ABNORMALITY_TICKS);
                        }
                    })
                    .onInside((one, inside) -> {
                        if (inside % 20 != 0) return;
                        if (lifted) {
                            if (inside == 0) StarDragonFx.lifted(sl, one, heavybringer ? LIFT_HIGHER : LIFT_HEIGHT,
                                    Spells.holdLength(one, ABNORMALITY_TICKS), "sd_lifted_" + one.getId());
                            return;
                        }
                        StarDragonFx.pressed(sl, one);
                        if (heavybringer && !Spells.isBoss(one) && one.getHealth() < one.getMaxHealth() * COLLAPSE_BELOW
                                && collapsed.add(one.getUUID())) {
                            StarDragon.hit(player, one, COLLAPSE_DAMAGE);
                            StarDragonFx.collapse(sl, one);
                        }
                    })
                    .onLeave(one -> {
                        if (one instanceof Player shown) {
                            StarDragonFx.statusEnd(shown, Status.LIFTED);
                            StarDragonFx.statusEnd(shown, Status.PRESSED);
                        }
                        if (lifted && heavybringer && one.isAlive() && !Spells.isBoss(one)) {
                            Weight.put(one, SLAM_WEIGHT, 60);
                            Later.run(sl, 6, () -> {
                                if (one.isAlive() && one.onGround()) StarDragonFx.slammed(sl, one, one.position());
                            });
                        }
                    })
                    .onPulse(ticksLeft -> {
                        if (ticksLeft % 20 < 4) StarDragonFx.gravityTick(sl, center, radius, lifted);
                    })
                    .onEnd(() -> StarDragonFx.gravityEnd(sl, center, radius, lifted, tag));
            if (lifted) {
                field = field.lifts(heavybringer ? LIFT_HIGHER : LIFT_HEIGHT)
                        .liftsFor(one -> Spells.holdLength(one, ABNORMALITY_TICKS));
                if (heavybringer) field = field.slamsDown(SLAM_SPEED);
            } else {
                field = field.presses(PRESSED_WEIGHT);
            }
            field.raise(player, sl, ABNORMALITY);
        });
        return AbilityWheel.Result.DONE;
    }

    /** Tidal Scholar: lightened. You fly for a while, and the air you shed hits everything near. */
    private static void lighten(ServerPlayer player, ServerLevel sl) {
        Flight.grant(player, LIGHTEN_TICKS);
        String tag = "sd_lightened_" + player.getId();
        StarDragonFx.lightened(sl, player, LIGHTEN_TICKS, tag);
        StarDragonFx.airWave(sl, player, AIR_WAVE_RADIUS);
        StarDragonFx.status(player, Status.LIGHTENED, LIGHTEN_TICKS);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), AIR_WAVE_RADIUS)) {
            StarDragon.hit(player, enemy, AIR_WAVE_DAMAGE);
            Spells.push(enemy, player.position(), 0.8, 0.3);
        }
        Later.run(sl, LIGHTEN_TICKS, () -> StarDragonFx.lightenedEnd(sl, player, tag));
    }

    // =========================
    // Electromagnetic Attraction: the enemy you look at (MAGNET_RANGE). If another enemy stands within MAGNET_PARTNER
    // of it, the two are drawn into each other over MAGNET_TICKS and collide: MAGNET_DAMAGE to both and a stagger.
    // Alone, you are drawn to it instead and a hammer-like blow lands on it as you meet.
    // =========================
    static AbilityWheel.Result electromagneticAttraction(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, MAGNET_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("Nothing in sight to draw on."));
            return AbilityWheel.Result.REFUSED;
        }
        LivingEntity partner = null;
        double best = MAGNET_PARTNER * MAGNET_PARTNER;
        for (LivingEntity other : Spells.enemiesAround(player, sl, target.position(), MAGNET_PARTNER)) {
            if (other == target) continue;
            double d = other.distanceToSqr(target);
            if (d < best) {
                best = d;
                partner = other;
            }
        }
        if (partner != null) collide(player, sl, target, partner);
        else drawIn(player, sl, target);
        return AbilityWheel.Result.DONE;
    }

    private static void collide(ServerPlayer player, ServerLevel sl, LivingEntity a, LivingEntity b) {
        StarDragonFx.magnetPull(sl, a, b, MAGNET_TICKS);
        boolean[] met = {false};
        for (int t = 0; t <= MAGNET_TICKS; t += 2) {
            boolean last = t >= MAGNET_TICKS;
            Later.run(sl, t, () -> {
                if (met[0] || !Spells.casterStillHere(player, sl) || !a.isAlive() || !b.isAlive()) return;
                Vec3 middle = a.position().add(b.position()).scale(0.5);
                if (a.distanceTo(b) < 1.6 || last) {
                    met[0] = true;
                    for (LivingEntity one : List.of(a, b)) {
                        StarDragon.hit(player, one, MAGNET_DAMAGE);
                        Root.apply(one, Spells.holdLength(one, MAGNET_STAGGER), RootRestriction.MOVE, RootRestriction.ATTACK);
                    }
                    StarDragonFx.magnetCollide(sl, middle.add(0, 1, 0));
                    return;
                }
                Spells.pull(a, b.position(), MAGNET_SPEED, 0.05);
                Spells.pull(b, a.position(), MAGNET_SPEED, 0.05);
            });
        }
    }

    private static void drawIn(ServerPlayer player, ServerLevel sl, LivingEntity target) {
        StarDragonFx.magnetPull(sl, player, target, MAGNET_TICKS);
        boolean[] met = {false};
        for (int t = 0; t <= MAGNET_TICKS; t += 2) {
            boolean last = t >= MAGNET_TICKS;
            Later.run(sl, t, () -> {
                if (met[0] || !Spells.casterStillHere(player, sl) || !target.isAlive()) return;
                if (player.distanceTo(target) < 2.2 || last) {
                    met[0] = true;
                    StarDragon.hit(player, target, MAGNET_DAMAGE);
                    Spells.push(target, player.position(), 0.9, 0.35);
                    Root.apply(target, Spells.holdLength(target, MAGNET_STAGGER), RootRestriction.ATTACK);
                    StarDragonFx.hammerBlow(sl, player, target);
                    return;
                }
                if (!Root.has(player, RootRestriction.MOVE)) Spells.pull(player, target.position(), MAGNET_SPEED, 0.05);
            });
        }
    }

    // =========================
    // Heavybringing: you are a source of gravity for HEAVYBRINGING_TICKS (a GravityField round you). Everything within
    // HEAVYBRINGING_RADIUS (24 from the Star Shepherd) slides toward you, hidden enemies are exposed, enemy projectiles
    // bend toward you; within INVERSION_RADIUS space inverts (Nausea), they are Heavy and cannot teleport. Nothing moves
    // you while it holds (Spells.addMoveImmunity, in StarDragon). Bosses are only slowed. Allies are left alone.
    // =========================
    static AbilityWheel.Result heavybringing(ServerPlayer player, ServerLevel sl) {
        double radius = StarDragon.grown(player, HEAVYBRINGING_WIDE_SEQUENCE) ? HEAVYBRINGING_WIDE : HEAVYBRINGING_RADIUS;
        String tag = "sd_heavybringing_" + player.getId();
        GravityField.close(player, HEAVYBRINGING);
        StarDragonFx.heavybringing(sl, player, radius, HEAVYBRINGING_TICKS, tag);
        StarDragonFx.status(player, Status.HEAVYBRINGING, HEAVYBRINGING_TICKS);
        GravityField.around(player, radius)
                .height(radius)
                .lasts(HEAVYBRINGING_TICKS)
                .every(5)
                .pulls(HEAVYBRINGING_PULL)
                .bossesOnlySlowed(Spells::isBoss)
                .onInside((one, inside) -> {
                    if (Concealment.isHidden(one) || Unnoticed.isUnnoticed(one) || one.hasEffect(MobEffects.INVISIBILITY)) {
                        Exposed.apply(one, 20);
                    }
                    if (one.distanceTo(player) > INVERSION_RADIUS) return;
                    Spells.keepEffect(one, MobEffects.NAUSEA, 0, 80);
                    if (!Spells.isBoss(one)) Weight.put(one, INVERSION_WEIGHT, 10);
                    Root.apply(one, 10, RootRestriction.TELEPORT);
                    if (one instanceof Player shown && inside % 20 == 0) StarDragonFx.status(shown, Status.HEAVY, 20);
                })
                .onPulse(ticksLeft -> {
                    if (ticksLeft % 10 < 5) StarDragonFx.heavybringingTick(sl, player, radius);
                    bendShots(player, sl, radius);
                })
                .onEnd(() -> {
                    StarDragonFx.heavybringingEnd(sl, player, tag);
                    StarDragonFx.statusEnd(player, Status.HEAVYBRINGING);
                })
                .raise(player, sl, HEAVYBRINGING);
        return AbilityWheel.Result.DONE;
    }

    /** True while a Heavybringer's own field holds (nothing moves them). */
    static boolean isHeavybringing(Player player) {
        return GravityField.isRaised(player, HEAVYBRINGING);
    }

    /** Every beat: projectiles of those not on your side near you are bent a little toward you. */
    private static void bendShots(ServerPlayer player, ServerLevel sl, double radius) {
        Vec3 heart = player.position().add(0, player.getBbHeight() * 0.6, 0);
        AABB box = player.getBoundingBox().inflate(radius);
        for (Projectile shot : sl.getEntitiesOfClass(Projectile.class, box)) {
            if (shot.getOwner() instanceof LivingEntity owner && Spells.isFriend(player, owner)) continue;
            Vec3 toward = heart.subtract(shot.position());
            if (toward.lengthSqr() < 1.0 || toward.lengthSqr() > radius * radius) continue;
            Vec3 bent = shot.getDeltaMovement().add(toward.normalize().scale(BEND));
            shot.setDeltaMovement(bent);
            shot.hurtMarked = true;
            if (sl.getRandom().nextInt(6) == 0) StarDragonFx.bent(sl, shot.position(), shot.position().add(bent.scale(3)));
        }
    }

    // =========================
    // Heavy Sphere: the air round the enemy you look at (SPHERE_RANGE) turns dense: a sphere SPHERE_RADIUS wide for
    // SPHERE_TICKS (70% on a player, half on a boss) that nobody but your side crosses either way (a Dome), whose shell
    // stops projectiles, and inside which nothing teleports and nothing stays hidden.
    // =========================
    static AbilityWheel.Result heavySphere(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, SPHERE_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one to close the air round."));
            return AbilityWheel.Result.REFUSED;
        }
        Vec3 center = target.getBoundingBox().getCenter();
        int ticks = Spells.holdLength(target, SPHERE_TICKS);
        String tag = "sd_sphere_" + player.getId();
        Dome.close(player, SPHERE);
        StarDragonFx.heavySphere(sl, center, SPHERE_RADIUS, ticks, tag);
        Dome.sphere(center, SPHERE_RADIUS)
                .lasts(ticks)
                .keepsEnemiesIn()
                .stopsProjectiles()
                .outsiders(one -> !Spells.isFriend(player, one))
                .onBlock(at -> StarDragonFx.sphereBlock(sl, at))
                .onEnd(() -> StarDragonFx.heavySphereEnd(sl, center, tag))
                .raise(player, sl, SPHERE);
        Zone.at(center.subtract(0, SPHERE_RADIUS, 0), SPHERE_RADIUS)
                .height(SPHERE_RADIUS * 2)
                .lasts(ticks)
                .every(5)
                .affects(one -> true)
                .onEnter(one -> {
                    if (one instanceof Player shown) StarDragonFx.status(shown, Status.SEALED, Zone.ticksLeft(player, SPHERE_SEAL));
                })
                .onInside((one, inside) -> {
                    Root.apply(one, 8, RootRestriction.TELEPORT);
                    if (!Spells.isFriend(player, one)) Exposed.apply(one, 8);
                })
                .onLeave(one -> {
                    if (one instanceof Player shown) StarDragonFx.statusEnd(shown, Status.SEALED);
                })
                .open(player, sl, SPHERE_SEAL);
        return AbilityWheel.Result.DONE;
    }

    /** After a relog or a death. */
    static void clearState(Player player) {
        GravityField.close(player, ABNORMALITY);
        GravityField.close(player, HEAVYBRINGING);
        Dome.close(player, SPHERE);
        Zone.close(player, SPHERE_SEAL);
    }
}
