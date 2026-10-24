package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PathwayAbilities;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Casts: what every Beyonder cast, where and when, for the abilities that watch, copy or answer other abilities
 * (Key Of Stars' Record, Replication, Omnipresence, Quick Study; the last power Star Divination reads). Every ability
 * key press that went off and every wheel pick that went off is told here, by any Pathway, without that Pathway
 * knowing about it:
 *
 *   Casts.Cast last = Casts.last(player);                         // what they cast last (null: nothing in KEPT ticks)
 *   Casts.Cast near = Casts.latestNear(sl, spot, 32, 100, cast -> cast.caster() != me);   // the newest one cast
 *                                                                 // within 32 blocks in the last 5 seconds
 *   Casts.addListener((caster, sl, cast) -> ...)                  // once, in a static block: every cast as it goes off
 *   boolean wentOff = Casts.recast(player, cast);                 // casts it again for this player: free, with no
 *                                                                 // Sequence gate and no cooldown, as a copy
 *   Casts.copying(player, () -> ...)                              // what runs inside is a copy: not told, not copied
 *
 * How a cast is known: a key press counts when it took spirituality (every ability checks first and pays after, as
 * Beyonder.castUnbound already relies on); a wheel pick counts when its entry went off (DONE or FREE). A key press that
 * only opens a wheel counts as nothing until the pick. The ability keys tell keyPressed/keyDone (network/
 * SKeyPressAbility...Used), SoulCore tells spent, AbilityWheel tells wheelCast: nothing else has to.
 *
 * A Cast keeps what is needed to cast it again: the wheel and entry it came from, or the Pathway's ability on that key
 * and whether it was the sneak form. copyable() is false for an ability that only works for its own Pathway
 * (PathwayAbilities' OWN_PATHWAY_ONLY; a wheel entry marked AbilityWheel.notCopyable): copying it would do nothing or
 * break that Pathway's state.
 *
 * Copies (recast, copying) are never told: a copy cannot be copied again, and an echo does not echo. While a copy
 * runs, the caster's single-key cooldowns (combat/Cooldowns) neither stop it nor start.
 * Mobs do not use the keys, so a Beyonder mob's powers are not told here; madeUpFor makes a cast of one of them for
 * the abilities that copy from a mob directly (Replication, Record). Not saved: a restart forgets them.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Casts {

    /** How long a cast is remembered (ticks). */
    public static int KEPT = 1200;

    /**
     * One cast that went off.
     * caster:    who cast it (its UUID: they may have logged out since). casterName: for messages.
     * pathway:   the Pathway it belongs to. key: 1..7, the ability key (0 for a wheel pick made from the screen).
     * sneak:     the sneak form of a key (wheel picks: whether they sneaked).
     * wheel, entry: the wheel entry it was (null for a single key).
     * ability:   the Pathway's ability on that key (PathwayAbilities; may be null for a wheel of a Pathway not listed).
     * dimension: as Anchors.dimensionOf writes it. at: where the caster stood. time: the game time it went off.
     */
    public record Cast(UUID caster, String casterName, String pathway, int key, boolean sneak,
                       AbilityWheel wheel, AbilityWheel.Entry entry, PathwayAbilities.Ability ability,
                       String dimension, Vec3 at, long time) {

        /** "Blink", "Door Opening (sneak)". */
        public String name() {
            if (entry != null) return entry.name();
            if (ability == null) return "an unknown power";
            return sneak && ability.sneakForm() ? ability.name() + " (sneak)" : ability.name();
        }

        /** The Sequence it needs: the entry's own, else the key's. 9 when unknown. */
        public int sequence() {
            if (entry != null) return entry.sequence();
            return ability == null ? 9 : ability.sequence();
        }

        /** False for what only its own Pathway can use (see the class comment). */
        public boolean copyable() {
            if (entry != null) return entry.copyable();                  // a wheel: each entry says (AbilityWheel.notCopyable)
            return ability != null && ability.stealable();
        }

        public boolean isIn(ServerLevel sl) {
            return dimension.equals(sl.dimension().toString());
        }

        /** Ticks since it went off. */
        public long age(ServerLevel sl) {
            return sl.getGameTime() - time;
        }
    }

    /** Told after every cast that went off (never for a copy). */
    @FunctionalInterface
    public interface Listener {
        void cast(ServerPlayer caster, ServerLevel sl, Cast cast);
    }

    /** The key press being handled (abilities run on the server thread, one press at a time). */
    private static final class Pending {
        final Player player;
        final int key;
        final boolean sneak;
        boolean spent;
        boolean told;

        Pending(Player player, int key, boolean sneak) {
            this.player = player;
            this.key = key;
            this.sneak = sneak;
        }
    }

    private static Pending pending;
    private static final Map<UUID, Integer> COPYING = new HashMap<>();
    private static final Map<UUID, Cast> LAST = new HashMap<>();
    private static final List<Cast> RECENT = new ArrayList<>();
    private static final List<Listener> LISTENERS = new ArrayList<>();

    private Casts() {}

    // =========================
    // Asking
    // =========================

    /** What this entity cast last, within KEPT ticks; null if nothing. */
    public static Cast last(Entity entity) {
        Cast cast = LAST.get(entity.getUUID());
        if (cast == null) return null;
        if (entity.level().getGameTime() - cast.time() > KEPT) {
            LAST.remove(entity.getUUID());
            return null;
        }
        return cast;
    }

    /** The newest cast within 'radius' of 'at' in this level, no older than 'maxAge' ticks, that 'which' accepts; or null. */
    public static Cast latestNear(ServerLevel sl, Vec3 at, double radius, int maxAge, Predicate<Cast> which) {
        for (int i = RECENT.size() - 1; i >= 0; i--) {
            Cast cast = RECENT.get(i);
            if (cast.age(sl) > maxAge) break;
            if (!cast.isIn(sl) || cast.at().distanceTo(at) > radius) continue;
            if (which == null || which.test(cast)) return cast;
        }
        return null;
    }

    /** Once, in a static block. */
    public static void addListener(Listener listener) {
        LISTENERS.add(listener);
    }

    /** True while a copy runs for this player (recast, copying). */
    public static boolean isCopying(Entity entity) {
        return entity != null && COPYING.containsKey(entity.getUUID());
    }

    // =========================
    // Copying
    // =========================

    /** Runs 'copy' as a copy for this player and returns what it returns: nothing it casts is told, and single-key cooldowns let it through. */
    public static <T> T copied(Player player, Supplier<T> copy) {
        COPYING.merge(player.getUUID(), 1, Integer::sum);
        try {
            return copy.get();
        } finally {
            COPYING.computeIfPresent(player.getUUID(), (id, depth) -> depth <= 1 ? null : depth - 1);
        }
    }

    /** The same for code that returns nothing. */
    public static void copying(Player player, Runnable copy) {
        copied(player, () -> {
            copy.run();
            return null;
        });
    }

    /**
     * Casts 'cast' again for this player, now, as a copy: no Pathway check, no Sequence gate, no cost
     * (Beyonder.castUnbound), no cooldown, in the form it was cast in (they are made to sneak or stand for the moment).
     * True if it went off. An ability that throws is logged and counts as gone off, so it cannot fail again and again.
     */
    public static boolean recast(ServerPlayer player, Cast cast) {
        if (!(player.level() instanceof ServerLevel sl) || cast == null || !cast.copyable()) return false;
        return copied(player, () -> {
            boolean wasSneaking = player.isShiftKeyDown();
            player.setShiftKeyDown(cast.sneak());
            try {
                if (cast.entry() != null && cast.wheel() != null) {
                    AbilityWheel.Result[] result = {AbilityWheel.Result.REFUSED};
                    Beyonder.castUnbound(player, () -> result[0] = cast.wheel().cast(player, cast.entry(), true));
                    return result[0] != AbilityWheel.Result.REFUSED;
                }
                return Beyonder.castUnbound(player, () -> cast.ability().cast().run(player, sl, sl));
            } catch (RuntimeException e) {
                CircleOfInevitability.LOGGER.error("A copied ability failed: {}", cast.name(), e);
                return true;
            } finally {
                player.setShiftKeyDown(wasSneaking);
            }
        });
    }

    /**
     * A cast as if 'caster' had just cast its Pathway's 'ability' where it stands, for one that does not use the keys
     * (a Beyonder mob, blasphemy_slate/beyonder_mobs). It is not told to anyone: hand it to recast (Replication) or
     * keep it (Record). Pick the ability with Beyonders.randomTakeable so it is one the caster really has.
     *
     *   PathwayAbilities.Ability ability = Beyonders.randomTakeable(mob, random, null);
     *   Casts.Cast cast = Casts.madeUpFor(mob, sl, ability, ability.sneakForm() && random.nextBoolean());
     */
    public static Cast madeUpFor(LivingEntity caster, ServerLevel sl, PathwayAbilities.Ability ability, boolean sneak) {
        return new Cast(caster.getUUID(), caster.getName().getString(), ability.pathway(), ability.key(),
                sneak && ability.sneakForm(), null, null, ability, sl.dimension().toString(), caster.position(),
                sl.getGameTime());
    }

    /**
     * A creature that is no player cast something for real (a Beyonder mob, blasphemy_slate/beyonder_mobs/MobCasting):
     * it is remembered like a player's cast, so last() and latestNear() find it (the crystal ball's "last power",
     * Replication copying what was just cast nearby). The listeners are not told: they are written for player casters.
     * Make the cast with madeUpFor.
     */
    public static void mobCast(LivingEntity caster, ServerLevel sl, Cast cast) {
        if (caster == null || cast == null || caster instanceof Player) return;
        LAST.put(caster.getUUID(), cast);
        RECENT.add(cast);
    }

    // =========================
    // Being told (the keys, SoulCore, AbilityWheel)
    // =========================

    /** An ability key was pressed (before its abilities run). */
    public static void keyPressed(Player player, int key) {
        pending = new Pending(player, key, player.isShiftKeyDown());
    }

    /** The key press is handled: if it took spirituality and no wheel pick told it already, it is told now. */
    public static void keyDone(Player player) {
        Pending done = pending;
        pending = null;
        if (done == null || done.player != player || !done.spent || done.told) return;
        if (!(player instanceof ServerPlayer caster) || !(player.level() instanceof ServerLevel sl)) return;
        String pathway = SoulCore.getPathway(player);
        PathwayAbilities.Ability ability = PathwayAbilities.of(pathway, done.key);
        if (ability == null) return;
        tell(caster, sl, new Cast(player.getUUID(), player.getName().getString(), pathway, done.key, done.sneak,
                null, null, ability, sl.dimension().toString(), player.position(), sl.getGameTime()));
    }

    /** SoulCore: this player's spirituality went down (an ability paid). */
    public static void spent(Player player) {
        if (pending != null && pending.player == player && !isCopying(player)) pending.spent = true;
    }

    /** AbilityWheel: an entry went off for this player. */
    public static void wheelCast(ServerPlayer player, AbilityWheel wheel, AbilityWheel.Entry entry) {
        if (isCopying(player) || !(player.level() instanceof ServerLevel sl)) return;
        int key = 0;
        if (pending != null && pending.player == player) {
            pending.told = true;                                         // the press is this pick, not a cast of its own
            key = pending.key;
        }
        String pathway = SoulCore.getPathway(player);
        PathwayAbilities.Ability ability = key > 0 ? PathwayAbilities.of(pathway, key) : null;
        tell(player, sl, new Cast(player.getUUID(), player.getName().getString(), pathway, key, player.isShiftKeyDown(),
                wheel, entry, ability, sl.dimension().toString(), player.position(), sl.getGameTime()));
    }

    private static void tell(ServerPlayer caster, ServerLevel sl, Cast cast) {
        LAST.put(caster.getUUID(), cast);
        RECENT.add(cast);
        for (Listener listener : List.copyOf(LISTENERS)) {
            try {
                listener.cast(caster, sl, cast);
            } catch (Exception e) {
                CircleOfInevitability.LOGGER.error("A cast listener failed", e);
            }
        }
    }

    // =========================
    // Events
    // =========================

    /** Old casts are forgotten. */
    @SubscribeEvent
    public static void onCastsServerTick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel sl) || sl.getGameTime() % 100 != 0 || RECENT.isEmpty()) return;
        long now = sl.getGameTime();
        RECENT.removeIf(cast -> cast.isIn(sl) && now - cast.time() > KEPT);
        LAST.values().removeIf(cast -> cast.isIn(sl) && now - cast.time() > KEPT);   // mobs that died never log out
        if (RECENT.size() > 2000) RECENT.subList(0, RECENT.size() - 2000).clear();
    }

    @SubscribeEvent
    public static void onCastsLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST.remove(event.getEntity().getUUID());
        COPYING.remove(event.getEntity().getUUID());
        if (pending != null && pending.player == event.getEntity()) pending = null;
    }
}
