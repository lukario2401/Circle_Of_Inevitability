package net.lukario.circleofinevitability.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only text displays. No server entity, target name or global glow is changed. */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability", value = Dist.CLIENT)
public final class WhiteAngelLabels {
  private record Label(Entity target, Display.TextDisplay display, long until) {}

  private static final Map<Integer, Label> LABELS = new HashMap<>();
  private static int nextId = -900000;

  private static final class LabelDisplay extends Display.TextDisplay {
    LabelDisplay(net.minecraft.client.multiplayer.ClientLevel level, String text) {
      super(EntityTypes.TEXT_DISPLAY, level);
      var tag = new net.minecraft.nbt.CompoundTag();
      var component = new net.minecraft.nbt.CompoundTag();
      component.putString("text", text);
      component.putString("color", "#ffd85b");
      tag.put("text", component);
      tag.putString("billboard", "center");
      tag.putInt("background", 0);
      readAdditionalSaveData(
          net.minecraft.world.level.storage.TagValueInput.create(
              net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), tag));
    }
  }

  public static void show(int targetId, String text, int ticks) {
    var level = Minecraft.getInstance().level;
    if (level == null) return;
    Entity target = level.getEntity(targetId);
    if (target == null) return;
    Label old = LABELS.remove(targetId);
    if (old != null) old.display.discard();
    var display = new LabelDisplay(level, text);
    display.setId(nextId--);
    display.setPos(target.getX(), target.getY() + target.getBbHeight() + .6, target.getZ());
    level.addEntity(display);
    LABELS.put(targetId, new Label(target, display, level.getGameTime() + Math.min(6000, ticks)));
  }

  @SubscribeEvent
  public static void tick(TickEvent.ClientTickEvent.Post e) {
    var level = Minecraft.getInstance().level;
    LABELS
        .entrySet()
        .removeIf(
            entry -> {
              var label = entry.getValue();
              if (level == null
                  || label.target.level() != level
                  || !label.target.isAlive()
                  || label.until <= level.getGameTime()) {
                label.display.discard();
                return true;
              }
              label.display.setPos(
                  label.target.getX(),
                  label.target.getY() + label.target.getBbHeight() + .6,
                  label.target.getZ());
              return false;
            });
  }
}
