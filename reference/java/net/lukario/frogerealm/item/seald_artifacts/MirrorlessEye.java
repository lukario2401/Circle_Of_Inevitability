package net.lukario.frogerealm.item.seald_artifacts;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Optional;

/**
 * ══════════════════════════════════════════════════════════════
 *   THE MIRRORLESS EYE  —  Sealed Artifact, Grade 0
 * ══════════════════════════════════════════════════════════════
 *
 * Inspired by Lord of the Mysteries (Light / Radiance / Divinity).
 * A perfectly smooth sphere of white glass, the size of an eye,
 * that casts no reflection and emits no shadow. It was the literal
 * eye of a Sequence 0 Sun deity, struck blind by its own radiance.
 * The eye still sees. It sees everything.
 * Including you.
 *
 * ── PASSIVE — Divine Gaze ────────────────────────────────────
 *   While held, the Eye observes all entities within a radius.
 *   Radius grows with "Gaze Level" (increases every 60 s, max 4):
 *     Level 0: 20 blocks   Level 2: 32 blocks
 *     Level 1: 26 blocks   Level 3: 40 blocks   Level 4: 48 blocks
 *
 *   All observed entities are ILLUMINATED:
 *     • Glowing effect (visible through walls)
 *     • Undead entities burn (Fire 5 s, refreshed)
 *     • All positive effects stripped every 5 s
 *     (Potions, Speed, Strength, Resistance, Absorption — all gone)
 *
 * ── PASSIVE — Reciprocal Sight ────────────────────────────────
 *   Every 30 s: the Eye reveals a truth about the holder in chat.
 *   Truths cycle:
 *     1. Current HP and max HP
 *     2. Exact coordinates
 *     3. Total kill count (approximated via tickCount)
 *     4. Nearest player's name and distance
 *     5. "It knows what you fear." (flavour — then Gaze Inverts)
 *   After 5 revelations: truths broadcast to all players
 *   within 64 blocks every subsequent revelation.
 *
 * ── ACTIVE (right-click) — Blinding Light ────────────────────
 *   Fires a 30-block cone of divine radiance forward:
 *     • Blindness II + Wither I (10 s) on all entities in cone
 *     • Undead entities ignited for 10 s
 *     • Costs 8 hearts (16 HP). Min 1 HP remaining.
 *     • 2-minute cooldown.
 *   SOUL_FIRE_FLAME + FLASH particles along cone.
 *
 * ── GRADE 0 — THE GAZE INVERTS ───────────────────────────────
 *   Triggers after 10 total revelations.
 *   State: "ME_Inverted" = true (permanent until item discarded).
 *
 *   Effects:
 *     • Nausea II bursts every 8 s
 *     • Mobs within 64 blocks aggro the player from spawn
 *     • Every 60 s: server-wide broadcast of player's position
 *       ("The Eye has found [name] at X, Y, Z")
 *     • Glowing applied to the PLAYER permanently
 *       (everyone can see you through walls)
 *     • Darkness effect pulses every 15 s
 *
 * ── STATE (player.getPersistentData()) ────────────────────────
 *   "ME_RevealTicks"    int     — ticks until next revelation
 *   "ME_Revelations"    int     — total revelations so far
 *   "ME_GazeLevel"      int     — current gaze radius tier (0–4)
 *   "ME_GazeTicks"      int     — ticks until next gaze upgrade
 *   "ME_BroadcastTicks" int     — ticks until next server broadcast
 *   "ME_Inverted"       boolean — Gaze Inversion active
 *
 * ── REGISTRATION ──────────────────────────────────────────────
 *   public static final RegistryObject<Item> MIRRORLESS_EYE =
 *       ITEMS.register("mirrorless_eye",
 *           () -> new MirrorlessEye(
 *               new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
 */
@Mod.EventBusSubscriber
public class MirrorlessEye extends Item {

    // ── Constants ─────────────────────────────────────────────

    private static final int[]  GAZE_RADII         = {20, 26, 32, 40, 48};
    private static final int    GAZE_UPGRADE_TICKS = 1200; // 60 s per level
    private static final int    REVEAL_INTERVAL    = 600;  // 30 s
    private static final int    BROADCAST_INTERVAL = 1200; // 60 s
    private static final int    INVERT_THRESHOLD   = 10;   // revelations
    private static final int    ACTIVE_COOLDOWN    = 2400; // 2 min
    private static final float  ACTIVE_HP_COST     = 16f;  // 8 hearts
    private static final double CONE_LENGTH        = 30.0;
    private static final double CONE_HALF_ANGLE    = Math.toRadians(30); // 60° total FOV
    private static final double AGGRO_RADIUS_INVERTED = 64.0;

    // ── Strip-list: effect IDs to purge from Illuminated ─────
    // These are the registry keys of positive effects
    private static final ResourceLocation[] STRIP_EFFECTS = {
            ResourceLocation.fromNamespaceAndPath("minecraft", "speed"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "strength"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "resistance"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "absorption"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "regeneration"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "fire_resistance"),
            ResourceLocation.fromNamespaceAndPath("minecraft", "invisibility"),
    };

    // ── Constructor ───────────────────────────────────────────

    public MirrorlessEye(Properties properties) {
        super(properties);
    }

    // ── Active: Blinding Light ────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);

        // HP cost
        float newHp = Math.max(1f, player.getHealth() - ACTIVE_HP_COST);
        player.setHealth(newHp);

        Vec3 look   = player.getLookAngle();
        Vec3 origin = player.getEyePosition();

        // Find all entities in cone
        level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(CONE_LENGTH),
                e -> e != player && isInCone(origin, look, e.position(), CONE_LENGTH, CONE_HALF_ANGLE)
        ).forEach(e -> {
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,  200, 1, false, true));
            e.addEffect(new MobEffectInstance(MobEffects.WITHER,     200, 0, false, true));
            if (e.isUnderWater() || isSensitiveToLight(e)) {
                e.igniteForSeconds(10);
            }
            // Undead burn
            if (e instanceof Monster m && m.isSensitiveToWater()) {
                e.igniteForSeconds(10);
            }
        });

        // Cone particles
        if (level instanceof ServerLevel sl) {
            for (double d = 1.0; d <= CONE_LENGTH; d += 1.5) {
                double spread = d * Math.tan(CONE_HALF_ANGLE) * 0.5;
                sl.sendParticles(ParticleTypes.END_ROD,
                        origin.x + look.x * d + (Math.random() - 0.5) * spread,
                        origin.y + look.y * d + (Math.random() - 0.5) * spread,
                        origin.z + look.z * d + (Math.random() - 0.5) * spread,
                        1, 0, 0, 0, 0.01);
            }
            sl.sendParticles(ParticleTypes.FLASH,
                    player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        }

        player.displayClientMessage(
                Component.literal("Let there be light.")
                        .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD), true);

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
        if (findEye(player) == null) return;

        var data = player.getPersistentData();
        boolean inverted = data.getBoolean("ME_Inverted");

        // ── Gaze level upgrade ──
        int gazeTicks = data.getInt("ME_GazeTicks") - 1;
        int gazeLevel = data.getInt("ME_GazeLevel");
        if (gazeTicks <= 0 && gazeLevel < GAZE_RADII.length - 1) {
            gazeLevel++;
            data.putInt("ME_GazeLevel", gazeLevel);
            data.putInt("ME_GazeTicks", GAZE_UPGRADE_TICKS);
            player.displayClientMessage(
                    Component.literal("The Eye opens wider.")
                            .withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC), true);
        } else {
            data.putInt("ME_GazeTicks", Math.max(0, gazeTicks));
        }

        double radius = GAZE_RADII[gazeLevel];

        // ── Divine Gaze: Illuminate entities ──
        if ((player.tickCount % 20) == 0) {
            tickDivineGaze(player, level, radius);
        }

        // ── Gaze particles ──
        if (level instanceof ServerLevel sl && (player.tickCount % 10) == 0) {
            sl.sendParticles(ParticleTypes.END_ROD,
                    player.getX() + (Math.random() - 0.5) * 0.4,
                    player.getY() + 1.4,
                    player.getZ() + (Math.random() - 0.5) * 0.4,
                    1, 0, 0.05, 0, 0.01);
        }

        // ── Reciprocal Sight: revelation timer ──
        int revealTicks = data.getInt("ME_RevealTicks") - 1;
        if (revealTicks <= 0) {
            data.putInt("ME_RevealTicks", REVEAL_INTERVAL);
            int revelations = data.getInt("ME_Revelations") + 1;
            data.putInt("ME_Revelations", revelations);
            deliverRevelation(player, level, revelations, inverted);

            if (revelations >= INVERT_THRESHOLD && !inverted) {
                triggerGazeInversion(player);
                inverted = true;
            }
        } else {
            data.putInt("ME_RevealTicks", revealTicks);
        }

        // ── Inverted state effects ──
        if (inverted) {
            tickInvertedState(player, level, data);
        }
    }

    // ── Divine Gaze tick ─────────────────────────────────────

    private static void tickDivineGaze(Player player, Level level, double radius) {
        level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(radius),
                e -> e != player
        ).forEach(e -> {
            // Glowing — permanent while in range
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));

            // Undead burn
            if (e.isSensitiveToWater()) {
                e.igniteForSeconds(10);
            }

            // Strip positive effects every 5 s
            if ((player.tickCount % 100) == 0) {
                stripPositiveEffects(e, level);
            }
        });
    }

    // ── Strip positive effects ────────────────────────────────

    private static void stripPositiveEffects(LivingEntity entity, Level level) {
        for (ResourceLocation effectId : STRIP_EFFECTS) {
            // 1. Create a Key specifically for a Mob Effect
            ResourceKey<MobEffect> key = ResourceKey.create(Registries.MOB_EFFECT, effectId);

            // 2. Safely ask the registry for the Holder
            Optional<Holder.Reference<MobEffect>> effectHolder =
                    net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(key);

            // 3. If it exists, and the entity has it, remove it! No casting required.
            if (effectHolder.isPresent() && entity.hasEffect(effectHolder.get())) {
                entity.removeEffect(effectHolder.get());
            }
        }

        // Particle burst when stripped
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLASH,
                    entity.getX(), entity.getY() + entity.getBbHeight() * 0.5,
                    entity.getZ(), 1, 0, 0, 0, 0);
        }
    }

    // ── Reciprocal Sight: deliver revelation ──────────────────

    private static void deliverRevelation(Player player, Level level,
                                          int count, boolean broadcast) {
        Component truth;
        int cycle = ((count - 1) % 5) + 1;

        truth = switch (cycle) {
            case 1 -> Component.literal("The Eye sees: ")
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(
                            String.format("%.1f / %.1f HP", player.getHealth(), player.getMaxHealth()))
                            .withStyle(ChatFormatting.YELLOW));

            case 2 -> Component.literal("The Eye sees: ")
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(
                            String.format("%.0f, %.0f, %.0f",
                                    player.getX(), player.getY(), player.getZ()))
                            .withStyle(ChatFormatting.YELLOW));

            case 3 -> Component.literal("The Eye sees: ")
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal(
                            player.getKillCredit() != null
                                    ? "blood on your hands"
                                    : "you have not yet killed")
                            .withStyle(ChatFormatting.YELLOW));

            case 4 -> {
                // Find nearest other player
                Player nearest = null;
                double nearestDist = Double.MAX_VALUE;
                for (Player other : level.getEntitiesOfClass(Player.class,
                        player.getBoundingBox().inflate(200), p -> p != player)) {
                    double d = other.distanceTo(player);
                    if (d < nearestDist) { nearestDist = d; nearest = other; }
                }
                yield nearest != null
                        ? Component.literal("The Eye sees: ")
                            .withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(
                                    nearest.getName().getString()
                                    + " is " + (int) nearestDist + " blocks away")
                                    .withStyle(ChatFormatting.YELLOW))
                        : Component.literal("The Eye sees: ")
                            .withStyle(ChatFormatting.WHITE)
                            .append(Component.literal("you are alone")
                                    .withStyle(ChatFormatting.YELLOW));
            }

            default -> Component.literal("The Eye sees: ")
                    .withStyle(ChatFormatting.WHITE)
                    .append(Component.literal("it knows what you fear.")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        };

        // Always tell the player
        player.sendSystemMessage(truth);

        // Broadcast to nearby players after 5 revelations
        if (broadcast && level instanceof ServerLevel sl) {
            Component broadcast_msg = Component.literal("[The Eye] ")
                    .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD)
                    .append(truth);

            sl.getEntitiesOfClass(Player.class,
                    player.getBoundingBox().inflate(64),
                    p -> p != player
            ).forEach(p -> p.sendSystemMessage(broadcast_msg));
        }
    }

    // ── Gaze Inversion trigger ────────────────────────────────

    private static void triggerGazeInversion(Player player) {
        player.getPersistentData().putBoolean("ME_Inverted", true);
        player.getPersistentData().putInt("ME_BroadcastTicks", BROADCAST_INTERVAL);

        player.sendSystemMessage(Component.literal("══════════════════════════════").withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(
                Component.literal("THE GAZE INVERTS")
                        .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        player.sendSystemMessage(
                Component.literal("It has seen enough of the world.")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        player.sendSystemMessage(
                Component.literal("Now it watches only you.")
                        .withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal("══════════════════════════════").withStyle(ChatFormatting.WHITE));
    }

    // ── Inverted state tick ───────────────────────────────────

    private static void tickInvertedState(Player player, Level level,
                                          net.minecraft.nbt.CompoundTag data) {
        // Player is permanently Glowing (everyone sees them through walls)
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));

        // Nausea bursts every 8 s
        if ((player.tickCount % 160) == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 800, 1, false, true));
        }

        // Darkness pulses every 15 s
        if ((player.tickCount % 300) == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, true));
        }

        // Force aggro from mobs within 64 blocks
        if ((player.tickCount % 60) == 0) {
            level.getEntitiesOfClass(Monster.class,
                    player.getBoundingBox().inflate(AGGRO_RADIUS_INVERTED),
                    mob -> mob.getTarget() == null
            ).forEach(mob -> mob.setTarget(player));
        }

        // Server-wide position broadcast every 60 s
        int broadcastTicks = data.getInt("ME_BroadcastTicks") - 1;
        if (broadcastTicks <= 0) {
            data.putInt("ME_BroadcastTicks", BROADCAST_INTERVAL);
            broadcastPosition(player, level);
        } else {
            data.putInt("ME_BroadcastTicks", broadcastTicks);
        }

        // Inverted particles — constant light motes leaking from player
        if (level instanceof ServerLevel sl && (player.tickCount % 5) == 0) {
            sl.sendParticles(ParticleTypes.END_ROD,
                    player.getX() + (Math.random() - 0.5) * 1.0,
                    player.getY() + Math.random() * 2.2,
                    player.getZ() + (Math.random() - 0.5) * 1.0,
                    2, 0, 0.05, 0, 0.02);
        }
    }

    // ── Server-wide position broadcast ───────────────────────

    private static void broadcastPosition(Player player, Level level) {
        Component msg = Component.literal("[The Eye has found ")
                .withStyle(ChatFormatting.WHITE)
                .append(player.getName().copy().withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(String.format(" at %.0f, %.0f, %.0f]",
                        player.getX(), player.getY(), player.getZ()))
                        .withStyle(ChatFormatting.WHITE));

        // Broadcast to entire server
        if (level instanceof ServerLevel sl) {
            MinecraftServer server = sl.getServer();
            for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                sp.sendSystemMessage(msg);
            }
        }
    }

    // ── Cone check ────────────────────────────────────────────

    private static boolean isInCone(Vec3 origin, Vec3 direction, Vec3 target,
                                    double maxLength, double halfAngle) {
        Vec3 toTarget = target.subtract(origin);
        double dist   = toTarget.length();
        if (dist > maxLength || dist < 0.1) return false;
        double angle  = Math.acos(
                Math.max(-1, Math.min(1, toTarget.normalize().dot(direction.normalize()))));
        return angle <= halfAngle;
    }

    // ── Undead/light-sensitive check ──────────────────────────

    private static boolean isSensitiveToLight(LivingEntity entity) {
        return entity.isSensitiveToWater(); // undead entities are water-sensitive
    }

    // ── Utility ───────────────────────────────────────────────

    private static ItemStack findEye(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof MirrorlessEye) return stack;
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
        tooltip.add(Component.literal("Light / Radiance / Divinity Pathway")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Divine Gaze (20–48 blocks, grows over time):")
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  All entities Illuminated: Glowing, undead burn")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  Strips all positive effects every 5 s")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Reciprocal Sight:")
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Every 30 s: Eye reveals a truth about you")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("  After 5: truths broadcast to nearby players")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Right-click — Blinding Light:")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Cost: 8 hearts. 30-block cone of radiance.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  Blindness + Wither on all in cone. 2 min cooldown.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("⚠ Grade 0 — The Gaze Inverts (10 revelations):")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  You glow permanently — visible through walls")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  Mobs aggro from 64 blocks away")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  Server-wide broadcast of your position every 60 s")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  Nausea + Darkness bursts. It watches only you.")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("\"It casts no reflection. It needs none.\"")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
