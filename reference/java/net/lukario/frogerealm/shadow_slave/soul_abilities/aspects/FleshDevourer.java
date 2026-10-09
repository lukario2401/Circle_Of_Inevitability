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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

/**
 * Flesh Devourer
 * -----------------------------------------------
 * Consumes enemies to grow stronger. Every fight makes you more monstrous.
 *
 * Core Mechanic — FLESH MASS (0–100):
 *   0–20   → Normal
 *   20–40  → Increased damage
 *   40–60  → High damage + lifesteal
 *   60–80  → Can devour at 25% HP, slight range boost
 *   80–100 → Damage resistance + max HP increase, devour at 30% HP, moderate range boost
 *
 * Devour threshold (base 20% HP, scales with mass and Apex):
 *   Normal          → 20% HP
 *   60–80 mass      → 25% HP
 *   80–100 mass     → 30% HP
 *   Apex Abomination→ 40% HP
 *
 * Ability kills count as devours (grant mass + healing).
 *
 * Player NBT keys:
 *   "FleshMass"        → float  current flesh mass (0–100)
 *   "FleshDecayTimer"  → int    ticks until next mass decay
 *   "FleshBleedTarget" → UUID   entity UUID with bleed applied
 *   "FleshBleedTimer"  → int    ticks remaining on bleed
 *   "FleshApex"        → int    ticks remaining on Apex Abomination
 *   "FleshFrenzy"      → int    ticks remaining on Blood Frenzy
 */
public class FleshDevourer {

    // ─── NBT keys ─────────────────────────────────────────────────────────────
    private static final String NBT_MASS         = "FleshMass";
    private static final String NBT_DECAY_TIMER  = "FleshDecayTimer";
    private static final String NBT_BLEED_TARGET = "FleshBleedTarget";
    private static final String NBT_BLEED_TIMER  = "FleshBleedTimer";
    private static final String NBT_APEX         = "FleshApex";
    private static final String NBT_FRENZY       = "FleshFrenzy";

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final float MASS_MAX            = 100f;
    private static final int   DECAY_INTERVAL      = 60;   // 3 seconds per 1 mass lost
    private static final int   BLEED_TICK_INTERVAL = 20;   // bleed damage every second
    private static final int   APEX_DURATION       = 240;  // 12 seconds
    private static final int   FRENZY_DURATION     = 100;  // 5 seconds

    // ─── Mass Helpers ─────────────────────────────────────────────────────────

    public static float getMass(Player player) {
        return player.getPersistentData().getFloat(NBT_MASS);
    }

    public static void setMass(Player player, float mass) {
        player.getPersistentData().putFloat(NBT_MASS, Math.max(0f, Math.min(MASS_MAX, mass)));
    }

    private static void addMass(Player player, float amount) {
        setMass(player, getMass(player) + (inApex(player) ? amount * 2f : amount));
    }

    public static boolean inApex(Player player) {
        return player.getPersistentData().getInt(NBT_APEX) > 0;
    }

    public static boolean inFrenzy(Player player) {
        return player.getPersistentData().getInt(NBT_FRENZY) > 0;
    }

    /** HP threshold below which Devour can be used. */
    private static float devourThreshold(Player player) {
        if (inApex(player)) return 0.40f;
        float mass = getMass(player);
        if (mass >= 80f) return 0.30f;
        if (mass >= 60f) return 0.25f;
        return 0.20f;
    }

    /** Damage multiplier based on current Flesh Mass. */
    private static float damageMult(Player player) {
        if (inApex(player)) return 2.0f;
        float mass = getMass(player);
        if (mass >= 80f) return 1.6f;
        if (mass >= 60f) return 1.4f;
        if (mass >= 40f) return 1.25f;
        if (mass >= 20f) return 1.1f;
        return 1.0f;
    }

    /** Coloured mass status for chat messages. */
    private static String massStatus(Player player) {
        float m = getMass(player);
        String tier = m >= 80f ? "§4[Abomination]"
                : m >= 60f ? "§c[Grotesque]"
                : m >= 40f ? "§6[Mutated]"
                : m >= 20f ? "§e[Hungry]"
                : "§7[Famished]";
        return tier + " §fMass: §b" + String.format("%.0f", m) + "/100";
    }

    // ─── Attribute application ────────────────────────────────────────────────

    private static void applyMassAttributes(Player player) {
        float mass = getMass(player);

        // Max health boost at 80+ mass or during Apex
        var maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            double bonus = (inApex(player)) ? 10.0
                    : (mass >= 80f)    ? 6.0
                    : 0.0;
            maxHealth.setBaseValue(20.0 + bonus);
            if (player.getHealth() > player.getMaxHealth())
                player.setHealth(player.getMaxHealth());
        }

        // Damage resistance at 80+ mass or during Apex
        if (mass >= 80f || inApex(player)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 25, 0, true, false));
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

    // ─── Core devour logic ────────────────────────────────────────────────────

    private static void performDevour(Player player, LivingEntity target,
                                      Level level, ServerLevel sl) {
        float maxHP   = target.getMaxHealth();
        float massGain = Math.min(15f + (maxHP / 10f), 30f);
        float healAmt  = Math.min(player.getMaxHealth() * 0.12f, 6f);

        target.hurt(level.damageSources().playerAttack(player), 10000f);
        player.heal(healAmt * (inApex(player) ? 2.5f : 1f));
        addMass(player, massGain);

        // AOE burst on devour during Apex
        if (inApex(player)) {
            AABB aoeBox = target.getBoundingBox().inflate(5);
            level.getEntitiesOfClass(LivingEntity.class, aoeBox,
                            e -> e != player && e != target && e.isAlive())
                    .forEach(e -> {
                        e.hurt(level.damageSources().playerAttack(player), 12.0f * damageMult(player));
                        e.invulnerableTime = 0;
                        sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                                e.getX(), e.getY() + 1, e.getZ(), 6, 0.3, 0.3, 0.3, 0.03);
                    });
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
        }

        sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                target.getX(), target.getY() + 1, target.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
        level.playSound(null, target.blockPosition(),
                SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1f, 0.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§4Devoured! §c+" + String.format("%.1f", massGain) + " mass, +"
                            + String.format("%.1f", healAmt) + " HP. " + massStatus(player)));
    }

    // =========================================================================
    //  EVENTS — single class, unique name
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class FleshEvents {

        @SubscribeEvent
        public static void onFleshPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Flesh Devourer")) return;

            // ── Apex countdown ────────────────────────────────────────────────
            int apex = player.getPersistentData().getInt(NBT_APEX);
            if (apex > 0) {
                player.getPersistentData().putInt(NBT_APEX, apex - 1);
                if (player.tickCount % 6 == 0)
                    sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                            player.getX(), player.getY() + 1, player.getZ(), 5, 0.5, 0.5, 0.5, 0.04);
            }

            // ── Blood Frenzy countdown ────────────────────────────────────────
            int frenzy = player.getPersistentData().getInt(NBT_FRENZY);
            if (frenzy > 0) {
                player.getPersistentData().putInt(NBT_FRENZY, frenzy - 1);
                if (player.tickCount % 20 == 0) {
                    float hpCost = 2.0f;
                    if (player.getHealth() > hpCost + 1f) {
                        player.hurt(player.level().damageSources().magic(), hpCost);
                        addMass(player, 8f);
                    } else {
                        player.getPersistentData().putInt(NBT_FRENZY, 0);
                        if (player instanceof ServerPlayer sp)
                            sp.sendSystemMessage(Component.literal("§cBlood Frenzy ended — not enough HP!"));
                    }
                }
            }

            // ── Bleed tick ────────────────────────────────────────────────────
            int bleedTimer = player.getPersistentData().getInt(NBT_BLEED_TIMER);
            if (bleedTimer > 0) {
                bleedTimer--;
                player.getPersistentData().putInt(NBT_BLEED_TIMER, bleedTimer);

                if (player.getPersistentData().contains(NBT_BLEED_TARGET)) {
                    UUID bleedUUID = player.getPersistentData().getUUID(NBT_BLEED_TARGET);
                    if (bleedTimer % BLEED_TICK_INTERVAL == 0) {
                        sl.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(30),
                                        e -> e.getUUID().equals(bleedUUID) && e.isAlive())
                                .forEach(e -> {
                                    e.hurt(sl.damageSources().playerAttack(player), 3.0f);
                                    e.invulnerableTime = 0;
                                    sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                                            e.getX(), e.getY() + 1, e.getZ(), 4, 0.2, 0.2, 0.2, 0.02);
                                });
                    }
                }
                if (bleedTimer == 0) player.getPersistentData().remove(NBT_BLEED_TARGET);
            }

            // ── Passive mass decay ────────────────────────────────────────────
            float mass = getMass(player);
            if (mass > 0) {
                int decayTimer = player.getPersistentData().getInt(NBT_DECAY_TIMER);
                decayTimer--;
                if (decayTimer <= 0) {
                    setMass(player, mass - 1f);
                    decayTimer = DECAY_INTERVAL;
                }
                player.getPersistentData().putInt(NBT_DECAY_TIMER, decayTimer);
            }

            // ── Attribute effects (every second) ──────────────────────────────
            if (player.tickCount % 20 == 0) applyMassAttributes(player);

            // ── Mass HUD particles ────────────────────────────────────────────
            if (player.tickCount % 10 == 0 && mass > 0) {
                int count = Math.max(1, (int)(mass / 25f));
                sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                        player.getX(), player.getY() + 2.0, player.getZ(),
                        count, 0.3, 0.1, 0.3, 0.01);
            }
        }

        /** Lifesteal on hit when mass >= 40. */
        @SubscribeEvent
        public static void onFleshHit(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Flesh Devourer")) return;
            float mass = getMass(player);
            if (mass < 40f && !inApex(player)) return;
            float healPct = (mass >= 80f || inApex(player)) ? 0.12f : 0.07f;
            player.heal(event.getAmount() * healPct);
        }

        /** Ability kills grant mass + healing (count as devours). */
        @SubscribeEvent
        public static void onFleshKill(LivingDeathEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Flesh Devourer")) return;
            if (!(player.level() instanceof ServerLevel sl)) return;

            LivingEntity dead = event.getEntity();
            float massGain = Math.min(8f + dead.getMaxHealth() / 15f, 20f);
            addMass(player, massGain);
            player.heal(Math.min(player.getMaxHealth() * 0.08f, 4f) * (inApex(player) ? 2.5f : 1f));

            // Apex: AOE burst on every kill
            if (inApex(player)) {
                AABB aoeBox = dead.getBoundingBox().inflate(5);
                player.level().getEntitiesOfClass(LivingEntity.class, aoeBox,
                                e -> e != player && e.isAlive())
                        .forEach(e -> {
                            e.hurt(sl.damageSources().playerAttack(player), 10.0f);
                            e.invulnerableTime = 0;
                            sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                                    e.getX(), e.getY() + 1, e.getZ(), 5, 0.3, 0.3, 0.3, 0.03);
                        });
            }

            sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    dead.getX(), dead.getY() + 1, dead.getZ(), 10, 0.4, 0.4, 0.4, 0.04);
        }
    }

    // =========================================================================
    //  ABILITY 1 — REND
    // =========================================================================

    public static void fleshDevourerRend(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Flesh Devourer")) return;
        if (SoulCore.getSoulEssence(player) < 300) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 300);

        int   stage  = SoulCore.getAscensionStage(player);
        LivingEntity target = rayCastFirst(player, level, 5 + stage);
        if (target == null) return;

        float damage = (12.0f + stage * 2) * damageMult(player);
        float hpPct  = target.getHealth() / target.getMaxHealth();
        if (hpPct < 0.35f) damage *= 1.4f;

        target.hurt(level.damageSources().playerAttack(player), damage);
        target.invulnerableTime = 0;

        player.getPersistentData().putUUID(NBT_BLEED_TARGET, target.getUUID());
        player.getPersistentData().putInt(NBT_BLEED_TIMER, 100);

        sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                target.getX(), target.getY() + 1, target.getZ(), 12, 0.4, 0.4, 0.4, 0.05);
        level.playSound(null, target.blockPosition(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 0.7f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§4Rend: §f" + String.format("%.1f", damage) + " dmg + §cbleed. "
                            + (hpPct < 0.35f ? "§6[Low HP bonus ×1.4!] " : "") + massStatus(player)));
    }

    // =========================================================================
    //  ABILITY 2 — DEVOUR
    // =========================================================================

    public static void devour(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Flesh Devourer")) return;
        if (SoulCore.getSoulEssence(player) < 500) return;
        if (SoulCore.getAscensionStage(player) < 1) return;

        LivingEntity target = rayCastFirst(player, level, 4 + SoulCore.getAscensionStage(player));
        if (target == null) return;

        float threshold = devourThreshold(player);
        float hpPct     = target.getHealth() / target.getMaxHealth();

        if (hpPct > threshold) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§cTarget must be below §e" + (int)(threshold * 100)
                                + "% HP §cto Devour! Current: §e" + String.format("%.0f", hpPct * 100) + "%"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 500);
        performDevour(player, target, level, sl);
    }

    // =========================================================================
    //  ABILITY 3 — MEAT BURST
    // =========================================================================

    public static void meatBurst(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Flesh Devourer")) return;
        if (SoulCore.getSoulEssence(player) < 400) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        float mass = getMass(player);
        if (mass < 5f) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNeed at least 5 Flesh Mass!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 400);

        int   stage    = SoulCore.getAscensionStage(player);
        float consumed = Math.min(mass, 50f);
        float damage   = (8.0f + consumed * 0.6f + stage * 2) * damageMult(player);
        float radius   = 3.0f + consumed / 20f + stage * 0.3f;

        setMass(player, mass - consumed);

        AABB box = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, box, e -> e != player && e.isAlive());

        for (LivingEntity e : targets) {
            e.hurt(level.damageSources().playerAttack(player), damage);
            e.invulnerableTime = 0;
            Vec3 push = e.position().subtract(player.position()).normalize().scale(0.7);
            e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.3, push.z));
            sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    e.getX(), e.getY() + 1, e.getZ(), 8, 0.3, 0.3, 0.3, 0.04);
        }

        for (int i = 0; i < 24; i++) {
            double angle = (2 * Math.PI / 24) * i;
            double px = player.getX() + radius * Math.cos(angle);
            double pz = player.getZ() + radius * Math.sin(angle);
            sl.sendParticles(ParticleTypes.CRIMSON_SPORE, px, player.getY() + 0.5, pz, 1, 0, 0.2, 0, 0.02);
        }
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, player.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1f, 0.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§4Meat Burst: consumed §c" + String.format("%.0f", consumed)
                            + " mass §4→ §f" + String.format("%.1f", damage)
                            + " dmg, r" + String.format("%.1f", radius)
                            + ", hit §c" + targets.size() + "§f. " + massStatus(player)));
    }

    // =========================================================================
    //  ABILITY 4 — GROTESQUE GROWTH
    // =========================================================================

    public static void grotesqueGrowth(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Flesh Devourer")) return;
        if (SoulCore.getSoulEssence(player) < 300) return;
        if (SoulCore.getAscensionStage(player) < 3) return;

        float mass = getMass(player);
        if (mass < 10f) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNeed at least 10 Flesh Mass to grow!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 300);

        int   stage    = SoulCore.getAscensionStage(player);
        float consumed = Math.min(mass, 30f);
        float healAmt  = consumed * 0.4f + stage * 1.5f;

        setMass(player, mass - consumed);
        player.heal(healAmt);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, true));

        sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                player.getX(), player.getY() + 1, player.getZ(), 16, 0.4, 0.4, 0.4, 0.04);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 0.8f, 0.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§4Grotesque Growth: consumed §c" + String.format("%.0f", consumed)
                            + " mass §4→ §ahealed " + String.format("%.1f", healAmt) + " HP. " + massStatus(player)));
    }

    // =========================================================================
    //  ABILITY 5 — FLESH HOOK
    // =========================================================================

    public static void fleshHook(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Flesh Devourer")) return;
        if (SoulCore.getSoulEssence(player) < 600) return;
        if (SoulCore.getAscensionStage(player) < 4) return;

        int stage = SoulCore.getAscensionStage(player);
        LivingEntity target = rayCastFirst(player, level, 16 + stage);
        if (target == null) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 600);

        // Tendril particle trail
        Vec3 start = target.position().add(0, 1, 0);
        Vec3 end   = player.position().add(0, 1, 0);
        Vec3 tDir  = end.subtract(start).normalize();
        double dist = start.distanceTo(end);
        Vec3 cur = start;
        for (double d = 0; d < dist; d += 0.5) {
            sl.sendParticles(ParticleTypes.CRIMSON_SPORE, cur.x, cur.y, cur.z, 1, 0.05, 0.05, 0.05, 0);
            cur = cur.add(tDir.scale(0.5));
        }

        // Instant devour if target is low
        float hpPct = target.getHealth() / target.getMaxHealth();
        if (hpPct <= devourThreshold(player)) {
            performDevour(player, target, level, sl);
            return;
        }

        // Pull toward player
        Vec3 pull = player.position().subtract(target.position()).normalize().scale(1.5);
        target.setDeltaMovement(target.getDeltaMovement().add(pull.x, 0.4, pull.z));
        target.hurtMarked = true;

        // Apply bleed
        player.getPersistentData().putUUID(NBT_BLEED_TARGET, target.getUUID());
        player.getPersistentData().putInt(NBT_BLEED_TIMER, 80);

        float hookDmg = (8.0f + stage * 1.5f) * damageMult(player);
        target.hurt(level.damageSources().playerAttack(player), hookDmg);
        target.invulnerableTime = 0;

        level.playSound(null, target.blockPosition(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8f, 0.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§4Flesh Hook: §fpulled + §cbleed. §f"
                            + String.format("%.1f", hookDmg) + " dmg. " + massStatus(player)));
    }

    // =========================================================================
    //  ABILITY 6 — BLOOD FRENZY
    // =========================================================================

    public static void bloodFrenzy(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Flesh Devourer")) return;
        if (SoulCore.getSoulEssence(player) < 200) return;
        if (SoulCore.getAscensionStage(player) < 5) return;
        if (player.getHealth() <= 6f) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNot enough HP for Blood Frenzy!"));
            return;
        }
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 200);

        player.getPersistentData().putInt(NBT_FRENZY, FRENZY_DURATION);

        sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1f, 0.4f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§4§lBlood Frenzy! §r§cSacrificing HP for mass. " + massStatus(player)));
    }

    // =========================================================================
    //  ABILITY 7 — APEX ABOMINATION
    // =========================================================================

    public static void apexAbomination(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Flesh Devourer")) return;
        if (SoulCore.getSoulEssence(player) < 5000) return;
        if (SoulCore.getAscensionStage(player) < 7) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 5000);

        player.getPersistentData().putInt(NBT_APEX, APEX_DURATION);

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,      APEX_DURATION, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,  APEX_DURATION, 1, false, false));

        sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                player.getX(), player.getY() + 1, player.getZ(), 50, 1.0, 1.0, 1.0, 0.07);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(), 2, 0.3, 0.3, 0.3, 0);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.5f, 0.3f);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1f, 0.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§4§l☠ APEX ABOMINATION ☠ §r§cDevour at 40% HP. Double mass. Every kill AOE heals. 12s."));
    }
}