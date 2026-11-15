package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.filthy_monarch;

import java.util.*;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.filthy_monarch.FilthyMonarch.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/** Foul Tongue targeting, Seed finishers and owned debuffs. Silence is enforced at the common cast gate. */
final class FilthyMonarchTongue {
    static LivingEntity wordTarget(State s,double range) {
        return Spells.aimLiving(s.player,s.level,range*(s.sequence<=3?2:1),s.sequence<=3,
                t->enemy(s,t)&&Spells.canBeChosen(s.player,t));
    }
    static AbilityWheel.Result cast(State s,String id) {
        var p=s.player;LivingEntity target=null;
        if(Set.of("die","sloth","blasphemy").contains(id)) {
            target=wordTarget(s,24);if(target==null)return refuse(p,"Look toward an enemy in word range.");
        }
        switch(id) {
            case "slow" -> {
                for(var t:around(s,p.position(),8)){Spells.strike(p,t,15);Spells.keepEffect(t,MobEffects.SLOWNESS,4,Spells.isBoss(t)?20:40);}
                fx(s,id,p.position(),40);
            }
            case "die" -> {
                int n=Depravity.spend(p,target);
                boolean execute=n>=3&&!Spells.isBoss(target)&&target.getHealth()<=target.getMaxHealth()*.3f;
                Spells.strike(p,target,execute?Math.max(60+30*n,target.getHealth()*10):60+30*n);
                Bleeding.apply(target,p,80,3);fx(s,id,target.position(),30);
            }
            case "corruption" -> {
                var t=aim(s,22);Vec3 at=t==null?Spells.aimGround(p,s.level,22):t.position();
                for(var e:around(s,at,4)){magic(s,e,45);seed(s,e,2);}
                FilthyMonarchBlood.field(s,"corruption",at,4,100,6,false);FilthyMonarchFx.sound(s.level,at,id);
            }
            case "confusion" -> {for(var t:around(s,p.position(),12))confuse(s,t,120);fx(s,id,p.position(),120);}
            case "sloth" -> {
                Spells.keepEffect(target,MobEffects.SLOWNESS,2,160);Spells.keepEffect(target,MobEffects.WEAKNESS,1,160);
                AttackTempo.apply(target,s.tag("sloth"),.5,160);track(s,target,160);
                afflict(s,target,"sloth",160,12);fx(s,id,target.position(),160);
            }
            case "blasphemy" -> {
                int n=Depravity.spend(p,target);magic(s,target,130+20*n);Silence.apply(target,60+20*n);
                fx(s,id,target.position(),60+20*n);
            }
            case "blather" -> {
                target=aim(s,40);
                if(target==null)target=around(s,p.position(),100).stream().filter(t->Depravity.count(p,t)>0&&Spells.canBeChosen(p,t))
                        .max(Comparator.<LivingEntity>comparingInt(t->Depravity.count(p,t)).thenComparingDouble(t->-p.distanceToSqr(t))).orElse(null);
                if(target==null)return refuse(p,"Look at an enemy or plant Seeds on one within 100 blocks.");
                int n=Depravity.spend(p,target);magic(s,target,150+40*n);confuse(s,target,60);
                s.curses.put(target.getUUID(),s.now()+240);fx(s,id,target.position(),60);
            }
            default -> {return AbilityWheel.Result.REFUSED;}
        }
        return AbilityWheel.Result.DONE;
    }
    static void confuse(State s,LivingEntity t,int ticks) {
        Falter.apply(t,s.tag("confusion"),.4f,ticks);Spells.keepEffect(t,MobEffects.NAUSEA,0,ticks);track(s,t,ticks);
        if(t instanceof Mob) {
            var nearest=around(s,t.position(),24).stream().filter(e->e!=t).min(Comparator.comparingDouble(t::distanceToSqr)).orElse(s.player);
            if(Taunt.apply(t,nearest,ticks))s.taunts.put(t.getUUID(),Taunt.snapshot(t));
        }
    }
    static void afflict(State s,LivingEntity t,String kind,int ticks,float damage) {
        s.afflictions.put(kind+"_"+t.getUUID(),new Affliction(t,s.now()+ticks,s.now()+20,damage,kind));
    }
}
