package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.server.level.ServerLevel;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Glide: a player is carried along where they look for a while, fast, sinking a little, with no fall damage.
 * For wings, a dive, a leap on the wind. Works from any class/ability:
 *
 *   Glide.start(player, 50, 0.95, 0.08);                    // 50 ticks, 0.95 blocks a tick, sinking 0.08 a tick
 *   Glide.start(player, 50, 0.95, 0.08, 2.5, (glider, passed) -> {
 *       // at the end: every enemy that came within 2.5 blocks on the way (still alive), first passed first
 *   });
 *   Glide.isGliding(entity)    Glide.ticksLeft(entity)
 *   Glide.end(player);          // lands them now (the end callback runs)
 *
 * Looking down dives faster, looking up climbs (as far as the speed allows). The fall after it is safe
 * (FallGuard, LANDING_GUARD_TICKS). Only the moving: wings or anything else to show for it are yours to add.
 * Starting again replaces the old glide (without its callback). Not saved: dying or logging out drops it.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Glide {

    /** How long the landing after a glide is safe. */
    public static int LANDING_GUARD_TICKS = 200;
    /** The fastest it ever goes down (blocks per tick), however steeply they look down. */
    public static double MAX_DIVE = 0.6;

    @FunctionalInterface
    public interface Done {
        void run(Player glider, List<LivingEntity> passed);
    }

    private static final class State {
        long until;
        double speed;
        double sink;
        double catchRadius;
        final Set<LivingEntity> passed = new LinkedHashSet<>();
        Done done;
    }

    private static final Map<UUID, State> GLIDES = new HashMap<>();

    private Glide() {}

    // =========================
    // API
    // =========================

    public static void start(Player player, int ticks, double speed, double sink) {
        start(player, ticks, speed, sink, 0, null);
    }

    /** catchRadius: enemies this close on the way are handed to 'done' at the end (0 = none are looked for). */
    public static void start(Player player, int ticks, double speed, double sink, double catchRadius, Done done) {
        if (!(player.level() instanceof ServerLevel sl) || ticks <= 0) return;
        State state = new State();
        state.until = sl.getGameTime() + ticks;
        state.speed = speed;
        state.sink = sink;
        state.catchRadius = Math.max(0, catchRadius);
        state.done = done;
        GLIDES.put(player.getUUID(), state);
        FallGuard.protect(player, ticks + LANDING_GUARD_TICKS);
        if (player.onGround()) {                                    // off the ground first
            player.setDeltaMovement(player.getDeltaMovement().x, 0.5, player.getDeltaMovement().z);
            player.hurtMarked = true;
        }
    }

    public static boolean isGliding(Entity entity) {
        return entity != null && GLIDES.containsKey(entity.getUUID());
    }

    public static int ticksLeft(Entity entity) {
        State state = entity == null ? null : GLIDES.get(entity.getUUID());
        return state == null ? 0 : (int) Math.max(0, state.until - entity.level().getGameTime());
    }

    /** Lands them now; the end callback runs. Does nothing if they are not gliding. */
    public static void end(Player player) {
        State state = GLIDES.remove(player.getUUID());
        if (state == null) return;
        FallGuard.protect(player, LANDING_GUARD_TICKS);
        if (state.done == null || !(player.level() instanceof ServerLevel sl) || !Spells.casterStillHere(player, sl)) return;
        List<LivingEntity> passed = new ArrayList<>();
        for (LivingEntity one : state.passed) {
            if (one.isAlive() && one.level() == sl) passed.add(one);
        }
        state.done.run(player, passed);
    }

    // =========================
    // Inside
    // =========================

    @SubscribeEvent
    public static void onGlidePlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (GLIDES.isEmpty()) return;
        Player player = event.player();
        if (!(player.level() instanceof ServerLevel sl)) return;
        State state = GLIDES.get(player.getUUID());
        if (state == null) return;
        if (sl.getGameTime() >= state.until || player.isSpectator() || !player.isAlive()) {
            end(player);
            return;
        }
        Vec3 look = player.getLookAngle();
        double down = Math.max(-MAX_DIVE, look.y * state.speed - state.sink);
        player.setDeltaMovement(look.x * state.speed, down, look.z * state.speed);
        player.hurtMarked = true;                                   // makes the player's client take the motion
        player.fallDistance = 0;
        if (state.catchRadius > 0) {
            state.passed.addAll(Spells.enemiesAround(player, sl, player.getBoundingBox().getCenter(), state.catchRadius));
        }
    }

    @SubscribeEvent
    public static void onGlideLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        GLIDES.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onGlideRespawn(PlayerEvent.PlayerRespawnEvent event) {
        GLIDES.remove(event.getEntity().getUUID());
    }
}
