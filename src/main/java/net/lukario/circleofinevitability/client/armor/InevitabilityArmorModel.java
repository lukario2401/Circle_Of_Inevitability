package net.lukario.circleofinevitability.client.armor;

import com.google.gson.JsonObject;
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
import java.util.Map;

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
        return createLayer(ArmorRig.load(InevitabilityArmor.id("armor/inevitability.rig.json")), slot);
    }
    /** Pure rig baking entry point, also used by the standalone geometry regression test. */
    public static LayerDefinition createLayer(JsonObject rig, EquipmentSlot slot) {
        MeshDefinition mesh = new MeshDefinition();
        Map<String, PartDefinition> parts = ArmorRig.bake(rig, slot, mesh);
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
        Vector3f local = new Vector3f();
        float reach = ArmorRig.strike(state, strike, gameTime, dimension, body, local);
        Vec3 target = new Vec3(local.x, local.y, local.z);
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
