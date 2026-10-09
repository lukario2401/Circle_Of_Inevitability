package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;

public class VoidAscendant {



    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class StormEvents {

        @SubscribeEvent
        public static void onVoidAscendantDamageTaken(LivingHurtEvent event) {
            LivingEntity victim = event.getEntity();
            if (victim.level().isClientSide) return;

            if (victim instanceof Player player && SoulCore.getAspect(player).equals("Void Ascendant")) {
                if (SoulCore.getAscensionStage(player) < 7) return;
                if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                    float attackerCorruption = attacker.getPersistentData().getFloat("VoidAscendantCorruption");

                    if (attackerCorruption >= 300) {
                        event.setAmount(0);
                        player.invulnerableTime=40;
                        attacker.getPersistentData().putFloat("VoidAscendantCorruption", attackerCorruption-300);

                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.ENDER_EYE_DEATH, SoundSource.PLAYERS, 0.5f, 0.5f);
                    }
                    else if (attackerCorruption >= 200) {
                        event.setAmount(event.getAmount() * 0.3f); // 70% reduction
                        player.invulnerableTime=10;
                        attacker.getPersistentData().putFloat("VoidAscendantCorruption", attackerCorruption-200);

                    }
                    else if (attackerCorruption >= 100) {
                        event.setAmount(event.getAmount() * 0.5f); // 50% reduction
                        player.invulnerableTime=20;
                        attacker.getPersistentData().putFloat("VoidAscendantCorruption", attackerCorruption-100);

                    }
                }
            }
        }

        @SubscribeEvent
        public static void onVoidAscendantTick(LivingEvent.LivingTickEvent event) {
            LivingEntity target = event.getEntity();
            if (target.level().isClientSide) return;

            // --- NEW: 1. Handle Void Link ---
            String linkedPlayerUUID = target.getPersistentData().getString("VoidLinkOwner");
            if (!linkedPlayerUUID.isEmpty() && target.level() instanceof ServerLevel sl) {
                // Find the player who owns this link
                Player owner = sl.getServer().getPlayerList().getPlayer(java.util.UUID.fromString(linkedPlayerUUID));

                // Check if link is valid (owner online, alive, and within 32 blocks)
                if (owner != null && owner.isAlive() && target.distanceToSqr(owner) < 32 * 32) {

                    if (target.tickCount % 20 == 0) { // Ticks every 1 second
                        // Target continuously gains corruption
                        applyCorruption(target, 15f, 1.1f);

                        float currentCorrupt = target.getPersistentData().getFloat("VoidAscendantCorruption");

                        // Player heals slightly, scaling with the target's corruption
                        float healAmount = 1.0f + (currentCorrupt / 100f);
                        owner.heal(healAmount);

                        // Visual Link: Draw a purple particle beam between them
                        DustParticleOptions linkDust = new DustParticleOptions(new Vector3f(0.5f, 0.0f, 0.8f), 1.2f);
                        Vec3 start = target.position().add(0, target.getBbHeight() / 2, 0);
                        Vec3 end = owner.position().add(0, owner.getBbHeight() / 2, 0);
                        Vec3 dir = end.subtract(start);
                        for (double i = 0; i < dir.length(); i += 0.5) {
                            Vec3 pos = start.add(dir.normalize().scale(i));
                            sl.sendParticles(linkDust, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
                        }
                    }
                } else {
                    // Break link if player is offline, dead, or too far away
                    target.getPersistentData().remove("VoidLinkOwner");
                }
            }

            // --- 2. Refresh Corruption & Threshold Checks ---
            float corruption = target.getPersistentData().getFloat("VoidAscendantCorruption");
            if (corruption < 60) return; // Now safe to return, link logic already processed

            // Existing Decay
            if (target.tickCount % 10 == 0) {
                target.getPersistentData().putFloat("VoidAscendantCorruption", corruption - 1);
            }

            // Existing 60 Threshold
            if (target.tickCount % 60 == 0) {
                float baseDamage = 12f;
                float totalDamage = baseDamage + (baseDamage * (corruption / 50f));
                target.hurt(target.level().damageSources().magic(), totalDamage);
                if (target.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.3, 0.5, 0.3, 0.02);
                }
            }

            // Existing 100 Threshold
            if (target.tickCount % 40 == 0 && corruption >= 100) {
                float baseDamage = 12f;
                float totalDamage = baseDamage + (baseDamage * (corruption / 50f));
                target.hurt(target.level().damageSources().magic(), totalDamage);
                if (target.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.WITCH, target.getX(), target.getY() + 1.0, target.getZ(), 20, 0.3, 0.5, 0.3, 0.02);
                }
            }

            // --- UPDATED: 500 Threshold (Rupture & Heal Spike) ---
            if (corruption >= 500) {
                AABB explosionArea = target.getBoundingBox().inflate(3.0);
                List<LivingEntity> nearbyEntities = target.level().getEntitiesOfClass(
                        LivingEntity.class, explosionArea, e -> e != target && e.isAlive()
                );

                for (LivingEntity nearby : nearbyEntities) {
                    nearby.hurt(target.level().damageSources().magic(), 750);
                    nearby.getPersistentData().putFloat("VoidAscendantCorruption", 0);
                }

                if (target.level() instanceof ServerLevel sl) {
                    DustParticleOptions redDust = new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), 1.5f);
                    sl.sendParticles(redDust, target.getX(), target.getY() + 1.0, target.getZ(), 40, 1.5, 0.5, 1.5, 0.1);
                    target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.0f, 0.5f);

                    // NEW: Heal Spike Logic
                    String ownerUUID = target.getPersistentData().getString("VoidLinkOwner");
                    if (!ownerUUID.isEmpty()) {
                        Player owner = sl.getServer().getPlayerList().getPlayer(java.util.UUID.fromString(ownerUUID));
                        if (owner != null && owner.isAlive()) {
                            owner.heal(20.0f); // Massive heal spike (10 hearts)
                            owner.sendSystemMessage(Component.literal("Target ruptured! Vitality absorbed.").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
                        }
                    }
                }

                // Final cleanup
                target.getPersistentData().putFloat("VoidAscendantCorruption", 0);
                target.getPersistentData().remove("VoidLinkOwner"); // Always sever the link on rupture
            }
        }
    }

    private static void applyCorruption(LivingEntity target, Float corruptionToApply, Float mult){

        float corruption = target.getPersistentData().getFloat("VoidAscendantCorruption");
        target.getPersistentData().putFloat("VoidAscendantCorruption", Math.min(500,((corruption+corruptionToApply)*mult)));

    }

    private static void damageBasedOnCorruption(Player player, LivingEntity target, Float damage){

        float corruption = target.getPersistentData().getFloat("VoidAscendantCorruption");

        float totalDamage = damage + (damage * (corruption/50));

        target.hurt(player.level().damageSources().playerAttack(player), totalDamage);
        target.invulnerableTime = 2;

        player.sendSystemMessage(Component.literal(target.getName().getString()+", has: " + corruption + " Corruption"));
        player.sendSystemMessage(Component.literal(totalDamage + ", damage dealt"));
    }

    public static void voidAscendantAbilityOne(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Void Ascendant")) return;
        if (SoulCore.getSoulEssence(player) < 1200) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 1200);

        Vec3 location = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();

        for (float i = 0; i <= 32; i+=0.5f){
            Vec3 c = location.add(direction.scale(i));

            DustParticleOptions redDust = new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), 1.0f);
            sl.sendParticles(redDust,
                    c.x, c.y, c.z, 3, 0, 0, 0, 0);


            List<LivingEntity> hits = level.getEntitiesOfClass(
                    LivingEntity.class, new AABB(c, c).inflate(0.5),
                    e -> e != player && e.isAlive());

            for (LivingEntity target : hits){

                applyCorruption(target,20f, 1.2f);
                damageBasedOnCorruption(player, target, 12f);

                return;
            }
        }
    }

    public static void voidAscendantAbilityTwo(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Void Ascendant")) return;
        if (SoulCore.getSoulEssence(player) < 2400) return;
        if (SoulCore.getAscensionStage(player) < 2) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 2400);

        Vec3 location = player.getEyePosition();

        List<LivingEntity> hits = level.getEntitiesOfClass(
                LivingEntity.class, new AABB(location, location).inflate(6),
                e -> e != player && e.isAlive());

        for (LivingEntity target : hits){
            applyCorruption(target,20f, 1.2f);
            damageBasedOnCorruption(player, target, 8f);

        }

        List<LivingEntity> hitss = level.getEntitiesOfClass(
                LivingEntity.class, new AABB(location, location).inflate(3),
                e -> e != player && e.isAlive());

        for (LivingEntity target : hitss){
            applyCorruption(target,20f, 1.2f);
            damageBasedOnCorruption(player, target, 8f);

        }

    }

    public static void voidAscendantAbilityThree(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Void Ascendant")) return;
        if (SoulCore.getSoulEssence(player) < 3600) return;
        if (SoulCore.getAscensionStage(player) < 3) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 3600);

        Vec3 location = player.getEyePosition();

        level.getEntitiesOfClass(LivingEntity.class, new AABB(location, location).inflate(24),
                        e -> e != player && e.isAlive())
                .stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(location.x, location.y, location.z)))
                .ifPresent(target -> {

                    float corruption = target.getPersistentData().getFloat("VoidAscendantCorruption");
                    damageBasedOnCorruption(player, target,corruption);
                    target.getPersistentData().putFloat("VoidAscendantCorruption",0);

                });
    }

    public static void voidAscendantAbilityFour(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Void Ascendant")) return;
        if (SoulCore.getSoulEssence(player) < 4800) return; // Adjust cost as needed
        if (SoulCore.getAscensionStage(player) < 4) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 4800);

        Vec3 location = player.getEyePosition();

        level.getEntitiesOfClass(LivingEntity.class, new AABB(location, location).inflate(24),
                        e -> e != player && e.isAlive())
                .stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(location.x, location.y, location.z)))
                .ifPresent(target -> {
                    // Establish the link by saving the Player's UUID to the target
                    target.getPersistentData().putString("VoidLinkOwner", player.getStringUUID());

                    // Initial burst of corruption to start the link
                    applyCorruption(target, 20f, 1.2f);

                    player.sendSystemMessage(Component.literal("Linked to " + target.getName().getString()));

                    // Sound effect for the link connecting
                    sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.8f);
                });
    }

    public static void voidAscendantAbilityFive(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Void Ascendant")) return;
        if (SoulCore.getSoulEssence(player) < 12000) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 12000);

        Vec3 location = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();

        for (float i = 0; i <= 32; i += 0.5f) {
            Vec3 c = location.add(direction.scale(i));

            DustParticleOptions redDust = new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), 1.0f);
            sl.sendParticles(redDust,
                    c.x, c.y, c.z, 3, 0, 0, 0, 0);


            List<LivingEntity> hits = level.getEntitiesOfClass(
                    LivingEntity.class, new AABB(c, c).inflate(0.5),
                    e -> e != player && e.isAlive());

            for (LivingEntity mainTarget : hits) {

                List<LivingEntity> aoeHits = level.getEntitiesOfClass(
                        LivingEntity.class, new AABB(c, c).inflate(3.5),
                        e -> e != player && e.isAlive());

                for (LivingEntity targets : aoeHits) {
                    applyCorruption(targets, 60f, 1.2f);
                    targets.hurt(sl.damageSources().playerAttack(player), 1f);
                }

                return;
            }
        }
    }

    public static void voidAscendantAbilitySix(Player player, Level level, ServerLevel sl) {
        // 1. Core Checks & Costs
        if (!SoulCore.getAspect(player).equals("Void Ascendant")) return;
        if (SoulCore.getSoulEssence(player) < 24000) return; // Ultimate tier cost
        if (SoulCore.getAscensionStage(player) < 6) return;  // Ultimate tier stage
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 24000);

        Vec3 location = player.position();

        // 2. Define a massive 16-block radius
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, new AABB(location, location).inflate(16),
                e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            // 3. The Black Hole Effect: Pull entities towards the player
            float strength = 0.8f*3;
            Vec3 pullDir = location.subtract(target.position()).normalize().scale(strength);
            target.setDeltaMovement(target.getDeltaMovement().add(pullDir));
            target.hurtMarked = true; // Forces the server to update the entity's velocity immediately

            // 4. Massive Corruption Spike & Damage
            applyCorruption(target, 100f, 1.5f);
            damageBasedOnCorruption(player, target, 20f);
        }

        // 5. Ultimate Visuals & Audio
        // Uses a massive ring of dark/purple particles to simulate a collapsing void
        DustParticleOptions darkVoidDust = new DustParticleOptions(new Vector3f(0.2f, 0.0f, 0.3f), 2.5f);
        sl.sendParticles(darkVoidDust,
                location.x, location.y + 1, location.z,
                300,   // High particle count
                8.0,   // Wide horizontal spread
                1.0,   // Low vertical spread (creates a disk/ring shape)
                8.0,
                0.05);

        sl.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PORTAL_TRIGGER, SoundSource.PLAYERS, 1.0f, 0.3f);
    }


}
