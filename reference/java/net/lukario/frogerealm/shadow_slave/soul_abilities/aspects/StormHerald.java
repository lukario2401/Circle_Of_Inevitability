package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * =====================================================================
 *  STORM HERALD — Full 7-Ability Implementation
 * =====================================================================
 *
 *  NBT Keys used on entities:
 *    "StaticCharge"        — int: ticks remaining on the charge debuff
 *    "StaticStacks"        — int: 0-5, stacks of Static applied to the entity
 *    "StormFieldActive"    — long: level game-time when the field expires (stored on PLAYER)
 *    "StormFieldX/Y/Z"     — double: centre of the active Storm Field (stored on PLAYER)
 *    "TempestActive"       — int: ticks remaining on Tempest Ascension (stored on PLAYER)
 *    "TempestLightningCD"  — int: tick countdown between lightning strikes during Tempest
 *    "Ability1CD"          — int: cooldown ticks for Spark Slash
 *    "Ability3CD"          — int: cooldown ticks for Gale Dash
 *    "Ability4CD"          — int: cooldown ticks for Storm Field
 *    "Ability5CD"          — int: cooldown ticks for Chain Lightning
 *    "Ability6CD"          — int: cooldown ticks for Thunder Leap
 *    "Ability7CD"          — int: cooldown ticks for Tempest Ascension
 *
 *  Soul Essence costs (deducted from SoulCore):
 *    Ability 1 — Spark Slash        :  200
 *    Ability 3 — Gale Dash          :  300
 *    Ability 4 — Storm Field        :  500
 *    Ability 5 — Chain Lightning    :  400
 *    Ability 6 — Thunder Leap       :  600
 *    Ability 7 — Tempest Ascension  : 1000
 *
 *  (Ability 2 — Static Charge — is a fully passive system, no cost.)
 *
 *  Cooldowns (in ticks, 20 ticks = 1 second):
 *    Ability 1:  100  (5 s)
 *    Ability 3:  160  (8 s)
 *    Ability 4:  300  (15 s)
 *    Ability 5:  200  (10 s)
 *    Ability 6:  400  (20 s)
 *    Ability 7:  2400 (2 min)
 * =====================================================================
 */
public class StormHerald {

    // ─────────────────────────────────────────────────────────────────
    //  CONSTANTS
    // ─────────────────────────────────────────────────────────────────

    private static final int MAX_STATIC_STACKS   = 5;
    /** Damage dealt when an enemy reaches max Static Stacks and is struck */
    private static final float STATIC_BURST_DMG  = 14.0f;
    /** Ticks of slowness applied per Static stack refresh (Slowness I) */
    private static final int   SLOW_DURATION     = 40;

    // ─────────────────────────────────────────────────────────────────
    //  EVENT BUS — Tick-driven passive logic
    // ─────────────────────────────────────────────────────────────────

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        @SubscribeEvent
        public static void onLivingTick(LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity.level().isClientSide) return;
            if (!(entity.level() instanceof ServerLevel level)) return;

            // ── Passive: Static Charge timer ──────────────────────────
            tickStaticCharge(entity, level);

            // ── Player-only passives ───────────────────────────────────
            if (entity instanceof Player player) {
                tickCooldowns(player);
                tickStormField(player, level);
                tickTempestAscension(player, level);
            }
        }

        // ── Static Charge decay & particle ────────────────────────────
        private static void tickStaticCharge(LivingEntity entity, ServerLevel level) {
            CompoundTag nbt = entity.getPersistentData();

            if (nbt.contains("StaticCharge")) {
                int ticks = nbt.getInt("StaticCharge");
                if (ticks > 0) {
                    nbt.putInt("StaticCharge", ticks - 1);
                    if (ticks % 10 == 0) {
                        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                entity.getX(), entity.getY() + 1.0, entity.getZ(),
                                2, 0.3, 0.3, 0.3, 0.02);
                    }
                } else {
                    nbt.remove("StaticCharge");
                    nbt.remove("StaticStacks");
                }
            }
        }

        // ── Cooldown tick-down ─────────────────────────────────────────
        private static void tickCooldowns(Player player) {
            CompoundTag nbt = player.getPersistentData();
            for (String key : List.of(
                    "Ability1CD", "Ability3CD", "Ability4CD",
                    "Ability5CD", "Ability6CD", "Ability7CD")) {
                if (nbt.contains(key)) {
                    int v = nbt.getInt(key);
                    if (v > 0) nbt.putInt(key, v - 1);
                    else nbt.remove(key);
                    if (SoulCore.getAscensionStage(player)==8){
                        if (v > 0) nbt.putInt(key, 0);
                        else nbt.remove(key);
                    }
                }
            }
        }

        // ── Storm Field — strikes random enemies inside radius ─────────
        private static void tickStormField(Player player, ServerLevel level) {
            CompoundTag nbt = player.getPersistentData();
            if (!nbt.contains("StormFieldActive")) return;

            long expiry = nbt.getLong("StormFieldActive");
            if (level.getGameTime() > expiry) {
                nbt.remove("StormFieldActive");
                nbt.remove("StormFieldX");
                nbt.remove("StormFieldY");
                nbt.remove("StormFieldZ");
                return;
            }

            double cx = nbt.getDouble("StormFieldX");
            double cy = nbt.getDouble("StormFieldY");
            double cz = nbt.getDouble("StormFieldZ");
            int ascension = SoulCore.getAscensionStage(player);
            double radius  = ascension >= 7 ? 8.0 : 5.0;
            // Strike frequency: every 30 ticks base, every 20 at high ascension
            int strikeInterval = ascension >= 7 ? 20 : 30;

            // Use game-time parity as a cheap interval trigger
            if (level.getGameTime() % strikeInterval != 0) return;

            AABB fieldBox = new AABB(cx - radius, cy - 4, cz - radius,
                    cx + radius, cy + 8, cz + radius);

            List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, fieldBox,
                    e -> e != player && e.isAlive());

            // Apply speed to player while inside the field
            double dx = player.getX() - cx;
            double dz = player.getZ() - cz;
            if (Math.sqrt(dx * dx + dz * dz) <= radius) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false));
            }

            if (enemies.isEmpty()) return;

            // Pick a random enemy to strike
            LivingEntity target = enemies.get(level.getRandom().nextInt(enemies.size()));

            // Slow all enemies in field
            for (LivingEntity e : enemies) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
            }

            // Strike the chosen target
            float strikeDmg = 4.0f + ascension * 0.5f;
            target.hurt(level.damageSources().playerAttack(player), strikeDmg);
            addStaticStacks(target, player, 1);

            level.sendParticles(ParticleTypes.FLASH,
                    target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.4f, 1.8f);

            // Ambient storm particles around the centre
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    cx, cy + 2, cz, 8, radius * 0.5, 1.0, radius * 0.5, 0.05);
        }

        // ── Tempest Ascension — ambient lightning, aura, jump bursts ──
        private static void tickTempestAscension(Player player, ServerLevel level) {
            CompoundTag nbt = player.getPersistentData();
            if (!nbt.contains("TempestActive")) return;

            int ticks = nbt.getInt("TempestActive");
            if (ticks <= 0) {
                nbt.remove("TempestActive");
                nbt.remove("TempestLightningCD");
                return;
            }
            nbt.putInt("TempestActive", ticks - 1);

            // Continuous speed & strength buff
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,  30, 2, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,    30, 1, false, false));

            // Aura particles
            if (ticks % 4 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        player.getX(), player.getY() + 1, player.getZ(),
                        6, 0.6, 0.8, 0.6, 0.08);
            }

            // Lightning CD countdown
            int lightningCD = nbt.getInt("TempestLightningCD");
            if (lightningCD > 0) {
                nbt.putInt("TempestLightningCD", lightningCD - 1);
                return;
            }

            // Strike a random nearby enemy
            int ascension = SoulCore.getAscensionStage(player);
            double range  = 8.0 + ascension;
            AABB aura = new AABB(
                    player.getX() - range, player.getY() - 2, player.getZ() - range,
                    player.getX() + range, player.getY() + 4, player.getZ() + range);

            List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, aura,
                    e -> e != player && e.isAlive());

            if (!nearby.isEmpty()) {
                LivingEntity target = nearby.get(level.getRandom().nextInt(nearby.size()));
                float dmg = 6.0f + ascension;
                target.hurt(level.damageSources().playerAttack(player), dmg);
                // Slight heal for the player
                player.heal(1.0f);
                addStaticStacks(target, player, 2);

                level.sendParticles(ParticleTypes.FLASH,
                        target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
                level.playSound(null, player.blockPosition(),
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5f, 1.6f);
            }

            // Reset lightning CD (faster at higher ascension)
            int nextCD = Math.max(10, 25 - ascension);
            nbt.putInt("TempestLightningCD", nextCD);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 1 — SPARK SLASH  (Active, ~5 s CD, 200 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Fires a short lightning arc from the player's eye, hitting 1–2 enemies
     * (3 at ascension ≥ 6). Each hit enemy receives a Static stack and light damage.
     * If the target already has a Static stack it takes bonus damage.
     */
    public static void stormHeraldAbilityOneUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "Ability1CD", 100, 200)) return;

        Vec3 eyePos  = player.getEyePosition();
        Vec3 look    = player.getLookAngle();
        int maxHits  = SoulCore.getAscensionStage(player) >= 6 ? 3 : 2;

        Set<Integer> hitIds = new HashSet<>();

        for (double i = 1; i < 12; i += 0.5) {
            Vec3 checkPos = eyePos.add(look.scale(i));

            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    checkPos.x, checkPos.y, checkPos.z, 2, 0.1, 0.1, 0.1, 0.02);

            AABB box = centeredBox(checkPos, 0.75);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != player && e.isAlive() && !hitIds.contains(e.getId()));

            for (LivingEntity target : targets) {
                if (hitIds.size() >= maxHits) break;

                CompoundTag nbt    = target.getPersistentData();
                int stacks         = nbt.getInt("StaticStacks");
                float damage       = (stacks > 0) ? 10.0f : 4.0f;

                target.hurt(level.damageSources().playerAttack(player), damage);
                addStaticStacks(target, player, 1);
                hitIds.add(target.getId());

                level.sendParticles(ParticleTypes.FLASH,
                        target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
            }

            if (!level.getBlockState(BlockPos.containing(checkPos)).isAir()) break;
            if (hitIds.size() >= maxHits) break;
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.6f, 2.0f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-200);
        setCooldown(player, "Ability1CD", 100);
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 2 — STATIC CHARGE  (Passive — driven by addStaticStacks)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Pure passive — no activation method needed.
     * Called internally by every ability that applies Static.
     * At MAX_STATIC_STACKS the enemy is burst-struck and optionally chains.
     */
    public static void addStaticStacks(LivingEntity target, Player source, int amount) {
        if (target.level().isClientSide) return;
        if (!(target.level() instanceof ServerLevel level)) return;

        CompoundTag nbt = target.getPersistentData();
        int stacks = nbt.getInt("StaticStacks") + amount;

        if (stacks >= MAX_STATIC_STACKS) {
            // ── MAX STACKS: burst strike ───────────────────────────────
            stacks = 0;
            nbt.putInt("StaticStacks", 0);
            nbt.remove("StaticCharge");

            target.hurt(level.damageSources().playerAttack(source), STATIC_BURST_DMG);
            level.sendParticles(ParticleTypes.FLASH,
                    target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.5f);

            // Late upgrade: chain burst to nearby enemies (ascension ≥ 7)
            if (SoulCore.getAscensionStage(source) >= 7) {
                AABB chainBox = centeredBox(target.position(), 4.0);
                List<LivingEntity> chainTargets = level.getEntitiesOfClass(LivingEntity.class, chainBox,
                        e -> e != target && e != source && e.isAlive());
                for (LivingEntity chained : chainTargets) {
                    chained.hurt(level.damageSources().playerAttack(source), STATIC_BURST_DMG * 0.5f);
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            chained.getX(), chained.getY() + 1, chained.getZ(),
                            3, 0.2, 0.2, 0.2, 0.05);
                }
            }

        } else {
            // ── Stack up & apply slowing charge ───────────────────────
            nbt.putInt("StaticStacks", stacks);

            int slowAmp = Math.max(0, stacks - 1); // 0–3 slowness amplifier scaling with stacks
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, SLOW_DURATION, slowAmp, false, false));

            // Refresh the visible charge timer
            int duration = 100 + (20 * SoulCore.getAscensionStage(source));
            nbt.putInt("StaticCharge", duration);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 3 — GALE DASH  (Active, ~8 s CD, 300 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Dashes the player 6 blocks (8 at ascension ≥ 5) in their look direction.
     * Enemies in the dash path are knocked aside and receive 2 Static stacks.
     * At ascension ≥ 7 a wind trail (Slowness II) is left behind for 3 seconds.
     */
    public static void stormHeraldAbilityThreeUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "Ability3CD", 160, 300)) return;

        int ascension    = SoulCore.getAscensionStage(player);
        double dashDist  = ascension >= 5 ? 10.0 : 6.0;
        Vec3 look        = player.getLookAngle();
        Vec3 dashVel     = look.multiply(1, 0.3, 1).normalize().scale(dashDist * 0.5);

        player.setDeltaMovement(dashVel);
        player.hurtMarked = true; // forces velocity sync to clients

        // Sweep along dash path for enemies
        Vec3 start = player.position();
        for (double t = 0.5; t <= dashDist; t += 0.5) {
            Vec3 pos = start.add(look.multiply(1, 0, 1).normalize().scale(t));

            // Wind trail particles
            level.sendParticles(ParticleTypes.CLOUD,
                    pos.x, pos.y + 0.5, pos.z, 2, 0.2, 0.3, 0.2, 0.01);

            AABB box = centeredBox(pos, 1.2);
            List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != player && e.isAlive());

            for (LivingEntity target : hit) {
                // Knockback perpendicular to dash
                Vec3 knockback = new Vec3(-look.z, 0.4, look.x).normalize().scale(0.8);
                target.setDeltaMovement(target.getDeltaMovement().add(knockback));
                target.hurtMarked = true;
                addStaticStacks(target, player, 2);
            }

            // Late upgrade: leave a slowing wind trail
            if (ascension >= 7) {
                List<LivingEntity> trailHit = level.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e != player && e.isAlive());
                for (LivingEntity trailed : trailHit) {
                    trailed.addEffect(new MobEffectInstance(
                            MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false));
                }
            }
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, 1.0f, 1.4f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-300);
        setCooldown(player, "Ability3CD", 160);
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 4 — STORM FIELD  (Active, ~15 s CD, 500 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Places a 5-block (8 at ascension ≥ 7) storm zone centred on the player
     * for 6 seconds. The tick handler (tickStormField) manages ongoing strikes,
     * slowness, and the player speed bonus. This method just initialises the field.
     */
    public static void stormHeraldAbilityFourUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "Ability4CD", 300, 500)) return;

        CompoundTag nbt  = player.getPersistentData();
        long expiry      = level.getGameTime() + 120L; // 6 seconds = 120 ticks

        nbt.putLong("StormFieldActive", expiry);
        nbt.putDouble("StormFieldX", player.getX());
        nbt.putDouble("StormFieldY", player.getY());
        nbt.putDouble("StormFieldZ", player.getZ());

        // Announce the field visually
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 1, player.getZ(),
                30, 4.0, 1.0, 4.0, 0.05);
        level.playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 0.8f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-500);
        setCooldown(player, "Ability4CD", 300);
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 5 — CHAIN LIGHTNING  (Active, ~10 s CD, 400 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Fires a bolt at the nearest enemy in a 20-block cone, then chains to
     * up to 4 additional targets (5 total; 7 at ascension ≥ 7).
     * Each jump applies 1 Static stack and deals slightly less damage.
     */
    public static void stormHeraldAbilityFiveUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "Ability5CD", 200, 400)) return;

        int ascension  = SoulCore.getAscensionStage(player);
        int maxTargets = ascension >= 7 ? 7 : 5;
        float baseDmg  = 10.0f + ascension;

        // Find initial target via ray-march
        LivingEntity firstTarget = findClosestInSight(player, level, 20.0);
        if (firstTarget == null) return;

        Set<Integer> chainedIds = new HashSet<>();
        chainedIds.add(firstTarget.getId());

        LivingEntity current = firstTarget;
        float damage         = baseDmg;

        for (int jump = 0; jump < maxTargets; jump++) {
            current.hurt(level.damageSources().playerAttack(player), damage);
            addStaticStacks(current, player, 1);

            level.sendParticles(ParticleTypes.FLASH,
                    current.getX(), current.getY() + 1, current.getZ(), 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    current.getX(), current.getY() + 1, current.getZ(),
                    5, 0.3, 0.5, 0.3, 0.05);

            // Find the next closest unchained enemy within 6 blocks of current
            LivingEntity next = findClosestNeighbour(current, level, player, chainedIds, 6.0);
            if (next == null) break;

            // Draw a spark "arc" between current and next
            drawArc(level, current.getEyePosition(), next.getEyePosition());

            chainedIds.add(next.getId());
            current = next;
            damage  = Math.max(2.0f, damage * 0.75f); // 25% falloff per jump
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8f, 1.4f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-400);
        setCooldown(player, "Ability5CD", 200);
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 6 — THUNDER LEAP  (Active, ~20 s CD, 600 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Launches the player upward. On landing (detected in the tick handler below
     * via a flag approach), a shockwave deals heavy AOE damage, instantly maxes
     * Static stacks on all targets, and — at ascension ≥ 7 — follows up with
     * additional lightning strikes.
     *
     * Implementation note: the landing shockwave is detected by watching for the
     * player to touch the ground after the leap flag is set; this is handled in
     * the separate ThunderLeapLandingCheck event below.
     */
    public static void stormHeraldAbilitySixUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "Ability6CD", 400, 600)) return;


        //needs to be tuned
        player.teleportTo(player.getX(),player.getY()+0.2,player.getZ());

        // Launch upward
        Vec3 vel = player.getDeltaMovement();
        player.setDeltaMovement(vel.x * 0.5, 1.6, vel.z * 0.5);
        player.hurtMarked = true;

        // Mark the player so the landing handler can fire the shockwave
        player.getPersistentData().putBoolean("ThunderLeapActive", true);

        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY(), player.getZ(),
                12, 1.0, 0.2, 1.0, 0.08);
        level.playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.0f, 0.7f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-600);
        setCooldown(player, "Ability6CD", 400);
    }

    /** Detects landing and fires the Thunder Leap shockwave. */
    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ThunderLeapLanding {

        @SubscribeEvent
        public static void onLandingTick(LivingEvent.LivingTickEvent event) {
            if (!(event.getEntity() instanceof Player player)) return;
            if (player.level().isClientSide) return;
            if (!(player.level() instanceof ServerLevel level)) return;

            CompoundTag nbt = player.getPersistentData();
            if (!nbt.getBoolean("ThunderLeapActive")) return;
            if (!player.onGround()) return;

            // Player has just landed — fire shockwave
            nbt.remove("ThunderLeapActive");
            fireThunderShockwave(player, level);
        }

        private static void fireThunderShockwave(Player player, ServerLevel level) {
            int ascension  = SoulCore.getAscensionStage(player);
            double radius  = ascension >= 7 ? 7.0 : 5.0;
            float shockDmg = 14.0f + ascension * 1.5f;

            AABB impactBox = centeredBox(player.position(), radius);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, impactBox,
                    e -> e != player && e.isAlive());

            for (LivingEntity target : targets) {
                target.hurt(level.damageSources().playerAttack(player), shockDmg);

                // Instantly max out Static stacks
                CompoundTag tnbt = target.getPersistentData();
                tnbt.putInt("StaticStacks", MAX_STATIC_STACKS - 1);
                addStaticStacks(target, player, 1); // this pushes it to burst

                // Extra knockback outward from impact
                Vec3 knockDir = target.position().subtract(player.position()).normalize();
                target.setDeltaMovement(knockDir.scale(1.2).add(0, 0.5, 0));
                target.hurtMarked = true;
            }

            // Shockwave particles
            level.sendParticles(ParticleTypes.EXPLOSION,
                    player.getX(), player.getY(), player.getZ(),
                    4, radius * 0.4, 0.2, radius * 0.4, 0.2);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    24, radius * 0.5, 0.5, radius * 0.5, 0.1);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.2f, 0.6f);

            // Late upgrade: follow-up lightning strikes on each target
            if (ascension >= 7) {
                for (LivingEntity target : targets) {
                    target.hurt(level.damageSources().playerAttack(player), 8.0f);
                    level.sendParticles(ParticleTypes.FLASH,
                            target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
                }
                level.playSound(null, player.blockPosition(),
                        SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1.0f, 1.2f);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 7 — TEMPEST ASCENSION  (Ultimate, 2 min CD, 1000 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Activates a 10-second (200 tick) storm avatar state.
     * All ongoing effects (continuous strikes, healing, speed, aura) are managed
     * by tickTempestAscension() in the Events class above.
     */
    public static void stormHeraldAbilitySevenUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "Ability7CD", 2400, 1000)) return;

        CompoundTag nbt = player.getPersistentData();
        nbt.putInt("TempestActive", 200);       // 10 seconds
        nbt.putInt("TempestLightningCD", 0);    // strike immediately

        // Dramatic activation burst
        level.sendParticles(ParticleTypes.FLASH,
                player.getX(), player.getY() + 1, player.getZ(), 3, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 1, player.getZ(),
                40, 1.5, 1.5, 1.5, 0.15);
        level.playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.5f, 0.5f);


        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-1000);
        setCooldown(player, "Ability7CD", 2400);
    }

    // ─────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Returns true if the player has the right aspect, enough soul essence,
     * and the given cooldown has expired. Does NOT deduct essence or set the CD —
     * the caller is responsible for doing that after the ability fires.
     */
    private static boolean validateUse(Player player, String cdKey, int cdTicks, int essenceCost) {
        if (!SoulCore.getAspect(player).equals("Storm Herald")) return false;
        if (SoulCore.getSoulEssence(player) < essenceCost) return false;
        CompoundTag nbt = player.getPersistentData();
        return !nbt.contains(cdKey) || nbt.getInt(cdKey) <= 0;
    }

    private static void setCooldown(Player player, String cdKey, int ticks) {
        player.getPersistentData().putInt(cdKey, ticks);
    }

    /** Creates a square AABB centred on pos with the given half-size. */
    private static AABB centeredBox(Vec3 pos, double halfSize) {
        return new AABB(
                pos.x - halfSize, pos.y - halfSize, pos.z - halfSize,
                pos.x + halfSize, pos.y + halfSize, pos.z + halfSize);
    }

    /**
     * Marches a ray from the player's eye and returns the first LivingEntity
     * found within maxDistance, or null if none.
     */
    private static LivingEntity findClosestInSight(Player player, ServerLevel level, double maxDist) {
        Vec3 eye  = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        for (double d = 1.0; d <= maxDist; d += 0.5) {
            Vec3 pos = eye.add(look.scale(d));
            if (!level.getBlockState(BlockPos.containing(pos)).isAir()) break;
            AABB box  = centeredBox(pos, 0.9);
            List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != player && e.isAlive());
            if (!found.isEmpty()) return found.get(0);
        }
        return null;
    }

    /**
     * Finds the closest living entity to {@code origin} within {@code radius},
     * excluding already-chained IDs and the player.
     */
    private static LivingEntity findClosestNeighbour(
            LivingEntity origin, ServerLevel level, Player player,
            Set<Integer> excluded, double radius) {

        AABB box = centeredBox(origin.position(), radius);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && !excluded.contains(e.getId()));
        if (candidates.isEmpty()) return null;
        candidates.sort(Comparator.comparingDouble(e -> e.distanceToSqr(origin)));
        return candidates.get(0);
    }

    /**
     * Draws a particle arc between two Vec3 positions to represent a
     * chain-lightning bolt visually.
     */
    private static void drawArc(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from);
        double len = step.length();
        Vec3 unit  = step.normalize();
        for (double t = 0; t < len; t += 0.4) {
            Vec3 p = from.add(unit.scale(t));
            // Slight random wobble for a lightning look
            double wx = (level.getRandom().nextDouble() - 0.5) * 0.3;
            double wy = (level.getRandom().nextDouble() - 0.5) * 0.3;
            double wz = (level.getRandom().nextDouble() - 0.5) * 0.3;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    p.x + wx, p.y + wy, p.z + wz, 1, 0, 0, 0, 0);
        }
    }
}
