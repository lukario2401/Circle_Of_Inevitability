package net.lukario.frogerealm.particles.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Client-side particle that draws and moves according to a ParticleFx.
 * You shouldn't need to edit this to make new particles — only to add new ParticleFx options.
 */
public class ParticleFxParticle extends TextureSheetParticle {

    private final ParticleFx fx;
    private final TextureAtlasSprite[] frames;
    private final float sizeMultiplier;
    private final float spinRadians;

    protected ParticleFxParticle(ClientLevel level, double x, double y, double z,
                                 double xd, double yd, double zd, ParticleFx fx) {
        super(level, x, y, z); // this constructor doesn't randomize the velocity
        this.fx = fx;

        // exact velocity from whoever spawned it
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;

        this.lifetime = fx.lifetimeTicks()
                + (fx.lifetimeRandomTicks() > 0 ? this.random.nextInt(fx.lifetimeRandomTicks() + 1) : 0);
        this.gravity = fx.gravityStrength();
        this.friction = fx.frictionFactor();
        this.hasPhysics = fx.collides();
        this.sizeMultiplier = 1f + (this.random.nextFloat() * 2f - 1f) * fx.sizeRandomness();
        this.spinRadians = fx.spinDegrees() * Mth.DEG_TO_RAD;
        if (fx.hasRandomRotation()) {
            this.roll = this.random.nextFloat() * Mth.TWO_PI;
            this.oRoll = this.roll;
        }

        // look up the textures in the particle atlas (every PNG in textures/particle/ is in it)
        TextureAtlas atlas = (TextureAtlas) Minecraft.getInstance().getTextureManager()
                .getTexture(TextureAtlas.LOCATION_PARTICLES);
        List<ResourceLocation> textures = fx.textures();
        this.frames = new TextureAtlasSprite[textures.size()];
        for (int i = 0; i < textures.size(); i++) {
            frames[i] = atlas.getSprite(textures.get(i)); // missing -> purple/black texture, no crash
        }
        this.setSprite(frames[0]);

        applyColor(0f);
    }

    @Override
    public void tick() {
        this.oRoll = this.roll;
        super.tick(); // moves, applies gravity/friction, removes when too old
        if (this.removed) return;

        this.roll += spinRadians;

        float progress = (float) this.age / this.lifetime;
        if (frames.length > 1) {
            int frame = Math.min(frames.length - 1, (int) (progress * frames.length));
            this.setSprite(frames[frame]);
        }
        applyColor(progress);
    }

    private void applyColor(float progress) {
        int from = fx.startColor();
        int to = fx.finalColor();
        this.setColor(
                Mth.lerp(progress, ((from >> 16) & 0xFF) / 255f, ((to >> 16) & 0xFF) / 255f),
                Mth.lerp(progress, ((from >> 8) & 0xFF) / 255f, ((to >> 8) & 0xFF) / 255f),
                Mth.lerp(progress, (from & 0xFF) / 255f, (to & 0xFF) / 255f));
        this.setAlpha(Mth.lerp(progress, ((from >>> 24) & 0xFF) / 255f, ((to >>> 24) & 0xFF) / 255f));
    }

    @Override
    public float getQuadSize(float partialTick) {
        float progress = Mth.clamp((this.age + partialTick) / this.lifetime, 0f, 1f);
        return Mth.lerp(progress, fx.startSize(), fx.finalSize()) * sizeMultiplier;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return fx.glows() ? LightTexture.FULL_BRIGHT : super.getLightColor(partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    /** Registered in ClientParticleHandler. */
    public static Particle create(ParticleFx fx, ClientLevel level,
                                  double x, double y, double z, double xd, double yd, double zd) {
        return new ParticleFxParticle(level, x, y, z, xd, yd, zd, fx);
    }
}
