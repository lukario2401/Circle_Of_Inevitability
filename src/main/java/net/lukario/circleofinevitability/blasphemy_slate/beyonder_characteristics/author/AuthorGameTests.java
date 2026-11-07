package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.author;

import java.util.*;
import java.util.function.Consumer;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

/** Real server regressions for payment, control ownership, multiplayer isolation and delayed work. */
public final class AuthorGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, "circle_of_inevitability");
    static {
        FUNCTIONS.register("author_costs", () -> AuthorGameTests::costs);
        FUNCTIONS.register("author_insight", () -> AuthorGameTests::insight);
        FUNCTIONS.register("author_controls", () -> AuthorGameTests::controls);
        FUNCTIONS.register("author_dreams", () -> AuthorGameTests::dreams);
        FUNCTIONS.register("author_support", () -> AuthorGameTests::support);
        FUNCTIONS.register("author_cleanup", () -> AuthorGameTests::cleanup);
        FUNCTIONS.register("author_forms", () -> AuthorGameTests::forms);
        FUNCTIONS.register("author_scripts", () -> AuthorGameTests::scripts);
        FUNCTIONS.register("author_visionary", () -> AuthorGameTests::visionary);
        FUNCTIONS.register("author_manipulation", () -> AuthorGameTests::manipulation);
    }

    private static final Map<GameTestHelper, Integer> HEIGHTS = new WeakHashMap<>();
    private static int nextHeight;

    private static ServerPlayer player(GameTestHelper h, int sequence, double x) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "author" + UUID.randomUUID().toString().substring(0, 6)), false);
        var p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); p.setNoGravity(true);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        Vec3 at = h.absoluteVec(new Vec3(x, HEIGHTS.computeIfAbsent(h, ignored -> 12 + 24 * (nextHeight++ % 10)), 1.5)); p.setPos(at.x, at.y, at.z); p.setYRot(0); p.setXRot(0);
        SoulCore.setPathway(p, Author.PATHWAY); SoulCore.setSequence(p, sequence);
        SoulCore.setSpiritualityQuietly(p, Beyonder.maxSpirituality(p));
        Author.initialize();
        // Embedded connections are not polled by ServerConnectionListener. Drive the same player tick
        // that a real connection performs, so Forge PlayerTick events exercise production handlers.
        h.onEachTick(p::doTick);
        h.addCleanup(ok -> { AuthorMind.clear(p); h.getLevel().getServer().getPlayerList().remove(p); });
        return p;
    }

    private static Mob target(GameTestHelper h, ServerPlayer p, double z, float health) {
        Mob mob = EntityTypes.HUSK.create(h.getLevel(), EntitySpawnReason.MOB_SUMMONED);
        Vec3 at = p.position().add(0, 0, z); mob.setPos(at.x, at.y, at.z);
        mob.setNoAi(true); mob.setNoGravity(true);
        mob.getPersistentData().putBoolean("circle_of_inevitability_beyonder_rolled", true);
        mob.getAttribute(Attributes.ARMOR).setBaseValue(0);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health); mob.setHealth(health);
        h.getLevel().addFreshEntity(mob); h.addCleanup(ok -> mob.discard()); return mob;
    }

    private static AbilityWheel.Result cast(ServerPlayer p, String id) {
        for (var w : Author.WHEELS)
            if (w.entry(id) != null) return w.cast(p, w.entry(id));
        throw new IllegalArgumentException(id);
    }

    private static void floor(GameTestHelper h, ServerPlayer p) {
        for (int x = -4; x <= 4; x++) for (int z = -7; z <= 9; z++) {
            var pos = net.minecraft.core.BlockPos.containing(p.position().add(x, -1, z));
            var before = h.getLevel().getBlockState(pos);
            h.getLevel().setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            h.addCleanup(ok -> h.getLevel().setBlockAndUpdate(pos, before));
        }
    }

    private static void forms(GameTestHelper h) {
        var p = player(h, 4, 1.5); var t = target(h, p, 4, 500);
        h.assertTrue(cast(p, "dragon_form") == AbilityWheel.Result.DONE && Form.isIn(p, AuthorAdvanced.DRAGON)
                && p.getAbilities().mayfly, "Dragon form grants flight");
        h.assertTrue(cast(p, "discern") == AbilityWheel.Result.REFUSED, "Discerner stays locked at Manipulator");
        h.assertTrue(cast(p, "mind_storm") == AbilityWheel.Result.DONE, "Mind Storm starts");
        h.assertTrue(cast(p, "virtual_persona") == AbilityWheel.Result.DONE, "Persona arms");
        var q = player(h, 4, 12.5);
        h.assertTrue(AuthorAdvanced.resists(q, p) && AuthorMind.resolved(p)
                && !AuthorAdvanced.resists(q, p), "Persona intercepts once, then is consumed");
        h.runAfterDelay(66, () -> {
            h.assertTrue(t.getHealth() <= 434, "Mind Storm delivers three visible pulses: health=" + t.getHealth());
            p.getPersistentData().putFloat(AuthorAdvanced.STRAIN, 99);
            h.runAfterDelay(22, () -> {
                h.assertTrue(!Form.isIn(p, AuthorAdvanced.DRAGON) && !p.getAbilities().mayfly,
                        "Full Strain ends the form and restores flight");
                h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.WEAKNESS), "Overloaded mind has aftermath");
                h.succeed();
            });
        });
    }

    private static void scripts(GameTestHelper h) {
        var p = player(h, 1, 1.5); var t = target(h, p, 4, 500);
        AuthorMind.add(p, t, 5);
        h.assertTrue(cast(p, "foreshadow") == AbilityWheel.Result.DONE, "Foreshadow arms");
        p.invulnerableTime = 0;
        p.hurtServer(p.level(), t.damageSources().mobAttack(t), 2);
        h.assertTrue(AuthorMind.state(p).advanced.script == null && t.getHealth() == 500,
                "Foreshadow consumes its script and warns before countering");
        h.runAfterDelay(13, () -> {
            h.assertTrue(t.getHealth() <= 460, "Foreshadow counters once: health=" + t.getHealth());
            AuthorMind.add(p, t, 5); t.invulnerableTime = 0;
            h.assertTrue(cast(p, "fate_script") == AbilityWheel.Result.DONE, "Fate Script arms");
            float health = t.getHealth();
            t.hurtServer(p.level(), p.damageSources().playerAttack(p), 2);
            h.assertTrue(t.getHealth() <= health - 50 && AuthorMind.state(p).advanced.script == null,
                    "Fate Script adds damage once without recursion");
            h.assertTrue(cast(p, "plot_twist") == AbilityWheel.Result.DONE, "Plot Twist arms");
            float before = p.getHealth(); p.invulnerableTime = 0;
            p.hurtServer(p.level(), t.damageSources().mobAttack(t), 4);
            h.assertTrue(p.getHealth() >= before - 2.01f && AuthorMind.state(p).advanced.script == null,
                    "Plot Twist reduces one direct blow");
            h.succeed();
        });
    }

    private static void visionary(GameTestHelper h) {
        var p = player(h, 0, 1.5); floor(h, p); var t = target(h, p, 4, 500);
        h.assertTrue(cast(p, "dream_anchor") == AbilityWheel.Result.DONE, "Dream Anchor opens");
        h.assertTrue(cast(p, "discern") == AbilityWheel.Result.DONE, "Discerner's Gaze opens");
        h.assertTrue(cast(p, "envision") == AbilityWheel.Result.DONE, "Envision forms its blade");
        float health = t.getHealth(); t.hurtServer(p.level(), p.damageSources().playerAttack(p), 2);
        h.assertTrue(t.getHealth() <= health - 20, "Envision empowers a direct melee hit");
        h.assertTrue(cast(p, "life_creation") == AbilityWheel.Result.DONE, "Life Creation summons a drake");
        UUID summoned = AuthorMind.state(p).advanced.summon;
        var drake = AuthorMind.entity(p.level(), summoned);
        h.assertTrue(drake != null && drake.getMaxHealth() == 80 && Marionette.isSummoned(drake), "Drake has health and no-drop summon ownership");
        SoulCore.setSpiritualityQuietly(p, Beyonder.maxSpirituality(p));
        h.assertTrue(cast(p, "sea_of_minds") == AbilityWheel.Result.DONE, "Sea of minds opens");
        AuthorMind.add(p, t, 5);
        h.runAfterDelay(34, () -> {
            h.assertTrue(AuthorMind.state(p).advanced.seaPaid.contains(t.getUUID()), "Sea spends five Insight for its target payoff");
            AuthorMind.clear(p);
            h.assertTrue(AuthorMind.entity(p.level(), summoned) == null, "Owner cleanup discards imagined life");
            h.succeed();
        });
    }

    private static void manipulation(GameTestHelper h) {
        var p = player(h, 3, 1.5); floor(h, p); var t = target(h, p, 4, 100);
        AuthorMind.add(p, t, 5);
        h.assertTrue(cast(p, "mind_walk") == AbilityWheel.Result.DONE, "Mind Walk validates a safe route");
        h.assertTrue(p.distanceToSqr(t) >= 16, "Mind Walk has a visible transit delay");
        h.runAfterDelay(15, () -> {
            h.assertTrue(p.distanceToSqr(t) <= 9, "Mind Walk arrives beside the observed target");
            p.setPos(t.getX(), t.getY(), t.getZ() - 4); p.setYRot(0); p.setXRot(0);
            AuthorMind.add(p, t, 5); SoulCore.setSpiritualityQuietly(p, 59780);
            h.assertTrue(cast(p, "manipulate") == AbilityWheel.Result.DONE && Marionette.isMarionetteOf(t, p),
                    "Five Insight controls an ordinary hostile mob");
            Cleansing.cleanseStatuses(t);
            h.assertTrue(!Marionette.isMarionetteOf(t, p) && AuthorMind.resolved(t), "Cleanse releases the puppet");
            h.succeed();
        });
    }

    private static void costs(GameTestHelper h) {
        var p = player(h, 9, 1.5);
        h.assertTrue(Author.WHEELS.stream().mapToInt(w -> w.entries().size()).sum() == 26,
                "All twenty-six abilities are implemented");
        h.assertTrue(cast(p, "observe") == AbilityWheel.Result.REFUSED && SoulCore.getSpirituality(p) == 20,
                "Empty Observe charges nothing");
        var t = target(h, p, 4, 100);
        h.assertTrue(Author.aim(p, 24) == t, "Target can be aimed at");
        Author.key(p, p.level(), p.level(), 1, false);
        h.assertTrue(SoulCore.getSpirituality(p) == 16 && AuthorMind.insight(p, t) == 1, "G costs four and starts observation");
        String legacyCooldown = Author.BASIC.id() + "_ready_observe";
        h.assertTrue(!p.getPersistentData().contains(legacyCooldown), "Observe saves no cooldown");
        p.getPersistentData().putLong(legacyCooldown, p.level().getGameTime() + 10);
        Author.key(p, p.level(), p.level(), 1, false);
        h.assertTrue(SoulCore.getSpirituality(p) == 12 && AuthorMind.insight(p, t) == 2,
                "G immediately recasts Observe even with a legacy cooldown");
        p.setShiftKeyDown(true); Author.key(p, p.level(), p.level(), 1, false); p.setShiftKeyDown(false);
        h.assertTrue(SoulCore.getSpirituality(p) == 6, "Sneak G is affordable and distinct");
        h.assertTrue(cast(p, "suggestion") == AbilityWheel.Result.REFUSED && SoulCore.getSpirituality(p) == 6, "Sequence gate is free");
        SoulCore.setSequence(p, 6); SoulCore.setSpiritualityQuietly(p, 6400); AuthorMind.state(p);
        h.assertTrue(cast(p, "suggestion") == AbilityWheel.Result.REFUSED && SoulCore.getSpirituality(p) == 6400, "Missing Insight is free");
        AuthorMind.add(p, t, 3); SoulCore.setSpiritualityQuietly(p, 0);
        h.assertTrue(cast(p, "sleep") == AbilityWheel.Result.REFUSED && AuthorMind.insight(p, t) == 3, "Missing spirituality keeps Insight");
        h.succeed();
    }

    private static void insight(GameTestHelper h) {
        var p = player(h, 7, 1.5); var q = player(h, 7, 10.5); var t = target(h, p, 4, 100);
        AuthorMind.add(p, t, 5); AuthorMind.add(q, t, 4);
        h.assertTrue(cast(p, "psychoanalysis") == AbilityWheel.Result.DONE, "Exposed target can be analyzed");
        h.assertTrue(AuthorMind.insight(p, t) == 2 && AuthorMind.insight(q, t) == 4, "Insight is caster-local");
        h.assertTrue(t.getHealth() < 100, "Psychoanalysis damages its target");
        var state = AuthorMind.state(q); state.insight.get(t.getUUID()).decay = q.level().getGameTime();
        Author.tickPlayer(q); h.assertTrue(AuthorMind.insight(q, t) == 3, "Idle Insight decays");
        Friends.add(p, t.getUUID(), "test ally", false);
        float before = SoulCore.getSpirituality(p);
        h.assertTrue(cast(p, "frenzy") == AbilityWheel.Result.REFUSED && SoulCore.getSpirituality(p) == before, "Friends cannot be targeted");
        Friends.remove(p, t.getUUID());
        for (int i = 0; i < 10; i++) AuthorMind.add(q, target(h, p, 10 + i, 20), 1);
        h.assertTrue(AuthorMind.state(q).insight.size() == 8, "Insight store is bounded to eight targets");
        h.succeed();
    }

    private static void controls(GameTestHelper h) {
        var p = player(h, 6, 1.5); var t = target(h, p, 4, 100);
        Root.apply(t, 200, RootRestriction.MOVE);
        AuthorMind.add(p, t, 5);
        h.assertTrue(cast(p, "sleep") == AbilityWheel.Result.DONE && !Slumber.isAsleep(t), "Sleep has a visible induction");
        h.runAfterDelay(18, () -> {
            h.assertTrue(Slumber.isAsleep(t), "Induction: now="+p.level().getGameTime()+" tick="+p.tickCount+" pending="+AuthorMind.state(p).pending+" eligible="+Author.eligible(p,t,20,true)+" p="+p.position()+" t="+t.position());
            Spells.strike(p, t, 1);
            h.assertTrue(!Slumber.isAsleep(t) && AuthorMind.resolved(t), "Positive damage wakes and grants Resolve");
            h.assertTrue(Root.has(t, RootRestriction.MOVE), "Waking preserves unrelated roots");
            h.assertTrue(!Root.has(t, RootRestriction.ABILITIES), "Only the sleep restrictions are removed");
            AuthorMind.add(p, t, 5); float before = SoulCore.getSpirituality(p);
            h.assertTrue(cast(p, "battle_hypnosis") == AbilityWheel.Result.REFUSED && SoulCore.getSpirituality(p) == before,
                    "Resolve rejects repeat control for free");
            t.getPersistentData().remove(AuthorMind.RESOLVE);
            h.assertTrue(cast(p, "battle_hypnosis") == AbilityWheel.Result.DONE, "Battle hypnosis holds an eligible target");
            Cleansing.cleanseStatuses(t);
            h.assertTrue(!Root.has(t, RootRestriction.ATTACK) && Root.has(t, RootRestriction.MOVE), "Cleanse removes only Author's lease");
            h.assertTrue(AuthorMind.duration(p, t, 100, false) == 100, "Ordinary duration=" + AuthorMind.duration(p,t,100,false) + " max=" + t.getMaxHealth());
            t.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
            h.assertTrue(AuthorMind.duration(p, t, 100, false) == 30, "Boss control is capped at 1.5 seconds");
            h.succeed();
        });
    }

    private static void dreams(GameTestHelper h) {
        var p = player(h, 5, 1.5); var t = target(h, p, 4, 150);
        var q = player(h, 5, 12.5); var other = target(h, q, 4, 150);
        AuthorMind.add(p, t, 3);
        h.assertTrue(cast(p, "dream_entry") == AbilityWheel.Result.DONE && AuthorMind.insight(p, t) == 0, "Dream Entry spends three Insight");
        h.assertTrue(cast(p, "nightmare") == AbilityWheel.Result.DONE && t.getHealth() == 150, "Nightmare leaves a dodge warning");
        h.runAfterDelay(18, () -> {
            h.assertTrue(t.getHealth() < 150 && AuthorMind.state(p).link == null, "Nightmare: health="+t.getHealth()+" tick="+p.tickCount+" pending="+AuthorMind.state(p).pending+" eligible="+Author.eligible(p,t,24,true));
            h.assertTrue(cast(p, "dream_entry") == AbilityWheel.Result.REFUSED, "Dream Entry still requires Insight");
            AuthorMind.add(p, t, 5);
            h.assertTrue(cast(p, "dream_entry") == AbilityWheel.Result.DONE, "Dream Entry can recast after link consumption");
            float before = SoulCore.getSpirituality(p);
            AuthorMind.add(p, t, 3);
            p.setShiftKeyDown(true);
            Author.key(p, p.level(), p.level(), 5, false);
            p.setShiftKeyDown(false);
            h.assertTrue(SoulCore.getSpirituality(p) == before - 2200 && AuthorMind.insight(p, t) == 2,
                    "Sneak L immediately recasts Dream Entry at its normal cost");
            // A second player tests a link that becomes invalid before resolution.
            AuthorMind.add(q, other, 3); cast(q, "dream_entry"); cast(q, "nightmare");
            Friends.add(q, other.getUUID(), "new ally", false);
            h.runAfterDelay(18, () -> {
                h.assertTrue(other.getHealth() == 150, "Delayed Nightmare rechecks friendship");
                Friends.remove(q, other.getUUID());
                h.succeed();
            });
        });
    }

    private static void support(GameTestHelper h) {
        var p = player(h, 6, 1.5); var t = target(h, p, 4, 100);
        AuthorMind.add(p, t, 5);
        h.assertTrue(cast(p, "frenzy") == AbilityWheel.Result.DONE && Falter.chance(t) == .25f, "Frenzy applies attack falter");
        h.assertTrue(cast(p, "suggestion") == AbilityWheel.Result.DONE, "Suggestion arms with Insight");
        p.setHealth(p.getMaxHealth());
        float before = p.getHealth();
        p.hurtServer(p.level(), t.damageSources().mobAttack(t), 10);
        h.assertTrue(p.getHealth() > before - 10, "Suggestion reduces the next attack");
        h.assertTrue(!AuthorMind.state(p).marks.containsKey("suggestion" + t.getUUID()), "Suggestion is consumed once");
        SoulCore.setSpiritualityQuietly(p, 6400);
        h.assertTrue(cast(p, "psychological_invisibility") == AbilityWheel.Result.DONE && Unnoticed.isUnnoticed(p), "Psychological invisibility hides intentions");
        AuthorMind.add(p, t, 5);
        h.assertTrue(cast(p, "psychoanalysis") == AbilityWheel.Result.DONE && !Unnoticed.isUnnoticed(p), "Hostile casting ends concealment");
        Fear.apply(p, t, 100); p.setHealth(5); SoulCore.setSpiritualityQuietly(p, 6400);
        h.assertTrue(cast(p, "placate") == AbilityWheel.Result.DONE && p.getHealth() == 13
                && !Fear.isAfraid(p) && AuthorMind.resolved(p), "Placate heals, cleanses and grants Resolve");
        h.succeed();
    }

    private static void cleanup(GameTestHelper h) {
        var p = player(h, 5, 1.5); floor(h, p); var t = target(h, p, 4, 100);
        AuthorMind.add(p, t, 5); cast(p, "sleep");
        SoulCore.setPathway(p, "none"); Author.tickPlayer(p);
        h.runAfterDelay(18, () -> {
            h.assertTrue(!Slumber.isAsleep(t) && !AuthorMind.STATES.containsKey(p.getUUID()), "Pathway change cancels pending sleep");
            SoulCore.setPathway(p, Author.PATHWAY); SoulCore.setSpiritualityQuietly(p, 15500);
            AuthorMind.add(p, t, 5); cast(p, "awe");
            h.runAfterDelay(8, () -> {
                h.assertTrue(Fear.isAfraid(t), "Awe applies after its tell");
                AuthorMind.clear(p); h.assertTrue(!Fear.isAfraid(t), "Caster cleanup releases owned fear");
                // A visible floor lets the actual Dream Maze cast choose its ground.
                p.setXRot(33); SoulCore.setSpiritualityQuietly(p, 15500);
                h.assertTrue(cast(p, "dream_maze") == AbilityWheel.Result.DONE, "Maze can be placed on visible ground");
                h.runAfterDelay(25, () -> {
                    h.assertTrue(Falter.chance(t) == 0 && AuthorMind.resolved(t), "Resolve protects a recently released target from the maze");
                    t.getPersistentData().remove(AuthorMind.RESOLVE);
                    h.runAfterDelay(6, () -> {
                        h.assertTrue(Falter.chance(t) >= .2f, "Maze affects an eligible enemy after warning");
                        SoulCore.setSequence(p, 9); Author.tickPlayer(p);
                        h.assertTrue(Falter.ticksLeft(t, AuthorMind.token(p, "maze")) == 0, "Sequence change clears maze effects");
                        h.succeed();
                    });
                });
            });
        });
    }
}
