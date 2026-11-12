package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker;

import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.tags.DamageTypeTags;
import java.util.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker.NatureWalker.*;

@Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class NatureWalkerPassives {
    static final ThreadLocal<Mutation.Kind> HIT_KIND=ThreadLocal.withInitial(()->Mutation.Kind.ORGANS);
    private static boolean reacting;
    static void tick(State s) {
        long now=s.level.getGameTime();
        if(now%20==0)Spells.keepEffect(s.owner,MobEffects.STRENGTH,0,30);
        if(now%100==0)Spells.growPlants(s.owner,s.level,s.owner.position(),6,2);
        if(s.sequence<=5&&now%60==0) {
            var ground=Terrain.under(s.owner);
            if(ground==Terrain.Kind.SOIL||ground==Terrain.Kind.STONE||ground==Terrain.Kind.SAND)Healing.apply(s.owner,s.owner,1);
        }
        if(s.sequence<=2&&now%20==0) {
            for(var ally:friends(s,s.owner.position(),16))Spells.keepEffect(ally,MobEffects.REGENERATION,0,30);
            for(var foe:enemies(s,s.owner.position(),16)) { NoHeal.apply(foe,30);Decay.apply(foe,30,.05f); }
        }
        if(s.sequence<=1) { s.owner.setAirSupply(s.owner.getMaxAirSupply());s.owner.clearFire();FallGuard.protect(s.owner,30); }
        if(s.sequence==0) {
            if(now%20==0)Spells.growPlants(s.owner,s.level,s.owner.position(),24,6);
            if(now%80==0)for(var plant:s.plants)plant.clock.advance(40);
            if(now%100==0) { var foes=enemies(s,s.owner.position(),12);if(!foes.isEmpty())Mutation.apply(s.owner,foes.getFirst(),Mutation.Kind.ORGANS,1,240); }
        }
        if(!(s.owner instanceof ServerPlayer p))return;
        if(s.sequence<=2)p.getFoodData().setFoodLevel(20);
        else if(s.sequence<=5) {
            int food=p.getFoodData().getFoodLevel();
            if(s.lastFood>=0&&food<s.lastFood) {
                s.foodCredit+=(s.lastFood-food)*.5;
                int refund=(int)s.foodCredit;s.foodCredit-=refund;p.getFoodData().setFoodLevel(Math.min(20,food+refund));
            }
        }
        s.lastFood=p.getFoodData().getFoodLevel();
        if(s.sequence<=3&&now%40==0)for(var body:Fallen.near(s.level,p.position(),48,400))
            ParticleShapes.spawnFor(p,NatureWalkerFx.model("grave_mark",45,tag(s,"grave_"+body.id())).scale(.5f),body.where(),Vec3.ZERO);
        if(now%100==0&&s.level.canHaveWeather())
            HudTimer.show(p,PREFIX+"weather",s.level.isRaining()?"Rain ends":"Rain arrives",Math.max(s.level.getWeatherData().getClearWeatherTime(),s.level.getWeatherData().getRainTime()),0x85D3BF);
        if(s.sequence<=8&&now%10==0)for(var e:friends(s,p.position(),24)) {
            if(e.getHealth()<e.getMaxHealth()*.5f||NoHeal.isApplied(e)||Decay.isDecaying(e)||Madness.get(e)>0)
                ParticleShapes.spawnFor(p,NatureWalkerFx.particle("healing_spark",NatureWalkerFx.SPIRIT),e.position().add(0,e.getBbHeight()+.3,0),Vec3.ZERO);
        }
        if(s.sequence<=6&&now%40==0)for(var e:Spells.livingAround(p,s.level,p.position(),16))
            if(Spells.canBeChosen(p,e))EntitySight.show(p,e,45);
        if(s.sequence<=8&&p.isShiftKeyDown()) {
            var e=Spells.aimLiving(p,s.level,24,false,t->Spells.canBeChosen(p,t));
            if(e!=null) {
                if(!e.getUUID().equals(s.observed)) { s.observed=e.getUUID();s.observedTicks=0; }
                if(++s.observedTicks>=20&&s.dossierReady.getOrDefault(e.getUUID(),0L)<=now) {
                    Dossier.tell(p,e,s.sequence<=6?2:1);s.dossierReady.put(e.getUUID(),now+100);s.observedTicks=0;
                }
            } else { s.observed=null;s.observedTicks=0; }
        } else { s.observed=null;s.observedTicks=0; }
        if(now%100==0)s.dossierReady.values().removeIf(until->until<=now);
    }
    @SubscribeEvent public static void effect(MobEffectEvent.Applicable event) {
        var e=event.getEntity();if(e.level().isClientSide()||!own(e))return;
        var type=event.getEffectInstance().getEffect();
        if(sequence(e)<=8&&type.equals(MobEffects.NAUSEA)||sequence(e)<=6&&type.equals(MobEffects.POISON))event.setResult(Result.DENY);
    }
    @SubscribeEvent public static void hurt(LivingHurtEvent event) {
        var e=event.getEntity();if(e.level().isClientSide()||!own(e)||sequence(e)>7)return;
        var source=event.getSource();
        if(sequence(e)<=1&&(source.is(DamageTypeTags.IS_FIRE)||source.is(DamageTypes.LIGHTNING_BOLT)||source.is(DamageTypes.FALL)))event.setAmount(0);
        if(source.is(DamageTypes.CACTUS)||source.is(DamageTypes.SWEET_BERRY_BUSH)||source.is(DamageTypes.THORNS))event.setAmount(0);
    }
    @SubscribeEvent public static void damaged(LivingDamageEvent event) {
        if(reacting||event.getEntity().level().isClientSide()||event.getAmount()<=0)return;
        var victim=event.getEntity();var attacker=event.getSource().getEntity();
        if(!(attacker instanceof LivingEntity source))return;
        LivingEntity owner=source;
        if(!own(owner)) {
            UUID id=Construct.ownerOf(source);if(id!=null&&source.level() instanceof net.minecraft.server.level.ServerLevel sl&&sl.getEntity(id) instanceof LivingEntity found)owner=found;
        }
        if(!own(owner)||!enemy(owner,victim))return;
        State s=state(owner);reacting=true;
        try {
            if(s.sequence<=4) { Mutation.hit(owner,victim,HIT_KIND.get(),1);Madness.add(owner,victim,1); }
            if(s.sequence<=3&&owner==source) {
                Healing.apply(owner,owner,event.getAmount()*.12f);Decay.apply(victim,60,.02f);
                NatureWalkerAdvanced.trial(s,victim,.2f,true);
                if(s.sequence<=2)Madness.add(owner,victim,2);
            }
        } finally { reacting=false; }
    }
    @SubscribeEvent public static void heldDamage(LivingHurtEvent event) {
        for(var s:STATES.values())if(valid(s)) {
            var hold=s.advanced.holds.get(event.getEntity().getUUID());
            if(hold!=null&&hold.until()>s.level.getGameTime()) { event.setAmount(event.getAmount()*1.25f);break; }
        }
    }
    @SubscribeEvent public static void died(LivingDeathEvent event) {
        var dead=event.getEntity();if(dead.level().isClientSide())return;
        for(var s:new ArrayList<>(STATES.values()))if(valid(s)&&!Spells.isBoss(dead)&&!Marionette.isSummoned(dead)
                &&!(dead instanceof Player)&&NatureWalkerAdvanced.inKingdom(s,dead.position())&&!Spells.isFriend(s.owner,dead)&&dead!=s.owner)
            Later.run(s.level,1,()->{if(valid(s)&&!dead.isAlive())NatureWalkerBrood.spirit(s,dead.position(),Math.min(120,dead.getMaxHealth()*.7f));});
    }
}
