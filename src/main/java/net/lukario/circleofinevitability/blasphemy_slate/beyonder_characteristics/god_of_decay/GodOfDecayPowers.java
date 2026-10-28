package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.particles.fx.ModelFx;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay.GodOfDecayFx.Ability.*;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay.GodOfDecayMarks.Kind;

/** Gameplay actions and cancellable cast lifetimes. No delayed repeating tasks outlive their cast. */
public final class GodOfDecayPowers {
    static final String DOUBLE_GROUP="god_decay_double";
    static final Map<UUID,EnumMap<GodOfDecayFx.Ability,State>> STATES=new HashMap<>();
    static final class State {
        final ServerPlayer owner;
        final ServerLevel level;
        final GodOfDecayFx.Ability ability;
        final String tag="god_decay_cast_"+UUID.randomUUID();
        final long start, until;
        final String pathway;
        final Map<UUID,Integer> counts=new HashMap<>();
        final Map<Integer,Long> regrow=new HashMap<>();
        Vec3 spot, lastStep;
        int charges=3, absent;
        State(ServerPlayer owner,ServerLevel level,GodOfDecayFx.Ability ability,int ticks) {
            this.owner=owner;this.level=level;this.ability=ability;pathway=SoulCore.getPathway(owner);start=level.getGameTime();until=start+ticks;
            spot=owner.position();lastStep=spot;
        }
    }
    private GodOfDecayPowers() {}
    static State state(LivingEntity e,GodOfDecayFx.Ability ability) {
        var states=STATES.get(e.getUUID());return states==null?null:states.get(ability);
    }
    static boolean active(LivingEntity e,GodOfDecayFx.Ability ability) {
        State s=state(e,ability); return s!=null && e.level()==s.level && s.until>s.level.getGameTime();
    }
    static State start(ServerPlayer p,ServerLevel sl,GodOfDecayFx.Ability ability,int ticks) {
        end(p,ability);
        State s=new State(p,sl,ability,ticks);
        STATES.computeIfAbsent(p.getUUID(),k->new EnumMap<>(GodOfDecayFx.Ability.class)).put(ability,s);
        GodOfDecayFx.status(p,ability,ticks);return s;
    }
    static void end(LivingEntity entity,GodOfDecayFx.Ability ability) { end(entity,ability,true); }
    static void end(LivingEntity entity,GodOfDecayFx.Ability ability,boolean reform) {
        var states=STATES.get(entity.getUUID()); if(states==null) return;
        State s=states.remove(ability); if(states.isEmpty()) STATES.remove(entity.getUUID()); if(s==null)return;
        GodOfDecayFx.end(s.level,entity,s.spot,s.tag); GodOfDecayFx.statusEnd(s.owner,ability);
        if(ability==SWOLLEN_FORM) GiantForm.end(entity);
        if(ability==DIVISION) Construct.endAll(entity,DOUBLE_GROUP);
        if(ability==PATHOGEN_FORM || ability==VIRUS_INCARNATION) {
            if(reform && entity.isAlive() && entity.level()==s.level) {
                Vec3 destination=s.spot;
                List<Mob> doubles=Construct.ofOwner(entity,DOUBLE_GROUP);
                if(!doubles.isEmpty()) destination=doubles.getFirst().position();
                Vec3 safe=Spells.freeSpotNear(entity,s.level,destination,Spells.flatLook(entity),0);
                if(safe!=null && !Root.has(entity,RootRestriction.TELEPORT)) entity.teleportTo(safe.x,safe.y,safe.z);
                GodOfDecayFx.reform(s.level,entity,s.tag);
            }
        }
        if(ability==RAGNAROK) for(ServerPlayer viewer:s.level.players()) net.lukario.circleofinevitability.screen.ScreenImages.hide(viewer,"decay_"+s.tag+"_*");
    }
    static void clear(LivingEntity entity) {
        DOOM_WARDS.keySet().removeIf(key->key.startsWith(entity.getUUID()+"/") || key.endsWith("/"+entity.getUUID()));
        for(GodOfDecayFx.Ability a:GodOfDecayFx.Ability.values()) end(entity,a,false);
    }
    static boolean hasDoubles(LivingEntity p) { return !Construct.ofOwner(p,DOUBLE_GROUP).isEmpty(); }
    static List<State> domains(LivingEntity victim,GodOfDecayFx.Ability ability) {
        List<State> found=new ArrayList<>();
        for(var states:STATES.values()) {
            State s=states.get(ability);
            if(s!=null && s.owner.isAlive() && s.level==victim.level() && s.until>s.level.getGameTime()
                    && Spells.isEnemy(s.owner,victim) && victim.distanceToSqr(s.owner)<=radius(s)*radius(s)) found.add(s);
        }
        return found;
    }
    static boolean inEnemyDomain(LivingEntity e,GodOfDecayFx.Ability a) { return !domains(e,a).isEmpty(); }
    static double radius(State s) {
        return switch(s.ability) {case CORRUPTION->10;case DECAY->GodOfDecay.is(s.owner,1)?30:20;case SLOW_TIME->24;case ENTROPY->30;case RAGNAROK->48;case VIRUS_INCARNATION->12;default->16;};
    }
    static String description(GodOfDecayFx.Ability a) {
        return switch(a) {
            case LAZINESS -> "Halve attack speed and suppress sprinting for 14 seconds.";
            case OBESITY -> "Prevent jumping and sprinting; amplify damage and resist knockback for 14 seconds.";
            case DEGENERATION -> "Weaken attacks and reduce maximum health over 14 seconds.";
            case INCUBATION -> "Hide three diseases for 8 seconds, then erupt at double strength for 10 seconds.";
            case ROT -> "Rot away 2% maximum health each second for 16 seconds; block healing and spread on death.";
            case CERTAIN_DEATH -> "Execute weak mobs; doom stronger targets. Existing substitutes and death wards can save them.";
            case SURE_HIT -> "An unavoidable 55-damage beam through cover, then three extended melee swings.";
            case DECAY -> "Weather enemy effects, constructs, barriers and equipment in an area for 12 seconds.";
            case SWOLLEN_FORM -> "Become a pathogen giant for 40 seconds. Stronger Sequences unlock the Time Giant and ancient form. Recast to dismiss free.";
            case LEFT_HAND_OF_DECAY -> "Hold visible enemies in a cone for 4 seconds, then pronounce doom.";
            case WITHERING_PALM -> "Guard your front for 3 seconds; wither projectiles and soften ranged and melee attacks.";
            case GRAVEL_SHOT -> "Fire six weathered shards over 1.5 seconds; each hit inflicts Rot.";
            case SLOW_TIME -> "Slow enemies and their projectiles while quickening allies for 10 seconds.";
            case SEGMENTED_TIME -> "Delay the target's blows and abilities by 2 seconds for 8 seconds.";
            case SENESCENCE -> "Age a target or area for 15 seconds. Weak mobs die of old age after 6 seconds.";
            case ENTROPY -> "For 20 seconds deny healing and death wards, double Sickness and raise enemy costs.";
            case VIRUS_INCARNATION -> "Become a disease swarm for 30 seconds. Recast to dismiss free.";
            case RAGNAROK -> "Build entropy toward collapse in a 48-block domain for 30 seconds. Recast to dismiss free.";
            default -> GodOfDecay.label(a);
        };
    }
    static AbilityWheel.Result cast(ServerPlayer p,ServerLevel sl,GodOfDecayFx.Ability a) {
        GodOfDecayEvents.initialize();
        if((a==SWOLLEN_FORM || a==VIRUS_INCARNATION || a==RAGNAROK) && active(p,a)) { end(p,a);return AbilityWheel.Result.FREE; }
        switch(a) {
            case ULCEROUS_BURST -> {
                GodOfDecayFx.ulcerousBurst(sl,p);
                for(LivingEntity t:Spells.enemiesAround(p,sl,p.position(),5)) GodOfDecayMarks.put(p,t,Kind.INFECTED,240);
            }
            case CARRIERS_TOUCH -> {State s=start(p,sl,a,300);GodOfDecayFx.carriersTouch(sl,p,300,s.tag);}
            case RED_TAPE -> {
                LivingEntity t=Spells.aimEnemy(p,sl,20);if(t==null)return refused(p,"Aim at an enemy within 20 blocks.");
                GodOfDecayMarks.put(p,t,Kind.RED_TAPE,160);Spells.keepEffect(t,MobEffects.SLOWNESS,1,160);
                GodOfDecayFx.redTape(sl,t,160,GodOfDecayMarks.tag(t,GodOfDecayMarks.key(p,Kind.RED_TAPE)));
                if(t instanceof Mob) Root.apply(t,40,RootRestriction.MOVE);
            }
            case OFFICIAL_CAPACITY -> {State s=start(p,sl,a,400);GodOfDecayFx.officialCapacity(sl,p,400,s.tag);}
            case CORRUPTION -> {State s=start(p,sl,a,300);GodOfDecayFx.corruption(sl,p.position(),10,300,s.tag);}
            case HOLLOW_OUT -> {
                LivingEntity t=Spells.aimEnemy(p,sl,20);if(t==null)return refused(p,"Aim at an enemy within 20 blocks.");
                GodOfDecayMarks.put(p,t,Kind.HOLLOWED,240);
                List<LivingEntity> near=Spells.enemiesAround(p,sl,t.position(),6).stream().filter(e->e!=t).sorted(Comparator.comparingDouble(t::distanceToSqr)).toList();
                if(near.size()>=3) near.stream().limit(3).forEach(e->GodOfDecayMarks.put(p,e,Kind.HOLLOWED,240));
            }
            case LAZINESS,OBESITY,DEGENERATION,INCUBATION,ROT -> {
                List<LivingEntity> targets=diseaseTargets(p,sl);if(targets.isEmpty())return refused(p,"Aim at an enemy, or at an area from Sequence 4.");
                Kind kind=switch(a){case LAZINESS->Kind.LAZY;case OBESITY->Kind.OBESE;case DEGENERATION->Kind.DEGENERATE;case INCUBATION->Kind.LATENT;default->Kind.ROTTING;};
                for(LivingEntity t:targets) {
                    int ticks=a==ROT?320:a==INCUBATION?360:280;
                    if(a!=INCUBATION && GodOfDecay.is(p,3) && GodOfDecayMarks.count(p,t)>=3)ticks*=2;
                    GodOfDecayMarks.put(p,t,kind,ticks);
                }
            }
            case DIVISION -> {
                if(hasDoubles(p))return AbilityWheel.Result.FREE;
                if(Root.has(p,RootRestriction.TELEPORT) || Decree.violate(p,Decree.Rule.STAND_INS))return AbilityWheel.Result.REFUSED;
                State s=start(p,sl,a,600);int count=GodOfDecay.is(p,4)?3:1;
                for(int i=0;i<count;i++) spawnDouble(s,i);
                if(!hasDoubles(p)){end(p,a);return AbilityWheel.Result.REFUSED;}
            }
            case PATHOGEN_FORM,VIRUS_INCARNATION -> {
                if(Root.has(p,RootRestriction.TELEPORT))return refused(p,"You cannot reform while teleportation is sealed.");
                Vec3 destination=Spells.blinkSpot(p,sl,24);if(destination==null)return refused(p,"There is no safe place to reform.");
                end(p,a==PATHOGEN_FORM?VIRUS_INCARNATION:PATHOGEN_FORM);
                State s=start(p,sl,a,a==PATHOGEN_FORM?120:600);s.spot=destination;
                GodOfDecayFx.pathogenForm(sl,p,(int)(s.until-s.start),s.tag,a==VIRUS_INCARNATION);
            }
            case CERTAIN_DEATH,SURE_HIT -> {
                List<LivingEntity> targets=(active(p,PATHOGEN_FORM)||active(p,VIRUS_INCARNATION))?Spells.enemiesAround(p,sl,p.position(),8):List.of();
                if(targets.isEmpty()) {LivingEntity t=aimThrough(p,sl,a==SURE_HIT?32:28,a==SURE_HIT);if(t==null)return refused(p,"Aim at an enemy.");targets=List.of(t);}
                for(LivingEntity target:targets) fireBeam(p,sl,target,a==CERTAIN_DEATH);
                if(active(p,PATHOGEN_FORM)||active(p,VIRUS_INCARNATION)) GodOfDecayFx.dissolvedBeam(sl,p,a==CERTAIN_DEATH);
                if(a==SURE_HIT)start(p,sl,a,160);
            }
            case DECAY -> {State s=start(p,sl,a,240);GodOfDecayFx.decay(sl,p.position(),radius(s),240,s.tag,GodOfDecay.is(p,1));}
            case SWOLLEN_FORM -> {
                State s=start(p,sl,a,800);int seq=SoulCore.getSequence(p);
                float height=seq<=1?30:seq<=2?24:seq<=3?6:5;
                GodOfDecayFx.Form form=form(p);
                // Huge high-Sequence bodies are visual; practical collision/reach remains six blocks tall.
                GiantForm.of(Math.min(6,height)/Math.max(.1,p.getBbHeight())).reach(3).stepHeight(1).lasts(800)
                        .madness(seq<=2?0:seq<=3?1f/60:1f/35)
                        .onMad(e->GodOfDecayMarks.put(e,e,Kind.DEGENERATE,300))
                        .onEnd((e,why)->end(e,SWOLLEN_FORM)).grow(p);
                GodOfDecayFx.form(sl,p,form,height,800,s.tag);
            }
            case LEFT_HAND_OF_DECAY -> {
                List<LivingEntity> targets=Spells.enemiesInCone(p,sl,p.getEyePosition(),p.getLookAngle(),24,90).stream()
                        .filter(t->Spells.clearLine(t,sl,t.getEyePosition(),p.getEyePosition())).toList();
                if(targets.isEmpty())return refused(p,"No enemy can see your raised hand.");
                String tag="decay_hand_"+UUID.randomUUID();GodOfDecayFx.leftHand(sl,p,96,tag,false);
                Later.run(sl,GodOfDecayFx.HAND_RISEN,()->{
                    if(!Spells.casterStillHere(p,sl))return;
                    for(LivingEntity t:targets) {
                        if(!t.isAlive()||t.level()!=sl||!Spells.clearLine(t,sl,t.getEyePosition(),p.getEyePosition()))continue;
                        if(Dispel.isConjured(t)) {UUID owner=Construct.ownerOf(t);Dispel.unmake(t);if(owner!=null&&sl.getEntity(owner) instanceof LivingEntity real)Spells.strikeTrue(p,real,real.getMaxHealth()*.2f);continue;}
                        Root.apply(t,80,RootRestriction.EVERYTHING);if(t instanceof Player viewer)GodOfDecayFx.handVision(viewer,80,tag);
                        float health=t.getMaxHealth();Object ward=DeathWard.snapshot(t);
                        for(int i=1;i<=4;i++){int step=i;Later.run(sl,i*20,()->{
                            if(!Spells.casterStillHere(p,sl)||!t.isAlive()||t.level()!=sl)return;
                            fatedHit(p,t,health*.1f,ward);if(step==4 && t.isAlive())doom(p,t,ward);
                        });}
                    }
                });
            }
            case WITHERING_PALM -> {State s=start(p,sl,a,60);GodOfDecayFx.leftHand(sl,p,60,s.tag,true);}
            case GRAVEL_SHOT -> {
                for(int i=0;i<6;i++)Later.run(sl,i*5,()->{
                    if(!Spells.casterStillHere(p,sl))return;
                    GodOfDecayFx.gravelLaunch(sl,p);
                    Shot.of(ModelFx.of("god_of_decay/gravel").scale(.25f)).speed(3).range(28).width(.2)
                            .onHit((target,at)->{Spells.strike(p,target,20);GodOfDecayMarks.put(p,target,Kind.ROTTING,80);GodOfDecayFx.weathering(sl,at);})
                            .fire(p,sl,p.getEyePosition(),p.getLookAngle());
                });
            }
            case SLOW_TIME -> {State s=start(p,sl,a,200);GodOfDecayFx.slowTime(sl,p.position(),24,200,s.tag);}
            case SEGMENTED_TIME -> {
                LivingEntity t=Spells.aimEnemy(p,sl,30);if(t==null)return refused(p,"Aim at an enemy within 30 blocks.");
                GodOfDecayMarks.put(p,t,Kind.SEGMENTED,160);
            }
            case SENESCENCE -> {
                LivingEntity target=Spells.aimEnemy(p,sl,30);Vec3 at=target==null?Spells.aimPoint(p,sl,30):target.position();
                List<LivingEntity> targets=target==null?Spells.enemiesAround(p,sl,at,10):List.of(target);
                if(targets.isEmpty())return refused(p,"No enemy is in the aimed area.");
                GodOfDecayFx.senescence(sl,at,target==null?10:1,300,"decay_age_"+UUID.randomUUID());
                for(LivingEntity t:targets)GodOfDecayMarks.put(p,t,Kind.AGED,300);
            }
            case ENTROPY -> {State s=start(p,sl,a,400);GodOfDecayFx.entropy(sl,p.position(),30,400,s.tag);}
            case RAGNAROK -> {State s=start(p,sl,a,600);GodOfDecayFx.ragnarok(sl,p.position(),48,600,s.tag);}
        }
        return AbilityWheel.Result.DONE;
    }
    static AbilityWheel.Result refused(Player p,String text){p.sendSystemMessage(net.minecraft.network.chat.Component.literal(text));return AbilityWheel.Result.REFUSED;}
    static List<LivingEntity> diseaseTargets(ServerPlayer p,ServerLevel sl) {
        LivingEntity t=Spells.aimEnemy(p,sl,20);if(t!=null)return List.of(t);
        return GodOfDecay.is(p,4)?Spells.enemiesAround(p,sl,Spells.aimPoint(p,sl,24),8):List.of();
    }
    static LivingEntity aimThrough(LivingEntity p,ServerLevel sl,double range,boolean through) {
        if(!through)return Spells.aimEnemy(p,sl,range);
        Vec3 from=p.getEyePosition(),to=from.add(p.getLookAngle().scale(range));
        return sl.getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(.5),e->e!=p && e.isAlive() && Spells.isEnemy(p,e) && Spells.canBeChosen(p,e)
                        && e.getBoundingBox().inflate(.4).clip(from,to).isPresent()).stream().min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
    }
    static void fireBeam(ServerPlayer p,ServerLevel sl,LivingEntity target,boolean certain) {
        Object substitute=Substitute.snapshot(target),ward=DeathWard.snapshot(target);
        GodOfDecayFx.beam(sl,p,target.getBoundingBox().getCenter(),certain);
        Later.run(sl,GodOfDecayFx.BEAM_HITS,()->{
            if(!Spells.casterStillHere(p,sl)||!target.isAlive()||target.level()!=sl)return;
            if(Substitute.takeSnapshot(target,substitute,p.damageSources().playerAttack(p),55))return;
            GodOfDecayFx.beamImpact(sl,target,certain);
            if(certain)doom(p,target,ward);
            else {GodOfDecayMarks.put(p,target,Kind.FATED,160);fatedHit(p,target,55,ward);}
        });
    }
    // Existing death wards are captured before the cast. A newly armed ward cannot undo this doom.
    private static final Map<String,Object> DOOM_WARDS=new HashMap<>();
    static void doom(LivingEntity caster,LivingEntity target,Object ward) {
        float threshold=GodOfDecay.is(caster,3)?300:150;
        if(!(target instanceof Player) && !Spells.isNaturalBoss(target) && target.getMaxHealth()<=threshold) {fatedHit(caster,target,target.getMaxHealth()*4,ward);return;}
        GodOfDecayMarks.put(caster,target,Kind.DOOMED,GodOfDecay.is(caster,3)?360:240);
        DOOM_WARDS.put(caster.getUUID()+"/"+target.getUUID(),ward);
    }
    static void forgetDoom(UUID owner,LivingEntity target) { DOOM_WARDS.remove(owner+"/"+target.getUUID()); }
    static void doomHit(LivingEntity caster,LivingEntity target,float amount) {fatedHit(caster,target,amount,DOOM_WARDS.get(caster.getUUID()+"/"+target.getUUID()));}
    static void fatedHit(LivingEntity caster,LivingEntity target,float amount,Object ward) {
        CertainDamage.strike(caster,target,amount*(1+Math.min(.30f,.06f*GodOfDecayMarks.count(caster,target))),ward);
        if(ward!=null && DeathWard.snapshot(target)!=ward) {
            GodOfDecayMarks.end(caster,target,Kind.DOOMED);
            DOOM_WARDS.remove(caster.getUUID()+"/"+target.getUUID());
        }
    }
    static void wear(LivingEntity target,float amount,boolean fraction) { wear(target,amount,fraction,false); }
    static void wear(LivingEntity target,float amount,boolean fraction,boolean armorOnly) {
        for(EquipmentSlot slot:EquipmentSlot.values()) {
            if(armorOnly && slot!=EquipmentSlot.HEAD && slot!=EquipmentSlot.CHEST && slot!=EquipmentSlot.LEGS && slot!=EquipmentSlot.FEET && slot!=EquipmentSlot.BODY)continue;
            var stack=target.getItemBySlot(slot);if(!stack.isDamageableItem())continue;
            int damage=fraction?Math.max(1,Math.round(stack.getMaxDamage()*amount)):Math.max(1,(int)amount);
            stack.hurtAndBreak(damage,target,slot);
        }
    }
    static GodOfDecayFx.Form form(LivingEntity p) {return GodOfDecay.is(p,1)?GodOfDecayFx.Form.ANCIENT_GOD:GodOfDecay.is(p,2)?GodOfDecayFx.Form.TIME_GIANT:GodOfDecayFx.Form.SWOLLEN;}
    static void spawnDouble(State s,int index) {
        Vec3 direction=Spells.turned(Spells.flatLook(s.owner),index*120);
        Vec3 at=Spells.freeSpotNear(s.owner,s.level,s.owner.position(),direction,3);
        if(at==null)return;
        int ticks=(int)(s.until-s.level.getGameTime());if(ticks<=0)return;
        Mob mob=Construct.of(net.minecraft.world.entity.EntityTypes.HUSK).health(30).damage(0).speed(0).steady().lasts(ticks).leash(48).limit(DOUBLE_GROUP,3)
                .onEnd((gone,why)->{
                    GodOfDecayFx.end(s.level,gone,gone.position(),s.tag+index);
                    if(why==Construct.End.DIED && GodOfDecay.is(s.owner,4) && state(s.owner,DIVISION)==s)s.regrow.put(index,s.level.getGameTime()+400);
                }).build(s.owner,s.level,at,s.owner.getYRot());
        if(mob!=null){mob.setNoAi(true);mob.getPersistentData().putInt("decay_double_index",index);GodOfDecayFx.division(s.level,at,s.owner.getYRot(),ticks,s.tag+index);}
    }
    static boolean saveByDouble(LivingEntity p) {
        if(!active(p,DIVISION)||Root.has(p,RootRestriction.TELEPORT))return false;
        List<Mob> doubles=Construct.ofOwner(p,DOUBLE_GROUP);if(doubles.isEmpty())return false;
        Mob doubleEntity=doubles.getFirst();Vec3 at=Spells.freeSpotNear(p,(ServerLevel)p.level(),doubleEntity.position(),Spells.flatLook(p),3);if(at==null)return false;
        State s=state(p,DIVISION);int index=doubleEntity.getPersistentData().getIntOr("decay_double_index",0);
        Construct.end(doubleEntity);GodOfDecayFx.end(s.level,p,doubleEntity.position(),s.tag+index);
        if(GodOfDecay.is(p,4))s.regrow.put(index,s.level.getGameTime()+400);
        p.setHealth(Math.max(1,p.getMaxHealth()*.35f));p.teleportTo(at.x,at.y,at.z);p.fallDistance=0;
        Concealment.hide(p,40);GodOfDecayFx.reform(s.level,p,s.tag+index);return true;
    }
    static void tick(ServerPlayer p,ServerLevel sl) {
        var states=STATES.get(p.getUUID());if(states==null)return;
        for(State s:List.copyOf(states.values())) {
            if(s.level!=sl || !p.isAlive() || !s.pathway.equals(SoulCore.getPathway(p)) || sl.getGameTime()>=s.until){end(p,s.ability);continue;}
            long age=sl.getGameTime()-s.start;
            if(s.ability==CORRUPTION||s.ability==DECAY||s.ability==SLOW_TIME||s.ability==ENTROPY||s.ability==RAGNAROK){
                s.spot=p.position();GodOfDecayFx.moveZone(sl,s.spot,s.tag,s.ability);
            }
            if(s.ability==DIVISION) {
                for(var entry:List.copyOf(s.regrow.entrySet()))if(sl.getGameTime()>=entry.getValue()){s.regrow.remove(entry.getKey());spawnDouble(s,entry.getKey());}
            }
            if(s.ability==PATHOGEN_FORM||s.ability==VIRUS_INCARNATION) {
                p.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.INVISIBILITY,6,0,false,false));
                Spells.keepEffect(p,MobEffects.SPEED,2,6);
                if(age%5==0)GodOfDecayFx.pathogenTrail(sl,p,s.ability==VIRUS_INCARNATION);
                if(age%10==0)for(LivingEntity t:Spells.enemiesAround(p,sl,p.position(),1.5))GodOfDecayMarks.put(p,t,Kind.INFECTED,160);
            }
            if(s.ability==WITHERING_PALM) {
                Vec3 facing=Spells.flatLook(p);
                for(Projectile shot:sl.getEntitiesOfClass(Projectile.class,p.getBoundingBox().inflate(4))) {
                    if(!(shot.getOwner() instanceof LivingEntity sender)||Spells.isFriend(p,sender)||shot.position().subtract(p.position()).normalize().dot(facing)<.3)continue;
                    GodOfDecayFx.palmBlock(sl,shot.position());GodOfDecayMarks.put(p,sender,Kind.INFECTED,160);shot.discard();
                }
            }
            if(age%20!=0)continue;
            switch(s.ability) {
                case OFFICIAL_CAPACITY -> {
                    List<LivingEntity> allies=Spells.livingAround(p,sl,p.position(),16).stream().filter(e->e!=p&&Spells.isFriend(p,e)).toList();
                    if(allies.isEmpty()){if(++s.absent>=3)end(p,s.ability);}else{s.absent=0;GodOfDecayFx.capacityLink(sl,p,allies.getFirst());}
                }
                case CORRUPTION -> {
                    for(LivingEntity t:Spells.enemiesAround(p,sl,p.position(),10))GodOfDecayMarks.put(p,t,Kind.CORRUPTED,25);
                    GodOfDecayFx.zoneTick(sl,p.position(),10,GodOfDecayFx.GOLD);
                }
                case DECAY -> {
                    double radius=radius(s);Weathering.area(p,sl,p.position(),radius,40);
                    for(LivingEntity t:Spells.enemiesAround(p,sl,p.position(),radius)) {
                        t.clearFire();wear(t,GodOfDecay.is(p,1)?2:1,false);
                        if(GodOfDecayMarks.count(p,t)>0){t.removeEffect(MobEffects.RESISTANCE);t.removeEffect(MobEffects.ABSORPTION);t.setAbsorptionAmount(0);}
                        if(GodOfDecay.is(p,1))GodOfDecayMarks.countedAge(p,t,25);
                    }
                    GodOfDecayFx.zoneTick(sl,p.position(),radius,GodOfDecayFx.PATHWAY_COLOR);
                }
                case SWOLLEN_FORM -> {
                    Spells.keepEffect(p,MobEffects.STRENGTH,1,25);Spells.keepEffect(p,MobEffects.RESISTANCE,1,25);
                    if(p.position().distanceToSqr(s.lastStep)>.4) {
                        s.lastStep=p.position();Vec3 pool=p.position();String tag=s.tag+"_pool_"+age;
                        GodOfDecayFx.pathogenPool(sl,pool,100,tag);
                        Zone.at(pool,1.5).lasts(100).every(10).onInside((t,ticks)->GodOfDecayMarks.put(p,t,Kind.INFECTED,160)).open(p,sl,tag);
                        GodOfDecayFx.formStep(sl,p,form(p),GodOfDecay.is(p,3)?6:5);
                    }
                    if(!GodOfDecay.is(p,2)) {
                        GodOfDecayFx.madness(p,GiantForm.madness(p),25,s.tag);
                        for(LivingEntity t:Spells.enemiesAround(p,sl,p.position(),20))if(!Spells.isBoss(t)&&Spells.clearLine(t,sl,t.getEyePosition(),p.getEyePosition()))Spells.keepEffect(t,MobEffects.NAUSEA,0,40);
                    }
                }
                case VIRUS_INCARNATION -> {
                    for(LivingEntity t:Spells.enemiesAround(p,sl,p.position(),12)) for(Kind kind:List.of(Kind.INFECTED,Kind.LAZY,Kind.OBESE,Kind.DEGENERATE,Kind.ROTTING,Kind.CORRUPTED,Kind.AGED))GodOfDecayMarks.put(p,t,kind,100);
                }
                case SLOW_TIME -> {
                    for(LivingEntity t:Spells.livingAround(p,sl,p.position(),24)) {
                        if(Spells.isFriend(p,t)){Spells.keepEffect(t,MobEffects.SPEED,0,25);t.getPersistentData().putLong("decay_quickened",sl.getGameTime()+25);}
                        else {Spells.keepEffect(t,MobEffects.SLOWNESS,2,25);t.getPersistentData().putLong("decay_slowed",sl.getGameTime()+25);}
                    }
                    Spells.keepEffect(p,MobEffects.SPEED,0,25);p.getPersistentData().putLong("decay_quickened",sl.getGameTime()+25);
                    GodOfDecayFx.zoneTick(sl,p.position(),24,GodOfDecayFx.TIME);
                }
                case ENTROPY -> {
                    for(LivingEntity t:Spells.enemiesAround(p,sl,p.position(),30)) {
                        s.counts.merge(t.getUUID(),1,Integer::sum);Substitute.disarm(t);DeathWard.disarm(t);
                    }
                    GodOfDecayFx.zoneTick(sl,p.position(),30,GodOfDecayFx.ENTROPY);
                }
                case RAGNAROK -> {
                    Set<UUID> inside=new HashSet<>();
                    for(LivingEntity t:Spells.enemiesAround(p,sl,p.position(),48)) {
                        inside.add(t.getUUID());int count=Math.min(100,s.counts.getOrDefault(t.getUUID(),0)+5+GodOfDecayMarks.count(p,t));s.counts.put(t.getUUID(),count);
                        GodOfDecayFx.entropyCount(p,t,count);
                        if(count>=100){GodOfDecayFx.collapse(sl,t);if(!(t instanceof Player)&&!Spells.isNaturalBoss(t)&&t.getMaxHealth()<=150)CertainDamage.strike(p,t,t.getMaxHealth()*4,null);else{CertainDamage.strike(p,t,t.getMaxHealth()*.6f,null);if(t.isAlive())doom(p,t,null);}s.counts.put(t.getUUID(),0);}
                    }
                    for(UUID id:List.copyOf(s.counts.keySet()))if(!inside.contains(id)){int n=s.counts.get(id)-10;if(n<=0)s.counts.remove(id);else s.counts.put(id,n);}
                    for(ServerPlayer viewer:sl.players())if(viewer.distanceToSqr(p)<=48*48)GodOfDecayFx.duskSky(viewer,s.tag);
                    GodOfDecayFx.zoneTick(sl,p.position(),48,GodOfDecayFx.ENTROPY);
                }
                default -> { }
            }
        }
    }
}
