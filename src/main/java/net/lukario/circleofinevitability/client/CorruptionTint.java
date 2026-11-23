package net.lukario.circleofinevitability.client;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

public final class CorruptionTint {
    private static int color;

    private CorruptionTint() {}

    public static void update(int corruption) {
        if (corruption < SoulCore.FIRST_HARMFUL_CORRUPTION) {
            color = 0;
            return;
        }
        float opacity = Mth.clampedMap(corruption, SoulCore.FIRST_HARMFUL_CORRUPTION,
                SoulCore.MAX_CORRUPTION, 0.08f, 0.35f);
        color = ARGB.color(opacity, 0xB00000);
    }

    static void render(GuiGraphicsExtractor graphics) {
        if (color != 0 && Minecraft.getInstance().player != null) {
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), color);
        }
    }
}
