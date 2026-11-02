package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.white_angel;

import java.util.function.Consumer;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.DeferredRegister;

public final class WhiteAngelGameTests {
  public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
      DeferredRegister.create(Registries.TEST_FUNCTION, "circle_of_inevitability");

  static {
    FUNCTIONS.register("white_angel_costs", () -> WhiteAngelGameTests::costs);
    FUNCTIONS.register("white_angel_authorities", () -> WhiteAngelGameTests::authorities);
    FUNCTIONS.register("white_angel_justice", () -> WhiteAngelGameTests::justice);
    FUNCTIONS.register("white_angel_pacts", () -> WhiteAngelGameTests::pacts);
    FUNCTIONS.register("white_angel_equipment", () -> WhiteAngelGameTests::equipment);
    FUNCTIONS.register("white_angel_rescue", () -> WhiteAngelGameTests::rescue);
    FUNCTIONS.register("white_angel_copied_keys", () -> WhiteAngelGameTests::copiedKeys);
    FUNCTIONS.register("white_angel_believer", () -> WhiteAngelGameTests::believer);
  }

  private static ServerPlayer player(GameTestHelper h, int sequence, double x, double z) {
    var cookie =
        net.minecraft.server.network.CommonListenerCookie.createInitial(
            new com.mojang.authlib.GameProfile(
                java.util.UUID.randomUUID(),
                "white" + java.util.UUID.randomUUID().toString().substring(0, 6)),
            false);
    var p =
        new ServerPlayer(
            h.getLevel().getServer(),
            h.getLevel(),
            cookie.gameProfile(),
            cookie.clientInformation());
    var connection =
        new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
    new io.netty.channel.embedded.EmbeddedChannel(connection);
    h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
    p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    p.setNoGravity(true);
    p.connection.handleAcceptPlayerLoad(
        new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
    var at = h.absoluteVec(new Vec3(x, 11, z));
    p.setPos(at.x, at.y, at.z);
    SoulCore.setPathway(p, WhiteAngel.PATHWAY);
    SoulCore.setSequence(p, sequence);
    SoulCore.setSpiritualityQuietly(
        p, net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder.maxSpirituality(p));
    h.addCleanup(
        ok -> {
          WhiteAngel.clear(p);
          h.getLevel().getServer().getPlayerList().remove(p);
        });
    return p;
  }

  private static void authorities(GameTestHelper h) {
    var p = player(h, 0, 1.5, 1.5);
    p.setPos(p.getX(), p.getY() + 200, p.getZ());
    int count =
        4
            + WhiteAngel.THEURGY.entries().size()
            + WhiteAngel.NOTARY.entries().size()
            + WhiteAngel.RADIANCE.entries().size()
            + WhiteAngelJustice.WHEEL.entries().size()
            + WhiteAngelSun.WHEEL.entries().size();
    h.assertTrue(count == 29, "All 29 abilities are registered");
    float mana = SoulCore.getSpirituality(p);
    WhiteAngelSun.WHEEL.cast(p, WhiteAngelSun.WHEEL.entry("sun"));
    h.assertTrue(
        Zone.isOpen(p, "white_angel_sun"), "Sun can be afforded from the real Sequence 0 pool");
    h.assertTrue(SoulCore.getSpirituality(p) == mana - 50000, "Sun costs 50000");
    SoulCore.setSpiritualityQuietly(p, 0);
    WhiteAngelSun.WHEEL.cast(p, WhiteAngelSun.WHEEL.entry("sun"));
    h.assertTrue(!Zone.isOpen(p, "white_angel_sun"), "Sun lowers for free at zero spirituality");
    SoulCore.setSequence(p, 2);
    SoulCore.setSpiritualityQuietly(p, 78400);
    WhiteAngelSun.WHEEL.cast(p, WhiteAngelSun.WHEEL.entry("solar_envoy"));
    h.assertTrue(
        Form.isIn(p, "white_angel_envoy") && p.getAbilities().mayfly, "Envoy grants flight");
    SoulCore.setSpiritualityQuietly(p, 0);
    WhiteAngelSun.WHEEL.cast(p, WhiteAngelSun.WHEEL.entry("solar_envoy"));
    h.assertTrue(
        !Form.isIn(p, "white_angel_envoy") && !p.getAbilities().mayfly,
        "Envoy lowering restores flight flags");
    SoulCore.setSpiritualityQuietly(p, 78400);
    h.assertTrue(
        WhiteAngelSun.WHEEL.cast(p, WhiteAngelSun.WHEEL.entry("holy_kingdom"))
            == AbilityWheel.Result.REFUSED,
        "Kingdom remains locked at Sequence 2");
    net.lukario.circleofinevitability.root.Root.apply(
        p, 100, net.lukario.circleofinevitability.root.RootRestriction.TELEPORT);
    var from = p.position();
    p.setYRot(0);
    p.setXRot(0);
    h.assertTrue(
        WhiteAngelSun.WHEEL.cast(p, WhiteAngelSun.WHEEL.entry("praise_the_sun"))
            == AbilityWheel.Result.DONE,
        "Praise escapes an existing teleport root");
    h.assertTrue(
        p.position().distanceToSqr(from) > 1 && WhiteAngelSun.anchored(p),
        "Praise moves and anchors the caster");
    SoulCore.setSequence(p, 9);
    SoulCore.setSpiritualityQuietly(p, 20);
    WhiteAngel.key(p, p.level(), h.getLevel(), 1, false);
    h.assertTrue(
        SoulCore.getSpirituality(p) == 15, "Bard can afford a hymn from its twenty-point pool");
    WhiteAngel.clear(p);
    h.succeed();
  }

  private static void justice(GameTestHelper h) {
    var p = player(h, 3, 1.5, 1.5);
    p.setYRot(0);
    p.setXRot(0);
    var pig = EntityTypes.PIG.create(h.getLevel(), EntitySpawnReason.MOB_SUMMONED);
    var at = p.position().add(0, 0, 4);
    pig.setPos(at.x, at.y, at.z);
    h.getLevel().addFreshEntity(pig);
    pig.setNoAi(true);
    pig.setNoGravity(true);
    h.addCleanup(ok -> pig.discard());
    h.assertTrue(WhiteAngelJustice.guilt(p, pig) == 0, "A peaceful bystander starts innocent");
    float mana = SoulCore.getSpirituality(p);
    h.assertTrue(
        WhiteAngelJustice.WHEEL.cast(p, WhiteAngelJustice.WHEEL.entry("judgment"))
            == AbilityWheel.Result.REFUSED,
        "Innocent judgments are refused");
    h.assertTrue(SoulCore.getSpirituality(p) == mana, "Innocent judgments cost nothing");
    h.assertTrue(
        WhiteAngelJustice.choose(p, WhiteAngelJustice.Principle.PURITY),
        "A free principle may be chosen");
    var husk = EntityTypes.HUSK.create(h.getLevel(), EntitySpawnReason.MOB_SUMMONED);
    pig.discard();
    husk.setPos(at.x, at.y, at.z);
    husk.setNoAi(true);
    husk.setNoGravity(true);
    husk.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);
    husk.setHealth(1000);
    h.getLevel().addFreshEntity(husk);
    h.addCleanup(ok -> husk.discard());
    h.assertTrue(WhiteAngelJustice.guilt(p, husk) == 3, "Purity recognizes undead guilt");
    h.assertTrue(
        Spells.aimEnemy(p, h.getLevel(), 32) == husk,
        "Aim at husk: "
            + p.getEyePosition()
            + " direction "
            + p.getLookAngle()
            + " body "
            + husk.getBoundingBox()
            + " aim "
            + Spells.aimEnemy(p, h.getLevel(), 32));
    WhiteAngelJustice.WHEEL.cast(p, WhiteAngelJustice.WHEEL.entry("justice_halo"));
    h.assertTrue(
        !WhiteAngelJustice.choose(p, WhiteAngelJustice.Principle.ORDER),
        "A standing halo locks its principle");
    h.assertTrue(
        WhiteAngelJustice.WHEEL.cast(p, WhiteAngelJustice.WHEEL.entry("judgment"))
            == AbilityWheel.Result.DONE,
        "Guilty judgment starts");
    h.assertTrue(husk.getHealth() == 1000, "Judgment leaves a dodge warning");
    h.runAfterDelay(
        32,
        () -> {
          h.assertTrue(
              husk.getHealth() <= 870, "Judgment delivers 40 + 30 per guilt after the warning");
          h.assertTrue(Falter.chance(husk) >= .6f, "Judgment condemns the guilty");
          WhiteAngel.clear(p);
          h.succeed();
        });
  }

  private static void pacts(GameTestHelper h) {
    var p = player(h, 6, 1.5, 1.5);
    var t = player(h, 5, 1.5, 5.5);
    p.setYRot(0);
    p.setXRot(0);
    h.assertTrue(
        Spells.aimLiving(p, h.getLevel(), 16, false, e -> e != p) == t,
        "Aim at pact player: "
            + p.getEyePosition()
            + " direction "
            + p.getLookAngle()
            + " body "
            + t.getBoundingBox()
            + " visible "
            + p.hasLineOfSight(t));
    h.assertTrue(
        WhiteAngel.NOTARY.cast(p, WhiteAngel.NOTARY.entry("contract")) == AbilityWheel.Result.DONE,
        "Contract offer starts");
    h.assertTrue(WhiteAngel.pactCount(p) == 0, "A player is never bound before consent");
    h.getLevel()
        .getServer()
        .getCommands()
        .performPrefixedCommand(t.createCommandSourceStack(), "whitepact accept");
    h.assertTrue(WhiteAngel.pactCount(p) == 1, "Explicit player consent creates the pact");
    float health = p.getHealth();
    Spells.strike(t, p, 4);
    h.assertTrue(p.getHealth() == health, "One Sequence stronger cannot violate the pact");
    WhiteAngel.clear(p);
    h.assertTrue(WhiteAngel.pactCount(p) == 0, "Pacts clear with the caster state");
    h.succeed();
  }

  private static void believer(GameTestHelper h) {
    var p = player(h, 1, 1.5, 1.5);
    p.setYRot(0);
    p.setXRot(0);
    var mob = EntityTypes.HUSK.create(h.getLevel(), EntitySpawnReason.MOB_SUMMONED);
    mob.setPos(p.getX(), p.getY(), p.getZ() + 3);
    mob.setNoAi(true);
    mob.setNoGravity(true);
    h.getLevel().addFreshEntity(mob);
    h.addCleanup(ok -> mob.discard());
    h.assertTrue(
        WhiteAngelSun.WHEEL.cast(p, WhiteAngelSun.WHEEL.entry("servant_of_faith"))
            == AbilityWheel.Result.DONE,
        "Faith begins conversion of a hostile mob");
    h.assertTrue(!Marionette.isMarionette(mob), "Conversion must wait for its channel");
    // Embedded GameTest connections tick the entity without the ordinary player-post hook.
    // Drive the real event bus so the channel, passive aura and ownership lifecycle all run.
    for (int tick = 1; tick <= 100; tick++)
      h.runAfterDelay(
          tick,
          () ->
              net.minecraftforge.event.TickEvent.PlayerTickEvent.Post.BUS.post(
                  new net.minecraftforge.event.TickEvent.PlayerTickEvent.Post(p)));
    h.runAfterDelay(
        103,
        () -> {
          h.assertTrue(
              WhiteAngelSun.believerCount(p) == 1 && Spells.isFriend(p, mob),
              "Conversion after five seconds: count="
                  + WhiteAngelSun.believerCount(p)
                  + " alive="
                  + mob.isAlive()
                  + " hp="
                  + mob.getHealth()
                  + " ticks="
                  + p.tickCount
                  + " left="
                  + Countdown.ticksLeft(p)
                  + " distance="
                  + p.distanceToSqr(mob)
                  + " owner="
                  + Marionette.masterId(mob));
          WhiteAngel.clear(p);
          h.assertTrue(WhiteAngelSun.believerCount(p) == 0, "Believers clear with their owner");
          h.succeed();
        });
  }

  private static void equipment(GameTestHelper h) {
    var p = player(h, 3, 1.5, 1.5);
    var ally = EntityTypes.PIG.create(h.getLevel(), EntitySpawnReason.MOB_SUMMONED);
    ally.setPos(p.getX() + 2, p.getY(), p.getZ());
    ally.setNoAi(true);
    ally.setNoGravity(true);
    h.getLevel().addFreshEntity(ally);
    Friends.add(p, ally.getUUID(), "ally", false);
    h.addCleanup(ok -> ally.discard());
    WhiteAngel.RADIANCE.cast(p, WhiteAngel.RADIANCE.entry("holy_equipment"));
    h.assertTrue(ConjuredArmor.isWorn(ally, "white_angel_armor"), "Sequence 3 shares the armor");
    h.assertTrue(!ConjuredWeapon.isDrawn(ally, "white_angel_mace"), "Allies do not receive a mace");
    h.assertTrue(ConjuredWeapon.isDrawn(p, "white_angel_mace"), "The caster receives a mace");
    Madness.add(p, ally, 3);
    h.assertTrue(Madness.get(ally) == 0, "Holy equipment resists madness");
    WhiteAngel.clear(p);
    h.assertTrue(
        !ConjuredArmor.isWorn(ally, "white_angel_armor"),
        "Caster cleanup removes shared equipment");
    Madness.add(p, ally, 1);
    h.assertTrue(Madness.get(ally) > 0, "Equipment immunity ends with the set");
    h.succeed();
  }

  private static void rescue(GameTestHelper h) {
    var p = player(h, 0, 1.5, 1.5);
    p.setPos(p.getX(), p.getY() + 300, p.getZ());
    float max =
        net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder.maxSpirituality(p);
    p.hurtServer(h.getLevel(), h.getLevel().damageSources().generic(), 1000000);
    h.assertTrue(
        p.isAlive() && p.getHealth() == p.getMaxHealth() * .5f,
        "Inextinguishable Light cancels a fatal hit at half health");
    h.assertTrue(SoulCore.getSpirituality(p) == max * .5f, "Rescue spends half the maximum pool");
    h.assertTrue(Invulnerable.isGranted(p), "Rescue grants its invulnerability window");
    Invulnerable.end(p);
    p.invulnerableTime = 0;
    SoulCore.setSpiritualityQuietly(p, max * .5f - 1);
    p.hurtServer(h.getLevel(), h.getLevel().damageSources().generic(), 1000000);
    h.assertTrue(!p.isAlive(), "Insufficient spirituality cannot prevent death");
    h.succeed();
  }

  private static void copiedKeys(GameTestHelper h) {
    var p = player(h, 2, 1.5, 1.5);
    SoulCore.setPathway(p, "Worm Of Time");
    var power =
        new net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics
            .PathwayAbilities.Ability("test", 7, "Authority", 0, false, true, (a, b, c) -> {});
    net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time
        .StolenAbilities.hold(p, power, false, null, 200, 5);
    h.addCleanup(
        ok ->
            net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time
                .StolenAbilities.dropAll(p));
    h.assertTrue(
        AbilityKeys.strongest(p) == 2, "The strongest copied power seals its actual container key");
    h.succeed();
  }

  private static void costs(GameTestHelper h) {
    var cookie =
        net.minecraft.server.network.CommonListenerCookie.createInitial(
            new com.mojang.authlib.GameProfile(
                java.util.UUID.randomUUID(),
                "white" + java.util.UUID.randomUUID().toString().substring(0, 6)),
            false);
    var p =
        new ServerPlayer(
            h.getLevel().getServer(),
            h.getLevel(),
            cookie.gameProfile(),
            cookie.clientInformation());
    var connection =
        new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
    new io.netty.channel.embedded.EmbeddedChannel(connection);
    h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
    p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    p.setNoGravity(true);
    p.connection.handleAcceptPlayerLoad(
        new net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket());
    var at = h.absoluteVec(new net.minecraft.world.phys.Vec3(1.5, 1, 1.5));
    p.setPos(at.x, at.y, at.z);
    SoulCore.setPathway(p, "White Angel");
    SoulCore.setSequence(p, 4);
    SoulCore.setSpiritualityQuietly(p, 20000);
    h.addCleanup(
        ok -> {
          for (String n : java.util.List.of("domain", "daytime", "halo"))
            Zone.close(p, "white_angel_" + n);
          h.getLevel().getServer().getPlayerList().remove(p);
        });
    float before = SoulCore.getSpirituality(p);
    var domain =
        WhiteAngel.RADIANCE.entries().stream()
            .filter(e -> e.key().equals("domain"))
            .findFirst()
            .orElseThrow();
    WhiteAngel.RADIANCE.cast(p, domain);
    h.assertTrue(Zone.isOpen(p, "white_angel_domain"), "Domain opens");
    h.assertTrue(SoulCore.getSpirituality(p) == before - 4000, "Domain costs 4000 once");
    SoulCore.setSpiritualityQuietly(p, 0);
    WhiteAngel.RADIANCE.cast(p, domain);
    h.assertTrue(
        !Zone.isOpen(p, "white_angel_domain") && SoulCore.getSpirituality(p) == 0,
        "Domain lowers without mana");
    SoulCore.setSpiritualityQuietly(p, 1000);
    p.setShiftKeyDown(true);
    before = SoulCore.getSpirituality(p);
    WhiteAngel.key(p, p.level(), h.getLevel(), 1, false);
    h.assertTrue(SoulCore.getSpirituality(p) == before - 250, "Renewal cannot refund the caster");
    SoulCore.setSequence(p, 9);
    before = SoulCore.getSpirituality(p);
    WhiteAngel.key(p, p.level(), h.getLevel(), 2, false);
    h.assertTrue(SoulCore.getSpirituality(p) == before, "Sequence gate refuses Light at Bard");
    h.succeed();
  }
}
