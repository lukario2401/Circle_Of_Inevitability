package net.lukario.circleofinevitability.boss.star_dragon;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobRules;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.entity.*;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.*;

@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID)
public final class StarDragonBossRegistry {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, "star_dragon_boss");
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, CircleOfInevitability.MOD_ID);
    public static final RegistryObject<EntityType<StarDragonBoss>> BOSS = ENTITIES.register("star_dragon_boss",
            () -> EntityType.Builder.of(StarDragonBoss::new, MobCategory.MONSTER).sized(3.2f, 3.4f)
                    .eyeHeight(2.8f).fireImmune().clientTrackingRange(12).updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, ID)));
    private StarDragonBossRegistry() {}
    @SubscribeEvent
    public static void attributes(EntityAttributeCreationEvent event) {
        event.put(BOSS.get(), StarDragonBoss.createAttributes().build());
        // Stats belong to this encounter; avoid the generic Beyonder percentage boost on top.
        BeyonderMobRules.register(BOSS.get(), BeyonderMobRules.Rule.chance(1).pathways("Star Dragon").sequence(2, 1, 0, 0));
    }
}
