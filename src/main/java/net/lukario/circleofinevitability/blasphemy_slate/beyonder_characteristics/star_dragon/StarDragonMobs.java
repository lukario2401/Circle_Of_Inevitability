package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragonMarks.Mark;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragonRays.*;

/**
 * Star Dragon abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 * The costs and effects are the player abilities' own (StarDragonRays); only the cooldowns are a mob's. To port
 * another ability: one more MobAbilities.register(...) in register(), and its cast method below.
 *
 *   Piercing Ray (6, key 3 sneak): two beams of blue light from its eyes at its target, PIERCING_RANGE long, through
 *                                  every enemy on the line: PIERCING_DAMAGE each and Dimmed for DIMMED_TICKS.
 */
public final class StarDragonMobs {

    private static final int PIERCING_MOB_COOLDOWN = 160;      // ticks

    private StarDragonMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        MobAbilities.register(MobAbility.of(StarDragon.PATHWAY, 3, MobAbility.SNEAK).named("Piercing Ray")
                .cost(PIERCING_COST).cooldown(PIERCING_MOB_COOLDOWN)
                .range(PIERCING_RANGE).sight()
                .cast(StarDragonMobs::piercingRay));
    }

    // =========================
    // Piercing Ray
    // =========================

    private static boolean piercingRay(Mob mob, LivingEntity target, ServerLevel sl) {
        MobSpells.face(mob, target);
        Vec3 eye = mob.getEyePosition();
        Vec3 to = MobSpells.lineEnd(mob, sl, eye, MobSpells.toward(mob, target), PIERCING_RANGE);
        StarDragonFx.piercingRay(sl, mob, to);
        for (LivingEntity hit : MobSpells.enemiesOnLine(mob, sl, eye, to, PIERCING_WIDTH)) {
            MobSpells.strike(mob, hit, PIERCING_DAMAGE);
            if (hit.isAlive()) StarDragonMarks.put(hit, Mark.DIMMED, mob, DIMMED_TICKS);
        }
        return true;
    }
}
