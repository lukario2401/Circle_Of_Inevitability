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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
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
 * Bloodbound Ascetic
 * -----------------------------------------------
 * HP is the resource. The lower your HP → the stronger you become.
 * You willingly hurt yourself to accumulate Blood Debt and gain power.
 *
 * Core Mechanic — BLOOD DEBT (0–100):
 *   0–20   → Normal
 *   20–40  → Increased damage
 *   40–60  → High damage + lifesteal
 *   60–80  → Extreme damage ⚠ (incoming damage also increased)
 *   80–90  → 30% chance to negate incoming damage hits
 *
 * Player NBT keys:
 *   "BloodDebt"            → float  current blood debt (0–100)
 *   "BloodDebtDecayTimer"  → int    ticks until next passive decay
 *   "BloodBleedTarget"     → UUID   entity with bleed applied
 *   "BloodBleedTimer"      → int    ticks remaining on bleed
 *   "BloodDrainTarget"     → UUID   entity being drained (Sanguine Drain)
 *   "BloodDrainTimer"      → int    ticks remaining on drain link
 *   "BloodLastStand"       → int    ticks remaining on Last Stand buff
 *   "BloodFrenzy"          → int    ticks remaining on Blood Frenzy
 *   "BloodMartyr"          → int    ticks remaining on Martyr's Ascension
 */
public class BloodBoundAscetic {

    // ─── NBT keys ─────────────────────────────────────────────────────────────
    private static final String NBT_DEBT          = "BloodDebt";
    private static final String NBT_DECAY_TIMER   = "BloodDebtDecayTimer";
    private static final String NBT_BLEED_TARGET  = "BloodBleedTarget";
    private static final String NBT_BLEED_TIMER   = "BloodBleedTimer";
    private static final String NBT_DRAIN_TARGET  = "BloodDrainTarget";
    private static final String NBT_DRAIN_TIMER   = "BloodDrainTimer";
    private static final String NBT_LAST_STAND    = "BloodLastStand";
    private static final String NBT_FRENZY        = "BloodFrenzy";
    private static final String NBT_MARTYR        = "BloodMartyr";

    // ─── Attribute modifier IDs ───────────────────────────────────────────────
    private static final ResourceLocation FRENZY_BLOCK_RANGE_ID  =
            ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "frenzy_block_range");

    private static final ResourceLocation FRENZY_ENTITY_RANGE_ID =
            ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "frenzy_entity_range");

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final float DEBT_MAX           = 100f;
    private static final int   DECAY_INTERVAL     = 50;  // lose 1 debt every 2.5s
    private static final int   BLEED_INTERVAL     = 20;  // bleed ticks every second
    private static final int   DRAIN_INTERVAL     = 10;  // drain ticks every 0.5s
    private static final int   MARTYR_DURATION    = 200; // 10 seconds
    private static final int   FRENZY_DURATION    = 120; // 6 seconds
    private static final int   LAST_STAND_DURATION = 100; // 5 seconds

    private static final Random RNG = new Random();

    // ─── Debt Helpers ─────────────────────────────────────────────────────────

    public static float getDebt(Player player) {
        return player.getPersistentData().getFloat(NBT_DEBT);
    }

    public static void setDebt(Player player, float debt) {
        player.getPersistentData().putFloat(NBT_DEBT, Math.max(0f, Math.min(DEBT_MAX, debt)));
    }

    private static void addDebt(Player player, float amount) {
        setDebt(player, getDebt(player) + amount);
    }

    public static boolean inMartyr(Player player) {
        return player.getPersistentData().getInt(NBT_MARTYR) > 0;
    }

    public static boolean inFrenzy(Player player) {
        return player.getPersistentData().getInt(NBT_FRENZY) > 0;
    }

    public static boolean inLastStand(Player player) {
        return player.getPersistentData().getInt(NBT_LAST_STAND) > 0;
    }

    /** Damage multiplier from Blood Debt. */
    private static float damageMult(Player player) {
        if (inMartyr(player)) return 3.0f;
        float debt = getDebt(player);
        float hpPct = player.getHealth() / player.getMaxHealth();

        float mult = 1.0f;
        if (debt >= 80f) mult = 2.2f;
        else if (debt >= 60f) mult = 1.8f;
        else if (debt >= 40f) mult = 1.5f;
        else if (debt >= 20f) mult = 1.2f;

        // Lower HP = more power (up to ×1.5 bonus at 10% HP)
        if (hpPct < 0.10f) mult *= 1.5f;
        else if (hpPct < 0.25f) mult *= 1.3f;
        else if (hpPct < 0.50f) mult *= 1.15f;

        if (inLastStand(player)) mult *= 1.6f;
        if (inFrenzy(player))    mult *= 1.4f;

        return mult;
    }

    /** Lifesteal rate (0.0–0.25) based on debt tier. */
    private static float lifestealRate(Player player) {
        if (inMartyr(player)) return 0.30f;
        float debt = getDebt(player);
        if (inLastStand(player)) return 0.20f;
        if (debt >= 60f) return 0.15f;
        if (debt >= 40f) return 0.10f;
        return 0f;
    }

    /** Coloured debt tier tag. */
    private static String debtTag(float debt) {
        if (debt >= 80f) return "§5[Blood Pact]";
        if (debt >= 60f) return "§4[Extreme ⚠]";
        if (debt >= 40f) return "§c[High]";
        if (debt >= 20f) return "§6[Rising]";
        return "§7[Low]";
    }

    private static String debtStatus(Player player) {
        float d = getDebt(player);
        return debtTag(d) + " §fDebt: §b" + String.format("%.0f", d) + "/100";
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

    // ─── Range attribute helpers ──────────────────────────────────────────────

    private static void applyRangeBoost(Player player) {
        var blockRange  = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        var entityRange = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (blockRange != null && blockRange.getModifier(FRENZY_BLOCK_RANGE_ID) == null) {
            blockRange.addTransientModifier(new AttributeModifier(
                    FRENZY_BLOCK_RANGE_ID, 2.0, AttributeModifier.Operation.ADD_VALUE));
        }
        if (entityRange != null && entityRange.getModifier(FRENZY_ENTITY_RANGE_ID) == null) {
            entityRange.addTransientModifier(new AttributeModifier(
                    FRENZY_ENTITY_RANGE_ID, 2.0, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void removeRangeBoost(Player player) {
        var blockRange  = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        var entityRange = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (blockRange  != null) blockRange.removeModifier(FRENZY_BLOCK_RANGE_ID);
        if (entityRange != null) entityRange.removeModifier(FRENZY_ENTITY_RANGE_ID);
    }

    // =========================================================================
    //  EVENTS — single class, unique name
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class BloodEvents {

        @SubscribeEvent
        public static void onBloodPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Bloodbound Ascetic")) return;

            float debt = getDebt(player);

            // ── Martyr's Ascension countdown ──────────────────────────────────
            int martyr = player.getPersistentData().getInt(NBT_MARTYR);
            if (martyr > 0) {
                player.getPersistentData().putInt(NBT_MARTYR, martyr - 1);
                // Lock HP at 1
                if (player.getHealth() < 1f) player.setHealth(1f);
                // Extreme lifesteal handled in damage event
                if (player.tickCount % 5 == 0)
                    sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                            player.getX(), player.getY() + 1, player.getZ(), 6, 0.5, 0.5, 0.5, 0.05);
                if (martyr == 1) {
                    // Ascension ended — restore debt to full after
                    setDebt(player, DEBT_MAX);
                }
            }

            // ── Blood Frenzy countdown ────────────────────────────────────────
            int frenzy = player.getPersistentData().getInt(NBT_FRENZY);
            if (frenzy > 0) {
                player.getPersistentData().putInt(NBT_FRENZY, frenzy - 1);
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 5, 2, true, false));
                applyRangeBoost(player);
                if (player.tickCount % 8 == 0)
                    sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                            player.getX(), player.getY() + 1, player.getZ(), 3, 0.3, 0.3, 0.3, 0.03);
            } else {
                removeRangeBoost(player);
            }

            // ── Last Stand countdown ──────────────────────────────────────────
            int lastStand = player.getPersistentData().getInt(NBT_LAST_STAND);
            if (lastStand > 0) {
                player.getPersistentData().putInt(NBT_LAST_STAND, lastStand - 1);
                if (player.tickCount % 6 == 0)
                    sl.sendParticles(ParticleTypes.CRIT,
                            player.getX(), player.getY() + 1, player.getZ(), 3, 0.3, 0.3, 0.3, 0.04);
            }

            // ── Bleed tick ────────────────────────────────────────────────────
            int bleedTimer = player.getPersistentData().getInt(NBT_BLEED_TIMER);
            if (bleedTimer > 0) {
                bleedTimer--;
                player.getPersistentData().putInt(NBT_BLEED_TIMER, bleedTimer);
                if (player.getPersistentData().contains(NBT_BLEED_TARGET)) {
                    UUID bleedUUID = player.getPersistentData().getUUID(NBT_BLEED_TARGET);
                    if (bleedTimer % BLEED_INTERVAL == 0) {
                        sl.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(30),
                                        e -> e.getUUID().equals(bleedUUID) && e.isAlive())
                                .forEach(e -> {
                                    e.hurt(sl.damageSources().playerAttack(player), 3.5f * damageMult(player));
                                    e.invulnerableTime = 0;
                                    sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                                            e.getX(), e.getY() + 1, e.getZ(), 4, 0.2, 0.2, 0.2, 0.02);
                                });
                    }
                }
                if (bleedTimer == 0) player.getPersistentData().remove(NBT_BLEED_TARGET);
            }

            // ── Sanguine Drain tick ───────────────────────────────────────────
            int drainTimer = player.getPersistentData().getInt(NBT_DRAIN_TIMER);
            if (drainTimer > 0) {
                drainTimer--;
                player.getPersistentData().putInt(NBT_DRAIN_TIMER, drainTimer);
                if (player.getPersistentData().contains(NBT_DRAIN_TARGET)
                        && player.tickCount % DRAIN_INTERVAL == 0) {
                    UUID drainUUID = player.getPersistentData().getUUID(NBT_DRAIN_TARGET);
                    sl.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(20),
                                    e -> e.getUUID().equals(drainUUID) && e.isAlive())
                            .forEach(e -> {
                                float drainDmg = 2.0f;
                                e.hurt(sl.damageSources().playerAttack(player), drainDmg);
                                e.invulnerableTime = 0;
                                player.heal(drainDmg * 0.8f);
                                addDebt(player, 1f);
                                // Line particles from target to player
                                Vec3 from = e.position().add(0, 1, 0);
                                Vec3 to   = player.position().add(0, 1, 0);
                                Vec3 d    = to.subtract(from).normalize();
                                Vec3 c    = from;
                                double dist = from.distanceTo(to);
                                for (double dd = 0; dd < dist; dd += 0.6) {
                                    sl.sendParticles(ParticleTypes.CRIMSON_SPORE, c.x, c.y, c.z, 1, 0.05, 0.05, 0.05, 0);
                                    c = c.add(d.scale(0.6));
                                }
                            });
                }
                if (drainTimer == 0) player.getPersistentData().remove(NBT_DRAIN_TARGET);
            }

            // ── Passive debt decay ────────────────────────────────────────────
            if (debt > 0 && !inMartyr(player)) {
                int decayTimer = player.getPersistentData().getInt(NBT_DECAY_TIMER);
                decayTimer--;
                if (decayTimer <= 0) {
                    setDebt(player, debt - 1f);
                    decayTimer = DECAY_INTERVAL;
                }
                player.getPersistentData().putInt(NBT_DECAY_TIMER, decayTimer);
            }

            // ── Auto trigger Last Stand at low HP ─────────────────────────────
            float hpPct = player.getHealth() / player.getMaxHealth();
            if (hpPct < 0.20f && !inLastStand(player) && !inMartyr(player)
                    && SoulCore.getAscensionStage(player) >= 4) {
                player.getPersistentData().putInt(NBT_LAST_STAND, LAST_STAND_DURATION);
                if (player instanceof ServerPlayer sp)
                    sp.sendSystemMessage(Component.literal("§4§lLAST STAND! §r§cDamage massively increased!"));
                sl.sendParticles(ParticleTypes.CRIT,
                        player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.06);
            }

            // ── HUD particles (debt indicator above head) ─────────────────────
            if (player.tickCount % 8 == 0 && debt > 0) {
                int count = Math.max(1, (int)(debt / 20f));
                sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                        player.getX(), player.getY() + 2.2, player.getZ(),
                        count, 0.3, 0.1, 0.3, 0.01);
            }
        }

        /** Modify incoming damage based on Blood Debt. */
        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void onBloodDamageTaken(LivingHurtEvent event) {
            if (!(event.getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Bloodbound Ascetic")) return;

            float debt = getDebt(player);

            // Martyr: cannot die, negate all damage
            if (inMartyr(player)) {
                event.setCanceled(true);
                return;
            }

            // 80–90 debt: 30% chance to negate hit entirely
            if (debt >= 80f && RNG.nextFloat() < 0.30f) {
                event.setCanceled(true);
                if (player.level() instanceof ServerLevel sl)
                    sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                            player.getX(), player.getY() + 1, player.getZ(), 6, 0.3, 0.3, 0.3, 0.04);
                return;
            }

            // 60–80 debt: incoming damage increased ⚠
            if (debt >= 60f) {
                event.setAmount(event.getAmount() * 1.25f);
            }

            // Build debt from taking hits
            addDebt(player, event.getAmount() * 0.3f);
        }

        /** Outgoing damage: apply multiplier + lifesteal + debt building. */
        @SubscribeEvent(priority = EventPriority.NORMAL)
        public static void onBloodDamageDealt(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Bloodbound Ascetic")) return;

            LivingEntity target = event.getEntity();

            // Apply multiplier
            float mult = damageMult(player);
            if (inFrenzy(player)) mult *= 1.4f;
            event.setAmount(event.getAmount() * mult);

            // Lifesteal
            float ls = lifestealRate(player);
            if (ls > 0f) player.heal(event.getAmount() * ls);

            // Martyr: every hit heals slightly
            if (inMartyr(player)) player.heal(1.5f);

            // Build small debt from dealing damage
            addDebt(player, 1.5f);
        }
    }

    // =========================================================================
    //  ABILITY 1 — BLOOD SLASH
    // =========================================================================

    /**
     * Melee slash. Costs a small % of current HP. Applies bleed.
     * Deals bonus damage scaled to current Blood Debt.
     * Cost: HP (3% of max), 200 essence.
     */
    public static void bloodSlash(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Bloodbound Ascetic")) return;
        if (SoulCore.getSoulEssence(player) < 200) return;

        int   stage   = SoulCore.getAscensionStage(player);
        float hpCost  = inFrenzy(player) ? player.getMaxHealth() * 0.05f
                : player.getMaxHealth() * 0.03f;

        if (!inMartyr(player) && player.getHealth() <= hpCost + 1f) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNot enough HP for Blood Slash!"));
            return;
        }

        LivingEntity target = rayCastFirst(player, level, 5 + stage);
        if (target == null) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 200);
        if (!inMartyr(player)) player.hurt(player.level().damageSources().magic(), hpCost);
        addDebt(player, 8f);

        float damage = (14.0f + stage * 2) * damageMult(player);
        target.hurt(level.damageSources().playerAttack(player), damage);
        target.invulnerableTime = 0;

        // Apply bleed
        player.getPersistentData().putUUID(NBT_BLEED_TARGET, target.getUUID());
        player.getPersistentData().putInt(NBT_BLEED_TIMER, 100);

        sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                target.getX(), target.getY() + 1, target.getZ(), 12, 0.4, 0.4, 0.4, 0.05);
        level.playSound(null, target.blockPosition(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 0.7f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§4Blood Slash: §f" + String.format("%.1f", damage)
                            + " dmg + §cbleed §7(-" + String.format("%.1f", hpCost) + " HP). "
                            + debtStatus(player)));
    }

    // =========================================================================
    //  ABILITY 2 — CRIMSON OFFERING
    // =========================================================================

    /**
     * Sacrifice HP to rapidly build Blood Debt.
     * Costs 15% max HP. Gains ~25 debt. Heals slightly over time.
     * Cost: 300 essence.  Requires stage ≥ 1.
     */
    public static void crimsonOffering(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Bloodbound Ascetic")) return;
        if (SoulCore.getSoulEssence(player) < 300) return;
        if (SoulCore.getAscensionStage(player) < 1) return;

        float hpCost = player.getMaxHealth() * (inFrenzy(player) ? 0.20f : 0.15f);
        if (!inMartyr(player) && player.getHealth() <= hpCost + 1f) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNot enough HP for Crimson Offering!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 300);
        if (!inMartyr(player)) player.hurt(player.level().damageSources().magic(), hpCost);
        addDebt(player, 25f);

        // Heal over time via regen
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 80, 0, false, true));

        sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.06);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.GENERIC_HURT, SoundSource.PLAYERS, 1f, 0.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§4Crimson Offering: §c-" + String.format("%.1f", hpCost)
                            + " HP §4→ §b+25 Debt §7+ regen. " + debtStatus(player)));
    }

    // =========================================================================
    //  ABILITY 3 — HEMORRHAGE BURST
    // =========================================================================

    /**
     * AOE explosion scaling with current Blood Debt.
     * Consumes 40 debt. Radius and damage scale with debt spent.
     * Cost: 600 essence.  Requires stage ≥ 2.
     */
    public static void hemorrhageBurst(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Bloodbound Ascetic")) return;
        if (SoulCore.getSoulEssence(player) < 600) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        float debt = getDebt(player);
        if (debt < 5f) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNeed at least 5 Blood Debt!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 600);

        int   stage    = SoulCore.getAscensionStage(player);
        float consumed = Math.min(debt, 40f);
        float damage   = (12.0f + consumed * 0.7f + stage * 2) * damageMult(player);
        float radius   = 3.5f + consumed / 15f + stage * 0.4f;

        setDebt(player, debt - consumed);

        AABB box = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, box, e -> e != player && e.isAlive());

        for (LivingEntity e : targets) {
            e.hurt(level.damageSources().playerAttack(player), damage);
            e.invulnerableTime = 0;
            Vec3 push = e.position().subtract(player.position()).normalize().scale(0.6);
            e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.25, push.z));
            sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    e.getX(), e.getY() + 1, e.getZ(), 8, 0.3, 0.3, 0.3, 0.04);
        }

        // Ring particles
        for (int i = 0; i < 24; i++) {
            double angle = (2 * Math.PI / 24) * i;
            double px = player.getX() + radius * Math.cos(angle);
            double pz = player.getZ() + radius * Math.sin(angle);
            sl.sendParticles(ParticleTypes.CRIMSON_SPORE, px, player.getY() + 0.5, pz, 1, 0, 0.2, 0, 0.02);
        }
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, player.blockPosition(),
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2f, 0.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§4Hemorrhage Burst: consumed §c" + String.format("%.0f", consumed)
                            + " debt §4→ §f" + String.format("%.1f", damage)
                            + " dmg r" + String.format("%.1f", radius)
                            + ", hit §c" + targets.size() + "§f. " + debtStatus(player)));
    }

    // =========================================================================
    //  ABILITY 4 — SANGUINE DRAIN
    // =========================================================================

    /**
     * Links to an enemy for 5 seconds. Drains HP every 0.5s, heals you,
     * and slowly builds Blood Debt.
     * Cost: 400 essence.  Requires stage ≥ 3.
     */
    public static void sanguineDrain(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Bloodbound Ascetic")) return;
        if (SoulCore.getSoulEssence(player) < 400) return;
        if (SoulCore.getAscensionStage(player) < 3) return;

        int stage = SoulCore.getAscensionStage(player);
        LivingEntity target = rayCastFirst(player, level, 14 + stage);
        if (target == null) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 400);

        player.getPersistentData().putUUID(NBT_DRAIN_TARGET, target.getUUID());
        player.getPersistentData().putInt(NBT_DRAIN_TIMER, 100); // 5 seconds

        sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                target.getX(), target.getY() + 1, target.getZ(), 10, 0.3, 0.3, 0.3, 0.04);
        level.playSound(null, target.blockPosition(),
                SoundEvents.GENERIC_EAT, SoundSource.HOSTILE, 0.8f, 0.4f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§4Sanguine Drain: §clinked to §f" + target.getName().getString()
                            + "§c for §b5s§c. Draining HP. " + debtStatus(player)));
    }

    // =========================================================================
    //  ABILITY 5 — LAST STAND
    // =========================================================================

    /**
     * Manually trigger Last Stand (also auto-triggers below 20% HP at stage ≥ 4).
     * For 5 seconds: massive damage boost, boosted lifesteal.
     * Cost: 500 essence.  Requires stage ≥ 4.
     */
    public static void lastStand(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Bloodbound Ascetic")) return;
        if (SoulCore.getSoulEssence(player) < 500) return;
        if (SoulCore.getAscensionStage(player) < 4) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 500);

        player.getPersistentData().putInt(NBT_LAST_STAND, LAST_STAND_DURATION);
        addDebt(player, 15f);

        sl.sendParticles(ParticleTypes.CRIT,
                player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.06);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 0.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§4§lLAST STAND! §r§cDamage ×1.6, lifesteal ×2 for §b5s§c. "
                            + debtStatus(player)));
    }

    // =========================================================================
    //  ABILITY 6 — BLOOD FRENZY
    // =========================================================================

    /**
     * For 6 seconds:
     *   - Abilities cost more HP but deal ×1.4 more damage
     *   - Attack speed increased (Haste III)
     *   - Block and entity interaction range +2
     * Cost: 800 essence.  Requires stage ≥ 5.
     */
    public static void bloodBoundAsceticBloodFrenzy(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Bloodbound Ascetic")) return;
        if (SoulCore.getSoulEssence(player) < 800) return;
        if (SoulCore.getAscensionStage(player) < 5) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 800);

        player.getPersistentData().putInt(NBT_FRENZY, FRENZY_DURATION);
        addDebt(player, 20f);

        sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.05);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 0.4f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§4§lBlood Frenzy! §r§c×1.4 dmg, Haste III, +2 range for §b6s§c. "
                            + debtStatus(player)));
    }

    // =========================================================================
    //  ABILITY 7 — MARTYR'S ASCENSION
    // =========================================================================

    /**
     * 10-second ultimate:
     *   - HP locked at 1 (cannot die)
     *   - Damage ×3.0 (from damageMult)
     *   - Extreme lifesteal (30%)
     *   - Blood Debt maxed at 100
     *   - Every hit heals 1.5 HP
     * Cost: 5000 essence.  Requires stage ≥ 7.
     */
    public static void martyrsAscension(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Bloodbound Ascetic")) return;
        if (SoulCore.getSoulEssence(player) < 5000) return;
        if (SoulCore.getAscensionStage(player) < 7) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 5000);

        player.getPersistentData().putInt(NBT_MARTYR, MARTYR_DURATION);
        setDebt(player, DEBT_MAX);

        // Drop HP to 1 immediately as the price of ascension
        player.setHealth(1f);

        sl.sendParticles(ParticleTypes.CRIMSON_SPORE,
                player.getX(), player.getY() + 1, player.getZ(), 60, 1.0, 1.0, 1.0, 0.08);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(), 3, 0.4, 0.4, 0.4, 0);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5f, 0.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§4§l✦ MARTYR'S ASCENSION ✦ §r§cHP locked at 1. ×3 damage. Extreme lifesteal. 10s."));
    }
}