package net.lukario.circleofinevitability.particles.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side life of one damage number: counts its ticks and stays above its entity's head.
 * It goes in DamageNumberRenderer's particle group, which draws all of them in one go.
 * The look (size, colour, how long it stays) is at the top of DamageNumberRenderer.
 */
public class DamageNumberParticle extends Particle {

    private final ClientLevel clientLevel;
    private final int entityId;
    private final String text;
    private final int row;
    private long lastTickTime;
    private Vec3 top;           // the top of the head, where it was last seen
    private boolean named;      // a player: the number goes above the name

    protected DamageNumberParticle(ClientLevel level, double x, double y, double z, DamageNumber number) {
        super(level, x, y, z);
        this.clientLevel = level;
        this.entityId = number.entityId();
        this.text = number.text();
        this.lifetime = DamageNumberRenderer.HOLD_TICKS + DamageNumberRenderer.FADE_TICKS;
        this.gravity = 0f;
        this.hasPhysics = false;
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.lastTickTime = level.getGameTime();
        this.top = new Vec3(x, y, z);
        this.row = DamageNumberRenderer.claimRow(this, level);
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.lastTickTime = clientLevel.getGameTime();
        if (this.age++ >= this.lifetime) this.remove();
    }

    // (26.2: drawn by the NUMBERS particle group; NO_RENDER only if that group could not be registered)
    @Override
    public ParticleRenderType getGroup() {
        return DamageNumberRenderer.isRegistered() ? DamageNumberRenderer.NUMBERS : ParticleRenderType.NO_RENDER;
    }

    // ---------- used by DamageNumberRenderer ----------

    int entityId() {
        return entityId;
    }

    String text() {
        return text;
    }

    /** Which line above the head it is on: 0 = the lowest. It keeps its line for as long as it shows. */
    int row() {
        return row;
    }

    /** Ticks since it appeared. */
    float time(float partialTick) {
        return this.age + partialTick;
    }

    /**
     * The top of its entity's head this frame. When the entity is gone (dead and removed, too far away) the
     * number stays where the head was last.
     */
    Vec3 top(float partialTick) {
        Entity entity = entityId < 0 ? null : clientLevel.getEntity(entityId);
        if (entity != null) {
            top = entity.getPosition(partialTick).add(0, entity.getBbHeight(), 0);
            named = entity instanceof Player;
        }
        return top;
    }

    /** True for a player. Call it after top(...). */
    boolean named() {
        return named;
    }

    /** True when the particle engine stopped ticking it (level changed, particles cleared...). */
    boolean isStale(ClientLevel currentLevel) {
        return currentLevel != clientLevel || currentLevel.getGameTime() - lastTickTime > 5;
    }

    void discard() {
        this.remove();
    }

    /** Registered in ClientParticleHandler. */
    public static Particle create(DamageNumber number, ClientLevel level,
                                  double x, double y, double z, double xd, double yd, double zd) {
        return new DamageNumberParticle(level, x, y, z, number);
    }
}
