package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker;

import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker.NatureWalker.*;

@Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class NatureWalkerPassives {
    static void tick(State s) {
        long now=s.level.getGameTime();
        if(now%20==0)Spells.keepEffect(s.owner,MobEffects.STRENGTH,0,30);
        if(now%100==0)Spells.growPlants(s.owner,s.level,s.owner.position(),6,2);
        if(s.sequence<=5&&now%60==0) {
            var ground=Terrain.under(s.owner);
            if(ground==Terrain.Kind.SOIL||ground==Terrain.Kind.STONE||ground==Terrain.Kind.SAND)Healing.apply(s.owner,1);
        }
        if(!(s.owner instanceof ServerPlayer p))return;
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
        if(source.is(DamageTypes.CACTUS)||source.is(DamageTypes.SWEET_BERRY_BUSH)||source.is(DamageTypes.THORNS))event.setAmount(0);
    }
}
