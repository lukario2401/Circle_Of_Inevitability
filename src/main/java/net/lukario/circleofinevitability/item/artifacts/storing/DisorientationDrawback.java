package net.lukario.circleofinevitability.item.artifacts.storing;

import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.screen.ScreenImages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

/**
 * Fits of disorientation: each second carried there is a 1 in oneIn[tier] chance of a fit. The screen is covered
 * with 'overlayImage' (textures/gui/<overlayImage>.png, stretched over the whole screen) and the bearer gets Nausea
 * and Slowness and stumbles; from 'darknessFromTier' the world also goes dark.
 */
public final class DisorientationDrawback implements ArtifactDrawback {
    private final int minTier;
    private final int[] oneIn;
    private final int darknessFromTier;
    private final String overlayImage;

    public DisorientationDrawback(int minTier, int[] oneIn, int darknessFromTier, String overlayImage) {
        this.minTier = minTier;
        this.oneIn = oneIn;
        this.darknessFromTier = darknessFromTier;
        this.overlayImage = overlayImage;
    }

    @Override public int minTier() { return minTier; }

    @Override
    public Component describe(int tier) {
        return Component.literal("Fits of disorientation cloud your sight" + (tier >= darknessFromTier ? " and darken the world." : "."));
    }

    @Override
    public void second(Bearer b) {
        if (!b.carried() || b.exempt() || !appliesAt(b.tier())) return;
        if (b.player().getRandom().nextInt(Math.max(1, b.at(oneIn))) == 0) fit(b);
    }

    /** A fit, now. */
    public void fit(Bearer b) {
        var p = b.player();
        int strength = Math.max(0, b.tier() - minTier);
        p.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160 + 40 * strength, 0));
        p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, strength));
        if (b.tier() >= darknessFromTier) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
        ScreenImages.showFullScreen(p, b.key("overlay"), overlayImage, 70 + 20 * strength);
        double angle = p.getRandom().nextDouble() * Math.PI * 2;
        p.setDeltaMovement(p.getDeltaMovement().add(new Vec3(Math.cos(angle), 0.15, Math.sin(angle)).scale(0.45)));
        p.hurtMarked = true;                                                  // the client accepts the stumble
        Spells.sound(b.level(), p.position(), SoundEvents.WITHER_AMBIENT, 0.35f, 0.5f);
        p.sendOverlayMessage(Component.literal(b.artifact().name() + " writhes. The world tilts.").withStyle(ChatFormatting.DARK_RED));
    }
}
