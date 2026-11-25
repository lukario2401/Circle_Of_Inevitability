package net.lukario.circleofinevitability.client.mount;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Shared motion for bipedal mounts. Additional, unanimated bones follow their parent. */
final class MountModel extends EntityModel<MountRenderer.State> {
    private final ModelPart body, neck, head, tail, tip, leftLeg, rightLeg;

    MountModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        neck = body.getChild("neck");
        head = neck.getChild("head");
        tail = body.getChild("tail");
        tip = tail.getChild("tip");
        leftLeg = body.getChild("left_leg");
        rightLeg = body.getChild("right_leg");
    }

    @Override
    public void setupAnim(MountRenderer.State state) {
        super.setupAnim(state);
        float speed = state.walkAnimationSpeed;
        float stride = state.walkAnimationPos * 0.68F;
        float breath = Mth.sin(state.ageInTicks * 0.09F);
        body.y += breath * 0.2F;
        neck.xRot += breath * 0.025F + Mth.cos(stride * 2) * speed * 0.06F;
        head.yRot += state.yRot * Mth.DEG_TO_RAD * 0.65F;
        head.xRot += state.xRot * Mth.DEG_TO_RAD * 0.55F;
        tail.yRot += Mth.sin(state.ageInTicks * 0.08F) * 0.09F + Mth.cos(stride) * speed * 0.16F;
        tip.yRot += Mth.sin(state.ageInTicks * 0.08F - 0.7F) * 0.14F;

        float swing = Mth.sin(stride) * speed;
        leg(leftLeg, swing, state.airborne);
        leg(rightLeg, -swing, state.airborne);
        if (body.hasChild("left_wing") && body.hasChild("right_wing")) {
            wing(body.getChild("left_wing"), -1, state);
            wing(body.getChild("right_wing"), 1, state);
        }
    }

    private static void leg(ModelPart leg, float swing, boolean flying) {
        leg.xRot += flying ? 0.8F : swing * 0.9F;
        leg.getChild("shin").xRot += flying ? -0.65F : Math.max(0, -swing) * 0.82F;
    }

    private static void wing(ModelPart wing, int side, MountRenderer.State state) {
        float flap = Mth.sin(state.ageInTicks * 0.34F);
        if (state.airborne) {
            wing.zRot += side * flap * 0.48F;
            wing.getChild("tip").zRot += side * Mth.sin(state.ageInTicks * 0.34F - 0.8F) * 0.33F;
        } else {
            wing.yRot -= side * 0.92F;
            wing.zRot -= side * (0.38F + Mth.sin(state.ageInTicks * 0.08F) * 0.035F);
            wing.getChild("tip").yRot -= side * 0.6F;
        }
    }
}
