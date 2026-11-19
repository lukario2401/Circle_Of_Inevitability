package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.conqueror.ConquerorCombos;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.dark_angel.DarkAngelCombos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.stream.Stream;

/** Two-key patterns registered by pathways. Slots are the seven remappable ability keys. */
public final class AbilityCombos {
    public enum Input {
        CHORD(3), SEQUENCE(8);

        private final int window;

        Input(int window) {
            this.window = window;
        }

        public int window() {
            return window;
        }
    }

    @FunctionalInterface
    public interface Prepare {
        /** Checks and captures the target. Returns null to refuse the cast before charging spirituality. */
        Runnable prepare(ServerPlayer player, ServerLevel level);
    }

    public record Combo(String pathway, String key, String name, int sequence, Input input, int first, int second,
                        float cost, int cooldown, Prepare prepare) {
        public Combo {
            if (first < 1 || first > 7 || second < 1 || second > 7 || first == second) {
                throw new IllegalArgumentException("A combo needs two distinct ability slots from 1 to 7");
            }
        }

        public String id() {
            return "combo/" + pathway + "/" + key;
        }

        public boolean startsWith(int slot) {
            return first == slot || input == Input.CHORD && second == slot;
        }

        public boolean matches(int start, int finish, boolean released, long elapsed) {
            if (elapsed >= input.window() || released != (input == Input.SEQUENCE)) return false;
            return first == start && second == finish || input == Input.CHORD && second == start && first == finish;
        }
    }

    private static final List<Combo> ALL = Stream.of(ConquerorCombos.ALL, DarkAngelCombos.ALL)
            .flatMap(List::stream).toList();

    private AbilityCombos() {}

    public static List<Combo> startingWith(String pathway, int sequence, int slot) {
        return ALL.stream().filter(combo -> available(combo, pathway, sequence) && combo.startsWith(slot)).toList();
    }

    public static boolean usesSlot(String pathway, int sequence, int slot) {
        return ALL.stream().anyMatch(combo -> available(combo, pathway, sequence) && (combo.first() == slot || combo.second() == slot));
    }

    private static boolean available(Combo combo, String pathway, int sequence) {
        return sequence <= combo.sequence() && combo.pathway().equals(pathway);
    }
}
