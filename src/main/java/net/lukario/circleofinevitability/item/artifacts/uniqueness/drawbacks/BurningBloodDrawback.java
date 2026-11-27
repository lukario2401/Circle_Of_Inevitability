package net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks;

import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquenessDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquenessItem;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.Wielder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.BaseFireBlock;

/**
 * Burning blood: the wielder catches fire on their own (1 in igniteOneIn[level] each second), Fire Resistance burns
 * away, burning hurts burnDamage a second through the uniqueness's protection, and water and rain scald them
 * (waterDamage[level] a second). From groundFireFrom the ground under them sometimes bursts into flame.
 */
public final class BurningBloodDrawback implements UniquenessDrawback {
    private static final String WARNED = "burning_warned";

    private final int[] igniteOneIn;
    private final float burnDamage;
    private final float[] waterDamage;
    private final int groundFireFrom, groundFireOneIn;

    public BurningBloodDrawback(int[] igniteOneIn, float burnDamage, float[] waterDamage, int groundFireFrom, int groundFireOneIn) {
        this.igniteOneIn = igniteOneIn;
        this.burnDamage = burnDamage;
        this.waterDamage = waterDamage;
        this.groundFireFrom = groundFireFrom;
        this.groundFireOneIn = groundFireOneIn;
    }

    @Override
    public Component describe() {
        return Component.literal("Burning blood: you catch fire on your own, Fire Resistance does nothing, water and rain "
                + "scald you; at Extreme the ground under you ignites.");
    }

    @Override
    public void second(Wielder w) {
        if (!UniquenessItem.active(this, w)) return;
        var player = w.player();
        var level = w.level();
        var random = player.getRandom();
        if (player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            player.removeEffect(MobEffects.FIRE_RESISTANCE);
            if (!w.state().getBooleanOr(WARNED, false)) {
                w.state().putBoolean(WARNED, true);
                player.sendSystemMessage(Component.literal("Your blood burns the protection away.").withStyle(ChatFormatting.GOLD));
            }
        }
        if (random.nextInt(Math.max(1, w.at(igniteOneIn))) == 0 && !player.isInWaterOrRain()) {
            player.igniteForSeconds(4);
            player.sendOverlayMessage(Component.literal("Your blood boils over.").withStyle(ChatFormatting.GOLD));
        }
        if (player.isOnFire() && burnDamage > 0) {
            player.invulnerableTime = 0;
            player.hurtServer(level, w.uniqueness().damage(level), burnDamage);
        }
        if (player.isInWaterOrRain() && w.at(waterDamage) > 0) {
            player.invulnerableTime = 0;
            player.hurtServer(level, w.uniqueness().damage(level), w.at(waterDamage));
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1, player.getZ(), 6, .3, .5, .3, .02);
            Spells.sound(level, player.position(), SoundEvents.FIRE_EXTINGUISH, .5f, 1.4f);
        }
        if (w.corruptionLevel() >= groundFireFrom && random.nextInt(Math.max(1, groundFireOneIn)) == 0) {
            BlockPos feet = player.blockPosition();
            if (level.getBlockState(feet).isAir() && BaseFireBlock.canBePlacedAt(level, feet, player.getDirection()))
                level.setBlockAndUpdate(feet, BaseFireBlock.getState(level, feet));
        }
    }

    @Override
    public void died(Wielder w) {
        w.state().putBoolean(WARNED, false);
    }
}
