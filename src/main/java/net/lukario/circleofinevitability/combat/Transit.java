package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.FallGuard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Transit: the caster becomes part of something they throw, travels with it and steps out where it lands.
 * A flame they merge with, a shadow that slides along the ground, a card they fold into, lightning they ride:
 *
 *   boolean went = Transit.by(LANCE.onHit((target, at) -> Spells.strike(player, target, 30f)))   // any Shot
 *           .onDepart(from -> SpellFx.fireBurst(sl, from, 2.5))          // where they left (their feet)
 *           .onArrive((at, hitBlock) -> SpellFx.fireBurst(sl, at, 2.5))  // where they came out (their feet)
 *           .go(player, sl, hand, Spells.aimFrom(player, sl, hand, 20));
 *
 * While it flies the caster is concealed (unseen, unhurt, mobs forget them) and held where they stood, so
 * their body is "in" the shot; when it lands they appear there, facing the way they went, with no fall damage
 * for a few seconds. .visible() leaves them seen and hurtable meanwhile (they are only held in place).
 *
 * The shot keeps its own onHit: it still hurts what it flies into. Its onEnd is taken over: put what should
 * happen at the end into onArrive instead.
 *
 * go(...) returns false, and does nothing, when the caster may not travel (rooted with TELEPORT). Check
 * Transit.canGo(player) first if the cast should cost nothing then. Where it lands is never inside a block:
 * against a wall they come out in front of it; when there is no room at all they stay where they were (and
 * onArrive still runs, there).
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class Transit {

    @FunctionalInterface
    public interface Depart {
        /** 'from' = the caster's feet when they left. */
        void depart(Vec3 from);
    }

    @FunctionalInterface
    public interface Arrive {
        /** 'at' = the caster's feet where they came out; hitBlock = the shot ended on a block. */
        void arrive(Vec3 at, boolean hitBlock);
    }

    /** No fall damage for this long after coming out (they may come out in the air, at a wall). */
    public static int LANDING_TICKS = 100;
    /** How far back from where the shot stopped they come out, so they are not inside the wall it hit. */
    private static final double STEP_BACK = 0.7;
    /** How far around that spot a free place is looked for, when it is not free. */
    private static final double SEARCH = 2.5;

    private final Shot shot;
    private boolean hidden = true;
    private Depart onDepart = null;
    private Arrive onArrive = null;

    private Transit(Shot shot) {
        this.shot = shot;
    }

    /** Travel inside this shot. Nothing happens until .go(...). */
    public static Transit by(Shot shot) {
        return new Transit(shot);
    }

    /** The caster stays visible and can be hurt during the flight (default: concealed). */
    public Transit visible() {
        this.hidden = false;
        return this;
    }

    /** Runs right before they leave, with their feet. */
    public Transit onDepart(Depart action) {
        this.onDepart = action;
        return this;
    }

    /** Runs right after they came out, with their feet there. */
    public Transit onArrive(Arrive action) {
        this.onArrive = action;
        return this;
    }

    /** False when the caster may not travel right now (rooted with TELEPORT). */
    public static boolean canGo(Player player) {
        return !Root.has(player, RootRestriction.TELEPORT);
    }

    /**
     * Fires the shot from 'from' along 'direction' and sends the caster with it.
     * Returns false (nothing happens, nothing is fired) when they may not travel.
     */
    public boolean go(Player player, ServerLevel sl, Vec3 from, Vec3 direction) {
        if (!canGo(player)) return false;
        Vec3 way = direction.normalize();
        Vec3 left = player.position();
        if (onDepart != null) onDepart.depart(left);

        int flight = shot.onEnd((at, hitBlock) -> arrive(player, sl, at, way, hitBlock, left)).fire(player, sl, from, way);

        // the body goes into the shot: held where it stood (and unseen) until the shot lands
        Root.apply(player, flight + 2, RootRestriction.MOVE, RootRestriction.JUMP);
        if (hidden) Concealment.hide(player, flight + 2);
        return true;
    }

    private void arrive(Player player, ServerLevel sl, Vec3 stop, Vec3 way, boolean hitBlock, Vec3 left) {
        if (!Spells.casterStillHere(player, sl)) return;            // died or left on the way: nothing to bring out
        Root.remove(player, RootRestriction.MOVE, RootRestriction.JUMP);
        if (hidden) Concealment.reveal(player);

        Vec3 feet = landing(player, sl, stop, way);
        if (feet == null) feet = player.position();                  // nowhere to come out: back where they were
        player.teleportTo(feet.x, feet.y, feet.z);
        player.fallDistance = 0;
        FallGuard.protect(player, LANDING_TICKS);
        if (onArrive != null) onArrive.arrive(feet, hitBlock);
    }

    /** Where the caster's feet go for a shot that stopped at 'stop': a step back from it, never inside a block. */
    private static Vec3 landing(Player player, ServerLevel sl, Vec3 stop, Vec3 way) {
        Vec3 back = stop.subtract(way.scale(STEP_BACK));
        Vec3 feet = back.add(0, -player.getBbHeight() * 0.5, 0);    // the middle of the body where the shot was
        if (Spells.fits(player, sl, feet)) return feet;
        Vec3 ground = Spells.groundNear(player, sl, back, 1.5);       // standing on the ground right under it
        if (ground != null && Spells.fits(player, sl, ground)) return ground;
        return Spells.freeSpotNear(player, sl, back, way.scale(-1), SEARCH);
    }
}
