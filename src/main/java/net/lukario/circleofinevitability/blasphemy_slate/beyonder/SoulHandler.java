package net.lukario.circleofinevitability.blasphemy_slate.beyonder;

import net.lukario.circleofinevitability.fate.Fate;
import net.lukario.circleofinevitability.network.CSyncSoulStatsPacket;
import net.lukario.circleofinevitability.network.PacketHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public class SoulHandler {

    private static final Map<UUID, CSyncSoulStatsPacket> LAST_SENT_SOUL_STATS = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;

        SoulCore.setSpirituality(player, SoulCore.getSpirituality(player) + Beyonder.spiritualityRegenPerTick(player)
                * Fate.tier(player).regen * (Fate.applies(player) ? 1.05f : 1f));

        CSyncSoulStatsPacket snapshot = new CSyncSoulStatsPacket(
                SoulCore.getSpirituality(player), Beyonder.maxSpirituality(player), SoulCore.getCorruption(player));
        if (!snapshot.equals(LAST_SENT_SOUL_STATS.get(player.getUUID()))) {
            PacketHandler.sendToPlayer(snapshot, player);
            LAST_SENT_SOUL_STATS.put(player.getUUID(), snapshot);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SENT_SOUL_STATS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide()) return;
        if (entity.tickCount % 20 != 0) return;

        if (SoulCore.getCorruption(entity) == SoulCore.MAX_CORRUPTION) {
            entity.kill((ServerLevel) entity.level());
        }
        if (SoulCore.getCorruption(entity)>95){
            entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 3));
        }
        if (SoulCore.getCorruption(entity)>90){
            entity.addEffect(new MobEffectInstance(MobEffects.POISON, 120, 2));
            entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 120, 2));
        }
        if (SoulCore.getCorruption(entity)>85){
            entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 120, 1));
        }
        if (SoulCore.getCorruption(entity)>75){
            if (entity.getRandom().nextInt(4) == 0) {
                entity.hurt(entity.damageSources().outOfBorder(), 1);
            }
            entity.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 0));
        }
        if (SoulCore.getCorruption(entity) >= SoulCore.FIRST_HARMFUL_CORRUPTION) {
            if (entity.getRandom().nextInt(5) == 0) {
                entity.hurt(entity.damageSources().outOfBorder(), 1);
            }
        }

        if (entity.tickCount % 400 != 0) return;
        if (SoulCore.getCorruption(entity)>0){
            SoulCore.setCorruption(entity,SoulCore.getCorruption(entity)-1);

        }
    }
}
// python 3.11.9


