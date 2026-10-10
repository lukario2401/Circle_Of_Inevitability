package net.lukario.frogerealm.particles.fx;

// TODO(redesign): placeholder. The real class (reference/.../particles/fx/SlashRenderTypes.java) defined two
// ParticleRenderType objects (ADDITIVE and TRANSLUCENT) that set the blend mode, the depth mask and the
// particle atlas in begin(Tesselator, TextureManager), plus a RenderLevelStageEvent listener that put the
// blend mode back after particles. None of that exists on 26.2 / Forge 65: ParticleRenderType is now a final
// record (name, shorthand) that only names a ParticleGroup, RenderSystem has no blend/depth state any more
// (that lives in a RenderPipeline), and RenderLevelStageEvent is gone (AddFramePassEvent replaces it).
// Rebuilding it means a custom RenderPipeline per blend mode plus a ParticleGroup registered with
// ParticleEngine.registerParticleGroup (see MIGRATION_NOTES.md, "Particle pattern"). Until then
// SlashParticle draws nothing (its getGroup() returns NO_RENDER).
public final class SlashRenderTypes {

    private SlashRenderTypes() {}
}
