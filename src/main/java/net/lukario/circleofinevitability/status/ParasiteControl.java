package net.lukario.circleofinevitability.status;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.network.CParasiteControlPacket;
import net.lukario.circleofinevitability.network.PacketHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Steering a host: a parasite (status/Parasite) moves its host, makes it strike and tells it what to hunt.
 * Only a Mob can be steered; a player host just carries its parasite. Parasite starts and stops this by itself
 * for a parasitism whose Hooks.control(host) gives Settings; nothing else needs to call it.
 *
 * What the parasite does (its own keys, read by client/ClientParasiteControl and sent with SParasiteControlPacket):
 *   - walk keys, jump, sprint: the host walks that way, relative to where the parasite looks; a host in the air or
 *     in water (a bee, a fish, a mob that is swimming) goes along the look, up and down too, and jump lifts it
 *   - the mouse: where the host looks (the parasite's view follows the mouse at once, the host's head a moment later)
 *   - left click: the host strikes what the parasite looks at: a mob that shoots (skeleton, pillager, witch,
 *     snow golem) shoots it, any other hits it if it is within reach
 *   - right click on something: the host hunts it on its own until it dies or another is picked; on nothing: it
 *     stops hunting
 * Your input wins: while the parasite is pressing, moving the mouse or clicking, the host's own will to walk, look
 * and jump is switched off (its goals that need those do not run). After Settings.idleTicks with no input it acts
 * on its own again (still hunting what it was told to). Holding sneak (the view out of the host) also lets it be.
 *
 * Nothing here is saved; it ends with the parasitism.
 */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ParasiteControl {

    /**
     * How a parasite steers. The ability that makes parasites gives these from Parasite.Hooks.control(host).
     * idleTicks:   after this long without input the host acts on its own again
     * walkSpeed:   how fast it walks when steered (1 = its normal speed when it walks to something)
     * sprintSpeed: the same while sprint is held
     * reach:       how far a strike reaches (blocks from its eyes to the body of what it hits)
     * strikeRest:  ticks between two strikes
     * bareDamage:  what a strike does for a host that has no attack of its own (a cow, a villager)
     * shotRange:   how far a host that shoots can be made to shoot
     * shotRest:    ticks between two shots
     * huntRange:   how far away right click can pick something to hunt
     */
    public record Settings(int idleTicks, double walkSpeed, double sprintSpeed, double reach, int strikeRest,
                           float bareDamage, double shotRange, int shotRest, double huntRange) {}

    /** How far ahead of the host the point it walks to is put each tick (blocks). Only the direction matters. */
    private static final double STEP = 2.0;
    private static final float MAX_TURN = 180f;               // the head follows the look at once
    private static final Goal.Flag[] OWN_WILL = {Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP};

    /** One steered host. */
    private static final class Steer {
        final UUID parasite;
        final Settings settings;
        final Parasite.Hooks hooks;
        float forward, strafe, yaw, pitch;       // the last input: forward/back -1..1, left(+)/right(-) -1..1, the look
        boolean jump, sprint;
        boolean strike, pick;                    // clicks waiting for the host's next tick
        long lastInput = Long.MIN_VALUE / 2;     // game time of the last input
        long nextStrike;                         // game time from which it may strike (or shoot) again
        boolean holding;                         // its own will is switched off right now
        UUID prey;                               // what right click told it to hunt

        Steer(UUID parasite, Settings settings, Parasite.Hooks hooks) {
            this.parasite = parasite;
            this.settings = settings;
            this.hooks = hooks;
        }
    }

    /** host -> who steers it. */
    private static final Map<UUID, Steer> BY_HOST = new HashMap<>();

    private ParasiteControl() {}

    // =========================
    // API (Parasite calls start and stop; the packet calls input)
    // =========================

    /** From now on the parasite steers this host. Tells its game to send what it presses. */
    static void start(ServerPlayer parasite, Mob host, Settings settings, Parasite.Hooks hooks) {
        Steer steer = new Steer(parasite.getUUID(), settings, hooks);
        steer.yaw = host.getYHeadRot();
        steer.pitch = host.getXRot();
        BY_HOST.put(host.getUUID(), steer);
        PacketHandler.sendToPlayer(new CParasiteControlPacket(host.getId(), settings.idleTicks()), parasite);
    }

    /** It is over: the host has its own will back (if it is still there) and the parasite's game stops sending. */
    static void stop(ServerPlayer parasite, LivingEntity host) {
        BY_HOST.values().removeIf(steer -> {
            if (!steer.parasite.equals(parasite.getUUID())) return false;
            if (steer.holding && host instanceof Mob mob) letGo(mob, steer);
            return true;
        });
        PacketHandler.sendToPlayer(new CParasiteControlPacket(-1, 0), parasite);
    }

    /** True while this player steers its host (it gave input within the last Settings.idleTicks). */
    public static boolean isSteering(Player parasite) {
        if (parasite == null) return false;
        for (Steer steer : BY_HOST.values()) {
            if (steer.parasite.equals(parasite.getUUID())) {
                return parasite.level().getGameTime() - steer.lastInput < steer.settings.idleTicks();
            }
        }
        return false;
    }

    /**
     * What the parasite's game sent this tick (SParasiteControlPacket). Ignored from a player who steers nothing.
     * forward and strafe are -1, 0 or 1 (strafe 1 = left); yaw and pitch are where they look.
     */
    public static void input(ServerPlayer parasite, int forward, int strafe, boolean jump, boolean sprint,
                             boolean strike, boolean pick, float yaw, float pitch) {
        if (!Parasite.isInside(parasite) || !Float.isFinite(yaw) || !Float.isFinite(pitch)) return;
        for (Steer steer : BY_HOST.values()) {
            if (!steer.parasite.equals(parasite.getUUID())) continue;
            steer.forward = Mth.clamp(forward, -1, 1);
            steer.strafe = Mth.clamp(strafe, -1, 1);
            steer.jump = jump;
            steer.sprint = sprint;
            steer.yaw = Mth.wrapDegrees(yaw);
            steer.pitch = Mth.clamp(pitch, -90f, 90f);
            steer.strike |= strike;
            steer.pick |= pick;
            steer.lastInput = parasite.level().getGameTime();
            return;
        }
    }

    // =========================
    // Inner workings
    // =========================

    /** Every tick of the host, before its own AI runs. */
    private static void tick(Mob host, Steer steer, ServerLevel sl) {
        ServerPlayer parasite = sl.getServer().getPlayerList().getPlayer(steer.parasite);
        if (parasite == null) return;                     // Parasite's own tick ends it

        hunt(host, steer, sl);
        long now = sl.getGameTime();
        boolean steering = now - steer.lastInput < steer.settings.idleTicks() && parasite.getCamera() == host;
        if (steering) steer(host, steer);
        else if (steer.holding) letGo(host, steer);

        if (steer.pick) {
            steer.pick = false;
            pick(parasite, host, steer, sl);
        }
        if (steer.strike) {
            steer.strike = false;
            strike(parasite, host, steer, sl, now);
        }
    }

    /** Your input wins: the host's own walking, looking and jumping are off, and it does what the keys say. */
    private static void steer(Mob host, Steer steer) {
        // every tick: the game itself switches them back on every few ticks (Mob.updateControlFlags)
        for (Goal.Flag flag : OWN_WILL) host.goalSelector.setControlFlag(flag, false);
        steer.holding = true;
        host.getNavigation().stop();

        Vec3 look = Vec3.directionFromRotation(steer.pitch, steer.yaw);
        host.setYRot(steer.yaw);
        host.setYHeadRot(steer.yaw);
        host.setXRot(steer.pitch);
        Vec3 eye = host.getEyePosition();
        host.getLookControl().setLookAt(eye.x + look.x * 8, eye.y + look.y * 8, eye.z + look.z * 8, MAX_TURN, MAX_TURN);

        boolean grounded = host.onGround();
        if (steer.forward != 0 || steer.strafe != 0 || (steer.jump && !grounded)) {
            Vec3 ahead = grounded ? Vec3.directionFromRotation(0f, steer.yaw) : look;
            Vec3 left = Vec3.directionFromRotation(0f, steer.yaw - 90f);
            Vec3 way = ahead.scale(steer.forward).add(left.scale(steer.strafe));
            if (steer.jump && !grounded) way = way.add(0, 1, 0);        // in the air or in water: jump lifts it
            if (way.lengthSqr() > 1.0E-4) {
                Vec3 to = host.position().add(way.normalize().scale(STEP));
                if (grounded) to = new Vec3(to.x, host.getY(), to.z);
                double speed = steer.sprint ? steer.settings.sprintSpeed() : steer.settings.walkSpeed();
                host.getMoveControl().setWantedPosition(to.x, to.y, to.z, speed);
            }
        }
        if (steer.jump) host.getJumpControl().jump();
    }

    /** No input for a while: its own will again. */
    private static void letGo(Mob host, Steer steer) {
        // as the game decides it (Mob.updateControlFlags): a mob ridden by a mob that steers it stays off
        boolean free = !(host.getControllingPassenger() instanceof Mob);
        for (Goal.Flag flag : OWN_WILL) host.goalSelector.setControlFlag(flag, free);
        steer.holding = false;
    }

    /** Keeps it on what it was told to hunt; forgets that once it is dead or gone. */
    private static void hunt(Mob host, Steer steer, ServerLevel sl) {
        if (steer.prey == null) return;
        Entity found = sl.getEntity(steer.prey);
        if (!(found instanceof LivingEntity prey) || !prey.isAlive() || prey.isSpectator()) {
            steer.prey = null;
            return;
        }
        if (host.getTarget() != prey || host.tickCount % 20 == 0) aim(host, prey);
    }

    /** Right click: hunt what the parasite looks at, or nothing. */
    private static void pick(ServerPlayer parasite, Mob host, Steer steer, ServerLevel sl) {
        LivingEntity prey = lookedAt(parasite, host, steer, sl, steer.settings.huntRange());
        if (prey == null) {
            steer.prey = null;
            aim(host, null);
            parasite.sendOverlayMessage(Component.literal("Your host hunts nothing now."));
            return;
        }
        steer.prey = prey.getUUID();
        aim(host, prey);
        parasite.sendOverlayMessage(Component.literal("Your host hunts " + prey.getName().getString() + "."));
    }

    /** Left click: shoot or hit what the parasite looks at. */
    private static void strike(ServerPlayer parasite, Mob host, Steer steer, ServerLevel sl, long now) {
        if (now < steer.nextStrike) return;
        Settings settings = steer.settings;

        if (host instanceof RangedAttackMob shooter) {
            LivingEntity prey = lookedAt(parasite, host, steer, sl, settings.shotRange());
            if (prey == null) return;
            steer.nextStrike = now + settings.shotRest();
            shooter.performRangedAttack(prey, 1.0f);
            struck(parasite, host, prey, steer, sl);
            return;
        }

        host.swing(InteractionHand.MAIN_HAND);
        LivingEntity prey = lookedAt(parasite, host, steer, sl, settings.reach());
        if (prey == null) return;                         // a swing at the air
        steer.nextStrike = now + settings.strikeRest();
        if (host.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
            host.doHurtTarget(sl, prey);                  // its own attack: its damage, weapon, knockback
        } else {
            prey.hurt(sl.damageSources().mobAttack(host), settings.bareDamage());   // it has none: a plain blow
        }
        struck(parasite, host, prey, steer, sl);
    }

    private static void struck(ServerPlayer parasite, Mob host, LivingEntity prey, Steer steer, ServerLevel sl) {
        try {
            steer.hooks.struck(parasite, host, prey, sl);
        } catch (RuntimeException e) {
            CircleOfInevitability.LOGGER.error("A parasite's struck hook failed", e);
        }
    }

    /**
     * What the parasite looks at through the host's eyes within 'range' (not through walls), or null. Never the
     * host itself, and never what Spells.isEnemy spares for the parasite (its allies, its own marionettes).
     */
    private static LivingEntity lookedAt(ServerPlayer parasite, Mob host, Steer steer, ServerLevel sl, double range) {
        Vec3 eye = host.getEyePosition();
        Vec3 far = eye.add(Vec3.directionFromRotation(steer.pitch, steer.yaw).scale(range));
        Vec3 end = sl.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, host)).getLocation();
        for (LivingEntity candidate : Spells.enemiesOnLine(parasite, sl, eye, end, 0.4)) {
            if (candidate != host) return candidate;
        }
        return null;
    }

    /** Points the mob at a target (or at nothing), also for mobs that keep their target in a brain. */
    private static void aim(Mob mob, LivingEntity target) {
        if (mob.getTarget() != target) mob.setTarget(target);
        if (target != null) {
            mob.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET, target);
        } else if (mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
    }

    // =========================
    // Events
    // =========================

    // At the start of the host's tick, so what it is told counts before its own AI runs this tick
    @SubscribeEvent
    public static void onParasiteControlLivingTick(LivingEvent.LivingTickEvent event) {
        if (BY_HOST.isEmpty()) return;
        if (!(event.getEntity() instanceof Mob host) || !(host.level() instanceof ServerLevel sl)) return;
        Steer steer = BY_HOST.get(host.getUUID());
        if (steer != null) tick(host, steer, sl);
    }
}
