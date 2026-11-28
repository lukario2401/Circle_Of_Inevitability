package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer.ObserverFx.Status;
import net.lukario.circleofinevitability.combat.Countdown;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.Enclosure;
import net.lukario.circleofinevitability.combat.Gaze;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Painting;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Teleport;
import net.lukario.circleofinevitability.combat.Territory;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.AbilityLock;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.ConjuredArmor;
import net.lukario.circleofinevitability.status.Construct;
import net.lukario.circleofinevitability.status.DeathWard;
import net.lukario.circleofinevitability.status.Dispel;
import net.lukario.circleofinevitability.status.Exile;
import net.lukario.circleofinevitability.status.Forgotten;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.NoHeal;
import net.lukario.circleofinevitability.status.Observation;
import net.lukario.circleofinevitability.status.Phase;
import net.lukario.circleofinevitability.status.Silence;
import net.lukario.circleofinevitability.status.Slumber;
import net.lukario.circleofinevitability.status.Vulnerable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Key 7, the Sublunary wheel (docs/pathways/observer/OBSERVER.md, "Key 7: Sublunary"): Foregone Loss, Foregone Survival
 * and Higher Revival (Observer 1), Bloom, Fall Silent and Erase (Sublunary Eye 0). Foregone Loss and Erase need
 * Observation 3 on the one they are aimed at; without it nothing is spent.
 */
final class ObserverSublunary {

    private ObserverSublunary() {}

    // =========================
    // Foregone Loss (Observer 1)
    // At an enemy at Observation 3 (LOSS_RANGE): for LOSS_TICKS it cannot heal and takes LOSS_MORE more from everything.
    // When the time ends: under LOSS_LINE of its health it falls (a true hit for what is left). A boss loses
    // LOSS_BOSS of its maximum health instead, a player LOSS_PLAYER.
    // =========================
    static final int LOSS_SEQUENCE = Observer.OBSERVER;
    static final int LOSS_COST = 30000;
    static final double LOSS_RANGE = 32.0;
    static final int LOSS_TICKS = 240;
    static final float LOSS_MORE = 0.25f;
    static final float LOSS_LINE = 0.35f;
    static final float LOSS_BOSS = 0.15f;
    static final float LOSS_PLAYER = 0.20f;

    // =========================
    // Foregone Survival (Observer 1)
    // An ally in sight (or you): cleansed and warded for SURVIVAL_TICKS; the next death does not happen and they come
    // back at SURVIVAL_HEALTH. It can be dispelled, and a Decree against resurrection stops it.
    // =========================
    static final int SURVIVAL_SEQUENCE = Observer.OBSERVER;
    static final int SURVIVAL_COST = 25000;
    static final double SURVIVAL_RANGE = 32.0;
    static final int SURVIVAL_TICKS = 1200;
    static final float SURVIVAL_HEALTH = 0.6f;
    static final String SURVIVAL = "observer_foregone_survival";      // DeathWard's ability name
    static final String SURVIVAL_TAG = "observer_survival_tag";

    // =========================
    // Higher Revival (Observer 1)
    // REVIVAL_RITUAL seconds hidden in Phase (leaving it breaks the ritual, nothing spent), then a sealed ward for
    // REVIVAL_TICKS (status/DeathWard.armSealed): you return hidden for REVIVAL_HIDDEN at REVIVAL_HEALTH, at your
    // Totem if your Territory is open, else where you fell.
    // =========================
    static final int REVIVAL_SEQUENCE = Observer.OBSERVER;
    static final int REVIVAL_COST = 45000;
    static final int REVIVAL_RITUAL = 8;
    static final int REVIVAL_TICKS = 12000;
    static final float REVIVAL_HEALTH = 0.8f;
    static final int REVIVAL_HIDDEN = 60;
    static final String REVIVAL = "observer_higher_revival";          // DeathWard's ability name and the Phase's source
    static final String REVIVAL_TAG = "observer_revival_tag";

    // =========================
    // Bloom (Sublunary Eye 0)
    // For BLOOM_TICKS every ally you see within BLOOM_RANGE (and you): Regeneration IV, a cleanse every second, 30% less
    // damage and BLOOM_ABSORPTION absorption (status/ConjuredArmor, once per sighting), flowers under them. They cannot
    // be silenced or locked while you see them; leaving your sight takes it off. It ends if you are blinded.
    // =========================
    static final int BLOOM_SEQUENCE = Observer.SUBLUNARY_EYE;
    static final int BLOOM_COST = 60000;                   // under the 78400 a full bar holds at 0
    static final int BLOOM_TICKS = 400;
    static final double BLOOM_RANGE = 48.0;
    static final double SIGHT_ANGLE = 60.0;                  // "in your sight": this far off the crosshair, clear line
    static final float BLOOM_CUT = 0.3f;
    static final float BLOOM_ABSORPTION = 20f;
    static final String BLOOM_ARMOR = "observer_bloom";

    // =========================
    // Fall Silent (Sublunary Eye 0)
    // For SILENT_TICKS every enemy within SILENT_RANGE that is out of your sight falls silent: no abilities, no attacks,
    // limp, and it forgets you. Worked out every second: who comes into view wakes, who leaves it falls silent again.
    // Silent the whole time: erased for SILENT_ERASE when it ends (half for a boss).
    // =========================
    static final int SILENT_SEQUENCE = Observer.SUBLUNARY_EYE;
    static final int SILENT_COST = 60000;
    static final int SILENT_TICKS = 200;
    static final double SILENT_RANGE = 48.0;
    static final int SILENT_HOLD = 30;                       // each second's silence lasts this long (renewed while out of sight)
    static final int SILENT_ERASE = 160;

    // =========================
    // Erase (Sublunary Eye 0)
    // At an enemy at Observation 3 (ERASE_RANGE): erased from the world for ERASE_TICKS (Spells.holdLength), back where
    // it stood. At nothing: everything conjured of the enemy side within ERASE_RADIUS of where you aim is unmade.
    // =========================
    static final int ERASE_SEQUENCE = Observer.SUBLUNARY_EYE;
    static final int ERASE_COST = 50000;
    static final double ERASE_RANGE = 48.0;
    static final int ERASE_TICKS = 200;
    static final double ERASE_RADIUS = 20.0;

    /** A Bloom or a Fall Silent under way: until when, its look's tag, and who it holds. */
    private static final class Eye {
        final long until;
        final String tag;
        final Set<UUID> held = new HashSet<>();
        final Map<UUID, Integer> silentFor = new HashMap<>();
        final Map<UUID, String> wilted = new HashMap<>();

        Eye(long until, String tag) {
            this.until = until;
            this.tag = tag;
        }
    }

    private static final Map<UUID, Eye> BLOOMS = new HashMap<>();
    private static final Map<UUID, Eye> SILENCES = new HashMap<>();

    static {
        // nothing silences or locks the ones a Bloom holds
        AbilityLock.addImmunity(ObserverSublunary::bloomed);
    }

    private static boolean bloomed(LivingEntity entity) {
        for (Eye eye : BLOOMS.values()) if (eye.held.contains(entity.getUUID())) return true;
        return false;
    }

    /** The target in the crosshair if it is Seen Through; else null, told, and the cast is not paid. */
    private static LivingEntity seenThrough(ServerPlayer player, ServerLevel sl, double range) {
        LivingEntity target = Spells.aimEnemy(player, sl, range);
        if (target == null) return null;
        if (Observation.level(player, target) < Observation.MAX) {
            ObserverFx.notWatchedEnough(player);
            Spells.tell(player, Component.literal("You have not watched it long enough."));
            return null;
        }
        return target;
    }

    // =========================
    // Foregone Loss
    // =========================

    static AbilityWheel.Result foregoneLoss(ServerPlayer player, ServerLevel sl) {
        LivingEntity aimed = Spells.aimEnemy(player, sl, LOSS_RANGE);
        if (aimed == null) {
            Spells.tell(player, Component.literal("There is no one in sight whose ending you could write."));
            return AbilityWheel.Result.REFUSED;
        }
        LivingEntity target = seenThrough(player, sl, LOSS_RANGE);
        if (target == null) return AbilityWheel.Result.REFUSED;
        String tag = ObserverFx.tagFor("foregone_loss");
        NoHeal.apply(target, LOSS_TICKS);
        Vulnerable.add(target, LOSS_MORE, LOSS_MORE, LOSS_TICKS);
        ObserverFx.foregoneLoss(sl, player, target, LOSS_TICKS, tag);
        ObserverFx.status(target, Status.FOREGONE_LOSS, LOSS_TICKS);
        Later.run(sl, LOSS_TICKS, () -> {
            ObserverFx.statusEnd(target, Status.FOREGONE_LOSS);
            if (!target.isAlive() || target.level() != sl) return;
            if (!Spells.casterStillHere(player, sl)) {
                ObserverFx.foregoneSpared(sl, target, tag);
                return;
            }
            float share = Spells.isBoss(target) ? LOSS_BOSS : target instanceof Player ? LOSS_PLAYER : 0f;
            if (share > 0f) {
                ObserverFx.foregoneFall(sl, target, tag);
                Spells.strikeTrue(player, target, target.getMaxHealth() * share);
            } else if (target.getHealth() < target.getMaxHealth() * LOSS_LINE) {
                ObserverFx.foregoneFall(sl, target, tag);
                Spells.strikeTrue(player, target, target.getHealth() + target.getAbsorptionAmount() + 1f);
            } else {
                ObserverFx.foregoneSpared(sl, target, tag);
            }
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Foregone Survival
    // =========================

    static AbilityWheel.Result foregoneSurvival(ServerPlayer player, ServerLevel sl) {
        LivingEntity ally = Spells.aimAlly(player, sl, SURVIVAL_RANGE);
        LivingEntity who = ally != null ? ally : player;
        if (DeathWard.isSealed(who)) {
            Spells.tell(player, Component.literal("A higher ending is already written for them."));
            return AbilityWheel.Result.REFUSED;
        }
        String tag = ObserverFx.tagFor("foregone_survival");
        Object ward = DeathWard.armWhen(who, SURVIVAL, SURVIVAL_TICKS, SURVIVAL_HEALTH, DeathWard.GRACE_TICKS, () -> true,
                (saved, source, level) -> {
                    ObserverFx.wardSpent(level, saved, saved.getPersistentData().getStringOr(SURVIVAL_TAG, ""));
                    saved.getPersistentData().remove(SURVIVAL_TAG);
                    ObserverFx.statusEnd(saved, Status.FOREGONE_SURVIVAL);
                });
        if (ward == null) {
            Spells.tell(player, Component.literal("Something here forbids the dead from returning."));
            return AbilityWheel.Result.REFUSED;
        }
        Spells.cleanse(who);
        who.getPersistentData().putString(SURVIVAL_TAG, tag);
        ObserverFx.foregoneSurvival(sl, player, who, SURVIVAL_TICKS, tag);
        ObserverFx.status(who, Status.FOREGONE_SURVIVAL, SURVIVAL_TICKS);
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Higher Revival
    // =========================

    static boolean inRitual(Player player) {
        return REVIVAL.equals(Phase.source(player)) && Countdown.isRunning(player);
    }

    static AbilityWheel.Result higherRevival(ServerPlayer player, ServerLevel sl) {
        if (Countdown.isRunning(player)) {
            Spells.tell(player, Component.literal("You are already in the middle of something."));
            return AbilityWheel.Result.REFUSED;
        }
        boolean ownPhase = ObserverDimension.isPhased(player);
        if (!ownPhase && !Phase.enter(player, REVIVAL_RITUAL * 20 + 20, REVIVAL)) {
            Spells.tell(player, Component.literal("Something here forbids slipping out of the world."));
            return AbilityWheel.Result.REFUSED;
        }
        if (!ownPhase) ObserverFx.phaseIn(sl, player);
        String tag = ObserverFx.tagFor("revival_ritual");
        ObserverFx.revivalRitual(sl, player, REVIVAL_RITUAL * 20, tag);
        Countdown.seconds(REVIVAL_RITUAL)
                .colors(0xFFE9B0, 0xFFD66E)
                .keepWhile(() -> player.isAlive() && Phase.isPhased(player))
                .onFinish(() -> {
                    if (REVIVAL.equals(Phase.source(player))) Phase.leave(player);
                    if (!Observer.pay(player, REVIVAL_COST, "higher_revival")) {
                        ObserverFx.revivalArmed(sl, player, tag);
                        return;
                    }
                    armRevival(player, sl);
                    ObserverFx.revivalArmed(sl, player, tag);
                })
                .onCancel(() -> {
                    if (REVIVAL.equals(Phase.source(player))) Phase.leave(player);
                    ObserverFx.revivalArmed(sl, player, tag);
                    Spells.tell(player, Component.literal("You left the Phase: the ritual is broken and nothing is spent."));
                })
                .start(player);
        return AbilityWheel.Result.FREE;                          // paid when the ritual is done
    }

    private static void armRevival(ServerPlayer player, ServerLevel sl) {
        String tag = ObserverFx.tagFor("higher_revival");
        DeathWard.armSealed(player, REVIVAL, REVIVAL_TICKS, REVIVAL_HEALTH, REVIVAL_HIDDEN, (saved, source, level) -> {
            ObserverFx.statusEnd(saved, Status.HIGHER_REVIVAL);
            Vec3 at = saved.position();
            if (saved instanceof Player owner && Territory.isOpen(owner) && Territory.center(owner) != null) {
                Vec3 home = Teleport.standNear(saved, level, Territory.center(owner).add(1.5, 0, 0), 4);
                if (home != null && Teleport.of(saved).ignoringTeleportRoots().fallGuard(60).go(level, home)) at = home;
            }
            Spells.cleanse(saved);
            Concealment.hide(saved, REVIVAL_HIDDEN);
            ObserverFx.higherRevival(level, saved, at);
        });
        ObserverFx.status(player, Status.HIGHER_REVIVAL, REVIVAL_TICKS);
        player.getPersistentData().putString(REVIVAL_TAG, tag);
    }

    // =========================
    // Bloom
    // =========================

    static AbilityWheel.Result bloom(ServerPlayer player, ServerLevel sl) {
        if (blinded(player)) {
            Spells.tell(player, Component.literal("You cannot see: nothing would bloom."));
            return AbilityWheel.Result.REFUSED;
        }
        endBloom(player, sl);
        String tag = ObserverFx.tagFor("bloom");
        BLOOMS.put(player.getUUID(), new Eye(sl.getGameTime() + BLOOM_TICKS, tag));
        ObserverFx.bloom(sl, player, BLOOM_RANGE, BLOOM_TICKS, tag);
        ObserverFx.status(player, Status.BLOOM, BLOOM_TICKS);
        Later.run(sl, ObserverFx.EYE_OPEN, () -> bloomSecond(player, sl));
        return AbilityWheel.Result.DONE;
    }

    private static boolean blinded(LivingEntity player) {
        return player.hasEffect(MobEffects.BLINDNESS) || player.hasEffect(MobEffects.DARKNESS);
    }

    /** "In your sight": in front of you within the angle, nothing between. */
    static boolean inSight(LivingEntity player, LivingEntity one, double range) {
        return one == player || Gaze.looking(player, one.getBoundingBox().getCenter(), SIGHT_ANGLE, range)
                || Gaze.looking(player, one.getEyePosition(), SIGHT_ANGLE, range);
    }

    private static void bloomSecond(ServerPlayer player, ServerLevel sl) {
        Eye eye = BLOOMS.get(player.getUUID());
        if (eye == null) return;
        long now = sl.getGameTime();
        if (now >= eye.until || !player.isAlive() || blinded(player)) {
            endBloom(player, sl);
            return;
        }
        int left = (int) (eye.until - now);
        Set<UUID> seen = new HashSet<>();
        for (LivingEntity ally : Spells.alliesAround(player, sl, player.position(), BLOOM_RANGE)) {
            if (inSight(player, ally, BLOOM_RANGE)) seen.add(ally.getUUID());
        }
        seen.add(player.getUUID());
        for (UUID id : seen) {
            if (!(sl.getEntity(id) instanceof LivingEntity ally)) continue;
            Spells.keepEffect(ally, MobEffects.REGENERATION, 3, 40);
            Spells.cleanse(ally);
            Silence.lift(ally);
            AbilityLock.lift(ally);
            if (!ConjuredArmor.isWorn(ally, BLOOM_ARMOR)) {
                ConjuredArmor.of(BLOOM_ARMOR).lasts(left).cut(BLOOM_CUT).absorption(BLOOM_ABSORPTION).wear(ally);
            }
            Spells.growPlants(player, sl, Spells.groundUnder(player, sl, ally), 1.5, 2);
            ObserverFx.bloomOn(sl, ally);
            if (ally != player) ObserverFx.status(ally, Status.BLOOM, left);
        }
        for (UUID id : eye.held) {                                                     // out of sight: it is off them
            if (seen.contains(id) || !(sl.getEntity(id) instanceof LivingEntity gone)) continue;
            ConjuredArmor.takeOff(gone, BLOOM_ARMOR);
            ObserverFx.statusEnd(gone, Status.BLOOM);
        }
        eye.held.clear();
        eye.held.addAll(seen);
        Later.run(sl, 20, () -> bloomSecond(player, sl));
    }

    private static void endBloom(Player player, ServerLevel sl) {
        Eye eye = BLOOMS.remove(player.getUUID());
        if (eye == null) return;
        for (UUID id : eye.held) {
            if (!(sl.getEntity(id) instanceof LivingEntity held)) continue;
            ConjuredArmor.takeOff(held, BLOOM_ARMOR);
            ObserverFx.statusEnd(held, Status.BLOOM);
        }
        ObserverFx.bloomEnd(sl, player, eye.tag);
        ObserverFx.statusEnd(player, Status.BLOOM);
    }

    // =========================
    // Fall Silent
    // =========================

    static AbilityWheel.Result fallSilent(ServerPlayer player, ServerLevel sl) {
        endSilence(player, sl, false);
        String tag = ObserverFx.tagFor("fall_silent");
        SILENCES.put(player.getUUID(), new Eye(sl.getGameTime() + SILENT_TICKS, tag));
        ObserverFx.fallSilent(sl, player, SILENT_RANGE, SILENT_TICKS, tag);
        ObserverFx.status(player, Status.FALL_SILENT, SILENT_TICKS);
        silentSecond(player, sl);
        return AbilityWheel.Result.DONE;
    }

    private static void silentSecond(ServerPlayer player, ServerLevel sl) {
        Eye eye = SILENCES.get(player.getUUID());
        if (eye == null) return;
        long now = sl.getGameTime();
        if (now >= eye.until || !player.isAlive()) {
            endSilence(player, sl, true);
            return;
        }
        Set<UUID> near = new HashSet<>();
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), SILENT_RANGE)) {
            near.add(enemy.getUUID());
            if (inSight(player, enemy, SILENT_RANGE)) {
                if (eye.held.remove(enemy.getUUID())) wake(sl, enemy, eye);
                eye.silentFor.remove(enemy.getUUID());
                continue;
            }
            Silence.apply(enemy, SILENT_HOLD);
            AbilityLock.apply(enemy, SILENT_HOLD);
            Slumber.put(enemy, SILENT_HOLD, Slumber.Kind.LIMP);
            if (enemy instanceof Mob mob) Forgotten.apply(mob, player, SILENT_HOLD);
            eye.silentFor.merge(enemy.getUUID(), 1, Integer::sum);
            if (eye.held.add(enemy.getUUID())) {
                String wilt = ObserverFx.tagFor("silenced");
                eye.wilted.put(enemy.getUUID(), wilt);
                ObserverFx.silenced(sl, enemy, (int) (eye.until - now), wilt);
                ObserverFx.status(enemy, Status.SILENT, (int) (eye.until - now));
            }
        }
        eye.silentFor.keySet().retainAll(near);
        Later.run(sl, 20, () -> silentSecond(player, sl));
    }

    private static void wake(ServerLevel sl, LivingEntity enemy, Eye eye) {
        Silence.lift(enemy);
        AbilityLock.lift(enemy);
        if (Slumber.isLimp(enemy)) Slumber.wake(enemy);
        String wilt = eye.wilted.remove(enemy.getUUID());
        ObserverFx.woken(sl, enemy, wilt == null ? "" : wilt);
        ObserverFx.statusEnd(enemy, Status.SILENT);
    }

    /** It ends: whoever was silent the whole time is erased for a while ('done': it ran its course). */
    private static void endSilence(Player player, ServerLevel sl, boolean done) {
        Eye eye = SILENCES.remove(player.getUUID());
        if (eye == null) return;
        int whole = SILENT_TICKS / 20 - 1;
        for (UUID id : eye.held) {
            if (!(sl.getEntity(id) instanceof LivingEntity enemy) || !enemy.isAlive()) continue;
            wake(sl, enemy, eye);
            if (!done || eye.silentFor.getOrDefault(id, 0) < whole) continue;
            int ticks = Spells.isBoss(enemy) ? SILENT_ERASE / 2 : SILENT_ERASE;
            ObserverFx.erase(sl, player, enemy);
            Vec3 at = enemy.position();
            if (Exile.banish(enemy, ticks, at, (back, level) -> ObserverFx.erasedReturn(level, back.position()))) {
                ObserverFx.status(enemy, Status.ERASED, ticks);
            }
        }
        ObserverFx.fallSilentEnd(sl, player, eye.tag);
        ObserverFx.statusEnd(player, Status.FALL_SILENT);
    }

    // =========================
    // Erase
    // =========================

    static AbilityWheel.Result erase(ServerPlayer player, ServerLevel sl) {
        if (Spells.aimEnemy(player, sl, ERASE_RANGE) != null) {
            LivingEntity target = seenThrough(player, sl, ERASE_RANGE);
            if (target == null) return AbilityWheel.Result.REFUSED;
            int ticks = Spells.holdLength(target, ERASE_TICKS);
            Vec3 at = target.position();
            ObserverFx.erase(sl, player, target);
            if (!Exile.banish(target, ticks, at, (back, level) -> ObserverFx.erasedReturn(level, back.position()))) {
                Spells.tell(player, Component.literal("It will not be rubbed out."));
                return AbilityWheel.Result.REFUSED;
            }
            ObserverFx.status(target, Status.ERASED, ticks);
            return AbilityWheel.Result.DONE;
        }
        Vec3 center = Spells.aimGround(player, sl, ERASE_RANGE);
        int undone = eraseArea(player, sl, center);
        ObserverFx.eraseArea(sl, center, ERASE_RADIUS);
        if (undone == 0) Spells.tell(player, Component.literal("Nothing of theirs here was drawn in; nothing was rubbed out."));
        return AbilityWheel.Result.DONE;
    }

    /** Unmakes what the enemy side conjured within ERASE_RADIUS of 'center'. Returns how many things. */
    static int eraseArea(Player player, ServerLevel sl, Vec3 center) {
        int undone = 0;
        AABB box = new AABB(center, center).inflate(ERASE_RADIUS);
        for (LivingEntity one : sl.getEntitiesOfClass(LivingEntity.class, box, e -> e.isAlive() && e.distanceToSqr(center) <= ERASE_RADIUS * ERASE_RADIUS)) {
            if (one == player || !Dispel.isConjured(one) && !Construct.isConstruct(one)) continue;
            LivingEntity maker = makerOf(sl, one);
            if (maker == null || !Spells.isEnemy(player, maker)) continue;
            ObserverFx.unmade(sl, one.getBoundingBox().getCenter());
            if (Construct.isConstruct(one)) Construct.end(one);
            else Dispel.unmake(one);
            undone++;
        }
        for (Painting.Hung hung : Painting.near(sl, center, ERASE_RADIUS)) {
            if (hung.owner() == player || !Spells.isEnemy(player, hung.owner())) continue;
            ObserverFx.unmade(sl, hung.spot());
            hung.end();
            undone++;
        }
        undone += Dome.closeNear(sl, center, ERASE_RADIUS, owner -> owner != player && Spells.isEnemy(player, owner));
        undone += Zone.closeNear(sl, center, ERASE_RADIUS, owner -> owner != player && Spells.isEnemy(player, owner));
        undone += Enclosure.closeNear(sl, center, ERASE_RADIUS, owner -> owner != player && Spells.isEnemy(player, owner));
        return undone;
    }

    private static LivingEntity makerOf(ServerLevel sl, LivingEntity one) {
        UUID id = Construct.isConstruct(one) ? Construct.ownerOf(one) : Marionette.masterId(one);
        Entity maker = id == null ? null : sl.getEntity(id);
        return maker instanceof LivingEntity living ? living : null;
    }

    // =========================
    // State
    // =========================

    static void clearState(Player player) {
        player.getPersistentData().remove(REVIVAL_TAG);
        player.getPersistentData().remove(SURVIVAL_TAG);
        if (player.level() instanceof ServerLevel sl) {
            endBloom(player, sl);
            endSilence(player, sl, false);
        }
    }
}
