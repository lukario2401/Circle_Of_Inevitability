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

/** Ordinary equippable armor for the mod's 3D sets (Inevitability, Unshadowed, Gray Fog, Death); all Minecraft durability, enchantment and smithing behavior is retained. */
public class TentacleArmorItem extends Item {
    private final String setId;
    private final EquipmentSlot slot;
    public TentacleArmorItem(Properties properties, ArmorMaterial material, ArmorType type, String setId) {
        super(properties.humanoidArmor(material, type).fireResistant()
                .component(DataComponents.LORE, new ItemLore(lore(setId, type))));
        this.setId = setId;
        this.slot = type.getSlot();
    }
    /** The power line, then (for Death) the curse every piece carries, then the full-set line. */
    private static List<Component> lore(String setId, ArmorType type) {
        String prefix = "tooltip.circle_of_inevitability." + setId + ".";
        Component power = Component.translatable(powerKey(setId, type)).withStyle(color(setId));
        Component full = Component.translatable(prefix + "full_set").withStyle(ChatFormatting.GRAY);
        if (DeathArmor.SET.equals(setId))
            return List.of(power, Component.translatable(prefix + "curse").withStyle(ChatFormatting.DARK_RED), full);
        return List.of(power, full);
    }
    /** The Gray Fog set gives every piece its own power, Death the same Reaping on every piece; the other sets describe the chestplate's strikes. */
    private static String powerKey(String setId, ArmorType type) {
        String prefix = "tooltip.circle_of_inevitability." + setId + ".";
        if (GrayFogArmor.SET.equals(setId)) return prefix + switch (type) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chestplate";
            case LEGGINGS -> "leggings";
            default -> "boots";
        };
        return prefix + (type == ArmorType.CHESTPLATE || DeathArmor.SET.equals(setId) ? "power" : "set");
    }
    private static ChatFormatting color(String setId) {
        return switch (setId) {
            case UnshadowedArmor.SET -> ChatFormatting.GOLD;
            case GrayFogArmor.SET -> ChatFormatting.DARK_AQUA;
            case DeathArmor.SET -> ChatFormatting.DARK_GREEN;
            default -> ChatFormatting.DARK_PURPLE;
        };
    }
    public String setId() { return setId; }
    public EquipmentSlot slot() { return slot; }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(net.lukario.circleofinevitability.client.armor.ArmorModelExtensions.forSet(this::setId));
    }
}
