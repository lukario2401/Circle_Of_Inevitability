package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition;

import net.lukario.circleofinevitability.menu.AbilityGuide;
import net.lukario.circleofinevitability.menu.AbilityGuide.Card;
import net.lukario.circleofinevitability.menu.AbilityGuide.Gesture;
import net.lukario.circleofinevitability.menu.AbilityWheel;

import java.util.ArrayList;
import java.util.List;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolition.*;
import static net.lukario.circleofinevitability.menu.AbilityGuide.Gesture.*;

public final class PrinceOfAbolitionGuide {
    private static final String ICON = "prince_of_abolition/";
    private static final String RECAST = "Repeat your last unlocked pick without a cooldown. With no eligible pick, open the wheel. "
            + "Pay 3× the entry cost at Sequences 9–7, 2× at 6, and 1× from 5 onward.";
    private static final String TRIBUTE = " Offhand gold ingot or emerald: spend one for 50% more duration and a 3-block spread. "
            + "At Sequence 4 even symbolic bribes spread. A Connected target halves the cost.";
    private static final String WHEEL_RULE = " Wheel cooldowns end at Sequence 4.";
    private static final String BASE = "Base values before Sequence Might. ";

    private PrinceOfAbolitionGuide() {}

    public static AbilityGuide guide() {
        List<Card> cards = new ArrayList<>();
        cards.add(slot("Verdict", "compromised", 1, PRESS, VERDICT_SEQUENCE, VERDICT_COST,
                "Strike with a gavel and leave the enemy Compromised.",
                BASE + "Aim within 20 blocks. After 0.5s deal 40 damage and stagger for 0.6s. For 10s the victim takes 25% more damage from you "
                        + "and your Bribe marks and Disorder last 50% longer. At Sequence 9 the ordinary spirituality bar holds 20."));
        cards.add(slot("Brute Force", "rash", 2, PRESS, BRUTE_SEQUENCE, BRUTE_COST,
                "Break the target's guard and scatter nearby enemies.",
                BASE + "Reach 4 blocks. After 0.2s strike for 60 if still in reach, knock the target back and block item use for 5s. "
                        + "Enemies within 2 blocks take 30 and are knocked aside. At Sequence 8 the ordinary bar holds 400."));
        cards.add(recast("Bribe", "avaricious", 3, PrinceOfAbolitionBribes.BRIBE_SEQUENCE));
        cards.add(slot("Distort", "distorted", 4, PRESS, DISTORT_SEQUENCE, DISTORT_COST,
                "Turn a projectile back or redirect an enemy's attacks.",
                BASE + "At 24 blocks, first return a projectile toward its sender. Otherwise twist the target's next 3 blows within 5s "
                        + "onto a nearby ally within 8 blocks, or make them miss. From Sequence 2 an aimed enemy dome takes priority over a creature and traps its own side."));
        cards.add(slot("Paces", "paces", 5, PRESS, DISORDER_SEQUENCE, PACES_COST,
                "Cross the distance and leave a decoy behind.",
                BASE + "Blink up to 24 blocks, or 48 from Sequence 4. Pursuers within 24 head for the decoy, which lasts 4s. "
                        + "An enemy within 1.6 triggers 2s of Disorder in a 3-block radius. You go unnoticed for up to 2s. Sequence 4 also grants 2s of protection from aimed attacks."));
        cards.add(recast("The Fallen", "rebirth", 6, PrinceOfAbolitionFallen.FALLEN_SEQUENCE));
        cards.add(recast("Sovereign", "coronation", 7, PrinceOfAbolitionSovereign.SOVEREIGN_SEQUENCE));

        cards.add(slot("Brainwash", "brainwashed", 1, SNEAK, VERDICT_SEQUENCE, BRAINWASH_COST,
                "Make an enemy lose its will to target you.",
                BASE + "Aim within 16 blocks at a target with at most 150 maximum health. Mobs forget you for 8s. "
                        + "Players lose aimed locks on you and deal 30% less damage to you for 5s. At Sequence 9 the ordinary bar holds 20."));
        cards.add(slot("Iron Will", "iron_will", 2, SNEAK, BRUTE_SEQUENCE, IRON_COST,
                "Shake off mind effects and stand firm.",
                "For 8s, clear mental marks, fear, sleep and nausea. Resist new mental marks, fear and sleep, and gain +1 knockback resistance. "
                        + "At Sequence 8 the ordinary bar holds 400."));
        cards.add(open("Bribe Wheel", "avaricious", 3, PrinceOfAbolitionBribes.BRIBE_SEQUENCE,
                "Choose a bribe or fire Coin Shot.", "Five entries unlock at Sequence 7."));
        cards.add(slot("Seal", "seal", 4, SNEAK, DISTORT_SEQUENCE, SEAL_COST,
                "Close a border that enemies and their shots cannot cross.",
                BASE + "At Sequences 6–5, close a 6-block sphere around yourself after 0.8s for the rest of an 8s cast. "
                        + "From Sequence 4, place a fixed 10×5-block gate up to 24 blocks ahead. It shuts after 0.4s for the rest of a 10s cast."));
        cards.add(slot("Disorder", "disordered", 5, SNEAK, DISORDER_SEQUENCE, DISORDER_COST,
                "Scramble enemy attacks, or free yourself from a hold.",
                BASE + "Aim within 20 blocks. For 6s the target's attacks have a 40% miss chance. Mobs lose their target and players get Nausea. "
                        + "If rooted, frozen, asleep or afraid, remove those holds instead and shake off new ones for 3s. From Sequence 4, gain 2s of protection from aimed attacks."));
        cards.add(open("Fallen Wheel", "rebirth", 6, PrinceOfAbolitionFallen.FALLEN_SEQUENCE,
                "Choose a mausoleum or a technique of the Fallen.", "Nine entries unlock across Sequences 8–4."));
        cards.add(open("Sovereign Wheel", "coronation", 7, PrinceOfAbolitionSovereign.SOVEREIGN_SEQUENCE,
                "Choose a decree, field or sovereign technique.", "Eight entries unlock across Sequences 6–0."));

        wheel(cards, "Bribes", 3, PrinceOfAbolitionBribes.WHEEL);
        wheel(cards, "Fallen", 6, PrinceOfAbolitionFallen.WHEEL);
        wheel(cards, "Sovereign", 7, PrinceOfAbolitionSovereign.WHEEL);

        cards.add(passive("Sequence Might", "magnify", 9, "Each Sequence increases your power and reach.",
                "Gain 5% per Sequence reached, from +5% at 9 to +50% at 0. Multiply outgoing damage, scaled range, area and duration by this factor. "
                        + "Divide incoming damage by the defense factor. Each standing mausoleum adds 5 percentage points to attack and defense."));
        cards.add(passive("Law & Order", "order", 9, "Standing orders reduce incoming damage.",
                "Marks, mausoleums, Seal, Entropy and Abolition can count as orders. Each counted order reduces incoming damage another 2% after defense, up to 18% at nine. "
                        + "Every two orders also add 1s to Majesty's base duration."));
        cards.add(passive("Silver Tongue", "brainwashed", 9, "Get better prices from villagers.",
                "Hero of the Village I refreshes while you follow this pathway."));
        cards.add(passive("Physical Enhancement", "iron_will", 8, "Gain Strength I and two extra hearts.",
                "Increase maximum health by 4 and maintain Strength I from Sequence 8 onward."));
        cards.add(passive("Mental Resistance", "seal", 8, "Recover more quickly from mental marks.",
                "Halve the duration of incoming Prince mental marks, including Brainwash, Charm, Arrogance, Corrosion's greed, kneeling and Bestowment."));
        cards.add(passive("Rebirth", "rebirth", 8, "A mausoleum can save you from a lethal hit.",
                "Recover 50% maximum health and hide, immobilized, for 10s. Rise from your nearest surviving mausoleum and consume it. "
                        + "Enemies within 10 base blocks kneel for 1.5 base seconds. If all mausoleums fall meanwhile, rise where you fell."));
        cards.add(passive("Everyone Has a Price", "avaricious", 7, "Piglins overlook you as a target.",
                "Ordinary piglins cannot acquire you as their attack target."));
        cards.add(passive("Sanctuary", "sanctuary", 7, "Your mausoleums sustain allies and hinder enemies.",
                "Within 8 base blocks, each mausoleum heals you and allies for 1 health per second and grants you an extra 50% of normal spirituality regeneration. Enemies get Slowness I."));
        cards.add(passive("Weakness Detection", "compromised", 6, "Spot a weakness and punish an unaware mob.",
                "Sight marks your aimed enemy within 24 blocks. Your first hit on a mob that has not noticed you and is not targeting you deals 1.5× damage."));
        cards.add(passive("Presence", "kneeling", 5, "Weak hostile mobs hesitate around you.",
                "Within 6 blocks, hostile mobs with at most 30 maximum health gain Slowness I and have a 50% chance to abandon an attempt to target you."));
        cards.add(passive("Favorable Ground", "connect", 4, "Expose hidden enemies nearby.",
                "Every second within 16 blocks, reveal Concealed and Unnoticed enemies. Invisible enemies glow for 2s. Exiled and reviving targets stay hidden."));
        cards.add(passive("Loose Order", "distorted", 2, "Deflect incoming projectiles off course.",
                "Check every other tick for hostile or ownerless projectiles approaching within 10 blocks. Each can drift once, up to 10° sideways with a vertical deflection."));
        cards.add(passive("Beyond the World", "exile", 1, "Suffer less harm from the environment.",
                "Take 50% less damage from sources with no attacker after ordinary pathway defenses. Sources that bypass invulnerability also bypass this protection."));
        cards.add(passive("Their Order & Last Rebirth", "coronation", 0, "Prolong your orders and return even without a mausoleum.",
                "Scaled durations gain a further 25% after Sequence Might. With no mausoleum and at least 3 orders, survive a lethal hit at 25% health. "
                        + "Return where you fell after 10s in exile. Last Rebirth cooldown is 10 minutes."));
        cards.add(new Card("Passives", "Twelve-hit Melee Chain", ICON + "rash", "Cycle through twelve imperial melee flourishes.",
                "Attacks on living targets at 80% attack strength or higher advance the sequence. A gap over 2s resets it. The flourishes use ordinary melee damage.",
                9, 0, 0, ATTACK, 0, 0));
        return new AbilityGuide(PATHWAY, cards);
    }

    private static Card slot(String name, String icon, int key, Gesture gesture, int sequence, int cost, String summary, String details) {
        return new Card(gesture == SNEAK ? "Sneak" : "Abilities", name, ICON + icon, summary, details, sequence, cost, 0, gesture, key, 0);
    }

    private static Card recast(String name, String icon, int key, int sequence) {
        return slot(name, icon, key, PRESS, sequence, Card.VARIABLE_COST, "Repeat your last pick from this wheel.", RECAST);
    }

    private static Card open(String name, String icon, int key, int sequence, String summary, String unlocks) {
        return slot(name, icon, key, SNEAK, sequence, 0, summary, unlocks + " Opening is free. Pay the selected entry's cost." + WHEEL_RULE);
    }

    private static Card passive(String name, String icon, int sequence, String summary, String details) {
        return new Card("Passives", name, ICON + icon, summary, details, sequence, 0, 0, PASSIVE, 0, 0);
    }

    private record Copy(String summary, String details) {}

    private static void wheel(List<Card> cards, String section, int key, AbilityWheel wheel) {
        for (var entry : wheel.entries()) {
            var copy = switch (entry.key()) {
                case "weaken" -> new Copy("Diminish an enemy's attacks and expose it to yours.",
                        "Aim within 16 blocks. For 12s its attacks deal 35% less damage and yours deal 25% more to it." + TRIBUTE);
                case "charm" -> new Copy("Turn a mob's aggression away from you.",
                        "Aim within 16 blocks at up to 150 max health. For 10s mobs avoid targeting you and players deal 50% less to you. "
                                + "Each second a mob has a 5% chance (10% from Sequence 4) to turn on another enemy within 10 for 5s." + TRIBUTE);
                case "arrogance" -> new Copy("Make an overconfident enemy miss its blows.",
                        "Aim within 16 blocks. For 12s its attacks have a 30% miss chance." + TRIBUTE);
                case "connect" -> new Copy("Mark an enemy to extend your aim and discount bribes.",
                        "Aim within 32 blocks. For 30s the target glows and eligible aimed powers can reach it at 3× range. "
                                + "Weaken, Charm and Arrogance cost half against it. An offhand gold ingot or emerald grants 50% more duration.");
                case "coin_shot" -> new Copy("Ricochet a coin from one enemy to another.",
                        "Fire along a 24-block line for 30 damage, then bounce to another enemy within 6 for 15. "
                                + "Spend an offhand gold ingot or emerald for 45 and 22.5 damage and Weaken the first target for 6s.");
                case "raise_mausoleum" -> new Copy("Raise a stele to strengthen you and enable Rebirth.",
                        "Aim at ground within 12 blocks. Costs 100 extra per existing mausoleum. Lasts 120s, or 225s at Sequence 0. "
                                + "Each has 80 health and regenerates 2 per second after 5s unharmed. A tenth replaces your oldest. Each gives +5% attack and defense.");
                case "corrosion" -> new Copy("Spread lasting rot and turn enemy mobs greedy.",
                        "Within 10 blocks, greed lasts 10s: 20% of attacks miss and mobs have a 40% chance each second to retarget within 8. "
                                + "Rot deals 2 damage per second, growing by 1 each second to 40. After 5s it blocks healing until cleansed.");
                case "step" -> new Copy("Travel to the door of one of your mausoleums.",
                        "Aim within 15° of a mausoleum up to 40 blocks away, or use your nearest. Arrive after 0.3s if there is room. "
                                + "From Sequence 4, bring allies within 3 blocks of your departure.");
                case "majesty" -> new Copy("Force nearby enemies to kneel.",
                        "In a 10-block radius, apply Slowness IV and prevent jumping and attacking. Lasts 1.5s plus 1s for every two orders. "
                                + "Non-player enemies with at most 60 max health kneel for at least 6s.");
                case "bestowment" -> new Copy("Force a feeling on a target suited to its condition.",
                        "Aim within 16 blocks for 10s. Below 40% health: Defeated, −60% damage. Ranged: Anxious, 40% misses. Fast: Sluggish, Slowness II and Mining Fatigue II. "
                                + "Heavy brutes: Rash, Haste II, forced movement and +40% damage taken. Otherwise Avaricious: mobs chase valuables within 16, players deal 30% less.");
                case "exploit" -> new Copy("Exploit your footing, an enemy's buffs, or your own timers.",
                        "Airborne: fly for 8s, or moon-leap in your own Entropy from Sequence 2. Grounded: strip benefits, wards and special states from an enemy within 24. "
                                + "Without a target, double remaining beneficial timers, adding up to 30s each and 120s per mausoleum. Free in your own Entropy.");
                case "magnify" -> new Copy("Amplify your next blow or widen a bribe.",
                        "For up to 10s gain 1.5× reach and area. Your next damaging tick deals 2.5×, executing targets of at most 150 max health projected below 25%. "
                                + "A magnified bribe spreads in a 6-block quake. On cast, seize a Connected target within 100, pull up to 10 and root for 2s. Free in your own Entropy.");
                case "mausoleum_salvo" -> new Copy("Order your mausoleums to fire on an enemy group.",
                        "Requires a standing mausoleum and a target within 40 blocks. Each stele fires one 35-damage beam, staggered 0.15s apart, "
                                + "cycling between the target and enemies within 8 blocks.");
                case "mausoleum_blast" -> new Copy("Sacrifice nearby mausoleums in overlapping blasts.",
                        "Detonate each of your mausoleums within 24 blocks. Each is consumed, deals 90 damage in a 5-block radius and throws enemies back. Overlapping blasts can hit again.");
                case "ruin" -> new Copy("Drive a piercing line of rot through enemies.",
                        "Fire after 0.3s along a 24-block line with 0.6-block tolerance. Each enemy takes 70. Rot starts at 5 damage per second, "
                                + "rises by 1.5 each second to 40 and prevents healing after 5s until cleansed.");
                case "frenzy" -> new Copy("Distribute twelve bolts among nearby enemies.",
                        "Within 24 blocks select up to 12, nearest first. After 0.4s fire one bolt per tick, cycling targets. Each deals 25 and applies Disorder for 2s.");
                case "entropy" -> new Copy("Scatter projectiles and disrupt movement in a field.",
                        "Aim at ground within 24 blocks for a 16-block radius, 20s field. Strength builds over 8s. At full power, enemy damage varies from 0.5× to 1.5×, "
                                + "shots scatter up to 30° and slowed enemies have a 30% chance of a shove every 0.5s. Your Exploit and Magnify are free inside.");
                case "abolition" -> new Copy("Cut a pocket of space off from the fight outside.",
                        "Raise a 12-block sphere for 15s. It bars enemies and their projectiles from crossing and blocks attacks and targeting across the boundary. "
                                + "Combat inside continues. Sequence 0 also excludes other owners' outside fields. Recast to close for free.");
                case "exile" -> new Copy("Banish an enemy, shelter an ally or slip away.",
                        "Aim within 24 blocks. Exile enemies for 6s, enemies above 150 maximum health for 2s, or allies for 3s. "
                                + "With no eligible target, leave for 4s and return up to 20 blocks toward your aim.");
                case "inversion" -> new Copy("Turn enemy attacks into healing and healing into harm.",
                        "A 20-block field follows you for 10s. Enemy hits on you or allies inside heal you instead. Enemy healing becomes equal magic damage. "
                                + "Each second, inflict Darkness and remove beneficial effects, turning supported effects into their opposites for up to 10s.");
                case "coronation" -> new Copy("Crown yourself with the might of your mausoleums.",
                        "Requires at least one mausoleum. For 20s double each stele's attack and defense bonus to 10%, gain +1 knockback resistance and pulse Majesty every 5s in 10 blocks.");
                case "decree" -> new Copy("Speak a word of judgment that pierces defenses.",
                        "Aim within 40 blocks. After 0.9s deal 300 true damage plus 30 per standing mausoleum. If a survivor with at most 150 maximum health falls below 40%, "
                                + "strike again for 10× its remaining health and absorption plus 100.");
                default -> throw new IllegalStateException("Missing Prince guide entry for " + entry.key());
            };
            cards.add(Card.wheel(section, key, SNEAK_WHEEL, entry, copy.summary(), BASE + copy.details() + WHEEL_RULE));
        }
    }
}
