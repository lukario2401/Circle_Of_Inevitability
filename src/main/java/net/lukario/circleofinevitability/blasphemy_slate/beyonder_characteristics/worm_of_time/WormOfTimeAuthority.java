package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.worm_of_time;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.*;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.*;
import net.lukario.circleofinevitability.status.*;
import net.minecraft.core.Holder;
import net.minecraft.server.level.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

/** Sequence 2 loopholes, Sequence 1 time authority and Sequence 0 Error. No world clock mutation. */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class WormOfTimeAuthority {
    private WormOfTimeAuthority() {}
    private static final String FIELD = "worm_of_time_authority";
    private record Moment(ServerLevel level, long tick, Vec3 position, float health) {}
    private static final Map<UUID, Deque<Moment>> HISTORY = new HashMap<>();
    // Snapshots are per field; closing a field always restores its projectile states.
    private record Shot(Projectile entity, Vec3 velocity, boolean noGravity, Set<UUID> owners) {}
    private record FieldShots(ServerLevel level, Set<UUID> shots) {}
    private static final Map<UUID, Shot> SUSPENDED = new HashMap<>();
    private static final Map<UUID, FieldShots> FIELDS = new HashMap<>();
    private static final String SHOT_TAG = "worm_of_time_suspended";

    static {
        Silence.addCastsThrough((p, key) -> key == 7 && !Exile.isExiled(p)
                && WormOfTimePowers.PATHWAY.equals(SoulCore.getPathway(p)) && Beyonder.hasSequence(p, 2));
    }

    static final AbilityWheel WHEEL = AbilityWheel.create("worm_of_time_authority", "Time and Error")
            .radius(90)
            .quickCastCooldown(false).quickCastCost(3f)
            .entry("loophole", WormOfTimePowers.icon("loophole"), "Exploit Loophole", 2, 12000, 400, WormOfTimePowers.guarded(WormOfTimeAuthority::loophole))
            .entry("aging", WormOfTimePowers.icon("aging"), "Steal Lifespan", 1, 18000, 200, WormOfTimePowers.guarded(WormOfTimeAuthority::aging))
            .entry("slow", WormOfTimePowers.icon("slow"), "Slow Time", 1, 16000, 400, WormOfTimePowers.guarded(WormOfTimeAuthority::slow)).notCopyable()
            .entry("stop", WormOfTimePowers.icon("stop"), "Ancient Clock", 1, 30000, 600, WormOfTimePowers.guarded(WormOfTimeAuthority::stop)).notCopyable()
            .entry("haste", WormOfTimePowers.icon("haste"), "Accelerate Time", 1, 16000, 400, WormOfTimePowers.guarded(WormOfTimeAuthority::haste))
            .entry("rewind", WormOfTimePowers.icon("rewind"), "Reverse Time", 1, 24000, 600, WormOfTimePowers.guarded(WormOfTimeAuthority::rewind)).notCopyable()
            .entry("history", WormOfTimePowers.icon("history"), "Curtain of History", 1, 8000, 120, WormOfTimePowers.guarded(WormOfTimeAuthority::history))
            .entry("error", WormOfTimePowers.icon("error"), "Error: Leave Reality", 0, 40000, 600, WormOfTimePowers.guarded(WormOfTimeAuthority::error)).notCopyable()
            .entry("misdirect", WormOfTimePowers.icon("misdirect"), "Error: Misdirection", 0, 35000, 500, WormOfTimePowers.guarded(WormOfTimeAuthority::misdirect));

    public static void authority(Player p, Level level, ServerLevel sl, boolean bypass) {
        if (!WormOfTimePowers.ready(p, bypass, 2)) return;
        // A silenced key may only exploit its escape loophole, never cast other authorities through the hold.
        if (Root.has(p, RootRestriction.ABILITIES) && p instanceof ServerPlayer serverPlayer) {
            boolean quick = p.isShiftKeyDown();
            WHEEL.cast(serverPlayer, WHEEL.entry("loophole"), quick, quick ? 3f : 1f);
            return;
        }
        if (p.isShiftKeyDown()) WHEEL.quickCast(p); else WHEEL.open(p);
    }

    private static AbilityWheel.Result loophole(ServerPlayer p, ServerLevel sl) {
        Unbound.free(p, 60, true);
        WormOfTimeFx.clock(sl, p.position().add(0, 1.5, 0), 1.5f, 30, true);
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result aging(ServerPlayer p, ServerLevel sl) {
        LivingEntity t = WormOfTimePowers.target(p, sl, 40);
        if (t == null) return AbilityWheel.Result.REFUSED;
        float before = t.getHealth();
        Spells.strikeTrue(p, t, 25 * WormOfTimePowers.might(p));
        p.heal(Math.max(0, before - t.getHealth()) * 0.4f);
        if (t.isAlive()) {
            Decay.apply(t, 200, 0.2f);
            t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Spells.holdLength(t, 200), 1));
            t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
        }
        WormOfTimeFx.steal(sl, p, t);
        WormOfTimeFx.clock(sl, t.position().add(0, 1.5, 0), 2, 35, false);
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result slow(ServerPlayer p, ServerLevel sl) {
        Zone.around(p, 12).height(12).lasts(160).every(10)
                .onInside((t, age) -> {
                    t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 15, 2));
                    t.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 15, 1));
                    t.setDeltaMovement(t.getDeltaMovement().scale(0.5));
                }).onPulse(left -> {
                    if (left % 40 == 0) WormOfTimeFx.clock(sl, p.position().add(0, 2, 0), 3, 30, false);
                    for (Projectile shot : Spells.hostileShotsAround(p, sl, p.position(), 12).stream().limit(64).toList())
                        shot.setDeltaMovement(shot.getDeltaMovement().scale(0.6));
                }).open(p, sl, FIELD);
        HudTimer.show(p, FIELD, "Slow Time", 160, 0x72E8DC);
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result stop(ServerPlayer p, ServerLevel sl) {
        Zone.close(p, FIELD);
        UUID owner = p.getUUID();
        Set<UUID> shots = new HashSet<>();
        FIELDS.put(owner, new FieldShots(sl, shots));
        Zone.around(p, 12).height(12).lasts(60).every(1)
                .onInside((t, age) -> {
                    // Shared restrictions honor root immunity and shorter boss/player hold durations.
                    if (age < Spells.holdLength(t, 60)) Root.apply(t, 4, RootRestriction.EVERYTHING);
                    if (!Spells.cannotBeMoved(t) && Root.has(t, RootRestriction.MOVE)) t.setDeltaMovement(Vec3.ZERO);
                }).onPulse(left -> {
                    // Reference counts preserve the original velocity across overlapping clocks.
                    for (Iterator<UUID> it = shots.iterator(); it.hasNext();) {
                        UUID id = it.next();
                        Shot held = SUSPENDED.get(id);
                        if (held == null || held.entity.isRemoved() || held.entity.level() != sl
                                || !Zone.covers(p, FIELD, held.entity.position())) {
                            release(owner, id); it.remove();
                        }
                    }
                    for (Projectile shot : Spells.hostileShotsAround(p, sl, p.position(), 12)) {
                        if (shots.size() >= 64 && !shots.contains(shot.getUUID())) continue;
                        shots.add(shot.getUUID());
                        Shot held = SUSPENDED.computeIfAbsent(shot.getUUID(), id -> {
                            var tag = new net.minecraft.nbt.CompoundTag();
                            tag.putDouble("x", shot.getDeltaMovement().x);
                            tag.putDouble("y", shot.getDeltaMovement().y);
                            tag.putDouble("z", shot.getDeltaMovement().z);
                            tag.putBoolean("gravity", shot.isNoGravity());
                            shot.getPersistentData().put(SHOT_TAG, tag);
                            return new Shot(shot, shot.getDeltaMovement(), shot.isNoGravity(), new HashSet<>());
                        });
                        held.owners.add(owner);
                        shot.setNoGravity(true);
                        shot.setDeltaMovement(Vec3.ZERO);
                        shot.hurtMarked = true;
                    }
                    if (left % 20 == 0) WormOfTimeFx.clock(sl, p.position().add(0, 2.5, 0), 4, 25, false);
                }).onEnd(() -> {
                    for (UUID id : shots) release(owner, id);
                    shots.clear(); FIELDS.remove(owner);
                })
                .open(p, sl, FIELD);
        HudTimer.show(p, FIELD, "Stopped Time", 60, 0x72E8DC);
        return AbilityWheel.Result.DONE;
    }

    private static void restore(Shot shot) {
        if (shot.entity.isRemoved()) return;
        shot.entity.setNoGravity(shot.noGravity);
        shot.entity.setDeltaMovement(shot.velocity);
        shot.entity.hurtMarked = true;
        shot.entity.getPersistentData().remove(SHOT_TAG);
    }

    private static void release(UUID owner, UUID id) {
        Shot shot = SUSPENDED.get(id);
        if (shot == null) return;
        shot.owners.remove(owner);
        if (shot.owners.isEmpty()) { restore(shot); SUSPENDED.remove(id); }
    }

    // Zone's generic unload drops its callbacks. Restore our external state before that happens.
    @SubscribeEvent(priority = Priority.HIGH)
    public static void unload(LevelEvent.Unload event) {
        for (Iterator<Map.Entry<UUID, FieldShots>> it = FIELDS.entrySet().iterator(); it.hasNext();) {
            var field = it.next();
            if (field.getValue().level != event.getLevel()) continue;
            for (UUID id : field.getValue().shots) release(field.getKey(), id);
            it.remove();
        }
        HISTORY.values().removeIf(history -> !history.isEmpty() && history.getLast().level == event.getLevel());
    }

    // A chunk saved during a clock must never leave a projectile permanently suspended after restart.
    @SubscribeEvent
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Projectile shot)
                || !shot.getPersistentData().contains(SHOT_TAG)) return;
        Shot old = SUSPENDED.get(shot.getUUID());
        if (old != null && old.entity == shot) return;
        SUSPENDED.remove(shot.getUUID());
        var tag = shot.getPersistentData().getCompoundOrEmpty(SHOT_TAG);
        shot.setDeltaMovement(new Vec3(tag.getDoubleOr("x", 0), tag.getDoubleOr("y", 0), tag.getDoubleOr("z", 0)));
        shot.setNoGravity(tag.getBooleanOr("gravity", false));
        shot.getPersistentData().remove(SHOT_TAG);
    }

    private static AbilityWheel.Result haste(ServerPlayer p, ServerLevel sl) {
        List<LivingEntity> friends = new ArrayList<>(Spells.alliesAround(p, sl, p.position(), 12));
        if (!friends.contains(p)) friends.add(p);
        for (LivingEntity ally : friends.stream().limit(24).toList()) {
            ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 160, 1));
            ally.addEffect(new MobEffectInstance(MobEffects.HASTE, 160, 1));
        }
        for (LivingEntity enemy : Spells.enemiesAround(p, sl, p.position(), 12).stream().limit(24).toList()) {
            // Accelerated expiry only applies to finite beneficial potion effects, never unrelated saved timers.
            for (MobEffectInstance effect : new ArrayList<>(enemy.getActiveEffects())) {
                if (!effect.getEffect().value().isBeneficial() || effect.isInfiniteDuration()) continue;
                Holder<MobEffect> kind = effect.getEffect();
                int remaining = effect.getDuration() - 100;
                enemy.removeEffect(kind);
                if (remaining > 0) enemy.addEffect(new MobEffectInstance(kind, remaining, effect.getAmplifier(),
                        effect.isAmbient(), effect.isVisible(), effect.showIcon()));
            }
        }
        WormOfTimeFx.clock(sl, p.position().add(0, 2, 0), 3, 35, false);
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result rewind(ServerPlayer p, ServerLevel sl) {
        if (Root.has(p, RootRestriction.TELEPORT)) return WormOfTimePowers.refuse(p, "Your position is bound.");
        Deque<Moment> history = HISTORY.get(p.getUUID());
        Moment then = history == null ? null : history.stream()
                .filter(m -> m.level == sl && sl.getGameTime() - m.tick >= 60).reduce((a, b) -> b).orElse(null);
        if (then == null) return WormOfTimePowers.refuse(p, "Three seconds of uninterrupted history are needed.");
        if (!Spells.fits(p, sl, then.position)) return WormOfTimePowers.refuse(p, "Your earlier position is obstructed.");
        Vec3 from = p.position();
        p.teleportTo(then.position.x, then.position.y, then.position.z);
        p.setHealth(Math.max(p.getHealth(), Math.min(p.getMaxHealth(), then.health)));
        p.setDeltaMovement(Vec3.ZERO);
        FallGuard.protect(p, 100);
        HISTORY.remove(p.getUUID());
        WormOfTimeFx.clock(sl, from.add(0, 1.5, 0), 2, 30, true);
        WormOfTimeFx.clock(sl, then.position.add(0, 1.5, 0), 2, 30, true);
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result error(ServerPlayer p, ServerLevel sl) {
        if (!Exile.banish(p, 60, p.position(), (who, world) -> WormOfTimeFx.burst(world, who.position())))
            return WormOfTimePowers.refuse(p, "You cannot leave reality in this state.");
        WormOfTimeFx.clock(sl, p.position().add(0, 1.5, 0), 2.5f, 40, true);
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result history(ServerPlayer p, ServerLevel sl) {
        Casts.Cast seen = Casts.latestNear(sl, p.position(), 32, 600, cast -> !cast.caster().equals(p.getUUID()));
        if (seen == null) return WormOfTimePowers.refuse(p, "No recent use of power remains in this area's history.");
        p.sendSystemMessage(net.minecraft.network.chat.Component.literal(seen.casterName() + " used " + seen.name()
                + " here " + (sl.getGameTime() - seen.time()) / 20 + " seconds ago."));
        WormOfTimeFx.clock(sl, seen.at().add(0, 1.5, 0), 2, 60, true);
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result misdirect(ServerPlayer p, ServerLevel sl) {
        if (Decree.violate(p, Decree.Rule.STAND_INS)) return WormOfTimePowers.refuse(p, "A stand-in is forbidden here.");
        LivingEntity receiver = WormOfTimePowers.target(p, sl, 32);
        if (receiver == null) return AbilityWheel.Result.REFUSED;
        Substitute.arm(p, 100, 3, (saved, source, amount, world) -> {
            if (receiver.isAlive() && receiver.level() == world && receiver.distanceTo(saved) < 40 && Spells.isEnemy(saved, receiver)) {
                Spells.strike(saved, receiver, Math.min(amount, 150));
                WormOfTimeFx.steal(world, saved, receiver);
            }
        });
        if (!Substitute.isArmed(p)) return WormOfTimePowers.refuse(p, "A stand-in is forbidden here.");
        return AbilityWheel.Result.DONE;
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent.Post event) {
        Player p = event.player();
        if (!(p.level() instanceof ServerLevel sl)) return;
        if (!p.isAlive() || p.isSpectator() || !WormOfTimePowers.PATHWAY.equals(SoulCore.getPathway(p)) || !Beyonder.hasSequence(p, 1)) {
            HISTORY.remove(p.getUUID());
            Zone.close(p, FIELD);
            return;
        }
        if (p.tickCount % 5 != 0) return;
        Deque<Moment> history = HISTORY.computeIfAbsent(p.getUUID(), id -> new ArrayDeque<>());
        if (!history.isEmpty() && history.getLast().level != sl) history.clear();
        history.addLast(new Moment(sl, sl.getGameTime(), p.position(), p.getHealth()));
        while (!history.isEmpty() && sl.getGameTime() - history.getFirst().tick > 100) history.removeFirst();
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { HISTORY.remove(event.getEntity().getUUID()); }
    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent event) { HISTORY.remove(event.getEntity().getUUID()); }
}
