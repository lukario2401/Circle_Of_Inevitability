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
        FUNCTIONS.register("filthy_monarch_costs",()->h->prepare(h,FilthyMonarchGameTests::costs));
        FUNCTIONS.register("filthy_monarch_seeds",()->h->prepare(h,FilthyMonarchGameTests::seeds));
        FUNCTIONS.register("filthy_monarch_spree",()->h->prepare(h,FilthyMonarchGameTests::spree));
        FUNCTIONS.register("filthy_monarch_summons",()->h->prepare(h,FilthyMonarchGameTests::summons));
        FUNCTIONS.register("filthy_monarch_volley",()->h->prepare(h,FilthyMonarchGameTests::volley));
        FUNCTIONS.register("filthy_monarch_forms",()->h->prepare(h,FilthyMonarchGameTests::forms));
        FUNCTIONS.register("filthy_monarch_fields",()->h->prepare(h,FilthyMonarchGameTests::fields));
        FUNCTIONS.register("filthy_monarch_desire",()->h->prepare(h,FilthyMonarchGameTests::desire));
        FUNCTIONS.register("filthy_monarch_channel",()->h->prepare(h,FilthyMonarchGameTests::channel));
        FUNCTIONS.register("filthy_monarch_cleanup",()->h->prepare(h,FilthyMonarchGameTests::cleanup));
        FUNCTIONS.register("filthy_monarch_words",()->h->prepare(h,FilthyMonarchGameTests::words));
        FUNCTIONS.register("filthy_monarch_word_range",()->h->prepare(h,FilthyMonarchGameTests::word_range));
        FUNCTIONS.register("filthy_monarch_links",()->h->prepare(h,FilthyMonarchGameTests::links));
        FUNCTIONS.register("filthy_monarch_malice",()->h->prepare(h,FilthyMonarchGameTests::malice));
        FUNCTIONS.register("filthy_monarch_phantoms",()->h->prepare(h,FilthyMonarchGameTests::phantoms));
        FUNCTIONS.register("filthy_monarch_blood",()->h->prepare(h,FilthyMonarchGameTests::blood));
        FUNCTIONS.register("filthy_monarch_swamp",()->h->prepare(h,FilthyMonarchGameTests::swamp));
        FUNCTIONS.register("filthy_monarch_tide",()->h->prepare(h,FilthyMonarchGameTests::tide));
        FUNCTIONS.register("filthy_monarch_corroder",()->h->prepare(h,FilthyMonarchGameTests::corroder));
        FUNCTIONS.register("filthy_monarch_kingdom",()->h->prepare(h,FilthyMonarchGameTests::kingdom));
        FUNCTIONS.register("filthy_monarch_passives",()->h->prepare(h,FilthyMonarchGameTests::passives));
        FUNCTIONS.register("filthy_monarch_kit",()->h->prepare(h,FilthyMonarchGameTests::kit));
    }
    private static final Map<GameTestHelper,Integer> HEIGHTS=new WeakHashMap<>();
    private static int nextHeight;
    private static void prepare(GameTestHelper h,Consumer<GameTestHelper> test) {
        int slot=HEIGHTS.computeIfAbsent(h,ignored->nextHeight++);
        var center=BlockPos.containing(h.absoluteVec(new Vec3(1.5+slot*256,48,1.5)));
        int cx=center.getX()>>4,cz=center.getZ()>>4;
        for(int x=cx-2;x<=cx+2;x++)for(int z=cz-2;z<=cz+2;z++) {
            final int a=x,b=z;
            if(h.getLevel().setChunkForced(x,z,true))h.addCleanup(ok->h.getLevel().setChunkForced(a,b,false));
        }
        h.startSequence().thenIdle(20).thenExecute(()->test.accept(h));
    }
    private static ServerPlayer player(GameTestHelper h,int sequence,double x) {
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(UUID.randomUUID(),"filthy"+UUID.randomUUID().toString().substring(0,6)),false);
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setNoGravity(true);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        int slot=HEIGHTS.computeIfAbsent(h,ignored->nextHeight++);
        Vec3 at=h.absoluteVec(new Vec3(x+slot*256,48,1.5));
        h.getLevel().getChunkAt(BlockPos.containing(at));
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
        h.assertTrue(WHEELS.stream().mapToInt(w->w.entries().size()).sum()==35,"Exactly thirty-five casts are registered");
        h.assertTrue(PathwayAbilities.of(PATHWAY).size()==7,"All seven implemented keys enter the ability catalog");
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
        p.hurtServer(p.level(),t.damageSources().mobAttack(t),10);p.setDeltaMovement(Vec3.ZERO);
        h.assertTrue(p.getHealth()>=hp-2.01f,"Liquid form reduces damage by 80%");
        t.setTarget(p);h.assertTrue(t.getTarget()==null,"Mobs cannot acquire the liquid form");
        h.runAfterDelay(24,()->{
            h.assertTrue(t.getHealth()<1000&&Depravity.count(p,t)>0,"Liquid aura health="+t.getHealth()+", Seeds="+Depravity.count(p,t)+", distance="+p.distanceTo(t)+", form="+Form.isIn(p,LIQUID));
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

    private static void words(GameTestHelper h) {
        var p=player(h,4,1.5);var t=target(h,p,0,4,1000);var q=player(h,6,16);
        var friend=target(h,p,1,4,1000);Friends.add(p,friend.getUUID(),"safe",false);
        h.assertTrue(cast(p,"slow")==AbilityWheel.Result.DONE&&t.hasEffect(MobEffects.SLOWNESS),"Slow damages and slows nearby enemies");
        Depravity.plant(p,t,3,120);Depravity.plant(q,t,2,120);float before=t.getHealth();fund(p);
        h.assertTrue(cast(p,"die")==AbilityWheel.Result.DONE&&t.getHealth()<before-140&&Depravity.count(p,t)==0&&Depravity.count(q,t)==2,"Die spends only the caster's Seeds");
        fund(p);cast(p,"corruption");h.assertTrue(Depravity.count(p,t)==2&&friend.getHealth()==1000,"Corruption plants two Seeds and respects allies");
        fund(p);cast(p,"confusion");h.assertTrue(Falter.chance(t)>=.4&&t.hasEffect(MobEffects.NAUSEA),"Confusion applies mind effects");
        fund(p);cast(p,"sloth");h.assertTrue(AttackTempo.multiplier(t)==.5&&t.hasEffect(MobEffects.WEAKNESS),"Sloth halves attack tempo");
        fund(p);cast(p,"blasphemy");h.assertTrue(Silence.ticksLeft(t)>=95&&Depravity.count(p,t)==0,"Blasphemy spends Seeds for longer silence");
        before=t.getHealth();final float previous=before;
        h.runAfterDelay(25,()->{h.assertTrue(t.getHealth()<previous-10,"Sloth and corruption continue dealing damage");h.succeed();});
    }
    private static void word_range(GameTestHelper h) {
        var p=player(h,3,1.5);var t=target(h,p,0,40,1000);
        BlockPos wall=BlockPos.containing(p.getEyePosition().add(0,0,3));var old=h.getLevel().getBlockState(wall);
        h.getLevel().setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());h.addCleanup(ok->h.getLevel().setBlockAndUpdate(wall,old));
        h.assertTrue(cast(p,"blasphemy")==AbilityWheel.Result.DONE&&t.getHealth()<900,"Blatherer words double reach and penetrate terrain");
        t.setPos(t.getX()+8,t.getY(),t.getZ());Depravity.plant(p,t,4,120);fund(p);float before=t.getHealth();
        h.assertTrue(cast(p,"blather")==AbilityWheel.Result.DONE&&t.getHealth()<before-300&&Depravity.count(p,t)==0,"Blather finds the strongest marked target out of aim");
        h.assertTrue(state(p).curses.containsKey(t.getUUID()),"Blather leaves its caster-specific vulnerability");
        Silence.apply(p,40);float money=SoulCore.getSpirituality(p);
        h.assertTrue(cast(p,"slow")==AbilityWheel.Result.REFUSED&&SoulCore.getSpirituality(p)==money,"Silence rejects words without payment");h.succeed();
    }
    private static void links(GameTestHelper h) {
        var p=player(h,5,1.5);var t=target(h,p,0,4,1000);
        h.assertTrue(cast(p,"desire_symbiosis")==AbilityWheel.Result.DONE&&DamageLink.redirectTarget(p)==t,"Symbiosis arms a redirect");
        p.removeEffect(MobEffects.RESISTANCE);float before=p.getHealth(),enemyBefore=t.getHealth();
        p.hurtServer(p.level(),p.damageSources().generic(),4);
        h.assertTrue(Math.abs(p.getHealth()-(before-3))<.1&&t.getHealth()<enemyBefore,"Quarter of a blow reaches the linked enemy");
        p.invulnerableTime=0;p.hurtServer(p.level(),p.damageSources().generic(),100);
        h.assertTrue(p.isAlive()&&p.getHealth()==1&&Invulnerable.isGranted(p)&&DamageLink.redirectTarget(p)==null,"Lethal damage consumes symbiosis and leaves one health");
        Invulnerable.end(p);p.setHealth(p.getMaxHealth());DESIRE.reset(p);fund(p);cast(p,"desire_symbiosis");
        Object mine=state(p).wardToken;DeathWard.arm(p,200,.5f,null);clear(p);
        h.assertTrue(DeathWard.isArmed(p)&&DeathWard.snapshot(p)!=mine,"Owner cleanup preserves a replacement ward");DeathWard.disarm(p);h.succeed();
    }
    private static void malice(GameTestHelper h) {
        var p=player(h,2,1.5);var t=target(h,p,0,5,1000);floor(h,p);
        h.assertTrue(cast(p,"seed_of_malice")==AbilityWheel.Result.DONE,"Malice anchors a living enemy");float before=t.getHealth();
        p.hurtServer(p.level(),p.damageSources().generic(),1000);
        h.assertTrue(p.isAlive()&&Math.abs(p.getHealth()-p.getMaxHealth()*.4f)<.1&&p.distanceToSqr(t)<5,"Malice revives beside its anchor with forty percent health");
        h.assertTrue(t.getHealth()<before-90&&state(p).malice==null,"Malice hurts its anchor and is consumed");
        Invulnerable.end(p);fund(p);DESIRE.reset(p);p.setPos(t.getX(),t.getY(),t.getZ()-4);cast(p,"seed_of_malice");
        t.discard();h.assertTrue(!FilthyMonarchDesire.anchored(state(p)),"A dead anchor cannot provide rebirth");h.succeed();
    }
    private static void phantoms(GameTestHelper h) {
        var p=player(h,4,1.5);var t=target(h,p,0,5,1000);var friend=target(h,p,1,5,1000);
        Friends.add(p,friend.getUUID(),"safe",false);t.setTarget(p);
        h.assertTrue(cast(p,"filthy_illusion")==AbilityWheel.Result.DONE&&state(p).phantoms.size()==3,"Three real decoys are summoned");
        h.assertTrue(Taunt.targetOf(t)!=p&&Taunt.targetOf(t)!=null,"Existing aggression redirects to a decoy");
        h.runAfterDelay(30,()->{h.assertTrue(t.getHealth()<900&&Depravity.count(p,t)>=2&&friend.getHealth()==1000,"Pursuing phantoms burst on contact without friendly damage");h.succeed();});
    }
    private static void blood(GameTestHelper h) {
        var p=player(h,2,1.5);var t=target(h,p,0,4,1000);
        p.setHealth(5);float money=SoulCore.getSpirituality(p);
        h.assertTrue(cast(p,"blood_burst")==AbilityWheel.Result.REFUSED&&p.getHealth()==5&&SoulCore.getSpirituality(p)==money,"Low-health blood price refuses without charging");
        p.setHealth(20);h.assertTrue(cast(p,"blood_burst")==AbilityWheel.Result.DONE&&p.getHealth()==16&&Corrosion.isCorroding(t),"Blood Burst pays twenty percent and corrodes");
        Corrosion.remove(t);t.removeEffect(MobEffects.POISON);t.setPos(p.getX(),p.getY(),p.getZ()+4);t.setDeltaMovement(Vec3.ZERO);
        seed(state(p),t,5);fund(p);h.assertTrue(cast(p,"blood_ignition")==AbilityWheel.Result.DONE&&Depravity.count(p,t)==0&&NoHeal.isApplied(t),"Ignition spends Seeds and prevents healing");
        float before=t.getHealth();h.runAfterDelay(23,()->{
            h.assertTrue(before-t.getHealth()>=29&&before-t.getHealth()<=31,"Boss ignition damage is capped at three percent after multipliers");
            float wounded=t.getHealth();Healing.apply(t,100);h.assertTrue(t.getHealth()==wounded,"Ignition blocks spell healing too");h.succeed();
        });
    }
    private static void swamp(GameTestHelper h) {
        var p=player(h,4,1.5);floor(h,p);p.setXRot(25);Vec3 at=Spells.aimGround(p,p.level(),24);
        var t=target(h,p,at.x-p.getX(),at.z-p.getZ(),1000);t.setPos(at.x,at.y,at.z);
        t.setItemSlot(EquipmentSlot.CHEST,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_CHESTPLATE));
        h.assertTrue(cast(p,"acid_swamp")==AbilityWheel.Result.DONE,"Acid swamp casts");
        h.runAfterDelay(86,()->{
            h.assertTrue(t.getHealth()<950&&Corrosion.isCorroding(t)&&t.getItemBySlot(EquipmentSlot.CHEST).getDamageValue()>=9,"Continuous acid exposure deals damage, wears armor and escalates corrosion");
            String zone=state(p).tag("acid");clear(p);h.assertTrue(!Zone.isOpen(p,zone),"Acid field closes with its owner");h.succeed();
        });
    }
    private static void tide(GameTestHelper h) {
        var p=player(h,1,1.5);var t=target(h,p,0,18,1000);var friend=target(h,p,1,18,1000);Friends.add(p,friend.getUUID(),"safe",false);
        h.assertTrue(cast(p,"tide_of_filth")==AbilityWheel.Result.DONE&&t.getHealth()==1000,"Tide travels before reaching a distant enemy");
        h.runAfterDelay(26,()->{
            h.assertTrue(t.getHealth()<850&&Corrosion.isCorroding(t)&&Root.has(t,RootRestriction.MOVE)&&friend.getHealth()==1000,"Tide strikes, leaves corrosion and grasps only enemies");
            var old=state(p);clear(p);h.assertTrue(!Root.has(t,RootRestriction.MOVE)&&old.zones.isEmpty(),"Tide ownership cleanup releases hands and fields");h.succeed();
        });
    }
    private static void corroder(GameTestHelper h) {
        var p=player(h,1,1.5);var owner=target(h,p,8,8,1000);var t=target(h,p,0,10,1000);
        Wall.at(p.position().add(0,0,5),0,8,6).lasts(200).raise(owner,p.level(),"test_wall");
        Dome.sphere(t.position().add(0,1,0),3).lasts(200).raise(owner,p.level(),"test_dome");
        Wall.at(p.position().add(0,0,3),0,8,6).lasts(200).raise(p,p.level(),"friendly_wall");
        h.addCleanup(ok->{Wall.close(owner,"test_wall");Dome.close(owner,"test_dome");Wall.close(p,"friendly_wall");});
        h.assertTrue(cast(p,"the_corroder")==AbilityWheel.Result.DONE,"Corroder acquires a target through barriers");
        h.runAfterDelay(5,()->h.assertTrue(Wall.isWeakened(owner,"test_wall")&&Dome.isWeakened(owner,"test_dome"),"Contact immediately weakens both barrier kinds"));
        h.runAfterDelay(45,()->h.assertTrue(!Wall.isRaised(owner,"test_wall")&&!Dome.isRaised(owner,"test_dome")&&Wall.isRaised(p,"friendly_wall"),"Two seconds of continuous contact breaks only hostile barriers"));
        h.runAfterDelay(85,()->{h.assertTrue(t.getHealth()<550&&!state(p).channel.active()&&!Root.has(p,RootRestriction.MOVE),"Four growing pulses complete and release the caster");h.succeed();});
    }
    private static void kingdom(GameTestHelper h) {
        var p=player(h,0,1.5);var t=target(h,p,0,4,1000);
        h.assertTrue(cast(p,"abyssal_sea")==AbilityWheel.Result.DONE&&Dome.isInside(p,SEA,p)&&costScale(p)==.5,"Sea seals its shell and halves prices");
        fund(p);cast(p,"the_abyss");h.assertTrue(costScale(p)==.375,"Abyss form stacks its twenty-five percent discount multiplicatively");
        p.setHealth(10);float before=t.getHealth();Spells.strike(p,t,20);
        h.assertTrue(t.getHealth()<before-29&&p.getHealth()>10&&Depravity.count(p,t)>0,"Abyss attacks gain damage, lifesteal and Seeds");
        float money=SoulCore.getSpirituality(p);cast(p,"sulfur_flame");h.assertTrue(money-SoulCore.getSpirituality(p)==75,"Direct entries receive both discounts");
        h.runAfterDelay(25,()->{
            h.assertTrue(Depravity.count(p,t)>=3&&t.getHealth()<850,"Sea grows Seeds and damages every second");
            SoulCore.setSpiritualityQuietly(p,0);h.assertTrue(cast(p,"abyssal_sea")==AbilityWheel.Result.FREE&&!Dome.isRaised(p,SEA),"Sea dismisses free with no funds");
            h.assertTrue(cast(p,"the_abyss")==AbilityWheel.Result.FREE&&!Form.isIn(p,ABYSS),"Abyss form dismisses free with no funds");h.succeed();
        });
    }
    private static void passives(GameTestHelper h) {
        var p=player(h,2,1.5);var t=target(h,p,0,8,1000);
        h.assertTrue(Information.blocked(p)&&!Bleeding.apply(p,t,80,2)&&!Corrosion.apply(p,t,2,1,80),"Interference and blood immunities register");
        Fear.apply(p,t,80);h.assertTrue(Fear.ticksLeft(p)<=40,"Coldblooded halves fear");
        p.addEffect(new MobEffectInstance(MobEffects.POISON,200,0));h.assertTrue(!p.hasEffect(MobEffects.POISON),"Poison is rejected immediately");
        FilthyMonarchDesire.armWard(state(p));p.hurtServer(p.level(),p.damageSources().generic(),1000);
        h.assertTrue(p.isAlive()&&Invulnerable.isGranted(p)&&Root.has(p,RootRestriction.ABILITIES)&&!FilthyMonarchPassives.dropReady(state(p)),"Single Drop starts invulnerable reformation and its cooldown");
        h.runAfterDelay(64,()->{
            h.assertTrue(p.getHealth()>=p.getMaxHealth()*.3f&&!Root.has(p,RootRestriction.ABILITIES),"Single Drop reforms after three seconds");
            var old=state(p);SoulCore.setPathway(p,"Wanderer");clear(p);
            h.assertTrue(!Physique.has(p,Attributes.MAX_HEALTH,Physique.id("filthy_coldblooded"))&&!FilthyMonarchPassives.dropReady(old),"Losing the pathway removes its body and passive ward eligibility");h.succeed();
        });
    }
    private static void kit(GameTestHelper h) {
        var p=player(h,9,1.5);var t=target(h,p,0,4,1000);
        float before=p.getHealth();h.assertTrue(!Spells.payBlood(p,Float.NaN,1)&&!Spells.payBlood(p,before,1)&&p.getHealth()==before,"Shared blood costs reject invalid and lethal payments atomically");
        Object token=DamageLink.redirectWhen(p,t,.25f,100,()->p.distanceToSqr(t)<25);
        t.setPos(t.getX()+20,t.getY(),t.getZ());h.assertTrue(DamageLink.redirectTarget(p)==null,"Redirect conditions are checked at access before a tick");
        Object old=DeathWard.armWhen(p,100,.5f,20,()->t.isAlive(),null);t.discard();
        h.assertTrue(!DeathWard.isArmed(p),"Conditional ward is invalid as soon as its anchor dies");
        DeathWard.arm(p,100,.5f,null);h.assertTrue(!DeathWard.disarmIf(p,old)&&DeathWard.isArmed(p),"Old ward cleanup cannot erase a replacement");DeathWard.disarm(p);
        DeathWard.arm(p,100,.5f,null);Object identical=DeathWard.snapshot(p);DeathWard.arm(p,100,.5f,null);
        h.assertTrue(!DeathWard.disarmIf(p,identical)&&DeathWard.isArmed(p),"Same-tick identical wards still have distinct ownership");DeathWard.disarm(p);
        Root.applyOwned(p,"test_foreign",100,Set.of(RootRestriction.JUMP));int[] pulses={0},ends={0};
        var channel=new Channel(p,60,20,0,true,()->true,i->pulses[0]++,()->ends[0]++);channel.tick();Silence.apply(p,20);channel.tick();channel.close();
        h.assertTrue(pulses[0]==1&&ends[0]==1&&!Root.has(p,RootRestriction.MOVE)&&Root.has(p,RootRestriction.JUMP),"Channel interrupts exactly once and preserves foreign roots");
        Root.removeOwned(p,"test_foreign");
        Vec3 middle=p.position().add(0,0,3);
        Wall.at(middle,0,4,4).raise(p,p.level(),"renew_test");Wall.weaken(p,"renew_test",40);Wall.renew(p,"renew_test");
        h.assertTrue(!Wall.isWeakened(p,"renew_test"),"Renewing a wall resets weakening with its lifetime");
        Object oldWall=Wall.crossedBy(p.level(),p.getEyePosition(),p.getEyePosition().add(0,0,6)).stream().filter(w->w.name().equals("renew_test")).findFirst().orElseThrow().token();
        Wall.at(middle,0,4,4).raise(p,p.level(),"renew_test");Wall.closeIf(p,"renew_test",oldWall);
        h.assertTrue(Wall.isRaised(p,"renew_test"),"A replacement barrier survives cleanup of the old instance");Wall.close(p,"renew_test");h.succeed();
    }
}
