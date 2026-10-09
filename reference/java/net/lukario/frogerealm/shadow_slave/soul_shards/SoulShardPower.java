package net.lukario.frogerealm.shadow_slave.soul_shards;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "forgerealmmod")
public class SoulShardPower {

    public static final int MAX_SHARDS = 7000;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // We check every tick to ensure the 5-tick window is caught immediately
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

//        applySoulShardPowers(event.player);
    }

    public static void applySoulShardPowers(Player player) {
        int shards = SoulCore.getSoulShards(player);
        double percent = (double) shards / MAX_SHARDS;

        // TIER 1: Speed (500+ Shards)
        if (shards >= 500) {
            handleEffect(player, MobEffects.MOVEMENT_SPEED, (int) (percent * 4));
        }

        // TIER 2: Strength (1500+ Shards)
        if (shards >= 1500) {
            handleEffect(player, MobEffects.DAMAGE_BOOST, (int) (percent * 3));
        }

        // TIER 3: Resistance (3000+ Shards)
        if (shards >= 3000) {
            int amplifier = (int) (percent * 2);
            if (amplifier>5){
                amplifier=4;
            }
            if (amplifier==5){
                amplifier=4;
            }
            // temp
            handleEffect(player, MobEffects.DAMAGE_RESISTANCE, amplifier-2);
        }

        // TIER 4: Regeneration (5000+ Shards)
        if (shards >= 5000) {
            handleEffect(player, MobEffects.REGENERATION, (int) (percent * 2));
        }
    }

    /**
     * Logic: If the player doesn't have the effect, OR the level (amplifier) changed,
     * OR the effect is about to run out (<= 5 ticks), reapply it.
     */
    private static void handleEffect(Player player, Holder<MobEffect> effect, int targetAmplifier) {
        MobEffectInstance active = player.getEffect(effect);

        if (active == null || active.getDuration() <= 5 || active.getAmplifier() != targetAmplifier) {
            // Duration is 120 ticks (6 seconds).
            // This provides a huge buffer so the "shouldRefresh" logic has plenty of time to react.
            player.addEffect(new MobEffectInstance(effect, 120, targetAmplifier, false, false));
        }
    }
}