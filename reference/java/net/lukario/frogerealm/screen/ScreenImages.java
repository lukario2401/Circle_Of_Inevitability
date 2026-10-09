package net.lukario.frogerealm.screen;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.network.CScreenImagePacket;
import net.lukario.frogerealm.network.PacketHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Put pictures on a player's screen from server code (e.g. inside an ability).
 *
 * 1. Drop a PNG into  src/main/resources/assets/forgerealmmod/textures/gui/
 * 2. Call one line, using the file name without ".png" (subfolders are fine: "icons/fire"):
 *
 *    ScreenImages.show(player, "my_pic", "attendant_stack_orb", 32);                      // center, 32px, stays
 *    ScreenImages.show(player, "my_pic", "attendant_stack_orb", 32, 60);                  // center, gone after 3s
 *    ScreenImages.show(player, "my_pic", "attendant_stack_orb", ScreenAnchor.TOP_RIGHT, -10, 10, 24, ScreenImages.FOREVER);
 *    ScreenImages.hide(player, "my_pic");            // fade out now
 *    ScreenImages.hide(player, "my_pic", 20);        // fade out in 1 second
 *    ScreenImages.hide(player, "my_*");              // everything whose id starts with "my_"
 *    ScreenImages.hideAll(player);
 *
 * The id is your name for that picture: showing the same id again replaces it,
 * and you use it to hide it. Sizes/offsets are in GUI pixels (the hotbar is 182 wide).
 * Pictures fade in and out on their own and are cleared when the player leaves the world.
 */
public class ScreenImages {

    public static final int FOREVER = -1;

    /** Centered, stays until hidden. */
    public static void show(Player player, String id, String image, int size) {
        show(player, id, image, ScreenAnchor.CENTER, 0, 0, size, size, FOREVER);
    }

    /** Centered, disappears after durationTicks (20 ticks = 1 second). */
    public static void show(Player player, String id, String image, int size, int durationTicks) {
        show(player, id, image, ScreenAnchor.CENTER, 0, 0, size, size, durationTicks);
    }

    /** Square picture anywhere on screen. */
    public static void show(Player player, String id, String image, ScreenAnchor anchor, int x, int y, int size, int durationTicks) {
        show(player, id, image, anchor, x, y, size, size, durationTicks);
    }

    /** Full control: any width/height. */
    public static void show(Player player, String id, String image, ScreenAnchor anchor, int x, int y,
                            int width, int height, int durationTicks) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        PacketHandler.sendToPlayer(
                CScreenImagePacket.show(id, texture(image), anchor, x, y, width, height, durationTicks),
                serverPlayer);
    }

    /** Fade out now. The id may end with * to hide every picture whose id starts with it. */
    public static void hide(Player player, String id) {
        hide(player, id, 0);
    }

    /** Fade out after delayTicks. */
    public static void hide(Player player, String id, int delayTicks) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        PacketHandler.sendToPlayer(CScreenImagePacket.hide(id, delayTicks), serverPlayer);
    }

    public static void hideAll(Player player) {
        hide(player, "*");
    }

    /** "attendant_stack_orb" -> forgerealmmod:textures/gui/attendant_stack_orb.png ; full ids like "minecraft:textures/..." also work */
    public static ResourceLocation texture(String image) {
        if (image.contains(":")) {
            return ResourceLocation.parse(image);
        }
        return ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, "textures/gui/" + image + ".png");
    }
}
