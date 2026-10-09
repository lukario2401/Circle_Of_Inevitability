package net.lukario.frogerealm.item.seald_artifacts;

import net.minecraft.ChatFormatting;
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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * ═══════════════════════════════════════════════════════════
 *   THE BONE DEFINER'S LENS  —  Sealed Artifact, Grade II
 * ═══════════════════════════════════════════════════════════
 *
 * Inspired by Lord of the Mysteries (Seer / Visionary pathway).
 *
 * ── ACTIVE  (right-click) ──────────────────────────────────
 *   Toggles SCRYING MODE.
 *   • All living entities within 20 blocks gain Glowing.
 *   • The nearest entity is MARKED — takes 3× damage from holder.
 *   • END_ROD particles orbit the player while active.
 *
 * ── DRAWBACK (escalates every second) ─────────────────────
 *   0–5 s   → Wither I
 *   5–10 s  → + Nausea II
 *   10+ s   → + Blindness + permanent max-HP loss (−2 hearts
 *              per 10 s, up to −8 hearts total).
 *   Passive → Sprint is ALWAYS blocked while Lens is in hand.
 *
 * ── STATE (stored on player.getPersistentData()) ───────────
 *   "BDL_Active"      boolean
 *   "BDL_ScryTicks"   int
 *   "BDL_MaxHPStacks" int
 */
@Mod.EventBusSubscriber
public class BoneDefinersLens extends Item {

    // ── Constants ────────────────────────────────────────────

    private static final double SCRY_RADIUS = 20.0;

    private static final ResourceLocation MAX_HP_MOD_ID =
            ResourceLocation.fromNamespaceAndPath("frogerealmmod", "bdl_max_hp_penalty");

    private static final double HP_DEBUFF_PER_STACK = 4.0; // 2 hearts per stack
    private static final int    MAX_HP_STACKS        = 4;  // −8 hearts max

    // ── Constructor ──────────────────────────────────────────

    public BoneDefinersLens(Properties properties) {
        super(properties);
    }

    // ── Active Use ───────────────────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        var data = player.getPersistentData();
        boolean active = data.getBoolean("BDL_Active");

        if (active) {
            deactivateLens(player);
        } else {
            data.putBoolean("BDL_Active", true);
            data.putInt("BDL_ScryTicks", 0);
            player.displayClientMessage(
                    Component.literal("The Lens opens. Reality thins.")
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC),
                    true
            );
        }

        return InteractionResultHolder.success(stack);
    }

    // ── Server Tick ──────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        Level  level  = player.level();
        if (level.isClientSide) return;

        if (findLens(player) == null) return;

        var data = player.getPersistentData();

        // ── Always block sprinting ──
        if (player.isSprinting()) {
            player.setSprinting(false);
            if ((player.tickCount % 40) == 0) {
                player.displayClientMessage(
                        Component.literal("The Lens demands stillness.")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                        true
                );
            }
        }

        if (!data.getBoolean("BDL_Active")) return;

        // ── Increment scry timer ──
        int scryTicks = data.getInt("BDL_ScryTicks") + 1;
        data.putInt("BDL_ScryTicks", scryTicks);
        int scrySeconds = scryTicks / 20;

        // ── Glowing + nearest-target marking ──
        List<LivingEntity> nearby = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(SCRY_RADIUS),
                e -> e != player
        );

        LivingEntity nearest    = null;
        double       nearestDist = Double.MAX_VALUE;

        for (LivingEntity entity : nearby) {
            entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
            entity.getPersistentData().remove("BDL_Marked");

            double dist = entity.distanceToSqr(player);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest     = entity;
            }
        }

        if (nearest != null) {
            nearest.getPersistentData().putBoolean("BDL_Marked", true);
        }

        // ── Orbiting particles ──
        if (level instanceof ServerLevel serverLevel && (scryTicks % 5) == 0) {
            for (int i = 0; i < 6; i++) {
                double angle = (Math.PI * 2.0 / 6) * i + (scryTicks * 0.1);
                serverLevel.sendParticles(
                        ParticleTypes.END_ROD,
                        player.getX() + Math.cos(angle) * 0.8,
                        player.getY() + 1.0,
                        player.getZ() + Math.sin(angle) * 0.8,
                        1, 0, 0, 0, 0.01
                );
            }
        }

        // ── Drawbacks (once per second) ──
        if ((scryTicks % 20) != 0) return;

        // Tier 1 — Wither I
        player.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0, false, false));

        // Tier 2 — Nausea II after 5 s
        if (scrySeconds >= 5) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 1, false, false));
        }

        // Tier 3 — Blindness + max-HP loss after 10 s
        if (scrySeconds >= 10) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));

            int stacks = data.getInt("BDL_MaxHPStacks");
            if (stacks < MAX_HP_STACKS && (scrySeconds % 10) == 0) {
                int newStacks = stacks + 1;
                applyMaxHPDebuff(player, newStacks);
                data.putInt("BDL_MaxHPStacks", newStacks);

                player.displayClientMessage(
                        Component.literal("Your mind fractures. The Lens consumes.")
                                .withStyle(ChatFormatting.DARK_RED),
                        true
                );
            }
        }
    }

    // ── 3× Damage on Marked target ───────────────────────────

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (findLens(attacker) == null) return;

        LivingEntity target = event.getEntity();
        if (!target.getPersistentData().getBoolean("BDL_Marked")) return;

        event.setAmount(event.getAmount() * 3.0f);
        target.getPersistentData().remove("BDL_Marked"); // one-shot mark
    }

    // ── Deactivation ─────────────────────────────────────────

    public static void deactivateLens(Player player) {
        var data = player.getPersistentData();
        data.putBoolean("BDL_Active", false);
        data.putInt("BDL_ScryTicks", 0);

        player.level().getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(SCRY_RADIUS),
                e -> e != player
        ).forEach(e -> e.getPersistentData().remove("BDL_Marked"));

        player.displayClientMessage(
                Component.literal("The Lens closes. The world feels wrong.")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                true
        );
    }

    // ── Max-HP attribute modifier ─────────────────────────────

    private static void applyMaxHPDebuff(Player player, int stackCount) {
        var attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;

        attr.removeModifier(MAX_HP_MOD_ID);
        attr.addPermanentModifier(new AttributeModifier(
                MAX_HP_MOD_ID,
                -HP_DEBUFF_PER_STACK * stackCount,
                AttributeModifier.Operation.ADD_VALUE
        ));

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    /**
     * Purifies all max-HP stacks.
     * Call this on Milk drink, player death, purification ritual, etc.
     */
    public static void purifyMaxHPDebuff(Player player, ItemStack stack) {
        var attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) attr.removeModifier(MAX_HP_MOD_ID);

        player.getPersistentData().putInt("BDL_MaxHPStacks", 0);
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // ── Utility ──────────────────────────────────────────────

    private static ItemStack findLens(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof BoneDefinersLens) return stack;
        }
        return null;
    }

//    @SubscribeEvent
//    public static void onLivingHurt(LivingHurtEvent event) {
//        if (event.getSource().getEntity() instanceof Player attacker) {
//            BoneDefinersLens.tryAmplifyMarkedDamage(event, attacker);
//        }
//    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Find lens in inventory and wipe the HP-debuff stacks on death
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof BoneDefinersLens) {
                BoneDefinersLens.purifyMaxHPDebuff(player, stack);
            }
        }
    }

    // ── Tooltip ───────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.literal("Sealed Artifact — Grade II")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Right-click: Toggle Scrying Mode")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  Reveals all entities within 20 blocks")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  Marks nearest target for 3× damage")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Drawbacks while active:")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  Wither I — always")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  Nausea II — after 5 seconds")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  Blindness + Max HP loss — after 10 s")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Blocks sprinting while held.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}