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

/** Client-only models, recreated after resource reload. Separate models keep deferred slot submissions isolated. */
public final class InevitabilityArmorExtensions implements IClientItemExtensions {
    private EntityModelSet bakedFor;
    private final Map<EquipmentSlot, InevitabilityArmorModel> models = new EnumMap<>(EquipmentSlot.class);
    @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntityRenderState state, ItemStack stack,
                                                           EquipmentSlot slot, HumanoidModel<?> original) {
        EntityModelSet current = Minecraft.getInstance().getEntityModels();
        if (bakedFor != current) { models.clear(); bakedFor = current; }
        return models.computeIfAbsent(slot, key -> new InevitabilityArmorModel(
                InevitabilityArmorModel.createLayer(key).bakeRoot(), key));
    }
}
