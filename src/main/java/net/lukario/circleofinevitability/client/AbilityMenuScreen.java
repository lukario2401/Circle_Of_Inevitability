package net.lukario.circleofinevitability.client;

import net.lukario.circleofinevitability.network.COpenAbilityMenuPacket;
import net.lukario.circleofinevitability.network.PacketHandler;
import net.lukario.circleofinevitability.network.SAbilityMenuChoicePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Generic icon menu used by every AbilityMenu. You don't need to touch this to make a new menu —
 * define it with AbilityMenu on the server side. Edit here only to change how all menus look.
 */
public class AbilityMenuScreen extends Screen {

    private static final float HOVER_SCALE = 1.2f;
    private static final int LABEL_COLOR = 0xFFD8D8E0;
    private static final int LABEL_HOVER_COLOR = 0xFFFFE08A;
    private static final int TITLE_COLOR = 0xFFFFE08A;
    private static final int DETAIL_COLOR = 0xFFA8A8B8;
    private static final int LOCKED_COLOR = 0xFF6A6A72;
    private static final float LOCKED_SHADE = 0.3f;
    private static final float DETAIL_SCALE = 0.75f;

    private final String menuId;
    private final int iconSize;
    private final List<COpenAbilityMenuPacket.Option> options;
    private boolean answered = false;

    private AbilityMenuScreen(String menuId, String title, int iconSize, List<COpenAbilityMenuPacket.Option> options) {
        super(Component.literal(title));
        this.menuId = menuId;
        this.iconSize = iconSize;
        this.options = options;
    }

    public static void open(String menuId, String title, int iconSize, List<COpenAbilityMenuPacket.Option> options) {
        Minecraft.getInstance().gui.setScreen(new AbilityMenuScreen(menuId, title, iconSize, options));
    }

    @Override
    public boolean isPauseScreen() {
        return false; // the world keeps running while the menu is open
    }

    // ---------- input ----------

    private int hoveredIndex(double mouseX, double mouseY) {
        for (int i = 0; i < options.size(); i++) {
            COpenAbilityMenuPacket.Option option = options.get(i);
            double dx = mouseX - (width / 2.0 + option.x());
            double dy = mouseY - (height / 2.0 + option.y());
            if (Math.abs(dx) <= iconSize / 2.0 && Math.abs(dy) <= iconSize / 2.0) return i;
        }
        return -1;
    }

    private void choose(int index) {
        answered = true;
        PacketHandler.sendToServer(new SAbilityMenuChoicePacket(menuId, index));
        onClose();
    }

    // 26.2: mouseClicked(MouseButtonEvent, doubleClick) and keyPressed(KeyEvent) replace the int/double versions
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int index = hoveredIndex(event.x(), event.y());
            if (index >= 0) {
                if (!options.get(index).locked()) choose(index);
            } else {
                onClose(); // clicked empty space
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int index = event.key() - GLFW.GLFW_KEY_1; // number keys 1..9 pick options
        if (index >= 0 && index < 9 && index < options.size()) {
            if (!options.get(index).locked()) choose(index);
            return true;
        }
        return super.keyPressed(event); // Esc closes
    }

    @Override
    public void removed() {
        // closed without picking (Esc, click outside, death screen...) -> tell the server
        if (!answered) {
            answered = true;
            PacketHandler.sendToServer(new SAbilityMenuChoicePacket(menuId, -1));
        }
        super.removed();
    }

    // ---------- drawing ----------

    // 26.2 draws the background (blur + menu texture) in its own pass before extractRenderState, even when that
    // is overridden. The old render() drew none, so neither does this (subtitles still get drawn, as before).
    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.minecraft.gui.hud.extractDeferredSubtitles();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        // soft dark overlay instead of the default blur, so the world stays visible
        guiGraphics.fillGradient(0, 0, width, height, 0x70000000, 0xA0000000);

        int centerX = width / 2;
        int centerY = height / 2;
        int hovered = hoveredIndex(mouseX, mouseY);

        // title above the highest icon
        if (!title.getString().isEmpty()) {
            int top = 0;
            for (COpenAbilityMenuPacket.Option option : options) top = Math.min(top, option.y());
            guiGraphics.centeredText(font, title, centerX, centerY + top - iconSize / 2 - 22, TITLE_COLOR);
        }

        for (int i = 0; i < options.size(); i++) {
            COpenAbilityMenuPacket.Option option = options.get(i);
            boolean isHovered = i == hovered;
            int iconX = centerX + option.x();
            int iconY = centerY + option.y();

            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(iconX, iconY);
            if (isHovered) guiGraphics.pose().scale(HOVER_SCALE, HOVER_SCALE);

            float shade = option.locked() ? LOCKED_SHADE : isHovered ? 1f : 0.8f;
            int half = iconSize / 2;
            // 26.2: no setColor; the tint is the blit's last argument
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, option.icon(), -half, -half, 0f, 0f, iconSize, iconSize,
                    iconSize, iconSize, iconSize, iconSize, ARGB.colorFromFloat(1f, shade, shade, shade));
            guiGraphics.pose().popMatrix();

            // label + number key under the icon
            String label = (i + 1) + ". " + option.label();
            int labelY = iconY + (int) (half * (isHovered ? HOVER_SCALE : 1f)) + 4;
            int labelColor = option.locked() ? LOCKED_COLOR : isHovered ? LABEL_HOVER_COLOR : LABEL_COLOR;
            guiGraphics.centeredText(font, label, iconX, labelY, labelColor);

            // the small second line: cost, cooldown, what it needs
            if (!option.detail().isEmpty()) {
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate(iconX, labelY + 10);                    // under the 9 px tall label
                guiGraphics.pose().scale(DETAIL_SCALE, DETAIL_SCALE);
                guiGraphics.centeredText(font, option.detail(), 0, 0, option.locked() ? LOCKED_COLOR : DETAIL_COLOR);
                guiGraphics.pose().popMatrix();
            }
        }
        if (hovered >= 0 && !options.get(hovered).description().isEmpty()) {
            guiGraphics.setTooltipForNextFrame(font,
                    font.split(Component.literal(options.get(hovered).description()), Math.min(260, Math.max(80, width - 24))),
                    mouseX, mouseY);
        }
    }
}
