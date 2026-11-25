package net.lukario.circleofinevitability.mount;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Consumer;

public final class MountGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, CircleOfInevitability.MOD_ID);
    static {
        FUNCTIONS.register("mount_raptor", () -> MountGameTests::raptor);
        FUNCTIONS.register("mount_wyvern", () -> MountGameTests::wyvern);
        FUNCTIONS.register("mount_whistle", () -> MountGameTests::whistle);
    }

    private MountGameTests() {}

    private static Player rider(GameTestHelper test) {
        Player player = test.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(test.absoluteVec(new Vec3(2.5, 1, 2.5)));
        test.setBlock(new BlockPos(2, 0, 2), Blocks.STONE);
        return player;
    }

    private static RidingMount ride(GameTestHelper test, Player player, Mounts.Breed breed) {
        ItemStack stack = new ItemStack(breed.whistle().get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        test.assertTrue(stack.use(test.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "Whistle activated");
        test.assertTrue(player.getControlledVehicle() instanceof RidingMount, "Player controls the summoned mount");
        RidingMount mount = (RidingMount)player.getControlledVehicle();
        test.assertTrue(mount.getType() == breed.type().get() && stack.getCount() == 1, "Right breed and reusable whistle");
        test.addCleanup(ok -> mount.discard());
        return mount;
    }

    private static void raptor(GameTestHelper test) {
        Player player = rider(test);
        RidingMount mount = ride(test, player, Mounts.RAPTOR);
        player.setYRot(90);
        player.zza = 1;
        mount.setOnGround(true);
        mount.tick();
        test.assertTrue(mount.getX() < player.getX() - 0.1 && mount.getYRot() == 90, "Ground movement follows rider yaw");
        player.setJumping(true);
        mount.setOnGround(true);
        double height = mount.getY();
        mount.tick();
        test.assertTrue(mount.getY() > height + 0.5 && !mount.isFlyingVehicle(), "Raptor jumps from the ground");
        player.setXRot(-90);
        for (int i = 0; i < 17; i++) mount.tick();
        test.assertTrue(mount.getDeltaMovement().y < 0, "Holding jump and looking up cannot sustain flight");
        test.succeed();
    }

    private static void wyvern(GameTestHelper test) {
        Player player = rider(test);
        RidingMount mount = ride(test, player, Mounts.WYVERN);
        double height = mount.getY();
        player.setJumping(true);
        mount.tick();
        test.assertTrue(mount.isFlyingVehicle() && mount.getY() > height, "Jump lifts the wyvern from rest");
        mount.setPos(mount.position().add(0, 3, 0));
        mount.setDeltaMovement(Vec3.ZERO);
        player.setJumping(false);
        height = mount.getY();
        mount.tick();
        test.assertTrue(Math.abs(height - mount.getY()) < 0.001, "Wyvern hovers with neutral input");
        player.zza = 1;
        player.setXRot(45);
        double z = mount.getZ();
        mount.tick();
        test.assertTrue(mount.getZ() > z && mount.getY() < height, "Forward and downward pitch produce a dive");
        test.assertTrue(!mount.causeFallDamage(60, 1, mount.damageSources().fall()) && player.getHealth() == player.getMaxHealth(),
                "Controlled landing protects the rider");
        test.succeed();
    }

    private static void whistle(GameTestHelper test) {
        Player player = rider(test);
        test.setBlock(new BlockPos(2, 3, 2), Blocks.STONE);
        // The seated player's head fits below Y=4, but intersects the block beginning at Y=3.
        test.assertTrue(!RidingMount.summon(player, Mounts.WYVERN) && !player.isPassenger(), "Low ceiling blocks the summon");
        test.setBlock(new BlockPos(2, 3, 2), Blocks.AIR);
        RidingMount mount = ride(test, player, Mounts.WYVERN);
        test.assertTrue(!RidingMount.summon(player, Mounts.RAPTOR) && player.getVehicle() == mount, "Repeat call preserves current ride");
        Player other = test.makeMockPlayer(GameType.SURVIVAL);
        test.assertTrue(mount.interact(other, InteractionHand.MAIN_HAND, Vec3.ZERO) == InteractionResult.PASS && !other.isPassenger(),
                "Another player cannot take an occupied ride");
        player.stopRiding();
        test.runAfterDelay(2, () -> {
            test.assertTrue(mount.isRemoved(), "Dismount dismisses a whistle mount");
            RidingMount permanent = test.spawn(Mounts.RAPTOR.type().get(), new BlockPos(2, 1, 2));
            test.assertTrue(permanent.interact(other, InteractionHand.MAIN_HAND, Vec3.ZERO).consumesAction()
                    && other.getControlledVehicle() == permanent, "A placed mount can also be ridden");
            other.stopRiding();
            permanent.tick();
            test.assertTrue(!permanent.isRemoved(), "Placed mounts remain available for another rider");
            test.succeed();
        });
    }
}
