package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.Unbound;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Teleport: the one place for an ability to move someone from here to there at once. It asks whether they may go
 * (a Teleport root, or a hold such as a Decree against passage), puts them there, takes the fall off and, if asked,
 * shakes off what controls them. Every Key Of Stars door, hop and blink goes through it; any Pathway's can.
 *
 *   boolean went = Teleport.of(player).go(sl, there);                     // false (nothing happens) when rooted
 *   Teleport.of(player).fallGuard(40).go(sl, there);                       // no fall damage for 2 s after
 *   Teleport.of(player).ignoringTeleportRoots().go(sl, there);             // an Apprentice's Door Opening
 *   Teleport.of(player).unbinding().go(sl, there);                         // mind control lets go (status/Unbound)
 *   Teleport.canGo(player)                                                 // ask first, so a refusal costs nothing
 *
 * Finding where to go:
 *   Teleport.fits(entity, sl, feet)                    // its body fits with its feet there (any entity)
 *   Vec3 spot = Teleport.standNear(entity, sl, wanted, 3)   // the nearest spot within 3 blocks where it fits, or null
 *   Teleport.WallPass pass = Teleport.throughWall(player, sl, 6)   // a wall right ahead, no thicker than 6 blocks:
 *                                                      // pass.face() where it starts, pass.exit() the far side; or null
 * (Spells.blinkSpot and Spells.freeSpotNear find spots toward the crosshair and to the side.)
 *
 * Something that should know about every teleport made through here (a Safe Landing, a trail left behind):
 *   Teleport.addListener((entity, from, to) -> ...)    // once, in a static block
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class Teleport {

    /** Told after every teleport made through here. */
    @FunctionalInterface
    public interface Listener {
        void moved(LivingEntity entity, Vec3 from, Vec3 to);
    }

    /** A wall in front of someone: where it starts (on their side, at their feet) and where they come out. */
    public record WallPass(Vec3 face, Vec3 exit) {}

    private static final List<Listener> LISTENERS = new ArrayList<>();

    private final LivingEntity who;
    private boolean ignoreRoots;
    private int fallGuard;
    private boolean unbind;

    private Teleport(LivingEntity who) {
        this.who = who;
    }

    public static Teleport of(LivingEntity who) {
        return new Teleport(who);
    }

    /** It goes even with a Teleport root or a hold on it (not with a root on everything: that stops casting). */
    public Teleport ignoringTeleportRoots() {
        this.ignoreRoots = true;
        return this;
    }

    /** No fall damage for this many ticks after it arrives. */
    public Teleport fallGuard(int ticks) {
        this.fallGuard = Math.max(0, ticks);
        return this;
    }

    /** Arriving also ends the control on it (status/Unbound.breakControl): a mind snatched away with the body. */
    public Teleport unbinding() {
        this.unbind = true;
        return this;
    }

    /** Puts it at 'feet'. False, and nothing happens, when it may not go. */
    public boolean go(ServerLevel sl, Vec3 feet) {
        if (feet == null || !who.isAlive() || who.level() != sl) return false;
        if (!ignoreRoots && !canGo(who)) return false;
        Vec3 from = who.position();
        who.teleportTo(feet.x, feet.y, feet.z);
        who.fallDistance = 0;
        if (fallGuard > 0) FallGuard.protect(who, fallGuard);
        if (unbind) Unbound.breakControl(who);
        for (Listener listener : List.copyOf(LISTENERS)) {
            try {
                listener.moved(who, from, feet);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A teleport listener failed", e);
            }
        }
        return true;
    }

    // =========================
    // Asking
    // =========================

    /** False while a Teleport root (or a hold that acts as one, such as a Decree against passage) is on it. */
    public static boolean canGo(Entity entity) {
        return !Root.has(entity, RootRestriction.TELEPORT);
    }

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    /** True if the entity's body fits with its feet on this spot. */
    public static boolean fits(Entity entity, ServerLevel sl, Vec3 feet) {
        return sl.noCollision(entity, entity.getBoundingBox().move(feet.subtract(entity.position())));
    }

    /**
     * The spot nearest to 'wanted' (no more than 'search' blocks away) where the entity fits: 'wanted' itself, a
     * little higher or lower, then in rings round it. Null if there is none.
     */
    public static Vec3 standNear(Entity entity, ServerLevel sl, Vec3 wanted, double search) {
        for (double up : new double[]{0.0, 1.0, -1.0, 2.0}) {
            Vec3 spot = wanted.add(0, up, 0);
            if (fits(entity, sl, spot)) return spot;
        }
        for (double r = 1.0; r <= search; r += 1.0) {
            int steps = Math.max(8, (int) (r * 8));
            for (int i = 0; i < steps; i++) {
                double a = Math.PI * 2 * i / steps;
                for (double up : new double[]{0.0, 1.0, -1.0}) {
                    Vec3 spot = wanted.add(Math.cos(a) * r, up, Math.sin(a) * r);
                    if (fits(entity, sl, spot)) return spot;
                }
            }
        }
        return null;
    }

    /**
     * The wall right in front of a player (no further than 1.6 blocks ahead, the way they face on the flat) and the
     * first spot beyond it, no more than 'maxThickness' blocks of wall later, where they fit (it may be a step up or
     * down). Null when there is no wall there, or it is thicker than that, or there is no room behind it.
     */
    public static WallPass throughWall(Player player, ServerLevel sl, double maxThickness) {
        Vec3 start = player.position();
        Vec3 way = Spells.flatLook(player);
        double step = 0.25;
        double wallAt = -1;
        for (double d = 0.25; d <= 1.6; d += step) {
            if (!fits(player, sl, start.add(way.scale(d)))) {
                wallAt = d;
                break;
            }
        }
        if (wallAt < 0) return null;
        Vec3 face = start.add(way.scale(Math.max(0.0, wallAt - 0.3)));
        for (double d = wallAt + step; d <= wallAt + maxThickness + 1.0; d += step) {
            Vec3 at = start.add(way.scale(d));
            for (double up : new double[]{0.0, 1.0, -1.0}) {
                Vec3 feet = at.add(0, up, 0);
                if (!fits(player, sl, feet)) continue;
                Vec3 ground = Spells.groundAt(player, sl, feet);                // stand on what is there, not in the air
                Vec3 exit = feet.y - ground.y <= 1.5 && fits(player, sl, ground) ? ground : feet;
                return new WallPass(face, exit);
            }
        }
        return null;
    }
}
