package net.lukario.frogerealm.client.animation;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderAvatarEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;

/**
 * Third person (F5, and every other player you see): while a swing plays for a player, their sword arm gets our
 * own arm pose instead of vanilla's "holding an item" pose, and vanilla's attack swing is switched off for that
 * frame. The held sword follows the arm by itself.
 *
 * How (26.2 / Forge 65): RenderAvatarEvent.Pre runs before the model is posed, with the render state. We set
 * state.rightArmPose (or left) to SWING, a HumanoidModel.ArmPose made with Forge's ArmPose.create, and
 * state.attackTime to 0. HumanoidModel.setupAnim then calls our transformer for that arm. Vanilla fills the
 * render state again every frame, so nothing needs restoring, and it must not be restored in Post: the model
 * is drawn later in the frame from the same state. Model parts are reset by every setupAnim.
 * If ArmPose.create fails (Forge did not extend the enum), third person just stays vanilla.
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ThirdPersonSwing {

    private static final Map<Integer, SwingAnimation.Pose> THIS_FRAME = new HashMap<>(); // entity id -> pose to draw
    private static HumanoidModel.ArmPose swingPose;
    private static boolean triedCreate;
    private static boolean failed; // after one error: vanilla for the rest of the session

    private ThirdPersonSwing() {}

    /** Our arm pose, made on first use. Null if Forge could not make it (third person then stays vanilla). */
    private static HumanoidModel.ArmPose swingPose() {
        if (!triedCreate) {
            triedCreate = true;
            try {
                swingPose = HumanoidModel.ArmPose.create("CIRCLE_OF_INEVITABILITY_SWING", false, false, ThirdPersonSwing::applyTransform);
            } catch (Throwable e) {
                swingPose = null;
                ForgeRealm.LOGGER.warn("Swing animations: could not add an arm pose, third-person swings stay vanilla", e);
            }
        }
        return swingPose;
    }

    @SubscribeEvent
    public static void onThirdPersonSwingRenderAvatar(RenderAvatarEvent.Pre event) {
        AvatarRenderState state = event.getState();
        THIS_FRAME.remove(state.id);
        Minecraft minecraft = Minecraft.getInstance();
        if (failed || minecraft.level == null) return;
        try {
            if (!(minecraft.level.getEntity(state.id) instanceof Player player)) return;
            SwingAnimation.Pose pose = SwingPlayer.poseFor(player, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
            if (pose == null) return;
            if (state.isUsingItem && state.useItemHand == InteractionHand.MAIN_HAND) return;  // eating, blocking...: vanilla
            boolean right = state.mainArm == HumanoidArm.RIGHT;
            HumanoidModel.ArmPose current = right ? state.rightArmPose : state.leftArmPose;
            if (current != HumanoidModel.ArmPose.ITEM && current != HumanoidModel.ArmPose.EMPTY) return; // crossbow, spear...: vanilla
            HumanoidModel.ArmPose ours = swingPose();
            if (ours == null) return;

            THIS_FRAME.put(state.id, pose);
            if (right) state.rightArmPose = ours;
            else state.leftArmPose = ours;
            if (state.attackArm == state.mainArm) state.attackTime = 0f;  // no vanilla swing on top of ours
        } catch (RuntimeException e) {
            fail(e);
        }
    }

    /** Called by HumanoidModel.setupAnim for the arm that has our pose. Angles in the pose are degrees. */
    private static void applyTransform(HumanoidModel<? extends HumanoidRenderState> model, HumanoidRenderState state, HumanoidArm arm) {
        if (failed || !(state instanceof AvatarRenderState avatar)) return;
        SwingAnimation.Pose pose = THIS_FRAME.get(avatar.id);
        if (pose == null) return;
        try {
            if (arm == HumanoidArm.LEFT) pose = pose.mirrored();
            ModelPart part = model.getArm(arm);
            part.xRot = part.xRot * 0.5F + pose.armPitch * Mth.DEG_TO_RAD;   // keeps half the walking swing, like vanilla's ITEM pose
            part.yRot = pose.armYaw * Mth.DEG_TO_RAD;
            part.zRot = pose.armRoll * Mth.DEG_TO_RAD;
            float body = pose.bodyYaw * Mth.DEG_TO_RAD;
            if (body != 0f) {
                // twist the upper body and move both shoulders with it, as vanilla's setupAttackAnimation does
                model.body.yRot = body;
                model.rightArm.z = Mth.sin(body) * 5.0F * state.ageScale;
                model.rightArm.x = -Mth.cos(body) * 5.0F * state.ageScale;
                model.leftArm.z = -Mth.sin(body) * 5.0F * state.ageScale;
                model.leftArm.x = Mth.cos(body) * 5.0F * state.ageScale;
                part.yRot += body;
            }
        } catch (RuntimeException e) {
            fail(e);
        }
    }

    private static void fail(RuntimeException e) {
        failed = true;
        THIS_FRAME.clear();
        ForgeRealm.LOGGER.error("Swing animations: third-person posing failed, using vanilla swings from now on", e);
    }

    @SubscribeEvent
    public static void onThirdPersonSwingClientTick(TickEvent.ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null && !THIS_FRAME.isEmpty()) THIS_FRAME.clear(); // left the world
    }
}
