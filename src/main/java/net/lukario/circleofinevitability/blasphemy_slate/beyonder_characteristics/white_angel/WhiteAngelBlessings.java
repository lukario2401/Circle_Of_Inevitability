package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.white_angel;

import java.util.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Named light presets; strongest resistance wins through the shared resistance system. */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class WhiteAngelBlessings {
  private static final Map<UUID, Long> WARD = new HashMap<>(),
      OATH = new HashMap<>(),
      HOLY = new HashMap<>(),
      ARMOR_HOLY = new HashMap<>();

  private static boolean active(Map<UUID, Long> map, LivingEntity e) {
    return map.getOrDefault(e.getUUID(), 0L) > e.level().getGameTime();
  }

  static {
    Fear.addImmunity(e -> (active(HOLY, e) || active(ARMOR_HOLY, e)));
    Madness.addImmunity(e -> (active(HOLY, e) || active(ARMOR_HOLY, e)));
    Dispel.addRule(
        e -> {
          boolean had =
              active(WARD, e) || active(OATH, e) || active(HOLY, e) || active(ARMOR_HOLY, e);
          clear(e);
          return had;
        });
  }

  static void ward(LivingEntity e, int ticks) {
    WARD.merge(e.getUUID(), e.level().getGameTime() + ticks, Math::max);
    WhiteAngel.courage(e, ticks);
    if (SupernaturalResistance.ticksLeft(e, "white_angel_ward") < ticks)
      SupernaturalResistance.grant(e, "white_angel_ward", ticks, .2f, false, null);
  }

  static void oath(LivingEntity e, int ticks) {
    OATH.merge(e.getUUID(), e.level().getGameTime() + ticks, Math::max);
  }

  static void armorSanctity(LivingEntity e, int ticks) {
    ARMOR_HOLY.put(e.getUUID(), e.level().getGameTime() + ticks);
    Fear.end(e);
    Madness.clear(e);
    Taunt.remove(e);
  }

  static void endArmorSanctity(LivingEntity e) {
    ARMOR_HOLY.remove(e.getUUID());
  }

  static void sanctify(LivingEntity e, int ticks) {
    HOLY.merge(e.getUUID(), e.level().getGameTime() + ticks, Math::max);
    Fear.end(e);
    Madness.clear(e);
    Taunt.remove(e);
  }

  static void fervor(LivingEntity e, int ticks) {
    ward(e, ticks);
    sanctify(e, ticks);
    if (SupernaturalResistance.ticksLeft(e, "white_angel_fervor") >= ticks) return;
    SupernaturalResistance.grant(e, "white_angel_fervor", ticks, .6f, true, null);
    DamageWard.grant(
        e,
        "white_angel_fervor",
        ticks,
        (s, a) ->
            !s.is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                && (s.is(DamageTypes.WITHER)
                    || s.is(DamageTypes.MAGIC)
                    || s.is(DamageTypes.INDIRECT_MAGIC)
                    || s.is(DamageTypeTags.IS_FREEZING)));
  }

  @SubscribeEvent
  public static void hurt(LivingHurtEvent e) {
    if (e.getSource().getEntity() instanceof LivingEntity a) {
      if (active(WARD, a) && WhiteAngel.purifyMultiplier(e.getEntity()) > 1)
        e.setAmount(e.getAmount() * 1.2f);
      if (active(OATH, a)
          && (e.getSource().is(DamageTypeTags.IS_FIRE)
              || net.lukario.circleofinevitability.combat.Spells.isSpellHit(e.getEntity())))
        e.setAmount(e.getAmount() * 1.2f);
    }
  }

  static void clear(LivingEntity e) {
    WARD.remove(e.getUUID());
    OATH.remove(e.getUUID());
    HOLY.remove(e.getUUID());
    ARMOR_HOLY.remove(e.getUUID());
    SupernaturalResistance.end(e, "white_angel_ward");
    SupernaturalResistance.end(e, "white_angel_fervor");
    DamageWard.end(e, "white_angel_fervor");
  }

  @SubscribeEvent
  public static void tick(LivingEvent.LivingTickEvent event) {
    var e = event.getEntity();
    if (e.level().isClientSide()) return;
    for (var map : List.of(WARD, OATH, HOLY, ARMOR_HOLY))
      if (map.getOrDefault(e.getUUID(), Long.MAX_VALUE) <= e.level().getGameTime())
        map.remove(e.getUUID());
  }

  @SubscribeEvent
  public static void death(LivingDeathEvent e) {
    clear(e.getEntity());
  }

  @SubscribeEvent
  public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
    clear(e.getEntity());
  }
}
