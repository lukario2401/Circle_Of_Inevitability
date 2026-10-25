package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobs;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStarsFx.Status;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.combat.Echo;
import net.lukario.circleofinevitability.combat.Enclosure;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Seal;
import net.lukario.circleofinevitability.status.Starlit;
import net.lukario.circleofinevitability.status.Toy;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Key Of Stars, key 7: the Starlight wheel (Planeswalker, Sequence 2, up to Door). docs/KEY_OF_STARS.md, "Key 7 wheel".
 *
 *   Toy Box (2)               a veil where you aim: small enemies in it become wind-up toys (status/Toy), the rest slow
 *   Replication (2)           the last ability anyone cast near you, cast again by you at once (combat/Casts)
 *   Space Fragmentation (1)   nine planets circle where you aim: their gravity drags and shreds, then they collapse
 *   Starlight Seal (1)        a caught enemy is Sealed (status/Seal: no abilities, no teleports, it mends itself);
 *                             aimed at an ally, their connections are sealed instead
 *   Space-Time Labyrinth (1)  a maze: enemies in it are slowed, cannot teleport, and walk out only to come back in
 *                             (combat/Enclosure)
 *   Alternate World (0)       a kingdom of Doors round you: no exits for enemies, free travel for you and yours, roots
 *                             and Seals on enemies twice as long; cast again to end it
 *   Omnipresence (0)          six afterimages round you repeat every ability you cast (combat/Echo) and take the hits
 *                             meant for you
 *   Close the Door (0)        every enemy near loses travel, their Seals and roots double, the hidden are forced out
 *
 * Sneak casts the last pick again without the wheel. Every entry's cost goes through KeyOfStars.wheelCostFor and every
 * cooldown through KeyOfStars.cooldownFor. The looks are KeyOfStarsFx's; every number is right below.
 */
public final class KeyOfStarsStarlight {

    private KeyOfStarsStarlight() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int STARLIGHT_SEQUENCE = 2;                    // the key (Planeswalker)
    static final int KEY_OF_STARS_SEQUENCE = 1;
    static final int DOOR_SEQUENCE = 0;

    // Toy Box
    private static final int TOY_COST = 18000;
    private static final int TOY_COOLDOWN = 600;
    private static final double TOY_RANGE = 28.0;
    private static final double TOY_RADIUS = 8.0;
    private static final int TOY_TICKS = 160;
    private static final float TOY_MAX_HEALTH = 150f;           // bigger than this (and players) are only slowed
    private static final int TOY_SLOWED = 60;

    // Replication
    private static final int REPLICATION_COST = 15000;
    private static final int REPLICATION_COOLDOWN = 200;
    private static final double REPLICATION_RANGE = 32.0;
    private static final int REPLICATION_AGE = 100;             // cast no longer ago than this
    private static final float REPLICATION_STRENGTH = 0.8f;

    // Space Fragmentation
    private static final int FRAGMENTATION_COST = 30000;
    private static final int FRAGMENTATION_COOLDOWN = 900;
    private static final double FRAGMENTATION_RANGE = 32.0;
    private static final double FRAGMENTATION_RADIUS = 12.0;
    private static final int FRAGMENTATION_TICKS = 120;
    private static final float FRAGMENTATION_DPS = 25f;         // each second, to every enemy inside
    private static final float FRAGMENTATION_COLLAPSE = 120f;   // the shatter at the end
    private static final double FRAGMENTATION_PULL = 0.35;      // blocks a tick at most
    private static final double FRAGMENTATION_BEND = 0.25;      // shots are pushed away this much a beat
    private static final double ANNIHILATION_RADIUS = 40.0;     // Door: the planet-annihilation
    private static final float ANNIHILATION_DPS = 60f;
    private static final float ANNIHILATION_COLLAPSE = 400f;

    // Starlight Seal
    private static final int SEAL_COST = 25000;
    private static final int SEAL_COOLDOWN = 600;
    private static final double SEAL_RANGE = 24.0;
    private static final int SEAL_TICKS = 160;
    private static final int SEAL_BOSS = 80;
    private static final int SEAL_ALLY = 240;

    // Space-Time Labyrinth
    private static final int LABYRINTH_COST = 30000;
    private static final int LABYRINTH_COOLDOWN = 1200;
    private static final double LABYRINTH_RANGE = 32.0;
    private static final double LABYRINTH_RADIUS = 14.0;
    private static final int LABYRINTH_TICKS = 300;

    // Alternate World
    private static final int WORLD_COST = 60000;
    private static final int WORLD_COOLDOWN = 2400;
    private static final double WORLD_RADIUS = 40.0;
    private static final int WORLD_TICKS = 1200;
    private static final float WORLD_HOLDS_LONGER = 2f;         // roots and Seals on enemies inside

    // Omnipresence
    private static final int OMNI_COST = 45000;
    private static final int OMNI_COOLDOWN = 1800;
    private static final int OMNI_TICKS = 200;
    private static final int AFTERIMAGES = 6;
    private static final float OMNI_STRENGTH = 0.6f;

    // Close the Door
    private static final int CLOSE_COST = 40000;
    private static final int CLOSE_COOLDOWN = 1800;
    private static final double CLOSE_RADIUS = 48.0;
    private static final int CLOSE_TICKS = 300;
    private static final float CLOSE_LONGER = 2f;               // Seals, roots and Starlit marks on enemies

    // =========================
    // Names and what is kept
    // =========================
    private static final String TOY_ZONE = "key_of_stars_toy_box";              // combat/Zone
    private static final String FRAGMENTATION_ZONE = "key_of_stars_fragmentation";
    private static final String CLOSE_ZONE = "key_of_stars_close_the_door";
    static final String LABYRINTH = "key_of_stars_labyrinth";                    // combat/Enclosure
    static final String WORLD = "key_of_stars_alternate_world";
    private static final String OMNI_UNTIL = "key_of_stars_omnipresence_until";
    private static final String OMNI_LEFT = "key_of_stars_omnipresence_left";    // which afterimages still stand (bits)

    /** The toys of each caster's Toy Box, released when it closes. */
    private static final Map<UUID, List<LivingEntity>> TOYS = new HashMap<>();

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("key_of_stars_starlight", "Starlight")
            .radius(84)
            .quickCastCooldown(false).quickCastCost(KeyOfStars.QUICK_CAST_COST)     // sneak: no cooldown, 3x the cost
            .cooldownBy(KeyOfStars::cooldownFor)
            .entry("toy_box", KeyOfStarsFx.WHEEL_TOY_BOX, "Toy Box", STARLIGHT_SEQUENCE, TOY_COST, TOY_COOLDOWN,
                    KeyOfStarsStarlight::toyBox)
            .costBy(KeyOfStars::wheelCostFor)
            .entry("replication", KeyOfStarsFx.WHEEL_REPLICATION, "Replication", STARLIGHT_SEQUENCE, REPLICATION_COST,
                    REPLICATION_COOLDOWN, KeyOfStarsStarlight::replication)
            .costBy(KeyOfStars::wheelCostFor)
            .notCopyable()                                                       // a copy of a copier copies nothing
            .entry("space_fragmentation", KeyOfStarsFx.WHEEL_SPACE_FRAGMENTATION, "Space Fragmentation",
                    KEY_OF_STARS_SEQUENCE, FRAGMENTATION_COST, FRAGMENTATION_COOLDOWN, KeyOfStarsStarlight::spaceFragmentation)
            .costBy(KeyOfStars::wheelCostFor)
            .entry("starlight_seal", KeyOfStarsFx.WHEEL_STARLIGHT_SEAL, "Starlight Seal", KEY_OF_STARS_SEQUENCE, SEAL_COST,
                    SEAL_COOLDOWN, KeyOfStarsStarlight::starlightSeal)
            .costBy(KeyOfStars::wheelCostFor)
            .entry("labyrinth", KeyOfStarsFx.WHEEL_LABYRINTH, "Space-Time Labyrinth", KEY_OF_STARS_SEQUENCE,
                    LABYRINTH_COST, LABYRINTH_COOLDOWN, KeyOfStarsStarlight::labyrinth)
            .costBy(KeyOfStars::wheelCostFor)
            .entry("alternate_world", KeyOfStarsFx.WHEEL_ALTERNATE_WORLD, "Alternate World", DOOR_SEQUENCE, WORLD_COST,
                    WORLD_COOLDOWN, KeyOfStarsStarlight::alternateWorld)
            .costBy(KeyOfStars::wheelCostFor)
            .free(player -> Enclosure.isOpen(player, WORLD))                     // ending it early: free
            .notCopyable()                                                       // its rules are this Pathway's
            .entry("omnipresence", KeyOfStarsFx.WHEEL_OMNIPRESENCE, "Omnipresence", DOOR_SEQUENCE, OMNI_COST,
                    OMNI_COOLDOWN, KeyOfStarsStarlight::omnipresence)
            .costBy(KeyOfStars::wheelCostFor)
            .notCopyable()                                                       // its echoes are this Pathway's
            .entry("close_the_door", KeyOfStarsFx.WHEEL_CLOSE_THE_DOOR, "Close the Door", DOOR_SEQUENCE, CLOSE_COST,
                    CLOSE_COOLDOWN, KeyOfStarsStarlight::closeTheDoor)
            .costBy(KeyOfStars::wheelCostFor);

    // =========================
    // Ability 7 - Starlight
    // Normal: the Starlight wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void keyOfStarsStarlight(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!KeyOfStars.ready(player, bypassClassCheck, STARLIGHT_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    /** A relog or a respawn: what was standing is gone. */
    static void clearState(Player player) {
        player.getPersistentData().remove(OMNI_UNTIL);
        player.getPersistentData().remove(OMNI_LEFT);
        Enclosure.close(player, LABYRINTH);
        Enclosure.close(player, WORLD);
        Zone.close(player, TOY_ZONE);
        Zone.close(player, FRAGMENTATION_ZONE);
        Zone.close(player, CLOSE_ZONE);
    }

    /** Every tick for a Key Of Stars. */
    static void tick(Player player, ServerLevel sl) {
        long until = player.getPersistentData().getLongOr(OMNI_UNTIL, 0L);
        if (until != 0L && until <= sl.getGameTime()) endOmnipresence(player, sl);
    }

    // =========================
    // Toy Box: a veil of TOY_RADIUS where you aim (TOY_RANGE) for TOY_TICKS (combat/Zone). An enemy that steps in (or
    // stands in it) with up to TOY_MAX_HEALTH, and not a player, shrinks into a wind-up toy at KeyOfStarsFx.TOY_SHRUNK
    // (status/Toy: a quarter of its size, much slower, a fifth of its damage) until the box closes. Players and bigger
    // creatures are slowed (Slowness III for TOY_SLOWED). A new Toy Box releases the old one's toys.
    // =========================
    private static AbilityWheel.Result toyBox(ServerPlayer player, ServerLevel sl) {
        Vec3 center = Spells.aimGround(player, sl, TOY_RANGE);
        String tag = "ks_toy_" + player.getId() + "_" + sl.getGameTime();
        Zone.close(player, TOY_ZONE);                                            // the old toys are let go first
        List<LivingEntity> toys = new ArrayList<>();
        TOYS.put(player.getUUID(), toys);
        Zone.at(center, TOY_RADIUS)
                .height(4.0)
                .lasts(TOY_TICKS)
                .every(10)
                .onEnter(enemy -> intoTheBox(player, sl, enemy, toys, tag))
                .onInside((enemy, ticksInside) -> {
                    if (Toy.isToy(enemy) && ticksInside % 20 == 0) KeyOfStarsFx.toyTick(sl, enemy);
                })
                .onEnd(() -> {
                    for (LivingEntity toy : toys) {
                        if (!Toy.isToy(toy)) continue;
                        Toy.release(toy);
                        KeyOfStarsFx.toyReleased(sl, toy, tag);
                    }
                    toys.clear();
                    TOYS.remove(player.getUUID(), toys);
                    KeyOfStarsFx.toyBoxEnd(sl, center, tag);
                })
                .open(player, sl, TOY_ZONE);
        KeyOfStarsFx.toyBox(sl, player, center, TOY_RADIUS, TOY_TICKS, tag);
        return AbilityWheel.Result.DONE;
    }

    private static void intoTheBox(Player player, ServerLevel sl, LivingEntity enemy, List<LivingEntity> toys, String tag) {
        if (Toy.isToy(enemy)) return;
        if (enemy instanceof Player || enemy.getMaxHealth() > TOY_MAX_HEALTH) {
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, TOY_SLOWED, 2));
            KeyOfStarsFx.toySlowed(sl, enemy, TOY_SLOWED);
            return;
        }
        int ticks = Math.max(20, Zone.ticksLeft(player, TOY_ZONE));
        KeyOfStarsFx.toyed(sl, enemy, ticks, tag);
        Later.run(sl, KeyOfStarsFx.TOY_SHRUNK, () -> {
            if (!enemy.isAlive() || !Zone.isOpen(player, TOY_ZONE)) return;
            if (Toy.make(enemy, Math.max(20, Zone.ticksLeft(player, TOY_ZONE)))) toys.add(enemy);
        });
    }

    // =========================
    // Replication: the newest ability another Beyonder cast within REPLICATION_RANGE in the last REPLICATION_AGE ticks
    // (combat/Casts) is cast again by you, now, toward your crosshair, its hits at REPLICATION_STRENGTH. No slot, no
    // roll. Mobs cast nothing, so aiming at a Beyonder mob (blasphemy_slate/beyonder_mobs) within REPLICATION_RANGE
    // replicates one of its Pathway's abilities, up to its Sequence, instead. If nothing was cast, or the copy cannot
    // go off (no target), nothing is spent.
    // =========================
    private static AbilityWheel.Result replication(ServerPlayer player, ServerLevel sl) {
        LivingEntity aimed = Spells.aimEnemy(player, sl, REPLICATION_RANGE);
        Casts.Cast fromMob = BeyonderMobs.castOf(aimed, sl, player.getRandom());         // null unless a Beyonder mob
        Casts.Cast cast = fromMob != null ? fromMob : Casts.latestNear(sl, player.position(), REPLICATION_RANGE,
                REPLICATION_AGE, one -> !one.caster().equals(player.getUUID()) && one.copyable());
        if (cast == null) {
            player.sendSystemMessage(Component.literal("Nothing was cast near you to replicate."));
            return AbilityWheel.Result.REFUSED;
        }
        LivingEntity source = fromMob != null ? aimed : sl.getPlayerByUUID(cast.caster());
        if (source != null) KeyOfStarsFx.replication(sl, player, source);
        boolean went = Echo.weaker(player, REPLICATION_STRENGTH, () -> Casts.recast(player, cast));
        if (!went) {
            player.sendSystemMessage(Component.literal(cast.name() + " would not go off from here."));
            return AbilityWheel.Result.REFUSED;
        }
        player.sendSystemMessage(Component.literal("Replicated " + cast.name() + " of " + cast.casterName() + "."));
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Space Fragmentation (Key of Stars): nine planets circle where you aim (FRAGMENTATION_RANGE) out to
    // FRAGMENTATION_RADIUS for FRAGMENTATION_TICKS (combat/Zone). Enemies inside are pulled to the middle, take
    // FRAGMENTATION_DPS each second, are slowed (Slowness II) and their arms grow heavy (Mining Fatigue II: slower
    // attacks); shots inside are bent away. At the end the planets collapse: FRAGMENTATION_COLLAPSE to every enemy
    // inside. Door: ANNIHILATION_RADIUS, ANNIHILATION_DPS and ANNIHILATION_COLLAPSE.
    // =========================
    private static AbilityWheel.Result spaceFragmentation(ServerPlayer player, ServerLevel sl) {
        boolean annihilation = Beyonder.hasSequence(player, DOOR_SEQUENCE);
        double radius = annihilation ? ANNIHILATION_RADIUS : FRAGMENTATION_RADIUS;
        float dps = annihilation ? ANNIHILATION_DPS : FRAGMENTATION_DPS;
        float collapse = annihilation ? ANNIHILATION_COLLAPSE : FRAGMENTATION_COLLAPSE;
        Vec3 center = Spells.aimGround(player, sl, FRAGMENTATION_RANGE);
        Vec3 middle = center.add(0, 1.5, 0);
        String tag = "ks_fragment_" + player.getId() + "_" + sl.getGameTime();
        Zone.at(center, radius)
                .height(8.0)
                .lasts(FRAGMENTATION_TICKS)
                .every(5)
                .onInside((enemy, ticksInside) -> {
                    Spells.pull(enemy, middle, FRAGMENTATION_PULL, 0.02);
                    if (ticksInside % 20 != 0) return;
                    Spells.keepEffect(enemy, MobEffects.SLOWNESS, 1, 30);
                    Spells.keepEffect(enemy, MobEffects.MINING_FATIGUE, 1, 30);
                    Spells.strike(player, enemy, dps);
                })
                .onPulse(ticksLeft -> {
                    KeyOfStarsFx.fragmentationTick(sl, center, radius);
                    for (Projectile shot : Spells.hostileShotsAround(player, sl, middle, radius)) {
                        Vec3 away = shot.position().subtract(middle);
                        if (away.lengthSqr() < 1.0E-4) continue;
                        shot.setDeltaMovement(shot.getDeltaMovement().add(away.normalize().scale(FRAGMENTATION_BEND)));
                        shot.hurtMarked = true;
                    }
                })
                .onEnd(() -> {
                    KeyOfStarsFx.fragmentationCollapse(sl, center, radius, tag);
                    if (!Spells.casterStillHere(player, sl)) return;
                    for (LivingEntity enemy : Spells.enemiesAround(player, sl, middle, radius)) Spells.strike(player, enemy, collapse);
                })
                .open(player, sl, FRAGMENTATION_ZONE);
        KeyOfStarsFx.spaceFragmentation(sl, player, center, radius, FRAGMENTATION_TICKS, tag);
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Starlight Seal (Key of Stars): aimed at an enemy within SEAL_RANGE that is caught (you have it Starlit, or it is
    // held: rooted or Sealed already), starlight falls on it and it is Sealed at KeyOfStarsFx.SEALED_AT for SEAL_TICKS
    // (a boss SEAL_BOSS; Door of Doors and an Alternate World longer): no abilities, no teleports, and nothing that
    // cleanses or frees opens it (status/Seal). Aimed at an ally instead: their connections are sealed for SEAL_ALLY,
    // so no enemy ability can pick them and nothing can Starlit them.
    // =========================
    private static AbilityWheel.Result starlightSeal(ServerPlayer player, ServerLevel sl) {
        LivingEntity enemy = Spells.aimEnemy(player, sl, SEAL_RANGE);
        if (enemy != null) {
            if (!Starlit.has(enemy, player) && !Root.isRooted(enemy) && !Seal.isSealed(enemy)) {
                player.sendSystemMessage(Component.literal("It is not caught: Starlit it or hold it first."));
                return AbilityWheel.Result.REFUSED;
            }
            int ticks = KeyOfStars.rootTicks(player, Spells.isBoss(enemy) ? SEAL_BOSS : SEAL_TICKS);
            if (inEnemyWorld(enemy)) ticks = Math.round(ticks * WORLD_HOLDS_LONGER);
            int sealed = ticks;
            String tag = "ks_seal_" + enemy.getId();
            KeyOfStarsFx.starlightSeal(sl, player, enemy, sealed, tag);
            Later.run(sl, KeyOfStarsFx.SEALED_AT, () -> {
                if (enemy.isAlive()) Seal.apply(enemy, sealed);
            });
            Later.run(sl, KeyOfStarsFx.SEALED_AT + sealed, () -> {
                if (!Seal.isSealed(enemy)) KeyOfStarsFx.sealEnd(sl, enemy, tag);
            });
            return AbilityWheel.Result.DONE;
        }
        LivingEntity ally = Spells.aimAlly(player, sl, SEAL_RANGE);
        if (ally == null) {
            player.sendSystemMessage(Component.literal("Nobody stands where you look."));
            return AbilityWheel.Result.REFUSED;
        }
        Seal.guard(ally, SEAL_ALLY);
        KeyOfStarsFx.sealAlly(sl, player, ally, SEAL_ALLY, "ks_seal_ally_" + ally.getId());
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Space-Time Labyrinth (Key of Stars): a maze fills LABYRINTH_RADIUS round where you aim (LABYRINTH_RANGE) for
    // LABYRINTH_TICKS, standing at KeyOfStarsFx.LABYRINTH_RISEN (combat/Enclosure). Enemies inside are slowed
    // (Slowness III), cannot teleport, and walking out of the edge brings them back in at the opposite side. You and
    // your allies walk freely.
    // =========================
    private static AbilityWheel.Result labyrinth(ServerPlayer player, ServerLevel sl) {
        Vec3 center = Spells.aimGround(player, sl, LABYRINTH_RANGE);
        String tag = "ks_labyrinth_" + player.getId() + "_" + sl.getGameTime();
        Enclosure.close(player, LABYRINTH);
        KeyOfStarsFx.labyrinth(sl, player, center, LABYRINTH_RADIUS, LABYRINTH_TICKS, tag);
        int risen = KeyOfStarsFx.LABYRINTH_RISEN;
        Later.run(sl, risen, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            Enclosure.at(center, LABYRINTH_RADIUS)
                    .height(8.0)
                    .lasts(LABYRINTH_TICKS - risen)
                    .every(20)
                    .onInside(enemy -> {
                        Spells.keepEffect(enemy, MobEffects.SLOWNESS, 2, 30);
                        Root.apply(enemy, 30, RootRestriction.TELEPORT);
                        KeyOfStarsFx.lost(sl, enemy, 30);
                    })
                    .onLoop((enemy, from, to) -> KeyOfStarsFx.labyrinthLoop(sl, from, to))
                    .onEnd(() -> {
                        KeyOfStarsFx.labyrinthEnd(sl, center, LABYRINTH_RADIUS, tag);
                        KeyOfStarsFx.statusEnd(player, Status.LABYRINTH);
                    })
                    .open(player, sl, LABYRINTH);
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Alternate World (Door): a kingdom of Doors covers WORLD_RADIUS round you for WORLD_TICKS, in force from
    // KeyOfStarsFx.WORLD_UNFOLDED (combat/Enclosure). No Exits: enemies inside cannot teleport and walking out brings
    // them back in. Open Doors: you and every Key Of Stars on your side inside use Door Opening, Escape Trick, Blink and
    // Traveler's Door for free (KeyOfStars.freeTravel). Sealed: roots and Seals put on enemies inside last
    // WORLD_HOLDS_LONGER times as long. Cast again: it ends, free.
    // =========================
    private static AbilityWheel.Result alternateWorld(ServerPlayer player, ServerLevel sl) {
        if (Enclosure.isOpen(player, WORLD)) {
            Enclosure.close(player, WORLD);
            return AbilityWheel.Result.FREE;
        }
        Vec3 center = player.position();
        String tag = "ks_world_" + player.getId() + "_" + sl.getGameTime();
        KeyOfStarsFx.alternateWorld(sl, player, center, WORLD_RADIUS, WORLD_TICKS, tag);
        int unfolded = KeyOfStarsFx.WORLD_UNFOLDED;
        Later.run(sl, unfolded, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            Enclosure.at(center, WORLD_RADIUS)
                    .height(24.0)
                    .lasts(WORLD_TICKS - unfolded)
                    .every(20)
                    .onInside(enemy -> Root.apply(enemy, 30, RootRestriction.TELEPORT))
                    .onLoop((enemy, from, to) -> KeyOfStarsFx.labyrinthLoop(sl, from, to))
                    .onPulse(ticksLeft -> KeyOfStarsFx.alternateWorldTick(sl, center, WORLD_RADIUS))
                    .onEnd(() -> {
                        KeyOfStarsFx.alternateWorldEnd(sl, center, WORLD_RADIUS, tag);
                        KeyOfStarsFx.statusEnd(player, Status.ALTERNATE_WORLD);
                    })
                    .open(player, sl, WORLD);
        });
        return AbilityWheel.Result.DONE;
    }

    /** Open Doors: this Key Of Stars stands in an Alternate World of their own or of someone on their side. */
    static boolean inFriendlyWorld(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return false;
        for (Player owner : Enclosure.ownersAround(sl, player.position(), WORLD)) {
            if (Spells.isFriend(owner, player)) return true;
        }
        return false;
    }

    /** Sealed: this creature stands in the Alternate World of someone it is an enemy of. */
    static boolean inEnemyWorld(LivingEntity entity) {
        if (!(entity.level() instanceof ServerLevel sl)) return false;
        for (Player owner : Enclosure.ownersAround(sl, entity.position(), WORLD)) {
            if (!Spells.isFriend(owner, entity)) return true;
        }
        return false;
    }

    /** How much longer a root on this creature lasts (Root.addDurationScale, from KeyOfStars). */
    static float worldHoldScale(LivingEntity target) {
        return inEnemyWorld(target) ? WORLD_HOLDS_LONGER : 1f;
    }

    // =========================
    // Omnipresence (Door): for OMNI_TICKS you are everywhere at once. AFTERIMAGES afterimages stand round you and every
    // ability you cast is repeated from each one still standing, its hits at OMNI_STRENGTH, free (combat/Echo; your
    // travels are not repeated). Anything that would hit you hits one of you at random: an afterimage that is hit
    // fades and you take nothing. Blink is free while it lasts.
    // =========================
    private static AbilityWheel.Result omnipresence(ServerPlayer player, ServerLevel sl) {
        if (isOmnipresent(player)) {
            player.sendSystemMessage(Component.literal("You are everywhere already."));
            return AbilityWheel.Result.REFUSED;
        }
        player.getPersistentData().putLong(OMNI_UNTIL, sl.getGameTime() + OMNI_TICKS);
        player.getPersistentData().putInt(OMNI_LEFT, (1 << AFTERIMAGES) - 1);
        KeyOfStarsFx.omnipresence(sl, player, OMNI_TICKS, omniTag(player));
        return AbilityWheel.Result.DONE;
    }

    static boolean isOmnipresent(Player player) {
        return player.getPersistentData().getLongOr(OMNI_UNTIL, 0L) > player.level().getGameTime();
    }

    /** Every cast of an Omnipresent Beyonder: each afterimage still standing casts it too. */
    static void echo(ServerPlayer player, ServerLevel sl, Casts.Cast cast) {
        if (!isOmnipresent(player)) return;
        int left = player.getPersistentData().getIntOr(OMNI_LEFT, 0);
        List<Vec3> spots = new ArrayList<>();
        List<Integer> index = new ArrayList<>();
        for (int i = 0; i < AFTERIMAGES; i++) {
            if ((left & (1 << i)) == 0) continue;
            spots.add(KeyOfStarsFx.afterimageAt(player, i));
            index.add(i);
        }
        Echo.repeat(player, cast, spots, OMNI_STRENGTH, i -> KeyOfStarsFx.omnipresenceEcho(sl, player, index.get(i)));
    }

    /**
     * A hit on an Omnipresent Beyonder: one of the seven of them is struck at random. True if it was an afterimage
     * (the hit is cancelled; that afterimage fades).
     */
    static boolean afterimageTakes(Player player, ServerLevel sl) {
        if (!isOmnipresent(player)) return false;
        int left = player.getPersistentData().getIntOr(OMNI_LEFT, 0);
        int standing = Integer.bitCount(left);
        if (standing == 0) return false;
        int pick = player.getRandom().nextInt(standing + 1);                     // 0..standing-1: an afterimage; standing: you
        if (pick == standing) return false;
        for (int i = 0; i < AFTERIMAGES; i++) {
            if ((left & (1 << i)) == 0) continue;
            if (pick-- > 0) continue;
            player.getPersistentData().putInt(OMNI_LEFT, left & ~(1 << i));
            KeyOfStarsFx.afterimageStruck(sl, player, omniTag(player), i);
            return true;
        }
        return false;
    }

    private static void endOmnipresence(Player player, ServerLevel sl) {
        player.getPersistentData().remove(OMNI_UNTIL);
        player.getPersistentData().remove(OMNI_LEFT);
        KeyOfStarsFx.omnipresenceEnd(sl, player, omniTag(player));
    }

    private static String omniTag(Player player) {
        return "ks_omni_" + player.getId();
    }

    // =========================
    // Close the Door (Door): every Door closes. A great door slams behind you and at KeyOfStarsFx.DOORS_SHUT, for
    // CLOSE_TICKS within CLOSE_RADIUS (combat/Zone): every enemy loses its teleports, Blink and every ability that
    // travels (a Teleport root, kept on while it is inside), and the hidden are forced out (status/Concealment) and
    // kept out. At the shutting, every Seal, root and Starlit mark on enemies inside starts over CLOSE_LONGER as long.
    // =========================
    private static AbilityWheel.Result closeTheDoor(ServerPlayer player, ServerLevel sl) {
        Vec3 center = player.position();
        String tag = "ks_close_" + player.getId() + "_" + sl.getGameTime();
        KeyOfStarsFx.closeTheDoor(sl, player, center, CLOSE_RADIUS, CLOSE_TICKS, tag);
        Later.run(sl, KeyOfStarsFx.DOORS_SHUT, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            forceOut(player, sl, center);
            for (LivingEntity enemy : Spells.enemiesAround(player, sl, center, CLOSE_RADIUS)) doubleHolds(player, enemy);
            Zone.at(center, CLOSE_RADIUS)
                    .height(24.0)
                    .lasts(CLOSE_TICKS)
                    .every(10)
                    .onEnter(enemy -> KeyOfStarsFx.doorsClosed(sl, enemy, Zone.ticksLeft(player, CLOSE_ZONE), tag))
                    .onInside((enemy, ticksInside) -> Root.apply(enemy, 15, RootRestriction.TELEPORT))
                    .onPulse(ticksLeft -> forceOut(player, sl, center))
                    .open(player, sl, CLOSE_ZONE);
        });
        return AbilityWheel.Result.DONE;
    }

    /** The hidden enemies within CLOSE_RADIUS come out of hiding. */
    private static void forceOut(Player player, ServerLevel sl, Vec3 center) {
        AABB area = new AABB(center, center).inflate(CLOSE_RADIUS);
        for (LivingEntity hidden : sl.getEntitiesOfClass(LivingEntity.class, area,
                one -> one.isAlive() && Concealment.isHidden(one) && !Spells.isFriend(player, one)
                        && one.position().distanceTo(center) <= CLOSE_RADIUS)) {
            Concealment.reveal(hidden);
            KeyOfStarsFx.forcedOut(sl, hidden);
        }
    }

    /** Every Seal, root and Starlit mark on this enemy starts over CLOSE_LONGER as long as it had left. */
    private static void doubleHolds(Player player, LivingEntity enemy) {
        if (Seal.isSealed(enemy)) Seal.apply(enemy, Math.round(Seal.ticksLeft(enemy) * CLOSE_LONGER));
        for (RootRestriction restriction : RootRestriction.values()) {
            int left = Root.ticksLeft(enemy, restriction);
            if (left > 0) Root.apply(enemy, Math.round(left * CLOSE_LONGER), restriction);
        }
        int lit = Starlit.ticksLeft(enemy, player);
        if (lit > 0) KeyOfStars.starlit(player, enemy, Math.round(lit * CLOSE_LONGER));
    }

    /** The entries a copy from elsewhere (an echo) never repeats: they shape the world round you, or copy. */
    static boolean isWorldShaping(String entryKey) {
        return entryKey.equals("alternate_world") || entryKey.equals("omnipresence") || entryKey.equals("close_the_door")
                || entryKey.equals("replication");
    }
}
