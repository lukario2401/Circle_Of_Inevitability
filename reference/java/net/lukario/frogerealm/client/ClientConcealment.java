package net.lukario.frogerealm.client;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * Client side of Concealment: the entities this game should not draw right now.
 * The server says who is hidden (CConcealPacket) and repeats it every few ticks while it lasts, so an entry
 * that stops being repeated (the entity unloaded, the packet got lost) runs out by itself.
 * You shouldn't need to touch this: use Concealment.hide / Concealment.reveal on the server.
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientConcealment {

    /** Without hearing from the server again, an entity is drawn again after this many ticks. */
    private static final int WITHOUT_REMINDER = 30;

    private static final Map<Integer, Long> HIDDEN_UNTIL = new HashMap<>(); // entity id -> game time it shows again

    /** ticks = how long the entity stays hidden; 0 = draw it again now. */
    public static void set(int entityId, int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ticks <= 0 || minecraft.level == null) {
            HIDDEN_UNTIL.remove(entityId);
            return;
        }
        HIDDEN_UNTIL.put(entityId, minecraft.level.getGameTime() + Math.min(ticks, WITHOUT_REMINDER));
    }

    public static boolean isHidden(Entity entity) {
        return isHidden(entity.getId());
    }

    /** Same, by entity id: for things that are drawn AT an entity, like the 3D models stuck to it. */
    public static boolean isHidden(int entityId) {
        Long until = HIDDEN_UNTIL.get(entityId);
        if (until == null) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.level.getGameTime() > until) {
            HIDDEN_UNTIL.remove(entityId);
            return false;
        }
        return true;
    }

    // Cancelling this skips the whole entity: body, armor, held items, name tag.
    // (The 3D models stuck to it are skipped in ModelFxRenderer.)
    @SubscribeEvent
    public static void onClientConcealmentRender(RenderLivingEvent.Pre<?, ?> event) {
        if (isHidden(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientConcealmentTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (Minecraft.getInstance().level == null && !HIDDEN_UNTIL.isEmpty()) HIDDEN_UNTIL.clear(); // left the world
    }
}
