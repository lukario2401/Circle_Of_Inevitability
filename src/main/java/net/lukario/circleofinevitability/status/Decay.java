package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Decay: an entity rots for a while. Twilight, age, a curse of withering. Works from any class/ability, on players
 * and mobs:
 *
 *   Decay.apply(target, 200, 0.2f);      // 10 seconds; its maximum health is 20% lower meanwhile
 *   Decay.isDecaying(entity)     Decay.depth(entity)     Decay.ticksLeft(entity)     Decay.cure(target)
 *
 * While it lasts:
 *   - it cannot heal (status/NoHeal), and it has wither and weakness
 *   - every WEAR_EVERY ticks its armor and what it holds lose WEAR durability (items, never blocks)
 *   - its maximum health is lower by 'depth' (0.2 = 20%, MAX_DEPTH at most); it comes back when the decay ends
 * Applying it again never makes it shorter or shallower, so call it every second with a growing depth for "the
 * longer they stay the worse it gets":   Decay.apply(enemy, 40, 0.1f + ticksInside / 400f);
 *
 * Some cannot rot (a Pathway's own authority over Twilight): Decay.addImmunity(entity -> ...). apply() then does
 * nothing and returns false.
 *
 * Saved with the entity (the time it ends and the depth), so a mob that rots when the world closes still rots, and
 * gets its health back, after it opens again.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Decay {

    /** The most of its maximum health a decay can take (0.6 = 60%). */
    public static float MAX_DEPTH = 0.6f;
    /** Armor and held items lose WEAR durability this often (ticks). */
    public static int WEAR_EVERY = 20;
    public static int WEAR = 1;

    private static final String TAG = "circle_of_inevitability_decay";
    private static final String UNTIL = "Until";
    private static final String DEPTH = "Depth";
    private static final Identifier HEALTH_ID = Physique.id("decay_health");
    private static final EquipmentSlot[] WORN = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    private static final List<Predicate<LivingEntity>> IMMUNE = new ArrayList<>();

    private Decay() {}

    // =========================
    // API
    // =========================

    /** One more kind of entity that cannot rot: Decay.addImmunity(entity -> ...). */
    public static void addImmunity(Predicate<LivingEntity> immune) {
        IMMUNE.add(immune);
    }

    public static boolean isImmune(LivingEntity entity) {
        for (Predicate<LivingEntity> immune : IMMUNE) {
            if (immune.test(entity)) return true;
        }
        return false;
    }

    /**
     * Rots it for 'ticks' with its maximum health 'depth' lower (0..MAX_DEPTH). Never shortens or lessens one
     * already on it. Returns false (and does nothing) when it cannot rot.
     */
    public static boolean apply(LivingEntity target, int ticks, float depth) {
        if (ticks <= 0 || !target.isAlive() || !(target.level() instanceof ServerLevel sl) || isImmune(target)) return false;
        CompoundTag data = target.getPersistentData();
        CompoundTag tag = data.contains(TAG) ? data.getCompoundOrEmpty(TAG) : new CompoundTag();
        long until = Math.max(tag.getLongOr(UNTIL, 0L), sl.getGameTime() + ticks);
        float deep = Math.max(tag.getFloatOr(DEPTH, 0f), Math.max(0f, Math.min(MAX_DEPTH, depth)));
        tag.putLong(UNTIL, until);
        tag.putFloat(DEPTH, deep);
        data.put(TAG, tag);

        int left = (int) (until - sl.getGameTime());
        NoHeal.apply(target, left);
        keep(target, MobEffects.WITHER, left);
        keep(target, MobEffects.WEAKNESS, left);
        body(target, deep);
        return true;
    }

    public static boolean isDecaying(Entity entity) {
        return entity != null && entity.getPersistentData().contains(TAG);
    }

    /** How much of its maximum health it has lost to the decay now (0 = none). */
    public static float depth(Entity entity) {
        if (!isDecaying(entity)) return 0f;
        return entity.getPersistentData().getCompoundOrEmpty(TAG).getFloatOr(DEPTH, 0f);
    }

    public static int ticksLeft(Entity entity) {
        if (!isDecaying(entity)) return 0;
        long until = entity.getPersistentData().getCompoundOrEmpty(TAG).getLongOr(UNTIL, 0L);
        return (int) Math.max(0, until - entity.level().getGameTime());
    }

    /** Ends it now: the health comes back, the wither and weakness it gave end, and it can heal again. */
    public static void cure(LivingEntity target) {
        if (!isDecaying(target)) return;
        target.getPersistentData().remove(TAG);
        Physique.remove(target, Attributes.MAX_HEALTH, HEALTH_ID);
        target.removeEffect(MobEffects.WITHER);
        target.removeEffect(MobEffects.WEAKNESS);
        if (NoHeal.isApplied(target)) NoHeal.remove(target);
    }

    // =========================
    // Internals
    // =========================

    private static void keep(LivingEntity target, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int ticks) {
        MobEffectInstance has = target.getEffect(effect);
        if (has != null && (has.getDuration() >= ticks || has.isInfiniteDuration())) return;
        target.addEffect(new MobEffectInstance(effect, ticks, has == null ? 0 : has.getAmplifier()));
    }

    private static void body(LivingEntity target, float depth) {
        Physique.keep(target, Attributes.MAX_HEALTH, HEALTH_ID, -depth, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, depth > 0f);
    }

    private static void wear(LivingEntity target) {
        for (EquipmentSlot slot : WORN) {
            ItemStack stack = target.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamageableItem()) stack.hurtAndBreak(WEAR, target, slot);
        }
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onDecayLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel) || !isDecaying(entity)) return;
        if (ticksLeft(entity) <= 0) {
            entity.getPersistentData().remove(TAG);
            Physique.remove(entity, Attributes.MAX_HEALTH, HEALTH_ID);           // its health comes back (the effects run out by themselves)
            return;
        }
        if (entity.tickCount % WEAR_EVERY != 0) return;
        body(entity, depth(entity));                                             // the modifier is not saved: put it back after a reload
        wear(entity);
    }

    @SubscribeEvent
    public static void onDecayDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide() && isDecaying(entity)) {
            entity.getPersistentData().remove(TAG);
            Physique.remove(entity, Attributes.MAX_HEALTH, HEALTH_ID);
        }
    }
}
