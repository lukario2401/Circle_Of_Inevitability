package net.lukario.circleofinevitability.network;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.conqueror.ConquerorCombos;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Trap;
import net.lukario.circleofinevitability.status.Taunt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Consumer;

import static net.lukario.circleofinevitability.network.SAbilityKeyPacket.Action.*;

public final class AbilityComboGameTest {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, CircleOfInevitability.MOD_ID);

    static {
        FUNCTIONS.register("conqueror_ability_combos", () -> AbilityComboGameTest::run);
    }

    private AbilityComboGameTest() {}

    private static void run(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        Vec3 origin = test.absoluteVec(new Vec3(1.5, 35, 1.5));
        BlockPos floor = BlockPos.containing(origin).below();
        for (BlockPos position : BlockPos.betweenClosed(floor.offset(-3, 0, -3), floor.offset(3, 0, 8))) {
            level.setBlockAndUpdate(position, Blocks.STONE.defaultBlockState());
        }
        ServerPlayer player = player(test, origin);
        Mob quarry = target(test, origin.add(0, 0, 5));
        Mob trapVictim = target(test, origin.add(0, 5, 0));

        test.startSequence()
                .thenExecute(() -> {
                    player.setXRot(60);
                    Vec3 trapPosition = Spells.aimGround(player, level, 16);
                    float before = SoulCore.getSpirituality(player);
                    AbilityKeys.input(player, 3, PRESS);
                    test.assertTrue(SoulCore.getSpirituality(player) == before, "A chord starter waits");
                    AbilityKeys.input(player, 1, PRESS);
                    assertCombo(test, player, ConquerorCombos.RAVEN_TRAP, before, 700);
                    AbilityKeys.input(player, 3, REPEAT);
                    AbilityKeys.input(player, 1, REPEAT);
                    release(player, 3);
                    release(player, 1);
                    test.assertTrue(Trap.count(player, ConquerorCombos.RAVEN_TRAP, 3) == 1
                            && Trap.count(player, "conqueror_trap", 3) == 0, "Only the fused trap is armed");
                    test.assertTrue(SoulCore.getSpirituality(player) == before - 700, "Chord repeats and releases cost nothing");
                    trapVictim.setPos(trapPosition.x, trapPosition.y, trapPosition.z);

                    before = SoulCore.getSpirituality(player);
                    AbilityKeys.input(player, 2, PRESS);
                    AbilityKeys.input(player, 4, PRESS);
                    assertCombo(test, player, ConquerorCombos.ASHEN_BASTION, before, 1600);
                    test.assertTrue(Dome.isRaised(player, ConquerorCombos.ASHEN_BASTION), "The bastion raises a dome");
                    release(player, 2);
                    release(player, 4);
                })
                .thenIdle(35)
                .thenExecute(() -> {
                    test.assertTrue(Trap.count(player, ConquerorCombos.RAVEN_TRAP, 3) == 0
                            && trapVictim.getHealth() < 960, "The trap springs and its ravens deal damage");
                    trapVictim.discard();
                    player.setXRot(0);
                    float before = SoulCore.getSpirituality(player);
                    tap(player, 2);
                    AbilityKeys.input(player, 5, PRESS);
                    release(player, 5);
                    assertCombo(test, player, "conqueror_executioners_challenge", before, 2100);
                    test.assertTrue(Taunt.isTaunted(quarry), "The challenge provokes the quarry before Cull strikes");
                })
                .thenIdle(18)
                .thenExecute(() -> {
                    test.assertTrue(quarry.getHealth() < 960, "The fused Cull hits its provoked quarry");
                    quarry.setHealth(1000);
                    player.setOnGround(true);
                    float before = SoulCore.getSpirituality(player);
                    tap(player, 4);
                    tap(player, 6);
                    assertCombo(test, player, "conqueror_iron_quake", before, 3200);
                })
                .thenIdle(14)
                .thenExecute(() -> {
                    test.assertTrue(quarry.getHealth() < 930 && quarry.hasEffect(MobEffects.SLOWNESS), "Iron Quake hits and slows");
                    tap(player, 2);
                    AbilityKeys.input(player, 0, CANCEL);
                })
                .thenIdle(9)
                .thenExecute(() -> {
                    test.assertTrue(Casts.last(player).name().equals("Iron Quake"), "Menu cancellation drops a released prefix");
                    tap(player, 2);
                })
                .thenIdle(9)
                .thenExecute(() -> test.assertTrue(Casts.last(player).key() == 2, "An untaken sequence falls back to Provocation"))
                .thenSucceed();
    }

    private static void assertCombo(GameTestHelper test, ServerPlayer player, String key, float before, float cost) {
        Casts.Cast cast = Casts.last(player);
        test.assertTrue(cast != null && cast.combo() != null && cast.combo().key().equals(key), "Expected combo " + key);
        test.assertTrue(SoulCore.getSpirituality(player) == before - cost, "Only the combo's spirituality is charged");
    }

    private static void tap(ServerPlayer player, int slot) {
        AbilityKeys.input(player, slot, PRESS);
        release(player, slot);
    }

    private static void release(ServerPlayer player, int slot) {
        AbilityKeys.input(player, slot, RELEASE);
    }

    private static ServerPlayer player(GameTestHelper test, Vec3 origin) {
        ServerLevel level = test.getLevel();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "combo_test"), false);
        var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        player.setPos(origin.x, origin.y, origin.z);
        player.setNoGravity(true);
        player.setYRot(0);
        SoulCore.setPathway(player, "Conqueror");
        SoulCore.setSequence(player, 4);
        SoulCore.setSpiritualityQuietly(player, Beyonder.maxSpirituality(player));
        test.onEachTick(player::doTick);
        test.addCleanup(success -> level.getServer().getPlayerList().remove(player));
        return player;
    }

    private static Mob target(GameTestHelper test, Vec3 at) {
        var target = EntityTypes.HUSK.create(test.getLevel(), EntitySpawnReason.MOB_SUMMONED);
        target.setPos(at.x, at.y, at.z);
        target.setNoAi(true);
        target.setNoGravity(true);
        target.getPersistentData().putBoolean("circle_of_inevitability_beyonder_rolled", true);
        target.getAttribute(Attributes.ARMOR).setBaseValue(0);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1000);
        test.getLevel().addFreshEntity(target);
        test.addCleanup(success -> target.discard());
        return target;
    }
}
