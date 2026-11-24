package net.lukario.circleofinevitability.ritual;

import net.lukario.circleofinevitability.menu.AbilityMenu;
import net.lukario.circleofinevitability.item.ModItems;
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
        if (!faith.hasAssignment()) faith.bind(pos);
        var altar = RitualAltar.inspect(player.level(), pos);
        var menu = menu(deity.name() + " · " + (faith.exalted() ? "Favour " + faith.favour() : "Friendship " + faith.friendship()) + "/100");
        option(menu, "white_angel/blessing", "Offerings", -85, -56, "Gifts, blood and prayer",
                "Choose the cost and see the friendship it earns.", false, this::offerings);
        option(menu, "hand_of_order/contract", "Ritual task", 0, -56, waitText(faith, "task_ready"),
                faith.task().instructions(), false, this::task);
        option(menu, "white_angel/domain", "Altar structure", 85, -56, "Rank " + altar.rank() + "/3",
                altar.pattern(), false, this::structure);
        option(menu, deity.icon().toString(), "Sacrifice heart", -85, 14,
                faith.hearts() + "/3 · " + (faith.exalted() ? "Complete" : "Needs " + faith.gate()),
                "Requires enough friendship and an altar that supports that level.", !faith.canSacrifice(altar), this::heartScreen);
        option(menu, "white_angel/holy_eye", "Blessings", 0, 14, "Rank " + faith.rank() + "/7",
                deity.blessing().perRank() + " " + faith.nextRank(), false, this::blessings);
        option(menu, OATH, "Change faith", 85, 14, "Choose a deity",
                "Changing faith restores sacrificed hearts and resets your worship.", false, p -> roster(p, 0));
        if (faith.exalted()) {
            option(menu, deity.icon().toString(), "Higher favour", 0, 70, faith.favour() + "/100",
                    "Complete tasks at a rank-three sanctuary. One favour fades every two online minutes.", false, this::higher);
        }
        menu.open(player);
    }

    private static String waitText(Worship faith, String key) {
        int seconds = faith.waitSeconds(key);
        return seconds <= 0 ? "Available" : "Ready in " + seconds + "s";
    }

    private void back(AbilityMenu menu) {
        option(menu, BACK, "Back", 0, 70, "Return to table", "", false, this::home);
    }

    private void offerings(ServerPlayer p) {
        giftPage(p, 0);
    }

    private void giftPage(ServerPlayer p, int page) {
        Worship f = new Worship(p);
        RitualAltar altar = RitualAltar.inspect(p.level(), pos);
        AbilityMenu menu = menu("Offerings · " + waitText(f, "offering_ready"));
        var gifts = f.deity.offerings();
        int room = f.room(altar, false);
        for (int i = page * 3; i < Math.min(gifts.size(), page * 3 + 3); i++) {
            var gift = gifts.get(i);
            option(menu, "white_angel/blessing", gift.label(), (i % 3 - 1) * 95, -58,
                    "+" + Math.min(room, gift.points()) + " friendship",
                    "Spend " + gift.label() + " for up to " + gift.points() + " friendship. Offerings share a 45s cooldown.",
                    room == 0 || f.waitSeconds("offering_ready") > 0,
                    player -> perform(player, faith -> faith.offer(pos, gift), "The altar could not accept that offering."));
        }
        boolean holdingKnife = p.getMainHandItem().is(ModItems.SACRIFICIAL_KNIFE.get());
        option(menu, "filthy_monarch/blood_burst", "Donate blood", -65, 0, holdingKnife ? waitText(f, "blood_ready") : "Hold a Sacrificial Knife",
                holdingKnife ? "Pay one current heart for up to 3 friendship. Cooldown 90s."
                        : "To craft a knife, carry glass bottles in your offhand and kill two adult mobs such as zombies or cows.",
                !holdingKnife || room == 0 || f.waitSeconds("blood_ready") > 0,
                player -> perform(player, faith -> faith.donateBlood(pos), "Hold your Sacrificial Knife. The donation must draw health."));
        option(menu, OATH, "Offer experience", 65, 0, waitText(f, "prayer_ready"),
                "Offer one experience level in prayer for up to 6 friendship. Cooldown 5 minutes.", room == 0 || f.waitSeconds("prayer_ready") > 0,
                player -> perform(player, faith -> faith.pray(pos), "You need one experience level."));
        if (page > 0) giftNav(menu, "Previous", -86, page - 1);
        if ((page + 1) * 3 < gifts.size()) giftNav(menu, "Next", 86, page + 1);
        back(menu);
        menu.open(p);
    }

    private void giftNav(AbilityMenu menu, String label, int x, int page) {
        option(menu, BACK, label, x, 70, "", "", false, p -> giftPage(p, page));
    }

    private void task(ServerPlayer p) {
        Worship f = new Worship(p);
        if (!f.hasAssignment()) f.bind(pos);
        var t = f.task();
        var altar = RitualAltar.inspect(p.level(), pos);
        var menu = menu("Ritual task · " + waitText(f, "task_ready"));
        int done = f.taskProgress(pos);
        String terms = t.instructions() + " Earn up to " + t.reward() + (f.exalted() ? " favour" : " friendship")
                + ". The next task opens " + t.cooldown() + "s after reporting.";
        p.sendSystemMessage(Component.literal(terms));
        option(menu, "hand_of_order/contract", "Report task", -65, 0, done + "/" + t.count(),
                terms, done < t.count() || f.room(altar, true) == 0,
                player -> perform(player, faith -> faith.claimTask(pos), "The task is incomplete or this altar has reached its cap."));
        option(menu, OATH, "Move assignment", 65, 0, "Use this altar",
                "Restart the same assignment here. Progress at the former altar is cleared.",
                f.boundHere(pos) || f.waitSeconds("task_ready") > 0, player -> { new Worship(player).bind(pos); task(player); });
        back(menu);
        menu.open(p);
    }

    private void structure(ServerPlayer p) {
        RitualAltar altar = RitualAltar.inspect(p.level(), pos);
        var menu = menu("Altar " + altar.rank() + "/3 · Friendship cap " + altar.limit());
        p.sendSystemMessage(Component.literal(altar.pattern()));
        for (var missing : altar.missing()) {
            p.sendSystemMessage(Component.literal(missing.description()));
            missing.mark(p);
        }
        option(menu, "white_angel/domain", "Building pattern", 0, -10, altar.missing().size() + " missing",
                altar.pattern() + " Check chat for the missing blocks and their coordinates.", false, this::structure);
        back(menu);
        menu.open(p);
    }

    private void blessings(ServerPlayer p) {
        Worship f = new Worship(p);
        var blessing = f.deity.blessing();
        p.sendSystemMessage(Component.literal(blessing.name() + " · " + blessing.perRank()));
        for (var stage : blessing.stages()) {
            p.sendSystemMessage(Component.literal("Rank " + stage.rank() + (f.rank() >= stage.rank() ? " (active) · " : " · ") + stage.description()));
        }
        p.sendSystemMessage(Component.literal(f.nextRank()));
        home(p);
    }

    private void higher(ServerPlayer p) {
        Worship f = new Worship(p);
        var altar = RitualAltar.inspect(p.level(), pos);
        var menu = menu("Higher favour · " + f.favour() + "/100");
        p.sendSystemMessage(Component.literal("Sanctuary tasks replenish favour. One point fades every two online minutes. Rank seven needs 50."));
        if (f.deity.revival()) {
            long minutes = (f.revivalWait() + 59_999) / 60_000;
            String detail = f.armed() ? "Armed" : minutes > 0 ? minutes + "m cooldown" : "Needs 100 favour";
            option(menu, f.deity.icon().toString(), "Invoke miracle", 0, -15, detail,
                    "At full favour, your three covenant hearts qualify you for one revival. The miracle fades at zero favour. Revival spends the remaining favour and starts a two-hour cooldown.",
                    !f.canArm(altar), player -> perform(player, faith -> faith.arm(pos), "The miracle is unavailable."));
        }
        back(menu);
        menu.open(p);
    }

    private void heartScreen(ServerPlayer player) {
        Worship faith = new Worship(player);
        if (!faith.canSacrifice(RitualAltar.inspect(player.level(), pos))) { home(player); return; }
        AbilityMenu menu = menu("Give up one maximum heart?");
        option(menu, "filthy_monarch/blood_burst", "Sacrifice heart", -68, 0,
                "Max health decreases by 2", "This cost lasts through death and relogging. Change faith to recover the heart.",
                false, p -> perform(p, f -> f.sacrifice(pos), "The heart sacrifice is unavailable."));
        option(menu, BACK, "Keep heart", 68, 0, "Return to table", "", false, this::home);
        menu.open(player);
    }

    private void perform(ServerPlayer player, Predicate<Worship> action, String failure) {
        if (!action.test(new Worship(player))) player.sendOverlayMessage(Component.literal(failure));
        home(player);
    }
}
