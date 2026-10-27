package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.DamageFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.SpellFx;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.fate.DisasterBalance;
import net.lukario.circleofinevitability.fate.Fate;
import net.lukario.circleofinevitability.fate.WheelTier;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Misfortune: an entity's luck is taken for a while. It carries a number of levels (1 to MAX_LEVEL), and every second
 * it rolls: the chance of a mishap is CHANCE_PER_LEVEL times its level. Works from any class/ability, on players and
 * mobs:
 *
 *   Misfortune.add(target, player, 3, 3, 400);          // 3 levels at once, 3 at most, all of them 20 seconds from now
 *   Misfortune.add(target, player, 1, 5, 400);          // one more level, up to 5 (a field that worsens it each beat)
 *   Misfortune.add(target, player, 2, 5, 400, true);    // Catastrophe: its worst mishap is a disaster
 *   Misfortune.level(entity)    Misfortune.ticksLeft(entity)    Misfortune.clear(target)
 *   Misfortune.sourceId(entity)                         // who laid it (the last to add to it), or null
 *   Misfortune.addImmunity(entity -> ...)               // some cannot be cursed (a Pathway's own authority)
 *   Misfortune.addHalfLevels(entity -> ...)             // some carry only half the levels (bosses)
 *   Misfortune.addListener((victim, mishap, source) -> ...)   // the look and sound of each mishap
 *
 * The mishaps (each needs its level; one is picked at random from those the level allows):
 *   STUMBLE        1  Slowness IV for STUMBLE_TICKS and a little trip
 *   FUMBLE         2  its next blow is FUMBLE_CUT weaker; equipment is untouched
 *   BACKFIRE       3  the next projectile fizzles harmlessly instead of hitting a bystander
 *   FALLING_STONE  4  magic damage, STONE_SHARE of its maximum health, from nobody (DamageFx counts it as the source's)
 *   CALAMITY       5  a disaster: CALAMITY_SHARE of its maximum health (CATASTROPHE_SHARE, and it is thrown up, with
 *                     Catastrophe)
 * The damage itself is here; how a mishap looks and sounds is up to a listener.
 *
 * Adding again never lowers the level and makes all of it last 'ticks' from now. Saved with the entity.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Misfortune {

    public enum Mishap {
        STUMBLE(1), FUMBLE(2), BACKFIRE(3), FALLING_STONE(4), CALAMITY(5),
        GRAVITY_CRUSH(6), WITHERING(6), LIGHTNING(7), METEOR_STORM(8), SEISMIC_BURST(9), DOOM(10);

        public final int fromLevel;

        Mishap(int fromLevel) {
            this.fromLevel = fromLevel;
        }
    }

    /** Runs for every mishap that befalls someone. 'source' = who cursed it (may be null: gone, or nobody). */
    @FunctionalInterface
    public interface Listener {
        void befell(LivingEntity victim, Mishap mishap, Player source);
    }

    public static int MAX_LEVEL = 10;
    /** Ticks between two rolls. */
    public static int ROLL_EVERY = 20;
    /** Chance of a mishap at each roll, per level (0.05 = 5% at level 1, 50% at level 10). */
    public static float CHANCE_PER_LEVEL = 0.05f;
    public static int STUMBLE_TICKS = 20;
    public static float FUMBLE_CUT = 0.3f;
    public static float BACKFIRE_DEGREES = 30f;
    public static float STONE_SHARE = 0.05f;
    public static float CALAMITY_SHARE = 0.15f;
    public static float CATASTROPHE_SHARE = 0.30f;

    private static final String TAG = "circle_of_inevitability_misfortune";
    private static final String LEVEL = "level";
    private static final String UNTIL = "until";              // game time
    private static final String SOURCE = "source";
    private static final String CATASTROPHE = "catastrophe";
    private static final String FUMBLED = "fumbled";          // the next blow is weaker
    private static final String BACKFIRES = "backfires";     // the next shot flies wide


    private static final List<Predicate<LivingEntity>> IMMUNE = new ArrayList<>();
    private static final List<Predicate<LivingEntity>> HALF = new ArrayList<>();
    private static final List<Listener> LISTENERS = new ArrayList<>();

    private static final String RECOVERY = "next_mishap";
    private static LivingEntity mishapDamage;
    public static boolean isMishapDamage(LivingEntity victim) { return mishapDamage == victim; }
    private Misfortune() {}

    /** Early Snake casts feed the shared curse without replacing other Pathways' status hooks. */
    public static void add(Player source, LivingEntity target, int levels) {
        if (net.lukario.circleofinevitability.combat.Spells.isFriend(source, target)) return;
        levels = net.lukario.circleofinevitability.fate.Fate.tier(source).fortuneLevels(levels, source.getRandom().nextFloat());
        if (levels <= 0 || add(target, source, levels, 10, 600) <= 0) return;
        CompoundTag tag = target.getPersistentData().getCompoundOrEmpty(TAG);
        tag.putLong("snake_decay", target.level().getGameTime() + 100);
        target.getPersistentData().put(TAG, tag);
    }

    // =========================
    // API
    // =========================

    /** The same from anyone: a Beyonder mob's curse has no player to credit (its source is left empty). */
    public static int add(LivingEntity target, LivingEntity source, int levels, int max, int ticks, boolean catastrophe) {
        return add(target, source instanceof Player player ? player : null, levels, max, ticks, catastrophe);
    }

    public static int add(LivingEntity target, LivingEntity source, int levels, int max, int ticks) {
        return add(target, source instanceof Player player ? player : null, levels, max, ticks, false);
    }

    /** 'levels' more (up to 'max'), all of them lasting 'ticks' from now. Returns the level it has now (0 = immune). */
    public static int add(LivingEntity target, Player source, int levels, int max, int ticks) {
        return add(target, source, levels, max, ticks, false);
    }

    /** The same; catastrophe = its CALAMITY is a Catastrophe (stays on until the Misfortune ends). */
    public static int add(LivingEntity target, Player source, int levels, int max, int ticks, boolean catastrophe) {
        if (target.level().isClientSide() || ticks <= 0 || levels <= 0 || !target.isAlive()) return 0;
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return 0;
        if (isImmune(target)) return 0;
        int cap = Math.max(1, Math.min(MAX_LEVEL, max));
        if (halved(target)) {
            cap = Math.max(1, cap / 2);
            levels = Math.max(1, levels / 2);
        }
        ticks = Math.max(1, Math.round(ticks * Fate.tier(source).duration));
        CompoundTag tag = target.getPersistentData().getCompoundOrEmpty(TAG);
        int had = level(target);
        int level = Math.max(had, Math.min(cap, had + levels));
        tag.putInt(LEVEL, level);
        tag.remove("explicit_source");
        tag.putLong(UNTIL, target.level().getGameTime() + ticks);
        if (source != null) tag.store(SOURCE, UUIDUtil.CODEC, source.getUUID());
        if (catastrophe) tag.putBoolean(CATASTROPHE, true);
        target.getPersistentData().put(TAG, tag);
        return level;
    }

    /** An explicitly chosen curse may affect an ally; ordinary automatic curse applications keep friendly protection. */
    public static int addDeliberate(LivingEntity target, Player source, int levels, int ticks) {
        int applied = add(target, source, levels, 10, ticks);
        if (applied > 0 && source != null) {
            CompoundTag tag = target.getPersistentData().getCompoundOrEmpty(TAG);
            tag.putString("explicit_source", source.getUUID().toString());
            target.getPersistentData().put(TAG, tag);
        }
        return applied;
    }

    /** 0 when it carries none. */
    public static int level(Entity entity) {
        if (entity == null || !entity.getPersistentData().contains(TAG)) return 0;
        CompoundTag tag = entity.getPersistentData().getCompoundOrEmpty(TAG);
        if (tag.getLongOr(UNTIL, 0L) <= entity.level().getGameTime()) return 0;
        return tag.getIntOr(LEVEL, 0);
    }

    public static int ticksLeft(Entity entity) {
        if (level(entity) <= 0) return 0;
        return (int) Math.max(0, entity.getPersistentData().getCompoundOrEmpty(TAG).getLongOr(UNTIL, 0L) - entity.level().getGameTime());
    }

    /** Who laid the Misfortune it carries now (the last to add to it), or null: none, or laid by nobody. */
    public static UUID sourceId(Entity entity) {
        if (level(entity) <= 0) return null;
        return entity.getPersistentData().getCompoundOrEmpty(TAG).read(SOURCE, UUIDUtil.CODEC).orElse(null);
    }

    public static boolean isCatastrophe(Entity entity) {
        return level(entity) > 0 && entity.getPersistentData().getCompoundOrEmpty(TAG).getBooleanOr(CATASTROPHE, false);
    }

    public static void clear(LivingEntity target) {
        target.getPersistentData().remove(TAG);
    }

    public static boolean isImmune(LivingEntity entity) {
        for (Predicate<LivingEntity> immune : IMMUNE) if (immune.test(entity)) return true;
        return false;
    }

    public static void addImmunity(Predicate<LivingEntity> immune) {
        IMMUNE.add(immune);
    }

    /** Those this says true for carry half the levels (at least one). */
    public static void addHalfLevels(Predicate<LivingEntity> half) {
        HALF.add(half);
    }

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    /** Forced spell attempt, still respecting immunity, player-safe effects and shared recovery. */
    public static boolean trigger(LivingEntity victim, boolean severe) {
        if (!(victim.level() instanceof ServerLevel sl) || !victim.isAlive() || isImmune(victim) || level(victim) <= 0
                || victim instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        CompoundTag tag = victim.getPersistentData().getCompoundOrEmpty(TAG);
        long now = sl.getGameTime();
        if (tag.getLongOr(RECOVERY, 0L) > now) return false;
        int level = level(victim);
        tag.putLong(RECOVERY, now + DisasterBalance.recovery(level, !(victim instanceof Player)));
        victim.getPersistentData().put(TAG, tag);
        Mishap chosen = roll(victim, level);
        if (severe) for (Mishap candidate : Mishap.values())
            if (candidate.fromLevel <= level && (!(victim instanceof Player) || candidate.ordinal() <= Mishap.CALAMITY.ordinal())) chosen = candidate;
        befall(victim, chosen);
        return true;
    }

    /** Makes a mishap befall it now (whatever its level), as if it had rolled it. */
    public static void befall(LivingEntity victim, Mishap mishap) {
        if (!(victim.level() instanceof ServerLevel sl)) return;
        Player source = sourceOf(victim, sl);
        CompoundTag tag = victim.getPersistentData().getCompoundOrEmpty(TAG);
        if (source != null && net.lukario.circleofinevitability.combat.Spells.isFriend(source, victim)
                && !source.getUUID().toString().equals(tag.getStringOr("explicit_source", ""))) return;
        WheelTier tier = Fate.tier(source);
        switch (mishap) {
            case STUMBLE -> {
                victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, STUMBLE_TICKS, 3, false, false));
                double a = victim.getRandom().nextDouble() * Math.PI * 2;
                victim.setDeltaMovement(victim.getDeltaMovement().add(Math.cos(a) * 0.25, 0.1, Math.sin(a) * 0.25));
                victim.hurtMarked = true;
            }
            case FUMBLE -> {
                // A fumble spoils the next strike; equipment is never destroyed by a random roll.
                tag.putBoolean(FUMBLED, true);
            }
            case BACKFIRE -> tag.putBoolean(BACKFIRES, true);
            case FALLING_STONE -> hurt(sl, victim, source, Math.min(6f, victim.getMaxHealth() * STONE_SHARE));
            case CALAMITY -> {
                boolean catastrophe = tag.getBooleanOr(CATASTROPHE, false);
                hurt(sl, victim, source, DisasterBalance.damage(victim.getMaxHealth(), level(victim), catastrophe, !(victim instanceof Player)));
                if (!(victim instanceof Player) && level(victim) >= 7) impactFx(sl, victim, 2 + level(victim) * .4);
                if (catastrophe && victim.isAlive()) {                // the ground gives way under it: a fall it did not see coming
                    victim.setDeltaMovement(victim.getDeltaMovement().x, 1.1, victim.getDeltaMovement().z);
                    victim.hurtMarked = true;
                    FallGuard.remove(victim);
                }
            }
            case GRAVITY_CRUSH -> {
                Root.apply(victim, Math.round((victim.getMaxHealth() > 150 ? 15 : 30) * tier.duration), RootRestriction.MOVE, RootRestriction.JUMP);
                victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Math.round(60 * tier.duration), 3));
                hurt(sl, victim, source, 8 + level(victim) * 2);
                impactFx(sl, victim, 3 * tier.aoe);
            }
            case WITHERING -> {
                if (source != null) DamageFx.credit(source, victim);
                victim.addEffect(new MobEffectInstance(MobEffects.WITHER, Math.round(100 * tier.duration), level(victim) >= 9 ? 2 : 1));
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, Math.round(100 * tier.duration), 1));
                sl.sendParticles(ParticleTypes.SMOKE, victim.getX(), victim.getY() + 1, victim.getZ(), 35, .7, .8, .7, .04);
            }
            case LIGHTNING -> {
                // Visual lightning: never lights fires, charges creepers or transforms villagers.
                for (int y = 0; y < 12; y++)
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY() + y, victim.getZ(), 5, .2, .4, .2, .1);
                Spells.sound(sl, victim.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2f, .8f);
                hurt(sl, victim, source, DisasterBalance.damage(victim.getMaxHealth(), level(victim), false, !(victim instanceof Player)));
                Root.apply(victim, Math.round(15 * tier.duration), RootRestriction.ATTACK);
            }
            case METEOR_STORM -> {
                for (int i = 0; i < 3; i++) {
                    Later.run(sl, i * 12, () -> {
                        if (!victim.isAlive() || victim.isRemoved() || victim.level() != sl || (source != null && Spells.isFriend(source, victim))) return;
                        impactFx(sl, victim, 3 * tier.aoe);
                        hurt(sl, victim, source, victim instanceof Player ? 4 : 12 + level(victim) * 2);
                    });
                }
            }
            case SEISMIC_BURST -> {
                impactFx(sl, victim, 6 * tier.aoe);
                hurt(sl, victim, source, DisasterBalance.damage(victim.getMaxHealth(), level(victim), false, !(victim instanceof Player)));
                if (source != null) for (LivingEntity enemy : Spells.enemiesAround(source, sl, victim.position(), 6 * tier.aoe)) {
                    if (enemy == victim || enemy instanceof Player) continue;
                    hurt(sl, enemy, source, 16 + level(victim));
                    enemy.setDeltaMovement(enemy.getDeltaMovement().add(0, .8, 0));
                    enemy.hurtMarked = true;
                }
            }
            case DOOM -> {
                impactFx(sl, victim, 7 * tier.aoe);
                hurt(sl, victim, source, DisasterBalance.damage(victim.getMaxHealth(), 10, true, !(victim instanceof Player)));
                victim.addEffect(new MobEffectInstance(MobEffects.WITHER, Math.round(120 * tier.duration), 2));
                Root.apply(victim, Math.round((victim.getMaxHealth() > 150 ? 10 : 30) * tier.duration), RootRestriction.ATTACK);
            }
        }
        if (!tag.isEmpty() && victim.isAlive()) victim.getPersistentData().put(TAG, tag);
        for (Listener listener : LISTENERS) {
            try {
                listener.befell(victim, mishap, source);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A misfortune listener failed", e);
            }
        }
    }

    // =========================
    // Internals
    // =========================

    private static void impactFx(ServerLevel sl, LivingEntity victim, double radius) {
        Vec3 at = victim.position().add(0, .5, 0);
        sl.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 6, radius * .3, .8, radius * .3, .03);
        sl.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 30, radius * .4, 1, radius * .4, .08);
        SpellFx.shockRing(sl, victim.position(), radius, 0xFFE75060, 12, 0);
        Spells.sound(sl, at, SoundEvents.GENERIC_EXPLODE.value(), 1.2f, .65f);
    }

    private static boolean halved(LivingEntity entity) {
        for (Predicate<LivingEntity> half : HALF) if (half.test(entity)) return true;
        return false;
    }

    private static Player sourceOf(LivingEntity victim, ServerLevel sl) {
        UUID id = victim.getPersistentData().getCompoundOrEmpty(TAG).read(SOURCE, UUIDUtil.CODEC).orElse(null);
        return id == null ? null : sl.getPlayerByUUID(id);
    }

    private static void hurt(ServerLevel sl, LivingEntity victim, Player source, float damage) {
        if (damage <= 0f) return;
        if (source != null) DamageFx.credit(source, victim);
        LivingEntity before = mishapDamage;
        mishapDamage = victim;
        try { victim.hurt(sl.damageSources().magic(), damage); }
        finally { mishapDamage = before; }
    }

    private static Mishap roll(LivingEntity victim, int level) {
        // At severe buildup, prefer disasters instead of mostly rolling mild stumbles.
        if (!(victim instanceof Player) && level >= 7 && victim.getRandom().nextInt(100) < 75) {
            List<Mishap> severe = new ArrayList<>();
            for (Mishap mishap : Mishap.values()) if (mishap.fromLevel >= 5 && mishap.fromLevel <= level) severe.add(mishap);
            return severe.get(victim.getRandom().nextInt(severe.size()));
        }
        List<Mishap> allowed = new ArrayList<>();
        for (Mishap mishap : Mishap.values()) if (mishap.fromLevel <= level && (!(victim instanceof Player) || mishap.ordinal() <= Mishap.CALAMITY.ordinal())) allowed.add(mishap);
        return allowed.get(victim.getRandom().nextInt(allowed.size()));
    }

    // =========================
    // Events
    // =========================

    @SubscribeEvent
    public static void onMisfortuneLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide() || entity.tickCount % 5 != 0 || !entity.getPersistentData().contains(TAG)) return;
        int level = level(entity);
        if (level <= 0) {
            clear(entity);
            return;
        }
        CompoundTag tag = entity.getPersistentData().getCompoundOrEmpty(TAG);
        long now = entity.level().getGameTime();
        long decay = tag.getLongOr("snake_decay", 0L);
        if (decay > 0 && now >= decay) {
            level--;
            if (level <= 0) { clear(entity); return; }
            tag.putInt(LEVEL, level);
            tag.putLong("snake_decay", now + 60);
            entity.getPersistentData().put(TAG, tag);
        }
        if (now < tag.getLongOr(RECOVERY, 0L)) return;
        tag.putLong(RECOVERY, now + Math.max(5, ROLL_EVERY));
        entity.getPersistentData().put(TAG, tag);
        if (entity.getRandom().nextFloat() < DisasterBalance.chance(level, !(entity instanceof Player))) {
            tag.putLong(RECOVERY, now + DisasterBalance.recovery(level, !(entity instanceof Player)));
            entity.getPersistentData().put(TAG, tag);
            befall(entity, roll(entity, level));
        }
    }

    // FUMBLE: its next blow is weaker
    @SubscribeEvent
    public static void onMisfortuneHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker) || attacker.level().isClientSide()) return;
        if (!attacker.getPersistentData().contains(TAG)) return;
        CompoundTag tag = attacker.getPersistentData().getCompoundOrEmpty(TAG);
        if (level(attacker) <= 0 || !tag.getBooleanOr(FUMBLED, false)) return;
        tag.remove(FUMBLED);
        attacker.getPersistentData().put(TAG, tag);
        event.setAmount(event.getAmount() * (1f - FUMBLE_CUT));
    }

    // BACKFIRE: its next shot fizzles
    @SubscribeEvent
    public static void onMisfortuneShot(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof Projectile projectile) || event.getLevel().isClientSide()) return;
        if (!(projectile.getOwner() instanceof LivingEntity shooter) || !shooter.getPersistentData().contains(TAG)) return;
        CompoundTag tag = shooter.getPersistentData().getCompoundOrEmpty(TAG);
        if (level(shooter) <= 0 || !tag.getBooleanOr(BACKFIRES, false)) return;
        tag.remove(BACKFIRES);
        shooter.getPersistentData().put(TAG, tag);
        // A misfire must not redirect arrows into teammates or ignite terrain.
        projectile.discard();
    }

    @SubscribeEvent
    public static void onMisfortuneDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) clear(event.getEntity());
    }
}
