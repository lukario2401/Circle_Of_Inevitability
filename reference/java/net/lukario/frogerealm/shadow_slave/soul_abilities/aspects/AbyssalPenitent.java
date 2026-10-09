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
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * Abyssal Penitent
 * -----------------------------------------------
 * Theme: sacrifice, madness, forbidden whispers, self-corruption
 * Role: DPS (high-risk burst + corruption spread)
 *
 * Core Mechanic — CORRUPTION (0–100):
 *   0–30  → stable
 *   30–70 → bonus damage
 *   70–90 → abilities enhanced
 *   90–100→ ☠ Madness: massive boost, random targeting, self-damage
 *
 * Secondary Mechanic — WHISPERS:
 *   Build as Corruption rises. At high levels abilities may cast twice,
 *   misfire, or change behaviour unpredictably.
 *
 * Player NBT keys:
 *   "APCorruption"        → float  0–100
 *   "APCorruptionDecay"   → int    ticks until next decay
 *   "APWhispers"          → int    0–10 whisper stacks
 *   "APWhisperTimer"      → int    ticks until next whisper tick
 *   "APNextEmpowered"     → bool   next ability is empowered (Sacrificial Offering)
 *   "APChainsBound"       → string comma-separated UUIDs of chained enemies
 *   "APChainsTimer"       → int    ticks remaining on Chains of Repentance
 *   "APInsightTimer"      → int    ticks remaining on Forbidden Insight channel
 *   "APDescentActive"     → int    ticks remaining on Descent into the Abyss
 *   "APDescentDmgDealt"   → float  total damage dealt during Descent (for final explosion)
 *   "APSelfDmgCooldown"   → int    ticks until next madness self-damage tick
 */
public class AbyssalPenitent {

    // ─── NBT keys ─────────────────────────────────────────────────────────────
    private static final String NBT_CORRUPTION      = "APCorruption";
    private static final String NBT_DECAY           = "APCorruptionDecay";
    private static final String NBT_WHISPERS        = "APWhispers";
    private static final String NBT_WHISPER_TIMER   = "APWhisperTimer";
    private static final String NBT_NEXT_EMPOWERED  = "APNextEmpowered";
    private static final String NBT_CHAINS_BOUND    = "APChainsBound";
    private static final String NBT_CHAINS_TIMER    = "APChainsTimer";
    private static final String NBT_INSIGHT_TIMER   = "APInsightTimer";
    private static final String NBT_DESCENT_ACTIVE  = "APDescentActive";
    private static final String NBT_DESCENT_DMG     = "APDescentDmgDealt";
    private static final String NBT_SELF_DMG_CD     = "APSelfDmgCooldown";

    // ─── Attribute modifier ResourceLocations (1.21 API) ─────────────────────
    private static final ResourceLocation INSIGHT_DMG_ID =
            ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "ap_insight_damage");

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final float CORRUPTION_MAX       = 100f;
    private static final int   DECAY_INTERVAL       = 80;   // 1 corruption every 4s
    private static final int   WHISPER_INTERVAL     = 30;   // whisper tick every 1.5s
    private static final int   WHISPERS_MAX         = 10;
    private static final int   CHAINS_DURATION      = 120;  // 6 seconds
    private static final int   INSIGHT_DURATION     = 60;   // 3 seconds channel
    private static final int   DESCENT_DURATION     = 240;  // 12 seconds
    private static final int   SELF_DMG_INTERVAL    = 40;   // madness self-dmg every 2s
    private static final float MADNESS_THRESHOLD    = 90f;
    private static final float ENHANCED_THRESHOLD   = 70f;
    private static final float BONUS_DMG_THRESHOLD  = 30f;

    private static final Random RNG = new Random();

    // =========================================================================
    //  CORRUPTION HELPERS
    // =========================================================================

    public static float getCorruption(Player player) {
        return player.getPersistentData().getFloat(NBT_CORRUPTION);
    }

    public static void setCorruption(Player player, float val) {
        player.getPersistentData().putFloat(NBT_CORRUPTION,
                Math.max(0f, Math.min(CORRUPTION_MAX, val)));
    }

    public static void addCorruption(Player player, float amount) {
        setCorruption(player, getCorruption(player) + amount);
    }

    public static boolean inMadness(Player player) {
        return getCorruption(player) >= MADNESS_THRESHOLD || inDescent(player);
    }

    public static boolean isEnhanced(Player player) {
        return getCorruption(player) >= ENHANCED_THRESHOLD || inDescent(player);
    }

    public static boolean inDescent(Player player) {
        return player.getPersistentData().getInt(NBT_DESCENT_ACTIVE) > 0;
    }

    public static boolean isNextEmpowered(Player player) {
        return player.getPersistentData().getBoolean(NBT_NEXT_EMPOWERED);
    }

    public static int getWhispers(Player player) {
        return player.getPersistentData().getInt(NBT_WHISPERS);
    }

    private static void addWhispers(Player player, int amount) {
        int cur = getWhispers(player);
        player.getPersistentData().putInt(NBT_WHISPERS,
                Math.max(0, Math.min(WHISPERS_MAX, cur + amount)));
    }

    /** Core damage multiplier from Corruption level. */
    public static float corruptionMult(Player player) {
        float c = getCorruption(player);
        float mult;
        if (inDescent(player))        mult = 3.0f;
        else if (c >= MADNESS_THRESHOLD)   mult = 2.4f;
        else if (c >= ENHANCED_THRESHOLD)  mult = 1.75f;
        else if (c >= BONUS_DMG_THRESHOLD) mult = 1.30f;
        else                               mult = 1.00f;
        if (isNextEmpowered(player)) mult *= 1.40f;
        return mult;
    }

    private static String corruptionTag(Player player) {
        float c = getCorruption(player);
        if (inDescent(player))             return "§4§l[☠ DESCENT]";
        if (c >= MADNESS_THRESHOLD)        return "§5§l[☠ MADNESS]";
        if (c >= ENHANCED_THRESHOLD)       return "§c[Enhanced]";
        if (c >= BONUS_DMG_THRESHOLD)      return "§6[Corrupted]";
        return "§7[Stable]";
    }

    private static String corruptionStatus(Player player) {
        float c = getCorruption(player);
        return corruptionTag(player) + " §fCorruption: §b"
                + String.format("%.0f", c) + "/100"
                + (getWhispers(player) > 0 ? " §8[Whispers: " + getWhispers(player) + "]" : "");
    }

    // =========================================================================
    //  WHISPER EFFECTS
    // =========================================================================

    /**
     * Roll a whisper effect. Called when an ability is used with whispers active.
     * Returns true if the whisper caused a misfire (caller should skip normal effect).
     */
    private static boolean rollWhisper(Player player, Level level, ServerLevel sl,
                                       LivingEntity target) {
        int whispers = getWhispers(player);
        if (whispers == 0) return false;

        float roll = RNG.nextFloat();
        // Higher whispers = higher chance of whisper proc
        float procChance = whispers * 0.08f; // 8% per whisper stack

        if (roll > procChance) return false;

        int effect = RNG.nextInt(4);
        switch (effect) {
            case 0 -> {
                // Double cast: deal bonus damage
                if (target != null) {
                    float bonusDmg = 8f * corruptionMult(player);
                    target.hurt(level.damageSources().playerAttack(player), bonusDmg);
                    target.invulnerableTime = 0;
                    sl.sendParticles(ParticleTypes.WITCH,
                            target.getX(), target.getY() + 1, target.getZ(),
                            6, 0.2, 0.2, 0.2, 0.04);
                }
                if (player instanceof ServerPlayer sp)
                    sp.sendSystemMessage(Component.literal(
                            "§8§o[Whisper]: The voices echo your strike..."));
            }
            case 1 -> {
                // Misfire: deal self-damage
                player.hurt(player.level().damageSources().magic(), 3f);
                addCorruption(player, 5f);
                sl.sendParticles(ParticleTypes.WITCH,
                        player.getX(), player.getY() + 1, player.getZ(),
                        8, 0.3, 0.3, 0.3, 0.04);
                if (player instanceof ServerPlayer sp)
                    sp.sendSystemMessage(Component.literal(
                            "§8§o[Whisper]: The power turns against you."));
                return true; // misfire
            }
            case 2 -> {
                // Random nearby target struck instead
                List<LivingEntity> nearby = level.getEntitiesOfClass(
                        LivingEntity.class, player.getBoundingBox().inflate(10),
                        e -> e != player && e.isAlive());
                if (!nearby.isEmpty()) {
                    LivingEntity rand = nearby.get(RNG.nextInt(nearby.size()));
                    float randDmg = 12f * corruptionMult(player);
                    rand.hurt(level.damageSources().playerAttack(player), randDmg);
                    rand.invulnerableTime = 0;
                    sl.sendParticles(ParticleTypes.WITCH,
                            rand.getX(), rand.getY() + 1, rand.getZ(),
                            8, 0.3, 0.3, 0.3, 0.04);
                    if (player instanceof ServerPlayer sp)
                        sp.sendSystemMessage(Component.literal(
                                "§8§o[Whisper]: Something else caught their attention..."));
                }
            }
            case 3 -> {
                // Behaviour change: apply weakness to nearby enemies
                level.getEntitiesOfClass(LivingEntity.class,
                                player.getBoundingBox().inflate(6),
                                e -> e != player && e.isAlive())
                        .forEach(e -> e.addEffect(new MobEffectInstance(
                                MobEffects.WEAKNESS, 60, 1, false, false)));
                if (player instanceof ServerPlayer sp)
                    sp.sendSystemMessage(Component.literal(
                            "§8§o[Whisper]: Forbidden knowledge seeps into the air..."));
            }
        }
        addWhispers(player, -1);
        return false;
    }

    // =========================================================================
    //  CHAINS HELPERS
    // =========================================================================

    private static Set<UUID> getChainsBound(Player player) {
        String raw = player.getPersistentData().getString(NBT_CHAINS_BOUND);
        Set<UUID> set = new LinkedHashSet<>();
        if (!raw.isBlank()) {
            for (String s : raw.split(",")) {
                try { set.add(UUID.fromString(s.trim())); }
                catch (Exception ignored) {}
            }
        }
        return set;
    }

    private static void setChainsBound(Player player, Set<UUID> uuids) {
        StringBuilder sb = new StringBuilder();
        for (UUID u : uuids) {
            if (!sb.isEmpty()) sb.append(',');
            sb.append(u);
        }
        player.getPersistentData().putString(NBT_CHAINS_BOUND, sb.toString());
    }

    private static void addChainBound(Player player, UUID uuid) {
        Set<UUID> set = getChainsBound(player);
        set.add(uuid);
        setChainsBound(player, set);
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
                    LivingEntity.class, new AABB(cur, cur).inflate(0.55),
                    e -> e != player && e.isAlive());
            if (!hits.isEmpty()) return hits.get(0);
        }
        return null;
    }

    // ─── Corruption particle helper ───────────────────────────────────────────
    private static void corruptionBurst(ServerLevel sl, Vec3 pos, int count) {
        sl.sendParticles(ParticleTypes.WITCH,
                pos.x, pos.y + 1, pos.z, count, 0.3, 0.3, 0.3, 0.04);
        sl.sendParticles(ParticleTypes.SOUL,
                pos.x, pos.y + 1, pos.z, count / 2, 0.2, 0.2, 0.2, 0.02);
    }

    // =========================================================================
    //  EVENTS
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class AbyssalPenitentEvents {

        @SubscribeEvent
        public static void onAbyssalPenitentTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Abyssal Penitent")) return;

            float corruption = getCorruption(player);

            // ── Descent into the Abyss countdown ─────────────────────────────
            int descent = player.getPersistentData().getInt(NBT_DESCENT_ACTIVE);
            if (descent > 0) {
                player.getPersistentData().putInt(NBT_DESCENT_ACTIVE, descent - 1);
                setCorruption(player, CORRUPTION_MAX);

                // Random self-damage during descent
                int selfCd = player.getPersistentData().getInt(NBT_SELF_DMG_CD) - 1;
                if (selfCd <= 0) {
                    player.hurt(player.level().damageSources().magic(), 2.5f);
                    selfCd = SELF_DMG_INTERVAL;
                    sl.sendParticles(ParticleTypes.WITCH,
                            player.getX(), player.getY() + 1, player.getZ(),
                            5, 0.2, 0.2, 0.2, 0.03);
                }
                player.getPersistentData().putInt(NBT_SELF_DMG_CD, selfCd);

                if (player.tickCount % 5 == 0) {
                    sl.sendParticles(ParticleTypes.SOUL,
                            player.getX(), player.getY() + 1, player.getZ(),
                            4, 0.5, 0.5, 0.5, 0.05);
                    sl.sendParticles(ParticleTypes.WITCH,
                            player.getX(), player.getY() + 1, player.getZ(),
                            3, 0.4, 0.4, 0.4, 0.03);
                }

                // Final explosion when Descent ends
                if (descent == 1) {
                    float totalDmg = player.getPersistentData().getFloat(NBT_DESCENT_DMG);
                    float explDmg  = Math.min(totalDmg * 0.35f, 120f);
                    float explRad  = 6f + SoulCore.getAscensionStage(player);
                    List<LivingEntity> explTargets = sl.getEntitiesOfClass(
                            LivingEntity.class, player.getBoundingBox().inflate(explRad),
                            e -> e != player && e.isAlive());
                    for (LivingEntity e : explTargets) {
                        e.hurt(sl.damageSources().playerAttack(player), explDmg);
                        e.invulnerableTime = 0;
                        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                                e.getX(), e.getY() + 1, e.getZ(), 1, 0, 0, 0, 0);
                    }
                    for (int i = 0; i < 36; i++) {
                        double angle = Math.toRadians(i * 10);
                        sl.sendParticles(ParticleTypes.WITCH,
                                player.getX() + explRad * Math.cos(angle),
                                player.getY() + 0.5,
                                player.getZ() + explRad * Math.sin(angle),
                                1, 0, 0.3, 0, 0.03);
                    }
                    sl.playSound(null, player.blockPosition(),
                            SoundEvents.WITHER_DEATH, SoundSource.PLAYERS, 1.5f, 0.4f);
                    player.getPersistentData().putFloat(NBT_DESCENT_DMG, 0f);
                    if (player instanceof ServerPlayer sp)
                        sp.sendSystemMessage(Component.literal(
                                "§5§l☠ DESCENT ENDED §r§f— Final explosion: §c"
                                        + String.format("%.1f", explDmg)
                                        + " dmg to §e" + explTargets.size() + "§f targets."));
                }
            }

            // ── Forbidden Insight channel countdown ───────────────────────────
            int insight = player.getPersistentData().getInt(NBT_INSIGHT_TIMER);
            if (insight > 0) {
                player.getPersistentData().putInt(NBT_INSIGHT_TIMER, insight - 1);
                addCorruption(player, 1.5f);
                addWhispers(player, 0); // whispers tick handled separately
                player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SLOWDOWN, 5, 10, true, false));
                if (player.tickCount % 6 == 0)
                    sl.sendParticles(ParticleTypes.WITCH,
                            player.getX(), player.getY() + 1, player.getZ(),
                            4, 0.4, 0.4, 0.4, 0.03);
                if (insight == 1) {
                    // Remove damage boost on channel end
                    var dmgAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
                    if (dmgAttr != null) dmgAttr.removeModifier(INSIGHT_DMG_ID);
                    if (player instanceof ServerPlayer sp)
                        sp.sendSystemMessage(Component.literal(
                                "§cForbidden Insight §7channel ended. "
                                        + corruptionStatus(player)));
                }
            }

            // ── Chains of Repentance countdown ────────────────────────────────
            int chains = player.getPersistentData().getInt(NBT_CHAINS_TIMER);
            if (chains > 0) {
                chains--;
                player.getPersistentData().putInt(NBT_CHAINS_TIMER, chains);
                addCorruption(player, 0.4f); // passive gain while chains active

                Set<UUID> bound = getChainsBound(player);
                if (!bound.isEmpty()) {
                    sl.getEntitiesOfClass(LivingEntity.class,
                                    player.getBoundingBox().inflate(30),
                                    e -> bound.contains(e.getUUID()) && e.isAlive())
                            .forEach(e -> {
                                e.addEffect(new MobEffectInstance(
                                        MobEffects.MOVEMENT_SLOWDOWN, 25, 2, false, false));
                                if (player.tickCount % 6 == 0)
                                    sl.sendParticles(ParticleTypes.WITCH,
                                            e.getX(), e.getY() + 1, e.getZ(),
                                            2, 0.2, 0.2, 0.2, 0.02);
                            });
                }
                if (chains == 0) {
                    player.getPersistentData().putString(NBT_CHAINS_BOUND, "");
                }
            }

            // ── Madness self-damage (outside Descent) ─────────────────────────
            if (inMadness(player) && !inDescent(player)) {
                int selfCd = player.getPersistentData().getInt(NBT_SELF_DMG_CD) - 1;
                if (selfCd <= 0) {
                    player.hurt(player.level().damageSources().magic(), 1.5f);
                    selfCd = SELF_DMG_INTERVAL;
                    sl.sendParticles(ParticleTypes.WITCH,
                            player.getX(), player.getY() + 1, player.getZ(),
                            3, 0.2, 0.2, 0.2, 0.02);
                }
                player.getPersistentData().putInt(NBT_SELF_DMG_CD, selfCd);
            }

            // ── Whisper tick ──────────────────────────────────────────────────
            if (corruption >= BONUS_DMG_THRESHOLD) {
                int wTimer = player.getPersistentData().getInt(NBT_WHISPER_TIMER) - 1;
                if (wTimer <= 0) {
                    int gain = corruption >= MADNESS_THRESHOLD ? 2 : 1;
                    addWhispers(player, gain);
                    wTimer = WHISPER_INTERVAL;
                    if (getWhispers(player) >= 5 && player instanceof ServerPlayer sp)
                        sp.sendSystemMessage(Component.literal(
                                "§8§o[The whispers grow louder...]"));
                }
                player.getPersistentData().putInt(NBT_WHISPER_TIMER, wTimer);
            }

            // ── Corruption decay ──────────────────────────────────────────────
            if (corruption > 0 && !inDescent(player)) {
                int decayTimer = player.getPersistentData().getInt(NBT_DECAY) - 1;
                if (decayTimer <= 0) {
                    setCorruption(player, corruption - 1f);
                    decayTimer = DECAY_INTERVAL;
                }
                player.getPersistentData().putInt(NBT_DECAY, decayTimer);
            }

            // ── HUD particles ─────────────────────────────────────────────────
            if (player.tickCount % 12 == 0 && corruption > 10f) {
                int count = Math.max(1, (int)(corruption / 25f));
                sl.sendParticles(ParticleTypes.WITCH,
                        player.getX(), player.getY() + 2.4, player.getZ(),
                        count, 0.2, 0.1, 0.2, 0.005);
            }
        }

        /** Gain Corruption when taking damage. Also track descent damage dealt. */
        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void onAbyssalPenitentDamageTaken(LivingHurtEvent event) {
            if (!(event.getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Abyssal Penitent")) return;

            // Build corruption from pain
            addCorruption(player, event.getAmount() * 0.4f);

            // Chained enemies take damage when they attack the player
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                Set<UUID> bound = getChainsBound(player);
                if (bound.contains(attacker.getUUID())) {
                    float reflect = event.getAmount() * 0.5f * corruptionMult(player);
                    attacker.hurt(player.level().damageSources().magic(), reflect);
                    attacker.invulnerableTime = 0;
                    if (player.level() instanceof ServerLevel sl)
                        sl.sendParticles(ParticleTypes.WITCH,
                                attacker.getX(), attacker.getY() + 1, attacker.getZ(),
                                5, 0.2, 0.2, 0.2, 0.03);
                }
            }
        }

        /** Apply corruption multiplier to outgoing damage. Track descent total. */
        @SubscribeEvent(priority = EventPriority.NORMAL)
        public static void onAbyssalPenitentDamageDealt(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Abyssal Penitent")) return;

            float mult = corruptionMult(player);
            event.setAmount(event.getAmount() * mult);

            // Track total damage during Descent for final explosion
            if (inDescent(player)) {
                float prev = player.getPersistentData().getFloat(NBT_DESCENT_DMG);
                player.getPersistentData().putFloat(NBT_DESCENT_DMG,
                        prev + event.getAmount());
            }
        }
    }

    // =========================================================================
    //  ABILITY 1 — ABYSSAL PENITENT PROFANE CUT
    // =========================================================================

    /**
     * Fast melee strike. Applies a corruption bleed and raises your own Corruption.
     * Madness Bonus: hits up to 4 random nearby enemies unpredictably.
     * Cost: 200 soul-essence. Stage 0+.
     */
    public static void abyssalPenitentProfaneCut(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Abyssal Penitent")) return;
        if (SoulCore.getSoulEssence(player) < 200) return;

        int ascStage = SoulCore.getAscensionStage(player);

        // Gather targets — madness hits random nearby, otherwise raycasts
        List<LivingEntity> targets = new ArrayList<>();
        if (inMadness(player)) {
            List<LivingEntity> nearby = level.getEntitiesOfClass(
                    LivingEntity.class, player.getBoundingBox().inflate(6f + ascStage),
                    e -> e != player && e.isAlive());
            Collections.shuffle(nearby, RNG);
            int hitCount = Math.min(nearby.size(), inDescent(player) ? 6 : 4);
            targets.addAll(nearby.subList(0, hitCount));
        } else {
            LivingEntity t = rayCastFirst(player, level, 6 + ascStage);
            if (t != null) targets.add(t);
        }

        if (targets.isEmpty()) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo target in range!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 200);
        addCorruption(player, 8f);
        addWhispers(player, 1);

        boolean misfired = rollWhisper(player, level, sl,
                targets.isEmpty() ? null : targets.get(0));
        if (misfired) return;

        float dmg = (10f + ascStage * 2f) * corruptionMult(player);
        if (isNextEmpowered(player)) {
            dmg *= 1.4f;
            player.getPersistentData().putBoolean(NBT_NEXT_EMPOWERED, false);
        }

        for (LivingEntity e : targets) {
            e.hurt(level.damageSources().playerAttack(player), dmg);
            e.invulnerableTime = 0;
            // Corruption bleed: slow + weakness
            e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, true));
            corruptionBurst(sl, e.position(), 5);
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 0.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§8Profane Cut: §c" + String.format("%.1f", dmg)
                            + " dmg §7× §e" + targets.size() + "§7. "
                            + corruptionStatus(player)));
    }

    // =========================================================================
    //  ABILITY 2 — ABYSSAL PENITENT SACRIFICIAL OFFERING
    // =========================================================================

    /**
     * Sacrifice 20% of current HP to massively build Corruption and
     * empower the next ability (×1.4 damage).
     * Enhanced (70+ Corruption): sacrifice 15% instead of 20%.
     * Cost: 300 soul-essence. Requires stage ≥ 1.
     */
    public static void abyssalPenitentSacrificialOffering(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Abyssal Penitent")) return;
        if (SoulCore.getSoulEssence(player) < 300) return;
        if (SoulCore.getAscensionStage(player) < 1) return;

        float hpCost = player.getHealth()
                * (isEnhanced(player) ? 0.15f : 0.20f);
        if (player.getHealth() <= hpCost + 2f) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§cNot enough HP to offer — too close to death!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 300);

        player.hurt(player.level().damageSources().magic(), hpCost);
        float corrGain = 20f + hpCost * 1.5f;
        if (inDescent(player)) corrGain *= 1.5f;
        addCorruption(player, corrGain);
        addWhispers(player, 2);

        player.getPersistentData().putBoolean(NBT_NEXT_EMPOWERED, true);

        corruptionBurst(sl, player.position(), 16);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.WITHER_HURT, SoundSource.PLAYERS, 1f, 0.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§cSacrificial Offering: §f-§e" + String.format("%.1f", hpCost)
                            + " HP §f→ §b+" + String.format("%.0f", corrGain)
                            + " Corruption§f. Next ability §d×1.4§f empowered. "
                            + corruptionStatus(player)));
    }

    // =========================================================================
    //  ABILITY 3 — ABYSSAL PENITENT BLASPHEMOUS BURST
    // =========================================================================

    /**
     * AOE explosion centred on the player scaling heavily with Corruption.
     * Madness Bonus: explosion repeats 1–3 more times at random nearby positions.
     * Cost: 600 soul-essence. Requires stage ≥ 2.
     */
    public static void abyssalPenitentBlasphemousBurst(Player player, Level level,
                                                       ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Abyssal Penitent")) return;
        if (SoulCore.getSoulEssence(player) < 600) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        int   ascStage = SoulCore.getAscensionStage(player);
        float corruption = getCorruption(player);
        float radius   = 5f + (corruption / 20f) + ascStage * 0.5f;
        float dmg      = (12f + corruption * 0.4f + ascStage * 3f) * corruptionMult(player);

        if (isNextEmpowered(player)) {
            dmg *= 1.4f;
            player.getPersistentData().putBoolean(NBT_NEXT_EMPOWERED, false);
        }

        boolean misfired = rollWhisper(player, level, sl, null);
        if (misfired) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 600);
        addCorruption(player, 12f);
        addWhispers(player, 2);

        // Primary explosion
        explodeBurst(player, level, sl, player.position(), radius, dmg);

        // Madness: repeat explosions
        if (inMadness(player)) {
            int repeats = inDescent(player) ? 3 : 1 + RNG.nextInt(2);
            for (int i = 0; i < repeats; i++) {
                double ox = (RNG.nextDouble() - 0.5) * radius * 1.5;
                double oz = (RNG.nextDouble() - 0.5) * radius * 1.5;
                Vec3 offset = player.position().add(ox, 0, oz);
                float repeatDmg = dmg * 0.6f;
                explodeBurst(player, level, sl, offset, radius * 0.7f, repeatDmg);
            }
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§5§l[MADNESS] §r§cBlasphemous Burst echoed §e" + repeats + "§cx!"));
        }

        sl.playSound(null, player.blockPosition(),
                SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.2f, 0.4f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5Blasphemous Burst: §c" + String.format("%.1f", dmg)
                            + " dmg r" + String.format("%.1f", radius) + ". "
                            + corruptionStatus(player)));
    }

    private static void explodeBurst(Player player, Level level, ServerLevel sl,
                                     Vec3 centre, float radius, float dmg) {
        level.getEntitiesOfClass(LivingEntity.class,
                        new AABB(centre, centre).inflate(radius),
                        e -> e != player && e.isAlive())
                .forEach(e -> {
                    e.hurt(level.damageSources().playerAttack(player), dmg);
                    e.invulnerableTime = 0;
                    Vec3 push = e.position().subtract(centre).normalize().scale(0.5);
                    e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.25, push.z));
                    sl.sendParticles(ParticleTypes.WITCH,
                            e.getX(), e.getY() + 1, e.getZ(),
                            5, 0.2, 0.2, 0.2, 0.03);
                });
        for (int i = 0; i < 20; i++) {
            double angle = Math.toRadians(i * 18);
            sl.sendParticles(ParticleTypes.WITCH,
                    centre.x + radius * Math.cos(angle),
                    centre.y + 0.5,
                    centre.z + radius * Math.sin(angle),
                    1, 0, 0.3, 0, 0.02);
        }
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                centre.x, centre.y + 1, centre.z, 1, 0, 0, 0, 0);
    }

    // =========================================================================
    //  ABILITY 4 — ABYSSAL PENITENT CHAINS OF REPENTANCE
    // =========================================================================

    /**
     * Binds target enemy for 6 seconds. Bound enemies:
     *   - Take damage whenever they attack you (50% of their hit reflected)
     *   - Are slowed
     *   - Passively build your Corruption
     * Madness Bonus: spreads to 2 additional nearby enemies.
     * Cost: 400 soul-essence. Requires stage ≥ 3.
     */
    public static void abyssalPenitentChainsOfRepentance(Player player, Level level,
                                                         ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Abyssal Penitent")) return;
        if (SoulCore.getSoulEssence(player) < 400) return;
        if (SoulCore.getAscensionStage(player) < 3) return;

        int ascStage = SoulCore.getAscensionStage(player);
        LivingEntity primary = rayCastFirst(player, level, 14 + ascStage);
        if (primary == null) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo target in range!"));
            return;
        }

        boolean misfired = rollWhisper(player, level, sl, primary);
        if (misfired) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 400);
        addCorruption(player, 10f);
        addWhispers(player, 1);

        if (isNextEmpowered(player))
            player.getPersistentData().putBoolean(NBT_NEXT_EMPOWERED, false);

        // Bind primary
        addChainBound(player, primary.getUUID());
        player.getPersistentData().putInt(NBT_CHAINS_TIMER, CHAINS_DURATION);

        int bound = 1;

        // Madness: spread to nearby
        if (inMadness(player)) {
            int spreadCount = inDescent(player) ? 3 : 2;
            List<LivingEntity> nearby = level.getEntitiesOfClass(
                    LivingEntity.class, primary.getBoundingBox().inflate(6f),
                    e -> e != player && e != primary && e.isAlive());
            int added = 0;
            for (LivingEntity e : nearby) {
                if (added++ >= spreadCount) break;
                addChainBound(player, e.getUUID());
                bound++;
                // Chain particle line
                Vec3 from = primary.position().add(0, 1, 0);
                Vec3 to   = e.position().add(0, 1, 0);
                Vec3 d    = to.subtract(from).normalize();
                double dist = from.distanceTo(to);
                Vec3 c = from;
                for (double dd = 0; dd < dist; dd += 0.5) {
                    sl.sendParticles(ParticleTypes.WITCH, c.x, c.y, c.z,
                            1, 0.03, 0.03, 0.03, 0);
                    c = c.add(d.scale(0.5));
                }
            }
        }

        corruptionBurst(sl, primary.position(), 10);
        level.playSound(null, primary.blockPosition(),
                SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 0.8f, 0.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5Chains of Repentance: §fbound §e" + bound
                            + "§f enemies for §b6s§f. Damage reflected on hit. "
                            + corruptionStatus(player)));
    }

    // =========================================================================
    //  ABILITY 5 — ABYSSAL PENITENT FORBIDDEN INSIGHT
    // =========================================================================

    /**
     * Channel for 3 seconds, immobilising yourself.
     * During channel: +6 attack damage, +1.5 Corruption per tick.
     * After channel: attack bonus persists for 4 seconds.
     * Risk: you cannot move. Whispers build rapidly.
     * Cost: 500 soul-essence. Requires stage ≥ 5.
     */
    public static void abyssalPenitentForbiddenInsight(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Abyssal Penitent")) return;
        if (SoulCore.getSoulEssence(player) < 500) return;
        if (SoulCore.getAscensionStage(player) < 5) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 500);

        player.getPersistentData().putInt(NBT_INSIGHT_TIMER, INSIGHT_DURATION);
        addCorruption(player, 15f);
        addWhispers(player, 3);

        if (isNextEmpowered(player))
            player.getPersistentData().putBoolean(NBT_NEXT_EMPOWERED, false);

        // Apply attack damage boost for channel + 4s post-channel
        var dmgAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmgAttr != null) {
            dmgAttr.removeModifier(INSIGHT_DMG_ID);
            float boost = 6f + (inMadness(player) ? 4f : 0f)
                    + SoulCore.getAscensionStage(player);
            dmgAttr.addTransientModifier(new AttributeModifier(
                    INSIGHT_DMG_ID, boost, AttributeModifier.Operation.ADD_VALUE));
        }

        // Slow to a halt
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, INSIGHT_DURATION, 10, false, false));

        corruptionBurst(sl, player.position(), 20);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.7f, 1.4f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§8§lForbidden Insight: §r§fchanneling §b3s§f — "
                            + "§c+atk dmg§f, §bCorruption building§f. Cannot move. "
                            + corruptionStatus(player)));
    }

    // =========================================================================
    //  ABILITY 6 — ABYSSAL PENITENT ABYSSAL RELEASE
    // =========================================================================

    /**
     * Dump Corruption as a directed burst of dark energy at a target.
     * Damage = base + (corruption × 0.6). Then resets Corruption by 50%.
     * Tradeoff: you lose your power spike after the burst.
     * Cost: 700 soul-essence. Requires stage ≥ 5.
     */
    public static void abyssalPenitentAbyssalRelease(Player player, Level level,
                                                     ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Abyssal Penitent")) return;
        if (SoulCore.getSoulEssence(player) < 700) return;
        if (SoulCore.getAscensionStage(player) < 5) return;

        int ascStage = SoulCore.getAscensionStage(player);
        float corruption = getCorruption(player);

        LivingEntity target = rayCastFirst(player, level, 16 + ascStage);
        if (target == null) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo target in range!"));
            return;
        }

        boolean misfired = rollWhisper(player, level, sl, target);
        if (misfired) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 700);

        float dmg = (20f + corruption * 0.6f + ascStage * 4f) * corruptionMult(player);

        if (isNextEmpowered(player)) {
            dmg *= 1.4f;
            player.getPersistentData().putBoolean(NBT_NEXT_EMPOWERED, false);
        }

        target.hurt(level.damageSources().playerAttack(player), dmg);
        target.invulnerableTime = 0;

        // Splash 30% to nearby enemies
        float splashDmg = dmg * 0.30f;
        level.getEntitiesOfClass(LivingEntity.class,
                        target.getBoundingBox().inflate(4f),
                        e -> e != player && e != target && e.isAlive())
                .forEach(e -> {
                    e.hurt(level.damageSources().playerAttack(player), splashDmg);
                    e.invulnerableTime = 0;
                    sl.sendParticles(ParticleTypes.WITCH,
                            e.getX(), e.getY() + 1, e.getZ(),
                            4, 0.2, 0.2, 0.2, 0.03);
                });

        // Reset corruption by 50%
        float newCorr = inDescent(player) ? corruption : corruption * 0.5f;
        setCorruption(player, newCorr);
        // Whispers also halved
        player.getPersistentData().putInt(NBT_WHISPERS,
                getWhispers(player) / 2);

        // Beam particle line
        Vec3 from = player.getEyePosition();
        Vec3 to   = target.position().add(0, 1, 0);
        Vec3 d    = to.subtract(from).normalize();
        double dist = from.distanceTo(to);
        Vec3 c = from;
        for (double dd = 0; dd < dist; dd += 0.4) {
            sl.sendParticles(ParticleTypes.WITCH, c.x, c.y, c.z,
                    1, 0.04, 0.04, 0.04, 0);
            c = c.add(d.scale(0.4));
        }
        corruptionBurst(sl, target.position(), 14);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, target.blockPosition(),
                SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 1f, 0.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5Abyssal Release: §c" + String.format("%.1f", dmg)
                            + " dmg §7(Corruption released)§f. "
                            + "Corruption reset §b→ §e"
                            + String.format("%.0f", newCorr) + "§f. "
                            + corruptionStatus(player)));
    }

    // =========================================================================
    //  ABILITY 7 — ABYSSAL PENITENT DESCENT INTO THE ABYSS
    // =========================================================================

    /**
     * 12-second ultimate:
     *   - Instantly enters Madness (Corruption locked at 100)
     *   - All abilities gain chaotic amplification
     *   - Self-damage every 2s (unavoidable)
     *   - Whispers fire constantly
     *   - On end: massive explosion based on total damage dealt during Descent
     * Cost: 5000 soul-essence. Requires stage ≥ 7.
     */
    public static void abyssalPenitentDescentIntoTheAbyss(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Abyssal Penitent")) return;
        if (SoulCore.getSoulEssence(player) < 5000) return;
        if (SoulCore.getAscensionStage(player) < 7) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 5000);

        player.getPersistentData().putInt(NBT_DESCENT_ACTIVE, DESCENT_DURATION);
        player.getPersistentData().putFloat(NBT_DESCENT_DMG, 0f);
        player.getPersistentData().putInt(NBT_SELF_DMG_CD, SELF_DMG_INTERVAL);
        setCorruption(player, CORRUPTION_MAX);
        player.getPersistentData().putInt(NBT_WHISPERS, WHISPERS_MAX);

        // Grand visual
        for (int i = 0; i < 40; i++) {
            double angle = Math.toRadians(i * 9);
            sl.sendParticles(ParticleTypes.WITCH,
                    player.getX() + 4 * Math.cos(angle),
                    player.getY() + 1,
                    player.getZ() + 4 * Math.sin(angle),
                    1, 0, 0.5, 0, 0.03);
        }
        sl.sendParticles(ParticleTypes.SOUL,
                player.getX(), player.getY() + 1, player.getZ(),
                30, 0.8, 0.8, 0.8, 0.06);
        sl.sendParticles(ParticleTypes.FLASH,
                player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(), 2, 0.4, 0.3, 0.4, 0);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.5f, 0.3f);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1f, 0.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5§l☠ DESCENT INTO THE ABYSS ☠ §r§fMadness locked. "
                            + "×3 damage. Chaos reigns. §b12s. "
                            + "§7[Final explosion awaits.]"));
    }
}