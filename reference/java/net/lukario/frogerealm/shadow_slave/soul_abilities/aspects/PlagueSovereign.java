package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * Plague Sovereign
 * -----------------------------------------------
 * Theme: plague, decay, corruption, epidemic
 * Role: DPS (sustained DoT + scaling infection)
 *
 * Core Mechanic — INFECTION STAGES (per enemy, stored on entity NBT):
 *   0 → Clean
 *   1 → Infected    — mild DoT (1.5/s)
 *   2 → Festering   — stronger DoT (3/s), Slowness I
 *   3 → Rotting     — heavy DoT (5.5/s), Slowness II + Weakness I, spreads on death
 *   4 → Plague Host — extreme DoT (9/s), Slowness III + Weakness II, death explosion
 *
 * Core Resource — PLAGUE ESSENCE (0–100):
 *   Harvested from dying infected enemies.
 *   Decays slowly when idle.
 *   Powers mutations and amplifies abilities.
 *
 * Mutations (built from Plague Essence, persisted in player NBT):
 *   "MutVirulence"  (0–3) → spread radius +1 per rank, spread chance +15% per rank
 *   "MutNecrosis"   (0–3) → DoT multiplier +20% per rank
 *   "MutPutrefaction" (0–3) → death explosion radius +1 and damage +25% per rank
 *
 * Player NBT keys:
 *   "PlagueEssence"       → float  current essence (0–100)
 *   "PlagueEssenceDecay"  → int    ticks until next decay
 *   "MutVirulence"        → int    0–3
 *   "MutNecrosis"         → int    0–3
 *   "MutPutrefaction"     → int    0–3
 *   "PlagueSovereign"     → int    ticks remaining on Sovereign's Plague ultimate
 *
 * Entity NBT keys (stored on the entity):
 *   "PlagueStage"         → int    0–4
 *   "PlagueDoTTimer"      → int    ticks until next DoT tick
 *   "PlagueOwner"         → UUID   player who infected this entity
 */
public class PlagueSovereign {

    // ─── Infection stage constants ─────────────────────────────────────────────
    public static final int STAGE_CLEAN       = 0;
    public static final int STAGE_INFECTED    = 1;
    public static final int STAGE_FESTERING   = 2;
    public static final int STAGE_ROTTING     = 3;
    public static final int STAGE_HOST        = 4;

    // ─── NBT keys (player) ────────────────────────────────────────────────────
    private static final String NBT_ESSENCE        = "PlagueEssence";
    private static final String NBT_ESSENCE_DECAY  = "PlagueEssenceDecay";
    private static final String NBT_MUT_VIR        = "MutVirulence";
    private static final String NBT_MUT_NEC        = "MutNecrosis";
    private static final String NBT_MUT_PUT        = "MutPutrefaction";
    private static final String NBT_SOVEREIGN      = "PlagueSovereign";

    // ─── NBT keys (entity) ────────────────────────────────────────────────────
    private static final String NBT_STAGE          = "PlagueStage";
    private static final String NBT_DOT_TIMER      = "PlagueDoTTimer";
    private static final String NBT_OWNER          = "PlagueOwner";

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final float ESSENCE_MAX         = 100f;
    private static final int   ESSENCE_DECAY_TICKS = 60;   // 1 essence lost every 3s
    private static final int   DOT_INTERVAL        = 20;   // DoT ticks every second
    private static final int   SOVEREIGN_DURATION  = 200;  // 10 seconds

    // DoT damage per stage per tick (before mutations)
    private static final float[] STAGE_DOT = { 0f, 1.5f, 3.0f, 5.5f, 9.0f };

    // Spread radius base (blocks) — Virulence adds +1 per rank
    private static final float BASE_SPREAD_RADIUS  = 4f;
    // Spread chance base — Virulence adds +15% per rank
    private static final float BASE_SPREAD_CHANCE  = 0.30f;

    // Mutation upgrade costs (essence)
    private static final float MUT_COST_VIR = 25f;
    private static final float MUT_COST_NEC = 30f;
    private static final float MUT_COST_PUT = 35f;

    private static final Random RNG = new Random();

    // =========================================================================
    //  ESSENCE HELPERS
    // =========================================================================

    public static float getEssence(Player player) {
        return player.getPersistentData().getFloat(NBT_ESSENCE);
    }

    public static void setEssence(Player player, float val) {
        player.getPersistentData().putFloat(NBT_ESSENCE,
                Math.max(0f, Math.min(ESSENCE_MAX, val)));
    }

    private static void addEssence(Player player, float amount) {
        setEssence(player, getEssence(player) + amount);
    }

    private static boolean spendEssence(Player player, float cost) {
        if (getEssence(player) < cost) return false;
        setEssence(player, getEssence(player) - cost);
        return true;
    }

    public static boolean inSovereign(Player player) {
        return player.getPersistentData().getInt(NBT_SOVEREIGN) > 0;
    }

    // ─── Mutation getters ─────────────────────────────────────────────────────

    public static int getMutVirulence(Player player) {
        return player.getPersistentData().getInt(NBT_MUT_VIR);
    }

    public static int getMutNecrosis(Player player) {
        return player.getPersistentData().getInt(NBT_MUT_NEC);
    }

    public static int getMutPutrefaction(Player player) {
        return player.getPersistentData().getInt(NBT_MUT_PUT);
    }

    /** DoT multiplier from Necrosis mutation. */
    private static float necrosisMult(Player player) {
        return 1.0f + getMutNecrosis(player) * 0.20f;
    }

    /** Spread radius including Virulence mutation. */
    private static float spreadRadius(Player player) {
        return BASE_SPREAD_RADIUS + getMutVirulence(player);
    }

    /** Spread chance including Virulence mutation. */
    private static float spreadChance(Player player) {
        return Math.min(1.0f, BASE_SPREAD_CHANCE + getMutVirulence(player) * 0.15f);
    }

    /** Death explosion radius including Putrefaction mutation. */
    private static float deathExplosionRadius(Player player, int stage) {
        float base = stage == STAGE_HOST ? 5f : 3f;
        return base + getMutPutrefaction(player);
    }

    /** Death explosion damage including Putrefaction mutation. */
    private static float deathExplosionDamage(Player player, int stage) {
        float base = stage == STAGE_HOST ? 18f : 10f;
        return base * (1f + getMutPutrefaction(player) * 0.25f);
    }

    private static String essenceStatus(Player player) {
        float e = getEssence(player);
        String tag;
        if (e >= 75f)      tag = "§5[Virulent]";
        else if (e >= 50f) tag = "§2[Festering]";
        else if (e >= 25f) tag = "§a[Building]";
        else               tag = "§7[Scarce]";
        return tag + " §fEssence: §b" + String.format("%.0f", e) + "/100";
    }

    // =========================================================================
    //  INFECTION STAGE HELPERS (stored on entity)
    // =========================================================================

    public static int getStage(LivingEntity entity) {
        return entity.getPersistentData().getInt(NBT_STAGE);
    }

    public static void setStage(LivingEntity entity, int stage) {
        int clamped = Math.max(0, Math.min(STAGE_HOST, stage));
        entity.getPersistentData().putInt(NBT_STAGE, clamped);
        if (clamped > 0 && !entity.getPersistentData().contains(NBT_DOT_TIMER)) {
            entity.getPersistentData().putInt(NBT_DOT_TIMER, DOT_INTERVAL);
        }
    }

    public static void advanceStage(LivingEntity entity, Player owner) {
        int current = getStage(entity);
        if (current < STAGE_HOST) {
            setStage(entity, current + 1);
            entity.getPersistentData().putUUID(NBT_OWNER, owner.getUUID());
        }
    }

    public static void clearInfection(LivingEntity entity) {
        entity.getPersistentData().putInt(NBT_STAGE, 0);
        entity.getPersistentData().remove(NBT_DOT_TIMER);
        entity.getPersistentData().remove(NBT_OWNER);
    }

    private static String stageName(int stage) {
        return switch (stage) {
            case 1 -> "§aInfected";
            case 2 -> "§2Festering";
            case 3 -> "§6Rotting";
            case 4 -> "§5§lPlague Host";
            default -> "§7Clean";
        };
    }

    /**
     * Apply stage-appropriate debuffs to an infected entity.
     * Called each DoT tick.
     */
    private static void applyStageDebuffs(LivingEntity entity, int stage) {
        switch (stage) {
            case STAGE_FESTERING ->
                    entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, false));
            case STAGE_ROTTING -> {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1, false, false));
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          30, 0, false, false));
            }
            case STAGE_HOST -> {
                entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2, false, false));
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          30, 1, false, false));
            }
        }
    }

    // =========================================================================
    //  DEATH EXPLOSION + SPREAD
    // =========================================================================

    /**
     * Triggered when an infected entity dies.
     * Stage 3+: spreads infection to nearby enemies.
     * Stage 4:  full death explosion hitting all nearby.
     */
    private static void onInfectedDeath(LivingEntity dead, Player owner, ServerLevel sl) {
        int stage = getStage(dead);
        if (stage < STAGE_ROTTING) return;

        float radius   = deathExplosionRadius(owner, stage);
        float dmg      = deathExplosionDamage(owner, stage);
        float sRadius  = spreadRadius(owner);
        float sChance  = spreadChance(owner);
        int   ascStage = SoulCore.getAscensionStage(owner);

        List<LivingEntity> nearby = sl.getEntitiesOfClass(
                LivingEntity.class,
                dead.getBoundingBox().inflate(stage == STAGE_HOST ? radius : sRadius),
                e -> e != dead && e.isAlive());

        for (LivingEntity e : nearby) {
            double dist = e.distanceTo(dead);

            // Spread infection
            if (dist <= sRadius && (inSovereign(owner) || RNG.nextFloat() < sChance)) {
                int spreadStage = stage == STAGE_HOST
                        ? Math.min(STAGE_HOST, getStage(e) + 2)
                        : Math.min(STAGE_ROTTING, getStage(e) + 1);
                setStage(e, spreadStage);
                e.getPersistentData().putUUID(NBT_OWNER, owner.getUUID());
                sl.sendParticles(ParticleTypes.WARPED_SPORE,
                        e.getX(), e.getY() + 1, e.getZ(), 4, 0.2, 0.2, 0.2, 0.02);
            }

            // Stage 4 death explosion
            if (stage == STAGE_HOST && dist <= radius) {
                float explosionDmg = dmg * (1f - (float)(dist / radius) * 0.4f);
                e.hurt(sl.damageSources().playerAttack(owner), explosionDmg);
                e.invulnerableTime = 0;
                sl.sendParticles(ParticleTypes.WARPED_SPORE,
                        e.getX(), e.getY() + 1, e.getZ(), 8, 0.3, 0.3, 0.3, 0.04);
            }
        }

        // Harvest essence from a plague host death
        float essenceGain = switch (stage) {
            case STAGE_HOST    -> 20f + ascStage * 2f;
            case STAGE_ROTTING -> 12f + ascStage;
            default            -> 6f;
        };
        addEssence(owner, essenceGain);

        // Visual
        for (int i = 0; i < 20; i++) {
            double angle = Math.toRadians(i * 18);
            double px = dead.getX() + radius * Math.cos(angle);
            double pz = dead.getZ() + radius * Math.sin(angle);
            sl.sendParticles(ParticleTypes.WARPED_SPORE, px, dead.getY() + 0.5, pz,
                    1, 0, 0.2, 0, 0.02);
        }
        sl.playSound(null, dead.blockPosition(),
                SoundEvents.SLIME_DEATH, SoundSource.HOSTILE, 1f, 0.4f);

        if (owner instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§2☠ " + stageName(stage) + " §fdied → spread + §b+"
                            + String.format("%.0f", essenceGain) + " Essence§f. "
                            + essenceStatus(owner)));
    }

    // =========================================================================
    //  RAY-CAST HELPER
    // =========================================================================

    private static LivingEntity rayCastFirst(Player player, Level level, int blocks) {
        Vec3 start = player.getEyePosition();
        Vec3 dir   = player.getLookAngle().normalize();
        Vec3 cur   = start;
        for (int i = 0; i < blocks; i++) {
            cur = cur.add(dir);
            List<LivingEntity> hits = level.getEntitiesOfClass(
                    LivingEntity.class, new AABB(cur, cur).inflate(0.5),
                    e -> e != player && e.isAlive());
            if (!hits.isEmpty()) return hits.get(0);
        }
        return null;
    }

    // =========================================================================
    //  EVENTS
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class PlagueEvents {

        @SubscribeEvent
        public static void onPlagueTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Plague Sovereign")) return;

            // ── Sovereign's Plague countdown ──────────────────────────────────
            int sovereign = player.getPersistentData().getInt(NBT_SOVEREIGN);
            if (sovereign > 0) {
                player.getPersistentData().putInt(NBT_SOVEREIGN, sovereign - 1);
                if (player.tickCount % 8 == 0)
                    sl.sendParticles(ParticleTypes.WARPED_SPORE,
                            player.getX(), player.getY() + 1, player.getZ(),
                            6, 0.5, 0.5, 0.5, 0.04);
            }

            // ── DoT tick on all infected entities nearby ──────────────────────
            List<LivingEntity> infected = sl.getEntitiesOfClass(
                    LivingEntity.class,
                    player.getBoundingBox().inflate(40),
                    e -> e != player && e.isAlive() && getStage(e) > 0
                            && e.getPersistentData().contains(NBT_OWNER)
                            && e.getPersistentData().getUUID(NBT_OWNER).equals(player.getUUID()));

            for (LivingEntity e : infected) {
                int stage = getStage(e);
                int timer = e.getPersistentData().getInt(NBT_DOT_TIMER) - 1;

                if (timer <= 0) {
                    // Deal DoT
                    float dot = STAGE_DOT[stage] * necrosisMult(player);
                    if (inSovereign(player)) dot *= 1.6f;
                    e.hurt(sl.damageSources().playerAttack(player), dot);
                    e.invulnerableTime = 0;
                    applyStageDebuffs(e, stage);

                    // Gain small essence per DoT tick
                    addEssence(player, 0.5f * stage);

                    // Particles scaled to stage
                    sl.sendParticles(ParticleTypes.WARPED_SPORE,
                            e.getX(), e.getY() + 1, e.getZ(),
                            stage, 0.2, 0.2, 0.2, 0.01);
                    if (stage >= STAGE_ROTTING)
                        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                                e.getX(), e.getY() + 1, e.getZ(), 2, 0.3, 0.3, 0.3, 0.01);

                    timer = DOT_INTERVAL;
                }
                e.getPersistentData().putInt(NBT_DOT_TIMER, timer);

                // Passive spread chance in Sovereign mode
                if (inSovereign(player) && player.tickCount % 40 == 0) {
                    List<LivingEntity> adj = sl.getEntitiesOfClass(
                            LivingEntity.class, e.getBoundingBox().inflate(spreadRadius(player)),
                            n -> n != player && n != e && n.isAlive() && getStage(n) < stage);
                    for (LivingEntity n : adj) {
                        setStage(n, Math.min(STAGE_HOST, getStage(n) + 1));
                        n.getPersistentData().putUUID(NBT_OWNER, player.getUUID());
                        sl.sendParticles(ParticleTypes.WARPED_SPORE,
                                n.getX(), n.getY() + 1, n.getZ(), 3, 0.2, 0.2, 0.2, 0.02);
                    }
                }
            }

            // ── Passive Essence decay ─────────────────────────────────────────
            float essence = getEssence(player);
            if (essence > 0) {
                int decayTimer = player.getPersistentData().getInt(NBT_ESSENCE_DECAY) - 1;
                if (decayTimer <= 0) {
                    setEssence(player, essence - 1f);
                    decayTimer = ESSENCE_DECAY_TICKS;
                }
                player.getPersistentData().putInt(NBT_ESSENCE_DECAY, decayTimer);
            }

            // ── HUD particles ─────────────────────────────────────────────────
            if (player.tickCount % 15 == 0 && essence > 0) {
                int count = Math.max(1, (int)(essence / 25f));
                sl.sendParticles(ParticleTypes.WARPED_SPORE,
                        player.getX(), player.getY() + 2.3, player.getZ(),
                        count, 0.25, 0.1, 0.25, 0.005);
            }
        }

        /** Grant essence when the player kills an infected entity. */
        @SubscribeEvent(priority = EventPriority.LOW)
        public static void onInfectedEntityDeath(LivingDeathEvent event) {
            if (!(event.getEntity() instanceof LivingEntity dead)) return;
            if (getStage(dead) == 0) return;
            if (!dead.getPersistentData().contains(NBT_OWNER)) return;

            // Find the owning player in the world
            if (!(dead.level() instanceof ServerLevel sl)) return;
            UUID ownerUUID = dead.getPersistentData().getUUID(NBT_OWNER);
            sl.players().stream()
                    .filter(p -> p.getUUID().equals(ownerUUID)
                            && SoulCore.getAspect(p).equals("Plague Sovereign"))
                    .findFirst()
                    .ifPresent(owner -> onInfectedDeath(dead, owner, sl));
        }

        /** Boost outgoing damage slightly based on essence level. */
        @SubscribeEvent(priority = EventPriority.NORMAL)
        public static void onPlagueDamageDealt(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Plague Sovereign")) return;

            float essence = getEssence(player);
            float mult    = 1.0f + (essence / ESSENCE_MAX) * 0.35f;
            if (inSovereign(player)) mult *= 1.4f;
            event.setAmount(event.getAmount() * mult);

            // Each hit passively adds a tiny bit of essence
            addEssence(player, 1.0f);
        }
    }

    // =========================================================================
    //  ABILITY 1 — PLAGUE TOUCH
    // =========================================================================

    /**
     * Infects target enemy, advancing their stage by 1.
     * If already at Stage 4, refreshes DoT timer and applies bonus damage.
     * Cost: 150 essence-soul. Stage 0+.
     */
    public static void plagueTouch(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Plague Sovereign")) return;
        if (SoulCore.getSoulEssence(player) < 150) return;

        int stage = SoulCore.getAscensionStage(player);
        LivingEntity target = rayCastFirst(player, level, 6 + stage);
        if (target == null) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo target in range!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 150);
        addEssence(player, 5f);

        int currentStage = getStage(target);
        if (currentStage == STAGE_HOST) {
            // Refresh DoT and deal bonus
            target.getPersistentData().putInt(NBT_DOT_TIMER, DOT_INTERVAL);
            float bonusDmg = 12f * necrosisMult(player);
            if (inSovereign(player)) bonusDmg *= 1.6f;
            target.hurt(level.damageSources().playerAttack(player), bonusDmg);
            target.invulnerableTime = 0;
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§5Plague Touch: §falready §5§lPlague Host§f — refreshed + §c"
                                + String.format("%.1f", bonusDmg) + " bonus dmg. "
                                + essenceStatus(player)));
        } else {
            // Advance stage (Virulence rank 3: +2 stages)
            int advance = (getMutVirulence(player) >= 3 || inSovereign(player)) ? 2 : 1;
            setStage(target, currentStage + advance);
            target.getPersistentData().putUUID(NBT_OWNER, player.getUUID());
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§2Plague Touch: §f" + target.getName().getString()
                                + " → " + stageName(getStage(target)) + "§f. "
                                + essenceStatus(player)));
        }

        sl.sendParticles(ParticleTypes.WARPED_SPORE,
                target.getX(), target.getY() + 1, target.getZ(), 10, 0.3, 0.3, 0.3, 0.04);
        level.playSound(null, target.blockPosition(),
                SoundEvents.SLIME_ATTACK, SoundSource.HOSTILE, 0.8f, 0.4f);
    }

    // =========================================================================
    //  ABILITY 2 — MIASMA CLOUD
    // =========================================================================

    /**
     * AOE infection wave. All enemies in radius gain Stage +1.
     * Radius and spread scale with Virulence mutation and essence level.
     * Cost: 400 soul-essence. Requires stage ≥ 1.
     */
    public static void miasmaCloud(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Plague Sovereign")) return;
        if (SoulCore.getSoulEssence(player) < 400) return;
        if (SoulCore.getAscensionStage(player) < 1) return;

        int   ascStage = SoulCore.getAscensionStage(player);
        float radius   = 5f + getMutVirulence(player) * 1.5f + ascStage * 0.5f;
        if (inSovereign(player)) radius *= 1.5f;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 400);
        addEssence(player, 10f);

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive());

        int infected = 0;
        for (LivingEntity e : targets) {
            int newStage = Math.min(STAGE_HOST,
                    getStage(e) + (inSovereign(player) ? 2 : 1));
            setStage(e, newStage);
            e.getPersistentData().putUUID(NBT_OWNER, player.getUUID());
            infected++;
            sl.sendParticles(ParticleTypes.WARPED_SPORE,
                    e.getX(), e.getY() + 1, e.getZ(), 6, 0.3, 0.3, 0.3, 0.03);
        }

        // Ring particles
        for (int i = 0; i < 32; i++) {
            double angle = (2 * Math.PI / 32) * i;
            double px = player.getX() + radius * Math.cos(angle);
            double pz = player.getZ() + radius * Math.sin(angle);
            sl.sendParticles(ParticleTypes.WARPED_SPORE, px, player.getY() + 0.3, pz,
                    1, 0, 0.4, 0, 0.01);
        }
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                player.getX(), player.getY() + 1, player.getZ(),
                20, radius * 0.4, 0.5, radius * 0.4, 0.02);
        level.playSound(null, player.blockPosition(),
                SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1f, 0.3f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§2Miasma Cloud: §finfected §c" + infected + "§f enemies in r§e"
                            + String.format("%.1f", radius) + "§f. " + essenceStatus(player)));
    }

    // =========================================================================
    //  ABILITY 3 — ACCELERATE DECAY
    // =========================================================================

    /**
     * Forces target enemy to jump directly to Stage 4 (Plague Host).
     * Deals immediate damage per stage skipped.
     * Cost: 600 soul-essence. Requires stage ≥ 2.
     */
    public static void accelerateDecay(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Plague Sovereign")) return;
        if (SoulCore.getSoulEssence(player) < 600) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        int ascStage = SoulCore.getAscensionStage(player);
        LivingEntity target = rayCastFirst(player, level, 12 + ascStage);
        if (target == null) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo target in range!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 600);
        addEssence(player, 15f);

        int currentStage = getStage(target);
        int stagesSkipped = STAGE_HOST - currentStage;

        // Immediate damage per stage skipped
        float dmg = (8f + stagesSkipped * 6f + ascStage * 2f) * necrosisMult(player);
        if (inSovereign(player)) dmg *= 1.5f;

        setStage(target, STAGE_HOST);
        target.getPersistentData().putUUID(NBT_OWNER, player.getUUID());
        target.getPersistentData().putInt(NBT_DOT_TIMER, DOT_INTERVAL);

        target.hurt(level.damageSources().playerAttack(player), dmg);
        target.invulnerableTime = 0;
        applyStageDebuffs(target, STAGE_HOST);

        // Dramatic particles
        for (int i = 0; i < 16; i++) {
            double angle = Math.toRadians(i * 22.5);
            double px = target.getX() + 1.5 * Math.cos(angle);
            double pz = target.getZ() + 1.5 * Math.sin(angle);
            sl.sendParticles(ParticleTypes.WARPED_SPORE, px, target.getY() + 1, pz,
                    1, 0, 0.3, 0, 0.03);
        }
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                target.getX(), target.getY() + 1, target.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
        level.playSound(null, target.blockPosition(),
                SoundEvents.SLIME_DEATH, SoundSource.HOSTILE, 1f, 0.3f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§6Accelerate Decay: §f" + target.getName().getString()
                            + " §fskipped §e" + stagesSkipped + " stages §f→ §5§lPlague Host§f! "
                            + "§c" + String.format("%.1f", dmg) + " dmg§f. "
                            + essenceStatus(player)));
    }

    // =========================================================================
    //  ABILITY 4 — HARVEST SOUL
    // =========================================================================

    /**
     * Instantly kills a low-health infected enemy, harvesting massive Plague Essence.
     * Execute threshold scales with infection stage and Essence level.
     * Cost: 300 soul-essence. Requires stage ≥ 0.
     */
    public static void harvestSoul(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Plague Sovereign")) return;
        if (SoulCore.getSoulEssence(player) < 300) return;

        int ascStage = SoulCore.getAscensionStage(player);
        LivingEntity target = rayCastFirst(player, level, 10 + ascStage);
        if (target == null) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo target in range!"));
            return;
        }

        int infStage = getStage(target);
        if (infStage == 0 && !inSovereign(player)) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cTarget must be infected to harvest!"));
            return;
        }

        // Execute threshold: 15% base + 5% per infection stage + 5% per essence tier
        float essenceTier  = getEssence(player) / 25f; // 0–4
        float threshold    = 0.15f + infStage * 0.05f + essenceTier * 0.05f;
        if (inSovereign(player)) threshold += 0.20f;

        float hpPct = target.getHealth() / target.getMaxHealth();

        if (hpPct > threshold) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§cTarget above execute threshold! Need ≤§e"
                                + String.format("%.0f", threshold * 100f) + "%§c HP. Has §e"
                                + String.format("%.0f", hpPct * 100f) + "%§c."));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 300);

        // Kill and harvest
        float essenceGain = 20f + infStage * 10f + ascStage * 3f;
        if (inSovereign(player)) essenceGain *= 1.5f;
        addEssence(player, essenceGain);

        // Trigger death (will fire onInfectedEntityDeath for spread/explosion)
        target.hurt(level.damageSources().playerAttack(player), target.getMaxHealth() * 2f);

        sl.sendParticles(ParticleTypes.WARPED_SPORE,
                target.getX(), target.getY() + 1, target.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                target.getX(), target.getY() + 1, target.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
        level.playSound(null, target.blockPosition(),
                SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 0.6f, 1.4f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5Harvest Soul: §fdevoured §c" + target.getName().getString()
                            + " §f(§e" + stageName(infStage) + "§f) → §b+"
                            + String.format("%.0f", essenceGain) + " Essence§f. "
                            + essenceStatus(player)));
    }

    // =========================================================================
    //  ABILITY 5 — EPIDEMIC
    // =========================================================================

    /**
     * Every infected enemy within range instantly spreads their infection stage
     * to all nearby clean/lower-stage enemies.
     * Cost: 700 soul-essence. Requires stage ≥ 3.
     */
    public static void epidemic(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Plague Sovereign")) return;
        if (SoulCore.getSoulEssence(player) < 700) return;
        if (SoulCore.getAscensionStage(player) < 3) return;

        int   ascStage   = SoulCore.getAscensionStage(player);
        float scanRadius = 25f + ascStage;
        float sRadius    = spreadRadius(player) * (inSovereign(player) ? 2f : 1f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 700);
        addEssence(player, 20f);

        List<LivingEntity> allNearby = level.getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(scanRadius),
                e -> e != player && e.isAlive());

        int totalSpread = 0;
        for (LivingEntity source : allNearby) {
            int srcStage = getStage(source);
            if (srcStage == 0) continue;
            if (!source.getPersistentData().contains(NBT_OWNER)) continue;
            if (!source.getPersistentData().getUUID(NBT_OWNER).equals(player.getUUID())) continue;

            List<LivingEntity> spreadTargets = level.getEntitiesOfClass(
                    LivingEntity.class, source.getBoundingBox().inflate(sRadius),
                    e -> e != player && e != source && e.isAlive()
                            && getStage(e) < srcStage);

            for (LivingEntity t : spreadTargets) {
                int newStage = Math.min(STAGE_HOST,
                        inSovereign(player) ? srcStage : srcStage - 1);
                setStage(t, Math.max(getStage(t), newStage));
                t.getPersistentData().putUUID(NBT_OWNER, player.getUUID());
                totalSpread++;
                // Line particles from source to target
                Vec3 from = source.position().add(0, 1, 0);
                Vec3 to   = t.position().add(0, 1, 0);
                Vec3 d    = to.subtract(from).normalize();
                double dist = from.distanceTo(to);
                Vec3 c = from;
                for (double dd = 0; dd < dist; dd += 0.6) {
                    sl.sendParticles(ParticleTypes.WARPED_SPORE, c.x, c.y, c.z,
                            1, 0.04, 0.04, 0.04, 0);
                    c = c.add(d.scale(0.6));
                }
            }
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.2f, 0.2f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§2§lEpidemic! §r§fSpread infection to §c" + totalSpread
                            + "§f new targets. " + essenceStatus(player)));
    }

    // =========================================================================
    //  ABILITY 6 — NECROTIC BURST
    // =========================================================================

    /**
     * Detonates the infection inside one target.
     * Damage scales massively with infection stage.
     * Resets target to Stage 1 (doesn't kill the infection entirely).
     * Cost: 800 soul-essence. Requires stage ≥ 5.
     */
    public static void necroticBurst(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Plague Sovereign")) return;
        if (SoulCore.getSoulEssence(player) < 800) return;
        if (SoulCore.getAscensionStage(player) < 5) return;

        int ascStage = SoulCore.getAscensionStage(player);
        LivingEntity target = rayCastFirst(player, level, 16 + ascStage);
        if (target == null) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo target in range!"));
            return;
        }

        int infStage = getStage(target);
        if (infStage == 0 && !inSovereign(player)) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cTarget must be infected!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 800);
        addEssence(player, 15f);

        int effectiveStage = inSovereign(player) ? STAGE_HOST : infStage;
        float dmg = (20f + effectiveStage * 12f + ascStage * 3f)
                * necrosisMult(player)
                * (1f + getMutPutrefaction(player) * 0.25f);
        if (inSovereign(player)) dmg *= 1.5f;

        target.hurt(level.damageSources().playerAttack(player), dmg);
        target.invulnerableTime = 0;

        // Splash AOE around target
        float splashRadius = 3f + getMutPutrefaction(player) * 0.5f;
        List<LivingEntity> splash = level.getEntitiesOfClass(
                LivingEntity.class, target.getBoundingBox().inflate(splashRadius),
                e -> e != player && e != target && e.isAlive());
        for (LivingEntity e : splash) {
            float splashDmg = dmg * 0.35f;
            e.hurt(level.damageSources().playerAttack(player), splashDmg);
            e.invulnerableTime = 0;
            addVibStacks: // infect splash targets
            {
                if (getStage(e) < STAGE_INFECTED) {
                    setStage(e, STAGE_INFECTED);
                    e.getPersistentData().putUUID(NBT_OWNER, player.getUUID());
                }
            }
        }

        // Reset target stage to 1 (keeps the cycle going)
        setStage(target, STAGE_INFECTED);
        target.getPersistentData().putUUID(NBT_OWNER, player.getUUID());

        // Explosion particles
        for (int i = 0; i < 24; i++) {
            double angle = Math.toRadians(i * 15);
            double px = target.getX() + splashRadius * Math.cos(angle);
            double pz = target.getZ() + splashRadius * Math.sin(angle);
            sl.sendParticles(ParticleTypes.WARPED_SPORE, px, target.getY() + 0.5, pz,
                    1, 0, 0.3, 0, 0.02);
        }
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, target.blockPosition(),
                SoundEvents.SLIME_DEATH, SoundSource.HOSTILE, 1.2f, 0.25f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§6§lNecrotic Burst: §r§f" + String.format("%.1f", dmg)
                            + " dmg (stage §e" + effectiveStage + "§f) + §c"
                            + splash.size() + "§f splash targets. Stage reset to §aInfected§f. "
                            + essenceStatus(player)));
    }

    // =========================================================================
    //  ABILITY 7 — SOVEREIGN'S PLAGUE (ULTIMATE)
    // =========================================================================

    /**
     * 10-second ultimate:
     *   - ALL nearby enemies jump to Stage 4 (Plague Host)
     *   - Mutations treated as maxed (rank 3) for duration
     *   - Passive spread fires every 2s automatically
     *   - All Plague Essence abilities cost 0 essence for duration
     * Cost: 5000 soul-essence. Requires stage ≥ 7.
     */
    public static void sovereignsPlague(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Plague Sovereign")) return;
        if (SoulCore.getSoulEssence(player) < 5000) return;
        if (SoulCore.getAscensionStage(player) < 7) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 5000);

        player.getPersistentData().putInt(NBT_SOVEREIGN, SOVEREIGN_DURATION);

        // Immediately infect all nearby enemies at Stage 4
        int ascStage = SoulCore.getAscensionStage(player);
        float radius = 20f + ascStage;
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive());

        for (LivingEntity e : targets) {
            setStage(e, STAGE_HOST);
            e.getPersistentData().putUUID(NBT_OWNER, player.getUUID());
            e.getPersistentData().putInt(NBT_DOT_TIMER, DOT_INTERVAL);
            sl.sendParticles(ParticleTypes.WARPED_SPORE,
                    e.getX(), e.getY() + 1, e.getZ(), 10, 0.4, 0.4, 0.4, 0.05);
        }

        // Burst visual
        for (int i = 0; i < 40; i++) {
            double angle = Math.toRadians(i * 9);
            double px = player.getX() + radius * 0.5 * Math.cos(angle);
            double pz = player.getZ() + radius * 0.5 * Math.sin(angle);
            sl.sendParticles(ParticleTypes.WARPED_SPORE, px, player.getY() + 1, pz,
                    1, 0, 0.4, 0, 0.02);
        }
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR,
                player.getX(), player.getY() + 1, player.getZ(),
                40, radius * 0.3, 1.0, radius * 0.3, 0.03);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(), 3, 0.5, 0.3, 0.5, 0);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.5f, 0.4f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5§l☠ SOVEREIGN'S PLAGUE ☠ §r§2All §c" + targets.size()
                            + "§2 nearby enemies → §5§lPlague Host§2. "
                            + "Mutations maxed. Infinite spread. §b10s."));
    }

    // =========================================================================
    //  MUTATION UPGRADES (spends Plague Essence)
    // =========================================================================

    /** Upgrade Virulence mutation (max rank 3). Costs 25 Plague Essence per rank. */
    public static void upgradeMutVirulence(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Plague Sovereign")) return;
        int current = getMutVirulence(player);
        if (current >= 3) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cVirulence already at max rank!"));
            return;
        }
        if (!spendEssence(player, MUT_COST_VIR)) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§cNeed §e" + MUT_COST_VIR + "§c Plague Essence for Virulence upgrade!"));
            return;
        }
        player.getPersistentData().putInt(NBT_MUT_VIR, current + 1);
        sl.sendParticles(ParticleTypes.WARPED_SPORE,
                player.getX(), player.getY() + 1, player.getZ(), 12, 0.4, 0.4, 0.4, 0.04);
        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§aMutation — Virulence §frank §e" + (current + 1)
                            + "§f: spread radius §b+" + (current + 1)
                            + "§f, spread chance §b+" + ((current + 1) * 15) + "%§f. "
                            + essenceStatus(player)));
    }

    /** Upgrade Necrosis mutation (max rank 3). Costs 30 Plague Essence per rank. */
    public static void upgradeMutNecrosis(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Plague Sovereign")) return;
        int current = getMutNecrosis(player);
        if (current >= 3) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNecrosis already at max rank!"));
            return;
        }
        if (!spendEssence(player, MUT_COST_NEC)) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§cNeed §e" + MUT_COST_NEC + "§c Plague Essence for Necrosis upgrade!"));
            return;
        }
        player.getPersistentData().putInt(NBT_MUT_NEC, current + 1);
        sl.sendParticles(ParticleTypes.WARPED_SPORE,
                player.getX(), player.getY() + 1, player.getZ(), 12, 0.4, 0.4, 0.4, 0.04);
        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§aMutation — Necrosis §frank §e" + (current + 1)
                            + "§f: DoT multiplier §b×" + String.format("%.1f", 1f + (current + 1) * 0.2f)
                            + "§f. " + essenceStatus(player)));
    }

    /** Upgrade Putrefaction mutation (max rank 3). Costs 35 Plague Essence per rank. */
    public static void upgradeMutPutrefaction(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Plague Sovereign")) return;
        int current = getMutPutrefaction(player);
        if (current >= 3) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cPutrefaction already at max rank!"));
            return;
        }
        if (!spendEssence(player, MUT_COST_PUT)) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§cNeed §e" + MUT_COST_PUT + "§c Plague Essence for Putrefaction upgrade!"));
            return;
        }
        player.getPersistentData().putInt(NBT_MUT_PUT, current + 1);
        sl.sendParticles(ParticleTypes.WARPED_SPORE,
                player.getX(), player.getY() + 1, player.getZ(), 12, 0.4, 0.4, 0.4, 0.04);
        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§aMutation — Putrefaction §frank §e" + (current + 1)
                            + "§f: death explosion radius §b+" + (current + 1)
                            + "§f, explosion damage §b+" + ((current + 1) * 25) + "%§f. "
                            + essenceStatus(player)));
    }
}
