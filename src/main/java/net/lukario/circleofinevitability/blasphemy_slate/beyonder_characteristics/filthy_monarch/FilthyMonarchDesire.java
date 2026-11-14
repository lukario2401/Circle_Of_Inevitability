package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.filthy_monarch;

import java.util.*;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.filthy_monarch.FilthyMonarch.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.particles.fx.*;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;

/** First four Desire and Malice entries; unimplemented entries are deliberately absent. */
final class FilthyMonarchDesire {
    private FilthyMonarchDesire() {}
    static AbilityWheel.Result cast(State s,String id) {
        var p=s.player;
        switch(id) {
            case "shadow_shift" -> {
                Vec3 from=p.position(),to=Spells.blinkSpot(p,s.level,24);
                if(to==null||!s.level.getWorldBorder().isWithinBounds(BlockPos.containing(to))||!Teleport.of(p).fallGuard(80).go(s.level,to))
                    return refuse(p,"There is no safe shadow to flow into.");
                for(LivingEntity t:Spells.enemiesOnLine(p,s.level,from.add(0,.8,0),to.add(0,.8,0),1))if(enemy(s,t)) {
                    Spells.strike(p,t,30);seed(s,t,1);
                }
                String tag=s.tag("afterimage_"+UUID.randomUUID());
                ParticleShapes.model(s.level,FilthyMonarchFx.model("shadow_afterimage",30,tag),from,p.getYRot(),0,0);
                ParticleShapes.line(s.level,FilthyMonarchFx.particle("shadow",FilthyMonarchFx.BLACK),from,to,3);
                FilthyMonarchFx.sound(s.level,from,id);
                later(s,30,()->{
                    for(LivingEntity t:around(s,from,3))Spells.strike(p,t,40);
                    ParticleShapes.burst(s.level,FilthyMonarchFx.particle("shadow",FilthyMonarchFx.BLACK),from,24,.04,.2);
                    FilthyMonarchFx.sound(s.level,from,"illusion_burst");
                });
            }
            case "desire_control" -> {
                LivingEntity target=aim(s,24);if(target==null)return refuse(p,"Look at an enemy within 24 blocks.");
                List<LivingEntity> targets=s.sequence<=4?around(s,target.position(),10):List.of(target);
                for(LivingEntity t:targets) {
                    Spells.strike(p,t,40);seed(s,t,3);
                    Falter.apply(t,s.tag("desire"),.4f,100);track(s,t,100);s.noSprint.put(t.getUUID(),s.now()+100);
                    if(t instanceof Mob) {
                        LivingEntity nearest=around(s,t.position(),16).stream().filter(other->other!=t)
                                .min(Comparator.comparingDouble(t::distanceToSqr)).orElse(p);
                        if(Taunt.apply(t,nearest,100))s.taunts.put(t.getUUID(),Taunt.snapshot(t));
                    }
                    if(Depravity.count(p,t)>=5){Spells.strike(p,t,100);hold(s,t,"desire",20,RootRestriction.MOVEMENT);}
                    fx(s,id,t.position(),30);
                }
            }
            case "mental_shock" -> {
                for(LivingEntity t:cone(s,12,35)) {
                    magic(s,t,55);hold(s,t,"shock",Spells.holdLength(t,20),RootRestriction.MOVEMENT);
                    Fear.apply(t,p,40);seed(s,t,1);
                }
                fx(s,id,p.position().add(p.getLookAngle().scale(3)),25);
            }
            case "desire_incarnation" -> {
                if(Form.isIn(p,LIQUID)){Form.end(p);return AbilityWheel.Result.FREE;}
                Form.of(LIQUID).lasts(120).cutsDamage(.8f).effect(MobEffects.SPEED,1).keepWhile(q->valid(s)&&own(q))
                        .every(5,q->{
                            if(!valid(s))return;
                            for(LivingEntity e:around(s,p.position(),32))if(e instanceof Mob mob&&mob.getTarget()==p)mob.setTarget(null);
                        }).everySecond(q->{
                            if(!valid(s))return;
                            for(LivingEntity t:around(s,p.position(),2.5)) {
                                magic(s,t,25);Spells.keepEffect(t,MobEffects.SLOWNESS,1,25);seed(s,t,1);
                            }
                        }).onEnd((q,why)->FilthyMonarchFx.end(s.level,p,p.position(),s.tag("form"))).take(p);
                FilthyMonarchFx.form(s.level,p,"desire_liquid",120,s.tag("form"),1);
                FilthyMonarchFx.sound(s.level,p.position(),id);
            }
            default -> {return AbilityWheel.Result.REFUSED;}
        }
        return AbilityWheel.Result.DONE;
    }
}
