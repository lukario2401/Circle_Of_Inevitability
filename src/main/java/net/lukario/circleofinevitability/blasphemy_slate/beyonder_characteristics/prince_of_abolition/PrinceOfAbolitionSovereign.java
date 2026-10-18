package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionMarks.Kind;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.Bleeding;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Prince Of Abolition, key 7: the Sovereign wheel (Baron of Corruption, Sequence 6). docs/PRINCE_OF_ABOLITION.md,
 * "Key 7 wheel: Sovereign".
 *
 *   Ruin   (6) a black-and-gold beam along a line: every enemy on it is struck and Corroded (a direct hit)
 *
 * Frenzy (3), Entropy (2), Abolition and Exile (1), Inversion, Coronation and Decree (0) are the next half of the
 * Pathway; their pictures, sounds and looks are ready (WHEEL_FRENZY ... WHEEL_DECREE, and their displays, in
 * PrinceOfAbolitionFx).
 *
 * Sneak opens the wheel; a normal press casts the last pick again (PrinceOfAbolition.recastCost).
 */
public final class PrinceOfAbolitionSovereign {

    private PrinceOfAbolitionSovereign() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks; base values before Might)
    // =========================

    static final int SOVEREIGN_SEQUENCE = 6;                    // the key

    // Ruin
    private static final int RUIN_SEQUENCE = 6;
    private static final int RUIN_COST = 2800;
    private static final int RUIN_COOLDOWN = 200;
    private static final double RUIN_RANGE = 24.0;
    private static final double RUIN_WIDTH = 0.6;               // how far from the line a body is still struck
    private static final float RUIN_DAMAGE = 70f;
    private static final int CORRODED_TICKS = 120;              // Corroded this long:
    private static final float CORRODED_BURN = 5f;              // this much a second (status/Bleeding)...
    static final float RUIN_HEAL_CUT = 0.5f;                    // ...and half of its healing does not take

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("poa_sovereign", "Sovereign")
            .radius(80)
            .quickCastCooldown(false).quickCastCostBy(PrinceOfAbolition::recastCost)
            .noCooldownsWhen(PrinceOfAbolition::noCooldowns)
            .entry("ruin", PrinceOfAbolitionFx.WHEEL_RUIN, "Ruin", RUIN_SEQUENCE,
                    RUIN_COST, RUIN_COOLDOWN, PrinceOfAbolitionSovereign::ruin);

    // =========================
    // Ability 7 - Sovereign
    // Sneak: the Sovereign wheel. Normal: the last pick again, without the wheel.
    // =========================
    public static void princeOfAbolitionSovereign(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!PrinceOfAbolition.ready(player, bypassClassCheck, SOVEREIGN_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.open(player);
        else WHEEL.quickCast(player);
    }

    // =========================
    // Ruin (a direct hit): black and gold gather in your hand, then a beam leaves it along a RUIN_RANGE line (it stops
    // at the first wall) at PrinceOfAbolitionFx.RUIN_FIRES: RUIN_DAMAGE to every enemy on it, hitscan. Whatever it hits
    // is Corroded for CORRODED_TICKS: CORRODED_BURN a second and RUIN_HEAL_CUT of its healing lost.
    // =========================
    private static AbilityWheel.Result ruin(ServerPlayer player, ServerLevel sl) {
        double range = PrinceOfAbolition.range(player, RUIN_RANGE);
        Vec3 end = Spells.aimPoint(player, sl, range);
        double width = PrinceOfAbolition.area(player, RUIN_WIDTH);
        int corroded = PrinceOfAbolition.duration(player, CORRODED_TICKS);
        PrinceOfAbolitionFx.ruin(sl, player, end);
        Later.run(sl, PrinceOfAbolitionFx.RUIN_FIRES, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            Vec3 from = player.getEyePosition();
            for (LivingEntity victim : Spells.enemiesOnLine(player, sl, from, end, width)) {
                Spells.strike(player, victim, RUIN_DAMAGE);
                PrinceOfAbolitionFx.ruinHit(sl, victim);
                if (!victim.isAlive()) continue;
                Bleeding.apply(victim, player, corroded, CORRODED_BURN);
                int lasts = PrinceOfAbolitionMarks.put(player, victim, Kind.CORRODED, corroded);
                if (lasts > 0) PrinceOfAbolitionFx.corroded(sl, victim, lasts, PrinceOfAbolitionMarks.tag(victim, Kind.CORRODED));
            }
        });
        return AbilityWheel.Result.DONE;
    }
}
