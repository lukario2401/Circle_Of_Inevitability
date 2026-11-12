package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker;

import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** First twenty Nature Walker abilities, shared by players and Beyonder mobs. */
@Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class NatureWalker {
    public static final String PATHWAY="Nature Walker", PREFIX="nature_walker_";
    public record Spell(String id, int key, int sequence, int cost, int cooldown) {}
    public static final List<Spell> SPELLS=List.of(
        new Spell("thorn_seed",1,9,5,40), new Spell("heartfruit_seed",1,9,5,40),
        new Spell("treatment",2,8,100,60), new Spell("soul_suture",2,8,200,160),
        new Spell("seed_catalyzation",3,7,700,120), new Spell("quicken_life",3,7,1250,400),
        new Spell("command_flora",3,7,1250,300), new Spell("weather_ritual",3,7,1250,1200),
        new Spell("reap",3,7,700,80), new Spell("cross_breed",4,6,2450,200),
        new Spell("toxin_bloom",4,6,1500,120), new Spell("underground_travel",5,5,3000,200),
        new Spell("swamp",5,5,3000,400), new Spell("wrath_of_nature",5,5,3000,600),
        new Spell("child_of_the_oak",5,5,2450,400), new Spell("stone_golem",5,5,3500,600),
        new Spell("bear_form",5,5,3000,800), new Spell("stone_bird",6,4,4500,400),
        new Spell("mud_golem",6,4,6000,800), new Spell("steel_golem",6,4,7500,1200));
    public static final AbilityWheel BASIC=wheel("basic","Sow, Treatment and Biology");
    public static final AbilityWheel HARVEST=wheel("harvest","Harvest");
    public static final AbilityWheel EARTH=wheel("earth","Earth");
    public static final AbilityWheel ALCHEMY=wheel("alchemy","Alchemy");
    public static final List<AbilityWheel> WHEELS=List.of(BASIC,HARVEST,EARTH,ALCHEMY);
    static final Map<UUID,State> STATES=new HashMap<>();
    private static boolean initialized;

    static final class State {
        final LivingEntity owner;
        final ServerLevel level;
        final int sequence;
        final List<NatureWalkerGrowths.Plant> plants=new ArrayList<>();
        final List<NatureWalkerBrood.Companion> companions=new ArrayList<>();
        final Map<UUID,Long> reaped=new HashMap<>();
        final Map<UUID,Long> wrath=new HashMap<>();
        Boolean oakStone;
        long quickenUntil, weatherUntil, oakUntil;
        boolean raining;
        Vec3 weatherAt;
        NatureWalkerEarth.Journey journey;
        UUID observed;
        int observedTicks;
        final Map<UUID,Long> dossierReady=new HashMap<>();
        State(LivingEntity owner) { this.owner=owner;level=(ServerLevel)owner.level();sequence=sequence(owner); }
    }

    static {
        for (Spell spell:SPELLS) {
            AbilityWheel wheel=wheelFor(spell.key());
            wheel.entry(spell.id(),"ability_menus/nature_walker/"+spell.id(),label(spell.id()),spell.sequence(),
                    spell.cost(),spell.cooldown(),(p,sl)->perform(p,spell.id()))
                    // These casts own live boards, companions or pathway-only lifecycle state.
                    .notCopyable();
            if (spell.id().equals("quicken_life")) wheel.free(p -> quickening(p));
            if (spell.id().equals("underground_travel")) wheel.free(p -> travelling(p));
        }
    }
    private NatureWalker() {}
    private static AbilityWheel wheel(String id,String title) {
        return AbilityWheel.create(PREFIX+id,title).pathway(PATHWAY).quickCastCooldown(false).quickCastCost(2f);
    }
    public static AbilityWheel wheelFor(int key) { return key==3?HARVEST:key==5?EARTH:key==6?ALCHEMY:BASIC; }
    public static String label(String id) {
        StringBuilder text=new StringBuilder();
        for(String word:id.split("_")) { if(!text.isEmpty())text.append(' ');text.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)); }
        return text.toString();
    }
    public static void initialize() {
        if(initialized)return;initialized=true;
        EffectDuration.addRule((e,effect)->own(e)&&sequence(e)<=8
                &&(effect.is(MobEffects.POISON)||effect.is(MobEffects.HUNGER))?.5:1);
    }
    static boolean own(LivingEntity e) { return PATHWAY.equals(Beyonders.pathwayOf(e)); }
    static int sequence(LivingEntity e) { return Math.clamp(Beyonders.sequenceOf(e),0,9); }
    static float verdancy(LivingEntity e) { return 1+.06f*(10-sequence(e)); }
    static int duration(LivingEntity e,int base) { return Math.round(base*verdancy(e)); }
    static State state(LivingEntity e) {
        State old=STATES.get(e.getUUID());
        if(old!=null && (old.owner!=e || old.level!=e.level() || old.sequence!=sequence(e)))clear(e);
        return STATES.computeIfAbsent(e.getUUID(),id->new State(e));
    }
    static boolean valid(State s) {
        return STATES.get(s.owner.getUUID())==s && s.owner.isAlive() && !s.owner.isRemoved()
                && s.owner.level()==s.level && own(s.owner) && sequence(s.owner)==s.sequence
                && (!(s.owner instanceof ServerPlayer p)||!p.hasDisconnected());
    }
    static String tag(State s,String name) { return PREFIX+s.owner.getUUID()+"_"+name; }
    public static boolean quickening(LivingEntity e) { var s=STATES.get(e.getUUID());return s!=null&&valid(s)&&s.quickenUntil>s.level.getGameTime(); }
    public static boolean travelling(LivingEntity e) { var s=STATES.get(e.getUUID());return s!=null&&valid(s)&&s.journey!=null; }
    static boolean enemy(LivingEntity caster,LivingEntity target) {
        if(!Spells.isEnemy(caster,target)||!Spells.canBeChosen(caster,target))return false;
        if(target instanceof Player p && (p.isCreative()||p.isSpectator()))return false;
        return !(caster instanceof Player p && target instanceof Player other && !p.canHarmPlayer(other));
    }
    static List<LivingEntity> enemies(State s,Vec3 at,double radius) {
        return Spells.enemiesAround(s.owner,s.level,at,radius).stream().filter(e->enemy(s.owner,e)).toList();
    }
    static List<LivingEntity> friends(State s,Vec3 at,double radius) {
        List<LivingEntity> result=new ArrayList<>(Spells.livingAround(s.owner,s.level,at,radius).stream()
                .filter(e->Spells.isFriend(s.owner,e)).toList());
        if(s.owner.isAlive()&&Spells.distanceToBody(s.owner,at)<=radius)result.add(s.owner);
        return result;
    }
    static LivingEntity aimed(State s,double range,boolean ally) {
        return Spells.aimLiving(s.owner,s.level,range,false,e->ally?Spells.isFriend(s.owner,e):enemy(s.owner,e));
    }
    static void strike(State s,LivingEntity target,float damage,boolean brood) {
        if(!valid(s)||!enemy(s.owner,target))return;
        float bonus=brood&&s.reaped.getOrDefault(target.getUUID(),0L)>s.level.getGameTime()?1.15f:1;
        Spells.strike(s.owner,target,damage*bonus);
    }
    static void effect(State s,String model,Vec3 at,int ticks,float scale) {
        ParticleShapes.model(s.level,NatureWalkerFx.model(model,ticks,tag(s,model)).scale(scale),at,0,0,0);
    }
    static void sound(State s,String name) { NatureWalkerFx.sound(s.level,s.owner.position(),name); }
    static AbilityWheel.Result refuse(LivingEntity caster,String message) {
        Spells.tell(caster,Component.literal(message));return AbilityWheel.Result.REFUSED;
    }

    public static void key(Player player,Level level,ServerLevel sl,int key,boolean bypass) {
        if(!(player instanceof ServerPlayer p)||!own(p)||!p.isAlive())return;
        if(key==7) { p.sendOverlayMessage(Component.literal("Return abilities are not implemented yet."));return; }
        if(key<1||key>6||!Beyonder.hasSequence(p,10-key))return;
        // Cancelling the two channels does not depend on the current wheel selection or available resource.
        if(key==3&&quickening(p)) { HARVEST.cast(p,HARVEST.entry("quicken_life"));return; }
        if(key==5&&travelling(p)) { EARTH.cast(p,EARTH.entry("underground_travel"));return; }
        if(key==1||key==2||key==4) {
            String id=key==1?(p.isShiftKeyDown()?"heartfruit_seed":"thorn_seed"):
                    key==2?(p.isShiftKeyDown()?"soul_suture":"treatment"):(p.isShiftKeyDown()?"toxin_bloom":"cross_breed");
            BASIC.cast(p,BASIC.entry(id));
        } else if(p.isShiftKeyDown())wheelFor(key).quickCast(p);else wheelFor(key).open(p);
    }

    /** Payment is owned by AbilityWheel or MobCasting, after this operation succeeds. */
    static AbilityWheel.Result perform(LivingEntity caster,String id) {
        Spell spell=SPELLS.stream().filter(a->a.id().equals(id)).findFirst().orElse(null);
        if(spell==null||!(caster.level() instanceof ServerLevel)||!own(caster)||!caster.isAlive()
                ||caster.isSpectator()||sequence(caster)>spell.sequence()||Root.has(caster,RootRestriction.ABILITIES)
                ||Silence.isSilenced(caster))return AbilityWheel.Result.REFUSED;
        initialize();State s=state(caster);long now=s.level.getGameTime();
        if(id.equals("quicken_life")&&s.quickenUntil>now) { endQuicken(s);return AbilityWheel.Result.FREE; }
        if(id.equals("underground_travel")&&s.journey!=null) { NatureWalkerEarth.surface(s,true);return AbilityWheel.Result.FREE; }
        switch(id) {
            case "thorn_seed", "heartfruit_seed" -> {
                Vec3 feet=Spells.aimGround(caster,s.level,id.equals("thorn_seed")?20:6);
                if(!NatureWalkerGrowths.canPlant(s,feet))return refuse(caster,"Aim at natural ground with room for a growth.");
                if(id.equals("heartfruit_seed"))NatureWalkerGrowths.plant(s,NatureWalkerGrowths.Kind.HEARTFRUIT,feet);
                else {
                    Vec3 from=caster.getEyePosition(), direction=feet.subtract(from);
                    Shot.of(NatureWalkerFx.model("seed",30,tag(s,"seed_"+now)).scale(.3f))
                            .speed(1.6).range(direction.length()).width(0)
                            .onEnd((at,hit)-> { if(valid(s)) {
                                Vec3 ground=Spells.groundAt(caster,s.level,at);
                                if(NatureWalkerGrowths.canPlant(s,ground))NatureWalkerGrowths.plant(s,NatureWalkerGrowths.Kind.THORNLING,ground);
                            }}).fire(caster,s.level,from,direction);
                }
            }
            case "treatment" -> treatment(s);
            case "soul_suture" -> suture(s);
            case "seed_catalyzation" -> {
                Vec3 at=Spells.aimGround(caster,s.level,24);
                var plants=NatureWalkerGrowths.near(s,at,6*verdancy(caster));
                if(plants.isEmpty()) {
                    if(!NatureWalkerGrowths.canPlant(s,at))return AbilityWheel.Result.REFUSED;
                    plants=List.of(NatureWalkerGrowths.plant(s,NatureWalkerGrowths.Kind.THORNLING,at));
                }
                for(var plant:plants)NatureWalkerGrowths.ripen(s,plant);
                for(var e:enemies(s,at,sequence(caster)<=5?7:5)) {
                    strike(s,e,12,false);Root.apply(e,sequence(caster)<=5?100:60,RootRestriction.MOVE,RootRestriction.JUMP);
                    if(sequence(caster)<=5)Bleeding.apply(e,caster,60,1);
                }
                effect(s,"vine_burst",at,30,3);
            }
            case "quicken_life" -> {
                s.quickenUntil=now+duration(caster,100);
                Root.applyOwned(caster,tag(s,"quicken"),duration(caster,100),EnumSet.of(RootRestriction.MOVE,RootRestriction.JUMP));
            }
            case "command_flora" -> { if(!NatureWalkerBrood.command(s))return AbilityWheel.Result.REFUSED; }
            case "weather_ritual" -> {
                s.raining=!(s.weatherUntil>now?s.raining:s.level.isRaining());
                s.weatherAt=Spells.aimGround(caster,s.level,24);
                s.weatherUntil=now+duration(caster,s.raining?800:600);
            }
            case "reap" -> {
                for(var e:Spells.enemiesInCone(caster,s.level,caster.position(),caster.getLookAngle(),5,60))
                    if(enemy(caster,e)) { strike(s,e,30,false);s.reaped.put(e.getUUID(),now+160); }
                int harvested=0;
                for(var plant:new ArrayList<>(s.plants)) if(inCone(caster,plant.at,5,120)) {
                    NatureWalkerGrowths.harvest(s,plant);harvested++;
                }
                // Refund runs after the wheel's payment; state identity cancels stale delayed work.
                int refund=Math.min(500,harvested*100);
                if(refund>0)Later.run(s.level,1,()->{if(valid(s))Spells.giveSpirituality(caster,refund);});
                NatureWalkerFx.melee(s.level,caster,caster.position().add(caster.getLookAngle().scale(4)),false);
                ParticleShapes.modelOnTurning(s.level,NatureWalkerFx.model("harvest_scythe",20,tag(s,"scythe"))
                        .key(0,net.lukario.circleofinevitability.particles.fx.ModelFx.pose().yaw(-70))
                        .key(20,net.lukario.circleofinevitability.particles.fx.ModelFx.pose().yaw(80)),caster,Vec3.ZERO,0,0,0);
            }
            case "cross_breed" -> { if(!NatureWalkerBrood.hybrid(s))return AbilityWheel.Result.REFUSED; }
            case "toxin_bloom" -> {
                for(var e:Spells.enemiesInCone(caster,s.level,caster.getEyePosition(),caster.getLookAngle(),7,45))
                    if(enemy(caster,e)) { Spells.keepEffect(e,MobEffects.POISON,1,120);Spells.keepEffect(e,MobEffects.WEAKNESS,0,120); }
                Vec3 at=Spells.aimGround(caster,s.level,7);
                if(s.sequence<=5&&NatureWalkerGrowths.canPlant(s,at))NatureWalkerGrowths.plant(s,NatureWalkerGrowths.Kind.SPOREBED,at);
                ParticleShapes.burst(s.level,NatureWalkerFx.particle("spore",NatureWalkerFx.MUTATION),caster.getEyePosition(),28,.03,.2);
            }
            case "underground_travel","swamp","wrath_of_nature","child_of_the_oak","bear_form" -> {
                if(!NatureWalkerEarth.cast(s,id))return AbilityWheel.Result.REFUSED;
            }
            case "stone_golem","stone_bird","mud_golem","steel_golem" -> {
                if(!NatureWalkerBrood.construct(s,id))return AbilityWheel.Result.REFUSED;
            }
            default -> { return AbilityWheel.Result.REFUSED; }
        }
        sound(s,id);return AbilityWheel.Result.DONE;
    }
    static boolean inCone(LivingEntity caster,Vec3 at,double radius,double degrees) {
        Vec3 d=at.subtract(caster.position());
        return d.lengthSqr()<=radius*radius&&(d.lengthSqr()<.25||d.normalize().dot(Spells.flatLook(caster))>=Math.cos(Math.toRadians(degrees/2)));
    }
    private static void treatment(State s) {
        LivingEntity target=aimed(s,12,true);if(target==null)target=s.owner;
        boolean critical=target.getHealth()<target.getMaxHealth()*.3f;
        if(s.sequence<=4) { NoHeal.remove(target);Decay.cure(target); }
        float fraction=s.sequence<=4?(critical?1:.6f):(critical?.6f:.3f);
        Healing.apply(target,target.getMaxHealth()*fraction*verdancy(s.owner));
        Spells.cleanse(target);
        if(critical) {
            Spells.keepEffect(target,MobEffects.RESISTANCE,0,100);
            Spells.keepEffect(target,MobEffects.ABSORPTION,0,100);
        }
        if(s.sequence<=4)Spells.keepEffect(target,MobEffects.REGENERATION,2,60);
        effect(s,"healing_blossom",target.position(),30,1);
    }
    private static void suture(State s) {
        LivingEntity target=aimed(s,6,false);
        if(target!=null) { Silence.apply(target,Spells.isBoss(target)?30:60);Dispel.boons(target); }
        else {
            target=aimed(s,12,true);if(target==null)target=s.owner;
            Cleansing.cleanseStatuses(target);Fear.end(target);Slumber.wake(target);Silence.lift(target);
            Blind.remove(target);Corrosion.remove(target);Decay.cure(target);Bleeding.remove(target);NoHeal.remove(target);
            Madness.clear(target);Misfortune.clear(target);
        }
        effect(s,"soul_suture",target.position(),35,1.5f);
    }
    static void endQuicken(State s) { s.quickenUntil=0;Root.removeOwned(s.owner,tag(s,"quicken")); }

    static void tick(State s) {
        long now=s.level.getGameTime();
        if(s.quickenUntil>now&&now%20==0) {
            double radius=12*verdancy(s.owner);
            for(var plant:NatureWalkerGrowths.near(s,s.owner.position(),radius))plant.age+=40;
            for(var e:friends(s,s.owner.position(),radius))Spells.keepEffect(e,MobEffects.REGENERATION,0,30);
            for(var e:enemies(s,s.owner.position(),radius))Spells.keepEffect(e,MobEffects.SLOWNESS,0,30);
            Spells.growPlants(s.owner,s.level,s.owner.position(),radius,6);
            effect(s,"life_ring",s.owner.position(),22,(float)(radius*2));
        } else if(s.quickenUntil!=0&&s.quickenUntil<=now)endQuicken(s);
        if(s.weatherUntil>now&&now%20==0) {
            double radius=24*verdancy(s.owner);
            for(var e:friends(s,s.weatherAt,radius)) {
                if(s.raining) { e.clearFire();Spells.keepEffect(e,MobEffects.REGENERATION,0,30); }
                else { Blind.remove(e);e.removeEffect(MobEffects.DARKNESS);Spells.keepEffect(e,MobEffects.SPEED,0,30); }
            }
            if(s.raining)for(var e:Spells.livingAround(s.owner,s.level,s.weatherAt,radius))e.clearFire();
            NatureWalkerFx.weather(s.level,s.weatherAt,s.raining?"rain":"clear",22,tag(s,"weather"));
        }
        NatureWalkerGrowths.tick(s);NatureWalkerBrood.tick(s);NatureWalkerEarth.tick(s);
        NatureWalkerPassives.tick(s);
        s.reaped.values().removeIf(until->until<=now);
        if(s.owner instanceof ServerPlayer p&&now%20==0) {
            HudCounter.show(p,PREFIX+"growths","nature_walker/growths","Growths",s.plants.size(),NatureWalkerGrowths.cap(s),0xA3CA6F);
            EntitySight.tick(p);
        }
    }
    static void clear(LivingEntity owner) {
        State s=STATES.remove(owner.getUUID());if(s==null)return;
        endQuicken(s);NatureWalkerEarth.clear(s);NatureWalkerBrood.clear(s);NatureWalkerGrowths.clear(s);
        if(owner instanceof Player p) {
            HudCounter.hide(p,PREFIX+"growths");HudTimer.hide(p,PREFIX+"weather");EntitySight.clear(p);
        }
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent.Post event) {
        if(event.player() instanceof ServerPlayer p&&own(p)&&p.isAlive())state(p);
    }
    @SubscribeEvent public static void mobTick(LivingEvent.LivingTickEvent event) {
        var e=event.getEntity();if(e instanceof Mob && e.level() instanceof ServerLevel && own(e)&&e.isAlive())state(e);
    }
    @SubscribeEvent public static void levelTick(TickEvent.LevelTickEvent.Post event) {
        if(!(event.level() instanceof ServerLevel sl))return;
        for(State s:new ArrayList<>(STATES.values()))if(s.level==sl) {
            if(!valid(s))clear(s.owner);else tick(s);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) { clear(e.getEntity()); }
    @SubscribeEvent public static void death(LivingDeathEvent e) { clear(e.getEntity());NatureWalkerBrood.death(e.getEntity()); }
    @SubscribeEvent public static void leave(EntityLeaveLevelEvent e) {
        if(e.getEntity() instanceof LivingEntity living)clear(living);
        NatureWalkerBrood.leave(e.getEntity());
    }
    @SubscribeEvent public static void unload(LevelEvent.Unload e) {
        for(State s:new ArrayList<>(STATES.values()))if(s.level==e.getLevel())clear(s.owner);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { STATES.clear(); }

    public static void registerMobs() {
        for(Spell spell:SPELLS)MobAbilities.register(MobAbility.of(PATHWAY,spell.key(),false).entry(spell.id())
                .named(label(spell.id())).sequence(spell.sequence()).cost(spell.cost()).cooldown(spell.cooldown())
                .range(24).sight().cast((mob,target,sl)->{
                    if(target!=null)MobSpells.face(mob,target);
                    return perform(mob,spell.id())==AbilityWheel.Result.DONE;
                }));
    }
    public static final MeleeCombo COMBO=combo();
    private static MeleeCombo combo() {
        MeleeCombo combo=MeleeCombo.forPathway(PATHWAY);
        for(int i=0;i<8;i++) { final boolean impact=i%2==1;
            combo.step((p,t,sl)->{if(enemy(p,t))NatureWalkerFx.melee(sl,p,t.position().add(0,.8,0),impact);});
        }
        return combo;
    }
}
