package net.lukario.frogerealm.item.custom.ranged;

import net.lukario.frogerealm.item.custom.detection.ClickState;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;

public class Terminator extends Item {

    public Terminator(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;

        if (ClickState.leftClickPressed){
            if (!player.level().isClientSide && !player.getCooldowns().isOnCooldown(this)) {

                Level level = player.level();

                player.getCooldowns().addCooldown(this, 1);

                terminatorShootLeftClick(level, player, -7.5d);
                terminatorShootLeftClick(level, player, 0d);
                terminatorShootLeftClick(level, player, 7.5d);

//                    player.sendSystemMessage(Component.literal("Trig: " + ClickState.leftClickPressed));
            }
            if (player.level().isClientSide && !player.getCooldowns().isOnCooldown(this)){

                Vec3 l = player.position();
                player.level().playLocalSound(l.x,l.y,l.z,SoundEvents.ARROW_SHOOT,SoundSource.PLAYERS,1,1,false);

            }
        }

        return true;
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        if (!level.isClientSide) {

            if (ClickState.rightClickPressed && !player.getCooldowns().isOnCooldown(this)){

                terminatorShootRightClick(level, player, 7.5d);
                terminatorShootRightClick(level, player, 0d);
                terminatorShootRightClick(level, player, -7.5d);

//                player.sendSystemMessage(Component.literal("Trig Left: " + ClickState.leftClickPressed));
//                player.sendSystemMessage(Component.literal("Trig Right: " + ClickState.leftClickPressed));

                player.getCooldowns().addCooldown(this, 1);
            }

            player.getPersistentData().putBoolean("terminator_right_click", true);

        }
        if (level.isClientSide && !player.getCooldowns().isOnCooldown(this)){
            Vec3 l = player.position();
            level.playLocalSound(l.x,l.y,l.z,SoundEvents.CROSSBOW_SHOOT,SoundSource.PLAYERS,1,1,false);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    private static void terminatorShootRightClick(Level level, Player player, Double offset){
        if (!(level instanceof ServerLevel serverLevel)) return;

        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        double distanceToTravel = 48.0;

        double yaw = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
        double pitch = (float)Math.toDegrees(Math.asin(-direction.y));

        yaw += offset;

        float fYaw = (float) yaw;
        float fPitch = (float) pitch;

        direction = Vec3.directionFromRotation(fPitch, fYaw);

        Vec3 step = direction.scale(0.5);

        Vec3 c = start;
        for (double distance = 0; distance <= distanceToTravel; distance +=0.5 ){

            Vector3f color = new Vector3f(1f, 0f, 0f);
            DustParticleOptions redDust = new DustParticleOptions(color, 2f);

            serverLevel.sendParticles(redDust, c.x, c.y, c.z, 1, 0, 0, 0, 0);
//            serverLevel.sendParticles(ParticleTypes.SOUL, c.x, c.y, c.z, 1, 0, 0, 0, 0);

            List<LivingEntity> entities = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(c, c).inflate(0.25),
                    e -> e != player
            );

            for (LivingEntity entity : entities){

                entity.setHealth(entity.getHealth()-32.0f);
                entity.setLastHurtByPlayer(player);

                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
                serverLevel.playSound(null,c.x,c.y,c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS,8 ,1);

                player.playNotifySound(
                        SoundEvents.ARROW_HIT_PLAYER,
                        SoundSource.PLAYERS,
                        4f,
                        1f
                );
            }

            c = c.add(step);
        }
    }

    private static void terminatorShootLeftClick(Level level, Player player, Double offset){

        if (!(level instanceof ServerLevel serverLevel)) return;

        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        double distanceToTravel = 32.0;

        double yaw = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
        double pitch = (float)Math.toDegrees(Math.asin(-direction.y));

        yaw += offset;

        float fYaw = (float) yaw;
        float fPitch = (float) pitch;

        direction = Vec3.directionFromRotation(fPitch, fYaw);

        Vec3 step = direction.scale(0.5);

        Vec3 c = start;
        for (double distance = 0; distance <= distanceToTravel; distance +=0.5 ){

            BlockPos blockPos = new BlockPos(Mth.floor(c.x), Mth.floor(c.y), Mth.floor(c.z));
            BlockState blockState = level.getBlockState(blockPos);

            if (blockState.getBlock().defaultBlockState().isSolid()){
                break;
            }

            serverLevel.sendParticles(ParticleTypes.SOUL, c.x, c.y, c.z, 1, 0, 0, 0, 0);
//            player.sendSystemMessage(Component.literal("left triggered"));

            List<LivingEntity> entities = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(c, c).inflate(0.5),
                    e -> e != player
            );

            for (LivingEntity entity : entities){
                entity.hurt(level.damageSources().playerAttack(player),24.0f);

                //The Estuary Key
                entity.invulnerableTime = 0;
                //The key

                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
                serverLevel.playSound(null,c.x,c.y,c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.MASTER,8 ,1);

                player.playNotifySound(
                        SoundEvents.ARROW_HIT_PLAYER,
                        SoundSource.PLAYERS,
                        4f,
                        1f
                );
            }

            c = c.add(step);
            if (!entities.isEmpty()){
                break;
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {

        if (Screen.hasShiftDown()){
            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.6"));
            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.7"));
            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.5"));

            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.13"));
            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.14"));

            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.5"));

            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.8"));
            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.9"));
            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.10"));
            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.11"));
            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.12"));

            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.5"));
            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.1"));
            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.2"));
            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.3"));
            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.4"));
            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.5"));
        }else{
            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.6"));
            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.7"));
            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.5"));
            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip"));
            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.2"));
            pTooltipComponents.add(Component.translatable("tooltip.forgerealmmod.terminator.tooltip.shift.5"));
        }

        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId(stack)).withStyle(ChatFormatting.LIGHT_PURPLE);
    }
}
