package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.filthy_monarch;

import java.util.*;
import java.util.function.Predicate;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.filthy_monarch.FilthyMonarch.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.particles.fx.*;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.listener.*;
import net.minecraftforge.fml.common.Mod;

/** Innate progression; persistent Single Drop cooldown prevents death/relog refreshing it. */
@Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class FilthyMonarchPassives {
    private static final String DROP="filthy_monarch_drop_ready";
    private static final List<Predicate<LivingEntity>> PURIFIED=new ArrayList<>();
    /** Register once at common setup. Exempt a purified target from the King of Filth aura. */
    public static void addPurificationExemption(Predicate<LivingEntity> rule){PURIFIED.add(Objects.requireNonNull(rule));}
    static boolean innate(LivingEntity e,int seq){return e instanceof Player p&&own(p)&&SoulCore.getSequence(p)<=seq;}
    static void initialize() {
        Information.addBlocker(e->e instanceof LivingEntity t&&innate(t,7));
        Fear.addDurationScale(e->innate(e,8)?.5:1);
        Falter.addDurationScale(e->innate(e,4)?.5:1);
        EffectDuration.addRule((e,effect)->innate(e,8)&&effect.getEffect().equals(MobEffects.NAUSEA)?.5:1);
        Bleeding.addImmunity(e->innate(e,2));Corrosion.addImmunity(e->innate(e,2));
        addPurificationExemption(e->e instanceof Player p&&"White Angel".equals(SoulCore.getPathway(p)));
    }
    static boolean purified(LivingEntity e){return PURIFIED.stream().anyMatch(rule->rule.test(e));}
    static boolean dropReady(State s){return own(s.player)&&s.sequence<=2&&s.player.getPersistentData().getLongOr(DROP,0L)<=s.now();}
    static void singleDrop(State s) {
        var p=s.player;p.getPersistentData().putLong(DROP,s.now()+6000);p.setHealth(1);
        Invulnerable.grant(p,60);Root.applyOwned(p,s.tag("drop"),60,RootRestriction.EVERYTHING);
        if(s.channel!=null)s.channel.close();
        FilthyMonarchFx.form(s.level,p,"desire_liquid",60,s.tag("drop"),1);
        FilthyMonarchFx.sound(s.level,p.position(),"single_drop");
        later(s,60,()->{p.setHealth(p.getMaxHealth()*.3f);Root.removeOwned(p,s.tag("drop"));FilthyMonarchFx.end(s.level,p,p.position(),s.tag("drop"));});
    }
    static void tick(State s) {
        var p=s.player;if(!own(p))return;
        if(s.now()%20!=0)return;
        Physique.keep(p,Attributes.MAX_HEALTH,Physique.id("filthy_coldblooded"),4,AttributeModifier.Operation.ADD_VALUE,s.sequence<=8);
        if(s.sequence<=2&&p.getHealth()<p.getMaxHealth()*.5f)Healing.apply(p,1);
        if(s.sequence<=3)for(var t:List.copyOf(s.marked.values()))if(enemy(s,t))magic(s,t,2*Depravity.count(p,t));
        if(s.sequence<=1)for(var t:around(s,p.position(),12))if(!purified(t))magic(s,t,1);
        s.threats.values().removeIf(until->until<=s.now());
        double range=s.sequence<=4?48:s.sequence<=6?32:16;
        for(var t:around(s,p.position(),range)) {
            boolean hunting=t instanceof Mob mob&&mob.getTarget()==p;
            // Demons also perceive an approaching hostile mob before it acquires a target.
            boolean approaching=s.sequence<=4&&t instanceof net.minecraft.world.entity.monster.Monster
                    &&t.getDeltaMovement().dot(p.position().subtract(t.position()))>0;
            if(!hunting&&!approaching)continue;
            boolean fresh=!s.threats.containsKey(t.getUUID());s.threats.put(t.getUUID(),s.now()+60);
            if(s.sequence<=6){if(fresh)FilthyMonarchFx.danger(p,t,true);}
            else if(fresh&&!p.hasLineOfSight(t))FilthyMonarchFx.danger(p,t,false);
        }
    }
    @SubscribeEvent public static void hurt(LivingHurtEvent event) {
        if(!(event.getEntity() instanceof ServerPlayer p)||!own(p)||event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        State s=state(p);LivingEntity source=event.getSource().getEntity() instanceof LivingEntity t?t:null;
        float scale=1;
        if(s.sequence<=6&&(event.getSource().is(DamageTypeTags.IS_FIRE)||event.getSource().is(DamageTypes.WITHER)
                ||event.getSource().is(DamageTypes.MAGIC)||event.getSource().is(DamageTypes.INDIRECT_MAGIC)))scale*=.5f;
        if(s.sequence<=6&&source!=null&&s.threats.getOrDefault(source.getUUID(),0L)>s.now())scale*=.9f;
        event.setAmount(event.getAmount()*scale);
        if(s.sequence==0&&enemy(s,source)&&s.now()>=s.defenseReady){s.defenseReady=s.now()+200;Fear.apply(source,p,40);}
    }
    @SubscribeEvent(priority=Priority.LOWEST) public static void abyssHit(LivingDamageEvent event) {
        if(!(event.getSource().getEntity() instanceof ServerPlayer p)||!Form.isIn(p,ABYSS)||event.getAmount()<=0)return;
        State s=STATES.get(p.getUUID());if(s==null||!valid(s)||!enemy(s,event.getEntity()))return;
        seed(s,event.getEntity(),1);Healing.apply(p,p,Math.min(Math.min(event.getAmount(),DamageLimit.maximum(event.getEntity())),event.getEntity().getHealth())*.05f);
    }
    @SubscribeEvent public static void poison(MobEffectEvent.Applicable event) {
        if(innate(event.getEntity(),6)&&event.getEffectInstance().getEffect().equals(MobEffects.POISON))event.setResult(Result.DENY);
    }
    @SubscribeEvent public static boolean naturalHealing(LivingHealEvent event) {
        LivingEntity t=event.getEntity();if(t.level().isClientSide()||purified(t)||Healing.isSpellHealing())return false;
        for(State s:List.copyOf(STATES.values()))if(valid(s)&&own(s.player)&&s.sequence<=1&&enemy(s,t)&&s.player.distanceToSqr(t)<=144)return true;
        return false;
    }
    @SubscribeEvent public static void nearbyDeath(LivingDeathEvent event) {
        var t=event.getEntity();if(t.level().isClientSide())return;
        for(State s:List.copyOf(STATES.values()))if(valid(s)&&own(s.player)&&s.sequence==0&&s.level==t.level()
                &&s.player.distanceToSqr(t)<=576&&!Spells.isFriend(s.player,t))Healing.apply(s.player,s.player.getMaxHealth()*.05f);
    }
    @SubscribeEvent public static void clone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        long ready=event.getOriginal().getPersistentData().getLongOr(DROP,0L);
        if(ready>0)event.getEntity().getPersistentData().putLong(DROP,ready);
    }
    static void clear(State s) {
        Physique.remove(s.player,Attributes.MAX_HEALTH,Physique.id("filthy_coldblooded"));
        Root.removeOwned(s.player,s.tag("drop"));FilthyMonarchFx.end(s.level,s.player,s.player.position(),s.tag("drop"));
        s.threats.clear();
    }
}
