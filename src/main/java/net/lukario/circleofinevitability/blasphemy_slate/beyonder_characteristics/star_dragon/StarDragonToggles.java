package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Star Dragon passives a player can switch off for themselves, with /stardragonpassive (any player, no permission):
 *
 *   /stardragonpassive                      lists every toggle and whether it is on
 *   /stardragonpassive warning off          Spiritual Intuition's warning chime is silent (the stars at your feet stay)
 *   /stardragonpassive radiance off         Radiance (Radiant Angel) no longer burns enemies around you, nor glows
 *   /stardragonpassive <name> on            back on
 *
 * Saved on the player, kept through death and relogging. Everything starts on.
 *
 * A new toggle: one line below (add("name", "what it is")), then where the passive runs:
 *   if (!StarDragonToggles.on(player, StarDragonToggles.MY_TOGGLE)) return;
 * The command, the list and the save pick it up by themselves.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StarDragonToggles {

    private StarDragonToggles() {}

    /** One passive a player may switch off. tag: the save key on the player (true = switched off). */
    public record Toggle(String id, String description, String tag) {}

    private static final List<Toggle> ALL = new ArrayList<>();

    public static final Toggle WARNING = add("warning", "Spiritual Intuition's warning chime");
    public static final Toggle RADIANCE = add("radiance", "Radiance's burning aura (passive area damage)");

    private static Toggle add(String id, String description) {
        Toggle toggle = new Toggle(id, description, "star_dragon_off_" + id);
        ALL.add(toggle);
        return toggle;
    }

    public static List<Toggle> all() { return List.copyOf(ALL); }

    /** True unless this player switched it off. Anything that is not a player (a Star Dragon mob) always has it on. */
    public static boolean on(net.minecraft.world.entity.Entity entity, Toggle toggle) {
        return !(entity instanceof Player player) || !player.getPersistentData().getBooleanOr(toggle.tag(), false);
    }

    public static void set(Player player, Toggle toggle, boolean on) {
        if (on) player.getPersistentData().remove(toggle.tag());
        else player.getPersistentData().putBoolean(toggle.tag(), true);
    }

    // =========================
    // The command
    // =========================

    @SubscribeEvent
    public static void onStarDragonTogglesCommands(RegisterCommandsEvent event) {
        // no .requires(...): every player may use it, on themselves only
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("stardragonpassive")
                .executes(ctx -> list(ctx.getSource()));
        for (Toggle toggle : ALL) {
            root.then(Commands.literal(toggle.id())
                    .executes(ctx -> show(ctx.getSource(), toggle))
                    .then(Commands.literal("on").executes(ctx -> change(ctx.getSource(), toggle, true)))
                    .then(Commands.literal("off").executes(ctx -> change(ctx.getSource(), toggle, false))));
        }
        event.getDispatcher().register(root);
    }

    private static int list(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        StringBuilder text = new StringBuilder("Star Dragon passives (/stardragonpassive <name> on|off):");
        for (Toggle toggle : ALL) {
            text.append("\n  ").append(toggle.id()).append(": ").append(on(player, toggle) ? "on" : "off")
                    .append(" - ").append(toggle.description());
        }
        source.sendSuccess(() -> Component.literal(text.toString()), false);
        return 1;
    }

    private static int show(CommandSourceStack source, Toggle toggle) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        source.sendSuccess(() -> Component.literal(toggle.description() + ": " + (on(player, toggle) ? "on" : "off")), false);
        return 1;
    }

    private static int change(CommandSourceStack source, Toggle toggle, boolean value) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        set(player, toggle, value);
        source.sendSuccess(() -> Component.literal(toggle.description() + " is now " + (value ? "on" : "off") + "."), false);
        return 1;
    }

    // what a player switched off stays off after dying
    @SubscribeEvent
    public static void onStarDragonTogglesClone(PlayerEvent.Clone event) {
        CompoundTag old = event.getOriginal().getPersistentData();
        for (Toggle toggle : ALL) {
            if (old.getBooleanOr(toggle.tag(), false)) event.getEntity().getPersistentData().putBoolean(toggle.tag(), true);
        }
    }
}
