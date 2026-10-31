package net.lukario.circleofinevitability.combat;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.world.entity.player.Player;
/** One-shot cost/damage modifier. Refused casts and opening menus do not spend it; Echo carries delayed damage. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class NextCast {
    private record Grant(long until,float cost,float strength){}
    private static final Map<UUID,Grant> GRANTS=new HashMap<>();
    private static final Set<UUID> RUNNING=new HashSet<>();
    static {Spells.addCostScale(NextCast::cost);}
    public static void arm(Player p,int ticks,float cost,float strength){GRANTS.put(p.getUUID(),new Grant(p.level().getGameTime()+ticks,Math.max(0,cost),Math.max(0,strength)));}
    private static Grant live(Player p){var g=GRANTS.get(p.getUUID());if(g!=null && g.until<=p.level().getGameTime()){used(p);return null;}return g;}
    public static float cost(Player p){var g=live(p);return g==null?1:g.cost;}
    public static boolean active(Player p){return live(p)!=null;}
    public static void used(Player p){GRANTS.remove(p.getUUID());}
    @net.minecraftforge.eventbus.api.listener.SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){used(e.getEntity());}
    @net.minecraftforge.eventbus.api.listener.SubscribeEvent public static void died(net.minecraftforge.event.entity.living.LivingDeathEvent e){if(e.getEntity() instanceof Player p)used(p);}
    public static <T> T run(Player p,Supplier<T> action){
        var grant=live(p);if(grant==null || !RUNNING.add(p.getUUID()))return action.get();
        try {Object[] answer={null};Echo.weaker(p,grant.strength,()->{answer[0]=action.get();return true;});
            @SuppressWarnings("unchecked") T result=(T)answer[0];return result;
        }finally{RUNNING.remove(p.getUUID());}
    }
}
