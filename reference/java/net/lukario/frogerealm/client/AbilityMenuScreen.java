package net.lukario.frogerealm.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.lukario.frogerealm.network.COpenAbilityMenuPacket;
import net.lukario.frogerealm.network.PacketHandler;
import net.lukario.frogerealm.network.SAbilityMenuChoicePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
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
        Minecraft.getInstance().setScreen(new AbilityMenuScreen(menuId, title, iconSize, options));
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int index = hoveredIndex(mouseX, mouseY);
            if (index >= 0) {
                choose(index);
            } else {
                onClose(); // clicked empty space
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int index = keyCode - GLFW.GLFW_KEY_1; // number keys 1..9 pick options
        if (index >= 0 && index < 9 && index < options.size()) {
            choose(index);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers); // Esc closes
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

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // soft dark overlay instead of the default blur, so the world stays visible
        guiGraphics.fillGradient(0, 0, width, height, 0x70000000, 0xA0000000);

        int centerX = width / 2;
        int centerY = height / 2;
        int hovered = hoveredIndex(mouseX, mouseY);

        // title above the highest icon
        if (!title.getString().isEmpty()) {
            int top = 0;
            for (COpenAbilityMenuPacket.Option option : options) top = Math.min(top, option.y());
            guiGraphics.drawCenteredString(font, title, centerX, centerY + top - iconSize / 2 - 22, TITLE_COLOR);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int i = 0; i < options.size(); i++) {
            COpenAbilityMenuPacket.Option option = options.get(i);
            boolean isHovered = i == hovered;
            int iconX = centerX + option.x();
            int iconY = centerY + option.y();

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(iconX, iconY, 0);
            if (isHovered) guiGraphics.pose().scale(HOVER_SCALE, HOVER_SCALE, 1f);

            float shade = isHovered ? 1f : 0.8f;
            guiGraphics.setColor(shade, shade, shade, 1f);
            int half = iconSize / 2;
            guiGraphics.blit(option.icon(), -half, -half, iconSize, iconSize,
                    0f, 0f, iconSize, iconSize, iconSize, iconSize);
            guiGraphics.setColor(1f, 1f, 1f, 1f);
            guiGraphics.pose().popPose();

            // label + number key under the icon
            String label = (i + 1) + ". " + option.label();
            int labelY = iconY + (int) (half * (isHovered ? HOVER_SCALE : 1f)) + 4;
            guiGraphics.drawCenteredString(font, label, iconX, labelY, isHovered ? LABEL_HOVER_COLOR : LABEL_COLOR);
        }
        RenderSystem.disableBlend();
    }
}
