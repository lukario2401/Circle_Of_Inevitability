package net.lukario.circleofinevitability.item.artifacts.uniqueness;

import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.UUID;

/**
 * A shadow soldier: one of the dead of old wars, clawing its way out of the ground. Uniquenesses raise them.
 *
 * Rank 0 Levy, 1 Legionary, 2 Centurion, 3 Warlord (client/uniqueness/ShadowSoldierModel draws each differently).
 * A hunter goes after its 'owner' (the wielder it was raised against) and nobody else unless struck; its blows are
 * never softened by a uniqueness's protection. A loyal one ('loyal') fights FOR its owner instead. An illusion looks
 * the same but its first blow, or the first blow it takes, unmakes it. Every soldier fades after its lifetime and
 * leaves nothing behind except what it carries (the Usurper of Final Conquest carries the uniqueness).
 */
public class ShadowSoldier extends Monster {
    public static final int RISE_TICKS = 30;
    public static final String SHADOW = "shadow", USURPER = "usurper", LOYAL = "loyal";

    private static final EntityDataAccessor<Integer> RANK = SynchedEntityData.defineId(ShadowSoldier.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> PALETTE = SynchedEntityData.defineId(ShadowSoldier.class, EntityDataSerializers.STRING);

    /** Per rank: health, damage, armor, speed, knockback resistance. */
    private static final double[] HEALTH = {24, 40, 70, 140}, DAMAGE = {5, 7, 10, 15}, ARMOR = {2, 8, 12, 16},
            SPEED = {.27, .28, .3, .31}, STEADY = {0, .2, .4, .6};

    private UUID owner;
    private boolean loyal, illusion;
    private int lifetime = 20 * 90;
    private ItemStack carried = ItemStack.EMPTY;

    public ShadowSoldier(EntityType<? extends ShadowSoldier> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24).add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.MOVEMENT_SPEED, .27).add(Attributes.FOLLOW_RANGE, 48).add(Attributes.ARMOR, 2);
    }

    /**
     * Raises one at 'at' (it climbs out of the ground there over RISE_TICKS). owner: the player it hunts, or serves when
     * loyal. Returns it, already in the world.
     */
    public static ShadowSoldier rise(ServerLevel level, Vec3 at, int rank, String palette, ServerPlayer owner,
                                     boolean loyal, boolean illusion, int lifetimeTicks) {
        ShadowSoldier soldier = new ShadowSoldier(ShadowSoldiers.SOLDIER.get(), level);
        soldier.setRank(rank, palette);
        soldier.owner = owner.getUUID();
        soldier.loyal = loyal;
        soldier.illusion = illusion;
        soldier.lifetime = lifetimeTicks;
        double dx = owner.getX() - at.x, dz = owner.getZ() - at.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        soldier.snapTo(at.x, at.y, at.z, loyal ? owner.getYRot() : yaw, 0);
        soldier.setYHeadRot(soldier.getYRot());
        soldier.yBodyRot = soldier.getYRot();
        level.addFreshEntity(soldier);
        BlockState ground = level.getBlockState(BlockPos.containing(at).below());
        if (!ground.isAir())
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + .1, at.z, 30, .4, .1, .4, .1);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + .2, at.z, 12, .3, .1, .3, .02);
        level.sendParticles(ParticleTypes.SOUL, at.x, at.y + .3, at.z, 6, .3, .2, .3, .02);
        Spells.sound(level, at, SoundEvents.ROOTED_DIRT_BREAK, 1f, .5f);
        Spells.sound(level, at, SoundEvents.WITHER_SKELETON_AMBIENT, .8f, .5f);
        return soldier;
    }

    /** A spot 'min'..'max' blocks from 'center' where one can stand, or null. */
    public static Vec3 spotNear(ServerLevel level, Vec3 center, double min, double max, net.minecraft.util.RandomSource random) {
        for (int attempt = 0; attempt < 16; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2, distance = min + random.nextDouble() * (max - min);
            BlockPos column = BlockPos.containing(center.x + Math.cos(angle) * distance, center.y, center.z + Math.sin(angle) * distance);
            for (int dy = 3; dy >= -4; dy--) {
                BlockPos feet = column.above(dy);
                if (level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), net.minecraft.core.Direction.UP)
                        && level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                        && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty())
                    return Vec3.atBottomCenterOf(feet);
            }
        }
        return null;
    }

    public void setRank(int rank, String palette) {
        rank = Math.clamp(rank, 0, 3);
        entityData.set(RANK, rank);
        entityData.set(PALETTE, palette);
        boolean usurper = USURPER.equals(palette);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(usurper ? 300 : HEALTH[rank]);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(usurper ? 20 : DAMAGE[rank]);
        getAttribute(Attributes.ARMOR).setBaseValue(usurper ? 20 : ARMOR[rank]);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(usurper ? .32 : SPEED[rank]);
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(usurper ? 1 : STEADY[rank]);
        getAttribute(Attributes.SCALE).setBaseValue(usurper ? 1.4 : rank == 3 ? 1.25 : 1);
        setHealth(getMaxHealth());
    }

    public int rank() { return entityData.get(RANK); }
    public String palette() { return entityData.get(PALETTE); }
    public boolean loyal() { return loyal; }
    public boolean illusion() { return illusion; }
    public UUID owner() { return owner; }

    /** It still climbs out of the ground (no moving, no fighting). */
    public boolean rising() { return tickCount < RISE_TICKS; }

    /** Something to drop when it falls (Final Conquest gives the Usurper the uniqueness). */
    public void carry(ItemStack stack) { this.carried = stack.copy(); }

    public @Nullable ServerPlayer ownerPlayer() {
        if (owner == null || !(level() instanceof ServerLevel level)) return null;
        return level.getServer().getPlayerList().getPlayer(owner);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RANK, 0);
        builder.define(PALETTE, SHADOW);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true) {
            @Override public boolean canUse() { return !rising() && super.canUse(); }
        });
        goalSelector.addGoal(5, new FollowOwnerGoal());
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, .8) {
            @Override public boolean canUse() { return !rising() && super.canUse(); }
        });
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, ShadowSoldier.class) {
            @Override public boolean canUse() { return !loyalOwnerHit() && super.canUse(); }
        });
        targetSelector.addGoal(2, new OwnTargetGoal());
    }

    private boolean loyalOwnerHit() {
        return loyal && getLastHurtByMob() != null && getLastHurtByMob().getUUID().equals(owner);
    }

    /** A hunter keeps its eyes on its owner; a loyal one picks whoever its owner fights, or whatever is hostile near them. */
    private final class OwnTargetGoal extends TargetGoal {
        OwnTargetGoal() {
            super(ShadowSoldier.this, false, false);
            setFlags(EnumSet.of(Flag.TARGET));
        }

        private LivingEntity pick() {
            ServerPlayer master = ownerPlayer();
            boolean masterHere = master != null && master.isAlive() && master.level() == level();
            if (USURPER.equals(palette()) && (!masterHere || master.isCreative() || master.isSpectator()))
                return level().getEntitiesOfClass(ServerPlayer.class, getBoundingBox().inflate(32),
                                p -> p.isAlive() && !p.isCreative() && !p.isSpectator())
                        .stream().min(java.util.Comparator.comparingDouble(ShadowSoldier.this::distanceToSqr)).orElse(null);
            if (!masterHere) return null;
            if (!loyal) return master.isCreative() || master.isSpectator() || distanceToSqr(master) > 64 * 64 ? null : master;
            LivingEntity fought = master.getLastHurtMob();
            if (fought != null && fought.isAlive() && fought != ShadowSoldier.this && distanceToSqr(fought) < 32 * 32
                    && !(fought instanceof ShadowSoldier s && s.loyal)) return fought;
            LivingEntity attacker = master.getLastHurtByMob();
            if (attacker != null && attacker.isAlive() && !(attacker instanceof ShadowSoldier s && s.loyal)) return attacker;
            return level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(16),
                            m -> m.isAlive() && m instanceof Enemy && !(m instanceof ShadowSoldier s && s.loyal))
                    .stream().min(java.util.Comparator.comparingDouble(ShadowSoldier.this::distanceToSqr)).orElse(null);
        }

        @Override
        public boolean canUse() {
            if (rising() || tickCount % 10 != 0) return false;
            LivingEntity target = pick();
            if (target == null || target == getTarget()) return false;
            targetMob = target;
            return true;
        }

        @Override
        public void start() {
            setTarget(targetMob);
            super.start();
        }
    }

    /** A loyal soldier with nothing to fight stays near its owner. */
    private final class FollowOwnerGoal extends net.minecraft.world.entity.ai.goal.Goal {
        FollowOwnerGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            ServerPlayer master = loyal ? ownerPlayer() : null;
            return master != null && getTarget() == null && master.level() == level() && distanceToSqr(master) > 36;
        }

        @Override
        public void tick() {
            ServerPlayer master = ownerPlayer();
            if (master != null && getNavigation().isDone()) getNavigation().moveTo(master, 1.2);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) {
            if (random.nextInt(rising() ? 1 : 4) == 0)
                level().addParticle(rising() ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                        getRandomX(.6), getY() + random.nextDouble() * (rising() ? .4 : getBbHeight() * .5), getRandomZ(.6), 0, .02, 0);
            return;
        }
        if (rising()) setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
        if (--lifetime <= 0) {
            unmake();
            return;
        }
        ServerPlayer master = ownerPlayer();
        if (tickCount > 100 && !USURPER.equals(palette()) && (master == null || !master.isAlive() || master.level() != level())) unmake();
    }

    @Override
    public void checkDespawn() {
        // They come and go with their uniqueness: never despawned the usual way (not even in Peaceful).
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (rising()) return false;
        if (illusion) {
            unmake();
            if (target instanceof ServerPlayer player)
                player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("It was never there.")
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            return false;
        }
        if (loyal && target.getUUID().equals(owner)) return false;
        boolean hit = super.doHurtTarget(level, target);
        if (hit && (rank() == 3 || USURPER.equals(palette()))) target.igniteForSeconds(3);
        return hit;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (illusion && source.getEntity() != null) {
            unmake();
            return false;
        }
        if (loyal && source.getEntity() != null && source.getEntity().getUUID().equals(owner)) return false;
        return super.hurtServer(level, source, damage);
    }

    /** It falls apart into smoke and ash. */
    public void unmake() {
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + getBbHeight() / 2, getZ(), 20, .3, getBbHeight() / 3, .3, .02);
            level.sendParticles(ParticleTypes.ASH, getX(), getY() + getBbHeight() / 2, getZ(), 30, .4, getBbHeight() / 3, .4, .02);
            Spells.sound(level, position(), SoundEvents.SOUL_ESCAPE.value(), 1f, .7f);
            if (!carried.isEmpty()) spawnAtLocation(level, carried);
            carried = ItemStack.EMPTY;
        }
        discard();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide()) unmake();
    }

    @Override
    protected boolean shouldDropLoot(ServerLevel level) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_SKELETON_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return .55f + random.nextFloat() * .1f;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("SoldierRank", rank());
        out.putString("SoldierPalette", palette());
        out.putString("SoldierOwner", owner == null ? "" : owner.toString());
        out.putBoolean("SoldierLoyal", loyal);
        out.putBoolean("SoldierIllusion", illusion);
        out.putInt("SoldierLifetime", lifetime);
        if (!carried.isEmpty()) out.store("SoldierCarried", ItemStack.CODEC, carried);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        entityData.set(RANK, in.getIntOr("SoldierRank", 0));
        entityData.set(PALETTE, in.getStringOr("SoldierPalette", SHADOW));
        try {
            String id = in.getStringOr("SoldierOwner", "");
            owner = id.isEmpty() ? null : UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            owner = null;
        }
        loyal = in.getBooleanOr("SoldierLoyal", false);
        illusion = in.getBooleanOr("SoldierIllusion", false);
        lifetime = in.getIntOr("SoldierLifetime", 20 * 60);
        carried = in.read("SoldierCarried", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }
}
