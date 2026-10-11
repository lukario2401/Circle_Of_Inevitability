package net.lukario.frogerealm.shadow_slave.beyonder;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.network.CPlayerPathwayPacket;
import net.lukario.frogerealm.network.CSyncPathwayPacket;
import net.lukario.frogerealm.network.PacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps the client's copy of the player's pathway up to date.
 * Checks every tick and only sends a packet when the pathway actually changed
 * (join, /soul setPathway, or anything else that calls SoulCore.setPathway).
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PathwaySync {

    private static final Map<UUID, String> LAST_SENT_PATHWAY = new HashMap<>();

    @SubscribeEvent
    public static void onPathwaySyncPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;

        String pathway = SoulCore.getPathway(player);
        int sequence = SoulCore.getSequence(player);
        int digestion = Digestion.percent(player);
        // the pathway alone decides what other players are told; the HUD line also needs Sequence and digestion
        String sent = LAST_SENT_PATHWAY.get(player.getUUID());
        String now = pathway + "|" + sequence + "|" + digestion;
        if (!now.equals(sent)) {
            LAST_SENT_PATHWAY.put(player.getUUID(), now);
            PacketHandler.sendToPlayer(new CSyncPathwayPacket(pathway, sequence, digestion), player);
        }
        if (sent == null || !sent.startsWith(pathway + "|")) {
            // everyone else too, for this player's swing animations (client/animation/SwingRules)
            PacketHandler.sendToAllClients(new CPlayerPathwayPacket(player.getUUID(), pathway));
        }
    }

    @SubscribeEvent
    public static void onPathwaySyncPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer joined)) return;
        // the players already online, for their swing animations (the joiner's own pathway goes out on its first tick)
        for (ServerPlayer other : joined.level().getServer().getPlayerList().getPlayers()) {
            if (other != joined) PacketHandler.sendToPlayer(new CPlayerPathwayPacket(other.getUUID(), SoulCore.getPathway(other)), joined);
        }
    }

    @SubscribeEvent
    public static void onPathwaySyncPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        // so they get a fresh sync next time they join
        LAST_SENT_PATHWAY.remove(event.getEntity().getUUID());
    }
}
