package net.lukario.frogerealm.shadow_slave.soul_shards;

import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = "forgerealmmod")
public class SoulHandler {

    @SubscribeEvent
    public static void onPlayerGainXp(PlayerXpEvent.XpChange event) {
        Player player = event.getEntity();

        if (player.level().isClientSide) return; // server only

        int xpGained = event.getAmount();
        if (xpGained <= 0) return;

        int soulShardsToAdd = xpGained * 2;

        SoulCore.addSoulShards(player, soulShardsToAdd);
        SoulShardPower.applySoulShardPowers(player);
        player.sendSystemMessage(Component.literal("You have: "+ SoulCore.getSoulShards(player)));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;
        Player player = event.player;

            SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) + (float) (SoulCore.getAspectTier(player) * SoulCore.getAspectTier(player) * SoulCore.getAscensionStage(player)) /(8-SoulCore.getAscensionStage(player)));

            player.displayClientMessage(
                    Component.literal("Soul Essence: " + SoulCore.getSoulEssence(player)),
                    true
            );
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide) return;
        if (entity.tickCount % 20 != 0) return;

        if (SoulCore.getCorruption(entity)==100){
            entity.kill();
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
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 0));
        }
        if (SoulCore.getCorruption(entity)>65){
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


