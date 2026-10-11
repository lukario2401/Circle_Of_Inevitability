package net.lukario.circleofinevitability.client;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Client side of Concealment: the entities this game should not draw right now.
 * The server says who is hidden (CConcealPacket) and repeats it every few ticks while it lasts, so an entry
 * that stops being repeated (the entity unloaded, the packet got lost) runs out by itself.
 * You shouldn't need to touch this: use Concealment.hide / Concealment.reveal on the server.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
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
    // (26.2: returning true cancels. The event no longer carries the entity, only what will be drawn: its type and
    // the spot it is drawn at this frame, which lies between where it was last tick and where it is now. A hidden
    // entity of the same type drawn in that span is the one being drawn.)
    @SubscribeEvent
    public static boolean onClientConcealmentRender(RenderLivingEvent.Pre<?, ?, ?> event) {
        return isHidden(event.getState());
    }

    private static boolean isHidden(EntityRenderState state) {
        if (HIDDEN_UNTIL.isEmpty()) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) return false;
        long now = minecraft.level.getGameTime();
        for (Iterator<Map.Entry<Integer, Long>> it = HIDDEN_UNTIL.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Long> hidden = it.next();
            if (now > hidden.getValue()) { // ran out, same as isHidden(int)
                it.remove();
                continue;
            }
            Entity entity = minecraft.level.getEntity(hidden.getKey());
            if (entity != null && entity.getType() == state.entityType
                    && between(state.x, entity.xOld, entity.getX())
                    && between(state.y, entity.yOld, entity.getY())
                    && between(state.z, entity.zOld, entity.getZ())) return true;
        }
        return false;
    }

    private static boolean between(double value, double a, double b) {
        return value >= Math.min(a, b) - 1.0E-4 && value <= Math.max(a, b) + 1.0E-4;
    }

    @SubscribeEvent
    public static void onClientConcealmentTick(TickEvent.ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null && !HIDDEN_UNTIL.isEmpty()) HIDDEN_UNTIL.clear(); // left the world
    }
}
