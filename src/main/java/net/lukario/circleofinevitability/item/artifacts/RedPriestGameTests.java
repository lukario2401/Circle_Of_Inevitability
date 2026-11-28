package net.lukario.circleofinevitability.item.artifacts;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Healing;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.ShadowSoldier;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.Uniqueness;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquenessItem;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.Uniquenesses;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.Wielder;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks.LoseYourselfDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks.WarNeverRestsDrawback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Consumer;

/** Real-server checks for the Red Priest uniqueness and its framework. Run with -PredPriestTests. */
public final class RedPriestGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, CircleOfInevitability.MOD_ID);
    static {
        FUNCTIONS.register("red_priest_corruption", () -> RedPriestGameTests::corruption);
        FUNCTIONS.register("red_priest_gates", () -> RedPriestGameTests::gates);
        FUNCTIONS.register("red_priest_might", () -> RedPriestGameTests::might);
        FUNCTIONS.register("red_priest_downsides", () -> RedPriestGameTests::downsides);
        FUNCTIONS.register("red_priest_soldiers", () -> RedPriestGameTests::soldiers);
        FUNCTIONS.register("red_priest_final_conquest", () -> RedPriestGameTests::finalConquest);
        FUNCTIONS.register("red_priest_abilities", () -> RedPriestGameTests::abilities);
    }
    private RedPriestGameTests() {}

    private static final Uniqueness U = RedPriest.UNIQUENESS;

    private static ServerPlayer player(GameTestHelper h, boolean carrying) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "red-priest-test"), false);
        ServerPlayer p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            @Override public boolean isClientAuthoritative() { return false; }
        };
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
        p.setGameMode(GameType.SURVIVAL);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        Vec3 at = h.absoluteVec(new Vec3(1.5, 1, .5));
        p.setPos(at.x, at.y, at.z);
        p.setNoGravity(true);
        if (carrying) p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(U.item()));
        h.addCleanup(ok -> p.discard());
        return p;
    }

    private static void tick(ServerPlayer p) {
        SealedArtifacts.tickCarried(p.level(), p);
    }

    private static Wielder wielder(ServerPlayer p) {
        return U.wielder(p.level(), p, SealedArtifacts.carried(p, U.id()));
    }

    private static void corruption(GameTestHelper h) {
        ServerPlayer p = player(h, true);
        tick(p);
        h.assertTrue(Math.abs(U.corruption(p) - U.gainPerSecond()) < 1e-6, "Carrying it corrupts you a little each second");
        h.assertTrue(U.levelAt(0) == RedPriest.LOW && U.levelAt(30) == RedPriest.MEDIUM && U.levelAt(60) == RedPriest.HIGH
                && U.levelAt(80) == RedPriest.EXTREME, "Four levels: Low, Medium, High, Extreme");
        U.setCorruption(p, 30);
        ItemStack spear = p.getMainHandItem();
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        tick(p);
        h.assertTrue(U.corruption(p) < 30, "Put away before Extreme, it fades");
        p.setItemInHand(InteractionHand.MAIN_HAND, spear);
        U.setCorruption(p, 75 - U.gainPerSecond() / 2);
        tick(p);
        h.assertTrue(U.bound(p), "At Extreme it binds itself to you");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        double before = U.corruption(p);
        tick(p);
        h.assertTrue(!SealedArtifacts.carried(p, U.id()).isEmpty(), "Bound, it comes back when put away");
        h.assertTrue(U.corruption(p) >= before, "Bound, its corruption no longer fades");
        U.reset(p);
        h.succeed();
    }

    private static void gates(GameTestHelper h) {
        h.assertTrue(U.requiredLevel(9) == RedPriest.LOW && U.requiredLevel(3) == RedPriest.LOW, "Sequence 3-9 from the start");
        h.assertTrue(U.requiredLevel(2) == RedPriest.HIGH && U.requiredLevel(1) == RedPriest.HIGH, "Sequence 1-2 need High");
        h.assertTrue(U.requiredLevel(0) == RedPriest.EXTREME, "Sequence 0 needs Extreme");
        h.assertTrue(U.powers().stream().anyMatch(power -> power.name().contains("Hunter's Trap")), "It lends the Conqueror's keys");
        h.assertTrue(U.power("red_priest/iron_blood") != null && U.power("red_priest/crimson_dominion") != null,
                "And its own abilities");
        h.assertTrue(U.powers().getFirst().sequence() == 0 && U.powers().getFirst().id().startsWith("red_priest/"),
                "The picker lists the strongest Sequence first, its own abilities leading");
        for (int i = 1; i < U.powers().size(); i++)
            h.assertTrue(U.powers().get(i - 1).sequence() <= U.powers().get(i).sequence(), "Sorted from Sequence 0 down to 9");
        h.assertTrue(U.powers().stream().filter(power -> power.sequence() <= 2 && power.id().startsWith("red_priest/")).count() >= 9,
                "At least three of its own abilities at each of Sequence 2, 1 and 0");
        ServerPlayer p = player(h, true);
        Mob zombie = h.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 1, 3));
        zombie.setNoAi(true);
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, zombie.getEyePosition());
        U.select(p, U.power("red_priest/conquest"));
        U.item().use(p.level(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(zombie.getHealth() == zombie.getMaxHealth(), "Conquest is refused at Low");
        U.setCorruption(p, 55);
        U.item().use(p.level(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(zombie.getHealth() < zombie.getMaxHealth() || !zombie.isAlive(), "At High it goes off");
        h.assertTrue(p.getHealth() < p.getMaxHealth(), "And it costs blood");
        U.reset(p);
        h.succeed();
    }

    private static void might(GameTestHelper h) {
        ServerPlayer p = player(h, true);
        float before = p.getHealth();
        p.hurtServer(p.level(), p.damageSources().generic(), 10);
        h.assertTrue(Math.abs(before - p.getHealth() - 1) < .01f, "It takes 90% off a hit");
        p.invulnerableTime = 0;
        before = p.getHealth();
        p.hurtServer(p.level(), U.damage(p.level()), 2);
        h.assertTrue(Math.abs(before - p.getHealth() - 2) < .01f, "But nothing off its own harm");
        Mob zombie = h.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 1, 2));
        zombie.setNoAi(true);
        zombie.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).setCount(0);
        float zBefore = zombie.getHealth();
        Spells.strike(p, zombie, 3);
        h.assertTrue(zBefore - zombie.getHealth() > 3.5f, "Its wielder hits twice as hard");
        p.setHealth(4);
        Spells.strike(p, zombie, 1000);
        h.assertTrue(p.getHealth() >= p.getMaxHealth() - .01f, "A kill heals the victim's maximum health");
        U.reset(p);
        h.succeed();
    }

    private static void downsides(GameTestHelper h) {
        ServerPlayer p = player(h, true);
        WarNeverRestsDrawback rest = U.drawbacks().stream().filter(WarNeverRestsDrawback.class::isInstance)
                .map(WarNeverRestsDrawback.class::cast).findFirst().orElseThrow();
        h.assertTrue(rest.refusesSleep(wielder(p)) != null, "You cannot sleep");
        p.setHealth(10);
        U.setCorruption(p, 55);
        p.heal(4);
        h.assertTrue(Math.abs(p.getHealth() - 12) < .01f, "At High healing is halved");
        U.setCorruption(p, 80);
        p.heal(4);
        h.assertTrue(Math.abs(p.getHealth() - 12) < .01f, "At Extreme it stops");
        Healing.apply(p, 4);
        h.assertTrue(p.getHealth() > 12, "Abilities still heal");
        LoseYourselfDrawback lost = U.drawbacks().stream().filter(LoseYourselfDrawback.class::isInstance)
                .map(LoseYourselfDrawback.class::cast).findFirst().orElseThrow();
        U.setCorruption(p, 100);
        h.assertTrue(lost.swallowChance(wielder(p)) >= .29f, "At full corruption many inputs are lost");
        h.assertTrue(Uniquenesses.suffering(p).size() == 1, "The downsides act on the wielder");
        p.setGameMode(GameType.CREATIVE);
        h.assertTrue(Uniquenesses.suffering(p).isEmpty(), "Creative players are spared");
        U.reset(p);
        h.succeed();
    }

    private static void soldiers(GameTestHelper h) {
        ServerPlayer p = player(h, true);
        p.setNoGravity(false);
        int risen = RedPriest.SOLDIERS.wave(wielder(p));
        h.assertTrue(risen == 1, "At Low one soldier rises");
        var soldiers = p.level().getEntitiesOfClass(ShadowSoldier.class, p.getBoundingBox().inflate(16));
        h.assertTrue(soldiers.size() == 1 && soldiers.getFirst().rank() == 0, "A Levy");
        h.assertTrue(soldiers.getFirst().rising(), "It is still climbing out of the ground");
        U.setCorruption(p, 80);
        h.assertTrue(RedPriest.SOLDIERS.wave(wielder(p)) >= 3, "At Extreme a bigger wave");
        float before = p.getHealth();
        p.hurtServer(p.level(), p.damageSources().mobAttack(soldiers.getFirst()), 4);
        h.assertTrue(before - p.getHealth() > 3.9f, "Soldiers' blows ignore the protection");
        for (ShadowSoldier soldier : p.level().getEntitiesOfClass(ShadowSoldier.class, p.getBoundingBox().inflate(16))) soldier.discard();
        U.reset(p);
        h.succeed();
    }

    private static void finalConquest(GameTestHelper h) {
        ServerPlayer p = player(h, true);
        U.setCorruption(p, Uniqueness.MAX);
        U.state(p).putBoolean(Uniqueness.BOUND, true);
        U.state(p).putInt(net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks.FinalConquestDrawback.CLOCK,
                RedPriest.FINAL_CONQUEST.seconds() - 1);
        tick(p);
        h.assertTrue(p.isDeadOrDying(), "At the end of the clock it conquers you");
        var usurpers = p.level().getEntitiesOfClass(ShadowSoldier.class, p.getBoundingBox().inflate(8),
                s -> ShadowSoldier.USURPER.equals(s.palette()));
        h.assertTrue(usurpers.size() == 1, "A Usurper rises where you fell");
        h.assertTrue(U.corruption(p) == 0 && !U.bound(p), "And you start over");
        h.assertTrue(p.getInventory().getNonEquipmentItems().stream().noneMatch(s -> s.getItem() instanceof UniquenessItem), "It took the spear");
        usurpers.getFirst().unmake();
        h.succeed();
    }

    private static void abilities(GameTestHelper h) {
        ServerPlayer p = player(h, true);
        h.assertTrue(U.requiredLevel(U.selected(p).sequence()) == RedPriest.LOW, "At Low the default pick is one you can cast");
        U.setCorruption(p, 80);
        Mob zombie = h.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 1, 2));
        zombie.setNoAi(true);
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, zombie.getEyePosition());
        U.select(p, U.power("red_priest/subjugation"));
        U.item().use(p.level(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(zombie.getHealth() < zombie.getMaxHealth() * .5f, "Subjugation breaks the one you look at");
        U.select(p, U.power("red_priest/endless_war"));
        U.state(p).putLong("ready_at", 0);
        U.item().use(p.level(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.conqueror.ConquerorHigher.endlessWarActive(p),
                "Endless War rages");
        Spells.strike(p, zombie, 1000);
        h.runAfterDelay(20, () -> {
            var risen = p.level().getEntitiesOfClass(ShadowSoldier.class, p.getBoundingBox().inflate(16),
                    s -> s.loyal() && p.getUUID().equals(s.owner()));
            h.assertTrue(risen.size() == 1, "The fallen rises again as your soldier");
            risen.forEach(ShadowSoldier::unmake);
            U.reset(p);
            h.succeed();
        });
    }
}
