package net.lukario.frogerealm.shadow_slave.soul_abilities.expeditioners;

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
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Crescendo Blade
 * -----------------------------------------------
 * Theme: Perfection-based swordsman. The better you play → the stronger
 * you become. Inspired by Verso from Clair Obscur: Expedition 33.
 *
 * Core Mechanic — PERFECTION RANK (D → C → B → A → S):
 *   Perfection is a 0–100 float. Each threshold unlocks a rank.
 *   D: 0–20  | C: 20–40 | B: 40–60 | A: 60–80 | S: 80–100
 *
 *   Higher rank = higher damage multiplier on ALL attacks:
 *     D: ×1.0 | C: ×1.2 | B: ×1.5 | A: ×1.8 | S: ×2.5
 *
 * Gaining Perfection:
 *   - Each ability use: +8–15 perfection
 *   - Multi-hit abilities: +3–5 per hit
 *   - Blocking when hit (parry): reduced loss
 *
 * Losing Perfection:
 *   - Being hit: −25 (roughly one full rank)
 *   - Blocking when hit: −10 instead
 *   - Passive decay: −1 every 4 seconds
 *
 * Rank Bonuses — each ability has ONE specific rank where it becomes
 * stronger. The bonus only activates at EXACTLY that rank, not above it.
 *   Ability 1 (Quick Strike)   → Rank B only: Haste II for 3s
 *   Ability 2 (Marking Shot)   → Rank C only: target briefly rooted
 *   Ability 3 (Strike Storm)   → Rank A only: all 6 hits guaranteed crits
 *   Ability 4 (Steeled Strike) → Rank S only: ×1.5 + AOE explosion
 *   Ability 5 (Overload)       → no rank bonus (it IS the rank tool)
 *   Ability 6 (From Fire)      → Rank B only: ignites + bonus hit
 *   Ability 7 (End Bringer)    → Rank S only: AOE splash on impact
 *
 * Player NBT keys:
 *   "CrescendoPerf"       → float  perfection score (0–100)
 *   "CrescendoDecay"      → int    ticks until next decay tick
 *   "CrescendoSteelTimer" → int    ticks remaining on Steeled Strike charge
 *   "CrescendoSteelUUID"  → UUID   target of Steeled Strike
 *   "CrescendoSteelCancel"→ byte   1 = strike cancelled by damage this tick
 *   "CrescendoLocked"     → int    ticks remaining on rank lock (End Bringer)
 *
 * Entity NBT keys:
 *   "CrescendoMarked"     → UUID   UUID of player who applied mark
 */
public class Verso {

    // ─── NBT keys ─────────────────────────────────────────────────────────────
    private static final String NBT_PERF          = "CrescendoPerf";
    private static final String NBT_DECAY         = "CrescendoDecay";
    private static final String NBT_STEEL_TIMER   = "CrescendoSteelTimer";
    private static final String NBT_STEEL_UUID    = "CrescendoSteelUUID";
    private static final String NBT_STEEL_CANCEL  = "CrescendoSteelCancel";
    private static final String NBT_LOCKED        = "CrescendoLocked";

    // ─── Rank thresholds ──────────────────────────────────────────────────────
    private static final float RANK_C_MIN = 20f;
    private static final float RANK_B_MIN = 40f;
    private static final float RANK_A_MIN = 60f;
    private static final float RANK_S_MIN = 80f;
    private static final float PERF_MAX   = 100f;

    // ─── Perfection loss on hit ───────────────────────────────────────────────
    private static final float HIT_LOSS   = 25f;  // standard hit
    private static final float PARRY_LOSS = 10f;  // while blocking

    // ─── Timing ───────────────────────────────────────────────────────────────
    private static final int DECAY_INTERVAL   = 80;   // 1 perf lost every 4s
    private static final int STEEL_CHARGE     = 60;   // 3s charge time
    private static final int LOCK_DURATION    = 200;  // 10s rank lock

    private static final Random RNG = new Random();

    // =========================================================================
    //  PERFECTION HELPERS
    // =========================================================================

    public static float getPerfection(Player player) {
        return player.getPersistentData().getFloat(NBT_PERF);
    }

    public static void setPerfection(Player player, float val) {
        player.getPersistentData().putFloat(NBT_PERF,
                Math.max(0f, Math.min(PERF_MAX, val)));
    }

    private static void addPerfection(Player player, float amount) {
        setPerfection(player, getPerfection(player) + amount);
    }

    /** Returns the current rank string: D, C, B, A, or S. */
    public static String getRank(Player player) {
        float p = getPerfection(player);
        if (p >= RANK_S_MIN) return "S";
        if (p >= RANK_A_MIN) return "A";
        if (p >= RANK_B_MIN) return "B";
        if (p >= RANK_C_MIN) return "C";
        return "D";
    }

    public static boolean isRankLocked(Player player) {
        return player.getPersistentData().getInt(NBT_LOCKED) > 0;
    }

    /**
     * Damage multiplier applied universally to all player attacks.
     * This is the core payoff of maintaining high Perfection rank.
     */
    public static float rankMult(Player player) {
        return switch (getRank(player)) {
            case "S" -> 2.5f;
            case "A" -> 1.8f;
            case "B" -> 1.5f;
            case "C" -> 1.2f;
            default  -> 1.0f;
        };
    }

    private static String rankColored(Player player) {
        return switch (getRank(player)) {
            case "S" -> "§6§l[S]";
            case "A" -> "§e[A]";
            case "B" -> "§a[B]";
            case "C" -> "§b[C]";
            default  -> "§7[D]";
        };
    }

    private static String perfStatus(Player player) {
        return rankColored(player)
                + " §f" + String.format("%.0f", getPerfection(player)) + "/100"
                + " §7(×" + String.format("%.1f", rankMult(player)) + ")";
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

    // ─── Steeled Strike detonation (called from tick) ─────────────────────────

    private static void detonateSteelStrike(Player player, ServerLevel sl) {
        if (!player.getPersistentData().contains(NBT_STEEL_UUID)) return;
        UUID targetUUID = player.getPersistentData().getUUID(NBT_STEEL_UUID);
        player.getPersistentData().remove(NBT_STEEL_UUID);

        List<LivingEntity> found = sl.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(50),
                e -> e.getUUID().equals(targetUUID) && e.isAlive());

        if (found.isEmpty()) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§7Steeled Strike: target gone before detonation."));
            return;
        }

        LivingEntity target = found.get(0);
        int   stage      = SoulCore.getAscensionStage(player);
        boolean rankS    = getRank(player).equals("S");
        float dmgPerHit  = 7f + stage;   // rank mult applied by event handler
        int   hits       = 13;
        float totalShown = 0f;

        for (int i = 0; i < hits; i++) {
            target.hurt(sl.damageSources().playerAttack(player), dmgPerHit);
            target.invulnerableTime = 0;
            // Each hit counts for perfection gain
            addPerfection(player, 2f);
            totalShown += dmgPerHit * rankMult(player)
                    * (rankS ? 1.5f : 1f); // display estimate

            sl.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1, target.getZ(),
                    2, 0.25, 0.25, 0.25, 0.04);
        }

        // Rank S ONLY: ×1.5 bonus applied as an extra hit, plus AOE explosion
        if (rankS) {
            // Bonus hit (the ×1.5 is reflected as 50% extra damage in one more strike)
            float bonusDmg = dmgPerHit * 6.5f; // rough ×0.5 spread across 13 hits
            target.hurt(sl.damageSources().playerAttack(player), bonusDmg);
            target.invulnerableTime = 0;

            // AOE explosion around target
            sl.getEntitiesOfClass(LivingEntity.class,
                            target.getBoundingBox().inflate(5f),
                            e -> !e.getUUID().equals(player.getUUID())
                                    && !e.getUUID().equals(target.getUUID())
                                    && e.isAlive())
                    .forEach(e -> {
                        e.hurt(sl.damageSources().playerAttack(player), dmgPerHit * 4f);
                        e.invulnerableTime = 0;
                        sl.sendParticles(ParticleTypes.EXPLOSION,
                                e.getX(), e.getY() + 1, e.getZ(), 3, 0.3, 0.3, 0.3, 0.03);
                    });
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    target.getX(), target.getY() + 1, target.getZ(), 2, 0.4, 0.4, 0.4, 0);
        }

        sl.playSound(null, target.blockPosition(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.5f, 0.4f);
        sl.playSound(null, target.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1f, 1.2f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    rankColored(player) + " §6§lSteeled Strike DETONATED! §r§f13 hits ≈ §c"
                            + String.format("%.1f", totalShown) + " total"
                            + (rankS ? " §6[S BONUS: ×1.5 + AOE explosion!]" : "")
                            + " " + perfStatus(player)));
    }

    // =========================================================================
    //  EVENTS
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class CrescendoEvents {

        @SubscribeEvent
        public static void onCrescendoPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Crescendo Blade")) return;

            // ── Rank lock countdown ───────────────────────────────────────────
            int locked = player.getPersistentData().getInt(NBT_LOCKED);
            if (locked > 0) {
                player.getPersistentData().putInt(NBT_LOCKED, locked - 1);
                if (player.tickCount % 5 == 0)
                    sl.sendParticles(ParticleTypes.GLOW,
                            player.getX(), player.getY() + 2.2, player.getZ(),
                            2, 0.3, 0.1, 0.3, 0.01);
            }

            // ── Steeled Strike charge ─────────────────────────────────────────
            int steelTimer = player.getPersistentData().getInt(NBT_STEEL_TIMER);
            if (steelTimer > 0) {
                // Cancelled by damage?
                if (player.getPersistentData().getByte(NBT_STEEL_CANCEL) == 1) {
                    player.getPersistentData().putInt(NBT_STEEL_TIMER, 0);
                    player.getPersistentData().putByte(NBT_STEEL_CANCEL, (byte) 0);
                    player.getPersistentData().remove(NBT_STEEL_UUID);
                    addPerfection(player, -10f);
                    sl.sendParticles(ParticleTypes.SMOKE,
                            player.getX(), player.getY() + 1, player.getZ(),
                            10, 0.3, 0.3, 0.3, 0.03);
                    sl.playSound(null, player.blockPosition(),
                            SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f);
                    if (player instanceof ServerPlayer sp)
                        sp.sendSystemMessage(Component.literal(
                                "§c§lSteeled Strike CANCELLED §r§c— interrupted! §7-10 perf. "
                                        + perfStatus(player)));
                } else {
                    steelTimer--;
                    player.getPersistentData().putInt(NBT_STEEL_TIMER, steelTimer);

                    // Charging particles (pulse faster as it nears detonation)
                    int pulseInterval = Math.max(2, steelTimer / 8);
                    if (player.tickCount % pulseInterval == 0)
                        sl.sendParticles(ParticleTypes.CRIT,
                                player.getX(), player.getY() + 1, player.getZ(),
                                3, 0.35, 0.35, 0.35, 0.02);

                    if (steelTimer == 0) detonateSteelStrike(player, sl);
                }
            }

            // ── Passive perfection decay ──────────────────────────────────────
            float perf = getPerfection(player);
            if (perf > 0) {
                int decay = player.getPersistentData().getInt(NBT_DECAY) - 1;
                if (decay <= 0) {
                    setPerfection(player, perf - 1f);
                    decay = DECAY_INTERVAL;
                }
                player.getPersistentData().putInt(NBT_DECAY, decay);
            }

            // ── HUD: rank indicator particles above head ──────────────────────
            if (player.tickCount % 10 == 0 && perf > 0) {
                int rankLevel = switch (getRank(player)) {
                    case "S" -> 5; case "A" -> 4; case "B" -> 3;
                    case "C" -> 2; default -> 1;
                };
                double arcSpan = Math.toRadians(110);
                for (int i = 0; i < rankLevel; i++) {
                    double angle = -arcSpan / 2
                            + (rankLevel > 1 ? arcSpan / (rankLevel - 1) * i : 0);
                    double px = player.getX() + 0.75 * Math.sin(angle);
                    double pz = player.getZ() + 0.75 * Math.cos(angle);
                    var particle = switch (getRank(player)) {
                        case "S" -> ParticleTypes.GLOW;
                        case "A" -> ParticleTypes.CRIT;
                        default  -> ParticleTypes.PORTAL;
                    };
                    sl.sendParticles(particle, px, player.getY() + 2.2, pz, 1, 0, 0, 0, 0);
                }
            }

            // ── Rank S constant aura ──────────────────────────────────────────
            if (getRank(player).equals("S") && player.tickCount % 4 == 0) {
                sl.sendParticles(ParticleTypes.GLOW,
                        player.getX(), player.getY() + 1, player.getZ(),
                        3, 0.45, 0.45, 0.45, 0.03);
            }
        }

        /**
         * Being hit reduces Perfection.
         * Blocking reduces the penalty (parry mechanic).
         * Rank-locked players (End Bringer buff) take no Perfection loss.
         * Getting hit while Steeled Strike is charging cancels it.
         */
        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void onCrescendoDamageTaken(LivingHurtEvent event) {
            if (!(event.getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Crescendo Blade")) return;

            // Rank locked: no perfection loss
            if (isRankLocked(player)) {
                if (player.level() instanceof ServerLevel sl)
                    sl.sendParticles(ParticleTypes.GLOW,
                            player.getX(), player.getY() + 1, player.getZ(),
                            5, 0.2, 0.2, 0.2, 0.04);
                return;
            }

            // Cancel Steeled Strike if charging
            if (player.getPersistentData().getInt(NBT_STEEL_TIMER) > 0)
                player.getPersistentData().putByte(NBT_STEEL_CANCEL, (byte) 1);

            // Blocking = parry, reduced loss
            float loss     = player.isBlocking() ? PARRY_LOSS : HIT_LOSS;
            String prevRank = getRank(player);
            addPerfection(player, -loss);
            String newRank  = getRank(player);

            if (!prevRank.equals(newRank) && player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§c§lRANK DOWN §r§7" + prevRank + " → §c" + newRank
                                + " §7| -" + (int) loss + " perf. " + perfStatus(player)));
        }

        /**
         * All outgoing player damage is multiplied by the current rank multiplier.
         * Marked targets also take an additional +20%.
         */
        @SubscribeEvent(priority = EventPriority.NORMAL)
        public static void onCrescendoDamageDealt(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Crescendo Blade")) return;

            LivingEntity target = event.getEntity();
            float mult = rankMult(player);

            // Mark bonus
            if (target.getPersistentData().contains("CrescendoMarked")) {
                UUID markedBy = target.getPersistentData().getUUID("CrescendoMarked");
                if (markedBy.equals(player.getUUID())) mult *= 1.20f;
            }

            event.setAmount(event.getAmount() * mult);
        }
    }

    // =========================================================================
    //  ABILITY 1 — CRESCENDO BLADE QUICK STRIKE
    // =========================================================================

    /**
     * 3-hit rapid melee combo. Each hit generates perfection.
     * Rank B ONLY bonus: gain Haste II for 3 seconds after the combo.
     * Cost: 200 essence.
     */
    public static void crescendoBladeQuickStrike(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Crescendo Blade")) return;
        if (SoulCore.getSoulEssence(player) < 200) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 200);

        int   stage  = SoulCore.getAscensionStage(player);
        LivingEntity target = rayCastFirst(player, level, 6 + stage);
        if (target == null) return;

        float dmgPerHit = 8f + stage;
        boolean rankB   = getRank(player).equals("B"); // ONLY at B, not above

        for (int i = 0; i < 3; i++) {
            target.hurt(level.damageSources().playerAttack(player), dmgPerHit);
            target.invulnerableTime = 0;
            addPerfection(player, 5f); // +15 total
            sl.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1 + i * 0.15, target.getZ(),
                    3, 0.2, 0.2, 0.2, 0.03);
        }

        if (rankB) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 60, 1, false, true));
            sl.sendParticles(ParticleTypes.CRIT,
                    player.getX(), player.getY() + 1, player.getZ(),
                    10, 0.3, 0.3, 0.3, 0.05);
        }

        level.playSound(null, target.blockPosition(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.9f, 1.4f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    rankColored(player) + " Quick Strike: §f3 hits × §c"
                            + String.format("%.1f", dmgPerHit * rankMult(player))
                            + (rankB ? " §a[B BONUS: Haste II 3s]" : "")
                            + " " + perfStatus(player)));
    }

    // =========================================================================
    //  ABILITY 2 — CRESCENDO BLADE MARKING SHOT
    // =========================================================================

    /**
     * Mark an enemy — they take +20% damage from all your attacks permanently
     * until the mark is removed. Deals initial damage.
     * Rank C ONLY bonus: target is briefly rooted (2 seconds).
     * Cost: 300 essence.  Requires stage ≥ 1.
     */
    public static void crescendoBladeMarkingShot(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Crescendo Blade")) return;
        if (SoulCore.getSoulEssence(player) < 300) return;
        if (SoulCore.getAscensionStage(player) < 1) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 300);

        int   stage  = SoulCore.getAscensionStage(player);
        LivingEntity target = rayCastFirst(player, level, 16 + stage);
        if (target == null) return;

        addPerfection(player, 8f);
        boolean rankC = getRank(player).equals("C"); // ONLY at C

        // Apply mark
        target.getPersistentData().putUUID("CrescendoMarked", player.getUUID());
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400, 0, false, false));

        // Initial impact damage
        float dmg = 12f + stage * 2f;
        target.hurt(level.damageSources().playerAttack(player), dmg);
        target.invulnerableTime = 0;

        // Rank C ONLY: root (Slowness X = effectively rooted)
        if (rankC) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    40, 10, false, true));
            sl.sendParticles(ParticleTypes.ENCHANT,
                    target.getX(), target.getY() + 1, target.getZ(),
                    10, 0.3, 0.3, 0.3, 0.04);
        }

        sl.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + 1, target.getZ(),
                6, 0.3, 0.3, 0.3, 0.04);
        level.playSound(null, target.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 1.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    rankColored(player) + " Marking Shot: §f" + String.format("%.1f", dmg * rankMult(player))
                            + " dmg. §eTarget marked §7(+20% all dmg)"
                            + (rankC ? " §b[C BONUS: rooted 2s!]" : "")
                            + " " + perfStatus(player)));
    }

    // =========================================================================
    //  ABILITY 3 — CRESCENDO BLADE STRIKE STORM
    // =========================================================================

    /**
     * 6-hit rapid barrage. Crits generate extra perfection.
     * Rank A ONLY bonus: ALL 6 hits are guaranteed critical hits (×1.5 each).
     * Cost: 500 essence.  Requires stage ≥ 2.
     */
    public static void crescendoBladeStrikeStorm(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Crescendo Blade")) return;
        if (SoulCore.getSoulEssence(player) < 500) return;
        if (SoulCore.getAscensionStage(player) < 2) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 500);

        int   stage  = SoulCore.getAscensionStage(player);
        LivingEntity target = rayCastFirst(player, level, 10 + stage);
        if (target == null) return;

        boolean rankA     = getRank(player).equals("A"); // ONLY at A
        float   baseDmg   = 10f + stage * 1.5f;
        float   totalShown = 0f;
        int     extraPerf  = 0;

        for (int i = 0; i < 6; i++) {
            boolean isCrit = rankA || RNG.nextFloat() < 0.30f;
            float   hitDmg = baseDmg * (isCrit ? 1.5f : 1f);

            target.hurt(level.damageSources().playerAttack(player), hitDmg);
            target.invulnerableTime = 0;
            addPerfection(player, 5f);
            totalShown += hitDmg * rankMult(player);

            if (isCrit) {
                extraPerf += 4;
                sl.sendParticles(ParticleTypes.CRIT,
                        target.getX(), target.getY() + 1 + i * 0.1, target.getZ(),
                        4, 0.2, 0.2, 0.2, 0.05);
            } else {
                sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                        target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
            }
        }

        addPerfection(player, extraPerf);

        level.playSound(null, target.blockPosition(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 1.0f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    rankColored(player) + " Strike Storm: §f≈" + String.format("%.1f", totalShown)
                            + " total §7(6 hits"
                            + (rankA ? "§6 ALL CRITS)" : ", 30% crit)")
                            + (rankA ? " §e[A BONUS: guaranteed crits!]" : "")
                            + (extraPerf > 0 ? " §a+" + extraPerf + " crit perf" : "")
                            + " " + perfStatus(player)));
    }

    // =========================================================================
    //  ABILITY 4 — CRESCENDO BLADE STEELED STRIKE
    // =========================================================================

    /**
     * Charges for 3 seconds, then detonates 13 rapid hits on a locked target.
     * If YOU are hit while charging: CANCELLED. You lose 10 Perfection.
     * Rank S ONLY bonus: hits deal ×1.5, final hit triggers AOE explosion.
     * Cost: 600 essence.  Requires stage ≥ 3.
     */
    public static void crescendoBladeSteelStrike(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Crescendo Blade")) return;
        if (SoulCore.getSoulEssence(player) < 600) return;
        if (SoulCore.getAscensionStage(player) < 3) return;

        if (player.getPersistentData().getInt(NBT_STEEL_TIMER) > 0) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cSteeled Strike already charging!"));
            return;
        }

        int stage = SoulCore.getAscensionStage(player);
        LivingEntity target = rayCastFirst(player, level, 14 + stage);
        if (target == null) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 600);
        addPerfection(player, 5f);

        player.getPersistentData().putUUID(NBT_STEEL_UUID, target.getUUID());
        player.getPersistentData().putInt(NBT_STEEL_TIMER, 60);

        sl.sendParticles(ParticleTypes.CRIT,
                player.getX(), player.getY() + 1, player.getZ(),
                12, 0.4, 0.4, 0.4, 0.05);
        level.playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 0.4f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    rankColored(player) + " §6Steeled Strike: §fcharging 13-hit strike on §e"
                            + target.getName().getString() + "§f. §c§lDon't get hit! §r§7(3s) "
                            + (getRank(player).equals("S") ? "§6[S BONUS: ×1.5 + explosion on ready!]" : "")
                            + " " + perfStatus(player)));
    }

    // =========================================================================
    //  ABILITY 5 — CRESCENDO BLADE OVERLOAD
    // =========================================================================

    /**
     * Instantly boosts Perfection to Rank S (100).
     * Cost: your HP drops to 1. High risk, ultimate setup tool.
     * Essence cost: 1000.  Requires stage ≥ 4.
     */
    public static void crescendoBladeOverload(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Crescendo Blade")) return;
        if (SoulCore.getSoulEssence(player) < 1000) return;
        if (SoulCore.getAscensionStage(player) < 4) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 1000);

        String prevRank = getRank(player);
        float  prevPerf = getPerfection(player);

        // Drop HP to 1 — the price of instant power
        player.setHealth(1f);
        setPerfection(player, 100f);

        sl.sendParticles(ParticleTypes.GLOW,
                player.getX(), player.getY() + 1, player.getZ(),
                40, 0.7, 0.7, 0.7, 0.07);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.5f, 0.3f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§6§l⚡ OVERLOAD! §r§c HP → 1. §7Perf: §8"
                            + String.format("%.0f", prevPerf) + " §7(" + prevRank + ") → §6100 §6§l(S)§r. "
                            + "§fAll attacks ×2.5. §cDon't get hit! " + perfStatus(player)));
    }

    // =========================================================================
    //  ABILITY 6 — CRESCENDO BLADE FROM FIRE
    // =========================================================================

    /**
     * 3-hit strike. Heals you for each hit if the target is burning.
     * Rank B ONLY bonus: if target is NOT burning, ignites them; if already
     * burning, deals a massive bonus 4th hit scaled by remaining fire ticks.
     * Cost: 400 essence.  Requires stage ≥ 2.
     */
    public static void crescendoBladeFromFire(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Crescendo Blade")) return;
        if (SoulCore.getSoulEssence(player) < 400) return;
        if (SoulCore.getAscensionStage(player) < 2) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 400);

        int   stage  = SoulCore.getAscensionStage(player);
        LivingEntity target = rayCastFirst(player, level, 8 + stage);
        if (target == null) return;

        addPerfection(player, 10f);
        boolean rankB        = getRank(player).equals("B"); // ONLY at B
        boolean targetBurning = target.isOnFire();
        float   dmgPerHit    = 12f + stage * 1.5f;
        float   totalHeal    = 0f;

        for (int i = 0; i < 3; i++) {
            target.hurt(level.damageSources().playerAttack(player), dmgPerHit);
            target.invulnerableTime = 0;
            addPerfection(player, 3f);

            if (targetBurning) {
                float heal = 2.5f + stage * 0.5f;
                player.heal(heal);
                totalHeal += heal;
                sl.sendParticles(ParticleTypes.HEART,
                        player.getX(), player.getY() + 1 + i * 0.15, player.getZ(),
                        1, 0.1, 0.1, 0.1, 0);
            }

            sl.sendParticles(ParticleTypes.FLAME,
                    target.getX(), target.getY() + 1, target.getZ(),
                    3, 0.2, 0.2, 0.2, 0.03);
        }

        // Rank B ONLY
        if (rankB) {
            if (!targetBurning) {
                // Not burning: ignite target
                target.setRemainingFireTicks(120);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        target.getX(), target.getY() + 1, target.getZ(),
                        10, 0.4, 0.4, 0.4, 0.05);
            } else {
                // Already burning: bonus 4th hit scaled by fire ticks
                float bonusDmg = Math.min(target.getRemainingFireTicks() * 0.4f, 25f) + stage;
                target.hurt(level.damageSources().playerAttack(player), bonusDmg);
                target.invulnerableTime = 0;
                float bonusHeal = bonusDmg * 0.3f;
                player.heal(bonusHeal);
                totalHeal += bonusHeal;
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
            }
        }

        level.playSound(null, target.blockPosition(),
                SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8f, 1.2f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    rankColored(player) + " From Fire: §f3 hits × §c"
                            + String.format("%.1f", dmgPerHit * rankMult(player))
                            + (totalHeal > 0 ? " §a+" + String.format("%.1f", totalHeal) + " HP heal" : " §7(no heal)")
                            + (rankB && !targetBurning ? " §c[B BONUS: ignited!]"
                            : rankB ? " §6[B BONUS: bonus fire hit!]" : "")
                            + " " + perfStatus(player)));
    }

    // =========================================================================
    //  ABILITY 7 — CRESCENDO BLADE END BRINGER
    // =========================================================================

    /**
     * Single crushing strike. Damage = base + (perfection × 2.5).
     * Requires Rank A or S to use.
     * After firing: Rank is LOCKED for 10 seconds (cannot lose Perfection).
     * Rank S ONLY bonus: impact triggers AOE splash at 50% damage.
     * Cost: 2000 essence.  Requires stage ≥ 7.
     */
    public static void crescendoBladeEndBringer(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Crescendo Blade")) return;
        if (SoulCore.getSoulEssence(player) < 2000) return;
        if (SoulCore.getAscensionStage(player) < 7) return;

        String rank = getRank(player);
        if (!rank.equals("A") && !rank.equals("S")) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§cEnd Bringer requires §eRank A or S§c! Currently: "
                                + rankColored(player)));
            return;
        }

        int stage = SoulCore.getAscensionStage(player);
        LivingEntity target = rayCastFirst(player, level, 18 + stage);
        if (target == null) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 2000);
        addPerfection(player, 10f);

        float perf    = getPerfection(player);
        float damage  = 30f + stage * 5f + perf * 2.5f;
        boolean rankS = rank.equals("S");

        target.hurt(level.damageSources().playerAttack(player), damage);
        target.invulnerableTime = 0;

        // Rank S ONLY: AOE splash at 50% damage
        if (rankS) {
            float splashDmg = damage * 0.50f;
            float splashR   = 6f + stage * 0.5f;
            level.getEntitiesOfClass(LivingEntity.class,
                            target.getBoundingBox().inflate(splashR),
                            e -> !e.getUUID().equals(player.getUUID())
                                    && !e.getUUID().equals(target.getUUID()) && e.isAlive())
                    .forEach(e -> {
                        e.hurt(level.damageSources().playerAttack(player), splashDmg);
                        e.invulnerableTime = 0;
                        sl.sendParticles(ParticleTypes.EXPLOSION,
                                e.getX(), e.getY() + 1, e.getZ(), 4, 0.3, 0.3, 0.3, 0.03);
                    });
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    target.getX(), target.getY() + 1, target.getZ(), 3, 0.4, 0.4, 0.4, 0);
        }

        // Lock rank (can't lose Perfection for 10 seconds)
        player.getPersistentData().putInt(NBT_LOCKED, LOCK_DURATION);

        // Grand visual
        for (int i = 0; i < 20; i++) {
            double angle = Math.toRadians(i * 18);
            double r = 3.0;
            sl.sendParticles(ParticleTypes.GLOW,
                    target.getX() + r * Math.cos(angle), target.getY() + 1,
                    target.getZ() + r * Math.sin(angle), 1, 0, 0.2, 0, 0.04);
        }
        sl.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + 1, target.getZ(),
                20, 0.5, 0.5, 0.5, 0.06);
        sl.playSound(null, target.blockPosition(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.5f, 0.3f);
        sl.playSound(null, target.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2f, 0.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    rankColored(player) + " §6§l✦ END BRINGER! §r§f≈"
                            + String.format("%.1f", damage * rankMult(player)) + " dmg"
                            + (rankS ? " §6[S BONUS: +" + String.format("%.1f", damage * rankMult(player) * 0.5f) + " AOE!]" : "")
                            + " §bRank locked 10s. " + perfStatus(player)));
    }
}

