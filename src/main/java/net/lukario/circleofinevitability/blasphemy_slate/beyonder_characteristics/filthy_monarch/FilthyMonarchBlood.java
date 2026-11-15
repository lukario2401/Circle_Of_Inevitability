package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.filthy_monarch;

import java.util.*;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.filthy_monarch.FilthyMonarch.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.particles.fx.*;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.phys.Vec3;

/** Blood, persistent filth and the two Abyss capstones. No block placement or destruction. */
final class FilthyMonarchBlood {
    static AbilityWheel.Result cast(State s,String id) {
        var p=s.player;
        switch(id) {
            case "acid_swamp" -> {
                Vec3 at=Spells.aimGround(p,s.level,24);field(s,"acid",at,4,240,14,true);FilthyMonarchFx.sound(s.level,at,id);
            }
            case "blood_ignition" -> {
                LivingEntity t=aim(s,32);if(t==null)return refuse(p,"Look at an enemy within 32 blocks.");
                int n=Depravity.spend(p,t);FilthyMonarchTongue.afflict(s,t,"ignition",120,25+10*n);
                NoHeal.apply(t,25);fx(s,id,t.position(),120);
            }
            case "blood_burst" -> {
                if(!Spells.payBloodFraction(p,.2f,6))return refuse(p,"Blood Burst requires at least 6 health.");
                for(var t:around(s,p.position(),4.5)) {
                    magic(s,t,120);Spells.push(t,p.position(),1.8,.3);Corrosion.apply(t,p,3,1,120);Spells.keepEffect(t,MobEffects.POISON,1,120);
                }
                Spells.keepEffect(p,MobEffects.REGENERATION,1,100);fx(s,id,p.position(),35);
            }
            case "tide_of_filth" -> {
                Vec3 from=p.position(),dir=p.getLookAngle().multiply(1,0,1).normalize();
                if(dir.lengthSqr()<.1)return refuse(p,"Look toward the ground ahead of you.");
                Set<UUID> struck=new HashSet<>();String wave=s.tag("tide_wave");
                // A new tide replaces the previous one's travel and pool.
                String identity=UUID.randomUUID().toString();p.getPersistentData().putString(s.tag("tide_id"),identity);
                for(int i=1;i<=8;i++) {
                    final int step=i;
                    later(s,(i-1)*3,()->{
                        if(!identity.equals(p.getPersistentData().getStringOr(s.tag("tide_id"),"")))return;
                        Vec3 at=from.add(dir.scale(step*3));
                        ParticleShapes.modelAlong(s.level,FilthyMonarchFx.model("sludge_wave",5,wave).scale(10,2,2),at,dir);
                        for(var t:Spells.enemiesOnLine(p,s.level,at.subtract(dir.scale(3)),at,10))if(enemy(s,t)&&struck.add(t.getUUID())) {
                            magic(s,t,150);Spells.push(t,from,1.5,.3);Corrosion.apply(t,p,4,1,160);
                        }
                        if(step==8)tidePool(s,from,dir);
                    });
                }
                FilthyMonarchFx.sound(s.level,p.position(),id);
            }
            case "the_corroder" -> {
                LivingEntity t=Spells.aimLiving(p,s.level,40,false,e->enemy(s,e)&&Spells.canBeChosen(p,e));
                if(t==null)return refuse(p,"Look toward an enemy within 40 blocks.");
                s.corroder=t;s.erosion.clear();
                s.channel=new Channel(p,81,20,20,true,()->valid(s)&&enemy(s,t)&&p.distanceToSqr(t)<=1600&&p.hasLineOfSight(t),index->{
                    magic(s,t,30*(1<<index));Corrosion.apply(t,p,3,1,60);
                },()->{s.corroder=null;s.erosion.clear();FilthyMonarchFx.end(s.level,p,p.position(),s.tag("corroder"));});
                FilthyMonarchFx.sound(s.level,p.position(),id);
            }
            case "abyssal_sea" -> {
                if(Dome.isRaised(p,SEA)){closeSea(s);return AbilityWheel.Result.FREE;}
                Vec3 at=p.position();
                Dome.sphere(at.add(0,1,0),16).lasts(1200).keepsEnemiesIn().holdsOwner().sealsProjectiles()
                        .outsiders(t->true).keepWhile(()->valid(s)).onEnd(()->{
                            Zone.close(p,SEA);FilthyMonarchFx.end(s.level,p,at,s.tag("sea"));
                        }).raise(p,s.level,SEA);
                Map<UUID,Long> feared=new HashMap<>();s.zones.add(SEA);
                Zone.at(at,16).height(32).lasts(1200).every(20).affects(t->valid(s)&&enemy(s,t)&&Dome.isInside(p,SEA,t))
                        .onInside((t,ticks)->{
                            seed(s,t,1);int n=Depravity.count(p,t);magic(s,t,15*n);
                            if(n==5&&feared.getOrDefault(t.getUUID(),0L)<=s.now()){Fear.apply(t,p,60);feared.put(t.getUUID(),s.now()+200);}
                        }).onLeave(t->feared.remove(t.getUUID())).open(p,s.level,SEA);
                FilthyMonarchFx.kingdom(s.level,at,16,1200,s.tag("sea"));FilthyMonarchFx.sound(s.level,at,id);
            }
            case "the_abyss" -> {
                if(Form.isIn(p,ABYSS)){Form.end(p);return AbilityWheel.Result.FREE;}
                Form.of(ABYSS).lasts(600).cutsDamage(.4f).keepWhile(q->valid(s)&&own(q))
                        .every(120,q->{if(valid(s))for(var t:around(s,p.position(),12))Fear.apply(t,p,40);})
                        .every(200,q->{if(valid(s))FilthyMonarchFx.form(s.level,p,"abyss_form",Math.min(360,Form.ticksLeft(p)),s.tag("form"),1.5f);})
                        .onEnd((q,why)->FilthyMonarchFx.end(s.level,p,p.position(),s.tag("form"))).take(p);
                FilthyMonarchFx.form(s.level,p,"abyss_form",360,s.tag("form"),1.5f);FilthyMonarchFx.sound(s.level,p.position(),id);
            }
            default -> {return AbilityWheel.Result.REFUSED;}
        }
        return AbilityWheel.Result.DONE;
    }
    static void field(State s,String kind,Vec3 at,double radius,int duration,float damage,boolean acid) {
        String name=s.tag(kind);s.zones.add(name);
        Zone.at(at,radius).lasts(duration).every(20).affects(t->valid(s)&&enemy(s,t))
                .onInside((t,stay)->{
                    magic(s,t,damage);
                    if(acid) {
                        Spells.keepEffect(t,MobEffects.SLOWNESS,1,25);
                        Spells.wearArmor(t,3);
                        if(stay>=60)Corrosion.apply(t,s.player,2+stay/60f,1,40);
                    }
                }).onEnd(()->FilthyMonarchFx.end(s.level,s.player,at,name)).open(s.player,s.level,name);
        ParticleShapes.model(s.level,FilthyMonarchFx.model(acid?"acid_pool":"corrupted_fog",duration,name).scale((float)radius),at,0,0,0);
    }
    private static void tidePool(State s,Vec3 from,Vec3 direction) {
        var p=s.player;String name=s.tag("sludge");s.zones.add(name);Vec3 center=from.add(direction.scale(12));
        java.util.function.Predicate<LivingEntity> inside=t->{
            Vec3 d=t.position().subtract(from);double along=d.dot(direction);
            return valid(s)&&enemy(s,t)&&along>=0&&along<=24&&d.subtract(direction.scale(along)).horizontalDistanceSqr()<=100;
        };
        Zone.at(center,17).lasts(160).every(20).affects(inside).onInside((t,stay)->{
            magic(s,t,12);Spells.keepEffect(t,MobEffects.SLOWNESS,1,25);
        }).onEnd(()->FilthyMonarchFx.end(s.level,p,center,name)).open(p,s.level,name);
        ParticleShapes.modelAlong(s.level,FilthyMonarchFx.model("sludge_pool",160,name).scale(10,1,12),center,direction);
        around(s,center,17).stream().filter(inside).sorted(Comparator.comparingDouble(p::distanceToSqr)).limit(6).forEach(t->{
            hold(s,t,"filth",40,RootRestriction.MOVEMENT);
            FilthyMonarchFx.form(s.level,t,"filth_hand",40,s.tag("hand_"+t.getUUID()),1);
        });
    }
    static void tick(State s) {
        if(s.corroder!=null&&s.channel!=null&&s.channel.active()&&enemy(s,s.corroder)) {
            Vec3 from=s.player.getEyePosition(),to=s.corroder.getBoundingBox().getCenter();
            s.erosion.tick(s.level,from,to,t->enemy(s,t),40);
            if(s.now()%5==0)FilthyMonarchFx.beam(s.level,from,to,true,8,s.tag("corroder"));
        }
        s.curses.values().removeIf(until->until<=s.now());
        for(var entry:List.copyOf(s.afflictions.entrySet())) {
            Affliction a=entry.getValue();var target=a.target();
            if(!enemy(s,target)||s.now()>a.until()){s.afflictions.remove(entry.getKey());continue;}
            if(a.kind().equals("ignition"))NoHeal.apply(target,2);
            if(s.now()<a.next())continue;
            float amount=a.damage();
            hurtCapped(s,target,amount,a.kind().equals("ignition"));
            if(a.kind().equals("ignition"))for(var t:around(s,target.position(),3))if(t!=target)hurtCapped(s,t,amount*.5f,true);
            if(s.afflictions.get(entry.getKey())==a)s.afflictions.put(entry.getKey(),new Affliction(target,a.until(),s.now()+20,a.damage(),a.kind()));
        }
    }
    private static void hurtCapped(State s,LivingEntity t,float damage,boolean capBoss) {
        if(capBoss&&Spells.isBoss(t))DamageLimit.run(t,t.getMaxHealth()*.03f,()->magic(s,t,damage));else magic(s,t,damage);
    }
    static void closeSea(State s) {Dome.close(s.player,SEA);Zone.close(s.player,SEA);FilthyMonarchFx.end(s.level,s.player,s.player.position(),s.tag("sea"));}
    static void clear(State s) {
        closeSea(s);for(String zone:s.zones)Zone.close(s.player,zone);s.zones.clear();
        s.afflictions.clear();s.curses.clear();s.erosion.clear();
        s.player.getPersistentData().remove(s.tag("tide_id"));
    }
}
