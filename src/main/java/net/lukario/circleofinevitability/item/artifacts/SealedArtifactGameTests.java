package net.lukario.circleofinevitability.item.artifacts;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.item.ModItems;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Consumer;

/** Real-server checks for artifact activation, side effects and shared item plumbing. */
public final class SealedArtifactGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, CircleOfInevitability.MOD_ID);
    static {
        FUNCTIONS.register("artifact_purification", () -> SealedArtifactGameTests::purification);
        FUNCTIONS.register("artifact_cooldown", () -> SealedArtifactGameTests::cooldown);
        FUNCTIONS.register("artifact_heat", () -> SealedArtifactGameTests::heat);
        FUNCTIONS.register("artifact_blocked", () -> SealedArtifactGameTests::blocked);
        FUNCTIONS.register("artifact_allies", () -> SealedArtifactGameTests::allies);
        FUNCTIONS.register("artifact_cooling", () -> SealedArtifactGameTests::cooling);
    }
    private SealedArtifactGameTests() {}

    private static SunBroochItem brooch() { return (SunBroochItem) ModItems.SUN_BROOCH.get(); }

    private static final class TestPlayer extends ServerPlayer {
        TestPlayer(net.minecraft.server.level.ServerLevel level, net.minecraft.server.network.CommonListenerCookie cookie) {
            super(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        }

        @Override
        public boolean isClientAuthoritative() { return false; }

        void refreshFluids() { updateFluidInteraction(); }
    }

    private static TestPlayer player(GameTestHelper h) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "artifact-test-player"), false);
        TestPlayer p = new TestPlayer(h.getLevel(), cookie);
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
        p.setGameMode(GameType.SURVIVAL);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        Vec3 at = h.absoluteVec(new Vec3(1.5, 1, 1.5));
        p.setPos(at.x, at.y, at.z);
        p.setNoGravity(true);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SUN_BROOCH.get()));
        h.addCleanup(ok -> p.discard());
        return p;
    }

    private static Mob enemy(GameTestHelper h, ServerPlayer player) {
        Mob target = h.spawn(EntityTypes.HUSK, new BlockPos(1, 1, 2));
        target.setNoAi(true);
        target.setNoGravity(true);
        target.setPos(player.getX(), player.getY(), player.getZ() + .9);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        target.setHealth(100);
        h.addCleanup(ok -> target.discard());
        return target;
    }

    private static void purification(GameTestHelper h) {
        ServerPlayer p = player(h);
        Mob target = enemy(h, p);
        Mob cow = h.spawn(EntityTypes.COW, new BlockPos(1, 1, 2));
        cow.setNoAi(true);
        float cowHealth = cow.getHealth();
        p.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        p.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0));
        h.assertTrue(brooch().use(p.level(), p, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS, "Brooch activates without a pathway");
        h.assertTrue(target.getHealth() < 100 && target.hasEffect(MobEffects.GLOWING), "Undead is damaged and revealed: health=" + target.getHealth() + ", glow=" + target.hasEffect(MobEffects.GLOWING) + ", sight=" + p.hasLineOfSight(target) + ", enemy=" + net.lukario.circleofinevitability.combat.Spells.isEnemy(p, target));
        h.assertTrue(cow.getHealth() == cowHealth, "Neutral animals are unharmed");
        h.assertTrue(!p.hasEffect(MobEffects.POISON) && !p.hasEffect(MobEffects.WITHER) && p.hasEffect(MobEffects.NIGHT_VISION), "Self purification works");
        h.assertTrue(brooch().heat(p) == SunBroochItem.CAST_HEAT, "Successful activation charges heat once");
        h.succeed();
    }

    private static void cooldown(GameTestHelper h) {
        ServerPlayer p = player(h);
        brooch().use(p.level(), p, InteractionHand.MAIN_HAND);
        int heat = brooch().heat(p);
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.SUN_BROOCH.get()));
        h.assertTrue(brooch().use(p.level(), p, InteractionHand.OFF_HAND) == InteractionResult.FAIL, "A second copy cannot bypass cooldown");
        h.assertTrue(brooch().heat(p) == heat, "Refused activation adds no heat");
        h.assertTrue(brooch().cooldownLeft(p) == SunBroochItem.COOLDOWN_TICKS, "Cooldown is fixed, without pathway modifiers");
        var saved = p.getPersistentData().getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG);
        h.assertTrue(saved.contains("circle_of_inevitability_artifact_sun_brooch"), "Heat and cooldown use Forge's death-persisted data");
        brooch().state(p).putLong("ready_at", p.level().getGameTime() - 1);
        h.assertTrue(brooch().use(p.level(), p, InteractionHand.OFF_HAND) == InteractionResult.SUCCESS, "Expired cooldown permits a new use");
        h.succeed();
    }

    private static void heat(GameTestHelper h) {
        ServerPlayer p = player(h);
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.SUN_BROOCH.get()));
        SealedArtifacts.tickCarried(p.level(), p);
        h.assertTrue(brooch().heat(p) == 1, "Duplicate copies heat the player only once per second");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        SealedArtifacts.tickCarried(p.level(), p);
        h.assertTrue(brooch().heat(p) == 2, "Offhand counts as carried");
        brooch().state(p).putInt("heat", SunBroochItem.HOT - 1);
        SealedArtifacts.tickCarried(p.level(), p);
        h.assertTrue(p.hasEffect(MobEffects.WEAKNESS), "Heat threshold applies weakness");
        brooch().state(p).putInt("heat", 1000);
        SealedArtifacts.tickCarried(p.level(), p);
        h.assertTrue(brooch().heat(p) == SunBroochItem.MAX_HEAT && p.hasEffect(MobEffects.SLOWNESS), "Heat is capped and extreme heat slows");
        p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        SealedArtifacts.tickCarried(p.level(), p);
        h.assertTrue(brooch().heat(p) == SunBroochItem.MAX_HEAT - 2, "Removing the brooch cools lingering heat");
        p.setGameMode(GameType.CREATIVE);
        SealedArtifacts.tickCarried(p.level(), p);
        h.assertTrue(brooch().heat(p) == 0, "Creative play is exempt from heat");
        h.succeed();
    }

    private static void blocked(GameTestHelper h) {
        ServerPlayer p = player(h);
        Root.apply(p, 100, RootRestriction.ABILITIES);
        h.assertTrue(brooch().use(p.level(), p, InteractionHand.MAIN_HAND) == InteractionResult.FAIL, "Ability root blocks use");
        h.assertTrue(brooch().cooldownLeft(p) == 0 && brooch().heat(p) == 0, "Blocked use costs nothing");
        Root.remove(p);
        Mob target = enemy(h, p);
        for (int x = 0; x < 3; x++) for (int y = 1; y < 4; y++) h.setBlock(x, y, 2, Blocks.STONE);
        target.setPos(p.getX(), p.getY(), p.getZ() + 3);
        brooch().use(p.level(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(target.getHealth() == 100, "Purification cannot strike through walls");
        h.succeed();
    }

    private static void allies(GameTestHelper h) {
        ServerPlayer p = player(h);
        Mob friend = enemy(h, p);
        var board = p.level().getScoreboard();
        var team = board.addPlayerTeam("artifact_" + p.getId());
        board.addPlayerToTeam(p.getScoreboardName(), team);
        board.addPlayerToTeam(friend.getScoreboardName(), team);
        h.addCleanup(ok -> board.removePlayerTeam(team));
        brooch().use(p.level(), p, InteractionHand.MAIN_HAND);
        h.assertTrue(friend.getHealth() == 100 && !friend.hasEffect(MobEffects.GLOWING), "Allied undead is neither damaged nor revealed");
        h.succeed();
    }

    private static void cooling(GameTestHelper h) {
        TestPlayer p = player(h);
        h.setBlock(1, 1, 1, Blocks.WATER);
        h.runAfterDelay(3, () -> {
            p.refreshFluids();
            h.assertTrue(p.isInWaterOrRain(), "Bearer is immersed in real world water");
            brooch().state(p).putInt("heat", 70);
            SealedArtifacts.tickCarried(p.level(), p);
            h.assertTrue(brooch().heat(p) == 65, "Immersion cools by five rather than adding heat");
            h.succeed();
        });
    }
}
