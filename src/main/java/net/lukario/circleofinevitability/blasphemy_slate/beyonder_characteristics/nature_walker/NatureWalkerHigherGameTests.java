package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker;

import java.util.*;
import java.util.function.Consumer;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker.NatureWalker.*;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker.NatureWalkerGameTests.*;

/** Real-server coverage of the remaining nineteen entries and their shared extension contracts. */
public final class NatureWalkerHigherGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS=DeferredRegister.create(Registries.TEST_FUNCTION,"circle_of_inevitability");
    static {
        FUNCTIONS.register("nature_walker_high_alchemy",()->NatureWalkerHigherGameTests::alchemy);
        FUNCTIONS.register("nature_walker_high_mutations",()->NatureWalkerHigherGameTests::mutations);
        FUNCTIONS.register("nature_walker_high_return",()->NatureWalkerHigherGameTests::returns);
        FUNCTIONS.register("nature_walker_high_fields",()->NatureWalkerHigherGameTests::fields);
        FUNCTIONS.register("nature_walker_high_rebirth",()->NatureWalkerHigherGameTests::rebirth);
        FUNCTIONS.register("nature_walker_high_realms",()->NatureWalkerHigherGameTests::realms);
        FUNCTIONS.register("nature_walker_high_mother",()->NatureWalkerHigherGameTests::mother);
        FUNCTIONS.register("nature_walker_high_upgrades",()->NatureWalkerHigherGameTests::upgrades);
        FUNCTIONS.register("nature_walker_high_lifecycle",()->NatureWalkerHigherGameTests::lifecycle);
    }
    private static final Map<GameTestHelper,Integer> SPACES=new WeakHashMap<>();private static int next;
    private static ServerPlayer actor(GameTestHelper h,int seq,double x) {
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(),"nature_hi"+UUID.randomUUID().toString().substring(0,5)),false);
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.setNoGravity(true);p.setYRot(0);p.setXRot(0);
        SoulCore.setPathway(p,PATHWAY);SoulCore.setSequence(p,seq);ready(p);initialize();
        h.onEachTick(p::doTick);h.addCleanup(ok->{clear(p);h.getLevel().getServer().getPlayerList().remove(p);});
        int space=SPACES.computeIfAbsent(h,k->++next);
        Vec3 point=h.absoluteVec(new Vec3(1000+space*160+x,140,1.5));p.setPos(point.x,point.y,point.z);
        int cx=p.blockPosition().getX()>>4,cz=p.blockPosition().getZ()>>4;
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++) {
            final int fx=cx+dx,fz=cz+dz;
            if(h.getLevel().setChunkForced(fx,fz,true))h.addCleanup(ok->h.getLevel().setChunkForced(fx,fz,false));
        }
        floor(h,p);
        for(int dx=-8;dx<=8;dx++)for(int dz=-8;dz<=8;dz++) {
            var pos=p.blockPosition().offset(dx,-1,dz);var previous=h.getLevel().getBlockState(pos);
            h.getLevel().setBlockAndUpdate(pos,net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());
            h.addCleanup(ok->h.getLevel().setBlockAndUpdate(pos,previous));
        }
        return p;
    }
    private static void aim(ServerPlayer p,LivingEntity target) {
        Vec3 v=target.getBoundingBox().getCenter().subtract(p.getEyePosition());
        p.setYRot((float)(Math.toDegrees(Math.atan2(v.z,v.x))-90));
        p.setXRot((float)-Math.toDegrees(Math.atan2(v.y,Math.sqrt(v.x*v.x+v.z*v.z))));
    }
    private static void done(GameTestHelper h,ServerPlayer p,String id) {
        ready(p);h.assertTrue(cast(p,id)==AbilityWheel.Result.DONE,id+" casts successfully");
    }
    private static void alchemy(GameTestHelper h) {
        var p=actor(h,4,1.5);
        h.runAfterDelay(3,()->{ // Chunk forcing is promoted on a server tick.
            var t=target(h,p,4,500,EntityTypes.PIG);var s=state(p);aim(p,t);
            done(h,p,"mutate");h.assertTrue(Mutation.level(p,t)==2,"Mutate applies level two");
            ALCHEMY.resetCooldown(p,"mutate");done(h,p,"mutate");h.assertTrue(Mutation.level(p,t)==3,"Mutation caps at three");
            float before=t.getHealth();ALCHEMY.resetCooldown(p,"mutate");done(h,p,"mutate");h.assertTrue(t.getHealth()<before,"Fully mutated target blooms");
            done(h,p,"life_aura");done(h,p,"brood_form");
            h.assertTrue(Form.isIn(p,NatureWalkerAdvanced.BROOD)&&p.getAttributeValue(Attributes.SCALE)>1.5,"Partial brood form changes body");
            var plant=NatureWalkerGrowths.plant(s,NatureWalkerGrowths.Kind.HEARTFRUIT,p.position().add(0,0,-4));
            h.runAfterDelay(25,()->{
                h.assertTrue(p.hasEffect(MobEffects.REGENERATION)&&plant.clock.mature(),"Life aura supports allies and accelerates growth");
                clear(p);h.assertTrue(!Form.isIn(p,NatureWalkerAdvanced.BROOD)&&!Zone.isOpen(p,PREFIX+"life_aura"),"Aura and form clean up");h.succeed();
            });
        });
    }
    private static void mutations(GameTestHelper h) {
        var p=actor(h,6,1.5);
        h.runAfterDelay(3,()->{ // Chunk forcing is promoted on a server tick.
            var q=actor(h,6,15.5);var t=target(h,p,4,60,EntityTypes.PIG);
            Mutation.hit(p,t,Mutation.Kind.VINES,1);Mutation.hit(p,t,Mutation.Kind.VINES,1);
            h.assertTrue(Mutation.level(p,t)==0,"Partial mutagen does not apply a mutation");Mutation.hit(p,t,Mutation.Kind.VINES,1);
            h.assertTrue(Mutation.level(p,t)==1,"Third hit rolls a mutation");Mutation.apply(q,t,Mutation.Kind.FUNGUS,2,240);
            Mutation.clearSource(p);h.assertTrue(Mutation.level(q,t)==2&&Mutation.level(p,t)==0,"Sources are independent");
            Root.applyOwned(t,"test_foreign",80,EnumSet.of(RootRestriction.JUMP));Cleansing.cleanseStatuses(t);
            h.assertTrue(Mutation.level(q,t)==0&&Root.has(t,RootRestriction.JUMP),"Cleanse removes mutations without unrelated roots");Root.removeOwned(t,"test_foreign");
            var clock=new Growth(2,4,2);h.assertTrue(clock.tick(1,2,3)==Growth.Stage.SPROUT,"Shared clock sprouts");
            h.assertTrue(clock.tick(2,2,3)==Growth.Stage.BLOOM&&clock.tick(5,0,3)==Growth.Stage.WITHERED&&clock.tick(7,0,3)==Growth.Stage.DEAD,"Shared clock matures, withers and expires");h.succeed();
        });
    }
    private static void returns(GameTestHelper h) {
        var p=actor(h,3,1.5);
        h.runAfterDelay(3,()->{ // Chunk forcing is promoted on a server tick.
            var corpse=target(h,p,-3,20,EntityTypes.PIG);
            Spells.strikeTrue(p,corpse,100);
            h.runAfterDelay(2,()->{
                done(h,p,"evil_spirits");h.assertTrue(Construct.ofOwner(p,NatureWalkerBrood.SPIRITS).size()==1,"A real fallen body becomes a spirit");
                h.assertTrue(Fallen.near(h.getLevel(),corpse.position(),1,400).isEmpty(),"Spirit consumes its body once");
                var t=target(h,p,5,500);float before=t.getHealth();done(h,p,"return_to_earth");h.assertTrue(t.getHealth()<before,"Return damages undead bosses");
                Burial.release(t);Root.remove(t);t.setPos(p.getX(),p.getY(),p.getZ()+5);p.setXRot(15);done(h,p,"descent_of_death");p.setXRot(0);done(h,p,"maternal_embrace");
                h.assertTrue(Root.has(t,RootRestriction.TELEPORT)&&NoHeal.isApplied(t),"Maternal embrace seals and prevents healing");
                clear(p);h.assertTrue(!Root.has(t,RootRestriction.TELEPORT)&&Construct.ofOwner(p,NatureWalkerBrood.SPIRITS).isEmpty(),"Owner cleanup releases holds and spirits: root="+Root.has(t,RootRestriction.TELEPORT)+", spirits="+Construct.ofOwner(p,NatureWalkerBrood.SPIRITS).size());h.succeed();
            });
        });
    }
    private static void fields(GameTestHelper h) {
        var p=actor(h,2,1.5);
        h.runAfterDelay(3,()->{ // Chunk forcing is promoted on a server tick.
            p.setXRot(20);var t=target(h,p,5,1000,EntityTypes.PIG);
            done(h,p,"blossoming_land");done(h,p,"barren_land");done(h,p,"desolation");
            h.runAfterDelay(45,()->{
                h.assertTrue(p.hasEffect(MobEffects.REGENERATION)&&NoHeal.isApplied(t)&&t.hasEffect(MobEffects.HUNGER),"Land fields apply life and blight");
                h.assertTrue(t.getHealth()<950&&state(p).plants.size()>0,"Desolation damages and blossoming creates growths: health="+t.getHealth()+", plants="+state(p).plants.size());
                clear(p);h.assertTrue(state(p).advanced.fields.isEmpty(),"All field registrations are removed");h.succeed();
            });
        });
    }
    private static void rebirth(GameTestHelper h) {
        var p=actor(h,2,1.5);
        h.runAfterDelay(3,()->{ // Chunk forcing is promoted on a server tick.
            done(h,p,"seed_of_rebirth");
            h.assertTrue(DeathWard.isArmed(p),"Seed arms a ward");
            var enemy=target(h,p,4,500);p.invulnerableTime=0;p.hurtServer(p.level(),enemy.damageSources().mobAttack(enemy),10000);
            h.assertTrue(p.isAlive()&&!DeathWard.isArmed(p)&&Concealment.isHidden(p)&&Root.has(p,RootRestriction.ABILITIES),"Lethal hit consumes seed and creates a cocoon");
            h.runAfterDelay(125,()->{
                h.assertTrue(p.isAlive()&&!Concealment.isHidden(p)&&!Root.has(p,RootRestriction.ABILITIES)&&p.getHealth()>=p.getMaxHealth()*.6f,"Rebirth blooms after 120 ticks");
                ALCHEMY.resetCooldown(p,"seed_of_rebirth");done(h,p,"seed_of_rebirth");
                DeathWard.arm(p,400,.75f,null);Object replacement=DeathWard.snapshot(p);clear(p);
                h.assertTrue(DeathWard.snapshot(p)==replacement,"Cleanup preserves a later ward from another spell");DeathWard.disarm(p);h.succeed();
            });
        });
    }
    private static void realms(GameTestHelper h) {
        var p=actor(h,1,1.5);
        h.runAfterDelay(3,()->{ // Chunk forcing is promoted on a server tick.
            var t=target(h,p,5,1000);done(h,p,"world_of_nature");p.setXRot(15);done(h,p,"verdant_barrier");
            h.assertTrue(Enclosure.isOpen(p,PREFIX+"world_of_nature")&&Wall.isRaised(p,NatureWalkerAdvanced.BARRIER),"Realm and barrier use shared boundaries");
            h.runAfterDelay(45,()->{
                h.assertTrue(t.hasEffect(MobEffects.SLOWNESS)&&state(p).plants.stream().anyMatch(g->g.temporary),"Realm slows foes and creates temporary thorns");
                p.setPos(p.getX()+40,p.getY(),p.getZ());
                h.runAfterDelay(3,()->{
                    h.assertTrue(!Enclosure.isOpen(p,PREFIX+"world_of_nature")&&state(p).plants.stream().noneMatch(g->g.temporary),"Realm collapses when owner leaves and removes its growths");clear(p);h.succeed();
                });
            });
        });
    }
    private static void mother(GameTestHelper h) {
        var p=actor(h,0,1.5);
        h.runAfterDelay(3,()->{ // Chunk forcing is promoted on a server tick.
            var s=state(p);p.setXRot(15);done(h,p,"steel_golem");
            var golem=Construct.ofOwner(p,NatureWalkerBrood.CONSTRUCTS).getFirst();aim(p,golem);done(h,p,"awaken");
            h.assertTrue(golem.getMaxHealth()>1000&&s.companions.stream().anyMatch(c->c.awakened),"Awaken doubles a construct");
            h.assertTrue(Marionette.ticksLeft(golem)>72000&&golem.getEffect(MobEffects.INVISIBILITY).getDuration()>72000,"Awaken extends summon control and the hidden model body");
            Marionette.ageSummon(golem,2400);
            h.assertTrue(golem.isAlive(),"Awakened construct survives its original lifetime");
            var tree=NatureWalkerGrowths.plant(s,NatureWalkerGrowths.Kind.MOTHER_TREE,p.position());
            for(int i=0;i<NatureWalkerGrowths.cap(s)+1;i++)NatureWalkerGrowths.plant(s,NatureWalkerGrowths.Kind.HEARTFRUIT,p.position().add(0,0,-4));
            h.assertTrue(s.plants.contains(tree),"Mother tree survives garden replacement by its fruit");
            var t=target(h,p,3,2000);aim(p,t);done(h,p,"embrace_of_the_earth");h.assertTrue(t.getHealth()<2000&&Corrosion.isCorroding(t),"Earth embrace deals true damage and corrosion");
            done(h,p,"earthquake");h.assertTrue(Weight.isHeavy(t)&&Falter.chance(t)>=.5f,"Earthquake destabilizes enemies");
            done(h,p,"great_harvest");done(h,p,"bountiful_earth");
            h.assertTrue(DeathWard.isArmed(p)&&Dome.isRaised(p,PREFIX+"bountiful_earth"),"Kingdom arms a protective ward and realm");
            var low=target(h,p,7,80,EntityTypes.PIG);low.setHealth(10);HARVEST.resetCooldown(p,"great_harvest");done(h,p,"great_harvest");
            h.assertTrue(!low.isAlive(),"Great Harvest executes low-health nonboss mobs");
            clear(p);h.assertTrue(!DeathWard.isArmed(p)&&golem.isRemoved(),"Kingdom and awakened construct leave with owner");h.succeed();
        });
    }
    private static void upgrades(GameTestHelper h) {
        var p=actor(h,1,1.5);
        h.runAfterDelay(3,()->{ // Chunk forcing is promoted on a server tick.
            var s=state(p);var a=NatureWalkerGrowths.plant(s,NatureWalkerGrowths.Kind.THORNLING,p.position());
            var b=NatureWalkerGrowths.plant(s,NatureWalkerGrowths.Kind.THORNLING,p.position().add(0,0,20));
            float before=Spells.spiritualityOf(p);h.assertTrue(cast(p,"underground_travel")==AbilityWheel.Result.DONE,"Nature Walk casts");
            h.assertTrue(p.position().distanceTo(b.at)<1&&Spells.spiritualityOf(p)==before-6000,"Nature Walk uses growth network and upgraded cost");
            var mob=target(h,p,4,40);BeyonderMobs.make(mob,PATHWAY,1);
            var ability=MobAbilities.all().stream().filter(x->x.pathway().equals(PATHWAY)&&x.entry().equals("underground_travel")).findFirst().orElseThrow();
            h.assertTrue(ability.cost(mob)==6000&&ability.copy().cost(mob)==6000,"Dynamic mob cost survives encounter copies");mob.discard();
            clear(p);SoulCore.setSequence(p,2);ready(p);s=state(p);
            NatureWalkerGrowths.plant(s,NatureWalkerGrowths.Kind.THORNLING,p.position().add(0,0,-3));
            NatureWalkerGrowths.plant(s,NatureWalkerGrowths.Kind.HEARTFRUIT,p.position().add(0,0,-7));
            var beast=target(h,p,5,200,EntityTypes.PIG);aim(p,beast);
            h.assertTrue(NatureWalkerGrowths.near(s,p.position(),10).size()==2,"Two valid growths at the network destination: "+s.plants.size()+", at="+p.position());
            h.assertTrue(aimed(s,16,false)==beast,"The intended hybrid is under the crosshair");
            done(h,p,"cross_breed");
            h.assertTrue(state(p).companions.stream().anyMatch(c->c.mob==beast&&c.extra!=null),"Higher Cross-breed consumes two growths for two traits");
            done(h,p,"brood_form");h.assertTrue(p.getAttributeValue(Attributes.SCALE)>=3,"Complete brood form triples size");clear(p);h.succeed();
        });
    }
    private static void lifecycle(GameTestHelper h) {
        var p=actor(h,2,1.5);
        h.runAfterDelay(3,()->{ // Chunk forcing is promoted on a server tick.
            var t=target(h,p,5,1000,EntityTypes.PIG);p.setXRot(15);done(h,p,"root_eruption");float before=t.getHealth();
            h.runAfterDelay(45,()->{
                h.assertTrue(t.getHealth()<before&&Root.has(t,RootRestriction.MOVE),"Eruption waits then bursts and roots");
                Root.remove(t);Decay.cure(t);t.setHealth(t.getMaxHealth());t.invulnerableTime=0;
                RETURN.resetCooldown(p,"root_eruption");done(h,p,"root_eruption");SoulCore.setPathway(p,"Author");float health=t.getHealth();
                h.runAfterDelay(45,()->{h.assertTrue(t.getHealth()==health&&!STATES.containsKey(p.getUUID()),"Pathway change cancels delayed eruption");h.succeed();});
            });
        });
    }
}
