package net.lukario.frogerealm.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.GameType;

import java.util.HashMap;
import java.util.Map;

/**
 * Small icon above the hunger bar showing the player's current aspect.
 * Drawn every frame by HudLayerHook; disappears when the aspect has no icon (or is "none").
 *
 * To give another aspect an icon: add a PNG to assets/forgerealmmod/textures/gui/aspect_icons/
 * and one line in the static block below, using the exact name from /soul setAspect.
 */
public class AspectHudIcon {

    private static final int ICON_SIZE = 16; // on-screen size in GUI pixels (any square PNG works)

    private static final Map<String, ResourceLocation> ICONS = new HashMap<>();

    static {
        ICONS.put("Attendant Of Mysteries", icon("attendant_of_mysteries"));
    }

    private static ResourceLocation icon(String fileName) {
        return ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "textures/gui/aspect_icons/" + fileName + ".png");
    }

    public static void render(GuiGraphics guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        if (minecraft.gameMode != null && minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) return;

        ResourceLocation texture = ICONS.get(ClientAspectData.getAspect());
        if (texture == null) return;

        // Vanilla hunger bar: right edge at screen center + 91, top at 39 px above the bottom
        int x = guiGraphics.guiWidth() / 2 + 91 - ICON_SIZE;
        int y = guiGraphics.guiHeight() - 39 - ICON_SIZE - 1;

        // move up when the air bubbles row is showing (it sits right above the hunger bar)
        if (player.isEyeInFluid(FluidTags.WATER) || player.getAirSupply() < player.getMaxAirSupply()) {
            y -= 10;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        guiGraphics.blit(texture, x, y, ICON_SIZE, ICON_SIZE, 0f, 0f, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        RenderSystem.disableBlend();
    }
}
