package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Cleansing: the one place that says what "cleansed" means, so a status of your own can be taken off by every
 * cleanse in the mod without any of them knowing about it.
 *
 *   Cleansing.addListener(MyStatus::remove);       // once, in a static block: a cleanse now takes MyStatus off too
 *   Spells.cleanse(entity);                        // what every cleanse ability calls: harmful potion effects, fire,
 *                                                  // and every status that listens here
 *   Cleansing.cleanseStatuses(entity);             // only the statuses that listen (not the potion effects)
 *
 * Drinking a milk bucket cleanses the listening statuses too (the game already takes the potion effects off).
 * MILK_CLEANSES = false turns that off for everyone.
 *
 * Who listens today: status/Corrosion. Bleeding, Decay and the like do not on purpose (a wound is not a curse);
 * add a listener for them if a Pathway should be able to wash them off.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Cleansing {

    /** A milk bucket drunk takes off the statuses that listen here, as a cleanse does. */
    public static boolean MILK_CLEANSES = true;

    private static final List<Consumer<LivingEntity>> LISTENERS = new ArrayList<>();

    private Cleansing() {}

    /** 'remove' runs on every entity that is cleansed. Call it once (a static block), not every cast. */
    public static void addListener(Consumer<LivingEntity> remove) {
        LISTENERS.add(remove);
    }

    /** Every status that listens here is taken off 'entity' (potion effects and fire are left alone). */
    public static void cleanseStatuses(LivingEntity entity) {
        if (entity == null || entity.level().isClientSide()) return;
        for (Consumer<LivingEntity> listener : LISTENERS) {
            try {
                listener.accept(entity);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A cleansing listener failed", e);
            }
        }
    }

    @SubscribeEvent
    public static void onCleansingMilk(LivingEntityUseItemEvent.Finish event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !MILK_CLEANSES) return;
        if (event.getItem().is(Items.MILK_BUCKET)) cleanseStatuses(entity);
    }
}
