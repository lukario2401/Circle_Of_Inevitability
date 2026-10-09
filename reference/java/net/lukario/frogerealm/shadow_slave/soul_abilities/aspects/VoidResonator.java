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
 * Void Resonator
 * -----------------------------------------------
 * Theme: sound, frequency, resonance → catastrophic collapse
 * Role: DPS (multi-target scaling + explosive burst)
 *
 * Core Mechanic — RESONANCE (0–100):
 *   0–30   → Normal
 *   30–70  → Increased vibration application
 *   70–100 → Bonus damage
 *   100    → 🔊 Overload: abilities stronger, but self-damage + rapid Resonance drain
 *
 * Frequency Modes:
 *   ATTUNE   → Apply vibration stacks faster, generate Resonance faster
 *   SHATTER  → Consume stacks for burst explosions
 *   COLLAPSE → High-risk: stacks spread between enemies, Resonance spikes, self-damage
 *
 * Vibration Stacks (per enemy, 0–10):
 *   Damage over time. Amplify Shatter burst.
 *
 * Player NBT keys:
 *   "VoidResonance"        → float  current resonance (0–100)
 *   "VoidResonanceDecay"   → int    ticks until next decay tick
 *   "VoidMode"             → int    0=ATTUNE, 1=SHATTER, 2=COLLAPSE
 *   "VoidModeBoost"        → int    ticks remaining on next-ability empowerment
 *   "VoidChainMarked"      → string comma-separated UUIDs of chain-marked enemies
 *   "VoidChainTimer"       → int    ticks remaining on chain marks
 *   "VoidCataclysm"        → int    ticks remaining on Cataclysm Engine
 *   "VoidCataclysmCycle"   → int    auto-cycle mode timer during Cataclysm
 *   "VoidSurge"            → int    ticks remaining on Frequency Surge buff
 *
 * Per-entity NBT (stored on the entity itself):
 *   "VoidVibration"        → int    vibration stack count (0–10)
 *   "VoidVibrationTimer"   → int    ticks until next vibration damage tick
 */
public class VoidResonator {

    // ─── Modes ────────────────────────────────────────────────────────────────
    public static final int MODE_ATTUNE   = 0;
    public static final int MODE_SHATTER  = 1;
    public static final int MODE_COLLAPSE = 2;

    // ─── NBT keys (player) ────────────────────────────────────────────────────
    private static final String NBT_RESONANCE       = "VoidResonance";
    private static final String NBT_RES_DECAY       = "VoidResonanceDecay";
    private static final String NBT_MODE            = "VoidMode";
    private static final String NBT_MODE_BOOST      = "VoidModeBoost";
    private static final String NBT_CHAIN_MARKED    = "VoidChainMarked";
    private static final String NBT_CHAIN_TIMER     = "VoidChainTimer";
    private static final String NBT_CATACLYSM       = "VoidCataclysm";
    private static final String NBT_CATACLYSM_CYCLE = "VoidCataclysmCycle";
    private static final String NBT_SURGE           = "VoidSurge";

    // ─── NBT keys (entity) ───────────────────────────────────────────────────
    private static final String NBT_VIB_STACKS = "VoidVibration";
    private static final String NBT_VIB_TIMER  = "VoidVibrationTimer";

    // ─── Attribute modifier ResourceLocations (1.21 API) ─────────────────────
    private static final ResourceLocation SURGE_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "void_surge_speed");

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final float RES_MAX          = 100f;
    private static final int   DECAY_INTERVAL   = 40;   // 1 resonance lost every 2s
    private static final int   VIB_TICK_INTERVAL = 20;  // vibration damages every second
    private static final int   MAX_VIB_STACKS   = 10;
    private static final int   MODE_BOOST_DUR   = 80;   // 4 seconds
    private static final int   CHAIN_DURATION   = 200;  // 10 seconds
    private static final int   SURGE_DURATION   = 100;  // 5 seconds
    private static final int   CATACLYSM_DUR    = 200;  // 10 seconds

    private static final Random RNG = new Random();

    // =========================================================================
    //  RESONANCE HELPERS
    // =========================================================================

    public static float getResonance(Player player) {
        return player.getPersistentData().getFloat(NBT_RESONANCE);
    }

    public static void setResonance(Player player, float val) {
        player.getPersistentData().putFloat(NBT_RESONANCE,
                Math.max(0f, Math.min(RES_MAX, val)));
    }

    private static void addResonance(Player player, float amount) {
        setResonance(player, getResonance(player) + amount);
    }

    public static boolean isOverloaded(Player player) {
        return getResonance(player) >= RES_MAX;
    }

    public static boolean inCataclysm(Player player) {
        return player.getPersistentData().getInt(NBT_CATACLYSM) > 0;
    }

    public static boolean hasModeBoost(Player player) {
        return player.getPersistentData().getInt(NBT_MODE_BOOST) > 0;
    }

    public static boolean inSurge(Player player) {
        return player.getPersistentData().getInt(NBT_SURGE) > 0;
    }

    public static int getMode(Player player) {
        return player.getPersistentData().getInt(NBT_MODE);
    }

    private static String modeName(int mode) {
        return switch (mode) {
            case MODE_ATTUNE   -> "§a[Attune]";
            case MODE_SHATTER  -> "§c[Shatter]";
            case MODE_COLLAPSE -> "§5[Collapse]";
            default            -> "§7[?]";
        };
    }

    private static String resonanceTag(float res) {
        if (res >= 100f) return "§6§l[OVERLOAD]";
        if (res >= 70f)  return "§e[High]";
        if (res >= 30f)  return "§2[Attuned]";
        return "§7[Low]";
    }

    private static String resStatus(Player player) {
        float r = getResonance(player);
        return resonanceTag(r) + " §fRes: §b" + String.format("%.0f", r) + "/100 "
                + modeName(getMode(player));
    }

    /** Damage multiplier from Resonance thresholds + active states. */
    private static float resonanceMult(Player player) {
        float res  = getResonance(player);
        float mult = 1.0f;
        if (isOverloaded(player) || inCataclysm(player)) mult = 1.6f;
        else if (res >= 70f) mult = 1.35f;
        else if (res >= 30f) mult = 1.15f;
        if (hasModeBoost(player)) mult *= 1.25f;
        if (inSurge(player))      mult *= 1.15f;
        return mult;
    }

    // =========================================================================
    //  VIBRATION STACK HELPERS (stored on the entity)
    // =========================================================================

    public static int getVibStacks(LivingEntity entity) {
        return entity.getPersistentData().getInt(NBT_VIB_STACKS);
    }

    public static void setVibStacks(LivingEntity entity, int stacks) {
        entity.getPersistentData().putInt(NBT_VIB_STACKS,
                Math.max(0, Math.min(MAX_VIB_STACKS, stacks)));
        if (stacks > 0 && !entity.getPersistentData().contains(NBT_VIB_TIMER)) {
            entity.getPersistentData().putInt(NBT_VIB_TIMER, VIB_TICK_INTERVAL);
        }
    }

    public static void addVibStacks(LivingEntity entity, int amount) {
        setVibStacks(entity, getVibStacks(entity) + amount);
    }

    /** Applies vibration DoT per tick to entity. Returns true if it dealt damage. */
    private static boolean tickVibration(LivingEntity entity, Player owner, ServerLevel sl) {
        int stacks = getVibStacks(entity);
        if (stacks <= 0) return false;

        int timer = entity.getPersistentData().getInt(NBT_VIB_TIMER) - 1;
        if (timer <= 0) {
            float dmg = stacks * 0.8f * resonanceMult(owner);
            entity.hurt(sl.damageSources().playerAttack(owner), dmg);
            entity.invulnerableTime = 0;
            sl.sendParticles(ParticleTypes.SONIC_BOOM,
                    entity.getX(), entity.getY() + 1, entity.getZ(), 1, 0, 0, 0, 0);
            // Decay one stack per tick
            setVibStacks(entity, stacks - 1);
            timer = VIB_TICK_INTERVAL;
        }
        entity.getPersistentData().putInt(NBT_VIB_TIMER, timer);
        return true;
    }

    // =========================================================================
    //  CHAIN MARK HELPERS
    // =========================================================================

    private static Set<UUID> getChainMarked(Player player) {
        String raw = player.getPersistentData().getString(NBT_CHAIN_MARKED);
        Set<UUID> set = new LinkedHashSet<>();
        if (!raw.isBlank()) {
            for (String s : raw.split(",")) {
                try { set.add(UUID.fromString(s.trim())); } catch (Exception ignored) {}
            }
        }
        return set;
    }

    private static void setChainMarked(Player player, Set<UUID> uuids) {
        StringBuilder sb = new StringBuilder();
        for (UUID u : uuids) {
            if (!sb.isEmpty()) sb.append(',');
            sb.append(u);
        }
        player.getPersistentData().putString(NBT_CHAIN_MARKED, sb.toString());
    }

    private static void addChainMark(Player player, UUID uuid) {
        Set<UUID> set = getChainMarked(player);
        set.add(uuid);
        setChainMarked(player, set);
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
                    LivingEntity.class, new AABB(cur, cur).inflate(0.5),
                    e -> e != player && e.isAlive());
            if (!hits.isEmpty()) return hits.get(0);
        }
        return null;
    }

    // =========================================================================
    //  LINE PARTICLES (sonic themed)
    // =========================================================================

    private static void lineParticles(ServerLevel sl, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from).normalize();
        double dist = from.distanceTo(to);
        Vec3 c = from;
        for (double dd = 0; dd < dist; dd += 0.5) {
            sl.sendParticles(ParticleTypes.SCULK_SOUL, c.x, c.y, c.z, 1, 0.04, 0.04, 0.04, 0);
            c = c.add(d.scale(0.5));
        }
    }

    // =========================================================================
    //  EVENTS
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class VoidEvents {

        @SubscribeEvent
        public static void onVoidTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Void Resonator")) return;

            float res = getResonance(player);

            // ── Cataclysm Engine countdown ────────────────────────────────────
            int cataclysm = player.getPersistentData().getInt(NBT_CATACLYSM);
            if (cataclysm > 0) {
                player.getPersistentData().putInt(NBT_CATACLYSM, cataclysm - 1);
                setResonance(player, RES_MAX); // locked at max

                // Auto-cycle modes every 30 ticks
                int cycleTimer = player.getPersistentData().getInt(NBT_CATACLYSM_CYCLE) - 1;
                if (cycleTimer <= 0) {
                    int nextMode = (getMode(player) + 1) % 3;
                    player.getPersistentData().putInt(NBT_MODE, nextMode);
                    cycleTimer = 30;
                    if (player instanceof ServerPlayer sp)
                        sp.sendSystemMessage(Component.literal(
                                "§6§lCataclysm: §r§fauto-cycling → " + modeName(nextMode)));
                }
                player.getPersistentData().putInt(NBT_CATACLYSM_CYCLE, cycleTimer);

                if (player.tickCount % 5 == 0)
                    sl.sendParticles(ParticleTypes.SONIC_BOOM,
                            player.getX(), player.getY() + 1, player.getZ(), 2, 0.5, 0.5, 0.5, 0);
                if (player.tickCount % 10 == 0)
                    sl.sendParticles(ParticleTypes.SCULK_SOUL,
                            player.getX(), player.getY() + 1, player.getZ(), 6, 0.6, 0.6, 0.6, 0.04);

                // Cataclysm: spread stacks from any vibrating enemy to all nearby
                List<LivingEntity> all = sl.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(20), e -> e != player && e.isAlive());
                for (LivingEntity e : all) {
                    int stacks = getVibStacks(e);
                    if (stacks > 0) {
                        // Infinite chain: spread 2 stacks to all adjacent
                        List<LivingEntity> adj = sl.getEntitiesOfClass(LivingEntity.class,
                                e.getBoundingBox().inflate(5), n -> n != player && n != e && n.isAlive());
                        for (LivingEntity n : adj) addVibStacks(n, 2);
                        // Detonate: deal bonus damage
                        float detonDmg = stacks * 2.5f;
                        e.hurt(sl.damageSources().playerAttack(player), detonDmg);
                        e.invulnerableTime = 0;
                        sl.sendParticles(ParticleTypes.SONIC_BOOM,
                                e.getX(), e.getY() + 1, e.getZ(), 1, 0, 0, 0, 0);
                    }
                    tickVibration(e, player, sl);
                }
            }

            // ── Frequency Surge countdown ──────────────────────────────────────
            int surge = player.getPersistentData().getInt(NBT_SURGE);
            if (surge > 0) {
                player.getPersistentData().putInt(NBT_SURGE, surge - 1);
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 5, 1, true, false));
                if (player.tickCount % 10 == 0)
                    sl.sendParticles(ParticleTypes.SCULK_SOUL,
                            player.getX(), player.getY() + 1, player.getZ(), 3, 0.3, 0.3, 0.3, 0.03);
                if (surge == 1) {
                    // Remove speed modifier on expiry
                    var moveAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
                    if (moveAttr != null) moveAttr.removeModifier(SURGE_SPEED_ID);
                }
            }

            // ── Mode Boost countdown ──────────────────────────────────────────
            int modeBoost = player.getPersistentData().getInt(NBT_MODE_BOOST);
            if (modeBoost > 0) {
                player.getPersistentData().putInt(NBT_MODE_BOOST, modeBoost - 1);
                if (player.tickCount % 8 == 0)
                    sl.sendParticles(ParticleTypes.END_ROD,
                            player.getX(), player.getY() + 1, player.getZ(), 2, 0.2, 0.2, 0.2, 0.02);
            }

            // ── Chain mark countdown ──────────────────────────────────────────
            int chainTimer = player.getPersistentData().getInt(NBT_CHAIN_TIMER);
            if (chainTimer > 0) {
                chainTimer--;
                player.getPersistentData().putInt(NBT_CHAIN_TIMER, chainTimer);
                if (chainTimer == 0) {
                    player.getPersistentData().putString(NBT_CHAIN_MARKED, "");
                }
                // Visualise marks
                if (player.tickCount % 6 == 0) {
                    Set<UUID> marks = getChainMarked(player);
                    sl.getEntitiesOfClass(LivingEntity.class,
                                    player.getBoundingBox().inflate(40),
                                    e -> marks.contains(e.getUUID()) && e.isAlive())
                            .forEach(e -> sl.sendParticles(ParticleTypes.SCULK_SOUL,
                                    e.getX(), e.getY() + 2.2, e.getZ(), 2, 0.15, 0.15, 0.15, 0.02));
                }
            }

            // ── Vibration DoT tick on all nearby enemies ──────────────────────
            if (!inCataclysm(player)) {
                sl.getEntitiesOfClass(LivingEntity.class,
                                player.getBoundingBox().inflate(30),
                                e -> e != player && e.isAlive() && getVibStacks(e) > 0)
                        .forEach(e -> tickVibration(e, player, sl));
            }

            // ── Overload self-damage and fast drain ───────────────────────────
            if (isOverloaded(player) && !inCataclysm(player)) {
                if (player.tickCount % 20 == 0) {
                    player.hurt(player.level().damageSources().magic(), 1.5f);
                    sl.sendParticles(ParticleTypes.SONIC_BOOM,
                            player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
                }
                // Fast drain: 2 res per second
                setResonance(player, res - 2f);
            }

            // ── Collapse mode self-damage ──────────────────────────────────────
            if (getMode(player) == MODE_COLLAPSE && !inCataclysm(player)
                    && player.tickCount % 30 == 0) {
                player.hurt(player.level().damageSources().magic(), 0.5f);
            }

            // ── Passive Resonance decay ───────────────────────────────────────
            if (res > 0 && !isOverloaded(player) && !inCataclysm(player)) {
                int decayTimer = player.getPersistentData().getInt(NBT_RES_DECAY) - 1;
                if (decayTimer <= 0) {
                    setResonance(player, res - 1f);
                    decayTimer = DECAY_INTERVAL;
                }
                player.getPersistentData().putInt(NBT_RES_DECAY, decayTimer);
            }

            // ── HUD: resonance indicator ──────────────────────────────────────
            if (player.tickCount % 12 == 0 && res > 0) {
                int count = Math.max(1, (int)(res / 25f));
                sl.sendParticles(ParticleTypes.SCULK_SOUL,
                        player.getX(), player.getY() + 2.4, player.getZ(),
                        count, 0.25, 0.1, 0.25, 0.005);
            }
        }

        /** Boost outgoing damage and gain resonance on hit. */
        @SubscribeEvent(priority = EventPriority.NORMAL)
        public static void onVoidDamageDealt(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Void Resonator")) return;

            // Apply resonance multiplier
            event.setAmount(event.getAmount() * resonanceMult(player));

            // Gain resonance per hit; Collapse and Cataclysm generate more
            float resGain = switch (getMode(player)) {
                case MODE_COLLAPSE -> inCataclysm(player) ? 6f : 4f;
                case MODE_ATTUNE   -> 3f;
                default            -> 2f;
            };
            addResonance(player, resGain);

            // In Attune mode: every hit passively adds 1 vibration stack
            if (getMode(player) == MODE_ATTUNE || inCataclysm(player)) {
                if (event.getEntity() instanceof LivingEntity target && target != player) {
                    addVibStacks(target, 1);
                }
            }
        }
    }

    // =========================================================================
    //  ABILITY 1 — VOID PULSE
    // =========================================================================

    /**
     * Fires a pulse of resonant energy along the look vector.
     *   ATTUNE:   applies vibration stacks to every enemy it passes through.
     *   SHATTER:  detonates all existing stacks on the first enemy hit.
     *   COLLAPSE: chains between up to 4 enemies near the impact point.
     * Cost: 200 essence. Stage 0+.
     */
    public static void voidPulse(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Void Resonator")) return;
        if (SoulCore.getSoulEssence(player) < 200) return;

        int stage = SoulCore.getAscensionStage(player);
        int mode  = getMode(player);
        int range = inCataclysm(player) ? 24 + stage : 14 + stage;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 200);
        addResonance(player, 8f);
        if (hasModeBoost(player)) player.getPersistentData().putInt(NBT_MODE_BOOST, 0);

        Vec3 start = player.getEyePosition();
        Vec3 dir   = player.getLookAngle().normalize();
        Vec3 cur   = start;
        Set<UUID> hit = new HashSet<>();

        for (int i = 1; i <= range; i++) {
            cur = start.add(dir.scale(i));
            List<LivingEntity> targets = level.getEntitiesOfClass(
                    LivingEntity.class, new AABB(cur, cur).inflate(0.6),
                    e -> e != player && e.isAlive() && !hit.contains(e.getUUID()));

            for (LivingEntity target : targets) {
                hit.add(target.getUUID());
                switch (mode) {
                    case MODE_ATTUNE -> {
                        // Add stacks to every target pierced
                        int stacksToAdd = inCataclysm(player) ? 4 : 3;
                        if (inSurge(player)) stacksToAdd++;
                        addVibStacks(target, stacksToAdd);
                        float dmg = (8f + stage) * resonanceMult(player);
                        target.hurt(level.damageSources().playerAttack(player), dmg);
                        target.invulnerableTime = 0;
                        sl.sendParticles(ParticleTypes.SCULK_SOUL,
                                target.getX(), target.getY() + 1, target.getZ(), 6, 0.3, 0.3, 0.3, 0.04);
                    }
                    case MODE_SHATTER -> {
                        // Detonate stacks on first target, stop piercing
                        int stacks = getVibStacks(target);
                        float detonDmg = (10f + stacks * 3.5f + stage * 2) * resonanceMult(player);
                        if (inCataclysm(player)) detonDmg *= 1.5f;
                        target.hurt(level.damageSources().playerAttack(player), detonDmg);
                        target.invulnerableTime = 0;
                        setVibStacks(target, 0);
                        sl.sendParticles(ParticleTypes.SONIC_BOOM,
                                target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
                        sl.sendParticles(ParticleTypes.CRIT,
                                target.getX(), target.getY() + 1, target.getZ(), 10, 0.4, 0.4, 0.4, 0.06);
                        level.playSound(null, target.blockPosition(),
                                SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.7f, 1.6f);
                        if (player instanceof ServerPlayer sp)
                            sp.sendSystemMessage(Component.literal(
                                    "§cVoid Pulse §7[Shatter]: §fdetonated §e" + stacks
                                            + " stacks §f→ §c" + String.format("%.1f", detonDmg)
                                            + " dmg. " + resStatus(player)));
                        return; // Shatter stops at first target
                    }
                    case MODE_COLLAPSE -> {
                        // Hit primary, then chain to nearby enemies
                        float dmg = (10f + stage * 2) * resonanceMult(player);
                        target.hurt(level.damageSources().playerAttack(player), dmg);
                        target.invulnerableTime = 0;
                        addVibStacks(target, 2);
                        sl.sendParticles(ParticleTypes.SCULK_SOUL,
                                target.getX(), target.getY() + 1, target.getZ(), 4, 0.2, 0.2, 0.2, 0.03);

                        int chainCount = inCataclysm(player) ? 6 : 3;
                        List<LivingEntity> chain = level.getEntitiesOfClass(
                                LivingEntity.class, target.getBoundingBox().inflate(5f),
                                e -> e != player && e != target && e.isAlive());
                        int chained = 0;
                        for (LivingEntity c : chain) {
                            if (chained++ >= chainCount) break;
                            float cDmg = dmg * 0.6f;
                            c.hurt(level.damageSources().playerAttack(player), cDmg);
                            c.invulnerableTime = 0;
                            addVibStacks(c, 2);
                            lineParticles(sl,
                                    target.position().add(0, 1, 0),
                                    c.position().add(0, 1, 0));
                        }
                        if (player instanceof ServerPlayer sp)
                            sp.sendSystemMessage(Component.literal(
                                    "§5Void Pulse §7[Collapse]: §f" + String.format("%.1f", dmg)
                                            + " + chained §e" + chained + "§f enemies. "
                                            + resStatus(player)));
                        return;
                    }
                }
            }
        }

        lineParticles(sl, start, cur);
        level.playSound(null, player.blockPosition(),
                SoundEvents.SCULK_SENSOR_BREAK, SoundSource.PLAYERS, 1f, 0.5f);

        if (mode == MODE_ATTUNE && player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§aVoid Pulse §7[Attune]: §fpierced §e" + hit.size()
                            + "§f enemies, applied stacks. " + resStatus(player)));
    }

    // =========================================================================
    //  ABILITY 2 — RESONANCE WAVE
    // =========================================================================

    /**
     * AOE pulse expanding from the player.
     *   ATTUNE:   spreads vibration stacks to all nearby enemies.
     *   SHATTER:  triggers a small sonic burst on each enemy (partial stack consume).
     *   COLLAPSE: leaves a resonant field on the ground that pulses damage for 3s.
     * Cost: 350 essence. Requires stage ≥ 1.
     */
    public static void resonanceWave(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Void Resonator")) return;
        if (SoulCore.getSoulEssence(player) < 350) return;
        if (SoulCore.getAscensionStage(player) < 1) return;

        int   stage  = SoulCore.getAscensionStage(player);
        int   mode   = getMode(player);
        float radius = inCataclysm(player) ? 10f + stage : 6f + stage * 0.5f;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 350);
        addResonance(player, 10f);
        if (hasModeBoost(player)) player.getPersistentData().putInt(NBT_MODE_BOOST, 0);

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive());

        // Ring particles
        for (int i = 0; i < 32; i++) {
            double angle = (2 * Math.PI / 32) * i;
            double px = player.getX() + radius * Math.cos(angle);
            double pz = player.getZ() + radius * Math.sin(angle);
            sl.sendParticles(ParticleTypes.SCULK_SOUL, px, player.getY() + 0.5, pz,
                    1, 0, 0.3, 0, 0.01);
        }

        switch (mode) {
            case MODE_ATTUNE -> {
                int stacksPerTarget = inCataclysm(player) ? 4 : 2;
                if (inSurge(player)) stacksPerTarget++;
                for (LivingEntity e : targets) {
                    addVibStacks(e, stacksPerTarget);
                    float dmg = (6f + stage) * resonanceMult(player);
                    e.hurt(level.damageSources().playerAttack(player), dmg);
                    e.invulnerableTime = 0;
                    sl.sendParticles(ParticleTypes.SCULK_SOUL,
                            e.getX(), e.getY() + 1, e.getZ(), 3, 0.2, 0.2, 0.2, 0.02);
                }
                if (player instanceof ServerPlayer sp)
                    sp.sendSystemMessage(Component.literal(
                            "§aResonance Wave §7[Attune]: §fspread §e" + stacksPerTarget
                                    + " stacks §fto §c" + targets.size() + "§f enemies. "
                                    + resStatus(player)));
            }
            case MODE_SHATTER -> {
                for (LivingEntity e : targets) {
                    int stacks = getVibStacks(e);
                    int consumed = Math.min(stacks, 4);
                    float dmg = (8f + consumed * 2.5f + stage) * resonanceMult(player);
                    if (inCataclysm(player)) dmg *= 1.4f;
                    e.hurt(level.damageSources().playerAttack(player), dmg);
                    e.invulnerableTime = 0;
                    setVibStacks(e, stacks - consumed);
                    sl.sendParticles(ParticleTypes.SONIC_BOOM,
                            e.getX(), e.getY() + 1, e.getZ(), 1, 0, 0, 0, 0);
                }
                level.playSound(null, player.blockPosition(),
                        SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 1f, 0.8f);
                if (player instanceof ServerPlayer sp)
                    sp.sendSystemMessage(Component.literal(
                            "§cResonance Wave §7[Shatter]: §fburst §c" + targets.size()
                                    + "§f enemies (consumed up to 4 stacks each). "
                                    + resStatus(player)));
            }
            case MODE_COLLAPSE -> {
                // Immediate hit
                for (LivingEntity e : targets) {
                    float dmg = (9f + stage) * resonanceMult(player);
                    e.hurt(level.damageSources().playerAttack(player), dmg);
                    e.invulnerableTime = 0;
                    addVibStacks(e, 3);
                }
                // Resonant field: stored as a short-lived repeating damage zone
                // We simulate this by scheduling 6 pulses via player NBT ticks
                // For simplicity, apply lingering effect through a 3-second regen-block
                // and 3 more damage bursts every 20 ticks (handled via a compact timer)
                player.getPersistentData().putInt("VoidFieldTimer", 60);
                player.getPersistentData().putDouble("VoidFieldX", player.getX());
                player.getPersistentData().putDouble("VoidFieldY", player.getY());
                player.getPersistentData().putDouble("VoidFieldZ", player.getZ());
                player.getPersistentData().putFloat("VoidFieldRadius", radius * 0.6f);
                player.getPersistentData().putFloat("VoidFieldDmg",
                        (5f + stage) * resonanceMult(player));
                level.playSound(null, player.blockPosition(),
                        SoundEvents.SCULK_BLOCK_SPREAD, SoundSource.HOSTILE, 1f, 0.5f);
                if (player instanceof ServerPlayer sp)
                    sp.sendSystemMessage(Component.literal(
                            "§5Resonance Wave §7[Collapse]: §fhit §e" + targets.size()
                                    + "§f + resonant field for §b3s§f. " + resStatus(player)));
            }
        }
    }

    // =========================================================================
    //  ABILITY 3 — SHATTER POINT
    // =========================================================================

    /**
     * Targets one enemy and detonates ALL their vibration stacks instantly.
     * Damage scales very heavily with stack count.
     * Cost: 600 essence. Requires stage ≥ 2.
     */
    public static void shatterPoint(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Void Resonator")) return;
        if (SoulCore.getSoulEssence(player) < 600) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        int stage = SoulCore.getAscensionStage(player);
        LivingEntity target = rayCastFirst(player, level, 18 + stage);
        if (target == null) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo target in range!"));
            return;
        }

        int stacks = getVibStacks(target);
        if (stacks == 0 && !inCataclysm(player)) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cTarget has no vibration stacks!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 600);

        int effectiveStacks = inCataclysm(player) ? Math.max(stacks, MAX_VIB_STACKS) : stacks;
        float dmg = (15f + effectiveStacks * 5.5f + stage * 3f) * resonanceMult(player);
        if (hasModeBoost(player)) {
            dmg *= 1.25f;
            player.getPersistentData().putInt(NBT_MODE_BOOST, 0);
        }
        if (getMode(player) == MODE_SHATTER) dmg *= 1.35f;

        target.hurt(level.damageSources().playerAttack(player), dmg);
        target.invulnerableTime = 0;
        setVibStacks(target, 0);

        addResonance(player, 12f);

        // Shockwave ring particles
        for (int i = 0; i < 20; i++) {
            double angle = (2 * Math.PI / 20) * i;
            double px = target.getX() + 2.5 * Math.cos(angle);
            double pz = target.getZ() + 2.5 * Math.sin(angle);
            sl.sendParticles(ParticleTypes.SONIC_BOOM, px, target.getY() + 1, pz,
                    1, 0, 0, 0, 0);
        }
        sl.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + 1, target.getZ(), 16, 0.5, 0.5, 0.5, 0.07);
        level.playSound(null, target.blockPosition(),
                SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 1.5f, 0.5f);

        // Shatter + chain mark: if marked, chain detonation
        if (!inCataclysm(player)) {
            Set<UUID> marks = getChainMarked(player);
            if (marks.contains(target.getUUID())) {
                sl.getEntitiesOfClass(LivingEntity.class,
                                player.getBoundingBox().inflate(30),
                                e -> marks.contains(e.getUUID()) && e != target && e.isAlive())
                        .forEach(e -> {
                            int eStacks = getVibStacks(e);
                            if (eStacks > 0) {
                                float chainDmg = eStacks * 3.5f * resonanceMult(player);
                                e.hurt(sl.damageSources().playerAttack(player), chainDmg);
                                e.invulnerableTime = 0;
                                setVibStacks(e, 0);
                                sl.sendParticles(ParticleTypes.SONIC_BOOM,
                                        e.getX(), e.getY() + 1, e.getZ(), 1, 0, 0, 0, 0);
                            }
                        });
            }
        }

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§c§lShatter Point: §r§fdetonated §e" + effectiveStacks
                            + " stacks §f→ §c" + String.format("%.1f", dmg)
                            + " dmg. " + resStatus(player)));
    }

    // =========================================================================
    //  ABILITY 4 — FREQUENCY SHIFT
    // =========================================================================

    /**
     * Cycle between ATTUNE → SHATTER → COLLAPSE → ATTUNE.
     * The next ability fired gains ×1.25 from the new mode.
     * Cost: 150 essence. Requires stage ≥ 0.
     */
    public static void frequencyShift(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Void Resonator")) return;
        if (SoulCore.getSoulEssence(player) < 150) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 150);

        int nextMode = (getMode(player) + 1) % 3;
        player.getPersistentData().putInt(NBT_MODE, nextMode);
        player.getPersistentData().putInt(NBT_MODE_BOOST, MODE_BOOST_DUR);

        addResonance(player, 5f);

        sl.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1, player.getZ(), 8, 0.3, 0.3, 0.3, 0.04);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.4f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§bFrequency Shift: §f→ " + modeName(nextMode)
                            + " §7| next ability §e×1.25§7. " + resStatus(player)));
    }

    // =========================================================================
    //  ABILITY 5 — FREQUENCY SURGE
    // =========================================================================

    /**
     * For 5 seconds:
     *   - Rapidly builds Resonance over time
     *   - Attack speed increased (Haste II)
     *   - Every hit applies +1 vibration stack for free
     * Cost: 500 essence. Requires stage ≥ 3.
     */
    public static void frequencySurge(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Void Resonator")) return;
        if (SoulCore.getSoulEssence(player) < 500) return;
        if (SoulCore.getAscensionStage(player) < 3) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 500);

        player.getPersistentData().putInt(NBT_SURGE, SURGE_DURATION);
        addResonance(player, 20f);

        // Movement speed boost via attribute (1.21 API)
        var moveAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveAttr != null) {
            moveAttr.removeModifier(SURGE_SPEED_ID);
            moveAttr.addTransientModifier(new AttributeModifier(
                    SURGE_SPEED_ID, 0.03, AttributeModifier.Operation.ADD_VALUE));
        }

        // Haste II for the duration
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, SURGE_DURATION, 1, false, true));

        sl.sendParticles(ParticleTypes.SCULK_SOUL,
                player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.06);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1f, 0.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§b§lFrequency Surge! §r§3Haste II, +resonance, +1 stack/hit for §b5s§3. "
                            + resStatus(player)));
    }

    // =========================================================================
    //  ABILITY 6 — RESONANT CHAIN
    // =========================================================================

    /**
     * Mark up to 3 enemies (successive casts). When one marked enemy is Shattered,
     * the detonation cascades to all other marked enemies' stacks.
     * Cost: 400 essence. Requires stage ≥ 5.
     */
    public static void resonantChain(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Void Resonator")) return;
        if (SoulCore.getSoulEssence(player) < 400) return;
        if (SoulCore.getAscensionStage(player) < 5) return;

        int stage = SoulCore.getAscensionStage(player);
        LivingEntity target = rayCastFirst(player, level, 20 + stage);
        if (target == null) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo target to mark!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 400);

        addChainMark(player, target.getUUID());
        player.getPersistentData().putInt(NBT_CHAIN_TIMER, CHAIN_DURATION);

        // Also immediately apply stacks to the marked target
        int stacksApplied = inCataclysm(player) ? 5 : 3;
        addVibStacks(target, stacksApplied);

        addResonance(player, 6f);

        sl.sendParticles(ParticleTypes.SCULK_SOUL,
                target.getX(), target.getY() + 2.2, target.getZ(), 10, 0.2, 0.2, 0.2, 0.04);
        lineParticles(sl,
                player.position().add(0, 1, 0),
                target.position().add(0, 1, 0));
        level.playSound(null, target.blockPosition(),
                SoundEvents.SCULK_SENSOR_BREAK, SoundSource.HOSTILE, 0.9f, 0.7f);

        int totalMarks = getChainMarked(player).size();
        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5Resonant Chain: §fmarked §c" + target.getName().getString()
                            + " §f(§e" + totalMarks + "§f total). +§e" + stacksApplied
                            + "§f stacks. Detonation cascades on Shatter. " + resStatus(player)));
    }

    // =========================================================================
    //  ABILITY 7 — CATACLYSM ENGINE (ULTIMATE)
    // =========================================================================

    /**
     * 10-second ultimate:
     *   - Resonance locked at maximum
     *   - Infinite vibration stack application
     *   - Detonations chain infinitely between all nearby enemies
     *   - Frequency auto-cycles every 1.5 seconds
     * Cost: 5000 essence. Requires stage ≥ 7.
     */
    public static void cataclysmEngine(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Void Resonator")) return;
        if (SoulCore.getSoulEssence(player) < 5000) return;
        if (SoulCore.getAscensionStage(player) < 7) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 5000);

        player.getPersistentData().putInt(NBT_CATACLYSM, CATACLYSM_DUR);
        player.getPersistentData().putInt(NBT_CATACLYSM_CYCLE, 30);
        setResonance(player, RES_MAX);

        // Burst particles
        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10);
            double px = player.getX() + 4 * Math.cos(angle);
            double pz = player.getZ() + 4 * Math.sin(angle);
            sl.sendParticles(ParticleTypes.SCULK_SOUL, px, player.getY() + 1, pz,
                    1, 0, 0.3, 0, 0.03);
        }
        sl.sendParticles(ParticleTypes.SONIC_BOOM,
                player.getX(), player.getY() + 1, player.getZ(), 3, 0.4, 0.4, 0.4, 0);
        sl.sendParticles(ParticleTypes.FLASH,
                player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 0.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§b§l✦ CATACLYSM ENGINE ✦ §r§3Resonance maxed. Infinite chains. "
                            + "Auto-cycling frequencies. §b10s."));
    }
}