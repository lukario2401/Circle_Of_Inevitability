package net.lukario.frogerealm.item.custom.ranged;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

public class LaserStaff extends Item {

    public LaserStaff(Properties pProperties) {
        super(pProperties);
    }

    private static final int FIRE_COOLDOWN = 1; // ticks (0.5 sec)

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player) {

            if (!player.level().isClientSide) {

                // 🔒 prevent multiple fires
                if (!player.getCooldowns().isOnCooldown(this)) {
                    Level level = player.level();

                    test(level, player);
                    player.getCooldowns().addCooldown(this, FIRE_COOLDOWN);
                }
            }

            return true; // cancel default swing
        }
        return false;
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        laserRay(level, player);

        return InteractionResultHolder.success(stack);
    }

    public static void laserRay(Level level, LivingEntity shooter) {
        Vec3 start = shooter.getEyePosition();
        Vec3 dir = shooter.getLookAngle();
        double maxDistance = 30.0;

        Vec3 end = start.add(dir.scale(maxDistance));

        /* ---------- BLOCK RAY ---------- */
        BlockHitResult blockHit = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                shooter
        ));

        double maxEntityDistance = maxDistance;
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            maxEntityDistance = start.distanceTo(blockHit.getLocation());
        }

        /* ---------- ENTITY RAY ---------- */
        EntityHitResult entityHit = null;
        double closest = maxEntityDistance;

        for (Entity entity : level.getEntities(
                shooter,
                shooter.getBoundingBox()
                        .expandTowards(dir.scale(maxEntityDistance))
                        .inflate(1.0),
                e -> e instanceof LivingEntity && e != shooter
        )) {
            AABB box = entity.getBoundingBox().inflate(0.3);
            var hit = box.clip(start, end);

            if (hit.isPresent()) {
                double dist = start.distanceTo(hit.get());
                if (dist < closest) {
                    closest = dist;
                    entityHit = new EntityHitResult(entity);
                }
            }
        }

        /* ---------- FINAL HIT POINT ---------- */
        Vec3 hitPoint = entityHit != null
                ? start.add(dir.scale(closest))
                : (blockHit.getType() == HitResult.Type.BLOCK
                ? blockHit.getLocation()
                : end);

        /* ---------- CLIENT: PARTICLES ---------- */
        if (level.isClientSide) {
            spawnBeamParticles(level, start, hitPoint);
        }

        /* ---------- SERVER: DAMAGE ---------- */
        if (!level.isClientSide && entityHit != null) {
            (entityHit.getEntity())
                    .hurt(level.damageSources().magic(), 8.0F);
        }
    }

    private static void spawnBeamParticles(Level level, Vec3 start, Vec3 end) {
        Vec3 diff = end.subtract(start);
        double length = diff.length();
        Vec3 step = diff.normalize().scale(0.3);

        Vec3 pos = start;
        for (double traveled = 0; traveled < length; traveled += 0.3) {
            level.addParticle(
                    ParticleTypes.END_ROD,
                    pos.x, pos.y, pos.z,
                    0, 0, 0
            );
            pos = pos.add(step);
        }
    }

    private static void test(Level level,Player player){
        if (!(level instanceof ServerLevel serverLevel)) return;

        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 step = direction.scale(0.5);
        double distanceToTravel = 32.0;

        Vec3 c = start;
        for (double distance = 0; distance <= distanceToTravel; distance +=0.5 ){

            BlockPos blockPos = new BlockPos(Mth.floor(c.x), Mth.floor(c.y), Mth.floor(c.z));
            BlockState blockState = level.getBlockState(blockPos);

            if (blockState.getBlock().defaultBlockState().isSolid()){
                break;
            }

            serverLevel.sendParticles(ParticleTypes.FIREWORK, c.x, c.y, c.z, 1, 0, 0, 0, 0);

            List<LivingEntity> entities = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(c, c).inflate(0.5),
                    e -> e != player
            );

            for (LivingEntity entity : entities){
                entity.hurt(level.damageSources().playerAttack(player),32.0f);
                serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
                serverLevel.playSound(null,c.x,c.y,c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS,16 ,1);
            }

//            player.sendSystemMessage(Component.literal("Entities: " + entities));

            //---------------------------
            c = c.add(step);
            if (!entities.isEmpty()){
                break;
            }
        }
    }
}
