package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.AbominationFx.Status;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.Hurled;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Bleeding;
import net.lukario.circleofinevitability.status.DamageLink;
import net.lukario.circleofinevitability.status.Misfortune;
import net.lukario.circleofinevitability.status.NoHeal;
import net.lukario.circleofinevitability.status.Slumber;
import net.lukario.circleofinevitability.status.Vulnerable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Abomination, key 6: the Chains wheel (Puppet, Sequence 4; and Chained, Sequence 0). docs/ABOMINATION.md, "Key 6 wheel".
 *
 *   Animate Objects     (Puppet)  every loose thing near you (dropped items, arrows, armor stands, boats; never
 *                                 blocks) rises on strings and is thrown at the enemies in sight (combat/Hurled).
 *                                 Arrows enemies shot become yours. More and further from Abomination (1)
 *   Grave Shackles      (Puppet)  black chains hold every enemy near you, and for a while they feel each other's
 *                                 pain (status/DamageLink). From Chained (0) every enemy in sight, and the chains
 *                                 hold for as long as you hold your ground
 *   Thorned Mausoleum   (Chained) a sphere of thorns and bandages round where you aim: no enemy gets out or in,
 *                                 those inside are slowed, cannot heal and bleed; allies inside are shielded (combat/Dome)
 *   Coffin of Ages      (Chained) an enemy sealed asleep and untouchable in a coffin, waking cursed; aimed at
 *                                 nothing, you seal yourself and rise healed, cleansed, in a burst of thorns
 *   Thorn Garden        (Chained) thorns and roses over a wide circle: enemies slow, are held and bleed the longer
 *                                 they stay; allies are healed and shielded by the roses
 *
 * Sneak casts the last pick again without the wheel (no cooldown, double the cost).
 */
public final class AbominationChains {

    private AbominationChains() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int CHAINS_SEQUENCE = 4;                        // the key
    private static final float QUICK_CAST_COST = 2f;

    // Animate Objects
    private static final int ANIMATE_SEQUENCE = 4;
    private static final int ANIMATE_COST = 3000;
    private static final int ANIMATE_COOLDOWN = 200;
    private static final double ANIMATE_RADIUS = 16.0;           // things this close to you rise...
    private static final int ANIMATE_MOST = 12;                  // ...this many at most
    private static final int ANIMATE_GREAT_SEQUENCE = 1;         // Abomination: further and more
    private static final double ANIMATE_GREAT_RADIUS = 32.0;
    private static final int ANIMATE_GREAT_MOST = 30;
    private static final double ANIMATE_SIGHT = 60.0;            // enemies within this many degrees of the crosshair...
    private static final double ANIMATE_REACH = 32.0;            // ...and this far are thrown at
    private static final float ANIMATE_DAMAGE = 10f;
    private static final double ANIMATE_SPEED = 1.4;
    private static final int ANIMATE_STAGGER = 2;                // ticks between two throws
    private static final int ANIMATE_FLIGHT = 60;

    // Grave Shackles
    private static final int SHACKLES_SEQUENCE = 4;
    private static final int SHACKLES_COST = 4000;
    private static final int SHACKLES_COOLDOWN = 400;
    private static final double SHACKLES_RADIUS = 10.0;
    private static final int SHACKLES_MOST = 8;
    private static final int SHACKLES_HOLD = 80;                 // held in place this long...
    private static final int SHACKLES_LINK = 160;                // ...and feel each other's pain this long
    private static final float SHACKLES_SHARE = 0.5f;            // half of what one takes, the others take too
    private static final int SHACKLES_CHAINED_SEQUENCE = 0;      // Chained: every enemy in sight...
    private static final double SHACKLES_CHAINED_RANGE = 24.0;
    private static final int SHACKLES_CHAINED_MOST = 24;
    private static final double SHACKLES_GROUND = 1.5;           // ...and held while you stay this close to where you cast

    // Thorned Mausoleum
    private static final int MAUSOLEUM_SEQUENCE = 0;
    private static final int MAUSOLEUM_COST = 40000;
    private static final int MAUSOLEUM_COOLDOWN = 2400;
    private static final double MAUSOLEUM_RANGE = 32.0;
    private static final double MAUSOLEUM_RADIUS = 6.0;          // 12 blocks across
    private static final int MAUSOLEUM_TICKS = 600;
    private static final float MAUSOLEUM_SHIELD = 0.4f;          // allies inside take 40% less
    private static final float MAUSOLEUM_THORNS = 8f;            // enemies inside bleed this much a second
    private static final int MAUSOLEUM_SLOW = 1;                 // Slowness II inside

    // Coffin of Ages
    private static final int COFFIN_SEQUENCE = 0;
    private static final int COFFIN_COST = 30000;
    private static final int COFFIN_COOLDOWN = 1800;
    private static final double COFFIN_RANGE = 32.0;
    private static final int COFFIN_TICKS = 240;                 // an enemy sleeps sealed this long...
    private static final float COFFIN_BOSS_HEALTH = 150f;        // (not a boss)
    private static final int COFFIN_CURSE = 3;                   // ...and wakes with Misfortune III...
    private static final int COFFIN_CURSE_TICKS = 300;
    private static final float COFFIN_VULNERABLE = 0.2f;         // ...and taking 20% more, as long
    private static final int COFFIN_SELF_TICKS = 120;            // sealed yourself this long...
    private static final float COFFIN_SELF_HEAL = 0.5f;          // ...you rise healed by half of your health...
    private static final double COFFIN_BURST = 6.0;              // ...and the thorns hold everything this close...
    private static final int COFFIN_BURST_HOLD = 60;             // ...this long
    private static final float COFFIN_BURST_DAMAGE = 12f;

    // Thorn Garden
    private static final int GARDEN_SEQUENCE = 0;
    private static final int GARDEN_COST = 35000;
    private static final int GARDEN_COOLDOWN = 1800;
    private static final double GARDEN_RANGE = 32.0;
    private static final double GARDEN_RADIUS = 10.0;            // 20 blocks across
    private static final int GARDEN_TICKS = 400;
    private static final int GARDEN_SLOW_EVERY = 20;             // one level of slowness more every second inside...
    private static final int GARDEN_MOST_SLOW = 3;               // ...up to IV
    private static final int GARDEN_HELD_AFTER = 60;             // after 3 seconds inside: held while in it
    private static final float GARDEN_THORNS = 2f;               // bleeding a second, one more every GARDEN_THORNS_EVERY...
    private static final int GARDEN_THORNS_EVERY = 40;
    private static final float GARDEN_MOST_THORNS = 6f;          // ...up to this
    private static final float GARDEN_HEAL = 2f;                 // allies (and you) heal this much a second...
    private static final int GARDEN_ABSORPTION = 0;              // ...and have Absorption I

    // =========================
    // Names and what is saved
    // =========================
    private static final String SHACKLES_TAG = "abomination_shackles_tag";
    private static final String MAUSOLEUM_DOME = "abomination_mausoleum";
    private static final String MAUSOLEUM_ZONE = "abomination_mausoleum_inside";
    private static final String GARDEN_ENEMIES = "abomination_garden_thorns";
    private static final String GARDEN_ALLIES = "abomination_garden_roses";
    /** On anyone in a Coffin of Ages: until this game time nothing can hurt it. */
    static final String SEALED_UNTIL = "abomination_sealed_until";

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("abomination_chains", "Chains")
            .radius(80)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .castsWhileSilenced(Abomination::castsThroughHush)
            .entry("animate_objects", AbominationFx.WHEEL_ANIMATE_OBJECTS, "Animate Objects", ANIMATE_SEQUENCE,
                    ANIMATE_COST, ANIMATE_COOLDOWN, AbominationChains::animateObjects)
            .entry("grave_shackles", AbominationFx.WHEEL_GRAVE_SHACKLES, "Grave Shackles", SHACKLES_SEQUENCE,
                    SHACKLES_COST, SHACKLES_COOLDOWN, AbominationChains::graveShackles)
            .entry("thorned_mausoleum", AbominationFx.WHEEL_THORNED_MAUSOLEUM, "Thorned Mausoleum", MAUSOLEUM_SEQUENCE,
                    MAUSOLEUM_COST, MAUSOLEUM_COOLDOWN, AbominationChains::thornedMausoleum)
            .entry("coffin_of_ages", AbominationFx.WHEEL_COFFIN_OF_AGES, "Coffin of Ages", COFFIN_SEQUENCE,
                    COFFIN_COST, COFFIN_COOLDOWN, AbominationChains::coffinOfAges)
            .entry("thorn_garden", AbominationFx.WHEEL_THORN_GARDEN, "Thorn Garden", GARDEN_SEQUENCE,
                    GARDEN_COST, GARDEN_COOLDOWN, AbominationChains::thornGarden);

    // =========================
    // Ability 6 - Chains
    // Normal: the Chains wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void abominationChains(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!Abomination.ready(player, bypassClassCheck, CHAINS_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Animate Objects: every loose thing within ANIMATE_RADIUS of you (Spells.looseThingsAround: dropped items,
    // projectiles, armor stands, boats, minecarts; never blocks), ANIMATE_MOST at most, rises on purple strings and
    // is thrown at the enemies in sight (ANIMATE_SIGHT, ANIMATE_REACH), shared out in turn, one every
    // ANIMATE_STAGGER. Each hits once for ANIMATE_DAMAGE. Arrows and other projectiles are turned round and become
    // yours. Abomination (1): ANIMATE_GREAT_RADIUS, ANIMATE_GREAT_MOST. Nothing to move, or no one to throw at: refused.
    // =========================
    private static AbilityWheel.Result animateObjects(ServerPlayer player, ServerLevel sl) {
        boolean great = Beyonder.hasSequence(player, ANIMATE_GREAT_SEQUENCE);
        List<Entity> things = Spells.looseThingsAround(player, sl, player.position(), great ? ANIMATE_GREAT_RADIUS : ANIMATE_RADIUS);
        int most = great ? ANIMATE_GREAT_MOST : ANIMATE_MOST;
        if (things.size() > most) things = new ArrayList<>(things.subList(0, most));
        if (things.isEmpty()) {
            player.sendSystemMessage(Component.literal("There is nothing loose near you to move."));
            return AbilityWheel.Result.REFUSED;
        }
        List<LivingEntity> enemies = new ArrayList<>(Spells.enemiesInSight(player, sl, ANIMATE_REACH, ANIMATE_SIGHT));
        enemies.removeIf(enemy -> !Spells.canBeChosen(player, enemy));
        if (enemies.isEmpty()) {
            player.sendSystemMessage(Component.literal("There is no one before you to throw at."));
            return AbilityWheel.Result.REFUSED;
        }

        AbominationFx.animateObjects(sl, player, things);
        for (int i = 0; i < things.size(); i++) {
            LivingEntity enemy = enemies.get(i % enemies.size());
            Hurled.at(things.get(i), player, enemy)
                    .risesFor(AbominationFx.OBJECTS_RISE + i * ANIMATE_STAGGER)
                    .speed(ANIMATE_SPEED)
                    .damage(ANIMATE_DAMAGE)
                    .lasts(ANIMATE_FLIGHT)
                    .onLaunch(thing -> AbominationFx.hurlLaunch(sl, thing))
                    .onTrail(thing -> AbominationFx.hurledTrail(sl, thing))
                    .onHit((thing, victim) -> AbominationFx.hurledHit(sl, thing.position()))
                    .launch(sl);
        }
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Grave Shackles: black chains rise from the ground round every enemy within SHACKLES_RADIUS (SHACKLES_MOST at
    // most). From AbominationFx.SHACKLES_LOCK they are held in place for SHACKLES_HOLD, and for SHACKLES_LINK they feel
    // each other's pain: SHACKLES_SHARE of what one takes, each of the others takes too (status/DamageLink; a lone
    // enemy is only held). Chained (0): every enemy in sight within SHACKLES_CHAINED_RANGE (SHACKLES_CHAINED_MOST),
    // and while you stay within SHACKLES_GROUND of where you cast, the chains hold for the whole link.
    // =========================
    private static AbilityWheel.Result graveShackles(ServerPlayer player, ServerLevel sl) {
        boolean chained = Beyonder.hasSequence(player, SHACKLES_CHAINED_SEQUENCE);
        List<LivingEntity> bound = new ArrayList<>();
        List<LivingEntity> near = chained
                ? Spells.enemiesAround(player, sl, player.position(), SHACKLES_CHAINED_RANGE)
                : Spells.enemiesAround(player, sl, player.position(), SHACKLES_RADIUS);
        near.sort((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)));
        for (LivingEntity enemy : near) {
            if (bound.size() >= (chained ? SHACKLES_CHAINED_MOST : SHACKLES_MOST)) break;
            if (!Spells.canBeChosen(player, enemy)) continue;
            if (chained && !player.hasLineOfSight(enemy)) continue;
            bound.add(enemy);
        }
        if (bound.isEmpty()) {
            player.sendSystemMessage(Component.literal("There is no one near you to shackle."));
            return AbilityWheel.Result.REFUSED;
        }

        String tag = "ab_shackles_" + player.getId() + "_" + sl.getGameTime();
        player.getPersistentData().putString(SHACKLES_TAG, tag);
        int shown = chained ? SHACKLES_LINK : SHACKLES_HOLD;
        AbominationFx.graveShackles(sl, bound, AbominationFx.SHACKLES_LOCK + shown, tag);
        AbominationFx.status(player, Status.SHACKLES, AbominationFx.SHACKLES_LOCK + SHACKLES_LINK);
        Vec3 ground = player.position();
        Later.run(sl, AbominationFx.SHACKLES_LOCK, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            List<LivingEntity> held = new ArrayList<>();
            for (LivingEntity enemy : bound) {
                if (!enemy.isAlive() || enemy.level() != sl) continue;
                Root.apply(enemy, SHACKLES_HOLD, RootRestriction.MOVEMENT);
                held.add(enemy);
            }
            DamageLink.share(held, SHACKLES_SHARE, SHACKLES_LINK, player);
            if (chained) holdGround(player, sl, held, ground, tag, SHACKLES_LINK - SHACKLES_HOLD);
            Later.run(sl, shown, () -> AbominationFx.graveShacklesEnd(sl, bound, tag));
            Later.run(sl, SHACKLES_LINK, () -> {
                if (player.getPersistentData().getStringOr(SHACKLES_TAG, "").equals(tag)) {
                    player.getPersistentData().remove(SHACKLES_TAG);
                    AbominationFx.statusEnd(player, Status.SHACKLES);
                }
            });
        });
        return AbilityWheel.Result.DONE;
    }

    /** Chained: the hold is renewed once a second while the caster stands their ground, up to 'left' more ticks. */
    private static void holdGround(Player player, ServerLevel sl, List<LivingEntity> held, Vec3 ground, String tag, int left) {
        Later.run(sl, Math.max(1, SHACKLES_HOLD - 20), () -> renewHold(player, sl, held, ground, tag, left));
    }

    private static void renewHold(Player player, ServerLevel sl, List<LivingEntity> held, Vec3 ground, String tag, int left) {
        if (left <= 0 || !Spells.casterStillHere(player, sl)) return;
        if (!player.getPersistentData().getStringOr(SHACKLES_TAG, "").equals(tag)) return;     // shackled others since
        if (player.position().distanceTo(ground) > SHACKLES_GROUND) return;                    // you moved: they break
        for (LivingEntity enemy : held) {
            if (enemy.isAlive() && enemy.level() == sl) Root.apply(enemy, 40, RootRestriction.MOVEMENT);
        }
        Later.run(sl, 20, () -> renewHold(player, sl, held, ground, tag, left - 20));
    }

    // =========================
    // Thorned Mausoleum: a sphere of black thorns and yellow bandages closes round the place you aim at
    // (MAUSOLEUM_RANGE), MAUSOLEUM_RADIUS round, for MAUSOLEUM_TICKS. From AbominationFx.MAUSOLEUM_SEALED nothing crosses
    // it but your allies (combat/Dome: enemies kept in and out, their projectiles stopped), allies inside take
    // MAUSOLEUM_SHIELD less, and enemies inside are slowed, cannot heal and bleed MAUSOLEUM_THORNS a second.
    // =========================
    private static AbilityWheel.Result thornedMausoleum(ServerPlayer player, ServerLevel sl) {
        Vec3 center = Spells.aimGround(player, sl, MAUSOLEUM_RANGE);
        String tag = "ab_mausoleum_" + player.getId() + "_" + sl.getGameTime();
        Dome.close(player, MAUSOLEUM_DOME);                                // one at a time: the old one crumbles first
        Zone.close(player, MAUSOLEUM_ZONE);
        AbominationFx.mausoleum(sl, center, MAUSOLEUM_RADIUS, MAUSOLEUM_TICKS, tag);
        AbominationFx.status(player, Status.MAUSOLEUM, MAUSOLEUM_TICKS);
        int stands = MAUSOLEUM_TICKS - AbominationFx.MAUSOLEUM_SEALED;
        Later.run(sl, AbominationFx.MAUSOLEUM_SEALED, () -> {
            if (!Spells.casterStillHere(player, sl)) {
                AbominationFx.mausoleumEnd(sl, center, MAUSOLEUM_RADIUS, tag);
                return;
            }
            Dome.sphere(center, MAUSOLEUM_RADIUS)
                    .lasts(stands)
                    .keepsEnemiesIn()
                    .stopsProjectiles()
                    .shields(MAUSOLEUM_SHIELD)
                    .onEnd(() -> {
                        Zone.close(player, MAUSOLEUM_ZONE);
                        AbominationFx.mausoleumEnd(sl, center, MAUSOLEUM_RADIUS, tag);
                        AbominationFx.statusEnd(player, Status.MAUSOLEUM);
                    })
                    .raise(player, sl, MAUSOLEUM_DOME);
            Zone.at(center, MAUSOLEUM_RADIUS)                                // the half of the sphere above ground
                    .height(MAUSOLEUM_RADIUS)
                    .lasts(stands)
                    .every(20)
                    .onInside((enemy, inside) -> {
                        if (enemy.position().distanceTo(center) > MAUSOLEUM_RADIUS + 0.5) return;
                        Spells.keepEffect(enemy, MobEffects.SLOWNESS, MAUSOLEUM_SLOW, 40);
                        NoHeal.apply(enemy, 40);
                        Bleeding.apply(enemy, player, 40, MAUSOLEUM_THORNS);
                        if (inside % 40 == 0) AbominationFx.mausoleumThorn(sl, enemy);
                    })
                    .onPulse(left -> AbominationFx.mausoleumTick(sl, center, MAUSOLEUM_RADIUS))
                    .open(player, sl, MAUSOLEUM_ZONE);
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Coffin of Ages. Aimed at an enemy (COFFIN_RANGE; not a boss, COFFIN_BOSS_HEALTH): bandages wind round it and it
    // is held at once; from AbominationFx.COFFIN_SHUT it lies sealed in a coffin for COFFIN_TICKS, asleep
    // (status/Slumber, ETERNAL), held and untouchable, and it wakes with Misfortune COFFIN_CURSE and taking
    // COFFIN_VULNERABLE more for COFFIN_CURSE_TICKS. Aimed at nothing: you seal yourself for COFFIN_SELF_TICKS, held
    // and untouchable, and rise healed by COFFIN_SELF_HEAL of your health and cleansed, in a burst of thorns that
    // strikes and holds every enemy within COFFIN_BURST for COFFIN_BURST_HOLD.
    // =========================
    private static AbilityWheel.Result coffinOfAges(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, COFFIN_RANGE);
        if (target != null && target.getMaxHealth() >= COFFIN_BOSS_HEALTH) {
            player.sendSystemMessage(Component.literal("That one is too great for any coffin."));
            return AbilityWheel.Result.REFUSED;
        }
        if (target != null) coffinEnemy(player, sl, target);
        else coffinSelf(player, sl);
        return AbilityWheel.Result.DONE;
    }

    private static void coffinEnemy(Player player, ServerLevel sl, LivingEntity target) {
        String tag = "ab_coffin_" + target.getId() + "_" + sl.getGameTime();
        int whole = AbominationFx.COFFIN_SHUT + COFFIN_TICKS;
        AbominationFx.coffinOfAges(sl, target, whole + 10, tag);
        Root.apply(target, AbominationFx.COFFIN_SHUT, RootRestriction.EVERYTHING);
        Later.run(sl, AbominationFx.COFFIN_SHUT, () -> {
            if (!target.isAlive() || target.level() != sl) return;
            long until = sl.getGameTime() + COFFIN_TICKS;
            target.getPersistentData().putLong(SEALED_UNTIL, until);
            Slumber.put(target, COFFIN_TICKS, Slumber.Kind.ETERNAL);
            Root.apply(target, COFFIN_TICKS, RootRestriction.EVERYTHING);
            Later.run(sl, COFFIN_TICKS, () -> {
                if (target.getPersistentData().getLongOr(SEALED_UNTIL, 0L) != until) return;     // sealed again since
                if (!target.isAlive() || target.level() != sl) return;
                AbominationFx.coffinOpen(sl, target, tag);
                Later.run(sl, AbominationFx.COFFIN_OPENED, () -> {
                    target.getPersistentData().remove(SEALED_UNTIL);
                    if (!target.isAlive()) return;
                    Slumber.wake(target);
                    Root.remove(target, RootRestriction.EVERYTHING.toArray(new RootRestriction[0]));
                    Misfortune.add(target, player, COFFIN_CURSE, Misfortune.MAX_LEVEL, Abomination.curseTicks(player, COFFIN_CURSE_TICKS));
                    Vulnerable.add(target, COFFIN_VULNERABLE, COFFIN_VULNERABLE, Abomination.curseTicks(player, COFFIN_CURSE_TICKS));
                });
            });
        });
    }

    private static void coffinSelf(Player player, ServerLevel sl) {
        String tag = "ab_coffin_" + player.getId() + "_" + sl.getGameTime();
        long until = sl.getGameTime() + COFFIN_SELF_TICKS;
        player.getPersistentData().putLong(SEALED_UNTIL, until);                // untouchable at once: it is a refuge
        Root.apply(player, COFFIN_SELF_TICKS, RootRestriction.EVERYTHING);
        AbominationFx.coffinOfAges(sl, player, COFFIN_SELF_TICKS + 10, tag);
        AbominationFx.status(player, Status.COFFIN, COFFIN_SELF_TICKS);
        Later.run(sl, COFFIN_SELF_TICKS, () -> {
            if (player.getPersistentData().getLongOr(SEALED_UNTIL, 0L) != until || !Spells.casterStillHere(player, sl)) return;
            AbominationFx.coffinOpen(sl, player, tag);
            Later.run(sl, AbominationFx.COFFIN_OPENED, () -> {
                player.getPersistentData().remove(SEALED_UNTIL);
                if (!Spells.casterStillHere(player, sl)) return;
                Root.remove(player, RootRestriction.EVERYTHING.toArray(new RootRestriction[0]));
                player.heal(player.getMaxHealth() * COFFIN_SELF_HEAL);
                Spells.cleanse(player);
                Bleeding.remove(player);
                AbominationFx.coffinRise(sl, player, COFFIN_BURST);
                for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), COFFIN_BURST)) {
                    Spells.strike(player, enemy, COFFIN_BURST_DAMAGE);
                    if (enemy.isAlive()) Root.apply(enemy, COFFIN_BURST_HOLD, RootRestriction.MOVEMENT);
                }
            });
        });
    }

    /** True while the entity lies sealed in a Coffin of Ages: nothing can hurt it. */
    static boolean isSealed(Entity entity) {
        return Abomination.isActive(entity, SEALED_UNTIL);
    }

    // =========================
    // Thorn Garden: a garden of black thorns and bright roses grows over GARDEN_RADIUS round where you aim
    // (GARDEN_RANGE) for GARDEN_TICKS, from the moment it has grown (AbominationFx.GARDEN_GROWN). Enemies in it are
    // slowed one level more every GARDEN_SLOW_EVERY (up to GARDEN_MOST_SLOW), held after GARDEN_HELD_AFTER, and bleed
    // GARDEN_THORNS a second, more the longer they stay. Allies in it, and you, heal GARDEN_HEAL a second and have
    // Absorption from the roses.
    // =========================
    private static AbilityWheel.Result thornGarden(ServerPlayer player, ServerLevel sl) {
        Vec3 center = Spells.aimGround(player, sl, GARDEN_RANGE);
        String tag = "ab_garden_" + player.getId() + "_" + sl.getGameTime();
        Zone.close(player, GARDEN_ENEMIES);                                 // one garden at a time
        Zone.close(player, GARDEN_ALLIES);
        AbominationFx.thornGarden(sl, center, GARDEN_RADIUS, GARDEN_TICKS, tag);
        AbominationFx.status(player, Status.THORN_GARDEN, GARDEN_TICKS);
        int grows = GARDEN_TICKS - AbominationFx.GARDEN_GROWN;
        Later.run(sl, AbominationFx.GARDEN_GROWN, () -> {
            if (!Spells.casterStillHere(player, sl)) {
                AbominationFx.thornGardenEnd(sl, center, tag);
                return;
            }
            Zone.at(center, GARDEN_RADIUS)
                    .lasts(grows)
                    .every(20)
                    .onInside((enemy, inside) -> {
                        int depth = Math.min(GARDEN_MOST_SLOW, inside / GARDEN_SLOW_EVERY);
                        Spells.keepEffect(enemy, MobEffects.SLOWNESS, depth, 40);
                        boolean held = inside >= GARDEN_HELD_AFTER;
                        if (held) Root.apply(enemy, 30, RootRestriction.MOVEMENT);
                        float thorns = Math.min(GARDEN_MOST_THORNS, GARDEN_THORNS + inside / GARDEN_THORNS_EVERY);
                        Bleeding.apply(enemy, player, 40, thorns);
                        if (inside % 40 == 0 || inside == GARDEN_HELD_AFTER) AbominationFx.gardenThorn(sl, enemy, inside == GARDEN_HELD_AFTER);
                    })
                    .onPulse(left -> {
                        AbominationFx.gardenTick(sl, center, GARDEN_RADIUS);
                        // the owner is never inside a zone: the roses look after them here
                        if (player.position().distanceTo(center) <= GARDEN_RADIUS) rose(sl, player, left);
                    })
                    .onEnd(() -> {
                        AbominationFx.thornGardenEnd(sl, center, tag);
                        AbominationFx.statusEnd(player, Status.THORN_GARDEN);
                    })
                    .open(player, sl, GARDEN_ENEMIES);
            Zone.at(center, GARDEN_RADIUS)
                    .lasts(grows)
                    .every(20)
                    .affects(one -> !Spells.isEnemy(player, one))
                    .onInside((ally, inside) -> rose(sl, ally, inside))
                    .open(player, sl, GARDEN_ALLIES);
        });
        return AbilityWheel.Result.DONE;
    }

    /** The roses tend one ally once a second; the look every other second. */
    private static void rose(ServerLevel sl, LivingEntity ally, int beat) {
        if (ally.getHealth() < ally.getMaxHealth()) ally.heal(GARDEN_HEAL);
        Spells.keepEffect(ally, MobEffects.ABSORPTION, GARDEN_ABSORPTION, 100);
        if ((beat / 20) % 2 == 0) AbominationFx.roseHeal(sl, ally);
    }

    // =========================
    // Called from Abomination
    // =========================

    /** After a relog or a death: start clean. */
    static void clearState(Player player) {
        CompoundTag data = player.getPersistentData();
        data.remove(SHACKLES_TAG);
        data.remove(SEALED_UNTIL);
    }
}
