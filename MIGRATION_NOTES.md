# Migration notes

## API renames found (old -> new)
| Old (1.21 / Forge 51) | New (26.2 / Forge 65) | Where fixed |
| --- | --- | --- |
| ResourceLocation | Identifier | all files, by script |
| GuiGraphics and its render methods | GuiGraphicsExtractor and extract... methods (reported for NeoForge 26.1; verify for Forge) | stage 4 |
| mod id "forgerealmmod" | "circle_of_inevitability" (MOD_ID value only; field name ForgeRealm.MOD_ID unchanged) | all files, by script |
| net.minecraftforge.eventbus.api.SubscribeEvent | net.minecraftforge.eventbus.api.listener.SubscribeEvent | all files, by script |
| net.minecraftforge.eventbus.api.IEventBus | net.minecraftforge.eventbus.api.bus.BusGroup | by hand (stage 1: ForgeRealm) |
| FMLJavaModLoadingContext.get().getModEventBus() | constructor parameter FMLJavaModLoadingContext context, then context.getModBusGroup() (get() is deprecated forRemoval; getModEventBus() is gone) | by hand (stage 1: ForgeRealm) |
| modEventBus.addListener(this::commonSetup) | FMLCommonSetupEvent.getBus(busGroup).addListener(this::commonSetup) | by hand (stage 1: ForgeRealm) |
| ModLoadingContext.get().registerConfig(...) | context.registerConfig(...) (ModLoadingContext.get() is deprecated forRemoval) | by hand (stage 1: ForgeRealm) |
| DeferredRegister.register(IEventBus) | DeferredRegister.register(BusGroup) (expected; the stubbed calls in ForgeRealm pass the BusGroup) | stages 2 and 4 |

Unchanged and still compiling: MinecraftForge.EVENT_BUS.register(Object) (now an EventBusMigrationHelper), @Mod.EventBusSubscriber(modid, bus = Bus.MOD, value = Dist.CLIENT), ForgeConfigSpec, ModConfigEvent, ForgeRegistries.ITEMS.containsKey/getValue.

Rename script: tools/apply_renames.sh <files>. It applies every "by script" row above with sed and refuses to touch reference/. Run it right after copying files in each stage; add new by-script rows to both the table and the script.

## Decisions
- Stage 1: mod id is circle_of_inevitability, not forgerealmmod (user instruction; CLAUDE.md updated to match). Used in @Mod (ForgeRealm.MOD_ID), mods.toml, gradle.properties, build.gradle run config and the assets folder.
- Stage 1: MDK example package net.lukario.circleofinevitability (ExampleMod.java, Config.java) deleted; replaced by net.lukario.frogerealm.ForgeRealm and Config copied from reference.
- Stage 1: cp --no-preserve=mode was denied by the session permission layer; with the user's permission the two files were copied byte-for-byte with cat > (verified identical with cmp before editing).
- Stage 1: later-stage registrations are commented out with stage-specific tags rather than TODO(stage5): ModEffects -> TODO(stage2), ModParticles -> TODO(stage4). grep "TODO(stage" to find them.
- Stage 1: the addCreative method and its listener were removed entirely (its only job was adding ModItems.COIN to a creative tab; no items or tabs in this port).
- Stage 1: MinecraftForge.EVENT_BUS.register(this) kept as in the old code (still exists in Forge 65 and compiles under the eventbus validator).
- Stage 1: build.gradle group -> net.lukario.frogerealm; gameTest namespace and data run --mod -> circle_of_inevitability; settings.gradle rootProject.name -> circle_of_inevitability (jar is now circle_of_inevitability-1.0.0.jar).
- Stage 1: mods.toml minecraft dependency range fixed from the malformed "[26.2,26.3,)" to "[26.2,26.3)". The forge block already read modId="forge", versionRange="[65,)". The [[dependencies.circle_of_inevitability]] headers are correct: they name the mod that has the dependencies, not a dependency on itself.

## Removed (references to dropped classes and items)
- ForgeRealm: import and call CreativeModTabs.register(modEventBus) (item/creative tab).
- ForgeRealm: import and call ModItems.register(modEventBus) (items).
- ForgeRealm: unused import net.lukario.frogerealm.item.seald_artifacts.IronCoif (item).
- ForgeRealm: addCreative(...) method, its modEventBus.addListener(this::addCreative) registration, and the now-unused CreativeModeTabs / BuildCreativeModeTabContentsEvent imports (it only accepted ModItems.COIN).

## Still broken or to check in game
- Stage 1, not verified at runtime: runServer stops at the EULA check (eula.txt not accepted in this VM) before mods are constructed. Check in ./gradlew runClient:
  - the game reaches the title screen with no mod loading error;
  - Mods list shows modId circle_of_inevitability;
  - run/config/circle_of_inevitability-common.toml is created with logDirtBlock, magicNumber, magicNumberIntroduction, items;
  - the log has no eventbus strictRuntimeChecks error about ForgeRealm.onServerStarting (instance @SubscribeEvent registered via MinecraftForge.EVENT_BUS.register(this)). If it does, the smallest fix is ServerStartingEvent.BUS.addListener(this::onServerStarting).
- Stage 2: re-enable ModEffects.register(modEventBus) in ForgeRealm (TODO(stage2)).
- Stage 4: re-enable ModParticles.PARTICLES.register(modEventBus) in ForgeRealm (TODO(stage4)).
- Stage 6: mods.toml displayName="Example Mod", authors, description and #logoFile="examplemod.png" are still MDK placeholders.
- Stage 6: pack.mcmeta description uses ${mod_id} but build.gradle has no processResources expand, so it shows literally.

## Stage log
| Stage | Date | Credit used (Settings > Usage) | Lines ported | Commit |
| --- | --- | --- | --- | --- |
| 1 scaffold | 2026-10-09 | (fill in) | 143 (ForgeRealm 80, Config 63) | Stage 1: Scaffold |
