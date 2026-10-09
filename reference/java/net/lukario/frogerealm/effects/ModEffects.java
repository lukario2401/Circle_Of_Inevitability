package net.lukario.frogerealm.effects;

import net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.AbyssalMonarch;
import net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.RuinBladeAscendant;
import net.lukario.frogerealm.shadow_slave.soul_abilities.aspects.ShadowSlaveAspect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, "forgerealmmod");

    // This is your actual registry object
    public static final RegistryObject<MobEffect> SHADOW_MARK = MOB_EFFECTS.register("shadow_mark",
            () -> new ShadowSlaveAspect.ShadowMarkEffect());

    public static final RegistryObject<MobEffect> Abyssal_Monarch_MARK = MOB_EFFECTS.register("abyssal_monarch_mark",
            () -> new AbyssalMonarch.Abyssal_Monarch_Mark());

    public static final RegistryObject<MobEffect> RuinBade_Ruin = MOB_EFFECTS.register("ruin_blade_ruin",
            () -> new RuinBladeAscendant.Ruinblade_Ruin_Effect());

    public static final RegistryObject<MobEffect> RuinBade_Overload = MOB_EFFECTS.register("ruin_blade_overload",
            () -> new RuinBladeAscendant.Ruinblade_Overload_Effect());

    public static final RegistryObject<MobEffect> RuinBade_cataclysm = MOB_EFFECTS.register("ruin_blade_cataclysm",
            () -> new RuinBladeAscendant.Ruinblade_Cataclysm_Effect());



    public static void register(IEventBus eventBus) {
        MOB_EFFECTS.register(eventBus);
    }
}