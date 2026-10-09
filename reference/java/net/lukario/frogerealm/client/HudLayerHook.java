package net.lukario.frogerealm.client;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;

/**
 * Single place where the mod draws on the HUD.
 *
 * Forge 1.21 (51.x) has no working "render HUD" event, so we add our own layer to vanilla's
 * HUD layer list (Gui.layers). It draws on top of the vanilla HUD, is hidden with F1,
 * and still shows while chat/inventory is open.
 * If that ever fails (e.g. a Forge update renames things) it falls back to drawing
 * during the chat layer, which works but hides while the chat screen is open.
 *
 * To draw something new on the HUD, call your render method from renderAll().
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class HudLayerHook {

    private static boolean injected = false;
    private static boolean injectionFailed = false;

    private static void renderAll(GuiGraphics guiGraphics) {
        AspectHudIcon.render(guiGraphics);
        ScreenImageRenderer.render(guiGraphics);
        AbilityHudOverlay.render(guiGraphics);
        HudTimerOverlay.render(guiGraphics);
    }

    @SubscribeEvent
    public static void onHudLayerHookClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            ClientAspectData.setAspect("none"); // left the world
        }

        if (injected || injectionFailed || minecraft.gui == null) return;
        try {
            Field layersField = ObfuscationReflectionHelper.findField(Gui.class, "layers");
            LayeredDraw layers = (LayeredDraw) layersField.get(minecraft.gui);
            layers.add(
                    new LayeredDraw().add((guiGraphics, deltaTracker) -> renderAll(guiGraphics)),
                    () -> !minecraft.options.hideGui
            );
            injected = true;
        } catch (Exception e) {
            ForgeRealm.LOGGER.error("ForgeRealm: couldn't add HUD layer, falling back to the chat layer", e);
            injectionFailed = true;
        }
    }

    @SubscribeEvent
    public static void onHudLayerHookChatFallback(CustomizeGuiOverlayEvent.Chat event) {
        if (!injectionFailed) return;
        renderAll(event.getGuiGraphics());
    }
}
