package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Consumer;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god.ThunderGod.*;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.thunder_god.ThunderGodPowers.*;

/** Real server regressions; vertically isolated actors keep area casts from leaking between parallel tests. */
public final class ThunderGodGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS=DeferredRegister.create(Registries.TEST_FUNCTION,CircleOfInevitability.MOD_ID);
    private static int nextArena;
    static {
        FUNCTIONS.register("thunder_unlock_costs",()->ThunderGodGameTests::unlockCosts);
        FUNCTIONS.register("thunder_wheel_recast",()->ThunderGodGameTests::wheelRecast);
        FUNCTIONS.register("thunder_refused",()->ThunderGodGameTests::refused);
        FUNCTIONS.register("thunder_soaked_arcs",()->ThunderGodGameTests::arcs);
        FUNCTIONS.register("thunder_paralysis",()->ThunderGodGameTests::paralysis);
        FUNCTIONS.register("thunder_rage",()->ThunderGodGameTests::rage);
        FUNCTIONS.register("thunder_rage_decay",()->ThunderGodGameTests::rageDecay);
        FUNCTIONS.register("thunder_cleanse",()->ThunderGodGameTests::cleanseVeil);
        FUNCTIONS.register("thunder_curtain",()->ThunderGodGameTests::curtain);
        FUNCTIONS.register("thunder_voice",()->ThunderGodGameTests::voice);
        FUNCTIONS.register("thunder_rain",()->ThunderGodGameTests::rain);
        FUNCTIONS.register("thunder_passives",()->ThunderGodGameTests::passives);
        FUNCTIONS.register("thunder_tideheal",()->ThunderGodGameTests::heal);
        FUNCTIONS.register("thunder_lifecycle",()->ThunderGodGameTests::lifecycle);
        FUNCTIONS.register("thunder_bolt_arrow",()->ThunderGodGameTests::arrow);
        FUNCTIONS.register("thunder_wind_wave",()->ThunderGodGameTests::waveWind);
    }
    private ThunderGodGameTests(){}
    static ServerPlayer player(GameTestHelper h,int sequence){
        var profile=new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"thunder_"+nextArena);
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false);
        ServerPlayer p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),profile,cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
        p.setGameMode(GameType.SURVIVAL);p.getFoodData().setFoodLevel(10);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        Vec3 at=h.absoluteVec(new Vec3(1.5,40+nextArena++*512,1.5));p.setPos(at.x,at.y,at.z);
        p.setNoGravity(true);p.setYRot(0);p.setXRot(0);
        SoulCore.setPathway(p,PATHWAY);SoulCore.setSequence(p,sequence);SoulCore.setSpiritualityQuietly(p,Float.MAX_VALUE);
        ThunderGodEvents.initialize();
        h.addCleanup(ok->{clear(p);h.getLevel().getServer().getPlayerList().remove(p);});return p;
    }
    static Mob target(GameTestHelper h,ServerPlayer p,double x,double z){
        Mob t=h.spawn(EntityTypes.COW,new BlockPos(1,1,2));t.setNoAi(true);t.setNoGravity(true);
        t.setPos(p.getX()+x,p.getY(),p.getZ()+z);t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);t.setHealth(1000);
        h.addCleanup(ok->{cleanse(t);t.discard();});return t;
    }
    static void same(GameTestHelper h,double got,double want,String why){h.assertTrue(Math.abs(got-want)<.05,why+": got "+got+", want "+want);}
    static void friend(GameTestHelper h,ServerPlayer p,Mob t){
        var board=h.getLevel().getScoreboard();var team=board.addPlayerTeam("thunder_"+p.getId());
        board.addPlayerToTeam(p.getScoreboardName(),team);board.addPlayerToTeam(t.getScoreboardName(),team);h.addCleanup(ok->board.removePlayerTeam(team));
    }
    private static void unlockCosts(GameTestHelper h){
        ServerPlayer p=player(h,9);float before=SoulCore.getSpirituality(p);
        p.setShiftKeyDown(true);key(p,p.level(),h.getLevel(),1,false);
        same(h,before-SoulCore.getSpirituality(p),10,"Sailor can afford scales at unlock");
        h.assertTrue(ConjuredArmor.isWorn(p,SCALES),"Scales worn");SoulCore.setSpiritualityQuietly(p,0);key(p,p.level(),h.getLevel(),1,false);
        h.assertTrue(!ConjuredArmor.isWorn(p,SCALES),"Dismissal is free with an empty bar");
        SoulCore.setPathway(p,"none");
        h.assertTrue(Beyonder.castUnbound(p,()->key(p,p.level(),h.getLevel(),1,true)),"The registered stealable Sailor kit can be borrowed");
        p.hurt(p.damageSources().generic(),5);
        same(h,p.getAbsorptionAmount(),2,"Borrowed scales also provide their 20 percent damage cut");
        ConjuredArmor.takeOff(p,SCALES);SoulCore.setPathway(p,PATHWAY);
        SoulCore.setSequence(p,8);SoulCore.setSpiritualityQuietly(p,Float.MAX_VALUE);before=SoulCore.getSpirituality(p);
        key(p,p.level(),h.getLevel(),2,false);same(h,before-SoulCore.getSpirituality(p),200,"Folk can afford Wrath at unlock");
        h.assertTrue(Form.isIn(p,WRATH),"Wrath started");SoulCore.setSpiritualityQuietly(p,0);key(p,p.level(),h.getLevel(),2,false);
        h.assertTrue(!Form.isIn(p,WRATH),"Wrath dismissal is free");h.succeed();
    }
    private static void wheelRecast(GameTestHelper h){
        ServerPlayer p=player(h,5);var wheel=TIDES;var entry=wheel.entry("tideheal");
        float before=SoulCore.getSpirituality(p);h.assertTrue(wheel.cast(p,entry)==AbilityWheel.Result.DONE,"Initial cast succeeds");
        same(h,before-SoulCore.getSpirituality(p),1500,"Normal price");int wait=wheel.cooldownLeft(p,entry);
        before=SoulCore.getSpirituality(p);wheel.quickCast(p);same(h,SoulCore.getSpirituality(p),before,"Recast cannot bypass cooldown");
        same(h,wheel.cooldownLeft(p,entry),wait,"Refused recast preserves deadline");wheel.resetCooldown(p,"tideheal");
        wheel.quickCast(p);same(h,before-SoulCore.getSpirituality(p),1500,"Ready recast pays full price");
        h.assertTrue(wheel.cooldownLeft(p,entry)>0,"Recast starts its cooldown");h.succeed();
    }
    private static void refused(GameTestHelper h){
        ServerPlayer p=player(h,7);float before=SoulCore.getSpirituality(p);
        h.assertTrue(TIDES.cast(p,TIDES.entry("water_sphere"))==AbilityWheel.Result.REFUSED,"Sequence gate");
        h.assertTrue(TIDES.cast(p,TIDES.entry("water_veil"))==AbilityWheel.Result.REFUSED,"No target");same(h,SoulCore.getSpirituality(p),before,"Refusals cost nothing");
        SoulCore.setSequence(p,5);SoulCore.setSpiritualityQuietly(p,Float.MAX_VALUE);before=SoulCore.getSpirituality(p);
        h.assertTrue(THUNDER.cast(p,THUNDER.entry("lightning_bolt_arrow"))==AbilityWheel.Result.REFUSED,"Bow required");
        same(h,SoulCore.getSpirituality(p),before,"No bow is free");h.assertTrue(THUNDER.lastPick(p)==null,"Refusal doesn't save a pick");
        Root.apply(p,40,RootRestriction.ABILITIES);h.assertTrue(VOICE.cast(p,VOICE.entry("siren_song"))==AbilityWheel.Result.REFUSED,"Silenced menu selection refused");h.succeed();
    }
    private static void arcs(GameTestHelper h){
        ServerPlayer p=player(h,5);Mob a=target(h,p,0,3),b=target(h,p,2,3),ally=target(h,p,-2,3),dry=target(h,p,4,3);friend(h,p,ally);
        Soaked.apply(a,200);Soaked.apply(b,200);Soaked.apply(ally,200);
        Shock.strike(p,a,10);same(h,1000-a.getHealth(),14,"Water adds 40 percent");same(h,1000-b.getHealth(),7.14,"Secondary arc is half, with water bonus");
        same(h,ally.getHealth(),1000,"No friendly arc");same(h,dry.getHealth(),1000,"No arc to dry targets");same(h,Rage.count(p),2,"One point per hit enemy");
        h.assertTrue(Root.has(a,RootRestriction.MOVE),"Lightning paralyzes");
        dry.setItemSlot(EquipmentSlot.HEAD,Items.COPPER_HELMET.getDefaultInstance());h.assertTrue(Shock.metal(dry),"Copper armor conducts");h.succeed();
    }
    private static void paralysis(GameTestHelper h){
        ServerPlayer p=player(h,5);Mob t=target(h,p,0,3);Shock.strike(p,t,1);
        h.runAfterDelay(15,()->{
            h.assertTrue(!Root.has(t,RootRestriction.MOVE),"First paralysis wears off");Shock.strike(p,t,1);
            h.assertTrue(!Root.has(t,RootRestriction.MOVE),"Immunity prevents a lightning lock");
        });
        h.runAfterDelay(65,()->{Shock.strike(p,t,1);h.assertTrue(Root.has(t,RootRestriction.MOVE),"Paralysis returns after immunity");h.succeed();});
    }
    private static void rage(GameTestHelper h){
        ServerPlayer p=player(h,8);Mob t=target(h,p,0,3);Rage.Cast cast=Rage.cast(p);
        cast.strike(t,1);cast.strike(t,1);same(h,Rage.count(p),1,"Repeated ticks in one cast grant one point");
        Rage.cast(p).strike(t,1);same(h,Rage.count(p),2,"Separate casts count separately");
        Rage.drink(p);Rage.cast(p).strike(t,1);same(h,Rage.count(p),4,"Tipsy doubles gains");
        Rage.add(p,50);same(h,Rage.count(p),10,"Cap is ten");same(h,Rage.spend(p),10,"Spend returns stored count");same(h,Rage.count(p),0,"Spend empties Rage");
        Rage.cast(t).strike(p,1);Rage.cast(t).strike(p,1);same(h,Rage.count(p),2,"Incoming hits share one half-second throttle and Tipsy multiplier");h.succeed();
    }
    private static void rageDecay(GameTestHelper h){
        ServerPlayer p=player(h,8);Rage.add(p,3);
        h.runAfterDelay(101,()->same(h,Rage.count(p),2,"One Rage decays per five idle seconds"));
        h.runAfterDelay(201,()->{same(h,Rage.count(p),1,"Decay continues without another gain");h.succeed();});
    }
    private static void cleanseVeil(GameTestHelper h){
        ServerPlayer p=player(h,5);Mob t=target(h,p,0,3);
        h.assertTrue(cast(p,h.getLevel(),Spell.WATER_VEIL)==AbilityWheel.Result.DONE,"Veil targets enemy");
        h.assertTrue(afflicted(t,Affliction.VEIL)&&Silence.isSilenced(t),"Veil silences");
        h.runAfterDelay(25,()->{
            h.assertTrue(t.getHealth()<1000,"Veil deals periodic damage");same(h,Rage.count(p),1,"Veil only feeds Rage once");
            Root.apply(t,80,RootRestriction.TELEPORT);Spells.cleanse(t);
            h.assertTrue(!afflicted(t,Affliction.VEIL)&&!Silence.isSilenced(t),"Cleanse removes only Veil's silence");
            h.assertTrue(Root.has(t,RootRestriction.TELEPORT),"Cleansing Veil preserves an unrelated root");
            h.assertTrue(cast(p,h.getLevel(),Spell.WATER_SPHERE)==AbilityWheel.Result.DONE,"Sphere targets enemy");
            h.assertTrue(Root.has(t,RootRestriction.MOVE),"Sphere holds");Spells.cleanse(t);
            h.assertTrue(!Root.has(t,RootRestriction.MOVE),"Cleanse frees the sphere");h.succeed();
        });
    }
    private static void curtain(GameTestHelper h){
        ServerPlayer p=player(h,5);Mob enemy=target(h,p,0,2);cast(p,h.getLevel(),Spell.WATER_CURTAIN);
        var arrow=h.spawn(EntityTypes.ARROW,new BlockPos(1,2,1));arrow.setNoGravity(true);arrow.setOwner(enemy);
        arrow.setPos(p.getX(),p.getY()+1,p.getZ()+2.7);arrow.setDeltaMovement(0,0,-1);
        h.addCleanup(ok->arrow.discard());
        h.runAfterDelay(3,()->{h.assertTrue(arrow.isRemoved(),"Curtain stops hostile arrows");
            enemy.setPos(p.getX(),p.getY(),p.getZ()+1.7);
            h.runAfterDelay(3,()->{same(h,enemy.getZ(),p.getZ()+1.7,"Enemy can pass through curtain");h.succeed();});});
    }
    private static void voice(GameTestHelper h){
        ServerPlayer p=player(h,5);Mob t=target(h,p,0,3),ally=target(h,p,2,3);friend(h,p,ally);
        cast(p,h.getLevel(),Spell.SIREN_SONG);h.assertTrue(Slumber.isAsleep(t)&&!Slumber.isAsleep(ally),"Siren Song spares friends");
        Rage.cast(p).strike(t,1);h.assertTrue(!Slumber.isAsleep(t),"Damage wakes a siren victim");
        h.assertTrue(Fear.apply(p,t,80),"Sailor resists, rather than ignores, fear");same(h,Fear.ticksLeft(p),40,"Fear lasts half as long");
        cast(p,h.getLevel(),Spell.WRATH);h.assertTrue(!Fear.isAfraid(p)&&!Fear.apply(p,t,80),"Wrath fully resists fear");h.succeed();
    }
    private static void rain(GameTestHelper h){
        ServerPlayer p=player(h,5);p.setXRot(45);Mob enemy=target(h,p,0,8),ally=target(h,p,1,8);friend(h,p,ally);
        boolean worldRain=h.getLevel().isRaining(),worldThunder=h.getLevel().isThundering();
        cast(p,h.getLevel(),Spell.CORROSIVE_RAIN);Vec3 center=Zone.centerOf(p,RAIN);
        enemy.setPos(center.x,center.y,center.z);ally.setPos(center.x+1,center.y,center.z);
        h.runAfterDelay(90,()->{
            h.assertTrue(Soaked.isSoaked(enemy)&&enemy.getHealth()<1000,"Rain soaks and damages enemies");
            h.assertTrue(Corrosion.isCorroding(enemy),"Staying in rain causes corrosion");
            same(h,ally.getHealth(),1000,"Rain does not harm allies");h.assertTrue(!Soaked.isSoaked(ally),"Rain does not mark allies");
            h.assertTrue(h.getLevel().isRaining()==worldRain&&h.getLevel().isThundering()==worldThunder,"Rain stays local");
            Zone.close(p,RAIN);h.succeed();
        });
    }
    private static void passives(GameTestHelper h){
        ServerPlayer p=player(h,8);ThunderGodEvents.body(p);same(h,p.getMaxHealth(),24,"Folk has two extra hearts");
        SoulCore.setPathway(p,"none");ThunderGodEvents.body(p);same(h,p.getMaxHealth(),20,"Changing pathway restores health attribute");h.succeed();
    }
    private static void heal(GameTestHelper h){
        ServerPlayer p=player(h,7);p.setHealth(4);p.setRemainingFireTicks(200);cast(p,h.getLevel(),Spell.TIDEHEAL);
        h.assertTrue(!p.isOnFire(),"Tideheal extinguishes immediately");
        h.runAfterDelay(125,()->{same(h,p.getHealth(),16,"Tideheal restores twelve health over six seconds");h.succeed();});
    }
    private static void lifecycle(GameTestHelper h){
        ServerPlayer p=player(h,5);p.setHealth(8);TIDES.cast(p,TIDES.entry("tideheal"));int left=TIDES.cooldownLeft(p,TIDES.entry("tideheal"));Rage.add(p,3);
        release(p);same(h,Rage.count(p),3,"Logout release preserves saved Rage");same(h,TIDES.cooldownLeft(p,TIDES.entry("tideheal")),left,"Logout release preserves cooldowns");
        clear(p);same(h,Rage.count(p),0,"Pathway loss clears Rage");h.assertTrue(TIDES.lastPick(p)==null,"Pathway loss resets selections");
        h.runAfterDelay(25,()->{same(h,p.getHealth(),8,"Lifecycle release cancels queued healing");h.succeed();});
    }
    private static void arrow(GameTestHelper h){
        ServerPlayer p=player(h,5);Mob first=target(h,p,0,3),second=target(h,p,0,5),third=target(h,p,0,7);
        p.setItemSlot(EquipmentSlot.OFFHAND,Items.BOW.getDefaultInstance());cast(p,h.getLevel(),Spell.LIGHTNING_BOLT_ARROW);
        same(h,first.getHealth(),1000,"Bolt Arrow has a real draw time");
        h.runAfterDelay(23,()->{h.assertTrue(first.getHealth()<1000&&second.getHealth()<1000,"Arrow pierces two targets");same(h,third.getHealth(),1000,"Arrow stops damaging after two targets");h.succeed();});
    }
    private static void waveWind(GameTestHelper h){
        ServerPlayer p=player(h,5);Mob t=target(h,p,0,5),ally=target(h,p,1,5);friend(h,p,ally);
        cast(p,h.getLevel(),Spell.SWEEPING_WAVE);
        h.runAfterDelay(35,()->{
            h.assertTrue(Soaked.isSoaked(t)&&t.getHealth()<1000,"Wave hits and soaks");same(h,ally.getHealth(),1000,"Wave spares allies");h.assertTrue(!Soaked.isSoaked(ally),"Wave does not mark allies");h.succeed();
        });
    }
}
