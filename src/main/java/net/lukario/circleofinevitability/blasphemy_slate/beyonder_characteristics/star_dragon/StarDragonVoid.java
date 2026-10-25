package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragonFx.Status;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragonFx.VoidDepth;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragonMarks.Mark;
import net.lukario.circleofinevitability.combat.Cooldowns;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Freeze;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Blind;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Exposed;
import net.lukario.circleofinevitability.status.Falter;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.Unnoticed;
import net.lukario.circleofinevitability.status.Vulnerable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Star Dragon, the void and Fate (docs/STAR_DRAGON.md): Cosmic Void (Star Sacrifice, key 2), Void Passage (key 3),
 * Fate Convergence and Remote Sight (Heavybringer, key 5), Quantum Uncertainty (the Cosmos wheel, key 7). Every number
 * is right below; the looks are StarDragonFx's.
 */
public final class StarDragonVoid {

    private StarDragonVoid() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    // Cosmic Void (Star Sacrificer)
    static final int VOID_SEQUENCE = 7;
    static final int VOID_COST = 1800;                          // the plan's 2450: Sequence 7 holds only 1980
    static final int VOID_COOLDOWN = 360;
    static final double VOID_RANGE = 20.0;
    static final double VOID_RADIUS = 2.0;              // a sphere 4 across...
    static final int VOID_TICKS = 80;                   // ...for 4 seconds
    static final float VOID_BREAKS = 0.4f;              // it ends early once the target took this share of its health
    static final int MANIFEST_SEQUENCE = 5;                     // Tidal Scholar: rays manifest inside...
    static final float VOID_RAY = 0.25f;                        // ...this much stronger, and nothing stops them
    static final int POCKET_SEQUENCE = 4;               // Heavybringer: a true pocket...
    static final double POCKET_RADIUS = 3.0;            // ...6 across...
    static final int POCKET_TICKS = 160;                // ...for 8 seconds...
    static final float POCKET_FRAGILE = 0.3f;           // ...abnormally fragile, teleports sealed
    static final int ZERO_SEQUENCE = 1;                 // Star Dragon: near absolute zero...
    static final double ZERO_RADIUS = 5.0;              // ...10 across...
    static final int ZERO_FREEZE_EVERY = 40;            // ...frozen 2 seconds every 2 seconds, and irradiated
    static final int ZERO_FREEZE = 40;

    // Void Passage (Navigator, key 3)
    static final int PASSAGE_COST = 2450;
    static final int PASSAGE_COOLDOWN = 100;
    static final double PASSAGE_RANGE = 24.0;
    static final double PASSAGE_FAR = 40.0;             // Heavybringer: further...
    static final int PASSAGE_FAR_SEQUENCE = 4;
    static final double PASSAGE_ALLY = 3.0;             // ...and an ally this close comes through with you
    static final int PASSAGE_UNNOTICED = 60;            // no mob can find you this long
    static final double PASSAGE_EXPOSES = 12.0;         // the hidden this close to either door are forced out
    static final int PASSAGE_EXPOSED = 60;

    // Fate Convergence (Heavybringer)
    static final int FATE_SEQUENCE = 4;
    static final int FATE_COST = 6000;
    static final int FATE_COOLDOWN = 800;
    static final double FATE_RADIUS = 20.0;
    static final int FATE_TICKS = 200;
    static final int FATE_FOREVER_SEQUENCE = 0;         // Condenser: twice as long, and on bosses too
    static final float FATE_LONGER = 2f;

    // Remote Sight (Heavybringer)
    static final int SIGHT_SEQUENCE = 4;
    static final int SIGHT_COST = 2450;
    static final int SIGHT_COOLDOWN = 400;
    static final int SIGHT_TICKS = 300;
    static final double SIGHT_RANGE = 64.0;
    static final int SIGHT_PASSAGES = 12;               // doors and portals this close show as passages

    // Quantum Uncertainty (Condenser, the Cosmos wheel)
    static final int QUANTUM_SEQUENCE = 0;
    static final int QUANTUM_COST = 45000;
    static final int QUANTUM_COOLDOWN = 800;
    static final double QUANTUM_RADIUS = 20.0;
    static final int QUANTUM_TICKS = 300;
    static final float QUANTUM_MISS = 0.4f;             // enemies in it miss this share (status/Falter)...
    static final double QUANTUM_SLIP = 8.0;             // ...and a teleport into it lands up to this far off

    // =========================
    // Names and what is kept
    // =========================
    static final String QUANTUM = "star_dragon_quantum";                  // its Zone and its status/Falter reason
    private static final Set<UUID> QUANTUM_OWNERS = new HashSet<>();               // who has a field open (for teleports)
    static final String VOID = "star_dragon_cosmic_void";                 // its Dome
    static final String FATE = "star_dragon_fate_convergence";            // its Zone
    static final String VOID_TAKEN = "star_dragon_void_taken";            // on the target: health lost inside
    static final String CD_PASSAGE = "star_dragon_void_passage";
    static final String SIGHT_CAST = "star_dragon_sight_cast";

    // =========================
    // Cosmic Void: the enemy you look at (VOID_RANGE) falls into a half-real void for VOID_TICKS (70% on a player, half
    // on a boss): blind, slowed, and it cannot find the way out (a Dome it alone cannot cross). Its body stays in the
    // world and can be hurt; the void ends early once it took VOID_BREAKS of its health. Tidal Scholar: your rays on it
    // manifest inside (StarDragon.rayHit). Heavybringer: a true pocket, POCKET_RADIUS, abnormally fragile, teleports
    // sealed, POCKET_TICKS. Star Dragon: near absolute zero, ZERO_RADIUS, frozen and irradiated every 2 seconds.
    // =========================
    static AbilityWheel.Result cosmicVoid(LivingEntity player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, VOID_RANGE);
        if (target == null) {
            Spells.tell(player, Component.literal("There is no one to cast into the void."));
            return AbilityWheel.Result.REFUSED;
        }
        VoidDepth depth = StarDragon.grown(player, ZERO_SEQUENCE) ? VoidDepth.ABSOLUTE_ZERO
                : StarDragon.grown(player, POCKET_SEQUENCE) ? VoidDepth.TRUE_POCKET : VoidDepth.HALF_REAL;
        double radius = depth == VoidDepth.ABSOLUTE_ZERO ? ZERO_RADIUS : depth == VoidDepth.TRUE_POCKET ? POCKET_RADIUS : VOID_RADIUS;
        int ticks = Spells.holdLength(target, depth == VoidDepth.HALF_REAL ? VOID_TICKS : POCKET_TICKS);
        Dome.close(player, VOID);                                                  // one void at a time: the last one ends
        String tag = voidTag(target);
        Vec3 center = target.getBoundingBox().getCenter();
        target.getPersistentData().putFloat(VOID_TAKEN, 0f);
        StarDragonFx.cosmicVoid(sl, target, radius, ticks, depth, tag);
        StarDragonMarks.put(target, Mark.VOIDED, player, ticks);
        Blind.apply(target, ticks);
        Spells.keepEffect(target, MobEffects.SLOWNESS, 1, ticks);
        if (depth != VoidDepth.HALF_REAL) {
            Vulnerable.add(target, POCKET_FRAGILE, POCKET_FRAGILE, ticks);
            Root.apply(target, ticks, RootRestriction.TELEPORT);
        }
        Dome.sphere(center, radius)
                .lasts(ticks)
                .keepsEnemiesIn()
                .outsiders(one -> one == target)
                .keepWhile(() -> target.isAlive() && StarDragonMarks.fromCaster(target, Mark.VOIDED, player))
                .onEnd(() -> {
                    StarDragonFx.voidEnd(sl, target, tag);
                    if (StarDragonMarks.fromCaster(target, Mark.VOIDED, player)) StarDragonMarks.end(target, Mark.VOIDED);
                    Blind.remove(target);
                    target.getPersistentData().remove(VOID_TAKEN);
                })
                .raise(player, sl, VOID);
        if (depth == VoidDepth.ABSOLUTE_ZERO) {
            for (int t = ZERO_FREEZE_EVERY; t < ticks; t += ZERO_FREEZE_EVERY) {
                Later.run(sl, t, () -> {
                    if (!Dome.isRaised(player, VOID) || !target.isAlive()) return;
                    Freeze.apply(target, Spells.holdLength(target, ZERO_FREEZE));
                    StarDragonMarks.put(target, Mark.IRRADIATED, player, ZERO_FREEZE_EVERY + 20);
                    StarDragonFx.voidFreeze(sl, target, radius);
                });
            }
        }
        return AbilityWheel.Result.DONE;
    }

    /** A hurt on someone in a Cosmic Void: once it took VOID_BREAKS of its health, the void lets it go. */
    static void hurt(LivingHurtEvent event) {
        LivingEntity hurt = event.getEntity();
        if (!StarDragonMarks.has(hurt, Mark.VOIDED) || !(hurt.level() instanceof ServerLevel sl)) return;
        float taken = hurt.getPersistentData().getFloatOr(VOID_TAKEN, 0f) + event.getAmount();
        hurt.getPersistentData().putFloat(VOID_TAKEN, taken);
        if (taken < hurt.getMaxHealth() * VOID_BREAKS) return;
        UUID by = StarDragonMarks.from(hurt, Mark.VOIDED);
        StarDragonMarks.end(hurt, Mark.VOIDED);                                    // the Dome's keepWhile lets go next tick
        if (by != null && sl.getPlayerByUUID(by) == null) StarDragonFx.voidEnd(sl, hurt, voidTag(hurt));
    }

    private static String voidTag(LivingEntity target) {
        return "sd_void_" + target.getId();
    }

    // =========================
    // Void Passage (key 3): you slip into a hidden passage of the void and step out where you aim (PASSAGE_RANGE;
    // PASSAGE_FAR from the Heavybringer) StarDragonFx.PASSAGE_ARRIVES later, unnoticed for PASSAGE_UNNOTICED (no mob can
    // find you; it ends when you hit something). The hidden near either door are forced out. Not while held in place
    // (Root TELEPORT or MOVE): nothing spent. Heavybringer: the ally nearest you within PASSAGE_ALLY comes too.
    // =========================
    static void voidPassage(Player player, ServerLevel sl) {
        if (Root.has(player, RootRestriction.TELEPORT) || Root.has(player, RootRestriction.MOVE)) {
            player.sendSystemMessage(Component.literal("You are held fast: the void's passages are closed to you."));
            return; // nothing spent
        }
        double range = StarDragon.grown(player, PASSAGE_FAR_SEQUENCE) ? PASSAGE_FAR : PASSAGE_RANGE;
        Vec3 to = Spells.blinkSpot(player, sl, range);
        if (to == null || to.distanceToSqr(player.position()) < 4.0) {
            player.sendSystemMessage(Component.literal("There is no passage there."));
            return; // nothing spent
        }
        if (!Cooldowns.ready(player, CD_PASSAGE, "Void Passage")) return;
        if (!StarDragon.pay(player, PASSAGE_COST, CD_PASSAGE)) return;
        Cooldowns.start(player, CD_PASSAGE, PASSAGE_COOLDOWN);
        passThroughVoid(player, sl, to);
    }

    /** What voidPassage does once it is paid for (a Beyonder mob casts it this way too). */
    static void passThroughVoid(LivingEntity player, ServerLevel sl, Vec3 to) {
        Vec3 from = player.position();
        LivingEntity ally = null;
        if (StarDragon.grown(player, PASSAGE_FAR_SEQUENCE)) {
            double best = PASSAGE_ALLY * PASSAGE_ALLY;
            for (LivingEntity one : Spells.alliesAround(player, sl, from, PASSAGE_ALLY)) {
                double d = one.distanceToSqr(player);
                if (d < best && !Root.has(one, RootRestriction.TELEPORT)) {
                    best = d;
                    ally = one;
                }
            }
        }
        LivingEntity companion = ally;
        StarDragonFx.voidPassage(sl, player, from, to);
        if (companion != null) StarDragonFx.voidPassage(sl, companion, companion.position(), to);
        exposeNear(player, sl, from);
        Later.run(sl, StarDragonFx.PASSAGE_ARRIVES, () -> {
            if (!Spells.casterStillHere(player, sl) || Root.has(player, RootRestriction.TELEPORT)) return;
            player.teleportTo(to.x, to.y, to.z);
            player.fallDistance = 0;
            FallGuard.protect(player, 40);
            Unnoticed.hide(player, PASSAGE_UNNOTICED);
            StarDragonFx.unnoticed(sl, player, PASSAGE_UNNOTICED, "sd_unnoticed_" + player.getId());
            StarDragonFx.status(player, Status.UNNOTICED, PASSAGE_UNNOTICED);
            exposeNear(player, sl, to);
            if (companion != null && companion.isAlive() && companion.level() == sl) {
                Vec3 beside = Spells.freeSpotNear(player, sl, to, Spells.rightOf(player), 1.2);
                Vec3 at = beside != null ? beside : to;
                companion.teleportTo(at.x, at.y, at.z);
                companion.fallDistance = 0;
                FallGuard.protect(companion, 40);
            }
        });
    }

    /** The hidden near a door of the void are forced out of hiding. */
    private static void exposeNear(LivingEntity player, ServerLevel sl, Vec3 at) {
        for (LivingEntity one : Spells.livingAround(player, sl, at, PASSAGE_EXPOSES)) {
            if (Spells.isFriend(player, one)) continue;
            if (!Concealment.isHidden(one) && !Unnoticed.isUnnoticed(one) && !one.hasEffect(MobEffects.INVISIBILITY)) continue;
            Exposed.apply(one, PASSAGE_EXPOSED);
            StarDragonFx.voidForcedOut(sl, one);
        }
    }

    // =========================
    // Fate Convergence: Fate bends toward you for FATE_TICKS (a Zone round you, FATE_RADIUS, once a second). Every
    // enemy in it is under Misfortune (StarDragonMarks.MISFORTUNE: misfires, stumbles, and under a quarter of its health
    // its future narrows); bosses are spared until the Condenser. Allies in it get Luck, and the first hit on each of
    // them in this cast is halved. Condenser: twice as long.
    // =========================
    static AbilityWheel.Result fateConvergence(LivingEntity player, ServerLevel sl) {
        boolean condenser = StarDragon.grown(player, FATE_FOREVER_SEQUENCE);
        int ticks = condenser ? Math.round(FATE_TICKS * FATE_LONGER) : FATE_TICKS;
        String tag = "sd_fate_" + player.getId();
        Set<UUID> warded = new HashSet<>();
        Zone.close(player, FATE);
        StarDragonFx.fateConvergence(sl, player, FATE_RADIUS, ticks, tag);
        StarDragonFx.status(player, Status.FATE_CONVERGENCE, ticks);
        Zone.around(player, FATE_RADIUS)
                .height(FATE_RADIUS)
                .lasts(ticks)
                .every(20)
                .affects(one -> true)
                .onInside((one, inside) -> {
                    if (Spells.isEnemy(player, one)) {
                        if (Spells.isBoss(one) && !condenser) return;
                        StarDragonMarks.put(one, Mark.MISFORTUNE, player, 40);
                        return;
                    }
                    Spells.keepEffect(one, MobEffects.LUCK, 0, 60);
                    if (warded.add(one.getUUID())) StarDragon.luckWard(one, Zone.ticksLeft(player, FATE) + 20);
                    if (inside % 60 == 0) StarDragonFx.luck(sl, one);
                })
                .onPulse(ticksLeft -> {
                    Spells.keepEffect(player, MobEffects.LUCK, 0, 60);
                    if (warded.add(player.getUUID())) StarDragon.luckWard(player, ticksLeft + 20);
                })
                .onEnd(() -> {
                    StarDragonFx.fateConvergenceEnd(sl, player, tag);
                    StarDragonFx.statusEnd(player, Status.FATE_CONVERGENCE);
                })
                .open(player, sl, FATE);
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Remote Sight: for SIGHT_TICKS you perceive far past your sight. Every second, for you alone: every living thing
    // within SIGHT_RANGE through walls, every door, trapdoor, gate and portal within SIGHT_PASSAGES as a passage, and
    // where an ender pearl in flight is going. At the cast, chat names how many there are and the strongest, with its
    // health.
    // =========================
    static AbilityWheel.Result remoteSight(ServerPlayer player, ServerLevel sl) {
        String tag = "sd_sight_" + player.getId();
        long cast = sl.getGameTime();
        player.getPersistentData().putLong(SIGHT_CAST, cast);
        StarDragonFx.remoteSight(sl, player, SIGHT_TICKS, tag);
        StarDragonFx.status(player, Status.REMOTE_SIGHT, SIGHT_TICKS);
        List<LivingEntity> seen = sl.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(SIGHT_RANGE),
                one -> one != player && one.isAlive() && !one.isSpectator());
        LivingEntity strongest = null;
        for (LivingEntity one : seen) if (strongest == null || one.getMaxHealth() > strongest.getMaxHealth()) strongest = one;
        player.sendSystemMessage(Component.literal(strongest == null ? "Nothing lives within your sight."
                : "You perceive " + seen.size() + " living things. The strongest: " + strongest.getDisplayName().getString()
                + " (" + Math.round(strongest.getHealth()) + "/" + Math.round(strongest.getMaxHealth()) + ")."));
        for (int t = 0; t < SIGHT_TICKS; t += 20) {
            Later.run(sl, t, () -> {
                if (!Spells.casterStillHere(player, sl) || player.getPersistentData().getLongOr(SIGHT_CAST, 0L) != cast) return;
                sight(player, sl);
            });
        }
        Later.run(sl, SIGHT_TICKS, () -> {
            if (player.getPersistentData().getLongOr(SIGHT_CAST, 0L) == cast) StarDragonFx.remoteSightEnd(sl, player, tag);
        });
        return AbilityWheel.Result.DONE;
    }

    private static void sight(ServerPlayer player, ServerLevel sl) {
        for (LivingEntity one : sl.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(SIGHT_RANGE),
                one -> one != player && one.isAlive() && !one.isSpectator())) {
            StarDragonFx.sighted(player, one);
        }
        Vec3 eye = player.getEyePosition();
        BlockPos middle = player.blockPosition();
        int r = SIGHT_PASSAGES;
        int shown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(middle.offset(-r, -r / 2, -r), middle.offset(r, r / 2, r))) {
            if (shown >= 12) break;
            BlockState state = sl.getBlockState(pos);
            if (state.isAir() || !isPassage(state)) continue;
            if (state.is(BlockTags.DOORS) && pos.getY() > 0 && sl.getBlockState(pos.below()).is(BlockTags.DOORS)) continue;   // a door's top half
            StarDragonFx.passage(player, eye.add(player.getLookAngle()), Vec3.atCenterOf(pos));
            shown++;
        }
        for (Projectile shot : sl.getEntitiesOfClass(Projectile.class, player.getBoundingBox().inflate(SIGHT_RANGE),
                one -> one.getType() == EntityTypes.ENDER_PEARL)) {
            StarDragonFx.passage(player, shot.position(), shot.position().add(shot.getDeltaMovement().scale(20)));
        }
    }

    private static boolean isPassage(BlockState state) {
        if (state.is(BlockTags.DOORS) || state.is(BlockTags.TRAPDOORS) || state.is(BlockTags.FENCE_GATES)) return true;
        String name = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return name.contains("portal") || name.contains("gateway");
    }

    // =========================
    // Quantum Uncertainty (the Cosmos wheel, Condenser): a partial authority over Quantum Mechanics. For QUANTUM_TICKS a
    // field QUANTUM_RADIUS round you is uncertain: enemies in it miss QUANTUM_MISS of the time (status/Falter), and a
    // teleport of anyone not on your side (an ender pearl, chorus fruit, an enderman) that would end in it lands at a
    // random spot up to QUANTUM_SLIP from where it meant to (teleport, from StarDragon's events). You and your allies
    // are unaffected.
    // =========================
    static AbilityWheel.Result quantumUncertainty(LivingEntity player, ServerLevel sl) {
        String tag = "sd_quantum_" + player.getId();
        Zone.close(player, QUANTUM);
        StarDragonFx.quantumUncertainty(sl, player, QUANTUM_RADIUS, QUANTUM_TICKS, tag);
        StarDragonFx.status(player, Status.QUANTUM, QUANTUM_TICKS);
        UUID owner = player.getUUID();
        QUANTUM_OWNERS.add(owner);
        Zone.around(player, QUANTUM_RADIUS)
                .height(QUANTUM_RADIUS)
                .lasts(QUANTUM_TICKS)
                .every(10)
                .onEnter(one -> {
                    if (one instanceof Player shown) StarDragonFx.status(shown, Status.UNCERTAIN, Zone.ticksLeft(player, QUANTUM));
                })
                .onInside((one, inside) -> {
                    Falter.apply(one, QUANTUM, QUANTUM_MISS, 20);
                    if (inside % 20 == 0) StarDragonFx.uncertain(sl, one);
                })
                .onLeave(one -> {
                    Falter.remove(one, QUANTUM);
                    if (one instanceof Player shown) StarDragonFx.statusEnd(shown, Status.UNCERTAIN);
                })
                .onEnd(() -> {
                    QUANTUM_OWNERS.remove(owner);
                    StarDragonFx.quantumEnd(sl, player, tag);
                    StarDragonFx.statusEnd(player, Status.QUANTUM);
                })
                .open(player, sl, QUANTUM);
        return AbilityWheel.Result.DONE;
    }

    /** A teleport that would end in someone's uncertain field (not one of theirs, not a command) slips aside. */
    static void teleport(EntityTeleportEvent event) {
        if (QUANTUM_OWNERS.isEmpty()) return;
        if (event instanceof EntityTeleportEvent.TeleportCommand || event instanceof EntityTeleportEvent.SpreadPlayersCommand) return;
        if (!(event.getEntity() instanceof LivingEntity one) || !(one.level() instanceof ServerLevel sl)) return;
        Vec3 meant = event.getTarget();
        for (UUID id : List.copyOf(QUANTUM_OWNERS)) {
            Player owner = sl.getPlayerByUUID(id);
            if (owner == null) continue;                                          // in another world: their field is too
            if (!Zone.isOpen(owner, QUANTUM)) {
                QUANTUM_OWNERS.remove(id);
                continue;
            }
            if (owner == one || Spells.isFriend(owner, one) || !Zone.covers(owner, QUANTUM, meant)) continue;
            double angle = sl.getRandom().nextDouble() * Math.PI * 2;
            double far = 2.0 + sl.getRandom().nextDouble() * (QUANTUM_SLIP - 2.0);
            Vec3 spot = Spells.groundAt(owner, sl, meant.add(Math.cos(angle) * far, 2.0, Math.sin(angle) * far));
            event.setTargetX(spot.x);
            event.setTargetY(spot.y);
            event.setTargetZ(spot.z);
            StarDragonFx.quantumSlip(sl, meant, spot);
            return;
        }
    }

    /** After a relog or a death. */
    static void clearState(LivingEntity player) {
        Zone.close(player, QUANTUM);
        QUANTUM_OWNERS.remove(player.getUUID());
        Dome.close(player, VOID);
        Zone.close(player, FATE);
        player.getPersistentData().remove(SIGHT_CAST);
    }
}
