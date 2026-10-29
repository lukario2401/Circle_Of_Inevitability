package net.lukario.circleofinevitability.item;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.item.artifacts.SunBroochItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

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

    private ModItems() {}

    /** Properties with the item's id already set (the name is the one given to ITEMS.register). */
    private static Item.Properties properties(String name) {
        return new Item.Properties().setId(ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, name)));
    }

    public static void register(BusGroup eventBus) {
        ITEMS.register(eventBus);
    }
}
