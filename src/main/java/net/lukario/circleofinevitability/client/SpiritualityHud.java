package net.lukario.circleofinevitability.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.util.Mth;

public final class SpiritualityHud {

    private static final int WIDTH = ContextualBar.WIDTH / 2;
    private static final int HEIGHT = 6;
    private static final int LABEL_GAP = 2;
    // The vanilla slot highlight starts 23 GUI pixels above the bottom.
    private static final int BOTTOM_MARGIN = 24;
    private static final int BORDER_COLOR = 0xFF1C1527;
    private static final int TRACK_COLOR = 0xFF352B49;
    private static final int FILL_TOP_COLOR = 0xFFA999FF;
    private static final int FILL_BOTTOM_COLOR = 0xFF6D51D6;
    private static final int TEXT_COLOR = 0xFFECE5FF;

    private static String label;
    private static int filledWidth;

    private SpiritualityHud() {}

    public static void update(float current, float maximum) {
        label = (int) current + "/" + (int) maximum;
        filledWidth = Math.round(Mth.clamp(current / maximum, 0f, 1f) * (WIDTH - 2));
    }

    public static void clear() {
        label = null;
    }

    private static boolean isVisible(Minecraft minecraft) {
        return label != null && minecraft.player != null && !minecraft.player.isSpectator();
    }

    static int hotbarHudOffset() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isVisible(minecraft)) return 0;

        // Leave space below the vanilla contextual bar for the meter and its label.
        return BOTTOM_MARGIN + HEIGHT + LABEL_GAP + minecraft.font.lineHeight
                + LABEL_GAP - ContextualBar.MARGIN_BOTTOM;
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isVisible(minecraft)) return;

        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() - BOTTOM_MARGIN - HEIGHT;
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, BORDER_COLOR);
        graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, TRACK_COLOR);
        if (filledWidth > 0) {
            graphics.fillGradient(x + 1, y + 1, x + 1 + filledWidth, y + HEIGHT - 1,
                    FILL_TOP_COLOR, FILL_BOTTOM_COLOR);
        }
        graphics.centeredText(minecraft.font, label, x + WIDTH / 2, y - minecraft.font.lineHeight - LABEL_GAP, TEXT_COLOR);
    }
}
