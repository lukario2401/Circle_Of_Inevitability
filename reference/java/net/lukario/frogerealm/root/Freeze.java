package net.lukario.frogerealm.root;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.particles.fx.ModelFx;
import net.lukario.frogerealm.particles.fx.ParticleFx;
import net.lukario.frogerealm.particles.fx.ParticleShapes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Freeze: encase an entity (mob or player) in ice for a while. Works from any class/ability:
 *
 *   Freeze.apply(target, 80);      // 4 seconds
 *   Freeze.isFrozen(target)
 *   Freeze.ticksLeft(target)
 *   Freeze.thaw(target)            // break the ice early
 *
 * Mobs get no AI while frozen: they don't move, turn, attack, fall or get knocked back (they still take damage).
 * Players are rooted instead (see Root): can't move, attack, use items or abilities.
 *
 * Looks: a see-through ice block around it with ice crystals growing on it
 * (models/model_fx/ice_shell + ice_crystals), and it shatters into shards when the freeze ends or the
 * entity dies. Freezing something that is already frozen only makes it last longer.
 * Saved with the entity, so a frozen mob still thaws properly after a reload.
 */
@Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Freeze {

    private static final String TAG = "forgerealm_freeze";
    private static final String TICKS = "Ticks";
    private static final String WAS_NO_AI = "WasNoAi";
    /** The ice models carry this tag, so thawing removes them and nothing else that is stuck to the entity. */
    private static final String MODEL_TAG = "freeze";

    // ---------- looks ----------

    /** See-through ice block, scaled to the entity's size. */
    public static final ModelFx ICE_SHELL = ModelFx.of("ice_shell")
            .seeThrough().glow().fadeIn(3).tag(MODEL_TAG);

    /** Glowing crystal cluster that pops out of the ice. */
    public static final ModelFx ICE_CRYSTALS = ModelFx.of("ice_crystals")
            .glow().aura(0x5560D8FF, 0.05f, 2).fadeIn(2).tag(MODEL_TAG)
            .key(0, ModelFx.pose().scale(0.1f))
            .key(5, ModelFx.pose().scale(1f), ModelFx.Ease.OUT_BACK);

    private static final ParticleFx FROST = ParticleFx.of("fx/flake")
            .color(0xFFE8FBFF).fadeOut()
            .size(0.08f).endSize(0.03f)
            .lifetime(16, 8).friction(0.86f).gravity(0.02f)
            .glow().spin(8f).randomRotation();

    private static final ParticleFx ICE_SHARD = ParticleFx.of("fx/shard")
            .color(0xFFF2FCFF).endColor(0x0060D8FF)
            .size(0.12f).endSize(0.05f).sizeRandom(0.4f)
            .lifetime(18, 10).gravity(0.9f).friction(0.96f)
            .glow().collide().spin(25f).randomRotation();

    private Freeze() {}

    // =========================
    // API
    // =========================

    public static void apply(LivingEntity target, int ticks) {
        if (ticks <= 0 || !target.isAlive() || !(target.level() instanceof ServerLevel sl)) return;

        CompoundTag tag = freezeTag(target);
        int left = tag == null ? 0 : tag.getInt(TICKS);
        if (ticks <= left) return; // already frozen for at least that long

        if (tag == null) {
            tag = new CompoundTag();
            tag.putBoolean(WAS_NO_AI, target instanceof Mob mob && mob.isNoAi());
            target.getPersistentData().put(TAG, tag);
            spawnIce(sl, target, ticks, 0, true);
            frostBurst(sl, target);
        } else {
            // already frozen: the new ice takes over exactly when the old one runs out
            spawnIce(sl, target, ticks - left, left, false);
        }
        tag.putInt(TICKS, ticks);
        hold(target);
        if (!(target instanceof Mob)) Root.apply(target, ticks, RootRestriction.EVERYTHING);
    }

    public static boolean isFrozen(Entity entity) {
        return entity != null && freezeTag(entity) != null;
    }

    public static int ticksLeft(Entity entity) {
        CompoundTag tag = entity == null ? null : freezeTag(entity);
        return tag == null ? 0 : tag.getInt(TICKS);
    }

    /** Breaks the ice now (with the shatter effect). */
    public static void thaw(LivingEntity target) {
        if (!isFrozen(target)) return;
        if (!(target instanceof Mob)) Root.remove(target);
        end(target);
    }

    // =========================
    // Internals
    // =========================

    private static CompoundTag freezeTag(Entity entity) {
        CompoundTag data = entity.getPersistentData();
        return data.contains(TAG) ? data.getCompound(TAG) : null;
    }

    /** Mobs: no AI = no moving, turning, attacking, gravity or knockback. Players are handled by Root. */
    private static void hold(LivingEntity target) {
        if (target instanceof Mob mob) {
            mob.setNoAi(true);
            mob.getNavigation().stop();
            mob.setDeltaMovement(Vec3.ZERO);
            if (mob instanceof Creeper creeper) creeper.setSwellDir(-1); // don't keep hissing towards an explosion
        }
    }

    private static void end(LivingEntity target) {
        CompoundTag tag = freezeTag(target);
        if (tag == null) return;
        target.getPersistentData().remove(TAG);
        if (target instanceof Mob mob) mob.setNoAi(tag.getBoolean(WAS_NO_AI));
        if (target.level() instanceof ServerLevel sl) {
            ParticleShapes.clearModels(sl, target, MODEL_TAG);
            shatter(sl, target);
        }
    }

    /**
     * Ice block + crystal clusters stuck to the entity for 'lifetime' ticks, appearing after 'delay'.
     * fresh = false: continues ice that is already there (no fade/grow, so the change can't be seen).
     */
    private static void spawnIce(ServerLevel sl, LivingEntity target, int lifetime, int delay, boolean fresh) {
        float width = target.getBbWidth();
        float height = target.getBbHeight();
        float facing = target.yBodyRot;
        RandomSource random = target.getRandom();

        ModelFx shell = ICE_SHELL.scale(width + 0.3f, height + 0.2f, width + 0.3f).lifetime(lifetime).delay(delay);
        ModelFx crystals = ICE_CRYSTALS.lifetime(lifetime).delay(delay);
        if (!fresh) {
            shell = shell.fadeIn(0);
            crystals = crystals.fadeIn(0).noKeys();
        }
        float size = Mth.clamp(Math.min(width * 2f, height * 0.9f), 0.5f, 4f);

        ParticleShapes.modelOn(sl, shell, target, new Vec3(0, -0.05, 0), facing, 0, 0);

        // a cluster around the feet, one on the head and one sticking out of each side
        ParticleShapes.modelOn(sl, crystals.scale(size), target, Vec3.ZERO, random.nextFloat() * 360f, 0, 0);
        ParticleShapes.modelOn(sl, crystals.scale(size * 0.45f), target, new Vec3(0, height - 0.08, 0),
                random.nextFloat() * 360f, 0, (random.nextFloat() - 0.5f) * 30f);
        double yawRad = Math.toRadians(facing);
        Vec3 right = new Vec3(-Math.cos(yawRad), 0, -Math.sin(yawRad));
        for (int side = -1; side <= 1; side += 2) {
            Vec3 offset = new Vec3(0, height * 0.62, 0).add(right.scale(side * width * 0.5));
            ParticleShapes.modelOn(sl, crystals.scale(size * 0.38f), target, offset, facing, 0, side * 60f);
        }
    }

    private static void frostBurst(ServerLevel sl, LivingEntity target) {
        ParticleShapes.burst(sl, FROST, target.getBoundingBox().getCenter(), 16, 0.05, 0.2);
        sl.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_CLUSTER_PLACE, SoundSource.PLAYERS, 0.9f, 0.6f);
    }

    private static void shatter(ServerLevel sl, LivingEntity target) {
        Vec3 center = target.getBoundingBox().getCenter();
        int count = Mth.clamp((int) (target.getBbWidth() * target.getBbHeight() * 20), 12, 60);
        ParticleShapes.burst(sl, ICE_SHARD, center, count, 0.12, 0.35);
        ParticleShapes.burst(sl, FROST, center, count / 2, 0.05, 0.2);
        sl.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1f, 1.2f);
        sl.playSound(null, target.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.8f, 0.8f);
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onFreezeLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        CompoundTag tag = freezeTag(entity);
        if (tag == null) return;

        int left = tag.getInt(TICKS) - 1;
        if (left <= 0) {
            end(entity);
            return;
        }
        tag.putInt(TICKS, left);
        hold(entity); // stays stuck even if something else turned its AI back on
    }

    @SubscribeEvent
    public static void onFreezeLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || !isFrozen(entity)) return;
        end(entity);
    }
}
