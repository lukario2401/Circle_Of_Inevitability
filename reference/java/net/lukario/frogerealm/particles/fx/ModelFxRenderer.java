package net.lukario.frogerealm.particles.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.client.ClientConcealment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws every active ModelFx, right after vanilla particles.
 * Order: normal models, then .seeThrough() ones (far to near), then the glowing auras on top.
 * Models with .frames(n) show one of their models, or (with .smooth()) a shape in between two of them.
 * Also loads every model in models/model_fx/ automatically.
 * You shouldn't need to edit this to make new effects — only to add new ModelFx options.
 */
public final class ModelFxRenderer {

    private static final String VARIANT = "standalone";
    /** Plain white texture the auras are drawn with (textures/model_fx/glow_white.png). */
    private static final ResourceLocation WHITE =
            ResourceLocation.fromNamespaceAndPath(ForgeRealm.MOD_ID, ModelFx.FOLDER + "/glow_white");
    private static final Direction[] SIDES = Arrays.copyOf(Direction.values(), Direction.values().length + 1); // + null

    private static final List<ModelFxParticle> ACTIVE = new ArrayList<>();
    private static final Map<ResourceLocation, ModelResourceLocation> LOCATIONS = new HashMap<>();
    private static final RandomSource RANDOM = RandomSource.create();

    // scratch space (render thread only)
    private static final float[] PX = new float[4], PY = new float[4], PZ = new float[4];
    private static final float[] U = new float[4], V = new float[4];
    // the same corners in the frame it is gliding toward
    private static final float[] QX = new float[4], QY = new float[4], QZ = new float[4];
    private static final float[] QU = new float[4], QV = new float[4];
    private static final Vector3f POSITION = new Vector3f();
    private static final Vector3f NORMAL = new Vector3f();
    /**
     * Where Minecraft's main light comes from (the same in every dimension). A face that looks this way gets
     * full brightness, so .unshaded() models tell the game that all their faces look this way.
     */
    private static final Vector3f FULLY_LIT = new Vector3f(0.2f, 1.0f, -0.7f).normalize();

    private ModelFxRenderer() {}

    /** next / blend: with .smooth() frames, the model it is gliding toward and how far it has got (0..1). next = null: just 'model'. */
    private record Drawn(ModelFx fx, BakedModel model, BakedModel next, float blend, boolean mirrored,
                         Matrix4f pose, Matrix3f normal, float alpha, int light, float modelScale, double distance) {}

    static void track(ModelFxParticle particle) {
        ACTIVE.add(particle);
    }

    /** Removes the models stuck to this entity id: all of them, or only the ones with this tag ("" = all). */
    static void clearFollowing(int entityId, String tag) {
        for (ModelFxParticle particle : ACTIVE) {
            if (particle.fx().followId() != entityId) continue;
            if (tag.isEmpty() || tag.equals(particle.fx().tagName())) particle.discard();
        }
    }

    /** The baked-model key used for a model_fx model. */
    public static ModelResourceLocation location(ResourceLocation model) {
        return LOCATIONS.computeIfAbsent(model, id -> new ModelResourceLocation(id, VARIANT));
    }

    // =========================
    // Drawing
    // =========================

    private static void render(Camera camera, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            ACTIVE.clear();
            return;
        }
        ACTIVE.removeIf(particle -> !particle.isAlive() || particle.isStale(level));
        if (ACTIVE.isEmpty()) return;

        Vec3 cam = camera.getPosition();
        ModelManager models = mc.getModelManager();
        List<Drawn> drawn = new ArrayList<>();

        for (ModelFxParticle particle : ACTIVE) {
            ModelFx fx = particle.fx();
            float time = particle.time(partialTick);
            if (time < 0 || time > fx.lifetimeTicks()) continue;
            // what is stuck to someone concealed is not drawn either: it would give them away (see status/Concealment)
            if (fx.followsEntity() && ClientConcealment.isHidden(fx.followId())) continue;
            // what you wear on your own head is not drawn in first person, like a helmet: it would hang in your face
            if (fx.turnsWithHead() && !camera.isDetached() && camera.getEntity() != null
                    && camera.getEntity().getId() == fx.followId()) continue;

            ModelFx.Pose pose = fx.poseAt(time);
            float alpha = ((fx.tintColor() >>> 24) / 255f) * pose.alpha() * fx.fadeAt(time);
            if (alpha <= 0.004f) continue;
            alpha = Math.min(alpha, 1f);

            float scale = Math.abs(pose.scale()) < 0.001f ? 0.001f : pose.scale();
            Vec3 anchor = particle.anchor(partialTick);
            Matrix4f matrix = new Matrix4f()
                    .translate((float) (anchor.x - cam.x), (float) (anchor.y - cam.y), (float) (anchor.z - cam.z));
            // facing: the model's front (north, -Z) turns to the yaw; pitch tips it forward, roll to the right
            // (turn = how far the entity it is stuck to has turned, for the models that turn with it)
            if (fx.turnsWithHead()) {
                // worn on a head: first where the head looks, left/right and (around the neck) up/down,
                // then the model's own yaw on that head
                float neck = particle.aboveNeck();
                matrix.rotateY(rad(180f - particle.turn(partialTick)))
                        .translate(0f, -neck, 0f)
                        .rotateX(rad(-particle.nod(partialTick)))
                        .translate(0f, neck, 0f)
                        .rotateY(rad(-fx.yaw()));
            } else {
                matrix.rotateY(rad(180f - fx.yaw() - particle.turn(partialTick)));
            }
            matrix.rotateX(rad(-fx.pitch()))
                    .rotateZ(rad(-fx.roll()))
                    // keyframe: offset (right, up, forward = -Z), then rotation around the pivot
                    .translate(pose.right(), pose.up(), -pose.forward())
                    .rotateY(rad(-pose.yaw()))
                    .rotateX(rad(-pose.pitch()))
                    .rotateZ(rad(-pose.roll()))
                    // spin: around the model's own up axis, so it works however the model is tilted
                    .rotateY(rad(-(pose.spin() + fx.spinDegrees() * time)))
                    .scale(fx.scaleX() * scale, fx.scaleY() * scale, fx.scaleZ() * scale)
                    .translate(-fx.pivotX() / 16f, -fx.pivotY() / 16f, -fx.pivotZ() / 16f);
            Matrix3f normal = matrix.normal(new Matrix3f());

            int light = fx.glows() ? LightTexture.FULL_BRIGHT
                    : LevelRenderer.getLightColor(level, BlockPos.containing(anchor));
            float modelScale = (Math.abs(fx.scaleX()) + Math.abs(fx.scaleY()) + Math.abs(fx.scaleZ())) / 3f * Math.abs(scale);

            BakedModel model;
            BakedModel next = null;
            float blend = 0f;
            int frames = fx.frameCount();
            if (frames > 1) {
                float frame = Mth.clamp(pose.frame(), 0f, frames - 1f);
                if (fx.isSmooth()) {
                    int from = Math.min((int) frame, frames - 2);
                    blend = frame - from;
                    model = models.getModel(location(fx.frameModel(from)));
                    if (blend > 0.999f) {
                        model = models.getModel(location(fx.frameModel(from + 1)));
                    } else if (blend > 0.001f) {
                        next = models.getModel(location(fx.frameModel(from + 1)));
                    }
                } else {
                    model = models.getModel(location(fx.frameModel(Math.round(frame))));
                }
            } else {
                model = models.getModel(location(fx.model()));
            }
            // a mirrored model (negative scale on one axis) has its faces wound the other way round
            boolean mirrored = matrix.determinant() < 0f;
            drawn.add(new Drawn(fx, model, next, blend, mirrored, matrix, normal, alpha, light, modelScale,
                    anchor.distanceToSqr(cam)));
        }
        if (drawn.isEmpty()) return;
        drawn.sort((a, b) -> Double.compare(b.distance(), a.distance())); // far first, so see-through ones blend right

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        RenderType modelType = Sheets.translucentCullBlockSheet();

        // 1. normal models
        drawPass(buffers, modelType, drawn, false);
        // 2. see-through models (ice, glass...), after everything they might contain
        drawPass(buffers, modelType, drawn, true);

        // 3. auras: additive light on top (doesn't hide anything)
        RenderType glowType = RenderType.eyes(TextureAtlas.LOCATION_BLOCKS);
        TextureAtlasSprite white = mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WHITE);
        float whiteU = (white.getU0() + white.getU1()) * 0.5f;
        float whiteV = (white.getV0() + white.getV1()) * 0.5f;
        VertexConsumer glow = null;
        for (Drawn d : drawn) {
            int aura = d.fx().auraColor();
            float strength = ((aura >>> 24) / 255f) * d.alpha();
            if (strength <= 0.004f) continue;
            if (glow == null) glow = buffers.getBuffer(glowType);
            int layers = d.fx().auraLayerCount();
            for (int layer = 1; layer <= layers; layer++) {
                // inner layers are stronger; they all overlap near the model, only the outer ones reach the edge
                float intensity = strength * (1f - (layer - 1f) / layers) * 0.9f / layers;
                float inflate = d.fx().auraBlocks() * layer / layers / Math.max(d.modelScale(), 0.001f);
                drawModel(glow, d, ((aura >> 16) & 0xFF) / 255f * intensity, ((aura >> 8) & 0xFF) / 255f * intensity,
                        (aura & 0xFF) / 255f * intensity, intensity, LightTexture.FULL_BRIGHT, inflate, whiteU, whiteV);
            }
        }
        if (glow != null) buffers.endBatch(glowType);
    }

    private static void drawPass(MultiBufferSource.BufferSource buffers, RenderType type, List<Drawn> drawn, boolean seeThrough) {
        VertexConsumer consumer = null;
        for (Drawn d : drawn) {
            if (d.fx().isSeeThrough() != seeThrough) continue;
            if (consumer == null) consumer = buffers.getBuffer(type);
            int tint = d.fx().tintColor();
            drawModel(consumer, d, ((tint >> 16) & 0xFF) / 255f, ((tint >> 8) & 0xFF) / 255f, (tint & 0xFF) / 255f,
                    d.alpha(), d.light(), 0f, Float.NaN, Float.NaN);
        }
        if (consumer != null) buffers.endBatch(type);
    }

    /**
     * Every quad of the model. inflate > 0 pushes each face outwards (in model blocks) for aura shells;
     * flatU/flatV (not NaN) = use that one texture point instead of the model's texture.
     */
    private static void drawModel(VertexConsumer consumer, Drawn d, float r, float g, float b, float a, int light,
                                  float inflate, float flatU, float flatV) {
        for (Direction side : SIDES) {
            RANDOM.setSeed(42L);
            List<BakedQuad> quads = d.model().getQuads(null, side, RANDOM);
            List<BakedQuad> toward = null;
            if (d.next() != null) {
                RANDOM.setSeed(42L);
                toward = d.next().getQuads(null, side, RANDOM);
                if (toward.size() != quads.size()) {
                    // the two frames are not made of the same faces: no gliding, show the nearer one
                    if (d.blend() >= 0.5f) quads = toward;
                    toward = null;
                }
            }
            for (int i = 0; i < quads.size(); i++) {
                drawQuad(consumer, quads.get(i), toward == null ? null : toward.get(i), d.blend(), d.mirrored(),
                        d.pose(), d.normal(), d.fx().isUnshaded(), r, g, b, a, light, inflate, flatU, flatV);
            }
        }
    }

    /**
     * Moves the corners in PX/PY/PZ 'blend' of the way to the same face in the next frame. Returns false when that
     * face cannot be read. Which corner belongs to which is found by the texture: the corner that shows the same
     * spot of the texture is the same corner of the same cube, however the cube was turned in between.
     */
    private static boolean glideToward(BakedQuad toward, float blend) {
        int[] data = toward.getVertices();
        int stride = IQuadTransformer.STRIDE;
        if (data.length < stride * 4) return false;
        for (int i = 0; i < 4; i++) {
            int position = i * stride + IQuadTransformer.POSITION;
            int uv = i * stride + IQuadTransformer.UV0;
            QX[i] = Float.intBitsToFloat(data[position]);
            QY[i] = Float.intBitsToFloat(data[position + 1]);
            QZ[i] = Float.intBitsToFloat(data[position + 2]);
            QU[i] = Float.intBitsToFloat(data[uv]);
            QV[i] = Float.intBitsToFloat(data[uv + 1]);
        }
        int shift = -1;
        for (int s = 0; s < 4 && shift != -2; s++) {
            boolean same = true;
            for (int i = 0; i < 4 && same; i++) {
                int j = (i + s) & 3;
                same = Math.abs(U[i] - QU[j]) < 1.0E-5f && Math.abs(V[i] - QV[j]) < 1.0E-5f;
            }
            if (same) shift = shift == -1 ? s : -2;   // -2: fits more than one way, the texture can't tell
        }
        if (shift < 0) {
            // painted differently in the two frames: take the turn that moves the corners least
            float best = Float.MAX_VALUE;
            for (int s = 0; s < 4; s++) {
                float cost = 0f;
                for (int i = 0; i < 4; i++) {
                    int j = (i + s) & 3;
                    float dx = PX[i] - QX[j], dy = PY[i] - QY[j], dz = PZ[i] - QZ[j];
                    cost += dx * dx + dy * dy + dz * dz;
                }
                if (cost < best) {
                    best = cost;
                    shift = s;
                }
            }
        }
        for (int i = 0; i < 4; i++) {
            int j = (i + shift) & 3;
            PX[i] += (QX[j] - PX[i]) * blend;
            PY[i] += (QY[j] - PY[i]) * blend;
            PZ[i] += (QZ[j] - PZ[i]) * blend;
        }
        return true;
    }

    private static void drawQuad(VertexConsumer consumer, BakedQuad quad, BakedQuad toward, float blend, boolean mirrored,
                                 Matrix4f pose, Matrix3f normalMatrix,
                                 boolean unshaded, float r, float g, float b, float a, int light,
                                 float inflate, float flatU, float flatV) {
        int[] data = quad.getVertices();
        int stride = IQuadTransformer.STRIDE;
        if (data.length < stride * 4) return;

        for (int i = 0; i < 4; i++) {
            int position = i * stride + IQuadTransformer.POSITION;
            int uv = i * stride + IQuadTransformer.UV0;
            PX[i] = Float.intBitsToFloat(data[position]);
            PY[i] = Float.intBitsToFloat(data[position + 1]);
            PZ[i] = Float.intBitsToFloat(data[position + 2]);
            U[i] = Float.intBitsToFloat(data[uv]);
            V[i] = Float.intBitsToFloat(data[uv + 1]);
        }

        // gliding between two frames: the face is somewhere between where it is in this frame and in the next.
        // Half way through a turn it no longer looks along the direction the model file gave it, so its normal
        // is taken from its corners as they are (the corners of a face always go round the same way).
        boolean gliding = toward != null && glideToward(toward, blend);

        // face normal from the corners (also right for rotated cubes), pointing the way the face looks
        float e1x = PX[1] - PX[0], e1y = PY[1] - PY[0], e1z = PZ[1] - PZ[0];
        float e2x = PX[3] - PX[0], e2y = PY[3] - PY[0], e2z = PZ[3] - PZ[0];
        float nx = e1y * e2z - e1z * e2y;
        float ny = e1z * e2x - e1x * e2z;
        float nz = e1x * e2y - e1y * e2x;
        float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        Direction direction = quad.getDirection();
        if (length < 1.0E-7f) {
            nx = direction.getStepX();
            ny = direction.getStepY();
            nz = direction.getStepZ();
        } else {
            nx /= length;
            ny /= length;
            nz /= length;
            if (!gliding && nx * direction.getStepX() + ny * direction.getStepY() + nz * direction.getStepZ() < 0) {
                nx = -nx;
                ny = -ny;
                nz = -nz;
            }
        }

        if (inflate > 0f) {
            // grow the face by 'inflate' on every side and push it out by 'inflate',
            // so neighbouring faces of a cube still meet at the edges
            float l1 = (float) Math.sqrt(e1x * e1x + e1y * e1y + e1z * e1z);
            float l2 = (float) Math.sqrt(e2x * e2x + e2y * e2y + e2z * e2z);
            if (l1 > 1.0E-6f && l2 > 1.0E-6f) {
                e1x /= l1; e1y /= l1; e1z /= l1;
                e2x /= l2; e2y /= l2; e2z /= l2;
                float cx = (PX[0] + PX[1] + PX[2] + PX[3]) * 0.25f;
                float cy = (PY[0] + PY[1] + PY[2] + PY[3]) * 0.25f;
                float cz = (PZ[0] + PZ[1] + PZ[2] + PZ[3]) * 0.25f;
                for (int i = 0; i < 4; i++) {
                    float dx = PX[i] - cx, dy = PY[i] - cy, dz = PZ[i] - cz;
                    float s1 = Math.signum(dx * e1x + dy * e1y + dz * e1z);
                    float s2 = Math.signum(dx * e2x + dy * e2y + dz * e2z);
                    PX[i] += inflate * (s1 * e1x + s2 * e2x + nx);
                    PY[i] += inflate * (s1 * e1y + s2 * e2y + ny);
                    PZ[i] += inflate * (s1 * e1z + s2 * e2z + nz);
                }
            }
        }

        if (unshaded) {
            NORMAL.set(FULLY_LIT);               // the game only uses the normal to decide how bright the face is
        } else {
            NORMAL.set(nx, ny, nz);
            normalMatrix.transform(NORMAL);
            NORMAL.normalize();
        }
        boolean flat = !Float.isNaN(flatU);

        for (int n = 0; n < 4; n++) {
            int i = mirrored ? 3 - n : n;        // mirrored: go round the other way, or the game hides the face as a back side
            POSITION.set(PX[i], PY[i], PZ[i]);
            pose.transformPosition(POSITION);
            consumer.addVertex(POSITION.x(), POSITION.y(), POSITION.z())
                    .setColor(r, g, b, a)
                    .setUv(flat ? flatU : U[i], flat ? flatV : V[i])
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(NORMAL.x(), NORMAL.y(), NORMAL.z());
        }
    }

    private static float rad(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    // =========================
    // Events
    // =========================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ModelFxRenderEvents {

        private ModelFxRenderEvents() {}

        @SubscribeEvent
        public static void onModelFxRenderStage(RenderLevelStageEvent event) {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
            render(event.getCamera(), event.getPartialTick());
        }
    }

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModelFxModelEvents {

        private ModelFxModelEvents() {}

        // Load every models/model_fx/**.json (any namespace) so ModelFx.of("name") just works.
        @SubscribeEvent
        public static void onModelFxRegisterModels(ModelEvent.RegisterAdditional event) {
            try {
                ResourceManager resources = Minecraft.getInstance().getResourceManager();
                Map<ResourceLocation, Resource> files = resources.listResources("models/" + ModelFx.FOLDER,
                        file -> file.getPath().endsWith(".json"));
                for (ResourceLocation file : files.keySet()) {
                    String path = file.getPath(); // models/model_fx/order_hammer.json
                    String id = path.substring("models/".length(), path.length() - ".json".length());
                    event.register(new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(file.getNamespace(), id), VARIANT));
                }
            } catch (Exception e) {
                ForgeRealm.LOGGER.error("Could not load the model_fx models", e);
            }
        }
    }
}
