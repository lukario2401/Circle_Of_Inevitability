package net.lukario.circleofinevitability.item;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.item.artifacts.SunBroochItem;
import net.lukario.circleofinevitability.ritual.RitualTableBlock;
import net.lukario.circleofinevitability.ritual.SacrificialKnifeItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemLore;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * The mod's items. Since 1.21.2 every item needs its id in its Properties (setId), so each one is made through
 * properties(name) below. A new item: a line here, its model in assets/circle_of_inevitability/items/ and
 * models/item/, its texture in textures/item/, and its name in lang/en_us.json.
 */
public final class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, CircleOfInevitability.MOD_ID);

    /** One item for every Pathway and Sequence; which one it is lives in ModDataComponents.CHARACTERISTIC. */
    public static final RegistryObject<Item> BEYONDER_CHARACTERISTIC = ITEMS.register("beyonder_characteristic",
            () -> new BeyonderCharacteristicItem(properties("beyonder_characteristic")
                    .stacksTo(BeyonderCharacteristicItem.STACK_SIZE)
                    .rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> SUN_BROOCH = ITEMS.register("sun_brooch",
            () -> new SunBroochItem(properties("sun_brooch")
                    .rarity(Rarity.RARE)));

    public static final RegistryObject<Item> INEVITABILITY_CORE = ITEMS.register("inevitability_core",
            () -> new Item(properties("inevitability_core").rarity(Rarity.EPIC).fireResistant()));
    public static final RegistryObject<Item> INEVITABILITY_HELMET = armor("inevitability_helmet", net.minecraft.world.item.equipment.ArmorType.HELMET);
    public static final RegistryObject<Item> INEVITABILITY_CHESTPLATE = armor("inevitability_chestplate", net.minecraft.world.item.equipment.ArmorType.CHESTPLATE);
    public static final RegistryObject<Item> INEVITABILITY_LEGGINGS = armor("inevitability_leggings", net.minecraft.world.item.equipment.ArmorType.LEGGINGS);
    public static final RegistryObject<Item> INEVITABILITY_BOOTS = armor("inevitability_boots", net.minecraft.world.item.equipment.ArmorType.BOOTS);

    private static RegistryObject<Item> armor(String name, net.minecraft.world.item.equipment.ArmorType type) {
        return ITEMS.register(name, () -> new net.lukario.circleofinevitability.item.armor.TentacleArmorItem(
                properties(name).rarity(Rarity.EPIC), net.lukario.circleofinevitability.item.armor.InevitabilityArmor.MATERIAL,
                type, net.lukario.circleofinevitability.item.armor.InevitabilityArmor.SET));
    }

    public static final RegistryObject<BlockItem> RITUAL_TABLE = ITEMS.register("ritual_table", () ->
            new BlockItem(RitualTableBlock.TABLE.get(), properties("ritual_table").useBlockDescriptionPrefix()));

    public static final RegistryObject<Item> SACRIFICIAL_KNIFE = ITEMS.register("sacrificial_knife", () ->
            new SacrificialKnifeItem(properties("sacrificial_knife").sword(ToolMaterial.IRON, 1.0F, -2.2F).durability(128)
                    .component(DataComponents.LORE, new ItemLore(List.of(
                            Component.translatable("tooltip.circle_of_inevitability.sacrificial_knife"))))));

    public static final RegistryObject<Item> MOB_BLOOD_VIAL = bloodVial("mob_blood_vial");
    public static final RegistryObject<Item> RITUAL_BLOOD_VIAL = bloodVial("ritual_blood_vial");

    private static RegistryObject<Item> bloodVial(String name) {
        return ITEMS.register(name, () -> new Item(properties(name).stacksTo(16).craftRemainder(Items.GLASS_BOTTLE)
                .component(DataComponents.LORE, new ItemLore(List.of(
                        Component.translatable("tooltip.circle_of_inevitability." + name))))));
    }

    private ModItems() {}

    /** Properties with the item's id already set (the name is the one given to ITEMS.register). */
    public static Item.Properties properties(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, name)));
    }

    public static void register(BusGroup eventBus) {
        ITEMS.register(eventBus);
    }
}
