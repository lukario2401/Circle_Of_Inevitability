package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel.Result;
import net.lukario.circleofinevitability.particles.fx.*;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.screen.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.*;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god.ThunderGod.*;

/** Cataclysmic Interrer through Tyrant. Bounded local fields; no block or global-weather mutations. */
public final class ThunderGodHigher {
    private static final List<Field> FIELDS=new ArrayList<>();
    private static final Map<UUID,Light> LIGHT=new HashMap<>();
    private static final Map<UUID,Arrival> ARRIVALS=new HashMap<>();
    private static final List<Ward> WARDS=new ArrayList<>();
    private static final List<Rejection> REJECTIONS=new ArrayList<>();
    private static final List<Banish> BANISHED=new ArrayList<>();
    private static final String SPENT=PREFIX+"spent";
    private ThunderGodHigher(){}
    private static String id(Spell s){return PREFIX+s.id();}
    private static ParticleFx spark(){return ParticleFx.of("fx/thunder_god/spark").color(ThunderGodFx.SILVER).size(.08f).lifetime(8,0).glow();}
    private static boolean here(Player p,ServerLevel sl,long generation){return ThunderGodPowers.live(p,sl,generation);}
    public static boolean source(LivingEntity e){return e instanceof Player p&&Form.isIn(p,SOURCE)&&Form.ticksLeft(p)>0;}
    public static boolean calmRefused(Player p,ServerLevel sl,Spell spell){
        return source(p)&&(spell==Spell.TIDEHEAL||spell==Spell.SIREN_SONG||spell==Spell.REQUIEM
                ||spell==Spell.CLOUD_SWORD&&Spells.aimAlly(p,sl,40)!=null);
    }
    public static boolean dismissable(Player p,Spell spell){
        return spell==Spell.INCARNATION_OF_LIGHT&&LIGHT.containsKey(p.getUUID())
                ||(spell==Spell.STORM_HELL||spell==Spell.CHASM_OF_STORMS||spell==Spell.DREADFUL_HEAVEN)&&field(p,spell)!=null;
    }
    public static boolean inChasm(Player p){Field f=field(p,Spell.CHASM_OF_STORMS);return f!=null&&f.covers(p.position());}
    public static boolean inDreadful(Player p){Field f=field(p,Spell.DREADFUL_HEAVEN);return f!=null&&f.covers(p.position());}
    private static Field field(Player p,Spell spell){return FIELDS.stream().filter(f->f.owner.getUUID().equals(p.getUUID())&&f.spell==spell&&!f.closed&&!f.auxiliary).findFirst().orElse(null);}
    public static boolean hasField(Player p,Spell spell){return field(p,spell)!=null;}
    public static boolean invulnerable(Entity e){return LIGHT.containsKey(e.getUUID())||ARRIVALS.containsKey(e.getUUID());}
    public static boolean presence(LivingEntity owner){return owner instanceof Player p&&field(p,Spell.TYRANTS_PRESENCE)!=null;}
    public static boolean possessionBlocked(LivingEntity host){return FIELDS.stream().anyMatch(f->f.spell==Spell.TYRANTS_PRESENCE&&f.level==host.level()&&f.covers(host.getBoundingBox().getCenter()));}
    public static boolean suppressedSubstitute(Entity e){return FIELDS.stream().anyMatch(f->f.spell==Spell.MAELSTROM&&f.level==e.level()&&f.captured.contains(e.getUUID())&&!f.closed);}
    public static boolean held(Entity e,RootRestriction restriction){return restriction==RootRestriction.TELEPORT&&(suppressedSubstitute(e)||FIELDS.stream().anyMatch(f->f.kingdom()&&f.level==e.level()&&f.covers(e.position())));}
    public static boolean blockedInformation(Entity e){return e instanceof LivingEntity living&&FIELDS.stream().anyMatch(f->f.spell==Spell.ELECTROMAGNETIC_DISRUPTION&&f.level==e.level()&&f.covers(e.position())&&!Spells.isFriend(f.owner,living));}
    public static Vec3 nearestWeather(Player p){return FIELDS.stream().filter(f->f.wet&&f.level==p.level()).map(Field::center).min(Comparator.comparingDouble(p.position()::distanceToSqr)).orElse(null);}
    public static boolean wet(LivingEntity e){return FIELDS.stream().anyMatch(f->f.wet&&f.level==e.level()&&f.covers(e.position()));}
    public static boolean doubleCost(Player p){return FIELDS.stream().anyMatch(f->f.spell==Spell.DREADFUL_HEAVEN&&f.level==p.level()&&f.covers(p.position())&&!Spells.isFriend(f.owner,p));}
    public static float wardCut(LivingEntity target){return WARDS.stream().anyMatch(w->w.ally==target&&w.live()&&w.behind(target.position()))?.3f:0;}
    public static boolean wardBlocks(LivingEntity attacker,LivingEntity target){
        for(Ward ward:WARDS)if(ward.ally==target&&ward.live()){
            double a=attacker.getBoundingBox().getCenter().subtract(ward.wall()).dot(ward.normal),b=target.getBoundingBox().getCenter().subtract(ward.wall()).dot(ward.normal);
            if(a*b>=0)continue;
            Vec3 crossing=attacker.getBoundingBox().getCenter().lerp(target.getBoundingBox().getCenter(),a/(a-b));
            Vec3 side=new Vec3(ward.normal.z,0,-ward.normal.x),delta=crossing.subtract(ward.wall());
            if(Math.abs(delta.dot(side))<=1.8&&delta.y>=0&&delta.y<=4)return true;
        }return false;
    }
    public static boolean separated(LivingEntity a,LivingEntity b){
        for(Field f:FIELDS)if(f.kingdom()&&f.level==a.level()&&a.level()==b.level()&&f.covers(a.position())!=f.covers(b.position()))return true;return false;
    }
    public static boolean blockedAim(LivingEntity chooser,LivingEntity target){
        if(separated(chooser,target))return true;
        for(Field f:FIELDS){
            if(f.level!=chooser.level())continue;
            if(f.spell==Spell.HURRICANE&&target==f.owner&&Spells.isEnemy(f.owner,chooser)&&!f.covers(chooser.position()))return true;
            if(f.spell==Spell.STORM_HELL&&f.covers(chooser.position())&&Spells.isEnemy(f.owner,chooser)&&chooser.distanceToSqr(target)>36)return true;
        }
        return false;
    }
    public static void lightningLanded(ServerLevel sl,Vec3 at){
        for(ServerPlayer p:sl.players())if(is(p,1)&&p.position().distanceToSqr(at)<=64&&sl.getGameTime()>=p.getPersistentData().getLongOr(PREFIX+"authority_ready",0L)){
            p.getPersistentData().putLong(PREFIX+"authority_ready",sl.getGameTime()+20);Rage.add(p,1);
        }
    }
    public static Result cast(ServerPlayer p,ServerLevel sl,Spell spell){
        if(calmRefused(p,sl,spell))return Result.REFUSED;
        if(dismissable(p,spell)){
            if(spell==Spell.INCARNATION_OF_LIGHT)finishLight(p,true);else close(field(p,spell),false);
            return Result.FREE;
        }
        return switch(spell){
            case SOURCE_OF_WRATH->sourceWrath(p,sl);
            case SEA_MASTER->seaMaster(p,sl);
            case MAELSTROM->{open(p,sl,spell,Spells.aimGround(p,sl,24),5,24,160,5,false,true);yield Result.DONE;}
            case DESTRUCTIVE_DROPLET->droplet(p,sl);
            case WINDBLADE_LEGION->legion(p,sl);
            case SONIC_BOOM->sonic(p,sl);
            case CLOUD_SWORD->cloudSword(p,sl);
            case INCARNATION_OF_LIGHT->light(p,sl);
            case STARFALL->starfall(p,sl);
            case CHAIN_LIGHTNING->chain(p,sl);
            case LIGHTNING_STORM->{open(p,sl,spell,Spells.aimGround(p,sl,30),4.5,8,160,4,false,false);yield Result.DONE;}
            case MAGNETIC_POLE_PULL->{open(p,sl,spell,Spells.aimGround(p,sl,30),14,28,120,5,false,false);yield Result.DONE;}
            case ELECTROMAGNETIC_DISRUPTION->disruption(p,sl);
            case HEAVENLY_PUNISHMENT->punishment(p,sl);
            case OCEAN_OF_LIGHTNING->{open(p,sl,spell,Spells.aimGround(p,sl,50),28,8,400,5,false,false);yield Result.DONE;}
            case LIGHTNING_FOREST->{Field f=open(p,sl,spell,Spells.aimGround(p,sl,60),40,80,100,5,false,false);breakBarriers(f);yield Result.DONE;}
            case ROAR->roar(p,sl);
            case DEATH_DIRGE->{open(p,sl,spell,p.position(),16,18,240,10,true,false);yield Result.DONE;}
            case SOUND_OF_HORROR->horror(p,sl);
            case REQUIEM->requiem(p,sl);
            case ROAR_OF_THE_THUNDER_GOD->godRoar(p,sl);
            case TYRANTS_PRESENCE->{open(p,sl,spell,p.position(),24,28,600,5,true,false);yield Result.DONE;}
            case HURRICANE->{open(p,sl,spell,p.position(),3,8,200,5,true,true);yield Result.DONE;}
            case TSUNAMI->tsunami(p,sl);
            case WRATH_OF_THE_SEA->seaWrath(p,sl);
            case STORM_INCARNATION->{open(p,sl,spell,Spells.aimGround(p,sl,60),9,24,400,5,false,true);yield Result.DONE;}
            case STORM_HELL->{open(p,sl,spell,Spells.aimGround(p,sl,80),40,50,900,5,false,true);yield Result.DONE;}
            case CHASM_OF_STORMS,DREADFUL_HEAVEN->kingdom(p,sl,spell);
            case BANISH_INTO_THE_STORM->banish(p,sl);
            default->Result.REFUSED;
        };
    }
    private static Result sourceWrath(ServerPlayer p,ServerLevel sl){
        if(source(p)){Form.end(p);return Result.FREE;}
        if(!is(p,2)||Rage.count(p)<10)return Result.REFUSED;
        Rage.spend(p);
        Form.of(SOURCE).lasts(400).keepWhile(q->is(q,2)).onEnd((q,why)->{
            SequenceBoost.end(q);Rage.spend(q);ScreenImages.hide(q,id(Spell.SOURCE_OF_WRATH));
            if(q.isAlive()&&(why==Form.Ending.TIME||why==Form.Ending.ENDED||why==Form.Ending.REPLACED)){
                Marks.add(q,SPENT,1,200);q.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,200,1));q.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,200,0));
            }
        }).take(p);
        SequenceBoost.apply(p,1,400);
        ScreenImages.show(p,id(Spell.SOURCE_OF_WRATH),"thunder_god/source_of_wrath",ScreenAnchor.BOTTOM,132,-3,16,400);
        return Result.DONE;
    }
    private static final class Field {
        final ServerPlayer owner;final ServerLevel level;final Spell spell;final long generation,start;final int ticks,beat;
        final double radius,height;final boolean follows,wet;final String tag;final Rage.Cast damage;final Set<UUID> captured=new HashSet<>();
        final List<Mob> summons=new ArrayList<>();Vec3 point;Entity marker;boolean closed,auxiliary;String zoneKey;
        Vec3 pathStart,pathEnd;
        Field(ServerPlayer p,ServerLevel sl,Spell spell,Vec3 at,double radius,double height,int ticks,int beat,boolean follows,boolean wet){
            owner=p;level=sl;this.spell=spell;point=at;this.radius=radius;this.height=height;this.ticks=ticks;this.beat=beat;this.follows=follows;this.wet=wet;
            zoneKey=id(spell);tag=id(spell)+"_"+p.getUUID()+"_"+UUID.randomUUID();start=sl.getGameTime();generation=ThunderGodPowers.generation(p);damage=Rage.cast(p);
        }
        Vec3 center(){return follows?owner.position():marker!=null?marker.position():point;}
        boolean kingdom(){return spell==Spell.CHASM_OF_STORMS||spell==Spell.DREADFUL_HEAVEN;}
        boolean covers(Vec3 at){
            if(closed)return false;Vec3 center=center();
            if(kingdom())return center.distanceToSqr(at)<=radius*radius;
            if(pathStart!=null)return distanceToSegment(at,pathStart,pathEnd)<=24&&at.y>=pathStart.y-1&&at.y<=pathStart.y+3;
            double x=at.x-center.x,z=at.z-center.z;
            return x*x+z*z<=radius*radius&&at.y>=center.y-1&&at.y<=center.y+height;
        }
        long age(){return level.getGameTime()-start;}
    }
    private static Field open(ServerPlayer p,ServerLevel sl,Spell spell,Vec3 at,double radius,double height,int ticks,int beat,boolean follows,boolean wet){
        return open(p,sl,spell,at,radius,height,ticks,beat,follows,wet,f->{});
    }
    private static Field open(ServerPlayer p,ServerLevel sl,Spell spell,Vec3 at,double radius,double height,int ticks,int beat,boolean follows,boolean wet,java.util.function.Consumer<Field> setup){
        Field f=new Field(p,sl,spell,at,radius,height,ticks,beat,follows,wet);setup.accept(f);
        Field old=field(p,spell);if(old!=null&&!f.auxiliary)close(old,false);FIELDS.add(f);
        if(spell==Spell.STORM_INCARNATION){
            f.marker=EntityTypes.MARKER.create(sl,EntitySpawnReason.TRIGGERED);
            if(f.marker!=null){f.marker.setPos(at);sl.addFreshEntity(f.marker);}
        }
        Zone zone=follows?Zone.around(p,radius):f.marker!=null?Zone.around(f.marker,radius):Zone.at(f.kingdom()?at.add(0,-radius,0):at,radius);
        zone.height(height).lasts(ticks).every(beat).affects(e->targets(p,e)&&f.covers(e.getBoundingBox().getCenter()))
                .onEnter(e->{
                    f.captured.add(e.getUUID());
                    if(spell==Spell.LIGHTNING_STORM)Root.apply(e,30,RootRestriction.MOVE,RootRestriction.JUMP);
                    if(spell==Spell.TYRANTS_PRESENCE&&Beyonders.sequenceOf(e)!=0)Root.apply(e,Spells.isBoss(e)?30:60,RootRestriction.EVERYTHING);
                }).onInside((e,inside)->inside(f,e,inside)).onPulse(left->pulse(f))
                .onEnd(()->close(f,f.age()>=f.ticks)).open(p,sl,f.zoneKey);
        if(spell==Spell.LIGHTNING_STORM)for(LivingEntity ally:Spells.alliesAround(p,sl,at,radius))if(f.covers(ally.position()))Root.apply(ally,30,RootRestriction.MOVE,RootRestriction.JUMP);
        draw(f);return f;
    }
    private static void inside(Field f,LivingEntity e,int inside){
        if(f.closed||!here(f.owner,f.level,f.generation)||!e.isAlive())return;
        if(f.wet)Soaked.apply(e,200);
        boolean second=f.age()%20==0;
        switch(f.spell){
            case MAELSTROM->{
                Vec3 to=f.center().subtract(e.position());e.setDeltaMovement(to.normalize().scale(Math.min(.9,to.length()*.2)));e.hurtMarked=true;
                if(to.length()<1.8){Root.apply(e,8,RootRestriction.MOVE,RootRestriction.JUMP);f.damage.strike(e,7.5f);}
            }
            case MAGNETIC_POLE_PULL->{
                double power=.15+.75*Math.min(1,f.age()/80.0);if(Shock.metal(e))power*=2;
                Vec3 to=f.center().subtract(e.position());Vec3 tangent=new Vec3(-to.z,0,to.x).normalize();
                e.setDeltaMovement(to.normalize().scale(power).add(f.age()>=60?tangent.scale(.7):Vec3.ZERO));e.hurtMarked=true;
            }
            case ELECTROMAGNETIC_DISRUPTION->{
                Falter.apply(e,id(f.spell),.5f,10);
                if(e instanceof Player p){Connection.clearAll(p);Prediction.end(p);}
            }
            case OCEAN_OF_LIGHTNING->{
                Spells.keepEffect(e,MobEffects.SLOWNESS,1,12);
                if(second)shock(f,e,20*(Shock.metal(e)?2:1),Soaked.isSoaked(e)&&f.age()%60==0?10:0);
            }
            case LIGHTNING_FOREST->{if(second){reveal(e);shock(f,e,80,10);}}
            case DEATH_DIRGE->{ArtifactLock.apply(e,15);Spells.keepEffect(e,MobEffects.SLOWNESS,0,15);f.damage.strike(e,2);}
            case TYRANTS_PRESENCE->{
                reveal(e);slow(e,PREFIX+"presence_slow",.25f,true);
                ServerPlayer parasite=Parasite.parasiteIn(e);if(parasite!=null)Parasite.leave(parasite,Parasite.End.OUT_OF_POWER);
            }
            case HURRICANE->{f.damage.strike(e,5);Spells.push(e,f.center(),.9,.35);}
            case STORM_INCARNATION->{Spells.push(e,f.center(),.3,.08);if(f.age()%100==0)Root.apply(e,20,RootRestriction.MOVE,RootRestriction.JUMP);}
            case STORM_HELL->{
                Blind.apply(e,10,6);Spells.keepEffect(e,MobEffects.DARKNESS,0,12);Spells.keepEffect(e,MobEffects.SLOWNESS,0,12);
                if(e instanceof Player p){Flight.end(p);Glide.end(p);p.stopFallFlying();if(!p.isCreative()){p.getAbilities().flying=false;p.onUpdateAbilities();}}
                if(!e.onGround()){Vec3 motion=e.getDeltaMovement();e.setDeltaMovement(motion.x,Math.min(-.6,motion.y),motion.z);e.hurtMarked=true;}
            }
            case CHASM_OF_STORMS->{if(f.age()%60==0){ThunderGodFx.bolt(f.level,e.position().add(0,24,0),e.position(),.18f,8);shock(f,e,80,10);}}
            case DREADFUL_HEAVEN->{Fear.apply(e,f.owner,15);Falter.apply(e,id(f.spell),.5f,15);}
            case TSUNAMI->{Spells.keepEffect(e,MobEffects.SLOWNESS,1,15);}
            default->{}
        }
    }
    private static void pulse(Field f){
        if(f.closed)return;if(!here(f.owner,f.level,f.generation)){close(f,false);return;}
        List<LivingEntity> enemies=around(f.owner,f.level,f.center(),f.radius).stream().filter(e->f.covers(e.getBoundingBox().getCenter())).toList();
        switch(f.spell){
            case LIGHTNING_STORM->fall(f,enemies,25,10);
            case STORM_INCARNATION->{
                if(f.marker!=null){Vec3 wanted=Spells.aimGround(f.owner,f.level,60),delta=wanted.subtract(f.marker.position());f.marker.setPos(f.marker.position().add(delta.normalize().scale(Math.min(1.5,delta.length()))));}
                fall(f,enemies,30,10);
                if(f.age()%100==0)ThunderGodFx.cue(f.level,f.center(),"roar");
            }
            case STORM_HELL->{if(f.age()%20==0)fall(f,enemies,40,10);}
            case LIGHTNING_FOREST->{if(f.age()%20==0){for(LivingEntity e:enemies.stream().limit(6).toList())ThunderGodFx.bolt(f.level,e.position().add(0,24,0),e.position(),.3f,8);}}
            case SEA_MASTER->{
                if(f.age()%20==0)for(var residue:Residue.near(f.level,f.center(),f.radius,600))ParticleShapes.spawnFor(f.owner,spark(),residue.at().add(0,.2,0),new Vec3(0,.02,0));
            }
            case OCEAN_OF_LIGHTNING->{if(f.age()%10==0)ParticleShapes.ring(f.level,spark(),f.center().add(0,.6,0),f.radius,Math.min(64,(int)(f.radius*4)),.03);}
            case CHASM_OF_STORMS->{if(f.age()%20==0)Rage.add(f.owner,10);}
            default->{}
        }
        if(f.spell==Spell.HURRICANE||f.spell==Spell.OCEAN_OF_LIGHTNING||f.spell==Spell.STORM_HELL||f.spell==Spell.MAGNETIC_POLE_PULL){
            for(Projectile shot:Spells.hostileShotsAround(f.owner,f.level,f.center(),f.radius)){
                if(!f.covers(shot.position()))continue;
                if(f.spell==Spell.HURRICANE){shot.setDeltaMovement(shot.position().subtract(f.center()).normalize().scale(1.5));shot.hurtMarked=true;}
                else if(f.spell==Spell.MAGNETIC_POLE_PULL){if(!(shot instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow))continue;shot.setDeltaMovement(f.center().add(0,1,0).subtract(shot.position()).normalize());shot.hurtMarked=true;}
                else shot.discard();
            }
        }
    }
    private static void fall(Field f,List<LivingEntity> enemies,float damage,int paralysis){
        LivingEntity target=enemies.isEmpty()?null:enemies.get(f.owner.getRandom().nextInt(enemies.size()));
        if(f.spell==Spell.LIGHTNING_STORM)target=null;
        Vec3 at=target==null?f.center().add((f.owner.getRandom().nextDouble()-.5)*f.radius,0,(f.owner.getRandom().nextDouble()-.5)*f.radius):target.getBoundingBox().getCenter();
        ThunderGodFx.bolt(f.level,at.add(0,18,0),at,.13f,8);Shock.boltLanded(f.level,at);
        if(f.spell==Spell.LIGHTNING_STORM){
            for(LivingEntity e:Spells.livingAround(f.owner,f.level,at,1.5))if(f.covers(e.position())){
                boolean friend=Spells.isFriend(f.owner,e);
                Shock.strike(f.owner,e,damage*(friend&&!source(f.owner)?.5f:1),paralysis,f.damage,friend||source(f.owner));
            }
        }else if(target!=null)shock(f,target,damage,paralysis);
    }
    private static void shock(Field f,LivingEntity e,float damage,int paralysis){Shock.strike(f.owner,e,damage,paralysis,f.damage,source(f.owner));}
    private static void draw(Field f){
        if(f.spell==Spell.ELECTROMAGNETIC_DISRUPTION)return;
        if(f.spell==Spell.STORM_INCARNATION){
            Entity attached=f.marker==null?f.owner:f.marker;
            ParticleShapes.modelOn(f.level,ThunderGodFx.model("storm_cloud",f.ticks).scale(7).tag(f.tag),attached,new Vec3(0,12,0),0,0,0);
            ParticleShapes.modelOn(f.level,ThunderGodFx.model("cloud_face",f.ticks).scale(5).tag(f.tag),attached,new Vec3(0,14,-3),0,0,0);
            ParticleShapes.modelOn(f.level,ThunderGodFx.model("rain_sheet",f.ticks).scale(8,6,1).tag(f.tag),attached,new Vec3(0,6,0),0,0,0);
            return;
        }
        if(f.kingdom()){
            ParticleShapes.model(f.level,ThunderGodFx.model(f.spell==Spell.DREADFUL_HEAVEN?"kingdom_dreadful":"kingdom_chasm",f.ticks).scale(20).tag(f.tag),f.center(),0,0,0);
            ParticleShapes.model(f.level,ThunderGodFx.model("storm_cloud",f.ticks).scale(10).tag(f.tag),f.center().add(0,12,0),0,0,0);
            if(f.spell==Spell.CHASM_OF_STORMS)for(int i=-1;i<=1;i++)ParticleShapes.model(f.level,ThunderGodFx.model("rain_sheet",f.ticks).scale(6,6,1).tag(f.tag),f.center().add(i*8,5,0),0,0,0);
            return;
        }
        if(f.spell==Spell.HURRICANE){
            for(int layer=0;layer<4;layer++)ParticleShapes.modelOn(f.level,ThunderGodFx.model("whirlpool",f.ticks).scale(2+layer*.3f).tag(f.tag)
                    .key(f.ticks,ModelFx.pose().spin(1800)),f.owner,new Vec3(0,layer*2,0),0,0,0);
            return;
        }
        if(f.spell==Spell.TYRANTS_PRESENCE||f.spell==Spell.DEATH_DIRGE){
            ParticleShapes.modelOn(f.level,ThunderGodFx.model(f.spell==Spell.TYRANTS_PRESENCE?"tyrant_crown":"sound_wave",f.ticks).scale(1.4f).tag(f.tag),f.owner,new Vec3(0,2,0),0,0,0);return;
        }
        if(f.spell==Spell.STORM_HELL){
            ParticleShapes.model(f.level,ThunderGodFx.model("storm_cloud",f.ticks).scale(15).tag(f.tag),f.center().add(0,20,0),0,0,0);
            for(int i=-1;i<=1;i++)ParticleShapes.model(f.level,ThunderGodFx.model("rain_sheet",f.ticks).scale(20,12,1).tag(f.tag),f.center().add(i*20,10,0),0,0,0);
            return;
        }
        String model=f.spell==Spell.LIGHTNING_STORM||f.spell==Spell.LIGHTNING_FOREST?"ball_lightning":"whirlpool";
        ModelFx fx=ThunderGodFx.model(model,f.ticks).scale((float)(f.radius/(model.equals("whirlpool")?1:8))).tag(f.tag);
        if(f.spell==Spell.OCEAN_OF_LIGHTNING)fx=fx.color(0xFFD6ECFF).glow();
        ParticleShapes.model(f.level,fx,f.center().add(0,.1,0),0,0,0);
    }
    private static void close(Field f,boolean expired){
        if(f==null||f.closed)return;f.closed=true;FIELDS.remove(f);
        Zone.close(f.owner,f.zoneKey);
        if(f.kingdom())Dome.close(f.owner,PREFIX+"kingdom");
        if(f.marker!=null){ParticleShapes.clearModels(f.level,f.marker,f.tag);f.marker.discard();}
        ParticleShapes.clearModels(f.level,f.center(),f.tag);ParticleShapes.clearModels(f.level,f.owner,f.tag);
        for(UUID id:f.captured){Entity entity=f.level.getEntity(id);if(entity instanceof LivingEntity e){slow(e,PREFIX+"presence_slow",.25f,false);Falter.remove(e,ThunderGodHigher.id(f.spell));}}
        for(Mob summon:f.summons)summon.discard();
        if(!expired||!here(f.owner,f.level,f.generation))return;
        if(f.spell==Spell.HURRICANE&&sequence(f.owner)<=3)open(f.owner,f.level,Spell.LIGHTNING_STORM,f.center(),4.5,8,80,4,false,false);
        if(f.spell==Spell.MAGNETIC_POLE_PULL)for(LivingEntity e:around(f.owner,f.level,f.center(),14)){f.damage.strike(e,120);Spells.push(e,f.center(),3,.7);}
        if(f.spell==Spell.LIGHTNING_FOREST)afterglow(f.owner,f.level,f.center(),40,120);
    }
    private static void afterglow(ServerPlayer p,ServerLevel sl,Vec3 at,double radius,int ticks){
        open(p,sl,Spell.OCEAN_OF_LIGHTNING,at,radius,3,ticks,10,false,false,f->{f.auxiliary=true;f.zoneKey=id(Spell.OCEAN_OF_LIGHTNING)+"_afterglow_"+UUID.randomUUID();});
    }
    private static Result seaMaster(ServerPlayer p,ServerLevel sl){
        Field f=open(p,sl,Spell.SEA_MASTER,p.position(),16,12,600,5,false,true);
        for(int i=0;i<3;i++){
            Vec3 at=p.position().add(Math.cos(i*Math.PI*2/3)*3,0,Math.sin(i*Math.PI*2/3)*3);
            Mob summon=Marionette.spawnSummon(i==2?EntityTypes.DROWNED:EntityTypes.GUARDIAN,p,sl,at,p.getYRot(),600);
            if(summon!=null)f.summons.add(summon);
        }
        return Result.DONE;
    }
    public static double projectileFactor(Projectile shot){
        if(!(shot.getOwner() instanceof LivingEntity owner))return 1;
        return FIELDS.stream().anyMatch(f->f.spell==Spell.SEA_MASTER&&f.level==shot.level()&&f.covers(shot.position())&&!Spells.isFriend(f.owner,owner))?.4:1;
    }
    private static Result droplet(ServerPlayer p,ServerLevel sl){
        Rage.Cast cast=Rage.cast(p);long generation=ThunderGodPowers.generation(p);
        Later.run(sl,20,()->{
            if(!here(p,sl,generation)||Silence.isSilenced(p))return;
            Shot.of(ThunderGodFx.model("droplet",30).glow().scale(.3f)).speed(4).range(40).width(.3)
                    .onEnd((at,block)->{
                        if(!here(p,sl,generation))return;
                        for(LivingEntity e:around(p,sl,at,3)){
                            cast.strike(e,!Spells.isBoss(e)&&e.getMaxHealth()<=60?Math.max(140,e.getMaxHealth()*3):140);
                            if(e.isAlive()){NoHeal.apply(e,200);e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,200,1));e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,200,0));}
                        }
                        ParticleShapes.burst(sl,spark(),at,30,.02,.3);
                    }).fire(p,sl,p.getEyePosition(),look(p));
        });
        return Result.DONE;
    }
    private static Result legion(ServerPlayer p,ServerLevel sl){
        Vec3 at=Spells.aimPoint(p,sl,30);Rage.Cast cast=Rage.cast(p);long generation=ThunderGodPowers.generation(p);
        for(int i=0;i<12;i++){final int step=i;Later.run(sl,i*40/12,()->{
            if(!here(p,sl,generation))return;
            for(LivingEntity e:around(p,sl,at,2.5)){
                e.stopUsingItem();cast.strikeTrue(e,8);wear(e,1);
                ParticleShapes.slash(sl,SlashFx.arc("slash/streaks").color(0x80D6ECFF).radius(2.4f).lifetime(5),e.getBoundingBox().getCenter(),step*30,0,step%2==0?45:-45);
            }
        });}return Result.DONE;
    }
    private static Result sonic(ServerPlayer p,ServerLevel sl){
        Vec3 at=Spells.aimPoint(p,sl,40);Rage.Cast cast=Rage.cast(p);
        for(LivingEntity e:around(p,sl,at,4)){cast.strike(e,90);Spells.push(e,at,2.5,.6);Silence.apply(e,30);}
        ParticleShapes.model(sl,ThunderGodFx.model("sound_wave",20).scale(.2f).key(18,ModelFx.pose().scale(4).alpha(0)),at,0,0,0);
        return Result.DONE;
    }
    private record Ward(ServerPlayer owner,LivingEntity ally,ServerLevel level,Vec3 at,Vec3 normal,long until,long generation,String tag){
        Vec3 wall(){return at.add(normal.scale(1.3));}
        boolean behind(Vec3 p){return p.subtract(wall()).dot(normal)<=0;}
        boolean live(){return level.getGameTime()<until&&ally.isAlive()&&here(owner,level,generation)&&ally.position().distanceToSqr(at)<=36;}
    }
    private static Result cloudSword(ServerPlayer p,ServerLevel sl){
        LivingEntity ally=Spells.aimAlly(p,sl,40);
        if(ally!=null){
            if(source(p))return Result.REFUSED;
            String tag=PREFIX+"cloud_guard_"+UUID.randomUUID();Vec3 at=ally.position();
            Ward ward=new Ward(p,ally,sl,at,Spells.flatLook(p),sl.getGameTime()+300,ThunderGodPowers.generation(p),tag);WARDS.add(ward);
            Wall.at(at.add(Spells.flatLook(p).scale(1.3)),p.getYRot(),3,4).lasts(300).stopsProjectiles()
                    .onEnd(()->{WARDS.remove(ward);ParticleShapes.clearModels(sl,at,tag);}).raise(p,sl,tag);
            ParticleShapes.model(sl,ThunderGodFx.model("cloud_sword",300).scale(2).tag(tag).key(10,ModelFx.pose().up(-2)),at.add(0,2,0),p.getYRot(),0,0);
            return Result.DONE;
        }
        Vec3 end=Spells.aimPoint(p,sl,40),from=end.add(0,12,0);Rage.Cast cast=Rage.cast(p);
        ParticleShapes.model(sl,ThunderGodFx.model("cloud_sword",14).scale(2).key(10,ModelFx.pose().up(-12)),from,p.getYRot(),0,180);
        long generation=ThunderGodPowers.generation(p);
        Later.run(sl,10,()->{if(here(p,sl,generation))for(LivingEntity e:line(p,sl,from,end,1.5)){cast.strike(e,160);Spells.push(e,end,1.7,.4);}});
        return Result.DONE;
    }
    private static final class Light {
        final ServerPlayer owner;final ServerLevel level;final long until,generation;final Rage.Cast damage;final Set<UUID> hit=new HashSet<>();
        Vec3 direction;double travelled;boolean released;
        Light(ServerPlayer p,ServerLevel sl){owner=p;level=sl;until=sl.getGameTime()+120;generation=ThunderGodPowers.generation(p);damage=Rage.cast(p);direction=look(p);released=!p.isShiftKeyDown();}
    }
    private static Result light(ServerPlayer p,ServerLevel sl){
        if(!Transit.canGo(p)||Root.has(p,RootRestriction.MOVE))return Result.REFUSED;
        Light light=new Light(p,sl);LIGHT.put(p.getUUID(),light);Concealment.hide(p,122);FallGuard.protect(p,320);
        ParticleShapes.modelOn(sl,ThunderGodFx.model("ball_lightning",120).scale(.5f).tag(id(Spell.INCARNATION_OF_LIGHT)),p,new Vec3(0,1,0),0,0,0);
        return Result.DONE;
    }
    private static void lightTick(Light f){
        ServerPlayer p=f.owner;
        if(!here(p,f.level,f.generation)||!p.isAlive()){finishLight(p,false);return;}
        if(!p.isShiftKeyDown())f.released=true;
        if(f.level.getGameTime()>=f.until||f.travelled>=150||f.released&&p.isShiftKeyDown()||Root.has(p,RootRestriction.TELEPORT)||Root.has(p,RootRestriction.MOVE)){
            finishLight(p,true);return;
        }
        f.direction=Spells.turnToward(f.direction,look(p),12);Vec3 from=p.position(),next=from.add(f.direction.scale(Math.min(4,150-f.travelled)));
        var wall=f.level.clip(new ClipContext(from.add(0,1,0),next.add(0,1,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        if(wall.getType()!=HitResult.Type.MISS)next=wall.getLocation().subtract(f.direction.scale(.8)).add(0,-1,0);
        if(!Spells.fits(p,f.level,next)){finishLight(p,true);return;}
        for(LivingEntity e:line(p,f.level,from.add(0,1,0),next.add(0,1,0),1))if(f.hit.add(e.getUUID()))f.damage.strike(e,120);
        ParticleShapes.line(f.level,spark().lifetime(60,0),from.add(0,1,0),next.add(0,1,0),2);
        p.teleportTo(next.x,next.y,next.z);p.fallDistance=0;f.travelled+=from.distanceTo(next);Concealment.hide(p,4);
        if(wall.getType()!=HitResult.Type.MISS)finishLight(p,true);
    }
    private static void finishLight(Player p,boolean explode){
        Light f=LIGHT.remove(p.getUUID());if(f==null)return;Concealment.reveal(p);ParticleShapes.clearModels(f.level,p,id(Spell.INCARNATION_OF_LIGHT));FallGuard.protect(p,200);
        if(explode&&here(p,f.level,f.generation))for(LivingEntity e:around(p,f.level,p.position(),5))f.damage.strike(e,80);
    }
    private record Arrival(ServerPlayer owner,ServerLevel level,Vec3 at,long until,long generation,Rage.Cast damage){}
    private static Result starfall(ServerPlayer p,ServerLevel sl){
        if(!Transit.canGo(p))return Result.REFUSED;
        Vec3 spot=Spells.aimGround(p,sl,160);if(!Spells.fits(p,sl,spot))return Result.REFUSED;
        Arrival arrival=new Arrival(p,sl,spot,sl.getGameTime()+40,ThunderGodPowers.generation(p),Rage.cast(p));ARRIVALS.put(p.getUUID(),arrival);
        Concealment.hide(p,42);Root.apply(p,40,RootRestriction.MOVE,RootRestriction.JUMP);FallGuard.protect(p,240);
        ParticleShapes.model(sl,ThunderGodFx.model("ball_lightning",40).scale(.5f).key(20,ModelFx.pose().up(24)).key(40,ModelFx.pose().alpha(0)),p.position(),0,0,0);
        Later.run(sl,10,()->{if(ARRIVALS.get(p.getUUID())==arrival)ParticleShapes.model(sl,ThunderGodFx.model("warning_ring",30).scale(7),spot.add(0,.05,0),0,0,0);});
        return Result.DONE;
    }
    private static void arrive(Arrival f){
        ARRIVALS.remove(f.owner.getUUID(),f);Concealment.reveal(f.owner);Root.remove(f.owner,RootRestriction.MOVE,RootRestriction.JUMP);
        if(!here(f.owner,f.level,f.generation)||!Transit.canGo(f.owner)||!Spells.fits(f.owner,f.level,f.at))return;
        f.owner.teleportTo(f.at.x,f.at.y,f.at.z);f.owner.fallDistance=0;FallGuard.protect(f.owner,200);
        for(LivingEntity e:around(f.owner,f.level,f.at,7)){
            double ratio=Math.min(1,Spells.distanceToBody(e,f.at)/7);Shock.strike(f.owner,e,(float)(400-250*ratio),20,f.damage,source(f.owner));Spells.push(e,f.at,3,.8);
        }
        afterglow(f.owner,f.level,f.at,7,200);
    }
    private static Result chain(ServerPlayer p,ServerLevel sl){
        List<LivingEntity> initial=around(p,sl,p.position(),40).stream().filter(e->p.hasLineOfSight(e)).sorted(Comparator.comparingDouble(p::distanceToSqr)).limit(3).toList();
        if(initial.isEmpty())return Result.REFUSED;Rage.Cast cast=Rage.cast(p);Set<UUID> visited=new HashSet<>();
        for(LivingEntity e:initial){visited.add(e.getUUID());ThunderGodFx.bolt(sl,p.getEyePosition(),e.getBoundingBox().getCenter(),.08f,6);Shock.strike(p,e,60,10,cast,source(p));}
        for(LivingEntity e:initial)for(LivingEntity next:around(p,sl,e.position(),8).stream().filter(t->!visited.contains(t.getUUID())).sorted(Comparator.comparingDouble(e::distanceToSqr)).limit(Soaked.isSoaked(e)?3:1).toList())
            chainStep(p,sl,e.getBoundingBox().getCenter(),next,42,3,visited,cast);
        return Result.DONE;
    }
    private static void chainStep(Player p,ServerLevel sl,Vec3 from,LivingEntity target,float damage,int jumps,Set<UUID> visited,Rage.Cast cast){
        if(!target.isAlive()||!visited.add(target.getUUID()))return;
        ThunderGodFx.bolt(sl,from,target.getBoundingBox().getCenter(),.08f,6);Shock.strike(p,target,damage,10,cast,source(p));
        if(jumps==0)return;
        for(LivingEntity next:around(p,sl,target.position(),8).stream().filter(e->!visited.contains(e.getUUID())).sorted(Comparator.comparingDouble(target::distanceToSqr)).limit(Soaked.isSoaked(target)?3:1).toList())
            chainStep(p,sl,target.getBoundingBox().getCenter(),next,damage*.7f,jumps-1,visited,cast);
    }
    private static Result disruption(ServerPlayer p,ServerLevel sl){
        Vec3 eye=p.getEyePosition(),far=eye.add(look(p).scale(40));
        var aimed=sl.clip(new ClipContext(eye,far,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        LivingEntity target=aimEnemy(p,sl,40);
        Vec3 center=target!=null?target.position():aimed.getType()==HitResult.Type.BLOCK?aimed.getLocation():p.position();
        Field f=open(p,sl,Spell.ELECTROMAGNETIC_DISRUPTION,center,18,24,400,5,false,false);
        if(sequence(p)<=1)for(LivingEntity e:rawAround(p,sl,f.center(),18))if(!Spells.isFriend(p,e)){
            reveal(e);Unnoticed.end(e);Marks.clearFrom(p,e);
            if(e instanceof Player enemy){Connection.clear(enemy,p);Countdown.cancel(enemy);Study.forget(enemy,p);}
        }
        return Result.DONE;
    }
    private static Result punishment(ServerPlayer p,ServerLevel sl){
        LivingEntity target=Spells.aimLiving(p,sl,80,false,e->!Spells.isFriend(p,e)&&!separated(p,e));if(target==null)return Result.REFUSED;
        long generation=ThunderGodPowers.generation(p);Rage.Cast cast=Rage.cast(p);
        ParticleShapes.modelOn(sl,ThunderGodFx.model("heavenly_pillar",24).scale(3,8,3),target,new Vec3(0,3,0),0,0,0);
        Later.run(sl,24,()->{
            if(!here(p,sl,generation)||!target.isAlive()||target.level()!=sl)return;
            Vec3 at=target.position();List<LivingEntity> victims=rawAround(p,sl,at,4);
            for(LivingEntity e:victims)if(e==target||targets(p,e)||source(p)&&!Spells.isFriend(p,e)){
                reveal(e);if(e instanceof Player form&&Form.current(form)!=null)Form.end(form);Swarm.end(e);
                float damage=e==target?350:210;if(purified(e))damage*=2;
                ThunderGodFx.bolt(sl,e.position().add(0,28,0),e.position(),.3f,10);Shock.boltLanded(sl,e.position());
                if(!Shock.immune(e))cast.strikeTrue(e,damage);
            }
        });return Result.DONE;
    }
    private static boolean purified(LivingEntity e){return Spells.isEvil(e)||Corrosion.isCorroding(e)||e.hasEffect(MobEffects.WITHER);}
    private static void reveal(LivingEntity e){if(Exile.isExiled(e))Exile.bringBack(e);Concealment.reveal(e);e.removeEffect(MobEffects.INVISIBILITY);}
    private static void breakBarriers(Field f){
        java.util.function.Predicate<LivingEntity> whose=owner->!Spells.isFriend(f.owner,owner);
        Dome.ageNear(f.level,f.center(),f.radius,whose,1000000);Wall.ageNear(f.level,f.center(),f.radius,whose,1000000);Zone.closeNear(f.level,f.center(),f.radius,whose);
        for(LivingEntity e:rawAround(f.owner,f.level,f.center(),f.radius))if(!Spells.isFriend(f.owner,e))reveal(e);
    }
    private static Result roar(ServerPlayer p,ServerLevel sl){
        Rage.Cast cast=Rage.cast(p);
        for(LivingEntity e:Spells.enemiesInCone(p,sl,p.getEyePosition(),look(p),20,35)){
            cast.strike(e,70);Spells.push(e,p.position(),2.1,.4);e.stopRiding();e.stopUsingItem();Root.apply(e,100,RootRestriction.USE_ITEMS);Root.apply(e,20,RootRestriction.MOVE,RootRestriction.JUMP);
            if(sequence(p)<=1){Fear.apply(e,p,60);Silence.apply(e,20);}
        }return Result.DONE;
    }
    private static Result horror(ServerPlayer p,ServerLevel sl){
        for(LivingEntity e:around(p,sl,p.position(),64)){Fear.apply(e,p,Spells.isBoss(e)?60:160);ArtifactLock.apply(e,200);}return Result.DONE;
    }
    private static Result requiem(ServerPlayer p,ServerLevel sl){
        LivingEntity target=aimEnemy(p,sl,32);if(target==null)return Result.REFUSED;
        Rage.Cast cast=Rage.cast(p);long generation=ThunderGodPowers.generation(p);Silence.apply(target,160);Falter.apply(target,id(Spell.REQUIEM),.6f,160);
        for(int t=0;t<160;t+=20)Later.run(sl,t,()->{
            if(here(p,sl,generation)&&target.isAlive()&&target.level()==sl){cast.strike(target,8);Madness.add(p,target,1);ParticleShapes.burst(sl,ParticleFx.of("fx/thunder_god/spirit_hand").color(ThunderGodFx.SILVER).size(.3f).lifetime(18),target.getBoundingBox().getCenter(),6,.05,.12);}
        });return Result.DONE;
    }
    private static Result godRoar(ServerPlayer p,ServerLevel sl){
        Rage.Cast cast=Rage.cast(p);
        for(LivingEntity e:rawAround(p,sl,p.position(),30))if(!Spells.isFriend(p,e)||source(p)){
            reveal(e);cast.strike(e,purified(e)?240:120);Fear.apply(e,p,120);Falter.apply(e,id(Spell.ROAR_OF_THE_THUNDER_GOD),.5f,120);Root.apply(e,20,RootRestriction.MOVE,RootRestriction.JUMP);
        }return Result.DONE;
    }
    private static Result kingdom(ServerPlayer p,ServerLevel sl,Spell spell){
        close(field(p,Spell.CHASM_OF_STORMS),false);close(field(p,Spell.DREADFUL_HEAVEN),false);
        Field f=open(p,sl,spell,p.position().add(0,1,0),15,30,1200,5,false,true);
        Dome.sphere(f.center(),15).lasts(1200).keepsEnemiesIn().keepsEnemiesOut().holdsOwner().outsiders(e->true).sealsProjectiles()
                .onEnd(()->close(f,false)).raise(p,sl,PREFIX+"kingdom");
        return Result.DONE;
    }
    private static Result tsunami(ServerPlayer p,ServerLevel sl){
        boolean king=sequence(p)<=3;double width=king?48:24,height=king?40:10,range=king?80:40;int ticks=king?60:40;
        Vec3 start=p.position();Vec3[] front={start},direction={Spells.flatLook(p)};Set<UUID> hit=new HashSet<>();Rage.Cast cast=Rage.cast(p);long generation=ThunderGodPowers.generation(p);
        ThunderGodFx.wave(sl,start,p.getYRot(),(float)(width/1.78),(float)(height/.9),(float)range,ticks);
        for(int t=1;t<=ticks;t++){final boolean last=t==ticks;Later.run(sl,t,()->{
            if(!here(p,sl,generation))return;
            Rejection marked=REJECTIONS.stream().filter(r->r.owner==p&&r.target.isAlive()).findFirst().orElse(null);
            if(marked!=null)direction[0]=Spells.turnToward(direction[0],marked.target.position().subtract(front[0]).multiply(1,0,1),4);
            front[0]=front[0].add(direction[0].scale(range/ticks));Vec3 side=new Vec3(direction[0].z,0,-direction[0].x);
            for(LivingEntity e:sl.getEntitiesOfClass(LivingEntity.class,new AABB(front[0].add(-width/2,-1,-width/2),front[0].add(width/2,height,width/2)),LivingEntity::isAlive)){
                Vec3 d=e.position().subtract(front[0]);if(Math.abs(d.dot(side))>width/2+e.getBbWidth()/2||Math.abs(d.dot(direction[0]))>2)continue;
                e.clearFire();if(targets(p,e)&&hit.add(e.getUUID())){Soaked.apply(e,200);cast.strike(e,110);Spells.push(e,start,3,.7);}
            }
            for(Entity loose:Spells.looseThingsAround(p,sl,front[0],width/2)){loose.setDeltaMovement(direction[0].scale(2).add(0,.2,0));loose.hurtMarked=true;}
            if(last&&king){Vec3 middle=start.lerp(front[0],.5);open(p,sl,Spell.TSUNAMI,middle,range/2+24,3,200,5,false,true,flood->{flood.pathStart=start;flood.pathEnd=front[0];});}
        });}return Result.DONE;
    }
    private static final class Rejection {
        final ServerPlayer owner;final LivingEntity target;final ServerLevel level;final long until,generation;final Rage.Cast damage;Vec3 dry;
        Rejection(ServerPlayer p,LivingEntity e,ServerLevel sl){owner=p;target=e;level=sl;until=sl.getGameTime()+400;generation=ThunderGodPowers.generation(p);damage=Rage.cast(p);dry=e.position();}
    }
    private static Result seaWrath(ServerPlayer p,ServerLevel sl){
        LivingEntity target=aimEnemy(p,sl,40);if(target==null)return Result.REFUSED;
        REJECTIONS.removeIf(r->r.owner==p&&r.target==target);REJECTIONS.add(new Rejection(p,target,sl));return Result.DONE;
    }
    private static boolean waterNear(LivingEntity e,ServerLevel sl){
        if(Soaked.isWet(e))return true;
        if(FIELDS.stream().anyMatch(f->f.wet&&f.level==sl&&f.center().distanceToSqr(e.position())<=(f.radius+10)*(f.radius+10)))return true;
        BlockPos p=e.blockPosition();for(BlockPos pos:BlockPos.betweenClosed(p.offset(-10,-10,-10),p.offset(10,10,10)))
            if(pos.distSqr(p)<=100&&sl.getFluidState(pos).is(FluidTags.WATER))return true;
        return false;
    }
    private static final class Banish {
        final ServerPlayer owner;final LivingEntity target;final ServerLevel level;final long generation;boolean cancelled;
        Banish(ServerPlayer p,LivingEntity target,ServerLevel sl){owner=p;this.target=target;level=sl;generation=ThunderGodPowers.generation(p);}
    }
    private static Result banish(ServerPlayer p,ServerLevel sl){
        LivingEntity ally=Spells.aimAlly(p,sl,40),target=ally!=null?ally:aimEnemy(p,sl,40);if(target==null)return Result.REFUSED;
        Banish banish=new Banish(p,target,sl);BANISHED.add(banish);boolean friendly=ally!=null;
        boolean done=Exile.banish(target,friendly?80:Spells.isBoss(target)?80:200,null,(e,level)->{
            BANISHED.remove(banish);
            if(banish.cancelled||!here(p,level,banish.generation))return;
            if(friendly)ConjuredArmor.of(PREFIX+"storm_shelter").lasts(200).absorption(20).wear(e);
            else{Soaked.apply(e,200);Fear.apply(e,p,80);Rage.cast(p).strike(e,100);}
            ThunderGodFx.cue(level,e.position(),"banish_return");
        });
        if(!done){BANISHED.remove(banish);return Result.REFUSED;}
        return Result.DONE;
    }
    public static void tick(ServerLevel sl){
        for(ServerPlayer p:sl.players())if(Form.isIn(p,SOURCE)&&Form.ticksLeft(p)==0)Form.end(p);
        for(Light f:new ArrayList<>(LIGHT.values()))if(f.level==sl)lightTick(f);
        for(Arrival a:new ArrayList<>(ARRIVALS.values()))if(a.level==sl&&sl.getGameTime()>=a.until)arrive(a);
        for(Ward ward:new ArrayList<>(WARDS))if(ward.level==sl&&!ward.live()){WARDS.remove(ward);Wall.close(ward.owner,ward.tag);}
        for(Rejection r:new ArrayList<>(REJECTIONS))if(r.level==sl){
            if(!r.target.isAlive()||sl.getGameTime()>=r.until||!here(r.owner,sl,r.generation)){REJECTIONS.remove(r);continue;}
            if(sl.getGameTime()%5==0){r.target.stopRiding();r.target.setSwimming(false);if(r.target.isInWater()){
                if(!sl.getFluidState(BlockPos.containing(r.dry)).is(FluidTags.WATER))r.target.teleportTo(r.dry.x,r.dry.y,r.dry.z);
                else{r.target.setDeltaMovement(0,.6,0);r.target.hurtMarked=true;}
            }else r.dry=r.target.position();}
            if(sl.getGameTime()%40==0&&waterNear(r.target,sl)){Soaked.apply(r.target,200);r.damage.strike(r.target,30);Spells.push(r.target,r.owner.position(),1,.2);}
        }
    }
    public static void clear(Player p){
        finishLight(p,false);Arrival arrival=ARRIVALS.remove(p.getUUID());if(arrival!=null){Concealment.reveal(p);Root.remove(p,RootRestriction.MOVE,RootRestriction.JUMP);}
        for(Field f:new ArrayList<>(FIELDS))if(f.owner.getUUID().equals(p.getUUID()))close(f,false);
        for(Ward w:new ArrayList<>(WARDS))if(w.owner.getUUID().equals(p.getUUID())){WARDS.remove(w);Wall.close(w.owner,w.tag);}
        REJECTIONS.removeIf(r->r.owner.getUUID().equals(p.getUUID()));
        for(Banish b:new ArrayList<>(BANISHED))if(b.owner.getUUID().equals(p.getUUID())){b.cancelled=true;BANISHED.remove(b);Exile.bringBack(b.target);}
        if(Form.isIn(p,SOURCE))Form.end(p);SequenceBoost.end(p);
        ScreenImages.hide(p,PREFIX+"intuition");
    }
    public static void passive(Player p,ServerLevel sl){
        if(is(p,4)){
            boolean stronger=sl.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(32),e->e!=p&&Beyonders.isBeyonder(e)&&Beyonders.sequenceOf(e)<SoulCore.getSequence(p)).size()>0;
            if(stronger)ScreenImages.show(p,PREFIX+"intuition","thunder_god/spiritual_intuition",ScreenAnchor.TOP_RIGHT,-10,10,16,30);else ScreenImages.hide(p,PREFIX+"intuition");
            if(p.isInWater())Spells.keepEffect(p,MobEffects.CONDUIT_POWER,0,60);
        }
        for(Mob e:sl.getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(8),e->is(p,0)&&e.getMaxHealth()<=60&&Spells.isEnemy(p,e)))slow(e,PREFIX+"tyrant_aura",.25f,true);
    }
    public static void targetTick(LivingEntity e){
        boolean awe=FIELDS.stream().anyMatch(f->f.spell==Spell.TYRANTS_PRESENCE&&f.level==e.level()&&targets(f.owner,e)&&f.covers(e.position()));
        slow(e,PREFIX+"presence_slow",.25f,awe);
        boolean aura=false;if(e instanceof Mob mob&&e.getMaxHealth()<=60&&e.level() instanceof ServerLevel sl)
            aura=sl.players().stream().anyMatch(p->is(p,0)&&p.distanceToSqr(e)<=64&&Spells.isEnemy(p,e));
        slow(e,PREFIX+"tyrant_aura",.25f,aura);
    }
    private static void slow(LivingEntity e,String id,float cut,boolean wanted){Physique.keep(e,net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED,Physique.id(id),-cut,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,wanted);}
    private static void wear(LivingEntity e,int amount){for(EquipmentSlot slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET}){var stack=e.getItemBySlot(slot);if(stack.isDamageableItem())stack.hurtAndBreak(amount,e,slot);}}
    private static List<LivingEntity> rawAround(Player p,ServerLevel sl,Vec3 at,double radius){return sl.getEntitiesOfClass(LivingEntity.class,new AABB(at,at).inflate(radius+2),e->e!=p&&e.isAlive()&&!e.isSpectator()&&!separated(p,e)&&Spells.distanceToBody(e,at)<=radius);}
    private static List<LivingEntity> line(Player p,ServerLevel sl,Vec3 from,Vec3 to,double width){return sl.getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(width+2),e->targets(p,e)&&!separated(p,e)&&e.getBoundingBox().inflate(width).clip(from,to).isPresent());}
    private static double distanceToSegment(Vec3 p,Vec3 a,Vec3 b){Vec3 delta=b.subtract(a);double t=delta.lengthSqr()<.001?0:Math.clamp(p.subtract(a).dot(delta)/delta.lengthSqr(),0,1);return p.distanceTo(a.add(delta.scale(t)));}
}
