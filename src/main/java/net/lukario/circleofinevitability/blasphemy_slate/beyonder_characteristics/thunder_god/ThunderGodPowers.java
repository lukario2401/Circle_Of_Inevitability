package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel.Result;
import net.lukario.circleofinevitability.particles.fx.*;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.*;
import net.lukario.circleofinevitability.screen.ScreenImages;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god.ThunderGod.*;

/** Concrete casts. All scheduled work checks the original caster/level and never edits blocks or world weather. */
public final class ThunderGodPowers {
    public static final String RAIN = PREFIX+"rain", CURTAIN = PREFIX+"curtain", COAT = PREFIX+"lightning_coat";
    private static final String SIREN_BONUS = PREFIX+"siren_bonus", GALE = PREFIX+"gale", SCALES_BURST = PREFIX+"scales_burst";
    private static final Identifier SPHERE_MOVE=Physique.id(PREFIX+"sphere_move"), VEIL_SLOW=Physique.id(PREFIX+"veil_slow");
    private static final Map<UUID,EnumMap<Affliction,Effect>> EFFECTS = new HashMap<>();
    private static final Map<UUID,Rain> RAINS = new HashMap<>();
    private static final Map<UUID,Curtain> CURTAINS = new HashMap<>();
    public enum Affliction { VEIL, SPHERE, DISCORD }
    private static final class Effect {
        final Player caster;
        final ServerLevel level;
        final long until;
        final Rage.Cast cast;
        long pulse;
        UUID taunted;
        Effect(Player caster,int ticks,Rage.Cast cast){this.caster=caster;this.level=(ServerLevel)caster.level();this.until=caster.level().getGameTime()+ticks;this.pulse=caster.level().getGameTime()+10;this.cast=cast;}
    }
    private record Rain(Player owner,ServerLevel level,Vec3 center) {}
    private record Curtain(Player owner,ServerLevel level,Vec3 center,Vec3 normal,Vec3 side) {
        boolean covers(Vec3 at) {
            Vec3 delta=at.subtract(center);
            return Math.abs(delta.dot(normal))<=.8 && Math.abs(delta.dot(side))<=4.3 && delta.y>=-.3 && delta.y<=4.3;
        }
    }
    private ThunderGodPowers() {}
    private static void image(Player p,Spell s,int ticks) { ScreenImages.show(p,PREFIX+s.id(),"thunder_god/"+s.id(),24,ticks); }
    private static boolean here(Player p,ServerLevel sl){return Spells.casterStillHere(p,sl);}
    private static long generation(Player p){return p.getPersistentData().getLongOr(PREFIX+"generation",0L);}
    private static boolean here(Player p,ServerLevel sl,long generation){return here(p,sl)&&generation(p)==generation;}
    private static void tell(Player p,String text){p.sendOverlayMessage(net.minecraft.network.chat.Component.literal(text));}
    public static Result cast(ServerPlayer p,ServerLevel sl,Spell spell) {
        if(!here(p,sl)||Root.has(p,RootRestriction.ABILITIES))return Result.REFUSED;
        Rage.Cast cast=Rage.cast(p);
        Result result=switch(spell){
            case HARPOON -> harpoon(p,sl,cast);
            case ILLUSORY_SCALES -> scales(p,sl);
            case RAGING_BLOW -> ragingBlow(p,sl);
            case WRATH -> wrath(p,sl);
            case WATER_VEIL -> waterHold(p,sl,Affliction.VEIL,cast);
            case WATER_SPHERE -> waterHold(p,sl,Affliction.SPHERE,cast);
            case SWEEPING_WAVE -> wave(p,sl,cast);
            case TIDEHEAL -> tideheal(p,sl);
            case WATER_CURTAIN -> curtain(p,sl);
            case CORROSIVE_RAIN -> rain(p,sl,cast);
            case WINDBLADE -> windblade(p,sl,cast);
            case GALE_BURST -> galeBurst(p,sl,cast);
            case GALE_RIDE -> galeRide(p,sl);
            case WIND_BINDING -> windBinding(p,sl);
            case LIGHTNING_STRIKE -> lightning(p,sl,cast);
            case LIGHTNING_BOLT_ARROW -> boltArrow(p,sl,cast);
            case SIREN_SONG -> siren(p,sl);
            case DISCORD -> discord(p,sl,cast);
            case THUNDERCLAP -> thunderclap(p,sl,cast);
        };
        if(result==Result.DONE){ThunderGodFx.cue(sl,p.position(),spell.id());image(p,spell,40);}
        return result;
    }

    private static Result harpoon(ServerPlayer p,ServerLevel sl,Rage.Cast cast) {
        long generation=generation(p);
        Vec3 from=p.getEyePosition(),direction=p.getLookAngle();
        if(Beyonder.hasSequence(p,7)) {
            LivingEntity assisted=Spells.enemiesInSight(p,sl,24,15).stream()
                    .filter(t->Spells.clearLine(p,sl,from,t.getBoundingBox().getCenter()))
                    .min(Comparator.comparingDouble(t->directionDot(p,t))).orElse(null);
            if(assisted!=null)direction=Spells.turnToward(direction,assisted.getBoundingBox().getCenter().subtract(from),15);
        }
        var block=sl.clip(new ClipContext(from,from.add(direction.scale(24)),ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,p));
        Vec3 stop=block.getLocation();
        LivingEntity target=Spells.firstEnemyOnLine(p,sl,from,stop,.4);
        if(target!=null)stop=target.getBoundingBox().getCenter();
        final Vec3 end=stop;final LivingEntity victim=target;
        int flight=Math.max(1,(int)Math.ceil(from.distanceTo(end)/2.5));
        String tag=PREFIX+UUID.randomUUID();
        ParticleShapes.modelAlong(sl,ThunderGodFx.model("harpoon",flight+42).scale(.6f).tag(tag)
                .key(flight,ModelFx.pose().up((float)from.distanceTo(end))),from,end.subtract(from));
        for(int t=0;t<=flight;t+=2){final int tick=t;Later.run(sl,t,()->{
            if(here(p,sl,generation))ParticleShapes.line(sl,rope(),p.getEyePosition(),from.lerp(end,(double)tick/flight),2);
        });}
        Later.run(sl,flight,()->{
            if(!here(p,sl,generation)){ParticleShapes.clearModels(sl,end,tag);return;}
            if(victim!=null&&victim.isAlive()&&victim.level()==sl&&Spells.distanceToBody(victim,end)<2.4&&Spells.isEnemy(p,victim)){
                cast.strike(victim,30);
                Vec3 toward=p.position().subtract(victim.position());
                Vec3 anchor=victim.position().add(toward.normalize().scale(Math.min(5,Math.max(0,toward.length()-1.2))));
                Reel.in(victim,()->here(p,sl,generation)?anchor:null).speed(1).lasts(20)
                        .onEnd(()->ParticleShapes.clearModels(sl,end,tag)).start(sl);
                ropeFor(p,sl,()->victim.isAlive()?victim.getBoundingBox().getCenter():null,20);
                ThunderGodFx.cue(sl,end,"harpoon_hit");
            }else if(victim==null&&block.getType()!=HitResult.Type.MISS){
                Reel.in(p,()->end).speed(1.5).stopAt(.7).lasts(40)
                        .onEnd(()->{FallGuard.protect(p,200);ParticleShapes.clearModels(sl,end,tag);}).start(sl);
                ropeFor(p,sl,()->end,40);
                ThunderGodFx.cue(sl,end,"rope_reel");
            }else ParticleShapes.clearModels(sl,end,tag);
        });
        return Result.DONE;
    }
    private static double directionDot(Player p,LivingEntity t){return -p.getLookAngle().dot(t.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize());}
    private static ParticleFx rope(){return ParticleFx.of("fx/thunder_god/rope").color(0xFF9C8465).size(.025f).lifetime(3,0);}
    private static void ropeFor(Player p,ServerLevel sl,java.util.function.Supplier<Vec3> anchor,int ticks){
        long generation=generation(p);
        for(int t=0;t<ticks;t+=2)Later.run(sl,t,()->{Vec3 at=anchor.get();if(here(p,sl,generation)&&at!=null)ParticleShapes.line(sl,rope(),p.getEyePosition(),at,2);});
    }

    private static Result scales(ServerPlayer p,ServerLevel sl) {
        if(ConjuredArmor.isWorn(p,SCALES)){ConjuredArmor.takeOff(p,SCALES);return Result.FREE;}
        int advancements=Math.max(0,9-SoulCore.getSequence(p));
        p.getPersistentData().putBoolean(SCALES_BURST,false);
        ConjuredArmor.of(SCALES).lasts(200).absorption(6+2*advancements)
                .onEnd(e->{ParticleShapes.clearModels(sl,p,SCALES);ScreenImages.hide(p,PREFIX+"illusory_scales");}).wear(p);
        ParticleShapes.modelOnTurning(sl,ThunderGodFx.model("scale_armor",200).tag(SCALES),p,Vec3.ZERO,0,0,0);
        return Result.DONE;
    }
    private static Result ragingBlow(ServerPlayer p,ServerLevel sl) {
        if(Rage.count(p)<3){tell(p,"Not angry enough — Raging Blow needs 3 Rage.");return Result.REFUSED;}
        LivingEntity target=Spells.aimEnemy(p,sl,4);
        if(target==null){tell(p,"No enemy in reach.");return Result.REFUSED;}
        int rage=Rage.spend(p),perRage=8+2*Math.max(0,8-SoulCore.getSequence(p));
        float damage=20+perRage*rage;
        if(Marks.take(p,SIREN_BONUS)>0)damage*=1.3f;
        Rage.Cast cast=Rage.blow(p);cast.strike(target,damage);Spells.push(target,p.position(),1.6,.3);
        for(LivingEntity other:Spells.enemiesAround(p,sl,target.position(),2.5))if(other!=target){cast.strike(other,damage*.5f);Spells.push(other,target.position(),1,.2);}
        if(rage==10){target.stopUsingItem();Root.apply(target,100,RootRestriction.USE_ITEMS);Root.apply(target,20,RootRestriction.MOVE,RootRestriction.JUMP);}
        ParticleShapes.model(sl,ThunderGodFx.model("sound_wave",12).scale(.1f).key(10,ModelFx.pose().scale(2.5f).alpha(0)),target.position().add(0,1,0),p.getYRot(),0,0);
        ThunderGodFx.rage(p,Rage.count(p));return Result.DONE;
    }
    private static Result wrath(ServerPlayer p,ServerLevel sl) {
        if(Form.isIn(p,WRATH)){Form.end(p);return Result.FREE;}
        Form.of(WRATH).lasts(200).keepWhile(q->is(q,8))
                .effect(MobEffects.STRENGTH,1).effect(MobEffects.SPEED,0)
                .attribute(Attributes.ATTACK_SPEED,.3,AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                .onEnd((q,why)->{
                    ScreenImages.hide(q,PREFIX+"wrath");
                    if((why==Form.Ending.TIME||why==Form.Ending.ENDED)&&q.isAlive())q.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,80,0));
                    ThunderGodFx.cue(sl,q.position(),"wrath_end");
                }).take(p);
        Fear.end(p);if(Beyonder.hasSequence(p,5))Rage.add(p,3);return Result.DONE;
    }

    static boolean afflicted(Entity e,Affliction kind) {
        var map=EFFECTS.get(e.getUUID());Effect effect=map==null?null:map.get(kind);
        return effect!=null&&e.level()==effect.caster.level()&&e.level().getGameTime()<effect.until;
    }
    private static Result waterHold(ServerPlayer p,ServerLevel sl,Affliction kind,Rage.Cast cast) {
        LivingEntity target=Spells.aimEnemy(p,sl,20);
        if(target==null)return Result.REFUSED;
        int ticks=kind==Affliction.VEIL?120:100;if(Spells.isBoss(target))ticks/=2;
        afflict(p,target,kind,ticks,cast);Soaked.apply(target,200);
        String model=kind==Affliction.VEIL?"water_film":"water_sphere";
        ParticleShapes.modelOn(sl,ThunderGodFx.model(model,ticks).scale(kind==Affliction.VEIL?.7f:3.3333f).tag(PREFIX+kind.name()),
                target,new Vec3(0,kind==Affliction.VEIL?target.getEyeHeight():target.getBbHeight()/2,kind==Affliction.VEIL?-.3:0),0,0,0);
        return Result.DONE;
    }
    private static void afflict(Player owner,LivingEntity target,Affliction kind,int ticks,Rage.Cast cast) {
        remove(target,kind);EFFECTS.computeIfAbsent(target.getUUID(),id->new EnumMap<>(Affliction.class)).put(kind,new Effect(owner,ticks,cast));
        body(target);
    }
    private static void body(LivingEntity target) {
        Physique.keep(target,Attributes.MOVEMENT_SPEED,SPHERE_MOVE,-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,afflicted(target,Affliction.SPHERE));
        Physique.keep(target,Attributes.JUMP_STRENGTH,SPHERE_MOVE,-1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,afflicted(target,Affliction.SPHERE));
        Physique.keep(target,Attributes.MOVEMENT_SPEED,VEIL_SLOW,-.25,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,afflicted(target,Affliction.VEIL));
    }
    private static void remove(LivingEntity target,Affliction kind) {
        var map=EFFECTS.get(target.getUUID());Effect old=map==null?null:map.remove(kind);
        if(map!=null&&map.isEmpty())EFFECTS.remove(target.getUUID());
        if(old==null)return;
        body(target);
        if(target.level() instanceof ServerLevel sl)ParticleShapes.clearModels(sl,target,PREFIX+kind.name());
        if(kind==Affliction.DISCORD){
            Falter.remove(target,PREFIX+"discord");
            LivingEntity taunted=Taunt.targetOf(target);
            if(taunted!=null&&taunted.getUUID().equals(old.taunted))Taunt.remove(target);
        }
    }
    public static void cleanse(LivingEntity target) { for(Affliction kind:Affliction.values())remove(target,kind); }
    public static void tickTarget(LivingEntity target,ServerLevel sl) {
        var effects=EFFECTS.get(target.getUUID());if(effects==null)return;
        long now=sl.getGameTime();
        for(var entry:new ArrayList<>(effects.entrySet())){
            Effect effect=entry.getValue();Affliction kind=entry.getKey();
            if(!target.isAlive()||!here(effect.caster,sl)||!Spells.isEnemy(effect.caster,target)||now>effect.until){remove(target,kind);continue;}
            if(kind==Affliction.SPHERE){target.setDeltaMovement(Vec3.ZERO);target.hurtMarked=true;target.fallDistance=0;if(target instanceof Mob mob)mob.getNavigation().stop();}
            if(kind==Affliction.VEIL&&target instanceof Player p)p.setSprinting(false);
            if(now>=effect.pulse){
                effect.pulse+=10;
                if(kind==Affliction.VEIL){Soaked.apply(target,200);effect.cast.strike(target,2.5f);}
                if(kind==Affliction.SPHERE){Soaked.apply(target,200);effect.cast.strike(target,3);}
                if(kind==Affliction.DISCORD){
                    if(target instanceof Player)effect.cast.strike(target,1.5f);
                    if(target instanceof Mob){
                        LivingEntity nearest=Spells.livingAround(target,sl,target.position(),16).stream()
                                .filter(t->Spells.canBeChosen(target,t)).min(Comparator.comparingDouble(target::distanceToSqr)).orElse(null);
                        if(nearest!=null&&Taunt.apply(target,nearest,(int)(effect.until-now)))effect.taunted=nearest.getUUID();
                    }
                }
            }
            if(now>=effect.until)remove(target,kind);
        }
    }
    private static Result wave(ServerPlayer p,ServerLevel sl,Rage.Cast cast) {
        long generation=generation(p);
        Vec3 start=p.position(),forward=Spells.flatLook(p),side=new Vec3(forward.z,0,-forward.x);
        Set<UUID> hit=new HashSet<>();ThunderGodFx.wave(sl,start,p.getYRot(),3.4f,18,30);
        for(int t=1;t<=30;t++){final double distance=18.0*t/30;Later.run(sl,t,()->{
            if(!here(p,sl,generation))return;
            Vec3 front=start.add(forward.scale(distance));
            for(LivingEntity target:sl.getEntitiesOfClass(LivingEntity.class,new AABB(front,front).inflate(4,3,4),LivingEntity::isAlive)){
                Vec3 d=target.position().subtract(front);
                if(Math.abs(d.dot(side))>3+target.getBbWidth()/2||Math.abs(d.dot(forward))>1||d.y< -1||d.y>3)continue;
                target.clearFire();
                if(Spells.isEnemy(p,target)&&hit.add(target.getUUID())){Soaked.apply(target,200);cast.strike(target,45);Spells.push(target,start,1.6,.25);}
            }
        });}
        return Result.DONE;
    }
    private static Result tideheal(ServerPlayer p,ServerLevel sl) {
        long generation=generation(p);
        LivingEntity looked=Spells.aimAlly(p,sl,24),target=looked==null?p:looked;
        Spells.cleanse(target);target.clearFire();
        for(int t=20;t<=120;t+=20)Later.run(sl,t,()->{
            if(here(p,sl,generation)&&target.isAlive()&&target.level()==sl)Support.heal(p,target,2);
        });
        ParticleShapes.modelOn(sl,ThunderGodFx.model("droplet",120).scale(.3f),target,new Vec3(0,2,0),0,0,0);
        return Result.DONE;
    }
    private static Result curtain(ServerPlayer p,ServerLevel sl) {
        Vec3 middle=p.position().add(Spells.flatLook(p).scale(2)),normal=Spells.flatLook(p),side=new Vec3(normal.z,0,-normal.x);
        Curtain curtain=new Curtain(p,sl,middle,normal,side);
        Wall.at(middle,p.getYRot(),8,4).lasts(120).stopsProjectiles().allowsLivingThrough()
                .onBlock(at->ParticleShapes.burst(sl,ParticleFx.of("fx/thunder_god/splash").color(ThunderGodFx.SILVER).lifetime(8),at,4,.02,.06))
                .onEnd(()->{CURTAINS.remove(p.getUUID(),curtain);ParticleShapes.clearModels(sl,middle,CURTAIN);})
                .raise(p,sl,CURTAIN);
        CURTAINS.put(p.getUUID(),curtain);
        ParticleShapes.model(sl,ThunderGodFx.model("water_curtain",120).scale(8,4,1).tag(CURTAIN),middle,p.getYRot(),0,0);
        return Result.DONE;
    }
    public static boolean inCurtain(LivingEntity e){return CURTAINS.values().stream().anyMatch(c->c.level==e.level()&&c.covers(e.getBoundingBox().getCenter()));}
    public static void slowInCurtain(LivingEntity e){
        boolean inside=CURTAINS.values().stream().anyMatch(c->c.level==e.level()&&Spells.isEnemy(c.owner,e)&&c.covers(e.getBoundingBox().getCenter()));
        Physique.keep(e,Attributes.MOVEMENT_SPEED,Physique.id(PREFIX+"curtain_slow"),-.6,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,inside);
    }
    public static boolean wet(LivingEntity e){return inCurtain(e)||RAINS.values().stream().anyMatch(r->r.level==e.level()&&Zone.covers(r.owner,RAIN,e.position()));}
    public static Vec3 nearestRain(Player p){return RAINS.values().stream().filter(r->r.level==p.level()).map(Rain::center).min(Comparator.comparingDouble(p.position()::distanceToSqr)).orElse(null);}
    private static Result rain(ServerPlayer p,ServerLevel sl,Rage.Cast cast) {
        Vec3 ground=Spells.aimGround(p,sl,12);Rain rain=new Rain(p,sl,ground);
        Zone.at(ground,3.5).height(8).lasts(200).every(10)
                .affects(e->Spells.isEnemy(p,e)).onEnter(e->Soaked.apply(e,200))
                .onInside((e,inside)->{
                    Soaked.apply(e,200);cast.strike(e,3);
                    // Equipment wear uses normal item break handling and never edits terrain.
                    for(var slot:new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET}){
                        var stack=e.getItemBySlot(slot);if(stack.isDamageableItem())stack.hurtAndBreak(1,e,slot);
                    }
                    if(inside>=80)Corrosion.apply(e,p,1,0,60);
                }).onEnd(()->{RAINS.remove(p.getUUID(),rain);ParticleShapes.clearModels(sl,ground,RAIN);})
                .open(p,sl,RAIN);
        RAINS.put(p.getUUID(),rain);
        ParticleShapes.model(sl,ThunderGodFx.model("storm_cloud",200).scale(3.2f).tag(RAIN),ground.add(0,6,0),0,0,0);
        for(int x=-2;x<=2;x+=2)for(int z=-2;z<=2;z+=2)
            ParticleShapes.model(sl,ThunderGodFx.model("rain_sheet",200).scale(2,3,1).tag(RAIN),ground.add(x,3,z),0,0,0);
        return Result.DONE;
    }

    private static Result windblade(ServerPlayer p,ServerLevel sl,Rage.Cast cast) {
        for(int angle:new int[]{-12,0,12}){
            Vec3 direction=Spells.turned(p.getLookAngle(),angle);int[] hits={0};
            Ray.from(p.getEyePosition(),direction).range(24).width(.25).pierces()
                    .onFire((from,to)->ParticleShapes.slashBetween(sl,SlashFx.line("slash/streaks").color(0x55D6ECFF).width(.15f).lifetime(7),from,to))
                    .onHit((target,at)->{if(hits[0]++<2)cast.strike(target,22);}).fire(p,sl);
        }return Result.DONE;
    }
    private static Result galeBurst(ServerPlayer p,ServerLevel sl,Rage.Cast cast) {
        for(LivingEntity target:Spells.enemiesAround(p,sl,p.position(),8)){cast.strike(target,20);Spells.push(target,p.position(),1.5,.4);}
        Set<Entity> deflected=new HashSet<>(Spells.hostileShotsAround(p,sl,p.position(),8));
        for(Entity shot:deflected){
            Vec3 away=shot.position().subtract(p.position()).normalize();shot.setDeltaMovement(away.scale(Math.max(.8,shot.getDeltaMovement().length())));shot.hurtMarked=true;
        }
        for(Entity loose:Spells.looseThingsAround(p,sl,p.position(),8)){
            if(deflected.contains(loose)||!(loose instanceof ItemEntity||loose instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow))continue;
            Vec3 pull=p.getEyePosition().subtract(loose.position());loose.setDeltaMovement(pull.normalize().scale(Math.min(1.2,pull.length()*.3)));loose.hurtMarked=true;
        }
        voiceWave(p,sl,8);return Result.DONE;
    }
    private static Result galeRide(ServerPlayer p,ServerLevel sl) {
        int ticks=p.onGround()?160:Beyonder.hasSequence(p,5)?240:120;
        p.getPersistentData().putLong(GALE,sl.getGameTime()+ticks);
        FallGuard.protect(p,ticks+200);
        if(p.onGround())p.getPersistentData().putBoolean(GALE+"_ground",true);
        else if(Decree.forbids(p,Decree.Rule.PASSAGE)){
            // Passage also forbids Glide in the shared Decree enforcer; offer a grounded wind run.
            p.getPersistentData().putBoolean(GALE+"_ground",true);
            tell(p,"Passage is forbidden here; the wind carries your stride instead.");
        }else{
            p.getPersistentData().putBoolean(GALE+"_ground",false);Flight.grant(p,ticks);
        }
        return Result.DONE;
    }
    public static boolean gale(Player p){return p.getPersistentData().getLongOr(GALE,0L)>p.level().getGameTime();}
    public static void galeTick(Player p){
        if(gale(p)&&p.getPersistentData().getBooleanOr(GALE+"_ground",false)&&!p.onGround()&&p.getDeltaMovement().y>.05
                &&!Decree.forbids(p,Decree.Rule.PASSAGE)){
            int ticks=Beyonder.hasSequence(p,5)?240:120;
            p.getPersistentData().putBoolean(GALE+"_ground",false);
            p.getPersistentData().putLong(GALE,p.level().getGameTime()+ticks);
            Flight.grant(p,ticks);
        }
        boolean grounded=gale(p)&&p.getPersistentData().getBooleanOr(GALE+"_ground",false);
        Physique.keep(p,Attributes.MOVEMENT_SPEED,Physique.id(GALE),1,AttributeModifier.Operation.ADD_MULTIPLIED_BASE,grounded);
        if(gale(p)&&!grounded&&Flight.has(p)&&p.isShiftKeyDown()){p.setDeltaMovement(Vec3.ZERO);p.hurtMarked=true;}
    }
    private static Result windBinding(ServerPlayer p,ServerLevel sl) {
        LivingEntity target=Spells.aimEnemy(p,sl,20);if(target==null)return Result.REFUSED;
        Root.apply(target,60,RootRestriction.MOVEMENT);
        for(LivingEntity other:Spells.enemiesAround(p,sl,target.position(),3))if(other!=target){Spells.pull(other,target.position(),.4,.03);Root.apply(other,30,RootRestriction.MOVEMENT);}
        ParticleShapes.modelOn(sl,ThunderGodFx.model("sound_wave",60).scale(1.8f).key(60,ModelFx.pose().spin(720)),target,new Vec3(0,1,0),0,0,0);
        return Result.DONE;
    }
    private static Result lightning(ServerPlayer p,ServerLevel sl,Rage.Cast cast) {
        LivingEntity target=Spells.aimEnemy(p,sl,40);
        if(target==null){
            WeaponCoat.arm(p,COAT,6,200,(owner,victim,amount,level)->{Shock.strike(owner,victim,15,6,Rage.cast(owner));return amount;});
            return Result.DONE;
        }
        ThunderGodFx.bolt(sl,target.position().add(0,18,0),target.getBoundingBox().getCenter(),.14f,8);
        Shock.strike(p,target,55,10,cast);return Result.DONE;
    }
    private static boolean bow(Player p){return p.getMainHandItem().is(Items.BOW)||p.getMainHandItem().is(Items.CROSSBOW)||p.getOffhandItem().is(Items.BOW)||p.getOffhandItem().is(Items.CROSSBOW);}
    private static Result boltArrow(ServerPlayer p,ServerLevel sl,Rage.Cast cast) {
        long generation=generation(p);
        if(!bow(p)){tell(p,"Lightning Bolt Arrow needs a bow or crossbow in either hand.");return Result.REFUSED;}
        Later.run(sl,20,()->{
            if(!here(p,sl,generation)||!bow(p)||Silence.isSilenced(p)||Root.has(p,RootRestriction.USE_ITEMS))return;
            int[] hits={0};
            Ray.from(p.getEyePosition(),p.getLookAngle()).range(60).width(.3).pierces()
                    .onFire((from,to)->ThunderGodFx.bolt(sl,from,to,.09f,8))
                    .onHit((target,at)->{if(hits[0]++<2)Shock.strike(p,target,95,10,cast);}).fire(p,sl);
        });
        ParticleShapes.modelOnTurning(sl,ThunderGodFx.model("lightning_arrow",20).scale(.35f),p,new Vec3(0,1.2,.4),0,90,0);
        return Result.DONE;
    }
    private static void voiceWave(Player p,ServerLevel sl,float radius){
        ParticleShapes.model(sl,ThunderGodFx.model("sound_wave",20).scale(.1f).key(18,ModelFx.pose().scale(radius).alpha(0)),p.position().add(0,1,0),p.getYRot(),0,0);
    }
    private static Result siren(ServerPlayer p,ServerLevel sl) {
        if(Silence.isSilenced(p))return Result.REFUSED;
        for(LivingEntity target:Spells.enemiesAround(p,sl,p.position(),14))Slumber.put(target,120,Slumber.Kind.SLEEP);
        Marks.add(p,SIREN_BONUS,1,200);voiceWave(p,sl,14);return Result.DONE;
    }
    private static Result discord(ServerPlayer p,ServerLevel sl,Rage.Cast cast) {
        if(Silence.isSilenced(p))return Result.REFUSED;
        for(LivingEntity target:Spells.enemiesAround(p,sl,p.position(),14)){
            afflict(p,target,Affliction.DISCORD,160,cast);if(target instanceof Player)Falter.apply(target,PREFIX+"discord",.4f,160);
        }
        voiceWave(p,sl,14);return Result.DONE;
    }
    private static Result thunderclap(ServerPlayer p,ServerLevel sl,Rage.Cast cast) {
        if(Silence.isSilenced(p))return Result.REFUSED;
        for(LivingEntity target:Spells.enemiesAround(p,sl,p.position(),10)){
            cast.strike(target,25);Root.apply(target,30,RootRestriction.MOVE,RootRestriction.JUMP);Fear.apply(target,p,60);
        }
        voiceWave(p,sl,10);return Result.DONE;
    }
    public static void melee(Player p,LivingEntity target,ServerLevel sl,int step) {
        ParticleShapes.slash(sl,SlashFx.arc("slash/streaks").color(0x99D6ECFF).radius(1.5f+step*.08f).arc(110+step*10)
                .width(.22f).lifetime(8),target.getBoundingBox().getCenter(),p.getYRot(),0,step%2==0?35:-35);
    }
    public static void meleeArc(Player p,LivingEntity primary,ServerLevel sl){
        if(!is(p,5))return;
        LivingEntity next=Spells.enemiesAround(p,sl,primary.position(),5).stream().filter(e->e!=primary)
                .min(Comparator.comparingDouble(primary::distanceToSqr)).orElse(null);
        if(next!=null){ThunderGodFx.bolt(sl,primary.getBoundingBox().getCenter(),next.getBoundingBox().getCenter(),.035f,4);Shock.strike(p,next,5);}
    }
    public static void scalesTick(Player p,ServerLevel sl){
        if(ConjuredArmor.isWorn(p,SCALES)&&SoulCore.getSequence(p)<=5&&p.getAbsorptionAmount()<=0&&!p.getPersistentData().getBooleanOr(SCALES_BURST,false)){
            p.getPersistentData().putBoolean(SCALES_BURST,true);Rage.Cast cast=Rage.cast(p);
            for(LivingEntity target:Spells.enemiesAround(p,sl,p.position(),4)){cast.strike(target,30);Spells.push(target,p.position(),1.2,.3);}
            ThunderGodFx.cue(sl,p.position(),"scales_break");
        }
    }
    public static void release(Player p){
        p.getPersistentData().putLong(PREFIX+"generation",generation(p)+1);
        if(p.level() instanceof ServerLevel sl){
            Zone.close(p,RAIN);RAINS.remove(p.getUUID());Wall.close(p,CURTAIN);CURTAINS.remove(p.getUUID());
            for(var entry:new ArrayList<>(EFFECTS.entrySet())){
                Entity entity=sl.getEntity(entry.getKey());if(!(entity instanceof LivingEntity target))continue;
                for(var effect:new ArrayList<>(entry.getValue().entrySet()))if(effect.getValue().caster.getUUID().equals(p.getUUID()))remove(target,effect.getKey());
            }
        }
        cleanse(p);ConjuredArmor.takeOff(p,SCALES);if(Form.isIn(p,WRATH))Form.end(p);
        WeaponCoat.disarm(p,COAT);
        if(gale(p)&&!p.getPersistentData().getBooleanOr(GALE+"_ground",false))Flight.end(p);
        p.getPersistentData().remove(GALE);galeTick(p);
        ThunderGodFx.rageEnd(p);
    }
    public static void prune(ServerLevel level){
        for(var entry:new ArrayList<>(EFFECTS.entrySet())){
            for(var effect:new ArrayList<>(entry.getValue().entrySet())){
                Effect value=effect.getValue();
                if(value.level!=level||here(value.caster,level)&&level.getGameTime()<=value.until)continue;
                Entity entity=level.getEntity(entry.getKey());
                if(entity instanceof LivingEntity target)remove(target,effect.getKey());else entry.getValue().remove(effect.getKey());
            }
            if(entry.getValue().isEmpty())EFFECTS.remove(entry.getKey());
        }
    }
    public static void clear(Player p){
        release(p);TIDES.reset(p);SKY.reset(p);THUNDER.reset(p);VOICE.reset(p);Rage.clear(p);
    }
}
