package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker;

import java.util.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker.NatureWalker.*;

/** An owner's transient garden. Stages, weather and quickening all advance the same age clock. */
final class NatureWalkerGrowths {
    enum Kind {
        THORNLING("thornling",60,240), HEARTFRUIT("heartfruit",80,300),
        BRIAR_ORCHARD("briar_orchard",80,300), SPOREBED("sporebed",1,160), MOTHER_TREE("mother_tree",1,6000);
        final String model;final int mature,lifetime;
        Kind(String model,int mature,int lifetime) { this.model=model;this.mature=mature;this.lifetime=lifetime; }
    }
    static final class Plant {
        final Kind kind;final Vec3 at;final String tag;
        final Growth clock;long focusUntil;UUID focused;
        String field;boolean temporary;
        boolean eaten;String stage="seed";
        Plant(Kind kind,Vec3 at,String tag) { this.kind=kind;this.at=at;this.tag=tag;clock=new Growth(Math.min(20,kind.mature),kind.mature,20); }
    }
    private NatureWalkerGrowths() {}
    static int cap(State s) { return 3+2*(9-s.sequence); }
    static boolean canPlant(State s,Vec3 at) {
        if(!Terrain.supported(s.level,at))return false;
        Terrain.Kind ground=Terrain.at(s.level,BlockPos.containing(at.add(0,-.1,0)));
        return (s.sequence<=2||ground==Terrain.Kind.SOIL||ground==Terrain.Kind.WOOD)
                && s.level.getBlockState(BlockPos.containing(at.add(0,.1,0))).getCollisionShape(s.level,BlockPos.containing(at.add(0,.1,0))).isEmpty();
    }
    static List<Plant> near(State s,Vec3 at,double radius) {
        return s.plants.stream().filter(p->!p.clock.withering()&&p.at.distanceToSqr(at)<=radius*radius).toList();
    }
    static Plant plant(State s,Kind kind,Vec3 at) {
        return add(s,kind,at,null,0);
    }
    static Plant temporary(State s,Kind kind,Vec3 at,String field,int limit) { return add(s,kind,at,field,limit); }
    private static Plant add(State s,Kind kind,Vec3 at,String field,int limit) {
        if(!valid(s)||!canPlant(s,at))return null;
        if(field!=null&&s.plants.stream().filter(p->field.equals(p.field)).count()>=limit)return null;
        if(field==null&&(kind==Kind.THORNLING||kind==Kind.HEARTFRUIT)) {
            Kind opposite=kind==Kind.THORNLING?Kind.HEARTFRUIT:Kind.THORNLING;
            Plant neighbor=near(s,at,3).stream().filter(p->p.kind==opposite).findFirst().orElse(null);
            if(neighbor!=null&&!neighbor.temporary) { remove(s,neighbor);kind=Kind.BRIAR_ORCHARD;sound(s,"cross_pollinate"); }
        }
        while(field==null&&s.plants.stream().filter(p->!p.temporary).count()>=cap(s))
            remove(s,s.plants.stream().filter(p->!p.temporary&&p.kind!=Kind.MOTHER_TREE).findFirst()
                    .orElseGet(()->s.plants.stream().filter(p->!p.temporary).findFirst().orElseThrow()));
        Plant plant=new Plant(kind,at,tag(s,"plant_"+UUID.randomUUID()));plant.field=field;plant.temporary=field!=null;s.plants.add(plant);
        show(s,plant);return plant;
    }
    static void ripen(State s,Plant plant) { if(plant!=null)plant.clock.ripen(); }
    static void show(State s,Plant plant) { NatureWalkerFx.growth(s.level,plant.at,plant.kind.model,plant.stage,45,plant.tag); }
    static void tick(State s) {
        long now=s.level.getGameTime();
        for(Plant p:new ArrayList<>(s.plants)) {
            if(!s.level.hasChunkAt(BlockPos.containing(p.at))) { remove(s,p);continue; }
            boolean rain=s.level.isRainingAt(BlockPos.containing(p.at))
                    ||s.raining&&s.weatherUntil>now&&s.weatherAt!=null&&s.weatherAt.distanceToSqr(p.at)<=Math.pow(24*verdancy(s.owner),2);
            var stage=p.clock.tick(now,verdancy(s.owner)*(rain?2:1)*NatureWalkerAdvanced.growthSpeed(s,p.at),duration(s.owner,p.kind.lifetime));
            if(stage==Growth.Stage.DEAD) { remove(s,p);continue; }
            String next=stage.name().toLowerCase(java.util.Locale.ROOT);
            if(stage==Growth.Stage.BLOOM&&(p.kind==Kind.HEARTFRUIT||p.kind==Kind.MOTHER_TREE)&&!p.eaten)next="ripe";
            if(!next.equals(p.stage)) {
                p.stage=next;show(s,p);sound(s,next.equals("sprout")?"growth_sprout":"growth_bloom");
            } else if(now%30==0)show(s,p);
            if(!p.clock.mature()||p.clock.withering())continue;
            if(now%10==0&&(p.kind==Kind.THORNLING||p.kind==Kind.BRIAR_ORCHARD||p.kind==Kind.MOTHER_TREE)) {
                for(var e:enemies(s,p.at,2.5)) {
                    float focused=p.focusUntil>now&&e.getUUID().equals(p.focused)?1.5f:1;
                    strike(s,e,3*focused*(p.kind==Kind.BRIAR_ORCHARD?1.15f:1),true);
                    Spells.keepEffect(e,MobEffects.SLOWNESS,0,20);Bleeding.apply(e,s.owner,60,1);
                }
            }
            if(p.kind==Kind.MOTHER_TREE&&now%40==0) {
                Vec3 point=Spells.groundAt(s.owner,s.level,p.at.add(Math.cos(now)*3,1,Math.sin(now)*3));
                plant(s,Kind.HEARTFRUIT,point);
            }
            if(now%10==0&&p.kind==Kind.SPOREBED)for(var e:enemies(s,p.at,3)) {
                Spells.keepEffect(e,MobEffects.POISON,0,40);Spells.keepEffect(e,MobEffects.NAUSEA,0,40);
                if(s.sequence<=4&&now%20==0)Madness.add(s.owner,e,1);
                if(s.sequence<=4&&now%20==0)Mutation.hit(s.owner,e,Mutation.Kind.FUNGUS,1);
            }
            if(!p.eaten&&(p.kind==Kind.HEARTFRUIT||p.kind==Kind.BRIAR_ORCHARD)&&now%5==0) {
                var eater=friends(s,p.at,1.5).stream().filter(e->e.getHealth()<e.getMaxHealth()&&!NoHeal.isApplied(e))
                        .min(Comparator.comparingDouble(e->e.getHealth()/e.getMaxHealth())).orElse(null);
                if(eater!=null) {
                    float amount=eater.getMaxHealth()*.3f*verdancy(s.owner)*(s.sequence<=7?1.25f:1);
                    if(Healing.apply(s.owner,eater,amount)>0) {
                        Spells.keepEffect(eater,MobEffects.REGENERATION,0,80);p.eaten=true;
                        p.clock.expireBy(now+100);sound(s,"heartfruit_eat");
                    }
                }
            }
        }
    }
    static void harvest(State s,Plant p) {
        if(p.kind==Kind.HEARTFRUIT||p.kind==Kind.BRIAR_ORCHARD) {
            if(!p.eaten)for(var e:friends(s,p.at,3)) {
                Healing.apply(s.owner,e,e.getMaxHealth()*.3f*verdancy(s.owner)*(s.sequence<=7?1.25f:1));
                Spells.keepEffect(e,MobEffects.REGENERATION,0,80);
            }
        }
        if(p.kind==Kind.THORNLING||p.kind==Kind.BRIAR_ORCHARD||p.kind==Kind.MOTHER_TREE)for(var e:enemies(s,p.at,3))strike(s,e,15,true);
        remove(s,p);
    }
    static void remove(State s,Plant p) { s.plants.remove(p);ParticleShapes.clearModels(s.level,p.at,p.tag); }
    static void clear(State s) { for(var p:new ArrayList<>(s.plants))remove(s,p); }
}
