package net.lukario.circleofinevitability.item;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SequenceCommands;
import net.lukario.circleofinevitability.item.artifacts.SealedArtifactItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** Characteristics and sealed artifacts, each in their own creative tab. */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CircleOfInevitability.MOD_ID);

    public static final RegistryObject<CreativeModeTab> CHARACTERISTICS = CREATIVE_TABS.register("characteristics",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.circle_of_inevitability.characteristics"))
                    .icon(() -> BeyonderCharacteristicItem.of(SequenceCommands.PATHWAYS.get(0), Beyonder.WEAKEST_SEQUENCE))
                    .displayItems((parameters, output) -> {
                        // one row of ten per Pathway (a creative row is nine wide, so it wraps; the order still reads 9 -> 0)
                        for (String pathway : SequenceCommands.PATHWAYS) {
                            for (int sequence = Beyonder.WEAKEST_SEQUENCE; sequence >= Beyonder.STRONGEST_SEQUENCE; sequence--) {
                                output.accept(BeyonderCharacteristicItem.of(pathway, sequence));
                            }
                        }
                    })
                    .build());

    public static final RegistryObject<CreativeModeTab> SEALED_ARTIFACTS = CREATIVE_TABS.register("sealed_artifacts",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.circle_of_inevitability.sealed_artifacts"))
                    .icon(() -> new ItemStack(ModItems.SUN_BROOCH.get()))
                    .displayItems((parameters, output) -> {
                        for (var entry : ModItems.ITEMS.getEntries())
                            if (entry.get() instanceof SealedArtifactItem)
                                output.accept(entry.get());
                    })
                    .build());

    private ModCreativeTabs() {}

    public static void register(BusGroup eventBus) {
        CREATIVE_TABS.register(eventBus);
    }
}
