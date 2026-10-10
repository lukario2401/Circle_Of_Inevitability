package net.lukario.frogerealm.particles.fx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The ModelFx models: every models/model_fx/**.json (any namespace), read again on every resource reload.
 * Their textures come from the block atlas (textures/model_fx/, added to it by assets/minecraft/atlases/blocks.json).
 * (26.2: the game's model baker no longer takes standalone models (no ModelEvent.RegisterAdditional, no BakedModel),
 * so the parts of the block-model format these files use are read here: elements with from/to, an optional rotation
 * (origin with axis + angle, or x/y/z; rescale) and faces with uv, rotation and texture. The corners come out in the
 * same order, at the same places and with the same texture spots as the game's own FaceBakery makes them.)
 * A file that cannot be read is logged and left out; ModelFxRenderer then simply draws nothing for it.
 */
final class ModelFxModels {

    /** One face: 4 corners in blocks (0..1 = one block), texture spots 0..1 inside its texture, the side it faces. */
    record Quad(float[] x, float[] y, float[] z, float[] u, float[] v, Identifier texture, Direction direction) {}

    /** The faces of one model, element by element, in the order DOWN, UP, NORTH, SOUTH, WEST, EAST within an element. */
    record Model(List<Quad> quads) {}

    private static final String FOLDER = "models/" + ModelFx.FOLDER;

    private static volatile Map<Identifier, Model> models = Map.of();

    private ModelFxModels() {}

    /** The model with this id (namespace:model_fx/name), or null when there is no such file or it could not be read. */
    static Model get(Identifier id) {
        return models.get(id);
    }

    /** Reads the files off the main thread, swaps them in on it. Registered in ModelFxRenderer. */
    static final PreparableReloadListener RELOADER = new SimplePreparableReloadListener<Map<Identifier, Model>>() {
        @Override
        protected Map<Identifier, Model> prepare(ResourceManager resources, ProfilerFiller profiler) {
            return load(resources);
        }

        @Override
        protected void apply(Map<Identifier, Model> loaded, ResourceManager resources, ProfilerFiller profiler) {
            models = loaded;
        }
    };

    static Map<Identifier, Model> load(ResourceManager resources) {
        Map<Identifier, Model> loaded = new HashMap<>();
        Map<Identifier, Resource> files;
        try {
            files = resources.listResources(FOLDER, file -> file.getPath().endsWith(".json"));
        } catch (RuntimeException e) {
            ForgeRealm.LOGGER.error("Could not list the model_fx models", e);
            return Map.of();
        }
        for (Map.Entry<Identifier, Resource> file : files.entrySet()) {
            String path = file.getKey().getPath(); // models/model_fx/order_hammer.json
            Identifier id = Identifier.fromNamespaceAndPath(file.getKey().getNamespace(),
                    path.substring("models/".length(), path.length() - ".json".length()));
            try (Reader reader = file.getValue().openAsReader()) {
                loaded.put(id, read(JsonParser.parseReader(reader).getAsJsonObject()));
            } catch (Exception e) {
                ForgeRealm.LOGGER.error("Could not read the model_fx model {}: {}", file.getKey(), e.toString());
            }
        }
        return Map.copyOf(loaded);
    }

    // =========================
    // Reading one file
    // =========================

    static Model read(JsonObject root) {
        Map<String, String> textures = new HashMap<>();
        if (root.has("textures")) {
            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("textures").entrySet()) {
                textures.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        List<Quad> quads = new ArrayList<>();
        if (root.has("elements")) {
            for (JsonElement element : root.getAsJsonArray("elements")) {
                readElement(element.getAsJsonObject(), textures, quads);
            }
        }
        return new Model(List.copyOf(quads));
    }

    private static void readElement(JsonObject element, Map<String, String> textures, List<Quad> quads) {
        Vector3f from = vector(element.getAsJsonArray("from"));
        Vector3f to = vector(element.getAsJsonArray("to"));

        Matrix4f rotation = null;
        Vector3f origin = null;
        if (element.has("rotation")) {
            JsonObject r = element.getAsJsonObject("rotation");
            origin = vector(r.getAsJsonArray("origin")).mul(1f / 16f);
            rotation = new Matrix4f();
            if (r.has("axis") || r.has("angle")) {
                float angle = r.get("angle").getAsFloat();
                Direction.Axis axis = Direction.Axis.byName(r.get("axis").getAsString().toLowerCase(Locale.ROOT));
                if (axis == null) throw new IllegalArgumentException("Invalid rotation axis: " + r.get("axis"));
                if (angle != 0f) rotation.rotation((float) Math.toRadians(angle), axis.getPositive().getUnitVec3f());
            } else {
                float x = r.has("x") ? r.get("x").getAsFloat() : 0f;
                float y = r.has("y") ? r.get("y").getAsFloat() : 0f;
                float z = r.has("z") ? r.get("z").getAsFloat() : 0f;
                rotation.rotationZYX((float) Math.toRadians(z), (float) Math.toRadians(y), (float) Math.toRadians(x));
            }
            if (r.has("rescale") && r.get("rescale").getAsBoolean()) {
                // stretch the turned element back out to the size it had (the game's CuboidRotation does the same)
                rotation.scale(rescale(rotation, Direction.Axis.X), rescale(rotation, Direction.Axis.Y),
                        rescale(rotation, Direction.Axis.Z));
            }
        }

        JsonObject faces = element.getAsJsonObject("faces");
        for (Direction direction : Direction.values()) {
            if (!faces.has(direction.getName())) continue;
            JsonObject face = faces.getAsJsonObject(direction.getName());

            float[] uv = face.has("uv") ? uvs(face.getAsJsonArray("uv")) : defaultUv(from, to, direction);
            int shift = quarterTurns(face.has("rotation") ? face.get("rotation").getAsInt() : 0);
            Identifier texture = texture(face.get("texture").getAsString(), textures);

            float[] x = new float[4], y = new float[4], z = new float[4], u = new float[4], v = new float[4];
            FaceInfo info = FaceInfo.fromFacing(direction);
            for (int i = 0; i < 4; i++) {
                Vector3f corner = info.getVertexInfo(i).select(from, to).div(16f);
                if (rotation != null) {
                    corner.sub(origin);
                    rotation.transformPosition(corner);
                    corner.add(origin);
                }
                x[i] = corner.x();
                y[i] = corner.y();
                z[i] = corner.z();
                int spot = (i + shift) % 4; // the face's own rotation turns the texture, not the corners
                u[i] = (spot == 0 || spot == 1 ? uv[0] : uv[2]) / 16f;
                v[i] = (spot == 0 || spot == 3 ? uv[1] : uv[3]) / 16f;
            }
            quads.add(new Quad(x, y, z, u, v, texture, direction));
        }
    }

    private static float rescale(Matrix4f rotation, Direction.Axis axis) {
        Vector3f turned = rotation.transformDirection(new Vector3f(axis.getPositive().getUnitVec3f()));
        return 1f / Math.max(Math.max(Math.abs(turned.x()), Math.abs(turned.y())), Math.abs(turned.z()));
    }

    /** The texture spots used when a face has no "uv": the part of the texture the face covers in a 16 x 16 square. */
    private static float[] defaultUv(Vector3f from, Vector3f to, Direction direction) {
        return switch (direction) {
            case DOWN -> new float[]{from.x(), 16f - to.z(), to.x(), 16f - from.z()};
            case UP -> new float[]{from.x(), from.z(), to.x(), to.z()};
            case NORTH -> new float[]{16f - to.x(), 16f - to.y(), 16f - from.x(), 16f - from.y()};
            case SOUTH -> new float[]{from.x(), 16f - to.y(), to.x(), 16f - from.y()};
            case WEST -> new float[]{from.z(), 16f - to.y(), to.z(), 16f - from.y()};
            case EAST -> new float[]{16f - to.z(), 16f - to.y(), 16f - from.z(), 16f - from.y()};
        };
    }

    private static int quarterTurns(int degrees) {
        int turned = Math.floorMod(degrees, 360);
        if (turned % 90 != 0) throw new IllegalArgumentException("Invalid rotation " + degrees + ", only 0/90/180/270 allowed");
        return turned / 90;
    }

    /** "#0" -> the texture called 0 in "textures" (which may again point at another one). */
    private static Identifier texture(String reference, Map<String, String> textures) {
        String name = reference;
        for (int depth = 0; depth < 8; depth++) {
            String key = name.startsWith("#") ? name.substring(1) : name;
            String next = textures.get(key);
            if (next == null) break;
            name = next;
        }
        if (name.startsWith("#")) throw new IllegalArgumentException("Texture " + reference + " is not defined");
        Identifier id = Identifier.tryParse(name);
        if (id == null) throw new IllegalArgumentException("Invalid texture " + name);
        return id;
    }

    private static Vector3f vector(JsonArray array) {
        if (array.size() != 3) throw new IllegalArgumentException("Expected 3 values, found " + array.size());
        return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    private static float[] uvs(JsonArray array) {
        if (array.size() != 4) throw new IllegalArgumentException("Expected 4 uv values, found " + array.size());
        return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat(), array.get(3).getAsFloat()};
    }
}
