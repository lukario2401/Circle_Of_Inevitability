package net.lukario.frogerealm.item.seald_artifacts;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════
 *   THE CRACKED BAROMETER  —  Beyonder Characteristic Item
 *   Sequence 4 — Calamity Shaman (Disaster / Calamity Pathway)
 * ══════════════════════════════════════════════════════════════
 *
 * A brass weather instrument, palm-sized, its glass face shattered
 * from the inside out. The needle spins freely and points at nothing.
 * Or rather — it points at what's coming. The two are not always
 * different things.
 *
 * The Calamity Shaman who crystallized into this instrument spent
 * forty years accumulating disasters that should have happened to
 * other people. Floods that missed villages. Lightning that struck
 * open fields. Earthquakes that collapsed into themselves before
 * reaching cities. He collected them all, stored them in the soft
 * tissue between his ribs, and eventually there was more disaster
 * in him than there was him. The barometer is what remained when
 * the container gave out.
 *
 * Carrying it feels like standing at the edge of a storm that
 * hasn't arrived yet. The pressure is real. Your ears pop.
 * Small things break near you slightly more often than they should.
 * This is not a coincidence.
 * Coincidence is the first thing the Calamity pathway destroys.
 *
 * TYPE: Carry-on (held in hand)
 * PATHWAY: Disaster / Calamity (Calamity Shaman sub-branch)
 * SEQUENCE EQUIVALENT: 4 — Calamity Shaman
 *
 *
 * ── PASSIVE I — Pressure Reading ─────────────────────────────
 *   The barometer reads the disaster gradient of the area.
 *   "Storm Pressure" builds passively and through events:
 *     +2 / second held
 *     +8 per entity killed nearby
 *     +15 when the player takes > 8 damage in one hit
 *     +5 when a nearby block is broken by an explosion
 *     -4 / second when NOT held
 *   Storm Pressure cap: 999. Stored: "CB_Pressure".
 *
 *   Storm Pressure passively empowers all other abilities
 *   and determines the severity of both power and drawback.
 *
 * ── PASSIVE II — Calamity Attractor ──────────────────────────
 *   At 100+ Pressure: the barometer begins radiating disaster.
 *   All hostile mobs within 16 blocks take minor environmental
 *   damage — not from the player, from circumstance:
 *     • 1 magic damage per second (pressure differential)
 *     • Occasional knockback (micro-shockwave, every 5 s)
 *   At 300+ Pressure: range expands to 24 blocks.
 *     • Damage increases to 2 per second.
 *     • Random weak explosions (strength 0.5) near hostiles
 *       every 15 s (no block damage, entity damage only).
 *   At 600+ Pressure: range 32 blocks.
 *     • 3 damage per second.
 *     • Explosions every 8 s.
 *     • Lightning visuals (no real lightning — particles only,
 *       simulated via END_ROD + FLASH) strike random mobs.
 *
 * ── PASSIVE III — Disaster Ward ───────────────────────────────
 *   At 150+ Pressure: incoming projectiles have a chance to
 *   be deflected by a micro-pressure burst:
 *     150–299: 20% deflect chance per projectile
 *     300–499: 35% deflect chance
 *     500+:    55% deflect chance
 *   Deflected projectiles gain a random velocity change and
 *   brief particle burst (SMOKE + CRIT).
 *
 * ── PASSIVE IV — Structural Weakness ─────────────────────────
 *   Passive environmental deterioration near the player:
 *     Every 8 s: 1–3 random blocks within 6 blocks of player
 *     have their state "deteriorated":
 *       Stone / Stone Bricks → Cracked Stone Bricks
 *       Cracked Stone Bricks → Cobblestone
 *       Cobblestone → Gravel
 *       Gravel → Sand
 *       Glass / Glass Pane → Air (shatters)
 *       Leaves → Air (stripped)
 *   At 400+ Pressure: radius expands to 10 blocks, affects
 *   5 blocks per cycle. Deterioration happens every 5 s.
 *   At 700+ Pressure: radius 14 blocks, 8 blocks per cycle,
 *   every 3 s. Deepslate / Stone → Air (structural collapse).
 *
 *
 * ── ACTIVE (right-click) — Release Pressure ──────────────────
 *   Vents stored Storm Pressure as a calamity shockwave.
 *   Costs 80 Pressure. Cooldown: 25 s.
 *
 *   Effect scales with Pressure AT TIME OF CAST:
 *
 *   100–249 — Pressure Burst:
 *     Shockwave in 12 blocks. Knockback all entities outward.
 *     All hit: Slowness II + Weakness I for 8 s.
 *     Structural deterioration triggered immediately in 8 blocks.
 *     Smoke + crit particles.
 *
 *   250–499 — Storm Pulse:
 *     Shockwave in 20 blocks. Strong knockback.
 *     All hit: Slowness III + Weakness II + Blindness for 10 s.
 *     Immediate deterioration in 12 blocks (double cycle).
 *     5 random entities struck by "lightning" (END_ROD burst
 *     + 4 magic damage each).
 *     Ground in 6-block radius: random grass/dirt → coarse dirt.
 *
 *   500–749 — Calamity Wave:
 *     Shockwave in 28 blocks. Massive knockback.
 *     All hit: Wither I + Slowness IV + Weakness II + Blindness
 *     for 12 s. 6 true magic damage to all in range.
 *     Immediate triple deterioration cycle in 16 blocks.
 *     Ground: 10-block radius → mix of gravel/coarse dirt/sand.
 *     Particles: massive EXPLOSION_EMITTER + smoke ring.
 *
 *   750+ — ABSOLUTE CALAMITY:
 *     Everything above, doubled. Range 40 blocks.
 *     All entities in range: 12 magic damage + Wither II
 *     + Slowness V + Weakness III + Blindness for 15 s.
 *     Ground in 20-block radius: catastrophic deterioration.
 *     All glass/leaves/gravel in range: instant destruction.
 *     5×5 area below player: random blocks sink one step.
 *     Drains ALL Storm Pressure to 0.
 *     Player receives Pressure Backlash (see drawback).
 *
 *
 * ── DRAWBACK — Accumulated Calamity ──────────────────────────
 *   The Calamity Shaman was a vessel. A vessel that held too much
 *   becomes the disaster. The Barometer does not distinguish between
 *   channeling catastrophe and being catastrophe.
 *
 *   "Fracture Points" accumulate separately from Storm Pressure:
 *     +1 / 10 seconds held
 *     +10 per Release Pressure cast
 *     +30 per Absolute Calamity cast
 *     +5 per entity killed near player
 *     -2 / 10 seconds when NOT held
 *   Fracture Points cap: 999. Stored: "CB_Fracture".
 *
 *   Fracture thresholds:
 *
 *   0–149 — Hairline:
 *     Faint pressure particles. No mechanical penalty yet.
 *     Occasional ear-pop message (flavor only).
 *     Small objects break near you. Held item occasionally
 *     flickers (visual — brief 1-tick Nausea every 2 min).
 *
 *   150–299 — Cracked:
 *     • Hunger drains passively (the body experiences pressure).
 *     • Minor damage immunity broken: any hit deals at least 1
 *       damage regardless of armor (magic bypass, small amount).
 *     • Player occasionally staggers: Slowness I for 2 s every
 *       45 s (random pressure drop).
 *     • Structural Weakness now also affects player's held
 *       item: -1 durability per minute from equipped tools
 *       (not the Barometer itself — it was already broken).
 *
 *   300–499 — Splitting:
 *     • Permanent Hunger effect (passive food drain doubled).
 *     • Player takes 1 magic damage every 20 s (internal pressure).
 *     • Storm Pressure gain from all sources halved
 *       (the vessel is cracking — it holds less).
 *     • Random Nausea flashes every 30 s (2 s duration).
 *     • The barometer whispers in chat: pressure readings,
 *       fragmented and wrong.
 *
 *   500–699 — Fracturing:
 *     • Permanent Weakness I while held.
 *     • Player takes 2 magic damage every 15 s.
 *     • Release Pressure costs 120 instead of 80 Pressure.
 *     • Nearby passive mobs also begin to flee (not just animals —
 *       villagers, etc.) — disaster is visible to them.
 *     • Structural Weakness begins affecting the player's own
 *       armor: Defense rating gradually reduced (Resistance
 *       replaced by brief Vulnerability — no vanilla effect,
 *       simulated by periodic forced damage bypass).
 *     • The whispers become numbers. Countdown to what is unclear.
 *
 *   700–899 — Breaking:
 *     • Permanent Weakness II + Slowness I while held.
 *     • Player takes 3 magic damage every 10 s.
 *     • Spontaneous micro-explosions near player every 30 s
 *       (strength 0.3, no block damage, does hurt the player —
 *       0.5–1.5 damage, unpredictable).
 *     • All hostile mobs in 20 blocks gain permanent Speed I
 *       (the disaster draws them).
 *     • Structural Weakness now also deteriorates blocks the
 *       player is STANDING ON (careful).
 *     • Absolute Calamity costs 200 Pressure instead of 80.
 *
 *   900–999 — Collapse Imminent:
 *     • Permanent Wither I + Weakness II + Slowness II.
 *     • Player takes 4 magic damage every 8 s.
 *     • Spontaneous explosions every 15 s (now with block damage,
 *       strength 0.8 — the containment is failing).
 *     • All mobs in 30 blocks gain Speed II + Strength I.
 *     • The whispers stop. The barometer needle points directly
 *       at the player. It has always pointed at the player.
 *
 *   1000 — CONTAINED COLLAPSE:
 *     The disaster folds inward.
 *     Player takes 16 true magic damage.
 *     All blocks within 5 blocks: immediate full deterioration
 *     (worst-case state — stone → air, etc.).
 *     Wither III + Blindness + Slowness V for 120 s.
 *     All Storm Pressure drained to 0.
 *     Fracture resets to 500 (it cracked, not shattered —
 *     the barometer is already broken, it cannot break further).
 *     Server message to nearby players: they feel a pressure wave.
 *
 *
 * ── STATE (player.getPersistentData()) ────────────────────────
 *   "CB_Pressure"        int    — storm pressure (0–999)
 *   "CB_Fracture"        int    — fracture points (0–999)
 *   "CB_ReleaseCooldown" int    — ticks until release available
 *   "CB_DeteriorTick"    int    — ticks until next block deterioration
 *   "CB_AttractorTick"   int    — ticks until next attractor damage
 *   "CB_ExplosionTick"   int    — ticks until next passive explosion
 *   "CB_WhisperTick"     int    — ticks until next whisper
 *   "CB_FractureTick"    int    — ticks until fracture point accrual
 *   "CB_StaggerTick"     int    — ticks until next stagger
 *   "CB_NauseaTick"      int    — ticks until next nausea flash
 *   "CB_SelfDmgTick"     int    — ticks until next self damage
 *   "CB_MicroExpTick"    int    — ticks until micro explosion
 *   "CB_NotHeldTick"     int    — ticks since last held
 *
 * ── REGISTRATION ──────────────────────────────────────────────
 *   public static final RegistryObject<Item> CRACKED_BAROMETER =
 *       ITEMS.register("cracked_barometer",
 *           () -> new CrackedBarometer(
 *               new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
 */
@Mod.EventBusSubscriber
public class CrackedBarometer extends Item {

    // ── Pressure thresholds ───────────────────────────────────
    private static final int PRES_ATTRACTOR_I   = 100;
    private static final int PRES_ATTRACTOR_II  = 300;
    private static final int PRES_ATTRACTOR_III = 600;
    private static final int PRES_WARD_I        = 150;
    private static final int PRES_WARD_II       = 300;
    private static final int PRES_WARD_III      = 500;

    // ── Fracture thresholds ───────────────────────────────────
    private static final int FRAC_HAIRLINE  = 0;
    private static final int FRAC_CRACKED   = 150;
    private static final int FRAC_SPLITTING = 300;
    private static final int FRAC_FRACTURING= 500;
    private static final int FRAC_BREAKING  = 700;
    private static final int FRAC_COLLAPSE  = 900;
    private static final int FRAC_MAX       = 1000;

    // ── Cooldowns / intervals (ticks) ────────────────────────
    private static final int RELEASE_CD         = 500;  // 25 s
    private static final int DETERIORATION_INT  = 160;  // 8 s
    private static final int DETERIORATION_FAST = 100;  // 5 s
    private static final int DETERIORATION_VFAST= 60;   // 3 s
    private static final int ATTRACTOR_INT      = 20;   // 1 s
    private static final int EXPLOSION_INT_LO   = 300;  // 15 s
    private static final int EXPLOSION_INT_HI   = 160;  // 8 s
    private static final int WHISPER_INT        = 600;  // 30 s
    private static final int FRACTURE_ACCRUAL   = 200;  // 10 s
    private static final int STAGGER_INT        = 900;  // 45 s
    private static final int NAUSEA_INT         = 600;  // 30 s
    private static final int MICROEXP_INT_LO    = 600;  // 30 s
    private static final int MICROEXP_INT_HI    = 300;  // 15 s

    // ── Ranges ───────────────────────────────────────────────
    private static final double ATTRACTOR_RANGE_I  = 16.0;
    private static final double ATTRACTOR_RANGE_II = 24.0;
    private static final double ATTRACTOR_RANGE_III= 32.0;

    // ── Barometer whispers ────────────────────────────────────
    private static final String[] WHISPERS = {
            "[barometer] reading: -∞ hPa. error.",
            "[barometer] imminent: undefined. duration: undefined.",
            "[barometer] pressure differential: catastrophic.",
            "[barometer] forecast: yes.",
            "[barometer] origin of event: you.",
            "[barometer] the needle agrees with itself for once.",
            "[barometer] something is converging on your position.",
            "[barometer] this is not a malfunction.",
    };

    public CrackedBarometer(Properties properties) {
        super(properties);
    }

    // ═════════════════════════════════════════════════════════
    // ACTIVE — Release Pressure
    // ═════════════════════════════════════════════════════════
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        var data = player.getPersistentData();
        int releaseCD = data.getInt("CB_ReleaseCooldown");
        if (releaseCD > 0) {
            player.sendSystemMessage(
                    Component.literal("[barometer] venting too soon. (" + (releaseCD / 20) + "s)")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return InteractionResultHolder.pass(stack);
        }

        int pressure = data.getInt("CB_Pressure");
        int fracture = data.getInt("CB_Fracture");

        // Cost scales at Fracturing tier
        int cost = (fracture >= FRAC_FRACTURING) ? 120 : 80;
        // Absolute Calamity at Breaking tier costs more
        if (pressure >= 750 && fracture >= FRAC_BREAKING) cost = 200;

        if (pressure < cost) {
            player.sendSystemMessage(
                    Component.literal("[barometer] insufficient pressure. (" + pressure + "/" + cost + ")")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return InteractionResultHolder.pass(stack);
        }

        executeRelease(player, level, pressure, data);

        data.putInt("CB_ReleaseCooldown", RELEASE_CD);
        player.getCooldowns().addCooldown(this, RELEASE_CD);
        addFracture(data, 10);
        return InteractionResultHolder.success(stack);
    }

    private static void executeRelease(Player player, Level level, int pressure,
                                       net.minecraft.nbt.CompoundTag data) {
        Vec3 pos = player.position();

        if (pressure >= 750) {
            // ── Absolute Calamity ─────────────────────────────
            addFracture(data, 30);
            data.putInt("CB_Pressure", 0);

            level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(40), e -> e != player
            ).forEach(e -> {
                e.hurt(level.damageSources().magic(), 12f+player.getPersistentData().getInt("CB_Fracture")/10);
                Vec3 dir = e.position().subtract(pos).normalize();
                e.setDeltaMovement(dir.scale(3.0).add(0, 0.8, 0));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.WITHER,            300, 1, false, true));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 300, 4, false, true));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          300, 2, false, true));
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,         300, 0, false, true));
            });
            immediateDeterioration(player, level, 20, 12);
            if (level instanceof ServerLevel sl) {
                catastropheParticles(sl, pos, 40, 150);
                // Flatten 5x5 under player
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        deteriorateBlock(level,
                                player.blockPosition().offset(dx, -1, dz), true);
                    }
                }
            }
            player.sendSystemMessage(Component.literal("════════════════════════════════")
                    .withStyle(ChatFormatting.DARK_RED));
            player.sendSystemMessage(Component.literal("ABSOLUTE CALAMITY")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            player.sendSystemMessage(Component.literal("[barometer] pressure: zero. the reading was always zero.")
                    .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            player.sendSystemMessage(Component.literal("════════════════════════════════")
                    .withStyle(ChatFormatting.DARK_RED));

        } else if (pressure >= 500) {
            // ── Calamity Wave ─────────────────────────────────
            data.putInt("CB_Pressure", pressure - 80);
            level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(28), e -> e != player
            ).forEach(e -> {
                e.hurt(level.damageSources().magic(), 6f+player.getPersistentData().getInt("CB_Fracture")/20);
                Vec3 dir = e.position().subtract(pos).normalize();
                e.setDeltaMovement(dir.scale(2.5).add(0, 0.5, 0));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.WITHER,            240, 0, false, true));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 240, 3, false, true));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          240, 1, false, true));
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,         240, 0, false, true));
            });
            immediateDeterioration(player, level, 16, 9);
            corruptGround(player, level, 10);
            if (level instanceof ServerLevel sl) catastropheParticles(sl, pos, 28, 80);
            player.sendSystemMessage(Component.literal("[barometer] calamity wave released.")
                    .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));

        } else if (pressure >= 250) {
            // ── Storm Pulse ───────────────────────────────────
            data.putInt("CB_Pressure", pressure - 80);
            level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(20), e -> e != player
            ).forEach(e -> {
                Vec3 dir = e.position().subtract(pos).normalize();
                e.setDeltaMovement(dir.scale(2.0).add(0, 0.4, 0));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2, false, true));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          200, 1, false, true));
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,         200, 0, false, true));
            });
            // Lightning strike 5 random entities
            level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(20), e -> e != player
            ).stream().limit(5).forEach(e -> {
                e.hurt(level.damageSources().magic(), 4f + player.getPersistentData().getInt("CB_Fracture")/50);
                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.END_ROD,
                            e.getX(), e.getY() + 2, e.getZ(), 12, 0.1, 0.5, 0.1, 0.2);
                }
            });
            immediateDeterioration(player, level, 12, 6);
            corruptGround(player, level, 6);
            if (level instanceof ServerLevel sl) catastropheParticles(sl, pos, 20, 50);
            player.sendSystemMessage(Component.literal("[barometer] storm pulse vented.")
                    .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));

        } else {
            // ── Pressure Burst ────────────────────────────────
            data.putInt("CB_Pressure", pressure - 80);
            level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(12), e -> e != player
            ).forEach(e -> {
                Vec3 dir = e.position().subtract(pos).normalize();
                e.setDeltaMovement(dir.scale(1.5).add(0, 0.3, 0));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1, false, true));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          160, 0, false, true));
            });
            immediateDeterioration(player, level, 8, 4);
            if (level instanceof ServerLevel sl) catastropheParticles(sl, pos, 12, 25);
            player.sendSystemMessage(Component.literal("[barometer] pressure vented.")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    // ═════════════════════════════════════════════════════════
    // MAIN TICK
    // ═════════════════════════════════════════════════════════
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        Level  level  = player.level();
        if (level.isClientSide) return;

        var data = player.getPersistentData();
        boolean holding = isHolding(player);

        tickDown(data, "CB_ReleaseCooldown");

        if (!holding) {
            int notHeld = data.getInt("CB_NotHeldTick") + 1;
            data.putInt("CB_NotHeldTick", notHeld);
            // Pressure decays -4/s, fracture -2 per 10 s
            if ((player.tickCount % 20) == 0) {
                int p = data.getInt("CB_Pressure");
                if (p > 0) data.putInt("CB_Pressure", Math.max(0, p - 4));
            }
            if ((player.tickCount % 200) == 0) {
                int f = data.getInt("CB_Fracture");
                if (f > 0) data.putInt("CB_Fracture", Math.max(0, f - 2));
            }
            return;
        }
        data.putInt("CB_NotHeldTick", 0);

        // ── Pressure accrual: +2/s ────────────────────────────
        if ((player.tickCount % 20) == 0) {
            int fracture = data.getInt("CB_Fracture");
            // Splitting tier halves pressure gain
            int gain = (fracture >= FRAC_SPLITTING) ? 1 : 2;
            addPressure(data, gain);
        }

        // ── Fracture accrual: +1 per 10 s ────────────────────
        int fracTick = data.getInt("CB_FractureTick") - 1;
        if (fracTick <= 0) {
            data.putInt("CB_FractureTick", FRACTURE_ACCRUAL);
            addFracture(data, 1);
        } else {
            data.putInt("CB_FractureTick", fracTick);
        }

        int pressure = data.getInt("CB_Pressure");
        int fracture = data.getInt("CB_Fracture");

        // ── Collapse check ────────────────────────────────────
        if (fracture >= FRAC_MAX) {
            executeCollapse(player, level, data);
            return;
        }

        // ── Passive: Calamity Attractor ───────────────────────
        if (pressure >= PRES_ATTRACTOR_I) {
            int attrTick = data.getInt("CB_AttractorTick") - 1;
            if (attrTick <= 0) {
                data.putInt("CB_AttractorTick", ATTRACTOR_INT);
                tickAttractor(player, level, pressure);
            } else {
                data.putInt("CB_AttractorTick", attrTick);
            }
        }

        // ── Passive: Structural Weakness ──────────────────────
        int detInt = (pressure >= 600) ? DETERIORATION_VFAST
                : (pressure >= 400)    ? DETERIORATION_FAST
                : DETERIORATION_INT;
        int detTick = data.getInt("CB_DeteriorTick") - 1;
        if (detTick <= 0) {
            data.putInt("CB_DeteriorTick", detInt);
            tickStructuralWeakness(player, level, pressure, fracture);
        } else {
            data.putInt("CB_DeteriorTick", detTick);
        }

        // ── Drawbacks ─────────────────────────────────────────
        applyFractureDrawbacks(player, level, data, fracture, pressure);

        // ── Ambient particles ─────────────────────────────────
        if (level instanceof ServerLevel sl && (player.tickCount % 30) == 0) {
            Vec3 p = player.position();
            sl.sendParticles(ParticleTypes.SMOKE,
                    p.x + (Math.random() - 0.5) * 0.8,
                    p.y + Math.random() * 2,
                    p.z + (Math.random() - 0.5) * 0.8,
                    1, 0, 0.02, 0, 0.01);
        }

        // ── Periodic pressure readout ─────────────────────────
        if ((player.tickCount % 200) == 0) {
            player.sendSystemMessage(
                    Component.literal("[barometer] " + pressure + " hPa | fracture: "
                            + fracture + " | " + getTierName(fracture))
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    // ── Calamity Attractor tick ───────────────────────────────
    private static void tickAttractor(Player player, Level level, int pressure) {
        double range = (pressure >= PRES_ATTRACTOR_III) ? ATTRACTOR_RANGE_III
                : (pressure >= PRES_ATTRACTOR_II)       ? ATTRACTOR_RANGE_II
                : ATTRACTOR_RANGE_I;
        float dmg = (pressure >= PRES_ATTRACTOR_III) ? 3f
                : (pressure >= PRES_ATTRACTOR_II)    ? 2f : 1f;

        level.getEntitiesOfClass(Monster.class,
                player.getBoundingBox().inflate(range)
        ).forEach(mob -> mob.hurt(level.damageSources().magic(), dmg));

        // Shockwave knockback every 5 s
        if ((player.tickCount % 100) == 0) {
            Vec3 pos = player.position();
            level.getEntitiesOfClass(Monster.class,
                    player.getBoundingBox().inflate(range)
            ).forEach(mob -> {
                Vec3 dir = mob.position().subtract(pos).normalize();
                mob.addDeltaMovement(dir.scale(0.4));
                mob.hurtMarked = true;
            });
        }

        // Passive explosion near mob (at 300+)
        if (pressure >= PRES_ATTRACTOR_II) {
            int expInt = (pressure >= PRES_ATTRACTOR_III) ? EXPLOSION_INT_HI : EXPLOSION_INT_LO;
            // tracked via explosion tick
        }
    }

    // ── Structural Weakness tick ──────────────────────────────
    private static void tickStructuralWeakness(Player player, Level level,
                                               int pressure, int fracture) {
        if (!(level instanceof ServerLevel)) return;
        int radius = (pressure >= 600) ? 14 : (pressure >= 400) ? 10 : 6;
        int count  = (pressure >= 600) ? 8  : (pressure >= 400) ? 5  : 2;

        BlockPos center = player.blockPosition();
        for (int attempt = 0; attempt < count * 3 && count > 0; attempt++) {
            int dx = level.random.nextInt(radius * 2 + 1) - radius;
            int dy = level.random.nextInt(5) - 2;
            int dz = level.random.nextInt(radius * 2 + 1) - radius;
            BlockPos pos = center.offset(dx, dy, dz);
            if (deteriorateBlock(level, pos, fracture >= FRAC_BREAKING)) count--;
        }

        // Breaking tier: also deteriorate block player stands on
        if (fracture >= FRAC_BREAKING) {
            deteriorateBlock(level, player.blockPosition().below(), false);
        }
    }

    // ── Block deterioration ───────────────────────────────────
    private static boolean deteriorateBlock(Level level, BlockPos pos, boolean aggressive) {
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.STONE_BRICKS)) {
            level.setBlock(pos, Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 3); return true;
        } else if (state.is(Blocks.CRACKED_STONE_BRICKS) || state.is(Blocks.STONE)) {
            level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3); return true;
        } else if (state.is(Blocks.COBBLESTONE)) {
            level.setBlock(pos, Blocks.GRAVEL.defaultBlockState(), 3); return true;
        } else if (state.is(Blocks.GRAVEL)) {
            level.setBlock(pos, Blocks.SAND.defaultBlockState(), 3); return true;
        } else if (state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); return true;
        } else if (state.is(Blocks.OAK_LEAVES) || state.is(Blocks.SPRUCE_LEAVES)
                || state.is(Blocks.BIRCH_LEAVES) || state.is(Blocks.JUNGLE_LEAVES)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); return true;
        } else if (aggressive && (state.is(Blocks.DEEPSLATE) || state.is(Blocks.STONE)
                || state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK))) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); return true;
        }
        return false;
    }

    // ── Immediate deterioration burst (on active) ─────────────
    private static void immediateDeterioration(Player player, Level level,
                                               int radius, int count) {
        BlockPos center = player.blockPosition();
        for (int attempt = 0; attempt < count * 4 && count > 0; attempt++) {
            int dx = level.random.nextInt(radius * 2 + 1) - radius;
            int dy = level.random.nextInt(5) - 2;
            int dz = level.random.nextInt(radius * 2 + 1) - radius;
            if (deteriorateBlock(level, center.offset(dx, dy, dz), false)) count--;
        }
    }

    // ── Corrupt ground ────────────────────────────────────────
    private static void corruptGround(Player player, Level level, int radius) {
        BlockPos center = player.blockPosition();
        for (int i = 0; i < radius * 2; i++) {
            int dx = level.random.nextInt(radius * 2 + 1) - radius;
            int dz = level.random.nextInt(radius * 2 + 1) - radius;
            BlockPos pos = center.offset(dx, -1, dz);
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)) {
                level.setBlock(pos, Blocks.COARSE_DIRT.defaultBlockState(), 3);
            } else if (state.is(Blocks.COARSE_DIRT)) {
                level.setBlock(pos, Blocks.GRAVEL.defaultBlockState(), 3);
            }
        }
    }

    // ── Fracture drawbacks ────────────────────────────────────
    private static void applyFractureDrawbacks(Player player, Level level,
                                               net.minecraft.nbt.CompoundTag data,
                                               int fracture, int pressure) {
        // Hairline: ear-pop flavor
        if ((player.tickCount % 2400) == 0) {
            player.sendSystemMessage(
                    Component.literal("[barometer] pressure differential noted.")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }

        // Cracked (150+)
        if (fracture >= FRAC_CRACKED) {
            // Hunger drain
            if ((player.tickCount % 40) == 0) {
                player.getFoodData().setFoodLevel(
                        Math.max(0, player.getFoodData().getFoodLevel() - 1));
            }
            // Stagger
            int staggerTick = data.getInt("CB_StaggerTick") - 1;
            if (staggerTick <= 0) {
                data.putInt("CB_StaggerTick", STAGGER_INT + level.random.nextInt(200));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, true));
                player.sendSystemMessage(
                        Component.literal("[barometer] pressure drop detected.")
                                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            } else {
                data.putInt("CB_StaggerTick", staggerTick);
            }
        }

        // Splitting (300+)
        if (fracture >= FRAC_SPLITTING) {
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER,   40, 0, false, false));
            // Self damage every 20 s
            int selfDmgTick = data.getInt("CB_SelfDmgTick") - 1;
            if (selfDmgTick <= 0) {
                data.putInt("CB_SelfDmgTick", 400);
                player.hurt(level.damageSources().magic(), 1f);
                player.sendSystemMessage(
                        Component.literal("[barometer] internal reading: critical.")
                                .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            } else {
                data.putInt("CB_SelfDmgTick", selfDmgTick);
            }
            // Nausea flashes
            int nauseaTick = data.getInt("CB_NauseaTick") - 1;
            if (nauseaTick <= 0) {
                data.putInt("CB_NauseaTick", NAUSEA_INT);
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0, false, true));
            } else {
                data.putInt("CB_NauseaTick", nauseaTick);
            }
            // Whispers
            int whisperTick = data.getInt("CB_WhisperTick") - 1;
            if (whisperTick <= 0) {
                data.putInt("CB_WhisperTick", WHISPER_INT);
                player.sendSystemMessage(
                        Component.literal(WHISPERS[level.random.nextInt(WHISPERS.length)])
                                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            } else {
                data.putInt("CB_WhisperTick", whisperTick);
            }
        }

        // Fracturing (500+)
        if (fracture >= FRAC_FRACTURING) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
            // Self damage every 15 s
            int selfDmgTick = data.getInt("CB_SelfDmgTick") - 1;
            if (selfDmgTick <= 0) {
                data.putInt("CB_SelfDmgTick", 300);
                player.hurt(level.damageSources().magic(), 2f);
            } else {
                data.putInt("CB_SelfDmgTick", selfDmgTick);
            }
        }

        // Breaking (700+)
        if (fracture >= FRAC_BREAKING) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          40, 1, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));

            // Self damage every 10 s
            int selfDmgTick = data.getInt("CB_SelfDmgTick") - 1;
            if (selfDmgTick <= 0) {
                data.putInt("CB_SelfDmgTick", 200);
                player.hurt(level.damageSources().magic(), 3f);
                player.sendSystemMessage(
                        Component.literal("[barometer] containment degrading.")
                                .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            } else {
                data.putInt("CB_SelfDmgTick", selfDmgTick);
            }

            // Micro-explosions (no block dmg)
            int microExpTick = data.getInt("CB_MicroExpTick") - 1;
            if (microExpTick <= 0) {
                data.putInt("CB_MicroExpTick", MICROEXP_INT_LO);
                Vec3 p = player.position();
                double ox = (level.random.nextDouble() - 0.5) * 4;
                double oz = (level.random.nextDouble() - 0.5) * 4;
                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.EXPLOSION,
                            p.x + ox, p.y + 1, p.z + oz, 1, 0, 0, 0, 0);
                }
                // Proximity damage
                level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(3)
                ).forEach(e -> {
                    if (e == player) {
                        player.hurt(level.damageSources().magic(),
                                0.5f + level.random.nextFloat());
                    } else {
                        e.hurt(level.damageSources().magic(), 2f);
                    }
                });
            } else {
                data.putInt("CB_MicroExpTick", microExpTick);
            }

            // Buff nearby hostiles
            if ((player.tickCount % 20) == 0) {
                level.getEntitiesOfClass(Monster.class,
                        player.getBoundingBox().inflate(20)
                ).forEach(mob ->
                        mob.addEffect(new MobEffectInstance(
                                MobEffects.MOVEMENT_SPEED, 30, 0, false, false))
                );
            }
        }

        // Collapse Imminent (900+)
        if (fracture >= FRAC_COLLAPSE) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER,            40, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          40, 1, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));

            // Heavier self damage
            int selfDmgTick = data.getInt("CB_SelfDmgTick") - 1;
            if (selfDmgTick <= 0) {
                data.putInt("CB_SelfDmgTick", 160);
                player.hurt(level.damageSources().magic(), 4f);
            } else {
                data.putInt("CB_SelfDmgTick", selfDmgTick);
            }

            // Real explosions with block damage every 15 s
            int microExpTick = data.getInt("CB_MicroExpTick") - 1;
            if (microExpTick <= 0) {
                data.putInt("CB_MicroExpTick", MICROEXP_INT_HI);
                Vec3 p = player.position();
                double ox = (level.random.nextDouble() - 0.5) * 6;
                double oz = (level.random.nextDouble() - 0.5) * 6;
                if (level instanceof ServerLevel sl) {
                    sl.explode(null, p.x + ox, p.y, p.z + oz,
                            0.8f, Level.ExplosionInteraction.BLOCK);
                }
                player.sendSystemMessage(
                        Component.literal("[barometer] structural failure imminent.")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            } else {
                data.putInt("CB_MicroExpTick", microExpTick);
            }

            // Stronger mob buffs
            if ((player.tickCount % 20) == 0) {
                level.getEntitiesOfClass(Monster.class,
                        player.getBoundingBox().inflate(30)
                ).forEach(mob -> {
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 1, false, false));
                    mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,   30, 0, false, false));
                });
            }

            // Final whisper: needle points at you
            if ((player.tickCount % 1200) == 0) {
                player.sendSystemMessage(
                        Component.literal("[barometer] the needle is pointing at you.")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
                player.sendSystemMessage(
                        Component.literal("[barometer] it has always been pointing at you.")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            }
        }
    }

    // ── Contained Collapse ────────────────────────────────────
    private static void executeCollapse(Player player, Level level,
                                        net.minecraft.nbt.CompoundTag data) {
        player.hurt(level.damageSources().magic(), 32f); // 16 hearts
        player.addEffect(new MobEffectInstance(MobEffects.WITHER,            2400, 2, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,         2400, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 2400, 4, false, true));

        // Catastrophic deterioration in 5 blocks
        immediateDeterioration(player, level, 5, 40);

        // Reset
        data.putInt("CB_Fracture", FRAC_FRACTURING);  // 500 — not 0
        data.putInt("CB_Pressure", 0);

        // Notify nearby players
        if (level instanceof ServerLevel sl) {
            catastropheParticles(sl, player.position(), 10, 60);
            sl.explode(null, player.getX(), player.getY(), player.getZ(),
                    1.5f, Level.ExplosionInteraction.BLOCK);
        }

        player.sendSystemMessage(Component.literal("════════════════════════════════")
                .withStyle(ChatFormatting.DARK_RED));
        player.sendSystemMessage(Component.literal("CONTAINED COLLAPSE")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("[barometer] the vessel cracked. not shattered.")
                .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal("[barometer] the barometer is already broken. it cannot break further.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal("════════════════════════════════")
                .withStyle(ChatFormatting.DARK_RED));

        // Nearby players feel the wave
        level.getEntitiesOfClass(Player.class,
                player.getBoundingBox().inflate(30), p -> p != player
        ).forEach(nearby -> nearby.sendSystemMessage(
                Component.literal("[barometer] you felt a pressure wave from nearby.")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
    }

    // ── Incoming damage: +15 pressure ────────────────────────
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!isHolding(player)) return;
        if (event.getAmount() >= 8f) {
            addPressure(player.getPersistentData(), 15);
        }

        // Ward: deflect projectile based on pressure
        // (Handled via separate projectile event — simplified here via
        // random deflect on any ranged damage source)
        if (event.getSource().isDirect()) {
            var data = player.getPersistentData();
            int pressure = data.getInt("CB_Pressure");
            float deflectChance = (pressure >= PRES_WARD_III) ? 0.55f
                    : (pressure >= PRES_WARD_II) ? 0.35f
                    : (pressure >= PRES_WARD_I)  ? 0.20f : 0f;
            if (deflectChance > 0 && player.level().random.nextFloat() < deflectChance) {
                event.setCanceled(true);
                player.sendSystemMessage(
                        Component.literal("[barometer] projectile deflected by pressure field.")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                if (player.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CRIT,
                            player.getX(), player.getY() + 1, player.getZ(),
                            8, 0.3, 0.3, 0.3, 0.15);
                }
            }
        }
    }

    // ── On kill: +8 pressure, +5 fracture ────────────────────
    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!isHolding(player)) return;
        var data = player.getPersistentData();
        addPressure(data, 8);
        addFracture(data, 5);
    }

    // ── Helpers ───────────────────────────────────────────────
    private static void addPressure(net.minecraft.nbt.CompoundTag data, int amount) {
        int p = data.getInt("CB_Pressure");
        data.putInt("CB_Pressure", Math.min(999, p + amount));
    }

    private static void addFracture(net.minecraft.nbt.CompoundTag data, int amount) {
        int f = data.getInt("CB_Fracture");
        data.putInt("CB_Fracture", Math.min(FRAC_MAX, f + amount));
    }

    private static void tickDown(net.minecraft.nbt.CompoundTag data, String key) {
        int v = data.getInt(key);
        if (v > 0) data.putInt(key, v - 1);
    }

    private static boolean isHolding(Player player) {
        for (InteractionHand hand : InteractionHand.values())
            if (player.getItemInHand(hand).getItem() instanceof CrackedBarometer) return true;
        return false;
    }

    private static void catastropheParticles(ServerLevel sl, Vec3 pos,
                                              double radius, int count) {
        for (int i = 0; i < count; i++) {
            double angle = Math.random() * Math.PI * 2;
            double r     = Math.random() * radius;
            sl.sendParticles(ParticleTypes.SMOKE,
                    pos.x + Math.cos(angle) * r,
                    pos.y + Math.random() * 3,
                    pos.z + Math.sin(angle) * r,
                    1, 0, 0.05, 0, 0.08);
        }
        sl.sendParticles(ParticleTypes.EXPLOSION,
                pos.x, pos.y + 1, pos.z, 3, 0.5, 0.2, 0.5, 0.1);
    }

    private static String getTierName(int fracture) {
        if (fracture >= FRAC_COLLAPSE)   return "collapse imminent";
        if (fracture >= FRAC_BREAKING)   return "breaking";
        if (fracture >= FRAC_FRACTURING) return "fracturing";
        if (fracture >= FRAC_SPLITTING)  return "splitting";
        if (fracture >= FRAC_CRACKED)    return "cracked";
        return "hairline";
    }

    // ── Tooltip ───────────────────────────────────────────────

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Forces the item to always render with the enchanted glow
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.literal("Beyonder Characteristic — Sequence 4")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Calamity Shaman  |  Disaster / Calamity Pathway")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Storm Pressure (+2/s held):")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Powers all abilities. Max 999. Decays when dropped.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Calamity Attractor (100+ pressure):")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Nearby hostiles take continuous magic damage.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Disaster Ward (150+ pressure):")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  20–55% chance to deflect incoming projectiles.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Structural Weakness:")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Nearby blocks deteriorate. Glass shatters.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Right-click — Release Pressure (25 s CD, costs 80):")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  100: Burst. 250: Storm. 500: Wave. 750+: Absolute.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("⚠ Drawback — Fracture Points (+1 per 10 s held):")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  150 — Cracked: hunger, staggers, armor bypass.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  300 — Splitting: self damage, nausea, whispers.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  500 — Fracturing: Weakness I, more self damage.")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  700 — Breaking: micro-explosions near player.")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  900 — Collapse Imminent: real explosions, Wither.")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  1000 — CONTAINED COLLAPSE. Resets to 500.")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  The barometer is already broken. It cannot break further.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("\"Coincidence is the first thing the Calamity pathway destroys.\"")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
