package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobRules;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Substitute;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Apocalypse abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 *
 * Apocalypse's keys are written for any caster (its caster a LivingEntity, its aim the caster's facing, its sneak
 * form read from isShiftKeyDown), so a mob runs the very key a player presses through MobSpells.key, and each entry of
 * its two wheels as an ability of its own (MobSpells.aimedEntry, then the wheels' own afterword: out of the mirror,
 * one more stack). Its stacks are counted as a player's; the fifth calls down a catastrophe its Sequence reaches, at
 * random. ApocalypseMobs.Events runs its mirror world and charges its shards as a player's tick does.
 *
 *   1  Fatal Step (9: behind its target; its next blow a backstab), Mirror Shard (9, sneak)
 *   2  Black Flame (8), Mirror Substitute (8, sneak)
 *   3  Mirror World (7: into a mirror; out again where it aims, by its target)
 *   4  Silk Weaver (6): Silk Snare, Web Swing, Trip-web (5, both ends), Cocoon (5), Threads of Life (4), Web of Fate
 *      (3, and drawing it in)
 *   5  Plague (5), Curse (5, sneak)
 *   6  Unaging (4: when hurt), Shatter (4, sneak: its shards go off)
 *   7  Catastrophe (3): Earthquake, Hailstorm, Tornado (2), Hopeless Ice Age (2), Black Death (2), Black Flame Rain
 *      (1), Doomsday Sky (1), where its target stands
 */
public final class ApocalypseMobs {

    private static final String P = Apocalypse.PATHWAY;

    private ApocalypseMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        // ---- key 1: Fatal Step / Mirror Shard
        key(1, false, null, Apocalypse.STEP_COST, 120, Apocalypse.STEP_RANGE, true,
                (mob, target, sl) -> mob.distanceTo(target) > 3.0, Apocalypse::apocalypseFatalStep);
        key(1, true, "Mirror Shard", Apocalypse.SHARD_COST, 300, 16.0, false, null, Apocalypse::apocalypseFatalStep);

        // ---- key 2: Black Flame / Mirror Substitute
        key(2, false, null, Apocalypse.FLAME_COST, 100, Apocalypse.FLAME_RANGE, true, null, Apocalypse::apocalypseBlackFlame);
        key(2, true, "Mirror Substitute", Apocalypse.SUBSTITUTE_COST, Apocalypse.SUBSTITUTE_TICKS + 100, 16.0, false,
                (mob, target, sl) -> !Substitute.isArmed(mob), Apocalypse::apocalypseBlackFlame);

        // ---- key 3: Mirror World, and out of it by the target (free)
        key(3, false, null, Apocalypse.MIRROR_COST, Apocalypse.MIRROR_TICKS + 200, Apocalypse.MIRROR_EXIT_RANGE, true,
                (mob, target, sl) -> !Apocalypse.inMirrorWorld(mob) && mob.distanceTo(target) > 8.0,
                Apocalypse::apocalypseMirrorWorld);
        MobAbilities.register(MobAbility.of(P, 3, MobAbility.NORMAL).entry("mirror_exit").named("Mirror World (out)")
                .sequence(7).cooldown(20).weight(30).range(Apocalypse.MIRROR_EXIT_RANGE).sight()
                .when((mob, target, sl) -> Apocalypse.inMirrorWorld(mob))
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    Apocalypse.exitWhereYouAim(mob, sl);
                    return true;
                }));

        // ---- key 4: the Silk Weaver wheel
        entry(4, "snare", "Silk Snare", 6, Apocalypse.SILK_LOW_COST, Apocalypse.SILK_LOW_COOLDOWN, Apocalypse.SNARE_RANGE, true,
                Apocalypse::silkSnare, null);
        entry(4, "swing", "Web Swing", 6, Apocalypse.SILK_LOW_COST, Apocalypse.SILK_LOW_COOLDOWN, Apocalypse.SWING_RANGE, true,
                Apocalypse::webSwing, (mob, target, sl) -> mob.distanceTo(target) > 6.0);
        entry(4, "trip", "Trip-web", 5, Apocalypse.SILK_MEDIUM_COST, Apocalypse.SILK_MEDIUM_COOLDOWN, Apocalypse.TRIP_RANGE, true,
                Apocalypse::tripWeb, (mob, target, sl) -> !Apocalypse.tyingTripWeb(mob));
        entry(4, "trip_tie", "Trip-web (the other end)", 5, 0, 20, Apocalypse.TRIP_RANGE, true,                // free, as a player's
                Apocalypse::tripWeb, (mob, target, sl) -> Apocalypse.tyingTripWeb(mob));
        entry(4, "cocoon", "Cocoon", 5, Apocalypse.SILK_MEDIUM_COST, Apocalypse.SILK_MEDIUM_COOLDOWN, Apocalypse.COCOON_RANGE, true,
                Apocalypse::cocoon, null);
        entry(4, "threads", "Threads of Life", 4, Apocalypse.SILK_HIGH_COST, Apocalypse.SILK_HIGH_COOLDOWN, Apocalypse.THREADS_RANGE, true,
                Apocalypse::threadsOfLife, (mob, target, sl) -> Marionette.of(mob).size() < BeyonderMobRules.MAX_SUMMONS);
        entry(4, "fate", "Web of Fate", 3, Apocalypse.SILK_VERY_HIGH_COST, Apocalypse.SILK_VERY_HIGH_COOLDOWN, Apocalypse.FATE_RANGE, true,
                Apocalypse::webOfFate, (mob, target, sl) -> !Apocalypse.fateHangs(mob));
        entry(4, "fate_draw", "Web of Fate (drawn in)", 3, 0, 60, Apocalypse.FATE_RANGE + 8.0, false,              // free, as a player's
                Apocalypse::webOfFate, (mob, target, sl) -> Apocalypse.fateHangs(mob));

        // ---- key 5: Plague / Curse
        key(5, false, null, Apocalypse.PLAGUE_COST, Apocalypse.PLAGUE_TICKS + 200, Apocalypse.PLAGUE_RANGE, true, null,
                Apocalypse::apocalypsePlague);
        key(5, true, "Curse", Apocalypse.CURSE_COST, Apocalypse.CURSE_TICKS + 100, Apocalypse.CURSE_RANGE, true, null,
                Apocalypse::apocalypsePlague);

        // ---- key 6: Unaging / Shatter
        key(6, false, null, Apocalypse.UNAGING_COST, Apocalypse.UNAGING_TICKS, 16.0, false,
                (mob, target, sl) -> MobSpells.hurt(mob, 0.5f), Apocalypse::apocalypseUnaging);
        key(6, true, "Shatter", Apocalypse.SHATTER_COST, 200, 16.0, false, null, Apocalypse::apocalypseUnaging);

        // ---- key 7: the Catastrophe wheel, where the target stands
        for (Apocalypse.Disaster disaster : Apocalypse.DISASTERS) {
            entry(7, disaster.key(), disaster.name(), disaster.sequence(), disaster.cost(), disaster.cooldown(),
                    Apocalypse.DISASTER_RANGE, true, (mob, sl) -> {
                        disaster.cast().cast(mob, sl, Spells.aimGround(mob, sl, Apocalypse.DISASTER_RANGE), 1f);
                        return AbilityWheel.Result.DONE;
                    }, (mob, target, sl) -> mob.distanceTo(target) > 6.0);              // not on itself
        }
    }

    /** The player's key, run by the mob in one form. */
    private interface PlayerKey {
        void run(LivingEntity caster, Level level, ServerLevel sl, boolean bypassClassCheck);
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

    /** One wheel entry as an ability of its own; once it went off, the wheels' afterword (out of the mirror, a stack). */
    private static void entry(int key, String entry, String name, int sequence, int cost, int cooldown, double range,
                              boolean sight, MobSpells.Entry cast, MobAbility.When when) {
        MobAbility.Cast aimed = MobSpells.aimedEntry(cast);
        MobAbility ability = MobAbility.of(P, key, MobAbility.NORMAL).entry(entry).named(name)
                .sequence(sequence).cost(cost).cooldown(Math.max(20, cooldown)).range(range)
                .cast((mob, target, sl) -> {
                    if (!aimed.run(mob, target, sl)) return false;
                    Apocalypse.leaveMirrorWorld(mob, sl);
                    Apocalypse.addStack(mob, sl);
                    return true;
                });
        if (sight) ability.sight();
        if (when != null) ability.when(when);
        MobAbilities.register(ability);
    }

    // =========================
    // Events
    // =========================

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        /** An Apocalypse mob: its mirror world runs out and its shards charge, as a player's do. */
        @SubscribeEvent
        public static void onApocalypseMobTick(LivingEvent.LivingTickEvent event) {
            if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel sl)) return;
            if (!P.equals(Beyonders.pathwayOf(mob))) return;
            Apocalypse.mirrorWorldTick(mob, sl);
            if (mob.tickCount % 20 == 0) Apocalypse.chargeShards(mob, sl);
        }
    }
}
