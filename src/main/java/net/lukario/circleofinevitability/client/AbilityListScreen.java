package net.lukario.circleofinevitability.client;

import net.lukario.circleofinevitability.network.COpenAbilityListPacket;
import net.lukario.circleofinevitability.network.PacketHandler;
import net.lukario.circleofinevitability.network.SAbilityListChoicePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generic scrollable pick list used by every menu/AbilityList. Define lists on the server; edit here only to change
 * how all of them look. One pick: a click sends it. Several: clicks toggle rows, the button or Enter sends them.
 */
public final class AbilityListScreen extends Screen {
    private static final int ROW = 26;
    private static final int HEADER = 30;
    private static final int FOOTER = 34;
    private static final int GOLD = 0xFFD1A94B;
    private static final int TEXT = 0xFFF4EBD0;
    private static final int DIM = 0xFFB4A7BD;
    private static final int LOCKED = 0xFF6E6676;

    private final COpenAbilityListPacket list;
    private final List<Integer> picks = new ArrayList<>();
    private final Map<String, Optional<Identifier>> icons = new HashMap<>();
    private int scroll;
    private boolean answered;

    private AbilityListScreen(COpenAbilityListPacket list) {
        super(Component.literal(list.title()));
        this.list = list;
    }

    public static void open(COpenAbilityListPacket list) {
        Minecraft.getInstance().gui.setScreen(new AbilityListScreen(list));
    }

    @Override public boolean isPauseScreen() { return false; }

    private boolean multi() { return list.maxPicks() > 1; }
    private int panelWidth() { return Math.min(380, width - 24); }
    private int left() { return (width - panelWidth()) / 2; }
    private int visible() { return Math.max(1, Math.min(list.rows().size(), (height - 40 - HEADER - FOOTER) / ROW)); }
    private int top() { return Math.max(12, (height - (HEADER + visible() * ROW + FOOTER)) / 2); }
    private int rowsTop() { return top() + HEADER; }
    private int bottom() { return rowsTop() + visible() * ROW; }
    private void clamp() { scroll = Math.clamp(scroll, 0, Math.max(0, list.rows().size() - visible())); }

    /** The confirm button: x0, y0, x1, y1. */
    private int[] button() {
        int w = Math.max(70, font.width(buttonLabel()) + 16);
        int x1 = left() + panelWidth() - 8, y0 = bottom() + 8;
        return new int[]{x1 - w, y0, x1, y0 + 18};
    }

    private String buttonLabel() {
        return list.confirmLabel() + " (" + picks.size() + "/" + list.maxPicks() + ")";
    }

    private int rowAt(double x, double y) {
        if (x < left() + 6 || x >= left() + panelWidth() - 6 || y < rowsTop() || y >= bottom()) return -1;
        int index = scroll + (int) ((y - rowsTop()) / ROW);
        return index < list.rows().size() ? index : -1;
    }

    private void send(List<Integer> chosen) {
        answered = true;
        PacketHandler.sendToServer(new SAbilityListChoicePacket(list.listId(), chosen));
        onClose();
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        scroll -= (int) Math.signum(dy) * 2;
        clamp();
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubleClick);
        int[] b = button();
        if (multi() && event.x() >= b[0] && event.x() < b[2] && event.y() >= b[1] && event.y() < b[3]) {
            if (!picks.isEmpty()) send(List.copyOf(picks));
            return true;
        }
        int index = rowAt(event.x(), event.y());
        if (index < 0 || list.rows().get(index).locked()) return true;
        if (!multi()) {
            send(List.of(index));
        } else if (picks.contains(index)) {
            picks.remove(Integer.valueOf(index));
        } else if (picks.size() < list.maxPicks()) {
            picks.add(index);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (multi() && (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) && !picks.isEmpty()) {
            send(List.copyOf(picks));
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_DOWN || event.key() == GLFW.GLFW_KEY_PAGE_DOWN) { scroll += event.key() == GLFW.GLFW_KEY_DOWN ? 1 : visible(); clamp(); return true; }
        if (event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_PAGE_UP) { scroll -= event.key() == GLFW.GLFW_KEY_UP ? 1 : visible(); clamp(); return true; }
        return super.keyPressed(event);                                       // Esc closes
    }

    @Override
    public void removed() {
        if (!answered) {
            answered = true;
            PacketHandler.sendToServer(new SAbilityListChoicePacket(list.listId(), List.of()));
        }
        super.removed();
    }

    private Optional<Identifier> icon(String texture) {
        if (texture.isEmpty()) return Optional.empty();
        return icons.computeIfAbsent(texture, t -> {
            Identifier id = Identifier.tryParse(t);
            return id != null && minecraft.getResourceManager().getResource(id).isPresent() ? Optional.of(id) : Optional.empty();
        });
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        this.minecraft.gui.hud.extractDeferredSubtitles();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float tick) {
        clamp();
        int x = left(), w = panelWidth(), y = top(), rowsTop = rowsTop(), bottom = bottom();
        g.fillGradient(0, 0, width, height, 0x88000000, 0xAA000000);
        g.fill(x, y, x + w, bottom + FOOTER, 0xF21C1820);
        g.fill(x, y, x + w, y + 2, 0xFF8A1C2C);
        g.text(font, font.plainSubstrByWidth(title.getString(), w - 20), x + 10, y + 10, TEXT);
        int hovered = rowAt(mx, my);
        for (int n = 0; n < visible() && scroll + n < list.rows().size(); n++) {
            int index = scroll + n;
            COpenAbilityListPacket.Row row = list.rows().get(index);
            int yy = rowsTop + n * ROW;
            boolean picked = picks.contains(index);
            int bg = picked ? 0xFF5A2430 : row.marked() ? 0xFF4C4028 : index == hovered && !row.locked() ? 0xFF38313F : 0xFF25212B;
            g.fill(x + 6, yy, x + w - 6, yy + ROW - 1, bg);
            if (picked) g.fill(x + 6, yy, x + 8, yy + ROW - 1, GOLD);
            int tx = x + 12;
            Optional<Identifier> icon = icon(row.icon());
            if (icon.isPresent()) {
                float shade = row.locked() ? .35f : 1f;
                g.blit(RenderPipelines.GUI_TEXTURED, icon.get(), tx, yy + 4, 0f, 0f, 18, 18, 18, 18, 18, 18,
                        ARGB.colorFromFloat(1f, shade, shade, shade));
                tx += 24;
            }
            int right = x + w - (multi() ? 28 : 12);
            g.text(font, font.plainSubstrByWidth(row.name(), Math.max(20, right - tx)), tx, yy + 4, row.locked() ? LOCKED : TEXT);
            if (!row.detail().isEmpty())
                g.text(font, font.plainSubstrByWidth(row.detail(), Math.max(20, right - tx)), tx, yy + 14,
                        row.locked() ? LOCKED : row.marked() ? GOLD : DIM);
            if (multi() && !row.locked()) g.text(font, picked ? "☑" : "☐", x + w - 22, yy + 9, picked ? GOLD : DIM);
            if (index == hovered) {
                String tip = row.name() + (row.detail().isEmpty() ? "" : "\n" + row.detail())
                        + (row.description().isEmpty() ? "" : "\n" + row.description());
                g.setTooltipForNextFrame(font, font.split(Component.literal(tip), Math.min(280, width - 24)), mx, my);
            }
        }
        if (list.rows().isEmpty()) g.centeredText(font, "Nothing here.", width / 2, rowsTop + 8, DIM);
        if (list.rows().size() > visible()) {
            int track = visible() * ROW, thumb = Math.max(8, track * visible() / list.rows().size());
            int sy = rowsTop + (track - thumb) * scroll / Math.max(1, list.rows().size() - visible());
            g.fill(x + w - 4, sy, x + w - 1, sy + thumb, GOLD);
        }
        String hint = !list.hint().isEmpty() ? list.hint()
                : multi() ? "Click to pick up to " + list.maxPicks() + "   Scroll: browse   Esc: none"
                : "Click to pick   Scroll: browse   Esc: none";
        int hintWidth = multi() ? button()[0] - x - 16 : w - 20;
        g.text(font, font.plainSubstrByWidth(hint, Math.max(20, hintWidth)), x + 10, bottom + 13, DIM);
        if (multi()) {
            int[] b = button();
            boolean ready = !picks.isEmpty();
            boolean hover = mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3];
            g.fill(b[0], b[1], b[2], b[3], ready ? (hover ? 0xFFA0283C : 0xFF7E1F30) : 0xFF3A3340);
            g.centeredText(font, buttonLabel(), (b[0] + b[2]) / 2, b[1] + 5, ready ? TEXT : LOCKED);
        }
    }
}
