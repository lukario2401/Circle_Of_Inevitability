package net.lukario.circleofinevitability.client.armor;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Vector3f;
import java.io.*;
import java.util.*;

/** Shared resource-pack rig baking and strike-snapshot math for the mod's 3D armor sets. */
public final class ArmorRig {
    private ArmorRig() {}

    public static JsonObject load(Identifier id) {
        try (Reader r = Minecraft.getInstance().getResourceManager().getResourceOrThrow(id).openAsReader()) {
            return JsonParser.parseReader(r).getAsJsonObject();
        } catch (IOException e) { throw new IllegalStateException("Cannot load armor rig " + id, e); }
    }

    /** Adds every bone of the rig to the mesh, keeping only cubes for the given slot. Returns bones by name. */
    public static Map<String, PartDefinition> bake(JsonObject rig, EquipmentSlot slot, MeshDefinition mesh) {
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
        return parts;
    }
    private static float f(JsonArray a, int i) { return a.get(i).getAsFloat(); }

    /**
     * Evaluates a TentacleCombat strike snapshot. Returns the eased 0..1 reach (0 when idle, expired or in another
     * dimension) and writes the strike point, in the body part's local model space, into {@code target}.
     */
    public static float strike(HumanoidRenderState state, CompoundTag strike, long gameTime, String dimension,
                               ModelPart body, Vector3f target) {
        target.zero();
        if (dimension.isEmpty() || !strike.getStringOr("dimension", "").equals(dimension)) return 0;
        float elapsed = gameTime - strike.getLongOr("started", Long.MIN_VALUE / 2)
                + (state.ageInTicks - (float)Math.floor(state.ageInTicks));
        int duration = strike.getIntOr("duration", 12), windup = strike.getIntOr("windup", 6);
        if (elapsed < 0 || elapsed >= duration) return 0;
        float reach = elapsed <= windup ? elapsed / windup : (duration - elapsed) / (duration - windup);
        reach = reach * reach * (3 - 2 * reach);
        double dx = strike.getDoubleOr("x", state.x) - state.x;
        double dz = strike.getDoubleOr("z", state.z) - state.z;
        double yaw = Math.toRadians(state.bodyRot);
        // Inverse of the living renderer's Y rotation and mirrored model axes.
        double scale = Math.max(.01, state.scale * state.ageScale);
        target.set((float)((dx*Math.cos(yaw)+dz*Math.sin(yaw))*16/scale),
                (float)(24-(strike.getDoubleOr("y", state.y+1)-state.y)*16/scale),
                (float)((dx*Math.sin(yaw)-dz*Math.cos(yaw))*16/scale));
        target.sub(body.x, body.y, body.z).rotateZ(-body.zRot).rotateY(-body.yRot).rotateX(-body.xRot);
        return reach;
    }
}
