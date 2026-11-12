package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker;

import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobForm;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker.NatureWalker.*;

/** Later-sequence orchestration. Shared effects live in the spell kit; this class owns only pathway rules. */
final class NatureWalkerAdvanced {
    static final String BROOD=PREFIX+"brood_form", BARRIER=PREFIX+"barrier";
    static final class Field {
        final String id;final Vec3 at;final double radius;final long started,until;
        final Set<UUID> warded=new HashSet<>();int seeds;
        Field(State s,String id,Vec3 at,double radius,int ticks) { this.id=id;this.at=at;this.radius=radius;started=s.level.getGameTime();until=started+ticks; }
        boolean realm() { return id.equals("world_of_nature")||id.equals("bountiful_earth"); }
    }
    record Ward(LivingEntity target,Object token) {}
    record Cocoon(LivingEntity target,long until,long priorHiddenUntil) {}
    record Hold(LivingEntity target,long until) {}
    static final class Board {
        final Map<String,Field> fields=new HashMap<>();
        final Map<UUID,Ward> wards=new HashMap<>();
        final Map<UUID,Cocoon> cocoons=new HashMap<>();
        final Map<UUID,Hold> holds=new HashMap<>();
        final Map<String,Vec3> warnings=new HashMap<>();
        long broodUntil;double strain;float barrierYaw;
    }
    private NatureWalkerAdvanced() {}
    static void initialize() {
        Corrosion.onBite((target,source,amount)-> {
            if(source==null)return;var s=STATES.get(source.getUUID());
            if(s==null||!valid(s))return;var field=s.advanced.fields.get("descent_of_death");
            if(field!=null&&contains(s,field,target.position()))Healing.apply(source,source,amount*.25f);
        });
        Mutation.addListener(new Mutation.Listener() {
            public void changed(LivingEntity source,LivingEntity target,Mutation.Kind kind,int level,int ticks) {
                if(own(source)&&source.level() instanceof ServerLevel sl)
                    NatureWalkerFx.mutation(sl,target,kind.name().toLowerCase(Locale.ROOT),level,ticks,PREFIX+"mutation_"+source.getUUID());
            }
            public void ended(LivingEntity source,LivingEntity target) {
                if(target.level() instanceof ServerLevel sl)ParticleShapes.clearModels(sl,target,PREFIX+"mutation_"+source.getUUID());
            }
        });
        Madness.onBreak((source,target)-> {
            if(own(source)&&enemy(source,target)&&target instanceof Mob mob) { mob.setTarget(null);Fear.apply(mob,source,80); }
        });
    }
    static boolean cast(State s,String id) {
        long now=s.level.getGameTime();
        switch(id) {
            case "life_aura" -> open(s,id,s.owner.position(),14,400);
            case "mutate" -> {
                var target=aimed(s,20,false);if(target==null)return false;
                if(!Mutation.bloom(s.owner,target,100))Mutation.apply(s.owner,target,Mutation.Kind.ORGANS,2,240);
                Madness.add(s.owner,target,3);
            }
            case "brood_form" -> form(s);
            case "evil_spirits" -> { if(!spirits(s))return false; }
            case "descent_of_death" -> open(s,id,Spells.aimGround(s.owner,s.level,24),10,160);
            case "return_to_earth" -> { for(var e:enemies(s,s.owner.position(),24))returnToEarth(s,e);effect(s,"root_burst",s.owner.position(),30,8); }
            case "maternal_embrace" -> {
                var target=aimed(s,20,false);if(target==null)return false;
                int ticks=Spells.isBoss(target)?80:Spells.isEvil(target)?320:160;
                Root.applyOwned(target,tag(s,"embrace"),ticks,EnumSet.allOf(RootRestriction.class));
                NoHeal.apply(target,ticks);s.advanced.holds.put(target.getUUID(),new Hold(target,now+ticks));
                NatureWalkerFx.form(s.level,target,"coffin",ticks,tag(s,"coffin"),1.5f);
            }
            case "blossoming_land","barren_land" -> open(s,id,Spells.aimGround(s.owner,s.level,24),32,800);
            case "desolation" -> open(s,id,s.owner.position(),32,400);
            case "root_eruption" -> eruption(s);
            case "seed_of_rebirth" -> { var target=aimed(s,12,true);if(target==null)target=s.owner;if(!ward(s,target,true))return false; }
            case "world_of_nature" -> open(s,id,s.owner.position(),20,800);
            case "verdant_barrier" -> barrier(s);
            case "great_harvest" -> harvest(s);
            case "awaken" -> { if(!awaken(s))return false; }
            case "earthquake" -> quake(s);
            case "embrace_of_the_earth" -> {
                var target=aimed(s,5,false);if(target==null)return false;
                float damage=Math.max(100,target.getMaxHealth()*.25f),before=target.getHealth();
                Spells.strikeTrue(s.owner,target,damage);Healing.apply(s.owner,s.owner,Math.max(0,before-target.getHealth()));
                NoHeal.apply(target,200);Corrosion.apply(target,s.owner,4,2,200);effect(s,"earth_lance",target.position(),25,2);
            }
            case "bountiful_earth" -> open(s,id,s.owner.position(),40,800);
            default -> { return false; }
        }
        return true;
    }
    static double growthSpeed(State s,Vec3 at) {
        for(var f:s.advanced.fields.values())if((f.id.equals("life_aura")||f.id.equals("blossoming_land"))&&contains(s,f,at))return 4;
        return 1;
    }
    private static Vec3 center(State s,Field f) { return f.id.equals("life_aura")||f.id.equals("desolation")?s.owner.position():f.at; }
    static boolean contains(State s,Field f,Vec3 at) { return f.until>s.level.getGameTime()&&center(s,f).distanceToSqr(at)<=f.radius*f.radius; }
    static boolean inKingdom(State s,Vec3 at) { var f=s.advanced.fields.get("bountiful_earth");return f!=null&&contains(s,f,at); }
    private static void open(State s,String id,Vec3 at,double radius,int ticks) {
        close(s,id);Field f=new Field(s,id,at,radius,ticks);s.advanced.fields.put(id,f);
        Zone zone=id.equals("life_aura")||id.equals("desolation")?Zone.around(s.owner,radius):Zone.at(at,radius);
        zone.height(radius*2).lasts(ticks).every(20).onPulse(left->{if(valid(s)&&s.advanced.fields.get(id)==f)pulse(s,f);})
                .onEnd(()->finishField(s,f)).open(s.owner,s.level,PREFIX+id);
        if(f.realm()) {
            // The enclosure loops escapees; the dome supplies a visible bounded shell without a second movement rule.
            Dome.sphere(at,radius).lasts(ticks).outsiders(e->enemy(s.owner,e))
                    .keepWhile(()->valid(s)&&s.owner.position().distanceTo(at)<=radius+10).onEnd(()->close(s,id))
                    .raise(s.owner,s.level,PREFIX+id);
            Enclosure.at(at,radius).height(radius*2).lasts(ticks).keeps(e->enemy(s.owner,e)).open(s.owner,s.level,PREFIX+id);
            NatureWalkerFx.realm(s.level,at,(float)radius,id.equals("bountiful_earth"),ticks,tag(s,id));
        }
    }
    private static void close(State s,String id) {
        Field f=s.advanced.fields.get(id);Zone.close(s.owner,PREFIX+id);if(f!=null)finishField(s,f);
    }
    private static void finishField(State s,Field f) {
        if(!s.advanced.fields.remove(f.id,f))return;
        if(f.realm()) { Dome.close(s.owner,PREFIX+f.id);Enclosure.close(s.owner,PREFIX+f.id); }
        for(var plant:new ArrayList<>(s.plants))if(f.id.equals(plant.field))NatureWalkerGrowths.remove(s,plant);
        ParticleShapes.clearModels(s.level,f.at,tag(s,f.id));
    }
    private static void pulse(State s,Field f) {
        long age=s.level.getGameTime()-f.started;Vec3 at=center(s,f);
        var foes=enemies(s,at,f.radius);var allies=friends(s,at,f.radius);
        switch(f.id) {
            case "life_aura" -> {
                for(var ally:allies) { Spells.keepEffect(ally,MobEffects.REGENERATION,1,30);Spells.keepEffect(ally,MobEffects.SPEED,0,30); }
                for(var c:s.companions)if(c.mob.position().distanceToSqr(at)<=f.radius*f.radius)Healing.apply(s.owner,c.mob,2);
                for(var foe:foes) { Madness.add(s.owner,foe,1);if(age%80==0)Mutation.apply(s.owner,foe,Mutation.Kind.ORGANS,1,240); }
                effect(s,"life_ring",at,22,(float)(f.radius*2));
            }
            case "blossoming_land" -> {
                for(var ally:allies) { Spells.keepEffect(ally,MobEffects.REGENERATION,1,30);Spells.keepEffect(ally,MobEffects.ABSORPTION,0,30); }
                Spells.growPlants(s.owner,s.level,at,f.radius,6);
                if(age%40==0&&f.seeds<6) {
                    double a=(age/40)*Math.PI/3;Vec3 point=Spells.groundAt(s.owner,s.level,at.add(Math.cos(a)*5,1,Math.sin(a)*5));
                    if(NatureWalkerGrowths.plant(s,NatureWalkerGrowths.Kind.HEARTFRUIT,point)!=null)f.seeds++;
                }
                effect(s,"life_ring",at,22,(float)(f.radius*2));
            }
            case "barren_land" -> {
                for(var foe:foes) { NoHeal.apply(foe,30);Decay.apply(foe,30,.1f);Spells.keepEffect(foe,MobEffects.HUNGER,1,30);Spells.keepEffect(foe,MobEffects.WEAKNESS,0,30); }
                effect(s,"wither_ring",at,22,(float)(f.radius*2));
            }
            case "descent_of_death" -> {
                for(var foe:foes) { Decay.apply(foe,30,.1f+age/400f);NoHeal.apply(foe,30);Corrosion.apply(foe,s.owner,4,0,30); }
                for(var plant:NatureWalkerGrowths.near(s,at,f.radius))plant.clock.advance(40);
                effect(s,"death_canopy",at,22,(float)f.radius);
            }
            case "desolation" -> {
                for(var ally:allies)NoHeal.apply(ally,30);
                for(var foe:foes) {
                    if(Spells.isEvil(foe))returnToEarth(s,foe);
                    else { Decay.apply(foe,30,Math.min(.6f,.1f+age/800f));strike(s,foe,foe.getMaxHealth()*.02f,false); }
                    NoHeal.apply(foe,30);if(age%40==0)Madness.add(s.owner,foe,1);
                }
                effect(s,"wither_ring",at,22,(float)(f.radius*2));
            }
            case "world_of_nature","bountiful_earth" -> {
                boolean kingdom=f.id.equals("bountiful_earth");
                for(var ally:allies) {
                    Spells.keepEffect(ally,MobEffects.REGENERATION,kingdom?2:1,30);
                    Spells.keepEffect(ally,kingdom?MobEffects.RESISTANCE:MobEffects.STRENGTH,0,30);
                    if(kingdom&&f.warded.add(ally.getUUID())&&!DeathWard.isArmed(ally))ward(s,ally,false);
                }
                for(var foe:foes) {
                    Spells.keepEffect(foe,MobEffects.SLOWNESS,1,30);
                    if(kingdom) { Decay.apply(foe,30,.1f);NoHeal.apply(foe,30);if(age%60==0)Mutation.apply(s.owner,foe,Mutation.Kind.ORGANS,1,240); }
                    else if(age%40==0)Madness.add(s.owner,foe,1);
                }
                if(age%40==0&&!foes.isEmpty())NatureWalkerGrowths.temporary(s,NatureWalkerGrowths.Kind.THORNLING,
                        Spells.groundAt(s.owner,s.level,foes.get((int)(age/40%foes.size())).position()),f.id,kingdom?12:8);
            }
        }
    }
    private static boolean spirits(State s) {
        int limit=s.sequence<=2?10:6,count=0;
        for(var body:Fallen.near(s.level,s.owner.position(),s.sequence<=2?48:30,400)) {
            var entity=s.level.getEntity(body.id());if(entity!=null&&entity.isAlive())continue;
            if(!s.level.hasChunkAt(net.minecraft.core.BlockPos.containing(body.where())))continue;
            if(NatureWalkerBrood.spirit(s,body.where(),Math.min(120,body.maxHealth()*(s.sequence<=2?.7f:.4f)))) {
                Fallen.take(body);if(++count>=limit)break;
            }
        }
        return count>0;
    }
    static void returnToEarth(State s,LivingEntity target) {
        if(Spells.isEvil(target)) { strike(s,target,Spells.isBoss(target)?target.getMaxHealth()*.1f:60,false);Burial.bury(target,20,20); }
        else Weight.put(target,1,40);
    }
    static void trial(State s,LivingEntity target,float threshold,boolean roll) {
        if(target instanceof Mob&&!Spells.isBoss(target)&&enemy(s.owner,target)&&target.getHealth()<target.getMaxHealth()*threshold
                &&(!roll||s.owner.getRandom().nextBoolean()))Spells.strikeTrue(s.owner,target,target.getHealth()+1);
    }
    private static void eruption(State s) {
        Vec3 at=Spells.aimGround(s.owner,s.level,28);String warning=tag(s,"eruption_"+UUID.randomUUID());
        s.advanced.warnings.put(warning,at);
        ParticleShapes.model(s.level,NatureWalkerFx.model("warning_ring",40,warning).scale(28),at,0,0,0);
        Later.run(s.level,40,()->{
            if(!valid(s)||s.advanced.warnings.remove(warning)==null)return;
            for(var e:enemies(s,at,14)) { strike(s,e,80,false);Root.apply(e,60,RootRestriction.MOVE,RootRestriction.JUMP);trial(s,e,.3f,false);Madness.add(s.owner,e,3); }
            effect(s,"root_eruption",at,50,8);sound(s,"root_impact");
        });
    }
    private static void harvest(State s) {
        int count=s.plants.size();List<Vec3> wheatSpots=new ArrayList<>();
        for(var e:enemies(s,s.owner.position(),20)) {
            boolean wheat=Mutation.kind(s.owner,e)==Mutation.Kind.WHEAT;
            if(e.getHealth()<e.getMaxHealth()*.25f) {
                if(Spells.isBoss(e))strike(s,e,e.getMaxHealth()*.15f,false);
                else if(e instanceof Mob)Spells.strikeTrue(s.owner,e,e.getHealth()+1);
                else strike(s,e,30,false);
            } else { strike(s,e,30,false);s.reaped.put(e.getUUID(),s.level.getGameTime()+160); }
            count++;if(wheat)wheatSpots.add(Spells.groundAt(s.owner,s.level,e.position()));
        }
        for(var p:new ArrayList<>(s.plants)) { p.eaten=false;NatureWalkerGrowths.harvest(s,p); }
        for(var at:wheatSpots)NatureWalkerGrowths.plant(s,NatureWalkerGrowths.Kind.HEARTFRUIT,at);
        for(var ally:friends(s,s.owner.position(),20))Healing.apply(s.owner,ally,ally.getMaxHealth()*Math.min(1,(count/3)*.1f));
        effect(s,"harvest_crown",s.owner.position(),40,5);
    }
    private static void quake(State s) {
        for(var e:enemies(s,s.owner.position(),40)) { Falter.apply(e,tag(s,"quake"),.5f,100);Spells.keepEffect(e,MobEffects.SLOWNESS,2,100);Weight.put(e,1,100); }
        Vec3 from=s.owner.position(),to=from.add(Spells.flatLook(s.owner).scale(30));
        for(var e:Spells.enemiesOnLine(s.owner,s.level,from,to,2))if(enemy(s.owner,e)) { strike(s,e,100,false);Burial.bury(e,20,60); }
        ParticleShapes.modelAlong(s.level,NatureWalkerFx.model("rift",60,tag(s,"rift")).scale(4,1,30),from,to.subtract(from));
    }
    private static void barrier(State s) {
        Vec3 at=Spells.aimGround(s.owner,s.level,12);s.advanced.barrierYaw=s.owner.getYRot();int[] renewals={0};
        Wall.at(at,s.owner.getYRot(),12,6).lasts(400).stopsProjectiles().outsiders(e->enemy(s.owner,e))
                .onBlock(point->{if(valid(s)&&renewals[0]++<5)Wall.renew(s.owner,BARRIER);})
                .onEnd(()->ParticleShapes.clearModels(s.level,at,tag(s,"barrier"))).raise(s.owner,s.level,BARRIER);
        // One model spans the same wall plane; refresh occurs while the wall is alive.
        s.advanced.warnings.put(tag(s,"barrier"),at);
    }
    private static boolean awaken(State s) {
        LivingEntity target=Spells.aimLiving(s.owner,s.level,16,false,e->s.companions.stream().anyMatch(c->c.mob==e&&!c.kind.equals("flora")));
        if(target!=null) {
            var c=s.companions.stream().filter(one->one.mob==target).findFirst().orElseThrow();
            if(c.awakened)return false;c.awakened=true;
            if(c.kind.equals("hybrid"))c.extra=NatureWalkerGrowths.Kind.BRIAR_ORCHARD;
            Physique.keep(c.mob,Attributes.MAX_HEALTH,Physique.id(PREFIX+"awaken_health"),1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,true);
            Physique.keep(c.mob,Attributes.ATTACK_DAMAGE,Physique.id(PREFIX+"awaken_damage"),1,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL,true);
            Healing.apply(s.owner,c.mob,c.mob.getMaxHealth());
            if(Construct.isBuilt(c.mob))Construct.extend(c.mob,72000);
            effect(s,"awaken_sigil",target.position(),40,3);return true;
        }
        Vec3 at=Spells.aimGround(s.owner,s.level,16);
        var plant=NatureWalkerGrowths.near(s,at,3).stream().findFirst().orElse(null);if(plant==null||plant.kind==NatureWalkerGrowths.Kind.MOTHER_TREE)return false;
        for(var old:new ArrayList<>(s.plants))if(old.kind==NatureWalkerGrowths.Kind.MOTHER_TREE)NatureWalkerGrowths.remove(s,old);
        NatureWalkerGrowths.remove(s,plant);var tree=NatureWalkerGrowths.plant(s,NatureWalkerGrowths.Kind.MOTHER_TREE,plant.at);
        if(tree!=null)tree.clock.ripen();return tree!=null;
    }
    private static boolean ward(State s,LivingEntity target,boolean seed) {
        Object previous=DeathWard.snapshot(target);
        DeathWard.arm(target,seed?2400:800,seed?.05f:.5f,(saved,damage,sl)->{
            if(!valid(s)||saved.level()!=s.level)return;
            if(seed) {
                long prior=sl.getGameTime()+Concealment.ticksLeft(saved);
                Concealment.hide(saved,120);Root.applyOwned(saved,tag(s,"rebirth"),120,EnumSet.allOf(RootRestriction.class));
                s.advanced.cocoons.put(saved.getUUID(),new Cocoon(saved,sl.getGameTime()+120,prior));
                NatureWalkerFx.form(sl,saved,"rebirth_seed",120,tag(s,"rebirth"),2);sound(s,"rebirth_trigger");
            }
        });
        Object token=DeathWard.snapshot(target);if(token==null||token==previous)return false;
        s.advanced.wards.put(target.getUUID(),new Ward(target,token));
        if(target instanceof Player p)HudTimer.show(p,tag(s,"ward"),seed?"Seed of Rebirth":"Earth's protection",seed?2400:800,0xA3CA6F);
        return true;
    }
    private static void release(State s,Cocoon c,boolean bloom) {
        Root.removeOwned(c.target,tag(s,"rebirth"));
        if(Concealment.ticksLeft(c.target)<=Math.max(0,c.until-s.level.getGameTime())+1) {
            Concealment.reveal(c.target);int prior=(int)(c.priorHiddenUntil-s.level.getGameTime());if(prior>0)Concealment.hide(c.target,prior);
        }
        ParticleShapes.clearModels(s.level,c.target,tag(s,"rebirth"));
        if(bloom&&c.target.isAlive()) {
            NoHeal.remove(c.target);Decay.cure(c.target);Healing.apply(s.owner,c.target,Math.max(0,c.target.getMaxHealth()*.6f-c.target.getHealth()));
            for(var ally:friends(s,c.target.position(),6))if(ally!=c.target)Healing.apply(s.owner,ally,ally.getMaxHealth()*.2f);
            effect(s,"healing_blossom",c.target.position(),40,4);sound(s,"rebirth_bloom");
        }
    }
    private static void form(State s) {
        boolean complete=s.sequence<=2;int ticks=complete?400:160;double scale=complete?2:.6;
        var add=AttributeModifier.Operation.ADD_VALUE;var multiply=AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        s.advanced.broodUntil=s.level.getGameTime()+ticks;s.advanced.strain=0;
        if(s.owner instanceof Player p)Form.of(BROOD).lasts(ticks).attribute(Attributes.SCALE,scale,multiply)
                .attribute(Attributes.MAX_HEALTH,complete?30:12,add).attribute(Attributes.ATTACK_DAMAGE,complete?20:10,add)
                .attribute(Attributes.ENTITY_INTERACTION_RANGE,complete?2.5:1,add).effect(MobEffects.INVISIBILITY,0)
                .keepWhile(x->valid(s)).onEnd((x,why)->endFormFx(s)).take(p);
        else if(s.owner instanceof Mob m)MobForm.of(BROOD).lasts(ticks).attribute(Attributes.SCALE,scale,multiply)
                .attribute(Attributes.MAX_HEALTH,complete?30:12,add).attribute(Attributes.ATTACK_DAMAGE,complete?20:10,add)
                .effect(MobEffects.INVISIBILITY,0).onEnd(x->endFormFx(s)).take(m);
        showForm(s);
    }
    private static boolean inForm(State s) { return s.owner instanceof Player p?Form.isIn(p,BROOD):MobForm.isIn(s.owner,BROOD); }
    private static void showForm(State s) { NatureWalkerFx.form(s.level,s.owner,s.sequence<=2?"brood_form_complete":"brood_form",240,tag(s,"brood"),s.sequence<=2?3:1.6f); }
    private static void endFormFx(State s) { s.advanced.broodUntil=0;ParticleShapes.clearModels(s.level,s.owner,tag(s,"brood")); }
    static void tick(State s) {
        long now=s.level.getGameTime();
        for(var f:new ArrayList<>(s.advanced.fields.values()))if(f.realm()&&s.owner.position().distanceTo(f.at)>f.radius+10)close(s,f.id);
        for(var entry:new ArrayList<>(s.advanced.wards.entrySet())) {
            Ward w=entry.getValue();if(w.target.level()!=s.level||!w.target.isAlive()||DeathWard.snapshot(w.target)!=w.token) {
                DeathWard.disarmIf(w.target,w.token);if(w.target instanceof Player p)HudTimer.hide(p,tag(s,"ward"));s.advanced.wards.remove(entry.getKey());
            }
        }
        for(var entry:new ArrayList<>(s.advanced.cocoons.entrySet())) {
            var c=entry.getValue();if(now>=c.until||!c.target.isAlive()||c.target.level()!=s.level) {
                s.advanced.cocoons.remove(entry.getKey());release(s,c,c.target.level()==s.level&&now>=c.until);
            }
        }
        for(var entry:new ArrayList<>(s.advanced.holds.entrySet()))if(entry.getValue().until<=now||!entry.getValue().target.isAlive()) {
            var target=entry.getValue().target;Root.removeOwned(target,tag(s,"embrace"));Spells.keepEffect(target,MobEffects.WEAKNESS,0,100);s.advanced.holds.remove(entry.getKey());
        }
        if(now%20==0&&inForm(s)) {
            s.advanced.strain+=s.sequence<=2?1d/30:1d/8;
            for(var e:enemies(s,s.owner.position(),s.sequence<=2?10:6))Mutation.apply(s.owner,e,Mutation.Kind.ORGANS,1,240);
            if(s.advanced.strain>=1) { if(s.owner instanceof Player p)Form.end(p);else if(s.owner instanceof Mob m)MobForm.end(m); }
            else if(now%200==0)showForm(s);
        }
        if(now%20==0) {
            Vec3 at=s.advanced.warnings.get(tag(s,"barrier"));
            if(at!=null) {
                if(!Wall.isRaised(s.owner,BARRIER))s.advanced.warnings.remove(tag(s,"barrier"));
                else ParticleShapes.model(s.level,NatureWalkerFx.model("verdant_barrier",22,tag(s,"barrier")).scale(12,6,1),at,s.advanced.barrierYaw,0,0);
            }
        }
    }
    static void clear(State s) {
        for(String id:new ArrayList<>(s.advanced.fields.keySet()))close(s,id);
        Wall.close(s.owner,BARRIER);
        for(var entry:s.advanced.warnings.entrySet())ParticleShapes.clearModels(s.level,entry.getValue(),entry.getKey());s.advanced.warnings.clear();
        for(var ward:s.advanced.wards.values()) { DeathWard.disarmIf(ward.target,ward.token);if(ward.target instanceof Player p)HudTimer.hide(p,tag(s,"ward")); }s.advanced.wards.clear();
        for(var c:s.advanced.cocoons.values())release(s,c,false);s.advanced.cocoons.clear();
        for(var hold:s.advanced.holds.values()) { Root.removeOwned(hold.target,tag(s,"embrace"));ParticleShapes.clearModels(s.level,hold.target,tag(s,"coffin")); }s.advanced.holds.clear();
        if(s.owner instanceof Player p&&Form.isIn(p,BROOD))Form.end(p);
        if(s.owner instanceof Mob m&&MobForm.isIn(m,BROOD))MobForm.end(m);
    }
}
