package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.effects.ModEffects;
import net.lukario.frogerealm.particles.ModParticles;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class ShadowSlaveAspect {
    private static final HashMap<Player, Float> AbilityOneCooldown = new HashMap<>();
    private static final HashMap<Player, Float> AbilityTwoCooldown = new HashMap<>();
    private static final HashMap<Player, Float> AbilityFourCooldown = new HashMap<>();
    private static final HashMap<Player, Float> AbilitySixCooldown = new HashMap<>();
    /**
     * 1. THE LIFESTEAL EVENT
     * This handles the "Heal more based on Shadow Mark" logic.
     * Being a static inner class with @Mod.EventBusSubscriber makes it work automatically.
     */
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {
        @SubscribeEvent
        public static void onDamage(LivingHurtEvent event) {
            if (event.getSource().getEntity() instanceof Player player) {
                if (SoulCore.getAspect(player).equals("Shadow Slave")) {
                    if (SoulCore.getSoulEssence(player) <= 100 || SoulCore.getAscensionStage(player) < 3) return;
                    SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 100);

                    LivingEntity victim = event.getEntity();
                    var effectHolder = ModEffects.SHADOW_MARK.getHolder().get();

                    if (victim.hasEffect(effectHolder)) {
                        int amp = victim.getEffect(effectHolder).getAmplifier();

                        float voidCloakPassiveBuff = 0;
                        if (player.getHealth()<player.getMaxHealth()/3 && SoulCore.getAscensionStage(player)>=5 || SoulCore.getAscensionStage(player)>=7){
                            voidCloakPassiveBuff+=0.1f;
                            if (SoulCore.getAscensionStage(player)>=7){
                                voidCloakPassiveBuff+=0.2f;
                            }
                        }else{
                            voidCloakPassiveBuff=0;
                        }


                        // Lifesteal calculation: 5% base + 2% per stack
                        float lifestealMult = 0.05f + voidCloakPassiveBuff + (amp * 0.02f);
                        float healAmount = event.getAmount() * lifestealMult;

                        player.heal(healAmount);

                        if (player.level() instanceof ServerLevel serverLevel) {
                            serverLevel.sendParticles(ParticleTypes.HEART,
                                    player.getX(), player.getY() + 1.5, player.getZ(),
                                    2, 0.2, 0.2, 0.2, 0.05);
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

            Player player = event.player;

            if (event.phase == TickEvent.Phase.START) {
                coolDownManagement(player);
                if (event.player.tickCount % 2 == 0 && SoulCore.getAscensionStage(player)>=5){
                    coolDownManagement(player);
                }
                if (event.player.tickCount % 2 == 0 && SoulCore.getAscensionStage(player)>=6){
                    coolDownManagement(player);
                    coolDownManagement(player);
                }
                if (event.player.tickCount % 2 == 0 && SoulCore.getAscensionStage(player)>=7){
                    AbilityOneCooldown.put(player, AbilityOneCooldown.get(player) - AbilityOneCooldown.get(player)/5);
                    AbilityTwoCooldown.put(player, AbilityTwoCooldown.get(player) - AbilityTwoCooldown.get(player)/5);
                    AbilityFourCooldown.put(player, AbilityFourCooldown.get(player) - AbilityFourCooldown.get(player)/10);
                    AbilitySixCooldown.put(player, AbilitySixCooldown.get(player) - AbilitySixCooldown.get(player)/10);

                    SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) + (float) (SoulCore.getAspectTier(player) * SoulCore.getAspectTier(player) * SoulCore.getAscensionStage(player)) /(8-SoulCore.getAscensionStage(player)));
                    SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) + (float) (SoulCore.getAspectTier(player) * SoulCore.getAspectTier(player) * SoulCore.getAscensionStage(player)) /(8-SoulCore.getAscensionStage(player)));
                    SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) + (float) (SoulCore.getAspectTier(player) * SoulCore.getAspectTier(player) * SoulCore.getAscensionStage(player)) /(8-SoulCore.getAscensionStage(player)));
                }

                if (player.getHealth() < player.getMaxHealth() / 3 && SoulCore.getAscensionStage(player)>=5 || SoulCore.getAscensionStage(player)>=7) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 5, 1));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5, 1));
                }
            }
        }

        private static void coolDownManagement(Player player){
            /// Ability One
            AbilityOneCooldown.putIfAbsent(player, 0f);
            if (AbilityOneCooldown.get(player) > 0) {
                AbilityOneCooldown.put(player, AbilityOneCooldown.get(player) - 1f);
            }

            /// Ability Two
            AbilityTwoCooldown.putIfAbsent(player, 0f);
            if (AbilityTwoCooldown.get(player) > 0) {
                AbilityTwoCooldown.put(player, AbilityTwoCooldown.get(player) - 1f);
            }

            /// Ability Four
            AbilityFourCooldown.putIfAbsent(player, 0f);
            if (AbilityFourCooldown.get(player) > 0) {
                AbilityFourCooldown.put(player, AbilityFourCooldown.get(player) - 1f);
            }

            /// Ability Six
            AbilitySixCooldown.putIfAbsent(player, 0f);
            if (AbilitySixCooldown.get(player) > 0) {
                AbilitySixCooldown.put(player, AbilitySixCooldown.get(player) - 1f);
            }
        }
    }


    /**
     * 2. THE CUSTOM EFFECT
     * Put this inside your Aspect class so you don't need a separate file for the Effect.
     */
    public static class ShadowMarkEffect extends MobEffect {
        public ShadowMarkEffect() {
            super(MobEffectCategory.HARMFUL, 0x3e0066);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return false;
        }
    }

    /**
     * 3. ABILITY ONE: DASH & AOE
     */
    public static void shadowSlaveAspectAbilityOneUsed(Player player, Level level, ServerLevel serverLevel){

        if (SoulCore.getAspect(player).equals("Shadow Slave")){
            if (SoulCore.getSoulEssence(player)<250)return;
            if (AbilityOneCooldown.get(player)>0){
                String formattedTime = String.format("%.2f", AbilityOneCooldown.getOrDefault(player, 0f) / 20.0);
                player.sendSystemMessage(Component.literal("On Cooldown For: " + formattedTime + "s"));
                return;
            }


            SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player) - 250);
            AbilityOneCooldown.put(player,400f);

            List<LivingEntity> entities = level.getEntitiesOfClass(
                    LivingEntity.class,
                    new AABB(player.position(), player.position()).inflate(2.5),
                    e -> e != player
            );

            for (LivingEntity livingEntity : entities){

                livingEntity.hurt(level.damageSources().playerAttack(player),12.0f);
                livingEntity.invulnerableTime = 0;

                serverLevel.sendParticles(
                        ParticleTypes.SOUL,
                        livingEntity.getX(),
                        livingEntity.getY() + 1,
                        livingEntity.getZ(),
                        15,
                        0.3, 0.3, 0.3,
                        0.01
                );
                serverLevel.sendParticles(
                        ModParticles.VOID_RIFT.get(),
                        livingEntity.getX(),
                        livingEntity.getY() + 1,
                        livingEntity.getZ(),
                        10,
                        0.3, 0.3, 0.3,
                        0.02
                );
                level.playSound(null,player.getX(), player.getY(), player.getZ(), SoundEvents.SOUL_ESCAPE, SoundSource.MASTER,8 ,1);

                player.playNotifySound(SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 4f,1f);
            }

        }

    }

    /**
     * 4. ABILITY TWO: SHADOW BEAM
     */
    public static void shadowSlaveAspectAbilityTwoUsed(Player player, Level level, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Shadow Slave")) return;
        if (SoulCore.getSoulEssence(player) < 300 || SoulCore.getAscensionStage(player) < 2) return;
        if (AbilityTwoCooldown.get(player)>0){
            String formattedTime = String.format("%.2f", AbilityTwoCooldown.getOrDefault(player, 0f) / 20.0);
            player.sendSystemMessage(Component.literal("On Cooldown For: " + formattedTime + "s"));
            return;
        }

        AbilityTwoCooldown.put(player,200f);
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 300);

        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        var effectHolder = ModEffects.SHADOW_MARK.getHolder().get();

        for (double i = 0; i < 8; i += 0.5) {
            Vec3 pos = start.add(direction.scale(i));
            serverLevel.sendParticles(ParticleTypes.SOUL, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);

            level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(0.3), e -> e != player)
                    .forEach(entity -> {
                        int amp = entity.hasEffect(effectHolder) ?
                                Math.min(entity.getEffect(effectHolder).getAmplifier() + 1, 12) : 0;

                        entity.addEffect(new MobEffectInstance(effectHolder, 100, amp));
                        entity.hurt(level.damageSources().playerAttack(player), 6.0f + (amp * 2.5f));
                        entity.invulnerableTime = 0;
                    });
        }
    }

    public static void shadowSlaveAspectAbilityFourUsed(Player player, Level level, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Shadow Slave")) return;
        if (SoulCore.getSoulEssence(player) < 1200 || SoulCore.getAscensionStage(player) < 4) return;
        if (AbilityFourCooldown.get(player)>0){
            String formattedTime = String.format("%.2f", AbilityFourCooldown.getOrDefault(player, 0f) / 20.0);
            player.sendSystemMessage(Component.literal("On Cooldown For: " + formattedTime + "s"));
            return;
        }

        // Find the closest entity instead of looping through all
        LivingEntity target = level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(12), e -> e != player)
                .stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);

        if (target != null) {
            SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 1200);
            AbilityFourCooldown.put(player,1200f);

            // Teleport slightly behind or next to them instead of inside them
            Vec3 targetPos = target.position().add(target.getLookAngle().scale(-1.5));
            player.teleportTo(targetPos.x, target.getY(), targetPos.z);

            // Effects
            serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 20, 0.2, 0.2, 0.2, 0.1);
            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    public static void shadowSlaveAspectAbilitySixUsed(Player player, Level level, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Shadow Slave")) return;
        if (SoulCore.getSoulEssence(player) < 12000 || SoulCore.getAscensionStage(player) < 6) return;
        if (AbilitySixCooldown.getOrDefault(player, 0f) > 0) {
            String formattedTime = String.format("%.2f", AbilitySixCooldown.getOrDefault(player, 0f) / 20.0);
            player.sendSystemMessage(Component.literal("On Cooldown For: " + formattedTime + "s"));
            return;
        }
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 12000);
        AbilitySixCooldown.put(player, 900f);

        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        double maxDistance = 32.0; // Long range laser

        Vec3 impactPoint = start.add(direction.scale(maxDistance)); // Default to max range

        // 1. Trace the laser beam
        for (double i = 0; i < maxDistance; i += 0.50) {
            Vec3 currentPos = start.add(direction.scale(i));

            // Visual for the beam
            serverLevel.sendParticles(ParticleTypes.SOUL, currentPos.x, currentPos.y, currentPos.z, 2, 0.05, 0.05, 0.05, 0.02);

            // Check for Block Collision
            if (!level.getBlockState(new BlockPos((int)currentPos.x, (int)currentPos.y, (int)currentPos.z)).isAir()) {
                impactPoint = currentPos;
                break;
            }

            // Check for Entity Collision
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(currentPos, currentPos).inflate(0.25), e -> e != player);
            if (!targets.isEmpty()) {
                impactPoint = currentPos;
                break;
            }
        }

        createShadowExplosion(player, level, serverLevel, impactPoint);
    }

    private static void createShadowExplosion(Player player, Level level, ServerLevel serverLevel, Vec3 pos) {
        float explosionRadius = 6.0f;
        var effectHolder = ModEffects.SHADOW_MARK.getHolder().get();

        // Visuals for the impact
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 50, 1.0, 1.0, 1.0, 0.2);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 0.5f);

        // Damage logic
        List<LivingEntity> victims = level.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(explosionRadius), e -> e != player);

        for (LivingEntity entity : victims) {
            int amp = entity.hasEffect(effectHolder) ? Math.min(entity.getEffect(effectHolder).getAmplifier() + 2, 12) : 1;

            // Double the mark stacks because this is an ultimate
            entity.addEffect(new MobEffectInstance(effectHolder, 200, amp));

            // Massive AOE Damage
            entity.hurt(level.damageSources().playerAttack(player), 30.0f + (amp * 5.0f));
            entity.invulnerableTime = 0;
        }
    }
}