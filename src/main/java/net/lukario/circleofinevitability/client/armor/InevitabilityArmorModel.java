package net.lukario.circleofinevitability.client.armor;

import com.google.gson.*;
import net.lukario.circleofinevitability.item.armor.InevitabilityArmor;
import net.lukario.circleofinevitability.item.armor.TentacleCombat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.io.*;
import java.util.*;

/** Resource-pack editable armor rig with four independently animated, tapered, twelve-segment tentacles. */
public final class InevitabilityArmorModel extends HumanoidModel<HumanoidRenderState> {
    private static final int TENTACLES = 4, SEGMENTS = 12;
    private final EquipmentSlot slot;
    private final ModelPart[][] tendrils = new ModelPart[TENTACLES][SEGMENTS];
    public InevitabilityArmorModel(ModelPart root, EquipmentSlot slot) {
        super(root);
        this.slot = slot;
        if (slot == EquipmentSlot.CHEST)
            for (int i = 0; i < TENTACLES; i++) for (int j = 0; j < SEGMENTS; j++)
                tendrils[i][j] = body.getChild("tendril_" + i + "_" + j);
    }
    public static LayerDefinition createLayer(EquipmentSlot slot) {
        try (Reader r = Minecraft.getInstance().getResourceManager()
                .getResourceOrThrow(InevitabilityArmor.id("armor/inevitability.rig.json")).openAsReader()) {
            return createLayer(JsonParser.parseReader(r).getAsJsonObject(), slot);
        } catch (IOException e) { throw new IllegalStateException("Cannot load Inevitability armor rig", e); }
    }
    /** Pure rig baking entry point, also used by the standalone geometry regression test. */
    public static LayerDefinition createLayer(JsonObject rig, EquipmentSlot slot) {
        MeshDefinition mesh = new MeshDefinition();
        Map<String, PartDefinition> parts = new HashMap<>();
        parts.put("root", mesh.getRoot());
        for (JsonElement element : rig.getAsJsonArray("bones")) {
            JsonObject b = element.getAsJsonObject();
            CubeListBuilder cubes = CubeListBuilder.create();
            for (JsonElement ce : b.getAsJsonArray("cubes")) {
                JsonObject c = ce.getAsJsonObject();
                if (!c.get("slot").getAsString().equals(slot.getSerializedName())) continue;
                JsonArray p = c.getAsJsonArray("origin"), s = c.getAsJsonArray("size"), uv = c.getAsJsonArray("uv");
                cubes.texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt())
                        .addBox(f(p,0), f(p,1), f(p,2), f(s,0), f(s,1), f(s,2));
            }
            JsonArray p = b.getAsJsonArray("pivot"), r = b.getAsJsonArray("rotation");
            String name = b.get("name").getAsString();
            parts.put(name, parts.get(b.get("parent").getAsString()).addOrReplaceChild(name, cubes,
                    PartPose.offsetAndRotation(f(p,0),f(p,1),f(p,2),f(r,0),f(r,1),f(r,2))));
        }
        if (slot == EquipmentSlot.CHEST) {
            for (int i = 0; i < TENTACLES; i++) for (int j = 0; j < SEGMENTS; j++) {
                float width = 2.9f - j * .205f;
                parts.get("body").addOrReplaceChild("tendril_" + i + "_" + j,
                        CubeListBuilder.create().texOffs(64,0).addBox(-width/2,0,-width/2,width,3,width)
                                .texOffs(96,32).addBox(-width*.3f,.6f,-width*.5f-.3f,width*.6f,.65f,.5f), PartPose.ZERO);
            }
        }
        return LayerDefinition.create(mesh, 128, 64);
    }
    private static float f(JsonArray a, int i) { return a.get(i).getAsFloat(); }

    @Override public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        if (slot != EquipmentSlot.CHEST) return;
        CompoundTag strike = state.chestEquipment.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getCompoundOrEmpty(TentacleCombat.VISUAL_KEY);
        var level = Minecraft.getInstance().level;
        animateTentacles(state, strike, level == null ? 0 : level.getGameTime(),
                level == null ? "" : level.dimension().identifier().toString());
    }
    /** Evaluate a visual snapshot without accessing live entities or mutating the equipped item. */
    public void animateTentacles(HumanoidRenderState state, CompoundTag strike, long gameTime, String dimension) {
        if (slot != EquipmentSlot.CHEST) return;
        float reach = 0;
        Vec3 target = Vec3.ZERO;
        if (!dimension.isEmpty() && strike.getStringOr("dimension", "").equals(dimension)) {
            float elapsed = gameTime - strike.getLongOr("started", Long.MIN_VALUE / 2)
                    + (state.ageInTicks - (float)Math.floor(state.ageInTicks));
            int duration = strike.getIntOr("duration", 12), windup = strike.getIntOr("windup", 6);
            if (elapsed >= 0 && elapsed < duration) {
                reach = elapsed <= windup ? elapsed / windup : (duration - elapsed) / (duration - windup);
                reach = reach * reach * (3 - 2 * reach);
                double dx = strike.getDoubleOr("x", state.x) - state.x;
                double dz = strike.getDoubleOr("z", state.z) - state.z;
                double yaw = Math.toRadians(state.bodyRot);
                // Inverse of the living renderer's Y rotation and mirrored model axes.
                double scale = Math.max(.01, state.scale * state.ageScale);
                Vector3f local = new Vector3f((float)((dx*Math.cos(yaw)+dz*Math.sin(yaw))*16/scale),
                        (float)(24-(strike.getDoubleOr("y", state.y+1)-state.y)*16/scale),
                        (float)((dx*Math.sin(yaw)-dz*Math.cos(yaw))*16/scale));
                local.sub(body.x, body.y, body.z).rotateZ(-body.zRot).rotateY(-body.yRot).rotateX(-body.xRot);
                target = new Vec3(local.x, local.y, local.z);
            }
        }
        for (int i = 0; i < TENTACLES; i++) {
            double side = i % 2 == 0 ? -1 : 1;
            double height = i < 2 ? 2 : 8;
            double wave = Math.sin(state.ageInTicks*.075 + i*1.7);
            Vec3 start = new Vec3(side*3.8, height, 3.4);
            Vec3 idleTip = new Vec3(side*(13+wave*2), height-10+wave*3, 7+Math.cos(state.ageInTicks*.05+i)*4);
            // All four lash toward the same authoritative strike point, with distinct curved paths.
            Vec3 end = idleTip.lerp(target, reach);
            Vec3 bend1 = start.add(side*(12+reach*4), -9-wave*2, 9);
            Vec3 bend2 = end.add(side*(7*(1-reach)), -8*(1-reach), 6*(1-reach));
            for (int j=0;j<SEGMENTS;j++) {
                Vec3 a=bezier(start,bend1,bend2,end,j/(double)SEGMENTS);
                Vec3 b=bezier(start,bend1,bend2,end,(j+1)/(double)SEGMENTS);
                Vec3 d=b.subtract(a);
                ModelPart part=tendrils[i][j];
                part.x=(float)a.x; part.y=(float)a.y; part.z=(float)a.z;
                part.xRot=(float)Math.acos(Math.clamp(d.y/Math.max(.001,d.length()),-1,1));
                part.yRot=(float)Math.atan2(d.x,d.z); part.zRot=0;
                part.yScale=(float)d.length()/3+.035f;
            }
        }
    }
    private static Vec3 bezier(Vec3 a, Vec3 b, Vec3 c, Vec3 d, double t) {
        double u=1-t;
        return a.scale(u*u*u).add(b.scale(3*u*u*t)).add(c.scale(3*u*t*t)).add(d.scale(t*t*t));
    }
}
