package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runic Sequencer
 * -----------------------------------------------
 * Theme: ritual combat, ordered casting, perfect sequences.
 * Abilities MUST be used in the correct order.
 * Completing the full 5-step sequence = massive payoff.
 *
 * Sequence steps (in order):
 *   1. MARK    — tag enemies, store initial energy
 *   2. BIND    — root marked enemies, increase energy
 *   3. AMPLIFY — increase damage taken, stack energy
 *   4. CONVERGE— pull together, compress energy
 *   5. EXECUTE — release, massive AOE burst
 *
 * Wrong order → reset. Too slow → reset. Full sequence → auto-reset.
 *
 * Player NBT keys:
 *   "RuneStep"          → int    current sequence step (0=none, 1–5)
 *   "RuneEnergy"        → float  stored energy (scales with steps done)
 *   "RuneStepTimer"     → int    ticks until sequence resets from inactivity
 *   "RuneMarkedTargets" → ListTag of UUIDs that have been marked
 *   "RuneLoops"         → int    loops completed during Perfect Ritual
 *   "RuneRitual"        → int    ticks remaining on Perfect Ritual
 *   "RuneSkipCharge"    → int    Quick Inscription charges (max 1, regens)
 *   "RuneSkipCD"        → int    ticks until Skip charge refills
 *
 * Target NBT (on the LivingEntity):
 *   "RuneMarked"        → byte   1 = marked
 *   "RuneBound"         → int    ticks remaining on bind (root)
 *   "RuneAmplified"     → int    ticks remaining on amplify (+damage taken)
 *   "RuneAmplifyPower"  → float  damage amplification multiplier
 *   "RuneOwner"         → UUID   player UUID who placed the rune
 */
public class RunicSequencer {

    // ─── Step constants ───────────────────────────────────────────────────────
    public static final int STEP_NONE     = 0;
    public static final int STEP_MARK     = 1;
    public static final int STEP_BIND     = 2;
    public static final int STEP_AMPLIFY  = 3;
    public static final int STEP_CONVERGE = 4;
    public static final int STEP_EXECUTE  = 5;

    private static final String[] STEP_NAMES = {
            "§7[None]", "§b[I: Mark]", "§a[II: Bind]",
            "§e[III: Amplify]", "§6[IV: Converge]", "§c[V: Execute]"
    };

    // ─── NBT keys ─────────────────────────────────────────────────────────────
    private static final String NBT_STEP         = "RuneStep";
    private static final String NBT_ENERGY       = "RuneEnergy";
    private static final String NBT_STEP_TIMER   = "RuneStepTimer";
    private static final String NBT_TARGETS      = "RuneMarkedTargets";
    private static final String NBT_LOOPS        = "RuneLoops";
    private static final String NBT_RITUAL       = "RuneRitual";
    private static final String NBT_SKIP_CHARGE  = "RuneSkipCharge";
    private static final String NBT_SKIP_CD      = "RuneSkipCD";

    // ─── Timing ───────────────────────────────────────────────────────────────
    private static final int STEP_TIMEOUT   = 200;  // 10s to use next ability
    private static final int RITUAL_DUR     = 240;  // 12 seconds
    private static final int SKIP_CD        = 400;  // 20s for skip recharge
    private static final float ENERGY_PER_STEP = 20f; // base energy gained per step

    // ─── Helpers ──────────────────────────────────────────────────────────────

    public static int getStep(Player player) {
        return player.getPersistentData().getInt(NBT_STEP);
    }

    private static void setStep(Player player, int step) {
        player.getPersistentData().putInt(NBT_STEP, step);
        // Reset inactivity timer whenever step changes
        player.getPersistentData().putInt(NBT_STEP_TIMER, STEP_TIMEOUT);
    }

    public static float getEnergy(Player player) {
        return player.getPersistentData().getFloat(NBT_ENERGY);
    }

    private static void addEnergy(Player player, float amount) {
        player.getPersistentData().putFloat(NBT_ENERGY,
                player.getPersistentData().getFloat(NBT_ENERGY) + amount);
    }

    public static boolean inRitual(Player player) {
        return player.getPersistentData().getInt(NBT_RITUAL) > 0;
    }

    /** Resets the sequence back to step 0, clears energy and targets. */
    private static void resetSequence(Player player, ServerLevel sl, boolean wasWrong) {
        player.getPersistentData().putInt(NBT_STEP, STEP_NONE);
        player.getPersistentData().putFloat(NBT_ENERGY, 0f);
        player.getPersistentData().putInt(NBT_STEP_TIMER, 0);
        clearMarkedTargets(player, sl);

        if (wasWrong && player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§cSequence broken. §7The ritual must be performed in order."));
    }

    /** Returns current step display string. */
    private static String sequenceStatus(Player player) {
        int step = getStep(player);
        float energy = getEnergy(player);
        int loops = player.getPersistentData().getInt(NBT_LOOPS);
        String loopStr = loops > 0 ? " §5[Loop " + loops + "]" : "";
        return STEP_NAMES[step] + " §fEnergy: §b" + String.format("%.0f", energy) + loopStr;
    }

    // ─── Target tracking ──────────────────────────────────────────────────────

    private static void addMarkedTarget(Player player, LivingEntity target) {
        ListTag list = getMarkedList(player);
        // Don't double-add
        for (int i = 0; i < list.size(); i++) {
            if (list.getCompound(i).getUUID("UUID").equals(target.getUUID())) return;
        }
        CompoundTag entry = new CompoundTag();
        entry.putUUID("UUID", target.getUUID());
        list.add(entry);
        player.getPersistentData().put(NBT_TARGETS, list);
    }

    private static ListTag getMarkedList(Player player) {
        var data = player.getPersistentData();
        if (!data.contains(NBT_TARGETS)) data.put(NBT_TARGETS, new ListTag());
        return data.getList(NBT_TARGETS, Tag.TAG_COMPOUND);
    }

    private static List<LivingEntity> getMarkedEntities(Player player, ServerLevel sl) {
        ListTag list = getMarkedList(player);
        List<LivingEntity> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            UUID uuid = list.getCompound(i).getUUID("UUID");
            sl.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(40),
                    e -> e.getUUID().equals(uuid) && e.isAlive())
                    .stream().findFirst().ifPresent(result::add);
        }
        return result;
    }

    private static void clearMarkedTargets(Player player, ServerLevel sl) {
        // Remove rune NBT from all targets
        List<LivingEntity> targets = getMarkedEntities(player, sl);
        for (LivingEntity e : targets) {
            e.getPersistentData().remove("RuneMarked");
            e.getPersistentData().remove("RuneBound");
            e.getPersistentData().remove("RuneAmplified");
            e.getPersistentData().remove("RuneAmplifyPower");
            e.getPersistentData().remove("RuneOwner");
        }
        player.getPersistentData().put(NBT_TARGETS, new ListTag());
    }

    // ─── Step validation ──────────────────────────────────────────────────────

    /**
     * Returns true if using the given step is valid right now.
     * In Perfect Ritual, wrong steps are forgiven — sequence auto-advances.
     */
    private static boolean validateStep(Player player, int requiredPrevStep,
                                        ServerLevel sl) {
        int current = getStep(player);

        // Perfect Ritual: auto-advance, no reset on wrong order
        if (inRitual(player)) return true;

        if (current != requiredPrevStep) {
            resetSequence(player, sl, true);
            return false;
        }
        return true;
    }

    // ─── Ray-cast helper ──────────────────────────────────────────────────────

    private static LivingEntity rayCastFirst(Player player, Level level, int blocks) {
        Vec3 start = player.getEyePosition();
        Vec3 dir   = player.getLookAngle().normalize();
        Vec3 cur   = start;
        for (int i = 0; i < blocks; i++) {
            cur = cur.add(dir);
            List<LivingEntity> hits = level.getEntitiesOfClass(
                    LivingEntity.class, new AABB(cur, cur).inflate(0.5),
                    e -> e != player && e.isAlive());
            if (!hits.isEmpty()) return hits.get(0);
        }
        return null;
    }

    // =========================================================================
    //  EVENTS
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class RuneEvents {

        @SubscribeEvent
        public static void onRunePlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Runic Sequencer")) return;

            // ── Perfect Ritual countdown ──────────────────────────────────────
            int ritual = player.getPersistentData().getInt(NBT_RITUAL);
            if (ritual > 0) {
                player.getPersistentData().putInt(NBT_RITUAL, ritual - 1);
                if (player.tickCount % 6 == 0)
                    sl.sendParticles(ParticleTypes.ENCHANT,
                            player.getX(), player.getY() + 1, player.getZ(),
                            4, 0.4, 0.4, 0.4, 0.03);
            }

            // ── Skip charge recharge ──────────────────────────────────────────
            int skipCD = player.getPersistentData().getInt(NBT_SKIP_CD);
            if (skipCD > 0) {
                skipCD--;
                player.getPersistentData().putInt(NBT_SKIP_CD, skipCD);
                if (skipCD == 0 && player.getPersistentData().getInt(NBT_SKIP_CHARGE) == 0) {
                    player.getPersistentData().putInt(NBT_SKIP_CHARGE, 1);
                    if (player instanceof ServerPlayer sp)
                        sp.sendSystemMessage(Component.literal("§aQuick Inscription ready."));
                }
            }

            // ── Sequence inactivity timeout ───────────────────────────────────
            int step = getStep(player);
            if (step > STEP_NONE && step < STEP_EXECUTE) {
                int timer = player.getPersistentData().getInt(NBT_STEP_TIMER);
                timer--;
                player.getPersistentData().putInt(NBT_STEP_TIMER, timer);
                if (timer <= 0 && !inRitual(player)) {
                    resetSequence(player, sl, false);
                    if (player instanceof ServerPlayer sp)
                        sp.sendSystemMessage(Component.literal(
                                "§7Sequence faded. §8The ritual requires momentum."));
                }
            }

            // ── Bound entity tick (root) ──────────────────────────────────────
            List<LivingEntity> targets = getMarkedEntities(player, sl);
            for (LivingEntity e : targets) {
                int bound = e.getPersistentData().getInt("RuneBound");
                if (bound > 0) {
                    e.getPersistentData().putInt("RuneBound", bound - 1);
                    // Re-apply slow/root every tick so movement is truly locked
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 10, true, false));
                    e.setDeltaMovement(0, e.getDeltaMovement().y, 0);
                }

                int amp = e.getPersistentData().getInt("RuneAmplified");
                if (amp > 0) {
                    e.getPersistentData().putInt("RuneAmplified", amp - 1);
                    if (player.tickCount % 10 == 0)
                        sl.sendParticles(ParticleTypes.ENCHANT,
                                e.getX(), e.getY() + 1, e.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
                }
            }

            // ── Perfect Ritual auto-sequence ──────────────────────────────────
            // During ritual, sequence loops automatically every 2s if player has targets
            if (inRitual(player) && player.tickCount % 40 == 0 && !targets.isEmpty()) {
                int curStep = getStep(player);
                // Auto-advance to next step
                if (curStep == STEP_NONE || curStep == STEP_EXECUTE) {
                    setStep(player, STEP_MARK);
                }
            }
        }
    }

    // =========================================================================
    //  ABILITY 1 — SIGIL: MARK
    // =========================================================================

    /**
     * Tags up to 5 enemies in a cone in front of you.
     * Begins the sequence. Stores initial energy.
     * Can be used at any step (always valid as the start or restart).
     * Cost: 300 essence.
     */
    public static void runicSequencerSigilMark(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Runic Sequencer")) return;
        if (SoulCore.getSoulEssence(player) < 300) return;

        // Mark is always valid — it restarts the sequence if called out of order
        // (unless in ritual, where it just advances)
        if (getStep(player) != STEP_NONE && !inRitual(player)) {
            resetSequence(player, sl, false);
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 300);

        int stage = SoulCore.getAscensionStage(player);
        clearMarkedTargets(player, sl);

        // Mark enemies in a wide cone (±45°, 15 blocks)
        Vec3 dir   = player.getLookAngle().normalize();
        AABB cone  = player.getBoundingBox().inflate(15 + stage);
        int marked = 0;

        List<LivingEntity> candidates = level.getEntitiesOfClass(
                LivingEntity.class, cone, e -> e != player && e.isAlive());

        for (LivingEntity e : candidates) {
            if (marked >= 3 + stage) break;
            Vec3 toTarget = e.position().subtract(player.position()).normalize();
            double dot = dir.dot(toTarget);
            if (dot < 0.5) continue; // outside ±60° cone

            addMarkedTarget(player, e);
            e.getPersistentData().putByte("RuneMarked", (byte) 1);
            e.getPersistentData().putUUID("RuneOwner", player.getUUID());
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, STEP_TIMEOUT, 0, false, false));

            sl.sendParticles(ParticleTypes.ENCHANT,
                    e.getX(), e.getY() + 1, e.getZ(), 8, 0.4, 0.4, 0.4, 0.04);
            marked++;
        }

        if (marked == 0) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo targets in range for Mark."));
            return;
        }

        // Store initial energy
        float energyGain = ENERGY_PER_STEP * (1 + stage * 0.1f) * (inRitual(player) ? 1.3f : 1f);
        player.getPersistentData().putFloat(NBT_ENERGY, 0f); // fresh start
        addEnergy(player, energyGain);

        setStep(player, STEP_MARK);

        sl.playSound(null, player.blockPosition(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8f, 1.4f);
        level.playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.5f, 1.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§b[I] Mark: §f" + marked + " targets marked. " + sequenceStatus(player)));
    }

    // =========================================================================
    //  ABILITY 2 — SIGIL: BIND
    // =========================================================================

    /**
     * Roots all marked enemies for 3 seconds. Increases stored energy.
     * ONLY works if Mark was the previous step.
     * Cost: 400 essence.  Requires stage ≥ 1.
     */
    public static void runicSequencerSigilBind(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Runic Sequencer")) return;
        if (SoulCore.getSoulEssence(player) < 400) return;
        if (SoulCore.getAscensionStage(player) < 1) return;
        if (!validateStep(player, STEP_MARK, sl)) return;

        List<LivingEntity> targets = getMarkedEntities(player, sl);
        if (targets.isEmpty()) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo marked targets for Bind!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 400);

        int stage     = SoulCore.getAscensionStage(player);
        int bindTicks = 60 + stage * 10; // 3–4.5 seconds

        for (LivingEntity e : targets) {
            e.getPersistentData().putInt("RuneBound", bindTicks);
            sl.sendParticles(ParticleTypes.WITCH,
                    e.getX(), e.getY() + 1, e.getZ(), 10, 0.4, 0.4, 0.4, 0.04);
        }

        float energyGain = ENERGY_PER_STEP * targets.size() * (inRitual(player) ? 1.3f : 1f);
        addEnergy(player, energyGain);
        setStep(player, STEP_BIND);

        level.playSound(null, player.blockPosition(),
                SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6f, 0.8f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§a[II] Bind: §f" + targets.size() + " targets rooted. " + sequenceStatus(player)));
    }

    // =========================================================================
    //  ABILITY 3 — SIGIL: AMPLIFY
    // =========================================================================

    /**
     * Amplifies all marked+bound targets — they take increased damage for 6s.
     * ONLY works if Bind was the previous step.
     * Cost: 500 essence.  Requires stage ≥ 2.
     */
    public static void runicSequencerSigilAmplify(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Runic Sequencer")) return;
        if (SoulCore.getSoulEssence(player) < 500) return;
        if (SoulCore.getAscensionStage(player) < 2) return;
        if (!validateStep(player, STEP_BIND, sl)) return;

        List<LivingEntity> targets = getMarkedEntities(player, sl);
        if (targets.isEmpty()) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo bound targets for Amplify!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 500);

        int   stage      = SoulCore.getAscensionStage(player);
        float ampPower   = 1.3f + stage * 0.05f; // 1.3× → 1.65× at stage 7
        int   ampTicks   = 120 + stage * 10;

        for (LivingEntity e : targets) {
            e.getPersistentData().putInt("RuneAmplified", ampTicks);
            e.getPersistentData().putFloat("RuneAmplifyPower", ampPower);
            sl.sendParticles(ParticleTypes.CRIT,
                    e.getX(), e.getY() + 1, e.getZ(), 12, 0.4, 0.4, 0.4, 0.05);
        }

        float energyGain = ENERGY_PER_STEP * targets.size() * ampPower * (inRitual(player) ? 1.3f : 1f);
        addEnergy(player, energyGain);
        setStep(player, STEP_AMPLIFY);

        level.playSound(null, player.blockPosition(),
                SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.6f, 1.2f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§e[III] Amplify: §f×" + String.format("%.2f", ampPower)
                    + " damage on §e" + targets.size() + "§f targets. " + sequenceStatus(player)));
    }

    // =========================================================================
    //  ABILITY 4 — SIGIL: CONVERGE
    // =========================================================================

    /**
     * Pulls all marked targets toward the center point between them.
     * ONLY works if Amplify was the previous step.
     * Cost: 600 essence.  Requires stage ≥ 3.
     */
    public static void runicSequencerSigilConverge(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Runic Sequencer")) return;
        if (SoulCore.getSoulEssence(player) < 600) return;
        if (SoulCore.getAscensionStage(player) < 3) return;
        if (!validateStep(player, STEP_AMPLIFY, sl)) return;

        List<LivingEntity> targets = getMarkedEntities(player, sl);
        if (targets.isEmpty()) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo amplified targets for Converge!"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 600);

        // Find centroid of all targets
        Vec3 center = Vec3.ZERO;
        for (LivingEntity e : targets)
            center = center.add(e.position());
        center = center.scale(1.0 / targets.size());

        final Vec3 finalCenter = center;

        for (LivingEntity e : targets) {
            Vec3 pull = finalCenter.subtract(e.position()).normalize().scale(1.8);
            e.setDeltaMovement(pull.x, 0.3, pull.z);
            e.hurtMarked = true;
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    e.getX(), e.getY() + 1, e.getZ(), 8, 0.3, 0.3, 0.3, 0.03);
        }

        // Particle vortex at center
        for (int i = 0; i < 20; i++) {
            double angle = (2 * Math.PI / 20) * i;
            double r = 2.0;
            sl.sendParticles(ParticleTypes.ENCHANT,
                    center.x + r * Math.cos(angle), center.y + 1,
                    center.z + r * Math.sin(angle), 1, 0, 0.1, 0, 0.03);
        }

        float energyGain = ENERGY_PER_STEP * targets.size() * 1.5f * (inRitual(player) ? 1.3f : 1f);
        addEnergy(player, energyGain);
        setStep(player, STEP_CONVERGE);

        level.playSound(null, finalCenter.x, finalCenter.y, finalCenter.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6f, 1.8f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§6[IV] Converge: §f" + targets.size() + " targets pulled together. "
                    + sequenceStatus(player)));
    }

    // =========================================================================
    //  ABILITY 5 — SIGIL: EXECUTE
    // =========================================================================

    /**
     * Releases ALL stored energy in a massive AOE burst.
     * Damage scales with:
     *   - stored energy
     *   - number of targets
     *   - number of steps successfully completed (1–5)
     *   - amplification state of targets
     * ONLY works if Converge was the previous step.
     * Cost: 800 essence.  Requires stage ≥ 4.
     */
    public static void runicSequencerSigilExecute(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Runic Sequencer")) return;
        if (SoulCore.getSoulEssence(player) < 800) return;
        if (SoulCore.getAscensionStage(player) < 4) return;
        if (!validateStep(player, STEP_CONVERGE, sl)) return;

        List<LivingEntity> targets = getMarkedEntities(player, sl);
        if (targets.isEmpty()) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNo converged targets for Execute!"));
            resetSequence(player, sl, false);
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 800);

        int   stage         = SoulCore.getAscensionStage(player);
        float energy        = getEnergy(player);
        int   targetCount   = targets.size();
        int   loops         = player.getPersistentData().getInt(NBT_LOOPS);
        float loopMult      = inRitual(player) ? 1f + loops * 0.2f : 1f;

        // Base damage formula: energy × target scaling × loop multiplier
        float baseDamage = (energy * 0.5f + stage * 5f) * (1f + targetCount * 0.15f) * loopMult;

        // Deal damage to all targets + broad AOE splash
        Vec3 center = Vec3.ZERO;
        for (LivingEntity e : targets) center = center.add(e.position());
        center = center.scale(1.0 / targets.size());
        final Vec3 finalCenter = center;

        for (LivingEntity e : targets) {
            float dmg = baseDamage;
            // Amplify bonus
            if (e.getPersistentData().getInt("RuneAmplified") > 0) {
                dmg *= e.getPersistentData().getFloat("RuneAmplifyPower");
            }
            e.hurt(level.damageSources().playerAttack(player), dmg);
            e.invulnerableTime = 0;
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                    e.getX(), e.getY() + 1, e.getZ(), 1, 0, 0, 0, 0);
        }

        // AOE splash to nearby enemies not in the target list
        float splashDmg = baseDamage * 0.4f;
        level.getEntitiesOfClass(LivingEntity.class,
                new AABB(center, center).inflate(6f + stage),
                e -> e != player && e.isAlive()
                        && targets.stream().noneMatch(t -> t.getUUID().equals(e.getUUID())))
                .forEach(e -> {
                    e.hurt(level.damageSources().playerAttack(player), splashDmg);
                    e.invulnerableTime = 0;
                    sl.sendParticles(ParticleTypes.CRIT,
                            e.getX(), e.getY() + 1, e.getZ(), 5, 0.3, 0.3, 0.3, 0.03);
                });

        // Explosion visual
        for (int ring = 1; ring <= 4; ring++) {
            double r = ring * 2.0;
            int count = ring * 12;
            for (int i = 0; i < count; i++) {
                double angle = (2 * Math.PI / count) * i;
                sl.sendParticles(ParticleTypes.ENCHANT,
                        center.x + r * Math.cos(angle), center.y + 0.5,
                        center.z + r * Math.sin(angle), 1, 0, 0.3, 0, 0.05);
            }
        }
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y + 1, center.z, 3, 0.5, 0.5, 0.5, 0);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5f, 0.5f);
        level.playSound(null, center.x, center.y, center.z,
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1f, 0.3f);

        // Perfect Ritual aftershock
        if (inRitual(player)) {
            int newLoops = loops + 1;
            player.getPersistentData().putInt(NBT_LOOPS, newLoops);
            // Aftershock: secondary ring of damage
            final float aftershockDmg = baseDamage * 0.5f;
            level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(center, center).inflate(10f),
                    e -> e != player && e.isAlive())
                    .forEach(e -> {
                        e.hurt(level.damageSources().playerAttack(player), aftershockDmg);
                        e.invulnerableTime = 0;
                    });
            sl.sendParticles(ParticleTypes.REVERSE_PORTAL,
                    center.x, center.y + 1, center.z, 30, 2.0, 1.0, 2.0, 0.04);
        }

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§c§l[V] EXECUTE! §r§f" + String.format("%.1f", baseDamage)
                    + " dmg × §e" + targetCount + " §ftargets"
                    + (inRitual(player) ? " §5[Loop " + (loops + 1) + " ×" + String.format("%.1f", loopMult) + "]" : "")
                    + (inRitual(player) ? " §d+ Aftershock!" : "")));

        // Reset sequence (auto-loops in ritual)
        if (inRitual(player)) {
            setStep(player, STEP_NONE);
            player.getPersistentData().putFloat(NBT_ENERGY, 0f);
            clearMarkedTargets(player, sl);
        } else {
            resetSequence(player, sl, false);
        }
    }

    // =========================================================================
    //  ABILITY 6 — QUICK INSCRIPTION
    // =========================================================================

    /**
     * Skips one step in the current sequence — forgiveness tool.
     * Automatically advances the sequence by 1 without the normal ability.
     * Only 1 charge, recharges in 20 seconds.
     * Cost: 200 essence.
     */
    public static void runicSequencerQuickInscription(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Runic Sequencer")) return;
        if (SoulCore.getSoulEssence(player) < 200) return;

        int step = getStep(player);
        if (step == STEP_NONE || step == STEP_EXECUTE) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cNothing to skip right now."));
            return;
        }

        int skipCharge = player.getPersistentData().getInt(NBT_SKIP_CHARGE);
        if (skipCharge <= 0) {
            int skipCD = player.getPersistentData().getInt(NBT_SKIP_CD);
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§cQuick Inscription recharging. §7(" + (int)Math.ceil(skipCD / 20.0) + "s)"));
            return;
        }

        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 200);
        player.getPersistentData().putInt(NBT_SKIP_CHARGE, 0);
        player.getPersistentData().putInt(NBT_SKIP_CD, SKIP_CD);

        // Add partial energy for the skipped step
        addEnergy(player, ENERGY_PER_STEP * 0.5f);

        // Advance step
        setStep(player, step + 1);

        sl.sendParticles(ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1, player.getZ(), 12, 0.4, 0.4, 0.4, 0.05);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.6f, 1.8f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§aQuick Inscription: §fskipped to " + STEP_NAMES[step + 1]
                    + ". " + sequenceStatus(player)));
    }

    // =========================================================================
    //  ABILITY 7 — PERFECT RITUAL (ultimate)
    // =========================================================================

    /**
     * 12-second ultimate:
     *   - Abilities auto-progress sequence (no wrong-order resets)
     *   - Sequence loops continuously on Execute
     *   - Each loop increases damage by ×0.2
     *   - Every completed Execute creates an aftershock ring
     * Cost: 5000 essence.  Requires stage ≥ 7.
     */
    public static void runicSequencerPerfectRitual(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Runic Sequencer")) return;
        if (SoulCore.getSoulEssence(player) < 5000) return;
        if (SoulCore.getAscensionStage(player) < 7) return;
        if (inRitual(player)) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§cThe Perfect Ritual is already active."));
            return;
        }

        // Consume ultimate cost
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 5000);

        // Initialize Ritual State
        player.getPersistentData().putInt(NBT_RITUAL, RITUAL_DUR);
        player.getPersistentData().putInt(NBT_LOOPS, 0);

        // If the player isn't in a sequence yet, ensure they are at STEP_NONE so the auto-loop catches them
        if (getStep(player) == STEP_NONE) {
            setStep(player, STEP_NONE);
            player.getPersistentData().putFloat(NBT_ENERGY, 0f);
        }

        // Massive visual and audio feedback for Ultimate activation
        sl.playSound(null, player.blockPosition(),
                SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 1.0f, 0.8f);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.5f, 1.5f);

        // Create an expanding dome of runes and soul particles
        for (int i = 0; i < 150; i++) {
            double phi = Math.acos(1 - 2 * Math.random());
            double theta = 2 * Math.PI * Math.random();
            double radius = 4.0;

            double x = player.getX() + radius * Math.sin(phi) * Math.cos(theta);
            double y = player.getY() + 1.0 + radius * Math.cos(phi);
            double z = player.getZ() + radius * Math.sin(phi) * Math.sin(theta);

            sl.sendParticles(ParticleTypes.ENCHANT, x, y, z, 1, 0, 0, 0, 0.05);
            if (i % 3 == 0) {
                sl.sendParticles(ParticleTypes.WITCH, x, y, z, 1, 0.1, 0.1, 0.1, 0.02);
            }
        }

        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("§d§l[PERFECT RITUAL INITIATED]"));
            sp.sendSystemMessage(Component.literal("§5The sequence is unbroken. The flow of essence bends to your will."));
        }
    }
}
