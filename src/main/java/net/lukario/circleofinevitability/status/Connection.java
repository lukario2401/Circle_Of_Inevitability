package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Connection: who a player is connected to. A curse that reaches across the world, a scrying, a mark that finds its
 * bearer anywhere all need "someone I have touched". Every hit a player lands is a connection by itself (melee,
 * arrows, spells: anything whose damage is theirs); abilities can make one on purpose too:
 *
 *   Connection.touch(player, target);                     // connected now (a chain, a curse, a relic)
 *   Connection.has(player, target, 12000)                 // touched in the last 10 minutes?
 *   Connection.ticksSince(player, target)                 // how long ago (-1 = never, or forgotten)
 *   Connection.latest(player, 12000)                      // the newest one still alive anywhere on the server, or null
 *   Connection.all(player, 12000)                         // every one of them alive anywhere, newest first
 *   Connection.clear(player, target);   Connection.clearAll(player);
 *   Connection.addListener((owner, target, fresh) -> ...) // a connection was touched; fresh = new, or it had run out
 *
 * The question "how long ago still counts" is asked by the ability (10 minutes for one, 30 for a stronger one), so
 * one connection serves every ability. Nothing older than KEEP_TICKS is kept, and a player keeps their MAX_PER_OWNER
 * newest. Connections are not saved: a restart forgets them all. A target that dies is forgotten by everyone.
 * Time is the server's game time, so it counts the same in every dimension.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Connection {

    /** The longest anything is remembered (ticks): 30 minutes. */
    public static int KEEP_TICKS = 36000;
    /** How many connections one player keeps at most (the oldest go first). */
    public static int MAX_PER_OWNER = 64;
    /** False: hits no longer make connections by themselves, only touch(...) does. */
    public static boolean HITS_CONNECT = true;

    /** A connection was touched. fresh = it is new, or it was older than KEEP_TICKS. */
    @FunctionalInterface
    public interface Listener {
        void touched(Player owner, LivingEntity target, boolean fresh);
    }

    /** owner -> (target -> game time it was last touched), oldest first. */
    private static final Map<UUID, LinkedHashMap<UUID, Long>> CONNECTIONS = new HashMap<>();
    private static final List<Listener> LISTENERS = new ArrayList<>();

    private Connection() {}

    // =========================
    // API
    // =========================

    /** Connects 'owner' to 'target' now (or renews it). */
    public static void touch(Player owner, LivingEntity target) {
        if (owner == null || target == null || owner == target || !(owner.level() instanceof ServerLevel sl)) return;
        long now = now(sl.getServer());
        LinkedHashMap<UUID, Long> mine = CONNECTIONS.computeIfAbsent(owner.getUUID(), id -> new LinkedHashMap<>());
        Long had = mine.remove(target.getUUID());                 // put back at the end: newest last
        mine.put(target.getUUID(), now);
        forgetOld(mine, now);
        boolean fresh = had == null || now - had > KEEP_TICKS;
        for (Listener listener : LISTENERS) {
            try {
                listener.touched(owner, target, fresh);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A connection listener failed", e);
            }
        }
    }

    /** True if 'owner' touched 'target' within the last 'withinTicks'. */
    public static boolean has(Player owner, Entity target, int withinTicks) {
        long since = ticksSince(owner, target);
        return since >= 0 && since <= withinTicks;
    }

    /** Ticks since 'owner' last touched 'target', or -1 if never (or forgotten). */
    public static long ticksSince(Player owner, Entity target) {
        if (owner == null || target == null || !(owner.level() instanceof ServerLevel sl)) return -1;
        LinkedHashMap<UUID, Long> mine = CONNECTIONS.get(owner.getUUID());
        Long at = mine == null ? null : mine.get(target.getUUID());
        if (at == null) return -1;
        long since = now(sl.getServer()) - at;
        return since > KEEP_TICKS || since < 0 ? -1 : since;
    }

    /** Every connection touched within 'withinTicks' that is alive somewhere on the server, newest first. */
    public static List<LivingEntity> all(Player owner, int withinTicks) {
        List<LivingEntity> found = new ArrayList<>();
        if (owner == null || !(owner.level() instanceof ServerLevel sl)) return found;
        LinkedHashMap<UUID, Long> mine = CONNECTIONS.get(owner.getUUID());
        if (mine == null) return found;
        long now = now(sl.getServer());
        List<UUID> newestFirst = new ArrayList<>(mine.keySet());
        Collections.reverse(newestFirst);
        for (UUID id : newestFirst) {
            if (now - mine.get(id) > withinTicks) break;          // the rest are older still
            LivingEntity living = find(sl.getServer(), id);
            if (living != null) found.add(living);
        }
        return found;
    }

    /** The newest connection touched within 'withinTicks' that is alive somewhere on the server, or null. */
    public static LivingEntity latest(Player owner, int withinTicks) {
        List<LivingEntity> all = all(owner, withinTicks);
        return all.isEmpty() ? null : all.get(0);
    }

    /** The living entity with this id in any loaded dimension, or null. */
    public static LivingEntity find(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(id) instanceof LivingEntity living && living.isAlive()) return living;
        }
        return null;
    }

    public static void clear(Player owner, Entity target) {
        LinkedHashMap<UUID, Long> mine = CONNECTIONS.get(owner.getUUID());
        if (mine != null) mine.remove(target.getUUID());
    }

    public static void clearAll(Player owner) {
        CONNECTIONS.remove(owner.getUUID());
    }

    /** Runs for every touch: the look of a connection made, a sound only the owner hears. */
    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    // =========================
    // Internals
    // =========================

    private static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    private static void forgetOld(LinkedHashMap<UUID, Long> mine, long now) {
        Iterator<Map.Entry<UUID, Long>> iterator = mine.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            boolean tooMany = mine.size() > MAX_PER_OWNER;
            if (tooMany || now - entry.getValue() > KEEP_TICKS || entry.getValue() > now) iterator.remove();
            else break;                                           // oldest first: the rest are newer
        }
    }

    // =========================
    // Events
    // =========================

    // LOWEST: only a hit that really lands (not cancelled, not cut to nothing) connects
    @SubscribeEvent(priority = Priority.LOWEST)
    public static void onConnectionDamage(LivingDamageEvent event) {
        if (!HITS_CONNECT || event.getAmount() <= 0f) return;
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !(event.getSource().getEntity() instanceof Player owner) || owner == victim) return;
        touch(owner, victim);
    }

    @SubscribeEvent
    public static void onConnectionDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide() || CONNECTIONS.isEmpty() || dead instanceof Player) return;   // a player comes back
        UUID id = dead.getUUID();
        for (LinkedHashMap<UUID, Long> mine : CONNECTIONS.values()) mine.remove(id);
    }
}
