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

    /**
     * Forge asks for client extensions from inside Item's constructor, before a subclass has set its fields, so the
     * set id is read lazily, when the first model is baked, never at registration.
     */
    public static ArmorModelExtensions forSet(java.util.function.Supplier<String> setId) {
        return new ArmorModelExtensions(slot -> factory(setId.get()).apply(slot));
    }
    private static Function<EquipmentSlot, HumanoidModel<?>> factory(String setId) {
        if (net.lukario.circleofinevitability.item.armor.UnshadowedArmor.SET.equals(setId))
            return slot -> new UnshadowedArmorModel(UnshadowedArmorModel.createLayer(slot).bakeRoot(), slot);
        if (net.lukario.circleofinevitability.item.armor.GrayFogArmor.SET.equals(setId))
            return slot -> new GrayFogArmorModel(GrayFogArmorModel.createLayer(slot).bakeRoot(), slot);
        return slot -> new InevitabilityArmorModel(InevitabilityArmorModel.createLayer(slot).bakeRoot(), slot);
    }
    @Override public HumanoidModel<?> getHumanoidArmorModel(LivingEntityRenderState state, ItemStack stack,
                                                           EquipmentSlot slot, HumanoidModel<?> original) {
        EntityModelSet current = Minecraft.getInstance().getEntityModels();
        if (bakedFor != current) { models.clear(); bakedFor = current; }
        return models.computeIfAbsent(slot, factory);
    }
}
