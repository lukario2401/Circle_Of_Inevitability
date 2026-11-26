package net.lukario.circleofinevitability.item.artifacts;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobs;
import net.lukario.circleofinevitability.combat.AbilityCatalog;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.item.ModItems;
import net.lukario.circleofinevitability.item.artifacts.storing.AbilitySlots;
import net.lukario.circleofinevitability.item.artifacts.storing.AggroDrawback;
import net.lukario.circleofinevitability.item.artifacts.storing.ArtifactDrawback;
import net.lukario.circleofinevitability.item.artifacts.storing.Bearer;
import net.lukario.circleofinevitability.item.artifacts.storing.BloodThirstDrawback;
import net.lukario.circleofinevitability.item.artifacts.storing.CorruptionDrawback;
import net.lukario.circleofinevitability.item.artifacts.storing.DisorientationDrawback;
import net.lukario.circleofinevitability.item.artifacts.storing.FeedOrDieDrawback;
import net.lukario.circleofinevitability.item.artifacts.storing.StoredAbilities;
import net.lukario.circleofinevitability.item.artifacts.storing.StoringArtifactItem;
import net.lukario.circleofinevitability.item.artifacts.storing.StoringArtifacts;
import net.lukario.circleofinevitability.menu.AbilityList;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** Real-server checks for Creeping Hunger and the StoringArtifact framework under it. */
public final class CreepingHungerGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, CircleOfInevitability.MOD_ID);
    static {
        FUNCTIONS.register("creeping_hunger_slots", () -> CreepingHungerGameTests::slots);
        FUNCTIONS.register("creeping_hunger_devour_random", () -> CreepingHungerGameTests::devourRandom);
        FUNCTIONS.register("creeping_hunger_devour_choice", () -> CreepingHungerGameTests::devourChoice);
        FUNCTIONS.register("creeping_hunger_cast_and_thirst", () -> CreepingHungerGameTests::castAndThirst);
        FUNCTIONS.register("creeping_hunger_feed_or_die", () -> CreepingHungerGameTests::feedOrDie);
        FUNCTIONS.register("creeping_hunger_corruption", () -> CreepingHungerGameTests::corruption);
        FUNCTIONS.register("creeping_hunger_aggro", () -> CreepingHungerGameTests::aggro);
        FUNCTIONS.register("creeping_hunger_disorientation", () -> CreepingHungerGameTests::disorientation);
        FUNCTIONS.register("creeping_hunger_family_tick", () -> CreepingHungerGameTests::familyTick);
        FUNCTIONS.register("creeping_hunger_multi_pick", () -> CreepingHungerGameTests::multiPick);
        FUNCTIONS.register("creeping_hunger_lists", () -> CreepingHungerGameTests::lists);
    }
    private CreepingHungerGameTests() {}

    private static int ticksRun;

    /** Made when a test runs (never at startup), on a pathway no mob has, so real gameplay never sees it. */
    private static AbilityCatalog.Power power(String key) {
        AbilityWheel.create("creeping_hunger_test_wheel", "Test").pathway("Creeping Hunger Test")
                .entry("weak", "storing_artifact/stored", "Test Weak", 8, 1, 0, (p, sl) -> { ticksRun++; return AbilityWheel.Result.DONE; })
                .entry("strong", "storing_artifact/stored", "Test Strong", 2, 1, 0, (p, sl) -> AbilityWheel.Result.DONE)
                .entry("refused", "storing_artifact/stored", "Test Refused", 8, 1, 0, (p, sl) -> AbilityWheel.Result.REFUSED);
        return AbilityCatalog.all().get("wheel/creeping_hunger_test_wheel/" + key);
    }

    private static StoringArtifactItem tier(int tier) {
        return (StoringArtifactItem) ModItems.CREEPING_HUNGER.get(tier - 1).get();
    }

    private static <T extends ArtifactDrawback> T drawback(Class<T> type) {
        return CreepingHunger.ARTIFACT.drawbacksOf(type).getFirst();
    }

    private static ServerPlayer player(GameTestHelper h, int tier) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "hunger-test-player"), false);
        ServerPlayer p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation()) {
            @Override public boolean isClientAuthoritative() { return false; }
        };
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
        p.setGameMode(GameType.SURVIVAL);
        p.connection.handleAcceptPlayerLoad(new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
        Vec3 at = h.absoluteVec(new Vec3(1.5, 1, 1.5));
        p.setPos(at.x, at.y, at.z);
        p.setNoGravity(true);
        if (tier > 0) p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tier(tier)));
        SoulCore.setCorruption(p, 0);
        h.addCleanup(ok -> p.discard());
        return p;
    }

    private static Bearer bearer(ServerPlayer p) {
        return CreepingHunger.ARTIFACT.bearer(p.level(), p, SealedArtifacts.carried(p, CreepingHunger.ID));
    }

    private static Mob beyonder(GameTestHelper h, ServerPlayer p, int sequence) {
        Mob mob = h.spawn(EntityTypes.HUSK, new BlockPos(1, 1, 2));
        h.assertTrue(BeyonderMobs.make(mob, "Wanderer", sequence), "Test mob becomes a Wanderer Beyonder");
        mob.setNoAi(true);
        mob.setPos(p.getX(), p.getY(), p.getZ() + 1);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        mob.setHealth(100);
        h.addCleanup(ok -> mob.discard());
        return mob;
    }

    private static List<Integer> repeat(int count, int sequence) {
        return new ArrayList<>(Collections.nCopies(count, sequence));
    }

    private static void slots(GameTestHelper h) {
        var t = CreepingHunger.ARTIFACT;
        h.assertTrue(t.tiers() == 5, "Five tiers");
        h.assertTrue(t.slots(1).size() == 5 && t.slots(1).fits(repeat(5, 5)) && !t.slots(1).fits(List.of(4)), "Tier I: five, none above Sequence 5");
        h.assertTrue(t.slots(2).size() == 7 && t.slots(2).fits(List.of(4, 5, 5, 5, 5, 5, 5)) && !t.slots(2).fits(List.of(4, 4)), "Tier II: seven, one Sequence 4");
        List<Integer> third = repeat(6, 5);
        third.addAll(List.of(2, 2, 4));
        h.assertTrue(t.slots(3).size() == 9 && t.slots(3).fits(third) && !t.slots(3).fits(List.of(2, 2, 2)), "Tier III: nine, two Sequence 2");
        h.assertTrue(t.slots(4).size() == 9 && t.slots(4).fits(List.of(1, 1, 1, 5, 5, 5, 5, 5, 5)) && !t.slots(4).fits(List.of(1, 1, 1, 1)) && !t.slots(4).fits(List.of(0)), "Tier IV: nine, three Sequence 1");
        h.assertTrue(t.slots(5).size() == 11 && t.slots(5).fits(repeat(11, 0)) && !t.slots(5).fits(repeat(12, 9)), "Tier V: eleven, any Sequence");
        for (int tier = 1; tier < 5; tier++)
            h.assertTrue(t.slots(tier + 1).fits(t.slots(tier).caps()), "Upgrading from tier " + tier + " keeps everything stored");
        AbilitySlots mixed = t.slots(3);
        h.assertTrue(mixed.strongestAddable(List.of(5)) == 2 && mixed.strongestAddable(List.of(2, 2)) == 4, "Free slots keep the strongest caps open");
        h.succeed();
    }

    private static void devourRandom(GameTestHelper h) {
        ServerPlayer p = player(h, 1);
        ItemStack glove = p.getMainHandItem();
        for (int i = 0; i < 8; i++) StoringArtifacts.killed(p.level(), p, beyonder(h, p, 3));
        List<AbilityCatalog.Power> inside = StoredAbilities.powers(glove);
        h.assertTrue(!inside.isEmpty() && inside.size() <= 5, "Tier I devours abilities, at most five: " + inside.size());
        h.assertTrue(inside.stream().allMatch(power -> power.sequence() >= 5 && power.cast().pathway().equals("Wanderer")),
                "Tier I never takes anything above Sequence 5");
        h.assertTrue(inside.stream().map(AbilityCatalog.Power::id).distinct().count() == inside.size(), "No ability is stored twice");
        int before = inside.size();
        Mob cow = h.spawn(EntityTypes.COW, new BlockPos(1, 1, 2));
        StoringArtifacts.killed(p.level(), p, cow);
        h.assertTrue(StoredAbilities.powers(glove).size() == before, "Ordinary mobs feed it but give no ability");
        h.succeed();
    }

    private static void devourChoice(GameTestHelper h) {
        ServerPlayer p = player(h, 3);
        ItemStack glove = p.getMainHandItem();
        StoringArtifacts.killed(p.level(), p, beyonder(h, p, 7));
        h.assertTrue(StoredAbilities.ids(glove).isEmpty(), "Tier III waits for the bearer's choice");
        h.assertTrue(AbilityList.openFor(p) != null && AbilityList.openFor(p).maxPicks() == 1, "Tier III picks one, from a list");
        AbilityList.handleChoice(p, CreepingHunger.ID + "_devour", List.of(0));
        h.assertTrue(StoredAbilities.powers(glove).size() == 1, "The chosen ability is devoured");
        // A full glove: the choice leads to the replace list.
        List<String> full = new ArrayList<>();
        AbilityCatalog.all().values().stream().filter(pw -> pw.sequence() >= 5 && pw.cast().copyable()
                && !pw.cast().pathway().equals("Wanderer")).limit(9).forEach(pw -> full.add(pw.id()));
        StoredAbilities.set(glove, full);
        h.assertTrue(StoredAbilities.powers(glove).size() == 9, "Filled for the test");
        AbilityCatalog.Power weak = power("weak");
        h.assertTrue(StoredAbilities.add(glove, tier(3).slots(), weak) == StoredAbilities.Result.FULL, "A full glove refuses");
        h.assertTrue(StoredAbilities.replace(glove, tier(3).slots(), 0, weak) && StoredAbilities.contains(glove, weak.id()),
                "Replacing makes room");
        h.assertTrue(StoredAbilities.selectedPower(glove).id().equals(weak.id()), "What was just devoured is selected");
        h.succeed();
    }

    private static void castAndThirst(GameTestHelper h) {
        ServerPlayer p = player(h, 3);
        ItemStack glove = p.getMainHandItem();
        StoringArtifactItem item = tier(3);
        h.assertTrue(item.use(p.level(), p, InteractionHand.MAIN_HAND) == InteractionResult.FAIL, "Empty glove refuses");
        h.assertTrue(StoredAbilities.add(glove, item.slots(), power("refused")) == StoredAbilities.Result.ADDED, "Refusing entry stored");
        h.assertTrue(item.use(p.level(), p, InteractionHand.MAIN_HAND) == InteractionResult.FAIL && item.cooldownLeft(p) == 0,
                "A cast that does not go off costs nothing");
        h.assertTrue(StoredAbilities.add(glove, item.slots(), power("weak")) == StoredAbilities.Result.ADDED, "Weak entry stored and selected");
        int before = ticksRun;
        h.assertTrue(SoulCore.getPathway(p).equals("none") || SoulCore.getPathway(p).isEmpty(), "Bearer has no pathway");
        h.assertTrue(item.use(p.level(), p, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS && ticksRun == before + 1,
                "The stored ability casts without a pathway");
        h.assertTrue(item.cooldownLeft(p) == CreepingHunger.ARTIFACT.cooldownFor(8), "Cooldown follows the ability's Sequence");
        h.assertTrue(SoulCore.getCorruption(p) == 3, "A tier III cast corrupts by 3: " + SoulCore.getCorruption(p));
        BloodThirstDrawback thirst = drawback(BloodThirstDrawback.class);
        h.assertTrue(thirst.thirsty(bearer(p)) && !thirst.parched(bearer(p)), "Casting at tier III makes the bearer thirst");
        CreepingHunger.ARTIFACT.bearer(p.level(), p, glove).state().putLong(BloodThirstDrawback.UNTIL, p.level().getGameTime() - 1);
        item.state(p).putLong("ready_at", 0);
        SealedArtifacts.tickCarried(p.level(), p);
        h.assertTrue(p.hasEffect(MobEffects.WEAKNESS) && p.hasEffect(MobEffects.HUNGER), "Parched bearers wither");
        h.assertTrue(item.use(p.level(), p, InteractionHand.MAIN_HAND) == InteractionResult.FAIL && ticksRun == before + 1,
                "Parched bearers cannot cast");
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.MOB_BLOOD_VIAL.get()));
        h.assertTrue(ModItems.MOB_BLOOD_VIAL.get().use(p.level(), p, InteractionHand.OFF_HAND) == InteractionResult.SUCCESS,
                "Mob blood can be drunk while thirsty");
        h.assertTrue(p.getOffhandItem().is(Items.GLASS_BOTTLE) && !thirst.thirsty(bearer(p)), "Drinking quenches and returns the bottle");
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.MOB_BLOOD_VIAL.get()));
        h.assertTrue(ModItems.MOB_BLOOD_VIAL.get().use(p.level(), p, InteractionHand.OFF_HAND) == InteractionResult.PASS
                && p.getOffhandItem().is(ModItems.MOB_BLOOD_VIAL.get()), "No thirst: the vial is not wasted");
        // Tier I and II have no blood thirst.
        ServerPlayer low = player(h, 2);
        StoredAbilities.add(low.getMainHandItem(), tier(2).slots(), power("weak"));
        h.assertTrue(tier(2).use(low.level(), low, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS
                && !thirst.thirsty(bearer(low)), "Tier II casts without thirst");
        h.assertTrue(StoredAbilities.add(low.getMainHandItem(), tier(2).slots(), power("strong")) == StoredAbilities.Result.TOO_STRONG,
                "Tier II cannot hold a Sequence 2 ability");
        h.succeed();
    }

    private static void feedOrDie(GameTestHelper h) {
        ServerPlayer p = player(h, 1);
        FeedOrDieDrawback hunger = drawback(FeedOrDieDrawback.class);
        SealedArtifacts.tickCarried(p.level(), p);
        h.assertTrue(hunger.hunger(bearer(p)) == 20, "Carrying it makes it hungry");
        ItemStack glove = p.getMainHandItem();
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        SealedArtifacts.tickCarried(p.level(), p);
        h.assertTrue(SealedArtifactItem.state(p, CreepingHunger.ID).getIntOr(FeedOrDieDrawback.HUNGER, 0) == 20, "Put away, the hunger pauses");
        p.setItemInHand(InteractionHand.MAIN_HAND, glove);
        SealedArtifactItem.state(p, CreepingHunger.ID).putInt(FeedOrDieDrawback.HUNGER, hunger.periodTicks() * 9 / 10);
        StoringArtifacts.killed(p.level(), p, h.spawn(EntityTypes.CHICKEN, new BlockPos(1, 1, 1)));
        h.assertTrue(hunger.hunger(bearer(p)) == 0, "Killing a mob feeds it");
        SealedArtifactItem.state(p, CreepingHunger.ID).putInt(FeedOrDieDrawback.HUNGER, hunger.periodTicks() - 20);
        SealedArtifacts.tickCarried(p.level(), p);
        h.assertTrue(p.isDeadOrDying(), "A day unfed: it kills its bearer");
        h.assertTrue(SealedArtifactItem.state(p, CreepingHunger.ID).getIntOr(FeedOrDieDrawback.HUNGER, -1) == 0, "And the clock starts over");
        ServerPlayer creative = player(h, 5);
        creative.setGameMode(GameType.CREATIVE);
        SealedArtifacts.tickCarried(creative.level(), creative);
        h.assertTrue(hunger.hunger(bearer(creative)) == 0, "Creative players are spared");
        h.succeed();
    }

    private static void corruption(GameTestHelper h) {
        ServerPlayer p = player(h, 1);
        CorruptionDrawback corruption = drawback(CorruptionDrawback.class);
        var state = SealedArtifactItem.state(p, CreepingHunger.ID);
        state.putInt("corruption_clock", 59);
        SealedArtifacts.tickCarried(p.level(), p);
        h.assertTrue(SoulCore.getCorruption(p) == 1 && corruption.share(bearer(p)) == 1, "Carrying tier I corrupts once a minute");
        Spells.cleanse(p);
        h.assertTrue(SoulCore.getCorruption(p) == 0 && corruption.share(bearer(p)) == 0, "Cleansing removes it");
        corruption.add(bearer(p), 10);
        SoulCore.setCorruption(p, SoulCore.getCorruption(p) + 5);   // from somewhere else
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        SealedArtifacts.tickCarried(p.level(), p);
        h.assertTrue(SoulCore.getCorruption(p) == 13 && corruption.share(bearer(p)) == 8, "Put away, its own share drains");
        Spells.cleanse(p);
        h.assertTrue(SoulCore.getCorruption(p) == 5, "Only its own share is cleansed: " + SoulCore.getCorruption(p));
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tier(1)));
        SoulCore.setCorruption(p, 40);
        corruption.add(bearer(p), 5);
        h.assertTrue(SoulCore.getCorruption(p) == 40, "Tier I never corrupts past 40");
        h.succeed();
    }

    private static void aggro(GameTestHelper h) {
        AggroDrawback aggro = drawback(AggroDrawback.class);
        ServerPlayer p = player(h, 1);
        Mob zombie = h.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 1, 2));
        Mob cow = h.spawn(EntityTypes.COW, new BlockPos(2, 1, 2));
        h.assertTrue(!aggro.appliesAt(1), "Tier I does not draw mobs");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tier(2)));
        aggro.pulse(bearer(p));
        h.assertTrue(zombie.getTarget() == p && cow.getTarget() != p, "Tier II: hostile mobs hunt the bearer, neutral ones do not");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tier(4)));
        aggro.pulse(bearer(p));
        h.assertTrue(cow.getTarget() == p, "Tier IV: neutral mobs join in");
        h.succeed();
    }

    private static void disorientation(GameTestHelper h) {
        DisorientationDrawback fits = drawback(DisorientationDrawback.class);
        h.assertTrue(!fits.appliesAt(3) && fits.appliesAt(4), "Disorientation starts at tier IV");
        ServerPlayer p = player(h, 4);
        fits.fit(bearer(p));
        h.assertTrue(p.hasEffect(MobEffects.NAUSEA) && p.hasEffect(MobEffects.SLOWNESS) && !p.hasEffect(MobEffects.DARKNESS), "Tier IV fit");
        ServerPlayer q = player(h, 5);
        fits.fit(bearer(q));
        h.assertTrue(q.hasEffect(MobEffects.DARKNESS), "Tier V fits darken the world");
        h.assertTrue(CreepingHunger.ARTIFACT.drawbacksAt(1).size() == 2 && CreepingHunger.ARTIFACT.drawbacksAt(2).size() == 3
                && CreepingHunger.ARTIFACT.drawbacksAt(3).size() == 4 && CreepingHunger.ARTIFACT.drawbacksAt(5).size() == 5,
                "Each tier adds one downside");
        h.succeed();
    }

    private static void familyTick(GameTestHelper h) {
        ServerPlayer p = player(h, 1);
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(tier(5)));
        h.assertTrue(((StoringArtifactItem) SealedArtifacts.carried(p, CreepingHunger.ID).getItem()).tier() == 5, "The strongest carried tier counts");
        SoulCore.setCorruption(p, 50);
        SealedArtifactItem.state(p, CreepingHunger.ID).putInt("corruption_clock", 11);
        SealedArtifacts.tickCarried(p.level(), p);
        h.assertTrue(SoulCore.getCorruption(p) == 51, "One tick, at tier V's rate and cap: " + SoulCore.getCorruption(p));
        h.assertTrue(drawback(FeedOrDieDrawback.class).hunger(bearer(p)) == 20, "Two tiers carried still hunger once");
        h.succeed();
    }

    private static void multiPick(GameTestHelper h) {
        ServerPlayer four = player(h, 4);
        StoringArtifacts.killed(four.level(), four, beyonder(h, four, 7));
        String devour = CreepingHunger.ID + "_devour";
        h.assertTrue(AbilityList.openFor(four).maxPicks() == 2, "Tier IV may pick two from one kill");
        h.assertTrue(!AbilityList.handleChoice(four, devour, List.of(0, 1, 2)), "Three picks are refused at tier IV");
        h.assertTrue(!AbilityList.handleChoice(four, devour, List.of(0, 0)), "The same ability cannot be picked twice");
        h.assertTrue(AbilityList.handleChoice(four, devour, List.of(0, 1)) && StoredAbilities.powers(four.getMainHandItem()).size() == 2,
                "Two picks devour two abilities");
        ServerPlayer five = player(h, 5);
        StoringArtifacts.killed(five.level(), five, beyonder(h, five, 7));
        h.assertTrue(AbilityList.openFor(five).maxPicks() == 3, "Tier V may pick three");
        h.assertTrue(AbilityList.handleChoice(five, devour, List.of(2, 0, 1)) && StoredAbilities.powers(five.getMainHandItem()).size() == 3,
                "Three picks devour three abilities");
        // A full glove: picks that need room ask which ability to replace, one after another.
        ServerPlayer full = player(h, 4);
        ItemStack glove = full.getMainHandItem();
        List<String> ids = new ArrayList<>();
        AbilityCatalog.all().values().stream().filter(pw -> pw.sequence() >= 5 && pw.cast().copyable()
                && !pw.cast().pathway().equals("Wanderer")).limit(9).forEach(pw -> ids.add(pw.id()));
        StoredAbilities.set(glove, ids);
        StoringArtifacts.killed(full.level(), full, beyonder(h, full, 7));
        h.assertTrue(AbilityList.handleChoice(full, devour, List.of(0, 1)), "Picks are accepted when full");
        h.assertTrue(AbilityList.openFor(full) != null && AbilityList.openFor(full).id().equals(CreepingHunger.ID + "_replace"),
                "The first needs room: which one does it replace?");
        AbilityList.handleChoice(full, CreepingHunger.ID + "_replace", List.of(0));
        h.assertTrue(AbilityList.openFor(full) != null && AbilityList.openFor(full).id().equals(CreepingHunger.ID + "_replace"),
                "Then the second");
        AbilityList.handleChoice(full, CreepingHunger.ID + "_replace", List.of());
        List<String> after = StoredAbilities.ids(glove);
        h.assertTrue(after.size() == 9 && !after.contains(ids.get(0)) && after.contains(ids.get(1)),
                "One replaced, the second let go: " + after);
        h.succeed();
    }

    private static void lists(GameTestHelper h) {
        ServerPlayer p = player(h, 5);
        ItemStack glove = p.getMainHandItem();
        StoredAbilities.add(glove, tier(5).slots(), power("weak"));
        StoredAbilities.add(glove, tier(5).slots(), power("strong"));
        p.setShiftKeyDown(true);
        h.assertTrue(tier(5).use(p.level(), p, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS, "Sneak-use opens the list");
        AbilityList select = AbilityList.openFor(p);
        h.assertTrue(select != null && select.rows().size() == 2 + 9 + 1, "Two stored, nine empty slots and Discard: " + (select == null ? 0 : select.rows().size()));
        h.assertTrue(select.rows().get(1).marked() && select.rows().get(2).locked(), "The selection is marked, empty slots locked");
        AbilityList.handleChoice(p, CreepingHunger.ID + "_select", List.of(0));
        h.assertTrue(StoredAbilities.selectedPower(glove).id().equals(power("weak").id()), "Picking a row selects it");
        h.assertTrue(item5Cooldown(p) == 0, "Selecting costs no cooldown");
        tier(5).use(p.level(), p, InteractionHand.MAIN_HAND);
        AbilityList.handleChoice(p, CreepingHunger.ID + "_select", List.of(AbilityList.openFor(p).rows().size() - 1));
        h.assertTrue(AbilityList.openFor(p).id().equals(CreepingHunger.ID + "_discard") && AbilityList.openFor(p).maxPicks() == 2,
                "Discard can let go of several at once");
        AbilityList.handleChoice(p, CreepingHunger.ID + "_discard", List.of(0, 1));
        h.assertTrue(StoredAbilities.ids(glove).isEmpty(), "Both discarded");
        h.succeed();
    }

    private static int item5Cooldown(ServerPlayer p) {
        return tier(5).cooldownLeft(p);
    }
}
