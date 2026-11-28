package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer.ObserverFx.Status;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.Dossier;
import net.lukario.circleofinevitability.combat.Painting;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Trap;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.DamageLink;
import net.lukario.circleofinevitability.status.DeathWard;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Marks;
import net.lukario.circleofinevitability.status.Observation;
import net.lukario.circleofinevitability.status.Phase;
import net.lukario.circleofinevitability.status.Substitute;
import net.lukario.circleofinevitability.status.Unnoticed;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.entity.living.MobEffectEvent;

import java.util.List;
import java.util.UUID;

/**
 * Key 2, the Sight wheel (docs/pathways/observer/OBSERVER.md, "Key 2: Sight"): True Sight and Many Angles (Reporter 8),
 * Read Intent (Painter 7). Seeing, never breaking: nothing here dispels or ends what it shows.
 */
final class ObserverSight {

    private ObserverSight() {}

    // =========================
    // True Sight (Reporter 8)
    // For TRUE_SIGHT_TICKS, for you alone, within TRUE_SIGHT_RANGE: the hidden are outlined, Marionette strings and
    // Damage Links are threads, an armed Substitute or Death Ward is a mark over its owner, hidden Traps and others'
    // Paintings show. Illusions (Blindness, Darkness, Nausea) end on you and cannot be put on you. Press again: it ends.
    // =========================
    static final int TRUE_SIGHT_SEQUENCE = Observer.REPORTER;
    static final int TRUE_SIGHT_COST = 350;
    static final int TRUE_SIGHT_TICKS = 240;
    static final int TRUE_SIGHT_TICKS_PIXIE = 400;          // Pixie (5)
    static final int TRUE_SIGHT_TICKS_VISITOR = 600;        // Visitor (4)
    static final double TRUE_SIGHT_RANGE = 24.0;

    // =========================
    // Many Angles (Reporter 8)
    // For ANGLES_TICKS every living thing within ANGLES_RADIUS glows for you through walls with a line to what it looks
    // at or hunts (red when that is you). Every enemy it shows gains one level of Observation, once a cast.
    // =========================
    static final int ANGLES_SEQUENCE = Observer.REPORTER;
    static final int ANGLES_COST = 350;
    static final int ANGLES_TICKS = 160;
    static final double ANGLES_RADIUS = 32.0;

    // =========================
    // Read Intent (Painter 7)
    // At an enemy (READ_RANGE): its Dossier in chat, deeper every two Sequences, and a level of Observation. At a hung
    // Painting, a hidden Trap or a Dome: who made it, what it does, how long it has left.
    // =========================
    static final int READ_SEQUENCE = Observer.PAINTER;
    static final int READ_COST = 1250;
    static final double READ_RANGE = 32.0;

    static final String TRUE_UNTIL = "observer_true_sight_until";
    static final String TRUE_TAG = "observer_true_sight_tag";
    static final String ANGLES_UNTIL = "observer_many_angles_until";
    static final String ANGLES_TAG = "observer_many_angles_tag";
    static final String ANGLES_CAST = "observer_many_angles_cast";       // status/Marks: shown once this cast

    private static final List<Holder<MobEffect>> ILLUSIONS = List.of(MobEffects.BLINDNESS, MobEffects.DARKNESS, MobEffects.NAUSEA);

    // =========================
    // True Sight
    // =========================

    static boolean isTrueSight(Player player) {
        return Observer.isActive(player, TRUE_UNTIL);
    }

    static AbilityWheel.Result trueSight(ServerPlayer player, ServerLevel sl) {
        if (isTrueSight(player)) {
            endTrueSight(player, sl);
            return AbilityWheel.Result.FREE;
        }
        int ticks = Observer.grown(player, Observer.VISITOR) ? TRUE_SIGHT_TICKS_VISITOR
                : Observer.grown(player, Observer.PIXIE) ? TRUE_SIGHT_TICKS_PIXIE : TRUE_SIGHT_TICKS;
        String tag = ObserverFx.tagFor("true_sight");
        player.getPersistentData().putLong(TRUE_UNTIL, sl.getGameTime() + ticks);
        player.getPersistentData().putString(TRUE_TAG, tag);
        ObserverFx.trueSight(sl, player, ticks, tag);
        ObserverFx.status(player, Status.TRUE_SIGHT, ticks);
        for (Holder<MobEffect> illusion : ILLUSIONS) player.removeEffect(illusion);
        trueSightLooks(player, sl);
        return AbilityWheel.Result.DONE;
    }

    private static void endTrueSight(ServerPlayer player, ServerLevel sl) {
        String tag = player.getPersistentData().getStringOr(TRUE_TAG, "");
        player.getPersistentData().remove(TRUE_UNTIL);
        player.getPersistentData().remove(TRUE_TAG);
        if (!tag.isEmpty()) ObserverFx.trueSightEnd(sl, player, tag);
        ObserverFx.statusEnd(player, Status.TRUE_SIGHT);
    }

    /** Every 10 ticks while it is open: what it shows. */
    private static void trueSightLooks(ServerPlayer player, ServerLevel sl) {
        Vec3 at = player.position();
        for (LivingEntity one : Spells.livingAround(player, sl, at, TRUE_SIGHT_RANGE)) {
            boolean hiding = Concealment.isHidden(one) || Unnoticed.isUnnoticed(one) || one.hasEffect(MobEffects.INVISIBILITY);
            if (hiding && Phase.seenBy(player, one)) ObserverFx.trueSightSees(player, one);
            if (Marionette.isMarionette(one)) {
                UUID master = Marionette.masterId(one);
                Entity holder = master == null ? null : sl.getEntity(master);
                if (holder != null) ObserverFx.trueSightThread(player, holder.getBoundingBox().getCenter(), one.getBoundingBox().getCenter());
            }
            for (LivingEntity linked : DamageLink.sharedWith(one)) {
                if (linked.getId() < one.getId()) continue;                     // each link once
                ObserverFx.trueSightThread(player, one.getBoundingBox().getCenter(), linked.getBoundingBox().getCenter());
            }
            if (Substitute.isArmed(one) || DeathWard.isArmed(one)) ObserverFx.trueSightWard(player, one);
        }
        if (player.tickCount % 20 == 0) {
            for (Vec3 trap : Trap.hiddenNear(sl, at, TRUE_SIGHT_RANGE)) ObserverFx.trueSightTrap(player, trap);
            for (Painting.Hung hung : Painting.near(sl, at, TRUE_SIGHT_RANGE)) {
                if (!hung.owner().getUUID().equals(player.getUUID())) ObserverFx.trueSightTrap(player, hung.spot());
            }
        }
    }

    /** True Sight: no illusion takes on you while it is open. */
    static void effect(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) return;
        if (!isTrueSight(player) && !Observer.isObserver(player, Observer.SUBLUNARY_EYE)) return;
        for (Holder<MobEffect> illusion : ILLUSIONS) {
            if (event.getEffectInstance().is(illusion)) {
                event.setResult(Result.DENY);
                return;
            }
        }
    }

    // =========================
    // Many Angles
    // =========================

    static AbilityWheel.Result manyAngles(ServerPlayer player, ServerLevel sl) {
        String old = player.getPersistentData().getStringOr(ANGLES_TAG, "");
        if (!old.isEmpty()) ObserverFx.manyAnglesEnd(sl, player, old);
        String tag = ObserverFx.tagFor("many_angles");
        player.getPersistentData().putLong(ANGLES_UNTIL, sl.getGameTime() + ANGLES_TICKS);
        player.getPersistentData().putString(ANGLES_TAG, tag);
        ObserverFx.manyAngles(sl, player, ANGLES_RADIUS, ANGLES_TICKS, tag);
        ObserverFx.status(player, Status.MANY_ANGLES, ANGLES_TICKS);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), ANGLES_RADIUS)) {
            Marks.clear(enemy, player, ANGLES_CAST);                             // a new cast: each can be shown again
        }
        manyAnglesLooks(player, sl);
        return AbilityWheel.Result.DONE;
    }

    /** Every 10 ticks while it is open: every living thing glows for you with a line to what it looks at. */
    private static void manyAnglesLooks(ServerPlayer player, ServerLevel sl) {
        for (LivingEntity one : Spells.livingAround(player, sl, player.position(), ANGLES_RADIUS)) {
            LivingEntity hunted = one instanceof Mob mob ? mob.getTarget() : null;
            Vec3 lookingAt = hunted != null ? hunted.getEyePosition() : one.getEyePosition().add(one.getLookAngle().scale(6));
            ObserverFx.manyAnglesLine(player, one, lookingAt, hunted == player);
            if (Spells.isEnemy(player, one) && Marks.count(one, player, ANGLES_CAST) == 0) {
                Marks.add(one, player, ANGLES_CAST, 1, 1, ANGLES_TICKS + 20);
                Observation.raise(player, one, 1);
            }
        }
    }

    // =========================
    // Read Intent
    // =========================

    /** How deep a Dossier reads: one depth more every two Sequences from Painter. */
    static int depth(LivingEntity player) {
        if (Observer.grown(player, Observer.STRING_PLAYER)) return 3;
        if (Observer.grown(player, Observer.VISITOR)) return 2;
        return 1;
    }

    static AbilityWheel.Result readIntent(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, READ_RANGE);
        if (target != null) {
            ObserverFx.readIntent(sl, player, target.getBoundingBox().getCenter());
            Dossier.tell(player, target, depth(player));
            Observation.raise(player, target, 1);
            return AbilityWheel.Result.DONE;
        }
        // a hung Painting near the crosshair's line
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 aim = Spells.aimPoint(player, sl, READ_RANGE);
        Painting.Hung best = null;
        double nearest = 2.5 * 2.5;
        for (Painting.Hung hung : Painting.near(sl, eye, READ_RANGE)) {
            Vec3 toIt = hung.spot().subtract(eye);
            double along = toIt.dot(look);
            if (along < 0) continue;
            double off = toIt.subtract(look.scale(along)).lengthSqr();
            if (off < nearest) {
                nearest = off;
                best = hung;
            }
        }
        if (best != null) {
            ObserverFx.readIntent(sl, player, best.spot());
            Spells.tell(player, Component.literal(ObserverCanvas.describe(best)));
            return AbilityWheel.Result.DONE;
        }
        List<Vec3> traps = Trap.hiddenNear(sl, aim, 3.0);
        if (!traps.isEmpty()) {
            ObserverFx.readIntent(sl, player, traps.getFirst());
            Spells.tell(player, Component.literal("A hidden trap lies here: it springs on the first enemy of its maker who steps on it."));
            return AbilityWheel.Result.DONE;
        }
        for (Dome.Raised dome : Dome.raisedAt(sl, aim)) {
            ObserverFx.readIntent(sl, player, dome.center());
            int left = Dome.ticksLeft(dome.owner(), dome.name()) / 20;
            Spells.tell(player, Component.literal("A dome raised by " + dome.owner().getName().getString() + ", "
                    + Math.round(dome.radius()) + " blocks across: it shelters whoever is inside"
                    + (left > 0 && left < 100000 ? ", " + left + " s left." : ".")));
            return AbilityWheel.Result.DONE;
        }
        Spells.tell(player, Component.literal("There is nothing here to read."));
        return AbilityWheel.Result.REFUSED;
    }

    // =========================
    // Every 10 ticks, for an Observer
    // =========================

    static void tick(ServerPlayer player, ServerLevel sl) {
        if (isTrueSight(player) || Observer.isObserver(player, Observer.SUBLUNARY_EYE)) {   // a passive from Sublunary Eye (0)
            for (Holder<MobEffect> illusion : ILLUSIONS) player.removeEffect(illusion);
            trueSightLooks(player, sl);
        } else if (player.getPersistentData().contains(TRUE_TAG)) {
            endTrueSight(player, sl);                                            // its time is up
        }
        if (Observer.isActive(player, ANGLES_UNTIL)) {
            manyAnglesLooks(player, sl);
        } else if (player.getPersistentData().contains(ANGLES_TAG)) {
            ObserverFx.manyAnglesEnd(sl, player, player.getPersistentData().getStringOr(ANGLES_TAG, ""));
            player.getPersistentData().remove(ANGLES_TAG);
            player.getPersistentData().remove(ANGLES_UNTIL);
        }
    }

    static void clearState(Player player) {
        player.getPersistentData().remove(TRUE_UNTIL);
        player.getPersistentData().remove(TRUE_TAG);
        player.getPersistentData().remove(ANGLES_UNTIL);
        player.getPersistentData().remove(ANGLES_TAG);
    }
}
