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
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/**
 * Death Descendant  (formerly Crystal Tyrant rename — Umbral Harvester design)
 * -----------------------------------------------
 * Theme: harvesting souls, execution, death scaling
 * Role: DPS (single-target → multi-target snowball)
 *
 * Core Mechanic — ESSENCE (0–100, player resource):
 *   0–40  → normal
 *   40–80 → increased damage
 *   80–100→ bonus execution effects
 *
 * Mark Stacks (per enemy, 0–5):
 *   Increase damage taken. Required for executions.
 *
 * Harvest Modes:
 *   REAP        → apply extra Marks, faster Essence gain
 *   EXECUTION   → bonus dmg to marked, can instantly execute low HP
 *   CONSUMPTION → abilities consume Essence for massive buffs
 *
 * Player NBT keys:
 *   "UHEssence"         → float  0–100
 *   "UHEssenceDecay"    → int    ticks until next decay
 *   "UHMode"            → int    0=REAP, 1=EXECUTION, 2=CONSUMPTION
 *   "UHModeBoost"       → int    ticks remaining on mode-shift empowerment
 *   "UHDarkFeast"       → int    ticks remaining on Dark Feast buff
 *   "UHAbyssal"         → int    ticks remaining on Abyssal Harvest ultimate
 *   "UHBindTarget"      → UUID   currently bound target (Umbral Bind)
 *   "UHBindTimer"       → int    ticks remaining on bind
 *
 * Entity NBT keys:
 *   "UHMarks"           → int    0–5 mark stacks
 *   "UHMarkOwner"       → UUID   player who applied marks
 *   "UHBindDoTTimer"    → int    ticks until next bind DoT tick (Consumption mode)
 */
public class DeathDescendant {

    // ─── Modes ────────────────────────────────────────────────────────────────
    public static final int MODE_REAP        = 0;
    public static final int MODE_EXECUTION   = 1;
    public static final int MODE_CONSUMPTION = 2;

    // ─── NBT keys (player) ────────────────────────────────────────────────────
    private static final String NBT_ESSENCE       = "UHEssence";
    private static final String NBT_ESSENCE_DECAY = "UHEssenceDecay";
    private static final String NBT_MODE          = "UHMode";
    private static final String NBT_MODE_BOOST    = "UHModeBoost";
    private static final String NBT_DARK_FEAST    = "UHDarkFeast";
    private static final String NBT_ABYSSAL       = "UHAbyssal";
    private static final String NBT_BIND_TARGET   = "UHBindTarget";
    private static final String NBT_BIND_TIMER    = "UHBindTimer";

    // ─── NBT keys (entity) ────────────────────────────────────────────────────
    private static final String NBT_MARKS      = "UHMarks";
    private static final String NBT_MARK_OWNER = "UHMarkOwner";
    private static final String NBT_BIND_DOT   = "UHBindDoTTimer";

    // ─── Attribute modifier ResourceLocations (1.21 API) ─────────────────────
    private static final ResourceLocation FEAST_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "uh_feast_speed");
    private static final ResourceLocation FEAST_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "uh_feast_damage");

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final float ESSENCE_MAX        = 100f;
    private static final int   ESSENCE_DECAY_TICK = 50;   // 1 essence lost every 2.5s
    private static final int   MARKS_MAX          = 5;
    private static final int   MODE_BOOST_DUR     = 120;   // 4 seconds
    private static final int   DARK_FEAST_DUR     = 100;  // 5 seconds
    private static final int   ABYSSAL_DUR        = 240;  // 12 seconds
    private static final int   BIND_DURATION      = 60;   // 3 seconds
    private static final int   BIND_DOT_INTERVAL  = 10;   // bind DoT every 0.75s

    // Execute HP thresholds per mode
    private static final float EXEC_THRESHOLD_BASE       = 0.20f; // 20% HP
    private static final float EXEC_THRESHOLD_EXECUTION  = 0.30f; // 30% HP in Execution mode
    private static final float EXEC_THRESHOLD_ABYSSAL    = 0.45f; // 45% HP during ult

    // Mark damage-taken amplification
    private static final float[] MARK_DMG_MULT = {
            1.00f, 1.10f, 1.20f, 1.35f, 1.50f, 1.70f
    };

    private static final Random RNG = new Random();

    // =========================================================================
    //  ESSENCE HELPERS
    // =========================================================================

    public static float getEssence(Player player) {
        return player.getPersistentData().getFloat(NBT_ESSENCE);
    }

    public static void setEssence(Player player, float val) {
        player.getPersistentData().putFloat(NBT_ESSENCE,
                Math.max(0f, Math.min(ESSENCE_MAX, val)));
    }

    public static void addEssence(Player player, float amount) {
        setEssence(player, getEssence(player) + amount);
    }

    private static boolean spendEssence(Player player, float cost) {
        if (getEssence(player) < cost) return false;
        setEssence(player, getEssence(player) - cost);
        return true;
    }

    public static int getMode(Player player) {
        return player.getPersistentData().getInt(NBT_MODE);
    }

    public static boolean inAbyssal(Player player) {
        return player.getPersistentData().getInt(NBT_ABYSSAL) > 0;
    }

    public static boolean hasModeBoost(Player player) {
        return player.getPersistentData().getInt(NBT_MODE_BOOST) > 0;
    }

    public static boolean hasDarkFeast(Player player) {
        return player.getPersistentData().getInt(NBT_DARK_FEAST) > 0;
    }

    /** Global damage multiplier from Essence level + active buffs. */
    private static float essenceMult(Player player) {
        float ess  = getEssence(player);
        float mult = 1.0f;
        if (ess >= 80f)      mult = 1.5f;
        else if (ess >= 40f) mult = 1.25f;
        if (hasModeBoost(player)) mult *= 1.20f;
        if (hasDarkFeast(player)) mult *= 1.30f;
        if (inAbyssal(player))    mult *= 1.50f;
        return mult;
    }

    /** Execute HP threshold based on current state. */
    private static float execThreshold(Player player) {
        if (inAbyssal(player)) return EXEC_THRESHOLD_ABYSSAL;
        if (getMode(player) == MODE_EXECUTION) return EXEC_THRESHOLD_EXECUTION;
        return EXEC_THRESHOLD_BASE;
    }

    private static String modeName(int mode) {
        return switch (mode) {
            case MODE_REAP        -> "§c[Reap]";
            case MODE_EXECUTION   -> "§4[Execution]";
            case MODE_CONSUMPTION -> "§5[Consumption]";
            default               -> "§7[?]";
        };
    }

    private static String essenceTag(float ess) {
        if (ess >= 80f)      return "§5[Death Surge]";
        else if (ess >= 40f) return "§4[Empowered]";
        return "§7[Building]";
    }

    private static String essenceStatus(Player player) {
        float e = getEssence(player);
        return essenceTag(e) + " §fEssence: §b" + String.format("%.0f", e)
                + "/100 " + modeName(getMode(player));
    }

    // =========================================================================
    //  MARK HELPERS (stored on entity)
    // =========================================================================

    public static int getMarks(LivingEntity entity) {
        return entity.getPersistentData().getInt(NBT_MARKS);
    }

    public static void setMarks(LivingEntity entity, int marks) {
        entity.getPersistentData().putInt(NBT_MARKS,
                Math.max(0, Math.min(MARKS_MAX, marks)));
    }

    public static void addMarks(LivingEntity entity, Player owner, int amount) {
        setMarks(entity, getMarks(entity) + amount);
        entity.getPersistentData().putUUID(NBT_MARK_OWNER, owner.getUUID());
    }

    public static void clearMarks(LivingEntity entity) {
        entity.getPersistentData().putInt(NBT_MARKS, 0);
        entity.getPersistentData().remove(NBT_MARK_OWNER);
    }

    private static boolean ownedBy(LivingEntity entity, Player player) {
        return entity.getPersistentData().contains(NBT_MARK_OWNER)
                && entity.getPersistentData().getUUID(NBT_MARK_OWNER).equals(player.getUUID());
    }

    private static String markStatus(LivingEntity entity) {
        int m = getMarks(entity);
        String col = m >= 4 ? "§5" : m >= 2 ? "§c" : "§7";
        return col + "Marks: §f[" + m + "/5]";
    }

    // =========================================================================
    //  EXECUTION LOGIC
    // =========================================================================

    /**
     * Attempts to execute a target. Returns true if execution succeeded.
     * Triggers AOE explosion during Abyssal Harvest.
     */
    private static boolean tryExecute(LivingEntity target, Player player,
                                      Level level, ServerLevel sl) {
        float hpPct = target.getHealth() / target.getMaxHealth();
        if (hpPct > execThreshold(player)) return false;

        int marks = getMarks(target);
        clearMarks(target);

        // Essence gain from execution
        float essGain = 20f + marks * 5f + SoulCore.getAscensionStage(player) * 3f;
        if (inAbyssal(player)) essGain *= 1.5f;
        addEssence(player, essGain);

        // Kill
        target.hurt(level.damageSources().playerAttack(player),
                target.getMaxHealth() * 2f);

        // AOE explosion during Abyssal or if Essence >= 80
        if (inAbyssal(player) || getEssence(player) >= 80f) {
            float explRadius = 5f + SoulCore.getAscensionStage(player) * 0.5f;
            float explDmg    = (15f + marks * 4f) * essenceMult(player);
            List<LivingEntity> nearby = level.getEntitiesOfClass(
                    LivingEntity.class,
                    target.getBoundingBox().inflate(explRadius),
                    e -> e != player && e != target && e.isAlive());
            for (LivingEntity e : nearby) {
                e.hurt(level.damageSources().playerAttack(player), explDmg);
                e.invulnerableTime = 0;
                // Spread 1 mark in Reap or Abyssal
                if (getMode(player) == MODE_REAP || inAbyssal(player))
                    addMarks(e, player, 1);
                sl.sendParticles(ParticleTypes.SOUL,
                        e.getX(), e.getY() + 1, e.getZ(), 4, 0.2, 0.2, 0.2, 0.03);
            }
            for (int i = 0; i < 20; i++) {
                double ang = Math.toRadians(i * 18);
                sl.sendParticles(ParticleTypes.SOUL,
                        target.getX() + explRadius * Math.cos(ang),
                        target.getY() + 0.5,
                        target.getZ() + explRadius * Math.sin(ang),
                        1, 0, 0.2, 0, 0.02);
            }
            sl.playSound(null, target.blockPosition(),
                    SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 0.8f, 1.2f);
        }

        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                target.getX(), target.getY() + 1, target.getZ(),
                16, 0.3, 0.3, 0.3, 0.05);
        sl.sendParticles(ParticleTypes.SOUL,
                target.getX(), target.getY() + 1, target.getZ(),
                10, 0.4, 0.4, 0.4, 0.04);
        sl.playSound(null, target.blockPosition(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 0.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§4§l☠ EXECUTED §r§f" + target.getName().getString()
                            + " §7(§e" + marks + " marks§7) → §b+"
                            + String.format("%.0f", essGain) + " Essence. "
                            + essenceStatus(player)));
        return true;
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

    // ─── Particle line helper ─────────────────────────────────────────────────
    private static void soulLine(ServerLevel sl, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from).normalize();
        double dist = from.distanceTo(to);
        Vec3 c = from;
        for (double dd = 0; dd < dist; dd += 0.45) {
            sl.sendParticles(ParticleTypes.SOUL, c.x, c.y, c.z, 1, 0.03, 0.03, 0.03, 0);
            c = c.add(d.scale(0.45));
        }
    }

    // =========================================================================
    //  EVENTS
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class DeathDescendantEvents {

        @SubscribeEvent
        public static void onDeathDescendantTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Death Descendant")) return;

            // ── Abyssal Harvest countdown ─────────────────────────────────────
            int abyssal = player.getPersistentData().getInt(NBT_ABYSSAL);
            if (abyssal > 0) {
                player.getPersistentData().putInt(NBT_ABYSSAL, abyssal - 1);
                // Constantly refill essence
                if (player.tickCount % 10 == 0) addEssence(player, 3f);
                // Auto-apply marks to all nearby enemies every 1s
                if (player.tickCount % 20 == 0) {
                    sl.getEntitiesOfClass(LivingEntity.class,
                                    player.getBoundingBox().inflate(20),
                                    e -> e != player && e.isAlive())
                            .forEach(e -> addMarks(e, player, 1));
                }
                if (player.tickCount % 6 == 0) {
                    sl.sendParticles(ParticleTypes.SOUL,
                            player.getX(), player.getY() + 1, player.getZ(),
                            4, 0.5, 0.5, 0.5, 0.04);
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            player.getX(), player.getY() + 1, player.getZ(),
                            2, 0.3, 0.3, 0.3, 0.03);
                }
            }

            // ── Dark Feast countdown ──────────────────────────────────────────
            int feast = player.getPersistentData().getInt(NBT_DARK_FEAST);
            if (feast > 0) {
                player.getPersistentData().putInt(NBT_DARK_FEAST, feast - 1);
                if (player.tickCount % 12 == 0)
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            player.getX(), player.getY() + 1, player.getZ(),
                            2, 0.2, 0.2, 0.2, 0.02);
                if (feast == 1) {
                    // Remove attribute modifiers on expiry
                    var spd = player.getAttribute(Attributes.MOVEMENT_SPEED);
                    var dmg = player.getAttribute(Attributes.ATTACK_DAMAGE);
                    if (spd != null) spd.removeModifier(FEAST_SPEED_ID);
                    if (dmg != null) dmg.removeModifier(FEAST_DAMAGE_ID);
                }
            }

            // ── Mode boost countdown ──────────────────────────────────────────
            int modeBoost = player.getPersistentData().getInt(NBT_MODE_BOOST);
            if (modeBoost > 0) {
                player.getPersistentData().putInt(NBT_MODE_BOOST, modeBoost - 1);
                if (player.tickCount % 10 == 0)
                    sl.sendParticles(ParticleTypes.SOUL,
                            player.getX(), player.getY() + 1, player.getZ(),
                            2, 0.2, 0.2, 0.2, 0.02);
            }

            // ── Umbral Bind DoT tick ──────────────────────────────────────────
            int bindTimer = player.getPersistentData().getInt(NBT_BIND_TIMER);
            if (bindTimer > 0) {
                bindTimer--;
                player.getPersistentData().putInt(NBT_BIND_TIMER, bindTimer);
                if (player.getPersistentData().contains(NBT_BIND_TARGET)) {
                    UUID bindUUID = player.getPersistentData().getUUID(NBT_BIND_TARGET);
                    sl.getEntitiesOfClass(LivingEntity.class,
                                    player.getBoundingBox().inflate(30),
                                    e -> e.getUUID().equals(bindUUID) && e.isAlive())
                            .forEach(e -> {
                                // Root
                                e.addEffect(new MobEffectInstance(
                                        MobEffects.MOVEMENT_SLOWDOWN, 25, 5, false, false));
                                // Consumption mode: DoT drain
                                if (getMode(player) == MODE_CONSUMPTION || inAbyssal(player)) {
                                    int dotTimer = e.getPersistentData()
                                            .getInt(NBT_BIND_DOT) - 1;
                                    if (dotTimer <= 0) {
                                        float dotDmg = 4f * essenceMult(player);
                                        e.hurt(sl.damageSources().playerAttack(player), dotDmg);
                                        e.invulnerableTime = 0;
                                        spendEssence(player, 2f); // drain essence
                                        addEssence(player, 1f);   // net slight gain
                                        sl.sendParticles(ParticleTypes.SOUL,
                                                e.getX(), e.getY() + 1, e.getZ(),
                                                3, 0.2, 0.2, 0.2, 0.02);
                                        dotTimer = BIND_DOT_INTERVAL;
                                    }
                                    e.getPersistentData().putInt(NBT_BIND_DOT, dotTimer);
                                }
                                // Reap mode: rapidly apply marks every 10 ticks
                                if ((getMode(player) == MODE_REAP || inAbyssal(player))
                                        && player.tickCount % 10 == 0) {
                                    addMarks(e, player, 1);
                                }
                                // Execution: lower threshold handled in tryExecute
                                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                        e.getX(), e.getY() + 1, e.getZ(),
                                        1, 0.15, 0.15, 0.15, 0.01);
                            });
                }
                if (bindTimer == 0) {
                    player.getPersistentData().remove(NBT_BIND_TARGET);
                }
            }

            // ── Passive Essence decay ─────────────────────────────────────────
            float essence = getEssence(player);
            if (essence > 0 && !inAbyssal(player)) {
                int decayTimer = player.getPersistentData()
                        .getInt(NBT_ESSENCE_DECAY) - 1;
                if (decayTimer <= 0) {
                    setEssence(player, essence - 1f);
                    decayTimer = ESSENCE_DECAY_TICK;
                }
                player.getPersistentData().putInt(NBT_ESSENCE_DECAY, decayTimer);
            }

            // ── HUD particles ─────────────────────────────────────────────────
            if (player.tickCount % 14 == 0 && essence > 10f) {
                int count = Math.max(1, (int)(essence / 30f));
                sl.sendParticles(ParticleTypes.SOUL,
                        player.getX(), player.getY() + 2.3, player.getZ(),
                        count, 0.25, 0.1, 0.25, 0.005);
            }
        }

        /** Amplify outgoing damage based on marks + essence. Gain essence on hit. */
        @SubscribeEvent(priority = EventPriority.NORMAL)
        public static void onDeathDescendantDamageDealt(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Death Descendant")) return;

            LivingEntity target = event.getEntity();
            int marks = getMarks(target);

            // Mark amplification
            if (marks > 0) event.setAmount(event.getAmount() * MARK_DMG_MULT[marks]);

            // Essence multiplier
            event.setAmount(event.getAmount() * essenceMult(player));

            // Gain essence per hit
            float essGain = getMode(player) == MODE_REAP ? 3f : 2f;
            if (inAbyssal(player)) essGain *= 1.5f;
            addEssence(player, essGain);

            // Reap: passively add 1 mark on every hit
            if (getMode(player) == MODE_REAP || inAbyssal(player))
                addMarks(target, player, 1);
        }

        /** Grant essence bonus when any marked entity dies. */
        @SubscribeEvent(priority = EventPriority.LOW)
        public static void onMarkedEntityDeath(LivingDeathEvent event) {
            if (!(event.getEntity() instanceof LivingEntity dead)) return;
            if (getMarks(dead) == 0) return;
            if (!dead.getPersistentData().contains(NBT_MARK_OWNER)) return;
            if (!(dead.level() instanceof ServerLevel sl)) return;

            UUID ownerUUID = dead.getPersistentData().getUUID(NBT_MARK_OWNER);
            sl.players().stream()
                    .filter(p -> p.getUUID().equals(ownerUUID)
                            && SoulCore.getAspect(p).equals("Death Descendant"))
                    .findFirst()
                    .ifPresent(owner -> {
                        int marks = getMarks(dead);
                        addEssence(owner, 5f + marks * 3f);
                        clearMarks(dead);
                    });
        }
    }

    // =========================================================================
    //  ABILITY 1 — SHADOW CUT
    // =========================================================================

    /**
     * Fast melee strike. Behaviour varies by mode:
     *   REAP:        applies 2–3 marks + moderate damage
     *   EXECUTION:   bonus damage per mark, attempts execute
     *   CONSUMPTION: spends 20 essence for massive damage spike
     * Cost: 200 soul-essence. Stage 0+.
     */
    public static void shadowCut(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Death Descendant")) return;
        if (SoulCore.getSoulEssence(player) < 1200) return;

        int ascStage = SoulCore.getAscensionStage(player);
        int mode     = getMode(player);
        int range    = inAbyssal(player) ? 10 + ascStage : 5 + ascStage;

        LivingEntity target = rayCastFirst(player, level, range);
        if (target == null) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo target in range!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 1200);
        if (hasModeBoost(player)) player.getPersistentData().putInt(NBT_MODE_BOOST, 0);

        int   marks  = getMarks(target);
        float dmg;
        String modeNote;

        switch (mode) {
            case MODE_REAP -> {
                int toAdd = inAbyssal(player) ? 3 : 2;
                addMarks(target, player, toAdd);
                dmg = (10f + ascStage * 2f) * essenceMult(player);
                modeNote = "§c[Reap] §f+" + toAdd + " marks → " + markStatus(target);
            }
            case MODE_EXECUTION -> {
                dmg = (10f + marks * 4f + ascStage * 2f) * essenceMult(player);
                if (inAbyssal(player)) dmg *= 1.4f;
                modeNote = "§4[Execution] §f×mark bonus (§e" + marks + "§f marks)";
            }
            case MODE_CONSUMPTION -> {
                float essSpend = inAbyssal(player) ? 0f : 20f;
                if (!spendEssence(player, essSpend) && !inAbyssal(player)) {
                    if (player instanceof ServerPlayer sp)
                        sp.sendSystemMessage(Component.literal("§cNeed 20 Essence for Consumption mode!"));
                    return;
                }
                dmg = (18f + ascStage * 3f) * essenceMult(player) * 1.5f;
                modeNote = "§5[Consumption] §fEssence consumed → spike damage";
            }
            default -> {
                dmg = (10f + ascStage * 2f) * essenceMult(player);
                modeNote = "";
            }
        }

        target.hurt(level.damageSources().playerAttack(player), dmg);
        target.invulnerableTime = 0;

        // Try execute in Execution mode or Abyssal
        if ((mode == MODE_EXECUTION || inAbyssal(player))
                && tryExecute(target, player, level, sl)) return;

        soulLine(sl, player.getEyePosition(), target.position().add(0, 1, 0));
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                target.getX(), target.getY() + 1, target.getZ(),
                6, 0.2, 0.2, 0.2, 0.04);
        level.playSound(null, target.blockPosition(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 0.8f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§8Shadow Cut: §f" + String.format("%.1f", dmg)
                            + " dmg. " + modeNote + ". " + essenceStatus(player)));
    }

    // =========================================================================
    //  ABILITY 2 — UMBRAL BIND
    // =========================================================================

    /**
     * Roots a target for 3 seconds.
     *   REAP:        rapidly builds marks every 0.5s
     *   EXECUTION:   lowers execute threshold for the duration
     *   CONSUMPTION: drains essence every 0.75s → deals DoT
     * Cost: 350 soul-essence. Requires stage ≥ 1.
     */
    public static void umbralBind(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Death Descendant")) return;
        if (SoulCore.getSoulEssence(player) < 350) return;
        if (SoulCore.getAscensionStage(player) < 1) return;

        int ascStage = SoulCore.getAscensionStage(player);
        LivingEntity target = rayCastFirst(player, level, 12 + ascStage);
        if (target == null) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo target in range!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 350);
        if (hasModeBoost(player)) player.getPersistentData().putInt(NBT_MODE_BOOST, 0);

        int bindDur = inAbyssal(player) ? BIND_DURATION + 40 : BIND_DURATION;
        player.getPersistentData().putUUID(NBT_BIND_TARGET, target.getUUID());
        player.getPersistentData().putInt(NBT_BIND_TIMER, bindDur);
        target.getPersistentData().putInt(NBT_BIND_DOT, BIND_DOT_INTERVAL);
        addEssence(player, 8f);

        String modeNote = switch (getMode(player)) {
            case MODE_REAP        -> "§c[Reap] §frapid mark buildup";
            case MODE_EXECUTION   -> "§4[Execution] §fexecute threshold lowered";
            case MODE_CONSUMPTION -> "§5[Consumption] §fEssence drain → DoT";
            default               -> "";
        };

        // Bind visual
        for (int i = 0; i < 12; i++) {
            double angle = Math.toRadians(i * 30);
            sl.sendParticles(ParticleTypes.SOUL,
                    target.getX() + 0.8 * Math.cos(angle),
                    target.getY() + 0.5,
                    target.getZ() + 0.8 * Math.sin(angle),
                    1, 0, 0.3, 0, 0.01);
        }
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                target.getX(), target.getY() + 1, target.getZ(),
                8, 0.2, 0.3, 0.2, 0.03);
        level.playSound(null, target.blockPosition(),
                SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 0.7f, 1.4f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§8Umbral Bind: §frooted §c" + target.getName().getString()
                            + " §ffor §b" + (bindDur / 20) + "s§f. "
                            + modeNote + ". " + essenceStatus(player)));
    }

    // =========================================================================
    //  ABILITY 3 — HARVEST
    // =========================================================================

    /**
     * Core ability. Consumes all marks on target for burst damage.
     * If target is below execute threshold → instant execution + large essence gain.
     * Cost: 500 soul-essence. Requires stage ≥ 2.
     */
    public static void harvest(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Death Descendant")) return;
        if (SoulCore.getSoulEssence(player) < 500) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        int ascStage = SoulCore.getAscensionStage(player);
        LivingEntity target = rayCastFirst(player, level, 14 + ascStage);
        if (target == null) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo target in range!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 500);
        if (hasModeBoost(player)) player.getPersistentData().putInt(NBT_MODE_BOOST, 0);

        // Try execute first
        if (tryExecute(target, player, level, sl)) return;

        // Otherwise consume marks for burst
        int marks = getMarks(target);
        clearMarks(target);

        float dmg = (16f + marks * 7f + ascStage * 3f) * essenceMult(player);
        if (inAbyssal(player)) dmg *= 1.5f;

        target.hurt(level.damageSources().playerAttack(player), dmg);
        target.invulnerableTime = 0;

        float essGain = 8f + marks * 4f;
        addEssence(player, essGain);

        // Large visual burst
        for (int i = 0; i < 16; i++) {
            double angle = Math.toRadians(i * 22.5);
            sl.sendParticles(ParticleTypes.SOUL,
                    target.getX() + 1.5 * Math.cos(angle),
                    target.getY() + 1,
                    target.getZ() + 1.5 * Math.sin(angle),
                    1, 0, 0.3, 0, 0.03);
        }
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                target.getX(), target.getY() + 1, target.getZ(),
                12, 0.4, 0.4, 0.4, 0.05);
        level.playSound(null, target.blockPosition(),
                SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 1f, 0.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§8Harvest: §fconsumed §e" + marks + " marks §f→ §c"
                            + String.format("%.1f", dmg) + " dmg§f, §b+"
                            + String.format("%.0f", essGain) + " Essence§f. "
                            + essenceStatus(player)));
    }

    // =========================================================================
    //  ABILITY 4 — VEIL STEP
    // =========================================================================

    /**
     * Teleports behind the target.
     *   REAP:        applies 2 marks on arrival
     *   EXECUTION:   crit multiplier on next hit (×1.8)
     *   CONSUMPTION: free recast if Essence ≥ 60
     * Cost: 300 soul-essence. Requires stage ≥ 0.
     */
    public static void veilStep(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Death Descendant")) return;
        if (SoulCore.getSoulEssence(player) < 300) return;

        int ascStage = SoulCore.getAscensionStage(player);
        int mode     = getMode(player);
        int range    = inAbyssal(player) ? 22 + ascStage : 14 + ascStage;

        LivingEntity target = rayCastFirst(player, level, range);
        if (target == null) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo target in range!"));
            return;
        }

        // Consumption: free if essence >= 60
        boolean free = mode == MODE_CONSUMPTION && getEssence(player) >= 60f
                && !inAbyssal(player);
        if (!free) SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 300);
        if (hasModeBoost(player)) player.getPersistentData().putInt(NBT_MODE_BOOST, 0);

        // Teleport behind target
        Vec3 behind = target.position()
                .add(target.getLookAngle().scale(-1.5))
                .add(0, 0.1, 0);
        if (player instanceof ServerPlayer sp) {
            sp.teleportTo(behind.x, behind.y, behind.z);
            // Face toward target
            Vec3 dir = target.position().subtract(behind).normalize();
            float yaw = (float)(Math.toDegrees(Math.atan2(-dir.x, dir.z)));
            sp.setYRot(yaw);
            sp.yRotO = yaw;
        }

        addEssence(player, 6f);

        switch (mode) {
            case MODE_REAP -> {
                addMarks(target, player, inAbyssal(player) ? 3 : 2);
                if (player instanceof ServerPlayer sp)
                    sp.sendSystemMessage(Component.literal(
                            "§8Veil Step §c[Reap]§f: teleported + §e+"
                                    + (inAbyssal(player) ? 3 : 2) + " marks §f→ "
                                    + markStatus(target)));
            }
            case MODE_EXECUTION -> {
                // Store a crit boost via mode-boost
                player.getPersistentData().putInt(NBT_MODE_BOOST, 40); // 2 seconds
                if (player instanceof ServerPlayer sp)
                    sp.sendSystemMessage(Component.literal(
                            "§8Veil Step §4[Execution]§f: teleported + §c×1.8 crit §fon next hit."));
            }
            case MODE_CONSUMPTION -> {
                String freeNote = free ? " §7(free recast!)" : "";
                if (player instanceof ServerPlayer sp)
                    sp.sendSystemMessage(Component.literal(
                            "§8Veil Step §5[Consumption]§f: teleported."
                                    + freeNote + " " + essenceStatus(player)));
            }
        }

        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                behind.x, behind.y + 1, behind.z, 10, 0.3, 0.3, 0.3, 0.04);
        sl.sendParticles(ParticleTypes.SOUL,
                target.getX(), target.getY() + 1, target.getZ(),
                6, 0.2, 0.2, 0.2, 0.03);
        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.4f);
    }

    // =========================================================================
    //  ABILITY 5 — SOUL FRACTURE
    // =========================================================================

    /**
     * Splits damage across all nearby enemies.
     *   REAP:        spreads 1 mark to each enemy hit
     *   EXECUTION:   spreads execution damage (kills low-HP targets)
     *   CONSUMPTION: spends 30 essence → AOE damage amplified ×1.6
     * Cost: 600 soul-essence. Requires stage ≥ 3.
     */
    public static void soulFracture(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Death Descendant")) return;
        if (SoulCore.getSoulEssence(player) < 600) return;
        if (SoulCore.getAscensionStage(player) < 3) return;

        int   ascStage = SoulCore.getAscensionStage(player);
        int   mode     = getMode(player);
        float radius   = 8f + ascStage * 0.5f + (inAbyssal(player) ? 4f : 0f);

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive());

        if (targets.isEmpty()) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo enemies in range!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 600);
        if (hasModeBoost(player)) player.getPersistentData().putInt(NBT_MODE_BOOST, 0);

        float baseDmg = 12f + ascStage * 2f;
        float aoeMult = 1.0f;

        if (mode == MODE_CONSUMPTION && !inAbyssal(player)) {
            if (spendEssence(player, 30f)) aoeMult = 1.6f;
        }
        if (inAbyssal(player)) aoeMult *= 1.5f;

        int executed   = 0;
        int totalHit   = 0;
        float totalDmg = 0f;

        for (LivingEntity e : targets) {
            int marks = getMarks(e);
            float dmg = baseDmg * MARK_DMG_MULT[marks] * essenceMult(player) * aoeMult;
            totalDmg += dmg;
            totalHit++;

            // REAP: spread 1 mark
            if (mode == MODE_REAP || inAbyssal(player))
                addMarks(e, player, 1);

            // EXECUTION: try execute on low-HP targets
            if ((mode == MODE_EXECUTION || inAbyssal(player))
                    && tryExecute(e, player, level, sl)) {
                executed++;
                continue;
            }

            e.hurt(level.damageSources().playerAttack(player), dmg);
            e.invulnerableTime = 0;
            sl.sendParticles(ParticleTypes.SOUL,
                    e.getX(), e.getY() + 1, e.getZ(), 4, 0.2, 0.2, 0.2, 0.03);

            // Lines from player to each target
            soulLine(sl, player.position().add(0, 1, 0),
                    e.position().add(0, 1, 0));
        }

        addEssence(player, 5f * executed);

        // Ring particles
        for (int i = 0; i < 28; i++) {
            double angle = Math.toRadians(i * (360.0 / 28));
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX() + radius * Math.cos(angle),
                    player.getY() + 0.5,
                    player.getZ() + radius * Math.sin(angle),
                    1, 0, 0.2, 0, 0.02);
        }
        level.playSound(null, player.blockPosition(),
                SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1f, 0.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§8Soul Fracture: §fhit §c" + totalHit + "§f enemies, §e"
                            + String.format("%.1f", totalDmg) + " total dmg"
                            + (executed > 0 ? ", §4" + executed + " executed" : "")
                            + ". " + essenceStatus(player)));
    }

    // =========================================================================
    //  ABILITY 6 — DARK FEAST
    // =========================================================================

    /**
     * Consumes 40 Essence to enter Dark Feast state for 5 seconds:
     *   - Heals 20% max HP
     *   - +30% damage (attribute)
     *   - +20% movement speed (attribute)
     * Cost: 500 soul-essence. Requires stage ≥ 5.
     */
    public static void darkFeast(Player player, ServerLevel sl) {
        if (player.isShiftKeyDown())return;
        if (!SoulCore.getAspect(player).equals("Death Descendant")) return;
        if (SoulCore.getSoulEssence(player) < 500) return;
        if (SoulCore.getAscensionStage(player) < 5) return;

        float essenceCost = inAbyssal(player) ? 0f : 40f;
        if (!spendEssence(player, essenceCost) && !inAbyssal(player)) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§cNeed 40 Plague Essence for Dark Feast!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 500);

        player.getPersistentData().putInt(NBT_DARK_FEAST, DARK_FEAST_DUR);

        // Heal
        float healAmt = player.getMaxHealth() * (inAbyssal(player) ? 0.35f : 0.20f);
        player.heal(healAmt);

        // Speed boost
        var spd = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null) {
            spd.removeModifier(FEAST_SPEED_ID);
            spd.addTransientModifier(new AttributeModifier(
                    FEAST_SPEED_ID, 0.05, AttributeModifier.Operation.ADD_VALUE));
        }

        // Damage boost
        var dmgAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmgAttr != null) {
            dmgAttr.removeModifier(FEAST_DAMAGE_ID);
            dmgAttr.addTransientModifier(new AttributeModifier(
                    FEAST_DAMAGE_ID, 3.0, AttributeModifier.Operation.ADD_VALUE));
        }

        // Night vision during feast
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,
                DARK_FEAST_DUR + 20, 0, false, false));

        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                player.getX(), player.getY() + 1, player.getZ(),
                20, 0.4, 0.4, 0.4, 0.05);
        sl.sendParticles(ParticleTypes.SOUL,
                player.getX(), player.getY() + 1, player.getZ(),
                15, 0.5, 0.5, 0.5, 0.04);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.6f, 1.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5§lDark Feast! §r§fHealed §a+" + String.format("%.1f", healAmt)
                            + " HP§f, §c+30% dmg§f, §b+20% speed §ffor §b5s§f. "
                            + essenceStatus(player)));
    }

    // =========================================================================
    //  ABILITY 7 — ABYSSAL HARVEST (ULTIMATE)
    // =========================================================================

    /**
     * 12-second ultimate:
     *   - All nearby enemies auto-gain marks every second
     *   - Executions trigger instantly at 45% HP threshold
     *   - Essence constantly refills (+3 every 0.5s)
     *   - All abilities enhanced in all modes simultaneously
     *   - Every execution causes AOE soul explosion
     * Cost: 5000 soul-essence. Requires stage ≥ 7.
     */
    public static void abyssalHarvest(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Death Descendant")) return;
        if (SoulCore.getSoulEssence(player) < 5000) return;
        if (SoulCore.getAscensionStage(player) < 7) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 5000);

        player.getPersistentData().putInt(NBT_ABYSSAL, ABYSSAL_DUR);
        setEssence(player, ESSENCE_MAX);

        // Immediately mark all nearby enemies at max stacks
        int ascStage = SoulCore.getAscensionStage(player);
        float radius = 20f + ascStage;
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive());

        for (LivingEntity e : targets) {
            setMarks(e, MARKS_MAX);
            e.getPersistentData().putUUID(NBT_MARK_OWNER, player.getUUID());
            sl.sendParticles(ParticleTypes.SOUL,
                    e.getX(), e.getY() + 1, e.getZ(), 8, 0.3, 0.3, 0.3, 0.04);
        }

        // Grand visual
        for (int i = 0; i < 40; i++) {
            double angle = Math.toRadians(i * 9);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX() + (radius * 0.4) * Math.cos(angle),
                    player.getY() + 1,
                    player.getZ() + (radius * 0.4) * Math.sin(angle),
                    1, 0, 0.4, 0, 0.03);
        }
        sl.sendParticles(ParticleTypes.SOUL,
                player.getX(), player.getY() + 1, player.getZ(),
                40, radius * 0.3, 1.0, radius * 0.3, 0.04);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(), 3, 0.5, 0.3, 0.5, 0);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.5f, 0.3f);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1f, 0.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§4§l☠ ABYSSAL HARVEST ☠ §r§f" + targets.size()
                            + " enemies §cmax-marked§f. "
                            + "Execute at §e45% HP§f. "
                            + "Essence refilling. §b12s."));
    }

    // =========================================================================
    //  ABILITY 4-BONUS — MODE SHIFT (Veil Step companion)
    // =========================================================================

    /**
     * Cycle Harvest Mode: REAP → EXECUTION → CONSUMPTION → REAP.
     * Grants a 4-second mode-boost empowerment on the new mode.
     * Cost: 150 soul-essence.
     */
    public static void shiftMode(Player player, ServerLevel sl) {
        if (!player.isShiftKeyDown())return;
        if (!SoulCore.getAspect(player).equals("Death Descendant")) return;
        if (SoulCore.getSoulEssence(player) < 150) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 150);

        int nextMode = (getMode(player) + 1) % 3;
        player.getPersistentData().putInt(NBT_MODE, nextMode);
        player.getPersistentData().putInt(NBT_MODE_BOOST, MODE_BOOST_DUR);
        addEssence(player, 5f);

        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                player.getX(), player.getY() + 1, player.getZ(),
                8, 0.3, 0.3, 0.3, 0.04);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 0.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§8Mode Shift: §f→ " + modeName(nextMode)
                            + " §7| next ability §e×1.20 empowered§7. "
                            + essenceStatus(player)));
    }
}