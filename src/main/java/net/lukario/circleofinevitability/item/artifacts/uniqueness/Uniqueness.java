package net.lukario.circleofinevitability.item.artifacts.uniqueness;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.AbilityCatalog;
import net.lukario.circleofinevitability.combat.HudCounter;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.item.artifacts.SealedArtifactItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntUnaryOperator;

/**
 * A Uniqueness: a "tier 6" sealed artifact, the concentrated essence of a whole Pathway. It lends that Pathway's
 * abilities to whoever carries it (no Pathway, Sequence or spirituality needed), at the price of a corruption that
 * grows while it is carried. The corruption passes through levels (Low, Medium, High, Extreme...): the strongest
 * abilities wait for the higher levels, the downsides grow with them, and from one level on it can never be put away.
 * Putting it away (anywhere but on you) before then lets the corruption fade.
 *
 * Everything about one is declared in one place; UniquenessItem (the item) and Uniquenesses (the events) do the rest.
 * The Red Priest (item/artifacts/RedPriest) is the worked example:
 *
 *   public static final Uniqueness MY_ONE = Uniqueness.create("my_one", "My Uniqueness")
 *           .pathway("Wanderer")                                   // every catalog ability of this Pathway
 *           .power(UniquePower.of("my_one/extra", ...))            // plus any written for it
 *           .level("Low", 0, 0xC8A060).level("High", 50, 0xE04030) // levels from these corruption values (0..100)
 *           .gate(sequence -> sequence <= 2 ? 1 : 0)               // the level index an ability of that Sequence needs
 *           .noReturnFrom(1)                                       // from High it cannot be put away
 *           .corruption(1 / 8.0, sequence -> .05, .5)              // per second carried, per cast, recovered per second away
 *           .cooldown(4).damageTaken(.1f).damageDealt(2f).killHeal(1f)
 *           .weapon(10, -2.6f, 1.5f)                               // optional: a melee weapon too
 *           .drawback(new SomeDrawback(...));
 *
 *   // ModItems: public static final RegistryObject<Item> MY_ONE = MyOne.MY_ONE.register(ITEMS, properties("my_one").rarity(...));
 *
 * Then its items/<id>.json, models/item/<id>.json and texture, the lang names (item.circle_of_inevitability.<id>,
 * tooltip.circle_of_inevitability.<id>.power / .containment) and a damage type data/circle_of_inevitability/damage_type/<id>.json
 * with its death message (the downsides that hurt use it, and it is never softened by the uniqueness's own protection).
 * See docs/SEALED_ARTIFACTS.md.
 */
public final class Uniqueness {
    public static final int TIER = 6;
    public static final double MAX = 100;
    public static final String CORRUPTION = "corruption", BOUND = "bound", SELECTED = "selected",
            LEVEL_SEEN = "level_seen", GENERATION = "generation";

    /** A corruption level: its name, the corruption it starts at, and its colour (HUD, messages). */
    public record Level(String name, double from, int rgb) {
        public Component label() {
            return Component.literal(name).withStyle(s -> s.withColor(TextColor.fromRgb(rgb)));
        }
    }

    /** What happens around the wielder; override only what you want to see or hear. */
    public interface Effects {
        default void cast(ServerLevel level, ServerPlayer player, UniquePower power) {}

        /** The corruption rose into a new level (also the first time). */
        default void levelReached(ServerLevel level, ServerPlayer player, Level reached) {}
    }

    private static final List<Uniqueness> ALL = new ArrayList<>();

    private final String id;
    private final String name;
    private final List<String> pathways = new ArrayList<>();
    private final List<UniquePower> extraPowers = new ArrayList<>();
    private final List<Level> levels = new ArrayList<>();
    private final List<UniquenessDrawback> drawbacks = new ArrayList<>();
    private IntUnaryOperator gate = sequence -> 0;
    private int noReturnFrom = Integer.MAX_VALUE;
    private double gainPerSecond = 1 / 8.0;
    private IntToDoubleFunction perCast = sequence -> 0;
    private double recoverPerSecond = .5;
    private int cooldown = 4;
    private float damageTaken = 1f, damageDealt = 1f, killHeal = 0f;
    private float attackDamage = -1, attackSpeed, extraReach;
    private String hudIcon = "";
    private Effects effects = new Effects() {};
    private List<UniquePower> powers;
    private RegistryObject<Item> item;

    private Uniqueness(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /** 'id' is the item id and the saved-state key; 'name' is used in messages. */
    public static Uniqueness create(String id, String name) {
        Uniqueness uniqueness = new Uniqueness(id, name);
        ALL.add(uniqueness);
        return uniqueness;
    }

    public static List<Uniqueness> all() {
        return List.copyOf(ALL);
    }

    // ---------- declaring

    /** Lends every copyable ability of this Pathway (its keys and wheel entries). Can be called more than once. */
    public Uniqueness pathway(String pathway) { pathways.add(pathway); return this; }

    /** Lends one more ability, written for it. */
    public Uniqueness power(UniquePower power) { extraPowers.add(power); return this; }

    /** The next corruption level: from this corruption on (0..100). Declare them lowest first; the first starts at 0. */
    public Uniqueness level(String name, double from, int rgb) { levels.add(new Level(name, from, rgb)); return this; }

    /** The index of the level an ability of that Sequence needs (0 = usable from the start). */
    public Uniqueness gate(IntUnaryOperator levelForSequence) { this.gate = levelForSequence; return this; }

    /** From this level index on it is bound: it cannot be put away and its corruption no longer fades. */
    public Uniqueness noReturnFrom(int levelIndex) { this.noReturnFrom = levelIndex; return this; }

    /** Corruption gained each second carried, per cast by the ability's Sequence, and lost each second put away. */
    public Uniqueness corruption(double perSecondCarried, IntToDoubleFunction perCastBySequence, double recoverPerSecond) {
        this.gainPerSecond = perSecondCarried;
        this.perCast = perCastBySequence;
        this.recoverPerSecond = recoverPerSecond;
        return this;
    }

    /** Ticks between casts, whatever the ability (UniquePower.ownCooldown can add to it). */
    public Uniqueness cooldown(int ticks) { this.cooldown = Math.max(0, ticks); return this; }

    /** While carried: the share of damage the wielder still takes (0.1 = 90% less), except from the uniqueness itself. */
    public Uniqueness damageTaken(float share) { this.damageTaken = share; return this; }

    /** While carried: everything the wielder hits is hit this many times as hard. */
    public Uniqueness damageDealt(float multiplier) { this.damageDealt = multiplier; return this; }

    /** While carried: each kill heals this share of the victim's maximum health (1 = all of it). */
    public Uniqueness killHeal(float shareOfVictimMaxHealth) { this.killHeal = shareOfVictimMaxHealth; return this; }

    /** Makes the item a melee weapon in the main hand: attack damage (total), attack speed modifier, extra reach. */
    public Uniqueness weapon(float attackDamage, float attackSpeedModifier, float extraReach) {
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeedModifier;
        this.extraReach = extraReach;
        return this;
    }

    /** The picture next to the corruption counter on the HUD (textures/gui/<image>.png). */
    public Uniqueness hudIcon(String image) { this.hudIcon = image; return this; }

    public Uniqueness drawback(UniquenessDrawback drawback) { drawbacks.add(drawback); return this; }

    public Uniqueness effects(Effects effects) { this.effects = effects; return this; }

    /** Registers its item (one per uniqueness: there is only one of each in the world, in spirit). */
    public RegistryObject<Item> register(DeferredRegister<Item> items, Item.Properties properties) {
        if (attackDamage >= 0) {
            var modifiers = ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, attackDamage - 1,
                            AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ATTACK_SPEED, new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, attackSpeed,
                            AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            if (extraReach > 0) modifiers.add(Attributes.ENTITY_INTERACTION_RANGE, new AttributeModifier(
                    Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, id + "_reach"), extraReach,
                    AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);
            properties = properties.attributes(modifiers.build());
        }
        Item.Properties finalProperties = properties;
        item = items.register(id, () -> new UniquenessItem(finalProperties, this));
        return item;
    }

    // ---------- reading

    public String id() { return id; }

    /** Its item (after register). */
    public Item item() { return item.get(); }

    public String name() { return name; }
    public List<Level> levels() { return List.copyOf(levels); }
    public List<UniquenessDrawback> drawbacks() { return List.copyOf(drawbacks); }
    public Effects effects() { return effects; }
    public int cooldown() { return cooldown; }
    public float damageTaken() { return damageTaken; }
    public float damageDealt() { return damageDealt; }
    public float killHeal() { return killHeal; }
    public int noReturnFrom() { return noReturnFrom; }
    public double gainPerSecond() { return gainPerSecond; }
    public double recoverPerSecond() { return recoverPerSecond; }
    public double perCast(int sequence) { return Math.max(0, perCast.applyAsDouble(sequence)); }

    public Level level(int index) {
        return levels.isEmpty() ? new Level("Corrupted", 0, 0xC03030) : levels.get(Math.clamp(index, 0, levels.size() - 1));
    }

    /** The level index of this corruption. */
    public int levelAt(double corruption) {
        int index = 0;
        for (int i = 0; i < levels.size(); i++) if (corruption >= levels.get(i).from()) index = i;
        return index;
    }

    /** The level index an ability of this Sequence needs. */
    public int requiredLevel(int sequence) {
        return Math.clamp(gate.applyAsInt(sequence), 0, Math.max(0, levels.size() - 1));
    }

    /** Everything it lends: the Pathway's abilities (weakest Sequence first), then the ones written for it. */
    public List<UniquePower> powers() {
        if (powers == null) {
            List<UniquePower> list = new ArrayList<>();
            for (String pathway : pathways)
                AbilityCatalog.all().values().stream()
                        .filter(p -> p.cast().pathway().equals(pathway) && p.cast().copyable())
                        .sorted(Comparator.comparingInt((AbilityCatalog.Power p) -> -p.sequence()).thenComparing(AbilityCatalog.Power::name))
                        .forEach(p -> list.add(UniquePower.fromCatalog(p)));
            list.addAll(extraPowers);
            list.sort(Comparator.comparingInt((UniquePower p) -> -p.sequence()));
            powers = List.copyOf(list);
        }
        return powers;
    }

    public UniquePower power(String powerId) {
        for (UniquePower power : powers()) if (power.id().equals(powerId)) return power;
        return null;
    }

    // ---------- the wielder's corruption (saved per player, survives relogging)

    public CompoundTag state(ServerPlayer player) {
        return SealedArtifactItem.state(player, id);
    }

    public double corruption(ServerPlayer player) {
        return state(player).getDoubleOr(CORRUPTION, 0);
    }

    public void setCorruption(ServerPlayer player, double value) {
        state(player).putDouble(CORRUPTION, Math.clamp(value, 0, MAX));
    }

    public void addCorruption(ServerPlayer player, double amount) {
        setCorruption(player, corruption(player) + amount);
    }

    public boolean bound(ServerPlayer player) {
        return state(player).getBooleanOr(BOUND, false);
    }

    public Wielder wielder(ServerLevel level, ServerPlayer player, ItemStack carried) {
        double corruption = corruption(player);
        return new Wielder(this, level, player, carried, corruption, levelAt(corruption), state(player));
    }

    public ResourceKey<DamageType> damageType() {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, id));
    }

    /** Damage dealt by the uniqueness to its own wielder: never softened by its protection. */
    public DamageSource damage(ServerLevel level) {
        return new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(damageType()));
    }

    /** The selected ability's id (shared by every copy on this player), or the first ability. */
    public UniquePower selected(ServerPlayer player) {
        UniquePower power = power(state(player).getStringOr(SELECTED, ""));
        return power != null ? power : powers().isEmpty() ? null : powers().getFirst();
    }

    public void select(ServerPlayer player, UniquePower power) {
        state(player).putString(SELECTED, power.id());
    }

    // ---------- once a second (UniquenessItem.carriedTick)

    /**
     * Grows or fades the corruption, binds it at the level of no return, shows the HUD counter and runs every
     * downside's second(). 'carried' is the stack on the player, or EMPTY.
     */
    public void second(ServerLevel level, ServerPlayer player, ItemStack carried) {
        CompoundTag state = state(player);
        double corruption = corruption(player);
        boolean bound = bound(player);
        if (carried.isEmpty() && bound) carried = UniquenessItem.recall(this, level, player);
        if (!carried.isEmpty()) corruption += gainPerSecond;
        else corruption -= recoverPerSecond;
        corruption = Math.clamp(corruption, 0, MAX);
        state.putDouble(CORRUPTION, corruption);
        int now = levelAt(corruption);
        if (!carried.isEmpty() && !bound && now >= noReturnFrom) {
            state.putBoolean(BOUND, true);
            player.sendSystemMessage(Component.literal(name + " has taken root in you. You can no longer put it away.")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            Spells.sound(level, player.position(), SoundEvents.WITHER_SPAWN, .5f, .6f);
        }
        int seen = state.getIntOr(LEVEL_SEEN, -1);
        if (now != seen && (!carried.isEmpty() || corruption <= 0)) {
            state.putInt(LEVEL_SEEN, now);
            if (now > seen && !carried.isEmpty()) {
                player.sendSystemMessage(Component.literal(name + ": corruption is ").append(level(now).label())
                        .append(Component.literal(".")).withStyle(ChatFormatting.GRAY));
                effects.levelReached(level, player, level(now));
            }
        }
        showHud(player, corruption, !carried.isEmpty());
        Wielder wielder = new Wielder(this, level, player, carried, corruption, now, state);
        for (UniquenessDrawback drawback : drawbacks) {
            try {
                drawback.second(wielder);
            } catch (RuntimeException e) {
                CircleOfInevitability.LOGGER.error("A {} downside failed", id, e);
            }
            if (!player.isAlive()) return;
        }
    }

    /** The corruption counter next to the hotbar: "Corruption · High 63/100", in the level's colour. */
    public void showHud(ServerPlayer player, double corruption, boolean carried) {
        String key = id + "_corruption";
        if (!carried && corruption <= 0) {
            HudCounter.hide(player, key);
            return;
        }
        Level level = level(levelAt(corruption));
        HudCounter.show(player, key, hudIcon, level.name(), (int) Math.floor(corruption), (int) MAX, level.rgb());
    }

    /** Forgets this player's corruption (death, a test). */
    public void reset(ServerPlayer player) {
        CompoundTag state = state(player);
        state.putDouble(CORRUPTION, 0);
        state.putBoolean(BOUND, false);
        state.putInt(LEVEL_SEEN, -1);
        HudCounter.hide(player, id + "_corruption");
    }

    // ---------- tooltip

    public List<Component> lore() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("tooltip.circle_of_inevitability." + id + ".power"));
        lines.add(Component.literal("Tier VI · Uniqueness · every " + String.join(" and ", pathways) + " ability")
                .withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal("Right-click: cast the selected ability. Sneak + right-click: choose one."));
        StringBuilder gates = new StringBuilder();
        for (int i = 1; i < levels.size(); i++) {
            int index = i;
            List<Integer> sequences = new ArrayList<>();
            for (int sequence = 0; sequence <= 9; sequence++) if (requiredLevel(sequence) == index) sequences.add(sequence);
            if (sequences.isEmpty()) continue;
            if (!gates.isEmpty()) gates.append(", ");
            gates.append("Sequence ").append(sequences.size() == 1 ? sequences.getFirst()
                    : sequences.getFirst() + "-" + sequences.getLast()).append(" needs ").append(levels.get(i).name());
        }
        if (!gates.isEmpty()) lines.add(Component.literal(gates + ".").withStyle(ChatFormatting.RED));
        if (noReturnFrom < levels.size())
            lines.add(Component.literal("From " + levels.get(noReturnFrom).name() + " it can never be put away.")
                    .withStyle(ChatFormatting.DARK_RED));
        for (UniquenessDrawback drawback : drawbacks)
            lines.add(Component.literal("• ").append(drawback.describe()).withStyle(ChatFormatting.DARK_RED));
        lines.add(Component.translatable("tooltip.circle_of_inevitability." + id + ".containment"));
        return lines;
    }
}
