package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionFx.Feeling;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionFx.Status;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionMarks.Kind;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionMausoleums.Mausoleum;
import net.lukario.circleofinevitability.combat.Countdown;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Corrosion;
import net.lukario.circleofinevitability.status.DeathWard;
import net.lukario.circleofinevitability.status.Exile;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.Flight;
import net.lukario.circleofinevitability.status.Glide;
import net.lukario.circleofinevitability.status.Substitute;
import net.lukario.circleofinevitability.status.Swarm;
import net.lukario.circleofinevitability.status.Taunt;
import net.lukario.circleofinevitability.status.Unnoticed;
import net.lukario.circleofinevitability.status.Vulnerable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Prince Of Abolition, key 6: the Fallen wheel (Barbarian, Sequence 8). docs/PRINCE_OF_ABOLITION.md, "Key 6 wheel".
 *
 *   Raise Mausoleum   (8) a mausoleum rises where you look (PrinceOfAbolitionMausoleums: 2 minutes, +5% attack and
 *                         defense, Sanctuary, and it brings you back once)
 *   Corrosion         (6) everyone round you turns dark and greedy: enemies lose their discipline, and a rot sets in
 *                         that eats deeper every second until it is cleansed (status/Corrosion)
 *   Step              (6) you step through a mausoleum: in a gold door here, out of the mausoleum there
 *   Majesty           (5) your presence presses down: enemies kneel, longer for every two standing orders
 *   Bestowment        (4) a feeling picked from the target: Sluggish, Anxious, Defeated, Rash or Avaricious
 *   Exploit           (4) a state exploited: hang in the air, strip an enemy's states, or stretch your own
 *   Magnify           (4) the next blow or ability is magnified; a Connected enemy in view is seized
 *   Mausoleum Salvo   (4) every standing mausoleum fires a gold beam at the enemy you look at (a direct hit)
 *   Mausoleum Blast   (4) every mausoleum near you goes off: heavy damage round each one, and they are gone
 *
 * Sneak opens the wheel; a normal press casts the last pick again (PrinceOfAbolition.recastCost). The mausoleums are
 * the Black Emperor's own: a thief casting this key gets whatever entry comes up, and Step, Salvo and Blast need
 * mausoleums. Exploit and Magnify cost nothing inside your own Entropy field (PrinceOfAbolitionSovereign).
 *
 * A new entry is one .entry(...) line in WHEEL, its settings below and its method; give it a WHEEL_ picture in
 * PrinceOfAbolitionFx (tools/model_gen/prince_of_abolition/icons.py draws them).
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
    private static final int CORROSION_TICKS = 200;             // the greed (a mind effect) lasts this long...
    private static final float CORROSION_TURN = 0.4f;           // ...every second: the chance a mob picks a random target nearby
    private static final double CORROSION_TURN_REACH = 8.0;
    static final float CORROSION_MISS = 0.2f;                   // ...and 20% of their attacks go wide
    static final float CORROSION_ROT = 2f;                      // the rot: 2 a second at first...
    static final float CORROSION_ROT_GROWTH = 1f;               // ...1 more every second, until cleansed (no healing after 5 s)

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
    static final int MAJESTY_TICKS = 30;                        // 1.5 seconds...
    static final int MAJESTY_PER_ORDERS = 20;                   // ...a second more for every two standing orders
    private static final float MAJESTY_WEAK_HEALTH = 60f;       // mobs this weak stay down...
    private static final int MAJESTY_WEAK_TICKS = 120;          // ...6 seconds
    private static final int KNEEL_SLOW = 3;                    // Slowness IV while kneeling

    // Bestowment
    private static final int BESTOW_SEQUENCE = 4;
    private static final int BESTOW_COST = 6000;
    private static final int BESTOW_COOLDOWN = 240;
    private static final double BESTOW_RANGE = 16.0;
    private static final int BESTOW_TICKS = 200;
    private static final float DEFEATED_HEALTH = 0.4f;          // under 40% of its health: Defeated...
    static final float DEFEATED_CUT = 0.6f;                     // ...its damage cut by 60%, a mob drops its target
    static final float ANXIOUS_MISS = 0.4f;                     // archers and casters: Anxious, 40% of their attacks go wide
    private static final double FAST_SPEED = 0.3;               // a mob this fast (movement speed): Sluggish
    private static final float BRUTE_HEALTH = 40f;              // this much health...
    private static final double SLOW_SPEED = 0.26;              // ...and slower than this: a brute, Rash
    private static final float RASH_EXTRA = 0.4f;               // Rash: takes 40% more...
    private static final int RASH_HASTE = 1;                    // ...attacks faster (Haste II)...
    private static final double RASH_SHOVE = 0.35;              // ...and cannot stop moving forward
    static final float AVARICE_CUT = 0.3f;                      // an Avaricious player deals 30% less
    private static final double AVARICE_REACH = 16.0;           // an Avaricious mob runs after valuables this close

    // Exploit
    private static final int EXPLOIT_SEQUENCE = 4;
    private static final int EXPLOIT_COST = 4000;
    private static final int EXPLOIT_COOLDOWN = 300;
    private static final int GLIDE_TICKS = 160;                 // in the air: flight for 8 seconds
    private static final double STRIP_RANGE = 24.0;             // at an enemy: its states end
    private static final int STRETCH_MOST = 600;                // on yourself: what runs is doubled, by 30 s at most...
    private static final int STRETCH_MAUSOLEUMS_MOST = 2400;    // ...and every mausoleum by 2 minutes at most
    private static final int MOON_SEQUENCE = 2;                 // Duke of Entropy, in the air in your own Entropy field:
    private static final double MOON_UP = 2.6;                  // a leap to the Moon
    private static final double MOON_FORWARD = 2.2;

    // Magnify
    private static final int MAGNIFY_SEQUENCE = 4;
    private static final int MAGNIFY_COST = 5000;
    private static final int MAGNIFY_COOLDOWN = 400;
    private static final int MAGNIFY_TICKS = 200;               // the next blow or ability within 10 seconds...
    private static final float MAGNIFY_DAMAGE = 2.5f;           // ...does 2.5 times the damage...
    static final double MAGNIFY_REACH = 1.5;                    // ...reaches 1.5 times as far and wide...
    private static final float MAGNIFY_EXECUTE = 0.25f;         // ...and a non-boss left under 25% dies
    static final double MAGNIFIED_BRIBE_RADIUS = 6.0;           // a magnified Bribe lands as a quake this wide
    private static final double SEIZE_RANGE = 100.0;            // a Connected enemy in view this far...
    private static final double SEIZE_PULL = 10.0;              // ...is pulled this far toward you...
    private static final int SEIZE_HOLD = 40;                   // ...and held 2 seconds

    // Mausoleum Salvo
    private static final int SALVO_SEQUENCE = 4;
    private static final int SALVO_COST = 4000;
    private static final int SALVO_COOLDOWN = 160;
    private static final double SALVO_RANGE = 40.0;
    private static final float SALVO_DAMAGE = 35f;
    private static final double SALVO_SHARE = 8.0;              // enemies this close to the target share the beams

    // Mausoleum Blast
    private static final int BLAST_SEQUENCE = 4;
    private static final int BLAST_COST = 5000;
    private static final int BLAST_COOLDOWN = 200;
    private static final double BLAST_REACH = 24.0;             // every mausoleum this close to you goes off...
    private static final double BLAST_RADIUS = 5.0;             // ...hitting every enemy this close to it...
    private static final float BLAST_DAMAGE = 90f;              // ...this hard (between two of them: both)
    private static final double BLAST_THROW = 1.2;

    // Saved on the player
    private static final String MAGNIFY_UNTIL = "poa_magnify_until";
    private static final String MAGNIFY_TAG = "poa_magnify_tag";
    private static final String MAGNIFY_SPENT = "poa_magnify_spent";     // the game tick it was spent (hits that tick all count)

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("poa_fallen", "The Fallen")
            .radius(88)
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
            .entry("bestowment", PrinceOfAbolitionFx.WHEEL_BESTOWMENT, "Bestowment", BESTOW_SEQUENCE,
                    BESTOW_COST, BESTOW_COOLDOWN, PrinceOfAbolitionFallen::bestowment)
            .entry("exploit", PrinceOfAbolitionFx.WHEEL_EXPLOIT, "Exploit", EXPLOIT_SEQUENCE,
                    EXPLOIT_COST, EXPLOIT_COOLDOWN, PrinceOfAbolitionFallen::exploit)
            .free(PrinceOfAbolitionSovereign::inOwnEntropy)
            .entry("magnify", PrinceOfAbolitionFx.WHEEL_MAGNIFY, "Magnify", MAGNIFY_SEQUENCE,
                    MAGNIFY_COST, MAGNIFY_COOLDOWN, PrinceOfAbolitionFallen::magnify)
            .free(PrinceOfAbolitionSovereign::inOwnEntropy)
            .entry("mausoleum_salvo", PrinceOfAbolitionFx.WHEEL_MAUSOLEUM_SALVO, "Mausoleum Salvo", SALVO_SEQUENCE,
                    SALVO_COST, SALVO_COOLDOWN, PrinceOfAbolitionFallen::salvo)
            .entry("mausoleum_blast", PrinceOfAbolitionFx.WHEEL_MAUSOLEUM_BLAST, "Mausoleum Blast", BLAST_SEQUENCE,
                    BLAST_COST, BLAST_COOLDOWN, PrinceOfAbolitionFallen::blast);

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
    // Corrosion: everyone within CORROSION_RADIUS of you turns dark and greedy. Two parts:
    //   - the greed (a mind effect, CORROSION_TICKS): every second a mob picks a random target nearby CORROSION_TURN
    //     of the time (mostly its own side), and CORROSION_MISS of all their attacks go wide;
    //   - the rot (status/Corrosion, not a mind effect): CORROSION_ROT a second, CORROSION_ROT_GROWTH more every
    //     second, until it is cleansed (a cleanse ability, a milk bucket) or the bearer dies; after 5 seconds of it
    //     they cannot heal at all.
    // Your allies are not touched.
    // =========================
    private static AbilityWheel.Result corrosion(ServerPlayer player, ServerLevel sl) {
        double radius = PrinceOfAbolition.area(player, CORROSION_RADIUS);
        int ticks = PrinceOfAbolition.duration(player, CORROSION_TICKS);
        PrinceOfAbolitionFx.corrosion(sl, player, radius);
        PrinceOfAbolitionFx.status(player, Status.CORROSION, ticks);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), radius)) {
            Corrosion.apply(enemy, player, CORROSION_ROT, CORROSION_ROT_GROWTH);
            int lasts = PrinceOfAbolitionMarks.put(player, enemy, Kind.CORRUPTED, ticks);
            if (lasts > 0 && enemy instanceof Mob mob) {
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
        PrinceOfAbolitionFx.majesty(sl, player, radius);
        majestyPulse(player, sl, radius);
        return AbilityWheel.Result.DONE;
    }

    /** Every enemy within 'radius' kneels as Majesty makes them (Majesty, Coronation's pulses). */
    static void majestyPulse(Player player, ServerLevel sl, double radius) {
        int ticks = MAJESTY_TICKS + MAJESTY_PER_ORDERS * (PrinceOfAbolitionOrders.count(player) / 2);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), radius)) {
            boolean weak = !(enemy instanceof Player) && enemy.getMaxHealth() <= MAJESTY_WEAK_HEALTH;
            kneel(player, sl, enemy, PrinceOfAbolition.duration(player, weak ? Math.max(ticks, MAJESTY_WEAK_TICKS) : ticks));
        }
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
    // Bestowment: a feeling for the enemy you look at (BESTOW_RANGE) for BESTOW_TICKS, picked from what it is
    // (feelingFor). A mind effect: Iron Will stands up to it.
    //   Defeated    under DEFEATED_HEALTH of its health: its damage cut by DEFEATED_CUT; a mob drops its target and
    //               will not pick another
    //   Anxious     archers and casters (a mob that shoots, a player holding a bow): ANXIOUS_MISS of its attacks go
    //               wide, and it is slowed
    //   Sluggish    fast things (a mob of FAST_SPEED, a sprinting player): Slowness II and slow attacks (Mining
    //               Fatigue II)
    //   Rash        slow brutes (BRUTE_HEALTH and slower than SLOW_SPEED): takes RASH_EXTRA more, attacks faster
    //               (Haste II), and is shoved forward every second
    //   Avaricious  everything else: a mob drops its target, will not pick another and runs after the nearest gold,
    //               emerald or diamond lying within AVARICE_REACH; a player deals AVARICE_CUT less
    // =========================
    private static AbilityWheel.Result bestowment(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = PrinceOfAbolition.aim(player, sl, BESTOW_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one before you to give a feeling to."));
            return AbilityWheel.Result.REFUSED;
        }
        Feeling feeling = feelingFor(target);
        int ticks = PrinceOfAbolition.duration(player, BESTOW_TICKS);
        Kind kind = kindOf(feeling);
        PrinceOfAbolitionFx.bestowment(sl, player, target, feeling, ticks, PrinceOfAbolitionMarks.tag(target, kind));
        Later.run(sl, PrinceOfAbolitionFx.BESTOW_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            bestow(player, sl, target, feeling, ticks);
        });
        return AbilityWheel.Result.DONE;
    }

    /** The feeling Bestowment gives this target. */
    static Feeling feelingFor(LivingEntity target) {
        if (target.getHealth() < target.getMaxHealth() * DEFEATED_HEALTH) return Feeling.DEFEATED;
        if (target instanceof RangedAttackMob) return Feeling.ANXIOUS;
        if (target instanceof Player player) {
            ItemStack held = player.getMainHandItem();
            if (held.is(Items.BOW) || held.is(Items.CROSSBOW)) return Feeling.ANXIOUS;
            if (player.isSprinting()) return Feeling.SLUGGISH;
            return player.getMaxHealth() >= BRUTE_HEALTH ? Feeling.RASH : Feeling.AVARICIOUS;
        }
        double speed = target.getAttribute(Attributes.MOVEMENT_SPEED) != null
                ? target.getAttribute(Attributes.MOVEMENT_SPEED).getValue() : 0.0;
        if (speed >= FAST_SPEED) return Feeling.SLUGGISH;
        if (target.getMaxHealth() >= BRUTE_HEALTH && speed < SLOW_SPEED) return Feeling.RASH;
        return Feeling.AVARICIOUS;
    }

    static Kind kindOf(Feeling feeling) {
        return switch (feeling) {
            case SLUGGISH -> Kind.SLUGGISH;
            case ANXIOUS -> Kind.ANXIOUS;
            case DEFEATED -> Kind.DEFEATED;
            case RASH -> Kind.RASH;
            case AVARICIOUS -> Kind.AVARICIOUS;
        };
    }

    /** The feeling takes (when Iron Will does not stop it). */
    private static void bestow(Player player, ServerLevel sl, LivingEntity target, Feeling feeling, int ticks) {
        int lasts = PrinceOfAbolitionMarks.put(player, target, kindOf(feeling), ticks);
        if (lasts <= 0) return;
        switch (feeling) {
            case SLUGGISH -> {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, lasts, 1));
                target.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, lasts, 1));
            }
            case ANXIOUS -> {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, lasts, 0));
                target.setSprinting(false);
            }
            case DEFEATED -> {
                if (target instanceof Mob mob) mob.setTarget(null);
            }
            case RASH -> {
                Vulnerable.add(target, RASH_EXTRA, RASH_EXTRA, lasts);
                target.addEffect(new MobEffectInstance(MobEffects.HASTE, lasts, RASH_HASTE));
                Later.run(sl, 20, () -> feelingBeat(sl, target, Kind.RASH));
            }
            case AVARICIOUS -> {
                if (target instanceof Mob mob) {
                    mob.setTarget(null);
                    Later.run(sl, 1, () -> feelingBeat(sl, target, Kind.AVARICIOUS));
                }
            }
        }
    }

    /** Once a second while it lasts: Rash is shoved forward, Avaricious runs after valuables. */
    private static void feelingBeat(ServerLevel sl, LivingEntity target, Kind kind) {
        if (!target.isAlive() || target.level() != sl || !PrinceOfAbolitionMarks.has(target, kind)) return;
        if (kind == Kind.RASH) {
            Vec3 ahead = target.getLookAngle().multiply(1, 0, 1);
            if (ahead.lengthSqr() > 1.0E-4) Spells.push(target, target.position().subtract(ahead.normalize()), RASH_SHOVE, 0.05);
        } else if (target instanceof Mob mob) {
            ItemEntity valuable = nearestValuable(sl, mob);
            if (valuable != null) {
                mob.setTarget(null);
                mob.getNavigation().moveTo(valuable.getX(), valuable.getY(), valuable.getZ(), 1.2);
            }
        }
        Later.run(sl, 20, () -> feelingBeat(sl, target, kind));
    }

    private static ItemEntity nearestValuable(ServerLevel sl, Mob mob) {
        AABB box = mob.getBoundingBox().inflate(AVARICE_REACH);
        ItemEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ItemEntity item : sl.getEntitiesOfClass(ItemEntity.class, box, Entity::isAlive)) {
            ItemStack stack = item.getItem();
            if (!(stack.is(Items.GOLD_INGOT) || stack.is(Items.GOLD_NUGGET) || stack.is(Items.EMERALD) || stack.is(Items.DIAMOND))) continue;
            double distance = item.distanceToSqr(mob);
            if (distance < bestDistance) {
                best = item;
                bestDistance = distance;
            }
        }
        return best;
    }

    // =========================
    // Exploit: a state exploited, picked by what you do it to.
    //   In the air:      the state of being off the ground is stretched: flight for GLIDE_TICKS (hold jump to rise).
    //                    Duke of Entropy (2), inside your own Entropy field: a leap to the Moon instead, a great arc
    //                    with no fall damage at the end.
    //   At an enemy:     its states end ahead of time (STRIP_RANGE): good potion effects, a Substitute, a death ward,
    //                    hiding, flight and gliding, a Swarm, a countdown it is running.
    //   Otherwise:       what runs on you is doubled, by STRETCH_MOST at most each: good potion effects, hiding,
    //                    flight; and every standing mausoleum by STRETCH_MAUSOLEUMS_MOST at most.
    // Free inside your own Entropy field.
    // =========================
    private static AbilityWheel.Result exploit(ServerPlayer player, ServerLevel sl) {
        boolean inAir = !player.onGround() && !player.isInWater() && !player.isPassenger();
        if (inAir) {
            if (Beyonder.hasSequence(player, MOON_SEQUENCE) && PrinceOfAbolitionSovereign.inOwnEntropy(player)) moonLeap(player, sl);
            else glide(player, sl);
            return AbilityWheel.Result.DONE;
        }
        LivingEntity target = PrinceOfAbolition.aim(player, sl, STRIP_RANGE);
        if (target != null) strip(player, sl, target);
        else stretch(player, sl);
        return AbilityWheel.Result.DONE;
    }

    private static void glide(ServerPlayer player, ServerLevel sl) {
        String tag = "poa_glide_" + player.getId() + "_" + sl.getGameTime();
        Flight.grant(player, GLIDE_TICKS);
        PrinceOfAbolitionFx.exploitGlide(sl, player, GLIDE_TICKS, tag);
        PrinceOfAbolitionFx.status(player, Status.EXPLOIT, GLIDE_TICKS);
        Later.run(sl, GLIDE_TICKS, () -> PrinceOfAbolitionFx.exploitGlideEnd(sl, player, tag));
    }

    private static void moonLeap(ServerPlayer player, ServerLevel sl) {
        Vec3 ahead = Spells.flatLook(player);
        player.setDeltaMovement(ahead.x * MOON_FORWARD, MOON_UP, ahead.z * MOON_FORWARD);
        player.hurtMarked = true;
        player.fallDistance = 0;
        FallGuard.protect(player, 600);
        PrinceOfAbolitionFx.moonLeap(sl, player);
    }

    private static void strip(ServerPlayer player, ServerLevel sl, LivingEntity target) {
        for (MobEffectInstance effect : new ArrayList<>(target.getActiveEffects())) {
            if (effect.getEffect().value().isBeneficial()) target.removeEffect(effect.getEffect());
        }
        if (Substitute.isArmed(target)) Substitute.disarm(target);
        if (DeathWard.isArmed(target)) DeathWard.disarm(target);
        if (Concealment.isHidden(target) && !Exile.isExiled(target)) Concealment.reveal(target);
        if (Unnoticed.isUnnoticed(target)) Unnoticed.end(target);
        if (Swarm.isActive(target)) Swarm.end(target);
        if (target instanceof Player victim) {
            if (Flight.has(victim)) Flight.end(victim);
            if (Glide.isGliding(victim)) Glide.end(victim);
            if (Countdown.isRunning(victim)) Countdown.cancel(victim);
        }
        PrinceOfAbolitionFx.exploitStrip(sl, player, target);
    }

    private static void stretch(ServerPlayer player, ServerLevel sl) {
        for (MobEffectInstance effect : new ArrayList<>(player.getActiveEffects())) {
            if (!effect.getEffect().value().isBeneficial() || effect.isInfiniteDuration()) continue;
            int left = effect.getDuration();
            player.addEffect(new MobEffectInstance(effect.getEffect(), left + Math.min(left, STRETCH_MOST), effect.getAmplifier()));
        }
        if (Concealment.isHidden(player) && !Exile.isExiled(player)) {
            int left = Concealment.ticksLeft(player);
            Concealment.hide(player, left + Math.min(left, STRETCH_MOST));
        }
        if (Flight.has(player)) {
            int left = Flight.ticksLeft(player);
            Flight.grant(player, left + Math.min(left, STRETCH_MOST));
        }
        PrinceOfAbolitionSovereign.stretchCoronation(player, STRETCH_MOST);
        PrinceOfAbolitionMausoleums.stretchAll(player, STRETCH_MAUSOLEUMS_MOST);
        PrinceOfAbolitionFx.exploitExtend(sl, player);
    }

    // =========================
    // Magnify: for MAGNIFY_TICKS your next blow or ability is magnified. While it waits your abilities reach
    // MAGNIFY_REACH times as far and as wide (PrinceOfAbolition.range, area); the first damage you deal spends it
    // (every hit of that same tick counts, so a beam or a blast is magnified whole): MAGNIFY_DAMAGE times the damage,
    // and a non-boss left under MAGNIFY_EXECUTE of its health dies on the spot. A magnified Bribe travels as a thrown
    // flower and lands as a quake (PrinceOfAbolitionBribes). Cast with a Connected enemy in view (SEIZE_RANGE), it is
    // seized at once: pulled SEIZE_PULL toward you and held SEIZE_HOLD. Free inside your own Entropy field.
    // =========================
    private static AbilityWheel.Result magnify(ServerPlayer player, ServerLevel sl) {
        LivingEntity far = Spells.aimEnemy(player, sl, PrinceOfAbolition.range(player, SEIZE_RANGE));
        if (far != null && PrinceOfAbolitionMarks.from(far, Kind.CONNECTED, player)) seize(player, sl, far);

        CompoundTag data = player.getPersistentData();
        PrinceOfAbolitionFx.magnifyEnd(sl, player, data.getStringOr(MAGNIFY_TAG, ""));      // cast again: one glow
        long until = sl.getGameTime() + MAGNIFY_TICKS;
        String tag = "poa_magnify_" + player.getId() + "_" + sl.getGameTime();
        data.putLong(MAGNIFY_UNTIL, until);
        data.putString(MAGNIFY_TAG, tag);
        PrinceOfAbolitionFx.magnify(sl, player, MAGNIFY_TICKS, tag);
        PrinceOfAbolitionFx.status(player, Status.MAGNIFY, MAGNIFY_TICKS);
        Later.run(sl, MAGNIFY_TICKS, () -> {
            if (player.getPersistentData().getLongOr(MAGNIFY_UNTIL, 0L) != until) return;
            endMagnify(player, sl);
        });
        return AbilityWheel.Result.DONE;
    }

    private static void seize(ServerPlayer player, ServerLevel sl, LivingEntity target) {
        PrinceOfAbolitionFx.seize(sl, player, target);
        Vec3 toward = player.position().subtract(target.position());
        double pull = Math.min(PrinceOfAbolition.range(player, SEIZE_PULL), toward.length() - 2.0);
        if (pull > 0.5 && !Root.has(target, RootRestriction.TELEPORT)) {
            Vec3 spot = target.position().add(toward.normalize().scale(pull));
            Vec3 ground = Spells.groundNear(player, sl, spot, 3.0);
            if (ground != null) {
                target.teleportTo(ground.x, ground.y, ground.z);
                target.fallDistance = 0;
            }
        }
        Root.apply(target, PrinceOfAbolition.duration(player, SEIZE_HOLD), RootRestriction.MOVEMENT);
        PrinceOfAbolitionFx.magnifiedHit(sl, target.position().add(0, target.getBbHeight() * 0.6, 0));
    }

    /** True while Magnify waits for its blow. */
    static boolean isMagnified(Player player) {
        return PrinceOfAbolition.isActive(player, MAGNIFY_UNTIL);
    }

    /** Magnify is spent now (by a hit, or by a Bribe). False if it was not waiting. */
    static boolean spendMagnify(Player player, ServerLevel sl) {
        if (!isMagnified(player)) return false;
        endMagnify(player, sl);
        player.getPersistentData().putLong(MAGNIFY_SPENT, sl.getGameTime());
        return true;
    }

    private static void endMagnify(Player player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        PrinceOfAbolitionFx.magnifyEnd(sl, player, data.getStringOr(MAGNIFY_TAG, ""));
        data.remove(MAGNIFY_UNTIL);
        data.remove(MAGNIFY_TAG);
        PrinceOfAbolitionFx.statusEnd(player, Status.MAGNIFY);
    }

    /**
     * Called by the hurt event for every hit the player deals: a magnified one (the first while it waits, and every
     * other hit of that same tick) does MAGNIFY_DAMAGE times as much, and leaves no non-boss under MAGNIFY_EXECUTE.
     */
    static float magnifyHit(Player player, LivingEntity victim, float amount) {
        if (!(player.level() instanceof ServerLevel sl)) return amount;
        boolean sameTick = player.getPersistentData().getLongOr(MAGNIFY_SPENT, -1L) == sl.getGameTime();
        if (!sameTick && !spendMagnify(player, sl)) return amount;
        float magnified = amount * MAGNIFY_DAMAGE;
        PrinceOfAbolitionFx.magnifiedHit(sl, victim.position().add(0, victim.getBbHeight() * 0.6, 0));
        if (victim.getMaxHealth() <= PrinceOfAbolition.BOSS_HEALTH
                && victim.getHealth() - magnified < victim.getMaxHealth() * MAGNIFY_EXECUTE) {
            magnified = Math.max(magnified, (victim.getHealth() + victim.getAbsorptionAmount()) * 20f + 100f);   // through any armor
        }
        return magnified;
    }

    /** After a relog or a death: nothing waits. */
    static void clearState(Player player) {
        player.getPersistentData().remove(MAGNIFY_UNTIL);
        player.getPersistentData().remove(MAGNIFY_TAG);
        player.getPersistentData().remove(MAGNIFY_SPENT);
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

    // =========================
    // Mausoleum Blast: every standing mausoleum within BLAST_REACH of you goes off. Its seams glow, and at
    // PrinceOfAbolitionFx.BLAST_LANDS it bursts: BLAST_DAMAGE to every enemy within BLAST_RADIUS of it, thrown away
    // from it (an enemy caught between two takes both). The mausoleums are gone, orders and all, so Rebirth has less
    // to rise from. With none near it does nothing and costs nothing.
    // =========================
    private static AbilityWheel.Result blast(ServerPlayer player, ServerLevel sl) {
        double reach = PrinceOfAbolition.range(player, BLAST_REACH);
        List<Mausoleum> near = new ArrayList<>();
        for (Mausoleum mausoleum : PrinceOfAbolitionMausoleums.standing(player)) {
            if (mausoleum.ground.distanceTo(player.position()) <= reach) near.add(mausoleum);
        }
        if (near.isEmpty()) {
            player.sendSystemMessage(Component.literal("You have no mausoleum near enough to set off."));
            return AbilityWheel.Result.REFUSED;
        }
        double radius = PrinceOfAbolition.area(player, BLAST_RADIUS);
        for (Mausoleum mausoleum : near) {
            PrinceOfAbolitionFx.mausoleumBlast(sl, mausoleum.ground, mausoleum.yaw, radius, mausoleum.tag);
            Later.run(sl, PrinceOfAbolitionFx.BLAST_LANDS, () -> {
                if (!PrinceOfAbolitionMausoleums.detonate(mausoleum) || !Spells.casterStillHere(player, sl)) return;
                Vec3 middle = mausoleum.ground.add(0, 1.0, 0);
                for (LivingEntity enemy : Spells.enemiesAround(player, sl, middle, radius)) {
                    Spells.strike(player, enemy, BLAST_DAMAGE);
                    if (enemy.isAlive()) Spells.push(enemy, mausoleum.ground, BLAST_THROW, 0.5);
                }
            });
        }
        return AbilityWheel.Result.DONE;
    }
}
