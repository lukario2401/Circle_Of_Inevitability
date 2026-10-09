package net.lukario.frogerealm.client;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client-side renderer for the small timers next to the hotbar ("Revive 27s").
 * The server shows and hides them with combat/HudTimer. They count down here by themselves.
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class HudTimerOverlay {

    /** The hotbar is 182 wide; the timers start this far to the right of its edge (past the attack indicator). */
    private static final int GAP_FROM_HOTBAR = 30;
    private static final int LINE_HEIGHT = 10;
    /** One timer is two lines: its word, and the time under it. */
    private static final int TIMER_HEIGHT = LINE_HEIGHT * 2 + 2;
    /** The last seconds are shown in this color. */
    private static final int LAST_TICKS = 100;
    private static final int LAST_COLOR = 0xFF5555;
    private static final int LABEL_COLOR = 0xD0D0D0;

    private static final class Timer {
        String label;
        int color;
        int ticksLeft;
    }

    /** In the order they were shown: the oldest is the lowest. */
    private static final Map<String, Timer> TIMERS = new LinkedHashMap<>();

    /** ticks 0 (or less) = take it away. */
    public static void show(String name, String label, int ticks, int color) {
        if (ticks <= 0) {
            TIMERS.remove(name);
            return;
        }
        Timer timer = TIMERS.get(name);
        if (timer == null) {
            timer = new Timer();
            TIMERS.put(name, timer);
        }
        timer.label = label;
        timer.color = color;
        timer.ticksLeft = ticks;
    }

    public static void clear() {
        TIMERS.clear();
    }

    @SubscribeEvent
    public static void onHudTimerClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (TIMERS.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) { // left the world
            clear();
            return;
        }
        if (minecraft.isPaused()) return;

        Iterator<Timer> iterator = TIMERS.values().iterator();
        while (iterator.hasNext()) {
            Timer timer = iterator.next();
            timer.ticksLeft--;
            if (timer.ticksLeft <= 0) iterator.remove();
        }
    }

    // Called every frame by HudLayerHook.
    public static void render(GuiGraphics guiGraphics) {
        if (TIMERS.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        if (minecraft.gameMode != null && minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) return;

        Font font = minecraft.font;
        int x = guiGraphics.guiWidth() / 2 + 91 + GAP_FROM_HOTBAR;
        int y = guiGraphics.guiHeight() - TIMER_HEIGHT;      // the lowest timer stands beside the hotbar

        for (Timer timer : TIMERS.values()) {
            int color = timer.ticksLeft <= LAST_TICKS ? LAST_COLOR : timer.color;
            guiGraphics.drawString(font, timer.label, x, y, 0xFF000000 | LABEL_COLOR, true);
            guiGraphics.drawString(font, time(timer.ticksLeft), x, y + LINE_HEIGHT, 0xFF000000 | (color & 0xFFFFFF), true);
            y -= TIMER_HEIGHT + 2;                           // the next one above it
        }
    }

    /** "27s" under a minute, "1:05" from a minute on. A started second counts: 30 seconds begin at "30s", not "29s". */
    private static String time(int ticks) {
        int seconds = (ticks + 19) / 20;
        if (seconds < 60) return seconds + "s";
        int rest = seconds % 60;
        return seconds / 60 + ":" + (rest < 10 ? "0" : "") + rest;
    }
}
