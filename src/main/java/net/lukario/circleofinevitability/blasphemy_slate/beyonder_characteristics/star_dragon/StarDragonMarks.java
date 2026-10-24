package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Physique;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragonFx.Status;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Bleeding;
import net.lukario.circleofinevitability.status.Cleansing;
import net.lukario.circleofinevitability.status.Corrosion;
import net.lukario.circleofinevitability.status.Enfeebled;
import net.lukario.circleofinevitability.status.Exposed;
import net.lukario.circleofinevitability.status.Falter;
import net.lukario.circleofinevitability.status.NoHeal;
import net.lukario.circleofinevitability.status.Vulnerable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.UUID;

/**
 * The marks the Star Dragon leaves on others (docs/STAR_DRAGON.md, "New reusable parts", 4), in the style of
 * PrinceOfAbolitionMarks: put, has, from, ticksLeft, end. Each one knows who laid it, and does what it does by itself
 * (its effects, its once-a-second part, its look and, on a player, its picture above the hotbar):
 *
 *   StarDragonMarks.put(target, Mark.WEAKENED, player, 120);   // laid by 'player' for 6 seconds
 *   StarDragonMarks.has(target, Mark.CHARTED)   StarDragonMarks.from(target, Mark.CHARTED)   // who laid it, or null
 *   StarDragonMarks.stacks(target, Mark.WEAKENED)                // the Weakened marks it carries (1..WEAKENED_MOST)
 *   StarDragonMarks.end(target, Mark.VOIDED)
 *
 *   CHARTED      read by a Star Chart: it cannot hide (status/Exposed), its constellation is drawn for the one who
 *                charted it, and the first ability of theirs that lands on it does CHARTED_BONUS more (spendCharted)
 *   WEAKENED     a Weakening Ray: it bleeds a little each second and cannot heal until a cleanse; stacks (Series of
 *                Rays melts it at MELT_AT)
 *   MELTING      its flesh melts (the Tidal Scholar's Weakening Ray): half its armor gone, MELTING_EXTRA more damage taken
 *   DIMMED       a Piercing Ray: it deals DIMMED_CUT less (status/Enfeebled) and a player's spirituality drains
 *   MISFORTUNE   Fate Convergence: its attacks misfire (status/Falter), it stumbles every STUMBLE_EVERY, and under
 *                NARROW_BELOW of its health its future is narrowed: NARROWED_EXTRA more damage taken
 *   VOIDED       inside a Cosmic Void: what its caster's abilities do to it is read in StarDragon.hit
 *   IRRADIATED   radiation (the frozen void now, Fission Pulse later): bleeding that nothing heals, and Weakness
 *
 * Everything but CHARTED and VOIDED is washed off by a cleanse (status/Cleansing). Saved with the entity. A new mark is
 * one line in Mark, a case in put and, if it acts every second, a line in second.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class StarDragonMarks {

    private StarDragonMarks() {}

    public enum Mark { CHARTED, WEAKENED, MELTING, DIMMED, MISFORTUNE, VOIDED, IRRADIATED }

    // =========================
    // Settings
    // =========================

    static final float CHARTED_BONUS = 0.2f;                // the calculated shot
    static final int WEAKENED_MOST = 6;                     // stacks
    static final float WEAKENED_PER_SECOND = 2f;            // what each second takes (per stack, a little more)
    static final float WEAKENED_PER_STACK = 1f;
    static final int MELT_AT = 3;                           // Series of Rays: this many stacks and it melts
    static final float MELTING_EXTRA = 0.2f;                // melting: this much more damage taken...
    static final double MELTING_ARMOR = -0.5;               // ...and this share of its armor gone
    static final float DIMMED_CUT = 0.25f;                  // dimmed: this much less dealt...
    static final float DIMMED_DRAIN = 0.02f;                // ...and a player loses this share of their spirituality a second
    static final float MISFIRE = 0.2f;                      // misfortune: this share of its attacks go wide...
    static final int STUMBLE_EVERY = 60;                    // ...it stumbles this often...
    static final int STUMBLE_TICKS = 10;                    // ...for this long...
    static final float NARROW_BELOW = 0.25f;                // ...and under this share of its health...
    static final float NARROWED_EXTRA = 0.5f;               // ...it takes this much more
    static final float IRRADIATED_PER_SECOND = 4f;          // radiation

    private static final String TAG = "star_dragon_marks";                // {MARK: {By, Until, Stacks}}
    private static final String BY = "By";
    private static final String UNTIL = "Until";
    private static final String STACKS = "Stacks";
    private static final String CHART_SPENT = "Spent";
    private static final Identifier MELT_ARMOR = Physique.id("sd_melting_armor");
    private static final String FALTER = "sd_misfortune";
    private static final String DIM = "sd_dimmed";

    static {
        // a cleanse washes off everything but a chart and a void (status/Cleansing)
        Cleansing.addListener(StarDragonMarks::cleanse);
    }

    // =========================
    // API
    // =========================

    /** Lays 'mark' on 'target', by 'caster', for 'ticks' (never shorter than what is there). */
    public static void put(LivingEntity target, Mark mark, Player caster, int ticks) {
        if (ticks <= 0 || !target.isAlive() || !(target.level() instanceof ServerLevel sl)) return;
        CompoundTag all = target.getPersistentData().getCompoundOrEmpty(TAG);
        CompoundTag one = all.getCompoundOrEmpty(mark.name());
        long now = sl.getGameTime();
        boolean running = one.getLongOr(UNTIL, 0L) > now;
        long until = Math.max(now + ticks, running ? one.getLongOr(UNTIL, 0L) : 0L);
        int stacks = running ? one.getIntOr(STACKS, 1) : 0;
        one.putString(BY, caster.getUUID().toString());
        one.putLong(UNTIL, until);
        one.putInt(STACKS, mark == Mark.WEAKENED ? Math.min(WEAKENED_MOST, stacks + 1) : 1);
        if (!running) one.remove(CHART_SPENT);
        all.put(mark.name(), one);
        target.getPersistentData().put(TAG, all);
        int left = (int) (until - now);
        laid(sl, target, mark, caster, left, one.getIntOr(STACKS, 1));
    }

    public static boolean has(Entity entity, Mark mark) {
        return ticksLeft(entity, mark) > 0;
    }

    /** Who laid it, or null. */
    public static UUID from(Entity entity, Mark mark) {
        if (!has(entity, mark)) return null;
        String by = entity.getPersistentData().getCompoundOrEmpty(TAG).getCompoundOrEmpty(mark.name()).getStringOr(BY, "");
        try {
            return by.isEmpty() ? null : UUID.fromString(by);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** True if 'caster' laid it. */
    public static boolean fromCaster(Entity entity, Mark mark, Player caster) {
        return caster.getUUID().equals(from(entity, mark));
    }

    public static int ticksLeft(Entity entity, Mark mark) {
        if (entity == null || !entity.getPersistentData().contains(TAG)) return 0;
        long until = entity.getPersistentData().getCompoundOrEmpty(TAG).getCompoundOrEmpty(mark.name()).getLongOr(UNTIL, 0L);
        return (int) Math.max(0, until - entity.level().getGameTime());
    }

    /** How many it carries (Weakened stacks; 1 for the others while they last, 0 when it has none). */
    public static int stacks(Entity entity, Mark mark) {
        if (!has(entity, mark)) return 0;
        return entity.getPersistentData().getCompoundOrEmpty(TAG).getCompoundOrEmpty(mark.name()).getIntOr(STACKS, 1);
    }

    public static void end(LivingEntity target, Mark mark) {
        if (!target.getPersistentData().contains(TAG)) return;
        CompoundTag all = target.getPersistentData().getCompoundOrEmpty(TAG);
        all.remove(mark.name());
        if (all.isEmpty()) target.getPersistentData().remove(TAG);
        else target.getPersistentData().put(TAG, all);
        ended(target, mark);
    }

    /**
     * The calculated shot: true once per Star Chart, the first time an ability of the one who charted it lands on it
     * (StarDragon.hit asks it and adds CHARTED_BONUS).
     */
    static boolean spendCharted(LivingEntity target, Player caster) {
        if (!fromCaster(target, Mark.CHARTED, caster)) return false;
        CompoundTag all = target.getPersistentData().getCompoundOrEmpty(TAG);
        CompoundTag one = all.getCompoundOrEmpty(Mark.CHARTED.name());
        if (one.getBooleanOr(CHART_SPENT, false)) return false;
        one.putBoolean(CHART_SPENT, true);
        all.put(Mark.CHARTED.name(), one);
        target.getPersistentData().put(TAG, all);
        if (target.level() instanceof ServerLevel sl) StarDragonFx.calculatedHit(sl, target);
        return true;
    }

    // =========================
    // What each mark does when it is laid
    // =========================

    private static void laid(ServerLevel sl, LivingEntity target, Mark mark, Player caster, int ticks, int stacks) {
        String tag = tag(target, mark);
        Status picture = null;
        switch (mark) {
            case CHARTED -> {
                Exposed.apply(target, ticks);
                picture = Status.CHARTED;
            }
            case WEAKENED -> {
                float bite = WEAKENED_PER_SECOND + WEAKENED_PER_STACK * (stacks - 1);
                Corrosion.apply(target, caster, Math.max(bite, Corrosion.perSecond(target)), 0f, ticks);
                NoHeal.apply(target, ticks);
                StarDragonFx.weakened(sl, target, stacks, ticks, tag);
                picture = Status.WEAKENED;
            }
            case MELTING -> {
                Vulnerable.add(target, MELTING_EXTRA, MELTING_EXTRA, ticks);
                Physique.keep(target, Attributes.ARMOR, MELT_ARMOR, MELTING_ARMOR, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, true);
                StarDragonFx.melting(sl, target, ticks, tag);
                picture = Status.MELTING;
            }
            case DIMMED -> {
                Enfeebled.apply(target, DIM, DIMMED_CUT, ticks);
                StarDragonFx.dimmed(sl, target, ticks, tag);
                picture = Status.DIMMED;
            }
            case MISFORTUNE -> {
                Falter.apply(target, FALTER, MISFIRE, ticks);
                StarDragonFx.misfortune(sl, target, ticks, tag);
                picture = Status.MISFORTUNE;
            }
            case VOIDED -> picture = Status.VOIDED;
            case IRRADIATED -> {
                Bleeding.apply(target, caster, ticks, IRRADIATED_PER_SECOND);
                NoHeal.apply(target, ticks);
                Spells.keepEffect(target, MobEffects.WEAKNESS, 0, ticks);
                StarDragonFx.irradiated(sl, target, ticks, tag);
                picture = Status.IRRADIATED;
            }
        }
        if (picture != null && target instanceof Player shown) StarDragonFx.status(shown, picture, ticks);
    }

    /** It ended early (cleansed, end(...), death): what it put on comes off. */
    private static void ended(LivingEntity target, Mark mark) {
        String tag = tag(target, mark);
        ServerLevel sl = target.level() instanceof ServerLevel level ? level : null;
        Status picture = null;
        switch (mark) {
            case CHARTED -> {
                Exposed.end(target);
                picture = Status.CHARTED;
            }
            case WEAKENED -> {
                NoHeal.remove(target);
                if (sl != null) StarDragonFx.weakenedEnd(sl, target, tag);
                picture = Status.WEAKENED;
            }
            case MELTING -> {
                Physique.remove(target, Attributes.ARMOR, MELT_ARMOR);
                if (sl != null) StarDragonFx.meltingEnd(sl, target, tag);
                picture = Status.MELTING;
            }
            case DIMMED -> {
                Enfeebled.remove(target, DIM);
                if (sl != null) StarDragonFx.dimmedEnd(sl, target, tag);
                picture = Status.DIMMED;
            }
            case MISFORTUNE -> {
                Falter.remove(target, FALTER);
                if (sl != null) {
                    StarDragonFx.misfortune(sl, target, 0, tag);
                    StarDragonFx.narrowed(sl, target, 0, tag + "_narrowed");
                }
                picture = Status.MISFORTUNE;
                if (target instanceof Player shown) StarDragonFx.statusEnd(shown, Status.NARROWED);
            }
            case VOIDED -> picture = Status.VOIDED;
            case IRRADIATED -> {
                Bleeding.remove(target);
                NoHeal.remove(target);
                if (sl != null) StarDragonFx.irradiatedEnd(sl, target, tag);
                picture = Status.IRRADIATED;
            }
        }
        if (picture != null && target instanceof Player shown) StarDragonFx.statusEnd(shown, picture);
    }

    /** A cleanse: everything but a chart and a void. */
    private static void cleanse(LivingEntity target) {
        for (Mark mark : Mark.values()) {
            if (mark == Mark.CHARTED || mark == Mark.VOIDED) continue;
            if (has(target, mark)) end(target, mark);
        }
    }

    /** The look's tag for one mark on one entity. */
    static String tag(LivingEntity target, Mark mark) {
        return "sd_mark_" + mark.name().toLowerCase() + "_" + target.getId();
    }

    // =========================
    // Every tick: the marks that act over time, and those that ran out
    // =========================

    @SubscribeEvent
    public static void onStarDragonMarksTick(LivingEvent.LivingTickEvent event) {
        LivingEntity target = event.getEntity();
        if (!target.getPersistentData().contains(TAG) || !(target.level() instanceof ServerLevel sl)) return;
        CompoundTag all = target.getPersistentData().getCompoundOrEmpty(TAG);
        long now = sl.getGameTime();
        for (String key : new ArrayList<>(all.keySet())) {
            Mark mark;
            try {
                mark = Mark.valueOf(key);
            } catch (IllegalArgumentException e) {
                all.remove(key);
                continue;
            }
            CompoundTag one = all.getCompoundOrEmpty(key);
            if (one.getLongOr(UNTIL, 0L) <= now) {
                all.remove(key);
                if (all.isEmpty()) target.getPersistentData().remove(TAG);
                else target.getPersistentData().put(TAG, all);
                ended(target, mark);
                continue;
            }
            second(sl, target, mark, one);
        }
    }

    /** Every tick for each mark it carries (most of them act once a second). */
    private static void second(ServerLevel sl, LivingEntity target, Mark mark, CompoundTag one) {
        switch (mark) {
            case CHARTED -> {
                if (target.tickCount % 10 != 0) return;
                UUID by = from(target, mark);
                if (by != null && sl.getPlayerByUUID(by) instanceof ServerPlayer viewer) StarDragonFx.charted(viewer, target);
            }
            case MELTING -> {
                if (target.tickCount % 20 == 0) {
                    Physique.keep(target, Attributes.ARMOR, MELT_ARMOR, MELTING_ARMOR, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, true);
                }
            }
            case DIMMED -> {
                if (target.tickCount % 20 != 0 || !(target instanceof Player dimmed)) return;
                float now = SoulCore.getSpirituality(dimmed);
                SoulCore.setSpiritualityQuietly(dimmed, Math.max(0f, now - now * DIMMED_DRAIN));
            }
            case MISFORTUNE -> {
                if (target.tickCount % STUMBLE_EVERY == 0) {
                    Root.apply(target, Spells.holdLength(target, STUMBLE_TICKS), RootRestriction.MOVE);
                    StarDragonFx.stumble(sl, target);
                }
                if (target.tickCount % 20 == 0 && target.getHealth() < target.getMaxHealth() * NARROW_BELOW) {
                    Vulnerable.add(target, NARROWED_EXTRA, NARROWED_EXTRA, 30);
                    StarDragonFx.narrowed(sl, target, 30, tag(target, mark) + "_narrowed");
                    if (target instanceof Player shown) StarDragonFx.status(shown, Status.NARROWED, 30);
                }
            }
            default -> { }
        }
    }

    @SubscribeEvent
    public static void onStarDragonMarksDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide() || !dead.getPersistentData().contains(TAG)) return;
        for (Mark mark : Mark.values()) if (has(dead, mark)) end(dead, mark);
        dead.getPersistentData().remove(TAG);
    }
}
