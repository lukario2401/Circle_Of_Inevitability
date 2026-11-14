package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.filthy_monarch;

import java.util.*;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.filthy_monarch.FilthyMonarch.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.particles.fx.*;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Devil Arts and their owned projectile/field/channel lifecycles. */
final class FilthyMonarchDevil {
    private FilthyMonarchDevil() {}
    static final class Ball {
        Vec3 at,direction;
        final Set<UUID> seeded;
        final String tag;
        final float damage;
        final boolean homing;
        double traveled;
        Ball(Vec3 at,Vec3 direction,float damage,boolean homing,Set<UUID> seeded,String tag) {
            this.at=at;this.direction=direction.normalize();this.damage=damage;this.homing=homing;this.seeded=seeded;this.tag=tag;
        }
    }
    static final class Field {
        final String id,tag;
        final Vec3 at;
        final double radius;
        final long until;
        final LivingEntity target;
        final Map<UUID,Integer> inside=new HashMap<>();
        long next;
        Field(State s,String id,Vec3 at,double radius,int ticks,LivingEntity target) {
            this.id=id;this.tag=s.tag(id);this.at=at;this.radius=radius;this.until=s.now()+ticks;this.next=s.now()+20;this.target=target;
        }
    }
    static AbilityWheel.Result cast(State s,String id) {
        ServerPlayer p=s.player;
        switch(id) {
            case "sulfur_fireball" -> {
                boolean form=Form.isIn(p,DEVIL);
                volley(s,p.getEyePosition(),p.getLookAngle(),form?(s.sequence<=4?20:12):3,form?(s.sequence<=4?14:16):28);
                FilthyMonarchFx.sound(s.level,p.position(),id);
            }
            case "devil_form" -> {
                if(Form.isIn(p,DEVIL)){Form.end(p);return AbilityWheel.Result.FREE;}
                boolean demon=s.sequence<=4;String model=demon?"demon_form":"devil_form";
                Form.of(DEVIL).lasts(1200).attribute(Attributes.SCALE,demon?.9:.65,AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                        .attribute(Attributes.ENTITY_INTERACTION_RANGE,1.5,AttributeModifier.Operation.ADD_VALUE)
                        .attribute(Attributes.KNOCKBACK_RESISTANCE,.5,AttributeModifier.Operation.ADD_VALUE)
                        .effect(MobEffects.STRENGTH,demon?2:1).effect(MobEffects.RESISTANCE,0).effect(MobEffects.SPEED,0)
                        .keepWhile(q->valid(s)&&own(q)).every(200,q->{
                            if(valid(s))FilthyMonarchFx.form(s.level,p,model,Math.min(360,Form.ticksLeft(p)),s.tag("form"),demon?1.95f:1.7f);
                        }).onEnd((q,why)->{
                            FilthyMonarchFx.end(s.level,p,p.position(),s.tag("form"));
                            if(why==Form.Ending.TIME&&p.isAlive()) {
                                Spells.keepEffect(p,MobEffects.WEAKNESS,0,100);Spells.keepEffect(p,MobEffects.SLOWNESS,0,100);
                            }
                        }).take(p);
                FilthyMonarchFx.form(s.level,p,model,360,s.tag("form"),demon?1.95f:1.7f);
                FilthyMonarchFx.sound(s.level,p.position(),demon?"demon_form":id);
            }
            case "sword_of_lava" -> {
                for(LivingEntity t:cone(s,5,70))fire(s,t,85,80,4,false,true);
                fx(s,id,p.position().add(p.getLookAngle().scale(3)),24);
            }
            case "flame_cage" -> {
                LivingEntity target=aim(s,18);
                Vec3 at=target==null?p.position().add(p.getLookAngle().multiply(1,0,1).normalize().scale(3)):target.position();
                int ticks=target==null?80:Spells.isBoss(target)?30:60;
                close(s,"cage");Field f=new Field(s,"cage",at,1.5,ticks,target);s.fields.put(f.id,f);
                if(target!=null)hold(s,target,"cage",ticks,RootRestriction.MOVEMENT);
                for(int i=0;i<(target==null?1:4);i++) {
                    double a=i*Math.PI/2;
                    Vec3 wall=target==null?at:at.add(Math.sin(a)*1.5,0,Math.cos(a)*1.5);
                    Wall.at(wall,target==null?p.getYRot():i*90,3,3).lasts(ticks).allowsLivingThrough().stopsProjectiles()
                            .outsiders(t->enemy(s,t)).raise(p,s.level,s.tag("cage_wall_"+i));
                }
                ParticleShapes.model(s.level,FilthyMonarchFx.model("flame_cage",ticks,f.tag).scale(2),at,0,0,0);
                FilthyMonarchFx.sound(s.level,at,id);
            }
            case "volcanic_eruption" -> {
                Vec3 at=Spells.aimGround(p,s.level,22);
                String tag=s.tag("eruption_warning_"+UUID.randomUUID());
                ParticleShapes.model(s.level,FilthyMonarchFx.model("warning_ring",20,tag).scale(7),at,0,0,0);
                FilthyMonarchFx.sound(s.level,at,"eruption_warning");
                later(s,20,()->{
                    if(Silence.isSilenced(p))return;
                    for(LivingEntity t:around(s,at,3.5)) {
                        double fraction=Math.clamp(Spells.distanceToBody(t,at)/3.5,0,1);
                        fire(s,t,(float)(120-40*fraction),100,4,false,false);
                        t.setDeltaMovement(t.getDeltaMovement().multiply(1,0,1).add(0,1.3,0));t.hurtMarked=true;
                    }
                    close(s,"eruption");Field f=new Field(s,"eruption",at,3.5,80,null);s.fields.put(f.id,f);
                    ParticleShapes.model(s.level,FilthyMonarchFx.model("eruption_pillar",30,f.tag).scale(3),at,0,0,0);
                    FilthyMonarchFx.sound(s.level,at,id);
                });
            }
            case "toxic_vapor" -> {
                Vec3 at=Spells.aimGround(p,s.level,20);close(s,"fog");
                Field f=new Field(s,"fog",at,s.sequence<=4?4.5:3,200,null);s.fields.put(f.id,f);
                ParticleShapes.model(s.level,FilthyMonarchFx.model(s.sequence<=4?"corrosive_cloud":"toxic_cloud",200,f.tag)
                        .scale((float)(f.radius*1.5),1.8f,(float)(f.radius*1.5)),at,0,0,0);
                FilthyMonarchFx.sound(s.level,at,s.sequence<=4?"corrosive_fog":id);
                fogEntry(s,f);
            }
            case "hellfire" -> {
                List<LivingEntity> targets=cone(s,16,180).stream().limit(8).toList();
                if(targets.isEmpty())return refuse(p,"No visible enemies within 16 blocks.");
                for(LivingEntity target:targets) {
                    Vec3 at=target.position();String tag=s.tag("hellfire_"+target.getUUID()+"_"+UUID.randomUUID());
                    ParticleShapes.model(s.level,FilthyMonarchFx.model("warning_ring",20,tag).scale(2),at,0,0,0);
                    later(s,20,()->{
                        if(Silence.isSilenced(p)||!enemy(s,target)||Spells.distanceToBody(target,at)>1.5)return;
                        fire(s,target,110,100,6,true,false);seed(s,target,1);
                        ParticleShapes.model(s.level,FilthyMonarchFx.model("hellfire_pillar",30,tag).scale(2),at,0,0,0);
                        FilthyMonarchFx.sound(s.level,at,id);
                    });
                }
                FilthyMonarchFx.sound(s.level,p.position(),"eruption_warning");
            }
            case "flames_of_the_abyss" -> {
                s.channelUntil=s.now()+(s.sequence==0?100:60);s.channelNext=s.now();s.channelDirection=p.getLookAngle();
                Root.applyOwned(p,s.tag("channel"),(int)(s.channelUntil-s.now()),Set.of(RootRestriction.MOVE,RootRestriction.JUMP));
                FilthyMonarchFx.sound(s.level,p.position(),id);
            }
            default -> {return AbilityWheel.Result.REFUSED;}
        }
        return AbilityWheel.Result.DONE;
    }
    static void volley(State s,Vec3 from,Vec3 direction,int count,float damage) {
        Set<UUID> seeded=new HashSet<>();
        for(int i=0;i<count;i++) {
            while(s.balls.size()>=120)finish(s,s.balls.removeFirst());
            Vec3 dir=Spells.turned(direction,count==1?0:(i-(count-1)/2.0)*(count==3?12:2));
            String tag=s.tag("ball_"+UUID.randomUUID());Ball b=new Ball(from,dir,damage,s.sequence<=4,seeded,tag);s.balls.add(b);
            ParticleShapes.modelAlong(s.level,FilthyMonarchFx.model("sulfur_fireball",60,tag).scale(.6f),from,dir);
        }
    }
    static void tick(State s) {
        long now=s.now();
        for(Ball b:List.copyOf(s.balls)) {
            if(b.homing) {
                LivingEntity target=around(s,b.at,6).stream().filter(t->t.getEyePosition().subtract(b.at).normalize().dot(b.direction)>.5)
                        .filter(t->Spells.clearLine(s.player,s.level,b.at,t.getEyePosition()))
                        .min(Comparator.comparingDouble(t->t.distanceToSqr(b.at))).orElse(null);
                if(target!=null)b.direction=b.direction.scale(.92).add(target.getEyePosition().subtract(b.at).normalize().scale(.08)).normalize();
            }
            Vec3 next=b.at.add(b.direction.scale(1.5));
            var wall=s.level.clip(new ClipContext(b.at,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,s.player));
            boolean hit=wall.getType()!=HitResult.Type.MISS;Vec3 stop=hit?wall.getLocation():next;
            double nearest=b.at.distanceToSqr(stop);LivingEntity victim=null;
            for(LivingEntity t:Spells.enemiesOnLine(s.player,s.level,b.at,stop,.3)) {
                if(!enemy(s,t))continue;
                AABB box=t.getBoundingBox().inflate(.3);Vec3 contact=box.contains(b.at)?b.at:box.clip(b.at,stop).orElse(null);
                if(contact!=null&&contact.distanceToSqr(b.at)<=nearest){nearest=contact.distanceToSqr(b.at);stop=contact;victim=t;}
            }
            if(victim!=null)hit=true;
            b.traveled+=b.at.distanceTo(stop);b.at=stop;
            if(hit||b.traveled>=40) {
                Vec3 impact=b.at.subtract(b.direction.scale(.05));
                for(LivingEntity t:around(s,impact,3))if(Spells.clearLine(s.player,s.level,impact,t.getBoundingBox().getCenter())||t==victim) {
                    fire(s,t,b.damage,60,4,false,false);if(b.seeded.add(t.getUUID()))seed(s,t,1);
                }
                ParticleShapes.burst(s.level,FilthyMonarchFx.particle("sulfur_flame",FilthyMonarchFx.BLUE),impact,16,.04,.2);
                FilthyMonarchFx.sound(s.level,impact,"fireball_impact");finish(s,b);s.balls.remove(b);
            } else ParticleShapes.moveModelAlong(s.level,b.tag,b.at,b.direction);
        }
        for(Field f:List.copyOf(s.fields.values())) {
            if(now>f.until||f.target!=null&&!enemy(s,f.target)){close(s,f.id);continue;}
            if(f.id.equals("fog"))fogEntry(s,f);
            if(now<f.next)continue;f.next=now+20;
            if(f.id.equals("cage")) {if(f.target!=null)fire(s,f.target,10,20,0,false,false);continue;}
            for(LivingEntity t:around(s,f.at,f.radius)) {
                if(t.getY()>f.at.y+4||t.getY()+t.getBbHeight()<f.at.y-1)continue;
                if(f.id.equals("eruption")){if(!fireProtected(t))magic(s,t,8);}
                else {
                    magic(s,t,8);Spells.keepEffect(t,MobEffects.POISON,1,25);
                    if(s.sequence<=4&&f.inside.getOrDefault(t.getUUID(),0)>=60) {
                        Corrosion.apply(t,s.player,2,1,40);
                        for(EquipmentSlot slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)) {
                            ItemStack item=t.getItemBySlot(slot);if(!item.isEmpty())item.hurtAndBreak(1,t,slot);
                        }
                    }
                }
            }
        }
        if(s.channelUntil>0) {
            if(now>=s.channelUntil||Silence.isSilenced(s.player)||Root.has(s.player,RootRestriction.ATTACK)) {
                s.channelUntil=0;Root.removeOwned(s.player,s.tag("channel"));
                FilthyMonarchFx.end(s.level,s.player,s.player.position(),s.tag("channel_fx"));
            } else if(now>=s.channelNext) {
                s.channelNext=now+20;
                Vec3 from=s.player.getEyePosition(),to=from.add(s.channelDirection.scale(s.sequence==0?50:30));
                FilthyMonarchFx.beam(s.level,from,to,false,20,s.tag("channel_fx"));
                for(LivingEntity t:Spells.enemiesOnLine(s.player,s.level,from,to,s.sequence==0?5:2.5))if(enemy(s,t)) {
                    magic(s,t,fireProtected(t)?60:90);burn(s,t,40,4,false);
                    Spells.keepEffect(t,MobEffects.POISON,1,40);Corrosion.apply(t,s.player,3,1,60);seed(s,t,1);
                }
            }
        }
    }
    private static void fogEntry(State s,Field f) {
        Set<UUID> present=new HashSet<>();
        for(LivingEntity t:around(s,f.at,f.radius)) {
            if(t.getY()>f.at.y+4||t.getY()+t.getBbHeight()<f.at.y-1)continue;
            present.add(t.getUUID());int stayed=f.inside.merge(t.getUUID(),1,Integer::sum);
            if(stayed==1)Blind.apply(t,20);
            Spells.keepEffect(t,MobEffects.POISON,1,25);
        }
        f.inside.keySet().retainAll(present);
    }
    static void close(State s,String id) {
        Field f=s.fields.remove(id);if(f==null)return;
        if(id.equals("cage")) {
            for(int i=0;i<4;i++)Wall.close(s.player,s.tag("cage_wall_"+i));
            if(f.target!=null)Root.removeOwned(f.target,s.tag("cage"));
        }
        FilthyMonarchFx.end(s.level,s.player,f.at,f.tag);
    }
    private static void finish(State s,Ball ball) {FilthyMonarchFx.end(s.level,s.player,ball.at,ball.tag);}
    static void clear(State s) {
        for(String name:List.copyOf(s.fields.keySet()))close(s,name);
        for(Ball ball:s.balls)finish(s,ball);s.balls.clear();
        FilthyMonarchFx.end(s.level,s.player,s.player.position(),s.tag("channel_fx"));
    }
}
