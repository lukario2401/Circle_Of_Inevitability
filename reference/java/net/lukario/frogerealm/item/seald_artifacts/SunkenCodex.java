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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════
 *   THE SUNKEN CODEX  —  Sealed Artifact, Grade 0
 * ══════════════════════════════════════════════════════════════
 *
 * Inspired by Lord of the Mysteries (Abyss / Depth / Pressure).
 * A waterlogged book whose pages are made from the flattened skin
 * of deep-sea creatures that never had names. The text rewrites
 * itself every time you look away. Found at the bottom of a trench
 * deeper than any map acknowledges, sealed inside the ribcage of
 * something the size of a mountain.
 * The seal broke when it reached the surface.
 * The pressure followed it up.
 *
 * ── PASSIVE — Depth Counter ───────────────────────────────────
 *   Every second held, gains Depth Points based on Y position:
 *     Below Y=0:   +3 points/s (deep underground)
 *     Y=0–32:      +2 points/s (underground)
 *     Y=33–62:     +1 point/s  (near sea level)
 *     Above Y=62:  +0 points/s (surface, no gain)
 *   Underwater multiplies points ×2.
 *   Total "Fathom Score" never resets (stored in persistentData).
 *
 *   Fathom tiers unlock passives:
 *     500+   → Pressure Field I:  nearby entities Slowed
 *     2000+  → Pressure Field II: entities repelled (knockback)
 *     5000+  → Pressure Field III: repulsion + crushing damage
 *     5000+  → Block Erosion: random nearby stone→gravel, gravel→sand
 *     10000+ → THE TRENCH OPENS (Grade 0)
 *
 * ── ACTIVE (right-click) — Abyss Pulse ───────────────────────
 *   Shockwave in 20-block radius:
 *     • Massive knockback away from player
 *     • Targets hit the ground hard (velocity forced downward)
 *     • Fall damage enabled for 3 s (removes Slow Falling)
 *   Cost: 5 hearts (10 HP). Min 1 HP. 45 s cooldown.
 *
 * ── GRADE 0 — THE TRENCH OPENS (Fathom ≥ 10000) ─────────────
 *   Permanent "Abyssal Zone" forms around the player:
 *     • 5-block radius: random blocks flicker to Deepslate
 *     • Water sources spawn spontaneously in air pockets nearby
 *     • All entities in zone: 1 damage/s per second inside
 *       (escalates: 2 damage at 3 s, 4 damage at 6 s, etc.)
 *     • Player: permanent Mining Fatigue III + Slowness II
 *     • Player moves as though underwater (constant effect)
 *     • Codex whispers: fragmented obfuscated chat messages
 *     • Server-wide — other players near you see the zone too
 *
 * ── STATE (player.getPersistentData()) ────────────────────────
 *   "SC_FathomScore"   long    — total accumulated depth points
 *   "SC_TrenchOpen"    boolean — Grade 0 state active
 *   "SC_WhisperTicks"  int     — ticks until next whisper
 *   "SC_ErosionTicks"  int     — ticks until next block erosion
 *   "SC_ZoneTicks"     int     — ticks until next zone damage tick
 *
 * ── REGISTRATION ──────────────────────────────────────────────
 *   public static final RegistryObject<Item> SUNKEN_CODEX =
 *       ITEMS.register("sunken_codex",
 *           () -> new SunkenCodex(
 *               new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
 */
@Mod.EventBusSubscriber
public class SunkenCodex extends Item {

    // ── Fathom thresholds ─────────────────────────────────────
    private static final long FATHOM_FIELD_I    =   500L;
    private static final long FATHOM_FIELD_II   =  2000L;
    private static final long FATHOM_FIELD_III  =  5000L;
    private static final long FATHOM_TRENCH     = 10000L;

    // ── Tuning ────────────────────────────────────────────────
    private static final int   ACTIVE_COOLDOWN   = 900;   // 45 s
    private static final float ACTIVE_HP_COST    = 10f;   // 5 hearts
    private static final double PULSE_RADIUS     = 20.0;
    private static final double FIELD_RADIUS     = 12.0;
    private static final int   ZONE_RADIUS       = 5;     // blocks
    private static final int   EROSION_INTERVAL  = 100;   // 5 s
    private static final int   WHISPER_INTERVAL  = 300;   // 15 s

    // ── Codex whispers — fragmented, rewriting ────────────────
    private static final String[][] WHISPER_FRAGMENTS = {
            {"something ", "it ", "there is "},
            {"below ", "beneath ", "under "},
            {"you ", "here ", "the surface "},
            {"is watching ", "remembers ", "was always "},
            {"the depth", "the weight", "the dark"},
    };

    // ── Constructor ───────────────────────────────────────────

    public SunkenCodex(Properties properties) {
        super(properties);
    }

    // ── Active: Abyss Pulse ───────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);

        // HP cost
        player.setHealth(Math.max(1f, player.getHealth() - ACTIVE_HP_COST));

        Vec3 playerPos = player.position();

        // Knockback all entities in radius
        level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(PULSE_RADIUS),
                e -> e != player
        ).forEach(e -> {
            Vec3 dir = e.position().subtract(playerPos).normalize();
            // Strong outward knockback + downward slam
            e.setDeltaMovement(
                    dir.x * 2.5,
                    -1.8,          // forced downward — pressure slams them into ground
                    dir.z * 2.5
            );
            e.hurtMarked = true;

            // Remove slow falling so fall damage applies
            e.removeEffect(MobEffects.SLOW_FALLING);

            // Slowness II briefly — pressure aftermath
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, true));
        });

        // Shockwave particles
        if (level instanceof ServerLevel sl) {
            for (int i = 0; i < 36; i++) {
                double angle = (Math.PI * 2.0 / 36) * i;
                sl.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,
                        playerPos.x + Math.cos(angle) * 3,
                        playerPos.y + 0.5,
                        playerPos.z + Math.sin(angle) * 3,
                        1, 0, 0.2, 0, 0.1);
            }
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    playerPos.x, playerPos.y + 1, playerPos.z, 1, 0, 0, 0, 0);
        }

        player.displayClientMessage(
                Component.literal("The weight of the deep is released.")
                        .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC), true);

        player.getCooldowns().addCooldown(this, ACTIVE_COOLDOWN);
        return InteractionResultHolder.success(stack);
    }

    // ── Server Tick ───────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        Level  level  = player.level();
        if (level.isClientSide) return;
        if (findCodex(player) == null) return;

        var data = player.getPersistentData();

        // ── Depth Point accrual (once/s) ──
        if ((player.tickCount % 20) == 0) {
            long fathom = data.getLong("SC_FathomScore");
            int  gain   = depthGain(player);
            fathom += gain;
            data.putLong("SC_FathomScore", fathom);

            // Milestone notifications
            notifyMilestone(player, fathom, gain);

            // Trigger Trench
            if (fathom >= FATHOM_TRENCH && !data.getBoolean("SC_TrenchOpen")) {
                triggerTrenchOpens(player);
            }
        }


        long fathom = data.getLong("SC_FathomScore");

        // ── Pressure Field ──
        if ((player.tickCount % 20) == 0 && fathom >= FATHOM_FIELD_I) {
            tickPressureField(player, level, fathom);
        }

        // ── Block Erosion ──
        if (fathom >= FATHOM_FIELD_III) {
            int erosionTicks = data.getInt("SC_ErosionTicks") - 1;
            if (erosionTicks <= 0) {
                data.putInt("SC_ErosionTicks", EROSION_INTERVAL);
                tickBlockErosion(player, level);
            } else {
                data.putInt("SC_ErosionTicks", erosionTicks);
            }
        }

        // ── Depth particles (always) ──
        if (level instanceof ServerLevel sl && (player.tickCount % 15) == 0) {
            sl.sendParticles(ParticleTypes.DRIPPING_WATER,
                    player.getX() + (Math.random() - 0.5) * 1.5,
                    player.getY() + Math.random() * 2.2,
                    player.getZ() + (Math.random() - 0.5) * 1.5,
                    1, 0, 0, 0, 0);
        }

        // ── Grade 0: Trench Open state ──
        if (data.getBoolean("SC_TrenchOpen")) {
            tickTrenchState(player, level, data);
        }
        if (player.tickCount%100==0){
            player.sendSystemMessage(Component.literal(String.valueOf(data.getLong("SC_FathomScore"))));
            player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 120, 0, false, false));
        }
    }

    // ── Depth gain calculation ────────────────────────────────

    private static int depthGain(Player player) {
        double y      = player.getY();
        int    base   = y < 0 ? 3 : y < 32 ? 2 : y < 62 ? 1 : 0;
        boolean inWater = player.isUnderWater();
        return inWater ? base * 2 : base;
    }

    // ── Milestone messages ────────────────────────────────────

    private static void notifyMilestone(Player player, long fathom, int gain) {
        if (gain == 0) return;
        long[] milestones = {FATHOM_FIELD_I, FATHOM_FIELD_II, FATHOM_FIELD_III, FATHOM_TRENCH};
        String[] labels   = {
                "Pressure Field I — the Codex exerts itself.",
                "Pressure Field II — entities are repelled.",
                "Pressure Field III — crushing force.",
                "The Trench Opens."
        };
        for (int i = 0; i < milestones.length; i++) {
            long m = milestones[i];
            if (fathom >= m && fathom - gain < m) {
                player.sendSystemMessage(
                        Component.literal("[Sunken Codex] ")
                                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD)
                                .append(Component.literal(labels[i])
                                        .withStyle(ChatFormatting.AQUA)));
            }
        }
    }

    // ── Pressure Field ────────────────────────────────────────

    private static void tickPressureField(Player player, Level level, long fathom) {
        Vec3 playerPos = player.position();

        level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(FIELD_RADIUS),
                e -> e != player
        ).forEach(e -> {
            Vec3  dir  = e.position().subtract(playerPos);
            double dist = dir.length();
            if (dist < 0.5) return;
            Vec3 norm = dir.normalize();

            // Field I: Slowness
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));

            // Field II: repulsion knockback
            if (fathom >= FATHOM_FIELD_II) {
                double force = 0.12 * (1.0 - dist / FIELD_RADIUS); // stronger when closer
                e.addDeltaMovement(norm.scale(force));
                e.hurtMarked = true;
            }

            // Field III: crushing damage
            if (fathom >= FATHOM_FIELD_III) {
                e.hurt(level.damageSources().magic(), 1.5f);
            }
        });

        // Pressure bubble particles
        if (level instanceof ServerLevel sl && (player.tickCount % 10) == 0) {
            double angle = (player.tickCount * 0.2) % (Math.PI * 2);
            for (int i = 0; i < 3; i++) {
                double a = angle + (Math.PI * 2 / 3) * i;
                sl.sendParticles(ParticleTypes.BUBBLE,
                        playerPos.x + Math.cos(a) * (FIELD_RADIUS * 0.5),
                        playerPos.y + 1,
                        playerPos.z + Math.sin(a) * (FIELD_RADIUS * 0.5),
                        1, 0, 0, 0, 0.02);
            }
        }
    }

    // ── Block Erosion ─────────────────────────────────────────

    private static void tickBlockErosion(Player player, Level level) {
        if (!(level instanceof ServerLevel sl)) return;

        BlockPos center = player.blockPosition();
        int radius = 8;

        // Pick a few random blocks nearby and erode them
        for (int attempt = 0; attempt < 5; attempt++) {
            int dx = level.random.nextInt(radius * 2) - radius;
            int dy = level.random.nextInt(4) - 2;
            int dz = level.random.nextInt(radius * 2) - radius;

            BlockPos pos   = center.offset(dx, dy, dz);
            BlockState state = level.getBlockState(pos);

            if (state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE)
                    || state.is(Blocks.STONE_BRICKS)) {
                level.setBlock(pos, Blocks.GRAVEL.defaultBlockState(), 3);
            } else if (state.is(Blocks.GRAVEL)) {
                level.setBlock(pos, Blocks.SAND.defaultBlockState(), 3);
            }
        }

        // Drip particles at erosion site
        sl.sendParticles(ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
                center.getX() + 0.5,
                center.getY() + 1.5,
                center.getZ() + 0.5,
                3, 1, 0.5, 1, 0.1);
    }

    // ── Trench Opens trigger ──────────────────────────────────

    private static void triggerTrenchOpens(Player player) {
        player.getPersistentData().putBoolean("SC_TrenchOpen", true);
        player.getPersistentData().putInt("SC_WhisperTicks",   WHISPER_INTERVAL);
        player.getPersistentData().putInt("SC_ZoneTicks",      20);

        player.sendSystemMessage(Component.literal("════════════════════════════════").withStyle(ChatFormatting.DARK_AQUA));
        player.sendSystemMessage(
                Component.literal("THE TRENCH OPENS")
                        .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
        player.sendSystemMessage(
                Component.literal("The surface was always an illusion.")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        player.sendSystemMessage(
                Component.literal("You have been below this whole time.")
                        .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal("════════════════════════════════").withStyle(ChatFormatting.DARK_AQUA));
    }

    // ── Trench state tick ─────────────────────────────────────

    private static void tickTrenchState(Player player, Level level,
                                        net.minecraft.nbt.CompoundTag data) {
        // Permanent debuffs
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,     40, 2, false, false)); // Mining Fatigue III
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40, 1, false, false)); // Slowness II
        player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER,    40, 0, false, false)); // underwater breathing at least

        // Abyssal Zone: block flickering + water spawning
        if ((player.tickCount % 40) == 0 && level instanceof ServerLevel sl) {
            tickAbyssalZone(player, sl);
        }

        // Zone damage: entities in range accumulate pressure
        int zoneTicks = data.getInt("SC_ZoneTicks") - 1;
        if (zoneTicks <= 0) {
            data.putInt("SC_ZoneTicks", 20); // check every second
            applyZoneDamage(player, level);
        } else {
            data.putInt("SC_ZoneTicks", zoneTicks);
        }

        // Whispers
        int whisperTicks = data.getInt("SC_WhisperTicks") - 1;
        if (whisperTicks <= 0) {
            data.putInt("SC_WhisperTicks", WHISPER_INTERVAL);
            sendWhisper(player, level);
        } else {
            data.putInt("SC_WhisperTicks", whisperTicks);
        }

        // Dense particle aura
        if (level instanceof ServerLevel sl && (player.tickCount % 4) == 0) {
            sl.sendParticles(ParticleTypes.DRIPPING_WATER,
                    player.getX() + (Math.random() - 0.5) * ZONE_RADIUS,
                    player.getY() + Math.random() * 3,
                    player.getZ() + (Math.random() - 0.5) * ZONE_RADIUS,
                    2, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP,
                    player.getX() + (Math.random() - 0.5) * 2,
                    player.getY(),
                    player.getZ() + (Math.random() - 0.5) * 2,
                    1, 0, 0.1, 0, 0.05);
        }
    }

    // ── Abyssal Zone: block flickering ────────────────────────

    private static void tickAbyssalZone(Player player, ServerLevel sl) {
        BlockPos center = player.blockPosition();

        for (int attempt = 0; attempt < 3; attempt++) {
            int dx = sl.random.nextInt(ZONE_RADIUS * 2 + 1) - ZONE_RADIUS;
            int dy = sl.random.nextInt(3) - 1;
            int dz = sl.random.nextInt(ZONE_RADIUS * 2 + 1) - ZONE_RADIUS;
            BlockPos pos = center.offset(dx, dy, dz);

            BlockState state = sl.getBlockState(pos);

            // Flicker solid blocks to deepslate
            if (state.is(Blocks.STONE) || state.is(Blocks.GRASS_BLOCK)
                    || state.is(Blocks.DIRT) || state.is(Blocks.SAND)) {
                sl.setBlock(pos, Blocks.DEEPSLATE.defaultBlockState(), 3);
            }

            // Spontaneous water in air pockets
            BlockPos above = pos.above();
            if (sl.getBlockState(pos).is(Blocks.DEEPSLATE)
                    && sl.getBlockState(above).isAir()
                    && sl.random.nextFloat() < 0.05f) {
                sl.setBlock(above, Blocks.WATER.defaultBlockState(), 3);
            }
        }
    }

    // ── Zone damage: escalating pressure ─────────────────────

    private static void applyZoneDamage(Player player, Level level) {
        level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(ZONE_RADIUS),
                e -> e != player
        ).forEach(e -> {
            // Track how long they've been in zone
            int timeInZone = e.getPersistentData().getInt("SC_ZoneTime") + 1;
            e.getPersistentData().putInt("SC_ZoneTime", timeInZone);

            // Escalating damage: 1, 2, 4, 8... (doubles every 3 s)
            float dmg = (float) Math.pow(2, timeInZone / 3);
            e.hurt(level.damageSources().magic(), Math.min(dmg, 16f)); // cap at 8 hearts
        });

        // Decay zone time for entities that left
        level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(ZONE_RADIUS * 3),
                e -> e != player && !player.getBoundingBox().inflate(ZONE_RADIUS)
                        .intersects(e.getBoundingBox())
        ).forEach(e -> {
            int t = e.getPersistentData().getInt("SC_ZoneTime");
            if (t > 0) e.getPersistentData().putInt("SC_ZoneTime", t - 1);
        });
    }

    // ── Codex whispers ────────────────────────────────────────

    private static void sendWhisper(Player player, Level level) {
        // Build a fragmented sentence from random picks
        StringBuilder whisper = new StringBuilder();
        for (String[] group : WHISPER_FRAGMENTS) {
            whisper.append(group[level.random.nextInt(group.length)]);
        }

        // Mix plain and obfuscated text for eerie effect
        String full = whisper.toString();
        int splitAt = full.length() / 2;

        player.sendSystemMessage(
                Component.literal(full.substring(0, splitAt))
                        .withStyle(ChatFormatting.DARK_AQUA)
                        .append(Component.literal(full.substring(splitAt))
                                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.OBFUSCATED))
        );
    }

    // ── Utility ───────────────────────────────────────────────

    private static ItemStack findCodex(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof SunkenCodex) return stack;
        }
        return null;
    }

    // ── Tooltip ───────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.literal("Sealed Artifact — Grade 0")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Abyss / Depth / Pressure Pathway")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Depth Counter:")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Gains Fathom Score based on depth held")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  Deeper = more points. Underwater = doubled.")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Fathom Tiers:")
                .withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  500  — Pressure Field I: Slowness aura")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  2000 — Pressure Field II: Entity repulsion")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  5000 — Pressure Field III: Crushing damage + Block erosion")
                .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Right-click — Abyss Pulse:")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Cost: 5 hearts. Shockwave in 20 blocks.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  Massive knockback + ground slam. 45 s cooldown.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("⚠ Grade 0 — The Trench Opens (10,000 Fathom):")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  5-block Abyssal Zone: blocks erode to deepslate")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  Water spawns spontaneously nearby")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  Entities in zone: escalating pressure damage")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  Permanent Mining Fatigue III + Slowness II")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  The Codex whispers. It rewrites itself.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("\"The text changes every time you look away.\"")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
