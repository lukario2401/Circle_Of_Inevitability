package net.lukario.circleofinevitability.fate;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/** Server-owned, previewed outcome. Reading the wheel never rolls it. Only paid casts consume it. */
public final class Fate {
    private static final String OMEN = "snake_of_mercury_omen";
    private static final String NEXT = "snake_of_mercury_next_spin";
    public enum Omen {
        MISFORTUNE("Misfortune", 0xE75060, .75f), EVEN("Even", 0xC9D3E0, 1f), FORTUNE("Fortune", 0xF4CE62, 1.25f);
        public final String label;
        public final int color;
        public final float power;
        Omen(String label, int color, float power) { this.label = label; this.color = color; this.power = power; }
    }
    private Fate() {}
    public static Omen outcome(int sequence, int roll) {
        return Omen.values()[FateOdds.outcome(sequence, roll)];
    }
    public static void spin(Player player) {
        Omen next = outcome(SoulCore.getSequence(player), player.getRandom().nextInt(100));
        player.getPersistentData().putInt(OMEN, next.ordinal());
        player.getPersistentData().putInt("snake_wheel_sequence", SoulCore.getSequence(player));
        player.getPersistentData().putLong(NEXT, player.level().getGameTime() + 160);
    }
    public static Omen peek(Player player) {
        if (player.getPersistentData().getIntOr("snake_wheel_sequence", -1) != SoulCore.getSequence(player) || player.getPersistentData().getLongOr(NEXT, 0L) <= player.level().getGameTime()) spin(player);
        int value = player.getPersistentData().getIntOr(OMEN, 1);
        return Omen.values()[Math.clamp(value, 0, 2)];
    }
    public static int ticksLeft(Player player) {
        return (int) Math.max(0, player.getPersistentData().getLongOr(NEXT, 0L) - player.level().getGameTime());
    }
    /** Call after payment and successful activation; refund actual expenditure, not the nominal price. */
    public static void consume(Player player, Omen omen, float spent) {
        if (omen == Omen.FORTUNE) Spells.giveSpirituality(player, Math.max(0, spent) * .5f);
        if (omen == Omen.MISFORTUNE) player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
        spin(player);
    }
}
