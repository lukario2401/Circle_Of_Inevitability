package net.lukario.circleofinevitability.client;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client-side renderer for the big countdown number in the middle of the screen.
 * The server triggers it with CShowHudOverlayPacket. (Pictures go through ScreenImages instead.)
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class AbilityHudOverlay {

    private static final float NUMBER_SCALE = 5.0f;     // 1.0 = normal chat text size
    private static final float NUMBER_POP_SCALE = 1.6f; // number starts this much bigger and shrinks in
    private static final float NUMBER_POP_TICKS = 5f;
    private static final float NUMBER_FADE_TICKS = 5f;

    // What is currently shown
    private static boolean showing = false;
    private static int number;
    private static int color;
    private static int totalTicks;
    private static int ticksLeft;

    public static void showNumber(int newNumber, int newColor, int durationTicks) {
        showing = true;
        number = newNumber;
        color = newColor;
        totalTicks = Math.max(1, durationTicks);
        ticksLeft = totalTicks;
    }

    public static void clear() {
        showing = false;
        ticksLeft = 0;
    }

    @SubscribeEvent
    public static void onAbilityHudClientTick(TickEvent.ClientTickEvent.Post event) {
        if (!showing) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) { // left the world
            clear();
            return;
        }
        if (minecraft.isPaused()) return;

        ticksLeft--;
        if (ticksLeft <= 0) clear();
    }

    // Called every frame by HudLayerHook.
    public static void render(GuiGraphicsExtractor guiGraphics) {
        if (!showing) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;

        float partialTick = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float age = (totalTicks - ticksLeft) + partialTick; // ticks since it appeared
        float remaining = ticksLeft - partialTick;          // ticks until it disappears

        renderNumber(guiGraphics, minecraft.font, age, remaining);
    }

    private static void renderNumber(GuiGraphicsExtractor guiGraphics, Font font, float age, float remaining) {
        float alpha = Mth.clamp(remaining / NUMBER_FADE_TICKS, 0f, 1f);
        int a = (int) (alpha * 255);
        if (a < 8) return; // very low alpha is drawn fully opaque by Minecraft's font renderer

        float pop = easeOut(Mth.clamp(age / NUMBER_POP_TICKS, 0f, 1f));
        float scale = NUMBER_SCALE * Mth.lerp(pop, NUMBER_POP_SCALE, 1f);

        String text = String.valueOf(number);
        int argb = (a << 24) | (color & 0xFFFFFF);

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(guiGraphics.guiWidth() / 2f, guiGraphics.guiHeight() / 2f);
        guiGraphics.pose().scale(scale, scale);
        // digits are 7px tall, so -3.5 centers them vertically
        // (26.2: text() only takes int positions, so the half-pixel offset goes into the pose instead)
        guiGraphics.pose().translate(-font.width(text) / 2f, -3.5f);
        guiGraphics.text(font, text, 0, 0, argb, true);
        guiGraphics.pose().popMatrix();
    }

    private static float easeOut(float t) {
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }
}
