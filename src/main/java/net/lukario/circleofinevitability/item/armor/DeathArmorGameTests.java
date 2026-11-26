package net.lukario.circleofinevitability.item.armor;

import com.mojang.authlib.GameProfile;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.DragUnder;
import net.lukario.circleofinevitability.combat.FollowerSummon;
import net.lukario.circleofinevitability.item.ModItems;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Burial;
import net.lukario.circleofinevitability.status.KillToll;
import net.lukario.circleofinevitability.status.Marks;
import net.lukario.circleofinevitability.status.MaxHealthDrain;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Vestments of Death: smithing, Reaping marks by distance, the underworld's claim, the curse, the toll and the full set. */
public final class DeathArmorGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, CircleOfInevitability.MOD_ID);
    static {
        FUNCTIONS.register("death_armor_equipment", () -> DeathArmorGameTests::equipment);
        FUNCTIONS.register("death_armor_reaping", () -> DeathArmorGameTests::reaping);
        FUNCTIONS.register("death_armor_any_piece", () -> DeathArmorGameTests::anyPiece);
        FUNCTIONS.register("death_armor_spirit_guide", () -> DeathArmorGameTests::spiritGuide);
        FUNCTIONS.register("death_armor_claim", () -> DeathArmorGameTests::claim);
        FUNCTIONS.register("death_armor_curse", () -> DeathArmorGameTests::curse);
        FUNCTIONS.register("death_armor_toll", () -> DeathArmorGameTests::toll);
        FUNCTIONS.register("death_armor_full_set", () -> DeathArmorGameTests::fullSet);
    }

    private static ServerPlayer player(GameTestHelper h, EquipmentSlot slot) {
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "Death" + UUID.randomUUID().toString().substring(0, 6)), false);
        ServerPlayer p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
        p.setGameMode(GameType.SURVIVAL);
        p.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        Vec3 at = h.absoluteVec(new Vec3(4.5, 2, 4.5));
        p.setPos(at.x, at.y, at.z);
        p.setNoGravity(true);
        if (slot != null) p.setItemSlot(slot, new ItemStack(item(slot)));
        // Embedded test connections do not drive the player's tick; doTick runs Forge's living tick hook.
        h.onEachTick(p::doTick);
        h.addCleanup(ok -> {
            FollowerSummon.dismiss(p, DeathArmor.GUIDE);
            MaxHealthDrain.clear(p, DeathArmor.EBB);
            DeathArmor.TOLL.forget(p);
            h.getLevel().getServer().getPlayerList().remove(p);
        });
        return p;
    }

    private static net.minecraft.world.item.Item item(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> ModItems.DEATH_HELMET.get();
            case CHEST -> ModItems.DEATH_CHESTPLATE.get();
            case LEGS -> ModItems.DEATH_LEGGINGS.get();
            default -> ModItems.DEATH_BOOTS.get();
        };
    }

    private static void equipSet(ServerPlayer p) {
        for (var slot : InevitabilityArmor.SLOTS) p.setItemSlot(slot, new ItemStack(item(slot)));
    }

    private static Mob zombie(GameTestHelper h, ServerPlayer p, double distance, float health) {
        Mob m = h.spawnWithNoFreeWill(EntityTypes.HUSK, 1, 2, 1);
        m.setNoGravity(true);
        m.getPersistentData().putBoolean("circle_of_inevitability_beyonder_rolled", true);
        net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobs.clear(m);
        m.setPos(p.getX() + distance, p.getY(), p.getZ());
        m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        m.setHealth(health);
        h.addCleanup(ok -> m.discard());
        return m;
    }

    private static void equipment(GameTestHelper h) {
        var p = player(h, EquipmentSlot.CHEST);
        h.assertTrue(DeathArmor.pieces(p) == 1 && !DeathArmor.fullSet(p), "The Mantle alone is not the full set");
        equipSet(p);
        h.assertTrue(DeathArmor.fullSet(p) && !GrayFogArmor.fullSet(p) && !InevitabilityArmor.fullSet(p), "Four pieces activate only this set");
        h.assertTrue(DeathArmor.reach(4) > DeathArmor.reach(1), "Every piece widens the Reaping");
        for (var slot : InevitabilityArmor.SLOTS) {
            ItemStack stack = p.getItemBySlot(slot);
            h.assertTrue(stack.get(DataComponents.EQUIPPABLE).slot() == slot, "Correct equipment slot: " + slot);
            h.assertTrue(stack.has(DataComponents.ENCHANTABLE) && stack.has(DataComponents.REPAIRABLE), "Vanilla enchanting and repair components");
        }
        var bases = List.of(Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS);
        for (int i = 0; i < 4; i++) {
            ItemStack base = new ItemStack(bases.get(i));
            base.setDamageValue(9);
            var input = new net.minecraft.world.item.crafting.SmithingRecipeInput(
                    new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), base, new ItemStack(ModItems.DEATH_CORE.get()));
            var recipe = h.getLevel().getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMITHING, input, h.getLevel()).orElseThrow();
            ItemStack result = recipe.value().assemble(input);
            h.assertTrue(result.is(item(InevitabilityArmor.SLOTS[i])) && result.getDamageValue() == 9, "Smithing creates Death armor and keeps damage");
        }
        h.succeed();
    }

    /** Same damage at any distance, more marks up close, nothing for a mob out of reach; no Mantle needed. */
    private static void reaping(GameTestHelper h) {
        var p = player(h, EquipmentSlot.HEAD);
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(item(EquipmentSlot.FEET)));
        var near = zombie(h, p, 1.5, 100);
        var far = zombie(h, p, 5.5, 100);
        var outside = zombie(h, p, -7.5, 100);
        h.assertTrue(DeathArmor.reach(2) > 5.5 && DeathArmor.reach(2) < 7.5, "Two pieces reach the far mob, not the outside one");
        h.runAfterDelay(DeathArmor.pulseTicks(2) + 2, () -> {
            h.assertTrue(near.getHealth() < 100 && Math.abs(near.getHealth() - far.getHealth()) < .01f,
                    "Near and far take the same damage: " + near.getHealth() + " / " + far.getHealth());
            h.assertTrue(outside.getHealth() == 100, "Out of reach is spared");
            int nearMarks = Marks.count(near, p, DeathArmor.MARK), farMarks = Marks.count(far, p, DeathArmor.MARK);
            // One or two pulses may have landed by now, depending on where the wearer's pulse clock stood.
            h.assertTrue(farMarks >= 1 && nearMarks == 3 * farMarks, "Closer means more marks: " + nearMarks + " / " + farMarks);
            for (var slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.FEET))
                h.assertTrue(p.getItemBySlot(slot).getOrDefault(DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.EMPTY).copyTag().contains(DeathArmor.REAP_KEY), "Every worn piece shows the pulse");
            h.succeed();
        });
    }

    /** A single piece of any kind pulses, and every piece added makes the pulse stronger. */
    private static void anyPiece(GameTestHelper h) {
        for (int pieces = 1; pieces < 4; pieces++) {
            h.assertTrue(DeathArmor.reach(pieces + 1) > DeathArmor.reach(pieces), "More pieces reach further");
            h.assertTrue(DeathArmor.pulseDamage(pieces + 1) > DeathArmor.pulseDamage(pieces), "More pieces hit harder");
            h.assertTrue(DeathArmor.pulseTicks(pieces + 1) < DeathArmor.pulseTicks(pieces), "More pieces pulse sooner");
        }
        var p = player(h, EquipmentSlot.FEET);
        var husk = zombie(h, p, 2, 100);
        h.runAfterDelay(DeathArmor.pulseTicks(1) + 2, () -> {
            float lost = 100 - husk.getHealth();
            // At most two pulses fit in the wait; a husk's own armor softens each a little.
            h.assertTrue(lost > 0 && lost <= 2 * DeathArmor.pulseDamage(1) + .01f && Marks.count(husk, p, DeathArmor.MARK) > 0,
                    "The boots alone reap and mark, for one piece's damage: lost " + lost);
            h.succeed();
        });
    }

    /** The full set's Spirit Guide hurls soul skulls at hostiles (marking them) and leaves passive mobs alone. */
    private static void spiritGuide(GameTestHelper h) {
        var p = player(h, null);
        equipSet(p);
        // Hold the Reaping pulse back so whatever the husk loses is the guide's.
        Root.apply(p, 400, RootRestriction.ABILITIES);
        var husk = zombie(h, p, 4, 200);
        Mob pig = h.spawnWithNoFreeWill(EntityTypes.PIG, 1, 2, 1);
        pig.setNoGravity(true);
        pig.setPos(p.getX() - 2, p.getY(), p.getZ());
        h.addCleanup(ok -> pig.discard());
        h.runAfterDelay(DeathArmor.SPIRIT_GUIDE.cooldownTicks() + 10, () -> {
            h.assertTrue(FollowerSummon.isActive(p, DeathArmor.GUIDE), "The guide is out");
            h.assertTrue(husk.getHealth() <= 200 - DeathArmor.SPIRIT_GUIDE.damage() + .01f, "The guide strikes the hostile: " + husk.getHealth());
            h.assertTrue(Marks.count(husk, p, DeathArmor.MARK) >= 2, "Its soul skulls mark");
            h.assertTrue(pig.getHealth() == pig.getMaxHealth(), "Passive mobs are left alone");
            h.succeed();
        });
    }

    /** Past the limit it is dragged under, crushed, and thrown back out if it lives. */
    private static void claim(GameTestHelper h) {
        var p = player(h, null);
        var survivor = zombie(h, p, 3, 200);
        survivor.setNoGravity(false);
        double startY = survivor.getY();
        h.assertTrue(DeathArmor.mark(p, survivor, DeathArmor.MARK_LIMIT) == DeathArmor.MARK_LIMIT, "At the limit it is only marked");
        h.assertTrue(DeathArmor.mark(p, survivor, 1) == 0 && DragUnder.isDragging(survivor), "One more and the underworld takes it");
        h.assertTrue(Marks.count(survivor, p, DeathArmor.MARK) == 0, "Its marks are spent");
        h.runAfterDelay(DragUnder.SEIZE + 16, () -> h.assertTrue(Burial.isBuried(survivor) && survivor.getY() < startY - 1,
                "It is under the ground"));
        int done = DeathArmor.CLAIM.totalTicks();
        h.runAfterDelay(done + 1, () -> {
            h.assertTrue(survivor.isAlive() && survivor.getHealth() <= 200 - DeathArmor.CLAIM.damage() + .01f,
                    "It took the full crushing: " + survivor.getHealth());
            h.assertTrue(!Burial.isBuried(survivor) && !DragUnder.isDragging(survivor), "And is back on the surface");
            h.assertTrue(survivor.getDeltaMovement().y > .3 || survivor.getY() > startY + .3, "Thrown out of the ground");
            h.succeed();
        });
    }

    /** Hunger while worn, worse with every piece; a pang can hurt but never take the last half heart. */
    private static void curse(GameTestHelper h) {
        for (int pieces = 1; pieces < 4; pieces++) {
            h.assertTrue(DeathArmor.hungerAmplifier(pieces + 1) >= DeathArmor.hungerAmplifier(pieces), "Hunger never eases with more pieces");
            h.assertTrue(DeathArmor.pangChance(pieces + 1) > DeathArmor.pangChance(pieces), "More pieces, more pangs");
            h.assertTrue(DeathArmor.pangMax(pieces + 1) > DeathArmor.pangMax(pieces), "More pieces, harder pangs");
            h.assertTrue(DeathArmor.tollTicks(pieces + 1) < DeathArmor.tollTicks(pieces), "More pieces, a hungrier toll");
        }
        h.assertTrue(DeathArmor.hungerAmplifier(4) > DeathArmor.hungerAmplifier(1), "The full set starves you worse than one piece");
        var p = player(h, EquipmentSlot.FEET);
        var full = player(h, null);
        equipSet(full);
        h.runAfterDelay(2, () -> {
            h.assertTrue(p.hasEffect(MobEffects.HUNGER), "Any piece brings endless hunger");
            h.assertTrue(p.getEffect(MobEffects.HUNGER).getAmplifier() == DeathArmor.hungerAmplifier(1)
                    && full.getEffect(MobEffects.HUNGER).getAmplifier() == DeathArmor.hungerAmplifier(4), "Hunger by pieces worn");
            p.setHealth(20);
            h.assertTrue(DeathArmor.pang(p, h.getLevel(), DeathArmor.pangMax(4)) && p.getHealth() < 20, "A pang hurts");
            p.setHealth(2.5f);
            for (int i = 0; i < 10; i++) DeathArmor.pang(p, h.getLevel(), DeathArmor.pangMax(4));
            h.assertTrue(p.isAlive() && p.getHealth() >= 1 - 1e-3, "Pangs never kill: " + p.getHealth());
            p.setHealth(1);
            h.assertTrue(!DeathArmor.pang(p, h.getLevel(), DeathArmor.pangMax(4)), "At half a heart there is nothing to take");
            h.succeed();
        });
    }

    /** The clock runs only while worn, a kill feeds it, and when it runs out the wearer is taken. */
    private static void toll(GameTestHelper h) {
        int[] expired = {0}, warned = {0};
        KillToll test = KillToll.of("death_armor_test_toll", 10).warnAt(4).onWarning((b, left) -> warned[0]++)
                .onExpired(b -> expired[0]++);
        var p = player(h, EquipmentSlot.HEAD);
        h.addCleanup(ok -> test.forget(p));
        for (int i = 0; i < 6; i++) test.tick(p);
        h.assertTrue(test.ticksLeft(p) == 4 && warned[0] == 1, "Counts down and warns: " + test.ticksLeft(p));
        for (int i = 0; i < 4; i++) test.tick(p);
        h.assertTrue(expired[0] == 1 && test.ticksLeft(p) == 10, "Expires once and starts again full");
        int[] fastWarned = {0};
        KillToll fast = KillToll.of("death_armor_test_fast_toll", 20).warnAt(5).onWarning((b, left) -> fastWarned[0]++);
        h.addCleanup(ok -> fast.forget(p));
        for (int i = 0; i < 5; i++) fast.tick(p, 2);
        h.assertTrue(fast.ticksLeft(p) == 10 && fast.ticksLeft(p, 2) == 5 && fastWarned[0] == 1,
                "A clock at rate 2 runs twice as fast and warns in real time: " + fast.ticksLeft(p) + ", " + fastWarned[0]);
        var prey = zombie(h, p, 2, 4);
        h.runAfterDelay(3, () -> {
            int before = DeathArmor.TOLL.ticksLeft(p);
            h.assertTrue(DeathArmor.TOLL.binds(p) && before < DeathArmor.TOLL_TICKS, "The worn Visage's toll is running");
            prey.hurtServer(h.getLevel(), p.damageSources().playerAttack(p), 100);
            h.assertTrue(!prey.isAlive() && DeathArmor.TOLL.ticksLeft(p) == DeathArmor.TOLL_TICKS, "A kill feeds it");
            p.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        });
        h.runAfterDelay(8, () -> {
            int paused = DeathArmor.TOLL.ticksLeft(p);
            h.assertTrue(!DeathArmor.TOLL.binds(p), "Taken off, it no longer binds");
            h.runAfterDelay(5, () -> {
                h.assertTrue(DeathArmor.TOLL.ticksLeft(p) == paused, "Taking it off pauses the clock rather than resetting it");
                DeathArmor.takeWearer(p);
                h.assertTrue(!p.isAlive(), "An unpaid toll takes the wearer");
                h.succeed();
            });
        });
    }

    /** The Spirit Guide comes with the full set; life ebbs, and comes back with a piece off; at the end, death. */
    private static void fullSet(GameTestHelper h) {
        var p = player(h, null);
        equipSet(p);
        h.runAfterDelay(2, () -> {
            h.assertTrue(FollowerSummon.isActive(p, DeathArmor.GUIDE), "The full set summons the Spirit Guide");
            for (int i = 0; i < 4; i++) DeathArmor.ebbStep(p, h.getLevel());
            h.assertTrue(Math.abs(p.getMaxHealth() - (20 - 4 * DeathArmor.EBB_STEP)) < 1e-3, "Max health ebbs: " + p.getMaxHealth());
            p.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        });
        h.runAfterDelay(4, () -> h.assertTrue(!FollowerSummon.isActive(p, DeathArmor.GUIDE), "Without the full set the guide leaves"));
        h.runAfterDelay(DeathArmor.RECOVER_TICKS + 8, () -> {
            h.assertTrue(p.getMaxHealth() > 20 - 4 * DeathArmor.EBB_STEP && MaxHealthDrain.drained(p, DeathArmor.EBB) > 0,
                    "It slowly comes back: " + p.getMaxHealth());
            MaxHealthDrain.drain(p, DeathArmor.EBB, 100);
            h.assertTrue(Math.abs(p.getMaxHealth() - MaxHealthDrain.FLOOR) < 1e-3 && MaxHealthDrain.wouldEmpty(p, DeathArmor.EBB_STEP),
                    "A drain never goes below its floor by itself");
            DeathArmor.ebbStep(p, h.getLevel());
            h.assertTrue(!p.isAlive(), "With nothing left to give, the armor takes its wearer");
            h.succeed();
        });
    }
}
