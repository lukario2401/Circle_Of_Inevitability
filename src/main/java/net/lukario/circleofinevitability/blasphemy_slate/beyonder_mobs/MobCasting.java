package net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Casts;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * How a Beyonder mob casts (docs/BEYONDER_MOBS.md, "Mobs casting"). Every BeyonderMobRules.THINK_EVERY ticks it
 * gets spirituality back (MobSpirituality), and if it is in a fight (MobSpells.fightTarget) it may, at
 * CAST_CHANCE, cast one of its abilities (MobAbilities), picked by weight among those it can cast right now.
 *
 * It can cast an ability only if all of these hold (whyNot says which one does not):
 *   - it is a Beyonder mob of that ability's Pathway,
 *   - its Sequence is the ability's or stronger,
 *   - it has the ability's spirituality,
 *   - the ability's cooldown for this mob is over, and it has not just cast something (CAST_PAUSE),
 *   - nothing blocks it (it is not bound by Order: a root on ABILITIES, which a hold on everything also is; and
 *     every addBlocker rule),
 *   - its target is in range (and in sight if the ability needs that), and the ability's when() agrees.
 *
 * Then the ability's cast() runs. If it went off, the spirituality is paid, its cooldown starts, and the cast is
 * remembered like a player's (Casts.mobCast: the crystal ball shows it as its last power, Replication can copy it).
 *
 *   MobCasting.tryCast(mob, MobAbilities.byId("Hand Of Order/1"), target, sl);   // a cast now, with every check
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID)
public final class MobCasting {

    private static final String COOLDOWNS = "circle_of_inevitability_mob_cooldowns";
    private static final String PAUSE = "pause";                            // the CAST_PAUSE after any cast

    /** Why a mob cannot cast an ability now (NONE: it can, range and when() aside). */
    public enum Refusal {
        NONE("ready"),
        NOT_A_BEYONDER("no Beyonder"),
        WRONG_PATHWAY("another Pathway's"),
        SEQUENCE_TOO_WEAK("Sequence too weak"),
        NOT_ENOUGH_SPIRITUALITY("not enough spirituality"),
        COOLING_DOWN("cooling down"),
        BLOCKED("kept from casting");

        private final String words;

        Refusal(String words) {
            this.words = words;
        }

        /** "not enough spirituality", for /beyondermob info. */
        public String words() {
            return words;
        }
    }

    /** One more thing that keeps mobs from casting (a Decree that prohibits abilities, a sealed key...). */
    @FunctionalInterface
    public interface Blocker {
        boolean blocks(Mob mob, MobAbility ability);
    }

    private static final List<Blocker> BLOCKERS = new ArrayList<>();

    static {
        // bound by Order (status/Silence) or held (a root on everything): no abilities, as for a player
        addBlocker((mob, ability) -> Root.has(mob, RootRestriction.ABILITIES));
    }

    private MobCasting() {}

    /** Once, in a static block. */
    public static void addBlocker(Blocker blocker) {
        BLOCKERS.add(blocker);
    }

    // =========================
    // Asking
    // =========================

    /** Why this mob cannot cast it now, or NONE (its target, range and when() are not judged here). */
    public static Refusal whyNot(Mob mob, MobAbility ability) {
        if (!BeyonderMobs.isBeyonder(mob)) return Refusal.NOT_A_BEYONDER;
        if (!ability.pathway().equals(BeyonderMobs.pathway(mob))) return Refusal.WRONG_PATHWAY;
        if (BeyonderMobs.sequence(mob) > ability.sequence()) return Refusal.SEQUENCE_TOO_WEAK;
        if (!MobSpirituality.has(mob, ability.cost())) return Refusal.NOT_ENOUGH_SPIRITUALITY;
        if (cooldownLeft(mob, ability) > 0 || pauseLeft(mob) > 0) return Refusal.COOLING_DOWN;
        for (Blocker blocker : BLOCKERS) {
            if (blocker.blocks(mob, ability)) return Refusal.BLOCKED;
        }
        return Refusal.NONE;
    }

    public static boolean canCast(Mob mob, MobAbility ability) {
        return whyNot(mob, ability) == Refusal.NONE;
    }

    /** True if 'target' is where the ability can reach it from the mob (any target, or none, for anyRange). */
    public static boolean inReach(Mob mob, MobAbility ability, LivingEntity target) {
        if (ability.range() <= 0) return true;
        if (target == null || target.level() != mob.level()) return false;
        if (mob.distanceTo(target) > ability.range()) return false;
        return !ability.needsSight() || mob.hasLineOfSight(target);
    }

    /** Ticks before this mob can cast it again (0 = now). */
    public static int cooldownLeft(LivingEntity mob, MobAbility ability) {
        return left(mob, ability.id());
    }

    /** Ticks of the pause after its last cast (0 = none). */
    public static int pauseLeft(LivingEntity mob) {
        return left(mob, PAUSE);
    }

    // =========================
    // Casting
    // =========================

    /**
     * Casts it now at 'target' if every check passes (see the class comment). True if it went off. An ability that
     * throws is logged, and waits out its cooldown so it does not fail again at once.
     */
    public static boolean tryCast(Mob mob, MobAbility ability, LivingEntity target, ServerLevel sl) {
        if (mob == null || ability == null || sl == null || mob.level() != sl) return false;
        if (whyNot(mob, ability) != Refusal.NONE || !inReach(mob, ability, target)) return false;
        if (ability.when() != null && !ability.when().test(mob, target, sl)) return false;
        boolean wentOff;
        try {
            wentOff = ability.cast().run(mob, target, sl);
        } catch (RuntimeException e) {
            CircleOfInevitability.LOGGER.error("A mob ability failed: {} cast by {}", ability, mob, e);
            startCooldown(mob, sl, ability.id(), ability.cooldown());
            return false;
        }
        if (!wentOff) return false;                                         // refused: nothing spent
        MobSpirituality.spend(mob, ability.cost());
        startCooldown(mob, sl, ability.id(), Math.round(ability.cooldown() * Math.max(0f, BeyonderMobRules.COOLDOWN_SCALE)));
        startCooldown(mob, sl, PAUSE, BeyonderMobRules.CAST_PAUSE);
        if (ability.ability() != null) Casts.mobCast(mob, sl, Casts.madeUpFor(mob, sl, ability.ability(), ability.sneak()));
        return true;
    }

    /**
     * For testing (/beyondermob cast): casts it now at 'target' whatever its spirituality, cooldowns, range, when()
     * and the CAST_PAUSE say. Its Pathway and Sequence must still allow it. Nothing is spent; the cast is remembered.
     * The Refusal that stopped it, or NONE if it went off (null: it ran but refused, or failed).
     */
    public static Refusal forceCast(Mob mob, MobAbility ability, LivingEntity target, ServerLevel sl) {
        if (!BeyonderMobs.isBeyonder(mob)) return Refusal.NOT_A_BEYONDER;
        if (!ability.pathway().equals(BeyonderMobs.pathway(mob))) return Refusal.WRONG_PATHWAY;
        if (BeyonderMobs.sequence(mob) > ability.sequence()) return Refusal.SEQUENCE_TOO_WEAK;
        if (ability.range() > 0 && (target == null || target.level() != sl)) return null;     // it needs something to aim at
        boolean wentOff;
        try {
            wentOff = ability.cast().run(mob, target, sl);
        } catch (RuntimeException e) {
            CircleOfInevitability.LOGGER.error("A mob ability failed: {} cast by {}", ability, mob, e);
            return null;
        }
        if (!wentOff) return null;
        if (ability.ability() != null) Casts.mobCast(mob, sl, Casts.madeUpFor(mob, sl, ability.ability(), ability.sneak()));
        return Refusal.NONE;
    }

    /** It forgets every cooldown (it is no Beyonder any more, or a command). */
    public static void clearCooldowns(LivingEntity mob) {
        mob.getPersistentData().remove(COOLDOWNS);
    }

    // =========================
    // The brain
    // =========================

    @SubscribeEvent
    public static void onMobCastingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Mob mob) || !(entity.level() instanceof ServerLevel sl)) return;
        int every = Math.max(1, BeyonderMobRules.THINK_EVERY);
        if ((mob.tickCount + mob.getId()) % every != 0 || !BeyonderMobs.isBeyonder(mob)) return;
        MobSpirituality.regen(mob, every);
        if (BeyonderMobRules.MOBS_CAST && !mob.isNoAi()) think(mob, sl);
    }

    /** In a fight, maybe one cast: a weighted pick among what it can cast at its target right now. */
    private static void think(Mob mob, ServerLevel sl) {
        LivingEntity target = MobSpells.fightTarget(mob);
        if (target == null || pauseLeft(mob) > 0) return;
        if (mob.getRandom().nextDouble() >= BeyonderMobRules.CAST_CHANCE) return;
        List<MobAbility> choices = new ArrayList<>();
        int total = 0;
        for (MobAbility ability : MobAbilities.knownBy(mob)) {
            if (ability.weight() <= 0 || !canCast(mob, ability) || !inReach(mob, ability, target)) continue;
            if (ability.when() != null && !ability.when().test(mob, target, sl)) continue;
            choices.add(ability);
            total += ability.weight();
        }
        if (choices.isEmpty() || total <= 0) return;
        int roll = mob.getRandom().nextInt(total);
        for (MobAbility ability : choices) {
            roll -= ability.weight();
            if (roll < 0) {
                tryCast(mob, ability, target, sl);
                return;
            }
        }
    }

    // =========================
    // Internals
    // =========================

    private static int left(LivingEntity mob, String key) {
        CompoundTag tag = mob.getPersistentData().getCompoundOrEmpty(COOLDOWNS);
        long readyAt = tag.getLongOr(key, 0L);
        return (int) Math.max(0L, readyAt - mob.level().getGameTime());
    }

    private static void startCooldown(LivingEntity mob, ServerLevel sl, String key, int ticks) {
        if (ticks <= 0) return;
        CompoundTag tag = mob.getPersistentData().getCompoundOrEmpty(COOLDOWNS);
        tag.putLong(key, sl.getGameTime() + ticks);
        mob.getPersistentData().put(COOLDOWNS, tag);
    }
}
