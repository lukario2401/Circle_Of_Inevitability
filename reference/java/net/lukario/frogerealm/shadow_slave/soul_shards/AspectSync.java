package net.lukario.frogerealm.shadow_slave.soul_shards;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.network.CSyncAspectPacket;
import net.lukario.frogerealm.network.PacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps the client's copy of the player's aspect up to date.
 * Checks every tick and only sends a packet when the aspect actually changed
 * (join, /soul setAspect, or anything else that calls SoulCore.setAspect).
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AspectSync {

    private static final Map<UUID, String> LAST_SENT_ASPECT = new HashMap<>();

    @SubscribeEvent
    public static void onAspectSyncPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        String aspect = SoulCore.getAspect(player);
        if (!aspect.equals(LAST_SENT_ASPECT.get(player.getUUID()))) {
            LAST_SENT_ASPECT.put(player.getUUID(), aspect);
            PacketHandler.sendToPlayer(new CSyncAspectPacket(aspect), player);
        }
    }

    @SubscribeEvent
    public static void onAspectSyncPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // so they get a fresh sync next time they join
        LAST_SENT_ASPECT.remove(event.getEntity().getUUID());
    }
}
