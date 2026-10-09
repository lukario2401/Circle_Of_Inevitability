package net.lukario.frogerealm.item.custom.other;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Double jump (jump again in the air) and the dash after it (sneak in the air).
 *
 * This runs on the player's own game only. It reads the keyboard and moves, plays the sound and shows the
 * particles for the player sitting at this computer, and all of that belongs to the client thread. In single
 * player the same tick event also arrives from the built-in server, on the server's thread: that one must be
 * turned away (the first lines of onPlayerTick), or two threads use the client's world at once and the game
 * crashes ("Accessing LegacyRandomSource from multiple threads", or an error in the particle or sound engine).
 */
@Mod.EventBusSubscriber(modid = "forgerealmmod", value = Dist.CLIENT)
public class SneakLaunchHandler {

    private static final String DOUBLE_JUMP = "fr_double_jump";
    private static final String CAN_USE = "fr_can_use_double_jump";
    private static final String COOLDOWN = "fr_sneak_double_jump_cooldown";


    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // only the tick of this game's own player, on the client thread (see the note at the top)
        if (!event.player.level().isClientSide()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || event.player != player) return;

        // while flying (creative, or the wings of an ability) jump and sneak mean up and down, not jump and dash
        boolean flying = player.getAbilities().flying;

        var data = player.getPersistentData();

        if (!mc.options.keyJump.isDown() && !player.onGround()) {
            data.putBoolean(DOUBLE_JUMP, false);
        }
        if (!mc.options.keyJump.isDown() && player.onGround()) {
            data.putBoolean(DOUBLE_JUMP, true);
            data.putBoolean(CAN_USE, true);
        }

        if (mc.options.keyJump.consumeClick()) {
            boolean used = data.getBoolean(DOUBLE_JUMP);
            boolean can_use = data.getBoolean(CAN_USE);

            if (!used && !player.onGround() && can_use && !player.isCreative() && !flying) {
                launchPlayer(player,0.4,1.2);
                data.putBoolean(DOUBLE_JUMP, true);
                data.putBoolean(CAN_USE, false);
            }
        }

        if (mc.options.keyJump.isDown()) {
            data.putBoolean(DOUBLE_JUMP, true);
        }

        if (!player.isShiftKeyDown()){
            data.putBoolean(COOLDOWN, false);
        }

        if (!data.getBoolean(CAN_USE) && player.isShiftKeyDown() && !data.getBoolean(COOLDOWN) && !player.isCreative() && !flying){
            launchPlayer(player, 1.6, 0);
            data.putBoolean(COOLDOWN, true);
        }

//        displaySpeed(player);
    }


    private static void displaySpeed(Player player){
        double deltaX = player.getDeltaMovement().x;
        double deltaZ = player.getDeltaMovement().z;

        double currentSpeed = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 20;

        String formattedSpeed = String.format("%.1f", currentSpeed);

        player.displayClientMessage(Component.literal("§6Speed: §e" + formattedSpeed + " §fB/S"), true);
    }

    private static void launchPlayer(Player player, double forwardStrength, double upwardStrength) {

        Vec3 look = player.getLookAngle().normalize();
        player.push(look.x * forwardStrength, upwardStrength, look.z * forwardStrength);

        player.hurtMarked = true;

        Vec3 pos = player.position();
        player.level().playSound(player, pos.x, pos.y, pos.z,
                SoundEvents.FIREWORK_ROCKET_LAUNCH,
                SoundSource.PLAYERS,
                1.0f, 1.0f);

        spawnParticleCloud(player.level(), pos);
    }


    private static void spawnParticleCloud(Level level, Vec3 pos) {
        for (int i = 0; i < 45; i++) {
            double offsetX = (level.random.nextDouble() - 0.5) * 0.3;
            double offsetY = level.random.nextDouble() * 0.2;
            double offsetZ = (level.random.nextDouble() - 0.5) * 0.3;

            double speedX = (level.random.nextDouble() - 0.5) * 0.05;
            double speedY = level.random.nextDouble() * 0.05;
            double speedZ = (level.random.nextDouble() - 0.5) * 0.05;

            level.addParticle(
                    ParticleTypes.FIREWORK,
                    pos.x + offsetX*3,
                    pos.y + offsetY*3,
                    pos.z + offsetZ*3,
                    speedX, speedY, speedZ
            );
        }
    }

}
