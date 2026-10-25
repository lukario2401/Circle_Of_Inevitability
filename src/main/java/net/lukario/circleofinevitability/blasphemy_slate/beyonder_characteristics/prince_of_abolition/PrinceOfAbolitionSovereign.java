package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionFx.Status;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Corrosion;
import net.lukario.circleofinevitability.status.Exile;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Prince Of Abolition, key 7: the Sovereign wheel (Baron of Corruption, Sequence 6). docs/PRINCE_OF_ABOLITION.md,
 * "Key 7 wheel: Sovereign".
 *
 *   Ruin        (6) a black-and-gold beam along a line: every enemy on it is struck, and a rot sets in that eats
 *                   deeper every second until it is cleansed (status/Corrosion; a direct hit)
 *   Frenzy      (3) twelve black bolts, each Distorted onto a different enemy round you; each Disorders what it hits
 *   Entropy     (2) order comes undone in a field where you aim: enemies stumble, their shots fly wide and their
 *                   damage swings; Exploit and Magnify cost you nothing in it
 *   Abolition   (1) a sphere round you slips out of the real world: nothing crosses its shell either way
 *   Exile       (1) one thing is thrown out of the world for a while: an enemy, an ally (saved), or yourself
 *   Inversion   (0) everything round you turns into its opposite: their blows heal you, their healing hurts them
 *   Coronation  (0) the mausoleums crown you: their bonus doubles, Majesty pulses out of you, you stand unmoved
 *   Decree      (0) one word: damage nothing stops, and a non-boss left low dies (a direct hit)
 *
 * The questions the events ask (PrinceOfAbolition): inOwnEntropy, entropySwing, separated (Abolition),
 * shelteredFromZone (Black Emperor's Abolition), inverterGuarding / inverterOver (Inversion), isCrowned.
 *
 * Sneak opens the wheel; a normal press casts the last pick again (PrinceOfAbolition.recastCost). A new entry is one
 * .entry(...) line in WHEEL, its settings below and its method.
 */
public final class PrinceOfAbolitionSovereign {

    private PrinceOfAbolitionSovereign() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks; base values before Might)
    // =========================

    static final int SOVEREIGN_SEQUENCE = 6;                    // the key

    // Ruin
    static final int RUIN_SEQUENCE = 6;
    static final int RUIN_COST = 2800;
    static final int RUIN_COOLDOWN = 200;
    static final double RUIN_RANGE = 24.0;
    static final double RUIN_WIDTH = 0.6;               // how far from the line a body is still struck
    static final float RUIN_DAMAGE = 70f;
    static final float RUIN_ROT = 5f;                   // the rot: 5 a second at first...
    static final float RUIN_ROT_GROWTH = 1.5f;          // ...1.5 more every second, until cleansed (no healing after 5 s)

    // Frenzy
    static final int FRENZY_SEQUENCE = 3;
    static final int FRENZY_COST = 12000;
    static final int FRENZY_COOLDOWN = 200;
    static final double FRENZY_RANGE = 24.0;
    static final int FRENZY_BOLTS = 12;
    static final float FRENZY_DAMAGE = 25f;
    static final int FRENZY_DISORDER = 40;

    // Entropy
    static final int ENTROPY_SEQUENCE = 2;
    static final int ENTROPY_COST = 20000;
    static final int ENTROPY_COOLDOWN = 400;
    static final double ENTROPY_RANGE = 24.0;
    static final double ENTROPY_RADIUS = 16.0;
    static final int ENTROPY_TICKS = 400;
    static final int ENTROPY_FULL_AFTER = 160;          // it grows to full strength over 8 seconds
    static final float ENTROPY_SHOVE_CHANCE = 0.3f;     // twice a second at full strength: the chance of a shove...
    static final double ENTROPY_SHOVE = 0.6;            // ...this hard
    static final double ENTROPY_SCATTER = 30.0;         // projectiles in it turn up to this many degrees off
    static final float ENTROPY_SWING = 0.5f;            // enemies' damage swings between 0.5 and 1.5 times
    static final String ENTROPY_ZONE = "poa_entropy";

    // Abolition
    static final int ABOLITION_SEQUENCE = 1;
    static final int ABOLITION_COST = 30000;
    static final int ABOLITION_COOLDOWN = 600;
    static final double ABOLITION_RADIUS = 12.0;
    static final int ABOLITION_TICKS = 300;
    static final int ABOLITION_FIELDS_SEQUENCE = 0;     // Black Emperor: no one else's field reaches inside
    static final String ABOLITION_DOME = "poa_abolition";

    // Exile
    static final int EXILE_SEQUENCE = 1;
    static final int EXILE_COST = 15000;
    static final int EXILE_COOLDOWN = 400;
    static final double EXILE_RANGE = 24.0;
    static final int EXILE_TICKS = 120;                 // an enemy...
    static final int EXILE_BOSS_TICKS = 40;             // ...a boss...
    static final int EXILE_ALLY_TICKS = 60;             // ...an ally, out of harm's way...
    static final int EXILE_SELF_TICKS = 80;             // ...yourself, back...
    static final double EXILE_SELF_RANGE = 20.0;        // ...where you aim, this far

    // Inversion
    static final int INVERSION_SEQUENCE = 0;
    static final int INVERSION_COST = 45000;
    static final int INVERSION_COOLDOWN = 600;
    static final double INVERSION_RADIUS = 20.0;
    static final int INVERSION_TICKS = 200;
    static final int INVERTED_MOST = 200;               // a good effect turned lasts this long at most
    static final String INVERSION_ZONE = "poa_inversion";

    // Coronation
    static final int CORONATION_SEQUENCE = 0;
    static final int CORONATION_COST = 30000;
    static final int CORONATION_COOLDOWN = 600;
    static final int CORONATION_TICKS = 400;
    static final float CORONATION_MIGHT = 2f;                   // the mausoleum bonus, twice over
    static final int CORONATION_PULSE_EVERY = 100;      // a Majesty pulse every 5 seconds...
    static final double CORONATION_PULSE_RADIUS = 10.0; // ...this wide

    // Decree
    static final int DECREE_SEQUENCE = 0;
    static final int DECREE_COST = 25000;
    static final int DECREE_COOLDOWN = 600;
    static final double DECREE_RANGE = 40.0;
    static final float DECREE_DAMAGE = 300f;
    static final float DECREE_PER_MAUSOLEUM = 0.1f;     // 10% more for every standing mausoleum
    static final float DECREE_EXECUTE = 0.4f;           // a non-boss left under 40% dies

    // Saved on the player
    static final String CROWNED_UNTIL = "poa_crowned_until";
    static final String CROWN_TAG = "poa_crown_tag";

    /** Who has which field or pocket open right now (the zones and domes themselves are in Zone and Dome). */
    static final Set<UUID> ENTROPY_OWNERS = new HashSet<>();
    static final Map<UUID, Long> ENTROPY_START = new HashMap<>();
    static final Set<UUID> ABOLITION_OWNERS = new HashSet<>();
    static final Set<UUID> INVERTERS = new HashSet<>();

    /** Inversion: what each good effect turns into (missing here: it is simply taken off). */
    static final Map<Holder<MobEffect>, Holder<MobEffect>> OPPOSITE = new HashMap<>();

    static {
        OPPOSITE.put(MobEffects.STRENGTH, MobEffects.WEAKNESS);
        OPPOSITE.put(MobEffects.SPEED, MobEffects.SLOWNESS);
        OPPOSITE.put(MobEffects.REGENERATION, MobEffects.POISON);
        OPPOSITE.put(MobEffects.HASTE, MobEffects.MINING_FATIGUE);
        OPPOSITE.put(MobEffects.RESISTANCE, MobEffects.WEAKNESS);
        OPPOSITE.put(MobEffects.NIGHT_VISION, MobEffects.BLINDNESS);
        OPPOSITE.put(MobEffects.INVISIBILITY, MobEffects.GLOWING);
        OPPOSITE.put(MobEffects.SATURATION, MobEffects.HUNGER);
    }

    // =========================
    // The key
    // =========================

    static final AbilityWheel WHEEL = AbilityWheel.create("poa_sovereign", "Sovereign")
            .radius(80)
            .quickCastCooldown(false).quickCastCostBy(PrinceOfAbolition::recastCost)
            .noCooldownsWhen(PrinceOfAbolition::noCooldowns)
            .entry("ruin", PrinceOfAbolitionFx.WHEEL_RUIN, "Ruin", RUIN_SEQUENCE,
                    RUIN_COST, RUIN_COOLDOWN, PrinceOfAbolitionSovereign::ruin)
            .entry("frenzy", PrinceOfAbolitionFx.WHEEL_FRENZY, "Frenzy", FRENZY_SEQUENCE,
                    FRENZY_COST, FRENZY_COOLDOWN, PrinceOfAbolitionSovereign::frenzy)
            .entry("entropy", PrinceOfAbolitionFx.WHEEL_ENTROPY, "Entropy", ENTROPY_SEQUENCE,
                    ENTROPY_COST, ENTROPY_COOLDOWN, PrinceOfAbolitionSovereign::entropy)
            .entry("abolition", PrinceOfAbolitionFx.WHEEL_ABOLITION, "Abolition", ABOLITION_SEQUENCE,
                    ABOLITION_COST, ABOLITION_COOLDOWN, PrinceOfAbolitionSovereign::abolition)
            .free(player -> Dome.isRaised(player, ABOLITION_DOME))                    // cast again: closes it, free
            .entry("exile", PrinceOfAbolitionFx.WHEEL_EXILE, "Exile", EXILE_SEQUENCE,
                    EXILE_COST, EXILE_COOLDOWN, PrinceOfAbolitionSovereign::exile)
            .entry("inversion", PrinceOfAbolitionFx.WHEEL_INVERSION, "Inversion", INVERSION_SEQUENCE,
                    INVERSION_COST, INVERSION_COOLDOWN, PrinceOfAbolitionSovereign::inversion)
            .entry("coronation", PrinceOfAbolitionFx.WHEEL_CORONATION, "Coronation", CORONATION_SEQUENCE,
                    CORONATION_COST, CORONATION_COOLDOWN, PrinceOfAbolitionSovereign::coronation)
            .entry("decree", PrinceOfAbolitionFx.WHEEL_DECREE, "Decree", DECREE_SEQUENCE,
                    DECREE_COST, DECREE_COOLDOWN, PrinceOfAbolitionSovereign::decree);

    // =========================
    // Ability 7 - Sovereign
    // Sneak: the Sovereign wheel. Normal: the last pick again, without the wheel.
    // =========================
    public static void princeOfAbolitionSovereign(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!PrinceOfAbolition.ready(player, bypassClassCheck, SOVEREIGN_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.open(player);
        else WHEEL.quickCast(player);
    }

    // =========================
    // Ruin (a direct hit): black and gold gather in your hand, then a beam leaves it along a RUIN_RANGE line (it stops
    // at the first wall) at PrinceOfAbolitionFx.RUIN_FIRES: RUIN_DAMAGE to every enemy on it, hitscan. Whatever it hits
    // starts to rot (status/Corrosion): RUIN_ROT a second, RUIN_ROT_GROWTH more every second, until it is cleansed or
    // dies, and after 5 seconds of it no healing at all. A second Ruin on a rotting enemy deepens it.
    // =========================
    static AbilityWheel.Result ruin(LivingEntity player, ServerLevel sl) {
        double range = PrinceOfAbolition.range(player, RUIN_RANGE);
        Vec3 end = Spells.aimPoint(player, sl, range);
        double width = PrinceOfAbolition.area(player, RUIN_WIDTH);
        PrinceOfAbolitionFx.ruin(sl, player, end);
        Later.run(sl, PrinceOfAbolitionFx.RUIN_FIRES, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            Vec3 from = player.getEyePosition();
            for (LivingEntity victim : Spells.enemiesOnLine(player, sl, from, end, width)) {
                Spells.strike(player, victim, RUIN_DAMAGE);
                PrinceOfAbolitionFx.ruinHit(sl, victim);
                if (!victim.isAlive()) continue;
                float deeper = Corrosion.perSecond(victim) + RUIN_ROT;                // a second Ruin bites deeper
                Corrosion.apply(victim, player, deeper, RUIN_ROT_GROWTH);
            }
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Frenzy: you throw your arms open and FRENZY_BOLTS black bolts burst out, each Distorted to curve onto a
    // different enemy within FRENZY_RANGE (the nearest first; round again if there are fewer). Each does
    // FRENZY_DAMAGE and Disorders what it hits for FRENZY_DISORDER. With no enemies they fly out in a ring and fade.
    // =========================
    static AbilityWheel.Result frenzy(LivingEntity player, ServerLevel sl) {
        List<LivingEntity> near = Spells.enemiesAround(player, sl, player.position(), PrinceOfAbolition.range(player, FRENZY_RANGE));
        near.sort(Comparator.comparingDouble(enemy -> enemy.distanceToSqr(player)));
        List<LivingEntity> targets = near.size() > FRENZY_BOLTS ? new ArrayList<>(near.subList(0, FRENZY_BOLTS)) : near;
        PrinceOfAbolitionFx.frenzy(sl, player, targets);
        if (targets.isEmpty()) return AbilityWheel.Result.DONE;
        int disorder = PrinceOfAbolition.duration(player, FRENZY_DISORDER);
        for (int i = 0; i < FRENZY_BOLTS; i++) {
            LivingEntity victim = targets.get(i % targets.size());
            Later.run(sl, PrinceOfAbolitionFx.FRENZY_FIRST + i * PrinceOfAbolitionFx.FRENZY_STEP, () -> {
                if (!Spells.casterStillHere(player, sl) || !victim.isAlive() || victim.level() != sl) return;
                Spells.strike(player, victim, FRENZY_DAMAGE);
                PrinceOfAbolitionFx.frenzyHit(sl, victim);
                if (victim.isAlive()) PrinceOfAbolition.disorder(player, sl, victim, disorder);
            });
        }
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Entropy: order comes undone round the ground you aim at (ENTROPY_RANGE): a field ENTROPY_RADIUS wide for
    // ENTROPY_TICKS, growing to full strength over ENTROPY_FULL_AFTER. Enemies in it are slowed and shoved about at
    // random, their projectiles turn up to ENTROPY_SCATTER degrees off, and the damage they deal swings between
    // 1 - ENTROPY_SWING and 1 + ENTROPY_SWING of what it should be (all of it scaled by the field's strength). You and
    // your allies are untouched, and Exploit and Magnify cost you nothing inside it. One standing order.
    // =========================
    static AbilityWheel.Result entropy(LivingEntity player, ServerLevel sl) {
        double range = PrinceOfAbolition.range(player, ENTROPY_RANGE);
        Vec3 aimed = Spells.aimGround(player, sl, range);
        Vec3 ground = aimed == null ? null : Spells.groundNear(player, sl, aimed, 2.0);
        if (ground == null) {
            Spells.tell(player, Component.literal("There is no ground there for order to come undone on."));
            return AbilityWheel.Result.REFUSED;
        }
        double radius = PrinceOfAbolition.area(player, ENTROPY_RADIUS);
        int ticks = PrinceOfAbolition.duration(player, ENTROPY_TICKS);
        long start = sl.getGameTime();
        String tag = "poa_entropy_" + player.getId() + "_" + start;
        PrinceOfAbolitionFx.entropy(sl, ground, radius, ticks, tag);
        Zone.at(ground, radius).height(8).lasts(ticks).every(10)
                .onInside((enemy, inside) -> {
                    float strength = strength(sl, start);
                    Spells.keepEffect(enemy, MobEffects.SLOWNESS, 0, 30);
                    if (enemy.getRandom().nextFloat() < ENTROPY_SHOVE_CHANCE * strength) {
                        double angle = enemy.getRandom().nextDouble() * Math.PI * 2;
                        Vec3 from = enemy.position().add(Math.cos(angle), 0, Math.sin(angle));
                        Spells.push(enemy, from, ENTROPY_SHOVE * strength, 0.1);
                    }
                })
                .onPulse(left -> {
                    float strength = strength(sl, start);
                    scatter(player, sl, ground, radius, strength);
                    if (left % 20 < 10) PrinceOfAbolitionFx.entropyPulse(sl, ground, radius, strength);
                })
                .onEnd(() -> {
                    ENTROPY_OWNERS.remove(player.getUUID());
                    ENTROPY_START.remove(player.getUUID());
                    PrinceOfAbolitionFx.entropyEnd(sl, ground, tag);
                    PrinceOfAbolitionOrders.remove(player, "entropy");
                    PrinceOfAbolitionFx.statusEnd(player, Status.ENTROPY);
                })
                .open(player, sl, ENTROPY_ZONE);
        ENTROPY_OWNERS.add(player.getUUID());                    // after open: closing the old field took it out
        ENTROPY_START.put(player.getUUID(), start);
        PrinceOfAbolitionFx.status(player, Status.ENTROPY, ticks);
        if (PrinceOfAbolition.isPrince(player, PrinceOfAbolition.ANY_SEQUENCE)) {
            PrinceOfAbolitionOrders.add(player, "entropy", start + ticks, null);
        }
        return AbilityWheel.Result.DONE;
    }

    /** How strong a field opened at 'start' is now: 0 at first, 1 after ENTROPY_FULL_AFTER. */
    private static float strength(ServerLevel sl, long start) {
        return Math.min(1f, Math.max(0.1f, (sl.getGameTime() - start) / (float) ENTROPY_FULL_AFTER));
    }

    /** Projectiles of enemies (or of nobody) in the field turn off course, once each. */
    private static void scatter(LivingEntity player, ServerLevel sl, Vec3 ground, double radius, float strength) {
        AABB box = new AABB(ground, ground).inflate(radius, 8, radius).move(0, 4, 0);
        for (Projectile shot : sl.getEntitiesOfClass(Projectile.class, box, Entity::isAlive)) {
            if (shot.getPersistentData().getBooleanOr("poa_scattered", false)) continue;
            Entity owner = shot.getOwner();
            if (owner == player || (owner instanceof LivingEntity living && !Spells.isEnemy(player, living))) continue;
            double dx = shot.getX() - ground.x, dz = shot.getZ() - ground.z;
            if (dx * dx + dz * dz > radius * radius) continue;
            double off = (shot.getRandom().nextDouble() * 2 - 1) * ENTROPY_SCATTER * strength;
            Vec3 way = Spells.turned(shot.getDeltaMovement(), off);
            shot.setDeltaMovement(way.add(0, (shot.getRandom().nextDouble() - 0.5) * 0.3 * strength, 0));
            shot.hurtMarked = true;
            shot.getPersistentData().putBoolean("poa_scattered", true);
            PrinceOfAbolitionFx.looseOrder(sl, shot.position());
        }
    }

    /** True while the player stands in their own Entropy field. */
    static boolean inOwnEntropy(LivingEntity player) {
        return ENTROPY_OWNERS.contains(player.getUUID()) && Zone.covers(player, ENTROPY_ZONE, player.position());
    }

    /** What an attacker's damage is multiplied by: a random swing if it stands in an enemy's Entropy field, else 1. */
    static float entropySwing(ServerLevel sl, LivingEntity attacker) {
        if (ENTROPY_OWNERS.isEmpty()) return 1f;
        for (UUID id : ENTROPY_OWNERS) {
            Player owner = sl.getPlayerByUUID(id);
            if (owner == null || owner == attacker || !Spells.isEnemy(owner, attacker)) continue;
            if (!Zone.covers(owner, ENTROPY_ZONE, attacker.position())) continue;
            float strength = strength(sl, ENTROPY_START.getOrDefault(id, sl.getGameTime()));
            return 1f + (attacker.getRandom().nextFloat() * 2f - 1f) * ENTROPY_SWING * strength;
        }
        return 1f;
    }

    // =========================
    // Abolition: you abolish the space round you. After PrinceOfAbolitionFx.ABOLITION_CLOSED a sphere ABOLITION_RADIUS
    // wide slips out of the real world for ABOLITION_TICKS, a grey fog without sound. Nothing crosses its shell either
    // way: enemies cannot come in or go out, their projectiles stop at it, and no blow, aimed ability or mob's
    // attention reaches across it in either direction (separated). Those inside still fight each other. Black Emperor
    // (0): no one else's field or zone reaches inside either (shelteredFromZone). Cast it again to close it early
    // (free). One standing order.
    // =========================
    static AbilityWheel.Result abolition(LivingEntity player, ServerLevel sl) {
        if (Dome.isRaised(player, ABOLITION_DOME)) {
            Dome.close(player, ABOLITION_DOME);
            return AbilityWheel.Result.FREE;
        }
        Vec3 center = player.position().add(0, 1.0, 0);
        double radius = PrinceOfAbolition.area(player, ABOLITION_RADIUS);
        int ticks = PrinceOfAbolition.duration(player, ABOLITION_TICKS);
        String tag = "poa_abolition_" + player.getId() + "_" + sl.getGameTime();
        PrinceOfAbolitionFx.abolition(sl, center, radius, ticks, tag);
        PrinceOfAbolitionFx.status(player, Status.ABOLITION, ticks);
        Later.run(sl, PrinceOfAbolitionFx.ABOLITION_CLOSED, () -> {
            if (!Spells.casterStillHere(player, sl)) {
                PrinceOfAbolitionFx.abolitionEnd(sl, center, radius, tag);
                return;
            }
            int stands = Math.max(20, ticks - PrinceOfAbolitionFx.ABOLITION_CLOSED);
            Dome.sphere(center, radius)
                    .lasts(stands)
                    .keepsEnemiesIn()
                    .stopsProjectiles()
                    .onBlock(at -> PrinceOfAbolitionFx.looseOrder(sl, at))
                    .onEnd(() -> {
                        ABOLITION_OWNERS.remove(player.getUUID());
                        PrinceOfAbolitionFx.abolitionEnd(sl, center, radius, tag);
                        PrinceOfAbolitionOrders.remove(player, "abolition");
                        PrinceOfAbolitionFx.statusEnd(player, Status.ABOLITION);
                    })
                    .raise(player, sl, ABOLITION_DOME);
            ABOLITION_OWNERS.add(player.getUUID());
            if (PrinceOfAbolition.isPrince(player, PrinceOfAbolition.ANY_SEQUENCE)) {
                PrinceOfAbolitionOrders.add(player, "abolition", sl.getGameTime() + stands, null);
            }
        });
        return AbilityWheel.Result.DONE;
    }

    /** True if an Abolition pocket stands between the two: one is inside it and the other is not. */
    static boolean separated(Entity a, Entity b) {
        if (ABOLITION_OWNERS.isEmpty() || a == null || b == null || a.level() != b.level()) return false;
        if (!(a.level() instanceof ServerLevel sl)) return false;
        for (UUID id : ABOLITION_OWNERS) {
            Player owner = sl.getPlayerByUUID(id);
            if (owner == null) continue;
            if (Dome.isInside(owner, ABOLITION_DOME, a) != Dome.isInside(owner, ABOLITION_DOME, b)) return true;
        }
        return false;
    }

    /** Black Emperor's Abolition: no zone from outside the pocket acts on anyone inside it. */
    static boolean shelteredFromZone(LivingEntity zoneOwner, LivingEntity entity) {
        if (ABOLITION_OWNERS.isEmpty() || !(entity.level() instanceof ServerLevel sl)) return false;
        for (UUID id : ABOLITION_OWNERS) {
            Player owner = sl.getPlayerByUUID(id);
            if (owner == null || owner == zoneOwner || !PrinceOfAbolition.isPrince(owner, ABOLITION_FIELDS_SEQUENCE)) continue;
            if (Dome.isInside(owner, ABOLITION_DOME, entity) && !Dome.isInside(owner, ABOLITION_DOME, zoneOwner)) return true;
        }
        return false;
    }

    // =========================
    // Exile: you abolish one thing (status/Exile). Aimed at an enemy (EXILE_RANGE) it is thrown out of the real world
    // for EXILE_TICKS (a boss EXILE_BOSS_TICKS): it cannot be seen, hurt, targeted or act, and comes back where it
    // left. Aimed at an ally it is saved from whatever was coming, for EXILE_ALLY_TICKS. Aimed at nothing you abolish
    // yourself for EXILE_SELF_TICKS and come back where you aim, up to EXILE_SELF_RANGE away.
    // =========================
    static AbilityWheel.Result exile(LivingEntity player, ServerLevel sl) {
        double range = PrinceOfAbolition.range(player, EXILE_RANGE);
        LivingEntity target = PrinceOfAbolition.aim(player, sl, EXILE_RANGE);
        boolean ally = false;
        if (target == null) {
            target = aimedAlly(player, sl, range);
            ally = target != null;
        }
        if (target != null && !Exile.isExiled(target)) {
            int base = ally ? EXILE_ALLY_TICKS : target.getMaxHealth() > PrinceOfAbolition.BOSS_HEALTH ? EXILE_BOSS_TICKS : EXILE_TICKS;
            int ticks = PrinceOfAbolition.duration(player, base);
            PrinceOfAbolitionFx.exile(sl, target);
            if (!Exile.banish(target, ticks, null, PrinceOfAbolitionSovereign::exileBack)) {
                Spells.tell(player, Component.literal("It will not leave the world."));
                return AbilityWheel.Result.REFUSED;
            }
            if (target instanceof Player victim) PrinceOfAbolitionFx.status(victim, Status.EXILE, ticks);
            return AbilityWheel.Result.DONE;
        }
        if (Root.has(player, RootRestriction.TELEPORT)) {
            Spells.tell(player, Component.literal("You are held fast; the world will not let you go."));
            return AbilityWheel.Result.REFUSED;
        }
        Vec3 to = Spells.blinkSpot(player, sl, PrinceOfAbolition.range(player, EXILE_SELF_RANGE));
        PrinceOfAbolitionFx.exile(sl, player);
        Exile.banish(player, EXILE_SELF_TICKS, to != null ? to : player.position(), PrinceOfAbolitionSovereign::exileBack);
        PrinceOfAbolitionFx.status(player, Status.EXILE, EXILE_SELF_TICKS);
        return AbilityWheel.Result.DONE;
    }

    private static void exileBack(LivingEntity who, ServerLevel sl) {
        PrinceOfAbolitionFx.exileReturn(sl, who);
        if (who instanceof Player back) PrinceOfAbolitionFx.statusEnd(back, Status.EXILE);
    }

    /** The ally nearest the crosshair, within 'range', 1.2 blocks off the line of sight at most, in plain view. */
    private static LivingEntity aimedAlly(LivingEntity player, ServerLevel sl, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        LivingEntity best = null;
        double bestOff = 1.2;
        for (LivingEntity ally : Spells.alliesAround(player, sl, eye, range)) {
            Vec3 body = ally.position().add(0, ally.getBbHeight() * 0.5, 0);
            Vec3 to = body.subtract(eye);
            double along = to.dot(look);
            if (along <= 0 || along > range) continue;
            double off = to.subtract(look.scale(along)).length() - ally.getBbWidth() * 0.5;
            if (off < bestOff && Spells.clearLine(player, sl, eye, body)) {
                best = ally;
                bestOff = off;
            }
        }
        return best;
    }

    // =========================
    // Inversion: everything within INVERSION_RADIUS of you turns into its opposite for INVERSION_TICKS (it goes with
    // you). Damage enemies in it deal to you or your allies in it heals you instead (inverterGuarding); healing they
    // receive hurts them (inverterOver); every second their good effects turn into their opposites (OPPOSITE; the
    // rest are taken off), and the light goes out for them (Darkness).
    // =========================
    static AbilityWheel.Result inversion(LivingEntity player, ServerLevel sl) {
        double radius = PrinceOfAbolition.area(player, INVERSION_RADIUS);
        int ticks = PrinceOfAbolition.duration(player, INVERSION_TICKS);
        String tag = "poa_inversion_" + player.getId() + "_" + sl.getGameTime();
        Vec3 cast = player.position();
        PrinceOfAbolitionFx.inversion(sl, player, radius, ticks, tag);
        Zone.around(player, radius).height(radius).lasts(ticks).every(20)
                .onInside((enemy, inside) -> {
                    if (invert(enemy)) PrinceOfAbolitionFx.inverted(sl, enemy);
                    Spells.keepEffect(enemy, MobEffects.DARKNESS, 0, 40);
                })
                .onPulse(left -> PrinceOfAbolitionFx.inversionPulse(sl, player, radius))
                .onEnd(() -> {
                    INVERTERS.remove(player.getUUID());
                    ParticleShapes.clearModels(sl, cast, tag);
                    PrinceOfAbolitionFx.statusEnd(player, Status.INVERSION);
                })
                .open(player, sl, INVERSION_ZONE);
        INVERTERS.add(player.getUUID());
        PrinceOfAbolitionFx.status(player, Status.INVERSION, ticks);
        return AbilityWheel.Result.DONE;
    }

    /** Its good effects turn into their opposites. True if there was one. */
    private static boolean invert(LivingEntity enemy) {
        boolean any = false;
        for (MobEffectInstance effect : new ArrayList<>(enemy.getActiveEffects())) {
            if (!effect.getEffect().value().isBeneficial()) continue;
            enemy.removeEffect(effect.getEffect());
            Holder<MobEffect> opposite = OPPOSITE.get(effect.getEffect());
            if (opposite != null) {
                int ticks = effect.isInfiniteDuration() ? INVERTED_MOST : Math.min(INVERTED_MOST, effect.getDuration());
                enemy.addEffect(new MobEffectInstance(opposite, ticks, effect.getAmplifier()));
            }
            any = true;
        }
        return any;
    }

    /** The inverting Prince whose field turns this blow into healing for them, or null. */
    static Player inverterGuarding(LivingEntity victim, LivingEntity attacker) {
        if (INVERTERS.isEmpty() || !(victim.level() instanceof ServerLevel sl)) return null;
        for (UUID id : INVERTERS) {
            Player owner = sl.getPlayerByUUID(id);
            if (owner == null || attacker == owner || !Spells.isEnemy(owner, attacker)) continue;
            double radius = Zone.radiusOf(owner, INVERSION_ZONE);
            if (radius <= 0 || attacker.distanceTo(owner) > radius) continue;
            boolean ours = victim == owner || (!Spells.isEnemy(owner, victim) && victim.distanceTo(owner) <= radius);
            if (ours) return owner;
        }
        return null;
    }

    /** The inverting Prince in whose field this enemy's healing hurts it, or null. */
    static Player inverterOver(LivingEntity healed) {
        if (INVERTERS.isEmpty() || !(healed.level() instanceof ServerLevel sl)) return null;
        for (UUID id : INVERTERS) {
            Player owner = sl.getPlayerByUUID(id);
            if (owner == null || healed == owner || !Spells.isEnemy(owner, healed)) continue;
            double radius = Zone.radiusOf(owner, INVERSION_ZONE);
            if (radius > 0 && healed.distanceTo(owner) <= radius) return owner;
        }
        return null;
    }

    // =========================
    // Coronation: the mausoleums crown you for CORONATION_TICKS. It needs one standing and uses none up. Their bonus
    // is CORONATION_MIGHT times as big while it lasts (PrinceOfAbolitionMausoleums.might), a Majesty pulse rolls out
    // of you every CORONATION_PULSE_EVERY (PrinceOfAbolitionFallen.majestyPulse), and you cannot be knocked back.
    // =========================
    static AbilityWheel.Result coronation(LivingEntity player, ServerLevel sl) {
        if (PrinceOfAbolitionMausoleums.count(player) < 1) {
            Spells.tell(player, Component.literal("No mausoleum stands to crown you."));
            return AbilityWheel.Result.REFUSED;
        }
        CompoundTag data = player.getPersistentData();
        ParticleShapes.clearModels(sl, player, data.getStringOr(CROWN_TAG, ""));      // crowned again: one crown
        long until = sl.getGameTime() + CORONATION_TICKS;
        String tag = "poa_crown_" + player.getId() + "_" + sl.getGameTime();
        data.putLong(CROWNED_UNTIL, until);
        data.putString(CROWN_TAG, tag);
        PrinceOfAbolitionFx.coronation(sl, player, CORONATION_TICKS, tag);
        PrinceOfAbolitionFx.status(player, Status.CORONATION, CORONATION_TICKS);
        Later.run(sl, 20, () -> crownBeat(player, sl, tag, 20));
        return AbilityWheel.Result.DONE;
    }

    /** Once a second while crowned: the pulse when it is due, the end when the time is up. */
    private static void crownBeat(LivingEntity player, ServerLevel sl, String tag, int beat) {
        CompoundTag data = player.getPersistentData();
        if (!tag.equals(data.getStringOr(CROWN_TAG, ""))) return;                     // crowned again since
        if (!Spells.casterStillHere(player, sl) || !isCrowned(player)) {
            data.remove(CROWNED_UNTIL);
            data.remove(CROWN_TAG);
            if (player.level() instanceof ServerLevel here) PrinceOfAbolitionFx.coronationEnd(here, player, tag);
            PrinceOfAbolitionFx.statusEnd(player, Status.CORONATION);
            return;
        }
        if (beat % CORONATION_PULSE_EVERY == 0) {
            double radius = PrinceOfAbolition.area(player, CORONATION_PULSE_RADIUS);
            PrinceOfAbolitionFx.coronationPulse(sl, player, radius);
            PrinceOfAbolitionFallen.majestyPulse(player, sl, radius);
        }
        Later.run(sl, 20, () -> crownBeat(player, sl, tag, beat + 20));
    }

    /** True while the mausoleums crown the player. */
    static boolean isCrowned(LivingEntity player) {
        return PrinceOfAbolition.isActive(player, CROWNED_UNTIL);
    }

    /** Exploit on oneself: a Coronation running is doubled, by 'most' ticks at most. */
    static void stretchCoronation(LivingEntity player, int most) {
        if (!isCrowned(player) || !(player.level() instanceof ServerLevel sl)) return;
        CompoundTag data = player.getPersistentData();
        long now = sl.getGameTime();
        long until = data.getLongOr(CROWNED_UNTIL, now);
        until += Math.min(most, until - now);
        data.putLong(CROWNED_UNTIL, until);
        String tag = data.getStringOr(CROWN_TAG, "");
        ParticleShapes.clearModels(sl, player, tag);
        PrinceOfAbolitionFx.coronation(sl, player, (int) (until - now), tag);
        PrinceOfAbolitionFx.status(player, Status.CORONATION, (int) (until - now));
    }

    // =========================
    // Decree (a direct hit): you speak one word and the enemy you look at (DECREE_RANGE) takes DECREE_DAMAGE at
    // PrinceOfAbolitionFx.DECREE_LANDS, DECREE_PER_MAUSOLEUM more for every standing mausoleum. Nothing stops it
    // (Spells.strikeTrue): a raised shield, armor, Protection, Resistance and death wards do not hold. A non-boss left
    // under DECREE_EXECUTE of its health dies.
    // =========================
    static AbilityWheel.Result decree(LivingEntity player, ServerLevel sl) {
        LivingEntity target = PrinceOfAbolition.aim(player, sl, DECREE_RANGE);
        if (target == null) {
            Spells.tell(player, Component.literal("There is no one in sight to hear your word."));
            return AbilityWheel.Result.REFUSED;
        }
        int mausoleums = PrinceOfAbolitionMausoleums.count(player);
        PrinceOfAbolitionFx.decree(sl, player, target, mausoleums);
        Later.run(sl, PrinceOfAbolitionFx.DECREE_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            Spells.strikeTrue(player, target, DECREE_DAMAGE * (1f + DECREE_PER_MAUSOLEUM * mausoleums));
            if (target.isAlive() && target.getMaxHealth() <= PrinceOfAbolition.BOSS_HEALTH
                    && target.getHealth() < target.getMaxHealth() * DECREE_EXECUTE) {
                Spells.strikeTrue(player, target, (target.getHealth() + target.getAbsorptionAmount()) * 10f + 100f);
            }
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Cleanup
    // =========================

    /** The Pathway was lost: every field, pocket and crown of this player ends. */
    static void endAll(LivingEntity player) {
        Zone.close(player, ENTROPY_ZONE);
        Zone.close(player, INVERSION_ZONE);
        if (Dome.isRaised(player, ABOLITION_DOME)) Dome.close(player, ABOLITION_DOME);
        player.getPersistentData().remove(CROWNED_UNTIL);
        player.getPersistentData().remove(CROWN_TAG);
    }

    /** After a relog or a death: no crown is left over (its look did not survive either). */
    static void clearState(Player player) {
        player.getPersistentData().remove(CROWNED_UNTIL);
        player.getPersistentData().remove(CROWN_TAG);
    }
}
