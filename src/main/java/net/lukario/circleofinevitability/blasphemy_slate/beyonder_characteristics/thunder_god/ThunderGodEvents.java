package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god.ThunderGod.*;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god.ThunderGodPowers.*;

@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID)
public final class ThunderGodEvents {
    private static boolean initialized;
    private ThunderGodEvents() {}
    public static void initialize() {
        if(initialized)return;initialized=true;
        Fear.addDurationScale(e->is(e,9)?.5:1);
        Fear.addImmunity(e->e instanceof Player p&&Form.isIn(p,WRATH));
        Cleansing.addListener(ThunderGodPowers::cleanse);
        Root.addHold((e,restriction)->
                afflicted(e,Affliction.VEIL)&&(restriction==RootRestriction.ABILITIES||restriction==RootRestriction.ATTACK&&e instanceof Mob)
                ||afflicted(e,Affliction.SPHERE)&&RootRestriction.MOVEMENT.contains(restriction));
        Spells.addUnchoosableCheck((chooser,target)->chooser!=null&&afflicted(chooser,Affliction.VEIL));
        Soaked.addWetCheck(ThunderGodPowers::wet);
    }
    /** Attribute passives run for everyone, allowing a pathway or Sequence loss to remove them. */
    public static void body(Player p) {
        Physique.keep(p,Attributes.MAX_HEALTH,Physique.id(PREFIX+"hearts"),4,AttributeModifier.Operation.ADD_VALUE,is(p,8));
        Physique.keep(p,Attributes.WATER_MOVEMENT_EFFICIENCY,Physique.id(PREFIX+"swim"),1,AttributeModifier.Operation.ADD_VALUE,is(p,9));
    }
    public static void seaFavor(Player p) {
        if(!(p.level() instanceof ServerLevel sl))return;
        if(is(p,7)) {
            if(Soaked.isWet(p))Spells.giveSpirituality(p,Beyonder.spiritualityRegenPerTick(SoulCore.getSequence(p))*20*.25f);
            Vec3 rain=nearestRain(p);
            String weather=p.level().isRainingAt(p.blockPosition())?"rain here":rain==null?"clear":String.format(java.util.Locale.ROOT,"rain %.0fm at %d, %d",p.position().distanceTo(rain),(int)rain.x,(int)rain.z);
            p.sendOverlayMessage(Component.literal("Navigation: "+p.getBlockX()+", "+p.getBlockY()+", "+p.getBlockZ()+" · "+weather));
        }
        if(is(p,5))for(Mob animal:sl.getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(12),ThunderGodEvents::aquaticAnimal)) {
            if(animal.distanceToSqr(p)>9)animal.getNavigation().moveTo(p,1);
        }
    }
    private static boolean aquaticAnimal(Mob mob) {
        var type=mob.getType();return type==EntityTypes.DOLPHIN||type==EntityTypes.COD||type==EntityTypes.SALMON||type==EntityTypes.PUFFERFISH
                ||type==EntityTypes.TROPICAL_FISH||type==EntityTypes.SQUID||type==EntityTypes.GLOW_SQUID||type==EntityTypes.TURTLE;
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent.Post event) {
        Player p=event.player();if(!(p.level() instanceof ServerLevel sl))return;
        initialize();boolean mine=is(p,9);
        if(p.getPersistentData().getBooleanOr(PREFIX+"was_mine",false)&&!mine)clear(p);
        p.getPersistentData().putBoolean(PREFIX+"was_mine",mine);
        if(p.tickCount%20==0)body(p);
        if(p.isAlive()&&ConjuredArmor.isWorn(p,SCALES))scalesTick(p,sl);
        if(!mine||!p.isAlive())return;
        galeTick(p);
        if(p.tickCount%20==0){if(is(p,8))ThunderGodFx.rage(p,Rage.count(p));else Rage.clear(p);}
        if(Rage.count(p)==10||Form.isIn(p,WRATH)) {
            if(p.tickCount%4==0) {
                Vec3 front=p.getLookAngle().multiply(1,0,1).normalize().scale(.28);
                Vec3 side=new Vec3(front.z,0,-front.x).normalize().scale(.1);
                var ember=net.lukario.circleofinevitability.particles.fx.ParticleFx.of("fx/thunder_god/rage_ember").color(0xFFC0392B).size(.04f).lifetime(4,0).glow();
                net.lukario.circleofinevitability.particles.fx.ParticleShapes.line(sl,ember,p.getEyePosition().add(front).subtract(side),p.getEyePosition().add(front).add(side),15);
            }
        }
    }
    @SubscribeEvent public static void levelTick(TickEvent.LevelTickEvent.Post event){
        if(event.level() instanceof ServerLevel sl&&sl.getGameTime()%20==0)prune(sl);
    }
    @SubscribeEvent public static void livingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity target=event.getEntity();if(!(target.level() instanceof ServerLevel sl))return;
        tickTarget(target,sl);slowInCurtain(target);
    }
    @SubscribeEvent public static void hurt(LivingHurtEvent event) {
        LivingEntity target=event.getEntity();if(!(target.level() instanceof ServerLevel sl))return;
        float amount=event.getAmount();
        if(event.getSource().getEntity() instanceof LivingEntity attacker&&is(attacker,9)) {
            amount*=1+.02f*(is(attacker,8)?Rage.count(attacker):0);
            if(is(attacker,7)&&Soaked.isWet(attacker))amount*=1.1f;
        }
        if(target instanceof Player p) {
            if(ConjuredArmor.isWorn(p,SCALES)){
                float cut=.2f+.02f*Math.max(0,9-SoulCore.getSequence(p))+(Soaked.isWet(p)?.15f:0);
                amount*=1-cut;
            }
            if(Form.isIn(p,WRATH))amount*=.85f;
            if(is(p,7)&&Soaked.isWet(p))amount*=.9f;
        }
        if((event.getSource().is(DamageTypeTags.IS_FIRE)||event.getSource().is(DamageTypeTags.IS_EXPLOSION))&&inCurtain(target))amount*=.5f;
        event.setAmount(amount);
        if(amount>0&&is(target,8)&&sl.getGameTime()>=target.getPersistentData().getLongOr(PREFIX+"hit_ready",0L)){
            target.getPersistentData().putLong(PREFIX+"hit_ready",sl.getGameTime()+10);Rage.add(target,1);
        }
    }
    @SubscribeEvent(priority=Priority.LOWEST) public static void damage(LivingDamageEvent event) {
        LivingEntity target=event.getEntity();if(!(target.level() instanceof ServerLevel sl)||event.getAmount()<=0)return;
        if(event.getSource().getEntity() instanceof LivingEntity attacker&&is(attacker,8)){
            boolean spell=Spells.isSpellHit(target),melee=event.getSource().getDirectEntity()==attacker&&!spell;
            Rage.landed(attacker,target,spell,melee);
            if(melee&&attacker instanceof Player p)meleeArc(p,target,sl);
        }
    }
    @SubscribeEvent public static void drink(LivingEntityUseItemEvent.Finish event){
        if(!event.getEntity().level().isClientSide()&&is(event.getEntity(),8)&&(event.getItem().is(Items.POTION)||event.getItem().is(Items.HONEY_BOTTLE)))Rage.drink(event.getEntity());
    }
    @SubscribeEvent public static void fall(LivingFallEvent event){if(!event.getEntity().level().isClientSide()&&is(event.getEntity(),6))event.setDamageMultiplier(event.getDamageMultiplier()*.5f);}
    @SubscribeEvent public static boolean aquaticTarget(LivingChangeTargetEvent event){
        if(event.getEntity().level().isClientSide()||!(event.getNewTarget() instanceof Player p)||!is(p,5))return false;
        var type=event.getEntity().getType();return (type==EntityTypes.GUARDIAN||type==EntityTypes.ELDER_GUARDIAN||type==EntityTypes.DROWNED)&&p.getRandom().nextBoolean();
    }
    @SubscribeEvent public static void death(LivingDeathEvent event){
        if(event.getEntity().level().isClientSide())return;
        cleanse(event.getEntity());if(event.getEntity() instanceof Player p&&is(p,9)){release(p);Rage.clear(p);}
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){
        if(!event.getEntity().level().isClientSide()&&is(event.getEntity(),9))release(event.getEntity());
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){
        if(!event.getEntity().level().isClientSide()&&is(event.getEntity(),9))release(event.getEntity());
    }
}
