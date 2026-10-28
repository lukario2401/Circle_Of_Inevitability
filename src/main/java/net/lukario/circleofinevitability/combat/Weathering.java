package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.status.Construct;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Marks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/** Speeds up known timed effects. Custom statuses register a typed adapter; arbitrary save data is never edited. */
public final class Weathering {
    private static final List<BiConsumer<LivingEntity,Integer>> ADAPTERS=new ArrayList<>();
    private Weathering() {}
    public static void addAdapter(BiConsumer<LivingEntity,Integer> adapter){ADAPTERS.add(adapter);}
    public static void effects(LivingEntity entity,int ticks,boolean beneficialOnly){
        if(ticks<=0)return;
        for(MobEffectInstance effect:new ArrayList<>(entity.getActiveEffects())) {
            if(effect.isInfiniteDuration() || beneficialOnly&&!effect.getEffect().value().isBeneficial())continue;
            int remaining=effect.getDuration()-ticks;
            entity.removeEffect(effect.getEffect());
            if(remaining>0)entity.addEffect(new MobEffectInstance(effect.getEffect(),remaining,effect.getAmplifier(),effect.isAmbient(),effect.isVisible(),effect.showIcon()));
        }
    }
    public static void area(LivingEntity owner,ServerLevel sl,Vec3 center,double radius,int extraTicks){
        if(extraTicks<=0)return;
        for(LivingEntity target:Spells.enemiesAround(owner,sl,center,radius)) {
            effects(target,extraTicks,false);Marks.age(target,extraTicks);
            for(var adapter:List.copyOf(ADAPTERS))adapter.accept(target,extraTicks);
            if(Construct.isBuilt(target))Construct.extend(target,-extraTicks);
            else if(Marionette.isSummoned(target))Marionette.ageSummon(target,extraTicks);
        }
        Zone.ageNear(sl,center,radius,e->Spells.isEnemy(owner,e),extraTicks);
        Dome.ageNear(sl,center,radius,e->Spells.isEnemy(owner,e),extraTicks);
        Wall.ageNear(sl,center,radius,e->Spells.isEnemy(owner,e),extraTicks);
    }
}
