package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.filthy_monarch;

import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.particles.fx.*;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** First 18 Filthy Monarch abilities. The remaining 17 have assets, but no cast entries. */
@Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class FilthyMonarch {
    public static final String PATHWAY="Filthy Monarch", DEVIL="filthy_monarch_devil", LIQUID="filthy_monarch_liquid",
            SUMMONS="filthy_monarch_projections", OFFERINGS="filthy_monarch_offerings";
    public static final AbilityWheel BASIC=wheel("basic","Criminal, Unwinged Angel and Serial Killer").noCooldownsWhen(p->true);
    public static final AbilityWheel DEVIL_ARTS=wheel("devil_arts","Devil Arts");
    public static final AbilityWheel DESIRE=wheel("desire_and_malice","Desire and Malice");
    public static final List<AbilityWheel> WHEELS=List.of(BASIC,DEVIL_ARTS,DESIRE);
    private static final Set<UUID> BORROWED=new HashSet<>();
    static final Map<UUID,State> STATES=new HashMap<>();
    private static boolean initialized;
    static {
        entry(BASIC,"lawless_lunge","Lawless Lunge",9,10,0,"Lunge at a visible enemy, strike with your held item and stagger it.");
        entry(BASIC,"dirty_trick","Dirty Trick",9,10,0,"Blind an enemy and make 40% of its attacks miss for 3 s.");
        entry(BASIC,"sulfur_flame","Sulfur Flame",8,200,0,"A cone of blue fire: damage, burn, poison and Depravity.");
        entry(BASIC,"sink","Sink",8,200,0,"Slow an enemy, stop jumping and slam airborne targets down.");
        entry(BASIC,"killing_spree","Killing Spree",7,700,0,"Three bleeding cuts, then speed and 25% more damage.");
        entry(BASIC,"devil_worship","Devil Worship",7,900,0,"Summon a Devil projection strengthened by recent Offerings.");
        entry(DEVIL_ARTS,"sulfur_fireball","Sulfur Fireball",6,1200,80,"Fire 3 sulfur fireballs, or 12 in Devil Form (20 in Demon Form).");
        entry(DEVIL_ARTS,"devil_form","Devil Form",6,2500,600,"Become a Devil for 60 s: strength, resistance, speed, reach and burning blows.");
        DEVIL_ARTS.free(p->Form.isIn(p,DEVIL));
        entry(DEVIL_ARTS,"sword_of_lava","Sword of Lava",6,1800,120,"An armor-piercing 85-damage sweep with a blade of blue lava.");
        entry(DEVIL_ARTS,"flame_cage","Flame Cage",6,1800,200,"Hold and burn an enemy, or raise a screen against enemy projectiles.");
        entry(DEVIL_ARTS,"volcanic_eruption","Volcanic Eruption",6,2500,240,"Warn for 1 s, then erupt: 80–120 damage, launch and lingering fire.");
        entry(DEVIL_ARTS,"toxic_vapor","Toxic Vapor",6,1500,200,"Poisonous fog for 10 s; from Demon it corrodes and wears armor.");
        entry(DEVIL_ARTS,"hellfire","Hellfire",4,6000,300,"Warn, then strike up to eight visible enemies with soul-burning pillars.");
        entry(DEVIL_ARTS,"flames_of_the_abyss","Flames of the Abyss",1,30000,400,"Channel a torrent of fire, poison, corrosion and Depravity.");
        entry(DESIRE,"shadow_shift","Shadow-Shift",5,2500,120,"Flow through shadows, rake enemies and leave an exploding afterimage.");
        entry(DESIRE,"desire_control","Desire Control",5,3000,200,"Plant 3 Seeds, unbalance an enemy and erupt its desire at 5 Seeds.");
        entry(DESIRE,"mental_shock","Mental Shock",5,3500,240,"An armor-piercing mental cone: 55 damage, stagger, fear and a Seed.");
        entry(DESIRE,"desire_incarnation","Desire Incarnation",5,4000,400,"Become liquid for 6 s: 80% less damage, speed and a damaging aura.");
        DESIRE.free(p->Form.isIn(p,LIQUID));
    }
    private FilthyMonarch() {}
    private static AbilityWheel wheel(String id,String title) {
        return AbilityWheel.create("filthy_monarch_"+id,title).pathway(PATHWAY).quickCastCooldown(false).quickCastCost(1);
    }
    private static void entry(AbilityWheel wheel,String id,String title,int seq,int cost,int cooldown,String description) {
        wheel.entry(id,"ability_menus/filthy_monarch/"+id,title,seq,cost,cooldown,(p,sl)->perform(p,id))
                .description(description).notCopyable();
    }
    public static void initialize() {
        if(initialized)return;initialized=true;
        Cleansing.addListener(target->{
            for(State s:List.copyOf(STATES.values())) {
                Depravity.clear(s.player,target);s.marked.remove(target.getUUID());s.burns.remove(target.getUUID());
                FilthyMonarchFx.end(s.level,target,target.position(),s.tag("seeds_"+target.getUUID()));
            }
        });
        Spells.addEvilCheck(e->e instanceof Player p && own(p));
    }
    static boolean own(Player p) { return PATHWAY.equals(SoulCore.getPathway(p)); }
    static final class State {
        final ServerPlayer player;
        final ServerLevel level;
        final String pathway;
        final int sequence;
        final String token="filthy_"+UUID.randomUUID();
        final ArrayDeque<Long> offerings=new ArrayDeque<>();
        final Map<UUID,LivingEntity> marked=new HashMap<>();
        final Map<UUID,Burn> burns=new HashMap<>();
        final List<Pending> pending=new ArrayList<>();
        final List<FilthyMonarchDevil.Ball> balls=new ArrayList<>();
        final Map<String,FilthyMonarchDevil.Field> fields=new HashMap<>();
        final Map<UUID,LivingEntity> controlled=new HashMap<>();
        final Map<UUID,Long> noSprint=new HashMap<>(), controlUntil=new HashMap<>();
        final Map<UUID,String> taunts=new HashMap<>();
        long spreeUntil,channelUntil,channelNext;
        Vec3 channelDirection;
        UUID meleeTarget;
        int meleeHits,offeringHud=-1;
        State(ServerPlayer p) {player=p;level=p.level();pathway=SoulCore.getPathway(p);sequence=SoulCore.getSequence(p);}
        String tag(String name) {return token+"_"+name;}
        long now() {return level.getGameTime();}
    }
    record Pending(long at,Runnable action) {}
    record Burn(LivingEntity target,long until,long next,float damage,boolean soul) {}
    static State state(ServerPlayer p) {
        State s=STATES.get(p.getUUID());
        if(s!=null&&!valid(s)){clear(p);s=null;}
        if(s==null){s=new State(p);STATES.put(p.getUUID(),s);}return s;
    }
    static boolean valid(State s) {
        return STATES.get(s.player.getUUID())==s && s.player.isAlive() && !s.player.isRemoved()
                && s.player.level()==s.level && s.pathway.equals(SoulCore.getPathway(s.player))
                && s.sequence==SoulCore.getSequence(s.player);
    }
    static void later(State s,int ticks,Runnable action) {s.pending.add(new Pending(s.now()+ticks,action));}
    static boolean enemy(State s,LivingEntity t) {
        return t!=null&&t.isAlive()&&!t.isRemoved()&&t.level()==s.level&&Spells.isEnemy(s.player,t)
                && (!(t instanceof Player p)||!p.isCreative()&&!p.isSpectator()&&s.player.canHarmPlayer(p));
    }
    static LivingEntity aim(State s,double reach) {
        return Spells.aimLiving(s.player,s.level,reach,false,t->enemy(s,t)&&Spells.canBeChosen(s.player,t));
    }
    static List<LivingEntity> around(State s,Vec3 at,double radius) {
        return Spells.enemiesAround(s.player,s.level,at,radius).stream().filter(t->enemy(s,t)).toList();
    }
    static List<LivingEntity> cone(State s,double range,double halfAngle) {
        return Spells.enemiesInSight(s.player,s.level,range,halfAngle).stream().filter(t->enemy(s,t)).toList();
    }
    static AbilityWheel.Result refuse(ServerPlayer p,String message) {
        p.sendOverlayMessage(Component.literal(message));return AbilityWheel.Result.REFUSED;
    }
    public static void key(Player player,Level level,ServerLevel sl,int key,boolean borrowed) {
        if(!(player instanceof ServerPlayer p)||(!own(p)&&!borrowed)||!p.isAlive()||p.isSpectator())return;
        if(key<1||key>7||Silence.isSilenced(p))return;
        if(key>=6){if(!borrowed)p.sendOverlayMessage(Component.literal("This Filthy Monarch wheel is not implemented yet."));return;}
        if(borrowed&&key>3)return;
        if(key<=3) {
            if(borrowed)BORROWED.add(p.getUUID());
            try {BASIC.cast(p,BASIC.entries().get((key-1)*2+(p.isShiftKeyDown()?1:0)));}
            finally {if(borrowed)BORROWED.remove(p.getUUID());}
        } else {
            AbilityWheel wheel=key==4?DEVIL_ARTS:DESIRE;
            if(!Beyonder.hasSequence(p,key==4?6:5))return;
            if(p.isShiftKeyDown())wheel.open(p);else wheel.quickCast(p);
        }
    }
    static AbilityWheel.Result perform(ServerPlayer p,String id) {
        if((!own(p)&&!BORROWED.contains(p.getUUID()))||!p.isAlive()||p.isSpectator()||Silence.isSilenced(p))
            return AbilityWheel.Result.REFUSED;
        State s=state(p);
        if(s.channelUntil>s.now())return refuse(p,"Finish or interrupt the Abyss flame channel first.");
        if(DEVIL_ARTS.entry(id)!=null)return FilthyMonarchDevil.cast(s,id);
        if(DESIRE.entry(id)!=null)return FilthyMonarchDesire.cast(s,id);
        switch(id) {
            case "lawless_lunge" -> {
                LivingEntity t=aim(s,s.sequence<=6?12:8);
                if(t==null)return refuse(p,"Look at an enemy in lunge range.");
                if(Root.has(p,RootRestriction.MOVE))return AbilityWheel.Result.REFUSED;
                Vec3 toward=p.position().subtract(t.position()).multiply(1,0,1).normalize();
                Vec3 spot=t.position().add(toward.scale((p.getBbWidth()+t.getBbWidth())/2+.3));
                if(!s.level.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(spot))
                        ||!Spells.fits(p,s.level,spot)||!Teleport.of(p).fallGuard(80).go(s.level,spot))
                    return refuse(p,"There is no safe space beside that enemy.");
                float weapon=heldDamage(p);
                Spells.strike(p,t,(40+weapon)*(1+.1f*(9-s.sequence)));
                hold(s,t,"lunge",10,RootRestriction.MOVEMENT);
                if(s.sequence<=6)burn(s,t,60,4,false);
                fx(s,id,t.position(),18);
            }
            case "dirty_trick" -> {
                LivingEntity t=aim(s,6);if(t==null)return refuse(p,"Look at an enemy within 6 blocks.");
                Spells.strike(p,t,15);Blind.apply(t,60);Falter.apply(t,s.tag("trick"),.4f,60);
                track(s,t,60);fx(s,id,t.getEyePosition(),20);
            }
            case "sulfur_flame" -> {
                for(LivingEntity t:cone(s,s.sequence<=4?16:s.sequence<=6?12:9,35)) {
                    fire(s,t,45,80,4,false,false);seed(s,t,s.sequence<=4?2:1);
                }
                fx(s,id,p.getEyePosition().add(p.getLookAngle().scale(9)),24);
            }
            case "sink" -> {
                LivingEntity t=aim(s,20);if(t==null)return refuse(p,"Look at an enemy within 20 blocks.");
                sink(s,t,1);if(s.sequence<=5)for(LivingEntity other:around(s,t.position(),3))if(other!=t)sink(s,other,.5f);
                fx(s,id,t.position(),40);
            }
            case "killing_spree" -> {
                for(int i=0;i<3;i++)later(s,i*3,()->{
                    if(Silence.isSilenced(p)||Root.has(p,RootRestriction.ATTACK))return;
                    for(LivingEntity t:cone(s,4,30)){Spells.strike(p,t,18);Bleeding.apply(t,p,80,2);}
                });
                later(s,6,()->s.spreeUntil=s.now()+(s.sequence<=6?240:160));
                fx(s,id,p.position().add(p.getLookAngle().scale(2)),24);
            }
            case "devil_worship" -> {
                int n=offerings(s);float strength=n==0?.5f:1+n*.2f;
                Vec3 spot=p.position().add(Spells.turned(p.getLookAngle(),60).multiply(1,0,1).normalize().scale(2));
                if(!Spells.fits(p,s.level,spot))return refuse(p,"There is no room for a Devil projection.");
                Mob projection=Construct.of(EntityTypes.HUSK).health(120*strength).damage(25*strength).lasts(n==0?200:400)
                        .leash(48).limit(SUMMONS,s.sequence<=2?3:s.sequence<=4?2:1).build(p,s.level,spot,p.getYRot());
                if(projection==null)return AbilityWheel.Result.REFUSED;
                projection.getPersistentData().putBoolean("filthy_monarch_volley",n>=3);
                projection.getPersistentData().putLong("filthy_monarch_next_volley",s.now()+80);
                projection.getPersistentData().putBoolean("circle_of_inevitability_beyonder_rolled",true);
                projection.setPersistenceRequired();
                FilthyMonarchFx.form(s.level,projection,"devil_projection",Math.min(360,n==0?200:400),s.tag("projection"),1.5f);
                FilthyMonarchFx.sound(s.level,spot,id);
            }
            default -> {return AbilityWheel.Result.REFUSED;}
        }
        return AbilityWheel.Result.DONE;
    }
    private static void sink(State s,LivingEntity t,float factor) {
        boolean airborne=!t.onGround();Spells.strike(s.player,t,10+(airborne?20:0));
        int ticks=Math.max(1,(int)((Spells.isBoss(t)?40:80)*factor));
        hold(s,t,"sink",ticks,Set.of(RootRestriction.JUMP));Spells.keepEffect(t,MobEffects.SLOWNESS,2,ticks);
        if(airborne){t.setDeltaMovement(t.getDeltaMovement().multiply(1,0,1).add(0,-1.8,0));t.hurtMarked=true;}
    }
    static void hold(State s,LivingEntity t,String kind,int ticks,Set<RootRestriction> restrictions) {
        Root.applyOwned(t,s.tag(kind),ticks,restrictions);track(s,t,ticks);
    }
    static float heldDamage(ServerPlayer p) {
        if(p.getMainHandItem().isEmpty())return 0;
        return (float)Math.max(0,p.getMainHandItem().getOrDefault(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS,
                net.minecraft.world.item.component.ItemAttributeModifiers.EMPTY).compute(Attributes.ATTACK_DAMAGE,1,EquipmentSlot.MAINHAND));
    }
    static void track(State s,LivingEntity t,int ticks) {
        s.controlled.put(t.getUUID(),t);s.controlUntil.merge(t.getUUID(),s.now()+ticks,Math::max);
    }
    static void seed(State s,LivingEntity t,int n) {
        if(!enemy(s,t))return;
        Depravity.plant(s.player,t,n,s.sequence==0?240:120);s.marked.put(t.getUUID(),t);
        FilthyMonarchFx.seeds(s.level,t,Depravity.count(s.player,t),25,s.tag("seeds_"+t.getUUID()));
    }
    static void magic(State s,LivingEntity t,float amount) {
        if(!enemy(s,t)||amount<=0)return;
        DamageFx.credit(s.player,t);t.invulnerableTime=0;
        t.hurtServer(s.level,s.level.damageSources().indirectMagic(s.player,s.player),amount);
        s.player.setLastHurtMob(t);
    }
    static void burn(State s,LivingEntity t,int ticks,float damage,boolean soul) {
        if(!enemy(s,t))return;
        Burn previous=s.burns.get(t.getUUID());
        s.burns.put(t.getUUID(),new Burn(t,Math.max(s.now()+ticks,previous==null?0:previous.until),
                previous==null?s.now()+20:previous.next,Math.max(damage,previous==null?0:previous.damage),soul||previous!=null&&previous.soul));
    }
    static void fire(State s,LivingEntity t,float damage,int ticks,float burnDamage,boolean soul,boolean armorPiercing) {
        if(!enemy(s,t))return;
        if(armorPiercing)magic(s,t,damage);else Spells.strike(s.player,t,damage);
        burn(s,t,ticks,burnDamage,soul);Spells.keepEffect(t,MobEffects.POISON,s.sequence<=6?1:0,ticks);
        if(s.sequence<=1)Corrosion.apply(t,s.player,2,.5f,ticks);
    }
    static boolean fireProtected(LivingEntity t) {return t.fireImmune()||t.hasEffect(MobEffects.FIRE_RESISTANCE)||t.isInWaterOrRain();}
    static void fx(State s,String id,Vec3 at,int ticks) {
        FilthyMonarchFx.cast(s.level,s.player,at,FilthyMonarchFx.Ability.valueOf(id.toUpperCase(Locale.ROOT)),ticks,s.tag("cast"));
    }
    static int offerings(State s) {
        while(!s.offerings.isEmpty()&&s.offerings.peekFirst()<=s.now())s.offerings.removeFirst();return s.offerings.size();
    }
    static void offer(State s) {offerings(s);if(s.offerings.size()==5)s.offerings.removeFirst();s.offerings.addLast(s.now()+1200);}

    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent.Post event) {
        if(!(event.player() instanceof ServerPlayer p))return;
        State s=STATES.get(p.getUUID());
        if(s!=null&&!valid(s)){clear(p);s=null;}
        if(s==null){if(!own(p)||!p.isAlive())return;s=state(p);}
        tick(s);
    }
    static void tick(State s) {
        long now=s.now();ServerPlayer p=s.player;
        List<Pending> due=s.pending.stream().filter(j->j.at<=now).toList();s.pending.removeAll(due);
        for(Pending j:due){if(!valid(s))return;j.action.run();}
        FilthyMonarchDevil.tick(s);
        for(var entry:List.copyOf(s.burns.entrySet())) {
            Burn b=entry.getValue();
            if(!enemy(s,b.target)||now>b.until){s.burns.remove(entry.getKey());continue;}
            if(now>=b.next) {
                if(b.soul||!fireProtected(b.target))magic(s,b.target,b.damage);
                ParticleShapes.burst(s.level,FilthyMonarchFx.particle("sulfur_flame",FilthyMonarchFx.BLUE),b.target.position().add(0,.7,0),6,.02,.1);
                if(s.burns.get(entry.getKey())==b)s.burns.put(entry.getKey(),new Burn(b.target,b.until,now+20,b.damage,b.soul));
            }
        }
        if(s.spreeUntil>now)Spells.keepEffect(p,MobEffects.SPEED,1,5);
        for(var entry:List.copyOf(s.noSprint.entrySet())) {
            LivingEntity t=s.controlled.get(entry.getKey());
            if(t==null||!enemy(s,t)||entry.getValue()<=now){s.noSprint.remove(entry.getKey());continue;}
            t.setSprinting(false);
        }
        for(var entry:List.copyOf(s.controlUntil.entrySet()))if(entry.getValue()<=now) {
            LivingEntity target=s.controlled.remove(entry.getKey());String token=s.taunts.remove(entry.getKey());
            if(target!=null)Taunt.removeIf(target,token);
            s.controlUntil.remove(entry.getKey());s.noSprint.remove(entry.getKey());
        }
        if(now%20!=0)return;
        for(var entry:List.copyOf(s.marked.entrySet())) {
            LivingEntity t=entry.getValue();int n=Depravity.count(p,t);
            if(n==0||!enemy(s,t)){Depravity.clear(p,t);s.marked.remove(entry.getKey());n=0;}
            FilthyMonarchFx.seeds(s.level,t,n,25,s.tag("seeds_"+t.getUUID()));
        }
        if(own(p)&&s.sequence<=7) {
            int count=offerings(s);
            if(count!=s.offeringHud){HudCounter.show(p,OFFERINGS,"filthy_monarch/offerings","Offerings",count,5,0xC80815);s.offeringHud=count;}
        }
        for(Mob projection:Construct.ofOwner(p,SUMMONS)) {
            if(now%200==0)FilthyMonarchFx.form(s.level,projection,"devil_projection",Math.min(360,Construct.ticksLeft(projection)),s.tag("projection"),1.5f);
            LivingEntity target=p.getLastHurtMob();
            if(enemy(s,target)&&projection.distanceToSqr(target)<48*48)projection.setTarget(target);
            if(projection.getPersistentData().getBooleanOr("filthy_monarch_volley",false)
                    &&now>=projection.getPersistentData().getLongOr("filthy_monarch_next_volley",0L)&&enemy(s,projection.getTarget())) {
                projection.getPersistentData().putLong("filthy_monarch_next_volley",now+80);
                FilthyMonarchDevil.volley(s,projection.getEyePosition(),projection.getTarget().getEyePosition().subtract(projection.getEyePosition()),1,28);
            }
        }
    }
    @SubscribeEvent public static void hurt(LivingHurtEvent event) {
        if(event.getEntity().level().isClientSide()||event.getAmount()<=0)return;
        LivingEntity target=event.getEntity();
        ServerPlayer source=event.getSource().getEntity() instanceof ServerPlayer p?p:null;
        if(source==null&&event.getSource().getEntity() instanceof LivingEntity summon) {
            UUID id=Construct.ownerOf(summon);State owner=STATES.get(id);if(owner!=null)source=owner.player;
        }
        if(source!=null) {
            State s=STATES.get(source.getUUID());
            if(s!=null&&valid(s)&&enemy(s,target)) {
                event.setAmount(event.getAmount()*Depravity.bonus(source,target)*(s.spreeUntil>s.now()?1.25f:1));
                boolean melee=event.getSource().is(DamageTypes.PLAYER_ATTACK)&&!Spells.isSpellHit(target);
                if(melee) {
                    if(Form.isIn(source,DEVIL))burn(s,target,60,4,false);
                    if(own(source)&&s.sequence<=5) {
                        if(!target.getUUID().equals(s.meleeTarget)){s.meleeTarget=target.getUUID();s.meleeHits=0;}
                        if(++s.meleeHits%3==0)seed(s,target,1);
                    }
                }
            }
        }
    }
    @SubscribeEvent public static void death(LivingDeathEvent event) {
        LivingEntity dead=event.getEntity();
        if(dead instanceof ServerPlayer deadPlayer)clear(deadPlayer);
        LivingEntity killer=event.getSource().getEntity() instanceof LivingEntity e?e:null;
        ServerPlayer p=killer instanceof ServerPlayer player?player:null;
        if(p==null&&killer!=null){State owner=STATES.get(Construct.ownerOf(killer));if(owner!=null)p=owner.player;}
        if(p!=null&&own(p)&&SoulCore.getSequence(p)<=7&&!Spells.isFriend(p,dead)) {
            State s=state(p);offer(s);
            if(s.spreeUntil>s.now()) {
                s.spreeUntil=Math.min(s.now()+400,s.spreeUntil+60);
                if(s.sequence<=3)Healing.apply(p,p,p.getMaxHealth()*.1f);
            }
        }
    }
    @SubscribeEvent public static boolean target(LivingChangeTargetEvent event) {
        return event.getNewTarget() instanceof ServerPlayer p&&Form.isIn(p,LIQUID);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if(event.getEntity() instanceof ServerPlayer p)clear(p);
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if(event.getEntity() instanceof ServerPlayer p)clear(p);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {STATES.clear();BORROWED.clear();}
    public static void clear(ServerPlayer p) {
        State s=STATES.remove(p.getUUID());if(s==null)return;
        if(Form.isIn(p,DEVIL)||Form.isIn(p,LIQUID))Form.end(p);
        FilthyMonarchDevil.clear(s);Construct.endAll(p,SUMMONS);HudCounter.hide(p,OFFERINGS);
        Root.removeOwned(p,s.tag("channel"));
        for(LivingEntity t:s.marked.values()){Depravity.clear(p,t);FilthyMonarchFx.end(s.level,t,t.position(),s.tag("seeds_"+t.getUUID()));}
        for(LivingEntity t:s.controlled.values()) {
            for(String root:List.of("sink","lunge","cage","shock","desire"))Root.removeOwned(t,s.tag(root));
            Falter.remove(t,s.tag("trick"));Falter.remove(t,s.tag("desire"));Taunt.removeIf(t,s.taunts.get(t.getUUID()));
        }
        FilthyMonarchFx.end(s.level,p,p.position(),s.tag("cast"));
        s.pending.clear();s.burns.clear();s.noSprint.clear();
    }
}
