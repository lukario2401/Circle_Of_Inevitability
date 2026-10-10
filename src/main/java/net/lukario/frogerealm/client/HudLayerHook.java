package net.lukario.frogerealm.client;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Single place where the mod draws on the HUD.
 *
 * Forge 1.21 (51.x) had no working "render HUD" event, so the old code added its own layer to vanilla's
 * HUD layer list (Gui.layers) by reflection, with a chat-layer fallback.
 * Forge 65 (26.2) has a real hook: AddGuiOverlayLayersEvent, fired once when the HUD is built.
 * The layer goes at the end of the root stack, so as before it draws on top of the vanilla HUD,
 * is hidden with F1, and still shows while chat/inventory is open. There is nothing left to fall back from.
 *
 * To draw something new on the HUD, call your render method from renderAll().
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class HudLayerHook {

    private static final Identifier LAYER = Identifier.fromNamespaceAndPath(ForgeRealm.MOD_ID, "hud");

    private static void renderAll(GuiGraphicsExtractor guiGraphics) {
        AspectHudIcon.render(guiGraphics);
        ScreenImageRenderer.render(guiGraphics);
        AbilityHudOverlay.render(guiGraphics);
        HudTimerOverlay.render(guiGraphics);
    }

    @SubscribeEvent
    public static void onHudLayerHookClientTick(TickEvent.ClientTickEvent.Post event) {

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            ClientAspectData.setAspect("none"); // left the world
        }
    }

    // F1 is Hud.isHidden() on 26.2 (was options.hideGui).
    @SubscribeEvent
    public static void onHudLayerHookAddLayers(AddGuiOverlayLayersEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        event.getLayeredDraw().addWithCondition(LAYER,
                (guiGraphics, deltaTracker) -> renderAll(guiGraphics),
                () -> !minecraft.gui.hud.isHidden());
    }
}
