package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.camera.DetachedCamera;
import net.lukario.circleofinevitability.item.custom.abilitys.DashHandler;
import net.lukario.circleofinevitability.item.custom.abilitys.star_burst.StarBurstHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

/** Dash and Star Burst historically read the singleplayer host's keyboard. Only the host can use these shortcuts. */
public record SLegacyAbilityPacket(boolean dash, boolean burst) {
    public SLegacyAbilityPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(dash);
        buf.writeBoolean(burst);
    }

    public void handle(CustomPayloadEvent.Context context) {
        ServerPlayer player = context.getSender();
        if (player == null || !player.isAlive() || DetachedCamera.active(player)) return;
        var server = player.level().getServer();
        if (!server.isSingleplayer() || !server.isSingleplayerOwner(player.nameAndId())) return;
        if (dash) DashHandler.dash(player);
        if (burst) StarBurstHandler.starBurst(player);
    }
}
