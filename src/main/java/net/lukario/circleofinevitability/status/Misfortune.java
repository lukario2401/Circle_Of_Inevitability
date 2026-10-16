package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.DamageFx;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Misfortune: an entity's luck is taken for a while. It carries a number of levels (1 to MAX_LEVEL), and every second
 * it rolls: the chance of a mishap is CHANCE_PER_LEVEL times its level. Works from any class/ability, on players and
 * mobs:
 *
 *   Misfortune.add(target, player, 3, 3, 400);          // 3 levels at once, 3 at most, all of them 20 seconds from now
 *   Misfortune.add(target, player, 1, 5, 400);          // one more level, up to 5 (a field that worsens it each beat)
 *   Misfortune.add(target, player, 2, 5, 400, true);    // Catastrophe: its worst mishap is a disaster
 *   Misfortune.level(entity)    Misfortune.ticksLeft(entity)    Misfortune.clear(target)
 *   Misfortune.addImmunity(entity -> ...)               // some cannot be cursed (a Pathway's own authority)
 *   Misfortune.addHalfLevels(entity -> ...)             // some carry only half the levels (bosses)
 *   Misfortune.addListener((victim, mishap, source) -> ...)   // the look and sound of each mishap
 *
 * The mishaps (each needs its level; one is picked at random from those the level allows):
 *   STUMBLE        1  Slowness IV for STUMBLE_TICKS and a little trip
 *   FUMBLE         2  what it holds and wears loses FUMBLE_WEAR durability, and its next blow is FUMBLE_CUT weaker
 *   BACKFIRE       3  the next thing it shoots flies wide (up to BACKFIRE_DEGREES off)
 *   FALLING_STONE  4  magic damage, STONE_SHARE of its maximum health, from nobody (DamageFx counts it as the source's)
 *   CALAMITY       5  a disaster: CALAMITY_SHARE of its maximum health (CATASTROPHE_SHARE, and it is thrown up, with
 *                     Catastrophe)
 * The damage itself is here; how a mishap looks and sounds is up to a listener.
 *
 * Adding again never lowers the level and makes all of it last 'ticks' from now. Saved with the entity.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Misfortune {

    public enum Mishap {
        STUMBLE(1), FUMBLE(2), BACKFIRE(3), FALLING_STONE(4), CALAMITY(5);

        public final int fromLevel;

        Mishap(int fromLevel) {
            this.fromLevel = fromLevel;
        }
    }

    /** Runs for every mishap that befalls someone. 'source' = who cursed it (may be null: gone, or nobody). */
    @FunctionalInterface
    public interface Listener {
        void befell(LivingEntity victim, Mishap mishap, Player source);
    }

    public static int MAX_LEVEL = 5;
    /** Ticks between two rolls. */
    public static int ROLL_EVERY = 20;
    /** Chance of a mishap at each roll, per level (0.1 = 10% at level 1, 50% at level 5). */
    public static float CHANCE_PER_LEVEL = 0.10f;
    public static int STUMBLE_TICKS = 20;
    public static int FUMBLE_WEAR = 6;
    public static float FUMBLE_CUT = 0.3f;
    public static float BACKFIRE_DEGREES = 30f;
    public static float STONE_SHARE = 0.05f;
    public static float CALAMITY_SHARE = 0.15f;
    public static float CATASTROPHE_SHARE = 0.30f;

    private static final String TAG = "circle_of_inevitability_misfortune";
    private static final String LEVEL = "level";
    private static final String UNTIL = "until";              // game time
    private static final String SOURCE = "source";
    private static final String CATASTROPHE = "catastrophe";
    private static final String FUMBLED = "fumbled";          // the next blow is weaker
    private static final String BACKFIRES = "backfires";     // the next shot flies wide

    private static final EquipmentSlot[] WORN = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    private static final List<Predicate<LivingEntity>> IMMUNE = new ArrayList<>();
    private static final List<Predicate<LivingEntity>> HALF = new ArrayList<>();
    private static final List<Listener> LISTENERS = new ArrayList<>();

    private Misfortune() {}

    // =========================
    // API
    // =========================

    /** 'levels' more (up to 'max'), all of them lasting 'ticks' from now. Returns the level it has now (0 = immune). */
    public static int add(LivingEntity target, Player source, int levels, int max, int ticks) {
        return add(target, source, levels, max, ticks, false);
    }

    /** The same; catastrophe = its CALAMITY is a Catastrophe (stays on until the Misfortune ends). */
    public static int add(LivingEntity target, Player source, int levels, int max, int ticks, boolean catastrophe) {
        if (target.level().isClientSide() || ticks <= 0 || levels <= 0 || !target.isAlive()) return 0;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return 0;
        if (isImmune(target)) return 0;
        int cap = Math.max(1, Math.min(MAX_LEVEL, max));
        if (halved(target)) {
            cap = Math.max(1, cap / 2);
            levels = Math.max(1, levels / 2);
        }
        CompoundTag tag = target.getPersistentData().getCompoundOrEmpty(TAG);
        int had = level(target);
        int level = Math.max(had, Math.min(cap, had + levels));
        tag.putInt(LEVEL, level);
        tag.putLong(UNTIL, target.level().getGameTime() + ticks);
        if (source != null) tag.store(SOURCE, UUIDUtil.CODEC, source.getUUID());
        if (catastrophe) tag.putBoolean(CATASTROPHE, true);
        target.getPersistentData().put(TAG, tag);
        return level;
    }

    /** 0 when it carries none. */
    public static int level(Entity entity) {
        if (entity == null || !entity.getPersistentData().contains(TAG)) return 0;
        CompoundTag tag = entity.getPersistentData().getCompoundOrEmpty(TAG);
        if (tag.getLongOr(UNTIL, 0L) <= entity.level().getGameTime()) return 0;
        return tag.getIntOr(LEVEL, 0);
    }

    public static int ticksLeft(Entity entity) {
        if (level(entity) <= 0) return 0;
        return (int) Math.max(0, entity.getPersistentData().getCompoundOrEmpty(TAG).getLongOr(UNTIL, 0L) - entity.level().getGameTime());
    }

    public static boolean isCatastrophe(Entity entity) {
        return level(entity) > 0 && entity.getPersistentData().getCompoundOrEmpty(TAG).getBooleanOr(CATASTROPHE, false);
    }

    public static void clear(LivingEntity target) {
        target.getPersistentData().remove(TAG);
    }

    public static boolean isImmune(LivingEntity entity) {
        for (Predicate<LivingEntity> immune : IMMUNE) if (immune.test(entity)) return true;
        return false;
    }

    public static void addImmunity(Predicate<LivingEntity> immune) {
        IMMUNE.add(immune);
    }

    /** Those this says true for carry half the levels (at least one). */
    public static void addHalfLevels(Predicate<LivingEntity> half) {
        HALF.add(half);
    }

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    /** Makes a mishap befall it now (whatever its level), as if it had rolled it. */
    public static void befall(LivingEntity victim, Mishap mishap) {
        if (!(victim.level() instanceof ServerLevel sl)) return;
        Player source = sourceOf(victim, sl);
        CompoundTag tag = victim.getPersistentData().getCompoundOrEmpty(TAG);
        switch (mishap) {
            case STUMBLE -> {
                victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, STUMBLE_TICKS, 3, false, false));
                double a = victim.getRandom().nextDouble() * Math.PI * 2;
                victim.setDeltaMovement(victim.getDeltaMovement().add(Math.cos(a) * 0.25, 0.1, Math.sin(a) * 0.25));
                victim.hurtMarked = true;
            }
            case FUMBLE -> {
                for (EquipmentSlot slot : WORN) {
                    ItemStack stack = victim.getItemBySlot(slot);
                    if (!stack.isEmpty() && stack.isDamageableItem()) stack.hurtAndBreak(FUMBLE_WEAR, victim, slot);
                }
                tag.putBoolean(FUMBLED, true);
            }
            case BACKFIRE -> tag.putBoolean(BACKFIRES, true);
            case FALLING_STONE -> hurt(sl, victim, source, victim.getMaxHealth() * STONE_SHARE);
            case CALAMITY -> {
                boolean catastrophe = tag.getBooleanOr(CATASTROPHE, false);
                hurt(sl, victim, source, victim.getMaxHealth() * (catastrophe ? CATASTROPHE_SHARE : CALAMITY_SHARE));
                if (catastrophe && victim.isAlive()) {                // the ground gives way under it: a fall it did not see coming
                    victim.setDeltaMovement(victim.getDeltaMovement().x, 1.1, victim.getDeltaMovement().z);
                    victim.hurtMarked = true;
                    FallGuard.remove(victim);
                }
            }
        }
        if (!tag.isEmpty()) victim.getPersistentData().put(TAG, tag);
        for (Listener listener : LISTENERS) {
            try {
                listener.befell(victim, mishap, source);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A misfortune listener failed", e);
            }
        }
    }

    // =========================
    // Internals
    // =========================

    private static boolean halved(LivingEntity entity) {
        for (Predicate<LivingEntity> half : HALF) if (half.test(entity)) return true;
        return false;
    }

    private static Player sourceOf(LivingEntity victim, ServerLevel sl) {
        UUID id = victim.getPersistentData().getCompoundOrEmpty(TAG).read(SOURCE, UUIDUtil.CODEC).orElse(null);
        return id == null ? null : sl.getPlayerByUUID(id);
    }

    private static void hurt(ServerLevel sl, LivingEntity victim, Player source, float damage) {
        if (damage <= 0f) return;
        if (source != null) DamageFx.credit(source, victim);
        victim.invulnerableTime = 0;
        victim.hurt(sl.damageSources().magic(), damage);
    }

    private static Mishap roll(LivingEntity victim, int level) {
        List<Mishap> allowed = new ArrayList<>();
        for (Mishap mishap : Mishap.values()) if (mishap.fromLevel <= level) allowed.add(mishap);
        return allowed.get(victim.getRandom().nextInt(allowed.size()));
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onMisfortuneLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || entity.tickCount % ROLL_EVERY != 0 || !entity.getPersistentData().contains(TAG)) return;
        int level = level(entity);
        if (level <= 0) {
            clear(entity);
            return;
        }
        if (entity.getRandom().nextFloat() < CHANCE_PER_LEVEL * level) befall(entity, roll(entity, level));
    }

    // FUMBLE: its next blow is weaker
    @SubscribeEvent
    public static void onMisfortuneHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker.level().isClientSide()) return;
        if (!attacker.getPersistentData().contains(TAG)) return;
        CompoundTag tag = attacker.getPersistentData().getCompoundOrEmpty(TAG);
        if (!tag.getBooleanOr(FUMBLED, false)) return;
        tag.remove(FUMBLED);
        attacker.getPersistentData().put(TAG, tag);
        event.setAmount(event.getAmount() * (1f - FUMBLE_CUT));
    }

    // BACKFIRE: its next shot flies wide
    @SubscribeEvent
    public static void onMisfortuneShot(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Projectile projectile) || event.getLevel().isClientSide()) return;
        if (!(projectile.getOwner() instanceof LivingEntity shooter) || !shooter.getPersistentData().contains(TAG)) return;
        CompoundTag tag = shooter.getPersistentData().getCompoundOrEmpty(TAG);
        if (!tag.getBooleanOr(BACKFIRES, false)) return;
        tag.remove(BACKFIRES);
        shooter.getPersistentData().put(TAG, tag);
        Vec3 motion = projectile.getDeltaMovement();
        double yaw = Math.toRadians((shooter.getRandom().nextFloat() * 2f - 1f) * BACKFIRE_DEGREES);
        double cos = Math.cos(yaw), sin = Math.sin(yaw);
        double lift = (shooter.getRandom().nextFloat() - 0.5f) * 0.3 * motion.length();
        projectile.setDeltaMovement(motion.x * cos - motion.z * sin, motion.y + lift, motion.x * sin + motion.z * cos);
    }

    @SubscribeEvent
    public static void onMisfortuneDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) clear(event.getEntity());
    }
}
