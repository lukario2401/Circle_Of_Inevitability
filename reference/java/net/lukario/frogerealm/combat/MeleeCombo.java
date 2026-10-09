package net.lukario.frogerealm.combat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * A melee combo for one aspect: hit 1 does this, hit 2 does that, hit 3 does one of these...
 * then it starts over. Stop hitting for a moment and it also starts over.
 *
 *   public static final MeleeCombo COMBO = MeleeCombo.forAspect("Hand Of Order")
 *           .step(MyClass::firstHit)                                   // hit 1
 *           .step(MyClass::secondHit)                                  // hit 2
 *           .randomStep(MyClass::finisherA, MyClass::finisherB);       // hit 3 = one of these at random
 *
 * then add one line to MeleeCombos:  register(MyClass.COMBO);
 *
 * Each step gets (player, target, level) and can do anything: slash trails, sounds, extra damage,
 * effects, roots... It runs when the hit happens, on the server.
 */
public class MeleeCombo {

    @FunctionalInterface
    public interface Hit {
        void run(ServerPlayer player, LivingEntity target, ServerLevel level);
    }

    private static final String STEP_KEY = "forgerealm_combo_step";
    private static final String LAST_HIT_KEY = "forgerealm_combo_last_hit";
    private static final String ASPECT_KEY = "forgerealm_combo_aspect";

    private final String aspect;
    private int resetTicks = 40;
    private float minCharge = 0.8f;
    private final List<List<Hit>> steps = new ArrayList<>();

    private MeleeCombo(String aspect) {
        this.aspect = aspect;
    }

    /** The combo only works for players with this aspect (exact name used by /soul setAspect). */
    public static MeleeCombo forAspect(String aspect) {
        return new MeleeCombo(aspect);
    }

    /** Ticks without hitting before the combo starts over at hit 1 (default 40 = 2 seconds). */
    public MeleeCombo resetAfter(int ticks) {
        this.resetTicks = ticks;
        return this;
    }

    /**
     * How charged the attack must be to count (0..1, default 0.8). Weaker spam-click hits are ignored
     * (they don't advance or reset the combo). 0 = every click counts.
     */
    public MeleeCombo minCharge(float charge) {
        this.minCharge = charge;
        return this;
    }

    /** The next hit in the chain. */
    public MeleeCombo step(Hit hit) {
        steps.add(List.of(hit));
        return this;
    }

    /** The next hit in the chain picks one of these at random. */
    public MeleeCombo randomStep(Hit... options) {
        if (options.length > 0) steps.add(List.of(options));
        return this;
    }

    public String aspect() {
        return aspect;
    }

    /** Which hit (0 = first) the player's NEXT attack would be. */
    public int nextStep(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        boolean expired = player.serverLevel().getGameTime() - data.getLong(LAST_HIT_KEY) > resetTicks
                || !aspect.equals(data.getString(ASPECT_KEY));
        int step = data.getInt(STEP_KEY);
        return expired || step >= steps.size() ? 0 : step;
    }

    /** Called by MeleeCombos when the player lands a melee hit. */
    void onHit(ServerPlayer player, LivingEntity target) {
        if (steps.isEmpty()) return;
        if (player.getAttackStrengthScale(0.5f) < minCharge) return;

        int step = nextStep(player);
        CompoundTag data = player.getPersistentData();
        data.putInt(STEP_KEY, (step + 1) % steps.size());
        data.putLong(LAST_HIT_KEY, player.serverLevel().getGameTime());
        data.putString(ASPECT_KEY, aspect);

        List<Hit> options = steps.get(step);
        Hit hit = options.get(player.getRandom().nextInt(options.size()));
        hit.run(player, target, player.serverLevel());
    }
}
