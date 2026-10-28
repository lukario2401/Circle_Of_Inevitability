package net.lukario.circleofinevitability.client.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * First person: while a swing plays for the local player, the main-hand item is drawn here (vanilla's resting
 * spot + the swing's pose) and vanilla's drawing of that hand, with its up-and-down swing, is skipped.
 * Everything else (the other hand, empty hands, maps, crossbows, using or blocking with an item, spin attacks,
 * scoping, frames where nothing plays) is left to vanilla untouched.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class FirstPersonSwing {

    private static boolean failed; // after one error: vanilla for the rest of the session

    private FirstPersonSwing() {}

    // Returning true cancels the event: vanilla does not draw this hand (we already did).
    @SubscribeEvent
    public static boolean onFirstPersonSwingRenderHand(RenderHandEvent event) {
        if (failed || event.getHand() != InteractionHand.MAIN_HAND) return false;
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ItemStack stack = event.getItemStack();
        if (player == null || !vanillaWouldSwingIt(player, stack)) return false;
        SwingAnimation.Pose pose = SwingPlayer.poseFor(player, event.getPartialTick());
        if (pose == null) return false;

        boolean right = player.getMainArm() == HumanoidArm.RIGHT;
        if (!right) pose = pose.mirrored();
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        try {
            applyItemTransform(poseStack, pose, right, event.getEquipProgress());
            minecraft.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, stack,
                    right ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                    poseStack, event.getNodeCollector(), event.getPackedLight());
            return true;
        } catch (RuntimeException e) {
            failed = true;
            CircleOfInevitability.LOGGER.error("Swing animations: first-person drawing failed, using vanilla swings from now on", e);
            return false;
        } finally {
            poseStack.popPose();
        }
    }

    /**
     * Moves the pose stack from the hand origin to where the item is drawn for this pose (pivot = the hand).
     * 'pose' is already mirrored for the left hand. 'equipProgress' is vanilla's inverse arm height: after every
     * attack vanilla lowers the item by up to 0.6 blocks until the cooldown refills, which would push the whole
     * swing below the screen. It is only applied at rest (pose.weight 0, where vanilla draws it too) and faded
     * out as the swing starts, while the global offset and scale from SwingAnimations fade in.
     */
    static void applyItemTransform(PoseStack poseStack, SwingAnimation.Pose pose, boolean right, float equipProgress) {
        float side = right ? 1 : -1;
        float weight = Math.max(0f, Math.min(1f, pose.weight));
        // vanilla's resting spot for a held item (ItemInHandRenderer.applyItemArmTransform), dip only at rest
        poseStack.translate(side * 0.56F, -0.52F + equipProgress * -0.6F * (1f - weight), -0.72F);
        // the global swing spot, the same for every swing (x mirrored for the left hand)
        poseStack.translate(side * SwingTuning.global("offset_x") * weight,
                SwingTuning.global("offset_y") * weight, SwingTuning.global("offset_z") * weight);
        // the swing's own offsets and turns
        poseStack.translate(pose.itemX, pose.itemY, pose.itemZ);
        poseStack.mulPose(Axis.ZP.rotationDegrees(pose.itemRoll));
        poseStack.mulPose(Axis.YP.rotationDegrees(pose.itemYaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pose.itemPitch));
        float scale = pose.itemScale * (1f + (SwingTuning.global("scale") - 1f) * weight);
        if (scale != 1f && scale > 0f) poseStack.scale(scale, scale, scale);
    }

    /** The cases where vanilla would draw this item with its plain attack swing (ItemInHandRenderer.submitArmWithItem). */
    private static boolean vanillaWouldSwingIt(LocalPlayer player, ItemStack stack) {
        if (stack.isEmpty() || player.isScoping() || player.isAutoSpinAttack()) return false;
        if (stack.has(DataComponents.MAP_ID) || stack.getItem() instanceof CrossbowItem) return false;
        return !(player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == InteractionHand.MAIN_HAND);
    }
}
