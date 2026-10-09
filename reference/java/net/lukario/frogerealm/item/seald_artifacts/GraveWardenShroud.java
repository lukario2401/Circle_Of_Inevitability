package net.lukario.frogerealm.item.seald_artifacts;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════
 *   THE GRAVEWARDEN'S SHROUD  —  Sealed Artifact, Grade I
 * ══════════════════════════════════════════════════════════════
 *
 * Inspired by Lord of the Mysteries (Death / Undead pathway).
 * Stitched from the burial wrappings of seventeen Beyonders who
 * perished mid-sequence-advancement. Their incomplete
 * characteristics are woven into the fabric — hungry, purposeless,
 * and slowly consuming whoever dares wear it.
 *
 * ── PASSIVE (off-hand) ────────────────────────────────────────
 *   Undeath Aura:
 *   • Any living entity (except the player) that dies within
 *     12 blocks is reanimated as a Shadow Thrall (Wither Skeleton)
 *     tagged with "GWS_Thrall" + owner UUID.
 *   • Thralls attack all non-owner entities.
 *   • Maximum 6 thralls active at once.
 *
 * ── ACTIVE (right-click) ──────────────────────────────────────
 *   Death's Embrace:
 *   • Costs 40% of CURRENT HP (minimum 1 HP remaining).
 *   • Fully heals all thralls.
 *   • Grants thralls Strength II (10 s).
 *   • Applies Weakness II + Slowness II (5 s) to all enemies
 *     within 15 blocks.
 *   • 8-second cooldown.
 *
 * ── DRAWBACK (Grade I — potentially lethal) ───────────────────
 *   Every 30 seconds the Shroud is held in either hand:
 *   • Permanently removes 1 HP (0.5 heart) from MAX_HEALTH
 *     via attribute modifier (ResourceLocation key, no cap).
 *     If max HP reaches 2.0 (one heart), drain stops to prevent
 *     instant death — but the Shroud will keep trying to kill you
 *     another way.
 *   • Below 4 hearts max HP: random Wither I bursts (every ~15 s)
 *     even without actively using the item.
 *   • On player death while holding:
 *     - 40% chance: spawns a hostile Wither Skeleton at death
 *       coords carrying the Shroud (must be re-killed to reclaim).
 *     - 60% chance: Shroud simply drops as normal.
 *
 * ── STATE (player.getPersistentData()) ────────────────────────
 *   "GWS_DrainTicks"   int  — ticks since last max-HP drain
 *   "GWS_ThrallCount"  int  — current active thrall count
 *
 * ── REGISTRATION ──────────────────────────────────────────────
 *   public static final RegistryObject<Item> GRAVEWARDEN_SHROUD =
 *       ITEMS.register("gravewarden_shroud",
 *           () -> new GravewardenShroud(
 *               new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
 */
@Mod.EventBusSubscriber
public class GraveWardenShroud extends Item {

    // ─── Range attribute helpers ──────────────────────────────────────────────

    private static final ResourceLocation FRENZY_BLOCK_RANGE_ID  =
            ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "frenzy_block_range");

    private static final ResourceLocation FRENZY_ENTITY_RANGE_ID =
            ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "frenzy_entity_range");

    private static void applyRangeBoost(Player player) {
        var blockRange  = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        var entityRange = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (blockRange != null && blockRange.getModifier(FRENZY_BLOCK_RANGE_ID) == null) {
            blockRange.addTransientModifier(new AttributeModifier(
                    FRENZY_BLOCK_RANGE_ID, 2.0, AttributeModifier.Operation.ADD_VALUE));
        }
        if (entityRange != null && entityRange.getModifier(FRENZY_ENTITY_RANGE_ID) == null) {
            entityRange.addTransientModifier(new AttributeModifier(
                    FRENZY_ENTITY_RANGE_ID, 2.0, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void removeRangeBoost(Player player) {
        var blockRange  = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        var entityRange = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (blockRange  != null) blockRange.removeModifier(FRENZY_BLOCK_RANGE_ID);
        if (entityRange != null) entityRange.removeModifier(FRENZY_ENTITY_RANGE_ID);
    }

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        @SubscribeEvent
        public static void onLivingTick(LivingEvent.LivingTickEvent event) {
            // 1. Get the entity
            LivingEntity entity = event.getEntity();
            Level level = entity.level();

            // 2. Optimization: Only run logic on the server side (usually)
            if (level.isClientSide) return;

            // 3. Filter: Only run for a specific type (e.g., Wither Skeletons)
            if (entity instanceof Player player) {
                if (entity.tickCount % 20 == 0) {
                    double gwsAbilityState = player.getPersistentData().getDouble("GWS_AbilityDuration");
                    if (gwsAbilityState>0){
                    player.getPersistentData().putDouble("GWS_AbilityDuration",gwsAbilityState-1);
                    } else if (gwsAbilityState==0) {
                        removeRangeBoost(player);
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onLivingHurt(LivingHurtEvent event) {
            LivingEntity target = event.getEntity();
            LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity le ? le : null;
            if (attacker == null) return;

            // Prevent thralls hurting their owner
            if (target instanceof Player owner && GraveWardenShroud.isThrall(attacker, owner)) {
                event.setCanceled(true);
                return;
            }

            // Prevent thralls hurting other thralls of the same owner
            // (find any nearby player who owns the attacker-thrall)
            if (attacker.getPersistentData().getBoolean("GWS_Thrall")) {
                String ownerUUID = attacker.getPersistentData().getString("GWS_Owner");
                if (target.getPersistentData().getBoolean("GWS_Thrall")
                        && target.getPersistentData().getString("GWS_Owner").equals(ownerUUID)) {
                    event.setCanceled(true);
                }
            }
        }

        @SubscribeEvent
        public static void onItemFinishedUse(LivingEntityUseItemEvent.Finish event) {
            if (!(event.getEntity() instanceof Player player)) return;
            if (!event.getItem().is(Items.MILK_BUCKET)) return;

            GraveWardenShroud.purifyDrain(player);
            player.sendSystemMessage(
                    Component.literal("The corruption recedes... but the dead remember you.")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
            );
        }

        @SubscribeEvent
        public static void onLivingTarget(LivingChangeTargetEvent event) {
            // Check if the thing trying to attack is a mob
            if (event.getEntity() instanceof Mob mob) {
                // Check if the target is a player
                if (event.getNewTarget() instanceof Player player) {
                    // Check if the player is holding the Shroud
                    if (findShroud(player) != null) {
                        // Cancel the event so the mob forgets the player exists
                        event.setCanceled(true);
                    }
                }
            }
        }
    }
    // ── Constants ─────────────────────────────────────────────

    private static final double AURA_RADIUS        = 12.0;
    private static final double FEAR_PULSE_RADIUS  = 16.0;
    private static final int    MAX_THRALLS        = 12;
    private static final int    DRAIN_INTERVAL     = 1200;  // 30 s in ticks
    private static final double MIN_MAX_HP         = 1;  // 1 heart floor
    private static final double HP_DRAIN_AMOUNT    = 1.0;  // 0.5 heart per tick
    private static final int    ACTIVE_COOLDOWN    = 160;  // 8 s
    private static final float  SOUL_CAPTURE_CHANCE = 0.60f;

    private static final ResourceLocation MAX_HP_MOD_ID =
            ResourceLocation.fromNamespaceAndPath("frogerealm", "gws_max_hp_drain");

    // ── Constructor ───────────────────────────────────────────

    public GraveWardenShroud(Properties properties) {
        super(properties);
    }

    // ── Active: Death's Embrace ───────────────────────────────

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Forces the item to always render with the enchanted glow
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);

        boolean gwsAbilityState = player.getPersistentData().getBoolean("GWS_IsAbilityActive");
        if (!gwsAbilityState){
            player.getPersistentData().putBoolean("GWS_IsAbilityActive",true);
            player.getPersistentData().putDouble("GWS_AbilityDuration",6);
            applyRangeBoost(player);
        }

        // ── HP cost: 40% of current HP, leave at least 1 HP ──
        float cost = player.getHealth() * 0.40f;
        float remaining = player.getHealth() - cost;
        if (remaining < 1.0f) remaining = 1.0f;
        player.setHealth(remaining);

        // ── Heal + buff all owned thralls ──
        int healed = 0;
        for (LivingEntity entity : level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(64),
                e -> isThrall(e, player))) {

            entity.setHealth(entity.getMaxHealth());
            entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 1, false, true));
            healed++;
        }

        // ── Fear pulse on enemies ──
        level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(FEAR_PULSE_RADIUS),
                e -> e != player && !isThrall(e, player)
        ).forEach(e -> {
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,  100, 1, false, true));
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, true));
        });

        // ── Dark particle burst ──
        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 24; i++) {
                double angle = (Math.PI * 2.0 / 24) * i;
                serverLevel.sendParticles(ParticleTypes.SOUL,
                        player.getX() + Math.cos(angle) * 2.0,
                        player.getY() + 1.0,
                        player.getZ() + Math.sin(angle) * 2.0,
                        1, 0, 0.1, 0, 0.05);
            }
        }

        player.displayClientMessage(
                Component.literal("The dead answer your call.")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD),
                true
        );

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
        if (findShroud(player) == null) return;

        var data = player.getPersistentData();

        // ── Count live thralls ──
        long liveThralls = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(128),
                e -> isThrall(e, player)
        ).size();
        data.putInt("GWS_ThrallCount", (int) liveThralls);

        // ── Aura: reanimate nearby deaths ──
        // (handled in LivingDeathEvent below — but we pulse SOUL particles here)
        if (level instanceof ServerLevel serverLevel && (player.tickCount % 20) == 0) {
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX(), player.getY() + 0.1, player.getZ(),
                    3, 0.5, 0.0, 0.5, 0.02);
        }

        // ── Passive wither below 4 hearts max HP ──
        if (player.getMaxHealth() < 8.0 && (player.tickCount % 300) == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, false, false));
            if ((player.tickCount % 600) == 0) {
                player.displayClientMessage(
                        Component.literal("The Shroud gnaws at your soul.")
                                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC),
                        true
                );
            }
        }

        // ── Max-HP drain every 30 s ──
        int drainTicks = data.getInt("GWS_DrainTicks") + 1;
        data.putInt("GWS_DrainTicks", drainTicks);

        if (drainTicks >= DRAIN_INTERVAL) {
            data.putInt("GWS_DrainTicks", 0);
            drainMaxHP(player);
        }
    }

    // ── Reanimate on nearby death ─────────────────────────────

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        Level level = dead.level();
        if (level.isClientSide) return;

        // ── Check if a Shroud-holder is nearby ──
        List<Player> holders = level.getEntitiesOfClass(
                Player.class,
                dead.getBoundingBox().inflate(AURA_RADIUS),
                p -> findShroud(p) != null
        );

        if (holders.isEmpty()) return;
        Player owner = holders.get(0); // nearest holder becomes owner

        // Don't reanimate thralls or the player themselves
        if (dead == owner) return;
        if (isThrall(dead, owner)) return;

        // Check thrall cap
        var data = owner.getPersistentData();
        int thrallCount = data.getInt("GWS_ThrallCount");
        if (thrallCount >= MAX_THRALLS) return;

        // ── Spawn Wither Skeleton thrall at death position ──
        if (level instanceof ServerLevel serverLevel) {
            WitherSkeleton thrall = EntityType.WITHER_SKELETON.create(serverLevel);
            if (thrall == null) return;

            thrall.moveTo(dead.getX(), dead.getY(), dead.getZ(), dead.getYRot(), 0);

            // Tag with owner UUID so we can identify this thrall
            thrall.getPersistentData().putString("GWS_Owner", owner.getStringUUID());
            thrall.getPersistentData().putBoolean("GWS_Thrall", true);

            // Make it powerful — it's a grade I artifact after all
            thrall.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,   Integer.MAX_VALUE, 1, false, false));
            thrall.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));

            serverLevel.addFreshEntity(thrall);
            data.putInt("GWS_ThrallCount", thrallCount + 1);

            // Soul particle burst at resurrection point
            serverLevel.sendParticles(ParticleTypes.SOUL,
                    dead.getX(), dead.getY() + 1, dead.getZ(),
                    20, 0.3, 0.5, 0.3, 0.05);
        }

        // ── Soul-capture on PLAYER death ──
        if (dead instanceof Player deadPlayer) {
            ItemStack shroud = findShroud(deadPlayer);
            if (shroud == null) return;

            if (deadPlayer.level().random.nextFloat() < SOUL_CAPTURE_CHANCE) {
                // Spawn a hostile Wither Skeleton holding the Shroud at death coords
                if (level instanceof ServerLevel serverLevel) {
                    WitherSkeleton guardian = EntityType.WITHER_SKELETON.create(serverLevel);
                    if (guardian != null) {
                        guardian.moveTo(deadPlayer.getX(), deadPlayer.getY(), deadPlayer.getZ(),
                                deadPlayer.getYRot(), 0);
                        guardian.getPersistentData().putBoolean("GWS_SoulGuardian", true);

                        // Give it the Shroud to drop
                        guardian.setItemInHand(InteractionHand.OFF_HAND, shroud.copy());
                        guardian.addEffect(new MobEffectInstance(
                                MobEffects.DAMAGE_BOOST, Integer.MAX_VALUE, 3, false, false));
                        guardian.addEffect(new MobEffectInstance(
                                MobEffects.DAMAGE_RESISTANCE, Integer.MAX_VALUE, 2, false, false));

                        serverLevel.addFreshEntity(guardian);

                        // Remove the Shroud from drop so it only exists on the guardian
                        shroud.setCount(0);

                        deadPlayer.sendSystemMessage(
                                Component.literal("Your soul is bound. The Shroud guards its prize.")
                                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
                        );
                    }
                }
            }

            // Reset drain timer on death (small mercy)
            deadPlayer.getPersistentData().putInt("GWS_DrainTicks", 0);
        }
    }

    // ── Max-HP drain logic ────────────────────────────────────

    private static void drainMaxHP(Player player) {
        var attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;

        // Calculate new total penalty
        AttributeModifier existing = attr.getModifier(MAX_HP_MOD_ID);
        double currentPenalty = (existing != null) ? existing.amount() : 0.0;
        double newPenalty = currentPenalty - HP_DRAIN_AMOUNT;

        // Floor: never reduce max HP below MIN_MAX_HP
        double baseMax = attr.getBaseValue(); // default 20.0
        if (baseMax + newPenalty < MIN_MAX_HP) {
            newPenalty = MIN_MAX_HP - baseMax;
        }

        attr.removeModifier(MAX_HP_MOD_ID);
        attr.addPermanentModifier(new AttributeModifier(
                MAX_HP_MOD_ID,
                newPenalty,
                AttributeModifier.Operation.ADD_VALUE
        ));

        // Clamp current HP
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }

        // Warning messages at thresholds
        double maxHp = player.getMaxHealth();
        if (maxHp <= 4.0) {
            player.sendSystemMessage(
                    Component.literal("⚠ The Shroud has nearly consumed your life force.")
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
            );
        } else if (maxHp <= 8.0) {
            player.sendSystemMessage(
                    Component.literal("The Shroud drinks deeper. Your body wastes.")
                            .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC)
            );
        } else {
            player.sendSystemMessage(
                    Component.literal("The Shroud claims its tithe.")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
            );
        }
    }

    /**
     * Fully restores max HP lost to the Shroud.
     * Call from a ritual/purification item, or on a specific condition.
     * NOTE: Does NOT remove thralls — those persist until killed.
     */
    public static void purifyDrain(Player player) {
        var attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) attr.removeModifier(MAX_HP_MOD_ID);
        player.getPersistentData().putInt("GWS_DrainTicks", 0);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private static ItemStack findShroud(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof GraveWardenShroud) return stack;
        }
        return null;
    }

    /**
     * Returns true if the entity is a thrall belonging to the given player.
     */
    public static boolean isThrall(LivingEntity entity, Player owner) {
        var data = entity.getPersistentData();
        return data.getBoolean("GWS_Thrall")
                && data.getString("GWS_Owner").equals(owner.getStringUUID());
    }

    // ── Tooltip ───────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.literal("Sealed Artifact — Grade I")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Death / Undead Pathway")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Undeath Aura (12 blocks):")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Reanimates nearby dead as Shadow Thralls")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  Max 6 thralls at once")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Right-click — Death's Embrace:")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Cost: 40% of current HP")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  Heals + buffs all thralls (Strength II)")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  Fear pulse: Weakness + Slowness nearby")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("⚠ Drawbacks:")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  −0.5 heart max HP every 30 seconds (permanent)")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  Below 4 hearts: random Wither bursts")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  On death: 40% chance soul is captured")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  (Shroud guarded by powerful undead)")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}