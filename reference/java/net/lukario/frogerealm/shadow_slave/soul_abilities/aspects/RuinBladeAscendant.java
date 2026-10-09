package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.effects.ModEffects;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
import java.util.UUID;

/**
 * Ruinblade Ascendant
 * -----------------------------------------------
 * Stack-based DPS aspect.  Hits apply RUIN stacks
 * (potion-effect amplifier = stack count - 1).
 * Thresholds unlock secondary effects; abilities
 * consume or detonate stacks for burst damage.
 */
public class RuinBladeAscendant {

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final int MAX_STACKS_NORMAL   = 20; // amplifier 19
    private static final int MAX_STACKS_CATACLYSM = 30; // amplifier 29

    // Threshold amplifiers (stack - 1)
    private static final int THRESHOLD_BLEED     = 4;  // stack  5
    private static final int THRESHOLD_SPEED      = 9;  // stack 10
    private static final int THRESHOLD_SPIKE      = 14; // stack 15
    private static final int THRESHOLD_DETONATE   = 19; // stack 20

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Current ruin amplifier on a victim, or -1 if none. */
    private static int getRuinAmplifier(LivingEntity victim) {
        var effectHolder = ModEffects.RuinBade_Ruin.getHolder().get();
        if (victim.hasEffect(effectHolder)) {
            return victim.getEffect(effectHolder).getAmplifier();
        }
        return -1;
    }

    /** Apply / increment ruin stacks, capped by maxAmplifier. */
    private static void addRuinStacks(LivingEntity victim, Player owner, int stacksToAdd, int maxAmplifier) {
        var effectHolder = ModEffects.RuinBade_Ruin.getHolder().get();
        int current = getRuinAmplifier(victim);
        int newAmp  = Math.min((current == -1 ? 0 : current) + stacksToAdd, maxAmplifier);
        victim.addEffect(new MobEffectInstance(effectHolder, 300, newAmp)); // 15-second window
        owner.sendSystemMessage(Component.literal(victim.getName().getString() + " has " + (newAmp+1) + ", stacks"));
    }

    /** Returns true if the player is in CATACLYSM state. */
    private static boolean inCataclysm(Player player) {
        var effectHolder = ModEffects.RuinBade_cataclysm.getHolder().get();
        return player.hasEffect(effectHolder);
    }

    // =========================================================================
    //  EVENT LISTENERS
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        /**
         * Every player hit → passively apply Ruin stacks.
         * Cataclysm state jumps straight to max stacks.
         */
        @SubscribeEvent
        public static void onPlayerHit(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Ruinblade Ascendant")) return;

            LivingEntity victim = event.getEntity();
            int stage = SoulCore.getAscensionStage(player);

            // CATACLYSM: instant max stacks
            if (inCataclysm(player)) {
                int catMax = MAX_STACKS_CATACLYSM - 1; // amplifier
                addRuinStacks(victim, player, catMax + 1, catMax);
                return;
            }

            int stacksPerHit = (stage >= 5) ? 3 : 2; // Tier upgrade at stage 5
            int cap = MAX_STACKS_NORMAL - 1;
            addRuinStacks(victim, player, stacksPerHit, cap);
        }

        /**
         * Blood Surge passive — grants the owner haste/speed when targeting
         * a heavily marked enemy.  Runs every tick.
         */
        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel)) return;
            if (!SoulCore.getAspect(player).equals("Ruinblade Ascendant")) return;

            // Only run every 10 ticks to keep it cheap
            if (player.tickCount % 10 != 0) return;

            AABB area = player.getBoundingBox().inflate(16);
            List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                    LivingEntity.class, area, e -> e.isAlive() && !e.equals(player));

            boolean hasHighStackTarget = false;
            for (LivingEntity e : nearby) {
                int amp = getRuinAmplifier(e);
                if (amp >= THRESHOLD_SPEED) {
                    hasHighStackTarget = true;
                    break;
                }
            }

            // Grant movement / attack speed only while a high-stack target exists nearby
            if (hasHighStackTarget) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,  15, 0, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,       15, 0, true, false)); // DIG_SPEED = haste (attack speed proxy)
            }
        }
    }

    // =========================================================================
    //  RUIN MARK EFFECT
    // =========================================================================

    public static class Ruinblade_Ruin_Effect extends MobEffect {
        public Ruinblade_Ruin_Effect() {
            super(MobEffectCategory.HARMFUL, 0x8B0000); // dark-red colour
        }

        /** Apply threshold effects every 20 ticks (1 second). */
        @Override
        public boolean applyEffectTick(LivingEntity victim, int amplifier) {

            if (victim.tickCount % 20 == 0) {

                // THRESHOLD 1 — Bleed (stack 5+)
                if (amplifier >= THRESHOLD_BLEED) {
                    victim.hurt(victim.level().damageSources().magic(), 1.5f);
                    victim.invulnerableTime = 0;
                }

                // THRESHOLD 2 — Weakness (stack 10+)  → attacker gets speed from Blood Surge above
                if (amplifier >= THRESHOLD_SPEED) {
                    victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 25, 0, true, false));
                }

                // THRESHOLD 3 — Spike damage + slow (stack 15+)
                if (amplifier >= THRESHOLD_SPIKE) {
                    victim.hurt(victim.level().damageSources().magic(), 3.0f);
                    victim.invulnerableTime = 0;
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 1, true, false));
                }

                // THRESHOLD 4 — Max-stack DOT (stack 20+)
                if (amplifier >= THRESHOLD_DETONATE) {
                    victim.hurt(victim.level().damageSources().magic(), 5.0f);
                    victim.invulnerableTime = 0;
                    victim.addEffect(new MobEffectInstance(MobEffects.WITHER,            25, 0, true, false));
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 25, 2, true, false));
                }
            }
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }
    }

    // =========================================================================
    //  CATACLYSM EFFECT  (self-buff)
    // =========================================================================

    public static class Ruinblade_Cataclysm_Effect extends MobEffect {
        public Ruinblade_Cataclysm_Effect() {
            super(MobEffectCategory.NEUTRAL, 0xFF4500);
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            // Drain HP while active
            if (entity.tickCount % 20 == 0) {
                entity.hurt(entity.level().damageSources().magic(), 1.0f);
                entity.invulnerableTime = 0;
            }
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }
    }

    // =========================================================================
    //  ABILITY 1 — RUIN SLASH  (stack builder)
    // =========================================================================

    /**
     * Fast ray-cast strike that applies 2–3 Ruin stacks to the first enemy hit.
     * Cost: 200 soul essence.   Requires ascension stage ≥ 1.
     */
    public static void ruinSlash(Player player, Level level, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Ruinblade Ascendant")) return;
        if (SoulCore.getSoulEssence(player) < 200) return;
        if (SoulCore.getAscensionStage(player) < 1) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 200);

        int stage       = SoulCore.getAscensionStage(player);
        int stacksApply = (stage >= 5) ? 3 : 2;
        int cap         = MAX_STACKS_NORMAL - 1;

        Vec3 start     = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 current   = start;

        boolean hit = false;
        for (int i = 0; i <= 8 + stage && !hit; i++) {
            current = current.add(direction);

            // Particles
            serverLevel.sendParticles(ParticleTypes.CRIT, current.x, current.y, current.z, 3, 0.1, 0.1, 0.1, 0);

            List<LivingEntity> entities = level.getEntitiesOfClass(
                    LivingEntity.class, new AABB(current, current).inflate(0.5),
                    e -> e != player && e.isAlive());

            for (LivingEntity entity : entities) {
                // Damage scaling: base + 2 per existing stack
                int    existingAmp  = Math.max(0, getRuinAmplifier(entity));
                float  baseDamage   = 6.0f + existingAmp * 2.0f;

                entity.hurt(level.damageSources().playerAttack(player), baseDamage);
                entity.invulnerableTime = 5;
                addRuinStacks(entity, player, stacksApply, cap);

                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        entity.getX(), entity.getY() + 1, entity.getZ(), 5, 0.3, 0.3, 0.3, 0);

                hit = true; // Only pierce first enemy (single-target focus)
                break;
            }
        }

        // Cleave at high tier: also hit in a small arc around the target
        if (stage >= 5 && hit) {
            AABB cleaveBox = new AABB(current, current).inflate(1.5);
            level.getEntitiesOfClass(LivingEntity.class, cleaveBox, e -> e != player && e.isAlive())
                    .forEach(e -> {
                        e.hurt(level.damageSources().playerAttack(player), 3.0f);
                        e.invulnerableTime = 5;
                        addRuinStacks(e, player, 1, cap);
                    });
        }
    }

    // =========================================================================
    //  ABILITY 3 — REND  (half-stack consume for burst + bleed)
    // =========================================================================

    /**
     * Consumes half of current Ruin stacks on the target.
     * Deals burst damage proportional to stacks consumed and extends bleed.
     * Cost: 800 soul essence.   Requires ascension stage ≥ 3.
     */
    public static void rend(Player player, Level level, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Ruinblade Ascendant")) return;
        if (SoulCore.getSoulEssence(player) < 800) return;
        if (SoulCore.getAscensionStage(player) < 3) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 800);

        Vec3  start     = player.getEyePosition();
        Vec3  direction = player.getLookAngle().normalize();
        Vec3  current   = start;
        var   ruin      = ModEffects.RuinBade_Ruin.getHolder().get();
        int   stage     = SoulCore.getAscensionStage(player);
        boolean didHit  = false;

        for (int i = 0; i <= 10 + stage && !didHit; i++) {
            current = current.add(direction);
            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR, current.x, current.y, current.z, 2, 0.1, 0.1, 0.1, 0);

            List<LivingEntity> entities = level.getEntitiesOfClass(
                    LivingEntity.class, new AABB(current, current).inflate(0.5),
                    e -> e != player && e.isAlive());

            for (LivingEntity entity : entities) {
                int amp = getRuinAmplifier(entity);
                if (amp < 0) amp = 0;

                int consumed   = (amp + 1) / 2;               // half of current stacks
                int remaining  = (amp + 1) - consumed - 1;    // new amplifier
                float damage   = 8.0f + consumed * 4.0f;       // scales with consumed stacks

                entity.hurt(level.damageSources().playerAttack(player), damage);
                entity.invulnerableTime = 0;

                // Reset mark to remaining stacks (or remove if 0)
                if (remaining >= 0) {
                    entity.addEffect(new MobEffectInstance(ruin, 300, remaining));
                } else {
                    entity.removeEffect(ruin);
                }

                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        entity.getX(), entity.getY() + 1, entity.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
                level.playSound(null, entity.blockPosition(),
                        SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 0.8f);
                didHit = true;
                break;
            }
        }
    }

    // =========================================================================
    //  ABILITY 4 — RUIN DETONATION  (consume ALL stacks for nuke)
    // =========================================================================

    /**
     * Consumes ALL Ruin stacks on the target and triggers an explosion.
     * At max stacks the explosion also deals bonus true (magic) damage.
     * Cost: 2000 soul essence.  Requires ascension stage ≥ 4.
     */
    public static void ruinDetonation(Player player, Level level, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Ruinblade Ascendant")) return;
        if (SoulCore.getSoulEssence(player) < 2000) return;
        if (SoulCore.getAscensionStage(player) < 4) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 2000);

        Vec3  start     = player.getEyePosition();
        Vec3  direction = player.getLookAngle().normalize();
        Vec3  current   = start;
        var   ruin      = ModEffects.RuinBade_Ruin.getHolder().get();
        int   stage     = SoulCore.getAscensionStage(player);

        boolean didHit = false;
        for (int i = 0; i <= 12 + stage && !didHit; i++) {
            current = current.add(direction);
            serverLevel.sendParticles(ParticleTypes.FLAME, current.x, current.y, current.z, 3, 0.1, 0.1, 0.1, 0);

            List<LivingEntity> entities = level.getEntitiesOfClass(
                    LivingEntity.class, new AABB(current, current).inflate(0.5),
                    e -> e != player && e.isAlive());

            for (LivingEntity entity : entities) {
                int amp    = getRuinAmplifier(entity);
                int stacks = amp + 1; // convert amplifier → stack count

                // Detonate: 10 base + 5 per stack
                float damage = 10.0f + stacks * 5.0f;
                entity.hurt(level.damageSources().playerAttack(player), damage);
                entity.invulnerableTime = 0;

                // Max-stack bonus: extra magic true-damage burst
                if (amp >= THRESHOLD_DETONATE) {
                    entity.hurt(level.damageSources().magic(), stacks * 2.0f);
                    entity.invulnerableTime = 0;
                    serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                            entity.getX(), entity.getY() + 1, entity.getZ(), 1, 0, 0, 0, 0);
                }

                // Remove all stacks
                entity.removeEffect(ruin);

                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        entity.getX(), entity.getY() + 1, entity.getZ(), 6, 0.5, 0.5, 0.5, 0.05);
                level.playSound(null, entity.blockPosition(),
                        SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1f, 0.7f);

                didHit = true;
                break;
            }
        }
    }

    // =========================================================================
    //  ABILITY 5 — OVERLOAD  (self-buff: double stack gain, no decay, +damage taken)
    // =========================================================================

    /**
     * Self-buff that doubles Ruin stack gain and freezes decay for 6 seconds.
     * Represented as a potion effect; the downside (+15% damage taken) is
     * applied via the Overload mob effect below.
     * Cost: 1500 soul essence.  Requires ascension stage ≥ 5.
     */
    public static void overload(Player player) {
        if (!SoulCore.getAspect(player).equals("Ruinblade Ascendant")) return;
        if (SoulCore.getSoulEssence(player) < 1500) return;
        if (SoulCore.getAscensionStage(player) < 5) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 1500);

        // 6 seconds = 120 ticks
        player.addEffect(new MobEffectInstance(
                ModEffects.RuinBade_Overload.getHolder().get(), 120, 0, false, true));

        if (player.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLAME,
                    player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
            sl.playSound(null, player.blockPosition(),
                    SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1f, 1.2f);
        }
    }

    // ─── Overload Effect ──────────────────────────────────────────────────────

    public static class Ruinblade_Overload_Effect extends MobEffect {
        public Ruinblade_Overload_Effect() {
            super(MobEffectCategory.NEUTRAL, 0xFF6600);
        }

        /** While Overload is active, all Ruin stack applications are doubled.
         *  The +15% damage taken is handled inside onDamageTaken (see Events2). */
        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            // Visual reminder
            if (entity.tickCount % 10 == 0 && entity.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.FLAME,
                        entity.getX(), entity.getY() + 1, entity.getZ(), 2, 0.3, 0.3, 0.3, 0);
            }
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return true;
        }
    }

    // ─── Overload damage amplification ────────────────────────────────────────

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events2 {

        /** Increases damage taken by 15% while the player has Overload active. */
        @SubscribeEvent
        public static void onOverloadDamageTaken(LivingHurtEvent event) {
            if (!(event.getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Ruinblade Ascendant")) return;
            if (!player.hasEffect(ModEffects.RuinBade_Overload.getHolder().get())) return;
            event.setAmount(event.getAmount() * 1.15f);
        }

        /** Doubles Ruin stacks applied during Overload. */
        @SubscribeEvent
        public static void onOverloadStackBoost(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Ruinblade Ascendant")) return;
            if (!player.hasEffect(ModEffects.RuinBade_Overload.getHolder().get())) return;

            LivingEntity victim = event.getEntity();
            int amp = getRuinAmplifier(victim);
            if (amp < 0) return;

            // Add 1 extra stack on top of what onPlayerHit already added
            int cap = (inCataclysm(player) ? MAX_STACKS_CATACLYSM : MAX_STACKS_NORMAL) - 1;
            addRuinStacks(victim, player, 1, cap);
        }
    }

    // =========================================================================
    //  ABILITY 6 — EXECUTION DRIVE  (finisher)
    // =========================================================================

    /**
     * If the target is below 35% HP: consume all Ruin stacks and deal massive
     * execute damage.  On kill: restore HP and reset ruin stacks.
     * Cost: 3000 soul essence.  Requires ascension stage ≥ 6.
     */
    public static void executionDrive(Player player, Level level, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Ruinblade Ascendant")) return;
        if (SoulCore.getSoulEssence(player) < 3000) return;
        if (SoulCore.getAscensionStage(player) < 6) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 3000);

        Vec3  start     = player.getEyePosition();
        Vec3  direction = player.getLookAngle().normalize();
        Vec3  current   = start;
        var   ruin      = ModEffects.RuinBade_Ruin.getHolder().get();
        int   stage     = SoulCore.getAscensionStage(player);

        boolean didHit = false;
        for (int i = 0; i <= 14 + stage && !didHit; i++) {
            current = current.add(direction);
            serverLevel.sendParticles(ParticleTypes.SOUL,
                    current.x, current.y, current.z, 3, 0.1, 0.1, 0.1, 0);

            List<LivingEntity> entities = level.getEntitiesOfClass(
                    LivingEntity.class, new AABB(current, current).inflate(0.5),
                    e -> e != player && e.isAlive());

            for (LivingEntity entity : entities) {

                float hpPercent = entity.getHealth() / entity.getMaxHealth();
                if (hpPercent > 0.35f) {
                    // Target not low enough — refund essence and abort
                    SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) + 3000);
                    if (player instanceof ServerPlayer sp) {
                        sp.sendSystemMessage(Component.literal("§cTarget must be below 35% HP!"));
                    }
                    return;
                }

                int amp    = getRuinAmplifier(entity);
                int stacks = amp + 1;
                float damage = 20.0f + stacks * 8.0f; // executes scale hard with stacks

                boolean willKill = entity.getHealth() - damage <= 0;

                entity.hurt(level.damageSources().playerAttack(player), damage);
                entity.invulnerableTime = 0;
                entity.removeEffect(ruin);

                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        entity.getX(), entity.getY() + 1, entity.getZ(), 12, 0.4, 0.4, 0.4, 0.05);
                level.playSound(null, entity.blockPosition(),
                        SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.PLAYERS, 1f, 0.5f);

                if (willKill) {
                    // Restore 8 hearts
                    player.heal(16.0f);
                    // Reset cooldowns (vanilla method)
                    player.resetAttackStrengthTicker();
                    if (player instanceof ServerPlayer sp) {
                        sp.sendSystemMessage(Component.literal("§4Execution! §cHP restored, cooldowns reset."));
                    }
                }

                didHit = true;
                break;
            }
        }
    }

    // =========================================================================
    //  ABILITY 7 — CATACLYSM STATE  (ultimate, 10-second transformation)
    // =========================================================================

    /**
     * Activates CATACLYSM STATE for 10 seconds (200 ticks).
     * Every hit → max stacks instantly, stack cap → 30, Detonation has no
     * cooldown, huge attack speed.  HP drains while active.
     * On kill while active: transfer stacks to nearest enemy (handled in tick).
     * Cost: 6000 soul essence.  Requires ascension stage ≥ 7.
     */
    public static void cataclysmState(Player player, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Ruinblade Ascendant")) return;
        if (SoulCore.getSoulEssence(player) < 6000) return;
        if (SoulCore.getAscensionStage(player) < 7) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 6000);

        // 10 seconds = 200 ticks
        player.addEffect(new MobEffectInstance(
                ModEffects.RuinBade_cataclysm.getHolder().get(), 200, 0, false, true));
        // Massive haste during cataclysm
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 3, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 2, false, false));

        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(), 3, 0.5, 0.5, 0.5, 0);
        serverLevel.playSound(null, player.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.5f, 0.5f);

        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("§4§lCATACLYSM STATE ACTIVATED!"));
        }
    }

    // ─── Cataclysm Tick: stack transfer on kill ────────────────────────────────

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class CataclysmEvents {

        /**
         * If an entity with Ruin stacks dies near a Cataclysm-active player,
         * transfer those stacks to the nearest remaining enemy.
         */
        @SubscribeEvent
        public static void onRuinTargetDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
            LivingEntity dead = event.getEntity();
            if (!(dead.level() instanceof ServerLevel serverLevel)) return;

            var ruin = ModEffects.RuinBade_Ruin.getHolder().get();
            if (!dead.hasEffect(ruin)) return;

            int transferAmp = dead.getEffect(ruin).getAmplifier();

            // Find a nearby Cataclysm-active Ruinblade player
            List<? extends Player> players = serverLevel.players();
            for (Player player : players) {
                if (!SoulCore.getAspect(player).equals("RuinBlade Ascendant")) continue;
                if (!inCataclysm(player)) continue;
                if (player.distanceToSqr(dead) > 400) continue; // 20-block range

                // Find the nearest living enemy to the dead entity
                AABB area = dead.getBoundingBox().inflate(16);
                List<LivingEntity> nearby = serverLevel.getEntitiesOfClass(
                        LivingEntity.class, area,
                        e -> e.isAlive() && !e.equals(player) && !e.equals(dead));

                if (nearby.isEmpty()) break;

                LivingEntity nearest    = nearby.get(0);
                double       nearestDist = nearest.distanceToSqr(dead);
                for (LivingEntity e : nearby) {
                    double d = e.distanceToSqr(dead);
                    if (d < nearestDist) { nearest = e; nearestDist = d; }
                }

                // Transfer stacks
                int cap = MAX_STACKS_CATACLYSM - 1;
                addRuinStacks(nearest, player, transferAmp + 1, cap);

                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        nearest.getX(), nearest.getY() + 1, nearest.getZ(),
                        8, 0.4, 0.4, 0.4, 0.04);
                break;
            }
        }
    }
}
