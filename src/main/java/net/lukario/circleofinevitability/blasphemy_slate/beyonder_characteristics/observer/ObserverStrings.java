package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.observer.ObserverFx.Status;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.NextCast;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Teleport;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Construct;
import net.lukario.circleofinevitability.status.Falter;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Silence;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Key 6, the Strings wheel (docs/pathways/observer/OBSERVER.md, "Key 6: Strings"): Taut Chord, Resonance, Harmony and
 * Dissonance (String Player 3), Shadow From Above and Fold Space (Dimensional Shadow 2). Every range here grows by a
 * block per level of Ascension in Shadow Form (ObserverDimension.ascension).
 */
final class ObserverStrings {

    private ObserverStrings() {}

    // =========================
    // Taut Chord (String Player 3)
    // Strings to every enemy in sight (CHORD_CATCHES at most, CHORD_RANGE): pulled toward the middle of the web for
    // ObserverFx.TAUT_PULLS ticks, held, then played at ObserverFx.TAUT_PLAYS: CHORD_DAMAGE each.
    // =========================
    static final int CHORD_SEQUENCE = Observer.STRING_PLAYER;
    static final int CHORD_COST = 12000;
    static final double CHORD_RANGE = 28.0;
    static final double CHORD_ANGLE = 50.0;
    static final int CHORD_CATCHES = 6;
    static final float CHORD_DAMAGE = 45f;

    // =========================
    // Resonance (String Player 3)
    // One enemy's string (RESONANCE_RANGE) vibrates for RESONANCE_TICKS: each ability hit of yours on it adds a
    // Resonance, up to RESONANCE_MAX. At the top, or when the time ends, it bursts: RESONANCE_DAMAGE per Resonance in
    // RESONANCE_RADIUS and Silence for RESONANCE_SILENCE.
    // =========================
    static final int RESONANCE_SEQUENCE = Observer.STRING_PLAYER;
    static final int RESONANCE_COST = 9000;
    static final double RESONANCE_RANGE = 28.0;
    static final int RESONANCE_TICKS = 160;
    static final int RESONANCE_MAX = 5;
    static final float RESONANCE_DAMAGE = 20f;
    static final double RESONANCE_RADIUS = 4.0;
    static final int RESONANCE_SILENCE = 40;

    // =========================
    // Harmony (String Player 3)
    // Allies within HARMONY_RADIUS (and you): Regeneration II for HARMONY_TICKS and a cleanse; the next ability each
    // player of them casts within HARMONY_ARMED is free and HARMONY_STRENGTH as strong (combat/NextCast).
    // =========================
    static final int HARMONY_SEQUENCE = Observer.STRING_PLAYER;
    static final int HARMONY_COST = 12000;
    static final double HARMONY_RADIUS = 16.0;
    static final int HARMONY_TICKS = 200;
    static final int HARMONY_ARMED = 600;
    static final float HARMONY_STRENGTH = 1.2f;

    // =========================
    // Dissonance (String Player 3)
    // Every enemy within DISSONANCE_RADIUS: a player is silenced for DISSONANCE_SILENCE; a mob misses DISSONANCE_WIDE
    // of its attacks and is slowed for DISSONANCE_TICKS. Marionettes and constructs the enemy side holds there are
    // shaken loose.
    // =========================
    static final int DISSONANCE_SEQUENCE = Observer.STRING_PLAYER;
    static final int DISSONANCE_COST = 12000;
    static final double DISSONANCE_RADIUS = 14.0;
    static final int DISSONANCE_SILENCE = 60;
    static final float DISSONANCE_WIDE = 0.6f;
    static final int DISSONANCE_TICKS = 100;

    // =========================
    // Shadow From Above (Dimensional Shadow 2)
    // At an enemy (SHADOW_RANGE, walls do not matter): SHADOW_DAMAGE that no shield, substitute or ward stops
    // (Spells.strikeThrough). At the ground: blades in SHADOW_RADIUS for half. SHADOW_WORLD as much inside your
    // Painting World.
    // =========================
    static final int SHADOW_SEQUENCE = Observer.DIMENSIONAL_SHADOW;
    static final int SHADOW_COST = 20000;
    static final double SHADOW_RANGE = 40.0;
    static final float SHADOW_DAMAGE = 120f;
    static final double SHADOW_RADIUS = 8.0;
    static final float SHADOW_WORLD = 1.5f;

    // =========================
    // Fold Space (Dimensional Shadow 2)
    // At an enemy or a spot (FOLD_RANGE): you stand beside it at once and everything on the line between takes
    // FOLD_DAMAGE and is held for FOLD_HOLD. No fall damage.
    // =========================
    static final int FOLD_SEQUENCE = Observer.DIMENSIONAL_SHADOW;
    static final int FOLD_COST = 16000;
    static final double FOLD_RANGE = 48.0;
    static final float FOLD_DAMAGE = 60f;
    static final int FOLD_HOLD = 20;

    static final String HARMONY_TAG = "observer_harmony_tag";

    /** A plucked string: whose, until when, how many Resonances, the look's tag. */
    private static final class Plucked {
        final UUID player;
        final long until;
        final String tag;
        int count;

        Plucked(UUID player, long until, String tag) {
            this.player = player;
            this.until = until;
            this.tag = tag;
        }
    }

    private static final Map<UUID, Plucked> PLUCKED = new HashMap<>();
    /** The Harmony each ally is waiting to spend: ally -> its look's tag. */
    private static final Map<UUID, LivingEntity> HARMONIZED = new HashMap<>();

    /** Ranges grow a block per level of Ascension. */
    static double reach(Player player, double range) {
        return range + ObserverDimension.ascension(player);
    }

    // =========================
    // Taut Chord
    // =========================

    static AbilityWheel.Result tautChord(ServerPlayer player, ServerLevel sl) {
        List<LivingEntity> caught = new ArrayList<>(Spells.enemiesInSight(player, sl, reach(player, CHORD_RANGE), CHORD_ANGLE));
        if (caught.isEmpty()) {
            Spells.tell(player, Component.literal("There is no one in sight to draw a string to."));
            return AbilityWheel.Result.REFUSED;
        }
        if (caught.size() > CHORD_CATCHES) caught = new ArrayList<>(caught.subList(0, CHORD_CATCHES));
        Vec3 middle = Vec3.ZERO;
        for (LivingEntity one : caught) middle = middle.add(one.position());
        Vec3 web = middle.scale(1.0 / caught.size());
        List<LivingEntity> held = caught;
        ObserverFx.tautChord(sl, player, held);
        for (int t = 0; t < ObserverFx.TAUT_PULLS; t += 2) {
            Later.run(sl, t, () -> {
                for (LivingEntity one : held) if (one.isAlive()) Spells.pull(one, web, 0.6, 0.05);
            });
        }
        Later.run(sl, ObserverFx.TAUT_PULLS, () -> {
            for (LivingEntity one : held) {
                if (one.isAlive()) Root.apply(one, Spells.holdLength(one, ObserverFx.TAUT_PLAYS - ObserverFx.TAUT_PULLS), RootRestriction.MOVEMENT);
            }
        });
        Later.run(sl, ObserverFx.TAUT_PLAYS, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            List<LivingEntity> alive = new ArrayList<>();
            for (LivingEntity one : held) if (one.isAlive()) alive.add(one);
            ObserverFx.tautChordPlay(sl, player, alive);
            for (LivingEntity one : alive) Spells.strike(player, one, CHORD_DAMAGE);
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Resonance
    // =========================

    static AbilityWheel.Result resonance(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, reach(player, RESONANCE_RANGE));
        if (target == null) {
            Spells.tell(player, Component.literal("There is no string in sight to pluck."));
            return AbilityWheel.Result.REFUSED;
        }
        Plucked old = PLUCKED.remove(target.getUUID());
        if (old != null) burst(sl, target, old);
        String tag = ObserverFx.tagFor("resonance");
        PLUCKED.put(target.getUUID(), new Plucked(player.getUUID(), sl.getGameTime() + RESONANCE_TICKS, tag));
        ObserverFx.resonance(sl, player, target, RESONANCE_TICKS, tag);
        ObserverFx.status(target, Status.RESONATING, RESONANCE_TICKS);
        return AbilityWheel.Result.DONE;
    }

    /** An ability hit of the one who plucked it adds a Resonance. */
    static void hurt(LivingHurtEvent event) {
        LivingEntity hurt = event.getEntity();
        Plucked plucked = PLUCKED.get(hurt.getUUID());
        if (plucked == null || !(hurt.level() instanceof ServerLevel sl) || !Spells.isSpellHit(hurt)) return;
        if (!(event.getSource().getEntity() instanceof Player player) || !player.getUUID().equals(plucked.player)) return;
        plucked.count = Math.min(RESONANCE_MAX, plucked.count + 1);
        int left = (int) Math.max(1, plucked.until - sl.getGameTime());
        ObserverFx.resonanceAdd(sl, hurt, plucked.count, left, plucked.tag);
        if (plucked.count >= RESONANCE_MAX) {
            PLUCKED.remove(hurt.getUUID());
            Later.run(sl, 1, () -> burst(sl, hurt, plucked));                     // after this hit is done
        }
    }

    private static void burst(ServerLevel sl, LivingEntity target, Plucked plucked) {
        ObserverFx.statusEnd(target, Status.RESONATING);
        ObserverFx.resonanceBurst(sl, target, RESONANCE_RADIUS, plucked.count, plucked.tag);
        Entity owner = sl.getEntity(plucked.player);
        if (!(owner instanceof Player player) || plucked.count <= 0) return;
        float damage = RESONANCE_DAMAGE * plucked.count;
        for (LivingEntity one : Spells.enemiesAround(player, sl, target.position(), RESONANCE_RADIUS)) {
            Spells.strike(player, one, damage);
            Silence.apply(one, Spells.holdLength(one, RESONANCE_SILENCE));
        }
    }

    /** Every second: strings whose time ran out burst with what they gathered. */
    private static void resonanceTick(ServerLevel sl) {
        if (PLUCKED.isEmpty()) return;
        long now = sl.getGameTime();
        PLUCKED.entrySet().removeIf(e -> {
            Entity target = sl.getEntity(e.getKey());
            if (target == null) return now > e.getValue().until + 200;           // in another world: let it go in time
            if (!(target instanceof LivingEntity living) || !living.isAlive()) return true;
            if (now < e.getValue().until) return false;
            burst(sl, living, e.getValue());
            return true;
        });
    }

    // =========================
    // Harmony
    // =========================

    static AbilityWheel.Result harmony(ServerPlayer player, ServerLevel sl) {
        List<LivingEntity> allies = new ArrayList<>(Spells.alliesAround(player, sl, player.position(), HARMONY_RADIUS));
        if (!allies.contains(player)) allies.add(player);
        ObserverFx.harmony(sl, player, HARMONY_RADIUS, allies);
        for (LivingEntity ally : allies) {
            Spells.cleanse(ally);
            Spells.keepEffect(ally, MobEffects.REGENERATION, 1, HARMONY_TICKS);
            if (!(ally instanceof Player friend) || ally == player) continue;      // your own next cast would be this one
            NextCast.arm(friend, HARMONY_ARMED, 0f, HARMONY_STRENGTH);
            LivingEntity was = HARMONIZED.put(friend.getUUID(), friend);
            String old = friend.getPersistentData().getStringOr(HARMONY_TAG, "");
            if (was != null && !old.isEmpty()) ObserverFx.harmonySpent(sl, friend, old);
            String tag = ObserverFx.tagFor("harmony");
            friend.getPersistentData().putString(HARMONY_TAG, tag);
            ObserverFx.harmonyArmed(sl, friend, HARMONY_ARMED, tag);
            ObserverFx.status(friend, Status.HARMONY, HARMONY_ARMED);
        }
        return AbilityWheel.Result.DONE;
    }

    /** Every second: a Harmony that was spent (or ran out) loses its look. */
    private static void harmonyTick() {
        if (HARMONIZED.isEmpty()) return;
        HARMONIZED.values().removeIf(ally -> {
            if (ally instanceof Player friend && friend.isAlive() && !friend.isRemoved() && NextCast.active(friend)) return false;
            String tag = ally.getPersistentData().getStringOr(HARMONY_TAG, "");
            ally.getPersistentData().remove(HARMONY_TAG);
            if (ally.level() instanceof ServerLevel level && !tag.isEmpty()) ObserverFx.harmonySpent(level, ally, tag);
            ObserverFx.statusEnd(ally, Status.HARMONY);
            return true;
        });
    }

    // =========================
    // Dissonance
    // =========================

    static AbilityWheel.Result dissonance(ServerPlayer player, ServerLevel sl) {
        double radius = reach(player, DISSONANCE_RADIUS);
        ObserverFx.dissonance(sl, player, radius);
        for (LivingEntity one : Spells.enemiesAround(player, sl, player.position(), radius)) {
            ObserverFx.dissonant(sl, one);
            if (one instanceof Player) {
                Silence.apply(one, Spells.holdLength(one, DISSONANCE_SILENCE));
                ObserverFx.status(one, Status.DISSONANT, DISSONANCE_SILENCE);
            } else {
                Falter.apply(one, "observer_dissonance", DISSONANCE_WIDE, DISSONANCE_TICKS);
                Spells.keepEffect(one, MobEffects.SLOWNESS, 1, DISSONANCE_TICKS);
            }
            shakeLoose(player, sl, one);
        }
        return AbilityWheel.Result.DONE;
    }

    /** A marionette or construct held by the enemy side is let go. */
    private static void shakeLoose(Player player, ServerLevel sl, LivingEntity one) {
        if (!(one instanceof Mob mob)) return;
        if (Marionette.isMarionette(mob)) {
            UUID master = Marionette.masterId(mob);
            Entity holder = master == null ? null : sl.getEntity(master);
            if (holder instanceof LivingEntity living && Spells.isEnemy(player, living)) {
                if (Marionette.isSummoned(mob)) Marionette.collapse(mob);
                else Marionette.release(mob);
            }
            return;
        }
        if (Construct.isConstruct(mob)) {
            UUID owner = Construct.ownerOf(mob);
            Entity maker = owner == null ? null : sl.getEntity(owner);
            if (maker instanceof LivingEntity living && Spells.isEnemy(player, living)) Construct.end(mob);
        }
    }

    // =========================
    // Shadow From Above
    // =========================

    static AbilityWheel.Result shadowFromAbove(ServerPlayer player, ServerLevel sl) {
        double range = reach(player, SHADOW_RANGE);
        LivingEntity target = Spells.aimLiving(player, sl, range, true, one -> Spells.isEnemy(player, one));
        if (target != null) {
            float damage = SHADOW_DAMAGE * (ObserverTerritory.inWorld(player, target) ? SHADOW_WORLD : 1f);
            ObserverFx.shadowFromAbove(sl, target);
            Spells.strikeThrough(player, target, damage);
            return AbilityWheel.Result.DONE;
        }
        Vec3 ground = Spells.aimGround(player, sl, range);
        ObserverFx.shadowBladesRise(sl, ground, SHADOW_RADIUS);
        Later.run(sl, 6, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            for (LivingEntity one : Spells.enemiesAround(player, sl, ground, SHADOW_RADIUS)) {
                float damage = SHADOW_DAMAGE / 2f * (ObserverTerritory.inWorld(player, one) ? SHADOW_WORLD : 1f);
                Spells.strikeThrough(player, one, damage);
            }
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Fold Space
    // =========================

    static AbilityWheel.Result foldSpace(ServerPlayer player, ServerLevel sl) {
        if (!Teleport.canGo(player)) {
            Spells.tell(player, Component.literal("Something holds this plane flat."));
            return AbilityWheel.Result.REFUSED;
        }
        double range = reach(player, FOLD_RANGE);
        Vec3 from = player.position();
        LivingEntity target = Spells.aimEnemy(player, sl, range);
        Vec3 wanted;
        if (target != null) {
            Vec3 back = from.subtract(target.position());
            back = back.horizontalDistanceSqr() < 0.01 ? Spells.flatLook(player).scale(-1) : new Vec3(back.x, 0, back.z).normalize();
            wanted = target.position().add(back.scale(target.getBbWidth() / 2 + 1.2));
        } else {
            wanted = Spells.aimGround(player, sl, range);
        }
        Vec3 to = Teleport.standNear(player, sl, wanted, 3);
        if (to == null || to.distanceToSqr(from) < 4.0) {
            Spells.tell(player, Component.literal("There is nowhere to fold to."));
            return AbilityWheel.Result.REFUSED;
        }
        List<LivingEntity> folded = Spells.enemiesOnLine(player, sl, from.add(0, 1, 0), to.add(0, 1, 0), 1.2);
        if (target != null && !folded.contains(target)) folded.add(target);
        if (!Teleport.of(player).fallGuard(60).go(sl, to)) return AbilityWheel.Result.REFUSED;
        ObserverFx.foldSpace(sl, player, from, to);
        Later.run(sl, ObserverFx.FOLD_ARRIVES, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            for (LivingEntity one : folded) {
                if (!one.isAlive()) continue;
                ObserverFx.foldStruck(sl, one);
                Spells.strike(player, one, FOLD_DAMAGE);
                Root.apply(one, Spells.holdLength(one, FOLD_HOLD), RootRestriction.MOVEMENT);
            }
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Once a second, for any Observer (and the shared state)
    // =========================

    static void tick(ServerLevel sl) {
        resonanceTick(sl);
        harmonyTick();
    }

    static void forget(Player player) {
        HARMONIZED.remove(player.getUUID());
        player.getPersistentData().remove(HARMONY_TAG);
    }
}
