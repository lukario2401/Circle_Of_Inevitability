package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobs;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.Echo;
import net.lukario.circleofinevitability.combat.Enclosure;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.SpellFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Teleport;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Exile;
import net.lukario.circleofinevitability.status.Forgotten;
import net.lukario.circleofinevitability.status.Seal;
import net.lukario.circleofinevitability.status.Substitute;
import net.lukario.circleofinevitability.status.Toy;
import net.lukario.circleofinevitability.status.Unbound;
import net.lukario.circleofinevitability.status.Unreadable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStars.*;

/**
 * Key Of Stars abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 * The costs and effects are the player abilities' own (KeyOfStars and its wheel classes); only the cooldowns and when
 * a mob uses them are a mob's. A mob has no wheels: every wheel entry is an ability of its own. To port another
 * ability: one more MobAbilities.register(...) in register(), and its cast method below.
 *
 * A mob's "Starlit" mark is kept here (marked/mark): it makes its target glow, and lets its Exile and Starlight Seal
 * take it, as a player's Starlit does.
 *
 *   1  Door Opening (9), Slip Free (9, sneak)
 *   2  Tricks (8): Flash, Black Screen, Loud Noise, Frost Ray, Gale, Tumble, Escape Trick
 *   3  Star Divination (7): its target is marked; Interference (7, sneak): it cannot be picked out from afar
 *   4  Record (6): the first power cast near it that a mob can cast is written down; Replay (6, sneak) casts it
 *   5  Travel (5): Traveler's Door (to its target from afar), Blink (behind its target, dodging the next hit),
 *      Invisible Hand (its target yanked to its feet)
 *   6  Secrets and Space (4): Space Concealment, Transfiguration, Exile, Space Tearing (3)
 *   7  Starlight (2): Toy Box, Replication, Space Fragmentation (1), Starlight Seal (1), Space-Time Labyrinth (1),
 *      Alternate World (0), Omnipresence (0: its afterimages take the hits; they repeat nothing), Close the Door (0)
 *
 * Left out (no use to a mob): Star Anchor and Wandering (Travel), Pocket Space (an Ender Chest).
 */
public final class KeyOfStarsMobs {

    // ---------- mob cooldowns (ticks)
    private static final int DOOR_MOB_COOLDOWN = 100;
    private static final double DOOR_MOB_REACH = 16.0;         // its target no further than this...
    private static final double DOOR_MOB_FAR = 6.0;            // ...and further than this, or out of sight
    private static final int SLIP_MOB_COOLDOWN = 400;
    private static final int TRICK_MOB_COOLDOWN = 120;         // at least this, or the trick's own if longer
    private static final int DIVINATION_MOB_COOLDOWN = 400;
    private static final int RECORD_MOB_COOLDOWN = 600;
    private static final int TRAVEL_MOB_COOLDOWN = 200;
    private static final int SECRETS_MOB_COOLDOWN = 500;
    private static final int STARLIGHT_MOB_COOLDOWN = 900;

    // ---------- names
    private static final String TUMBLE_ZONE = "key_of_stars_mob_tumble";
    private static final String CONCEAL_DOME = "key_of_stars_mob_concealment";
    private static final String TOY_ZONE = "key_of_stars_mob_toy_box";
    private static final String FRAGMENTATION_ZONE = "key_of_stars_mob_fragmentation";
    private static final String LABYRINTH_ENCLOSURE = "key_of_stars_mob_labyrinth";
    private static final String WORLD_ENCLOSURE = "key_of_stars_mob_alternate_world";
    private static final String CLOSE_ZONE = "key_of_stars_mob_close_the_door";
    private static final String RECORDED = "circle_of_inevitability_mob_recorded";     // on the mob: the id written down
    private static final String LAST_TRICK = "circle_of_inevitability_mob_last_trick"; // on the mob: game time

    /** A mob's mark on a creature (its "Starlit"): whose, and until when. Not saved. */
    private record Mark(UUID mob, long until) {}
    private static final Map<UUID, Mark> MARKS = new HashMap<>();

    /** The mobs watching with Record, and until when. Not saved. */
    private static final Map<UUID, Long> WATCHING = new HashMap<>();

    private KeyOfStarsMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        Casts.addAnyCasterListener(KeyOfStarsMobs::recordSeen);              // Record: a watching mob sees the cast

        // ---- key 1: Door Opening
        MobAbilities.register(MobAbility.of(PATHWAY, 1, MobAbility.NORMAL)
                .cost(DOOR_COST).cooldown(DOOR_MOB_COOLDOWN)
                .range(DOOR_MOB_REACH)
                .when((mob, target, sl) -> mob.distanceTo(target) > DOOR_MOB_FAR || !mob.hasLineOfSight(target))
                .cast(KeyOfStarsMobs::doorOpening));
        MobAbilities.register(MobAbility.of(PATHWAY, 1, MobAbility.SNEAK).named("Slip Free")
                .cost(SLIP_COST).cooldown(SLIP_MOB_COOLDOWN)
                .anyRange().weight(30)                                                // when it is needed, it comes first
                .when((mob, target, sl) -> needsFreeing(mob))
                .cast(KeyOfStarsMobs::slipFree));

        // ---- key 2: the Tricks wheel
        trick("flash", "Flash", KeyOfStarsTricks.FLASH_COST, KeyOfStarsTricks.FLASH_COOLDOWN, KeyOfStarsTricks.FLASH_RANGE, true,
                (mob, target, sl) -> !target.hasEffect(MobEffects.BLINDNESS), KeyOfStarsMobs::flash);
        trick("black_screen", "Black Screen", KeyOfStarsTricks.SCREEN_COST, KeyOfStarsTricks.SCREEN_COOLDOWN, 16.0, false,
                (mob, target, sl) -> !(target instanceof Player) && MobSpells.hurt(mob, 0.5f), KeyOfStarsMobs::blackScreen);
        trick("loud_noise", "Loud Noise", KeyOfStarsTricks.NOISE_COST, KeyOfStarsTricks.NOISE_COOLDOWN, KeyOfStarsTricks.NOISE_RANGE, true,
                null, KeyOfStarsMobs::loudNoise);
        trick("frost_ray", "Frost Ray", KeyOfStarsTricks.FROST_COST, KeyOfStarsTricks.FROST_COOLDOWN, KeyOfStarsTricks.FROST_RANGE, true,
                null, KeyOfStarsMobs::frostRay);
        trick("gale", "Gale", KeyOfStarsTricks.GALE_COST, KeyOfStarsTricks.GALE_COOLDOWN, 6.0, true,
                null, KeyOfStarsMobs::gale);
        trick("tumble", "Tumble", KeyOfStarsTricks.TUMBLE_COST, KeyOfStarsTricks.TUMBLE_COOLDOWN, KeyOfStarsTricks.TUMBLE_RANGE, true,
                (mob, target, sl) -> !Zone.isOpen(mob, TUMBLE_ZONE), KeyOfStarsMobs::tumble);
        trick("escape_trick", "Escape Trick", KeyOfStarsTricks.ESCAPE_COST, KeyOfStarsTricks.ESCAPE_COOLDOWN, 5.0, false,
                (mob, target, sl) -> MobSpells.hurt(mob, 0.4f) && Teleport.canGo(mob), KeyOfStarsMobs::escapeTrick);

        // ---- key 3: Astrology
        MobAbilities.register(MobAbility.of(PATHWAY, 3, MobAbility.NORMAL)          // Star Divination
                .cost(DIVINATION_COST).cooldown(DIVINATION_MOB_COOLDOWN).weight(6)
                .range(DIVINATION_RANGE).sight()
                .when((mob, target, sl) -> !marked(mob, target))
                .cast(KeyOfStarsMobs::starDivination));
        MobAbilities.register(MobAbility.of(PATHWAY, 3, MobAbility.SNEAK).named("Interference")
                .cost(INTERFERENCE_COST).cooldown(INTERFERENCE_COOLDOWN).weight(6)
                .anyRange()
                .when((mob, target, sl) -> target instanceof Player && mob.distanceTo(target) > INTERFERENCE_RANGE
                        && !Unreadable.isUnreadable(mob))
                .cast(KeyOfStarsMobs::interference));

        // ---- key 4: Record
        MobAbilities.register(MobAbility.of(PATHWAY, 4, MobAbility.NORMAL)
                .cost(KeyOfStarsRecord.RECORD_COST).cooldown(RECORD_MOB_COOLDOWN).weight(6)
                .range(KeyOfStarsRecord.RECORD_RANGE)
                .when((mob, target, sl) -> recorded(mob) == null && !isWatching(mob, sl))
                .cast(KeyOfStarsMobs::record));
        MobAbilities.register(MobAbility.of(PATHWAY, 4, MobAbility.SNEAK).named("Replay")
                .cost(0).cooldown(KeyOfStarsRecord.REPLAY_COOLDOWN * 5).weight(20)
                .range(24.0)
                .when((mob, target, sl) -> recorded(mob) != null)
                .cast(KeyOfStarsMobs::replay));

        // ---- key 5: the Travel wheel
        MobAbilities.register(MobAbility.of(PATHWAY, 5, MobAbility.NORMAL).entry("travelers_door").named("Traveler's Door")
                .sequence(KeyOfStarsTravel.TRAVEL_SEQUENCE)
                .cost(KeyOfStarsTravel.DOOR_COST).cooldown(Math.max(TRAVEL_MOB_COOLDOWN, KeyOfStarsTravel.DOOR_COOLDOWN))
                .range(KeyOfStarsTravel.DOOR_RANGE)
                .when((mob, target, sl) -> mob.distanceTo(target) > DOOR_MOB_REACH && Teleport.canGo(mob))
                .cast(KeyOfStarsMobs::travelersDoor));
        MobAbilities.register(MobAbility.of(PATHWAY, 5, MobAbility.NORMAL).entry("blink").named("Blink")
                .sequence(KeyOfStarsTravel.TRAVEL_SEQUENCE)
                .cost(KeyOfStarsTravel.BLINK_COST).cooldown(TRAVEL_MOB_COOLDOWN)
                .range(KeyOfStarsTravel.BLINK_RANGE + 4.0)
                .when((mob, target, sl) -> Teleport.canGo(mob))
                .cast(KeyOfStarsMobs::blink));
        MobAbilities.register(MobAbility.of(PATHWAY, 5, MobAbility.NORMAL).entry("invisible_hand").named("Invisible Hand")
                .sequence(KeyOfStarsTravel.TRAVEL_SEQUENCE)
                .cost(KeyOfStarsTravel.HAND_COST).cooldown(TRAVEL_MOB_COOLDOWN)
                .range(KeyOfStarsTravel.HAND_RANGE).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > 5.0 && !Spells.isBoss(target) && !Spells.cannotBeMoved(target))
                .cast(KeyOfStarsMobs::invisibleHand));

        // ---- key 6: the Secrets and Space wheel
        MobAbilities.register(MobAbility.of(PATHWAY, 6, MobAbility.NORMAL).entry("space_concealment").named("Space Concealment")
                .sequence(KeyOfStarsSecrets.SECRETS_SEQUENCE)
                .cost(KeyOfStarsSecrets.CONCEAL_COST).cooldown(Math.max(SECRETS_MOB_COOLDOWN, KeyOfStarsSecrets.CONCEAL_COOLDOWN))
                .range(KeyOfStarsSecrets.CONCEAL_RADIUS)
                .when((mob, target, sl) -> !Dome.isRaised(mob, CONCEAL_DOME))
                .cast(KeyOfStarsMobs::spaceConcealment));
        MobAbilities.register(MobAbility.of(PATHWAY, 6, MobAbility.NORMAL).entry("transfiguration").named("Transfiguration")
                .sequence(KeyOfStarsSecrets.SECRETS_SEQUENCE)
                .cost(KeyOfStarsSecrets.TRANSFIG_COST).cooldown(KeyOfStarsSecrets.TRANSFIG_COOLDOWN)
                .anyRange()
                .when((mob, target, sl) -> MobSpells.hurt(mob, 0.7f) && !Substitute.isArmed(mob))
                .cast(KeyOfStarsMobs::transfiguration));
        MobAbilities.register(MobAbility.of(PATHWAY, 6, MobAbility.NORMAL).entry("exile").named("Exile")
                .sequence(KeyOfStarsSecrets.SECRETS_SEQUENCE)
                .cost(KeyOfStarsSecrets.EXILE_COST).cooldown(KeyOfStarsSecrets.EXILE_COOLDOWN)
                .range(KeyOfStarsSecrets.EXILE_RANGE).sight()
                .when((mob, target, sl) -> caught(mob, target))
                .cast(KeyOfStarsMobs::exile));
        MobAbilities.register(MobAbility.of(PATHWAY, 6, MobAbility.NORMAL).entry("space_tearing").named("Space Tearing")
                .sequence(KeyOfStarsSecrets.TEARING_SEQUENCE)
                .cost(KeyOfStarsSecrets.TEARING_COST).cooldown(KeyOfStarsSecrets.TEARING_COOLDOWN)
                .range(KeyOfStarsSecrets.TEARING_WIDE_RANGE).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) <= tearingRange(mob))
                .cast(KeyOfStarsMobs::spaceTearing));

        // ---- key 7: the Starlight wheel
        MobAbilities.register(MobAbility.of(PATHWAY, 7, MobAbility.NORMAL).entry("toy_box").named("Toy Box")
                .sequence(KeyOfStarsStarlight.STARLIGHT_SEQUENCE)
                .cost(KeyOfStarsStarlight.TOY_COST).cooldown(Math.max(STARLIGHT_MOB_COOLDOWN, KeyOfStarsStarlight.TOY_COOLDOWN))
                .range(KeyOfStarsStarlight.TOY_RANGE).sight()
                .when((mob, target, sl) -> !Zone.isOpen(mob, TOY_ZONE))
                .cast(KeyOfStarsMobs::toyBox));
        MobAbilities.register(MobAbility.of(PATHWAY, 7, MobAbility.NORMAL).entry("replication").named("Replication")
                .sequence(KeyOfStarsStarlight.STARLIGHT_SEQUENCE)
                .cost(KeyOfStarsStarlight.REPLICATION_COST).cooldown(Math.max(STARLIGHT_MOB_COOLDOWN / 2, KeyOfStarsStarlight.REPLICATION_COOLDOWN))
                .range(KeyOfStarsStarlight.REPLICATION_RANGE)
                .when((mob, target, sl) -> toReplicate(mob, sl) != null)
                .cast(KeyOfStarsMobs::replication));
        MobAbilities.register(MobAbility.of(PATHWAY, 7, MobAbility.NORMAL).entry("space_fragmentation").named("Space Fragmentation")
                .sequence(KeyOfStarsStarlight.KEY_OF_STARS_SEQUENCE)
                .cost(KeyOfStarsStarlight.FRAGMENTATION_COST).cooldown(KeyOfStarsStarlight.FRAGMENTATION_COOLDOWN)
                .range(KeyOfStarsStarlight.FRAGMENTATION_RANGE).sight()
                .when((mob, target, sl) -> !Zone.isOpen(mob, FRAGMENTATION_ZONE))
                .cast(KeyOfStarsMobs::spaceFragmentation));
        MobAbilities.register(MobAbility.of(PATHWAY, 7, MobAbility.NORMAL).entry("starlight_seal").named("Starlight Seal")
                .sequence(KeyOfStarsStarlight.KEY_OF_STARS_SEQUENCE)
                .cost(KeyOfStarsStarlight.SEAL_COST).cooldown(KeyOfStarsStarlight.SEAL_COOLDOWN)
                .range(KeyOfStarsStarlight.SEAL_RANGE).sight()
                .when((mob, target, sl) -> caught(mob, target) && !Seal.isSealed(target))
                .cast(KeyOfStarsMobs::starlightSeal));
        MobAbilities.register(MobAbility.of(PATHWAY, 7, MobAbility.NORMAL).entry("labyrinth").named("Space-Time Labyrinth")
                .sequence(KeyOfStarsStarlight.KEY_OF_STARS_SEQUENCE)
                .cost(KeyOfStarsStarlight.LABYRINTH_COST).cooldown(KeyOfStarsStarlight.LABYRINTH_COOLDOWN)
                .range(KeyOfStarsStarlight.LABYRINTH_RANGE).sight()
                .when((mob, target, sl) -> !Enclosure.isOpen(mob, LABYRINTH_ENCLOSURE))
                .cast(KeyOfStarsMobs::labyrinth));
        MobAbilities.register(MobAbility.of(PATHWAY, 7, MobAbility.NORMAL).entry("alternate_world").named("Alternate World")
                .sequence(KeyOfStarsStarlight.DOOR_SEQUENCE)
                .cost(KeyOfStarsStarlight.WORLD_COST).cooldown(KeyOfStarsStarlight.WORLD_COOLDOWN)
                .range(KeyOfStarsStarlight.WORLD_RADIUS)
                .when((mob, target, sl) -> !Enclosure.isOpen(mob, WORLD_ENCLOSURE))
                .cast(KeyOfStarsMobs::alternateWorld));
        MobAbilities.register(MobAbility.of(PATHWAY, 7, MobAbility.NORMAL).entry("omnipresence").named("Omnipresence")
                .sequence(KeyOfStarsStarlight.DOOR_SEQUENCE)
                .cost(KeyOfStarsStarlight.OMNI_COST).cooldown(KeyOfStarsStarlight.OMNI_COOLDOWN)
                .anyRange()
                .when((mob, target, sl) -> MobSpells.hurt(mob, 0.6f) && !Substitute.isArmed(mob))
                .cast(KeyOfStarsMobs::omnipresence));
        MobAbilities.register(MobAbility.of(PATHWAY, 7, MobAbility.NORMAL).entry("close_the_door").named("Close the Door")
                .sequence(KeyOfStarsStarlight.DOOR_SEQUENCE)
                .cost(KeyOfStarsStarlight.CLOSE_COST).cooldown(KeyOfStarsStarlight.CLOSE_COOLDOWN)
                .range(KeyOfStarsStarlight.CLOSE_RADIUS)
                .when((mob, target, sl) -> !Zone.isOpen(mob, CLOSE_ZONE))
                .cast(KeyOfStarsMobs::closeTheDoor));
    }

    /** One trick of the Tricks wheel (Sequence 8): its cost, its cooldown (never under TRICK_MOB_COOLDOWN), its reach. */
    private static void trick(String entry, String name, int cost, int cooldown, double range, boolean sight,
                              MobAbility.When when, MobAbility.Cast cast) {
        MobAbility ability = MobAbility.of(PATHWAY, 2, MobAbility.NORMAL).entry(entry).named(name)
                .sequence(KeyOfStarsTricks.TRICKS_SEQUENCE)
                .cost(cost).cooldown(Math.max(TRICK_MOB_COOLDOWN, cooldown))
                .range(range)
                .cast((mob, target, sl) -> {
                    boolean went = cast.run(mob, target, sl);
                    if (went && !entry.equals("escape_trick")) mob.getPersistentData().putLong(LAST_TRICK, sl.getGameTime());
                    return went;
                });
        if (sight) ability.sight();
        if (when != null) ability.when(when);
        MobAbilities.register(ability);
    }

    // =========================
    // The mark (a mob's Starlit)
    // =========================

    /** 'mob' marks 'target' for 'ticks': it glows, and the mob's Exile and Starlight Seal can take it. */
    private static void mark(Mob mob, ServerLevel sl, LivingEntity target, int ticks) {
        MARKS.put(target.getUUID(), new Mark(mob.getUUID(), sl.getGameTime() + ticks));
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, ticks, 0));
        String tag = "ks_mob_starlit_" + mob.getId() + "_" + target.getId();
        KeyOfStarsFx.starlitEnd(sl, target, tag);
        KeyOfStarsFx.starlit(sl, mob, target, ticks, tag);
    }

    private static boolean marked(Mob mob, LivingEntity target) {
        Mark mark = MARKS.get(target.getUUID());
        if (mark == null) return false;
        if (mark.until() <= target.level().getGameTime()) {
            MARKS.remove(target.getUUID());
            return false;
        }
        return mark.mob().equals(mob.getUUID());
    }

    /** Marked by it, or held (rooted, Sealed): what its Exile and Starlight Seal can take. */
    private static boolean caught(Mob mob, LivingEntity target) {
        return marked(mob, target) || Root.isRooted(target) || Seal.isSealed(target);
    }

    // =========================
    // Key 1: Door Opening, Slip Free
    // =========================

    private static boolean doorOpening(Mob mob, LivingEntity target, ServerLevel sl) {
        Vec3 exit = Teleport.standNear(mob, sl, target.position(), 2.0);
        if (exit == null || exit.distanceTo(mob.position()) < 2.0) return false;       // no room by it: nothing spent
        MobSpells.face(mob, target);
        Vec3 door = mob.position().add(Spells.flatLook(mob).scale(0.8));
        KeyOfStarsFx.doorOpening(sl, mob, door, exit);
        Later.run(sl, KeyOfStarsFx.DOOR_OPENED, () -> {
            if (!MobSpells.casterStillHere(mob, sl)) return;
            Teleport.of(mob).ignoringTeleportRoots().fallGuard(SAFE_LANDING_TICKS).go(sl, exit);
            if (target.isAlive() && target.level() == sl) MobSpells.face(mob, target);
        });
        return true;
    }

    private static boolean slipFree(Mob mob, LivingEntity target, ServerLevel sl) {
        Unbound.free(mob, SLIP_GUARD, MobSpells.hasSequence(mob, UNBAR_SEQUENCE));
        KeyOfStarsFx.slipFree(sl, mob, mob);
        return true;
    }

    /** Rooted, burning, or under a harmful effect. */
    private static boolean needsFreeing(Mob mob) {
        if (Root.isRooted(mob) || mob.isOnFire()) return true;
        for (MobEffectInstance effect : mob.getActiveEffects()) {
            if (!effect.getEffect().value().isBeneficial()) return true;
        }
        return false;
    }

    // =========================
    // Key 2: the Tricks wheel
    // =========================

    private static boolean flash(Mob mob, LivingEntity target, ServerLevel sl) {
        MobSpells.face(mob, target);
        KeyOfStarsFx.flash(sl, mob, KeyOfStarsTricks.FLASH_RANGE);
        for (LivingEntity enemy : Spells.enemiesInCone(mob, sl, mob.getEyePosition(), MobSpells.toward(mob, target),
                KeyOfStarsTricks.FLASH_RANGE, KeyOfStarsTricks.FLASH_ANGLE)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, KeyOfStarsTricks.FLASH_BLIND, 0));
            KeyOfStarsFx.blinded(sl, enemy, KeyOfStarsTricks.FLASH_BLIND);
            if (enemy instanceof Mob other) Forgotten.apply(other, mob, KeyOfStarsTricks.FLASH_FORGET);
        }
        return true;
    }

    private static boolean blackScreen(Mob mob, LivingEntity target, ServerLevel sl) {
        MobSpells.face(mob, target);
        float yaw = mob.getYRot();
        Vec3 normal = Spells.flatLook(mob);
        Vec3 center = Spells.groundAt(mob, sl, mob.position().add(normal.scale(KeyOfStarsTricks.SCREEN_AHEAD)).add(0, 1.0, 0));
        String tag = "ks_mob_screen_" + mob.getId() + "_" + sl.getGameTime();
        KeyOfStarsFx.blackScreen(sl, center, yaw, KeyOfStarsTricks.SCREEN_TICKS, tag);
        for (int t = 0; t < KeyOfStarsTricks.SCREEN_TICKS; t += KeyOfStarsTricks.SCREEN_CHECK) {
            Later.run(sl, t, () -> {                                             // those hunting it lose it behind the curtain
                if (!MobSpells.casterStillHere(mob, sl)) return;
                for (Mob hunter : Forgotten.huntersOf(sl, mob, KeyOfStarsTricks.SCREEN_HUNTERS)) {
                    Forgotten.apply(hunter, mob, KeyOfStarsTricks.SCREEN_CHECK * 2);
                }
            });
        }
        Later.run(sl, KeyOfStarsTricks.SCREEN_TICKS, () -> KeyOfStarsFx.blackScreenEnd(sl, center, tag));
        return true;
    }

    private static boolean loudNoise(Mob mob, LivingEntity target, ServerLevel sl) {
        Vec3 at = MobSpells.groundUnder(mob, sl, target);
        MobSpells.face(mob, target);
        KeyOfStarsFx.loudNoise(sl, mob, at);
        for (LivingEntity enemy : MobSpells.enemiesAround(mob, sl, at, KeyOfStarsTricks.NOISE_RADIUS)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, KeyOfStarsTricks.NOISE_SLOW, 0));
            KeyOfStarsFx.deafened(sl, enemy, KeyOfStarsTricks.NOISE_SLOW);
            if (enemy instanceof Mob other) Forgotten.apply(other, mob, KeyOfStarsTricks.NOISE_FORGET);
        }
        return true;
    }

    private static boolean frostRay(Mob mob, LivingEntity target, ServerLevel sl) {
        MobSpells.face(mob, target);
        Vec3 hand = SpellFx.handOf(mob, sl);
        if (hand == null) hand = mob.getEyePosition();
        Vec3 way = MobSpells.chestOf(target).subtract(hand).normalize();
        Vec3 stop = MobSpells.lineEnd(mob, sl, hand, way, KeyOfStarsTricks.FROST_RANGE);
        LivingEntity hit = Spells.firstEnemyOnLine(mob, sl, hand, stop, 0.4);
        Vec3 reach = hit == null ? stop : hit.getBoundingBox().getCenter();
        KeyOfStarsFx.frostRay(sl, mob, hand, reach);
        if (hit == null) return true;
        int flight = Math.max(1, (int) Math.ceil(hand.distanceTo(reach) / KeyOfStarsTricks.FROST_SPEED));
        Later.run(sl, flight, () -> {
            if (!hit.isAlive() || hit.level() != sl) return;
            Spells.strike(mob, hit, KeyOfStarsTricks.FROST_DAMAGE);
            hit.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, KeyOfStarsTricks.FROST_SLOW, 2));
            hit.setTicksFrozen(Math.max(hit.getTicksFrozen(), KeyOfStarsTricks.FROST_FROZEN));
            hit.clearFire();
            KeyOfStarsFx.frosted(sl, hit, KeyOfStarsTricks.FROST_SLOW, "ks_frost_" + hit.getId());
        });
        return true;
    }

    private static boolean gale(Mob mob, LivingEntity target, ServerLevel sl) {
        MobSpells.face(mob, target);
        Vec3 chest = mob.getEyePosition().add(0, -0.4, 0);
        Vec3 look = MobSpells.toward(mob, target);
        KeyOfStarsFx.gale(sl, mob, KeyOfStarsTricks.GALE_RANGE);
        for (LivingEntity enemy : Spells.enemiesInCone(mob, sl, chest, look, KeyOfStarsTricks.GALE_RANGE, KeyOfStarsTricks.GALE_ANGLE)) {
            Spells.push(enemy, mob.position(), KeyOfStarsTricks.GALE_PUSH, KeyOfStarsTricks.GALE_LIFT);
            enemy.clearFire();
            KeyOfStarsFx.galeHit(sl, enemy);
        }
        Vec3 aside = new Vec3(-look.z, 0, look.x);
        double narrowest = Math.cos(Math.toRadians(KeyOfStarsTricks.GALE_ANGLE));
        for (Projectile shot : Spells.hostileShotsAround(mob, sl, chest, KeyOfStarsTricks.GALE_RANGE)) {
            Vec3 toIt = shot.position().subtract(chest);
            if (toIt.lengthSqr() > 1.0 && toIt.normalize().dot(look) < narrowest) continue;
            double side = toIt.dot(aside) >= 0 ? 1.0 : -1.0;
            shot.setDeltaMovement(shot.getDeltaMovement().scale(0.3).add(aside.scale(side * KeyOfStarsTricks.GALE_SHOTS)).add(look.scale(0.6)));
            shot.hurtMarked = true;
        }
        return true;
    }

    private static boolean tumble(Mob mob, LivingEntity target, ServerLevel sl) {
        Vec3 center = MobSpells.groundUnder(mob, sl, target);
        double radius = KeyOfStarsTricks.TUMBLE_RADIUS;
        String tag = "ks_mob_tumble_" + mob.getId() + "_" + sl.getGameTime();
        MobSpells.face(mob, target);
        Zone.at(center, radius)
                .height(3.0)
                .lasts(KeyOfStarsTricks.TUMBLE_TICKS)
                .every(10)
                .onInside((enemy, ticksInside) -> {
                    if (enemy instanceof Player other && KeyOfStars.isKeyOfStars(other, KeyOfStarsTricks.TRICKS_SEQUENCE)) return;
                    Spells.keepEffect(enemy, MobEffects.SLOWNESS, 1, 20);
                    double a = enemy.getRandom().nextDouble() * Math.PI * 2;
                    Spells.push(enemy, enemy.position().add(Math.cos(a), 0, Math.sin(a)), KeyOfStarsTricks.TUMBLE_SHOVE, 0.05);
                    if (ticksInside > 0 && ticksInside % KeyOfStarsTricks.TUMBLE_STAGGER_EVERY == 0) {
                        Root.apply(enemy, KeyOfStarsTricks.TUMBLE_STAGGER, RootRestriction.MOVE, RootRestriction.ATTACK);
                        KeyOfStarsFx.slipped(sl, enemy, KeyOfStarsTricks.TUMBLE_STAGGER);
                    }
                })
                .onPulse(ticksLeft -> KeyOfStarsFx.tumbleTick(sl, center, radius))
                .onEnd(() -> KeyOfStarsFx.tumbleEnd(sl, center, tag))
                .open(mob, sl, TUMBLE_ZONE);
        KeyOfStarsFx.tumble(sl, mob, center, radius, KeyOfStarsTricks.TUMBLE_TICKS, tag);
        return true;
    }

    private static boolean escapeTrick(Mob mob, LivingEntity target, ServerLevel sl) {
        long last = mob.getPersistentData().getLongOr(LAST_TRICK, -1000L);
        boolean diversion = sl.getGameTime() - last <= KeyOfStarsTricks.DIVERSION_TICKS;
        double distance = !diversion ? KeyOfStarsTricks.ESCAPE_HOP
                : MobSpells.hasSequence(mob, KeyOfStarsTricks.ESCAPE_FARTHER_SEQUENCE) ? KeyOfStarsTricks.ESCAPE_FARTHER
                : KeyOfStarsTricks.ESCAPE_FAR;
        Vec3 from = mob.position();
        Vec3 away = from.subtract(target.position());                          // away from what it fights
        Vec3 to = Spells.freeSpotNear(mob, sl, from, new Vec3(away.x, 0, away.z), distance);
        if (to == null) return false;
        KeyOfStarsFx.escapeTrick(sl, mob, from, to, diversion);
        if (!MobSpells.stepTo(mob, sl, to)) return false;
        mob.getPersistentData().remove(LAST_TRICK);
        return true;
    }

    // =========================
    // Key 3: Star Divination, Interference
    // =========================

    private static boolean starDivination(Mob mob, LivingEntity target, ServerLevel sl) {
        MobSpells.face(mob, target);
        KeyOfStarsFx.starDivination(sl, mob, target);
        Later.run(sl, KeyOfStarsFx.DIVINED, () -> {
            if (!MobSpells.casterStillHere(mob, sl) || !target.isAlive() || target.level() != sl) return;
            mark(mob, sl, target, STARLIT_TICKS);
        });
        return true;
    }

    private static boolean interference(Mob mob, LivingEntity target, ServerLevel sl) {
        Unreadable.apply(mob, INTERFERENCE_TICKS, INTERFERENCE_RANGE);
        String tag = "ks_mob_interference_" + mob.getId();
        KeyOfStarsFx.interferenceEnd(sl, mob, tag);
        KeyOfStarsFx.interference(sl, mob, INTERFERENCE_TICKS, tag);
        Later.run(sl, INTERFERENCE_TICKS, () -> KeyOfStarsFx.interferenceEnd(sl, mob, tag));
        return true;
    }

    // =========================
    // Key 4: Record, Replay
    // =========================

    private static boolean record(Mob mob, LivingEntity target, ServerLevel sl) {
        int ticks = KeyOfStarsRecord.RECORD_TICKS;
        WATCHING.put(mob.getUUID(), sl.getGameTime() + ticks);
        String tag = "ks_mob_record_" + mob.getId();
        KeyOfStarsFx.record(sl, mob, ticks, tag);
        Later.run(sl, ticks, () -> {
            WATCHING.remove(mob.getUUID());
            KeyOfStarsFx.recordEnd(sl, mob, tag);
        });
        return true;
    }

    private static boolean isWatching(Mob mob, ServerLevel sl) {
        Long until = WATCHING.get(mob.getUUID());
        return until != null && until > sl.getGameTime();
    }

    /** Every cast anyone made: a mob watching near it, in sight, writes it down if it can cast it (one page). */
    private static void recordSeen(LivingEntity caster, ServerLevel sl, Casts.Cast cast) {
        if (WATCHING.isEmpty() || !cast.copyable()) return;
        MobAbility copy = MobAbilities.forCast(cast);
        if (copy == null) return;
        double range = KeyOfStarsRecord.RECORD_RANGE;
        AABB area = caster.getBoundingBox().inflate(range);
        for (Mob watcher : sl.getEntitiesOfClass(Mob.class, area, one -> WATCHING.containsKey(one.getUUID()))) {
            if (watcher == caster || !isWatching(watcher, sl) || watcher.distanceTo(caster) > range || !watcher.hasLineOfSight(caster)) continue;
            WATCHING.remove(watcher.getUUID());
            watcher.getPersistentData().putString(RECORDED, copy.id());
            KeyOfStarsFx.recordEnd(sl, watcher, "ks_mob_record_" + watcher.getId());
        }
    }

    /** The mob ability it has written down, or null. */
    private static MobAbility recorded(Mob mob) {
        String id = mob.getPersistentData().getStringOr(RECORDED, "");
        return id.isEmpty() ? null : MobAbilities.byId(id);
    }

    private static boolean replay(Mob mob, LivingEntity target, ServerLevel sl) {
        MobAbility page = recorded(mob);
        mob.getPersistentData().remove(RECORDED);                                // the page is used up either way
        if (page == null) return false;
        MobSpells.face(mob, target);
        KeyOfStarsFx.replay(sl, mob);
        float strength = Math.max(KeyOfStarsRecord.STRENGTH_MIN, Math.min(1f, KeyOfStarsRecord.STRENGTH
                + KeyOfStarsRecord.STRENGTH_PER_GAP * (page.sequence() - BeyonderMobs.sequence(mob))));
        return Echo.weaker(mob, strength, () -> page.cast().run(mob, target, sl));
    }

    // =========================
    // Key 5: the Travel wheel
    // =========================

    private static boolean travelersDoor(Mob mob, LivingEntity target, ServerLevel sl) {
        Vec3 there = Teleport.standNear(mob, sl, target.position(), 3.0);
        if (there == null) return false;
        Vec3 from = mob.position();
        int wait = MobSpells.hasSequence(mob, KeyOfStarsTravel.DOOR_SIGHT_SEQUENCE) ? 0
                : (int) Math.round(from.distanceTo(there) * KeyOfStarsTravel.DOOR_WAIT_PER_BLOCK);
        MobSpells.face(mob, target);
        KeyOfStarsFx.travelersDoor(sl, mob, from);
        Later.run(sl, KeyOfStarsFx.TRAVEL_ENTERED, () -> {
            if (!MobSpells.casterStillHere(mob, sl)) return;
            Concealment.hide(mob, wait + 4);
            Later.run(sl, wait, () -> {
                if (!MobSpells.casterStillHere(mob, sl)) return;
                Concealment.reveal(mob);
                if (MobSpells.stepTo(mob, sl, there)) KeyOfStarsFx.travelersArrive(sl, there, mob.getYRot());
            });
        });
        return true;
    }

    private static boolean blink(Mob mob, LivingEntity target, ServerLevel sl) {
        Vec3 behind = Spells.flatLook(target).scale(-1);                       // round to its back
        Vec3 to = Spells.freeSpotNear(mob, sl, target.position(), behind, target.getBbWidth() * 0.5 + 1.5);
        if (to == null || to.distanceTo(mob.position()) > KeyOfStarsTravel.BLINK_RANGE * 1.5) return false;
        Vec3 from = mob.position();
        KeyOfStarsFx.blink(sl, mob, from, to, 0);
        if (!MobSpells.stepTo(mob, sl, to)) return false;
        MobSpells.face(mob, target);
        Substitute.arm(mob, KeyOfStarsTravel.BLINK_DODGE, 1, (saved, source, amount, level) -> KeyOfStarsFx.blinkDodge(level, mob));
        if (MobSpells.hasSequence(mob, KeyOfStarsTravel.QUICK_SEQUENCE)) Unbound.breakControl(mob);
        return true;
    }

    private static boolean invisibleHand(Mob mob, LivingEntity target, ServerLevel sl) {
        MobSpells.face(mob, target);
        KeyOfStarsFx.invisibleHand(sl, mob, target);
        Later.run(sl, KeyOfStarsFx.HAND_GRABS, () -> {
            if (!MobSpells.casterStillHere(mob, sl) || !target.isAlive() || target.level() != sl) return;
            mark(mob, sl, target, STARLIT_TICKS);
            if (Spells.isBoss(target) || Spells.cannotBeMoved(target)) return;
            Vec3 feet = Teleport.standNear(target, sl, mob.position().add(Spells.flatLook(mob).scale(1.3)), 2.0);
            if (feet == null || !Teleport.of(target).ignoringTeleportRoots().go(sl, feet)) return;
            Root.apply(target, KeyOfStarsTravel.HAND_STAGGER, RootRestriction.MOVE, RootRestriction.ATTACK);
            KeyOfStarsFx.yanked(sl, target);
        });
        return true;
    }

    // =========================
    // Key 6: the Secrets and Space wheel
    // =========================

    private static boolean spaceConcealment(Mob mob, LivingEntity target, ServerLevel sl) {
        boolean hostel = MobSpells.hasSequence(mob, KeyOfStarsSecrets.HOSTEL_SEQUENCE);
        double radius = hostel ? KeyOfStarsSecrets.HOSTEL_RADIUS : KeyOfStarsSecrets.CONCEAL_RADIUS;
        int ticks = hostel ? KeyOfStarsSecrets.HOSTEL_TICKS : KeyOfStarsSecrets.CONCEAL_TICKS;
        Vec3 center = mob.position();
        float doorYaw = mob.getYRot() - 180f;
        String tag = "ks_mob_conceal_" + mob.getId() + "_" + sl.getGameTime();
        KeyOfStarsFx.spaceConcealment(sl, mob, center, radius, ticks, doorYaw, true, tag);
        Later.run(sl, 2, () -> {                                                   // a Spatial Cage: closed at once
            if (!MobSpells.casterStillHere(mob, sl)) return;
            for (LivingEntity enemy : MobSpells.enemiesAround(mob, sl, center, radius)) mark(mob, sl, enemy, STARLIT_TICKS);
            Dome.at(center, radius)
                    .lasts(ticks - 2)
                    .keepsEnemiesIn()
                    .stopsProjectiles()
                    .onEnd(() -> KeyOfStarsFx.concealmentEnd(sl, center, radius, tag))
                    .raise(mob, sl, CONCEAL_DOME);
        });
        for (int t = 10; t < ticks; t += 10) {                                     // it and its friends inside: hidden, healed
            boolean heal = t % 20 == 0;
            Later.run(sl, t, () -> {
                if (!MobSpells.casterStillHere(mob, sl) || !Dome.isRaised(mob, CONCEAL_DOME)) return;
                for (LivingEntity friend : MobSpells.friendsAround(mob, sl, center, radius)) {
                    if (net.lukario.circleofinevitability.combat.Support.blocked(mob, friend)) continue;
                    Concealment.hide(friend, 15);
                    if (heal && friend.getHealth() < friend.getMaxHealth()) {
                        friend.heal(KeyOfStarsSecrets.CONCEAL_HEAL);
                        KeyOfStarsFx.concealedHeal(sl, friend);
                    }
                }
            });
        }
        return true;
    }

    private static boolean transfiguration(Mob mob, LivingEntity target, ServerLevel sl) {
        int layers = MobSpells.hasSequence(mob, KeyOfStarsSecrets.MORE_LAYERS_SEQUENCE) ? KeyOfStarsSecrets.MORE_LAYERS : KeyOfStarsSecrets.LAYERS;
        int ticks = KeyOfStarsSecrets.TRANSFIG_TICKS;
        String tag = "ks_mob_transfig_" + mob.getId();
        KeyOfStarsFx.transfiguration(sl, mob, layers, ticks, tag);
        Substitute.arm(mob, ticks, layers, (saved, source, amount, level) -> {
            Vec3 from = mob.position();
            Vec3 side = Spells.rightOf(mob).scale(mob.getRandom().nextBoolean() ? 1 : -1);
            Vec3 to = Teleport.canGo(mob) ? Spells.freeSpotNear(mob, level, from, side, KeyOfStarsSecrets.LAYER_STEP) : null;
            if (to != null) MobSpells.stepTo(mob, level, to);
            int left = Substitute.charges(mob);
            KeyOfStarsFx.layerTorn(level, mob, from, to != null ? to : from, left);
            if (left <= 0) KeyOfStarsFx.transfigurationEnd(level, mob, tag);
        });
        Later.run(sl, ticks, () -> KeyOfStarsFx.transfigurationEnd(sl, mob, tag));
        return true;
    }

    private static boolean exile(Mob mob, LivingEntity target, ServerLevel sl) {
        int ticks = MobSpells.hasSequence(mob, KeyOfStarsSecrets.EXILE_LONGEST_SEQUENCE) ? KeyOfStarsSecrets.EXILE_LONGEST
                : MobSpells.hasSequence(mob, KeyOfStarsSecrets.EXILE_LONGER_SEQUENCE) ? KeyOfStarsSecrets.EXILE_LONGER
                : KeyOfStarsSecrets.EXILE_TICKS;
        Set<LivingEntity> exiled = new LinkedHashSet<>();
        exiled.add(target);
        if (MobSpells.hasSequence(mob, KeyOfStarsSecrets.EXILE_LONGEST_SEQUENCE)) {
            for (LivingEntity other : MobSpells.enemiesAround(mob, sl, mob.position(), KeyOfStarsSecrets.EXILE_RANGE)) {
                if (exiled.size() >= KeyOfStarsSecrets.EXILE_MANY) break;
                if (marked(mob, other)) exiled.add(other);
            }
        }
        MobSpells.face(mob, target);
        for (LivingEntity one : exiled) {
            MARKS.remove(one.getUUID());                                         // the mark is used up
            int time = Spells.isBoss(one) ? KeyOfStarsSecrets.EXILE_BOSS : ticks;
            KeyOfStarsFx.exile(sl, mob, one);
            Later.run(sl, KeyOfStarsFx.EXILE_SWALLOWED, () -> {
                if (!one.isAlive() || one.level() != sl) return;
                Vec3 at = one.position();
                String tag = "ks_mob_exile_" + one.getId();
                if (!Exile.banish(one, time, at, (who, level) -> {
                    KeyOfStarsFx.exileReturn(level, at, tag);
                    if (mob.isAlive() && mob.level() == level) Spells.strike(mob, who, KeyOfStarsSecrets.EXILE_DAMAGE);
                    else who.hurt(who.damageSources().magic(), KeyOfStarsSecrets.EXILE_DAMAGE);
                })) return;
                KeyOfStarsFx.exiled(sl, one, at, time, tag);
            });
        }
        return true;
    }

    private static double tearingRange(Mob mob) {
        return MobSpells.hasSequence(mob, KeyOfStarsSecrets.TEARING_WIDE_SEQUENCE) ? KeyOfStarsSecrets.TEARING_WIDE_RANGE
                : KeyOfStarsSecrets.TEARING_RANGE;
    }

    private static boolean spaceTearing(Mob mob, LivingEntity target, ServerLevel sl) {
        boolean wide = MobSpells.hasSequence(mob, KeyOfStarsSecrets.TEARING_WIDE_SEQUENCE);
        double radius = wide ? KeyOfStarsSecrets.TEARING_WIDE_RADIUS : KeyOfStarsSecrets.TEARING_RADIUS;
        MobSpells.face(mob, target);
        KeyOfStarsFx.spaceTearing(sl, mob, target, radius);
        if (wide) KeyOfStarsFx.spaceTearing(sl, mob, mob, radius);
        Later.run(sl, KeyOfStarsFx.TEAR_LANDS, () -> {
            if (!MobSpells.casterStillHere(mob, sl)) return;
            Set<LivingEntity> torn = new LinkedHashSet<>();
            if (target.isAlive() && target.level() == sl) {
                if (Spells.isEnemy(mob, target)) torn.add(target);
                torn.addAll(MobSpells.enemiesAround(mob, sl, target.position(), radius));
            }
            if (wide) torn.addAll(MobSpells.enemiesAround(mob, sl, mob.position(), radius));
            for (LivingEntity one : torn) {
                Spells.strike(mob, one, KeyOfStarsSecrets.TEARING_DAMAGE);
                storm(mob, sl, one);
            }
        });
        return true;
    }

    /** The space-time storm: STORM_DAMAGE and Slowness II each second for STORM_SECONDS. */
    private static void storm(Mob mob, ServerLevel sl, LivingEntity target) {
        if (!target.isAlive()) return;
        int seconds = KeyOfStarsSecrets.STORM_SECONDS;
        KeyOfStarsFx.storm(sl, target, seconds * 20, "ks_mob_storm_" + target.getId());
        for (int s = 1; s <= seconds; s++) {
            Later.run(sl, s * 20, () -> {
                if (!target.isAlive() || target.level() != sl) return;
                KeyOfStarsFx.stormTick(sl, target);
                Spells.keepEffect(target, MobEffects.SLOWNESS, 1, 25);
                if (MobSpells.casterStillHere(mob, sl)) Spells.strike(mob, target, KeyOfStarsSecrets.STORM_DAMAGE);
            });
        }
    }

    // =========================
    // Key 7: the Starlight wheel
    // =========================

    private static boolean toyBox(Mob mob, LivingEntity target, ServerLevel sl) {
        Vec3 center = MobSpells.groundUnder(mob, sl, target);
        double radius = KeyOfStarsStarlight.TOY_RADIUS;
        int ticks = KeyOfStarsStarlight.TOY_TICKS;
        String tag = "ks_mob_toy_" + mob.getId() + "_" + sl.getGameTime();
        List<LivingEntity> toys = new ArrayList<>();
        MobSpells.face(mob, target);
        Zone.at(center, radius)
                .height(4.0)
                .lasts(ticks)
                .every(10)
                .onEnter(enemy -> {
                    if (Toy.isToy(enemy)) return;
                    if (enemy instanceof Player || enemy.getMaxHealth() > KeyOfStarsStarlight.TOY_MAX_HEALTH) {
                        enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, KeyOfStarsStarlight.TOY_SLOWED, 2));
                        KeyOfStarsFx.toySlowed(sl, enemy, KeyOfStarsStarlight.TOY_SLOWED);
                        return;
                    }
                    int left = Math.max(20, Zone.ticksLeft(mob, TOY_ZONE));
                    KeyOfStarsFx.toyed(sl, enemy, left, tag);
                    Later.run(sl, KeyOfStarsFx.TOY_SHRUNK, () -> {
                        if (enemy.isAlive() && Zone.isOpen(mob, TOY_ZONE) && Toy.make(enemy, Math.max(20, Zone.ticksLeft(mob, TOY_ZONE)))) toys.add(enemy);
                    });
                })
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
                    KeyOfStarsFx.toyBoxEnd(sl, center, tag);
                })
                .open(mob, sl, TOY_ZONE);
        KeyOfStarsFx.toyBox(sl, mob, center, radius, ticks, tag);
        return true;
    }

    /** The newest power another cast near it in the last REPLICATION_AGE ticks that a mob can cast, or null. */
    private static Casts.Cast toReplicate(Mob mob, ServerLevel sl) {
        Casts.Cast cast = Casts.latestNear(sl, mob.position(), KeyOfStarsStarlight.REPLICATION_RANGE,
                KeyOfStarsStarlight.REPLICATION_AGE, one -> !one.caster().equals(mob.getUUID()) && one.copyable());
        return cast != null && MobAbilities.forCast(cast) != null ? cast : null;
    }

    private static boolean replication(Mob mob, LivingEntity target, ServerLevel sl) {
        Casts.Cast cast = toReplicate(mob, sl);
        MobAbility copy = cast == null ? null : MobAbilities.forCast(cast);
        if (copy == null || (copy.range() > 0 && (target == null || target.level() != sl))) return false;
        if (sl.getEntity(cast.caster()) instanceof LivingEntity source) KeyOfStarsFx.replication(sl, mob, source);
        MobSpells.face(mob, target);
        return Echo.weaker(mob, KeyOfStarsStarlight.REPLICATION_STRENGTH, () -> copy.cast().run(mob, target, sl));
    }

    private static boolean spaceFragmentation(Mob mob, LivingEntity target, ServerLevel sl) {
        boolean annihilation = MobSpells.hasSequence(mob, KeyOfStarsStarlight.DOOR_SEQUENCE);
        double radius = annihilation ? KeyOfStarsStarlight.ANNIHILATION_RADIUS : KeyOfStarsStarlight.FRAGMENTATION_RADIUS;
        float dps = annihilation ? KeyOfStarsStarlight.ANNIHILATION_DPS : KeyOfStarsStarlight.FRAGMENTATION_DPS;
        float collapse = annihilation ? KeyOfStarsStarlight.ANNIHILATION_COLLAPSE : KeyOfStarsStarlight.FRAGMENTATION_COLLAPSE;
        Vec3 center = MobSpells.groundUnder(mob, sl, target);
        Vec3 middle = center.add(0, 1.5, 0);
        int ticks = KeyOfStarsStarlight.FRAGMENTATION_TICKS;
        String tag = "ks_mob_fragment_" + mob.getId() + "_" + sl.getGameTime();
        MobSpells.face(mob, target);
        Zone.at(center, radius)
                .height(8.0)
                .lasts(ticks)
                .every(5)
                .onInside((enemy, ticksInside) -> {
                    Spells.pull(enemy, middle, KeyOfStarsStarlight.FRAGMENTATION_PULL, 0.02);
                    if (ticksInside % 20 != 0) return;
                    Spells.keepEffect(enemy, MobEffects.SLOWNESS, 1, 30);
                    Spells.keepEffect(enemy, MobEffects.MINING_FATIGUE, 1, 30);
                    Spells.strike(mob, enemy, dps);
                })
                .onPulse(ticksLeft -> {
                    KeyOfStarsFx.fragmentationTick(sl, center, radius);
                    for (Projectile shot : Spells.hostileShotsAround(mob, sl, middle, radius)) {
                        Vec3 away = shot.position().subtract(middle);
                        if (away.lengthSqr() < 1.0E-4) continue;
                        shot.setDeltaMovement(shot.getDeltaMovement().add(away.normalize().scale(KeyOfStarsStarlight.FRAGMENTATION_BEND)));
                        shot.hurtMarked = true;
                    }
                })
                .onEnd(() -> {
                    KeyOfStarsFx.fragmentationCollapse(sl, center, radius, tag);
                    if (!MobSpells.casterStillHere(mob, sl)) return;
                    for (LivingEntity enemy : MobSpells.enemiesAround(mob, sl, middle, radius)) Spells.strike(mob, enemy, collapse);
                })
                .open(mob, sl, FRAGMENTATION_ZONE);
        KeyOfStarsFx.spaceFragmentation(sl, mob, center, radius, ticks, tag);
        return true;
    }

    private static boolean starlightSeal(Mob mob, LivingEntity target, ServerLevel sl) {
        int sealed = Spells.isBoss(target) ? KeyOfStarsStarlight.SEAL_BOSS : KeyOfStarsStarlight.SEAL_TICKS;
        String tag = "ks_mob_seal_" + target.getId();
        MobSpells.face(mob, target);
        KeyOfStarsFx.starlightSeal(sl, mob, target, sealed, tag);
        Later.run(sl, KeyOfStarsFx.SEALED_AT, () -> {
            if (target.isAlive()) Seal.apply(target, sealed);
        });
        Later.run(sl, KeyOfStarsFx.SEALED_AT + sealed, () -> {
            if (!Seal.isSealed(target)) KeyOfStarsFx.sealEnd(sl, target, tag);
        });
        return true;
    }

    private static boolean labyrinth(Mob mob, LivingEntity target, ServerLevel sl) {
        Vec3 center = MobSpells.groundUnder(mob, sl, target);
        double radius = KeyOfStarsStarlight.LABYRINTH_RADIUS;
        int ticks = KeyOfStarsStarlight.LABYRINTH_TICKS;
        String tag = "ks_mob_labyrinth_" + mob.getId() + "_" + sl.getGameTime();
        MobSpells.face(mob, target);
        KeyOfStarsFx.labyrinth(sl, mob, center, radius, ticks, tag);
        int risen = KeyOfStarsFx.LABYRINTH_RISEN;
        Later.run(sl, risen, () -> {
            if (!MobSpells.casterStillHere(mob, sl)) return;
            Enclosure.at(center, radius)
                    .height(8.0)
                    .lasts(ticks - risen)
                    .every(20)
                    .onInside(enemy -> {
                        Spells.keepEffect(enemy, MobEffects.SLOWNESS, 2, 30);
                        Root.apply(enemy, 30, RootRestriction.TELEPORT);
                        KeyOfStarsFx.lost(sl, enemy, 30);
                    })
                    .onLoop((enemy, from, to) -> KeyOfStarsFx.labyrinthLoop(sl, from, to))
                    .onEnd(() -> KeyOfStarsFx.labyrinthEnd(sl, center, radius, tag))
                    .open(mob, sl, LABYRINTH_ENCLOSURE);
        });
        return true;
    }

    private static boolean alternateWorld(Mob mob, LivingEntity target, ServerLevel sl) {
        Vec3 center = mob.position();
        double radius = KeyOfStarsStarlight.WORLD_RADIUS;
        int ticks = KeyOfStarsStarlight.WORLD_TICKS;
        String tag = "ks_mob_world_" + mob.getId() + "_" + sl.getGameTime();
        KeyOfStarsFx.alternateWorld(sl, mob, center, radius, ticks, tag);
        int unfolded = KeyOfStarsFx.WORLD_UNFOLDED;
        Later.run(sl, unfolded, () -> {
            if (!MobSpells.casterStillHere(mob, sl)) return;
            Enclosure.at(center, radius)
                    .height(24.0)
                    .lasts(ticks - unfolded)
                    .every(20)
                    .onInside(enemy -> Root.apply(enemy, 30, RootRestriction.TELEPORT))
                    .onLoop((enemy, from, to) -> KeyOfStarsFx.labyrinthLoop(sl, from, to))
                    .onPulse(ticksLeft -> KeyOfStarsFx.alternateWorldTick(sl, center, radius))
                    .onEnd(() -> KeyOfStarsFx.alternateWorldEnd(sl, center, radius, tag))
                    .open(mob, sl, WORLD_ENCLOSURE);
        });
        return true;
    }

    /** A mob's Omnipresence: its afterimages stand round it and take the hits meant for it (they repeat nothing). */
    private static boolean omnipresence(Mob mob, LivingEntity target, ServerLevel sl) {
        int ticks = KeyOfStarsStarlight.OMNI_TICKS;
        String tag = "ks_mob_omni_" + mob.getId();
        int[] index = {0};
        KeyOfStarsFx.omnipresence(sl, mob, ticks, tag);
        Substitute.arm(mob, ticks, KeyOfStarsStarlight.AFTERIMAGES, (saved, source, amount, level) ->
                KeyOfStarsFx.afterimageStruck(level, mob, tag, Math.min(KeyOfStarsStarlight.AFTERIMAGES - 1, index[0]++)));
        Later.run(sl, ticks, () -> KeyOfStarsFx.omnipresenceEnd(sl, mob, tag));
        return true;
    }

    private static boolean closeTheDoor(Mob mob, LivingEntity target, ServerLevel sl) {
        Vec3 center = mob.position();
        double radius = KeyOfStarsStarlight.CLOSE_RADIUS;
        int ticks = KeyOfStarsStarlight.CLOSE_TICKS;
        String tag = "ks_mob_close_" + mob.getId() + "_" + sl.getGameTime();
        KeyOfStarsFx.closeTheDoor(sl, mob, center, radius, ticks, tag);
        Later.run(sl, KeyOfStarsFx.DOORS_SHUT, () -> {
            if (!MobSpells.casterStillHere(mob, sl)) return;
            forceOut(mob, sl, center, radius);
            for (LivingEntity enemy : MobSpells.enemiesAround(mob, sl, center, radius)) doubleHolds(mob, sl, enemy);
            Zone.at(center, radius)
                    .height(24.0)
                    .lasts(ticks)
                    .every(10)
                    .onEnter(enemy -> KeyOfStarsFx.doorsClosed(sl, enemy, Zone.ticksLeft(mob, CLOSE_ZONE), tag))
                    .onInside((enemy, ticksInside) -> Root.apply(enemy, 15, RootRestriction.TELEPORT))
                    .onPulse(ticksLeft -> forceOut(mob, sl, center, radius))
                    .open(mob, sl, CLOSE_ZONE);
        });
        return true;
    }

    /** The hidden that are not its friends come out of hiding. */
    private static void forceOut(Mob mob, ServerLevel sl, Vec3 center, double radius) {
        AABB area = new AABB(center, center).inflate(radius);
        for (LivingEntity hidden : sl.getEntitiesOfClass(LivingEntity.class, area,
                one -> one.isAlive() && Concealment.isHidden(one) && !Spells.isFriend(mob, one)
                        && one.position().distanceTo(center) <= radius)) {
            Concealment.reveal(hidden);
            KeyOfStarsFx.forcedOut(sl, hidden);
        }
    }

    /** Every Seal, root and mark on this enemy starts over CLOSE_LONGER as long as it had left. */
    private static void doubleHolds(Mob mob, ServerLevel sl, LivingEntity enemy) {
        float longer = KeyOfStarsStarlight.CLOSE_LONGER;
        if (Seal.isSealed(enemy)) Seal.apply(enemy, Math.round(Seal.ticksLeft(enemy) * longer));
        for (RootRestriction restriction : RootRestriction.values()) {
            int left = Root.ticksLeft(enemy, restriction);
            if (left > 0) Root.apply(enemy, Math.round(left * longer), restriction);
        }
        Mark mark = MARKS.get(enemy.getUUID());
        if (mark != null && mark.mob().equals(mob.getUUID())) {
            long left = mark.until() - sl.getGameTime();
            if (left > 0) mark(mob, sl, enemy, Math.round(left * longer));
        }
    }
}
