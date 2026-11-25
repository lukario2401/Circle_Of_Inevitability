package net.lukario.circleofinevitability.item.armor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import java.util.List;

/** Ordinary equippable armor; all Minecraft durability, enchantment and smithing behavior is retained. */
public class TentacleArmorItem extends Item {
    private final String setId;
    private final EquipmentSlot slot;
    public TentacleArmorItem(Properties properties, ArmorMaterial material, ArmorType type, String setId) {
        super(properties.humanoidArmor(material, type).fireResistant()
                .component(DataComponents.LORE, new ItemLore(List.of(
                        Component.translatable("tooltip.circle_of_inevitability.inevitability." +
                                (type == ArmorType.CHESTPLATE ? "tentacles" : "set")).withStyle(ChatFormatting.DARK_PURPLE),
                        Component.translatable("tooltip.circle_of_inevitability.inevitability.full_set").withStyle(ChatFormatting.GRAY)))));
        this.setId = setId;
        this.slot = type.getSlot();
    }
    public String setId() { return setId; }
    public EquipmentSlot slot() { return slot; }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.lukario.circleofinevitability.client.armor.InevitabilityArmorExtensions());
    }
}
