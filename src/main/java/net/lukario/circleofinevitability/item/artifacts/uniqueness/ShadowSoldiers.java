package net.lukario.circleofinevitability.item.artifacts.uniqueness;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/** The shadow soldier's entity type. Loyal soldiers count as their owner's allies for every ability. */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID)
public final class ShadowSoldiers {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "shadow_soldier");
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, CircleOfInevitability.MOD_ID);
    public static final RegistryObject<EntityType<ShadowSoldier>> SOLDIER = ENTITIES.register("shadow_soldier",
            () -> EntityType.Builder.of(ShadowSoldier::new, MobCategory.MONSTER).sized(.6f, 1.95f).eyeHeight(1.7f)
                    .fireImmune().clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, ID)));

    static {
        // Their blows come from the uniqueness itself: never softened by its protection.
        Uniquenesses.addOwnAttacker(entity -> entity instanceof ShadowSoldier soldier && !soldier.loyal());
        Spells.addAllyCheck((caster, other) -> other instanceof ShadowSoldier soldier && soldier.loyal()
                && caster.getUUID().equals(soldier.owner()));
    }

    private ShadowSoldiers() {}

    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(SOLDIER.get(), ShadowSoldier.createAttributes().build());
    }
}
