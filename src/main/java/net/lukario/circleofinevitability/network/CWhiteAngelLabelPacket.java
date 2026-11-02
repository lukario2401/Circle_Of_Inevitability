package net.lukario.circleofinevitability.network;

import net.lukario.circleofinevitability.client.WhiteAngelLabels;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;

/** Private overhead text for guilt and public notary proclamations. */
public record CWhiteAngelLabelPacket(int entityId, String text, int ticks) {
  public CWhiteAngelLabelPacket(FriendlyByteBuf b) {
    this(b.readInt(), b.readUtf(256), b.readInt());
  }

  public void encode(FriendlyByteBuf b) {
    b.writeInt(entityId);
    b.writeUtf(text, 256);
    b.writeInt(ticks);
  }

  public void handle(CustomPayloadEvent.Context c) {
    if (FMLEnvironment.dist.isClient()) WhiteAngelLabels.show(entityId, text, ticks);
  }
}
