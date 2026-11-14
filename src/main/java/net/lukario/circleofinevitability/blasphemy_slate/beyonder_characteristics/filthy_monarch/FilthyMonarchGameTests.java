package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.filthy_monarch;

import java.util.*;
import java.util.function.Consumer;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.filthy_monarch.FilthyMonarch.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PathwayAbilities;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

/** Real-server checks: costs, targeting, ownership, deferred effects, forms and cleanup. */
public final class FilthyMonarchGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS=
            DeferredRegister.create(Registries.TEST_FUNCTION,"circle_of_inevitability");
    static {
        FUNCTIONS.register("filthy_monarch_costs",()->FilthyMonarchGameTests::costs);
        FUNCTIONS.register("filthy_monarch_seeds",()->FilthyMonarchGameTests::seeds);
        FUNCTIONS.register("filthy_monarch_spree",()->FilthyMonarchGameTests::spree);
        FUNCTIONS.register("filthy_monarch_summons",()->FilthyMonarchGameTests::summons);
        FUNCTIONS.register("filthy_monarch_volley",()->FilthyMonarchGameTests::volley);
        FUNCTIONS.register("filthy_monarch_forms",()->FilthyMonarchGameTests::forms);
        FUNCTIONS.register("filthy_monarch_fields",()->FilthyMonarchGameTests::fields);
        FUNCTIONS.register("filthy_monarch_desire",()->FilthyMonarchGameTests::desire);
        FUNCTIONS.register("filthy_monarch_channel",()->FilthyMonarchGameTests::channel);
        FUNCTIONS.register("filthy_monarch_cleanup",()->FilthyMonarchGameTests::cleanup);
    }
    private static final Map<GameTestHelper,Integer> HEIGHTS=new WeakHashMap<>();
    private static int nextHeight;
    private static ServerPlayer player(GameTestHelper h,int sequence,double x) {
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(UUID.randomUUID(),"filthy"+UUID.randomUUID().toString().substring(0,6)),false);
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setNoGravity(true);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        Vec3 at=h.absoluteVec(new Vec3(x,HEIGHTS.computeIfAbsent(h,ignored->12+24*(nextHeight++%10)),1.5));
        p.setPos(at.x,at.y,at.z);p.setYRot(0);p.setXRot(0);
        SoulCore.setPathway(p,PATHWAY);SoulCore.setSequence(p,sequence);fund(p);
        initialize();h.onEachTick(p::doTick);
        h.addCleanup(ok->{clear(p);h.getLevel().getServer().getPlayerList().remove(p);});return p;
    }
    private static Mob target(GameTestHelper h,ServerPlayer p,double x,double z,float health) {
        Mob t=EntityTypes.HUSK.create(h.getLevel(),EntitySpawnReason.MOB_SUMMONED);
        Vec3 at=p.position().add(x,0,z);t.setPos(at.x,at.y,at.z);t.setNoAi(true);t.setNoGravity(true);
        t.getPersistentData().putBoolean("circle_of_inevitability_beyonder_rolled",true);
        t.getAttribute(Attributes.ARMOR).setBaseValue(0);t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);t.setHealth(health);
        h.getLevel().addFreshEntity(t);h.addCleanup(ok->t.discard());return t;
    }
    private static void fund(ServerPlayer p){SoulCore.setSpiritualityQuietly(p,Beyonder.maxSpirituality(p));}
    private static AbilityWheel.Result cast(ServerPlayer p,String id) {
        for(var w:WHEELS)if(w.entry(id)!=null)return w.cast(p,w.entry(id));throw new IllegalArgumentException(id);
    }
    private static void floor(GameTestHelper h,ServerPlayer p) {
        for(int x=-5;x<=5;x++)for(int z=-5;z<=28;z++) {
            BlockPos pos=BlockPos.containing(p.position().add(x,-1,z));var before=h.getLevel().getBlockState(pos);
            h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());h.addCleanup(ok->h.getLevel().setBlockAndUpdate(pos,before));
        }
    }
    private static void costs(GameTestHelper h) {
        var p=player(h,9,1.5);
        h.assertTrue(WHEELS.stream().mapToInt(w->w.entries().size()).sum()==18,"Exactly eighteen casts are registered");
        h.assertTrue(PathwayAbilities.of(PATHWAY).size()==5,"Only implemented keys enter the ability catalog");
        h.assertTrue(cast(p,"lawless_lunge")==AbilityWheel.Result.REFUSED&&SoulCore.getSpirituality(p)==20,"No-target lunge costs nothing");
        var t=target(h,p,0,4,1000);float before=t.getHealth();
        key(p,p.level(),p.level(),1,false);
        h.assertTrue(SoulCore.getSpirituality(p)==10&&t.getHealth()<before&&p.distanceToSqr(t)<4,"G lunges, lands safely and pays 10");
        p.setPos(t.getX(),t.getY(),t.getZ()-3);p.setYRot(0);p.setXRot(0);p.setShiftKeyDown(true);
        key(p,p.level(),p.level(),1,false);p.setShiftKeyDown(false);
        h.assertTrue(SoulCore.getSpirituality(p)==0&&Blind.isBlind(t)&&Falter.chance(t)>=.4f,"Sneak G blinds and makes attacks falter for 10");
        before=t.getHealth();h.assertTrue(cast(p,"sulfur_flame")==AbilityWheel.Result.REFUSED&&t.getHealth()==before,"Sequence gate is enforced");
        SoulCore.setPathway(p,"Wanderer");SoulCore.setSequence(p,8);fund(p);
        key(p,p.level(),p.level(),2,true);
        h.assertTrue(SoulCore.getSpirituality(p)==200&&Depravity.count(p,t)==1,"Borrowed direct cast is usable, charged once and seeds its target");
        p.setItemSlot(EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
        p.addEffect(new MobEffectInstance(MobEffects.STRENGTH,80,4));
        h.assertTrue(heldDamage(p)==7,"Lunge reads held-item damage without double-counting Strength");
        h.succeed();
    }
    private static void seeds(GameTestHelper h) {
        var p=player(h,8,1.5);var q=player(h,8,12);var t=target(h,p,0,4,1000);var friend=target(h,p,1,4,1000);
        Friends.add(p,friend.getUUID(),"friend",false);
        h.assertTrue(cast(p,"sulfur_flame")==AbilityWheel.Result.DONE,"Flame casts");
        h.assertTrue(Depravity.count(p,t)==1&&Depravity.count(q,t)==0&&friend.getHealth()==1000,"Seeds and cone damage respect ownership and friendship");
        Depravity.plant(p,t,9,120);Depravity.plant(q,t,2,120);
        h.assertTrue(Depravity.count(p,t)==5&&Depravity.spend(q,t)==2&&Depravity.count(p,t)==5,"Seeds cap at five; spending preserves another caster");
        Marks.age(t,120);h.assertTrue(Depravity.count(p,t)==4,"One seed wears off per interval");
        t.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,100,0));t.removeEffect(MobEffects.POISON);
        float before=t.getHealth();
        h.runAfterDelay(24,()->{
            h.assertTrue(t.getHealth()==before,"Fire resistance stops the ordinary blue-fire burn");
            Cleansing.cleanseStatuses(t);h.assertTrue(Depravity.count(p,t)==0,"Cleansing removes Seed status");h.succeed();
        });
    }
    private static void spree(GameTestHelper h) {
        var p=player(h,7,1.5);var t=target(h,p,0,3,1000);
        h.assertTrue(cast(p,"killing_spree")==AbilityWheel.Result.DONE,"Spree starts");
        h.runAfterDelay(9,()->{
            h.assertTrue(t.getHealth()<=946&&t.getHealth()>940&&Bleeding.isBleeding(t),"Three 18-damage cuts land before bloodlust: "+t.getHealth());
            h.assertTrue(state(p).spreeUntil>p.level().getGameTime()&&p.hasEffect(MobEffects.SPEED),"Bloodlust follows the cuts");
            long before=state(p).spreeUntil;var victim=target(h,p,2,3,5);Spells.strike(p,victim,20);
            h.assertTrue(offerings(state(p))==1&&state(p).spreeUntil==before+60,"A kill grants an Offering and extends bloodlust");h.succeed();
        });
    }
    private static void summons(GameTestHelper h) {
        var p=player(h,7,1.5);floor(h,p);
        h.assertTrue(cast(p,"devil_worship")==AbilityWheel.Result.DONE,"Cut-short ritual still summons");
        var first=Construct.ofOwner(p,SUMMONS).getFirst();h.assertTrue(first.getMaxHealth()==60&&Marionette.isSummoned(first),"No Offerings gives half health and no-drop summon ownership");
        var s=state(p);offer(s);offer(s);offer(s);fund(p);
        h.assertTrue(cast(p,"devil_worship")==AbilityWheel.Result.DONE,"Funded ritual recasts");
        var second=Construct.ofOwner(p,SUMMONS).getFirst();
        h.assertTrue(second!=first&&first.isRemoved()&&Construct.ofOwner(p,SUMMONS).size()==1,"Recasting replaces the old projection at Sequence 7");
        h.assertTrue(Math.abs(second.getMaxHealth()-192)<.01&&second.getPersistentData().getBooleanOr("filthy_monarch_volley",false),"Three Offerings grant 60% health and fireballs");
        s.offerings.clear();s.offerings.add(s.now()-1);s.offerings.add(s.now()+40);
        h.assertTrue(offerings(s)==1,"Offerings expire independently");clear(p);
        h.assertTrue(second.isRemoved(),"Owner cleanup dismisses projections");h.succeed();
    }
    private static void volley(GameTestHelper h) {
        var p=player(h,6,1.5);var t=target(h,p,0,5,1000);var friend=target(h,p,2,5,1000);Friends.add(p,friend.getUUID(),"friend",false);
        h.assertTrue(cast(p,"sulfur_fireball")==AbilityWheel.Result.DONE&&state(p).balls.size()==3,"Normal cast fires three physical flights");
        float after=SoulCore.getSpirituality(p);
        h.assertTrue(cast(p,"sulfur_fireball")==AbilityWheel.Result.REFUSED&&SoulCore.getSpirituality(p)==after,"Open-wheel picks honor their cooldown");
        h.runAfterDelay(10,()->{
            h.assertTrue(t.getHealth()<=972&&Depravity.count(p,t)==1&&friend.getHealth()==1000,"Volley health="+t.getHealth()+", Seeds="+Depravity.count(p,t)+", friend="+friend.getHealth());
            fund(p);float money=SoulCore.getSpirituality(p);DEVIL_ARTS.quickCast(p);
            h.assertTrue(SoulCore.getSpirituality(p)==money-1200,"Quick recast ignores the cooldown at the same price");
            clear(p);SoulCore.setSequence(p,4);fund(p);DEVIL_ARTS.reset(p);
            h.assertTrue(cast(p,"devil_form")==AbilityWheel.Result.DONE,"Demon Form starts");
            fund(p);h.assertTrue(cast(p,"sulfur_fireball")==AbilityWheel.Result.DONE&&state(p).balls.size()==20,"Demon Form upgrades to twenty fireballs");h.succeed();
        });
    }
    private static void forms(GameTestHelper h) {
        var p=player(h,5,1.5);var t=target(h,p,0,2,1000);double original=p.getAttributeValue(Attributes.SCALE);
        h.assertTrue(cast(p,"devil_form")==AbilityWheel.Result.DONE&&p.getAttributeValue(Attributes.SCALE)>original,"Devil Form changes real size and reach");
        SoulCore.setSpiritualityQuietly(p,0);
        h.assertTrue(cast(p,"devil_form")==AbilityWheel.Result.FREE&&p.getAttributeValue(Attributes.SCALE)==original,"Form dismisses free through its cooldown and removes size");
        fund(p);h.assertTrue(cast(p,"desire_incarnation")==AbilityWheel.Result.DONE,"Liquid form starts");
        p.removeEffect(MobEffects.RESISTANCE);p.invulnerableTime=0;float hp=p.getHealth();
        p.hurtServer(p.level(),t.damageSources().mobAttack(t),10);
        h.assertTrue(p.getHealth()>=hp-2.01f,"Liquid form reduces damage by 80%");
        t.setTarget(p);h.assertTrue(t.getTarget()==null,"Mobs cannot acquire the liquid form");
        h.runAfterDelay(24,()->{
            h.assertTrue(t.getHealth()<1000&&Depravity.count(p,t)>0,"Liquid aura damages and plants Seeds");
            SoulCore.setSpiritualityQuietly(p,0);h.assertTrue(cast(p,"desire_incarnation")==AbilityWheel.Result.FREE&&!Form.isIn(p,LIQUID),"Liquid form ends for free");h.succeed();
        });
    }
    private static void fields(GameTestHelper h) {
        var p=player(h,4,1.5);floor(h,p);var t=target(h,p,0,4,1000);var friend=target(h,p,1,4,1000);Friends.add(p,friend.getUUID(),"friend",false);
        h.assertTrue(cast(p,"flame_cage")==AbilityWheel.Result.DONE&&Root.has(t,RootRestriction.MOVE),"Cage roots its target");
        h.assertTrue(Wall.isRaised(p,state(p).tag("cage_wall_0")),"Cage registers projectile-catching walls");
        var s=state(p);FilthyMonarchDevil.close(s,"cage");h.assertTrue(!Root.has(t,RootRestriction.MOVE),"Cage removal clears its owned root");
        p.setXRot(20);fund(p);h.assertTrue(cast(p,"volcanic_eruption")==AbilityWheel.Result.DONE,"Eruption warns");
        Vec3 at=Spells.aimGround(p,p.level(),22);t.setPos(at.x,at.y,at.z);friend.setPos(at.x+1,at.y,at.z);
        BlockPos ground=BlockPos.containing(at.add(0,-.1,0));var before=p.level().getBlockState(ground);float health=t.getHealth();
        h.runAfterDelay(10,()->h.assertTrue(t.getHealth()==health,"Eruption cannot damage during its warning"));
        h.runAfterDelay(24,()->{
            h.assertTrue(t.getHealth()<health&&friend.getHealth()==1000&&p.level().getBlockState(ground).equals(before),"Eruption health="+t.getHealth()+" (before "+health+"), friend="+friend.getHealth()+", terrain unchanged="+p.level().getBlockState(ground).equals(before));
            t.setPos(at.x,at.y,at.z);t.setDeltaMovement(Vec3.ZERO);fund(p);
            h.assertTrue(cast(p,"toxic_vapor")==AbilityWheel.Result.DONE,"Fog opens");
            var f=s.fields.get("fog");t.setPos(f.at.x,f.at.y,f.at.z);
            h.runAfterDelay(66,()->{h.assertTrue(Corrosion.isCorroding(t),"Demon fog corrodes after three continuous seconds");h.succeed();});
        });
    }
    private static void desire(GameTestHelper h) {
        var p=player(h,5,1.5);var t=target(h,p,0,4,1000);var other=target(h,p,1,4,1000);
        Depravity.plant(p,t,2,120);
        h.assertTrue(cast(p,"desire_control")==AbilityWheel.Result.DONE&&Depravity.count(p,t)==5,"Desire Control plants three Seeds");
        h.assertTrue(t.getHealth()<850&&Taunt.targetOf(t)==other&&Falter.chance(t)>=.4,"Five Seeds erupt and turn the mob on its nearest enemy");
        fund(p);h.assertTrue(cast(p,"mental_shock")==AbilityWheel.Result.DONE&&Fear.isAfraid(t),"Mental Shock applies cone fear and stagger");
        float hp=t.getHealth();fund(p);h.assertTrue(cast(p,"sword_of_lava")==AbilityWheel.Result.DONE&&t.getHealth()<hp-80,"Sword of Lava performs its wide sweep");
        fund(p);h.assertTrue(cast(p,"sink")==AbilityWheel.Result.DONE&&Root.has(t,RootRestriction.JUMP)&&Root.has(other,RootRestriction.JUMP),"Sink splashes to nearby enemies at Sequence 5");
        Root.remove(p);floor(h,p);p.setXRot(0);Vec3 from=p.position();fund(p);
        h.assertTrue(cast(p,"shadow_shift")==AbilityWheel.Result.DONE&&p.position().distanceToSqr(from)>16,"Shadow-Shift moves safely along the aimed route");
        t.setPos(from.x,from.y,from.z);t.setDeltaMovement(Vec3.ZERO);float before=t.getHealth();
        h.runAfterDelay(34,()->{h.assertTrue(t.getHealth()<before,"The old shadow bursts after 1.5 seconds");h.succeed();});
    }
    private static void channel(GameTestHelper h) {
        var p=player(h,1,1.5);var t=target(h,p,0,5,1000);
        t.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,200,0));
        h.assertTrue(cast(p,"flames_of_the_abyss")==AbilityWheel.Result.DONE&&Root.has(p,RootRestriction.MOVE),"Channel charges and fixes the caster in place");
        h.runAfterDelay(3,()->{
            h.assertTrue(t.getHealth()<950&&Depravity.count(p,t)==1&&Corrosion.isCorroding(t),"Fire resistance does not block the corruption share");
            Silence.apply(p,40);h.runAfterDelay(3,()->{
                h.assertTrue(state(p).channelUntil==0&&!Root.has(p,RootRestriction.MOVE),"Silence interrupts the channel and releases its movement root");
                Silence.lift(p);clear(p);SoulCore.setSequence(p,4);fund(p);DEVIL_ARTS.reset(p);p.setYRot(0);p.setXRot(0);
                float before=t.getHealth();h.assertTrue(cast(p,"hellfire")==AbilityWheel.Result.DONE,"Hellfire starts its warning");
                h.runAfterDelay(24,()->{h.assertTrue(t.getHealth()<before-100,"Hellfire lands despite Fire Resistance");h.succeed();});
            });
        });
    }
    private static void cleanup(GameTestHelper h) {
        var p=player(h,6,1.5);var q=player(h,6,12);var t=target(h,p,0,4,1000);floor(h,p);
        seed(state(p),t,3);seed(state(q),t,2);
        cast(p,"flame_cage");Root.applyOwned(t,"other_owner",200,Set.of(RootRestriction.JUMP));
        fund(p);cast(p,"sulfur_fireball");fund(p);cast(p,"volcanic_eruption");
        Taunt.apply(t,q,100);String firstTaunt=Taunt.snapshot(t);Taunt.apply(t,q,100);
        Taunt.removeIf(t,firstTaunt);h.assertTrue(Taunt.isTaunted(t),"Cleanup cannot erase a same-target replacement taunt");
        Taunt.removeIf(t,Taunt.snapshot(t));h.assertTrue(!Taunt.isTaunted(t),"Matching application token removes its taunt");
        float before=t.getHealth();var old=state(p);SoulCore.setPathway(p,"Wanderer");
        h.runAfterDelay(3,()->{
            h.assertTrue(!STATES.containsKey(p.getUUID())&&old.balls.isEmpty()&&old.fields.isEmpty(),"Pathway loss clears active flights and fields");
            h.assertTrue(Depravity.count(p,t)==0&&Depravity.count(q,t)==2&&Root.has(t,RootRestriction.JUMP),"Cleanup preserves another caster's marks and root");
            h.runAfterDelay(24,()->{h.assertTrue(t.getHealth()==before,"Queued damage cannot fire after owner cleanup");h.succeed();});
        });
    }
}
