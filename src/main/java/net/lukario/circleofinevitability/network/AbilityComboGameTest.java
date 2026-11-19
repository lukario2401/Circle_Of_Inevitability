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
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.status.Marks;
import net.lukario.circleofinevitability.combat.Cooldowns;
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
import net.minecraft.world.level.block.Block;
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
        FUNCTIONS.register("apocalypse_combo_keys", () -> AbilityComboGameTest::apocalypse);
        FUNCTIONS.register("dark_angel_techniques", () -> AbilityComboGameTest::techniques);
        FUNCTIONS.register("dark_angel_ability_keys", () -> AbilityComboGameTest::darkAngel);
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

    private static void darkAngel(GameTestHelper test) {
        Vec3 origin = test.absoluteVec(new Vec3(1.5, 35, 1.5));
        ServerPlayer player = player(test, origin);
        SoulCore.setPathway(player, "Dark Angel");
        SoulCore.setSequence(player, 6);
        Mob source = target(test, origin.add(0, 0, 5));
        Mob nearby = target(test, origin.add(5.5, 0, 5));
        Mob outside = target(test, origin.add(9, 0, 5));
        nearby.getAttribute(Attributes.MAX_HEALTH).setBaseValue(800);
        outside.getAttribute(Attributes.MAX_HEALTH).setBaseValue(700);
        nearby.setHealth(800);
        outside.setHealth(700);

        test.startSequence().thenExecute(() -> {
            SoulCore.setSpiritualityQuietly(player, 6400);
            AbilityKeys.input(player, 3, PRESS);
            AbilityKeys.input(player, 1, PRESS);
            assertCombo(test, player, "dark_angel_hangmans_snare", 6400, 900);
            release(player, 1);
            release(player, 3);
            tap(player, 2);
            tap(player, 4);
            assertCombo(test, player, "dark_angel_choir_of_flesh", 5500, 1500);
            test.assertFalse(nearby.hasEffect(MobEffects.WEAKNESS), "Choir gives an audible warning before it spreads");
        }).thenIdle(23).thenExecute(() -> {
            test.assertTrue(Root.isRooted(source) && !Root.isRooted(nearby), "Snare singles out the strongest enemy");
            test.assertTrue(source.hasEffect(MobEffects.WEAKNESS) && nearby.hasEffect(MobEffects.WEAKNESS)
                    && !outside.hasEffect(MobEffects.WEAKNESS), "Choir weakens the source and nearby enemies");
            player.setShiftKeyDown(true);
            AbilityKeys.input(player, 4, PRESS);
            test.assertTrue(Casts.last(player).sneak() && Casts.last(player).key() == 4, "Flesh Cloak casts on press");
            release(player, 4);
            player.setShiftKeyDown(false);
            SoulCore.setSpiritualityQuietly(player, 6400);
            AbilityKeys.input(player, 4, PRESS);
            test.assertTrue(SoulCore.getSpirituality(player) == 4900, "The first payment is taken on press");
        }).thenIdle(38).thenExecute(() -> primeInstallment(player))
          .thenIdle(4).thenExecute(() -> assertInstallment(test, player))
          .thenIdle(36).thenExecute(() -> primeInstallment(player))
          .thenIdle(4).thenExecute(() -> assertInstallment(test, player))
          .thenIdle(36).thenExecute(() -> {
              test.assertTrue(Casts.last(player).sneak(), "Flesh Bomb waits for release or the cap");
              primeInstallment(player);
          }).thenIdle(4).thenExecute(() -> {
              assertInstallment(test, player);
              Casts.Cast fired = Casts.last(player);
              test.assertTrue(fired.key() == 4 && !fired.sneak(), "Full charge fires without a release");
              AbilityKeys.input(player, 4, REPEAT);
              release(player, 4);
              test.assertTrue(Casts.last(player) == fired, "Repeat and release cannot fire a second bomb");
          }).thenIdle(48).thenExecute(() -> {
              test.assertTrue(source.getHealth() <= 856 && source.getHealth() > 850, "Full charge hits for 144");
              test.assertTrue(nearby.getHealth() < 800 && outside.getHealth() == 700, "Full charge reaches seven blocks");
              player.setYRot(180);
              SoulCore.setSpiritualityQuietly(player, 1500);
              AbilityKeys.input(player, 4, PRESS);
          }).thenIdle(42).thenExecute(() -> {
              test.assertTrue(Casts.last(player).time() >= player.level().getGameTime() - 3,
                      "An unaffordable second installment fires the paid bomb");
              release(player, 4);
              SoulCore.setSpiritualityQuietly(player, 3000);
              tap(player, 4);
              test.assertTrue(SoulCore.getSpirituality(player) == 1500, "A manual release consumes the base cost only");
              AbilityKeys.input(player, 4, PRESS);
              AbilityKeys.input(player, 0, CANCEL);
          }).thenIdle(42).thenExecute(() -> test.assertTrue(Casts.last(player).time() < player.level().getGameTime() - 40,
                  "A cancelled hold cannot fire later")).thenSucceed();
    }

    private static void techniques(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        Vec3 origin = test.absoluteVec(new Vec3(1.5, 75, 1.5));
        BlockPos floor = BlockPos.containing(origin).below();
        for (BlockPos block : BlockPos.betweenClosed(floor.offset(-4, 0, -2), floor.offset(4, 0, 12))) {
            level.setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
        }
        ServerPlayer player = player(test, origin);
        SoulCore.setPathway(player, "Dark Angel");
        SoulCore.setSequence(player, 3);
        SoulCore.setSpiritualityQuietly(player, 50000);
        Mob target = target(test, origin.add(0, 0, 5));
        target.setYRot(180);
        Mob witness = target(test, origin.add(3, 0, 5));
        test.startSequence().thenExecute(() -> {
            float before = SoulCore.getSpirituality(player);
            player.setXRot(60);
            Vec3 bell = Spells.aimGround(player, level, 20);
            chord(player, 3, 2);
            player.setXRot(0);
            target.setPos(bell.x, bell.y, bell.z);
            witness.setPos(bell.x + 3, bell.y, bell.z);
            assertCombo(test, player, "dark_angel_carrion_bell", before, 800);
        }).thenIdle(7).thenExecute(() -> {
            test.assertTrue(witness.hasEffect(MobEffects.GLOWING) && (witness.getX() < origin.x + 2.99 || witness.getDeltaMovement().x < 0),
                    "Bell glow " + witness.hasEffect(MobEffects.GLOWING) + ", x offset " + (witness.getX() - origin.x)
                            + ", velocity " + witness.getDeltaMovement() + ", traps " + Trap.count(player, "dark_angel_carrion_bell", 1));
            test.assertTrue(Trap.count(player, "dark_angel_carrion_bell", 1) == 0, "The bell springs once");
            witness.discard();
            target.setPos(origin.x, origin.y, origin.z + 5);
            target.setDeltaMovement(Vec3.ZERO);
            target.setYRot(180);
            passageWall(level, floor, Blocks.STONE);
            float before = SoulCore.getSpirituality(player);
            chord(player, 3, 5);
            test.assertTrue(SoulCore.getSpirituality(player) == before && player.distanceToSqr(origin) < 0.01,
                    "Obstructed passage spends nothing");
            passageWall(level, floor, Blocks.AIR);
            SoulCore.setSequence(player, 5);
            SoulCore.setSpiritualityQuietly(player, 10000);
            chord(player, 5, 3);
            assertCombo(test, player, "dark_angel_shepherds_passage", 10000, 1800);
            passageWall(level, floor, Blocks.STONE);
        }).thenIdle(5).thenExecute(() -> {
            test.assertTrue(player.distanceToSqr(origin) < 0.01 && SoulCore.getSpirituality(player) >= 10000
                    && Cooldowns.left(player, "dark_angel_shepherds_passage") == 0, "A closing passage refunds its cost and cooldown");
            passageWall(level, floor, Blocks.AIR);
            SoulCore.setSequence(player, 3);
            float before = SoulCore.getSpirituality(player);
            chord(player, 5, 3);
            assertCombo(test, player, "dark_angel_shepherds_passage", before, 1800);
            test.assertTrue(player.distanceToSqr(origin) < 0.01, "The whisper precedes teleportation");
        }).thenIdle(5).thenExecute(() -> {
            test.assertTrue(player.getZ() > target.getZ() + 1, "Passage lands behind the enemy");
            player.setPos(origin.x, origin.y, origin.z);
            float before = SoulCore.getSpirituality(player);
            chord(player, 4, 6);
            assertCombo(test, player, "dark_angel_black_procession", before, 3600);
        }).thenIdle(16).thenExecute(() -> {
            test.assertTrue(target.getHealth() == 968, "Overlapping ribs deal damage once");
            target.setPos(origin.x, origin.y, origin.z + 5);
            target.setDeltaMovement(Vec3.ZERO);
            target.setHealth(1000);
            float before = SoulCore.getSpirituality(player);
            tap(player, 1);
            tap(player, 4);
            assertCombo(test, player, "dark_angel_crimson_tithe", before, 900);
            tap(player, 5);
            tap(player, 7);
            assertCombo(test, player, "dark_angel_trinity_refrain", before - 900, 7500);
            AbilityKeys.input(player, 4, PRESS);
        }).thenIdle(42).thenExecute(() -> {
            release(player, 4);
            player.setYRot(180);
        }).thenIdle(39).thenExecute(() -> player.setHealth(2))
          .thenIdle(7).thenExecute(() -> {
            test.assertTrue(player.getHealth() >= 14 && player.getHealth() < 17
                    && Marks.count(target, player, "dark_angel_crimson_tithe") == 0,
                    "Tithe heals six hearts and consumes the mark. Health " + player.getHealth());
        }).thenIdle(13).thenExecute(() -> {
            test.assertTrue(target.getHealth() <= 892 && target.getHealth() > 888,
                    "Paid bomb deals 72 and the captured echo deals 36, even after turning. Health " + target.getHealth());
            player.setYRot(0);
            target.setPos(origin.x, origin.y, origin.z + 5);
            target.setDeltaMovement(Vec3.ZERO);
            target.setHealth(1000);
            target.removeEffect(MobEffects.WITHER);
            Cooldowns.reset(player, "dark_angel_trinity_refrain");
            tap(player, 5);
            tap(player, 7);
            tap(player, 3);
        }).thenIdle(44).thenExecute(() -> {
            test.assertTrue(target.getHealth() == 979 && Root.isRooted(target), "Binding hits for 14 and echoes for 7");
            target.setHealth(1000);
            target.setPos(origin.x, origin.y, origin.z + 5);
            target.setDeltaMovement(Vec3.ZERO);
            Cooldowns.reset(player, "dark_angel_trinity_refrain");
            tap(player, 5);
            tap(player, 7);
            tap(player, 2);
        }).thenIdle(39).thenExecute(() -> {
            test.assertTrue(target.getHealth() == 994 && target.hasEffect(MobEffects.WEAKNESS),
                    "The original Whispers and its echo tick independently for 4 and 2");
        }).thenSucceed();
    }

    private static void passageWall(ServerLevel level, BlockPos floor, Block block) {
        for (BlockPos spot : BlockPos.betweenClosed(floor.offset(0, 1, 6), floor.offset(0, 4, 7))) {
            level.setBlockAndUpdate(spot, block.defaultBlockState());
        }
    }

    private static void apocalypse(GameTestHelper test) {
        ServerLevel level = test.getLevel();
        Vec3 origin = test.absoluteVec(new Vec3(1.5, 70, 1.5));
        ServerPlayer player = player(test, origin);
        SoulCore.setPathway(player, "Apocalypse");
        SoulCore.setSequence(player, 7);
        Mob[] targets = new Mob[4];
        for (int i = 0; i < targets.length; i++) targets[i] = target(test, origin.add(0, 0, 3 + i * 2));
        String lance = "apocalypse_obsidian_lance";
        test.startSequence().thenExecute(() -> {
            SoulCore.setSpiritualityQuietly(player, 1800);
            AbilityKeys.input(player, 3, PRESS);
            AbilityKeys.input(player, 2, PRESS);
            test.assertTrue(SoulCore.getSpirituality(player) == 1400, "First installment taken once");
            release(player, 3);
            test.assertTrue(targets[0].getHealth() == 982 && targets[1].getHealth() == 1000, "Releasing slot 3 fires a tier-one lance");
            release(player, 2);
            Cooldowns.reset(player, lance);
            SoulCore.setSpiritualityQuietly(player, 1800);
            AbilityKeys.input(player, 2, PRESS);
            AbilityKeys.input(player, 3, PRESS);
        }).thenIdle(38).thenExecute(() -> SoulCore.setSpiritualityQuietly(player, 900))
          .thenIdle(4).thenExecute(() -> {
            float balance = SoulCore.getSpirituality(player);
            test.assertTrue(balance >= 500 && balance < 580, "One installment charged at two seconds");
            release(player, 2);
            release(player, 3);
            test.assertTrue(targets[0].getHealth() == 952 && targets[1].getHealth() == 970
                    && targets[2].getHealth() == 1000, "Tier two pierces two targets for 30");
            Cooldowns.reset(player, lance);
            SoulCore.setSpiritualityQuietly(player, 1980);
            AbilityKeys.input(player, 2, PRESS);
            AbilityKeys.input(player, 3, PRESS);
        }).thenIdle(122).thenExecute(() -> {
            test.assertTrue(targets[3].getHealth() == 946, "Full lance fires automatically and reaches the fourth target");
            AbilityKeys.input(player, 2, REPEAT);
            release(player, 3);
            release(player, 2);
            test.assertTrue(targets[3].getHealth() == 946, "Late release cannot fire twice");
            Cooldowns.reset(player, lance);
            SoulCore.setSpiritualityQuietly(player, 400);
            AbilityKeys.input(player, 3, PRESS);
            AbilityKeys.input(player, 2, PRESS);
        }).thenIdle(42).thenExecute(() -> {
            test.assertTrue(targets[0].getHealth() == 880 && targets[1].getHealth() == 916,
                    "An unaffordable installment releases the last funded tier");
            release(player, 2);
            release(player, 3);
          }).thenSucceed();
    }

    private static void chord(ServerPlayer player, int first, int second) {
        AbilityKeys.input(player, first, PRESS);
        AbilityKeys.input(player, second, PRESS);
        release(player, first);
        release(player, second);
    }

    private static void primeInstallment(ServerPlayer player) {
        SoulCore.setSpiritualityQuietly(player, 3000);
    }

    private static void assertInstallment(GameTestHelper test, ServerPlayer player) {
        float balance = SoulCore.getSpirituality(player);
        test.assertTrue(balance >= 1500 && balance < 1800, "One 1500 payment, plus passive regeneration: " + balance);
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
        UUID id = UUID.randomUUID();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(id, "combo_" + id.toString().substring(0, 8)), false);
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
