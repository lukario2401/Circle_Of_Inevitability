package net.lukario.circleofinevitability.combat;

import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

/** Owner-only vanilla outline metadata, restored on expiry; server entity flags remain untouched. */
public final class EntitySight {
    private record Seen(LivingEntity target,long until){}
    private static final Map<ServerPlayer,Map<UUID,Seen>> SEEN=new HashMap<>();
    private EntitySight(){}
    private static void send(ServerPlayer viewer,LivingEntity target,boolean on){
        byte flags=0;var values=target.getEntityData().getNonDefaultValues();
        if(values!=null)for(var value:values)if(value.id()==0&&value.value() instanceof Byte b)flags=b;
        if(on)flags|=64;
        viewer.connection.send(new ClientboundSetEntityDataPacket(target.getId(),List.of(new SynchedEntityData.DataValue<>(0,EntityDataSerializers.BYTE,flags))));
    }
    public static void show(ServerPlayer viewer,LivingEntity target,int ticks){
        SEEN.computeIfAbsent(viewer,p->new HashMap<>()).put(target.getUUID(),new Seen(target,viewer.level().getGameTime()+ticks));send(viewer,target,true);
    }
    public static void tick(ServerPlayer viewer){
        var seen=SEEN.get(viewer);if(seen==null)return;
        for(var entry:new ArrayList<>(seen.entrySet()))if(!entry.getValue().target.isAlive()||entry.getValue().target.level()!=viewer.level()||entry.getValue().until<=viewer.level().getGameTime()){
            send(viewer,entry.getValue().target,false);seen.remove(entry.getKey());
        }
        if(seen.isEmpty())SEEN.remove(viewer);
    }
    public static void clear(net.minecraft.world.entity.player.Player player){
        if(!(player instanceof ServerPlayer viewer))return;var seen=SEEN.remove(viewer);if(seen!=null)for(var value:seen.values())send(viewer,value.target,false);
    }
}
