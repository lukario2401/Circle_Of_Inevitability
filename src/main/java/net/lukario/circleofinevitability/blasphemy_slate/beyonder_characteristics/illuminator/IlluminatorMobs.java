package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.combat.History;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Appraisal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * Illuminator abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 *
 * The Illuminator's abilities are written for any caster (its caster a LivingEntity, its aim the caster's facing), so
 * a mob casts the code a player does: the wheel entries through MobSpells.aimedEntry, the keys' parts (throwFlask,
 * survey, ritualCircle, appraise, flawGives) once the mob has paid. Its Appraisal marks are its own (status/Appraisal
 * takes any caster), its dolls and golems are status/Construct's for it, and Recall steps back through combat/History,
 * which keeps a record for an Illuminator mob (IlluminatorMobs.Events). A mob pays what the ability costs: Quick
 * Study and the Rite are a player's discounts. Casting anything ends its Masterwork, as for a player.
 *
 *   1  Compound Flask (9; a second flask from 6), Recall (9, sneak: itself, when hurt)
 *   2  Survey (8), Ritual Circle (8, sneak)
 *   3  Appraise (7), Exploit Flaw (7, sneak: on what it Appraised; every marked enemy near from 1)
 *   4  Workshop (6): Clockwork Rifle, Spirit Cannon, Mystic Brooch
 *   5  Astronomy (5): Star of Curses, Blessing of Stars, False Constellations, Meteor Shower
 *   6  Alchemy and Arcana (4): Living Doll, Golem, Mechanical Body (no jetpack), Dissociation (3), Knowledge
 *      Spirit (3); the white steam (2) while it wears the body, when hurt
 *   7  Civilization (2): Rewrite Principles, Colossus and its Fusion Beam, Scrolls of Civilization (1), Mathematical
 *      Maze (1), Masterwork (1), Reality Knowledge (0), Illusory Book (0), Mechanical Heaven (0)
 *
 * Left out: Recall of an ally (1: it sends back a player ally; a mob steps back itself).
 */
public final class IlluminatorMobs {

    // ---------- mob cooldowns (ticks): a player's keys have none (Illuminator.KEY_COOLDOWNS)
    private static final int FLASK_MOB_COOLDOWN = 80;
    private static final int RECALL_MOB_COOLDOWN = 400;
    private static final int SURVEY_MOB_COOLDOWN = 400;
    private static final int RITUAL_MOB_COOLDOWN = 600;
    private static final int APPRAISE_MOB_COOLDOWN = 160;
    private static final int EXPLOIT_MOB_COOLDOWN = 120;
    private static final int STEAM_MOB_COOLDOWN = 400;
    private static final float RECALL_MOB_HURT = 0.5f;          // it steps back below this much of its health...
    private static final float RECALL_MOB_GAIN = 0.15f;         // ...to a moment it had this much more of its health
    private static final float STEAM_MOB_HURT = 0.4f;

    private IlluminatorMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        String P = Illuminator.PATHWAY;

        // ---- key 1: Compound Flask / Recall
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.NORMAL)
                .cost(Illuminator.FLASK_COST).cooldown(FLASK_MOB_COOLDOWN).weight(14)
                .range(Illuminator.FLASK_RANGE).sight()
                .cast(cast("compound_flask", (mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    Illuminator.throwFlask(mob, sl);
                    if (Beyonder.hasSequence(mob, Illuminator.TWIN_FLASK_SEQUENCE)) {
                        Later.run(sl, Illuminator.TWIN_FLASK_DELAY, () -> {
                            if (!MobSpells.casterStillHere(mob, sl)) return;
                            if (target.isAlive()) MobSpells.face(mob, target);
                            Illuminator.throwFlask(mob, sl);
                        });
                    }
                    return true;
                })));
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.SNEAK).named("Recall")
                .cost(Illuminator.RECALL_COST).cooldown(RECALL_MOB_COOLDOWN).anyRange()
                .when((mob, target, sl) -> mob.getHealth() < mob.getMaxHealth() * RECALL_MOB_HURT && recallTo(mob) != null)
                .cast(cast("recall", IlluminatorMobs::recall)));

        // ---- key 2: Survey / Ritual Circle
        MobAbilities.register(MobAbility.of(P, 2, MobAbility.NORMAL)
                .cost(Illuminator.SURVEY_COST).cooldown(SURVEY_MOB_COOLDOWN).weight(5)
                .range(Illuminator.SURVEY_REACH)
                .when((mob, target, sl) -> !target.hasEffect(MobEffects.GLOWING))
                .cast(cast("survey", MobSpells.aimedCore(Illuminator::survey))));
        MobAbilities.register(MobAbility.of(P, 2, MobAbility.SNEAK).named("Ritual Circle")
                .cost(Illuminator.RITUAL_COST).cooldown(Math.max(RITUAL_MOB_COOLDOWN, Illuminator.RITUAL_TICKS + 100))
                .range(12.0)
                .when((mob, target, sl) -> !Zone.isOpen(mob, Illuminator.RITUAL))
                .cast(cast("ritual_circle", MobSpells.aimedCore(Illuminator::ritualCircle))));

        // ---- key 3: Appraise / Exploit Flaw
        MobAbilities.register(MobAbility.of(P, 3, MobAbility.NORMAL)
                .cost(Illuminator.APPRAISE_COST).cooldown(APPRAISE_MOB_COOLDOWN).weight(14)
                .range(Illuminator.APPRAISE_RANGE).sight()
                .when((mob, target, sl) -> !Appraisal.isMarked(mob, target))
                .cast(cast("appraise", (mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    Illuminator.appraise(mob, sl, target);
                    return true;
                })));
        MobAbilities.register(MobAbility.of(P, 3, MobAbility.SNEAK).named("Exploit Flaw")
                .cost(Illuminator.EXPLOIT_COST).cooldown(EXPLOIT_MOB_COOLDOWN).weight(16)
                .range(Illuminator.EXPLOIT_RANGE).sight()
                .when((mob, target, sl) -> Appraisal.isMarked(mob, target))
                .cast(cast("exploit_flaw", IlluminatorMobs::exploitFlaw)));

        // ---- key 4: the Workshop wheel
        entry(4, "clockwork_rifle", "Clockwork Rifle", IlluminatorWorkshop.RIFLE_SEQUENCE, IlluminatorWorkshop.RIFLE_COST,
                IlluminatorWorkshop.RIFLE_COOLDOWN, IlluminatorWorkshop.RIFLE_RANGE, true, IlluminatorWorkshop::clockworkRifle, null);
        entry(4, "spirit_cannon", "Spirit Cannon", IlluminatorWorkshop.CANNON_SEQUENCE, IlluminatorWorkshop.CANNON_COST,
                Math.max(IlluminatorWorkshop.CANNON_COOLDOWN, IlluminatorWorkshop.CANNON_TICKS), IlluminatorWorkshop.CANNON_REACH,
                false, IlluminatorWorkshop::spiritCannon, null);
        entry(4, "mystic_brooch", "Mystic Brooch", IlluminatorWorkshop.BROOCH_SEQUENCE, IlluminatorWorkshop.BROOCH_COST,
                IlluminatorWorkshop.BROOCH_COOLDOWN, 16.0, false, IlluminatorWorkshop::mysticBrooch, null);

        // ---- key 5: the Astronomy wheel
        entry(5, "star_of_curses", "Star of Curses", IlluminatorAstronomy.CURSE_SEQUENCE, IlluminatorAstronomy.CURSE_COST,
                IlluminatorAstronomy.CURSE_COOLDOWN, IlluminatorAstronomy.CURSE_RANGE, true, IlluminatorAstronomy::starOfCurses, null);
        entry(5, "blessing_of_stars", "Blessing of Stars", IlluminatorAstronomy.BLESSING_SEQUENCE, IlluminatorAstronomy.BLESSING_COST,
                IlluminatorAstronomy.BLESSING_COOLDOWN, IlluminatorAstronomy.BLESSING_RANGE, true, IlluminatorAstronomy::blessingOfStars, null);
        entry(5, "false_constellations", "False Constellations", IlluminatorAstronomy.SKY_SEQUENCE, IlluminatorAstronomy.SKY_COST,
                Math.max(IlluminatorAstronomy.SKY_COOLDOWN, IlluminatorAstronomy.SKY_TICKS), IlluminatorAstronomy.SKY_RANGE, false,
                IlluminatorAstronomy::falseConstellations, null);
        entry(5, "meteor_shower", "Meteor Shower", IlluminatorAstronomy.METEOR_SEQUENCE, IlluminatorAstronomy.METEOR_COST,
                IlluminatorAstronomy.METEOR_COOLDOWN, IlluminatorAstronomy.METEOR_RANGE, true, IlluminatorAstronomy::meteorShower, null);

        // ---- key 6: the Alchemy and Arcana wheel
        entry(6, "living_doll", "Living Doll", IlluminatorAlchemy.DOLL_SEQUENCE, IlluminatorAlchemy.DOLL_COST,
                Math.max(IlluminatorAlchemy.DOLL_COOLDOWN, IlluminatorAlchemy.DOLL_TICKS), 16.0, false, IlluminatorAlchemy::livingDoll, null);
        entry(6, "golem", "Golem", IlluminatorAlchemy.GOLEM_SEQUENCE, IlluminatorAlchemy.GOLEM_COST,
                IlluminatorAlchemy.GOLEM_COOLDOWN, 16.0, false, IlluminatorAlchemy::golem, null);
        entry(6, "mechanical_body", "Mechanical Body", IlluminatorAlchemy.MECH_SEQUENCE, IlluminatorAlchemy.MECH_COST,
                IlluminatorAlchemy.MECH_COOLDOWN, 16.0, false, IlluminatorAlchemy::mechanicalBody,
                (mob, target, sl) -> !Illuminator.wearsMechanicalBody(mob));                // never to take it off
        MobAbilities.register(MobAbility.of(P, 6, MobAbility.NORMAL).entry("steam_form").named("White Steam")
                .sequence(IlluminatorAlchemy.STEAM_SEQUENCE).cooldown(STEAM_MOB_COOLDOWN)       // free, as a player's
                .range(IlluminatorAlchemy.STEAM_RANGE)
                .when((mob, target, sl) -> Illuminator.wearsMechanicalBody(mob) && !Illuminator.isActive(mob, IlluminatorAlchemy.STEAM_UNTIL)
                        && mob.getHealth() < mob.getMaxHealth() * STEAM_MOB_HURT)
                .cast(cast("mechanical_body", MobSpells.aimedCore(IlluminatorAlchemy::steamForm))));
        entry(6, "dissociation", "Dissociation", IlluminatorAlchemy.DISSOCIATION_SEQUENCE, IlluminatorAlchemy.DISSOCIATION_COST,
                IlluminatorAlchemy.DISSOCIATION_COOLDOWN, IlluminatorAlchemy.DISSOCIATION_RANGE, true, IlluminatorAlchemy::dissociation, null);
        entry(6, "knowledge_spirit", "Knowledge Spirit", IlluminatorAlchemy.SPIRIT_SEQUENCE, IlluminatorAlchemy.SPIRIT_COST,
                IlluminatorAlchemy.SPIRIT_COOLDOWN, IlluminatorAlchemy.SPIRIT_RANGE, true, IlluminatorAlchemy::knowledgeSpirit, null);

        // ---- key 7: the Civilization wheel
        entry(7, "rewrite_principles", "Rewrite Principles", IlluminatorCivilization.PRINCIPLES_SEQUENCE, IlluminatorCivilization.PRINCIPLES_COST,
                IlluminatorCivilization.PRINCIPLES_COOLDOWN, IlluminatorCivilization.PRINCIPLES_RANGE, true,
                IlluminatorCivilization::rewritePrinciples, null);
        entry(7, "colossus", "Colossus", IlluminatorCivilization.COLOSSUS_SEQUENCE, IlluminatorCivilization.COLOSSUS_COST,
                IlluminatorCivilization.COLOSSUS_COOLDOWN, 16.0, false, IlluminatorCivilization::colossus,
                (mob, target, sl) -> !IlluminatorCivilization.colossusStands(mob));
        MobAbilities.register(MobAbility.of(P, 7, MobAbility.NORMAL).entry("fusion_beam").named("Fusion Beam")
                .sequence(IlluminatorCivilization.COLOSSUS_SEQUENCE).cooldown(IlluminatorCivilization.FUSION_COOLDOWN)   // free
                .range(IlluminatorCivilization.FUSION_RANGE).sight().weight(20)
                .when((mob, target, sl) -> IlluminatorCivilization.colossusStands(mob))
                .cast(cast("colossus", MobSpells.aimedCore(IlluminatorCivilization::fireFusionBeam))));
        entry(7, "scrolls_of_civilization", "Scrolls of Civilization", IlluminatorCivilization.SCROLLS_SEQUENCE,
                IlluminatorCivilization.SCROLLS_COST, Math.max(IlluminatorCivilization.SCROLLS_COOLDOWN, IlluminatorCivilization.SCROLLS_TICKS),
                IlluminatorCivilization.SCROLLS_RADIUS + 10.0, false, IlluminatorCivilization::scrollsOfCivilization, null);
        entry(7, "mathematical_maze", "Mathematical Maze", IlluminatorCivilization.MAZE_SEQUENCE, IlluminatorCivilization.MAZE_COST,
                IlluminatorCivilization.MAZE_COOLDOWN, IlluminatorCivilization.MAZE_RANGE, true, IlluminatorCivilization::mathematicalMaze, null);
        entry(7, "masterwork", "Masterwork", IlluminatorCivilization.MASTERWORK_SEQUENCE, IlluminatorCivilization.MASTERWORK_COST,
                IlluminatorCivilization.MASTERWORK_COOLDOWN, IlluminatorCivilization.MASTERWORK_RADIUS, false, IlluminatorCivilization::masterwork,
                (mob, target, sl) -> !Zone.isOpen(mob, IlluminatorCivilization.MASTERWORK));
        entry(7, "reality_knowledge", "Reality Knowledge", IlluminatorCivilization.REALITY_SEQUENCE, IlluminatorCivilization.REALITY_COST,
                IlluminatorCivilization.REALITY_COOLDOWN, IlluminatorCivilization.REALITY_RANGE, true, IlluminatorCivilization::realityKnowledge, null);
        entry(7, "illusory_book", "Illusory Book", IlluminatorCivilization.BOOK_SEQUENCE, IlluminatorCivilization.BOOK_COST,
                IlluminatorCivilization.BOOK_COOLDOWN, IlluminatorCivilization.BOOK_RANGE, true, IlluminatorCivilization::illusoryBook, null);
        entry(7, "mechanical_heaven", "Mechanical Heaven", IlluminatorCivilization.HEAVEN_SEQUENCE, IlluminatorCivilization.HEAVEN_COST,
                Math.max(IlluminatorCivilization.HEAVEN_COOLDOWN, IlluminatorCivilization.HEAVEN_TICKS + 600), 32.0, false,
                IlluminatorCivilization::mechanicalHeaven,
                (mob, target, sl) -> !Zone.isOpen(mob, IlluminatorCivilization.HEAVEN));       // never to end it
    }

    /** One wheel entry as an ability of its own (aimed at its target; a refusal spends nothing). */
    private static void entry(int key, String entry, String name, int sequence, int cost, int cooldown, double range,
                              boolean sight, MobSpells.Entry cast, MobAbility.When when) {
        MobAbility ability = MobAbility.of(Illuminator.PATHWAY, key, MobAbility.NORMAL).entry(entry).named(name)
                .sequence(sequence).cost(cost).cooldown(Math.max(100, cooldown))
                .range(range).cast(cast(entry, MobSpells.aimedEntry(cast)));
        if (sight) ability.sight();
        if (when != null) ability.when(when);
        MobAbilities.register(ability);
    }

    /** A cast that went off ends the mob's Masterwork, as anything else a player casts ends theirs. */
    private static MobAbility.Cast cast(String name, MobAbility.Cast cast) {
        return (mob, target, sl) -> {
            if (!cast.run(mob, target, sl)) return false;
            IlluminatorCivilization.castSomething(mob, name);
            return true;
        };
    }

    // =========================
    // A mob's own versions
    // =========================

    /** How far back its Recall goes (as a player's: further from Astronomer). */
    private static int recallTicks(LivingEntity mob) {
        return Beyonder.hasSequence(mob, Illuminator.RECALL_FAR_SEQUENCE) ? Illuminator.RECALL_FAR_TICKS : Illuminator.RECALL_TICKS;
    }

    /** The moment it would step back to: one where it stood elsewhere, was healthier, and the spot is free. Or null. */
    private static History.Moment recallTo(LivingEntity mob) {
        if (Root.has(mob, RootRestriction.MOVE) || Root.has(mob, RootRestriction.TELEPORT)) return null;
        if (!(mob.level() instanceof ServerLevel sl)) return null;
        History.Moment then = History.asFarAs(mob, recallTicks(mob));
        if (then == null || then.position().distanceToSqr(mob.position()) < 0.25) return null;
        if (then.health() < mob.getHealth() + mob.getMaxHealth() * RECALL_MOB_GAIN) return null;
        return Spells.fits(mob, sl, then.position()) ? then : null;
    }

    /** Recall: it steps back to where it stood, with back half the health it lost since. */
    private static boolean recall(Mob mob, LivingEntity target, ServerLevel sl) {
        History.Moment then = recallTo(mob);
        if (then == null) return false;
        Vec3 from = mob.position();
        float lost = Math.max(0f, then.health() - mob.getHealth());
        mob.teleportTo(then.position().x, then.position().y, then.position().z);
        mob.fallDistance = 0;
        FallGuard.protect(mob, Illuminator.RECALL_LANDING);
        if (lost > 0f) mob.heal(lost * Illuminator.RECALL_HEAL);
        IlluminatorFx.recall(sl, mob, from, then.position());
        History.forget(mob);                                                     // a step back cannot be stepped back again
        return true;
    }

    /** Exploit Flaw on what it Appraised (from Sequence 1 every enemy near that it marked too). */
    private static boolean exploitFlaw(Mob mob, LivingEntity aimed, ServerLevel sl) {
        List<LivingEntity> targets = new ArrayList<>();
        if (Appraisal.isMarked(mob, aimed)) targets.add(aimed);
        if (Beyonder.hasSequence(mob, Illuminator.EXPLOIT_ALL_SEQUENCE)) {
            for (LivingEntity marked : Appraisal.markedBy(mob)) {
                if (marked != aimed && marked.distanceTo(mob) <= Illuminator.EXPLOIT_ALL_RANGE && Spells.isEnemy(mob, marked)) targets.add(marked);
            }
        }
        if (targets.isEmpty()) return false;
        MobSpells.face(mob, aimed);
        if (targets.size() > 1) IlluminatorFx.exploitAll(sl, mob, targets);
        else IlluminatorFx.exploitFlaw(sl, mob, targets.get(0));
        for (LivingEntity target : targets) {
            float share = target == aimed ? 1f : Illuminator.EXPLOIT_ALL_SHARE;
            Later.run(sl, IlluminatorFx.FLAW_LANDS, () -> {
                if (!MobSpells.casterStillHere(mob, sl) || !target.isAlive() || target.level() != sl) return;
                Illuminator.flawGives(mob, sl, target, share);
            });
        }
        return true;
    }

    // =========================
    // Events
    // =========================

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        /** An Illuminator mob: its record for Recall, and what a player's tick does for its casts (body, colossus...). */
        @SubscribeEvent
        public static void onIlluminatorMobTick(LivingEvent.LivingTickEvent event) {
            if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel sl)) return;
            if (!Illuminator.PATHWAY.equals(Beyonders.pathwayOf(mob))) return;
            History.write(mob);
            IlluminatorAlchemy.tick(mob, sl);
            IlluminatorCivilization.tick(mob, sl);
        }
    }
}
