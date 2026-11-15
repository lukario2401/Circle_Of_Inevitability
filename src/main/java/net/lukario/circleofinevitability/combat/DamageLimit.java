package net.lukario.circleofinevitability.combat;

import java.util.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.listener.*;
import net.minecraftforge.fml.common.Mod;

/** Scoped final-damage ceiling for percent-health boss attacks; nested actions restore the outer cap. */
@Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class DamageLimit {
    private static final Map<UUID,Float> CAPS=new HashMap<>();
    /** Current scoped ceiling, for listeners such as lifesteal that consume final damage. */
    public static float maximum(LivingEntity target){return CAPS.getOrDefault(target.getUUID(),Float.MAX_VALUE);}
    public static void run(LivingEntity target,float maximum,Runnable action) {
        if(!Float.isFinite(maximum)||maximum<0)throw new IllegalArgumentException("Finite nonnegative cap required");
        Float old=CAPS.put(target.getUUID(),Math.min(maximum,CAPS.getOrDefault(target.getUUID(),Float.MAX_VALUE)));
        try{action.run();}finally{if(old==null)CAPS.remove(target.getUUID());else CAPS.put(target.getUUID(),old);}
    }
    @SubscribeEvent(priority=Priority.LOWEST) public static void damage(LivingDamageEvent event) {
        Float cap=CAPS.get(event.getEntity().getUUID());if(cap!=null)event.setAmount(Math.min(cap,event.getAmount()));
    }
}
