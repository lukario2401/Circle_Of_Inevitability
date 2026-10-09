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
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════
 *   THE UNDYING CANKER  —  Sealed Artifact, Grade 0
 * ══════════════════════════════════════════════════════════════
 *
 * Inspired by Lord of the Mysteries (Plague / Rot pathway).
 * A lump of blackened flesh, still warm, that has never stopped
 * rotting despite being decades old. It belonged to a Sequence 1
 * Plague Monarch who attempted to infect the concept of death
 * itself during their ascension. They didn't finish.
 * The Canker did.
 *
 * ── PASSIVE — Infection Spread ────────────────────────────────
 *   Every second held: all living entities within 16 blocks gain
 *   1 Plague Stack (NBT "UC_Stacks" on entity PersistentData).
 *
 *   Stack thresholds on entities:
 *     5 stacks  → Wither I + Slowness I applied
 *     10 stacks → FULLY INFECTED:
 *       • Rapid HP drain (2 damage/s, bypasses armor)
 *       • On hit: spreads 3 stacks to struck entity
 *       • On death: 8-block infection explosion — all nearby
 *         entities gain 5 stacks instantly
 *
 * ── ACTIVE (right-click) — Monarch's Decree ──────────────────
 *   Instantly sets all entities within 24 blocks to 10 stacks.
 *   Costs 6 hearts (12 HP) of current HP. Min 1 HP remaining.
 *   90-second cooldown.
 *
 * ── BLOWBACK — Player Infection ───────────────────────────────
 *   Every 20 seconds held: player gains 1 personal Plague Stack.
 *   Stored in player.getPersistentData() as "UC_PlayerStacks".
 *
 *   Player stack thresholds:
 *     5 stacks  → Wither I + Hunger III (passive, refreshed)
 *     10 stacks → PLAGUE VECTOR:
 *       • All melee attacks spread 5 stacks to target
 *       • Max HP drains −1 every 60 s (attribute modifier)
 *       • Canker whispers: periodic obfuscated chat messages
 *
 * ── GRADE 0 — THE MONARCH AWAKENS ────────────────────────────
 *   Triggers when: player at 10 stacks AND "UC_DeathCount" ≥ 20.
 *   (DeathCount = entities killed by infection while you hold it.)
 *
 *   Monarch State ("UC_Monarch" = true, permanent until death):
 *     • Cannot drop the Canker (re-inserts each tick)
 *     • Infection aura radius expands to 32 blocks
 *     • Infects 2 stacks/s instead of 1
 *     • Every 30 s: 8 unavoidable rot damage
 *     • Particle aura: constant WARPED_SPORE cloud
 *
 *   Death resets: UC_PlayerStacks, UC_Monarch, UC_DeathCount,
 *                 UC_MaxHPStacks — but the Canker remains.
 *
 * ── STATE (player.getPersistentData()) ────────────────────────
 *   "UC_PlayerStacks"  int     — personal plague stacks (0–10)
 *   "UC_BlowbackTicks" int     — ticks until next player stack
 *   "UC_DeathCount"    int     — infection-caused entity deaths
 *   "UC_Monarch"       boolean — Monarch State active
 *   "UC_MonarchTicks"  int     — ticks since last rot burst
 *   "UC_MaxHPStacks"   int     — max HP debuff stacks applied
 *
 * ── REGISTRATION ──────────────────────────────────────────────
 *   public static final RegistryObject<Item> UNDYING_CANKER =
 *       ITEMS.register("undying_canker",
 *           () -> new UndyingCanker(
 *               new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
 */
@Mod.EventBusSubscriber
public class UndyingCanker extends Item {

    // ── Constants ─────────────────────────────────────────────

    private static final int    AURA_RADIUS_NORMAL  = 16;
    private static final int    AURA_RADIUS_MONARCH = 32;
    private static final int    STACK_INFECTED      = 10;
    private static final int    STACK_SYMPTOMATIC   = 5;
    private static final int    BLOWBACK_INTERVAL   = 400;  // 20 s
    private static final int    ACTIVE_COOLDOWN     = 1800; // 90 s
    private static final int    MONARCH_ROT_INTERVAL= 600;  // 30 s
    private static final float  ACTIVE_HP_COST      = 12f;  // 6 hearts
    private static final int    MONARCH_THRESHOLD   = 20;   // deaths to trigger
    private static final float  INFECTED_DRAIN      = 2.0f; // hp/s on fully infected
    private static final float  DEATH_EXPLOSION_RADIUS = 8f;
    private static final int    SPREAD_STACKS_ON_HIT  = 3;
    private static final int    SPREAD_STACKS_ON_DEATH = 5;
    private static final int    VECTOR_SPREAD_STACKS   = 5;

    private static final ResourceLocation MAX_HP_MOD_ID =
            ResourceLocation.fromNamespaceAndPath("frogerealm", "uc_max_hp_drain");

    private static final String[] WHISPERS = {
            "it grows",
            "you feed it",
            "let them rot",
            "the flesh remembers",
            "you cannot put it down",
            "it was always inside you",
            "the monarch is pleased",
    };

    // ── Constructor ───────────────────────────────────────────

    public UndyingCanker(Properties properties) {
        super(properties);
    }

    // ── Active: Monarch's Decree ──────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);

        // HP cost — leave at least 1 HP
        float newHp = Math.max(1f, player.getHealth() - ACTIVE_HP_COST);
        player.setHealth(newHp);

        // Max-infect all in range
        double radius = player.getPersistentData().getBoolean("UC_Monarch")
                ? AURA_RADIUS_MONARCH : AURA_RADIUS_NORMAL;

        level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(24),
                e -> e != player
        ).forEach(e -> e.getPersistentData().putInt("UC_Stacks", STACK_INFECTED));

        // Big particle burst
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.WARPED_SPORE,
                    player.getX(), player.getY() + 1, player.getZ(),
                    80, 3, 1, 3, 0.3);
        }

        player.displayClientMessage(
                Component.literal("They will all rot.")
                        .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD), true);

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

        var data = player.getPersistentData();
        boolean holding  = findCanker(player) != null;
        boolean monarch  = data.getBoolean("UC_Monarch");

        // Monarch state: re-insert if dropped
        if (monarch) {
            tickMonarchEnforceHold(player, level);
        }

        if (!holding && !monarch) return;

        // ── Aura radius ──
        double radius = monarch ? AURA_RADIUS_MONARCH : AURA_RADIUS_NORMAL;
        int    stacksPerTick = monarch ? 2 : 1;

        // ── Spread infection to nearby entities (once/s) ──
        if ((player.tickCount % 20) == 0) {
            level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(radius),
                    e -> e != player
            ).forEach(e -> {
                int current = e.getPersistentData().getInt("UC_Stacks");
                int next    = Math.min(STACK_INFECTED, current + stacksPerTick);
                e.getPersistentData().putInt("UC_Stacks", next);
                applyEntityStackEffects(e, level, next);
            });
        }

        // ── Fully infected entities: drain HP every second ──
        if ((player.tickCount % 20) == 0) {
            level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(radius * 2), // slightly wider catch
                    e -> e != player && e.getPersistentData().getInt("UC_Stacks") >= STACK_INFECTED
            ).forEach(e -> e.hurt(level.damageSources().magic(), INFECTED_DRAIN));
        }

        // ── Particle aura ──
        if (level instanceof ServerLevel sl) {
            int particleFreq = monarch ? 3 : 8;
            if ((player.tickCount % particleFreq) == 0) {
                sl.sendParticles(ParticleTypes.WARPED_SPORE,
                        player.getX() + (Math.random() - 0.5) * radius * 0.3,
                        player.getY() + Math.random() * 2,
                        player.getZ() + (Math.random() - 0.5) * radius * 0.3,
                        monarch ? 4 : 1, 0, 0.02, 0, 0.05);
            }
        }

        // ── Blowback: player gains stacks ──
        int blowbackTicks = data.getInt("UC_BlowbackTicks") - 1;
        if (blowbackTicks <= 0) {
            data.putInt("UC_BlowbackTicks", BLOWBACK_INTERVAL);
            int playerStacks = Math.min(STACK_INFECTED, data.getInt("UC_PlayerStacks") + 1);
            data.putInt("UC_PlayerStacks", playerStacks);
            applyPlayerStackEffects(player, playerStacks, level, data);
        } else {
            data.putInt("UC_BlowbackTicks", blowbackTicks);
        }

        // ── Monarch rot burst ──
        if (monarch) {
            int monarchTicks = data.getInt("UC_MonarchTicks") - 1;
            if (monarchTicks <= 0) {
                data.putInt("UC_MonarchTicks", MONARCH_ROT_INTERVAL);
                player.hurt(level.damageSources().magic(), 8f);
                player.displayClientMessage(
                        Component.literal("The Canker demands tribute.")
                                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC), true);
            } else {
                data.putInt("UC_MonarchTicks", monarchTicks);
            }
        }
    }

    // ── Apply stack effects to a non-player entity ────────────

    private static void applyEntityStackEffects(LivingEntity entity, Level level, int stacks) {
        if (stacks >= STACK_SYMPTOMATIC) {
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER,           40, 0, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40, 0, false, false));
        }
        if (stacks >= STACK_INFECTED) {
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER,           40, 1, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40, 2, false, false));
            entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,         40, 1, false, false));
        }
    }

    // ── Apply stack effects to the player ────────────────────

    private static void applyPlayerStackEffects(Player player, int stacks,
                                                Level level, net.minecraft.nbt.CompoundTag data) {
        if (stacks >= STACK_SYMPTOMATIC) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER,  60, 0, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 2, false, true));
            player.displayClientMessage(
                    Component.literal("The rot takes hold. [" + stacks + "/10]")
                            .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC), true);
        }

        if (stacks >= STACK_INFECTED) {
            // Max HP drain
            int hpStacks = data.getInt("UC_MaxHPStacks");
            if (hpStacks < 8) { // cap at −8 hearts
                applyMaxHPDebuff(player, hpStacks + 1);
                data.putInt("UC_MaxHPStacks", hpStacks + 1);
            }

            // Whisper
            String whisper = WHISPERS[level.random.nextInt(WHISPERS.length)];
            player.sendSystemMessage(
                    Component.literal("[ ")
                            .withStyle(ChatFormatting.DARK_GRAY)
                            .append(Component.literal(whisper)
                                    .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC))
                            .append(Component.literal(" ]")
                                    .withStyle(ChatFormatting.DARK_GRAY))
            );

            // Check Monarch threshold
            int deaths = data.getInt("UC_DeathCount");
            if (!data.getBoolean("UC_Monarch") && deaths >= MONARCH_THRESHOLD) {
                triggerMonarchAwakening(player);
            }
        }
    }

    // ── Infection spreads on hit (Vector state) ───────────────

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;
        if (findCanker(attacker) == null) return;

        int playerStacks = attacker.getPersistentData().getInt("UC_PlayerStacks");
        LivingEntity target = event.getEntity();

        // Plague Vector: spread stacks on hit
        if (playerStacks >= STACK_INFECTED) {
            int current = target.getPersistentData().getInt("UC_Stacks");
            target.getPersistentData().putInt("UC_Stacks",
                    Math.min(STACK_INFECTED, current + VECTOR_SPREAD_STACKS));
        }

        // Fully infected entities spread on hit too
        int targetStacks = target.getPersistentData().getInt("UC_Stacks");
        if (targetStacks >= STACK_INFECTED) {
            if (event.getSource().getEntity() instanceof LivingEntity hitEntity) {
                int current = hitEntity.getPersistentData().getInt("UC_Stacks");
                hitEntity.getPersistentData().putInt("UC_Stacks",
                        Math.min(STACK_INFECTED, current + SPREAD_STACKS_ON_HIT));
            }
        }
    }

    // ── Infection death explosion ─────────────────────────────

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity dead  = event.getEntity();
        Level        level = dead.level();
        if (level.isClientSide) return;

        int stacks = dead.getPersistentData().getInt("UC_Stacks");
        if (stacks < STACK_INFECTED) return;

        // Infection explosion: spread stacks to nearby entities
        level.getEntitiesOfClass(LivingEntity.class,
                dead.getBoundingBox().inflate(DEATH_EXPLOSION_RADIUS),
                e -> e != dead
        ).forEach(e -> {
            int current = e.getPersistentData().getInt("UC_Stacks");
            e.getPersistentData().putInt("UC_Stacks",
                    Math.min(STACK_INFECTED, current + SPREAD_STACKS_ON_DEATH));
        });

        // Particle burst at death position
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.WARPED_SPORE,
                    dead.getX(), dead.getY() + 1, dead.getZ(),
                    40, DEATH_EXPLOSION_RADIUS * 0.4,
                    1, DEATH_EXPLOSION_RADIUS * 0.4, 0.2);
        }

        // Increment death counter on nearby Canker holders
        level.getEntitiesOfClass(Player.class,
                dead.getBoundingBox().inflate(64),
                p -> findCanker(p) != null
        ).forEach(p -> {
            var data = p.getPersistentData();
            data.putInt("UC_DeathCount", data.getInt("UC_DeathCount") + 1);
        });
    }

    // ── Monarch Awakening ─────────────────────────────────────

    private static void triggerMonarchAwakening(Player player) {
        player.getPersistentData().putBoolean("UC_Monarch", true);
        player.getPersistentData().putInt("UC_MonarchTicks", MONARCH_ROT_INTERVAL);

        player.sendSystemMessage(Component.literal("══════════════════════════════").withStyle(ChatFormatting.DARK_GREEN));
        player.sendSystemMessage(
                Component.literal("THE MONARCH AWAKENS")
                        .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
        player.sendSystemMessage(
                Component.literal("You cannot put it down.")
                        .withStyle(ChatFormatting.GREEN, ChatFormatting.ITALIC));
        player.sendSystemMessage(
                Component.literal("You never could.")
                        .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal("══════════════════════════════").withStyle(ChatFormatting.DARK_GREEN));
    }

    // ── Monarch: enforce holding the Canker ──────────────────

    private static void tickMonarchEnforceHold(Player player, Level level) {
        // If Canker is not in either hand, find it in inventory and move it
        if (findCanker(player) != null) return;

        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() instanceof UndyingCanker) {
                // Move to main hand
                ItemStack current = player.getMainHandItem();
                player.getInventory().setItem(player.getInventory().selected, stack.copy());
                stack.setCount(0);
                return;
            }
        }

        // Not in inventory either — re-create it (it cannot be destroyed)
        player.addItem(new ItemStack(
                net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                        ResourceLocation.fromNamespaceAndPath("frogerealm", "undying_canker"))));

        if ((player.tickCount % 100) == 0) {
            player.displayClientMessage(
                    Component.literal("It always comes back.")
                            .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC), true);
        }
    }

    // ── Death reset ───────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        var data = player.getPersistentData();
        data.putBoolean("UC_Monarch",       false);
        data.putInt("UC_PlayerStacks",      0);
        data.putInt("UC_DeathCount",        0);
        data.putInt("UC_MonarchTicks",      0);
        data.putInt("UC_BlowbackTicks",     0);

        // Remove max HP debuff
        var attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr != null) attr.removeModifier(MAX_HP_MOD_ID);
        data.putInt("UC_MaxHPStacks", 0);
    }

    // ── Max HP drain ──────────────────────────────────────────

    private static void applyMaxHPDebuff(Player player, int stackCount) {
        var attr = player.getAttribute(Attributes.MAX_HEALTH);
        if (attr == null) return;

        attr.removeModifier(MAX_HP_MOD_ID);
        attr.addPermanentModifier(new AttributeModifier(
                MAX_HP_MOD_ID,
                -2.0 * stackCount, // −1 heart per stack
                AttributeModifier.Operation.ADD_VALUE
        ));

        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    // ── Utility ───────────────────────────────────────────────

    private static ItemStack findCanker(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof UndyingCanker) return stack;
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
        tooltip.add(Component.literal("Plague / Rot Pathway")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Infection Spread (16 blocks):")
                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  +1 Plague Stack/s to nearby entities")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("  5 stacks: Wither + Slowness")
                .withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("  10 stacks: HP drain, spreads on hit/death")
                .withStyle(ChatFormatting.DARK_GREEN));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Right-click — Monarch's Decree:")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Cost: 6 hearts. Max-infects 24-block radius.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  90 second cooldown.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("⚠ Blowback — Player Infection:")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  +1 stack on yourself every 20 seconds")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  5 stacks: Wither + Hunger on yourself")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  10 stacks: Plague Vector + max HP drain")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("⚠ Grade 0 — The Monarch Awakens:")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  10 personal stacks + 20 infection kills")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  → Cannot drop item. 32-block aura.")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  → 8 unavoidable rot damage every 30 s")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  Resets only on death.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("\"It has never stopped rotting.\"")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}