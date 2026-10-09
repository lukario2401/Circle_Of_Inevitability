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
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gravity Architect
 * -----------------------------------------------
 * Theme: gravitational manipulation, spatial geometry, physics warfare.
 * Role: DPS / Control hybrid.
 *
 * Core Identity:
 *   You don't chase enemies — you reshape the battlefield.
 *   Place Gravity Wells that bend space around them.
 *   Chain them together. Collapse them. Crush enemies
 *   between gravitational forces.
 *
 * ── Core Mechanic: GRAVITY WELLS ──────────────────────────────────
 * You place up to 3 Wells in the world (stored on player NBT).
 * Wells:
 *   - Slowly pull nearby enemies toward their center
 *   - Stack with each other (overlapping pull = more force)
 *   - Can be detonated individually or all at once
 *   - Enemies caught between TWO wells are crushed (bonus damage)
 *
 * ── Secondary Mechanic: SINGULARITY CHARGE ─────────────────────────
 * Each well detonation generates 1 Singularity Charge (max 3).
 * Charges power your strongest abilities.
 *
 * ── Positioning Bonus ──────────────────────────────────────────────
 * Being ABOVE an enemy: +20% damage (gravitational advantage)
 * Being BELOW an enemy: −10% damage (fighting uphill)
 *
 * Player NBT keys:
 *   "GravWells"          → ListTag  up to 3 wells (CompoundTag each)
 *   "GravCharges"        → int      singularity charges (0–3)
 *   "GravSingularity"    → int      ticks remaining on Gravitational Singularity
 *   "GravEventHorizon"   → int      ticks remaining on Event Horizon buff
 *   "GravCollapseCD"     → int      Gravity Collapse cooldown
 *   "GravTetherTarget"   → UUID     entity tethered by Gravity Tether
 *   "GravTetherTimer"    → int      ticks remaining on tether
 *
 * Well CompoundTag fields:
 *   "X/Y/Z"     → double  world position
 *   "Timer"     → int     ticks until the well expires (200 = 10s)
 *   "Power"     → float   pull strength (increases over time)
 *   "ID"        → int     unique well index (0, 1, 2)
 */
public class GravityArchitect {

    // ─── NBT keys ─────────────────────────────────────────────────────────────
    private static final String NBT_WELLS        = "GravWells";
    private static final String NBT_CHARGES      = "GravCharges";
    private static final String NBT_SINGULARITY  = "GravSingularity";
    private static final String NBT_EVENT_HOR    = "GravEventHorizon";
    private static final String NBT_COLLAPSE_CD  = "GravCollapseCD";
    private static final String NBT_TETHER_TARGET = "GravTetherTarget";
    private static final String NBT_TETHER_TIMER = "GravTetherTimer";

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final int   MAX_WELLS          = 3;
    private static final int   WELL_LIFETIME      = 200;  // 10 seconds
    private static final int   CHARGES_MAX        = 3;
    private static final float PULL_BASE          = 0.08f;
    private static final float PULL_RADIUS        = 8.0f;
    private static final int   SINGULARITY_DUR    = 200;  // 10 seconds
    private static final int   EVENT_HOR_DUR      = 120;  // 6 seconds
    private static final int   COLLAPSE_COOLDOWN  = 100;  // 5 seconds
    private static final int   TETHER_DURATION    = 80;   // 4 seconds

    // ─── Well Helpers ─────────────────────────────────────────────────────────

    private static net.minecraft.nbt.ListTag getWells(Player player) {
        var data = player.getPersistentData();
        if (!data.contains(NBT_WELLS))
            data.put(NBT_WELLS, new net.minecraft.nbt.ListTag());
        return data.getList(NBT_WELLS, net.minecraft.nbt.Tag.TAG_COMPOUND);
    }

    private static void saveWells(Player player, net.minecraft.nbt.ListTag wells) {
        player.getPersistentData().put(NBT_WELLS, wells);
    }

    private static int getCharges(Player player) {
        return player.getPersistentData().getInt(NBT_CHARGES);
    }

    private static void addCharge(Player player) {
        int c = getCharges(player);
        player.getPersistentData().putInt(NBT_CHARGES, Math.min(c + 1, CHARGES_MAX));
    }

    private static boolean spendCharge(Player player, int amount) {
        int c = getCharges(player);
        if (c < amount) return false;
        player.getPersistentData().putInt(NBT_CHARGES, c - amount);
        return true;
    }

    public static boolean inSingularity(Player player) {
        return player.getPersistentData().getInt(NBT_SINGULARITY) > 0;
    }

    public static boolean inEventHorizon(Player player) {
        return player.getPersistentData().getInt(NBT_EVENT_HOR) > 0;
    }

    /** +20% damage if above enemy, −10% if below. */
    private static float positionalMult(Player player, LivingEntity target) {
        double yDiff = player.getY() - target.getY();
        if (yDiff > 1.5)  return 1.20f;
        if (yDiff < -1.5) return 0.90f;
        return 1.0f;
    }

    /** Counts how many wells are currently pulling a given entity. */
    private static int wellsAffecting(Player player, LivingEntity target) {
        var wells = getWells(player);
        int count = 0;
        for (int i = 0; i < wells.size(); i++) {
            var w = wells.getCompound(i);
            Vec3 wPos = new Vec3(w.getDouble("X"), w.getDouble("Y"), w.getDouble("Z"));
            if (target.position().distanceTo(wPos) <= PULL_RADIUS) count++;
        }
        return count;
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

    // ─── Detonate a single well ────────────────────────────────────────────────

    /**
     * Detonates one well. Damage scales with power and nearby well count.
     * Generates 1 Singularity Charge.
     */
    private static void detonateWell(net.minecraft.nbt.CompoundTag well,
                                     Player player, ServerLevel sl, boolean crushBonus) {
        Vec3  pos    = new Vec3(well.getDouble("X"), well.getDouble("Y"), well.getDouble("Z"));
        float power  = well.getFloat("Power");
        int   stage  = SoulCore.getAscensionStage(player);

        float baseDmg = (18.0f + power * 30f + stage * 2) * positionalMult(player, player);
        if (crushBonus)       baseDmg *= 1.5f;
        if (inSingularity(player)) baseDmg *= 1.8f;
        if (inEventHorizon(player)) baseDmg *= 1.3f;

        float radius = PULL_RADIUS * (1f + power * 0.5f);

        // Implosion: enemies fly inward then get blasted
        List<LivingEntity> targets = sl.getEntitiesOfClass(LivingEntity.class,
                new AABB(pos, pos).inflate(radius),
                e -> !e.getUUID().equals(player.getUUID()) && e.isAlive());

        for (LivingEntity e : targets) {
            // Crush bonus: caught between 2+ wells
            float finalDmg = baseDmg;
            int wellCount = wellsAffecting(player, e);
            if (wellCount >= 2) finalDmg *= 1.4f; // caught between wells

            // Positional bonus vs this specific target
            finalDmg *= positionalMult(player, e);

            e.hurt(sl.damageSources().playerAttack(player), finalDmg);
            e.invulnerableTime = 0;

            // Implosion: pull toward center on detonation
            Vec3 pull = pos.subtract(e.position()).normalize().scale(1.2);
            e.setDeltaMovement(e.getDeltaMovement().add(pull.x, pull.y * 0.3, pull.z));

            sl.sendParticles(ParticleTypes.EXPLOSION,
                    e.getX(), e.getY() + 1, e.getZ(), 4, 0.3, 0.3, 0.3, 0.03);
        }

        // Visual implosion
        for (int ring = 3; ring >= 1; ring--) {
            double r = radius * ring / 3.0;
            int count = (int)(r * 10);
            for (int i = 0; i < count; i++) {
                double angle = (2 * Math.PI / count) * i;
                sl.sendParticles(ParticleTypes.PORTAL,
                        pos.x + r * Math.cos(angle), pos.y + 0.5,
                        pos.z + r * Math.sin(angle), 1, 0, 0.1, 0, 0.02);
            }
        }
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y + 1, pos.z, 1, 0, 0, 0, 0);
        sl.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2f, 0.5f);

        // Grant charge
        addCharge(player);
    }

    // =========================================================================
    //  EVENTS
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class GravityEvents {

        @SubscribeEvent
        public static void onGravityPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Gravity Architect")) return;

            // ── Singularity countdown ─────────────────────────────────────────
            int sing = player.getPersistentData().getInt(NBT_SINGULARITY);
            if (sing > 0) {
                player.getPersistentData().putInt(NBT_SINGULARITY, sing - 1);
                // Gravitational aura during singularity
                if (player.tickCount % 5 == 0) {
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                            player.getX(), player.getY() + 1, player.getZ(),
                            6, 0.6, 0.6, 0.6, 0.04);
                    // Pull all nearby enemies toward player
                    AABB aura = player.getBoundingBox().inflate(10);
                    sl.getEntitiesOfClass(LivingEntity.class, aura,
                                    e -> !e.getUUID().equals(player.getUUID()) && e.isAlive())
                            .forEach(e -> {
                                Vec3 pull = player.position().subtract(e.position()).normalize().scale(0.12);
                                e.setDeltaMovement(e.getDeltaMovement().add(pull.x, 0.02, pull.z));
                                e.hurtMarked = true;
                            });
                }
            }

            // ── Event Horizon countdown ───────────────────────────────────────
            int eh = player.getPersistentData().getInt(NBT_EVENT_HOR);
            if (eh > 0) {
                player.getPersistentData().putInt(NBT_EVENT_HOR, eh - 1);
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 5, 1, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 5, 2, true, false));
                if (player.tickCount % 8 == 0)
                    sl.sendParticles(ParticleTypes.PORTAL,
                            player.getX(), player.getY() + 1, player.getZ(),
                            3, 0.3, 0.3, 0.3, 0.03);
            }

            // ── Collapse CD ───────────────────────────────────────────────────
            int collapseCD = player.getPersistentData().getInt(NBT_COLLAPSE_CD);
            if (collapseCD > 0)
                player.getPersistentData().putInt(NBT_COLLAPSE_CD, collapseCD - 1);

            // ── Tether tick ───────────────────────────────────────────────────
            int tetherTimer = player.getPersistentData().getInt(NBT_TETHER_TIMER);
            if (tetherTimer > 0) {
                tetherTimer--;
                player.getPersistentData().putInt(NBT_TETHER_TIMER, tetherTimer);
                if (player.getPersistentData().contains(NBT_TETHER_TARGET)) {
                    UUID tUUID = player.getPersistentData().getUUID(NBT_TETHER_TARGET);
                    sl.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(30),
                                    e -> e.getUUID().equals(tUUID) && e.isAlive())
                            .forEach(e -> {
                                // Keep tethered enemy within 6 blocks of player
                                double dist = e.distanceTo(player);
                                if (dist > 6.0) {
                                    Vec3 pull = player.position().subtract(e.position()).normalize().scale(0.15);
                                    e.setDeltaMovement(e.getDeltaMovement().add(pull.x, 0, pull.z));
                                    e.hurtMarked = true;
                                }
                                // Slow tethered enemy
                                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                                        5, 1, true, false));
                                // Tether particle line
                                if (player.tickCount % 4 == 0) {
                                    Vec3 from = e.position().add(0, 1, 0);
                                    Vec3 to   = player.position().add(0, 1, 0);
                                    Vec3 d    = to.subtract(from).normalize();
                                    Vec3 c    = from;
                                    double dist2 = from.distanceTo(to);
                                    for (double dd = 0; dd < dist2; dd += 0.5) {
                                        sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                                                c.x, c.y, c.z, 1, 0.05, 0.05, 0.05, 0);
                                        c = c.add(d.scale(0.5));
                                    }
                                }
                            });
                }
                if (tetherTimer == 0) player.getPersistentData().remove(NBT_TETHER_TARGET);
            }

            // ── Well tick: pull, age, and display ─────────────────────────────
            var wells    = getWells(player);
            var remaining = new net.minecraft.nbt.ListTag();

            for (int i = 0; i < wells.size(); i++) {
                var well  = wells.getCompound(i);
                int timer = well.getInt("Timer");

                if (timer <= 0) {
                    // Well expired — collapse silently
                    continue;
                }

                timer--;
                well.putInt("Timer", timer);

                // Power ramps up over the well's lifetime (0.0 → 1.0)
                float power = 1.0f - (timer / (float) WELL_LIFETIME);
                well.putFloat("Power", power);

                Vec3 wPos = new Vec3(well.getDouble("X"), well.getDouble("Y"), well.getDouble("Z"));

                // Ambient particles — more intense as power grows
                if (player.tickCount % 3 == 0) {
                    int pCount = 2 + (int)(power * 6);
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                            wPos.x, wPos.y + 0.3, wPos.z, pCount, 0.4, 0.3, 0.4, 0.04);
                    sl.sendParticles(ParticleTypes.PORTAL,
                            wPos.x, wPos.y + 0.3, wPos.z, 1, 0, 0, 0, 0);
                }

                // Pull nearby enemies toward well center
                if (player.tickCount % 4 == 0) {
                    float pullStrength = PULL_BASE + power * 0.06f;
                    sl.getEntitiesOfClass(LivingEntity.class,
                                    new AABB(wPos, wPos).inflate(PULL_RADIUS),
                                    e -> !e.getUUID().equals(player.getUUID()) && e.isAlive())
                            .forEach(e -> {
                                Vec3 pull = wPos.subtract(e.position()).normalize().scale(pullStrength);
                                e.setDeltaMovement(e.getDeltaMovement().add(pull.x, pull.y * 0.1, pull.z));
                                e.hurtMarked = true;
                            });
                }

                // DOT damage from wells during Singularity
                if (inSingularity(player) && player.tickCount % 20 == 0) {
                    sl.getEntitiesOfClass(LivingEntity.class,
                                    new AABB(wPos, wPos).inflate(PULL_RADIUS),
                                    e -> !e.getUUID().equals(player.getUUID()) && e.isAlive())
                            .forEach(e -> {
                                e.hurt(sl.damageSources().playerAttack(player), 4.0f);
                                e.invulnerableTime = 0;
                            });
                }

                remaining.add(well);
            }

            saveWells(player, remaining);

            // ── Charges HUD particles ─────────────────────────────────────────
            int charges = getCharges(player);
            if (charges > 0 && player.tickCount % 6 == 0) {
                double arcSpan  = Math.toRadians(120);
                double arcStart = -arcSpan / 2.0;
                double spacing  = charges > 1 ? arcSpan / (charges - 1) : 0;
                for (int i = 0; i < charges; i++) {
                    double angle = arcStart + spacing * i;
                    double x = player.getX() + 0.9 * Math.sin(angle);
                    double z = player.getZ() + 0.9 * Math.cos(angle);
                    sl.sendParticles(inSingularity(player) ? ParticleTypes.GLOW : ParticleTypes.PORTAL,
                            x, player.getY() + 2.2, z, 1, 0, 0, 0, 0);
                }
            }
        }

        /** Bonus damage when enemy is caught between multiple wells. */
        @SubscribeEvent
        public static void onGravityDamageBonus(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Gravity Architect")) return;

            LivingEntity target = event.getEntity();
            float mult = positionalMult(player, target);

            int affectedByWells = wellsAffecting(player, target);
            if (affectedByWells >= 2) mult *= 1.30f; // crushed between wells
            if (inEventHorizon(player)) mult *= 1.25f;
            if (inSingularity(player))  mult *= 1.5f;

            event.setAmount(event.getAmount() * mult);
        }
    }

    // =========================================================================
    //  ABILITY 1 — PLACE GRAVITY WELL
    // =========================================================================

    /**
     * Places a Gravity Well at a targeted location (up to 12 blocks away).
     * Max 3 wells at once. Each well pulls enemies toward it and grows stronger.
     * Wells overlap — enemies between 2 wells are crushed for bonus damage.
     * Cost: 400 essence.
     */
    public static void placeGravityWell(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Gravity Architect")) return;
        if (SoulCore.getSoulEssence(player) < 400) return;

        var wells = getWells(player);
        int maxWells = MAX_WELLS + (inSingularity(player) ? 2 : 0);
        if (wells.size() >= maxWells) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§5Max wells placed! §7Detonate one first. §b[" + wells.size() + "/" + maxWells + "]"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 400);

        // Place 8 blocks ahead or at first solid surface
        int stage = SoulCore.getAscensionStage(player);
        Vec3 placePos = player.getEyePosition().add(player.getLookAngle().normalize().scale(8 + stage));

        var well = new net.minecraft.nbt.CompoundTag();
        well.putDouble("X", placePos.x);
        well.putDouble("Y", placePos.y);
        well.putDouble("Z", placePos.z);
        well.putInt("Timer", WELL_LIFETIME);
        well.putFloat("Power", 0f);
        well.putInt("ID", wells.size());

        wells.add(well);
        saveWells(player, wells);

        sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                placePos.x, placePos.y + 0.5, placePos.z, 16, 0.5, 0.5, 0.5, 0.04);
        sl.playSound(null, placePos.x, placePos.y, placePos.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6f, 0.4f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5Gravity Well placed. §7[" + wells.size() + "/" + maxWells + "] active. "
                            + "§bCharges: " + getCharges(player) + "/" + CHARGES_MAX));
    }

    // =========================================================================
    //  ABILITY 2 — DETONATE WELL
    // =========================================================================

    /**
     * Detonates the oldest placed well (FIFO order).
     * Damage scales with how long the well has been building power.
     * Enemies caught between multiple wells take crush bonus damage.
     * Grants 1 Singularity Charge.
     * Cost: 500 essence.  Requires stage ≥ 1.
     */
    public static void detonateWellAbility(Player player, Level level , ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Gravity Architect")) return;
        if (SoulCore.getSoulEssence(player) < 500) return;
        if (SoulCore.getAscensionStage(player) < 1) return;

        var wells = getWells(player);
        if (wells.isEmpty()) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo wells to detonate!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 500);

        // Detonate the well with most power (lowest remaining timer = oldest)
        net.minecraft.nbt.CompoundTag oldest = wells.getCompound(0);
        for (int i = 1; i < wells.size(); i++) {
            var w = wells.getCompound(i);
            if (w.getFloat("Power") > oldest.getFloat("Power")) oldest = w;
        }

        // Remove it from the list
        var newWells = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < wells.size(); i++) {
            var w = wells.getCompound(i);
            if (w != oldest) newWells.add(w);
        }
        saveWells(player, newWells);

        // Detonate
        detonateWell(oldest, player, sl, wells.size() > 1);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5Well detonated! §7Power: §b" + String.format("%.0f", oldest.getFloat("Power") * 100)
                            + "% §5| Charges: §b" + getCharges(player) + "/" + CHARGES_MAX));
    }

    // =========================================================================
    //  ABILITY 3 — GRAVITY COLLAPSE
    // =========================================================================

    /**
     * Detonates ALL active wells simultaneously.
     * Enemies between multiple wells take massive crush damage.
     * Costs 2 Singularity Charges.
     * Cost: 800 essence.  Requires stage ≥ 2.  5-second cooldown.
     */
    public static void gravityCollapse(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Gravity Architect")) return;
        if (SoulCore.getSoulEssence(player) < 800) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        int collapseCD = player.getPersistentData().getInt(NBT_COLLAPSE_CD);
        if (collapseCD > 0) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§cGravity Collapse on cooldown! §7(" + (int)Math.ceil(collapseCD / 20.0) + "s)"));
            return;
        }

        var wells = getWells(player);
        if (wells.isEmpty()) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo wells to collapse!"));
            return;
        }

        if (!spendCharge(player, 2)) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§cNeed 2 Singularity Charges! §7(have " + getCharges(player) + ")"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 800);

        int count = wells.size();
        List<net.minecraft.nbt.CompoundTag> toDetonate = new ArrayList<>();
        for (int i = 0; i < count; i++) toDetonate.add(wells.getCompound(i));

        // Clear wells first
        saveWells(player, new net.minecraft.nbt.ListTag());

        // Detonate all with crush bonus if more than 1
        for (var well : toDetonate) detonateWell(well, player, sl, count > 1);

        player.getPersistentData().putInt(NBT_COLLAPSE_CD, COLLAPSE_COOLDOWN);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5§lGRAVITY COLLAPSE! §r§7" + count + " wells detonated simultaneously!"
                            + " Charges: §b" + getCharges(player) + "/" + CHARGES_MAX));
    }

    // =========================================================================
    //  ABILITY 4 — GRAVITY TETHER
    // =========================================================================

    /**
     * Locks an enemy in a gravitational tether for 4 seconds.
     * Tethered enemy:
     *   - Cannot move more than 6 blocks from the player
     *   - Is permanently slowed
     *   - Takes ×1.3 damage from all sources while tethered
     * Costs 1 Singularity Charge.
     * Cost: 600 essence.  Requires stage ≥ 3.
     */
    public static void gravityTether(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Gravity Architect")) return;
        if (SoulCore.getSoulEssence(player) < 600) return;
        if (SoulCore.getAscensionStage(player) < 3) return;

        if (!spendCharge(player, 1)) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§cNeed 1 Singularity Charge! §7(have " + getCharges(player) + ")"));
            return;
        }

        LivingEntity target = rayCastFirst(player, level, 14 + SoulCore.getAscensionStage(player));
        if (target == null) {
            addCharge(player); // refund
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 600);

        player.getPersistentData().putUUID(NBT_TETHER_TARGET, target.getUUID());
        player.getPersistentData().putInt(NBT_TETHER_TIMER, TETHER_DURATION);

        sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                target.getX(), target.getY() + 1, target.getZ(), 16, 0.4, 0.4, 0.4, 0.04);
        sl.playSound(null, target.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1f, 0.4f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5Gravity Tether: §f" + target.getName().getString()
                            + " §5tethered for §b4s§5. ×1.3 damage taken. "
                            + "Charges: §b" + getCharges(player) + "/" + CHARGES_MAX));
    }

    // =========================================================================
    //  ABILITY 5 — EVENT HORIZON
    // =========================================================================

    /**
     * Personal gravity shield for 6 seconds.
     * Effects:
     *   - Speed II + Jump Boost III (gravitational propulsion)
     *   - All your attacks deal ×1.25 more damage
     *   - You leave a gravity trail that slows enemies passing through
     * Cost: 700 essence.  Requires stage ≥ 4.
     */
    public static void eventHorizon(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Gravity Architect")) return;
        if (SoulCore.getSoulEssence(player) < 700) return;
        if (SoulCore.getAscensionStage(player) < 4) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 700);

        player.getPersistentData().putInt(NBT_EVENT_HOR, EVENT_HOR_DUR);

        sl.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8f, 0.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5Event Horizon: §bSpeed II, Jump III, ×1.25 damage for §b6s§5."));
    }

    // =========================================================================
    //  ABILITY 6 — SINGULARITY BEAM
    // =========================================================================

    /**
     * Fire a focused gravitational beam that pierces all enemies in a line.
     * Pulls hit enemies toward the beam's end point.
     * Costs 2 Singularity Charges.
     * Cost: 900 essence.  Requires stage ≥ 5.
     */
    public static void singularityBeam(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Gravity Architect")) return;
        if (SoulCore.getSoulEssence(player) < 900) return;
        if (SoulCore.getAscensionStage(player) < 5) return;

        if (!spendCharge(player, 2)) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§cNeed 2 Singularity Charges! §7(have " + getCharges(player) + ")"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 900);

        int   stage  = SoulCore.getAscensionStage(player);
        float damage = (25.0f + stage * 3) * (inSingularity(player) ? 2.0f : 1.0f);
        int   range  = 20 + stage;

        Vec3 start = player.getEyePosition();
        Vec3 dir   = player.getLookAngle().normalize();
        Vec3 cur   = start;
        Vec3 endPos = start.add(dir.scale(range));

        // Beam visual + hit detection
        for (int i = 0; i < range; i++) {
            cur = cur.add(dir);
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL, cur.x, cur.y, cur.z, 2, 0.1, 0.1, 0.1, 0.01);

            final Vec3 beamEnd = endPos;
            level.getEntitiesOfClass(LivingEntity.class, new AABB(cur, cur).inflate(0.6),
                            e -> e != player && e.isAlive())
                    .forEach(e -> {
                        float finalDmg = damage * positionalMult(player, e);
                        int wAffect = wellsAffecting(player, e);
                        if (wAffect > 0) finalDmg *= 1.2f * wAffect;

                        e.hurt(level.damageSources().playerAttack(player), finalDmg);
                        e.invulnerableTime = 0;

                        // Pull toward beam endpoint
                        Vec3 pull = beamEnd.subtract(e.position()).normalize().scale(0.8);
                        e.setDeltaMovement(e.getDeltaMovement().add(pull.x, 0.2, pull.z));

                        sl.sendParticles(ParticleTypes.EXPLOSION,
                                e.getX(), e.getY() + 1, e.getZ(), 4, 0.3, 0.3, 0.3, 0.03);
                    });
        }

        sl.playSound(null, player.blockPosition(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1f, 0.3f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5Singularity Beam: §f" + String.format("%.1f", damage)
                            + " dmg, pierces all. §5Charges: §b" + getCharges(player) + "/" + CHARGES_MAX));
    }

    // =========================================================================
    //  ABILITY 7 — GRAVITATIONAL SINGULARITY (ultimate)
    // =========================================================================

    /**
     * 10-second ultimate transformation:
     *   - You become the center of a gravitational singularity
     *   - All wells gain DOT aura
     *   - Well cap increases to +2
     *   - All attacks deal ×1.5 damage
     *   - Pulling enemies toward any well deals bonus DOT
     *   - Every detonation during singularity generates 2 charges instead of 1
     *   - Final bonus: killing an enemy during singularity places a well at death location
     * Cost: 5000 essence.  Requires stage ≥ 7.
     */
    public static void gravitationalSingularity(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Gravity Architect")) return;
        if (SoulCore.getSoulEssence(player) < 5000) return;
        if (SoulCore.getAscensionStage(player) < 7) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 5000);

        player.getPersistentData().putInt(NBT_SINGULARITY, SINGULARITY_DUR);
        // Max out charges on activation
        player.getPersistentData().putInt(NBT_CHARGES, CHARGES_MAX);

        sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                player.getX(), player.getY() + 1, player.getZ(), 60, 1.0, 1.0, 1.0, 0.07);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(), 3, 0.4, 0.4, 0.4, 0);
        sl.sendParticles(ParticleTypes.PORTAL,
                player.getX(), player.getY() + 1, player.getZ(), 40, 1.0, 1.0, 1.0, 0.06);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 0.2f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5§l◈ GRAVITATIONAL SINGULARITY ◈ §r§dYou are the center of all things. "
                            + "10s. ×1.5 dmg. Wells gain aura. Kills spawn wells."));
    }

    // ── Singularity kill → auto-place well ────────────────────────────────────

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class GravityKillEvents {

        @SubscribeEvent
        public static void onGravityKill(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Gravity Architect")) return;
            if (!inSingularity(player)) return;
            if (!(player.level() instanceof ServerLevel sl)) return;

            LivingEntity dead = event.getEntity();
            var wells = getWells(player);
            int maxWells = MAX_WELLS + 2; // singularity cap

            if (wells.size() >= maxWells) return;

            // Auto-place well at kill location
            var well = new net.minecraft.nbt.CompoundTag();
            well.putDouble("X", dead.getX());
            well.putDouble("Y", dead.getY());
            well.putDouble("Z", dead.getZ());
            well.putInt("Timer", WELL_LIFETIME / 2); // shorter auto-wells
            well.putFloat("Power", 0.3f);            // start partially powered
            well.putInt("ID", wells.size());
            wells.add(well);
            saveWells(player, wells);

            // Grant bonus charge on kill during singularity
            addCharge(player);

            sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    dead.getX(), dead.getY() + 0.5, dead.getZ(), 12, 0.4, 0.4, 0.4, 0.04);

            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§5◈ Singularity kill — well auto-placed! "
                                + "§bCharges: " + getCharges(player) + "/" + CHARGES_MAX));
        }
    }
}