package net.lukario.circleofinevitability.status;

import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Named attack-speed reductions. Players use their real attack-speed attribute; mobs get a minimum
 * interval between melee attempts. Strongest reduction wins; removing one source preserves the rest. */
@Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class AttackTempo {
    private record Slow(LivingEntity target,long until,double multiplier) {}
    private static final Map<UUID,Map<String,Slow>> SLOWS=new HashMap<>();
    private static final Map<UUID,Long> LAST=new HashMap<>();
    private static final net.minecraft.resources.Identifier ID=Physique.id("attack_tempo");
    public static void apply(LivingEntity target,String name,double multiplier,int ticks) {
        if(target.level().isClientSide() || ticks<=0 || !Double.isFinite(multiplier))return;
        SLOWS.computeIfAbsent(target.getUUID(),k->new HashMap<>()).put(name,new Slow(target,target.level().getGameTime()+ticks,Math.clamp(multiplier,.05,1)));
        update(target);
    }
    public static void remove(LivingEntity target,String name) {
        var entries=SLOWS.get(target.getUUID());if(entries!=null)entries.remove(name);update(target);
    }
    public static double multiplier(LivingEntity target) {
        var entries=SLOWS.get(target.getUUID());if(entries==null)return 1;
        entries.values().removeIf(v->v.until<=target.level().getGameTime());
        return entries.values().stream().mapToDouble(Slow::multiplier).min().orElse(1);
    }
    private static void update(LivingEntity target) {
        double m=multiplier(target);
        Physique.keep(target,Attributes.ATTACK_SPEED,ID,m-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,m<1);
        if(m==1){SLOWS.remove(target.getUUID());LAST.remove(target.getUUID());}
    }
    @SubscribeEvent public static void tick(LivingEvent.LivingTickEvent event) {
        if(SLOWS.containsKey(event.getEntity().getUUID()))update(event.getEntity());
    }
    @SubscribeEvent public static boolean attack(LivingAttackEvent event) {
        if(!(event.getSource().getDirectEntity() instanceof LivingEntity mob) || mob instanceof Player
                || !event.getSource().is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK))return false;
        double m=multiplier(mob);if(m>=1)return false;long now=mob.level().getGameTime();
        Long last=LAST.get(mob.getUUID());if(last!=null && now-last<20/m)return true;
        LAST.put(mob.getUUID(),now);return false;
    }
    @SubscribeEvent public static void death(LivingDeathEvent event){clear(event.getEntity());}
    @SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){clear(event.getEntity());}
    private static void clear(LivingEntity target){SLOWS.remove(target.getUUID());LAST.remove(target.getUUID());Physique.remove(target,Attributes.ATTACK_SPEED,ID);}
    @SubscribeEvent public static void cleanup(net.minecraftforge.event.TickEvent.ServerTickEvent.Post event) {
        for(var entries:java.util.List.copyOf(SLOWS.values()))for(var slow:java.util.List.copyOf(entries.values())) {
            if(slow.target.isRemoved()||!slow.target.isAlive())clear(slow.target);else update(slow.target);
        }
    }
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){SLOWS.clear();LAST.clear();}
}
