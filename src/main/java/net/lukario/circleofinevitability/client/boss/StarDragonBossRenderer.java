package net.lukario.circleofinevitability.client.boss;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.boss.star_dragon.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class StarDragonBossRenderer extends MobRenderer<StarDragonBoss, StarDragonBossRenderer.State, StarDragonBossModel> {
    private static Identifier texture(String name) { return Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID,"textures/entity/star_dragon_boss/"+name+".png"); }
    private static final Identifier NORMAL=texture("normal"),RADIANT=texture("radiant");
    private static final RenderType EYES=RenderTypes.eyes(texture("normal_emissive")),GOLD_EYES=RenderTypes.eyes(texture("radiant_emissive"));
    public static final class State extends LivingEntityRenderState {
        public boolean radiant; public String clip="idle"; public float clipProgress=-1; public float hurtTime;
    }
    public StarDragonBossRenderer(EntityRendererProvider.Context ctx) {
        super(ctx,new StarDragonBossModel(ctx.bakeLayer(StarDragonBossModel.LAYER)),1.7f);
        // The radiant form gets its own emissive material in the same synchronized render state.
        addLayer(new EyesLayer<State,StarDragonBossModel>(this) {
            private boolean radiant;
            @Override public void submit(com.mojang.blaze3d.vertex.PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector,
                    int light, State state, float yRot, float xRot) {
                radiant=state.radiant; super.submit(pose,collector,light,state,yRot,xRot);
            }
            @Override public RenderType renderType() { return radiant?GOLD_EYES:EYES; }
        });
    }
    @Override public Identifier getTextureLocation(State s) { return s.radiant?RADIANT:NORMAL; }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(StarDragonBoss entity, State s, float partial) {
        super.extractRenderState(entity,s,partial);
        s.radiant=entity.radiant();s.clip=entity.animation();s.hurtTime=Math.max(0,entity.hurtTime-partial);
        s.clipProgress=((entity.level().getGameTime()-entity.animationStart())+partial)/entity.animationLength();
    }
    @Mod.EventBusSubscriber(modid=CircleOfInevitability.MOD_ID,value=Dist.CLIENT)
    public static final class Events {
        @SubscribeEvent public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) { e.registerLayerDefinition(StarDragonBossModel.LAYER,StarDragonBossModel::createLayer); }
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e) { e.registerEntityRenderer(StarDragonBossRegistry.BOSS.get(),StarDragonBossRenderer::new); }
    }
}
