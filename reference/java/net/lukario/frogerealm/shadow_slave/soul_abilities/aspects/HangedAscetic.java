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
 * Hanged Ascetic
 * -----------------------------------------------
 * Theme: Shadows, Grazing souls, Flesh & Blood, Degeneration/Depravity,
 *        Profane Language — inspired by the Hanged Man pathway.
 * Role: DPS / Control hybrid (corruption spread + soul harvesting)
 *
 * Sneaking while using an ability triggers a DEPRAVITY EMPOWERED version:
 *   Consumes a portion of current Depravity (never all of it).
 *   The more Depravity consumed → the stronger the empowered effect.
 *   Minimum 15 Depravity required to trigger empowered mode.
 *
 * Player NBT keys:
 *   "HADepravity"           → float  0–100
 *   "HADepravityDecay"      → int    ticks until next decay
 *   "HAGrazedSouls"         → int    0–3 grazed soul count
 *   "HAShadowLurk"          → int    ticks remaining on Shadow Lurk
 *   "HAShadowLurkCrit"      → bool   next strike is a crit from lurk
 *   "HACursedTarget"        → UUID   entity under Shadow Curse
 *   "HACurseTimer"          → int    ticks remaining on curse DoT
 *   "HAProfaneBound"        → UUID   entity bound by Profane Language
 *   "HAProfaneTimer"        → int    ticks remaining on Profane bind
 *   "HADescentActive"       → int    ticks remaining on Descent
 *   "HADescentSelfDmgCd"    → int    ticks until next self-damage during descent
 *
 * Entity NBT keys:
 *   "HACorrodedStacks"      → int    0–5 corrode stacks
 *   "HACorrodedOwner"       → UUID   player who applied stacks
 */
public class HangedAscetic {

    // ─── NBT keys (player) ────────────────────────────────────────────────────
    private static final String NBT_DEPRAVITY       = "HADepravity";
    private static final String NBT_DEP_DECAY       = "HADepravityDecay";
    private static final String NBT_GRAZED_SOULS    = "HAGrazedSouls";
    private static final String NBT_SHADOW_LURK     = "HAShadowLurk";
    private static final String NBT_LURK_CRIT       = "HAShadowLurkCrit";
    private static final String NBT_CURSE_TARGET    = "HACursedTarget";
    private static final String NBT_CURSE_TIMER     = "HACurseTimer";
    private static final String NBT_PROFANE_BOUND   = "HAProfaneBound";
    private static final String NBT_PROFANE_TIMER   = "HAProfaneTimer";
    private static final String NBT_DESCENT_ACTIVE  = "HADescentActive";
    private static final String NBT_DESCENT_SELF_CD = "HADescentSelfDmgCd";

    // ─── NBT keys (entity) ────────────────────────────────────────────────────
    private static final String NBT_CORRODE_STACKS  = "HACorrodedStacks";
    private static final String NBT_CORRODE_OWNER   = "HACorrodedOwner";

    // ─── Attribute modifier ResourceLocations (1.21 API) ─────────────────────
    private static final ResourceLocation SOUL_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "ha_soul_damage");
    private static final ResourceLocation SOUL_ARMOR_ID =
            ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "ha_soul_armor");
    private static final ResourceLocation LURK_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "ha_lurk_speed");
    private static final ResourceLocation DESCENT_DAMAGE_ID =
            ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "ha_descent_damage");

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final float DEP_MAX             = 300f;
    private static final float MADNESS_THRESHOLD   = 210f;
    private static final float ENHANCED_THRESHOLD  = 140f;
    private static final float BONUS_THRESHOLD     = 60f;
    private static final int   DECAY_INTERVAL      = 80;
    private static final int   GRAZED_MAX          = 10;
    private static final int   CURSE_DURATION      = 240;
    private static final int   CURSE_DOT_INTERVAL  = 20;
    private static final int   LURK_DURATION       = 240;
    private static final int   PROFANE_DURATION    = 200;
    private static final int   DESCENT_DURATION    = 480;
    private static final int   SELF_DMG_INTERVAL   = 100;
    private static final int   CORRODE_MAX         = 20;

    // ─── Empowered sneak constants ────────────────────────────────────────────
    /** Minimum Depravity required to trigger empowered mode. */
    private static final float EMPOWER_MIN_DEP     = 30f;
    /** Fraction of current Depravity consumed on empowered cast (never all). */
    private static final float EMPOWER_CONSUME_PCT = 0.30f;

    private static final Random RNG = new Random();

    // =========================================================================
    //  CLASS CHECK
    // =========================================================================

    private static boolean canUseClassHangedAscetic(Player player, Boolean dontCheck) {
        if (dontCheck) return true;
        return SoulCore.getAspect(player).equals("Hanged Ascetic");
    }

    // =========================================================================
    //  DEPRAVITY HELPERS
    // =========================================================================

    public static float getDepravity(Player player) {
        return player.getPersistentData().getFloat(NBT_DEPRAVITY);
    }

    public static void setDepravity(Player player, float val) {
        player.getPersistentData().putFloat(NBT_DEPRAVITY,
                Math.max(0f, Math.min(DEP_MAX, val)));
    }

    public static void addDepravity(Player player, float amount) {
        setDepravity(player, getDepravity(player) + amount);
    }

    public static boolean inMadness(Player player) {
        return getDepravity(player) >= MADNESS_THRESHOLD || inDescent(player);
    }

    public static boolean isEnhanced(Player player) {
        return getDepravity(player) >= ENHANCED_THRESHOLD || inDescent(player);
    }

    public static boolean inDescent(Player player) {
        return player.getPersistentData().getInt(NBT_DESCENT_ACTIVE) > 0;
    }

    public static boolean inLurk(Player player) {
        return player.getPersistentData().getInt(NBT_SHADOW_LURK) > 0;
    }

    public static int getGrazedSouls(Player player) {
        return player.getPersistentData().getInt(NBT_GRAZED_SOULS);
    }

    private static void addGrazedSoul(Player player, ServerLevel sl) {
        int current = getGrazedSouls(player);
        if (current >= GRAZED_MAX) return;
        player.getPersistentData().putInt(NBT_GRAZED_SOULS, current + 1);
        applyGrazedSoulPassives(player);
        sl.sendParticles(ParticleTypes.SOUL,
                player.getX(), player.getY() + 1, player.getZ(),
                10, 0.3, 0.3, 0.3, 0.04);
        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5Soul Grazed! §f[§e" + (current + 1) + "§f/" + GRAZED_MAX + " souls] "
                            + soulPassiveDesc(current + 1)));
    }

    private static void removeGrazedSoul(Player player, ServerLevel sl) {
        int current = getGrazedSouls(player);
        if (current < 1) return;
        player.getPersistentData().putInt(NBT_GRAZED_SOULS, current - 1);
        applyGrazedSoulPassives(player);
        sl.sendParticles(ParticleTypes.SOUL,
                player.getX(), player.getY() + 1, player.getZ(),
                10, 0.3, 0.3, 0.3, 0.04);
        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal("§5Grazed Soul Lost! | " + (current-1) + " Souls left"));
    }

    private static void applyGrazedSoulPassives(Player player) {
        int souls = getGrazedSouls(player);
        var dmgAttr   = player.getAttribute(Attributes.ATTACK_DAMAGE);
        var armorAttr = player.getAttribute(Attributes.ARMOR);
        if (dmgAttr != null) {
            dmgAttr.removeModifier(SOUL_DAMAGE_ID);
            dmgAttr.addTransientModifier(new AttributeModifier(
                    SOUL_DAMAGE_ID, souls * 1.5, AttributeModifier.Operation.ADD_VALUE));
        }
        if (armorAttr != null) {
            armorAttr.removeModifier(SOUL_ARMOR_ID);
            armorAttr.addTransientModifier(new AttributeModifier(
                    SOUL_ARMOR_ID, souls * 0.5, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void clearGrazedSouls(Player player) {
        player.getPersistentData().putInt(NBT_GRAZED_SOULS, 0);
        var dmgAttr   = player.getAttribute(Attributes.ATTACK_DAMAGE);
        var armorAttr = player.getAttribute(Attributes.ARMOR);
        if (dmgAttr   != null) dmgAttr.removeModifier(SOUL_DAMAGE_ID);
        if (armorAttr != null) armorAttr.removeModifier(SOUL_ARMOR_ID);
    }

    private static String soulPassiveDesc(int count) {
        return "§7(§b+" + (count * 1.5) + " atk§7, §b+" + (count * 0.5) + " armor§7)";
    }

    // ─── Empowered mode helpers ───────────────────────────────────────────────

    /**
     * Returns true if the player is sneaking AND has enough Depravity for empowered cast.
     */
    private static boolean isEmpowered(Player player) {
        return player.isShiftKeyDown() && getDepravity(player) >= EMPOWER_MIN_DEP;
    }

    /**
     * Consumes EMPOWER_CONSUME_PCT of current Depravity (minimum 15, never all).
     * Returns the amount consumed, which abilities use to scale their bonus effects.
     */
    private static float consumeDepravityForEmpower(Player player) {
        float current  = getDepravity(player);
        float consume  = Math.max(EMPOWER_MIN_DEP, current * EMPOWER_CONSUME_PCT);
        // Never drop below 5
        consume = Math.min(consume, current - 5f);
        if (consume <= 0) return 0f;
        setDepravity(player, current - consume);
        return consume;
    }

    /**
     * Returns a 0.0–1.0 power ratio for the consumed amount,
     * used to scale empowered bonuses smoothly.
     * 15 consumed → ~0.15, 40 consumed → ~0.55, 60+ consumed → 1.0
     */
    private static float empowerRatio(float consumed) {
        return  consumed / 60f + 1;
    }

    // ─── Damage / corrode helpers ─────────────────────────────────────────────

    public static float depravityMult(Player player) {
        float dep  = getDepravity(player);
        float mult;
        if (inDescent(player))              mult = 3.0f;
        else if (dep >= MADNESS_THRESHOLD)  mult = 2.2f;
        else if (dep >= ENHANCED_THRESHOLD) mult = 1.65f;
        else if (dep >= BONUS_THRESHOLD)    mult = 1.25f;
        else                                mult = 1.00f;
        mult *= (1f + getGrazedSouls(player) * 0.08f);
        return mult;
    }

    private static float corrodeMult(LivingEntity entity) {
        int stacks = entity.getPersistentData().getInt(NBT_CORRODE_STACKS);
        return 1f + stacks * 0.12f;
    }

    private static void addCorrodeStacks(LivingEntity entity, Player owner, int amount) {
        int cur = entity.getPersistentData().getInt(NBT_CORRODE_STACKS);
        entity.getPersistentData().putInt(NBT_CORRODE_STACKS,
                Math.min(CORRODE_MAX, cur + amount));
        entity.getPersistentData().putUUID(NBT_CORRODE_OWNER, owner.getUUID());
    }

    private static String depravityTag(Player player) {
        float d = getDepravity(player);
        if (inDescent(player))             return "§5§l[☠ DESCENT]";
        if (d >= MADNESS_THRESHOLD)        return "§4§l[☠ MADNESS]";
        if (d >= ENHANCED_THRESHOLD)       return "§c[Enhanced]";
        if (d >= BONUS_THRESHOLD)          return "§6[Depraved]";
        return "§7[Stable]";
    }

    private static String depravityStatus(Player player) {
        float d = getDepravity(player);
        int   s = getGrazedSouls(player);
        return depravityTag(player)
                + " §fDep: §b" + String.format("%.0f", d) + "/"+DEP_MAX
                + (s > 0 ? " §5[Souls: " + s + "]" : "");
    }

    // ─── Ray-cast / particle helpers ──────────────────────────────────────────

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

    private static void shadowLine(ServerLevel sl, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from).normalize();
        double dist = from.distanceTo(to);
        Vec3 c = from;
        for (double dd = 0; dd < dist; dd += 0.45) {
            sl.sendParticles(ParticleTypes.SOUL, c.x, c.y, c.z, 1, 0.03, 0.03, 0.03, 0);
            c = c.add(d.scale(0.45));
        }
    }

    private static void shadowBurst(ServerLevel sl, Vec3 pos, int count) {
        sl.sendParticles(ParticleTypes.SOUL,
                pos.x, pos.y + 1, pos.z, count, 0.35, 0.35, 0.35, 0.04);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                pos.x, pos.y + 1, pos.z, count / 2, 0.2, 0.2, 0.2, 0.02);
    }

    // =========================================================================
    //  EVENTS
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class HangedAsceticEvents {

        @SubscribeEvent
        public static void onHangedAsceticTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Hanged Ascetic")) return;

            float dep = getDepravity(player);

            // ── Descent countdown ─────────────────────────────────────────────
            int descent = player.getPersistentData().getInt(NBT_DESCENT_ACTIVE);
            if (descent > 0) {
                player.getPersistentData().putInt(NBT_DESCENT_ACTIVE, descent - 1);
                setDepravity(player, DEP_MAX);

                int selfCd = player.getPersistentData().getInt(NBT_DESCENT_SELF_CD) - 1;
                if (selfCd <= 0) {
                    player.hurt(player.level().damageSources().magic(), 2.0f + getGrazedSouls(player)/5);
                    selfCd = SELF_DMG_INTERVAL;
                    sl.sendParticles(ParticleTypes.SOUL,
                            player.getX(), player.getY() + 1, player.getZ(),
                            5, 0.2, 0.2, 0.2, 0.03);
                }
                player.getPersistentData().putInt(NBT_DESCENT_SELF_CD, selfCd);

                // Released soul phantom strikes every second
                if (player.tickCount % 20 == 0 && getGrazedSouls(player) > 0) {
                    int souls = getGrazedSouls(player);
                    sl.getEntitiesOfClass(LivingEntity.class,
                                    player.getBoundingBox().inflate(16),
                                    e -> e != player && e.isAlive())
                            .stream().limit(souls)
                            .forEach(e -> {
                                float soulDmg = (8f + SoulCore.getAscensionStage(player) * 2f)
                                        * depravityMult(player) + getGrazedSouls(player);
                                e.hurt(sl.damageSources().playerAttack(player), soulDmg);
                                e.invulnerableTime = 0;
                                addCorrodeStacks(e, player, 1);
                                shadowBurst(sl, e.position(), 4);
                            });
                }

                if (player.tickCount % 5 == 0) {
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            player.getX(), player.getY() + 1, player.getZ(),
                            4, 0.5, 0.5, 0.5, 0.04);
                }

                if (descent == 1) {
                    clearGrazedSouls(player);
                    var descentDmg = player.getAttribute(Attributes.ATTACK_DAMAGE);
                    if (descentDmg != null) descentDmg.removeModifier(DESCENT_DAMAGE_ID);
                    if (player instanceof ServerPlayer sp)
                        sp.sendSystemMessage(Component.literal(
                                "§5§l☠ Descent ended. §r§7Depravity fading..."));
                }
            }

            // ── Shadow Lurk countdown ─────────────────────────────────────────
            int lurk = player.getPersistentData().getInt(NBT_SHADOW_LURK);
            if (lurk > 0) {
                player.getPersistentData().putInt(NBT_SHADOW_LURK, lurk - 1);
                player.addEffect(new MobEffectInstance(
                        MobEffects.INVISIBILITY, 5, 0, true, false));
                player.addEffect(new MobEffectInstance(
                        MobEffects.MOVEMENT_SPEED, 5, 1, true, false));
                if (player.tickCount % 8 == 0)
                    sl.sendParticles(ParticleTypes.SOUL,
                            player.getX(), player.getY() + 0.5, player.getZ(),
                            2, 0.2, 0.1, 0.2, 0.01);
                if (lurk == 1) {
                    var spd = player.getAttribute(Attributes.MOVEMENT_SPEED);
                    if (spd != null) spd.removeModifier(LURK_SPEED_ID);
                }
            }

            // ── Shadow Curse DoT ──────────────────────────────────────────────
            int curseTimer = player.getPersistentData().getInt(NBT_CURSE_TIMER);
            if (curseTimer > 0) {
                curseTimer--;
                player.getPersistentData().putInt(NBT_CURSE_TIMER, curseTimer);
                if (player.getPersistentData().contains(NBT_CURSE_TARGET)
                        && player.tickCount % CURSE_DOT_INTERVAL == 0) {
                    UUID curseUUID = player.getPersistentData().getUUID(NBT_CURSE_TARGET);
                    sl.getEntitiesOfClass(LivingEntity.class,
                                    player.getBoundingBox().inflate(40),
                                    e -> e.getUUID().equals(curseUUID) && e.isAlive())
                            .forEach(e -> {
                                int corrStacks = e.getPersistentData()
                                        .getInt(NBT_CORRODE_STACKS);
                                float dotDmg = (3f + corrStacks * 1.5f)
                                        * depravityMult(player) + getGrazedSouls(player);
                                e.hurt(sl.damageSources().playerAttack(player), dotDmg);
                                e.invulnerableTime = 0;
                                e.addEffect(new MobEffectInstance(
                                        MobEffects.WEAKNESS, 30, 0, false, false));
                                sl.sendParticles(ParticleTypes.SOUL,
                                        e.getX(), e.getY() + 1, e.getZ(),
                                        3, 0.2, 0.2, 0.2, 0.02);
                            });
                }
                if (curseTimer == 0)
                    player.getPersistentData().remove(NBT_CURSE_TARGET);
            }

            // ── Profane Language bind tick ─────────────────────────────────────
            int profaneTimer = player.getPersistentData().getInt(NBT_PROFANE_TIMER);
            if (profaneTimer > 0) {
                profaneTimer--;
                player.getPersistentData().putInt(NBT_PROFANE_TIMER, profaneTimer);
                if (player.getPersistentData().contains(NBT_PROFANE_BOUND)) {
                    UUID profaneUUID = player.getPersistentData()
                            .getUUID(NBT_PROFANE_BOUND);
                    sl.getEntitiesOfClass(LivingEntity.class,
                                    player.getBoundingBox().inflate(40),
                                    e -> e.getUUID().equals(profaneUUID) && e.isAlive())
                            .forEach(e -> {
                                e.addEffect(new MobEffectInstance(
                                        MobEffects.MOVEMENT_SLOWDOWN, 25, 3, false, false));
                                if (player.tickCount % 15 == 0)
                                    addDepravity(player, 1f);
                                if (player.tickCount % 8 == 0)
                                    sl.sendParticles(ParticleTypes.WITCH,
                                            e.getX(), e.getY() + 1, e.getZ(),
                                            2, 0.2, 0.2, 0.2, 0.02);
                            });
                }
                if (profaneTimer == 0)
                    player.getPersistentData().remove(NBT_PROFANE_BOUND);
            }

            // ── Madness passive self-damage ────────────────────────────────────
            if (inMadness(player) && !inDescent(player)
                    && player.tickCount % 50 == 0) {
                player.hurt(player.level().damageSources().magic(), 1.0f +getGrazedSouls(player)/2);
                sl.sendParticles(ParticleTypes.WITCH,
                        player.getX(), player.getY() + 1, player.getZ(),
                        3, 0.2, 0.2, 0.2, 0.02);
            }

            // ── Depravity decay ───────────────────────────────────────────────
            if (dep > 0 && !inDescent(player)) {
                int decayTimer = player.getPersistentData().getInt(NBT_DEP_DECAY) - 1;
                if (decayTimer <= 0) {
                    setDepravity(player, dep - 1f);
                    decayTimer = DECAY_INTERVAL;
                }
                player.getPersistentData().putInt(NBT_DEP_DECAY, decayTimer);
            }

            // ── HUD particles ─────────────────────────────────────────────────
            if (player.tickCount % 14 == 0 && dep > 10f) {
                int count = Math.max(1, (int)(dep / 25f));
                sl.sendParticles(ParticleTypes.SOUL,
                        player.getX(), player.getY() + 2.4, player.getZ(),
                        count, 0.2, 0.1, 0.2, 0.005);
            }

            if (player.tickCount%2000==0){
                removeGrazedSoul(player,sl);
            }
        }

        @SubscribeEvent(priority = EventPriority.HIGH)
        public static void onHangedAsceticDamageTaken(LivingHurtEvent event) {
            if (!(event.getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Hanged Ascetic")) return;

            addDepravity(player, event.getAmount() * 0.35f);

            if (event.getSource().getEntity() instanceof LivingEntity attacker
                    && player.getPersistentData().contains(NBT_PROFANE_BOUND)) {
                UUID profaneUUID = player.getPersistentData().getUUID(NBT_PROFANE_BOUND);
                if (attacker.getUUID().equals(profaneUUID)) {
                    float reflect = event.getAmount() * 0.6f * depravityMult(player);
                    attacker.hurt(player.level().damageSources().magic(), reflect);
                    attacker.invulnerableTime = 0;
                    if (player.level() instanceof ServerLevel sl)
                        sl.sendParticles(ParticleTypes.WITCH,
                                attacker.getX(), attacker.getY() + 1, attacker.getZ(),
                                6, 0.2, 0.2, 0.2, 0.03);
                    if (player instanceof ServerPlayer sp)
                        sp.sendSystemMessage(Component.literal(
                                "§5[Profane Language] §c"
                                        + String.format("%.1f", reflect) + " reflected."));
                }
            }

            if (event.getAmount() > 4f && inLurk(player)) {
                player.getPersistentData().putInt(NBT_SHADOW_LURK, 0);
                player.getPersistentData().putBoolean(NBT_LURK_CRIT, false);
                if (player instanceof ServerPlayer sp)
                    sp.sendSystemMessage(Component.literal(
                            "§8Shadow Lurk §7broken by damage!"));
            }
        }

        @SubscribeEvent(priority = EventPriority.NORMAL)
        public static void onHangedAsceticDamageDealt(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Hanged Ascetic")) return;

            LivingEntity target = event.getEntity();
            event.setAmount(event.getAmount()
                    * depravityMult(player)
                    * corrodeMult(target));

            if (player.getPersistentData().getBoolean(NBT_LURK_CRIT)) {
                event.setAmount(event.getAmount() * 2.0f);
                player.getPersistentData().putBoolean(NBT_LURK_CRIT, false);
                if (player.level() instanceof ServerLevel sl)
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            target.getX(), target.getY() + 1, target.getZ(),
                            12, 0.3, 0.3, 0.3, 0.05);
                if (player instanceof ServerPlayer sp)
                    sp.sendSystemMessage(Component.literal(
                            "§8§l[Shadow Crit!] §r§fx2 damage from lurk."));
            }

            addDepravity(player, 1.2f);
        }
    }

    // =========================================================================
    //  ABILITY 1 — HANGED ASCETIC SHADOW CURSE
    // =========================================================================

    /**
     * Normal: Curse a target — DoT every second for 6s, Weakness, 1 Corrode stack.
     *
     * Empowered (sneak):
     *   Consumes up to 40% current Depravity.
     *   Low consumed  (~15): curse duration +3s, +1 extra corrode stack.
     *   Mid consumed  (~30): above + Wither applied to target for 4s.
     *   High consumed (~50+): above + curse spreads to 2 nearby enemies automatically,
     *                         DoT damage multiplied by 1.5.
     */
    public static void hangedAsceticShadowCurse(Player player, Level level,
                                                ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassHangedAscetic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;

        int   ascStage = SoulCore.getAscensionStage(player);
        int   range    = inDescent(player) ? 20 + ascStage : 12 + ascStage + getGrazedSouls(player);
        boolean empower = isEmpowered(player);

        LivingEntity target = rayCastFirst(player, level, range);
        if (target == null) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo target in range!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 250);
        addDepravity(player, 10f);

        // ── Empowered scaling ──────────────────────────────────────────────────
        float consumed   = empower ? consumeDepravityForEmpower(player) : 0f;
        float ratio      = empowerRatio(consumed);
        int   curseDur   = CURSE_DURATION + (empower ? (int)(ratio * 60f) : 0);  // up to +3s
        int   corrApply  = 1 + (empower && ratio >= 0.25f ? 1 : 0);              // +1 at mid
        boolean withered = empower && ratio >= 0.50f;
        boolean spread   = empower && ratio >= 0.80f;

        // Apply curse
        player.getPersistentData().putUUID(NBT_CURSE_TARGET, target.getUUID());
        player.getPersistentData().putInt(NBT_CURSE_TIMER, curseDur);
        addCorrodeStacks(target, player, corrApply);

        if (isEnhanced(player))
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    curseDur, 1, false, true));
        if (withered)
            target.addEffect(new MobEffectInstance(MobEffects.WITHER,
                    80, 0, false, true));

        // High-ratio spread
        if (spread) {
            level.getEntitiesOfClass(LivingEntity.class,
                            target.getBoundingBox().inflate(6f),
                            e -> e != player && e != target && e.isAlive())
                    .stream().limit(2)
                    .forEach(e -> {
                        addCorrodeStacks(e, player, 1);
                        e.addEffect(new MobEffectInstance(
                                MobEffects.WEAKNESS, 60, 0, false, false));
                        shadowBurst(sl, e.position(), 4);
                    });
        }

        // Madness natural spread (separate from empowered spread)
        if (inMadness(player) && !spread) {
            level.getEntitiesOfClass(LivingEntity.class,
                            target.getBoundingBox().inflate(5f),
                            e -> e != player && e != target && e.isAlive())
                    .stream().limit(2)
                    .forEach(e -> {
                        addCorrodeStacks(e, player, 1);
                        shadowBurst(sl, e.position(), 3);
                    });
        }

        shadowBurst(sl, target.position(), 10);
        level.playSound(null, target.blockPosition(),
                SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 0.7f, 0.6f);

        String empNote = empower
                ? " §d[Empowered ×" + String.format("%.2f", ratio) + " consumed §e"
                + String.format("%.0f", consumed) + "§d dep]"
                : "";
        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§8Shadow Curse: §fcursed §c" + target.getName().getString()
                            + " §7(§e+" + corrApply + " corrode§7, §b"
                            + (curseDur / 20) + "s§7)"
                            + (withered ? " §cWither" : "")
                            + (spread ? " §5+spread" : "")
                            + empNote + ". " + depravityStatus(player)));
    }

    // =========================================================================
    //  ABILITY 2 — HANGED ASCETIC FLESH BOMB
    // =========================================================================

    /**
     * Normal: Tear off flesh, throw as explosive. Costs HP. Corrosive blood rain.
     *
     * Empowered (sneak):
     *   Consumes up to 40% current Depravity.
     *   Low  (~15): +1 corrode stack on all hit, radius +1.
     *   Mid  (~30): above + secondary shrapnel ring fires outward pushing enemies.
     *   High (~50+): above + all hit enemies are briefly blinded (Darkness effect),
     *                bomb detonation triggers a second corrosive pulse 1s later.
     */
    public static void hangedAsceticFleshBomb(Player player, Level level,
                                              ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassHangedAscetic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 400) return;
        if (SoulCore.getAscensionStage(player) < 1) return;

        int   ascStage = SoulCore.getAscensionStage(player);
        boolean empower = isEmpowered(player);
        float hpCost   = isEnhanced(player) ? 1.5f : 3.0f;

        if (player.getHealth() <= hpCost + 1f) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNot enough HP to tear flesh!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 400);
        player.hurt(player.level().damageSources().magic(), hpCost);
        addDepravity(player, 12f);

        float consumed   = empower ? consumeDepravityForEmpower(player) : 0f;
        float ratio      = empowerRatio(consumed);
        int   range      = inDescent(player) ? 20 + ascStage : 12 + ascStage;
        float radius     = 4f + ascStage * 0.5f + (isEnhanced(player) ? 2f : 0f)
                + (empower ? ratio * 3f : 0f) +getGrazedSouls(player) + getGrazedSouls(player)/2;     // up to +3 radius
        float dmg        = (14f + ascStage * 2.5f) * depravityMult(player);
        boolean shrapnel = empower && ratio >= 0.50f;
        boolean blind    = empower && ratio >= 0.80f;

        Vec3 impactPoint;
        LivingEntity primaryHit = rayCastFirst(player, level, range);
        impactPoint = primaryHit != null
                ? primaryHit.position()
                : player.getEyePosition().add(player.getLookAngle().scale(range));

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                new AABB(impactPoint, impactPoint).inflate(radius),
                e -> e != player && e.isAlive());

        for (LivingEntity e : targets) {
            e.hurt(level.damageSources().playerAttack(player),
                    dmg * corrodeMult(e) + getGrazedSouls(player));
            e.invulnerableTime = 0;
            addCorrodeStacks(e, player, empower && ratio >= 0.25f ? 2 : 1);
            e.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, false, true));
            if (blind)
                e.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, true));
            sl.sendParticles(ParticleTypes.WITCH,
                    e.getX(), e.getY() + 1, e.getZ(), 5, 0.25, 0.25, 0.25, 0.03);
        }

        // Shrapnel ring — push enemies outward
        if (shrapnel) {
            for (LivingEntity e : targets) {
                Vec3 push = e.position().subtract(impactPoint).normalize().scale(0.8);
                e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.3, push.z));
            }
            for (int i = 0; i < 24; i++) {
                double angle = Math.toRadians(i * 15);
                sl.sendParticles(ParticleTypes.CRIT,
                        impactPoint.x + radius * 0.6 * Math.cos(angle),
                        impactPoint.y + 0.5,
                        impactPoint.z + radius * 0.6 * Math.sin(angle),
                        1, 0, 0.2, 0, 0.04);
            }
        }

        // Madness secondary bomb
        if (inMadness(player)) {
            LivingEntity largest = targets.stream()
                    .max(Comparator.comparingDouble(LivingEntity::getHealth))
                    .orElse(null);
            if (largest != null) {
                float secDmg = dmg * 0.5f +getGrazedSouls(player);
                level.getEntitiesOfClass(LivingEntity.class,
                                largest.getBoundingBox().inflate(radius * 0.6f),
                                e -> e != player && e.isAlive())
                        .forEach(e -> {
                            e.hurt(level.damageSources().playerAttack(player), secDmg );
                            e.invulnerableTime = 0;
                            addCorrodeStacks(e, player, 1);
                        });
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        largest.getX(), largest.getY() + 1, largest.getZ(),
                        1, 0, 0, 0, 0);
            }
        }

        for (int i = 0; i < 20; i++) {
            double angle = Math.toRadians(i * 18);
            sl.sendParticles(ParticleTypes.WITCH,
                    impactPoint.x + radius * Math.cos(angle),
                    impactPoint.y + 0.3,
                    impactPoint.z + radius * Math.sin(angle),
                    1, 0, 0.3, 0, 0.02);
        }
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                impactPoint.x, impactPoint.y + 1, impactPoint.z, 1, 0, 0, 0, 0);
        level.playSound(null, player.blockPosition(),
                SoundEvents.SLIME_DEATH, SoundSource.HOSTILE, 1f, 0.3f);

        String empNote = empower
                ? " §d[+r" + String.format("%.1f", ratio * 3f)
                + (shrapnel ? " shrapnel" : "")
                + (blind ? " blind" : "")
                + " §e-" + String.format("%.0f", consumed) + "§d dep]"
                : "";
        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§cFlesh Bomb: §f" + String.format("%.1f", dmg)
                            + " dmg r§e" + String.format("%.1f", radius)
                            + empNote + ". Hit §c" + targets.size() + "§f. "
                            + depravityStatus(player)));
    }

    // =========================================================================
    //  ABILITY 3 — HANGED ASCETIC SHADOW LURK
    // =========================================================================

    /**
     * Normal: Vanish into shadows 4s — invisible, faster, next strike ×2.
     *
     * Empowered (sneak):
     *   Consumes up to 40% current Depravity.
     *   Low  (~15): lurk duration +2s.
     *   Mid  (~30): above + shadow double confuses enemies in 10 blocks,
     *               next strike applies 2 corrode stacks on hit.
     *   High (~50+): above + lurk crit becomes ×3 instead of ×2,
     *                first strike also knocks target back and up.
     */
    public static void hangedAsceticShadowLurk(Player player, Level level,
                                               ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassHangedAscetic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 300) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        boolean empower = isEmpowered(player);
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 300);
        addDepravity(player, 8f);

        float consumed    = empower ? consumeDepravityForEmpower(player) : 0f;
        float ratio       = empowerRatio(consumed);
        int   lurkDur     = (inDescent(player) ? LURK_DURATION + 60 : LURK_DURATION)
                + (empower ? (int)(ratio * 40f) : 0);    // up to +2s
        boolean dblConfuse = empower && ratio >= 0.50f;
        boolean superCrit  = empower && ratio >= 0.80f;

        player.getPersistentData().putInt(NBT_SHADOW_LURK, lurkDur);
        player.getPersistentData().putBoolean(NBT_LURK_CRIT, true);
        // Store super-crit flag so the event handler can pick it up
        player.getPersistentData().putBoolean("HASuperCrit", superCrit);
        // Store corrode-on-crit flag for mid ratio
        player.getPersistentData().putBoolean("HALurkCorrode", dblConfuse);

        var spd = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null) {
            spd.removeModifier(LURK_SPEED_ID);
            spd.addTransientModifier(new AttributeModifier(
                    LURK_SPEED_ID, 0.04 + (empower ? ratio * 0.02 : 0),
                    AttributeModifier.Operation.ADD_VALUE));
        }

        // Shadow double confuse
        if (isEnhanced(player) || dblConfuse) {
            float confuseRange = dblConfuse ? 10f : 8f;
            level.getEntitiesOfClass(LivingEntity.class,
                            player.getBoundingBox().inflate(confuseRange),
                            e -> e != player && e.isAlive())
                    .forEach(e -> e.addEffect(new MobEffectInstance(
                            MobEffects.CONFUSION, 40, 0, false, false)));
            Vec3 origin = player.position();
            for (int i = 0; i < 16; i++) {
                double angle = Math.toRadians(i * 22.5);
                sl.sendParticles(ParticleTypes.SOUL,
                        origin.x + 0.6 * Math.cos(angle),
                        origin.y + 1,
                        origin.z + 0.6 * Math.sin(angle),
                        1, 0, 0.3, 0, 0.02);
            }
        }

        for (int i = 0; i < 20; i++) {
            double angle = Math.toRadians(i * 18);
            sl.sendParticles(ParticleTypes.SOUL,
                    player.getX() + 0.8 * Math.cos(angle),
                    player.getY() + 0.5,
                    player.getZ() + 0.8 * Math.sin(angle),
                    1, 0, 0.2, 0, 0.01);
        }
        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6f, 0.5f);

        String empNote = empower
                ? " §d[+" + (lurkDur / 20 - LURK_DURATION / 20) + "s"
                + (dblConfuse ? " confuse+" : "")
                + (superCrit ? " ×3crit" : "")
                + " §e-" + String.format("%.0f", consumed) + "§d dep]"
                : "";
        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§8Shadow Lurk: §fconcealed §b" + (lurkDur / 20) + "s§f. "
                            + (superCrit ? "§dNext strike ×3" : "§dNext strike ×2")
                            + empNote + ". " + depravityStatus(player)));
    }

    // =========================================================================
    //  ABILITY 4 — HANGED ASCETIC GRAZE
    // =========================================================================

    /**
     * Normal: Execute a low-HP enemy (≤25% + 5% per corrode stack), harvest their soul.
     *         Grants +1.5 atk dmg and +0.5 armor per soul (stacking, max 3 souls).
     *
     * Empowered (sneak):
     *   Consumes up to 40% current Depravity.
     *   Low  (~15): execute threshold raised by +10%.
     *   Mid  (~30): above + on successful graze, nearby enemies lose 20% of their
     *               current HP as the soul is torn free (soul shockwave).
     *   High (~50+): above + the grazed soul immediately fires one free phantom strike
     *                on the nearest enemy upon harvesting.
     *
     * IMPORTANT: Soul graze is ONLY performed here — no other ability adds grazed souls.
     */
    public static void hangedAsceticGraze(Player player, Level level,
                                          ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassHangedAscetic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 500) return;
        if (SoulCore.getAscensionStage(player) < 3) return;

        if (getGrazedSouls(player) >= GRAZED_MAX) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§cSoul slots full! §7[" + GRAZED_MAX + "/" + GRAZED_MAX + "]"));
            return;
        }

        boolean empower  = isEmpowered(player);
        int     ascStage = SoulCore.getAscensionStage(player);
        float consumed   = empower ? consumeDepravityForEmpower(player) : 0f;
        float ratio      = empowerRatio(consumed);

        LivingEntity target = rayCastFirst(player, level,
                inDescent(player) ? 18 + ascStage : 10 + ascStage);
        if (target == null) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo target in range!"));
            return;
        }

        int corrStacks      = target.getPersistentData().getInt(NBT_CORRODE_STACKS);
        float baseThreshold = inMadness(player) ? 0.45f : 0.25f;
        float empBonus      = empower ? ratio * 0.20f : 0f;          // up to +20% threshold
        float threshold     = baseThreshold + corrStacks * 0.05f + empBonus;
        float hpPct         = target.getHealth() / target.getMaxHealth();

        if (hpPct > threshold && !inDescent(player) && target.getHealth()>24f) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§cTarget above Graze threshold! Need ≤§e"
                                + String.format("%.0f", threshold * 100f)
                                + "%§c HP. Has §e"
                                + String.format("%.0f", hpPct * 100f) + "%§c."));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 500);
        addDepravity(player, 15f);

        // Execute and graze
        target.hurt(level.damageSources().playerAttack(player),
                target.getMaxHealth() * 2f);
        addGrazedSoul(player, sl);

        // Mid empowered: soul shockwave — nearby enemies lose 20% current HP
        if (empower && ratio >= 0.50f) {
            level.getEntitiesOfClass(LivingEntity.class,
                            target.getBoundingBox().inflate(8f),
                            e -> e != player && e != target && e.isAlive())
                    .forEach(e -> {
                        float shockDmg = e.getHealth() * 0.20f + getGrazedSouls(player);
                        e.hurt(level.damageSources().magic(), shockDmg);
                        e.invulnerableTime = 0;
                        sl.sendParticles(ParticleTypes.SOUL,
                                e.getX(), e.getY() + 1, e.getZ(),
                                5, 0.2, 0.2, 0.2, 0.03);
                    });
        }

        // High empowered: immediate phantom strike from new soul
        if (empower && ratio >= 0.80f) {
            level.getEntitiesOfClass(LivingEntity.class,
                            player.getBoundingBox().inflate(14f),
                            e -> e != player && e != target && e.isAlive())
                    .stream().findFirst()
                    .ifPresent(e -> {
                        float phantomDmg = (10f + ascStage * 2f) * depravityMult(player) + getGrazedSouls(player)*2;
                        e.hurt(level.damageSources().playerAttack(player), phantomDmg);
                        e.invulnerableTime = 0;
                        shadowLine(sl, target.position().add(0, 1, 0),
                                e.position().add(0, 1, 0));
                        if (player instanceof ServerPlayer sp)
                            sp.sendSystemMessage(Component.literal(
                                    "§5[Soul Phantom Strike] §c"
                                            + String.format("%.1f", phantomDmg) + " dmg."));
                    });
        }

        shadowBurst(sl, target.position(), 16);
        shadowLine(sl, player.position().add(0, 1, 0),
                target.position().add(0, 1, 0));
        level.playSound(null, target.blockPosition(),
                SoundEvents.WITHER_DEATH, SoundSource.HOSTILE, 0.8f, 1.4f);

        String empNote = empower
                ? " §d[threshold+§e" + String.format("%.0f", empBonus * 100) + "%"
                + (ratio >= 0.50f ? " shockwave" : "")
                + (ratio >= 0.80f ? " phantom" : "")
                + " §e-" + String.format("%.0f", consumed) + "§d dep]"
                : "";
        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5Graze: §fdevoured §c" + target.getName().getString()
                            + "§f's soul. §7[§e" + getGrazedSouls(player)
                            + "§7/"+ GRAZED_MAX + "]" + empNote + " " + depravityStatus(player)));
    }

    // =========================================================================
    //  ABILITY 5 — HANGED ASCETIC CULL OF SPIRITUAL FLESH
    // =========================================================================

    /**
     * Normal: Black Greatsword strike — massive single-target damage scaling with
     *         corrode stacks, applies Wither + Weakness, adds 2 corrode stacks.
     *
     * Empowered (sneak):
     *   Consumes up to 40% current Depravity.
     *   Low  (~15): cull arc widens, hitting all enemies in a 90° forward cone (sweep).
     *   Mid  (~30): above + each enemy hit by the sweep gains +2 corrode stacks,
     *               bonus damage per enemy struck beyond the first.
     *   High (~50+): above + every hit enemy is launched upward and suffers a
     *                lingering Soul Rend (Wither II for 5s).
     */
    public static void hangedAsceticCullOfSpiritualFlesh(Player player, Level level,
                                                         ServerLevel sl,
                                                         boolean bypassClassCheck) {
        if (!canUseClassHangedAscetic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 700) return;
        if (SoulCore.getAscensionStage(player) < 4) return;

        boolean empower  = isEmpowered(player);
        int     ascStage = SoulCore.getAscensionStage(player);
        float consumed   = empower ? consumeDepravityForEmpower(player) : 0f;
        float ratio      = empowerRatio(consumed);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 700);
        addDepravity(player, 18f);

        boolean sweepMode = empower && ratio >= 0.25f;
        boolean multiBonus = empower && ratio >= 0.50f;
        boolean soulRend  = empower && ratio >= 0.80f;

        List<LivingEntity> hitTargets = new ArrayList<>();

        if (sweepMode) {
            // Wide cone sweep — all enemies in 90° half-angle, 10 block range
            float coneRange  = 10f + ascStage * 0.5f;
            float cosA       = (float) Math.cos(Math.toRadians(45));
            Vec3 look        = player.getLookAngle().normalize();
            Vec3 from        = player.getEyePosition();
            level.getEntitiesOfClass(LivingEntity.class,
                            player.getBoundingBox().inflate(coneRange),
                            e -> {
                                if (e == player || !e.isAlive()) return false;
                                Vec3 dir = e.position().add(0, e.getBbHeight() / 2, 0)
                                        .subtract(from).normalize();
                                return (float) look.dot(dir) >= cosA
                                        && from.distanceTo(e.position()) <= coneRange;
                            })
                    .forEach(hitTargets::add);
        } else {
            // Normal single target
            LivingEntity target = rayCastFirst(player, level,
                    inDescent(player) ? 18 + ascStage : 10 + ascStage);
            if (target == null) {
                if (player instanceof ServerPlayer sp)
                    sp.sendSystemMessage(Component.literal("§cNo target in range!"));
                return;
            }
            hitTargets.add(target);
        }

        if (hitTargets.isEmpty()) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo targets in sweep!"));
            return;
        }

        float totalDmg = 0f;
        int   idx      = 0;
        for (LivingEntity e : hitTargets) {
            int corrStacks = e.getPersistentData().getInt(NBT_CORRODE_STACKS);
            float dmg = (22f + corrStacks * 6f + ascStage * 4f) * depravityMult(player);

            // Multi-target bonus: each enemy beyond the first deals +15% more
            if (multiBonus && idx > 0) dmg *= (1f + idx * 0.15f);

            // Enhanced obliterate 2 stacks into bonus
            float burstBonus = 0f;
            if (isEnhanced(player) && corrStacks >= 2) {
                int obliterate = Math.min(corrStacks, 2);
                burstBonus = obliterate * 12f * depravityMult(player) + getGrazedSouls(player)*2;
                e.getPersistentData().putInt(NBT_CORRODE_STACKS,
                        corrStacks - obliterate);
            }

            e.hurt(level.damageSources().playerAttack(player), dmg + burstBonus);
            e.invulnerableTime = 0;
            addCorrodeStacks(e, player, multiBonus ? 2 : 1);
            totalDmg += dmg + burstBonus;

            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 1, false, true));
            if (soulRend) {
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1, false, true));
                e.setDeltaMovement(e.getDeltaMovement().add(0, 0.6, 0));
            } else {
                e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, false, true));
            }

            // Madness soul strike
            if (inMadness(player)) {
                e.hurt(level.damageSources().magic(), dmg * 0.35f + getGrazedSouls(player));
                e.invulnerableTime = 0;
            }

            sl.sendParticles(ParticleTypes.SOUL,
                    e.getX(), e.getY() + 1, e.getZ(), 5, 0.2, 0.2, 0.2, 0.03);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    e.getX(), e.getY() + 1, e.getZ(), 3, 0.2, 0.2, 0.2, 0.02);
            idx++;
        }

        // Sweep arc visual
        Vec3 origin = player.getEyePosition();
        Vec3 look   = player.getLookAngle().normalize();
        float arcRange = sweepMode ? 10f : 8f;
        float arcHalf  = sweepMode ? 45f : 20f;
        for (int i = 0; i < 14; i++) {
            double t      = (i / 13.0) - 0.5;
            double ang    = Math.toRadians(arcHalf * t * 2);
            double cos    = Math.cos(ang), sin = Math.sin(ang);
            Vec3 fanned   = new Vec3(
                    look.x * cos - look.z * sin,
                    look.y * 0.5,
                    look.x * sin + look.z * cos).normalize().scale(arcRange);
            Vec3 tip = origin.add(fanned);
            Vec3 d   = tip.subtract(origin).normalize();
            Vec3 c   = origin;
            double dist = origin.distanceTo(tip);
            for (double dd = 0; dd < dist; dd += 0.5) {
                sl.sendParticles(ParticleTypes.SOUL, c.x, c.y, c.z,
                        1, 0.03, 0.03, 0.03, 0);
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y, c.z,
                        1, 0.02, 0.02, 0.02, 0);
                c = c.add(d.scale(0.5));
            }
        }

        level.playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 0.4f);
        level.playSound(null, player.blockPosition(),
                SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 0.8f, 0.6f);

        String empNote = empower
                ? " §d[sweep" + (multiBonus ? " multibonus" : "")
                + (soulRend ? " SoulRend" : "")
                + " §e-" + String.format("%.0f", consumed) + "§d dep]"
                : "";
        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§8§lCull of Spiritual Flesh: §r§c"
                            + String.format("%.1f", totalDmg) + " dmg §7× §e"
                            + hitTargets.size() + "§7 targets"
                            + empNote + ". " + depravityStatus(player)));
    }

    // =========================================================================
    //  ABILITY 6 — HANGED ASCETIC SHADOW SWEEP
    // =========================================================================

    /**
     * Close-range sweeping shadow strike hitting ALL enemies within 5 blocks
     * in a 360° radius. Applies corrode stacks and Weakness to everyone hit.
     *
     * Normal: 5-block radius, 1 corrode stack each, moderate damage.
     *
     * Empowered (sneak):
     *   Consumes up to 40% current Depravity.
     *   Low  (~15): radius expands to 8 blocks, damage +30%.
     *   Mid  (~30): above + sweep leaves a shadow field for 3s — enemies inside
     *               are slowed and take 2 DoT per second.
     *   High (~50+): above + every enemy hit is pulled toward the player (implosion),
     *                then blasted outward — creating a violent crushing effect.
     */
    public static void hangedAsceticShadowSweep(Player player, Level level,
                                                ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseClassHangedAscetic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 450) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        boolean empower  = isEmpowered(player);
        int     ascStage = SoulCore.getAscensionStage(player);
        float consumed   = empower ? consumeDepravityForEmpower(player) : 0f;
        float ratio      = empowerRatio(consumed);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 450);
        addDepravity(player, 10f);

        float radius    = 5f + ascStage * 0.3f + (empower ? ratio * 3f : 0f) + getGrazedSouls(player)/2; // up to +3
        float dmgMult   = 1f + (empower && ratio >= 0.25f ? 0.30f : 0f);
        boolean field   = empower && ratio >= 0.50f;
        boolean implode = empower && ratio >= 0.80f;

        float baseDmg = (12f + ascStage * 2f) * depravityMult(player) * dmgMult;

        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                e -> e != player && e.isAlive());

        Vec3 playerPos = player.position();

        for (LivingEntity e : targets) {
            float dmg = baseDmg * corrodeMult(e) + getGrazedSouls(player);
            e.hurt(level.damageSources().playerAttack(player), dmg);
            e.invulnerableTime = 0;
            addCorrodeStacks(e, player, 1);
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, true));

            if (implode) {
                // Pull toward player first
                Vec3 toPlayer = playerPos.subtract(e.position()).normalize().scale(0.9);
                e.setDeltaMovement(toPlayer.x, 0.4, toPlayer.z);
                // Then a second tick later they get blasted out — approximate with
                // scheduled delayed knockback using a flag particle
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        e.getX(), e.getY() + 1, e.getZ(), 1, 0, 0, 0, 0);
                // Outward knockback on same tick (crush)
                Vec3 away = e.position().subtract(playerPos).normalize().scale(1.2);
                e.setDeltaMovement(e.getDeltaMovement().add(away.x, 0.5, away.z));
            }

            sl.sendParticles(ParticleTypes.SOUL,
                    e.getX(), e.getY() + 1, e.getZ(), 4, 0.2, 0.2, 0.2, 0.03);
        }

        // Shadow field: slow + DoT stored as a passive marker on the player for 3s
        if (field) {
            player.getPersistentData().putInt("HASweepField", 60);
            player.getPersistentData().putDouble("HASweepFX", playerPos.x);
            player.getPersistentData().putDouble("HASweepFY", playerPos.y);
            player.getPersistentData().putDouble("HASweepFZ", playerPos.z);
            player.getPersistentData().putFloat("HASweepFRadius", radius * 0.8f);
            player.getPersistentData().putFloat("HASweepFDmg",
                    2f * depravityMult(player));
        }

        // Full 360° ring visual
        int ringPoints = Math.max(24, (int)(radius * 8));
        for (int i = 0; i < ringPoints; i++) {
            double angle = (2 * Math.PI / ringPoints) * i;
            sl.sendParticles(ParticleTypes.SOUL,
                    playerPos.x + radius * Math.cos(angle),
                    playerPos.y + 0.4,
                    playerPos.z + radius * Math.sin(angle),
                    1, 0, 0.2, 0, 0.01);
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    playerPos.x + radius * 0.5 * Math.cos(angle),
                    playerPos.y + 0.8,
                    playerPos.z + radius * 0.5 * Math.sin(angle),
                    1, 0, 0.15, 0, 0.01);
        }
        level.playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 0.5f);
        level.playSound(null, player.blockPosition(),
                SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 0.6f, 0.7f);

        String empNote = empower
                ? " §d[r+" + String.format("%.1f", ratio * 3f)
                + " dmg×" + String.format("%.2f", dmgMult)
                + (field ? " field" : "")
                + (implode ? " implode" : "")
                + " §e-" + String.format("%.0f", consumed) + "§d dep]"
                : "";
        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§8Shadow Sweep: §f" + String.format("%.1f", baseDmg)
                            + " dmg ×§e" + targets.size() + "§f in r§e"
                            + String.format("%.1f", radius)
                            + empNote + ". " + depravityStatus(player)));
    }

    // =========================================================================
    //  ABILITY 7 — HANGED ASCETIC DESCENT INTO DEPRAVITY
    // =========================================================================

    /**
     * Normal: 12s ultimate. Release all Grazed Souls. Depravity locked at 100.
     *         Soul phantoms auto-strike nearby enemies every second.
     *         Unavoidable self-damage every 2s.
     *
     * Empowered (sneak):
     *   Consumes up to 40% current Depravity (before locking).
     *   Low  (~15): descent duration +3s, phantom strike radius +4.
     *   Mid  (~30): above + on entry, all nearby enemies cursed with Shadow Curse
     *               immediately (no cost, no soul consumption).
     *   High (~50+): above + all enemies within 20 blocks are pulled in and
     *                dealt a massive opening strike (20 + ascStage×4) × mult,
     *                descent self-damage reduced by 50%.
     */
    public static void hangedAsceticDescentIntoDepravity(Player player, Level level,
                                                         ServerLevel sl,
                                                         boolean bypassClassCheck) {
        if (!canUseClassHangedAscetic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 5000) return;
        if (SoulCore.getAscensionStage(player) < 7) return;
        if (getGrazedSouls(player) == 0) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§cMust have at least 1 Grazed Soul to descend!"));
            return;
        }

        boolean empower  = isEmpowered(player);
        float consumed   = empower ? consumeDepravityForEmpower(player) : 0f;
        float ratio      = empowerRatio(consumed);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 5000);

        int   descentDur     = DESCENT_DURATION + (empower ? (int)(ratio * 60f) : 0);
        boolean entryCurse   = empower && ratio >= 0.50f;
        boolean openingBlast = empower && ratio >= 0.80f;

        player.getPersistentData().putInt(NBT_DESCENT_ACTIVE, descentDur);
        player.getPersistentData().putInt(NBT_DESCENT_SELF_CD, SELF_DMG_INTERVAL);
        // Store reduced self-damage flag
        player.getPersistentData().putBoolean("HADescentReducedSelf", openingBlast);
        setDepravity(player, DEP_MAX);

        int   ascStage   = SoulCore.getAscensionStage(player);
        float novaRadius = 18f + ascStage + (empower ? ratio * 4f : 0f) + getGrazedSouls(player);

        var dmgAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmgAttr != null) {
            dmgAttr.removeModifier(DESCENT_DAMAGE_ID);
            dmgAttr.addTransientModifier(new AttributeModifier(
                    DESCENT_DAMAGE_ID, 8.0 + getGrazedSouls(player) * 3.0,
                    AttributeModifier.Operation.ADD_VALUE));
        }

        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class, player.getBoundingBox().inflate(novaRadius),
                e -> e != player && e.isAlive());

        for (LivingEntity e : nearby) {
            addCorrodeStacks(e, player, 3);
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
                    100, 1, false, true));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
                    100, 1, false, true));
            shadowBurst(sl, e.position(), 6);
        }

        // Entry curse: immediately apply Shadow Curse to all nearby
        if (entryCurse) {
            nearby.forEach(e -> {
                addCorrodeStacks(e, player, 1);
                e.addEffect(new MobEffectInstance(MobEffects.POISON,
                        CURSE_DURATION, 0, false, true));
            });
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§5[Entry Curse] §fAll nearby enemies cursed on descent!"));
        }

        // Opening blast: pull in + massive strike
        if (openingBlast) {
            Vec3 playerPos = player.position();
            for (LivingEntity e : nearby) {
                Vec3 toPlayer = playerPos.subtract(e.position()).normalize().scale(1.0);
                e.setDeltaMovement(toPlayer.x, 0.3, toPlayer.z);
                float blastDmg = (20f + ascStage * 4f) * depravityMult(player) + getGrazedSouls(player)*2;
                e.hurt(level.damageSources().playerAttack(player), blastDmg);
                e.invulnerableTime = 0;
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        e.getX(), e.getY() + 1, e.getZ(), 1, 0, 0, 0, 0);
            }
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§5[Opening Blast] §cAll nearby enemies pulled and struck!"));
        }

        // Enter lurk immediately on descent
        player.getPersistentData().putInt(NBT_SHADOW_LURK, 40);
        player.getPersistentData().putBoolean(NBT_LURK_CRIT, true);

        // Grand visual
        for (int i = 0; i < 48; i++) {
            double angle = Math.toRadians(i * 7.5);
            sl.sendParticles(ParticleTypes.SOUL,
                    player.getX() + novaRadius * 0.4 * Math.cos(angle),
                    player.getY() + 1,
                    player.getZ() + novaRadius * 0.4 * Math.sin(angle),
                    1, 0, 0.5, 0, 0.04);
        }
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                player.getX(), player.getY() + 1, player.getZ(),
                40, 1.0, 1.0, 1.0, 0.06);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(),
                2, 0.4, 0.3, 0.4, 0);
        sl.sendParticles(ParticleTypes.FLASH,
                player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.5f, 0.3f);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1f, 0.5f);

        String empNote = empower
                ? " §d[+" + (descentDur / 20 - DESCENT_DURATION / 20) + "s"
                + (entryCurse ? " entryCurse" : "")
                + (openingBlast ? " openingBlast -50%selfDmg" : "")
                + " §e-" + String.format("%.0f", consumed) + "§d dep]"
                : "";
        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§5§l☠ DESCENT INTO DEPRAVITY ☠ §r§f"
                            + getGrazedSouls(player) + " souls released. "
                            + "Dep locked §c100§f. §b"
                            + (descentDur / 20) + "s§f."
                            + empNote));
    }
}
