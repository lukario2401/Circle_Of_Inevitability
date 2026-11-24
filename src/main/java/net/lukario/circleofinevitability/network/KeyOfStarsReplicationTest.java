package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStars;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStarsReplication;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.omniscient_eye.Codex;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobs;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobCasting;
import net.lukario.circleofinevitability.combat.AbilityCatalog;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.menu.AbilityMenu;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.Vec3;

final class KeyOfStarsReplicationTest {
    private KeyOfStarsReplicationTest() {}

    static void run(GameTestHelper test) {
        var level = test.getLevel();
        Vec3 origin = test.absoluteVec(new Vec3(1.5, 12, 1.5));
        var player = AbilityComboGameTest.player(test, origin);
        var mob = AbilityComboGameTest.target(test, origin.add(0, 0, 4));
        SoulCore.setPathway(player, KeyOfStars.PATHWAY);
        SoulCore.setSequence(player, 2);
        SoulCore.setSpiritualityQuietly(player, 78000);
        BeyonderMobs.make(mob, KeyOfStars.PATHWAY, 2);
        var observerMob = AbilityComboGameTest.target(test, origin.add(1, 0, 0));
        BeyonderMobs.make(observerMob, KeyOfStars.PATHWAY, 2);
        observerMob.setYRot(0);
        observerMob.setYHeadRot(0);
        var memory = KeyOfStarsReplication.MEMORY;
        String id = "wheel/key_of_stars_starlight/toy_box";

        var toyBox = MobAbilities.byId("Key Of Stars/6/toy_box");
        test.assertTrue(MobCasting.forceCast(mob, toyBox, player, level) == MobCasting.Refusal.NONE, "Mob cast Toy Box");
        test.assertTrue(toyBox == MobAbilities.byId("Key Of Stars/7/toy_box"), "Old Starlight pages still resolve");
        var cast = Casts.last(mob);
        test.assertTrue(AbilityCatalog.id(cast).equals(id), "Witnesses see the exact wheel entry");
        double first = memory.progress(player, id);
        test.assertTrue(first > 0 && first < 100, "First sight adds partial study");
        player.setYRot(180);
        Casts.mobCast(mob, level, cast);
        test.assertTrue(memory.progress(player, id) == first, "Looking away earns no study");
        player.setYRot(0);
        Casts.mobCast(mob, level, cast);
        test.assertTrue(memory.progress(player, id) == first * 2, "Repeated sights build on prior study");
        Codex.choice(player, id, false);
        AbilityComboGameTest.tap(player, 7);
        test.assertTrue(memory.selected(player).isEmpty() && SoulCore.getSpirituality(player) == 78000,
                "An incomplete memory cannot be selected or cast");

        for (int i = 2; i < Math.ceil(100 / first); i++) Casts.mobCast(mob, level, cast);
        test.assertTrue(memory.complete(observerMob, toyBox.storageId()), "A mob also memorizes witnessed casts");
        mob.discard();
        test.assertTrue(MobCasting.forceCast(observerMob, MobAbilities.byId("Key Of Stars/7"), player, level)
                == MobCasting.Refusal.NONE, "A mob replicates a mastered power after the source leaves");
        player.setShiftKeyDown(true);
        AbilityComboGameTest.tap(player, 7);
        Codex.choice(player, id, true);
        Codex.choice(player, id, false);
        player.setShiftKeyDown(false);
        test.assertTrue(memory.selected(player).equals(id) && memory.favorite(player, id) && Codex.study(player, id) == 0,
                "Selection and favorite belong to the Planeswalker's library");
        var saved = player.getPersistentData().getCompoundOrEmpty("circle_of_inevitability").copy();
        player.getPersistentData().put("circle_of_inevitability", saved);
        SoulCore.setSequence(player, 3);
        AbilityComboGameTest.tap(player, 7);
        test.assertTrue(Casts.last(player) == null, "Dropping below Planeswalker disables Replication");
        SoulCore.setSequence(player, 2);
        SoulCore.setSpiritualityQuietly(player, 14000);
        AbilityComboGameTest.tap(player, 7);
        test.assertTrue(SoulCore.getSpirituality(player) == 14000 && Casts.last(player) == null,
                "An unfunded copy is refused");
        SoulCore.setSpiritualityQuietly(player, 78000);
        AbilityComboGameTest.tap(player, 7);
        var wheel = AbilityWheel.find("key_of_stars_starlight");
        test.assertTrue(wheel.lastPick(player) == wheel.entry("toy_box") && SoulCore.getSpirituality(player) == 63000,
                "The dedicated key casts the memorized entry after the caster leaves and charges once");
        test.assertTrue(!Casts.last(player).copyable() && memory.complete(player, id), "Replication keeps its memory and cannot be copied");
        AbilityComboGameTest.tap(player, 7);
        test.assertTrue(SoulCore.getSpirituality(player) == 63000, "Replication observes its cooldown");

        player.setShiftKeyDown(true);
        AbilityComboGameTest.tap(player, 6);
        AbilityMenu.handleChoice(player, "key_of_stars_starlight", 0);
        test.assertTrue(SoulCore.getSpirituality(player) == 45000 && wheel.entry("replication") == null,
                "Sneak + slot 6 opens Starlight with Toy Box available at its normal cost");
        test.succeed();
    }
}
