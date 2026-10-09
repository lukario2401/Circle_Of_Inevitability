package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
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
 * Astral Arbiter
 * -----------------------------------------------
 * Orb-based resource management DPS.
 * Orbs = Ascension Stage count (max 7).
 * All state is stored in PersistentData on the player:
 *
 *   "AstralOrbs"            → int  current orbs
 *   "AstralTempOrbs"        → int  temporary orbs (from Orb Split)
 *   "AstralTempOrbTimer"    → int  ticks until temp orbs expire
 *   "AstralOverdrawTimer"   → int  ticks remaining on Cosmic Overdraw
 *   "AstralConvergenceTimer"→ int  ticks remaining on Astral Convergence
 *   "AstralOrbRegenTimer"   → int  ticks until next auto-regen tick (Convergence)
 */
public class AstralArbiter {

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final String NBT_ORBS          = "AstralOrbs";
    private static final String NBT_TEMP_ORBS     = "AstralTempOrbs";
    private static final String NBT_TEMP_TIMER    = "AstralTempOrbTimer";
    private static final String NBT_OVERDRAW       = "AstralOverdrawTimer";
    private static final String NBT_CONVERGENCE    = "AstralConvergenceTimer";
    private static final String NBT_REGEN_TIMER    = "AstralOrbRegenTimer";
    private static final String NBT_PASSIVE_REGEN  = "AstralPassiveRegenTimer";

    // Passive regen: 1 orb every 100 ticks (5 seconds) when below max
    private static final int PASSIVE_REGEN_INTERVAL = 100;

    // ─── Orb Helpers ──────────────────────────────────────────────────────────

    /** Max orbs = ascension stage, +2 during Convergence. */
    public static int maxOrbs(Player player) {
        int base = SoulCore.getAscensionStage(player);
        if (inConvergence(player)) base += 2;
        return base;
    }

    public static int getOrbs(Player player) {
        return player.getPersistentData().getInt(NBT_ORBS);
    }

    public static void setOrbs(Player player, int amount) {
        int capped = Math.max(0, Math.min(amount, maxOrbs(player)));
        player.getPersistentData().putInt(NBT_ORBS, capped);
    }

    public static int getTempOrbs(Player player) {
        return player.getPersistentData().getInt(NBT_TEMP_ORBS);
    }

    public static void setTempOrbs(Player player, int amount) {
        player.getPersistentData().putInt(NBT_TEMP_ORBS, Math.max(0, amount));
    }

    /** Total usable orbs = real + temp. */
    public static int totalOrbs(Player player) {
        return getOrbs(player) + getTempOrbs(player);
    }

    /**
     * Consume {@code amount} orbs, spending temp orbs first.
     * Returns how many were actually consumed (may be less if not enough).
     */
    private static int consumeOrbs(Player player, int amount) {
        int consumed = 0;
        int temp = getTempOrbs(player);
        if (temp > 0) {
            int fromTemp = Math.min(temp, amount);
            setTempOrbs(player, temp - fromTemp);
            consumed += fromTemp;
            amount   -= fromTemp;
        }
        if (amount > 0) {
            int real = getOrbs(player);
            int fromReal = Math.min(real, amount);
            setOrbs(player, real - fromReal);
            consumed += fromReal;
        }
        return consumed;
    }

    /** Restores orbs, capped at max.  Returns actual amount restored. */
    private static int restoreOrbs(Player player, int amount) {
        int before = getOrbs(player);
        int after  = Math.min(before + amount, maxOrbs(player));
        setOrbs(player, after);
        return after - before;
    }

    public static boolean inOverdraw(Player player) {
        return player.getPersistentData().getInt(NBT_OVERDRAW) > 0;
    }

    public static boolean inConvergence(Player player) {
        // Can't call maxOrbs() here — that would recurse. Read raw timer.
        return player.getPersistentData().getInt(NBT_CONVERGENCE) > 0;
    }

    /** Cost modifier during Convergence: abilities use 1 fewer orb (min 0). */
    private static int costWithModifiers(Player player, int baseCost) {
        int cost = baseCost;
        if (inConvergence(player)) cost = Math.max(0, cost - 1);
        return cost;
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

    // ─── Orb chain explosion (Convergence bonus) ──────────────────────────────

    /** When Convergence is active, every orb spent chains damage to nearby enemies. */
    private static void convergenceChain(Player player, ServerLevel sl, Vec3 origin, int orbsSpent) {
        if (!inConvergence(player)) return;
        float chainDamage = orbsSpent * 3.0f;
        AABB area = new AABB(origin, origin).inflate(5);
        List<LivingEntity> nearby = sl.getEntitiesOfClass(
                LivingEntity.class, area, e -> e != player && e.isAlive());
        for (LivingEntity e : nearby) {
            e.hurt(sl.damageSources().magic(), chainDamage);
            e.invulnerableTime = 0;
            sl.sendParticles(ParticleTypes.END_ROD,
                    e.getX(), e.getY() + 1, e.getZ(), 4, 0.2, 0.2, 0.2, 0.03);
        }
    }

    // ─── Orb status string (shown after every ability) ────────────────────────

    /** Returns a coloured orb count string, e.g. "§b[Orbs: §32/5§b]" */
    private static String orbStatus(Player player) {
        int current = totalOrbs(player);
        int max     = maxOrbs(player);
        int real    = getOrbs(player);
        int temp    = getTempOrbs(player);
        String tempStr = temp > 0 ? " §7(+" + temp + " temp)§b" : "";
        return "§b[Orbs: §3" + real + tempStr + "§b/§3" + max + "§b]";
    }

    // =========================================================================
    //  TICK EVENT — timers, temp orb expiry, Convergence regen, orb particles
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class TickEvents {

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Astral Arbiter")) return;

            // ── Overdraw countdown ────────────────────────────────────────────
            int overdraw = player.getPersistentData().getInt(NBT_OVERDRAW);
            if (overdraw > 0)
                player.getPersistentData().putInt(NBT_OVERDRAW, overdraw - 1);

            // ── Convergence countdown ─────────────────────────────────────────
            int conv = player.getPersistentData().getInt(NBT_CONVERGENCE);
            if (conv > 0) {
                player.getPersistentData().putInt(NBT_CONVERGENCE, conv - 1);

                // Auto regen: every 20 ticks (1s) at normal, every 10 ticks at final tier
                int regenInterval = (SoulCore.getAscensionStage(player) >= 7) ? 10 : 20;
                int regenTimer = player.getPersistentData().getInt(NBT_REGEN_TIMER);
                regenTimer--;
                if (regenTimer <= 0) {
                    restoreOrbs(player, 1);
                    regenTimer = regenInterval;
                    sl.sendParticles(ParticleTypes.END_ROD,
                            player.getX(), player.getY() + 1.5, player.getZ(),
                            3, 0.3, 0.3, 0.3, 0.02);
                }
                player.getPersistentData().putInt(NBT_REGEN_TIMER, regenTimer);
            }

            // ── Temp orb expiry ───────────────────────────────────────────────
            int tempTimer = player.getPersistentData().getInt(NBT_TEMP_TIMER);
            if (getTempOrbs(player) > 0) {
                tempTimer--;
                if (tempTimer <= 0) {
                    setTempOrbs(player, 0);
                    tempTimer = 0;
                    if (player instanceof ServerPlayer sp)
                        sp.sendSystemMessage(Component.literal("§7Temporary orbs expired."));
                }
                player.getPersistentData().putInt(NBT_TEMP_TIMER, tempTimer);
            }

            // ── Passive orb regen: +1 orb every 5 seconds when below max ─────
            // Convergence has its own faster regen above, so skip passive there
            if (!inConvergence(player)) {
                if (getOrbs(player) < maxOrbs(player)) {
                    int passiveTimer = player.getPersistentData().getInt(NBT_PASSIVE_REGEN);
                    passiveTimer--;
                    if (passiveTimer <= 0) {
                        restoreOrbs(player, 1);
                        passiveTimer = PASSIVE_REGEN_INTERVAL;
                        // Small visual cue on regen
                        sl.sendParticles(ParticleTypes.END_ROD,
                                player.getX(), player.getY() + 1.5, player.getZ(),
                                4, 0.3, 0.3, 0.3, 0.02);
                        if (player instanceof ServerPlayer sp)
                            sp.sendSystemMessage(Component.literal(
                                    "§bOrb regenerated. " + orbStatus(player)));
                    }
                    player.getPersistentData().putInt(NBT_PASSIVE_REGEN, passiveTimer);
                } else {
                    // Reset timer so the countdown always starts fresh after spending an orb
                    player.getPersistentData().putInt(NBT_PASSIVE_REGEN, PASSIVE_REGEN_INTERVAL);
                }
            }

            // ── Static orb particles above the player's head ──────────────────
            // Runs every 3 ticks. Orbs sit in a fixed horizontal arc above the
            // player — they follow the player but do NOT spin or rotate.
            if (player.tickCount % 3 == 0) {
                int total    = totalOrbs(player);
                int tempOrbs = getTempOrbs(player);
                if (total <= 0) return;

                // Spread orbs evenly across a fixed 120-degree arc above the head
                double arcSpan   = Math.toRadians(160);
                double arcStart  = arcSpan / 2.0;
                double spacing   = total > 1 ? arcSpan / (total - 1) : 0;

                for (int i = 0; i < total; i++) {
                    double angle = arcStart + spacing * i;
                    double x = player.getX() + 0.9 * Math.sin(angle);
                    double z = player.getZ() + 0.9 * Math.cos(angle);
                    double y = player.getY() + 2.3; // float above head

                    boolean isTemp = i >= (total - tempOrbs);

                    if (inConvergence(player)) {
                        sl.sendParticles(ParticleTypes.GLOW,       x, y, z, 1, 0, 0, 0, 0);
                    } else if (inOverdraw(player)) {
                        sl.sendParticles(ParticleTypes.FLAME,       x, y, z, 1, 0, 0, 0, 0);
                    } else if (isTemp) {
                        sl.sendParticles(ParticleTypes.BUBBLE_POP,  x, y, z, 1, 0, 0, 0, 0);
                    } else {
                        sl.sendParticles(ParticleTypes.END_ROD,     x, y, z, 1, 0, 0, 0, 0);
                    }
                }
            }
//            Component message = Component.literal("§bOrbs: §f" + totalOrbs(player) + " §7| §5Temporary: §f" + getTempOrbs(player) + "s");
//            player.displayClientMessage(message, true);
        }
    }

    // =========================================================================
    //  ABILITY 1 — ASTRAL BOLT  (basic shot, optional 1-orb upgrade)
    // =========================================================================

    /**
     * Fires a fast piercing ray-cast bolt.
     * Free version: low damage, hits one target.
     * Orb version (costs 1 orb): double damage + pierces all enemies in path.
     * Cost: 300 soul essence.
     */
    public static void astralBolt(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Astral Arbiter")) return;
        if (SoulCore.getSoulEssence(player) < 300) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 300);

        int stage   = SoulCore.getAscensionStage(player);
        int orbCost = costWithModifiers(player, 1);

        boolean empowered = totalOrbs(player) >= orbCost && orbCost > 0;
        int consumed = 0;
        if (empowered) consumed = consumeOrbs(player, orbCost);

        Vec3 start = player.getEyePosition();
        Vec3 dir   = player.getLookAngle().normalize();
        Vec3 cur   = start;
        int range  = 12 + stage;

        float baseDamage = empowered ? 14.0f : 6.0f;

        // Visual beam
        for (int i = 0; i < range; i++) {
            cur = cur.add(dir);
            sl.sendParticles(
                    empowered ? ParticleTypes.END_ROD : ParticleTypes.CRIT,
                    cur.x, cur.y, cur.z, 1, 0.05, 0.05, 0.05, 0);

            List<LivingEntity> hits = level.getEntitiesOfClass(
                    LivingEntity.class, new AABB(cur, cur).inflate(0.4),
                    e -> e != player && e.isAlive());

            for (LivingEntity hit : hits) {
                hit.hurt(level.damageSources().playerAttack(player), baseDamage);
                hit.invulnerableTime = 5;
                sl.sendParticles(ParticleTypes.CRIT,
                        hit.getX(), hit.getY() + 1, hit.getZ(), 6, 0.2, 0.2, 0.2, 0.04);
                if (!empowered) { // non-pierce: stop at first target
                    if (inConvergence(player) && consumed > 0)
                        convergenceChain(player, sl, hit.position(), consumed);
                    return;
                }
            }
        }

        if (inConvergence(player) && consumed > 0)
            convergenceChain(player, sl, cur, consumed);

        level.playSound(null, player.blockPosition(),
                SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.8f,
                empowered ? 0.6f : 1.2f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§bAstral Bolt " + (empowered ? "§3[Empowered]" : "§7[Basic]") + " " + orbStatus(player)));
    }

    // =========================================================================
    //  ABILITY 2 — ORB RECLAIM  (recovery beam with arc)
    // =========================================================================

    /**
     * Fires a beam + small horizontal arc:
     *   0 enemies hit → recover 1 orb
     *   1 enemy hit   → recover 1 orb
     *   2 enemies hit → recover 2 orbs
     *   3+ enemies hit → recover 3 orbs
     * At 0 current orbs → recovery is doubled.
     * Cost: 400 soul essence.  Requires stage ≥ 1.
     */
    public static void orbReclaim(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Astral Arbiter")) return;
        if (SoulCore.getSoulEssence(player) < 400) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 400);

        int stage = SoulCore.getAscensionStage(player);
        Vec3 start = player.getEyePosition();
        Vec3 dir   = player.getLookAngle().normalize();

        // Collect all unique entities hit (beam + arc)
        java.util.Set<UUID> hitUUIDs = new java.util.HashSet<>();

        // ── Forward beam ──────────────────────────────────────────────────────
        Vec3 cur = start;
        for (int i = 0; i < 14 + stage; i++) {
            cur = cur.add(dir);
            sl.sendParticles(ParticleTypes.FALLING_WATER, cur.x, cur.y, cur.z, 1, 0.05, 0.05, 0.05, 0);
            level.getEntitiesOfClass(LivingEntity.class, new AABB(cur, cur).inflate(0.4),
                            e -> e != player && e.isAlive())
                    .forEach(e -> hitUUIDs.add(e.getUUID()));
        }

        // ── Horizontal arc: sweep ±45° around look direction ──────────────────
        double baseYaw = Math.toDegrees(Math.atan2(-dir.x, dir.z));
        for (int offset = -45; offset <= 45; offset += 15) {
            double yaw    = Math.toRadians(baseYaw + offset);
            Vec3   arcDir = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)).normalize();
            Vec3   arcCur = start;
            for (int i = 0; i < 8; i++) {
                arcCur = arcCur.add(arcDir);
                sl.sendParticles(ParticleTypes.FALLING_WATER,
                        arcCur.x, arcCur.y, arcCur.z, 1, 0.05, 0.05, 0.05, 0);
                level.getEntitiesOfClass(LivingEntity.class,
                                new AABB(arcCur, arcCur).inflate(0.4),
                                e -> e != player && e.isAlive())
                        .forEach(e -> hitUUIDs.add(e.getUUID()));
            }
        }

        // ── Calculate recovery ────────────────────────────────────────────────
        int hitCount = hitUUIDs.size();
        int recover  = hitCount == 0 ? 1
                : hitCount == 1 ? 1
                : hitCount == 2 ? 2
                : 3;

        boolean atZero = getOrbs(player) == 0;
        if (atZero) recover *= 2;

        int actual = restoreOrbs(player, recover);

        level.playSound(null, player.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1f, 0.8f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§bOrb Reclaim: §3hit " + hitCount + " enemies → §b+" + actual + " orb(s)"
                            + (atZero ? " §7(doubled — was empty)" : "") + " " + orbStatus(player)));
    }

    // =========================================================================
    //  ABILITY 3 — STARFALL STRIKE  (targeted burst, up to 3 orbs)
    // =========================================================================

    /**
     * Calls a strike from above on the looked-at target.
     * Consumes up to 3 orbs:
     *   0 orbs → 20 damage, no AOE
     *   1 orb  → 35 damage, small AOE r2
     *   2 orbs → 55 damage, AOE r4
     *   3 orbs → 80 damage, AOE r6
     * Cost: 800 soul essence.  Requires stage ≥ 2.
     */
    public static void starfallStrike(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Astral Arbiter")) return;
        if (SoulCore.getSoulEssence(player) < 800) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        LivingEntity target = rayCastFirst(player, level, 16 + SoulCore.getAscensionStage(player)*2);
        if (target == null) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 800);

        int available = Math.min(totalOrbs(player), costWithModifiers(player, 3) == 0 ? 0 : 3);
        int toConsume = Math.min(available, costWithModifiers(player, 3));
        int consumed  = consumeOrbs(player, toConsume);

        float damage   = 20.0f + consumed * 25.0f;
        float aoeRadius = consumed * 2.0f;

        if (inOverdraw(player)) damage *= 1.5f;

        // Strike-from-above particle pillar
        Vec3 strikePos = target.position();
        for (double dy = 0; dy < 12; dy += 0.5) {
            sl.sendParticles(ParticleTypes.END_ROD,
                    strikePos.x, strikePos.y + dy, strikePos.z, 1, 0.1, 0, 0.1, 0);
        }

        target.hurt(level.damageSources().playerAttack(player), damage);
        target.invulnerableTime = 0;
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                strikePos.x, strikePos.y + 1, strikePos.z, 1, 0, 0, 0, 0);

        // AOE if any orbs were spent
        if (aoeRadius > 0) {
            AABB aoeBox = target.getBoundingBox().inflate(aoeRadius);
            float finalDamage = damage;
            level.getEntitiesOfClass(LivingEntity.class, aoeBox,
                            e -> e != player && e != target && e.isAlive())
                    .forEach(e -> {
                        e.hurt(level.damageSources().playerAttack(player), finalDamage * 0.5f);
                        e.invulnerableTime = 0;
                        sl.sendParticles(ParticleTypes.CRIT,
                                e.getX(), e.getY() + 1, e.getZ(), 5, 0.2, 0.2, 0.2, 0.04);
                    });
        }

        if (inConvergence(player) && consumed > 0)
            convergenceChain(player, sl, strikePos, consumed);

        level.playSound(null, target.blockPosition(),
                SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1f, 1.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§bStarfall Strike: §3" + consumed + " orb(s) spent → §b"
                            + String.format("%.0f", damage) + " dmg, r§3"
                            + String.format("%.0f", aoeRadius) + " §b" + orbStatus(player)));
    }

    // =========================================================================
    //  ABILITY 4 — ASTRAL PULSE  (AOE burst, consumes ALL orbs)
    // =========================================================================

    /**
     * Releases a shockwave around the player.
     * Consumes ALL orbs; each orb adds +4 radius and +8 damage.
     * Base: radius 4, damage 10.
     * Cost: 600 soul essence.  Requires stage ≥ 3.
     */
    public static void astralPulse(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Astral Arbiter")) return;
        if (SoulCore.getSoulEssence(player) < 600) return;
        if (SoulCore.getAscensionStage(player) < 3) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 600);

        int consumed = consumeOrbs(player, totalOrbs(player)); // consume ALL
        float radius = 4.0f + consumed * 4.0f;
        float damage = 10.0f + consumed * 8.0f;

        if (inOverdraw(player)) damage *= 1.5f;

        AABB pulseBox = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, pulseBox, e -> e != player && e.isAlive());

        for (LivingEntity e : targets) {
            e.hurt(level.damageSources().playerAttack(player), damage);
            e.invulnerableTime = 0;
            // Knock outward
            Vec3 pushDir = e.position().subtract(player.position()).normalize().scale(0.6);
            e.setDeltaMovement(e.getDeltaMovement().add(pushDir.x, 0.3, pushDir.z));
        }

        // Expanding ring of particles
        for (int ring = 0; ring < 3; ring++) {
            double r = (ring + 1) * (radius / 3.0);
            int    count = (int)(r * 8);
            for (int i = 0; i < count; i++) {
                double angle = (2 * Math.PI / count) * i;
                double px = player.getX() + r * Math.cos(angle);
                double pz = player.getZ() + r * Math.sin(angle);
                sl.sendParticles(ParticleTypes.END_ROD, px, player.getY() + 0.5, pz, 1, 0, 0.1, 0, 0);
            }
        }
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);

        if (inConvergence(player) && consumed > 0)
            convergenceChain(player, sl, player.position(), consumed);

        level.playSound(null, player.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.2f, 0.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§bAstral Pulse: consumed §3" + consumed + "§b orbs → radius §3"
                            + String.format("%.0f", radius) + "§b, hit §3" + targets.size()
                            + "§b enemies. " + orbStatus(player)));
    }

    // =========================================================================
    //  ABILITY 5 — ORB SPLIT  (1 real orb → 3 temporary orbs)
    // =========================================================================

    /**
     * Converts 1 real orb into 3 temporary orbs (expire in 4 seconds / 80 ticks).
     * Temp orbs are weaker: abilities treat them the same mechanically but
     * they are consumed first and expire if unused.
     * Cost: 200 soul essence.  Requires stage ≥ 4.
     */
    public static void orbSplit(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Astral Arbiter")) return;
        if (SoulCore.getSoulEssence(player) < 200) return;
        if (SoulCore.getAscensionStage(player) < 4) return;
        if (getOrbs(player) < 1) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNeed at least 1 orb to split!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 200);

        setOrbs(player, getOrbs(player) - 1);
        setTempOrbs(player, getTempOrbs(player) + 3);
        player.getPersistentData().putInt(NBT_TEMP_TIMER, 80); // 4 seconds

        sl.sendParticles(ParticleTypes.BUBBLE_POP,
                player.getX(), player.getY() + 1, player.getZ(), 15, 0.4, 0.4, 0.4, 0.05);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1f, 1.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§bOrb Split: 1 orb → §33 temp orbs§b (4 seconds). " + orbStatus(player)));
    }

    // =========================================================================
    //  ABILITY 6 — COSMIC OVERDRAW  (consume ALL orbs, massive 5s DPS window)
    // =========================================================================

    /**
     * Consumes ALL orbs for a 5-second empowered window.
     * During Overdraw all abilities deal ×1.5 damage (applied in each ability).
     * After, you have 0 orbs — significant downside.
     * Cost: 2500 soul essence.  Requires stage ≥ 5.
     */
    public static void cosmicOverdraw(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Astral Arbiter")) return;
        if (SoulCore.getSoulEssence(player) < 2500) return;
        if (SoulCore.getAscensionStage(player) < 5) return;
        if (totalOrbs(player) == 0) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo orbs to draw from!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 2500);

        // Drain all orbs
        int drained = totalOrbs(player);
        consumeOrbs(player, drained);
        setTempOrbs(player, 0);

        // 5 seconds = 100 ticks
        player.getPersistentData().putInt(NBT_OVERDRAW, 100);

        sl.sendParticles(ParticleTypes.FLAME,
                player.getX(), player.getY() + 1, player.getZ(), 30, 0.6, 0.6, 0.6, 0.08);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5f, 0.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§4§lCosmic Overdraw! §r§cAll abilities ×1.5 for 5s. " + orbStatus(player)));
    }

    // =========================================================================
    //  ABILITY 7 — ASTRAL CONVERGENCE  (ultimate, 10 seconds)
    // =========================================================================

    /**
     * 10-second ultimate transformation:
     *  - Orbs regenerate automatically (1 per second; final tier: 1 per 0.5s)
     *  - Orb cap +2
     *  - Abilities cost 1 fewer orb
     *  - Every orb spent chains damage to nearby enemies
     * Cost: 5000 soul essence.  Requires stage ≥ 6.
     */
    public static void astralConvergence(Player player, ServerLevel sl, Boolean dontCheck) {
        if (!canUseClassAstralArbiter(player,dontCheck)) return;
        if (SoulCore.getSoulEssence(player) < 5000) return;
        if (SoulCore.getAscensionStage(player) < 6) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 5000);

        // 10 seconds = 200 ticks
        player.getPersistentData().putInt(NBT_CONVERGENCE, 200);
        player.getPersistentData().putInt(NBT_REGEN_TIMER,
                SoulCore.getAscensionStage(player) >= 7 ? 10 : 20);

        sl.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1, player.getZ(), 40, 1.0, 1.0, 1.0, 0.06);
        sl.sendParticles(ParticleTypes.GLOW,
                player.getX(), player.getY() + 1, player.getZ(), 20, 0.8, 0.8, 0.8, 0.04);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 1.2f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§e§l★ Astral Convergence! §r§6Orbs regenerate. Cap +2. Abilities cheaper. Every orb chains. "
                            + orbStatus(player)));
    }

    private static boolean canUseClassAstralArbiter(Player player, Boolean dontCheck){
        if (dontCheck)return true;
        return SoulCore.getAspect(player).equals("Astral Arbiter");
    }


}