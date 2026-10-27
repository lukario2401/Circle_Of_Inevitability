package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.status.*;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

/** Combat adaptations for Beyonder mobs: bounded life/distance theft (4), intent/dream theft (5),
 * deceit (3), aging/time stop (1), and Error misdirection (0). Inventory theft, held powers,
 * host camera control, avatar management and rewind remain player-only. */
public final class WormOfTimeMobs {

    private static final String P = "Worm Of Time";

    private WormOfTimeMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.NORMAL).named("Life Theft").sequence(4)
                .cost(1800).cooldown(100).weight(14).range(24.0).sight()
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    float before = target.getHealth();
                    Spells.strikeTrue(mob, target, 24f);
                    float taken = Math.max(0f, before - (target.isAlive() ? target.getHealth() : 0f));
                    if (taken > 0f) mob.heal(taken);
                    Vec3 from = MobSpells.chestOf(target);
                    Vec3 way = MobSpells.chestOf(mob).subtract(from);
                    for (double along = 0; along <= 1.0; along += 0.1) {
                        Vec3 at = from.add(way.scale(along));
                        sl.sendParticles(ParticleTypes.WITCH, at.x, at.y, at.z, 2, 0.05, 0.05, 0.05, 0.0);
                    }
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.SNEAK).named("Distance Theft").sequence(4)
                .cost(1500).cooldown(200).range(32.0).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > 8.0 && !Root.has(mob, RootRestriction.TELEPORT))
                .cast((mob, target, sl) -> {
                    Vec3 spot = MobSpells.spotBefore(mob, sl, target);
                    return spot != null && MobSpells.stepTo(mob, sl, spot);
                }));
        MobAbilities.register(MobAbility.of(P, 5, MobAbility.NORMAL).entry("intent").named("Steal Intent").sequence(5)
                .cost(2000).cooldown(160).range(24).sight().cast((mob, target, sl) -> {
                    Root.apply(target, Spells.holdLength(target, 40), RootRestriction.DISARM);
                    WormOfTimeFx.steal(sl, mob, target);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 5, MobAbility.NORMAL).entry("dream").named("Steal Dream").sequence(5)
                .cost(2400).cooldown(240).range(24).sight().cast((mob, target, sl) -> {
                    if (!Slumber.put(target, Spells.holdLength(target, 80), Slumber.Kind.SLEEP)) return false;
                    WormOfTimeFx.steal(sl, mob, target);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 5, MobAbility.NORMAL).entry("deceive").named("Deceit").sequence(3)
                .cost(6000).cooldown(300).range(16).sight().cast((mob, target, sl) -> {
                    Falter.apply(target, "worm_of_time_deceit", 0.35f, 160);
                    WormOfTimeFx.clock(sl, mob.position().add(0, 2, 0), 2, 30, false);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 7, MobAbility.NORMAL).entry("aging").named("Steal Lifespan").sequence(1)
                .cost(18000).cooldown(240).range(32).sight().cast((mob, target, sl) -> {
                    float before = target.getHealth();
                    Spells.strikeTrue(mob, target, 100);
                    mob.heal(Math.max(0, before - target.getHealth()) * 0.4f);
                    Decay.apply(target, 160, 0.2f);
                    WormOfTimeFx.steal(sl, mob, target);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 7, MobAbility.NORMAL).entry("stop").named("Ancient Clock").sequence(1)
                .cost(30000).cooldown(600).range(12).sight().cast((mob, target, sl) -> {
                    Root.apply(target, Spells.holdLength(target, 40), RootRestriction.EVERYTHING);
                    WormOfTimeFx.clock(sl, mob.position().add(0, 2, 0), 3, 40, false);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 7, MobAbility.NORMAL).entry("misdirect").named("Error: Misdirection").sequence(0)
                .cost(35000).cooldown(500).range(24).sight().cast((mob, target, sl) -> {
                    Substitute.arm(mob, 80, 2, (saved, source, damage, world) -> {
                        if (target.isAlive() && target.level() == world && saved.distanceTo(target) < 32)
                            Spells.strike(saved, target, Math.min(150, damage));
                    });
                    return Substitute.isArmed(mob);
                }));
    }
}
