package net.lukario.circleofinevitability.mount;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class RidingMount extends PathfinderMob {
    private final Mounts.Gait gait;
    private boolean calledByRider;

    RidingMount(EntityType<RidingMount> type, Level level, Mounts.Gait gait) {
        super(type, level);
        this.gait = gait;
    }

    /** Shared entry point for whistles and abilities. A successful call starts the ride immediately. */
    public static boolean summon(Player rider, Mounts.Breed breed) {
        return summon(rider, breed.type().get());
    }

    static boolean summon(Player rider, EntityType<RidingMount> type) {
        if (!(rider.level() instanceof ServerLevel level) || rider.isPassenger() || !rider.isAlive() || rider.isSpectator()) return false;
        RidingMount mount = type.create(level, EntitySpawnReason.TRIGGERED);
        if (mount == null) return false;
        mount.snapTo(rider.position(), rider.getYRot(), 0);
        mount.setYBodyRot(rider.getYRot());
        mount.setYHeadRot(rider.getYRot());
        Vec3 seated = mount.getPassengerRidingPosition(rider).subtract(rider.getVehicleAttachmentPoint(mount));
        AABB riderBox = rider.getDimensions(Pose.STANDING).makeBoundingBox(seated);
        if (!level.getWorldBorder().isWithinBounds(mount.getBoundingBox())
                || !level.noCollision(rider, mount.getBoundingBox()) || !level.noCollision(rider, riderBox)) return false;

        mount.calledByRider = true;
        if (!level.addFreshEntity(mount)) return false;
        if (!rider.startRiding(mount)) {
            mount.discard();
            return false;
        }
        mount.positionRider(rider);
        return true;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (calledByRider || isVehicle() || player.isPassenger() || player.isSecondaryUseActive()) return InteractionResult.PASS;
        return level().isClientSide() || player.startRiding(this) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    @Override
    public @Nullable Player getControllingPassenger() {
        return getFirstPassenger() instanceof Player player ? player : null;
    }

    @Override
    protected Vec3 getRiddenInput(Player rider, Vec3 selfInput) {
        double forward = rider.zza < 0 ? rider.zza * 0.45 : rider.zza;
        if (!gait.flies()) return new Vec3(rider.xxa * 0.65, 0, forward);

        double pitch = Math.toRadians(rider.getXRot());
        double lift = (rider.isJumping() ? 0.8 : 0) - Mth.sin(pitch) * forward;
        return new Vec3(rider.xxa * 0.75, lift, Mth.cos(pitch) * forward);
    }

    @Override
    protected void tickRidden(Player rider, Vec3 input) {
        super.tickRidden(rider, input);
        setRot(rider.getYRot(), rider.getXRot() * 0.35F);
        yBodyRot = yHeadRot = getYRot();
        if (!gait.flies() && onGround() && rider.isJumping() && canSimulateMovement()) jumpFromGround();
    }

    @Override
    protected float getRiddenSpeed(Player rider) {
        return (float)getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    @Override
    public boolean isFlyingVehicle() {
        return gait.flies() && getControllingPassenger() != null;
    }

    @Override
    public void travel(Vec3 input) {
        if (isFlyingVehicle()) {
            travelFlying(input, (float)getAttributeValue(Attributes.FLYING_SPEED));
        } else {
            super.travel(input);
        }
    }

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return !isFlyingVehicle() && super.causeFallDamage(distance, multiplier, source);
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        if (onGround()) {
            for (int side : new int[]{1, -1}) {
                Vec3 beside = position().add(getCollisionHorizontalEscapeVector(
                        getBbWidth(), passenger.getBbWidth(), getYRot() + side * 90));
                Vec3 safe = DismountHelper.findSafeDismountLocation(passenger.getType(), level(), BlockPos.containing(beside), true);
                if (safe != null) return safe;
            }
        }
        // The whistle mount disappears when the rider leaves, so its footprint is also free.
        return calledByRider ? position() : super.getDismountLocationForPassenger(passenger);
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) { return false; }

    @Override
    public void tick() {
        if (calledByRider && !level().isClientSide() && !isVehicle()) discard();
        else super.tick();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("CalledByRider", calledByRider);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        calledByRider = input.getBooleanOr("CalledByRider", false);
    }
}
