package net.lukario.frogerealm.particles.fx;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side life of one ModelFx: counts its ticks and follows its entity.
 * It is a NO_RENDER particle — ModelFxRenderer draws all of them in one go.
 * You shouldn't need to edit this to make new effects — only to add new ModelFx options.
 */
public class ModelFxParticle extends Particle {

    /** Where the neck is under the top of the head, as a part of how tall the entity is (a player: 0.4 of 1.8 blocks). */
    private static final float NECK_BELOW_TOP = 0.22f;

    private final ModelFx fx;
    private final ClientLevel clientLevel;
    private long lastTickTime;

    protected ModelFxParticle(ClientLevel level, double x, double y, double z, ModelFx fx) {
        super(level, x, y, z);
        this.fx = fx;
        this.clientLevel = level;
        this.lifetime = fx.delayTicks() + fx.lifetimeTicks();
        this.gravity = 0f;
        this.hasPhysics = false;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.lastTickTime = level.getGameTime();

        if (fx.isClear()) {
            ModelFxRenderer.clearFollowing(fx.followId(), fx.tagName());
            this.remove();
            return;
        }
        if (fx.followsEntity()) {
            Entity entity = level.getEntity(fx.followId());
            if (entity == null || !entity.isAlive()) {
                this.remove();
                return;
            }
            moveTo(entity.position().add(offsetOn(entity)));
            this.xo = this.x;
            this.yo = this.y;
            this.zo = this.z;
        }
        ModelFxRenderer.track(this);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.lastTickTime = clientLevel.getGameTime();
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        if (fx.followsEntity()) {
            Entity entity = clientLevel.getEntity(fx.followId());
            if (entity == null || !entity.isAlive()) {
                this.remove();
                return;
            }
            moveTo(entity.position().add(offsetOn(entity)));
        }
    }

    /**
     * Where on the entity the model sits, from its feet. What is worn on the head is counted from the top of
     * the head as the entity stands right now, so it comes down with a player who sneaks.
     */
    private Vec3 offsetOn(Entity entity) {
        return fx.turnsWithHead() ? fx.followOffset().add(0, entity.getBbHeight(), 0) : fx.followOffset();
    }

    private void moveTo(Vec3 position) {
        this.setPos(position.x, position.y, position.z);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        // drawn by ModelFxRenderer
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.NO_RENDER;
    }

    @Override
    public boolean shouldCull() {
        return false;
    }

    // ---------- used by ModelFxRenderer ----------

    ModelFx fx() {
        return fx;
    }

    /** Ticks since it appeared (after the delay); negative = not visible yet. */
    float time(float partialTick) {
        return this.age + partialTick - fx.delayTicks();
    }

    /** Where the pivot is this frame. */
    Vec3 anchor(float partialTick) {
        if (fx.followsEntity()) {
            Entity entity = clientLevel.getEntity(fx.followId());
            if (entity != null) return entity.getPosition(partialTick).add(offsetOn(entity));
        }
        return new Vec3(this.xo + (this.x - this.xo) * partialTick,
                this.yo + (this.y - this.yo) * partialTick,
                this.zo + (this.z - this.zo) * partialTick);
    }

    /**
     * How far the entity it is stuck to has turned, in degrees, for models that turn with it (ModelFx.turning()).
     * The way its BODY faces, not its head: what is worn on the back stays on the back while the head looks round.
     * What is worn on the head (ModelFx.turningWithHead()) goes by the head instead.
     * 0 for every other model.
     */
    float turn(float partialTick) {
        if (!fx.turnsWithEntity() || !fx.followsEntity()) return 0f;
        Entity entity = clientLevel.getEntity(fx.followId());
        if (entity instanceof LivingEntity living) {
            return fx.turnsWithHead()
                    ? Mth.rotLerp(partialTick, living.yHeadRotO, living.yHeadRot)
                    : Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);
        }
        return entity == null ? 0f : entity.getViewYRot(partialTick);
    }

    /**
     * How far the head it is worn on looks up or down, in degrees (+ = down), for models worn on the head.
     * 0 for every other model.
     */
    float nod(float partialTick) {
        if (!fx.turnsWithHead() || !fx.followsEntity()) return 0f;
        Entity entity = clientLevel.getEntity(fx.followId());
        return entity == null ? 0f : entity.getViewXRot(partialTick);
    }

    /**
     * How far above the neck the model's spot is, in blocks: a head nods around its neck, not around its top,
     * so what sits on it swings forward a little when the head looks down. (A head is about 0.22 of a body.)
     */
    float aboveNeck() {
        if (!fx.turnsWithHead() || !fx.followsEntity()) return 0f;
        Entity entity = clientLevel.getEntity(fx.followId());
        return entity == null ? 0f : (float) fx.followOffset().y + entity.getBbHeight() * NECK_BELOW_TOP;
    }

    /** True when the particle engine stopped ticking it (level changed, particles cleared...). */
    boolean isStale(ClientLevel currentLevel) {
        return currentLevel != clientLevel || currentLevel.getGameTime() - lastTickTime > 5;
    }

    void discard() {
        this.remove();
    }

    /** Registered in ClientParticleHandler. */
    public static Particle create(ModelFx fx, ClientLevel level,
                                  double x, double y, double z, double xd, double yd, double zd) {
        return new ModelFxParticle(level, x, y, z, fx);
    }
}
