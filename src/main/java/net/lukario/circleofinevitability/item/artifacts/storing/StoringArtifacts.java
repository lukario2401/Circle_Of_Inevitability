package net.lukario.circleofinevitability.item.artifacts.storing;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.item.artifacts.SealedArtifacts;
import net.lukario.circleofinevitability.status.Cleansing;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/** The events every StoringArtifact shares: kills feed the carried one, deaths and cleanses reach every drawback. */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StoringArtifacts {
    static {
        Cleansing.addListener(entity -> {
            if (entity instanceof ServerPlayer player) cleansed(player);
        });
    }

    private StoringArtifacts() {}

    @SubscribeEvent
    public static void onStoringArtifactDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) return;
        if (victim instanceof ServerPlayer dead) died(level, dead);
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || killer == victim
                || victim instanceof ArmorStand || !killer.isAlive()) return;
        killed(level, killer, victim);
    }

    /** 'killer' killed 'victim': each storing artifact they carry (the strongest tier of each) feeds. */
    public static void killed(ServerLevel level, ServerPlayer killer, LivingEntity victim) {
        List<ItemStack> carried = new ArrayList<>(SealedArtifacts.carriedByFamily(killer).values());
        for (ItemStack stack : carried)
            if (stack.getItem() instanceof StoringArtifactItem item) item.onKill(level, killer, stack, victim);
    }

    private static void died(ServerLevel level, ServerPlayer player) {
        for (StoringArtifact artifact : StoringArtifact.all()) {
            Bearer bearer = artifact.bearer(level, player, ItemStack.EMPTY);
            for (ArtifactDrawback drawback : artifact.drawbacks()) drawback.bearerDied(bearer);
        }
    }

    /** Called by every cleanse (status/Cleansing) and milk. */
    public static void cleansed(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;
        for (StoringArtifact artifact : StoringArtifact.all()) {
            Bearer bearer = artifact.bearer(level, player, SealedArtifacts.carried(player, artifact.id()));
            for (ArtifactDrawback drawback : artifact.drawbacks()) drawback.cleansed(bearer);
        }
    }
}
