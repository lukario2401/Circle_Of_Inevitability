package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.Illuminator.Group;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorFx.Curse;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorFx.Status;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorFx.Weakness;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Bleeding;
import net.lukario.circleofinevitability.status.Enfeebled;
import net.lukario.circleofinevitability.status.Gravity;
import net.lukario.circleofinevitability.status.NoHeal;
import net.lukario.circleofinevitability.status.Unreadable;
import net.lukario.circleofinevitability.status.Vulnerable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Illuminator, key 5: the Astronomy wheel (Astronomer, Sequence 5). docs/ILLUMINATOR.md, "Key 5 wheel".
 *
 *   Star of Curses        a ray of condensed starlight into the enemy you look at; struck again within 12 seconds it
 *                         climbs a ladder of curses: Weakness, then Trauma, then Mutation, then from the start
 *   Blessing of Stars     starlight over the ground you aim at: enemies pressed down, allies weightless
 *   False Constellations  a fake starry sky: your side cannot be picked out from more than 6 blocks; a Meteor Shower
 *                         cast under it is stronger
 *   Meteor Shower         eight meteors of light fall round where you aim
 *
 * Sneak casts the last pick again without the wheel (no cooldown, double the cost). Every entry's cost goes through
 * Illuminator.costFor (Quick Study). The looks are IlluminatorFx's; every number is right below.
 */
public final class IlluminatorAstronomy {

    private IlluminatorAstronomy() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int ASTRONOMY_SEQUENCE = 5;                    // the key
    static final float QUICK_CAST_COST = Illuminator.QUICK_CAST_COST;            // a sneak recast: no cooldown, this many times the cost

    // Star of Curses
    static final int CURSE_SEQUENCE = 5;
    static final int CURSE_COST = 2500;                 // Sequence 5 holds 15500
    static final int CURSE_COOLDOWN = 60;
    static final double CURSE_RANGE = 32.0;
    static final float CURSE_DAMAGE = 40f;              // a starlight hit
    static final int LADDER_WINDOW = 240;               // struck again within this, the next curse on the ladder
    static final int WEAK_TICKS = 160;                  // Weakness II
    static final int TRAUMA_TICKS = 160;                // Trauma: bleeding (status/Bleeding) that nothing heals...
    static final float TRAUMA_PER_SECOND = 4f;          // ...this much a second
    static final int MUTATION_TICKS = 200;              // Mutation: it deals this much less (status/Enfeebled)...
    static final float MUTATION_WEAKER = 0.3f;
    static final float MUTATION_VULNERABLE = 0.2f;      // ...takes this much more, and glows

    // Blessing of Stars
    static final int BLESSING_SEQUENCE = 5;
    static final int BLESSING_COST = 3000;
    static final int BLESSING_COOLDOWN = 400;
    static final double BLESSING_RANGE = 28.0;
    static final double BLESSING_RADIUS = 8.0;
    static final int BLESSING_TICKS = 160;
    static final float PRESS_DAMAGE = 3f;               // a gravity hit, every second, to each enemy inside...
    static final double PRESS_GRAVITY = 2.5;            // ...who is this much heavier (status/Gravity), slowed, unable to jump

    // False Constellations
    static final int SKY_SEQUENCE = 5;
    static final int SKY_COST = 3500;
    static final int SKY_COOLDOWN = 800;
    static final double SKY_RANGE = 24.0;
    static final double SKY_RADIUS = 14.0;
    static final int SKY_TICKS = 400;
    static final double SKY_UNREADABLE = 6.0;           // your side inside cannot be picked out from further (status/Unreadable)

    // Meteor Shower
    static final int METEOR_SEQUENCE = 5;
    static final int METEOR_COST = 5000;
    static final int METEOR_COOLDOWN = 500;
    static final double METEOR_RANGE = 32.0;
    static final double METEOR_AREA = 8.0;              // they fall within this of where you aim...
    static final int METEOR_COUNT = 8;
    static final int METEOR_EVERY = 10;                 // ...one this often (eight over 4 seconds)...
    static final double METEOR_RADIUS = 2.5;            // ...each striking this wide...
    static final double METEOR_STRONG_RADIUS = 3.2;     // (under a false sky)
    static final float METEOR_DAMAGE = 28f;             // ...for this much, a starlight hit...
    static final float METEOR_STRONG = 1.5f;            // ...this much more under a false sky
    static final double METEOR_SHOVE = 0.4;

    // =========================
    // Names and what is kept
    // =========================
    static final String BLESSING = "illuminator_blessing";               // its Zone
    static final String SKY = "illuminator_false_sky";                           // its Zone
    static final String PRESSED = "illuminator_pressed";                 // its Gravity
    static final String MUTATED = "illuminator_mutation";                // its Enfeebled

    /** The ladder of the Star of Curses on each enemy: whose, the step it is at, and when it was struck. Not saved. */
    private record Ladder(UUID caster, int step, long last) {}
    static final Map<UUID, Ladder> LADDERS = new HashMap<>();

    // =========================
    // The key
    // =========================

    static final AbilityWheel WHEEL = AbilityWheel.create("illuminator_astronomy", "Astronomy")
            .radius(72)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .entry("star_of_curses", IlluminatorFx.WHEEL_STAR_OF_CURSES, "Star of Curses", CURSE_SEQUENCE,
                    CURSE_COST, CURSE_COOLDOWN, IlluminatorAstronomy::starOfCurses)
            .costBy((player, cost) -> Illuminator.costFor(player, cost, "star_of_curses", Group.ASTRONOMY))
            .entry("blessing_of_stars", IlluminatorFx.WHEEL_BLESSING_OF_STARS, "Blessing of Stars", BLESSING_SEQUENCE,
                    BLESSING_COST, BLESSING_COOLDOWN, IlluminatorAstronomy::blessingOfStars)
            .costBy((player, cost) -> Illuminator.costFor(player, cost, "blessing_of_stars", Group.ASTRONOMY))
            .entry("false_constellations", IlluminatorFx.WHEEL_FALSE_CONSTELLATIONS, "False Constellations", SKY_SEQUENCE,
                    SKY_COST, SKY_COOLDOWN, IlluminatorAstronomy::falseConstellations)
            .costBy((player, cost) -> Illuminator.costFor(player, cost, "false_constellations", Group.ASTRONOMY))
            .entry("meteor_shower", IlluminatorFx.WHEEL_METEOR_SHOWER, "Meteor Shower", METEOR_SEQUENCE,
                    METEOR_COST, METEOR_COOLDOWN, IlluminatorAstronomy::meteorShower)
            .costBy((player, cost) -> Illuminator.costFor(player, cost, "meteor_shower", Group.ASTRONOMY))
            .onCast((player, sl, entry) -> Illuminator.afterCast(player, entry.key()));

    // =========================
    // Ability 5 - Astronomy
    // Normal: the Astronomy wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void illuminatorAstronomy(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!Illuminator.ready(player, bypassClassCheck, ASTRONOMY_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Star of Curses: a ray from your hand into the enemy you look at (CURSE_RANGE); at IlluminatorFx.CURSE_LANDS,
    // CURSE_DAMAGE (a starlight hit). Struck by you again within LADDER_WINDOW, it climbs one step of the ladder with
    // that hit: Weakness II (WEAK_TICKS), then Trauma (TRAUMA_PER_SECOND bleeding, no healing, TRAUMA_TICKS), then
    // Mutation (MUTATION_WEAKER less dealt, MUTATION_VULNERABLE more taken, glowing, MUTATION_TICKS); after Mutation the
    // next strike is a plain ray again and the ladder starts over.
    // =========================
    static AbilityWheel.Result starOfCurses(LivingEntity player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, CURSE_RANGE);
        if (target == null) {
            Spells.tell(player, Component.literal("The starlight has no one to fall on."));
            return AbilityWheel.Result.REFUSED;
        }
        long now = sl.getGameTime();
        LADDERS.values().removeIf(ladder -> now - ladder.last() > LADDER_WINDOW);
        Ladder ladder = LADDERS.get(target.getUUID());
        int step = ladder != null && ladder.caster().equals(player.getUUID()) ? (ladder.step() + 1) % 4 : 0;
        Curse curse = step == 0 ? null : Curse.values()[step - 1];
        LADDERS.put(target.getUUID(), new Ladder(player.getUUID(), step, now));
        IlluminatorFx.starOfCurses(sl, player, target, curse);
        Later.run(sl, IlluminatorFx.CURSE_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            Illuminator.hit(player, target, CURSE_DAMAGE, Weakness.STARLIGHT);
            if (curse != null && target.isAlive()) curse(player, sl, target, curse);
        });
        return AbilityWheel.Result.DONE;
    }

    /** One step of the ladder on 'target' (its mark over the head replaces the step before). */
    private static void curse(LivingEntity player, ServerLevel sl, LivingEntity target, Curse curse) {
        String tag = "il_curse_" + target.getId();
        switch (curse) {
            case WEAKNESS -> {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WEAK_TICKS, 1));
                IlluminatorFx.cursed(sl, target, curse, WEAK_TICKS, tag);
                if (target instanceof Player cursed) IlluminatorFx.status(cursed, Status.CURSED_WEAK, WEAK_TICKS);
            }
            case TRAUMA -> {
                Bleeding.apply(target, player, TRAUMA_TICKS, TRAUMA_PER_SECOND);
                NoHeal.apply(target, TRAUMA_TICKS);
                IlluminatorFx.cursed(sl, target, curse, TRAUMA_TICKS, tag);
                if (target instanceof Player cursed) IlluminatorFx.status(cursed, Status.TRAUMA, TRAUMA_TICKS);
            }
            case MUTATION -> {
                Enfeebled.apply(target, MUTATED, MUTATION_WEAKER, MUTATION_TICKS);
                Vulnerable.add(target, MUTATION_VULNERABLE, MUTATION_VULNERABLE, MUTATION_TICKS);
                target.addEffect(new MobEffectInstance(MobEffects.GLOWING, MUTATION_TICKS, 0, false, false));
                IlluminatorFx.cursed(sl, target, curse, MUTATION_TICKS, tag);
                if (target instanceof Player cursed) IlluminatorFx.status(cursed, Status.MUTATION, MUTATION_TICKS);
            }
        }
    }

    // =========================
    // Blessing of Stars: starlight gathers over the ground you aim at (BLESSING_RANGE); from IlluminatorFx.BLESSING_RISEN,
    // for the rest of BLESSING_TICKS, inside BLESSING_RADIUS (a Zone, once a second): enemies are pressed down
    // (Slowness III, no jumping, PRESS_GRAVITY times as heavy, PRESS_DAMAGE a gravity hit) and allies, you too, are
    // weightless (Jump Boost II, Slow Falling, Speed I). Cast again: the old one ends.
    // =========================
    static AbilityWheel.Result blessingOfStars(LivingEntity player, ServerLevel sl) {
        Vec3 center = Spells.aimGround(player, sl, BLESSING_RANGE);
        long cast = sl.getGameTime();
        String tag = "il_blessing_" + player.getId() + "_" + cast;
        Zone.close(player, BLESSING);
        IlluminatorFx.blessingOfStars(sl, center, BLESSING_RADIUS, BLESSING_TICKS, tag);
        Later.run(sl, IlluminatorFx.BLESSING_RISEN, () -> {
            if (!Spells.casterStillHere(player, sl)) {
                IlluminatorFx.blessingEnd(sl, center, BLESSING_RADIUS, tag);
                return;
            }
            Zone.at(center, BLESSING_RADIUS)
                    .height(6)
                    .lasts(BLESSING_TICKS - IlluminatorFx.BLESSING_RISEN)
                    .every(20)
                    .affects(one -> true)
                    .onEnter(one -> {
                        if (!(one instanceof Player shown)) return;
                        int left = Zone.ticksLeft(player, BLESSING);
                        IlluminatorFx.status(shown, Spells.isEnemy(player, one) ? Status.PRESSED : Status.WEIGHTLESS, left);
                    })
                    .onInside((one, inside) -> {
                        if (Spells.isEnemy(player, one)) press(player, sl, one);
                        else weightless(player, sl, one);
                    })
                    .onLeave(one -> {
                        Gravity.clear(one, PRESSED);
                        if (one instanceof Player shown) {
                            IlluminatorFx.statusEnd(shown, Status.PRESSED);
                            IlluminatorFx.statusEnd(shown, Status.WEIGHTLESS);
                        }
                    })
                    .onPulse(ticksLeft -> {
                        IlluminatorFx.blessingTick(sl, center, BLESSING_RADIUS);
                        if (Zone.covers(player, BLESSING, player.position())) weightless(player, sl, player);
                    })
                    .onEnd(() -> IlluminatorFx.blessingEnd(sl, center, BLESSING_RADIUS, tag))
                    .open(player, sl, BLESSING);
        });
        return AbilityWheel.Result.DONE;
    }

    /** An enemy pressed down for the next second and a bit. */
    private static void press(LivingEntity player, ServerLevel sl, LivingEntity enemy) {
        Spells.keepEffect(enemy, MobEffects.SLOWNESS, 2, 40);
        Root.apply(enemy, 25, RootRestriction.JUMP);
        Gravity.set(enemy, PRESSED, PRESS_GRAVITY, 25);
        Illuminator.hit(player, enemy, PRESS_DAMAGE, Weakness.GRAVITY);
        IlluminatorFx.pressed(sl, enemy);
    }

    /** An ally (or the caster) weightless for the next second and a bit. */
    private static void weightless(LivingEntity player, ServerLevel sl, LivingEntity ally) {
        if (net.lukario.circleofinevitability.combat.Support.blocked(player, ally)) return;
        Spells.keepEffect(ally, MobEffects.JUMP_BOOST, 1, 40);
        Spells.keepEffect(ally, MobEffects.SLOW_FALLING, 0, 40);
        Spells.keepEffect(ally, MobEffects.SPEED, 0, 40);
        IlluminatorFx.weightless(sl, ally);
    }

    // =========================
    // False Constellations: a fake starry sky over SKY_RADIUS where you aim (SKY_RANGE) for SKY_TICKS (a Zone). Your
    // side inside, you too, cannot be picked out from further than SKY_UNREADABLE (status/Unreadable, put on again
    // every half second): mobs further away forget them, and abilities cannot choose them from further. A Meteor
    // Shower whose middle is under it is METEOR_STRONG stronger. Cast again: the old sky ends.
    // =========================
    static AbilityWheel.Result falseConstellations(LivingEntity player, ServerLevel sl) {
        Vec3 center = Spells.aimGround(player, sl, SKY_RANGE);
        String tag = "il_false_sky_" + player.getId() + "_" + sl.getGameTime();
        Zone.close(player, SKY);
        IlluminatorFx.falseConstellations(sl, center, SKY_RADIUS, SKY_TICKS, tag);
        IlluminatorFx.status(player, Status.FALSE_SKY, SKY_TICKS);
        Zone.at(center, SKY_RADIUS)
                .height(IlluminatorFx.FALSE_SKY_HEIGHT + 1.0)
                .lasts(SKY_TICKS)
                .every(10)
                .affects(one -> !Spells.isEnemy(player, one))
                .onInside((ally, inside) -> Unreadable.apply(ally, 20, SKY_UNREADABLE))
                .onPulse(ticksLeft -> {
                    if (Zone.covers(player, SKY, player.position())) Unreadable.apply(player, 20, SKY_UNREADABLE);
                })
                .onEnd(() -> {
                    IlluminatorFx.falseConstellationsEnd(sl, center, SKY_RADIUS, tag);
                    IlluminatorFx.statusEnd(player, Status.FALSE_SKY);
                })
                .open(player, sl, SKY);
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Meteor Shower: METEOR_COUNT meteors, one every METEOR_EVERY, each on a random spot of ground within METEOR_AREA of
    // where you aim (METEOR_RANGE). Each shows a warning ring and lands at IlluminatorFx.METEOR_LANDS: METEOR_DAMAGE (a
    // starlight hit) and a shove to every enemy within METEOR_RADIUS; allies take nothing. Under your own false sky:
    // METEOR_STRONG times the damage, METEOR_STRONG_RADIUS wide. Light only: no fire, no broken ground.
    // =========================
    static AbilityWheel.Result meteorShower(LivingEntity player, ServerLevel sl) {
        Vec3 center = Spells.aimGround(player, sl, METEOR_RANGE);
        boolean strong = Zone.covers(player, SKY, center);
        double radius = strong ? METEOR_STRONG_RADIUS : METEOR_RADIUS;
        float damage = strong ? METEOR_DAMAGE * METEOR_STRONG : METEOR_DAMAGE;
        IlluminatorFx.meteorShower(sl, center, METEOR_AREA);
        for (int i = 0; i < METEOR_COUNT; i++) {
            Later.run(sl, i * METEOR_EVERY, () -> {
                if (!Spells.casterStillHere(player, sl)) return;
                Vec3 ground = meteorSpot(player, sl, center);
                IlluminatorFx.meteor(sl, ground, strong);
                Later.run(sl, IlluminatorFx.METEOR_LANDS, () -> {
                    if (!Spells.casterStillHere(player, sl)) return;
                    for (LivingEntity enemy : Spells.enemiesAround(player, sl, ground.add(0, 0.5, 0), radius)) {
                        Illuminator.hit(player, enemy, damage, Weakness.STARLIGHT);
                        if (enemy.isAlive()) Spells.push(enemy, ground, METEOR_SHOVE, 0.3);
                    }
                });
            });
        }
        return AbilityWheel.Result.DONE;
    }

    /** A random spot of ground within METEOR_AREA of the middle (the middle itself if none is found). */
    private static Vec3 meteorSpot(LivingEntity player, ServerLevel sl, Vec3 center) {
        for (int tries = 0; tries < 6; tries++) {
            double angle = sl.getRandom().nextDouble() * Math.PI * 2;
            double far = Math.sqrt(sl.getRandom().nextDouble()) * METEOR_AREA;
            Vec3 ground = Spells.groundNear(player, sl, center.add(Math.cos(angle) * far, 0, Math.sin(angle) * far), 4);
            if (ground != null) return ground;
        }
        return center;
    }

    // =========================
    // Kept tidy by Illuminator
    // =========================

    /** After a relog, a death or a logout: the ladders this player started are forgotten. */
    static void forget(LivingEntity player) {
        LADDERS.values().removeIf(ladder -> ladder.caster().equals(player.getUUID()));
    }
}
