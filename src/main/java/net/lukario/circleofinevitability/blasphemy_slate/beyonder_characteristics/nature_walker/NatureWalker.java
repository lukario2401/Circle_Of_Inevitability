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

/** Nature Walker abilities, shared by players and Beyonder mobs. */
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
        new Spell("mud_golem",6,4,6000,800), new Spell("steel_golem",6,4,7500,1200),
        new Spell("life_aura",6,4,6000,800),new Spell("mutate",6,4,3000,160),new Spell("brood_form",6,4,6000,3600),
        new Spell("evil_spirits",7,3,8000,600),new Spell("descent_of_death",7,3,10000,900),
        new Spell("return_to_earth",7,3,6000,600),new Spell("maternal_embrace",7,3,9000,800),
        new Spell("blossoming_land",3,2,10000,1800),new Spell("barren_land",3,2,10000,1800),
        new Spell("desolation",7,2,15000,1200),new Spell("root_eruption",7,2,12000,400),
        new Spell("seed_of_rebirth",6,2,12000,1200),new Spell("world_of_nature",5,1,25000,6000),
        new Spell("verdant_barrier",5,1,9000,1200),new Spell("great_harvest",3,0,20000,1200),
        new Spell("awaken",6,0,20000,1800),new Spell("earthquake",5,0,30000,3600),
        new Spell("embrace_of_the_earth",7,0,15000,1200),new Spell("bountiful_earth",7,0,60000,6000));
    public static final AbilityWheel BASIC=wheel("basic","Sow, Treatment and Biology");
    public static final AbilityWheel HARVEST=wheel("harvest","Harvest");
    public static final AbilityWheel EARTH=wheel("earth","Earth");
    public static final AbilityWheel ALCHEMY=wheel("alchemy","Alchemy");
    public static final AbilityWheel RETURN=wheel("return","Return");
    public static final List<AbilityWheel> WHEELS=List.of(BASIC,HARVEST,EARTH,ALCHEMY,RETURN);
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
        int lastFood=-1;double foodCredit;
        final Map<UUID,Long> dossierReady=new HashMap<>();
        final NatureWalkerAdvanced.Board advanced=new NatureWalkerAdvanced.Board();
        State(LivingEntity owner) { this.owner=owner;level=(ServerLevel)owner.level();sequence=sequence(owner); }
    }

    /** The wheel's hover text for each spell (shown when the mouse is over its icon). */
    static String description(String id) {
        return switch (id) {
            case "thorn_seed" -> "Throw a seed that plants a Thornling: enemies near it are pricked, slowed and bleed while it stands.";
            case "heartfruit_seed" -> "Plant a Heartfruit sapling. Its fruit heals whoever eats it (you or an ally standing close) and gives Regeneration.";
            case "treatment" -> "Heal the ally you aim at (or yourself) and cleanse every harmful effect. Badly hurt targets are healed far more and protected.";
            case "soul_suture" -> "Mend an ally (or yourself): end every curse and mind effect and clear Madness. Aimed at a close enemy it cuts instead, silencing it.";
            case "seed_catalyzation" -> "Your Growths near where you aim bloom at once and thorned vines burst out, hurting and rooting nearby enemies. With none near, a Thornling blooms there.";
            case "quicken_life" -> "Channel an aura around you: your Growths and crops grow fast, allies regenerate and enemies slow. You cannot move; press again to stop.";
            case "command_flora" -> "Nearby small arthropods become your marionettes, and your Growths focus the enemy you aim at, dealing more damage to it.";
            case "weather_ritual" -> "Change the local weather where you aim. Clear skies: call rain that douses fire, speeds Growths and regenerates allies. Rain: clear it, cleansing blindness and giving Speed.";
            case "reap" -> "A scythe sweep in front of you: damage, harvest your Growths in the cone for their effects and spirituality, and mark hit enemies as Reaped.";
            case "cross_breed" -> "Turn the mob you aim at into a Hybrid marionette with more health and a trait from one of your nearby Growths (which is consumed).";
            case "toxin_bloom" -> "A cone of toxin from your hair: Poison and Weakness. Later it also leaves a poisonous Sporebed and adds Madness.";
            case "underground_travel" -> "Sink into the ground, hidden and unhurt, and travel along your look in hops, bursting out to hurt and root enemies. From Sequence 1, Nature Walk: through earth, wood or water, or between your Growths.";
            case "swamp" -> "The ground where you aim turns to mire: enemies sink deeper and slower, cannot jump, and are buried once.";
            case "wrath_of_nature" -> "Name the enemies in sight as adversaries: each suffers a hazard every second that fits the ground around it.";
            case "child_of_the_oak" -> "Bark covers you. On soil it is wood (less damage, regrowing absorption); on rock it is stone (even less damage, no knockback).";
            case "stone_golem" -> "A stone golem rises where you aim and fights for you: tough, strong and immune to knockback.";
            case "bear_form" -> "Become a bear twice your size: more reach and health, less damage taken, heavy claws and a roar that frightens weak mobs. Weakness afterwards.";
            case "stone_bird" -> "A flying stone bird with ruby eyes fights for you; what it hits glows for your allies.";
            case "mud_golem" -> "A slow, tough mud golem whose blows slow and weigh enemies down. It heals in your Swamp or in water.";
            case "steel_golem" -> "A huge steel golem with very high health and damage and no knockback.";
            case "life_aura" -> "An aura of life around you: allies regenerate and speed up, your brood heals, Growths grow fast, and enemies mutate and gain Madness.";
            case "mutate" -> "Mutate the enemy you aim at and add Madness. One already at full mutation blooms: heavy damage and everything near it is rooted.";
            case "brood_form" -> "Your Mythical Creature Form, a mass of flesh, wheat and vines: bigger, mutating every enemy nearby. Madness builds until it breaks. Complete from Sequence 2.";
            case "evil_spirits" -> "The recently dead around you rise as Evil Spirits that fight for you.";
            case "descent_of_death" -> "Death descends where you aim: enemies rot, cannot heal and corrode, you heal from every bite, and your Growths inside advance.";
            case "return_to_earth" -> "Everything evil or undead around you takes heavy damage and is buried. Others are only made Heavy.";
            case "maternal_embrace" -> "An earthen coffin closes on the enemy you aim at: held, sealed, cannot heal and takes more damage, then let out weakened.";
            case "blossoming_land" -> "Land blooms around where you aim: Growths grow fast, Heartfruit sprouts, allies regenerate and gain absorption.";
            case "barren_land" -> "Land goes barren around where you aim: enemies cannot heal, are weakened, starve and decay.";
            case "desolation" -> "All life drains in a wide aura around you: enemies rot and lose health every second, undead return to the earth, nothing heals, and Madness grows.";
            case "root_eruption" -> "The ground cracks where you aim, then roots erupt: heavy damage, rooted, Madness, and weak mobs may die outright.";
            case "seed_of_rebirth" -> "Ward yourself or an aimed ally: the next death turns them into a seed instead, and they bloom back with 60% health, healing allies nearby.";
            case "world_of_nature" -> "Your realm: a sphere enemies cannot leave. Allies regenerate and grow stronger, enemies slow, go mad and sprout thorns under them.";
            case "verdant_barrier" -> "A wall of wheat, flowers and trees across your view that enemies and their projectiles cannot cross. It regrows when it stops something.";
            case "great_harvest" -> "Harvest everything near you: enemies under a quarter health die (bosses take heavy damage), the rest are Reaped, your Growths yield fully, and allies heal.";
            case "awaken" -> "Awaken one of your constructs, hybrids or Growths: a construct gets double health and damage and never runs out; a Growth becomes a Mother Tree.";
            case "earthquake" -> "The earth shakes: every enemy nearby falters, slows and is made Heavy, and a rift opens along your look that hurts and buries those on it.";
            case "embrace_of_the_earth" -> "Pierce the enemy in front of you for a quarter of its max health that nothing softens, healed to you; it cannot heal and corrodes.";
            case "bountiful_earth" -> "Your Divine Kingdom of life and death: allies regenerate, resist and get a free death ward; enemies rot, cannot heal and mutate.";
            default -> "";
        };
    }

    static {
        for (Spell spell:SPELLS) {
            AbilityWheel wheel=wheelFor(spell.key());
            wheel.entry(spell.id(),"ability_menus/nature_walker/"+spell.id(),label(spell.id()),spell.sequence(),
                    spell.cost(),spell.cooldown(),(p,sl)->perform(p,spell.id()))
                    .description(description(spell.id()))
                    // These casts own live boards, companions or pathway-only lifecycle state.
                    .notCopyable();
            if (spell.id().equals("quicken_life")) wheel.free(p -> quickening(p));
            if (spell.id().equals("underground_travel")) wheel.free(p -> travelling(p)).costBy((p,c)->sequence(p)<=1?c*2:c);
        }
    }
    static { ALCHEMY.cooldownBy((p,t)->t==3600&&sequence(p)>2?1800:t); }
    private NatureWalker() {}
    private static AbilityWheel wheel(String id,String title) {
        return AbilityWheel.create(PREFIX+id,title).pathway(PATHWAY).quickCastCooldown(false).quickCastCost(2f);
    }
    public static AbilityWheel wheelFor(int key) { return key==3?HARVEST:key==5?EARTH:key==6?ALCHEMY:key==7?RETURN:BASIC; }
    public static String label(String id) {
        StringBuilder text=new StringBuilder();
        for(String word:id.split("_")) { if(!text.isEmpty())text.append(' ');text.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)); }
        return text.toString();
    }
    public static void initialize() {
        if(initialized)return;initialized=true;
        NatureWalkerAdvanced.initialize();
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
        Mutation.Kind before=NatureWalkerPassives.HIT_KIND.get();
        NatureWalkerPassives.HIT_KIND.set(brood?Mutation.Kind.VINES:Mutation.Kind.ORGANS);
        try { Spells.strike(s.owner,target,damage*bonus); }finally { NatureWalkerPassives.HIT_KIND.set(before); }
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
        if(key<1||key>7||!Beyonder.hasSequence(p,10-key))return;
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
                List<Vec3> wheatSpots=new ArrayList<>();
                for(var e:Spells.enemiesInCone(caster,s.level,caster.position(),caster.getLookAngle(),5,60))
                    if(enemy(caster,e)) {
                        boolean wheat=Mutation.kind(caster,e)==Mutation.Kind.WHEAT;
                        strike(s,e,30,false);s.reaped.put(e.getUUID(),now+160);
                        if(wheat)wheatSpots.add(Spells.groundAt(caster,s.level,e.position()));
                    }
                int harvested=0;
                for(var plant:new ArrayList<>(s.plants)) if(inCone(caster,plant.at,5,120)) {
                    NatureWalkerGrowths.harvest(s,plant);harvested++;
                }
                for(var at:wheatSpots)NatureWalkerGrowths.plant(s,NatureWalkerGrowths.Kind.HEARTFRUIT,at);
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
                    if(enemy(caster,e)) { Spells.keepEffect(e,MobEffects.POISON,1,120);Spells.keepEffect(e,MobEffects.WEAKNESS,0,120);if(s.sequence<=4)Mutation.hit(caster,e,Mutation.Kind.FUNGUS,1); }
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
            default -> { if(!NatureWalkerAdvanced.cast(s,id))return AbilityWheel.Result.REFUSED; }
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
        Healing.apply(s.owner,target,target.getMaxHealth()*fraction*verdancy(s.owner));
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
        if(target!=null) { Silence.apply(target,Spells.isBoss(target)?30:60);Dispel.boons(target);if(s.sequence<=4)Mutation.hit(s.owner,target,Mutation.Kind.WHEAT,1); }
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
            for(var plant:NatureWalkerGrowths.near(s,s.owner.position(),radius))plant.clock.advance(40);
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
            if(s.raining&&s.sequence<=1&&now%40==0) {
                var foes=enemies(s,s.weatherAt,radius);
                if(!foes.isEmpty()) { strike(s,foes.getFirst(),30,false);effect(s,"lightning",foes.getFirst().position(),15,2); }
            }
            if(s.raining)for(var e:Spells.livingAround(s.owner,s.level,s.weatherAt,radius))e.clearFire();
            NatureWalkerFx.weather(s.level,s.weatherAt,s.raining?"rain":"clear",22,tag(s,"weather"));
        }
        NatureWalkerGrowths.tick(s);NatureWalkerBrood.tick(s);NatureWalkerEarth.tick(s);
        NatureWalkerPassives.tick(s);NatureWalkerAdvanced.tick(s);
        s.reaped.values().removeIf(until->until<=now);
        if(s.owner instanceof ServerPlayer p&&now%20==0) {
            HudCounter.show(p,PREFIX+"growths","nature_walker/growths","Growths",(int)s.plants.stream().filter(g->!g.temporary).count(),NatureWalkerGrowths.cap(s),0xA3CA6F);
            EntitySight.tick(p);
        }
    }
    static void clear(LivingEntity owner) {
        State s=STATES.remove(owner.getUUID());if(s==null)return;
        endQuicken(s);NatureWalkerAdvanced.clear(s);Mutation.clearSource(s.owner);NatureWalkerEarth.clear(s);NatureWalkerBrood.clear(s);NatureWalkerGrowths.clear(s);
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
                .costBy(mob->spell.cost()*(spell.id().equals("underground_travel")&&sequence(mob)<=1?2:1))
                .cooldownBy(mob->spell.id().equals("brood_form")&&sequence(mob)>2?1800:spell.cooldown())
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
