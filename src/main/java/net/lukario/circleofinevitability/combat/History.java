package net.lukario.circleofinevitability.combat;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraft.world.entity.LivingEntity;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * History: where every player was, and how hurt, over the last few seconds. For anything that goes back in time:
 * a step back to where you stood, a wound undone, an echo that walks your path again.
 *
 *   History.Moment then = History.ago(player, 60);       // 3 seconds ago (null: not known that far back, or another dimension)
 *   if (then != null) {
 *       player.teleportTo(then.position().x, then.position().y, then.position().z);
 *       float lost = History.healthLost(player, 60);     // what they lost since then (0 if they are better now)
 *   }
 *   History.forget(player);                               // start the record afresh (after a step back, so it cannot chain)
 *
 * A moment is written every EVERY ticks for every player on the server, and KEEP_TICKS of them are kept. It is not
 * saved, and logging out, dying or changing dimension starts it afresh (a moment in another world is no use).
 * Players are followed by themselves; anything else only while something calls History.write(entity) for it every
 * tick (a Beyonder mob whose ability steps back in time: blasphemy_slate/beyonder_mobs).
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class History {

    /** How far back it remembers (ticks). Recall goes 8 seconds back at most. */
    public static final int KEEP_TICKS = 200;
    /** How often a moment is written (ticks). */
    public static final int EVERY = 2;

    /** One moment: where they stood, which way they looked, their health, and when. */
    public record Moment(Vec3 position, float yRot, float xRot, float health, long tick, ResourceKey<Level> dimension) {}

    private static final Map<UUID, Deque<Moment>> RECORDS = new HashMap<>();

    private History() {}

    // =========================
    // API
    // =========================

    /**
     * Where the player was 'ticksAgo' ticks ago (the nearest moment written at or before then), or null when the record
     * does not go that far back (they just logged in, died, changed dimension or the record was forgotten).
     */
    public static Moment ago(LivingEntity player, int ticksAgo) {
        Deque<Moment> record = RECORDS.get(player.getUUID());
        if (record == null || record.isEmpty()) return null;
        long wanted = player.level().getGameTime() - Math.max(0, ticksAgo);
        Moment found = null;
        Iterator<Moment> newestFirst = record.descendingIterator();
        while (newestFirst.hasNext()) {
            Moment moment = newestFirst.next();
            if (moment.tick() <= wanted) {
                found = moment;
                break;
            }
        }
        if (found == null || found.dimension() != player.level().dimension()) return null;
        return found;
    }

    /** The oldest moment kept, at most 'ticksAgo' back, or null: "as far back as it goes". */
    public static Moment asFarAs(LivingEntity player, int ticksAgo) {
        Moment exact = ago(player, ticksAgo);
        if (exact != null) return exact;
        Deque<Moment> record = RECORDS.get(player.getUUID());
        if (record == null || record.isEmpty()) return null;
        Moment oldest = record.peekFirst();
        return oldest.dimension() == player.level().dimension() ? oldest : null;
    }

    /** The health lost since 'ticksAgo' ticks ago (0 when they have more now, or the record does not reach). */
    public static float healthLost(LivingEntity player, int ticksAgo) {
        Moment then = ago(player, ticksAgo);
        return then == null ? 0f : Math.max(0f, then.health() - player.getHealth());
    }

    /** Starts the player's record afresh. */
    public static void forget(LivingEntity player) {
        RECORDS.remove(player.getUUID());
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onHistoryPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        Player player = event.player();
        if (player.isSpectator()) return;
        write(player);
    }

    /** Called every tick for something that is no player, it keeps a record too (a moment every EVERY ticks). */
    public static void write(LivingEntity player) {
        if (player.level().isClientSide()) return;
        long now = player.level().getGameTime();
        if (now % EVERY != 0) return;
        Deque<Moment> record = RECORDS.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
        Moment last = record.peekLast();
        if (last != null && (last.dimension() != player.level().dimension() || !player.isAlive())) record.clear();
        if (!player.isAlive()) return;
        record.addLast(new Moment(player.position(), player.getYRot(), player.getXRot(), player.getHealth(), now,
                player.level().dimension()));
        while (!record.isEmpty() && now - record.peekFirst().tick() > KEEP_TICKS) record.removeFirst();
    }

    @SubscribeEvent
    public static void onHistoryDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player) && RECORDS.containsKey(event.getEntity().getUUID())) forget(event.getEntity());
    }

    @SubscribeEvent
    public static void onHistoryLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        forget(event.getEntity());
    }

    @SubscribeEvent
    public static void onHistoryRespawn(PlayerEvent.PlayerRespawnEvent event) {
        forget(event.getEntity());
    }
}
