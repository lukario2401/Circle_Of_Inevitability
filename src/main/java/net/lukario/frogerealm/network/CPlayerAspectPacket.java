package net.lukario.frogerealm.network;

import net.lukario.frogerealm.client.animation.PlayerAspectCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.UUID;

/**
 * Server -> Client: which aspect another player has, so this client can pick their swing animation
 * (client/animation/SwingRules). Sent by AspectSync when an aspect changes and to a player who joins.
 */
public class CPlayerAspectPacket {

    private final UUID player;
    private final String aspect;

    public CPlayerAspectPacket(UUID player, String aspect) {
        this.player = player;
        this.aspect = aspect;
    }

    public CPlayerAspectPacket(FriendlyByteBuf buffer) {
        this.player = buffer.readUUID();
        this.aspect = buffer.readUtf();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUUID(player);
        buffer.writeUtf(aspect);
    }

    public void handle(CustomPayloadEvent.Context context) {
        if (FMLEnvironment.dist.isClient()) {
            PlayerAspectCache.set(player, aspect);
        }
    }
}
