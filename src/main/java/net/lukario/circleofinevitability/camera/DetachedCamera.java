package net.lukario.circleofinevitability.camera;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.network.PacketHandler;
import net.lukario.circleofinevitability.status.Parasite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Server-thread entry points for abilities. A player owns at most one view, and keeps their real body. */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DetachedCamera {
    public record View(Vec3 position, Vec3 direction) {}
    @FunctionalInterface
    public interface Action {
        void use(ServerPlayer player, View view);
    }

    private static final Map<ServerPlayer, Session> ACTIVE = new HashMap<>();

    private record Pose(Vec3 position, float yaw, float pitch) {}

    private static final class Session {
        final UUID id = UUID.randomUUID();
        final CameraPreset preset;
        final CameraPreset.Mode mode;
        final ServerLevel level;
        final Entity anchor;
        final Vec3 fixedPoint;
        final Action action;
        Vec3 position;
        Vec3 velocity = Vec3.ZERO;
        Input keys = Input.EMPTY;
        float yaw, pitch, sentYaw, sentPitch;
        Pose destination;
        int travelTicks;
        int age, lastInput, returnTicks;
        boolean use;

        Session(ServerPlayer player, CameraPreset preset, Vec3 point, Entity anchor, Action action, boolean scripted) {
            this.mode = scripted ? CameraPreset.Mode.SCRIPTED : preset.motion().mode();
            this.level = player.level();
            this.preset = preset;
            this.anchor = anchor;
            this.fixedPoint = point;
            this.position = point;
            this.action = action;
            sentYaw = yaw = player.getYRot();
            sentPitch = pitch = player.getXRot();
        }

        Vec3 center() { return anchor == null ? fixedPoint : anchor.getEyePosition(); }
        Vec3 direction() { return Vec3.directionFromRotation(pitch, yaw); }
    }

    private DetachedCamera() {}

    /** A bare name resolves in this mod's namespace. Fully qualified datapack IDs also work. */
    public static boolean start(ServerPlayer player, String preset) {
        return follow(player, preset, player, null);
    }

    public static boolean start(ServerPlayer player, String preset, Action action) {
        return follow(player, preset, player, action);
    }

    /** Open an ability-driven view and capture its handle in the same call. */
    public static Optional<Handle> startScripted(ServerPlayer player) {
        return startScripted(player, "scripted");
    }

    /** Use the supplied preset's limits, collision and display settings with ability-controlled motion. */
    public static Optional<Handle> startScripted(ServerPlayer player, String preset) {
        return open(player, preset, player.getEyePosition(), player, null, true);
    }

    /** Start and tether the view at a point in the player's dimension. */
    public static boolean at(ServerPlayer player, String preset, Vec3 point) {
        return at(player, preset, point, null);
    }

    public static boolean at(ServerPlayer player, String preset, Vec3 point, Action action) {
        return open(player, preset, point, null, action, false).isPresent();
    }

    /** Use a moving entity's eyes as the anchor for a fixed view, orbit, or flight tether. */
    public static boolean follow(ServerPlayer player, String preset, Entity anchor) {
        return follow(player, preset, anchor, null);
    }

    public static boolean follow(ServerPlayer player, String preset, Entity anchor, Action action) {
        if (anchor == null || !anchor.isAlive() || anchor.level() != player.level()) return false;
        return open(player, preset, anchor.getEyePosition(), anchor, action, false).isPresent();
    }

    private static Optional<Handle> open(ServerPlayer player, String name, Vec3 point, Entity anchor, Action action, boolean scripted) {
        Identifier key = Identifier.tryParse(name.contains(":") ? name : CircleOfInevitability.MOD_ID + ":" + name);
        CameraPreset preset = key == null ? null : CameraPresets.loaded.get(key);
        if (preset == null || active(player) || !player.isAlive() || player.isPassenger() || player.isSleeping()
                || player.getCamera() != player || Parasite.isInside(player) || !available(player, point)
                || !CameraTravel.fits(player.level(), point, preset.collision())) return Optional.empty();
        Session s = new Session(player, preset, point, anchor, action, scripted);
        if (s.mode == CameraPreset.Mode.ORBIT) s.position = orbit(player, s, point);
        ACTIVE.put(player, s);
        player.stopUsingItem();
        player.setSprinting(false);
        PacketHandler.sendToPlayer(CameraFrame.open(s.id, s.position, s.yaw, s.pitch, preset.display(),
                s.mode == CameraPreset.Mode.SCRIPTED), player);
        return Optional.of(new Handle(player, s.id));
    }

    public static boolean active(ServerPlayer player) { return ACTIVE.containsKey(player); }

    /** Capture this when a delayed ability callback needs to refer to its own session. */
    public static Optional<Handle> handle(ServerPlayer player) {
        Session s = ACTIVE.get(player);
        return s == null ? Optional.empty() : Optional.of(new Handle(player, s.id));
    }

    public record Handle(ServerPlayer player, UUID id) {
        public Optional<View> view() {
            Session s = ACTIVE.get(player);
            return s != null && s.id.equals(id) ? Optional.of(new View(s.position, s.direction())) : Optional.empty();
        }

        /** Set the next tick's pose. Call each ability tick to drive an arbitrary path. */
        public boolean setPose(Vec3 position, float yaw, float pitch) {
            return moveTo(position, yaw, pitch, 1);
        }

        /** Move and turn over the given number of game ticks. A new call replaces the current destination. */
        public boolean moveTo(Vec3 position, float yaw, float pitch, int ticks) {
            Session s = ACTIVE.get(player);
            if (s == null || !s.id.equals(id) || s.mode != CameraPreset.Mode.SCRIPTED || s.returnTicks > 0
                    || s.level != player.level() || !player.isAlive() || ticks < 1
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch) || !available(player, position)) return false;
            s.destination = new Pose(position, Mth.wrapDegrees(yaw), Mth.clamp(pitch, -90, 90));
            s.travelTicks = ticks;
            return true;
        }

        public void stop() {
            Session s = ACTIVE.get(player);
            if (s != null && s.id.equals(id)) DetachedCamera.stop(player);
        }
    }

    /** An ability can end its view early. Repeated stops are harmless. */
    public static void stop(ServerPlayer player) {
        Session s = ACTIVE.get(player);
        if (s == null || s.returnTicks > 0) return;
        s.returnTicks = s.preset.display().returnTicks();
        if (s.returnTicks == 0) close(player);
        else {
            s.keys = Input.EMPTY;
            s.use = false;
            PacketHandler.sendToPlayer(CameraFrame.returning(s.id, s.returnTicks), player);
        }
    }

    private static void close(ServerPlayer player) {
        Session s = ACTIVE.remove(player);
        if (s != null) PacketHandler.sendToPlayer(CameraFrame.close(s.id), player);
    }

    static void input(ServerPlayer player, CameraInput packet) {
        Session s = ACTIVE.get(player);
        if (s == null || !s.id.equals(packet.session())) return;
        if (packet.lostView()) { close(player); return; }
        if (packet.exit() && s.preset.manualExit()) { stop(player); return; }
        if (s.returnTicks > 0) return;
        if (s.mode != CameraPreset.Mode.SCRIPTED) {
            if (!Float.isFinite(packet.yaw()) || !Float.isFinite(packet.pitch())) return;
            s.yaw = Mth.wrapDegrees(packet.yaw());
            s.pitch = Mth.clamp(packet.pitch(), -90, 90);
            s.keys = packet.keys();
            s.lastInput = s.age;
        }
        s.use |= packet.use() && s.action != null;
    }

    private static boolean available(ServerPlayer player, Vec3 point) {
        if (point == null || !point.isFinite()) return false;
        BlockPos pos = BlockPos.containing(point);
        ServerLevel level = player.level();
        return level.isInWorldBounds(pos) && level.getWorldBorder().isWithinBounds(point)
                && player.getChunkTrackingView().contains(SectionPos.blockToSectionCoord(pos.getX()),
                        SectionPos.blockToSectionCoord(pos.getZ()))
                && level.hasChunkAt(pos);
    }

    private static Vec3 orbit(ServerPlayer player, Session s, Vec3 center) {
        double radius = s.preset.motion().orbitRadius();
        if (s.preset.range() > 0) radius = Math.min(radius, s.preset.range());
        Vec3 endpoint = center.subtract(s.direction().scale(radius));
        return available(player, endpoint) ? CameraTravel.sweep(s.level, center, endpoint, s.preset.collision()) : center;
    }

    private static Vec3 fly(Session s) {
        Input keys = s.keys;
        int forward = Boolean.compare(keys.forward(), keys.backward());
        int left = Boolean.compare(keys.left(), keys.right());
        int up = Boolean.compare(keys.jump(), keys.shift());
        Vec3 wish = s.direction().scale(forward).add(Vec3.directionFromRotation(0, s.yaw - 90).scale(left)).add(0, up, 0);
        CameraPreset.Motion motion = s.preset.motion();
        double speed = motion.speed() * (keys.sprint() ? motion.sprintMultiplier() : 1);
        s.velocity = s.velocity.lerp(wish.normalize().scale(speed), wish.lengthSqr() == 0 ? motion.braking() : motion.acceleration());
        if (s.velocity.lengthSqr() < 1e-8) s.velocity = Vec3.ZERO;
        return s.position.add(s.velocity);
    }

    private static Vec3 advanceScript(Session s) {
        if (s.travelTicks == 0) return s.position;
        double fraction = 1.0 / s.travelTicks--;
        if (s.travelTicks == 0) {
            s.yaw = s.destination.yaw();
            s.pitch = s.destination.pitch();
            return s.destination.position();
        }
        s.yaw = Mth.rotLerp((float) fraction, s.yaw, s.destination.yaw());
        s.pitch = Mth.lerp((float) fraction, s.pitch, s.destination.pitch());
        return s.position.lerp(s.destination.position(), fraction);
    }

    // Ability paths can cover many chunks in a single tick. Walk the crossed chunk columns without loading them.
    private static boolean trackedPath(ServerPlayer player, Vec3 from, Vec3 to) {
        Vec3 start = new Vec3(from.x / 16, 0, from.z / 16);
        Vec3 end = new Vec3(to.x / 16, 0, to.z / 16);
        return BlockGetter.traverseBlocks(start, end, player, (p, column) -> {
            int x = column.getX(), z = column.getZ();
            return p.getChunkTrackingView().contains(x, z) && p.level().hasChunk(x, z) ? null : Boolean.FALSE;
        }, p -> Boolean.TRUE);
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent.Post event) {
        if (!(event.player() instanceof ServerPlayer player)) return;
        Session s = ACTIVE.get(player);
        if (s == null) return;
        if (s.level != player.level() || !player.isAlive() || player.isPassenger() || player.isSleeping()
                || player.getCamera() != player || Parasite.isInside(player)
                || (s.anchor != null && (!s.anchor.isAlive() || s.anchor.level() != s.level))
                || !available(player, s.center()) || !available(player, s.position)) {
            close(player);
            return;
        }

        if (s.returnTicks > 0) {
            Vec3 next = s.position.lerp(player.getEyePosition(), 1.0 / s.returnTicks);
            // Snap home when the animated path would cross a wall or untracked terrain.
            if (--s.returnTicks == 0 || !available(player, next)
                    || !CameraTravel.sweep(s.level, s.position, next, s.preset.collision()).equals(next)) {
                close(player);
                return;
            }
            move(player, s, next);
            return;
        }
        if (++s.age > s.preset.duration() && s.preset.duration() > 0) { stop(player); return; }
        if (s.age - s.lastInput > 5) s.keys = Input.EMPTY;
        Vec3 center = s.center();
        Vec3 next = switch (s.mode) {
            case FREE -> fly(s);
            case FIXED -> center;
            case SCRIPTED -> advanceScript(s);
            case ORBIT -> orbit(player, s, center);
        };
        double range = s.preset.range();
        if (range > 0 && next.distanceToSqr(center) > range * range + 1e-8) {
            if (s.preset.returnAtBoundary()) { stop(player); return; }
            next = center.add(next.subtract(center).normalize().scale(range));
        }
        if (available(player, next) && (s.mode != CameraPreset.Mode.SCRIPTED || trackedPath(player, s.position, next))) {
            Vec3 resolved = CameraTravel.sweep(s.level, s.position, next, s.preset.collision());
            if (range > 0 && resolved.distanceToSqr(center) > range * range + 1e-6) { stop(player); return; }
            if (!resolved.equals(next)) s.velocity = Vec3.ZERO;
            move(player, s, resolved);
        } else {
            s.velocity = Vec3.ZERO;
            if (s.preset.returnAtBoundary()) { stop(player); return; }
            move(player, s, s.position);
        }
        if (s.use) {
            s.use = false;
            s.action.use(player, new View(s.position, s.direction()));
        }
    }

    private static void move(ServerPlayer player, Session s, Vec3 position) {
        boolean scripted = s.mode == CameraPreset.Mode.SCRIPTED && s.returnTicks == 0;
        boolean turned = scripted && (s.yaw != s.sentYaw || s.pitch != s.sentPitch);
        if (!turned && position.equals(s.position)) return;
        s.position = position;
        s.sentYaw = s.yaw;
        s.sentPitch = s.pitch;
        CameraFrame frame = scripted ? CameraFrame.pose(s.id, position, s.yaw, s.pitch) : CameraFrame.move(s.id, position);
        PacketHandler.sendToPlayer(frame, player);
    }

    @SubscribeEvent(priority = Priority.LOWEST)
    public static void damage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0) {
            Session s = ACTIVE.get(player);
            if (s != null && s.preset.cancelOnDamage()) close(player);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) close(player);
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer player) close(player);
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) ACTIVE.remove(player);
    }

    @SubscribeEvent
    public static void shutdown(ServerStoppedEvent event) { ACTIVE.clear(); }
}
