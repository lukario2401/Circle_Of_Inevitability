package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.lukario.circleofinevitability.combat.Orbit;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.status.Substitute;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Knowledge Emperor abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 *
 * The Knowledge Emperor's abilities are written for any caster (its caster a LivingEntity, its aim the caster's
 * facing), so a mob casts the code a player does: the wheel entries through MobSpells.aimedEntry, the keys' parts
 * (pry, weakPointStrike, sidestep, hex, spellbolts) once the mob has paid. Its Study is its own (status/Study takes any
 * caster), and casting anything ends its Embodiment of Knowledge, as for a player. A mob pays what an ability costs:
 * the Law of Three, the Whisper and Obscurity's discounts on costs are a player's. KnowledgeEmperorMobs.Events keeps
 * its casts going as a player's tick does.
 *
 *   1  Mystery Prying (9)
 *   2  Weak-Point Strike (8), Sidestep (8, sneak)
 *   3  Hex (7), Spellbolts (7, sneak; the Knowledge Orbs from 2)
 *   5  Stars (5): Starlight Cage, Stellar Self, Star Fall, Star Guard
 *   6  Retellings and Prophecy (4): Sleeping Beauty, Avalon, Achilles' Heel, Pandora's Box (3), Foresight (3),
 *      Prophecy of Ruin (3)
 *   7  Knowledge (2): Informatization, Erase, Knowledge Overload (1), Lance of Knowledge (1), Condense Knowledge (1),
 *      Embodiment of Knowledge (0), Indoctrination (0), Divine Kingdom (0)
 *
 * Left out: Dowsing (1, sneak: it points a player at what hides), Read Scroll and Inscribe Scroll (4: a player's
 * pouch of scrolls).
 */
public final class KnowledgeEmperorMobs {

    private static final int PRY_MOB_COOLDOWN = 600;
    private static final int STRIKE_MOB_COOLDOWN = 100;
    private static final int SIDESTEP_MOB_COOLDOWN = 200;
    private static final int HEX_MOB_COOLDOWN = 400;
    private static final int BOLT_MOB_COOLDOWN = 120;

    private KnowledgeEmperorMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        String P = KnowledgeEmperor.PATHWAY;

        // ---- key 1: Mystery Prying (Dowsing shows a player the way: left out)
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.NORMAL)
                .cost(KnowledgeEmperor.PRY_COST).cooldown(Math.max(PRY_MOB_COOLDOWN, KnowledgeEmperor.PRY_TICKS + 200))
                .range(KnowledgeEmperor.PRY_REACH)
                .when((mob, target, sl) -> !KnowledgeEmperor.isActive(mob, KnowledgeEmperor.PRY_UNTIL))
                .cast(cast(MobSpells.aimedCore(KnowledgeEmperor::pry))));

        // ---- key 2: Weak-Point Strike / Sidestep
        MobAbilities.register(MobAbility.of(P, 2, MobAbility.NORMAL)
                .cost(KnowledgeEmperor.STRIKE_COST).cooldown(STRIKE_MOB_COOLDOWN).weight(14)
                .range(KnowledgeEmperor.STRIKE_STEP + KnowledgeEmperor.STRIKE_REACH).sight()
                .cast(cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    Vec3 stand = KnowledgeEmperor.strikeSpot(mob, sl, target);
                    if (stand == null) return false;
                    KnowledgeEmperor.weakPointStrike(mob, sl, target, stand);
                    return true;
                })));
        MobAbilities.register(MobAbility.of(P, 2, MobAbility.SNEAK).named("Sidestep")
                .cost(KnowledgeEmperor.SIDESTEP_COST).cooldown(SIDESTEP_MOB_COOLDOWN)
                .range(5.0)
                .when((mob, target, sl) -> !Substitute.isArmed(mob) && !KnowledgeEmperor.isActive(mob, KnowledgeEmperor.SIDESTEP_UNTIL))
                .cast(cast(MobSpells.aimedCore(KnowledgeEmperor::sidestep))));

        // ---- key 3: Hex / Spellbolts
        MobAbilities.register(MobAbility.of(P, 3, MobAbility.NORMAL)
                .cost(KnowledgeEmperor.HEX_COST).cooldown(Math.max(HEX_MOB_COOLDOWN, KnowledgeEmperor.HEX_TICKS))
                .range(KnowledgeEmperor.HEX_RANGE).sight()
                .cast(cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    KnowledgeEmperor.hex(mob, sl, target, KnowledgeEmperor.HEX_TICKS);
                    return true;
                })));
        MobAbilities.register(MobAbility.of(P, 3, MobAbility.SNEAK).named("Spellbolts")
                .cost(KnowledgeEmperor.BOLT_COST).cooldown(BOLT_MOB_COOLDOWN).weight(14)
                .range(KnowledgeEmperor.BOLT_RANGE).sight()
                .cast(cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    return KnowledgeEmperor.spellbolts(mob, sl, 1f);
                })));

        // ---- key 5: the Stars wheel
        entry(5, "starlight_cage", "Starlight Cage", KnowledgeEmperorStars.CAGE_SEQUENCE, KnowledgeEmperorStars.CAGE_COST,
                KnowledgeEmperorStars.CAGE_COOLDOWN, KnowledgeEmperorStars.CAGE_RANGE, true, KnowledgeEmperorStars::starlightCage, null);
        entry(5, "stellar_self", "Stellar Self", KnowledgeEmperorStars.STELLAR_SEQUENCE, KnowledgeEmperorStars.STELLAR_COST,
                KnowledgeEmperorStars.STELLAR_COOLDOWN, KnowledgeEmperorStars.STELLAR_RANGE + 4.0, true, KnowledgeEmperorStars::stellarSelf,
                (mob, target, sl) -> mob.distanceTo(target) > 6.0);                       // to get at a target off
        entry(5, "star_fall", "Star Fall", KnowledgeEmperorStars.FALL_SEQUENCE, KnowledgeEmperorStars.FALL_COST,
                KnowledgeEmperorStars.FALL_COOLDOWN, KnowledgeEmperorStars.FALL_RANGE, true, KnowledgeEmperorStars::starFall, null);
        entry(5, "star_guard", "Star Guard", KnowledgeEmperorStars.GUARD_SEQUENCE, KnowledgeEmperorStars.GUARD_COST,
                Math.max(KnowledgeEmperorStars.GUARD_COOLDOWN, KnowledgeEmperorStars.GUARD_TICKS), 16.0, false, KnowledgeEmperorStars::starGuard,
                (mob, target, sl) -> !Orbit.isOpen(mob, KnowledgeEmperorStars.GUARD));

        // ---- key 6: the Retellings and Prophecy wheel
        entry(6, "sleeping_beauty", "Sleeping Beauty", KnowledgeEmperorRetellings.BEAUTY_SEQUENCE, KnowledgeEmperorRetellings.BEAUTY_COST,
                KnowledgeEmperorRetellings.BEAUTY_COOLDOWN, KnowledgeEmperorRetellings.BEAUTY_RADIUS, false,
                KnowledgeEmperorRetellings::sleepingBeauty, null);
        entry(6, "avalon", "Avalon", KnowledgeEmperorRetellings.AVALON_SEQUENCE, KnowledgeEmperorRetellings.AVALON_COST,
                Math.max(KnowledgeEmperorRetellings.AVALON_COOLDOWN, KnowledgeEmperorRetellings.AVALON_TICKS), KnowledgeEmperorRetellings.AVALON_RADIUS,
                false, KnowledgeEmperorRetellings::avalon, null);
        entry(6, "achilles_heel", "Achilles' Heel", KnowledgeEmperorRetellings.HEEL_SEQUENCE, KnowledgeEmperorRetellings.HEEL_COST,
                KnowledgeEmperorRetellings.HEEL_COOLDOWN, KnowledgeEmperorRetellings.HEEL_RANGE, true, KnowledgeEmperorRetellings::achillesHeel, null);
        entry(6, "pandoras_box", "Pandora's Box", KnowledgeEmperorRetellings.BOX_SEQUENCE, KnowledgeEmperorRetellings.BOX_COST,
                KnowledgeEmperorRetellings.BOX_COOLDOWN, KnowledgeEmperorRetellings.BOX_RADIUS, false, KnowledgeEmperorRetellings::pandorasBox, null);
        entry(6, "foresight", "Foresight", KnowledgeEmperorRetellings.FORESIGHT_SEQUENCE, KnowledgeEmperorRetellings.FORESIGHT_COST,
                Math.max(KnowledgeEmperorRetellings.FORESIGHT_COOLDOWN, KnowledgeEmperorRetellings.FORESIGHT_TICKS), 12.0, false,
                KnowledgeEmperorRetellings::foresight, null);
        entry(6, "prophecy_of_ruin", "Prophecy of Ruin", KnowledgeEmperorRetellings.PROPHECY_SEQUENCE, KnowledgeEmperorRetellings.PROPHECY_COST,
                KnowledgeEmperorRetellings.PROPHECY_COOLDOWN, KnowledgeEmperorRetellings.PROPHECY_RANGE, true,
                KnowledgeEmperorRetellings::prophecyOfRuin, null);

        // ---- key 7: the Knowledge wheel
        entry(7, "informatization", "Informatization", KnowledgeEmperorKnowledge.INFO_SEQUENCE, KnowledgeEmperorKnowledge.INFO_COST,
                KnowledgeEmperorKnowledge.INFO_COOLDOWN, 12.0, false, KnowledgeEmperorKnowledge::informatization,
                (mob, target, sl) -> !KnowledgeEmperorKnowledge.isInformatized(mob)
                        && mob.getHealth() < mob.getMaxHealth() * 0.6f);                   // when the fight goes badly
        entry(7, "erase", "Erase", KnowledgeEmperorKnowledge.ERASE_SEQUENCE, KnowledgeEmperorKnowledge.ERASE_COST,
                KnowledgeEmperorKnowledge.ERASE_COOLDOWN, KnowledgeEmperorKnowledge.ERASE_RANGE, true, KnowledgeEmperorKnowledge::erase, null);
        entry(7, "knowledge_overload", "Knowledge Overload", KnowledgeEmperorKnowledge.OVERLOAD_SEQUENCE, KnowledgeEmperorKnowledge.OVERLOAD_COST,
                KnowledgeEmperorKnowledge.OVERLOAD_COOLDOWN, KnowledgeEmperorKnowledge.OVERLOAD_RANGE, true,
                KnowledgeEmperorKnowledge::knowledgeOverload, null);
        entry(7, "lance_of_knowledge", "Lance of Knowledge", KnowledgeEmperorKnowledge.LANCE_SEQUENCE, KnowledgeEmperorKnowledge.LANCE_COST,
                KnowledgeEmperorKnowledge.LANCE_COOLDOWN, KnowledgeEmperorKnowledge.LANCE_RANGE, true,
                KnowledgeEmperorKnowledge::lanceOfKnowledge, null);
        entry(7, "condense_knowledge", "Condense Knowledge", KnowledgeEmperorKnowledge.CONDENSE_SEQUENCE, KnowledgeEmperorKnowledge.CONDENSE_COST,
                KnowledgeEmperorKnowledge.CONDENSE_COOLDOWN, 12.0, false, KnowledgeEmperorKnowledge::condenseKnowledge, null);
        entry(7, "embodiment_of_knowledge", "Embodiment of Knowledge", KnowledgeEmperorKnowledge.EMBODIMENT_SEQUENCE,
                KnowledgeEmperorKnowledge.EMBODIMENT_COST, KnowledgeEmperorKnowledge.EMBODIMENT_COOLDOWN, 16.0, false,
                KnowledgeEmperorKnowledge::embodiment,
                (mob, target, sl) -> !KnowledgeEmperorKnowledge.isEmbodied(mob) && mob.getHealth() < mob.getMaxHealth() * 0.5f);
        entry(7, "indoctrination", "Indoctrination", KnowledgeEmperorKnowledge.INDOCTRINATION_SEQUENCE,
                KnowledgeEmperorKnowledge.INDOCTRINATION_COST, KnowledgeEmperorKnowledge.INDOCTRINATION_COOLDOWN,
                KnowledgeEmperorKnowledge.INDOCTRINATION_RADIUS, false, KnowledgeEmperorKnowledge::indoctrination, null);
        entry(7, "divine_kingdom", "Divine Kingdom", KnowledgeEmperorKnowledge.KINGDOM_SEQUENCE, KnowledgeEmperorKnowledge.KINGDOM_COST,
                Math.max(KnowledgeEmperorKnowledge.KINGDOM_COOLDOWN, KnowledgeEmperorKnowledge.KINGDOM_TICKS + 600), 32.0, false,
                KnowledgeEmperorKnowledge::divineKingdom,
                (mob, target, sl) -> !Zone.isOpen(mob, KnowledgeEmperorKnowledge.KINGDOM));    // never to end it
    }

    /** One wheel entry as an ability of its own (aimed at its target; a refusal spends nothing). */
    private static void entry(int key, String entry, String name, int sequence, int cost, int cooldown, double range,
                              boolean sight, MobSpells.Entry cast, MobAbility.When when) {
        MobAbility ability = MobAbility.of(KnowledgeEmperor.PATHWAY, key, MobAbility.NORMAL).entry(entry).named(name)
                .sequence(sequence).cost(cost).cooldown(Math.max(100, cooldown))
                .range(range).cast(cast(MobSpells.aimedEntry(cast)));
        if (sight) ability.sight();
        if (when != null) ability.when(when);
        MobAbilities.register(ability);
    }

    /** A cast that went off ends the mob's Embodiment of Knowledge (cast before this tick), as a player's casts do. */
    private static MobAbility.Cast cast(MobAbility.Cast cast) {
        return (mob, target, sl) -> {
            if (!cast.run(mob, target, sl)) return false;
            KnowledgeEmperorKnowledge.castSomething(mob);
            return true;
        };
    }

    // =========================
    // Events
    // =========================

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        /** A Knowledge Emperor mob: what a player's tick does for its casts (the prying eyes, Foresight, Informatization). */
        @SubscribeEvent
        public static void onKnowledgeEmperorMobTick(LivingEvent.LivingTickEvent event) {
            if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel sl)) return;
            if (!KnowledgeEmperor.PATHWAY.equals(Beyonders.pathwayOf(mob))) return;
            if (KnowledgeEmperor.isActive(mob, KnowledgeEmperor.PRY_UNTIL)) KnowledgeEmperor.pryTick(mob, sl);
            KnowledgeEmperorRetellings.tick(mob, sl);
            KnowledgeEmperorKnowledge.tick(mob, sl);
        }
    }
}
