package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * NoHeal: an entity cannot heal for a while, from anything: food, regeneration, potions, a mob's own healing.
 * Frostbite, a festering wound, a curse on the blood. Works from any class/ability:
 *
 *   NoHeal.apply(target, 100);          // 5 seconds
 *   NoHeal.isApplied(entity)     NoHeal.ticksLeft(entity)     NoHeal.remove(target)
 *
 * Applying it again only ever makes it last longer. Health set directly (setHealth, a DeathWard bringing
 * someone back) is not healing and still works. Saved with the entity.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NoHeal {

    private static final String UNTIL = "circle_of_inevitability_no_heal_until";     // game time

    private NoHeal() {}

    public static void apply(LivingEntity target, int ticks) {
        if (target.level().isClientSide() || ticks <= 0) return;
        long until = target.level().getGameTime() + ticks;
        if (until > target.getPersistentData().getLongOr(UNTIL, 0L)) target.getPersistentData().putLong(UNTIL, until);
    }

    public static boolean isApplied(Entity entity) {
        return ticksLeft(entity) > 0;
    }

    public static int ticksLeft(Entity entity) {
        if (entity == null) return 0;
        return (int) Math.max(0, entity.getPersistentData().getLongOr(UNTIL, 0L) - entity.level().getGameTime());
    }

    public static void remove(LivingEntity target) {
        target.getPersistentData().remove(UNTIL);
    }

    // returning true cancels the healing
    @SubscribeEvent
    public static boolean onNoHealHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return false;
        if (!isApplied(entity)) {
            if (entity.getPersistentData().contains(UNTIL)) entity.getPersistentData().remove(UNTIL);   // tidy up
            return false;
        }
        return true;
    }
}
