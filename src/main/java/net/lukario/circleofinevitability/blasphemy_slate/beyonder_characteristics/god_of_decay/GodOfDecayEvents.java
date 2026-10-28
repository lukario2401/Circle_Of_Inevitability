package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay.GodOfDecayFx.Ability.*;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay.GodOfDecayMarks.Kind;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay.GodOfDecayPowers.*;

@Mod.EventBusSubscriber(modid=CircleOfInevitability.MOD_ID)
public final class GodOfDecayEvents {
    private static boolean initialized,redirecting;
    private GodOfDecayEvents(){}
    public static void initialize(){
        if(initialized)return;initialized=true;
        Support.addBlocker((provider,receiver)->GodOfDecayMarks.has(receiver,Kind.CORRUPTED)&&Spells.isFriend(provider,receiver));
        Cleansing.addListener(GodOfDecayMarks::cleanse);
        Dispel.addRule(GodOfDecayMarks::cleanse);
        Weathering.addAdapter(GodOfDecayMarks::age);
        ProjectileTempo.addRule(shot->{
            if(!(shot.level() instanceof ServerLevel sl)||!(shot.getOwner() instanceof LivingEntity owner))return 1;
            double factor=1;
            for(ServerPlayer p:sl.players())if(Spells.isEnemy(p,owner)){
                double distance=p.distanceToSqr(shot);
                if(active(p,SLOW_TIME)&&distance<=24*24)factor=Math.min(factor,.5);
                if(GodOfDecay.is(p,2)&&distance<=144)factor=Math.min(factor,.7);
                if(GodOfDecay.is(p,3)&&distance<=16){factor=Math.min(factor,.7);shot.getPersistentData().putBoolean("god_decay_weathered",true);}
            }
            return factor;
        });
        Decay.addImmunity(e->GodOfDecay.is(e,4));
        Spells.addMoveImmunity(e->GodOfDecayMarks.has(e,Kind.OBESE)||active(e,WITHERING_PALM));
        Spells.addUnchoosableCheck((viewer,target)->active(target,PATHOGEN_FORM)||active(target,VIRUS_INCARNATION));
        Spells.addCostScale(p->{
            float result=(1+.5f*GodOfDecayMarks.strength(p,Kind.DEGENERATE));
            if(active(p,OFFICIAL_CAPACITY)&&protectedByCapacity(p))result*=.8f;
            float entropy=0;
            for(State s:domains(p,ENTROPY))entropy=Math.max(entropy,Math.min(1,s.counts.getOrDefault(p.getUUID(),0)*.05f));
            return result*(1+entropy);
        });
        HeldActions.addRule(e->{
            boolean tape=false;
            for(var m:GodOfDecayMarks.all(e))if(m.kind()==Kind.RED_TAPE&&GodOfDecayMarks.alive(e,m.data())&&!m.data().getBooleanOr("held",false)){m.data().putBoolean("held",true);tape=true;}
            return tape||GodOfDecayMarks.has(e,Kind.SEGMENTED)?40:GodOfDecayMarks.has(e,Kind.LAZY)?20:0;
        });
        Casts.addListener((p,sl,cast)->{
            if(!GodOfDecay.is(p,4)||!hasDoubles(p)||cast.wheel()!=GodOfDecay.DISEASE&&cast.wheel()!=GodOfDecay.DOOM)return;
            List<net.minecraft.world.phys.Vec3> positions=Construct.ofOwner(p,DOUBLE_GROUP).stream().map(Entity::position).toList();
            Echo.repeat(p,cast,positions,.5f,null);
        });
    }
    static boolean protectedByCapacity(LivingEntity target){
        for(var states:STATES.values()) {
            State s=states.get(OFFICIAL_CAPACITY);
            if(s==null||s.level!=target.level()||s.until<=s.level.getGameTime()||!s.owner.isAlive()||s.owner.distanceToSqr(target)>256||!Spells.isFriend(s.owner,target))continue;
            if(Spells.livingAround(s.owner,s.level,s.owner.position(),16).stream().anyMatch(e->e!=s.owner&&Spells.isFriend(s.owner,e)))return true;
        }
        return false;
    }
    @SubscribeEvent(priority=Priority.HIGHEST)
    public static boolean attack(LivingAttackEvent event){
        LivingEntity victim=event.getEntity();if(!(victim.level() instanceof ServerLevel sl))return false;
        var source=event.getSource();if(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))return false;
        if(inEnemyDomain(victim,ENTROPY)){Substitute.disarm(victim);DeathWard.disarm(victim);}
        if(active(victim,PATHOGEN_FORM)&&!Spells.isPiercing(victim)) {
            // Physical contact, arrows and falls are stopped; spell/purifying/magic sources remain dangerous.
            boolean physical=!Spells.isSpellHit(victim)&&(source.is(DamageTypeTags.IS_PROJECTILE)||source.is(DamageTypeTags.IS_FALL)||source.getDirectEntity() instanceof LivingEntity);
            if(physical)return true;
        }
        if(!(source.getEntity() instanceof LivingEntity dealer)||HeldActions.replaying(dealer))return false;
        // Segmentation holds every blow. Red Tape holds only the first ranged attack or ability.
        boolean ranged=source.is(DamageTypeTags.IS_PROJECTILE)||Spells.isSpellHit(victim);
        int delay=0;
        if(GodOfDecayMarks.has(dealer,Kind.SEGMENTED))delay=40;
        else if(ranged)for(var m:GodOfDecayMarks.all(dealer))if(m.kind()==Kind.RED_TAPE&&GodOfDecayMarks.alive(dealer,m.data())&&!m.data().getBooleanOr("held",false)){m.data().putBoolean("held",true);delay=40;break;}
        if(delay>0){
            float damage=event.getAmount();double range=ranged?Math.max(7,dealer.distanceTo(victim)+2):7;
            GodOfDecayFx.heldBlow(sl,dealer,false);
            Later.run(sl,delay,()->{
                if(!Spells.casterStillHere(dealer,sl)||!victim.isAlive()||victim.level()!=sl||dealer.distanceToSqr(victim)>range*range)return;
                HeldActions.replay(dealer,()->{victim.invulnerableTime=0;victim.hurt(source,damage);});GodOfDecayFx.heldBlow(sl,victim,true);
            });return true;
        }
        if(!redirecting&&GodOfDecayMarks.has(dealer,Kind.CORRUPTED)&&sl.getRandom().nextFloat()<.25f){
            LivingEntity ally=Spells.livingAround(dealer,sl,dealer.position(),16).stream().filter(e->e!=dealer&&e!=victim&&Spells.isFriend(dealer,e))
                    .min(Comparator.comparingDouble(dealer::distanceToSqr)).orElse(null);
            if(ally!=null){redirecting=true;try{ally.invulnerableTime=0;ally.hurt(source,event.getAmount());}finally{redirecting=false;}return true;}
        }
        return false;
    }
    @SubscribeEvent
    public static void hurt(LivingHurtEvent event){
        LivingEntity victim=event.getEntity();if(!(victim.level() instanceof ServerLevel sl))return;
        if(event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        LivingEntity dealer=event.getSource().getEntity() instanceof LivingEntity e?e:null;
        float amount=event.getAmount();
        if(dealer!=null){
            amount*=1+Math.min(.30f,.06f*GodOfDecayMarks.count(dealer,victim));
            amount*=Math.max(0,1-.25f*GodOfDecayMarks.strength(dealer,Kind.DEGENERATE));
            if(event.getSource().getDirectEntity() instanceof Projectile shot && (shot.getPersistentData().getBooleanOr("god_decay_weathered",false)||GodOfDecay.is(victim,3)))amount*=.7f;
            if(active(victim,WITHERING_PALM)&&dealer.position().subtract(victim.position()).normalize().dot(Spells.flatLook(victim))>.3)
                amount*=Spells.isSpellHit(victim)||event.getSource().is(DamageTypeTags.IS_PROJECTILE)?.3f:.6f;
            boolean melee=event.getSource().getDirectEntity()==dealer&&!Spells.isSpellHit(victim);
            if(melee&&Spells.isEnemy(dealer,victim)){
                if(active(dealer,CARRIERS_TOUCH)) {
                    GodOfDecayMarks.put(dealer,victim,Kind.INFECTED,160);GodOfDecayFx.infectedHit(sl,victim);
                    Spells.enemiesAround(dealer,sl,victim.position(),3).stream().filter(e->e!=victim).min(Comparator.comparingDouble(victim::distanceToSqr))
                            .ifPresent(e->{GodOfDecayMarks.put(dealer,e,Kind.INFECTED,160);GodOfDecayFx.contagion(sl,victim,e);});
                }
                if(GodOfDecay.is(victim,9)&&sl.getRandom().nextInt(5)==0)GodOfDecayMarks.put(victim,dealer,Kind.INFECTED,160);
            }
        }
        float obese=GodOfDecayMarks.strength(victim,Kind.OBESE);
        amount*=1+.15f*obese;
        if(obese>0&&event.getSource().is(DamageTypeTags.IS_FALL))amount*=2;
        if(protectedByCapacity(victim))amount*=.8f;
        if(active(victim,VIRUS_INCARNATION))amount*=.3f;
        event.setAmount(amount);
        if(amount>=20&&GodOfDecayMarks.has(victim,Kind.HOLLOWED)) {
            for(var m:GodOfDecayMarks.all(victim))if(m.kind()==Kind.HOLLOWED&&GodOfDecayMarks.alive(victim,m.data())) {
                LivingEntity caster=GodOfDecayMarks.owner(sl,m);GodOfDecayMarks.remove(victim,m);
                if(caster!=null)Later.run(sl,1,()->{if(victim.isAlive()&&victim.level()==sl&&Spells.casterStillHere(caster,sl)){GodOfDecayFx.collapse(sl,victim);Spells.strikeTrue(caster,victim,victim.getMaxHealth()*.25f);}});
            }
            GodOfDecayMarks.refreshBody(victim);
        }
    }
    @SubscribeEvent(priority=Priority.LOWEST)
    public static void damage(LivingDamageEvent event){
        LivingEntity e=event.getEntity();if(e.level().isClientSide()||event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        if(active(e,VIRUS_INCARNATION)&&e.getHealth()-event.getAmount()<e.getMaxHealth()*.2f){
            event.setAmount(Math.max(0,e.getHealth()-e.getMaxHealth()*.2f));end(e,VIRUS_INCARNATION);
        }
    }
    @SubscribeEvent(priority=Priority.HIGH)
    public static boolean deathSave(LivingDeathEvent event){
        LivingEntity e=event.getEntity();if(e.level().isClientSide()||Spells.isPiercing(e)||event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)||inEnemyDomain(e,ENTROPY))return false;
        return !Decree.violate(e,Decree.Rule.RESURRECTION)&&saveByDouble(e);
    }
    @SubscribeEvent(priority=Priority.LOWEST)
    public static void death(LivingDeathEvent event){
        LivingEntity e=event.getEntity();if(!(e.level() instanceof ServerLevel sl))return;
        Set<UUID> healed=new HashSet<>();
        for(var m:GodOfDecayMarks.all(e)){
            LivingEntity owner=GodOfDecayMarks.owner(sl,m);if(owner==null||!GodOfDecayMarks.alive(e,m.data()))continue;
            if(m.kind()==Kind.ROTTING)for(LivingEntity t:Spells.enemiesAround(owner,sl,e.position(),5))if(t!=e)GodOfDecayMarks.put(owner,t,Kind.ROTTING,Math.max(1,(int)(m.data().getLongOr("until",sl.getGameTime())-sl.getGameTime())));
            if(m.kind().sickness&&active(owner,VIRUS_INCARNATION)&&event.getSource().getEntity()==owner&&healed.add(owner.getUUID()))owner.heal(owner.getMaxHealth()*.1f);
        }
        clear(e);GodOfDecayMarks.clear(e);
    }
    @SubscribeEvent
    public static boolean heal(LivingHealEvent event){
        LivingEntity e=event.getEntity();if(e.level().isClientSide())return false;
        if(GodOfDecayMarks.has(e,Kind.ROTTING)||GodOfDecayMarks.has(e,Kind.DOOMED)||inEnemyDomain(e,ENTROPY))return true;
        if(e.level() instanceof ServerLevel sl)for(ServerPlayer p:sl.players())if(GodOfDecay.is(p,1)&&active(p,SWOLLEN_FORM)&&Spells.isEnemy(p,e)&&p.distanceToSqr(e)<=400)return true;
        return false;
    }
    @SubscribeEvent(priority=Priority.LOWEST)
    public static boolean sureSwing(AttackEntityEvent event) {
        Player p=event.getEntity();
        if (!(p instanceof ServerPlayer player) || !(event.getTarget() instanceof LivingEntity target) || !active(p,SURE_HIT)
                || !Spells.isEnemy(p,target) || p.distanceToSqr(target)>49 || Root.has(p,RootRestriction.ATTACK)) return false;
        State s=state(p,SURE_HIT);
        float charge=p.getAttackStrengthScale(.5f);
        float damage=(float)p.getAttributeValue(Attributes.ATTACK_DAMAGE)*(.2f+.8f*charge*charge);
        GodOfDecayFx.sureHitSwing(player.level(),p,target);
        CertainDamage.strike(p,target,damage,DeathWard.snapshot(target));
        p.resetAttackStrengthTicker();
        if(--s.charges<=0)end(p,SURE_HIT);
        return true;
    }

    @SubscribeEvent
    public static boolean knockback(LivingKnockBackEvent event){return !event.getEntity().level().isClientSide()&&(GodOfDecayMarks.has(event.getEntity(),Kind.OBESE)||active(event.getEntity(),WITHERING_PALM));}
    @SubscribeEvent
    public static void effects(MobEffectEvent.Applicable event){
        LivingEntity e=event.getEntity();if(e.level().isClientSide())return;
        var effect=event.getEffectInstance().getEffect();
        if(GodOfDecay.is(e,9)&&(effect==MobEffects.POISON||effect==MobEffects.NAUSEA||effect==MobEffects.HUNGER)
                ||GodOfDecay.is(e,4)&&effect==MobEffects.WITHER)event.setResult(Result.DENY);
    }
    @SubscribeEvent
    public static void addedEffect(MobEffectEvent.Added event) {
        LivingEntity target=event.getEntity();
        if (!(target.level() instanceof ServerLevel sl) || !event.getEffectInstance().getEffect().value().isBeneficial()
                || !(event.getEffectSource() instanceof LivingEntity provider) || !Support.blocked(provider,target)) return;
        var effect=event.getEffectInstance().getEffect();
        var old=event.getOldEffectInstance()==null?null:new net.minecraft.world.effect.MobEffectInstance(event.getOldEffectInstance());
        Later.run(sl,1,()->{
            if(!target.isAlive() || target.level()!=sl)return;
            target.removeEffect(effect);
            if(old!=null)target.addEffect(old);
        });
    }

    @SubscribeEvent
    public static void trade(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Villager villager) || !(villager.level() instanceof ServerLevel sl)
                || !(villager.getTradingPlayer() instanceof ServerPlayer player) || !GodOfDecay.is(player,8)) return;
        boolean changed=false;
        for(var offer:villager.getOffers())if(offer.getCostA().getCount()>1){
            offer.addToSpecialPriceDiff(1-offer.getCostA().getCount());changed=true;
        }
        // Vanilla resets special prices when trading ends, so other customers keep their own prices.
        if(changed)player.sendMerchantOffers(player.containerMenu.containerId,villager.getOffers(),villager.getVillagerData().level(),villager.getVillagerXp(),villager.showProgressBar(),villager.canRestock());
    }

    @SubscribeEvent
    public static boolean target(LivingChangeTargetEvent event){
        LivingEntity target=event.getNewTarget();
        return target!=null && (GodOfDecay.is(target,8)&&event.getEntity() instanceof AbstractPiglin
                || active(target,PATHOGEN_FORM)||active(target,VIRUS_INCARNATION)||GodOfDecayMarks.has(event.getEntity(),Kind.LAZY)||GodOfDecayMarks.has(event.getEntity(),Kind.SEGMENTED));
    }
    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event){
        LivingEntity e=event.getEntity();if(!(e.level() instanceof ServerLevel sl))return;
        if(e.getPersistentData().contains(GodOfDecayMarks.DATA))GodOfDecayMarks.tick(e,sl);
        else if(e.tickCount%20==0)GodOfDecayMarks.refreshBody(e);
        long now=sl.getGameTime();
        boolean slowed=e.getPersistentData().getLongOr("decay_slowed",0L)>now,quick=e.getPersistentData().getLongOr("decay_quickened",0L)>now;
        Physique.keep(e,Attributes.ATTACK_SPEED,Physique.id("god_decay_time_speed"),slowed?-.4:.2,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,slowed||quick);
        if(!(e instanceof ServerPlayer p))return;
        GodOfDecayPowers.tick(p,sl);
        Physique.keep(p,Attributes.MAX_HEALTH,Physique.id("god_decay_constitution"),4,AttributeModifier.Operation.ADD_VALUE,GodOfDecay.is(p,9));
        Physique.keep(p,Attributes.ENTITY_INTERACTION_RANGE,Physique.id("god_decay_sure_reach"),Math.max(0,7-p.getAttribute(Attributes.ENTITY_INTERACTION_RANGE).getBaseValue()),AttributeModifier.Operation.ADD_VALUE,active(p,SURE_HIT));
        if(!GodOfDecay.is(p,9))return;
        if(GodOfDecay.is(p,2)&&p.isFullyFrozen())p.setTicksFrozen(Math.max(0,p.getTicksFrozen()-2));
        if(p.tickCount%20!=0)return;
        int diseased=0;
        for(LivingEntity t:Spells.enemiesAround(p,sl,p.position(),32)) {
            int count=GodOfDecayMarks.count(p,t);if(count>0&&p.distanceToSqr(t)<=144)diseased++;
            if(GodOfDecay.is(p,6)&&p.distanceToSqr(t)<=32*32)GodOfDecayFx.sickness(p,t,count);
            if(GodOfDecay.is(p,4)&&p.distanceToSqr(t)<=64&&!inEnemyDomain(t,DECAY)){Weathering.effects(t,20,true);t.clearFire();}
            if(active(p,SWOLLEN_FORM)&&GodOfDecay.is(p,2)&&p.distanceToSqr(t)<=400)t.getPersistentData().putLong("decay_giant_drag",now+25);
            if(GodOfDecay.is(p,1)&&p.distanceToSqr(t)<=256){
                float depth=Math.min(active(p,SWOLLEN_FORM)?.4f:.2f,t.getPersistentData().getFloatOr("decay_passive_age",0)+.01f);
                t.getPersistentData().putFloat("decay_passive_age",depth);t.getPersistentData().putLong("decay_passive_until",now+25);
            }
        }
        if(GodOfDecay.is(p,7)&&diseased>0)Spells.keepEffect(p,MobEffects.REGENERATION,diseased>=3?1:0,25);
        if(GodOfDecay.is(p,5)) {
            for(var effect:new ArrayList<>(p.getActiveEffects()))if(!effect.getEffect().value().isBeneficial()&&!effect.isInfiniteDuration()) {
                // 30% shorter remaining duration is applied on acquisition, not compounded every second.
                String key="decay_effect_seen_"+net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
                long expires=now+effect.getDuration();long seen=p.getPersistentData().getLongOr(key,0L);
                if(expires>seen+5){int ticks=Math.max(1,Math.round(effect.getDuration()*(GodOfDecay.is(p,2)&&effect.getEffect()==MobEffects.SLOWNESS?.5f:.7f)));p.removeEffect(effect.getEffect());p.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect.getEffect(),ticks,effect.getAmplifier(),effect.isAmbient(),effect.isVisible(),effect.showIcon()));p.getPersistentData().putLong(key,now+ticks);}
            }
        }
    }
    @SubscribeEvent
    public static void ageBody(LivingEvent.LivingTickEvent event){
        LivingEntity e=event.getEntity();if(e.level().isClientSide())return;
        boolean aged=e.getPersistentData().getLongOr("decay_passive_until",0L)>e.level().getGameTime();
        Physique.keep(e,Attributes.MAX_HEALTH,Physique.id("god_decay_passive_age"),-e.getPersistentData().getFloatOr("decay_passive_age",0),AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,aged);
        if(!aged)e.getPersistentData().remove("decay_passive_age");
        boolean drag=e.getPersistentData().getLongOr("decay_giant_drag",0L)>e.level().getGameTime();
        Physique.keep(e,Attributes.MOVEMENT_SPEED,Physique.id("god_decay_giant_drag"),-.15,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,drag);
    }
    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event){clear(event.getEntity());GodOfDecayMarks.clear(event.getEntity());}
    @SubscribeEvent
    public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){clear(event.getEntity());GodOfDecayMarks.clear(event.getEntity());}
    @SubscribeEvent
    public static void unload(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel sl)for(var states:List.copyOf(STATES.values()))for(State s:List.copyOf(states.values()))if(s.level==sl)end(s.owner,s.ability,false);}
}
