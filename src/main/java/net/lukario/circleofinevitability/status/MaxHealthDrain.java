package net.lukario.circleofinevitability.status;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * MaxHealthDrain: max health taken away bit by bit, and given back the same way. For a curse that eats someone's
 * life while it lasts, armor that feeds on its wearer, a wound that will not close. Works on players and mobs:
 *
 *   Identifier id = InevitabilityArmor.id("my_curse");
 *   MaxHealthDrain.drain(player, id, 1);          // one more point of max health gone; returns how much is gone now
 *   MaxHealthDrain.recover(player, id, 1);        // one point back; returns how much is still gone
 *   MaxHealthDrain.drained(player, id)            // how much is gone (0 = none)
 *   MaxHealthDrain.clear(player, id);             // all of it back at once
 *   MaxHealthDrain.wouldEmpty(player, 1)          // true when taking one more point leaves less than half a heart
 *
 * Each id is its own drain (one attribute modifier), so several curses add up and each recovers on its own. Current
 * health is cut down to the new maximum at once. It is saved with the entity, so it survives logging out; a player
 * who dies comes back whole (the game does not carry modifiers over a respawn).
 *
 * Nothing here kills: when the maximum cannot fall any further, the game keeps it at its floor. What happens then
 * (the curse claims them, it simply stops) is the caller's, with wouldEmpty.
 */
public final class MaxHealthDrain {

    /** The smallest maximum a drain leaves: half a heart. */
    public static final double FLOOR = 1;

    private MaxHealthDrain() {}

    /** Takes 'amount' more max health away. Returns how much this drain has taken in all. */
    public static double drain(LivingEntity entity, Identifier id, double amount) {
        AttributeInstance max = entity.getAttribute(Attributes.MAX_HEALTH);
        if (max == null || amount <= 0) return drained(entity, id);
        double room = Math.max(0, entity.getMaxHealth() - FLOOR);
        double total = drained(entity, id) + Math.min(amount, room);
        set(entity, max, id, total);
        return total;
    }

    /** Gives 'amount' of it back. Returns how much is still gone. */
    public static double recover(LivingEntity entity, Identifier id, double amount) {
        AttributeInstance max = entity.getAttribute(Attributes.MAX_HEALTH);
        if (max == null) return 0;
        double total = Math.max(0, drained(entity, id) - Math.max(0, amount));
        set(entity, max, id, total);
        return total;
    }

    /** How much max health this drain has taken (0 = none). */
    public static double drained(LivingEntity entity, Identifier id) {
        AttributeInstance max = entity.getAttribute(Attributes.MAX_HEALTH);
        AttributeModifier modifier = max == null ? null : max.getModifier(id);
        return modifier == null ? 0 : -modifier.amount();
    }

    public static void clear(LivingEntity entity, Identifier id) {
        AttributeInstance max = entity.getAttribute(Attributes.MAX_HEALTH);
        if (max != null) max.removeModifier(id);
    }

    /** True when taking 'amount' more would leave a maximum below the floor. */
    public static boolean wouldEmpty(LivingEntity entity, double amount) {
        return entity.getMaxHealth() - amount < FLOOR - 1e-6;
    }

    private static void set(LivingEntity entity, AttributeInstance max, Identifier id, double total) {
        if (total <= 1e-6) max.removeModifier(id);
        else max.addOrReplacePermanentModifier(new AttributeModifier(id, -total, AttributeModifier.Operation.ADD_VALUE));
        if (entity.getHealth() > entity.getMaxHealth()) entity.setHealth(entity.getMaxHealth());
    }
}
