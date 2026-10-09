package net.lukario.frogerealm.item.seald_artifacts;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ══════════════════════════════════════════════════════════════
 *   THE UNFINISHED HOURGLASS  —  Sealed Artifact, Grade 0
 * ══════════════════════════════════════════════════════════════
 *
 * Inspired by Lord of the Mysteries (Fate / Time pathway).
 * The sand inside is not sand — it is compressed unlived time,
 * harvested from entities that died before their fated moment.
 * It runs backwards. It has always been running backwards.
 *
 * ── PASSIVE — Debt Ledger ─────────────────────────────────────
 *   Every second while held: records a snapshot of the player's
 *   position and HP into a rolling 60-entry history buffer.
 *   Subtle END_ROD particles trail behind the player.
 *
 * ── ACTIVE (right-click) — Rewind ────────────────────────────
 *   Replays the snapshot history in reverse:
 *   • Teleports the player backward through recorded positions.
 *   • Restores HP to the value recorded at each step.
 *   • Each second rewound → 1 "Fate-Debt" charge stored.
 *   • Cooldown = seconds rewound × 20 ticks.
 *   • Cannot activate during an active rewind.
 *
 * ── FATE'S CORRECTION ─────────────────────────────────────────
 *   After rewind completes, the universe corrects:
 *   • For each Fate-Debt charge: deals 1.5 damage after a delay.
 *   • Damage arrives in waves — faster and harder the more
 *     time was rewound (more charges = shorter intervals).
 *   • Cannot be blocked, absorbed, or reduced.
 *     (Delivered via MagicDamage, bypasses armor.)
 *
 * ── GRADE 0: TIMELINE COLLAPSE ────────────────────────────────
 *   If rewind exceeds 45 seconds (45 entries):
 *   • A GHOST is spawned at the player's pre-rewind position
 *     (a Wither Skeleton tagged GH_Ghost with Invisibility +
 *      Glowing — visible but untargetable from afar).
 *   • Lasts 10 seconds. If ANY entity touches the ghost AABB:
 *     → Explosion centered on the PLAYER (not the ghost).
 *     → Ghost despawns instantly.
 *   • Hourglass gains one CRACK (NBT "UH_Cracks", max 3).
 *   • At 3 cracks: item transforms into Dead Hourglass.
 *     Dead Hourglass applies Wither IV to anyone holding it
 *     and cannot be dropped (it re-inserts itself each tick).
 *
 * ── STATE ─────────────────────────────────────────────────────
 *   Runtime (in-memory, per session):
 *     snapshotBuffers  — Map<UUID, Deque<Snapshot>>
 *     rewindState      — Map<UUID, RewindState>
 *     debtState        — Map<UUID, DebtState>
 *     ghostState       — Map<UUID, GhostState>
 *
 *   Persistent (ItemStack tag):
 *     "UH_Cracks"  int  — 0–3; at 3 becomes Dead Hourglass
 *
 * ── REGISTRATION ──────────────────────────────────────────────
 *   public static final RegistryObject<Item> UNFINISHED_HOURGLASS =
 *       ITEMS.register("unfinished_hourglass",
 *           () -> new UnfinishedHourglass(
 *               new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
 *
 *   public static final RegistryObject<Item> DEAD_HOURGLASS =
 *       ITEMS.register("dead_hourglass",
 *           () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
 *   (Dead Hourglass behaviour is handled inside this class's tick.)
 */
@Mod.EventBusSubscriber
public class UnfinishedHourglass extends Item {

    // ── Tuning ────────────────────────────────────────────────

    private static final int   MAX_SNAPSHOTS        = 60;   // 60 s history
    private static final int   SNAPSHOT_INTERVAL    = 20;   // 1 snapshot/s
    private static final int   COLLAPSE_THRESHOLD   = 45;   // entries for Grade 0
    private static final int   MAX_CRACKS           = 3;
    private static final float DEBT_DAMAGE_PER_STEP = 5f; // per charge
    private static final int   GHOST_DURATION       = 200;  // 10 s in ticks
    private static final double GHOST_TRIGGER_RADIUS = 1.5;

    // ── Runtime state (session-only, not persisted) ───────────

    /** Rolling snapshot ring per player UUID. */
    private static final Map<UUID, Deque<Snapshot>> snapshotBuffers = new HashMap<>();

    /** Active rewind state per player. */
    private static final Map<UUID, RewindState> rewindStates = new HashMap<>();

    /** Pending Fate-Debt correction per player. */
    private static final Map<UUID, DebtState> debtStates = new HashMap<>();

    /** Active ghost state per player. */
    private static final Map<UUID, GhostState> ghostStates = new HashMap<>();

    // ── Data records ──────────────────────────────────────────

    private record Snapshot(Vec3 pos, float hp) {}

    private static class RewindState {
        Deque<Snapshot> remaining;
        int             tickTimer = 0; // ticks until next step
        int             totalSteps;

        RewindState(Deque<Snapshot> history) {
            // Copy into a new deque to replay in reverse (history is already newest-first)
            this.remaining  = new ArrayDeque<>(history);
            this.totalSteps = history.size();
        }
    }

    private static class DebtState {
        int   charges;         // how many correction hits remain
        int   tickTimer;       // ticks until next hit
        int   intervalTicks;   // ticks between hits (shorter = more rewind)

        DebtState(int charges) {
            this.charges       = charges;
            this.intervalTicks = Math.max(10, 60 - charges); // faster corrections for longer rewinds
            this.tickTimer     = intervalTicks;
        }
    }

    private static class GhostState {
        Vec3 position;
        int  ticksRemaining;
        net.minecraft.world.entity.monster.WitherSkeleton ghost;

        GhostState(Vec3 pos, net.minecraft.world.entity.monster.WitherSkeleton ghost) {
            this.position       = pos;
            this.ticksRemaining = GHOST_DURATION;
            this.ghost          = ghost;
        }
    }

    // ── Constructor ───────────────────────────────────────────

    public UnfinishedHourglass(Properties properties) {
        super(properties);
    }

    // ── Active: Rewind ────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        UUID uuid = player.getUUID();

        // Block if already rewinding
        if (rewindStates.containsKey(uuid)) return InteractionResultHolder.pass(stack);
        if (player.getCooldowns().isOnCooldown(this))  return InteractionResultHolder.pass(stack);

        Deque<Snapshot> buffer = snapshotBuffers.getOrDefault(uuid, new ArrayDeque<>());
        if (buffer.isEmpty()) {
            player.displayClientMessage(
                    Component.literal("The sand has not yet settled.")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResultHolder.pass(stack);
        }

        int entries = buffer.size();

        // ── Timeline Collapse check ──
        if (entries >= COLLAPSE_THRESHOLD) {
            triggerTimelineCollapse(player, level, stack);
        }

        // Begin rewind
        rewindStates.put(uuid, new RewindState(new ArrayDeque<>(buffer)));
        snapshotBuffers.put(uuid, new ArrayDeque<>()); // clear buffer during rewind

        player.displayClientMessage(
                Component.literal("The sand flows upward.")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC), true);

        return InteractionResultHolder.success(stack);
    }

    // ── Server Tick ───────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        Level  level  = player.level();
        if (level.isClientSide) return;

        UUID uuid = player.getUUID();

        // ── Dead Hourglass tick (curse) ──
        tickDeadHourglass(player, level, uuid);

        // ── Only proceed with Hourglass logic if holding it ──
        ItemStack held = findHourglass(player);

        // ── Ghost tick (runs regardless of holding) ──
        tickGhost(player, level, uuid);

        // ── Debt correction tick ──
        tickDebtCorrection(player, level, uuid);

        // ── Active rewind tick ──
        if (rewindStates.containsKey(uuid)) {
            tickRewind(player, level, uuid, held);
            return; // no snapshot recording during rewind
        }

        if (held == null) return;

        // ── Snapshot recording (every 20 ticks = 1 s) ──
        if ((player.tickCount % SNAPSHOT_INTERVAL) == 0) {
            Deque<Snapshot> buffer = snapshotBuffers.computeIfAbsent(uuid, k -> new ArrayDeque<>());
            buffer.addFirst(new Snapshot(player.position(), player.getHealth()));
            if (buffer.size() > MAX_SNAPSHOTS) buffer.removeLast();
        }

        // ── Trailing time-particles ──
        if (level instanceof ServerLevel sl && (player.tickCount % 8) == 0) {
            sl.sendParticles(ParticleTypes.END_ROD,
                    player.getX() + (Math.random() - 0.5) * 0.3,
                    player.getY() + Math.random() * 1.8,
                    player.getZ() + (Math.random() - 0.5) * 0.3,
                    1, 0, 0.02, 0, 0.005);
        }
    }

    // ── Rewind stepping ───────────────────────────────────────

    private static void tickRewind(Player player, Level level, UUID uuid, ItemStack held) {
        RewindState state = rewindStates.get(uuid);

        state.tickTimer++;
        if (state.tickTimer < 4) return; // step every 4 ticks = fast but visible
        state.tickTimer = 0;

        if (state.remaining.isEmpty()) {
            // Rewind complete
            rewindStates.remove(uuid);
            int rewound = state.totalSteps;

            // Apply cooldown = seconds rewound
            if (held != null) {
                player.getCooldowns().addCooldown(held.getItem(), rewound * SNAPSHOT_INTERVAL);
            }

            // Queue Fate-Debt correction
            debtStates.put(uuid, new DebtState(rewound));

            player.displayClientMessage(
                    Component.literal("Fate takes note.")
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC), true);
            return;
        }

        // Apply next snapshot step
        Snapshot snap = state.remaining.pollFirst();
        player.teleportTo(snap.pos().x, snap.pos().y, snap.pos().z);
        player.setHealth(Math.min(snap.hp(), player.getMaxHealth()));
        player.fallDistance = 0;

        // Rewind particles — blue smoke
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    player.getX(), player.getY() + 1, player.getZ(),
                    5, 0.3, 0.5, 0.3, 0.05);
        }
    }

    // ── Fate-Debt correction ──────────────────────────────────

    private static void tickDebtCorrection(Player player, Level level, UUID uuid) {
        DebtState debt = debtStates.get(uuid);
        if (debt == null) return;

        debt.tickTimer--;
        if (debt.tickTimer > 0) return;

        debt.tickTimer = debt.intervalTicks;

        // Unavoidable magic damage — bypasses armor
        player.hurt(level.damageSources().magic(), DEBT_DAMAGE_PER_STEP);

        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX(), player.getY() + 1, player.getZ(),
                    4, 0.4, 0.4, 0.4, 0.03);
        }

        debt.charges--;
        if (debt.charges <= 0) {
            debtStates.remove(uuid);
            player.displayClientMessage(
                    Component.literal("The debt is settled. For now.")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }

    // ── Timeline Collapse ─────────────────────────────────────

    private static void triggerTimelineCollapse(Player player, Level level, ItemStack stack) {
        UUID uuid = player.getUUID();

        // Crack the hourglass (stored on player — ItemStack tag API removed in 1.21)
        int cracks = player.getPersistentData().getInt("UH_Cracks") + 1;
        player.getPersistentData().putInt("UH_Cracks", cracks);

        player.sendSystemMessage(
                Component.literal("The Hourglass cracks. [" + cracks + "/" + MAX_CRACKS + "]")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));

        // Shatter at max cracks — transform into Dead Hourglass
        if (cracks >= MAX_CRACKS) {
            player.sendSystemMessage(
                    Component.literal("The Hourglass shatters. Time does not forgive.")
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));

            // Replace with Dead Hourglass
            // Assumes ModItems.DEAD_HOURGLASS is registered
            ItemStack dead = new ItemStack(
                    net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                            ResourceLocation.fromNamespaceAndPath("frogerealm", "dead_hourglass")));
            player.setItemInHand(
                    player.getItemInHand(InteractionHand.MAIN_HAND).getItem() instanceof UnfinishedHourglass
                            ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND,
                    dead);
            return;
        }

        // ── Spawn ghost at current position ──
        if (!(level instanceof ServerLevel sl)) return;

        Vec3 ghostPos = player.position();
        net.minecraft.world.entity.monster.WitherSkeleton ghost =
                net.minecraft.world.entity.EntityType.WITHER_SKELETON.create(sl);
        if (ghost == null) return;

        ghost.moveTo(ghostPos.x, ghostPos.y, ghostPos.z, player.getYRot(), 0);
        ghost.getPersistentData().putBoolean("UH_Ghost", true);
        ghost.getPersistentData().putString("UH_Owner", player.getStringUUID());

        // Make it look like a ghost: invisible but glowing, no AI aggression
        ghost.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        ghost.addEffect(new MobEffectInstance(MobEffects.GLOWING,      Integer.MAX_VALUE, 0, false, false));
        ghost.setNoAi(true);
        ghost.setInvulnerable(true);
        ghost.setSilent(true);

        sl.addFreshEntity(ghost);
        ghostStates.put(uuid, new GhostState(ghostPos, ghost));

        // Flash + explosion particles at ghost spawn
        sl.sendParticles(ParticleTypes.FLASH, ghostPos.x, ghostPos.y + 1, ghostPos.z, 1, 0, 0, 0, 0);

        player.sendSystemMessage(
                Component.literal("A moment fractures. Your past self lingers.")
                        .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    // ── Ghost tick ────────────────────────────────────────────

    private static void tickGhost(Player player, Level level, UUID uuid) {
        GhostState state = ghostStates.get(uuid);
        if (state == null) return;

        state.ticksRemaining--;

        // Check if any entity walks into the ghost AABB
        if (level instanceof ServerLevel sl) {
            List<LivingEntity> intruders = level.getEntitiesOfClass(
                    LivingEntity.class,
                    state.ghost.getBoundingBox().inflate(GHOST_TRIGGER_RADIUS),
                    e -> e != player && !e.getPersistentData().getBoolean("UH_Ghost")
            );

            if (!intruders.isEmpty()) {
                // Explode centered on the PLAYER, not the ghost
                sl.explode(null,
                        player.getX(), player.getY(), player.getZ(),
                        4.0f,
                        Level.ExplosionInteraction.NONE); // no block damage

                state.ghost.discard();
                ghostStates.remove(uuid);

                player.displayClientMessage(
                        Component.literal("The timelines collide.")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
                return;
            }

            // Ghost particles
            if ((state.ticksRemaining % 6) == 0) {
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        state.position.x + (Math.random() - 0.5),
                        state.position.y + Math.random() * 2,
                        state.position.z + (Math.random() - 0.5),
                        3, 0.2, 0.3, 0.2, 0.02);
            }
        }

        if (state.ticksRemaining <= 0) {
            state.ghost.discard();
            ghostStates.remove(uuid);
            player.displayClientMessage(
                    Component.literal("The moment passes.")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
        }
    }

    // ── Dead Hourglass curse ──────────────────────────────────

    private static void tickDeadHourglass(Player player, Level level, UUID uuid) {
        ResourceLocation deadId = ResourceLocation.fromNamespaceAndPath("frogerealm", "dead_hourglass");

        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty() && stack.getItem().builtInRegistryHolder().key().location().equals(deadId)) {
                // Apply Wither IV every second
                if ((player.tickCount % 20) == 0) {
                    player.hurt(level.damageSources().wither(), 4.0f);
                    player.displayClientMessage(
                            Component.literal("The dead sand consumes you.")
                                    .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
                }
                // Cannot be dropped — re-insert if inventory is manipulated
                // (Forge's ItemTooltipEvent or ContainerEvent would be needed for
                //  full drop prevention; this at minimum punishes holding it.)
            }
        }
    }

    // ── Utility ───────────────────────────────────────────────

    private static ItemStack findHourglass(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof UnfinishedHourglass) return stack;
        }
        return null;
    }

    // ── Tooltip ───────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        // Crack count is stored on the player (player context unavailable in tooltip)
        // — shown in the player's action bar during gameplay instead
        int cracks = 0; // placeholder; real value read from player.getPersistentData()

        tooltip.add(Component.literal("Sealed Artifact — Grade 0")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Fate / Time Pathway")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Debt Ledger:")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Records your position & HP every second")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  Stores up to 60 seconds of history")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Right-click — Rewind:")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Replays history in reverse (position + HP)")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("  Cooldown = seconds rewound")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("⚠ Fate's Correction:")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  After rewind: unavoidable damage waves")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  More rewind = faster, harsher correction")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("⚠ Grade 0 — Timeline Collapse (>45 s rewind):")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Ghost spawns at pre-rewind position")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  If touched: explosion centered on YOU")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  Each collapse cracks this artifact")
                .withStyle(ChatFormatting.DARK_RED));

        if (cracks > 0) {
            tooltip.add(Component.literal(" "));
            tooltip.add(Component.literal("Cracks: " + cracks + "/" + MAX_CRACKS)
                    .withStyle(cracks >= MAX_CRACKS - 1 ? ChatFormatting.DARK_RED : ChatFormatting.RED,
                            ChatFormatting.BOLD));
            if (cracks >= MAX_CRACKS - 1) {
                tooltip.add(Component.literal("  One more collapse will shatter it forever.")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            }
        }

        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("\"The sand runs upward. It remembers where it fell.\"")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}