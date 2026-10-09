package net.lukario.frogerealm.particles.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side renderer for SlashFx: builds a textured ribbon along an arc/spiral/line every frame.
 * The texture runs ALONG the ribbon (x = start..end of the slash, y = inner..outer edge).
 * You shouldn't need to edit this to make new slashes — only to add new SlashFx options.
 */
public class SlashParticle extends Particle {

    private static final float CORE_WIDTH = 0.3f;   // core line width compared to the glow
    private static final float EDGE_SOFTNESS = 0.05f; // fade at the moving ends of the ribbon
    private static final float FADE_START = 0.45f;   // fraction of the lifetime before it starts fading

    private final SlashFx fx;
    private final TextureAtlasSprite sprite;

    // orientation (unit vectors, world space)
    private final Vec3 forward;
    private final Vec3 right;
    private final Vec3 up;

    // spin for the frame being drawn
    private double spinRadians;
    private Vec3 lineRight;
    private Vec3 lineUp;

    // per-streak variation, decided once so the slash doesn't flicker
    private final float[] layerOffset;
    private final float[] layerStart;
    private final float[] layerLength;
    private final float[] layerWidth;
    private final float[] layerAlpha;

    protected SlashParticle(ClientLevel level, double x, double y, double z, SlashFx fx) {
        super(level, x, y, z);
        this.fx = fx;
        this.lifetime = fx.delayTicks() + fx.lifetimeTicks();
        this.gravity = 0f;
        this.hasPhysics = false;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;

        TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager()
                .getTexture(TextureAtlas.LOCATION_PARTICLES);
        this.sprite = atlas.getSprite(fx.texture());

        // forward from yaw/pitch like an entity's look; right/up rotated around it by roll (same math as the server)
        Vec3[] axes = fx.axes();
        this.forward = axes[0];
        this.right = axes[1];
        this.up = axes[2];

        int count = fx.layerCount();
        layerOffset = new float[count];
        layerStart = new float[count];
        layerLength = new float[count];
        layerWidth = new float[count];
        layerAlpha = new float[count];
        for (int i = 0; i < count; i++) {
            if (i == 0) { // the main streak
                layerLength[i] = 1f;
                layerWidth[i] = 1f;
                layerAlpha[i] = 1f;
                continue;
            }
            int side = (i % 2 == 1) ? -1 : 1;        // alternate inside / outside
            int ring = (i + 1) / 2;                   // 1, 1, 2, 2, 3...
            layerOffset[i] = side * ring * fx.spreadBlocks() * (0.75f + random.nextFloat() * 0.5f);
            layerLength[i] = 0.55f + random.nextFloat() * 0.35f;
            layerStart[i] = random.nextFloat() * (1f - layerLength[i]);
            layerWidth[i] = 0.4f + random.nextFloat() * 0.35f;
            layerAlpha[i] = 0.5f + random.nextFloat() * 0.4f;
        }
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) this.remove();
    }

    @Override
    public boolean shouldCull() {
        return false; // the slash is much bigger than a normal particle's box
    }

    @Override
    public ParticleRenderType getRenderType() {
        return fx.isAdditive() ? SlashRenderTypes.ADDITIVE : SlashRenderTypes.TRANSLUCENT;
    }

    // =========================
    // Drawing
    // =========================

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        float time = this.age + partialTick - fx.delayTicks();
        if (time < 0) return;

        float life = fx.lifetimeTicks();
        float sweep = Math.min(fx.sweepTicks(), life * 0.5f); // longer sweeps would never be fully visible
        float head;
        float tail;
        if (fx.growsFromCenter()) {
            float grow = Mth.clamp(time / sweep, 0f, 1f);          // both ends move out from the middle
            head = 0.5f + 0.5f * grow;
            tail = 0.5f - 0.5f * grow;
        } else {
            head = Mth.clamp(time / sweep, 0f, 1f);                  // front end, draws in
            tail = Mth.clamp((time - (life - sweep)) / sweep, 0f, 1f); // back end, wipes away at the end
        }
        if (head - tail < 0.002f) return;

        float progress = time / life;
        float fade = progress < FADE_START ? 1f : 1f - smooth((progress - FADE_START) / (1f - FADE_START));
        if (fade <= 0.01f) return;

        // spin: arcs turn around their center, lines turn like a propeller
        spinRadians = Math.toRadians(fx.spinDegrees() * time);
        double cos = Math.cos(spinRadians);
        double sin = Math.sin(spinRadians);
        lineRight = right.scale(cos).add(up.scale(sin));
        lineUp = up.scale(cos).subtract(right.scale(sin));

        Vec3 cam = camera.getPosition();
        Vec3 origin = new Vec3(this.x - cam.x, this.y - cam.y, this.z - cam.z);

        for (int i = 0; i < layerOffset.length; i++) {
            ribbon(buffer, origin, i, head, tail, fade, 0, 1f, true);
            if ((fx.coreColor() >>> 24) != 0) {
                ribbon(buffer, origin, i, head, tail, fade, fx.coreColor(), CORE_WIDTH, false);
            }
        }
    }

    /**
     * Draws one streak between the tail and head positions (0..1 along the whole slash).
     * gradient = true: color fades tail -> color -> head along the slash; false: uses fixedColor.
     */
    private void ribbon(VertexConsumer buffer, Vec3 origin, int layer, float head, float tail, float fade,
                        int fixedColor, float widthScale, boolean gradient) {
        float start = layerStart[layer];
        float end = start + layerLength[layer];
        float t0 = Math.max(tail, start);
        float t1 = Math.min(head, end);
        if (t1 - t0 < 0.002f) return;

        float alphaScale = fade * layerAlpha[layer];
        int segments = segments();
        int firstIndex = (int) Math.ceil(t0 * segments);
        int lastIndex = (int) Math.floor(t1 * segments);

        Vec3[] previous = null;
        float previousU = 0f;
        float[] previousColor = null;

        // walk t0 -> segment points -> t1
        for (int k = firstIndex - 1; k <= lastIndex + 1; k++) {
            float t;
            if (k == firstIndex - 1) t = t0;
            else if (k == lastIndex + 1) t = t1;
            else t = (float) k / segments;
            if (t < t0 || t > t1) continue;

            float local = (t - start) / (end - start); // 0..1 along this streak
            float halfWidth = fx.widthBlocks() * layerWidth[layer] * widthScale * profile(local) * 0.5f;
            Vec3[] edges = edges(origin, t, layer, halfWidth);

            // soften the ends that are still moving (the real ends are already thin from the taper)
            float tailFade = t0 > start + 0.0001f ? (t - t0) / EDGE_SOFTNESS : 1f;
            float headFade = t1 < end - 0.0001f ? (t1 - t) / EDGE_SOFTNESS : 1f;
            float edgeFade = Mth.clamp(Math.min(tailFade, headFade), 0f, 1f);

            float[] color = rgba(gradient ? colorAt(t) : fixedColor);
            color[3] *= alphaScale * edgeFade;

            float u = Mth.lerp(0.02f + local * 0.96f, sprite.getU0(), sprite.getU1());
            if (previous != null) {
                quad(buffer, previous, edges, previousU, u, previousColor, color);
            }
            previous = edges;
            previousU = u;
            previousColor = color;
        }
    }

    /**
     * Color along the slash: tail color for the back quarter, blends into the main color by the middle,
     * main color until 60%, blends into the head color by 90%.
     */
    private int colorAt(float t) {
        if (t < 0.5f) return lerpColor(fx.tailGlowColor(), fx.glowColor(), Mth.clamp((t - 0.25f) / 0.25f, 0f, 1f));
        return lerpColor(fx.glowColor(), fx.headGlowColor(), Mth.clamp((t - 0.6f) / 0.3f, 0f, 1f));
    }

    private static int lerpColor(int from, int to, float amount) {
        int result = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int a = (from >>> shift) & 0xFF;
            int b = (to >>> shift) & 0xFF;
            result |= (Math.round(a + (b - a) * amount) & 0xFF) << shift;
        }
        return result;
    }

    private static float[] rgba(int argb) {
        return new float[]{((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f,
                ((argb >>> 24) & 0xFF) / 255f};
    }

    /** Point on the slash's main line at position t (camera-relative), before any streak offset. */
    private Vec3 pathAt(Vec3 origin, float t) {
        if (fx.shape() == SlashFx.Shape.LINE) {
            return origin.add(lineRight.scale((t * 2f - 1f) * fx.radiusBlocks()));
        }
        double radius = Mth.lerp(t, fx.radiusBlocks(), fx.endRadiusBlocks());
        return origin.add(radialAt(t).scale(radius));
    }

    /** Direction from the arc's center to position t (in the slash's plane), including spin. */
    private Vec3 radialAt(float t) {
        double angle = Math.toRadians(-fx.arcDegrees() / 2.0 + t * fx.arcDegrees()) + spinRadians;
        return right.scale(Math.sin(angle)).add(forward.scale(Math.cos(angle)));
    }

    /** Inner and outer edge of one streak at position t (camera-relative). */
    private Vec3[] edges(Vec3 origin, float t, int layer, float halfWidth) {
        Vec3 path = pathAt(origin, t);
        Vec3 planeAcross = fx.shape() == SlashFx.Shape.LINE ? lineUp : radialAt(t); // flat: width lies in the plane
        Vec3 across = planeAcross;

        if (!fx.isFlat()) {
            // turn the width toward the camera: perpendicular to the ribbon's direction AND to the view ray
            // (the camera is at 0,0,0 here, so the view ray to this point is just 'path')
            Vec3 tangent = pathAt(origin, t + 0.002f).subtract(pathAt(origin, t - 0.002f));
            Vec3 facing = tangent.cross(path);
            double length = facing.length();
            if (length > 1.0E-6) {
                facing = facing.scale(1.0 / length);
                across = facing.dot(planeAcross) < 0 ? facing.scale(-1) : facing; // keep inner/outer the same way round
            }
        }

        // extra streaks sit side by side across the ribbon, so they stay apart from any angle
        Vec3 center = path.add(across.scale(layerOffset[layer]));
        return new Vec3[]{center.subtract(across.scale(halfWidth)), center.add(across.scale(halfWidth))};
    }

    private void quad(VertexConsumer buffer, Vec3[] from, Vec3[] to, float u0, float u1, float[] c0, float[] c1) {
        float vInner = Mth.lerp(0.03f, sprite.getV0(), sprite.getV1());
        float vOuter = Mth.lerp(0.97f, sprite.getV0(), sprite.getV1());
        // front
        vertex(buffer, from[0], u0, vInner, c0);
        vertex(buffer, from[1], u0, vOuter, c0);
        vertex(buffer, to[1], u1, vOuter, c1);
        vertex(buffer, to[0], u1, vInner, c1);
        // back (so it's visible from both sides)
        vertex(buffer, to[0], u1, vInner, c1);
        vertex(buffer, to[1], u1, vOuter, c1);
        vertex(buffer, from[1], u0, vOuter, c0);
        vertex(buffer, from[0], u0, vInner, c0);
    }

    private static void vertex(VertexConsumer buffer, Vec3 pos, float u, float v, float[] c) {
        buffer.addVertex((float) pos.x, (float) pos.y, (float) pos.z)
                .setUv(u, v)
                .setColor(c[0], c[1], c[2], c[3])
                .setLight(LightTexture.FULL_BRIGHT);
    }

    /** Thickness along the streak (0..1). */
    private float profile(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return switch (fx.taperStyle()) {
            case CRESCENT -> (float) Math.pow(Math.sin(Math.PI * t), 0.8);
            case COMET -> t < 0.85f
                    ? (float) Math.pow(t / 0.85f, 1.3)
                    : 1f - (t - 0.85f) / 0.15f;
            case UNIFORM -> Math.min(1f, Math.min(t, 1f - t) / 0.06f);
        };
    }

    private int segments() {
        if (fx.shape() == SlashFx.Shape.LINE) return 12;
        return Mth.clamp((int) (Math.abs(fx.arcDegrees()) / 6f), 8, 128);
    }

    private static float smooth(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    /** Registered in ClientParticleHandler. */
    public static Particle create(SlashFx fx, ClientLevel level,
                                  double x, double y, double z, double xd, double yd, double zd) {
        return new SlashParticle(level, x, y, z, fx);
    }
}
