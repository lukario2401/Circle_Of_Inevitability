package net.lukario.frogerealm.item.seald_artifacts;

import net.lukario.frogerealm.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * ══════════════════════════════════════════════════════════════
 *   THE HANGED MAN'S CHORD  —  Beyonder Characteristic Item
 *   Sequence 1 — Justiciar (Hanged Man Pathway)
 * ══════════════════════════════════════════════════════════════
 *
 * A length of silver rope, three feet long, frayed at both ends.
 * It weighs nothing. It leaves no mark on the skin. But if you try
 * to drop it, you realize — at some point between picking it up
 * and right now — it tied itself to your wrist.
 * You don't remember when.
 *
 * The Justiciar it came from was the last arbiter of a dead god's
 * court. He passed sentence on seventeen thousand souls before the
 * pathway collapsed beneath him. His final judgment was himself.
 * The rope was what remained.
 *
 * A Sequence 1 is not a Beyonder. It is a principle given flesh.
 * The Hanged Man pathway binds, inverts, and corrects.
 * At Sequence 1, those three things become the same thing.
 *
 * TYPE: Carry-on (held in hand — but it will remind you it's there)
 * PATHWAY: Hanged Man (Sacrifice / Reversal / Binding / Judgment)
 * SEQUENCE EQUIVALENT: 1 — Justiciar
 *
 *
 * ── PASSIVE I — Judgment Mark ─────────────────────────────────
 *   Every entity that deals damage to the player is Marked.
 *   Marked entities glow silver (Glowing effect).
 *   Marked entities take +1 damage for every 2 HP they dealt
 *   the player, returned as magic damage over 10 seconds.
 *   (The Chord remembers. It always collects.)
 *   Mark data stored in entity's persistentData: "HC_Mark_dmg".
 *   Max 5 entities marked at once. Oldest mark removed on overflow.
 *
 * ── PASSIVE II — Inversion Field ──────────────────────────────
 *   Within 16 blocks, the Chord inverts momentum:
 *   Any entity that CHARGES toward the player (moving fast and
 *   closing distance) has their velocity halved and direction
 *   partially reversed — they don't stop, but they slow and drift.
 *   Strength: scales with Verdict Charge (see below).
 *   Only triggers on entities moving > 0.3 blocks/tick toward player.
 *
 * ── PASSIVE III — The Hanged Vigil ────────────────────────────
 *   The player cannot be stunned, knocked back, or levitated by
 *   external effects (Levitation removed on application, knockback
 *   resistance set to maximum while held).
 *   The Chord keeps you where judgment placed you.
 *
 * ── PASSIVE IV — Silver Verdict (on kill) ─────────────────────
 *   When the player kills a marked entity:
 *     • Verdict Charge +1 (max 7, stored in "HC_Verdict")
 *     • A burst of silver light erupts (END_ROD particles)
 *     • All OTHER marked entities within 12 blocks receive half
 *       the remaining marked damage instantly (judgment shared)
 *
 * ── ACTIVE (right-click) — Sentence ──────────────────────────
 *   Consumes ALL current Verdict Charge to execute judgment.
 *   Effect scales with charge consumed (1 charge = 1 tier):
 *
 *   Charge 1–2 — Binding Sentence:
 *     Target nearest entity within 20 blocks.
 *     Apply Slowness IV + Mining Fatigue II for 10 s.
 *     Pull target 3 blocks toward player.
 *
 *   Charge 3–4 — Inversion Sentence:
 *     All marked entities in 24 blocks:
 *     Their current velocity is exactly reversed (they fly backward).
 *     Slowness IV + Blindness for 6 s.
 *     Marked damage doubled and applied instantly.
 *
 *   Charge 5–6 — Reversal Sentence:
 *     All entities in 30 blocks (not just marked):
 *     20% of their MAX health dealt as magic damage (unavoidable).
 *     Velocity reversed + Wither II for 8 s.
 *     All marks consumed and explode (full mark damage instantly).
 *
 *   Charge 7 — THE FINAL VERDICT:
 *     Radius 40 blocks. ALL living entities (not player):
 *     40% of max HP magic damage. Velocity reversed.
 *     Wither III + Slowness V + Blindness for 10 s.
 *     All marks explode. Ground shakes (particle eruption).
 *     The Chord whispers the true name of each entity slain.
 *     Costs 200 Suspension immediately (see drawback).
 *
 *   Cooldown after Sentence: 30 s regardless of charge used.
 *
 *
 * ── DRAWBACK — Suspension ─────────────────────────────────────
 *   The Justiciar pathway demands reciprocity.
 *   Every judgment made is also made upon the judge.
 *   The Chord does not distinguish between arbiter and condemned.
 *
 *   Suspension accumulates:
 *     +2 / second held (the rope tightens)
 *     +10 per Judgment Mark applied to player
 *     +30 per Sentence activation
 *     +200 on Final Verdict
 *     -8 / second while NOT held (it loosens slowly)
 *     -50 when the player takes damage of any kind (pain clears debt)
 *
 *   Suspension thresholds:
 *
 *   0–199 — The Vigil: faint silver particles. No drawbacks yet.
 *           The rope is still learning you.
 *
 *   200–399 — First Binding: the rope judges your movement.
 *             Jump height reduced (Slow Falling applied — you don't
 *             fall, you descend. Gravity becomes deliberate).
 *             Attacks deal -1 damage (your judgment is uneven).
 *             Occasional silver flashes in vision (Nausea I, brief).
 *
 *   400–599 — Second Binding: the rope begins marking YOU.
 *             Every 15 s: player receives "self-marked" — 3 magic
 *             damage over 10 s (the chord collects from itself).
 *             Permanent Slowness I. Hunger drains faster.
 *             Player glows silver (permanent Glowing while held).
 *
 *   600–799 — Third Binding: inversion turns inward.
 *             Every 20 s: player velocity reversed for 1 tick
 *             (brief stagger backward — the Chord corrects you).
 *             Permanent Weakness I. Wither I applied every 30 s.
 *             Right-click Sentence costs double Suspension.
 *
 *   800–999 — Judgment Pending: the Chord has found you guilty.
 *             Permanent Wither II while held.
 *             Mining Fatigue III. Cannot sprint.
 *             Every 10 s: one random item in inventory dropped
 *             (the Chord strips the condemned of possessions).
 *             Player takes 2 magic damage every 5 s.
 *             Silver smoke pours from the player constantly.
 *
 *   1000+   — SENTENCE PASSED: the Chord judges the holder.
 *             Item is not destroyed — it cannot be destroyed.
 *             Player is bound: Slowness V + Wither III + Blindness
 *             for 60 s. 15 true damage. Glowing permanently for
 *             60 s (visible to all players on server).
 *             Suspension resets to 600 — you are not released.
 *             You are merely sentenced again later.
 *             The rope never leaves.
 *
 *
 * ── NOTE ON THE ROPE ──────────────────────────────────────────
 *   Unlike most characteristic items, the Chord cannot be dropped.
 *   If the player attempts to toss it (Q key), it reappears in the
 *   first available inventory slot within 2 seconds.
 *   If the player dies, it reappears in inventory on respawn.
 *   It was already tied. It was always going to be.
 *
 *
 * ── STATE (player.getPersistentData()) ────────────────────────
 *   "HC_Suspension"      int     — current suspension level
 *   "HC_Verdict"         int     — current verdict charge (0–7)
 *   "HC_SentenceCD"      int     — ticks until sentence available
 *   "HC_SelfMarkTicks"   int     — ticks until next self-mark tick
 *   "HC_InversionTicks"  int     — ticks until inversion stagger
 *   "HC_WitherTicks"     int     — ticks until wither pulse
 *   "HC_DropCheckTicks"  int     — ticks until item drop check
 *   "HC_NotHeldTicks"    int     — ticks since last held
 *
 *   Per-entity mark data (on entity's persistentData):
 *   "HC_Mark_dmg"        float   — accumulated damage to return
 *   "HC_Mark_ticks"      int     — remaining ticks on mark
 *   "HC_Mark_payTick"    int     — ticks until next mark damage tick
 *
 * ── REGISTRATION ──────────────────────────────────────────────
 *   public static final RegistryObject<Item> HANGED_MAN_CHORD =
 *       ITEMS.register("hanged_man_chord",
 *           () -> new HangedManChord(
 *               new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
 */
@Mod.EventBusSubscriber
public class HangedManChord extends Item {

    // ── Suspension thresholds ─────────────────────────────────
    private static final int SUSP_FIRST    = 200;
    private static final int SUSP_SECOND   = 400;
    private static final int SUSP_THIRD    = 600;
    private static final int SUSP_PENDING  = 800;
    private static final int SUSP_SENTENCE = 1000;

    // ── Verdict charge ────────────────────────────────────────
    private static final int VERDICT_MAX   = 7;

    // ── Cooldowns ─────────────────────────────────────────────
    private static final int SENTENCE_CD       = 600;   // 30 s
    private static final int SELF_MARK_INT     = 300;   // 15 s
    private static final int INVERSION_INT     = 400;   // 20 s
    private static final int WITHER_INT        = 600;   // 30 s
    private static final int DROP_INT          = 200;   // 10 s
    private static final int MARK_DURATION     = 200;   // 10 s
    private static final int MARK_PAY_INTERVAL = 20;    // 1 s
    private static final double INVERSION_RADIUS = 16.0;

    // ── Whispers on Final Verdict ─────────────────────────────
    private static final String[] FINAL_VERDICT_LINES = {
            "Judgment is not cruelty. It is geometry.",
            "Every debt has a shape. Every shape has a weight.",
            "The rope was always here. You only just felt it.",
            "The court is adjourned.",
    };

    public HangedManChord(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // Forces the item to always render with the enchanted glow
    }

    // ═════════════════════════════════════════════════════════
    // ACTIVE — Sentence
    // ═════════════════════════════════════════════════════════
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);

        var data = player.getPersistentData();
        if (data.getInt("HC_SentenceCD") > 0) {
            int remaining = data.getInt("HC_SentenceCD") / 20;
            player.sendSystemMessage(
                    Component.literal("The Chord deliberates. (" + remaining + "s)")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return InteractionResultHolder.pass(stack);
        }

        int charge = data.getInt("HC_Verdict");
        if (charge == 0) {
            player.sendSystemMessage(
                    Component.literal("No verdict has been reached.")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return InteractionResultHolder.pass(stack);
        }

        int suspCost = 30;
        // Third binding doubles cost
        if (data.getInt("HC_Suspension") >= SUSP_THIRD) suspCost *= 2;

        addSuspension(data, suspCost);
        data.putInt("HC_Verdict", 0);
        data.putInt("HC_SentenceCD", SENTENCE_CD);
        player.getCooldowns().addCooldown(this, SENTENCE_CD);

        executeSentence(player, level, charge);
        return InteractionResultHolder.success(stack);
    }

    private static void executeSentence(Player player, Level level, int charge) {
        Vec3 pos = player.position();
        ServerLevel sl = level instanceof ServerLevel ? (ServerLevel) level : null;

        if (charge <= 2) {
            // ── Binding Sentence ──────────────────────────────
            level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(20), e -> e != player
            ).stream().min((a, b) -> Double.compare(
                    a.distanceToSqr(player), b.distanceToSqr(player)
            )).ifPresent(target -> {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 3, false, true));
                target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,      200, 1, false, true));
                // Pull toward player
                Vec3 pull = pos.subtract(target.position()).normalize().scale(0.8);
                target.setDeltaMovement(target.getDeltaMovement().add(pull));
                target.hurtMarked = true;
            });
            player.sendSystemMessage(
                    Component.literal("Binding Sentence. Approach is denied.")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

        } else if (charge <= 4) {
            // ── Inversion Sentence ────────────────────────────
            level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(24), e -> e != player
            ).forEach(e -> {
                if (e.getPersistentData().contains("HC_Mark_dmg")) {
                    float markDmg = e.getPersistentData().getFloat("HC_Mark_dmg") * 2;
                    e.hurt(level.damageSources().magic(), markDmg);
                    clearMark(e);
                }
                Vec3 vel = e.getDeltaMovement();
                e.setDeltaMovement(vel.scale(-1.4));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3, false, true));
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,         120, 0, false, true));
            });
            player.sendSystemMessage(
                    Component.literal("Inversion Sentence. Every direction is wrong.")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

        } else if (charge <= 6) {
            // ── Reversal Sentence ─────────────────────────────
            level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(30), e -> e != player
            ).forEach(e -> {
                float maxHp = e.getMaxHealth();
                e.hurt(level.damageSources().magic(), maxHp * 0.20f);
                Vec3 vel = e.getDeltaMovement();
                e.setDeltaMovement(vel.scale(-1.6));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.WITHER,            160, 1, false, true));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 4, false, true));
                // Explode marks
                if (e.getPersistentData().contains("HC_Mark_dmg")) {
                    e.hurt(level.damageSources().magic(),
                            e.getPersistentData().getFloat("HC_Mark_dmg"));
                    clearMark(e);
                }
            });
            if (sl != null) silverExplosion(sl, pos, 30, 60);
            player.sendSystemMessage(
                    Component.literal("Reversal Sentence. The debt is paid in full.")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));

        } else {
            // ── FINAL VERDICT ─────────────────────────────────
            addSuspension(player.getPersistentData(), 200);

            level.getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(40), e -> e != player
            ).forEach(e -> {
                float maxHp = e.getMaxHealth();
                e.hurt(level.damageSources().magic(), maxHp * 0.40f);
                Vec3 vel = e.getDeltaMovement();
                e.setDeltaMovement(vel.scale(-1.8));
                e.hurtMarked = true;
                e.addEffect(new MobEffectInstance(MobEffects.WITHER,            200, 2, false, true));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 4, false, true));
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,         200, 0, false, true));
                if (e.getPersistentData().contains("HC_Mark_dmg")) {
                    e.hurt(level.damageSources().magic(),
                            e.getPersistentData().getFloat("HC_Mark_dmg"));
                    clearMark(e);
                }
            });

            if (sl != null) {
                silverExplosion(sl, pos, 40, 120);
                // Ground shockwave ring
                for (int i = 0; i < 48; i++) {
                    double angle = (Math.PI * 2.0 / 48) * i;
                    sl.sendParticles(ParticleTypes.END_ROD,
                            pos.x + Math.cos(angle) * 18, pos.y + 0.2,
                            pos.z + Math.sin(angle) * 18,
                            1, 0, 0.05, 0, 0.1);
                }
            }

            // Whispers
            for (String line : FINAL_VERDICT_LINES) {
                player.sendSystemMessage(
                        Component.literal(line)
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
            player.sendSystemMessage(
                    Component.literal("THE FINAL VERDICT")
                            .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        }
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

        var data = player.getPersistentData();
        boolean holding = isHolding(player);

        // ── Rope returns to inventory if dropped ──────────────
//        ensureRopeReturns(player, level, data);

        // ── Tick cooldowns ────────────────────────────────────
        tickDown(data, "HC_SentenceCD");

        // ── Mark payback tick (always, even when not held) ────
        tickMarkPayback(player, level);

        if (!holding) {
            // Suspension decays faster while not held
            int notHeld = data.getInt("HC_NotHeldTicks") + 1;
            data.putInt("HC_NotHeldTicks", notHeld);
            if ((player.tickCount % 20) == 0) {
                int susp = data.getInt("HC_Suspension");
                if (susp > 0) data.putInt("HC_Suspension", Math.max(0, susp - 8));
            }
            return;
        }
        data.putInt("HC_NotHeldTicks", 0);

        // ── Suspension accrual: +2/s ──────────────────────────
        if ((player.tickCount % 20) == 0) {
            addSuspension(data, 2);
        }

        int susp = data.getInt("HC_Suspension");

        // ── Sentence passed check ─────────────────────────────
        if (susp >= SUSP_SENTENCE) {
            executeSentenceOnHolder(player, level, data);
            return;
        }

        // ── Base passives ─────────────────────────────────────
        applyBasePassives(player, level);

        // ── Inversion field ───────────────────────────────────
        if ((player.tickCount % 5) == 0) {
            tickInversionField(player, level, susp);
        }

        // ── Suspension drawbacks ──────────────────────────────
        applyDrawbacks(player, level, data, susp);

        // ── Ambient particles ─────────────────────────────────
        if (level instanceof ServerLevel sl && (player.tickCount % 20) == 0) {
            Vec3 p = player.position();
            sl.sendParticles(ParticleTypes.END_ROD,
                    p.x + (Math.random() - 0.5) * 0.5,
                    p.y + 1.0 + Math.random() * 0.8,
                    p.z + (Math.random() - 0.5) * 0.5,
                    1, 0, 0.02, 0, 0.02);
        }
        if (susp%50==0 && susp != 0){
            player.sendSystemMessage(Component.literal("Suspension: " + susp));
            data.putInt("HC_Suspension",data.getInt("HC_Suspension")+1);
        }
    }

    // ── Base passives ─────────────────────────────────────────
    private static void applyBasePassives(Player player, Level level) {
        // Hanged Vigil: resist knockback + levitation
        player.removeEffect(MobEffects.LEVITATION);
        // Note: full knockback resistance requires attribute modification
        // which is handled best via equipment attributes, but we suppress
        // levitation and add resistance to mimic the vigil:
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false));
    }

    // ── Inversion field ───────────────────────────────────────
    private static void tickInversionField(Player player, Level level, int susp) {
        Vec3 playerPos = player.position();
        double threshold = 0.30;
        float strength = 0.5f + (susp / 1000f) * 0.5f; // scales with suspension

        level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(INVERSION_RADIUS),
                e -> e != player
        ).forEach(e -> {
            Vec3 toPlayer = playerPos.subtract(e.position());
            double dist   = toPlayer.length();
            if (dist < 0.5 || dist > INVERSION_RADIUS) return;

            Vec3 vel = e.getDeltaMovement();
            // Dot product: positive = moving toward player
            double dot = vel.dot(toPlayer.normalize());
            double speed = vel.length();

            if (dot > 0 && speed > threshold) {
                // Halve forward velocity and partially push back
                Vec3 newVel = vel.scale(0.5).add(toPlayer.normalize().scale(-strength * 0.2));
                e.setDeltaMovement(newVel);
                e.hurtMarked = true;
            }
        });
    }

    // ── Mark payback (entity persistentData) ──────────────────
    private static void tickMarkPayback(Player player, Level level) {
        level.getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(64), // generous range
                e -> e.getPersistentData().contains("HC_Mark_ticks")
        ).forEach(e -> {
            var ed = e.getPersistentData();
            int ticks = ed.getInt("HC_Mark_ticks") - 1;
            if (ticks <= 0) {
                clearMark(e);
                return;
            }
            ed.putInt("HC_Mark_ticks", ticks);

            // Pay damage in intervals
            int payTick = ed.getInt("HC_Mark_payTick") - 1;
            if (payTick <= 0) {
                ed.putInt("HC_Mark_payTick", MARK_PAY_INTERVAL);
                float total = ed.getFloat("HC_Mark_dmg");
                float perTick = total / (MARK_DURATION / MARK_PAY_INTERVAL);
                e.hurt(level.damageSources().magic(), perTick);
                if (level instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.END_ROD,
                            e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ(),
                            3, 0.2, 0.2, 0.2, 0.05);
                }
            } else {
                ed.putInt("HC_Mark_payTick", payTick);
            }
        });
    }

    // ── Drawback tiers ────────────────────────────────────────
    private static void applyDrawbacks(Player player, Level level,
                                       net.minecraft.nbt.CompoundTag data, int susp) {
        // First Binding (200+)
        if (susp >= SUSP_FIRST) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false));
            // Nausea flash
            if ((player.tickCount % 600) == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0, false, true));
            }
        }

        // Second Binding (400+)
        if (susp >= SUSP_SECOND) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING,           40, 0, false, false));
            // Hunger drain
            if ((player.tickCount % 20) == 0) {
                player.getFoodData().setFoodLevel(
                        Math.max(0, player.getFoodData().getFoodLevel() - 1));
            }
            // Self-mark
            int selfMark = data.getInt("HC_SelfMarkTicks") - 1;
            if (selfMark <= 0) {
                data.putInt("HC_SelfMarkTicks", SELF_MARK_INT);
                player.hurt(level.damageSources().magic(), 3f);
                player.sendSystemMessage(
                        Component.literal("The Chord collects.")
                                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            } else {
                data.putInt("HC_SelfMarkTicks", selfMark);
            }
        }

        // Third Binding (600+)
        if (susp >= SUSP_THIRD) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
            // Inversion stagger
            int invTicks = data.getInt("HC_InversionTicks") - 1;
            if (invTicks <= 0) {
                data.putInt("HC_InversionTicks", INVERSION_INT);
                Vec3 vel = player.getDeltaMovement();
                player.setDeltaMovement(vel.scale(-0.8).add(0, 0.1, 0));
                player.hurtMarked = true;
                player.sendSystemMessage(
                        Component.literal("The Chord corrects your path.")
                                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            } else {
                data.putInt("HC_InversionTicks", invTicks);
            }
            // Wither pulse
            int witherTicks = data.getInt("HC_WitherTicks") - 1;
            if (witherTicks <= 0) {
                data.putInt("HC_WitherTicks", WITHER_INT);
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0, false, true));
            } else {
                data.putInt("HC_WitherTicks", witherTicks);
            }
        }

        // Judgment Pending (800+)
        if (susp >= SUSP_PENDING) {
            player.addEffect(new MobEffectInstance(MobEffects.WITHER,      40, 1, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN,40, 2, false, false));
            if (player.isSprinting()) player.setSprinting(false);

            // Random item drop every 10 s
            int dropTick = data.getInt("HC_DropCheckTicks") - 1;
            if (dropTick <= 0) {
                data.putInt("HC_DropCheckTicks", DROP_INT);
                dropRandomItem(player, level);
            } else {
                data.putInt("HC_DropCheckTicks", dropTick);
            }

            // Damage every 5 s
            if ((player.tickCount % 100) == 0) {
                player.hurt(level.damageSources().magic(), 2f);
                // Dense smoke particles
                if (level instanceof ServerLevel sl) {
                    Vec3 p = player.position();
                    sl.sendParticles(ParticleTypes.END_ROD,
                            p.x, p.y + 1, p.z, 12, 0.4, 0.6, 0.4, 0.1);
                }
            }
        }
    }

    // ── Sentence on holder ────────────────────────────────────
    private static void executeSentenceOnHolder(Player player, Level level,
                                                net.minecraft.nbt.CompoundTag data) {
        player.hurt(level.damageSources().magic(), 30f); // 15 hearts
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1200, 4, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.WITHER,            1200, 2, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,         1200, 0, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING,           1200, 0, false, true));

        // Does not destroy — resets to 600 (Third Binding)
        data.putInt("HC_Suspension", SUSP_THIRD);

        if (level instanceof ServerLevel sl) {
            silverExplosion(sl, player.position(), 15, 80);
        }

        player.sendSystemMessage(Component.literal("══════════════════════════════════").withStyle(ChatFormatting.WHITE));
        player.sendSystemMessage(Component.literal("SENTENCE PASSED")
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        player.sendSystemMessage(Component.literal("You are not released. You are sentenced again.")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal("The rope was always tied.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        player.sendSystemMessage(Component.literal("══════════════════════════════════").withStyle(ChatFormatting.WHITE));
    }

    // ── Incoming damage: apply Judgment Mark ──────────────────
    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!isHolding(player)) return;

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof LivingEntity le)) return;

        float dmg = event.getAmount();
        var data = player.getPersistentData();

        // Apply mark to attacker
        var ed = le.getPersistentData();
        float existing = ed.getFloat("HC_Mark_dmg");
        // Return damage = half the damage dealt
        ed.putFloat("HC_Mark_dmg",  existing + dmg * 0.5f);
        ed.putInt("HC_Mark_ticks",  MARK_DURATION);
        ed.putInt("HC_Mark_payTick",MARK_PAY_INTERVAL);
        le.addEffect(new MobEffectInstance(MobEffects.GLOWING, MARK_DURATION, 0, false, false));

        // Suspension penalty for being hit
        addSuspension(data, 10);
        // Pain clears some suspension too
        int susp = data.getInt("HC_Suspension");
        data.putInt("HC_Suspension", Math.max(0, susp - 50));
    }

    // ── Verdict charge on kill ────────────────────────────────
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide) return;

        // Find the player who killed it
        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof Player player)) return;
        if (!isHolding(player)) return;

        var data = player.getPersistentData();
        boolean wasMarked = dead.getPersistentData().contains("HC_Mark_dmg");

        if (wasMarked) {
            int verdict = Math.min(VERDICT_MAX, data.getInt("HC_Verdict") + 1);
            data.putInt("HC_Verdict", verdict);

            // Silver burst
            if (dead.level() instanceof ServerLevel sl) {
                silverExplosion(sl, dead.position(), 6, 20);
            }

            player.sendSystemMessage(
                    Component.literal("Verdict recorded. [" + verdict + "/" + VERDICT_MAX + "]")
                            .withStyle(ChatFormatting.WHITE, ChatFormatting.ITALIC));

            // Splash mark damage to other nearby marked entities
            dead.level().getEntitiesOfClass(LivingEntity.class,
                    dead.getBoundingBox().inflate(12),
                    e -> e != dead && e != player && e.getPersistentData().contains("HC_Mark_dmg")
            ).forEach(e -> {
                float spillDmg = dead.getPersistentData().getFloat("HC_Mark_dmg") * 0.5f;
                e.hurt(dead.level().damageSources().magic(), spillDmg);
            });

            clearMark(dead);
        }
    }

    // ── Drop random item (Judgment Pending) ───────────────────
    private static void dropRandomItem(Player player, Level level) {
        var inv = player.getInventory();
        // Find a random non-empty slot (skip hotbar slot 0 = held item)
        for (int attempt = 0; attempt < 10; attempt++) {
            int slot = level.random.nextInt(inv.getContainerSize());
            ItemStack stack = inv.getItem(slot);
            if (!stack.isEmpty() && !(stack.getItem() instanceof HangedManChord)) {
                player.drop(stack.copy(), false);
                inv.setItem(slot, ItemStack.EMPTY);
                player.sendSystemMessage(
                        Component.literal("The Chord strips the condemned.")
                                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                return;
            }
        }
    }

    // ── Rope returns to inventory ─────────────────────────────
    private static void ensureRopeReturns(Player player, Level level, net.minecraft.nbt.CompoundTag data) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof HangedManChord) return;
        }

        // Directly grab the registered item from your DeferredRegister
        player.addItem(new ItemStack(ModItems.HANGED_MAN_CHORD.get()));

        player.sendSystemMessage(
                Component.literal("It was already tied.")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    // ── Helpers ───────────────────────────────────────────────
    private static void clearMark(LivingEntity e) {
        e.getPersistentData().remove("HC_Mark_dmg");
        e.getPersistentData().remove("HC_Mark_ticks");
        e.getPersistentData().remove("HC_Mark_payTick");
    }

    private static void addSuspension(net.minecraft.nbt.CompoundTag data, int amount) {
        int s = data.getInt("HC_Suspension");
        data.putInt("HC_Suspension", Math.min(SUSP_SENTENCE + 100, s + amount));
    }

    private static void tickDown(net.minecraft.nbt.CompoundTag data, String key) {
        int v = data.getInt(key);
        if (v > 0) data.putInt(key, v - 1);
    }

    private static boolean isHolding(Player player) {
        for (InteractionHand hand : InteractionHand.values())
            if (player.getItemInHand(hand).getItem() instanceof HangedManChord) return true;
        return false;
    }

    private static void silverExplosion(ServerLevel sl, Vec3 pos, int radius, int count) {
        for (int i = 0; i < count; i++) {
            double dx = (sl.random.nextDouble() - 0.5) * radius * 0.5;
            double dy = sl.random.nextDouble() * radius * 0.3;
            double dz = (sl.random.nextDouble() - 0.5) * radius * 0.5;
            sl.sendParticles(ParticleTypes.END_ROD,
                    pos.x + dx, pos.y + 1 + dy, pos.z + dz,
                    1, 0, 0, 0, 0.15);
        }
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                pos.x, pos.y + 1, pos.z, 1, 0, 0, 0, 0);
    }

    // ── Tooltip ───────────────────────────────────────────────
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);

        tooltip.add(Component.literal("Beyonder Characteristic — Sequence 1")
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        tooltip.add(Component.literal("Justiciar  |  Hanged Man Pathway")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Judgment Mark:")
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Attackers are Marked. Return 50% damage over 10 s.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Inversion Field (16 blocks):")
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Charging entities are slowed and deflected.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — The Hanged Vigil:")
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Immune to Levitation. Knockback resisted.")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Passive — Silver Verdict (on kill):")
                .withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Killing a marked entity: +1 Verdict Charge (max 7).")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("Right-click — Sentence (30 s cooldown):")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Charge 1–2: Binding. Charge 3–4: Inversion.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("  Charge 5–6: Reversal. Charge 7: Final Verdict.")
                .withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("⚠ Drawback — Suspension:")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  +2/s held. +10 per hit taken. +30 per Sentence.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  200 — Slow falling, movement dulled, Nausea.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  400 — Self-marks every 15 s. Glow. Hunger drain.")
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.literal("  600 — Stagger. Wither pulses. Weakness.")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  800 — Wither II, Fatigue III. Items drop randomly.")
                .withStyle(ChatFormatting.DARK_RED));
        tooltip.add(Component.literal("  1000 — SENTENCE PASSED. Resets to 600. Forever.")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        tooltip.add(Component.literal("  Rope cannot be dropped. It was already tied.")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.literal(" "));
        tooltip.add(Component.literal("\"It doesn't tie itself. You just finally noticed.\"")
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
