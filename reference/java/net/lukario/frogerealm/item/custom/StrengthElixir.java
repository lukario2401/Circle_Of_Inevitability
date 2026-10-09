package net.lukario.frogerealm.item.custom;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

public class StrengthElixir extends Item {


    public StrengthElixir(Properties pProperties) {
        super(pProperties);
    }

    public static final FoodProperties STRENGTH_ELIXIR =
            new FoodProperties.Builder()
                    .alwaysEdible()
                    .nutrition(0)
                    .saturationModifier(0f)
                    .effect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 2), 1.0f)
                    .effect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200, 1), 1.0f)
                    .effect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 1), 1.0f)
                    .effect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 1), 1.0f)
                    .effect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 1), 1.0f)
                    .effect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 1200, 1), 1.0f)
                    .effect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200, 1), 1.0f)
                    .build();

    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.DRINK;
    }
}
