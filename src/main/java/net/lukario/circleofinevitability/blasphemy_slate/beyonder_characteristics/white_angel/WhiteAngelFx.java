package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.white_angel;

import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.network.*;
import net.lukario.circleofinevitability.particles.fx.*;
import net.lukario.circleofinevitability.sounds.ModSounds;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Cosmetic adapter for authored models, private guilt displays and overhead proclamations. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class WhiteAngelFx {
  private static final java.util.List<String> PREVIEWS =
      java.util.List.of(
          "sun_halo",
          "holy_mace",
          "spear_of_light",
          "golden_scales",
          "blade_of_light",
          "sunbird",
          "sunbird_incomplete",
          "sun",
          "hymn_note",
          "holy_armor",
          "kingdom_gate",
          "domain_boundary",
          "purification_ring",
          "justice_halo",
          "contract_scroll",
          "notary_seal",
          "oath_sigil",
          "holy_eye",
          "believer_sun",
          "flame_plume",
          "ray_core",
          "warning_sigil");

  @net.minecraftforge.eventbus.api.listener.SubscribeEvent
  public static void commands(net.minecraftforge.event.RegisterCommandsEvent event) {
    event
        .getDispatcher()
        .register(
            net.minecraft.commands.Commands.literal("whitefx")
                .requires(
                    net.minecraft.commands.Commands.hasPermission(
                        net.minecraft.commands.Commands.LEVEL_GAMEMASTERS))
                .then(
                    net.minecraft.commands.Commands.argument(
                            "model", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .suggests(
                            (c, b) -> {
                              b.suggest("all");
                              PREVIEWS.forEach(b::suggest);
                              return b.buildFuture();
                            })
                        .executes(
                            c -> {
                              var p = c.getSource().getPlayerOrException();
                              String pick =
                                  com.mojang.brigadier.arguments.StringArgumentType.getString(
                                      c, "model");
                              var names =
                                  pick.equals("all")
                                      ? PREVIEWS
                                      : PREVIEWS.contains(pick)
                                          ? java.util.List.of(pick)
                                          : java.util.List.<String>of();
                              for (int i = 0; i < names.size(); i++) {
                                String name = names.get(i);
                                Later.run(
                                    p.level(),
                                    i * 40,
                                    () -> {
                                      if (Spells.casterStillHere(p, p.level())) {
                                        proclaim(p, name);
                                        at(
                                            p.level(),
                                            p.position().add(p.getLookAngle().scale(3)),
                                            name,
                                            40);
                                      }
                                    });
                              }
                              return names.size();
                            })));
  }

  static void sun(ServerLevel sl, Vec3 at, double radius, int ticks) {
    ParticleShapes.model(
        sl, model("sun", ticks).scale((float) (radius * 16 / 8.8)), at.add(0, radius, 0), 0, 0, 0);
  }

  static ModelFx model(String name, int ticks) {
    return ModelFx.of("white_angel/" + name)
        .pivot(8, 8, 8)
        .glow()
        .unshaded()
        .lifetime(ticks)
        .fade(2, 4);
  }

  public static void at(ServerLevel sl, Vec3 at, String name, int ticks) {
    ParticleShapes.model(sl, model(name, ticks), at, 0, 0, 0);
  }

  public static void above(ServerLevel sl, LivingEntity e, String name, int ticks) {
    ParticleShapes.modelOn(
        sl, model(name, ticks), e, new Vec3(0, e.getBbHeight() + .4, 0), 0, 0, 0);
  }

  public static void ring(ServerLevel sl, Vec3 at, String name, double radius, int ticks) {
    ParticleShapes.model(
        sl,
        model(name, ticks)
            .scale(
                (float)
                    (radius
                        * 16
                        / (name.equals("warning_sigil")
                            ? 11
                            : name.equals("domain_boundary")
                                ? 14
                                : name.equals("justice_halo")
                                    ? 9.4
                                    : name.equals("purification_ring") ? 10.4 : 7))),
        at,
        0,
        0,
        0);
  }

  public static void sound(ServerPlayer p, String name) {
    Spells.sound(p.level(), p.position(), ModSounds.WHITE_ANGEL.get(name).get(), .7f, 1);
  }

  static void beam(ServerLevel sl, Vec3 from, Vec3 to, int ticks) {
    ParticleShapes.slashBetween(
        sl,
        SlashFx.line("white_angel/ray_streak")
            .color(0xFFFFD85B)
            .core(0xFFFFFFFF)
            .width(.3f)
            .lifetime(ticks),
        from,
        to);
  }

  static void sunbird(ServerLevel sl, net.minecraft.world.entity.player.Player p, int ticks) {
    ParticleShapes.modelOnTurning(
        sl,
        model(
                net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder.hasSequence(
                        p, 2)
                    ? "sunbird"
                    : "sunbird_incomplete",
                ticks)
            .frames(4)
            .key(5, ModelFx.pose().frame(1))
            .key(10, ModelFx.pose().frame(2))
            .key(15, ModelFx.pose().frame(3))
            .key(20, ModelFx.pose().frame(0))
            .smooth()
            .scale(1.3f),
        p,
        new Vec3(0, 1, 0),
        0,
        0,
        0);
  }

  static void armor(ServerLevel sl, LivingEntity wearer, int ticks) {
    ParticleShapes.modelOnTurning(
        sl, model("holy_armor", ticks), wearer, new Vec3(0, 1, 0), 0, 0, 0);
  }

  static void mace(ServerLevel sl, LivingEntity wearer, int ticks) {
    ParticleShapes.modelOnTurning(
        sl,
        model("holy_mace", ticks).key(0, ModelFx.pose().right(.65f)),
        wearer,
        new Vec3(0, .9, 0),
        0,
        0,
        0);
  }

  static void proclaim(ServerPlayer p, String text) {
    for (var viewer : p.level().players())
      if (viewer.distanceToSqr(p) <= 48 * 48)
        PacketHandler.sendToPlayer(new CWhiteAngelLabelPacket(p.getId(), text, 60), viewer);
  }

  static void guilt(ServerPlayer p, LivingEntity target, int level, int ticks) {
    for (var viewer : p.level().players())
      if (viewer == p || Spells.isFriend(p, viewer) && viewer.distanceToSqr(target) <= 64 * 64) {
        PacketHandler.sendToPlayer(
            new CWhiteAngelLabelPacket(target.getId(), "Guilt " + level + "/5", ticks), viewer);
        EntitySight.show(viewer, target, ticks);
      }
  }

  static void slash(ServerPlayer p, LivingEntity target, ServerLevel sl, int step) {
    ParticleShapes.slash(
        sl,
        SlashFx.arc("white_angel/light_crescent")
            .color(step == 1 ? 0xFFFFFFFF : 0xFFFFD85B)
            .core(0xFFFFFFFF)
            .radius(1.8f)
            .arc(step == 1 ? -150 : 170)
            .width(.35f)
            .lifetime(9)
            .sweep(3),
        p.position().add(0, 1, 0),
        p.getYRot(),
        0,
        step == 2 ? 65 : -25);
    sound(p, "melee_swing");
  }
}
