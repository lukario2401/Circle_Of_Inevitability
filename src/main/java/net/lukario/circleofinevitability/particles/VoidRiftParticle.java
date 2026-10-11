package net.lukario.circleofinevitability.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class VoidRiftParticle extends SingleQuadParticle {

    private final SpriteSet spriteSet;

    protected VoidRiftParticle(ClientLevel level,
                               double x, double y, double z,
                               double xd, double yd, double zd,
                               SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd, spriteSet.first()); // 26.2: needs a sprite; set from the age below, as before

        this.spriteSet = spriteSet;

        // Lifetime
        this.lifetime = 30 + level.getRandom().nextInt(10);
        this.gravity = 0f;

        // Slight inward drift feel
        this.xd = xd * 0.05;
        this.yd = 0.02 + level.getRandom().nextFloat() * 0.02;
        this.zd = zd * 0.05;

        // Size variation
        this.quadSize = 0.4f + level.getRandom().nextFloat() * 0.3f;

        // Void-like purple color
        this.rCol = 0.4f + level.getRandom().nextFloat() * 0.2f;
        this.gCol = 0.0f;
        this.bCol = 0.6f + level.getRandom().nextFloat() * 0.3f;

        this.alpha = 1.0f;

        // Force start at frame 0
        this.setSpriteFromAge(this.spriteSet);
    }

    @Override
    public void tick() {
        super.tick();

        // Remove if dead
        if (this.age >= this.lifetime) {
            this.remove();
            return;
        }

        // Animate properly from age
        this.setSpriteFromAge(this.spriteSet);

        // Slow horizontal motion
        this.xd *= 0.92;
        this.zd *= 0.92;

        // Smooth fade out
        this.alpha = 1.0f - ((float) this.age / this.lifetime);
    }

    @Override
    protected SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    // =========================
    // Provider
    // =========================

    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        @Override
        public Particle createParticle(SimpleParticleType type,
                                       ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd,
                                       RandomSource random) {

            return new VoidRiftParticle(
                    level,
                    x, y, z,
                    xd, yd, zd,
                    this.spriteSet
            );
        }
    }
}
