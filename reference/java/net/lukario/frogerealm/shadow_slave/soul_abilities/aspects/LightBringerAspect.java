package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class LightBringerAspect{


    public static void lightBringerAspectAbilityOneUsed(Player player, Level level, ServerLevel serverLevel) {

        if (SoulCore.getAspect(player).equals("Light Bringer")){
            if (SoulCore.getSoulEssence(player)<150)return;
            SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-150);
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20*20, 2, false, false));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20*20, 0, false, false));

            player.addEffect(new MobEffectInstance(MobEffects.HEAL, 1, 2, false, false));
        }

    }

}
