package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
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
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * =====================================================================
 *  AETHER WARDEN — Full 7-Ability Implementation
 * =====================================================================
 *
 *  Role    : Territorial control / sustain DPS
 *  Theme   : Mystical spatial dominator
 *  Strength: Unstoppable inside own circle, strong sustain, AOE pressure
 *  Weakness: Weak outside circle, requires positioning discipline
 *
 *  ── Core Mechanic: AETHER CIRCLE ─────────────────────────────────
 *  A circle of radius scaled by ascension stage always follows the player.
 *  Most abilities only function on enemies inside the circle.
 *
 *  Circle radii by ascension stage:
 *    1 → 4 blocks   2 → 6    3 → 8    4 → 10   5 → 14   6 → 18   7 → 24
 *
 *  ── Core Mechanic: AETHER STACKS ────────────────────────────────
 *  Applied to enemies inside the circle by most abilities.
 *  Stored on the TARGET entity.
 *    "AetherStacks"   — int : 0–5 stacks
 *    "AetherTimer"    — int : ticks remaining before stacks expire
 *  Each stack: +6% damage taken from the Aether Warden.
 *  At 5 stacks: Aether Burst — deals bonus damage and resets stacks.
 *
 *  ── Player NBT keys (stored on PLAYER) ──────────────────────────
 *    "AetherLinkTarget"       — int  : entity ID of tethered target
 *    "AetherLinkChain1/2"     — int  : entity IDs of chain link targets (high tier)
 *    "AetherLinkTimer"        — int  : ticks remaining on current link
 *    "AetherCompressionActive"— int  : ticks remaining on Aether Compression
 *    "ContainmentActive"      — int  : ticks of Containment Field
 *    "OrbitShards"            — int  : number of active orbiting shards
 *    "OrbitAngle"             — float: current rotation angle of shard orbit
 *    "AetherDominionActive"   — int  : ticks of Aether Dominion (ultimate)
 *    "Ability1CD"             — int  : Aether Link cooldown
 *    "Ability2CD"             — int  : Gravitic Pull cooldown
 *    "Ability3CD"             — int  : Aether Compression cooldown
 *    "Ability4CD"             — int  : Orbiting Fragments cooldown
 *    "Ability5CD"             — int  : Containment Field cooldown
 *    "Ability6CD"             — int  : Dimensional Tear cooldown
 *    "Ability7CD"             — int  : Aether Dominion cooldown
 *
 *  ── Soul Essence costs ──────────────────────────────────────────
 *    Ability 1 — Aether Link          : 300
 *    Ability 2 — Gravitic Pull        : 250
 *    Ability 3 — Aether Compression   : 350
 *    Ability 4 — Orbiting Fragments   : 300
 *    Ability 5 — Containment Field    : 400
 *    Ability 6 — Dimensional Tear     : 450
 *    Ability 7 — Aether Dominion      : 1000
 *
 *  ── Cooldowns (ticks) ───────────────────────────────────────────
 *    Ability 1:  240  (12 s)
 *    Ability 2:  180  (9 s)
 *    Ability 3:  300  (15 s)
 *    Ability 4:  400  (20 s)
 *    Ability 5:  300  (15 s)
 *    Ability 6:  220  (11 s)
 *    Ability 7:  2400 (2 min)
 * =====================================================================
 */
public class AetherWarden {

    // ─────────────────────────────────────────────────────────────────
    //  CONSTANTS
    // ─────────────────────────────────────────────────────────────────

    private static final int   MAX_AETHER_STACKS      = 5;
    private static final int   AETHER_STACK_DURATION  = 160; // 8 seconds
    private static final float AETHER_STACK_DMG_BONUS = 0.06f; // +6% per stack
    private static final float AETHER_BURST_DMG       = 18.0f;

    /** Radii indexed by ascension stage (index 0 unused, stages 1–7). */
    private static final double[] CIRCLE_RADII = { 0, 4, 6, 8, 10, 14, 18, 24 };

    // ─────────────────────────────────────────────────────────────────
    //  HELPERS — Circle
    // ─────────────────────────────────────────────────────────────────

    /** Returns the current circle radius for the player based on ascension stage. */
    public static double getCircleRadius(Player player) {
        int stage = Math.min(7, Math.max(1, SoulCore.getAscensionStage(player)));
        return CIRCLE_RADII[stage];
    }

    /** Returns true if the given entity is inside the player's Aether Circle. */
    public static boolean isInCircle(Player player, LivingEntity entity) {
        double radius = getCircleRadius(player);
        double dx = entity.getX() - player.getX();
        double dz = entity.getZ() - player.getZ();
        return (dx * dx + dz * dz) <= (radius * radius);
    }

    /** Returns all living entities inside the player's Aether Circle. */
    public static List<LivingEntity> getEntitiesInCircle(Player player, ServerLevel level) {
        double r = getCircleRadius(player);
        AABB box = new AABB(
                player.getX() - r, player.getY() - 4, player.getZ() - r,
                player.getX() + r, player.getY() + 8, player.getZ() + r);
        return level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive() && isInCircle(player, e));
    }

    // ─────────────────────────────────────────────────────────────────
    //  HELPERS — Aether Stacks
    // ─────────────────────────────────────────────────────────────────

    /**
     * Applies {@code amount} Aether Stacks to a target.
     * At MAX_AETHER_STACKS triggers Aether Burst and resets stacks.
     */
    public static void applyAetherStacks(LivingEntity target, Player source, int amount) {
        if (target.level().isClientSide) return;
        if (!(target.level() instanceof ServerLevel level)) return;

        CompoundTag nbt = target.getPersistentData();
        int stacks      = nbt.getInt("AetherStacks") + amount;

        if (stacks >= MAX_AETHER_STACKS) {
            // ── AETHER BURST ─────────────────────────────────────────
            nbt.putInt("AetherStacks", 0);
            nbt.remove("AetherTimer");

            int ascension = SoulCore.getAscensionStage(source);
            float dmg     = AETHER_BURST_DMG + ascension * 2.0f;
            target.hurt(level.damageSources().playerAttack(source), dmg);

            level.sendParticles(ParticleTypes.END_ROD,
                    target.getX(), target.getY() + 1, target.getZ(),
                    20, 0.5, 0.6, 0.5, 0.12);
            level.sendParticles(ParticleTypes.PORTAL,
                    target.getX(), target.getY() + 1, target.getZ(),
                    10, 0.4, 0.4, 0.4, 0.08);
            level.playSound(null, target.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.9f, 1.4f);
        } else {
            nbt.putInt("AetherStacks", stacks);
            nbt.putInt("AetherTimer",  AETHER_STACK_DURATION);

            level.sendParticles(ParticleTypes.END_ROD,
                    target.getX(), target.getY() + 1.2, target.getZ(),
                    stacks * 2, 0.2, 0.2, 0.2, 0.04);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  EVENT BUS
    // ─────────────────────────────────────────────────────────────────

    @Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        @SubscribeEvent
        public static void onLivingTick(LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity.level().isClientSide) return;
            if (!(entity.level() instanceof ServerLevel level)) return;

            // Aether Stack decay on any entity
            tickAetherStacks(entity, level);

            if (entity instanceof Player player) {
                if (!SoulCore.getAspect(player).equals("Aether Warden")) return;
                tickCooldowns(player);
                tickCircleParticles(player, level);
                tickAetherLink(player, level);
                tickAetherCompression(player, level);
                tickOrbitShards(player, level);
                tickContainmentField(player, level);
                tickAetherDominion(player, level);
                tickDimensionalTearLinger(player, level);
            }
        }

        /** Amplifies damage dealt TO entities that have Aether Stacks. */
        @SubscribeEvent
        public static void onLivingDamage(LivingDamageEvent event) {
            LivingEntity target = event.getEntity();
            if (target.level().isClientSide) return;

            int stacks = target.getPersistentData().getInt("AetherStacks");
            if (stacks > 0) {
                event.setAmount(event.getAmount() * (1.0f + stacks * AETHER_STACK_DMG_BONUS));
            }
        }

        // ── Aether Stack timer decay ──────────────────────────────────
        private static void tickAetherStacks(LivingEntity entity, ServerLevel level) {
            CompoundTag nbt = entity.getPersistentData();
            if (!nbt.contains("AetherTimer")) return;
            int t = nbt.getInt("AetherTimer");
            if (t > 0) {
                nbt.putInt("AetherTimer", t - 1);
            } else {
                nbt.remove("AetherTimer");
                nbt.remove("AetherStacks");
            }
        }

        // ── Cooldowns ─────────────────────────────────────────────────
        private static void tickCooldowns(Player player) {
            CompoundTag nbt = player.getPersistentData();
            for (String key : List.of("Ability1CD", "Ability2CD", "Ability3CD",
                    "Ability4CD", "Ability5CD", "Ability6CD", "Ability7CD")) {
                if (nbt.contains(key)) {
                    int v = nbt.getInt(key);
                    if (v > 0) nbt.putInt(key, v - 1);
                    else nbt.remove(key);
                    if (SoulCore.getAscensionStage(player) == 8) {
                        v = nbt.getInt(key);
                        if (v > 0) nbt.putInt(key, 0);
                        else nbt.remove(key);
                    }
                }
            }
        }

        // ── Circle ambient particles ──────────────────────────────────
        private static void tickCircleParticles(Player player, ServerLevel level) {
            // Only render every 8 ticks to avoid spam
            if (level.getGameTime() % 8 != 0) return;

            double radius    = getCircleRadius(player);
            int    points    = (int)(radius * 3);
            boolean dominion = player.getPersistentData().getInt("AetherDominionActive") > 0;

            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI * i) / points;
                double x     = player.getX() + radius * Math.cos(angle);
                double z     = player.getZ() + radius * Math.sin(angle);
                double y     = player.getY() + 0.15;

                if (dominion) {
                    level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0.1, 0, 0.01);
                } else {
                    level.sendParticles(ParticleTypes.WITCH, x, y, z, SoulCore.getAscensionStage(player), 0, 0.1, 0, 0.01);
                }
            }
        }

        // ── Aether Link — continuous heal/damage tether ───────────────
        private static void tickAetherLink(Player player, ServerLevel level) {
            CompoundTag nbt = player.getPersistentData();
            if (!nbt.contains("AetherLinkTarget")) return;

            int timer = nbt.getInt("AetherLinkTimer");
            if (timer <= 0) {
                clearAetherLink(player);
                return;
            }
            nbt.putInt("AetherLinkTimer", timer - 1);

            int ascension = SoulCore.getAscensionStage(player);

            // Process all linked targets (primary + up to 2 chains at high tier)
            List<Integer> linkedIds = new ArrayList<>();
            linkedIds.add(nbt.getInt("AetherLinkTarget"));
            if (nbt.contains("AetherLinkChain1")) linkedIds.add(nbt.getInt("AetherLinkChain1"));
            if (nbt.contains("AetherLinkChain2")) linkedIds.add(nbt.getInt("AetherLinkChain2"));

            int validLinks = 0;
            float totalHealShare = 0;

            for (int id : linkedIds) {
                LivingEntity target = findEntityById(level, id);
                if (target == null || !target.isAlive()) continue;

                // Break link if target leaves circle
                if (!isInCircle(player, target)) {
                    // Only break the primary link to end the whole ability
                    if (id == nbt.getInt("AetherLinkTarget")) {
                        clearAetherLink(player);
                        level.playSound(null, player.blockPosition(),
                                SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 0.6f, 0.8f);
                        return;
                    }
                    continue;
                }

                // DOT on target every 10 ticks
                if (timer % 10 == 0) {
                    float dot = 1.0f + ascension * 0.4f;
                    target.hurt(level.damageSources().playerAttack(player), dot);
                    applyAetherStacks(target, player, 1);
                }

                // Accumulate heal share
                float healPerLink = (0.3f + ascension * 0.1f);
                totalHealShare += healPerLink;
                validLinks++;

                // Tether particle line
                if (timer % 5 == 0) {
                    drawTetherLine(level, player.getEyePosition(), target.getEyePosition());
                }
            }

            // Heal player split across all active links (every 10 ticks)
            if (validLinks > 0 && timer % 10 == 0) {
                player.heal(totalHealShare);
            }
        }

        // ── Aether Compression — AOE damage field ─────────────────────
        private static void tickAetherCompression(Player player, ServerLevel level) {
            CompoundTag nbt = player.getPersistentData();
            int ticks = nbt.getInt("AetherCompressionActive");
            if (ticks <= 0) return;

            nbt.putInt("AetherCompressionActive", ticks - 1);

            if (ticks % 15 != 0) return; // pulse every 15 ticks

            int ascension    = SoulCore.getAscensionStage(player);
            boolean highTier = ascension >= 5;

            List<LivingEntity> targets = getEntitiesInCircle(player, level);
            for (LivingEntity target : targets) {
                float dmg;
                if (highTier) {
                    // Closer to center = more damage
                    double dist   = target.distanceTo(player);
                    double radius = getCircleRadius(player);
                    float factor  = (float)(1.0 - (dist / radius)); // 0.0 at edge, 1.0 at center
                    dmg = (2.0f + ascension * 0.8f) * (1.0f + factor);
                } else {
                    dmg = 2.0f + ascension * 0.5f;
                }

                target.hurt(level.damageSources().playerAttack(player), dmg);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, false));
                applyAetherStacks(target, player, 1);
            }

            // Compression visual pulse
            level.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1, player.getZ(),
                    12, getCircleRadius(player) * 0.4, 0.5, getCircleRadius(player) * 0.4, 0.06);
        }

        // ── Orbiting Fragments — rotating shards ──────────────────────
        private static void tickOrbitShards(Player player, ServerLevel level) {
            CompoundTag nbt   = player.getPersistentData();
            int shards        = nbt.getInt("OrbitShards");
            if (shards <= 0) return;

            int ascension    = SoulCore.getAscensionStage(player);
            float orbitSpeed = ascension >= 6 ? 0.18f : 0.10f;
            float orbitRadius= 2.5f;

            float angle = nbt.getFloat("OrbitAngle") + orbitSpeed;
            if (angle > 2 * Math.PI) angle -= (float)(2 * Math.PI);
            nbt.putFloat("OrbitAngle", angle);

            for (int i = 0; i < shards; i++) {
                double shardAngle = angle + (2 * Math.PI * i / shards);
                double sx = player.getX() + orbitRadius * Math.cos(shardAngle);
                double sy = player.getY() + 1.2;
                double sz = player.getZ() + orbitRadius * Math.sin(shardAngle);

                level.sendParticles(ParticleTypes.END_ROD, sx, sy, sz, 2, 0.05, 0.05, 0.05, 0.01);

                // Check if any enemy is close enough to the shard position to hit
                AABB shardBox = new AABB(sx - 0.6, sy - 0.6, sz - 0.6,
                        sx + 0.6, sy + 0.6, sz + 0.6);
                List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class, shardBox,
                        e -> e != player && e.isAlive() && isInCircle(player, e));

                for (LivingEntity target : hit) {
                    float dmg = 2.0f + ascension * 0.5f;

                    // High tier: shards explode on contact
                    if (ascension >= 6) {
                        dmg *= 2.5f;
                        level.sendParticles(ParticleTypes.EXPLOSION,
                                sx, sy, sz, 1, 0.1, 0.1, 0.1, 0.05);
                        // Remove one shard on explosion
                        nbt.putInt("OrbitShards", Math.max(0, shards - 1));
                    }

                    target.hurt(level.damageSources().playerAttack(player), dmg);
                    applyAetherStacks(target, player, 1);
                    level.playSound(null, target.blockPosition(),
                            SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.4f, 1.6f);
                    break; // one hit per shard per tick
                }
            }
        }

        // ── Containment Field ─────────────────────────────────────────
        private static void tickContainmentField(Player player, ServerLevel level) {
            CompoundTag nbt = player.getPersistentData();
            int ticks = nbt.getInt("ContainmentActive");
            if (ticks <= 0) return;

            nbt.putInt("ContainmentActive", ticks - 1);

            int    ascension  = SoulCore.getAscensionStage(player);
            double radius     = getCircleRadius(player);
            double edgeBuffer = 1.5; // how close to edge before push kicks in

            List<LivingEntity> targets = getEntitiesInCircle(player, level);

            // Also catch entities JUST outside who are trying to leave
            double bigR = radius + 2;
            AABB bigBox = new AABB(
                    player.getX() - bigR, player.getY() - 4, player.getZ() - bigR,
                    player.getX() + bigR, player.getY() + 8, player.getZ() + bigR);
            List<LivingEntity> nearEdge = level.getEntitiesOfClass(LivingEntity.class, bigBox,
                    e -> e != player && e.isAlive());

            for (LivingEntity target : nearEdge) {
                double dx   = target.getX() - player.getX();
                double dz   = target.getZ() - player.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (dist >= radius - edgeBuffer) {
                    // Push entity back toward center
                    Vec3 inward  = new Vec3(-dx, 0, -dz).normalize().scale(0.55);
                    target.setDeltaMovement(target.getDeltaMovement().add(inward));
                    target.hurtMarked = true;
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, false, false));

                    // High tier: damage on escape attempt
                    if (ascension >= 6 && ticks % 10 == 0) {
                        target.hurt(level.damageSources().playerAttack(player), 3.0f + ascension);
                        applyAetherStacks(target, player, 1);
                    }
                }
            }

            // Edge visual
            if (ticks % 10 == 0) {
                int points = (int)(radius * 4);
                for (int i = 0; i < points; i++) {
                    double a = (2 * Math.PI * i) / points;
                    level.sendParticles(ParticleTypes.END_ROD,
                            player.getX() + radius * Math.cos(a),
                            player.getY() + 0.3,
                            player.getZ() + radius * Math.sin(a),
                            1, 0, 0.3, 0, 0.01);
                }
            }
        }

        // ── Aether Dominion tick ──────────────────────────────────────
        private static void tickAetherDominion(Player player, ServerLevel level) {
            CompoundTag nbt = player.getPersistentData();
            int ticks = nbt.getInt("AetherDominionActive");
            if (ticks <= 0) return;

            nbt.putInt("AetherDominionActive", ticks - 1);

            int    ascension = SoulCore.getAscensionStage(player);
            double radius    = getCircleRadius(player) * 1.5; // overcharged radius

            // Continuous player buffs
            if (!player.getActiveEffects().equals(MobEffects.DAMAGE_RESISTANCE)){
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1, false, false));
            }
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,         30, 1, false, false));

            // Pulse AOE damage + slow every 15 ticks
            if (ticks % 15 == 0) {
                List<LivingEntity> targets = getEntitiesInCircle(player, level);
                for (LivingEntity target : targets) {
                    float dmg = 3.0f + ascension * 0.8f;
                    target.hurt(level.damageSources().playerAttack(player), dmg);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2, false, false));
                    applyAetherStacks(target, player, 1);
                }

                // Auto-apply Aether Link to up to 3 enemies if not already linked
                if (!nbt.contains("AetherLinkTarget")) {
                    List<LivingEntity> inCircle = getEntitiesInCircle(player, level);
                    if (!inCircle.isEmpty()) {
                        nbt.putInt("AetherLinkTarget", inCircle.get(0).getId());
                        nbt.putInt("AetherLinkTimer",  120);
                        if (inCircle.size() >= 2) nbt.putInt("AetherLinkChain1", inCircle.get(1).getId());
                        if (inCircle.size() >= 3) nbt.putInt("AetherLinkChain2", inCircle.get(2).getId());
                    }
                }
            }

            // Final tier: unstable circle — inward pulse every 20 ticks
            if (ascension >= 7 && ticks % 20 == 0) {
                double r = radius;
                AABB bigBox = new AABB(
                        player.getX() - r - 4, player.getY() - 4, player.getZ() - r - 4,
                        player.getX() + r + 4, player.getY() + 8, player.getZ() + r + 4);
                List<LivingEntity> edgeTargets = level.getEntitiesOfClass(LivingEntity.class, bigBox,
                        e -> e != player && e.isAlive());

                for (LivingEntity t : edgeTargets) {
                    // Outward damage pulse
                    float dist = (float) t.distanceTo(player);
                    if (dist <= r + 4) {
                        t.hurt(level.damageSources().playerAttack(player), 4.0f);
                    }
                    // Pull inward
                    Vec3 inward = new Vec3(player.getX() - t.getX(), 0,
                            player.getZ() - t.getZ()).normalize().scale(0.4);
                    t.setDeltaMovement(t.getDeltaMovement().add(inward));
                    t.hurtMarked = true;
                }

                level.sendParticles(ParticleTypes.EXPLOSION,
                        player.getX(), player.getY() + 1, player.getZ(),
                        3, r * 0.4, 0.3, r * 0.4, 0.08);
            }

            // Ambient dominion particles
            if (ticks % 5 == 0) {
                level.sendParticles(ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 1.5, player.getZ(),
                        6, radius * 0.3, 0.8, radius * 0.3, 0.06);
            }

            if (ticks == 0) {
                nbt.remove("AetherDominionActive");
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 1 — AETHER LINK  (Active, 12 s CD, 300 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Attaches a spectral tether to the nearest enemy inside the circle.
     * While linked: player heals, target takes DOT and gains Aether Stacks.
     * Link breaks if either party leaves the circle.
     * High tier (ascension ≥ 5): chains to 2–3 nearby enemies.
     */
    public static void aetherWardenAbilityOneUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "Ability1CD", 240, 300)) return;

        List<LivingEntity> inCircle = getEntitiesInCircle(player, level);
        if (inCircle.isEmpty()) return;

        // Sort by distance — closest first
        inCircle.sort(Comparator.comparingDouble(e -> e.distanceTo(player)));

        CompoundTag nbt   = player.getPersistentData();
        int ascension     = SoulCore.getAscensionStage(player);
        int linkDuration  = 2000 + ascension * 200; // scales with ascension

        nbt.putInt("AetherLinkTarget", inCircle.get(0).getId());
        nbt.putInt("AetherLinkTimer",  linkDuration);
        nbt.remove("AetherLinkChain1");
        nbt.remove("AetherLinkChain2");

        // High tier: chain links
        if (ascension >= 5 && inCircle.size() >= 2) {
            nbt.putInt("AetherLinkChain1", inCircle.get(1).getId());
        }
        if (ascension >= 6 && inCircle.size() >= 3) {
            nbt.putInt("AetherLinkChain2", inCircle.get(2).getId());
        }

        // Apply initial stack on link
        applyAetherStacks(inCircle.get(0), player, 1);

        level.sendParticles(ParticleTypes.END_ROD,
                inCircle.get(0).getX(), inCircle.get(0).getY() + 1, inCircle.get(0).getZ(),
                10, 0.3, 0.5, 0.3, 0.06);
        level.playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 0.7f);


        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-300);
        setCooldown(player, "Ability1CD", 240);
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 2 — GRAVITIC PULL  (Active, 9 s CD, 250 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Pulls all enemies within a wide outer range toward the circle's center.
     * Enemies already inside are slowed. High tier (ascension ≥ 5): brief root.
     */
    public static void aetherWardenAbilityTwoUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "Ability2CD", 180, 250)) return;

        int    ascension  = SoulCore.getAscensionStage(player)*2;
        double pullRadius = getCircleRadius(player) + 6.0; // pull from beyond the circle too
        float  pullForce  = 0.8f + ascension * 0.1f;

        AABB pullBox = new AABB(
                player.getX() - pullRadius, player.getY() - 4, player.getZ() - pullRadius,
                player.getX() + pullRadius, player.getY() + 8, player.getZ() + pullRadius);

        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, pullBox,
                e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            Vec3 toCenter = new Vec3(
                    player.getX() - target.getX(),
                    player.getY() - target.getY(),
                    player.getZ() - target.getZ()).normalize().scale(pullForce);

            target.setDeltaMovement(target.getDeltaMovement().add(toCenter));
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false));

            // High tier: root (Slowness V ≈ root)
            if (ascension >= 5) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 5, false, false));
            }

            applyAetherStacks(target, player, 1);
        }

        // Inward particle burst
        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1, player.getZ(),
                30, pullRadius * 0.3, 1.0, pullRadius * 0.3, 0.1);
        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.5f, 1.8f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-250);
        setCooldown(player, "Ability2CD", 180);
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 3 — AETHER COMPRESSION  (Active, 15 s CD, 350 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Activates a compressed space field inside the circle for 8 seconds.
     * All enemies inside take periodic AOE damage and movement slow.
     * High tier (ascension ≥ 5): damage scales up closer to center.
     * The tick handler (tickAetherCompression) manages ongoing damage.
     */
    public static void aetherWardenAbilityThreeUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "Ability3CD", 300, 350)) return;

        player.getPersistentData().putInt("AetherCompressionActive", 160); // 8 seconds

        level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1, player.getZ(),
                20, getCircleRadius(player) * 0.3, 0.8, getCircleRadius(player) * 0.3, 0.08);
        level.playSound(null, player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7f, 1.2f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-350);
        setCooldown(player, "Ability3CD", 300);
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 4 — ORBITING FRAGMENTS  (Active, 20 s CD, 300 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Summons orbiting void shards that rotate around the player and
     * damage any enemies they contact inside the circle.
     * Shard count scales with ascension. High tier (ascension ≥ 6): shards explode.
     */
    public static void aetherWardenAbilityFourUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "Ability4CD", 400, 300)) return;

        int ascension = SoulCore.getAscensionStage(player);
        int shards    = 2 + (ascension / 2); // 2–5 shards scaling with ascension

        player.getPersistentData().putInt("OrbitShards", shards);
        player.getPersistentData().putFloat("OrbitAngle", 0.0f);

        level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1.5, player.getZ(),
                shards * 3, 1.5, 0.3, 1.5, 0.05);
        level.playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.PLAYERS, 0.8f, 1.0f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-300);
        setCooldown(player, "Ability4CD", 400);
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 5 — CONTAINMENT FIELD  (Active, 15 s CD, 400 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Locks the circle boundary for 5 seconds. Enemies near the edge are
     * pushed back inward and heavily slowed. High tier (ascension ≥ 6): enemies
     * take damage when they try to escape.
     */
    public static void aetherWardenAbilityFiveUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "Ability5CD", 300, 400)) return;

        player.getPersistentData().putInt("ContainmentActive", 100); // 5 seconds

        // Apply stacks to all enemies in circle on activation
        List<LivingEntity> targets = getEntitiesInCircle(player, level);
        for (LivingEntity target : targets) {
            applyAetherStacks(target, player, 1);
        }

        level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 0.1, player.getZ(),
                (int)(getCircleRadius(player) * 5), getCircleRadius(player) * 0.5,
                0.1, getCircleRadius(player) * 0.5, 0.02);
        level.playSound(null, player.blockPosition(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 0.8f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-400);
        setCooldown(player, "Ability5CD", 300);
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 6 — DIMENSIONAL TEAR  (Active, 11 s CD, 450 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * The only ability NOT limited to the circle.
     * Opens a rift at a target point up to 20 blocks away, pulling all
     * enemies near the rift into the player's circle and dealing burst damage.
     * High tier (ascension ≥ 6): leaves a lingering damage trail at the rift point.
     */
    public static void aetherWardenAbilitySixUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "Ability6CD", 220, 450)) return;

        // Target point: 20 blocks along look vector (or until block hit)
        Vec3 eye  = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 riftPos = eye;
        for (double d = 1; d <= 20; d += 0.5) {
            Vec3 check = eye.add(look.scale(d));
            if (!level.getBlockState(net.minecraft.core.BlockPos.containing(check)).isAir()) break;
            riftPos = check;
        }

        int    ascension  = SoulCore.getAscensionStage(player);
        double pullRadius = ascension >= 6 ? 6.0 : 4.0;
        float  pullForce  = 1.2f + ascension * 0.15f;
        float  burstDmg   = 8.0f + ascension * 1.5f;

        // Collect enemies around rift point
        Vec3 finalRiftPos = riftPos;
        AABB riftBox = new AABB(
                riftPos.x - pullRadius, riftPos.y - 3, riftPos.z - pullRadius,
                riftPos.x + pullRadius, riftPos.y + 5, riftPos.z + pullRadius);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, riftBox,
                e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            // Pull toward player's circle
            Vec3 toPlayer = new Vec3(
                    player.getX() - target.getX(),
                    player.getY() - target.getY(),
                    player.getZ() - target.getZ()).normalize().scale(pullForce);
            target.setDeltaMovement(toPlayer);
            target.hurtMarked = true;

            target.hurt(level.damageSources().playerAttack(player), burstDmg);
            applyAetherStacks(target, player, 2);
        }

        // Rift visual
        level.sendParticles(ParticleTypes.PORTAL,
                riftPos.x, riftPos.y + 1, riftPos.z,
                30, pullRadius * 0.4, 1.0, pullRadius * 0.4, 0.12);
        level.sendParticles(ParticleTypes.EXPLOSION,
                riftPos.x, riftPos.y + 1, riftPos.z,
                3, 0.3, 0.3, 0.3, 0.06);
        level.playSound(null, net.minecraft.core.BlockPos.containing(riftPos),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.8f, 1.4f);

        // High tier: linger damage trail stored as a task via NBT on player
        if (ascension >= 6) {
            CompoundTag nbt = player.getPersistentData();
            nbt.putDouble("TearLingerX", riftPos.x);
            nbt.putDouble("TearLingerY", riftPos.y);
            nbt.putDouble("TearLingerZ", riftPos.z);
            nbt.putInt("TearLingerTimer", 60); // 3 seconds
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-450);
        setCooldown(player, "Ability6CD", 220);
    }

    /**
     * Ticks the Dimensional Tear lingering trail. Call from onLivingTick for players,
     * or wire into Events.onLivingTick (already handled below).
     */
    public static void tickDimensionalTearLinger(Player player, ServerLevel level) {
        CompoundTag nbt = player.getPersistentData();
        if (!nbt.contains("TearLingerTimer")) return;

        int t = nbt.getInt("TearLingerTimer");
        if (t <= 0) {
            nbt.remove("TearLingerTimer");
            nbt.remove("TearLingerX");
            nbt.remove("TearLingerY");
            nbt.remove("TearLingerZ");
            return;
        }
        nbt.putInt("TearLingerTimer", t - 1);
        if (t % 10 != 0) return;

        double lx = nbt.getDouble("TearLingerX");
        double ly = nbt.getDouble("TearLingerY");
        double lz = nbt.getDouble("TearLingerZ");

        AABB lingerBox = new AABB(lx - 2, ly - 1, lz - 2, lx + 2, ly + 4, lz + 2);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, lingerBox,
                e -> e != player && e.isAlive());

        for (LivingEntity target : targets) {
            target.hurt(level.damageSources().playerAttack(player), 3.0f);
            applyAetherStacks(target, player, 1);
        }

        level.sendParticles(ParticleTypes.PORTAL, lx, ly + 1, lz, 6, 0.4, 0.5, 0.4, 0.05);
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 7 — AETHER DOMINION  (Ultimate, 2 min CD, 1000 essence)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Transforms the circle for 12 seconds (240 ticks).
     * All tick effects handled in tickAetherDominion in the Events class.
     */
    public static void aetherWardenAbilitySevenUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "Ability7CD", 2400, 1000)) return;

        player.getPersistentData().putInt("AetherDominionActive", 240);

        level.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1, player.getZ(),
                50, getCircleRadius(player) * 0.5, 1.5, getCircleRadius(player) * 0.5, 0.15);
        level.sendParticles(ParticleTypes.EXPLOSION,
                player.getX(), player.getY() + 1, player.getZ(),
                6, 0.6, 0.5, 0.6, 0.08);
        level.playSound(null, player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 0.5f);
        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.0f, 0.6f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player)-1000);
        setCooldown(player, "Ability7CD", 2400);
    }

    // ─────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────

    private static boolean validateUse(Player player, String cdKey, int cdTicks, int essenceCost) {
        if (!SoulCore.getAspect(player).equals("Aether Warden")) return false;
        if (SoulCore.getSoulEssence(player) < essenceCost) return false;
        CompoundTag nbt = player.getPersistentData();
        return !nbt.contains(cdKey) || nbt.getInt(cdKey) <= 0;
    }

    private static void setCooldown(Player player, String cdKey, int ticks) {
        player.getPersistentData().putInt(cdKey, ticks);
    }

    private static void clearAetherLink(Player player) {
        CompoundTag nbt = player.getPersistentData();
        nbt.remove("AetherLinkTarget");
        nbt.remove("AetherLinkChain1");
        nbt.remove("AetherLinkChain2");
        nbt.remove("AetherLinkTimer");
    }

    /** Draws a particle line between two Vec3 positions (tether visual). */
    private static void drawTetherLine(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from);
        double len = step.length();
        Vec3 unit  = step.normalize();
        for (double t = 0; t < len; t += 0.5) {
            Vec3 p = from.add(unit.scale(t));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
    }

    /** Finds a LivingEntity by numeric entity ID. */
    private static LivingEntity findEntityById(ServerLevel level, int id) {
        AABB searchBox = new AABB(-30000, -64, -30000, 30000, 320, 30000);
        List<LivingEntity> all = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e.getId() == id);
        return all.isEmpty() ? null : all.get(0);
    }
}
