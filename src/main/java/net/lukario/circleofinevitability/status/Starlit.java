package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Starlit: a mark of starlight one player puts on a few creatures. It says "this one is caught": the Key Of Stars'
 * Exile and Starlight Seal need it, and its caster and their allies see it through walls. Each player has their own
 * marks, at most MAX_PER_CASTER at once; a new one past that replaces their oldest. Works from any class/ability:
 *
 *   Starlit.mark(target, player, 240);         // 12 seconds
 *   Starlit.has(target, player)                // marked by this player?   Starlit.hasAny(target)
 *   Starlit.ticksLeft(target, player)
 *   Starlit.consume(target, player);           // used up (an Exile that needed it): true if it was there
 *   Starlit.marked(player)                     // every creature this player has Starlit now, oldest first
 *   Starlit.markers(target)                    // the players who have it Starlit
 *   Starlit.addListener((caster, target) -> ...)   // once, in a static block: a mark ended (time up, replaced,
 *                                                  // consumed, the target died or left); for taking its look off
 *   Starlit.addImmunity(entity -> ...)          // once, in a static block: nothing can mark these (mark says false)
 *
 * Nothing shows by itself: the Pathway draws it (KeyOfStarsFx.starlit, starlitSeen each second).
 * Not saved: marks are gone after a restart, and when their caster logs out.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Starlit {

    /** How many creatures one player can have Starlit at once. */
    public static int MAX_PER_CASTER = 3;

    private static final List<Predicate<LivingEntity>> IMMUNE = new ArrayList<>();

    /** A mark ended: 'caster' may be null (logged out), 'target' may be null (gone). */
    @FunctionalInterface
    public interface Ended {
        void ended(Player caster, LivingEntity target);
    }

    private record Mark(ServerLevel level, UUID target, long until) {}

    private static final Map<UUID, List<Mark>> BY_CASTER = new HashMap<>();
    private static final List<Ended> LISTENERS = new ArrayList<>();

    private Starlit() {}

    // =========================
    // API
    // =========================

    /**
     * 'caster' Starlits 'target' for 'ticks' (marked again: its time starts over). False, and nothing is marked, for the
     * dead and for what an addImmunity rule keeps unmarked (status/Seal's sealed connections).
     */
    public static boolean mark(LivingEntity target, Player caster, int ticks) {
        if (!(target.level() instanceof ServerLevel sl) || ticks <= 0 || !target.isAlive()) return false;
        for (Predicate<LivingEntity> immune : IMMUNE) if (immune.test(target)) return false;
        List<Mark> marks = BY_CASTER.computeIfAbsent(caster.getUUID(), id -> new ArrayList<>());
        marks.removeIf(mark -> mark.target().equals(target.getUUID()));
        while (marks.size() >= Math.max(1, MAX_PER_CASTER)) {
            Mark oldest = marks.remove(0);
            tell(caster, entityOf(oldest));
        }
        marks.add(new Mark(sl, target.getUUID(), sl.getGameTime() + ticks));
        return true;
    }

    /** Once, in a static block: while it says true for an entity, nothing can Starlit it. */
    public static void addImmunity(Predicate<LivingEntity> immune) {
        IMMUNE.add(immune);
    }

    public static boolean has(Entity target, Player caster) {
        return find(target, caster) != null;
    }

    public static boolean hasAny(Entity target) {
        return !markers(target).isEmpty();
    }

    public static int ticksLeft(Entity target, Player caster) {
        Mark mark = find(target, caster);
        return mark == null ? 0 : (int) Math.max(0, mark.until() - mark.level().getGameTime());
    }

    /** Takes the mark off (an ability used it up). True if it was there. */
    public static boolean consume(Entity target, Player caster) {
        List<Mark> marks = BY_CASTER.get(caster.getUUID());
        if (marks == null || target == null) return false;
        boolean removed = marks.removeIf(mark -> mark.target().equals(target.getUUID()));
        if (removed) tell(caster, target instanceof LivingEntity living ? living : null);
        return removed;
    }

    /** Every creature this player has Starlit now (loaded and alive), oldest first. */
    public static List<LivingEntity> marked(Player caster) {
        List<LivingEntity> out = new ArrayList<>();
        List<Mark> marks = BY_CASTER.get(caster.getUUID());
        if (marks == null) return out;
        for (Mark mark : marks) {
            LivingEntity entity = entityOf(mark);
            if (entity != null && mark.until() > mark.level().getGameTime()) out.add(entity);
        }
        return out;
    }

    /** The players who have this creature Starlit now. */
    public static List<Player> markers(Entity target) {
        List<Player> out = new ArrayList<>();
        if (target == null) return out;
        for (Map.Entry<UUID, List<Mark>> entry : BY_CASTER.entrySet()) {
            for (Mark mark : entry.getValue()) {
                if (!mark.target().equals(target.getUUID()) || mark.until() <= mark.level().getGameTime()) continue;
                Player caster = casterOf(entry.getKey(), mark.level());
                if (caster != null) out.add(caster);
                break;
            }
        }
        return out;
    }

    public static void addListener(Ended listener) {
        LISTENERS.add(listener);
    }

    // =========================
    // Inside
    // =========================

    private static Mark find(Entity target, Player caster) {
        if (target == null || caster == null) return null;
        List<Mark> marks = BY_CASTER.get(caster.getUUID());
        if (marks == null) return null;
        for (Mark mark : marks) {
            if (mark.target().equals(target.getUUID()) && mark.until() > mark.level().getGameTime()) return mark;
        }
        return null;
    }

    /** The caster as they are now (a respawn makes a new player object), or null when they are not online. */
    private static Player casterOf(UUID id, ServerLevel any) {
        return any.getServer().getPlayerList().getPlayer(id);
    }

    private static LivingEntity entityOf(Mark mark) {
        return mark.level().getEntity(mark.target()) instanceof LivingEntity living && living.isAlive() ? living : null;
    }

    private static void tell(Player caster, LivingEntity target) {
        for (Ended listener : List.copyOf(LISTENERS)) {
            try {
                listener.ended(caster, target);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A Starlit listener failed", e);
            }
        }
    }

    // =========================
    // Events
    // =========================

    /** Once a second: marks whose time is up, or whose creature died or left, end. */
    @SubscribeEvent
    public static void onStarlitLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel level) || BY_CASTER.isEmpty() || level.getGameTime() % 20 != 0) return;
        for (Map.Entry<UUID, List<Mark>> entry : new ArrayList<>(BY_CASTER.entrySet())) {
            Player caster = casterOf(entry.getKey(), level);
            Iterator<Mark> it = entry.getValue().iterator();
            List<LivingEntity> ended = new ArrayList<>();
            boolean any = false;
            while (it.hasNext()) {
                Mark mark = it.next();
                if (mark.level() != level) continue;
                LivingEntity entity = entityOf(mark);
                if (entity == null || mark.until() <= level.getGameTime()) {
                    it.remove();
                    ended.add(entity);
                    any = true;
                }
            }
            if (entry.getValue().isEmpty()) BY_CASTER.remove(entry.getKey());
            if (any) for (LivingEntity entity : ended) tell(caster, entity);
        }
    }

    @SubscribeEvent
    public static void onStarlitLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        List<Mark> marks = BY_CASTER.remove(id);
        if (marks != null) for (Mark mark : marks) tell(event.getEntity(), entityOf(mark));
    }

    @SubscribeEvent
    public static void onStarlitLevelUnload(LevelEvent.Unload event) {
        for (List<Mark> marks : BY_CASTER.values()) marks.removeIf(mark -> mark.level() == event.getLevel());
    }
}
