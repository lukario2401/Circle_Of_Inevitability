package net.lukario.frogerealm.item.seald_artifacts;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════
 *   THE FRACTURED CHORUS  —  Sealed Artifact, Grade I
 * ══════════════════════════════════════════════════════════════
 *
 * Inspired by Lord of the Mysteries (Chaos / Madness pathway).
 * A porcelain mask cracked down the centre, each half depicting
 * a different face — one serene, one screaming. It was worn by
 * a Sequence 3 Chaos Harbinger who attempted to contain two
 * contradictory Beyonder characteristics simultaneously.
 * They succeeded. The mask remembers them both.
 *
 * Worn in the HELMET armor slot (extends ArmorItem).
 *
 * ── PASSIVE — Dual Resonance ──────────────────────────────────
 *   Every 30–90 s (random) the mask shifts personality:
 *
 *   SCHEMER  (calm half):
 *     Speed II + Night Vision + +4 attack damage for 20 s.
 *     Particle: ENCHANT (gold flecks).
 *
 *   BERSERKER  (screaming half):
 *     Strength III + Resistance I for 20 s.
 *     Forces attack on nearest entity every 2 s (ignores allies).
 *     Particle: SOUL_FIRE_FLAME (red).
 *
 * ── ACTIVE (right-click while holding, not worn) ──────────────
 *   Forces an immediate personality shift. 60 s cooldown.
 *
 * ── FORBIDDEN KNOWLEDGE (passive, always on) ──────────────────
 *   Every 2 s: spawns numeric particle clusters above nearby
 *   entities approximating their HP (10 = full, 1 = near-dead).
 *
 * ── DRAWBACKS ─────────────────────────────────────────────────
 *   Every 60 s: one random permanent debuff applied:
 *     • Mining Fatigue II (10 s)
 *     • Slowness II (10 s)
 *     • Hunger III (10 s)
 *
 *   After 10 total shifts → FRACTURE STATE:
 *     Both personalities fight for control every 5 s.
 *     Conflicting effects slam you simultaneously.
 *     Nausea + Blindness bursts.
 *
 *   On death while wearing:
 *     40% chance mask does NOT drop — stays "fused".
 *     Written as NBT "FC_Fused" on the player.
 *     While fused, drawback debuffs apply even without wearing.
 *     Drops only after taking 20+ damage in next life.
 *
 * ── STATE (player.getPersistentData()) ────────────────────────
 *   "FC_ShiftTicks"     int     — ticks until next forced shift
 *   "FC_Personality"    int     — 0 = Schemer, 1 = Berserker
 *   "FC_ActiveTicks"    int     — ticks remaining on personality
 *   "FC_TotalShifts"    int     — lifetime shift counter
 *   "FC_DebuffTicks"    int     — ticks until next random debuff
 *   "FC_Fused"          boolean — mask fused to face after death
 *   "FC_FusedDamage"    float   — damage absorbed while fused
 *
 * ── REGISTRATION ──────────────────────────────────────────────
 *   public static final RegistryObject<Item> FRACTURED_CHORUS =
 *       ITEMS.register("fractured_chorus",
 *           () -> new FracturedChorus(
 *               ArmorMaterials.NETHERITE,
 *               new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
 */
@Mod.EventBusSubscriber
public class FracturedChorus extends ArmorItem {

    // ── Constants ─────────────────────────────────────────────

    private static final int  PERSONALITY_DURATION   = 400;  // 20 s
    private static final int  SHIFT_MIN              = 600;  // 30 s
    private static final int  SHIFT_MAX              = 1800; // 90 s
    private static final int  FORCED_SHIFT_COOLDOWN  = 1200; // 60 s
    private static final int  DEBUFF_INTERVAL        = 1200; // 60 s
    private static final int  FRACTURE_THRESHOLD     = 10;   // shifts before fracture
    private static final int  FRACTURE_BURST_INTERVAL = 100; // 5 s
    private static final float FUSED_DAMAGE_THRESHOLD = 20f;
    private static final float FUSE_CHANCE            = 0.40f;

    private static final ResourceLocation ATTACK_BONUS_ID =
            ResourceLocation.fromNamespaceAndPath("frogerealm", "fc_schemer_attack");

    // ── Constructor ───────────────────────────────────────────

    public FracturedChorus(Holder<ArmorMaterial> material, Properties properties) {
        super(material, ArmorItem.Type.HELMET, properties);
    }

    // ── Active: force shift (right-click while holding) ───────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);

        forceShift(player, level);
        player.getCooldowns().addCooldown(this, FORCED_SHIFT_COOLDOWN);

        return InteractionResultHolder.success(stack);
    }

    // ── Wearing check ─────────────────────────────────────────

    private static boolean isWearing(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof FracturedChorus;
    }

    // ── Server Tick ───────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        Level  level  = player.level();
        if (level.isClientSide) return;

        var data = player.getPersistentData();
        boolean wearing = isWearing(player);
        boolean fused   = data.getBoolean("FC_Fused");

        if (!wearing && !fused) return;

        // ── Fused damage tracking (not wearing, but fused) ──
        if (fused && !wearing) {
            tickFusedDebuffs(player);
            return;
        }

        // ── Forbidden Knowledge: HP readout via particles ──
        if (level instanceof ServerLevel sl && (player.tickCount % 40) == 0) {
            tickForbiddenKnowledge(player, sl);
        }

        // ── Personality active tick ──
        int activeTicks = data.getInt("FC_ActiveTicks");
        if (activeTicks > 0) {
            data.putInt("FC_ActiveTicks", activeTicks - 1);
            applyPersonalityEffects(player, level, data.getInt("FC_Personality"), activeTicks);
        } else {
            // Personality worn off — remove attack bonus if Schemer was active
            removeAttackBonus(player);
        }

        // ── Shift timer ──
        int shiftTicks = data.getInt("FC_ShiftTicks") - 1;
        if (shiftTicks <= 0) {
            forceShift(player, level);
        } else {
            data.putInt("FC_ShiftTicks", shiftTicks);
        }

        // ── Fracture State bursts ──
        int totalShifts = data.getInt("FC_TotalShifts");
        if (totalShifts >= FRACTURE_THRESHOLD && (player.tickCount % FRACTURE_BURST_INTERVAL) == 0) {
            tickFractureState(player, level);
        }

        // ── Random debuff every 60 s ──
        int debuffTicks = data.getInt("FC_DebuffTicks") - 1;
        if (debuffTicks <= 0) {
            applyRandomDebuff(player);
            data.putInt("FC_DebuffTicks", DEBUFF_INTERVAL);
        } else {
            data.putInt("FC_DebuffTicks", debuffTicks);
        }
    }

    // ── Apply personality effects ─────────────────────────────

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();

        // Copy persistent data (Forge does this automatically for
        // getPersistentData(), but the mask item itself needs re-giving)
        if (!event.isWasDeath()) return;

        boolean fused = original.getPersistentData().getBoolean("FC_Fused");
        if (!fused) return;

        // Search original's inventory for the mask and re-give it
        original.getInventory().items.stream()
                .filter(s -> s.getItem() instanceof FracturedChorus)
                .findFirst()
                .ifPresent(mask -> {
                    // Put it in the head slot of the new player
                    newPlayer.getInventory().armor.set(3, mask.copy());
                });
    }


    private static void applyPersonalityEffects(Player player, Level level,
                                                int personality, int remaining) {
        if (personality == 0) {
            // ── SCHEMER ──
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,  40, 1, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION,    40, 0, false, false));

            // +4 attack damage via attribute
            var attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (attr != null && attr.getModifier(ATTACK_BONUS_ID) == null) {
                attr.addPermanentModifier(new AttributeModifier(
                        ATTACK_BONUS_ID, 24.0, AttributeModifier.Operation.ADD_VALUE));
            }

            // Enchant particles
            if (level instanceof ServerLevel sl && (player.tickCount % 10) == 0) {
                sl.sendParticles(ParticleTypes.ENCHANT,
                        player.getX(), player.getY() + 1.5, player.getZ(),
                        3, 0.3, 0.3, 0.3, 0.5);
            }

            if (remaining == PERSONALITY_DURATION) {
                player.displayClientMessage(
                        Component.literal("The Schemer awakens. Think carefully.")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC), true);
            }

        } else {
            // ── BERSERKER ──
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,      40, 2, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false));

            // Force attack nearest entity every 2 s
            if ((player.tickCount % 40) == 0) {
                LivingEntity nearest = level.getEntitiesOfClass(
                        LivingEntity.class,
                        player.getBoundingBox().inflate(6),
                        e -> e != player
                ).stream().min((a, b) ->
                        Double.compare(a.distanceToSqr(player), b.distanceToSqr(player))
                ).orElse(null);

                if (nearest != null) {
                    player.swing(InteractionHand.MAIN_HAND);
                    nearest.hurt(
                            level.damageSources().playerAttack(player),
                            (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE)
                    );
                }
            }

            // Soul fire particles
            if (level instanceof ServerLevel sl && (player.tickCount % 6) == 0) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        player.getX(), player.getY() + 1.5, player.getZ(),
                        2, 0.3, 0.3, 0.3, 0.02);
            }

            if (remaining == PERSONALITY_DURATION) {
                player.displayClientMessage(
                        Component.literal("The Berserker screams. You cannot resist.")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
            }
        }
    }

    // ── Force a personality shift ─────────────────────────────

    private static void forceShift(Player player, Level level) {
        var data = player.getPersistentData();

        // Remove previous attack bonus before switching
        removeAttackBonus(player);

        // Pick new personality — always swap
        int current = data.getInt("FC_Personality");
        int next    = (current == 0) ? 1 : 0;

        data.putInt("FC_Personality", next);
        data.putInt("FC_ActiveTicks", PERSONALITY_DURATION);

        // Randomise next shift window
        int nextShift = SHIFT_MIN + level.random.nextInt(SHIFT_MAX - SHIFT_MIN);
        data.putInt("FC_ShiftTicks", nextShift);

        // Increment shift counter
        int shifts = data.getInt("FC_TotalShifts") + 1;
        data.putInt("FC_TotalShifts", shifts);

        // Fracture warning
        if (shifts == FRACTURE_THRESHOLD) {
            player.sendSystemMessage(
                    Component.literal("The Chorus fractures. Both voices speak at once.")
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        }

        // Shift flash particles
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FLASH,
                    player.getX(), player.getY() + 1, player.getZ(),
                    1, 0, 0, 0, 0);
        }
    }

    // ── Fracture state: both personalities conflict ───────────

    private static void tickFractureState(Player player, Level level) {
        // Slam both sets of effects simultaneously — they conflict
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,  60, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,    60, 2, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION,       100, 1, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,        40, 0, false, true));

        player.displayClientMessage(
                Component.literal("They both scream.")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.OBFUSCATED)
                        .append(Component.literal(" They both scream.")
                                .withStyle(ChatFormatting.DARK_PURPLE)), true);

        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION,
                    player.getX(), player.getY() + 1, player.getZ(),
                    3, 0.5, 0.5, 0.5, 0.1);
        }
    }

    // ── Forbidden Knowledge: HP readout ──────────────────────

    private static void tickForbiddenKnowledge(Player player, ServerLevel sl) {
        sl.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(16),
                e -> e != player
        ).forEach(entity -> {
            // Approximate HP bucket: 1–10 scale
            float ratio   = entity.getHealth() / entity.getMaxHealth();
            int   bucket  = Math.max(1, Math.round(ratio * 10));
            // Spawn 'bucket' number of CRIT particles above head as a readout
            for (int i = 0; i < bucket; i++) {
                sl.sendParticles(ParticleTypes.CRIT,
                        entity.getX() + (Math.random() - 0.5) * 0.4,
                        entity.getY() + entity.getBbHeight() + 0.3 + (i * 0.15),
                        entity.getZ() + (Math.random() - 0.5) * 0.4,
                        1, 0, 0, 0, 0);
            }
        });
    }

    // ── Random debuff ─────────────────────────────────────────

    private static void applyRandomDebuff(Player player) {
        int roll = player.level().random.nextInt(3);
        switch (roll) {
            case 0 -> {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 200, 1, false, true));
                player.displayClientMessage(Component.literal("A thought slips away...")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
            case 1 -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1, false, true));
                player.displayClientMessage(Component.literal("Your legs feel distant.")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
            case 2 -> {
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, 2, false, true));
                player.displayClientMessage(Component.literal("The Chorus hungers through you.")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
        }
    }

    // ── Fused state debuffs (no longer wearing but still bound) ─

    private static void tickFusedDebuffs(Player player) {
        if ((player.tickCount % DEBUFF_INTERVAL) == 0) {
            applyRandomDebuff(player);
        }
        if ((player.tickCount % 200) == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, true));
            player.displayClientMessage(
                    Component.literal("The mask is not done with you.")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), true);
        }
    }

    // ── Death: fuse chance ────────────────────────────────────

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isWearing(player)) return;

        if (player.level().random.nextFloat() < FUSE_CHANCE) {
            player.getPersistentData().putBoolean("FC_Fused", true);
            player.getPersistentData().putFloat("FC_FusedDamage", 0f);

            // Prevent normal drop — item stays in head slot
            // (slot is cleared on death normally; we re-give it on respawn via clone event)
            player.sendSystemMessage(
                    Component.literal("The Chorus fuses to your face. It is not finished.")
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        }
    }

    // ── Fused damage tracking — drop after 20 damage taken ───

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        var data = player.getPersistentData();
        if (!data.getBoolean("FC_Fused")) return;
        if (isWearing(player)) return; // only track when not re-equipped

        float absorbed = data.getFloat("FC_FusedDamage") + event.getAmount();
        if (absorbed >= FUSED_DAMAGE_THRESHOLD) {
            // Forcibly drop the mask
            data.putBoolean("FC_Fused", false);
            data.putFloat("FC_FusedDamage", 0f);

            // Find mask in inventory and drop it
            for (ItemStack stack : player.getInventory().items) {
                if (stack.getItem() instanceof FracturedChorus) {
                    player.drop(stack.copy(), false);
                    stack.setCount(0);
                    break;
                }
            }

            player.displayClientMessage(
                    Component.literal("The Chorus releases its grip. For now.")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC), false);
        } else {
            data.putFloat("FC_FusedDamage", absorbed);
        }
    }

    // ── Cleanup ───────────────────────────────────────────────

    private static void removeAttackBonus(Player player) {
        var attr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attr != null) attr.removeModifier(ATTACK_BONUS_ID);
    }

    // ── Tooltip ───────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.literal("Sealed Artifact — Grade I")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Chaos / Madness Pathway")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Forbidden Knowledge:")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  See nearby entities' HP at all times")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Dual Resonance (shifts every 30–90 s):")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Schemer: Speed II + Night Vision + +4 damage")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  Berserker: Strength III + forced attacks")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Right-click (held): Force shift [60 s cooldown]")
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("⚠ Drawbacks:")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Random debuff every 60 s")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  After 10 shifts: Fracture State")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  On death: 40% chance mask fuses to face")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  (drops only after taking 20 damage)")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("\"Both of them remember wearing it.\"")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}