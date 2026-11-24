package net.lukario.circleofinevitability.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

record RitualTask(Kind kind, String description, int count, int reward, int cooldown,
                 Optional<Stat<?>> statistic, List<EntityType<?>> targets, Optional<Holder<Item>> item) {
    static final int RADIUS = 8;
    enum Kind implements StringRepresentable {
        STATISTIC, PROCESSION, ASSEMBLY, DELIVERY;
        public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }

    private static final Codec<Stat<?>> STAT = ObjectiveCriteria.CODEC.comapFlatMap(
            criterion -> criterion instanceof Stat<?> stat ? DataResult.success(stat)
                    : DataResult.error(() -> "A ritual task requires a player statistic"), stat -> stat);
    static final Codec<RitualTask> CODEC = RecordCodecBuilder.<RitualTask>create(i -> i.group(
            StringRepresentable.fromEnum(Kind::values).optionalFieldOf("kind", Kind.STATISTIC).forGetter(RitualTask::kind),
            Codec.STRING.fieldOf("description").forGetter(RitualTask::description),
            Codec.intRange(1, 1000000).fieldOf("count").forGetter(RitualTask::count),
            Codec.intRange(1, 30).optionalFieldOf("reward", 10).forGetter(RitualTask::reward),
            Codec.intRange(30, 86400).optionalFieldOf("cooldown_seconds", 180).forGetter(RitualTask::cooldown),
            STAT.optionalFieldOf("statistic").forGetter(RitualTask::statistic),
            EntityType.CODEC.listOf().optionalFieldOf("targets", List.of()).forGetter(RitualTask::targets),
            Item.CODEC.optionalFieldOf("item").forGetter(RitualTask::item)
    ).apply(i, RitualTask::new)).validate(task -> switch (task.kind) {
        case STATISTIC -> task.statistic.isPresent() ? DataResult.success(task) : DataResult.error(() -> "Missing statistic");
        case PROCESSION, ASSEMBLY -> !task.targets.isEmpty() ? DataResult.success(task) : DataResult.error(() -> "Missing targets");
        case DELIVERY -> task.item.isPresent() ? DataResult.success(task) : DataResult.error(() -> "Missing delivery item");
    });

    boolean accepts(LivingEntity entity) {
        return targets.contains(entity.getType());
    }

    int total(ServerPlayer player) {
        return statistic.map(stat -> player.getStats().getValue(stat)).orElse(0);
    }

    int progress(ServerPlayer player, BlockPos altar, int baseline, int kills) {
        return switch (kind) {
            case STATISTIC -> Math.clamp((long) total(player) - baseline, 0, count);
            case PROCESSION -> Math.min(kills, count);
            case ASSEMBLY -> {
                Vec3 center = Vec3.atCenterOf(altar);
                var present = player.level().getEntitiesOfClass(LivingEntity.class, new AABB(altar).inflate(RADIUS),
                        entity -> entity.isAlive() && accepts(entity) && entity.distanceToSqr(center) <= RADIUS * RADIUS);
                yield Math.min(present.size(), count);
            }
            case DELIVERY -> Math.min(RitualDeity.stock(player, item.orElseThrow()), count);
        };
    }
}
