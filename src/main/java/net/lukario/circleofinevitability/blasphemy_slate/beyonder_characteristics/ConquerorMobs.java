package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Conqueror abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 *
 * Conqueror's abilities are written for any caster (its caster a LivingEntity, its aim the caster's facing) and pay
 * for themselves inside, so a mob runs the code a player does through MobSpells.paidCore: it turns to its target,
 * the ability pays (out of what the mob's cast covers) and goes off, or refuses and nothing is spent.
 *
 *   1  Hunter's Trap (9: under its target), Explosive Charge (9, sneak)
 *   2  Provocation (8), Repugnant Aura (8, sneak: when two or more mobs fight it)
 *   3  Fire Ravens (7), Dash (7, sneak: at a target a few blocks off)
 *   4  Flame Transformation (6), Misdirection (6, sneak: when hurt)
 *   5  Fireball (5), Cull (5, sneak)
 *   6  Galvanization (4), Iron Charge (4, sneak)
 *   7  Fog of War (3), War Banner (3, sneak)
 *
 * Simplified: Flame Transformation. A player rides the flame lance and steers it (combat/Transit); a mob's lance
 * flies at its target, and the mob comes out of the flame beside it, with the bursts where it left and arrived.
 */
public final class ConquerorMobs {

    private static final String P = Conqueror.PATHWAY;

    private ConquerorMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        // ---- key 1: Hunter's Trap / Explosive Charge (under the target)
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.NORMAL)
                .cost(Conqueror.TRAP_COST).cooldown(200).range(Conqueror.TRAP_RANGE).sight()
                .cast((mob, target, sl) -> {
                    Vec3 ground = MobSpells.groundUnder(mob, sl, target);
                    if (!Conqueror.onSolidGround(sl, ground)) return false;
                    MobSpells.face(mob, target);
                    Conqueror.setTrap(mob, sl, ground);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.SNEAK).named("Explosive Charge")
                .cost(Conqueror.CHARGE_COST).cooldown(160).weight(12).range(Conqueror.CHARGE_RANGE).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > Conqueror.CHARGE_RADIUS + 1.0)   // not on itself
                .cast((mob, target, sl) -> {
                    Vec3 ground = MobSpells.groundUnder(mob, sl, target);
                    if (!Conqueror.onSolidGround(sl, ground)) return false;
                    MobSpells.face(mob, target);
                    Conqueror.plantCharge(mob, sl, ground);
                    return true;
                }));

        // ---- key 2: Provocation / Repugnant Aura
        MobAbilities.register(MobAbility.of(P, 2, MobAbility.NORMAL)
                .cost(Conqueror.PROVOKE_COST).cooldown(Conqueror.PROVOKE_TICKS + 100).range(Conqueror.PROVOKE_RANGE).sight()
                .cast(MobSpells.paidCore(Conqueror::provoke)));
        MobAbilities.register(MobAbility.of(P, 2, MobAbility.SNEAK).named("Repugnant Aura")
                .cost(Conqueror.AURA_COST).cooldown(Conqueror.AURA_TICKS + 200).range(Conqueror.AURA_RADIUS)
                .cast(MobSpells.paidCore(Conqueror::repugnantAura)));

        // ---- key 3: Fire Ravens / Dash
        MobAbilities.register(MobAbility.of(P, 3, MobAbility.NORMAL)
                .cost(Conqueror.RAVENS_COST).cooldown(160).weight(14).range(Conqueror.RAVENS_RANGE).sight()
                .cast(MobSpells.paidCore(Conqueror::fireRavens)));
        MobAbilities.register(MobAbility.of(P, 3, MobAbility.SNEAK).named("Dash")
                .cost(Conqueror.DASH_COST).cooldown(120).range(14.0).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > 5.0 && !Root.has(mob, RootRestriction.MOVE))
                .cast(MobSpells.paidCore(Conqueror::dash)));

        // ---- key 4: Flame Transformation (a mob's own) / Misdirection
        MobAbilities.register(MobAbility.of(P, 4, MobAbility.NORMAL)
                .cost(Conqueror.FLAME_COST).cooldown(300).range(32.0).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > 6.0 && !Root.has(mob, RootRestriction.TELEPORT))
                .cast(ConquerorMobs::flameTransformation));
        MobAbilities.register(MobAbility.of(P, 4, MobAbility.SNEAK).named("Misdirection")
                .cost(Conqueror.DECOY_COST).cooldown(600).range(8.0)
                .when((mob, target, sl) -> MobSpells.hurt(mob, 0.5f))
                .cast(MobSpells.paidCore(Conqueror::misdirection)));

        // ---- key 5: Fireball (sneak on the key: as the key has it, Fireball is the sneak form) / Cull
        MobAbilities.register(MobAbility.of(P, 5, MobAbility.SNEAK).named("Fireball")
                .cost(Conqueror.FIREBALL_COST).cooldown(100).weight(14).range(Conqueror.FIREBALL_RANGE).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > Conqueror.FIREBALL_RADIUS + 1.0)
                .cast(MobSpells.paidCore(Conqueror::fireball)));
        MobAbilities.register(MobAbility.of(P, 5, MobAbility.NORMAL)
                .cost(Conqueror.CULL_COST).cooldown(300).range(Conqueror.CULL_RANGE).sight()
                .cast(MobSpells.paidCore(Conqueror::cull)));

        // ---- key 6: Galvanization / Iron Charge
        MobAbilities.register(MobAbility.of(P, 6, MobAbility.NORMAL)
                .cost(Conqueror.GALV_COST).cooldown(Conqueror.GALV_TICKS + 200).range(16.0)
                .when((mob, target, sl) -> !Conqueror.isGalvanized(mob))            // never to take it off
                .cast(MobSpells.paidCore(Conqueror::galvanization)));
        MobAbilities.register(MobAbility.of(P, 6, MobAbility.SNEAK).named("Iron Charge")
                .cost(Conqueror.IRON_CHARGE_COST).cooldown(200).range(Conqueror.IRON_CHARGE_TICKS * Conqueror.IRON_CHARGE_SPEED).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > 3.0 && !Root.has(mob, RootRestriction.MOVE))
                .cast(MobSpells.paidCore(Conqueror::ironCharge)));

        // ---- key 7: Fog of War / War Banner
        MobAbilities.register(MobAbility.of(P, 7, MobAbility.NORMAL)
                .cost(Conqueror.FOG_COST).cooldown(Conqueror.FOG_TICKS + 400).range(Conqueror.FOG_RADIUS - 2.0)
                .when((mob, target, sl) -> !Zone.isOpen(mob, Conqueror.FOG_ZONE))     // never to lift it
                .cast(MobSpells.paidCore(Conqueror::fogOfWar)));
        MobAbilities.register(MobAbility.of(P, 7, MobAbility.SNEAK).named("War Banner")
                .cost(Conqueror.BANNER_COST).cooldown(Conqueror.BANNER_TICKS + 200).range(Conqueror.BANNER_RADIUS)
                .when((mob, target, sl) -> !Zone.isOpen(mob, Conqueror.BANNER_ZONE))
                .cast(MobSpells.paidCore(Conqueror::warBanner)));
    }

    /** Flame Transformation for a mob: a burst where it stood, the lance at its target, and it steps out beside it. */
    private static boolean flameTransformation(Mob mob, LivingEntity target, ServerLevel sl) {
        Vec3 spot = MobSpells.spotBefore(mob, sl, target);
        if (spot == null) return false;
        MobSpells.face(mob, target);
        Vec3 left = mob.position();
        Conqueror.fireBlast(mob, sl, left.add(0, 0.6, 0), Conqueror.FLAME_BURST_RADIUS, Conqueror.FLAME_BURST_DAMAGE);
        Vec3 hand = Spells.clearStart(mob, sl, mob.getEyePosition().add(Spells.rightOf(mob).scale(0.35)).add(0, -0.25, 0));
        int flight = Conqueror.FLAME_LANCE.range(mob.distanceTo(target) + 4.0).onHit((hit, at) -> {
            Spells.strike(mob, hit, Conqueror.LANCE_DAMAGE);
            hit.setRemainingFireTicks(Conqueror.BLAST_BURN_TICKS);
        }).fire(mob, sl, hand, MobSpells.toward(mob, target));
        Spells.sound(sl, left, SoundEvents.BLAZE_SHOOT, 1f, 0.8f);
        Later.run(sl, Math.max(1, flight), () -> {
            if (!MobSpells.casterStillHere(mob, sl) || !MobSpells.stepTo(mob, sl, spot)) return;
            Conqueror.fireBlast(mob, sl, spot.add(0, 0.6, 0), Conqueror.FLAME_BURST_RADIUS, Conqueror.FLAME_BURST_DAMAGE);
        });
        return true;
    }
}
