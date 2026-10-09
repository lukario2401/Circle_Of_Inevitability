package net.lukario.frogerealm.menu;

import net.lukario.frogerealm.network.COpenAbilityMenuPacket;
import net.lukario.frogerealm.network.PacketHandler;
import net.lukario.frogerealm.root.Root;
import net.lukario.frogerealm.root.RootRestriction;
import net.lukario.frogerealm.screen.ScreenImages;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
 * - icon:  PNG in assets/forgerealmmod/textures/gui/ without ".png" (same as ScreenImages)
 * - x, y:  where the icon's center goes, in GUI pixels from the middle of the screen (+x right, +y down)
 * - action runs on the server when the player clicks that icon (or presses its number key 1-9)
 * Pressing Esc or clicking empty space closes the menu without doing anything.
 */
public class AbilityMenu {

    @FunctionalInterface
    public interface Action {
        void run(ServerPlayer player, ServerLevel level);
    }

    private record Option(String icon, String label, int x, int y, Action action) {}

    // which menu each player currently has open, so a click can only trigger what we actually showed them
    private static final Map<UUID, AbilityMenu> OPEN_MENUS = new HashMap<>();

    private final String id;
    private String title = "";
    private int iconSize = 32;
    private final List<Option> options = new ArrayList<>();

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

    /** Adds a clickable icon. Options are numbered 1, 2, 3... in the order you add them. */
    public AbilityMenu option(String icon, String label, int x, int y, Action action) {
        options.add(new Option(icon, label, x, y, action));
        return this;
    }

    /** Shows this menu to the player. */
    public void open(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        List<COpenAbilityMenuPacket.Option> sent = new ArrayList<>();
        for (Option option : options) {
            sent.add(new COpenAbilityMenuPacket.Option(
                    ScreenImages.texture(option.icon()), option.label(), option.x(), option.y()));
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
        if (Root.has(player, RootRestriction.ABILITIES)) return; // got rooted while the menu was open
        menu.options.get(choice).action().run(player, player.serverLevel());
    }
}
