package net.lukario.circleofinevitability.ritual;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.stream.Stream;

public record RitualDeity(String name, Identifier icon, String domain, Holder<Item> offering, List<Offering> tributes,
                         RitualTask task, List<RitualTask> rotation, RitualBlessing blessing, boolean revival) {
    public static final ResourceKey<Registry<RitualDeity>> REGISTRY = ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "ritual_deity"));
    public static final Codec<RitualDeity> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(RitualDeity::name),
            Identifier.CODEC.fieldOf("icon").forGetter(RitualDeity::icon),
            Codec.STRING.fieldOf("domain").forGetter(RitualDeity::domain),
            Item.CODEC.fieldOf("offering").forGetter(RitualDeity::offering),
            Offering.CODEC.listOf().optionalFieldOf("tributes", List.of()).forGetter(RitualDeity::tributes),
            RitualTask.CODEC.fieldOf("task").forGetter(RitualDeity::task),
            RitualTask.CODEC.listOf().optionalFieldOf("rotation", List.of()).forGetter(RitualDeity::rotation),
            RitualBlessing.CODEC.fieldOf("blessing").forGetter(RitualDeity::blessing),
            Codec.BOOL.optionalFieldOf("revival", false).forGetter(RitualDeity::revival)
    ).apply(i, RitualDeity::new));

    record Offering(Holder<Item> item, int count, int points) {
        static final Codec<Offering> CODEC = RecordCodecBuilder.create(i -> i.group(
                Item.CODEC.fieldOf("item").forGetter(Offering::item),
                Codec.intRange(1, 64).fieldOf("count").forGetter(Offering::count),
                Codec.intRange(1, 30).fieldOf("points").forGetter(Offering::points)
        ).apply(i, Offering::new));
        String label() { return count + " × " + item.value().getDefaultInstance().getHoverName().getString(); }
    }

    List<Offering> offerings() {
        return Stream.concat(Stream.of(new Offering(offering, 1, 2)), tributes.stream()).toList();
    }

    RitualTask taskAt(int index) {
        int slot = Math.floorMod(index, rotation.size() + 1);
        return slot == 0 ? task : rotation.get(slot - 1);
    }

    static int stock(ServerPlayer player, Holder<Item> item) {
        return player.getInventory().getNonEquipmentItems().stream()
                .filter(stack -> stack.is(item)).mapToInt(stack -> stack.getCount()).sum();
    }

    static boolean pay(ServerPlayer player, Holder<Item> item, int count) {
        if (stock(player, item) < count) return false;
        for (var stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.is(item)) continue;
            int take = Math.min(stack.getCount(), count);
            stack.shrink(take);
            count -= take;
            if (count == 0) break;
        }
        player.getInventory().setChanged();
        return true;
    }
}
