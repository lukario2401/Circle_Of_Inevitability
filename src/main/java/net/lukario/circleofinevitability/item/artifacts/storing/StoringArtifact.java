package net.lukario.circleofinevitability.item.artifacts.storing;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.AbilityCatalog;
import net.lukario.circleofinevitability.item.artifacts.SealedArtifactItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

/**
 * A tiered sealed artifact that stores abilities taken from the Beyonders its bearer kills, with downsides that grow
 * with its tier. Everything about one is declared in a single place; the shared item (StoringArtifactItem) and the
 * events (StoringArtifacts) do the rest. Creeping Hunger (item/artifacts/CreepingHunger) is the worked example:
 *
 *   public static final StoringArtifact MY_RELIC = StoringArtifact.create("my_relic", "My Relic")
 *           .tier(AbilitySlots.of(3, 7))                    // tier 1: three slots, Sequence 7 or weaker
 *           .tier(AbilitySlots.of(1, 5).and(3, 7))          // tier 2 ...
 *           .chooseOnKillFrom(2)                            // tier 2+: the bearer picks which ability to take
 *           .picksPerKill(1, 2)                             // ...and at tier 2 up to two from one kill
 *           .drawback(new FeedOrDieDrawback(1, 24000))      // reusable downsides, each from some tier
 *           .drawback(new CorruptionDrawback(2, ...));
 *
 *   // ModItems:  public static final List<RegistryObject<Item>> MY_RELIC = MyRelic.MY_RELIC.register(ITEMS,
 *   //                    name -> properties(name).rarity(Rarity.EPIC));      // my_relic_1, my_relic_2, ...
 *
 * Then add per tier items/<id>_<tier>.json, models/item/<id>_<tier>.json and a texture, the lang names
 * (item.circle_of_inevitability.<id>_<tier>, tooltip.circle_of_inevitability.<id>.power / .containment), and a damage
 * type data/circle_of_inevitability/damage_type/<id>.json with its death message (the drawbacks that hurt use it).
 * See docs/SEALED_ARTIFACTS.md.
 */
public final class StoringArtifact {

    /** What happens around the bearer; override only what you want to see or hear. */
    public interface Effects {
        default void devoured(ServerLevel level, ServerPlayer player, LivingEntity victim, AbilityCatalog.Power power) {}
        default void cast(ServerLevel level, ServerPlayer player, AbilityCatalog.Power power) {}
    }

    private static final List<StoringArtifact> ALL = new ArrayList<>();

    private final String id;
    private final String name;
    private final List<AbilitySlots> tiers = new ArrayList<>();
    private final List<ArtifactDrawback> drawbacks = new ArrayList<>();
    private int chooseFromTier = Integer.MAX_VALUE;
    private int[] picksPerKill = {1};
    private IntUnaryOperator cooldownBySequence = sequence -> 20 * (2 + 2 * (9 - sequence));   // 2 s .. 20 s
    private Effects effects = new Effects() {};

    private StoringArtifact(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /** 'id' is the item id prefix and the saved-state key; 'name' is used in messages. */
    public static StoringArtifact create(String id, String name) {
        StoringArtifact artifact = new StoringArtifact(id, name);
        ALL.add(artifact);
        return artifact;
    }

    /** Every declared storing artifact (for the events that have no item at hand). */
    public static List<StoringArtifact> all() {
        return List.copyOf(ALL);
    }

    // ---------- declaring

    /** The next tier (the first call is tier 1) and its slots. */
    public StoringArtifact tier(AbilitySlots slots) {
        tiers.add(slots);
        return this;
    }

    /** From this tier up the bearer chooses which of a kill's abilities to take; below it one is taken at random. */
    public StoringArtifact chooseOnKillFrom(int tier) {
        this.chooseFromTier = tier;
        return this;
    }

    /**
     * How many abilities the bearer may pick from one kill, per tier (only for tiers that choose; tiers that take at
     * random always take one). picksPerKill(1, 1, 1, 2, 3): tier IV up to two, tier V up to three.
     */
    public StoringArtifact picksPerKill(int... perTier) {
        this.picksPerKill = perTier.length == 0 ? new int[]{1} : perTier.clone();
        return this;
    }

    public StoringArtifact drawback(ArtifactDrawback drawback) {
        drawbacks.add(drawback);
        return this;
    }

    /** Ticks of cooldown after casting a stored ability of that Sequence (default 2 s for 9 up to 20 s for 0). */
    public StoringArtifact cooldown(IntUnaryOperator bySequence) {
        this.cooldownBySequence = bySequence;
        return this;
    }

    public StoringArtifact effects(Effects effects) {
        this.effects = effects;
        return this;
    }

    /** Registers one item per tier: <id>_1, <id>_2, ... in tier order. */
    public List<RegistryObject<Item>> register(DeferredRegister<Item> items, Function<String, Item.Properties> properties) {
        List<RegistryObject<Item>> registered = new ArrayList<>();
        for (int tier = 1; tier <= tiers.size(); tier++) {
            int t = tier;
            String itemName = itemName(t);
            registered.add(items.register(itemName, () -> new StoringArtifactItem(properties.apply(itemName), this, t)));
        }
        return List.copyOf(registered);
    }

    // ---------- reading

    public String id() { return id; }
    public String name() { return name; }
    public int tiers() { return tiers.size(); }
    public String itemName(int tier) { return id + "_" + tier; }
    public List<ArtifactDrawback> drawbacks() { return List.copyOf(drawbacks); }
    public Effects effects() { return effects; }

    public AbilitySlots slots(int tier) {
        return tiers.get(Math.clamp(tier - 1, 0, tiers.size() - 1));
    }

    public boolean choosesOnKill(int tier) {
        return tier >= chooseFromTier;
    }

    /** How many abilities one kill gives at this tier: picksPerKill when it chooses, else 1. */
    public int picksAt(int tier) {
        return choosesOnKill(tier) ? Math.max(1, Bearer.at(picksPerKill, tier)) : 1;
    }

    public int cooldownFor(int sequence) {
        return Math.max(0, cooldownBySequence.applyAsInt(sequence));
    }

    public int maxCooldown() {
        int most = 0;
        for (int sequence = 0; sequence <= 9; sequence++) most = Math.max(most, cooldownFor(sequence));
        return most;
    }

    /** The drawbacks that apply at this tier. */
    public List<ArtifactDrawback> drawbacksAt(int tier) {
        return drawbacks.stream().filter(d -> d.appliesAt(tier)).toList();
    }

    /** True if this artifact has a drawback of that class (for code that checks one kind across artifacts). */
    public <T extends ArtifactDrawback> List<T> drawbacksOf(Class<T> type) {
        return drawbacks.stream().filter(type::isInstance).map(type::cast).toList();
    }

    public ResourceKey<DamageType> damageType() {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, id));
    }

    /** Damage dealt by the artifact to its own bearer (data/circle_of_inevitability/damage_type/<id>.json). */
    public DamageSource damage(ServerLevel level) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(damageType()));
    }

    public Bearer bearer(ServerLevel level, ServerPlayer player, ItemStack carried) {
        int tier = !carried.isEmpty() && carried.getItem() instanceof StoringArtifactItem item ? item.tier() : 0;
        return new Bearer(this, level, player, carried, tier, SealedArtifactItem.state(player, id));
    }

    /** The tooltip of one tier. */
    public List<Component> lore(int tier) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.circle_of_inevitability." + id + ".power"));
        lines.add(Component.literal("Tier " + roman(tier) + " · " + slots(tier).describe()).withStyle(ChatFormatting.GOLD));
        int picks = picksAt(tier);
        lines.add(Component.literal(!choosesOnKill(tier)
                ? "Kill a Beyonder while carrying it: it devours one of their abilities at random."
                : picks == 1 ? "Kill a Beyonder while carrying it: choose one of their abilities to devour."
                : "Kill a Beyonder while carrying it: choose up to " + picks + " of their abilities to devour."));
        lines.add(Component.literal("Right-click: cast the selected ability. Sneak + right-click: choose or discard."));
        for (ArtifactDrawback drawback : drawbacksAt(tier))
            lines.add(Component.literal("• ").append(drawback.describe(tier)).withStyle(ChatFormatting.DARK_RED));
        lines.add(Component.translatable("tooltip.circle_of_inevitability." + id + ".containment"));
        return lines;
    }

    public static String roman(int number) {
        return switch (number) {
            case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; case 5 -> "V";
            case 6 -> "VI"; case 7 -> "VII"; case 8 -> "VIII"; case 9 -> "IX"; case 10 -> "X";
            default -> Integer.toString(number);
        };
    }
}
