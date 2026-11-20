package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.conqueror;

import net.lukario.circleofinevitability.menu.AbilityGuide;
import net.lukario.circleofinevitability.menu.AbilityGuide.Card;
import net.lukario.circleofinevitability.menu.AbilityGuide.Gesture;

import java.util.ArrayList;
import java.util.List;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.conqueror.Conqueror.*;
import static net.lukario.circleofinevitability.menu.AbilityGuide.Gesture.*;

public final class ConquerorGuide {
    private static final String ICON = "ability_menus/";
    private static final String BLAST = " Damage falls linearly from center to rim. Knocks enemies back and burns them for 4s.";
    private static final String FUSE = " The fuse requires you to stay alive in the same dimension.";
    private static final String HUNTER_COST = " At Sequence 9 your ordinary spirituality bar holds 20.";
    private static final String TRAP = "Set on solid ground within 16 blocks. Arms after 1s and expires after 60s. "
            + "When sprung, deals 40 damage and roots enemies within 1.5 blocks for 3s.";

    private ConquerorGuide() {}

    public static AbilityGuide guide() {
        List<Card> cards = new ArrayList<>();
        cards.add(slot("Hunter's Trap", "apocalypse/trip_web", TRAP_SEQUENCE, 1, PRESS, TRAP_COST,
                "Set a hidden trap to catch and root enemies.",
                TRAP + " A fourth replaces your oldest Hunter's Trap." + HUNTER_COST));
        cards.add(slot("Provocation", "thunder_god/wrath", PROVOKE_SEQUENCE, 2, PRESS, PROVOKE_COST,
                "Draw a mob's aggression and expose its weakness.",
                "Aim within 24 blocks. The target takes 25% more damage for 10s. Mobs also turn on you. "
                        + "Players take the vulnerability. Recasting refreshes it."));
        cards.add(slot("Fire Ravens", "filthy_monarch/flames_of_the_abyss", RAVENS_SEQUENCE, 3, PRESS, RAVENS_COST,
                "Send a flock of homing fire ravens after visible prey.",
                "Six ravens launch 0.1s apart at visible enemies within 24 blocks, nearest first. "
                        + "Each hunts for up to 4s, deals 10 damage on contact and burns its victim for 4s."));
        cards.add(slot("Flame Transformation", "white_angel/spear_of_light", FLAME_SEQUENCE, 4, PRESS, FLAME_COST,
                "Ride a steerable flame lance. Sneak to dismount.",
                "Fly at 1.2 blocks/tick, turning up to 15°/tick. Stop on collision, root or sneak. "
                        + "Enemy impact deals 30. Departure and arrival each blast for 16–8 damage within 2.5 blocks." + BLAST
                        + " From 0–100% digestion, range grows linearly: Seq 6, 10–120 blocks. Seq 5, 120–360. "
                        + "Seq 4, 360–3,600. Seq 3–0 have unlimited range and a 1-hour ride limit."));
        cards.add(slot("Cull", "nature_walker/reap", CULL_SEQUENCE, 5, HOLD_RELEASE, CULL_COST,
                "Charge a scythe strike. Reap wounded or marked prey.",
                "Release to aim within 20 blocks. A miss selects the nearest visible enemy within 30° of aim. "
                        + "Root for 0.8s, then add one of your marks. Deal 45 per mark, rising linearly to 90 at 2s held. "
                        + "Mark caps: 25/30/35/40 at Seq 5/4/3/2–0. One drops every 20s. Each hit restarts that timer. "
                        + "At 30% health or less on impact, deal 10× max health + 1,000 instead. Survivors get Wither II for 5s."));
        cards.add(slot("Galvanization", "hand_of_god/dawn_armor", GALV_SEQUENCE, 6, PRESS, GALV_COST,
                "Wear an iron mantle that halves damage and returns blows.",
                "For 20s, gain Strength II and Fire Resistance. Take 50% less damage. A direct living attacker "
                        + "within 6 blocks takes 30% of the incoming blow before reduction. Recast to remove the mantle for free."));
        cards.add(slot("Fog of War", "abomination/bane_mist", FOG_SEQUENCE, 7, PRESS, FOG_COST,
                "Blind and weaken enemies in fog. Move faster inside.",
                "A stationary 16-block field lasts 20s and pulses every 0.5s. Blindness limits mob sight to 4 blocks. "
                        + "Enemies gain Slowness I, Weakness I and 25% vulnerability, sharing Provocation's cap. "
                        + "You gain Speed I inside. Recast to lift the fog for free."));

        cards.add(slot("Explosive Charge", "filthy_monarch/blood_burst", TRAP_SEQUENCE, 1, SNEAK, CHARGE_COST,
                "Plant a powder keg with a short fuse.",
                "Aim at solid ground within 16 blocks. After 2s, blast a 4-block radius for 48–24 damage." + BLAST + FUSE + HUNTER_COST));
        cards.add(slot("Repugnant Aura", "prince_of_abolition/frenzy", PROVOKE_SEQUENCE, 2, SNEAK, AURA_COST,
                "Turn nearby enemy mobs against each other.",
                "Requires at least two enemy mobs within 10 blocks. Up to the nearest 12 fight one another for 10s. "
                        + "Marionettes are excluded. With too few targets you spend nothing."));
        cards.add(slot("Dash", "prince_of_abolition/step", RAVENS_SEQUENCE, 3, SNEAK, DASH_COST,
                "Leap along your aim and land safely.",
                "Horizontal launch speed is 4× the horizontal part of your look direction. Vertical speed is "
                        + "2× the vertical part + 0.2 blocks/tick. Protects your next landing for up to 3s. The fire trail is cosmetic."));
        cards.add(slot("Misdirection", "worm_of_time/misdirect", FLAME_SEQUENCE, 4, SNEAK, DECOY_COST,
                "Vanish and leave an explosive flame double.",
                "Gain concealment and Speed III for 5s. A normal melee attack reveals you early. "
                        + "After 2s the double blasts a 3.5-block radius for 36–18 damage." + BLAST + FUSE));
        cards.add(slot("Fireball", "filthy_monarch/sulfur_fireball", CULL_SEQUENCE, 5, SNEAK, FIREBALL_COST,
                "Fire a straight shot that bursts on impact.",
                "Fires on press at 1.4 blocks/tick. At the first enemy, block or end of its 48-block range, "
                        + "blast a 3.5-block radius for 40–20 damage." + BLAST));
        cards.add(slot("Iron Charge", "filthy_monarch/lawless_lunge", GALV_SEQUENCE, 6, SNEAK, IRON_CHARGE_COST,
                "Rush forward and throw enemies aside.",
                "Charge horizontally for 8 ticks at 1.8 blocks/tick, about 14.4 blocks unobstructed. "
                        + "Deal 80 once to each enemy within 1.8 blocks and knock them away. Fall protection lasts up to 2.4s."));
        cards.add(slot("War Banner", "hand_of_god/divine_kingdom", FOG_SEQUENCE, 7, SNEAK, BANNER_COST,
                "Raise a banner that rallies allies and weakens enemies.",
                "One banner at your position for 30s. Every 0.5s within 10 blocks, you and allies gain Strength II, "
                        + "Resistance I and Regeneration I for 3s. Enemies gain Weakness II for 2s. A new banner replaces yours."));

        for (var combo : ConquerorCombos.ALL) {
            var copy = switch (combo.key()) {
                case ConquerorCombos.RAVEN_TRAP -> new Copy("filthy_monarch/flame_cage", "Spring a trap and unleash three fire ravens.",
                        TRAP + " Keep up to three Raven Traps in addition to your Hunter's Traps. Launch three ravens "
                                + "0.1s apart at visible prey within 12 blocks, prioritizing the triggering enemy. Each deals 10 and burns for 4s.");
                case ConquerorCombos.ASHEN_BASTION -> new Copy("hand_of_god/dawn_armor", "Shelter allies under a dome of ash.",
                        "Stand on solid ground. The 5-block dome lasts 6s, stops incoming hostile or ownerless projectiles "
                                + "and wards you and allies inside against new harmful effects. Enemies can walk through.");
                case "conqueror_executioners_challenge" -> new Copy("nature_walker/reap", "Provoke your prey, then strike with Cull.",
                        "Use Cull's targeting within 20 blocks. Apply Provocation for 10s, then root for 0.8s and add a Cull mark. "
                                + "Deal 45 per mark before the 25% vulnerability. At 30% health or less, use Cull's execute damage "
                                + "(10× max health + 1,000). Survivors get Wither II for 5s.");
                case "conqueror_iron_quake" -> new Copy("nature_walker/earthquake", "Send a shockwave outward along the ground.",
                        "Cast while grounded. The wave reaches 8 blocks in 0.6s, catching enemies within 2 blocks vertically. "
                                + "Deal 70 once to each enemy, knock them back and apply Slowness II for 3s.");
                default -> throw new IllegalStateException("Missing Conqueror guide entry for " + combo.key());
            };
            cards.add(Card.combo("Combos", combo, ICON + copy.icon(), copy.summary(), copy.details()));
        }
        cards.add(new Card("Combos", "Five-hit Melee Chain", ICON + "filthy_monarch/sword_of_lava",
                "Chain melee attacks to cycle through five slash effects.",
                "Attack living targets with at least 80% attack strength. Cross-cut, ground rend, sweep, cleave and ring "
                        + "play in order, using ordinary melee damage. A gap longer than 2s resets the chain.",
                9, 0, 0, ATTACK, 0, 0));
        return new AbilityGuide(PATHWAY, cards);
    }

    private record Copy(String icon, String summary, String details) {}

    private static Card slot(String name, String icon, int sequence, int slot, Gesture gesture, int cost,
                             String summary, String details) {
        return new Card(gesture == SNEAK ? "Sneak" : "Abilities", name, ICON + icon, summary, details,
                sequence, cost, 0, gesture, slot, 0);
    }
}
