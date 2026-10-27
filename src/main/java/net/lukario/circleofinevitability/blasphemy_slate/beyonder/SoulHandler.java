package net.lukario.circleofinevitability.blasphemy_slate.beyonder;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public class SoulHandler {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (event.player().level().isClientSide()) return;
        Player player = event.player();

            SoulCore.setSpirituality(player, SoulCore.getSpirituality(player) + Beyonder.spiritualityRegenPerTick(player) * net.lukario.circleofinevitability.fate.Fate.tier(player).regen);

            player.sendOverlayMessage(
                    Component.translatable("hud.circle_of_inevitability.spirituality", String.valueOf(SoulCore.getSpirituality(player)))
            );
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();

        if (entity.level().isClientSide()) return;
        if (entity.tickCount % 20 != 0) return;

        if (SoulCore.getCorruption(entity)==100){
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


