package net.lukario.circleofinevitability.status;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * KeySeal: ONE ability key of a player is dead for a while; the others still work. Not taken away (StolenAbilities
 * does that), only made unusable. The Hand Of Order's Deprivation strips the power used last; anything that should
 * shut one power and not all of them (Silence and AbilityLock shut every key) can use it:
 *
 *   int key = KeySeal.lastUsed(target);             // the ability key (1..7) it pressed last, 0 = none yet
 *   KeySeal.seal(target, key, 600);                 // 30 seconds without it
 *   KeySeal.isSealed(player, 3)    KeySeal.ticksLeft(player, 3)    KeySeal.unseal(player, 3)    KeySeal.unsealAll(player)
 *   KeySeal.lastUsed(target, 3)                     // up to three different keys, the latest first
 *
 * The ability keys ask KeySeal.blocksKey(player, key) and then tell it KeySeal.pressed(player, key)
 * (network/SKeyPressAbility...Used), so every key press is remembered and a sealed key is refused with a message.
 * Only players have keys: for a mob's special attacks use AbilityLock. Saved with the player.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class KeySeal {

    private static final String TAG = "circle_of_inevitability_key_seal";        // {"1": until, "5": until, ...}
    private static final String LAST = "circle_of_inevitability_keys_used";      // "5,3,1": the latest first
    private static final int REMEMBER = 3;

    private KeySeal() {}

    // =========================
    // API
    // =========================

    /** Key 'key' (1..7) cannot be used for 'ticks'. Sealing again keeps the longer time. */
    public static void seal(Player player, int key, int ticks) {
        if (key < 1 || key > 7 || ticks <= 0) return;
        CompoundTag tag = player.getPersistentData().getCompoundOrEmpty(TAG).copy();
        long until = player.level().getGameTime() + ticks;
        tag.putLong(String.valueOf(key), Math.max(until, tag.getLongOr(String.valueOf(key), 0L)));
        player.getPersistentData().put(TAG, tag);
    }

    public static boolean isSealed(Player player, int key) {
        return ticksLeft(player, key) > 0;
    }

    public static int ticksLeft(Player player, int key) {
        long until = player.getPersistentData().getCompoundOrEmpty(TAG).getLongOr(String.valueOf(key), 0L);
        return (int) Math.max(0, until - player.level().getGameTime());
    }

    public static void unseal(Player player, int key) {
        CompoundTag tag = player.getPersistentData().getCompoundOrEmpty(TAG).copy();
        tag.remove(String.valueOf(key));
        if (tag.isEmpty()) player.getPersistentData().remove(TAG);
        else player.getPersistentData().put(TAG, tag);
    }

    public static void unsealAll(Player player) {
        player.getPersistentData().remove(TAG);
    }

    /** The ability key this player pressed last (1..7), or 0 if none is remembered. */
    public static int lastUsed(Player player) {
        List<Integer> keys = lastUsed(player, 1);
        return keys.isEmpty() ? 0 : keys.get(0);
    }

    /** Up to 'count' different keys this player pressed lately, the latest first. */
    public static List<Integer> lastUsed(Player player, int count) {
        List<Integer> out = new ArrayList<>();
        for (String part : player.getPersistentData().getStringOr(LAST, "").split(",")) {
            if (out.size() >= count) break;
            try {
                int key = Integer.parseInt(part.trim());
                if (key >= 1 && key <= 7 && !out.contains(key)) out.add(key);
            } catch (NumberFormatException ignored) {
                // an empty list
            }
        }
        return out;
    }

    // =========================
    // Called by the ability keys
    // =========================

    /** True (and the player is told) when this key is sealed now. Asked before anything else is cast. */
    public static boolean blocksKey(Player player, int key) {
        int left = ticksLeft(player, key);
        if (left <= 0) return false;
        player.sendSystemMessage(Component.literal("That power has been taken from you for now (" + (left + 19) / 20 + "s)."));
        return true;
    }

    /** A key was pressed and let through: it becomes the one used last. */
    public static void pressed(Player player, int key) {
        List<Integer> keys = lastUsed(player, REMEMBER);
        keys.remove(Integer.valueOf(key));
        keys.add(0, key);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < Math.min(REMEMBER, keys.size()); i++) {
            if (i > 0) text.append(',');
            text.append(keys.get(i));
        }
        player.getPersistentData().putString(LAST, text.toString());
    }
}
