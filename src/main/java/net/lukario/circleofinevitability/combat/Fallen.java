package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.status.Marionette;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Fallen: who died where, lately. Every mob that dies is written down (its kind, where it fell, which way it
 * faced, when) and forgotten after KEEP_TICKS. Players, bosses (MAX_HEALTH or more) and marionettes are left out.
 * For abilities that raise the dead, call back what just fell, or count the bodies on a battlefield:
 *
 *   for (Fallen.Body body : Fallen.near(sl, spot, 8, 200)) {        // fell within 8 blocks in the last 10 seconds
 *       Mob risen = Fallen.rise(body, player, sl, 600);             // up again as what it was, fighting for the player
 *   }
 *
 * near(...) is nearest first. take(body) takes one off the list so it is found only once: take it when you start
 * showing it rise, and rise(...) it when the look is done (rise takes it too, if nobody did).
 * Nothing is saved: a restart forgets them all, as does the end of KEEP_TICKS.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Fallen {

    /** How long a body is remembered (ticks). */
    public static int KEEP_TICKS = 400;
    /** Anything with this much maximum health or more is a boss and is not written down. */
    public static float MAX_HEALTH = 150f;

    /** One that fell: what it was ('type'), where and which way ('where', 'yaw'), and at which game time. */
    public record Body(UUID id, EntityType<?> type, ResourceKey<Level> dimension, Vec3 where, float yaw, long tick, boolean baby) {
        public long age(ServerLevel sl) {
            return sl.getGameTime() - tick;
        }
    }

    private static final LinkedList<Body> BODIES = new LinkedList<>();

    private Fallen() {}

    // =========================
    // API
    // =========================

    /** The bodies that fell within 'radius' of 'center' in this level in the last 'maxAgeTicks', nearest first. */
    public static List<Body> near(ServerLevel sl, Vec3 center, double radius, int maxAgeTicks) {
        forgetOld(sl);
        List<Body> found = new ArrayList<>();
        for (Body body : BODIES) {
            if (body.dimension() != sl.dimension() || body.age(sl) > maxAgeTicks) continue;
            if (body.where().distanceTo(center) <= radius) found.add(body);
        }
        found.sort(Comparator.comparingDouble(body -> body.where().distanceToSqr(center)));
        return found;
    }

    /** Takes it off the list: it will not be found again. False if it was already taken. */
    public static boolean take(Body body) {
        return BODIES.remove(body);
    }

    /**
     * The body gets up again where it fell, as what it was (a skeleton as a skeleton, a baby as a baby), and fights
     * for 'master' for 'ticks' as a summon (Marionette.summon: it vanishes when its time is up, with no drops).
     * It is taken off the list if it is still on it. Null if it cannot rise (not a mob, no room, another dimension,
     * or it never really died).
     */
    public static Mob rise(Body body, Player master, ServerLevel sl, int ticks) {
        if (body.dimension() != sl.dimension()) return null;
        take(body);
        Entity same = sl.getEntity(body.id());
        if (same != null && same.isAlive()) return null;                // its death was cheated: it is still up
        Entity made = body.type().create(sl, EntitySpawnReason.MOB_SUMMONED);
        if (!(made instanceof Mob mob)) {
            if (made != null) made.discard();
            return null;
        }
        mob.snapTo(body.where().x, body.where().y, body.where().z, body.yaw(), 0f);
        mob.setYHeadRot(body.yaw());
        if (body.baby()) mob.setBaby(true);
        if (!sl.addFreshEntity(mob)) return null;
        if (!Marionette.summon(mob, master, ticks)) {
            mob.discard();
            return null;
        }
        return mob;
    }

    // =========================
    // Internals
    // =========================

    private static void forgetOld(ServerLevel sl) {
        long now = sl.getGameTime();
        Iterator<Body> iterator = BODIES.iterator();
        while (iterator.hasNext()) {
            Body body = iterator.next();
            if (now - body.tick() > KEEP_TICKS || body.tick() > now) iterator.remove();     // old, or from before a /time change
        }
    }

    private static boolean remembered(LivingEntity entity) {
        if (entity instanceof Player || !(entity instanceof Mob)) return false;
        if (entity.getMaxHealth() >= MAX_HEALTH) return false;
        return !Marionette.isSummoned(entity);                           // a summon that vanishes left no body
    }

    // =========================
    // Events
    // =========================

    // LOWEST: whatever cheats death (DeathWard) has had its say first; rise() checks once more that it is gone
    @SubscribeEvent(priority = Priority.LOWEST)
    public static void onFallenDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel sl) || !remembered(entity)) return;
        try {
            forgetOld(sl);
            BODIES.add(new Body(entity.getUUID(), entity.getType(), sl.dimension(), entity.position(), entity.getYRot(),
                    sl.getGameTime(), entity.isBaby()));
        } catch (Exception e) {
            CircleOfInevitability.LOGGER.error("Fallen could not write down a death", e);
        }
    }
}
