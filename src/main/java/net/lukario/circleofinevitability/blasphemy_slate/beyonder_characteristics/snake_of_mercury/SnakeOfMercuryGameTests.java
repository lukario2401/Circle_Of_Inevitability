package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.snake_of_mercury;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.fate.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.Luck;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Consumer;

/** Real-server regressions: transfers, cooldown progression, resource-safe history and free form dismissal. */
public final class SnakeOfMercuryGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, CircleOfInevitability.MOD_ID);
    static {
        FUNCTIONS.register("snake_signed_transfer", () -> SnakeOfMercuryGameTests::transfer);
        FUNCTIONS.register("snake_spin_cooldown", () -> SnakeOfMercuryGameTests::spin);
        FUNCTIONS.register("snake_safe_rewind", () -> SnakeOfMercuryGameTests::rewind);
        FUNCTIONS.register("snake_form_toggle", () -> SnakeOfMercuryGameTests::form);
        FUNCTIONS.register("snake_machine_mind", () -> SnakeOfMercuryGameTests::mind);
        FUNCTIONS.register("snake_loop_cleanup", () -> SnakeOfMercuryGameTests::loop);
        FUNCTIONS.register("snake_ledger", () -> SnakeOfMercuryGameTests::ledger);
        FUNCTIONS.register("snake_reincarnation", () -> SnakeOfMercuryGameTests::reincarnation);
        FUNCTIONS.register("snake_failed_cast", () -> SnakeOfMercuryGameTests::failed);
        FUNCTIONS.register("snake_sneak_recast", () -> SnakeOfMercuryGameTests::sneakRecast);
        FUNCTIONS.register("snake_sky_pulse", () -> SnakeOfMercuryGameTests::sky);
    }
    private SnakeOfMercuryGameTests() {}
    private static ServerPlayer player(GameTestHelper h, int sequence) {
        ServerPlayer p = h.makeMockServerPlayerInLevel();
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        Vec3 at = h.absoluteVec(new Vec3(1.5, 1, 1.5)); p.setPos(at.x, at.y, at.z);
        SoulCore.setPathway(p, Fate.PATHWAY); SoulCore.setSequence(p, sequence); SoulCore.setSpiritualityQuietly(p, Float.MAX_VALUE);
        Fate.set(p, WheelTier.GOLD);
        p.getPersistentData().putInt("snake_wheel_sequence", sequence);
        p.getPersistentData().putLong("snake_of_mercury_next_spin", h.getLevel().getGameTime() + 160);
        h.addCleanup(success -> { SnakeOfMercuryCycle.forget(p); p.discard(); });
        return p;
    }
    private static void equal(GameTestHelper h, double actual, double expected, String label) { h.assertTrue(Math.abs(actual - expected) < .0001, label + ": " + actual + " != " + expected); }
    private static void sneakRecast(GameTestHelper h) {
        ServerPlayer p = player(h, 0);
        var wheels = new AbilityWheel[]{SnakeOfMercuryPowers.MISFORTUNE, SnakeOfMercuryPowers.CHAOS, SnakeOfMercuryPowers.CYCLE};
        String[] ids = {"snake_of_mercury_misfortune", "snake_of_mercury_chaos", "snake_of_mercury_cycle"};
        String[] keys = {"gambit", "aura", "loop"};
        for (int i = 0; i < wheels.length; i++) {
            var wheel = wheels[i]; var entry = wheel.entry(keys[i]);
            for (WheelTier tier : WheelTier.values()) {
                SoulCore.setSpiritualityQuietly(p, Float.MAX_VALUE);
                Fate.set(p, tier);
                p.getPersistentData().putString(ids[i] + "_last", keys[i]);
                long ready = h.getLevel().getGameTime() + entry.cooldown();
                p.getPersistentData().putLong(ids[i] + "_ready_" + keys[i], ready);
                h.assertTrue(wheel.cast(p, entry) == AbilityWheel.Result.REFUSED, "Normal cast respects existing cooldown");
                float before = SoulCore.getSpirituality(p);
                int multiplier = tier == WheelTier.NETHERITE ? 1 : tier == WheelTier.DIAMOND ? 2 : 3;
                wheel.quickCast(p);
                equal(h, before - SoulCore.getSpirituality(p), entry.cost() * multiplier, "Sneak price for " + ids[i] + " / " + tier);
                if (i != 0) equal(h, p.getPersistentData().getLongOr(ids[i] + "_ready_" + keys[i], 0L), ready, "Sneak cast starts no cooldown");
                wheel.resetCooldown(p, keys[i]);
                SoulCore.setSpiritualityQuietly(p, Float.MAX_VALUE);
                Fate.set(p, tier);
                wheel.quickCast(p);
                equal(h, wheel.cooldownLeft(p, entry), 0, "Sneak cast with no existing cooldown stays ready");
            }
        }
        for (WheelTier tier : WheelTier.values()) {
            SoulCore.setSpiritualityQuietly(p, Float.MAX_VALUE);
            Fate.set(p, tier);
            p.setShiftKeyDown(true);
            p.getPersistentData().putString("snake_of_mercury_chaos_last", "words");
            net.lukario.circleofinevitability.combat.Cooldowns.start(p, "snake_words", 400);
            float before = SoulCore.getSpirituality(p);
            SnakeOfMercuryPowers.CHAOS.quickCast(p);
            net.lukario.circleofinevitability.menu.AbilityMenu.handleChoice(p, "snake_words", 0);
            int multiplier = tier == WheelTier.NETHERITE ? 1 : tier == WheelTier.DIAMOND ? 2 : 3;
            equal(h, before - SoulCore.getSpirituality(p), 12000 * multiplier, "Sneak words bypass cooldown at " + tier);
        }
        h.succeed();
    }
    private static void transfer(GameTestHelper h) {
        var source = h.spawnWithNoFreeWill(EntityTypes.PIG, 1, 1, 1);
        var target = h.spawnWithNoFreeWill(EntityTypes.PIG, 2, 1, 1);
        Luck.set(source, -30); Luck.set(target, 6);
        equal(h, Luck.transfer(source, target, -10), -10, "Signed transfer");
        equal(h, Luck.balance(source), -20, "Source pays negative reserve");
        equal(h, Luck.balance(target), -4, "Opposite signs cancel");
        Luck.set(source, 30); Luck.set(target, 98);
        equal(h, Luck.transfer(source, target, 20), 2, "Only accepted amount is debited");
        equal(h, Luck.balance(source), 28, "No loss at cap");
        equal(h, Luck.transfer(source, source, 20), 0, "No self duplication");
        ServerPlayer resistant = player(h, 1); Luck.set(resistant, 0);
        equal(h, Luck.transfer(source, resistant, 10), 5, "Fate Resistance");
        equal(h, Luck.balance(source), 23, "Resistance preserves conservation");
        h.succeed();
    }
    private static void spin(GameTestHelper h) {
        ServerPlayer p = player(h, 5); p.setShiftKeyDown(true); Luck.set(p, 100);
        float before = SoulCore.getSpirituality(p);
        SnakeOfMercuryPowers.luck(p, h.getLevel(), h.getLevel(), false);
        equal(h, before - SoulCore.getSpirituality(p), 250, "Spin pays once");
        double reserve = Luck.balance(p); float paid = SoulCore.getSpirituality(p);
        SnakeOfMercuryPowers.luck(p, h.getLevel(), h.getLevel(), false);
        equal(h, Luck.balance(p), reserve, "Cooldown prevents second roll");
        equal(h, SoulCore.getSpirituality(p), paid, "Cooldown prevents second payment");
        SoulCore.setSequence(p, 4);
        SnakeOfMercuryPowers.luck(p, h.getLevel(), h.getLevel(), false);
        float again = SoulCore.getSpirituality(p);
        SnakeOfMercuryPowers.luck(p, h.getLevel(), h.getLevel(), false);
        equal(h, again - SoulCore.getSpirituality(p), 250, "Sequence 4 has no spin cooldown");
        Luck.set(p, 0); Fate.spin(p); h.assertTrue(Fate.peek(p) == WheelTier.GOLD, "Zero must be neutral");
        h.succeed();
    }
    private static void rewind(GameTestHelper h) {
        ServerPlayer p = player(h, 9); Luck.set(p, 40);
        Vec3 home = p.position(); p.setHealth(p.getMaxHealth());
        FateTimeline.record(h.getLevel(), home, 8);
        long recorded = h.getLevel().getGameTime();
        h.runAfterDelay(10, () -> {
            p.setPos(home.x + .5, home.y, home.z); p.setHealth(5);
            Luck.set(p, 7); SoulCore.setSpiritualityQuietly(p, 7);
            int ago = (int) (h.getLevel().getGameTime() - recorded);
            h.assertTrue(FateTimeline.has(p, ago), "Recorded snapshot exists at the actual elapsed server time");
            h.assertTrue(p.isAlive(), "Player remains alive");
            h.assertTrue(!net.lukario.circleofinevitability.root.Root.has(p, net.lukario.circleofinevitability.root.RootRestriction.TELEPORT), "No teleport restriction");
            h.assertTrue(h.getLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(home)), "Recorded chunk remains loaded");
            h.assertTrue(h.getLevel().noCollision(p, p.getBoundingBox().move(home.subtract(p.position()))), "Recorded position is collision-free: " + h.getLevel().getBlockState(net.minecraft.core.BlockPos.containing(home)));
            h.assertTrue(FateTimeline.rewind(p, ago, false), "Available safe history must restore");
            equal(h, p.getHealth(), p.getMaxHealth(), "Health rewound");
            equal(h, Luck.balance(p), 7, "Luck spending is not refunded");
            equal(h, SoulCore.getSpirituality(p), 7, "Spirituality is not refunded");
            h.succeed();
        });
    }
    private static void sky(GameTestHelper h) {
        ServerPlayer p = player(h, 0);
        var wheel = SnakeOfMercuryPowers.CYCLE; var entry = wheel.entry("wheel");
        h.assertTrue(wheel.cast(p, entry) == AbilityWheel.Result.DONE, "Sky Wheel starts");
        p.setHealth(10); Luck.set(p, 0);
        SnakeOfMercuryCycle.playerTick(p, h.getLevel());
        equal(h, Luck.balance(p), 0, "Sky Wheel cannot pulse before four seconds");
        p.getPersistentData().putLong(SnakeOfMercuryPowers.PREFIX + "sky_pulse", h.getLevel().getGameTime());
        SnakeOfMercuryCycle.playerTick(p, h.getLevel());
        equal(h, Luck.balance(p), 10, "Caster receives its own sky blessing");
        equal(h, p.getHealth(), 13, "Caster receives 15 percent healing");
        SnakeOfMercuryCycle.playerTick(p, h.getLevel());
        equal(h, Luck.balance(p), 10, "One pulse per interval");
        float paid = SoulCore.getSpirituality(p);
        h.assertTrue(wheel.cast(p, entry) == AbilityWheel.Result.FREE, "Sky Wheel stops freely through its paid cooldown");
        equal(h, SoulCore.getSpirituality(p), paid, "Early stop does not pay");
        equal(h, Luck.balance(p), 10, "Early stop does not reroll");
        h.succeed();
    }
    private static void failed(GameTestHelper h) {
        ServerPlayer p = player(h, 4); p.setShiftKeyDown(true);
        SoulCore.setSpiritualityQuietly(p, 0); Luck.set(p, -20);
        SnakeOfMercuryPowers.luck(p, h.getLevel(), h.getLevel(), false);
        equal(h, Luck.balance(p), -20, "Failed Spin never consumes negative luck");
        h.assertTrue(Fate.peek(p) == WheelTier.GOLD, "Failed cast preserves displayed material");
        h.assertTrue(SnakeOfMercuryPowers.MISFORTUNE.cast(p, SnakeOfMercuryPowers.MISFORTUNE.entry("serpent")) == AbilityWheel.Result.REFUSED, "Unpaid menu spell refused");
        h.assertFalse(SnakeOfMercuryPowers.active(p, "serpent"), "Unpaid spell cannot create form state");
        h.succeed();
    }
    private static void reincarnation(GameTestHelper h) {
        ServerPlayer p = player(h, 1);
        h.assertTrue(SnakeOfMercuryPowers.CYCLE.cast(p, SnakeOfMercuryPowers.CYCLE.entry("reincarnation")) == AbilityWheel.Result.DONE, "Reincarnation arms");
        h.assertTrue(net.lukario.circleofinevitability.status.DeathWard.isArmed(p), "Ward is armed");
        // Exercise the ward independently of the earlier Body of Mercury passive.
        p.getPersistentData().putLong(SnakeOfMercuryPowers.PREFIX + "body_ready", h.getLevel().getGameTime() + 1800);
        p.setHealth(1); p.invulnerableTime = 0;
        p.hurt(h.getLevel().damageSources().magic(), 100);
        h.assertTrue(p.isAlive() && p.getHealth() >= p.getMaxHealth() * .5f, "Lethal hit revives at half health");
        h.assertFalse(net.lukario.circleofinevitability.status.DeathWard.isArmed(p), "Ward consumed exactly once");
        h.assertTrue(Luck.balance(p) >= 7 && Luck.balance(p) <= 10, "Revival adds ten luck and rolls once");
        h.succeed();
    }
    private static void mind(GameTestHelper h) {
        ServerPlayer p = player(h, 8); p.setShiftKeyDown(true);
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 100));
        SnakeOfMercuryPowers.calculation(p, h.getLevel(), h.getLevel(), false);
        h.assertFalse(p.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS), "Machine Mind clears blindness");
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 100));
        h.assertFalse(p.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS), "Machine Mind denies new slowness");
        h.succeed();
    }
    private static void loop(GameTestHelper h) {
        ServerPlayer p = player(h, 1);
        var wheel = SnakeOfMercuryPowers.CYCLE; var entry = wheel.entry("loop");
        h.assertTrue(wheel.cast(p, entry) == AbilityWheel.Result.DONE, "Fate Loop starts");
        var mediums = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.decoration.ArmorStand.class,
                p.getBoundingBox().inflate(35), e -> e.getPersistentData().getBooleanOr(SnakeOfMercuryPowers.PREFIX + "medium", false));
        h.assertFalse(mediums.isEmpty(), "Loop has a destructible medium");
        SnakeOfMercuryCycle.forget(p);
        h.assertTrue(mediums.stream().noneMatch(net.minecraft.world.entity.Entity::isAlive), "Logout removes caster's medium");
        h.succeed();
    }
    private static void ledger(GameTestHelper h) {
        ServerPlayer p = player(h, 6); p.setShiftKeyDown(false);
        var attacker = h.spawnWithNoFreeWill(EntityTypes.PIG, 2, 1, 1);
        SnakeOfMercuryPowers.calamity(p, h.getLevel(), h.getLevel(), false);
        p.getPersistentData().putLong("snake_of_mercury_next_spin", h.getLevel().getGameTime() + 1000);
        Luck.set(p, 0);
        float before = p.getHealth();
        p.invulnerableTime = 0;
        var source = new net.minecraft.world.damagesource.DamageSource(h.getLevel().damageSources().magic().typeHolder(), attacker);
        h.assertFalse(p.isInvulnerableTo(h.getLevel(), source), "Mock survival player is damageable");
        p.hurt(source, 8);
        equal(h, before - p.getHealth(), 4, "Ledger defers half of incoming damage");
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WITHER, 100));
        h.assertFalse(p.hasEffect(net.minecraft.world.effect.MobEffects.WITHER), "Ledger defers harmful effects");
        h.runAfterDelay(130, () -> {
            h.assertFalse(SnakeOfMercuryPowers.active(p, "ledger"), "Ledger duration expired");
            h.assertTrue(attacker.getHealth() < attacker.getMaxHealth(), "Ledger returned damage to strongest attacker; player ticks=" + p.tickCount + ", attacker loaded=" + (h.getLevel().getEntity(attacker.getUUID()) != null));
            h.succeed();
        });
    }
    private static void form(GameTestHelper h) {
        ServerPlayer p = player(h, 4);
        var wheel = SnakeOfMercuryPowers.MISFORTUNE; var entry = wheel.entry("serpent");
        h.assertTrue(wheel.cast(p, entry) == AbilityWheel.Result.DONE, "Serpent starts");
        float paid = SoulCore.getSpirituality(p); double reserve = Luck.balance(p);
        h.assertTrue(wheel.cast(p, entry) == AbilityWheel.Result.FREE, "Second cast ends form freely");
        equal(h, SoulCore.getSpirituality(p), paid, "Dismissal costs nothing");
        equal(h, Luck.balance(p), reserve, "Dismissal never rerolls");
        h.assertFalse(SnakeOfMercuryPowers.active(p, "serpent"), "Form ended");
        h.succeed();
    }
}
