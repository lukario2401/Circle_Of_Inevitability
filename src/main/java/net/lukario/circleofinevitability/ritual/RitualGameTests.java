package net.lukario.circleofinevitability.ritual;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.item.ModItems;
import net.lukario.circleofinevitability.menu.AbilityMenu;
import net.lukario.circleofinevitability.network.COpenAbilityMenuPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
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
        FUNCTIONS.register("ritual_blessings", () -> RitualGameTests::blessings);
        FUNCTIONS.register("ritual_tasks", () -> RitualGameTests::tasks);
        FUNCTIONS.register("ritual_miracle", () -> RitualGameTests::miracle);
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
            if (nextPage < 0) throw new AssertionError("Missing menu choice " + label + " among "
                    + menu.options().stream().map(COpenAbilityMenuPacket.Option::label).toList());
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
        p.getInventory().add(new ItemStack(Items.GOLD_INGOT, 6));
        client.click("Offerings");
        String oldMenu = client.menu().menuId();
        client.click("5 × Gold Ingot");
        AbilityMenu.handleChoice(p, oldMenu, 1);
        Worship f = new Worship(p);
        BlockPos pos = h.absolutePos(TABLE);
        h.assertTrue(f.friendship() == 8 && p.getInventory().countItem(Items.GOLD_INGOT) == 1, "Pay for the larger offering exactly once");
        h.assertTrue(!f.offer(pos, f.deity.offerings().getFirst()), "All gifts share a cooldown");
        float health = p.getHealth();
        h.assertTrue(!f.donateBlood(pos) && p.getHealth() == health, "A missing knife costs no health or cooldown");
        var knife = new ItemStack(ModItems.SACRIFICIAL_KNIFE.get());
        p.setItemInHand(InteractionHand.MAIN_HAND, knife);
        client.click("Offerings");
        client.click("Donate blood");
        h.assertTrue(f.friendship() == 11 && p.getHealth() < health && p.getMaxHealth() == 20, "Blood grants friendship and an early blessing");
        h.assertTrue(!f.donateBlood(pos) && knife.getDamageValue() == 1, "Only a successful donation wears the knife");
        p.giveExperienceLevels(1);
        client.click("Offerings");
        client.click("Offer experience");
        h.assertTrue(f.friendship() == 17 && p.experienceLevel == 0, "Prayer consumes a level");
        client.click("Change faith");
        client.click("The Anarchy");
        h.setBlock(TABLE, Blocks.AIR);
        client.click("Change faith");
        h.assertTrue(new Worship(p).deity.name().equals("God Almighty") && f.friendship() == 17,
                "Breaking the altar invalidates a pending choice");
        h.succeed();
    }

    private static TestClient pledge(GameTestHelper h, String name) {
        var client = new TestClient(h);
        h.setBlock(TABLE, RitualTableBlock.TABLE.get());
        h.useBlock(TABLE, client.player);
        client.click(name);
        client.click("Pledge faith");
        return client;
    }

    private static void floor(GameTestHelper h, BlockPos pos) {
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++)
            h.getLevel().setBlockAndUpdate(pos.offset(x, -1, z), Blocks.POLISHED_DEEPSLATE.defaultBlockState());
    }

    private static void sanctuary(GameTestHelper h, BlockPos pos) {
        floor(h, pos);
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos base = pos.relative(side, 2);
            h.getLevel().setBlockAndUpdate(base, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
            h.getLevel().setBlockAndUpdate(base.above(), Blocks.SOUL_LANTERN.defaultBlockState());
        }
    }

    private static void max(GameTestHelper h, Worship f, BlockPos pos) {
        sanctuary(h, pos);
        for (int i = 0; i < 3; i++) {
            f.grant(100);
            h.assertTrue(f.sacrifice(pos), "A new friendship gate accepts exactly one permanent heart");
        }
    }

    private static void covenant(GameTestHelper h) {
        var client = pledge(h, "The Anarchy");
        var p = client.player;
        var f = new Worship(p);
        var pos = h.absolutePos(TABLE);
        var level = h.getLevel();
        var bare = RitualAltar.inspect(level, pos);
        h.assertTrue(bare.rank() == 1 && bare.limit() == 25 && bare.missing().size() == 9, "Bare altar shows its missing foundation");
        f.grant(25);
        h.assertTrue(f.room(bare, false) == 0 && p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) > 0, "Altar cap and ranked attributes");
        floor(h, pos);
        var improved = RitualAltar.inspect(level, pos);
        h.assertTrue(improved.rank() == 2 && improved.limit() == 60 && improved.missing().size() == 8, "A shared floor advances the altar");
        f.grant(100);
        h.useBlock(TABLE, p);
        client.click("Sacrifice heart");
        client.click("Keep heart");
        h.assertTrue(f.hearts() == 0, "Cancel sacrifice");
        client.click("Sacrifice heart");
        level.setBlockAndUpdate(pos.below(), Blocks.AIR.defaultBlockState());
        client.click("Sacrifice heart");
        h.assertTrue(f.hearts() == 0, "Recheck the structure at confirmation");
        max(h, f, pos);
        var other = new TestClient(h);
        h.assertTrue(new Worship(other.player).deity == null, "Worship remains personal at a shared altar");
        other.player.restoreFrom(p, false);
        Worship.respawn(new PlayerEvent.PlayerRespawnEvent(other.player, false));
        h.assertTrue(other.player.getMaxHealth() == 14 && new Worship(other.player).rank() == 6, "Respawn restores covenant");
        var saved = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, p.registryAccess());
        p.saveWithoutId(saved);
        p.load(TagValueInput.create(ProblemReporter.DISCARDING, p.registryAccess(), saved.buildResult()));
        Worship.login(new PlayerEvent.PlayerLoggedInEvent(p));
        f = new Worship(p);
        h.assertTrue(f.hearts() == 3 && p.getAttributeValue(Attributes.ARMOR) == 1, "Reload keeps only one set of modifiers");
        h.useBlock(TABLE, p);
        client.click("Change faith");
        client.click("God Almighty");
        client.click("Change faith");
        h.assertTrue(new Worship(p).rank() == 0 && p.getMaxHealth() == 20 && p.getAttributeValue(Attributes.ARMOR) == 0,
                "Switch refunds hearts and removes former rank modifiers");
        h.succeed();
    }

    private static void tasks(GameTestHelper h) {
        var client = pledge(h, "God Almighty");
        var p = client.player;
        var f = new Worship(p);
        var pos = h.absolutePos(TABLE);
        var center = Vec3.atCenterOf(pos);
        var input = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.IRON_INGOT),
                ItemStack.EMPTY, new ItemStack(Items.AMETHYST_SHARD), ItemStack.EMPTY,
                new ItemStack(Items.COPPER_INGOT), ItemStack.EMPTY, ItemStack.EMPTY));
        var knife = h.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
                .orElseThrow().value().assemble(input);
        h.assertTrue(knife.is(ModItems.SACRIFICIAL_KNIFE.get()) && knife.getMaxDamage() == 128
                && knife.isValidRepairItem(new ItemStack(Items.IRON_INGOT)), "Craft and repair the sacrificial knife");
        p.setItemInHand(InteractionHand.MAIN_HAND, knife);
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLASS_BOTTLE, 2));
        Mob local = h.spawn(EntityTypes.ZOMBIE, TABLE);
        local.setPos(center);
        local.setHealth(0.01f);
        p.attack(local);
        h.assertTrue(f.taskProgress(pos) == 0 && p.getOffhandItem().getCount() == 2, "A local kill gives neither task credit nor blood");
        var spellKill = h.spawn(EntityTypes.HUSK, TABLE);
        spellKill.setPos(center.add(10, 0, 0));
        f.observeQuarry();
        spellKill.setPos(center);
        spellKill.hurtServer(h.getLevel(), p.damageSources().playerAttack(p), 100);
        h.assertTrue(f.taskProgress(pos) == 0 && p.getOffhandItem().getCount() == 2,
                "Holding the knife during another source of damage cannot collect blood");
        for (var type : List.of(EntityTypes.ZOMBIE, EntityTypes.HUSK, EntityTypes.SKELETON)) {
            var mob = h.spawn(type, TABLE);
            mob.setNoAi(true);
            mob.setPos(center.add(10, 0, 0));
            f.observeQuarry();
            mob.setPos(center);
            mob.setHealth(0.01f);
            p.attack(mob);
        }
        h.assertTrue(p.getOffhandItem().is(ModItems.RITUAL_BLOOD_VIAL.get())
                && p.getInventory().getNonEquipmentItems().stream().anyMatch(stack -> stack.is(ModItems.RITUAL_BLOOD_VIAL.get())),
                "A stack of two bottles collects two vials, and a third kill still counts without a bottle");
        h.assertTrue(f.taskProgress(pos) == 3 && f.claimTask(pos), "Bring three different mobs to the altar and slay them");
        h.assertTrue(!f.claimTask(pos) && f.waitSeconds("task_ready") == 240 && f.task().kind() == RitualTask.Kind.ASSEMBLY,
                "Paid task rotates and starts its cooldown");
        h.useBlock(TABLE, p);
        client.click("Offerings");
        client.click("1 × Ritual Blood Vial");
        h.assertTrue(f.friendship() == 18 && f.waitSeconds("offering_ready") > 0
                && p.getInventory().countItem(Items.GLASS_BOTTLE) == 1, "Dedicating blood earns four friendship and returns its bottle");
        var assembly = pledge(h, "The Anarchy");
        var crowd = new Worship(assembly.player);
        assembly.player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.RITUAL_BLOOD_VIAL.get()));
        assembly.click("Offerings");
        assembly.click("1 × Ritual Blood Vial");
        h.assertTrue(crowd.friendship() == 4 && assembly.player.getOffhandItem().is(Items.GLASS_BOTTLE),
                "An offhand-only vial can be dedicated and returns a bottle to that hand");
        for (int i = 0; i < 9; i++) h.spawn(EntityTypes.VILLAGER, TABLE).setPos(center);
        h.assertTrue(crowd.taskProgress(pos) == 9 && !crowd.claimTask(pos), "Nine villagers cannot complete the gathering");
        h.spawn(EntityTypes.VILLAGER, TABLE).setPos(center);
        h.assertTrue(crowd.claimTask(pos) && crowd.friendship() == 24, "Ten living villagers gathered at once");
        h.succeed();
    }

    private static void blessings(GameTestHelper h) {
        var client = pledge(h, "God Almighty");
        var p = client.player;
        var f = new Worship(p);
        f.grant(50);
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 1));
        p.getFoodData().setFoodLevel(20);
        p.setHealth(8);
        f.onlineSecond();
        h.assertTrue(p.hasEffect(MobEffects.REGENERATION) && p.getEffect(MobEffects.REGENERATION).getAmplifier() == 0,
                "A wounded worshipper receives low-level regeneration");
        p.getFoodData().setFoodLevel(5);
        f.onlineSecond();
        h.assertTrue(!p.hasEffect(MobEffects.SATURATION), "A second eligible effect waits for the shared pulse cooldown");
        h.useBlock(TABLE, p);
        client.click("Change faith");
        client.click("Lord of the Mysteries");
        client.click("Change faith");
        h.assertTrue(!p.hasEffect(MobEffects.REGENERATION) && p.getEffect(MobEffects.NIGHT_VISION).getAmplifier() == 1,
                "Changing removes the ritual effect and preserves an external potion");
        var seer = pledge(h, "Lord of the Mysteries").player;
        var gray = new Worship(seer);
        double speed = seer.getAttributeValue(Attributes.MOVEMENT_SPEED);
        gray.grant(25);
        h.assertTrue(Math.abs(seer.getAttributeValue(Attributes.MOVEMENT_SPEED) - speed * 1.04) < 0.00001,
                "Early movement gain remains four percent");
        var mob = h.spawn(EntityTypes.HUSK, TABLE);
        mob.setPos(seer.position());
        gray.onlineSecond();
        h.assertTrue(mob.hasEffect(MobEffects.GLOWING), "Spiritual intuition reveals a nearby hostile");
        h.succeed();
    }

    private static int command(ServerPlayer p, String command, boolean op) {
        CommandSourceStack source = p.createCommandSourceStack().withSuppressedOutput()
                .withPermission(op ? PermissionSet.ALL_PERMISSIONS : PermissionSet.NO_PERMISSIONS);
        try {
            return p.level().getServer().getCommands().getDispatcher().execute(command, source);
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException denied) {
            return 0;
        }
    }

    private static void miracle(GameTestHelper h) {
        var client = pledge(h, "Lord of the Mysteries");
        var p = client.player;
        var pos = h.absolutePos(TABLE);
        var f = new Worship(p);
        String grant = "favour add " + p.getScoreboardName() + " 100";
        h.assertTrue(command(p, grant, false) == 0 && f.friendship() == 0, "Only operators can grant points");
        h.assertTrue(command(p, grant, true) == 60 && f.hearts() == 0, "Operator grant stops at the first heart gate");
        max(h, f, pos);
        p.awardStat(Stats.ITEM_USED.get(Items.ENDER_PEARL), 2);
        h.assertTrue(f.claimTask(pos) && f.favour() == 10 && f.friendship() == 100, "Sanctuary tasks refill the temporary pool");
        h.assertTrue(!f.offer(pos, f.deity.offerings().getFirst()), "Gifts cannot purchase higher favour");
        f.grant(40);
        h.assertTrue(f.rank() == 7, "Higher favour activates rank seven");
        for (int i = 0; i < 120; i++) f.onlineSecond();
        h.assertTrue(f.favour() == 49 && f.rank() == 6, "Online decay removes the seventh rank only");
        f.grant(100);
        h.useBlock(TABLE, p);
        client.click("Higher favour");
        client.click("Invoke miracle");
        h.assertTrue(f.armed() && !f.arm(pos), "Arm a single miracle");
        p.invulnerableTime = 0;
        p.hurtServer(h.getLevel(), p.damageSources().magic(), 1000);
        h.assertTrue(p.isAlive() && p.getHealth() > 0 && !f.armed() && f.favour() == 0 && f.hearts() == 3
                && f.revivalWait() > Worship.REVIVAL_COOLDOWN_MS - 5000, "A lethal hit spends favour and starts the two-hour cooldown");
        f.grant(100);
        h.assertTrue(!f.arm(pos), "Refilling favour cannot bypass the revival cooldown");
        h.useBlock(TABLE, p);
        client.click("Change faith");
        client.click("God Almighty");
        client.click("Change faith");
        max(h, new Worship(p), pos);
        f = new Worship(p);
        f.grant(100);
        h.assertTrue(!f.arm(pos) && f.revivalWait() > 0, "The miracle is deity-specific and its cooldown survives conversion");
        h.succeed();
    }
}
