package net.lukario.circleofinevitability.status;
import java.util.*;
import java.util.function.ToDoubleBiFunction;
import net.lukario.circleofinevitability.combat.Later;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Shared effect-duration reductions. Rules return a multiplier; the replacement effect is guarded from recursion. */
@Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class EffectDuration {
    private static final List<ToDoubleBiFunction<LivingEntity,MobEffectInstance>> RULES=new ArrayList<>();
    private static final Set<UUID> REPLACING=new HashSet<>();
    public static void addRule(ToDoubleBiFunction<LivingEntity,MobEffectInstance> rule){RULES.add(rule);}
    @SubscribeEvent public static void apply(MobEffectEvent.Applicable event){
        var entity=event.getEntity();var effect=event.getEffectInstance();
        if(!(entity.level() instanceof ServerLevel sl)||effect.isInfiniteDuration()||effect.getDuration()<20||REPLACING.contains(entity.getUUID()))return;
        double scale=1;for(var rule:RULES)scale*=Math.clamp(rule.applyAsDouble(entity,effect),0,1);
        if(scale>=1)return;
        event.setResult(Result.DENY);
        int duration=Math.max(1,(int)Math.round(effect.getDuration()*scale));
        Later.run(sl,1,()->{if(!entity.isAlive() || entity.level()!=sl)return;REPLACING.add(entity.getUUID());
            try{entity.addEffect(new MobEffectInstance(effect.getEffect(),duration,effect.getAmplifier(),effect.isAmbient(),effect.isVisible()));}
            finally{REPLACING.remove(entity.getUUID());}});
    }
}
