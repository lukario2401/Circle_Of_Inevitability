package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.white_angel;

import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.server.level.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.util.Result;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Innate defenses and the banked-spirituality resurrection gate. */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class WhiteAngelPassives {
  private static final Map<UUID, Set<UUID>> HUNTERS = new HashMap<>();

  static {
    Parasite.addHostImmunity(e -> innate(e, 4));
    Corrosion.addImmunity(e -> innate(e, 4));
    Decay.addImmunity(e -> innate(e, 2));
    Blind.addImmunity(e -> innate(e, 2));
  }

  private static boolean innate(LivingEntity e, int sequence) {
    return e instanceof Player p && WhiteAngel.own(p) && Beyonder.hasSequence(p, sequence);
  }

  static void tick(ServerPlayer p) {
    EntitySight.tick(p);
    if (p.level().getGameTime() % 20 != 0) return;
    boolean high = Beyonder.hasSequence(p, 4);
    Physique.keep(
        p,
        Attributes.MAX_HEALTH,
        Physique.id("white_angel_health"),
        Beyonder.hasSequence(p, 7) ? 8 : 4,
        AttributeModifier.Operation.ADD_VALUE,
        true);

    if (Beyonder.hasSequence(p, 2)) Blind.remove(p);
    var hunters = new HashSet<UUID>();
    for (var enemy : WhiteAngel.visibleAndHiddenEnemies(p, p.level(), p.position(), 24)) {
      if (Beyonder.hasSequence(p, 8) && WhiteAngel.purifyMultiplier(enemy) > 1)
        EntitySight.show(p, enemy, 25);
      if (Beyonder.hasSequence(p, 5)
          && p.distanceToSqr(enemy) <= 64
          && WhiteAngel.purifyMultiplier(enemy) > 1
          && !WhiteAngelSun.converting(p, enemy)) {
        WhiteAngel.strike(p, enemy, 2);
        enemy.igniteForSeconds(2);
      }
      if (high
          && p.distanceToSqr(enemy) <= 256
          && (enemy.isInvisible() || Concealment.isHidden(enemy) || Unnoticed.isUnnoticed(enemy))) {
        Exposed.apply(enemy, 25);
        EntitySight.show(p, enemy, 25);
      }
      if (high && enemy instanceof Mob mob && mob.getTarget() == p && !p.hasLineOfSight(enemy))
        hunters.add(enemy.getUUID());
    }
    if (high
        && hunters.stream()
            .anyMatch(id -> !HUNTERS.getOrDefault(p.getUUID(), Set.of()).contains(id)))
      WhiteAngelFx.sound(p, "intuition");
    HUNTERS.put(p.getUUID(), hunters);
    int pacts = WhiteAngel.pactCount(p), believers = WhiteAngelSun.believerCount(p);
    if (Beyonder.hasSequence(p, 1)) {
      int resources = Math.min(8, pacts + believers);
      p.heal(resources * .5f);
      Spells.giveSpirituality(p, Beyonder.maxSpirituality(p) * .005f * resources);
    }
    if (Beyonder.hasSequence(p, 6))
      HudCounter.show(
          p,
          "white_angel_pacts",
          "white_angel/pacts",
          "Pacts",
          pacts,
          WhiteAngel.slots(p),
          0xFFD85B);
    if (Beyonder.hasSequence(p, 1))
      HudCounter.show(
          p, "white_angel_believers", "white_angel/believers", "Believers", believers, 4, 0xFFD85B);
  }

  @SubscribeEvent
  public static void lightBody(LivingHurtEvent e) {
    if (!innate(e.getEntity(), 4) || e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))
      return;
    boolean special =
        e.getSource().is(DamageTypeTags.IS_FREEZING)
            || e.getSource().is(DamageTypes.WITHER)
            || e.getSource().is(DamageTypes.MAGIC)
            || e.getSource().is(DamageTypes.INDIRECT_MAGIC);
    float cut =
        innate(e.getEntity(), 2) && special
            ? .4f
            : SupernaturalResistance.isSupernatural(e.getSource()) ? .25f : 0;
    float existing =
        SupernaturalResistance.isSupernatural(e.getSource())
            ? SupernaturalResistance.cut(e.getEntity())
            : 0;
    if (cut > existing) e.setAmount(e.getAmount() * (1 - cut) / (1 - existing));
  }

  @SubscribeEvent
  public static void effects(MobEffectEvent.Applicable e) {
    var target = e.getEntity();
    var effect = e.getEffectInstance().getEffect();
    if (innate(target, 2) && effect.equals(MobEffects.BLINDNESS)) e.setResult(Result.DENY);
    if (target.level() instanceof ServerLevel sl
        && sl.players().stream()
            .anyMatch(
                p ->
                    WhiteAngel.own(p)
                        && Spells.isFriend(p, target)
                        && Beyonder.hasSequence(p, 3)
                        && Zone.covers(p, "white_angel_domain", target.position()))
        && (effect.equals(MobEffects.WITHER)
            || effect.equals(MobEffects.POISON)
            || effect.equals(MobEffects.DARKNESS))) e.setResult(Result.DENY);
  }

  // Returning true cancels Forge's death event. Spending half the maximum pool prevents chaining.
  @SubscribeEvent(priority = Priority.HIGH)
  public static boolean death(LivingDeathEvent e) {
    if (!(e.getEntity() instanceof ServerPlayer p)
        || !innate(p, 0)
        || e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
    float half = Beyonder.maxSpirituality(p) * .5f;
    if (SoulCore.getSpirituality(p) < half) return false;
    SoulCore.setSpirituality(p, SoulCore.getSpirituality(p) - half);
    p.setHealth(p.getMaxHealth() * .5f);
    Invulnerable.grant(p, 60);
    WhiteAngelFx.ring(p.level(), p.position(), "purification_ring", 10, 30);
    for (var enemy : Spells.enemiesAround(p, p.level(), p.position(), 10))
      WhiteAngel.strike(p, enemy, 60);
    WhiteAngelFx.sound(p, "inextinguishable_light");
    return true;
  }

  static void clear(Player p) {
    HUNTERS.remove(p.getUUID());
    EntitySight.clear(p);
    Physique.remove(p, Attributes.MAX_HEALTH, Physique.id("white_angel_health"));
    SupernaturalResistance.end(p, "white_angel_innate");
    ConjuredArmor.takeOff(p, "white_angel_armor");
    ConjuredWeapon.putAway(p, "white_angel_mace");
    HudCounter.hide(p, "white_angel_pacts");
    HudCounter.hide(p, "white_angel_believers");
  }
}
