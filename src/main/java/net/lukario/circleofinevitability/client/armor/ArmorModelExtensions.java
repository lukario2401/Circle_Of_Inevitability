package net.lukario.circleofinevitability.client.armor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/** Client-only models, recreated after resource reload. Separate models keep deferred slot submissions isolated. */
public final class ArmorModelExtensions implements IClientItemExtensions {
    private final Function<EquipmentSlot, HumanoidModel<?>> factory;
    private EntityModelSet bakedFor;
    private final Map<EquipmentSlot, HumanoidModel<?>> models = new EnumMap<>(EquipmentSlot.class);
    public ArmorModelExtensions(Function<EquipmentSlot, HumanoidModel<?>> factory) { this.factory = factory; }

    public static ArmorModelExtensions forSet(String setId) {
        return new ArmorModelExtensions(switch (setId) {
            case net.lukario.circleofinevitability.item.armor.UnshadowedArmor.SET ->
                    slot -> new UnshadowedArmorModel(UnshadowedArmorModel.createLayer(slot).bakeRoot(), slot);
            case net.lukario.circleofinevitability.item.armor.GrayFogArmor.SET ->
                    slot -> new GrayFogArmorModel(GrayFogArmorModel.createLayer(slot).bakeRoot(), slot);
            default -> slot -> new InevitabilityArmorModel(InevitabilityArmorModel.createLayer(slot).bakeRoot(), slot);
        });
    }
    @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntityRenderState state, ItemStack stack,
                                                           EquipmentSlot slot, HumanoidModel<?> original) {
        EntityModelSet current = Minecraft.getInstance().getEntityModels();
        if (bakedFor != current) { models.clear(); bakedFor = current; }
        return models.computeIfAbsent(slot, factory);
    }
}
