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
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * /beyondermob (op level 2), for testing and for maps (docs/BEYONDER_MOBS.md):
 *
 *   /beyondermob set <targets> "<Pathway>" <sequence>   makes them Beyonders of that Pathway and Sequence
 *   /beyondermob clear <targets>                         makes them plain mobs again
 *   /beyondermob reroll <targets>                        clears them and rolls again by their Rule (BeyonderMobRules)
 *   /beyondermob info <target>                           what it is, and which abilities can be taken from it
 *
 * Players among the targets are skipped (/sequence is for them). To summon one: /summon it, then
 * /beyondermob set @e[type=zombie,sort=nearest,limit=1] "Hand Of Order" 7
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID)
public final class BeyonderMobCommands {

    private BeyonderMobCommands() {}

    @SubscribeEvent
    public static void onRegisterBeyonderMobCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("beyondermob")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))

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
