package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Residue: where Beyonder abilities were cast lately, by whom, of which Pathway and Sequence. What a cast leaves in the
 * world for an environment reader to find (the Illuminator's Survey from Arcane Scholar), a trail for a hunter.
 *
 *   for (Residue.Cast cast : Residue.near(sl, center, 64, 1200)) {
 *       cast.at();  cast.pathway();  cast.sequence();  cast.caster();  cast.age(sl);
 *   }
 *
 * Every time a player spends spirituality on an ability (SoulCore.setSpirituality going down) a cast is written where
 * they stand, at most one every NOTE_EVERY ticks for each player (an upkeep paid every second leaves a trail, not a
 * pile). A stolen cast that costs nothing leaves none. Kept KEEP_TICKS, not saved.
 */
public final class Residue {

    /** How long a cast is remembered (ticks). */
    public static int KEEP_TICKS = 1200;
    /** At most one cast written for each player this often (ticks). */
    public static int NOTE_EVERY = 20;
    /** At most this many casts are kept in all (the oldest go first). */
    public static int MAX_KEPT = 512;

    /** One cast: who, of which Pathway at which Sequence, where (and in which dimension), and when. */
    public record Cast(UUID caster, String pathway, int sequence, Vec3 at, ResourceKey<Level> dimension, long tick) {
        public long age(ServerLevel sl) {
            return sl.getGameTime() - tick;
        }
    }

    private static final LinkedList<Cast> CASTS = new LinkedList<>();
    private static final Map<UUID, Long> LAST_NOTED = new HashMap<>();

    private Residue() {}

    /** A player just spent spirituality on an ability (SoulCore calls this). */
    public static void spent(Player player) {
        if (!(player.level() instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        Long last = LAST_NOTED.get(player.getUUID());
        if (last != null && now - last < NOTE_EVERY && now >= last) return;
        LAST_NOTED.put(player.getUUID(), now);
        CASTS.addLast(new Cast(player.getUUID(), SoulCore.getPathway(player), SoulCore.getSequence(player),
                player.position(), sl.dimension(), now));
        while (CASTS.size() > MAX_KEPT) CASTS.removeFirst();
    }

    /** The casts within 'radius' of 'center' in this level in the last 'maxAgeTicks', newest first. */
    public static List<Cast> near(ServerLevel sl, Vec3 center, double radius, int maxAgeTicks) {
        forgetOld(sl);
        List<Cast> found = new ArrayList<>();
        Iterator<Cast> newestFirst = CASTS.descendingIterator();
        while (newestFirst.hasNext()) {
            Cast cast = newestFirst.next();
            if (cast.dimension() != sl.dimension() || cast.age(sl) > maxAgeTicks) continue;
            if (cast.at().distanceToSqr(center) <= radius * radius) found.add(cast);
        }
        return found;
    }

    private static void forgetOld(ServerLevel sl) {
        long now = sl.getGameTime();
        CASTS.removeIf(cast -> cast.dimension() == sl.dimension() && (now - cast.tick() > KEEP_TICKS || cast.tick() > now));
        if (LAST_NOTED.size() > 256) LAST_NOTED.clear();
    }
}
