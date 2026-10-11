package net.lukario.frogerealm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;

/**
 * One short line under the spirituality text above the hotbar: "Sequence N - Pathway - Digestion P%".
 * Drawn every frame by HudLayerHook from ClientPathwayData (synced by PathwaySync). Hidden while there is no Pathway.
 */
public class SequenceHudLine {

    /** Vanilla draws the action bar text (the spirituality line) at 68 px above the bottom; this goes right under it. */
    private static final int Y_FROM_BOTTOM = 68 - 10;
    private static final int COLOR = 0xFFC8B8FF;

    public static void render(GuiGraphicsExtractor guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        if (minecraft.gameMode != null && minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) return;
        String pathway = ClientPathwayData.getPathway();
        if (pathway.equals("none")) return;

        Font font = minecraft.font;
        Component line = Component.translatable("hud.circle_of_inevitability.sequence_line",
                ClientPathwayData.getSequence(), pathway, ClientPathwayData.getDigestion());
        int x = guiGraphics.guiWidth() / 2 - font.width(line) / 2;
        int y = guiGraphics.guiHeight() - Y_FROM_BOTTOM;
        guiGraphics.text(font, line, x, y, COLOR, true);
    }
}
