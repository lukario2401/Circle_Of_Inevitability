package net.lukario.frogerealm.combat;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Run something a few ticks from now, on the server. Handy for attacks with stages:
 *
 *   Later.run(serverLevel, 5, () -> ParticleShapes.burst(serverLevel, SHARD, where, 20, 0.1, 0.3));
 *
 * Capture positions (Vec3) rather than entities when you can — the entity may be dead by then.
 * Tasks for a level are dropped when it unloads.
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Later {

    private record Task(ServerLevel level, long due, Runnable action) {}

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> ADDED_WHILE_RUNNING = new ArrayList<>();
    private static boolean running;

    private Later() {}

    /** Runs action after 'ticks' ticks (0 or less = right now). */
    public static void run(ServerLevel level, int ticks, Runnable action) {
        if (ticks <= 0) {
            action.run();
            return;
        }
        Task task = new Task(level, level.getGameTime() + ticks, action);
        if (running) ADDED_WHILE_RUNNING.add(task);
        else TASKS.add(task);
    }

    @SubscribeEvent
    public static void onLaterLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        if (TASKS.isEmpty()) return;

        long now = level.getGameTime();
        running = true;
        try {
            Iterator<Task> iterator = TASKS.iterator();
            while (iterator.hasNext()) {
                Task task = iterator.next();
                if (task.level() != level || task.due() > now) continue;
                iterator.remove();
                try {
                    task.action().run();
                } catch (Exception e) {
                    ForgeRealm.LOGGER.error("A delayed task failed", e);
                }
            }
        } finally {
            running = false;
            TASKS.addAll(ADDED_WHILE_RUNNING);
            ADDED_WHILE_RUNNING.clear();
        }
    }

    @SubscribeEvent
    public static void onLaterLevelUnload(LevelEvent.Unload event) {
        TASKS.removeIf(task -> task.level() == event.getLevel());
    }
}
