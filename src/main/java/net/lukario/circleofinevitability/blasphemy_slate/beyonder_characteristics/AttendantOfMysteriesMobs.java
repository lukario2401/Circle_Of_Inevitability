package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobRules;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.lukario.circleofinevitability.combat.Countdown;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Substitute;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Attendant Of Mysteries abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 *
 * Its keys are written for any caster (its caster a LivingEntity, its aim the caster's facing, its sneak form read
 * from isShiftKeyDown), so a mob runs the very key a player presses: MobSpells.key turns it to its target, crouches it
 * for the sneak form, and the key checks, pays (out of what the mob's cast covers) and goes off, stacks and echoes of
 * its past self included. AttendantOfMysteriesMobs.Events counts its juggle and projection down as a player's tick.
 *
 *   1  Paper Daggers (9), Wide Fan (9, sneak: close)
 *   2  Flaming Jump (8: to a target far off), Flame Control (8, sneak)
 *   3  Damage Transfer (7), Paper Figurine Substitute (7, sneak)
 *   4  Air Cannon (6), Air Leap (6, sneak: when an enemy is close)
 *   5  Spirit Body Threads (5: its target becomes its marionette), Thread Web (5, sneak)
 *   6  Historical Void (4: a projection of its past self), Fog of History (4, sneak: when hurt)
 *   7  Grafting (3), Miracle (3, sneak)
 */
public final class AttendantOfMysteriesMobs {

    private static final String P = AttendantOfMysteries.PATHWAY;

    private AttendantOfMysteriesMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        // ---- key 1: Paper Daggers
        key(1, false, null, AttendantOfMysteries.DAGGER_COST, 40, AttendantOfMysteries.DAGGER_RANGE, true, null,
                AttendantOfMysteries::attendantOfMysteriesPaperDagger);
        key(1, true, "Wide Fan", AttendantOfMysteries.DAGGER_COST, 80, AttendantOfMysteries.WIDE_FAN_RANGE, true, null,
                AttendantOfMysteries::attendantOfMysteriesPaperDagger);

        // ---- key 2: Flaming Jump / Flame Control
        key(2, false, null, AttendantOfMysteries.FLAME_COST, 200, AttendantOfMysteries.JUMP_NEW_FLAME_RANGE, true,
                (mob, target, sl) -> mob.distanceTo(target) > 8.0, AttendantOfMysteries::attendantOfMysteriesFlamingJump);
        key(2, true, "Flame Control", AttendantOfMysteries.FLAME_COST, AttendantOfMysteries.FLAME_TICKS, AttendantOfMysteries.FLAME_RANGE, true,
                null, AttendantOfMysteries::attendantOfMysteriesFlamingJump);

        // ---- key 3: Damage Transfer / Paper Figurine Substitute
        key(3, false, null, AttendantOfMysteries.TRANSFER_COST, AttendantOfMysteries.TRANSFER_TICKS + 100, AttendantOfMysteries.TRANSFER_RANGE,
                true, null, AttendantOfMysteries::attendantOfMysteriesDamageTransfer);
        key(3, true, "Paper Figurine Substitute", AttendantOfMysteries.TRANSFER_COST, AttendantOfMysteries.SUBSTITUTE_TICKS + 100, 16.0,
                false, (mob, target, sl) -> !Substitute.isArmed(mob), AttendantOfMysteries::attendantOfMysteriesDamageTransfer);

        // ---- key 4: Air Cannon / Air Leap
        key(4, false, null, AttendantOfMysteries.CANNON_COST, 100, AttendantOfMysteries.CANNON_RANGE, true,
                (mob, target, sl) -> mob.distanceTo(target) > AttendantOfMysteries.CANNON_BLAST_RADIUS,
                AttendantOfMysteries::attendantOfMysteriesAirCannon);
        key(4, true, "Air Leap", AttendantOfMysteries.CANNON_COST, 200, AttendantOfMysteries.LEAP_BLAST_RADIUS, false, null,
                AttendantOfMysteries::attendantOfMysteriesAirCannon);

        // ---- key 5: Spirit Body Threads / Thread Web
        key(5, false, null, AttendantOfMysteries.THREADS_COST, AttendantOfMysteries.COUNTDOWN_SECONDS * 20 + 200, AttendantOfMysteries.THREADS_RANGE,
                true, (mob, target, sl) -> !Countdown.isRunning(mob) && Marionette.of(mob).size() < BeyonderMobRules.MAX_SUMMONS,
                AttendantOfMysteries::attendantOfMysteriesCountdown);
        key(5, true, "Thread Web", AttendantOfMysteries.THREADS_COST, AttendantOfMysteries.COUNTDOWN_SECONDS * 20 + 200,
                AttendantOfMysteries.WEB_RADIUS, false, (mob, target, sl) -> !Countdown.isRunning(mob),
                AttendantOfMysteries::attendantOfMysteriesCountdown);

        // ---- key 6: Historical Void / Fog of History
        key(6, false, null, AttendantOfMysteries.HISTORY_COST, AttendantOfMysteries.PROJECTION_TICKS + 200, 16.0, false, null,
                AttendantOfMysteries::attendantOfMysteriesHistoricalVoid);
        key(6, true, "Fog of History", AttendantOfMysteries.HISTORY_COST, AttendantOfMysteries.FOG_TICKS + 400, 16.0, false,
                (mob, target, sl) -> MobSpells.hurt(mob, 0.4f) && !Concealment.isHidden(mob),     // never to step back out
                AttendantOfMysteries::attendantOfMysteriesHistoricalVoid);

        // ---- key 7: Grafting / Miracle
        key(7, false, null, AttendantOfMysteries.GRAFT_COST, AttendantOfMysteries.GRAFT_TICKS + 200, AttendantOfMysteries.GRAFT_RANGE, true,
                null, AttendantOfMysteries::attendantOfMysteriesGrafting);
        key(7, true, "Miracle", AttendantOfMysteries.GRAFT_COST, AttendantOfMysteries.MIRACLE_TICKS, 16.0, false,
                (mob, target, sl) -> MobSpells.hurt(mob, 0.5f), AttendantOfMysteries::attendantOfMysteriesGrafting);
    }

    /** The player's key, run by the mob in one form. */
    private interface PlayerKey {
        void run(net.minecraft.world.entity.LivingEntity caster, net.minecraft.world.level.Level level, ServerLevel sl, boolean bypassClassCheck);
    }

    private static void key(int key, boolean sneak, String name, int cost, int cooldown, double range, boolean sight,
                            MobAbility.When when, PlayerKey cast) {
        MobAbility ability = MobAbility.of(P, key, sneak).cost(cost).cooldown(Math.max(40, cooldown)).range(range)
                .cast(MobSpells.key(sneak, (mob, sl) -> cast.run(mob, sl, sl, true)));
        if (name != null) ability.named(name);
        if (sight) ability.sight();
        if (when != null) ability.when(when);
        MobAbilities.register(ability);
    }

    // =========================
    // Events
    // =========================

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        /** An Attendant mob: its juggle and its projection count down as a player's do. */
        @SubscribeEvent
        public static void onAttendantOfMysteriesMobTick(LivingEvent.LivingTickEvent event) {
            if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel)) return;
            if (!P.equals(Beyonders.pathwayOf(mob))) return;
            AttendantOfMysteries.countDown(mob.getPersistentData(), AttendantOfMysteries.JUGGLE_LEFT);
            AttendantOfMysteries.countDown(mob.getPersistentData(), AttendantOfMysteries.PROJECTION_LEFT);
        }
    }
}
