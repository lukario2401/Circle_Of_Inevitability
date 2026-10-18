package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionFx.Bribe;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionFx.Status;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionMarks.Kind;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.Connection;
import net.lukario.circleofinevitability.status.Taunt;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Prince Of Abolition, key 3: the Bribe wheel (Briber, Sequence 7). docs/PRINCE_OF_ABOLITION.md, "Key 3 wheel: Bribe".
 *
 *   Weaken     the enemy's attacks are weaker and it takes more from you
 *   Charm      it cannot bring itself to fight you; now and then it turns on its own side
 *   Arrogance  it underestimates you: many of its attacks miss
 *   Connect    a thin black thread: you see it (it glows), your aimed abilities reach it three times as far, and
 *              your other Bribes on it cost half
 *   Coin Shot  a coin flicked like a bullet: hitscan, then it ricochets once (a direct hit)
 *
 * A Bribe is symbolic, or actual: holding a gold ingot or an emerald in the off hand spends one, and the Bribe lasts
 * half as long again and spreads to every enemy within BRIBE_SPREAD of the target (Coin Shot: more damage, and its
 * first enemy is Weakened). From Earl of the Fallen (4) every Bribe spreads, and Charm turns them twice as often.
 *
 * Magnify (Earl of the Fallen) turns a Weaken, Charm or Arrogance into a thrown flower that lands as a quake: every
 * enemy round where the target stood gets it.
 *
 * Sneak opens the wheel; a normal press casts the last pick again (no cooldown; 3x the cost at Briber, 2x at Baron of
 * Corruption, 1x from Mentor of Disorder; no cooldowns at all from Earl of the Fallen). PrinceOfAbolition.recastCost.
 */
public final class PrinceOfAbolitionBribes {

    private PrinceOfAbolitionBribes() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks; base values before Might)
    // =========================

    static final int BRIBE_SEQUENCE = 7;                        // the key
    private static final double BRIBE_RANGE = 16.0;
    private static final double BRIBE_SPREAD = 3.0;             // an actual Bribe reaches the enemies this close to the target
    private static final float ACTUAL_LONGER = 1.5f;
    private static final int SPREAD_SEQUENCE = 4;               // Earl of the Fallen: every Bribe spreads

    // Weaken
    private static final int WEAKEN_COST = 1250;
    private static final int WEAKEN_COOLDOWN = 160;
    private static final int WEAKEN_TICKS = 240;
    static final float WEAKEN_CUT = 0.35f;                      // its attacks 35% weaker...
    static final float WEAKEN_EXTRA = 0.25f;                    // ...and it takes 25% more from you

    // Charm
    private static final int CHARM_COST = 1500;
    private static final int CHARM_COOLDOWN = 280;
    private static final int CHARM_TICKS = 200;
    static final float CHARM_CUT = 0.5f;                        // a charmed player deals 50% less to you
    private static final float CHARM_TURN = 0.05f;              // every second: the chance a mob turns on its own side...
    private static final int CHARM_TURN_TICKS = 100;            // ...for this long
    private static final double CHARM_TURN_REACH = 10.0;

    // Arrogance
    private static final int ARROGANCE_COST = 1250;
    private static final int ARROGANCE_COOLDOWN = 240;
    private static final int ARROGANCE_TICKS = 240;
    static final float ARROGANCE_MISS = 0.3f;                   // 30% of its attacks miss

    // Connect
    private static final int CONNECT_COST = 800;
    private static final int CONNECT_COOLDOWN = 120;
    private static final double CONNECT_RANGE = 32.0;
    private static final int CONNECT_TICKS = 600;
    private static final float CONNECTED_COST = 0.5f;           // other Bribes on a Connected enemy cost half

    // Coin Shot
    private static final int COIN_COST = 1000;
    private static final int COIN_COOLDOWN = 60;
    private static final double COIN_RANGE = 24.0;
    private static final float COIN_DAMAGE = 30f;
    private static final float COIN_ACTUAL = 1.5f;              // an actual coin: 50% more...
    private static final int COIN_WEAKEN_TICKS = 120;           // ...and the first enemy is Weakened this long
    private static final double RICOCHET_REACH = 6.0;
    private static final float RICOCHET_SHARE = 0.5f;

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("poa_bribe", "Bribe")
            .radius(72)
            .quickCastCooldown(false).quickCastCostBy(PrinceOfAbolition::recastCost)
            .noCooldownsWhen(PrinceOfAbolition::noCooldowns)
            .entry("weaken", PrinceOfAbolitionFx.WHEEL_WEAKEN, "Weaken", BRIBE_SEQUENCE,
                    WEAKEN_COST, WEAKEN_COOLDOWN, (player, sl) -> bribe(player, sl, Bribe.WEAKEN)).costBy(PrinceOfAbolitionBribes::connectedCost)
            .entry("charm", PrinceOfAbolitionFx.WHEEL_CHARM, "Charm", BRIBE_SEQUENCE,
                    CHARM_COST, CHARM_COOLDOWN, (player, sl) -> bribe(player, sl, Bribe.CHARM)).costBy(PrinceOfAbolitionBribes::connectedCost)
            .entry("arrogance", PrinceOfAbolitionFx.WHEEL_ARROGANCE, "Arrogance", BRIBE_SEQUENCE,
                    ARROGANCE_COST, ARROGANCE_COOLDOWN, (player, sl) -> bribe(player, sl, Bribe.ARROGANCE)).costBy(PrinceOfAbolitionBribes::connectedCost)
            .entry("connect", PrinceOfAbolitionFx.WHEEL_CONNECT, "Connect", BRIBE_SEQUENCE,
                    CONNECT_COST, CONNECT_COOLDOWN, PrinceOfAbolitionBribes::connect)
            .entry("coin_shot", PrinceOfAbolitionFx.WHEEL_COIN_SHOT, "Coin Shot", BRIBE_SEQUENCE,
                    COIN_COST, COIN_COOLDOWN, PrinceOfAbolitionBribes::coinShot);

    // =========================
    // Ability 3 - Bribe
    // Sneak: the Bribe wheel. Normal: the last pick again, without the wheel.
    // =========================
    public static void princeOfAbolitionBribe(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!PrinceOfAbolition.ready(player, bypassClassCheck, BRIBE_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.open(player);
        else WHEEL.quickCast(player);
    }

    /** A Bribe aimed at someone this player has Connected costs half. */
    private static int connectedCost(ServerPlayer player, int cost) {
        if (!(player.level() instanceof ServerLevel sl)) return cost;
        LivingEntity target = PrinceOfAbolition.aim(player, sl, BRIBE_RANGE);
        return target != null && PrinceOfAbolitionMarks.from(target, Kind.CONNECTED, player) ? Math.round(cost * CONNECTED_COST) : cost;
    }

    // =========================
    // Weaken, Charm, Arrogance: a coin is pressed on the enemy you look at (BRIBE_RANGE); when it lands
    // (PrinceOfAbolitionFx.BRIBE_LANDS) the Bribe takes. Actual (a gold ingot or emerald spent): ACTUAL_LONGER as long,
    // and every enemy within BRIBE_SPREAD of the target is bribed too (from Earl of the Fallen, always).
    // =========================
    private static AbilityWheel.Result bribe(ServerPlayer player, ServerLevel sl, Bribe bribe) {
        LivingEntity target = PrinceOfAbolition.aim(player, sl, BRIBE_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one before you to bribe."));
            return AbilityWheel.Result.REFUSED;
        }
        if (bribe == Bribe.CHARM && target.getMaxHealth() > PrinceOfAbolition.BOSS_HEALTH) {
            player.sendSystemMessage(Component.literal("Nothing you could offer would sway it."));
            return AbilityWheel.Result.REFUSED;
        }
        boolean actual = payTribute(player);
        boolean spreads = actual || Beyonder.hasSequence(player, SPREAD_SEQUENCE);
        int base = switch (bribe) {
            case WEAKEN -> WEAKEN_TICKS;
            case CHARM -> CHARM_TICKS;
            default -> ARROGANCE_TICKS;
        };
        int ticks = PrinceOfAbolition.duration(player, actual ? Math.round(base * ACTUAL_LONGER) : base);
        if (PrinceOfAbolitionFallen.spendMagnify(player, sl)) {
            magnifiedBribe(player, sl, target, bribe, ticks);
            return AbilityWheel.Result.DONE;
        }
        PrinceOfAbolitionFx.bribe(sl, player, target, bribe, actual);
        Later.run(sl, PrinceOfAbolitionFx.BRIBE_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            List<LivingEntity> bribed = new ArrayList<>();
            bribed.add(target);
            if (spreads) {
                double reach = PrinceOfAbolition.area(player, BRIBE_SPREAD);
                for (LivingEntity near : Spells.enemiesAround(player, sl, target.position(), reach)) {
                    if (near != target && !(bribe == Bribe.CHARM && near.getMaxHealth() > PrinceOfAbolition.BOSS_HEALTH)) bribed.add(near);
                }
                PrinceOfAbolitionFx.bribeSpread(sl, target.position(), reach, bribe);
            }
            for (LivingEntity one : bribed) takes(player, sl, one, bribe, ticks);
        });
        return AbilityWheel.Result.DONE;
    }

    /**
     * Magnify (Earl of the Fallen): the Bribe travels as a thrown flower to where the target stands and lands as a
     * quake: every enemy within PrinceOfAbolitionFallen.MAGNIFIED_BRIBE_RADIUS of it is shaken and gets the Bribe.
     */
    private static void magnifiedBribe(Player player, ServerLevel sl, LivingEntity target, Bribe bribe, int ticks) {
        Vec3 at = target.position();
        int flight = PrinceOfAbolitionFx.magnifiedFlower(sl, player, at);
        Later.run(sl, flight, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            double radius = PrinceOfAbolition.area(player, PrinceOfAbolitionFallen.MAGNIFIED_BRIBE_RADIUS);
            PrinceOfAbolitionFx.quake(sl, at, radius);
            for (LivingEntity one : Spells.enemiesAround(player, sl, at, radius)) {
                if (bribe == Bribe.CHARM && one.getMaxHealth() > PrinceOfAbolition.BOSS_HEALTH) continue;
                Spells.push(one, at, 0.4, 0.35);
                takes(player, sl, one, bribe, ticks);
            }
        });
    }

    /** One Bribe takes on one enemy. */
    private static void takes(Player player, ServerLevel sl, LivingEntity target, Bribe bribe, int ticks) {
        Kind kind = switch (bribe) {
            case WEAKEN -> Kind.WEAKENED;
            case CHARM -> Kind.CHARMED;
            case ARROGANCE -> Kind.ARROGANT;
            case CONNECT -> Kind.CONNECTED;
        };
        int lasts = PrinceOfAbolitionMarks.put(player, target, kind, ticks);
        if (lasts <= 0) return;
        PrinceOfAbolitionFx.bribed(sl, target, bribe, lasts, PrinceOfAbolitionMarks.tag(target, kind));
        if (bribe == Bribe.CHARM) {
            if (target instanceof Mob mob && mob.getTarget() == player) mob.setTarget(null);   // it wanders off
            long until = sl.getGameTime() + lasts;
            Later.run(sl, 20, () -> charmBeat(player, sl, target, until));
        }
    }

    /** Once a second while Charmed: a mob may turn on its own side for a while. */
    private static void charmBeat(Player player, ServerLevel sl, LivingEntity target, long until) {
        if (!target.isAlive() || target.level() != sl || sl.getGameTime() >= until || !PrinceOfAbolitionMarks.from(target, Kind.CHARMED, player)) return;
        float chance = Beyonder.hasSequence(player, SPREAD_SEQUENCE) ? CHARM_TURN * 2f : CHARM_TURN;
        if (target instanceof Mob mob && mob.getRandom().nextFloat() < chance) {
            LivingEntity ally = nearestOtherEnemy(player, sl, mob);
            if (ally != null && Taunt.apply(mob, ally, CHARM_TURN_TICKS)) PrinceOfAbolitionFx.charmTurn(sl, mob, ally);
        }
        Later.run(sl, 20, () -> charmBeat(player, sl, target, until));
    }

    /** The nearest of the caster's other enemies to 'mob' (its own side), within CHARM_TURN_REACH, or null. */
    private static LivingEntity nearestOtherEnemy(Player player, ServerLevel sl, Mob mob) {
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity other : Spells.enemiesAround(player, sl, mob.position(), CHARM_TURN_REACH)) {
            if (other == mob) continue;
            double distance = other.distanceToSqr(mob);
            if (distance < bestDistance) {
                best = other;
                bestDistance = distance;
            }
        }
        return best;
    }

    // =========================
    // Connect: a thin black thread ties you to the enemy you look at (CONNECT_RANGE) for CONNECT_TICKS. It glows for
    // the whole time, your aimed abilities reach it at PrinceOfAbolition.CONNECT_REACH times their range, and your
    // other Bribes on it cost CONNECTED_COST. It also becomes your connection (status/Connection).
    // =========================
    private static AbilityWheel.Result connect(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = PrinceOfAbolition.aim(player, sl, CONNECT_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one in sight to tie a thread to."));
            return AbilityWheel.Result.REFUSED;
        }
        boolean actual = payTribute(player);
        int ticks = PrinceOfAbolition.duration(player, actual ? Math.round(CONNECT_TICKS * ACTUAL_LONGER) : CONNECT_TICKS);
        PrinceOfAbolitionFx.bribe(sl, player, target, Bribe.CONNECT, actual);
        Later.run(sl, PrinceOfAbolitionFx.BRIBE_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            int lasts = PrinceOfAbolitionMarks.put(player, target, Kind.CONNECTED, ticks);
            if (lasts <= 0) return;
            Connection.touch(player, target);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, lasts, 0, false, false));
            PrinceOfAbolitionFx.connect(sl, player, target, lasts, PrinceOfAbolitionMarks.tag(target, Kind.CONNECTED));
            PrinceOfAbolitionFx.status(player, Status.CONNECT, lasts);
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Coin Shot (a direct hit): you flick a coin and it flies like a bullet along a COIN_RANGE line: COIN_DAMAGE to the
    // first enemy on it (hitscan: strafing does not dodge it), then it ricochets once to the nearest other enemy within
    // RICOCHET_REACH for RICOCHET_SHARE. Actual: COIN_ACTUAL times the damage, and the first enemy is Weakened.
    // =========================
    private static AbilityWheel.Result coinShot(ServerPlayer player, ServerLevel sl) {
        double range = PrinceOfAbolition.range(player, COIN_RANGE);
        Vec3 eye = player.getEyePosition();
        Vec3 end = Spells.aimPoint(player, sl, range);
        LivingEntity first = Spells.firstEnemyOnLine(player, sl, eye, end, 0.3);
        boolean actual = payTribute(player);
        float damage = actual ? COIN_DAMAGE * COIN_ACTUAL : COIN_DAMAGE;
        Vec3 hitAt = first != null ? first.position().add(0, first.getBbHeight() * 0.6, 0) : end;
        PrinceOfAbolitionFx.coinShot(sl, player, hitAt, actual);
        if (first == null) return AbilityWheel.Result.DONE;                     // the coin is spent all the same
        Spells.strike(player, first, damage);
        if (actual && first.isAlive()) takes(player, sl, first, Bribe.WEAKEN, PrinceOfAbolition.duration(player, COIN_WEAKEN_TICKS));
        LivingEntity next = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity other : Spells.enemiesAround(player, sl, hitAt, PrinceOfAbolition.area(player, RICOCHET_REACH))) {
            if (other == first) continue;
            double distance = other.distanceToSqr(first);
            if (distance < bestDistance) {
                next = other;
                bestDistance = distance;
            }
        }
        if (next != null) {
            PrinceOfAbolitionFx.ricochet(sl, hitAt, next.position().add(0, next.getBbHeight() * 0.6, 0));
            Spells.strike(player, next, damage * RICOCHET_SHARE);
        }
        return AbilityWheel.Result.DONE;
    }

    /** An actual Bribe: a gold ingot or an emerald in the off hand is spent. False (nothing spent) when there is none. */
    static boolean payTribute(Player player) {
        ItemStack held = player.getOffhandItem();
        if (held.isEmpty() || !(held.is(Items.GOLD_INGOT) || held.is(Items.EMERALD))) return false;
        if (!player.isCreative()) held.shrink(1);
        return true;
    }
}
