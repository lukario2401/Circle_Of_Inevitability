package net.lukario.circleofinevitability.client.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.lukario.circleofinevitability.client.animation.SwingAnimation.Easing;
import net.lukario.circleofinevitability.client.animation.SwingAnimation.Pose;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Validated immutable edits and the on-disk format, independent of the game runtime. */
public final class SwingAnimationEdits {
    public static final List<String> FIELDS = List.of("at", "item_x", "item_y", "item_z", "item_pitch", "item_yaw",
            "item_roll", "item_scale", "arm_pitch", "arm_yaw", "arm_roll", "body_yaw", "weight");

    private SwingAnimationEdits() {}

    public static String validId(String id) {
        if (!id.matches("[a-z][a-z0-9_]{0,79}")) throw new IllegalArgumentException("Use a lowercase id, letters/numbers/underscores, at most 80 characters.");
        return id;
    }

    public static float bounded(float value, float min, float max) {
        if (!Float.isFinite(value) || value < min || value > max)
            throw new IllegalArgumentException("Expected a finite number from " + min + " to " + max + ".");
        return value;
    }

    public static SwingAnimation copy(SwingAnimation source, String id, int ticks, List<SwingAnimation.Keyframe> keys) {
        validId(id);
        if (ticks < 1 || ticks > 1200) throw new IllegalArgumentException("Duration must be 1..1200 ticks.");
        if (keys.size() < 2 || keys.size() > 128) throw new IllegalArgumentException("Use 2..128 keyframes.");
        var sorted = new ArrayList<>(keys);
        sorted.sort(java.util.Comparator.comparingDouble(SwingAnimation.Keyframe::at));
        SwingAnimation result = SwingAnimation.of(id, ticks);
        float previous = -1;
        for (var key : sorted) {
            bounded(key.at(), 0, 1);
            if (key.at() <= previous) throw new IllegalArgumentException("Keyframe times must be distinct.");
            previous = key.at();
            result = result.key(key.at(), key.easing(), key.pose());
        }
        return source.mirror() ? result.mirrored(id) : result;
    }

    public static SwingAnimation key(SwingAnimation source, int index, String field, float value) {
        var keys = new ArrayList<>(source.keyframes());
        var old = keys.get(index);
        if (field.equals("at")) keys.set(index, new SwingAnimation.Keyframe(bounded(value, 0, 1), old.easing(), old.pose()));
        else keys.set(index, new SwingAnimation.Keyframe(old.at(), old.easing(), pose(old.pose(), field, value)));
        return copy(source, source.id(), source.ticks(), keys);
    }

    private static Pose pose(Pose p, String field, float value) {
        if (!FIELDS.contains(field)) throw new IllegalArgumentException("Unknown field: " + field);
        switch (field) {
            case "weight" -> bounded(value, 0, 1);
            case "item_scale" -> bounded(value, .01f, 10);
            case "item_x", "item_y", "item_z" -> bounded(value, -16, 16);
            default -> bounded(value, -3600, 3600);
        }
        Pose changed = switch (field) {
            case "item_x" -> p.item(value, p.itemY, p.itemZ);
            case "item_y" -> p.item(p.itemX, value, p.itemZ);
            case "item_z" -> p.item(p.itemX, p.itemY, value);
            case "item_pitch" -> p.itemRot(value, p.itemYaw, p.itemRoll);
            case "item_yaw" -> p.itemRot(p.itemPitch, value, p.itemRoll);
            case "item_roll" -> p.itemRot(p.itemPitch, p.itemYaw, value);
            case "item_scale" -> p.scale(value);
            case "arm_pitch" -> p.arm(value, p.armYaw, p.armRoll);
            case "arm_yaw" -> p.arm(p.armPitch, value, p.armRoll);
            case "arm_roll" -> p.arm(p.armPitch, p.armYaw, value);
            case "body_yaw" -> p.body(value);
            case "weight" -> p.withWeight(value);
            default -> throw new IllegalArgumentException("Unknown pose field: " + field);
        };
        return field.equals("weight") ? changed : changed.withWeight(p.weight == 0 ? 1 : p.weight);
    }

    public static SwingAnimation easing(SwingAnimation source, int index, String easing) {
        var keys = new ArrayList<>(source.keyframes());
        var old = keys.get(index);
        keys.set(index, new SwingAnimation.Keyframe(old.at(), Easing.valueOf(easing.toUpperCase(Locale.ROOT)), old.pose()));
        return copy(source, source.id(), source.ticks(), keys);
    }

    public static SwingAnimation add(SwingAnimation source, float at) {
        bounded(at, 0, 1);
        var keys = new ArrayList<>(source.keyframes());
        Pose pose = source.sample(at);
        if (source.mirror()) pose = pose.mirrored();
        keys.add(new SwingAnimation.Keyframe(at, Easing.LINEAR, pose));
        return copy(source, source.id(), source.ticks(), keys);
    }

    public static SwingAnimation remove(SwingAnimation source, int index) {
        var keys = new ArrayList<>(source.keyframes());
        keys.remove(index);
        return copy(source, source.id(), source.ticks(), keys);
    }

    public static JsonObject encode(SwingAnimation animation) {
        JsonObject json = new JsonObject();
        json.addProperty("ticks", animation.ticks());
        json.addProperty("mirror", animation.mirror());
        JsonArray keys = new JsonArray();
        for (var key : animation.keyframes()) {
            JsonObject item = new JsonObject();
            item.addProperty("at", key.at());
            item.addProperty("easing", key.easing().name());
            Pose p = key.pose();
            float[] values = {p.itemX, p.itemY, p.itemZ, p.itemPitch, p.itemYaw, p.itemRoll, p.itemScale,
                    p.armPitch, p.armYaw, p.armRoll, p.bodyYaw, p.weight};
            for (int i = 1; i < FIELDS.size(); i++) item.addProperty(FIELDS.get(i), values[i - 1]);
            keys.add(item);
        }
        json.add("keys", keys);
        return json;
    }

    public static SwingAnimation decode(String id, JsonObject json) {
        float ticks = number(json, "ticks");
        bounded(ticks, 1, 1200);
        if (ticks != Math.floor(ticks)) throw new IllegalArgumentException("Duration must be a whole number of ticks.");
        SwingAnimation source = SwingAnimation.of(validId(id), (int) ticks);
        if (!json.has("mirror") || !json.get("mirror").isJsonPrimitive() || !json.getAsJsonPrimitive("mirror").isBoolean())
            throw new IllegalArgumentException("mirror must be a JSON boolean.");
        if (json.get("mirror").getAsBoolean()) source = source.mirrored(id);
        List<SwingAnimation.Keyframe> keys = new ArrayList<>();
        for (var element : json.getAsJsonArray("keys")) {
            JsonObject key = element.getAsJsonObject();
            Pose pose = Pose.REST;
            for (String field : FIELDS.subList(1, FIELDS.size())) pose = pose(pose, field, number(key, field));
            keys.add(new SwingAnimation.Keyframe(number(key, "at"), Easing.valueOf(key.get("easing").getAsString().toUpperCase(Locale.ROOT)), pose));
        }
        return copy(source, id, (int) ticks, keys);
    }

    static float number(JsonObject json, String field) {
        var value = json.get(field);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException(field + " must be a JSON number.");
        return value.getAsFloat();
    }

    /** Paste inside SwingAnimations or another class in this package; preserves mirror and blend weights. */
    public static String javaSource(SwingAnimation animation) {
        StringBuilder out = new StringBuilder("SwingAnimation.of(\"" + animation.id() + "\", " + animation.ticks() + ")");
        for (var key : animation.keyframes()) {
            Pose p = key.pose();
            out.append("\n    .key(").append(key.at()).append("f, Easing.").append(key.easing()).append(", ");
            if (p == Pose.REST) out.append("Pose.REST");
            else out.append("Pose.REST.item(").append(p.itemX).append("f, ").append(p.itemY).append("f, ").append(p.itemZ)
                    .append("f).itemRot(").append(p.itemPitch).append("f, ").append(p.itemYaw).append("f, ").append(p.itemRoll)
                    .append("f).scale(").append(p.itemScale).append("f).arm(").append(p.armPitch).append("f, ").append(p.armYaw)
                    .append("f, ").append(p.armRoll).append("f).body(").append(p.bodyYaw).append("f).withWeight(").append(p.weight).append("f)");
            out.append(")");
        }
        if (animation.mirror()) out.append("\n    .mirrored(\"").append(animation.id()).append("\")");
        return out.append(";\n").toString();
    }
}
