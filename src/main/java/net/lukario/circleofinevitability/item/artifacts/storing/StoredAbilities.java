package net.lukario.circleofinevitability.item.artifacts.storing;

import net.lukario.circleofinevitability.combat.AbilityCatalog;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The abilities stored inside one artifact stack (its custom data), as stable AbilityCatalog ids, so they travel with
 * the item: drop it, trade it or put it in a chest and the next bearer finds the same powers inside.
 *
 *   List<AbilityCatalog.Power> inside = StoredAbilities.powers(stack);
 *   StoredAbilities.Result r = StoredAbilities.add(stack, slots, power);   // ADDED, or why not
 *   StoredAbilities.replace(stack, slots, index, power);                   // in place of the one at 'index'
 *   StoredAbilities.selected(stack) / select(stack, index)                  // what right-click casts
 */
public final class StoredAbilities {
    private static final String COUNT = "stored_ability_count";
    private static final String ENTRY = "stored_ability_";
    private static final String SELECTED = "stored_ability_selected";

    public enum Result { ADDED, ALREADY_STORED, TOO_STRONG, FULL, NOT_STORABLE }

    private StoredAbilities() {}

    private static CompoundTag read(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    /** The stored ids, in the order they were taken. Ids the catalog no longer knows are kept (see powers). */
    public static List<String> ids(ItemStack stack) {
        CompoundTag tag = read(stack);
        int count = Math.max(0, tag.getIntOr(COUNT, 0));
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = tag.getStringOr(ENTRY + i, "");
            if (!id.isEmpty()) ids.add(id);
        }
        return ids;
    }

    /** The stored abilities the catalog knows, in order. */
    public static List<AbilityCatalog.Power> powers(ItemStack stack) {
        Map<String, AbilityCatalog.Power> all = AbilityCatalog.all();
        List<AbilityCatalog.Power> found = new ArrayList<>();
        for (String id : ids(stack)) {
            AbilityCatalog.Power power = all.get(id);
            if (power != null) found.add(power);
        }
        return found;
    }

    /** The index of the stored ability with this id in powers(stack), or -1. */
    public static int indexOf(ItemStack stack, String id) {
        List<AbilityCatalog.Power> inside = powers(stack);
        for (int i = 0; i < inside.size(); i++) if (inside.get(i).id().equals(id)) return i;
        return -1;
    }

    public static boolean contains(ItemStack stack, String id) {
        return ids(stack).contains(id);
    }

    /** Replaces the whole list (drops what the catalog does not know) and keeps the selection in range. */
    public static void set(ItemStack stack, List<String> ids) {
        int selected = selected(stack);
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            int before = Math.max(0, tag.getIntOr(COUNT, 0));
            for (int i = 0; i < before; i++) tag.remove(ENTRY + i);
            for (int i = 0; i < ids.size(); i++) tag.putString(ENTRY + i, ids.get(i));
            tag.putInt(COUNT, ids.size());
            tag.putInt(SELECTED, ids.isEmpty() ? 0 : Math.clamp(selected, 0, ids.size() - 1));
        });
    }

    /** The index right-click casts (0 when empty). */
    public static int selected(ItemStack stack) {
        CompoundTag tag = read(stack);
        int count = Math.max(0, tag.getIntOr(COUNT, 0));
        return count == 0 ? 0 : Math.clamp(tag.getIntOr(SELECTED, 0), 0, count - 1);
    }

    public static void select(ItemStack stack, int index) {
        int count = ids(stack).size();
        if (count == 0) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(SELECTED, Math.clamp(index, 0, count - 1)));
    }

    /** The selected ability, or null when nothing (known) is stored. */
    public static AbilityCatalog.Power selectedPower(ItemStack stack) {
        List<AbilityCatalog.Power> inside = powers(stack);
        if (inside.isEmpty()) return null;
        return inside.get(Math.min(selected(stack), inside.size() - 1));
    }

    /** Forgets ids the catalog no longer knows (an ability renamed or removed), so indexes match powers(). */
    public static void clean(ItemStack stack) {
        List<String> ids = ids(stack);
        List<String> known = powers(stack).stream().map(AbilityCatalog.Power::id).toList();
        if (known.size() != ids.size()) set(stack, known);
    }

    /** Whether 'power' could be added as it is now (ADDED means it would be). */
    public static Result check(ItemStack stack, AbilitySlots slots, AbilityCatalog.Power power) {
        if (power == null || !power.cast().copyable()) return Result.NOT_STORABLE;
        List<AbilityCatalog.Power> inside = powers(stack);
        if (inside.stream().anyMatch(p -> p.id().equals(power.id()))) return Result.ALREADY_STORED;
        List<Integer> sequences = new ArrayList<>(inside.stream().map(AbilityCatalog.Power::sequence).toList());
        sequences.add(power.sequence());
        if (slots.fits(sequences)) return Result.ADDED;
        return inside.size() >= slots.size() ? Result.FULL : Result.TOO_STRONG;
    }

    /** Adds it if it fits (and selects it). Returns ADDED, or why it was not added. */
    public static Result add(ItemStack stack, AbilitySlots slots, AbilityCatalog.Power power) {
        Result result = check(stack, slots, power);
        if (result != Result.ADDED) return result;
        List<String> ids = new ArrayList<>(ids(stack));
        ids.add(power.id());
        set(stack, ids);
        select(stack, ids.size() - 1);
        return Result.ADDED;
    }

    /** True if 'power' would fit in place of the stored ability at 'index'. */
    public static boolean canReplace(ItemStack stack, AbilitySlots slots, int index, AbilityCatalog.Power power) {
        if (power == null || !power.cast().copyable() || contains(stack, power.id())) return false;
        List<AbilityCatalog.Power> inside = new ArrayList<>(powers(stack));
        if (index < 0 || index >= inside.size()) return false;
        inside.remove(index);
        List<Integer> sequences = new ArrayList<>(inside.stream().map(AbilityCatalog.Power::sequence).toList());
        sequences.add(power.sequence());
        return slots.fits(sequences);
    }

    /** Puts 'power' in place of the stored ability at 'index' (and selects it). False if it would not fit. */
    public static boolean replace(ItemStack stack, AbilitySlots slots, int index, AbilityCatalog.Power power) {
        if (!canReplace(stack, slots, index, power)) return false;
        List<String> ids = new ArrayList<>(powers(stack).stream().map(AbilityCatalog.Power::id).toList());
        ids.set(index, power.id());
        set(stack, ids);
        select(stack, index);
        return true;
    }

    /** Forgets the stored ability at 'index'. */
    public static void remove(ItemStack stack, int index) {
        List<String> ids = new ArrayList<>(powers(stack).stream().map(AbilityCatalog.Power::id).toList());
        if (index < 0 || index >= ids.size()) return;
        ids.remove(index);
        set(stack, ids);
    }

    /** The Sequences of what is stored, for AbilitySlots. */
    public static List<Integer> sequences(ItemStack stack) {
        return powers(stack).stream().map(AbilityCatalog.Power::sequence).toList();
    }
}
