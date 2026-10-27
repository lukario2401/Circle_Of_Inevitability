package net.lukario.circleofinevitability.fate;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Server-owned material Wheel. Combat uses the displayed tier; paid native casts reroll after activation. */
public final class Fate {
    public static final String PATHWAY = "Snake Of Mercury";
    private static final String TIER = "snake_wheel_tier", NEXT = "snake_of_mercury_next_spin";
    private Fate() {}
    public static boolean applies(LivingEntity entity) {
        return entity instanceof Player player && !player.level().isClientSide()
                && PATHWAY.equals(SoulCore.getPathway(player));
    }
    public static WheelTier outcome(int sequence, int roll) { return WheelTier.values()[FateOdds.outcome(sequence, roll)]; }
    public static void spin(Player player) {
        set(player, outcome(Math.clamp(SoulCore.getSequence(player), 0, 9), player.getRandom().nextInt(100)));
        player.getPersistentData().putInt("snake_wheel_sequence", SoulCore.getSequence(player));
        player.getPersistentData().putLong(NEXT, player.level().getGameTime() + 160);
    }
    public static void set(Player player, WheelTier tier) { player.getPersistentData().putInt(TIER, tier.ordinal()); }
    public static WheelTier peek(Player player) {
        if (!player.getPersistentData().contains(TIER) || player.getPersistentData().getIntOr("snake_wheel_sequence", -1) != SoulCore.getSequence(player)
                || player.getPersistentData().getLongOr(NEXT, 0L) <= player.level().getGameTime()) spin(player);
        return WheelTier.values()[Math.clamp(player.getPersistentData().getIntOr(TIER, 3), 0, 6)];
    }
    public static WheelTier tier(LivingEntity entity) { return applies(entity) ? peek((Player) entity) : WheelTier.GOLD; }
    public static int ticksLeft(Player player) { return (int) Math.max(0, player.getPersistentData().getLongOr(NEXT, 0L) - player.level().getGameTime()); }
    public static void consume(Player player) { spin(player); }
}
