package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

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
 * Some power needs no voice (a poem resonated in the mind): let such a key through anyway with
 *   Silence.addCastsThrough((player, key) -> key == 2 && MyPathway.resonates(player));
 * and give its wheel .castsWhileSilenced(...) (or its AbilityMenu, for a menu of its own) so the pick goes through.
 * The ability keys ask Silence.blocksKey(player, key) before anything else.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class Silence {

    /** Says this player's key may be used though their abilities are rooted (key = 1..7, the ability keys). */
    @FunctionalInterface
    public interface CastsThrough {
        boolean castsWhileSilenced(Player player, int key);
    }

    private static final List<CastsThrough> CASTS_THROUGH = new ArrayList<>();

    private Silence() {}

    /** One more power that is not stopped by Silence (or any root with ABILITIES) on this key. */
    public static void addCastsThrough(CastsThrough rule) {
        CASTS_THROUGH.add(rule);
    }

    /** True when this ability key is stopped: abilities are rooted and no addCastsThrough rule lets it through. */
    public static boolean blocksKey(Player player, int key) {
        if (!Root.has(player, RootRestriction.ABILITIES)) return false;
        for (CastsThrough rule : CASTS_THROUGH) {
            if (rule.castsWhileSilenced(player, key)) return false;
        }
        return true;
    }

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
