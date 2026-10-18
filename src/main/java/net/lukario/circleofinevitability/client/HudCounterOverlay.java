package net.lukario.circleofinevitability.client;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.screen.ScreenImages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Client-side renderer for the counters to the left of the hotbar ("[sigil] Order 4/9").
 * The server shows, updates and hides them with combat/HudCounter. Drawn every frame by HudLayerHook.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class HudCounterOverlay {

    /** The hotbar is 182 wide; a counter ends this far to the left of its edge (past the off-hand slot). */
    private static final int GAP_FROM_HOTBAR = 32;
    private static final int ICON = 16;
    private static final int LINE = ICON + 2;
    private static final int LABEL_COLOR = 0xD0D0D0;

    private record Counter(Identifier image, String label, String number, int color) {}

    /** In the order they were shown: the oldest is the lowest. */
    private static final Map<String, Counter> COUNTERS = new LinkedHashMap<>();

    public static void show(String name, String image, String label, int value, int max, int color) {
        String number = max > 0 ? value + "/" + max : String.valueOf(value);
        COUNTERS.put(name, new Counter(image.isEmpty() ? null : ScreenImages.texture(image), label, number, color));
    }

    public static void hide(String name) {
        COUNTERS.remove(name);
    }

    public static void clear() {
        COUNTERS.clear();
    }

    @SubscribeEvent
    public static void onHudCounterClientTick(TickEvent.ClientTickEvent.Post event) {
        if (!COUNTERS.isEmpty() && Minecraft.getInstance().player == null) clear();     // left the world
    }

    // Called every frame by HudLayerHook.
    public static void render(GuiGraphicsExtractor guiGraphics) {
        if (COUNTERS.isEmpty()) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        Font font = minecraft.font;
        int right = guiGraphics.guiWidth() / 2 - 91 - GAP_FROM_HOTBAR;   // everything lines up on its right edge
        int y = guiGraphics.guiHeight() - LINE;
        for (Counter counter : COUNTERS.values()) {
            int numberWidth = font.width(counter.number());
            int labelWidth = font.width(counter.label());
            int textY = y + (ICON - 9) / 2 + 1;
            int numberX = right - numberWidth;
            int labelX = numberX - 4 - labelWidth;
            guiGraphics.text(font, counter.number(), numberX, textY, 0xFF000000 | (counter.color() & 0xFFFFFF), true);
            guiGraphics.text(font, counter.label(), labelX, textY, 0xFF000000 | LABEL_COLOR, true);
            if (counter.image() != null) {
                int iconX = labelX - 3 - ICON;
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, counter.image(), iconX, y, 0f, 0f,
                        ICON, ICON, ICON, ICON, ICON, ICON, 0xFFFFFFFF);
            }
            y -= LINE + 2;                                                // the next one above it
        }
    }
}
