package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.white_angel;

import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.*;
import net.lukario.circleofinevitability.particles.fx.*;
import net.lukario.circleofinevitability.sounds.ModSounds;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.*;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.Commands;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** First fifteen Sun pathway abilities. All temporary state is server-owned and cleared on exit. */
@Mod.EventBusSubscriber(modid="circle_of_inevitability")
public final class WhiteAngel {
    public static final String PATHWAY="White Angel";
    private static final String PREFIX="white_angel_";
    private static final Map<UUID,Long> COURAGE=new HashMap<>(), OATH=new HashMap<>();
    private record Pact(ServerPlayer owner, LivingEntity target, long until, boolean holy) {}
    private record Offer(ServerPlayer owner,long until) {}
    private static final List<Pact> PACTS=new ArrayList<>();
    private static final Map<UUID,Offer> OFFERS=new HashMap<>();
    private static AbilityWheel wheel(String id,String name){return AbilityWheel.create(PREFIX+id,name).pathway(PATHWAY).quickCastCooldown(false).quickCastCost(1f);}
    private static String icon(String id){return "ability_menus/white_angel/"+id;}
    public static final AbilityWheel THEURGY=wheel("theurgy","Theurgy")
        .entry("holy_light",icon("holy_light"),"Holy Light Summoning",7,900,0,WhiteAngel::holyLight)
        .entry("sun_halo",icon("sun_halo"),"Sun Halo",7,700,0,(p,sl)->field(p,sl,"halo",Beyonder.hasSequence(p,5)?16:12,400))
        .free(p->Zone.isOpen(p,PREFIX+"halo"))
        .entry("holy_oath",icon("holy_oath"),"Holy Oath",7,600,0,WhiteAngel::oath)
        .entry("cleave",icon("cleave"),"Cleave of Purification",7,500,0,WhiteAngel::cleave)
        .entry("fire_of_light",icon("fire_of_light"),"Fire of Light",7,1100,0,WhiteAngel::fire);
    public static final AbilityWheel NOTARY=wheel("notary","Notarization")
        .entry("contract",icon("contract"),"Contract",6,1500,0,WhiteAngel::contract).notCopyable()
        .entry("authenticate",icon("authenticate"),"Authenticate",6,800,0,WhiteAngel::authenticate)
        .entry("amplify",icon("amplify"),"Amplify",6,600,0,WhiteAngel::amplify).costBy((p,c)->Beyonder.hasSequence(p,0)?c/4:c)
        .entry("nullify",icon("nullify"),"Nullify",6,700,0,WhiteAngel::nullify).costBy((p,c)->Beyonder.hasSequence(p,0)?c/4:c);
    public static final AbilityWheel RADIANCE=wheel("radiance","Radiance")
        .entry("purification",icon("purification"),"Purification",4,3000,0,WhiteAngel::purification)
        .entry("domain",icon("domain"),"Unshadowed Domain",4,4000,0,(p,sl)->field(p,sl,"domain",14,300))
        .free(p->Zone.isOpen(p,PREFIX+"domain"));
    static {Cooldowns.addFree(Cooldowns.keysStartingWith(PREFIX));Fear.addImmunity(e->active(COURAGE,e) || e instanceof Player p && own(p) && Beyonder.hasSequence(p,7));Corrosion.addImmunity(WhiteAngel::sheltered);Decay.addImmunity(WhiteAngel::sheltered);Misfortune.addImmunity(WhiteAngel::sheltered);}
    private static boolean sheltered(LivingEntity e){
        if(!(e.level() instanceof ServerLevel sl))return false;
        return sl.players().stream().anyMatch(p->own(p) && (e==p || !Spells.isEnemy(p,e)) && Zone.covers(p,PREFIX+"domain",e.position()));
    }
    public static void initialize() {}
    private static boolean active(Map<UUID,Long> state,LivingEntity e){return state.getOrDefault(e.getUUID(),0L)>e.level().getGameTime();}
    private static double might(Player p){return 1+.05*(9-SoulCore.getSequence(p));}
    private static int duration(Player p,int ticks){return (int)(ticks*might(p));}
    private static boolean own(Player p){return SoulCore.getPathway(p).equals(PATHWAY);}
    private static boolean valid(Player p,ServerLevel sl){return p.isAlive() && p.level()==sl && own(p);}
    private static List<LivingEntity> friends(ServerPlayer p,ServerLevel sl,double radius){var list=new ArrayList<>(Spells.alliesAround(p,sl,p.position(),radius));list.add(p);return list;}
    public static void key(Player p,Level level,ServerLevel sl,int key,boolean bypass){
        if(!(p instanceof ServerPlayer sp) || (!bypass && !own(p)) || key<1 || key>5 || !Beyonder.hasSequence(p,new int[]{0,9,8,7,6,4}[key]))return;
        if(key>=3){var wheel=key==3?THEURGY:key==4?NOTARY:RADIANCE;if(p.isShiftKeyDown())wheel.quickCast(sp);else wheel.open(sp);return;}
        if(key==2 && p.isShiftKeyDown() && Zone.isOpen(p,PREFIX+"daytime")){Zone.close(p,PREFIX+"daytime");return;}
        int cost=key==1?250:p.isShiftKeyDown()?1250:600;
        if(!Spells.paySpirituality(p,cost))return;
        if(key==1)hymn(sp,sl,p.isShiftKeyDown());
        else if(p.isShiftKeyDown())field(sp,sl,"daytime",16,400);
        else {Vec3 at=Spells.aimGround(p,sl,24);WhiteAngelFx.at(sl,at,"sun",20);for(var e:Spells.enemiesAround(p,sl,at,6)){strike(sp,e,14);Blind.apply(e,80);e.igniteForSeconds(6);}WhiteAngelFx.sound(sp,"sunshine");}
    }
    public static float purifyMultiplier(LivingEntity e){return (e.is(EntityTypeTags.UNDEAD)?2.5f:Spells.isEvil(e)?1.5f:1)*(Corrosion.isCorroding(e) || Decay.isDecaying(e) || Madness.get(e)>0?1.5f:1);}
    private static void strike(ServerPlayer p,LivingEntity e,float amount){
        boolean sunlight=Nocturnality.inDaySun(p) || Zone.isOpen(p,PREFIX+"daytime") || Zone.isOpen(p,PREFIX+"domain") || Zone.isOpen(p,PREFIX+"halo");
        Spells.strike(p,e,amount*purifyMultiplier(e)*(sunlight?1.15f:1)*(active(OATH,p)?1.2f:1));
    }
    private static void courage(LivingEntity e,int ticks){Fear.end(e);COURAGE.put(e.getUUID(),e.level().getGameTime()+ticks);}
    private static void hymn(ServerPlayer p,ServerLevel sl,boolean renewal){
        for(var e:friends(p,sl,12*might(p))){
            if(renewal){Spells.keepEffect(e,MobEffects.REGENERATION,0,duration(p,200));Spells.keepEffect(e,MobEffects.SPEED,0,duration(p,400));if(e instanceof Player ally && e!=p)Spells.giveSpirituality(ally,Beyonder.maxSpirituality(ally)*.05f);}
            else {courage(e,duration(p,400));Spells.keepEffect(e,MobEffects.STRENGTH,0,duration(p,400));}
            WhiteAngelFx.above(sl,e,"hymn_note",40);
        }WhiteAngelFx.sound(p,renewal?"hymn_of_renewal":"hymn_of_valor");
    }
    private static AbilityWheel.Result field(ServerPlayer p,ServerLevel sl,String name,double base,int ticks){
        if(Zone.isOpen(p,PREFIX+name)){Zone.close(p,PREFIX+name);return AbilityWheel.Result.FREE;}
        double max=base*might(p);int life=duration(p,ticks);
        Zone.around(p,max).lasts(life).every(name.equals("domain")?10:20).onPulse(left->{
            if(!own(p)){Zone.close(p,PREFIX+name);return;}
            double r=name.equals("daytime")?Math.min(max,(10+(life-left)/20.)*might(p)):max;
            Nocturnality.forceDay(sl,p.position(),r,name.equals("domain")?10:20);
            WhiteAngelFx.ring(sl,p.position(),name.equals("domain")?"domain_boundary":"sun_halo",r,22);
            for(var e:friends(p,sl,r)){courage(e,25);Spells.keepEffect(e,MobEffects.NIGHT_VISION,0,220);if(name.equals("domain") || name.equals("halo"))clean(e);}
            for(var e:Spells.enemiesAround(p,sl,p.position(),r)){
                if(name.equals("domain"))Exposed.apply(e,25);
                if(purifyMultiplier(e)>1 && (!name.equals("halo") || Beyonder.hasSequence(p,5))){strike(p,e,name.equals("halo")?4:name.equals("domain")?2:3);e.igniteForSeconds(2);}
            }
        }).open(p,sl,PREFIX+name);
        WhiteAngelFx.sound(p,name.equals("halo")?"sun_halo":name);return AbilityWheel.Result.DONE;
    }
    private static AbilityWheel.Result holyLight(ServerPlayer p,ServerLevel sl){
        LivingEntity primary=Spells.aimEnemy(p,sl,28);Vec3 at=primary==null?Spells.aimGround(p,sl,28):primary.position();boolean high=Beyonder.hasSequence(p,5);WhiteAngelFx.ring(sl,at,"warning_sigil",high?4:2.5,8);
        Later.run(sl,8,()->{if(!valid(p,sl))return;WhiteAngelFx.at(sl,at,"blade_of_light",15);for(var e:Spells.enemiesAround(p,sl,at,high?4:2.5)){strike(p,e,high?90:e==primary?30:15);if(high && !Spells.isBoss(e) && Spells.isEvil(e) && e.getHealth()<e.getMaxHealth()*.3f)Spells.strikeTrue(p,e,e.getHealth());}});
        WhiteAngelFx.sound(p,"holy_light");return AbilityWheel.Result.DONE;
    }
    private static AbilityWheel.Result oath(ServerPlayer p,ServerLevel sl){for(var e:friends(p,sl,Beyonder.hasSequence(p,4)?16*might(p):0)){Spells.keepEffect(e,MobEffects.STRENGTH,0,duration(p,600));Spells.keepEffect(e,MobEffects.SPEED,0,duration(p,600));OATH.put(e.getUUID(),sl.getGameTime()+duration(p,600));WhiteAngelFx.above(sl,e,"oath_sigil",40);}WhiteAngelFx.sound(p,"holy_oath");return AbilityWheel.Result.DONE;}
    private static AbilityWheel.Result cleave(ServerPlayer p,ServerLevel sl){WeaponCoat.arm(p,PREFIX+"cleave",8,duration(p,400),(a,t,damage,l)->{if(purifyMultiplier(t)>1)Exposed.apply(t,20);return damage+8*purifyMultiplier(t);});WhiteAngelFx.sound(p,"cleave");return AbilityWheel.Result.DONE;}
    private static AbilityWheel.Result fire(ServerPlayer p,ServerLevel sl){Vec3 at=Spells.aimGround(p,sl,24);Zone.at(at,6*might(p)).lasts(120).every(10).onPulse(left->{if(!own(p))return;WhiteAngelFx.at(sl,at,"flame_plume",12);for(var e:Spells.enemiesAround(p,sl,at,6*might(p))){strike(p,e,5);e.igniteForSeconds(2);Exposed.apply(e,15);}for(var e:Spells.alliesAround(p,sl,at,6*might(p))){Spells.keepEffect(e,MobEffects.FIRE_RESISTANCE,0,25);courage(e,15);}}).open(p,sl,PREFIX+"fire");WhiteAngelFx.sound(p,"fire_of_light");return AbilityWheel.Result.DONE;}
    private static int slots(ServerPlayer p){return Beyonder.hasSequence(p,1)?9:Beyonder.hasSequence(p,3)?5:Beyonder.hasSequence(p,4)?3:2;}
    private static boolean bind(ServerPlayer p,LivingEntity t){if(t instanceof Player a && SoulCore.getSequence(a)<SoulCore.getSequence(p)-(Beyonder.hasSequence(p,3)?3:2))return false;if(PACTS.stream().filter(a->a.owner==p).count()>=slots(p))return false;PACTS.add(new Pact(p,t,p.level().getGameTime()+duration(p,600),Beyonder.hasSequence(p,3)));if(t instanceof Mob m)m.setTarget(null);return true;}
    private static AbilityWheel.Result contract(ServerPlayer p,ServerLevel sl){
        LivingEntity t=Spells.aimLiving(p,sl,16,false,e->e!=p);if(t==null || Spells.isBoss(t) || (!(t instanceof Player) && t.getMaxHealth()>150) || PACTS.stream().filter(a->a.owner==p).count()>=slots(p))return AbilityWheel.Result.REFUSED;
        if(t instanceof ServerPlayer target){OFFERS.put(target.getUUID(),new Offer(p,sl.getGameTime()+400));target.sendSystemMessage(Component.literal(p.getName().getString()+" offers a truce. Use /whitepact accept within 20 seconds."));}
        else if(!bind(p,t))return AbilityWheel.Result.REFUSED;
        WhiteAngelFx.above(sl,t,"contract_scroll",40);WhiteAngelFx.sound(p,"contract");return AbilityWheel.Result.DONE;
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("whitepact").then(Commands.literal("accept").executes(c->{var t=c.getSource().getPlayerOrException();var o=OFFERS.remove(t.getUUID());if(o==null || o.until<t.level().getGameTime() || !o.owner.isAlive() || !own(o.owner) || o.owner.level()!=t.level() || o.owner.distanceToSqr(t)>256 || !bind(o.owner,t))return 0;t.sendSystemMessage(Component.literal("Truce sealed for 30 seconds."));return 1;})));}
    private static AbilityWheel.Result authenticate(ServerPlayer p,ServerLevel sl){for(var e:Spells.enemiesAround(p,sl,p.position(),12*might(p))){if(Dispel.isConjured(e))Dispel.unmake(e);else Exposed.apply(e,80);}WhiteAngelFx.above(sl,p,"holy_eye",60);WhiteAngelFx.sound(p,"authenticate");return AbilityWheel.Result.DONE;}
    private static AbilityWheel.Result amplify(ServerPlayer p,ServerLevel sl){var t=Spells.aimAlly(p,sl,24);if(t==null)t=p;if(!(t instanceof Player ally))return AbilityWheel.Result.REFUSED;NextCast.arm(ally,Beyonder.hasSequence(p,4)?400:200,1,Beyonder.hasSequence(p,4)?2:1.5f);p.sendSystemMessage(Component.literal("God says it's effective!"));WhiteAngelFx.sound(p,"amplify");return AbilityWheel.Result.DONE;}
    private static AbilityWheel.Result nullify(ServerPlayer p,ServerLevel sl){var at=Spells.aimPoint(p,sl,24);var shots=Spells.hostileShotsAround(p,sl,at,3);if(!shots.isEmpty())shots.get(0).discard();else{var t=Spells.aimEnemy(p,sl,24);if(t==null)return AbilityWheel.Result.REFUSED;Dispel.boons(t);Dispel.statuses(t);if(t instanceof Player target){KeySeal.seal(target,KeySeal.lastUsed(target),120);NextCast.used(target);}else AbilityLock.apply(t,80);}p.sendSystemMessage(Component.literal("God says it's ineffective!"));WhiteAngelFx.sound(p,"nullify");return AbilityWheel.Result.DONE;}
    private static void clean(LivingEntity e){Spells.cleanse(e);Cleansing.cleanseStatuses(e);Blind.remove(e);Fear.end(e);e.clearFire();var parasite=Parasite.parasiteIn(e);if(parasite!=null)Parasite.leave(parasite,Parasite.End.LEFT);}
    private static AbilityWheel.Result purification(ServerPlayer p,ServerLevel sl){var enemy=Spells.aimEnemy(p,sl,20);if(enemy!=null){WhiteAngelFx.above(sl,enemy,"notary_seal",60);Countdown.seconds(3).keepWhile(()->valid(p,sl) && enemy.isAlive() && enemy.level()==sl && p.distanceToSqr(enemy)<=576).onFinish(()->{Dispel.boons(enemy);Dispel.statuses(enemy);if(enemy instanceof Player t){SoulCore.setSpirituality(t,SoulCore.getSpirituality(t)*.65f);int key=net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PathwayAbilities.of(SoulCore.getPathway(t)).stream().filter(a->Beyonder.hasSequence(t,a.sequence())).min(java.util.Comparator.comparingInt(a->a.sequence())).map(a->a.key()).orElse(KeySeal.lastUsed(t));KeySeal.seal(t,key,240);}else strike(p,enemy,40);}).start(p);}else{var ally=Spells.aimAlly(p,sl,20);clean(ally==null?p:ally);}WhiteAngelFx.sound(p,"purification");return AbilityWheel.Result.DONE;}
    @SubscribeEvent public static void hurt(LivingHurtEvent e){
        var attacker=e.getSource().getEntity();var target=e.getEntity();
        for(var pact:new ArrayList<>(PACTS)){
            if(pact.until<=target.level().getGameTime() || !pact.owner.isAlive() || !own(pact.owner))continue;
            if((attacker==pact.owner && target==pact.target)||(attacker==pact.target && target==pact.owner)){
                int strength=attacker instanceof Player a?SoulCore.getSequence(a):9;
                int unbreakable=SoulCore.getSequence(pact.owner)-(pact.holy?2:1);
                if(strength==unbreakable){e.setAmount(0);continue;}
                PACTS.remove(pact);
                if(attacker instanceof LivingEntity offender && offender.level() instanceof ServerLevel sl){
                    for(int tick=0;tick<120;tick+=20)Later.run(sl,tick,()->{if(offender.isAlive() && offender.level()==sl)offender.hurtServer(sl,sl.damageSources().magic(),6);});
                    if(offender instanceof Player a && strength< SoulCore.getSequence(pact.owner))SoulCore.setSpirituality(a,SoulCore.getSpirituality(a)*.75f);
                    if(pact.holy)Later.run(sl,1,()->{if(offender.isAlive())offender.hurtServer(sl,sl.damageSources().magic(),offender.getMaxHealth()*.25f);});
                    WhiteAngelFx.sound(pact.owner,"pact_breach");
                }
            }
        }
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent.Post e){if(!(e.player() instanceof ServerPlayer p))return;long now=p.level().getGameTime();PACTS.removeIf(a->a.until<=now || !a.owner.isAlive() || !a.target.isAlive() || !own(a.owner) || a.owner.level()!=a.target.level());OFFERS.entrySet().removeIf(a->a.getValue().until<=now || !a.getValue().owner.isAlive());COURAGE.entrySet().removeIf(a->a.getValue()<=now);OATH.entrySet().removeIf(a->a.getValue()<=now);if(!own(p)){clear(p);return;}if(now%20==0 && Beyonder.hasSequence(p,7))Spells.keepEffect(p,MobEffects.FIRE_RESISTANCE,0,40);}
    private static void clear(Player p){COURAGE.remove(p.getUUID());OATH.remove(p.getUUID());OFFERS.remove(p.getUUID());OFFERS.values().removeIf(o->o.owner==p);PACTS.removeIf(a->a.owner==p || a.target==p);for(String n:List.of("daytime","halo","domain","fire"))Zone.close(p,PREFIX+n);WeaponCoat.disarm(p,PREFIX+"cleave");}
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){clear(e.getEntity());}
    @SubscribeEvent public static void death(LivingDeathEvent e){if(e.getEntity() instanceof Player p)clear(p);}
}
