package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker;

import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobForm;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker.NatureWalker.*;

final class NatureWalkerEarth {
    static final String SWAMP=PREFIX+"swamp", OAK=PREFIX+"oak", BEAR=PREFIX+"bear";
    static final class Journey {
        final Vec3 direction;final long priorHiddenUntil,started;int age,hops;
        Journey(State s) { direction=Spells.flatLook(s.owner);started=s.level.getGameTime();priorHiddenUntil=s.level.getGameTime()+Concealment.ticksLeft(s.owner); }
    }
    static boolean cast(State s,String id) {
        long now=s.level.getGameTime();
        switch(id) {
            case "underground_travel" -> {
                if(!Teleport.canGo(s.owner)||!Terrain.supported(s.level,s.owner.position()))return false;
                s.journey=new Journey(s);Concealment.hide(s.owner,45);
                if(!Concealment.isHidden(s.owner)) { s.journey=null;return false; }
                Root.applyOwned(s.owner,tag(s,"journey"),45,EnumSet.of(RootRestriction.MOVE,RootRestriction.JUMP));
                effect(s,"earth_portal",s.owner.position(),25,2);
            }
            case "swamp" -> {
                Vec3 at=Spells.aimGround(s.owner,s.level,24);if(!Terrain.supported(s.level,at))return false;
                Set<UUID> buried=new HashSet<>();
                Zone.at(at,10).height(5).lasts(200).every(5).countsTo(80).affects(e->enemy(s.owner,e))
                        .onInside((e,ticks)-> {
                            if(!valid(s))return;
                            Spells.keepEffect(e,MobEffects.SLOWNESS,Math.min(2,ticks/40),12);
                            if(ticks>=40)Root.applyOwned(e,tag(s,"swamp_jump"),12,EnumSet.of(RootRestriction.JUMP));
                            if(ticks>=80&&buried.add(e.getUUID()))Burial.bury(e,20,40);
                        }).onLeave(e->Root.removeOwned(e,tag(s,"swamp_jump")))
                        .onEnd(()->ParticleShapes.clearModels(s.level,at,tag(s,"mire_disc")))
                        .open(s.owner,s.level,SWAMP);
                effect(s,"mire_disc",at.add(0,.02,0),200,20);
            }
            case "wrath_of_nature" -> {
                int cap=s.sequence<=1?50:s.sequence==2?20:s.sequence==3?12:s.sequence==4?8:5;
                var targets=Spells.enemiesInSight(s.owner,s.level,24,45).stream().filter(e->enemy(s.owner,e))
                        .sorted(Comparator.comparingDouble(s.owner::distanceToSqr)).limit(cap).toList();
                if(targets.isEmpty())return false;
                s.wrath.clear();for(var target:targets)s.wrath.put(target.getUUID(),now+300);
            }
            case "child_of_the_oak" -> { s.oakUntil=now+600;s.oakStone=null;oak(s); }
            case "bear_form" -> bear(s);
            default -> { return false; }
        }
        return true;
    }
    static void surface(State s,boolean attack) {
        Journey journey=s.journey;if(journey==null)return;s.journey=null;
        Root.removeOwned(s.owner,tag(s,"journey"));
        long now=s.level.getGameTime();
        // Preserve concealment supplied by another ability before or during the journey.
        if(Concealment.ticksLeft(s.owner)<=Math.max(0,journey.started+45-now)+1) {
            Concealment.reveal(s.owner);
            int prior=(int)(journey.priorHiddenUntil-now);if(prior>0)Concealment.hide(s.owner,prior);
        }
        FallGuard.protect(s.owner,60);
        if(attack&&valid(s)) {
            for(var e:enemies(s,s.owner.position(),3)) { strike(s,e,15,false);Root.apply(e,30,RootRestriction.MOVE,RootRestriction.JUMP); }
            effect(s,"earth_portal",s.owner.position(),20,2);sound(s,"root_impact");
        }
    }
    private static void travel(State s) {
        Journey j=s.journey;if(j==null)return;
        if(!Teleport.canGo(s.owner)||!Concealment.isHidden(s.owner)) { surface(s,true);return; }
        j.age++;if(j.age<20||j.age%2!=0)return;
        Vec3 next=s.owner.position().add(j.direction.scale(2.4));Vec3 safe=null;
        for(int dy:new int[]{0,1,-1}) {
            Vec3 candidate=next.add(0,dy,0);
            if(Terrain.supported(s.level,candidate)&&Teleport.fits(s.owner,s.level,candidate)) { safe=candidate;break; }
        }
        if(safe==null||!Teleport.of(s.owner).go(s.level,safe)) { surface(s,true);return; }
        if(++j.hops>=10)surface(s,true);
    }
    private static void oak(State s) {
        boolean stone=Terrain.under(s.owner)==Terrain.Kind.STONE;
        if(s.oakStone!=null&&s.oakStone==stone)return;
        s.oakStone=stone;
        ConjuredArmor.takeOff(s.owner,OAK);
        var armor=ConjuredArmor.of(OAK).lasts((int)(s.oakUntil-s.level.getGameTime())).cut(stone?.45f:.3f);
        if(stone)armor.steady();else armor.absorption(6).regrow(7);
        armor.wear(s.owner);
        NatureWalkerFx.form(s.level,s.owner,stone?"bark_skin_stone":"bark_skin_wood",(int)(s.oakUntil-s.level.getGameTime()),tag(s,"oak"),1);
    }
    static boolean isBear(LivingEntity e) { return e instanceof Player p?Form.isIn(p,BEAR):MobForm.isIn(e,BEAR); }
    private static void bear(State s) {
        var add=AttributeModifier.Operation.ADD_VALUE;var multiply=AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        if(s.owner instanceof Player p) {
            Form.of(BEAR).lasts(400).attribute(Attributes.SCALE,1,multiply)
                    .attribute(Attributes.MAX_HEALTH,12,add).attribute(Attributes.ATTACK_DAMAGE,10,add)
                    .attribute(Attributes.ENTITY_INTERACTION_RANGE,1,add).attribute(Attributes.STEP_HEIGHT,1,add)
                    .cutsDamage(.2f).effect(MobEffects.INVISIBILITY,0).keepWhile(x->valid(s))
                    .onEnd((x,why)-> {
                        ParticleShapes.clearModels(s.level,x,tag(s,"bear"));
                        if(why==Form.Ending.TIME||why==Form.Ending.ENDED)Spells.keepEffect(x,MobEffects.WEAKNESS,0,100);
                    }).take(p);
        } else if(s.owner instanceof Mob mob) {
            MobForm.of(BEAR).lasts(400).attribute(Attributes.SCALE,1,multiply)
                    .attribute(Attributes.MAX_HEALTH,12,add).attribute(Attributes.ATTACK_DAMAGE,10,add)
                    .attribute(Attributes.STEP_HEIGHT,1,add).cutsDamage(.2f).effect(MobEffects.INVISIBILITY,0)
                    .onEnd(x->{ParticleShapes.clearModels(s.level,x,tag(s,"bear"));if(x.isAlive())Spells.keepEffect(x,MobEffects.WEAKNESS,0,100);}).take(mob);
        }
        NatureWalkerFx.form(s.level,s.owner,"bear_form",240,tag(s,"bear"),2);
        for(var e:enemies(s,s.owner.position(),6))if(e instanceof Mob&&e.getMaxHealth()<=60)Fear.apply(e,s.owner,40);
    }
    private static void hazard(State s,LivingEntity e) {
        Terrain.Kind ground=Terrain.under(e);BlockPos pos=e.blockPosition();
        if(ground==Terrain.Kind.SOIL||ground==Terrain.Kind.WOOD) {
            strike(s,e,8,false);Root.apply(e,40,RootRestriction.MOVE,RootRestriction.JUMP);effect(s,"vine_burst",e.position(),20,2);return;
        }
        for(int y=2;y<=6;y++)if(Terrain.at(s.level,pos.above(y))==Terrain.Kind.STONE) {
            strike(s,e,25,false);effect(s,"falling_stone",e.position().add(0,2,0),20,1.5f);return;
        }
        for(var dir:List.of(new Vec3(1,0,0),new Vec3(-1,0,0),new Vec3(0,0,1),new Vec3(0,0,-1))) {
            boolean drop=true;
            for(int depth=1;depth<=4;depth++) {
                BlockPos test=BlockPos.containing(e.position().add(dir.scale(2)).add(0,-depth,0));
                if(!s.level.hasChunkAt(test)||Terrain.at(s.level,test)!=Terrain.Kind.NONE) { drop=false;break; }
            }
            if(drop) { e.push(dir.x*.8,.15,dir.z*.8);return; }
        }
        for(BlockPos test:BlockPos.betweenClosed(pos.offset(-2,-1,-2),pos.offset(2,1,2)))if(Terrain.at(s.level,test)==Terrain.Kind.WATER) {
            Weight.put(e,2,60);strike(s,e,6,false);effect(s,"undertow",e.position(),20,2);return;
        }
        if(s.level.isRainingAt(pos)||s.raining&&s.weatherUntil>s.level.getGameTime()&&s.weatherAt.distanceToSqr(e.position())<Math.pow(24*verdancy(s.owner),2)) {
            strike(s,e,30,false);effect(s,"lightning",e.position(),15,2);return;
        }
        Burial.bury(e,10,10);
    }
    static void tick(State s) {
        travel(s);long now=s.level.getGameTime();
        if(s.oakUntil>now) { if(now%5==0)oak(s); }
        else if(s.oakUntil!=0) { s.oakUntil=0;ConjuredArmor.takeOff(s.owner,OAK);ParticleShapes.clearModels(s.level,s.owner,tag(s,"oak")); }
        if(isBear(s.owner)&&now%200==0)NatureWalkerFx.form(s.level,s.owner,"bear_form",240,tag(s,"bear"),2);
        if(now%20==0)for(var entry:new ArrayList<>(s.wrath.entrySet())) {
            var e=s.level.getEntity(entry.getKey());
            if(entry.getValue()<=now||!(e instanceof LivingEntity living)||!enemy(s.owner,living)||s.owner.distanceToSqr(living)>48*48)s.wrath.remove(entry.getKey());
            else hazard(s,living);
        }
    }
    static void clear(State s) {
        surface(s,false);Zone.close(s.owner,SWAMP);ConjuredArmor.takeOff(s.owner,OAK);s.wrath.clear();
        if(s.owner instanceof Player p&&Form.isIn(p,BEAR))Form.end(p);
        if(s.owner instanceof Mob m&&MobForm.isIn(m,BEAR))MobForm.end(m);
        ParticleShapes.clearModels(s.level,s.owner,tag(s,"oak"));ParticleShapes.clearModels(s.level,s.owner,tag(s,"bear"));
        if(s.weatherAt!=null)ParticleShapes.clearModels(s.level,s.weatherAt,tag(s,"weather"));
    }
}
