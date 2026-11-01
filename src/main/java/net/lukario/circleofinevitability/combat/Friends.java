package net.lukario.circleofinevitability.combat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Personal, one-way allies. Forge saves PlayerPersisted and carries it across respawns. */
public final class Friends {
    private static final String KEY = "circle_of_inevitability_friends";

    public record Entry(UUID id, String name, boolean player) {}

    private Friends() {}

    private static CompoundTag data(Player owner) {
        return owner.getPersistentData().getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG).getCompoundOrEmpty(KEY);
    }

    private static void save(Player owner, CompoundTag friends) {
        CompoundTag kept = owner.getPersistentData().getCompoundOrEmpty(ServerPlayer.PERSISTED_NBT_TAG);
        kept.put(KEY, friends);
        owner.getPersistentData().put(ServerPlayer.PERSISTED_NBT_TAG, kept);
    }

    public static boolean contains(Player owner, Entity other) {
        return data(owner).contains(other.getUUID().toString());
    }

    /** False for a duplicate or the owner themself. Names are labels; identity always uses UUID. */
    public static boolean add(Player owner, UUID id, String name, boolean player) {
        CompoundTag friends = data(owner);
        if (id.equals(owner.getUUID()) || friends.contains(id.toString())) return false;
        CompoundTag entry = new CompoundTag();
        entry.putString("name", name);
        entry.putBoolean("player", player);
        friends.put(id.toString(), entry);
        save(owner, friends);
        return true;
    }

    public static boolean remove(Player owner, UUID id) {
        CompoundTag friends = data(owner);
        if (!friends.contains(id.toString())) return false;
        friends.remove(id.toString());
        save(owner, friends);
        return true;
    }

    public static List<Entry> entries(Player owner) {
        List<Entry> result = new ArrayList<>();
        CompoundTag friends = data(owner);
        for (String key : friends.keySet()) {
            try {
                CompoundTag entry = friends.getCompoundOrEmpty(key);
                result.add(new Entry(UUID.fromString(key), entry.getStringOr("name", key), entry.getBooleanOr("player", false)));
            } catch (IllegalArgumentException ignored) {
                // A malformed saved entry must not prevent managing the remaining friends.
            }
        }
        result.sort(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER).thenComparing(Entry::id));
        return List.copyOf(result);
    }
}
