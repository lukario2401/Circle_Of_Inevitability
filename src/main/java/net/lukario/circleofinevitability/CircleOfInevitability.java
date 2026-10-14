package net.lukario.circleofinevitability;

import com.mojang.logging.LogUtils;
import net.lukario.circleofinevitability.effects.ModEffects;
import net.lukario.circleofinevitability.particles.ModParticles;
import net.lukario.circleofinevitability.sounds.ModSounds;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(CircleOfInevitability.MOD_ID)
public class CircleOfInevitability
{
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "circle_of_inevitability";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();


    public CircleOfInevitability(FMLJavaModLoadingContext context)
    {
        BusGroup modEventBus = context.getModBusGroup();

        // Register the commonSetup method for modloading
        FMLCommonSetupEvent.getBus(modEventBus).addListener(this::commonSetup);
        // Register ourselves for server and other game events we are interested in
        ServerStartingEvent.BUS.addListener(this::onServerStarting);

        ModParticles.PARTICLES.register(modEventBus);
        ModSounds.SOUNDS.register(modEventBus);
        ModEffects.register(modEventBus);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event){

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event){

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents{
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {

        }
    }
}
