package net.lukario.circleofinevitability.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public final class SpiritualityHud {

    private static final int WIDTH = ContextualBar.WIDTH / 2;
    private static final int HEIGHT = 6;
    private static final int LABEL_GAP = 2;
    private static final int STATUS_ROW_FROM_BOTTOM = 39;
    private static final int STATUS_ROW_SPACING = 10;
    private static final int STATUS_GAP = 2;
    private static final int ITEM_NAME_FROM_BOTTOM = 59;
    private static final int CREATIVE_ITEM_NAME_DROP = 14;
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

    static boolean isVisible(Minecraft minecraft) {
        return label != null && minecraft.player != null && minecraft.gameMode != null && !minecraft.player.isSpectator();
    }

    static int rightStatusTopOffset(Minecraft minecraft) {
        Hud hud = minecraft.gui.hud;
        int vehicleHearts = hud.getVehicleMaxHearts(hud.getPlayerVehicleWithHealth());
        int rows = Math.max(1, hud.getVisibleVehicleHeartRows(vehicleHearts));
        Player player = hud.getCameraPlayer();
        if (minecraft.gameMode != null && minecraft.gameMode.canHurtPlayer() && player != null
                && (player.isEyeInFluid(FluidTags.WATER) || player.getAirSupply() < player.getMaxAirSupply())) {
            rows++;
        }
        return STATUS_ROW_FROM_BOTTOM + (rows - 1) * STATUS_ROW_SPACING;
    }

    private static int barTopOffset(Minecraft minecraft) {
        return rightStatusTopOffset(minecraft) + STATUS_GAP + HEIGHT;
    }

    static int messageOffset(Minecraft minecraft, boolean selectedItem) {
        if (!isVisible(minecraft)) return 0;

        // Make room for the item name above the spirituality label, and keep the action bar above both.
        int labelTop = barTopOffset(minecraft) + LABEL_GAP + minecraft.font.lineHeight;
        int messageOffset = labelTop + minecraft.font.lineHeight + LABEL_GAP - ITEM_NAME_FROM_BOTTOM;
        if (selectedItem && !minecraft.gameMode.canHurtPlayer()) messageOffset += CREATIVE_ITEM_NAME_DROP;
        return messageOffset;
    }

    public static void render(GuiGraphicsExtractor graphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isVisible(minecraft)) return;

        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() - barTopOffset(minecraft);
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, BORDER_COLOR);
        graphics.fill(x + 1, y + 1, x + WIDTH - 1, y + HEIGHT - 1, TRACK_COLOR);
        if (filledWidth > 0) {
            graphics.fillGradient(x + 1, y + 1, x + 1 + filledWidth, y + HEIGHT - 1,
                    FILL_TOP_COLOR, FILL_BOTTOM_COLOR);
        }
        graphics.centeredText(minecraft.font, label, x + WIDTH / 2, y - minecraft.font.lineHeight - LABEL_GAP, TEXT_COLOR);
    }
}
