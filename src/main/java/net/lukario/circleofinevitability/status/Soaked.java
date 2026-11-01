package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Shared water mark. Local rain may supply a wet environment without marking allies. */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID)
public final class Soaked {
    private static final String MARK = "soaked";
    private static final List<Predicate<LivingEntity>> WET = new ArrayList<>();
    private Soaked() {}
    public static void addWetCheck(Predicate<LivingEntity> rule) { WET.add(rule); }
    public static void apply(LivingEntity target, int ticks) {
        Marks.add(target, MARK, 1, Math.max(ticks, Marks.ticksLeft(target, MARK)));
        target.clearFire();
    }
    public static boolean isSoaked(Entity entity) {
        return entity != null && (Marks.count(entity, MARK) > 0 || entity.isInWater()
                || entity.level().isRainingAt(entity.blockPosition()));
    }
    public static boolean isWet(LivingEntity entity) {
        if (entity.isInWater() || entity.level().isRainingAt(entity.blockPosition())) return true;
        for (var rule : WET) if (rule.test(entity)) return true;
        return false;
    }
    public static void clear(LivingEntity entity) { Marks.clear(entity, MARK); }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        LivingEntity e = event.getEntity();
        if (!e.level().isClientSide() && isSoaked(e) && e.isOnFire()) e.clearFire();
    }
    @SubscribeEvent public static boolean fire(LivingAttackEvent event) {
        return !event.getEntity().level().isClientSide() && event.getSource().is(DamageTypeTags.IS_FIRE)
                && isSoaked(event.getEntity());
    }
}
