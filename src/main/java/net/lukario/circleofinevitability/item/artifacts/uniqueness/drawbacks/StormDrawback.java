package net.lukario.circleofinevitability.item.artifacts.uniqueness.drawbacks;

import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquenessDrawback;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.UniquenessItem;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.Wielder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * The storm follows: from minLevel a thunderstorm gathers over the wielder's world (it is kept up while they carry
 * it). From lightningFrom, lightning strikes near them every few seconds (1 in oneIn each second), and 'hitShare' of
 * those strikes find the wielder (when the sky can see them), for 'damage' that ignores the uniqueness's protection.
 */
public final class StormDrawback implements UniquenessDrawback {
    private final int minLevel, lightningFrom, oneIn;
    private final float hitShare, damage;

    public StormDrawback(int minLevel, int lightningFrom, int oneIn, float hitShare, float damage) {
        this.minLevel = minLevel;
        this.lightningFrom = lightningFrom;
        this.oneIn = oneIn;
        this.hitShare = hitShare;
        this.damage = damage;
    }

    @Override public int minLevel() { return minLevel; }

    @Override
    public Component describe() {
        return Component.literal("The storm follows: at High a thunderstorm gathers over you, at Extreme lightning strikes "
                + "around you, and sometimes strikes you.");
    }

    @Override
    public void second(Wielder w) {
        if (!UniquenessItem.active(this, w)) return;
        var level = w.level();
        if (!level.dimensionType().hasSkyLight() || level.dimensionType().hasCeiling()) return;
        if (!level.isThundering() || level.getGameTime() % 600 == 0) level.getServer().setWeatherParameters(0, 1200, true, true);
        if (w.corruptionLevel() >= lightningFrom && w.player().getRandom().nextInt(Math.max(1, oneIn)) == 0) strike(w);
    }

    /** A strike now: near the wielder, or (hitShare of the time, under the open sky) on them. True if it hit them. */
    public boolean strike(Wielder w) {
        var level = w.level();
        var player = w.player();
        var random = player.getRandom();
        boolean onThem = random.nextFloat() < hitShare && level.canSeeSky(player.blockPosition());
        Vec3 at;
        if (onThem) {
            at = player.position();
        } else {
            double angle = random.nextDouble() * Math.PI * 2, distance = 4 + random.nextDouble() * 8;
            BlockPos column = BlockPos.containing(player.getX() + Math.cos(angle) * distance, player.getY(), player.getZ() + Math.sin(angle) * distance);
            at = Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column));
        }
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) return false;
        bolt.snapTo(at);
        bolt.setVisualOnly(onThem);
        level.addFreshEntity(bolt);
        if (onThem) {
            player.invulnerableTime = 0;
            player.hurtServer(level, w.uniqueness().damage(level), damage);
            player.igniteForSeconds(3);
            player.sendOverlayMessage(Component.literal("The storm remembers you.").withStyle(ChatFormatting.GOLD));
        }
        return onThem;
    }
}
