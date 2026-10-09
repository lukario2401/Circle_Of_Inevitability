package net.lukario.frogerealm.item;

import net.lukario.frogerealm.ForgeRealm;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class CreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ForgeRealm.MOD_ID);

    public static final RegistryObject<CreativeModeTab> SOULTAB = CREATIVE_MODE_TAB.register("soul_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.SOULCOIN.get()))
                    .title(Component.translatable("creativetab.forgerealmmod.soul_tab"))
                    .displayItems((itemDisplayParameters, output) -> {

                        output.accept(ModItems.SOULESSENCE.get());
                        output.accept(ModItems.SOULCOIN.get());
                        output.accept(ModItems.SHADOW_SWORD.get());
                        output.accept(ModItems.ENERGY_SHARD.get());
                        output.accept(ModItems.DEPLETED_ENERGY_SHARD.get());
                        output.accept(ModItems.LASER_STAFF.get());
                        output.accept(ModItems.TERMINATOR.get());

                        output.accept(ModItems.TAROTDECK.get());

                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> SEALED_ARTIFACTS = CREATIVE_MODE_TAB.register("sealed_artifacts",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.HOLLOW_NAME.get()))
                    .withTabsBefore(SOULTAB.getId())
                    .title(Component.translatable("creativetab.forgerealmmod.sealed_artifacts"))
                    .displayItems((itemDisplayParameters, output) -> {

                        output.accept(ModItems.BONE_DEFINERS_LENS.get());
                        output.accept(ModItems.GRAVEWARDEN_SHROUD.get());
                        output.accept(ModItems.HOLLOW_NAME.get());
                        output.accept(ModItems.FRACTURED_CHORUS.get());
                        output.accept(ModItems.UNFINISHED_HOUR_GLASS.get());
                        output.accept(ModItems.UNDYING_CANKER.get());
                        output.accept(ModItems.MIRRORLESS_EYE.get());
                        output.accept(ModItems.SUNKEN_CODEX.get());
                        output.accept(ModItems.CRYSTALLIZED_PROPHECY_MONOCLE.get());
                        output.accept(ModItems.SCARLET_SUTURE.get());
                        output.accept(ModItems.HANGED_MAN_CHORD.get());
                        output.accept(ModItems.BONE_FETISH.get());
                        output.accept(ModItems.CRACKED_BAROMETER.get());
                        output.accept(ModItems.SPLICED_COMPASS.get());
                        output.accept(ModItems.LAST_BREATH.get());
                        output.accept(ModItems.COLLAPSAR_BEAD.get());
                        output.accept(ModItems.IRON_COIF.get());

                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> EXTRASTAB = CREATIVE_MODE_TAB.register("extras_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.COIN.get()))
                    .withTabsBefore(SOULTAB.getId())
                    .title(Component.translatable("creativetab.forgerealmmod.extras_tab"))
                    .displayItems((itemDisplayParameters, output) -> {

                        output.accept(ModItems.COIN.get());
                        output.accept(ModItems.DIAMOND_CARROT.get());
                        output.accept(ModItems.STRENGTH_ELIXIR.get());
                        output.accept(ModItems.SHEPARDS_BLADE.get());

                    })
                    .build());

    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
