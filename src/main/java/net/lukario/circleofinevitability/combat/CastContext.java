package net.lukario.circleofinevitability.combat;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.world.entity.player.Player;
/** The stable identity of the power currently checking/paying its price. Nested borrowed casts restore the caller. */
public final class CastContext {
    private static final Map<UUID,String> ACTIVE=new HashMap<>();
    public static String id(Player p){return ACTIVE.getOrDefault(p.getUUID(),"");}
    public static void begin(Player p,String id){ACTIVE.put(p.getUUID(),id);}
    public static void end(Player p){ACTIVE.remove(p.getUUID());}
    public static <T> T run(Player p,String id,Supplier<T> action){
        String before=ACTIVE.put(p.getUUID(),id);
        try{return action.get();}finally{if(before==null)end(p);else begin(p,before);}
    }
}
