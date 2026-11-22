package net.lukario.circleofinevitability.client.mixin;

import net.lukario.circleofinevitability.client.ClientDetachedCamera;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    // Vanilla has already applied sensitivity, inversion and smoothing.
    @Redirect(method = "turnPlayer", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void coi$turnView(LocalPlayer player, double x, double y) {
        ClientDetachedCamera.turn(player, x, y);
    }
}
