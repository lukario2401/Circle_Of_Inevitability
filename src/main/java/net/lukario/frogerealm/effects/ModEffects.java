package net.lukario.frogerealm.effects;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, "circle_of_inevitability");

    // Removed: SHADOW_MARK, Abyssal_Monarch_MARK, RuinBade_Ruin, RuinBade_Overload, RuinBade_cataclysm
    // (their effect classes live in the dropped aspects; see MIGRATION_NOTES.md, Removed)



    public static void register(BusGroup eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}
