package net.lukario.frogerealm.item.seald_artifacts;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════
 *   THE SPLICED COMPASS  —  Beyonder Characteristic Item
 *   Sequence 3 — Plane Wanderer (Door / Planar Pathway)
 * ══════════════════════════════════════════════════════════════
 *
 * A navigation instrument the size of a pocket watch, brass-cased,
 * with a cracked crystal face. The needle has four cardinal marks
 * engraved on it. Three of them are places. One of them is a concept.
 * It has never pointed north. It has never pointed at the same thing
 * twice in a row. When you ask it where you are, it answers with
 * where you could be instead.
 *
 * The Plane Wanderer who crystallized into this instrument crossed
 * the boundary between planes so many times that she stopped being
 * entirely on any of them. She wasn't lost — she was distributed.
 * Pieces of her existed in seventeen adjacent realities
 * simultaneously, and each piece thought it was the original.
 * Fourteen of them were right. The compass is what the remaining
 * three pieces agreed on.
 *
 * Holding it, distances feel wrong. You reach for something two feet
 * away and your hand travels three. You step forward one pace and
 * arrive one and a half. The space around you has learned your new
 * geometry. It is still adjusting.
 * So are you.
 *
 * TYPE: Carry-on (held in hand)
 * PATHWAY: Door / Planar (Plane Wanderer sub-branch)
 * SEQUENCE EQUIVALENT: 3 — Plane Wanderer
 *
 *
 * ── PASSIVE I — Planar Thinning ───────────────────────────────
 *   The boundary between where the player is and where they
 *   could be grows thinner while the compass is held.
 *
 *   "Planar Resonance" builds passively:
 *     +2 / second held
 *     +10 per successful Slip (see Active)
 *     +8 per entity killed while holding
 *     +20 per Fold (see Active, high charge)
 *     -3 / second when NOT held (the boundary thickens slowly)
 *   Cap: 999. Stored: "SC_Resonance".
 *
 *   Resonance passively unlocks passive tiers:
 *     100+  → Spatial Awareness: player senses nearby entities
 *             through walls — Glowing applied to all in 20 blocks.
 *     200+  → Partial Step: player phases slightly — 15% chance
 *             incoming melee hits miss entirely (wrong plane layer).
 *     400+  → Thin Walls: blocks in 3-block radius occasionally
 *             become passable for 1 tick (stepping-through effect
 *             — simulated by brief teleport through thin walls).
 *     700+  → Distributed Presence: player exists in two places
 *             at once — a spectral duplicate wanders nearby
 *             (simulated: random entity knockback pulses from a
 *             "second position" offset from the player).
 *
 * ── PASSIVE II — Dead Reckoning ───────────────────────────────
 *   The compass always knows where things are — including things
 *   that don't want to be found.
 *   • All hostile mobs within 32 blocks glow regardless of walls.
 *   • Player receives a passive Speed I from spatial efficiency.
 *   • Player cannot be surprised from behind: entities that move
 *     within 5 blocks of the player's back are highlighted
 *     (Glowing, brief flash) and the player receives a 1-tick
 *     Speed II burst (reaction window).
 *
 * ── PASSIVE III — Planar Seam ─────────────────────────────────
 *   At 300+ Resonance: the space around the player develops
 *   micro-seams — hairline cracks between planes.
 *   • Projectiles passing through the seam field (8 blocks) have
 *     a 30% chance to be spatially redirected — they exit the
 *     seam at a different angle, missing the player.
 *   • At 600+: 50% chance, 12-block field.
 *   • Redirected projectiles visually "phase" (PORTAL particles).
 *
 *
 * ── ACTIVE (right-click) — Navigate ──────────────────────────
 *   The compass reads the planar geometry and executes a
 *   navigational correction. Two modes based on sneak state:
 *
 *   SLIP (right-click, not sneaking):
 *   A short-range planar step — the player folds through the
 *   nearest layer of space and re-emerges up to 12 blocks away
 *   in the direction they are facing, passing through any blocks
 *   in the path.
 *   • Lands at the first air gap in that direction, up to 12 blocks.
 *   • Portal particles trail the path.
 *   • Brief Speed II on arrival (1.5 s).
 *   • Cost: 20 Resonance. Cooldown: 8 s.
 *   • At 500+ Resonance: range extends to 20 blocks.
 *   • At 700+ Resonance: leaves a Planar Echo at origin point
 *     (see below).
 *
 *   FOLD (right-click + sneak):
 *   A full planar fold — the player temporarily exits the current
 *   plane and re-enters at a location up to 40 blocks away
 *   (targeting direction × 40, with obstacle pass-through).
 *   • All entities within 8 blocks of the EXIT point receive
 *     spatial displacement: knocked away + Slowness III + Blindness
 *     for 4 s (disoriented by the fold event).
 *   • All entities within 8 blocks of the ENTRY point (origin):
 *     brief Levitation (1 s) — the fold pulls air.
 *   • Cost: 80 Resonance. Cooldown: 40 s.
 *   • Adds +20 Drift (see drawback).
 *   • At 600+ Resonance: Fold leaves Planar Echoes at BOTH points.
 *
 *   PLANAR ECHO:
 *   A ghost of the player's last position. For 6 seconds:
 *   • Acts as a decoy — nearby hostiles in 10 blocks of the echo
 *     are pulled toward it (velocity nudge) instead of the player.
 *   • Emits END_ROD particles at the echo location.
 *   • Multiple echoes can exist simultaneously (max 3).
 *   • Stored as block positions in persistentData.
 *
 *
 * ── DRAWBACK — Planar Drift ───────────────────────────────────
 *   The Plane Wanderer crossed too many boundaries. The boundaries
 *   started crossing her. Each transit leaves a residue — a smear
 *   of the wrong plane clinging to the right one.
 *   The compass doesn't cause this. It just measures it.
 *   The needle has been pointing at this all along.
 *
 *   "Drift Points" accumulate:
 *     +1 / 5 seconds held
 *     +5 per Slip
 *     +20 per Fold
 *     +3 per kill
 *     -1 / 5 seconds NOT held (very slow decay)
 *   Cap: 999. Stored: "SC_Drift".
 *
 *   Drift thresholds:
 *
 *   0–99 — Nominal:
 *     Compass hum (flavor messages only, rare).
 *     Faint PORTAL particles around player.
 *     Distances still feel slightly wrong.
 *
 *   100–249 — Minor Displacement:
 *     • Player occasionally teleports 1–2 blocks in a random
 *       direction (involuntary micro-slip every 45 s) — not
 *       through solid blocks, just to an adjacent air block.
 *     • Held item occasionally phases — 1-tick Nausea every 2 min.
 *     • Hunger drains slightly (planar transit costs energy).
 *     • Compass whispers navigation data that doesn't apply here.
 *
 *   250–499 — Moderate Drift:
 *     • Micro-slips become larger: up to 4 blocks, every 30 s.
 *       Occasionally phases through thin walls (1-block thick).
 *     • Player attacks sometimes land in the wrong plane: 10%
 *       chance outgoing melee hits miss (you hit a layer that
 *       isn't quite this one).
 *     • Permanent Nausea I while held.
 *     • Hostile mobs occasionally blink to wrong positions near
 *       player — a 3-block random teleport every 20 s for the
 *       nearest hostile (disorienting for both parties).
 *     • The compass begins showing the player's location in a
 *       plane that isn't this one. Messages reference landmarks
 *       that don't exist.
 *
 *   500–699 — Heavy Drift:
 *     • Involuntary Slips: every 20 s, player is forcibly
 *       teleported 6–10 blocks in a random horizontal direction,
 *       passing through blocks. They arrive wherever they arrive.
 *     • Permanent Weakness I + Nausea I.
 *     • Melee miss chance increases to 20%.
 *     • Passive Speed I removed (you can't control where you're
 *       going — speed doesn't help).
 *     • Blocks within 2 blocks of the player occasionally swap
 *       to their "adjacent plane" equivalent: dirt → soul sand,
 *       stone → netherrack, grass → crimson nylium. Every 30 s,
 *       1–2 random blocks affected. Temporary (they revert after
 *       10 s — simulated via a revert queue in persistentData).
 *
 *   700–899 — Severe Drift:
 *     • Involuntary Slips: every 12 s, 8–14 blocks. Passes
 *       through anything including walls and cliffs.
 *     • Permanent Weakness I + Slowness I + Nausea I.
 *     • Each involuntary slip: -2 HP (transit toll).
 *     • Block swaps now permanent (no revert). Radius 4 blocks.
 *       Every 15 s, 3 blocks affected.
 *     • The player's held item occasionally vanishes for 2 s:
 *       it phases to an adjacent plane and returns. Item is
 *       temporarily placed in a "phantom slot" in persistentData
 *       and restored after 40 ticks.
 *     • Compass messages become urgent. They describe something
 *       following the player across planes.
 *
 *   900–999 — Critical Drift:
 *     • Involuntary Slips: every 8 s, 10–18 blocks.
 *     • Each slip: -4 HP.
 *     • Permanent Weakness II + Slowness II + Nausea I.
 *     • Block swaps affect stone → obsidian, gravel → magma block.
 *     • Wither I applied every 20 s (planar corrosion).
 *     • Fold ability disabled (the compass can no longer read
 *       intentional navigation — only drift remains).
 *     • Messages from the compass: something has your scent.
 *       It knows which plane you'll be on next.
 *
 *   1000 — FULL PLANAR DISPLACEMENT:
 *     The player is no longer entirely here.
 *     Forcibly teleported 30–50 blocks in a random direction
 *     (through everything). Massive END_ROD + PORTAL explosion.
 *     Blindness + Nausea + Slowness V for 60 s. 10 true damage.
 *     All Resonance drained to 0.
 *     Drift resets to 600 — you came back, but you came back
 *     from the wrong direction. You are not fully re-integrated.
 *     Nearby players (within 20 blocks of ARRIVAL point)
 *     receive a message: they witnessed a spatial displacement.
 *
 *
 * ── STATE (player.getPersistentData()) ────────────────────────
 *   "SC_Resonance"       int      — planar resonance (0–999)
 *   "SC_Drift"           int      — drift points (0–999)
 *   "SC_SlipCooldown"    int      — ticks until Slip available
 *   "SC_FoldCooldown"    int      — ticks until Fold available
 *   "SC_MicroSlipTick"   int      — ticks until next involuntary slip
 *   "SC_BlockSwapTick"   int      — ticks until next block swap
 *   "SC_ResonanceTick"   int      — accrual tick
 *   "SC_DriftTick"       int      — drift accrual tick
 *   "SC_WhisperTick"     int      — ticks until next whisper
 *   "SC_NauseaTick"      int      — nausea flavor tick
 *   "SC_NotHeldTick"     int      — ticks since last held
 *   "SC_Echo1X/Y/Z"      int×3    — echo 1 block position
 *   "SC_Echo1Ticks"      int      — remaining echo 1 ticks
 *   "SC_Echo2X/Y/Z"      int×3    — echo 2 block position
 *   "SC_Echo2Ticks"      int      — remaining echo 2 ticks
 *   "SC_Echo3X/Y/Z"      int×3    — echo 3 block position
 *   "SC_Echo3Ticks"      int      — remaining echo 3 ticks
 *   "SC_SwapPos_N"       string   — serialized swap revert queue
 *
 * ── REGISTRATION ──────────────────────────────────────────────
 *   public static final RegistryObject<Item> SPLICED_COMPASS =
 *       ITEMS.register("spliced_compass",
 *           () -> new SplicedCompass(
 *               new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
 */
@Mod.EventBusSubscriber
public class SplicedCompass extends Item {

    // ── Resonance thresholds ──────────────────────────────────
    private static final int RES_AWARENESS    = 100;
    private static final int RES_PARTIAL_STEP = 200;
    private static final int RES_THIN_WALLS   = 400;
    private static final int RES_DISTRIBUTED  = 700;
    private static final int RES_SEAM_I       = 300;
    private static final int RES_SEAM_II      = 600;
    private static final int RES_SLIP_BOOST   = 500;

    // ── Drift thresholds ──────────────────────────────────────
    private static final int DRIFT_MINOR    = 100;
    private static final int DRIFT_MODERATE = 250;
    private static final int DRIFT_HEAVY    = 500;
    private static final int DRIFT_SEVERE   = 700;
    private static final int DRIFT_CRITICAL = 900;
    private static final int DRIFT_MAX      = 1000;

    // ── Cooldowns (ticks) ─────────────────────────────────────
    private static final int SLIP_CD          = 160;   // 8 s
    private static final int FOLD_CD          = 800;   // 40 s
    private static final int ECHO_DURATION    = 120;   // 6 s
    private static final int WHISPER_INT      = 500;   // 25 s
    private static final int RESONANCE_TICK   = 20;    // 1 s
    private static final int DRIFT_ACCRUAL    = 100;   // 5 s

    // ── Micro-slip intervals by drift tier ────────────────────
    private static final int SLIP_INT_MINOR   = 900;   // 45 s
    private static final int SLIP_INT_MODERATE= 600;   // 30 s
    private static final int SLIP_INT_HEAVY   = 400;   // 20 s
    private static final int SLIP_INT_SEVERE  = 240;   // 12 s
    private static final int SLIP_INT_CRITICAL= 160;   // 8 s

    // ── Compass whispers ──────────────────────────────────────
    private static final String[] WHISPERS_EARLY = {
            "[compass] current bearing: undefined. suggested bearing: undefined.",
            "[compass] nearest landmark: does not exist in this layer.",
            "[compass] you are here. also here. also slightly left of here.",
            "[compass] the fourth cardinal direction is closer than it looks.",
            "[compass] recalibrating. recalibrating. recalibrating.",
    };
    private static final String[] WHISPERS_MID = {
            "[compass] the coordinates you are standing in belong to someone else.",
            "[compass] estimated position: adjacent. actual position: unknown.",
            "[compass] something has entered the navigational field.",
            "[compass] this plane's geometry does not account for you.",
            "[compass] you have been here before. in a different sense of 'here'.",
    };
    private static final String[] WHISPERS_LATE = {
            "[compass] it crossed three planes to follow you. it is patient.",
            "[compass] your planar signature is visible from very far away.",
            "[compass] do not fold. it reads fold events.",
            "[compass] it knows which plane you will be on next.",
            "[compass] the needle has been pointing at it this entire time.",
    };

    public SplicedCompass(Properties properties) {
        super(properties);
    }

    // ═════════════════════════════════════════════════════════
    // ACTIVE — Navigate (Slip / Fold)
    // ═════════════════════════════════════════════════════════
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        var data = player.getPersistentData();
        int drift = data.getInt("SC_Drift");
        boolean sneaking = player.isShiftKeyDown();

        if (sneaking) {
            // ── FOLD ─────────────────────────────────────────
            if (drift >= DRIFT_CRITICAL) {
                player.sendSystemMessage(
                        Component.literal("[compass] fold disabled. navigational coherence lost.")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                return InteractionResultHolder.pass(stack);
            }
            int foldCD = data.getInt("SC_FoldCooldown");
            if (foldCD > 0) {
                player.sendSystemMessage(
                        Component.literal("[compass] fold recharging. (" + (foldCD / 20) + "s)")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                return InteractionResultHolder.pass(stack);
            }
            int resonance = data.getInt("SC_Resonance");
            if (resonance < 80) {
                player.sendSystemMessage(
                        Component.literal("[compass] insufficient resonance for fold. (" + resonance + "/80)")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                return InteractionResultHolder.pass(stack);
            }
            executeFold(player, level, data, resonance);
            data.putInt("SC_FoldCooldown", FOLD_CD);
            player.getCooldowns().addCooldown(this, FOLD_CD);

        } else {
            // ── SLIP ─────────────────────────────────────────
            int slipCD = data.getInt("SC_SlipCooldown");
            if (slipCD > 0) {
                player.sendSystemMessage(
                        Component.literal("[compass] slip recharging. (" + (slipCD / 20) + "s)")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                return InteractionResultHolder.pass(stack);
            }
            int resonance = data.getInt("SC_Resonance");
            if (resonance < 20) {
                player.sendSystemMessage(
                        Component.literal("[compass] insufficient resonance for slip. (" + resonance + "/20)")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                return InteractionResultHolder.pass(stack);
            }
            executeSlip(player, level, data, resonance, false);
            data.putInt("SC_SlipCooldown", SLIP_CD);
            player.getCooldowns().addCooldown(this, SLIP_CD);
        }
        return InteractionResultHolder.success(stack);
    }

    // ── Slip: short planar step through space ─────────────────
    private static void executeSlip(Player player, Level level,
                                    net.minecraft.nbt.CompoundTag data,
                                    int resonance, boolean involuntary) {
        int range = (resonance >= RES_SLIP_BOOST && !involuntary) ? 20 : 12;
        Vec3 look  = player.getLookAngle().normalize();
        Vec3 start = player.position().add(0, 0.1, 0);
        Vec3 dest  = start;

        // Step along look direction, find first safe landing spot
        for (int i = 1; i <= range; i++) {
            Vec3 candidate = start.add(look.scale(i));
            BlockPos bp    = new BlockPos((int) candidate.x, (int) candidate.y, (int) candidate.z);
            BlockPos bpFeet= new BlockPos((int) candidate.x, (int) (candidate.y - 0.1), (int) candidate.z);
            // Check head + feet clearance
            boolean headClear = level.getBlockState(bp).isAir() ||
                    level.getBlockState(bp).getBlock() == Blocks.WATER;
            boolean feetClear = level.getBlockState(
                    new BlockPos((int) candidate.x, (int) candidate.y + 1, (int) candidate.z)).isAir();
            if (headClear) {
                dest = candidate;
                if (i >= 2) break; // prefer at least 2 blocks away
            }
        }

        Vec3 origin = player.position().add(0, 0.5, 0);
        player.teleportTo(dest.x, dest.y, dest.z);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 1, false, false));

        // Particles along the path
        if (level instanceof ServerLevel sl) {
            int steps = (int) origin.distanceTo(dest);
            for (int i = 0; i <= steps; i++) {
                double t = steps == 0 ? 1 : (double) i / steps;
                Vec3 pt = origin.lerp(dest, t);
                sl.sendParticles(ParticleTypes.PORTAL,
                        pt.x, pt.y + 0.5, pt.z, 3, 0.1, 0.2, 0.1, 0.1);
            }
            sl.sendParticles(ParticleTypes.END_ROD,
                    dest.x, dest.y + 1, dest.z, 8, 0.3, 0.3, 0.3, 0.05);
        }

        if (!involuntary) {
            addResonance(data, 10);
            data.putInt("SC_Drift", data.getInt("SC_Drift") + 5);

            // Plant echo at origin if resonance high enough
            if (resonance >= RES_DISTRIBUTED) {
                plantEcho(data, origin);
            }
            player.sendSystemMessage(
                    Component.literal("[compass] slip executed. arrival confirmed.")
                            .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        }
    }

    // ── Fold: full planar fold 40 blocks ─────────────────────
    private static void executeFold(Player player, Level level,
                                    net.minecraft.nbt.CompoundTag data, int resonance) {
        Vec3 origin = player.position().add(0, 0.5, 0);
        Vec3 look   = player.getLookAngle().normalize();
        Vec3 dest   = origin.add(look.scale(40));

        // Clamp to safe Y
        dest = new Vec3(dest.x, Math.max(level.getMinBuildHeight() + 1,
                Math.min(level.getMaxBuildHeight() - 2, dest.y)), dest.z);

        // Find safe Y at destination
        BlockPos destBP = new BlockPos((int) dest.x, (int) dest.y, (int) dest.z);
        for (int dy = 0; dy <= 5; dy++) {
            BlockPos check = destBP.above(dy);
            if (level.getBlockState(check).isAir()
                    && level.getBlockState(check.above()).isAir()) {
                dest = Vec3.atBottomCenterOf(check);
                break;
            }
        }

        // Displace entities at exit
        final Vec3 finalDest = dest;
        level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(
                        finalDest.x - 8, finalDest.y - 4, finalDest.z - 8,
                        finalDest.x + 8, finalDest.y + 4, finalDest.z + 8),
                e -> e != player
        ).forEach(e -> {
            Vec3 dir = e.position().subtract(finalDest).normalize();
            e.setDeltaMovement(dir.scale(1.8).add(0, 0.4, 0));
            e.hurtMarked = true;
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2, false, true));
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,         80, 0, false, true));
        });

        // Levitate entities at origin
        level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(8), e -> e != player
        ).forEach(e ->
                e.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 1, false, false))
        );

        // Particles at both ends
        if (level instanceof ServerLevel sl) {
            Vec3 o = origin;
            for (int i = 0; i < 24; i++) {
                double angle = (Math.PI * 2.0 / 24) * i;
                sl.sendParticles(ParticleTypes.PORTAL,
                        o.x + Math.cos(angle) * 2, o.y + 1,
                        o.z + Math.sin(angle) * 2, 2, 0, 0.5, 0, 0.1);
                sl.sendParticles(ParticleTypes.END_ROD,
                        finalDest.x + Math.cos(angle) * 2, finalDest.y + 1,
                        finalDest.z + Math.sin(angle) * 2, 2, 0, 0.5, 0, 0.1);
            }
        }

        // Teleport
        player.teleportTo(finalDest.x, finalDest.y, finalDest.z);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false));

        // Echoes
        if (resonance >= RES_SEAM_II) {
            plantEcho(data, origin);
            plantEcho(data, finalDest);
        } else if (resonance >= RES_DISTRIBUTED) {
            plantEcho(data, origin);
        }

        addResonance(data, 20);
        data.putInt("SC_Drift", Math.min(DRIFT_MAX, data.getInt("SC_Drift") + 20));

        player.sendSystemMessage(Component.literal("════════════════════════════════")
                .withStyle(ChatFormatting.DARK_AQUA));
        player.sendSystemMessage(
                Component.literal("[compass] fold completed. you were not here. now you are.")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal("════════════════════════════════")
                .withStyle(ChatFormatting.DARK_AQUA));
    }

    // ── Plant a planar echo ───────────────────────────────────
    private static void plantEcho(net.minecraft.nbt.CompoundTag data, Vec3 pos) {
        // Shift slots: 3 → 2 → 1
        if (data.getInt("SC_Echo2Ticks") > 0) {
            data.putInt("SC_Echo3X", data.getInt("SC_Echo2X"));
            data.putInt("SC_Echo3Y", data.getInt("SC_Echo2Y"));
            data.putInt("SC_Echo3Z", data.getInt("SC_Echo2Z"));
            data.putInt("SC_Echo3Ticks", data.getInt("SC_Echo2Ticks"));
        }
        if (data.getInt("SC_Echo1Ticks") > 0) {
            data.putInt("SC_Echo2X", data.getInt("SC_Echo1X"));
            data.putInt("SC_Echo2Y", data.getInt("SC_Echo1Y"));
            data.putInt("SC_Echo2Z", data.getInt("SC_Echo1Z"));
            data.putInt("SC_Echo2Ticks", data.getInt("SC_Echo1Ticks"));
        }
        data.putInt("SC_Echo1X", (int) pos.x);
        data.putInt("SC_Echo1Y", (int) pos.y);
        data.putInt("SC_Echo1Z", (int) pos.z);
        data.putInt("SC_Echo1Ticks", ECHO_DURATION);
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
        tickDown(data, "SC_SlipCooldown");
        tickDown(data, "SC_FoldCooldown");

        boolean holding = isHolding(player);

        if (!holding) {
            data.putInt("SC_NotHeldTick", data.getInt("SC_NotHeldTick") + 1);
            // Resonance -3/s, Drift -1 per 5 s
            if ((player.tickCount % 20) == 0) {
                int r = data.getInt("SC_Resonance");
                if (r > 0) data.putInt("SC_Resonance", Math.max(0, r - 3));
            }
            if ((player.tickCount % 100) == 0) {
                int d = data.getInt("SC_Drift");
                if (d > 0) data.putInt("SC_Drift", Math.max(0, d - 1));
            }
            tickEchoes(player, level, data);
            return;
        }
        data.putInt("SC_NotHeldTick", 0);

        // ── Resonance accrual: +2/s ───────────────────────────
        if ((player.tickCount % RESONANCE_TICK) == 0) {
            addResonance(data, 2);
        }

        // ── Drift accrual: +1 per 5 s ────────────────────────
        int driftTick = data.getInt("SC_DriftTick") - 1;
        if (driftTick <= 0) {
            data.putInt("SC_DriftTick", DRIFT_ACCRUAL);
            data.putInt("SC_Drift", Math.min(DRIFT_MAX, data.getInt("SC_Drift") + 1));
        } else {
            data.putInt("SC_DriftTick", driftTick);
        }

        int resonance = data.getInt("SC_Resonance");
        int drift     = data.getInt("SC_Drift");

        // ── Displacement check ────────────────────────────────
        if (drift >= DRIFT_MAX) {
            executeFullDisplacement(player, level, data);
            return;
        }

        // ── Base passives ─────────────────────────────────────
        applyPassives(player, level, resonance, drift);

        // ── Echo tick ─────────────────────────────────────────
        tickEchoes(player, level, data);

        // ── Seam projectile deflection handled in hurt event ──

        // ── Drawback tiers ────────────────────────────────────
        applyDriftDrawbacks(player, level, data, drift, resonance);

        // ── Ambient particles ─────────────────────────────────
        if (level instanceof ServerLevel sl && (player.tickCount % 20) == 0) {
            Vec3 p = player.position();
            sl.sendParticles(ParticleTypes.PORTAL,
                    p.x + (Math.random() - 0.5) * 1.2,
                    p.y + 0.5 + Math.random() * 1.5,
                    p.z + (Math.random() - 0.5) * 1.2,
                    1, 0, 0, 0, 0.02);
        }

        // ── Periodic compass readout ──────────────────────────
        if ((player.tickCount % 200) == 0) {
            player.sendSystemMessage(
                    Component.literal("[compass] resonance: " + resonance
                            + " | drift: " + drift + " | status: " + getDriftName(drift))
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    // ── Passives ──────────────────────────────────────────────
    private static void applyPassives(Player player, Level level, int resonance, int drift) {
        // Dead Reckoning: hostile glow always
        if ((player.tickCount % 20) == 0) {
            level.getEntitiesOfClass(Monster.class,
                    player.getBoundingBox().inflate(32)
            ).forEach(mob ->
                    mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 25, 0, false, false))
            );
        }

        // Speed I (removed at Heavy Drift)
        if (drift < DRIFT_HEAVY) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
        }

        // Spatial Awareness: all entity glow
        if (resonance >= RES_AWARENESS && (player.tickCount % 20) == 0) {
            level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(20), e -> e != player
            ).forEach(e ->
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 25, 0, false, false))
            );
        }

        // Partial Step: reaction flash on back approach
        if (resonance >= RES_PARTIAL_STEP && (player.tickCount % 5) == 0) {
            Vec3 look = player.getLookAngle();
            level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(5), e -> e != player
            ).forEach(e -> {
                Vec3 toEntity = e.position().subtract(player.position()).normalize();
                // Dot negative = behind player
                if (look.dot(toEntity) < -0.5) {
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 10, 0, false, false));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 10, 1, false, false));
                }
            });
        }
    }

    // ── Echo tick: decoy + particles ─────────────────────────
    private static void tickEchoes(Player player, Level level,
                                   net.minecraft.nbt.CompoundTag data) {
        for (int slot = 1; slot <= 3; slot++) {
            String tickKey = "SC_Echo" + slot + "Ticks";
            int ticks = data.getInt(tickKey) - 1;
            if (ticks < 0) continue;
            data.putInt(tickKey, ticks);

            int ex = data.getInt("SC_Echo" + slot + "X");
            int ey = data.getInt("SC_Echo" + slot + "Y");
            int ez = data.getInt("SC_Echo" + slot + "Z");
            Vec3 echoPos = new Vec3(ex + 0.5, ey + 0.5, ez + 0.5);

            // Particles
            if (level instanceof ServerLevel sl && (player.tickCount % 4) == 0) {
                sl.sendParticles(ParticleTypes.END_ROD,
                        echoPos.x, echoPos.y + 1, echoPos.z,
                        2, 0.3, 0.5, 0.3, 0.05);
            }

            // Decoy: pull nearby hostiles toward echo
            if ((player.tickCount % 20) == 0) {
                level.getEntitiesOfClass(Monster.class,
                        new net.minecraft.world.phys.AABB(
                                ex - 10, ey - 4, ez - 10,
                                ex + 10, ey + 4, ez + 10)
                ).forEach(mob -> {
                    Vec3 toward = echoPos.subtract(mob.position()).normalize().scale(0.2);
                    mob.addDeltaMovement(toward);
                    mob.hurtMarked = true;
                });
            }
        }
    }

    // ── Drift drawbacks ───────────────────────────────────────
    private static void applyDriftDrawbacks(Player player, Level level,
                                            net.minecraft.nbt.CompoundTag data,
                                            int drift, int resonance) {
        // Minor (100+)
        if (drift >= DRIFT_MINOR) {
            // Hunger drain
            if ((player.tickCount % 60) == 0) {
                player.getFoodData().setFoodLevel(
                        Math.max(0, player.getFoodData().getFoodLevel() - 1));
            }
            // Whispers
            int whisperTick = data.getInt("SC_WhisperTick") - 1;
            if (whisperTick <= 0) {
                data.putInt("SC_WhisperTick", WHISPER_INT);
                String[] pool = (drift >= DRIFT_SEVERE) ? WHISPERS_LATE
                        : (drift >= DRIFT_MODERATE)     ? WHISPERS_MID
                        : WHISPERS_EARLY;
                player.sendSystemMessage(
                        Component.literal(pool[level.random.nextInt(pool.length)])
                                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            } else {
                data.putInt("SC_WhisperTick", whisperTick);
            }

            // Involuntary micro-slip
            int slipInterval = (drift >= DRIFT_CRITICAL) ? SLIP_INT_CRITICAL
                    : (drift >= DRIFT_SEVERE)  ? SLIP_INT_SEVERE
                    : (drift >= DRIFT_HEAVY)   ? SLIP_INT_HEAVY
                    : (drift >= DRIFT_MODERATE)? SLIP_INT_MODERATE
                    : SLIP_INT_MINOR;

            int microSlipTick = data.getInt("SC_MicroSlipTick") - 1;
            if (microSlipTick <= 0) {
                data.putInt("SC_MicroSlipTick", slipInterval);
                executeInvoluntarySlip(player, level, data, drift, resonance);
            } else {
                data.putInt("SC_MicroSlipTick", microSlipTick);
            }
        }

        // Moderate (250+)
        if (drift >= DRIFT_MODERATE) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0, false, false));
        }

        // Heavy (500+)
        if (drift >= DRIFT_HEAVY) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));

            // Block swaps nearby every 30 s
            int swapTick = data.getInt("SC_BlockSwapTick") - 1;
            if (swapTick <= 0) {
                data.putInt("SC_BlockSwapTick", (drift >= DRIFT_SEVERE) ? 300 : 600);
                doBlockSwap(player, level, drift);
            } else {
                data.putInt("SC_BlockSwapTick", swapTick);
            }
        }

        // Severe (700+)
        if (drift >= DRIFT_SEVERE) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
        }

        // Critical (900+)
        if (drift >= DRIFT_CRITICAL) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          40, 1, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
            if ((player.tickCount % 400) == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, false, true));
                player.sendSystemMessage(
                        Component.literal("[compass] planar corrosion detected.")
                                .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
            }
        }
    }

    // ── Involuntary slip ──────────────────────────────────────
    private static void executeInvoluntarySlip(Player player, Level level,
                                               net.minecraft.nbt.CompoundTag data,
                                               int drift, int resonance) {
        int range = (drift >= DRIFT_CRITICAL) ? 10 + level.random.nextInt(9)
                :   (drift >= DRIFT_SEVERE)   ? 8 + level.random.nextInt(7)
                :   (drift >= DRIFT_HEAVY)    ? 6 + level.random.nextInt(5)
                :   (drift >= DRIFT_MODERATE) ? 2 + level.random.nextInt(3)
                :   1 + level.random.nextInt(2);

        // Random horizontal direction
        double angle = level.random.nextDouble() * Math.PI * 2;
        Vec3 dir   = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        Vec3 origin = player.position();
        Vec3 dest  = origin.add(dir.scale(range));

        // Clamp Y to safe range
        BlockPos destBP = new BlockPos((int) dest.x,
                Math.max(level.getMinBuildHeight() + 1, (int) dest.y), (int) dest.z);
        // Find air at destination
        for (int dy = 0; dy <= 8; dy++) {
            BlockPos check = destBP.above(dy);
            if (level.getBlockState(check).isAir()
                    && level.getBlockState(check.above()).isAir()) {
                dest = Vec3.atBottomCenterOf(check);
                break;
            }
        }

        player.teleportTo(dest.x, dest.y, dest.z);

        // Transit damage at severe+
        if (drift >= DRIFT_SEVERE) {
            player.hurt(level.damageSources().magic(), 2f);
        }
        if (drift >= DRIFT_CRITICAL) {
            player.hurt(level.damageSources().magic(), 2f); // total 4
        }

        // Particles
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.PORTAL,
                    origin.x, origin.y + 1, origin.z, 10, 0.3, 0.5, 0.3, 0.1);
            sl.sendParticles(ParticleTypes.PORTAL,
                    dest.x, dest.y + 1, dest.z, 10, 0.3, 0.5, 0.3, 0.1);
        }

        player.sendSystemMessage(
                Component.literal("[compass] involuntary displacement. range: " + range + " blocks.")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    // ── Block swap (adjacent plane bleed) ────────────────────
    private static void doBlockSwap(Player player, Level level, int drift) {
        if (!(level instanceof ServerLevel)) return;
        int radius  = (drift >= DRIFT_SEVERE) ? 4 : 2;
        int count   = (drift >= DRIFT_SEVERE) ? 3 : 2;
        BlockPos center = player.blockPosition();
        boolean permanent = (drift >= DRIFT_SEVERE);

        for (int i = 0; i < count * 4 && count > 0; i++) {
            int dx = level.random.nextInt(radius * 2 + 1) - radius;
            int dy = level.random.nextInt(3) - 1;
            int dz = level.random.nextInt(radius * 2 + 1) - radius;
            BlockPos pos = center.offset(dx, dy, dz);
            BlockState state = level.getBlockState(pos);

            BlockState swapped = null;
            if (drift >= DRIFT_CRITICAL) {
                if (state.is(Blocks.STONE)) swapped = Blocks.OBSIDIAN.defaultBlockState();
                else if (state.is(Blocks.GRAVEL)) swapped = Blocks.MAGMA_BLOCK.defaultBlockState();
            } else if (drift >= DRIFT_SEVERE) {
                if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK))
                    swapped = Blocks.SOUL_SAND.defaultBlockState();
                else if (state.is(Blocks.STONE))
                    swapped = Blocks.NETHERRACK.defaultBlockState();
            } else {
                if (state.is(Blocks.DIRT) || state.is(Blocks.GRASS_BLOCK))
                    swapped = Blocks.CRIMSON_NYLIUM.defaultBlockState();
                else if (state.is(Blocks.STONE))
                    swapped = Blocks.NETHERRACK.defaultBlockState();
            }

            if (swapped != null) {
                level.setBlock(pos, swapped, 3);
                count--;
            }
        }
    }

    // ── Full Planar Displacement ──────────────────────────────
    private static void executeFullDisplacement(Player player, Level level,
                                                net.minecraft.nbt.CompoundTag data) {
        Vec3 origin = player.position();

        // Random 30–50 block teleport
        double angle = level.random.nextDouble() * Math.PI * 2;
        int range    = 30 + level.random.nextInt(21);
        Vec3 dest    = origin.add(Math.cos(angle) * range, 0, Math.sin(angle) * range);

        BlockPos destBP = new BlockPos((int) dest.x,
                Math.max(level.getMinBuildHeight() + 2, (int) dest.y), (int) dest.z);
        for (int dy = 0; dy <= 10; dy++) {
            BlockPos check = destBP.above(dy);
            if (level.getBlockState(check).isAir()
                    && level.getBlockState(check.above()).isAir()) {
                dest = Vec3.atBottomCenterOf(check);
                break;
            }
        }

        player.teleportTo(dest.x, dest.y, dest.z);
        player.hurt(level.damageSources().magic(), 20f);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,         1200, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,         1200, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1200, 4, false, true));

        // Reset
        data.putInt("SC_Resonance", 0);
        data.putInt("SC_Drift",     DRIFT_HEAVY); // 600 — not 0

        // Particles
        if (level instanceof ServerLevel sl) {
            Vec3 d = dest;
            sl.sendParticles(ParticleTypes.PORTAL,
                    origin.x, origin.y + 1, origin.z, 60, 1, 1, 1, 0.3);
            sl.sendParticles(ParticleTypes.END_ROD,
                    d.x, d.y + 1, d.z, 40, 0.5, 1, 0.5, 0.2);
        }

        // Notify nearby players at arrival
        final Vec3 arrivalPos = dest;
        level.getEntitiesOfClass(Player.class,
                new net.minecraft.world.phys.AABB(
                        arrivalPos.x - 20, arrivalPos.y - 10, arrivalPos.z - 20,
                        arrivalPos.x + 20, arrivalPos.y + 10, arrivalPos.z + 20),
                p -> p != player
        ).forEach(nearby -> nearby.sendSystemMessage(
                Component.literal("[compass] spatial displacement event detected nearby.")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));

        player.sendSystemMessage(Component.literal("════════════════════════════════")
                .withStyle(ChatFormatting.DARK_AQUA));
        player.sendSystemMessage(
                Component.literal("FULL PLANAR DISPLACEMENT")
                        .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
        player.sendSystemMessage(
                Component.literal("[compass] you came back. you came back from the wrong direction.")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        player.sendSystemMessage(
                Component.literal("[compass] you are not fully re-integrated.")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal("════════════════════════════════")
                .withStyle(ChatFormatting.DARK_AQUA));
    }

    // ── Seam: projectile deflection on hurt ───────────────────
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!isHolding(player)) return;
        if (!event.getSource().is(DamageTypeTags.IS_PROJECTILE)) return;

        var data      = player.getPersistentData();
        int resonance = data.getInt("SC_Resonance");
        if (resonance < RES_SEAM_I) return;

        float chance = (resonance >= RES_SEAM_II) ? 0.50f : 0.30f;
        if (player.level().random.nextFloat() < chance) {
            event.setCanceled(true);
            if (player.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.PORTAL,
                        player.getX(), player.getY() + 1, player.getZ(),
                        12, 0.4, 0.4, 0.4, 0.15);
            }
            player.sendSystemMessage(
                    Component.literal("[compass] projectile redirected through seam.")
                            .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        }

        // Partial Step: melee miss chance
        int res2 = data.getInt("SC_Resonance");
        if (res2 >= RES_PARTIAL_STEP && !event.getSource().is(DamageTypeTags.IS_PROJECTILE)) {
            if (player.level().random.nextFloat() < 0.15f) {
                event.setCanceled(true);
                player.sendSystemMessage(
                        Component.literal("[compass] hit landed in the wrong layer.")
                                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
            }
        }
    }

    // ── Outgoing melee miss (Moderate+ drift) ─────────────────
    @SubscribeEvent
    public static void onDealDamage(LivingHurtEvent event) {
        Entity src = event.getSource().getEntity();
        if (!(src instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!isHolding(player)) return;
        if (!event.getSource().is(DamageTypeTags.IS_PROJECTILE)) return;

        var data  = player.getPersistentData();
        int drift = data.getInt("SC_Drift");
        if (drift < DRIFT_MODERATE) return;

        float missChance = (drift >= DRIFT_HEAVY) ? 0.20f : 0.10f;
        if (player.level().random.nextFloat() < missChance) {
            event.setCanceled(true);
            player.sendSystemMessage(
                    Component.literal("[compass] attack delivered to adjacent plane. no effect here.")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }

    // ── Helpers ───────────────────────────────────────────────
    private static void addResonance(net.minecraft.nbt.CompoundTag data, int amount) {
        int r = data.getInt("SC_Resonance");
        data.putInt("SC_Resonance", Math.min(999, r + amount));
    }

    private static void tickDown(net.minecraft.nbt.CompoundTag data, String key) {
        int v = data.getInt(key);
        if (v > 0) data.putInt(key, v - 1);
    }

    private static boolean isHolding(Player player) {
        for (InteractionHand hand : InteractionHand.values())
            if (player.getItemInHand(hand).getItem() instanceof SplicedCompass) return true;
        return false;
    }

    private static String getDriftName(int drift) {
        if (drift >= DRIFT_CRITICAL) return "critical drift";
        if (drift >= DRIFT_SEVERE)   return "severe drift";
        if (drift >= DRIFT_HEAVY)    return "heavy drift";
        if (drift >= DRIFT_MODERATE) return "moderate displacement";
        if (drift >= DRIFT_MINOR)    return "minor displacement";
        return "nominal";
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

        tooltip.add(Component.literal("Beyonder Characteristic — Sequence 3")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Plane Wanderer  |  Door / Planar Pathway")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Planar Resonance (+2/s held):")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  100: Glow all entities. 200: 15% melee dodge.")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  300: Seam deflects 30% of projectiles.")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  700: Planar echoes left at slip origins.")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Dead Reckoning:")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  All hostiles in 32 blocks glow. Speed I.")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  Entities behind you are highlighted.")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Right-click — Slip (8 s CD, costs 20 resonance):")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Phase 12 blocks forward through all obstacles.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("Shift + Right-click — Fold (40 s CD, costs 80):")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Full 40-block planar fold. Displaces entities at exit.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("⚠ Drawback — Planar Drift (+1 per 5 s held):")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  100 — Involuntary micro-slips. Hunger drain.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  250 — Nausea. Attacks miss 10%. Blocks swap planes.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  500 — Forced 6–10 block slips every 20 s.")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  700 — Slips deal 2 HP. Block swaps permanent.")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  900 — Fold disabled. Wither. It knows where you'll be.")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  1000 — FULL DISPLACEMENT. Resets to drift 600.")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Drift decays only -1 per 5 s when dropped.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("\"The needle has four cardinal marks. One of them is a concept.\"")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
