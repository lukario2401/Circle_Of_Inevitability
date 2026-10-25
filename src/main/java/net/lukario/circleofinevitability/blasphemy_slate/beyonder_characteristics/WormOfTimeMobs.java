package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Worm Of Time abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 * Worm Of Time's key 1 is written inside the key for a player, so this is a mob's own version, with its numbers.
 *
 *   1  Life Theft (9: health taken from its target, more every time; through armor), Time Step (9, sneak: to a
 *      target far off)
 *
 * Left out: Theft (8: a stolen ability is a player's to hold and release) and Parasite (4: a player who rides and
 * steers a host).
 */
public final class WormOfTimeMobs {

    private static final String P = "Worm Of Time";

    private WormOfTimeMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.NORMAL)
                .cost(250).cooldown(100).weight(14).range(24.0).sight()
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    int hits = Math.max(1, target.getPersistentData().getIntOr(WormOfTime.LIFE_THEFT_VULNERABILITY, 0));
                    float before = target.getHealth();
                    Spells.strikeTrue(mob, target, 15f * hits);
                    float taken = Math.max(0f, before - (target.isAlive() ? target.getHealth() : 0f));
                    if (taken > 0f) mob.heal(taken);
                    target.getPersistentData().putInt(WormOfTime.LIFE_THEFT_VULNERABILITY, hits + 1);
                    Vec3 from = MobSpells.chestOf(target);
                    Vec3 way = MobSpells.chestOf(mob).subtract(from);
                    for (double along = 0; along <= 1.0; along += 0.1) {
                        Vec3 at = from.add(way.scale(along));
                        sl.sendParticles(ParticleTypes.WITCH, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.0);
                    }
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.SNEAK).named("Time Step")
                .cost(250).cooldown(200).range(32.0).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > 8.0 && !Root.has(mob, RootRestriction.TELEPORT))
                .cast((mob, target, sl) -> {
                    Vec3 spot = MobSpells.spotBefore(mob, sl, target);
                    return spot != null && MobSpells.stepTo(mob, sl, spot);
                }));
    }
}
