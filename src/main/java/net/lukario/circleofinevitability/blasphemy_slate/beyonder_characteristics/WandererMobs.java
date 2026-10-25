package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobAbility;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.MobSpells;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Wanderer abilities as a Beyonder mob casts them (blasphemy_slate/beyonder_mobs: MobAbility, MobCasting).
 *
 * Wanderer's abilities are written inside its keys for a player (the cost, the checks and the effect in one method),
 * so these are a mob's own versions of each, with the same numbers and the same marks on the target (the seals and the
 * Cosmic Plague are the target's NBT, which Wanderer's own events run for anything). A mob's stars are counted as a
 * player's (one more a second, up to its power level) under the same key; its hits are Spells.strike and only land
 * on its enemies.
 *
 *   1  Star Burst (9: one star), Star Storm (9, sneak: every star, a longer line)
 *   2  Dash (8: a rush that hits what it passes), Blink (8, sneak: toward a target far off)
 *   3  Buff (7: Strength, Haste, Absorption in a fight), Great Buff (7, sneak: when hurt)
 *   4  Seal (7: what the target takes is held back and lands later, half again), Seal of Defense (7, sneak: what it
 *      deals is held back)
 *   5  Transport (7: to a target far off), Summon Target (7, sneak: the target to it)
 *   6  Orbital Stars (5: its stars circle the target), Collapse (5, sneak: they detonate)
 *   7  Cosmic Plague (3: round it), Plague Fan (3, sneak: a fan of rays)
 */
public final class WandererMobs {

    private static final String P = "Wanderer";
    private static final int STAR_EVERY = 20;                    // a mob gains a star this often (as a player)
    private static final String MOB_STAR_TIMER = "wanderer_mob_star_timer";
    private static final String MOB_DASH_UNTIL = "wanderer_mob_dash_until";
    /** The enemies a dashing mob already struck this dash. Not saved. */
    private static final Map<UUID, Set<UUID>> DASH_HITS = new HashMap<>();

    private WandererMobs() {}

    /** MobAbilities calls this once. */
    public static void register() {
        // ---- key 1: Star Burst / Star Storm
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.NORMAL)
                .cost(250).cooldown(40).weight(14).range(16.0).sight()
                .when((mob, target, sl) -> stars(mob) >= 1)
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    setStars(mob, stars(mob) - 1);
                    Vec3 from = mob.getEyePosition();
                    Vec3 to = MobSpells.lineEnd(mob, sl, from, MobSpells.toward(mob, target), 16.0);
                    starLine(sl, from, to, 1, 0.0);
                    mob.level().playSound(null, mob.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1f, 1f);
                    for (LivingEntity hit : MobSpells.enemiesOnLine(mob, sl, from, to, 0.5)) {
                        Spells.strike(mob, hit, 21f);
                        break;                                               // the first one it meets
                    }
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 1, MobAbility.SNEAK).named("Star Storm")
                .cost(250).cooldown(200).weight(12).range(32.0).sight()
                .when((mob, target, sl) -> stars(mob) >= 3)
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    int count = stars(mob);
                    setStars(mob, 0);
                    Vec3 from = mob.getEyePosition();
                    Vec3 to = from.add(MobSpells.toward(mob, target).scale(32.0));
                    starLine(sl, from, to, 4, 0.2);
                    mob.level().playSound(null, mob.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2f, 1f);
                    for (LivingEntity hit : MobSpells.enemiesOnLine(mob, sl, from, to, 1.0)) Spells.strike(mob, hit, 21f + count * 3f);
                    return true;
                }));

        // ---- key 2: Dash / Blink
        MobAbilities.register(MobAbility.of(P, 2, MobAbility.NORMAL)
                .cost(250).cooldown(100).range(12.0).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > 4.0)
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    mob.setDeltaMovement(MobSpells.toward(mob, target).scale(2.5));
                    mob.hurtMarked = true;
                    mob.getPersistentData().putLong(MOB_DASH_UNTIL, sl.getGameTime() + 15);
                    DASH_HITS.remove(mob.getUUID());
                    mob.level().playSound(null, mob.blockPosition(), SoundEvents.WIND_CHARGE_THROW, SoundSource.HOSTILE, 1f, 1f);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 2, MobAbility.SNEAK).named("Blink")
                .cost(250).cooldown(160).range(32.0).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > 10.0)
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    Vec3 there = Spells.blinkSpot(mob, sl, Math.min(16.0, mob.distanceTo(target) - 2.0));
                    if (there == null || !MobSpells.stepTo(mob, sl, there)) return false;
                    mob.level().playSound(null, mob.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 1f);
                    return true;
                }));

        // ---- key 3: Buff / Great Buff
        MobAbilities.register(MobAbility.of(P, 3, MobAbility.NORMAL)
                .cost(250).cooldown(300).range(8.0)
                .when((mob, target, sl) -> !mob.hasEffect(MobEffects.STRENGTH))
                .cast((mob, target, sl) -> {
                    mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 120, 2));
                    mob.addEffect(new MobEffectInstance(MobEffects.HASTE, 120, 1));
                    mob.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 0));
                    mob.level().playSound(null, mob.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.HOSTILE, 1f, 1f);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 3, MobAbility.SNEAK).named("Great Buff")
                .cost(250).cooldown(800).anyRange()
                .when((mob, target, sl) -> MobSpells.hurt(mob, 0.5f) && !mob.hasEffect(MobEffects.RESISTANCE))
                .cast((mob, target, sl) -> {
                    mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 400, 2));
                    mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 400, 1));
                    mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 0));
                    mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0));
                    mob.level().playSound(null, mob.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.HOSTILE, 1f, 1f);
                    return true;
                }));

        // ---- key 4: Seal (offense) / Seal of Defense: Wanderer's own events run both on any target
        MobAbilities.register(MobAbility.of(P, 4, MobAbility.NORMAL)
                .cost(250).cooldown(300).range(16.0).sight()
                .when((mob, target, sl) -> !target.getPersistentData().getBooleanOr(Wanderer.NBT_WANDERER_IS_SEALED_OFFENSE, false))
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    starLine(sl, mob.getEyePosition(), MobSpells.chestOf(target), 1, 0.0);
                    target.getPersistentData().putBoolean(Wanderer.NBT_WANDERER_IS_SEALED_OFFENSE, true);
                    target.getPersistentData().putFloat(Wanderer.NBT_WANDERER_IS_SEALED_OFFENSE_DURATION, 100);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 4, MobAbility.SNEAK).named("Seal of Defense")
                .cost(250).cooldown(300).range(16.0).sight()
                .when((mob, target, sl) -> !target.getPersistentData().getBooleanOr(Wanderer.NBT_WANDERER_IS_SEALED_DEFENSE, false))
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    starLine(sl, mob.getEyePosition(), MobSpells.chestOf(target), 1, 0.0);
                    target.getPersistentData().putBoolean(Wanderer.NBT_WANDERER_IS_SEALED_DEFENSE, true);
                    target.getPersistentData().putFloat(Wanderer.NBT_WANDERER_IS_SEALED_DEFENSE_DURATION, 100);
                    return true;
                }));

        // ---- key 5: Transport / Summon Target
        MobAbilities.register(MobAbility.of(P, 5, MobAbility.NORMAL)
                .cost(250).cooldown(200).range(32.0).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > 8.0)
                .cast((mob, target, sl) -> {
                    Vec3 spot = MobSpells.spotBefore(mob, sl, target);
                    return spot != null && MobSpells.stepTo(mob, sl, spot);
                }));
        MobAbilities.register(MobAbility.of(P, 5, MobAbility.SNEAK).named("Summon Target")
                .cost(250).cooldown(300).range(32.0).sight()
                .when((mob, target, sl) -> mob.distanceTo(target) > 6.0)
                .cast((mob, target, sl) -> {
                    Vec3 spot = Spells.freeSpotNear(mob, sl, mob.position(), Spells.flatLook(mob), 1.5);
                    if (spot == null) return false;
                    target.teleportTo(spot.x, spot.y, spot.z);
                    return true;
                }));

        // ---- key 6: Orbital Stars / Collapse
        MobAbilities.register(MobAbility.of(P, 6, MobAbility.NORMAL)
                .cost(500).cooldown(200).range(24.0).sight()
                .when((mob, target, sl) -> stars(mob) >= 2 && !orbiting(mob))
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    CompoundTag data = mob.getPersistentData();
                    data.store(Wanderer.NBT_ORBITAL_TARGET, UUIDUtil.CODEC, target.getUUID());
                    data.putBoolean(Wanderer.NBT_ORBITAL_IS_BLOCK, false);
                    data.putBoolean(Wanderer.NBT_ORBITAL_ACTIVE, true);
                    data.putInt(Wanderer.NBT_ORBITAL_STAR_COUNT, stars(mob));
                    data.putInt(Wanderer.NBT_ORBITAL_TICK, 0);
                    setStars(mob, 0);
                    starLine(sl, mob.getEyePosition(), MobSpells.chestOf(target), 1, 0.05);
                    mob.level().playSound(null, mob.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1f, 1.5f);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 6, MobAbility.SNEAK).named("Collapse")
                .cooldown(60).weight(20).anyRange()                          // free, as a player's
                .when((mob, target, sl) -> orbiting(mob) && mob.getPersistentData().getIntOr(Wanderer.NBT_ORBITAL_TICK, 0) >= 60)
                .cast(WandererMobs::collapse));

        // ---- key 7: Cosmic Plague / Plague Fan
        MobAbilities.register(MobAbility.of(P, 7, MobAbility.NORMAL).named("Cosmic Plague")
                .cost(250).cooldown(400).range(6.0)
                .cast((mob, target, sl) -> {
                    Vec3 center = mob.position();
                    for (int i = 0; i < 16; i++) {
                        double angle = i * 2 * Math.PI / 16;
                        sl.sendParticles(ParticleTypes.END_ROD, center.x + Math.cos(angle) * 7, center.y + 0.1, center.z + Math.sin(angle) * 7,
                                1, 0, 0, 0, 0);
                    }
                    for (LivingEntity enemy : MobSpells.enemiesAround(mob, sl, center, 6.0)) plague(enemy);
                    return true;
                }));
        MobAbilities.register(MobAbility.of(P, 7, MobAbility.SNEAK).named("Plague Fan")
                .cost(250).cooldown(400).range(16.0).sight()
                .cast((mob, target, sl) -> {
                    MobSpells.face(mob, target);
                    Vec3 from = mob.getEyePosition();
                    Vec3 aim = MobSpells.toward(mob, target);
                    for (int offset = -25; offset <= 25; offset += 5) {
                        Vec3 way = Spells.turned(aim, offset);
                        Vec3 to = MobSpells.lineEnd(mob, sl, from, way, 16.0);
                        starLine(sl, from, to, 1, 0.0);
                        for (LivingEntity hit : MobSpells.enemiesOnLine(mob, sl, from, to, 0.5)) {
                            plague(hit);
                            break;
                        }
                    }
                    return true;
                }));
    }

    // =========================
    // Its stars, and the parts the casts share
    // =========================

    static int stars(LivingEntity mob) {
        return mob.getPersistentData().getIntOr(Wanderer.NBT_WANDERER_STAR_COUNT, 0);
    }

    private static void setStars(LivingEntity mob, int count) {
        mob.getPersistentData().putInt(Wanderer.NBT_WANDERER_STAR_COUNT, Math.max(0, count));
    }

    private static boolean orbiting(LivingEntity mob) {
        return mob.getPersistentData().getBooleanOr(Wanderer.NBT_ORBITAL_ACTIVE, false);
    }

    private static void plague(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        data.putFloat(Wanderer.NBT_COSMIC_PLAGUE_DURATION, data.getFloatOr(Wanderer.NBT_COSMIC_PLAGUE_DURATION, 0f) + 400);
    }

    /** A line of starlight, one block a step. */
    private static void starLine(ServerLevel sl, Vec3 from, Vec3 to, int count, double spread) {
        Vec3 way = to.subtract(from);
        double length = way.length();
        if (length < 1.0E-4) return;
        Vec3 step = way.scale(1.0 / length);
        for (double along = 0; along <= length; along += 1.0) {
            Vec3 at = from.add(step.scale(along));
            sl.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, count, spread, spread, spread, 0.02);
        }
    }

    /** Its orbiting stars, once a tick: they circle the target and bite it once a second. */
    private static void orbitTick(Mob mob, ServerLevel sl) {
        CompoundTag data = mob.getPersistentData();
        int count = data.getIntOr(Wanderer.NBT_ORBITAL_STAR_COUNT, 0);
        LivingEntity target = data.read(Wanderer.NBT_ORBITAL_TARGET, UUIDUtil.CODEC)
                .map(id -> sl.getEntity(id) instanceof LivingEntity found ? found : null).orElse(null);
        if (count <= 0 || target == null || target.isDeadOrDying() || target.level() != sl) {
            data.putBoolean(Wanderer.NBT_ORBITAL_ACTIVE, false);
            return;
        }
        int tick = data.getIntOr(Wanderer.NBT_ORBITAL_TICK, 0) + 1;
        data.putInt(Wanderer.NBT_ORBITAL_TICK, tick);
        if (tick % 20 == 0) Spells.strike(mob, target, 0.75f * count);
        for (int s = 0; s < count; s++) {
            double offset = 2 * Math.PI / count * s;
            double angle = Math.toRadians(tick * 6) + offset;
            double x = target.getX() + Math.cos(angle) * 2.5;
            double y = target.getY() + 1.0 + Math.sin(tick * 0.05 + offset) * 0.4;
            double z = target.getZ() + Math.sin(angle) * 2.5;
            sl.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 1, 0.05, 0.05, 0.05, 0.01);
        }
    }

    /** The orbiting stars detonate round their target: more and wider for every star. */
    private static boolean collapse(Mob mob, LivingEntity aimed, ServerLevel sl) {
        CompoundTag data = mob.getPersistentData();
        int count = data.getIntOr(Wanderer.NBT_ORBITAL_STAR_COUNT, 0);
        LivingEntity target = data.read(Wanderer.NBT_ORBITAL_TARGET, UUIDUtil.CODEC)
                .map(id -> sl.getEntity(id) instanceof LivingEntity found ? found : null).orElse(null);
        data.putBoolean(Wanderer.NBT_ORBITAL_ACTIVE, false);
        data.putInt(Wanderer.NBT_ORBITAL_STAR_COUNT, 0);
        data.remove(Wanderer.NBT_ORBITAL_TARGET);
        if (target == null || target.level() != sl || count <= 0) return false;
        Vec3 center = target.position().add(0, 1.0, 0);
        float damage = 8f + count * 5f;
        float radius = 2.5f + count * 0.4f;
        for (LivingEntity hit : MobSpells.enemiesAround(mob, sl, center, radius)) {
            float falloff = 1f - (float) (hit.position().distanceTo(center) / radius);
            Spells.strike(mob, hit, Math.max(damage * falloff, 4f));
            Spells.push(hit, center, 1.2, 0.5);
        }
        sl.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 40, radius * 0.3, radius * 0.3, radius * 0.3, 0.2);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z, 60, radius * 0.2, radius * 0.2, radius * 0.2, 0.15);
        sl.playSound(null, target.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.5f, 0.5f);
        return true;
    }

    // =========================
    // Events
    // =========================

    @Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class Events {

        /** A Wanderer mob: its stars come back, its dash strikes what it passes, its orbiting stars circle. */
        @SubscribeEvent
        public static void onWandererMobTick(LivingEvent.LivingTickEvent event) {
            if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel sl)) return;
            if (!P.equals(Beyonders.pathwayOf(mob))) return;
            CompoundTag data = mob.getPersistentData();
            int timer = data.getIntOr(MOB_STAR_TIMER, 0) + 1;
            if (timer >= STAR_EVERY) {
                timer = 0;
                if (stars(mob) < Beyonder.powerLevel(mob)) setStars(mob, stars(mob) + 1);
            }
            data.putInt(MOB_STAR_TIMER, timer);
            if (data.getLongOr(MOB_DASH_UNTIL, 0L) > sl.getGameTime()) {
                Set<UUID> struck = DASH_HITS.computeIfAbsent(mob.getUUID(), id -> new HashSet<>());
                for (LivingEntity hit : MobSpells.enemiesAround(mob, sl, mob.position(), 1.5)) {
                    if (struck.add(hit.getUUID())) Spells.strike(mob, hit, 24f);
                }
            } else if (data.contains(MOB_DASH_UNTIL)) {
                data.remove(MOB_DASH_UNTIL);
                DASH_HITS.remove(mob.getUUID());
            }
            if (orbiting(mob)) orbitTick(mob, sl);
        }
    }
}
