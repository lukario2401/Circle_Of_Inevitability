package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.Predicate;

/**
 * Echo: a cast repeated from somewhere else, weaker. Afterimages that cast what their Beyonder casts (Key Of Stars'
 * Omnipresence, the Planeswalker's Blink), a projection, a mirror self. Works for any ability of any Pathway, because
 * it repeats what combat/Casts saw go off:
 *
 *   Casts.addListener((caster, sl, cast) -> {
 *       if (hasAfterimages(caster)) Echo.repeat(caster, cast, spots, 0.6f, i -> showEcho(caster, i));
 *   });
 *   Echo.weaker(player, 0.8f, () -> Casts.recast(player, cast));   // a copy whose hits land at 80%
 *   Echo.strength(entity)                                           // what its hits land at right now (1 = whole)
 *   Echo.addSkip(cast -> ...)                                       // once, in a static block: never repeated elsewhere
 *
 * repeat(...) stands the caster on each spot for the moment of the cast (on the server only: nobody sees them move),
 * casts it there as a copy (Casts.recast: free, no cooldown, not told again, so an echo never echoes) and puts them
 * back. A copy that moved the caster itself (a teleport) puts them back properly. They keep facing where they face,
 * so what is aimed is aimed from the spot along the same look.
 *
 * How "weaker" works: while it runs, every hit the caster makes lands at 'strength' (LivingHurtEvent), and so does
 * every hit of what it set off for later: a combat/Later task it scheduled carries the strength with it, and a
 * projectile it fired is marked. What is not a hit (a root, a heal, a teleport) is whole.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Echo {

    private static final String SHOT_TAG = "circle_of_inevitability_echo_strength";

    /** caster -> the strength their hits land at while an echo of theirs runs. */
    private static final Map<UUID, Float> STRENGTH = new HashMap<>();
    private static final List<Predicate<Casts.Cast>> SKIPS = new ArrayList<>();

    private Echo() {}

    // =========================
    // API
    // =========================

    /**
     * Casts 'cast' again from every spot in 'spots' (feet positions), its hits at 'strength'. 'shown' (may be null)
     * runs with the spot's index after each one went off, for the look. Nothing happens for a skipped or uncopyable
     * cast. Returns how many went off.
     */
    public static int repeat(ServerPlayer caster, Casts.Cast cast, List<Vec3> spots, float strength, IntConsumer shown) {
        if (cast == null || !cast.copyable() || isSkipped(cast) || !(caster.level() instanceof ServerLevel sl)) return 0;
        int wentOff = 0;
        for (int i = 0; i < spots.size(); i++) {
            Vec3 spot = spots.get(i);
            if (spot == null || !caster.isAlive() || caster.level() != sl) continue;
            Vec3 home = caster.position();
            caster.setPos(spot.x, spot.y, spot.z);
            boolean went;
            try {
                went = weaker(caster, strength, () -> Casts.recast(caster, cast));
            } finally {
                if (caster.level() == sl) {
                    if (caster.position().distanceToSqr(spot) > 0.01) caster.teleportTo(home.x, home.y, home.z);   // it moved them
                    else caster.setPos(home.x, home.y, home.z);
                }
            }
            if (went) {
                wentOff++;
                if (shown != null) shown.accept(i);
            }
        }
        return wentOff;
    }

    /** Runs 'run' with every hit 'caster' makes in it (and in what it starts for later) at 'strength'. Returns its answer. */
    public static boolean weaker(Entity caster, float strength, BooleanSupplier run) {
        UUID id = caster.getUUID();
        Float before = STRENGTH.get(id);
        STRENGTH.put(id, Math.max(0f, strength) * (before == null ? 1f : before));
        try {
            return run.getAsBoolean();
        } finally {
            if (before == null) STRENGTH.remove(id);
            else STRENGTH.put(id, before);
        }
    }

    /** The strength this entity's hits land at right now: 1 outside every echo. */
    public static float strength(Entity entity) {
        if (entity == null || STRENGTH.isEmpty()) return 1f;
        Float strength = STRENGTH.get(entity.getUUID());
        return strength == null ? 1f : strength;
    }

    /** Once, in a static block: casts this says true for are never repeated from elsewhere (a door that moves you). */
    public static void addSkip(Predicate<Casts.Cast> skip) {
        SKIPS.add(skip);
    }

    public static boolean isSkipped(Casts.Cast cast) {
        for (Predicate<Casts.Cast> skip : SKIPS) if (skip.test(cast)) return true;
        return false;
    }

    /**
     * combat/Later wraps every task it is given in this: a task scheduled while an echo runs keeps its strength when it
     * runs later. Outside an echo it hands the task back as it is.
     */
    public static Runnable carry(Runnable task) {
        if (STRENGTH.isEmpty()) return task;
        Map<UUID, Float> then = new HashMap<>(STRENGTH);
        return () -> {
            Map<UUID, Float> before = new HashMap<>(STRENGTH);
            STRENGTH.putAll(then);
            try {
                task.run();
            } finally {
                STRENGTH.clear();
                STRENGTH.putAll(before);
            }
        };
    }

    // =========================
    // Events
    // =========================

    /** A shot fired during an echo keeps its strength. */
    @SubscribeEvent
    public static void onEchoShot(EntityJoinLevelEvent event) {
        if (STRENGTH.isEmpty() || !(event.getEntity() instanceof Projectile shot) || event.getLevel().isClientSide()) return;
        float strength = strength(shot.getOwner());
        if (strength < 1f) shot.getPersistentData().putFloat(SHOT_TAG, strength);
    }

    /** The hits of an echo land weaker. */
    @SubscribeEvent
    public static void onEchoHurt(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        float strength = strength(event.getSource().getEntity());
        Entity direct = event.getSource().getDirectEntity();
        if (direct != null && direct.getPersistentData().contains(SHOT_TAG)) {
            strength = Math.min(strength, direct.getPersistentData().getFloatOr(SHOT_TAG, 1f));
        }
        if (strength < 1f) event.setAmount(event.getAmount() * strength);
    }
}
