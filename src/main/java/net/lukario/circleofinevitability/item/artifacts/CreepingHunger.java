package net.lukario.circleofinevitability.item.artifacts;

import net.lukario.circleofinevitability.combat.AbilityCatalog;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.item.artifacts.storing.AbilitySlots;
import net.lukario.circleofinevitability.item.artifacts.storing.AggroDrawback;
import net.lukario.circleofinevitability.item.artifacts.storing.BloodThirstDrawback;
import net.lukario.circleofinevitability.item.artifacts.storing.CorruptionDrawback;
import net.lukario.circleofinevitability.item.artifacts.storing.DisorientationDrawback;
import net.lukario.circleofinevitability.item.artifacts.storing.FeedOrDieDrawback;
import net.lukario.circleofinevitability.item.artifacts.storing.StoringArtifact;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Creeping Hunger: a living glove that devours the Beyonders its bearer kills and lends their powers, at the price of
 * its hunger. Five tiers (creeping_hunger_1 .. _5). Every number lives here; the mechanics are the reusable
 * StoringArtifact framework (item/artifacts/storing). docs/SEALED_ARTIFACTS.md describes it for players.
 *
 * Tier: slots                                  | downsides (each tier adds one, all grow stronger)
 *  I    5, Sequence 5 or weaker                | daily kill or die, corruption
 *  II   7: 1 up to Sequence 4, 6 up to 5       | + hostile mobs hunt you
 *  III  9: 2 up to Seq 2, 1 up to 4, 6 up to 5 | + blood thirst after casting; you choose what to devour
 *  IV   9: 3 up to Sequence 1, 6 up to 5       | + disorientation fits; neutral mobs hunt you too; 2 picks per kill
 *  V    11, any Sequence                       | everything at full strength, fits darken the world; 3 picks per kill
 *
 * Cast cooldown (Sequence 9 .. Sequence 0): I 2 .. 20 s, II 1.9 .. 15.4 s, III 1.75 .. 10.75 s, IV 1.65 .. 6.15 s,
 * V 1.5 s for everything.
 */
public final class CreepingHunger {
    public static final String ID = "creeping_hunger";
    public static final String OVERLAY = "creeping_hunger/hunger_vision";

    public static final StoringArtifact ARTIFACT = StoringArtifact.create(ID, "Creeping Hunger")
            .tier(AbilitySlots.of(5, 5))
            .tier(AbilitySlots.of(1, 4).and(6, 5))
            .tier(AbilitySlots.of(2, 2).and(1, 4).and(6, 5))
            .tier(AbilitySlots.of(3, 1).and(6, 5))
            .tier(AbilitySlots.any(11))
            .chooseOnKillFrom(3)
            .picksPerKill(1, 1, 1, 2, 3)                     // tier IV: up to two abilities from one kill, tier V: three
            .topTierCooldown(30)                             // tier V: 1.5 s for any Sequence; tiers II-IV slide there
            .drawback(new FeedOrDieDrawback(1, 24000, 3))
            .drawback(new CorruptionDrawback(1,
                    new int[]{60, 45, 30, 20, 12},           // seconds carried per point
                    new int[]{40, 55, 70, 80, 90},           // never past this
                    new int[]{1, 2, 3, 4, 5},                // per cast
                    2))                                      // drained per second while put away
            .drawback(new AggroDrawback(2, new int[]{0, 8, 12, 16, 24}, 4))
            .drawback(new BloodThirstDrawback(3, new float[]{1, 1, 1, 1.5f, 2}, 2400))
            .drawback(new DisorientationDrawback(4, new int[]{0, 0, 0, 90, 45}, 5, OVERLAY))
            .effects(new StoringArtifact.Effects() {
                @Override
                public void devoured(ServerLevel level, ServerPlayer player, LivingEntity victim, AbilityCatalog.Power power) {
                    Vec3 from = victim.position().add(0, victim.getBbHeight() / 2, 0);
                    Vec3 to = player.position().add(0, 1, 0);
                    for (int i = 0; i <= 12; i++) {
                        Vec3 at = from.lerp(to, i / 12.0);
                        level.sendParticles(ParticleTypes.SCULK_SOUL, at.x, at.y, at.z, 2, .1, .1, .1, .01);
                    }
                    Spells.sound(level, player.position(), SoundEvents.SCULK_SHRIEKER_SHRIEK, .5f, .5f);
                }

                @Override
                public void cast(ServerLevel level, ServerPlayer player, AbilityCatalog.Power power) {
                    Vec3 hand = player.getEyePosition().add(player.getLookAngle().scale(.6)).add(0, -.4, 0);
                    level.sendParticles(ParticleTypes.SOUL, hand.x, hand.y, hand.z, 8, .15, .15, .15, .03);
                    Spells.sound(level, player.position(), SoundEvents.WITHER_AMBIENT, .3f, 1.6f);
                }
            });

    private CreepingHunger() {}
}
