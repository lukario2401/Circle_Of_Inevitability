package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.conqueror;

import net.lukario.circleofinevitability.combat.Healing;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.item.artifacts.uniqueness.ShadowSoldier;
import net.lukario.circleofinevitability.screen.ScreenImages;
import net.lukario.circleofinevitability.status.Marionette;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The Conqueror Pathway's abilities above Sequence 3, and its one heal. For now they are lent only by the Red Priest
 * uniqueness (item/artifacts/RedPriest): the Pathway's seven keys are full, so giving them to Conqueror Beyonders
 * means a wheel on a key, like the other Pathways' Sequence 2..0 wheels. Each returns whether it went off.
 *
 *   Iron Blood (Sequence 4, Iron-blooded Knight): heal a third of your health and put out the fire in you.
 *   Storm of War (Sequence 2, Weather Warlock): a storm over where you look, lightning on everyone in it, then a gale.
 *   Conquest (Sequence 1, Conqueror): everyone hostile in sight kneels, and the weakest of them fight for you.
 *   Crimson Dominion (Sequence 0, Red Priest): the sky bleeds, burning spears rain on your enemies, and a legion of
 *   shadow soldiers rises to fight for you.
 */
public final class ConquerorHigher {
    // Iron Blood
    public static final float IRON_BLOOD_SHARE = 1 / 3f;
    // Storm of War
    public static final double STORM_RANGE = 40, STORM_RADIUS = 10;
    public static final int STORM_BOLTS = 8, STORM_BOLT_EVERY = 5;
    public static final float STORM_BOLT_DAMAGE = 16, STORM_GALE_DAMAGE = 8;
    // Conquest
    public static final double CONQUEST_RANGE = 20;
    public static final float CONQUEST_SHARE = .25f, CONQUEST_BOSS_SHARE = .08f, CONQUEST_BREAKS_BELOW = .3f;
    public static final int CONQUEST_KNEEL_TICKS = 100, CONQUEST_SERVE_TICKS = 20 * 30, CONQUEST_MOST = 6;
    // Crimson Dominion
    public static final double DOMINION_RADIUS = 16;
    public static final int DOMINION_SPEARS = 24, DOMINION_LEGION = 4, DOMINION_LEGION_TICKS = 20 * 45;
    public static final float DOMINION_SPEAR_DAMAGE = 30;

    private static final int CRIMSON = 0xC0182A, EMBER = 0xFF7A30, GOLD = 0xE0B048;

    private ConquerorHigher() {}

    // =========================
    // Iron Blood (Sequence 4)
    // =========================

    public static boolean ironBlood(ServerPlayer player, ServerLevel level) {
        if (player.getHealth() >= player.getMaxHealth() && !player.isOnFire()) {
            player.sendOverlayMessage(Component.literal("Your blood is already iron."));
            return false;
        }
        Healing.apply(player, player.getMaxHealth() * IRON_BLOOD_SHARE);
        player.clearFire();
        Vec3 at = player.position();
        for (int i = 0; i < 24; i++) {
            double angle = i * Math.PI / 6, y = i * .09;
            level.sendParticles(new DustParticleOptions(i % 3 == 0 ? GOLD : CRIMSON, 1f),
                    at.x + Math.cos(angle) * .7, at.y + y, at.z + Math.sin(angle) * .7, 1, 0, 0, 0, 0);
        }
        level.sendParticles(ParticleTypes.HEART, at.x, at.y + 2, at.z, 3, .3, .1, .3, 0);
        Spells.sound(level, at, SoundEvents.ANVIL_USE, .6f, .5f);
        Spells.sound(level, at, SoundEvents.BEACON_POWER_SELECT, .5f, 1.6f);
        return true;
    }

    // =========================
    // Storm of War (Sequence 2)
    // =========================

    public static boolean stormOfWar(ServerPlayer player, ServerLevel level) {
        Vec3 center = Spells.aimGround(player, level, STORM_RANGE);
        if (center == null) {
            player.sendOverlayMessage(Component.literal("There is no ground there for the storm."));
            return false;
        }
        Spells.sound(level, center, SoundEvents.LIGHTNING_BOLT_THUNDER, 2f, .6f);
        for (int i = 0; i < 40; i++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2, r = player.getRandom().nextDouble() * STORM_RADIUS;
            level.sendParticles(i % 2 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.CLOUD,
                    center.x + Math.cos(angle) * r, center.y + 9 + player.getRandom().nextDouble() * 2, center.z + Math.sin(angle) * r,
                    2, .5, .2, .5, .01);
        }
        for (int i = 0; i < STORM_BOLTS; i++) {
            int index = i;
            Later.run(level, 6 + i * STORM_BOLT_EVERY, () -> {
                if (!player.isAlive()) return;
                List<LivingEntity> enemies = Spells.enemiesAround(player, level, center, STORM_RADIUS);
                Vec3 at;
                if (!enemies.isEmpty()) at = enemies.get(index % enemies.size()).position();
                else {
                    double angle = player.getRandom().nextDouble() * Math.PI * 2, r = player.getRandom().nextDouble() * STORM_RADIUS;
                    at = Spells.groundAt(player, level, center.add(Math.cos(angle) * r, 4, Math.sin(angle) * r));
                    if (at == null) return;
                }
                bolt(level, at);
                for (LivingEntity hit : Spells.enemiesAround(player, level, at, 2.5)) {
                    Spells.strike(player, hit, STORM_BOLT_DAMAGE);
                    hit.igniteForSeconds(4);
                }
            });
        }
        Later.run(level, 8 + STORM_BOLTS * STORM_BOLT_EVERY, () -> {
            if (!player.isAlive()) return;
            for (int i = 0; i < 36; i++) {
                double angle = i * Math.PI / 18;
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x + Math.cos(angle) * 4, center.y + 1,
                        center.z + Math.sin(angle) * 4, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.CLOUD, center.x + Math.cos(angle) * 2, center.y + .5,
                        center.z + Math.sin(angle) * 2, 0, Math.cos(angle), .1, Math.sin(angle), .8);
            }
            Spells.sound(level, center, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.5f, .6f);
            for (LivingEntity hit : Spells.enemiesAround(player, level, center, STORM_RADIUS)) {
                Spells.strike(player, hit, STORM_GALE_DAMAGE);
                Spells.push(hit, center, 1.6, .6);
            }
        });
        player.sendOverlayMessage(Component.literal("The sky answers the Weather Warlock.").withStyle(ChatFormatting.GOLD));
        return true;
    }

    private static void bolt(ServerLevel level, Vec3 at) {
        LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
        if (bolt == null) return;
        bolt.snapTo(at);
        bolt.setVisualOnly(true);
        level.addFreshEntity(bolt);
    }

    // =========================
    // Conquest (Sequence 1)
    // =========================

    public static boolean conquest(ServerPlayer player, ServerLevel level) {
        List<LivingEntity> enemies = new ArrayList<>(Spells.enemiesInSight(player, level, CONQUEST_RANGE, 70));
        enemies.addAll(Spells.enemiesAround(player, level, player.position(), 5).stream().filter(e -> !enemies.contains(e)).toList());
        if (enemies.isEmpty()) {
            player.sendOverlayMessage(Component.literal("There is no one here to conquer."));
            return false;
        }
        Vec3 at = player.position();
        Spells.sound(level, at, SoundEvents.RAID_HORN.value(), 2f, 1.1f);
        Spells.sound(level, at, SoundEvents.WARDEN_SONIC_BOOM, .6f, .5f);
        for (int i = 0; i < 48; i++) {
            double angle = i * Math.PI / 24;
            level.sendParticles(new DustParticleOptions(GOLD, 1.4f), at.x + Math.cos(angle) * 3, at.y + .2,
                    at.z + Math.sin(angle) * 3, 1, 0, 0, 0, 0);
        }
        int served = 0;
        for (LivingEntity enemy : enemies) {
            float share = Spells.isBoss(enemy) ? CONQUEST_BOSS_SHARE : CONQUEST_SHARE;
            Spells.strikeThrough(player, enemy, enemy.getMaxHealth() * share);
            enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, CONQUEST_KNEEL_TICKS, 4));
            enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, CONQUEST_KNEEL_TICKS, 2));
            enemy.setDeltaMovement(0, -.6, 0);
            enemy.hurtMarked = true;
            crown(level, enemy);
            if (enemy instanceof Mob mob && !(enemy instanceof Player) && !Spells.isBoss(enemy) && mob.isAlive()
                    && mob.getHealth() < mob.getMaxHealth() * CONQUEST_BREAKS_BELOW && served < CONQUEST_MOST
                    && Marionette.controlInGroup(mob, player, CONQUEST_SERVE_TICKS, "red_priest_conquest", CONQUEST_MOST)) {
                mob.setTarget(null);
                served++;
            } else if (enemy instanceof ServerPlayer other) {
                other.sendSystemMessage(Component.literal(player.getName().getString() + " conquers you. Kneel.")
                        .withStyle(ChatFormatting.GOLD));
            }
        }
        player.sendOverlayMessage(Component.literal(enemies.size() + " kneel" + (served > 0 ? ", " + served + " now serve you" : "") + ".")
                .withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** A small gold crown over a conquered head. */
    private static void crown(ServerLevel level, LivingEntity enemy) {
        Vec3 top = enemy.position().add(0, enemy.getBbHeight() + .35, 0);
        double r = Math.max(.3, enemy.getBbWidth() * .45);
        for (int i = 0; i < 10; i++) {
            double angle = i * Math.PI / 5;
            double lift = i % 2 == 0 ? .25 : 0;
            level.sendParticles(new DustParticleOptions(GOLD, .9f), top.x + Math.cos(angle) * r, top.y + lift,
                    top.z + Math.sin(angle) * r, 1, 0, 0, 0, 0);
        }
    }

    // =========================
    // Crimson Dominion (Sequence 0)
    // =========================

    public static boolean crimsonDominion(ServerPlayer player, ServerLevel level) {
        Vec3 at = player.position();
        Spells.sound(level, at, SoundEvents.WITHER_SPAWN, 1.5f, .5f);
        Spells.sound(level, at, SoundEvents.RAID_HORN.value(), 3f, .7f);
        for (ServerPlayer near : level.getEntitiesOfClass(ServerPlayer.class, player.getBoundingBox().inflate(48)))
            ScreenImages.showFullScreen(near, "red_priest_dominion", "red_priest/war_flash", 40);
        for (int i = 0; i < 80; i++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2, r = player.getRandom().nextDouble() * DOMINION_RADIUS;
            level.sendParticles(new DustParticleOptions(CRIMSON, 2.5f), at.x + Math.cos(angle) * r, at.y + 14 + player.getRandom().nextDouble() * 3,
                    at.z + Math.sin(angle) * r, 1, .5, .2, .5, 0);
        }
        for (int i = 0; i < DOMINION_SPEARS; i++) {
            int index = i;
            Later.run(level, 10 + i * 2, () -> {
                if (!player.isAlive()) return;
                List<LivingEntity> enemies = Spells.enemiesAround(player, level, at, DOMINION_RADIUS);
                Vec3 target;
                if (!enemies.isEmpty() && index % 3 != 2) target = enemies.get(index % enemies.size()).position();
                else {
                    double angle = player.getRandom().nextDouble() * Math.PI * 2, r = 3 + player.getRandom().nextDouble() * (DOMINION_RADIUS - 3);
                    target = Spells.groundAt(player, level, at.add(Math.cos(angle) * r, 4, Math.sin(angle) * r));
                    if (target == null) return;
                }
                spear(level, player, target);
            });
        }
        Later.run(level, 14 + DOMINION_SPEARS * 2, () -> {
            if (!player.isAlive()) return;
            int[] ranks = {2, 2, 3, 3};
            int risen = 0;
            for (int i = 0; i < DOMINION_LEGION; i++) {
                Vec3 spot = ShadowSoldier.spotNear(level, player.position(), 2, 4, player.getRandom());
                if (spot == null) continue;
                ShadowSoldier.rise(level, spot, ranks[i % ranks.length], ShadowSoldier.LOYAL, player, true, false, DOMINION_LEGION_TICKS);
                risen++;
            }
            if (risen > 0) player.sendSystemMessage(Component.literal("Your legion rises from the ashes of the war.")
                    .withStyle(ChatFormatting.GOLD));
        });
        player.sendOverlayMessage(Component.literal("The sky bleeds for the Red Priest.").withStyle(ChatFormatting.DARK_RED));
        return true;
    }

    /** One burning spear falling from the red sky onto 'target'. */
    private static void spear(ServerLevel level, ServerPlayer player, Vec3 target) {
        for (int i = 0; i < 14; i++) {
            Vec3 p = target.add(0, 14 - i, 0);
            level.sendParticles(i < 4 ? ParticleTypes.END_ROD : ParticleTypes.FLAME, p.x, p.y, p.z, 1, .02, .1, .02, 0);
        }
        level.sendParticles(new DustParticleOptions(EMBER, 2f), target.x, target.y + .3, target.z, 12, .4, .2, .4, 0);
        level.sendParticles(ParticleTypes.LAVA, target.x, target.y + .2, target.z, 6, .3, .1, .3, 0);
        level.sendParticles(ParticleTypes.EXPLOSION, target.x, target.y + .5, target.z, 1, 0, 0, 0, 0);
        Spells.sound(level, target, SoundEvents.TRIDENT_HIT_GROUND, 1.2f, .6f);
        Spells.sound(level, target, SoundEvents.GENERIC_EXPLODE.value(), .6f, 1.2f);
        for (LivingEntity hit : Spells.enemiesAround(player, level, target, 3)) {
            Spells.strike(player, hit, DOMINION_SPEAR_DAMAGE);
            hit.igniteForSeconds(5);
        }
    }
}
