package net.lukario.frogerealm.combat;

import net.lukario.frogerealm.network.CHudTimerPacket;
import net.lukario.frogerealm.network.PacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * A small clock next to the hotbar: a word and the time that is left ("Revive 27s"). For anything that runs
 * for a while and that the player should be able to keep an eye on: a ward, a buff, a transformation, a cooldown.
 *
 *   HudTimer.show(player, "my_class_ward", "Ward", 600, 0x9CFFD2);   // 30 seconds, in this color (0xRRGGBB)
 *   HudTimer.hide(player, "my_class_ward");                          // it ended early
 *
 * The first text is its name: showing a timer with a name that is on screen already replaces it (new time, new
 * word), so call show again to make it longer. Several timers with different names stand above one another.
 * Start the name with your class.
 *
 * It only SHOWS time: it counts down on the player's screen by itself and goes away at 0, and nothing happens
 * when it does. What the time is for is yours to do (status/DeathWard, status/Flight, Later.run...). Its last
 * five seconds are red.
 *
 * It is drawn to the right of the hotbar. Only that player sees it. It is gone when they log out.
 * (Countdown is the other clock: big numbers in the middle of the screen, and then something happens.)
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class HudTimer {

    private HudTimer() {}

    /** Shows the timer on this player's screen, or starts it again if it is there already. 'rgb' = 0xRRGGBB. */
    public static void show(Player player, String name, String label, int ticks, int rgb) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketHandler.sendToPlayer(new CHudTimerPacket(name, label, Math.max(0, ticks), rgb), serverPlayer);
        }
    }

    /** Takes the timer off this player's screen. Does nothing if it is not there. */
    public static void hide(Player player, String name) {
        show(player, name, "", 0, 0);
    }
}
