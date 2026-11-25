package net.lukario.circleofinevitability.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import java.util.HashMap;
import java.util.Map;

/** Bakes parent-first cube rigs in Java model pixels, with rotations authored in degrees. */
public final class CubeRig {
    private CubeRig() {}

    public static LayerDefinition bake(JsonObject rig) {
        MeshDefinition mesh = new MeshDefinition();
        Map<String, PartDefinition> parts = new HashMap<>();
        parts.put("_root", mesh.getRoot());
        for (JsonElement element : rig.getAsJsonArray("bones")) {
            JsonObject bone = element.getAsJsonObject();
            String name = bone.get("name").getAsString();
            JsonElement parentName = bone.get("parent");
            PartDefinition parent = parentName == null || parentName.isJsonNull()
                    ? mesh.getRoot() : parts.get(parentName.getAsString());
            if (parent == null) throw new IllegalArgumentException("Unknown parent for bone " + name);

            CubeListBuilder cubes = CubeListBuilder.create();
            for (JsonElement entry : bone.getAsJsonArray("cubes")) {
                JsonObject cube = entry.getAsJsonObject();
                JsonArray p = cube.getAsJsonArray("origin"), size = cube.getAsJsonArray("size"), uv = cube.getAsJsonArray("uv");
                cubes.texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt())
                        .addBox(value(p, 0), value(p, 1), value(p, 2), value(size, 0), value(size, 1), value(size, 2));
            }
            JsonArray p = bone.getAsJsonArray("pivot"), r = bone.getAsJsonArray("rotation");
            PartPose pose = PartPose.offsetAndRotation(value(p, 0), value(p, 1), value(p, 2),
                    radians(r, 0), radians(r, 1), radians(r, 2));
            parts.put(name, parent.addOrReplaceChild(name, cubes, pose));
        }
        JsonArray size = rig.getAsJsonArray("texture_size");
        return LayerDefinition.create(mesh, size.get(0).getAsInt(), size.get(1).getAsInt());
    }

    private static float value(JsonArray values, int axis) { return values.get(axis).getAsFloat(); }
    private static float radians(JsonArray values, int axis) { return (float)Math.toRadians(value(values, axis)); }
}
