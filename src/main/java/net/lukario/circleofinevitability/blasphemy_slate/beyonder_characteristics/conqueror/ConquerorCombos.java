package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.conqueror;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGodFx;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.pathway_fx.ConquerorFx;
import net.lukario.circleofinevitability.combat.AbilityCombos.Combo;
import net.lukario.circleofinevitability.combat.AbilityCombos.Input;
import net.lukario.circleofinevitability.combat.Dome;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Seeker;
import net.lukario.circleofinevitability.combat.SpellFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public final class ConquerorCombos {
    public static final String RAVEN_TRAP = "conqueror_raven_trap";
    public static final String ASHEN_BASTION = "conqueror_ashen_bastion";
    private static final int BASTION_TICKS = 120;
    private static final double BASTION_RADIUS = 5;
    private static final int BASTION_COLOR = 0xC0FF7538;
    private static final int QUAKE_TICKS = 12;
    private static final double QUAKE_RADIUS = 8;
    private static final float QUAKE_DAMAGE = 70;

    public static final List<Combo> ALL = List.of(
            new Combo(Conqueror.PATHWAY, RAVEN_TRAP, "Raven Trap", 7, Input.CHORD, 1, 3, 700, 80, ConquerorCombos::ravenTrap),
            new Combo(Conqueror.PATHWAY, ASHEN_BASTION, "Ashen Bastion", 6, Input.CHORD, 2, 4, 1600, 200, ConquerorCombos::ashenBastion),
            new Combo(Conqueror.PATHWAY, "conqueror_executioners_challenge", "Executioner's Challenge", 5, Input.SEQUENCE, 2, 5,
                    Conqueror.PROVOKE_COST + Conqueror.CULL_COST, 100, ConquerorCombos::executionersChallenge),
            new Combo(Conqueror.PATHWAY, "conqueror_iron_quake", "Iron Quake", 4, Input.SEQUENCE, 4, 6, 3200, 120, ConquerorCombos::ironQuake));

    private ConquerorCombos() {}

    private static Runnable ravenTrap(ServerPlayer player, ServerLevel level) {
        Vec3 ground = Spells.aimGround(player, level, Conqueror.TRAP_RANGE);
        if (!Conqueror.onSolidGround(level, ground)) {
            player.sendSystemMessage(Component.literal("There is no ground there to set Raven Trap on."));
            return null;
        }
        return () -> Conqueror.setTrap(player, level, ground, RAVEN_TRAP, victim -> {
            Vec3 from = ground.add(0, 0.9, 0);
            List<LivingEntity> prey = Spells.enemiesAround(player, level, from, 12);
            prey.removeIf(enemy -> !Spells.clearLine(player, level, from, enemy.getBoundingBox().getCenter()));
            prey.sort(Comparator.comparingDouble(enemy -> enemy.distanceToSqr(from)));
            if (victim.isAlive() && prey.remove(victim)) prey.addFirst(victim);
            if (prey.isEmpty()) return;

            Seeker raven = Conqueror.ravenSeeker(player, level);
            for (int i = 0; i < 3; i++) {
                LivingEntity target = prey.get(i % prey.size());
                Later.run(level, i * Conqueror.RAVENS_EVERY, () -> {
                    if (Spells.casterStillHere(player, level)) raven.launch(player, level, from, null, target);
                });
            }
        });
    }

    private static Runnable ashenBastion(ServerPlayer player, ServerLevel level) {
        Vec3 ground = player.position();
        if (!Conqueror.onSolidGround(level, ground)) {
            player.sendSystemMessage(Component.literal("Stand on solid ground to raise Ashen Bastion."));
            return null;
        }
        // Each bastion owns its model tag so overlapping domes can expire independently.
        String tag = ASHEN_BASTION + "_" + player.getUUID();
        return () -> {
            Dome.at(ground, BASTION_RADIUS)
                    .lasts(BASTION_TICKS)
                    .stopsProjectiles()
                    .wardsEffects()
                    .onBlock(at -> ParticleShapes.burst(level, SpellFx.FIRE_EMBER, at, 8, 0.02, 0.12))
                    .onEnd(() -> ParticleShapes.clearModels(level, ground, tag))
                    .raise(player, level, ASHEN_BASTION);
            HandOfGodFx.dome(level, ground, BASTION_RADIUS, BASTION_COLOR, BASTION_TICKS, tag);
            Spells.sound(level, ground, SoundEvents.FIRECHARGE_USE, 0.8f, 0.7f);
        };
    }

    private static Runnable executionersChallenge(ServerPlayer player, ServerLevel level) {
        LivingEntity target = Conqueror.cullTarget(player, level);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is nothing in sight to challenge."));
            return null;
        }
        return () -> {
            Conqueror.provokeTarget(player, level, target);
            Conqueror.cullTarget(player, level, target, 1f);
        };
    }

    private static Runnable ironQuake(ServerPlayer player, ServerLevel level) {
        if (!player.onGround()) {
            player.sendSystemMessage(Component.literal("Land before casting Iron Quake."));
            return null;
        }
        Vec3 ground = player.position();
        return () -> {
            ConquerorFx.ironClang(level, ground);
            SpellFx.steadyRing(level, ground, QUAKE_RADIUS, 0xF0FF984F, QUAKE_TICKS, 0);
            Spells.wave(player, level, ground, QUAKE_RADIUS, QUAKE_TICKS, 2, enemy -> {
                Spells.strike(player, enemy, QUAKE_DAMAGE);
                Spells.push(enemy, ground, 1.4, 0.35);
                Spells.keepEffect(enemy, MobEffects.SLOWNESS, 1, 60);
            });
        };
    }
}
