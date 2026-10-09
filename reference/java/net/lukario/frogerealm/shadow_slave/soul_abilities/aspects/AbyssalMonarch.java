package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.effects.ModEffects;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class AbyssalMonarch {

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        @SubscribeEvent
        public static void onDamageTaken(LivingHurtEvent event) {
            // 1. Ensure the attacker is a player and the victim is a living entity
            if (event.getSource().getEntity() instanceof Player player) {
                LivingEntity victim = event.getEntity();

                // 2. Requirements check
                if (SoulCore.getAspect(player).equals("Abyssal Monarch") &&
                        SoulCore.getSoulEssence(player) >= 250 &&
                        SoulCore.getAscensionStage(player) >= 5) {

                    // Deduct Essence
                    SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 250);

                    // 3. Define the effect and calculate the amplifier
                    var effectHolder = ModEffects.Abyssal_Monarch_MARK.getHolder().get();
                    int finalAmplifier = 0;

                    // If stage > 4, we calculate stacking
                    if (SoulCore.getAscensionStage(player) > 4) {
                        MobEffectInstance activeMark = victim.getEffect(effectHolder);
                        if (activeMark != null) {
                            // Increment the stack, maxing out at Level 10 (amplifier 9)
                            if (SoulCore.getAscensionStage(player) == 7){
                                finalAmplifier = Math.min(activeMark.getAmplifier() + 1, 23);
                            }else {
                                finalAmplifier = Math.min(activeMark.getAmplifier() + 1, 11);
                            }
                        }
                    }

                    // 4. Apply the effect (this replaces the old one automatically)
                    victim.addEffect(new MobEffectInstance(effectHolder, 200, finalAmplifier));
                } else if (SoulCore.getAspect(player).equals("Abyssal Monarch") &&
                        SoulCore.getSoulEssence(player) >= 250 &&
                        SoulCore.getAscensionStage(player) >= 2) {
                    victim.addEffect(new MobEffectInstance(ModEffects.Abyssal_Monarch_MARK.getHolder().get(), 200, 0));
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ShadeEvents {

        @SubscribeEvent
        public static void onAbyssalZoneTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel serverLevel)) return;

            // 1. Find all active Zones (anchored by ArmorStands)
            for (Entity entity1 : serverLevel.getAllEntities()) {
                if (entity1.isAlive() && entity1 instanceof Player player){
                    if (!SoulCore.getAspect(player).equals("Abyssal Monarch"))return;
                    if (SoulCore.getAscensionStage(player) < 8) return;
//fix later
                    double radius = 12;

                    AABB zoneArea = player.getBoundingBox().inflate(radius);
                    serverLevel.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY()+0.1, player.getZ(), 10, radius/2, 0.1, radius/2, 0.05);

                    List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(LivingEntity.class, zoneArea, e -> e.isAlive() && !e.equals(player) && e instanceof LivingEntity);
                    for (LivingEntity enemy : enemies) {
                        enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 1)); // Slow II

                        if (player.tickCount % 20 == 0) {
                            enemy.hurt(serverLevel.damageSources().magic(), 2.0f);
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onShadeTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel serverLevel)) return;

            // 1. Loop through EVERY ArmorStand in the world (more reliable than searching around players)
            for (Entity entity : serverLevel.getAllEntities()) {
                if (!(entity instanceof ArmorStand shade)) continue;

                // 2. Filter for your Shades
                if (!shade.getPersistentData().contains("ShadeOwner")) continue;

                UUID ownerUUID = shade.getPersistentData().getUUID("ShadeOwner");
                int life = shade.getPersistentData().getInt("ShadeLife");

                // 3. Handle Lifetime
                if (life > 0) {
                    shade.getPersistentData().putInt("ShadeLife", life - 1);
                } else {
                    shade.discard();
                    continue;
                }

                ServerPlayer owner = (ServerPlayer) serverLevel.getPlayerByUUID(ownerUUID);
                if (owner == null) continue;

                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, shade.getX(), shade.getY()+0.1, shade.getZ()+1, 2, 0.3, 0.3, 0.3, 0);
                serverLevel.sendParticles(ParticleTypes.FLAME, shade.getX(), shade.getY()+0.1, shade.getZ()+1, 1, 0.2, 0.2, 0.2, 0);


                AABB attackBox = owner.getBoundingBox().inflate(12.0);
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, attackBox, target -> {
                    // 1. Basic safety: Must be alive and NOT the shade itself
                    if (!target.isAlive() || target.getUUID().equals(shade.getUUID())) {
                        return false;
                    }
                    // 2. Protect the owner
                    if (target.getUUID().equals(ownerUUID)) {
                        return false;
                    }
                    if (target.getPersistentData().contains("AbyssalSpawnOwner")) {
                        // 2. Safely get the UUID and use .equals()
                        if (target.getPersistentData().getUUID("AbyssalSpawnOwner").equals(ownerUUID)) {
                            return false; // It's an ally, don't target it
                        }
                    }
                    // 3. Handle other Shades
                    if (target.getPersistentData().contains("ShadeOwner")) {
                        UUID otherOwner = target.getPersistentData().getUUID("ShadeOwner");
                        // Only ignore if it belongs to the SAME owner (allies)
                        // If you want shades to fight OTHER players' shades, keep this as is.
                        return !otherOwner.equals(ownerUUID);
                    }
                    // 4. If it's not a shade and not the owner, it's a valid enemy (Zombie, Skeleton, etc.)
                    return true;
                });

                if (targets.isEmpty()) {
                    // No targets: Return to owner
                    shade.teleportTo(owner.getX() + 1, owner.getY() + 1, owner.getZ() + 1);

                } else {
                    // 1. Find the target closest to the PLAYER (Bodyguard mode)
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

                    // 2. Teleport the shade to that specific target
                    shade.teleportTo(closestToPlayer.getX(), closestToPlayer.getY(), closestToPlayer.getZ());

                    // 3. Damage Logic: Check if the shade is actually touching the target
                    // We check distance from SHADE to TARGET here for the hit
                    if (owner.tickCount % 5 == 0) {
                        if (shade.distanceToSqr(closestToPlayer) <= 2.25){
                            closestToPlayer.hurt(serverLevel.damageSources().playerAttack(owner), 2.5f);
                            closestToPlayer.invulnerableTime = 0;
                        }
                    }
                }
            }
        }
        @SubscribeEvent
        public static void onAbyssalSpawnTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel serverLevel)) return;

            for (Entity entity : serverLevel.getAllEntities()) {
                if (!(entity instanceof ArmorStand abyssalSpawn)) continue;

                // 2. Filter for your Shades
                if (!abyssalSpawn.getPersistentData().contains("AbyssalSpawnOwner")) continue;

                UUID ownerUUID = abyssalSpawn.getPersistentData().getUUID("AbyssalSpawnOwner");
                int life = abyssalSpawn.getPersistentData().getInt("AbyssalSpawnLife");

                // 3. Handle Lifetime
                if (life > 0) {
                    abyssalSpawn.getPersistentData().putInt("AbyssalSpawnLife", life - 1);
                } else {
                    abyssalSpawn.discard();
                    continue;
                }

                ServerPlayer owner = (ServerPlayer) serverLevel.getPlayerByUUID(ownerUUID);
                if (owner == null) continue;

                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, abyssalSpawn.getX(), abyssalSpawn.getY()+1, abyssalSpawn.getZ(), 3, 0, 0.5, 0, 0);
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, abyssalSpawn.getX(), abyssalSpawn.getY()+1, abyssalSpawn.getZ(), 3, 0.5, 0, 0, 0);
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, abyssalSpawn.getX(), abyssalSpawn.getY()+1, abyssalSpawn.getZ(), 3, 0, 0, 0.5, 0);

                AABB attackBox = owner.getBoundingBox().inflate(12.0);
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, attackBox, target -> {
                    // 1. Basic safety: Must be alive and NOT the shade itself
                    if (!target.isAlive() || target.getUUID().equals(abyssalSpawn.getUUID())) {
                        return false;
                    }
                    if (target.getPersistentData().contains("ShadeOwner")) {
                        // 2. Safely get the UUID and use .equals()
                        if (target.getPersistentData().getUUID("ShadeOwner").equals(ownerUUID)) {
                            return false; // It's an ally, don't target it
                        }
                    }
                    // 2. Protect the owner
                    if (target.getUUID().equals(ownerUUID)) {
                        return false;
                    }
                    // 3. Handle other Shades
                    if (target.getPersistentData().contains("AbyssalSpawnOwner")) {
                        UUID otherOwner = target.getPersistentData().getUUID("AbyssalSpawnOwner");
                        // Only ignore if it belongs to the SAME owner (allies)
                        // If you want shades to fight OTHER players' shades, keep this as is.
                        return !otherOwner.equals(ownerUUID);
                    }
                    // 4. If it's not a shade and not the owner, it's a valid enemy (Zombie, Skeleton, etc.)
                    return true;
                });

                if (targets.isEmpty()) {
                    // No targets: Return to owner
                    abyssalSpawn.teleportTo(owner.getX() + 1, owner.getY() + 1, owner.getZ() + 1);

                } else {
                    // 1. Find the target closest to the PLAYER (Bodyguard mode)
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
                    // 3. Damage Logic: Check if the shade is actually touching the target
                    // We check distance from SHADE to TARGET here for the hit
                    if (owner.tickCount % 20 == 0) {
                        twoPlaceRayCast(owner, serverLevel, abyssalSpawn, closestToPlayer);
                    }
                }
            }
        }

        private static void twoPlaceRayCast(Player player, ServerLevel serverLevel, ArmorStand armorStand, LivingEntity target){

            Vec3 start = armorStand.position().add(0,1,0);
            Vec3 end  = target.position();

            Vec3 direction = end.subtract(start).normalize();
            double distanceToTravel = start.distanceTo(end);

            Vec3 step = direction.scale(0.5);

            Vec3 c = start;
            for (double distance = 0; distance <= distanceToTravel; distance +=0.5 ){

                serverLevel.sendParticles(ParticleTypes.TRIAL_OMEN, c.x, c.y, c.z, 2, 0, 0, 0, 0);
                c = c.add(step);
            }

            target.hurt(serverLevel.damageSources().playerAttack(player),24.0f);
            target.invulnerableTime = 0;
        }
    }

    public static class Abyssal_Monarch_Mark extends MobEffect {
        public Abyssal_Monarch_Mark() {
            super(MobEffectCategory.HARMFUL, 0x191970);
        }

        @Override
        public boolean applyEffectTick(LivingEntity victim, int amplifier) {
            // Minecraft levels are Amplifier + 1.
            // Level > 5 means amplifier is 5 or higher.

            if (victim.tickCount%50==0){
                if (amplifier >= 5) {
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0, true, false));
                }

                // TIER 2: Level 11+ (Amplifier 10) -> Weakness
                if (amplifier >= 10) {
                    victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false));
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, true, false));

                }

                // TIER 3: Level 16+ (Amplifier 15) -> Wither (The "And so on" part)
                if (amplifier >= 15) {
                    victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, true, false));
                    victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, true, false));
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, true, false));
                }

                if (amplifier >= 20) {
                    victim.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, true, false));
                    victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1, true, false));
                    victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 2, true, false));
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3, true, false));
                }
            }
            if (amplifier >= 24) {
                if (victim.tickCount%40==0){
                    victim.hurt(victim.level().damageSources().magic(), 2);
                    victim.invulnerableTime = 0;
                }
            }

//            if (victim.tickCount % 5 == 0) { // Only check every 2 seconds to avoid lag
//                String msg = "§dMark Lvl: §l" + (amplifier + 1) + " §r| §7Target: " + victim.getName().getString();
//                victim.level().players().forEach(p -> p.sendSystemMessage(Component.literal(msg)));
//            }
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            // Return true so applyEffectTick runs every single tick while the effect is active
            return true;
        }
    }

    public static void abyssalMonarchAspectAbilityOneUsed(Player player, Level level, ServerLevel serverLevel){
        if (!SoulCore.getAspect(player).equals("Abyssal Monarch"))return;
        if (SoulCore.getSoulEssence(player)<500)return;
        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-500);

        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,-30);
        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,-20);
        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,-10);
        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,0);
        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,10);
        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,20);
        abyssalMonarchAspectAbilityOneBeam(player,serverLevel,level,30);

    }
    private static void abyssalMonarchAspectAbilityOneBeam(Player player, ServerLevel serverLevel, Level level, float offset){
        Vec3 startPosition = player.getEyePosition().add(0,-1,0);
        Vec3 direction = player.getLookAngle().normalize();
        var effectHolder = ModEffects.Abyssal_Monarch_MARK.getHolder().get();

        double yaw = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));

        yaw += offset;

        float fYaw = (float) yaw;

        direction = Vec3.directionFromRotation(0, fYaw);
        Vec3 current = startPosition;
        for (int i = 0; i <= (5+SoulCore.getAscensionStage(player)); i++){
            current = current.add(direction.scale(1));

            serverLevel.sendParticles(ParticleTypes.TRIAL_OMEN, current.x, current.y+Math.sin(0.5*i)*0.5, current.z, 10, 0.1, 0.1, 0.1, 0);

            level.getEntitiesOfClass(LivingEntity.class, new AABB(current, current).inflate(0.3), e -> e != player)
                    .forEach(entity -> {
                        if (!entity.isInvulnerable()){
                            int amp = entity.hasEffect(effectHolder) ?
                                    Math.min(entity.getEffect(effectHolder).getAmplifier() + 1, 12) : 0;

                            entity.addEffect(new MobEffectInstance(effectHolder, 100, amp));
                            entity.hurt(level.damageSources().playerAttack(player), 6.0f + (amp * 2.5f));
                            entity.invulnerableTime = 5;
                        }
                    });
        }
    }

    public static void abyssalMonarchAspectAbilityThree(Player player, Level level, ServerLevel serverLevel){
        if (!SoulCore.getAspect(player).equals("Abyssal Monarch"))return;
        if (SoulCore.getSoulEssence(player)<1500)return;
        if (SoulCore.getAscensionStage(player) < 3) return;
        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-1500);

        AtomicBoolean canRun = new AtomicBoolean(true);

        Vec3 startPosition = player.getEyePosition().add(0,0,0);
        Vec3 direction = player.getLookAngle().normalize();
        var effectHolder = ModEffects.Abyssal_Monarch_MARK.getHolder().get();

        Vec3 current = startPosition;
        for (int i = 0; i <= (12+SoulCore.getAscensionStage(player)); i++){
            current = current.add(direction.scale(1));

            serverLevel.sendParticles(ParticleTypes.TRIAL_OMEN, current.x, current.y, current.z, 3, 0.1, 0.1, 0.1, 0);

            level.getEntitiesOfClass(LivingEntity.class, new AABB(current, current).inflate(0.3), e -> e != player)
                    .forEach(entity -> {
                        int amp = entity.hasEffect(effectHolder) ?
                        Math.min(entity.getEffect(effectHolder).getAmplifier() + 1, 12) : 0;

                        entity.addEffect(new MobEffectInstance(effectHolder, 200, amp));
                        entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 200, false, false));
                        Vec3 delta = player.getDeltaMovement();
                        player.setDeltaMovement(delta.x, 0, delta.z);
                        entity.setSprinting(false);
                        canRun.set(false);
                        serverLevel.sendParticles(ParticleTypes.EXPLOSION, entity.getX(), entity.getY(), entity.getZ(), 3, 0.1, 0.1, 0.1, 0);
                        player.playNotifySound(SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0F, 1.0F);                    });
            if (!canRun.get()){
                return;
            }
        }
    }

    public static void abyssalMonarchAbilityFour(Player player, ServerLevel level) {
        if (SoulCore.getSoulEssence(player) < 3000) return;
        if (!SoulCore.getAspect(player).equals("Abyssal Monarch"))return;
        int stage = SoulCore.getAscensionStage(player);
        if (SoulCore.getAscensionStage(player)<4)return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 3000);
//        int count = (stage >= 5) ? 2 : 1;
        int count = 1;

        for (int i = 0; i < count; i++) {
            ArmorStand shade = EntityType.ARMOR_STAND.create(level);
            if (shade != null) {
                // Setup "Shade" Appearance
                shade.isMarker();
                shade.setInvisible(true);    // Makes it invisible
                shade.setNoGravity(true);
                shade.setNoBasePlate(true);
                shade.setCustomName(Component.literal("Shade of " + player.getName().getString()));


                // Position near player
                shade.moveTo(player.getX() + (i * 1.2), player.getY() + 0.5, player.getZ(), player.getYRot(), 0);

                // Store Metadata (Owner and Duration)
                // Using Forge/Vanilla Tags to identify it later
                shade.getPersistentData().putUUID("ShadeOwner", player.getUUID());
                shade.getPersistentData().putInt("ShadeLife", 600); // 30 seconds
                shade.getPersistentData().putInt("ShadeStage", stage);

                level.addFreshEntity(shade);

                // Sound/Particles
                level.playSound(null, shade.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 0.5f);
            }
        }
    }
    public static void abyssalMonarchAbilitySix(Player player, ServerLevel level) {
        if (SoulCore.getSoulEssence(player) < 6000) return;
        if (!SoulCore.getAspect(player).equals("Abyssal Monarch"))return;
        int stage = SoulCore.getAscensionStage(player);
        if (SoulCore.getAscensionStage(player)<5)return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 6000);
//        int count = (stage >= 5) ? 2 : 1;
        int count = 1;

        for (int i = 0; i < count; i++) {
            ArmorStand shade = EntityType.ARMOR_STAND.create(level);
            if (shade != null) {
                // Setup "Shade" Appearance
                shade.isMarker();
                shade.setInvisible(true);    // Makes it invisible
                shade.setNoGravity(true);
                shade.setNoBasePlate(true);
                shade.setCustomName(Component.literal("Abyssal Spawn of " + player.getName().getString()));

                // Position near player
                shade.moveTo(player.getX() + (i * 1.2), player.getY() + 0.5, player.getZ(), player.getYRot(), 0);

                // Store Metadata (Owner and Duration)
                // Using Forge/Vanilla Tags to identify it later
                shade.getPersistentData().putUUID("AbyssalSpawnOwner", player.getUUID());
                shade.getPersistentData().putInt("AbyssalSpawnLife", 900); // 30 seconds
                shade.getPersistentData().putInt("AbyssalSpawnStage", stage);

                level.addFreshEntity(shade);

                // Sound/Particles
                level.playSound(null, shade.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 0.5f);
            }
        }
    }
}



