package net.lukario.circleofinevitability.client;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.gui.overlay.ForgeLayer;
import net.minecraftforge.client.gui.overlay.ForgeLayeredDraw;
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
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class HudLayerHook {

    private static final Identifier LAYER = Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "hud");

    private static void renderAll(GuiGraphicsExtractor guiGraphics) {
        PathwayHudIcon.render(guiGraphics);
        ScreenImageRenderer.render(guiGraphics);
        AbilityHudOverlay.render(guiGraphics);
        SpiritualityHud.render(guiGraphics);
        HudTimerOverlay.render(guiGraphics);
        HudCounterOverlay.render(guiGraphics);
    }

    @SubscribeEvent
    public static void onHudLayerHookClientTick(TickEvent.ClientTickEvent.Post event) {

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            ClientPathwayData.setPathway("none"); // left the world
            ClientPathwayData.setProgress(9, 0);
            SpiritualityHud.clear();
        }
    }

    // F1 is Hud.isHidden() on 26.2 (was options.hideGui).
    @SubscribeEvent
    public static void onHudLayerHookAddLayers(AddGuiOverlayLayersEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ForgeLayeredDraw layers = event.getLayeredDraw();
        liftLayers(layers, ForgeLayeredDraw.HOTBAR_AND_DECOS,
                ForgeLayeredDraw.BACKGROUND, ForgeLayeredDraw.CONTEXTUAL_INFO, ForgeLayeredDraw.EXPERIENCE_LEVEL,
                ForgeLayeredDraw.HEALTH_BAR, ForgeLayeredDraw.VEHICLE_HEALTH, ForgeLayeredDraw.SELECTED_ITEM_NAME);
        liftLayers(layers, ForgeLayeredDraw.POST_SLEEP_STACK, ForgeLayeredDraw.HOTBAR_MESSAGE);
        layers.addWithCondition(LAYER,
                (guiGraphics, deltaTracker) -> renderAll(guiGraphics),
                () -> !minecraft.gui.hud.isHidden());
    }

    private static void liftLayers(ForgeLayeredDraw layers, Identifier stackId, Identifier... layerIds) {
        ForgeLayeredDraw stack = layers.getChild(stackId);
        if (stack == null) return;

        for (Identifier layerId : layerIds) {
            ForgeLayer layer = stack.getLayer(layerId);
            if (layer == null) continue;

            layers.replace(stackId, layerId, (graphics, deltaTracker) -> {
                graphics.pose().pushMatrix();
                try {
                    graphics.pose().translate(0, -SpiritualityHud.hotbarHudOffset());
                    layer.extract(graphics, deltaTracker);
                } finally {
                    graphics.pose().popMatrix();
                }
            });
        }
    }
}
