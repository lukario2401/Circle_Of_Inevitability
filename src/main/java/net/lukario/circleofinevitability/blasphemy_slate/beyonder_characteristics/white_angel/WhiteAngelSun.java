package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.white_angel;

import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Sun authorities, with one replaceable instance per field and explicit upkeep. */
public final class WhiteAngelSun {
  private static final String ENVOY = "white_angel_envoy",
      KINGDOM = "white_angel_kingdom",
      SUN = "white_angel_sun";
  private static final Map<UUID, UUID> CONVERTING = new HashMap<>();

  static boolean converting(Player p, LivingEntity target) {
    return target.getUUID().equals(CONVERTING.get(p.getUUID()));
  }

  private static final Map<UUID, Long> ANCHORS = new HashMap<>();
  private static final Map<UUID, List<Mob>> BELIEVERS = new HashMap<>();
  public static final AbilityWheel WHEEL =
      WhiteAngel.wheel("sun", "Sun")
          .entry(
              "solar_envoy",
              WhiteAngel.icon("solar_envoy"),
              "Solar Envoy",
              2,
              12000,
              0,
              WhiteAngelSun::envoy)
          .free(p -> Zone.isOpen(p, ENVOY))
          .entry(
              "praise_the_sun",
              WhiteAngel.icon("praise_the_sun"),
              "Praise the Sun",
              2,
              8000,
              0,
              WhiteAngelSun::praise)
          .entry(
              "holy_kingdom",
              WhiteAngel.icon("holy_kingdom"),
              "Holy Kingdom",
              1,
              40000,
              0,
              WhiteAngelSun::kingdom)
          .free(p -> Zone.isOpen(p, KINGDOM))
          .notCopyable()
          .entry(
              "servant_of_faith",
              WhiteAngel.icon("servant_of_faith"),
              "Servant of Faith",
              1,
              15000,
              0,
              WhiteAngelSun::servant)
          .notCopyable()
          .entry(
              "pure_white_light",
              WhiteAngel.icon("pure_white_light"),
              "Pure White Light",
              1,
              25000,
              0,
              WhiteAngelSun::whiteLight)
          .entry("sun", WhiteAngel.icon("sun"), "Sun", 0, 50000, 0, WhiteAngelSun::sun)
          .free(p -> Zone.isOpen(p, SUN))
          .notCopyable();

  static {
    Spells.addMoveImmunity(WhiteAngelSun::anchored);
    Root.addImmunity((e, r) -> anchored(e) && r == RootRestriction.TELEPORT);
    Enclosure.addImmunity(WhiteAngelSun::anchored);
    Spells.addCostScale(
        p -> {
          if (!(p.level() instanceof ServerLevel sl)) return 1f;
          return sl.players().stream()
                  .anyMatch(
                      owner ->
                          WhiteAngel.own(owner)
                              && Spells.isEnemy(owner, p)
                              && Zone.covers(owner, SUN, p.position()))
              ? 2f
              : 1f;
        });
  }

  public static void initialize() {}

  static boolean anchored(LivingEntity e) {
    return ANCHORS.getOrDefault(e.getUUID(), 0L) > e.level().getGameTime();
  }

  static int believerCount(Player p) {
    return believers(p).size();
  }

  private static List<Mob> believers(Player p) {
    var list = BELIEVERS.computeIfAbsent(p.getUUID(), id -> new ArrayList<>());
    list.removeIf(m -> !m.isAlive() || m.level() != p.level() || !Marionette.isMarionetteOf(m, p));
    return list;
  }

  private static AbilityWheel.Result envoy(ServerPlayer p, ServerLevel sl) {
    WhiteAngel.authorize(p);
    if (Zone.isOpen(p, ENVOY)) {
      Zone.close(p, ENVOY);
      return AbilityWheel.Result.FREE;
    }
    int seq = SoulCore.getSequence(p),
        ticks = WhiteAngel.duration(p, seq == 0 ? 600 : seq == 1 ? 400 : 280);
    double radius = (seq == 0 ? 40 : seq == 1 ? 24 : 12) * WhiteAngel.might(p);
    Zone.around(p, radius)
        .lasts(ticks)
        .every(10)
        .onPulse(
            left -> {
              if (!WhiteAngel.valid(p, sl)
                  || (!Form.isIn(p, ENVOY) && Zone.ticksOpen(p, ENVOY) > 0)) {
                Zone.close(p, ENVOY);
                return;
              }
              Nocturnality.forceDay(sl, p.position(), radius, 12);
              WhiteAngel.clean(p);
              WhiteAngelBlessings.fervor(p, 12);
              for (var enemy : WhiteAngel.visibleAndHiddenEnemies(p, sl, p.position(), radius)) {
                Exposed.apply(enemy, 15);
                WhiteAngel.strike(p, enemy, 8);
                Blind.apply(enemy, 15);
                Exposed.apply(enemy, 15);
              }
            })
        .onEnd(
            () -> {
              if (Form.isIn(p, ENVOY)) Form.end(p);
            })
        .open(p, sl, ENVOY);
    Form.of(ENVOY)
        .lasts(ticks)
        .flies()
        .cutsDamage(.5f)
        .keepWhile(
            a -> a instanceof ServerPlayer sp && WhiteAngel.valid(sp, sl) && Zone.isOpen(a, ENVOY))
        .everySecond(
            a -> {
              if (!Spells.paySpirituality(a, 400)) Zone.close(a, ENVOY);
            })
        .every(
            20,
            a -> {
              if (a.level() instanceof ServerLevel level) WhiteAngelFx.sunbird(level, a, 22);
            })
        .onEnd((a, why) -> Zone.close(a, ENVOY))
        .take(p);
    WhiteAngelFx.sunbird(sl, p, 22);
    WhiteAngelFx.sound(p, "solar_envoy");
    return AbilityWheel.Result.DONE;
  }

  private static AbilityWheel.Result praise(ServerPlayer p, ServerLevel sl) {
    int seq = SoulCore.getSequence(p);
    double range = seq == 0 ? 160 : seq == 1 ? 96 : 48;
    Vec3 from = p.position(), at = Spells.blinkSpot(p, sl, range);
    if (at == null || from.distanceToSqr(at) < 1) return AbilityWheel.Result.REFUSED;
    Long prior = ANCHORS.put(p.getUUID(), sl.getGameTime() + 100);
    if (!Teleport.of(p).fallGuard(60).unbinding().go(sl, at)) {
      if (prior == null) ANCHORS.remove(p.getUUID());
      else ANCHORS.put(p.getUUID(), prior);
      return AbilityWheel.Result.REFUSED;
    }
    for (var enemy : Spells.enemiesOnLine(p, sl, from.add(0, 1, 0), at.add(0, 1, 0), 1))
      WhiteAngel.strike(p, enemy, 50);
    Invulnerable.grant(p, 10);
    Concealment.hide(p, 10);
    WhiteAngelFx.beam(sl, from, at, 12);
    WhiteAngelFx.sound(p, "praise_the_sun");
    return AbilityWheel.Result.DONE;
  }

  private static AbilityWheel.Result kingdom(ServerPlayer p, ServerLevel sl) {
    if (Zone.isOpen(p, KINGDOM)) {
      Zone.close(p, KINGDOM);
      return AbilityWheel.Result.FREE;
    }
    Vec3 at = p.position();
    double radius = (Beyonder.hasSequence(p, 0) ? 48 : 40) * WhiteAngel.might(p);
    int ticks = WhiteAngel.duration(p, 900);
    Decree.at(at, radius)
        .height(radius)
        .lasts(ticks)
        .forbid(Decree.Rule.RESURRECTION, Decree.Rule.CONCEALMENT, Decree.Rule.STAND_INS)
        .binds(e -> WhiteAngel.purifyMultiplier(e) > 1 || WhiteAngelJustice.guilt(p, e) > 0)
        .write(p, sl, KINGDOM);
    Zone.at(at, radius)
        .lasts(ticks)
        .height(radius)
        .every(20)
        .onPulse(
            left -> {
              if (!WhiteAngel.valid(p, sl)
                  || p.position().distanceToSqr(at) > (radius + 20) * (radius + 20)
                  || (Zone.ticksOpen(p, KINGDOM) > 0 && !Spells.paySpirituality(p, 600))) {
                Zone.close(p, KINGDOM);
                return;
              }
              Nocturnality.forceDay(sl, at, radius, 22);
              WhiteAngelFx.ring(sl, at, "domain_boundary", radius, 22);
              // Kingdom's ban on undead applies even to the caster's own spirits.
              for (var one :
                  sl.getEntitiesOfClass(
                      LivingEntity.class,
                      new net.minecraft.world.phys.AABB(at, at).inflate(radius),
                      e -> e != p && e.isAlive() && e.distanceToSqr(at) <= radius * radius)) {
                if (WhiteAngel.purifyMultiplier(one) > 1) {
                  if (one.getMaxHealth() <= 100 && !(one instanceof Player)) Dispel.unmake(one);
                  else {
                    Exposed.apply(one, 25);
                    Spells.strikeTrue(p, one, one.getMaxHealth() * .05f);
                  }
                } else if (Spells.isFriend(p, one)) kingdomBlessing(one);
              }
              kingdomBlessing(p);
            })
        .onEnd(() -> Decree.close(p, KINGDOM))
        .open(p, sl, KINGDOM);
    WhiteAngelFx.at(sl, at, "kingdom_gate", 60);
    WhiteAngelFx.sound(p, "holy_kingdom");
    return AbilityWheel.Result.DONE;
  }

  private static void kingdomBlessing(LivingEntity e) {
    WhiteAngel.clean(e);
    WhiteAngelBlessings.ward(e, 25);
    Spells.keepEffect(e, MobEffects.REGENERATION, 1, 25);
    Spells.keepEffect(e, MobEffects.SPEED, 0, 25);
    Spells.keepEffect(e, MobEffects.STRENGTH, 0, 25);
  }

  private static AbilityWheel.Result servant(ServerPlayer p, ServerLevel sl) {
    var target = Spells.aimLiving(p, sl, 24, false, e -> e != p);
    if (target != null && Spells.isEnemy(p, target)) {
      if (!(target instanceof Mob mob)
          || Spells.isBoss(mob)
          || mob.getMaxHealth() > 150
          || (!Spells.isEvil(mob) && mob.getTarget() == null)) return AbilityWheel.Result.REFUSED;
      WhiteAngelFx.above(sl, mob, "believer_sun", 100);
      Countdown.seconds(5)
          .keepWhile(
              () ->
                  WhiteAngel.valid(p, sl)
                      && mob.isAlive()
                      && mob.level() == sl
                      && p.distanceToSqr(mob) <= 24 * 24)
          .onFinish(
              () -> {
                CONVERTING.remove(p.getUUID());
                var list = believers(p);
                // Summon control is deliberately separate from the ordinary three marionette slots.
                if (!Marionette.summon(mob, p, 6000)) return;
                while (list.size() >= 4) Marionette.release(list.remove(0));
                list.add(mob);
                WhiteAngelBlessings.ward(mob, 6000);
                WhiteAngelFx.above(sl, mob, "believer_sun", 6000);
                WhiteAngelFx.sound(p, "believer_convert");
              })
          .onCancel(() -> CONVERTING.remove(p.getUUID()))
          .start(p);
      CONVERTING.put(p.getUUID(), mob.getUUID());
    } else if (target != null) WhiteAngelBlessings.fervor(target, WhiteAngel.duration(p, 400));
    else
      for (var ally : WhiteAngel.friends(p, sl, 16 * WhiteAngel.might(p)))
        WhiteAngelBlessings.fervor(ally, WhiteAngel.duration(p, 400));
    WhiteAngelFx.sound(p, "servant_of_faith");
    return AbilityWheel.Result.DONE;
  }

  private static AbilityWheel.Result whiteLight(ServerPlayer p, ServerLevel sl) {
    Vec3 at = Spells.aimGround(p, sl, 40);
    double radius = 16 * WhiteAngel.might(p);
    WhiteAngelFx.ring(sl, at, "warning_sigil", radius, 40);
    Later.run(
        sl,
        40,
        () -> {
          if (!WhiteAngel.valid(p, sl)) return;
          WhiteAngelFx.ring(sl, at, "purification_ring", radius, 30);
          Zone.closeNear(sl, at, radius, owner -> Spells.isEnemy(p, owner));
          Dome.closeNear(sl, at, radius, owner -> Spells.isEnemy(p, owner));
          Enclosure.closeNear(sl, at, radius, owner -> Spells.isEnemy(p, owner));
          for (var shot : Spells.hostileShotsAround(p, sl, at, radius)) shot.discard();
          for (var enemy : WhiteAngel.visibleAndHiddenEnemies(p, sl, at, radius)) {
            if (Dispel.isConjured(enemy) || Construct.isConstruct(enemy)) Dispel.unmake(enemy);
            else {
              Dispel.boons(enemy);
              Dispel.statuses(enemy);
              ConjuredArmor.takeOff(enemy, "white_angel_armor");
              ConjuredWeapon.putAway(enemy, "white_angel_mace");
              Spells.strikeTrue(p, enemy, 200);
            }
          }
          for (var thing : Spells.looseThingsAround(p, sl, at, radius))
            if (Dispel.isConjured(thing)) Dispel.unmake(thing);
          for (var ally : WhiteAngel.friendsAt(p, sl, at, radius)) WhiteAngel.clean(ally);
          if (p.distanceToSqr(at) <= radius * radius) WhiteAngel.clean(p);
        });
    WhiteAngelFx.sound(p, "pure_white_light");
    return AbilityWheel.Result.DONE;
  }

  private static AbilityWheel.Result sun(ServerPlayer p, ServerLevel sl) {
    if (Zone.isOpen(p, SUN)) {
      Zone.close(p, SUN);
      return AbilityWheel.Result.FREE;
    }
    double radius = 64 * WhiteAngel.might(p);
    Zone.around(p, radius)
        .height(80)
        .lasts(WhiteAngel.duration(p, 800))
        .every(20)
        .onPulse(
            left -> {
              if (!WhiteAngel.valid(p, sl)
                  || (Zone.ticksOpen(p, SUN) > 0 && !Spells.paySpirituality(p, 1500))) {
                Zone.close(p, SUN);
                return;
              }
              Nocturnality.forceDay(sl, p.position(), radius, 22);
              WhiteAngelFx.sun(sl, p.position().add(0, 32, 0), 8, 22);
              var enemies = new ArrayList<>(Spells.enemiesAround(p, sl, p.position(), radius));
              enemies.sort(
                  Comparator.<LivingEntity>comparingInt(e -> WhiteAngelJustice.guilt(p, e))
                      .thenComparingDouble(WhiteAngel::purifyMultiplier)
                      .reversed());
              for (var enemy : enemies.stream().limit(8).toList()) {
                WhiteAngelFx.beam(sl, p.position().add(0, 40, 0), enemy.position(), 12);
                WhiteAngel.strike(p, enemy, 120);
                Blind.apply(enemy, 25);
              }
              for (var ally : WhiteAngel.friends(p, sl, radius))
                WhiteAngelBlessings.fervor(ally, 25);
            })
        .onEnd(() -> Nocturnality.forceDay(sl, p.position(), radius, 2400))
        .open(p, sl, SUN);
    WhiteAngelFx.proclaim(p, "Let there be light!");
    WhiteAngelFx.sound(p, "sun");
    return AbilityWheel.Result.DONE;
  }

  static void clear(Player p) {
    for (String name : List.of(ENVOY, KINGDOM, SUN)) Zone.close(p, name);
    Decree.close(p, KINGDOM);
    ANCHORS.remove(p.getUUID());
    CONVERTING.remove(p.getUUID());
    var list = BELIEVERS.remove(p.getUUID());
    if (list != null)
      for (var mob : list) if (Marionette.isMarionetteOf(mob, p)) Marionette.release(mob);
    if (Form.isIn(p, ENVOY)) Form.end(p);
    Countdown.cancel(p);
  }
}
