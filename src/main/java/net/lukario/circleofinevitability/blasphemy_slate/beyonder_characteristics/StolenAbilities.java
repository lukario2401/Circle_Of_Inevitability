package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.combat.HudTimer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The abilities a player holds that are not their own: who holds what, for how long, and whom it was taken from.
 * Worm Of Time's theft uses it; anything else that lends, steals or copies an ability can too.
 *
 *   Held held = StolenAbilities.hold(thief, ability, sneakForm, victim, 1200, 5); // victim may be null (a mob); 5 at most
 *   Held newest = StolenAbilities.newest(thief);                                 // the one a release casts, or null
 *   StolenAbilities.spend(thief, newest);                                        // used up: the victim has it back
 *
 * While a player's ability is held by someone, that player cannot use its key: the ability keys ask
 * blocksKey(player, key) first (network/SKeyPressAbility...Used). They get it back when it is spent, when its
 * time is up, or when the holder dies or logs out.
 *
 * The holder sees each held ability as a clock by the hotbar (combat/HudTimer), and so does the one it was taken
 * from. Nothing here is saved: a server restart gives everything back.
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class StolenAbilities {

    private static final int HELD_COLOR = 0x52E3C4;   // the clock of the one who holds it
    private static final int LOST_COLOR = 0xFF7A6A;   // the clock of the one it was taken from

    /**
     * ability:   what is held.
     * sneakForm: true if its sneak form comes out when it is released.
     * victim:    the player it was taken from, or null (a mob, or nobody).
     * until:     the game time at which it runs out.
     * slot:      0, 1, 2... the lowest one free when it was taken, kept for as long as it is held. For showing each
     *            one in a place of its own.
     */
    public record Held(PathwayAbilities.Ability ability, boolean sneakForm, UUID victim, long until, int slot) {

        /** "Dash" or "Dash (sneak)". */
        public String label() {
            return sneakForm ? ability.name() + " (sneak)" : ability.name();
        }
    }

    /** holder -> what they hold, oldest first (so the newest is last). */
    private static final Map<UUID, List<Held>> HELD = new HashMap<>();

    private StolenAbilities() {}

    // =========================
    // API
    // =========================

    /**
     * Gives 'ability' to the holder for 'ticks' ticks. 'victim' loses the use of it meanwhile (null = taken from
     * no player). 'maxHeld' is how many the holder may hold at once (0 or less = no limit); the ability that
     * steals decides it. Returns what is now held, or null if the holder already holds 'maxHeld' (see isFull).
     */
    public static Held hold(Player holder, PathwayAbilities.Ability ability, boolean sneakForm, Player victim, int ticks, int maxHeld) {
        if (isFull(holder, maxHeld)) return null;
        List<Held> list = HELD.computeIfAbsent(holder.getUUID(), id -> new ArrayList<>());

        int slot = 0;
        while (slotTaken(list, slot)) slot++;
        Held held = new Held(ability, sneakForm, victim == null ? null : victim.getUUID(),
                holder.level().getGameTime() + ticks, slot);
        list.add(held);

        HudTimer.show(holder, holderClock(held), held.label(), ticks, HELD_COLOR);
        if (victim != null) {
            HudTimer.show(victim, victimClock(ability), "Lost: " + ability.name(), ticks, LOST_COLOR);
        }
        return held;
    }

    public static int count(Player holder) {
        List<Held> list = HELD.get(holder.getUUID());
        return list == null ? 0 : list.size();
    }

    /** True if the holder holds 'maxHeld' or more already (never true when 'maxHeld' is 0 or less: no limit). */
    public static boolean isFull(Player holder, int maxHeld) {
        return maxHeld > 0 && count(holder) >= maxHeld;
    }

    /** The one taken last: what a release casts (newest first). Null if the player holds nothing. */
    public static Held newest(Player holder) {
        List<Held> list = HELD.get(holder.getUUID());
        return list == null || list.isEmpty() ? null : list.get(list.size() - 1);
    }

    /** True while the holder still holds exactly this one (it was not spent and has not run out). */
    public static boolean holds(Player holder, Held held) {
        List<Held> list = HELD.get(holder.getUUID());
        return list != null && list.contains(held);
    }

    /** The holder used it up: it is gone from them, and the player it was taken from has it back. */
    public static void spend(Player holder, Held held) {
        List<Held> list = HELD.get(holder.getUUID());
        if (list == null || !list.remove(held)) return;
        if (list.isEmpty()) HELD.remove(holder.getUUID());
        HudTimer.hide(holder, holderClock(held));
        if (holder.level() instanceof ServerLevel sl) giveBack(held, sl);
    }

    /** True if this ability of this player is held by someone else right now. */
    public static boolean isTaken(Player victim, PathwayAbilities.Ability ability) {
        UUID id = victim.getUUID();
        for (List<Held> list : HELD.values()) {
            for (Held held : list) {
                if (id.equals(held.victim()) && held.ability() == ability) return true;
            }
        }
        return false;
    }

    /**
     * For the ability keys: true if the ability this player has on that key (1..7) was stolen from them.
     * Tells them so. A key press that gets true does nothing.
     */
    public static boolean blocksKey(Player player, int key) {
        UUID id = player.getUUID();
        for (List<Held> list : HELD.values()) {
            for (Held held : list) {
                if (id.equals(held.victim()) && held.ability().key() == key) {
                    player.sendSystemMessage(Component.literal("Your " + held.ability().name() + " was stolen. It is not yours to use right now."));
                    return true;
                }
            }
        }
        return false;
    }

    // =========================
    // Inner workings
    // =========================

    private static boolean slotTaken(List<Held> list, int slot) {
        for (Held held : list) {
            if (held.slot() == slot) return true;
        }
        return false;
    }

    private static String holderClock(Held held) {
        return "stolen_ability_" + held.slot();
    }

    private static String victimClock(PathwayAbilities.Ability ability) {
        return "lost_ability_" + ability.key();
    }

    /** Tells the player it was taken from (if they are online) that it is theirs again. */
    private static void giveBack(Held held, ServerLevel sl) {
        if (held.victim() == null) return;
        for (ServerPlayer online : sl.getServer().getPlayerList().getPlayers()) {
            if (!online.getUUID().equals(held.victim())) continue;
            HudTimer.hide(online, victimClock(held.ability()));
            online.sendSystemMessage(Component.literal("Your " + held.ability().name() + " returns to you."));
            return;
        }
    }

    /** Everything this player holds goes back (they died or left). */
    private static void dropAll(Player holder) {
        List<Held> list = HELD.remove(holder.getUUID());
        if (list == null) return;
        for (Held held : list) {
            HudTimer.hide(holder, holderClock(held));
            if (holder.level() instanceof ServerLevel sl) giveBack(held, sl);
        }
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onStolenAbilitiesPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        Player holder = event.player();
        if (!(holder.level() instanceof ServerLevel sl)) return;
        List<Held> list = HELD.get(holder.getUUID());
        if (list == null) return;

        long now = sl.getGameTime();
        for (Held held : new ArrayList<>(list)) {
            if (held.until() > now) continue;
            list.remove(held);                           // its clock by the hotbar has run out by itself
            holder.sendSystemMessage(Component.literal("The stolen " + held.label() + " slips away from you."));
            giveBack(held, sl);
        }
        if (list.isEmpty()) HELD.remove(holder.getUUID());
    }

    @SubscribeEvent
    public static void onStolenAbilitiesDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide()) dropAll(player);
    }

    @SubscribeEvent
    public static void onStolenAbilitiesLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!event.getEntity().level().isClientSide()) dropAll(event.getEntity());
    }
}
