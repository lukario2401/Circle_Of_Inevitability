package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.white_angel;

import java.util.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.particles.fx.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sun pathway keys and shared ability services. All temporary state is server-owned and cleared on
 * exit.
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class WhiteAngel {
  public static final String PATHWAY = "White Angel";
  public static final MeleeCombo COMBO =
      MeleeCombo.forPathway(PATHWAY)
          .step((p, t, sl) -> WhiteAngelFx.slash(p, t, sl, 0))
          .step((p, t, sl) -> WhiteAngelFx.slash(p, t, sl, 1))
          .step((p, t, sl) -> WhiteAngelFx.slash(p, t, sl, 2));
  private static final String PREFIX = "white_angel_";
  private static final Map<UUID, Long> COURAGE = new HashMap<>();

  private record Pact(
      ServerPlayer owner, LivingEntity target, long until, boolean holy, UUID group) {}

  private record Offer(ServerPlayer owner, long until) {}

  private static final List<Pact> PACTS = new ArrayList<>();
  private static final Map<UUID, Offer> OFFERS = new HashMap<>();

  private record Active(net.minecraft.resources.ResourceKey<Level> dimension, int sequence) {}

  private static final Map<UUID, Active> ACTIVE = new HashMap<>();

  private record Borrowed(String pathway, Active state, long until) {}

  private static final Map<UUID, Borrowed> BORROWED = new HashMap<>();

  static void authorize(ServerPlayer p) {
    var state = new Active(p.level().dimension(), SoulCore.getSequence(p));
    if (own(p)) ACTIVE.putIfAbsent(p.getUUID(), state);
    else if (Beyonder.isCastingUnbound(p)) {
      BORROWED.put(
          p.getUUID(), new Borrowed(SoulCore.getPathway(p), state, p.level().getGameTime() + 2100));
      ACTIVE.putIfAbsent(p.getUUID(), state);
    }
  }

  private static boolean borrowed(Player p) {
    var b = BORROWED.get(p.getUUID());
    return b != null
        && b.until > p.level().getGameTime()
        && b.pathway.equals(SoulCore.getPathway(p))
        && b.state.equals(new Active(p.level().dimension(), SoulCore.getSequence(p)));
  }

  static AbilityWheel wheel(String id, String name) {
    return AbilityWheel.create(PREFIX + id, name)
        .pathway(PATHWAY)
        .quickCastCooldown(false)
        .quickCastCost(1f)
        .onCast((p, sl, e) -> authorize(p));
  }

  static String icon(String id) {
    return "ability_menus/white_angel/" + id;
  }

  public static final AbilityWheel THEURGY =
      wheel("theurgy", "Theurgy")
          .entry(
              "holy_light",
              icon("holy_light"),
              "Holy Light Summoning",
              7,
              900,
              0,
              WhiteAngel::holyLight)
          .entry(
              "sun_halo",
              icon("sun_halo"),
              "Sun Halo",
              7,
              700,
              0,
              (p, sl) -> field(p, sl, "halo", Beyonder.hasSequence(p, 5) ? 16 : 12, 400))
          .free(p -> Zone.isOpen(p, PREFIX + "halo"))
          .entry("holy_oath", icon("holy_oath"), "Holy Oath", 7, 600, 0, WhiteAngel::oath)
          .entry("cleave", icon("cleave"), "Cleave of Purification", 7, 500, 0, WhiteAngel::cleave)
          .entry(
              "fire_of_light",
              icon("fire_of_light"),
              "Fire of Light",
              7,
              1100,
              0,
              WhiteAngel::fire);
  public static final AbilityWheel NOTARY =
      wheel("notary", "Notarization")
          .entry("contract", icon("contract"), "Contract", 6, 1500, 0, WhiteAngel::contract)
          .notCopyable()
          .entry(
              "authenticate",
              icon("authenticate"),
              "Authenticate",
              6,
              800,
              0,
              WhiteAngel::authenticate)
          .entry("amplify", icon("amplify"), "Amplify", 6, 600, 0, WhiteAngel::amplify)
          .costBy((p, c) -> Beyonder.hasSequence(p, 0) ? c / 4 : c)
          .entry("nullify", icon("nullify"), "Nullify", 6, 700, 0, WhiteAngel::nullify)
          .costBy((p, c) -> Beyonder.hasSequence(p, 0) ? c / 4 : c);
  public static final AbilityWheel RADIANCE =
      wheel("radiance", "Radiance")
          .entry(
              "purification",
              icon("purification"),
              "Purification",
              4,
              3000,
              0,
              WhiteAngel::purification)
          .entry(
              "domain",
              icon("domain"),
              "Unshadowed Domain",
              4,
              4000,
              0,
              (p, sl) -> field(p, sl, "domain", 14, 300))
          .free(p -> Zone.isOpen(p, PREFIX + "domain"))
          .entry(
              "flaring_sun",
              icon("flaring_sun"),
              "Flaring Sun",
              4,
              4500,
              0,
              WhiteAngelRadiance::flaringSun)
          .entry("spear", icon("spear"), "Unshadowed Spear", 4, 3000, 0, WhiteAngelRadiance::spear)
          .entry(
              "white_ray",
              icon("white_ray"),
              "Pure White Ray",
              4,
              3500,
              0,
              WhiteAngelRadiance::whiteRay)
          .entry(
              "holy_equipment",
              icon("holy_equipment"),
              "Holy Equipment",
              4,
              3500,
              0,
              WhiteAngelRadiance::equipment);

  static {
    Cooldowns.addFree(Cooldowns.keysStartingWith(PREFIX));
    Fear.addImmunity(
        e -> active(COURAGE, e) || e instanceof Player p && own(p) && Beyonder.hasSequence(p, 7));
    Corrosion.addImmunity(WhiteAngel::sheltered);
    Decay.addImmunity(WhiteAngel::sheltered);
    Misfortune.addImmunity(WhiteAngel::sheltered);
  }

  private static boolean sheltered(LivingEntity e) {
    if (!(e.level() instanceof ServerLevel sl)) return false;
    return sl.players().stream()
        .anyMatch(
            p ->
                own(p) && Spells.isFriend(p, e) && Zone.covers(p, PREFIX + "domain", e.position()));
  }

  public static void initialize() {
    WhiteAngelJustice.initialize();
    WhiteAngelSun.initialize();
  }

  private static boolean active(Map<UUID, Long> state, LivingEntity e) {
    return state.getOrDefault(e.getUUID(), 0L) > e.level().getGameTime();
  }

  static double might(Player p) {
    return 1 + .05 * (9 - SoulCore.getSequence(p));
  }

  static int duration(Player p, int ticks) {
    return (int) (ticks * might(p));
  }

  static boolean own(Player p) {
    return SoulCore.getPathway(p).equals(PATHWAY);
  }

  static boolean valid(Player p, ServerLevel sl) {
    return p.isAlive() && p.level() == sl && (own(p) || borrowed(p));
  }

  static List<LivingEntity> friends(ServerPlayer p, ServerLevel sl, double radius) {
    var list = new ArrayList<>(friendsAt(p, sl, p.position(), radius));
    list.add(p);
    return list;
  }

  static List<LivingEntity> friendsAt(ServerPlayer p, ServerLevel sl, Vec3 at, double radius) {
    return Spells.livingAround(p, sl, at, radius).stream()
        .filter(e -> Spells.isFriend(p, e))
        .toList();
  }

  static List<LivingEntity> visibleAndHiddenEnemies(
      ServerPlayer p, ServerLevel sl, Vec3 at, double radius) {
    return Spells.livingAround(p, sl, at, radius).stream()
        .filter(e -> !Spells.isFriend(p, e))
        .toList();
  }

  public static void key(Player p, Level level, ServerLevel sl, int key, boolean bypass) {
    if (!(p instanceof ServerPlayer sp)
        || (!bypass && !own(p))
        || key < 1
        || key > 7
        || !Beyonder.hasSequence(p, new int[] {0, 9, 8, 7, 6, 4, 3, 2}[key])) return;
    authorize(sp);
    if (key >= 3) {
      var wheel =
          key == 3
              ? THEURGY
              : key == 4
                  ? NOTARY
                  : key == 5 ? RADIANCE : key == 6 ? WhiteAngelJustice.WHEEL : WhiteAngelSun.WHEEL;
      if (p.isShiftKeyDown()) wheel.quickCast(sp);
      else wheel.open(sp);
      return;
    }
    if (key == 2 && p.isShiftKeyDown() && Zone.isOpen(p, PREFIX + "daytime")) {
      Zone.close(p, PREFIX + "daytime");
      return;
    }
    int cost =
        key == 1
            ? Math.min(250, (int) (Beyonder.maxSpirituality(p) * .25f))
            : p.isShiftKeyDown()
                ? Math.min(1250, (int) (Beyonder.maxSpirituality(p) * .625f))
                : Math.min(600, (int) (Beyonder.maxSpirituality(p) * .25f));
    if (!Spells.paySpirituality(p, cost)) return;
    if (key == 1) hymn(sp, sl, p.isShiftKeyDown());
    else if (p.isShiftKeyDown()) field(sp, sl, "daytime", 16, 400);
    else {
      Vec3 at = Spells.aimGround(p, sl, 24);
      WhiteAngelFx.at(sl, at, "sun", 20);
      for (var e : Spells.enemiesAround(p, sl, at, 6)) {
        strike(sp, e, 14);
        Blind.apply(e, 80);
        e.igniteForSeconds(6);
      }
      WhiteAngelFx.sound(sp, "sunshine");
    }
  }

  public static float purifyMultiplier(LivingEntity e) {
    return (e.is(EntityTypeTags.UNDEAD) ? 2.5f : Spells.isEvil(e) ? 1.5f : 1)
        * (Corrosion.isCorroding(e) || Decay.isDecaying(e) || Madness.get(e) > 0 ? 1.5f : 1);
  }

  static void strike(ServerPlayer p, LivingEntity e, float amount) {
    boolean sunlight =
        Nocturnality.inDaySun(p)
            || Zone.isOpen(p, PREFIX + "daytime")
            || Zone.isOpen(p, PREFIX + "domain")
            || Zone.isOpen(p, PREFIX + "halo");
    Appraisal.strike(p, e, amount * purifyMultiplier(e) * (sunlight ? 1.15f : 1), "purification");
  }

  static void courage(LivingEntity e, int ticks) {
    Fear.end(e);
    COURAGE.merge(e.getUUID(), e.level().getGameTime() + ticks, Math::max);
  }

  private static void hymn(ServerPlayer p, ServerLevel sl, boolean renewal) {
    for (var e : friends(p, sl, 12 * might(p))) {
      int bond = Marionette.isMarionetteOf(e, p) ? 2 : 1;
      if (renewal) {
        Spells.keepEffect(e, MobEffects.REGENERATION, 0, bond * duration(p, 200));
        Spells.keepEffect(e, MobEffects.SPEED, 0, bond * duration(p, 400));
        if (e instanceof Player ally && e != p)
          Spells.giveSpirituality(ally, Beyonder.maxSpirituality(ally) * .05f);
      } else {
        courage(e, bond * duration(p, 400));
        Spells.keepEffect(e, MobEffects.STRENGTH, 0, bond * duration(p, 400));
      }
      if (Beyonder.hasSequence(p, 3)) WhiteAngelBlessings.ward(e, duration(p, 400));
      if (Beyonder.hasSequence(p, 1)) WhiteAngelBlessings.fervor(e, duration(p, 100));
      WhiteAngelFx.above(sl, e, "hymn_note", 40);
    }
    WhiteAngelFx.sound(p, renewal ? "hymn_of_renewal" : "hymn_of_valor");
  }

  private static AbilityWheel.Result field(
      ServerPlayer p, ServerLevel sl, String name, double base, int ticks) {
    authorize(p);
    if (Zone.isOpen(p, PREFIX + name)) {
      Zone.close(p, PREFIX + name);
      return AbilityWheel.Result.FREE;
    }
    double max = base * might(p);
    int life = duration(p, ticks);
    Zone.around(p, max)
        .lasts(life)
        .every(name.equals("domain") ? 10 : 20)
        .onPulse(
            left -> {
              if (!valid(p, sl)) {
                Zone.close(p, PREFIX + name);
                return;
              }
              double r =
                  name.equals("daytime")
                      ? Math.min(max, (10 + (life - left) / 20.) * might(p))
                      : max;
              Nocturnality.forceDay(sl, p.position(), r, name.equals("domain") ? 10 : 20);
              WhiteAngelFx.ring(
                  sl, p.position(), name.equals("domain") ? "domain_boundary" : "sun_halo", r, 22);
              for (var e : friends(p, sl, r)) {
                WhiteAngelBlessings.ward(e, 25);
                Spells.keepEffect(e, MobEffects.NIGHT_VISION, 0, 220);
                if (name.equals("domain") || name.equals("halo") && Beyonder.hasSequence(p, 5))
                  clean(e);
                else if (name.equals("halo")) {
                  Blind.remove(e);
                  for (var effect :
                      List.of(MobEffects.NAUSEA, MobEffects.WITHER, MobEffects.DARKNESS))
                    e.removeEffect(effect);
                }
              }
              for (var e : visibleAndHiddenEnemies(p, sl, p.position(), r)) {
                if (name.equals("domain")) Exposed.apply(e, 25);
                else if (!Spells.isEnemy(p, e)) continue;
                if (purifyMultiplier(e) > 1
                    && (!name.equals("halo") || Beyonder.hasSequence(p, 5))) {
                  strike(p, e, name.equals("halo") ? 4 : name.equals("domain") ? 2 : 3);
                  e.igniteForSeconds(2);
                }
              }
            })
        .open(p, sl, PREFIX + name);
    WhiteAngelFx.sound(p, name.equals("halo") ? "sun_halo" : name);
    return AbilityWheel.Result.DONE;
  }

  private static AbilityWheel.Result holyLight(ServerPlayer p, ServerLevel sl) {
    LivingEntity primary = Spells.aimEnemy(p, sl, 28);
    Vec3 at = primary == null ? Spells.aimGround(p, sl, 28) : primary.position();
    boolean high = Beyonder.hasSequence(p, 5);
    WhiteAngelFx.ring(sl, at, "warning_sigil", high ? 4 : 2.5, 8);
    Later.run(
        sl,
        8,
        () -> {
          if (!valid(p, sl)) return;
          WhiteAngelFx.at(sl, at, "blade_of_light", 15);
          for (var e : Spells.enemiesAround(p, sl, at, high ? 4 : 2.5)) {
            strike(p, e, high ? 90 : e == primary ? 30 : 15);
            if (high
                && !Spells.isBoss(e)
                && Spells.isEvil(e)
                && e.getHealth() < e.getMaxHealth() * .3f) Spells.strikeTrue(p, e, e.getHealth());
          }
        });
    WhiteAngelFx.sound(p, "holy_light");
    return AbilityWheel.Result.DONE;
  }

  private static AbilityWheel.Result oath(ServerPlayer p, ServerLevel sl) {
    for (var e : friends(p, sl, Beyonder.hasSequence(p, 4) ? 16 * might(p) : 0)) {
      Spells.keepEffect(e, MobEffects.STRENGTH, 0, duration(p, 600));
      Spells.keepEffect(e, MobEffects.SPEED, 0, duration(p, 600));
      WhiteAngelBlessings.oath(e, duration(p, 600));
      WhiteAngelFx.above(sl, e, "oath_sigil", 40);
    }
    WhiteAngelFx.sound(p, "holy_oath");
    return AbilityWheel.Result.DONE;
  }

  private static AbilityWheel.Result cleave(ServerPlayer p, ServerLevel sl) {
    WeaponCoat.arm(
        p,
        PREFIX + "cleave",
        8,
        duration(p, 400),
        (a, t, damage, l) -> {
          if (purifyMultiplier(t) > 1) Exposed.apply(t, 20);
          return damage + 8 * purifyMultiplier(t);
        });
    WhiteAngelFx.sound(p, "cleave");
    return AbilityWheel.Result.DONE;
  }

  private static AbilityWheel.Result fire(ServerPlayer p, ServerLevel sl) {
    Vec3 at = Spells.aimGround(p, sl, 24);
    Zone.at(at, 6 * might(p))
        .lasts(120)
        .every(10)
        .onPulse(
            left -> {
              if (!valid(p, sl)) return;
              WhiteAngelFx.at(sl, at, "flame_plume", 12);
              for (var e : visibleAndHiddenEnemies(p, sl, at, 6 * might(p))) {
                if (!Spells.isEnemy(p, e) && purifyMultiplier(e) <= 1) continue;
                Exposed.apply(e, 15);
                strike(p, e, 5);
                e.igniteForSeconds(2);
                Exposed.apply(e, 15);
              }
              for (var e : friendsAt(p, sl, at, 6 * might(p))) {
                Spells.keepEffect(e, MobEffects.FIRE_RESISTANCE, 0, 25);
                courage(e, 15);
              }
            })
        .open(p, sl, PREFIX + "fire");
    WhiteAngelFx.sound(p, "fire_of_light");
    return AbilityWheel.Result.DONE;
  }

  static int slots(ServerPlayer p) {
    return Beyonder.hasSequence(p, 1)
        ? 9
        : Beyonder.hasSequence(p, 3) ? 5 : Beyonder.hasSequence(p, 4) ? 3 : 2;
  }

  private static boolean eligible(ServerPlayer p, LivingEntity t) {
    if (t instanceof Player a)
      return SoulCore.getPathway(a).equals("none")
          || SoulCore.getSequence(a)
              >= SoulCore.getSequence(p) - (Beyonder.hasSequence(p, 3) ? 3 : 2);
    return !Spells.isBoss(t) && t.getMaxHealth() <= 150;
  }

  private static boolean bind(ServerPlayer p, LivingEntity t) {
    return bind(p, t, UUID.randomUUID(), false);
  }

  private static boolean bind(ServerPlayer p, LivingEntity t, UUID group, boolean grouped) {
    if (!Beyonder.hasSequence(p, 6)
        || !eligible(p, t)
        || (!grouped && pactCount(p) >= slots(p))
        || PACTS.stream().anyMatch(a -> a.owner == p && a.target == t)) return false;
    PACTS.add(
        new Pact(
            p, t, p.level().getGameTime() + duration(p, 600), Beyonder.hasSequence(p, 3), group));
    if (t instanceof Mob mob) {
      mob.setTarget(null);
      Forgotten.apply(mob, p, duration(p, 600));
    }
    return true;
  }

  private static AbilityWheel.Result contract(ServerPlayer p, ServerLevel sl) {
    if (pactCount(p) >= slots(p)) return AbilityWheel.Result.REFUSED;
    LivingEntity t = Spells.aimLiving(p, sl, 16, false, e -> e != p);
    if (t == null) {
      if (!Beyonder.hasSequence(p, 3)) return AbilityWheel.Result.REFUSED;
      UUID group = UUID.randomUUID();
      int count = 0;
      for (var enemy : Spells.enemiesAround(p, sl, p.position(), 16)) {
        if (enemy instanceof Mob mob
            && (Spells.isEvil(mob) || mob.getTarget() != null)
            && bind(p, enemy, group, true)) {
          WhiteAngelFx.above(sl, enemy, "contract_scroll", 40);
          if (++count == 8) break;
        }
      }
      if (count == 0) return AbilityWheel.Result.REFUSED;
    } else {
      if (!eligible(p, t) || PACTS.stream().anyMatch(a -> a.owner == p && a.target == t))
        return AbilityWheel.Result.REFUSED;
      if (t instanceof ServerPlayer target) {
        OFFERS.put(target.getUUID(), new Offer(p, sl.getGameTime() + 400));
        target.sendSystemMessage(
            Component.literal(
                p.getName().getString()
                    + " offers a truce. Use /whitepact accept within 20 seconds."));
      } else if (!bind(p, t)) return AbilityWheel.Result.REFUSED;
      WhiteAngelFx.above(sl, t, "contract_scroll", 40);
    }
    WhiteAngelFx.proclaim(p, "God bears witness to this contract.");
    WhiteAngelFx.sound(p, "contract");
    return AbilityWheel.Result.DONE;
  }

  @SubscribeEvent
  public static void commands(RegisterCommandsEvent e) {
    e.getDispatcher()
        .register(
            Commands.literal("whitepact")
                .then(
                    Commands.literal("accept")
                        .executes(
                            c -> {
                              var t = c.getSource().getPlayerOrException();
                              var o = OFFERS.remove(t.getUUID());
                              if (o == null
                                  || o.until < t.level().getGameTime()
                                  || !o.owner.isAlive()
                                  || !own(o.owner)
                                  || o.owner.level() != t.level()
                                  || o.owner.distanceToSqr(t) > 256
                                  || !bind(o.owner, t)) return 0;
                              t.sendSystemMessage(
                                  Component.literal("Truce sealed for 30 seconds."));
                              return 1;
                            })));
  }

  private static AbilityWheel.Result authenticate(ServerPlayer p, ServerLevel sl) {
    for (var e : visibleAndHiddenEnemies(p, sl, p.position(), 12 * might(p))) {
      if (Dispel.isConjured(e)) Dispel.unmake(e);
      else {
        Exposed.apply(e, 80);
        EntitySight.show(p, e, 60);
      }
    }
    for (var thing : Spells.looseThingsAround(p, sl, p.position(), 12 * might(p)))
      if (Dispel.isConjured(thing)) Dispel.unmake(thing);
    WhiteAngelFx.proclaim(p, "God says it is authentic!");
    WhiteAngelFx.above(sl, p, "holy_eye", 60);
    WhiteAngelFx.sound(p, "authenticate");
    return AbilityWheel.Result.DONE;
  }

  private static AbilityWheel.Result amplify(ServerPlayer p, ServerLevel sl) {
    var t = Spells.aimAlly(p, sl, 24);
    if (t == null) t = p;
    if (!(t instanceof Player ally)) return AbilityWheel.Result.REFUSED;
    NextCast.arm(
        ally, Beyonder.hasSequence(p, 4) ? 400 : 200, 1, Beyonder.hasSequence(p, 4) ? 2 : 1.5f);
    WhiteAngelFx.proclaim(p, "God says it's effective!");
    WhiteAngelFx.sound(p, "amplify");
    return AbilityWheel.Result.DONE;
  }

  private static AbilityWheel.Result nullify(ServerPlayer p, ServerLevel sl) {
    var at = Spells.aimPoint(p, sl, 24);
    var shots = Spells.hostileShotsAround(p, sl, at, 3);
    if (!shots.isEmpty()) shots.get(0).discard();
    else {
      if (Beyonder.hasSequence(p, 4)) {
        var shells =
            Dome.crossedBy(sl, p.getEyePosition(), at).stream()
                .filter(d -> Spells.isEnemy(p, d.owner()))
                .toList();
        if (!shells.isEmpty()) {
          var d = shells.get(0);
          Dome.weaken(d.owner(), d.name(), 200);
          WhiteAngelFx.proclaim(p, "God says it's ineffective!");
          return AbilityWheel.Result.DONE;
        }
      }
      var t = Spells.aimEnemy(p, sl, 24);
      if (t == null) return AbilityWheel.Result.REFUSED;
      Dispel.boons(t);
      Dispel.statuses(t);
      if (t instanceof Player target) {
        KeySeal.seal(target, KeySeal.lastUsed(target), 120);
        NextCast.used(target);
      } else AbilityLock.apply(t, 80);
    }
    WhiteAngelFx.proclaim(p, "God says it's ineffective!");
    WhiteAngelFx.sound(p, "nullify");
    return AbilityWheel.Result.DONE;
  }

  static void clean(LivingEntity e) {
    Spells.cleanse(e);
    Decay.cure(e);
    Corrosion.remove(e);
    Madness.clear(e);
    Misfortune.clear(e);
    Slumber.wake(e);
    Bleeding.remove(e);
    Blind.remove(e);
    Fear.end(e);
    e.clearFire();
    var parasite = Parasite.parasiteIn(e);
    if (parasite != null) Parasite.leave(parasite, Parasite.End.LEFT);
  }

  private static AbilityWheel.Result purification(ServerPlayer p, ServerLevel sl) {
    var enemy = Spells.aimEnemy(p, sl, 20);
    if (enemy != null) {
      WhiteAngelFx.above(sl, enemy, "notary_seal", 60);
      Countdown.seconds(3)
          .keepWhile(
              () ->
                  valid(p, sl)
                      && enemy.isAlive()
                      && enemy.level() == sl
                      && p.distanceToSqr(enemy) <= 576)
          .onFinish(
              () -> {
                Dispel.boons(enemy);
                Dispel.statuses(enemy);
                if (enemy instanceof ServerPlayer t) {
                  SoulCore.setSpirituality(t, SoulCore.getSpirituality(t) * .65f);
                  int key = AbilityKeys.strongest(t);
                  KeySeal.seal(t, key, 240);
                } else strike(p, enemy, 40);
              })
          .start(p);
    } else {
      var ally = Spells.aimAlly(p, sl, 20);
      clean(ally == null ? p : ally);
    }
    WhiteAngelFx.sound(p, "purification");
    return AbilityWheel.Result.DONE;
  }

  @SubscribeEvent
  public static void hurt(LivingHurtEvent e) {
    var attacker = e.getSource().getEntity();
    var target = e.getEntity();
    for (var pact : new ArrayList<>(PACTS)) {
      if (pact.owner.level() != target.level()
          || pact.until <= target.level().getGameTime()
          || !pact.owner.isAlive()
          || !own(pact.owner)) continue;
      if ((attacker == pact.owner && target == pact.target)
          || (attacker == pact.target && target == pact.owner)) {
        int strength =
            attacker instanceof Player a && !SoulCore.getPathway(a).equals("none")
                ? SoulCore.getSequence(a)
                : 9;
        int unbreakable = SoulCore.getSequence(pact.owner) - (pact.holy ? 2 : 1);
        if (strength <= SoulCore.getSequence(pact.owner) - 1 && strength >= unbreakable) {
          e.setAmount(0);
          continue;
        }
        PACTS.remove(pact);
        if (attacker instanceof LivingEntity offender
            && offender.level() instanceof ServerLevel sl) {
          WhiteAngelJustice.breach(offender);
          for (int tick = 1; tick <= 120; tick += 20)
            Later.run(
                sl,
                tick,
                () -> {
                  if (offender.isAlive() && offender.level() == sl)
                    CertainDamage.strike(pact.owner, offender, 6, null);
                });
          if (offender instanceof Player a && strength < SoulCore.getSequence(pact.owner))
            SoulCore.setSpirituality(a, SoulCore.getSpirituality(a) * .75f);
          if (pact.holy)
            Later.run(
                sl,
                1,
                () -> {
                  if (offender.isAlive())
                    CertainDamage.strike(
                        pact.owner, offender, offender.getMaxHealth() * .25f, null);
                });
          WhiteAngelFx.sound(pact.owner, "pact_breach");
        }
      }
    }
  }

  @SubscribeEvent
  public static void tick(TickEvent.PlayerTickEvent.Post e) {
    if (!(e.player() instanceof ServerPlayer p)) return;
    long now = p.level().getGameTime();
    PACTS.removeIf(
        a ->
            a.until <= now
                || !a.owner.isAlive()
                || !a.target.isAlive()
                || !own(a.owner)
                || a.owner.level() != a.target.level());
    OFFERS.entrySet().removeIf(a -> a.getValue().until <= now || !a.getValue().owner.isAlive());
    COURAGE.entrySet().removeIf(a -> a.getValue() <= now);
    if (!own(p) || !p.isAlive()) {
      if (p.isAlive() && borrowed(p)) {
        EntitySight.tick(p);
        return;
      }
      if (ACTIVE.containsKey(p.getUUID())) clear(p);
      return;
    }
    var current = new Active(p.level().dimension(), SoulCore.getSequence(p));
    var previous = ACTIVE.get(p.getUUID());
    if (previous != null && !previous.equals(current)) clear(p);
    ACTIVE.put(p.getUUID(), current);
    WhiteAngelPassives.tick(p);
    if (now % 20 == 0 && Beyonder.hasSequence(p, 7))
      Spells.keepEffect(p, MobEffects.FIRE_RESISTANCE, 0, 40);
  }

  static int pactCount(Player p) {
    return (int) PACTS.stream().filter(a -> a.owner == p).map(Pact::group).distinct().count();
  }

  static void clear(Player p) {
    for (var marked : Appraisal.markedBy(p)) Appraisal.spend(p, marked);
    WhiteAngelSun.clear(p);
    WhiteAngelRadiance.clear(p);
    WhiteAngelJustice.clear(p);
    WhiteAngelPassives.clear(p);
    WhiteAngelBlessings.clear(p);
    ACTIVE.remove(p.getUUID());
    BORROWED.remove(p.getUUID());
    COURAGE.remove(p.getUUID());
    OFFERS.remove(p.getUUID());
    OFFERS.values().removeIf(o -> o.owner == p);
    PACTS.removeIf(a -> a.owner == p || a.target == p);
    for (String n : List.of("daytime", "halo", "domain", "fire", "afterglow"))
      Zone.close(p, PREFIX + n);
    WeaponCoat.disarm(p, PREFIX + "cleave");
  }

  @SubscribeEvent
  public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
    clear(e.getEntity());
  }

  @SubscribeEvent
  public static void death(LivingDeathEvent e) {
    if (e.getEntity() instanceof Player p) clear(p);
  }
}
