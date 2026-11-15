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

/** Desire and Malice: shadows, aggression, conditional life links and rebirth. */
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
            case "desire_symbiosis" -> {
                LivingEntity t=aim(s,24);if(t==null)return refuse(p,"Look at a living enemy within 24 blocks.");
                clearLink(s);s.symbiosis=t;s.symbiosisUntil=s.now()+240;
                s.linkToken=DamageLink.redirectWhen(p,t,.25f,240,()->valid(s)&&enemy(s,t)&&p.distanceToSqr(t)<=1600);
                armWard(s);fx(s,id,t.position(),80);
            }
            case "seed_of_malice" -> {
                LivingEntity t=aim(s,32);if(t==null)return refuse(p,"Look at a living enemy within 32 blocks.");
                Spells.strike(p,t,40);seed(s,t,2);Fear.apply(t,p,60);
                if(enemy(s,t)){s.malice=t;s.maliceUntil=s.now()+1800;armWard(s);}
                fx(s,id,t.position(),80);
            }
            case "filthy_illusion" -> {
                for(Mob phantom:s.phantoms.values())phantom.discard();s.phantoms.clear();
                for(int i=0;i<3;i++) {
                    Vec3 at=p.position().add(Spells.turned(p.getLookAngle(),(i-1)*40).scale(1.5));
                    Mob phantom=Construct.of(EntityTypes.HUSK).health(30).damage(0).lasts(120).leash(30)
                            .limit("filthy_phantoms",3).build(p,s.level,at,p.getYRot());
                    if(phantom==null)continue;
                    phantom.setNoGravity(true);phantom.setNoAi(true);
                    phantom.getPersistentData().putBoolean("circle_of_inevitability_beyonder_rolled",true);
                    s.phantoms.put(phantom.getUUID(),phantom);
                    FilthyMonarchFx.form(s.level,phantom,"filth_phantom",120,s.tag("phantom"),1);
                }
                for(var t:around(s,p.position(),24))if(t instanceof Mob mob && mob.getTarget()==p && !s.phantoms.isEmpty()) {
                    Mob decoy=s.phantoms.values().stream().min(Comparator.comparingDouble(t::distanceToSqr)).orElseThrow();
                    if(Taunt.apply(t,decoy,120)){track(s,t,120);s.taunts.put(t.getUUID(),Taunt.snapshot(t));}
                }
                FilthyMonarchFx.sound(s.level,p.position(),id);
            }
            default -> {return AbilityWheel.Result.REFUSED;}
        }
        return AbilityWheel.Result.DONE;
    }

    static boolean linked(State s) {return s.symbiosisUntil>s.now()&&enemy(s,s.symbiosis)&&s.player.distanceToSqr(s.symbiosis)<=1600;}
    static boolean anchored(State s) {return s.maliceUntil>s.now()&&enemy(s,s.malice)&&rebirthSpot(s,s.malice)!=null;}
    static Vec3 rebirthSpot(State s,LivingEntity target) {
        if(!enemy(s,target))return null;
        for(int i=0;i<8;i++) {
            Vec3 at=target.position().add(Math.cos(i*Math.PI/4)*1.5,0,Math.sin(i*Math.PI/4)*1.5);
            if(s.level.getWorldBorder().isWithinBounds(BlockPos.containing(at))&&Spells.fits(s.player,s.level,at))return at;
        }
        return null;
    }
    /** One conditional ward resolves the explicit links first, then the passive. */
    static void armWard(State s) {
        Object current=DeathWard.snapshot(s.player);
        if(current!=null && current!=s.wardToken)return;
        DeathWard.disarmIf(s.player,s.wardToken);
        s.wardToken=DeathWard.armWhen(s.player,1801,.01f,0,
                ()->current(s)&&(linked(s)||anchored(s)||FilthyMonarchPassives.dropReady(s)),
                (saved,source,level)->{
                    s.wardToken=null;
                    if(linked(s)) {
                        saved.setHealth(1);clearLink(s);Invulnerable.grant(saved,30);
                        FilthyMonarchFx.sound(level,saved.position(),"link_break");
                    } else if(anchored(s)) {
                        LivingEntity victim=s.malice;Vec3 at=rebirthSpot(s,victim);s.malice=null;s.maliceUntil=0;
                        if(at!=null)Teleport.of(saved).fallGuard(80).go(level,at);
                        saved.setHealth(saved.getMaxHealth()*.4f);Invulnerable.grant(saved,40);
                        float damage=Spells.isBoss(victim)?victim.getMaxHealth()*.1f:victim.getHealth()*.5f;
                        DamageLimit.run(victim,damage,()->magic(s,victim,damage));
                        FilthyMonarchFx.sound(level,saved.position(),"malice_rebirth");
                    } else FilthyMonarchPassives.singleDrop(s);
                });
    }
    static void tick(State s) {
        if(s.symbiosis!=null&&!linked(s))clearLink(s);
        if(s.malice!=null && (s.maliceUntil<=s.now()||!enemy(s,s.malice))){s.malice=null;s.maliceUntil=0;}
        Object current=DeathWard.snapshot(s.player);
        if(s.wardToken!=null && current!=s.wardToken)s.wardToken=null;
        if(s.wardToken==null&&(linked(s)||anchored(s)||FilthyMonarchPassives.dropReady(s)))armWard(s);
        for(Mob phantom:List.copyOf(s.phantoms.values())) {
            if(!phantom.isAlive()||phantom.isRemoved()){s.phantoms.remove(phantom.getUUID());continue;}
            var target=around(s,phantom.position(),24).stream().min(Comparator.comparingDouble(phantom::distanceToSqr)).orElse(null);
            if(target==null)continue;
            Vec3 delta=target.position().subtract(phantom.position());
            if(delta.lengthSqr()<2.25) {
                for(var t:around(s,phantom.position(),3)){magic(s,t,50);seed(s,t,1);}
                FilthyMonarchFx.sound(s.level,phantom.position(),"illusion_burst");phantom.discard();s.phantoms.remove(phantom.getUUID());
            } else {
                Vec3 next=phantom.position().add(delta.normalize().scale(.45));
                if(Spells.fits(phantom,s.level,next))phantom.teleportTo(next.x,next.y,next.z);
            }
        }
    }
    static void clearLink(State s) {
        DamageLink.clearRedirectIf(s.player,s.linkToken);s.linkToken=null;s.symbiosis=null;s.symbiosisUntil=0;
    }
    static void clear(State s) {
        clearLink(s);DeathWard.disarmIf(s.player,s.wardToken);s.wardToken=null;s.malice=null;
        for(Mob phantom:s.phantoms.values())phantom.discard();s.phantoms.clear();
        Construct.endAll(s.player,"filthy_phantoms");
    }
}
