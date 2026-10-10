# Migration notes

## API renames found (old -> new)
| Old (1.21 / Forge 51) | New (26.2 / Forge 65) | Where fixed |
| --- | --- | --- |
| ResourceLocation | Identifier | all files, by script |
| GuiGraphics and its render methods | GuiGraphicsExtractor (class exists in 26.2: net.minecraft.client.gui.GuiGraphicsExtractor) and extract... methods | class name by script since stage 2; the method renames by hand in stage 4 |
| mod id "forgerealmmod" | "circle_of_inevitability" (MOD_ID value only; field name ForgeRealm.MOD_ID unchanged). Not applied to SoulCore's MOD_TAG, which is a persistent-data key (see Decisions) | all files, by script |
| net.minecraftforge.eventbus.api.SubscribeEvent | net.minecraftforge.eventbus.api.listener.SubscribeEvent | all files, by script |
| net.minecraftforge.eventbus.api.IEventBus | net.minecraftforge.eventbus.api.bus.BusGroup | by hand (stage 1: ForgeRealm) |
| FMLJavaModLoadingContext.get().getModEventBus() | constructor parameter FMLJavaModLoadingContext context, then context.getModBusGroup() (get() is deprecated forRemoval; getModEventBus() is gone) | by hand (stage 1: ForgeRealm) |
| modEventBus.addListener(this::commonSetup) | FMLCommonSetupEvent.getBus(busGroup).addListener(this::commonSetup) | by hand (stage 1: ForgeRealm) |
| ModLoadingContext.get().registerConfig(...) | context.registerConfig(...) (ModLoadingContext.get() is deprecated forRemoval) | by hand (stage 1: ForgeRealm) |
| DeferredRegister.register(IEventBus) | DeferredRegister.register(BusGroup) (confirmed in Forge 65 sources) | by hand (stage 2: ModEffects.register(BusGroup)) |
| net.minecraftforge.eventbus.api.EventPriority, EventPriority.HIGH | net.minecraftforge.eventbus.api.listener.Priority, Priority.HIGH (byte constants; @SubscribeEvent(priority = Priority.HIGH) still works) | by script |
| event.setCanceled(true) in a void listener | the listener returns boolean: true = cancel, false = let it through (only allowed for Cancellable events; a cancelled event stops reaching later listeners, as before) | by hand (stage 2: Root, FallGuard, Blind, Concealment, Marionette, Substitute, DeathWard) |
| TickEvent.PlayerTickEvent + if (event.phase != TickEvent.Phase.END) return; | TickEvent.PlayerTickEvent.Post (sealed Pre/Post records; no phase field) | by script (only END is used by kept code; Phase.START appears only in the dropped ShadowSlaveAspect) |
| event.player (PlayerTickEvent field) | event.player() (record accessor) | by script |
| TickEvent.ClientTickEvent (fired for START and END) | TickEvent.ClientTickEvent.Pre / .Post (also Server/Level/RenderTickEvent) | stage 4 (ClientForgeHandler) |
| MobEffects.MOVEMENT_SPEED, MOVEMENT_SLOWDOWN, DIG_SPEED, DIG_SLOWDOWN, DAMAGE_BOOST, HEAL, HARM, JUMP, CONFUSION, DAMAGE_RESISTANCE | SPEED, SLOWNESS, HASTE, MINING_FATIGUE, STRENGTH, INSTANT_HEALTH, INSTANT_DAMAGE, JUMP_BOOST, NAUSEA, RESISTANCE | by script |
| CompoundTag.getInt/getLong/getFloat/getDouble/getBoolean/getString(key) (0/false/"" when missing) | getIntOr(key, 0), getLongOr(key, 0L), getFloatOr(key, 0f), getDoubleOr(key, 0.0), getBooleanOr(key, false), getStringOr(key, "") (the plain getters now return Optional) | by script (single-argument calls only; lines with ...ArgumentType. are skipped) |
| CompoundTag.getCompound(key) (new empty tag when missing) | getCompoundOrEmpty(key) (same: a new, unattached empty tag when missing) | by script |
| CompoundTag.getAllKeys() | keySet() | by script |
| CompoundTag.putUUID / hasUUID / getUUID(key) | store(key, UUIDUtil.CODEC, uuid) / read(key, UUIDUtil.CODEC).isPresent() / read(key, UUIDUtil.CODEC).orElseThrow() (same int[4] NBT format; script adds the UUIDUtil import) | by script |
| level.isClientSide (public field) | level.isClientSide() (field is private now) | by script |
| player.displayClientMessage(msg, true / false) | player.sendOverlayMessage(msg) / player.sendSystemMessage(msg) | by script for one-line calls; by hand when the call spans lines (stage 2: SoulHandler) |
| .requires(source -> source.hasPermission(2)) | .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)) (0..4 = LEVEL_ALL, LEVEL_MODERATORS, LEVEL_GAMEMASTERS, LEVEL_ADMINS, LEVEL_OWNERS) | by script |
| entity.kill() | entity.kill(ServerLevel) | by hand (stage 2: Marionette, SoulHandler) |
| livingEntity.isInvulnerableTo(source) | isInvulnerableTo(ServerLevel, source) | by hand (stage 2: Substitute) |
| Event inheritance (a listener for a parent event also got its subclasses) | only for events that implement InheritableEvent (EntityTeleportEvent, PlayerInteractEvent, LivingEntityUseItemEvent do; BlockEvent.EntityPlaceEvent does NOT, so EntityMultiPlaceEvent needs its own listener) | by hand (stage 2: Root) |
| @Mod.EventBusSubscriber default bus FORGE | default bus is now BOTH (mod-bus events go to the mod bus, the rest to the game bus). With strict checks (on in our runs) registration throws if the class has no @SubscribeEvent method, a method taking an event lacks @SubscribeEvent, a listener is not static, or a mod-bus event sits in a bus = FORGE class | check every subscriber class |
| TickEvent.LevelTickEvent + if (event.phase != TickEvent.Phase.END ...) | TickEvent.LevelTickEvent.Post (sealed Pre/Post records, like PlayerTickEvent) | by script (only END is used by kept code) |
| event.level (LevelTickEvent field) | event.level() (record accessor) | by script (not when the text before is a dot, so the package net.minecraftforge.event.level is left alone) |
| level.random (public field) | level.getRandom() (the field is protected now) | by script |
| player.serverLevel() | player.level() (ServerPlayer.level() returns ServerLevel now) | by script |
| net.minecraft.world.entity.MobSpawnType, MobSpawnType.X | net.minecraft.world.entity.EntitySpawnReason, EntitySpawnReason.X (same constant names) | by script |
| ResourceLocationArgument (id(), getId(ctx, name)) | IdentifierArgument (same methods) | by script |
| FriendlyByteBuf.readResourceLocation() / writeResourceLocation(id) | readIdentifier() / writeIdentifier(id) | by script |
| BuiltInRegistries.X.get(Identifier) (the value; default value for a defaulted registry) | getValue(Identifier) (get now returns Optional<Holder.Reference>) | by hand (stage 3: SpawnFieldCommand) |
| EntityType.create(Level) | create(Level, EntitySpawnReason) | by hand (stage 3: SpawnFieldCommand, EntitySpawnReason.COMMAND as vanilla /summon uses) |
| Entity.moveTo(x, y, z, yRot, xRot) | snapTo(x, y, z, yRot, xRot) | by hand (stage 3: SpawnFieldCommand) |
| OwnableEntity.getOwnerUUID() (null when no owner) | getOwnerReference() (an EntityReference, null when no owner) .getUUID() | by hand (stage 3: Spells.isEnemy) |
| SimpleChannel.messageBuilder(Class, NetworkDirection) with encoder(FriendlyByteBuf) / decoder(FriendlyByteBuf) | unchanged and compiling. NetworkDirection is generic now (PLAY_TO_X is NetworkDirection<RegistryFriendlyByteBuf>); the old encode/decode methods taking FriendlyByteBuf still fit as method references. The method is deprecated (javadoc: "Use SimpleConnection") but not forRemoval | no change (stage 3: PacketHandler) |

Unchanged and still compiling: MinecraftForge.EVENT_BUS.register(Object) (now an EventBusMigrationHelper), @Mod.EventBusSubscriber(modid, bus = Bus.MOD, value = Dist.CLIENT), ForgeConfigSpec, ModConfigEvent, ForgeRegistries.ITEMS.containsKey/getValue. Stage 3 adds: ChannelBuilder.named(Identifier).serverAcceptedVersions/clientAcceptedVersions((status, version) -> ...).networkProtocolVersion(int).simpleChannel(), PacketDistributor.SERVER.noArg() / PLAYER.with(player) / ALL.noArg(), Channel.send(msg, target), CustomPayloadEvent.Context.getSender(), FMLEnvironment.dist.isClient(), RegisterCommandsEvent.getDispatcher() (record accessor of the same name), ForgeRegistries.ENTITY_TYPES.getEntries(), EntityType.spawn(ServerLevel, BlockPos, reason), LevelEvent.Unload, AttackEntityEvent, Entity.hurt(DamageSource, float) (deprecated; on the server it calls hurtServer, on the client it does nothing; every caller here is server side).

Rename script: scripts/renames.sh [files] (no arguments = every .java file under src/). It applies every "by script" row above with sed, is idempotent (a second run changes nothing; checked again in stage 3 by running it twice over all of src/ and diffing) and refuses any path under reference/ (also ../reference). Run it right after copying files in each stage; add new by-script rows to both the table and the script. It includes all rules of tools/apply_renames.sh, which is kept unchanged but superseded: do not use tools/apply_renames.sh any more, its blanket forgerealmmod rule would also rename SoulCore's MOD_TAG data key.

## Decisions
- Stage 1: mod id is circle_of_inevitability, not forgerealmmod (user instruction; CLAUDE.md updated to match). Used in @Mod (ForgeRealm.MOD_ID), mods.toml, gradle.properties, build.gradle run config and the assets folder.
- Stage 1: MDK example package net.lukario.circleofinevitability (ExampleMod.java, Config.java) deleted; replaced by net.lukario.frogerealm.ForgeRealm and Config copied from reference.
- Stage 1: cp --no-preserve=mode was denied by the session permission layer; with the user's permission the two files were copied byte-for-byte with cat > (verified identical with cmp before editing).
- Stage 1: later-stage registrations are commented out with stage-specific tags rather than TODO(stage5): ModEffects -> TODO(stage2), ModParticles -> TODO(stage4). grep "TODO(stage" to find them.
- Stage 1: the addCreative method and its listener were removed entirely (its only job was adding ModItems.COIN to a creative tab; no items or tabs in this port).
- Stage 1: MinecraftForge.EVENT_BUS.register(this) kept as in the old code (still exists in Forge 65 and compiles under the eventbus validator).
- Stage 1: build.gradle group -> net.lukario.frogerealm; gameTest namespace and data run --mod -> circle_of_inevitability; settings.gradle rootProject.name -> circle_of_inevitability (jar is now circle_of_inevitability-1.0.0.jar).
- Stage 1: mods.toml minecraft dependency range fixed from the malformed "[26.2,26.3,)" to "[26.2,26.3)". The forge block already read modId="forge", versionRange="[65,)". The [[dependencies.circle_of_inevitability]] headers are correct: they name the mod that has the dependencies, not a dependency on itself.

- Stage 2: "root package classes" read as the root/ package (Freeze, Root, RootRestriction: 3 classes), matching the CLAUDE.md stage list; the top-level ForgeRealm and Config were already ported in stage 1. In scope: 23 files (root 3, status 11, effects 1, handler 2, events 1, shadow_slave/soul_shards 5).
- Stage 2: cp --no-preserve=mode worked this session; all 23 files were byte-identical to reference/ (cmp) before scripts/renames.sh ran.
- Stage 2: SoulCore.MOD_TAG keeps the value "forgerealmmod". It is the key of the per-player data compound in getPersistentData(), not the mod id, and CLAUDE.md says to keep the same keys. scripts/renames.sh skips lines with "MOD_TAG =" for the mod-id rule. (The @Mod.EventBusSubscriber(modid = ...) strings in the same files are renamed.)
- Stage 2: out-of-scope dependencies are disabled line by line: the original line stays in place behind a "// TODO(stageN): " prefix, so a later stage only uncomments it. The rest of the class keeps working now (timers, damage, roots, targeting). Whole-file placeholders only where the whole file is out-of-scope wiring (the two handler classes) and for Flight (as asked). grep -rn "TODO(stage" src lists all of them.
- Stage 2: cancelling listeners were converted mechanically: void -> boolean, early "return;" -> "return false;", "if (X) event.setCanceled(true);" at the end -> "return X;". Where work followed setCanceled (FallGuard, Substitute, DeathWard) the work stays in the same order and "return true" moved after it; nothing else in those methods changed.
- Stage 2: Root got one new listener, onRootMultiPlaceBlock(BlockEvent.EntityMultiPlaceEvent), with the same body as onRootPlaceBlock. In Forge 51 the EntityPlaceEvent listener also caught multi-block placements (beds, doors) by inheritance; Forge 65 posts those only to EntityMultiPlaceEvent.BUS. This keeps the old behaviour (a BLOCKS-rooted player cannot place beds/doors); it is not a new feature.
- Stage 2: Substitute.isRealHit keeps its public signature (LivingEntity, DamageSource, float). The invulnerability check now needs a ServerLevel, so it is only made on the server; on the client that one check is skipped. All callers in the old code call it from the server side.
- Stage 2: SoulHandler: entity.kill() -> entity.kill((ServerLevel) entity.level()); the cast is safe because the method returns early on the client.
- Stage 2: the event-subscriber registration was verified outside the game: a throwaway harness (scratchpad only, not committed) loaded every compiled class and ran Forge 65's own AutomaticEventSubscriber$EventBusSubscriberLogic.registerStrict on each @Mod.EventBusSubscriber class with -Deventbus.api.strictRuntimeChecks=true, using the bus read from the class file (mod-bus classes against a fresh BusGroup). All 19 classes registered (including stage 1's Config and ForgeRealm$ClientModEvents), their static initialisers ran, and no class has @SubscribeEvent methods without the annotation. ForgeRealm's own constructor and DeferredRegister registration could not be run outside FML; they were checked by reading the code.
- Stage 2: also checked in the Forge 65 sources that every event the stage-2 classes listen to is still posted (LivingTick/Attack/Hurt/Damage/Death/Fall/ChangeTarget, AttackEntity, RightClickItem/Block, LeftClickBlock, UseItem Start, Break/EntityPlace/EntityMultiPlace, the EntityTeleportEvent subclasses, XpChange, PlayerLoggedOut, Clone, RegisterCommands, PlayerTickEvent.Post).

- Stage 3: in scope 24 files: combat 10, network 11 (18 minus the seven SKeyPressAbility...Used packets, which are stage 5), item/custom/commands 2, item/custom/other 1. cp --no-preserve=mode worked; all 24 were byte-identical to reference/ (cmp) before scripts/renames.sh ran.
- Stage 3, networking: Forge 65 still has the Forge 51 API this mod uses (ChannelBuilder -> SimpleChannel, messageBuilder(Class, NetworkDirection).encoder/decoder/consumerMainThread/add(), send(msg, PacketDistributor...)). No packet needed a change for the channel API; PacketHandler only got the Identifier rename. The newer SimpleConnection/protocol(...).flow(...).add(type, StreamCodec, handler) API was not adopted: it would mean a StreamCodec per packet, a redesign the old behaviour does not need. messageBuilder is deprecated (javadoc only, no forRemoval), so the build prints a deprecation note; move to SimpleConnection only if a later Forge removes it. Checked outside the game (scratchpad harness, not committed): PacketHandler loads, the channel registers, register() runs with no exception, and every in-scope packet round-trips (encode, decode, encode gives the same bytes). Calling register() twice throws ("already registered"), so it must stay called from one place only (CommonModEvents).
- Stage 3, networking: message ids are handed out in registration order (nextIndex). With the seven stage-5 packets and two stage-4 packets commented out, ids differ from the old mod's; harmless because client and server always run the same jar. When the commented registrations come back they return to the old order.
- Stage 3: the TODO(stage3) lines that stage 2 left in events/CommonModEvents (PacketHandler.register()), shadow_slave/soul_shards/AspectSync (CSyncAspectPacket) and status/Concealment (two CConcealPacket sends) were uncommented, nothing else in those files changed; they now match reference/ apart from the earlier renames. This touches three files outside the stage-3 folder list: without the CommonModEvents line no packet is registered and the first Countdown/HudTimer/AspectSync send would fail at runtime. (The stage-3 instructions allowed ForgeRealm.java for a registration line, but the packet registration line lives in CommonModEvents.)
- Stage 3: combat/AnimatedShot, combat/Shot and combat/SpellFx deferred to stage 4 as empty placeholders (their public API is ModelFx/SlashFx/ParticleFx types, and AnimatedShot's path and hit detection follow ModelFx.poseAt). Nothing in src uses them yet; only the seven classes do.
- Stage 3: network/CScreenImagePacket (needs screen.ScreenAnchor in its fields and wire format) and network/STextPromptAnswerPacket (needs menu.AbilityTextPrompt.MAX_LENGTH for its wire size limits) deferred to stage 4 as empty placeholders; their registrations in PacketHandler are commented out with TODO(stage4). Their only senders are stage-4 classes.
- Stage 3: the other client-bound packets keep their full wire format; only the one call into a stage-4 client/menu class in handle() is commented out, so they register, travel and are received, and do nothing on arrival until stage 4.
- Stage 3: SpawnFieldCommand create(level) -> create(level, EntitySpawnReason.COMMAND) (the reason vanilla /summon uses).
- Stage 3: event subscribers checked with the stage-2 method (Forge 65's own EventBusSubscriberLogic.registerStrict, -Deventbus.api.strictRuntimeChecks=true and strictRegistrationChecks=true, bus read from the class file with ASM): all 62 compiled classes load and run their static initialisers; all 25 @Mod.EventBusSubscriber classes register, including the 6 new ones (Countdown, Later, MeleeCombos, Zone, ModCommands, SneakLaunchHandler). All new listeners are static and annotated; no class in scope uses MinecraftForge.EVENT_BUS.register(this).

## Removed (references to dropped classes and items)
- ForgeRealm: import and call CreativeModTabs.register(modEventBus) (item/creative tab).
- ForgeRealm: import and call ModItems.register(modEventBus) (items).
- ForgeRealm: unused import net.lukario.frogerealm.item.seald_artifacts.IronCoif (item).
- ForgeRealm: addCreative(...) method, its modEventBus.addListener(this::addCreative) registration, and the now-unused CreativeModeTabs / BuildCreativeModeTabContentsEvent imports (it only accepted ModItems.COIN).
- ModEffects (stage 2): all five registered effects, because each effect class is an inner class of a dropped aspect: SHADOW_MARK "shadow_mark" (ShadowSlaveAspect.ShadowMarkEffect), Abyssal_Monarch_MARK "abyssal_monarch_mark" (AbyssalMonarch.Abyssal_Monarch_Mark), RuinBade_Ruin "ruin_blade_ruin", RuinBade_Overload "ruin_blade_overload", RuinBade_cataclysm "ruin_blade_cataclysm" (RuinBladeAscendant.Ruinblade_*_Effect). Also their three aspect imports and the then-unused RegistryObject import. Only the dropped aspects used these effects. MOB_EFFECTS stays (empty) and is registered, so later effects have a place to go. The effect textures/lang entries for these ids are stage 6 cleanup.

## Stubs (disabled until a later stage; grep "TODO(stage")
- TODO(stage3) network: none left. Stage 3 re-enabled CommonModEvents' PacketHandler.register(), AspectSync's CSyncAspectPacket send and Concealment's two CConcealPacket sends.
- TODO(stage4) client / particles:
  - combat/AnimatedShot, combat/Shot, combat/SpellFx: empty placeholders (need particles.fx). Re-copy from reference/ and run scripts/renames.sh.
  - network/CScreenImagePacket, network/STextPromptAnswerPacket: empty placeholders (need screen.ScreenAnchor / menu.AbilityTextPrompt); their PacketHandler registrations are commented TODO(stage4).
  - network handle() bodies: CConcealPacket (client.ClientConcealment.set), CHudTimerPacket (client.HudTimerOverlay.show), COpenAbilityMenuPacket (client.AbilityMenuScreen.open), COpenTextPromptPacket (client.TextPromptScreen.open), CShowHudOverlayPacket (client.AbilityHudOverlay.showNumber), CSyncAspectPacket (client.ClientAspectData.setAspect), SAbilityMenuChoicePacket (menu.AbilityMenu.handleChoice). The packets themselves are complete.
  - handler/ClientForgeHandler: placeholder class (no annotation, no listener). Real one polls client.Keybindings and sends the SKeyPress... packets.
  - handler/ClientModHandler: placeholder class (no annotation, no listener). Real one registers the Keybindings in RegisterKeyMappingsEvent.
  - root/Freeze: ICE_SHELL, ICE_CRYSTALS, FROST, ICE_SHARD, the ModelFx part of spawnIce, ParticleShapes calls in end/frostBurst/shatter. Sounds still play.
  - root/Root: ROOT_MARK and the ParticleShapes.ring call.
  - status/Concealment: FOG and the ParticleShapes.burst call.
  - status/DamageLink: PASSED and the ParticleShapes.burst call.
  - status/Marionette: CONTROL_CROSS, STRING_SNAP, ParticleShapes.clearModels/modelOn/burst. hangStrings and its sounds still run.
- TODO(stage5):
  - combat/MeleeCombos: the five register(X.MELEE_COMBO) lines and their imports (HandOfOrder, AttendantOfMysteries, KeyOfStars, PrinceOfAbolition, PaleEmperor). The listener stays registered and finds no combo, so melee hits do nothing extra until stage 5.
  - network/PacketHandler: the seven SKeyPressAbility{One..Seven}Used registrations (the packets are not copied yet).
  - status/Flight: placeholder with the same public API (FALL_GUARD_TICKS, grant, has, ticksLeft, end) that does nothing (has() is always false). Its only caller is PaleEmperor.

## Still broken or to check in game
- Stage 1, not verified at runtime: runServer stops at the EULA check (eula.txt not accepted in this VM) before mods are constructed. Check in ./gradlew runClient:
  - the game reaches the title screen with no mod loading error;
  - Mods list shows modId circle_of_inevitability;
  - run/config/circle_of_inevitability-common.toml is created with logDirtBlock, magicNumber, magicNumberIntroduction, items;
  - the log has no eventbus strictRuntimeChecks error about ForgeRealm.onServerStarting (instance @SubscribeEvent registered via MinecraftForge.EVENT_BUS.register(this)). If it does, the smallest fix is ServerStartingEvent.BUS.addListener(this::onServerStarting).
- Stage 2: done: ModEffects.register(modEventBus) is re-enabled in ForgeRealm.
- Stage 2, not verified at runtime (the game cannot be launched here). Check in ./gradlew runClient:
  - the game still reaches the title screen; the log has no "Failed to register" / "Missing @SubscribeEvent" / "No listeners found" eventbus error;
  - /soul get <you> works for an op and prints stage, aspect, tier, corruption, shards, essence; /soul setshards, addshards, addSoulEssence, settier, corruption, setstage, setAspect change them;
  - gaining XP prints "You have: N" in chat; "Soul Essence: X" shows above the hotbar every tick and rises;
  - /soul corruption <you> 80, then wait: slowness and occasional 1 damage; at 100 you die about a second later; values survive death (PlayerEvent.Clone);
  - after reload, a world created in stage 1 still loads.
- Stage 2, behaviour that is missing until a later stage (expected, not a bug):
  - no keybindings registered and ability keys do nothing (stage 4: ClientModHandler/ClientForgeHandler);
  - Concealment only gives vanilla invisibility: other players still see the hidden entity's armour, held items and name, because the CConcealPacket handler is stubbed (the packet is sent since stage 3; client.ClientConcealment comes in stage 4);
  - the client receives the player's aspect since stage 3 but ignores it until client.ClientAspectData exists (stage 4);
  - Freeze/Root/Concealment/DamageLink/Marionette have no particles or models (stage 4);
  - Flight does nothing (stage 5).
- Stage 2, runtime-only risks to keep an eye on:
  - EntityTeleportEvent: Root listens on the parent class and relies on InheritableEvent to get EnderPearl/ChorusFruit/EnderEntity events posted to their own buses. Not testable until abilities can root someone (stage 5), or with a test command.
  - Root's new EntityMultiPlaceEvent listener: same as above (a BLOCKS-rooted player should not be able to place a bed or door).
  - Stage 3+ copies of files that use PlayerTickEvent with Phase.START would be mis-renamed to .Post by the script; none exist in the kept code (only the dropped ShadowSlaveAspect).
- Stage 3, not verified at runtime (the game cannot be launched here; mod construction, common setup and packet registration were checked by reading the code and by the outside-the-game harness above). Check in ./gradlew runClient:
  - the game still reaches the title screen; the log has no eventbus "Failed to register" error and no "Attempted to register channel" / "already registered" / "Failed to register SimpleChannel message" error;
  - joining a single-player world works (the channel circle_of_inevitability:main passes the version check) and the log shows no "Unknown message" / decoder error;
  - /spawnfield appears for an op (tab completion: /spawnfield <entity> <radius> <density> <maxCount>);
  - double jump: jump, then jump again in the air (survival): a push up with a firework sound and particles; then sneak in the air: a dash forward. Not in creative or while flying;
  - /soul setAspect <you> <aspect> and back: no error in the log (that is the CSyncAspectPacket being sent; the client ignores it until stage 4).
- Stage 3, deliberate fix of an old bug (Luka's decision): SpawnFieldCommand cast level.getRandom() (a RandomSource) to java.util.Random, so /spawnfield always threw a ClassCastException in the old code too; it now uses the RandomSource directly (var random = level.getRandom(); nextDouble/nextFloat are the same calls). Check: /spawnfield minecraft:pig 5 0.5 10 spawns up to 10 pigs with no AI around you and prints "Spawned N minecraft:pig (density=0.5)".
- Stage 3, vanilla behaviour change: EntityType.create(level, reason) and spawn(level, pos, reason) now refuse hostile mobs on Peaceful (canSpawn check); the old create(level) only checked enabled features. Affects SpawnFieldCommand and SKeyPressSpawnEntityPacket (whose key is wired up in stage 4): on Peaceful a hostile pick spawns nothing.
- Stage 3, runtime-only risks:
  - PacketHandler.register() must run exactly once (a second call throws). It runs in CommonModEvents' FMLCommonSetupEvent enqueueWork, as before.
  - Packets sent from tick handlers (AspectSync on every player tick when the aspect changed, Concealment every REFRESH ticks) go through PacketDistributor.PLAYER / ALL; a client without the mod is accepted by the version check ((status, version) -> true, as in the old code) and would get an unknown payload. Same as before.
- Stage 4: re-enable ModParticles.PARTICLES.register(modEventBus) in ForgeRealm (TODO(stage4)).
- Stage 6: mods.toml displayName="Example Mod", authors, description and #logoFile="examplemod.png" are still MDK placeholders.
- Stage 6: pack.mcmeta description uses ${mod_id} but build.gradle has no processResources expand, so it shows literally.

## Stage log
| Stage | Date | Credit used (Settings > Usage) | Lines ported | Commit |
| --- | --- | --- | --- | --- |
| 1 scaffold | 2026-10-09 | (fill in) | 143 (ForgeRealm 80, Config 63) | Stage 1: Scaffold |
| 2 foundation | 2026-10-10 | (Luka fills in) | 2813 in 23 files (root 534, status 1633, effects 20, handler 17, events 22, soul_shards 587; reference had 2975) | Stage 2: Foundation |
| 3 combat, network, helpers | 2026-10-10 | (Luka fills in) | 2120 in 24 files (combat 1345, network 493, commands 149, other 133; reference had 2981 for the same 24, the gap is the five stage-4 placeholders) | Stage 3: Combat, network, helpers |
