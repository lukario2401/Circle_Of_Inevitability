package net.lukario.circleofinevitability.client.armor;

import net.lukario.circleofinevitability.item.artifacts.MonocleItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

/**
 * The Monocle, worn: a gold rim and pale lens over the right eye with a chain back to the ear. Once the wearer is far
 * gone (stage III, saved on the stack as MonocleItem.STAGE_KEY) a marionette's control bar hangs above their head
 * with strings running down to it: someone else is holding them now.
 * Texture: textures/entity/equipment/humanoid/monocle.png (64x32), made by tools/model_gen/monocle/generate.py.
 */
public final class MonocleModel extends HumanoidModel<HumanoidRenderState> {
    // Atlas regions, kept in sync with the generator.
    private static final int[] GOLD = {0, 0}, LENS = {16, 0}, CHAIN = {32, 0}, STRING = {48, 0}, WOOD = {0, 16}, IRIS = {16, 16};
    private final ModelPart strings;

    public MonocleModel(ModelPart root) {
        super(root);
        strings = head.getChild("strings");
    }

    public static MonocleModel create() {
        return new MonocleModel(createLayer().bakeRoot());
    }

    private static CubeListBuilder at(CubeListBuilder cubes, int[] uv) {
        return cubes.texOffs(uv[0], uv[1]);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder monocle = CubeListBuilder.create();
        // Rim over the right eye (model -X), just in front of the face (z = -4).
        at(monocle, GOLD).addBox(-3.5f, -5.2f, -4.7f, 3, .5f, .5f)
                .addBox(-3.5f, -2.0f, -4.7f, 3, .5f, .5f)
                .addBox(-3.9f, -4.8f, -4.7f, .5f, 2.9f, .5f)
                .addBox(-.6f, -4.8f, -4.7f, .5f, 2.9f, .5f);
        at(monocle, LENS).addBox(-3.45f, -4.75f, -4.5f, 2.9f, 2.8f, .15f);
        at(monocle, IRIS).addBox(-2.3f, -3.6f, -4.56f, .6f, .6f, .1f);
        // The chain sags from the bottom of the rim back to the right ear.
        for (int k = 0; k < 6; k++) {
            float t = k / 5f;
            at(monocle, CHAIN).addBox(-3.9f - t * .5f, -1.9f + (float) Math.sin(t * Math.PI) * 1.1f - t * 1.6f,
                    -4.4f + t * 4.2f, .35f, .35f, .8f);
        }
        PartDefinition head = root.addOrReplaceChild("head", monocle, PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        // The marionette's control bar and its strings, above the head.
        CubeListBuilder bar = CubeListBuilder.create();
        at(bar, WOOD).addBox(-5, -22.4f, -.4f, 10, .8f, .8f)
                .addBox(-.4f, -22.4f, -4, .8f, .8f, 8);
        for (float[] s : new float[][]{{-4.6f, 0}, {4.2f, 0}, {0, -3.6f}, {0, 3.2f}})
            at(bar, STRING).addBox(s[0], -21.6f, s[1], .15f, 13.4f, .15f);
        head.addOrReplaceChild("strings", bar, PartPose.ZERO);
        for (String part : new String[]{"body", "right_arm", "left_arm", "right_leg", "left_leg"})
            root.addOrReplaceChild(part, CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        int stage = state.headEquipment.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getIntOr(MonocleItem.STAGE_KEY, 0);
        strings.visible = stage >= 3;
        // The bar sways a little, as if a hand were working it.
        strings.yRot = (float) Math.sin(state.ageInTicks * .05f) * .25f;
        strings.zRot = (float) Math.sin(state.ageInTicks * .07f) * .05f;
    }
}
