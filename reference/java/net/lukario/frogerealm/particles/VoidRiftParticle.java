package net.lukario.frogerealm.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;

public class VoidRiftParticle extends TextureSheetParticle {

    private final SpriteSet spriteSet;

    protected VoidRiftParticle(ClientLevel level,
                               double x, double y, double z,
                               double xd, double yd, double zd,
                               SpriteSet spriteSet) {
        super(level, x, y, z, xd, yd, zd);

        this.spriteSet = spriteSet;

        // Lifetime
        this.lifetime = 30 + level.random.nextInt(10);
        this.gravity = 0f;

        // Slight inward drift feel
        this.xd = xd * 0.05;
        this.yd = 0.02 + level.random.nextFloat() * 0.02;
        this.zd = zd * 0.05;

        // Size variation
        this.quadSize = 0.4f + level.random.nextFloat() * 0.3f;

        // Void-like purple color
        this.rCol = 0.4f + level.random.nextFloat() * 0.2f;
        this.gCol = 0.0f;
        this.bCol = 0.6f + level.random.nextFloat() * 0.3f;

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
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
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
                                       double xd, double yd, double zd) {

            return new VoidRiftParticle(
                    level,
                    x, y, z,
                    xd, yd, zd,
                    this.spriteSet
            );
        }
    }
}
