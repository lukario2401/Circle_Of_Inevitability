package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionFx.Bribe;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Prince Of Abolition abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 *
 * The Prince's abilities are written for any caster (its caster a LivingEntity, its aim the caster's facing), so a mob
 * casts the code a player does: the wheel entries through MobSpells.aimedEntry, the keys' parts (verdict, brainwash,
 * bruteForce, ironWill, distortShot / distortIntent, seal / gate, paces, disorder / unbind) once the mob has paid. Its
 * marks are its own (PrinceOfAbolitionMarks), its mausoleums stand for it (and bring it back once), and Sequence Might
 * and the mausoleums' bonus count on its blows and the blows it takes, as for a player. PrinceOfAbolitionMobs.Events
 * keeps its casts going as a player's tick does.
 *
 *   1  Verdict (9), Brainwash (9, sneak)
 *   2  Brute Force (8), Iron Will (8, sneak: when something is wrong with it)
 *   3  Bribe (7): Weaken, Charm, Arrogance, Connect, Coin Shot (an actual Bribe when it holds gold or an emerald)
 *   4  Distort (6: a projectile turned back, or the target's intent; a shell flipped from 2), Seal (6, sneak; a gate
 *      from 4)
 *   5  Paces (5: toward a target far off), Disorder (5, sneak; it frees itself when held)
 *   6  The Fallen (8): Raise Mausoleum, Corrosion (6), Step (6), Majesty (5), Bestowment (4), Exploit (4: strips its
 *      target's states), Magnify (4), Mausoleum Salvo (4), Mausoleum Blast (4)
 *   7  Sovereign (6): Ruin, Frenzy (3), Entropy (2), Abolition (1), Exile (1), Inversion (0), Coronation (0), Decree (0)
 *
 * Simplified: Exploit only strips (hanging in the air and stretching a state are a player's flight). A mob pays what an
 * ability costs: Connect's half price and the recast rules are a player's.
 */
public final class PrinceOfAbolitionMobs {

    private static final int VERDICT_MOB_COOLDOWN = 80;
    private static final int BRAINWASH_MOB_COOLDOWN = 400;
    private static final int BRUTE_MOB_COOLDOWN = 100;
    private static final int IRON_MOB_COOLDOWN = 400;
    private static final int DISTORT_MOB_COOLDOWN = 200;
    private static final int SEAL_MOB_COOLDOWN = 600;
    private static final int PACES_MOB_COOLDOWN = 240;
    private static final int DISORDER_MOB_COOLDOWN = 240;
    private static final double PACES_MOB_FAR = 8.0;            // it strides toward a target further than this

    private PrinceOfAbolitionMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        String P = PrinceOfAbolition.PATHWAY;

        // ---- key 1: Verdict / Brainwash
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.NORMAL)
                .cost(PrinceOfAbolition.VERDICT_COST).cooldown(VERDICT_MOB_COOLDOWN).weight(14)
                .range(PrinceOfAbolition.VERDICT_RANGE).sight()
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    PrinceOfAbolition.verdict(mob, sl, target);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.SNEAK).named("Brainwash")
                .cost(PrinceOfAbolition.BRAINWASH_COST).cooldown(BRAINWASH_MOB_COOLDOWN)
                .range(PrinceOfAbolition.BRAINWASH_RANGE).sight()
                .when((mob, target, sl) -> target.getMaxHealth() <= PrinceOfAbolition.BOSS_HEALTH)
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    PrinceOfAbolition.brainwash(mob, sl, target);
                    return true;
                }));

        // ---- key 2: Brute Force / Iron Will
        MobAbilities.register(MobAbility.of(P, 2, MobAbility.NORMAL)
                .cost(PrinceOfAbolition.BRUTE_COST).cooldown(BRUTE_MOB_COOLDOWN).weight(14)
                .range(PrinceOfAbolition.BRUTE_REACH).sight()
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    PrinceOfAbolition.bruteForce(mob, sl, target, PrinceOfAbolition.range(mob, PrinceOfAbolition.BRUTE_REACH));
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 2, MobAbility.SNEAK).named("Iron Will")
                .cost(PrinceOfAbolition.IRON_COST).cooldown(IRON_MOB_COOLDOWN)
                .when((mob, target, sl) -> !Spells.harmfulEffects(mob).isEmpty())
                .cast(MobSpells.aimedCore(PrinceOfAbolition::ironWill)));

        // ---- key 3: the Bribe wheel
        entry(3, "weaken", "Weaken", PrinceOfAbolitionBribes.BRIBE_SEQUENCE, PrinceOfAbolitionBribes.WEAKEN_COST,
                PrinceOfAbolitionBribes.WEAKEN_COOLDOWN, PrinceOfAbolitionBribes.BRIBE_RANGE, true,
                (mob, sl) -> PrinceOfAbolitionBribes.bribe(mob, sl, Bribe.WEAKEN), null);
        entry(3, "charm", "Charm", PrinceOfAbolitionBribes.BRIBE_SEQUENCE, PrinceOfAbolitionBribes.CHARM_COST,
                PrinceOfAbolitionBribes.CHARM_COOLDOWN, PrinceOfAbolitionBribes.BRIBE_RANGE, true,
                (mob, sl) -> PrinceOfAbolitionBribes.bribe(mob, sl, Bribe.CHARM),
                (mob, target, sl) -> target.getMaxHealth() <= PrinceOfAbolition.BOSS_HEALTH);
        entry(3, "arrogance", "Arrogance", PrinceOfAbolitionBribes.BRIBE_SEQUENCE, PrinceOfAbolitionBribes.ARROGANCE_COST,
                PrinceOfAbolitionBribes.ARROGANCE_COOLDOWN, PrinceOfAbolitionBribes.BRIBE_RANGE, true,
                (mob, sl) -> PrinceOfAbolitionBribes.bribe(mob, sl, Bribe.ARROGANCE), null);
        entry(3, "connect", "Connect", PrinceOfAbolitionBribes.BRIBE_SEQUENCE, PrinceOfAbolitionBribes.CONNECT_COST,
                Math.max(PrinceOfAbolitionBribes.CONNECT_COOLDOWN, PrinceOfAbolitionBribes.CONNECT_TICKS), PrinceOfAbolitionBribes.CONNECT_RANGE,
                true, PrinceOfAbolitionBribes::connect, null);
        entry(3, "coin_shot", "Coin Shot", PrinceOfAbolitionBribes.BRIBE_SEQUENCE, PrinceOfAbolitionBribes.COIN_COST,
                PrinceOfAbolitionBribes.COIN_COOLDOWN, PrinceOfAbolitionBribes.COIN_RANGE, true, PrinceOfAbolitionBribes::coinShot, null);

        // ---- key 4: Distort / Seal (a gate from Earl of the Fallen)
        MobAbilities.register(MobAbility.of(P, 4, MobAbility.NORMAL)
                .cost(PrinceOfAbolition.DISTORT_COST).cooldown(DISTORT_MOB_COOLDOWN)
                .range(PrinceOfAbolition.DISTORT_RANGE).sight()
                .cast(PrinceOfAbolitionMobs::distort));
        MobAbilities.register(MobAbility.of(P, 4, MobAbility.SNEAK).named("Seal")
                .cost(PrinceOfAbolition.SEAL_COST)
                .cooldown(Math.max(SEAL_MOB_COOLDOWN, PrinceOfAbolition.SEAL_TICKS + 200))
                .range(PrinceOfAbolition.SEAL_RADIUS)
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    if (Beyonder.hasSequence(mob, PrinceOfAbolition.GATE_SEQUENCE)) {
                        Vec3 middle = PrinceOfAbolition.gateSpot(mob, sl);
                        if (middle == null) return false;
                        PrinceOfAbolition.gate(mob, sl, middle);
                    } else {
                        PrinceOfAbolition.seal(mob, sl);
                    }
                    return true;
                }));

        // ---- key 5: Paces (normal) / Disorder (sneak), as the key has them
        MobAbilities.register(MobAbility.of(P, 5, MobAbility.NORMAL).named("Paces")
                .cost(PrinceOfAbolition.PACES_COST).cooldown(PACES_MOB_COOLDOWN)
                .range(PrinceOfAbolition.PACES_RANGE).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > PACES_MOB_FAR && !PrinceOfAbolition.isHeld(mob))
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    double range = PrinceOfAbolition.range(mob, Beyonder.hasSequence(mob, PrinceOfAbolition.PACES_EARL_SEQUENCE)
                            ? PrinceOfAbolition.PACES_EARL_RANGE : PrinceOfAbolition.PACES_RANGE);
                    Vec3 to = Spells.blinkSpot(mob, sl, Math.min(range, mob.distanceTo(target)));
                    if (to == null) return false;
                    PrinceOfAbolition.paces(mob, sl, to);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 5, MobAbility.SNEAK).named("Disorder")
                .cost(PrinceOfAbolition.DISORDER_COST).cooldown(DISORDER_MOB_COOLDOWN)
                .range(PrinceOfAbolition.DISORDER_RANGE).sight()
                .cast((mob, target, sl) -> {
                    if (PrinceOfAbolition.isHeld(mob)) {
                        PrinceOfAbolition.unbind(mob, sl);
                        return true;
                    }
                    MobSpells.face(mob, target);
                    PrinceOfAbolition.disorder(mob, sl, target, PrinceOfAbolition.duration(mob, PrinceOfAbolition.DISORDER_TICKS));
                    PrinceOfAbolition.untouchable(mob, sl);
                    return true;
                }));

        // ---- key 6: the Fallen wheel
        entry(6, "raise_mausoleum", "Raise Mausoleum", PrinceOfAbolitionFallen.RAISE_SEQUENCE, PrinceOfAbolitionFallen.RAISE_COST,
                600, PrinceOfAbolitionFallen.RAISE_RANGE + 8.0, false, PrinceOfAbolitionFallen::raiseMausoleum,
                (mob, target, sl) -> PrinceOfAbolitionMausoleums.count(mob) < 3);                // a few, not all nine
        entry(6, "corrosion", "Corrosion", PrinceOfAbolitionFallen.CORROSION_SEQUENCE, PrinceOfAbolitionFallen.CORROSION_COST,
                PrinceOfAbolitionFallen.CORROSION_COOLDOWN, PrinceOfAbolitionFallen.CORROSION_RADIUS, false, PrinceOfAbolitionFallen::corrosion, null);
        entry(6, "step", "Step", PrinceOfAbolitionFallen.STEP_SEQUENCE, PrinceOfAbolitionFallen.STEP_COST,
                Math.max(200, PrinceOfAbolitionFallen.STEP_COOLDOWN), PrinceOfAbolitionFallen.STEP_RANGE, false, PrinceOfAbolitionFallen::step,
                (mob, target, sl) -> PrinceOfAbolitionMausoleums.count(mob) > 0 && mob.getHealth() < mob.getMaxHealth() * 0.4f);
        entry(6, "majesty", "Majesty", PrinceOfAbolitionFallen.MAJESTY_SEQUENCE, PrinceOfAbolitionFallen.MAJESTY_COST,
                PrinceOfAbolitionFallen.MAJESTY_COOLDOWN, PrinceOfAbolitionFallen.MAJESTY_RADIUS, false, PrinceOfAbolitionFallen::majesty, null);
        entry(6, "bestowment", "Bestowment", PrinceOfAbolitionFallen.BESTOW_SEQUENCE, PrinceOfAbolitionFallen.BESTOW_COST,
                PrinceOfAbolitionFallen.BESTOW_COOLDOWN, PrinceOfAbolitionFallen.BESTOW_RANGE, true, PrinceOfAbolitionFallen::bestowment, null);
        MobAbilities.register(MobAbility.of(P, 6, MobAbility.NORMAL).entry("exploit").named("Exploit")
                .sequence(PrinceOfAbolitionFallen.EXPLOIT_SEQUENCE).cost(PrinceOfAbolitionFallen.EXPLOIT_COST)
                .cooldown(PrinceOfAbolitionFallen.EXPLOIT_COOLDOWN)
                .range(PrinceOfAbolitionFallen.STRIP_RANGE).sight()
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    PrinceOfAbolitionFallen.strip(mob, sl, target);
                    return true;
                }));
        entry(6, "magnify", "Magnify", PrinceOfAbolitionFallen.MAGNIFY_SEQUENCE, PrinceOfAbolitionFallen.MAGNIFY_COST,
                Math.max(PrinceOfAbolitionFallen.MAGNIFY_COOLDOWN, PrinceOfAbolitionFallen.MAGNIFY_TICKS), 8.0, false,
                PrinceOfAbolitionFallen::magnify, null);
        entry(6, "mausoleum_salvo", "Mausoleum Salvo", PrinceOfAbolitionFallen.SALVO_SEQUENCE, PrinceOfAbolitionFallen.SALVO_COST,
                PrinceOfAbolitionFallen.SALVO_COOLDOWN, PrinceOfAbolitionFallen.SALVO_RANGE, true, PrinceOfAbolitionFallen::salvo,
                (mob, target, sl) -> PrinceOfAbolitionMausoleums.count(mob) > 0);
        entry(6, "mausoleum_blast", "Mausoleum Blast", PrinceOfAbolitionFallen.BLAST_SEQUENCE, PrinceOfAbolitionFallen.BLAST_COST,
                PrinceOfAbolitionFallen.BLAST_COOLDOWN, PrinceOfAbolitionFallen.BLAST_REACH, false, PrinceOfAbolitionFallen::blast,
                (mob, target, sl) -> PrinceOfAbolitionMausoleums.count(mob) > 0);

        // ---- key 7: the Sovereign wheel
        entry(7, "ruin", "Ruin", PrinceOfAbolitionSovereign.RUIN_SEQUENCE, PrinceOfAbolitionSovereign.RUIN_COST,
                PrinceOfAbolitionSovereign.RUIN_COOLDOWN, PrinceOfAbolitionSovereign.RUIN_RANGE, true, PrinceOfAbolitionSovereign::ruin, null);
        entry(7, "frenzy", "Frenzy", PrinceOfAbolitionSovereign.FRENZY_SEQUENCE, PrinceOfAbolitionSovereign.FRENZY_COST,
                PrinceOfAbolitionSovereign.FRENZY_COOLDOWN, PrinceOfAbolitionSovereign.FRENZY_RANGE, false, PrinceOfAbolitionSovereign::frenzy, null);
        entry(7, "entropy", "Entropy", PrinceOfAbolitionSovereign.ENTROPY_SEQUENCE, PrinceOfAbolitionSovereign.ENTROPY_COST,
                Math.max(PrinceOfAbolitionSovereign.ENTROPY_COOLDOWN, PrinceOfAbolitionSovereign.ENTROPY_TICKS), PrinceOfAbolitionSovereign.ENTROPY_RANGE,
                true, PrinceOfAbolitionSovereign::entropy, null);
        entry(7, "abolition", "Abolition", PrinceOfAbolitionSovereign.ABOLITION_SEQUENCE, PrinceOfAbolitionSovereign.ABOLITION_COST,
                Math.max(PrinceOfAbolitionSovereign.ABOLITION_COOLDOWN, PrinceOfAbolitionSovereign.ABOLITION_TICKS + 200),
                PrinceOfAbolitionSovereign.ABOLITION_RADIUS - 2.0, false, PrinceOfAbolitionSovereign::abolition,
                (mob, target, sl) -> !Dome.isRaised(mob, PrinceOfAbolitionSovereign.ABOLITION_DOME));   // never to close it
        entry(7, "exile", "Exile", PrinceOfAbolitionSovereign.EXILE_SEQUENCE, PrinceOfAbolitionSovereign.EXILE_COST,
                PrinceOfAbolitionSovereign.EXILE_COOLDOWN, PrinceOfAbolitionSovereign.EXILE_RANGE, true, PrinceOfAbolitionSovereign::exile, null);
        entry(7, "inversion", "Inversion", PrinceOfAbolitionSovereign.INVERSION_SEQUENCE, PrinceOfAbolitionSovereign.INVERSION_COST,
                Math.max(PrinceOfAbolitionSovereign.INVERSION_COOLDOWN, PrinceOfAbolitionSovereign.INVERSION_TICKS),
                PrinceOfAbolitionSovereign.INVERSION_RADIUS - 4.0, false, PrinceOfAbolitionSovereign::inversion, null);
        entry(7, "coronation", "Coronation", PrinceOfAbolitionSovereign.CORONATION_SEQUENCE, PrinceOfAbolitionSovereign.CORONATION_COST,
                Math.max(PrinceOfAbolitionSovereign.CORONATION_COOLDOWN, PrinceOfAbolitionSovereign.CORONATION_TICKS), 16.0, false,
                PrinceOfAbolitionSovereign::coronation,
                (mob, target, sl) -> !PrinceOfAbolitionSovereign.isCrowned(mob) && PrinceOfAbolitionMausoleums.count(mob) > 0);
        entry(7, "decree", "Decree", PrinceOfAbolitionSovereign.DECREE_SEQUENCE, PrinceOfAbolitionSovereign.DECREE_COST,
                PrinceOfAbolitionSovereign.DECREE_COOLDOWN, PrinceOfAbolitionSovereign.DECREE_RANGE, true, PrinceOfAbolitionSovereign::decree, null);
    }

    /** One wheel entry as an ability of its own (aimed at its target; a refusal spends nothing). */
    private static void entry(int key, String entry, String name, int sequence, int cost, int cooldown, double range,
                              boolean sight, MobSpells.Entry cast, MobAbility.When when) {
        MobAbility ability = MobAbility.of(PrinceOfAbolition.PATHWAY, key, MobAbility.NORMAL).entry(entry).named(name)
                .sequence(sequence).cost(cost).cooldown(Math.max(100, cooldown))
                .range(range).cast(MobSpells.aimedEntry(cast));
        if (sight) ability.sight();
        if (when != null) ability.when(when);
        MobAbilities.register(ability);
    }

    /** Distort: a projectile near its line of sight is turned back; else a shell is flipped (2); else its target's intent. */
    private static boolean distort(Mob mob, LivingEntity target, ServerLevel sl) {
        MobSpells.face(mob, target);
        double range = PrinceOfAbolition.range(mob, PrinceOfAbolition.DISTORT_RANGE);
        Projectile shot = PrinceOfAbolition.aimedProjectile(mob, sl, range);
        if (shot != null) {
            PrinceOfAbolition.distortShot(mob, sl, shot);
            return true;
        }
        if (Beyonder.hasSequence(mob, PrinceOfAbolition.FLIP_SEQUENCE)) {
            Dome.Raised shell = PrinceOfAbolition.aimedShell(mob, sl, range);
            if (shell != null) {
                Dome.flip(shell.owner(), shell.name());
                PrinceOfAbolitionFx.shellFlip(sl, shell.center(), shell.radius());
                return true;
            }
        }
        PrinceOfAbolition.distortIntent(mob, sl, target);
        return true;
    }

    // =========================
    // Events
    // =========================

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        /** A Prince Of Abolition mob: what a player's tick does for its casts (Unbound, its orders and mausoleums). */
        @SubscribeEvent
        public static void onPrinceOfAbolitionMobTick(LivingEvent.LivingTickEvent event) {
            if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel sl)) return;
            if (!PrinceOfAbolition.PATHWAY.equals(Beyonders.pathwayOf(mob))) return;
            if (PrinceOfAbolition.isActive(mob, PrinceOfAbolition.UNBOUND_UNTIL)) PrinceOfAbolition.shakeHolds(mob);
            if (mob.tickCount % 20 == 0) {
                PrinceOfAbolitionOrders.tick(mob);
                PrinceOfAbolitionMausoleums.ownerTick(mob, sl);
            }
        }
    }
}
