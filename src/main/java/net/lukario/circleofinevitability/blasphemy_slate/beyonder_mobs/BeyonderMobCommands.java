package net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SequenceCommands;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PathwayAbilities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * /beyondermob (op level 2), for testing and for maps (docs/BEYONDER_MOBS.md):
 *
 *   /beyondermob summon <entity> "<Pathway>" <sequence> [pos] [count]
 *                                                        summons new mobs that already are Beyonders of it (any
 *                                                        living mob, any Sequence 9 to 0; at most MAX_SUMMON at once)
 *   /beyondermob set <targets> "<Pathway>" <sequence>   makes them Beyonders of that Pathway and Sequence
 *   /beyondermob clear <targets>                         makes them plain mobs again
 *   /beyondermob reroll <targets>                        clears them and rolls again by their Rule (BeyonderMobRules)
 *   /beyondermob info <target>                           what it is, and which abilities can be taken from it
 *
 * Players among the targets are skipped (/sequence is for them). Example:
 * /beyondermob summon zombie "Hand Of Order" 7 ~ ~ ~ 3
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID)
public final class BeyonderMobCommands {

    /** The most mobs one /beyondermob summon makes. */
    public static final int MAX_SUMMON = 64;

    private BeyonderMobCommands() {}

    @SubscribeEvent
    public static void onRegisterBeyonderMobCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("beyondermob")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))

                // /beyondermob summon <entity> "<Pathway>" <sequence> [pos] [count]
                .then(Commands.literal("summon")
                        .then(Commands.argument("entity", IdentifierArgument.id())
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        BuiltInRegistries.ENTITY_TYPE.keySet().stream().map(BeyonderMobCommands::shortId), builder))
                                .then(Commands.argument("pathway_name", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                SequenceCommands.PATHWAYS.stream().map(name -> "\"" + name + "\""), builder))
                                        .then(Commands.argument("sequence", IntegerArgumentType.integer(Beyonder.STRONGEST_SEQUENCE, Beyonder.WEAKEST_SEQUENCE))
                                                .executes(ctx -> summon(ctx, ctx.getSource().getPosition(), 1))
                                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                                        .executes(ctx -> summon(ctx, Vec3Argument.getVec3(ctx, "pos"), 1))
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, MAX_SUMMON))
                                                                .executes(ctx -> summon(ctx, Vec3Argument.getVec3(ctx, "pos"),
                                                                        IntegerArgumentType.getInteger(ctx, "count")))))))))

                // /beyondermob set <targets> "<Pathway>" <sequence>
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .then(Commands.argument("pathway_name", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                SequenceCommands.PATHWAYS.stream().map(name -> "\"" + name + "\""), builder))
                                        .then(Commands.argument("sequence", IntegerArgumentType.integer(Beyonder.STRONGEST_SEQUENCE, Beyonder.WEAKEST_SEQUENCE))
                                                .executes(BeyonderMobCommands::set)))))

                // /beyondermob clear <targets>
                .then(Commands.literal("clear")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(ctx -> {
                                    List<LivingEntity> mobs = mobs(ctx);
                                    for (LivingEntity mob : mobs) BeyonderMobs.clear(mob);
                                    int count = mobs.size();
                                    ctx.getSource().sendSuccess(() -> Component.literal("Cleared " + count + " mob(s)."), true);
                                    return count;
                                })))

                // /beyondermob reroll <targets>
                .then(Commands.literal("reroll")
                        .then(Commands.argument("targets", EntityArgument.entities())
                                .executes(ctx -> {
                                    List<LivingEntity> mobs = mobs(ctx);
                                    int became = 0;
                                    for (LivingEntity mob : mobs) {
                                        if (BeyonderMobs.reroll(mob)) became++;
                                    }
                                    int count = mobs.size();
                                    int finalBecame = became;
                                    ctx.getSource().sendSuccess(() -> Component.literal("Rolled " + count + " mob(s): "
                                            + finalBecame + " became Beyonders."), true);
                                    return became;
                                })))

                // /beyondermob info <target>
                .then(Commands.literal("info")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(ctx -> {
                                    Entity entity = EntityArgument.getEntity(ctx, "target");
                                    if (!(entity instanceof LivingEntity living)) {
                                        ctx.getSource().sendFailure(Component.literal("That is not a living creature."));
                                        return 0;
                                    }
                                    ctx.getSource().sendSuccess(() -> info(living), false);
                                    return 1;
                                })))
        );
    }

    private static int summon(CommandContext<CommandSourceStack> ctx, Vec3 at, int count) {
        CommandSourceStack source = ctx.getSource();
        Identifier id = IdentifierArgument.getId(ctx, "entity");
        String pathway = StringArgumentType.getString(ctx, "pathway_name");
        int sequence = IntegerArgumentType.getInteger(ctx, "sequence");
        if (!BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {      // getValue would quietly give a pig
            source.sendFailure(Component.literal("No mob is called \"" + id + "\"."));
            return 0;
        }
        if (!SequenceCommands.PATHWAYS.contains(pathway)) {
            source.sendFailure(Component.literal("No Pathway is called \"" + pathway + "\"."));
            return 0;
        }
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        ServerLevel sl = source.getLevel();
        int made = 0;
        LivingEntity last = null;
        for (int i = 0; i < count; i++) {
            LivingEntity mob = BeyonderMobs.summon(sl, type, at, pathway, sequence);
            if (mob == null) break;                                  // the same type fails the same way every time
            last = mob;
            made++;
        }
        if (made == 0) {
            source.sendFailure(Component.literal("Could not summon \"" + id + "\" as a Beyonder there (it must be a living mob, not a player)."));
            return 0;
        }
        int finalMade = made;
        Component name = last.getDisplayName();
        source.sendSuccess(() -> finalMade == 1
                ? Component.literal("Summoned ").append(name)
                : Component.literal("Summoned " + finalMade + " x ").append(name), true);
        return made;
    }

    /** "zombie" for minecraft:zombie, the full id for any other mod's mob. */
    private static String shortId(Identifier id) {
        return "minecraft".equals(id.getNamespace()) ? id.getPath() : id.toString();
    }

    private static int set(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String pathway = StringArgumentType.getString(ctx, "pathway_name");
        int sequence = IntegerArgumentType.getInteger(ctx, "sequence");
        if (!SequenceCommands.PATHWAYS.contains(pathway)) {
            ctx.getSource().sendFailure(Component.literal("No Pathway is called \"" + pathway + "\"."));
            return 0;
        }
        int made = 0;
        for (LivingEntity mob : mobs(ctx)) {
            if (BeyonderMobs.make(mob, pathway, sequence)) made++;
        }
        int finalMade = made;
        ctx.getSource().sendSuccess(() -> Component.literal("Made " + finalMade + " mob(s) Beyonders of " + pathway
                + " at Sequence " + sequence + "."), true);
        return made;
    }

    /** The living, non-player targets. */
    private static List<LivingEntity> mobs(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        List<LivingEntity> mobs = new ArrayList<>();
        for (Entity entity : EntityArgument.getEntities(ctx, "targets")) {
            if (entity instanceof LivingEntity living && !(entity instanceof Player)) mobs.add(living);
        }
        return mobs;
    }

    private static Component info(LivingEntity entity) {
        String name = entity.getType().getDescription().getString();
        if (!Beyonders.isBeyonder(entity)) return Component.literal(name + " is no Beyonder.");
        List<String> takeable = new ArrayList<>();
        for (PathwayAbilities.Ability ability : Beyonders.takeableFrom(entity)) takeable.add(ability.name());
        return Component.literal(name + ": " + Beyonders.describe(entity) + ". Can be taken: "
                + (takeable.isEmpty() ? "nothing" : String.join(", ", takeable)) + ".");
    }
}
