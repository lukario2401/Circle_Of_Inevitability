package net.lukario.frogerealm.item.custom.abilitys.star_burst;


import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
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
public class StarBurstHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;

        Player player = event.player;
        Level level = event.player.level();

        if (StarBurstKeybindClient.STAR_BURST_KEY.consumeClick()) {

            LivingEntity target = getTarget(player);

            if (target!=null){
                if (!level.isClientSide){
                    shoot(player, level, target);
                    shoot(player, level, target);
                    shoot(player, level, target);
                }
            }
        }
    }

    private static void shoot(Player player, Level level, LivingEntity target){

        Vec3 position = player.position().add(0, player.getBbHeight() * 0.5, 0);

        float offsetX = -5 + 1.0f + level.random.nextFloat() * 8.0f;
        float offsetY = -1 + 1.0f + level.random.nextFloat() * 4;
        float offsetZ = -5 + 1.0f + level.random.nextFloat() * 8.0f;

        double x = position.x;
        double y = position.y;
        double z = position.z;

        Vec3 location = new Vec3(x+offsetX,y+offsetY,z+offsetZ);

        twoEntityBeam(player, location, target,true);

    }

    private static void twoEntityBeam(Player player, Vec3 start, LivingEntity target, Boolean displayParticles) {
        Level level = player.level();

        if (!(level instanceof ServerLevel serverLevel)) return;

         // middle of source entity
        Vec3 end = target.position().add(0, target.getBbHeight() * 0.5, 0); // middle of target entity

        int particles = Mth.floor(start.distanceTo(end)*2);

        Vec3 diff = end.subtract(start);

        spawnParticles(level, start, 25, 1.1, 0,0,0, ParticleTypes.SOUL_FIRE_FLAME);
        spawnParticles(level, start, 25, 0, 1.1,0,0, ParticleTypes.SOUL_FIRE_FLAME);
        spawnParticles(level, start, 25, 0, 0,1.1,0, ParticleTypes.SOUL_FIRE_FLAME);
        spawnParticles(level, start, 20, 0.3, 0.3,0.3,0, ParticleTypes.FLAME);

        for (float i = 0; i <= particles; i+=0.5) {
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

//            level.addParticle(ParticleTypes.SOUL, c.x, c.y, c.z, 0, 0, 0);
//            serverLevel.sendParticles(ParticleTypes.SOUL, c.x, c.y, c.z, 1, 0, 0, 0 ,0);
//            Vector3f color = new Vector3f(1f, 0f, 0f);
//            DustParticleOptions redDust = new DustParticleOptions(color, 2f);

//            serverLevel.sendParticles(redDust, c.x, c.y, c.z, 1, 0, 0, 0, 0);

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

    private static void spawnParticles(Level level, Vec3 pos, int amount,
                                       double xSpread, double ySpread, double zSpread,
                                       double speed, ParticleOptions particle) {

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    particle,
                    pos.x, pos.y, pos.z,
                    amount,
                    xSpread, ySpread, zSpread,
                    speed
            );
        }
    }




}
