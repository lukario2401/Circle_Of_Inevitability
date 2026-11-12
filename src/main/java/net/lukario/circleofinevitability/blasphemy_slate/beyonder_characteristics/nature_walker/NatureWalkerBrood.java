package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker;

import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker.NatureWalker.*;

/** Separate flora, hybrid and construct ownership; no shared three-marionette slot contention. */
@Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class NatureWalkerBrood {
    static final String CONSTRUCTS=PREFIX+"constructs", HYBRIDS=PREFIX+"hybrids", FLORA=PREFIX+"flora", SPIRITS=PREFIX+"spirits";
    static final Identifier HEALTH=Physique.id(PREFIX+"hybrid_health");
    static final class Companion {
        final Mob mob;final String kind;final NatureWalkerGrowths.Kind trait;final String tag;
        NatureWalkerGrowths.Kind extra;boolean awakened;
        Companion(State s,Mob mob,String kind,NatureWalkerGrowths.Kind trait) {
            this.mob=mob;this.kind=kind;this.trait=trait;tag=tag(s,"companion_"+mob.getUUID());
        }
        boolean briar() { return trait==NatureWalkerGrowths.Kind.THORNLING||trait==NatureWalkerGrowths.Kind.BRIAR_ORCHARD||extra==NatureWalkerGrowths.Kind.THORNLING||extra==NatureWalkerGrowths.Kind.BRIAR_ORCHARD; }
        boolean verdant() { return trait==NatureWalkerGrowths.Kind.HEARTFRUIT||trait==NatureWalkerGrowths.Kind.BRIAR_ORCHARD||extra==NatureWalkerGrowths.Kind.HEARTFRUIT||extra==NatureWalkerGrowths.Kind.BRIAR_ORCHARD; }
    }
    private static boolean reflecting;
    static int constructCap(State s) { return switch(s.sequence) { case 0->18;case 1->12;case 2->9;case 3->3;case 4->2;case 5->1;default->0; }; }
    static int hybridCap(State s) { return switch(s.sequence) { case 0->12;case 1->8;case 2->5;case 3->3;case 4->2;default->1; }; }
    static boolean arthropod(Mob m) {
        return m.is(EntityTypeTags.ARTHROPOD);
    }
    static boolean creature(Mob m) {
        // Humanoid families are excluded from the upgraded creature command.
        String id=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).getPath();
        return !Set.of("zombie","husk","drowned","zombie_villager","skeleton","stray","wither_skeleton","bogged",
                "villager","wandering_trader","witch","pillager","vindicator","evoker","illusioner","piglin",
                "piglin_brute","zombified_piglin","enderman","breeze","player").contains(id);
    }
    static boolean command(State s) {
        LivingEntity focus=aimed(s,30,false);boolean changed=false;
        if(focus!=null)for(var p:NatureWalkerGrowths.near(s,s.owner.position(),30)) {
            p.focused=focus.getUUID();p.focusUntil=s.level.getGameTime()+160;changed=true;
        }
        int taken=0;
        for(var e:enemies(s,s.owner.position(),30).stream().sorted(Comparator.comparingDouble(s.owner::distanceToSqr)).toList()) {
            if(!(e instanceof Mob mob)||Marionette.isMarionette(mob)||Spells.isBoss(mob)
                    ||mob.getMaxHealth()>(s.sequence<=4?60:40)||!(s.sequence<=4?creature(mob):arthropod(mob)))continue;
            if(Marionette.controlInGroup(mob,s.owner,400,FLORA,6)) {
                s.companions.add(new Companion(s,mob,"flora",null));changed=true;if(++taken==6)break;
            }
        }
        if(s.sequence<=4)for(var e:friends(s,s.owner.position(),30))Madness.reduce(e,1);
        if(changed)effect(s,"flora_sigil",s.owner.position(),30,3);
        return changed;
    }
    static boolean hybrid(State s) {
        LivingEntity target=aimed(s,16,false);
        if(!(target instanceof Mob mob)||Spells.isNaturalBoss(mob)||mob.getMaxHealth()>(s.sequence<=2?400:80)||Marionette.isMarionette(mob))return false;
        var available=NatureWalkerGrowths.near(s,s.owner.position(),10);
        if(available.size()<(s.sequence<=2?2:1))return false;
        var plant=available.getFirst();
        if(!Marionette.controlInGroup(mob,s.owner,s.sequence<=4?72000:600,HYBRIDS,hybridCap(s)))return false;
        float ratio=mob.getHealth()/mob.getMaxHealth();
        double bonus=(mob instanceof Animal?2.25:1.5)*verdancy(s.owner)-1;
        Physique.keep(mob,Attributes.MAX_HEALTH,HEALTH,bonus,AttributeModifier.Operation.ADD_MULTIPLIED_BASE,true);
        mob.setHealth(mob.getMaxHealth()*ratio);
        Companion c=new Companion(s,mob,"hybrid",plant.kind);
        if(s.sequence<=2) { c.extra=available.get(1).kind;NatureWalkerGrowths.remove(s,available.get(1)); }
        s.companions.add(c);NatureWalkerGrowths.remove(s,plant);
        NatureWalkerFx.form(s.level,mob,"hybrid_crown",240,tag(s,"companion_"+mob.getUUID()),1);
        return true;
    }
    static boolean construct(State s,String kind) {
        if(constructCap(s)==0)return false;
        Vec3 at=Spells.aimGround(s.owner,s.level,12);
        boolean bird=kind.equals("stone_bird");
        // Validate the actual construct footprint, not the smaller caster's hitbox.
        AABB box=new AABB(at.x-(bird?.2:.7),at.y,at.z-(bird?.2:.7),at.x+(bird?.2:.7),at.y+(bird?.9:2.7),at.z+(bird?.2:.7));
        if(!Terrain.supported(s.level,at)||!s.level.noCollision(box))return false;
        float health=switch(kind) { case "stone_bird"->60;case "mud_golem"->300;case "steel_golem"->450;default->150; };
        float damage=switch(kind) { case "stone_bird"->10;case "mud_golem"->14;case "steel_golem"->30;default->18; };
        Construct builder=Construct.of(bird?EntityTypes.VEX:EntityTypes.IRON_GOLEM)
                .health(health*verdancy(s.owner)).damage(damage*verdancy(s.owner))
                .lasts(kind.equals("steel_golem")?2400:1800).leash(40).limit(CONSTRUCTS,constructCap(s))
                .onEnd((mob,reason)->ParticleShapes.clearModels(s.level,mob,tag(s,"companion_"+mob.getUUID())));
        if(!bird)builder.steady();if(kind.equals("mud_golem"))builder.speed(.65f);
        Mob mob=builder.build(s.owner,s.level,at,s.owner.getYRot());if(mob==null)return false;
        Companion c=new Companion(s,mob,kind,null);s.companions.add(c);show(s,c);return true;
    }
    static boolean spirit(State s,Vec3 at,float health) {
        int cap=s.sequence==0?20:s.sequence==1?14:s.sequence==2?10:6;
        Mob mob=Construct.of(EntityTypes.VEX).health(Math.max(1,health)*verdancy(s.owner)).damage(10*verdancy(s.owner))
                .lasts(900).leash(48).limit(SPIRITS,cap).build(s.owner,s.level,at,s.owner.getYRot());
        if(mob==null)return false;Companion c=new Companion(s,mob,"evil_spirit",null);s.companions.add(c);show(s,c);return true;
    }
    private static void show(State s,Companion c) {
        NatureWalkerFx.form(s.level,c.mob,c.kind.equals("hybrid")?"hybrid_crown":c.kind,240,c.tag,1);
    }
    static void tick(State s) {
        long now=s.level.getGameTime();
        for(var c:new ArrayList<>(s.companions)) {
            if(!c.mob.isAlive()||c.mob.isRemoved()||c.mob.level()!=s.level||!Marionette.isMarionetteOf(c.mob,s.owner)) {
                remove(s,c,true);continue;
            }
            if(!c.kind.equals("flora")&&now%200==0)show(s,c);
            if(c.kind.equals("hybrid")&&s.sequence<=4&&now%200==0)
                Marionette.controlInGroup(c.mob,s.owner,72000,HYBRIDS,hybridCap(s));
            if(now%20==0) {
                if(c.awakened) {
                    Healing.apply(s.owner,c.mob,c.mob.getMaxHealth()*.02f);
                    if(Construct.isBuilt(c.mob)&&Construct.ticksLeft(c.mob)<1000)Construct.extend(c.mob,72000);
                }
                Physique.keep(c.mob,Attributes.MAX_HEALTH,Physique.id(PREFIX+"kingdom_health"),.5,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,NatureWalkerAdvanced.inKingdom(s,c.mob.position()));
                if(c.verdant())for(var e:friends(s,c.mob.position(),4))Healing.apply(s.owner,e,verdancy(s.owner));
                if(c.kind.equals("mud_golem")&&(c.mob.isInWater()||Zone.covers(s.owner,PREFIX+"swamp",c.mob.position())))Healing.apply(s.owner,c.mob,3);
            }
        }
    }
    static void remove(State s,Companion c,boolean end) {
        s.companions.remove(c);ParticleShapes.clearModels(s.level,c.mob,c.tag);
        Physique.remove(c.mob,Attributes.MAX_HEALTH,Physique.id(PREFIX+"awaken_health"));
        Physique.remove(c.mob,Attributes.ATTACK_DAMAGE,Physique.id(PREFIX+"awaken_damage"));
        Physique.remove(c.mob,Attributes.MAX_HEALTH,Physique.id(PREFIX+"kingdom_health"));
        if(c.kind.equals("hybrid")) {
            Physique.remove(c.mob,Attributes.MAX_HEALTH,HEALTH);
            c.mob.setHealth(Math.min(c.mob.getHealth(),c.mob.getMaxHealth()));
        }
        if(end&&Marionette.isMarionetteOf(c.mob,s.owner)) {
            if(Construct.isBuilt(c.mob))Construct.end(c.mob);else Marionette.release(c.mob);
        }
    }
    static void clear(State s) { for(var c:new ArrayList<>(s.companions))remove(s,c,true); }
    static void leave(Entity entity) {
        for(var s:new ArrayList<>(STATES.values()))for(var c:new ArrayList<>(s.companions))
            if(c.mob==entity)remove(s,c,true);
    }
    static void death(LivingEntity dead) {
        for(var s:new ArrayList<>(STATES.values()))if(valid(s))for(var c:new ArrayList<>(s.companions))
            if(c.mob==dead&&(c.trait==NatureWalkerGrowths.Kind.SPOREBED||c.extra==NatureWalkerGrowths.Kind.SPOREBED))
                for(var e:enemies(s,dead.position(),4))Madness.add(s.owner,e,1);
    }
    @SubscribeEvent public static void hurt(LivingHurtEvent event) {
        if(reflecting||event.getEntity().level().isClientSide())return;
        Entity attacker=event.getSource().getEntity();
        for(var s:new ArrayList<>(STATES.values()))if(valid(s))for(var c:new ArrayList<>(s.companions)) {
            if(attacker==c.mob&&enemy(s.owner,event.getEntity())) {
                if(c.kind.equals("hybrid"))event.setAmount(event.getAmount()*verdancy(s.owner)*(c.awakened?2:1));
                if(NatureWalkerAdvanced.inKingdom(s,c.mob.position()))event.setAmount(event.getAmount()*1.5f);
                if(s.reaped.getOrDefault(event.getEntity().getUUID(),0L)>s.level.getGameTime())event.setAmount(event.getAmount()*1.15f);
                if(c.briar())Bleeding.apply(event.getEntity(),s.owner,60,1);
                if((c.trait==NatureWalkerGrowths.Kind.SPOREBED||c.extra==NatureWalkerGrowths.Kind.SPOREBED))Madness.add(s.owner,event.getEntity(),1);
                if(c.kind.equals("stone_bird"))for(var ally:friends(s,c.mob.position(),32))
                    if(ally instanceof net.minecraft.server.level.ServerPlayer p)EntitySight.show(p,event.getEntity(),80);
                if(c.kind.equals("mud_golem")) { Spells.keepEffect(event.getEntity(),MobEffects.SLOWNESS,1,60);Weight.put(event.getEntity(),1,60); }
            }
            if(event.getEntity()==c.mob&&c.briar()&&attacker instanceof LivingEntity foe
                    &&event.getSource().getDirectEntity()==attacker&&enemy(s.owner,foe)) {
                reflecting=true;try { strike(s,foe,event.getAmount()*.2f,true); }finally { reflecting=false; }
            }
        }
    }
}
