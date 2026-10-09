package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * =====================================================================
 *  VOID WALKER — Full 7-Ability Implementation
 * =====================================================================
 *
 *  Role    : Single-target DPS / Assassin
 *  Theme   : Void assassin bending space
 *  Strength: Insane burst, boss deletion, high combo damage
 *  Weakness: Low HP, no AOE, skill-dependent survival
 *
 *  ── Core Mechanic: VOID MARK ─────────────────────────────────────
 *  Stored on the TARGET entity as NBT.
 *    "VoidMarkStacks"   — int  : 0-5 stacks on the target
 *    "VoidMarkTimer"    — int  : ticks remaining before stacks expire
 *    "VoidFractured"    — int  : ticks of the Fractured debuff (bonus dmg taken)
 *    "VoidAnchorId"     — int  : entity ID of the player's Anchor target (on PLAYER)
 *
 *  ── Player NBT keys ──────────────────────────────────────────────
 *    "PhaseShiftActive"      — int  : ticks of intangibility remaining
 *    "PhaseShiftTarget"      — int  : entity ID to teleport behind on Phase Shift end
 *    "VoidOverdriveActive"   — int  : ticks of Void Overdrive remaining
 *    "VoidOverdriveDmgTaken" — flag : used by damage event to apply +25% penalty
 *    "CollapseStrikeCD"      — int  : cooldown ticks
 *    "Ability1CD"            — int  : Void Pierce cooldown
 *    "Ability2CD"            — int  : Phase Shift cooldown
 *    "Ability3CD"            — int  : Void Anchor cooldown
 *    "Ability5CD"            — int  : Rift Step cooldown (charges tracked separately)
 *    "Ability5Charges"       — int  : Rift Step charges (0-2 at late upgrade)
 *    "Ability6CD"            — int  : Void Consumption cooldown
 *    "Ability7CD"            — int  : Void Overdrive cooldown
 *    "RiftStepInvisCD"       — int  : brief invisibility after Rift Step
 *
 *  ── Soul Essence costs ───────────────────────────────────────────
 *    Ability 1 — Void Pierce        : 150
 *    Ability 2 — Phase Shift        : 250
 *    Ability 3 — Void Anchor        : 200
 *    Ability 4 — Collapse Strike    : 300
 *    Ability 5 — Rift Step          : 250
 *    Ability 6 — Void Consumption   : 350
 *    Ability 7 — Void Overdrive     : 1000
 *
 *  ── Cooldowns (ticks) ────────────────────────────────────────────
 *    Ability 1:  70   (~3.5 s)
 *    Ability 2:  200  (10 s)
 *    Ability 3:  400  (20 s)
 *    Ability 4:  varies (see Collapse Strike)
 *    Ability 5:  180  (9 s per charge)
 *    Ability 6:  300  (15 s)
 *    Ability 7:  2400 (2 min)
 * =====================================================================
 */
public class VoidWalker {

    // ─────────────────────────────────────────────────────────────────
    //  CONSTANTS
    // ─────────────────────────────────────────────────────────────────

    private static final int   MAX_VOID_MARKS            = 5;
    private static final int   VOID_MARK_DURATION        = 200;
    private static final float VOID_MARK_DMG_BONUS       = 0.08f;
    private static final float RUPTURE_DMG               = 28.0f;
    private static final float COLLAPSE_DMG_PER_STACK    = 6.0f;

    // ─────────────────────────────────────────────────────────────────
    //  EVENT BUS — unique class name + modid to avoid Forge scan collision
    // ─────────────────────────────────────────────────────────────────

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class VoidWalkerEvents {

        @SubscribeEvent
        public static void onVoidWalkerPlayerAttack(AttackEntityEvent event) {
            if (!(event.getEntity() instanceof Player player)) return;
            if (!(event.getTarget() instanceof LivingEntity target)) return;
            if (!SoulCore.getAspect(player).equals("Void Walker")) return;
            if (player.level().isClientSide) return;

            VoidWalker.applyVoidMarks(target, player, 1);
        }

        @SubscribeEvent
        public static void onVoidWalkerLivingTick(LivingEvent.LivingTickEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity.level().isClientSide) return;
            if (!(entity.level() instanceof ServerLevel level)) return;

            tickVoidMarks(entity, level);
            tickFractured(entity);

            if (entity instanceof Player player) {
                if (!SoulCore.getAspect(player).equals("Void Walker")) return;
                tickCooldowns(player);
                tickPhaseShift(player, level);
                tickVoidOverdrive(player, level);
                tickRiftStepInvis(player);
                tickVoidAnchorPull(player, level);
                tickRiftStepCharges(player);
            }
        }

        @SubscribeEvent
        public static void onVoidWalkerLivingDeath(LivingDeathEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (player.level().isClientSide) return;
            if (!SoulCore.getAspect(player).equals("Void Walker")) return;
            onKillDuringOverdrive(player);
        }

        @SubscribeEvent
        public static void onVoidWalkerLivingDamage(LivingDamageEvent event) {
            LivingEntity entity = event.getEntity();
            if (entity.level().isClientSide) return;

            CompoundTag nbt = entity.getPersistentData();

            // ── Phase Shift: negate all incoming damage to the player ──
            if (entity instanceof Player player) {
                if (player.getPersistentData().getInt("PhaseShiftActive") > 0) {
                    event.setCanceled(true);
                    return;
                }
                // Void Overdrive penalty: player takes +25% damage
                if (player.getPersistentData().getInt("VoidOverdriveActive") > 0) {
                    event.setAmount(event.getAmount() * 1.25f);
                }
            }

            // ── Void Mark amplification on targets ────────────────────
            int stacks = nbt.getInt("VoidMarkStacks");
            if (stacks > 0) {
                float bonus = 1.0f + (stacks * VOID_MARK_DMG_BONUS);
                event.setAmount(event.getAmount() * bonus);
            }

            // ── Fractured debuff: +20% bonus damage taken ─────────────
            if (nbt.getInt("VoidFractured") > 0) {
                event.setAmount(event.getAmount() * 1.20f);
            }
        }

        // ── Void Mark timer decay ─────────────────────────────────────
        private static void tickVoidMarks(LivingEntity entity, ServerLevel level) {
            CompoundTag nbt = entity.getPersistentData();
            if (!nbt.contains("VoidMarkTimer")) return;

            int timer = nbt.getInt("VoidMarkTimer");
            if (timer > 0) {
                nbt.putInt("VoidMarkTimer", timer - 1);
                int stacks = nbt.getInt("VoidMarkStacks");
                if (stacks > 0 && timer % 15 == 0) {
                    level.sendParticles(ParticleTypes.PORTAL,
                            entity.getX(), entity.getY() + 1.0, entity.getZ(),
                            stacks, 0.3, 0.3, 0.3, 0.04);
                }
            } else {
                nbt.remove("VoidMarkTimer");
                nbt.remove("VoidMarkStacks");
            }
        }

        // ── Fractured debuff decay ────────────────────────────────────
        private static void tickFractured(LivingEntity entity) {
            CompoundTag nbt = entity.getPersistentData();
            if (!nbt.contains("VoidFractured")) return;
            int t = nbt.getInt("VoidFractured");
            if (t > 0) nbt.putInt("VoidFractured", t - 1);
            else nbt.remove("VoidFractured");
        }

        // ── Cooldown tick-down ────────────────────────────────────────
        private static void tickCooldowns(Player player) {
            CompoundTag nbt = player.getPersistentData();
            for (String key : List.of(
                    "Ability1CD", "Ability2CD", "Ability3CD", "CollapseStrikeCD",
                    "Ability5CD", "Ability6CD", "Ability7CD")) {
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

        // ── Phase Shift tick ─────────────────────────────────────────
        private static void tickPhaseShift(Player player, ServerLevel level) {
            CompoundTag nbt = player.getPersistentData();
            int ticks = nbt.getInt("PhaseShiftActive");
            if (ticks <= 0) return;

            ticks--;
            nbt.putInt("PhaseShiftActive", ticks);

            level.sendParticles(ParticleTypes.PORTAL,
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    6, 0.4, 0.6, 0.4, 0.05);

            if (ticks == 0) {
                nbt.remove("PhaseShiftActive");
                int ascension = SoulCore.getAscensionStage(player);
                if (ascension >= 5 && nbt.contains("PhaseShiftTarget")) {
                    int targetId = nbt.getInt("PhaseShiftTarget");
                    nbt.remove("PhaseShiftTarget");
                    LivingEntity anchor = findEntityById(level, targetId);
                    if (anchor != null) {
                        teleportBehind(player, anchor, level);
                        player.addEffect(new MobEffectInstance(
                                MobEffects.MOVEMENT_SPEED, 60, 1, false, false));
                    }
                }
            }
        }

        // ── Void Overdrive tick ───────────────────────────────────────
        private static void tickVoidOverdrive(Player player, ServerLevel level) {
            CompoundTag nbt = player.getPersistentData();
            int ticks = nbt.getInt("VoidOverdriveActive");
            if (ticks <= 0) return;

            ticks--;
            nbt.putInt("VoidOverdriveActive", ticks);

            if (ticks % 3 == 0) {
                level.sendParticles(ParticleTypes.PORTAL,
                        player.getX(), player.getY() + 0.1, player.getZ(),
                        4, 0.2, 0.1, 0.2, 0.02);
            }

            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,      30, 2, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 1, false, false));

            if (SoulCore.getAscensionStage(player) >= 7 && ticks % 20 == 0) {
                player.heal(1.5f);
            }

            if (ticks == 0) {
                nbt.remove("VoidOverdriveActive");
                level.sendParticles(ParticleTypes.SMOKE,
                        player.getX(), player.getY() + 1, player.getZ(),
                        10, 0.5, 0.5, 0.5, 0.02);
            }
        }

        // ── Rift Step invisibility tick ───────────────────────────────
        private static void tickRiftStepInvis(Player player) {
            CompoundTag nbt = player.getPersistentData();
            int t = nbt.getInt("RiftStepInvisCD");
            if (t <= 0) return;
            t--;
            nbt.putInt("RiftStepInvisCD", t);
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  CORE MECHANIC — VOID MARK
    // ─────────────────────────────────────────────────────────────────

    public static void applyVoidMarks(LivingEntity target, Player source, int amount) {
        if (target.level().isClientSide) return;
        if (!(target.level() instanceof ServerLevel level)) return;

        CompoundTag nbt = target.getPersistentData();
        int stacks      = nbt.getInt("VoidMarkStacks") + amount;

        if (stacks >= MAX_VOID_MARKS) {
            nbt.putInt("VoidMarkStacks", 0);
            nbt.remove("VoidMarkTimer");

            int ascension = SoulCore.getAscensionStage(source);
            float ruptDmg = RUPTURE_DMG + ascension * 2.0f;
            target.hurt(level.damageSources().playerAttack(source), ruptDmg);

            level.sendParticles(ParticleTypes.EXPLOSION,
                    target.getX(), target.getY() + 1, target.getZ(),
                    3, 0.4, 0.4, 0.4, 0.1);
            level.sendParticles(ParticleTypes.PORTAL,
                    target.getX(), target.getY() + 1, target.getZ(),
                    20, 0.6, 0.8, 0.6, 0.15);
            level.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.7f, 1.6f);

            if (source.getPersistentData().getInt("VoidOverdriveActive") > 0) {
                source.getPersistentData().remove("CollapseStrikeCD");
            }
        } else {
            nbt.putInt("VoidMarkStacks", stacks);
            nbt.putInt("VoidMarkTimer",  VOID_MARK_DURATION);

            level.sendParticles(ParticleTypes.PORTAL,
                    target.getX(), target.getY() + 1.2, target.getZ(),
                    stacks * 2, 0.25, 0.25, 0.25, 0.03);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 1 — VOID PIERCE
    // ─────────────────────────────────────────────────────────────────

    public static void voidWalkerAbilityOneUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
//        player.sendSystemMessage(Component.literal(String.valueOf(player.getPersistentData().getInt("Ability1CD"))));
        if (!validateUse(player, "Ability1CD", 70, 150)) return;

        int ascension       = SoulCore.getAscensionStage(player);
        LivingEntity target = findClosestInSight(player, level, 16.0);
        if (target == null) return;

        Vec3 dir     = target.position().subtract(player.position()).normalize();
        Vec3 dashPos = target.position().add(dir.scale(1.5));
        player.teleportTo(dashPos.x, dashPos.y, dashPos.z);

        if (ascension >= 6) player.invulnerableTime = 10;

        float dmg = 6.0f + ascension * 0.5f;
        target.hurt(level.damageSources().playerAttack(player), dmg);

        boolean overdrive = player.getPersistentData().getInt("VoidOverdriveActive") > 0;
        int marksToApply  = overdrive ? MAX_VOID_MARKS : (ascension >= 6 ? 2 : 1);
        applyVoidMarks(target, player, marksToApply);

        for (int i = 0; i < 6; i++) {
            Vec3 trailPos = player.position().subtract(dir.scale(i * 0.3));
            level.sendParticles(ParticleTypes.PORTAL,
                    trailPos.x, trailPos.y + 1, trailPos.z, 2, 0.1, 0.2, 0.1, 0.03);
        }
        level.playSound(null, target.blockPosition(),
                SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.6f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 150);
        setCooldown(player, "Ability1CD", 70);
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 2 — PHASE SHIFT
    // ─────────────────────────────────────────────────────────────────

    public static void voidWalkerAbilityTwoUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
//        player.sendSystemMessage(Component.literal(String.valueOf(player.getPersistentData().getInt("Ability2CD"))));

        if (!validateUse(player, "Ability2CD", 200, 250)) return;

        CompoundTag nbt = player.getPersistentData();
        nbt.putInt("PhaseShiftActive", 100);

        player.getActiveEffects().stream()
                .filter(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                .map(e -> e.getEffect())
                .toList()
                .forEach(player::removeEffect);

        int ascension = SoulCore.getAscensionStage(player);
        if (ascension >= 5 && nbt.contains("VoidAnchorId")) {
            nbt.putInt("PhaseShiftTarget", nbt.getInt("VoidAnchorId"));
        }

        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1, player.getZ(),
                15, 0.5, 0.8, 0.5, 0.08);
        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 1.8f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 250);
        setCooldown(player, "Ability2CD", 200);
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 3 — VOID ANCHOR
    // ─────────────────────────────────────────────────────────────────

    public static void voidWalkerAbilityThreeUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
//        player.sendSystemMessage(Component.literal(String.valueOf(player.getPersistentData().getInt("Ability3CD"))));

        if (!validateUse(player, "Ability3CD", 400, 200)) return;

        LivingEntity target = findClosestInSight(player, level, 20.0);
        if (target == null) return;

        CompoundTag nbt = player.getPersistentData();
        nbt.putInt("VoidAnchorId", target.getId());

        int ascension = SoulCore.getAscensionStage(player);
        if (ascension >= 5) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 400, 0, false, false));
            nbt.putBoolean("VoidAnchorCrit", true);
        } else {
            nbt.remove("VoidAnchorCrit");
        }

        level.sendParticles(ParticleTypes.PORTAL,
                target.getX(), target.getY() + 1, target.getZ(),
                12, 0.5, 0.8, 0.5, 0.06);
        level.playSound(null, target.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8f, 0.6f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 200);
        setCooldown(player, "Ability3CD", 400);
    }

    public static void tickVoidAnchorPull(Player player, ServerLevel level) {
        CompoundTag nbt = player.getPersistentData();
        if (!nbt.contains("VoidAnchorId")) return;

        LivingEntity anchor = findEntityById(level, nbt.getInt("VoidAnchorId"));
        if (anchor == null || !anchor.isAlive()) {
            nbt.remove("VoidAnchorId");
            nbt.remove("VoidAnchorCrit");
            return;
        }

        double dist = player.distanceTo(anchor);
        if (dist > 4.0) {
            Vec3 pull = anchor.position().subtract(player.position()).normalize().scale(0.04);
            player.setDeltaMovement(player.getDeltaMovement().add(pull));
            player.hurtMarked = true;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 4 — COLLAPSE STRIKE
    // ─────────────────────────────────────────────────────────────────

    public static void voidWalkerAbilityFourUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
        if (!validateUse(player, "CollapseStrikeCD", 0, 300)) return;

        LivingEntity target = getAnchorTarget(player, level);
        if (target == null) target = findClosestInSight(player, level, 12.0);
        if (target == null) return;

        CompoundTag tnbt = target.getPersistentData();
        int stacks       = tnbt.getInt("VoidMarkStacks");
        if (stacks < 1) stacks = 1;

        tnbt.putInt("VoidMarkStacks", 0);
        tnbt.remove("VoidMarkTimer");

        int ascension = SoulCore.getAscensionStage(player);
        float dmg     = COLLAPSE_DMG_PER_STACK * stacks + ascension * 1.5f;

        target.hurt(level.damageSources().playerAttack(player), dmg);

        if (ascension >= 6) tnbt.putInt("VoidFractured", 60);

        level.sendParticles(ParticleTypes.EXPLOSION,
                target.getX(), target.getY() + 1, target.getZ(),
                stacks, 0.3, 0.3, 0.3, 0.05);
        level.sendParticles(ParticleTypes.PORTAL,
                target.getX(), target.getY() + 1, target.getZ(),
                stacks * 4, 0.5, 0.8, 0.5, 0.1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.9f, 1.4f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 300);

        boolean overdrive = player.getPersistentData().getInt("VoidOverdriveActive") > 0;
        if (!overdrive) {
            int cd = Math.max(60, 160 - stacks * 20);
            setCooldown(player, "CollapseStrikeCD", cd);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 5 — RIFT STEP
    // ─────────────────────────────────────────────────────────────────

    public static void voidWalkerAbilityFiveUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;

        if (!SoulCore.getAspect(player).equals("Void Walker")) return;
        if (SoulCore.getSoulEssence(player) < 250) return;

        CompoundTag nbt = player.getPersistentData();
        int ascension   = SoulCore.getAscensionStage(player);
        int maxCharges  = ascension >= 6 ? 2 : 1;

        if (!nbt.contains("Ability5Charges")) nbt.putInt("Ability5Charges", maxCharges);

        int charges = nbt.getInt("Ability5Charges");
        if (charges <= 0) return;

        LivingEntity target = getAnchorTarget(player, level);
        if (target == null) target = findClosestInSight(player, level, 30.0);
        if (target == null) return;

        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1, player.getZ(),
                10, 0.3, 0.6, 0.3, 0.06);

        Vec3 dir    = target.position().subtract(player.position()).normalize();
        Vec3 arrPos = target.position().subtract(dir.scale(1.2));
        player.teleportTo(arrPos.x, arrPos.y, arrPos.z);

        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1, player.getZ(),
                10, 0.3, 0.6, 0.3, 0.06);
        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.4f);

        applyVoidMarks(target, player, 1);
        nbt.remove("Ability1CD");

        if (ascension >= 6) nbt.putInt("RiftStepInvisCD", 40);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 250);
        nbt.putInt("Ability5Charges", charges - 1);
        setCooldown(player, "Ability5CD", 180);
    }

    public static void tickRiftStepCharges(Player player) {
        CompoundTag nbt = player.getPersistentData();
        if (nbt.contains("Ability5CD")) return;

        int ascension  = SoulCore.getAscensionStage(player);
        int maxCharges = ascension >= 6 ? 2 : 1;
        int charges    = nbt.getInt("Ability5Charges");

        if (charges < maxCharges) {
            nbt.putInt("Ability5Charges", charges + 1);
            if (charges + 1 < maxCharges) setCooldown(player, "Ability5CD", 180);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 6 — VOID CONSUMPTION
    // ─────────────────────────────────────────────────────────────────

    public static void voidWalkerAbilitySixUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
//        player.sendSystemMessage(Component.literal(String.valueOf(player.getPersistentData().getInt("Ability6CD"))));

        if (!validateUse(player, "Ability6CD", 300, 350)) return;

        LivingEntity target = getAnchorTarget(player, level);
        if (target == null) target = findClosestInSight(player, level, 12.0);
        if (target == null) return;

        int ascension   = SoulCore.getAscensionStage(player);
        float threshold = ascension >= 7 ? 0.40f : 0.30f;
        float hpPercent = target.getHealth() / target.getMaxHealth();

        if (hpPercent > threshold) {
            level.sendParticles(ParticleTypes.SMOKE,
                    target.getX(), target.getY() + 2, target.getZ(),
                    5, 0.2, 0.2, 0.2, 0.02);
            return;
        }

        float dmg    = 20.0f + ascension * 3.0f + (target.getMaxHealth() * 0.15f);
        boolean willKill = target.getHealth() <= dmg;

        target.hurt(level.damageSources().playerAttack(player), dmg);

        level.sendParticles(ParticleTypes.EXPLOSION,
                target.getX(), target.getY() + 1, target.getZ(),
                5, 0.5, 0.5, 0.5, 0.1);
        level.sendParticles(ParticleTypes.PORTAL,
                target.getX(), target.getY() + 1, target.getZ(),
                16, 0.6, 0.8, 0.6, 0.12);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.2f);

        if (willKill) {
            player.heal(player.getMaxHealth() * 0.20f);
            player.getPersistentData().remove("CollapseStrikeCD");
            if (ascension >= 7)
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 80, 1, false, false));
            level.sendParticles(ParticleTypes.HEART,
                    player.getX(), player.getY() + 2, player.getZ(),
                    4, 0.4, 0.3, 0.4, 0.05);
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 350);
        setCooldown(player, "Ability6CD", 300);
    }

    // ─────────────────────────────────────────────────────────────────
    //  ABILITY 7 — VOID OVERDRIVE
    // ─────────────────────────────────────────────────────────────────

    public static void voidWalkerAbilitySevenUsed(Player player, ServerLevel level) {
        if (level.isClientSide) return;
//        player.sendSystemMessage(Component.literal(String.valueOf(player.getPersistentData().getInt("Ability7CD"))));

        if (!validateUse(player, "Ability7CD", 2400, 1000)) return;

        player.getPersistentData().putInt("VoidOverdriveActive", 200);

        level.sendParticles(ParticleTypes.EXPLOSION,
                player.getX(), player.getY() + 1, player.getZ(),
                6, 0.6, 0.6, 0.6, 0.1);
        level.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1, player.getZ(),
                40, 1.2, 1.5, 1.2, 0.15);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2f, 0.5f);
        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.4f);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 1000);
        setCooldown(player, "Ability7CD", 2400);
    }

    public static void onKillDuringOverdrive(Player player) {
        CompoundTag nbt = player.getPersistentData();
        int ticks = nbt.getInt("VoidOverdriveActive");
        if (ticks <= 0) return;
        if (SoulCore.getAscensionStage(player) < 7) return;
        nbt.putInt("VoidOverdriveActive", ticks + 60);
    }

    // ─────────────────────────────────────────────────────────────────
    //  DAMAGE AMPLIFICATION
    // ─────────────────────────────────────────────────────────────────

    public static float amplifyDamage(Player player, LivingEntity target, float baseDamage) {
        CompoundTag tnbt = target.getPersistentData();
        int stacks       = tnbt.getInt("VoidMarkStacks");
        float multiplier = 1.0f + (stacks * VOID_MARK_DMG_BONUS);

        CompoundTag pnbt = player.getPersistentData();
        if (pnbt.contains("VoidAnchorId") && pnbt.getInt("VoidAnchorId") == target.getId()) {
            multiplier += 0.20f;
        }
        return baseDamage * multiplier;
    }

    // ─────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────

    private static boolean validateUse(Player player, String cdKey, int cdTicks, int essenceCost) {
        if (!SoulCore.getAspect(player).equals("Void Walker")) return false;
        if (SoulCore.getSoulEssence(player) < essenceCost) return false;
        CompoundTag nbt = player.getPersistentData();
        return !nbt.contains(cdKey) || nbt.getInt(cdKey) <= 0;
    }

    private static void setCooldown(Player player, String cdKey, int ticks) {
        player.getPersistentData().putInt(cdKey, ticks);
    }

    private static AABB centeredBox(Vec3 pos, double halfSize) {
        return new AABB(
                pos.x - halfSize, pos.y - halfSize, pos.z - halfSize,
                pos.x + halfSize, pos.y + halfSize, pos.z + halfSize);
    }

    private static LivingEntity findClosestInSight(Player player, ServerLevel level, double maxDist) {
        Vec3 eye  = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        for (double d = 1.0; d <= maxDist; d += 0.4) {
            Vec3 pos = eye.add(look.scale(d));
            AABB box = centeredBox(pos, 0.9);
            List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e != player && e.isAlive());
            if (!found.isEmpty()) return found.get(0);
        }
        return null;
    }

    private static LivingEntity getAnchorTarget(Player player, ServerLevel level) {
        CompoundTag nbt = player.getPersistentData();
        if (!nbt.contains("VoidAnchorId")) return null;
        LivingEntity anchor = findEntityById(level, nbt.getInt("VoidAnchorId"));
        if (anchor == null || !anchor.isAlive()) {
            nbt.remove("VoidAnchorId");
            nbt.remove("VoidAnchorCrit");
            return null;
        }
        return anchor;
    }

    private static LivingEntity findEntityById(ServerLevel level, int id) {
        AABB searchBox = new AABB(-30000, -64, -30000, 30000, 320, 30000);
        List<LivingEntity> all = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e.getId() == id);
        return all.isEmpty() ? null : all.get(0);
    }

    private static void teleportBehind(Player player, LivingEntity target, ServerLevel level) {
        Vec3 dir    = target.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 behind = target.position().subtract(dir.scale(1.5));
        player.teleportTo(behind.x, behind.y, behind.z);
        level.sendParticles(ParticleTypes.PORTAL,
                behind.x, behind.y + 1, behind.z,
                8, 0.2, 0.5, 0.2, 0.05);
        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.8f);
    }
}