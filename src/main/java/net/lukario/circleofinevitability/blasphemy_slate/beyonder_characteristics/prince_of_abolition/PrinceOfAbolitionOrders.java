package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Order: everything a Prince Of Abolition leaves standing in the world (docs/PRINCE_OF_ABOLITION.md, "Order").
 * A Compromised mark, a Bribe, a Connection, a Seal, a mausoleum: each one is one standing order while it lasts.
 * Nine count at most (ORDER_CAP). Shown beside the hotbar as "[sigil] Order 4/9" (PrinceOfAbolitionFx.order).
 *
 *   PrinceOfAbolitionOrders.add(player, "seal", until, null);           // one more, until that game time
 *   PrinceOfAbolitionOrders.add(player, key, until, target);            // one that ends when 'target' dies, too
 *   PrinceOfAbolitionOrders.remove(player, "seal");                     // it ended early
 *   int orders = PrinceOfAbolitionOrders.count(player);                 // 0..9
 *
 * What it does is read elsewhere: Law & Order (each cuts the damage taken by 2%), Majesty (a second longer for every
 * two). Not saved: a relog starts from the orders that are made again (mausoleums crumble on logout anyway).
 */
public final class PrinceOfAbolitionOrders {

    private PrinceOfAbolitionOrders() {}

    /** One standing order: until this game time, and on this entity (null: not on anyone). */
    private record Order(long until, UUID on) {}

    private static final Map<UUID, Map<String, Order>> ORDERS = new HashMap<>();
    /** The count last shown on each player's screen (-1 / missing: show it again). */
    private static final Map<UUID, Integer> SHOWN = new HashMap<>();

    /** One more standing order (the same key again only moves its end). */
    public static void add(Player player, String key, long until, Entity on) {
        if (player == null || player.level().isClientSide()) return;
        ORDERS.computeIfAbsent(player.getUUID(), id -> new LinkedHashMap<>())
                .put(key, new Order(until, on == null ? null : on.getUUID()));
        refresh(player, true);
    }

    /** It ended early. */
    public static void remove(Player player, String key) {
        if (player == null) return;
        Map<String, Order> orders = ORDERS.get(player.getUUID());
        if (orders != null && orders.remove(key) != null) refresh(player, false);
    }

    /** The same, when only the owner's id is known (a mark ending on an enemy). */
    static void remove(ServerLevel sl, UUID owner, String key) {
        if (owner == null) return;
        Player player = sl.getPlayerByUUID(owner);
        if (player != null) {
            remove(player, key);
            return;
        }
        Map<String, Order> orders = ORDERS.get(owner);
        if (orders != null) orders.remove(key);
    }

    /** How many orders the player has standing now, at most ORDER_CAP. */
    public static int count(Player player) {
        Map<String, Order> orders = ORDERS.get(player.getUUID());
        if (orders == null || orders.isEmpty()) return 0;
        long now = player.level().getGameTime();
        int live = 0;
        for (Order order : orders.values()) if (order.until() > now) live++;
        return Math.min(PrinceOfAbolitionFx.ORDER_CAP, live);
    }

    /** Once a second for a Prince Of Abolition: the ones that ran out go, and the counter shows the count. */
    static void tick(Player player) {
        Map<String, Order> orders = ORDERS.get(player.getUUID());
        if (orders != null) {
            long now = player.level().getGameTime();
            orders.values().removeIf(order -> order.until() <= now);
        }
        refresh(player, false);
    }

    /** Something died: every order that was on it goes (a mark does not outlive its bearer). */
    static void bearerDied(ServerLevel sl, Entity dead) {
        UUID id = dead.getUUID();
        for (Map.Entry<UUID, Map<String, Order>> entry : ORDERS.entrySet()) {
            if (entry.getValue().values().removeIf(order -> id.equals(order.on()))) {
                Player owner = sl.getPlayerByUUID(entry.getKey());
                if (owner != null) refresh(owner, false);
            }
        }
    }

    /** Logout, a lost Pathway: nothing stands any more and the counter goes. */
    static void clear(Player player) {
        ORDERS.remove(player.getUUID());
        SHOWN.remove(player.getUUID());
        PrinceOfAbolitionFx.orderEnd(player);
    }

    /** After a relog or a respawn the counter is drawn again at the next tick. */
    static void showAgain(Player player) {
        SHOWN.remove(player.getUUID());
    }

    /** Sends the count when it changed; 'raised' shows the small crown when it went up. */
    private static void refresh(Player player, boolean raised) {
        int count = count(player);
        Integer shown = SHOWN.get(player.getUUID());
        if (shown != null && shown == count) return;
        if (raised && player.level() instanceof ServerLevel sl && (shown == null || count > shown)) {
            PrinceOfAbolitionFx.orderRaised(sl, player);
        }
        SHOWN.put(player.getUUID(), count);
        PrinceOfAbolitionFx.order(player, count);
    }
}
