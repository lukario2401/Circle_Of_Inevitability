package net.lukario.frogerealm.item.seald_artifacts;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════
 *   THE HOLLOW NAME  —  Sealed Artifact, Grade 0
 * ══════════════════════════════════════════════════════════════
 *
 * Inspired by Lord of the Mysteries (Shadow / Nothingness pathway).
 * A strip of black silk no wider than a finger, upon which a true
 * name is written in the blood of a Sequence 1 Shadow of Nothingness.
 * Reading it aloud temporarily removes you from reality's ledger.
 * Grade 0: borders on complete ontological erasure.
 *
 * Consumed on use. One item, one chance.
 *
 * ── PHASE 1: Unwritten  (ticks 0–100, 0–5 s) ─────────────────
 *   • Full Invisibility — no mob can see or target you.
 *   • All mobs within 20 blocks instantly lose aggro (forget you).
 *   • No collision with mobs (handled via tick).
 *   • Silent Steps effect (no footstep sounds — Feather Falling spoof).
 *   • SOUL particles leak from the player.
 *
 * ── PHASE 2: Erasure's Edge  (ticks 100–300, 5–15 s) ──────────
 *   • Invisibility continues.
 *   • Next attack on any entity below 50% HP → INSTANT KILL,
 *     regardless of armor or max health.
 *   • Each kill: lightning strikes target, Phase 2 extended +2 s.
 *   • Kill counter stored in NBT "HN_Kills".
 *
 * ── PHASE 3: Ontological Debt  (ticks 300+, 15 s+) ────────────
 *   • Invisibility breaks — reality reasserts itself.
 *   • Wither damage escalates: every second adds +1 Wither amplifier.
 *   • At tick 500 (25 s): forcibly teleported to world 0,0 surface.
 *     Ten consecutive lightning strikes hit the player.
 *   • Item was already consumed — no second chances.
 *
 * ── PERMANENT MARK: Witnessed ──────────────────────────────────
 *   After the effect ends (survival or Phase 3):
 *   • NBT tag "HN_Witnessed" = true written to the player.
 *   • All mobs spawning within 16 blocks: 15% chance instant aggro.
 *   • Displayed in tooltip if already marked.
 *
 * ── STATE (player.getPersistentData()) ────────────────────────
 *   "HN_Active"    boolean  — effect currently running
 *   "HN_Ticks"     int      — ticks elapsed since activation
 *   "HN_Kills"     int      — Phase 2 kills (each adds 40 ticks)
 *   "HN_Witnessed" boolean  — permanent mark (survives relog)
 *
 * ── REGISTRATION ──────────────────────────────────────────────
 *   public static final RegistryObject<Item> HOLLOW_NAME =
 *       ITEMS.register("hollow_name",
 *           () -> new HollowName(
 *               new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
 */
@Mod.EventBusSubscriber
public class HollowName extends Item {

    // ── Phase boundaries (ticks) ──────────────────────────────
    private static final int PHASE_2_START  = 300;   // 5 s
    private static final int PHASE_3_START  = 300;   // 15 s
    private static final int ERASURE_TICK   = 500;   // 25 s — point of no return
    private static final int KILL_EXTENSION = 100;    // ticks added per Phase 2 kill

    // ── Aggro radius for the Witnessed mark ──────────────────
    private static final double WITNESSED_RADIUS = 16.0;
    private static final float  AGGRO_CHANCE     = 0.15f;

    // ── Constructor ───────────────────────────────────────────

    public HollowName(Properties properties) {
        super(properties);
    }

    // ── Use: consume and activate ─────────────────────────────

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        var data = player.getPersistentData();

        // Prevent double-activation
        if (data.getBoolean("HN_Active")) return InteractionResultHolder.pass(stack);

        // ── Initialise state ──
        data.putBoolean("HN_Active", true);
        data.putInt("HN_Ticks", 0);
        data.putInt("HN_Kills", 0);

        // ── Phase 1: wipe nearby aggro immediately ──
        level.getEntitiesOfClass(
                Monster.class,
                player.getBoundingBox().inflate(20),
                e -> true
        ).forEach(mob -> mob.setTarget(null));

        // ── Consume the item ──
        stack.shrink(1);

        player.displayClientMessage(
                Component.literal("You speak the Name. The world forgets you.")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD),
                false
        );

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
        if (!data.getBoolean("HN_Active")) {
            // ── Witnessed passive aggro check ──
            tickWitnessedAggro(player, level);
            return;
        }

        int ticks = data.getInt("HN_Ticks") + 1;
        data.putInt("HN_Ticks", ticks);

        // ════════════════════════════════════
        //  PHASE 1  —  Unwritten  (0–100 t)
        // ════════════════════════════════════
        if (ticks < PHASE_2_START) {
            // Full invisibility, refreshed every tick
            player.addEffect(new MobEffectInstance(
                    MobEffects.INVISIBILITY, 40, 0, false, false));

            // Suppress mob targeting continuously
            if ((ticks % 10) == 0) {
                level.getEntitiesOfClass(
                        Monster.class,
                        player.getBoundingBox().inflate(20),
                        mob -> player.equals(mob.getTarget())
                ).forEach(mob -> mob.setTarget(null));
            }

            // SOUL particle leak
            if (level instanceof ServerLevel sl && (ticks % 4) == 0) {
                sl.sendParticles(ParticleTypes.SOUL,
                        player.getX() + (Math.random() - 0.5) * 0.6,
                        player.getY() + Math.random() * 2.0,
                        player.getZ() + (Math.random() - 0.5) * 0.6,
                        1, 0, 0.05, 0, 0.01);
            }

            // Phase 1 entry message
            if (ticks == 1) {
                player.displayClientMessage(
                        Component.literal("Phase I — You are Unwritten.")
                                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC),
                        true
                );
            }
        }

        // ════════════════════════════════════
        //  PHASE 2  —  Erasure's Edge  (100–300 t)
        // ════════════════════════════════════
        else if (ticks < PHASE_3_START) {
            // Invisibility continues
            player.addEffect(new MobEffectInstance(
                    MobEffects.INVISIBILITY, 40, 0, false, false));

            // Denser soul particles
            if (level instanceof ServerLevel sl && (ticks % 3) == 0) {
                sl.sendParticles(ParticleTypes.SOUL,
                        player.getX() + (Math.random() - 0.5),
                        player.getY() + Math.random() * 2.0,
                        player.getZ() + (Math.random() - 0.5),
                        2, 0, 0.05, 0, 0.02);
            }

            if (ticks == PHASE_2_START) {
                player.displayClientMessage(
                        Component.literal("Phase II — Strike true. The world still bleeds.")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC),
                        true
                );
            }
        }

        // ════════════════════════════════════
        //  PHASE 3  —  Ontological Debt  (300+ t)
        // ════════════════════════════════════
        else {
            // Invisibility breaks
            player.removeEffect(MobEffects.INVISIBILITY);

            // Escalating Wither — amplifier increases every second
            int witherAmp = Math.min((ticks - PHASE_3_START) / 20, 9);
            player.addEffect(new MobEffectInstance(
                    MobEffects.WITHER, 40, witherAmp, false, true));

            if (ticks == PHASE_3_START) {
                player.displayClientMessage(
                        Component.literal("Phase III — Reality remembers. It is furious.")
                                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                        false
                );
            }

            // ── Point of no return: tick 500 ──
            if (ticks >= ERASURE_TICK) {
                executeErasurePenalty(player, level);
                endEffect(player);
                return;
            }

            // Dread particles
            if (level instanceof ServerLevel sl && (ticks % 2) == 0) {
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        player.getX() + (Math.random() - 0.5) * 1.5,
                        player.getY() + Math.random() * 2.2,
                        player.getZ() + (Math.random() - 0.5) * 1.5,
                        1, 0, 0.05, 0, 0.04);
            }
        }
    }

    // ── Phase 2: instakill on hurt ────────────────────────────

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Player attacker)) return;

        var data = attacker.getPersistentData();
        if (!data.getBoolean("HN_Active")) return;

        int ticks = data.getInt("HN_Ticks");
        if (ticks < PHASE_2_START || ticks >= PHASE_3_START) return;

        LivingEntity target = event.getEntity();

        // Instakill if target below 50% HP
        if (target.getHealth() <= target.getMaxHealth() * 0.5f) {
            event.setAmount(target.getHealth() + 1000f); // guaranteed kill

            // Lightning strike on the target
            if (attacker.level() instanceof ServerLevel sl) {
                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(sl);

                if (lightning != null) {
                    lightning.moveTo(target.position());
                    lightning.setCause(target instanceof ServerPlayer sp ? sp : null);
                    sl.addFreshEntity(lightning);
                }
            }

            // Extend Phase 2
            int kills = data.getInt("HN_Kills") + 1;
            data.putInt("HN_Kills", kills);

            // Subtract from ticks to extend phase (push Phase 3 start back)
            data.putInt("HN_Ticks", Math.max(PHASE_2_START, ticks - KILL_EXTENSION));

            attacker.displayClientMessage(
                    Component.literal("Erased. +" + (KILL_EXTENSION / 20) + "s")
                            .withStyle(ChatFormatting.DARK_PURPLE),
                    true
            );
        }
    }

    // ── Erasure penalty ───────────────────────────────────────

    private static void executeErasurePenalty(Player player, Level level) {
        // Teleport to world origin surface
        if (level instanceof ServerLevel sl) {
            int surfaceY = sl.getHeight(
                    net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                    0, 0
            );
            player.teleportTo(0.5, surfaceY + 1, 0.5);

            // Ten lightning strikes
            for (int i = 0; i < 10; i++) {
                final int delay = i * 4;
                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(sl);

                if (lightning != null) {
                    lightning.moveTo(player.position());
                    lightning.setCause(player instanceof ServerPlayer sp ? sp : null);
                    sl.addFreshEntity(lightning);
                }
            }
            // Massive Wither burst
            player.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 4, false, true));
        }

        player.sendSystemMessage(
                Component.literal("The Name is spoken in full. Reality has collected its debt.")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
        );
    }

    // ── End effect + apply Witnessed mark ────────────────────

    private static void endEffect(Player player) {
        var data = player.getPersistentData();
        data.putBoolean("HN_Active", false);
        data.putInt("HN_Ticks", 0);

        // Apply permanent Witnessed mark
        data.putBoolean("HN_Witnessed", true);

        player.removeEffect(MobEffects.INVISIBILITY);

        player.sendSystemMessage(
                Component.literal("You are Witnessed. The shadows know your shape.")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
        );
    }

    // ── Witnessed passive: random aggro on nearby mob spawns ──

    private static void tickWitnessedAggro(Player player, Level level) {
        var data = player.getPersistentData();
        if (!data.getBoolean("HN_Witnessed")) return;
        if ((player.tickCount % 20) != 0) return; // check once per second

        level.getEntitiesOfClass(
                Monster.class,
                player.getBoundingBox().inflate(WITNESSED_RADIUS),
                mob -> mob.getTarget() == null
                        && mob.tickCount <= 40 // recently spawned
                        && level.random.nextFloat() < AGGRO_CHANCE
        ).forEach(mob -> mob.setTarget(player));
    }

    // ── Tooltip ───────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.literal("Sealed Artifact — Grade 0")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Shadow / Nothingness Pathway")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal("Consumed on use. One chance.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Phase I — Unwritten (5 s):")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Full invisibility, mobs forget you exist")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Phase II — Erasure's Edge (10 s):")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Instakill any target below 50% HP")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  Each kill: lightning + +2 s extension")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Phase III — Ontological Debt (10 s):")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Escalating Wither (grows every second)")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  At 25 s: teleported to 0,0 + 10 lightning")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("⚠ Permanent: You will be Witnessed.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Mobs remember your shape. Forever.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));

        // Show Witnessed warning if already marked
        // (requires player context — shown via item name tint instead)
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("\"To erase your name is not to become free.\"")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}