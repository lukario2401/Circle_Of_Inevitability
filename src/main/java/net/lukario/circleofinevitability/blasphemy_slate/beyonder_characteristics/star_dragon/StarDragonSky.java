package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragonMarks.Mark;
import net.lukario.circleofinevitability.combat.Cooldowns;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Orbit;
import net.lukario.circleofinevitability.combat.Shot;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Star Dragon, the sky (docs/STAR_DRAGON.md): Star Grazing and Fission Pulse (Star Shepherd, key 6), Fusion Cluster
 * and Planetfall (the Cosmos wheel, key 7). Every sky ability shows where it lands before it does (the warning circles
 * are in StarDragonFx). The grazed star, the radiation, the storm and the cracks of light are combat/Zone on the spot
 * they were shown; the circling fusion clusters are a combat/Orbit. Every number is right below.
 */
public final class StarDragonSky {

    private StarDragonSky() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Star Grazing (Star Shepherd, key 6)
    static final int GRAZE_SEQUENCE = 3;
    private static final int GRAZE_COST = 12000;
    private static final int GRAZE_COOLDOWN = 300;
    private static final double GRAZE_RANGE = 28.0;
    private static final double GRAZE_RADIUS = 4.0;             // an 8 block circle...
    private static final double GRAZE_WIDE = 6.0;               // ...12 from the Radiant Angel
    private static final float GRAZE_DAMAGE = 80f;
    private static final float GRAZE_RADIANT = 1.6f;            // Radiant Angel: 60% more
    private static final double GRAZE_SHOVE = 0.9;
    private static final int GRAZE_DRAGON_COUNT = 3;            // Star Dragon: three asteroids...
    private static final int GRAZE_DRAGON_STEP = 10;            // ...this far apart (and each 40% further out)
    private static final double STAR_HANGS = 6.0;               // the grazed star hangs this high...
    private static final double STAR_RADIUS = 10.0;             // ...feeds allies this close...
    private static final int STAR_TICKS = 600;                  // ...for 30 seconds (60 from the Radiant Angel)...
    private static final int STAR_LONGER = 1200;
    private static final int STAR_EVERY = 40;                   // ...every 2 seconds...
    private static final float STAR_FEEDS = 0.03f;              // ...this share of a player's spirituality bar

    // Fission Pulse (Star Shepherd, key 6 sneak)
    private static final int FISSION_COST = 9000;
    private static final int FISSION_COOLDOWN = 240;
    private static final double FISSION_RANGE = 24.0;
    private static final double FISSION_RADIUS = 3.0;           // a burst 6 across
    private static final float FISSION_DAMAGE = 80f;
    private static final int IRRADIATED_TICKS = 160;
    private static final int RADIATION_TICKS = 160;             // the zone it leaves
    private static final int STORM_SEQUENCE = 1;               // Star Dragon: an electromagnetic storm instead...
    private static final int STORM_TICKS = 200;
    private static final double STORM_RADIUS = 5.0;
    private static final float STORM_ARC = 10f;                 // ...an arc every half second...
    private static final float STORM_METAL = 2f;                // ...twice on metal armor

    // Fusion Cluster (Radiant Angel, the Cosmos wheel)
    static final int FUSION_SEQUENCE = 2;
    static final int FUSION_COST = 25000;
    static final int FUSION_COOLDOWN = 280;
    private static final int FUSION_COUNT = 3;
    private static final int FUSION_MORE = 5;                   // Condenser: five, no windup, half the cost
    private static final int FUSION_MORE_SEQUENCE = 0;
    static final float FUSION_CONDENSER_COST = 0.5f;
    private static final double FUSION_RANGE = 30.0;
    private static final double FUSION_FAN = 9.0;               // degrees between clusters
    private static final double FUSION_SPEED = 1.4;
    private static final double FUSION_BURST = 5.0;
    private static final float FUSION_DAMAGE = 80f;
    private static final int CONFINEMENT_SEQUENCE = 1;          // Star Dragon: Inertial Confinement...
    private static final double CONFINED_BURST = 8.0;
    private static final float CONFINED_DAMAGE = 1.5f;          // ...50% more, and no circling first
    static final float RADIANT_BOOST = 1.25f;                   // Fusion Cluster and Star Grazing in the Radiant Angel Form

    // Planetfall (Star Dragon, the Cosmos wheel)
    static final int PLANET_SEQUENCE = 1;
    static final int PLANET_COST = 40000;
    static final int PLANET_COOLDOWN = 2400;
    private static final double PLANET_RANGE = 40.0;
    private static final double PLANET_RADIUS = 12.0;           // a 24 block circle
    private static final float PLANET_PLAYER = 0.6f;            // players take this share of their maximum health...
    private static final float PLANET_BOSS = 0.25f;             // ...bosses this share...
    private static final float PLANET_HUGE = 300f;              // ...everything else this (it kills most mobs)
    private static final float PLANET_ALLY = 0.25f;             // allies in the circle take a quarter
    private static final int CRACKS_TICKS = 200;
    private static final float CRACKS_DAMAGE = 10f;             // a second, on the cracks of light

    // =========================
    // Names
    // =========================
    private static final String CD_GRAZE = "star_dragon_star_grazing";
    private static final String CD_FISSION = "star_dragon_fission_pulse";
    private static final String GRAZED_STAR = "star_dragon_grazed_star";          // its Zone
    private static final String RADIATION = "star_dragon_radiation";              // its Zone
    private static final String STORM = "star_dragon_em_storm";                   // its Zone
    private static final String FUSION = "star_dragon_fusion";                    // its Orbit
    private static final String CRACKS = "star_dragon_light_cracks";              // its Zone

    // =========================
    // Star Grazing (key 6): you graze a small asteroid out of its orbit. A warning circle GRAZE_RADIUS wide opens where
    // you aim (GRAZE_RANGE); StarDragonFx.GRAZE_LANDS later the rock in fire strikes it: GRAZE_DAMAGE and a shove to
    // every enemy in it (nothing on the ground changes). The grazed star then hangs over the spot for STAR_TICKS:
    // every STAR_EVERY the players on your side under it (you too) get STAR_FEEDS of their spirituality bar back.
    // Radiant Angel: GRAZE_WIDE, GRAZE_RADIANT more, STAR_LONGER. Star Dragon: GRAZE_DRAGON_COUNT asteroids, one after
    // another over the same zone. In the Radiant Angel Form it hits RADIANT_BOOST harder.
    // =========================
    static void starGrazing(Player player, ServerLevel sl) {
        if (!Cooldowns.ready(player, CD_GRAZE, "Star Grazing")) return;
        if (!StarDragon.pay(player, GRAZE_COST, CD_GRAZE)) return;
        Cooldowns.start(player, CD_GRAZE, GRAZE_COOLDOWN);
        boolean radiant = StarDragon.grown(player, FUSION_SEQUENCE);
        boolean dragon = StarDragon.grown(player, STORM_SEQUENCE);
        double radius = radiant ? GRAZE_WIDE : GRAZE_RADIUS;
        float damage = GRAZE_DAMAGE * (radiant ? GRAZE_RADIANT : 1f) * (StarDragonForms.isRadiantAngel(player) ? RADIANT_BOOST : 1f);
        Vec3 ground = Spells.aimGround(player, sl, GRAZE_RANGE);
        int count = dragon ? GRAZE_DRAGON_COUNT : 1;
        for (int i = 0; i < count; i++) {
            Later.run(sl, i * GRAZE_DRAGON_STEP, () -> {
                if (!Spells.casterStillHere(player, sl)) return;
                StarDragonFx.starGrazing(sl, ground, radius);
                Later.run(sl, StarDragonFx.GRAZE_LANDS, () -> {
                    if (!Spells.casterStillHere(player, sl)) return;
                    for (LivingEntity enemy : Spells.enemiesAround(player, sl, ground, radius)) {
                        StarDragon.hit(player, enemy, damage);
                        if (enemy.isAlive()) Spells.push(enemy, ground, GRAZE_SHOVE, 0.4);
                    }
                });
            });
        }
        int hangs = radiant ? STAR_LONGER : STAR_TICKS;
        Later.run(sl, StarDragonFx.GRAZE_LANDS + (count - 1) * GRAZE_DRAGON_STEP, () -> {
            if (Spells.casterStillHere(player, sl)) grazedStar(player, sl, ground, hangs);
        });
    }

    /** The grazed star over 'ground' for 'ticks', feeding the spirituality of the players on your side under it. */
    private static void grazedStar(Player player, ServerLevel sl, Vec3 ground, int ticks) {
        Vec3 above = ground.add(0, STAR_HANGS, 0);
        String tag = "sd_grazed_star_" + player.getId();
        Zone.close(player, GRAZED_STAR);                                         // one grazed star at a time
        StarDragonFx.grazedStar(sl, above, ticks, tag);
        Zone.at(ground, STAR_RADIUS)
                .height(STAR_HANGS + 2)
                .lasts(ticks)
                .every(STAR_EVERY)
                .affects(one -> false)                                           // it acts on nobody by itself...
                .onPulse(ticksLeft -> {                                          // ...it feeds who is under it
                    List<LivingEntity> fed = new ArrayList<>(Spells.alliesAround(player, sl, ground, STAR_RADIUS));
                    if (player.position().distanceToSqr(ground) <= STAR_RADIUS * STAR_RADIUS) fed.add(player);
                    for (LivingEntity one : fed) {
                        if (!(one instanceof Player ally)) continue;
                        float max = Beyonder.maxSpirituality(ally);
                        SoulCore.setSpiritualityQuietly(ally, Math.min(max, SoulCore.getSpirituality(ally) + max * STAR_FEEDS));
                        StarDragonFx.grazedStarFeeds(sl, above, ally);
                    }
                })
                .onEnd(() -> StarDragonFx.grazedStarEnd(sl, above, tag))
                .open(player, sl, GRAZED_STAR);
    }

    // =========================
    // Fission Pulse (key 6, sneak): the nuclei loosen in the enemy you look at (FISSION_RANGE). It glows for
    // StarDragonFx.FISSION_SPLITS, then splits: FISSION_DAMAGE to every enemy within FISSION_RADIUS and Irradiated for
    // IRRADIATED_TICKS (StarDragonMarks: bleeding nothing heals, Weakness). A radiation zone stays RADIATION_TICKS and
    // irradiates whoever stands in it. Star Dragon: an electromagnetic storm instead, STORM_TICKS: an arc on an enemy
    // in it every half second (STORM_METAL times on metal armor), and the projectiles of the other side stop in it.
    // No enemy in sight: nothing spent.
    // =========================
    static void fissionPulse(Player player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, FISSION_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There are no nuclei in sight to loosen."));
            return; // nothing spent
        }
        if (!Cooldowns.ready(player, CD_FISSION, "Fission Pulse")) return;
        if (!StarDragon.pay(player, FISSION_COST, CD_FISSION)) return;
        Cooldowns.start(player, CD_FISSION, FISSION_COOLDOWN);
        StarDragonFx.fissionPulse(sl, player, target);
        Vec3[] last = {target.position()};
        for (int t = 0; t < StarDragonFx.FISSION_SPLITS; t += 5) {
            Later.run(sl, t, () -> {
                if (target.isAlive()) last[0] = target.position();               // where it splits, even if it died
            });
        }
        Later.run(sl, StarDragonFx.FISSION_SPLITS, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            Vec3 at = target.isAlive() ? target.position() : last[0];
            Vec3 ground = Spells.groundAt(player, sl, at.add(0, 0.5, 0));
            StarDragonFx.fissionBurst(sl, at.add(0, 1, 0), FISSION_RADIUS);
            for (LivingEntity enemy : Spells.enemiesAround(player, sl, at.add(0, 1, 0), FISSION_RADIUS)) {
                StarDragon.hit(player, enemy, FISSION_DAMAGE);
                if (enemy.isAlive()) StarDragonMarks.put(enemy, Mark.IRRADIATED, player, IRRADIATED_TICKS);
            }
            if (StarDragon.grown(player, STORM_SEQUENCE)) emStorm(player, sl, ground);
            else radiationZone(player, sl, ground);
        });
    }

    private static void radiationZone(Player player, ServerLevel sl, Vec3 ground) {
        String tag = "sd_radiation_" + player.getId();
        Zone.close(player, RADIATION);
        StarDragonFx.radiationZone(sl, ground, FISSION_RADIUS, RADIATION_TICKS, tag);
        Zone.at(ground, FISSION_RADIUS)
                .height(3)
                .lasts(RADIATION_TICKS)
                .every(20)
                .onInside((enemy, inside) -> StarDragonMarks.put(enemy, Mark.IRRADIATED, player, 40))
                .onEnd(() -> StarDragonFx.radiationZoneEnd(sl, ground, tag))
                .open(player, sl, RADIATION);
    }

    private static void emStorm(Player player, ServerLevel sl, Vec3 ground) {
        String tag = "sd_storm_" + player.getId();
        Zone.close(player, STORM);
        StarDragonFx.emStorm(sl, ground, STORM_RADIUS, STORM_TICKS, tag);
        Zone.at(ground, STORM_RADIUS)
                .height(5)
                .lasts(STORM_TICKS)
                .every(10)
                .onPulse(ticksLeft -> {
                    List<LivingEntity> inside = Spells.enemiesAround(player, sl, ground.add(0, 1, 0), STORM_RADIUS);
                    if (!inside.isEmpty()) {
                        LivingEntity struck = inside.get(sl.getRandom().nextInt(inside.size()));
                        StarDragonFx.emArc(sl, struck.position().add(0, 5, 0), struck.getBoundingBox().getCenter());
                        StarDragon.hit(player, struck, STORM_ARC * (wearsMetal(struck) ? STORM_METAL : 1f));
                    }
                    for (Projectile shot : Spells.hostileShotsAround(player, sl, ground.add(0, 2, 0), STORM_RADIUS + 1)) {
                        StarDragonFx.stormStop(sl, shot.position());
                        shot.discard();
                    }
                })
                .onEnd(() -> StarDragonFx.emStormEnd(sl, ground, tag))
                .open(player, sl, STORM);
    }

    /** Wears a piece of iron, chain, gold or netherite (the storm's arcs shock it twice). */
    private static boolean wearsMetal(LivingEntity one) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack worn = one.getItemBySlot(slot);
            if (worn.isEmpty()) continue;
            String name = BuiltInRegistries.ITEM.getKey(worn.getItem()).getPath();
            if (name.startsWith("iron_") || name.startsWith("chainmail_") || name.startsWith("golden_")
                    || name.startsWith("netherite_") || name.startsWith("copper_")) return true;
        }
        return false;
    }

    // =========================
    // Fusion Cluster (the Cosmos wheel, Radiant Angel): FUSION_COUNT magnetically confined clusters condense round you
    // (StarDragonFx.FUSION_READY, a combat/Orbit), then fly in a fan at the enemy you look at (or where you aim). Each
    // bursts FUSION_BURST wide for FUSION_DAMAGE. Star Dragon: Inertial Confinement, no circling, they implode on hit
    // (CONFINED_BURST, CONFINED_DAMAGE more). Condenser: FUSION_MORE of them at once, for half the cost
    // (StarDragon.costFor, a cost rule). In the Radiant Angel Form, RADIANT_BOOST harder.
    // =========================
    static AbilityWheel.Result fusionCluster(ServerPlayer player, ServerLevel sl) {
        boolean confined = StarDragon.grown(player, CONFINEMENT_SEQUENCE);
        int count = StarDragon.grown(player, FUSION_MORE_SEQUENCE) ? FUSION_MORE : FUSION_COUNT;
        String tag = "sd_fusion_" + player.getId() + "_" + sl.getGameTime();
        Orbit.close(player, FUSION);
        if (confined) {
            StarDragonFx.fusionCluster(sl, player, count, 6, tag);
            for (int i = 0; i < count; i++) launch(player, sl, i, count, tag, 0, true);
            return AbilityWheel.Result.DONE;
        }
        StarDragonFx.fusionCluster(sl, player, count, StarDragonFx.FUSION_READY + 4, tag);
        Orbit.around(player, count)
                .radius(StarDragonFx.FUSION_RADIUS).height(StarDragonFx.FUSION_HEIGHT).speed(StarDragonFx.FUSION_SPEED)
                .lasts(StarDragonFx.FUSION_READY + 2)
                .onTick(age -> {
                    StarDragonFx.fusionOrbit(sl, player, count, tag, age);
                    if (age != StarDragonFx.FUSION_READY) return;
                    for (int i = 0; i < count; i++) {
                        if (Orbit.release(player, FUSION, i, 0) != null) launch(player, sl, i, count, tag, age, false);
                    }
                })
                .open(player, sl, FUSION);
        return AbilityWheel.Result.DONE;
    }

    /** Cluster 'index' leaves its spot for the enemy looked at (or where you aim), in its place in the fan. */
    private static void launch(ServerPlayer player, ServerLevel sl, int index, int count, String tag, int age, boolean confined) {
        Vec3 from = StarDragonFx.fusionLaunch(sl, player, index, count, tag, age);
        LivingEntity target = Spells.aimEnemy(player, sl, FUSION_RANGE);
        Vec3 aim = target != null ? target.getBoundingBox().getCenter().subtract(from).normalize()
                : Spells.aimFrom(player, sl, from, FUSION_RANGE);
        Vec3 way = Spells.turned(aim, (index - (count - 1) / 2.0) * FUSION_FAN);
        double burst = confined ? CONFINED_BURST : FUSION_BURST;
        float damage = FUSION_DAMAGE * (confined ? CONFINED_DAMAGE : 1f)
                * (StarDragonForms.isRadiantAngel(player) ? RADIANT_BOOST : 1f);
        Shot.of(StarDragonFx.FUSION_FLYING)
                .speed(FUSION_SPEED).range(FUSION_RANGE).width(0.6)
                .onEnd((at, hitBlock) -> {
                    StarDragonFx.fusionBurst(sl, at, burst, confined);
                    Later.run(sl, confined ? 6 : 0, () -> {
                        if (!Spells.casterStillHere(player, sl)) return;
                        for (LivingEntity enemy : Spells.enemiesAround(player, sl, at, burst)) StarDragon.hit(player, enemy, damage);
                    });
                })
                .fire(player, sl, from, way);
    }

    // =========================
    // Planetfall (the Cosmos wheel, Star Dragon): a circle PLANET_RADIUS wide opens where you aim (PLANET_RANGE), and a
    // huge asteroid lowers through the sky until StarDragonFx.PLANET_LANDS, its warning growing. Then: players take
    // PLANET_PLAYER of their maximum health, bosses PLANET_BOSS, everything else PLANET_HUGE; allies in the circle a
    // quarter of that; everything is thrown outward. The cracks of light burn CRACKS_DAMAGE a second for CRACKS_TICKS.
    // The ground is never changed.
    // =========================
    static AbilityWheel.Result planetfall(ServerPlayer player, ServerLevel sl) {
        Vec3 ground = Spells.aimGround(player, sl, PLANET_RANGE);
        StarDragonFx.planetfall(sl, ground, PLANET_RADIUS);
        Later.run(sl, StarDragonFx.PLANET_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            for (LivingEntity one : Spells.livingAround(player, sl, ground.add(0, 1, 0), PLANET_RADIUS)) {
                boolean enemy = Spells.isEnemy(player, one);
                float damage = one instanceof Player ? one.getMaxHealth() * PLANET_PLAYER
                        : Spells.isBoss(one) ? one.getMaxHealth() * PLANET_BOSS : PLANET_HUGE;
                if (enemy) {
                    StarDragon.hit(player, one, damage);
                } else {                                                         // the impact, not you: pets do not turn on them
                    one.invulnerableTime = 0;
                    one.hurt(sl.damageSources().magic(), damage * PLANET_ALLY);
                }
                if (one.isAlive()) Spells.push(one, ground, 1.6, 0.6);
            }
            String tag = "sd_cracks_" + player.getId();
            Zone.close(player, CRACKS);
            StarDragonFx.lightCracks(sl, ground, PLANET_RADIUS, CRACKS_TICKS, tag);
            Zone.at(ground, PLANET_RADIUS)
                    .height(3)
                    .lasts(CRACKS_TICKS)
                    .every(20)
                    .onInside((enemy, inside) -> {
                        StarDragon.hit(player, enemy, CRACKS_DAMAGE);
                        StarDragonFx.crackBurn(sl, enemy);
                    })
                    .onEnd(() -> StarDragonFx.lightCracksEnd(sl, ground, tag))
                    .open(player, sl, CRACKS);
        });
        return AbilityWheel.Result.DONE;
    }

    /** After a relog or a death. */
    static void clearState(Player player) {
        Zone.close(player, GRAZED_STAR);
        Zone.close(player, RADIATION);
        Zone.close(player, STORM);
        Zone.close(player, CRACKS);
        Orbit.close(player, FUSION);
    }
}
