package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Teleport;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.status.Unbound;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStars.*;

/**
 * Key Of Stars abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 * The costs and effects are the player abilities' own (KeyOfStars); only the cooldowns and when a mob uses them are
 * a mob's. To port another ability: one more MobAbilities.register(...) in register(), and its cast method below.
 *
 *   Door Opening (9):        when its target is far off or behind a wall, a Door opens and it steps out beside it
 *                            (DOOR_MOB_REACH blocks at most). It goes even with a Teleport root on it, as a player's.
 *   Slip Free (9, sneak):    when it is rooted, burning or under a harmful effect: all of that ends, and nothing of
 *                            the kind takes for SLIP_GUARD (status/Unbound). Secrets Sorcerer (4): Teleport roots too.
 */
public final class KeyOfStarsMobs {

    private static final int DOOR_MOB_COOLDOWN = 100;          // ticks
    private static final double DOOR_MOB_REACH = 16.0;         // its target no further than this...
    private static final double DOOR_MOB_FAR = 6.0;            // ...and further than this, or out of sight
    private static final int SLIP_MOB_COOLDOWN = 400;

    private KeyOfStarsMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        MobAbilities.register(MobAbility.of(PATHWAY, 1, MobAbility.NORMAL)          // Door Opening
                .cost(DOOR_COST).cooldown(DOOR_MOB_COOLDOWN)
                .range(DOOR_MOB_REACH)
                .when((mob, target, sl) -> mob.distanceTo(target) > DOOR_MOB_FAR || !mob.hasLineOfSight(target))
                .cast(KeyOfStarsMobs::doorOpening));

        MobAbilities.register(MobAbility.of(PATHWAY, 1, MobAbility.SNEAK).named("Slip Free")
                .cost(SLIP_COST).cooldown(SLIP_MOB_COOLDOWN)
                .anyRange().weight(30)                                                // when it is needed, it comes first
                .when((mob, target, sl) -> needsFreeing(mob))
                .cast(KeyOfStarsMobs::slipFree));
    }

    // =========================
    // Door Opening
    // =========================

    private static boolean doorOpening(Mob mob, LivingEntity target, ServerLevel sl) {
        Vec3 exit = Teleport.standNear(mob, sl, target.position(), 2.0);
        if (exit == null || exit.distanceTo(mob.position()) < 2.0) return false;       // no room by it: nothing spent
        MobSpells.face(mob, target);
        Vec3 door = mob.position().add(Spells.flatLook(mob).scale(0.8));
        KeyOfStarsFx.doorOpening(sl, mob, door, exit);
        Later.run(sl, KeyOfStarsFx.DOOR_OPENED, () -> {
            if (!MobSpells.casterStillHere(mob, sl)) return;
            Teleport.of(mob).ignoringTeleportRoots().fallGuard(SAFE_LANDING_TICKS).go(sl, exit);
            if (target.isAlive() && target.level() == sl) MobSpells.face(mob, target);
        });
        return true;
    }

    // =========================
    // Slip Free
    // =========================

    private static boolean slipFree(Mob mob, LivingEntity target, ServerLevel sl) {
        Unbound.free(mob, SLIP_GUARD, MobSpells.hasSequence(mob, UNBAR_SEQUENCE));
        KeyOfStarsFx.slipFree(sl, mob, mob);
        return true;
    }

    /** Rooted, burning, or under a harmful effect. */
    private static boolean needsFreeing(Mob mob) {
        if (Root.isRooted(mob) || mob.isOnFire()) return true;
        for (MobEffectInstance effect : mob.getActiveEffects()) {
            if (!effect.getEffect().value().isBeneficial()) return true;
        }
        return false;
    }
}
