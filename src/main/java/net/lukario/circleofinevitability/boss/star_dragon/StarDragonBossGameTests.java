package net.lukario.circleofinevitability.boss.star_dragon;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.*;
import net.lukario.circleofinevitability.root.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Consumer;

/** Real-server encounter regressions. No client classes are referenced by this package. */
public final class StarDragonBossGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS=DeferredRegister.create(Registries.TEST_FUNCTION,CircleOfInevitability.MOD_ID);
    static {
        FUNCTIONS.register("star_dragon_boss_identity",() -> StarDragonBossGameTests::identity);
        FUNCTIONS.register("star_dragon_boss_windup",() -> StarDragonBossGameTests::windup);
        FUNCTIONS.register("star_dragon_boss_interrupt",() -> StarDragonBossGameTests::interrupt);
        FUNCTIONS.register("star_dragon_boss_controls",() -> StarDragonBossGameTests::controls);
        FUNCTIONS.register("star_dragon_boss_characteristic",() -> StarDragonBossGameTests::characteristic);
        FUNCTIONS.register("star_dragon_boss_cost",() -> StarDragonBossGameTests::cost);
        FUNCTIONS.register("star_dragon_boss_saved_state",() -> StarDragonBossGameTests::savedState);
    }
    private static StarDragonBoss boss(GameTestHelper h) {
        StarDragonBoss b=h.spawnWithNoFreeWill(StarDragonBossRegistry.BOSS.get(),1,2,1);
        b.tick();b.mode("manual");b.setNoAi(false);b.setNoGravity(true);
        h.addCleanup(ok -> b.discard());return b;
    }
    private static Mob target(GameTestHelper h,StarDragonBoss b) {
        Mob t=h.spawnWithNoFreeWill(EntityTypes.PIG,12,2,1);t.setNoGravity(true);t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);t.setHealth(1000);
        b.target(t);h.addCleanup(ok -> t.discard());return t;
    }
    private static void identity(GameTestHelper h) {
        var b=boss(h);
        h.assertTrue("Star Dragon".equals(BeyonderMobs.pathway(b)) && BeyonderMobs.sequence(b)==2,"Always Star Dragon Sequence 2");
        h.assertTrue(MobAbilities.knownBy(b).size()==20,"All twenty abilities through Sequence 2, including Remote Sight");
        MobAbility forbidden=MobAbilities.byId("Star Dragon/7/neutron_star");
        h.assertTrue(MobCasting.forceCast(b,forbidden,b,h.getLevel())==MobCasting.Refusal.SEQUENCE_TOO_WEAK,"Cannot force Sequence 0 ability");
        h.assertTrue(MobAbilities.knownBy(b).stream().noneMatch(a -> a.sequence()<2),"No Sequence 1/0 powers");
        float hp=b.getHealth()-17;b.setHealth(hp);b.tick();h.assertTrue(b.getHealth()<=hp,"Repeated ticks do not refill health");h.succeed();
    }
    private static void windup(GameTestHelper h) {
        var b=boss(h);var t=target(h,b);var a=MobAbilities.byId("Star Dragon/1");
        float before=MobSpirituality.get(b);int windup=StarDragonBossSettings.get().attack(a).windupTicks();
        h.assertTrue(b.queueAttack(a,t),"Chart queued");h.assertTrue(MobSpirituality.get(b)==before,"Windup spends nothing");
        h.assertTrue(MobCasting.cooldownLeft(b,a)==0,"Windup starts no cooldown");
        h.runAfterDelay(windup+2,() -> {
            h.assertTrue(MobCasting.cooldownLeft(b,a)>0,"Released cast has cooldown");
            h.assertTrue(MobSpirituality.get(b)<before,"Released cast paid spirituality");
            h.assertTrue(!b.queueAttack(a,t),"Recovery/cooldown prevent repeated cast");h.succeed();
        });
    }
    private static void interrupt(GameTestHelper h) {
        var b=boss(h);var t=target(h,b);var a=MobAbilities.byId("Star Dragon/1");float before=MobSpirituality.get(b);
        h.assertTrue(b.queueAttack(a,t),"Chart queued");Root.apply(b,80,RootRestriction.ABILITIES);
        h.runAfterDelay(30,() -> {
            h.assertTrue(MobCasting.cooldownLeft(b,a)==0,"Interrupted windup starts no cooldown");
            h.assertTrue(MobSpirituality.get(b)==before,"Interrupted windup spends nothing");h.succeed();
        });
    }
    private static void controls(GameTestHelper h) {
        var b=boss(h);var t=target(h,b);var a=MobAbilities.byId("Star Dragon/1");
        b.enable(a.id(),false);h.assertTrue(!b.queueAttack(a,t),"Disabled attack cannot be queued");
        b.resetAttacks();b.mode("passive");h.assertTrue(!b.queueAttack(a,t),"Passive mode refuses casts");
        b.mode("manual");h.assertTrue(b.queueAttack(a,t),"Manual mode allows directed cast");b.mode("passive");
        h.runAfterDelay(30,() -> {h.assertTrue(MobCasting.cooldownLeft(b,a)==0,"Mode change cancels pending attack");h.succeed();});
    }
    private static void characteristic(GameTestHelper h) {
        var b=boss(h);b.mode("passive");
        b.hurtServer(h.getLevel(),b.damageSources().genericKill(),Float.MAX_VALUE);
        h.runAfterDelay(2,() -> {
            long drops=h.getLevel().getEntitiesOfClass(ItemEntity.class,b.getBoundingBox().inflate(8)).stream()
                    .filter(i -> i.getItem().getItem() instanceof net.lukario.circleofinevitability.item.BeyonderCharacteristicItem).count();
            h.assertTrue(drops==1,"Exactly one characteristic dropped");h.succeed();
        });
    }
    private static void savedState(GameTestHelper h) {
        var b=boss(h);var t=target(h,b);var a=MobAbilities.byId("Star Dragon/1");
        // Start actual hovering, then serialize: temporary no-gravity must not survive the lost form on reload.
        b.setNoGravity(false);b.mode("auto");
        h.assertTrue(MobCasting.forceCast(b,MobAbilities.byId("Star Dragon/7/radiant_angel_form"),t,h.getLevel())==MobCasting.Refusal.NONE,"Angel form cast");
        b.tick();h.assertTrue(b.isNoGravity(),"Radiant boss hovers");
        b.enable(a.id(),false);b.weight(a.id(),37);b.setHealth(123);MobSpirituality.set(b,4567);
        var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess());
        h.assertTrue(b.save(output),"Boss saved");
        StarDragonBoss restored=StarDragonBossRegistry.BOSS.get().create(h.getLevel(),EntitySpawnReason.COMMAND);
        restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,h.getLevel().registryAccess(),output.buildResult()));
        restored.setUUID(java.util.UUID.randomUUID());restored.setNoAi(true);restored.tick();
        h.assertTrue(!restored.isNoGravity(),"Temporary hovering restores original gravity on load");
        h.assertTrue(restored.getHealth()==123 && MobSpirituality.get(restored)==4567,"Reload never refills health or spirituality");
        h.assertTrue(!restored.attackEnabled(a) && restored.attackWeight(a)==37 && restored.mode().equals("auto"),"Overrides and mode survive save/load");
        restored.discard();h.succeed();
    }
    private static void cost(GameTestHelper h) {
        var b=boss(h);var t=target(h,b);var original=MobAbilities.byId("Star Dragon/2/cosmic_void");
        var tuned=original.copy().cost(1);float before=MobSpirituality.get(b);
        h.assertTrue(MobCasting.tryCast(b,tuned,t,h.getLevel(),5),"Tuned spell cast");
        h.assertTrue(MobSpirituality.get(b)==before-1,"Tuned cost charged exactly once");
        h.assertTrue(original.cost()!=1,"Encounter tuning leaves registry intact");h.succeed();
    }
}
