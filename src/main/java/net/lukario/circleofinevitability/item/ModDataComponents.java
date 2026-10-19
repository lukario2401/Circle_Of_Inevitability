package net.lukario.circleofinevitability.item;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** The mod's own item data components (the data a stack carries; the old "metadata"). */
public final class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CircleOfInevitability.MOD_ID);

    /** The Pathway and Sequence of a Beyonder Characteristic. Saved with the stack and sent to clients. */
    public static final RegistryObject<DataComponentType<BeyonderCharacteristic>> CHARACTERISTIC =
            DATA_COMPONENTS.register("characteristic", () -> DataComponentType.<BeyonderCharacteristic>builder()
                    .persistent(BeyonderCharacteristic.CODEC)
                    .networkSynchronized(BeyonderCharacteristic.STREAM_CODEC)
                    .build());

    private ModDataComponents() {}

    public static void register(BusGroup eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
