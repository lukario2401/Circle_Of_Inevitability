package net.lukario.circleofinevitability.client.armor;

import com.google.gson.JsonObject;
import net.lukario.circleofinevitability.item.armor.GrayFogArmor;
import net.lukario.circleofinevitability.item.armor.InevitabilityArmor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.CustomData;
import java.util.Map;

/**
 * Vestments of the Gray Fog: resource-pack editable rig, coat tails that swing with the legs, and a paper figurine
 * floating at the left shoulder that is gone while the Paper Substitute recharges.
 */
public final class GrayFogArmorModel extends HumanoidModel<HumanoidRenderState> {
    /** Figurine rest point at the left shoulder, in body model space. */
    public static final float FIGURINE_X = 8.5f, FIGURINE_Y = -4, FIGURINE_Z = 1.5f;
    private final EquipmentSlot slot;
    private final ModelPart tailLeft, tailRight, figurine;
    public GrayFogArmorModel(ModelPart root, EquipmentSlot slot) {
        super(root);
        this.slot = slot;
        tailLeft = body.getChild("coat_tail_left");
        tailRight = body.getChild("coat_tail_right");
        figurine = slot == EquipmentSlot.CHEST ? body.getChild("figurine") : null;
    }
    public static LayerDefinition createLayer(EquipmentSlot slot) {
        return createLayer(ArmorRig.load(InevitabilityArmor.id("armor/" + GrayFogArmor.SET + ".rig.json")), slot);
    }
    /** Pure rig baking entry point, also used by the standalone geometry regression test. */
    public static LayerDefinition createLayer(JsonObject rig, EquipmentSlot slot) {
        MeshDefinition mesh = new MeshDefinition();
        Map<String, PartDefinition> parts = ArmorRig.bake(rig, slot, mesh);
        if (slot == EquipmentSlot.CHEST)
            // A flat paper doll, about six pixels tall; texture region 'paper' of the atlas.
            parts.get("body").addOrReplaceChild("figurine", CubeListBuilder.create().texOffs(64, 32)
                    .addBox(-.9f, -3.6f, -.15f, 1.8f, 1.8f, .3f)       // head
                    .addBox(-1.1f, -1.8f, -.15f, 2.2f, 2.6f, .3f)      // body
                    .addBox(-1.9f, -1.7f, -.15f, .8f, 2.2f, .3f)       // arms
                    .addBox(1.1f, -1.7f, -.15f, .8f, 2.2f, .3f)
                    .addBox(-1, .8f, -.15f, .85f, 2.4f, .3f)           // legs
                    .addBox(.15f, .8f, -.15f, .85f, 2.4f, .3f), PartPose.offset(FIGURINE_X, FIGURINE_Y, FIGURINE_Z));
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        swayTails(state);
        if (slot != EquipmentSlot.CHEST) return;
        long readyAt = state.chestEquipment.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getLongOr(GrayFogArmor.PAPER_READY_KEY, 0L);
        Minecraft minecraft = Minecraft.getInstance();
        var level = minecraft == null ? null : minecraft.level;
        animateFigurine(state, level == null || level.getGameTime() >= readyAt);
    }

    /** Each tail is pushed back by its leg as that leg swings back, with a slow idle flutter. */
    public void swayTails(HumanoidRenderState state) {
        float flutter = (float)Math.sin(state.ageInTicks * .08f) * .03f;
        tailRight.xRot = .12f + Math.max(0, rightLeg.xRot) * .8f + flutter;
        tailLeft.xRot = .12f + Math.max(0, leftLeg.xRot) * .8f - flutter;
    }

    /** The figurine bobs and slowly turns while the substitute is ready, and is not drawn while it recharges. */
    public void animateFigurine(HumanoidRenderState state, boolean ready) {
        if (figurine == null) return;
        figurine.visible = ready;
        figurine.x = FIGURINE_X;
        figurine.y = FIGURINE_Y + (float)Math.sin(state.ageInTicks * .1f) * .8f;
        figurine.z = FIGURINE_Z;
        figurine.xRot = 0;
        figurine.yRot = state.ageInTicks * .04f;
        figurine.zRot = (float)Math.sin(state.ageInTicks * .07f) * .15f;
    }
}
