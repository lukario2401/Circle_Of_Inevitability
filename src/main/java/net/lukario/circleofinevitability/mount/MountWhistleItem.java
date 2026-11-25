package net.lukario.circleofinevitability.mount;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

final class MountWhistleItem extends Item {
    private final Supplier<EntityType<RidingMount>> mount;

    MountWhistleItem(Supplier<EntityType<RidingMount>> mount, Properties properties) {
        super(properties);
        this.mount = mount;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isPassenger()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            if (!RidingMount.summon(player, mount.get())) {
                player.sendOverlayMessage(Component.translatable("message.circle_of_inevitability.mount.no_room"));
                return InteractionResult.FAIL;
            }
            level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_FLUTE.value(), SoundSource.PLAYERS, 0.8F, 1.4F);
        }
        return InteractionResult.SUCCESS;
    }
}
