package net.lukario.frogerealm.item.seald_artifacts;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════
 *   THE COLLAPSAR BEAD  —  Sealed Artifact, Grade 0
 * ══════════════════════════════════════════════════════════════
 *
 * Inspired by Lord of the Mysteries (Void / Gravity / Collapse).
 * A perfectly spherical black marble. It was carved from the
 * collapsed core of a dead Beyonder god whose domain was gravity.
 * The god didn't die — it became too dense to interact with
 * anything anymore. The Bead is what remained on the surface
 * after everything else fell inward.
 *
 * No accumulation. No stacks. No charges. Pure gravity.
 *
 * ── PASSIVE — Gravity Sovereign ──────────────────────────────
 *   Right-click (not sneaking) cycles gravity mode:
 *     0 → NORMAL    (standard gravity)
 *     1 → INVERTED  (fall upward, Levitation II applied each tick)
 *     2 → ZERO      (free float, no gravity, Slow Falling + Levitation 0)
 *   Mode stored in "CB_GravMode" (0/1/2) on player persistentData.
 *   Immune to fall damage in all non-normal modes.
 *
 * ── ACTIVE — Singularity (right-click + sneak) ────────────────
 *   Projects a gravitational singularity to cursor target
 *   (max 20 blocks, must hit a block face).
 *   Duration: 3 seconds (60 ticks).
 *   Pull phase (ticks 1–40):
 *     All entities within 12 blocks pulled toward singularity.
 *     Crushing damage = 1.5 × (1 - distance/12) per tick (every 10t).
 *   Release phase (ticks 41–60):
 *     Explosive fling — all entities within 12 blocks launched away.
 *   Cooldown: 60 s. No charges.
 *
 *   State: "CB_SingActive" boolean, "CB_SingTicks" int,
 *          "CB_SingX/Y/Z" double (singularity position).
 *
 * ── GRADE 0 — COLLAPSE RADIUS ────────────────────────────────
 *   Every 40 s, the Bead slips its leash.
 *   Fires one of four effects at random, instantly, no warning:
 *
 *   0 — IMPLOSION:  All entities within 20 blocks pulled to player.
 *   1 — EXPULSION:  All entities within 20 blocks launched away.
 *   2 — INVERSION:  All entities within 20 blocks get Levitation III
 *                   for 10 s (flung upward).
 *   3 — COLLAPSE:   5-block sphere of solid blocks around player
 *                   converted to Air/Gravel randomly.
 *
 *   After each event: permanently rewrites one random attribute
 *   modifier on the player (replacing the previous one):
 *     Possible outcomes (uniform random):
 *       +4 attack damage  |  −4 attack damage
 *       +0.1 move speed   |  −0.05 move speed
 *       +4 max HP         |  −4 max HP
 *       +20% knockback resist | nothing (null roll)
 *   This modifier replaces the previous Bead modifier each time.
 *   The player never knows what's coming — stat or disaster.
 *
 *   State: "CB_CollapseTicks" int (countdown, resets to 800 each fire).
 *          "CB_LastModType"   int (which attribute was last modified).
 *
 * ── ALL STATE (player.getPersistentData()) ────────────────────
 *   "CB_GravMode"       int     — 0/1/2 gravity mode
 *   "CB_SingActive"     boolean — singularity running
 *   "CB_SingTicks"      int     — ticks elapsed in singularity
 *   "CB_SingX/Y/Z"      double  — singularity world position
 *   "CB_CollapseTicks"  int     — ticks until next collapse event
 *   "CB_LastModType"    int     — last attribute modifier type applied
 *
 * ── REGISTRATION ──────────────────────────────────────────────
 *   public static final RegistryObject<Item> COLLAPSAR_BEAD =
 *       ITEMS.register("collapsar_bead",
 *           () -> new CollapsarBead(
 *               new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
 */
@Mod.EventBusSubscriber
public class CollapsarBead extends Item {

    // ── Constants ─────────────────────────────────────────────
    private static final int    COLLAPSE_INTERVAL    = 260;  // 40 s
    private static final int    SINGULARITY_DURATION = 120;   // 3 s
    private static final int    SINGULARITY_COOLDOWN = 560; // 60 s
    private static final double SINGULARITY_RADIUS   = 12.0;
    private static final double SINGULARITY_RANGE    = 20.0;
    private static final double IMPULSE_RADIUS       = 20.0;

    // ── Attribute modifier ResourceLocation ───────────────────
    private static final ResourceLocation BEAD_MOD_ID =
            ResourceLocation.fromNamespaceAndPath("frogerealmmod", "cb_bead_modifier");

    // ── Gravity mode labels ───────────────────────────────────
    private static final String[] GRAV_NAMES = {"NORMAL", "INVERTED", "ZERO-G"};

    // ── Constructor ───────────────────────────────────────────

    public CollapsarBead(Properties properties) {
        super(properties);
    }

    // ── Active: cycle gravity / fire singularity ──────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        var data = player.getPersistentData();

        if (player.isShiftKeyDown()) {
            // ── Singularity ──
            if (player.getCooldowns().isOnCooldown(this))
                return InteractionResultHolder.pass(stack);
            if (data.getBoolean("CB_SingActive"))
                return InteractionResultHolder.pass(stack);

            // Raycast to find target position
            HitResult hit = player.pick(SINGULARITY_RANGE, 1.0f, false);
            Vec3 singPos;
            if (hit.getType() == HitResult.Type.BLOCK) {
                singPos = ((BlockHitResult) hit).getLocation();
            } else {
                // Place at max range along look vector
                singPos = player.getEyePosition()
                        .add(player.getLookAngle().scale(SINGULARITY_RANGE));
            }

            data.putBoolean("CB_SingActive", true);
            data.putInt("CB_SingTicks", 0);
            data.putDouble("CB_SingX", singPos.x);
            data.putDouble("CB_SingY", singPos.y);
            data.putDouble("CB_SingZ", singPos.z);

            // Singularity spawn particles
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        singPos.x, singPos.y, singPos.z,
                        30, 0.5, 0.5, 0.5, 0.3);
            }

            player.sendSystemMessage(
                    Component.literal("A point of no return opens.")
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));

            player.getCooldowns().addCooldown(this, SINGULARITY_COOLDOWN);

        } else {
            // ── Cycle gravity mode ──
            int mode = (data.getInt("CB_GravMode") + 1) % 3;
            data.putInt("CB_GravMode", mode);

            String label = GRAV_NAMES[mode];
            ChatFormatting color = switch (mode) {
                case 1  -> ChatFormatting.DARK_AQUA;
                case 2  -> ChatFormatting.LIGHT_PURPLE;
                default -> ChatFormatting.GRAY;
            };

            player.sendSystemMessage(
                    Component.literal("Gravity: ")
                            .withStyle(ChatFormatting.WHITE)
                            .append(Component.literal(label).withStyle(color, ChatFormatting.BOLD)));

            // Reset fall distance to avoid fall damage spike on mode switch
            player.fallDistance = 0;
        }

        return InteractionResultHolder.success(stack);
    }

    // ── Server Tick ───────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        Level  level  = player.level();
        if (level.isClientSide) return;
        if (findBead(player) == null) return;

        var data = player.getPersistentData();

        // ── Apply gravity mode ──
        tickGravityMode(player, level, data.getInt("CB_GravMode"));

        // ── Singularity tick ──
        if (data.getBoolean("CB_SingActive")) {
            tickSingularity(player, level, data);
        }

        // ── Collapse event countdown ──
        int collapseTicks = data.getInt("CB_CollapseTicks") - 1;
        if (collapseTicks <= 0) {
            data.putInt("CB_CollapseTicks", COLLAPSE_INTERVAL);
            fireCollapseEvent(player, level, data);
        } else {
            data.putInt("CB_CollapseTicks", collapseTicks);
        }

        // ── Ambient void particles ──
        if (level instanceof ServerLevel sl && (player.tickCount % 12) == 0) {
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    player.getX() + (Math.random() - 0.5) * 1.5,
                    player.getY() + Math.random() * 2.2,
                    player.getZ() + (Math.random() - 0.5) * 1.5,
                    1, 0, 0.02, 0, 0.05);
        }
    }

    // ── Gravity mode application ──────────────────────────────

    private static void tickGravityMode(Player player, Level level, int mode) {
        switch (mode) {
            case 1 -> {
                // INVERTED — fall upward
                player.addEffect(new MobEffectInstance(
                        MobEffects.LEVITATION, 200, 4, false, false));
                player.fallDistance = 0;
                // Ceiling hit protection
                if (level instanceof ServerLevel sl && (player.tickCount % 20) == 0) {
                    sl.sendParticles(ParticleTypes.FALLING_WATER,
                            player.getX(), player.getY() + 2.2, player.getZ(),
                            3, 0.3, 0, 0.3, 0.01);
                }
            }
            case 2 -> {
                // ZERO-G — free float
                player.addEffect(new MobEffectInstance(
                        MobEffects.SLOW_FALLING, 200, 0, false, false));
                // Counteract gravity manually
                double vy = player.getDeltaMovement().y;
                if (vy < 0 && !player.onGround()) {
                    player.setDeltaMovement(
                            player.getDeltaMovement().x,
                            Math.min(vy + 0.08, 0), // cancel gravity increment
                            player.getDeltaMovement().z);
                }
                player.fallDistance = 0;
            }
            // case 0: normal — do nothing
        }
    }

    // ── Singularity tick ──────────────────────────────────────

    private static void tickSingularity(Player player, Level level,
                                        net.minecraft.nbt.CompoundTag data) {
        int ticks = data.getInt("CB_SingTicks") + 1;
        data.putInt("CB_SingTicks", ticks);

        Vec3 singPos = new Vec3(
                data.getDouble("CB_SingX"),
                data.getDouble("CB_SingY"),
                data.getDouble("CB_SingZ"));

        if (ticks <= 40) {
            // ── Pull phase ──
            level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(
                            singPos.x - SINGULARITY_RADIUS, singPos.y - SINGULARITY_RADIUS,
                            singPos.z - SINGULARITY_RADIUS, singPos.x + SINGULARITY_RADIUS,
                            singPos.y + SINGULARITY_RADIUS, singPos.z + SINGULARITY_RADIUS),
                    e -> e.position().distanceTo(singPos) < SINGULARITY_RADIUS
            ).forEach(e -> {
                double dist = e.position().distanceTo(singPos);
                Vec3   pull = singPos.subtract(e.position()).normalize().scale(0.35);
                e.setDeltaMovement(e.getDeltaMovement().add(pull));
                e.hurtMarked = true;

                // Crushing damage every 10 ticks
                if ((ticks % 10) == 0) {
                    float dmg = (float)(1.5 * (1.0 - dist / SINGULARITY_RADIUS));
                    e.hurt(level.damageSources().magic(), Math.max(0.5f, dmg));
                }
            });

            // Pull particles spiraling inward
            if (level instanceof ServerLevel sl && (ticks % 3) == 0) {
                double angle = ticks * 0.4;
                double r     = SINGULARITY_RADIUS * (1.0 - ticks / 40.0);
                sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                        singPos.x + Math.cos(angle) * r,
                        singPos.y + (Math.random() - 0.5) * 2,
                        singPos.z + Math.sin(angle) * r,
                        2, 0, 0, 0, 0.05);
            }

        } else if (ticks <= SINGULARITY_DURATION) {
            // ── Release phase ──
            level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(
                            singPos.x - SINGULARITY_RADIUS, singPos.y - SINGULARITY_RADIUS,
                            singPos.z - SINGULARITY_RADIUS, singPos.x + SINGULARITY_RADIUS,
                            singPos.y + SINGULARITY_RADIUS, singPos.z + SINGULARITY_RADIUS),
                    e -> e.position().distanceTo(singPos) < SINGULARITY_RADIUS
            ).forEach(e -> {
                Vec3 fling = e.position().subtract(singPos).normalize().scale(2.8);
                e.setDeltaMovement(fling.x, Math.abs(fling.y) + 0.5, fling.z);
                e.hurtMarked = true;
            });

            // Explosion particles
            if (level instanceof ServerLevel sl && ticks == 41) {
                sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                        singPos.x, singPos.y, singPos.z, 1, 0, 0, 0, 0);
            }

        } else {
            // ── End singularity ──
            data.putBoolean("CB_SingActive", false);
            data.putInt("CB_SingTicks", 0);
        }
    }

    // ── Collapse event ────────────────────────────────────────

    private static void fireCollapseEvent(Player player, Level level,
                                          net.minecraft.nbt.CompoundTag data) {
        int roll = level.random.nextInt(4);

        switch (roll) {
            case 0 -> {
                // ── IMPLOSION: pull everything to player ──
                Vec3 playerPos = player.position();
                level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(IMPULSE_RADIUS),
                        e -> e != player
                ).forEach(e -> {
                    Vec3 pull = playerPos.subtract(e.position()).normalize().scale(3.0);
                    e.setDeltaMovement(pull);
                    e.hurtMarked = true;
                    e.hurt(level.damageSources().magic(), 6f);
                });

                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                            player.getX(), player.getY() + 1, player.getZ(),
                            60, 3, 1, 3, 0.4);
                }

                player.sendSystemMessage(
                        Component.literal("[Collapse] Implosion.")
                                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            }

            case 1 -> {
                // ── EXPULSION: fling everything away ──
                Vec3 playerPos = player.position();
                level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(IMPULSE_RADIUS),
                        e -> e != player
                ).forEach(e -> {
                    Vec3 fling = e.position().subtract(playerPos).normalize().scale(3.5);
                    e.setDeltaMovement(fling.x, 1.5, fling.z);
                    e.hurtMarked = true;
                });

                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                            player.getX(), player.getY() + 1, player.getZ(),
                            1, 0, 0, 0, 0);
                }

                player.sendSystemMessage(
                        Component.literal("[Collapse] Expulsion.")
                                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            }

            case 2 -> {
                // ── INVERSION: everyone floats upward ──
                level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(IMPULSE_RADIUS),
                        e -> e != player
                ).forEach(e -> e.addEffect(
                        new MobEffectInstance(MobEffects.LEVITATION, 200, 2, false, true)));

                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.CLOUD,
                            player.getX(), player.getY() + 1, player.getZ(),
                            40, 4, 0.5, 4, 0.05);
                }

                player.sendSystemMessage(
                        Component.literal("[Collapse] Inversion — up is down.")
                                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            }

            case 3 -> {
                // ── COLLAPSE: shatter blocks in 5-block sphere ──
                BlockPos center = player.blockPosition();
                int r = 5;
                for (int dx = -r; dx <= r; dx++) {
                    for (int dy = -r; dy <= r; dy++) {
                        for (int dz = -r; dz <= r; dz++) {
                            if (dx*dx + dy*dy + dz*dz > r*r) continue;
                            BlockPos pos = center.offset(dx, dy, dz);
                            var state    = level.getBlockState(pos);
                            if (state.isAir() || !state.isSolidRender(level, pos)) continue;

                            float chance = level.random.nextFloat();
                            if (chance < 0.15f) {
                                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                            } else if (chance < 0.35f) {
                                level.setBlock(pos, Blocks.GRAVEL.defaultBlockState(), 3);
                            }
                        }
                    }
                }

                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.LARGE_SMOKE,
                            player.getX(), player.getY() + 1, player.getZ(),
                            20, 2, 1, 2, 0.1);
                }

                player.sendSystemMessage(
                        Component.literal("[Collapse] Structure fails.")
                                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
            }
        }

        // ── Rewrite one random attribute ──
        rewriteBeadModifier(player, level, data);
    }

    // ── Attribute rewrite ─────────────────────────────────────

    private static void rewriteBeadModifier(Player player, Level level,
                                            net.minecraft.nbt.CompoundTag data) {
        // Remove previous modifier from whichever attribute it was on
        int lastType = data.getInt("CB_LastModType");
        removeBeadModifier(player, lastType);

        // Roll a new one (0–7)
        int roll = level.random.nextInt(8);
        data.putInt("CB_LastModType", roll);

        String desc;
        switch (roll) {
            case 0 -> {
                var attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attr != null) attr.addPermanentModifier(
                        new AttributeModifier(BEAD_MOD_ID, 8.0,
                                AttributeModifier.Operation.ADD_VALUE));
                desc = "+4 attack damage";
            }
            case 1 -> {
                var attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
                if (attr != null) attr.addPermanentModifier(
                        new AttributeModifier(BEAD_MOD_ID, -8.0,
                                AttributeModifier.Operation.ADD_VALUE));
                desc = "-4 attack damage";
            }
            case 2 -> {
                var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (attr != null) attr.addPermanentModifier(
                        new AttributeModifier(BEAD_MOD_ID, 0.1,
                                AttributeModifier.Operation.ADD_VALUE));
                desc = "+movement speed";
            }
            case 3 -> {
                var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
                if (attr != null) attr.addPermanentModifier(
                        new AttributeModifier(BEAD_MOD_ID, -0.05,
                                AttributeModifier.Operation.ADD_VALUE));
                desc = "-movement speed";
            }
            case 4 -> {
                var attr = player.getAttribute(Attributes.MAX_HEALTH);
                if (attr != null) {
                    attr.addPermanentModifier(
                            new AttributeModifier(BEAD_MOD_ID, 8.0,
                                    AttributeModifier.Operation.ADD_VALUE));
                    if (player.getHealth() > player.getMaxHealth())
                        player.setHealth(player.getMaxHealth());
                }
                desc = "+4 max HP";
            }
            case 5 -> {
                var attr = player.getAttribute(Attributes.MAX_HEALTH);
                if (attr != null) {
                    attr.addPermanentModifier(
                            new AttributeModifier(BEAD_MOD_ID, -8.0,
                                    AttributeModifier.Operation.ADD_VALUE));
                    if (player.getHealth() > player.getMaxHealth())
                        player.setHealth(player.getMaxHealth());
                }
                desc = "-4 max HP";
            }
            case 6 -> {
                var attr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
                if (attr != null) attr.addPermanentModifier(
                        new AttributeModifier(BEAD_MOD_ID, 0.5,
                                AttributeModifier.Operation.ADD_VALUE));
                desc = "+50% knockback resistance";
            }
            default -> {
                // Null roll — no modifier applied
                data.putInt("CB_LastModType", -1);
                desc = "nothing. The Bead considers you unworthy of change.";
            }
        }

        player.sendSystemMessage(
                Component.literal("[Collapse] The Bead rewrites you: ")
                        .withStyle(ChatFormatting.DARK_PURPLE)
                        .append(Component.literal(desc)
                                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)));
    }

    // ── Remove modifier from the correct attribute ────────────

    private static void removeBeadModifier(Player player, int type) {
        var attrByType = switch (type) {
            case 0, 1 -> player.getAttribute(Attributes.ATTACK_DAMAGE);
            case 2, 3 -> player.getAttribute(Attributes.MOVEMENT_SPEED);
            case 4, 5 -> player.getAttribute(Attributes.MAX_HEALTH);
            case 6    -> player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            default   -> null;
        };
        if (attrByType != null) attrByType.removeModifier(BEAD_MOD_ID);
    }

    // ── Utility ───────────────────────────────────────────────

    private static ItemStack findBead(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof CollapsarBead) return stack;
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
        tooltip.add(Component.literal("Void / Gravity / Collapse Pathway")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Right-click — Cycle Gravity Mode:")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  NORMAL → INVERTED → ZERO-G → NORMAL")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  No fall damage in non-normal modes.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Shift + Right-click — Singularity:")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Gravity point at cursor (20 blocks). 3 s.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  Pull + crush → explosive release. 60 s cooldown.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("⚠ Grade 0 — Collapse Radius (every 40 s):")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Random: Implosion / Expulsion / Inversion / Collapse")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  Then: permanently rewrites one of your attributes.")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  You don't choose. You don't know which is coming.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("\"It weighs exactly as much as you expect.\"")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("\"Then more. Then less. Never the same twice.\"")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
