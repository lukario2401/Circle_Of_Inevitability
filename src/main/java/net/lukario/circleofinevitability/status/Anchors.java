package net.lukario.circleofinevitability.status;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Anchors: places a player has saved, to go back to later. Key Of Stars' Star Anchors are built on it; a recall
 * point, a marked grave, a waypoint of any Pathway would be too. Every name is its own list, so start it with your
 * class. Works from any class/ability:
 *
 *   Anchor dropped = Anchors.add(player, "key_of_stars_anchor", sl, spot, 12000, 3);  // 10 minutes, at most 3:
 *                                                         // past that the oldest goes (returned, so its look can go)
 *   List<Anchors.Anchor> all = Anchors.list(player, "key_of_stars_anchor");          // oldest first, live ones only
 *   Anchors.Anchor here = Anchors.removeNear(player, "key_of_stars_anchor", sl, spot, 1.5);   // picked up (or null)
 *   Anchors.Anchor aimed = Anchors.aimed(player, "key_of_stars_anchor", 10);   // the one in this dimension nearest
 *                                                         // to the crosshair, no more than 10 degrees off (or null)
 *   Anchors.clear(player, "key_of_stars_anchor");
 *
 * an Anchor has its spot (at), its dimension (dimension, as Anchors.dimensionOf(level) writes it; isIn(level) asks)
 * and the game time it is gone (until). Saved with the player, and kept through death; the game time keeps running
 * in every dimension.
 * Going to one in another dimension: Teleport.of(player).goTo(Teleport.levelNamed(server, anchor.dimension()), anchor.at()).
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class Anchors {

    private static final String TAG = "circle_of_inevitability_anchors_";

    public record Anchor(String dimension, Vec3 at, long until) {
        public boolean isIn(Level level) {
            return dimension.equals(dimensionOf(level));
        }
    }

    private Anchors() {}

    /** How a dimension is written in an Anchor. */
    public static String dimensionOf(Level level) {
        return level.dimension().toString();
    }

    // =========================
    // API
    // =========================

    /**
     * Saves 'at' in this level for 'ticks', keeping at most 'max' of this name (the oldest goes first). Returns the
     * anchor that had to go to make room, or null.
     */
    public static Anchor add(Player owner, String name, ServerLevel sl, Vec3 at, int ticks, int max) {
        List<Anchor> anchors = list(owner, name);
        Anchor dropped = null;
        while (!anchors.isEmpty() && anchors.size() >= Math.max(1, max)) dropped = anchors.remove(0);
        anchors.add(new Anchor(dimensionOf(sl), at, sl.getGameTime() + ticks));
        save(owner, name, anchors);
        return dropped;
    }

    /** The live anchors of this name, oldest first (the ones whose time is up are dropped as it reads). */
    public static List<Anchor> list(Player owner, String name) {
        CompoundTag all = kept(owner).getCompoundOrEmpty(TAG + name);
        int count = all.getIntOr("Count", 0);
        long now = owner.level().getGameTime();
        List<Anchor> anchors = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CompoundTag one = all.getCompoundOrEmpty(String.valueOf(i));
            long until = one.getLongOr("Until", 0L);
            if (until <= now) continue;
            anchors.add(new Anchor(one.getStringOr("Dim", ""),
                    new Vec3(one.getDoubleOr("X", 0.0), one.getDoubleOr("Y", 0.0), one.getDoubleOr("Z", 0.0)), until));
        }
        if (anchors.size() != count) save(owner, name, anchors);
        return anchors;
    }

    /** The anchor of this name within 'within' blocks of 'at' in this level, taken away; null if there is none. */
    public static Anchor removeNear(Player owner, String name, ServerLevel sl, Vec3 at, double within) {
        List<Anchor> anchors = list(owner, name);
        Anchor nearest = null;
        double best = within * within;
        for (Anchor anchor : anchors) {
            if (!anchor.isIn(sl)) continue;
            double d = anchor.at().distanceToSqr(at);
            if (d <= best) {
                best = d;
                nearest = anchor;
            }
        }
        if (nearest != null) {
            anchors.remove(nearest);
            save(owner, name, anchors);
        }
        return nearest;
    }

    /**
     * The anchor of this name in the owner's dimension that lies nearest to where they look, if it is no more than
     * 'maxDegrees' off the crosshair (any distance). Null if none is.
     */
    public static Anchor aimed(Player owner, String name, double maxDegrees) {
        Vec3 eye = owner.getEyePosition();
        Vec3 look = owner.getLookAngle().normalize();
        double best = Math.cos(Math.toRadians(maxDegrees));
        Anchor aimed = null;
        for (Anchor anchor : list(owner, name)) {
            if (!anchor.isIn(owner.level())) continue;
            Vec3 to = anchor.at().add(0, 1.0, 0).subtract(eye);
            if (to.lengthSqr() < 1.0E-4) continue;
            double dot = to.normalize().dot(look);
            if (dot >= best) {
                best = dot;
                aimed = anchor;
            }
        }
        return aimed;
    }

    public static void clear(Player owner, String name) {
        CompoundTag kept = kept(owner);
        kept.remove(TAG + name);
        owner.getPersistentData().put(ServerPlayer.PERSISTED_NBT_TAG, kept);
    }

    /** Kept in the part of the player's data that survives death, so the places are still there after a respawn. */
    private static CompoundTag kept(Player owner) {
        return owner.getPersistentData().getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG);
    }

    private static void save(Player owner, String name, List<Anchor> anchors) {
        if (anchors.isEmpty()) {
            clear(owner, name);
            return;
        }
        CompoundTag all = new CompoundTag();
        all.putInt("Count", anchors.size());
        for (int i = 0; i < anchors.size(); i++) {
            Anchor anchor = anchors.get(i);
            CompoundTag one = new CompoundTag();
            one.putString("Dim", anchor.dimension());
            one.putDouble("X", anchor.at().x);
            one.putDouble("Y", anchor.at().y);
            one.putDouble("Z", anchor.at().z);
            one.putLong("Until", anchor.until());
            all.put(String.valueOf(i), one);
        }
        CompoundTag kept = kept(owner);
        kept.put(TAG + name, all);
        owner.getPersistentData().put(ServerPlayer.PERSISTED_NBT_TAG, kept);
    }
}
