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
import org.joml.Vector3f;
import java.util.Map;

/**
 * Vestments of the Unshadowed: resource-pack editable rig, a slowly turning sun halo on the helmet, and three
 * Spears of Light orbiting the chestplate that fly tip-first to the authoritative strike point and return.
 */
public final class UnshadowedArmorModel extends HumanoidModel<HumanoidRenderState> {
    public static final int SPEARS = 3;
    /** Spear length in model pixels; each spear is modelled along +Y from its pommel (0) to its tip. */
    public static final float SPEAR_LENGTH = 16;
    private static final float ORBIT_RADIUS = 12, ORBIT_HEIGHT = 9;
    private final EquipmentSlot slot;
    private final ModelPart[] spears = new ModelPart[SPEARS];
    private final ModelPart halo;
    public UnshadowedArmorModel(ModelPart root, EquipmentSlot slot) {
        super(root);
        this.slot = slot;
        halo = head.hasChild("halo") ? head.getChild("halo") : null;
        if (slot == EquipmentSlot.CHEST) for (int i = 0; i < SPEARS; i++) spears[i] = body.getChild("spear_" + i);
    }
    public static LayerDefinition createLayer(EquipmentSlot slot) {
        return createLayer(ArmorRig.load(InevitabilityArmor.id("armor/unshadowed.rig.json")), slot);
    }
    /** Pure rig baking entry point, also used by the standalone geometry regression test. */
    public static LayerDefinition createLayer(JsonObject rig, EquipmentSlot slot) {
        MeshDefinition mesh = new MeshDefinition();
        Map<String, PartDefinition> parts = ArmorRig.bake(rig, slot, mesh);
        if (slot == EquipmentSlot.CHEST) for (int i = 0; i < SPEARS; i++)
            parts.get("body").addOrReplaceChild("spear_" + i, CubeListBuilder.create()
                    .texOffs(96, 0).addBox(-.75f, -.5f, -.75f, 1.5f, 1, 1.5f)        // pommel
                    .texOffs(64, 0).addBox(-.5f, 0, -.5f, 1, 12.5f, 1)               // shaft
                    .texOffs(64, 0).addBox(-1.75f, 11.5f, -.5f, 3.5f, 1, 1)          // crossguard
                    .texOffs(96, 0).addBox(-1, 12.5f, -.6f, 2, 2.5f, 1.2f)           // blade
                    .texOffs(96, 0).addBox(-.5f, 15, -.4f, 1, 1, .8f), PartPose.ZERO); // tip, ends at y=16
        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        if (halo != null) halo.zRot = state.ageInTicks * .02f;
        if (slot != EquipmentSlot.CHEST) return;
        CompoundTag strike = state.chestEquipment.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY)
                .copyTag().getCompoundOrEmpty(TentacleCombat.VISUAL_KEY);
        Minecraft minecraft = Minecraft.getInstance();
        var level = minecraft == null ? null : minecraft.level;
        animateSpears(state, strike, level == null ? 0 : level.getGameTime(),
                level == null ? "" : level.dimension().identifier().toString());
    }

    /** Evaluate a visual snapshot without accessing live entities or mutating the equipped item. */
    public void animateSpears(HumanoidRenderState state, CompoundTag strike, long gameTime, String dimension) {
        if (slot != EquipmentSlot.CHEST) return;
        Vector3f target = new Vector3f();
        float reach = ArmorRig.strike(state, strike, gameTime, dimension, body, target);
        for (int i = 0; i < SPEARS; i++) {
            double angle = state.ageInTicks * .05 + i * Math.PI * 2 / SPEARS;
            float sin = (float)Math.sin(angle), cos = (float)Math.cos(angle);
            Vector3f center = new Vector3f(sin * ORBIT_RADIUS,
                    ORBIT_HEIGHT + (float)Math.sin(state.ageInTicks * .1 + i * 2.1) * 1.5f, cos * ORBIT_RADIUS);
            // Idle: standing tip-up (model Y points down), leaning slightly out from the wearer.
            Vector3f idleDir = new Vector3f(sin * .25f, -1, cos * .25f).normalize();
            Vector3f idleBase = new Vector3f(idleDir).mul(-SPEAR_LENGTH / 2).add(center);
            Vector3f dir = idleDir, base = idleBase;
            if (reach > 0) {
                // Each spear flies from its own orbit point so the three converge from different sides.
                Vector3f strikeDir = new Vector3f(target).sub(center);
                if (strikeDir.lengthSquared() < 1e-4f) strikeDir.set(idleDir); else strikeDir.normalize();
                Vector3f strikeBase = new Vector3f(strikeDir).mul(-SPEAR_LENGTH).add(target);
                dir = new Vector3f(idleDir).lerp(strikeDir, reach);
                if (dir.lengthSquared() < 1e-4f) dir.set(strikeDir); else dir.normalize();
                base = new Vector3f(idleBase).lerp(strikeBase, reach);
            }
            ModelPart part = spears[i];
            part.x = base.x; part.y = base.y; part.z = base.z;
            part.xRot = (float)Math.acos(Math.clamp(dir.y, -1, 1));
            part.yRot = (float)Math.atan2(dir.x, dir.z);
            part.zRot = 0;
        }
    }
}
