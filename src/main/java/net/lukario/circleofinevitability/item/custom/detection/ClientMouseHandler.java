package net.lukario.circleofinevitability.item.custom.detection;

import net.lukario.circleofinevitability.item.custom.detection.ClickState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import static net.lukario.circleofinevitability.item.custom.detection.ClickState.leftClickPressed;
import static net.lukario.circleofinevitability.item.custom.detection.ClickState.rightClickPressed;

@Mod.EventBusSubscriber(modid = "circle_of_inevitability", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientMouseHandler {

    @SubscribeEvent
    public static void onMouseClickPre(InputEvent.MouseButton.Pre event) {
        // Determine button pressed/released
        boolean pressed = event.getAction() == GLFW.GLFW_PRESS;
        boolean released = event.getAction() == GLFW.GLFW_RELEASE;

        // LEFT mouse button
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (pressed) leftClickPressed = true;
            if (released) leftClickPressed = false;
        }

        // RIGHT mouse button
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            if (pressed) rightClickPressed = true;
            if (released) rightClickPressed = false;
        }
    }
}
