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
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

/**
 * Eclipse Phantasm
 * -----------------------------------------------
 * Phase-based DPS. You are either untouchable OR lethal — never both.
 *
 * NBT keys on PLAYER:
 *   "EclipsePhase"          → String  "VOID" | "MATERIAL"
 *   "EclipseShiftCD"        → int     ticks until next Phase Shift allowed
 *   "EclipseMaterialBonus"  → int     ticks of +50% damage after phasing in
 *   "EclipseFirstHit"       → byte    1 = first hit bonus still available
 *   "EclipseEchoTimer"      → int     ticks until Echo Cut triggers
 *   "EclipseEchoTarget"     → UUID    target of pending echo
 *   "EclipseEchoDamage"     → float   damage of pending echo hit
 *   "EclipseEchoBonus"      → byte    1 = echo bonus (phased out before trigger)
 *   "EclipseLockTarget"     → UUID    target that is Phase Locked
 *   "EclipseLockTimer"      → int     ticks remaining on Phase Lock
 *   "EclipseLockAmp"        → byte    1 = amplified (player phased out during lock)
 *   "EclipseStateTimer"     → int     ticks remaining on Eclipse State
 *   "EclipseStateFlicker"   → int     internal flicker counter
 *   "EclipseFractureCD"     → int     ticks remaining in Reality Fracture bonus window
 */
public class EclipsePhantasm {

    // ─── Constants ────────────────────────────────────────────────────────────
    public static final String PHASE_VOID     = "VOID";
    public static final String PHASE_MATERIAL = "MATERIAL";

    private static final String NBT_PHASE         = "EclipsePhase";
    private static final String NBT_SHIFT_CD      = "EclipseShiftCD";
    private static final String NBT_MAT_BONUS     = "EclipseMaterialBonus";
    private static final String NBT_FIRST_HIT     = "EclipseFirstHit";
    private static final String NBT_ECHO_TIMER    = "EclipseEchoTimer";
    private static final String NBT_ECHO_TARGET   = "EclipseEchoTarget";
    private static final String NBT_ECHO_DAMAGE   = "EclipseEchoDamage";
    private static final String NBT_ECHO_BONUS    = "EclipseEchoBonus";
    private static final String NBT_LOCK_TARGET   = "EclipseLockTarget";
    private static final String NBT_LOCK_TIMER    = "EclipseLockTimer";
    private static final String NBT_LOCK_AMP      = "EclipseLockAmp";
    private static final String NBT_STATE_TIMER   = "EclipseStateTimer";
    private static final String NBT_STATE_FLICKER = "EclipseStateFlicker";
    private static final String NBT_FRACTURE_CD   = "EclipseFractureCD";

    private static final int SHIFT_COOLDOWN  = 15;
    private static final int MAT_BONUS_TICKS = 40;
    private static final int ECHO_DELAY      = 20;
    private static final int ECHO_DELAY_FAST = 10;
    private static final int FRACTURE_WINDOW = 20;
    private static final int LOCK_DURATION   = 100;

    // ─── Phase Helpers ────────────────────────────────────────────────────────

    public static String getPhase(Player player) {
        String s = player.getPersistentData().getString(NBT_PHASE);
        return s.isEmpty() ? PHASE_MATERIAL : s;
    }

    public static boolean inVoidPhase(Player player) {
        return getPhase(player).equals(PHASE_VOID);
    }

    public static boolean inMaterialPhase(Player player) {
        return getPhase(player).equals(PHASE_MATERIAL);
    }

    public static boolean inEclipseState(Player player) {
        return player.getPersistentData().getInt(NBT_STATE_TIMER) > 0;
    }

    private static boolean hasMaterialBonus(Player player) {
        return player.getPersistentData().getInt(NBT_MAT_BONUS) > 0;
    }

    private static float phaseLockMultiplier(Player player, LivingEntity target) {
        if (!player.getPersistentData().contains(NBT_LOCK_TARGET)) return 1.0f;
        UUID locked = player.getPersistentData().getUUID(NBT_LOCK_TARGET);
        if (!locked.equals(target.getUUID())) return 1.0f;
        if (player.getPersistentData().getInt(NBT_LOCK_TIMER) <= 0) return 1.0f;
        boolean amp = player.getPersistentData().getByte(NBT_LOCK_AMP) == 1;
        return amp ? 1.6f : 1.3f;
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

    // ─── Internal phase transition helpers ────────────────────────────────────

    private static void enterVoidPhase(Player player, ServerLevel sl, boolean silent) {
        player.getPersistentData().putString(NBT_PHASE, PHASE_VOID);
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

        // Amplify echo and lock if pending
        if (player.getPersistentData().getInt(NBT_ECHO_TIMER) > 0)
            player.getPersistentData().putByte(NBT_ECHO_BONUS, (byte) 1);
        if (player.getPersistentData().getInt(NBT_LOCK_TIMER) > 0)
            player.getPersistentData().putByte(NBT_LOCK_AMP, (byte) 1);

        if (!silent) {
            sl.sendParticles(ParticleTypes.PORTAL,
                    player.getX(), player.getY() + 1, player.getZ(), 16, 0.5, 0.5, 0.5, 0.05);
            sl.playSound(null, player.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.5f);
        }
    }

    private static void enterMaterialPhase(Player player, ServerLevel sl, boolean silent) {
        player.getPersistentData().putString(NBT_PHASE, PHASE_MATERIAL);
        player.setInvulnerable(false);
        player.getPersistentData().putInt(NBT_MAT_BONUS, MAT_BONUS_TICKS);
        player.getPersistentData().putByte(NBT_FIRST_HIT, (byte) 1);
        player.getPersistentData().putInt(NBT_FRACTURE_CD, FRACTURE_WINDOW);

        if (!silent) {
            sl.sendParticles(ParticleTypes.END_ROD,
                    player.getX(), player.getY() + 1, player.getZ(), 16, 0.5, 0.5, 0.5, 0.05);
            sl.playSound(null, player.blockPosition(),
                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 0.8f);
        }
    }

    // =========================================================================
    //  ALL EVENTS — single consolidated class with unique name
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class EclipseEvents {

        // ── Tick: all timers, phase enforcement, echo, lock ───────────────────
        @SubscribeEvent
        public static void onEclipsePlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Eclipse Phantasm")) return;

            // ── Shift cooldown ────────────────────────────────────────────────
            int shiftCd = player.getPersistentData().getInt(NBT_SHIFT_CD);
            if (shiftCd > 0) player.getPersistentData().putInt(NBT_SHIFT_CD, shiftCd - 1);

            // ── Material bonus countdown ──────────────────────────────────────
            int matBonus = player.getPersistentData().getInt(NBT_MAT_BONUS);
            if (matBonus > 0) player.getPersistentData().putInt(NBT_MAT_BONUS, matBonus - 1);

            // ── Reality Fracture window countdown ─────────────────────────────
            int fractureCD = player.getPersistentData().getInt(NBT_FRACTURE_CD);
            if (fractureCD > 0) player.getPersistentData().putInt(NBT_FRACTURE_CD, fractureCD - 1);

            // ── Phase Lock countdown + slow ───────────────────────────────────
            int lockTimer = player.getPersistentData().getInt(NBT_LOCK_TIMER);
            if (lockTimer > 0) {
                player.getPersistentData().putInt(NBT_LOCK_TIMER, lockTimer - 1);
                if (player.getPersistentData().contains(NBT_LOCK_TARGET)) {
                    UUID lockedUUID = player.getPersistentData().getUUID(NBT_LOCK_TARGET);
                    sl.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(20),
                                    e -> e.getUUID().equals(lockedUUID) && e.isAlive())
                            .forEach(e -> e.addEffect(new MobEffectInstance(
                                    MobEffects.MOVEMENT_SLOWDOWN, 10, 1, true, false)));
                }
                if (lockTimer == 1) {
                    player.getPersistentData().remove(NBT_LOCK_TARGET);
                    player.getPersistentData().remove(NBT_LOCK_AMP);
                }
            }

            // ── Eclipse State countdown + auto-flicker ────────────────────────
            int stateTimer = player.getPersistentData().getInt(NBT_STATE_TIMER);
            if (stateTimer > 0) {
                player.getPersistentData().putInt(NBT_STATE_TIMER, stateTimer - 1);
                int flicker = player.getPersistentData().getInt(NBT_STATE_FLICKER);
                flicker--;
                if (flicker <= 0) {
                    if (inMaterialPhase(player)) enterVoidPhase(player, sl, true);
                    else                         enterMaterialPhase(player, sl, true);
                    flicker = 15;
                }
                player.getPersistentData().putInt(NBT_STATE_FLICKER, flicker);
            }

            // ── Echo Cut countdown ────────────────────────────────────────────
            int echoTimer = player.getPersistentData().getInt(NBT_ECHO_TIMER);
            if (echoTimer > 0) {
                echoTimer--;
                player.getPersistentData().putInt(NBT_ECHO_TIMER, echoTimer);
                if (echoTimer == 0 && player.getPersistentData().contains(NBT_ECHO_TARGET)) {
                    UUID targetUUID   = player.getPersistentData().getUUID(NBT_ECHO_TARGET);
                    float echoDamage  = player.getPersistentData().getFloat(NBT_ECHO_DAMAGE);
                    boolean echoBonus = player.getPersistentData().getByte(NBT_ECHO_BONUS) == 1;
                    if (echoBonus) echoDamage *= 1.8f;
                    final float finalDmg = echoDamage;
                    sl.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(30),
                                    e -> e.getUUID().equals(targetUUID) && e.isAlive())
                            .forEach(e -> {
                                e.hurt(sl.damageSources().playerAttack(player),
                                        finalDmg * phaseLockMultiplier(player, e));
                                e.invulnerableTime = 0;
                                sl.sendParticles(ParticleTypes.PORTAL,
                                        e.getX(), e.getY() + 1, e.getZ(), 12, 0.4, 0.4, 0.4, 0.05);
                            });
                    player.getPersistentData().remove(NBT_ECHO_TARGET);
                    player.getPersistentData().remove(NBT_ECHO_DAMAGE);
                    player.getPersistentData().remove(NBT_ECHO_BONUS);
                }
            }

            // ── Void Phase: invulnerability + speed ───────────────────────────
            if (inVoidPhase(player)) {
                player.setInvulnerable(true);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3, 1, true, false));
                if (player.tickCount % 4 == 0)
                    sl.sendParticles(ParticleTypes.PORTAL,
                            player.getX(), player.getY() + 1, player.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
            } else {
                if (player.isInvulnerable()) player.setInvulnerable(false);
                if (player.tickCount % 4 == 0 && hasMaterialBonus(player))
                    sl.sendParticles(ParticleTypes.END_ROD,
                            player.getX(), player.getY() + 1, player.getZ(), 2, 0.2, 0.2, 0.2, 0.02);
            }
        }

        // ── Block incoming damage in Void Phase ───────────────────────────────
        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onEclipseDamageTaken(LivingHurtEvent event) {
            if (!(event.getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Eclipse Phantasm")) return;
            if (!inVoidPhase(player)) return;
            event.setCanceled(true);
        }

        // ── Block/reduce outgoing damage in Void Phase ────────────────────────
        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void onEclipseDamageDealt(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Eclipse Phantasm")) return;
            if (!inVoidPhase(player)) return;
            if (inEclipseState(player)) {
                event.setAmount(event.getAmount() * 0.5f);
                return;
            }
            event.setCanceled(true);
        }

        // ── Apply material bonus to outgoing damage ───────────────────────────
        @SubscribeEvent(priority = EventPriority.NORMAL)
        public static void onEclipseMaterialBonus(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Eclipse Phantasm")) return;
            if (!inMaterialPhase(player)) return;

            LivingEntity target = event.getEntity();

            if (player.getPersistentData().getByte(NBT_FIRST_HIT) == 1) {
                event.setAmount(event.getAmount() * 1.5f);
                player.getPersistentData().putByte(NBT_FIRST_HIT, (byte) 0);
            }
            if (hasMaterialBonus(player))  event.setAmount(event.getAmount() * 1.5f);
            if (inEclipseState(player))    event.setAmount(event.getAmount() * 2.0f);
            event.setAmount(event.getAmount() * phaseLockMultiplier(player, target));
        }
    }

    // =========================================================================
    //  ABILITY 1 — PHASE SHIFT
    // =========================================================================

    public static void phaseShift(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Eclipse Phantasm")) return;
        if (SoulCore.getSoulEssence(player) < 300) return;

        int shiftCd = player.getPersistentData().getInt(NBT_SHIFT_CD);
        if (shiftCd > 0) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§8Phase Shift on cooldown! §7(" + (int) Math.ceil(shiftCd / 20.0) + "s)"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 300);
        player.getPersistentData().putInt(NBT_SHIFT_CD, SHIFT_COOLDOWN);

        if (inMaterialPhase(player)) {
            enterVoidPhase(player, sl, false);
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§8◉ Void Phase — untouchable."));
        } else {
            enterMaterialPhase(player, sl, false);
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§f◎ Material Phase — §e+50% damage for 2s!"));
        }
    }

    // =========================================================================
    //  ABILITY 2 — PHANTOM STRIKE
    // =========================================================================

    public static void phantomStrike(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Eclipse Phantasm")) return;
        if (SoulCore.getSoulEssence(player) < 600) return;
        if (SoulCore.getAscensionStage(player) < 1) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 600);

        int   stage     = SoulCore.getAscensionStage(player);
        float damage    = 22.0f;
        boolean voidP   = inVoidPhase(player);
        boolean matB    = hasMaterialBonus(player);

        if (voidP && !inEclipseState(player)) damage *= 0.5f;
        if (matB)                             damage *= 2.0f;
        if (inEclipseState(player))           damage *= 2.0f;

        Vec3 dir = player.getLookAngle().normalize();
        player.setDeltaMovement(dir.scale(2.5));
        player.hurtMarked = true;

        for (int i = 0; i < 6; i++) {
            Vec3 trail = player.position().subtract(dir.scale(i * 0.3));
            sl.sendParticles(ParticleTypes.PORTAL, trail.x, trail.y + 1, trail.z, 2, 0.1, 0.1, 0.1, 0.02);
        }

        LivingEntity target = rayCastFirst(player, level, 8 + stage);
        if (target != null) {
            target.hurt(level.damageSources().playerAttack(player),
                    damage * phaseLockMultiplier(player, target));
            target.invulnerableTime = 0;
            sl.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1, target.getZ(), 8, 0.3, 0.3, 0.3, 0.04);
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.2f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§dPhantom Strike: §f" + String.format("%.1f", damage) + " dmg"
                            + (matB ? " §e[×2 burst!]" : "")
                            + (voidP && !inEclipseState(player) ? " §8[×0.5 void]" : "")));
    }

    // =========================================================================
    //  ABILITY 3 — ECHO CUT
    // =========================================================================

    public static void echoCut(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Eclipse Phantasm")) return;
        if (SoulCore.getSoulEssence(player) < 500) return;
        if (SoulCore.getAscensionStage(player) < 2) return;
        if (inVoidPhase(player) && !inEclipseState(player)) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§8Cannot use Echo Cut in Void Phase!"));
            return;
        }
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 500);

        int   stage  = SoulCore.getAscensionStage(player);
        float damage = 16.0f + stage;
        if (hasMaterialBonus(player)) damage *= 1.3f;
        if (inEclipseState(player))   damage *= 1.5f;

        LivingEntity target = rayCastFirst(player, level, 12 + stage);
        if (target == null) return;

        target.hurt(level.damageSources().playerAttack(player),
                damage * phaseLockMultiplier(player, target));
        target.invulnerableTime = 0;
        sl.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + 1, target.getZ(), 6, 0.2, 0.2, 0.2, 0.03);

        int echoDelay = inEclipseState(player) ? ECHO_DELAY_FAST : ECHO_DELAY;
        player.getPersistentData().putInt(NBT_ECHO_TIMER, echoDelay);
        player.getPersistentData().putUUID(NBT_ECHO_TARGET, target.getUUID());
        player.getPersistentData().putFloat(NBT_ECHO_DAMAGE, damage);
        player.getPersistentData().putByte(NBT_ECHO_BONUS, (byte) 0);

        level.playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.3f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§dEcho Cut: §f" + String.format("%.1f", damage)
                            + " dmg + echo in §b" + (echoDelay / 20.0) + "s"
                            + " §d(phase out for ×1.8!)"));
    }

    // =========================================================================
    //  ABILITY 4 — VOID DRIFT
    // =========================================================================

    public static void voidDrift(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Eclipse Phantasm")) return;
        if (SoulCore.getSoulEssence(player) < 400) return;
        if (SoulCore.getAscensionStage(player) < 3) return;
        if (!inVoidPhase(player)) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§8Void Drift requires Void Phase!"));
            return;
        }
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 400);

        int  stage = SoulCore.getAscensionStage(player);
        Vec3 dir   = player.getLookAngle().normalize();

        player.setDeltaMovement(dir.scale(4.5));
        player.hurtMarked = true;

        Vec3 pos = player.position();
        for (int i = 0; i < 12; i++) {
            Vec3 trail = pos.add(dir.scale(i * 0.4));
            sl.sendParticles(ParticleTypes.PORTAL, trail.x, trail.y + 1, trail.z, 1, 0.1, 0.1, 0.1, 0);
        }

        Vec3 destination = player.position().add(dir.scale(5));
        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class, new AABB(destination, destination).inflate(2.5),
                e -> e != player && e.isAlive());

        if (!nearby.isEmpty()) {
            float impact = (18.0f + stage * 2) * (inEclipseState(player) ? 2.0f : 1.0f);
            for (LivingEntity e : nearby) {
                e.hurt(level.damageSources().playerAttack(player),
                        impact * phaseLockMultiplier(player, e));
                e.invulnerableTime = 0;
                sl.sendParticles(ParticleTypes.EXPLOSION,
                        e.getX(), e.getY() + 1, e.getZ(), 4, 0.2, 0.2, 0.2, 0.02);
            }
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§8Void Drift: §dreappeared inside enemy! §f" + (int) impact + " impact dmg"));
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.8f);
    }

    // =========================================================================
    //  ABILITY 5 — REALITY FRACTURE
    // =========================================================================

    public static void realityFracture(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Eclipse Phantasm")) return;
        if (SoulCore.getSoulEssence(player) < 1200) return;
        if (SoulCore.getAscensionStage(player) < 4) return;
        if (inVoidPhase(player) && !inEclipseState(player)) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§8Cannot use Reality Fracture in Void Phase!"));
            return;
        }
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 1200);

        int     stage    = SoulCore.getAscensionStage(player);
        boolean inWindow = player.getPersistentData().getInt(NBT_FRACTURE_CD) > 0;
        float   damage   = 35.0f + stage * 3;

        if (inWindow)              damage *= 2.5f;
        if (hasMaterialBonus(player)) damage *= 1.3f;
        if (inEclipseState(player))   damage *= 2.0f;

        LivingEntity target = rayCastFirst(player, level, 14 + stage);
        if (target == null) return;

        target.hurt(level.damageSources().playerAttack(player),
                damage * phaseLockMultiplier(player, target));
        target.invulnerableTime = 0;

        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.END_ROD,
                target.getX(), target.getY() + 1, target.getZ(), 20, 0.5, 0.5, 0.5, 0.06);
        level.playSound(null, target.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1f, 1.4f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§dReality Fracture: §f" + String.format("%.1f", damage) + " dmg"
                            + (inWindow ? " §e§l[PERFECT TIMING ×2.5!]" : " §7(phase in first for ×2.5)")));
    }

    // =========================================================================
    //  ABILITY 6 — PHASE LOCK
    // =========================================================================

    public static void phaseLock(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Eclipse Phantasm")) return;
        if (SoulCore.getSoulEssence(player) < 800) return;
        if (SoulCore.getAscensionStage(player) < 5) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 800);

        int stage = SoulCore.getAscensionStage(player);
        LivingEntity target = rayCastFirst(player, level, 14 + stage);
        if (target == null) return;

        player.getPersistentData().putUUID(NBT_LOCK_TARGET, target.getUUID());
        player.getPersistentData().putInt(NBT_LOCK_TIMER, LOCK_DURATION);
        player.getPersistentData().putByte(NBT_LOCK_AMP, (byte) 0);

        sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                target.getX(), target.getY() + 1, target.getZ(), 16, 0.4, 0.4, 0.4, 0.03);
        level.playSound(null, target.blockPosition(),
                SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1f, 0.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§dPhase Lock: §f" + target.getName().getString()
                            + " §7trapped! ×1.3 dmg. §dPhase out for ×1.6!"));
    }

    // =========================================================================
    //  ABILITY 7 — ECLIPSE STATE
    // =========================================================================

    public static void eclipseState(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Eclipse Phantasm")) return;
        if (SoulCore.getSoulEssence(player) < 6000) return;
        if (SoulCore.getAscensionStage(player) < 7) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 6000);

        player.getPersistentData().putInt(NBT_STATE_TIMER, 200);
        player.getPersistentData().putInt(NBT_STATE_FLICKER, 15);

        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(), 2, 0.3, 0.3, 0.3, 0);
        sl.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1, player.getZ(), 30, 1.0, 1.0, 1.0, 0.07);
        sl.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1, player.getZ(), 30, 1.0, 1.0, 1.0, 0.07);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5f, 1.2f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5§l☽ ECLIPSE STATE ☾ §r§dReality fractures. You are both void and vengeance."));
    }
}
