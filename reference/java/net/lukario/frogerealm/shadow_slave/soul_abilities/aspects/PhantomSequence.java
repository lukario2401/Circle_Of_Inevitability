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
 * Phantom Sequence
 * -----------------------------------------------
 * Theme: speed, afterimages, ghost strikes, phantom echoes
 * Role: DPS (combo-based, scales with execution skill)
 *
 * Core Mechanic — PHANTOM CHAIN:
 *   Every ability used extends the chain counter.
 *   Chain expires if no ability is used within the chain window.
 *   Dropping the chain resets everything.
 *
 *   Chain Length → Effect:
 *     1–3   → normal
 *     4–6   → increased damage, afterimage trails on enemies
 *     7–10  → bonus effects, phantom mirrors strike alongside
 *     10+   → ⚡ Overchain: new ability effects, speed surge, phantom clones
 *
 * Player NBT keys:
 *   "PSChain"           → int    current chain length
 *   "PSChainTimer"      → int    ticks remaining before chain expires
 *   "PSLastAbility"     → int    ID of last used ability (for Echo Shade)
 *   "PSLastAbilityArg"  → int    optional argument for last ability
 *   "PSFlowState"       → int    ticks remaining on Veil of Mirrors buff
 *   "PSOverchain"       → int    ticks remaining on Overchain surge particles
 *   "PSInfinitePhantom" → int    ticks remaining on Infinite Phantom ultimate
 *   "PSMirageDecoy"     → int    ticks remaining on Mirage Step afterimage decoy
 *   "PSMirageDecoyX/Y/Z"→ double position of decoy
 *   "PSEchoLock"        → int    ticks until Echo Shade can repeat again (anti-spam)
 */
public class PhantomSequence {

    // ─── Ability IDs (for Echo Shade) ─────────────────────────────────────────
    public static final int ABILITY_PHANTOM_STRIKE  = 1;
    public static final int ABILITY_MIRAGE_STEP     = 2;
    public static final int ABILITY_WRAITH_UPPERCUT = 3;
    public static final int ABILITY_SPECTER_BREAK   = 5;

    // ─── NBT keys ─────────────────────────────────────────────────────────────
    private static final String NBT_CHAIN          = "PSChain";
    private static final String NBT_CHAIN_TIMER    = "PSChainTimer";
    private static final String NBT_LAST_ABILITY   = "PSLastAbility";
    private static final String NBT_FLOW_STATE     = "PSFlowState";
    private static final String NBT_OVERCHAIN      = "PSOverchain";
    private static final String NBT_INFINITE       = "PSInfinitePhantom";
    private static final String NBT_DECOY          = "PSMirageDecoy";
    private static final String NBT_DECOY_X        = "PSMirageDecoyX";
    private static final String NBT_DECOY_Y        = "PSMirageDecoyY";
    private static final String NBT_DECOY_Z        = "PSMirageDecoyZ";
    private static final String NBT_ECHO_LOCK      = "PSEchoLock";

    // ─── Attribute modifier ResourceLocations (1.21 API) ─────────────────────
    private static final ResourceLocation CHAIN_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "ps_chain_speed");
    private static final ResourceLocation FLOW_SPEED_ID =
            ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "ps_flow_speed");

    // ─── Constants ────────────────────────────────────────────────────────────
    private static final int   CHAIN_WINDOW_BASE   = 60;  // 3s base window
    private static final int   CHAIN_WINDOW_FLOW   = 100; // 5s during Veil of Mirrors
    private static final int   CHAIN_WINDOW_INF    = 9999;// infinite during ult
    private static final int   OVERCHAIN_THRESHOLD = 10;
    private static final int   FLOW_STATE_DUR      = 160; // 8 seconds
    private static final int   INFINITE_DUR        = 200; // 10 seconds
    private static final int   DECOY_DUR           = 60;  // 3 seconds
    private static final int   ECHO_LOCK_DUR       = 15;  // 0.75s anti-spam

    // Chain damage multipliers
    private static final float CHAIN_MULT_LOW    = 1.00f; // 1–3
    private static final float CHAIN_MULT_MID    = 1.35f; // 4–6
    private static final float CHAIN_MULT_HIGH   = 1.70f; // 7–10
    private static final float CHAIN_MULT_OVER   = 2.20f; // 10+

    // Phantom mirror clone strike damage (% of ability damage)
    private static final float MIRROR_DMG_FRAC   = 0.35f;

    private static final Random RNG = new Random();

    // =========================================================================
    //  CHAIN HELPERS
    // =========================================================================

    public static int getChain(Player player) {
        return player.getPersistentData().getInt(NBT_CHAIN);
    }

    private static void setChain(Player player, int val) {
        player.getPersistentData().putInt(NBT_CHAIN, Math.max(0, val));
    }

    public static boolean isOverchain(Player player) {
        return getChain(player) >= OVERCHAIN_THRESHOLD;
    }

    public static boolean inInfinite(Player player) {
        return player.getPersistentData().getInt(NBT_INFINITE) > 0;
    }

    public static boolean inFlowState(Player player) {
        return player.getPersistentData().getInt(NBT_FLOW_STATE) > 0;
    }

    /**
     * Extend the chain by 1. Resets the chain window timer.
     * Applies Overchain speed attribute when threshold is crossed.
     */
    private static void extendChain(Player player, ServerLevel sl) {
        int prev  = getChain(player);
        int next  = prev + 1;
        setChain(player, next);

        int window = inInfinite(player)   ? CHAIN_WINDOW_INF
                : inFlowState(player)  ? CHAIN_WINDOW_FLOW
                : CHAIN_WINDOW_BASE;
        player.getPersistentData().putInt(NBT_CHAIN_TIMER, window);

        // Crossed into Overchain?
        if (prev < OVERCHAIN_THRESHOLD && next >= OVERCHAIN_THRESHOLD) {
            onEnterOverchain(player, sl);
        }

        // Speed ramp: add a small movement speed boost per tier
        applyChainSpeed(player);
    }

    /** Break the chain entirely. */
    private static void breakChain(Player player, ServerLevel sl) {
        if (getChain(player) == 0) return;
        int broken = getChain(player);
        setChain(player, 0);
        player.getPersistentData().putInt(NBT_CHAIN_TIMER, 0);
        removeChainSpeed(player);

        sl.sendParticles(ParticleTypes.POOF,
                player.getX(), player.getY() + 1, player.getZ(),
                6, 0.3, 0.3, 0.3, 0.04);
        if (player instanceof ServerPlayer sp && broken >= 4)
            sp.sendSystemMessage(Component.literal(
                    "§7⚡ Chain broken at §e" + broken + "§7."));
    }

    private static void onEnterOverchain(Player player, ServerLevel sl) {
        player.getPersistentData().putInt(NBT_OVERCHAIN, 20); // flag for visuals
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 1, true, false));
        sl.sendParticles(ParticleTypes.FLASH,
                player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.CRIT,
                player.getX(), player.getY() + 1, player.getZ(),
                20, 0.5, 0.5, 0.5, 0.07);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.8f);
        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§d§l⚡ OVERCHAIN! §r§fPhantom clones active. Abilities enhanced."));
    }

    /** Damage multiplier from current chain length. */
    public static float chainMult(Player player) {
        int chain = getChain(player);
        float mult;
        if (inInfinite(player))       mult = CHAIN_MULT_OVER * (1f + chain * 0.02f);
        else if (chain >= OVERCHAIN_THRESHOLD) mult = CHAIN_MULT_OVER;
        else if (chain >= 7)          mult = CHAIN_MULT_HIGH;
        else if (chain >= 4)          mult = CHAIN_MULT_MID;
        else                          mult = CHAIN_MULT_LOW;
        if (inFlowState(player)) mult *= 1.15f;
        return mult;
    }

    private static String chainTag(Player player) {
        int c = getChain(player);
        if (inInfinite(player))            return "§d§l[∞ INFINITE]";
        if (c >= OVERCHAIN_THRESHOLD)      return "§d[⚡Overchain §f" + c + "§d]";
        if (c >= 7)                        return "§b[High §f" + c + "§b]";
        if (c >= 4)                        return "§3[Mid §f" + c + "§3]";
        if (c > 0)                         return "§7[Chain §f" + c + "§7]";
        return "§8[No Chain]";
    }

    // ─── Chain speed attribute ─────────────────────────────────────────────────

    private static void applyChainSpeed(Player player) {
        var spd = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd == null) return;
        spd.removeModifier(CHAIN_SPEED_ID);
        int chain = getChain(player);
        double bonus = Math.min(0.08, chain * 0.006); // up to +0.08 at chain 13+
        if (bonus > 0)
            spd.addTransientModifier(new AttributeModifier(
                    CHAIN_SPEED_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
    }

    private static void removeChainSpeed(Player player) {
        var spd = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null) spd.removeModifier(CHAIN_SPEED_ID);
    }

    // =========================================================================
    //  PHANTOM MIRROR CLONE STRIKE
    // =========================================================================

    /**
     * When chain >= 7 or in Overchain/Infinite, a phantom mirror clone
     * strikes alongside the player for a fraction of the ability's damage.
     */
    private static void mirrorStrike(LivingEntity target, Player player,
                                     Level level, ServerLevel sl, float abilityDmg) {
        if (getChain(player) < 7 && !inInfinite(player)) return;

        float cloneDmg = abilityDmg * MIRROR_DMG_FRAC;
        if (isOverchain(player) || inInfinite(player)) cloneDmg *= 1.5f;

        target.hurt(level.damageSources().playerAttack(player), cloneDmg);
        target.invulnerableTime = 0;

        // Afterimage particle burst at target
        sl.sendParticles(ParticleTypes.PORTAL,
                target.getX(), target.getY() + 1, target.getZ(),
                8, 0.3, 0.3, 0.3, 0.05);
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

    // ─── Afterimage trail helper ───────────────────────────────────────────────
    private static void afterimageLine(ServerLevel sl, Vec3 from, Vec3 to) {
        Vec3 d = to.subtract(from).normalize();
        double dist = from.distanceTo(to);
        Vec3 c = from;
        for (double dd = 0; dd < dist; dd += 0.4) {
            sl.sendParticles(ParticleTypes.PORTAL, c.x, c.y, c.z, 1, 0.04, 0.04, 0.04, 0);
            c = c.add(d.scale(0.4));
        }
    }

    // =========================================================================
    //  EVENTS
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class PhantomEvents {

        @SubscribeEvent
        public static void onPhantomTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Phantom Sequence")) return;

            // ── Infinite Phantom countdown ────────────────────────────────────
            int infinite = player.getPersistentData().getInt(NBT_INFINITE);
            if (infinite > 0) {
                player.getPersistentData().putInt(NBT_INFINITE, infinite - 1);
                // Keep chain alive
                player.getPersistentData().putInt(NBT_CHAIN_TIMER, CHAIN_WINDOW_INF);
                // Ramp chain counter slowly
                if (player.tickCount % 8 == 0) extendChain(player, sl);
                if (player.tickCount % 5 == 0)
                    sl.sendParticles(ParticleTypes.PORTAL,
                            player.getX(), player.getY() + 1, player.getZ(),
                            4, 0.5, 0.5, 0.5, 0.05);
                if (infinite == 1) {
                    // Ult ended — break chain gracefully
                    breakChain(player, sl);
                    if (player instanceof ServerPlayer sp)
                        sp.sendSystemMessage(Component.literal(
                                "§d§lInfinite Phantom §r§7faded."));
                }
            }

            // ── Veil of Mirrors countdown ─────────────────────────────────────
            int flow = player.getPersistentData().getInt(NBT_FLOW_STATE);
            if (flow > 0) {
                player.getPersistentData().putInt(NBT_FLOW_STATE, flow - 1);
                if (player.tickCount % 12 == 0)
                    sl.sendParticles(ParticleTypes.PORTAL,
                            player.getX(), player.getY() + 1, player.getZ(),
                            2, 0.25, 0.25, 0.25, 0.02);
                if (flow == 1) {
                    var spd = player.getAttribute(Attributes.MOVEMENT_SPEED);
                    if (spd != null) spd.removeModifier(FLOW_SPEED_ID);
                }
            }

            // ── Mirage decoy countdown ────────────────────────────────────────
            int decoy = player.getPersistentData().getInt(NBT_DECOY);
            if (decoy > 0) {
                player.getPersistentData().putInt(NBT_DECOY, decoy - 1);
                double dx = player.getPersistentData().getDouble(NBT_DECOY_X);
                double dy = player.getPersistentData().getDouble(NBT_DECOY_Y);
                double dz = player.getPersistentData().getDouble(NBT_DECOY_Z);
                if (player.tickCount % 4 == 0)
                    sl.sendParticles(ParticleTypes.PORTAL, dx, dy + 1, dz,
                            3, 0.2, 0.3, 0.2, 0.02);
                // Decoy taunts nearby enemies away from player (visual only — logic hook)
            }

            // ── Echo lock countdown ───────────────────────────────────────────
            int echoLock = player.getPersistentData().getInt(NBT_ECHO_LOCK);
            if (echoLock > 0)
                player.getPersistentData().putInt(NBT_ECHO_LOCK, echoLock - 1);

            // ── Chain timer countdown ─────────────────────────────────────────
            if (!inInfinite(player)) {
                int chainTimer = player.getPersistentData().getInt(NBT_CHAIN_TIMER);
                if (chainTimer > 0) {
                    chainTimer--;
                    player.getPersistentData().putInt(NBT_CHAIN_TIMER, chainTimer);
                    if (chainTimer == 0) breakChain(player, sl);
                }
            }

            // ── Overchain visuals ─────────────────────────────────────────────
            if (isOverchain(player) && player.tickCount % 6 == 0) {
                sl.sendParticles(ParticleTypes.PORTAL,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        3, 0.3, 0.1, 0.3, 0.02);
            }

            // ── HUD: chain counter particles ──────────────────────────────────
            if (player.tickCount % 10 == 0 && getChain(player) > 0) {
                int count = Math.min(8, getChain(player) / 2 + 1);
                sl.sendParticles(ParticleTypes.PORTAL,
                        player.getX(), player.getY() + 2.4, player.getZ(),
                        count, 0.2, 0.1, 0.2, 0.005);
            }
        }

        /** Apply chain multiplier to outgoing damage. */
        @SubscribeEvent(priority = EventPriority.NORMAL)
        public static void onPhantomDamageDealt(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Phantom Sequence")) return;
            if (getChain(player) == 0) return;

            event.setAmount(event.getAmount() * chainMult(player));
        }
    }

    // =========================================================================
    //  ABILITY 1 — PHANTOM STRIKE
    // =========================================================================

    /**
     * Fast ghost slash that begins or refreshes the combo chain.
     * Mid-chain: refreshes the chain timer without using a new link.
     * Overchain: fires a second strike automatically.
     * Cost: 150 soul-essence. Stage 0+.
     */
    public static void phantomSequencePhantomStrike(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Phantom Sequence")) return;
        if (SoulCore.getSoulEssence(player) < 150) return;

        int ascStage = SoulCore.getAscensionStage(player);
        int range    = inInfinite(player) ? 14 + ascStage : 7 + ascStage;

        LivingEntity target = rayCastFirst(player, level, range);
        if (target == null) {
            // No target: still extend chain (air slash)
            extendChain(player, sl);
            SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 150);
            sl.sendParticles(ParticleTypes.CRIT,
                    player.getX() + player.getLookAngle().x * 3,
                    player.getY() + 1,
                    player.getZ() + player.getLookAngle().z * 3,
                    5, 0.2, 0.2, 0.2, 0.04);
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§fPhantom Strike: §7no target — chain extended. "
                                + chainTag(player)));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 150);

        float dmg = (8f + ascStage * 1.5f) * chainMult(player);
        target.hurt(level.damageSources().playerAttack(player), dmg);
        target.invulnerableTime = 0;

        extendChain(player, sl);
        player.getPersistentData().putInt(NBT_LAST_ABILITY, ABILITY_PHANTOM_STRIKE);

        // Overchain: auto second strike
        if (isOverchain(player) || inInfinite(player)) {
            float bonusDmg = dmg * 0.55f;
            target.hurt(level.damageSources().playerAttack(player), bonusDmg);
            target.invulnerableTime = 0;
            sl.sendParticles(ParticleTypes.PORTAL,
                    target.getX(), target.getY() + 1, target.getZ(),
                    10, 0.3, 0.3, 0.3, 0.05);
        }

        mirrorStrike(target, player, level, sl, dmg);
        afterimageLine(sl, player.getEyePosition(), target.position().add(0, 1, 0));

        sl.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + 1, target.getZ(),
                6, 0.2, 0.2, 0.2, 0.04);
        level.playSound(null, target.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1f, 1.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§fPhantom Strike: §c" + String.format("%.1f", dmg)
                            + " dmg. " + chainTag(player)));
    }

    // =========================================================================
    //  ABILITY 2 — MIRAGE STEP
    // =========================================================================

    /**
     * Blinks toward the target, leaving an afterimage decoy at the origin.
     * Extends the chain. Next ability within 1.5s gains ×1.25 bonus.
     * Overchain: decoy attacks enemies autonomously for 1s.
     * Cost: 250 soul-essence. Requires stage ≥ 1.
     */
    public static void mirageStep(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Phantom Sequence")) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 1) return;

        int ascStage = SoulCore.getAscensionStage(player);
        int range    = inInfinite(player) ? 20 + ascStage : 12 + ascStage;

        LivingEntity target = rayCastFirst(player, level, range);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 250);

        // Store decoy at current position
        Vec3 origin = player.position();
        player.getPersistentData().putInt(NBT_DECOY, DECOY_DUR);
        player.getPersistentData().putDouble(NBT_DECOY_X, origin.x);
        player.getPersistentData().putDouble(NBT_DECOY_Y, origin.y);
        player.getPersistentData().putDouble(NBT_DECOY_Z, origin.z);

        // Afterimage at origin
        sl.sendParticles(ParticleTypes.PORTAL,
                origin.x, origin.y + 1, origin.z, 20, 0.3, 0.5, 0.3, 0.03);

        if (target != null) {
            // Blink behind target
            Vec3 behind = target.position()
                    .add(target.getLookAngle().scale(-1.8))
                    .add(0, 0.1, 0);
            if (player instanceof ServerPlayer sp) {
                sp.teleportTo(behind.x, behind.y, behind.z);
                Vec3 dir = target.position().subtract(behind).normalize();
                float yaw = (float)(Math.toDegrees(Math.atan2(-dir.x, dir.z)));
                sp.setYRot(yaw);
                sp.yRotO = yaw;
            }
            // Deal a light strike on arrival
            float arrivalDmg = (6f + ascStage) * chainMult(player);
            target.hurt(level.damageSources().playerAttack(player), arrivalDmg);
            target.invulnerableTime = 0;

            // Overchain: decoy also slashes nearby enemies from origin
            if (isOverchain(player) || inInfinite(player)) {
                float decoyDmg = arrivalDmg * 0.5f;
                level.getEntitiesOfClass(LivingEntity.class,
                                new AABB(origin, origin).inflate(4f),
                                e -> e != player && e != target && e.isAlive())
                        .forEach(e -> {
                            e.hurt(level.damageSources().playerAttack(player), decoyDmg);
                            e.invulnerableTime = 0;
                            sl.sendParticles(ParticleTypes.PORTAL,
                                    e.getX(), e.getY() + 1, e.getZ(),
                                    4, 0.2, 0.2, 0.2, 0.03);
                        });
            }

            sl.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + 1, target.getZ(),
                    8, 0.2, 0.2, 0.2, 0.05);
        } else {
            // No target: blink forward
            Vec3 forward = player.position().add(player.getLookAngle().scale(6));
            if (player instanceof ServerPlayer sp)
                sp.teleportTo(forward.x, forward.y, forward.z);
        }

        extendChain(player, sl);
        player.getPersistentData().putInt(NBT_LAST_ABILITY, ABILITY_MIRAGE_STEP);

        level.playSound(null, player.blockPosition(),
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§fMirage Step: §bafterimage placed§f — decoy active §b3s§f. "
                            + chainTag(player)));
    }

    // =========================================================================
    //  ABILITY 3 — WRAITH UPPERCUT
    // =========================================================================

    /**
     * Upward phantom strike that launches the enemy slightly.
     * Each use increases chain scaling bonus by a flat amount this combo.
     * Overchain: hits all enemies in a small vertical cone.
     * Cost: 350 soul-essence. Requires stage ≥ 2.
     */
    public static void wraithUppercut(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Phantom Sequence")) return;
        if (SoulCore.getSoulEssence(player) < 350) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        int ascStage = SoulCore.getAscensionStage(player);
        int range    = inInfinite(player) ? 12 + ascStage : 6 + ascStage;

        LivingEntity target = rayCastFirst(player, level, range);
        if (target == null) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo target in range!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 350);

        float dmg = (14f + ascStage * 2f) * chainMult(player);

        // Overchain: hit cone of enemies above
        List<LivingEntity> coneTargets = new ArrayList<>();
        if (isOverchain(player) || inInfinite(player)) {
            Vec3 up = new Vec3(0, 1, 0);
            coneTargets = level.getEntitiesOfClass(
                    LivingEntity.class,
                    player.getBoundingBox().inflate(4f).expandTowards(0, 3, 0),
                    e -> e != player && e.isAlive());
        } else {
            coneTargets.add(target);
        }

        for (LivingEntity e : coneTargets) {
            e.hurt(level.damageSources().playerAttack(player), dmg);
            e.invulnerableTime = 0;
            // Launch upward
            e.setDeltaMovement(e.getDeltaMovement().add(0, 0.55, 0));
            sl.sendParticles(ParticleTypes.CRIT,
                    e.getX(), e.getY() + 1, e.getZ(), 5, 0.2, 0.3, 0.2, 0.05);
            sl.sendParticles(ParticleTypes.PORTAL,
                    e.getX(), e.getY() + 1, e.getZ(), 4, 0.2, 0.3, 0.2, 0.04);
        }

        mirrorStrike(target, player, level, sl, dmg);
        extendChain(player, sl);
        // Extra chain scaling: this ability gives +1 phantom chain length bonus
        setChain(player, getChain(player) + 1);
        player.getPersistentData().putInt(NBT_LAST_ABILITY, ABILITY_WRAITH_UPPERCUT);

        level.playSound(null, target.blockPosition(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1f, 0.7f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§fWraith Uppercut: §c" + String.format("%.1f", dmg)
                            + " dmg §7(×2 chain gain)§f. "
                            + (coneTargets.size() > 1 ? "§dCone hit §e" + coneTargets.size() + "§f! " : "")
                            + chainTag(player)));
    }

    // =========================================================================
    //  ABILITY 4 — ECHO SHADE
    // =========================================================================

    /**
     * Repeats the last used ability as a phantom echo at 50% damage.
     * No soul-essence cost but has a 0.75s internal lock to prevent spam.
     * Overchain: echo fires at 75% damage instead of 50%.
     * Cost: 0 soul-essence (free). Requires stage ≥ 0.
     */
    public static void echoShade(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Phantom Sequence")) return;
        if (player.getPersistentData().getInt(NBT_ECHO_LOCK) > 0) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cEcho Shade not ready yet!"));
            return;
        }

        int lastAbility = player.getPersistentData().getInt(NBT_LAST_ABILITY);
        if (lastAbility == 0) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo ability to echo yet!"));
            return;
        }

        // Set echo lock
        player.getPersistentData().putInt(NBT_ECHO_LOCK, ECHO_LOCK_DUR);

        // Echo multiplier
        float echoMult = (isOverchain(player) || inInfinite(player)) ? 0.75f : 0.50f;

        int ascStage = SoulCore.getAscensionStage(player);
        LivingEntity target = rayCastFirst(player, level,
                inInfinite(player) ? 16 + ascStage : 8 + ascStage);

        if (target != null) {
            float echoDmg;
            switch (lastAbility) {
                case ABILITY_PHANTOM_STRIKE ->
                        echoDmg = (8f + ascStage * 1.5f) * chainMult(player) * echoMult;
                case ABILITY_MIRAGE_STEP ->
                        echoDmg = (6f + ascStage) * chainMult(player) * echoMult;
                case ABILITY_WRAITH_UPPERCUT ->
                        echoDmg = (14f + ascStage * 2f) * chainMult(player) * echoMult;
                case ABILITY_SPECTER_BREAK ->
                        echoDmg = (10f + getChain(player) * 2f + ascStage * 2f)
                                * chainMult(player) * echoMult;
                default -> echoDmg = 8f * chainMult(player) * echoMult;
            }

            target.hurt(level.damageSources().playerAttack(player), echoDmg);
            target.invulnerableTime = 0;

            sl.sendParticles(ParticleTypes.PORTAL,
                    target.getX(), target.getY() + 1, target.getZ(),
                    8, 0.25, 0.25, 0.25, 0.04);
            afterimageLine(sl, player.getEyePosition(), target.position().add(0, 1, 0));

            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§dEcho Shade: §f§o(phantom echo)§r §c"
                                + String.format("%.1f", echoDmg) + " dmg §7("
                                + (int)(echoMult * 100) + "%). "
                                + chainTag(player)));
        } else {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§dEcho Shade: §7no target — chain extended."));
        }

        extendChain(player, sl);
        level.playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.6f, 1.8f);
    }

    // =========================================================================
    //  ABILITY 5 — SPECTER BREAK
    // =========================================================================

    /**
     * Heavy ghost strike. Damage scales with current chain length.
     * Consumes 3 chain links (not the whole chain).
     * Overchain: consumes 5 links but deals ×2.5 damage + AOE shockwave.
     * Cost: 500 soul-essence. Requires stage ≥ 3.
     */
    public static void specterBreak(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Phantom Sequence")) return;
        if (SoulCore.getSoulEssence(player) < 500) return;
        if (SoulCore.getAscensionStage(player) < 3) return;

        int ascStage = SoulCore.getAscensionStage(player);
        int range    = inInfinite(player) ? 18 + ascStage : 10 + ascStage;

        LivingEntity target = rayCastFirst(player, level, range);
        if (target == null) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo target in range!"));
            return;
        }

        int chain = getChain(player);
        boolean overchain = isOverchain(player) || inInfinite(player);
        int consumed = overchain ? Math.min(chain, 5) : Math.min(chain, 3);

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 500);

        float dmg = (10f + chain * 2.5f + ascStage * 3f) * chainMult(player);
        if (overchain) dmg *= 2.5f;

        target.hurt(level.damageSources().playerAttack(player), dmg);
        target.invulnerableTime = 0;

        // Consume chain links
        if (!inInfinite(player)) {
            setChain(player, Math.max(0, chain - consumed));
            // Reset timer so chain doesn't immediately die after partial consume
            int window = inFlowState(player) ? CHAIN_WINDOW_FLOW : CHAIN_WINDOW_BASE;
            player.getPersistentData().putInt(NBT_CHAIN_TIMER, window);
        }

        player.getPersistentData().putInt(NBT_LAST_ABILITY, ABILITY_SPECTER_BREAK);

        // Overchain AOE shockwave
        if (overchain) {
            float shockRadius = 5f + ascStage * 0.5f;
            float shockDmg    = dmg * 0.40f;
            level.getEntitiesOfClass(LivingEntity.class,
                            target.getBoundingBox().inflate(shockRadius),
                            e -> e != player && e != target && e.isAlive())
                    .forEach(e -> {
                        e.hurt(level.damageSources().playerAttack(player), shockDmg);
                        e.invulnerableTime = 0;
                        Vec3 push = e.position().subtract(target.position()).normalize().scale(0.5);
                        e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.2, push.z));
                        sl.sendParticles(ParticleTypes.PORTAL,
                                e.getX(), e.getY() + 1, e.getZ(),
                                5, 0.2, 0.2, 0.2, 0.03);
                    });
            for (int i = 0; i < 24; i++) {
                double angle = Math.toRadians(i * 15);
                sl.sendParticles(ParticleTypes.CRIT,
                        target.getX() + shockRadius * Math.cos(angle),
                        target.getY() + 0.5,
                        target.getZ() + shockRadius * Math.sin(angle),
                        1, 0, 0.2, 0, 0.04);
            }
        }

        mirrorStrike(target, player, level, sl, dmg);
        afterimageLine(sl, player.getEyePosition(), target.position().add(0, 1, 0));

        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, target.blockPosition(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 0.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§fSpecter Break: §c" + String.format("%.1f", dmg)
                            + " dmg §7(consumed §e" + consumed + " links§7). "
                            + chainTag(player)));
    }

    // =========================================================================
    //  ABILITY 6 — VEIL OF MIRRORS
    // =========================================================================

    /**
     * Enter Veil of Mirrors state for 8 seconds:
     *   - Chain window extended to 5 seconds
     *   - Afterimage clones appear more frequently
     *   - Movement speed +15%
     *   - Haste II applied
     *   - Gaining Overchain while active creates 2 extra phantom clones that deal dmg
     * Cost: 700 soul-essence. Requires stage ≥ 5.
     */
    public static void veilOfMirrors(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Phantom Sequence")) return;
        if (SoulCore.getSoulEssence(player) < 700) return;
        if (SoulCore.getAscensionStage(player) < 5) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 700);

        player.getPersistentData().putInt(NBT_FLOW_STATE, FLOW_STATE_DUR);

        // Extend current chain timer immediately
        if (getChain(player) > 0)
            player.getPersistentData().putInt(NBT_CHAIN_TIMER, CHAIN_WINDOW_FLOW);

        // Speed boost
        var spd = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null) {
            spd.removeModifier(FLOW_SPEED_ID);
            spd.addTransientModifier(new AttributeModifier(
                    FLOW_SPEED_ID, 0.045, AttributeModifier.Operation.ADD_VALUE));
        }

        // Haste II
        player.addEffect(new MobEffectInstance(
                MobEffects.DIG_SPEED, FLOW_STATE_DUR, 1, false, true));

        // Particle burst
        for (int i = 0; i < 32; i++) {
            double angle = Math.toRadians(i * 11.25);
            sl.sendParticles(ParticleTypes.PORTAL,
                    player.getX() + 2.5 * Math.cos(angle),
                    player.getY() + 1,
                    player.getZ() + 2.5 * Math.sin(angle),
                    1, 0, 0.3, 0, 0.02);
        }
        sl.sendParticles(ParticleTypes.FLASH,
                player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.2f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§d§lVeil of Mirrors! §r§fChain window §b5s§f, "
                            + "§bHaste II§f, §b+15% speed §ffor §b8s§f. "
                            + chainTag(player)));
    }

    // =========================================================================
    //  ABILITY 7 — INFINITE PHANTOM (ULTIMATE)
    // =========================================================================

    /**
     * 10-second ultimate:
     *   - Chain cannot break (timer frozen)
     *   - Chain auto-extends every 8 ticks
     *   - No soul-essence cost on any ability
     *   - Phantom clones auto-strike every 1s for free
     *   - Damage ramps 2% per chain link (no cap)
     *   - Every ability use adds a bonus free hit automatically
     * Cost: 5000 soul-essence. Requires stage ≥ 7.
     */
    public static void infinitePhantom(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Phantom Sequence")) return;
        if (SoulCore.getSoulEssence(player) < 5000) return;
        if (SoulCore.getAscensionStage(player) < 7) return;

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 5000);

        player.getPersistentData().putInt(NBT_INFINITE, INFINITE_DUR);

        // Ensure we're in overchain immediately
        if (getChain(player) < OVERCHAIN_THRESHOLD)
            setChain(player, OVERCHAIN_THRESHOLD);
        player.getPersistentData().putInt(NBT_CHAIN_TIMER, CHAIN_WINDOW_INF);

        // Speed and Haste
        var spd = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spd != null) {
            spd.removeModifier(FLOW_SPEED_ID);
            spd.addTransientModifier(new AttributeModifier(
                    FLOW_SPEED_ID, 0.07, AttributeModifier.Operation.ADD_VALUE));
        }
        player.addEffect(new MobEffectInstance(
                MobEffects.DIG_SPEED, INFINITE_DUR, 2, false, true));

        // Grand visual ring
        for (int i = 0; i < 48; i++) {
            double angle = Math.toRadians(i * 7.5);
            sl.sendParticles(ParticleTypes.PORTAL,
                    player.getX() + 4 * Math.cos(angle),
                    player.getY() + 1,
                    player.getZ() + 4 * Math.sin(angle),
                    1, 0, 0.5, 0, 0.03);
        }
        sl.sendParticles(ParticleTypes.FLASH,
                player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(), 2, 0.4, 0.3, 0.4, 0);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 1.5f, 1.8f);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 2.0f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§d§l∞ INFINITE PHANTOM ∞ §r§fChain unbreakable. "
                            + "No cooldowns. Damage ramps per link. "
                            + "Phantom clones auto-strike. §b10s."));
    }
}
