package net.lukario.circleofinevitability.camera;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The normal server interactions remain unavailable even while a camera action is bound. */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CameraInteractions {
    private CameraInteractions() {}

    private static boolean observing(PlayerEvent event) {
        return event.getEntity() instanceof ServerPlayer player && DetachedCamera.active(player);
    }

    @SubscribeEvent(priority = Priority.HIGHEST)
    public static boolean attack(AttackEntityEvent e) { return observing(e); }

    @SubscribeEvent(priority = Priority.HIGHEST)
    public static boolean mine(PlayerInteractEvent.LeftClickBlock e) { return observing(e); }

    @SubscribeEvent(priority = Priority.HIGHEST)
    public static boolean place(PlayerInteractEvent.RightClickBlock e) { return observing(e); }

    @SubscribeEvent(priority = Priority.HIGHEST)
    public static boolean use(PlayerInteractEvent.RightClickItem e) { return observing(e); }

    @SubscribeEvent(priority = Priority.HIGHEST)
    public static boolean interact(PlayerInteractEvent.EntityInteractSpecific e) { return observing(e); }
}
