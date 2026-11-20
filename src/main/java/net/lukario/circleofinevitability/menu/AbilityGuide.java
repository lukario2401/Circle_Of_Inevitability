package net.lukario.circleofinevitability.menu;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.apocalypse.DemonessGuide;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.conqueror.ConquerorGuide;
import net.lukario.circleofinevitability.combat.AbilityCombos.Combo;
import net.lukario.circleofinevitability.combat.AbilityCombos.Input;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Read-only field guide. Descriptions live with the pathway and bindings are resolved by the viewing client. */
public record AbilityGuide(String title, List<Card> cards) {
    public static final Map<String, Supplier<AbilityGuide>> PATHWAYS = Map.of(
            "Apocalypse", DemonessGuide::guide,
            "Conqueror", ConquerorGuide::guide);

    public AbilityGuide {
        cards = List.copyOf(cards);
    }

    public enum Gesture { PRESS, HOLD_RELEASE, ATTACK, SNEAK, CHORD, HOLD_CHORD, ORDERED, WHEEL, PASSIVE }

    public record Card(String section, String name, String icon, String summary, String details,
                       int sequence, float baseCost, int cooldown, Gesture gesture, int first, int second) {
        public static Card combo(String section, Combo combo, String icon, String summary, String details) {
            Gesture gesture = combo.rule().metered() ? Gesture.HOLD_CHORD
                    : combo.input() == Input.CHORD ? Gesture.CHORD : Gesture.ORDERED;
            if (combo.rule().metered()) {
                var rule = combo.rule();
                details += " Pay " + rule.installmentCost() + " more every " + seconds(rule.intervalTicks())
                        + "s. Fires automatically at " + seconds(rule.maxTicks()) + "s or at the last funded tier.";
            }
            String timing = combo.input() == Input.CHORD ? " Press both within " + combo.input().window() + " ticks."
                    : " Release the first key, then press the second within " + combo.input().window() + " ticks of the first press.";
            return new Card(section, combo.name(), icon, summary, details + timing, combo.sequence(), combo.cost(),
                    combo.cooldown(), gesture, combo.first(), combo.second());
        }

        public static Card wheel(String section, int slot, AbilityWheel.Entry entry, String summary, String details) {
            return new Card(section, entry.name(), entry.icon(), summary, details, entry.sequence(), entry.cost(),
                    entry.cooldown(), Gesture.WHEEL, slot, 0);
        }
    }

    public static String seconds(int ticks) {
        return ticks % 20 == 0 ? Integer.toString(ticks / 20) : Float.toString(ticks / 20f);
    }
}
