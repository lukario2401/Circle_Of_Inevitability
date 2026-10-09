package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Chrono Reaver
 * -----------------------------------------------
 * Time-placement DPS. You don't hit enemies directly —
 * you place delayed zones that trigger after a set time.
 *
 * All pending zones are stored as a ListTag of CompoundTags
 * on the PLAYER under "ChronoZones". Each zone tag contains:
 *   "Type"      → String  SLASH | SNARE | REWIND | STEP | EXECUTE
 *   "X/Y/Z"     → double  world position
 *   "Timer"     → int     ticks until trigger
 *   "Damage"    → float   base damage
 *   "OwnerUUID" → UUID    player UUID
 *   "Phase"     → int     for REWIND only: 1 = forward, 2 = backward
 *   "TargetUUID"→ UUID    for EXECUTE only: target entity UUID
 *   "ExtraDmg"  → float   for EXECUTE: bonus from hits taken during delay
 *
 * Player NBT keys:
 *   "ChronoZones"       → ListTag  all pending zones
 *   "ChronoOverload"    → int      ticks remaining on Chrono Overload
 */
public class ChronoReaver {

    // ─── Zone type constants ──────────────────────────────────────────────────
    private static final String TYPE_SLASH   = "SLASH";
    private static final String TYPE_SNARE   = "SNARE";
    private static final String TYPE_REWIND  = "REWIND";
    private static final String TYPE_STEP    = "STEP";
    private static final String TYPE_EXECUTE = "EXECUTE";

    // ─── NBT keys ─────────────────────────────────────────────────────────────
    private static final String NBT_ZONES    = "ChronoZones";
    private static final String NBT_OVERLOAD = "ChronoOverload";

    // ─── Delays (ticks) ───────────────────────────────────────────────────────
    private static final int DELAY_SLASH   = 20;  // 1 second
    private static final int DELAY_SNARE   = 25;  // 1.25 seconds
    private static final int DELAY_REWIND  = 40;  // 2 seconds
    private static final int DELAY_REWIND2 = 15;  // 0.75s after first rewind hit
    private static final int DELAY_STEP    = 30;  // 1.5 seconds
    private static final int DELAY_EXECUTE = 60;  // 3 seconds

    // ─── Overload duration ────────────────────────────────────────────────────
    private static final int OVERLOAD_DURATION = 200; // 10 seconds

    // ─── Helpers ─────────────────────────────────────────────────────────────

    public static boolean inOverload(Player player) {
        return player.getPersistentData().getInt(NBT_OVERLOAD) > 0;
    }

    /** Returns the tick delay to use — 0 if in Chrono Overload, otherwise normal. */
    private static int delay(Player player, int normalDelay) {
        return inOverload(player) ? 0 : normalDelay;
    }

    /** Damage multiplier during Chrono Overload. */
    private static float overloadMult(Player player) {
        return inOverload(player) ? 1.5f : 1.0f;
    }

    /** Returns the ListTag of pending zones (creates it if absent). */
    private static ListTag getZones(Player player) {
        CompoundTag data = player.getPersistentData();
        if (!data.contains(NBT_ZONES)) data.put(NBT_ZONES, new ListTag());
        return data.getList(NBT_ZONES, Tag.TAG_COMPOUND);
    }

    /** Adds a zone CompoundTag to the player's zone list. */
    private static void addZone(Player player, CompoundTag zone) {
        ListTag zones = getZones(player);
        zones.add(zone);
        player.getPersistentData().put(NBT_ZONES, zones);
    }

    /** Builds a base zone tag with shared fields. */
    private static CompoundTag baseZone(String type, Vec3 pos, int timerTicks, float damage, UUID ownerUUID) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Type", type);
        tag.putDouble("X", pos.x);
        tag.putDouble("Y", pos.y);
        tag.putDouble("Z", pos.z);
        tag.putInt("Timer", timerTicks);
        tag.putFloat("Damage", damage);
        tag.putUUID("OwnerUUID", ownerUUID);
        return tag;
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
    //  EVENTS — single class, unique name
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ChronoEvents {

        @SubscribeEvent
        public static void onChronoPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Chrono Reaver")) return;

            // ── Overload countdown ────────────────────────────────────────────
            int overload = player.getPersistentData().getInt(NBT_OVERLOAD);
            if (overload > 0) {
                player.getPersistentData().putInt(NBT_OVERLOAD, overload - 1);
                if (player.tickCount % 4 == 0)
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                            player.getX(), player.getY() + 1, player.getZ(),
                            4, 0.4, 0.4, 0.4, 0.03);
            }

            // ── Tick all pending zones ────────────────────────────────────────
            CompoundTag data = player.getPersistentData();
            if (!data.contains(NBT_ZONES)) return;

            ListTag zones      = data.getList(NBT_ZONES, Tag.TAG_COMPOUND);
            ListTag remaining  = new ListTag();

            for (int i = 0; i < zones.size(); i++) {
                CompoundTag zone = zones.getCompound(i);
                int timer = zone.getInt("Timer");

                // Visual pulse every 5 ticks while pending
                if (timer % 5 == 0) {
                    Vec3 pos = new Vec3(zone.getDouble("X"), zone.getDouble("Y"), zone.getDouble("Z"));
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 0.2, pos.z, 2, 0.3, 0.1, 0.3, 0.01);
                }

                if (timer <= 0) {
                    // Trigger the zone
                    triggerZone(zone, player, sl);
                    // Don't add to remaining — zone is consumed
                } else {
                    zone.putInt("Timer", timer - 1);
                    remaining.add(zone);
                }
            }

            data.put(NBT_ZONES, remaining);
        }

        /** Tracks damage dealt to EXECUTE targets so we can add bonus damage. */
        @SubscribeEvent
        public static void onChronoExecuteHit(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Chrono Reaver")) return;

            LivingEntity victim = event.getEntity();
            CompoundTag data = player.getPersistentData();
            if (!data.contains(NBT_ZONES)) return;

            ListTag zones = data.getList(NBT_ZONES, Tag.TAG_COMPOUND);
            for (int i = 0; i < zones.size(); i++) {
                CompoundTag zone = zones.getCompound(i);
                if (!zone.getString("Type").equals(TYPE_EXECUTE)) continue;
                if (!zone.contains("TargetUUID")) continue;
                UUID targetUUID = zone.getUUID("TargetUUID");
                if (!targetUUID.equals(victim.getUUID())) continue;
                // Accumulate extra damage: 50% of the hit that landed
                float extra = zone.getFloat("ExtraDmg") + event.getAmount() * 0.5f;
                zone.putFloat("ExtraDmg", extra);
            }
            data.put(NBT_ZONES, zones);
        }
    }

    // =========================================================================
    //  ZONE TRIGGER — dispatches zone effects when timer hits 0
    // =========================================================================

    private static void triggerZone(CompoundTag zone, Player player, ServerLevel sl) {
        String type  = zone.getString("Type");
        Vec3   pos   = new Vec3(zone.getDouble("X"), zone.getDouble("Y"), zone.getDouble("Z"));
        float  dmg   = zone.getFloat("Damage");
        int    stage = SoulCore.getAscensionStage(player);

        switch (type) {
            case TYPE_SLASH  -> triggerSlash(zone, player, sl, pos, dmg);
            case TYPE_SNARE  -> triggerSnare(zone, player, sl, pos, dmg);
            case TYPE_REWIND -> triggerRewind(zone, player, sl, pos, dmg, stage);
            case TYPE_STEP   -> triggerStep(zone, player, sl, pos, dmg);
            case TYPE_EXECUTE-> triggerExecute(zone, player, sl);
        }
    }

    // ─── Slash trigger ────────────────────────────────────────────────────────

    private static void triggerSlash(CompoundTag zone, Player player, ServerLevel sl, Vec3 pos, float dmg) {
        // Line damage: ray-cast from stored position along stored direction
        double dx = zone.getDouble("DirX");
        double dy = zone.getDouble("DirY");
        double dz = zone.getDouble("DirZ");
        Vec3 dir  = new Vec3(dx, dy, dz).normalize();

        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.8f);

        Vec3 cur = pos;
        for (int i = 0; i < 10; i++) {
            cur = cur.add(dir);
            sl.sendParticles(ParticleTypes.CRIT, cur.x, cur.y, cur.z, 3, 0.2, 0.2, 0.2, 0.04);
            sl.getEntitiesOfClass(LivingEntity.class, new AABB(cur, cur).inflate(0.6),
                            e -> !e.getUUID().equals(player.getUUID()) && e.isAlive())
                    .forEach(e -> {
                        e.hurt(sl.damageSources().playerAttack(player), dmg);
                        e.invulnerableTime = 0;
                    });
        }
    }

    // ─── Snare trigger ────────────────────────────────────────────────────────

    private static void triggerSnare(CompoundTag zone, Player player, ServerLevel sl, Vec3 pos, float dmg) {
        float radius = zone.getFloat("Radius");
        sl.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 0.2, pos.z, 30, radius, 0.1, radius, 0.01);
        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.8f, 1.5f);

        sl.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(radius),
                        e -> !e.getUUID().equals(player.getUUID()) && e.isAlive())
                .forEach(e -> {
                    e.hurt(sl.damageSources().playerAttack(player), dmg);
                    e.invulnerableTime = 0;
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3, false, true));
                });
    }

    // ─── Rewind trigger ───────────────────────────────────────────────────────

    private static void triggerRewind(CompoundTag zone, Player player, ServerLevel sl, Vec3 pos, float dmg, int stage) {
        int phase = zone.getInt("Phase");

        if (phase == 1) {
            // Forward explosion
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 1, pos.z, 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.CRIT, pos.x, pos.y + 1, pos.z, 16, 0.5, 0.5, 0.5, 0.05);
            sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1f, 1.0f);

            sl.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(3.5),
                            e -> !e.getUUID().equals(player.getUUID()) && e.isAlive())
                    .forEach(e -> {
                        e.hurt(sl.damageSources().playerAttack(player), dmg);
                        e.invulnerableTime = 0;
                    });

            // Queue phase 2 (rewind hit)
            CompoundTag phase2 = baseZone(TYPE_REWIND, pos, DELAY_REWIND2, dmg * 0.8f, player.getUUID());
            phase2.putInt("Phase", 2);
            addZone(player, phase2);

        } else {
            // Rewind explosion — slightly wider, knocks back
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 1, pos.z, 20, 0.5, 0.5, 0.5, 0.05);
            sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1f, 0.6f);

            sl.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(4.5),
                            e -> !e.getUUID().equals(player.getUUID()) && e.isAlive())
                    .forEach(e -> {
                        e.hurt(sl.damageSources().playerAttack(player), dmg);
                        e.invulnerableTime = 0;
                        // Knock outward from zone center
                        Vec3 push = e.position().subtract(pos).normalize().scale(0.6);
                        e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.3, push.z));
                    });
        }
    }

    // ─── Step trigger ─────────────────────────────────────────────────────────

    private static void triggerStep(CompoundTag zone, Player player, ServerLevel sl, Vec3 pos, float dmg) {
        sl.sendParticles(ParticleTypes.EXPLOSION, pos.x, pos.y + 0.5, pos.z, 6, 0.4, 0.4, 0.4, 0.04);
        sl.sendParticles(ParticleTypes.CRIT, pos.x, pos.y + 0.5, pos.z, 10, 0.5, 0.5, 0.5, 0.05);
        sl.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8f, 1.3f);

        sl.getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(2.5),
                        e -> !e.getUUID().equals(player.getUUID()) && e.isAlive())
                .forEach(e -> {
                    e.hurt(sl.damageSources().playerAttack(player), dmg);
                    e.invulnerableTime = 0;
                });
    }

    // ─── Execute trigger ──────────────────────────────────────────────────────

    private static void triggerExecute(CompoundTag zone, Player player, ServerLevel sl) {
        if (!zone.contains("TargetUUID")) return;
        UUID targetUUID = zone.getUUID("TargetUUID");
        float baseDmg   = zone.getFloat("Damage");
        float extraDmg  = zone.getFloat("ExtraDmg");
        float totalDmg  = baseDmg + extraDmg;

        sl.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(50),
                        e -> e.getUUID().equals(targetUUID) && e.isAlive())
                .forEach(e -> {
                    e.hurt(sl.damageSources().playerAttack(player), totalDmg);
                    e.invulnerableTime = 0;
                    sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                            e.getX(), e.getY() + 1, e.getZ(), 2, 0, 0, 0, 0);
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                            e.getX(), e.getY() + 1, e.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
                    sl.playSound(null, e.getX(), e.getY(), e.getZ(),
                            SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2f, 0.7f);
                });

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§6Delayed Execution triggered: §f" + String.format("%.1f", baseDmg)
                            + " §7+ §e" + String.format("%.1f", extraDmg) + " bonus§f = §c"
                            + String.format("%.1f", totalDmg) + " total dmg"));
    }

    // =========================================================================
    //  ABILITY 1 — TEMPORAL SLASH
    // =========================================================================

    /**
     * Places a delayed slash line 3 blocks in front of the player.
     * After 1 second: activates in a 10-block line along look direction.
     * Cost: 300 essence.
     */
    public static void temporalSlash(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Chrono Reaver")) return;
        if (SoulCore.getSoulEssence(player) < 300) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 300);

        int   stage  = SoulCore.getAscensionStage(player);
        float damage = (14.0f + stage * 2) * overloadMult(player);
        int   d      = delay(player, DELAY_SLASH);

        Vec3 pos = player.getEyePosition().add(player.getLookAngle().normalize().scale(3));
        Vec3 dir = player.getLookAngle().normalize();

        CompoundTag zone = baseZone(TYPE_SLASH, pos, d, damage, player.getUUID());
        zone.putDouble("DirX", dir.x);
        zone.putDouble("DirY", dir.y);
        zone.putDouble("DirZ", dir.z);

        if (d == 0) {
            // Overload: instant
            triggerZone(zone, player, sl);
        } else {
            addZone(player, zone);
            // Preview particles at placement spot
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y, pos.z, 5, 0.3, 0.3, 0.3, 0.02);
            sl.playSound(null, player.blockPosition(), SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.5f, 1.8f);
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§bTemporal Slash placed. §7Triggers in §b1s."));
        }
    }

    // =========================================================================
    //  ABILITY 2 — TIME SNARE
    // =========================================================================

    /**
     * Places a circular zone at the player's feet (or aimed location).
     * After 1.25 seconds: enemies inside are slowed + take damage.
     * Cost: 400 essence.  Requires stage ≥ 1.
     */
    public static void timeSnare(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Chrono Reaver")) return;
        if (SoulCore.getSoulEssence(player) < 400) return;
        if (SoulCore.getAscensionStage(player) < 1) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 400);

        int   stage  = SoulCore.getAscensionStage(player);
        float damage = (10.0f + stage * 2) * overloadMult(player);
        float radius = 3.0f + stage * 0.5f;
        int   d      = delay(player, DELAY_SNARE);

        // Place at player's feet
        Vec3 pos = player.position();

        CompoundTag zone = baseZone(TYPE_SNARE, pos, d, damage, player.getUUID());
        zone.putFloat("Radius", radius);

        if (d == 0) {
            triggerZone(zone, player, sl);
        } else {
            addZone(player, zone);
            sl.sendParticles(ParticleTypes.SNOWFLAKE, pos.x, pos.y + 0.1, pos.z, 12, radius / 2, 0.05, radius / 2, 0.01);
            sl.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5f, 1.8f);
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§bTime Snare placed. §7Triggers in §b1.25s."));
        }
    }

    // =========================================================================
    //  ABILITY 3 — REWIND BURST
    // =========================================================================

    /**
     * Marks a location for a double explosion:
     *   Phase 1 (2 seconds): forward blast
     *   Phase 2 (0.75s after): rewind blast (hits again, wider, knockback)
     * Cost: 700 essence.  Requires stage ≥ 2.
     */
    public static void rewindBurst(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Chrono Reaver")) return;
        if (SoulCore.getSoulEssence(player) < 700) return;
        if (SoulCore.getAscensionStage(player) < 2) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 700);

        int   stage  = SoulCore.getAscensionStage(player);
        float damage = (20.0f + stage * 3) * overloadMult(player);
        int   d      = delay(player, DELAY_REWIND);

        // Place 5 blocks in front of player
        Vec3 pos = player.getEyePosition().add(player.getLookAngle().normalize().scale(5));

        CompoundTag zone = baseZone(TYPE_REWIND, pos, d, damage, player.getUUID());
        zone.putInt("Phase", 1);

        if (d == 0) {
            triggerZone(zone, player, sl);
        } else {
            addZone(player, zone);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.x, pos.y + 1, pos.z, 8, 0.4, 0.4, 0.4, 0.03);
            sl.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 0.6f);
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§bRewind Burst placed. §7Double detonation in §b2s."));
        }
    }

    // =========================================================================
    //  ABILITY 4 — FUTURE STEP
    // =========================================================================

    /**
     * Dashes forward instantly. Leaves a "ghost position" at the dash origin.
     * After 1.5 seconds: origin explodes.
     * Cost: 500 essence.  Requires stage ≥ 3.
     */
    public static void futureStep(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Chrono Reaver")) return;
        if (SoulCore.getSoulEssence(player) < 500) return;
        if (SoulCore.getAscensionStage(player) < 3) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 500);

        int   stage  = SoulCore.getAscensionStage(player);
        float damage = (16.0f + stage * 2) * overloadMult(player);
        int   d      = delay(player, DELAY_STEP);

        // Record current position before dash
        Vec3 origin = player.position();

        // Dash forward
        Vec3 dir = player.getLookAngle().normalize();
        player.setDeltaMovement(dir.scale(3.5));
        player.hurtMarked = true;

        // Particles at origin (ghost)
        sl.sendParticles(ParticleTypes.REVERSE_PORTAL, origin.x, origin.y + 1, origin.z, 10, 0.3, 0.3, 0.3, 0.03);
        sl.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.4f);

        CompoundTag zone = baseZone(TYPE_STEP, origin, d, damage, player.getUUID());

        if (d == 0) {
            triggerZone(zone, player, sl);
        } else {
            addZone(player, zone);
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§bFuture Step! §7Ghost explodes in §b1.5s."));
        }
    }

    // =========================================================================
    //  ABILITY 5 — TIME COLLAPSE
    // =========================================================================

    /**
     * Instantly triggers ALL pending zones.
     * This is the high-skill payoff ability — stack placements, then detonate.
     * Cost: 800 essence.  Requires stage ≥ 4.
     */
    public static void timeCollapse(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Chrono Reaver")) return;
        if (SoulCore.getSoulEssence(player) < 800) return;
        if (SoulCore.getAscensionStage(player) < 4) return;

        CompoundTag data = player.getPersistentData();
        if (!data.contains(NBT_ZONES)) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo pending time zones to collapse!"));
            return;
        }

        ListTag zones = data.getList(NBT_ZONES, Tag.TAG_COMPOUND);
        if (zones.isEmpty()) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo pending time zones to collapse!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 800);

        int count = zones.size();
        // Collect separately — triggerZone may add new zones (REWIND phase 2)
        List<CompoundTag> toTrigger = new ArrayList<>();
        for (int i = 0; i < zones.size(); i++) toTrigger.add(zones.getCompound(i));

        // Clear the list first
        data.put(NBT_ZONES, new ListTag());

        // Trigger all
        for (CompoundTag zone : toTrigger) {
            zone.putInt("Timer", 0);
            triggerZone(zone, player, sl);
        }

        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(), 3, 0.5, 0.5, 0.5, 0);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5f, 0.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§c§lTIME COLLAPSE! §r§b" + count + " zones detonated simultaneously!"));
    }

    // =========================================================================
    //  ABILITY 6 — DELAYED EXECUTION
    // =========================================================================

    /**
     * Marks an enemy. After 3 seconds: deals massive damage.
     * Any damage YOU deal to them during the delay adds 50% of that hit as bonus.
     * Cost: 1000 essence.  Requires stage ≥ 5.
     */
    public static void delayedExecution(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Chrono Reaver")) return;
        if (SoulCore.getSoulEssence(player) < 1000) return;
        if (SoulCore.getAscensionStage(player) < 5) return;

        LivingEntity target = rayCastFirst(player, level, 16 + SoulCore.getAscensionStage(player));
        if (target == null) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 1000);

        int   stage  = SoulCore.getAscensionStage(player);
        float damage = (30.0f + stage * 4) * overloadMult(player);
        int   d      = delay(player, DELAY_EXECUTE);

        // Place at target's current position (follows by UUID at trigger time)
        CompoundTag zone = baseZone(TYPE_EXECUTE, target.position(), d, damage, player.getUUID());
        zone.putUUID("TargetUUID", target.getUUID());
        zone.putFloat("ExtraDmg", 0f);

        if (d == 0) {
            triggerZone(zone, player, sl);
        } else {
            addZone(player, zone);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    target.getX(), target.getY() + 1, target.getZ(), 12, 0.3, 0.3, 0.3, 0.03);
            sl.playSound(null, target.blockPosition(),
                    SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 0.8f, 0.4f);
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§6Delayed Execution placed on §f" + target.getName().getString()
                                + "§6. Triggers in §b3s§6. Deal damage for bonus!"));
        }
    }

    // =========================================================================
    //  ABILITY 7 — CHRONO OVERLOAD  (ultimate)
    // =========================================================================

    /**
     * For 10 seconds:
     *   - All abilities have 0 delay (instant trigger)
     *   - All zones deal ×1.5 damage
     *   - Spam zones rapidly (no placement preview needed)
     * Cost: 5000 essence.  Requires stage ≥ 7.
     */
    public static void chronoOverload(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Chrono Reaver")) return;
        if (SoulCore.getSoulEssence(player) < 5000) return;
        if (SoulCore.getAscensionStage(player) < 7) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 5000);

        player.getPersistentData().putInt(NBT_OVERLOAD, OVERLOAD_DURATION);

        sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                player.getX(), player.getY() + 1, player.getZ(), 40, 1.0, 1.0, 1.0, 0.07);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(), 2, 0.3, 0.3, 0.3, 0);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 0.3f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§d§l⏳ CHRONO OVERLOAD! §r§bAll zones instant. ×1.5 damage. 10 seconds."));
    }
}