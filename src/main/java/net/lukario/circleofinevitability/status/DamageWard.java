package net.lukario.circleofinevitability.status;
import java.util.*;
import java.util.function.BiPredicate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Named, transient damage immunity with a caller-supplied source/amount rule. */
@Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class DamageWard {
    private record Ward(long until,BiPredicate<DamageSource,Float> rule){}
    private static final Map<UUID,Map<String,Ward>> WARDS=new HashMap<>();
    public static void grant(LivingEntity e,String name,int ticks,BiPredicate<DamageSource,Float> rule){WARDS.computeIfAbsent(e.getUUID(),k->new HashMap<>()).put(name,new Ward(e.level().getGameTime()+ticks,rule));}
    public static void clear(LivingEntity e){WARDS.remove(e.getUUID());}
    public static boolean blocks(LivingEntity e,DamageSource source,float amount){
        var wards=WARDS.get(e.getUUID());if(wards==null)return false;
        wards.values().removeIf(w->w.until<=e.level().getGameTime());
        if(wards.isEmpty()){clear(e);return false;}return wards.values().stream().anyMatch(w->w.rule.test(source,amount));
    }
    @SubscribeEvent public static boolean attack(LivingAttackEvent event){return blocks(event.getEntity(),event.getSource(),event.getAmount());}
    @SubscribeEvent public static void died(LivingDeathEvent event){clear(event.getEntity());}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){clear(event.getEntity());}
}
