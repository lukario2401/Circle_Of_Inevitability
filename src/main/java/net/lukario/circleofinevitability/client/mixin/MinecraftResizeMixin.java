package net.lukario.circleofinevitability.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 26.2 / Forge 65.1.0 crash workaround: if the window is resized (maximized by the window manager, snapped,
 * dragged) while Forge's loading screen is up, resizeGui runs before Minecraft has created its Gui and throws a
 * NullPointerException ("Window resize"). Skip it until the Gui exists; the game sizes its GUI once loading finishes.
 */
@Mixin(Minecraft.class)
abstract class MinecraftResizeMixin {
    @Shadow @Final public Gui gui;

    @Inject(method = "resizeGui", at = @At("HEAD"), cancellable = true)
    private void coi$skipResizeWhileLoading(CallbackInfo info) {
        if (gui == null) info.cancel();
    }
}
