package net.lukario.circleofinevitability.status;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Dispel: everything that keeps someone safe, taken off at once. For a deletion, a word of unmaking, a light that
 * breaks every ward. Knowledge Emperor's Erase is built on it.
 *
 *   int boons = Dispel.boons(target);           // every beneficial potion effect off; returns how many
 *   int wards = Dispel.statuses(target);        // every kit status that protects or hides it off; returns how many
 *   if (Dispel.isConjured(target)) Dispel.unmake(target);   // a summon, a decoy, a projection: gone (no drops)
 *
 * What statuses() takes off: a Substitute ready, a DeathWard, a DamageLink redirect and share, Concealment,
 * Unnoticed, Invulnerable, and a marionette's strings (the mob is itself again). Each Pathway can add its own,
 * once in a static block:
 *
 *   Dispel.addRule(entity -> MyWard.isUp(entity) && MyWard.breakIt(entity));   // true: something was taken off
 *   Dispel.addConjuredCheck(entity -> MyDecoys.isDecoy(entity));                // it counts as conjured
 *
 * Nothing here is saved or ticks: it only acts when it is called.
 */
public final class Dispel {

    /** Takes one kind of ward off an entity; true if it had one. */
    @FunctionalInterface
    public interface Rule {
        boolean strip(LivingEntity entity);
    }

    private static final List<Rule> RULES = new ArrayList<>();
    private static final List<Predicate<Entity>> CONJURED = new ArrayList<>();

    private Dispel() {}

    public static void addRule(Rule rule) {
        RULES.add(rule);
    }

    public static void addConjuredCheck(Predicate<Entity> check) {
        CONJURED.add(check);
    }

    /** Every beneficial potion effect off. Returns how many there were. */
    public static int boons(LivingEntity entity) {
        List<MobEffectInstance> good = new ArrayList<>();
        for (MobEffectInstance effect : entity.getActiveEffects()) {
            if (effect.getEffect().value().isBeneficial()) good.add(effect);
        }
        for (MobEffectInstance effect : good) entity.removeEffect(effect.getEffect());
        return good.size();
    }

    /** Every kit status that protects or hides it off (see the class comment). Returns how many there were. */
    public static int statuses(LivingEntity entity) {
        int taken = 0;
        if (Substitute.isArmed(entity)) {
            Substitute.disarm(entity);
            taken++;
        }
        if (DeathWard.isArmed(entity)) {
            DeathWard.disarm(entity);
            taken++;
        }
        if (DamageLink.redirectTarget(entity) != null) {
            DamageLink.clearRedirect(entity);
            taken++;
        }
        if (DamageLink.isShared(entity)) {
            DamageLink.unshare(entity);
            taken++;
        }
        if (Concealment.isHidden(entity)) {
            Concealment.reveal(entity);
            taken++;
        }
        if (Unnoticed.isUnnoticed(entity)) {
            Unnoticed.end(entity);
            taken++;
        }
        if (Invulnerable.isGranted(entity)) {
            Invulnerable.end(entity);
            taken++;
        }
        if (entity instanceof Mob mob && Marionette.isMarionette(mob) && !Marionette.isSummoned(mob)) {
            Marionette.release(mob);
            taken++;
        }
        for (Rule rule : RULES) {
            if (rule.strip(entity)) taken++;
        }
        return taken;
    }

    /** True for what was called up rather than born: summons, and whatever an addConjuredCheck names. */
    public static boolean isConjured(Entity entity) {
        if (Marionette.isSummoned(entity)) return true;
        for (Predicate<Entity> check : CONJURED) {
            if (check.test(entity)) return true;
        }
        return false;
    }

    /** A conjured thing is gone: a summon collapses as when its time runs out (no drops), anything else is removed. */
    public static void unmake(Entity entity) {
        if (entity instanceof Mob mob && Marionette.isSummoned(mob)) {
            Marionette.collapse(mob);
            if (mob.isAlive()) mob.discard();
            return;
        }
        entity.discard();
    }
}
