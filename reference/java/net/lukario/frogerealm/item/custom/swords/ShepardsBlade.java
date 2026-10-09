package net.lukario.frogerealm.item.custom.swords;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

public class ShepardsBlade extends SwordItem {

    public ShepardsBlade(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 24;
    }
}