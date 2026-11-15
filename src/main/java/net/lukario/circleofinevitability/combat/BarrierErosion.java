package net.lukario.circleofinevitability.combat;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Per-channel barrier exposure: weaken on contact, break after continuous exposure.
 * New barriers with the same name start a new timer. Call once per server tick; dropping contact resets it. */
public final class BarrierErosion {
    private final Map<Object,Long> first=new IdentityHashMap<>();
    public void tick(ServerLevel level,Vec3 from,Vec3 to,Predicate<LivingEntity> hostile,int breakAfter) {
        Set<Object> touched=Collections.newSetFromMap(new IdentityHashMap<>());long now=level.getGameTime();
        for(var shell:Dome.crossedBy(level,from,to))if(hostile.test(shell.owner())) {
            Object token=Dome.snapshot(shell.owner(),shell.name());if(token==null)continue;
            touched.add(token);Dome.weaken(shell.owner(),shell.name(),3);
            if(now-first.computeIfAbsent(token,k->now)>=breakAfter)Dome.closeIf(shell.owner(),shell.name(),token);
        }
        for(var wall:Wall.crossedBy(level,from,to))if(hostile.test(wall.owner())) {
            Object token=wall.token();touched.add(token);Wall.weaken(wall.owner(),wall.name(),3);
            if(now-first.computeIfAbsent(token,k->now)>=breakAfter)Wall.closeIf(wall.owner(),wall.name(),token);
        }
        first.keySet().retainAll(touched);
    }
    public void clear(){first.clear();}
}
