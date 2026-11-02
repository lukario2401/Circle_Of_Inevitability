package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.white_angel;

import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.*;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Justice has a saved principle, a thirty-second combat history and event-driven trials. */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class WhiteAngelJustice {
  private static final String PRINCIPLE = "white_angel_principle", HALO = "white_angel_justice";

  public enum Principle {
    MERCY,
    RETRIBUTION,
    PURITY,
    ORDER
  }

  private record Hit(UUID actor, UUID victim) {}

  private record Crime(Principle principle, long until) {}

  private record TrialBudget(long beat, int used) {}

  private static final Map<Hit, Long> HITS = new HashMap<>();
  private static final Map<UUID, List<Crime>> CRIMES = new HashMap<>();
  private static final Map<UUID, TrialBudget> TRIALS = new HashMap<>();
  private static boolean judging;
  public static final AbilityWheel WHEEL =
      WhiteAngel.wheel("justice", "Justice")
          .entry(
              "define_justice",
              WhiteAngel.icon("define_justice"),
              "Define Justice",
              3,
              0,
              0,
              WhiteAngelJustice::define)
          .notCopyable()
          .entry(
              "justice_halo",
              WhiteAngel.icon("justice_halo"),
              "Justice Halo",
              3,
              8000,
              0,
              WhiteAngelJustice::halo)
          .free(p -> Zone.isOpen(p, HALO))
          .entry(
              "judgment",
              WhiteAngel.icon("judgment"),
              "Judgment of Justice",
              3,
              6000,
              0,
              WhiteAngelJustice::judgment)
          .notCopyable()
          .entry(
              "faith_appraisal",
              WhiteAngel.icon("faith_appraisal"),
              "Faith Appraisal",
              3,
              1500,
              0,
              WhiteAngelJustice::appraise);
  private static final AbilityMenu PRINCIPLES = principles();

  static {
    Decree.addViolationListener((entity, rule) -> crime(entity, Principle.ORDER));
  }

  public static void initialize() {}

  private static AbilityMenu principles() {
    var menu = AbilityMenu.create("white_angel_principles").title("Define Justice").ring(70);
    for (var principle : Principle.values())
      menu.option(
              WhiteAngel.icon("principle_" + principle.name().toLowerCase(Locale.ROOT)),
              principle.name(),
              0,
              0,
              (p, sl) -> choose(p, principle))
          .lockedWhen(p -> !canChoose(p));
    return menu;
  }

  static Principle principle(Player p) {
    try {
      return Principle.valueOf(
          p.getPersistentData()
              .getCompoundOrEmpty("circle_of_inevitability")
              .getStringOr(PRINCIPLE, "MERCY"));
    } catch (IllegalArgumentException e) {
      return Principle.MERCY;
    }
  }

  static boolean canChoose(ServerPlayer p) {
    return WhiteAngel.own(p)
        && Beyonder.hasSequence(p, 3)
        && !Zone.isOpen(p, HALO)
        && WhiteAngel.pactCount(p) == 0;
  }

  static boolean choose(ServerPlayer p, Principle principle) {
    if (!canChoose(p)) return false;
    p.getPersistentData()
        .getCompoundOrEmpty("circle_of_inevitability")
        .putString(PRINCIPLE, principle.name());
    p.sendSystemMessage(Component.literal("Justice: " + principle.name()));
    return true;
  }

  private static AbilityWheel.Result define(ServerPlayer p, ServerLevel sl) {
    if (!canChoose(p)) {
      p.sendSystemMessage(
          Component.literal("Lower your Justice Halo and let your pacts end first."));
      return AbilityWheel.Result.REFUSED;
    }
    PRINCIPLES.open(p);
    return AbilityWheel.Result.FREE;
  }

  public static int guilt(ServerPlayer judge, LivingEntity target) {
    int score = 0;
    long now = target.level().getGameTime();
    var crimes = CRIMES.get(target.getUUID());
    if (crimes != null) {
      crimes.removeIf(c -> c.until <= now);
      score = (int) crimes.stream().filter(c -> c.principle == principle(judge)).count();
    }
    if (principle(judge) == Principle.PURITY)
      score =
          Math.max(
              score,
              WhiteAngel.purifyMultiplier(target) > 1
                  ? (target.is(net.minecraft.tags.EntityTypeTags.UNDEAD) ? 3 : 2)
                  : 0);
    if (principle(judge) == Principle.ORDER) score = Math.max(score, Disorder.of(target));
    if (target instanceof Mob mob
        && mob.getTarget() != null
        && (mob.getTarget() == judge || Spells.isFriend(judge, mob.getTarget())))
      score = Math.max(score, 1);
    return Math.min(5, score);
  }

  private static AbilityWheel.Result halo(ServerPlayer p, ServerLevel sl) {
    WhiteAngel.authorize(p);
    if (Zone.isOpen(p, HALO)) {
      Zone.close(p, HALO);
      return AbilityWheel.Result.FREE;
    }
    double radius = 20 * WhiteAngel.might(p);
    Zone.around(p, radius)
        .lasts(WhiteAngel.duration(p, 600))
        .every(10)
        .onPulse(
            left -> {
              if (!WhiteAngel.valid(p, sl)) {
                Zone.close(p, HALO);
                return;
              }
              WhiteAngelFx.ring(sl, p.position(), "justice_halo", radius, 12);
              for (var enemy : WhiteAngel.visibleAndHiddenEnemies(p, sl, p.position(), radius)) {
                int guilt = guilt(p, enemy);
                if (guilt == 0) continue;
                Enfeebled.apply(enemy, HALO, .1f * guilt, 15);
                if (guilt >= 3) NoHeal.apply(enemy, 15);
                WhiteAngelFx.guilt(p, enemy, guilt, 15);
              }
              for (var ally : WhiteAngel.friendsAt(p, sl, p.position(), radius)) {
                if (Spells.isEvil(ally) || ally.is(net.minecraft.tags.EntityTypeTags.UNDEAD))
                  Spells.push(ally, p.position(), 1, .2);
              }
            })
        .open(p, sl, HALO);
    WhiteAngelFx.sound(p, "justice_halo");
    return AbilityWheel.Result.DONE;
  }

  private static AbilityWheel.Result judgment(ServerPlayer p, ServerLevel sl) {
    var target = Spells.aimEnemy(p, sl, 32);
    if (target == null || guilt(p, target) == 0) {
      p.sendSystemMessage(Component.literal("The scales do not move."));
      return AbilityWheel.Result.REFUSED;
    }
    int guilt = guilt(p, target);
    WhiteAngelFx.above(sl, target, "golden_scales", 30);
    Later.run(
        sl,
        30,
        () -> {
          if (!WhiteAngel.valid(p, sl)
              || !target.isAlive()
              || target.level() != sl
              || p.distanceToSqr(target) > 32 * 32) return;
          judge(() -> Spells.strikeTrue(p, target, 40 + 30 * guilt));
          Falter.apply(target, "white_angel_condemned", .6f, 240);
          Enfeebled.apply(target, "white_angel_condemned", .4f, 240);
          Vulnerable.add(target, .3f, .3f, 240);
          if (target instanceof Player player)
            for (int key : KeySeal.lastUsed(player, 2)) KeySeal.seal(player, key, 240);
          WhiteAngelFx.above(sl, target, "blade_of_light", 20);
        });
    WhiteAngelFx.sound(p, "judgment");
    return AbilityWheel.Result.DONE;
  }

  private static AbilityWheel.Result appraise(ServerPlayer p, ServerLevel sl) {
    var target = Spells.aimEnemy(p, sl, 40);
    if (target == null) return AbilityWheel.Result.REFUSED;
    Dossier.tell(p, target, 3);
    p.sendSystemMessage(
        Component.literal(
            "Guilt "
                + guilt(p, target)
                + "/5; purification multiplier ×"
                + WhiteAngel.purifyMultiplier(target)));
    Appraisal.mark(p, target, "purification", 300, .2f);
    WhiteAngelFx.guilt(p, target, guilt(p, target), 300);
    WhiteAngelFx.sound(p, "faith_appraisal");
    return AbilityWheel.Result.DONE;
  }

  @SubscribeEvent
  public static void vulnerability(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
    var target = event.getEntity();
    if (!(target.level() instanceof ServerLevel sl)) return;
    int score = 0;
    for (var judge : sl.players())
      if (!Spells.isFriend(judge, target) && Zone.covers(judge, HALO, target.position()))
        score = Math.max(score, guilt(judge, target));
    // Calculate from live fields, so reapplying a beat never stacks or leaves a stale mark.
    if (score > 0) event.setAmount(event.getAmount() * (1 + .1f * score));
  }

  private static void judge(Runnable action) {
    boolean was = judging;
    judging = true;
    try {
      action.run();
    } finally {
      judging = was;
    }
  }

  static void breach(LivingEntity offender) {
    crime(offender, Principle.ORDER);
  }

  private static void crime(LivingEntity actor, Principle principle) {
    var list = CRIMES.computeIfAbsent(actor.getUUID(), k -> new ArrayList<>());
    long now = actor.level().getGameTime();
    list.removeIf(c -> c.until <= now);
    list.add(new Crime(principle, now + 600));
    if (list.size() > 30) list.remove(0);
    if (!(actor.level() instanceof ServerLevel sl)) return;
    if (actor instanceof ServerPlayer self
        && WhiteAngel.own(self)
        && Beyonder.hasSequence(self, 3)
        && principle(self) == principle) {
      SoulCore.setSpirituality(self, SoulCore.getSpirituality(self) * .9f);
      AbilityLock.apply(self, 100);
    }
    for (var owner : sl.players()) {
      if (!WhiteAngel.own(owner)
          || !Beyonder.hasSequence(owner, 2)
          || principle(owner) != principle
          || !Spells.isEnemy(owner, actor)
          || !Zone.covers(owner, HALO, actor.position())) continue;
      long beat = now / 10;
      var budget = TRIALS.get(owner.getUUID());
      int used = budget != null && budget.beat == beat ? budget.used : 0;
      if (used >= 5) continue;
      TRIALS.put(owner.getUUID(), new TrialBudget(beat, used + 1));
      int guilt = guilt(owner, actor);
      judge(() -> Spells.strikeTrue(owner, actor, 25 * guilt));
    }
  }

  @SubscribeEvent
  public static void damage(LivingDamageEvent e) {
    if (judging
        || e.getAmount() <= 0
        || !(e.getSource().getEntity() instanceof LivingEntity actor)
        || actor == e.getEntity()
        || actor.level().isClientSide()) return;
    var victim = e.getEntity();
    long now = actor.level().getGameTime();
    boolean victimAttacked =
        HITS.getOrDefault(new Hit(victim.getUUID(), actor.getUUID()), 0L) > now;
    if (!victimAttacked) crime(actor, Principle.RETRIBUTION);
    if (WhiteAngel.purifyMultiplier(actor) > 1) crime(actor, Principle.PURITY);
    boolean threat = victimAttacked || victim instanceof Mob m && m.getTarget() == actor;
    if (!threat && (!(victim instanceof net.minecraft.world.entity.monster.Monster)))
      crime(actor, Principle.MERCY);
    HITS.put(new Hit(actor.getUUID(), victim.getUUID()), now + 600);
    HITS.entrySet().removeIf(hit -> hit.getValue() <= now);
    CRIMES
        .entrySet()
        .removeIf(
            entry -> {
              entry.getValue().removeIf(c -> c.until <= now);
              return entry.getValue().isEmpty();
            });
  }

  static void clear(Player p) {
    Zone.close(p, HALO);
    TRIALS.remove(p.getUUID());
  }

  private static void forget(net.minecraft.world.entity.LivingEntity entity) {
    var id = entity.getUUID();
    CRIMES.remove(id);
    HITS.keySet().removeIf(hit -> hit.actor.equals(id) || hit.victim.equals(id));
    TRIALS.remove(id);
  }

  @SubscribeEvent
  public static void death(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
    forget(event.getEntity());
  }

  @SubscribeEvent
  public static void unload(LevelEvent.Unload e) {
    HITS.clear();
    CRIMES.clear();
    TRIALS.clear();
  }

  @SubscribeEvent
  public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
    clear(e.getEntity());
    forget(e.getEntity());
  }
}
