package net.lukario.circleofinevitability.combat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** /friend add <player-name|mob-uuid>, remove <name|uuid>, list. Available without OP. */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FriendCommands {
    private FriendCommands() {}

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("friend")
                .requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(ctx -> list(ctx.getSource()))
                .then(Commands.literal("add")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggest(ctx.getSource(), builder, false))
                                .executes(ctx -> add(ctx.getSource(), StringArgumentType.getString(ctx, "target")))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests((ctx, builder) -> suggest(ctx.getSource(), builder, true))
                                .executes(ctx -> remove(ctx.getSource(), StringArgumentType.getString(ctx, "target")))))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource()))));
    }

    private static CompletableFuture<Suggestions> suggest(CommandSourceStack source, SuggestionsBuilder builder, boolean removing) {
        ServerPlayer owner = source.getPlayer();
        Set<String> choices = new LinkedHashSet<>();
        if (owner != null) {
            if (removing) {
                for (Friends.Entry entry : Friends.entries(owner)) {
                    choices.add(entry.id().toString());
                    if (entry.player()) {
                        choices.add(entry.name());
                        ServerPlayer online = source.getServer().getPlayerList().getPlayer(entry.id());
                        if (online != null) choices.add(online.getPlainTextName());
                    }
                }
            } else {
                for (ServerPlayer online : source.getServer().getPlayerList().getPlayers()) {
                    if (online != owner && !Friends.contains(owner, online)) choices.add(online.getPlainTextName());
                }
                LivingEntity aimed = Spells.aimLiving(owner, source.getLevel(), 32, false, entity -> entity instanceof Mob);
                if (aimed != null && !Friends.contains(owner, aimed)) choices.add(aimed.getUUID().toString());
            }
        }
        return SharedSuggestionProvider.suggest(choices, builder);
    }

    private static UUID uuid(String text) {
        try {
            UUID id = UUID.fromString(text);
            return id.toString().equalsIgnoreCase(text) ? id : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static NameAndId player(CommandSourceStack source, String name) {
        if (!name.matches("[A-Za-z0-9_]{1,16}")) return null;
        ServerPlayer online = source.getServer().getPlayerList().getPlayer(name);
        return online != null ? online.nameAndId() : source.getServer().services().nameToIdCache().get(name).orElse(null);
    }

    private static int fail(CommandSourceStack source, String key, Object... args) {
        source.sendFailure(Component.translatable("message.circle_of_inevitability.friend." + key, args));
        return 0;
    }

    private static int add(CommandSourceStack source, String input) throws CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        UUID id = uuid(input);
        String name = input;
        boolean isPlayer = false;
        if (id == null) {
            NameAndId profile = player(source, input);
            if (profile == null) return fail(source, "unknown", input);
            id = profile.id();
            name = profile.name();
            isPlayer = true;
        } else {
            Entity found = source.getServer().getPlayerList().getPlayer(id);
            if (found == null) {
                for (var level : source.getServer().getAllLevels()) {
                    found = level.getEntity(id);
                    if (found != null) break;
                }
            }
            if (found != null) {
                if (!(found instanceof LivingEntity) || found instanceof ArmorStand || !found.isAlive())
                    return fail(source, "not_living");
                name = found.getName().getString();
                isPlayer = found instanceof ServerPlayer;
            } else {
                NameAndId profile = source.getServer().services().nameToIdCache().get(id).orElse(null);
                if (profile != null) {
                    name = profile.name();
                    isPlayer = true;
                }
                // An explicit UUID may belong to a mob in an unloaded chunk or dimension.
            }
        }
        if (owner.getUUID().equals(id)) return fail(source, "self");
        if (!Friends.add(owner, id, name, isPlayer)) return fail(source, "duplicate", name);
        String label = name;
        source.sendSuccess(() -> Component.translatable("message.circle_of_inevitability.friend.added", label), false);
        return 1;
    }

    private static int remove(CommandSourceStack source, String input) throws CommandSyntaxException {
        ServerPlayer owner = source.getPlayerOrException();
        UUID id = uuid(input);
        if (id == null) {
            // Keep the saved name usable even while a friend is offline or after their name changes.
            id = Friends.entries(owner).stream().filter(entry -> entry.player() && entry.name().equalsIgnoreCase(input))
                    .map(Friends.Entry::id).findFirst().orElse(null);
            if (id == null) {
                NameAndId profile = player(source, input);
                if (profile != null) id = profile.id();
            }
        }
        if (id == null || !Friends.remove(owner, id)) return fail(source, "missing", input);
        source.sendSuccess(() -> Component.translatable("message.circle_of_inevitability.friend.removed", input), false);
        return 1;
    }

    private static int list(CommandSourceStack source) throws CommandSyntaxException {
        var entries = Friends.entries(source.getPlayerOrException());
        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("message.circle_of_inevitability.friend.empty"), false);
        } else {
            source.sendSuccess(() -> Component.translatable("message.circle_of_inevitability.friend.list", entries.size()), false);
            for (Friends.Entry entry : entries) {
                ServerPlayer online = source.getServer().getPlayerList().getPlayer(entry.id());
                String name = online != null ? online.getPlainTextName() : entry.name();
                source.sendSuccess(() -> Component.translatable("message.circle_of_inevitability.friend.entry", name, entry.id().toString()), false);
            }
        }
        return entries.size();
    }
}
