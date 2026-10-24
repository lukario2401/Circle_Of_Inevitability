package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Chosen: ONE target a player has picked under a name, for a while. "The enemy I looked at" kept for later: the
 * Convict of a Punishment, the one memorized by Recognition, the signer of a Contract, a quarry, a duel. A player has
 * at most one target under each name; choosing another under the same name lets the first go. Works from any
 * class/ability:
 *
 *   Chosen.choose(player, "my_pathway_quarry", target, 600);     // 30 seconds; the old quarry (if any) is let go
 *   LivingEntity quarry = Chosen.of(player, "my_pathway_quarry");  // null: none, or it died, or it is in another world
 *   Chosen.is(target, player, "my_pathway_quarry")   Chosen.ticksLeft(player, name)   Chosen.release(player, name)
 *   Chosen.choosers(target, "my_pathway_quarry")     // every player who has this one chosen under that name
 *
 * Something to do when a choice ends, however it ends (its time is up, the target died, another was chosen, it was
 * released, the chooser logged out): once, in a static block,
 *
 *   Chosen.addListener("my_pathway_quarry", (chooser, target) -> MyFx.quarryEnd(...));
 *
 * (target is null when it is no longer there to be had.) Marks counts how much an entity carries; Chosen remembers
 * which entity one player meant. Not saved: a restart lets every choice go. Start the name with your Pathway.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Chosen {

    /** Runs when a choice ends. 'chooser' may be offline (null) at logout; 'target' is null when it is gone. */
    @FunctionalInterface
    public interface Ended {
        void ended(Player chooser, LivingEntity target);
    }

    private record Choice(UUID target, ServerLevel level, long until) {}

    /** chooser -> (name -> what was chosen). */
    private static final Map<UUID, Map<String, Choice>> CHOSEN = new HashMap<>();
    private static final Map<String, List<Ended>> LISTENERS = new HashMap<>();

    private Chosen() {}

    // =========================
    // API
    // =========================

    /** 'chooser' picks 'target' under 'name' for 'ticks'. Choosing the same one again only sets the time anew. */
    public static void choose(Player chooser, String name, LivingEntity target, int ticks) {
        if (ticks <= 0 || target == null || !(target.level() instanceof ServerLevel sl)) return;
        Map<String, Choice> mine = CHOSEN.computeIfAbsent(chooser.getUUID(), id -> new HashMap<>());
        Choice old = mine.get(name);
        if (old != null && !old.target().equals(target.getUUID())) {
            mine.remove(name);
            fire(chooser, name, find(old));
        }
        mine.put(name, new Choice(target.getUUID(), sl, sl.getGameTime() + ticks));
    }

    /** What 'chooser' has chosen under 'name', if it is still alive and in the world it was chosen in; else null. */
    public static LivingEntity of(Player chooser, String name) {
        Map<String, Choice> mine = CHOSEN.get(chooser.getUUID());
        if (mine == null) return null;
        Choice choice = mine.get(name);
        if (choice == null || choice.until() <= choice.level().getGameTime()) return null;
        LivingEntity found = find(choice);
        return found != null && found.isAlive() ? found : null;
    }

    public static boolean is(Entity target, Player chooser, String name) {
        LivingEntity chosen = of(chooser, name);
        return chosen != null && chosen == target;
    }

    public static int ticksLeft(Player chooser, String name) {
        Map<String, Choice> mine = CHOSEN.get(chooser.getUUID());
        Choice choice = mine == null ? null : mine.get(name);
        return choice == null ? 0 : (int) Math.max(0, choice.until() - choice.level().getGameTime());
    }

    /** Every player online who has 'target' chosen under 'name'. */
    public static List<Player> choosers(Entity target, String name) {
        List<Player> out = new ArrayList<>();
        if (!(target.level() instanceof ServerLevel sl)) return out;
        for (Map.Entry<UUID, Map<String, Choice>> entry : CHOSEN.entrySet()) {
            Choice choice = entry.getValue().get(name);
            if (choice == null || !choice.target().equals(target.getUUID()) || choice.until() <= sl.getGameTime()) continue;
            Player chooser = sl.getServer().getPlayerList().getPlayer(entry.getKey());
            if (chooser != null) out.add(chooser);
        }
        return out;
    }

    /** Lets the choice go now (its listeners run). Returns what had been chosen, or null. */
    public static LivingEntity release(Player chooser, String name) {
        Map<String, Choice> mine = CHOSEN.get(chooser.getUUID());
        if (mine == null) return null;
        Choice choice = mine.remove(name);
        if (mine.isEmpty()) CHOSEN.remove(chooser.getUUID());
        if (choice == null) return null;
        LivingEntity target = find(choice);
        fire(chooser, name, target);
        return target;
    }

    /** Once, in a static block: what to do when a choice under 'name' ends, however it ends. */
    public static void addListener(String name, Ended listener) {
        LISTENERS.computeIfAbsent(name, n -> new ArrayList<>()).add(listener);
    }

    // =========================
    // Internals
    // =========================

    private static LivingEntity find(Choice choice) {
        Entity entity = choice.level().getEntity(choice.target());
        return entity instanceof LivingEntity living ? living : null;
    }

    private static void fire(Player chooser, String name, LivingEntity target) {
        List<Ended> listeners = LISTENERS.get(name);
        if (listeners == null) return;
        for (Ended listener : listeners) {
            try {
                listener.ended(chooser, target);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A Chosen listener failed", e);
            }
        }
    }

    /** Ends every choice of this chooser whose time is up or whose target is gone. */
    private static void sweep(Player chooser, Map<String, Choice> mine) {
        Iterator<Map.Entry<String, Choice>> it = mine.entrySet().iterator();
        List<Map.Entry<String, LivingEntity>> ended = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry<String, Choice> entry = it.next();
            Choice choice = entry.getValue();
            LivingEntity target = find(choice);
            boolean over = choice.until() <= choice.level().getGameTime() || target == null || !target.isAlive();
            if (!over) continue;
            it.remove();
            ended.add(Map.entry(entry.getKey(), target == null || !target.isAlive() ? null : target));
        }
        // the listeners run after the loop: one may choose again
        for (Map.Entry<String, LivingEntity> one : ended) fire(chooser, one.getKey(), one.getValue());
    }

    // =========================
    // Events
    // =========================

    /** Twice a second for each chooser: what ran out or is gone is let go. */
    @SubscribeEvent
    public static void onChosenPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        Player player = event.player();
        if (player.level().isClientSide() || player.tickCount % 10 != 0) return;
        Map<String, Choice> mine = CHOSEN.get(player.getUUID());
        if (mine == null) return;
        sweep(player, mine);
        if (mine.isEmpty()) CHOSEN.remove(player.getUUID());
    }

    /** A chosen one died: every choice of it ends at once (the listeners see no target). */
    @SubscribeEvent
    public static void onChosenDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel sl) || CHOSEN.isEmpty()) return;
        List<Map.Entry<UUID, String>> ended = new ArrayList<>();
        for (Map.Entry<UUID, Map<String, Choice>> entry : CHOSEN.entrySet()) {
            entry.getValue().entrySet().removeIf(choice -> {
                if (!choice.getValue().target().equals(dead.getUUID())) return false;
                ended.add(Map.entry(entry.getKey(), choice.getKey()));
                return true;
            });
        }
        CHOSEN.values().removeIf(Map::isEmpty);
        for (Map.Entry<UUID, String> one : ended) {
            Player chooser = sl.getServer().getPlayerList().getPlayer(one.getKey());
            if (chooser != null) fire(chooser, one.getValue(), null);
        }
        // a chooser who died lets everything go
        if (dead instanceof Player chooser) releaseAll(chooser);
    }

    @SubscribeEvent
    public static void onChosenLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        releaseAll(event.getEntity());
    }

    private static void releaseAll(Player chooser) {
        Map<String, Choice> mine = CHOSEN.remove(chooser.getUUID());
        if (mine == null) return;
        for (Map.Entry<String, Choice> entry : mine.entrySet()) fire(chooser, entry.getKey(), find(entry.getValue()));
    }
}
