package net.lukario.circleofinevitability.status;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Construct: what is made rather than born. Golems of iron and snow, and whatever an ability builds and marks as
 * made (a doll, a golem of brass, a clockwork soldier). Abilities that take things apart ask it (the Illuminator's
 * Exploit Flaw and, later, Dissociation):
 *
 *   if (Construct.isConstruct(target)) ...          // iron and snow golems, anything marked, anything a rule names
 *   Construct.mark(mob);                            // an ability's own creation counts from now on (saved with it)
 *   Construct.addCheck(entity -> ...);              // once, in a static block: one more kind of made thing
 *
 * This is the part every Pathway can use now. The rest of what docs/ILLUMINATOR.md plans for it (spawning a mob,
 * dressing it with a model, handing it to Marionette, limits per owner, the doll material table) belongs here too
 * when the Alchemist's wheel is written: see "The second half" in docs/ILLUMINATOR.md.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class Construct {

    private static final String TAG = "circle_of_inevitability_construct";       // true: made, not born

    private static final List<Predicate<LivingEntity>> CHECKS = new ArrayList<>();

    private Construct() {}

    /** True for something made: an iron or snow golem, anything mark(...)ed, anything an addCheck rule names. */
    public static boolean isConstruct(Entity entity) {
        if (!(entity instanceof LivingEntity living)) return false;
        if (living.getType() == EntityTypes.IRON_GOLEM || living.getType() == EntityTypes.SNOW_GOLEM) return true;
        if (living.getPersistentData().getBooleanOr(TAG, false)) return true;
        for (Predicate<LivingEntity> check : CHECKS) if (check.test(living)) return true;
        return false;
    }

    /** From now on it counts as made (saved with it). */
    public static void mark(LivingEntity entity) {
        entity.getPersistentData().putBoolean(TAG, true);
    }

    /** One more kind of made thing: Construct.addCheck(entity -> MyGolems.isMine(entity)). */
    public static void addCheck(Predicate<LivingEntity> check) {
        CHECKS.add(check);
    }
}
