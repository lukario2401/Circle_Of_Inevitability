package net.lukario.frogerealm.network;

import net.lukario.frogerealm.client.animation.PlayerPathwayCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.UUID;

/**
 * Server -> Client: which pathway another player has, so this client can pick their swing animation
 * (client/animation/SwingRules). Sent by PathwaySync when a pathway changes and to a player who joins.
 */
public class CPlayerPathwayPacket {

    private final UUID player;
    private final String pathway;

    public CPlayerPathwayPacket(UUID player, String pathway) {
        this.player = player;
        this.pathway = pathway;
    }

    public CPlayerPathwayPacket(FriendlyByteBuf buffer) {
        this.player = buffer.readUUID();
        this.pathway = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(player);
        buffer.writeUtf(pathway);
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist.isClient()) {
            PlayerPathwayCache.set(player, pathway);
        }
    }
}
