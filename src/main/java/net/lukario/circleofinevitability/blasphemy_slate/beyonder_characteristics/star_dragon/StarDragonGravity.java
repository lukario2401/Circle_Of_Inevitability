package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragonFx.Status;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragonMarks.Mark;
import net.lukario.circleofinevitability.combat.Countdown;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.GravityField;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Freeze;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.AbilityLock;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.ConjuredArmor;
import net.lukario.circleofinevitability.status.Exposed;
import net.lukario.circleofinevitability.status.Flight;
import net.lukario.circleofinevitability.status.Gravity;
import net.lukario.circleofinevitability.status.Unnoticed;
import net.lukario.circleofinevitability.status.Weight;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Star Dragon, the gravity half of the wheels (docs/STAR_DRAGON.md): Gravity Abnormality and Electromagnetic Attraction
 * (Star Sacrifice, key 2), Heavybringing and the Heavy Sphere (Heavybringer, key 5), Gravitational Time Dilation, the
 * Neutron Star and the Singularity (the Cosmos wheel, key 7). All of them are combat/GravityField and status/Weight
 * underneath. Every number is right below; the looks are StarDragonFx's.
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

    // Gravitational Time Dilation (Star Dragon, the Cosmos wheel)
    static final int DILATION_SEQUENCE = 1;
    static final int DILATION_COST = 40000;
    static final int DILATION_COOLDOWN = 900;
    private static final double DILATION_RANGE = 30.0;
    private static final double DILATION_RADIUS = 8.0;          // a 16 block field...
    private static final int DILATION_TICKS = 160;              // ...for 8 seconds
    private static final int DILATION_SLOW = 3;                 // Slowness IV: about a quarter of their speed...
    private static final int DILATION_FATIGUE = 2;              // ...Mining Fatigue III: their blows come slowly...
    private static final double DILATION_FALL = 0.25;           // ...and they fall at a quarter (status/Gravity)
    private static final int DILATION_BOSS_SLOW = 2;            // bosses: Slowness III, about half
    private static final double DILATION_SHOTS = 0.25;          // projectiles hang: a quarter of their speed, no gravity

    // Neutron Star (Condenser, the Cosmos wheel)
    static final int NEUTRON_SEQUENCE = 0;
    static final int NEUTRON_COST = 60000;
    static final int NEUTRON_COOLDOWN = 1200;
    private static final int NEUTRON_TICKS = 240;               // 12 seconds
    private static final float NEUTRON_TOUGH = 0.5f;            // half damage, and no knockback (a ConjuredArmor)
    private static final double NEUTRON_RADIUS = 6.0;
    private static final int NEUTRON_WEIGHT = 3;                // enemies in it are pressed down (status/Weight)...
    private static final float NEUTRON_CRUSH = 10f;             // ...and crushed every half second...
    private static final int NEUTRON_MISFORTUNE = 30;           // ...and under Misfortune while they stay

    // Singularity (Condenser, the Cosmos wheel)
    static final int SINGULARITY_SEQUENCE = 0;
    static final int SINGULARITY_COST = 40000;                  // and then all that is left (at least this much)
    static final int SINGULARITY_COOLDOWN = 3600;
    private static final double SINGULARITY_RANGE = 30.0;
    private static final double SINGULARITY_RADIUS = 24.0;
    private static final double SINGULARITY_HANGS = 2.5;        // the black hole hangs this high over the ground
    private static final int SINGULARITY_TICKS = 160;
    private static final double SINGULARITY_PULL = 0.5;         // blocks a tick at most, at the edge of the core
    private static final double SINGULARITY_CORE = 4.0;         // within this: crushed...
    private static final float SINGULARITY_CRUSH = 0.15f;       // ...this share of the maximum health a second
    private static final double SINGULARITY_BEND = 0.15;        // projectiles bend into it this much a tick
    private static final float SINGULARITY_LEFT = 2f;           // the price: one heart left...
    private static final int SINGULARITY_SPENT = 400;           // ...and no abilities and weakness for 20 seconds

    // =========================
    // Names
    // =========================
    private static final String DILATION = "star_dragon_time_dilation";          // its GravityField
    private static final String DILATED = "star_dragon_dilated";                 // its status/Gravity reason
    private static final String NEUTRON = "star_dragon_neutron_star";            // its GravityField and its ConjuredArmor
    private static final String NEUTRON_UNTIL = "star_dragon_neutron_until";
    private static final String SINGULARITY = "star_dragon_singularity";         // its GravityField
    private static final String GATHERING_UNTIL = "star_dragon_gathering_until"; // the Singularity's gathering
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
        bendShots(player, sl, player.position().add(0, player.getBbHeight() * 0.6, 0), radius, BEND, false);
    }

    /** Projectiles not of your side within 'radius' of 'heart' are bent toward it, 'strength' a tick. */
    private static void bendShots(Player player, ServerLevel sl, Vec3 heart, double radius, double strength, boolean swallowed) {
        for (Projectile shot : Spells.hostileShotsAround(player, sl, heart, radius)) {
            Vec3 toward = heart.subtract(shot.position());
            if (toward.lengthSqr() < 1.0) {
                if (swallowed) {
                    StarDragonFx.swallowed(sl, shot.position(), heart);
                    shot.discard();
                }
                continue;
            }
            Vec3 bent = shot.getDeltaMovement().add(toward.normalize().scale(strength));
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

    // =========================
    // Gravitational Time Dilation (the Cosmos wheel, Star Dragon): heaviness causes slowness, and gravity drags on time.
    // A field DILATION_RADIUS wide where you aim (DILATION_RANGE) spreads by StarDragonFx.DILATION_RISEN and holds for
    // the rest of DILATION_TICKS: enemies in it move at about a quarter of their speed (DILATION_SLOW), strike slowly
    // (DILATION_FATIGUE), fall at DILATION_FALL; players are held 70% as long (Spells.holdLength); bosses are only
    // slowed to about half. Projectiles of the other side hang in it (DILATION_SHOTS, no gravity) until it ends. You and
    // your allies move normally.
    // =========================
    static AbilityWheel.Result timeDilation(ServerPlayer player, ServerLevel sl) {
        Vec3 center = Spells.aimGround(player, sl, DILATION_RANGE);
        String tag = "sd_dilation_" + player.getId();
        GravityField.close(player, DILATION);
        StarDragonFx.timeDilation(sl, center, DILATION_RADIUS, DILATION_TICKS, tag);
        int ticks = DILATION_TICKS - StarDragonFx.DILATION_RISEN;
        Set<Projectile> hung = new HashSet<>();
        Later.run(sl, StarDragonFx.DILATION_RISEN, () -> {
            if (!Spells.casterStillHere(player, sl)) {
                StarDragonFx.timeDilationEnd(sl, center, DILATION_RADIUS, tag);
                return;
            }
            GravityField.at(center, DILATION_RADIUS)
                    .height(6)
                    .lasts(ticks)
                    .every(5)
                    .onEnter(one -> {
                        int held = Spells.isBoss(one) ? ticks : Spells.holdLength(one, ticks);
                        StarDragonFx.dilated(sl, one, held, "sd_dilated_" + one.getId());
                        if (one instanceof Player shown) StarDragonFx.status(shown, Status.DILATED, held);
                    })
                    .onInside((one, inside) -> {
                        if (Spells.isBoss(one)) {
                            Spells.keepEffect(one, MobEffects.SLOWNESS, DILATION_BOSS_SLOW, 20);
                            return;
                        }
                        if (inside >= Spells.holdLength(one, ticks)) return;           // a player is held 70% as long
                        Spells.keepEffect(one, MobEffects.SLOWNESS, DILATION_SLOW, 20);
                        Spells.keepEffect(one, MobEffects.MINING_FATIGUE, DILATION_FATIGUE, 20);
                        Gravity.set(one, DILATED, DILATION_FALL, 10);
                    })
                    .onLeave(one -> {
                        Gravity.clear(one, DILATED);
                        StarDragonFx.dilated(sl, one, 0, "sd_dilated_" + one.getId());
                        if (one instanceof Player shown) StarDragonFx.statusEnd(shown, Status.DILATED);
                    })
                    .onPulse(ticksLeft -> hang(player, sl, center, hung))
                    .onEnd(() -> {
                        for (Projectile shot : hung) {
                            if (!shot.isAlive()) continue;
                            shot.setNoGravity(false);                                  // time runs on for them again
                            shot.hurtMarked = true;
                        }
                        StarDragonFx.timeDilationEnd(sl, center, DILATION_RADIUS, tag);
                    })
                    .raise(player, sl, DILATION);
        });
        return AbilityWheel.Result.DONE;
    }

    /** Every beat of Time Dilation: projectiles of the other side that came into it hang there. */
    private static void hang(Player player, ServerLevel sl, Vec3 center, Set<Projectile> hung) {
        for (Projectile shot : Spells.hostileShotsAround(player, sl, center.add(0, 2, 0), DILATION_RADIUS + 1)) {
            if (!hung.add(shot)) continue;
            shot.setDeltaMovement(shot.getDeltaMovement().scale(DILATION_SHOTS));
            shot.setNoGravity(true);
            shot.hurtMarked = true;
            StarDragonFx.timeHang(sl, shot.position());
        }
    }

    // =========================
    // Neutron Star (the Cosmos wheel, Condenser): your body takes the density of a neutron star for NEUTRON_TICKS.
    // Nothing moves, roots or freezes you (Spells.addMoveImmunity, Root.addImmunity, Freeze.addImmunity, in StarDragon:
    // isNeutronStar), and you take NEUTRON_TOUGH less, with no knockback (a ConjuredArmor). From
    // StarDragonFx.NEUTRON_READY your weight
    // crushes: enemies within NEUTRON_RADIUS are pressed down (Weight NEUTRON_WEIGHT; bosses only slowed), take
    // NEUTRON_CRUSH every half second, and are under Misfortune (StarDragonMarks) while they stay.
    // =========================
    static AbilityWheel.Result neutronStar(ServerPlayer player, ServerLevel sl) {
        String tag = "sd_neutron_" + player.getId();
        player.getPersistentData().putLong(NEUTRON_UNTIL, sl.getGameTime() + NEUTRON_TICKS);
        ConjuredArmor.of(NEUTRON).lasts(NEUTRON_TICKS).cut(NEUTRON_TOUGH).steady().wear(player);
        Freeze.thaw(player);
        Root.remove(player, RootRestriction.MOVE, RootRestriction.JUMP, RootRestriction.TELEPORT);
        Weight.end(player);
        GravityField.close(player, NEUTRON);
        StarDragonFx.neutronStar(sl, player, NEUTRON_RADIUS, NEUTRON_TICKS, tag);
        StarDragonFx.status(player, Status.NEUTRON_STAR, NEUTRON_TICKS);
        long cast = sl.getGameTime();
        Later.run(sl, StarDragonFx.NEUTRON_READY, () -> {
            if (!Spells.casterStillHere(player, sl) || !isNeutronStar(player)) return;
            GravityField.around(player, NEUTRON_RADIUS)
                    .height(4)
                    .lasts(NEUTRON_TICKS - StarDragonFx.NEUTRON_READY)
                    .every(10)
                    .presses(NEUTRON_WEIGHT)
                    .bossesOnlySlowed(Spells::isBoss)
                    .onInside((one, inside) -> {
                        StarDragon.hit(player, one, NEUTRON_CRUSH);
                        StarDragonFx.neutronCrush(sl, one);
                        if (one.isAlive()) StarDragonMarks.put(one, Mark.MISFORTUNE, player, NEUTRON_MISFORTUNE);
                    })
                    .onPulse(ticksLeft -> StarDragonFx.neutronTick(sl, player, NEUTRON_RADIUS))
                    .raise(player, sl, NEUTRON);
        });
        Later.run(sl, NEUTRON_TICKS, () -> {
            if (player.getPersistentData().getLongOr(NEUTRON_UNTIL, 0L) != cast + NEUTRON_TICKS) return;   // cast again since
            player.getPersistentData().remove(NEUTRON_UNTIL);
            StarDragonFx.neutronStarEnd(sl, player, tag);
            StarDragonFx.statusEnd(player, Status.NEUTRON_STAR);
        });
        return AbilityWheel.Result.DONE;
    }

    /** True while the Condenser's body has a neutron star's density (nothing moves, roots or freezes it). */
    public static boolean isNeutronStar(Player player) {
        return StarDragon.isActive(player, NEUTRON_UNTIL);
    }

    // =========================
    // Singularity (the Cosmos wheel, Condenser): self-sacrifice. You gather for StarDragonFx.SINGULARITY_GATHER (a
    // combat/Countdown; you are held in place, by your own will, even as a Condenser), then a black hole opens where you
    // aim (SINGULARITY_RANGE; on yourself if you aim at nothing) for SINGULARITY_TICKS. Every enemy within
    // SINGULARITY_RADIUS is dragged in, harder the closer it is (bosses half as hard, players 70%); within
    // SINGULARITY_CORE it takes SINGULARITY_CRUSH of its maximum health a second; projectiles bend into it and are
    // swallowed. Allies are not pulled. The price, when it opens: all your spirituality, all but SINGULARITY_LEFT of
    // your health (it never kills you), and for SINGULARITY_SPENT no abilities (status/AbilityLock) and weakness. If you
    // die or leave while gathering, it never opens.
    // =========================
    static AbilityWheel.Result singularity(ServerPlayer player, ServerLevel sl) {
        Vec3 eye = player.getEyePosition();
        Vec3 aim = Spells.aimPoint(player, sl, SINGULARITY_RANGE);
        boolean nothing = aim.distanceToSqr(eye) > (SINGULARITY_RANGE - 0.5) * (SINGULARITY_RANGE - 0.5);
        Vec3 ground = nothing ? player.position()
                : Spells.groundAt(player, sl, aim.subtract(player.getLookAngle().normalize().scale(0.3)));
        Vec3 center = ground.add(0, SINGULARITY_HANGS, 0);
        int gather = StarDragonFx.SINGULARITY_GATHER;
        player.getPersistentData().putLong(GATHERING_UNTIL, sl.getGameTime() + gather + 2);
        Root.apply(player, gather, RootRestriction.MOVE, RootRestriction.JUMP);
        StarDragonFx.singularityGather(sl, player);
        Countdown.seconds(Math.max(1, gather / 20))
                .colors(0xB8B0FF, 0x6A5CFF)
                .keepWhile(() -> Spells.casterStillHere(player, sl))
                .onFinish(() -> {
                    player.getPersistentData().remove(GATHERING_UNTIL);
                    openSingularity(player, sl, ground, center);
                })
                .onCancel(() -> {
                    player.getPersistentData().remove(GATHERING_UNTIL);
                    Root.remove(player, RootRestriction.MOVE, RootRestriction.JUMP);
                })
                .start(player);
        return AbilityWheel.Result.DONE;
    }

    /** True while a Condenser gathers for the Singularity (their own hold takes, even on a Condenser). */
    public static boolean isGathering(Player player) {
        return StarDragon.isActive(player, GATHERING_UNTIL);
    }

    private static void openSingularity(ServerPlayer player, ServerLevel sl, Vec3 ground, Vec3 center) {
        String tag = "sd_singularity_" + player.getId();
        SoulCore.setSpiritualityQuietly(player, 0f);                              // the price
        if (player.getHealth() > SINGULARITY_LEFT) player.setHealth(SINGULARITY_LEFT);
        AbilityLock.apply(player, SINGULARITY_SPENT);
        Spells.keepEffect(player, MobEffects.WEAKNESS, 0, SINGULARITY_SPENT);
        StarDragonFx.spent(sl, player);
        StarDragonFx.formWeakness(sl, player, SINGULARITY_SPENT, "sd_spent_" + player.getId());
        StarDragonFx.status(player, Status.SPENT, SINGULARITY_SPENT);
        GravityField.close(player, SINGULARITY);
        StarDragonFx.singularity(sl, center, SINGULARITY_RADIUS, SINGULARITY_TICKS, tag);
        Set<UUID> crushedAt = new HashSet<>();
        GravityField.at(ground, SINGULARITY_RADIUS)
                .height(SINGULARITY_RADIUS * 0.6)
                .lasts(SINGULARITY_TICKS)
                .every(5)
                .onInside((one, inside) -> {
                    double distance = one.getBoundingBox().getCenter().distanceTo(center);
                    double share = Spells.holdLength(one, 100) / 100.0;                // bosses half, players 70%
                    double speed = SINGULARITY_PULL * Math.max(0.2, 1.0 - distance / SINGULARITY_RADIUS) * share;
                    Spells.pull(one, center, speed, one.getY() + 0.5 < center.y ? 0.08 : 0.0);
                    if (distance > SINGULARITY_CORE || inside % 20 != 0) return;
                    StarDragon.hit(player, one, one.getMaxHealth() * SINGULARITY_CRUSH);
                    if (crushedAt.add(one.getUUID())) StarDragonFx.swallowed(sl, one.position(), center);
                })
                .onPulse(ticksLeft -> {
                    StarDragonFx.singularityTick(sl, center, SINGULARITY_RADIUS);
                    bendShots(player, sl, center, SINGULARITY_RADIUS, SINGULARITY_BEND, true);
                })
                .onEnd(() -> StarDragonFx.singularityEnd(sl, center, tag))
                .raise(player, sl, SINGULARITY);
    }

    /** After a relog or a death. */
    static void clearState(Player player) {
        GravityField.close(player, DILATION);
        GravityField.close(player, NEUTRON);
        GravityField.close(player, SINGULARITY);
        player.getPersistentData().remove(NEUTRON_UNTIL);
        player.getPersistentData().remove(GATHERING_UNTIL);
        ConjuredArmor.takeOff(player, NEUTRON);
        GravityField.close(player, ABNORMALITY);
        GravityField.close(player, HEAVYBRINGING);
        Dome.close(player, SPHERE);
        Zone.close(player, SPHERE_SEAL);
    }
}
