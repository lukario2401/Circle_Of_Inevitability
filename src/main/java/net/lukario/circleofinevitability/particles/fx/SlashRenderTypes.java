package net.lukario.circleofinevitability.particles.fx;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * How slash trails are blended with the world. Both use the particle atlas (textures/particle/),
 * don't write depth (so overlapping streaks don't cut each other) and are drawn with the vanilla see-through particles.
 * Any future particle can use these too: return SlashRenderTypes.ADDITIVE from SingleQuadParticle.getLayer().
 * (26.2: blend and depth state live in a RenderPipeline, and particles are drawn by their ParticleGroup; SLASHES is the
 * group SlashParticles go in, registered once from ClientParticleHandler.)
 */
public final class SlashRenderTypes {

    private SlashRenderTypes() {}

    private static final DepthStencilState NO_DEPTH_WRITE =
            new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false);

    /** Glowing: colors are added on top of what's behind (overlaps get brighter, turn white). */
    public static final SingleQuadParticle.Layer ADDITIVE = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES,
            RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "pipeline/slash_additive"))
                    .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING)) // SRC_ALPHA, ONE
                    .withDepthStencilState(NO_DEPTH_WRITE)
                    .build());

    /** Normal see-through blending. */
    public static final SingleQuadParticle.Layer TRANSLUCENT = new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES,
            RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "pipeline/slash_translucent"))
                    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                    .withDepthStencilState(NO_DEPTH_WRITE)
                    .build());

    /** The particle group every SlashParticle goes in. */
    public static final ParticleRenderType SLASHES = new ParticleRenderType("CIRCLE_OF_INEVITABILITY_SLASHES", "COIS");

    private static boolean registered;

    /**
     * Tells the particle engine how to draw SLASHES. Called once from ClientParticleHandler; never throws.
     * If it fails, slashes keep ticking but are not drawn (SlashParticle then stays NO_RENDER).
     */
    public static void register() {
        if (registered) return;
        try {
            ParticleEngine.registerParticleGroup(SLASHES, SlashGroup::new);
            registered = true;
        } catch (RuntimeException | LinkageError e) {
            CircleOfInevitability.LOGGER.error("Could not register the slash particle group; slashes will not be drawn", e);
        }
    }

    static boolean isRegistered() {
        return registered;
    }

    // =========================
    // Group: collects the slashes of this frame, the particle engine then builds their quads
    // =========================

    private static final class SlashGroup extends ParticleGroup<SlashParticle> {

        private SlashGroup(ParticleEngine engine) {
            super(engine);
        }

        @Override
        public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTick) {
            // not culled: a slash is much bigger than a normal particle's box
            return new SlashState(new ArrayList<>(this.particles), camera.position(), partialTick);
        }
    }

    /**
     * Drawn by vanilla's particle renderer, like any see-through particle; instead of one square per particle
     * it asks each slash for its ribbon.
     */
    private static final class SlashState extends QuadParticleRenderState {

        private final List<SlashParticle> slashes;
        private final Vec3 camera;
        private final float partialTick;
        private final Set<SingleQuadParticle.Layer> layers = new LinkedHashSet<>();

        private SlashState(List<SlashParticle> slashes, Vec3 camera, float partialTick) {
            this.slashes = slashes;
            this.camera = camera;
            this.partialTick = partialTick;
            for (SlashParticle slash : slashes) layers.add(slash.layer());
        }

        @Override
        public boolean isEmpty() {
            return slashes.isEmpty();
        }

        @Override
        public Set<SingleQuadParticle.Layer> layers() {
            return layers;
        }

        @Override
        public void buildLayer(SingleQuadParticle.Layer layer, VertexConsumer buffer) {
            for (SlashParticle slash : slashes) {
                if (slash.layer() == layer) slash.render(buffer, camera, partialTick);
            }
        }

        @Override
        public void submit(SubmitNodeCollector submitNodeCollector, CameraRenderState cameraState) {
            if (!slashes.isEmpty()) submitNodeCollector.submitQuadParticleGroup(this);
        }

        @Override
        public void clear() {
            slashes.clear();
            layers.clear();
        }
    }
}
