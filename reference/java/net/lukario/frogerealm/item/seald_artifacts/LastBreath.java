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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════
 *   THE LAST BREATH  —  Sealed Artifact, Grade 0
 * ══════════════════════════════════════════════════════════════
 *
 * Inspired by Lord of the Mysteries (Frost / Stillness / Preservation).
 * A small glass vial containing a single frozen exhale, suspended
 * in crystalline ice that never melts. The final breath of a
 * Sequence 0 Time Auditor who chose to stop rather than continue.
 * They breathed out, and simply ceased.
 * The breath has been waiting for someone to breathe it in.
 *
 * Consumed on use. One item, one chance. No going back.
 *
 * ── PHASE 1 — Stillness  (ticks 1–200, 0–10 s) ───────────────
 *   • All mobs within 30 blocks: NoAI = true (frozen solid).
 *   • Player gains Resistance IV + Speed III.
 *   • SNOWFLAKE particles blanket the area.
 *
 * ── PHASE 2 — Preservation  (ticks 201–500, 10–25 s) ─────────
 *   • Player becomes invulnerable (all incoming damage cancelled).
 *   • Mobs remain frozen.
 *   • Slowness I creeps in — the cold spreads inward.
 *
 * ── PHASE 3 — The Cold Spreads  (ticks 501–900, 25–45 s) ─────
 *   • Invulnerability ends.
 *   • Mobs unfreeze but with permanent Slowness III + Weakness II.
 *   • Blocks in 10-block radius randomly convert → Packed Ice.
 *   • Player: Slowness II + frost particle aura.
 *
 * ── PHASE 4 — Stillness Turns Inward  (ticks 901–1400, 45–70 s)
 *   • Mining Fatigue III applied.
 *   • Attack speed near zero (Weakness III).
 *   • Random 3-second freeze intervals (NoAI equivalent via
 *     velocity zero + no-jump enforcement).
 *   • Frost particles intensify.
 *
 * ── GRADE 0 — THE FINAL EXHALE  (ticks 1401–1600, 70–80 s) ───
 *   • Player frozen solid: fully invulnerable, cannot move
 *     or act for 10 seconds.
 *   • On release: permanent "LB_Marked" tag written.
 *
 * ── PERMANENT MARK — The Breath Remembers ─────────────────────
 *   "LB_Marked" = true (forever, survives death and relog).
 *   Every 90 s: player freezes for 3 s without warning.
 *   ("LB_FreezeTicks" countdown, resets after each freeze.)
 *
 * ── STATE (player.getPersistentData()) ────────────────────────
 *   "LB_Active"       boolean — effect running
 *   "LB_Ticks"        int     — ticks elapsed since consumption
 *   "LB_Invuln"       boolean — currently invulnerable
 *   "LB_Frozen"       boolean — currently in a random freeze
 *   "LB_FrozenTicks"  int     — ticks remaining in random freeze
 *   "LB_FreezeTicks"  int     — ticks until next random freeze
 *   "LB_Marked"       boolean — permanent mark (never resets)
 *
 * ── REGISTRATION ──────────────────────────────────────────────
 *   public static final RegistryObject<Item> LAST_BREATH =
 *       ITEMS.register("last_breath",
 *           () -> new LastBreath(
 *               new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
 */
@Mod.EventBusSubscriber
public class LastBreath extends Item {

    // ── Phase boundaries (ticks) ──────────────────────────────
    private static final int PHASE_1_END     =  200;  // 10 s
    private static final int PHASE_2_END     =  500;  // 25 s
    private static final int PHASE_3_END     =  900;  // 45 s
    private static final int PHASE_4_END     = 1400;  // 70 s
    private static final int FINAL_EXHALE_END= 1600;  // 80 s

    // ── Permanent freeze constants ────────────────────────────
    private static final int PERMANENT_FREEZE_INTERVAL = 1800; // 90 s
    private static final int PERMANENT_FREEZE_DURATION =   60; // 3 s

    // ── World effect constants ────────────────────────────────
    private static final double FREEZE_RADIUS     = 30.0;
    private static final double UNFREEZE_RADIUS   = 40.0;
    private static final int    ICE_RADIUS        = 10;

    // ── Constructor ───────────────────────────────────────────

    public LastBreath(Properties properties) {
        super(properties);
    }

    // ── Use: consume and activate ─────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        var data = player.getPersistentData();
        if (data.getBoolean("LB_Active")) return InteractionResultHolder.pass(stack);

        // Initialise state
        data.putBoolean("LB_Active", true);
        data.putInt("LB_Ticks", 0);
        data.putBoolean("LB_Invuln", false);
        data.putBoolean("LB_Frozen", false);

        // Consume the item
        stack.shrink(1);

        player.sendSystemMessage(
                Component.literal("You breathe in.")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        player.sendSystemMessage(
                Component.literal("The world pauses to listen.")
                        .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));

        return InteractionResultHolder.success(stack);
    }

    // ── Server Tick ───────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        Level  level  = player.level();
        if (level.isClientSide) return;

        var data = player.getPersistentData();

        // ── Permanent freeze tick (runs even without active effect) ──
        if (data.getBoolean("LB_Marked")) {
            tickPermanentFreeze(player, level, data);
        }

        if (!data.getBoolean("LB_Active")) return;

        int ticks = data.getInt("LB_Ticks") + 1;
        data.putInt("LB_Ticks", ticks);

        // ── Route to correct phase ──
        if (ticks <= PHASE_1_END) {
            tickPhase1(player, level, ticks);
        } else if (ticks <= PHASE_2_END) {
            tickPhase2(player, level, ticks, data);
        } else if (ticks <= PHASE_3_END) {
            tickPhase3(player, level, ticks, data);
        } else if (ticks <= PHASE_4_END) {
            tickPhase4(player, level, ticks, data);
        } else if (ticks <= FINAL_EXHALE_END) {
            tickFinalExhale(player, level, ticks, data);
        } else {
            endEffect(player, data);
        }
    }

    // ── PHASE 1 — Stillness ───────────────────────────────────

    private static void tickPhase1(Player player, Level level, int ticks) {
        // Freeze all nearby mobs
        level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(FREEZE_RADIUS),
                e -> e != player
        ).forEach(e -> {
            if (e instanceof Mob mob) {
                mob.setNoAi(true);
            }
            e.setDeltaMovement(0, 0, 0);
        });

        // Player buffs
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 3, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,    40, 2, false, false));

        // Snowflake particles expanding outward
        if (level instanceof ServerLevel sl && (ticks % 5) == 0) {
            for (int i = 0; i < 8; i++) {
                double angle = (Math.PI * 2.0 / 8) * i + ticks * 0.05;
                double r     = (ticks / (double) PHASE_1_END) * FREEZE_RADIUS;
                sl.sendParticles(ParticleTypes.SNOWFLAKE,
                        player.getX() + Math.cos(angle) * r,
                        player.getY() + 1.0,
                        player.getZ() + Math.sin(angle) * r,
                        1, 0, 0.1, 0, 0.01);
            }
        }

        if (ticks == 1) {
            player.sendSystemMessage(
                    Component.literal("[Phase I] Stillness — the world holds.")
                            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        }
    }

    // ── PHASE 2 — Preservation ────────────────────────────────

    private static void tickPhase2(Player player, Level level, int ticks,
                                   net.minecraft.nbt.CompoundTag data) {
        // Keep mobs frozen
        level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(FREEZE_RADIUS),
                e -> e != player
        ).forEach(e -> {
            if (e instanceof Mob mob) {
                mob.setNoAi(true);
            }
            e.setDeltaMovement(0, 0, 0);
        });

        // Mark invulnerable (damage cancelled in hurt event)
        data.putBoolean("LB_Invuln", true);

        // Slowness creeping in
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));

        // Frost particles condensing on skin
        if (level instanceof ServerLevel sl && (ticks % 4) == 0) {
            sl.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                    player.getX() + (Math.random() - 0.5) * 0.8,
                    player.getY() + Math.random() * 2.0,
                    player.getZ() + (Math.random() - 0.5) * 0.8,
                    2, 0, 0.02, 0, 0.05);
        }

        if (ticks == PHASE_1_END + 1) {
            player.sendSystemMessage(
                    Component.literal("[Phase II] Preservation — nothing can touch you.")
                            .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
        }
    }

    // ── PHASE 3 — The Cold Spreads ────────────────────────────

    private static void tickPhase3(Player player, Level level, int ticks,
                                   net.minecraft.nbt.CompoundTag data) {
        // End invulnerability
        data.putBoolean("LB_Invuln", false);

        // Unfreeze mobs — but debuffed
        level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(UNFREEZE_RADIUS),
                e -> e != player
        ).forEach(e -> {
            if (e instanceof Mob mob) {
                mob.setNoAi(true);
            }
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, false));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          60, 1, false, false));
        });

        // Player slowness worsens
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));

        // Block erosion → packed ice
        if ((ticks % 40) == 0 && level instanceof ServerLevel) {
            tickIceErosion(player, level);
        }

        // Dense frost particles
        if (level instanceof ServerLevel sl && (ticks % 4) == 0) {
            sl.sendParticles(ParticleTypes.SNOWFLAKE,
                    player.getX() + (Math.random() - 0.5) * 1.2,
                    player.getY() + Math.random() * 2.2,
                    player.getZ() + (Math.random() - 0.5) * 1.2,
                    2, 0, 0.03, 0, 0.02);
        }

        if (ticks == PHASE_2_END + 1) {
            player.sendSystemMessage(
                    Component.literal("[Phase III] The cold spreads — inward now.")
                            .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
        }
    }

    // ── PHASE 4 — Stillness Turns Inward ─────────────────────

    private static void tickPhase4(Player player, Level level, int ticks,
                                   net.minecraft.nbt.CompoundTag data) {
        // Heavy debuffs
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,     40, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,         40, 2, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40, 2, false, false));

        // Random 3-second micro-freezes
        if (!data.getBoolean("LB_Frozen")) {
            // 5% chance each tick to trigger a freeze
            if (level.random.nextFloat() < 0.05f) {
                data.putBoolean("LB_Frozen", true);
                data.putInt("LB_FrozenTicks", 60); // 3 s
                player.sendSystemMessage(
                        Component.literal("The breath preserves you. You cannot move.")
                                .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
            }
        } else {
            // Apply freeze: zero velocity, no jumping
            player.setDeltaMovement(0, Math.min(0, player.getDeltaMovement().y), 0);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 127, false, false));

            int frozenTicks = data.getInt("LB_FrozenTicks") - 1;
            if (frozenTicks <= 0) {
                data.putBoolean("LB_Frozen", false);
                player.sendSystemMessage(
                        Component.literal("It releases you. Briefly.")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            } else {
                data.putInt("LB_FrozenTicks", frozenTicks);
            }
        }

        // Intensifying frost aura
        if (level instanceof ServerLevel sl && (ticks % 3) == 0) {
            for (int i = 0; i < 3; i++) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE,
                        player.getX() + (Math.random() - 0.5) * 2.0,
                        player.getY() + Math.random() * 2.5,
                        player.getZ() + (Math.random() - 0.5) * 2.0,
                        1, 0, 0.02, 0, 0.01);
            }
        }

        if (ticks == PHASE_3_END + 1) {
            player.sendSystemMessage(
                    Component.literal("[Phase IV] Stillness turns inward. It wants to keep you.")
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        }
    }

    // ── GRADE 0 — The Final Exhale ────────────────────────────

    private static void tickFinalExhale(Player player, Level level, int ticks,
                                        net.minecraft.nbt.CompoundTag data) {
        // Full freeze: invulnerable, cannot move
        data.putBoolean("LB_Invuln", true);
        player.setDeltaMovement(0, 0, 0);
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 127, false, false));

        // Crystal particles — the player is encased
        if (level instanceof ServerLevel sl && (ticks % 2) == 0) {
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2.0 / 6) * i;
                sl.sendParticles(ParticleTypes.SNOWFLAKE,
                        player.getX() + Math.cos(angle) * 0.7,
                        player.getY() + 0.5 + Math.random() * 1.5,
                        player.getZ() + Math.sin(angle) * 0.7,
                        1, 0, 0.02, 0, 0.01);
                sl.sendParticles(ParticleTypes.END_ROD,
                        player.getX() + Math.cos(angle) * 0.5,
                        player.getY() + 0.3 + Math.random() * 1.8,
                        player.getZ() + Math.sin(angle) * 0.5,
                        1, 0, 0.01, 0, 0.005);
            }
        }

        if (ticks == PHASE_4_END + 1) {
            player.sendSystemMessage(Component.literal("══════════════════════════════")
                    .withStyle(ChatFormatting.AQUA));
            player.sendSystemMessage(
                    Component.literal("THE FINAL EXHALE")
                            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            player.sendSystemMessage(
                    Component.literal("You are preserved. Perfectly. Completely.")
                            .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
            player.sendSystemMessage(
                    Component.literal("This is what it chose.")
                            .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
            player.sendSystemMessage(Component.literal("══════════════════════════════")
                    .withStyle(ChatFormatting.AQUA));
        }
    }

    // ── End effect ────────────────────────────────────────────

    private static void endEffect(Player player, net.minecraft.nbt.CompoundTag data) {
        data.putBoolean("LB_Active",  false);
        data.putBoolean("LB_Invuln",  false);
        data.putBoolean("LB_Frozen",  false);
        data.putInt("LB_Ticks", 0);

        // Apply permanent mark
        data.putBoolean("LB_Marked", true);
        data.putInt("LB_FreezeTicks", PERMANENT_FREEZE_INTERVAL);

        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        player.removeEffect(MobEffects.MOVEMENT_SPEED);

        player.sendSystemMessage(
                Component.literal("It releases you.")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        player.sendSystemMessage(
                Component.literal("But the breath remembers you now.")
                        .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        player.sendSystemMessage(
                Component.literal("It always will.")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    // ── Permanent freeze tick ─────────────────────────────────

    private static void tickPermanentFreeze(Player player, Level level,
                                            net.minecraft.nbt.CompoundTag data) {
        // Active freeze in progress
        if (data.getBoolean("LB_Frozen") && !data.getBoolean("LB_Active")) {
            player.setDeltaMovement(0, Math.min(0, player.getDeltaMovement().y), 0);
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 127, false, false));
            data.putBoolean("LB_Invuln", true);

            if (level instanceof ServerLevel sl && (player.tickCount % 4) == 0) {
                for (int i = 0; i < 4; i++) {
                    double angle = (Math.PI * 2.0 / 4) * i + player.tickCount * 0.1;
                    sl.sendParticles(ParticleTypes.SNOWFLAKE,
                            player.getX() + Math.cos(angle) * 0.6,
                            player.getY() + 0.5 + Math.random() * 1.5,
                            player.getZ() + Math.sin(angle) * 0.6,
                            1, 0, 0.01, 0, 0.01);
                }
            }

            int frozenTicks = data.getInt("LB_FrozenTicks") - 1;
            if (frozenTicks <= 0) {
                data.putBoolean("LB_Frozen", false);
                data.putBoolean("LB_Invuln", false);
                data.putInt("LB_FreezeTicks", PERMANENT_FREEZE_INTERVAL);
                player.sendSystemMessage(
                        Component.literal("The breath releases its grip. For 90 seconds.")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            } else {
                data.putInt("LB_FrozenTicks", frozenTicks);
            }
            return;
        }

        // Countdown to next freeze
        int freezeTicks = data.getInt("LB_FreezeTicks") - 1;
        if (freezeTicks <= 0) {
            // Trigger freeze
            data.putBoolean("LB_Frozen", true);
            data.putInt("LB_FrozenTicks", PERMANENT_FREEZE_DURATION);

            player.sendSystemMessage(
                    Component.literal("The breath remembers.")
                            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
            player.sendSystemMessage(
                    Component.literal("You are still. You are preserved.")
                            .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        } else {
            data.putInt("LB_FreezeTicks", freezeTicks);
        }
    }

    // ── Block ice erosion ─────────────────────────────────────

    private static void tickIceErosion(Player player, Level level) {
        if (!(level instanceof ServerLevel)) return;
        BlockPos center = player.blockPosition();

        for (int attempt = 0; attempt < 4; attempt++) {
            int dx = level.random.nextInt(ICE_RADIUS * 2) - ICE_RADIUS;
            int dy = level.random.nextInt(4) - 2;
            int dz = level.random.nextInt(ICE_RADIUS * 2) - ICE_RADIUS;
            BlockPos pos = center.offset(dx, dy, dz);

            var state = level.getBlockState(pos);
            if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)
                    || state.is(Blocks.STONE) || state.is(Blocks.WATER)) {
                level.setBlock(pos, Blocks.PACKED_ICE.defaultBlockState(), 3);
            } else if (state.is(Blocks.PACKED_ICE) && level.random.nextFloat() < 0.1f) {
                level.setBlock(pos, Blocks.BLUE_ICE.defaultBlockState(), 3);
            }
        }
    }

    // ── Damage cancellation (invulnerable phases) ─────────────

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.getPersistentData().getBoolean("LB_Invuln")) {
            event.setCanceled(true);
        }
    }

    // ── Utility ───────────────────────────────────────────────

    private static ItemStack findBreath(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof LastBreath) return stack;
        }
        return null;
    }

    // ── Tooltip ───────────────────────────────────────────────

    @Override
    public boolean isFoil(ItemStack stack){
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.literal("Sealed Artifact — Grade 0")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Frost / Stillness / Preservation Pathway")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Consumed on use. No going back.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Phase I — Stillness (10 s):")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  All mobs within 30 blocks frozen solid")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  Resistance IV + Speed III")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Phase II — Preservation (15 s):")
                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Complete damage immunity")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  Slowness I begins")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Phase III — The Cold Spreads (20 s):")
                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Mobs unfreeze — Slowness III + Weakness II")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  Blocks convert to Packed Ice nearby")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Phase IV — Stillness Turns Inward (25 s):")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Mining Fatigue III + near-zero attack speed")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  Random 3-second freezes — cannot move or act")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("⚠ Grade 0 — The Final Exhale (10 s):")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Frozen solid. Invulnerable. Completely still.")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("⚠ Permanent Mark — The Breath Remembers:")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Every 90 s: frozen for 3 s without warning")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  Forever.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("\"It chose to stop. Now it remembers you chose differently.\"")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
