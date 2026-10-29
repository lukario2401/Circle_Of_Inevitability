package net.lukario.circleofinevitability.item.artifacts;

import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** LOTM's Sun Brooch: a balanced purification/heat adaptation, not Sealed Artifact 3-078. */
public final class SunBroochItem extends SealedArtifactItem {
    public static final int COOLDOWN_TICKS = 160;
    public static final double RADIUS = 6;
    public static final float DAMAGE = 8;
    public static final float UNDEAD_DAMAGE = 16;
    public static final int MAX_HEAT = 100;
    public static final int HOT = 60;
    public static final int CAST_HEAT = 20;

    public SunBroochItem(Properties properties) {
        super(properties, "sun_brooch", COOLDOWN_TICKS,
                "tooltip.circle_of_inevitability.sun_brooch.power",
                "tooltip.circle_of_inevitability.sun_brooch.drawback",
                "tooltip.circle_of_inevitability.sun_brooch.cooling");
    }

    @Override
    protected boolean activate(ServerLevel level, ServerPlayer player, ItemStack stack) {
        Vec3 center = player.position().add(0, .8, 0);
        for (var target : Spells.enemiesAround(player, level, player.position(), RADIUS)) {
            // Purification attacks monsters and undead, rather than neutral animals or other players.
            if (!Spells.isEvil(target) || !player.hasLineOfSight(target)) continue;
            Spells.strike(player, target, target.is(EntityTypeTags.UNDEAD) ? UNDEAD_DAMAGE : DAMAGE);
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
            level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY() + target.getBbHeight() / 2,
                    target.getZ(), 10, .25, .4, .25, .02);
        }
        // Self-cleanse only; it does not erase the artifact's own heat or drawbacks.
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200, 0));
        if (!player.isCreative()) setHeat(player, heat(player) + CAST_HEAT);
        for (int i = 0; i < 40; i++) {
            double angle = i * Math.PI * 2 / 40;
            level.sendParticles(ParticleTypes.END_ROD, center.x + Math.cos(angle) * RADIUS, center.y,
                    center.z + Math.sin(angle) * RADIUS, 1, .02, .08, .02, .01);
        }
        Spells.sound(level, player.position(), SoundEvents.BEACON_ACTIVATE, .8f, 1.6f);
        player.sendOverlayMessage(Component.translatable("message.circle_of_inevitability.sun_brooch.purify", heat(player), MAX_HEAT));
        return true;
    }

    public int heat(ServerPlayer player) {
        return Math.clamp(state(player).getIntOr("heat", 0), 0, MAX_HEAT);
    }

    private void setHeat(ServerPlayer player, int heat) {
        state(player).putInt("heat", Math.clamp(heat, 0, MAX_HEAT));
    }

    @Override
    public void carriedTick(ServerLevel level, ServerPlayer player, boolean carried) {
        int before = heat(player);
        boolean exempt = player.isCreative() || player.isSpectator();
        int next = exempt ? 0 : Math.clamp(before + (!carried ? -2 : player.isInWaterOrRain() ? -5 : 1), 0, MAX_HEAT);
        setHeat(player, next);
        if (!carried || exempt) return;
        if (next >= HOT) {
            player.causeFoodExhaustion(.4f);
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
            if (before < HOT) player.sendSystemMessage(Component.translatable("message.circle_of_inevitability.sun_brooch.hot"));
        }
        if (next == MAX_HEAT)
            player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, false, true));
        if (player.getMainHandItem().getItem() == this || player.getOffhandItem().getItem() == this)
            player.sendOverlayMessage(Component.translatable("message.circle_of_inevitability.sun_brooch.heat", next, MAX_HEAT));
    }
}
