package net.lukario.circleofinevitability.ritual;

import net.lukario.circleofinevitability.menu.AbilityMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Uses the mod's icon menu. Each choice belongs to one opening of one reachable table. */
record RitualTableMenu(ResourceKey<Level> dimension, BlockPos pos, int playerEntityId) {
    private static final String OATH = "white_angel/oath";
    private static final String BACK = "attendant_of_mysteries/transfer";
    private static final int PAGE_SIZE = 3;

    static void open(ServerPlayer player, BlockPos pos) {
        var site = new RitualTableMenu(player.level().dimension(), pos.immutable(), player.getId());
        site.home(player);
    }

    boolean accessible(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator() && player.getId() == playerEntityId
                && player.level().dimension().equals(dimension) && player.isWithinBlockInteractionRange(pos, 0)
                && player.level().getBlockState(pos).is(RitualTableBlock.TABLE.get());
    }

    private AbilityMenu menu(String title) {
        return AbilityMenu.create("ritual/" + UUID.randomUUID()).title(title).iconSize(24).castsWhileSilenced(p -> true);
    }

    private void option(AbilityMenu menu, String icon, String label, int x, int y,
                        String detail, String description, boolean locked, Consumer<ServerPlayer> action) {
        menu.option(icon, label, x, y, (player, level) -> {
            if (accessible(player)) action.accept(player);
        }).details(p -> detail).description(p -> description).lockedWhen(p -> locked);
    }

    private void roster(ServerPlayer player, int page) {
        var entries = Worship.registry(player).listElements()
                .sorted(Comparator.comparing(entry -> entry.value().name())).toList();
        Worship faith = new Worship(player);
        AbilityMenu menu = menu(entries.isEmpty() ? "No deities are available" : "Choose a deity");
        int first = page * PAGE_SIZE;
        for (int slot = 0; slot < PAGE_SIZE && first + slot < entries.size(); slot++) {
            var entry = entries.get(first + slot);
            var deity = entry.value();
            int x = slot == 0 ? 0 : slot == 1 ? -77 : 77;
            int y = slot == 0 ? -56 : 0;
            option(menu, deity.icon().toString(), deity.name(), x, y, deity.blessing().name(),
                    deity.domain() + " " + deity.blessing().perRank(), deity == faith.deity,
                    p -> pledgeScreen(p, entry));
        }
        if (page > 0) option(menu, BACK, "Previous", -80, 61, "", "", false, p -> roster(p, page - 1));
        if (faith.deity != null) option(menu, OATH, "My covenant", 0, 61, "", "", false, this::home);
        if (first + PAGE_SIZE < entries.size())
            option(menu, BACK, "Next", 80, 61, "", "", false, p -> roster(p, page + 1));
        menu.open(player);
    }

    private void pledgeScreen(ServerPlayer player, Holder.Reference<RitualDeity> choice) {
        Worship faith = new Worship(player);
        boolean switching = faith.deity != null;
        var deity = choice.value();
        AbilityMenu menu = menu("Pledge to " + deity.name() + "?");
        String terms = switching ? "Leave " + faith.deity.name() + ", lose its blessings and friendship, and recover "
                + faith.hearts() + " max hearts. Your new covenant starts at zero." : deity.domain();
        option(menu, deity.icon().toString(), switching ? "Change faith" : "Pledge faith", 0, -35,
                switching ? "Reset friendship and blessings" : "First blessing at 10 friendship", terms, false, p -> {
                    new Worship(p).pledge(choice);
                    home(p);
                });
        option(menu, BACK, "Back", 0, 33, switching ? "Keep current faith" : "Choose another deity", "", false,
                p -> roster(p, 0));
        menu.open(player);
    }

    private void home(ServerPlayer player) {
        Worship faith = new Worship(player);
        var deity = faith.deity;
        if (deity == null) {
            roster(player, 0);
            return;
        }
        AbilityMenu menu = menu(deity.name() + "  ·  Friendship " + faith.friendship() + "/100");
        String offer = deity.offering().value().getDefaultInstance().getHoverName().getString();
        option(menu, "white_angel/blessing", "Give offering", -74, -64,
                "1 " + offer + "  ·  +" + Worship.OFFERING_POINTS, "Offer one " + offer + " from your inventory.",
                !faith.canEarn(), p -> perform(p, Worship::offer, "You need one " + offer + " in your inventory."));
        option(menu, "filthy_monarch/blood_burst", "Donate blood", 74, -64,
                "1 current heart  ·  +" + Worship.BLOOD_POINTS, "Take damage to earn friendship. You need more than one current heart.",
                !faith.canEarn(), p -> perform(p, Worship::donateBlood, "Your blood donation must draw health. Recover and try again."));
        option(menu, "hand_of_order/contract", "Report task", -74, 0,
                faith.taskProgress() + "/" + deity.task().count() + "  ·  +" + Worship.TASK_POINTS,
                deity.task().description() + " Return here to collect friendship. This task is repeatable.",
                !faith.canEarn() || faith.taskProgress() < deity.task().count(),
                p -> perform(p, Worship::claimTask, "Complete the task first."));
        option(menu, deity.icon().toString(), "Sacrifice heart", 74, 0,
                faith.hearts() + "/" + Worship.MAX_HEARTS + "  ·  " + (faith.hearts() == Worship.MAX_HEARTS ? "Complete" : "Needs " + faith.limit()),
                "Give up one maximum heart for the next blessing rank. Lost max hearts return only when you change faith.",
                !faith.canSacrifice(), this::heartScreen);
        String blessing = deity.blessing().perRank() + " " + faith.nextRank();
        option(menu, "white_angel/holy_eye", deity.blessing().name(), -74, 62,
                "Blessing " + faith.rank() + "/" + Worship.MAX_RANK, blessing, false, p -> {
                    p.sendSystemMessage(Component.literal(deity.blessing().name() + " · " + blessing));
                    home(p);
                });
        option(menu, OATH, "Change faith", 74, 62, "Choose a deity",
                "Changing faith restores sacrificed hearts and resets your friendship and blessings.", false, p -> roster(p, 0));
        menu.open(player);
    }

    private void heartScreen(ServerPlayer player) {
        Worship faith = new Worship(player);
        if (!faith.canSacrifice()) {
            home(player);
            return;
        }
        AbilityMenu menu = menu("Give up one maximum heart?");
        option(menu, "filthy_monarch/blood_burst", "Sacrifice heart", -68, 0,
                "Max health decreases by 2", "This cost lasts through death and relogging. Change faith to recover the heart.",
                false, p -> perform(p, Worship::sacrifice, "The heart sacrifice is unavailable."));
        option(menu, BACK, "Keep heart", 68, 0, "Return to table", "", false, this::home);
        menu.open(player);
    }

    private void perform(ServerPlayer player, Predicate<Worship> action, String failure) {
        if (!action.test(new Worship(player))) player.sendOverlayMessage(Component.literal(failure));
        home(player);
    }
}
