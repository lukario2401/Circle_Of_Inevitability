package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.white_angel;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.server.level.*;
import net.minecraft.world.phys.Vec3;

/** Delayed area attacks, a travelling spear and the non-item equipment set. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class WhiteAngelRadiance {
  private record Equipment(
      ServerPlayer owner, net.minecraft.world.entity.LivingEntity wearer, long until) {}

  private static final java.util.Map<java.util.UUID, Equipment> EQUIPMENT =
      new java.util.HashMap<>();

  static void clear(net.minecraft.world.entity.player.Player owner) {
    for (var entry : new java.util.ArrayList<>(EQUIPMENT.values()))
      if (entry.owner == owner) {
        EQUIPMENT.remove(entry.wearer.getUUID());
        ConjuredArmor.takeOff(entry.wearer, "white_angel_armor");
        WhiteAngelBlessings.endArmorSanctity(entry.wearer);
        ConjuredWeapon.putAway(entry.wearer, "white_angel_mace");
      }
  }

  static {
    Dispel.addRule(
        e -> {
          boolean had = EQUIPMENT.remove(e.getUUID()) != null;
          ConjuredArmor.takeOff(e, "white_angel_armor");
          ConjuredWeapon.putAway(e, "white_angel_mace");
          return had;
        });
  }

  @net.minecraftforge.eventbus.api.listener.SubscribeEvent
  public static void tick(
      net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
    var wearer = event.getEntity();
    if (!(wearer.level() instanceof ServerLevel sl)) return;
    var set = EQUIPMENT.get(wearer.getUUID());
    if (set == null) return;
    if (!wearer.isAlive()
        || !WhiteAngel.valid(set.owner, sl)
        || set.until <= sl.getGameTime()
        || !ConjuredArmor.isWorn(wearer, "white_angel_armor")) {
      EQUIPMENT.remove(wearer.getUUID());
      ConjuredArmor.takeOff(wearer, "white_angel_armor");
      WhiteAngelBlessings.endArmorSanctity(wearer);
      ConjuredWeapon.putAway(wearer, "white_angel_mace");
      return;
    }
    if (sl.getGameTime() % 20 == 0) {
      WhiteAngelFx.armor(sl, wearer, 22);
      if (ConjuredWeapon.isDrawn(wearer, "white_angel_mace")) WhiteAngelFx.mace(sl, wearer, 22);
    }
  }

  static AbilityWheel.Result flaringSun(ServerPlayer p, ServerLevel sl) {
    Vec3 at = Spells.aimGround(p, sl, 28);
    double radius = 9 * WhiteAngel.might(p);
    WhiteAngelFx.ring(sl, at, "warning_sigil", radius, 30);
    Later.run(
        sl,
        30,
        () -> {
          if (!WhiteAngel.valid(p, sl)) return;
          WhiteAngelFx.sun(sl, at, radius, 25);
          for (var enemy : Spells.enemiesAround(p, sl, at, radius)) {
            WhiteAngel.strike(p, enemy, 70);
            enemy.igniteForSeconds(6);
          }
          Zone.at(at, radius)
              .lasts(80)
              .every(20)
              .onPulse(
                  left -> {
                    if (!WhiteAngel.valid(p, sl)) return;
                    WhiteAngelFx.at(sl, at, "flame_plume", 22);
                    for (var enemy : Spells.enemiesAround(p, sl, at, radius))
                      WhiteAngel.strike(p, enemy, 6);
                  })
              .open(p, sl, "white_angel_afterglow");
        });
    WhiteAngelFx.sound(p, "flaring_sun");
    return AbilityWheel.Result.DONE;
  }

  static AbilityWheel.Result spear(ServerPlayer p, ServerLevel sl) {
    boolean upgraded = Beyonder.hasSequence(p, 2);
    Shot shot =
        Shot.of(WhiteAngelFx.model("spear_of_light", 15))
            .speed(4)
            .range(40)
            .width(.5)
            .onHit(
                (enemy, at) -> {
                  if (!WhiteAngel.valid(p, sl)) return;
                  if (upgraded) {
                    WhiteAngel.strike(p, enemy, 110);
                    Root.apply(enemy, 60, RootRestriction.TELEPORT);
                    Blind.apply(enemy, 40);
                  }
                })
            .onEnd(
                (at, wall) -> {
                  if (!WhiteAngel.valid(p, sl)) return;
                  WhiteAngelFx.sun(sl, at, upgraded ? 8 : 5, 18);
                  for (var enemy : Spells.enemiesAround(p, sl, at, upgraded ? 8 : 5)) {
                    WhiteAngel.strike(p, enemy, upgraded ? 110 : 60);
                    Blind.apply(enemy, 40);
                    if (upgraded) Root.apply(enemy, 60, RootRestriction.TELEPORT);
                  }
                });
    if (upgraded) shot = shot.pierce();
    shot.fire(p, sl, p.getEyePosition(), p.getLookAngle());
    WhiteAngelFx.sound(p, "spear");
    return AbilityWheel.Result.DONE;
  }

  static AbilityWheel.Result whiteRay(ServerPlayer p, ServerLevel sl) {
    Ray.from(p.getEyePosition(), p.getLookAngle())
        .range(40)
        .width(.6)
        .warning(10)
        .pierces()
        .onWarn((from, to) -> WhiteAngelFx.beam(sl, from, to, 10))
        .onFire((from, to) -> WhiteAngelFx.beam(sl, from, to, 15))
        .onHit(
            (enemy, at) -> {
              if (WhiteAngel.valid(p, sl)) {
                WhiteAngel.strike(p, enemy, 80);
                Blind.apply(enemy, 40);
              }
            })
        .fire(p, sl);
    WhiteAngelFx.sound(p, "white_ray");
    return AbilityWheel.Result.DONE;
  }

  static AbilityWheel.Result equipment(ServerPlayer p, ServerLevel sl) {
    clear(p);
    int ticks = WhiteAngel.duration(p, 1200);
    for (var ally :
        WhiteAngel.friends(p, sl, Beyonder.hasSequence(p, 3) ? 24 * WhiteAngel.might(p) : 0)) {
      ConjuredArmor.of("white_angel_armor")
          .lasts(ticks)
          .cut(.35f)
          .absorption(6)
          .regrow(20)
          .steady()
          .wear(ally);
      if (Beyonder.hasSequence(p, 3)) WhiteAngelBlessings.armorSanctity(ally, ticks);
      EQUIPMENT.put(ally.getUUID(), new Equipment(p, ally, sl.getGameTime() + ticks));
      WhiteAngelFx.armor(sl, ally, 22);
    }
    ConjuredWeapon.of("white_angel_mace")
        .lasts(ticks)
        .reach(1)
        .onHit(
            (attacker, target, amount, charge, level) ->
                amount + 8 * charge * WhiteAngel.purifyMultiplier(target))
        .draw(p);
    WhiteAngelFx.sound(p, "holy_equipment");
    return AbilityWheel.Result.DONE;
  }
}
