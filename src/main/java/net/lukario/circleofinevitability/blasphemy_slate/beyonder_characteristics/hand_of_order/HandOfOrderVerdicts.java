package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PathwayAbilities;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.AbilityLock;
import net.lukario.circleofinevitability.status.Exile;
import net.lukario.circleofinevitability.status.Forgotten;
import net.lukario.circleofinevitability.status.KeySeal;
import net.lukario.circleofinevitability.status.Silence;
import net.lukario.circleofinevitability.status.Substitute;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hand Of Order, key 4: the Verdict wheel (Judge, Sequence 6). docs/HAND_OF_ORDER.md, "Key 4 wheel". Verdicts are one
 * spoken word; each shows its word above you (HandOfOrderFx.speak).
 *
 *   Imprison           layers of glass walls hold the enemy you look at; again on the same one: "Release", free.
 *                      From Imperative Mage (4): Confinement, a sealed cell, longer, its stand-ins fail
 *   Exile              an invisible force throws it far away. From 4: into a void for a while, back on the same spot.
 *                      From Chaos Hunter (3): a hurricane also throws away everything near it
 *   Death              a rush and a blow to the head it cannot dodge, then stunned. From Disciplinary Paladin (5): at
 *                      its latest wound, harder, and the wound opens
 *   Maintain Secrecy   it must obey and reveal nothing: no abilities (status/Silence); a mob loses its special attacks
 *                      and forgets you
 *   Deprivation (4)    the power it used last is dead for a while (status/KeySeal; a mob: status/AbilityLock). From
 *                      Chaos Hunter (3): up to three powers
 *   Execution (4)      the fist is raised while a ring closes on the target; a blow you take breaks it; then it falls
 *                      from any distance in view. Under 30% health it is simply killed
 *
 * Sneak casts the last pick again without the wheel (no cooldown, double the cost). Every entry's cost goes through
 * HandOfOrder.costFor, every cooldown through cooldownFor. The looks are HandOfOrderFx's; every number is right below.
 */
public final class HandOfOrderVerdicts {

    private HandOfOrderVerdicts() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int VERDICT_SEQUENCE = 6;                       // the key
    private static final float QUICK_CAST_COST = HandOfOrder.QUICK_CAST_COST;

    // Imprison / Confinement
    private static final int IMPRISON_SEQUENCE = 6;
    private static final int IMPRISON_COST = 2450;
    private static final int IMPRISON_COOLDOWN = 400;
    private static final double IMPRISON_RANGE = 20.0;
    private static final int IMPRISON_TICKS = 100;
    private static final int CONFINEMENT_SEQUENCE = 4;
    private static final int CONFINEMENT_TICKS = 160;

    // Exile
    private static final int EXILE_SEQUENCE = 6;
    private static final int EXILE_COST = 2450;
    private static final int EXILE_COOLDOWN = 400;
    private static final double EXILE_RANGE = 20.0;
    private static final double EXILE_PUSH = 2.4;               // blocks a tick: about 20 blocks away...
    private static final double EXILE_LIFT = 0.5;
    private static final double EXILE_SHOWN = 20.0;             // (where the streak is drawn to)
    private static final int EXILE_SLOW_TICKS = 40;             // ...then Slowness II this long
    private static final int VOID_SEQUENCE = 4;                 // Imperative Mage: into a void instead...
    private static final int VOID_TICKS = 80;                   // ...for this long
    private static final int HURRICANE_SEQUENCE = 3;            // Chaos Hunter: everything this close to it...
    private static final double HURRICANE_RADIUS = 5.0;
    private static final double HURRICANE_PUSH = 1.6;           // ...is thrown away too

    // Death
    private static final int DEATH_SEQUENCE = 6;
    private static final int DEATH_COST = 2450;
    private static final int DEATH_COOLDOWN = 500;
    private static final double DEATH_RANGE = 14.0;             // the rush reaches this far
    private static final float DEATH_DAMAGE = 60f;              // through everything (Spells.strikeTrue)
    private static final int DEATH_STUN = 30;
    private static final int WOUND_SEQUENCE = 5;                // Disciplinary Paladin: at its latest wound...
    private static final float WOUND_MORE = 1.5f;               // ...this much harder...
    private static final int WOUND_TICKS = 100;                 // ...and the wound opens (Wither) this long

    // Maintain Secrecy
    private static final int SECRECY_SEQUENCE = 6;
    private static final int SECRECY_COST = 1800;
    private static final int SECRECY_COOLDOWN = 600;
    private static final double SECRECY_RANGE = 12.0;
    private static final int SECRECY_TICKS = 160;
    private static final int SECRECY_FORGET_TICKS = 80;         // a mob also forgets you this long

    // Deprivation
    private static final int DEPRIVATION_SEQUENCE = 4;
    private static final int DEPRIVATION_COST = 6000;
    private static final int DEPRIVATION_COOLDOWN = 800;
    private static final double DEPRIVATION_RANGE = 24.0;
    private static final int DEPRIVATION_TICKS = 600;
    private static final int DEPRIVATION_BOSS_TICKS = 300;
    private static final int DEPRIVE_MORE_SEQUENCE = 3;         // Chaos Hunter: up to this many powers
    private static final int DEPRIVE_MORE = 3;

    // Execution
    private static final int EXECUTION_SEQUENCE = 4;
    private static final int EXECUTION_COST = 6000;
    private static final int EXECUTION_COOLDOWN = 1200;
    private static final double EXECUTION_RANGE = 32.0;
    private static final float EXECUTION_DAMAGE = 120f;         // through everything
    private static final float EXECUTION_THRESHOLD = 0.3f;      // under this much health it is simply killed...
    private static final float EXECUTION_STRONG = 2f;           // ...a player or a boss takes this many times the damage instead

    // =========================
    // What is kept (not saved: a restart ends every hold)
    // =========================

    /** Who is held by Imprison: by whom, under which tag (its look), and which hold (so an old end does not end a new one). */
    private record Held(UUID caster, String tag, long since) {}
    private static final Map<UUID, Held> IMPRISONED = new HashMap<>();

    /** An Execution being wound up: the caster -> its target and tag. */
    private record Pending(UUID target, String tag) {}
    private static final Map<UUID, Pending> EXECUTIONS = new HashMap<>();

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("hand_of_order_verdict", "Verdict")
            .radius(78)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .cooldownBy(HandOfOrder::cooldownFor)
            .entry("imprison", HandOfOrderFx.WHEEL_IMPRISON, "Imprison", IMPRISON_SEQUENCE,
                    IMPRISON_COST, IMPRISON_COOLDOWN, HandOfOrderVerdicts::imprison)
            .costBy(HandOfOrder::costFor)
            .entry("exile", HandOfOrderFx.WHEEL_EXILE, "Exile", EXILE_SEQUENCE,
                    EXILE_COST, EXILE_COOLDOWN, HandOfOrderVerdicts::exile)
            .costBy(HandOfOrder::costFor)
            .entry("death", HandOfOrderFx.WHEEL_DEATH, "Death", DEATH_SEQUENCE,
                    DEATH_COST, DEATH_COOLDOWN, HandOfOrderVerdicts::death)
            .costBy(HandOfOrder::costFor)
            .entry("maintain_secrecy", HandOfOrderFx.WHEEL_MAINTAIN_SECRECY, "Maintain Secrecy", SECRECY_SEQUENCE,
                    SECRECY_COST, SECRECY_COOLDOWN, HandOfOrderVerdicts::maintainSecrecy)
            .costBy(HandOfOrder::costFor)
            .entry("deprivation", HandOfOrderFx.WHEEL_DEPRIVATION, "Deprivation", DEPRIVATION_SEQUENCE,
                    DEPRIVATION_COST, DEPRIVATION_COOLDOWN, HandOfOrderVerdicts::deprivation)
            .costBy(HandOfOrder::costFor)
            .entry("execution", HandOfOrderFx.WHEEL_EXECUTION, "Execution", EXECUTION_SEQUENCE,
                    EXECUTION_COST, EXECUTION_COOLDOWN, HandOfOrderVerdicts::execution)
            .costBy(HandOfOrder::costFor);

    // =========================
    // Ability 4 - Verdict
    // Normal: the Verdict wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void handOfOrderVerdict(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!HandOfOrder.ready(player, bypassClassCheck, VERDICT_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Imprison: the enemy you look at (IMPRISON_RANGE) is held completely (Root, EVERYTHING) for IMPRISON_TICKS once
    // the walls close (HandOfOrderFx.IMPRISON_LANDS). Cast again at one you hold (an ally too): "Release", it is free
    // and costs nothing. Imperative Mage (4): Confinement, a sealed cell for CONFINEMENT_TICKS; a Substitute on it
    // is spent for nothing (stand-ins fail inside).
    // =========================
    private static AbilityWheel.Result imprison(ServerPlayer player, ServerLevel sl) {
        LivingEntity held = heldByMeInSight(player, sl);
        if (held != null) {
            Held hold = IMPRISONED.remove(held.getUUID());
            Root.remove(held, RootRestriction.values());
            HandOfOrderFx.release(sl, held, hold.tag());
            return AbilityWheel.Result.FREE;
        }
        LivingEntity target = Spells.aimEnemy(player, sl, IMPRISON_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one to imprison."));
            return AbilityWheel.Result.REFUSED;
        }
        boolean confinement = Beyonder.hasSequence(player, CONFINEMENT_SEQUENCE);
        int ticks = confinement ? CONFINEMENT_TICKS : IMPRISON_TICKS;
        String tag = "ho_imprison_" + target.getId() + "_" + sl.getGameTime();
        Held old = IMPRISONED.remove(target.getUUID());
        if (old != null) HandOfOrderFx.imprisonEnd(sl, target, old.tag());              // new walls over old ones
        if (confinement) HandOfOrderFx.confinement(sl, player, target, ticks, tag);
        else HandOfOrderFx.imprison(sl, player, target, ticks, tag);
        Held hold = new Held(player.getUUID(), tag, sl.getGameTime());
        IMPRISONED.put(target.getUUID(), hold);
        Later.run(sl, HandOfOrderFx.IMPRISON_LANDS, () -> {
            if (IMPRISONED.get(target.getUUID()) != hold || !target.isAlive() || target.level() != sl) return;
            Root.apply(target, ticks, RootRestriction.EVERYTHING);
            if (confinement) Substitute.disarm(target);
        });
        Later.run(sl, HandOfOrderFx.IMPRISON_LANDS + ticks, () -> {
            if (IMPRISONED.get(target.getUUID()) != hold) return;                         // released, or held anew
            IMPRISONED.remove(target.getUUID());
            HandOfOrderFx.imprisonEnd(sl, target, tag);
        });
        return AbilityWheel.Result.DONE;
    }

    /** The one this player holds in Imprison and is looking at (enemy or ally), or null. */
    private static LivingEntity heldByMeInSight(ServerPlayer player, ServerLevel sl) {
        for (LivingEntity looked : new LivingEntity[]{Spells.aimEnemy(player, sl, IMPRISON_RANGE), Spells.aimAlly(player, sl, IMPRISON_RANGE)}) {
            if (looked == null) continue;
            Held hold = IMPRISONED.get(looked.getUUID());
            if (hold != null && hold.caster().equals(player.getUUID())) return looked;
        }
        return null;
    }

    // =========================
    // Exile: the enemy you look at (EXILE_RANGE) is thrown away from you at HandOfOrderFx.EXILE_LANDS, about
    // EXILE_SHOWN blocks, then slowed for EXILE_SLOW_TICKS. Imperative Mage (4): instead it is put into a void for
    // VOID_TICKS (status/Exile: unseen, unhurt, unable to act) and comes back on the same spot. Chaos Hunter (3): a
    // hurricane also throws away every other enemy within HURRICANE_RADIUS of it.
    // =========================
    private static AbilityWheel.Result exile(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, EXILE_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one to exile."));
            return AbilityWheel.Result.REFUSED;
        }
        boolean intoVoid = Beyonder.hasSequence(player, VOID_SEQUENCE);
        boolean hurricane = Beyonder.hasSequence(player, HURRICANE_SEQUENCE);
        Vec3 away = target.position().subtract(player.position());
        Vec3 flat = new Vec3(away.x, 0, away.z);
        Vec3 direction = flat.lengthSqr() < 1.0E-4 ? Spells.flatLook(player) : flat.normalize();
        HandOfOrderFx.exile(sl, player, target, target.position().add(direction.scale(EXILE_SHOWN)));
        Later.run(sl, HandOfOrderFx.EXILE_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            Vec3 at = target.position();
            if (hurricane) {
                HandOfOrderFx.exileHurricane(sl, at, HURRICANE_RADIUS);
                for (LivingEntity near : Spells.enemiesAround(player, sl, at, HURRICANE_RADIUS)) {
                    if (near != target) Spells.push(near, at, HURRICANE_PUSH, EXILE_LIFT * 0.6);
                }
            }
            if (intoVoid) {
                String tag = "ho_void_" + target.getId() + "_" + sl.getGameTime();
                Vec3 chest = target.getBoundingBox().getCenter();
                HandOfOrderFx.exileVoid(sl, target, VOID_TICKS, tag);
                Exile.banish(target, VOID_TICKS, at, (who, level) -> HandOfOrderFx.exileReturn(level, chest, tag));
                return;
            }
            Spells.push(target, player.position(), EXILE_PUSH, EXILE_LIFT);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, EXILE_SLOW_TICKS, 1));
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Death: the enemy you look at (DEATH_RANGE). You rush to stand before it and, at HandOfOrderFx.DEATH_LANDS, strike
    // its head: DEATH_DAMAGE that nothing softens or dodges (Spells.strikeTrue), stunned (held) for DEATH_STUN.
    // Disciplinary Paladin (5): aimed at its latest wound, WOUND_MORE as hard, and the wound opens (Wither, WOUND_TICKS).
    // Not while you are held in place.
    // =========================
    private static AbilityWheel.Result death(ServerPlayer player, ServerLevel sl) {
        if (Root.has(player, RootRestriction.MOVE) || Root.has(player, RootRestriction.TELEPORT)) {
            player.sendSystemMessage(Component.literal("You cannot move."));
            return AbilityWheel.Result.REFUSED;
        }
        LivingEntity target = Spells.aimEnemy(player, sl, DEATH_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one to declare dead."));
            return AbilityWheel.Result.REFUSED;
        }
        Vec3 stand = strideSpot(player, sl, target);
        if (stand == null) {
            player.sendSystemMessage(Component.literal("There is no room to stand before it."));
            return AbilityWheel.Result.REFUSED;
        }
        boolean wound = Beyonder.hasSequence(player, WOUND_SEQUENCE);
        Vec3 from = player.position();
        player.teleportTo(stand.x, stand.y, stand.z);
        player.fallDistance = 0;
        HandOfOrderFx.death(sl, player, from, target, wound);
        Later.run(sl, HandOfOrderFx.DEATH_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            Spells.strikeTrue(player, target, wound ? DEATH_DAMAGE * WOUND_MORE : DEATH_DAMAGE);
            if (!target.isAlive()) return;
            Root.apply(target, DEATH_STUN, RootRestriction.EVERYTHING);
            if (target instanceof Player stunned) HandOfOrderFx.status(stunned, HandOfOrderFx.Status.STUNNED, DEATH_STUN);
            if (wound) target.addEffect(new MobEffectInstance(MobEffects.WITHER, WOUND_TICKS, 1));
        });
        return AbilityWheel.Result.DONE;
    }

    /** Where to stand to strike: just before the target, on the side facing you. Null: no room. */
    static Vec3 strideSpot(Player player, ServerLevel sl, LivingEntity target) {
        Vec3 back = player.position().subtract(target.position());
        Vec3 flat = new Vec3(back.x, 0, back.z);
        if (flat.lengthSqr() < 1.0E-4) return player.position();
        return Spells.freeSpotNear(player, sl, target.position(), flat.normalize(), target.getBbWidth() * 0.5 + 1.2);
    }

    // =========================
    // Maintain Secrecy: the enemy you look at (SECRECY_RANGE) must obey and reveal nothing for SECRECY_TICKS: a player
    // cannot use abilities (status/Silence); a mob loses its special attacks (status/AbilityLock) and forgets you for
    // SECRECY_FORGET_TICKS (status/Forgotten).
    // =========================
    private static AbilityWheel.Result maintainSecrecy(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, SECRECY_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one to silence."));
            return AbilityWheel.Result.REFUSED;
        }
        String tag = "ho_secrecy_" + target.getId();
        HandOfOrderFx.maintainSecrecy(sl, player, target, SECRECY_TICKS, tag);
        if (target instanceof Player) {
            Silence.apply(target, SECRECY_TICKS);
        } else {
            AbilityLock.apply(target, SECRECY_TICKS);
            if (target instanceof Mob mob) Forgotten.apply(mob, player, SECRECY_FORGET_TICKS);
        }
        Later.run(sl, SECRECY_TICKS, () -> HandOfOrderFx.secrecyEnd(sl, target, tag));
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Deprivation (Imperative Mage, 4): the enemy you look at (DEPRIVATION_RANGE) loses the power it used last for
    // DEPRIVATION_TICKS: a player's last ability key is sealed (status/KeySeal; from Chaos Hunter the last DEPRIVE_MORE
    // different keys); a mob loses its special attacks (status/AbilityLock; a boss for DEPRIVATION_BOSS_TICKS).
    // Nothing is taken away, only made unusable. A player who has used no power yet cannot be deprived: nothing spent.
    // =========================
    private static AbilityWheel.Result deprivation(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, DEPRIVATION_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one to deprive."));
            return AbilityWheel.Result.REFUSED;
        }
        int ticks;
        int count;
        if (target instanceof Player victim) {
            List<Integer> keys = KeySeal.lastUsed(victim, Beyonder.hasSequence(player, DEPRIVE_MORE_SEQUENCE) ? DEPRIVE_MORE : 1);
            if (keys.isEmpty()) {
                player.sendSystemMessage(Component.literal(victim.getDisplayName().getString() + " has used no power you could take."));
                return AbilityWheel.Result.REFUSED;
            }
            ticks = DEPRIVATION_TICKS;
            count = keys.size();
            List<String> names = new ArrayList<>();
            for (int key : keys) {
                KeySeal.seal(victim, key, ticks);
                names.add(abilityName(victim, key));
            }
            player.sendSystemMessage(Component.literal("You strip " + String.join(", ", names) + " from " + victim.getDisplayName().getString() + "."));
        } else {
            ticks = HandOfOrder.isBoss(target) ? DEPRIVATION_BOSS_TICKS : DEPRIVATION_TICKS;
            if (!AbilityLock.apply(target, ticks)) {
                player.sendSystemMessage(Component.literal("Its powers cannot be taken."));
                return AbilityWheel.Result.REFUSED;
            }
            count = 1;
        }
        String tag = "ho_deprived_" + target.getId();
        HandOfOrderFx.deprivation(sl, player, target, count);
        Later.run(sl, HandOfOrderFx.DEPRIVED, () -> {
            if (target.isAlive()) HandOfOrderFx.deprived(sl, target, ticks, tag);
        });
        Later.run(sl, HandOfOrderFx.DEPRIVED + ticks, () -> HandOfOrderFx.deprivedEnd(sl, target, tag));
        return AbilityWheel.Result.DONE;
    }

    /** What a player's key is called in their own Pathway (PathwayAbilities), or "key N". */
    private static String abilityName(Player player, int key) {
        for (PathwayAbilities.Ability ability : PathwayAbilities.of(SoulCore.getPathway(player))) {
            if (ability.key() == key) return ability.name();
        }
        return "key " + key;
    }

    // =========================
    // Execution (Imperative Mage, 4): the enemy you look at (EXECUTION_RANGE). The fist is raised and a ring closes on
    // it for HandOfOrderFx.EXECUTION_LANDS; any damage you take meanwhile breaks it (casterHurt). Then, if it is still
    // in view and in range: EXECUTION_DAMAGE that nothing softens; under EXECUTION_THRESHOLD of its health it is simply
    // killed (a player or a boss takes EXECUTION_STRONG times the damage instead).
    // =========================
    private static AbilityWheel.Result execution(ServerPlayer player, ServerLevel sl) {
        if (EXECUTIONS.containsKey(player.getUUID())) {
            player.sendSystemMessage(Component.literal("Your fist is raised already."));
            return AbilityWheel.Result.REFUSED;
        }
        LivingEntity target = Spells.aimEnemy(player, sl, EXECUTION_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one to execute."));
            return AbilityWheel.Result.REFUSED;
        }
        String tag = "ho_execution_" + player.getId() + "_" + sl.getGameTime();
        Pending pending = new Pending(target.getUUID(), tag);
        EXECUTIONS.put(player.getUUID(), pending);
        HandOfOrderFx.execution(sl, player, target, tag);
        Later.run(sl, HandOfOrderFx.EXECUTION_LANDS, () -> {
            if (EXECUTIONS.get(player.getUUID()) != pending) return;                       // broken, or the player left
            EXECUTIONS.remove(player.getUUID());
            if (!Spells.casterStillHere(player, sl)) return;
            if (!target.isAlive() || target.level() != sl || target.distanceTo(player) > EXECUTION_RANGE
                    || !player.hasLineOfSight(target)) {
                player.sendSystemMessage(Component.literal("Your Verdict found no one to fall on."));
                HandOfOrderFx.executionBroken(sl, player, target, tag);
                return;
            }
            boolean low = target.getHealth() < target.getMaxHealth() * EXECUTION_THRESHOLD;
            boolean strong = target instanceof Player || HandOfOrder.isBoss(target);
            boolean slain = low && !strong;
            HandOfOrderFx.executionStrike(sl, player, target, slain);
            float damage = slain ? target.getHealth() + target.getAbsorptionAmount() + EXECUTION_DAMAGE
                    : low ? EXECUTION_DAMAGE * EXECUTION_STRONG : EXECUTION_DAMAGE;
            Spells.strikeTrue(player, target, damage);
        });
        return AbilityWheel.Result.DONE;
    }

    /** The caster took damage: an Execution being wound up breaks. */
    static void casterHurt(Player player, ServerLevel sl) {
        Pending pending = EXECUTIONS.remove(player.getUUID());
        if (pending == null) return;
        LivingEntity target = sl.getEntity(pending.target()) instanceof LivingEntity found ? found : null;
        HandOfOrderFx.executionBroken(sl, player, target, pending.tag());
        player.sendSystemMessage(Component.literal("The blow broke your Execution."));
    }

    /** The player logged out, died or relogged: nothing of theirs is left waiting. */
    static void forget(Player player) {
        EXECUTIONS.remove(player.getUUID());
    }
}
