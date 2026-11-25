package net.lukario.circleofinevitability.item.armor;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.equipment.*;
import java.util.Map;

/** Shared set identity and balance. Other sets can use TentacleCombat with their own attack profile. */
public final class InevitabilityArmor {
    public static final String SET = "inevitability";
    public static final ArmorMaterial MATERIAL = new ArmorMaterial(42,
            Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 8, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3),
            18, SoundEvents.ARMOR_EQUIP_NETHERITE, 3f, .1f,
            TagKey.create(Registries.ITEM, id("repairs_inevitability_armor")),
            ResourceKey.create(EquipmentAssets.ROOT_ID, id(SET)));
    public static final TentacleCombat.Profile CHEST = new TentacleCombat.Profile(3, 6, 30, 6, 12);
    public static final TentacleCombat.Profile FULL_SET = new TentacleCombat.Profile(4, 8, 20, 6, 12);
    public static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private InevitabilityArmor() {}
    public static Identifier id(String path) { return Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, path); }
    public static boolean wearing(LivingEntity entity, EquipmentSlot slot) {
        return entity.getItemBySlot(slot).getItem() instanceof TentacleArmorItem item
                && item.setId().equals(SET) && item.slot() == slot;
    }
    public static boolean fullSet(LivingEntity entity) {
        for (EquipmentSlot slot : SLOTS) if (!wearing(entity, slot)) return false;
        return true;
    }
}
