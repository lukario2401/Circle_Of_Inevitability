package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionFx.Status;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionMarks.Kind;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionMausoleums.Mausoleum;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Taunt;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Prince Of Abolition, key 6: the Fallen wheel (Barbarian, Sequence 8). docs/PRINCE_OF_ABOLITION.md, "Key 6 wheel".
 *
 *   Raise Mausoleum   (8) a mausoleum rises where you look (PrinceOfAbolitionMausoleums: 2 minutes, +5% attack and
 *                         defense, Sanctuary, and it brings you back once)
 *   Corrosion         (6) everyone round you turns dark and greedy: enemies lose their discipline
 *   Step              (6) you step through a mausoleum: in a gold door here, out of the mausoleum there
 *   Majesty           (5) your presence presses down: enemies kneel, longer for every two standing orders
 *   Mausoleum Salvo   (4) every standing mausoleum fires a gold beam at the enemy you look at (a direct hit)
 *
 * Bestowment, Exploit and Magnify (Earl of the Fallen) are the next half of the Pathway; their pictures are ready
 * (WHEEL_BESTOWMENT, WHEEL_EXPLOIT, WHEEL_MAGNIFY in PrinceOfAbolitionFx).
 *
 * Sneak opens the wheel; a normal press casts the last pick again (PrinceOfAbolition.recastCost). The mausoleums are
 * the Black Emperor's own: a thief casting this key gets whatever entry comes up, and Step and Salvo need mausoleums.
 */
public final class PrinceOfAbolitionFallen {

    private PrinceOfAbolitionFallen() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks; base values before Might)
    // =========================

    static final int FALLEN_SEQUENCE = 8;                       // the key

    // Raise Mausoleum
    private static final int RAISE_SEQUENCE = 8;
    private static final int RAISE_COST = 700;
    private static final int RAISE_MORE = 100;                  // more for every mausoleum already standing
    private static final int RAISE_COOLDOWN = 100;
    private static final double RAISE_RANGE = 12.0;

    // Corrosion
    private static final int CORROSION_SEQUENCE = 6;
    private static final int CORROSION_COST = 2450;
    private static final int CORROSION_COOLDOWN = 400;
    private static final double CORROSION_RADIUS = 10.0;
    private static final int CORROSION_TICKS = 200;
    private static final float CORROSION_TURN = 0.4f;           // every second: the chance a mob picks a random target nearby
    private static final double CORROSION_TURN_REACH = 8.0;
    static final float CORROSION_MISS = 0.2f;                   // 20% of enemy attacks go wide

    // Step
    private static final int STEP_SEQUENCE = 6;
    private static final int STEP_COST = 800;
    private static final int STEP_COOLDOWN = 80;
    private static final double STEP_RANGE = 40.0;
    private static final double STEP_AIM = 15.0;                // degrees off the crosshair a mausoleum may be to be picked
    private static final int STEP_ALLIES_SEQUENCE = 4;          // Earl of the Fallen: allies this close come along
    private static final double STEP_ALLIES_REACH = 3.0;

    // Majesty
    private static final int MAJESTY_SEQUENCE = 5;
    private static final int MAJESTY_COST = 3000;
    private static final int MAJESTY_COOLDOWN = 360;
    private static final double MAJESTY_RADIUS = 10.0;
    private static final int MAJESTY_TICKS = 30;                // 1.5 seconds...
    private static final int MAJESTY_PER_ORDERS = 20;           // ...a second more for every two standing orders
    private static final float MAJESTY_WEAK_HEALTH = 60f;       // mobs this weak stay down...
    private static final int MAJESTY_WEAK_TICKS = 120;          // ...6 seconds
    private static final int KNEEL_SLOW = 3;                    // Slowness IV while kneeling

    // Mausoleum Salvo
    private static final int SALVO_SEQUENCE = 4;
    private static final int SALVO_COST = 4000;
    private static final int SALVO_COOLDOWN = 160;
    private static final double SALVO_RANGE = 40.0;
    private static final float SALVO_DAMAGE = 35f;
    private static final double SALVO_SHARE = 8.0;              // enemies this close to the target share the beams

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("poa_fallen", "The Fallen")
            .radius(80)
            .quickCastCooldown(false).quickCastCostBy(PrinceOfAbolition::recastCost)
            .noCooldownsWhen(PrinceOfAbolition::noCooldowns)
            .entry("raise_mausoleum", PrinceOfAbolitionFx.WHEEL_RAISE_MAUSOLEUM, "Raise Mausoleum", RAISE_SEQUENCE,
                    RAISE_COST, RAISE_COOLDOWN, PrinceOfAbolitionFallen::raiseMausoleum)
            .costBy((player, cost) -> cost + RAISE_MORE * PrinceOfAbolitionMausoleums.owned(player))
            .entry("corrosion", PrinceOfAbolitionFx.WHEEL_CORROSION, "Corrosion", CORROSION_SEQUENCE,
                    CORROSION_COST, CORROSION_COOLDOWN, PrinceOfAbolitionFallen::corrosion)
            .entry("step", PrinceOfAbolitionFx.WHEEL_STEP, "Step", STEP_SEQUENCE,
                    STEP_COST, STEP_COOLDOWN, PrinceOfAbolitionFallen::step)
            .entry("majesty", PrinceOfAbolitionFx.WHEEL_MAJESTY, "Majesty", MAJESTY_SEQUENCE,
                    MAJESTY_COST, MAJESTY_COOLDOWN, PrinceOfAbolitionFallen::majesty)
            .entry("mausoleum_salvo", PrinceOfAbolitionFx.WHEEL_MAUSOLEUM_SALVO, "Mausoleum Salvo", SALVO_SEQUENCE,
                    SALVO_COST, SALVO_COOLDOWN, PrinceOfAbolitionFallen::salvo);

    // =========================
    // Ability 6 - The Fallen
    // Sneak: the Fallen wheel. Normal: the last pick again, without the wheel.
    // =========================
    public static void princeOfAbolitionFallen(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!PrinceOfAbolition.ready(player, bypassClassCheck, FALLEN_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.open(player);
        else WHEEL.quickCast(player);
    }

    // =========================
    // Raise Mausoleum: a mausoleum rises on the ground where you look (RAISE_RANGE). RAISE_COST, and RAISE_MORE more
    // for every one you already have. See PrinceOfAbolitionMausoleums for what it does.
    // =========================
    private static AbilityWheel.Result raiseMausoleum(ServerPlayer player, ServerLevel sl) {
        double range = PrinceOfAbolition.range(player, RAISE_RANGE);
        Vec3 aimed = Spells.aimGround(player, sl, range);
        Vec3 ground = aimed == null ? null : Spells.groundNear(player, sl, aimed, 2.0);
        if (ground == null || ground.distanceTo(player.position()) > range + 2.0) {
            player.sendSystemMessage(Component.literal("There is no ground there to raise a mausoleum on."));
            return AbilityWheel.Result.REFUSED;
        }
        PrinceOfAbolitionMausoleums.raise(player, sl, ground);
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Corrosion: everyone within CORROSION_RADIUS of you turns dark and greedy for CORROSION_TICKS (a mind effect).
    // Enemies lose their discipline: every second a mob picks a random target nearby CORROSION_TURN of the time (mostly
    // its own side), and CORROSION_MISS of all their attacks go wide. Your allies are not touched.
    // =========================
    private static AbilityWheel.Result corrosion(ServerPlayer player, ServerLevel sl) {
        double radius = PrinceOfAbolition.area(player, CORROSION_RADIUS);
        int ticks = PrinceOfAbolition.duration(player, CORROSION_TICKS);
        PrinceOfAbolitionFx.corrosion(sl, player, radius);
        PrinceOfAbolitionFx.status(player, Status.CORROSION, ticks);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), radius)) {
            int lasts = PrinceOfAbolitionMarks.put(player, enemy, Kind.CORRUPTED, ticks);
            if (lasts <= 0) continue;
            PrinceOfAbolitionFx.corroded(sl, enemy, lasts, PrinceOfAbolitionMarks.tag(enemy, Kind.CORRUPTED));
            if (enemy instanceof Mob mob) {
                long until = sl.getGameTime() + lasts;
                Later.run(sl, 20, () -> greed(player, sl, mob, until));
            }
        }
        return AbilityWheel.Result.DONE;
    }

    /** Once a second while Corrupted: a mob may lash out at a random one nearby, for a second. */
    private static void greed(Player player, ServerLevel sl, Mob mob, long until) {
        if (!mob.isAlive() || mob.level() != sl || sl.getGameTime() >= until || !PrinceOfAbolitionMarks.has(mob, Kind.CORRUPTED)) return;
        if (mob.getRandom().nextFloat() < CORROSION_TURN) {
            List<LivingEntity> near = new ArrayList<>();
            for (LivingEntity other : Spells.livingAround(player, sl, mob.position(), CORROSION_TURN_REACH)) {
                if (other != mob && Spells.isEnemy(player, other)) near.add(other);          // its own side
            }
            if (near.isEmpty()) near.add(player);
            LivingEntity picked = near.get(mob.getRandom().nextInt(near.size()));
            if (picked != player) Taunt.apply(mob, picked, 20);
        }
        Later.run(sl, 20, () -> greed(player, sl, mob, until));
    }

    // =========================
    // Step: look at a mausoleum (STEP_RANGE, within STEP_AIM degrees of the crosshair; with none in view, the nearest
    // one) and you vanish into a gold door and come out of that mausoleum (PrinceOfAbolitionFx.STEP_OUT). Earl of the
    // Fallen (4): allies within STEP_ALLIES_REACH come with you. Being held from teleporting stops it.
    // =========================
    private static AbilityWheel.Result step(ServerPlayer player, ServerLevel sl) {
        if (Root.has(player, RootRestriction.TELEPORT)) {
            player.sendSystemMessage(Component.literal("You are held fast; no door opens for you."));
            return AbilityWheel.Result.REFUSED;
        }
        Mausoleum to = PrinceOfAbolitionMausoleums.aimed(player, PrinceOfAbolition.range(player, STEP_RANGE), STEP_AIM);
        if (to == null) to = PrinceOfAbolitionMausoleums.nearest(player, player.position());
        if (to == null) {
            player.sendSystemMessage(Component.literal("You have no mausoleum standing to step to."));
            return AbilityWheel.Result.REFUSED;
        }
        Vec3 door = to.door();
        Vec3 out = Spells.fits(player, sl, door) ? door : Spells.freeSpotNear(player, sl, to.ground, door.subtract(to.ground), 1.6);
        if (out == null) {
            player.sendSystemMessage(Component.literal("There is no room before that mausoleum's door."));
            return AbilityWheel.Result.REFUSED;
        }
        Vec3 from = player.position();
        List<LivingEntity> company = new ArrayList<>();
        if (Beyonder.hasSequence(player, STEP_ALLIES_SEQUENCE)) {
            company.addAll(Spells.alliesAround(player, sl, from, PrinceOfAbolition.area(player, STEP_ALLIES_REACH)));
        }
        PrinceOfAbolitionFx.step(sl, player, from, out);
        Concealment.hide(player, PrinceOfAbolitionFx.STEP_OUT + 2);
        Vec3 there = out;
        Later.run(sl, PrinceOfAbolitionFx.STEP_OUT, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            player.teleportTo(there.x, there.y, there.z);
            player.fallDistance = 0;
            Concealment.reveal(player);
            int i = 0;
            for (LivingEntity ally : company) {
                if (!ally.isAlive() || ally.level() != sl) continue;
                Vec3 beside = Spells.spotAround(there, player.getYRot() + 90f + 60f * i++, 1.2);
                ally.teleportTo(beside.x, there.y, beside.z);
                ally.fallDistance = 0;
            }
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Majesty: your presence presses down on everyone within MAJESTY_RADIUS. Enemies kneel (no jumping, sprinting or
    // attacking, Slowness IV) for MAJESTY_TICKS, plus MAJESTY_PER_ORDERS for every two standing orders; mobs of up to
    // MAJESTY_WEAK_HEALTH stay down MAJESTY_WEAK_TICKS. A mind effect: Iron Will stands up to it.
    // =========================
    private static AbilityWheel.Result majesty(ServerPlayer player, ServerLevel sl) {
        double radius = PrinceOfAbolition.area(player, MAJESTY_RADIUS);
        int orders = PrinceOfAbolitionOrders.count(player);
        int ticks = MAJESTY_TICKS + MAJESTY_PER_ORDERS * (orders / 2);
        PrinceOfAbolitionFx.majesty(sl, player, radius);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), radius)) {
            boolean weak = !(enemy instanceof Player) && enemy.getMaxHealth() <= MAJESTY_WEAK_HEALTH;
            kneel(player, sl, enemy, PrinceOfAbolition.duration(player, weak ? Math.max(ticks, MAJESTY_WEAK_TICKS) : ticks));
        }
        return AbilityWheel.Result.DONE;
    }

    /** One enemy kneels for 'ticks' (Majesty, and the Majesty pulse of a Rebirth). */
    static void kneel(Player player, ServerLevel sl, LivingEntity enemy, int ticks) {
        int lasts = PrinceOfAbolitionMarks.put(player, enemy, Kind.KNEELING, ticks);
        if (lasts <= 0) return;
        Root.apply(enemy, lasts, RootRestriction.JUMP, RootRestriction.ATTACK);
        Spells.keepEffect(enemy, MobEffects.SLOWNESS, KNEEL_SLOW, lasts);
        enemy.setSprinting(false);
        PrinceOfAbolitionFx.kneel(sl, enemy, lasts, PrinceOfAbolitionMarks.tag(enemy, Kind.KNEELING));
    }

    // =========================
    // Mausoleum Salvo (a direct hit): every standing mausoleum fires a gold beam at the enemy you look at (SALVO_RANGE),
    // SALVO_DAMAGE each, PrinceOfAbolitionFx.SALVO_STEP apart. Enemies within SALVO_SHARE of the target share the beams
    // with it, one each in turn. With no mausoleum standing it does nothing and costs nothing.
    // =========================
    private static AbilityWheel.Result salvo(ServerPlayer player, ServerLevel sl) {
        List<Mausoleum> standing = PrinceOfAbolitionMausoleums.standing(player);
        if (standing.isEmpty()) {
            player.sendSystemMessage(Component.literal("You have no mausoleum standing to fire."));
            return AbilityWheel.Result.REFUSED;
        }
        LivingEntity target = PrinceOfAbolition.aim(player, sl, SALVO_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one in sight for your mausoleums to strike."));
            return AbilityWheel.Result.REFUSED;
        }
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(target);
        for (LivingEntity near : Spells.enemiesAround(player, sl, target.position(), PrinceOfAbolition.area(player, SALVO_SHARE))) {
            if (near != target) targets.add(near);
        }
        for (int i = 0; i < standing.size(); i++) {
            Mausoleum mausoleum = standing.get(i);
            LivingEntity victim = targets.get(i % targets.size());
            Later.run(sl, i * PrinceOfAbolitionFx.SALVO_STEP, () -> {
                if (!Spells.casterStillHere(player, sl) || !victim.isAlive() || victim.level() != sl) return;
                PrinceOfAbolitionFx.salvo(sl, List.of(mausoleum.top()), victim);
                Spells.strike(player, victim, SALVO_DAMAGE);
                PrinceOfAbolitionFx.salvoHit(sl, victim);
            });
        }
        return AbilityWheel.Result.DONE;
    }
}
