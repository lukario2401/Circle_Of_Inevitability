package net.lukario.circleofinevitability.client.uniqueness;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.client.model.CubeRig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * The shadow soldier: one rig (assets/.../red_priest/shadow_soldier.rig.json, from tools/model_gen/red_priest/
 * shadow_soldier.py) whose gear bones are shown by rank. Hand-written motion: a heavy march, a guard stance with the
 * weapon forward, a spear thrust when it attacks, a cape that sways, and clawing out of the ground while it rises.
 */
public final class ShadowSoldierModel extends EntityModel<ShadowSoldierRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "shadow_soldier"), "main");
    private static final String RIG = "red_priest/shadow_soldier.rig.json";

    private final Map<String, ModelPart> bones = new HashMap<>();
    private final ModelPart head, body, rightArm, leftArm, rightLeg, leftLeg, cape;

    public ShadowSoldierModel(ModelPart root) {
        super(root);
        bones.put("_root", root);
        for (JsonElement e : read().getAsJsonArray("bones")) {
            JsonObject bone = e.getAsJsonObject();
            String name = bone.get("name").getAsString();
            JsonElement parent = bone.get("parent");
            String parentName = parent == null || parent.isJsonNull() ? "_root" : parent.getAsString();
            bones.put(name, bones.get(parentName).getChild(name));
        }
        head = bones.get("head");
        body = bones.get("body");
        rightArm = bones.get("right_arm");
        leftArm = bones.get("left_arm");
        rightLeg = bones.get("right_leg");
        leftLeg = bones.get("left_leg");
        cape = bones.get("cape");
    }

    public static LayerDefinition createLayer() {
        return CubeRig.bake(read());
    }

    private static JsonObject read() {
        Identifier id = Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, RIG);
        try (Reader reader = Minecraft.getInstance().getResourceManager().getResourceOrThrow(id).openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load the shadow soldier rig " + id, e);
        }
    }

    private void show(String bone, boolean visible) {
        ModelPart part = bones.get(bone);
        if (part != null) part.visible = visible;
    }

    @Override
    public void setupAnim(ShadowSoldierRenderer.State s) {
        super.setupAnim(s);
        int rank = s.rank;
        // ---- gear by rank
        show("tunic", rank == 0);
        show("kettle", rank == 0);
        show("face_eyes", rank == 0);
        show("sword", rank == 0);
        show("right_bracer", rank == 0);
        show("left_bracer", rank == 0);
        show("right_wraps", rank == 0);
        show("left_wraps", rank == 0);
        show("breastplate", rank >= 1);
        show("helm", rank >= 1);
        show("helm_eyes", rank >= 1);
        show("spear", rank >= 1);
        show("right_gauntlet", rank >= 1);
        show("left_gauntlet", rank >= 1);
        show("right_greave", rank >= 1);
        show("left_greave", rank >= 1);
        show("shield", rank == 1 || rank == 2);
        show("crest", rank == 2);
        show("crown", rank == 3);
        show("cape", rank >= 2);
        show("right_pauldron", rank >= 2);
        show("left_pauldron", rank >= 2);

        float age = s.ageInTicks;
        float speed = Math.min(1, s.walkAnimationSpeed);
        float stride = s.walkAnimationPos * .6662f;
        // ---- head looks around
        head.yRot = s.yRot * Mth.DEG_TO_RAD;
        head.xRot = s.xRot * Mth.DEG_TO_RAD;
        // ---- heavy march: legs swing, the body bobs and leans into it
        rightLeg.xRot = Mth.cos(stride) * 1.1f * speed;
        leftLeg.xRot = Mth.cos(stride + Mth.PI) * 1.1f * speed;
        body.xRot = .08f * speed;
        float bob = Math.abs(Mth.cos(stride)) * speed * .6f;
        body.y += bob;
        head.y += bob;
        rightArm.y += bob;
        leftArm.y += bob;
        // ---- guard stance: the weapon arm raised forward, the shield arm across the chest; a slow smoky sway
        float sway = Mth.sin(age * .07f) * .04f;
        rightArm.xRot = -.55f + Mth.cos(stride + Mth.PI) * .25f * speed + sway;
        rightArm.zRot = .08f;
        leftArm.xRot = (rank == 1 || rank == 2 ? -.35f : Mth.cos(stride) * .6f * speed) - sway;
        leftArm.yRot = rank == 1 || rank == 2 ? .45f : 0;
        leftArm.zRot = -.08f;
        // ---- the attack: a thrust (spear) or a cut (sword)
        if (s.attackTime > 0) {
            float t = s.attackTime;
            float jab = Mth.sin(t * Mth.PI);
            if (rank == 0) {
                rightArm.xRot = -.6f - jab * 1.6f;
                rightArm.yRot = jab * .5f;
                body.yRot = jab * .25f;
            } else {
                rightArm.xRot = -1.2f - jab * .35f;
                rightArm.z -= jab * 3;
                body.yRot = -jab * .2f;
                body.xRot += jab * .15f;
            }
        }
        // ---- the cape trails behind, more when marching
        if (cape != null) cape.xRot = .14f + speed * .5f + Mth.sin(age * .1f) * .05f;
        // ---- rising: clawing up out of the ground, arms reaching for the sky
        if (s.rise < 1) {
            float claw = 1 - s.rise;
            rightArm.xRot = Mth.lerp(claw, rightArm.xRot, -2.7f + Mth.sin(age * .6f) * .35f);
            leftArm.xRot = Mth.lerp(claw, leftArm.xRot, -2.7f + Mth.cos(age * .6f) * .35f);
            head.xRot = Mth.lerp(claw, head.xRot, -.5f);
            body.xRot = Mth.lerp(claw, body.xRot, .3f);
        }
    }
}
