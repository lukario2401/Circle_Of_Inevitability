package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.ConjuredArmor;
import net.lukario.circleofinevitability.status.ConjuredWeapon;
import net.lukario.circleofinevitability.status.GiantForm;
import net.lukario.circleofinevitability.status.Substitute;
import net.lukario.circleofinevitability.status.WeaponCoat;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * Hand Of God abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 *
 * Every Hand Of God ability is written for any caster (its caster a LivingEntity, its aim the caster's facing), so a
 * mob casts the very code a player does: MobSpells.aimedCore runs a key's ability once it is paid for, and
 * MobSpells.aimedEntry turns the mob to its target and runs a wheel entry (each entry is an ability of its own: a mob
 * has no wheel). What a player's swing does (Grandmaster's Edge, the Sword of Dawn, Alchemy's coat, the Riposte
 * counter) a mob's own melee blows do. HandOfGod.HandOfGodEvents keeps a mob's casts going (onHandOfGodMobTick).
 *
 *   1  Warrior's Charge (9: at a target a few blocks off), Divine Blood (9, sneak: when hurt)
 *   2  Gladiator's Flurry (8), Supernatural Resistance (8, sneak: against a Beyonder, or when held)
 *   3  Grandmaster's Edge (7), Riposte (7, sneak)
 *   4  Dawn (6): Light of Dawn, Dawn Armor, Sword of Dawn, Hurricane of Light
 *   5  Protection (5; the Cage from 2), Damage Transference (5, sneak: with friends near), the mend (1: hurt in its
 *      own dome)
 *   6  Demon Hunting (4): Eye of Demon Hunting, Alchemy, Mind Concealment, Giant Form
 *   7  Silver and Twilight (3): Mercury Liquefaction, Silver Rapier, Light Concealment, Twilight Sword (2), Divine
 *      Right Hand (1), Passage of Time (0), Decay (0), Divine Kingdom (0)
 *
 * The cooldowns, and when a mob thinks a cast worth it, are the mob's. Nothing is left out; the passives (Weapon
 * Mastery, Giant's Physique, Tracking, Divine Touch, the auras) are a player's.
 */
public final class HandOfGodMobs {

    // ---------- mob cooldowns (ticks)
    private static final int CHARGE_MOB_COOLDOWN = 160;
    private static final int BLOOD_MOB_COOLDOWN = 600;
    private static final int FLURRY_MOB_COOLDOWN = 140;
    private static final int RESIST_MOB_COOLDOWN = 400;
    private static final int EDGE_MOB_COOLDOWN = 600;
    private static final int RIPOSTE_MOB_COOLDOWN = 200;
    private static final int MEND_MOB_COOLDOWN = 600;
    private static final double CHARGE_MOB_NEAR = 3.0;          // a target closer than this is not rushed at
    private static final float BLOOD_MOB_HURT = 0.5f;           // Divine Blood below this much of its health
    private static final float MEND_MOB_HURT = 0.6f;

    private HandOfGodMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        String P = HandOfGod.PATHWAY;

        // ---- key 1: Warrior's Charge / Divine Blood
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.NORMAL)
                .cost(HandOfGod.CHARGE_COST).cooldown(CHARGE_MOB_COOLDOWN).weight(14)
                .range(HandOfGod.CHARGE_RANGE + 1).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > CHARGE_MOB_NEAR
                        && !HandOfGod.isActive(mob, HandOfGod.CHARGING_UNTIL) && !Root.has(mob, RootRestriction.MOVE))
                .cast(MobSpells.aimedCore(HandOfGod::charge)));
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.SNEAK).named("Divine Blood")
                .cost(HandOfGod.BLOOD_COST).cooldown(BLOOD_MOB_COOLDOWN).anyRange()
                .when((mob, target, sl) -> mob.getHealth() < mob.getMaxHealth() * BLOOD_MOB_HURT
                        && !HandOfGod.isActive(mob, HandOfGod.BLOOD_UNTIL))
                .cast(MobSpells.aimedCore(HandOfGod::divineBlood)));

        // ---- key 2: Gladiator's Flurry / Supernatural Resistance
        MobAbilities.register(MobAbility.of(P, 2, MobAbility.NORMAL)
                .cost(HandOfGod.FLURRY_COST).cooldown(FLURRY_MOB_COOLDOWN).weight(14)
                .range(HandOfGod.FLURRY_RANGE).sight()
                .when((mob, target, sl) -> !HandOfGod.isActive(mob, HandOfGod.FLURRY_UNTIL))
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    HandOfGod.flurry(mob, sl, target);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 2, MobAbility.SNEAK).named("Supernatural Resistance")
                .cost(HandOfGod.RESIST_COST).cooldown(RESIST_MOB_COOLDOWN)
                .when((mob, target, sl) -> !HandOfGod.isActive(mob, HandOfGod.RESIST_UNTIL)
                        && (Beyonders.isBeyonder(target) || Root.has(mob, RootRestriction.MOVE)))
                .cast(MobSpells.aimedCore(HandOfGod::supernaturalResistance)));

        // ---- key 3: Grandmaster's Edge / Riposte (both work on its own melee blows)
        MobAbilities.register(MobAbility.of(P, 3, MobAbility.NORMAL)
                .cost(HandOfGod.EDGE_COST).cooldown(EDGE_MOB_COOLDOWN)
                .range(6.0)
                .when((mob, target, sl) -> !HandOfGod.isActive(mob, HandOfGod.EDGE_UNTIL))
                .cast(MobSpells.aimedCore(HandOfGod::grandmastersEdge)));
        MobAbilities.register(MobAbility.of(P, 3, MobAbility.SNEAK).named("Riposte")
                .cost(HandOfGod.RIPOSTE_COST).cooldown(RIPOSTE_MOB_COOLDOWN)
                .range(5.0)
                .when((mob, target, sl) -> !Substitute.isArmed(mob) && !HandOfGod.isActive(mob, HandOfGod.RIPOSTE_UNTIL))
                .cast(MobSpells.aimedCore(HandOfGod::riposte)));

        // ---- key 4: the Dawn wheel
        entry(4, "light_of_dawn", "Light of Dawn", HandOfGodDawn.LIGHT_SEQUENCE, HandOfGodDawn.LIGHT_COST,
                HandOfGodDawn.LIGHT_COOLDOWN + HandOfGodDawn.LIGHT_TICKS, HandOfGodDawn.LIGHT_RADIUS, false,
                HandOfGodDawn::lightOfDawn, null);
        entry(4, "dawn_armor", "Dawn Armor", HandOfGodDawn.ARMOR_SEQUENCE, HandOfGodDawn.ARMOR_COST,
                HandOfGodDawn.ARMOR_COOLDOWN, 16.0, false, HandOfGodDawn::dawnArmor,
                (mob, target, sl) -> !ConjuredArmor.isWorn(mob, HandOfGodDawn.ARMOR));      // never to take it off
        entry(4, "sword_of_dawn", "Sword of Dawn", HandOfGodDawn.SWORD_SEQUENCE, HandOfGodDawn.SWORD_COST,
                HandOfGodDawn.SWORD_COOLDOWN, 12.0, false, HandOfGodDawn::swordOfDawn,
                (mob, target, sl) -> !ConjuredWeapon.isDrawn(mob, HandOfGodDawn.SWORD));     // never to put it away
        entry(4, "hurricane_of_light", "Hurricane of Light", HandOfGodDawn.HURRICANE_SEQUENCE, HandOfGodDawn.HURRICANE_COST,
                HandOfGodDawn.HURRICANE_COOLDOWN, HandOfGodDawn.HURRICANE_LENGTH, true, HandOfGodDawn::hurricaneOfLight, null);

        // ---- key 5: Protection / Damage Transference / the mend
        MobAbilities.register(MobAbility.of(P, 5, MobAbility.NORMAL)
                .cost(HandOfGod.PROTECTION_COST)
                .cooldown(Math.max(HandOfGod.PROTECTION_COOLDOWN, HandOfGod.PROTECTION_TICKS + 200))
                .when((mob, target, sl) -> !Dome.isRaised(mob, HandOfGod.DOME))
                .cast(MobSpells.aimedCore((mob, sl) -> {
                    if (Beyonder.hasSequence(mob, HandOfGod.CAGE_SEQUENCE)) HandOfGod.cage(mob, sl);
                    else HandOfGod.protection(mob, sl);
                })));
        MobAbilities.register(MobAbility.of(P, 5, MobAbility.SNEAK).named("Damage Transference")
                .cost(HandOfGod.TRANSFER_COST).cooldown(HandOfGod.TRANSFER_COOLDOWN)
                .when((mob, target, sl) -> !Spells.alliesAround(mob, sl, mob.position(), HandOfGod.TRANSFER_RADIUS).isEmpty())
                .cast((mob, target, sl) -> {
                    List<LivingEntity> allies = Spells.alliesAround(mob, sl, mob.position(), HandOfGod.TRANSFER_RADIUS);
                    if (allies.isEmpty()) return false;
                    HandOfGod.transference(mob, sl, allies);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 5, MobAbility.SNEAK).entry("mend").named("Protection (mended)")
                .sequence(HandOfGod.WARD_SEQUENCE)
                .cost(HandOfGod.MEND_COST).cooldown(MEND_MOB_COOLDOWN)
                .when((mob, target, sl) -> Dome.isRaised(mob, HandOfGod.DOME) && Dome.isInside(mob, HandOfGod.DOME, mob)
                        && mob.getHealth() < mob.getMaxHealth() * MEND_MOB_HURT)
                .cast(MobSpells.aimedCore(HandOfGod::mendDome)));

        // ---- key 6: the Demon Hunting wheel
        entry(6, "eye", "Eye of Demon Hunting", HandOfGodDemonHunting.EYE_SEQUENCE, HandOfGodDemonHunting.EYE_COST,
                HandOfGodDemonHunting.EYE_COOLDOWN + HandOfGodDemonHunting.EYE_TICKS, HandOfGodDemonHunting.EYE_RANGE, true,
                HandOfGodDemonHunting::eyeOfDemonHunting, (mob, target, sl) -> !HandOfGodDemonHunting.eyeOpen(mob));
        entry(6, "alchemy", "Alchemy", HandOfGodDemonHunting.ALCHEMY_SEQUENCE, HandOfGodDemonHunting.ALCHEMY_COST,
                HandOfGodDemonHunting.ALCHEMY_COOLDOWN, HandOfGodDemonHunting.ALCHEMY_RANGE, true, HandOfGodDemonHunting::alchemy,
                (mob, target, sl) -> !WeaponCoat.isArmed(mob, HandOfGodDemonHunting.COAT));
        entry(6, "mind_concealment", "Mind Concealment", HandOfGodDemonHunting.MIND_SEQUENCE, HandOfGodDemonHunting.MIND_COST,
                HandOfGodDemonHunting.MIND_COOLDOWN, 16.0, false, HandOfGodDemonHunting::mindConcealment,
                (mob, target, sl) -> !HandOfGodDemonHunting.mindConcealed(mob));
        entry(6, "giant_form", "Giant Form", HandOfGodDemonHunting.GIANT_SEQUENCE, HandOfGodDemonHunting.GIANT_COST,
                HandOfGodDemonHunting.GIANT_COOLDOWN, 16.0, false, HandOfGodDemonHunting::giantForm,
                (mob, target, sl) -> !GiantForm.isGiant(mob));                              // never to end it

        // ---- key 7: the Silver and Twilight wheel
        entry(7, "mercury", "Mercury Liquefaction", HandOfGodTwilight.MERCURY_SEQUENCE, HandOfGodTwilight.MERCURY_COST,
                HandOfGodTwilight.MERCURY_COOLDOWN, HandOfGodTwilight.MERCURY_RANGE, true, HandOfGodTwilight::mercuryLiquefaction,
                (mob, target, sl) -> !HandOfGodTwilight.isLiquid(mob));                     // never to re-form early
        entry(7, "silver_rapier", "Silver Rapier", HandOfGodTwilight.RAPIER_SEQUENCE, HandOfGodTwilight.RAPIER_COST,
                HandOfGodTwilight.RAPIER_COOLDOWN, HandOfGodTwilight.RAPIER_RANGE, true, HandOfGodTwilight::silverRapier, null);
        entry(7, "light_concealment", "Light Concealment", HandOfGodTwilight.LIGHT_SEQUENCE, HandOfGodTwilight.LIGHT_COST,
                HandOfGodTwilight.LIGHT_COOLDOWN, HandOfGodTwilight.LIGHT_RANGE, true, HandOfGodTwilight::lightConcealment,
                (mob, target, sl) -> mob.distanceTo(target) > 8.0);                          // to get at a target far off
        entry(7, "twilight_sword", "Twilight Sword", HandOfGodTwilight.SWORD_SEQUENCE, HandOfGodTwilight.SWORD_COST,
                HandOfGodTwilight.SWORD_COOLDOWN, HandOfGodTwilight.SWORD_RANGE, true, HandOfGodTwilight::twilightSword, null);
        entry(7, "divine_right_hand", "Divine Right Hand", HandOfGodTwilight.HAND_SEQUENCE, HandOfGodTwilight.HAND_COST,
                HandOfGodTwilight.HAND_COOLDOWN, HandOfGodTwilight.HAND_RANGE, true, HandOfGodTwilight::divineRightHand, null);
        entry(7, "passage_of_time", "Passage of Time", HandOfGodTwilight.TIME_SEQUENCE, HandOfGodTwilight.TIME_COST,
                HandOfGodTwilight.TIME_COOLDOWN, HandOfGodTwilight.TIME_RADIUS, false, HandOfGodTwilight::passageOfTime, null);
        entry(7, "decay", "Decay", HandOfGodTwilight.DECAY_SEQUENCE, HandOfGodTwilight.DECAY_COST,
                HandOfGodTwilight.DECAY_COOLDOWN, HandOfGodTwilight.DECAY_RANGE, true, HandOfGodTwilight::decay, null);
        entry(7, "divine_kingdom", "Divine Kingdom", HandOfGodTwilight.KINGDOM_SEQUENCE, HandOfGodTwilight.KINGDOM_COST,
                Math.max(HandOfGodTwilight.KINGDOM_COOLDOWN, HandOfGodTwilight.KINGDOM_TICKS + 600), 32.0, false,
                HandOfGodTwilight::divineKingdom,
                (mob, target, sl) -> !Zone.isOpen(mob, HandOfGodTwilight.KINGDOM_ZONE));    // never to end it
    }

    /** One wheel entry as an ability of its own (aimed at its target; a refusal spends nothing). */
    private static void entry(int key, String entry, String name, int sequence, int cost, int cooldown, double range,
                              boolean sight, MobSpells.Entry cast, MobAbility.When when) {
        MobAbility ability = MobAbility.of(HandOfGod.PATHWAY, key, MobAbility.NORMAL).entry(entry).named(name)
                .sequence(sequence).cost(cost).cooldown(Math.max(100, cooldown))
                .range(range).cast(MobSpells.aimedEntry(cast));
        if (sight) ability.sight();
        if (when != null) ability.when(when);
        MobAbilities.register(ability);
    }
}
