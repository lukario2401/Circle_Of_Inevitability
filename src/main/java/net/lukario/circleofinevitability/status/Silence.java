package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Silence: an entity's power is hushed for a while. Works from any class/ability:
 *
 *   Silence.apply(target, 100);        // 5 seconds
 *   Silence.isSilenced(entity)   Silence.ticksLeft(entity)   Silence.lift(target)
 *
 * A player cannot use abilities (every ability key refuses, as for a root with ABILITIES) but can still fight with
 * their weapons. A mob cannot attack at all, in melee or with what it shoots: it is its power that is hushed.
 *
 * It is root/Root underneath (ABILITIES, and ATTACK for a mob), so it is saved with the entity, it shows Root's ring,
 * and lift(...) also lifts a root of the same kinds from elsewhere.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class Silence {

    private Silence() {}

    public static void apply(LivingEntity target, int ticks) {
        if (target.level().isClientSide() || ticks <= 0 || !target.isAlive()) return;
        if (target instanceof Player) Root.apply(target, ticks, RootRestriction.ABILITIES);
        else Root.apply(target, ticks, RootRestriction.ABILITIES, RootRestriction.ATTACK);
    }

    public static boolean isSilenced(Entity entity) {
        return entity != null && Root.has(entity, RootRestriction.ABILITIES);
    }

    public static int ticksLeft(Entity entity) {
        return entity == null ? 0 : Root.ticksLeft(entity, RootRestriction.ABILITIES);
    }

    public static void lift(LivingEntity target) {
        if (target instanceof Player) Root.remove(target, RootRestriction.ABILITIES);
        else Root.remove(target, RootRestriction.ABILITIES, RootRestriction.ATTACK);
    }
}
