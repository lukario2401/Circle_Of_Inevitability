package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.network.CHudCounterPacket;
import net.lukario.circleofinevitability.network.PacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * A small counter next to the hotbar: a picture, a word and a number out of a cap ("[sigil] Order 4/9"). For anything
 * a Pathway counts and the player should keep an eye on: the Prince Of Abolition's standing orders, charges, stacks
 * that are better read as a number than as a row of pictures.
 *
 *   HudCounter.show(player, "abolition_order", "prince_of_abolition/order", "Order", 4, 9, 0xE0B04A);
 *   HudCounter.hide(player, "abolition_order");
 *
 * The first text is its name: showing a counter with a name that is on screen already replaces it, so call show
 * again whenever the number changes. The picture is under textures/gui/ (like ScreenImages: without .png); "" draws
 * none. max <= 0 shows the number alone ("Order 4"). Several counters with different names stand one above another,
 * to the LEFT of the hotbar (the clocks of HudTimer are on its right). Only that player sees it; it stays until it is
 * hidden or they log out. It only SHOWS a number: what the number does is the Pathway's to do.
 */
public final class HudCounter {

    private HudCounter() {}

    /** Shows (or updates) the counter on this player's screen. 'rgb' = 0xRRGGBB, the color of the number. */
    public static void show(Player player, String name, String image, String label, int value, int max, int rgb) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketHandler.sendToPlayer(new CHudCounterPacket(name, image, label, value, max, rgb, true), serverPlayer);
        }
    }

    /** Takes the counter off this player's screen. Does nothing if it is not there. */
    public static void hide(Player player, String name) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketHandler.sendToPlayer(new CHudCounterPacket(name, "", "", 0, 0, 0, false), serverPlayer);
        }
    }
}
