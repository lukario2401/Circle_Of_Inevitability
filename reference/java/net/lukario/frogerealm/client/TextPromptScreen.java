package net.lukario.frogerealm.client;

import net.lukario.frogerealm.menu.AbilityTextPrompt;
import net.lukario.frogerealm.network.PacketHandler;
import net.lukario.frogerealm.network.STextPromptAnswerPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Generic text prompt window used by every AbilityTextPrompt: a medium-sized panel
 * with a title, a text box in the middle, Enter to submit, Esc to close.
 * Edit here only to change how all prompts look; define prompts with AbilityTextPrompt.
 */
public class TextPromptScreen extends Screen {

    private static final int PANEL_WIDTH = 240;
    private static final int PANEL_HEIGHT = 96;
    private static final int BORDER_COLOR = 0xFFDEB85C;
    private static final int BORDER_DARK_COLOR = 0xFF7A5E26;
    private static final int PANEL_COLOR = 0xF0161422;
    private static final int TITLE_COLOR = 0xFFFFE08A;
    private static final int FOOTER_COLOR = 0xFF8C8A9A;

    private final String promptId;
    private final String hint;
    private EditBox textBox;
    private boolean answered = false;

    private TextPromptScreen(String promptId, String title, String hint) {
        super(Component.literal(title));
        this.promptId = promptId;
        this.hint = hint;
    }

    public static void open(String promptId, String title, String hint) {
        Minecraft.getInstance().setScreen(new TextPromptScreen(promptId, title, hint));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        String previous = textBox != null ? textBox.getValue() : ""; // keep text when the window is resized

        int boxWidth = PANEL_WIDTH - 40;
        textBox = new EditBox(font, (width - boxWidth) / 2, height / 2 - 10, boxWidth, 20, Component.literal("Words"));
        textBox.setMaxLength(AbilityTextPrompt.MAX_LENGTH);
        textBox.setHint(Component.literal(hint));
        textBox.setValue(previous);
        addRenderableWidget(textBox);
        setFocused(textBox);
    }

    private void submit() {
        answered = true;
        PacketHandler.sendToServer(new STextPromptAnswerPacket(promptId, textBox.getValue()));
        onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers); // Esc closes, everything else goes to the text box
    }

    @Override
    public void removed() {
        if (!answered) { // closed with Esc etc.
            answered = true;
            PacketHandler.sendToServer(new STextPromptAnswerPacket(promptId, ""));
        }
        super.removed();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // dim the world a little, no blur
        guiGraphics.fillGradient(0, 0, width, height, 0x50000000, 0x80000000);

        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        int right = left + PANEL_WIDTH;
        int bottom = top + PANEL_HEIGHT;

        // panel: gold outer border, dark gold inner line, dark background
        guiGraphics.fill(left - 2, top - 2, right + 2, bottom + 2, BORDER_COLOR);
        guiGraphics.fill(left - 1, top - 1, right + 1, bottom + 1, BORDER_DARK_COLOR);
        guiGraphics.fill(left, top, right, bottom, PANEL_COLOR);

        guiGraphics.drawCenteredString(font, title, width / 2, top + 10, TITLE_COLOR);
        textBox.render(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(font, "Enter to speak  •  Esc to close", width / 2, bottom - 16, FOOTER_COLOR);
    }
}
