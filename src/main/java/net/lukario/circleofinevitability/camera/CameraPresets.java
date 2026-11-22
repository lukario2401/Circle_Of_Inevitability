package net.lukario.circleofinevitability.camera;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CameraPresets extends SimpleJsonResourceReloadListener<CameraPreset> {
    static Map<Identifier, CameraPreset> loaded = Map.of();

    private CameraPresets() {
        super(CameraPreset.CODEC, FileToIdConverter.json("camera_presets"));
    }

    @SubscribeEvent
    public static void register(AddReloadListenerEvent event) {
        event.addListener(new CameraPresets());
    }

    @Override
    protected void apply(Map<Identifier, CameraPreset> values, ResourceManager resources, ProfilerFiller profiler) {
        loaded = Map.copyOf(values);
    }
}
