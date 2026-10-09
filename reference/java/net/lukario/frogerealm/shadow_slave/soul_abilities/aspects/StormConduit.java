package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

import java.util.List;

/**
 * Storm Conduit
 * -----------------------------------------------
 * The player channels raw storm energy — electricity builds up inside
 * their body (Voltage). Too much Voltage overloads the system and starts
 * arc-discharging back at the caster. But high Voltage = devastating DPS.
 *
 * Core Mechanic — VOLTAGE (0–100)
 *   0–40   → Safe zone
 *   40–70  → Charged: +damage bonus
 *   70–90  → Overcharged: BIG damage bonus
 *   90–100 → Arc feedback: self-damage over time ⚡
 *
 * Player NBT keys:
 *   "StormVoltage"        → float  current voltage (0.0–100.0)
 *   "StormOverclockTimer" → int    ticks remaining on Overclock
 *   "StormSurgeTimer"     → int    ticks remaining on Cognitive Surge channel
 *   "StormSingularity"    → int    ticks remaining on Singularity Mind
 *   "StormDecayTimer"     → int    internal: ticks until next passive decay tick
 *   "StormFeedbackTimer"  → int    internal: ticks until next arc-feedback damage
 */
public class StormConduit {

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final String NBT_VOLTAGE      = "StormVoltage";
    private static final String NBT_OVERCLOCK    = "StormOverclockTimer";
    private static final String NBT_SURGE        = "StormSurgeTimer";
    private static final String NBT_SINGULARITY  = "StormSingularity";
    private static final String NBT_DECAY_TIMER  = "StormDecayTimer";
    private static final String NBT_FEEDBACK     = "StormFeedbackTimer";

    // Voltage thresholds
    private static final float THRESHOLD_CHARGED      = 40f;
    private static final float THRESHOLD_OVERCHARGED   = 70f;
    private static final float THRESHOLD_FEEDBACK      = 90f;
    private static final float VOLTAGE_MAX             = 100f;

    // Passive decay: lose 1 voltage every 2 seconds
    private static final int DECAY_INTERVAL = 40;

    // Arc feedback: take damage every 1 second while above 90
    private static final int FEEDBACK_INTERVAL = 20;

    // ─── Voltage Helpers ──────────────────────────────────────────────────────

    public static float getVoltage(Player player) {
        return player.getPersistentData().getFloat(NBT_VOLTAGE);
    }

    public static void setVoltage(Player player, float voltage) {
        float clamped = Math.max(0f, Math.min(VOLTAGE_MAX, voltage));
        player.getPersistentData().putFloat(NBT_VOLTAGE, clamped);
    }

    /** Adds voltage, doubling the gain if Overclock is active. */
    private static void addVoltage(Player player, float amount) {
        float gain = inOverclock(player) ? amount * 2f : amount;
        setVoltage(player, getVoltage(player) + gain);
    }

    public static boolean inOverclock(Player player) {
        return player.getPersistentData().getInt(NBT_OVERCLOCK) > 0;
    }

    public static boolean inSingularity(Player player) {
        return player.getPersistentData().getInt(NBT_SINGULARITY) > 0;
    }

    /** Current voltage tier as a readable string. */
    private static String voltageTag(float v) {
        if (v >= THRESHOLD_FEEDBACK)    return "§c§l[ARC FEEDBACK]";
        if (v >= THRESHOLD_OVERCHARGED) return "§6§l[OVERCHARGED]";
        if (v >= THRESHOLD_CHARGED)     return "§e[Charged]";
        return "§7[Safe]";
    }

    /** Voltage status string for ability messages. */
    private static String voltageStatus(Player player) {
        float v = getVoltage(player);
        return voltageTag(v) + " §fV: §b" + String.format("%.0f", v) + "/100";
    }

    /**
     * Damage multiplier from current voltage:
     *   0–40   → ×1.0
     *   40–70  → ×1.2
     *   70–90  → ×1.5
     *   90–100 → ×1.8
     * Singularity locks multiplier at ×1.5 (safe overcharged) always.
     */
    private static float damageMult(Player player) {
        if (inSingularity(player)) return 1.8f;
        float v = getVoltage(player);
        if (v >= THRESHOLD_FEEDBACK)    return 1.8f;
        if (v >= THRESHOLD_OVERCHARGED) return 1.5f;
        if (v >= THRESHOLD_CHARGED)     return 1.2f;
        return 1.0f;
    }

    // ─── Chain lightning helper ───────────────────────────────────────────────

    /**
     * During Singularity: chains a bolt of electricity between nearby enemies.
     * Called after every ability hit.
     */
    private static void chainLightning(Player player, LivingEntity source, Level level, ServerLevel sl) {
        if (!inSingularity(player)) return;
        AABB area = source.getBoundingBox().inflate(6);
        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class, area,
                e -> e != player && e != source && e.isAlive());
        for (LivingEntity chained : nearby) {
            chained.hurt(level.damageSources().playerAttack(player), 8.0f);
            chained.invulnerableTime = 0;
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    chained.getX(), chained.getY() + 1, chained.getZ(), 8, 0.3, 0.3, 0.3, 0.04);
        }
        if (!nearby.isEmpty()) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    source.getX(), source.getY() + 1, source.getZ(), 6, 0.3, 0.3, 0.3, 0.04);
        }
    }

    // ─── Ray-cast helper ──────────────────────────────────────────────────────

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
    //  TICK EVENT — single class, unique name
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class StormEvents {

        @SubscribeEvent
        public static void onStormPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Storm Conduit")) return;

            float voltage = getVoltage(player);

            // ── Overclock countdown ───────────────────────────────────────────
            int overclock = player.getPersistentData().getInt(NBT_OVERCLOCK);
            if (overclock > 0) {
                player.getPersistentData().putInt(NBT_OVERCLOCK, overclock - 1);
                // Attack speed boost
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 5, 2, true, false));
                if (player.tickCount % 6 == 0)
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            player.getX(), player.getY() + 1, player.getZ(), 3, 0.4, 0.4, 0.4, 0.04);
                // Overclock generates voltage passively
                if (player.tickCount % 10 == 0) addVoltage(player, 2f);
            }

            // ── Singularity countdown ─────────────────────────────────────────
            int singularity = player.getPersistentData().getInt(NBT_SINGULARITY);
            if (singularity > 0) {
                player.getPersistentData().putInt(NBT_SINGULARITY, singularity - 1);
                // Lock voltage at 90 during singularity
                setVoltage(player, 90f);
                if (player.tickCount % 4 == 0)
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            player.getX(), player.getY() + 1, player.getZ(), 5, 0.5, 0.5, 0.5, 0.05);
                // Reduced cooldowns: haste buff as proxy (real CD reduction needs keybind system hook)
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 5, 1, true, false));
            }

            // ── Cognitive Surge channel ───────────────────────────────────────
            int surge = player.getPersistentData().getInt(NBT_SURGE);
            if (surge > 0) {
                player.getPersistentData().putInt(NBT_SURGE, surge - 1);
                addVoltage(player, 0.5f); // build voltage while channeling

                // Continuous damage beam every 10 ticks
                if (player.tickCount % 10 == 0) {
                    LivingEntity target = rayCastFirst(player, player.level(), 12);
                    if (target != null) {
                        float beamDmg = (4.0f + SoulCore.getAscensionStage(player)) * damageMult(player);
                        target.hurt(player.level().damageSources().playerAttack(player), beamDmg);
                        target.invulnerableTime = 0;
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                                target.getX(), target.getY() + 1, target.getZ(), 5, 0.2, 0.2, 0.2, 0.03);
                        chainLightning(player, target, player.level(), sl);
                    }
                    // Beam trail particles
                    Vec3 start = player.getEyePosition();
                    Vec3 dir   = player.getLookAngle().normalize();
                    Vec3 cur   = start;
                    for (int i = 0; i < 12; i++) {
                        cur = cur.add(dir);
                        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, cur.x, cur.y, cur.z, 1, 0.05, 0.05, 0.05, 0);
                    }
                }
            }

            // ── Passive voltage decay ─────────────────────────────────────────
            if (!inSingularity(player) && voltage > 0) {
                int decayTimer = player.getPersistentData().getInt(NBT_DECAY_TIMER);
                decayTimer--;
                if (decayTimer <= 0) {
                    setVoltage(player, getVoltage(player) - 1f);
                    decayTimer = DECAY_INTERVAL;
                }
                player.getPersistentData().putInt(NBT_DECAY_TIMER, decayTimer);
            }

            // ── Arc feedback self-damage (voltage > 90) ───────────────────────
            if (!inSingularity(player) && voltage >= THRESHOLD_FEEDBACK) {
                int feedbackTimer = player.getPersistentData().getInt(NBT_FEEDBACK);
                feedbackTimer--;
                if (feedbackTimer <= 0) {
                    float selfDmg = (voltage - THRESHOLD_FEEDBACK) * 0.3f; // 0–3 dmg per tick at 90–100
                    player.hurt(player.level().damageSources().magic(), selfDmg);
                    feedbackTimer = FEEDBACK_INTERVAL;
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            player.getX(), player.getY() + 1, player.getZ(), 6, 0.5, 0.3, 0.5, 0.06);
                    sl.playSound(null, player.blockPosition(),
                            SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.4f, 1.8f);
                }
                player.getPersistentData().putInt(NBT_FEEDBACK, feedbackTimer);
            }

            // ── Voltage HUD particles ─────────────────────────────────────────
            if (player.tickCount % 8 == 0 && voltage > 0) {
                int particleCount = (int)(voltage / 20); // 0–5 particles based on load
                var particle = voltage >= THRESHOLD_FEEDBACK    ? ParticleTypes.ELECTRIC_SPARK
                        : voltage >= THRESHOLD_OVERCHARGED ? ParticleTypes.CRIT
                        : voltage >= THRESHOLD_CHARGED     ? ParticleTypes.CRIT
                        : ParticleTypes.ELECTRIC_SPARK;
                sl.sendParticles(particle,
                        player.getX(), player.getY() + 2.2, player.getZ(),
                        particleCount, 0.3, 0.1, 0.3, 0.02);
            }
        }

        /** If player takes damage while Surge is channeling: backlash. */
        @SubscribeEvent
        public static void onStormSurgeInterrupt(LivingHurtEvent event) {
            if (!(event.getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Storm Conduit")) return;

            int surge = player.getPersistentData().getInt(NBT_SURGE);
            if (surge <= 0) return;

            // Channel interrupted — backlash
            player.getPersistentData().putInt(NBT_SURGE, 0);
            player.hurt(player.level().damageSources().magic(), 4.0f);

            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cCognitive Surge interrupted! Backlash!"));
        }
    }

    // =========================================================================
    //  ABILITY 1 — ARC SPIKE  (renamed from Mind Spike)
    // =========================================================================

    /**
     * Fast lightning bolt ray-cast. Generates +10 Voltage.
     * If Voltage > 70: deals bonus damage.
     * Cost: 250 essence.
     */
    public static void arcSpike(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Storm Conduit")) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 250);

        int   stage    = SoulCore.getAscensionStage(player);
        float voltage  = getVoltage(player);
        float baseDmg  = 8.0f + stage;
        float damage   = baseDmg * damageMult(player);

        addVoltage(player, 10f);

        Vec3 start = player.getEyePosition();
        Vec3 dir   = player.getLookAngle().normalize();
        Vec3 cur   = start;

        for (int i = 0; i < 12 + stage; i++) {
            cur = cur.add(dir);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, cur.x, cur.y, cur.z, 1, 0.05, 0.05, 0.05, 0);

            List<LivingEntity> hits = level.getEntitiesOfClass(
                    LivingEntity.class, new AABB(cur, cur).inflate(0.4),
                    e -> e != player && e.isAlive());

            for (LivingEntity hit : hits) {
                hit.hurt(level.damageSources().playerAttack(player), damage);
                hit.invulnerableTime = 5;
                sl.sendParticles(ParticleTypes.CRIT,
                        hit.getX(), hit.getY() + 1, hit.getZ(), 6, 0.2, 0.2, 0.2, 0.04);
                chainLightning(player, hit, level, sl);

                if (player instanceof ServerPlayer sp)
                    sp.sendSystemMessage(Component.literal(
                            "§bArc Spike: §f" + String.format("%.1f", damage) + " dmg. " + voltageStatus(player)));
                return;
            }
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.5f, 1.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal("§bArc Spike: §7missed. " + voltageStatus(player)));
    }

    // =========================================================================
    //  ABILITY 2 — DISCHARGE  (renamed from Neural Vent)
    // =========================================================================

    /**
     * Releases built-up voltage as an AOE burst around the player.
     * Removes 30–50 Voltage. Damage scales with how much was removed.
     * Cost: 400 essence.  Requires stage ≥ 1.
     */
    public static void discharge(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Storm Conduit")) return;
        if (SoulCore.getSoulEssence(player) < 400) return;
        if (SoulCore.getAscensionStage(player) < 1) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 400);

        int   stage      = SoulCore.getAscensionStage(player);
        float voltage    = getVoltage(player);
        float ventAmount = Math.min(voltage, 30f + stage * 2f); // 30–44 removed
        float damage     = ventAmount * 1.2f; // damage = 1.2× voltage removed

        setVoltage(player, voltage - ventAmount);

        float radius = 4.0f + stage * 0.5f;
        AABB box = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, box, e -> e != player && e.isAlive());

        for (LivingEntity e : targets) {
            e.hurt(level.damageSources().playerAttack(player), damage);
            e.invulnerableTime = 0;
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    e.getX(), e.getY() + 1, e.getZ(), 6, 0.3, 0.3, 0.3, 0.04);
            chainLightning(player, e, level, sl);
        }

        // Visual ring
        for (int i = 0; i < 20; i++) {
            double angle = (2 * Math.PI / 20) * i;
            double px = player.getX() + radius * Math.cos(angle);
            double pz = player.getZ() + radius * Math.sin(angle);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, px, player.getY() + 0.5, pz, 1, 0, 0.1, 0, 0.02);
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.2f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§bDischarge: released §e" + String.format("%.0f", ventAmount)
                            + "V §b→ §f" + String.format("%.1f", damage) + " AOE dmg, hit §b"
                            + targets.size() + "§f enemies. " + voltageStatus(player)));
    }

    // =========================================================================
    //  ABILITY 3 — OVERLOAD STRIKE  (renamed from Synapse Break)
    // =========================================================================

    /**
     * Heavy single-target burst. Damage scales with current Voltage.
     * Consumes 40 Voltage.
     * Cost: 600 essence.  Requires stage ≥ 2.
     */
    public static void overloadStrike(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Storm Conduit")) return;
        if (SoulCore.getSoulEssence(player) < 600) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        float voltage = getVoltage(player);
        if (voltage < 1f) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNeed Voltage to use Overload Strike!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 600);

        int   stage    = SoulCore.getAscensionStage(player);
        float consumed = Math.min(voltage, 40f);
        float damage   = (10.0f + stage * 2 + consumed * 0.8f) * damageMult(player);

        setVoltage(player, voltage - consumed);

        LivingEntity target = rayCastFirst(player, level, 14 + stage);
        if (target == null) return;

        target.hurt(level.damageSources().playerAttack(player), damage);
        target.invulnerableTime = 0;

        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                target.getX(), target.getY() + 1, target.getZ(), 20, 0.4, 0.4, 0.4, 0.06);
        level.playSound(null, target.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1f, 0.7f);

        chainLightning(player, target, level, sl);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§bOverload Strike: §f" + String.format("%.1f", damage)
                            + " dmg §7(consumed §e" + String.format("%.0f", consumed) + "V§7). "
                            + voltageStatus(player)));
    }

    // =========================================================================
    //  ABILITY 4 — OVERCLOCK
    // =========================================================================

    /**
     * For 5 seconds: abilities generate DOUBLE Voltage and attack speed increases.
     * Voltage also builds passively during this time.
     * Cost: 500 essence.  Requires stage ≥ 3.
     */
    public static void overclock(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Storm Conduit")) return;
        if (SoulCore.getSoulEssence(player) < 500) return;
        if (SoulCore.getAscensionStage(player) < 3) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 500);

        player.getPersistentData().putInt(NBT_OVERCLOCK, 100); // 5 seconds

        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.06);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 1f, 0.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§e§lOVERCLOCK! §r§bDouble Voltage gain for §e5s§b! " + voltageStatus(player)));
    }

    // =========================================================================
    //  ABILITY 5 — STORM CHANNEL  (renamed from Cognitive Surge)
    // =========================================================================

    /**
     * Channels a sustained lightning beam for up to 5 seconds (100 ticks).
     * Deals continuous damage + steadily builds Voltage.
     * If interrupted by taking damage: 4 damage backlash.
     * Cost: 700 essence.  Requires stage ≥ 4.
     */
    public static void stormChannel(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Storm Conduit")) return;
        if (SoulCore.getSoulEssence(player) < 700) return;
        if (SoulCore.getAscensionStage(player) < 4) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 700);

        player.getPersistentData().putInt(NBT_SURGE, 100); // 5 seconds channel

        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 1, player.getZ(), 10, 0.4, 0.4, 0.4, 0.05);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8f, 1.2f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§bStorm Channel started. §7Take damage to interrupt. " + voltageStatus(player)));
    }

    // =========================================================================
    //  ABILITY 6 — THUNDER COLLAPSE  (renamed from Neural Collapse)
    // =========================================================================

    /**
     * Emergency detonation of accumulated Voltage.
     * If Voltage > 80: massive explosion, resets Voltage to 0.
     * If Voltage < 80: weak effect, still resets.
     * Cost: 800 essence.  Requires stage ≥ 5.
     */
    public static void thunderCollapse(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Storm Conduit")) return;
        if (SoulCore.getSoulEssence(player) < 800) return;
        if (SoulCore.getAscensionStage(player) < 5) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 800);

        int   stage   = SoulCore.getAscensionStage(player);
        float voltage = getVoltage(player);
        boolean strong = voltage > 80f;

        float radius = strong ? (6.0f + stage) : 3.0f;
        float damage = strong
                ? (30.0f + voltage * 0.8f + stage * 3) * damageMult(player)
                : 10.0f + stage;

        AABB box = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, box, e -> e != player && e.isAlive());

        for (LivingEntity e : targets) {
            e.hurt(level.damageSources().playerAttack(player), damage);
            e.invulnerableTime = 0;
            if (strong) {
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        e.getX(), e.getY() + 1, e.getZ(), 1, 0, 0, 0, 0);
                chainLightning(player, e, level, sl);
            }
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    e.getX(), e.getY() + 1, e.getZ(), 10, 0.4, 0.4, 0.4, 0.06);
        }

        // Reset voltage
        setVoltage(player, 0f);

        if (strong) {
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    player.getX(), player.getY() + 1, player.getZ(), 3, 0.5, 0.5, 0.5, 0);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.5f, 0.5f);
        } else {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    player.getX(), player.getY() + 1, player.getZ(), 15, 0.5, 0.5, 0.5, 0.04);
            level.playSound(null, player.blockPosition(),
                    SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8f, 1.0f);
        }

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    strong
                            ? "§e§l⚡ THUNDER COLLAPSE! §r§f" + String.format("%.1f", damage)
                            + " dmg in r" + (int) radius + ". §7Voltage reset to 0."
                            : "§7Thunder Collapse (weak): §f" + String.format("%.1f", damage)
                            + " dmg. §7Need >80V for full power."));
    }

    // =========================================================================
    //  ABILITY 7 — SINGULARITY STORM  (renamed from Singularity Mind)
    // =========================================================================

    /**
     * Ultimate — 10 seconds.
     * Locks Voltage at 90 (perfect danger zone, no self-damage).
     * All abilities deal bonus damage. Cooldowns reduced (Haste proxy).
     * Every ability hit chains lightning between nearby enemies.
     * Cost: 5000 essence.  Requires stage ≥ 7.
     */
    public static void singularityStorm(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Storm Conduit")) return;
        if (SoulCore.getSoulEssence(player) < 5000) return;
        if (SoulCore.getAscensionStage(player) < 7) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 5000);

        player.getPersistentData().putInt(NBT_SINGULARITY, 200); // 10 seconds
        setVoltage(player, 90f); // lock at perfect zone immediately

        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                player.getX(), player.getY() + 1, player.getZ(), 50, 1.0, 1.0, 1.0, 0.08);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(), 2, 0.3, 0.3, 0.3, 0);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.5f, 0.4f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§e§l⚡ SINGULARITY STORM! §r§bVoltage locked at 90. No feedback. Chain lightning. 10s."));
    }
}