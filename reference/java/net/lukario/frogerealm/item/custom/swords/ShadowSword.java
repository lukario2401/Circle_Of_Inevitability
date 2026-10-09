package net.lukario.frogerealm.item.custom.swords;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

public class ShadowSword extends SwordItem {

    public ShadowSword(Tier tier, Item.Properties properties) {
        super(tier, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // --- DASH ---
            var look = player.getLookAngle();
            double dashStrength = 2;

            player.push(
                    look.x * dashStrength,
                    0.15,
                    look.z * dashStrength
            );
            player.hurtMarked = true;

            // --- SPEED CHECK ---
            var velocity = player.getDeltaMovement();
            double horizontalSpeed = Math.sqrt(
                    velocity.x * velocity.x +
                            velocity.z * velocity.z
            );

            double speedThreshold = 0.4; // tweak this

            if (horizontalSpeed >= speedThreshold) {
                // --- AOE DAMAGE ---
                level.getEntitiesOfClass(
                        net.minecraft.world.entity.LivingEntity.class,
                        player.getBoundingBox().inflate(2),
                        entity -> entity != player
                ).forEach(entity -> {
                    entity.hurt(
                            level.damageSources().playerAttack(player),
                            14.0F
                    );
                });
            }

            player.getCooldowns().addCooldown(this, 20);
        }

        return InteractionResultHolder.success(stack);
    }

    @Override
    public int getEnchantmentValue() {
        // Return the enchantability level.
        // Iron = 14, Diamond = 10, Netherite = 15, Gold = 22.
        return 24;
    }
}
