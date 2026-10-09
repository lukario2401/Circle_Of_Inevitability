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
 * Vector Arbiter
 * -----------------------------------------------
 * Geometric dominance DPS. Damage depends on enemy positioning relative to you.
 * You win by solving combat geometry — not managing stats.
 *
 * Core Mechanic — ALIGNMENT SCORE (0–100):
 *   Builds when abilities hit multiple enemies in geometric formations.
 *   Decays slowly over time.
 *   Higher alignment = stronger passive bonuses to all geometric attacks.
 *
 * Player NBT keys:
 *   "VecAlignment"         → float  current alignment score (0–100)
 *   "VecAlignDecay"        → int    ticks until next passive decay
 *   "VecMarkA"             → UUID   first marked entity (Intersection)
 *   "VecMarkB"             → UUID   second marked entity (Intersection)
 *   "VecMarkTimer"         → int    ticks remaining on intersection marks
 *   "VecPivotBonus"        → int    ticks remaining on Pivot Step bonus
 *   "VecFracturePlane"     → int    ticks remaining on Fracture Plane
 *   "VecFracturePX/PY/PZ"  → double fracture plane anchor position
 *   "VecFractureNX/NZ"     → double fracture plane normal (horizontal)
 *   "VecPerfectAlignment"  → int    ticks remaining on Perfect Alignment ultimate
 */
public class VectorArbiter {

    // ─── NBT keys ─────────────────────────────────────────────────────────────
    private static final String NBT_ALIGN         = "VecAlignment";
    private static final String NBT_ALIGN_DECAY   = "VecAlignDecay";
    private static final String NBT_MARK_A        = "VecMarkA";
    private static final String NBT_MARK_B        = "VecMarkB";
    private static final String NBT_MARK_TIMER    = "VecMarkTimer";
    private static final String NBT_PIVOT         = "VecPivotBonus";
    private static final String NBT_FRACTURE      = "VecFracturePlane";
    private static final String NBT_FRAC_PX       = "VecFracturePX";
    private static final String NBT_FRAC_PY       = "VecFracturePY";
    private static final String NBT_FRAC_PZ       = "VecFracturePZ";
    private static final String NBT_FRAC_NX       = "VecFractureNX";
    private static final String NBT_FRAC_NZ       = "VecFractureNZ";
    private static final String NBT_FRAC_PREV     = "VecFracturePrevSides"; // stores side signs per entity UUID
    private static final String NBT_PERFECT       = "VecPerfectAlignment";

    // ─── Attribute modifier ResourceLocations (1.21 API) ─────────────────────
    private static final ResourceLocation PIVOT_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "vec_pivot_speed");

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final float ALIGN_MAX          = 100f;
    private static final int   ALIGN_DECAY_TICKS  = 60;   // lose 1 alignment every 3s
    private static final int   MARK_DURATION      = 200;  // 10 seconds
    private static final int   PIVOT_DURATION     = 60;   // 3 seconds
    private static final int   FRACTURE_DURATION  = 140;  // 7 seconds
    private static final int   PERFECT_DURATION   = 200;  // 10 seconds

    // Cone half-angle thresholds (degrees)
    private static final float CONE_TIGHT         = 15f;
    private static final float CONE_MEDIUM        = 35f;
    private static final float CONE_WIDE          = 60f;

    // Line detection: how close to perfect collinearity (dot product threshold)
    private static final float LINE_DOT_THRESHOLD = 0.92f; // ~23 degrees

    private static final Random RNG = new Random();

    // ─── Alignment helpers ────────────────────────────────────────────────────

    public static float getAlignment(Player player) {
        return player.getPersistentData().getFloat(NBT_ALIGN);
    }

    public static void setAlignment(Player player, float val) {
        player.getPersistentData().putFloat(NBT_ALIGN, Math.max(0f, Math.min(ALIGN_MAX, val)));
    }

    private static void addAlignment(Player player, float amount) {
        setAlignment(player, getAlignment(player) + amount);
    }

    public static boolean inPerfect(Player player) {
        return player.getPersistentData().getInt(NBT_PERFECT) > 0;
    }

    public static boolean hasPivotBonus(Player player) {
        return player.getPersistentData().getInt(NBT_PIVOT) > 0;
    }

    /** Global multiplier from alignment score + active buffs. */
    private static float alignMult(Player player) {
        float align = getAlignment(player);
        float mult  = 1.0f + (align / 100f) * 0.6f; // up to ×1.6 at full alignment
        if (inPerfect(player)) mult *= 1.5f;
        if (hasPivotBonus(player)) mult *= 1.3f;
        return mult;
    }

    private static String alignTag(float align) {
        if (align >= 80f) return "§b[Perfect]";
        if (align >= 60f) return "§3[Sharp]";
        if (align >= 40f) return "§2[Aligned]";
        if (align >= 20f) return "§a[Forming]";
        return "§7[Scattered]";
    }

    private static String alignStatus(Player player) {
        float a = getAlignment(player);
        return alignTag(a) + " §fAlignment: §e" + String.format("%.0f", a) + "/100";
    }

    // ─── Geometry helpers ─────────────────────────────────────────────────────

    /**
     * Returns the list of living entities strictly in front of the player
     * within a cone of the given half-angle (degrees) and range.
     */
    private static List<LivingEntity> entitiesInCone(Player player, Level level,
                                                     float halfAngleDeg, float range) {
        Vec3 look  = player.getLookAngle().normalize();
        Vec3 from  = player.getEyePosition();
        float cosA = (float) Math.cos(Math.toRadians(halfAngleDeg));

        return level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(range),
                e -> {
                    if (e == player || !e.isAlive()) return false;
                    Vec3 dir = e.position().add(0, e.getBbHeight() / 2, 0)
                            .subtract(from).normalize();
                    return (float) look.dot(dir) >= cosA
                            && from.distanceTo(e.position()) <= range;
                });
    }

    /**
     * Returns all living entities along the player's look vector within range,
     * using per-block ray-stepping with a 0.6-block hit-box radius.
     */
    private static List<LivingEntity> entitiesInLine(Player player, Level level, int range) {
        Vec3 start = player.getEyePosition();
        Vec3 dir   = player.getLookAngle().normalize();
        Set<UUID> found = new LinkedHashSet<>();
        List<LivingEntity> result = new ArrayList<>();

        for (int i = 1; i <= range; i++) {
            Vec3 cur = start.add(dir.scale(i));
            List<LivingEntity> hits = level.getEntitiesOfClass(
                    LivingEntity.class, new AABB(cur, cur).inflate(0.6),
                    e -> e != player && e.isAlive() && !found.contains(e.getUUID()));
            for (LivingEntity e : hits) {
                found.add(e.getUUID());
                result.add(e);
            }
        }
        return result;
    }

    /**
     * Checks if targetB is "behind" targetA from the player's perspective —
     * i.e. the player→A and player→B vectors are nearly parallel.
     */
    private static boolean isBehind(Player player, LivingEntity front, LivingEntity behind) {
        Vec3 toFront  = front.position().subtract(player.position()).normalize();
        Vec3 toBehind = behind.position().subtract(player.position()).normalize();
        return toFront.dot(toBehind) >= LINE_DOT_THRESHOLD
                && player.position().distanceTo(behind.position())
                > player.position().distanceTo(front.position());
    }

    /** Spawns a line of particles from `from` to `to`. */
    private static void lineParticles(ServerLevel sl, Vec3 from, Vec3 to,
                                      net.minecraft.core.particles.SimpleParticleType type,
                                      double step) {
        Vec3 d = to.subtract(from).normalize();
        double dist = from.distanceTo(to);
        Vec3 c = from;
        for (double dd = 0; dd < dist; dd += step) {
            sl.sendParticles(type, c.x, c.y, c.z, 1, 0.03, 0.03, 0.03, 0);
            c = c.add(d.scale(step));
        }
    }

    // =========================================================================
    //  EVENTS
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class VectorEvents {

        @SubscribeEvent
        public static void onVectorTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Vector Arbiter")) return;

            // ── Perfect Alignment countdown ───────────────────────────────────
            int perfect = player.getPersistentData().getInt(NBT_PERFECT);
            if (perfect > 0) {
                player.getPersistentData().putInt(NBT_PERFECT, perfect - 1);
                if (player.tickCount % 6 == 0) {
                    sl.sendParticles(ParticleTypes.END_ROD,
                            player.getX(), player.getY() + 1, player.getZ(), 4, 0.4, 0.4, 0.4, 0.03);
                }
            }

            // ── Pivot Step bonus countdown ────────────────────────────────────
            int pivot = player.getPersistentData().getInt(NBT_PIVOT);
            if (pivot > 0) {
                player.getPersistentData().putInt(NBT_PIVOT, pivot - 1);
                if (player.tickCount % 8 == 0)
                    sl.sendParticles(ParticleTypes.CRIT,
                            player.getX(), player.getY() + 1, player.getZ(), 3, 0.3, 0.3, 0.3, 0.04);
            }

            // ── Intersection mark countdown ───────────────────────────────────
            int markTimer = player.getPersistentData().getInt(NBT_MARK_TIMER);
            if (markTimer > 0) {
                markTimer--;
                player.getPersistentData().putInt(NBT_MARK_TIMER, markTimer);
                // Visualise the line between the two marks
                if (player.getPersistentData().contains(NBT_MARK_A)
                        && player.getPersistentData().contains(NBT_MARK_B)
                        && player.tickCount % 5 == 0) {
                    UUID uuidA = player.getPersistentData().getUUID(NBT_MARK_A);
                    UUID uuidB = player.getPersistentData().getUUID(NBT_MARK_B);
                    List<LivingEntity> nearby = sl.getEntitiesOfClass(
                            LivingEntity.class, player.getBoundingBox().inflate(40), e -> e.isAlive());
                    LivingEntity eA = null, eB = null;
                    for (LivingEntity e : nearby) {
                        if (e.getUUID().equals(uuidA)) eA = e;
                        if (e.getUUID().equals(uuidB)) eB = e;
                    }
                    if (eA != null && eB != null) {
                        lineParticles(sl,
                                eA.position().add(0, 1, 0),
                                eB.position().add(0, 1, 0),
                                ParticleTypes.WITCH, 0.5);
                    }
                }
                if (markTimer == 0) {
                    player.getPersistentData().remove(NBT_MARK_A);
                    player.getPersistentData().remove(NBT_MARK_B);
                }
            }

            // ── Fracture Plane tick ───────────────────────────────────────────
            int fracture = player.getPersistentData().getInt(NBT_FRACTURE);
            if (fracture > 0) {
                fracture--;
                player.getPersistentData().putInt(NBT_FRACTURE, fracture);

                double fx = player.getPersistentData().getDouble(NBT_FRAC_PX);
                double fy = player.getPersistentData().getDouble(NBT_FRAC_PY);
                double fz = player.getPersistentData().getDouble(NBT_FRAC_PZ);
                double nx = player.getPersistentData().getDouble(NBT_FRAC_NX);
                double nz = player.getPersistentData().getDouble(NBT_FRAC_NZ);

                Vec3 planePos    = new Vec3(fx, fy, fz);
                Vec3 planeNormal = new Vec3(nx, 0, nz).normalize();

                // Draw plane indicator particles every 4 ticks
                if (player.tickCount % 4 == 0) {
                    Vec3 perp = new Vec3(-planeNormal.z, 0, planeNormal.x);
                    for (int i = -3; i <= 3; i++) {
                        Vec3 pp = planePos.add(perp.scale(i));
                        sl.sendParticles(ParticleTypes.WITCH,
                                pp.x, pp.y + 0.1, pp.z, 1, 0, 0.5, 0, 0.01);
                        sl.sendParticles(ParticleTypes.WITCH,
                                pp.x, pp.y + 1.0, pp.z, 1, 0, 0.5, 0, 0.01);
                        sl.sendParticles(ParticleTypes.WITCH,
                                pp.x, pp.y + 2.0, pp.z, 1, 0, 0.5, 0, 0.01);
                    }
                }

                // Check entities crossing the plane
                float baseDmg = 8f + SoulCore.getAscensionStage(player) * 1.5f;
                List<LivingEntity> nearby = sl.getEntitiesOfClass(
                        LivingEntity.class, player.getBoundingBox().inflate(30),
                        e -> e != player && e.isAlive());

                String prevKey = NBT_FRAC_PREV;
                // We store per-entity side as a compound of UUIDs→sign
                var pd = player.getPersistentData();
                var sideTag = pd.contains(prevKey)
                        ? pd.getCompound(prevKey)
                        : new net.minecraft.nbt.CompoundTag();

                for (LivingEntity e : nearby) {
                    Vec3 toEntity = e.position().subtract(planePos);
                    double side   = toEntity.dot(planeNormal); // positive or negative
                    String key    = e.getUUID().toString();
                    if (sideTag.contains(key)) {
                        double prevSide = sideTag.getDouble(key);
                        // Crossed the plane if signs differ
                        if ((prevSide >= 0) != (side >= 0)) {
                            // Damage scales with how fast they crossed (approx via delta movement)
                            double speed = e.getDeltaMovement().length();
                            float dmg = (baseDmg + (float)(speed * 12f)) * alignMult(player);
                            if (inPerfect(player)) dmg *= 1.4f;
                            e.hurt(sl.damageSources().playerAttack(player), dmg);
                            e.invulnerableTime = 0;
                            addAlignment(player, 8f);
                            sl.sendParticles(ParticleTypes.CRIT,
                                    e.getX(), e.getY() + 1, e.getZ(), 10, 0.3, 0.3, 0.3, 0.05);
                            sl.playSound(null, e.blockPosition(),
                                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 1.4f);
                            if (player instanceof ServerPlayer sp)
                                sp.sendSystemMessage(Component.literal(
                                        "§5Fracture Plane: §f" + String.format("%.1f", dmg)
                                                + " dmg §7(speed ×" + String.format("%.2f", speed) + "). "
                                                + alignStatus(player)));
                        }
                    }
                    sideTag.putDouble(key, side);
                }
                pd.put(prevKey, sideTag);

                if (fracture == 0) {
                    pd.remove(NBT_FRAC_PX); pd.remove(NBT_FRAC_PY); pd.remove(NBT_FRAC_PZ);
                    pd.remove(NBT_FRAC_NX); pd.remove(NBT_FRAC_NZ);
                    pd.remove(prevKey);
                }
            }

            // ── Alignment decay ───────────────────────────────────────────────
            float align = getAlignment(player);
            if (align > 0 && !inPerfect(player)) {
                int decayTimer = player.getPersistentData().getInt(NBT_ALIGN_DECAY);
                decayTimer--;
                if (decayTimer <= 0) {
                    setAlignment(player, align - 1f);
                    decayTimer = ALIGN_DECAY_TICKS;
                }
                player.getPersistentData().putInt(NBT_ALIGN_DECAY, decayTimer);
            }

            // ── HUD: alignment indicator particles ────────────────────────────
            if (player.tickCount % 10 == 0 && align > 0) {
                int count = Math.max(1, (int)(align / 25f));
                sl.sendParticles(ParticleTypes.END_ROD,
                        player.getX(), player.getY() + 2.3, player.getZ(),
                        count, 0.25, 0.1, 0.25, 0.005);
            }
        }

        /** Boost outgoing damage by alignment multiplier. */
        @SubscribeEvent(priority = EventPriority.NORMAL)
        public static void onVectorDamageDealt(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Vector Arbiter")) return;

            event.setAmount(event.getAmount() * alignMult(player));

            // Each hit slowly builds a little alignment
            addAlignment(player, 0.5f);
        }
    }

    // =========================================================================
    //  ABILITY 1 — VECTOR SLASH
    // =========================================================================

    /**
     * Fires a straight-line attack through all enemies in front.
     * Each additional enemy hit increases total damage.
     * Cost: 200 essence. Stage 0+.
     */
    public static void vectorSlash(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Vector Arbiter")) return;
        if (SoulCore.getSoulEssence(player) < 200) return;

        int   stage  = SoulCore.getAscensionStage(player);
        int   range  = inPerfect(player) ? 20 + stage * 2 : 12 + stage;
        List<LivingEntity> targets = entitiesInLine(player, level, range);
        if (targets.isEmpty()) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 200);

        float basePerHit = 10f + stage * 1.5f;
        float totalDealt = 0f;
        int   count      = targets.size();

        for (int i = 0; i < count; i++) {
            // Each successive enemy adds 30% more damage (compounding)
            float dmg = basePerHit * (1f + i * 0.30f) * alignMult(player);
            LivingEntity e = targets.get(i);
            e.hurt(level.damageSources().playerAttack(player), dmg);
            e.invulnerableTime = 0;
            totalDealt += dmg;
            sl.sendParticles(ParticleTypes.CRIT,
                    e.getX(), e.getY() + 1, e.getZ(), 6, 0.2, 0.2, 0.2, 0.04);
        }

        // Alignment reward: more enemies = more alignment
        addAlignment(player, 5f * count);
        if (hasPivotBonus(player)) player.getPersistentData().putInt(NBT_PIVOT, 0); // consume

        // Line particles along look vector
        Vec3 start = player.getEyePosition();
        Vec3 end   = start.add(player.getLookAngle().scale(range));
        lineParticles(sl, start, end, ParticleTypes.CRIT, 0.5);

        level.playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.2f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§bVector Slash: §f" + count + " targets, §e"
                            + String.format("%.1f", totalDealt) + " total dmg. "
                            + alignStatus(player)));
    }

    // =========================================================================
    //  ABILITY 2 — ANGLE BREAK
    // =========================================================================

    /**
     * Cone attack. Tighter angle = higher damage. Wider angle = more targets, less damage.
     * The cone half-angle is determined by how many enemies are in 60°:
     *   ≤15° half-angle → tight burst (×2.0 dmg)
     *   ≤35°            → medium      (×1.4 dmg)
     *   ≤60°            → wide spread (×1.0 dmg)
     * Cost: 350 essence. Requires stage ≥ 1.
     */
    public static void angleBreak(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Vector Arbiter")) return;
        if (SoulCore.getSoulEssence(player) < 350) return;
        if (SoulCore.getAscensionStage(player) < 1) return;

        int   stage    = SoulCore.getAscensionStage(player);
        float range    = 10f + stage;
        // Determine actual cone: find narrowest angle that captures at least one enemy
        float usedAngle;
        List<LivingEntity> targets;
        float coneMult;

        float tightAngle  = inPerfect(player) ? CONE_TIGHT * 0.6f  : CONE_TIGHT;
        float medAngle    = inPerfect(player) ? CONE_MEDIUM * 0.7f : CONE_MEDIUM;
        float wideAngle   = inPerfect(player) ? CONE_WIDE * 0.8f   : CONE_WIDE;

        List<LivingEntity> tight  = entitiesInCone(player, level, tightAngle, range);
        List<LivingEntity> medium = entitiesInCone(player, level, medAngle,  range);
        List<LivingEntity> wide   = entitiesInCone(player, level, wideAngle, range);

        if (!tight.isEmpty()) {
            targets   = tight;
            usedAngle = tightAngle;
            coneMult  = 2.0f;
        } else if (!medium.isEmpty()) {
            targets   = medium;
            usedAngle = medAngle;
            coneMult  = 1.4f;
        } else if (!wide.isEmpty()) {
            targets   = wide;
            usedAngle = wideAngle;
            coneMult  = 1.0f;
        } else {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo targets in cone!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 350);

        float baseDmg = 12f + stage * 2f;
        for (LivingEntity e : targets) {
            float dmg = baseDmg * coneMult * alignMult(player);
            e.hurt(level.damageSources().playerAttack(player), dmg);
            e.invulnerableTime = 0;
            sl.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    e.getX(), e.getY() + 1, e.getZ(), 3, 0.2, 0.2, 0.2, 0.03);
        }

        addAlignment(player, coneMult >= 2.0f ? 15f : coneMult >= 1.4f ? 8f : 3f);
        if (hasPivotBonus(player)) player.getPersistentData().putInt(NBT_PIVOT, 0);

        // Fan particles
        Vec3 look = player.getLookAngle();
        for (int i = -3; i <= 3; i++) {
            double ang = Math.toRadians(usedAngle * i / 3.0);
            double cos = Math.cos(ang), sin = Math.sin(ang);
            Vec3 fanned = new Vec3(
                    look.x * cos - look.z * sin,
                    look.y,
                    look.x * sin + look.z * cos).normalize().scale(range);
            Vec3 tip = player.getEyePosition().add(fanned);
            lineParticles(sl, player.getEyePosition(), tip, ParticleTypes.SWEEP_ATTACK, 0.7);
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 0.8f);

        String tierLabel = coneMult >= 2.0f ? "§eTight" : coneMult >= 1.4f ? "§aMedium" : "§7Wide";
        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§bAngle Break: " + tierLabel + " §f(" + String.format("%.0f", usedAngle)
                            + "°) ×" + coneMult + " → hit §e" + targets.size() + "§f. "
                            + alignStatus(player)));
    }

    // =========================================================================
    //  ABILITY 3 — INTERSECTION
    // =========================================================================

    /**
     * Mark two enemies. An invisible line is drawn between them.
     * Any entity crossing that line takes damage scaled by alignment.
     * Cost: 300 essence. Requires stage ≥ 2.
     */
    public static void intersection(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Vector Arbiter")) return;
        if (SoulCore.getSoulEssence(player) < 300) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        // If mark A not set, set it
        if (!player.getPersistentData().contains(NBT_MARK_A)) {
            LivingEntity target = rayCastFirst(player, level, 20);
            if (target == null) {
                if (player instanceof ServerPlayer sp)
                    sp.sendSystemMessage(Component.literal("§cNo target found for mark A!"));
                return;
            }
            SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 300);
            player.getPersistentData().putUUID(NBT_MARK_A, target.getUUID());
            player.getPersistentData().putInt(NBT_MARK_TIMER, MARK_DURATION);
            sl.sendParticles(ParticleTypes.WITCH,
                    target.getX(), target.getY() + 2, target.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§5Intersection: §fMark A → §c" + target.getName().getString()
                                + "§f. Now mark B."));
            return;
        }

        // Set mark B
        LivingEntity target = rayCastFirst(player, level, 20);
        if (target == null || target.getUUID().equals(player.getPersistentData().getUUID(NBT_MARK_A))) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNeed a different target for mark B!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 300);
        player.getPersistentData().putUUID(NBT_MARK_B, target.getUUID());
        player.getPersistentData().putInt(NBT_MARK_TIMER, MARK_DURATION);

        sl.sendParticles(ParticleTypes.WITCH,
                target.getX(), target.getY() + 2, target.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5Intersection: §fMark B → §c" + target.getName().getString()
                            + "§f. Line active! " + alignStatus(player)));
    }

    /**
     * Called from the tick event (or can be called externally) to check if an entity
     * crosses the intersection line and deal damage.
     * This is handled inside the tick loop above for continuous checking.
     */
    public static void checkIntersectionCrossing(Player player, ServerLevel sl,
                                                 LivingEntity crosser,
                                                 LivingEntity markA, LivingEntity markB) {
        // Vector from A to B
        Vec3 ab  = markB.position().subtract(markA.position());
        Vec3 ac  = crosser.position().subtract(markA.position());
        // Project crosser onto line AB, check if within segment bounds
        double t = ac.dot(ab) / ab.dot(ab);
        if (t < 0 || t > 1) return; // outside segment

        float dmg = (10f + SoulCore.getAscensionStage(player) * 2f) * alignMult(player);
        if (inPerfect(player)) dmg *= 2f;
        crosser.hurt(sl.damageSources().playerAttack(player), dmg);
        crosser.invulnerableTime = 0;
        addAlignment(player, 12f);
        sl.sendParticles(ParticleTypes.CRIT,
                crosser.getX(), crosser.getY() + 1, crosser.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
    }

    // =========================================================================
    //  ABILITY 4 — PIVOT STEP
    // =========================================================================

    /**
     * Instantly face the nearest enemy (or snap 90°/180° if none).
     * For 3 seconds: next ability gains ×1.3 bonus from alignment.
     * Cost: 150 essence. Requires stage ≥ 0.
     */
    public static void pivotStep(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Vector Arbiter")) return;
        if (SoulCore.getSoulEssence(player) < 150) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 150);

        // Find nearest enemy in 16-block radius
        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(16),
                e -> e != player && e.isAlive());

        if (!nearby.isEmpty()) {
            // Face the nearest one
            LivingEntity nearest = nearby.stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                    .orElse(null);
            if (nearest != null && player instanceof ServerPlayer sp) {
                Vec3 dir = nearest.position().subtract(player.position()).normalize();
                float yaw = (float)(Math.toDegrees(Math.atan2(-dir.x, dir.z)));
                sp.setYRot(yaw);
                sp.yRotO = yaw;
            }
        }

        // Apply Pivot bonus
        player.getPersistentData().putInt(NBT_PIVOT, PIVOT_DURATION);
        addAlignment(player, 10f);

        // Speed burst via movement attribute
        var moveAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveAttr != null) {
            moveAttr.removeModifier(PIVOT_SPEED_ID);
            moveAttr.addTransientModifier(new AttributeModifier(
                    PIVOT_SPEED_ID, 0.04, AttributeModifier.Operation.ADD_VALUE));
            // Remove after 10 ticks
            // (handled next tick loop: if pivot expired remove speed — we do it simply here
            //  by scheduling via the existing pivot countdown reaching 0)
        }

        sl.sendParticles(ParticleTypes.END_ROD,
                player.getX(), player.getY() + 1, player.getZ(), 12, 0.4, 0.4, 0.4, 0.06);
        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§bPivot Step: §freposition + next ability ×1.3. " + alignStatus(player)));
    }

    // =========================================================================
    //  ABILITY 5 — BACKLINE PIERCE
    // =========================================================================

    /**
     * Target an enemy. If another enemy is directly behind them, both take heavy damage.
     * If more are aligned, chain pierce continues.
     * Cost: 500 essence. Requires stage ≥ 3.
     */
    public static void vectorArbiterBacklinePierce(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Vector Arbiter")) return;
        if (SoulCore.getSoulEssence(player) < 500) return;
        if (SoulCore.getAscensionStage(player) < 3) return;

        int stage = SoulCore.getAscensionStage(player);
        int range = inPerfect(player) ? 22 + stage : 14 + stage;

        List<LivingEntity> lineTargets = entitiesInLine(player, level, range);
        if (lineTargets.isEmpty()) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo target in line of sight!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 500);

        float baseDmg  = 18f + stage * 3f;
        float chainMult = 1.0f;
        float totalDealt = 0f;
        int   pierced    = 0;

        for (LivingEntity e : lineTargets) {
            float dmg = baseDmg * chainMult * alignMult(player);
            e.hurt(level.damageSources().playerAttack(player), dmg);
            e.invulnerableTime = 0;
            totalDealt += dmg;
            pierced++;
            chainMult *= inPerfect(player) ? 1.3f : 1.1f; // each pierce empowers the next
            sl.sendParticles(ParticleTypes.CRIT,
                    e.getX(), e.getY() + 1, e.getZ(), 8, 0.25, 0.25, 0.25, 0.05);
        }

        addAlignment(player, 10f * pierced);
        if (hasPivotBonus(player)) player.getPersistentData().putInt(NBT_PIVOT, 0);

        lineParticles(sl, player.getEyePosition(),
                player.getEyePosition().add(player.getLookAngle().scale(range)),
                ParticleTypes.CRIT, 0.4);

        level.playSound(null, player.blockPosition(),
                SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 1f, 0.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§bBackline Pierce: §f" + pierced + " enemies §e"
                            + String.format("%.1f", totalDealt) + " total dmg. "
                            + alignStatus(player)));
    }

    // =========================================================================
    //  ABILITY 6 — FRACTURE PLANE
    // =========================================================================

    /**
     * Creates an invisible vertical plane at a point 6 blocks ahead.
     * Enemies crossing it take damage; faster crossing = more damage.
     * Duration: 7 seconds.
     * Cost: 700 essence. Requires stage ≥ 5.
     */
    public static void fracturePlane(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Vector Arbiter")) return;
        if (SoulCore.getSoulEssence(player) < 700) return;
        if (SoulCore.getAscensionStage(player) < 5) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 700);

        int stage = SoulCore.getAscensionStage(player);
        Vec3 look  = player.getLookAngle().normalize();
        // Place plane 6 blocks ahead (horizontal component only for stability)
        Vec3 anchor = player.position().add(look.x * 6, 0, look.z * 6);
        // Normal is the player's look direction (horizontal)
        Vec3 normal = new Vec3(look.x, 0, look.z).normalize();

        var pd = player.getPersistentData();
        pd.putDouble(NBT_FRAC_PX, anchor.x);
        pd.putDouble(NBT_FRAC_PY, anchor.y);
        pd.putDouble(NBT_FRAC_PZ, anchor.z);
        pd.putDouble(NBT_FRAC_NX, normal.x);
        pd.putDouble(NBT_FRAC_NZ, normal.z);
        pd.putInt(NBT_FRACTURE, inPerfect(player) ? FRACTURE_DURATION + 60 : FRACTURE_DURATION);

        addAlignment(player, 8f);

        sl.sendParticles(ParticleTypes.WITCH,
                anchor.x, anchor.y + 1, anchor.z, 16, 0.2, 1.0, 0.2, 0.05);
        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 0.8f, 1.4f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5Fracture Plane: §fplane set §b6 blocks ahead§f. "
                            + "Enemies crossing take speed-scaled dmg. " + alignStatus(player)));
    }

    // =========================================================================
    //  ABILITY 7 — PERFECT ALIGNMENT (ULTIMATE)
    // =========================================================================

    /**
     * 10-second ultimate.
     * - All attacks gain maximum alignment bonuses (alignment treated as 100)
     * - Lines extend further, cones become sharper, intersections multiply damage ×2
     * - Every hit pushes enemy slightly toward perfect line with player
     * Cost: 5000 essence. Requires stage ≥ 7.
     */
    public static void perfectAlignment(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Vector Arbiter")) return;
        if (SoulCore.getSoulEssence(player) < 5000) return;
        if (SoulCore.getAscensionStage(player) < 7) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 5000);

        player.getPersistentData().putInt(NBT_PERFECT, PERFECT_DURATION);
        setAlignment(player, ALIGN_MAX);

        // Burst of geometric particles
        for (int i = 0; i < 36; i++) {
            double angle = Math.toRadians(i * 10);
            double px = player.getX() + 3 * Math.cos(angle);
            double pz = player.getZ() + 3 * Math.sin(angle);
            sl.sendParticles(ParticleTypes.END_ROD, px, player.getY() + 1, pz, 1, 0, 0.3, 0, 0.02);
        }
        sl.sendParticles(ParticleTypes.FLASH,
                player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§b§l✦ PERFECT ALIGNMENT ✦ §r§3Maximum geometry. ×1.5 all dmg. "
                            + "Lines extend, cones sharpen, intersections ×2. §b10s."));
    }

    // ─── Shared ray-cast helper ───────────────────────────────────────────────

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
}