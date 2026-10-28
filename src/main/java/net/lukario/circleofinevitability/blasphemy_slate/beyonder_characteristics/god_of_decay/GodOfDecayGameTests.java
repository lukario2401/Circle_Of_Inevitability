package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Consumer;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay.GodOfDecayFx.Ability.*;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay.GodOfDecayMarks.Kind;

/** Real-server regression tests for costs, latent marks, lifecycle and shared gameplay primitives. */
public final class GodOfDecayGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS=DeferredRegister.create(Registries.TEST_FUNCTION,CircleOfInevitability.MOD_ID);
    static {
        FUNCTIONS.register("decay_projectile_tempo",()->GodOfDecayGameTests::projectileTempo);
        FUNCTIONS.register("decay_support",()->GodOfDecayGameTests::support);
        FUNCTIONS.register("decay_wheels",()->GodOfDecayGameTests::wheels);
        FUNCTIONS.register("decay_refused_cast",()->GodOfDecayGameTests::refused);
        FUNCTIONS.register("decay_incubation",()->GodOfDecayGameTests::incubation);
        FUNCTIONS.register("decay_owner_marks",()->GodOfDecayGameTests::marks);
        FUNCTIONS.register("decay_restores_health",()->GodOfDecayGameTests::restore);
        FUNCTIONS.register("decay_free_toggle",()->GodOfDecayGameTests::toggle);
        FUNCTIONS.register("decay_existing_ward",()->GodOfDecayGameTests::wards);
        FUNCTIONS.register("decay_weathering",()->GodOfDecayGameTests::weather);
        FUNCTIONS.register("decay_held_action",()->GodOfDecayGameTests::held);
        FUNCTIONS.register("decay_division",()->GodOfDecayGameTests::division);
        FUNCTIONS.register("decay_doom",()->GodOfDecayGameTests::doom);
        FUNCTIONS.register("decay_ally_safety",()->GodOfDecayGameTests::allies);
    }
    private GodOfDecayGameTests(){}
    private static ServerPlayer player(GameTestHelper h,int sequence){
        ServerPlayer p=h.makeMockServerPlayerInLevel();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        Vec3 at=h.absoluteVec(new Vec3(1.5,1,1.5));p.setPos(at.x,at.y,at.z);p.setYRot(0);p.setXRot(0);
        SoulCore.setPathway(p,GodOfDecay.PATHWAY);SoulCore.setSequence(p,sequence);SoulCore.setSpiritualityQuietly(p,Float.MAX_VALUE);
        GodOfDecayEvents.initialize();
        h.addCleanup(ok->{GodOfDecayPowers.clear(p);GodOfDecayMarks.clear(p);p.discard();});return p;
    }
    private static Mob enemy(GameTestHelper h,ServerPlayer p,float health){
        Mob mob=h.spawn(EntityTypes.HUSK,new net.minecraft.core.BlockPos(1,1,2));mob.setNoAi(true);mob.setNoGravity(true);
        mob.setPos(p.getX(),p.getY(),p.getZ()+2);mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);mob.setHealth(health);
        h.addCleanup(ok->mob.discard());return mob;
    }
    private static void equal(GameTestHelper h,double a,double b,String message){h.assertTrue(Math.abs(a-b)<.01,message+": "+a+" != "+b);}
    private static void wheels(GameTestHelper h){
        ServerPlayer p=player(h,0);enemy(h,p,1000);
        AbilityWheel[] wheels={GodOfDecay.DISEASE,GodOfDecay.DOOM,GodOfDecay.TIME};
        String[] ids={"disease","doom","time"},keys={"laziness","decay","slow_time"};
        for(int i=0;i<wheels.length;i++){
            var wheel=wheels[i];var entry=wheel.entry(keys[i]);
            float before=SoulCore.getSpirituality(p);
            h.assertTrue(wheel.cast(p,entry)==AbilityWheel.Result.DONE,"Normal wheel cast succeeds");
            equal(h,before-SoulCore.getSpirituality(p),entry.cost(),"Normal price");
            h.assertTrue(wheel.cooldownLeft(p,entry)>0,"Normal cast starts cooldown");
            h.assertTrue(wheel.cast(p,entry)==AbilityWheel.Result.REFUSED,"Normal respects cooldown");
            long ready=p.getPersistentData().getLongOr("god_of_decay_"+ids[i]+"_ready_"+keys[i],0L);
            SoulCore.setSpiritualityQuietly(p,Float.MAX_VALUE);before=SoulCore.getSpirituality(p);wheel.quickCast(p);
            equal(h,before-SoulCore.getSpirituality(p),entry.cost()*3,"Sneak recast costs exactly 3x");
            equal(h,p.getPersistentData().getLongOr("god_of_decay_"+ids[i]+"_ready_"+keys[i],0L),ready,"Sneak preserves prior deadline");
            wheel.resetCooldown(p,keys[i]);SoulCore.setSpiritualityQuietly(p,Float.MAX_VALUE);wheel.quickCast(p);
            equal(h,wheel.cooldownLeft(p,entry),0,"Sneak starts no cooldown");
        }
        h.succeed();
    }
    private static void refused(GameTestHelper h){
        ServerPlayer p=player(h,9);var wheel=GodOfDecay.DISEASE;var entry=wheel.entry("rot");
        float before=SoulCore.getSpirituality(p);h.assertTrue(wheel.cast(p,entry)==AbilityWheel.Result.REFUSED,"Sequence gate");
        equal(h,SoulCore.getSpirituality(p),before,"Refusal costs nothing");h.assertTrue(wheel.lastPick(p)==null,"Refusal does not change last selection");
        SoulCore.setSequence(p,6);SoulCore.setSpiritualityQuietly(p,Float.MAX_VALUE);before=SoulCore.getSpirituality(p);
        h.assertTrue(wheel.cast(p,wheel.entry("laziness"))==AbilityWheel.Result.REFUSED,"No target refused before payment");
        equal(h,SoulCore.getSpirituality(p),before,"No-target cast has no charge");
        h.succeed();
    }
    private static void incubation(GameTestHelper h){
        ServerPlayer p=player(h,6);Mob t=enemy(h,p,1000);
        GodOfDecayMarks.put(p,t,Kind.LATENT,360);equal(h,GodOfDecayMarks.count(p,t),0,"Latent does not count");
        h.assertTrue(!GodOfDecayMarks.has(t,Kind.LATENT),"Readers cannot detect latent");GodOfDecayMarks.cleanse(t);
        h.runAfterDelay(150,()->{equal(h,GodOfDecayMarks.count(p,t),0,"Still hidden before eight seconds");});
        h.runAfterDelay(165,()->{
            h.assertTrue(GodOfDecayMarks.has(t,Kind.LAZY)&&GodOfDecayMarks.has(t,Kind.OBESE)&&GodOfDecayMarks.has(t,Kind.DEGENERATE),"All three diseases break out");
            equal(h,GodOfDecayMarks.strength(t,Kind.LAZY),2,"Double-strength outbreak");h.succeed();
        });
    }
    private static void marks(GameTestHelper h){
        ServerPlayer p=player(h,9);Mob t=enemy(h,p,1000);Mob other=h.spawn(EntityTypes.COW,new net.minecraft.core.BlockPos(0,1,0));
        GodOfDecayMarks.put(p,t,Kind.INFECTED,80);GodOfDecayMarks.put(p,t,Kind.INFECTED,80);
        GodOfDecayMarks.put(p,t,Kind.DOOMED,80);GodOfDecayMarks.put(p,t,Kind.HOLLOWED,80);
        equal(h,GodOfDecayMarks.count(p,t),1,"Duplicates and nondisease marks do not inflate Sickness");
        equal(h,GodOfDecayMarks.count(other,t),0,"Owner isolation");
        h.assertTrue(GodOfDecayMarks.cleanse(t),"Ordinary diseases cleanse");equal(h,GodOfDecayMarks.count(p,t),0,"Clean count");
        SoulCore.setSequence(p,0);GodOfDecayMarks.put(p,t,Kind.INFECTED,80);GodOfDecayMarks.cleanse(t);
        equal(h,GodOfDecayMarks.count(p,t),1,"Second Law diseases resist cleansing");other.discard();h.succeed();
    }
    private static void restore(GameTestHelper h){
        ServerPlayer p=player(h,6);Mob t=enemy(h,p,1000);GodOfDecayMarks.put(p,t,Kind.DEGENERATE,60);
        h.runAfterDelay(35,()->h.assertTrue(t.getMaxHealth()<1000,"Degeneration reduces max health: "+t.getMaxHealth()+" marks="+t.getPersistentData().getCompoundOrEmpty(GodOfDecayMarks.DATA)));
        h.runAfterDelay(65,()->{equal(h,t.getMaxHealth(),1000,"Expiry restores attribute");GodOfDecayMarks.countedAge(p,t,60);});
        h.runAfterDelay(90,()->{equal(h,t.getMaxHealth(),1000,"Decay's Aged pip does not apply Senescence health loss");equal(h,GodOfDecayMarks.count(p,t),1,"Aged still counts as Sickness");h.succeed();});
    }
    private static void toggle(GameTestHelper h){
        ServerPlayer p=player(h,0);var wheel=GodOfDecay.DOOM;var entry=wheel.entry("swollen_form");
        h.assertTrue(wheel.cast(p,entry)==AbilityWheel.Result.DONE,"Form starts");SoulCore.setSpiritualityQuietly(p,0);
        wheel.quickCast(p);equal(h,SoulCore.getSpirituality(p),0,"Dismissal free despite cooldown and no spirituality");
        h.assertTrue(!GiantForm.isGiant(p)&&!GodOfDecayPowers.active(p,SWOLLEN_FORM),"Dismissal clears body and state");h.succeed();
    }
    private static void wards(GameTestHelper h){
        ServerPlayer p=player(h,6);Mob t=enemy(h,p,100);
        DeathWard.arm(t,100,.5f,null);Object old=DeathWard.snapshot(t);
        CertainDamage.strike(p,t,200,old);h.assertTrue(t.isAlive(),"Prior ward saves");equal(h,t.getHealth(),50,"Ward health fraction");
        h.assertTrue(!DeathWard.isArmed(t),"Prior ward consumed once");
        DeathWard.arm(t,100,.5f,null);CertainDamage.strike(p,t,200,old);
        h.assertTrue(!t.isAlive(),"Newly armed ward does not replace captured prior ward");h.succeed();
    }
    private static void weather(GameTestHelper h){
        ServerPlayer p=player(h,6);Mob t=enemy(h,p,1000);
        t.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.SPEED,100,0));
        Zone.at(t.position(),2).lasts(100).open(t,h.getLevel(),"test_weather");
        Dome.at(t.position(),2).lasts(100).raise(t,h.getLevel(),"test_weather");
        Weathering.area(p,h.getLevel(),t.position(),1,40);
        equal(h,t.getEffect(MobEffects.SPEED).getDuration(),60,"Effects age by extra ticks");
        h.assertTrue(Zone.ticksLeft(t,"test_weather")<=60&&Dome.ticksLeft(t,"test_weather")<=60,"Zone and dome lifetimes age");
        h.addCleanup(ok->{Zone.close(t,"test_weather");Dome.close(t,"test_weather");});h.succeed();
    }
    private static void held(GameTestHelper h){
        ServerPlayer p=player(h,6);Mob owner=enemy(h,p,1000);
        GodOfDecayMarks.put(owner,p,Kind.SEGMENTED,160);int[] calls={0};
        h.assertTrue(HeldActions.defer(p,()->calls[0]++),"Cast is held");equal(h,calls[0],0,"No immediate execution");
        h.runAfterDelay(30,()->equal(h,calls[0],0,"Still held before two seconds"));
        h.runAfterDelay(45,()->{equal(h,calls[0],1,"Runs once after delay");h.succeed();});
    }
    private static void division(GameTestHelper h){
        ServerPlayer p=player(h,5);p.setPos(p.getX(),p.getY()+10,p.getZ());
        for(int x=-5;x<=5;x++)for(int z=-5;z<=5;z++)h.getLevel().setBlock(p.blockPosition().offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        var result=GodOfDecayPowers.cast(p,h.getLevel(),DIVISION);
        h.assertTrue(GodOfDecayPowers.hasDoubles(p),"Double is a real destructible construct: "+result+" spot="+Spells.freeSpotNear(p,h.getLevel(),p.position(),Spells.flatLook(p),3));
        int count=Construct.ofOwner(p,GodOfDecayPowers.DOUBLE_GROUP).size();
        h.assertTrue(GodOfDecayPowers.cast(p,h.getLevel(),DIVISION)==AbilityWheel.Result.FREE,"Repeated Division is free");
        equal(h,Construct.ofOwner(p,GodOfDecayPowers.DOUBLE_GROUP).size(),count,"No duplicate spawning");
        h.assertTrue(GodOfDecayPowers.saveByDouble(p),"Double saves owner");
        equal(h,p.getHealth(),p.getMaxHealth()*.35f,"Reforms at 35 percent health");h.succeed();
    }
    private static void doom(GameTestHelper h){
        ServerPlayer p=player(h,4);Mob t=enemy(h,p,1000);
        GodOfDecayMarks.put(p,t,Kind.DOOMED,30);float before=t.getHealth();
        h.runAfterDelay(25,()->h.assertTrue(t.getHealth()<before,"Doom damages on its first second"));
        h.runAfterDelay(35,()->{h.assertTrue(t.getHealth()<before-250,"Doom collapses on expiry");h.assertTrue(!GodOfDecayMarks.has(t,Kind.DOOMED),"Expired mark removed");h.succeed();});
    }
    private static void projectileTempo(GameTestHelper h){
        ServerPlayer p=player(h,2);Mob shooter=enemy(h,p,1000);
        var arrow=h.spawn(EntityTypes.ARROW,new net.minecraft.core.BlockPos(1,2,1));
        arrow.setPos(p.getX()+4,p.getY()+5,p.getZ());arrow.setOwner(shooter);arrow.setNoGravity(true);
        arrow.setDeltaMovement(2,0,0);double start=arrow.getX();
        h.addCleanup(ok->arrow.discard());
        h.runAfterDelay(1,()->{
            double distance=arrow.getX()-start;
            h.assertTrue(distance>1.2&&distance<1.6,"Time Drag slows actual flight: "+distance);
            h.assertTrue(arrow.getDeltaMovement().x>1.8,"Natural velocity is restored rather than compounded");h.succeed();
        });
    }
    private static void support(GameTestHelper h){
        ServerPlayer p=player(h,7);Mob target=enemy(h,p,100);Mob provider=h.spawn(EntityTypes.HUSK,new net.minecraft.core.BlockPos(0,1,0));
        var board=h.getLevel().getScoreboard();var team=board.addPlayerTeam("decay_support_"+p.getId());
        board.addPlayerToTeam(target.getScoreboardName(),team);board.addPlayerToTeam(provider.getScoreboardName(),team);
        h.addCleanup(ok->{board.removePlayerTeam(team);provider.discard();});
        GodOfDecayMarks.put(p,target,Kind.CORRUPTED,100);target.setHealth(50);
        Support.heal(provider,target,10);equal(h,target.getHealth(),50,"Corruption blocks allied healing");
        h.assertTrue(!Support.effect(provider,target,new net.minecraft.world.effect.MobEffectInstance(MobEffects.RESISTANCE,100)),"Corruption blocks allied buffs");
        Support.heal(target,target,10);equal(h,target.getHealth(),60,"Self healing remains available");
        GodOfDecayMarks.cleanse(target);Support.heal(provider,target,10);equal(h,target.getHealth(),70,"Cleansing restores allied support");h.succeed();
    }
    private static void allies(GameTestHelper h){
        ServerPlayer p=player(h,6);Mob friend=enemy(h,p,100);var board=h.getLevel().getScoreboard();var team=board.addPlayerTeam("decay_test_"+p.getId());
        board.addPlayerToTeam(p.getScoreboardName(),team);board.addPlayerToTeam(friend.getScoreboardName(),team);
        h.addCleanup(ok->board.removePlayerTeam(team));
        equal(h,GodOfDecayMarks.put(p,friend,Kind.INFECTED,100),0,"Friendly target cannot be infected");
        GodOfDecayPowers.cast(p,h.getLevel(),ULCEROUS_BURST);equal(h,GodOfDecayMarks.count(p,friend),0,"AoE leaves allies safe");h.succeed();
    }
}
