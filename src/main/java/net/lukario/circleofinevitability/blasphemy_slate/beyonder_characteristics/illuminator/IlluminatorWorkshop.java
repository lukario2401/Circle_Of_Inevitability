package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.Illuminator.Group;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorFx.Status;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorFx.Weakness;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Shot;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.ConjuredArmor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Illuminator, key 4: the Workshop wheel (Artisan, Sequence 6). docs/ILLUMINATOR.md, "Key 4 wheel".
 *
 *   Clockwork Rifle  a brass rifle in your hands fires a volley at what the crosshair is on; against an enemy you
 *                    Appraised the rounds carry its weakness. From Alchemist (4) five rounds
 *   Spirit Cannon    a brass cannon unfolds where you aim and fires at the nearest enemy every 2 seconds. From
 *                    Alchemist (4) two at once
 *   Mystic Brooch    a brooch pinned on you, or on the ally you look at, soaks up the next 60 damage and bursts
 *
 * Sneak casts the last pick again without the wheel (no cooldown, double the cost), as for Hand Of God. Every entry's
 * cost goes through Illuminator.costFor (Quick Study, the Rite); Craftsman's Hands (6) makes the wheel's cooldowns
 * shorter and what it makes last longer, and the Rite of a Ritual Circle you stand in adds to both
 * (Illuminator.lasting). The looks are IlluminatorFx's; every number is right below.
 */
public final class IlluminatorWorkshop {

    private IlluminatorWorkshop() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int WORKSHOP_SEQUENCE = 6;                     // the key
    static final float QUICK_CAST_COST = Illuminator.QUICK_CAST_COST;            // a sneak recast: no cooldown, this many times the cost

    // Clockwork Rifle
    static final int RIFLE_SEQUENCE = 6;
    static final int RIFLE_COST = 1800;                 // Sequence 6 holds 6400
    static final int RIFLE_COOLDOWN = 100;
    static final double RIFLE_RANGE = 48.0;
    static final int RIFLE_ROUNDS = 3;
    static final int RIFLE_MORE_SEQUENCE = 4;           // Alchemist: more rounds
    static final int RIFLE_MORE_ROUNDS = 5;
    static final float RIFLE_DAMAGE = 24f;              // each round; a pierce hit, or the Appraised weakness
    static final float ARM_CANNON_COST = 0.5f;          // through the Mechanical Body's arm cannon (key 6): half
                                                                // the cost and no cooldown

    // Spirit Cannon
    static final int CANNON_SEQUENCE = 6;
    static final int CANNON_COST = 2450;
    static final int CANNON_COOLDOWN = 600;
    static final double CANNON_PLACE = 12.0;            // where you aim, this far at most
    static final int CANNON_TICKS = 400;                // it stands this long (longer with Craftsman's Hands and the Rite)...
    static final int CANNON_EVERY = 40;                 // ...and fires this often...
    static final double CANNON_REACH = 24.0;            // ...at the nearest enemy it can see this far
    static final double CANNONBALL_SPEED = 1.6;
    static final float CANNON_DAMAGE = 30f;             // to the one it hits (a pierce hit)...
    static final double CANNON_BLAST = 2.5;             // ...and this share of it to the rest within this
    static final float CANNON_SPLASH = 0.5f;
    static final int TWIN_CANNON_SEQUENCE = 4;          // Alchemist: two cannons at once

    // Mystic Brooch
    static final int BROOCH_SEQUENCE = 6;
    static final int BROOCH_COST = 1500;
    static final int BROOCH_COOLDOWN = 800;
    static final double BROOCH_RANGE = 24.0;            // the ally looked at, or you
    static final int BROOCH_TICKS = 1200;
    static final float BROOCH_WARD = 60f;               // it soaks up this much (status/ConjuredArmor's absorption)
    static final double BROOCH_BURST_RADIUS = 5.0;      // used up or run out: enemies this close are shoved...
    static final float BROOCH_BURST_DAMAGE = 20f;       // ...and hurt

    // =========================
    // Names
    // =========================
    static final String CANNON = "illuminator_cannon_";                   // + 0 or 1: its Zone
    static final String BROOCH = "illuminator_brooch";                            // its ConjuredArmor

    // =========================
    // The key
    // =========================

    static final AbilityWheel WHEEL = AbilityWheel.create("illuminator_workshop", "Workshop")
            .radius(72)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .cooldownBy((player, ticks) -> Illuminator.isIlluminator(player, Illuminator.CRAFTSMAN_SEQUENCE)
                    ? Math.round(ticks * Illuminator.CRAFTSMAN_COOLDOWN) : ticks)
            .entry("clockwork_rifle", IlluminatorFx.WHEEL_CLOCKWORK_RIFLE, "Clockwork Rifle", RIFLE_SEQUENCE,
                    RIFLE_COST, RIFLE_COOLDOWN, IlluminatorWorkshop::clockworkRifle)
            .costBy((player, cost) -> Illuminator.costFor(player,
                    Illuminator.wearsMechanicalBody(player) ? Math.round(cost * ARM_CANNON_COST) : cost, "clockwork_rifle", Group.WORKSHOP))
            .entry("spirit_cannon", IlluminatorFx.WHEEL_SPIRIT_CANNON, "Spirit Cannon", CANNON_SEQUENCE,
                    CANNON_COST, CANNON_COOLDOWN, IlluminatorWorkshop::spiritCannon)
            .costBy((player, cost) -> Illuminator.costFor(player, cost, "spirit_cannon", Group.WORKSHOP))
            .entry("mystic_brooch", IlluminatorFx.WHEEL_MYSTIC_BROOCH, "Mystic Brooch", BROOCH_SEQUENCE,
                    BROOCH_COST, BROOCH_COOLDOWN, IlluminatorWorkshop::mysticBrooch)
            .costBy((player, cost) -> Illuminator.costFor(player, cost, "mystic_brooch", Group.WORKSHOP))
            .onCast((player, sl, entry) -> {
                Illuminator.afterCast(player, entry.key());
                // the arm cannon fires again at once
                if (entry.key().equals("clockwork_rifle") && Illuminator.wearsMechanicalBody(player)) {
                    IlluminatorWorkshop.WHEEL.resetCooldown(player, entry.key());
                }
            });

    // =========================
    // Ability 4 - Workshop
    // Normal: the Workshop wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void illuminatorWorkshop(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!Illuminator.ready(player, bypassClassCheck, WORKSHOP_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Clockwork Rifle: RIFLE_ROUNDS rounds (RIFLE_MORE_ROUNDS from Alchemist), the first at IlluminatorFx.RIFLE_FIRES
    // and then every RIFLE_STEP, each at what the crosshair is on then (RIFLE_RANGE, very fast: it hits at once):
    // RIFLE_DAMAGE to the first enemy on the line. A pierce hit; against an enemy you Appraised, a hit of its weakness
    // (the match bonus on every round). Through the Mechanical Body's arm cannon (key 6) it fires at once, for half
    // the cost and with no cooldown.
    // =========================
    static AbilityWheel.Result clockworkRifle(LivingEntity player, ServerLevel sl) {
        int rounds = Beyonder.hasSequence(player, RIFLE_MORE_SEQUENCE) ? RIFLE_MORE_ROUNDS : RIFLE_ROUNDS;
        boolean armCannon = Illuminator.wearsMechanicalBody(player);
        int first = armCannon ? 2 : IlluminatorFx.RIFLE_FIRES;
        IlluminatorFx.rifle(sl, player, rounds, armCannon);
        if (armCannon) {
            IlluminatorFx.armCannon(sl, player, first + rounds * IlluminatorFx.RIFLE_STEP + 10, "il_arm_cannon_" + player.getId());
        }
        for (int i = 0; i < rounds; i++) {
            Later.run(sl, first + i * IlluminatorFx.RIFLE_STEP, () -> {
                if (Spells.casterStillHere(player, sl)) fireRound(player, sl);
            });
        }
        return AbilityWheel.Result.DONE;
    }

    /** One round from the muzzle to what the crosshair is on now. */
    private static void fireRound(LivingEntity player, ServerLevel sl) {
        Vec3 muzzle = Spells.clearStart(player, sl, player.getEyePosition()
                .add(Spells.rightOf(player).scale(0.38)).add(player.getLookAngle().scale(0.9)).add(0, -0.2, 0));
        Vec3 direction = Spells.aimFrom(player, sl, muzzle, RIFLE_RANGE);
        Vec3 far = sl.clip(new ClipContext(muzzle, muzzle.add(direction.scale(RIFLE_RANGE)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player)).getLocation();
        LivingEntity target = Spells.firstEnemyOnLine(player, sl, muzzle, far, 0.3);
        Vec3 to = far;
        Weakness carried = null;
        if (target != null) {
            double along = Math.max(0.0, target.getBoundingBox().getCenter().subtract(muzzle).dot(direction));
            to = muzzle.add(direction.scale(along));
            carried = Illuminator.appraisedWeakness(player, target);
        }
        IlluminatorFx.rifleShot(sl, player, muzzle, to, carried);
        if (target != null) Illuminator.hit(player, target, RIFLE_DAMAGE, carried != null ? carried : Weakness.PIERCE);
    }

    // =========================
    // Spirit Cannon: a cannon unfolds on the ground you aim at (CANNON_PLACE at most) and stands for CANNON_TICKS (a
    // Zone; Illuminator.lasting). From IlluminatorFx.CANNON_READY, every CANNON_EVERY it fires a cannonball (a Shot) at
    // the nearest enemy within CANNON_REACH that it can see and that can be chosen: CANNON_DAMAGE to the one it hits and
    // CANNON_SPLASH of it to the rest within CANNON_BLAST, all pierce hits. One at a time (a new one replaces the
    // old); two from Alchemist (the one with less time left goes).
    // =========================
    static AbilityWheel.Result spiritCannon(LivingEntity player, ServerLevel sl) {
        Vec3 spot = Spells.aimGround(player, sl, CANNON_PLACE);
        if (spot.distanceTo(player.position()) > CANNON_PLACE + 2.0) {
            player.sendSystemMessage(Component.literal("There is no ground close enough for the cannon."));
            return AbilityWheel.Result.REFUSED;
        }
        int slot = cannonSlot(player);
        String name = CANNON + slot;
        Zone.close(player, name);                                                  // the cannon this one replaces
        int ticks = Illuminator.lasting(player, CANNON_TICKS, Group.WORKSHOP);
        long opened = sl.getGameTime();
        String tag = "il_cannon_" + player.getId() + "_" + slot + "_" + opened;
        float yaw = player.getYRot();
        IlluminatorFx.spiritCannon(sl, spot, yaw, ticks, tag);
        Zone.at(spot, CANNON_REACH)
                .height(8)
                .lasts(ticks)
                .every(CANNON_EVERY)
                .onPulse(ticksLeft -> {
                    if (sl.getGameTime() - opened < IlluminatorFx.CANNON_READY) return;   // still unfolding
                    LivingEntity target = cannonTarget(player, sl, spot);
                    if (target != null) fireCannon(player, sl, spot, tag, target);
                })
                .onEnd(() -> IlluminatorFx.cannonEnd(sl, spot, tag))
                .open(player, sl, name);
        return AbilityWheel.Result.DONE;
    }

    /** Which cannon the new one is: the free slot, else the one with the least time left. */
    private static int cannonSlot(LivingEntity player) {
        int slots = Beyonder.hasSequence(player, TWIN_CANNON_SEQUENCE) ? 2 : 1;
        int best = 0;
        int least = Integer.MAX_VALUE;
        for (int slot = 0; slot < slots; slot++) {
            if (!Zone.isOpen(player, CANNON + slot)) return slot;
            int left = Zone.ticksLeft(player, CANNON + slot);
            if (left < least) {
                least = left;
                best = slot;
            }
        }
        return best;
    }

    /** The nearest enemy within reach that the cannon can see from its barrel and that can be chosen, or null. */
    private static LivingEntity cannonTarget(LivingEntity player, ServerLevel sl, Vec3 spot) {
        Vec3 barrel = spot.add(0, 0.8, 0);
        LivingEntity best = null;
        double nearest = Double.MAX_VALUE;
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, spot, CANNON_REACH)) {
            if (!Spells.canBeChosen(player, enemy)) continue;
            if (!Spells.clearLine(player, sl, barrel, enemy.getBoundingBox().getCenter())) continue;
            double distance = enemy.distanceToSqr(spot);
            if (distance < nearest) {
                nearest = distance;
                best = enemy;
            }
        }
        return best;
    }

    private static void fireCannon(LivingEntity player, ServerLevel sl, Vec3 spot, String tag, LivingEntity target) {
        Vec3 aim = target.getBoundingBox().getCenter();
        Vec3 muzzle = IlluminatorFx.cannonFire(sl, spot, tag, aim);
        Vec3 direction = aim.subtract(muzzle).normalize();
        List<LivingEntity> struck = new ArrayList<>();
        int arrives = Shot.of(IlluminatorFx.CANNONBALL)
                .speed(CANNONBALL_SPEED).range(CANNON_REACH + 4.0).width(0.4).tip(0.2)
                .onHit((hit, at) -> {
                    struck.add(hit);
                    Illuminator.hit(player, hit, CANNON_DAMAGE, Weakness.PIERCE);
                })
                .onEnd((at, hitBlock) -> {
                    IlluminatorFx.cannonBlast(sl, at, CANNON_BLAST);
                    if (!Spells.casterStillHere(player, sl)) return;
                    for (LivingEntity enemy : Spells.enemiesAround(player, sl, at, CANNON_BLAST)) {
                        if (!struck.contains(enemy)) Illuminator.hit(player, enemy, CANNON_DAMAGE * CANNON_SPLASH, Weakness.PIERCE);
                    }
                })
                .fire(player, sl, muzzle, direction);
        for (int t = 1; t < arrives; t++) {
            Vec3 at = muzzle.add(direction.scale(CANNONBALL_SPEED * t));
            Later.run(sl, t, () -> IlluminatorFx.cannonballTrail(sl, at));
        }
    }

    // =========================
    // Mystic Brooch: a brooch pins itself to the ally you look at (BROOCH_RANGE), or to you, for BROOCH_TICKS
    // (Illuminator.lasting). It soaks up the next BROOCH_WARD damage (status/ConjuredArmor's absorption). When it is
    // used up or runs out it bursts: enemies within BROOCH_BURST_RADIUS are shoved and take BROOCH_BURST_DAMAGE. A
    // new brooch on the same wearer replaces the old one without a burst.
    // =========================
    static AbilityWheel.Result mysticBrooch(LivingEntity player, ServerLevel sl) {
        LivingEntity aimed = Spells.aimAlly(player, sl, BROOCH_RANGE);
        LivingEntity wearer = aimed != null ? aimed : player;
        int ticks = Illuminator.lasting(player, BROOCH_TICKS, Group.WORKSHOP);
        String tag = "il_brooch_" + wearer.getId();
        IlluminatorFx.mysticBrooch(sl, wearer, ticks, tag);
        if (wearer instanceof Player pinned) IlluminatorFx.status(pinned, Status.BROOCH, ticks);
        ConjuredArmor.of(BROOCH)
                .lasts(ticks)
                .absorption(BROOCH_WARD)
                .onHit((hurt, amount) -> {
                    IlluminatorFx.broochAbsorb(sl, hurt);
                    Later.run(sl, 1, () -> {                                     // used up: the absorption is gone after the blow
                        if (ConjuredArmor.isWorn(hurt, BROOCH) && hurt.getAbsorptionAmount() <= 0.01f) ConjuredArmor.takeOff(hurt, BROOCH);
                    });
                })
                .onEnd(gone -> broochBursts(player, sl, gone, tag))
                .wear(wearer);
        return AbilityWheel.Result.DONE;
    }

    private static void broochBursts(LivingEntity player, ServerLevel sl, LivingEntity wearer, String tag) {
        if (wearer instanceof Player pinned) IlluminatorFx.statusEnd(pinned, Status.BROOCH);
        if (wearer.isRemoved() || wearer.level() != sl) return;                    // logged out: nothing to burst
        IlluminatorFx.broochBurst(sl, wearer, BROOCH_BURST_RADIUS, tag);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, wearer.position().add(0, 1, 0), BROOCH_BURST_RADIUS)) {
            if (enemy == wearer) continue;
            Spells.push(enemy, wearer.position(), 0.9, 0.35);
            Illuminator.hit(player, enemy, BROOCH_BURST_DAMAGE, null);
        }
    }

    // =========================
    // Kept tidy by Illuminator
    // =========================

    /** After a relog or a death. */
    static void clearState(Player player) {
        Zone.close(player, CANNON + 0);
        Zone.close(player, CANNON + 1);
    }
}
