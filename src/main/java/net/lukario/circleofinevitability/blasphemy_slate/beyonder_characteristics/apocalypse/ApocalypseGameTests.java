package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.apocalypse;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.Friends;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.EmpoweredHit;
import net.lukario.circleofinevitability.status.NoHeal;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Consumer;

/** Real-server regressions for the replacement passive, including paid casts and actual melee contact. */
public final class ApocalypseGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, CircleOfInevitability.MOD_ID);
    static {
        FUNCTIONS.register("apocalypse_no_cast_counter", () -> ApocalypseGameTests::noCastCounter);
        FUNCTIONS.register("apocalypse_weaving", () -> ApocalypseGameTests::weaving);
        FUNCTIONS.register("apocalypse_expiry", () -> ApocalypseGameTests::expiry);
        FUNCTIONS.register("apocalypse_harvest", () -> ApocalypseGameTests::harvest);
        FUNCTIONS.register("apocalypse_ambush", () -> ApocalypseGameTests::ambush);
        FUNCTIONS.register("apocalypse_mirror_fuel", () -> ApocalypseGameTests::fuel);
        FUNCTIONS.register("apocalypse_refused_cast", () -> ApocalypseGameTests::refused);
        FUNCTIONS.register("apocalypse_contacts", () -> ApocalypseGameTests::contacts);
        FUNCTIONS.register("apocalypse_cleanup", () -> ApocalypseGameTests::cleanup);
        FUNCTIONS.register("apocalypse_allies", () -> ApocalypseGameTests::allies);
    }
    private ApocalypseGameTests() {}

    private static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        Vec3 at = h.absoluteVec(new Vec3(1.5, 1, 1));
        p.setPos(at.x, at.y, at.z);
        p.setNoGravity(true);
        SoulCore.setPathway(p, Apocalypse.PATHWAY);
        SoulCore.setSequence(p, 0);
        SoulCore.setSpiritualityQuietly(p, 1_000_000);
        h.addCleanup(ok -> {
            ApocalypseMechanics.clear(p);
            Apocalypse.SHARDS.removeIf(shard -> shard.owner.equals(p.getUUID()));
            p.discard();
        });
        return p;
    }
    private static Mob enemy(GameTestHelper h, ServerPlayer p) {
        Mob mob = h.spawn(EntityTypes.HUSK, new BlockPos(1, 1, 2));
        mob.setNoAi(true);
        mob.setNoGravity(true);
        mob.setPos(p.getX(), p.getY(), p.getZ() + 1.2);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        mob.setHealth(1000);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, mob.getEyePosition());
        h.addCleanup(ok -> { ApocalypseMechanics.clear(mob); mob.discard(); });
        return mob;
    }
    private static Apocalypse.Shard shard(GameTestHelper h, ServerPlayer p, Vec3 at, float charge) {
        var shard = new Apocalypse.Shard(h.getLevel(), p.getUUID(), at, 0,
                "apocalypse_test_" + java.util.UUID.randomUUID(), h.getLevel().getGameTime() + 1000);
        shard.charge = charge;
        Apocalypse.SHARDS.add(shard);
        return shard;
    }
    private static void weave(GameTestHelper h, ServerPlayer p, Mob t) {
        for (var kind : ApocalypseMechanics.Affliction.values()) ApocalypseMechanics.afflict(p, h.getLevel(), t, kind);
    }
    private static void equal(GameTestHelper h, double actual, double expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < 0.01, message + ": " + actual + " != " + expected);
    }
    private static void noCastCounter(GameTestHelper h) {
        var p = player(h); var t = enemy(h, p);
        // Sequence 4 can pay for the casts, but has no catastrophe unlocked (the old passive burst here).
        SoulCore.setSequence(p, 4);
        p.setShiftKeyDown(true);
        for (int i = 0; i < 25; i++) Apocalypse.apocalypseFatalStep(p, h.getLevel(), h.getLevel(), false);
        p.setShiftKeyDown(false);
        equal(h, t.getHealth(), 1000, "Twenty-five casts do not trigger an automatic catastrophe or burst");
        h.assertTrue(!p.getPersistentData().contains("apocalypse_stacks"), "Casting does not rebuild the removed counter");
        equal(h, Apocalypse.shardsOf(p, h.getLevel()).size(), 3, "Normal mirror limit remains intact");
        h.succeed();
    }
    private static void weaving(GameTestHelper h) {
        var p = player(h); var other = player(h); var t = enemy(h, p);
        for (int i = 0; i < 30; i++) ApocalypseMechanics.afflict(p, h.getLevel(), t, ApocalypseMechanics.Affliction.FLAME);
        ApocalypseMechanics.afflict(p, h.getLevel(), t, ApocalypseMechanics.Affliction.SILK);
        ApocalypseMechanics.afflict(other, h.getLevel(), t, ApocalypseMechanics.Affliction.PLAGUE);
        h.assertTrue(!ApocalypseMechanics.hasDespair(p, t), "Duplicates and another caster's seals cannot complete Despair");
        ApocalypseMechanics.afflict(p, h.getLevel(), t, ApocalypseMechanics.Affliction.PLAGUE);
        h.assertTrue(ApocalypseMechanics.hasDespair(p, t) && NoHeal.isApplied(t), "Three families weave Despair and block healing");
        equal(h, ApocalypseMechanics.harvest(other, h.getLevel(), t), 0, "Another caster cannot steal the harvest");
        h.succeed();
    }
    private static void expiry(GameTestHelper h) {
        var p = player(h); var t = enemy(h, p);
        ApocalypseMechanics.afflict(p, h.getLevel(), t, ApocalypseMechanics.Affliction.FLAME);
        h.runAfterDelay(161, () -> {
            ApocalypseMechanics.afflict(p, h.getLevel(), t, ApocalypseMechanics.Affliction.SILK);
            ApocalypseMechanics.afflict(p, h.getLevel(), t, ApocalypseMechanics.Affliction.PLAGUE);
            h.assertTrue(!ApocalypseMechanics.hasDespair(p, t), "An expired seal cannot complete the weave");
            ApocalypseMechanics.afflict(p, h.getLevel(), t, ApocalypseMechanics.Affliction.FLAME);
            h.assertTrue(ApocalypseMechanics.hasDespair(p, t), "Fresh seal completes weave");
        });
        h.runAfterDelay(283, () -> {
            equal(h, ApocalypseMechanics.harvest(p, h.getLevel(), t), 0, "Expired Despair cannot be harvested");
            h.succeed();
        });
    }
    private static void harvest(GameTestHelper h) {
        var p = player(h); var t = enemy(h, p);
        p.setHealth(10);
        var near = shard(h, p, t.position(), .9f);
        var far = shard(h, p, t.position().add(30, 0, 0), .5f);
        weave(h, p, t);
        equal(h, ApocalypseMechanics.harvest(p, h.getLevel(), t), 24, "Despair adds finisher damage");
        equal(h, p.getHealth(), 14, "Harvest mends its owner");
        equal(h, near.charge, 1, "Nearby mirror charges, capped at full");
        equal(h, far.charge, .5, "Distant mirror does not charge");
        equal(h, ApocalypseMechanics.harvest(p, h.getLevel(), t), 0, "Window spent exactly once");
        weave(h, p, t);
        h.assertTrue(!ApocalypseMechanics.hasDespair(p, t), "Target cooldown prevents immediate farming");
        h.succeed();
    }
    private static void ambush(GameTestHelper h) {
        var p = player(h); var t = enemy(h, p);
        p.getPersistentData().putInt(Apocalypse.MIRROR_LEFT, Apocalypse.MIRROR_TICKS - 39);
        Apocalypse.leaveMirrorWorld(p, h.getLevel());
        h.assertTrue(!EmpoweredHit.isArmed(p, ApocalypseMechanics.AMBUSH), "Quick mirror toggles do not arm ambush");
        p.getPersistentData().putInt(Apocalypse.MIRROR_LEFT, Apocalypse.MIRROR_TICKS - 40);
        Apocalypse.leaveMirrorWorld(p, h.getLevel());
        h.assertTrue(EmpoweredHit.isArmed(p, ApocalypseMechanics.AMBUSH), "Waiting two seconds arms ambush on exit");
        Spells.strike(p, t, 1);
        h.assertTrue(EmpoweredHit.isArmed(p, ApocalypseMechanics.AMBUSH), "Spell hits do not consume melee ambush");
        t.invulnerableTime = 0;
        float before = t.getHealth();
        p.attack(t);
        h.assertTrue(before - t.getHealth() >= 12, "Real melee gains ambush damage");
        h.assertTrue(!EmpoweredHit.isArmed(p, ApocalypseMechanics.AMBUSH), "Actual melee consumes ambush");
        ApocalypseMechanics.prepareAmbush(p, h.getLevel(), 40);
        h.assertTrue(!EmpoweredHit.isArmed(p, ApocalypseMechanics.AMBUSH), "Ten-second cooldown prevents rearming");
        h.succeed();
    }
    private static void fuel(GameTestHelper h) {
        var p = player(h); var other = player(h);
        Vec3 at = p.position();
        var a = shard(h, p, at, 1); var b = shard(h, p, at, .5f);
        var foreign = shard(h, other, at, 1); var far = shard(h, p, at.add(30, 0, 0), 1);
        equal(h, ApocalypseMechanics.feedCatastrophe(p, h.getLevel(), at), 1.225, "Fuel scales with actual charge");
        equal(h, a.charge + b.charge, 0, "Nearby charge is consumed");
        equal(h, foreign.charge, 1, "Other caster's mirror untouched"); equal(h, far.charge, 1, "Distant mirror untouched");
        equal(h, ApocalypseMechanics.feedCatastrophe(p, h.getLevel(), at), 1, "Charge cannot be reused");
        h.assertTrue(Apocalypse.SHARDS.contains(a), "Drained mirrors remain available to recharge");
        h.succeed();
    }
    private static void refused(GameTestHelper h) {
        var p = player(h); var s = shard(h, p, Spells.aimGround(p, h.getLevel(), Apocalypse.DISASTER_RANGE), 1);
        SoulCore.setSpiritualityQuietly(p, 0);
        h.assertTrue(Apocalypse.CATASTROPHES.cast(p, Apocalypse.CATASTROPHES.entry("earthquake")) == AbilityWheel.Result.REFUSED,
                "Cannot cast without spirituality");
        equal(h, s.charge, 1, "Refused cast spends no mirror fuel");
        SoulCore.setSpiritualityQuietly(p, 1_000_000);
        float before = SoulCore.getSpirituality(p);
        h.assertTrue(Apocalypse.CATASTROPHES.cast(p, Apocalypse.CATASTROPHES.entry("earthquake")) == AbilityWheel.Result.DONE, "Paid cast executes");
        equal(h, s.charge, 0, "Successful wheel cast drains mirrors at its aimed impact");
        equal(h, SoulCore.getSpirituality(p), before - Apocalypse.DISASTER_MEDIUM_COST, "Mirror fuel does not waive spirituality price");
        h.assertTrue(Apocalypse.CATASTROPHES.cooldownLeft(p, Apocalypse.CATASTROPHES.entry("earthquake")) > 0, "Paid catastrophe retains cooldown");
        h.succeed();
    }
    private static void contacts(GameTestHelper h) {
        var p = player(h); var t = enemy(h, p);
        h.assertTrue(Apocalypse.cocoon(p, h.getLevel()) == AbilityWheel.Result.DONE, "Cocoon hits aimed enemy");
        Apocalypse.apocalypsePlague(p, h.getLevel(), h.getLevel(), false);
        Apocalypse.apocalypseBlackFlame(p, h.getLevel(), h.getLevel(), false);
        h.runAfterDelay(35, () -> {
            h.assertTrue(ApocalypseMechanics.hasDespair(p, t), "Real silk, plague and flame callbacks weave Despair");
            var mirror = shard(h, p, t.position(), 0);
            p.setHealth(10);
            Apocalypse.apocalypseFatalStep(p, h.getLevel(), h.getLevel(), false);
            h.assertTrue(EmpoweredHit.isArmed(p, Apocalypse.BACKSTAB), "Fatal Step readies a real backstab");
            t.invulnerableTime = 0;
            p.attack(t);
            h.assertTrue(!ApocalypseMechanics.hasDespair(p, t), "Real backstab harvests the woven window");
            equal(h, p.getHealth(), 14, "Real backstab applies harvest healing");
            equal(h, mirror.charge, .25, "Real backstab charges nearby mirror");
            h.succeed();
        });
    }
    private static void cleanup(GameTestHelper h) {
        var p = player(h); var t = enemy(h, p);
        weave(h, p, t); ApocalypseMechanics.prepareAmbush(p, h.getLevel(), 40);
        ApocalypseMechanics.clear(p);
        h.assertTrue(!ApocalypseMechanics.hasDespair(p, t) && !EmpoweredHit.isArmed(p, ApocalypseMechanics.AMBUSH), "Death/logout/dimension cleanup drops combat preparation");
        p.getPersistentData().putInt("apocalypse_stacks", 19);
        p.getPersistentData().putString("apocalypse_stack_image_19", "attendant_stack_orb");
        ApocalypseMechanics.removeLegacyStacks(p);
        h.assertTrue(!p.getPersistentData().contains("apocalypse_stacks") && !p.getPersistentData().contains("apocalypse_stack_image_19"), "Legacy saves shed counter and images");
        h.succeed();
    }
    private static void allies(GameTestHelper h) {
        var p = player(h); var t = enemy(h, p);
        Friends.add(p, t.getUUID(), "ally", false);
        weave(h, p, t);
        h.assertTrue(!ApocalypseMechanics.hasDespair(p, t) && !NoHeal.isApplied(t), "Friendly bodies cannot be marked");
        equal(h, ApocalypseMechanics.harvest(p, h.getLevel(), t), 0, "Friends cannot yield a harvest");
        h.succeed();
    }
}
