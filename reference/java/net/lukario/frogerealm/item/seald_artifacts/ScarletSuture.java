package net.lukario.frogerealm.item.seald_artifacts;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ══════════════════════════════════════════════════════════════
 *   THE SCARLET SUTURE  —  Sealed Artifact, Grade 0
 * ══════════════════════════════════════════════════════════════
 *
 * Inspired by Lord of the Mysteries (Blood / Sacrifice / Rebirth).
 * A surgical needle the length of a finger, threaded with a single
 * unbroken strand of crimson silk that has no visible end or
 * beginning. It belonged to a Sequence 1 Blood Emperor who stitched
 * together the wounds of reality itself during a war between gods.
 * The thread still remembers every wound it closed.
 * It wants to close more.
 *
 * ── PASSIVE — Blood Debt Ledger ──────────────────────────────
 *   Every time the player takes damage while holding it:
 *     "SS_DebtPool"    += damage amount (stored in persistentData)
 *     "SS_LifetimeDebt"+= damage amount (never resets)
 *   Debt pool is the resource spent by all active abilities.
 *
 * ── ACTIVE A (right-click targeting a mob ≤5 blocks) ─────────
 *   Wound Stitch:
 *   • Deals damage equal to full debt pool to target.
 *   • Resets debt pool to 0.
 *   • Tags target as "Sutured" (NBT on entity).
 *   • On Sutured mob's death:
 *       – Mob resurrects once at 50% HP.
 *       – All damage it took to die reflected to player
 *         as unavoidable magic damage.
 *   • 5 s cooldown.
 *
 * ── ACTIVE B (right-click in air — no mob in range) ──────────
 *   Self-Suture:
 *   • Costs 16 debt (= 8 hearts of absorbed damage).
 *   • Heals player for 8 HP (4 hearts).
 *   • Bad exchange rate by design — emergency use only.
 *   • If debt < 16: draws from current HP instead
 *     (costs real HP, heals same amount — net zero but
 *      converts HP into "spent" — still risky).
 *   • 10 s cooldown.
 *
 * ── GRADE 0 — THE THREAD PULLS TAUT ─────────────────────────
 *   Triggers when "SS_LifetimeDebt" ≥ 500.
 *   State: "SS_TautActive" = true (permanent).
 *
 *   Every 45 s: involuntarily stitches player to nearest entity.
 *   BLOOD BOND:
 *     • Duration: 30 s.
 *     • All damage player takes → split 50/50 with bonded entity.
 *     • All damage bonded entity takes → split 50/50 with player.
 *     • If bonded entity DIES while bonded:
 *         → Player takes 20 HP (10 hearts) unavoidable magic damage.
 *         → Bond breaks.
 *     • Bond breaks naturally after 30 s.
 *   Blood Bond state stored in runtime Map<UUID, BondState>.
 *   CRIT + DRIPPING_HONEY particles connect player to bonded entity.
 *
 * ── STATE (player.getPersistentData()) ────────────────────────
 *   "SS_DebtPool"      float   — current spendable blood debt
 *   "SS_LifetimeDebt"  float   — total lifetime debt (never resets)
 *   "SS_TautActive"    boolean — Grade 0 state active
 *   "SS_BondTicks"     int     — ticks until next forced bond
 *   "SS_ActiveBondTime"int     — ticks remaining on current bond
 *
 * ── REGISTRATION ──────────────────────────────────────────────
 *   public static final RegistryObject<Item> SCARLET_SUTURE =
 *       ITEMS.register("scarlet_suture",
 *           () -> new ScarletSuture(
 *               new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
 */
@Mod.EventBusSubscriber
public class ScarletSuture extends Item {

    // ── Constants ─────────────────────────────────────────────
    private static final float  TAUT_THRESHOLD       = 500f;
    private static final int    BOND_INTERVAL        = 900;  // 45 s
    private static final int    BOND_DURATION        = 600;  // 30 s
    private static final float  BOND_DEATH_DAMAGE    = 20f;  // 10 hearts
    private static final float  SELF_SUTURE_COST     = 16f;  // debt units
    private static final float  SELF_SUTURE_HEAL     = 8f;   // HP
    private static final double STITCH_RANGE         = 5.0;
    private static final float  TAUT_LIFETIME        = 500f;
    private static final int    WOUND_STITCH_COOLDOWN = 100; // 5 s
    private static final int    SELF_SUTURE_COOLDOWN  = 200; // 10 s

    // ── Runtime bond state (session-only) ─────────────────────
    // Maps player UUID → UUID of bonded entity
    static final Map<UUID, UUID> activeBonds = new HashMap<>();

    // ── Constructor ───────────────────────────────────────────

    public ScarletSuture(Properties properties) {
        super(properties);
    }

    // ── Active: Wound Stitch / Self-Suture ───────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        // Find nearest mob in stitch range
        LivingEntity target = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(STITCH_RANGE),
                e -> e != player
        ).stream().min((a, b) ->
                Double.compare(a.distanceTo(player), b.distanceTo(player))
        ).orElse(null);

        if (target != null) {
            // ── Wound Stitch ──
            if (player.getCooldowns().isOnCooldown(stack.getItem()))
                return InteractionResultHolder.pass(stack);

            float debt = player.getPersistentData().getFloat("SS_DebtPool");
            if (debt <= 0) {
                player.sendSystemMessage(
                        Component.literal("No blood debt to spend.")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                return InteractionResultHolder.pass(stack);
            }

            // Deal debt as damage
            target.hurt(level.damageSources().magic(), debt);

            // Tag as Sutured
            target.getPersistentData().putBoolean("SS_Sutured", true);
            target.getPersistentData().putString("SS_SutureOwner", player.getStringUUID());
            target.getPersistentData().putFloat("SS_SutureDamage", 0f);

            // Reset debt
            player.getPersistentData().putFloat("SS_DebtPool", 0f);

            // Stitch particles
            spawnStitchParticles(player, target, level);

            player.sendSystemMessage(
                    Component.literal(String.format("Stitched. %.1f debt spent.", debt))
                            .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));

            player.getCooldowns().addCooldown(stack.getItem(), WOUND_STITCH_COOLDOWN);

        } else {
            // ── Self-Suture ──
            if (player.getCooldowns().isOnCooldown(stack.getItem()))
                return InteractionResultHolder.pass(stack);

            float debt = player.getPersistentData().getFloat("SS_DebtPool");

            if (debt >= SELF_SUTURE_COST) {
                // Spend debt, heal HP
                player.getPersistentData().putFloat("SS_DebtPool", debt - SELF_SUTURE_COST);
                player.heal(SELF_SUTURE_HEAL);
                player.sendSystemMessage(
                        Component.literal("The wound closes. The debt grows.")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            } else {
                // Draw from current HP — net zero but dangerous
                float cost = SELF_SUTURE_COST - debt;
                player.getPersistentData().putFloat("SS_DebtPool", 0f);
                player.hurt(level.damageSources().magic(), cost);
                player.heal(SELF_SUTURE_HEAL);
                player.sendSystemMessage(
                        Component.literal("Insufficient debt. The Suture takes from you directly.")
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            }

            // Heal particles on player
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.HEART,
                        player.getX(), player.getY() + 1.5, player.getZ(),
                        6, 0.4, 0.4, 0.4, 0.1);
            }

            player.getCooldowns().addCooldown(stack.getItem(), SELF_SUTURE_COOLDOWN);
        }

        return InteractionResultHolder.success(stack);
    }

    // ── Damage taken → accumulate debt ───────────────────────

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (findSuture(player) == null) return;

        float dmg = event.getAmount();

        // Accumulate debt
        var data = player.getPersistentData();
        data.putFloat("SS_DebtPool",     data.getFloat("SS_DebtPool")     + dmg);
        data.putFloat("SS_LifetimeDebt", data.getFloat("SS_LifetimeDebt") + dmg);

        // Trigger Taut state
        if (!data.getBoolean("SS_TautActive")
                && data.getFloat("SS_LifetimeDebt") >= TAUT_THRESHOLD) {
            triggerTautState(player);
        }

        // ── Blood Bond: split damage with bonded entity ──
        UUID bondedUUID = activeBonds.get(player.getUUID());
        if (bondedUUID != null) {
            LivingEntity bonded = findEntityByUUID(player.level(), bondedUUID);
            if (bonded != null) {
                float half = dmg * 0.5f;
                event.setAmount(half); // player takes half
                bonded.hurt(player.level().damageSources().magic(), half); // bonded takes other half
            }
        }
    }

    // ── Bonded entity takes damage → split to player ─────────

    @SubscribeEvent
    public static void onBondedHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) return; // handled above

        // Check if this entity is bonded to a player
        UUID ownerUUID = findBondOwner(entity.getUUID());
        if (ownerUUID == null) return;

        Player owner = entity.level().getEntitiesOfClass(
                Player.class,
                entity.getBoundingBox().inflate(256),
                p -> p.getUUID().equals(ownerUUID)
        ).stream().findFirst().orElse(null);

        if (owner == null || findSuture(owner) == null) return;

        float half = event.getAmount() * 0.5f;
        event.setAmount(half);
        owner.hurt(entity.level().damageSources().magic(), half);
    }

    // ── Sutured mob death → resurrection + reflection ────────

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead  = event.getEntity();
        Level        level = dead.level();
        if (level.isClientSide) return;

        // ── Blood Bond death check ──
        UUID ownerUUID = findBondOwner(dead.getUUID());
        if (ownerUUID != null) {
            Player owner = level.getEntitiesOfClass(
                    Player.class,
                    dead.getBoundingBox().inflate(256),
                    p -> p.getUUID().equals(ownerUUID)
            ).stream().findFirst().orElse(null);

            if (owner != null) {
                // Bond death penalty — 10 hearts
                owner.hurt(level.damageSources().magic(), BOND_DEATH_DAMAGE);
                activeBonds.remove(ownerUUID);
                owner.sendSystemMessage(
                        Component.literal("The bond snaps. The Suture collects.")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            }
        }

        // ── Sutured mob resurrection ──
        var data = dead.getPersistentData();
        if (!data.getBoolean("SS_Sutured")) return;

        String ownerStr = data.getString("SS_SutureOwner");
        float  totalDmg = data.getFloat("SS_SutureDamage") + event.getSource().typeHolder().value().exhaustion();

        // Find the owner player
        Player owner = level.getEntitiesOfClass(
                Player.class,
                dead.getBoundingBox().inflate(256),
                p -> p.getStringUUID().equals(ownerStr)
        ).stream().findFirst().orElse(null);

        if (owner == null) return;

        // Reflect total damage taken back to owner
        float reflected = dead.getMaxHealth(); // reflect max HP as proxy for "all damage to kill"
        owner.hurt(level.damageSources().magic(), reflected);

        owner.sendSystemMessage(
                Component.literal(String.format(
                        "The Suture reflects the wound. (%.1f damage)", reflected))
                        .withStyle(ChatFormatting.DARK_RED));

        // Resurrect mob at 50% HP — cancel death, restore HP
        event.setCanceled(true);
        dead.setHealth(dead.getMaxHealth() * 0.5f);
        dead.getPersistentData().remove("SS_Sutured"); // one-time resurrection

        // Remove hostile tag so it doesn't target the owner... briefly
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT,
                    dead.getX(), dead.getY() + 1, dead.getZ(),
                    12, 0.5, 0.5, 0.5, 0.2);
        }

        owner.sendSystemMessage(
                Component.literal("It rises again.")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
    }

    // ── Server Tick ───────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        Level  level  = player.level();
        if (level.isClientSide) return;
        if (findSuture(player) == null) return;

        var data = player.getPersistentData();

        // ── Passive debt particles (show debt accumulation) ──
        float debt = data.getFloat("SS_DebtPool");
        if (debt > 0 && level instanceof ServerLevel sl && (player.tickCount % 20) == 0) {
            int count = Math.min((int)(debt / 5), 10);
            sl.sendParticles(ParticleTypes.DRIPPING_LAVA,
                    player.getX(), player.getY() + 0.5, player.getZ(),
                    Math.max(1, count), 0.3, 0.3, 0.3, 0.02);
        }

        if (!data.getBoolean("SS_TautActive")) return;

        // ── Taut state: bond timer ──
        int bondTicks = data.getInt("SS_BondTicks") - 1;
        if (bondTicks <= 0) {
            data.putInt("SS_BondTicks", BOND_INTERVAL);
            attemptForcedBond(player, level);
        } else {
            data.putInt("SS_BondTicks", bondTicks);
        }

        // ── Active bond duration countdown ──
        int bondTime = data.getInt("SS_ActiveBondTime");
        if (bondTime > 0) {
            data.putInt("SS_ActiveBondTime", bondTime - 1);

            // Bond particles between player and bonded entity
            UUID bondedUUID = activeBonds.get(player.getUUID());
            if (bondedUUID != null && level instanceof ServerLevel sl) {
                LivingEntity bonded = findEntityByUUID(level, bondedUUID);
                if (bonded != null) {
                    spawnBondParticles(player, bonded, sl);
                }
            }

            // Bond expired naturally
            if (bondTime == 1) {
                activeBonds.remove(player.getUUID());
                player.sendSystemMessage(
                        Component.literal("The thread goes slack.")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        }
    }

    // ── Taut state trigger ────────────────────────────────────

    private static void triggerTautState(Player player) {
        player.getPersistentData().putBoolean("SS_TautActive", true);
        player.getPersistentData().putInt("SS_BondTicks", BOND_INTERVAL);

        player.sendSystemMessage(Component.literal("══════════════════════════════").withStyle(ChatFormatting.DARK_RED));
        player.sendSystemMessage(
                Component.literal("THE THREAD PULLS TAUT")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        player.sendSystemMessage(
                Component.literal("The Suture has learned your shape.")
                        .withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
        player.sendSystemMessage(
                Component.literal("It will stitch you to the world whether you wish it or not.")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal("══════════════════════════════").withStyle(ChatFormatting.DARK_RED));
    }

    // ── Forced bond attempt ───────────────────────────────────

    private static void attemptForcedBond(Player player, Level level) {
        // Already bonded — skip
        if (activeBonds.containsKey(player.getUUID())) return;

        LivingEntity target = level.getEntitiesOfClass(
                LivingEntity.class,
                player.getBoundingBox().inflate(20),
                e -> e != player
        ).stream().min((a, b) ->
                Double.compare(a.distanceTo(player), b.distanceTo(player))
        ).orElse(null);

        if (target == null) {
            player.sendSystemMessage(
                    Component.literal("The Suture seeks but finds nothing.")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return;
        }

        // Create bond
        activeBonds.put(player.getUUID(), target.getUUID());
        player.getPersistentData().putInt("SS_ActiveBondTime", BOND_DURATION);

        player.sendSystemMessage(
                Component.literal("The Suture stitches you to: ")
                        .withStyle(ChatFormatting.DARK_RED)
                        .append(Component.literal(target.getName().getString())
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                        .append(Component.literal(". Damage is shared.")
                                .withStyle(ChatFormatting.DARK_RED)));

        // Bond initiation particles
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CRIT,
                    player.getX(), player.getY() + 1, player.getZ(),
                    8, 0.3, 0.5, 0.3, 0.15);
        }
    }

    // ── Stitch particles ──────────────────────────────────────

    private static void spawnStitchParticles(Player player, LivingEntity target, Level level) {
        if (!(level instanceof ServerLevel sl)) return;
        Vec3 from = player.getEyePosition();
        Vec3 to   = target.getEyePosition();
        Vec3 dir  = to.subtract(from);
        int steps = (int)(dir.length() / 0.5);
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            sl.sendParticles(ParticleTypes.CRIT,
                    from.x + dir.x * t,
                    from.y + dir.y * t,
                    from.z + dir.z * t,
                    1, 0, 0, 0, 0.01);
        }
    }

    // ── Bond particles ────────────────────────────────────────

    private static void spawnBondParticles(Player player, LivingEntity bonded, ServerLevel sl) {
        if ((player.tickCount % 10) != 0) return;
        Vec3 from = player.position().add(0, 1, 0);
        Vec3 to   = bonded.position().add(0, 1, 0);
        Vec3 dir  = to.subtract(from);
        int steps = Math.max(1, (int)(dir.length() / 1.0));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            sl.sendParticles(ParticleTypes.DRIPPING_HONEY,
                    from.x + dir.x * t,
                    from.y + dir.y * t,
                    from.z + dir.z * t,
                    1, 0, 0, 0, 0);
        }
    }

    // ── Helpers ───────────────────────────────────────────────

    private static ItemStack findSuture(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof ScarletSuture) return stack;
        }
        return null;
    }

    private static UUID findBondOwner(UUID entityUUID) {
        for (Map.Entry<UUID, UUID> entry : activeBonds.entrySet()) {
            if (entry.getValue().equals(entityUUID)) return entry.getKey();
        }
        return null;
    }

    private static LivingEntity findEntityByUUID(Level level, UUID uuid) {
        return level.getEntitiesOfClass(LivingEntity.class,
                new AABB(-30000000, -512, -30000000, 30000000, 512, 30000000),
                e -> e.getUUID().equals(uuid)
        ).stream().findFirst().orElse(null);
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

        tooltip.add(Component.literal("Sealed Artifact — Grade 0")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Blood / Sacrifice / Rebirth Pathway")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Blood Debt Ledger:")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Damage taken while held → stored as debt")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  Debt = your offensive/healing resource")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Right-click (mob in range) — Wound Stitch:")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Deals full debt pool as damage to target")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  Target sutured: resurrects once at 50% HP")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  On resurrection: full kill damage reflected to you")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Right-click (air) — Self-Suture:")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Costs 16 debt → heals 8 HP (bad rate)")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  No debt? Draws from your own HP instead.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("⚠ Grade 0 — The Thread Pulls Taut (500 lifetime debt):")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Every 45 s: involuntarily bonded to nearest entity")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  Bond: all damage split 50/50 for 30 s")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  Bonded entity dies: you take 10 hearts instantly")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("\"The thread has no beginning. It has no end.\"")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
