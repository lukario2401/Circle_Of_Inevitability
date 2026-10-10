package net.lukario.frogerealm.handler;

// TODO(stage4): placeholder. The real class (reference/.../handler/ClientForgeHandler.java) polls
// client.Keybindings (stage 4) every client tick and sends the network SKeyPress... packets (stage 3).
// Re-copy it in stage 4. Note for then: TickEvent.ClientTickEvent is now TickEvent.ClientTickEvent.Pre/Post,
// and the class must keep @Mod.EventBusSubscriber(..., value = Dist.CLIENT) only once it has a listener again
// (Forge 65 refuses an @EventBusSubscriber class with no @SubscribeEvent methods).
public class ClientForgeHandler {
}
