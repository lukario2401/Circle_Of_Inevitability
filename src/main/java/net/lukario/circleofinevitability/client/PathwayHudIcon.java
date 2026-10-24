package net.lukario.circleofinevitability.client;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.GameType;

import java.util.HashMap;
import java.util.Map;

/**
 * Small icon above the hunger bar showing the player's current pathway.
 * Drawn every frame by HudLayerHook; disappears when the pathway has no icon (or is "none").
 *
 * To give another pathway an icon: add a PNG to assets/circle_of_inevitability/textures/gui/pathway_icons/
 * and one line in the static block below, using the exact name from /soul setPathway.
 */
public class PathwayHudIcon {

    private static final int ICON_SIZE = 16; // on-screen size in GUI pixels (any square PNG works)

    private static final Map<String, Identifier> ICONS = new HashMap<>();

    static {
        ICONS.put("Attendant Of Mysteries", icon("attendant_of_mysteries"));
        ICONS.put("Beauty Goddess", icon("beauty_goddess"));            // shows once the Pathway exists (docs/BEAUTY_GODDESS.md)
        ICONS.put("Hand Of God", icon("hand_of_god"));                  // shows once the Pathway exists (docs/HAND_OF_GOD.md)
        ICONS.put("Knight Of Misfortune", icon("knight_of_misfortune")); // docs/KNIGHT_OF_MISFORTUNE.md
        ICONS.put("Abomination", icon("abomination"));                  // shows once the Pathway exists (docs/ABOMINATION.md)
        ICONS.put("Prince Of Abolition", icon("prince_of_abolition"));  // docs/PRINCE_OF_ABOLITION.md
        ICONS.put("Knowledge Emperor", icon("knowledge_emperor"));      // shows once the Pathway exists (docs/KNOWLEDGE_EMPEROR.md)
        ICONS.put("Illuminator", icon("illuminator"));                  // shows once the Pathway exists (docs/ILLUMINATOR.md)
        ICONS.put("Star Dragon", icon("star_dragon"));                  // shows once the Pathway exists (docs/STAR_DRAGON.md)
        ICONS.put("Hand Of Order", icon("hand_of_order"));              // docs/HAND_OF_ORDER.md
        ICONS.put("Key Of Stars", icon("key_of_stars"));                // shows once the Pathway exists (docs/KEY_OF_STARS.md)
    }

    private static Identifier icon(String fileName) {
        return Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "textures/gui/pathway_icons/" + fileName + ".png");
    }

    public static void render(GuiGraphicsExtractor guiGraphics) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return;
        if (minecraft.gameMode != null && minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) return;

        Identifier texture = ICONS.get(ClientPathwayData.getPathway());
        if (texture == null) return;

        // Vanilla hunger bar: right edge at screen center + 91, top at 39 px above the bottom
        int x = guiGraphics.guiWidth() / 2 + 91 - ICON_SIZE;
        int y = guiGraphics.guiHeight() - 39 - ICON_SIZE - 1;

        // move up when the air bubbles row is showing (it sits right above the hunger bar)
        if (player.isEyeInFluid(FluidTags.WATER) || player.getAirSupply() < player.getMaxAirSupply()) {
            y -= 10;
        }

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, ICON_SIZE, ICON_SIZE,
                ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
    }
}
