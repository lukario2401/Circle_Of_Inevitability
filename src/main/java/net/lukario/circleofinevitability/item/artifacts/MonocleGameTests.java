package net.lukario.circleofinevitability.item.artifacts;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.status.Marionette;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Consumer;

/** Real-server checks for the Monocle: stages, losing yourself, death, recovery, Gaze, threads and Foresight. */
public final class MonocleGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, CircleOfInevitability.MOD_ID);
    static {
        FUNCTIONS.register("monocle_stages", () -> MonocleGameTests::stages);
        FUNCTIONS.register("monocle_death", () -> MonocleGameTests::death);
        FUNCTIONS.register("monocle_acts", () -> MonocleGameTests::acts);
        FUNCTIONS.register("monocle_gaze", () -> MonocleGameTests::gaze);
        FUNCTIONS.register("monocle_foresight", () -> MonocleGameTests::foresight);
    }
    private MonocleGameTests() {}

    private static ServerPlayer player(GameTestHelper h, boolean wearing) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "monocle-test-player"), false);
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
        p.setYRot(0);
        p.setXRot(0);
        p.setNoGravity(true);
        if (wearing) p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(MonocleItem.item()));
        h.addCleanup(ok -> p.discard());
        return p;
    }

    private static void tick(ServerPlayer p) {
        SealedArtifacts.tickCarried(p.level(), p);
    }

    private static void stages(GameTestHelper h) {
        ServerPlayer p = player(h, false);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MonocleItem.item()));
        MonocleItem.item().use(p.level(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(MonocleItem.wearing(p) && p.getMainHandItem().isEmpty(), "Right-click puts it on");
        tick(p);
        h.assertTrue(MonocleItem.lost(p) == 1, "Each second worn is a second lost");
        h.assertTrue(p.hasEffect(MobEffects.NIGHT_VISION), "Spirit Vision sees in the dark");
        MonocleItem.setLost(p, MonocleItem.STAGE_THREE - 1);
        tick(p);
        ItemStack worn = p.getItemBySlot(EquipmentSlot.HEAD);
        h.assertTrue(MonocleItem.stage(MonocleItem.lost(p)) == 3, "Stage III after ten minutes");
        h.assertTrue(worn.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag()
                .getIntOr(MonocleItem.STAGE_KEY, 0) == 3, "The stage is on the stack for the model");
        p.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        p.getInventory().add(worn);
        tick(p);
        h.assertTrue(MonocleItem.lost(p) == MonocleItem.STAGE_THREE - 1, "Carried but not worn, you recover");
        h.assertTrue(MonocleItem.actChance(MonocleItem.LOSS_STARTS - 1) == 0, "Nothing happens on its own at first");
        h.assertTrue(MonocleItem.actChance(MonocleItem.DEATH) > .3f, "At the end it acts every few seconds");
        h.assertTrue(!MonocleItem.actsAt(0).contains(MonocleItem.Act.DROP)
                && MonocleItem.actsAt(MonocleItem.DEATH - 1).contains(MonocleItem.Act.STRIKE), "Worse acts come later");
        h.succeed();
    }

    private static void death(GameTestHelper h) {
        ServerPlayer p = player(h, true);
        MonocleItem.setLost(p, MonocleItem.DEATH - 1);
        tick(p);
        h.assertTrue(p.isDeadOrDying(), "Worn too long, it kills you");
        h.assertTrue(MonocleItem.lost(p) == 0, "And it starts over");
        ServerPlayer creative = player(h, true);
        creative.setGameMode(GameType.CREATIVE);
        MonocleItem.setLost(creative, MonocleItem.DEATH - 1);
        tick(creative);
        h.assertTrue(creative.isAlive(), "Creative players are spared");
        h.succeed();
    }

    private static void acts(GameTestHelper h) {
        ServerPlayer p = player(h, true);
        h.spawn(EntityTypes.CHICKEN, new BlockPos(1, 1, 1));
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        for (MonocleItem.Act act : MonocleItem.Act.values()) MonocleItem.act(p.level(), p, act);
        h.assertTrue(!p.getMainHandItem().is(Items.STICK), "It made you drop what you held");
        h.succeed();
    }

    private static Mob target(GameTestHelper h, ServerPlayer p) {
        Mob mob = h.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 1, 2));
        mob.setNoAi(true);
        mob.setPos(p.getX(), p.getY(), p.getZ() + 2);
        return mob;
    }

    private static void gaze(GameTestHelper h) {
        ServerPlayer p = player(h, true);
        Mob mob = target(h, p);
        MonocleItem.gaze(p);
        h.assertTrue(mob.hasEffect(MobEffects.GLOWING), "Gaze reads the target and marks it");
        h.assertTrue(MonocleItem.lost(p) == MonocleItem.GAZE_COST, "Gazing costs a piece of yourself");
        MonocleItem.gaze(p);
        h.assertTrue(MonocleItem.lost(p) == MonocleItem.GAZE_COST, "Gaze has a cooldown");
        SealedArtifactItem.state(p, MonocleItem.ID).putLong("gaze_ready_at", 0L);
        MonocleItem.setLost(p, MonocleItem.STAGE_THREE);
        MonocleItem.gaze(p);
        h.assertTrue(Marionette.isMarionetteOf(mob, p), "From stage III, Gaze pulls a mob's threads");
        h.assertTrue(MonocleItem.lost(p) == MonocleItem.STAGE_THREE + MonocleItem.THREADS_COST, "Threads cost more");
        h.succeed();
    }

    private static void foresight(GameTestHelper h) {
        ServerPlayer p = player(h, true);
        MonocleItem.setLost(p, MonocleItem.STAGE_TWO);
        float before = p.getHealth();
        p.hurtServer(p.level(), p.damageSources().generic(), 10);
        h.assertTrue(p.getHealth() == before, "Foresight: the big hit never lands");
        p.invulnerableTime = 0;
        p.hurtServer(p.level(), p.damageSources().generic(), 10);
        h.assertTrue(p.getHealth() < before, "Only once per recharge");
        ServerPlayer early = player(h, true);
        early.hurtServer(early.level(), early.damageSources().generic(), 10);
        h.assertTrue(early.getHealth() < early.getMaxHealth(), "Not before stage II");
        h.succeed();
    }
}
