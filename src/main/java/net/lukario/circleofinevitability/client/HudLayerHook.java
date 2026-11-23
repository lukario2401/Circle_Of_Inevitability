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

import java.util.function.IntSupplier;

/**
 * Single place where the mod draws on the HUD.
 *
 * Forge 1.21 (51.x) had no working "render HUD" event, so the old code added its own layer to vanilla's
 * HUD layer list (Gui.layers) by reflection, with a chat-layer fallback.
 * Forge 65 (26.2) has a real hook: AddGuiOverlayLayersEvent, fired once when the HUD is built.
 * The HUD widgets draw on top of the vanilla HUD and hide with F1. The corruption tint sits
 * beneath the HUD so it colors the world even when the widgets are hidden.
 *
 * To draw something new on the HUD, call your render method from renderAll().
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class HudLayerHook {

    private static final Identifier LAYER = Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "hud");
    private static final Identifier CORRUPTION_TINT = Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "corruption_tint");

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
            CorruptionTint.update(0);
        }
    }

    // F1 is Hud.isHidden() on 26.2 (was options.hideGui).
    @SubscribeEvent
    public static void onHudLayerHookAddLayers(AddGuiOverlayLayersEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        ForgeLayeredDraw layers = event.getLayeredDraw();
        layers.addBelow(CORRUPTION_TINT, ForgeLayeredDraw.PRE_SLEEP_STACK,
                (graphics, deltaTracker) -> CorruptionTint.render(graphics));
        liftLayer(layers, ForgeLayeredDraw.HOTBAR_AND_DECOS, ForgeLayeredDraw.SELECTED_ITEM_NAME,
                () -> SpiritualityHud.messageOffset(minecraft, true));
        liftLayer(layers, ForgeLayeredDraw.POST_SLEEP_STACK, ForgeLayeredDraw.HOTBAR_MESSAGE,
                () -> SpiritualityHud.messageOffset(minecraft, false));
        layers.addWithCondition(LAYER,
                (guiGraphics, deltaTracker) -> renderAll(guiGraphics),
                () -> !minecraft.gui.hud.isHidden());
    }

    private static void liftLayer(ForgeLayeredDraw layers, Identifier stackId, Identifier layerId, IntSupplier offset) {
        ForgeLayeredDraw stack = layers.getChild(stackId);
        if (stack == null) return;

        ForgeLayer layer = stack.getLayer(layerId);
        if (layer == null) return;

        layers.replace(stackId, layerId, (graphics, deltaTracker) -> {
            graphics.pose().pushMatrix();
            try {
                graphics.pose().translate(0, -offset.getAsInt());
                layer.extract(graphics, deltaTracker);
            } finally {
                graphics.pose().popMatrix();
            }
        });
    }
}
