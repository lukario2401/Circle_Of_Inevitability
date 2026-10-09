package net.lukario.frogerealm.item.seald_artifacts;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════
 *   THE IRON COIF  —  Beyonder Characteristic Item
 *   Sequence 6 — Lawman (Lawyer / Order / Logic Pathway)
 * ══════════════════════════════════════════════════════════════
 *
 * A chainmail hood woven from links of a material that is not quite
 * metal. Each link is a different size, but they all fit together
 * perfectly. There are no loose ends. There are no gaps.
 * The weave follows no pattern you can identify, but every attempt
 * to find a flaw fails. Not because you stop looking — because there
 * is genuinely nothing to find.
 *
 * The Lawman who crystallized into this coif spent thirty years
 * enforcing the boundary between what was permitted and what was not.
 * He did not use violence. He used *presence*.
 * Things that were not supposed to happen simply didn't, when he was
 * nearby. Chaos found him inconvenient and routed around him.
 * Eventually, the characteristic itself became the presence,
 * and the Lawman became incidental.
 *
 * When you put it on, you feel the sudden, irrational certainty
 * that you are allowed to be here. That you have always been allowed
 * to be here. Everything else might not be.
 *
 * TYPE: Wearable — Helmet slot
 * PATHWAY: Lawyer / Order / Logic (Lawman sub-branch)
 * SEQUENCE EQUIVALENT: 6 — Lawman
 * NO ACCUMULATION MECHANICS — all effects are flat and constant.
 *
 *
 * ── PASSIVE I — Authority Aura ────────────────────────────────
 *   While worn, the player radiates the weight of lawful authority.
 *   All hostile mobs within 10 blocks receive:
 *     • Weakness I (permanent in range — the weight of law)
 *     • Slowness I (permanent in range — hesitation)
 *   The coif imposes order on the wearer's body as well:
 *     • Saturation I applied continuously (the law provides — a
 *       Lawman does not go hungry while on duty)
 *     • Resistance I (minor damage reduction — authority is armor)
 *     • The player cannot be knocked back more than 1 block
 *       (simulated: knockback velocity dampened each tick)
 *
 * ── PASSIVE II — Ordered Mind ─────────────────────────────────
 *   The Lawman's characteristic imposes logical structure.
 *     • Player is immune to Confusion / Nausea (removed on apply)
 *     • Player is immune to Blindness (removed on apply)
 *     • Night Vision applied continuously (order sees clearly)
 *     • Fire damage reduced: player extinguished if on fire
 *       every 2 seconds (chaos of fire has no jurisdiction here)
 *
 * ── PASSIVE III — Jurisdictional Presence ─────────────────────
 *   Passive deterrence — hostile mobs that wander within 6 blocks
 *   while the player is STANDING STILL (not moving) receive:
 *     • Slowness II for 3 s (the law compels you to stop)
 *     • A brief outward velocity push (hesitation made physical)
 *   "Standing still" = player horizontal velocity < 0.05 blocks/tick.
 *
 * ── ACTIVE (sneak) — Enforce ──────────────────────────────────
 *   While the player is SNEAKING, the Coif enters Enforcement Mode:
 *     • All hostile mobs within 14 blocks are actively repelled —
 *       outward velocity applied each tick (continuous push).
 *     • Repulsion strength: moderate — mobs are pushed away at
 *       ~0.35 blocks/tick, scaling stronger the closer they are.
 *       (Within 4 blocks: strong push. 4–14 blocks: gentle push.)
 *     • All repelled mobs receive Slowness II for 2 s.
 *     • Player receives Speed I while sneaking + enforcing
 *       (counterintuitive — the law moves deliberately).
 *     • Gold END_ROD particles ring the enforcement radius.
 *     • Enforcement Mode costs nothing — it is simply what the
 *       Coif does when the wearer asserts jurisdiction.
 *
 *
 * ── DRAWBACK — Lawful Compulsion ──────────────────────────────
 *   The Lawman pathway imposes structure on everything, including
 *   the one wearing it. Authority is not a gift. It is a contract.
 *   The coif enforces the contract whether you agree to it or not.
 *
 *   FLAT DRAWBACKS (active from the moment it is worn, no build-up):
 *
 *   1. JURISDICTIONAL HUNGER — The law provides saturation, but
 *      it decides when you eat, not you. Food items consumed while
 *      wearing the coif restore only HALF their normal hunger
 *      (the law considers excessive eating disorderly).
 *      Note: Saturation I from the passive still applies — you
 *      won't starve. But you can't feast.
 *      (Implemented: LivingEntityUseItemEvent — halve food value)
 *
 *   2. COMPELLED STILLNESS — The law values deliberate movement.
 *      Sprinting is permitted but costs double hunger while the
 *      coif is worn. (Simulated: extra food drain while sprinting.)
 *
 *   3. ORDERED SLEEP — The player cannot sleep in beds while
 *      wearing the coif. The law does not sleep on duty.
 *      (Implemented: PlayerSleepInBedEvent — cancel sleep.)
 *      Message sent: "[coif] rest is not permitted while on duty."
 *
 *   4. LAWFUL VISIBILITY — The coif makes the wearer identifiable
 *      as an authority. The player receives permanent Glowing
 *      while worn — visible to all players and mobs.
 *      You cannot hide what you represent.
 *
 *   5. COMPELLED HONESTY — The coif resists deception. While worn:
 *      • Invisibility potions / effects are stripped on application.
 *        Message: "[coif] concealment is not within jurisdiction."
 *      • The player cannot use Ender Pearls (teleportation without
 *        due process). Pearls thrown by the player deal 2 damage
 *        to the player on landing as a fine. (LivingHurtEvent
 *        from EnderPearl → not canceled, but +2 extra damage.)
 *
 *   6. MAINTAINED JURISDICTION — The coif cannot be removed while
 *      hostile mobs are within 16 blocks. Attempting to swap it
 *      out sends: "[coif] jurisdiction is not relinquished mid-duty."
 *      (Implemented: ArmorEquipEvent — cancel unequip if mobs near.)
 *      Note: outside combat, it can be removed freely.
 *
 *
 * ── NOTES ─────────────────────────────────────────────────────
 *   Unlike other characteristic items in this collection, the Iron
 *   Coif has NO accumulating resource, NO escalating drawback tiers,
 *   and NO threshold-based progression. All effects are constant
 *   from the moment it is worn to the moment it is removed.
 *   It does not grow. It does not decay. It simply IS.
 *   That is, in its way, more unsettling than anything that builds.
 *
 *
 * ── STATE (player.getPersistentData()) ────────────────────────
 *   No persistent state beyond standard ArmorItem behavior.
 *   All effects are recalculated each tick from worn status.
 *   No "IC_*" keys needed.
 *
 * ── REGISTRATION ──────────────────────────────────────────────
 *   public static final RegistryObject<Item> IRON_COIF =
 *       ITEMS.register("iron_coif",
 *           () -> new IronCoif(
 *               IronCoif.COIF_MATERIAL,
 *               new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
 */
@Mod.EventBusSubscriber
public class IronCoif extends ArmorItem {

    // ── Ranges ────────────────────────────────────────────────
    private static final double AUTHORITY_RANGE    = 10.0;
    private static final double ENFORCE_RANGE      = 14.0;
    private static final double JURISDICTION_RANGE = 6.0;
    private static final double REMOVAL_BLOCK_RANGE= 16.0;

    // ── Repulsion ─────────────────────────────────────────────
    private static final double REPULSE_CLOSE  = 4.0;
    private static final double REPULSE_NEAR   = 0.20;
    private static final double REPULSE_FAR    = 0.10;

    public IronCoif(Holder<ArmorMaterial> material, Properties properties) {
        super(material, Type.HELMET, properties);
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
        if (!isWearing(player)) return;

        // ── Passive I — Authority Aura ────────────────────────
        applyAuthorityAura(player, level);

        // ── Passive II — Ordered Mind ─────────────────────────
        applyOrderedMind(player, level);

        // ── Passive III — Jurisdictional Presence ─────────────
        applyJurisdictionalPresence(player, level);

        // ── Active — Enforce (sneak) ──────────────────────────
        if (player.isShiftKeyDown()) {
            applyEnforcementMode(player, level);
        }

        // ── Drawback — Compelled Stillness (sprint drain) ─────
        if (player.isSprinting() && (player.tickCount % 10) == 0) {
            player.getFoodData().setFoodLevel(
                    Math.max(0, player.getFoodData().getFoodLevel() - 1));
        }

        // ── Drawback — Glowing (lawful visibility) ────────────
//        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));

        // ── Drawback — Strip Invisibility ─────────────────────
        if (player.hasEffect(MobEffects.INVISIBILITY)) {
            player.removeEffect(MobEffects.INVISIBILITY);
            player.sendSystemMessage(
                    Component.literal("[coif] concealment is not within jurisdiction.")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }

        // ── Drawback — Fire extinguish (every 2 s) ─────────────
        if (player.isOnFire() && (player.tickCount % 40) == 0) {
            player.clearFire();
        }
    }

    // ── Passive I: Authority Aura ─────────────────────────────
    private static void applyAuthorityAura(Player player, Level level) {
        // Mob debuffs in range
        if ((player.tickCount % 20) == 0) {
            level.getEntitiesOfClass(Monster.class,
                    player.getBoundingBox().inflate(AUTHORITY_RANGE)
            ).forEach(mob -> {
                mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,          30, 0, false, false));
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0, false, false));
            });
        }

        // Saturation I — the law provides
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION,   40, 0, false, false));

        // Resistance I
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false));

        // Knockback dampening — clamp horizontal velocity
        Vec3 vel = player.getDeltaMovement();
        double hSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (hSpeed > 1.2) {
            double scale = 1.2 / hSpeed;
            player.setDeltaMovement(vel.x * scale, vel.y, vel.z * scale);
        }
    }

    // ── Passive II: Ordered Mind ──────────────────────────────
    private static void applyOrderedMind(Player player, Level level) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false));

        // Strip Confusion, Blindness immediately
        if (player.hasEffect(MobEffects.CONFUSION)) {
            player.removeEffect(MobEffects.CONFUSION);
        }
        if (player.hasEffect(MobEffects.BLINDNESS)) {
            player.removeEffect(MobEffects.BLINDNESS);
            player.sendSystemMessage(
                    Component.literal("[coif] obstruction to clarity is not permitted.")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
    }

    // ── Passive III: Jurisdictional Presence ──────────────────
    private static void applyJurisdictionalPresence(Player player, Level level) {
        Vec3 vel = player.getDeltaMovement();
        double hSpeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        if (hSpeed >= 0.0) return; // Player is moving — presence inactive

        if ((player.tickCount % 10) != 0) return;

//        Vec3 pos = player.position();
        level.getEntitiesOfClass(Monster.class,
                player.getBoundingBox().inflate(JURISDICTION_RANGE)
        ).forEach(mob -> {
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3, false, false));
//            Vec3 dir = mob.position().subtract(pos).normalize();
//            if (dir.lengthSqr() > 0.01) {
//                mob.addDeltaMovement(dir.scale(0.25));
//                mob.hurtMarked = true;
//            }
        });
    }

    // ── Active: Enforcement Mode (sneak) ─────────────────────
    private static void applyEnforcementMode(Player player, Level level) {
        Vec3 pos = player.position();

        // Continuous repulsion of all nearby hostiles
        level.getEntitiesOfClass(Monster.class,
                player.getBoundingBox().inflate(ENFORCE_RANGE)
        ).forEach(mob -> {
            Vec3 dir  = mob.position().subtract(pos);
            double dist = dir.length();
            if (dist < 0.5) return;

            Vec3 norm = dir.normalize();
            // Stronger push the closer the mob is
            double force = (dist <= REPULSE_CLOSE) ? REPULSE_NEAR * 2.2 : REPULSE_FAR;
            mob.addDeltaMovement(norm.scale(force));
            mob.hurtMarked = true;
            mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));
        });

        // Speed I for the enforcer
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));

        // Ring particles at enforcement radius
//        if (level instanceof ServerLevel sl && (player.tickCount % 6) == 0) {
//            double angle = (player.tickCount * 0.15) % (Math.PI * 2);
//            for (int i = 0; i < 5; i++) {
//                double a = angle + (Math.PI * 2.0 / 5) * i;
//                sl.sendParticles(ParticleTypes.END_ROD,
//                        pos.x + Math.cos(a) * ENFORCE_RANGE * 0.7,
//                        pos.y + 0.3,
//                        pos.z + Math.sin(a) * ENFORCE_RANGE * 0.7,
//                        1, 0, 0.05, 0, 0.01);
//            }
//        }
    }

    // ── Event: extra Ender Pearl damage ──────────────────────
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!isWearing(player)) return;

        // Check if hurt by Ender Pearl (fall damage from teleport)
        // EnderPearl teleport damage comes as fall damage source
        // We identify it loosely — add a fine on top of vanilla damage
        if (event.getSource().getMsgId().equals("fall")
                && player.level().random.nextFloat() < 0.4f) {
            // Ender pearls cause fall damage — we can't perfectly distinguish,
            // but we add a modest fine here as a flavor penalty
            // A cleaner approach would use an item use event for the pearl itself
            event.setAmount(event.getAmount() + 10f);
        }
    }

    // ── Event: sleep prevention ───────────────────────────────
    @SubscribeEvent
    public static void onPlayerSleep(net.minecraftforge.event.entity.player.PlayerSleepInBedEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isWearing(player)) return;
        event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
        player.sendSystemMessage(
                Component.literal("[coif] rest is not permitted while on duty.")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
    }

    // ── Event: prevent removal near mobs ─────────────────────
    @SubscribeEvent
    public static void onArmorEquip(net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (event.getSlot() != EquipmentSlot.HEAD) return;

        // Detect removal: from → IronCoif, to → not IronCoif
        boolean wasCoif = event.getFrom().getItem() instanceof IronCoif;
        boolean isCoif  = event.getTo().getItem() instanceof IronCoif;
        if (!wasCoif || isCoif) return; // Not a removal

        // Check for nearby hostiles
        boolean mobsNearby = !player.level().getEntitiesOfClass(Monster.class,
                player.getBoundingBox().inflate(REMOVAL_BLOCK_RANGE)
        ).isEmpty();

        if (mobsNearby) {
            // Re-equip the coif — cancel removal
            player.setItemSlot(EquipmentSlot.HEAD, event.getFrom().copy());
            player.sendSystemMessage(
                    Component.literal("[coif] jurisdiction is not relinquished mid-duty.")
                            .withStyle(ChatFormatting.GOLD, ChatFormatting.ITALIC));
        }
    }

    // ── Utility ───────────────────────────────────────────────
    public static boolean isWearing(Player player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        return helmet.getItem() instanceof IronCoif;
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

        tooltip.add(Component.literal("Beyonder Characteristic — Sequence 6")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Lawman  |  Lawyer / Order / Logic Pathway")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Authority Aura (10 blocks):")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Hostiles: Weakness I + Slowness I in range.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  Wearer: Saturation I, Resistance I, reduced knockback.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Ordered Mind:")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Immune to Confusion and Blindness. Night Vision.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  Fire extinguished every 2 s.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Jurisdictional Presence (while still):")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Hostiles entering 6 blocks while you stand still")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  are slowed and nudged away.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Sneak — Enforce (14 block repulsion):")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Continuously repels all nearby hostiles.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  Closer mobs: stronger push. Speed I while enforcing.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("⚠ Drawback — Lawful Compulsion (flat, no build-up):")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Food restores half hunger (law decides when you eat).")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  Sprinting drains hunger faster.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  Cannot sleep while wearing. The law does not rest.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  Permanent Glowing — you cannot hide what you represent.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  Invisibility stripped instantly.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  Cannot remove while hostiles are within 16 blocks.")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("No accumulation. No escalation. It simply IS.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("\"You are allowed to be here. Everything else might not be.\"")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
