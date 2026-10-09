package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.item.ModItems;
import net.lukario.frogerealm.item.custom.swords.ShepardsBlade;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

public class Shepard {

    private static final String NBT_SHADOW_LURK_DURATION = "shepard_shadow_lurk_duration";
    private static final String NBT_FLESH_HEALING = "shepard_shadow_flesh_healing";
    private static final String NBT_GRAZED_SOULS = "shepard_grazed_souls"; // CompoundTag storing soul type + remaining duration
    private static final String NBT_ACTIVE_SOUL = "shepard_active_soul";
    private static final String NBT_GRAZING_COOLDOWN = "shepard_grazing_cooldown";
    private static final String NBT_COMMANDEERED_TARGETS = "shepard_commandeered_targets"; // ListTag of UUIDs
    private static final String NBT_COMMANDEER_COOLDOWN = "shepard_commandeer_cooldown";
    private static final String NBT_CULL_COOLDOWN = "shepard_cull_cooldown";
    private static final String NBT_CULL_TARGETS = "shepard_cull_targets"; // ListTag of {uuid, ticksRemaining}
    private static final String NBT_CHRYSALIS_COOLDOWN = "shepard_chrysalis_cooldown";
    private static final String NBT_CHRYSALIS_TARGETS = "shepard_chrysalis_targets"; // ListTag of {uuid, duration, storedDamage}

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ShepardEvents {
        @SubscribeEvent
        public static void onChrysalisAbsorbDamage(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
            LivingEntity victim = event.getEntity();
            if (victim.level().isClientSide()) return;
            if (!(victim.level() instanceof ServerLevel sl)) return;

            // Find if any Shepard player has this entity chrysalis'd
            for (ServerPlayer shepard : sl.getServer().getPlayerList().getPlayers()) {
                if (!SoulCore.getAspect(shepard).equals("Shepard")) continue;

                net.minecraft.nbt.CompoundTag data = shepard.getPersistentData();
                if (!data.contains(NBT_CHRYSALIS_TARGETS)) continue;

                net.minecraft.nbt.ListTag targets = data.getList(NBT_CHRYSALIS_TARGETS, net.minecraft.nbt.Tag.TAG_COMPOUND);

                for (int i = 0; i < targets.size(); i++) {
                    net.minecraft.nbt.CompoundTag entry = targets.getCompound(i);
                    UUID uuid = entry.getUUID("uuid");

                    if (!victim.getUUID().equals(uuid)) continue;

                    // Absorb damage into stored pool instead
                    float incoming = event.getAmount();
                    float stored = entry.getFloat("storedDamage");
                    entry.putFloat("storedDamage", stored + incoming);
                    event.setAmount(0); // negate the damage
                    event.setCanceled(true);

                    // Visual feedback that chrysalis absorbed a hit
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            victim.getX(), victim.getY() + 1.0, victim.getZ(),
                            10, 0.3, 0.3, 0.3, 0.05);
                    return;
                }
            }
        }
        @SubscribeEvent
        public static void onShepardTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Shepard")) return;

            int ascensionStage = SoulCore.getAscensionStage(player);

            float shadow_lurk_duration = player.getPersistentData().getFloat(NBT_SHADOW_LURK_DURATION);
            if (shadow_lurk_duration>0){
                player.getPersistentData().putFloat(NBT_SHADOW_LURK_DURATION,shadow_lurk_duration-1);
                if (player.getPersistentData().getFloat(NBT_SHADOW_LURK_DURATION)==0){
                    player.sendSystemMessage(Component.literal("Shadow Lurk Deactivated"));
                    player.playNotifySound(SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
                Position playerPosition = player.position();
                BlockPos targetPos = new BlockPos((int)playerPosition.x(), (int)playerPosition.y(), (int)playerPosition.z());

                if (sl.getMaxLocalRawBrightness(targetPos)- Math.floor(((double) ascensionStage /2))<2){
                    addEffects(player,ascensionStage);
                    removeAllAggro(player,sl,16);
                }
            }
            float flesh_heal_duration = player.getPersistentData().getFloat(NBT_FLESH_HEALING);
            if (flesh_heal_duration>0) {
                player.getPersistentData().putFloat(NBT_FLESH_HEALING, flesh_heal_duration - 1);
                if (player.getPersistentData().getFloat(NBT_FLESH_HEALING) == 0) {
                    player.sendSystemMessage(Component.literal("Flesh Heal Deactivated"));
                    player.playNotifySound(SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0f, 1.0f);
                }
            }

            int grazingCooldown = player.getPersistentData().getInt(NBT_GRAZING_COOLDOWN);
            if (grazingCooldown > 0) {
                player.getPersistentData().putInt(NBT_GRAZING_COOLDOWN, grazingCooldown - 1);
            }

            String activeSoul = player.getPersistentData().getString(NBT_ACTIVE_SOUL);
            if (!activeSoul.isEmpty()) {
                applyGrazedSoulEffects(player, activeSoul, ascensionStage);
            }

            int commandeerCooldown = player.getPersistentData().getInt(NBT_COMMANDEER_COOLDOWN);
            if (commandeerCooldown > 0) {
                player.getPersistentData().putInt(NBT_COMMANDEER_COOLDOWN, commandeerCooldown - 1);
            }
            tickCommandeeredShadows(player, sl);

            int cullCooldown = player.getPersistentData().getInt(NBT_CULL_COOLDOWN);
            if (cullCooldown > 0) {
                player.getPersistentData().putInt(NBT_CULL_COOLDOWN, cullCooldown - 1);
            }
            tickCullTargets(player, sl);

            int chrysalisCooldown = player.getPersistentData().getInt(NBT_CHRYSALIS_COOLDOWN);
            if (chrysalisCooldown > 0) {
                player.getPersistentData().putInt(NBT_CHRYSALIS_COOLDOWN, chrysalisCooldown - 1);
            }
            tickChrysalisTargets(player, sl);
        }
        @SubscribeEvent
        public static void onMobKill(LivingDeathEvent event) {
            LivingEntity victim = event.getEntity();
            DamageSource source = event.getSource();

            if (source.getEntity() instanceof Player player) {
                if (player.level().isClientSide()) return;
                if (!SoulCore.getAspect(player).equals("Shepard")) return;

                int ascensionStage = SoulCore.getAscensionStage(player);

                float flesh_heal_duration = player.getPersistentData().getFloat(NBT_FLESH_HEALING);
                if (flesh_heal_duration>0){
                    player.heal((float) (victim.getMaxHealth()/(10-ascensionStage*1.5)));
                    player.playNotifySound(SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.PLAYERS, 1.0f, 1.0f);
                    addEffects(player,ascensionStage,100);
                }
                if (player.getPersistentData().getFloat(NBT_FLESH_HEALING) > 0 || true) { // always eligible
                    int cooldown = player.getPersistentData().getInt(NBT_GRAZING_COOLDOWN);
                    if (cooldown <= 0) {
                        tryGrazeSoul(player, victim, ascensionStage);
                    }
                }
            }
        }
    }

    private static void tickCullTargets(Player player, ServerLevel sl) {
        net.minecraft.nbt.CompoundTag data = player.getPersistentData();
        if (!data.contains(NBT_CULL_TARGETS)) return;

        net.minecraft.nbt.ListTag targets = data.getList(NBT_CULL_TARGETS, net.minecraft.nbt.Tag.TAG_COMPOUND);
        net.minecraft.nbt.ListTag updated = new net.minecraft.nbt.ListTag();
        int ascensionStage = SoulCore.getAscensionStage(player);

        for (int i = 0; i < targets.size(); i++) {
            net.minecraft.nbt.CompoundTag entry = targets.getCompound(i);
            int remaining = entry.getInt("duration") - 1;
            if (remaining <= 0) continue;

            UUID targetUUID = entry.getUUID("uuid");
            LivingEntity target = (LivingEntity) sl.getEntity(targetUUID);
            if (target == null || target.isDeadOrDying()) continue;

            entry.putInt("duration", remaining);
            updated.add(entry);

            // Degeneration DoT: bypasses armor via magic damage every 20 ticks
            if (remaining % 20 == 0) {
                float dotDamage = 1.5f + ascensionStage * 0.3f;
                target.hurt(sl.damageSources().magic(), dotDamage);

                // Weaken the target — strip their armor effectiveness
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, ascensionStage >= 6 ? 2 : 1, false, false));
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1, false, false));

                // Dark particle trail on target
                sl.sendParticles(ParticleTypes.SOUL,
                        target.getX(), target.getY() + 1.0, target.getZ(),
                        5, 0.2, 0.3, 0.2, 0.01);
            }

            // Wither effect at higher ascension
            if (ascensionStage >= 5) {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 30, 0, false, false));
            }
        }

        data.put(NBT_CULL_TARGETS, updated);
    }
    private static void tickChrysalisTargets(Player player, ServerLevel sl) {
        net.minecraft.nbt.CompoundTag data = player.getPersistentData();
        if (!data.contains(NBT_CHRYSALIS_TARGETS)) return;

        net.minecraft.nbt.ListTag targets = data.getList(NBT_CHRYSALIS_TARGETS, net.minecraft.nbt.Tag.TAG_COMPOUND);
        net.minecraft.nbt.ListTag updated = new net.minecraft.nbt.ListTag();
        int ascensionStage = SoulCore.getAscensionStage(player);

        for (int i = 0; i < targets.size(); i++) {
            net.minecraft.nbt.CompoundTag entry = targets.getCompound(i);
            int remaining = entry.getInt("duration") - 1;
            float storedDamage = entry.getFloat("storedDamage");

            UUID targetUUID = entry.getUUID("uuid");
            LivingEntity target = (LivingEntity) sl.getEntity(targetUUID);

            // Chrysalis expired or target dead — trigger explosion
            if (remaining <= 0 || target == null || target.isDeadOrDying()) {
                triggerChrysalisExplosion(player, target, storedDamage, sl, ascensionStage);
                continue;
            }

            entry.putInt("duration", remaining);
            updated.add(entry);

            // Root and freeze target each tick
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 10, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.JUMP, 10, 128, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 10, 10, false, false));

            // Prevent the target from dealing damage by keeping them stunned
            if (target instanceof Mob mob) {
                mob.setTarget(null);
            }

            // Particle shell around target every tick
            if (remaining % 5 == 0) {
                double angle = (remaining * 15) % 360;
                double rad = Math.toRadians(angle);
                sl.sendParticles(ParticleTypes.SOUL,
                        target.getX() + Math.cos(rad) * 0.8,
                        target.getY() + 1.0,
                        target.getZ() + Math.sin(rad) * 0.8,
                        3, 0.05, 0.1, 0.05, 0.01);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        target.getX() + Math.cos(rad + Math.PI) * 0.8,
                        target.getY() + 1.0,
                        target.getZ() + Math.sin(rad + Math.PI) * 0.8,
                        3, 0.05, 0.1, 0.05, 0.01);
            }
        }

        data.put(NBT_CHRYSALIS_TARGETS, updated);
    }

    private static void triggerChrysalisExplosion(Player player, LivingEntity target, float storedDamage, ServerLevel sl, int ascensionStage) {
        // Determine explosion center
        double ex = target != null ? target.getX() : player.getX();
        double ey = target != null ? target.getY() : player.getY();
        double ez = target != null ? target.getZ() : player.getZ();

        float blastRadius = 4.0f + ascensionStage * 0.5f;
        float blastDamage = storedDamage * (0.5f + ascensionStage * 0.1f); // stored damage scales with ascension
//        target.hurt(player.level().damageSources().playerAttack(player), blastDamage);

        // Hit all nearby entities in blast radius except the player
        AABB blastArea = new AABB(ex - blastRadius, ey - blastRadius, ez - blastRadius,
                ex + blastRadius, ey + blastRadius, ez + blastRadius);

        List<LivingEntity> blastTargets = sl.getEntitiesOfClass(LivingEntity.class, blastArea, e ->
                e != player && !e.isDeadOrDying());

        for (LivingEntity blastTarget : blastTargets) {
            float distanceFactor = 1.0f - (float)(blastTarget.distanceTo(player) / blastRadius);
            float actualDamage = Math.max(blastDamage * distanceFactor, 3.0f);
            blastTarget.hurt(sl.damageSources().magic(), actualDamage);
            blastTarget.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            blastTarget.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1));
        }

        // Splash back on player if too close
        if (target != null && player.distanceTo(target) < 3.0f) {
            float selfDamage = blastDamage * 0.3f;
            player.hurt(sl.damageSources().magic(), selfDamage);
            player.sendSystemMessage(Component.literal("§8Too close — the chrysalis blast hit you for §7" + String.format("%.1f", selfDamage) + "§8 damage."));
        }

        // Large burst of soul particles at explosion center
        sl.sendParticles(ParticleTypes.SOUL, ex, ey + 1.0, ez, 60, blastRadius * 0.4, 1.0, blastRadius * 0.4, 0.08);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, ex, ey + 1.0, ez, 40, blastRadius * 0.3, 0.8, blastRadius * 0.3, 0.1);
        sl.sendParticles(ParticleTypes.SCULK_SOUL, ex, ey + 1.0, ez, 20, blastRadius * 0.2, 0.5, blastRadius * 0.2, 0.05);

        player.playNotifySound(SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 1.5f, 0.3f);
        player.sendSystemMessage(Component.literal("§8Shadow Chrysalis shattered."));
    }
    private static void tickCommandeeredShadows(Player player, ServerLevel sl) {
        net.minecraft.nbt.CompoundTag data = player.getPersistentData();
        if (!data.contains(NBT_COMMANDEERED_TARGETS)) return;

        net.minecraft.nbt.ListTag targets = data.getList(NBT_COMMANDEERED_TARGETS, net.minecraft.nbt.Tag.TAG_COMPOUND);
        net.minecraft.nbt.ListTag updated = new net.minecraft.nbt.ListTag();

        for (int i = 0; i < targets.size(); i++) {
            net.minecraft.nbt.CompoundTag entry = targets.getCompound(i);
            int remaining = entry.getInt("duration") - 1;
            if (remaining <= 0) continue;

            UUID targetUUID = entry.getUUID("uuid");
            LivingEntity target = (LivingEntity) sl.getEntity(targetUUID);

            if (target == null || target.isDeadOrDying()) continue;

            entry.putInt("duration", remaining);
            updated.add(entry);

            // Root the target every tick
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 10, false, false));
            target.addEffect(new MobEffectInstance(MobEffects.JUMP, 10, 128, false, false)); // jump suppression

            // Shadow damage: deal a small fraction of their max health as reflected darkness damage
            // every 40 ticks deal damage
            if (remaining % 40 == 0) {
                int ascensionStage = SoulCore.getAscensionStage(player);
                float shadowDmg = target.getMaxHealth() * (0.04f + ascensionStage * 0.005f);
                target.hurt(sl.damageSources().magic(), shadowDmg);

                // Particle effect at target's feet
                sl.sendParticles(ParticleTypes.SOUL,
                        target.getX(), target.getY(), target.getZ(),
                        8, 0.3, 0.1, 0.3, 0.05);

                // If target's health drops low enough, sever the shadow — deal bonus spirit damage
                if (target.getHealth() < target.getMaxHealth() * 0.25f) {
                    float spiritDmg = target.getMaxHealth() * 0.15f;
                    target.hurt(sl.damageSources().magic(), spiritDmg);
                    player.sendSystemMessage(Component.literal("§8Shadow severed — spirit fractured."));
                    continue; // don't re-add, shadow is gone
                }
            }
        }

        data.put(NBT_COMMANDEERED_TARGETS, updated);
    }

    private static int getMaxGrazedSouls(int ascensionStage) {
        if (ascensionStage >= 7) return 7;
        if (ascensionStage >= 5) return 5;
        if (ascensionStage >= 3) return 3;
        return 1;
    }

    private static String getSoulTypeFromMob(LivingEntity mob) {
        String name = mob.getType().toShortString();
        return switch (name) {
            case "enderman"       -> "enderman";
            case "blaze"          -> "blaze";
            case "spider"         -> "spider";
            case "witch"          -> "witch";
            case "warden"         -> "warden";
            case "creeper"        -> "creeper";
            case "skeleton"       -> "skeleton";
            default               -> "";
        };
    }

    private static void tryGrazeSoul(Player player, LivingEntity victim, int ascensionStage) {
        String soulType = getSoulTypeFromMob(victim);
        if (soulType.isEmpty()) return;

        net.minecraft.nbt.CompoundTag data = player.getPersistentData();
        net.minecraft.nbt.ListTag grazedSouls = data.contains(NBT_GRAZED_SOULS)
                ? data.getList(NBT_GRAZED_SOULS, net.minecraft.nbt.Tag.TAG_COMPOUND)
                : new net.minecraft.nbt.ListTag();

        int maxSouls = getMaxGrazedSouls(ascensionStage);

        // Check for duplicate
        for (int i = 0; i < grazedSouls.size(); i++) {
            if (grazedSouls.getCompound(i).getString("type").equals(soulType)) {
                // Refresh duration instead of adding duplicate
                grazedSouls.getCompound(i).putInt("duration", 6000);
                data.put(NBT_GRAZED_SOULS, grazedSouls);
                player.sendSystemMessage(Component.literal("§8Soul refreshed: §7" + soulType));
                return;
            }
        }

        // Evict oldest if at cap
        if (grazedSouls.size() >= maxSouls) {
            grazedSouls.remove(0);
            player.sendSystemMessage(Component.literal("§8Soul slot full — oldest soul released."));
        }

        net.minecraft.nbt.CompoundTag soulTag = new net.minecraft.nbt.CompoundTag();
        soulTag.putString("type", soulType);
        soulTag.putInt("duration", 6000); // 5 minutes at 20 tps
        grazedSouls.add(soulTag);
        data.put(NBT_GRAZED_SOULS, grazedSouls);

        player.sendSystemMessage(Component.literal("§8Grazed soul absorbed: §7" + soulType));
        player.playNotifySound(SoundEvents.SCULK_SENSOR_BREAK, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    private static void tickGrazedSoulDurations(Player player) {
        net.minecraft.nbt.CompoundTag data = player.getPersistentData();
        if (!data.contains(NBT_GRAZED_SOULS)) return;

        net.minecraft.nbt.ListTag grazedSouls = data.getList(NBT_GRAZED_SOULS, net.minecraft.nbt.Tag.TAG_COMPOUND);
        net.minecraft.nbt.ListTag updated = new net.minecraft.nbt.ListTag();

        for (int i = 0; i < grazedSouls.size(); i++) {
            net.minecraft.nbt.CompoundTag soul = grazedSouls.getCompound(i);
            int remaining = soul.getInt("duration") - 1;
            if (remaining > 0) {
                soul.putInt("duration", remaining);
                updated.add(soul);
            } else {
                String expired = soul.getString("type");
                player.sendSystemMessage(Component.literal("§8Soul faded: §7" + expired));
                // Clear active if it was the active one
                if (player.getPersistentData().getString(NBT_ACTIVE_SOUL).equals(expired)) {
                    player.getPersistentData().putString(NBT_ACTIVE_SOUL, "");
                }
            }
        }
        data.put(NBT_GRAZED_SOULS, updated);
    }

    private static void applyGrazedSoulEffects(Player player, String soulType, int ascensionStage) {
        int amplifier = ascensionStage >= 5 ? 1 : 0;
        switch (soulType) {
            case "enderman" ->
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10, amplifier));
            case "zombie" ->
                    player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 10, amplifier));
            case "blaze" ->
                    player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 10, 0));
            case "spider" -> {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 10, 0));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10, amplifier));
            }
            case "witch" ->
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, amplifier));
            case "warden" ->
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 10, amplifier));
            case "creeper" ->
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 10, amplifier));
            case "skeleton" ->
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 10, amplifier));
        }
    }

    private static void addEffects(Player player, int ascensionStage, int duration){
        if (ascensionStage>=7){
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 2));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 2));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 2));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 2));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, 2));

        } else if (ascensionStage >= 5) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, 1));

        } else if (ascensionStage >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, duration, 0));

        } else if (ascensionStage >= 1) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0));
        }
    }

    private static void addEffects(Player player, int ascensionStage){
        if (ascensionStage>=7){
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 2));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10, 2));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 10, 2));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 10, 0));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 10, 2));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 10, 2));

        } else if (ascensionStage >= 5) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 10, 1));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 10, 1));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 10, 1));

        } else if (ascensionStage >= 3) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 10, 0));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 30, 1));

        } else if (ascensionStage >= 1) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 10, 0));
        }
    }

    private static void removeAllAggro(Player player, Level level, double radius) {
        // 1. Create a bounding box around the player based on the radius
        AABB area = player.getBoundingBox().inflate(radius);

        // 2. Find all Mobs (entities with AI) within that area
        List<Mob> nearbyMobs = level.getEntitiesOfClass(Mob.class, area);

        for (Mob mob : nearbyMobs) {
            // 3. If the mob is currently targeting this player, wipe its target
            if (mob.getTarget() == player) {
                mob.setTarget(null); // Clears vanilla attack targets

                // 4. Force the mob's brain logic to forget the player (important for modern AI like Wardens/Piglin Brutes)
                if (mob.getBrain().hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET)) {
                    mob.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);
                }
                if (mob.getBrain().hasMemoryValue(net.minecraft.world.entity.ai.memory.MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)) {
                    mob.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                }
            }
        }
    }

    private static boolean canUseClassShepard(Player player, Boolean dontCheck) {
        if (dontCheck) return true;
        return SoulCore.getAspect(player).equals("Shepard");
    }


    // Ability 1
    public static void shepardShadowLurk(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassShepard(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 0) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-250);

        float shadow_lurk_duration = player.getPersistentData().getFloat(NBT_SHADOW_LURK_DURATION);
        player.getPersistentData().putFloat(NBT_SHADOW_LURK_DURATION, 200f+shadow_lurk_duration);
        player.sendSystemMessage(Component.literal("Shadow Lurk Activated"));
        player.playNotifySound(SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 1.0f, 1.0f);

    }

    // Ability 2
    public static void shepardShadowShaping(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassShepard(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 1250) return;
        if (SoulCore.getAscensionStage(player) < 1) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-1250);


        ItemStack itemStack = new ItemStack(ModItems.SHEPARDS_BLADE.get(), 1);

        if (!player.getInventory().add(itemStack)) {
            player.drop(itemStack, false); // false = drops at feet, true = throws forward
        }
        player.playNotifySound(SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 2.0f);

    }

    // Ability 3
    public static void shepardFleshHealing(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassShepard(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 2450) return;
        if (SoulCore.getAscensionStage(player) < 3) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-2450);

        float shadow_lurk_duration = player.getPersistentData().getFloat(NBT_SHADOW_LURK_DURATION);
        player.getPersistentData().putFloat(NBT_SHADOW_LURK_DURATION, 300f+shadow_lurk_duration);
        player.sendSystemMessage(Component.literal("Flesh Heal Activated"));
        player.playNotifySound(SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 1.0f, 1.0f);

    }

    // Ability 4 — Grazing: cycle active grazed soul
    public static void shepardGrazing(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassShepard(player, bypassClassCheck)) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        net.minecraft.nbt.CompoundTag data = player.getPersistentData();
        if (!data.contains(NBT_GRAZED_SOULS)) {
            player.sendSystemMessage(Component.literal("§8No souls grazed yet."));
            return;
        }

        net.minecraft.nbt.ListTag grazedSouls = data.getList(NBT_GRAZED_SOULS, net.minecraft.nbt.Tag.TAG_COMPOUND);
        if (grazedSouls.isEmpty()) {
            player.sendSystemMessage(Component.literal("§8No souls grazed yet."));
            return;
        }

        String current = data.getString(NBT_ACTIVE_SOUL);

        // Find next soul in list after current
        int nextIndex = 0;
        for (int i = 0; i < grazedSouls.size(); i++) {
            if (grazedSouls.getCompound(i).getString("type").equals(current)) {
                nextIndex = (i + 1) % grazedSouls.size();
                break;
            }
        }

        String nextSoul = grazedSouls.getCompound(nextIndex).getString("type");
        data.putString(NBT_ACTIVE_SOUL, nextSoul);

        player.sendSystemMessage(Component.literal("§8Active soul: §7" + nextSoul));
        player.playNotifySound(SoundEvents.SCULK_SENSOR_BREAK, SoundSource.PLAYERS, 0.7f, 1.2f);
    }

    // Ability 5 — Commandeer Shadow: bind nearest enemy to their shadow
    public static void shepardCommandeerShadow(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassShepard(player, bypassClassCheck)) return;
        if (SoulCore.getAscensionStage(player) < 4) return;
        if (SoulCore.getSoulEssence(player) < 3000) return;

        int cooldown = player.getPersistentData().getInt(NBT_COMMANDEER_COOLDOWN);
        if (cooldown > 0) {
            player.sendSystemMessage(Component.literal("§8Commandeer Shadow on cooldown: §7" + (cooldown / 20) + "s"));
            return;
        }

        // Find nearest living entity in range
        AABB area = player.getBoundingBox().inflate(12);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area, e ->
                e != player && !e.isDeadOrDying());

        if (nearby.isEmpty()) {
            player.sendSystemMessage(Component.literal("§8No targets in range."));
            return;
        }

        // Sort by distance, pick closest
        nearby.sort((a, b) -> Double.compare(
                a.distanceToSqr(player),
                b.distanceToSqr(player)));

        int ascensionStage = SoulCore.getAscensionStage(player);
        int maxTargets = ascensionStage >= 6 ? 3 : ascensionStage >= 4 ? 1 : 1;
        int bound = 0;

        net.minecraft.nbt.CompoundTag data = player.getPersistentData();
        net.minecraft.nbt.ListTag targets = data.contains(NBT_COMMANDEERED_TARGETS)
                ? data.getList(NBT_COMMANDEERED_TARGETS, net.minecraft.nbt.Tag.TAG_COMPOUND)
                : new net.minecraft.nbt.ListTag();

        for (LivingEntity target : nearby) {
            if (bound >= maxTargets) break;

            net.minecraft.nbt.CompoundTag entry = new net.minecraft.nbt.CompoundTag();
            entry.putUUID("uuid", target.getUUID());
            entry.putInt("duration", 200 + ascensionStage * 20); // ~10-14s base
            targets.add(entry);
            bound++;

            // Visual feedback on target
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX(), target.getY(), target.getZ(),
                    16, 0.4, 0.5, 0.4, 0.02);

            player.sendSystemMessage(Component.literal("§8Shadow commandeered: §7" + target.getName().getString()));
        }

        data.put(NBT_COMMANDEERED_TARGETS, targets);
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 3000);
        player.getPersistentData().putInt(NBT_COMMANDEER_COOLDOWN, 60); // 20s cooldown
        player.playNotifySound(SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.PLAYERS, 1.0f, 0.5f);
    }

    // Ability 6 — Cull of Spiritual Flesh: black greatsword strike that ignores armor and applies Degeneration DoT
    public static void shepardCullOfSpiritualFlesh(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassShepard(player, bypassClassCheck)) return;
        if (SoulCore.getAscensionStage(player) < 5) return;
        if (SoulCore.getSoulEssence(player) < 4500) return;

        int cooldown = player.getPersistentData().getInt(NBT_CULL_COOLDOWN);
        if (cooldown > 0) {
            player.sendSystemMessage(Component.literal("§8Cull on cooldown: §7" + (cooldown / 20) + "s"));
            return;
        }

        int ascensionStage = SoulCore.getAscensionStage(player);

        // Cone of targets in front of player within 8 blocks
        AABB area = player.getBoundingBox().inflate(8);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area, e ->
                e != player && !e.isDeadOrDying());

        if (nearby.isEmpty()) {
            player.sendSystemMessage(Component.literal("§8No targets in range."));
            return;
        }

        net.minecraft.nbt.CompoundTag data = player.getPersistentData();
        net.minecraft.nbt.ListTag cullTargets = data.contains(NBT_CULL_TARGETS)
                ? data.getList(NBT_CULL_TARGETS, net.minecraft.nbt.Tag.TAG_COMPOUND)
                : new net.minecraft.nbt.ListTag();

        for (LivingEntity target : nearby) {
            // Initial armor-bypassing strike
            float initialDamage = 6.0f + ascensionStage * 1.5f;
            target.hurt(sl.damageSources().magic(), initialDamage);

            // Apply Degeneration DoT tag
            net.minecraft.nbt.CompoundTag entry = new net.minecraft.nbt.CompoundTag();
            entry.putUUID("uuid", target.getUUID());
            entry.putInt("duration", 100 + ascensionStage * 10); // 5-10s DoT
            cullTargets.add(entry);

            // Explosive soul particles at each target
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX(), target.getY() + 1.0, target.getZ(),
                    20, 0.5, 0.5, 0.5, 0.05);
        }

        data.put(NBT_CULL_TARGETS, cullTargets);
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 4500);
        player.getPersistentData().putInt(NBT_CULL_COOLDOWN, 40); // 30s cooldown
        player.playNotifySound(SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0f, 0.3f);
        player.sendSystemMessage(Component.literal("§8Cull of Spiritual Flesh unleashed."));

        // Screen shake / visual cue for the caster
        sl.sendParticles(ParticleTypes.SCULK_SOUL,
                player.getX(), player.getY() + 1.0, player.getZ(),
                30, 1.0, 1.0, 1.0, 0.1);
    }
    // Ability 7 — Shadow Chrysalis: encase nearest target in a shadow prison that absorbs damage and explodes
    public static void shepardShadowChrysalis(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassShepard(player, bypassClassCheck)) return;
        if (SoulCore.getAscensionStage(player) < 6) return;
        if (SoulCore.getSoulEssence(player) < 5500) return;

        int cooldown = player.getPersistentData().getInt(NBT_CHRYSALIS_COOLDOWN);
        if (cooldown > 0) {
            player.sendSystemMessage(Component.literal("§8Shadow Chrysalis on cooldown: §7" + (cooldown / 20) + "s"));
            return;
        }

        int ascensionStage = SoulCore.getAscensionStage(player);

        // Find nearest target within 10 blocks
        AABB area = player.getBoundingBox().inflate(10);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area, e ->
                e != player && !e.isDeadOrDying());

        if (nearby.isEmpty()) {
            player.sendSystemMessage(Component.literal("§8No targets in range."));
            return;
        }

        nearby.sort((a, b) -> Double.compare(a.distanceToSqr(player), b.distanceToSqr(player)));
        LivingEntity target = nearby.get(0);

        net.minecraft.nbt.CompoundTag data = player.getPersistentData();
        net.minecraft.nbt.ListTag chrysalisTargets = data.contains(NBT_CHRYSALIS_TARGETS)
                ? data.getList(NBT_CHRYSALIS_TARGETS, net.minecraft.nbt.Tag.TAG_COMPOUND)
                : new net.minecraft.nbt.ListTag();

        net.minecraft.nbt.CompoundTag entry = new net.minecraft.nbt.CompoundTag();
        entry.putUUID("uuid", target.getUUID());
        entry.putInt("duration", 100 + ascensionStage * 15); // ~5-8s
        entry.putFloat("storedDamage", 0f);
        chrysalisTargets.add(entry);

        data.put(NBT_CHRYSALIS_TARGETS, chrysalisTargets);
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 5500);
        player.getPersistentData().putInt(NBT_CHRYSALIS_COOLDOWN, 20); // 40s cooldown

        // Cast particles spiraling upward around target
        for (int t = 0; t < 20; t++) {
            double angle = Math.toRadians(t * 18);
            double height = t * 0.1;
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    target.getX() + Math.cos(angle) * 0.9,
                    target.getY() + height,
                    target.getZ() + Math.sin(angle) * 0.9,
                    1, 0, 0, 0, 0);
        }

        player.playNotifySound(SoundEvents.SCULK_SENSOR_BREAK, SoundSource.PLAYERS, 1.0f, 0.4f);
        player.sendSystemMessage(Component.literal("§8Shadow Chrysalis formed around §7" + target.getName().getString()));
    }
}
