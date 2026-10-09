package net.lukario.frogerealm.item.tiers;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

public class ModTiers {

    public static final Tier SHEPARDS_TIER = new Tier() {
        @Override
        public int getUses() {
            return 5; // Forces the durability to be exactly 5
        }

        @Override
        public float getSpeed() {
            return Tiers.GOLD.getSpeed();
        }

        @Override
        public float getAttackDamageBonus() {
            return Tiers.GOLD.getAttackDamageBonus();
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return Tiers.GOLD.getIncorrectBlocksForDrops();
        }

        @Override
        public int getEnchantmentValue() {
            return 24;
        }

        @Override
        public Ingredient getRepairIngredient() {
            // Currently uses Gold Ingots. You can change this later!
            return Tiers.GOLD.getRepairIngredient();
        }
    };

    // You can add more custom tiers down here later (e.g., ABYSSAL_TIER)
}