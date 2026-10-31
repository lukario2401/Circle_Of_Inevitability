package net.lukario.circleofinevitability.combat;
import java.util.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** A temporary, one-use loan of a concrete key variant OR wheel entry, independent of the recipient's pathway. */
@Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class AbilityGrants {
    private record Grant(Casts.Cast cast,long until,float cost,float potency){}
    private static final Map<UUID,Grant> GRANTS=new HashMap<>();
    public static boolean give(ServerPlayer recipient,Casts.Cast cast,int ticks,float cost,float potency){
        if(cast==null || !cast.copyable() || ticks<=0)return false;
        GRANTS.put(recipient.getUUID(),new Grant(cast,recipient.level().getGameTime()+ticks,Math.max(0,cost),Math.max(0,potency)));
        HudTimer.show(recipient,"spellkit_grant",cast.name()+" — /castgrant",ticks,0xD1A94B);return true;
    }
    public static boolean has(ServerPlayer p){var g=GRANTS.get(p.getUUID());if(g==null)return false;if(g.until<=p.level().getGameTime()){clear(p);return false;}return true;}
    public static void clear(ServerPlayer p){GRANTS.remove(p.getUUID());HudTimer.hide(p,"spellkit_grant");}
    public static boolean use(ServerPlayer p){
        if(!has(p) || Root.has(p,RootRestriction.ABILITIES) || AbilityLock.isLocked(p) || Decree.blocksKey(p,4) || KeySeal.blocksKey(p,4))return false;
        var g=GRANTS.get(p.getUUID());
        boolean success=NextCast.run(p,()->Echo.weaker(p,g.potency,()->Casts.recastPaid(p,g.cast,g.cost*NextCast.cost(p))));
        if(success){clear(p);NextCast.used(p);}return success;
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){event.getDispatcher().register(Commands.literal("castgrant").executes(ctx->{
        ServerPlayer p=ctx.getSource().getPlayerOrException();boolean used=use(p);
        if(!used)p.sendSystemMessage(Component.literal("No usable borrowed power, no target, or insufficient spirituality."));return used?1:0;
    }));}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public static void died(LivingDeathEvent event){if(event.getEntity() instanceof ServerPlayer p)clear(p);}
}
