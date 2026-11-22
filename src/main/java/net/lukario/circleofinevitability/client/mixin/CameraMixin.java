package net.lukario.circleofinevitability.client.mixin;

import net.lukario.circleofinevitability.client.ClientDetachedCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class CameraMixin {
    @Shadow private boolean detached;
    @Shadow protected abstract void setPosition(Vec3 point);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void coi$alignDetachedView(float partial, CallbackInfo ci) {
        Marker view = ClientDetachedCamera.viewpoint();
        if (view == null) return;
        Vec3 eye = view.getPosition(partial);
        setPosition(eye);
        setRotation(view.getYRot(), view.getXRot());
        // Vanilla uses this flag to draw the local player's body.
        detached = eye.distanceToSqr(Minecraft.getInstance().player.getEyePosition(partial)) > 0.25;
    }
}
