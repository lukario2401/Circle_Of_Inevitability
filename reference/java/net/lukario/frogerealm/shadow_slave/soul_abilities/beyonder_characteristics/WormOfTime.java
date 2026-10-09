package net.lukario.frogerealm.shadow_slave.soul_abilities.beyonder_characteristics;

import net.lukario.frogerealm.combat.Spells;
import net.lukario.frogerealm.root.Root;
import net.lukario.frogerealm.root.RootRestriction;
import net.lukario.frogerealm.screen.ScreenAnchor;
import net.lukario.frogerealm.screen.ScreenImages;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.lukario.frogerealm.status.FallGuard;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class WormOfTime {

    private static final String LIFE_THEFT_VULNERABILITY = "word_of_time_life_theft_vulnerability";


    public static void wormOfTimeAbility1(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!canUseCharacteristic(player, bypassClassCheck)) return;
        if (SoulCore.getSoulEssence(player) < 250) return;
        if (SoulCore.getAscensionStage(player) < 0) return;

        SoulCore.setSoulEssence(player,SoulCore.getSoulEssence(player)-250);


        if (!player.isShiftKeyDown()){

            LivingEntity livingEntity = Spells.aimEnemy(player,sl,24);
            int hitCount = livingEntity.getPersistentData().getInt(LIFE_THEFT_VULNERABILITY);
            if (hitCount==0)hitCount=1;
            int healthStealAmount = 15*hitCount;

            if (livingEntity.getHealth()>healthStealAmount){
                livingEntity.setHealth(livingEntity.getHealth()-healthStealAmount);
                player.sendSystemMessage(Component.literal(""+hitCount));
                player.setHealth(player.getHealth()+healthStealAmount);
            }else{
                player.setHealth((player.getHealth()+livingEntity.getHealth()));
                livingEntity.kill();
            }
            livingEntity.getPersistentData().putInt(LIFE_THEFT_VULNERABILITY,hitCount+1);

        }else{
            if (Root.has(player, RootRestriction.TELEPORT)) {
                player.sendSystemMessage(Component.literal("You are held in place."));
                return;
            }

            Vec3 there = Spells.blinkSpot(player, sl, 32);

            LivingEntity livingEntity = Spells.aimEnemy(player,sl,32);
            if (livingEntity!=null){
                there=livingEntity.position();
            }

            if (there == null) {
                player.sendSystemMessage(Component.literal("There is no room for you there."));
                return;                                                  // nothing spent
            }
            player.teleportTo(there.x, there.y, there.z);
            FallGuard.protect(player, 200);

        }
    }



    private static boolean canUseCharacteristic(Player player, boolean bypassClassCheck){
        if (bypassClassCheck) return true;
        return "Worm Of Time".equals(SoulCore.getAspect(player));
    }
}
