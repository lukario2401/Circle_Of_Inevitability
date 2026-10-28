package net.lukario.circleofinevitability.combat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;
import java.util.function.ToIntFunction;

/** Reusable delayed cast entry point. Rules consume one-shot delays only when an action is accepted. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class HeldActions {
    private static final List<ToIntFunction<LivingEntity>> RULES=new ArrayList<>();
    private static final Set<UUID> REPLAYING=new HashSet<>();
    private record Ticket(UUID owner, ServerLevel level) {}
    private static final Set<Ticket> PENDING=Collections.newSetFromMap(new IdentityHashMap<>());
    private static long pending(LivingEntity e) { return PENDING.stream().filter(t->t.owner.equals(e.getUUID())).count(); }
    private HeldActions(){}
    public static void addRule(ToIntFunction<LivingEntity> rule){RULES.add(rule);}
    public static boolean replaying(LivingEntity entity){return REPLAYING.contains(entity.getUUID());}
    public static int delay(LivingEntity entity){
        if(replaying(entity))return 0;
        int ticks=0;for(var rule:RULES)ticks=Math.max(ticks,rule.applyAsInt(entity));return ticks;
    }
    public static boolean defer(ServerPlayer player,Runnable action){
        if(replaying(player))return false;
        if(pending(player)>=8)return true;
        int ticks=delay(player);if(ticks<=0)return false;
        ServerLevel sl=player.level();float yaw=player.getYRot(),pitch=player.getXRot();boolean sneak=player.isShiftKeyDown();
        String pathway=net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore.getPathway(player);
        Ticket ticket=new Ticket(player.getUUID(),sl); PENDING.add(ticket);
        Later.run(sl,ticks,()->{
            if(!PENDING.remove(ticket))return;
            if(!Spells.casterStillHere(player,sl)||player.hasDisconnected()
                    ||!pathway.equals(net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore.getPathway(player)))return;
            float oldYaw=player.getYRot(),oldPitch=player.getXRot();boolean oldSneak=player.isShiftKeyDown();
            player.setYRot(yaw);player.setXRot(pitch);player.setShiftKeyDown(sneak);REPLAYING.add(player.getUUID());
            try{action.run();}finally{REPLAYING.remove(player.getUUID());player.setYRot(oldYaw);player.setXRot(oldPitch);player.setShiftKeyDown(oldSneak);}
        });
        return true;
    }
    /** Mobs keep one pending cast; retrying AI cannot flood the delayed queue. Rechecks belong in action. */
    public static boolean defer(LivingEntity entity,Runnable action){
        if(entity instanceof ServerPlayer player)return defer(player,action);
        if(replaying(entity)||!(entity.level() instanceof ServerLevel sl))return false;
        if(pending(entity)>0)return true;
        int ticks=delay(entity);if(ticks<=0)return false;
        Ticket ticket=new Ticket(entity.getUUID(),sl);PENDING.add(ticket);
        Later.run(sl,ticks,()->{
            if(!PENDING.remove(ticket)||!Spells.casterStillHere(entity,sl))return;
            replay(entity,action);
        });
        return true;
    }
    public static void clear(LivingEntity entity){PENDING.removeIf(t->t.owner.equals(entity.getUUID()));}
    @net.minecraftforge.eventbus.api.listener.SubscribeEvent
    public static void unload(net.minecraftforge.event.level.LevelEvent.Unload event){PENDING.removeIf(t->t.level==event.getLevel());}
    @net.minecraftforge.eventbus.api.listener.SubscribeEvent
    public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event){clear(event.getEntity());}
    @net.minecraftforge.eventbus.api.listener.SubscribeEvent
    public static void dimension(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event){clear(event.getEntity());}
    public static void replay(LivingEntity entity,Runnable action){
        boolean fresh=REPLAYING.add(entity.getUUID());try{action.run();}finally{if(fresh)REPLAYING.remove(entity.getUUID());}
    }
}
