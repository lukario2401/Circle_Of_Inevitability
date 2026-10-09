package net.lukario.frogerealm.shadow_slave.soul_abilities.aspects;

import net.lukario.frogerealm.ForgeRealm;
import net.lukario.frogerealm.shadow_slave.soul_shards.SoulCore;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * Infernal Duelist
 * -----------------------------------------------
 * Stance-based DPS aspect. No stacks, no charges —
 * all power is immediate based on which stance is active.
 *
 * NBT keys stored on the PLAYER:
 *   "InfernalStance"         → String  "EMBER" | "BLAZE" | "INFERNO"
 *   "InfernalShiftTimer"     → int     ticks until next stance shift allowed
 *   "InfernalOverheatTimer"  → int     ticks remaining on Overheat
 *   "InfernalCataclysmTimer" → int     ticks remaining on Cataclysm Form
 *   "InfernalFlowTimer"      → int     internal tick counter for passive lifesteal
 */
public class InfernalDuelist {

    // ─── Constants ────────────────────────────────────────────────────────────
    public static final String STANCE_EMBER   = "EMBER";
    public static final String STANCE_BLAZE   = "BLAZE";
    public static final String STANCE_INFERNO = "INFERNO";

    private static final String NBT_STANCE     = "InfernalStance";
    private static final String NBT_SHIFT_CD   = "InfernalShiftTimer";
    private static final String NBT_OVERHEAT   = "InfernalOverheatTimer";
    private static final String NBT_CATACLYSM  = "InfernalCataclysmTimer";
    private static final String NBT_FLOW_TIMER = "InfernalFlowTimer";

    private static final int SHIFT_COOLDOWN = 20;

    // ─── Stance Helpers ───────────────────────────────────────────────────────

    public static String getStance(Player player) {
        String s = player.getPersistentData().getString(NBT_STANCE);
        return s.isEmpty() ? STANCE_EMBER : s;
    }

    public static void setStance(Player player, String stance) {
        player.getPersistentData().putString(NBT_STANCE, stance);
    }

    public static boolean inOverheat(Player player) {
        return player.getPersistentData().getInt(NBT_OVERHEAT) > 0;
    }

    public static boolean inCataclysm(Player player) {
        return player.getPersistentData().getInt(NBT_CATACLYSM) > 0;
    }

    private static String stanceTag(String stance) {
        return switch (stance) {
            case STANCE_EMBER   -> "§c[Ember]";
            case STANCE_BLAZE   -> "§6[Blaze]";
            case STANCE_INFERNO -> "§4[Inferno]";
            default             -> "§7[?]";
        };
    }

    // ─── Ray-cast helper ──────────────────────────────────────────────────────

    private static LivingEntity rayCastFirst(Player player, Level level, int blocks) {
        Vec3 start = player.getEyePosition();
        Vec3 dir   = player.getLookAngle().normalize();
        Vec3 cur   = start;
        for (int i = 0; i < blocks; i++) {
            cur = cur.add(dir);
            List<LivingEntity> hits = level.getEntitiesOfClass(
                    LivingEntity.class, new AABB(cur, cur).inflate(0.5),
                    e -> e != player && e.isAlive());
            if (!hits.isEmpty()) return hits.get(0);
        }
        return null;
    }

    // ─── Fire wave helper ─────────────────────────────────────────────────────

    private static void fireWave(Player player, Level level, ServerLevel sl, Vec3 origin) {
        if (!inCataclysm(player)) return;
        Vec3 dir = player.getLookAngle().normalize();
        Vec3 cur = origin;
        for (int i = 0; i < 6; i++) {
            cur = cur.add(dir);
            sl.sendParticles(ParticleTypes.FLAME, cur.x, cur.y + 0.5, cur.z, 3, 0.3, 0.3, 0.3, 0.04);
            level.getEntitiesOfClass(LivingEntity.class, new AABB(cur, cur).inflate(0.6),
                            e -> e != player && e.isAlive())
                    .forEach(e -> {
                        e.hurt(level.damageSources().playerAttack(player), 4.0f);
                        e.invulnerableTime = 0;
                        e.setRemainingFireTicks(60);
                    });
        }
    }

    // =========================================================================
    //  TICK EVENTS — unique class name to avoid Forge scanner collision
    // =========================================================================

    @Mod.EventBusSubscriber(modid = ForgeRealm.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class InfernalEvents {

        @SubscribeEvent
        public static void onInfernalPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            Player player = event.player;
            if (!(player.level() instanceof ServerLevel sl)) return;
            if (!SoulCore.getAspect(player).equals("Infernal Duelist")) return;

            // ── Shift cooldown ────────────────────────────────────────────────
            int shiftCd = player.getPersistentData().getInt(NBT_SHIFT_CD);
            if (shiftCd > 0) player.getPersistentData().putInt(NBT_SHIFT_CD, shiftCd - 1);

            // ── Overheat countdown ────────────────────────────────────────────
            int overheat = player.getPersistentData().getInt(NBT_OVERHEAT);
            if (overheat > 0) {
                player.getPersistentData().putInt(NBT_OVERHEAT, overheat - 1);
                if (player.tickCount % 5 == 0)
                    sl.sendParticles(ParticleTypes.FLAME,
                            player.getX(), player.getY() + 1, player.getZ(),
                            3, 0.4, 0.4, 0.4, 0.03);
            }

            // ── Cataclysm countdown ───────────────────────────────────────────
            int cataclysm = player.getPersistentData().getInt(NBT_CATACLYSM);
            if (cataclysm > 0) {
                player.getPersistentData().putInt(NBT_CATACLYSM, cataclysm - 1);
                if (player.tickCount % 5 == 0)
                    sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                            player.getX(), player.getY() + 1, player.getZ(),
                            6, 0.5, 0.5, 0.5, 0.04);
            }

            // ── Burning Flow passive (every 20 ticks = 1 second) ─────────────
            int flowTimer = player.getPersistentData().getInt(NBT_FLOW_TIMER);
            flowTimer--;
            if (flowTimer <= 0) {
                flowTimer = 20;
                applyFlowBuffs(player, getStance(player));
            }
            player.getPersistentData().putInt(NBT_FLOW_TIMER, flowTimer);
        }

        private static void applyFlowBuffs(Player player, String stance) {
            boolean overheat  = inOverheat(player);
            boolean cataclysm = inCataclysm(player);

            if (cataclysm) {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,      25, 2, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, 1, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,   25, 2, true, false));
                return;
            }
            switch (stance) {
                case STANCE_EMBER -> {
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,      25, overheat ? 3 : 1, true, false));
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 25, overheat ? 2 : 0, true, false));
                    player.heal(overheat ? 0.5f : 0.25f);
                }
                case STANCE_BLAZE -> {
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,    25, overheat ? 2 : 0, true, false));
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 25, overheat ? 1 : 0, true, false));
                }
                case STANCE_INFERNO -> {
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 25, overheat ? 3 : 1, true, false));
                    player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED,    25, -1,               true, false));
                }
            }
        }

        /** On kill during Cataclysm Form: +2 seconds. */
        @SubscribeEvent
        public static void onInfernalKill(LivingDeathEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Infernal Duelist")) return;
            if (!inCataclysm(player)) return;

            int current = player.getPersistentData().getInt(NBT_CATACLYSM);
            player.getPersistentData().putInt(NBT_CATACLYSM, current + 40);

            if (player.level() instanceof ServerLevel sl)
                sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                        event.getEntity().getX(), event.getEntity().getY() + 1,
                        event.getEntity().getZ(), 8, 0.3, 0.3, 0.3, 0.05);

            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal("§4Cataclysm extended! §c+2s"));
        }

        /** Fire wave on every melee hit during Cataclysm Form. */
        @SubscribeEvent
        public static void onInfernalMeleeHit(LivingHurtEvent event) {
            if (!(event.getSource().getEntity() instanceof Player player)) return;
            if (!SoulCore.getAspect(player).equals("Infernal Duelist")) return;
            if (!inCataclysm(player)) return;
            if (!(player.level() instanceof ServerLevel sl)) return;
            fireWave(player, player.level(), sl, event.getEntity().position());
        }
    }

    // =========================================================================
    //  ABILITY 1 — FLAME CUT
    // =========================================================================

    public static void flameCut(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Infernal Duelist")) return;

        String  stance   = inCataclysm(player) ? STANCE_INFERNO : getStance(player);
        boolean overheat = inOverheat(player);

        int   essenceCost;
        float damage;
        int   range;
        float aoeRadius;

        switch (stance) {
            case STANCE_EMBER -> { essenceCost = 200; damage = overheat ? 14f : 8f;  range = 8;  aoeRadius = 0; }
            case STANCE_BLAZE -> { essenceCost = 350; damage = overheat ? 28f : 18f; range = 10; aoeRadius = overheat ? 4f : 2f; }
            default           -> { essenceCost = 600; damage = overheat ? 65f : 40f; range = 12; aoeRadius = 0; }
        }
        if (inCataclysm(player)) damage *= 1.5f;
        if (SoulCore.getSoulEssence(player) < essenceCost) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - essenceCost);

        Vec3 start = player.getEyePosition();
        Vec3 dir   = player.getLookAngle().normalize();
        Vec3 cur   = start;
        LivingEntity primaryHit = null;

        for (int i = 0; i < range; i++) {
            cur = cur.add(dir);
            var particle = switch (stance) {
                case STANCE_EMBER -> ParticleTypes.FLAME;
                case STANCE_BLAZE -> ParticleTypes.LARGE_SMOKE;
                default           -> ParticleTypes.SOUL_FIRE_FLAME;
            };
            sl.sendParticles(particle, cur.x, cur.y, cur.z, 2, 0.1, 0.1, 0.1, 0.02);

            List<LivingEntity> hits = level.getEntitiesOfClass(
                    LivingEntity.class, new AABB(cur, cur).inflate(0.5),
                    e -> e != player && e.isAlive());

            if (!hits.isEmpty() && primaryHit == null) {
                primaryHit = hits.get(0);
                primaryHit.hurt(level.damageSources().playerAttack(player), damage);
                primaryHit.invulnerableTime = 0;
                primaryHit.setRemainingFireTicks(stance.equals(STANCE_INFERNO) ? 100 : 40);
                sl.sendParticles(ParticleTypes.EXPLOSION,
                        primaryHit.getX(), primaryHit.getY() + 1, primaryHit.getZ(), 4, 0.3, 0.3, 0.3, 0.02);
                if (inCataclysm(player)) fireWave(player, level, sl, primaryHit.position());

                if (aoeRadius > 0) {
                    final LivingEntity fh  = primaryHit;
                    final float        fd  = damage;
                    level.getEntitiesOfClass(LivingEntity.class,
                                    primaryHit.getBoundingBox().inflate(aoeRadius),
                                    e -> e != player && e != fh && e.isAlive())
                            .forEach(e -> {
                                e.hurt(level.damageSources().playerAttack(player), fd * 0.5f);
                                e.invulnerableTime = 0;
                                e.setRemainingFireTicks(40);
                            });
                }
                break;
            }
        }

        level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS,
                0.8f, stance.equals(STANCE_EMBER) ? 1.4f : stance.equals(STANCE_BLAZE) ? 1.0f : 0.6f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    stanceTag(stance) + " §cFlame Cut: §f" + damage + " dmg"
                            + (aoeRadius > 0 ? " §7+ AOE r" + (int) aoeRadius : "")
                            + (overheat ? " §6[Overheat]" : "")));
    }

    // =========================================================================
    //  ABILITY 2 — STANCE SHIFT
    // =========================================================================

    public static void stanceShift(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Infernal Duelist")) return;

        int shiftCd = player.getPersistentData().getInt(NBT_SHIFT_CD);
        if (shiftCd > 0) {
            if (player instanceof ServerPlayer sp)
                sp.sendSystemMessage(Component.literal(
                        "§cStance Shift on cooldown! §7(" + (int) Math.ceil(shiftCd / 20.0) + "s)"));
            return;
        }

        String current = getStance(player);
        String next    = switch (current) {
            case STANCE_EMBER -> STANCE_BLAZE;
            case STANCE_BLAZE -> STANCE_INFERNO;
            default           -> STANCE_EMBER;
        };

        setStance(player, next);
        player.getPersistentData().putInt(NBT_SHIFT_CD, SHIFT_COOLDOWN);

        if (!current.equals(STANCE_INFERNO)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,   60, 0, false, true));
        } else {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1, false, true));
        }

        var particle = switch (next) {
            case STANCE_EMBER -> ParticleTypes.FLAME;
            case STANCE_BLAZE -> ParticleTypes.LARGE_SMOKE;
            default           -> ParticleTypes.SOUL_FIRE_FLAME;
        };
        sl.sendParticles(particle, player.getX(), player.getY() + 1, player.getZ(), 12, 0.4, 0.4, 0.4, 0.05);
        sl.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6f, 1.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§6Stance Shift: " + stanceTag(current) + " §6→ " + stanceTag(next)));
    }

    // =========================================================================
    //  ABILITY 3 — IGNITION BURST
    // =========================================================================

    public static void ignitionBurst(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Infernal Duelist")) return;
        if (SoulCore.getAscensionStage(player) < 2) return;

        String  stance   = inCataclysm(player) ? STANCE_INFERNO : getStance(player);
        boolean overheat = inOverheat(player);

        float radius; float damage; int cost;
        switch (stance) {
            case STANCE_EMBER -> { radius = overheat ? 5f  : 3f;  damage = overheat ? 20f : 12f; cost = 300; }
            case STANCE_BLAZE -> { radius = overheat ? 9f  : 6f;  damage = overheat ? 38f : 25f; cost = 500; }
            default           -> { radius = overheat ? 14f : 10f; damage = overheat ? 80f : 50f; cost = 900; }
        }
        if (inCataclysm(player)) damage *= 1.5f;
        if (SoulCore.getSoulEssence(player) < cost) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - cost);

        Vec3 center = player.position().add(player.getLookAngle().normalize().scale(2));
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, new AABB(center, center).inflate(radius),
                e -> e != player && e.isAlive());

        for (LivingEntity e : targets) {
            e.hurt(level.damageSources().playerAttack(player), damage);
            e.invulnerableTime = 0;
            e.setRemainingFireTicks(stance.equals(STANCE_INFERNO) ? 100 : 60);
            Vec3 push = e.position().subtract(center).normalize().scale(0.8);
            e.setDeltaMovement(e.getDeltaMovement().add(push.x, 0.4, push.z));
        }

        for (int ring = 1; ring <= 3; ring++) {
            double r = radius * ring / 3.0;
            int count = (int)(r * 8);
            for (int i = 0; i < count; i++) {
                double angle = (2 * Math.PI / count) * i;
                sl.sendParticles(ParticleTypes.FLAME,
                        center.x + r * Math.cos(angle), center.y + 0.5,
                        center.z + r * Math.sin(angle), 1, 0, 0.2, 0, 0.02);
            }
        }
        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y + 1, center.z, 1, 0, 0, 0, 0);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.HOSTILE, 1.2f, stance.equals(STANCE_INFERNO) ? 0.5f : 0.9f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    stanceTag(stance) + " §cIgnition Burst: §f" + damage
                            + " dmg, r" + (int) radius + ", hit §c" + targets.size() + "§f enemies."));
    }

    // =========================================================================
    //  ABILITY 4 — FLARE DASH
    // =========================================================================

    public static void flareDash(Player player, Level level, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Infernal Duelist")) return;
        if (SoulCore.getAscensionStage(player) < 3) return;

        String  stance   = inCataclysm(player) ? STANCE_INFERNO : getStance(player);
        boolean overheat = inOverheat(player);

        double dashScale; float damage; float cleaveRadius; int cost;
        switch (stance) {
            case STANCE_EMBER -> { dashScale = overheat ? 5.0 : 3.5; damage = overheat ? 18f : 10f; cleaveRadius = 0;                    cost = 400; }
            case STANCE_BLAZE -> { dashScale = overheat ? 3.5 : 2.5; damage = overheat ? 32f : 20f; cleaveRadius = overheat ? 5f : 3f;   cost = 500; }
            default           -> { dashScale = overheat ? 2.5 : 1.5; damage = overheat ? 70f : 45f; cleaveRadius = 0;                    cost = 700; }
        }
        if (inCataclysm(player)) damage *= 1.5f;
        if (SoulCore.getSoulEssence(player) < cost) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - cost);

        Vec3 dir = player.getLookAngle().normalize();
        player.setDeltaMovement(dir.scale(dashScale));
        player.hurtMarked = true;

        Vec3 pos = player.position();
        for (int i = 0; i < 8; i++) {
            Vec3 trail = pos.subtract(dir.scale(i * 0.4));
            sl.sendParticles(ParticleTypes.FLAME, trail.x, trail.y + 0.5, trail.z, 2, 0.1, 0.1, 0.1, 0.02);
        }

        AABB impactBox = player.getBoundingBox().inflate(cleaveRadius > 0 ? cleaveRadius : 1.5);
        List<LivingEntity> targets = level.getEntitiesOfClass(
                LivingEntity.class, impactBox, e -> e != player && e.isAlive());

        for (LivingEntity e : targets) {
            e.hurt(level.damageSources().playerAttack(player), damage);
            e.invulnerableTime = 0;
            e.setRemainingFireTicks(40);
            if (inCataclysm(player)) fireWave(player, level, sl, e.position());
        }

        level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS,
                1f, stance.equals(STANCE_EMBER) ? 1.6f : stance.equals(STANCE_BLAZE) ? 1.2f : 0.7f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    stanceTag(stance) + " §cFlare Dash: §f" + damage + " impact"
                            + (cleaveRadius > 0 ? " §7+ cleave r" + (int) cleaveRadius : "")));
    }

    // =========================================================================
    //  ABILITY 6 — OVERHEAT
    // =========================================================================

    public static void overheat(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Infernal Duelist")) return;
        if (SoulCore.getSoulEssence(player) < 2000) return;
        if (SoulCore.getAscensionStage(player) < 5) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 2000);

        player.getPersistentData().putInt(NBT_OVERHEAT, 120);

        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                player.getX(), player.getY() + 1, player.getZ(), 20, 0.5, 0.5, 0.5, 0.06);
        sl.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.5f, 0.5f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§4§lOVERHEAT! §r" + stanceTag(getStance(player)) + " §camplified for §l6s§c!"));
    }

    // =========================================================================
    //  ABILITY 7 — CATACLYSM FORM
    // =========================================================================

    public static void cataclysmForm(Player player, ServerLevel sl) {
        if (!SoulCore.getAspect(player).equals("Infernal Duelist")) return;
        if (SoulCore.getSoulEssence(player) < 6000) return;
        if (SoulCore.getAscensionStage(player) < 7) return;
        SoulCore.setSoulEssence(player, SoulCore.getSoulEssence(player) - 6000);

        player.getPersistentData().putInt(NBT_CATACLYSM, 200);

        sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                player.getX(), player.getY() + 1, player.getZ(), 3, 0.5, 0.5, 0.5, 0);
        sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                player.getX(), player.getY() + 1, player.getZ(), 40, 1.0, 1.0, 1.0, 0.08);
        sl.playSound(null, player.blockPosition(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.5f, 0.7f);

        if (player instanceof ServerPlayer sp)
            sp.sendSystemMessage(Component.literal(
                    "§4§l🔥 CATACLYSM FORM! §r§cAll stances. Inferno damage. Fire waves. Kills extend duration."));
    }
}