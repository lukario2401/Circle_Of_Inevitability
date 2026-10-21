package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperorFx.Status;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Orbit;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Blind;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.Scrolls;
import net.lukario.circleofinevitability.status.Study;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Knowledge Emperor, key 5: the Stars wheel (Constellations Master, Sequence 5). docs/KNOWLEDGE_EMPEROR.md, "Key 5 wheel".
 *
 *   Starlight Cage   starlight flies to the enemy you look at and sets into amber round it and everyone near it:
 *                    held until the amber breaks or melts. Scrollable. From Sage (2) wider, and bosses held as long
 *   Stellar Self     you break into starlight and flow through any gap to where you aim, unhurt on the way; the
 *                    flash blinds those you leave. From Clairvoyant (3) further, and allies near you come along
 *   Star Fall        five small stars fall round where you aim, each bursting where it lands; the enemies you know
 *                    best are chosen first. Scrollable
 *   Star Guard       four stars circle you (combat/Orbit): each stops a projectile and comes back, and an enemy that
 *                    comes close is struck
 *
 * Sneak casts the last pick again without the wheel (no cooldown, double the cost), as for Hand Of God. Every entry's
 * cost goes through KnowledgeEmperor.costFor, every cooldown through cooldownFor. The looks are KnowledgeEmperorFx's; every
 * number is right below.
 */
public final class KnowledgeEmperorStars {

    private KnowledgeEmperorStars() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int STARS_SEQUENCE = 5;                         // the key
    private static final float QUICK_CAST_COST = 2f;            // a sneak recast: no cooldown, this many times the cost

    // Starlight Cage
    private static final int CAGE_SEQUENCE = 5;
    private static final int CAGE_COST = 3000;                  // Sequence 5 holds 15500
    private static final int CAGE_COOLDOWN = 280;
    private static final double CAGE_RANGE = 24.0;
    private static final double CAGE_RADIUS = 3.0;              // everyone this close to the target is caught too
    private static final int CAGE_TICKS = 100;
    private static final int CAGE_BOSS_TICKS = 40;              // bosses break out sooner...
    private static final float CAGE_BREAKS_AT = 100f;           // ...and the amber breaks after this much damage
    private static final int CAGE_STUDY = 15;
    private static final int SAGE_CAGE_SEQUENCE = 2;            // Sage: wider, and bosses held the whole time
    private static final double SAGE_CAGE_RADIUS = 5.0;

    // Stellar Self
    private static final int STELLAR_SEQUENCE = 5;
    private static final int STELLAR_COST = 2500;
    private static final int STELLAR_COOLDOWN = 200;
    private static final double STELLAR_RANGE = 16.0;
    private static final double STELLAR_BLIND_RADIUS = 4.0;     // the flash left behind blinds enemies this close...
    private static final int STELLAR_BLIND_TICKS = 40;          // ...this long
    private static final int CARRY_SEQUENCE = 3;                // Clairvoyant: further...
    private static final double CARRY_RANGE = 40.0;
    private static final double CARRY_RADIUS = 3.0;             // ...and allies this close come along

    // Star Fall
    private static final int FALL_SEQUENCE = 5;
    private static final int FALL_COST = 3500;
    private static final int FALL_COOLDOWN = 360;
    private static final double FALL_RANGE = 28.0;
    private static final int FALL_STARS = 5;
    private static final int FALL_EVERY = 8;                    // ticks between two stars (five over about 2 seconds)
    private static final double FALL_AREA = 6.0;                // they fall within this of where you aim...
    private static final double FALL_RADIUS = 3.0;              // ...and each bursts this wide...
    private static final float FALL_DAMAGE = 30f;               // ...for this much

    // Star Guard
    private static final int GUARD_SEQUENCE = 5;
    private static final int GUARD_COST = 2800;
    private static final int GUARD_COOLDOWN = 600;
    private static final int GUARD_TICKS = 600;
    private static final int GUARD_STARS = 4;
    private static final int GUARD_RETURN = 100;                // a star that stopped a projectile is back after this
    private static final double GUARD_RANGE = 3.0;              // an enemy this close...
    private static final int GUARD_EVERY = 40;                  // ...is struck by a star this often...
    private static final float GUARD_DAMAGE = 15f;              // ...for this much
    private static final int GUARD_STUDY = 5;
    // the orbit lines up with KnowledgeEmperorFx.starGuardTick (its GUARD_RADIUS, height and GUARD_SPEED)
    private static final double GUARD_ORBIT_RADIUS = 1.2;
    private static final double GUARD_ORBIT_HEIGHT = 1.1;
    private static final float GUARD_ORBIT_SPEED = 6f;

    // =========================
    // Names and what is kept
    // =========================
    static final String SCROLL_CAGE = "knowledge_emperor_starlight_cage";
    static final String SCROLL_STAR_FALL = "knowledge_emperor_star_fall";
    private static final String GUARD = "knowledge_emperor_star_guard";

    /** Who is held in amber: how much the amber has taken, and under which tag it is shown. Not saved. */
    private static final class Cage {
        final String tag;
        float taken;

        Cage(String tag) {
            this.tag = tag;
        }
    }
    private static final Map<UUID, Cage> CAGES = new HashMap<>();

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("knowledge_emperor_stars", "Stars")
            .radius(72)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .cooldownBy(KnowledgeEmperor::cooldownFor)
            .entry("starlight_cage", KnowledgeEmperorFx.WHEEL_STARLIGHT_CAGE, "Starlight Cage", CAGE_SEQUENCE,
                    CAGE_COST, CAGE_COOLDOWN, KnowledgeEmperorStars::starlightCage)
            .costBy(KnowledgeEmperor::costFor)
            .entry("stellar_self", KnowledgeEmperorFx.WHEEL_STELLAR_SELF, "Stellar Self", STELLAR_SEQUENCE,
                    STELLAR_COST, STELLAR_COOLDOWN, KnowledgeEmperorStars::stellarSelf)
            .costBy(KnowledgeEmperor::costFor)
            .entry("star_fall", KnowledgeEmperorFx.WHEEL_STAR_FALL, "Star Fall", FALL_SEQUENCE,
                    FALL_COST, FALL_COOLDOWN, KnowledgeEmperorStars::starFall)
            .costBy(KnowledgeEmperor::costFor)
            .entry("star_guard", KnowledgeEmperorFx.WHEEL_STAR_GUARD, "Star Guard", GUARD_SEQUENCE,
                    GUARD_COST, GUARD_COOLDOWN, KnowledgeEmperorStars::starGuard)
            .costBy(KnowledgeEmperor::costFor)
            .onCast((player, sl, entry) -> KnowledgeEmperor.afterCast(player, entry.cost()));

    /** The Scrollable stars (KnowledgeEmperor registers every scroll at once, before any pouch is read). */
    static void registerScrolls() {
        Scrolls.register(SCROLL_CAGE, "Starlight Cage", CAGE_COST, KnowledgeEmperorStars::cage);
        Scrolls.register(SCROLL_STAR_FALL, "Star Fall", FALL_COST, KnowledgeEmperorStars::fall);
    }

    // =========================
    // Ability 5 - Stars
    // Normal: the Stars wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void knowledgeEmperorStars(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!KnowledgeEmperor.ready(player, bypassClassCheck, STARS_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Starlight Cage: starlight flies to the enemy you look at (CAGE_RANGE) and, at KnowledgeEmperorFx.CAGE_LANDS,
    // sets into amber round it and every enemy within CAGE_RADIUS of it: held completely for CAGE_TICKS (bosses
    // CAGE_BOSS_TICKS) unless the amber round one of them takes CAGE_BREAKS_AT damage first. CAGE_STUDY each.
    // Sage (2): SAGE_CAGE_RADIUS, and bosses are held the whole time. Scrollable.
    // =========================
    private static AbilityWheel.Result starlightCage(ServerPlayer player, ServerLevel sl) {
        if (!cage(player, sl, 1f)) return AbilityWheel.Result.REFUSED;
        Scrolls.cast(player, SCROLL_CAGE);
        return AbilityWheel.Result.DONE;
    }

    private static boolean cage(ServerPlayer player, ServerLevel sl, float power) {
        LivingEntity target = Spells.aimEnemy(player, sl, CAGE_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("The starlight has no one to settle on."));
            return false; // nothing spent
        }
        boolean sage = Beyonder.hasSequence(player, SAGE_CAGE_SEQUENCE);
        KnowledgeEmperorFx.starlightCage(sl, player, target);
        Later.run(sl, KnowledgeEmperorFx.CAGE_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            Vec3 middle = target.getBoundingBox().getCenter();
            List<LivingEntity> caught = Spells.enemiesAround(player, sl, middle, sage ? SAGE_CAGE_RADIUS : CAGE_RADIUS);
            if (!caught.contains(target)) caught.add(target);
            for (LivingEntity held : caught) {
                int ticks = Math.round((KnowledgeEmperor.isBoss(held) && !sage ? CAGE_BOSS_TICKS : CAGE_TICKS) * power);
                encase(player, sl, held, ticks);
            }
        });
        return true;
    }

    /** One body set in amber for 'ticks'. */
    private static void encase(Player player, ServerLevel sl, LivingEntity held, int ticks) {
        Cage old = CAGES.remove(held.getUUID());
        if (old != null) KnowledgeEmperorFx.cageEnd(sl, held, old.tag);               // a new cage over an old one
        String tag = "ke_cage_" + held.getId() + "_" + sl.getGameTime();
        Cage cage = new Cage(tag);
        CAGES.put(held.getUUID(), cage);
        Root.apply(held, ticks, RootRestriction.EVERYTHING);
        KnowledgeEmperorFx.caged(sl, held, ticks, tag);
        if (held instanceof Player caged) KnowledgeEmperorFx.status(caged, Status.CAGED, ticks);
        Study.add(player, held, CAGE_STUDY);
        Later.run(sl, ticks, () -> {
            if (CAGES.get(held.getUUID()) != cage) return;                           // broken, or caged again since
            CAGES.remove(held.getUUID());
            if (held.isAlive()) KnowledgeEmperorFx.cageEnd(sl, held, tag);
        });
    }

    /** Damage got through to someone (after armor): the amber round them takes it, and breaks at CAGE_BREAKS_AT. */
    static void damaged(LivingEntity victim, ServerLevel sl, float amount) {
        if (CAGES.isEmpty()) return;
        Cage cage = CAGES.get(victim.getUUID());
        if (cage == null) return;
        cage.taken += amount;
        if (cage.taken < CAGE_BREAKS_AT) {
            KnowledgeEmperorFx.cageCrack(sl, victim);
            return;
        }
        CAGES.remove(victim.getUUID());
        Root.remove(victim, RootRestriction.values());
        KnowledgeEmperorFx.cageBreak(sl, victim, cage.tag);
        if (victim instanceof Player freed) KnowledgeEmperorFx.statusEnd(freed, Status.CAGED);
    }

    static void died(LivingEntity dead, ServerLevel sl) {
        Cage cage = CAGES.remove(dead.getUUID());
        if (cage != null) KnowledgeEmperorFx.cageEnd(sl, dead, cage.tag);
    }

    // =========================
    // Stellar Self: you break into starlight where you stand (enemies within STELLAR_BLIND_RADIUS are blinded by the
    // flash) and re-form where you aim, at most STELLAR_RANGE away, at KnowledgeEmperorFx.STELLAR_ARRIVES: hidden and
    // unhurt on the way (status/Concealment). It flows through any gap: only solid walls stop it, not glass, bars,
    // fences, leaves or open doors. Not while held in place. Clairvoyant (3): CARRY_RANGE, and every ally within
    // CARRY_RADIUS comes along.
    // =========================
    private static AbilityWheel.Result stellarSelf(ServerPlayer player, ServerLevel sl) {
        if (Root.has(player, RootRestriction.MOVE) || Root.has(player, RootRestriction.TELEPORT)) {
            player.sendSystemMessage(Component.literal("You are held fast and cannot break into starlight."));
            return AbilityWheel.Result.REFUSED;
        }
        boolean carry = Beyonder.hasSequence(player, CARRY_SEQUENCE);
        Vec3 to = stellarSpot(player, sl, carry ? CARRY_RANGE : STELLAR_RANGE);
        if (to == null || to.distanceToSqr(player.position()) < 1.0) {
            player.sendSystemMessage(Component.literal("There is no way for the starlight to go there."));
            return AbilityWheel.Result.REFUSED;
        }
        Vec3 from = player.position();
        int arrives = KnowledgeEmperorFx.STELLAR_ARRIVES;
        List<LivingEntity> allies = new ArrayList<>();
        if (carry) {
            for (LivingEntity ally : Spells.alliesAround(player, sl, from, CARRY_RADIUS)) {
                if (!Root.has(ally, RootRestriction.TELEPORT) && !Root.has(ally, RootRestriction.MOVE)) allies.add(ally);
            }
        }
        KnowledgeEmperorFx.stellarSelf(sl, player, from, to);
        Concealment.hide(player, arrives + 2);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, from.add(0, 1, 0), STELLAR_BLIND_RADIUS)) {
            Blind.apply(enemy, STELLAR_BLIND_TICKS);
        }
        for (LivingEntity ally : allies) {
            KnowledgeEmperorFx.stellarCarry(sl, ally, ally.position(), to);
            Concealment.hide(ally, arrives + 2);
        }
        Later.run(sl, arrives, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            Vec3 there = Spells.fits(player, sl, to) ? to : Spells.freeSpotNear(player, sl, to, Spells.flatLook(player).scale(-1), 2);
            if (there != null) {
                player.teleportTo(there.x, there.y, there.z);
                player.fallDistance = 0;
                FallGuard.protect(player, 100);
            }
            Concealment.reveal(player);
            Vec3 landing = there != null ? there : player.position();
            for (int i = 0; i < allies.size(); i++) {
                LivingEntity ally = allies.get(i);
                if (!ally.isAlive() || ally.level() != sl) continue;
                Vec3 side = Spells.spotAround(Vec3.ZERO, i * 360f / Math.max(1, allies.size()), 1.0);
                Vec3 beside = Spells.freeSpotNear(player, sl, landing, side, 1.5);
                Vec3 spot = beside != null ? beside : landing;
                ally.teleportTo(spot.x, spot.y, spot.z);
                ally.fallDistance = 0;
                FallGuard.protect(ally, 100);
                Concealment.reveal(ally);
            }
        });
        return AbilityWheel.Result.DONE;
    }

    /**
     * Where the starlight re-forms: along the look, through everything that lets starlight by, up to 'range'; then
     * the nearest place on that line (going back) where the player's body fits. Null when there is none.
     */
    static Vec3 stellarSpot(Player player, ServerLevel sl, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(range));
        Vec3 at = eye;
        Vec3 stop = end;
        for (int crossings = 0; crossings < 24; crossings++) {
            BlockHitResult hit = sl.clip(new ClipContext(at, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS) break;
            BlockPos pos = hit.getBlockPos();
            if (!letsStarlightBy(sl, pos)) {
                stop = hit.getLocation();
                break;
            }
            Vec3 past = hit.getLocation();                                             // on through that block
            for (int step = 0; step < 40 && BlockPos.containing(past.x, past.y, past.z).equals(pos); step++) past = past.add(look.scale(0.05));
            if (past.distanceToSqr(eye) >= range * range) break;
            at = past;
        }
        double far = eye.distanceTo(stop);
        for (double back : new double[]{0.4, 0.8, 1.2, 1.8, 2.6, 3.6}) {
            if (back >= far) break;
            Vec3 near = stop.subtract(look.scale(back));
            Vec3 ground = Spells.groundAt(player, sl, near);
            if (near.y - ground.y <= 3.0 && Spells.fits(player, sl, ground)) return ground;
            for (double drop : new double[]{1.0, 1.85, 0.0}) {
                Vec3 feet = near.add(0, -drop, 0);
                if (Spells.fits(player, sl, feet)) return feet;
            }
        }
        return null;
    }

    /** Starlight goes through glass, panes, bars, fences, leaves, chains and open doors and gates; not through walls. */
    private static boolean letsStarlightBy(ServerLevel sl, BlockPos pos) {
        BlockState state = sl.getBlockState(pos);
        if (state.isAir()) return true;
        if (state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN)) return true;
        String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return name.contains("glass") || name.contains("bars") || name.endsWith("fence") || name.contains("leaves")
                || name.equals("chain") || name.endsWith("_chain") || name.contains("cobweb") || name.contains("scaffolding");
    }

    // =========================
    // Star Fall: FALL_STARS stars fall one after another (FALL_EVERY apart) within FALL_AREA of where you aim
    // (FALL_RANGE); each shows a warning ring and lands at KnowledgeEmperorFx.STAR_LANDS, bursting for FALL_DAMAGE
    // within FALL_RADIUS. The enemies you know best (50% Study or more) are aimed at first, then the others, then the
    // ground. Scrollable.
    // =========================
    private static AbilityWheel.Result starFall(ServerPlayer player, ServerLevel sl) {
        fall(player, sl, 1f);
        Scrolls.cast(player, SCROLL_STAR_FALL);
        return AbilityWheel.Result.DONE;
    }

    private static boolean fall(ServerPlayer player, ServerLevel sl, float power) {
        Vec3 center = Spells.aimGround(player, sl, FALL_RANGE);
        String tag = "ke_starfall_" + player.getId() + "_" + sl.getGameTime();
        int lasts = (FALL_STARS - 1) * FALL_EVERY + KnowledgeEmperorFx.STAR_LANDS + 10;
        KnowledgeEmperorFx.starFallSky(sl, center, lasts, tag);
        Later.run(sl, lasts, () -> KnowledgeEmperorFx.starFallEnd(sl, center, tag));
        Set<UUID> aimedAt = new HashSet<>();
        Set<UUID> studied = new HashSet<>();
        for (int i = 0; i < FALL_STARS; i++) {
            Later.run(sl, i * FALL_EVERY, () -> {
                if (!Spells.casterStillHere(player, sl)) return;
                Vec3 ground = nextStarSpot(player, sl, center, aimedAt);
                KnowledgeEmperorFx.starFall(sl, ground, FALL_RADIUS);
                Later.run(sl, KnowledgeEmperorFx.STAR_LANDS, () -> {
                    if (!Spells.casterStillHere(player, sl)) return;
                    for (LivingEntity enemy : Spells.enemiesAround(player, sl, ground.add(0, 0.5, 0), FALL_RADIUS)) {
                        Study.strike(player, enemy, FALL_DAMAGE * power, studied.add(enemy.getUUID()) ? KnowledgeEmperor.ABILITY_STUDY : 0);
                    }
                });
            });
        }
        return true;
    }

    /** Where the next star falls: on an enemy not aimed at yet (the best known first), else on the ground near the middle. */
    private static Vec3 nextStarSpot(Player player, ServerLevel sl, Vec3 center, Set<UUID> aimedAt) {
        LivingEntity chosen = null;
        int best = -1;
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, center, FALL_AREA)) {
            if (aimedAt.contains(enemy.getUUID())) continue;
            int known = Study.get(player, enemy);
            int rank = known >= Study.WEAKNESS ? 1000 + known : 0;                  // the well known first, then the rest
            if (rank > best) {
                best = rank;
                chosen = enemy;
            }
        }
        if (chosen != null) {
            aimedAt.add(chosen.getUUID());
            return Spells.groundUnder(player, sl, chosen);
        }
        for (int tries = 0; tries < 6; tries++) {
            double angle = sl.getRandom().nextDouble() * Math.PI * 2;
            double far = Math.sqrt(sl.getRandom().nextDouble()) * (FALL_AREA - FALL_RADIUS * 0.5);
            Vec3 ground = Spells.groundNear(player, sl, center.add(Math.cos(angle) * far, 0, Math.sin(angle) * far), 3);
            if (ground != null) return ground;
        }
        return center;
    }

    // =========================
    // Star Guard: GUARD_STARS stars circle you for GUARD_TICKS (combat/Orbit). Each stops one projectile and is back
    // GUARD_RETURN later; an enemy within GUARD_RANGE is struck by one for GUARD_DAMAGE every GUARD_EVERY. Cast again
    // for a fresh guard.
    // =========================
    private static AbilityWheel.Result starGuard(ServerPlayer player, ServerLevel sl) {
        String tag = "ke_guard_" + player.getId() + "_" + sl.getGameTime();
        Orbit.close(player, GUARD);                                                  // the old guard first (its stars go)
        KnowledgeEmperorFx.starGuard(sl, player, GUARD_TICKS, tag);
        KnowledgeEmperorFx.status(player, Status.STAR_GUARD, GUARD_TICKS);
        Orbit.around(player, GUARD_STARS)
                .radius(GUARD_ORBIT_RADIUS).height(GUARD_ORBIT_HEIGHT).speed(GUARD_ORBIT_SPEED)
                .lasts(GUARD_TICKS)
                .catchesProjectiles(GUARD_RETURN)
                .strikes(GUARD_RANGE, GUARD_EVERY, (enemy, index) -> {
                    KnowledgeEmperorFx.starGuardStrike(sl, player, enemy);
                    Study.strike(player, enemy, GUARD_DAMAGE, GUARD_STUDY);
                })
                .onTick(age -> KnowledgeEmperorFx.starGuardTick(sl, player, tag, age))
                .onCatch((index, projectile, at) -> KnowledgeEmperorFx.starGuardBlock(sl, player, tag, index, at))
                .onBack((index, ticksLeft, age) -> KnowledgeEmperorFx.starGuardBack(sl, player, tag, index, ticksLeft, age))
                .onEnd(() -> {
                    KnowledgeEmperorFx.starGuardEnd(sl, player, tag);
                    KnowledgeEmperorFx.statusEnd(player, Status.STAR_GUARD);
                })
                .open(player, sl, GUARD);
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Kept tidy by KnowledgeEmperor
    // =========================

    /** After a relog or a death. */
    static void clearState(Player player) {
        Orbit.close(player, GUARD);
    }
}
