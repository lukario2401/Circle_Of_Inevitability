package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.effects.ModEffects;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

public class ChronoDuelist {

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        @SubscribeEvent
        public static void onEntityTick(LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            CompoundTag nbt = entity.getPersistentData();

            // If this entity is currently marked for rewind
            if (nbt.contains("RewindTimer")) {
                int timer = nbt.getInt("RewindTimer");

                if (timer > 0) {
                    nbt.putInt("RewindTimer", timer - 1);

                    // Particle Trail (Optional)
                    if (timer % 2 == 0 && entity.level() instanceof ServerLevel level) {
                        level.sendParticles(ParticleTypes.ENCHANTED_HIT, entity.getX(), entity.getY() + 1, entity.getZ(), 1, 0.1, 0.1, 0.1, 0);
                    }
                } else {
                    executeRewind(entity);
                }
            }

            if (entity instanceof Player player){
                if (!SoulCore.getAspect(player).equals("Chrono Duelist")) return;
                if (SoulCore.getAscensionStage(player)<7)return;

                SoulCore.setSoulEssence(player,20000000);
                CompoundTag data = player.getPersistentData();
                data.putInt("ChronoEchoes", 2);
            }
        }

        @SubscribeEvent
        public static void onAttack(LivingHurtEvent event) {
            if (event.getSource().getEntity() instanceof Player player) {
                if (!SoulCore.getAspect(player).equals("Chrono Duelist")) return;
                if (SoulCore.getAscensionStage(player)<5)return;
                CompoundTag playerData = player.getPersistentData();

                // Check if the player has the ability "loaded"
                if (playerData.getBoolean("RewindPrimed")) {
                    LivingEntity victim = event.getEntity();

                    // 1. Consume the essence and the flag
                    playerData.putBoolean("RewindPrimed", false);

                    // 2. Mark the victim with the 3-second timer
                    markVictim(victim, player);
                }
            }
        }

        @SubscribeEvent
        public static void onAttackStoreEcho(LivingHurtEvent event) {
            if (event.getSource().getEntity() instanceof Player player) {
                if (!SoulCore.getAspect(player).equals("Chrono Duelist")) return;
                if (SoulCore.getAscensionStage(player)<2)return;

                // Get current echoes from NBT
                CompoundTag data = player.getPersistentData();
                int currentEchoes = data.getInt("ChronoEchoes");

                // Max echoes: 3
                if (currentEchoes < SoulCore.getAscensionStage(player)) {
                    data.putInt("ChronoEchoes", currentEchoes + 1);

                    // Visual feedback: Blue sparks on the player
                    if (player.level() instanceof ServerLevel level) {
                        level.sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1, player.getZ(), 5, 0.2, 0.2, 0.2, 0.02);
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onDamageConsumeEcho(LivingHurtEvent event) {
            if (event.getEntity() instanceof Player player) {
                if (!SoulCore.getAspect(player).equals("Chrono Duelist")) return;

                CompoundTag data = player.getPersistentData();
                int currentEchoes = data.getInt("ChronoEchoes");

                if (currentEchoes > 0 && player.level() instanceof ServerLevel serverLevel) {
                    // Store the OLD position for the "shatter" effect
                    double oldX = player.getX();
                    double oldY = player.getY();
                    double oldZ = player.getZ();

                    // 1. Consume the echo
                    data.putInt("ChronoEchoes", currentEchoes - 1);

                    // 2. Reduce damage by 30%
                    event.setAmount(event.getAmount() * 0.7f);

                    // 3. Teleport slightly backward
                    Vec3 look = player.getLookAngle();
                    player.teleportTo(oldX - (look.x * 2.5), oldY, oldZ - (look.z * 2.5));

                    // 4. Late Upgrade: Restore Soul Essence
                    if (SoulCore.getAscensionStage(player) >= 5) {
                        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) + 150);
                    }

                    // 5. Visual & Sound Effects

                    // Effect A: Shatter at the OLD position (The Echo breaking)
                    serverLevel.sendParticles(ParticleTypes.POOF, oldX, oldY + 1, oldZ, 15, 0.2, 0.4, 0.2, 0.05);
                    serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, oldX, oldY + 1, oldZ, 10, 0.3, 0.5, 0.3, 0.1);

                    // Effect B: Puff at the NEW position (The Reappearance)
                    serverLevel.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1, player.getZ(), 5, 0.1, 0.1, 0.1, 0.01);

                    player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 2.0f);
                    player.level().playSound(null, player.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.5f);
                    player.invulnerableTime = 20;
                }
            }
        }

        @SubscribeEvent
        public static void onChronoSummonTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel serverLevel)) return;

            for (Entity entity : serverLevel.getAllEntities()) {
                if (!(entity instanceof ArmorStand shade)) continue;

                if (!shade.getPersistentData().contains("ChronoSummonOwner")) continue;

                UUID ownerUUID = shade.getPersistentData().getUUID("ChronoSummonOwner");
                int life = shade.getPersistentData().getInt("ChronoSummonLife");

                if (life > 0) {
                    shade.getPersistentData().putInt("ChronoSummonLife", life - 1);
                } else {
                    shade.discard();
                    continue;
                }

                ServerPlayer owner = (ServerPlayer) serverLevel.getPlayerByUUID(ownerUUID);
                if (owner == null) continue;

                AABB attackBox = owner.getBoundingBox().inflate(12.0);
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, attackBox, target -> {
                    if (!target.isAlive() || target.getUUID().equals(shade.getUUID())) {
                        return false;
                    }
                    if (target.getUUID().equals(ownerUUID)) {
                        return false;
                    }
                    if (target.getPersistentData().contains("ChronoSummonOwner")) {
                        UUID otherOwner = target.getPersistentData().getUUID("ChronoSummonOwner");
                        return !otherOwner.equals(ownerUUID);
                    }
                    return true;
                });

                if (targets.isEmpty()) {
                    shade.teleportTo(owner.getX() + 1, owner.getY() + 1, owner.getZ() + 1);

                } else {
                    LivingEntity closestToPlayer = targets.getFirst();
                    double closestDistSq = closestToPlayer.distanceToSqr(owner);

                    for (int i = 1; i < targets.size(); i++) {
                        LivingEntity potential = targets.get(i);
                        double distSq = potential.distanceToSqr(owner);

                        if (distSq < closestDistSq) {
                            closestToPlayer = potential;
                            closestDistSq = distSq;
                        }
                    }

                    shade.teleportTo(closestToPlayer.getX(), closestToPlayer.getY(), closestToPlayer.getZ());

                    if (owner.tickCount % 15 == 0) {
                        if (shade.distanceToSqr(closestToPlayer) <= 2.25){
                            closestToPlayer.hurt(serverLevel.damageSources().playerAttack(owner), 6.8f);
                            closestToPlayer.invulnerableTime = 0;
                        }
                    }
                }
                serverLevel.sendParticles(ParticleTypes.GLOW, shade.getX(), shade.getY()+1, shade.getZ(), 2, 0.3, 0.3, 0.3, 0);

            }
        }



    }

    public static void chronoDuelistAbilityOneUsed(Player player, Level level, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Chrono Duelist")) return;
        if (SoulCore.getSoulEssence(player) < 300 || SoulCore.getAscensionStage(player) < 1) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 300);

        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();


        for (int i = SoulCore.getAscensionStage(player) + 4; i > 0; i--) {
            Vec3 c = start.add(direction.scale(i));

            BlockPos blockPos = new BlockPos(Mth.floor(c.x), Mth.floor(c.y), Mth.floor(c.z));
            BlockState blockState = level.getBlockState(blockPos);

            if (!blockState.getBlock().defaultBlockState().isSolid()) {
                player.teleportTo(c.x, c.y, c.z);
                return;
            }
        }
    }

    public static void chronoDuelistAbilityThreeUsed(Player player, Level level, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Chrono Duelist")) return;
        if (SoulCore.getSoulEssence(player) < 1200 || SoulCore.getAscensionStage(player) < 2) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 1200);


        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 1200,
                (player.hasEffect(MobEffects.DIG_SPEED) ? player.getEffect(MobEffects.DIG_SPEED).getAmplifier() + 3 : 0)));

        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 1200,
                (player.hasEffect(MobEffects.MOVEMENT_SPEED) ? player.getEffect(MobEffects.MOVEMENT_SPEED).getAmplifier() + 2 : 0)));

        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 1200,
                (player.hasEffect(MobEffects.JUMP) ? player.getEffect(MobEffects.JUMP).getAmplifier() + 1 : 0)));

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1200,
                (player.hasEffect(MobEffects.DAMAGE_RESISTANCE) ? player.getEffect(MobEffects.DAMAGE_RESISTANCE).getAmplifier() + 1 : 0)));

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200,
                (player.hasEffect(MobEffects.DAMAGE_BOOST) ? player.getEffect(MobEffects.DAMAGE_BOOST).getAmplifier() + 1 : 0)));

    }

    public static void chronoDuelistAbilityFourUsed(Player player) {
        if (!SoulCore.getAspect(player).equals("Chrono Duelist")) return;
        if (SoulCore.getSoulEssence(player) < 4500 || SoulCore.getAscensionStage(player) < 5) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 4500);

        player.getPersistentData().putBoolean("RewindPrimed", true);

        player.level().playSound(null, player.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0f, 2.0f);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
        }
    }

    private static void markVictim(LivingEntity victim, Player owner) {
        CompoundTag nbt = victim.getPersistentData();

        nbt.putDouble("RewindX", victim.getX());
        nbt.putDouble("RewindY", victim.getY());
        nbt.putDouble("RewindZ", victim.getZ());
        nbt.putInt("RewindTimer", 120); // 3 seconds
        nbt.putUUID("RewindOwner", owner.getUUID());

        // Visual: Blue "Time" particles on the victim
        victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
    }

    public static void executeRewind(LivingEntity victim) {
        CompoundTag nbt = victim.getPersistentData();
        double oldX = nbt.getDouble("RewindX");
        double oldY = nbt.getDouble("RewindY");
        double oldZ = nbt.getDouble("RewindZ");

        // Calculate Damage: Distance moved * Multiplier
        double distance = victim.position().distanceTo(new Vec3(oldX, oldY, oldZ));
        float damage = (float) (6.0f + (distance * 1.5f));

        // Snap back and Damage
        victim.teleportTo(oldX, oldY, oldZ);
        Player owner = victim.level().getPlayerByUUID(nbt.getUUID("RewindOwner"));

        victim.hurt(victim.level().damageSources().magic(), damage);

        // Visuals & Sound
        victim.level().playSound(null, victim.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.5f);

        // Cleanup
        nbt.remove("RewindTimer");
        nbt.remove("RewindX");
        nbt.remove("RewindY");
        nbt.remove("RewindZ");
    }

    public static void chronoDuelistAbilityFive(Player player, ServerLevel level) {
        if (SoulCore.getSoulEssence(player) < 3000) return;
        if (!SoulCore.getAspect(player).equals("Chrono Duelist"))return;
        int stage = SoulCore.getAscensionStage(player);
        if (SoulCore.getAscensionStage(player)<5)return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 3000);
//        int count = (stage >= 5) ? 2 : 1;
        int count = 1;

        for (int i = 0; i < count; i++) {
            ArmorStand armorStand = EntityType.ARMOR_STAND.create(level);
            if (armorStand != null) {
                // Setup "armorStand" Appearance
                armorStand.isMarker();
                armorStand.setNoGravity(true);
                armorStand.setNoBasePlate(true);
                armorStand.setShowArms(true);
                armorStand.setCustomNameVisible(true);
                armorStand.setInvulnerable(true);
                armorStand.setInvisible(true);

                armorStand.setCustomName(Component.literal("Chrono Summon of " + player.getName().getString()));

                armorStand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
                armorStand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
                armorStand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
                armorStand.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));

                // 3. Give it a weapon/item in the hand
                armorStand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));

                // Position near player
                armorStand.moveTo(player.getX() + (i * 1.2), player.getY() + 0.5, player.getZ(), player.getYRot(), 0);

                // Store Metadata (Owner and Duration)
                // Using Forge/Vanilla Tags to identify it later
                armorStand.getPersistentData().putUUID("ChronoSummonOwner", player.getUUID());
                armorStand.getPersistentData().putInt("ChronoSummonLife", 360); // 30 seconds
                armorStand.getPersistentData().putInt("ChronoSummonStage", stage);

                level.addFreshEntity(armorStand);

                // Sound/Particles
                level.playSound(null, armorStand.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 0.5f);
            }
        }
    }

    public static void chronoDuelistAbilitySix(Player player, ServerLevel serverLevel) {
        if (SoulCore.getSoulEssence(player) < 3000) return;
        if (!SoulCore.getAspect(player).equals("Chrono Duelist")) return;
        int stage = SoulCore.getAscensionStage(player);
        if (stage < 6) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 3000);

        AABB attackBox = player.getBoundingBox().inflate(12.0);

        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, attackBox,
                entity -> entity != player && entity.isAlive() && !entity.isSpectator()
        );

        for (LivingEntity target : targets) {

            serverLevel.sendParticles(
                    ParticleTypes.GLOW,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    20,
                    0.5, 0.5, 0.5,
                    0.05
            );

            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 150));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 150));
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 120, 150));
            target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20, 150));

            target.setDeltaMovement(0,0,0);
            target.hurtMarked = true;
        }
    }
}
