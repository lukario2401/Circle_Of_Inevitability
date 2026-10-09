package net.lukario.frogerealm.item.custom.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;


import java.util.Random;

public class SpawnFieldCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        dispatcher.register(
                Commands.literal("spawnfield")
                        .requires(cs -> cs.hasPermission(2))

                        .then(Commands.argument("entity", ResourceLocationArgument.id())
                                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1.0))
                                        .then(Commands.argument("density", DoubleArgumentType.doubleArg(0.01))
                                                .then(Commands.argument("maxCount", IntegerArgumentType.integer(1))

                                                        .executes(ctx -> {

                                                            ServerLevel level = ctx.getSource().getLevel();

                                                            ResourceLocation id =
                                                                    ResourceLocationArgument.getId(ctx, "entity");

                                                            EntityType<?> entityType =
                                                                    BuiltInRegistries.ENTITY_TYPE.get(id);

                                                            if (entityType == null) {
                                                                ctx.getSource().sendFailure(
                                                                        net.minecraft.network.chat.Component.literal(
                                                                                "Invalid entity type: " + id
                                                                        )
                                                                );
                                                                return 0;
                                                            }

                                                            double radius =
                                                                    DoubleArgumentType.getDouble(ctx, "radius");

                                                            double density =
                                                                    DoubleArgumentType.getDouble(ctx, "density");

                                                            int maxCount =
                                                                    IntegerArgumentType.getInteger(ctx, "maxCount");

                                                            return execute(
                                                                    ctx.getSource(),
                                                                    entityType,
                                                                    radius,
                                                                    density,
                                                                    maxCount
                                                            );
                                                        })
                                                )
                                        )
                                )
                        )
        );
    }
    private static int execute(
            CommandSourceStack source,
            EntityType<?> type,
            double radius,
            double density,
            int maxCount
    ) {

        ServerLevel level = source.getLevel();
        Vec3 center = source.getPosition();

        double area = Math.PI * radius * radius;
        int targetCount = (int) Math.min(area * density, maxCount);

        int spawned = 0;
        Random random = (Random) level.random;

        for (int i = 0; i < targetCount; i++) {

            double angle = random.nextDouble() * Math.PI * 2;
            double dist = Math.sqrt(random.nextDouble()) * radius;

            double x = center.x + Math.cos(angle) * dist;
            double z = center.z + Math.sin(angle) * dist;
            double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    Mth.floor(x), Mth.floor(z));

            Entity entity = type.create(level);
            if (entity == null) continue;

            entity.moveTo(x, y, z, random.nextFloat() * 360F, 0);

            // 🔥 NoAI
            if (entity instanceof Mob mob) {
                mob.setNoAi(true);
            }

            level.addFreshEntity(entity);
            spawned++;
        }

        int finalSpawned = spawned;
        source.sendSuccess(
                () -> Component.literal(
                        "Spawned " + finalSpawned + " " +
                                EntityType.getKey(type) +
                                " (density=" + density + ")"
                ),
                true
        );

        return spawned;
    }
}