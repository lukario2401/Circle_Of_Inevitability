package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye;

import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.particles.fx.*;
import net.lukario.circleofinevitability.status.*;
import net.lukario.circleofinevitability.sounds.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.*;
import net.minecraft.tags.EntityTypeTags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class OmniscientEye {
    public static final String PATHWAY="Omniscient Eye";
    private static final String RITE="omniscient_rite";
    private static final WeaknessTable WEAKNESS=WeaknessTable.otherwise("pierce")
            .rule("frost",e->e.fireImmune())
            .rule("fire",e->e.is(EntityTypeTags.ARTHROPOD))
            .rule("lightning",e->e.isInWater() || e.getArmorValue()>=10)
            .rule("light",e->e.is(EntityTypeTags.UNDEAD))
            .rule("unmaking",Construct::isConstruct)
            .rule("decay",e->e.getMaxHealth()>=100);
    public static final AbilityWheel ARTS=AbilityWheel.create("omniscient_eye_mystic_arts","Mystic Arts")
            .entry("analyze","ability_menus/omniscient_eye/analyze","Analyze",6,1500,0,OmniscientEye::analyze).notCopyable()
            .entry("mirror_curse","ability_menus/omniscient_eye/mirror_curse","Mirror Curse",5,3500,500,OmniscientEye::mirror)
            .entry("vile_thoughts","ability_menus/omniscient_eye/vile_thoughts","Vile Thoughts",5,3000,400,OmniscientEye::vile)
            .entry("improvise","ability_menus/omniscient_eye/improvise","Improvise",5,4500,600,OmniscientEye::improvise)
            .entry("living_tome","ability_menus/omniscient_eye/living_tome","Living Tome",5,5000,900,OmniscientEye::tome)
            .entry("golden_armor_art","ability_menus/omniscient_eye/golden_armor_art","Golden Armor Art",4,6000,800,OmniscientEye::armor);
    static {
        Spells.addCostScale(p->SoulCore.getPathway(p).equals(PATHWAY) && Beyonder.hasSequence(p,5)
                && Zone.covers(p,RITE,p.position())?0.75f:1f);
    }
    public static ModelFx model(String name,int ticks) {
        return ModelFx.of("omniscient_eye/"+name).glow().seeThrough().lifetime(ticks).fade(3,5);
    }
    static void show(ServerLevel sl,LivingEntity target,String name,int ticks) {
        ParticleShapes.modelOn(sl,model(name,ticks).pivot(8,8,8),target,new Vec3(0,target.getBbHeight()+.5,0),0,0,0);
    }
    static void sound(ServerPlayer p,String cue) {
        Spells.sound(p.level(),p.position(),ModSounds.OMNISCIENT_EYE.get(cue).get(),.7f,1);
    }
    static boolean ready(Player p,int seq,boolean bypass) {
        return (bypass || SoulCore.getPathway(p).equals(PATHWAY)) && Beyonder.hasSequence(p,seq);
    }
    public static void key(Player p,Level level,ServerLevel sl,int key,boolean bypass) {
        if(!(p instanceof ServerPlayer sp) || !ready(p,new int[]{9,9,8,7,6,6,0,0}[key],bypass))return;
        boolean sneak=p.isShiftKeyDown();
        if(key==4) {if(sneak)Codex.open(sp);else Codex.replicate(sp);return;}
        if(key==5) {if(sneak)ARTS.quickCast(sp);else ARTS.open(sp);return;}
        if(key>5)return;
        if(key==1 && sneak) {ritual(sp,sl);return;}
        if(key==2 && sneak) {
            if(!Spells.paySpirituality(p,150))return;
            p.getPersistentData().putLong("omniscient_keen",sl.getGameTime()+240);sound(sp,"keen_eye");return;
        }
        if(key==3 && !sneak) {reconstruct(sp,sl);return;}
        LivingEntity target=Spells.aimEnemy(p,sl,key==3?6:24);
        if(target==null)return;
        if(key==3) {
            if(!Spells.paySpirituality(p,500))return;
            Vec3 spot=Spells.freeSpotNear(p,sl,p.position(),target.position(),3);
            if(spot!=null)p.teleportTo(spot.x,spot.y,spot.z);
            boolean marked=Appraisal.isMarked(p,target);
            for(int i=0;i<(marked?4:3);i++) {
                int hit=i;
                Later.run(sl,i*3,()->{if(p.isAlive() && target.isAlive() && p.level()==sl && target.level()==sl && p.distanceToSqr(target)<64) {
                    float amount=(hit<2?12:24)*(marked?1.25f:1);
                    if(hit>=2)Spells.strikeTrue(p,target,amount);else Spells.strike(p,target,amount);
                    if(hit==3)Spells.keepEffect(target,MobEffects.SLOWNESS,4,20);
                }});
            }
            if(marked)Appraisal.spend(p,target);sound(sp,"deductive_strike");return;
        }
        if(!Spells.paySpirituality(p,key==1?8:120))return;
        mark(p,target,200,.1f);
        if(key==1) {
            String mood=Fear.isAfraid(target)?"afraid":target instanceof Mob m && m.getTarget()!=null?"furious":"calm";
            String hunting=target instanceof Mob m && m.getTarget()!=null?m.getTarget().getName().getString():"nobody";
            p.sendSystemMessage(Component.literal(target.getName().getString()+" | Health "+target.getHealth()+"/"+target.getMaxHealth()+" | "+mood+" | Hunting "+hunting));
            for(var effect:target.getActiveEffects())p.sendSystemMessage(Component.literal(effect.toString()));
            if(Beyonder.hasSequence(p,8))p.sendSystemMessage(Component.literal("Weakness: "+WEAKNESS.of(target)));
            show(sl,target,"info_card",120);sound(sp,"reading");
        } else {
            Enfeebled.apply(target,"omniscient_deduction",.25f,200);
            p.getPersistentData().putLong("omniscient_deduction",sl.getGameTime()+200);
            p.sendSystemMessage(Component.literal("Weakness: "+WEAKNESS.of(target)));sound(sp,"deduction");
        }
    }
    private static void mark(Player p,LivingEntity t,int ticks,float bonus) {
        Appraisal.mark(p,t,WEAKNESS.of(t),ticks,Math.max(bonus,Appraisal.bonus(p,t)));
        show((ServerLevel)p.level(),t,"reading_eye",ticks);
    }
    private static void ritual(ServerPlayer p,ServerLevel sl) {
        if(Countdown.isRunning(p))return;
        if(!Spells.paySpirituality(p,15))return;
        Vec3 at=p.position();float health=p.getHealth();
        Runnable finish=()->{
            double radius=Beyonder.hasSequence(p,7)?3:Beyonder.hasSequence(p,8)?2.5:2;
            int duration=Beyonder.hasSequence(p,8)?800:600;
            ParticleShapes.model(sl,model("ritual_sigil",duration).scale((float)radius).unshaded(),at,0,0,0);
            Zone.at(at,radius).lasts(duration).every(5).affects(e->true).onPulse(left->{
                List<LivingEntity> friends=new ArrayList<>(Spells.alliesAround(p,sl,at,radius));
                if(p.position().distanceTo(at)<=radius)friends.add(p);
                for(var ally:friends){
                    ConjuredArmor.of(RITE).lasts(10).cut(.15f).wear(ally);
                    if(left%100==0)Spells.cleanse(ally);
                }
                for(var shot:Spells.hostileShotsAround(p,sl,at,radius))shot.setDeltaMovement(shot.getDeltaMovement().scale(.25));
                for(var enemy:Spells.enemiesAround(p,sl,at,radius))if(Construct.isConstruct(enemy) || Marionette.isSummoned(enemy)) {
                    Vec3 dir=enemy.position().subtract(at).normalize();
                    Vec3 out=Spells.freeSpotNear(enemy,sl,enemy.position(),at.add(dir.scale(radius+1)),1);
                    if(out!=null)enemy.teleportTo(out.x,out.y,out.z);
                }
            }).open(p,sl,RITE);sound(p,"ritual_circle");
        };
        if(Beyonder.hasSequence(p,7))finish.run();
        else Countdown.seconds(Beyonder.hasSequence(p,8)?2:3).keepWhile(()->p.position().distanceToSqr(at)<.01 && p.getHealth()>=health).onFinish(finish).start(p);
    }
    private static void reconstruct(ServerPlayer p,ServerLevel sl) {
        if(!Spells.paySpirituality(p,700))return;
        var casts=Casts.near(sl,p.position(),Beyonder.hasSequence(p,4)?48:24,Beyonder.hasSequence(p,4)?2400:1200);
        int named=0;
        for(var cast:casts) {
            if(named++<3)p.sendSystemMessage(Component.literal(cast.name()+" by "+cast.casterName()+" ("+cast.age(sl)/20+"s ago)"));
            for(int tick=0;tick<120;tick+=20)Later.run(sl,tick,()->ParticleShapes.model(sl,model("ghost_outline",20).unshaded(),cast.at(),0,0,0));
        }
        for(var other:sl.players())if(other.distanceToSqr(p)<=24*24)for(int tick=10;tick<=200;tick+=10) {
            var a=History.ago(other,tick);var b=History.ago(other,tick-10);
            if(a!=null && b!=null && a.dimension().equals(sl.dimension()))ParticleShapes.line(sl,ParticleFx.of("fx/omniscient_eye/ghost_line").glow(),a.position(),b.position(),2);
        }
        sound(p,"reconstruction");
    }
    private static AbilityWheel.Result analyze(ServerPlayer p,ServerLevel sl) {
        LivingEntity t=Spells.aimEnemy(p,sl,24);if(t==null)return AbilityWheel.Result.REFUSED;
        Countdown.seconds(Beyonder.hasSequence(p,5)?1:2).silent().keepWhile(()->t.isAlive() && p.level()==sl && Spells.aimEnemy(p,sl,24)==t)
                .onFinish(()->{mark(p,t,400,.15f);p.sendSystemMessage(Component.literal(t.getName().getString()+" — Weakness: "+WEAKNESS.of(t)));sound(p,"analyze");}).start(p);
        return AbilityWheel.Result.DONE;
    }
    private static AbilityWheel.Result mirror(ServerPlayer p,ServerLevel sl) {
        LivingEntity t=Spells.aimEnemy(p,sl,24);if(t==null)return AbilityWheel.Result.REFUSED;
        ParticleShapes.modelInFront(sl,model("brass_mirror",30),p,1,.2);
        Spells.strike(p,t,Math.min(Spells.isBoss(t)?60:120,(t.getMaxHealth()-t.getHealth())*(Spells.isBoss(t)?.2f:.45f)));
        Bleeding.apply(t,p,100,3);sound(p,"mirror_curse");return AbilityWheel.Result.DONE;
    }
    private static AbilityWheel.Result vile(ServerPlayer p,ServerLevel sl) {
        LivingEntity t=Spells.aimEnemy(p,sl,24);if(t==null)return AbilityWheel.Result.REFUSED;
        Spells.keepEffect(t,MobEffects.NAUSEA,0,120);Fear.apply(t,p,120);Falter.apply(t,"omniscient_vile",.5f,120);
        if(Construct.isConstruct(t)||Marionette.isSummoned(t))Spells.strike(p,t,40);
        show(sl,t,"cognize_eye",120);sound(p,"vile_thoughts");return AbilityWheel.Result.DONE;
    }
    private static AbilityWheel.Result improvise(ServerPlayer p,ServerLevel sl) {
        LivingEntity t=Spells.aimEnemy(p,sl,28);if(t==null)return AbilityWheel.Result.REFUSED;
        String kind=WEAKNESS.of(t);
        ParticleShapes.modelFacing(sl,model("force_spear",12).color(0xFFF4EBD0),t.getEyePosition(),p.getLookAngle());
        if(kind.equals("unmaking"))Dispel.unmake(t);
        else {
            Appraisal.strike(p,t,kind.equals("pierce")?40:60,kind);
            switch(kind){
                case "frost" -> Spells.keepEffect(t,MobEffects.SLOWNESS,2,100);
                case "fire" -> Spells.keepEffect(t,MobEffects.WEAKNESS,1,100);
                case "lightning" -> {for(var e:Spells.enemiesAround(p,sl,t.position(),5).stream().limit(3).toList()){if(e!=t)Appraisal.strike(p,e,40,kind);Spells.keepEffect(e,MobEffects.SLOWNESS,5,10);}}
            }
        }
        sound(p,"improvise");return AbilityWheel.Result.DONE;
    }
    private static AbilityWheel.Result tome(ServerPlayer p,ServerLevel sl) {
        Vec3 at=Spells.blinkSpot(p,sl,2);if(at==null)return AbilityWheel.Result.REFUSED;
        Mob made=Construct.of(EntityTypes.IRON_GOLEM).health(60).damage(8).lasts(600).leash(40).limit("omniscient_tome",1)
                .onEnd((mob,why)->{if(why==Construct.End.DIED)for(var e:Spells.enemiesAround(p,sl,mob.position(),4))Spells.keepEffect(e,MobEffects.BLINDNESS,0,40);})
                .build(p,sl,at,p.getYRot());
        if(made==null)return AbilityWheel.Result.REFUSED;
        ParticleShapes.modelOnTurning(sl,model("tome_creature",600).frames(4).smooth(),made,Vec3.ZERO,0,0,0);
        ParticleShapes.modelInFront(sl,model("living_tome_open",30),p,1,.3);
        sound(p,"living_tome");return AbilityWheel.Result.DONE;
    }
    private static AbilityWheel.Result armor(ServerPlayer p,ServerLevel sl) {
        LivingEntity t=Spells.aimAlly(p,sl,16);if(t==null)t=p;
        ConjuredArmor.of("omniscient_gold").lasts(600).cut(.4f).absorption(12).steady().wear(t);
        ParticleShapes.modelOnTurning(sl,model("golden_armor",600).color(0x99FFFFFF),t,Vec3.ZERO,0,0,0);
        sound(p,"golden_armor_art");return AbilityWheel.Result.DONE;
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent.Post event) {
        if(!(event.player() instanceof ServerPlayer p) || !SoulCore.getPathway(p).equals(PATHWAY) || p.tickCount%5!=0)return;
        ServerLevel sl=p.level();long now=sl.getGameTime();
        boolean keen=p.getPersistentData().getLongOr("omniscient_keen",0)>now;
        boolean deduction=p.getPersistentData().getLongOr("omniscient_deduction",0)>now;
        if(!keen && !deduction)return;
        var line=ParticleFx.of("fx/omniscient_eye/ghost_line").glow().lifetime(6);
        for(var shot:Spells.hostileShotsAround(p,sl,p.position(),24))ParticleShapes.line(sl,line,shot.position(),shot.position().add(shot.getDeltaMovement().scale(20)),2);
        if(keen) {
            for(Vec3 at:Trap.hiddenNear(sl,p.position(),24))ParticleShapes.model(sl,model("reading_eye",6),at,0,0,0);
            for(var target:Spells.livingAround(p,sl,p.position(),24)) {
                Vec3 delta=target.getEyePosition().subtract(p.getEyePosition());
                if(!Spells.isFriend(p,target) && p.hasLineOfSight(target) && delta.lengthSqr()>0
                        && p.getLookAngle().dot(delta.normalize())>.995)Exposed.apply(target,60);
            }
        }
    }
}
