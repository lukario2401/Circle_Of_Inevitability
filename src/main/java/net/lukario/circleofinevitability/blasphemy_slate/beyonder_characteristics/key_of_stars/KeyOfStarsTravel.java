package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStarsFx.Form;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStarsFx.Status;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.combat.Echo;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Teleport;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Anchors;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Exile;
import net.lukario.circleofinevitability.status.Substitute;
import net.lukario.circleofinevitability.status.Unbound;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Key Of Stars, key 5: the Travel wheel (Traveler, Sequence 5). docs/KEY_OF_STARS.md, "Key 5 wheel: Travel".
 *
 *   Traveler's Door   a Door to the Spirit World and out of another where you aim (or at the Star Anchor you aim at);
 *                     the further, the longer you wait inside; allies touching you come along
 *   Blink             a hop toward where you look, up to three in a row; the hop dodges the next hit. Planeswalker:
 *                     the afterimages it leaves stay a while and repeat what you cast (combat/Echo)
 *   Invisible Hand    an unseen hand: a creature is yanked to your feet and Starlit, loose things fly to you
 *   Star Anchor       a saved place to come back to by Traveler's Door (status/Anchors); cast again on it to lift it.
 *                     Wanderer: Traveler's Door aimed straight up goes to your newest Anchor in another dimension
 *   Wandering (3)     you are a trail of starlight in the Astral World for a while, through blocks and creatures,
 *                     unseen and unhurt; pressing the key again steps you out. Symbolization (2): longer, and allies
 *                     touching you come along. Conceptualization (0): enemies you fly through are banished for a while
 *
 * Sneak casts the last pick again without the wheel. Every entry's cost goes through KeyOfStars.wheelCostFor and every
 * cooldown through KeyOfStars.cooldownFor. Every teleport goes through combat/Teleport (KeyOfStars.teleport). The looks
 * are KeyOfStarsFx's; every number is right below.
 */
public final class KeyOfStarsTravel {

    private KeyOfStarsTravel() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int TRAVEL_SEQUENCE = 5;                       // the key

    // Traveler's Door
    private static final int DOOR_COST = 3000;
    private static final int DOOR_COOLDOWN = 200;
    private static final double DOOR_RANGE = 48.0;
    private static final int DOOR_FAR_SEQUENCE = 4;             // Secrets Sorcerer: this far
    private static final double DOOR_FAR = 96.0;
    private static final int DOOR_FARTHER_SEQUENCE = 3;         // Wanderer: this far
    private static final double DOOR_FARTHER = 192.0;
    private static final int DOOR_SIGHT_SEQUENCE = 2;           // Planeswalker: no waiting inside, any place you can see
    private static final double DOOR_SIGHT = 256.0;
    private static final double DOOR_WAIT_PER_BLOCK = 0.5;      // ticks inside for every block of the way
    private static final double DOOR_COMPANY = 2.0;             // allies this close come along
    private static final double ANCHOR_AIM = 6.0;               // aimed this close (degrees) at an Anchor: you go there
    private static final int DOOR_WORLDS_SEQUENCE = 3;          // Wanderer: Anchors in other dimensions...
    private static final float DOOR_WORLDS_PITCH = -75f;        // ...by looking at least this far up
    private static final int DOOR_WORLDS_WAIT = 30;             // the wait inside, whatever the way

    // Blink
    private static final int BLINK_COST = 500;
    private static final int BLINK_COOLDOWN = 80;               // starts with the first hop: the 2 s of the chain and 2 s more
    private static final double BLINK_RANGE = 8.0;
    private static final int BLINK_CHAIN = 3;                   // hops in a row...
    private static final int BLINK_WINDOW = 40;                 // ...within this (each hop after the first is paid on its own)
    private static final int BLINK_DODGE = 6;                   // the next hit within this misses
    private static final int QUICK_SEQUENCE = 4;                // Secrets Sorcerer: after a Blink, for QUICK_TICKS, wheel
    private static final int QUICK_TICKS = 60;                  // powers cost QUICK_COST and cool down in QUICK_COOLDOWN of
    static final float QUICK_COST = 0.7f;                       // the time; a Blink also tears you out of mind control
    static final float QUICK_COOLDOWN = 0.5f;
    private static final int ECHO_SEQUENCE = 2;                 // Planeswalker: the afterimage each Blink leaves stays...
    private static final int ECHO_TICKS = 80;                   // ...this long and repeats every power you cast...
    private static final int ECHO_MAX = 3;                      // ...at most this many at once...
    private static final float ECHO_STRENGTH = 0.4f;            // ...its hits at this strength (combat/Echo)

    // Invisible Hand
    private static final int HAND_COST = 1500;
    private static final int HAND_COOLDOWN = 120;
    private static final double HAND_RANGE = 24.0;
    private static final double HAND_LOOSE = 2.0;               // loose things this close to where you aim fly to you
    private static final int HAND_STAGGER = 20;

    // Star Anchor
    private static final int ANCHOR_COST = 1000;
    private static final int ANCHOR_COOLDOWN = 100;
    private static final int ANCHOR_TICKS = 12000;              // 10 minutes
    private static final int ANCHOR_MAX = 3;
    private static final int ANCHOR_MORE_SEQUENCE = 3;          // Wanderer: this many
    private static final int ANCHOR_MORE = 5;
    private static final double ANCHOR_LIFT = 1.5;              // cast standing this close to one: it is lifted

    // Wandering
    static final int WANDERING_SEQUENCE = 3;
    private static final int WANDERING_COST = 12000;
    private static final int WANDERING_COOLDOWN = 800;
    private static final int WANDERING_TICKS = 160;
    private static final double WANDERING_SPEED = 0.56;         // blocks a tick: twice a sprint
    private static final int SYMBOLIZATION_SEQUENCE = 2;        // Planeswalker: Symbolization, this long...
    private static final int SYMBOLIZATION_TICKS = 240;
    private static final double SYMBOLIZATION_COMPANY = 2.0;    // ...and allies this close come along
    private static final int CONCEPTUALIZATION_SEQUENCE = 0;    // Door: Conceptualization; enemies you fly through...
    private static final double CONCEPTUALIZATION_REACH = 1.2;
    private static final int CONCEPTUALIZATION_BANISH = 60;     // ...are out of the world this long (status/Exile)
    private static final int WANDERING_LANDING = 60;            // no fall damage this long after stepping out

    // =========================
    // Names and what is kept
    // =========================
    private static final String BLINK_HOPS = "key_of_stars_blink_hops";
    private static final String BLINK_UNTIL = "key_of_stars_blink_until";
    private static final String QUICK_UNTIL = "key_of_stars_quick_until";
    private static final String WANDER_UNTIL = "key_of_stars_wander_until";
    private static final String WANDER_FORM = "key_of_stars_wander_form";

    /** Who travels with a Wanderer, and where they keep to beside them. Not saved: a relog ends the Wandering. */
    private record Companion(UUID id, Vec3 offset) {}

    private static final Map<UUID, List<Companion>> WANDER_COMPANY = new HashMap<>();

    /** A Planeswalker's Blink afterimage: where it stands and when it fades. Not saved. */
    private record Afterimage(Vec3 at, long until, String tag) {}

    private static final Map<UUID, List<Afterimage>> AFTERIMAGES = new HashMap<>();

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("key_of_stars_travel", "Travel")
            .radius(72)
            .cooldownBy(KeyOfStars::cooldownFor)
            .entry("travelers_door", KeyOfStarsFx.WHEEL_TRAVELERS_DOOR, "Traveler's Door", TRAVEL_SEQUENCE, DOOR_COST,
                    DOOR_COOLDOWN, KeyOfStarsTravel::travelersDoor)
            .costBy(KeyOfStars::wheelCostFor)
            .free(KeyOfStars::freeTravel)                                       // an Alternate World's Open Doors
            .entry("blink", KeyOfStarsFx.WHEEL_BLINK, "Blink", TRAVEL_SEQUENCE, BLINK_COST, BLINK_COOLDOWN,
                    KeyOfStarsTravel::blink)
            .costBy(KeyOfStars::wheelCostFor)
            .free(player -> KeyOfStars.freeTravel(player) || chainOpen(player) || KeyOfStarsStarlight.isOmnipresent(player))
            .entry("invisible_hand", KeyOfStarsFx.WHEEL_INVISIBLE_HAND, "Invisible Hand", TRAVEL_SEQUENCE, HAND_COST,
                    HAND_COOLDOWN, KeyOfStarsTravel::invisibleHand)
            .costBy(KeyOfStars::wheelCostFor)
            .entry("star_anchor", KeyOfStarsFx.WHEEL_STAR_ANCHOR, "Star Anchor", TRAVEL_SEQUENCE, ANCHOR_COST,
                    ANCHOR_COOLDOWN, KeyOfStarsTravel::starAnchor)
            .costBy(KeyOfStars::wheelCostFor)
            .free(KeyOfStarsTravel::onOwnAnchor)                                // lifting one: free, no cooldown
            .entry("wandering", KeyOfStarsFx.WHEEL_WANDERING, "Wandering", WANDERING_SEQUENCE, WANDERING_COST,
                    WANDERING_COOLDOWN, KeyOfStarsTravel::wandering)
            .costBy(KeyOfStars::wheelCostFor)
            .notCopyable();                                                      // it flies on this Pathway's tick and key

    // =========================
    // Ability 5 - Travel
    // Normal: the Travel wheel. Sneak: the last pick again, without the wheel. While Wandering: steps you out.
    // =========================
    public static void keyOfStarsTravel(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (isWandering(player)) {
            endWandering(player, sl);
            return;
        }
        if (!KeyOfStars.ready(player, bypassClassCheck, TRAVEL_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    static void clearState(Player player) {
        player.getPersistentData().remove(BLINK_HOPS);
        player.getPersistentData().remove(BLINK_UNTIL);
        player.getPersistentData().remove(QUICK_UNTIL);
        if (isWandering(player) && player.level() instanceof ServerLevel sl) endWandering(player, sl);
        WANDER_COMPANY.remove(player.getUUID());
        AFTERIMAGES.remove(player.getUUID());
    }

    /** Every tick for a Key Of Stars: the Wandering moves on. */
    static void tick(Player player, ServerLevel sl) {
        if (isWandering(player)) wanderTick(player, sl);
    }

    // =========================
    // Traveler's Door: aim at a spot within DOOR_RANGE (Secrets Sorcerer DOOR_FAR, Wanderer DOOR_FARTHER,
    // Planeswalker DOOR_SIGHT and no waiting) or at one of your Star Anchors in this dimension (any distance). A Door
    // opens before you, you step in at KeyOfStarsFx.TRAVEL_ENTERED and wait inside DOOR_WAIT_PER_BLOCK a block of
    // the way (hidden), then step out of another Door there. Allies within DOOR_COMPANY come along.
    // A teleport: a Teleport root stops it, before it starts or while you wait (you come out where you went in).
    // Wanderer: looking straight up (DOOR_WORLDS_PITCH), it goes to your newest Anchor in another dimension.
    // =========================
    private static AbilityWheel.Result travelersDoor(ServerPlayer player, ServerLevel sl) {
        if (!Teleport.canGo(player)) {
            player.sendSystemMessage(Component.literal("Something holds you where you are."));
            return AbilityWheel.Result.REFUSED;
        }
        if (Beyonder.hasSequence(player, DOOR_WORLDS_SEQUENCE) && player.getXRot() <= DOOR_WORLDS_PITCH) {
            Anchors.Anchor away = newestElsewhere(player);
            if (away != null) return toAnotherWorld(player, sl, away);
        }
        Anchors.Anchor anchor = Anchors.aimed(player, KeyOfStars.ANCHOR, ANCHOR_AIM);
        Vec3 there = anchor != null ? Teleport.standNear(player, sl, anchor.at(), 3.0)
                : Spells.blinkSpot(player, sl, doorRange(player));
        if (there == null) {
            player.sendSystemMessage(Component.literal("There is no room where you look."));
            return AbilityWheel.Result.REFUSED;
        }
        Vec3 from = player.position();
        int wait = Beyonder.hasSequence(player, DOOR_SIGHT_SEQUENCE) ? 0
                : (int) Math.round(from.distanceTo(there) * DOOR_WAIT_PER_BLOCK);
        KeyOfStarsFx.travelersDoor(sl, player, from);
        Later.run(sl, KeyOfStarsFx.TRAVEL_ENTERED, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            List<LivingEntity> company = Spells.alliesAround(player, sl, player.position(), DOOR_COMPANY);
            Concealment.hide(player, wait + 4);
            for (LivingEntity ally : company) Concealment.hide(ally, wait + 4);
            Later.run(sl, wait, () -> {
                if (!Spells.casterStillHere(player, sl)) return;
                Concealment.reveal(player);
                float yaw = player.getYRot();
                if (KeyOfStars.teleport(player, sl, there, false)) KeyOfStarsFx.travelersArrive(sl, there, yaw);
                for (LivingEntity ally : company) {
                    Concealment.reveal(ally);
                    if (!ally.isAlive() || ally.level() != sl) continue;
                    Vec3 spot = Teleport.standNear(ally, sl, there, 2.0);
                    if (spot != null) Teleport.of(ally).fallGuard(KeyOfStars.safeLanding(player)).go(sl, spot);
                }
            });
        });
        return AbilityWheel.Result.DONE;
    }

    /** The newest of this player's Star Anchors that lies in another dimension, or null. */
    private static Anchors.Anchor newestElsewhere(Player player) {
        List<Anchors.Anchor> all = Anchors.list(player, KeyOfStars.ANCHOR);
        for (int i = all.size() - 1; i >= 0; i--) {
            if (!all.get(i).isIn(player.level())) return all.get(i);
        }
        return null;
    }

    /**
     * Wanderer: Traveler's Door to a Star Anchor in another dimension. The same Door, DOOR_WORLDS_WAIT inside whatever
     * the way, and allies touching you come along (combat/Teleport.goTo).
     */
    private static AbilityWheel.Result toAnotherWorld(ServerPlayer player, ServerLevel sl, Anchors.Anchor anchor) {
        ServerLevel there = Teleport.levelNamed(sl.getServer(), anchor.dimension());
        Vec3 spot = there == null ? null : Teleport.standNear(player, there, anchor.at(), 3.0);
        if (spot == null) {
            player.sendSystemMessage(Component.literal("The Door will not open to that Anchor."));
            return AbilityWheel.Result.REFUSED;
        }
        KeyOfStarsFx.travelersDoor(sl, player, player.position());
        Later.run(sl, KeyOfStarsFx.TRAVEL_ENTERED, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            List<LivingEntity> company = Spells.alliesAround(player, sl, player.position(), DOOR_COMPANY);
            Concealment.hide(player, DOOR_WORLDS_WAIT + 4);
            for (LivingEntity ally : company) Concealment.hide(ally, DOOR_WORLDS_WAIT + 4);
            Later.run(sl, DOOR_WORLDS_WAIT, () -> {
                if (!Spells.casterStillHere(player, sl)) return;
                Concealment.reveal(player);
                float yaw = player.getYRot();
                int guard = KeyOfStars.safeLanding(player);
                for (LivingEntity ally : company) {
                    Concealment.reveal(ally);
                    if (!ally.isAlive() || ally.level() != sl) continue;
                    Vec3 near = Teleport.standNear(ally, there, spot, 2.0);
                    if (near != null) Teleport.of(ally).fallGuard(guard).goTo(there, near);
                }
                if (Teleport.of(player).fallGuard(guard).goTo(there, spot)) KeyOfStarsFx.travelersArrive(there, spot, yaw);
            });
        });
        return AbilityWheel.Result.DONE;
    }

    private static double doorRange(Player player) {
        if (Beyonder.hasSequence(player, DOOR_SIGHT_SEQUENCE)) return DOOR_SIGHT;
        if (Beyonder.hasSequence(player, DOOR_FARTHER_SEQUENCE)) return DOOR_FARTHER;
        if (Beyonder.hasSequence(player, DOOR_FAR_SEQUENCE)) return DOOR_FAR;
        return DOOR_RANGE;
    }

    // =========================
    // Blink: a hop of BLINK_RANGE toward where you look, leaving an afterimage; the next hit within BLINK_DODGE
    // misses (status/Substitute). Up to BLINK_CHAIN hops in a row within BLINK_WINDOW: the first starts the cooldown,
    // the others pay their own cost. Secrets Sorcerer: for QUICK_TICKS after, wheel powers are cheaper and cool down
    // faster, and a Blink tears you out of mind control (status/Unbound). Door of Doors: free. Planeswalker: the
    // afterimage stays ECHO_TICKS and repeats every power you cast, its hits at ECHO_STRENGTH (combat/Echo), up to
    // ECHO_MAX of them.
    // =========================
    private static AbilityWheel.Result blink(ServerPlayer player, ServerLevel sl) {
        if (!Teleport.canGo(player)) {
            player.sendSystemMessage(Component.literal("Something holds you where you are."));
            return AbilityWheel.Result.REFUSED;
        }
        Vec3 from = player.position();
        Vec3 to = Spells.blinkSpot(player, sl, BLINK_RANGE);
        if (to == null) to = Spells.freeSpotNear(player, sl, from, player.getLookAngle(), BLINK_RANGE);
        if (to == null) {
            player.sendSystemMessage(Component.literal("There is no room to blink to."));
            return AbilityWheel.Result.REFUSED;
        }
        boolean chained = chainOpen(player);
        boolean doorOfDoors = KeyOfStars.freeTravel(player) || KeyOfStarsStarlight.isOmnipresent(player);
        if (chained && !doorOfDoors && !Spells.paySpirituality(player, KeyOfStars.wheelCostFor(player, BLINK_COST))) {
            return AbilityWheel.Result.REFUSED;
        }
        int hop = chained ? player.getPersistentData().getIntOr(BLINK_HOPS, 1) : 0;
        KeyOfStarsFx.blink(sl, player, from, to, hop);
        KeyOfStars.teleport(player, sl, to, false);
        Substitute.arm(player, BLINK_DODGE, 1, (saved, source, amount, level) -> KeyOfStarsFx.blinkDodge(level, player));
        if (Beyonder.hasSequence(player, QUICK_SEQUENCE)) {
            player.getPersistentData().putLong(QUICK_UNTIL, sl.getGameTime() + QUICK_TICKS);
            KeyOfStarsFx.quickened(player, QUICK_TICKS);
            Unbound.breakControl(player);
        }
        if (doorOfDoors) KeyOfStarsFx.doorOfDoors(sl, player);
        if (Beyonder.hasSequence(player, ECHO_SEQUENCE)) leaveAfterimage(player, sl, from);
        // the chain: the first hop opens it, the last closes it
        if (hop + 1 >= BLINK_CHAIN) {
            player.getPersistentData().remove(BLINK_HOPS);
            player.getPersistentData().remove(BLINK_UNTIL);
        } else {
            player.getPersistentData().putInt(BLINK_HOPS, hop + 1);
            if (!chained) player.getPersistentData().putLong(BLINK_UNTIL, sl.getGameTime() + BLINK_WINDOW);
        }
        return AbilityWheel.Result.DONE;
    }

    /** Planeswalker: the afterimage left at 'at' stays and echoes (the oldest goes past ECHO_MAX). */
    private static void leaveAfterimage(Player player, ServerLevel sl, Vec3 at) {
        List<Afterimage> images = AFTERIMAGES.computeIfAbsent(player.getUUID(), id -> new ArrayList<>());
        long now = sl.getGameTime();
        images.removeIf(image -> image.until() <= now);
        while (images.size() >= ECHO_MAX) {
            Afterimage oldest = images.remove(0);
            ParticleShapes.clearModels(sl, oldest.at(), oldest.tag());
        }
        String tag = "ks_blink_echo_" + player.getId() + "_" + now;
        images.add(new Afterimage(at, now + ECHO_TICKS, tag));
        KeyOfStarsFx.blinkEcho(sl, player, at, ECHO_TICKS, tag);
    }

    /** Every cast of this Key Of Stars: each Blink afterimage still standing casts it too. */
    static void echo(ServerPlayer player, ServerLevel sl, Casts.Cast cast) {
        List<Afterimage> images = AFTERIMAGES.get(player.getUUID());
        if (images == null) return;
        long now = sl.getGameTime();
        images.removeIf(image -> image.until() <= now);
        if (images.isEmpty()) {
            AFTERIMAGES.remove(player.getUUID());
            return;
        }
        List<Vec3> spots = new ArrayList<>();
        for (Afterimage image : images) spots.add(image.at());
        Echo.repeat(player, cast, spots, ECHO_STRENGTH, null);
    }

    /** The entries an echo (combat/Echo) never repeats from elsewhere: the ones that move you or are yours alone. */
    static boolean isTravel(String entryKey) {
        return entryKey.equals("travelers_door") || entryKey.equals("blink") || entryKey.equals("star_anchor")
                || entryKey.equals("wandering");
    }

    /** True while a chain of Blinks is open (a hop or two made within BLINK_WINDOW, fewer than BLINK_CHAIN). */
    private static boolean chainOpen(Player player) {
        int hops = player.getPersistentData().getIntOr(BLINK_HOPS, 0);
        return hops > 0 && hops < BLINK_CHAIN && player.getPersistentData().getLongOr(BLINK_UNTIL, 0L) > player.level().getGameTime();
    }

    /** The Secrets Sorcerer's quick powers after a Blink. */
    static boolean isQuickened(Player player) {
        return player.getPersistentData().getLongOr(QUICK_UNTIL, 0L) > player.level().getGameTime();
    }

    // =========================
    // Invisible Hand: an unseen hand grabs what you look at within HAND_RANGE. An enemy (not a boss, and not one that
    // cannot be moved) is yanked to your feet at KeyOfStarsFx.HAND_GRABS, staggered for HAND_STAGGER and Starlit; a boss
    // is only Starlit. Looking at no one: the loose things (items, arrows, XP and the like) within HAND_LOOSE of where
    // you look fly to you.
    // =========================
    private static AbilityWheel.Result invisibleHand(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, HAND_RANGE);
        if (target != null) {
            KeyOfStarsFx.invisibleHand(sl, player, target);
            Later.run(sl, KeyOfStarsFx.HAND_GRABS, () -> {
                if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
                KeyOfStars.starlit(player, target, KeyOfStars.STARLIT_TICKS);
                if (Spells.isBoss(target) || Spells.cannotBeMoved(target)) return;
                Vec3 feet = Teleport.standNear(target, sl, player.position().add(Spells.flatLook(player).scale(1.3)), 2.0);
                if (feet == null || !Teleport.of(target).ignoringTeleportRoots().go(sl, feet)) return;
                int ticks = KeyOfStars.rootTicks(player, HAND_STAGGER);
                Root.apply(target, ticks, RootRestriction.MOVE, RootRestriction.ATTACK);
                KeyOfStarsFx.yanked(sl, target);
            });
            return AbilityWheel.Result.DONE;
        }
        Vec3 aimed = Spells.aimPoint(player, sl, HAND_RANGE);
        List<Entity> loose = Spells.looseThingsAround(player, sl, aimed, HAND_LOOSE);
        if (loose.isEmpty()) {
            player.sendSystemMessage(Component.literal("The Hand finds nothing to take."));
            return AbilityWheel.Result.REFUSED;
        }
        KeyOfStarsFx.invisibleHandItem(sl, player, aimed);
        Vec3 to = player.position().add(0, 0.5, 0);
        Later.run(sl, KeyOfStarsFx.HAND_GRABS, () -> {
            for (Entity thing : loose) {
                if (!thing.isAlive() || thing.level() != sl) continue;
                thing.teleportTo(to.x, to.y, to.z);
                thing.setDeltaMovement(Vec3.ZERO);
            }
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Star Anchor: plant one where you stand for ANCHOR_TICKS (status/Anchors; at most ANCHOR_MAX, Wanderer
    // ANCHOR_MORE: a new one past that lifts the oldest). Cast standing within ANCHOR_LIFT of one of yours: it is lifted,
    // free. Traveler's Door goes to the one you aim at; Sense of Direction shows the way to each (KeyOfStars).
    // =========================
    private static AbilityWheel.Result starAnchor(ServerPlayer player, ServerLevel sl) {
        Vec3 here = player.position();
        Anchors.Anchor lifted = Anchors.removeNear(player, KeyOfStars.ANCHOR, sl, here, ANCHOR_LIFT);
        if (lifted != null) {
            KeyOfStarsFx.anchorLift(sl, lifted.at(), anchorTag(player, lifted.at()));
            return AbilityWheel.Result.FREE;
        }
        int max = Beyonder.hasSequence(player, ANCHOR_MORE_SEQUENCE) ? ANCHOR_MORE : ANCHOR_MAX;
        Anchors.Anchor dropped = Anchors.add(player, KeyOfStars.ANCHOR, sl, here, ANCHOR_TICKS, max);
        if (dropped != null && dropped.isIn(sl)) KeyOfStarsFx.anchorLift(sl, dropped.at(), anchorTag(player, dropped.at()));
        KeyOfStarsFx.starAnchor(sl, player, here, ANCHOR_TICKS, anchorTag(player, here));
        return AbilityWheel.Result.DONE;
    }

    /** True while the player stands within ANCHOR_LIFT of one of their own Star Anchors here. */
    private static boolean onOwnAnchor(Player player) {
        for (Anchors.Anchor anchor : Anchors.list(player, KeyOfStars.ANCHOR)) {
            if (anchor.isIn(player.level()) && anchor.at().distanceTo(player.position()) <= ANCHOR_LIFT) return true;
        }
        return false;
    }

    private static String anchorTag(Player player, Vec3 at) {
        return "ks_anchor_" + player.getUUID() + "_" + Math.round(at.x) + "_" + Math.round(at.y) + "_" + Math.round(at.z);
    }

    // =========================
    // Wandering (Wanderer): for WANDERING_TICKS you are a trail of starlight in the Astral World, flying
    // WANDERING_SPEED toward where you look, through blocks and creatures; nothing sees, targets or hurts you
    // (status/Concealment) and you cannot cast. You step out where you are when the time is up or you press the key
    // again (somewhere you fit), with no fall damage. Symbolization (Planeswalker): SYMBOLIZATION_TICKS, and allies
    // within SYMBOLIZATION_COMPANY come along. Conceptualization (Door): enemies you fly through are banished
    // (status/Exile) for CONCEPTUALIZATION_BANISH. A Teleport root stops it from starting.
    // =========================
    private static AbilityWheel.Result wandering(ServerPlayer player, ServerLevel sl) {
        if (!Teleport.canGo(player)) {
            player.sendSystemMessage(Component.literal("Something holds you where you are."));
            return AbilityWheel.Result.REFUSED;
        }
        Form form = Beyonder.hasSequence(player, CONCEPTUALIZATION_SEQUENCE) ? Form.CONCEPTUALIZATION
                : Beyonder.hasSequence(player, SYMBOLIZATION_SEQUENCE) ? Form.SYMBOLIZATION : Form.WANDERING;
        int ticks = form == Form.WANDERING ? WANDERING_TICKS : SYMBOLIZATION_TICKS;
        List<Companion> company = new ArrayList<>();
        if (form != Form.WANDERING) {
            for (LivingEntity ally : Spells.alliesAround(player, sl, player.position(), SYMBOLIZATION_COMPANY)) {
                company.add(new Companion(ally.getUUID(), ally.position().subtract(player.position())));
                Concealment.hide(ally, ticks + 5);
            }
        }
        WANDER_COMPANY.put(player.getUUID(), company);
        player.getPersistentData().putLong(WANDER_UNTIL, sl.getGameTime() + ticks);
        player.getPersistentData().putInt(WANDER_FORM, form.ordinal());
        Concealment.hide(player, ticks + 5);
        KeyOfStarsFx.wandering(sl, player, ticks, form, wanderTag(player));
        return AbilityWheel.Result.DONE;
    }

    /** True while this player is Wandering. */
    public static boolean isWandering(Player player) {
        return player.getPersistentData().getLongOr(WANDER_UNTIL, 0L) > 0L;
    }

    private static Form formOf(Player player) {
        int i = player.getPersistentData().getIntOr(WANDER_FORM, 0);
        return Form.values()[Math.max(0, Math.min(Form.values().length - 1, i))];
    }

    private static void wanderTick(Player player, ServerLevel sl) {
        if (sl.getGameTime() >= player.getPersistentData().getLongOr(WANDER_UNTIL, 0L) || !player.isAlive()) {
            endWandering(player, sl);
            return;
        }
        Form form = formOf(player);
        Vec3 next = player.position().add(player.getLookAngle().scale(WANDERING_SPEED));
        if (sl.isOutsideBuildHeight(BlockPos.containing(next))) next = new Vec3(next.x, player.getY(), next.z);
        player.teleportTo(next.x, next.y, next.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        for (Companion companion : WANDER_COMPANY.getOrDefault(player.getUUID(), List.of())) {
            if (sl.getEntity(companion.id()) instanceof LivingEntity ally && ally.isAlive()) {
                Vec3 at = next.add(companion.offset());
                ally.teleportTo(at.x, at.y, at.z);
                ally.setDeltaMovement(Vec3.ZERO);
                ally.fallDistance = 0;
            }
        }
        if (player.tickCount % 2 == 0) KeyOfStarsFx.wanderingTick(sl, player, form);
        if (form == Form.CONCEPTUALIZATION) {
            for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.getBoundingBox().getCenter(), CONCEPTUALIZATION_REACH)) {
                if (Exile.isExiled(enemy)) continue;
                KeyOfStarsFx.conceptualized(sl, enemy);
                Exile.banish(enemy, CONCEPTUALIZATION_BANISH);
            }
        }
    }

    /** Steps out of the Astral World where they are (somewhere they fit), with no fall damage after. */
    static void endWandering(Player player, ServerLevel sl) {
        player.getPersistentData().remove(WANDER_UNTIL);
        player.getPersistentData().remove(WANDER_FORM);
        Concealment.reveal(player);
        Vec3 out = Teleport.standNear(player, sl, player.position(), 6.0);
        if (out != null) Teleport.of(player).ignoringTeleportRoots().fallGuard(WANDERING_LANDING).go(sl, out);
        for (Companion companion : WANDER_COMPANY.getOrDefault(player.getUUID(), List.of())) {
            if (!(sl.getEntity(companion.id()) instanceof LivingEntity ally) || !ally.isAlive()) continue;
            Concealment.reveal(ally);
            Vec3 spot = Teleport.standNear(ally, sl, ally.position(), 6.0);
            if (spot != null) Teleport.of(ally).ignoringTeleportRoots().fallGuard(WANDERING_LANDING).go(sl, spot);
        }
        WANDER_COMPANY.remove(player.getUUID());
        KeyOfStarsFx.wanderingEnd(sl, player, wanderTag(player));
        KeyOfStarsFx.statusEnd(player, Status.WANDERING);
    }

    private static String wanderTag(Player player) {
        return "ks_wander_" + player.getId();
    }
}
