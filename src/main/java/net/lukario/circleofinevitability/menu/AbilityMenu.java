package net.lukario.circleofinevitability.menu;

import net.lukario.circleofinevitability.network.COpenAbilityMenuPacket;
import net.lukario.circleofinevitability.network.PacketHandler;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.screen.ScreenImages;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A pop-up menu of clickable icons. Everything about a menu is defined here on the server —
 * the client screen is generic and builds itself from what the server sends.
 *
 * Making a new menu:
 *
 *   private static final AbilityMenu MY_MENU = AbilityMenu.create("my_menu")
 *           .title("Pick one")
 *           .option("icons/fire",  "Fire",  -40, 0, (player, level) -> { ...what happens... })
 *           .option("icons/water", "Water",  40, 0, (player, level) -> { ... });
 *
 *   // then inside an ability:
 *   MY_MENU.open(player);
 *
 * - icon:  PNG in assets/circle_of_inevitability/textures/gui/ without ".png" (same as ScreenImages)
 * - x, y:  where the icon's center goes, in GUI pixels from the middle of the screen (+x right, +y down)
 * - action runs on the server when the player clicks that icon (or presses its number key 1-9)
 * Pressing Esc or clicking empty space closes the menu without doing anything.
 *
 * Optional, for the option added last (worked out for each player every time the menu opens):
 *   .details(player -> "1250 · 2s")                       // a small second line under its label
 *   .lockedWhen(player -> !Beyonder.hasSequence(player, 5)) // greyed out, cannot be picked
 * and for the whole menu:
 *   .ring(70)        // the icons evenly round a circle 70 px wide from the middle, the first at the top,
 *                    // clockwise (their own x, y are then not used)
 * A wheel of spells with costs, cooldowns and a remembered last pick is built on this: see AbilityWheel.
 */
public class AbilityMenu {

    @FunctionalInterface
    public interface Action {
        void run(ServerPlayer player, ServerLevel level);
    }

    /** The second line under an option's label, for this player right now ("" = none). */
    @FunctionalInterface
    public interface Details {
        String of(ServerPlayer player);
    }

    /** true = this player cannot pick the option right now: it is shown greyed out. */
    @FunctionalInterface
    public interface Lock {
        boolean locked(ServerPlayer player);
    }

    private static final class Option {
        final String icon;
        final String label;
        final int x;
        final int y;
        final Action action;
        Details details;
        Details description;
        Lock lock;

        Option(String icon, String label, int x, int y, Action action) {
            this.icon = icon;
            this.label = label;
            this.x = x;
            this.y = y;
            this.action = action;
        }

        boolean lockedFor(ServerPlayer player) {
            return lock != null && lock.locked(player);
        }
    }

    // which menu each player currently has open, so a click can only trigger what we actually showed them
    private static final Map<UUID, AbilityMenu> OPEN_MENUS = new HashMap<>();

    private final String id;
    private String title = "";
    private int iconSize = 32;
    private int ringRadius = 0;                  // > 0: the options are laid out round a circle
    private final List<Option> options = new ArrayList<>();
    private Predicate<ServerPlayer> castsWhileSilenced;   // true: a root's ABILITIES does not stop a pick

    private AbilityMenu(String id) {
        this.id = id;
    }

    public static AbilityMenu create(String id) {
        return new AbilityMenu(id);
    }

    /** Text shown above the icons (optional). */
    public AbilityMenu title(String title) {
        this.title = title;
        return this;
    }

    /** Size of every icon on screen in GUI pixels (default 32). */
    public AbilityMenu iconSize(int iconSize) {
        this.iconSize = iconSize;
        return this;
    }

    /** Lays every option out evenly round a circle 'radius' GUI pixels from the middle: the first at the top, clockwise. */
    public AbilityMenu ring(int radius) {
        this.ringRadius = Math.max(0, radius);
        return this;
    }

    /** Adds a clickable icon. Options are numbered 1, 2, 3... in the order you add them. */
    public AbilityMenu option(String icon, String label, int x, int y, Action action) {
        options.add(new Option(icon, label, x, y, action));
        return this;
    }

    /** A small second line under the label of the option added last (its cost, what it needs...). */
    public AbilityMenu details(Details details) {
        if (!options.isEmpty()) options.get(options.size() - 1).details = details;
        return this;
    }

    /** Hover description for the option added last. */
    public AbilityMenu description(Details description) {
        if (!options.isEmpty()) options.get(options.size() - 1).description = description;
        return this;
    }

    /** The option added last is greyed out and cannot be picked while this says true for the player. */
    public AbilityMenu lockedWhen(Lock lock) {
        if (!options.isEmpty()) options.get(options.size() - 1).lock = lock;
        return this;
    }

    /** While this says true for the player, a root with ABILITIES (status/Silence) does not stop a pick. */
    public AbilityMenu castsWhileSilenced(Predicate<ServerPlayer> when) {
        this.castsWhileSilenced = when;
        return this;
    }

    /** Shows this menu to the player. */
    public void open(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        List<COpenAbilityMenuPacket.Option> sent = new ArrayList<>();
        for (int i = 0; i < options.size(); i++) {
            Option option = options.get(i);
            int x = option.x;
            int y = option.y;
            if (ringRadius > 0) {
                double angle = Math.PI * 2 * i / options.size() - Math.PI / 2;     // the first at the top
                x = (int) Math.round(Math.cos(angle) * ringRadius);
                y = (int) Math.round(Math.sin(angle) * ringRadius);
            }
            String detail = option.details == null ? "" : option.details.of(serverPlayer);
            sent.add(new COpenAbilityMenuPacket.Option(ScreenImages.texture(option.icon), option.label, x, y,
                    detail == null ? "" : detail, option.lockedFor(serverPlayer),
                    option.description == null ? "" : option.description.of(serverPlayer)));
        }

        OPEN_MENUS.put(serverPlayer.getUUID(), this);
        PacketHandler.sendToPlayer(new COpenAbilityMenuPacket(id, title, iconSize, sent), serverPlayer);
    }

    /** Called by SAbilityMenuChoicePacket. choice = -1 means the menu was closed without picking. */
    public static void handleChoice(ServerPlayer player, String menuId, int choice) {
        AbilityMenu menu = OPEN_MENUS.get(player.getUUID());
        if (menu == null || !menu.id.equals(menuId)) return;
        OPEN_MENUS.remove(player.getUUID());

        if (choice < 0 || choice >= menu.options.size()) return;
        if (Root.has(player, RootRestriction.ABILITIES)                                    // got rooted while the menu was open
                && (menu.castsWhileSilenced == null || !menu.castsWhileSilenced.test(player))) return;
        Option option = menu.options.get(choice);
        if (option.lockedFor(player)) return;                     // the screen greys these out; never trust it alone
        option.action.run(player, player.level());
    }
}
