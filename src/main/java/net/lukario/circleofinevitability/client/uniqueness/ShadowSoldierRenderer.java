package net.lukario.circleofinevitability.client.uniqueness;

import com.mojang.blaze3d.vertex.PoseStack;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.ShadowSoldier;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.ShadowSoldiers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/** Draws shadow soldiers in their palette (shadow, usurper, loyal) with burning eyes, sunk into the ground while rising. */
public final class ShadowSoldierRenderer extends MobRenderer<ShadowSoldier, ShadowSoldierRenderer.State, ShadowSoldierModel> {
    public static final class State extends LivingEntityRenderState {
        public int rank;
        public String palette = ShadowSoldier.SHADOW;
        public float rise = 1;
        public float attackTime;
    }

    private static Identifier texture(String name) {
        return Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "textures/entity/red_priest/" + name + ".png");
    }

    private static final Map<String, Identifier> TEXTURES = Map.of(
            ShadowSoldier.SHADOW, texture("shadow"), ShadowSoldier.USURPER, texture("usurper"), ShadowSoldier.LOYAL, texture("loyal"));
    private static final Map<String, RenderType> GLOW = Map.of(
            ShadowSoldier.SHADOW, RenderTypes.eyes(texture("shadow_emissive")),
            ShadowSoldier.USURPER, RenderTypes.eyes(texture("usurper_emissive")),
            ShadowSoldier.LOYAL, RenderTypes.eyes(texture("loyal_emissive")));

    public ShadowSoldierRenderer(EntityRendererProvider.Context context) {
        super(context, new ShadowSoldierModel(context.bakeLayer(ShadowSoldierModel.LAYER)), .5f);
        addLayer(new EyesLayer<>(this) {
            private String palette = ShadowSoldier.SHADOW;

            @Override
            public void submit(PoseStack pose, SubmitNodeCollector collector, int light, State state, float yRot, float xRot) {
                palette = state.palette;
                super.submit(pose, collector, light, state, yRot, xRot);
            }

            @Override
            public RenderType renderType() {
                return GLOW.getOrDefault(palette, GLOW.get(ShadowSoldier.SHADOW));
            }
        });
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return TEXTURES.getOrDefault(state.palette, TEXTURES.get(ShadowSoldier.SHADOW));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ShadowSoldier soldier, State state, float partialTick) {
        super.extractRenderState(soldier, state, partialTick);
        state.rank = soldier.rank();
        state.palette = soldier.palette();
        state.rise = Math.clamp((soldier.tickCount + partialTick) / ShadowSoldier.RISE_TICKS, 0f, 1f);
        state.attackTime = soldier.getAttackAnim(partialTick);
    }

    /** While it rises it is drawn sunk into the ground, and climbs out. */
    @Override
    protected void scale(State state, PoseStack pose) {
        if (state.rise < 1) {
            float t = state.rise;
            pose.translate(0, (1 - t * t * (3 - 2 * t)) * 2.1f, 0);
        }
    }

    @Override
    protected float getShadowRadius(State state) {
        return super.getShadowRadius(state) * state.rise;
    }

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, value = Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent
        public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(ShadowSoldierModel.LAYER, ShadowSoldierModel::createLayer);
        }

        @SubscribeEvent
        public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ShadowSoldiers.SOLDIER.get(), ShadowSoldierRenderer::new);
        }
    }
}
