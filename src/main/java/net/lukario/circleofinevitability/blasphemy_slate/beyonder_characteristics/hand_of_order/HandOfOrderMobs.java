package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrder.*;

/**
 * Hand Of Order abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 * The numbers are the player abilities' own (HandOfOrder), so the two never disagree; only the cooldowns are a
 * mob's. To port another ability: one more MobAbilities.register(...) in register(), and its cast method below.
 *
 *   Halt (9):              its target is held for HALT_TICKS (a player HALT_PLAYER_TICKS; Judge 6: longer).
 *   Presence (9, sneak):   a wave of Authority round it: its enemies slowed, non-players weakened and cowed.
 */
public final class HandOfOrderMobs {

    private static final int HALT_MOB_COOLDOWN = 160;          // ticks (a player: docs/HAND_OF_ORDER.md)
    private static final int PRESENCE_MOB_COOLDOWN = 280;

    private HandOfOrderMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        MobAbilities.register(MobAbility.of(PATHWAY, 1, MobAbility.NORMAL)          // Halt
                .cost(HALT_COST).cooldown(HALT_MOB_COOLDOWN)
                .range(HALT_RANGE).sight()
                .when((mob, target, sl) -> !Root.has(target, RootRestriction.MOVE))  // not one already held
                .cast(HandOfOrderMobs::halt));

        MobAbilities.register(MobAbility.of(PATHWAY, 1, MobAbility.SNEAK).named("Presence")
                .cost(PRESENCE_COST).cooldown(PRESENCE_MOB_COOLDOWN)
                .range(PRESENCE_RADIUS)                                               // its target must be in the wave
                .cast(HandOfOrderMobs::presence));
    }

    // =========================
    // Halt
    // =========================

    private static boolean halt(Mob mob, LivingEntity target, ServerLevel sl) {
        boolean judge = MobSpells.hasSequence(mob, JUDGE_SEQUENCE);
        int ticks = target instanceof Player
                ? (judge ? HALT_JUDGE_PLAYER_TICKS : HALT_PLAYER_TICKS)
                : (judge ? HALT_JUDGE_TICKS : HALT_TICKS);
        MobSpells.face(mob, target);
        HandOfOrderFx.halt(sl, mob, target);
        Later.run(sl, HandOfOrderFx.HALT_LANDS, () -> {
            if (!MobSpells.casterStillHere(mob, sl) || !target.isAlive() || target.level() != sl) return;
            Root.apply(target, ticks, RootRestriction.EVERYTHING);
            HandOfOrderFx.halted(sl, target, ticks, "ho_halt_" + target.getId());
        });
        return true;
    }

    // =========================
    // Presence
    // =========================

    private static boolean presence(Mob mob, LivingEntity target, ServerLevel sl) {
        double radius = MobSpells.hasSequence(mob, JUDGE_SEQUENCE) ? PRESENCE_JUDGE_RADIUS : PRESENCE_RADIUS;
        boolean intimidation = MobSpells.hasSequence(mob, INTIMIDATION_SEQUENCE);
        HandOfOrderFx.presence(sl, mob, radius, intimidation);
        for (LivingEntity enemy : MobSpells.enemiesAround(mob, sl, mob.position(), radius)) {
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, PRESENCE_TICKS, 1));
            if (!(enemy instanceof Player)) {                                       // players are only slowed
                enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, PRESENCE_TICKS, 0));
                if (enemy.getMaxHealth() < COWED_MAX_HEALTH) {
                    Root.apply(enemy, COWED_TICKS, RootRestriction.ATTACK);
                    HandOfOrderFx.cowed(sl, enemy, COWED_TICKS, "ho_cowed_" + enemy.getId());
                }
            }
            if (intimidation) Root.apply(enemy, INTIMIDATION_TICKS, RootRestriction.MOVEMENT);
        }
        return true;
    }
}
