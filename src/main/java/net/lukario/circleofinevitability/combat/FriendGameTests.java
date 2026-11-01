package net.lukario.circleofinevitability.combat;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.item.ModItems;
import net.lukario.circleofinevitability.item.artifacts.SunBroochItem;
import net.lukario.circleofinevitability.status.Marionette;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Real command, save/respawn, targeting and ability regressions. */
public final class FriendGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, CircleOfInevitability.MOD_ID);
    static {
        FUNCTIONS.register("friend_players", () -> FriendGameTests::players);
        FUNCTIONS.register("friend_mob_completion", () -> FriendGameTests::mobs);
        FUNCTIONS.register("friend_persistence", () -> FriendGameTests::persistence);
        FUNCTIONS.register("friend_abilities", () -> FriendGameTests::abilities);
    }

    private FriendGameTests() {}

    private static ServerPlayer player(GameTestHelper h, String prefix) {
        String name = prefix + UUID.randomUUID().toString().substring(0, 6);
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), name), false);
        ServerPlayer p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
        p.setGameMode(GameType.SURVIVAL);
        p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        Vec3 at = h.absoluteVec(new Vec3(1.5, 1, 1.5));
        p.setPos(at.x, at.y, at.z);
        p.setNoGravity(true);
        h.addCleanup(ok -> h.getLevel().getServer().getPlayerList().remove(p));
        return p;
    }

    private static Mob mob(GameTestHelper h, ServerPlayer p, double offsetX, double offsetZ) {
        Mob mob = h.spawn(EntityTypes.HUSK, new BlockPos(1, 1, 1));
        mob.setNoAi(true);
        mob.setNoGravity(true);
        mob.setPos(p.getX() + offsetX, p.getY(), p.getZ() + offsetZ);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        mob.setHealth(100);
        h.addCleanup(ok -> mob.discard());
        return mob;
    }

    private static CommandSourceStack source(ServerPlayer p) {
        return p.createCommandSourceStack().withPermission(PermissionSet.NO_PERMISSIONS).withSuppressedOutput();
    }

    private static int command(GameTestHelper h, ServerPlayer p, String command) {
        try {
            return h.getLevel().getServer().getCommands().getDispatcher().execute(command, source(p));
        } catch (CommandSyntaxException e) {
            throw new AssertionError(command, e);
        }
    }

    private static List<String> suggestions(GameTestHelper h, ServerPlayer p, String command) {
        var dispatcher = h.getLevel().getServer().getCommands().getDispatcher();
        return dispatcher.getCompletionSuggestions(dispatcher.parse(command, source(p))).join().getList().stream()
                .map(com.mojang.brigadier.suggestion.Suggestion::getText).toList();
    }

    private static void players(GameTestHelper h) {
        ServerPlayer owner = player(h, "owner"), ally = player(h, "ally");
        String name = ally.getPlainTextName();
        h.assertTrue(!Spells.isFriend(owner, ally), "Other players start as nonfriends");
        h.assertTrue(suggestions(h, owner, "friend add ").contains(name), "Player names complete without OP");
        h.assertTrue(command(h, owner, "friend add " + name.toUpperCase(java.util.Locale.ROOT)) == 1, "Names resolve case insensitively without OP");
        h.assertTrue(Spells.isFriend(owner, ally) && !Spells.isEnemy(owner, ally), "Player is spared by enemy targeting");
        h.assertTrue(!Spells.isFriend(ally, owner) && Friends.entries(ally).isEmpty(), "Friendship is one-way and personal");
        h.assertTrue(command(h, owner, "friend add " + name) == 0 && Friends.entries(owner).size() == 1, "Duplicates rejected");
        h.assertTrue(command(h, owner, "friend add " + owner.getPlainTextName()) == 0, "Self rejected");
        h.assertTrue(command(h, owner, "friend add invalid-name") == 0, "Invalid input rejected");
        h.assertTrue(command(h, owner, "friend list") == 1 && command(h, owner, "friend") == 1, "Both list forms work");
        h.assertTrue(suggestions(h, owner, "friend remove ").contains(name), "Saved names complete for removal");
        var renamed = new ServerPlayer(h.getLevel().getServer(), h.getLevel(),
                new GameProfile(ally.getUUID(), "RenamedFriend"), ally.clientInformation());
        h.assertTrue(Spells.isFriend(owner, renamed), "Changing a name keeps UUID friendship");
        h.getLevel().getServer().getPlayerList().remove(ally);
        h.assertTrue(command(h, owner, "friend remove " + name) == 1, "Saved name can be removed offline");
        h.assertTrue(!Spells.isFriend(owner, renamed) && command(h, owner, "friend remove " + ally.getUUID()) == 0, "Removal takes effect and missing entry is rejected");
        var scoreboard = h.getLevel().getScoreboard();
        var team = scoreboard.addPlayerTeam("friend_" + UUID.randomUUID().toString().substring(0, 8));
        h.addCleanup(ok -> scoreboard.removePlayerTeam(team));
        scoreboard.addPlayerToTeam(owner.getScoreboardName(), team);
        scoreboard.addPlayerToTeam(ally.getScoreboardName(), team);
        Friends.add(owner, ally.getUUID(), name, true);
        command(h, owner, "friend remove " + name);
        h.assertTrue(Spells.isFriend(owner, ally), "Removing a personal friend preserves scoreboard team protection");
        h.succeed();
    }

    private static void mobs(GameTestHelper h) {
        ServerPlayer owner = player(h, "mobowner");
        // Keep both bodies inside the test structure's enclosing barrier walls.
        owner.setPos(owner.getX(), owner.getY(), owner.getZ() - 1);
        Mob mob = mob(h, owner, 0, 2);
        owner.setYRot(0);
        owner.setXRot(0);
        String id = mob.getUUID().toString();
        h.assertTrue(suggestions(h, owner, "friend add ").contains(id), "Looking at a mob offers its UUID");
        BlockPos wall = owner.blockPosition().offset(0, 1, 1);
        h.getLevel().setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
        h.assertTrue(!suggestions(h, owner, "friend add ").contains(id), "Completion respects walls");
        h.getLevel().setBlock(wall, Blocks.AIR.defaultBlockState(), 3);
        mob.setTarget(owner);
        h.assertTrue(command(h, owner, "friend add " + id) == 1 && Spells.isFriend(owner, mob), "UUID addition changes mob targeting");
        h.assertTrue(!suggestions(h, owner, "friend add ").contains(id), "Already added mob is not offered again");
        h.assertTrue(suggestions(h, owner, "friend remove ").contains(id), "Mob UUID completes for removal");
        h.assertTrue(mob.getTarget() == owner && Spells.isEnemy(mob, owner), "Adding a friend does not tame it or change its allegiance");
        h.assertTrue(command(h, owner, "friend remove " + id) == 1 && Spells.isEnemy(owner, mob), "Mob removal restores enemy targeting");
        UUID unloaded = UUID.randomUUID();
        h.assertTrue(command(h, owner, "friend add " + unloaded) == 1, "Explicit UUID may identify an unloaded mob");
        h.assertTrue(command(h, owner, "friend remove " + unloaded) == 1, "Unloaded entries remain removable");
        var stand = h.spawn(EntityTypes.ARMOR_STAND, new BlockPos(1, 1, 1));
        h.assertTrue(command(h, owner, "friend add " + stand.getUUID()) == 0, "Armor stands rejected");
        stand.discard();
        h.succeed();
    }

    private static void persistence(GameTestHelper h) {
        ServerPlayer owner = player(h, "saved"), clone = player(h, "clone"), loaded = player(h, "loaded");
        Mob mob = mob(h, owner, 0, 2);
        command(h, owner, "friend add " + mob.getUUID());
        clone.restoreFrom(owner, false);
        h.assertTrue(Friends.contains(clone, mob), "Actual death respawn retains friends");
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, clone.registryAccess());
        clone.saveWithoutId(output);
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, loaded.registryAccess(), output.buildResult()));
        h.assertTrue(Friends.contains(loaded, mob) && Friends.entries(loaded).equals(Friends.entries(clone)), "Player save/load retains UUID and label");
        command(h, loaded, "friend remove " + mob.getUUID());
        TagValueOutput removed = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, loaded.registryAccess());
        loaded.saveWithoutId(removed);
        loaded.load(TagValueInput.create(ProblemReporter.DISCARDING, loaded.registryAccess(), removed.buildResult()));
        h.assertTrue(!Friends.contains(loaded, mob), "Removal remains removed after save/load");
        h.succeed();
    }

    private static void abilities(GameTestHelper h) {
        ServerPlayer owner = player(h, "caster");
        Mob friend = mob(h, owner, 0, 1), enemy = mob(h, owner, 1, 1), puppet = mob(h, owner, -1, 1);
        command(h, owner, "friend add " + friend.getUUID());
        h.assertTrue(Spells.alliesAround(owner, h.getLevel(), owner.position(), 6).contains(friend), "Support abilities include personal friends");
        owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.SUN_BROOCH.get()));
        ((SunBroochItem) ModItems.SUN_BROOCH.get()).use(owner.level(), owner, InteractionHand.MAIN_HAND);
        h.assertTrue(friend.getHealth() == 100 && enemy.getHealth() < 100, "Real purification spares friend and damages enemy");
        h.assertTrue(Marionette.control(puppet, owner, 200), "Marionette controlled");
        h.addCleanup(ok -> Marionette.release(puppet));
        friend.invulnerableTime = 0;
        float before = friend.getHealth();
        friend.hurt(owner.level().damageSources().mobAttack(puppet), 10);
        h.assertTrue(friend.getHealth() == before, "Marionette accidental damage cannot hurt owner's friend");
        owner.setLastHurtMob(friend);
        h.runAfterDelay(15, () -> {
            h.assertTrue(puppet.getTarget() != friend, "Marionette does not target owner's friend even when owner hit them");
            command(h, owner, "friend remove " + friend.getUUID());
            friend.invulnerableTime = 0;
            friend.hurt(owner.level().damageSources().mobAttack(puppet), 10);
            h.assertTrue(friend.getHealth() < before, "Removing friendship restores marionette damage");
            h.succeed();
        });
    }
}
