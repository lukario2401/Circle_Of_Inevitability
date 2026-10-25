package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobRules;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.lukario.circleofinevitability.combat.Fallen;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Trap;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Bleeding;
import net.lukario.circleofinevitability.status.GiantForm;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Misfortune;
import net.lukario.circleofinevitability.status.NoHeal;
import net.lukario.circleofinevitability.status.Silence;
import net.lukario.circleofinevitability.status.Vulnerable;
import net.lukario.circleofinevitability.status.WeaponCoat;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.Abomination.*;

/**
 * Abomination abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 *
 * Most of them are written for any caster (Abomination's keys after payment and its wheel entries take a
 * LivingEntity, their aim the caster's facing), so a mob casts the same code a player does. Its Restraint, its
 * forms and its curses answer blows through Abomination's own hurt event (which serves this Pathway's mobs too); the
 * parts a player's tick keeps going are kept going here (AbominationMobEvents).
 *
 *   1  Blood Chain (9), Restraint (9, sneak)
 *   2  Moonstruck (8), Lunar Frenzy (8, sneak)
 *   3  Rending Pounce (7), Werewolf Form (7, sneak: the size, strength and claws; no madness)
 *   4  Zombie (6): Raise the Dead, Grave Chill, Undying Flesh, Death Sentence
 *   5  Wraith (5): Mirror Step, Ghost Host
 *   6  Chains (4): Animate Objects, Grave Shackles, Thorned Mausoleum (0), Coffin of Ages (0), Thorn Garden (0)
 *   7  Curses (3): Curse by Connection (on what it fights), Cursed Artifact (under what it fights), Rite of Silence,
 *      Bane Mist (2), Ancient Blight (2), Abomination Form (1)
 *
 * Left out: Possession (a mob has no body to leave).
 */
public final class AbominationMobs {

    private static final int KEY_MOB_COOLDOWN = 200;
    private static final int FORM_MOB_COOLDOWN = 1600;
    private static final String WOLF_COAT = "abomination_mob_wolf_claws";

    private AbominationMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        // ---- key 1: Blood Chain
        MobAbilities.register(MobAbility.of(PATHWAY, 1, MobAbility.NORMAL)
                .cost(CHAIN_COST).cooldown(KEY_MOB_COOLDOWN)
                .range(CHAIN_RANGE).sight()
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    bloodChain(mob, sl, target);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(PATHWAY, 1, MobAbility.SNEAK).named("Restraint")
                .cost(RESTRAINT_COST).cooldown(RESTRAINT_TICKS + KEY_MOB_COOLDOWN)
                .range(16.0)
                .when((mob, target, sl) -> MobSpells.hurt(mob, 0.6f) && !isActive(mob, RESTRAINT_UNTIL))
                .cast((mob, target, sl) -> {
                    restraint(mob, sl);
                    return true;
                }));

        // ---- key 2: Moonstruck
        MobAbilities.register(MobAbility.of(PATHWAY, 2, MobAbility.NORMAL)
                .cost(MOON_COST).cooldown(MOON_TICKS + KEY_MOB_COOLDOWN)
                .range(MOON_RANGE).sight()
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    moonstruck(mob, sl, target);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(PATHWAY, 2, MobAbility.SNEAK).named("Lunar Frenzy")
                .cost(FRENZY_COST).cooldown(FRENZY_TICKS + CRASH_TICKS + KEY_MOB_COOLDOWN)
                .range(8.0)
                .when((mob, target, sl) -> !isActive(mob, FRENZY_UNTIL))
                .cast((mob, target, sl) -> {
                    lunarFrenzy(mob, sl);
                    return true;
                }));

        // ---- key 3: Rending Pounce
        MobAbilities.register(MobAbility.of(PATHWAY, 3, MobAbility.NORMAL)
                .cost(POUNCE_COST).cooldown(KEY_MOB_COOLDOWN)
                .range(POUNCE_RANGE).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > 3.0 && !isActive(mob, POUNCE_UNTIL)
                        && !Root.has(mob, RootRestriction.MOVE))
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    pounce(mob, sl, target);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(PATHWAY, 3, MobAbility.SNEAK).named("Werewolf Form")
                .cost(WOLF_COST).cooldown(Math.max(WOLF_COOLDOWN, WOLF_TICKS + KEY_MOB_COOLDOWN))
                .range(12.0)
                .when((mob, target, sl) -> !GiantForm.isGiant(mob))
                .cast(AbominationMobs::werewolfForm));

        // ---- key 4: the Zombie wheel
        summon(4, "raise_the_dead", "Raise the Dead", AbominationZombie.RAISE_SEQUENCE, AbominationZombie.RAISE_COST,
                AbominationZombie.RAISE_COOLDOWN, AbominationZombie.RAISE_RANGE, AbominationZombie::raiseTheDead,
                (mob, target, sl) -> !Fallen.near(sl, mob.position(), AbominationZombie.FALLEN_RADIUS, 1200).isEmpty());
        entry(4, "grave_chill", "Grave Chill", AbominationZombie.CHILL_SEQUENCE, AbominationZombie.CHILL_COST,
                AbominationZombie.CHILL_COOLDOWN, AbominationZombie.CHILL_RADIUS, AbominationZombie::graveChill, null);
        entry(4, "undying_flesh", "Undying Flesh", AbominationZombie.UNDYING_SEQUENCE, AbominationZombie.UNDYING_COST,
                AbominationZombie.UNDYING_COOLDOWN, 0, AbominationZombie::undyingFlesh,
                (mob, target, sl) -> MobSpells.hurt(mob, 0.6f) && !AbominationZombie.isUndying(mob));
        entry(4, "death_sentence", "Death Sentence", AbominationZombie.SENTENCE_SEQUENCE, AbominationZombie.SENTENCE_COST,
                AbominationZombie.SENTENCE_COOLDOWN, AbominationZombie.SENTENCE_RANGE, AbominationZombie::deathSentence, null);

        // ---- key 5: the Wraith wheel
        entry(5, "mirror_step", "Mirror Step", AbominationWraith.MIRROR_SEQUENCE, AbominationWraith.MIRROR_COST,
                AbominationWraith.MIRROR_COOLDOWN, 24.0, AbominationWraith::mirrorStep, null);
        summon(5, "ghost_host", "Ghost Host", AbominationWraith.GHOSTS_SEQUENCE, AbominationWraith.GHOSTS_COST,
                AbominationWraith.GHOSTS_COOLDOWN, 24.0, AbominationWraith::ghostHost, null);

        // ---- key 6: the Chains wheel
        entry(6, "animate_objects", "Animate Objects", AbominationChains.ANIMATE_SEQUENCE, AbominationChains.ANIMATE_COST,
                AbominationChains.ANIMATE_COOLDOWN, AbominationChains.ANIMATE_RADIUS, AbominationChains::animateObjects, null);
        entry(6, "grave_shackles", "Grave Shackles", AbominationChains.SHACKLES_SEQUENCE, AbominationChains.SHACKLES_COST,
                AbominationChains.SHACKLES_COOLDOWN, AbominationChains.SHACKLES_RADIUS, AbominationChains::graveShackles, null);
        entry(6, "thorned_mausoleum", "Thorned Mausoleum", AbominationChains.MAUSOLEUM_SEQUENCE, AbominationChains.MAUSOLEUM_COST,
                AbominationChains.MAUSOLEUM_COOLDOWN, AbominationChains.MAUSOLEUM_RANGE, AbominationChains::thornedMausoleum, null);
        entry(6, "coffin_of_ages", "Coffin of Ages", AbominationChains.COFFIN_SEQUENCE, AbominationChains.COFFIN_COST,
                AbominationChains.COFFIN_COOLDOWN, AbominationChains.COFFIN_RANGE, AbominationChains::coffinOfAges, null);
        entry(6, "thorn_garden", "Thorn Garden", AbominationChains.GARDEN_SEQUENCE, AbominationChains.GARDEN_COST,
                AbominationChains.GARDEN_COOLDOWN, AbominationChains.GARDEN_RANGE, AbominationChains::thornGarden, null);

        // ---- key 7: the Curses wheel
        MobAbilities.register(MobAbility.of(PATHWAY, 7, MobAbility.NORMAL).entry("curse_by_connection").named("Curse by Connection")
                .sequence(AbominationCurses.CONNECTION_SEQUENCE)
                .cost(AbominationCurses.CONNECTION_COST).cooldown(AbominationCurses.CONNECTION_COOLDOWN)
                .range(AbominationCurses.CONNECTION_SIGHT)
                .when((mob, target, sl) -> Misfortune.level(target) < AbominationCurses.CONNECTION_CURSE)
                .cast(AbominationMobs::curseByConnection));
        MobAbilities.register(MobAbility.of(PATHWAY, 7, MobAbility.NORMAL).entry("cursed_artifact").named("Cursed Artifact")
                .sequence(AbominationCurses.RELIC_SEQUENCE)
                .cost(AbominationCurses.RELIC_COST).cooldown(Math.max(400, AbominationCurses.RELIC_COOLDOWN)).weight(5)
                .range(AbominationCurses.RELIC_RANGE).sight()
                .cast(AbominationMobs::cursedArtifact));
        entry(7, "rite_of_silence", "Rite of Silence", AbominationCurses.RITE_SEQUENCE, AbominationCurses.RITE_COST,
                AbominationCurses.RITE_COOLDOWN, AbominationCurses.RITE_RANGE, AbominationCurses::riteOfSilence, null);
        entry(7, "bane_mist", "Bane Mist", AbominationCurses.MIST_SEQUENCE, AbominationCurses.MIST_COST,
                AbominationCurses.MIST_COOLDOWN, AbominationCurses.MIST_RANGE, AbominationCurses::baneMist, null);
        entry(7, "ancient_blight", "Ancient Blight", AbominationCurses.BLIGHT_SEQUENCE, AbominationCurses.BLIGHT_COST,
                AbominationCurses.BLIGHT_COOLDOWN, AbominationCurses.BLIGHT_RANGE, AbominationCurses::ancientBlight, null);
        entry(7, "abomination_form", "Abomination Form", AbominationCurses.FORM_SEQUENCE, AbominationCurses.FORM_COST,
                Math.max(FORM_MOB_COOLDOWN, AbominationCurses.FORM_COOLDOWN), 16.0, AbominationCurses::abominationForm,
                (mob, target, sl) -> !AbominationCurses.inForm(mob) && !GiantForm.isGiant(mob));
    }

    /** One wheel entry written for any caster. range 0 = on itself (any range). */
    private static void entry(int key, String entry, String name, int sequence, int cost, int cooldown, double range,
                              MobSpells.Entry cast, MobAbility.When when) {
        MobAbility ability = MobAbility.of(PATHWAY, key, MobAbility.NORMAL).entry(entry).named(name)
                .sequence(sequence).cost(cost).cooldown(Math.max(100, cooldown))
                .cast(MobSpells.aimedEntry(cast));
        if (range > 0) ability.range(range).sight();
        else ability.anyRange();
        if (when != null) ability.when(when);
        MobAbilities.register(ability);
    }

    /** One that calls up servants: only while it has fewer than BeyonderMobRules.MAX_SUMMONS. */
    private static void summon(int key, String entry, String name, int sequence, int cost, int cooldown, double range,
                               MobSpells.Entry cast, MobAbility.When when) {
        entry(key, entry, name, sequence, cost, cooldown, range, cast, (mob, target, sl) ->
                Marionette.of(mob).size() < BeyonderMobRules.MAX_SUMMONS && (when == null || when.test(mob, target, sl)));
    }

    // =========================
    // A mob's own versions
    // =========================

    /** Werewolf Form: bigger, Strength II and Speed II, and claws that bleed and stop healing. No madness. */
    private static boolean werewolfForm(Mob mob, LivingEntity target, ServerLevel sl) {
        boolean puppet = MobSpells.hasSequence(mob, WOLF_PUPPET_SEQUENCE);
        double scale = puppet ? WOLF_PUPPET_SCALE : WOLF_SCALE;
        int ticks = WOLF_TICKS;
        String tag = "ab_mob_wolf_" + mob.getId() + "_" + sl.getGameTime();
        AbominationFx.werewolfForm(sl, mob, mob.getBbHeight() * scale, ticks, tag);
        GiantForm.of(scale)
                .reach(WOLF_REACH).stepHeight(WOLF_STEP)
                .lasts(ticks)
                .everySecond((giant, mad) -> {
                    Spells.keepEffect(mob, MobEffects.STRENGTH, WOLF_STRENGTH, 40);
                    Spells.keepEffect(mob, MobEffects.SPEED, WOLF_SPEED, 40);
                })
                .onEnd((giant, why) -> {
                    WeaponCoat.disarm(mob, WOLF_COAT);
                    if (mob.isAlive()) AbominationFx.werewolfFormEnd(sl, mob, tag);
                })
                .grow(mob);
        WeaponCoat.armAny(mob, WOLF_COAT, 10000, ticks, (attacker, struck, amount, level) -> {
            Bleeding.apply(struck, attacker, WOLF_CLAW_BLEED_TICKS, WOLF_CLAW_BLEED);
            NoHeal.apply(struck, WOLF_CLAW_NO_HEAL);
            AbominationFx.werewolfClaw(level, attacker, struck);
            return amount + WOLF_CLAW_DAMAGE;
        });
        Spells.keepEffect(mob, MobEffects.STRENGTH, WOLF_STRENGTH, 40);
        Spells.keepEffect(mob, MobEffects.SPEED, WOLF_SPEED, 40);
        return true;
    }

    /** Curse by Connection on what it fights (a mob keeps no connections: what it fights is its connection). */
    private static boolean curseByConnection(Mob mob, LivingEntity target, ServerLevel sl) {
        boolean bane = MobSpells.hasSequence(mob, AbominationCurses.CONNECTION_BANE_SEQUENCE);
        int ticks = curseTicks(mob, bane ? AbominationCurses.CONNECTION_CURSE_TICKS * 2 : AbominationCurses.CONNECTION_CURSE_TICKS);
        MobSpells.face(mob, target);
        AbominationFx.baneMistOn(sl, target, AbominationCurses.CONNECTION_CURSE);
        Later.run(sl, AbominationFx.CURSE_LANDS, () -> {
            if (!target.isAlive()) return;
            int level = curse(target, mob, AbominationCurses.CONNECTION_CURSE, AbominationCurses.CONNECTION_CURSE, ticks, false);
            Vulnerable.add(target, AbominationCurses.CONNECTION_VULNERABLE, AbominationCurses.CONNECTION_VULNERABLE, ticks);
            if (level > 0) AbominationFx.baneMistOn(sl, target, level);
        });
        return true;
    }

    /** Cursed Artifact: a relic set under what it fights; the first enemy to step on it is cursed and hushed. */
    private static boolean cursedArtifact(Mob mob, LivingEntity target, ServerLevel sl) {
        Vec3 ground = MobSpells.groundUnder(mob, sl, target);
        MobSpells.face(mob, target);
        Trap.at(ground, AbominationCurses.RELIC_RADIUS)
                .armsAfter(AbominationCurses.RELIC_ARMS)
                .lasts(AbominationCurses.RELIC_TICKS)
                .onSpring(victim -> {
                    int ticks = curseTicks(mob, AbominationCurses.RELIC_CURSE_TICKS);
                    AbominationFx.relicTriggered(sl, victim, ground, ticks, "ab_mob_relic_" + victim.getId());
                    curse(victim, mob, AbominationCurses.RELIC_CURSE, AbominationCurses.RELIC_CURSE, ticks, false);
                    Silence.apply(victim, AbominationCurses.RELIC_HUSH);
                })
                .set(mob, sl, "abomination_mob_relic", AbominationCurses.RELIC_MOST);
        return true;
    }

    // =========================
    // What a player's tick keeps going, for this Pathway's mobs
    // =========================

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class AbominationMobEvents {

        private AbominationMobEvents() {}

        @SubscribeEvent
        public static void onAbominationMobTick(LivingEvent.LivingTickEvent event) {
            if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel sl)) return;
            if (mob.tickCount % 5 != 0 || !PATHWAY.equals(Beyonders.pathwayOf(mob))) return;
            if (isActive(mob, RESTRAINT_UNTIL)) {                                  // bound in its own chains
                if (mob.isSprinting()) mob.setSprinting(false);
                if (!MobSpells.hasSequence(mob, RESTRAINT_SILENT_SEQUENCE)) Spells.keepEffect(mob, MobEffects.SLOWNESS, RESTRAINT_SLOW, 30);
            }
            if (isActive(mob, POUNCE_UNTIL)) AbominationFx.pounceTrail(sl, mob);
            if (isActive(mob, FRENZY_UNTIL)) AbominationFx.frenzyTick(sl, mob);
            if (mob.tickCount % 20 == 0 && AbominationZombie.isUndying(mob)) {
                if (mob.getHealth() < mob.getMaxHealth()) mob.heal(mob.getMaxHealth() * AbominationZombie.UNDYING_HEAL);
                AbominationFx.undyingTick(sl, mob);
            }
        }
    }
}
