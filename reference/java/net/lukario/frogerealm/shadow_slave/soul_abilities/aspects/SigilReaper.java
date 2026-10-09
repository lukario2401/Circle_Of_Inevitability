package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * Sigil Reaper
 * -----------------------------------------------
 * Marks enemies with three kinds of Sigils stored
 * in NBT (no potion effects needed).  All damage
 * comes from detonating the right combination at
 * the right moment.
 *
 * Sigil types  (stored as strings in NBT list "SigilReaperMarks"):
 *   "RUIN"     → raw explosion damage
 *   "VOID"     → chain to nearby enemies
 *   "COLLAPSE" → damage multiplier
 *
 * NBT keys on a LivingEntity:
 *   "SigilReaperMarks"        → ListTag of StringTags (one per sigil slot)
 *   "SigilReaperOwner"        → UUID of the player who placed them
 *   "SigilReaperDelay"        → ticks until Delayed Collapse auto-detonates (-1 = inactive)
 *   "SigilReaperDelayOwner"   → UUID for the delayed detonation
 *
 * NBT keys stored on the PLAYER entity:
 *   "SigilSurgeOwner"         → UUID (confirms surge belongs to this player)
 *   "SigilSurgeTimer"         → ticks remaining for surge
 *   "ApocalypseOwner"         → UUID
 *   "ApocalypseTimer"         → ticks remaining
 */
public class SigilReaper {

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final int      MAX_SIGILS_NORMAL  = 6;
    private static final String[] SIGIL_TYPES        = {"RUIN", "VOID", "COLLAPSE"};
    private static final String   NBT_MARKS          = "SigilReaperMarks";
    private static final String   NBT_OWNER          = "SigilReaperOwner";
    private static final String   NBT_DELAY          = "SigilReaperDelay";
    private static final String   NBT_DELAY_OWNER    = "SigilReaperDelayOwner";
    private static final String   NBT_SURGE_OWNER    = "SigilSurgeOwner";
    private static final String   NBT_SURGE_TIMER    = "SigilSurgeTimer";
    private static final String   NBT_APOC_OWNER     = "ApocalypseOwner";
    private static final String   NBT_APOC_TIMER     = "ApocalypseTimer";

    // ─── NBT Helpers ──────────────────────────────────────────────────────────

    /** Returns the sigil list on an entity (may be empty). */
    private static List<String> getSigils(LivingEntity entity) {
        List<String> result = new ArrayList<>();
        if (!entity.getPersistentData().contains(NBT_MARKS)) return result;
        ListTag list = entity.getPersistentData().getList(NBT_MARKS, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) result.add(list.getString(i));
        return result;
    }

    /** Saves the sigil list back to the entity's NBT. */
    private static void setSigils(LivingEntity entity, List<String> sigils) {
        ListTag list = new ListTag();
        for (String s : sigils) list.add(StringTag.valueOf(s));
        entity.getPersistentData().put(NBT_MARKS, list);
    }

    /** Counts how many sigils of a given type are on the entity. */
    private static long countSigil(List<String> sigils, String type) {
        return sigils.stream().filter(s -> s.equals(type)).count();
    }

    /** True if this player is currently in Sigil Surge. */
    private static boolean inSurge(Player player) {
        if (!player.getPersistentData().contains(NBT_SURGE_OWNER)) return false;
        UUID id = player.getPersistentData().getUUID(NBT_SURGE_OWNER);
        return id.equals(player.getUUID()) && player.getPersistentData().getInt(NBT_SURGE_TIMER) > 0;
    }

    /** True if this player is currently in Apocalypse Script. */
    private static boolean inApocalypse(Player player) {
        if (!player.getPersistentData().contains(NBT_APOC_OWNER)) return false;
        UUID id = player.getPersistentData().getUUID(NBT_APOC_OWNER);
        return id.equals(player.getUUID()) && player.getPersistentData().getInt(NBT_APOC_TIMER) > 0;
    }

    /**
     * Adds {@code count} random sigils to an entity.
     * Respects the cap unless {@code ignoreCap} is true.
     * Clears existing sigils if they belong to a different player.
     */
    private static void addSigils(LivingEntity entity, Player owner, int count, boolean ignoreCap) {
        List<String> sigils = getSigils(entity);

        // Reset if owned by someone else
        if (entity.getPersistentData().contains(NBT_OWNER)) {
            UUID existing = entity.getPersistentData().getUUID(NBT_OWNER);
            if (!existing.equals(owner.getUUID())) sigils.clear();
        }

        entity.getPersistentData().putUUID(NBT_OWNER, owner.getUUID());

        Random rng = new Random();
        for (int i = 0; i < count; i++) {
            if (!ignoreCap && sigils.size() >= MAX_SIGILS_NORMAL) break;
            sigils.add(SIGIL_TYPES[rng.nextInt(SIGIL_TYPES.length)]);
        }
        setSigils(entity, sigils);
    }

    /** Applies all three sigil types once each (Apocalypse Script hit). */
    private static void addAllSigils(LivingEntity entity, Player owner) {
        List<String> sigils = getSigils(entity);
        entity.getPersistentData().putUUID(NBT_OWNER, owner.getUUID());
        sigils.add("RUIN");
        sigils.add("VOID");
        sigils.add("COLLAPSE");
        setSigils(entity, sigils);
    }

    // ─── Ray-cast utility ─────────────────────────────────────────────────────

    /** Forward ray-cast returning the first living enemy in line-of-sight. */
    private static LivingEntity rayCastFirstEnemy(Player player, Level level, int maxBlocks) {
        Vec3 start     = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 current   = start;

        for (int i = 0; i < maxBlocks; i++) {
            current = current.add(direction);
            List<LivingEntity> hits = level.getEntitiesOfClass(
                    LivingEntity.class, new AABB(current, current).inflate(0.5),
                    e -> e != player && e.isAlive());
            if (!hits.isEmpty()) return hits.get(0);
        }
        return null;
    }

    // =========================================================================
    //  TICK EVENTS — timers + Delayed Collapse auto-detonation
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class TickEvents {

        @SubscribeEvent
        public static void onLevelTick(TickEvent.LevelTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            if (!(event.level instanceof ServerLevel serverLevel)) return;

            for (Entity entity : serverLevel.getAllEntities()) {

                // ── Player timers ─────────────────────────────────────────────
                if (entity instanceof Player player &&
                        SoulCore.getAspect(player).equals("Sigil Reaper")) {

                    // Surge countdown
                    if (player.getPersistentData().contains(NBT_SURGE_TIMER)) {
                        int t = player.getPersistentData().getInt(NBT_SURGE_TIMER);
                        if (t > 0) player.getPersistentData().putInt(NBT_SURGE_TIMER, t - 1);
                    }

                    // Apocalypse countdown
                    if (player.getPersistentData().contains(NBT_APOC_TIMER)) {
                        int t = player.getPersistentData().getInt(NBT_APOC_TIMER);
                        if (t > 0) {
                            player.getPersistentData().putInt(NBT_APOC_TIMER, t - 1);
                            serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH,
                                    player.getX(), player.getY() + 1, player.getZ(),
                                    6, 0.6, 0.6, 0.6, 0.04);
                        }
                    }
                }

                // ── Delayed Collapse countdown on any living entity ────────────
                if (entity instanceof LivingEntity living &&
                        living.getPersistentData().contains(NBT_DELAY)) {

                    int delay = living.getPersistentData().getInt(NBT_DELAY);
                    if (delay <= 0) continue;

                    delay--;
                    living.getPersistentData().putInt(NBT_DELAY, delay);

                    // Warning particles every 10 ticks
                    if (delay % 10 == 0) {
                        serverLevel.sendParticles(ParticleTypes.WITCH,
                                living.getX(), living.getY() + 1, living.getZ(),
                                4, 0.3, 0.3, 0.3, 0);
                    }

                    // Auto-detonate when timer hits zero
                    if (delay == 0) {
                        UUID ownerUUID = living.getPersistentData().getUUID(NBT_DELAY_OWNER);
                        ServerPlayer owner = (ServerPlayer) serverLevel.getPlayerByUUID(ownerUUID);
                        living.getPersistentData().remove(NBT_DELAY);
                        living.getPersistentData().remove(NBT_DELAY_OWNER);
                        if (owner != null) {
                            detonateSigils(owner, living, serverLevel, true, false);
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    //  RESONANCE — mixed-sigil damage multiplier (Ability 2 passive)
    // =========================================================================

    /**
     * Calculates the Resonance multiplier for a given sigil composition.
     *
     * Rules:
     *   Ruin + Collapse  → ×1.5 per Collapse sigil
     *   Void  + Ruin     → ×1.3 per Void sigil
     *   All three types  → additional ×2.0 flat bonus
     */
    private static float resonanceMultiplier(List<String> sigils) {
        long ruin     = countSigil(sigils, "RUIN");
        long voidS    = countSigil(sigils, "VOID");
        long collapse = countSigil(sigils, "COLLAPSE");

        float mult = 1.0f;
        if (collapse > 0 && ruin > 0)           mult *= (float) Math.pow(1.5, collapse);
        if (voidS   > 0 && ruin > 0)            mult *= (float) Math.pow(1.3, voidS);
        if (ruin > 0 && voidS > 0 && collapse > 0) mult *= 2.0f;
        return mult;
    }

    // =========================================================================
    //  CORE DETONATION LOGIC
    // =========================================================================

    /**
     * Detonates all sigils on {@code target}.
     *
     * @param delayBonus  +40% damage when triggered by Delayed Collapse
     * @param apocalypse  infinite chaining + mini-detonations
     */
    private static void detonateSigils(Player owner, LivingEntity target,
                                       ServerLevel serverLevel,
                                       boolean delayBonus, boolean apocalypse) {

        List<String> sigils = getSigils(target);
        if (sigils.isEmpty()) return;

        long  ruinCount    = countSigil(sigils, "RUIN");
        long  voidCount    = countSigil(sigils, "VOID");
        long  collapseCount = countSigil(sigils, "COLLAPSE");
        float resonance    = resonanceMultiplier(sigils);

        // Base: 10 per Ruin, 4 per Void, 5 per Collapse (Collapse multiplies via resonance)
        float baseDamage = (ruinCount * 10.0f) + (voidCount * 4.0f) + (collapseCount * 5.0f);
        baseDamage *= resonance;
        if (delayBonus)   baseDamage *= 1.4f;
        if (inSurge(owner)) baseDamage *= 0.7f; // Surge penalty

        // Apply to primary target
        target.hurt(serverLevel.damageSources().playerAttack(owner), baseDamage);
        target.invulnerableTime = 0;

        // Clear sigils
        setSigils(target, new ArrayList<>());
        target.getPersistentData().remove(NBT_OWNER);

        // ── Particles ─────────────────────────────────────────────────────────
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
        if (ruinCount  > 0) serverLevel.sendParticles(ParticleTypes.FLAME,
                target.getX(), target.getY() + 1, target.getZ(), (int) ruinCount  * 4, 0.4, 0.4, 0.4, 0.04);
        if (voidCount  > 0) serverLevel.sendParticles(ParticleTypes.PORTAL,
                target.getX(), target.getY() + 1, target.getZ(), (int) voidCount  * 4, 0.4, 0.4, 0.4, 0.04);
        if (collapseCount > 0) serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                target.getX(), target.getY() + 1, target.getZ(), (int) collapseCount * 4, 0.4, 0.4, 0.4, 0.04);
        serverLevel.playSound(null, target.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1f, 0.8f);

        // ── Void chain ────────────────────────────────────────────────────────
        if (voidCount > 0) {
            float chainDamage = baseDamage * 0.4f * voidCount;
            int   chainRadius = apocalypse ? 64 : (int)(4 + voidCount * 2);
            AABB  chainArea   = target.getBoundingBox().inflate(chainRadius);

            List<LivingEntity> chainTargets = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, chainArea,
                    e -> e.isAlive() && !e.equals(target) && !e.equals(owner));

            for (LivingEntity chained : chainTargets) {
                chained.hurt(serverLevel.damageSources().playerAttack(owner), chainDamage);
                chained.invulnerableTime = 0;
                serverLevel.sendParticles(ParticleTypes.PORTAL,
                        chained.getX(), chained.getY() + 1, chained.getZ(),
                        5, 0.3, 0.3, 0.3, 0.02);

                // Apocalypse: propagate detonation to that target's own sigils
                if (apocalypse && !getSigils(chained).isEmpty()) {
                    detonateSigils(owner, chained, serverLevel, false, true);
                }
            }
        }

        // ── Apocalypse mini-detonations ───────────────────────────────────────
        if (apocalypse) {
            AABB miniArea = target.getBoundingBox().inflate(6);
            List<LivingEntity> miniTargets = serverLevel.getEntitiesOfClass(
                    LivingEntity.class, miniArea,
                    e -> e.isAlive() && !e.equals(target) && !e.equals(owner));

            for (LivingEntity mini : miniTargets) {
                mini.hurt(serverLevel.damageSources().magic(), baseDamage * 0.25f);
                mini.invulnerableTime = 0;
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        mini.getX(), mini.getY() + 1, mini.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }

    // =========================================================================
    //  ABILITY 1 — SIGIL CARVE  (main stack builder)
    // =========================================================================

    /**
     * Fast ray-cast hit → applies 1 random sigil (2 at stage 5+).
     * Surge doubles it.  Apocalypse applies all three types.
     * Cost: 150 soul essence.
     */
    public static void sigilCarve(Player player, Level level, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Sigil Reaper")) return;
        if (SoulCore.getSoulEssence(player) < 150) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 150);

        int     stage      = SoulCore.getAscensionStage(player);
        boolean surge      = inSurge(player);
        boolean apoc       = inApocalypse(player);
        boolean ignoreCap  = surge || apoc;
        int     toApply    = (stage >= 5) ? 2 : 1;
        if (surge) toApply *= 2;

        LivingEntity target = rayCastFirstEnemy(player, level, 10 + stage);
        if (target == null) return;

        // Beam particles
        Vec3 start     = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        Vec3 pos       = start;
        for (int i = 0; i < 10 + stage; i++) {
            pos = pos.add(direction);
            serverLevel.sendParticles(ParticleTypes.WITCH, pos.x, pos.y, pos.z, 1, 0.05, 0.05, 0.05, 0);
            if (pos.distanceTo(target.position()) < 1.5) break;
        }

        if (apoc) {
            addAllSigils(target, player);
        } else {
            addSigils(target, player, toApply, ignoreCap);
        }

        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                target.getX(), target.getY() + 1, target.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
        level.playSound(null, target.blockPosition(),
                SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 1.4f);

        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal(
                    "§5Sigils on §d" + target.getName().getString() + "§5: " + getSigils(target)));
        }
    }

    // =========================================================================
    //  ABILITY 3 — DETONATE SIGILS  (core burst)
    // =========================================================================

    /**
     * Detonates ALL sigils on the looked-at enemy.
     * Cost: 1000 soul essence.  Requires stage ≥ 2.
     */
    public static void detonateSigilsAbility(Player player, Level level, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Sigil Reaper")) return;
        if (SoulCore.getSoulEssence(player) < 1000) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        LivingEntity target = rayCastFirstEnemy(player, level, 14 + SoulCore.getAscensionStage(player));
        if (target == null) return;

        if (getSigils(target).isEmpty()) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo sigils on that target!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 1000);
        detonateSigils(player, target, serverLevel, false, inApocalypse(player));
    }

    // =========================================================================
    //  ABILITY 4 — SIGIL SURGE  (setup buff, 6 seconds)
    // =========================================================================

    /**
     * For 6 seconds: sigil application doubled, no sigil cap.
     * Downside: detonations deal -30% damage.
     * Cost: 1500 soul essence.  Requires stage ≥ 3.
     */
    public static void sigilSurge(Player player, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Sigil Reaper")) return;
        if (SoulCore.getSoulEssence(player) < 1500) return;
        if (SoulCore.getAscensionStage(player) < 3) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 1500);

        player.getPersistentData().putUUID(NBT_SURGE_OWNER, player.getUUID());
        player.getPersistentData().putInt(NBT_SURGE_TIMER, 120); // 6 seconds

        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
        serverLevel.playSound(null, player.blockPosition(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 0.8f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal("§dSigil Surge! §76s, no cap, but §c-30%§7 detonation damage."));
    }

    // =========================================================================
    //  ABILITY 5 — SPREAD MARK  (AOE sigil transfer)
    // =========================================================================

    /**
     * Copies ~50% of the target's sigils to all enemies within 8 blocks.
     * Cost: 1200 soul essence.  Requires stage ≥ 4.
     */
    public static void spreadMark(Player player, Level level, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Sigil Reaper")) return;
        if (SoulCore.getSoulEssence(player) < 1200) return;
        if (SoulCore.getAscensionStage(player) < 4) return;

        LivingEntity source = rayCastFirstEnemy(player, level, 12 + SoulCore.getAscensionStage(player));
        if (source == null) return;

        List<String> sourceSigils = getSigils(source);
        if (sourceSigils.isEmpty()) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo sigils to spread!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 1200);

        // Build spread subset (every other sigil ≈ 50%)
        List<String> toSpread = new ArrayList<>();
        for (int i = 0; i < sourceSigils.size(); i += 2) toSpread.add(sourceSigils.get(i));

        AABB spreadArea = source.getBoundingBox().inflate(8);
        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class, spreadArea,
                e -> e.isAlive() && !e.equals(source) && !e.equals(player));

        for (LivingEntity neighbor : nearby) {
            List<String> neighborSigils = getSigils(neighbor);
            neighbor.getPersistentData().putUUID(NBT_OWNER, player.getUUID());
            neighborSigils.addAll(toSpread);
            if (!inSurge(player) && neighborSigils.size() > MAX_SIGILS_NORMAL)
                neighborSigils = neighborSigils.subList(0, MAX_SIGILS_NORMAL);
            setSigils(neighbor, neighborSigils);

            serverLevel.sendParticles(ParticleTypes.WITCH,
                    neighbor.getX(), neighbor.getY() + 1, neighbor.getZ(), 6, 0.3, 0.3, 0.3, 0);
        }

        serverLevel.sendParticles(ParticleTypes.PORTAL,
                source.getX(), source.getY() + 1, source.getZ(), 20, 1.0, 0.5, 1.0, 0.05);
        level.playSound(null, source.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5Spread §d" + toSpread.size() + "§5 sigils to §d" + nearby.size() + "§5 enemies."));
    }

    // =========================================================================
    //  ABILITY 6 — DELAYED COLLAPSE  (3-second auto-detonation)
    // =========================================================================

    /**
     * Sets a 3-second fuse on the target.  Auto-detonates with +40% bonus.
     * You can keep stacking sigils during the window.
     * Cost: 2000 soul essence.  Requires stage ≥ 5.
     */
    public static void delayedCollapse(Player player, Level level, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Sigil Reaper")) return;
        if (SoulCore.getSoulEssence(player) < 2000) return;
        if (SoulCore.getAscensionStage(player) < 5) return;

        LivingEntity target = rayCastFirstEnemy(player, level, 14 + SoulCore.getAscensionStage(player));
        if (target == null) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 2000);

        target.getPersistentData().putInt(NBT_DELAY, 60); // 3 seconds = 60 ticks
        target.getPersistentData().putUUID(NBT_DELAY_OWNER, player.getUUID());

        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                target.getX(), target.getY() + 1, target.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
        level.playSound(null, target.blockPosition(),
                SoundEvents.WITCH_DRINK, SoundSource.HOSTILE, 1f, 0.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5Delayed Collapse! §dStack more sigils — detonates in §l3s§d with §l+40%§d bonus damage."));
    }

    // =========================================================================
    //  ABILITY 7 — APOCALYPSE SCRIPT  (ultimate, 10 seconds)
    // =========================================================================

    /**
     * 10-second transformation:
     *  - Every hit applies all 3 sigil types automatically
     *  - No sigil cap
     *  - Detonations chain infinitely between enemies
     *  - Each detonation spawns mini-detonations on nearby enemies
     * Cost: 6000 soul essence.  Requires stage ≥ 7.
     */
    public static void apocalypseScript(Player player, ServerLevel serverLevel) {
        if (!SoulCore.getAspect(player).equals("Sigil Reaper")) return;
        if (SoulCore.getSoulEssence(player) < 6000) return;
        if (SoulCore.getAscensionStage(player) < 7) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 6000);

        player.getPersistentData().putUUID(NBT_APOC_OWNER, player.getUUID());
        player.getPersistentData().putInt(NBT_APOC_TIMER, 200); // 10 seconds

        serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH,
                player.getX(), player.getY() + 1, player.getZ(), 40, 1.0, 1.0, 1.0, 0.08);
        serverLevel.playSound(null, player.blockPosition(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5f, 0.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal("§4§l☠ APOCALYPSE SCRIPT ☠ §r§5All sigils. No limits. No mercy."));
    }

    // =========================================================================
    //  PASSIVE HIT EVENT — Apocalypse auto-applies all sigils on every melee hit
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        /** During Apocalypse Script, every hit auto-applies all three sigil types. */
        @SubscribeEvent
        public static void onMeleeHit(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Sigil Reaper")) return;
            if (!inApocalypse(player)) return;

            LivingEntity victim = event.getEntity();
            addAllSigils(victim, player);

            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.DRAGON_BREATH,
                        victim.getX(), victim.getY() + 1, victim.getZ(),
                        3, 0.2, 0.2, 0.2, 0.02);
            }
        }

        /** Clean up sigil NBT when the entity dies to avoid ghost data. */
        @SubscribeEvent
        public static void onEntityDeath(LivingDeathEvent event) {
            LivingEntity dead = event.getEntity();
            dead.getPersistentData().remove(NBT_MARKS);
            dead.getPersistentData().remove(NBT_OWNER);
            dead.getPersistentData().remove(NBT_DELAY);
            dead.getPersistentData().remove(NBT_DELAY_OWNER);
        }
    }
}