package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.dark_angel;

import net.lukario.circleofinevitability.menu.AbilityGuide;
import net.lukario.circleofinevitability.menu.AbilityGuide.Card;
import net.lukario.circleofinevitability.menu.AbilityGuide.Gesture;

import java.util.ArrayList;
import java.util.List;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.dark_angel.DarkAngel.*;
import static net.lukario.circleofinevitability.menu.AbilityGuide.Gesture.*;

public final class DarkAngelGuide {
    private DarkAngelGuide() {}

    public static AbilityGuide guide() {
        List<Card> cards = new ArrayList<>();
        cards.add(slot("Divination", "divination", 1, PRESS, DIVINATION_SEQUENCE, DIVINATION_COST,
                "Reveal nearby enemies and mark the strongest.",
                "After 1.1s, enemies within 24 blocks glow for 10s. The one with the most maximum health takes 25% more damage for 10s. "
                        + "Reports its health, distance and the number found. The ordinary Sequence 9 bar holds 20."));
        cards.add(slot("Whispers", "whispers", 2, PRESS, WHISPERS_SEQUENCE, WHISPERS_COST,
                "Wear down one enemy and disrupt its pursuit.",
                "Aim within 24 blocks. Deal 4 damage each second for 8s and refresh Weakness I and Slowness I. "
                        + "Mobs lose track of you. Whispering to another enemy ends your previous whispers."));
        cards.add(slot("Shadow Binding", "shadow_lurking", 3, PRESS, BINDING_SEQUENCE, BINDING_COST,
                "Pin enemies caught in a circle of shadows.",
                "Aim at ground within 20 blocks. After a 0.5s warning, catch enemies within 2.5 blocks. "
                        + "The shadows grip 0.7s later for 14 damage and a 2.3s movement root. While lurking, this key instead brings you out for free."));
        cards.add(slot("Flesh Bomb", "flesh_cloak", 4, HOLD_RELEASE, BOMB_SEQUENCE, BOMB_COST,
                "Charge a sticky bomb for a wider, harder blast.",
                "Pay 1,500 on press and every 2s held. Release to throw up to 24 blocks. Fires automatically at 6s or at the last funded tier. "
                        + "At 0/2/4/6s deal 36/72/108/144 at the center. Radius grows linearly from 4 to 7 blocks. "
                        + "A stuck target takes full damage after 2s. Terrain or end of range bursts immediately. Half damage at the rim, Wither I for 4s."));
        cards.add(slot("Grazing", "soul", 5, PRESS, GRAZING_SEQUENCE, Card.VARIABLE_COST,
                "Cast the ability in your selected soul slot.",
                "Cast the stored normal or sneak form at 3× its own spirituality cost, bypassing its Sequence gate. "
                        + "The soul stays in its slot. Select an occupied slot with the sneak wheel."));
        cards.add(slot("Cull of Spiritual Flesh", "replace", 6, PRESS, CULL_SEQUENCE, CULL_COST,
                "Sweep through enemies and strip their defenses.",
                "After 0.1s, hit enemies within 4.5 blocks in a 150° horizontal arc for 50 damage each. "
                        + "Survivors get Weakness I and take 30% more damage for 8s."));
        cards.add(slot("Degeneration", "whispers", 7, PRESS, DEGENERATION_SEQUENCE, DEGENERATION_COST,
                "Strip a target's buffs and rot it from within.",
                "Aim within 28 blocks. After 0.5s deal 20, remove beneficial effects and block healing for 8s. "
                        + "Apply Wither I and deal 6 damage per second. A mob turns on another mob within 12 blocks. Players gain Weakness II."));

        cards.add(slot("Ritual Circle", "ritual", 1, SNEAK, DIVINATION_SEQUENCE, 0,
                "Trade blood for a circle that restores spirituality.",
                "Requires more than 5 health and costs 4 health. One circle lasts 10s. Every 2s inside its 8-block radius "
                        + "and within 2 blocks of its height, regain 8% of maximum spirituality and gain Strength II and Resistance II for 3s."));
        cards.add(slot("Listen", "listen", 2, SNEAK, WHISPERS_SEQUENCE, LISTEN_COST,
                "Track nearby enemies by their glow.",
                "For 15s, listen at your position immediately and every 2s. Enemies within 16 blocks glow through walls for 2.5s per pulse. A warning marks mobs hunting you."));
        cards.add(slot("Shadow Lurking", "shadow_lurking", 3, SNEAK, BINDING_SEQUENCE, LURK_COST,
                "Hide in shadow and prepare an ambush.",
                "Conceal yourself with Speed II for up to 8s. Press the binding again to rise for free. A normal attack reveals you. "
                        + "Your first empowered hit while lurking or within 2s of emerging adds 25 damage and roots the victim for 1s."));
        cards.add(slot("Flesh Cloak", "flesh_cloak", 4, SNEAK, BOMB_SEQUENCE, CLOAK_COST,
                "Wrap yourself in flesh to heal and soften blows.",
                "For 15s take 40% less damage and heal 1 health every 0.5s. Recast to remove for free. "
                        + "With Black Armor active, their reductions multiply and you take 21% of incoming damage."));
        cards.add(slot("Grazing Wheel", "replace", 5, SNEAK, GRAZING_SEQUENCE, 0,
                "Select a stored soul or replace it at a corpse.",
                "The wheel shows your seven personal slots. Select an occupied slot for free. Within 4 blocks of your waiting soul, "
                        + "choose which slot it replaces. Stay within 6 blocks to confirm. Replacing also selects the new soul."));
        cards.add(slot("Black Armor", "black_armor", 6, SNEAK, CULL_SEQUENCE, ARMOR_COST,
                "Grow armored plates and brace against knockback.",
                "For 20s, take 65% less damage, grow 25% larger, gain +0.6 knockback resistance and Strength II. Recast to remove for free."));
        cards.add(slot("Trinity Body", "trinity", 7, SNEAK, DEGENERATION_SEQUENCE, TRINITY_COST,
                "Fight beside two bodies that can die in your place.",
                "For up to 20s deal 65% more damage. Each of two bodies can absorb a lethal blow, restoring you to 50% maximum health. "
                        + "Losing the second ends the form. Damage that bypasses the death ward still kills."));

        for (var combo : DarkAngelCombos.ALL) {
            var copy = switch (combo.key()) {
                case "dark_angel_hangmans_snare" -> new Copy("shadow_lurking", "Find and bind the strongest nearby enemy.",
                        "Choose the eligible enemy with the most maximum health within 24 blocks, nearest on ties. Reveal for 3.7s. After 0.7s, root movement for 3s.");
                case "dark_angel_choir_of_flesh" -> new Copy("whispers", "Make an enemy the voice of a weakening chorus.",
                        "Aim within 24 blocks. After 1s the source and enemies within 6 blocks of it get Weakness II for 5s.");
                case "dark_angel_carrion_bell" -> new Copy("listen", "Plant a bell that pulls nearby enemies together.",
                        "Aim at solid ground within 20 blocks. One bell lasts 20s and arms in 0.1s. An enemy within 1.5 blocks triggers it. "
                                + "Pull enemies within 6 blocks toward the bell and reveal them for 3s.");
                case "dark_angel_crimson_tithe" -> new Copy("ritual", "Mark a target to collect health with Flesh Bomb.",
                        "Aim within 24 blocks. For 5s your next damaging Flesh Bomb blast on that target consumes your mark. "
                                + "Heal for 25% of health and absorption actually lost, up to 12 health.");
                case "dark_angel_shepherds_passage" -> new Copy("shadow_lurking", "Step through a shadow behind your target.",
                        "Aim within 16 blocks. After 0.2s move to safe ground behind the target. If the landing fails or the target slips out of range, regain the cost and cooldown.");
                case "dark_angel_black_procession" -> new Copy("black_armor", "Raise a line of ribs to launch enemies.",
                        "Find ground 3, 6 and 9 blocks ahead. Ribs rise 0.25s apart. Each launches enemies within 2.2 blocks. Deal 32 damage once per enemy across all ribs.");
                case DarkAngelCombos.REFRAIN -> new Copy("trinity", "Echo your next Whispers, Shadow Binding or Flesh Bomb.",
                        "Cast an eligible normal form within 5s. It repeats after 0.4s for half damage. The echo halves the bind's root, the bomb's Wither or each whisper's debuff duration.");
                default -> throw new IllegalStateException("Missing Dark Angel guide entry for " + combo.key());
            };
            cards.add(Card.combo("Combos", combo, "hanged_man/" + copy.icon(), copy.summary(), copy.details()));
        }
        cards.add(new Card("Passives", "Soul Harvest", "hanged_man/soul", "Collect abilities from Beyonders you kill.",
                "Killing a player or mob with an eligible pathway ability stores one random unlocked, takeable form in the first free slot. "
                        + "All seven slots survive death. When full, the soul waits at the corpse for 7.5s for you to choose a replacement.",
                GRAZING_SEQUENCE, 0, 0, PASSIVE, 0, 0));
        return new AbilityGuide(PATHWAY, cards);
    }

    private record Copy(String icon, String summary, String details) {}

    private static Card slot(String name, String icon, int key, Gesture gesture, int sequence, int cost, String summary, String details) {
        return new Card(gesture == SNEAK ? "Sneak" : "Abilities", name, "hanged_man/" + icon, summary, details,
                sequence, cost, 0, gesture, key, 0);
    }
}
