package net.lukario.circleofinevitability.client.animation;

import com.google.gson.JsonParser;
import net.lukario.circleofinevitability.client.animation.SwingAnimation.Easing;
import net.lukario.circleofinevitability.client.animation.SwingAnimation.Pose;

import java.nio.file.Files;
import javax.tools.ToolProvider;

/** Standalone checks for live edits, interpolation, rejected input, JSON and compilable Java export. */
public final class SwingAnimationEditsTest {
    private static void close(float actual, float expected) {
        if (!Float.isFinite(actual) || Math.abs(actual - expected) > .0001f)
            throw new AssertionError(actual + " != " + expected);
    }

    private static void rejected(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException | IndexOutOfBoundsException expected) {
            return;
        }
        throw new AssertionError("Invalid edit was accepted");
    }

    private static void same(SwingAnimation a, SwingAnimation b) {
        if (!a.id().equals(b.id()) || !SwingAnimationEdits.encode(a).equals(SwingAnimationEdits.encode(b)))
            throw new AssertionError("Animation data did not survive round trip");
        for (int i = 0; i <= 200; i++) {
            Pose x = a.sample(i / 200f), y = b.sample(i / 200f);
            close(x.itemYaw, y.itemYaw);
            close(x.itemX, y.itemX);
            close(x.armRoll, y.armRoll);
            close(x.weight, y.weight);
        }
    }

    public static void main(String[] args) throws Exception {
        SwingAnimation original = SwingAnimation.of("test_cut", 14)
                .key(0, Easing.LINEAR, Pose.REST)
                .key(.3f, Easing.IN_OUT, Pose.REST.item(.1f, .2f, -.1f).itemRot(-110, -70, 20).arm(-40, 60, 5).body(20))
                .key(.5f, Easing.OUT_EXPO, Pose.REST.item(-.3f, .1f, -.2f).itemRot(-110, 80, 30).arm(-45, -60, 10).body(-20))
                .key(1, Easing.IN_OUT, Pose.REST);
        SwingAnimation edited = SwingAnimationEdits.key(original, 2, "item_yaw", 95);
        close(original.keyframes().get(2).pose().itemYaw, 80); // definitions remain immutable
        close(edited.sample(.5f).itemYaw, 95);
        for (String field : SwingAnimationEdits.FIELDS.subList(1, SwingAnimationEdits.FIELDS.size())) {
            float value = field.equals("weight") ? .4f : field.equals("item_scale") ? 1.4f : 3;
            SwingAnimation oneField = SwingAnimationEdits.key(original, 1, field, value);
            close(SwingAnimationEdits.encode(oneField).getAsJsonArray("keys").get(1).getAsJsonObject().get(field).getAsFloat(), value);
        }
        close(SwingAnimationEdits.key(original, 0, "item_x", .2f).keyframes().get(0).pose().weight, 1);
        SwingAnimation reordered = SwingAnimationEdits.key(original, 1, "at", .7f);
        close(reordered.keyframes().get(1).at(), .5f);
        close(reordered.keyframes().get(2).at(), .7f);
        SwingAnimation added = SwingAnimationEdits.add(edited, .7f);
        same(edited, SwingAnimationEdits.remove(added, 3));
        SwingAnimation eased = SwingAnimationEdits.easing(edited, 2, "out_back");
        if (eased.keyframes().get(2).easing() != Easing.OUT_BACK) throw new AssertionError();
        for (SwingAnimation animation : new SwingAnimation[]{original, edited, eased, edited.mirrored("mirrored_cut"), added}) {
            same(animation, SwingAnimationEdits.decode(animation.id(), JsonParser.parseString(SwingAnimationEdits.encode(animation).toString()).getAsJsonObject()));
        }
        SwingAnimation mirrored = edited.mirrored("mirrored_cut");
        close(SwingAnimationEdits.add(mirrored, .6f).sample(.6f).itemYaw, mirrored.sample(.6f).itemYaw);
        rejected(() -> SwingAnimationEdits.key(original, 1, "at", .5f));
        rejected(() -> SwingAnimationEdits.key(original, 1, "at", Float.NaN));
        rejected(() -> SwingAnimationEdits.key(original, 1, "item_scale", 0));
        rejected(() -> SwingAnimationEdits.key(original, 1, "item_x", Float.POSITIVE_INFINITY));
        rejected(() -> SwingAnimationEdits.key(original, 1, "arm_roll", 3601));
        rejected(() -> SwingAnimationEdits.key(original, 1, "weight", 2));
        rejected(() -> SwingAnimationEdits.key(original, 1, "typo", 1));
        rejected(() -> SwingAnimationEdits.remove(original, 99));
        rejected(() -> SwingAnimationEdits.add(original, .3f));
        rejected(() -> SwingAnimationEdits.validId("../../escape"));
        rejected(() -> SwingAnimationEdits.copy(original, "new_cut", 0, original.keyframes()));
        rejected(() -> SwingAnimationEdits.copy(original, "new_cut", 1201, original.keyframes()));
        rejected(() -> SwingAnimationEdits.remove(SwingAnimation.of("two", 10).key(0, Easing.LINEAR, Pose.REST).key(1, Easing.LINEAR, Pose.REST), 0));
        var badJson = SwingAnimationEdits.encode(original);
        badJson.addProperty("ticks", 1.5);
        rejected(() -> SwingAnimationEdits.decode("test_cut", badJson));
        var badPose = SwingAnimationEdits.encode(original);
        badPose.getAsJsonArray("keys").get(1).getAsJsonObject().addProperty("item_scale", -1);
        rejected(() -> SwingAnimationEdits.decode("test_cut", badPose));
        var badMirror = SwingAnimationEdits.encode(original);
        badMirror.addProperty("mirror", "not_a_boolean");
        rejected(() -> SwingAnimationEdits.decode("test_cut", badMirror));
        var badNumber = SwingAnimationEdits.encode(original);
        badNumber.getAsJsonArray("keys").get(1).getAsJsonObject().addProperty("item_x", "not_a_number");
        rejected(() -> SwingAnimationEdits.decode("test_cut", badNumber));

        // Compile the exported fluent expression and compare the resulting mirrored definition, including REST weights.
        var directory = Files.createTempDirectory("swing-export-test-");
        var source = directory.resolve("ExportedSwing.java");
        Files.writeString(source, "package net.lukario.circleofinevitability.client.animation;\n"
                + "import net.lukario.circleofinevitability.client.animation.SwingAnimation.*;\n"
                + "public class ExportedSwing { public static SwingAnimation animation() { return "
                + SwingAnimationEdits.javaSource(mirrored) + " } }");
        int result = ToolProvider.getSystemJavaCompiler().run(null, null, null, "-classpath", System.getProperty("java.class.path"),
                "-d", directory.toString(), source.toString());
        if (result != 0) throw new AssertionError("Java export did not compile");
        // Load in the animation's runtime package to exercise the exported package-private weight helper.
        byte[] bytecode = Files.readAllBytes(directory.resolve("net/lukario/circleofinevitability/client/animation/ExportedSwing.class"));
        Class<?> exported = java.lang.invoke.MethodHandles.lookup().defineClass(bytecode);
        same(mirrored, (SwingAnimation) exported.getMethod("animation").invoke(null));
        System.out.println("Swing animation editing, validation, JSON round trips and Java export passed.");
    }
}
