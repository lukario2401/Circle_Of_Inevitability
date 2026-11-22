package net.lukario.circleofinevitability.client.mixin;

import net.lukario.circleofinevitability.client.ClientDetachedCamera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Marker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Minecraft.class)
abstract class CameraPickMixin {
    @ModifyArg(method = "pick", index = 1, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;raycastHitResult(FLnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/HitResult;"))
    private Entity coi$pickFromView(Entity previous) {
        Marker view = ClientDetachedCamera.viewpoint();
        return view == null ? previous : view;
    }
}
