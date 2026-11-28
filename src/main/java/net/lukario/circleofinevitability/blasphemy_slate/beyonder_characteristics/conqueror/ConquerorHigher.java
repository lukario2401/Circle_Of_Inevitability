package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.conqueror;

import net.lukario.circleofinevitability.CircleOfInevitability;
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

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Conqueror Pathway's abilities above Sequence 3, and its one heal. For now they are lent only by the Red Priest
 * uniqueness (item/artifacts/RedPriest): the Pathway's seven keys are full, so giving them to Conqueror Beyonders
 * means a wheel on a key, like the other Pathways' Sequence 2..0 wheels. Each returns whether it went off.
 *
 *   Iron Blood (Sequence 4, Iron-blooded Knight): heal a third of your health and put out the fire in you.
 *   Storm of War (Sequence 2, Weather Warlock): a storm over where you look, lightning on everyone in it, then a gale.
 *   Hailstorm (Sequence 2): a storm of ice over where you look that batters, slows and freezes.
 *   War Cyclone (Sequence 2): a whirlwind that drags enemies in, grinds them, then flings them away.
 *   Conquest (Sequence 1, Conqueror): everyone hostile in sight kneels, and the weakest of them fight for you.
 *   Banner of Conquest (Sequence 1): a war banner that strengthens you and your allies and weakens enemies around it.
 *   Subjugation (Sequence 1): break one enemy; a broken mob serves you.
 *   Crimson Dominion (Sequence 0, Red Priest): the sky bleeds, burning spears rain on your enemies, and a legion of
 *   shadow soldiers rises to fight for you.
 *   Spear of Judgement (Sequence 0): a giant burning spear thrown through everything in a line, bursting at the end.
 *   Endless War (Sequence 0): for a while, everyone you kill rises again as your shadow soldier.
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

    // Hailstorm
    public static final double HAIL_RANGE = 40, HAIL_RADIUS = 8;
    public static final int HAIL_WAVES = 16, HAIL_EVERY = 5;
    public static final float HAIL_DAMAGE = 3;
    // War Cyclone
    public static final double CYCLONE_RANGE = 32, CYCLONE_RADIUS = 9;
    public static final int CYCLONE_TICKS = 20 * 5;
    public static final float CYCLONE_GRIND = 2, CYCLONE_FLING = 10;
    // Banner of Conquest
    public static final double BANNER_RADIUS = 12;
    public static final int BANNER_SECONDS = 20;
    // Subjugation
    public static final double SUBJUGATE_RANGE = 24;
    public static final float SUBJUGATE_SHARE = .4f, SUBJUGATE_BOSS_SHARE = .12f, SUBJUGATE_BREAKS_BELOW = .5f;
    public static final int SUBJUGATE_SERVE_TICKS = 20 * 60, SUBJUGATE_MOST = 3;
    // Spear of Judgement
    public static final double JUDGEMENT_RANGE = 48, JUDGEMENT_BURST = 5;
    public static final float JUDGEMENT_DAMAGE = 60, JUDGEMENT_BURST_DAMAGE = 30;
    // Endless War
    public static final int ENDLESS_WAR_TICKS = 20 * 30, ENDLESS_WAR_SOLDIER_TICKS = 20 * 60, ENDLESS_WAR_MOST = 8;

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

    // =========================
    // Hailstorm (Sequence 2)
    // =========================

    public static boolean hailstorm(ServerPlayer player, ServerLevel level) {
        Vec3 center = Spells.aimGround(player, level, HAIL_RANGE);
        if (center == null) {
            player.sendOverlayMessage(Component.literal("There is no ground there for the hail."));
            return false;
        }
        Spells.sound(level, center, SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2f, 1.4f);
        for (int i = 0; i < 30; i++) {
            double angle = player.getRandom().nextDouble() * Math.PI * 2, r = player.getRandom().nextDouble() * HAIL_RADIUS;
            level.sendParticles(ParticleTypes.CLOUD, center.x + Math.cos(angle) * r, center.y + 10, center.z + Math.sin(angle) * r,
                    2, .6, .2, .6, .01);
        }
        for (int wave = 0; wave < HAIL_WAVES; wave++) {
            Later.run(level, 4 + wave * HAIL_EVERY, () -> {
                if (!player.isAlive()) return;
                for (int i = 0; i < 14; i++) {
                    double angle = player.getRandom().nextDouble() * Math.PI * 2, r = player.getRandom().nextDouble() * HAIL_RADIUS;
                    level.sendParticles(ParticleTypes.ITEM_SNOWBALL, center.x + Math.cos(angle) * r, center.y + 6,
                            center.z + Math.sin(angle) * r, 0, 0, -1.2, 0, 1);
                    level.sendParticles(ParticleTypes.SNOWFLAKE, center.x + Math.cos(angle) * r, center.y + .3,
                            center.z + Math.sin(angle) * r, 1, .2, 0, .2, .02);
                }
                Spells.sound(level, center, SoundEvents.GLASS_BREAK, .5f, 1.6f + player.getRandom().nextFloat() * .3f);
                for (LivingEntity hit : Spells.enemiesAround(player, level, center, HAIL_RADIUS)) {
                    Spells.strike(player, hit, HAIL_DAMAGE);
                    hit.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 2));
                    hit.setTicksFrozen(Math.min(hit.getTicksRequiredToFreeze() + 60, hit.getTicksFrozen() + 12));
                }
            });
        }
        player.sendOverlayMessage(Component.literal("The sky turns to ice.").withStyle(ChatFormatting.AQUA));
        return true;
    }

    // =========================
    // War Cyclone (Sequence 2)
    // =========================

    public static boolean warCyclone(ServerPlayer player, ServerLevel level) {
        Vec3 center = Spells.aimGround(player, level, CYCLONE_RANGE);
        if (center == null) {
            player.sendOverlayMessage(Component.literal("There is no ground there for the wind."));
            return false;
        }
        Spells.sound(level, center, SoundEvents.BREEZE_WHIRL, 2f, .5f);
        for (int t = 0; t < CYCLONE_TICKS; t += 2) {
            int tick = t;
            Later.run(level, t, () -> {
                if (!player.isAlive()) return;
                double spin = tick * .45;
                for (int i = 0; i < 6; i++) {
                    double y = i * 1.1, r = 1 + y * .45, angle = spin + i * 1.2;
                    level.sendParticles(i % 2 == 0 ? ParticleTypes.CLOUD : ParticleTypes.POOF, center.x + Math.cos(angle) * r,
                            center.y + y, center.z + Math.sin(angle) * r, 1, 0, 0, 0, 0);
                }
                if (tick % 10 == 0) {
                    Spells.sound(level, center, SoundEvents.BREEZE_IDLE_GROUND, 1f, .6f);
                    for (LivingEntity hit : Spells.enemiesAround(player, level, center, CYCLONE_RADIUS)) {
                        Spells.pull(hit, center.add(0, 1.5, 0), .9, .25);
                        if (hit.distanceToSqr(center) < 9) Spells.strike(player, hit, CYCLONE_GRIND);
                    }
                }
            });
        }
        Later.run(level, CYCLONE_TICKS, () -> {
            if (!player.isAlive()) return;
            Spells.sound(level, center, SoundEvents.WIND_CHARGE_BURST.value(), 2f, .6f);
            level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 1, center.z, 3, 1, .5, 1, 0);
            for (LivingEntity hit : Spells.enemiesAround(player, level, center, CYCLONE_RADIUS)) {
                Spells.strike(player, hit, CYCLONE_FLING);
                Spells.push(hit, center, 1.8, 1.1);
            }
        });
        player.sendOverlayMessage(Component.literal("The wind marches for you.").withStyle(ChatFormatting.GOLD));
        return true;
    }

    // =========================
    // Banner of Conquest (Sequence 1)
    // =========================

    public static boolean bannerOfConquest(ServerPlayer player, ServerLevel level) {
        Vec3 at = player.position();
        Spells.sound(level, at, SoundEvents.RAID_HORN.value(), 2f, 1.3f);
        Spells.sound(level, at, SoundEvents.ANVIL_PLACE, .7f, .6f);
        for (int s = 0; s < BANNER_SECONDS; s++) {
            Later.run(level, s * 20, () -> {
                if (!player.isAlive() || player.level() != level) return;
                for (int i = 0; i < 12; i++)
                    level.sendParticles(new DustParticleOptions(i > 8 ? GOLD : CRIMSON, 1.3f), at.x, at.y + i * .3, at.z, 1, 0, 0, 0, 0);
                for (int i = 0; i < 6; i++)
                    level.sendParticles(new DustParticleOptions(CRIMSON, 1.6f), at.x + .2 + i * .15, at.y + 3.2 - (i % 2) * .15, at.z,
                            1, 0, .2, 0, 0);
                for (int i = 0; i < 32; i++) {
                    double angle = i * Math.PI / 16;
                    level.sendParticles(new DustParticleOptions(GOLD, .9f), at.x + Math.cos(angle) * BANNER_RADIUS, at.y + .2,
                            at.z + Math.sin(angle) * BANNER_RADIUS, 1, 0, 0, 0, 0);
                }
                List<LivingEntity> allies = new ArrayList<>(Spells.alliesAround(player, level, at, BANNER_RADIUS));
                if (player.position().distanceTo(at) <= BANNER_RADIUS && !allies.contains(player)) allies.add(player);
                for (LivingEntity ally : allies) {
                    ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 1));
                    ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 0));
                    ally.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0));
                }
                for (LivingEntity enemy : Spells.enemiesAround(player, level, at, BANNER_RADIUS)) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1));
                    enemy.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0));
                    enemy.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0));
                }
            });
        }
        player.sendOverlayMessage(Component.literal("The banner of conquest is raised.").withStyle(ChatFormatting.GOLD));
        return true;
    }

    // =========================
    // Subjugation (Sequence 1)
    // =========================

    public static boolean subjugation(ServerPlayer player, ServerLevel level) {
        LivingEntity target = Spells.aimEnemy(player, level, SUBJUGATE_RANGE);
        if (target == null) {
            player.sendOverlayMessage(Component.literal("Look at the one you would break."));
            return false;
        }
        boolean boss = Spells.isBoss(target);
        Spells.strikeThrough(player, target, target.getMaxHealth() * (boss ? SUBJUGATE_BOSS_SHARE : SUBJUGATE_SHARE));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 5));
        target.setDeltaMovement(0, -.8, 0);
        target.hurtMarked = true;
        crown(level, target);
        Vec3 at = target.position();
        level.sendParticles(new DustParticleOptions(CRIMSON, 2f), at.x, at.y + 1, at.z, 30, .4, .8, .4, 0);
        Spells.sound(level, at, SoundEvents.ANVIL_LAND, .8f, .5f);
        Spells.sound(level, at, SoundEvents.WARDEN_SONIC_BOOM, .4f, .7f);
        if (target instanceof Mob mob && !(target instanceof Player) && !boss && mob.isAlive()
                && mob.getHealth() < mob.getMaxHealth() * SUBJUGATE_BREAKS_BELOW
                && Marionette.controlInGroup(mob, player, SUBJUGATE_SERVE_TICKS, "red_priest_subjugation", SUBJUGATE_MOST)) {
            mob.setTarget(null);
            player.sendOverlayMessage(Component.literal(target.getName().getString() + " is broken and serves you.")
                    .withStyle(ChatFormatting.GOLD));
        } else {
            if (target instanceof ServerPlayer other) {
                other.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                other.sendSystemMessage(Component.literal(player.getName().getString() + " subjugates you. Kneel.")
                        .withStyle(ChatFormatting.GOLD));
            }
            player.sendOverlayMessage(Component.literal(target.getName().getString() + " kneels.").withStyle(ChatFormatting.GOLD));
        }
        return true;
    }

    // =========================
    // Spear of Judgement (Sequence 0)
    // =========================

    public static boolean spearOfJudgement(ServerPlayer player, ServerLevel level) {
        Vec3 from = player.getEyePosition();
        Vec3 to = Spells.aimPoint(player, level, JUDGEMENT_RANGE);
        if (to == null) to = from.add(player.getLookAngle().scale(JUDGEMENT_RANGE));
        Vec3 end = to;
        Vec3 step = end.subtract(from).scale(1 / 8.0);
        Spells.sound(level, from, SoundEvents.TRIDENT_THROW.value(), 2f, .5f);
        Spells.sound(level, from, SoundEvents.BLAZE_SHOOT, 1.2f, .6f);
        List<LivingEntity> struck = new ArrayList<>();
        for (int t = 0; t < 8; t++) {
            int tick = t;
            Later.run(level, t, () -> {
                if (!player.isAlive()) return;
                Vec3 a = from.add(step.scale(tick)), b = from.add(step.scale(tick + 1));
                for (int i = 0; i < 10; i++) {
                    Vec3 p = a.add(b.subtract(a).scale(i / 10.0));
                    level.sendParticles(new DustParticleOptions(i % 3 == 0 ? GOLD : CRIMSON, 2.2f), p.x, p.y, p.z, 1, .05, .05, .05, 0);
                    level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 1, .15, .15, .15, .01);
                }
                for (LivingEntity hit : Spells.enemiesOnLine(player, level, a, b, 1.5)) {
                    if (struck.contains(hit)) continue;
                    struck.add(hit);
                    Spells.strike(player, hit, JUDGEMENT_DAMAGE);
                    hit.igniteForSeconds(8);
                }
            });
        }
        Later.run(level, 8, () -> {
            if (!player.isAlive()) return;
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, end.x, end.y, end.z, 1, 0, 0, 0, 0);
            level.sendParticles(ParticleTypes.LAVA, end.x, end.y, end.z, 20, 1.5, .5, 1.5, 0);
            level.sendParticles(new DustParticleOptions(EMBER, 3f), end.x, end.y + 1, end.z, 40, 2, 1, 2, 0);
            Spells.sound(level, end, SoundEvents.GENERIC_EXPLODE.value(), 2f, .7f);
            for (LivingEntity hit : Spells.enemiesAround(player, level, end, JUDGEMENT_BURST)) {
                Spells.strike(player, hit, JUDGEMENT_BURST_DAMAGE);
                hit.igniteForSeconds(8);
                Spells.push(hit, end, 1.2, .5);
            }
        });
        player.sendOverlayMessage(Component.literal("Judgement.").withStyle(ChatFormatting.DARK_RED));
        return true;
    }

    // =========================
    // Endless War (Sequence 0)
    // =========================

    private static final Map<UUID, Long> ENDLESS_WAR = new ConcurrentHashMap<>();

    public static boolean endlessWar(ServerPlayer player, ServerLevel level) {
        ENDLESS_WAR.put(player.getUUID(), level.getGameTime() + ENDLESS_WAR_TICKS);
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, ENDLESS_WAR_TICKS, 1));
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, ENDLESS_WAR_TICKS, 1));
        Vec3 at = player.position();
        Spells.sound(level, at, SoundEvents.RAID_HORN.value(), 3f, .5f);
        Spells.sound(level, at, SoundEvents.WITHER_AMBIENT, 1f, .5f);
        for (int s = 0; s < ENDLESS_WAR_TICKS / 20; s++)
            Later.run(level, s * 20, () -> {
                if (!player.isAlive()) return;
                Vec3 p = player.position();
                level.sendParticles(new DustParticleOptions(CRIMSON, 1.4f), p.x, p.y + 1, p.z, 12, .5, .8, .5, 0);
                level.sendParticles(ParticleTypes.SOUL, p.x, p.y + .2, p.z, 2, .6, .1, .6, .01);
            });
        player.sendOverlayMessage(Component.literal("The war will not end. The fallen rise for you.").withStyle(ChatFormatting.DARK_RED));
        return true;
    }

    /** Whether this player's Endless War is still raging. */
    public static boolean endlessWarActive(ServerPlayer player) {
        Long until = ENDLESS_WAR.get(player.getUUID());
        if (until == null) return false;
        if (player.level().getGameTime() <= until) return true;
        ENDLESS_WAR.remove(player.getUUID());
        return false;
    }

    /** The rank a fallen enemy rises as: the stronger it was, the higher. */
    public static int endlessWarRank(LivingEntity fallen) {
        float health = fallen.getMaxHealth();
        return health >= 80 ? 3 : health >= 40 ? 2 : health >= 20 ? 1 : 0;
    }

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class EndlessWarEvents {
        private EndlessWarEvents() {}

        @SubscribeEvent
        public static void onFallen(LivingDeathEvent event) {
            LivingEntity fallen = event.getEntity();
            if (!(fallen.level() instanceof ServerLevel level) || fallen instanceof ShadowSoldier) return;
            if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || killer == fallen || !endlessWarActive(killer)) return;
            long risen = level.getEntitiesOfClass(ShadowSoldier.class, killer.getBoundingBox().inflate(64),
                    s -> s.loyal() && killer.getUUID().equals(s.owner())).size();
            if (risen >= ENDLESS_WAR_MOST) return;
            Vec3 at = fallen.position();
            Later.run(level, 10, () -> {
                if (!killer.isAlive()) return;
                ShadowSoldier.rise(level, at, endlessWarRank(fallen), ShadowSoldier.LOYAL, killer, true, false, ENDLESS_WAR_SOLDIER_TICKS);
            });
        }
    }
}
