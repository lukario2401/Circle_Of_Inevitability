package net.lukario.circleofinevitability.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

public final class SpiritualityHud {

    private static final int WIDTH = 182; // matches the hotbar
    private static final int HEIGHT = 6;
    private static final int Y_FROM_BOTTOM = 82; // above the vanilla action bar message
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

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || label == null) return;

        int center = graphics.guiWidth() / 2;
        int x = center - WIDTH / 2;
        int y = graphics.guiHeight() - Y_FROM_BOTTOM;
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, BORDER_COLOR);
        graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, TRACK_COLOR);
        if (filledWidth > 0) {
            graphics.fillGradient(x + 1, y + 1, x + 1 + filledWidth, y + HEIGHT - 1,
                    FILL_TOP_COLOR, FILL_BOTTOM_COLOR);
        }
        graphics.centeredText(minecraft.font, label, center, y - minecraft.font.lineHeight - 2, TEXT_COLOR);
    }
}
