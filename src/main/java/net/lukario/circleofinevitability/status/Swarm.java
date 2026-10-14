package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Swarm: for a while an entity is spread over a number of small bodies (bats, crows, rats, paper figures, leaves).
 * A blow kills some of them instead of hurting it: one for every 'perBody' damage, one at least. While one is left
 * it cannot be hurt; the blow that kills the last one lands with whatever is left of it, and the swarm is over.
 * Works from any class/ability, on players and mobs:
 *
 *   Swarm.start(player, 120, 12, 4f, new Swarm.Hooks() {
 *       public void bodyLost(LivingEntity who, ServerLevel sl, Vec3 at, int left) { ...a bat bursts at 'at'... }
 *       public void over(LivingEntity who, ServerLevel sl, Swarm.End why) { ...re-form, take the wings away... }
 *   });
 *   Swarm.isActive(entity)    Swarm.bodiesLeft(entity)    Swarm.ticksLeft(entity)
 *   Swarm.end(entity);        // over now (its 'over' hook runs with End.ENDED)
 *
 * It is only the counting. Flying, invisibility and the look of the swarm are yours to add
 * (BeautyGoddess's Bat Swarm adds Flight, invisibility and BeautyGoddessFx.batSwarm).
 * The void and /kill go through it. Starting again replaces the old swarm (without its 'over').
 * Not saved: a swarm is gone after its entity dies or logs out (no 'over' then) or the server stops.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Swarm {

    /** Why a swarm is over. */
    public enum End {
        /** its time ran out */
        TIME,
        /** the last body was killed (the rest of that blow landed) */
        BROKEN,
        /** Swarm.end(...) was called */
        ENDED
    }

    /** What happens along the way. Both are optional. */
    public interface Hooks {
        /** One body was killed. 'at' = a spot round the entity's middle, 'left' = how many are still alive. */
        default void bodyLost(LivingEntity who, ServerLevel sl, Vec3 at, int left) {}

        /** The swarm is over. */
        default void over(LivingEntity who, ServerLevel sl, End why) {}
    }

    private static final class State {
        long until;
        int left;
        float perBody;
        Hooks hooks;
    }

    private static final Map<UUID, State> SWARMS = new HashMap<>();

    private Swarm() {}

    // =========================
    // API
    // =========================

    /** 'entity' is 'bodies' small bodies for 'ticks' ticks; each blow kills one per 'perBody' damage. */
    public static void start(LivingEntity entity, int ticks, int bodies, float perBody, Hooks hooks) {
        if (!(entity.level() instanceof ServerLevel sl) || ticks <= 0 || bodies <= 0) return;
        State state = new State();
        state.until = sl.getGameTime() + ticks;
        state.left = bodies;
        state.perBody = Math.max(0.01f, perBody);
        state.hooks = hooks == null ? new Hooks() {} : hooks;
        SWARMS.put(entity.getUUID(), state);
    }

    public static boolean isActive(Entity entity) {
        return entity != null && SWARMS.containsKey(entity.getUUID());
    }

    public static int bodiesLeft(Entity entity) {
        State state = entity == null ? null : SWARMS.get(entity.getUUID());
        return state == null ? 0 : state.left;
    }

    public static int ticksLeft(Entity entity) {
        State state = entity == null ? null : SWARMS.get(entity.getUUID());
        return state == null ? 0 : (int) Math.max(0, state.until - entity.level().getGameTime());
    }

    /** Over now: its 'over' hook runs with End.ENDED. Does nothing if there is no swarm. */
    public static void end(LivingEntity entity) {
        finish(entity, End.ENDED);
    }

    // =========================
    // Inside
    // =========================

    private static void finish(LivingEntity entity, End why) {
        State state = SWARMS.remove(entity.getUUID());
        if (state != null && entity.level() instanceof ServerLevel sl) state.hooks.over(entity, sl, why);
    }

    @SubscribeEvent
    public static void onSwarmHurt(LivingHurtEvent event) {
        if (SWARMS.isEmpty()) return;
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel sl) || event.getAmount() <= 0f) return;
        State state = SWARMS.get(entity.getUUID());
        if (state == null || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        float amount = event.getAmount();
        int killed = Math.min(state.left, Math.max(1, (int) Math.ceil(amount / state.perBody)));
        state.left -= killed;
        Vec3 middle = entity.getBoundingBox().getCenter();
        double width = Math.max(0.6, entity.getBbWidth() * 2.0), height = Math.max(0.6, entity.getBbHeight() * 0.7);
        for (int i = 0; i < killed; i++) {
            Vec3 at = middle.add((entity.getRandom().nextDouble() - 0.5) * width,
                    (entity.getRandom().nextDouble() - 0.5) * height, (entity.getRandom().nextDouble() - 0.5) * width);
            state.hooks.bodyLost(entity, sl, at, state.left + killed - i - 1);
        }
        if (state.left > 0) {
            event.setAmount(0f);
            return;
        }
        event.setAmount(Math.max(0f, amount - killed * state.perBody));       // what is left of the blow lands
        finish(entity, End.BROKEN);
    }

    @SubscribeEvent
    public static void onSwarmLivingTick(LivingEvent.LivingTickEvent event) {
        if (SWARMS.isEmpty()) return;
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        State state = SWARMS.get(entity.getUUID());
        if (state != null && entity.level().getGameTime() >= state.until) finish(entity, End.TIME);
    }

    @SubscribeEvent
    public static void onSwarmDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide()) SWARMS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onSwarmLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SWARMS.remove(event.getEntity().getUUID());
    }
}
