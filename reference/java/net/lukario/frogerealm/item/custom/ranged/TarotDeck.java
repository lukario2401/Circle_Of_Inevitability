package net.lukario.frogerealm.item.custom.ranged;

import net.lukario.frogerealm.item.custom.detection.ClickState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class TarotDeck extends Item {
    public TarotDeck(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;

        if (ClickState.leftClickPressed){
            if (!player.level().isClientSide && !player.getCooldowns().isOnCooldown(this)) {

                Level level = player.level();

                int n = level.random.nextInt(4);
                if (n == 0){
                    shootDeathTarotCard(player, level, 32f);
                } else if (n == 1) {
                    shootTowerTarotCard(player, level, 32f);
                } else if (n == 2){
                    shootEmperorTarotCard(player, level, 32f);
                } else if (n == 3){
                    shootMagicianTarotCard(player, level, 32f);
                }

                player.getCooldowns().addCooldown(this, 1);
            }
        }
        return true;
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        if (!level.isClientSide) {

            if (ClickState.rightClickPressed && !player.getCooldowns().isOnCooldown(this)){

                shootDeathTarotCard(player, level, 32f);
                shootTowerTarotCard(player, level, 32f);
                shootEmperorTarotCard(player, level, 32f);
                shootMagicianTarotCard(player, level, 32f);

            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    private static void shootMagicianTarotCard(Player player, Level level, Float range){

        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();

        player.playNotifySound(SoundEvents.BREEZE_JUMP, SoundSource.MASTER, 1 , 0.9f);

        for (float i = 0; i <= range;i+=0.5f){

            List<LivingEntity> entities = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(start, start).inflate(0.25),
                    e -> e != player
            );

            for (LivingEntity entity : entities){

                Vec3 entityVec3 = entity.position();

                List<LivingEntity> targetEntities = level.getEntitiesOfClass(
                        LivingEntity.class,
                        new AABB(entityVec3, entityVec3).inflate(5),
                        e -> e != entity || e != player
                );

                for (LivingEntity targetEntity : targetEntities){
                    twoEntityBeam(player, entity, targetEntity, true);
                }

            }

            BlockPos blockPos = new BlockPos(Mth.floor(start.x), Mth.floor(start.y), Mth.floor(start.z));
            BlockState blockState = level.getBlockState(blockPos);

            if (!blockState.getCollisionShape(level, blockPos).isEmpty()) {
                break;
            }

            Vector3f color = new Vector3f(0.2f, 0.85f, 0.3f);
            DustParticleOptions redDust = new DustParticleOptions(color, 1f);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        redDust,
                        start.x, start.y, start.z,
                        1,      // count
                        0, 0, 0,// spread
                        0       // speed
                );
            }

            start = start.add(direction.scale(0.5));
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
                Vector3f color = new Vector3f(0.2f, 0.85f, 0.3f);
                DustParticleOptions tarotDust = new DustParticleOptions(color, 1f);

                serverLevel.sendParticles(
                        tarotDust,
                        point.x, point.y, point.z,
                        1,      // count
                        0, 0, 0,// spread
                        0       // speed
                );
            }

        }
        target.hurt(level.damageSources().playerAttack(player), 6.0f);
        entity.invulnerableTime = 0;
    }

    private static void shootEmperorTarotCard(Player player, Level level, Float range){

        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();

        player.playNotifySound(SoundEvents.BREEZE_JUMP, SoundSource.MASTER, 1 , 0.9f);

        for (float i = 0; i <= range;i+=0.5f){

            List<LivingEntity> entities = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(start, start).inflate(0.25),
                    e -> e != player
            );

            for (LivingEntity entity : entities){

                entity.invulnerableTime = 0;

                entity.addEffect(new MobEffectInstance( MobEffects.WEAKNESS,40,5));
                entity.addEffect(new MobEffectInstance( MobEffects.WEAKNESS,80,4));
                entity.addEffect(new MobEffectInstance( MobEffects.WEAKNESS,120,3));
                entity.addEffect(new MobEffectInstance( MobEffects.WEAKNESS,160,2));
                entity.addEffect(new MobEffectInstance( MobEffects.WEAKNESS,200,1));

                entity.addEffect(new MobEffectInstance( MobEffects.MOVEMENT_SLOWDOWN,40,5));
                entity.addEffect(new MobEffectInstance( MobEffects.MOVEMENT_SLOWDOWN,80,2));
                entity.addEffect(new MobEffectInstance( MobEffects.MOVEMENT_SLOWDOWN,200,1));

                entity.addEffect(new MobEffectInstance( MobEffects.WITHER,200,1));

                i+=256;

                player.playNotifySound(
                        SoundEvents.ARROW_HIT_PLAYER,
                        SoundSource.PLAYERS,
                        2f,
                        1f
                );
            }

            BlockPos blockPos = new BlockPos(Mth.floor(start.x), Mth.floor(start.y), Mth.floor(start.z));
            BlockState blockState = level.getBlockState(blockPos);

            if (!blockState.getCollisionShape(level, blockPos).isEmpty()) {
                break;
            }

            Vector3f color = new Vector3f(0.8f, 0.6f, 0.2f);
            DustParticleOptions redDust = new DustParticleOptions(color, 1f);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        redDust,
                        start.x, start.y, start.z,
                        1,      // count
                        0, 0, 0,// spread
                        0       // speed
                );
            }

            start = start.add(direction.scale(0.5));
        }
    }

    private static void shootTowerTarotCard(Player player, Level level, Float range){
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();

        player.playNotifySound(SoundEvents.BREEZE_JUMP, SoundSource.MASTER, 1 , 0.9f);

        for (float i = 0; i <= range;i+=0.5f){

            List<LivingEntity> entities = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(start, start).inflate(0.25),
                    e -> e != player
            );

            for (LivingEntity entity : entities){
                if (Math.random() < 0.5) {

                    entity.addDeltaMovement(new Vec3(0, 1.2, 0));
                    entity.hurtMarked = true;

                    if (level instanceof ServerLevel serverLevel) {
                        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
                        if (lightning != null) {
                            lightning.moveTo(start.x(), start.y(), start.z());
                            serverLevel.addFreshEntity(lightning);
                        }
                    }
                    i+=256;
                }else {
                    level.explode(
                            null,                   // source entity (can be player)
                            start.x(),
                            start.y(),
                            start.z(),
                            4.0f,                   // power (TNT = 4)
                            Level.ExplosionInteraction.TNT
                    );
                    i+=256;
                }

                player.playNotifySound(
                        SoundEvents.ARROW_HIT_PLAYER,
                        SoundSource.PLAYERS,
                        2f,
                        1f
                );
            }

            BlockPos blockPos = new BlockPos(Mth.floor(start.x), Mth.floor(start.y), Mth.floor(start.z));
            BlockState blockState = level.getBlockState(blockPos);

            if (!blockState.getCollisionShape(level, blockPos).isEmpty()) {
                break;
            }

            Vector3f color = new Vector3f(0.8f, 0.8f, 0.8f);
            DustParticleOptions redDust = new DustParticleOptions(color, 1f);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        redDust,
                        start.x, start.y, start.z,
                        1,      // count
                        0, 0, 0,// spread
                        0       // speed
                );
            }

            start = start.add(direction.scale(0.5));
        }
    }


    private static void shootDeathTarotCard(Player player, Level level, Float range){

        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();

//        level.playSound(player, start.x, start.y, start.z, SoundEvents.BREEZE_JUMP, SoundSource.MASTER, 1 , 0.9f);
        player.playNotifySound(SoundEvents.BREEZE_JUMP, SoundSource.MASTER, 1 , 0.9f);

        for (float i = 0; i <= range;i+=0.5f){

            List<LivingEntity> entities = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(start, start).inflate(0.25),
                    e -> e != player
            );

            for (LivingEntity entity : entities){

                entity.setHealth(entity.getHealth()-32.0f);
                entity.setLastHurtByPlayer(player);
                if (entity.getHealth()<=0){
                    entity.kill();
                }

                level.playSound(null,start.x,start.y,start.z, SoundEvents.SOUL_ESCAPE, SoundSource.BLOCKS,3 ,1);

                player.playNotifySound(
                        SoundEvents.ARROW_HIT_PLAYER,
                        SoundSource.PLAYERS,
                        2f,
                        1f
                );
            }

            BlockPos blockPos = new BlockPos(Mth.floor(start.x), Mth.floor(start.y), Mth.floor(start.z));
            BlockState blockState = level.getBlockState(blockPos);

            if (!blockState.getCollisionShape(level, blockPos).isEmpty()) {
                break;
            }

            Vector3f color = new Vector3f(1f, 0f, 0f);
            DustParticleOptions redDust = new DustParticleOptions(color, 1f);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        redDust,
                        start.x, start.y, start.z,
                        1,      // count
                        0, 0, 0,// spread
                        0       // speed
                );
            }


            start = start.add(direction.scale(0.5));
        }
    }

    private static void shootVanillaSnowballProjectile(Player player, Level level, Float velocity, Float inaccuracy){

        Snowball snowball = new Snowball(level, player);
        snowball.setPos(
                player.getX(),
                player.getEyeY() - 0.1,
                player.getZ()
        );

// direction + speed + inaccuracy
        snowball.shootFromRotation(
                player,
                player.getXRot(), // pitch
                player.getYRot(), // yaw
                0.0F,
                velocity, // speed
                inaccuracy  // inaccuracy
        );

        level.addFreshEntity(snowball);

    }

    private static void shootVanillaSmallFireball(Player player, Level level, float velocity, float inaccuracy) {
        if (level.isClientSide) return; // only run on server

        Vec3 look = player.getLookAngle();

        // Add Gaussian noise for inaccuracy
        RandomSource rand = level.getRandom();

        double nx = look.x + rand.nextGaussian() * inaccuracy;
        double ny = look.y + rand.nextGaussian() * inaccuracy;
        double nz = look.z + rand.nextGaussian() * inaccuracy;

        Vec3 motion = new Vec3(nx, ny, nz).normalize().scale(velocity);

        // spawn fireball slightly in front of player
        Vec3 spawnPos = player.getEyePosition(1.0f).add(look.x * 0.5, look.y * 0.5, look.z * 0.5);

        SmallFireball fireball = new SmallFireball(level, player, motion);
        fireball.setPos(spawnPos);

        level.addFreshEntity(fireball);
    }

    private static void shootLargeFireball(Player player, Level level, float velocity, float inaccuracy, int explosionPower) {
        if (level.isClientSide) return; // only run on server

        // Get player's look direction
        Vec3 look = player.getLookAngle();

        // Add Gaussian noise for inaccuracy
        RandomSource rand = level.getRandom();
        double nx = look.x + rand.nextGaussian() * inaccuracy;
        double ny = look.y + rand.nextGaussian() * inaccuracy;
        double nz = look.z + rand.nextGaussian() * inaccuracy;

        // normalize and scale by velocity
        Vec3 motion = new Vec3(nx, ny, nz).normalize().scale(velocity);

        // spawn fireball slightly in front of player
        Vec3 spawnPos = player.getEyePosition(1.0f).add(look.x * 0.5, look.y * 0.5, look.z * 0.5);

        // Create custom LargeFireball
        LargeFireball fireball = new LargeFireball(level, player, motion, explosionPower);
        fireball.setPos(spawnPos);

        // spawn entity
        level.addFreshEntity(fireball);
    }
}

