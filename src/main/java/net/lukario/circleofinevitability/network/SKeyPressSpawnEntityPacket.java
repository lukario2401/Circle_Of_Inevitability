package net.lukario.circleofinevitability.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class SKeyPressSpawnEntityPacket {
    public SKeyPressSpawnEntityPacket() {}
    public SKeyPressSpawnEntityPacket(FriendlyByteBuf buffer) {}
    public void encode(FriendlyByteBuf buffer) {}

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if(player == null)
            return;

        ServerLevel level = player.level();
        BlockPos position = player.blockPosition();

        List<Map.Entry<ResourceKey<EntityType<?>>, EntityType<?>>> entities =
                ForgeRegistries.ENTITY_TYPES.getEntries().stream().toList();

        EntityType<?> type = entities.get(ThreadLocalRandom.current().nextInt(entities.size())).getValue();
        type.spawn(level, position, EntitySpawnReason.MOB_SUMMONED);
    }
}