package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer.ObserverFx.Status;
import net.lukario.circleofinevitability.combat.HudCounter;
import net.lukario.circleofinevitability.combat.Ray;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Teleport;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Construct;
import net.lukario.circleofinevitability.status.Flight;
import net.lukario.circleofinevitability.status.Form;
import net.lukario.circleofinevitability.status.Hostel;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Phase;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Key 5, the Dimension wheel (docs/pathways/observer/OBSERVER.md, "Key 5: Dimension"): Phase, Pixie Wings and Painted
 * Lightning (Pixie 5), Dimensional Passage, Hostel and Shadow Form (Visitor 4). Phase is status/Phase and the Hostel is
 * status/Hostel, both reusable on their own.
 */
final class ObserverDimension {

    private ObserverDimension() {}

    // =========================
    // Phase (Pixie 5)
    // Between the Spirit World and reality: hidden, untouchable, free to fly; any cast, attack or item use brings you
    // back. PHASE_UPKEEP a second (none from Sublunary Eye 0, none in Shadow Form from Observer 1); 0 spirituality ends
    // it. Press again: back (free). From Visitor (4) the nearest ally within PHASE_ALLY goes along.
    // =========================
    static final int PHASE_SEQUENCE = Observer.PIXIE;
    static final int PHASE_COST = 3000;
    static final int PHASE_UPKEEP = 100;
    static final int PHASE_TICKS = 72000;                   // until it is ended (or cannot be paid)
    static final double PHASE_ALLY = 3.0;
    static final String PHASE = "observer_phase";           // status/Phase's source

    // =========================
    // Pixie Wings (Pixie 5)
    // Translucent dragonfly wings: WINGS_TICKS of flight, no fall damage after. Press again to fold them (free).
    // =========================
    static final int WINGS_SEQUENCE = Observer.PIXIE;
    static final int WINGS_COST = 1500;
    static final int WINGS_TICKS = 1200;
    static final String WINGS_TAG = "observer_wings_tag";
    static final String WINGS_UNTIL = "observer_wings_until";
    static final String WINGS_KEPT = "observer_wings_kept";

    // =========================
    // Painted Lightning (Pixie 5)
    // Three instant Rays in a narrow fan (BOLT_SPREAD degrees apart), BOLT_DAMAGE each; each jumps to a second enemy
    // within BOLT_JUMP for half. A hit enemy is held for BOLT_HOLD.
    // =========================
    static final int BOLT_SEQUENCE = Observer.PIXIE;
    static final int BOLT_COST = 3000;
    static final float BOLT_DAMAGE = 28f;
    static final double BOLT_RANGE = 32.0;
    static final double BOLT_SPREAD = 6.0;
    static final double BOLT_JUMP = 6.0;
    static final int BOLT_HOLD = 10;

    // =========================
    // Dimensional Passage (Visitor 4)
    // Through the wall ahead (PASSAGE_WALL thick at most) to its far side; with no wall, a blink of PASSAGE_BLINK toward
    // where you look. No fall damage afterwards.
    // =========================
    static final int PASSAGE_SEQUENCE = Observer.VISITOR;
    static final int PASSAGE_COST = 4500;
    static final double PASSAGE_WALL = 8.0;
    static final double PASSAGE_BLINK = 20.0;
    static final int PASSAGE_FALL_GUARD = 60;

    // =========================
    // Hostel (Visitor 4)
    // Aimed at a mob (not a boss, not a player, HOSTEL_MAX_HEALTH at most) below half health: lodged (HOSTEL_COST). With
    // nothing in sight: the oldest guest is let out as a summon for HOSTEL_OUT (RELEASE_COST), lodged again if it lives.
    // HOSTEL_PLACES places (HOSTEL_PLACES_SHADOW and HOSTEL_OUT_SHADOW from Dimensional Shadow 2).
    // =========================
    static final int HOSTEL_SEQUENCE = Observer.VISITOR;
    static final int HOSTEL_COST = 6000;
    static final int RELEASE_COST = 1500;
    static final double HOSTEL_RANGE = 16.0;
    static final float HOSTEL_MAX_HEALTH = 150f;
    static final int HOSTEL_PLACES = 3;
    static final int HOSTEL_PLACES_SHADOW = 5;
    static final int HOSTEL_OUT = 800;
    static final int HOSTEL_OUT_SHADOW = 1200;

    // =========================
    // Shadow Form (Visitor 4)
    // The Mythical Creature Form. Incomplete at 4: SHADOW_TICKS, Speed II, Resistance I, afterimages, a madness that
    // rises SHADOW_MADNESS a second to 100 (then it ends and you are weakened for SHADOW_WEAK_TICKS), Ascension up to
    // ASCENSION_INCOMPLETE. Complete from 2 (SHADOW_COST_COMPLETE): SHADOW_TICKS_COMPLETE, no madness, Ascension up to
    // ASCENSION_MAX. From 1 the tall faint figure stands over you (1.5 times your size) and Phase costs nothing in it.
    // Ascension: one level every ASCENSION_EVERY, each +ASCENSION_DAMAGE ability damage and +1 block on the Strings.
    // =========================
    static final int SHADOW_SEQUENCE = Observer.VISITOR;
    static final int SHADOW_COST = 6000;
    static final int SHADOW_COST_COMPLETE = 9000;
    static final int SHADOW_TICKS = 800;
    static final int SHADOW_TICKS_COMPLETE = 1800;
    static final int SHADOW_MADNESS = 3;
    static final int SHADOW_WEAK_TICKS = 200;
    static final int ASCENSION_INCOMPLETE = 3;
    static final int ASCENSION_MAX = 11;
    static final int ASCENSION_EVERY = 100;
    static final float ASCENSION_DAMAGE = 0.04f;
    static final double OBSERVER_FORM_SCALE = 0.5;          // +50% size from Observer (1)
    static final String SHADOW_FORM = "observer_shadow_form";
    static final String SHADOW_TAG = "observer_shadow_tag";
    static final String SHADOW_MAD = "observer_shadow_madness";
    static final String ASCENDED = "observer_ascension";
    static final String ASCENSION_HUD = "observer_ascension";
    static final String MADNESS_HUD = "observer_shadow_madness";

    static {
        // the toggle itself (and the ritual hidden in Phase) do not bring the caster back
        Phase.addCastException(cast -> cast.entry() != null
                && ("phase".equals(cast.entry().key()) || "higher_revival".equals(cast.entry().key())));
        Phase.addListener((entity, why) -> {
            if (!(entity.level() instanceof ServerLevel sl)) return;
            ObserverFx.phaseOut(sl, entity);
            ObserverFx.statusEnd(entity, Status.PHASE);
        });
        // True Sight from Visitor (4) sees through Phase; the Sublunary Eye always does
        Phase.addSeer((viewer, phased) -> viewer instanceof Player player && (Observer.isObserver(player, Observer.SUBLUNARY_EYE)
                || Observer.isObserver(player, Observer.VISITOR) && ObserverSight.isTrueSight(player)));
    }

    // =========================
    // Phase
    // =========================

    static boolean isPhased(Player player) {
        return PHASE.equals(Phase.source(player));
    }

    /** What a Phase costs now: nothing in Shadow Form from Observer (1). */
    static int phaseCost(Player player, int cost) {
        return Form.isIn(player, SHADOW_FORM) && Observer.grown(player, Observer.OBSERVER) ? 0 : cost;
    }

    static AbilityWheel.Result phase(ServerPlayer player, ServerLevel sl) {
        if (isPhased(player)) {
            Phase.leave(player);
            return AbilityWheel.Result.FREE;
        }
        if (!Phase.enter(player, PHASE_TICKS, PHASE)) {
            Spells.tell(player, Component.literal("Something here forbids slipping out of the world."));
            return AbilityWheel.Result.REFUSED;
        }
        ObserverFx.phaseIn(sl, player);
        ObserverFx.statusForever(player, Status.PHASE);
        if (Observer.grown(player, Observer.VISITOR)) {
            LivingEntity ally = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity one : Spells.alliesAround(player, sl, player.position(), PHASE_ALLY)) {
                if (one == player) continue;
                double d = one.distanceToSqr(player);
                if (d < best) {
                    best = d;
                    ally = one;
                }
            }
            if (ally != null && Phase.bring(ally, player, PHASE_TICKS, PHASE)) {
                ObserverFx.phaseIn(sl, ally);
                ObserverFx.statusForever(ally, Status.PHASE);
            }
        }
        return AbilityWheel.Result.DONE;
    }

    /** Once a second while phased: the upkeep. */
    private static void phaseUpkeep(ServerPlayer player) {
        if (!isPhased(player) || Observer.grown(player, Observer.SUBLUNARY_EYE)) return;
        int due = phaseCost(player, Observer.costFor(player, PHASE_UPKEEP, "phase"));
        if (due > 0 && !Spells.paySpirituality(player, due)) Phase.leave(player, Phase.Why.BROKEN);
    }

    // =========================
    // Pixie Wings
    // =========================

    static boolean hasWings(Player player) {
        return Observer.isActive(player, WINGS_UNTIL);
    }

    static AbilityWheel.Result pixieWings(ServerPlayer player, ServerLevel sl) {
        if (hasWings(player)) {
            foldWings(player, sl);
            return AbilityWheel.Result.FREE;
        }
        String tag = ObserverFx.tagFor("pixie_wings");
        long now = sl.getGameTime();
        player.getPersistentData().putLong(WINGS_UNTIL, now + WINGS_TICKS);
        player.getPersistentData().putLong(WINGS_KEPT, now + ObserverFx.WINGS_KEPT);
        player.getPersistentData().putString(WINGS_TAG, tag);
        Flight.grant(player, WINGS_TICKS);
        ObserverFx.pixieWings(sl, player, WINGS_TICKS, tag);
        ObserverFx.status(player, Status.PIXIE_WINGS, WINGS_TICKS);
        return AbilityWheel.Result.DONE;
    }

    private static void foldWings(Player player, ServerLevel sl) {
        String tag = player.getPersistentData().getStringOr(WINGS_TAG, "");
        player.getPersistentData().remove(WINGS_UNTIL);
        player.getPersistentData().remove(WINGS_KEPT);
        player.getPersistentData().remove(WINGS_TAG);
        if (Flight.has(player) && !isPhased(player)) Flight.end(player);
        if (!tag.isEmpty()) ObserverFx.pixieWingsEnd(sl, player, tag);
        ObserverFx.statusEnd(player, Status.PIXIE_WINGS);
    }

    /** Every 10 ticks: the wings go on beating, and fold when the flight is over. */
    private static void wingsTick(ServerPlayer player, ServerLevel sl) {
        if (!player.getPersistentData().contains(WINGS_TAG)) return;
        if (!hasWings(player) || !Flight.has(player)) {
            foldWings(player, sl);
            return;
        }
        long now = sl.getGameTime();
        if (now < player.getPersistentData().getLongOr(WINGS_KEPT, 0L)) return;
        int left = (int) (player.getPersistentData().getLongOr(WINGS_UNTIL, now) - now);
        player.getPersistentData().putLong(WINGS_KEPT, now + ObserverFx.WINGS_KEPT);
        ObserverFx.pixieWingsKeep(sl, player, left, player.getPersistentData().getStringOr(WINGS_TAG, ""));
    }

    // =========================
    // Painted Lightning
    // =========================

    static AbilityWheel.Result paintedLightning(ServerPlayer player, ServerLevel sl) {
        Vec3 eye = player.getEyePosition();
        LivingEntity aimed = Spells.aimEnemy(player, sl, BOLT_RANGE);
        Vec3 dir = aimed != null ? aimed.getBoundingBox().getCenter().subtract(eye).normalize() : player.getLookAngle().normalize();
        float damage = BOLT_DAMAGE;
        List<Ray> rays = new ArrayList<>();
        List<Vec3> ends = new ArrayList<>();
        Set<UUID> struck = new HashSet<>();
        for (int i = -1; i <= 1; i++) {
            Ray ray = Ray.from(eye, Spells.turned(dir, i * BOLT_SPREAD)).range(BOLT_RANGE).width(0.4).warning(0)
                    .onHit((target, at) -> {
                        Spells.strike(player, target, damage);
                        Root.apply(target, Spells.holdLength(target, BOLT_HOLD), RootRestriction.MOVEMENT);
                        if (!struck.add(target.getUUID())) return;
                        LivingEntity next = null;
                        double best = BOLT_JUMP * BOLT_JUMP;
                        for (LivingEntity other : Spells.enemiesAround(player, sl, target.position(), BOLT_JUMP)) {
                            double d = other.distanceToSqr(target);
                            if (other != target && !struck.contains(other.getUUID()) && d < best) {
                                best = d;
                                next = other;
                            }
                        }
                        if (next == null) return;
                        struck.add(next.getUUID());
                        ObserverFx.boltJump(sl, target.getBoundingBox().getCenter(), next.getBoundingBox().getCenter());
                        Spells.strike(player, next, damage / 2f);
                    });
            rays.add(ray);
            ends.add(ray.end(player, sl));
        }
        ObserverFx.paintedLightning(sl, player, ends);
        for (Ray ray : rays) ray.fire(player, sl);
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Dimensional Passage
    // =========================

    static AbilityWheel.Result dimensionalPassage(ServerPlayer player, ServerLevel sl) {
        if (!Teleport.canGo(player)) {
            Spells.tell(player, Component.literal("Something holds you in this Dimension."));
            return AbilityWheel.Result.REFUSED;
        }
        Vec3 from = player.position();
        Teleport.WallPass pass = Teleport.throughWall(player, sl, PASSAGE_WALL);
        Vec3 to = pass != null ? pass.exit() : Spells.blinkSpot(player, sl, PASSAGE_BLINK);
        if (to == null || to.distanceToSqr(from) < 1.0) {
            Spells.tell(player, Component.literal("There is nowhere to step through to."));
            return AbilityWheel.Result.REFUSED;
        }
        if (!Teleport.of(player).fallGuard(PASSAGE_FALL_GUARD).go(sl, to)) return AbilityWheel.Result.REFUSED;
        ObserverFx.passage(sl, player, from, to);
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Hostel
    // =========================

    static int places(LivingEntity player) {
        return Observer.grown(player, Observer.DIMENSIONAL_SHADOW) ? HOSTEL_PLACES_SHADOW : HOSTEL_PLACES;
    }

    /** The creature a Hostel cast would lodge, or null (then it releases). */
    static Mob guestInSight(Player player, ServerLevel sl) {
        LivingEntity aimed = Spells.aimEnemy(player, sl, HOSTEL_RANGE);
        if (aimed == null) aimed = ObserverTerritory.lookedAt(player, sl, HOSTEL_RANGE);
        if (!(aimed instanceof Mob mob) || Spells.isBoss(mob) || mob.getMaxHealth() > HOSTEL_MAX_HEALTH) return null;
        if (Construct.isConstruct(mob) || Marionette.isSummoned(mob)) return null;    // fiction and summons have nowhere to live
        return mob;
    }

    /** Lodging costs HOSTEL_COST, letting out RELEASE_COST. */
    static int hostelCost(Player player, int cost) {
        return player.level() instanceof ServerLevel sl && guestInSight(player, sl) == null && Hostel.count(player) > 0
                ? RELEASE_COST : cost;
    }

    static AbilityWheel.Result hostel(ServerPlayer player, ServerLevel sl) {
        Mob guest = guestInSight(player, sl);
        if (guest != null) {
            if (guest.getHealth() > guest.getMaxHealth() / 2f) {
                Spells.tell(player, Component.literal("It is too strong to be taken in yet: weaken it below half."));
                return AbilityWheel.Result.REFUSED;
            }
            if (Hostel.count(player) >= places(player)) {
                Spells.tell(player, Component.literal("Every place in your Hostel is taken."));
                return AbilityWheel.Result.REFUSED;
            }
            ObserverFx.hostelLodge(sl, player, guest);
            if (!Hostel.lodge(player, guest, places(player))) return AbilityWheel.Result.REFUSED;
            Spells.tell(player, Component.literal("Lodged: " + Hostel.count(player) + "/" + places(player) + " places taken."));
            return AbilityWheel.Result.DONE;
        }
        if (Hostel.count(player) == 0) {
            Spells.tell(player, Component.literal("Your Hostel is empty: weaken a creature and look at it to take it in."));
            return AbilityWheel.Result.REFUSED;
        }
        Vec3 spot = Spells.freeSpotNear(player, sl, player.getEyePosition(), Spells.flatLook(player), 2.0);
        Vec3 feet = Spells.groundAt(player, sl, spot == null ? player.position() : spot);
        int out = Observer.grown(player, Observer.DIMENSIONAL_SHADOW) ? HOSTEL_OUT_SHADOW : HOSTEL_OUT;
        Mob released = Hostel.release(player, sl, feet, out);
        if (released == null) {
            Spells.tell(player, Component.literal("Your guest could not come out here."));
            return AbilityWheel.Result.REFUSED;
        }
        ObserverFx.hostelRelease(sl, player, feet);
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Shadow Form
    // =========================

    static boolean inShadowForm(Player player) {
        return Form.isIn(player, SHADOW_FORM);
    }

    static int shadowCost(Player player, int cost) {
        return Observer.grown(player, Observer.DIMENSIONAL_SHADOW) ? SHADOW_COST_COMPLETE : cost;
    }

    /** The Ascension level now (0 out of the form). */
    static int ascension(Player player) {
        return inShadowForm(player) ? player.getPersistentData().getIntOr(ASCENDED, 0) : 0;
    }

    static AbilityWheel.Result shadowForm(ServerPlayer player, ServerLevel sl) {
        if (inShadowForm(player)) {
            Form.end(player);
            return AbilityWheel.Result.FREE;
        }
        boolean complete = Observer.grown(player, Observer.DIMENSIONAL_SHADOW);
        boolean observer = Observer.grown(player, Observer.OBSERVER);
        int ticks = complete ? SHADOW_TICKS_COMPLETE : SHADOW_TICKS;
        int maxAscension = complete ? ASCENSION_MAX : ASCENSION_INCOMPLETE;
        String tag = ObserverFx.tagFor("shadow_form");
        player.getPersistentData().putString(SHADOW_TAG, tag);
        player.getPersistentData().putInt(SHADOW_MAD, 0);
        player.getPersistentData().putInt(ASCENDED, 0);
        long started = sl.getGameTime();
        Form form = Form.of(SHADOW_FORM).lasts(ticks)
                .effect(MobEffects.SPEED, 1)
                .effect(MobEffects.RESISTANCE, 0)
                .keepWhile(p -> net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore.getPathway(p).equals(Observer.PATHWAY))
                .every(4, p -> {
                    if (p.level() instanceof ServerLevel level && p.getDeltaMovement().horizontalDistanceSqr() > 0.004) {
                        ObserverFx.shadowAfterimage(level, p);
                    }
                })
                .everySecond(p -> shadowSecond(p, started, complete, maxAscension))
                .onEnd((p, why) -> shadowEnded(p, why));
        if (observer) form = form.attribute(Attributes.SCALE, OBSERVER_FORM_SCALE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
        form.take(player);
        ObserverFx.shadowForm(sl, player, ticks, complete, tag);
        if (observer) ObserverFx.observerForm(sl, player, ticks, tag + "_figure");
        ObserverFx.status(player, Status.SHADOW_FORM, ticks);
        HudCounter.show(player, ASCENSION_HUD, "observer/shadow_form", "Dimension", 0, maxAscension, 0xBEB4FF);
        if (!complete) HudCounter.show(player, MADNESS_HUD, "observer/shadow_weakness", "Madness", 0, 100, 0xC83030);
        return AbilityWheel.Result.DONE;
    }

    private static void shadowSecond(Player player, long started, boolean complete, int maxAscension) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        long open = sl.getGameTime() - started;
        int level = (int) Math.min(maxAscension, open / ASCENSION_EVERY);
        if (level > player.getPersistentData().getIntOr(ASCENDED, 0)) {
            player.getPersistentData().putInt(ASCENDED, level);
            ObserverFx.ascension(sl, player, level);
            HudCounter.show(player, ASCENSION_HUD, "observer/shadow_form", "Dimension", level, maxAscension, 0xBEB4FF);
        }
        if (complete) return;
        int madness = Math.min(100, player.getPersistentData().getIntOr(SHADOW_MAD, 0) + SHADOW_MADNESS);
        player.getPersistentData().putInt(SHADOW_MAD, madness);
        HudCounter.show(player, MADNESS_HUD, "observer/shadow_weakness", "Madness", madness, 100, 0xC83030);
        if (madness >= 100) Form.end(player);                                 // at its height the form gives way
    }

    private static void shadowEnded(Player player, Form.Ending why) {
        String tag = player.getPersistentData().getStringOr(SHADOW_TAG, "");
        boolean mad = player.getPersistentData().getIntOr(SHADOW_MAD, 0) >= 100;
        player.getPersistentData().remove(SHADOW_TAG);
        player.getPersistentData().remove(SHADOW_MAD);
        player.getPersistentData().remove(ASCENDED);
        HudCounter.hide(player, ASCENSION_HUD);
        HudCounter.hide(player, MADNESS_HUD);
        ObserverFx.statusEnd(player, Status.SHADOW_FORM);
        if (!(player.level() instanceof ServerLevel sl)) return;
        if (!tag.isEmpty()) {
            ObserverFx.shadowFormEnd(sl, player, tag);
            ObserverFx.observerFormEnd(sl, player, tag + "_figure");
        }
        if (mad && why != Form.Ending.DIED && why != Form.Ending.LEFT) {
            Spells.keepEffect(player, MobEffects.WEAKNESS, 1, SHADOW_WEAK_TICKS);
            Spells.keepEffect(player, MobEffects.SLOWNESS, 1, SHADOW_WEAK_TICKS);
            String weak = ObserverFx.tagFor("shadow_weakness");
            ObserverFx.shadowWeakness(sl, player, SHADOW_WEAK_TICKS, weak);
            ObserverFx.status(player, Status.SHADOW_WEAKNESS, SHADOW_WEAK_TICKS);
        }
    }

    /** Ascension: the Shadow's ability hits do more. */
    static void hurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || !Spells.isSpellHit(event.getEntity())) return;
        int level = ascension(player);
        if (level > 0) event.setAmount(event.getAmount() * (1f + ASCENSION_DAMAGE * level));
    }

    // =========================
    // Every 10 ticks, for an Observer
    // =========================

    static void tick(ServerPlayer player, ServerLevel sl) {
        wingsTick(player, sl);
        if (player.tickCount % 20 != 0) return;
        phaseUpkeep(player);
        if (isPhased(player)) ObserverFx.phaseTick(player);
    }

    static void clearState(Player player) {
        player.getPersistentData().remove(WINGS_TAG);
        player.getPersistentData().remove(WINGS_UNTIL);
        player.getPersistentData().remove(WINGS_KEPT);
        player.getPersistentData().remove(SHADOW_TAG);
        player.getPersistentData().remove(SHADOW_MAD);
        player.getPersistentData().remove(ASCENDED);
        HudCounter.hide(player, ASCENSION_HUD);
        HudCounter.hide(player, MADNESS_HUD);
    }
}
