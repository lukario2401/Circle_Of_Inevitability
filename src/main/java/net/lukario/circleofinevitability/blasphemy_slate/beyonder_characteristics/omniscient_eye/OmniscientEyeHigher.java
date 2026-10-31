package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye;

import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.particles.fx.*;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Prophet through White Tower. Shared mechanics live in combat/ and status/, not in this pathway. */
@Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class OmniscientEyeHigher {
    public static final String FORM="omniscient_bookshelf", KINGDOM="omniscient_kingdom";
    public static final AbilityWheel FATE=AbilityWheel.create("omniscient_eye_fate","Fate and Cognizing").pathway(OmniscientEye.PATHWAY)
        .entry("prophecy","ability_menus/omniscient_eye/prophecy","Prophecy",4,8000,800,OmniscientEyeHigher::prophecy)
        .entry("bookshelf_form","ability_menus/omniscient_eye/bookshelf_form","Bookshelf Form",4,9000,1200,OmniscientEyeHigher::bookshelf)
            .free(p->Form.isIn(p,FORM)).notCopyable()
        .entry("cognize","ability_menus/omniscient_eye/cognize","Cognize",3,10000,600,OmniscientEyeHigher::cognize)
        .entry("leverage","ability_menus/omniscient_eye/leverage","Leverage",3,12000,700,OmniscientEyeHigher::leverage)
        .entry("decipher","ability_menus/omniscient_eye/decipher","Decipher",3,9000,500,OmniscientEyeHigher::decipher);
    public static final AbilityWheel OMNISCIENCE=AbilityWheel.create("omniscient_eye_omniscience","Omniscience").pathway(OmniscientEye.PATHWAY)
        .entry("evade_disaster","ability_menus/omniscient_eye/evade_disaster","Evade Disaster",2,14000,900,OmniscientEyeHigher::evade)
        .entry("wise_counsel","ability_menus/omniscient_eye/wise_counsel","Wise Counsel",2,16000,1200,OmniscientEyeHigher::counsel)
        .entry("omniscience","ability_menus/omniscient_eye/omniscience","Omniscience",1,20000,600,OmniscientEyeHigher::omniscience)
        .entry("exploit","ability_menus/omniscient_eye/exploit","Exploit",1,22000,800,OmniscientEyeHigher::exploit)
        .entry("redefine","ability_menus/omniscient_eye/redefine","Redefine",1,18000,1000,OmniscientEyeHigher::redefine)
        .entry("calamity_of_knowledge","ability_menus/omniscient_eye/calamity_of_knowledge","Calamity of Knowledge",0,40000,1800,OmniscientEyeHigher::calamity)
        .entry("open_source","ability_menus/omniscient_eye/open_source","Open Source",0,25000,2400,OmniscientEyeHigher::openSource).notCopyable()
        .entry("tower_of_omniscience","ability_menus/omniscient_eye/tower_of_omniscience","Tower of Omniscience",0,60000,3600,OmniscientEyeHigher::tower)
            .free(p->Zone.isOpen(p,KINGDOM)).notCopyable();

    static {
        EffectDuration.addRule((entity,effect)->entity instanceof Player p && nativePath(p) && SoulCore.getSequence(p)<=8
                && List.of(MobEffects.NAUSEA,MobEffects.BLINDNESS,MobEffects.DARKNESS).contains(effect.getEffect())?.75:1);
        Madness.onBreak((source,target)->{if(target instanceof Player p && Form.isIn(p,FORM)){
            Form.end(p);Spells.keepEffect(p,MobEffects.WEAKNESS,0,100);Spells.keepEffect(p,MobEffects.SLOWNESS,0,100);
        }});
        Casts.addAnyCasterListener(OmniscientEyeHigher::observed);
        Spells.addCostScale(p->p.getPersistentData().getLongOr("omniscient_counsel",0)>p.level().getGameTime()?.7f:1f);
        Spells.addCostScale(p->p.getPersistentData().getLongOr("omniscient_tower_discount",0)>p.level().getGameTime()?.75f:1f);
        Spells.addCostScale(p->{
            if(p.level().isClientSide())return 1f;
            for(var owner:((ServerLevel)p.level()).players())if(owner!=p && nativePath(owner) && SoulCore.getSequence(owner)==0
                    && Spells.isEnemy(owner,p) && owner.distanceToSqr(p)<=24*24){
                String id=CastContext.id(p);if(!id.isEmpty() && Codex.study(owner,id)>=100)return 1.25f;
            }
            return 1f;
        });
    }
    public static void initialize() {} // eager listener installation from OmniscientEye
    private static boolean nativePath(Player p){return SoulCore.getPathway(p).equals(OmniscientEye.PATHWAY);}
    private static LivingEntity target(ServerPlayer p,double range){return Spells.aimLiving(p,p.level(),range,false,e->!Spells.isFriend(p,e));}
    private static AbilityWheel.Result done(ServerPlayer p,String sound){OmniscientEye.sound(p,sound);return AbilityWheel.Result.DONE;}
    private static void inspect(ServerPlayer p,LivingEntity target,int depth){
        Dossier.tell(p,target,depth);
        p.sendSystemMessage(Component.literal("Weakness: "+OmniscientEye.weakness(target)));
        OmniscientEye.show((ServerLevel)target.level(),target,"info_card",120);
    }
    public static void studyEntity(ServerPlayer p,LivingEntity target,double amount){
        for(var power:AbilityCatalog.of(target))Codex.learn(p,power,amount);
    }
    public static boolean kingdom(Player p){return Zone.isOpen(p,KINGDOM) && Zone.covers(p,KINGDOM,p.position());}
    public static float replicationCost(ServerPlayer p){return kingdom(p)?1:1.5f;}
    public static void replicated(ServerPlayer p){
        if(Form.isIn(p,FORM) && SoulCore.getSequence(p)>2)Madness.add(p,p,1);
    }
    private static AbilityWheel.Result prophecy(ServerPlayer p,ServerLevel sl){
        int ticks=Beyonder.hasSequence(p,2)?400:240;
        Prediction.grant(p,ticks);HudTimer.show(p,"omniscient_prophecy","Prophecy",ticks,0xD1A94B);
        return done(p,"prophecy");
    }
    private static AbilityWheel.Result bookshelf(ServerPlayer p,ServerLevel sl){
        if(Form.isIn(p,FORM)){Form.end(p);return AbilityWheel.Result.FREE;}
        int seq=SoulCore.getSequence(p),ticks=seq<=2?1200:seq<=3?800:600;
        String model=seq<=1?"spiral_tower":seq<=2?"phantom_tower":"bookshelf_form";
        Form.of(FORM).lasts(ticks).cutsDamage(.3f).effect(MobEffects.RESISTANCE,0)
            .attribute(Attributes.MAX_HEALTH,8,AttributeModifier.Operation.ADD_VALUE)
            .keepWhile(who->nativePath(who)&&SoulCore.getSequence(who)<=4)
            .everySecond(who->{
                ServerPlayer caster=(ServerPlayer)who;int ownSeq=SoulCore.getSequence(who);
                for(var enemy:Spells.enemiesAround(who,caster.level(),who.position(),12)){
                    OmniscientEye.mark(who,enemy,40,.1f);studyEntity(caster,enemy,1);
                }
                if(ownSeq>2 && who.tickCount%(ownSeq<=3?40:20)==0)Madness.add(who,who,1);
                HudCounter.show(who,"omniscient_madness","omniscient_eye/madness","Madness",Madness.get(who),10,0x733D9D);
                if(ownSeq<=1){var enemy=Spells.aimEnemy(who,caster.level(),32);if(enemy!=null){studyEntity(caster,enemy,10);ParticleShapes.line(caster.level(),ParticleFx.of("fx/omniscient_eye/thread").color(0xFFD1A94B).glow(),who.getEyePosition().add(0,3,0),enemy.getEyePosition(),2);}}
            }).onEnd((who,why)->{
                ParticleShapes.clearModels(who.level(),who,"omniscient_form");HudCounter.hide(who,"omniscient_madness");
                Madness.clear(who);
            }).take(p);
        ParticleShapes.modelOnTurning(sl,OmniscientEye.model(model,ticks).tag("omniscient_form").color(0xAAFFFFFF).scale(seq<=2?3.2f:1.2f),p,Vec3.ZERO,0,0,0);
        return done(p,"bookshelf_form");
    }
    private static AbilityWheel.Result cognize(ServerPlayer p,ServerLevel sl){
        var enemies=Spells.livingAround(p,sl,p.position(),32).stream().filter(e->!Spells.isFriend(p,e) && p.hasLineOfSight(e)
                && e.getEyePosition().subtract(p.getEyePosition()).normalize().dot(p.getLookAngle())>.5).toList();
        if(enemies.isEmpty())return AbilityWheel.Result.REFUSED;
        for(var e:enemies){Exposed.apply(e,120);OmniscientEye.mark(p,e,400,.25f);inspect(p,e,3);studyEntity(p,e,50);}
        return done(p,"cognize");
    }
    private static AbilityWheel.Result leverage(ServerPlayer p,ServerLevel sl){
        var enemy=target(p,32);if(enemy==null)return AbilityWheel.Result.REFUSED;
        boolean hold=Appraisal.bonus(p,enemy)>=.25f && !Spells.isBoss(enemy);
        Dispel.statuses(enemy);Vulnerable.add(enemy,.3f,.3f,160);Weight.put(enemy,3,120);
        if(hold)Root.apply(enemy,40,RootRestriction.MOVEMENT);
        return done(p,"leverage");
    }
    private static AbilityWheel.Result decipher(ServerPlayer p,ServerLevel sl){
        Vec3 from=p.getEyePosition(),to=from.add(p.getLookAngle().scale(24));
        for(var dome:Dome.crossedBy(sl,from,to))if(Spells.isEnemy(p,dome.owner())){
            if(Dome.flip(dome.owner(),dome.name()))return done(p,"decipher");
        }
        Vec3 point=Spells.aimPoint(p,sl,24);
        if(Zone.closeNear(sl,point,2,owner->Spells.isEnemy(p,owner))>0)return done(p,"decipher");
        var body=Spells.aimLiving(p,sl,24,false,e->true);
        if(body==null)return AbilityWheel.Result.REFUSED;
        boolean bound=Root.isRooted(body)||AbilityLock.isLocked(body) || body instanceof Player player && java.util.stream.IntStream.rangeClosed(1,7).anyMatch(k->KeySeal.isSealed(player,k));
        if(!bound)return AbilityWheel.Result.REFUSED;
        Unbound.free(body,1,true);AbilityLock.lift(body);if(body instanceof Player player)KeySeal.unsealAll(player);
        return done(p,"decipher");
    }
    private static void escape(LivingEntity saved,ServerLevel sl){
        Vec3 best=null;double score=-1;
        for(int i=0;i<8;i++){
            Vec3 preferred=saved.position().add(Math.cos(i*Math.PI/4)*6,0,Math.sin(i*Math.PI/4)*6);
            Vec3 spot=Spells.freeSpotNear(saved,sl,saved.position(),preferred,2);
            if(spot==null || spot.distanceToSqr(saved.position())>36)continue;
            double safety=Spells.enemiesAround(saved,sl,spot,12).stream().mapToDouble(e->e.position().distanceToSqr(spot)).min().orElse(144);
            if(safety>score){score=safety;best=spot;}
        }
        if(best!=null)saved.teleportTo(best.x,best.y,best.z);
    }
    private static AbilityWheel.Result evade(ServerPlayer p,ServerLevel sl){
        Unbound.free(p,300,true);
        DamageWard.grant(p,"omniscient_evade",300,(source,amount)->source.getEntity()==null && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY));
        Substitute.armWhen(p,300,1,(source,amount)->amount>p.getMaxHealth()*.5f,(saved,source,amount,level)->escape(saved,level));
        return done(p,"evade_disaster");
    }
    private static AbilityWheel.Result counsel(ServerPlayer p,ServerLevel sl){
        Zone.around(p,24).lasts(400).every(20).affects(e->false).onPulse(left->{
            List<LivingEntity> friends=new ArrayList<>(Spells.alliesAround(p,sl,p.position(),24));friends.add(p);
            for(var friend:friends){Prediction.grant(friend,25);friend.getPersistentData().putLong("omniscient_counsel",sl.getGameTime()+25);friend.getPersistentData().putString("omniscient_counsel_owner",p.getUUID().toString());}
            for(var enemy:Spells.enemiesAround(p,sl,p.position(),24))OmniscientEye.mark(p,enemy,40,.2f);
        }).open(p,sl,"omniscient_counsel");
        return done(p,"wise_counsel");
    }
    private static AbilityWheel.Result omniscience(ServerPlayer p,ServerLevel sl){
        LivingEntity enemy=Spells.aimLiving(p,sl,200,true,e->!Spells.isFriend(p,e));
        if(enemy==null)enemy=Connection.latest(p,Connection.KEEP_TICKS);
        if(enemy==null || !enemy.isAlive() || Spells.isFriend(p,enemy))return AbilityWheel.Result.REFUSED;
        OmniscientEye.mark(p,enemy,400,.35f);inspect(p,enemy,3);studyEntity(p,enemy,50);
        p.sendSystemMessage(Component.literal("Location: "+enemy.blockPosition()+" in "+enemy.level().dimension()));
        if(enemy.level()==sl)SpellFx.tether(sl,SlashFx.line("fx/omniscient_eye/thread").color(0xFFD1A94B),p,enemy,400);
        return done(p,"omniscience");
    }
    private static AbilityWheel.Result exploit(ServerPlayer p,ServerLevel sl){
        var e=target(p,32);if(e==null)return AbilityWheel.Result.REFUSED;String flaw;
        if(Construct.isConstruct(e)||Marionette.isSummoned(e)){flaw="summoned";Dispel.unmake(e);}
        else if(Substitute.isArmed(e)||DeathWard.isArmed(e)||DamageLink.redirectTarget(e)!=null||DamageLink.isShared(e)){flaw="warded";Dispel.statuses(e);Vulnerable.add(e,.3f,.3f,200);}
        else if(Concealment.isHidden(e)||Unnoticed.isUnnoticed(e)||e.isInvisible()){flaw="hiding";Exposed.apply(e,200);}
        else if(e.fireImmune()){flaw="fire creature";Root.apply(e,60,RootRestriction.MOVEMENT);}
        else if(Spells.isEvil(e)){flaw="evil or undead";Appraisal.strike(p,e,60,"light");Spells.keepEffect(e,MobEffects.WEAKNESS,1,160);}
        else if(e.getArmorValue()>=10){flaw="heavy armor";Corrosion.apply(e,p,3,1,200);}
        else if(e.hasEffect(MobEffects.REGENERATION)||e.hasEffect(MobEffects.HEALTH_BOOST)){flaw="healing";NoHeal.apply(e,200);}
        else if(Beyonders.isBeyonder(e)){flaw="last ability";if(e instanceof Player player){int key=KeySeal.lastUsed(player);if(key>0)KeySeal.seal(player,key,200);else AbilityLock.apply(e,200);}else AbilityLock.apply(e,200);}
        else {flaw="old wound";Bleeding.apply(e,p,160,5);}
        p.sendSystemMessage(Component.literal("Exploited: "+flaw));return done(p,"exploit");
    }
    private static AbilityWheel.Result redefine(ServerPlayer p,ServerLevel sl){
        var e=Spells.aimLiving(p,sl,32,false,body->true);if(e==null)return AbilityWheel.Result.REFUSED;
        if(Spells.isFriend(p,e)){
            if(!(e instanceof Player player))return AbilityWheel.Result.REFUSED;
            NextCast.arm(player,1200,0,1.3f);
        }else if(e instanceof Player player){
            int key=KeySeal.lastUsed(player);if(key==0)return AbilityWheel.Result.REFUSED;
            KeySeal.seal(player,key,300);
        }else {if(!Beyonders.isBeyonder(e))return AbilityWheel.Result.REFUSED;AbilityLock.apply(e,120);}
        return done(p,"redefine");
    }
    private static AbilityWheel.Result calamity(ServerPlayer p,ServerLevel sl){
        var enemies=Spells.enemiesAround(p,sl,p.position(),24);if(enemies.isEmpty())return AbilityWheel.Result.REFUSED;
        for(var e:enemies){
            Spells.keepEffect(e,MobEffects.NAUSEA,0,60);
            if(!Appraisal.isMarked(p,e))continue;
            Madness.add(p,e,5);Spells.strike(p,e,e.getMaxHealth()*(Spells.isBoss(e)?.08f:.25f));
            for(var slot:EquipmentSlot.values())if(slot.isArmor()){
                var item=e.getItemBySlot(slot);if(item.isDamageableItem()){
                    int damage=item.getDamageValue()+Math.max(1,item.getMaxDamage()/3);
                    if(damage>=item.getMaxDamage())item.shrink(1);else item.setDamageValue(damage);
                }
            }
        }
        return done(p,"calamity_of_knowledge");
    }
    private static AbilityWheel.Result openSource(ServerPlayer p,ServerLevel sl){
        var cast=Casts.lastPerformed(p);if(cast==null || !cast.copyable())return AbilityWheel.Result.REFUSED;
        var allies=Spells.alliesAround(p,sl,p.position(),24).stream().filter(e->e instanceof ServerPlayer).limit(3).toList();
        if(allies.isEmpty())return AbilityWheel.Result.REFUSED;
        for(var ally:allies)AbilityGrants.give((ServerPlayer)ally,cast,1200,3,.5f);
        return done(p,"open_source");
    }
    private static AbilityWheel.Result tower(ServerPlayer p,ServerLevel sl){
        if(Zone.isOpen(p,KINGDOM)){Zone.close(p,KINGDOM);return AbilityWheel.Result.FREE;}
        Zone.around(p,40).height(24).lasts(1200).every(20).affects(e->false).onPulse(left->{
            for(var enemy:Spells.livingAround(p,sl,p.position(),40))if(!Spells.isFriend(p,enemy)){
                Exposed.apply(enemy,40);OmniscientEye.mark(p,enemy,40,.35f);if(left%40==0)Madness.add(p,enemy,1);
            }
            for(var ally:Spells.alliesAround(p,sl,p.position(),40))ally.getPersistentData().putLong("omniscient_tower_discount",sl.getGameTime()+25);
        }).onEnd(()->ParticleShapes.clearModels(sl,p,"omniscient_tower")).open(p,sl,KINGDOM);
        ParticleShapes.modelOnTurning(sl,OmniscientEye.model("spiral_tower",1200).scale(3.2f).tag("omniscient_tower").color(0x66FFFFFF),p,Vec3.ZERO,0,0,0);
        for(int i=0;i<12;i++){
            double angle=i*Math.PI/6;Vec3 at=p.position().add(Math.cos(angle)*20,0,Math.sin(angle)*20);
            ParticleShapes.modelOn(sl,OmniscientEye.model("kingdom_bookshelf",1200).scale(2).tag("omniscient_tower").color(0x44FFFFFF),p,at.subtract(p.position()),0,0,0);
        }
        return done(p,"tower_of_omniscience");
    }
    private static void observed(LivingEntity caster,ServerLevel sl,Casts.Cast cast){
        if(!cast.copyable())return;
        for(var owner:sl.players())if(owner!=caster && Codex.enabled(owner) && Zone.covers(owner,KINGDOM,caster.position()))Codex.learn(owner,AbilityCatalog.all().get(Codex.id(cast)),100);
    }
    @SubscribeEvent public static void hurt(LivingHurtEvent event){
        var body=event.getEntity();if(!(body.level() instanceof ServerLevel sl)||!(event.getSource().getEntity() instanceof LivingEntity enemy))return;
        if(body.getPersistentData().getLongOr("omniscient_counsel",0)<=sl.getGameTime())return;
        try {var owner=Connection.find(sl.getServer(),UUID.fromString(body.getPersistentData().getStringOr("omniscient_counsel_owner","")));
            if(owner!=null && Appraisal.isMarked(owner,enemy))event.setAmount(event.getAmount()*.8f);
        }catch(IllegalArgumentException ignored){}
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent.Post event){
        if(!(event.player() instanceof ServerPlayer p)||p.tickCount%20!=0)return;
        if(Prediction.active(p)){
            var line=ParticleFx.of("fx/omniscient_eye/ghost_line").glow().lifetime(25);
            for(var shot:Spells.hostileShotsAround(p,p.level(),p.position(),32))ParticleShapes.line(p.level(),line,shot.position(),shot.position().add(shot.getDeltaMovement().scale(20)),2);
            for(var e:Spells.enemiesAround(p,p.level(),p.position(),4))if(e instanceof Mob mob && mob.getTarget()==p)OmniscientEye.show(p.level(),p,"ghost_outline",20);
        }
        boolean own=nativePath(p);int seq=SoulCore.getSequence(p);
        Physique.keep(p,Attributes.MAX_HEALTH,Physique.id("omniscient_demigod"),4,AttributeModifier.Operation.ADD_VALUE,own&&seq<=4);
        Physique.keep(p,Attributes.ENTITY_INTERACTION_RANGE,Physique.id("omniscient_reach"),.5,AttributeModifier.Operation.ADD_VALUE,own&&seq<=7);
        Physique.keep(p,Attributes.ATTACK_SPEED,Physique.id("omniscient_attack"),.15,AttributeModifier.Operation.ADD_MULTIPLIED_BASE,own&&seq<=7);
        if(!nativePath(p)){if(Zone.isOpen(p,KINGDOM))Zone.close(p,KINGDOM);return;}
        if(seq<=7)Spells.keepEffect(p,MobEffects.SPEED,0,25);
        if(seq>0 && Zone.isOpen(p,KINGDOM))Zone.close(p,KINGDOM);
        if(seq<=5 && Zone.covers(p,"omniscient_rite",p.position()))Spells.giveSpirituality(p,Spells.spiritualityRegenOf(p)*5);
        if(seq<=3)for(var e:Spells.livingAround(p,p.level(),p.position(),8))if(!Spells.isFriend(p,e)&&p.hasLineOfSight(e))Exposed.apply(e,25);
        if(seq<=8)for(var e:Spells.livingAround(p,p.level(),p.position(),6))if(e.isInvisible()&&!Spells.isFriend(p,e))Spells.keepEffect(e,MobEffects.GLOWING,0,25);
        if(seq<=2 && !Substitute.isArmed(p) && p.getPersistentData().getLongOr("omniscient_foreseen_ready",0)<=p.level().getGameTime()){
            Substitute.armWhen(p,620,1,(source,amount)->amount>p.getMaxHealth()*.5f,(saved,source,amount,sl)->{escape(saved,sl);p.getPersistentData().putLong("omniscient_foreseen_ready",sl.getGameTime()+600);});
        }
        if(seq<=1)for(var e:Spells.enemiesAround(p,p.level(),p.position(),16))if(p.hasLineOfSight(e))studyEntity(p,e,5);
    }
}
