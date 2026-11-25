package net.lukario.circleofinevitability.client.mount;

import com.google.gson.JsonParser;
import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.client.model.CubeRig;
import net.lukario.circleofinevitability.mount.Mounts;
import net.lukario.circleofinevitability.mount.RidingMount;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.io.Reader;

@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, value = Dist.CLIENT)
public final class MountRenderer extends MobRenderer<RidingMount, MountRenderer.State, MountModel> {
    static final class State extends LivingEntityRenderState {
        boolean airborne;
    }

    private final Identifier texture;

    private MountRenderer(EntityRendererProvider.Context context, Mounts.Breed breed) {
        super(context, new MountModel(context.bakeLayer(layer(breed))), breed.type().get().getWidth() * 0.55F);
        texture = breed.id().withPath(path -> "textures/entity/mount/" + path + ".png");
    }

    @Override public Identifier getTextureLocation(State state) { return texture; }
    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(RidingMount mount, State state, float partialTick) {
        super.extractRenderState(mount, state, partialTick);
        state.airborne = !mount.onGround() && mount.isFlyingVehicle();
    }

    @Override
    protected AABB getBoundingBoxForCulling(RidingMount mount) {
        return super.getBoundingBoxForCulling(mount).inflate(2.5);
    }

    private static ModelLayerLocation layer(Mounts.Breed breed) {
        return new ModelLayerLocation(breed.id(), "main");
    }

    private static LayerDefinition load(Mounts.Breed breed) {
        Identifier asset = breed.id().withPath(path -> "mounts/" + path + ".rig.json");
        try (Reader reader = Minecraft.getInstance().getResourceManager().getResourceOrThrow(asset).openAsReader()) {
            return CubeRig.bake(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot load mount rig " + asset, ex);
        }
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        for (var breed : Mounts.breeds()) event.registerLayerDefinition(layer(breed), () -> load(breed));
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        for (var breed : Mounts.breeds()) {
            event.registerEntityRenderer(breed.type().get(), context -> new MountRenderer(context, breed));
        }
    }
}
