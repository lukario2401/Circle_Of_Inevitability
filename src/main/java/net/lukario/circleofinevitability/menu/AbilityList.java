package net.lukario.circleofinevitability.menu;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.network.COpenAbilityListPacket;
import net.lukario.circleofinevitability.network.PacketHandler;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.screen.ScreenImages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A scrollable list of rows to pick from, for when a ring (AbilityMenu) would have too many icons to fit. Like
 * AbilityMenu, everything is defined here on the server and the client screen (client/AbilityListScreen) is generic.
 *
 *   AbilityList.create("my_list").title("Pick up to two").maxPicks(2).confirmLabel("Take")
 *           .row("icons/fire", "Fire", "Sequence 7")                      // icon, name, a second line
 *           .row("icons/water", "Water", "too strong").locked(true)       // greyed out, cannot be picked
 *           .onPick((player, picks) -> { ...picks are row indexes, in the order they were picked... })
 *           .onCancel(player -> { ...closed without picking (optional)... })
 *           .open(player);
 *
 * maxPicks(1) (the default): clicking a row picks it at once. More: clicking toggles rows, and the confirm button
 * (or Enter) sends them. Esc closes without picking. The server checks every pick again (range, locked, count).
 * A list is usually built fresh each time it opens, so its rows can show this player's own state.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID)
public final class AbilityList {

    @FunctionalInterface
    public interface Picked {
        void run(ServerPlayer player, List<Integer> picks);
    }

    /** icon: as for ScreenImages ("" = none). marked: drawn highlighted (the current selection, say). */
    public record Row(String icon, String name, String detail, String description, boolean locked, boolean marked) {}

    private static final Map<UUID, AbilityList> OPEN = new HashMap<>();

    private final String id;
    private String title = "";
    private String hint = "";
    private String confirmLabel = "Confirm";
    private int maxPicks = 1;
    private final List<Row> rows = new ArrayList<>();
    private Picked picked = (player, picks) -> {};
    private Consumer<ServerPlayer> cancelled = player -> {};

    private AbilityList(String id) {
        this.id = id;
    }

    public static AbilityList create(String id) {
        return new AbilityList(id);
    }

    public AbilityList title(String title) { this.title = title; return this; }

    /** A small line under the list ("" = a default that explains the controls). */
    public AbilityList hint(String hint) { this.hint = hint; return this; }

    /** The confirm button's text when more than one row can be picked. */
    public AbilityList confirmLabel(String label) { this.confirmLabel = label; return this; }

    public AbilityList maxPicks(int maxPicks) { this.maxPicks = Math.max(1, maxPicks); return this; }

    public AbilityList row(String icon, String name, String detail) {
        rows.add(new Row(icon == null ? "" : icon, name, detail == null ? "" : detail, "", false, false));
        return this;
    }

    private AbilityList last(java.util.function.UnaryOperator<Row> change) {
        if (!rows.isEmpty()) rows.set(rows.size() - 1, change.apply(rows.getLast()));
        return this;
    }

    /** The row added last cannot be picked. */
    public AbilityList locked(boolean locked) {
        return last(r -> new Row(r.icon(), r.name(), r.detail(), r.description(), locked, r.marked()));
    }

    /** The row added last is drawn highlighted. */
    public AbilityList marked(boolean marked) {
        return last(r -> new Row(r.icon(), r.name(), r.detail(), r.description(), r.locked(), marked));
    }

    /** Hover text for the row added last. */
    public AbilityList description(String description) {
        return last(r -> new Row(r.icon(), r.name(), r.detail(), description == null ? "" : description, r.locked(), r.marked()));
    }

    public AbilityList onPick(Picked picked) { this.picked = picked; return this; }

    public AbilityList onCancel(Consumer<ServerPlayer> cancelled) { this.cancelled = cancelled; return this; }

    public String id() { return id; }
    public int maxPicks() { return maxPicks; }
    public List<Row> rows() { return List.copyOf(rows); }

    public void open(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        List<COpenAbilityListPacket.Row> sent = new ArrayList<>();
        for (Row row : rows)
            sent.add(new COpenAbilityListPacket.Row(row.icon().isEmpty() ? "" : ScreenImages.texture(row.icon()).toString(),
                    row.name(), row.detail(), row.description(), row.locked(), row.marked()));
        OPEN.put(serverPlayer.getUUID(), this);
        PacketHandler.sendToPlayer(new COpenAbilityListPacket(id, title, hint, confirmLabel, maxPicks, sent), serverPlayer);
    }

    /** The list this player has open, or null (for tests and for code that must not open a second one). */
    public static AbilityList openFor(Player player) {
        return OPEN.get(player.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        OPEN.remove(event.getEntity().getUUID());
    }

    /**
     * Called by SAbilityListChoicePacket (and tests). An empty list of picks means it was closed without picking.
     * Picks that are out of range, locked, repeated or too many are refused, and the list stays open.
     */
    public static boolean handleChoice(ServerPlayer player, String listId, List<Integer> picks) {
        AbilityList list = OPEN.get(player.getUUID());
        if (list == null || !list.id.equals(listId)) return false;
        if (picks.isEmpty()) {
            OPEN.remove(player.getUUID());
            list.cancelled.accept(player);
            return true;
        }
        if (picks.size() > list.maxPicks || new LinkedHashSet<>(picks).size() != picks.size()) return false;
        for (int pick : picks)
            if (pick < 0 || pick >= list.rows.size() || list.rows.get(pick).locked()) return false;
        if (Root.has(player, RootRestriction.ABILITIES)) return false;      // got rooted while the list was open
        OPEN.remove(player.getUUID());
        list.picked.run(player, List.copyOf(picks));
        return true;
    }
}
