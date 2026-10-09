package net.lukario.frogerealm.item.seald_artifacts;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════
 *   THE CRYSTALLIZED PROPHECY — Beyonder Characteristic Item
 *   Sequence 5 — Visionary (Seer / Prophet Pathway)
 * ══════════════════════════════════════════════════════════════
 *
 * A monocle lens ground from the crystallized eye of a Sequence 5
 * Visionary. The original frame rotted away centuries ago; the lens
 * itself survived, harder than diamond, cold as deep winter.
 * When pressed to the eye it fuses slightly — not permanently, but
 * enough that pulling it away too fast leaves a smear of red.
 * The Beyonder it came from was known for seeing everything at once.
 * She went blind in both eyes and saw more clearly for it.
 *
 * TYPE: Carry-on (held in hand to use passives, right-click for active)
 * PATHWAY: Seer / Visionary
 * SEQUENCE EQUIVALENT: 5
 *
 * ── PASSIVE I — Threat Preview ────────────────────────────────
 *   While held, the player gains a foresight window:
 *     • 1.5 seconds before a projectile or enemy enters 12 blocks,
 *       the player receives a brief Warning effect (Glowing on self).
 *     • Nearby entities about to attack are highlighted (Glowing).
 *     • Gives Night Vision (faint, no visual indicator).
 *     • Gives a gentle passive Speed I boost (foresight = better timing).
 *
 * ── PASSIVE II — Causal Thread ────────────────────────────────
 *   The Visionary's characteristic leaks probability:
 *     • Once every 30 s, a "causality correction" triggers:
 *       the hit that would kill the player instead deals 1 HP and
 *       stuns the attacker with Blindness + Slowness for 3 s.
 *       (Tracked via SC_CausalCooldown in persistentData)
 *     • Projectiles in 6-block radius are deflected (random angle
 *       added to velocity) 1 time every 8 s per projectile type.
 *
 * ── ACTIVE (right-click) — Prophetic Gaze ────────────────────
 *   Passive becomes active for 8 s:
 *     • All entities in 20-block radius glow for 8 s.
 *     • Player receives Slow Falling, Speed II, Resistance I.
 *     • Every hostile that enters range: summoned Arrow entity
 *       predicted and "tagged" — they take +50% damage for 6 s.
 *     • Cooldown: 60 s.
 *   Cost: 0 HP — but adds 40 Foresight Debt (see drawback below).
 *
 * ── DRAWBACK — Foresight Debt ─────────────────────────────────
 *   Seq 5 is powerful. The characteristic bleeds its nature into
 *   the holder. Every second held: +1 Debt point.
 *   Active use: +40 Debt instantly.
 *   Causality save: +60 Debt instantly.
 *
 *   Debt thresholds (stored as "PM_Debt" in persistentData):
 *     0–99    → Normal. Mild Nausea every 30 s (flicker).
 *     100–249 → The Glimpse: player begins receiving fake warning
 *               signals — Glowing appears on random peaceful mobs.
 *               Occasional phantom whispers (chat messages).
 *               Hunger drain ×2.
 *     250–499 → Fracture: real Blindness flashes every 20 s (2 s).
 *               Confusion applied. Player occasionally "sees" an
 *               event 3 ticks before it happens — then it doesn't
 *               happen (mob AI trolled: random nearby hostile
 *               gets a brief Slowness and path reset).
 *     500–749 → Unraveling: Wither I applied permanently while held.
 *               Player's attacks sometimes miss: 20% chance any
 *               outgoing hit is canceled (the "wrong future").
 *               All debuffs from lower tiers active.
 *     750+    → Crystallization: player's vision locks — permanent
 *               Blindness while held. They see TOO MUCH.
 *               Mining Fatigue III. Heart rate flicker (rapid
 *               Hunger drain). Player can no longer sprint.
 *               Random damage 1–3 HP every 10 s (the eye cracks).
 *               Debt never drops — only increases.
 *
 *   Debt reduction: 5 points per minute of NOT holding the monocle.
 *   Debt cap: 999. At 999, the monocle cracks — item is destroyed
 *   and player receives permanent Blindness for 60 s + 10 true damage.
 *
 * ── STATE (player.getPersistentData()) ────────────────────────
 *   "PM_Debt"            int     — current foresight debt
 *   "PM_CausalCooldown"  int     — ticks until causality save resets
 *   "PM_GazeTicks"       int     — ticks remaining on Prophetic Gaze
 *   "PM_GazeCooldown"    int     — ticks until gaze active available
 *   "PM_DeflectCooldown" int     — ticks until next deflection
 *   "PM_WhisperTicks"    int     — ticks until next phantom whisper
 *   "PM_BlindFlash"      int     — ticks until next blind flash
 *   "PM_HoldingSince"    int     — server tick when last picked up
 *   "PM_NotHoldingTicks" int     — ticks player has not held it
 *
 * ── REGISTRATION ──────────────────────────────────────────────
 *   public static final RegistryObject<Item> CRYSTALLIZED_PROPHECY =
 *       ITEMS.register("crystallized_prophecy",
 *           () -> new CrystallizedProphecyMonocle(
 *               new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
 */
@Mod.EventBusSubscriber
public class CrystallizedProphecyMonocle extends Item {

    // ── Debt thresholds ───────────────────────────────────────
    private static final int DEBT_GLIMPSE      = 100;
    private static final int DEBT_FRACTURE     = 250;
    private static final int DEBT_UNRAVELING   = 500;
    private static final int DEBT_CRYSTAL      = 750;
    private static final int DEBT_MAX          = 999;

    // ── Cooldown durations (ticks) ────────────────────────────
    private static final int GAZE_DURATION      = 160;  // 8 s
    private static final int GAZE_COOLDOWN      = 1200; // 60 s
    private static final int CAUSAL_COOLDOWN    = 600;  // 30 s
    private static final int DEFLECT_COOLDOWN   = 160;  // 8 s
    private static final int WHISPER_INTERVAL   = 400;  // 20 s
    private static final int BLIND_FLASH_INT    = 400;  // 20 s
    private static final int DEBT_DECAY_INTERVAL = 1200; // 1 min

    // ── Gaze radius ───────────────────────────────────────────
    private static final double GAZE_RADIUS     = 20.0;
    private static final double WARN_RADIUS     = 12.0;

    // ── Phantom whispers ──────────────────────────────────────
    private static final String[] WHISPERS = {
            "it hasn't happened yet",
            "you already knew that",
            "the outcome was fixed before you moved",
            "don't look left",
            "that wasn't a warning. it was a memory.",
            "you've seen this before",
            "it ends the same way",
            "the thread is fraying",
    };

    // ── Constructor ───────────────────────────────────────────
    public CrystallizedProphecyMonocle(Properties properties) {
        super(properties);
    }

    // ── Active: Prophetic Gaze ────────────────────────────────
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        var data = player.getPersistentData();
        int gazeCooldown = data.getInt("PM_GazeCooldown");
        if (gazeCooldown > 0) {
            player.displayClientMessage(
                    Component.literal("The lens hasn't settled. (" + (gazeCooldown / 20) + "s)")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            return InteractionResultHolder.pass(stack);
        }

        // Activate gaze
        data.putInt("PM_GazeTicks",    GAZE_DURATION);
        data.putInt("PM_GazeCooldown", GAZE_COOLDOWN);
        addDebt(data, 40);

        // Glow all nearby entities
        level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(GAZE_RADIUS),
                e -> e != player
        ).forEach(e -> {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, GAZE_DURATION, 0, false, false));
        });

        // Player buffs
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,  GAZE_DURATION, 0, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, GAZE_DURATION, 1, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, GAZE_DURATION, 0, false, false));

        // Visual
        if (level instanceof ServerLevel sl) {
            Vec3 p = player.position();
            for (int i = 0; i < 24; i++) {
                double angle = (Math.PI * 2.0 / 24) * i;
                sl.sendParticles(ParticleTypes.END_ROD,
                        p.x + Math.cos(angle) * 4, p.y + 1.5,
                        p.z + Math.sin(angle) * 4,
                        1, 0, 0.1, 0, 0.05);
            }
        }

        player.displayClientMessage(
                Component.literal("You see what hasn't happened yet.")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC), true);

        player.getCooldowns().addCooldown(this, GAZE_COOLDOWN);
        return InteractionResultHolder.success(stack);
    }

    // ── Main server tick ──────────────────────────────────────
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        Level  level  = player.level();
        if (level.isClientSide) return;

        var data = player.getPersistentData();
        boolean holding = isHolding(player);

        // ── Not holding: decay debt ──────────────────────────
        if (!holding) {
            int notHoldingTicks = data.getInt("PM_NotHoldingTicks") + 1;
            data.putInt("PM_NotHoldingTicks", notHoldingTicks);
            if (notHoldingTicks % DEBT_DECAY_INTERVAL == 0) {
                int debt = data.getInt("PM_Debt");
                if (debt > 0) {
                    data.putInt("PM_Debt", Math.max(0, debt - 5));
                }
            }
            tickCooldowns(data); // cooldowns still tick when not held
            return;
        }

        data.putInt("PM_NotHoldingTicks", 0);

        // ── Debt accrual: +1/s while held ────────────────────
        if ((player.tickCount % 20) == 0) {
            addDebt(data, 1);
        }

        int debt = data.getInt("PM_Debt");

        // ── Crystallization check ─────────────────────────────
        if (debt >= DEBT_MAX) {
            shatterMonocle(player, level);
            return;
        }

        // ── Passives ─────────────────────────────────────────
        applyBasePassives(player, level, debt);

        // ── Threat preview highlight ──────────────────────────
        if ((player.tickCount % 10) == 0) {
            tickThreatPreview(player, level);
        }

        // ── Projectile deflection ─────────────────────────────
        int deflectCD = data.getInt("PM_DeflectCooldown") - 1;
        if (deflectCD <= 0) {
            data.putInt("PM_DeflectCooldown", DEFLECT_COOLDOWN);
            tickProjectileDeflect(player, level);
        } else {
            data.putInt("PM_DeflectCooldown", deflectCD);
        }

        // ── Gaze active tick ─────────────────────────────────
        int gazeTicks = data.getInt("PM_GazeTicks");
        if (gazeTicks > 0) {
            data.putInt("PM_GazeTicks", gazeTicks - 1);
            if (gazeTicks == 1) {
                player.displayClientMessage(
                        Component.literal("The vision fades.")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
        }

        // ── Cooldown ticks ────────────────────────────────────
        tickCooldowns(data);

        // ── Drawback tiers ────────────────────────────────────
        applyDebtDrawbacks(player, level, data, debt);

        // ── Particles (subtle) ───────────────────────────────
        if (level instanceof ServerLevel sl && (player.tickCount % 30) == 0) {
            sl.sendParticles(ParticleTypes.ENCHANT,
                    player.getX() + (Math.random() - 0.5),
                    player.getY() + 1.8,
                    player.getZ() + (Math.random() - 0.5),
                    1, 0, 0.05, 0, 0.02);
        }
    }

    // ── Base passives ─────────────────────────────────────────
    private static void applyBasePassives(Player player, Level level, int debt) {
        // Night vision (faint, no HUD icon)
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false));
        // Speed I — foresight timing
        if (debt < DEBT_UNRAVELING) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false));
        }
    }

    // ── Threat preview: glow nearby hostiles ─────────────────
    private static void tickThreatPreview(Player player, Level level) {
        level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(WARN_RADIUS),
                e -> e != player && isHostile(e, player)
        ).forEach(e -> {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false));
        });
    }

    // ── Projectile deflection ─────────────────────────────────
    private static void tickProjectileDeflect(Player player, Level level) {
        level.getEntitiesOfClass(Arrow.class,
                player.getBoundingBox().inflate(6.0),
                a -> !a.getOwner().equals(player)
        ).forEach(arrow -> {
            Vec3 vel = arrow.getDeltaMovement();
            double spread = 0.6;
            Vec3 deflected = new Vec3(
                    vel.x + (level.random.nextDouble() - 0.5) * spread,
                    vel.y + (level.random.nextDouble() - 0.5) * spread,
                    vel.z + (level.random.nextDouble() - 0.5) * spread
            );
            arrow.setDeltaMovement(deflected);
            if (level instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.CRIT,
                        arrow.getX(), arrow.getY(), arrow.getZ(),
                        5, 0.2, 0.2, 0.2, 0.05);
            }
        });
    }

    // ── Drawback tiers ────────────────────────────────────────
    private static void applyDebtDrawbacks(Player player, Level level,
                                           net.minecraft.nbt.CompoundTag data, int debt) {

        // Tier 1: The Glimpse (100+)
        if (debt >= DEBT_GLIMPSE) {
            // Hunger drain
            if ((player.tickCount % 20) == 0) {
                player.getFoodData().setFoodLevel(
                        Math.max(0, player.getFoodData().getFoodLevel() - 1));
            }
            // Fake threat highlights on random peaceful mobs
            if ((player.tickCount % 200) == 0) {
                level.getEntitiesOfClass(LivingEntity.class,
                        player.getBoundingBox().inflate(WARN_RADIUS),
                        e -> e != player && !isHostile(e, player)
                ).stream().limit(2).forEach(e ->
                        e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false))
                );
            }
            // Phantom whispers
            int whisperTicks = data.getInt("PM_WhisperTicks") - 1;
            if (whisperTicks <= 0) {
                data.putInt("PM_WhisperTicks", WHISPER_INTERVAL);
                String whisper = WHISPERS[level.random.nextInt(WHISPERS.length)];
                player.sendSystemMessage(
                        Component.literal(whisper)
                                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
            } else {
                data.putInt("PM_WhisperTicks", whisperTicks);
            }
        }

        // Tier 2: Fracture (250+)
        if (debt >= DEBT_FRACTURE) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0, false, false));
            // Blindness flash
            int blindTicks = data.getInt("PM_BlindFlash") - 1;
            if (blindTicks <= 0) {
                data.putInt("PM_BlindFlash", BLIND_FLASH_INT);
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 400, 0, false, true));
            } else {
                data.putInt("PM_BlindFlash", blindTicks);
            }
        }

        // Tier 3: Unraveling (500+)
        if (debt >= DEBT_UNRAVELING) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0, false, false));
            // Extra hunger drain
            if ((player.tickCount % 10) == 0) {
                player.getFoodData().setFoodLevel(
                        Math.max(0, player.getFoodData().getFoodLevel() - 1));
            }
        }

        // Tier 4: Crystallization (750+)
        if (debt >= DEBT_CRYSTAL) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,   120, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,40, 2, false, false));
            // No sprinting
            if (player.isSprinting()) player.setSprinting(false);
            // Random eye crack damage every 10 s
            if ((player.tickCount % 200) == 0) {
                float dmg = 1f + level.random.nextInt(3);
                player.hurt(level.damageSources().magic(), dmg);
                player.sendSystemMessage(
                        Component.literal("The lens cracks further. (" + (DEBT_MAX - debt) + " until collapse)")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
            }
        }
    }

    // ── Causality save: intercept lethal blow ─────────────────
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Level level = player.level();
        if (level.isClientSide) return;
        if (!isHolding(player)) return;

        var data = player.getPersistentData();
        int causalCD = data.getInt("PM_CausalCooldown");

        // Check if this hit would be lethal
        float currentHP = player.getHealth();
        float damage    = event.getAmount();
        if (currentHP - damage <= 0 && causalCD <= 0) {
            // Cancel the killing blow, leave player at 1 HP
            event.setAmount(currentHP - 1f);
            data.putInt("PM_CausalCooldown", CAUSAL_COOLDOWN);
            addDebt(data, 60);

            // Stun attacker
            DamageSource source = event.getSource();
            if (source.getEntity() instanceof LivingEntity attacker) {
                attacker.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,   120, 0, false, true));
                attacker.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true));
            }

            // Visual
            if (level instanceof ServerLevel sl) {
                Vec3 p = player.position();
                for (int i = 0; i < 16; i++) {
                    double angle = (Math.PI * 2.0 / 16) * i;
                    sl.sendParticles(ParticleTypes.END_ROD,
                            p.x + Math.cos(angle) * 1.5, p.y + 1,
                            p.z + Math.sin(angle) * 1.5,
                            1, 0, 0.05, 0, 0.05);
                }
            }

            player.displayClientMessage(
                    Component.literal("Causality corrected. That outcome was not permitted.")
                            .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC), true);
        }
    }

    // ── 20% miss chance at Unraveling tier ───────────────────
    @SubscribeEvent
    public static void onAttackerHurt(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof Player player)) return;
        Level level = player.level();
        if (level.isClientSide) return;
        if (!isHolding(player)) return;

        var data = player.getPersistentData();
        int debt = data.getInt("PM_Debt");
        if (debt < DEBT_UNRAVELING) return;

        // 20% chance the hit gets canceled — "the wrong future"
        if (level.random.nextFloat() < 0.20f) {
            event.setCanceled(true);
            player.displayClientMessage(
                    Component.literal("The wrong future. Your blow lands in a moment that won't come.")
                            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC), true);
        }
    }

    // ── Monocle shatters at max debt ──────────────────────────
    private static void shatterMonocle(Player player, Level level) {
        // Remove the item from inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof CrystallizedProphecyMonocle) {
                player.getInventory().setItem(i, ItemStack.EMPTY);
                break;
            }
        }

        // Lethal-ish punishment: 10 true damage + 60 s blindness
        player.hurt(level.damageSources().magic(), 20f);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 1200, 0, false, true));
        player.getPersistentData().putInt("PM_Debt", 0); // reset on shatter

        // Visual
        if (level instanceof ServerLevel sl) {
            Vec3 p = player.position();
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, p.x, p.y + 1, p.z, 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 1, p.z, 80, 0.5, 0.5, 0.5, 0.3);
        }

        player.sendSystemMessage(Component.literal("══════════════════════════════════").withStyle(ChatFormatting.DARK_PURPLE));
        player.sendSystemMessage(Component.literal("THE LENS SHATTERS")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("You saw too much. There was nothing left to foresee.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal("══════════════════════════════════").withStyle(ChatFormatting.DARK_PURPLE));
    }

    // ── Utility: cooldown ticks ───────────────────────────────
    private static void tickCooldowns(net.minecraft.nbt.CompoundTag data) {
        int cc = data.getInt("PM_CausalCooldown");
        if (cc > 0) data.putInt("PM_CausalCooldown", cc - 1);
        int gc = data.getInt("PM_GazeCooldown");
        if (gc > 0) data.putInt("PM_GazeCooldown", gc - 1);
    }

    // ── Utility: debt ─────────────────────────────────────────
    private static void addDebt(net.minecraft.nbt.CompoundTag data, int amount) {
        int debt = data.getInt("PM_Debt");
        data.putInt("PM_Debt", Math.min(DEBT_MAX, debt + amount));
    }

    // ── Utility: holding check ────────────────────────────────
    private static boolean isHolding(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).getItem() instanceof CrystallizedProphecyMonocle)
                return true;
        }
        return false;
    }

    // ── Utility: hostile check ────────────────────────────────
    private static boolean isHostile(LivingEntity entity, Player player) {
        return entity instanceof net.minecraft.world.entity.monster.Monster
                || (entity instanceof net.minecraft.world.entity.animal.Animal == false
                && entity.getLastHurtByMob() != null);
    }

    // ── Tooltip ───────────────────────────────────────────────
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.literal("Beyonder Characteristic — Sequence 5")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Visionary  |  Seer / Prophet Pathway")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Threat Preview:")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Nearby hostiles glow. Night Vision. Speed I.")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Causality Save:")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Once per 30 s: lethal hit → 1 HP. Stuns attacker.")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("  Nearby arrows deflected every 8 s.")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Right-click — Prophetic Gaze (60 s cooldown):")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Glow all entities. Speed II, Resistance, Slow Falling.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  8 s duration. Costs 40 Foresight Debt.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("⚠ Drawback — Foresight Debt:")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  +1 Debt/s while held. Causality save: +60 Debt.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  100  — Glimpse: fake warnings, whispers, hunger.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  250  — Fracture: Confusion, Blindness flashes.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  500  — Unraveling: Wither I. Attacks miss 20%.")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  750  — Crystallization: Permanent Blindness, Fatigue III.")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  999  — The Lens Shatters. Item destroyed.")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Debt decays -5/min while NOT held.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("\"She went blind in both eyes. She saw more clearly for it.\"")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
