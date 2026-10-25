package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.SpellFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Teleport;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Forgotten;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Key Of Stars, key 2: the Tricks wheel (Trickmaster, Sequence 8). docs/KEY_OF_STARS.md, "Key 2 wheel: Tricks".
 * Strange but relatively weak: cheap and fast, and the real use is in what they set up.
 *
 *   Flash          a blinding flash in a cone: Blindness, and mobs lose you for a moment
 *   Black Screen   a dark curtain 4 blocks ahead: a mob that would see you through it loses you
 *   Loud Noise     a bang where you aim (or at your feet): enemies near are slowed, mobs forget you and go to look
 *   Frost Ray      a ray of frost: damage, Slowness III, freezing; the fire on what it hits goes out
 *   Gale           a gale in a cone: enemies shoved back, their arrows blown aside, their fire put out
 *   Tumble         a greased circle where you aim: enemies in it slip, are shoved and stagger now and then
 *   Escape Trick   a long shift toward where you look after another trick (the diversion), else a small hop
 *
 * Sneak casts the last trick again without the wheel. Every entry's cost goes through KeyOfStars.wheelCostFor and
 * every cooldown through KeyOfStars.cooldownFor. The looks are KeyOfStarsFx's; every number is right below.
 */
public final class KeyOfStarsTricks {

    private KeyOfStarsTricks() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int TRICKS_SEQUENCE = 8;                       // the key and every trick

    // Flash
    private static final int FLASH_COST = 300;
    private static final int FLASH_COOLDOWN = 160;
    private static final double FLASH_RANGE = 14.0;
    private static final double FLASH_ANGLE = 35.0;             // half the cone, degrees
    private static final int FLASH_BLIND = 60;
    private static final int FLASH_FORGET = 20;                 // mobs lose their target this long

    // Black Screen
    private static final int SCREEN_COST = 400;
    private static final int SCREEN_COOLDOWN = 240;
    private static final double SCREEN_AHEAD = 4.0;
    private static final double SCREEN_HALF_WIDTH = 3.0;        // 6 wide...
    private static final double SCREEN_HEIGHT = 3.0;            // ...3 tall
    private static final int SCREEN_TICKS = 160;
    private static final int SCREEN_CHECK = 10;                 // how often mobs behind it are looked at
    private static final double SCREEN_HUNTERS = 32.0;

    // Loud Noise
    private static final int NOISE_COST = 300;
    private static final int NOISE_COOLDOWN = 200;
    private static final double NOISE_RANGE = 24.0;
    private static final double NOISE_RADIUS = 8.0;
    private static final int NOISE_SLOW = 40;
    private static final int NOISE_FORGET = 60;

    // Frost Ray
    private static final int FROST_COST = 600;
    private static final int FROST_COOLDOWN = 60;
    private static final double FROST_RANGE = 10.0;
    private static final double FROST_SPEED = 3.0;              // blocks a tick, an arrow's
    private static final float FROST_DAMAGE = 14f;
    private static final int FROST_SLOW = 60;                   // Slowness III this long
    private static final int FROST_FROZEN = 160;                // freezing ticks it is left with

    // Gale
    private static final int GALE_COST = 500;
    private static final int GALE_COOLDOWN = 160;
    private static final double GALE_RANGE = 12.0;
    private static final double GALE_ANGLE = 40.0;
    private static final double GALE_PUSH = 1.2;                // about 6 blocks
    private static final double GALE_LIFT = 0.35;
    private static final double GALE_SHOTS = 1.2;               // how hard arrows and thrown things are blown aside

    // Tumble
    private static final int TUMBLE_COST = 700;
    private static final int TUMBLE_COOLDOWN = 280;
    private static final double TUMBLE_RANGE = 16.0;
    private static final double TUMBLE_RADIUS = 5.0;
    private static final int TUMBLE_TICKS = 120;
    private static final int TUMBLE_STAGGER_EVERY = 40;
    private static final int TUMBLE_STAGGER = 20;
    private static final double TUMBLE_SHOVE = 0.35;

    // Escape Trick
    private static final int ESCAPE_COST = 500;
    private static final int ESCAPE_COOLDOWN = 120;
    private static final int DIVERSION_TICKS = 60;              // another trick this recently is the diversion...
    private static final double ESCAPE_FAR = 10.0;              // ...and the shift goes this far
    private static final int ESCAPE_FARTHER_SEQUENCE = 4;       // Secrets Sorcerer: this far
    private static final double ESCAPE_FARTHER = 14.0;
    private static final double ESCAPE_HOP = 4.0;               // with no diversion

    // =========================
    // Names and what is kept
    // =========================
    private static final String LAST_TRICK = "key_of_stars_last_trick";            // game time of the last trick (not Escape)
    private static final String TUMBLE = "key_of_stars_tumble";                    // combat/Zone

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("key_of_stars_tricks", "Tricks")
            .radius(80)
            .quickCastCooldown(false).quickCastCost(KeyOfStars.QUICK_CAST_COST)     // sneak: no cooldown, 3x the cost
            .cooldownBy(KeyOfStars::cooldownFor)
            .entry("flash", KeyOfStarsFx.WHEEL_FLASH, "Flash", TRICKS_SEQUENCE, FLASH_COST, FLASH_COOLDOWN,
                    KeyOfStarsTricks::flash)
            .costBy(KeyOfStars::wheelCostFor)
            .entry("black_screen", KeyOfStarsFx.WHEEL_BLACK_SCREEN, "Black Screen", TRICKS_SEQUENCE, SCREEN_COST,
                    SCREEN_COOLDOWN, KeyOfStarsTricks::blackScreen)
            .costBy(KeyOfStars::wheelCostFor)
            .entry("loud_noise", KeyOfStarsFx.WHEEL_LOUD_NOISE, "Loud Noise", TRICKS_SEQUENCE, NOISE_COST, NOISE_COOLDOWN,
                    KeyOfStarsTricks::loudNoise)
            .costBy(KeyOfStars::wheelCostFor)
            .entry("frost_ray", KeyOfStarsFx.WHEEL_FROST_RAY, "Frost Ray", TRICKS_SEQUENCE, FROST_COST, FROST_COOLDOWN,
                    KeyOfStarsTricks::frostRay)
            .costBy(KeyOfStars::wheelCostFor)
            .entry("gale", KeyOfStarsFx.WHEEL_GALE, "Gale", TRICKS_SEQUENCE, GALE_COST, GALE_COOLDOWN,
                    KeyOfStarsTricks::gale)
            .costBy(KeyOfStars::wheelCostFor)
            .entry("tumble", KeyOfStarsFx.WHEEL_TUMBLE, "Tumble", TRICKS_SEQUENCE, TUMBLE_COST, TUMBLE_COOLDOWN,
                    KeyOfStarsTricks::tumble)
            .costBy(KeyOfStars::wheelCostFor)
            .entry("escape_trick", KeyOfStarsFx.WHEEL_ESCAPE_TRICK, "Escape Trick", TRICKS_SEQUENCE, ESCAPE_COST,
                    ESCAPE_COOLDOWN, KeyOfStarsTricks::escapeTrick)
            .costBy(KeyOfStars::wheelCostFor)
            .free(KeyOfStars::freeTravel)                                        // Door of Doors (or an Alternate World): free
            .onCast((player, sl, entry) -> {
                if (!entry.key().equals("escape_trick")) player.getPersistentData().putLong(LAST_TRICK, sl.getGameTime());
            });

    // =========================
    // Ability 2 - Tricks
    // Normal: the Tricks wheel. Sneak: the last trick again, without the wheel.
    // =========================
    public static void keyOfStarsTricks(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!KeyOfStars.ready(player, bypassClassCheck, TRICKS_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    /** The tricks an echo (combat/Echo) never repeats from elsewhere: the ones that move you. */
    static boolean isTravel(String entryKey) {
        return entryKey.equals("escape_trick");
    }

    static void clearState(Player player) {
        player.getPersistentData().remove(LAST_TRICK);
    }

    // =========================
    // Flash: every enemy in a cone FLASH_RANGE long, FLASH_ANGLE wide, is blinded for FLASH_BLIND; mobs lose their
    // target for FLASH_FORGET. No damage.
    // =========================
    private static AbilityWheel.Result flash(ServerPlayer player, ServerLevel sl) {
        KeyOfStarsFx.flash(sl, player, FLASH_RANGE);
        for (LivingEntity enemy : Spells.enemiesInCone(player, sl, player.getEyePosition(), player.getLookAngle(),
                FLASH_RANGE, FLASH_ANGLE)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, FLASH_BLIND, 0));
            KeyOfStarsFx.blinded(sl, enemy, FLASH_BLIND);
            if (enemy instanceof Mob mob) Forgotten.apply(mob, player, FLASH_FORGET);
        }
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Black Screen: a curtain SCREEN_HALF_WIDTH * 2 wide and SCREEN_HEIGHT tall, SCREEN_AHEAD in front of you, for
    // SCREEN_TICKS. Every SCREEN_CHECK ticks, a mob hunting you whose sight of you passes through it loses you.
    // You and your allies walk through it; projectiles pass.
    // =========================
    private static AbilityWheel.Result blackScreen(ServerPlayer player, ServerLevel sl) {
        float yaw = player.getYRot();
        Vec3 normal = Spells.flatLook(player);
        Vec3 center = Spells.groundAt(player, sl, player.position().add(normal.scale(SCREEN_AHEAD)).add(0, 1.0, 0));
        String tag = "ks_screen_" + player.getId() + "_" + sl.getGameTime();
        KeyOfStarsFx.blackScreen(sl, center, yaw, SCREEN_TICKS, tag);
        screenTick(player, sl, center, normal, SCREEN_TICKS);
        Later.run(sl, SCREEN_TICKS, () -> KeyOfStarsFx.blackScreenEnd(sl, center, tag));
        return AbilityWheel.Result.DONE;
    }

    private static void screenTick(Player player, ServerLevel sl, Vec3 center, Vec3 normal, int ticksLeft) {
        if (ticksLeft <= 0 || !Spells.casterStillHere(player, sl)) return;
        for (Mob hunter : Forgotten.huntersOf(sl, player, SCREEN_HUNTERS)) {
            if (behindScreen(center, normal, hunter.getEyePosition(), player.getEyePosition())) {
                Forgotten.apply(hunter, player, SCREEN_CHECK * 2);
            }
        }
        Later.run(sl, SCREEN_CHECK, () -> screenTick(player, sl, center, normal, ticksLeft - SCREEN_CHECK));
    }

    /** True when the line from 'eye' to 'seen' passes through the curtain standing at 'center' facing 'normal'. */
    private static boolean behindScreen(Vec3 center, Vec3 normal, Vec3 eye, Vec3 seen) {
        double a = eye.subtract(center).dot(normal);
        double b = seen.subtract(center).dot(normal);
        if (a * b >= 0) return false;                                       // both on one side: it is not in the way
        double t = a / (a - b);
        Vec3 crossing = eye.add(seen.subtract(eye).scale(t));
        Vec3 side = new Vec3(-normal.z, 0, normal.x);
        double across = crossing.subtract(center).dot(side);
        double up = crossing.y - center.y;
        return Math.abs(across) <= SCREEN_HALF_WIDTH && up >= 0 && up <= SCREEN_HEIGHT;
    }

    // =========================
    // Loud Noise: a bang where you aim (no further than NOISE_RANGE; aimed at nothing, at your feet). Enemies within
    // NOISE_RADIUS: Slowness I for NOISE_SLOW; mobs lose their target for NOISE_FORGET and walk to where it was.
    // =========================
    private static AbilityWheel.Result loudNoise(ServerPlayer player, ServerLevel sl) {
        Vec3 eye = player.getEyePosition();
        boolean aimed = Spells.aimPoint(player, sl, NOISE_RANGE).distanceTo(eye) < NOISE_RANGE - 0.5;
        Vec3 at = aimed ? Spells.aimGround(player, sl, NOISE_RANGE) : player.position();
        KeyOfStarsFx.loudNoise(sl, player, at);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, at, NOISE_RADIUS)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, NOISE_SLOW, 0));
            KeyOfStarsFx.deafened(sl, enemy, NOISE_SLOW);
            if (enemy instanceof Mob mob) {
                Forgotten.apply(mob, player, NOISE_FORGET);
                mob.getNavigation().moveTo(at.x, at.y, at.z, 1.0);
            }
        }
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Frost Ray: a ray of frost from the hand at an arrow's speed, FROST_RANGE long. The first enemy on it takes
    // FROST_DAMAGE, Slowness III for FROST_SLOW and is left freezing (FROST_FROZEN); the fire on it goes out.
    // =========================
    private static AbilityWheel.Result frostRay(ServerPlayer player, ServerLevel sl) {
        Vec3 hand = SpellFx.handOf(player, sl);
        if (hand == null) return AbilityWheel.Result.REFUSED;
        Vec3 way = Spells.aimFrom(player, sl, hand, FROST_RANGE);
        Vec3 end = hand.add(way.scale(FROST_RANGE));
        Vec3 stop = sl.clip(new ClipContext(hand, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getLocation();
        LivingEntity target = Spells.firstEnemyOnLine(player, sl, hand, stop, 0.4);
        Vec3 reach = target == null ? stop : target.getBoundingBox().getCenter();
        KeyOfStarsFx.frostRay(sl, player, hand, reach);
        if (target != null) {
            int flight = Math.max(1, (int) Math.ceil(hand.distanceTo(reach) / FROST_SPEED));
            Later.run(sl, flight, () -> {
                if (!target.isAlive() || target.level() != sl) return;
                Spells.strike(player, target, FROST_DAMAGE);
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, FROST_SLOW, 2));
                target.setTicksFrozen(Math.max(target.getTicksFrozen(), FROST_FROZEN));
                target.clearFire();
                KeyOfStarsFx.frosted(sl, target, FROST_SLOW, "ks_frost_" + target.getId());
            });
        }
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Gale: a gale in a cone GALE_RANGE long: enemies in it are shoved back (GALE_PUSH, GALE_LIFT) and their fire
    // goes out; hostile arrows and thrown things in it are blown aside.
    // =========================
    private static AbilityWheel.Result gale(ServerPlayer player, ServerLevel sl) {
        Vec3 chest = player.getEyePosition().add(0, -0.4, 0);
        Vec3 look = player.getLookAngle();
        KeyOfStarsFx.gale(sl, player, GALE_RANGE);
        for (LivingEntity enemy : Spells.enemiesInCone(player, sl, chest, look, GALE_RANGE, GALE_ANGLE)) {
            Spells.push(enemy, player.position(), GALE_PUSH, GALE_LIFT);
            enemy.clearFire();
            KeyOfStarsFx.galeHit(sl, enemy);
        }
        Vec3 aside = new Vec3(-look.z, 0, look.x);
        double narrowest = Math.cos(Math.toRadians(GALE_ANGLE));
        for (Projectile shot : Spells.hostileShotsAround(player, sl, chest, GALE_RANGE)) {
            Vec3 toIt = shot.position().subtract(chest);
            if (toIt.lengthSqr() > 1.0 && toIt.normalize().dot(look.normalize()) < narrowest) continue;
            double side = toIt.dot(aside) >= 0 ? 1.0 : -1.0;
            shot.setDeltaMovement(shot.getDeltaMovement().scale(0.3).add(aside.scale(side * GALE_SHOTS)).add(look.scale(0.6)));
            shot.hurtMarked = true;
        }
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Tumble: a greased circle TUMBLE_RADIUS wide where you aim (no further than TUMBLE_RANGE) for TUMBLE_TICKS
    // (combat/Zone). Enemies in it: Slowness II, a sideways shove every half second, and a stagger (TUMBLE_STAGGER)
    // every TUMBLE_STAGGER_EVERY. You, your allies and an enemy Key Of Stars from the Trickmaster (Nimble) do not slip.
    // =========================
    private static AbilityWheel.Result tumble(ServerPlayer player, ServerLevel sl) {
        Vec3 center = Spells.aimGround(player, sl, TUMBLE_RANGE);
        String tag = "ks_tumble_" + player.getId() + "_" + sl.getGameTime();
        Zone.at(center, TUMBLE_RADIUS)
                .height(3.0)
                .lasts(TUMBLE_TICKS)
                .every(10)
                .onInside((enemy, ticksInside) -> slip(player, sl, enemy, ticksInside))
                .onPulse(ticksLeft -> KeyOfStarsFx.tumbleTick(sl, center, TUMBLE_RADIUS))
                .onEnd(() -> KeyOfStarsFx.tumbleEnd(sl, center, tag))
                .open(player, sl, TUMBLE);
        KeyOfStarsFx.tumble(sl, player, center, TUMBLE_RADIUS, TUMBLE_TICKS, tag);
        return AbilityWheel.Result.DONE;
    }

    private static void slip(Player player, ServerLevel sl, LivingEntity enemy, int ticksInside) {
        if (enemy instanceof Player other && KeyOfStars.isKeyOfStars(other, TRICKS_SEQUENCE)) return;   // Nimble
        Spells.keepEffect(enemy, MobEffects.SLOWNESS, 1, 20);
        double a = enemy.getRandom().nextDouble() * Math.PI * 2;
        Spells.push(enemy, enemy.position().add(Math.cos(a), 0, Math.sin(a)), TUMBLE_SHOVE, 0.05);
        if (ticksInside > 0 && ticksInside % TUMBLE_STAGGER_EVERY == 0) {
            int ticks = KeyOfStars.rootTicks(player, TUMBLE_STAGGER);
            Root.apply(enemy, ticks, RootRestriction.MOVE, RootRestriction.ATTACK);
            KeyOfStarsFx.slipped(sl, enemy, ticks);
        }
    }

    // =========================
    // Escape Trick: with another trick in the last DIVERSION_TICKS (the diversion) you shift ESCAPE_FAR toward where
    // you look (Secrets Sorcerer: ESCAPE_FARTHER), leaving a crumbling door; with none it is a hop of ESCAPE_HOP.
    // A teleport: a Teleport root stops it.
    // =========================
    private static AbilityWheel.Result escapeTrick(ServerPlayer player, ServerLevel sl) {
        if (!Teleport.canGo(player)) {
            player.sendSystemMessage(Component.literal("Something holds you where you are."));
            return AbilityWheel.Result.REFUSED;
        }
        long last = player.getPersistentData().getLongOr(LAST_TRICK, -1000L);
        boolean diversion = sl.getGameTime() - last <= DIVERSION_TICKS;
        double distance = !diversion ? ESCAPE_HOP
                : Beyonder.hasSequence(player, ESCAPE_FARTHER_SEQUENCE)
                ? ESCAPE_FARTHER : ESCAPE_FAR;
        Vec3 from = player.position();
        Vec3 to = Spells.blinkSpot(player, sl, distance);
        if (to == null) to = Spells.freeSpotNear(player, sl, from, player.getLookAngle(), distance);
        if (to == null) {
            player.sendSystemMessage(Component.literal("There is no room to slip away to."));
            return AbilityWheel.Result.REFUSED;
        }
        KeyOfStarsFx.escapeTrick(sl, player, from, to, diversion);
        KeyOfStars.teleport(player, sl, to, false);
        if (KeyOfStars.freeTravel(player)) KeyOfStarsFx.doorOfDoors(sl, player);
        player.getPersistentData().remove(LAST_TRICK);                          // one diversion, one escape
        return AbilityWheel.Result.DONE;
    }
}
