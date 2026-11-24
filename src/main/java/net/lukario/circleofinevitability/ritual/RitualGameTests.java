package net.lukario.circleofinevitability.ritual;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.item.ModItems;
import net.lukario.circleofinevitability.menu.AbilityMenu;
import net.lukario.circleofinevitability.network.COpenAbilityMenuPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.stats.Stats;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.ForgePayload;
import net.minecraftforge.registries.DeferredRegister;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class RitualGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, CircleOfInevitability.MOD_ID);
    static {
        FUNCTIONS.register("ritual_table", () -> RitualGameTests::table);
        FUNCTIONS.register("ritual_covenant", () -> RitualGameTests::covenant);
    }

    private static final BlockPos TABLE = new BlockPos(1, 1, 1);

    /** Read the menu ID delivered to this client, just as the screen does before sending a choice. */
    private static final class TestClient extends Connection {
        private final ServerPlayer player;
        private ForgePayload lastMessage;

        TestClient(GameTestHelper h) {
            super(PacketFlow.SERVERBOUND);
            UUID id = UUID.randomUUID();
            var cookie = CommonListenerCookie.createInitial(new GameProfile(id, "ritual" + id.toString().substring(0, 6)), false);
            player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
            new EmbeddedChannel(this);
            var players = h.getLevel().getServer().getPlayerList();
            players.placeNewPlayer(this, player, cookie);
            player.setGameMode(GameType.SURVIVAL);
            player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
            player.setPos(h.absoluteVec(new Vec3(1.5, 1, 2.5)));
            h.addCleanup(ok -> players.remove(player));
        }

        @Override
        public void send(Packet<?> packet) {
            if (packet instanceof ClientboundCustomPayloadPacket custom && custom.payload() instanceof ForgePayload payload
                    && payload.id().getNamespace().equals(CircleOfInevitability.MOD_ID)) {
                lastMessage = payload;
            }
            super.send(packet);
        }

        COpenAbilityMenuPacket menu() {
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
            try {
                lastMessage.write(buffer);
                buffer.readVarInt();
                var menu = new COpenAbilityMenuPacket(buffer);
                if (!menu.menuId().startsWith("ritual/")) throw new AssertionError("Expected a ritual table menu");
                return menu;
            } finally {
                buffer.release();
            }
        }

        void click(String label) {
            var menu = menu();
            int nextPage = -1;
            for (int index = 0; index < menu.options().size(); index++) {
                String text = menu.options().get(index).label();
                if (text.equals(label)) {
                    AbilityMenu.handleChoice(player, menu.menuId(), index);
                    return;
                }
                if (text.equals("Next")) nextPage = index;
            }
            if (nextPage < 0) throw new AssertionError("Missing menu choice " + label);
            AbilityMenu.handleChoice(player, menu.menuId(), nextPage);
            click(label);
        }
    }

    private static void table(GameTestHelper h) {
        var level = h.getLevel();
        ItemStack empty = ItemStack.EMPTY;
        var input = CraftingInput.of(3, 3, List.of(empty, new ItemStack(Items.AMETHYST_SHARD), empty,
                new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.BOOK), new ItemStack(Items.COPPER_INGOT),
                empty, new ItemStack(Items.POLISHED_DEEPSLATE), empty));
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
        h.assertTrue(recipe.value().assemble(input).is(ModItems.RITUAL_TABLE.get()), "Craft the registered table");
        h.setBlock(TABLE, RitualTableBlock.TABLE.get());
        var drops = Block.getDrops(RitualTableBlock.TABLE.get().defaultBlockState(), level, h.absolutePos(TABLE), null);
        h.assertTrue(drops.size() == 1 && drops.getFirst().is(ModItems.RITUAL_TABLE.get()), "Recover the table");

        TestClient client = new TestClient(h);
        ServerPlayer p = client.player;
        h.useBlock(TABLE, p);
        client.click("God Almighty");
        client.click("Pledge faith");
        h.assertTrue(new Worship(p).deity.name().equals("God Almighty"), "Select a deity at a placed table");
        p.getInventory().add(new ItemStack(Items.GOLD_INGOT, 3));
        String oldMenu = client.menu().menuId();
        client.click("Give offering");
        AbilityMenu.handleChoice(p, oldMenu, 0);
        h.assertTrue(new Worship(p).friendship() == 5 && p.getInventory().countItem(Items.GOLD_INGOT) == 2,
                "One offering spends one item once");
        client.click("Give offering");
        h.assertTrue(p.getAttributeValue(Attributes.ARMOR) == 1 && p.getMaxHealth() == 20, "Early blessing before heart loss");
        float health = p.getHealth();
        client.click("Donate blood");
        h.assertTrue(new Worship(p).friendship() == 13 && p.getHealth() < health && p.getMaxHealth() == 20,
                "Blood costs current health only");
        p.setHealth(2);
        client.click("Donate blood");
        h.assertTrue(p.getHealth() == 2 && new Worship(p).friendship() == 13, "Refuse blood at one current heart");

        h.setBlock(TABLE, Blocks.AIR);
        client.click("Give offering");
        h.assertTrue(new Worship(p).friendship() == 13, "Removed table cannot take another offering");
        h.setBlock(TABLE, RitualTableBlock.TABLE.get());
        h.useBlock(TABLE, p);
        p.setPos(p.position().add(30, 0, 0));
        client.click("Give offering");
        h.assertTrue(new Worship(p).friendship() == 13, "Leaving the table cancels a pending choice");
        h.succeed();
    }

    private static void covenant(GameTestHelper h) {
        var client = new TestClient(h);
        var p = client.player;
        h.setBlock(TABLE, RitualTableBlock.TABLE.get());
        var zombies = Stats.ENTITY_KILLED.get(EntityTypes.ZOMBIE);
        p.awardStat(zombies, 9);
        h.useBlock(TABLE, p);
        client.click("God Almighty");
        client.click("Pledge faith");
        Worship faith = new Worship(p);
        h.assertTrue(faith.taskProgress() == 0 && !faith.claimTask(), "Only kills made after pledging count");
        p.awardStat(zombies, 3);
        h.useBlock(TABLE, p);
        client.click("Report task");
        h.assertTrue(new Worship(p).friendship() == 10 && !new Worship(p).claimTask(), "Claim one completed task once");

        p.getInventory().add(new ItemStack(Items.GOLD_INGOT, 24));
        for (int heart = 1; heart <= 3; heart++) {
            faith = new Worship(p);
            h.assertTrue(!faith.canSacrifice(), "Friendship gates each sacrifice");
            while (faith.canEarn()) {
                h.assertTrue(faith.offer(), "The deity accepts its offering until the gate");
            }
            int left = p.getInventory().countItem(Items.GOLD_INGOT);
            h.assertTrue(!faith.offer() && p.getInventory().countItem(Items.GOLD_INGOT) == left, "No offering is spent at a gate");
            h.useBlock(TABLE, p);
            client.click("Sacrifice heart");
            client.click("Keep heart");
            h.assertTrue(faith.hearts() == heart - 1, "Declining keeps the heart");
            client.click("Sacrifice heart");
            client.click("Sacrifice heart");
            faith = new Worship(p);
            h.assertTrue(faith.hearts() == heart && p.getMaxHealth() == 20 - 2 * heart
                    && p.getAttributeValue(Attributes.ARMOR) == 3 + heart, "Each confirmed heart grants one late rank");
        }
        h.assertTrue(faith.rank() == 6 && !faith.offer() && !faith.sacrifice(), "Cap all rewards and costs at the final rank");

        var other = new TestClient(h);
        h.assertTrue(new Worship(other.player).deity == null, "Players at one table keep independent covenants");
        other.player.restoreFrom(p, false);
        Worship.respawn(new PlayerEvent.PlayerRespawnEvent(other.player, false));
        h.assertTrue(other.player.getMaxHealth() == 14 && new Worship(other.player).rank() == 6, "Restore covenant after death");
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, p.registryAccess());
        p.saveWithoutId(output);
        p.load(TagValueInput.create(ProblemReporter.DISCARDING, p.registryAccess(), output.buildResult()));
        Worship.login(new PlayerEvent.PlayerLoggedInEvent(p));
        h.assertTrue(p.getMaxHealth() == 14 && p.getAttributeValue(Attributes.ARMOR) == 6, "Reload without stacking modifiers");

        h.useBlock(TABLE, p);
        client.click("Change faith");
        client.click("Lord of the Mysteries");
        client.click("Change faith");
        faith = new Worship(p);
        h.assertTrue(faith.deity.name().equals("Lord of the Mysteries") && faith.friendship() == 0 && faith.hearts() == 0
                && p.getMaxHealth() == 20 && p.getAttributeValue(Attributes.ARMOR) == 0, "Switching refunds hearts and clears worship");
        p.awardStat(Stats.ITEM_USED.get(Items.ENDER_PEARL), 2);
        h.assertTrue(faith.claimTask() && p.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0.1, "Reward the traveler");
        h.useBlock(TABLE, p);
        client.click("Change faith");
        client.click("The Anarchy");
        client.click("Change faith");
        faith = new Worship(p);
        double reach = p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        p.awardStat(Stats.TRADED_WITH_VILLAGER, 3);
        h.assertTrue(faith.claimTask() && p.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE) == reach + 0.25, "Reward the bargainer");
        h.useBlock(TABLE, p);
        client.click("Change faith");
        client.click("God Almighty");
        client.click("Change faith");
        h.assertTrue(new Worship(p).rank() == 0 && new Worship(p).taskProgress() == 0, "Returning to a former deity starts anew");
        h.succeed();
    }
}
