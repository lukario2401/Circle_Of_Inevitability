package net.lukario.frogerealm.item.custom;

import net.lukario.frogerealm.item.ModItems;
import net.minecraft.world.food.FoodProperties;

import java.util.function.Supplier;

public class ModFoodProperties {
    public static final FoodProperties DIAMOND_CARROT =
            new FoodProperties.Builder()
                    .alwaysEdible()
                    .nutrition(1)
                    .saturationModifier(1.0f)
                    .build();

}
