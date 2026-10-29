package net.lukario.circleofinevitability.client.boss;

import com.google.gson.*;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.Identifier;
import java.io.*;
import java.util.*;

/** Native cube rig and additive keyframes. Both assets are editable through resource packs, no animation library. */
public final class StarDragonBossModel extends EntityModel<StarDragonBossRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "star_dragon_boss"), "main");
    private final Map<String, ModelPart> bones = new HashMap<>();
    private final JsonObject clips;
    public StarDragonBossModel(ModelPart root) {
        super(root);
        collect(root);
        clips = read("star_dragon.animations.json").getAsJsonObject("clips");
    }
    private void collect(ModelPart part) {
        // Names come from the rig; traverse by parent rather than depending on ModelPart internals.
        JsonArray rig = read("star_dragon.rig.json").getAsJsonArray("bones");
        bones.put("_root", part);
        for (JsonElement e : rig) {
            JsonObject b = e.getAsJsonObject(); String name = b.get("name").getAsString();
            String parent = b.has("parent") && !b.get("parent").isJsonNull() ? b.get("parent").getAsString() : "_root";
            bones.put(name, bones.get(parent).getChild(name));
        }
    }
    public static LayerDefinition createLayer() {
        JsonObject rig = read("star_dragon.rig.json");
        MeshDefinition mesh = new MeshDefinition(); Map<String, PartDefinition> parts = new HashMap<>(); parts.put("_root", mesh.getRoot());
        for (JsonElement element : rig.getAsJsonArray("bones")) {
            JsonObject b = element.getAsJsonObject();
            CubeListBuilder cubes = CubeListBuilder.create();
            for (JsonElement ce : b.getAsJsonArray("cubes")) {
                JsonObject c = ce.getAsJsonObject(); JsonArray p = c.getAsJsonArray("origin"), s = c.getAsJsonArray("size"), uv = c.getAsJsonArray("uv");
                cubes.texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt()).addBox(f(p,0),f(p,1),f(p,2),f(s,0),f(s,1),f(s,2));
            }
            JsonArray p = b.getAsJsonArray("pivot"), r = b.getAsJsonArray("rotation");
            String parent = b.has("parent") && !b.get("parent").isJsonNull() ? b.get("parent").getAsString() : "_root";
            String name = b.get("name").getAsString();
            parts.put(name, parts.get(parent).addOrReplaceChild(name, cubes,
                    PartPose.offsetAndRotation(f(p,0),f(p,1),f(p,2),rad(f(r,0)),rad(f(r,1)),rad(f(r,2)))));
        }
        JsonArray size = rig.getAsJsonArray("texture_size");
        return LayerDefinition.create(mesh, size.get(0).getAsInt(), size.get(1).getAsInt());
    }
    @Override public void setupAnim(StarDragonBossRenderer.State s) {
        super.setupAnim(s);
        apply("idle", s.ageInTicks / 20, false, 1);
        if (s.walkAnimationSpeed > .01f) apply("walk", s.walkAnimationPos / 12, false, Math.min(1,s.walkAnimationSpeed*2));
        if (s.radiant) apply("radiant", s.ageInTicks / 20, false, 1);
        bones.get("halo").visible = s.radiant;
        for (var entry : bones.entrySet()) if (entry.getKey().startsWith("angel_")) entry.getValue().visible = s.radiant;
        bones.get("head").yRot += rad(s.yRot) * .5f;
        bones.get("head").xRot += rad(s.xRot) * .4f;
        if (s.clipProgress >= 0 && s.clipProgress <= 1) apply(s.clip, s.clipProgress, true, 1);
        if (s.hurtTime > 0) apply("hurt", 1-s.hurtTime/10f, true, 1);
        if (s.deathTime > 0) apply("death", Math.min(1,s.deathTime/20), true, 1);
    }
    private void apply(String clip, float time, boolean normalized, float weight) {
        if (!clips.has(clip)) return;
        JsonObject c = clips.getAsJsonObject(clip);
        float t = normalized ? time : time / c.get("seconds").getAsFloat();
        t = c.get("loop").getAsBoolean() ? t-(float)Math.floor(t) : Math.clamp(t,0,1);
        for (var entry : c.getAsJsonObject("bones").entrySet()) {
            ModelPart bone = bones.get(entry.getKey()); if (bone == null) continue;
            for (var track : entry.getValue().getAsJsonObject().entrySet()) {
                float[] v = interpolate(track.getValue().getAsJsonArray(), t);
                switch (track.getKey()) {
                    case "rotation" -> { bone.xRot += rad(v[0])*weight; bone.yRot += rad(v[1])*weight; bone.zRot += rad(v[2])*weight; }
                    case "position" -> { bone.x += v[0]*weight; bone.y += v[1]*weight; bone.z += v[2]*weight; }
                    case "scale" -> { bone.xScale *= 1+(v[0]-1)*weight; bone.yScale *= 1+(v[1]-1)*weight; bone.zScale *= 1+(v[2]-1)*weight; }
                    default -> throw new IllegalArgumentException("Unknown animation channel " + track.getKey());
                }
            }
        }
    }
    private static float[] interpolate(JsonArray keys, float t) {
        JsonArray a = keys.get(0).getAsJsonArray(), b = a;
        for (JsonElement e : keys) { b=e.getAsJsonArray(); if (f(b,0)>=t) break; a=b; }
        float d=f(b,0)-f(a,0), u=d<=0?0:Math.clamp((t-f(a,0))/d,0,1); u=u*u*(3-2*u);
        return new float[]{f(a,1)+(f(b,1)-f(a,1))*u,f(a,2)+(f(b,2)-f(a,2))*u,f(a,3)+(f(b,3)-f(a,3))*u};
    }
    private static float f(JsonArray a, int i) { return a.get(i).getAsFloat(); }
    private static float rad(float degrees) { return degrees*(float)Math.PI/180; }
    private static JsonObject read(String file) {
        Identifier id=Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID,"boss/star_dragon/"+file);
        try (Reader r=Minecraft.getInstance().getResourceManager().getResourceOrThrow(id).openAsReader()) {
            return JsonParser.parseReader(r).getAsJsonObject();
        } catch (IOException e) { throw new IllegalStateException("Cannot load boss asset "+id,e); }
    }
}
