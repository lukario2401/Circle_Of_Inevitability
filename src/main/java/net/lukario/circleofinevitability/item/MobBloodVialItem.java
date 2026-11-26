package net.lukario.circleofinevitability.item;

import net.lukario.circleofinevitability.item.artifacts.storing.BloodThirstDrawback;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Mob blood in a bottle (collected by killing a mob with an empty bottle in the off hand, ritual/Worship). Still a
 * crafting ingredient; right-click drinks it, but only when an artifact's blood thirst asks for it
 * (item/artifacts/storing/BloodThirstDrawback), so a vial is never wasted.
 */
public class MobBloodVialItem extends Item {
    public MobBloodVialItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer drinker)) return InteractionResult.PASS;
        if (!BloodThirstDrawback.quenchAll(drinker)) return InteractionResult.PASS;
        ItemStack vial = player.getItemInHand(hand);
        player.setItemInHand(hand, ItemUtils.createFilledResult(vial, player, new ItemStack(Items.GLASS_BOTTLE)));
        server.sendParticles(new DustParticleOptions(0x8A0A12, 1f), player.getX(), player.getEyeY() - .2, player.getZ(),
                12, .2, .15, .2, 0);
        player.sendOverlayMessage(Component.literal("You drink the blood. The thirst recedes."));
        return InteractionResult.SUCCESS;
    }
}
