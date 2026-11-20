package net.lukario.circleofinevitability.client;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.menu.AbilityGuide;
import net.lukario.circleofinevitability.menu.AbilityGuide.Card;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import org.joml.Vector2i;

import java.util.List;

public final class AbilityGuideScreen extends Screen {
    private static final int MINT = 0xFF91EEDC;
    private static final int GOLD = 0xFFFFCB85;
    private static final int WHITE = 0xFFF6F0FB;
    private static final int MUTED = 0xFFB9AEC6;
    private static final int CARD_HEIGHT = 59;
    private final AbilityGuide guide;
    private final int sequence;
    private final List<String> sections;
    private int section;
    private int page;
    private int pageCount;
    private int left;
    private int span;

    private AbilityGuideScreen(AbilityGuide guide, int sequence) {
        super(Component.literal(guide.title()));
        this.guide = guide;
        this.sequence = sequence;
        this.sections = guide.cards().stream().map(Card::section).distinct().toList();
    }

    public static void open(String pathway, int sequence) {
        var factory = AbilityGuide.PATHWAYS.get(pathway);
        if (factory != null) Minecraft.getInstance().gui.setScreen(new AbilityGuideScreen(factory.get(), sequence));
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        span = Math.min(width - 20, 800);
        left = (width - span) / 2;
        int tabWidth = Math.min(92, span / sections.size());
        for (int i = 0; i < sections.size(); i++) {
            int index = i;
            addRenderableWidget(new Tab(left + i * tabWidth, 35, tabWidth - 2, sections.get(i), section == i, () -> {
                section = index;
                page = 0;
                rebuildWidgets();
            }));
        }

        List<Card> cards = guide.cards().stream().filter(c -> c.section().equals(sections.get(section))).toList();
        int columns = Math.min(3, Math.max(1, span / 202));
        int rows = Math.max(1, (height - 82) / (CARD_HEIGHT + 3));
        int capacity = rows * columns;
        pageCount = (cards.size() + capacity - 1) / capacity;
        page = Math.clamp(page, 0, pageCount - 1);
        int cardWidth = (span + 4) / columns - 4;
        for (int i = 0; i < capacity && page * capacity + i < cards.size(); i++) {
            addRenderableWidget(new AbilityCard(left + i % columns * (cardWidth + 4),
                    59 + i / columns * (CARD_HEIGHT + 3), cardWidth, cards.get(page * capacity + i)));
        }
        if (pageCount > 1) {
            var prev = addRenderableWidget(new Tab(left, height - 21, 24, "‹", false, () -> changePage(-1)));
            var next = addRenderableWidget(new Tab(left + 74, height - 21, 24, "›", false, () -> changePage(1)));
            prev.active = page > 0;
            next.active = page + 1 < pageCount;
        }
        addRenderableWidget(new Tab(left + span - 49, height - 21, 49, "Close", false, this::onClose));
    }

    private void changePage(int delta) {
        int next = Math.clamp(page + delta, 0, pageCount - 1);
        if (page != next) {
            page = next;
            rebuildWidgets();
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (vertical == 0) return super.mouseScrolled(x, y, horizontal, vertical);
        changePage(vertical > 0 ? -1 : 1);
        return true;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float tick) {
        g.fillGradient(0, 0, width, height, 0xE7241A32, 0xEF0C111B);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float tick) {
        g.fill(left, 9, left + 3, 28, GOLD);
        g.pose().pushMatrix();
        g.pose().translate(left + 8, 10);
        g.pose().scale(1.8f, 1.8f);
        g.text(font, title, 0, 0, WHITE, false);
        g.pose().popMatrix();
        String rank = "Sequence " + sequence;
        g.text(font, rank, left + span - font.width(rank), 15, GOLD, false);
        if (pageCount > 1) g.centeredText(font, (page + 1) + " / " + pageCount, left + 49, height - 15, MUTED);
        g.centeredText(font, "Hover for details", width / 2, height - 15, MUTED);
        super.extractRenderState(g, mouseX, mouseY, tick);
    }

    private String key(int slot) {
        return Keybindings.INSTANCE.abilityKeys.get(slot - 1).getTranslatedKeyMessage().getString();
    }

    private String binding(Card c) {
        return switch (c.gesture()) {
            case PRESS -> key(c.first());
            case HOLD_RELEASE -> "Hold / release " + key(c.first());
            case ATTACK -> minecraft.options.keyAttack.getTranslatedKeyMessage().getString();
            case SNEAK -> minecraft.options.keyShift.getTranslatedKeyMessage().getString() + " + " + key(c.first());
            case WHEEL -> key(c.first()) + " wheel";
            case CHORD -> key(c.first()) + " + " + key(c.second());
            case HOLD_CHORD -> "Hold " + key(c.first()) + " + " + key(c.second());
            case ORDERED -> key(c.first()) + " then " + key(c.second());
            case PASSIVE -> "Passive";
        };
    }

    private final class AbilityCard extends AbstractWidget {
        private final Card card;
        private final String binding;
        private final Identifier icon;
        private final boolean locked;
        private final List<FormattedCharSequence> detailLines;

        AbilityCard(int x, int y, int w, Card card) {
            super(x, y, w, CARD_HEIGHT, Component.literal(card.name()));
            this.card = card;
            icon = Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "textures/gui/" + card.icon() + ".png");
            binding = binding(card);
            locked = sequence > card.sequence();
            String rank = (locked ? "Unlocks at sequence " : "Sequence ") + card.sequence();
            String stats = card.gesture() == AbilityGuide.Gesture.PASSIVE ? "" : "\nBase spirituality  " + (int) card.baseCost();
            if (card.cooldown() > 0) stats += "    Cooldown  " + AbilityGuide.seconds(card.cooldown()) + "s";
            var text = Component.literal(card.name() + "  ·  " + binding).withStyle(ChatFormatting.AQUA)
                    .append(Component.literal("\n" + rank + stats).withStyle(ChatFormatting.GOLD))
                    .append(Component.literal("\n\n" + card.details()).withStyle(ChatFormatting.WHITE));
            setTooltip(Tooltip.create(text));
            detailLines = font.split(text, Math.min(255, AbilityGuideScreen.this.width - 24));
        }

        @Override
        protected void extractTooltipForNextRenderPass(GuiGraphicsExtractor g, int mouseX, int mouseY) {
            boolean focused = isFocused() && minecraft.getLastInputType().isKeyboard();
            if (!isHovered() && !focused) return;
            int x = focused ? getX() + 8 : mouseX;
            int y = focused ? getY() + 8 : mouseY;
            g.setTooltipForNextFrame(font, detailLines, (screenW, screenH, mx, my, w, h) -> {
                int left = mx + 12 + w <= screenW - 6 ? mx + 12 : mx - w - 8;
                return new Vector2i(Math.clamp(left, 6, Math.max(6, screenW - w - 6)),
                        Math.clamp(my + 10, 6, Math.max(6, screenH - h - 6)));
            }, x, y, focused);
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float tick) {
            int x = getX(), y = getY();
            g.fill(x, y, getRight(), getBottom(), isHoveredOrFocused() ? 0xFA373343 : 0xDC241F2F);
            g.fill(x, y, x + 2, getBottom(), isHoveredOrFocused() ? GOLD : locked ? 0xFFA095AB : MINT);
            g.fill(x + 7, y + 7, x + 35, y + 35, 0xFF16131D);
            g.blit(RenderPipelines.GUI_TEXTURED, icon, x + 8, y + 8, 0, 0, 26, 26, 26, 26);
            int textX = x + 41;
            g.text(font, card.name(), textX, y + 7, locked ? MUTED : MINT, false);
            String unlock = locked ? "  ·  Locked " + card.sequence() : "";
            g.text(font, font.plainSubstrByWidth(binding + unlock, width - 47), textX, y + 20, GOLD, false);
            int lineY = y + 34;
            for (FormattedCharSequence line : font.split(Component.literal(card.summary()), width - 16)) {
                if (lineY + font.lineHeight > getBottom()) break;
                g.text(font, line, x + 8, lineY, WHITE, false);
                lineY += font.lineHeight;
            }
        }

        @Override protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, Component.literal(card.name() + ". " + binding + ". " + card.summary()));
        }
    }

    private final class Tab extends AbstractButton {
        private final boolean selected;
        private final Runnable action;

        Tab(int x, int y, int w, String text, boolean selected, Runnable action) {
            super(x, y, w, 18, Component.literal(text));
            this.selected = selected;
            this.action = action;
        }

        @Override public void onPress(InputWithModifiers input) { action.run(); }
        @Override protected void updateWidgetNarration(NarrationElementOutput output) { defaultButtonNarrationText(output); }
        @Override protected void extractContents(GuiGraphicsExtractor g, int mx, int my, float tick) {
            g.fill(getX(), getY(), getRight(), getBottom(), selected ? 0xFF62425D : isHoveredOrFocused() ? 0xFF413447 : 0xFF282532);
            if (selected || isFocused()) g.fill(getX(), getBottom() - 2, getRight(), getBottom(), GOLD);
            g.centeredText(font, getMessage(), getX() + width / 2, getY() + 5, active ? WHITE : MUTED);
        }
    }
}
