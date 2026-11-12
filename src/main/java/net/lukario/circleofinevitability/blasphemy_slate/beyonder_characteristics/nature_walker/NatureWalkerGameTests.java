package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker;

import java.util.*;
import java.util.function.Consumer;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.nature_walker.NatureWalker.*;

/** Dedicated-server regressions use real players, live ticks and normal wheel payment. */
public final class NatureWalkerGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS=
            DeferredRegister.create(Registries.TEST_FUNCTION,"circle_of_inevitability");
    static {
        FUNCTIONS.register("nature_walker_costs",()->NatureWalkerGameTests::costs);
        FUNCTIONS.register("nature_walker_growths",()->NatureWalkerGameTests::growths);
        FUNCTIONS.register("nature_walker_support",()->NatureWalkerGameTests::support);
        FUNCTIONS.register("nature_walker_harvest",()->NatureWalkerGameTests::harvest);
        FUNCTIONS.register("nature_walker_hybrids",()->NatureWalkerGameTests::hybrids);
        FUNCTIONS.register("nature_walker_earth",()->NatureWalkerGameTests::earth);
        FUNCTIONS.register("nature_walker_travel",()->NatureWalkerGameTests::travel);
        FUNCTIONS.register("nature_walker_constructs",()->NatureWalkerGameTests::constructs);
        FUNCTIONS.register("nature_walker_cleanup",()->NatureWalkerGameTests::cleanup);
        FUNCTIONS.register("nature_walker_mobs",()->NatureWalkerGameTests::mobs);
    }

    private static final Map<GameTestHelper, Integer> HEIGHTS = new WeakHashMap<>();
    private static int nextHeight;

    private static ServerPlayer player(GameTestHelper h, int sequence, double x) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "nature" + UUID.randomUUID().toString().substring(0, 6)), false);
        var p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); p.setNoGravity(true);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        Vec3 at = h.absoluteVec(new Vec3(x, HEIGHTS.computeIfAbsent(h, ignored -> 12 + 24 * (nextHeight++ % 10)), 1.5)); p.setPos(at.x, at.y, at.z); p.setYRot(0); p.setXRot(0);
        SoulCore.setPathway(p, NatureWalker.PATHWAY); SoulCore.setSequence(p, sequence);
        SoulCore.setSpiritualityQuietly(p, Beyonder.maxSpirituality(p));
        NatureWalker.initialize();
        // Embedded connections are not polled by ServerConnectionListener. Drive the same player tick
        // that a real connection performs, so Forge PlayerTick events exercise production handlers.
        h.onEachTick(p::doTick);
        h.addCleanup(ok -> { NatureWalker.clear(p); h.getLevel().getServer().getPlayerList().remove(p); });
        return p;
    }

    private static Mob target(GameTestHelper h, ServerPlayer p, double z, float health) {
        return target(h,p,z,health,EntityTypes.HUSK);
    }
    private static Mob target(GameTestHelper h,ServerPlayer p,double z,float health,EntityType<? extends Mob> type) {
        Mob mob = type.create(h.getLevel(), EntitySpawnReason.MOB_SUMMONED);
        Vec3 at = p.position().add(0, 0, z); mob.setPos(at.x, at.y, at.z);
        mob.setNoAi(true); mob.setNoGravity(true);
        mob.getPersistentData().putBoolean("circle_of_inevitability_beyonder_rolled", true);
        mob.getAttribute(Attributes.ARMOR).setBaseValue(0);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health); mob.setHealth(health);
        h.getLevel().addFreshEntity(mob); h.addCleanup(ok -> mob.discard()); return mob;
    }

    private static AbilityWheel.Result cast(ServerPlayer p, String id) {
        for (var w : NatureWalker.WHEELS)
            if (w.entry(id) != null) return w.cast(p, w.entry(id));
        throw new IllegalArgumentException(id);
    }

    private static void floor(GameTestHelper h, ServerPlayer p) {
        for (int x = -4; x <= 4; x++) for (int z = -7; z <= 28; z++) {
            var pos = net.minecraft.core.BlockPos.containing(p.position().add(x, -1, z));
            var before = h.getLevel().getBlockState(pos);
            h.getLevel().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState());
            h.addCleanup(ok -> h.getLevel().setBlockAndUpdate(pos, before));
        }
    }

    private static void ready(ServerPlayer p) { SoulCore.setSpiritualityQuietly(p,Beyonder.maxSpirituality(p)); }
    private static void costs(GameTestHelper h) {
        var p=player(h,9,1.5);floor(h,p);p.setXRot(45);
        h.assertTrue(SPELLS.size()==20,"Exactly twenty playable entries");
        h.assertTrue(cast(p,"treatment")==AbilityWheel.Result.REFUSED,"Sequence gate rejects locked casts");
        float before=Spells.spiritualityOf(p);
        h.assertTrue(cast(p,"heartfruit_seed")==AbilityWheel.Result.DONE,"Starting spirituality can buy a seed");
        h.assertTrue(Spells.spiritualityOf(p)==before-5,"One successful seed payment");
        h.assertTrue(cast(p,"heartfruit_seed")==AbilityWheel.Result.REFUSED&&Spells.spiritualityOf(p)==before-5,"Cooldown does not charge");
        h.assertTrue(cast(p,"thorn_seed")==AbilityWheel.Result.DONE,"Projectile seed casts");
        h.runAfterDelay(15,()->{
            h.assertTrue(state(p).plants.stream().anyMatch(g->g.kind==NatureWalkerGrowths.Kind.BRIAR_ORCHARD),"Projectile plants and cross-pollinates");h.succeed();
        });
    }
    private static void growths(GameTestHelper h) {
        var p=player(h,9,1.5);floor(h,p);var q=player(h,9,20.5);floor(h,q);
        var a=state(p);var b=state(q);p.setHealth(p.getMaxHealth()*.4f);float before=p.getHealth();
        NatureWalkerGrowths.plant(a,NatureWalkerGrowths.Kind.HEARTFRUIT,p.position());
        NatureWalkerGrowths.plant(b,NatureWalkerGrowths.Kind.THORNLING,q.position());
        var t=target(h,p,5,80);NatureWalkerGrowths.plant(a,NatureWalkerGrowths.Kind.THORNLING,t.position());
        h.runAfterDelay(100,()->{
            h.assertTrue(p.getHealth()>before,"Owner can eat its own ripe fruit");
            h.assertTrue(a.plants.stream().anyMatch(g->g.eaten),"Fruit is consumed once");
            h.assertTrue(t.getHealth()<80&&Bleeding.isBleeding(t),"Mature thorns harm and bleed enemies");
            for(int i=0;i<5;i++)NatureWalkerGrowths.plant(a,NatureWalkerGrowths.Kind.THORNLING,p.position().add(0,0,10+i));
            h.assertTrue(a.plants.size()==3&&b.plants.size()==1,"Owner caps are independent");
            NatureWalker.clear(p);h.assertTrue(a.plants.isEmpty()&&b.plants.size()==1,"Clearing one garden preserves the other");h.succeed();
        });
    }
    private static void support(GameTestHelper h) {
        var p=player(h,8,1.5);p.setHealth(p.getMaxHealth()*.2f);
        h.assertTrue(cast(p,"treatment")==AbilityWheel.Result.DONE,"Treatment heals self without ally target");
        h.assertTrue(p.getHealth()>p.getMaxHealth()*.8f&&p.hasEffect(MobEffects.RESISTANCE),"Critical healing adds protection");
        NoHeal.apply(p,100);p.setHealth(p.getMaxHealth()*.5f);float blocked=p.getHealth();
        Healing.apply(p,20);h.assertTrue(p.getHealth()==blocked,"Ordinary healing respects NoHeal");
        Madness.add(p,p,4);ready(p);
        h.assertTrue(cast(p,"soul_suture")==AbilityWheel.Result.DONE&&!NoHeal.isApplied(p)&&Madness.get(p)==0,"Suture removes wounds and mind effects");
        var q=player(h,4,15.5);NoHeal.apply(q,100);q.setHealth(q.getMaxHealth()*.5f);
        h.assertTrue(cast(q,"treatment")==AbilityWheel.Result.DONE&&!NoHeal.isApplied(q)&&q.getHealth()>q.getMaxHealth()*.9f,"Upgraded Treatment lifts NoHeal");
        var t=target(h,q,4,40);Spells.keepEffect(t,MobEffects.STRENGTH,0,100);ready(q);
        h.assertTrue(cast(q,"soul_suture")==AbilityWheel.Result.DONE&&Silence.isSilenced(t)&&!t.hasEffect(MobEffects.STRENGTH),"Enemy Suture silences and dispels");h.succeed();
    }
    private static void harvest(GameTestHelper h) {
        var p=player(h,7,1.5);floor(h,p);p.setXRot(45);var s=state(p);
        h.assertTrue(cast(p,"seed_catalyzation")==AbilityWheel.Result.DONE&&s.plants.size()==1,"Catalyzation creates a growth on an empty board");
        h.assertTrue(s.plants.getFirst().age>=60,"Catalyzation ripens immediately");ready(p);
        h.assertTrue(cast(p,"quicken_life")==AbilityWheel.Result.DONE&&Root.has(p,RootRestriction.MOVE),"Quicken channels");
        Root.applyOwned(p,"test_foreign_root",100,EnumSet.of(RootRestriction.JUMP));
        SoulCore.setSpiritualityQuietly(p,0);
        h.assertTrue(cast(p,"quicken_life")==AbilityWheel.Result.FREE&&!Root.has(p,RootRestriction.MOVE)&&Spells.spiritualityOf(p)==0,"Cancel is free at zero spirituality through cooldown");
        h.assertTrue(Root.has(p,RootRestriction.JUMP),"Cancelling preserves unrelated roots");Root.removeOwned(p,"test_foreign_root");ready(p);
        boolean weather=p.level().getWeatherData().isRaining();
        h.assertTrue(cast(p,"weather_ritual")==AbilityWheel.Result.DONE&&s.weatherUntil>p.level().getGameTime(),"Local weather starts");
        h.assertTrue(p.level().getWeatherData().isRaining()==weather,"Local ritual leaves global weather untouched");ready(p);
        p.setXRot(0);var t=target(h,p,3,80);
        h.assertTrue(cast(p,"reap")==AbilityWheel.Result.DONE&&t.getHealth()<80&&s.reaped.containsKey(t.getUUID()),"Reap damages and marks targets");
        h.assertTrue(s.plants.isEmpty(),"Reap harvests the forward growth");h.succeed();
    }
    private static void hybrids(GameTestHelper h) {
        var p=player(h,6,1.5);floor(h,p);var s=state(p);var t=target(h,p,4,40);
        float before=Spells.spiritualityOf(p);
        h.assertTrue(cast(p,"cross_breed")==AbilityWheel.Result.REFUSED&&Spells.spiritualityOf(p)==before,"Hybrid without growth refuses without charge");
        NatureWalkerGrowths.plant(s,NatureWalkerGrowths.Kind.THORNLING,p.position().add(0,0,-3));
        h.assertTrue(cast(p,"cross_breed")==AbilityWheel.Result.DONE&&Marionette.isMarionetteOf(t,p),"Hybrid takes ownership");
        h.assertTrue(t.getMaxHealth()>60&&s.plants.isEmpty(),"Hybrid gains health and consumes exactly one growth");
        var ordinary=target(h,p,8,40);Marionette.control(ordinary,p,200);
        Mob spider=EntityTypes.SPIDER.create(h.getLevel(),EntitySpawnReason.MOB_SUMMONED);
        spider.setPos(p.getX()+2,p.getY(),p.getZ()+5);spider.setNoAi(true);spider.setNoGravity(true);
        spider.getPersistentData().putBoolean("circle_of_inevitability_beyonder_rolled",true);h.getLevel().addFreshEntity(spider);h.addCleanup(ok->spider.discard());ready(p);
        h.assertTrue(cast(p,"command_flora")==AbilityWheel.Result.DONE&&Marionette.isMarionetteOf(spider,p),"Flora command controls arthropods");
        h.assertTrue(Marionette.isMarionetteOf(t,p)&&Marionette.isMarionetteOf(ordinary,p),"Flora, hybrid and ordinary control caps are separate");
        NatureWalker.clear(p);h.assertTrue(!Marionette.isMarionette(t)&&t.getMaxHealth()==40&&!Marionette.isMarionette(spider)&&Marionette.isMarionetteOf(ordinary,p),"Cleanup restores hybrids without touching unrelated controls");
        Marionette.release(ordinary);h.succeed();
    }
    private static void earth(GameTestHelper h) {
        var p=player(h,5,1.5);floor(h,p);p.setXRot(25);var t=target(h,p,4,500);
        h.assertTrue(cast(p,"swamp")==AbilityWheel.Result.DONE,"Swamp starts");ready(p);p.setXRot(0);
        h.assertTrue(cast(p,"wrath_of_nature")==AbilityWheel.Result.DONE,"Wrath marks enemies in sight");ready(p);
        h.assertTrue(cast(p,"child_of_the_oak")==AbilityWheel.Result.DONE&&ConjuredArmor.isWorn(p,NatureWalkerEarth.OAK),"Oak equips armor");
        h.runAfterDelay(90,()->{
            h.assertTrue(t.getHealth()<480,"Terrain hazards deal repeated damage");
            h.assertTrue(t.hasEffect(MobEffects.SLOWNESS)||Burial.isBuried(t),"Mire progresses to slow or burial");
            h.assertTrue(p.getAbsorptionAmount()>0,"Wood armor supplies absorption");
            h.getLevel().setBlockAndUpdate(p.blockPosition().below(),Blocks.STONE.defaultBlockState());
            h.runAfterDelay(10,()->{h.assertTrue(Boolean.TRUE.equals(state(p).oakStone)&&p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)>=1,"Stone ground changes bark protection");h.succeed();});
        });
    }
    private static void travel(GameTestHelper h) {
        var p=player(h,5,1.5);floor(h,p);Vec3 origin=p.position();
        h.assertTrue(cast(p,"underground_travel")==AbilityWheel.Result.DONE&&Concealment.isHidden(p),"Travel starts concealed");
        h.runAfterDelay(45,()->{
            h.assertTrue(!travelling(p)&&!Concealment.isHidden(p)&&p.position().distanceTo(origin)>20,"Travel surfaces after ten safe hops");
            h.assertTrue(FallGuard.isProtected(p)&&!Root.has(p,RootRestriction.MOVE),"Travel restores movement and guards falls");
            ready(p);p.setYRot(180);EARTH.resetCooldown(p,"underground_travel");
            h.assertTrue(cast(p,"underground_travel")==AbilityWheel.Result.DONE,"Travel can restart");
            SoulCore.setSpiritualityQuietly(p,0);
            h.assertTrue(cast(p,"underground_travel")==AbilityWheel.Result.FREE&&!travelling(p),"Early surfacing costs nothing");
            ready(p);h.assertTrue(cast(p,"bear_form")==AbilityWheel.Result.DONE&&NatureWalkerEarth.isBear(p),"Bear form starts");
            h.assertTrue(p.getAttributeValue(Attributes.SCALE)>=2&&p.getAttributeValue(Attributes.ATTACK_DAMAGE)>=10,"Bear grows and gains claws");
            NatureWalker.clear(p);h.assertTrue(!NatureWalkerEarth.isBear(p)&&p.getAttributeValue(Attributes.SCALE)==1,"Cleanup restores form attributes");
            ready(p);EARTH.resetCooldown(p,"underground_travel");
            Root.applyOwned(p,"test_no_teleport",60,EnumSet.of(RootRestriction.TELEPORT));
            float before=Spells.spiritualityOf(p);
            h.assertTrue(cast(p,"underground_travel")==AbilityWheel.Result.REFUSED&&Spells.spiritualityOf(p)==before,"Teleport restrictions refuse without payment");
            Root.removeOwned(p,"test_no_teleport");
            Vec3 stoppedAt=p.position();BlockPos wall=BlockPos.containing(stoppedAt.add(0,0,-2.4));
            for(int y=0;y<5;y++) { var pos=wall.above(y);var old=h.getLevel().getBlockState(pos);
                h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());h.addCleanup(ok->h.getLevel().setBlockAndUpdate(pos,old)); }
            h.assertTrue(cast(p,"underground_travel")==AbilityWheel.Result.DONE,"Travel can approach an obstructed destination");
            h.runAfterDelay(23,()->{
                h.assertTrue(!travelling(p)&&p.position().distanceTo(stoppedAt)<.1&&!Concealment.isHidden(p),"Blocked destination surfaces at last safe point");h.succeed();
            });
        });
    }
    private static void constructs(GameTestHelper h) {
        var p=player(h,4,1.5);floor(h,p);p.setXRot(15);var s=state(p);
        h.assertTrue(cast(p,"stone_golem")==AbilityWheel.Result.DONE,"Stone golem summons");
        var oldest=Construct.ofOwner(p,NatureWalkerBrood.CONSTRUCTS).getFirst();ready(p);p.setXRot(30);
        h.assertTrue(cast(p,"stone_bird")==AbilityWheel.Result.DONE,"Stone bird summons");ready(p);p.setXRot(45);
        h.assertTrue(cast(p,"mud_golem")==AbilityWheel.Result.DONE,"Mud golem summons");ready(p);p.setYRot(180);p.setXRot(20);
        h.assertTrue(cast(p,"steel_golem")==AbilityWheel.Result.DONE,"Steel golem summons");
        var owned=Construct.ofOwner(p,NatureWalkerBrood.CONSTRUCTS);
        h.assertTrue(owned.size()==2&&oldest.isRemoved(),"Construct cap replaces the oldest across kinds");
        h.assertTrue(owned.stream().anyMatch(m->m.getMaxHealth()>450),"Verdancy scales steel health");
        p.setYRot(0);p.setXRot(0);var t=target(h,p,4,80,EntityTypes.PIG);
        var behind=target(h,p,-4,80,EntityTypes.PIG);var side=target(h,p,0,80,EntityTypes.PIG);
        side.setPos(p.getX()+4,p.getY(),p.getZ());var ally=target(h,p,5,80,EntityTypes.PIG);
        Friends.add(p,ally.getUUID(),"ally",false);ready(p);
        h.assertTrue(cast(p,"toxin_bloom")==AbilityWheel.Result.DONE&&t.hasEffect(MobEffects.POISON)&&t.hasEffect(MobEffects.WEAKNESS),"Toxin applies both effects");
        h.assertTrue(!behind.hasEffect(MobEffects.POISON)&&!side.hasEffect(MobEffects.POISON)&&!ally.hasEffect(MobEffects.POISON),"Toxin respects the cone and friendships");
        NatureWalker.clear(p);h.assertTrue(owned.stream().allMatch(Entity::isRemoved),"Constructs leave with their owner");h.succeed();
    }
    private static void cleanup(GameTestHelper h) {
        var p=player(h,5,1.5);floor(h,p);p.setXRot(10);var old=state(p);
        h.assertTrue(cast(p,"thorn_seed")==AbilityWheel.Result.DONE,"Seed is in flight");ready(p);
        h.assertTrue(cast(p,"quicken_life")==AbilityWheel.Result.DONE,"Channel is active");
        SoulCore.setPathway(p,"Author");
        h.runAfterDelay(30,()->{
            h.assertTrue(!STATES.containsKey(p.getUUID())&&old.plants.isEmpty(),"Pathway change clears state and cancels delayed planting");
            h.assertTrue(!Root.has(p,RootRestriction.MOVE),"Pathway change releases only the channel root");h.succeed();
        });
    }
    private static void mobs(GameTestHelper h) {
        var p=player(h,5,1.5);floor(h,p);var mob=target(h,p,4,40);
        h.assertTrue(BeyonderMobs.make(mob,PATHWAY,5),"Mobs can take the pathway");
        long registered=MobAbilities.all().stream().filter(a->PATHWAY.equals(a.pathway())).count();
        h.assertTrue(registered==20,"Every active entry has mob casting metadata");
        h.assertTrue(perform(mob,"child_of_the_oak")==AbilityWheel.Result.DONE&&ConjuredArmor.isWorn(mob,NatureWalkerEarth.OAK),"Mob casts shared armor implementation");
        h.assertTrue(perform(mob,"bear_form")==AbilityWheel.Result.DONE&&MobForm.isIn(mob,NatureWalkerEarth.BEAR),"Mob casts shared bear form");
        NatureWalker.clear(mob);h.assertTrue(!MobForm.isIn(mob,NatureWalkerEarth.BEAR),"Mob form cleans up");h.succeed();
    }
}
