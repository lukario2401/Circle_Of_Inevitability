package net.lukario.circleofinevitability.mount;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.item.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID)
public final class Mounts {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, CircleOfInevitability.MOD_ID);
    private static final List<Breed> BREEDS = new ArrayList<>();

    public record Gait(double groundSpeed, double jumpStrength, double flightAcceleration) {
        public static Gait runner(double speed, double jump) { return new Gait(speed, jump, 0); }
        public static Gait flyer(double acceleration) { return new Gait(0.23, 0.42, acceleration); }
        public boolean flies() { return flightAcceleration > 0; }
    }

    public record Breed(Identifier id, Gait gait, RegistryObject<EntityType<RidingMount>> type,
                        RegistryObject<Item> whistle) {}

    public static final Breed WYVERN = register("wyvern", Gait.flyer(0.058), 1.5F, 1.8F, 1.48F);
    public static final Breed RAPTOR = register("raptor", Gait.runner(0.34, 0.72), 1.1F, 1.65F, 1.36F);

    private Mounts() {}

    public static List<Breed> breeds() { return Collections.unmodifiableList(BREEDS); }

    private static Breed register(String name, Gait gait, float width, float height, float seatHeight) {
        Identifier id = Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, name);
        var type = ENTITIES.register(name, () -> EntityType.Builder.<RidingMount>of(
                (t, level) -> new RidingMount(t, level, gait), MobCategory.CREATURE)
                .sized(width, height).passengerAttachments(seatHeight).clientTrackingRange(10).updateInterval(1)
                .noLootTable().build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
        String itemName = name + "_whistle";
        RegistryObject<Item> whistle = ModItems.ITEMS.register(itemName, () -> new MountWhistleItem(type, ModItems.properties(itemName)
                .stacksTo(1).component(DataComponents.LORE, new ItemLore(List.of(Component.translatable(
                        "tooltip.circle_of_inevitability.mount." + (gait.flies() ? "flying" : "ground")))))));
        var breed = new Breed(id, gait, type, whistle);
        BREEDS.add(breed);
        return breed;
    }

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        for (Breed breed : BREEDS) {
            Gait gait = breed.gait();
            event.put(breed.type().get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30)
                    .add(Attributes.MOVEMENT_SPEED, gait.groundSpeed())
                    .add(Attributes.JUMP_STRENGTH, gait.jumpStrength())
                    .add(Attributes.FLYING_SPEED, gait.flightAcceleration()).add(Attributes.STEP_HEIGHT, 1).build());
        }
    }

    @SubscribeEvent
    public static void creativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            for (Breed breed : BREEDS) event.accept(breed.whistle());
        }
    }
}
