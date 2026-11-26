package net.lukario.circleofinevitability.item.artifacts.storing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The slots of an ability-storing artifact. Each slot has a cap: the strongest Sequence it may hold (9 weakest ..
 * 0 strongest), so a cap of 5 holds Sequence 5, 6, 7, 8 or 9 abilities and a cap of 0 holds anything.
 *
 *   AbilitySlots.of(5, 5)                       // five slots, Sequence 5 or weaker
 *   AbilitySlots.of(2, 2).and(1, 4).and(6, 5)   // two up to Sequence 2, one up to 4, six up to 5
 *   AbilitySlots.any(11)                        // eleven slots, any Sequence
 *
 * Stored abilities are not tied to a slot: whatever is stored only has to fit somewhere, the strongest first.
 */
public record AbilitySlots(List<Integer> caps) {
    public static final int ANY_SEQUENCE = 0;

    public AbilitySlots {
        caps = caps.stream().sorted().toList();                    // the most permissive first
    }

    /** 'count' slots that hold Sequence 'strongest' or weaker. */
    public static AbilitySlots of(int count, int strongest) {
        return new AbilitySlots(List.of()).and(count, strongest);
    }

    /** 'count' slots that hold anything. */
    public static AbilitySlots any(int count) {
        return of(count, ANY_SEQUENCE);
    }

    /** These slots plus 'count' more that hold Sequence 'strongest' or weaker. */
    public AbilitySlots and(int count, int strongest) {
        List<Integer> more = new ArrayList<>(caps);
        for (int i = 0; i < count; i++) more.add(Math.clamp(strongest, 0, 9));
        return new AbilitySlots(more);
    }

    public int size() {
        return caps.size();
    }

    /** True if abilities of all these Sequences can be stored at once. */
    public boolean fits(Collection<Integer> sequences) {
        return free(sequences) != null;
    }

    /**
     * The caps of the slots left empty once these Sequences are stored, most permissive first, or null when they do
     * not fit. Each ability, the strongest first, takes the tightest slot that accepts it, so the free slots shown are
     * the most useful ones (each ability accepts every slot a stronger one does, so this never fails when a fit exists).
     */
    public List<Integer> free(Collection<Integer> sequences) {
        if (sequences.size() > caps.size()) return null;
        List<Integer> left = new ArrayList<>(caps);
        for (int sequence : sequences.stream().sorted().toList()) {
            int best = -1;
            for (int i = 0; i < left.size(); i++) if (left.get(i) <= sequence) best = i;   // sorted: the last is the tightest
            if (best < 0) return null;
            left.remove(best);
        }
        return left;
    }

    /** The strongest Sequence that could still be added to these, or -1 when nothing fits (full). */
    public int strongestAddable(Collection<Integer> sequences) {
        List<Integer> left = free(sequences);
        return left == null || left.isEmpty() ? -1 : left.getFirst();
    }

    /** "5 slots: Sequence 5 or weaker", "9 slots: 2 up to Sequence 2, 1 up to Sequence 4, 6 up to Sequence 5". */
    public String describe() {
        Map<Integer, Integer> counts = new TreeMap<>(Comparator.naturalOrder());
        for (int cap : caps) counts.merge(cap, 1, Integer::sum);
        if (counts.size() == 1) {
            int cap = caps.getFirst();
            return size() + " slots: " + (cap == ANY_SEQUENCE ? "any Sequence" : "Sequence " + cap + " or weaker");
        }
        List<String> parts = new ArrayList<>();
        counts.forEach((cap, count) -> parts.add(count + (cap == ANY_SEQUENCE ? " of any Sequence" : " up to Sequence " + cap)));
        return size() + " slots: " + String.join(", ", parts);
    }

    /** "Seq 5+" for a slot cap; "any" for ANY_SEQUENCE. */
    public static String capLabel(int cap) {
        return cap == ANY_SEQUENCE ? "any Sequence" : "Sequence " + cap + " or weaker";
    }
}
