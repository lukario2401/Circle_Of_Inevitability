package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Appraisal: one caster has read an entity for a while. It knows what the entity is weak to, and the caster's
 * ability hits on it land harder: by the mark's own bonus, and by MATCH_BONUS more when the hit is of the kind it is
 * weak to. Every caster has marks of their own; two casters never share theirs. The Illuminator is built on it
 * (docs/ILLUMINATOR.md, "The pathway mechanic"), and it fits any "I have studied you, now I know where to strike".
 *
 *   Appraisal.mark(player, target, "corrosion", 240, 0.15f);    // 12 seconds: weak to corrosion, +15% from my abilities
 *   Appraisal.strike(player, target, 36f, "corrosion");          // Spells.strike counted as an ability hit of that kind:
 *                                                               // +15%, and +50% more because it is what it is weak to
 *   Appraisal.as("pierce", () -> Spells.strikeTrue(player, target, 90f));   // any damage code, counted the same way
 *   Appraisal.weakness(player, target)       // "corrosion", or null when this caster has no mark on it
 *   Appraisal.spend(player, target)          // takes the mark off (a finisher used it up); false if there was none
 *   Appraisal.markedBy(player)               // everything this caster has marked right now
 *   Appraisal.wipe(target)                   // every caster's mark off it (an information shredder)
 *
 * Only hits made inside strike / as count as ability hits: a plain melee swing gets nothing, and neither does damage
 * that comes later on its own (bleeding, corrosion). Put the kind of the ability on the hit; null is "an ability of
 * no kind", which gets the bonus but never the match.
 *
 * What something is weak to is not decided here: a Pathway keeps a WeaknessTable of its own and passes the answer
 * to mark(...). The look is a listener's (Appraisal shows nothing): addListener(...).
 *
 * Not saved: marks last seconds, and a restart, the caster logging out or the target dying ends them.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Appraisal {

    /** How much more a hit of the kind the target is weak to does, on top of the mark's own bonus (0.5 = 50%). */
    public static float MATCH_BONUS = 0.5f;
    /** Ticks between two looks for marks that ran out. */
    private static final int SWEEP_EVERY = 5;

    /** Why a mark ended. */
    public enum End { TIME, SPENT, WIPED, DIED, CASTER_GONE }

    /** The look of every mark: Appraisal shows nothing itself. Every method is optional. */
    public interface Listener {
        /** A mark was put on (also when one is put on again over the old one). */
        default void marked(Player caster, LivingEntity target, String weakness, int ticks) {}

        /** A mark came off. The caster may have left already: it is given by id. */
        default void ended(UUID caster, LivingEntity target, End why) {}
    }

    /** One caster's mark on one entity. */
    private record Mark(LivingEntity target, String weakness, long until, float bonus) {}

    /** target -> caster -> its mark. */
    private static final Map<UUID, Map<UUID, Mark>> MARKS = new HashMap<>();
    private static final List<Listener> LISTENERS = new ArrayList<>();
    private static final List<Predicate<LivingEntity>> IMMUNE = new ArrayList<>();

    /** The kind of ability hit landing right now (only while strike / as runs), and how deep they are nested. */
    private static String hitKind = null;
    private static int abilityDepth = 0;

    private Appraisal() {}

    // =========================
    // API: marking
    // =========================

    /**
     * 'caster' has appraised 'target' for 'ticks': it is weak to 'weakness' (any word the Pathway uses; may be null),
     * and the caster's ability hits on it do 'bonus' more (0.15f = 15%). Replaces this caster's old mark on it. False when
     * it cannot be marked (immune, dead).
     */
    public static boolean mark(Player caster, LivingEntity target, String weakness, int ticks, float bonus) {
        if (target.level().isClientSide() || ticks <= 0 || !target.isAlive()) return false;
        for (Predicate<LivingEntity> immune : IMMUNE) if (immune.test(target)) return false;
        Mark mark = new Mark(target, weakness, target.level().getGameTime() + ticks, Math.max(0f, bonus));
        MARKS.computeIfAbsent(target.getUUID(), id -> new HashMap<>()).put(caster.getUUID(), mark);
        for (Listener listener : LISTENERS) {
            try {
                listener.marked(caster, target, weakness, ticks);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("An appraisal listener failed", e);
            }
        }
        return true;
    }

    /** Nobody can mark what this says true for (a mind no reading takes on). Once, in a static block. */
    public static void addImmunity(Predicate<LivingEntity> immune) {
        IMMUNE.add(immune);
    }

    /** True while 'caster' has a mark on 'target'. */
    public static boolean isMarked(Player caster, Entity target) {
        return mark(caster.getUUID(), target) != null;
    }

    /** True while anyone has a mark on it. */
    public static boolean isMarked(Entity target) {
        Map<UUID, Mark> on = target == null ? null : MARKS.get(target.getUUID());
        if (on == null) return false;
        long now = target.level().getGameTime();
        for (Mark mark : on.values()) if (mark.until() > now) return true;
        return false;
    }

    /** What 'caster' knows 'target' to be weak to, or null when they have no mark on it (or it named none). */
    public static String weakness(Player caster, Entity target) {
        Mark mark = mark(caster.getUUID(), target);
        return mark == null ? null : mark.weakness();
    }

    /** Ticks left on this caster's mark (0 = none). */
    public static int ticksLeft(Player caster, Entity target) {
        Mark mark = mark(caster.getUUID(), target);
        return mark == null ? 0 : (int) Math.max(0, mark.until() - target.level().getGameTime());
    }

    /** The mark's own bonus (0 = no mark). */
    public static float bonus(Player caster, Entity target) {
        Mark mark = mark(caster.getUUID(), target);
        return mark == null ? 0f : mark.bonus();
    }

    /**
     * What a hit by 'caster' of 'kind' on 'target' is multiplied by: 1 with no mark; 1 + bonus with one; and
     * MATCH_BONUS more when 'kind' is what it is weak to.
     */
    public static float multiplier(Player caster, Entity target, String kind) {
        Mark mark = mark(caster.getUUID(), target);
        if (mark == null) return 1f;
        boolean match = kind != null && kind.equals(mark.weakness());
        return 1f + mark.bonus() + (match ? MATCH_BONUS : 0f);
    }

    /** Takes this caster's mark off (a finisher used it up). False if there was none. */
    public static boolean spend(Player caster, LivingEntity target) {
        return end(caster.getUUID(), target, End.SPENT);
    }

    /** Takes every caster's mark off it. How many there were. */
    public static int wipe(LivingEntity target) {
        Map<UUID, Mark> on = MARKS.get(target.getUUID());
        if (on == null) return 0;
        List<UUID> casters = new ArrayList<>(on.keySet());
        int wiped = 0;
        for (UUID caster : casters) if (end(caster, target, End.WIPED)) wiped++;
        return wiped;
    }

    /** Everything 'caster' has a mark on right now, alive and in their level, nearest first. */
    public static List<LivingEntity> markedBy(Player caster) {
        List<LivingEntity> found = new ArrayList<>();
        long now = caster.level().getGameTime();
        for (Map<UUID, Mark> on : MARKS.values()) {
            Mark mark = on.get(caster.getUUID());
            if (mark == null || mark.until() <= now) continue;
            LivingEntity target = mark.target();
            if (target.isAlive() && !target.isRemoved() && target.level() == caster.level()) found.add(target);
        }
        found.sort((a, b) -> Double.compare(a.distanceToSqr(caster), b.distanceToSqr(caster)));
        return found;
    }

    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    // =========================
    // API: hitting
    // =========================

    /** Spells.strike, counted as an ability hit of 'kind' (null: of no kind): the mark's bonus and the match apply. */
    public static void strike(Player caster, LivingEntity target, float damage, String kind) {
        as(kind, () -> Spells.strike(caster, target, damage));
    }

    /** Spells.strikeTrue (nothing softens it), counted as an ability hit of 'kind'. */
    public static void strikeTrue(Player caster, LivingEntity target, float damage, String kind) {
        as(kind, () -> Spells.strikeTrue(caster, target, damage));
    }

    /**
     * Runs any damage code as an ability hit of 'kind': every hit it lands on something the hitting player has
     * marked gets the bonus (and the match). For a Shot's onHit, a blast, a strikeThrough.
     */
    public static void as(String kind, Runnable hits) {
        String before = hitKind;
        hitKind = kind;
        abilityDepth++;
        try {
            hits.run();
        } finally {
            abilityDepth--;
            hitKind = before;
        }
    }

    /** True while an ability hit made through strike / as is landing (for a status of your own that cares). */
    public static boolean isAbilityHit() {
        return abilityDepth > 0;
    }

    /** The kind of the ability hit landing right now, or null. */
    public static String hitKind() {
        return abilityDepth > 0 ? hitKind : null;
    }

    // =========================
    // Internals
    // =========================

    private static Mark mark(UUID caster, Entity target) {
        if (target == null) return null;
        Map<UUID, Mark> on = MARKS.get(target.getUUID());
        Mark mark = on == null ? null : on.get(caster);
        if (mark == null || mark.until() <= target.level().getGameTime()) return null;
        return mark;
    }

    private static boolean end(UUID caster, LivingEntity target, End why) {
        Map<UUID, Mark> on = MARKS.get(target.getUUID());
        Mark mark = on == null ? null : on.remove(caster);
        if (on != null && on.isEmpty()) MARKS.remove(target.getUUID());
        if (mark == null) return false;
        told(caster, target, why);
        return true;
    }

    private static void told(UUID caster, LivingEntity target, End why) {
        for (Listener listener : LISTENERS) {
            try {
                listener.ended(caster, target, why);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("An appraisal listener failed", e);
            }
        }
    }

    // =========================
    // Events
    // =========================

    /** The bonus: a caster's ability hits on what they marked. */
    @SubscribeEvent
    public static void onAppraisalHurt(LivingHurtEvent event) {
        if (abilityDepth <= 0 || MARKS.isEmpty()) return;
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof Player caster) || caster == target) return;
        float multiplier = multiplier(caster, target, hitKind);
        if (multiplier != 1f) event.setAmount(event.getAmount() * multiplier);
    }

    /** Marks whose time is up come off (and the listeners hear of it). */
    @SubscribeEvent
    public static void onAppraisalLevelTick(TickEvent.LevelTickEvent.Post event) {
        if (MARKS.isEmpty() || !(event.level() instanceof ServerLevel sl) || sl.getGameTime() % SWEEP_EVERY != 0) return;
        long now = sl.getGameTime();
        List<Object[]> over = new ArrayList<>();
        for (Map<UUID, Mark> on : MARKS.values()) {
            for (Map.Entry<UUID, Mark> entry : on.entrySet()) {
                Mark mark = entry.getValue();
                if (mark.target().level() == sl && mark.until() <= now) over.add(new Object[]{entry.getKey(), mark.target()});
            }
        }
        for (Object[] one : over) end((UUID) one[0], (LivingEntity) one[1], End.TIME);
    }

    @SubscribeEvent
    public static void onAppraisalDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead.level().isClientSide() || !MARKS.containsKey(dead.getUUID())) return;
        for (UUID caster : new ArrayList<>(MARKS.get(dead.getUUID()).keySet())) end(caster, dead, End.DIED);
    }

    @SubscribeEvent
    public static void onAppraisalLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID gone = event.getEntity().getUUID();
        List<LivingEntity> targets = new ArrayList<>();
        for (Map<UUID, Mark> on : MARKS.values()) {
            Mark mark = on.get(gone);
            if (mark != null) targets.add(mark.target());
        }
        for (LivingEntity target : targets) end(gone, target, End.CASTER_GONE);
        MARKS.remove(gone);                                             // marks on the one who left
    }

    @SubscribeEvent
    public static void onAppraisalLevelUnload(LevelEvent.Unload event) {
        Iterator<Map.Entry<UUID, Map<UUID, Mark>>> iterator = MARKS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map<UUID, Mark> on = iterator.next().getValue();
            on.values().removeIf(mark -> mark.target().level() == event.getLevel());
            if (on.isEmpty()) iterator.remove();
        }
    }
}
