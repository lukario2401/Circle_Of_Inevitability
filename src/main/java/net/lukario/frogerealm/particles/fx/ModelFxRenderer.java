package net.lukario.frogerealm.particles.fx;

import java.util.ArrayList;
import java.util.List;

// TODO(redesign): placeholder. The real class (reference/.../particles/fx/ModelFxRenderer.java) drew every
// active ModelFx in a RenderLevelStageEvent (AFTER_PARTICLES) listener: it looked up BakedModels by
// ModelResourceLocation (registered in ModelEvent.RegisterAdditional from models/model_fx/), read their
// BakedQuad vertex arrays through IQuadTransformer, and pushed them into MultiBufferSource with
// Sheets.translucentCullBlockSheet() and RenderType.eyes(). On 26.2 / Forge 65 all of these are gone:
// no RenderLevelStageEvent (AddFramePassEvent instead), no BakedModel / ModelResourceLocation /
// RegisterAdditional (the model system was rebuilt around BlockStateModel and render states), no
// MultiBufferSource / RenderType.eyes in that form, no LightTexture. That is a new renderer, not a port.
// What stays: the list of live ModelFxParticles, track() and clearFollowing(), so ModelFxParticle keeps
// counting ticks, following its entity and being cleared by tag. Nothing is drawn until the redesign.
public final class ModelFxRenderer {

    private static final List<ModelFxParticle> ACTIVE = new ArrayList<>();

    private ModelFxRenderer() {}

    static void track(ModelFxParticle particle) {
        // the old render() dropped dead particles every frame; with no render() they are dropped here instead
        ACTIVE.removeIf(active -> !active.isAlive());
        ACTIVE.add(particle);
    }

    /** Removes the models stuck to this entity id: all of them, or only the ones with this tag ("" = all). */
    static void clearFollowing(int entityId, String tag) {
        for (ModelFxParticle particle : ACTIVE) {
            if (particle.fx().followId() != entityId) continue;
            if (tag.isEmpty() || tag.equals(particle.fx().tagName())) particle.discard();
        }
    }
}
