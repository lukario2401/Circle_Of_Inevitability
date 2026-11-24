package net.lukario.circleofinevitability.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public record RitualDeity(String name, Identifier icon, String domain, Holder<Item> offering, Task task, Blessing blessing) {
    public static final ResourceKey<Registry<RitualDeity>> REGISTRY = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "ritual_deity"));

    public static final Codec<RitualDeity> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(RitualDeity::name),
            Identifier.CODEC.fieldOf("icon").forGetter(RitualDeity::icon),
            Codec.STRING.fieldOf("domain").forGetter(RitualDeity::domain),
            Item.CODEC.fieldOf("offering").forGetter(RitualDeity::offering),
            Task.CODEC.fieldOf("task").forGetter(RitualDeity::task),
            Blessing.CODEC.fieldOf("blessing").forGetter(RitualDeity::blessing)
    ).apply(i, RitualDeity::new));

    public record Task(String description, Stat<?> statistic, int count) {
        private static final Codec<Stat<?>> STAT = ObjectiveCriteria.CODEC.comapFlatMap(
                criterion -> criterion instanceof Stat<?> stat ? DataResult.success(stat)
                        : DataResult.error(() -> "A ritual task requires a player statistic"), stat -> stat);
        static final Codec<Task> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("description").forGetter(Task::description),
                STAT.fieldOf("statistic").forGetter(Task::statistic),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("count").forGetter(Task::count)
        ).apply(i, Task::new));

        int total(ServerPlayer player) {
            return player.getStats().getValue(statistic);
        }
    }

    public record Blessing(String name, String perRank, Holder<Attribute> attribute,
                           double amount, AttributeModifier.Operation operation) {
        static final Codec<Blessing> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(Blessing::name),
                Codec.STRING.fieldOf("per_rank").forGetter(Blessing::perRank),
                Attribute.CODEC.fieldOf("attribute").forGetter(Blessing::attribute),
                Codec.DOUBLE.fieldOf("amount").forGetter(Blessing::amount),
                AttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(Blessing::operation)
        ).apply(i, Blessing::new));
    }
}
