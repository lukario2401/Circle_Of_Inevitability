package net.lukario.frogerealm.item.custom.abilitys;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.ForgePacketHandler;
import org.joml.Vector3f;
import java.util.List;

@Mod.EventBusSubscriber(modid = "forgerealmmod", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DashHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;

        Player player = event.player;
        Level level = event.player.level();

        if (DashKeybindClient.DASH_KEY.isDown()) {

            LivingEntity target = getTarget(player);

            if (target!=null){

                List<LivingEntity> listOfTargets = detectEnemies(target);

                if (!level.isClientSide){
                    shoot(player, level, target, listOfTargets);
                }
            }
        }
    }

    private static void shoot(Player player, Level level, LivingEntity target, List<LivingEntity> livingEntityList){

        twoEntityBeam(player, player, target, false);

        for (LivingEntity additionalTargets : livingEntityList){
            twoEntityBeam(player, target, additionalTargets, true);
        }
    }

    private static void twoEntityBeam(Player player, LivingEntity entity, LivingEntity target, Boolean displayParticles) {
        Level level = entity.level();

        if (!(level instanceof ServerLevel serverLevel)) return;

        Vec3 start = entity.position().add(0, entity.getBbHeight() * 0.5, 0); // middle of source entity
        Vec3 end = target.position().add(0, target.getBbHeight() * 0.5, 0); // middle of target entity

        int particles = Mth.floor(start.distanceTo(end)*2);

        Vec3 diff = end.subtract(start);

        for (int i = 0; i <= particles; i++) {
            double factor = i / (double) particles;
            Vec3 point = start.add(diff.scale(factor));

            if (displayParticles){
                serverLevel.sendParticles(ParticleTypes.SOUL, point.x, point.y, point.z, 1, 0, 0, 0 ,0);
            }

            List<LivingEntity> entities = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(point, point).inflate(1),
                    e -> e != target
            );

            for (LivingEntity damageable : entities){
                if (damageable != player && damageable != target){
                    damageable.hurt(level.damageSources().playerAttack(player),24.0f);
                }
            }
        }
        target.hurt(level.damageSources().genericKill(), 32.0f);
    }


    private static List<LivingEntity> detectEnemies(LivingEntity livingEntity){

        Level level = livingEntity.level();
        Vec3 position = livingEntity.position();

        return level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(position, position).inflate(3)
        );
    }

    private static LivingEntity getTarget(Player player){
        Level level = player.level();

        if (!(level instanceof ServerLevel serverLevel)) return null;


        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        double distanceToTravel = 32.0;

        Vec3 step = direction.scale(0.5);

        Vec3 c = start;
        for (double distance = 0; distance <= distanceToTravel; distance +=0.5 ) {

            BlockPos blockPos = new BlockPos(Mth.floor(c.x), Mth.floor(c.y), Mth.floor(c.z));
            BlockState blockState = level.getBlockState(blockPos);

            if (blockState.getBlock().defaultBlockState().isSolid()) {
                break;
            }

            Vector3f color = new Vector3f(1f, 0f, 0f);
            DustParticleOptions redDust = new DustParticleOptions(color, 2f);

            serverLevel.sendParticles(redDust, c.x, c.y, c.z, 1, 0, 0, 0, 0);

            List<LivingEntity> entities = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(c, c).inflate(0.5),
                    e -> e != player
            );

            for (LivingEntity livingEntity : entities){
                return livingEntity;
            }

            c = c.add(step);
        }
        return null;
    }
}
