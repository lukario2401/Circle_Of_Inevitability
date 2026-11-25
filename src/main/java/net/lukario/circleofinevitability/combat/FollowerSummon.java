package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.particles.fx.ModelFx;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** A reusable model that follows its caster and strikes the nearest visible enemy from its hover point. */
@Mod.EventBusSubscriber(modid = CircleOfInevitability.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public record FollowerSummon(ModelFx model, Vec3 offset, double range, int cooldownTicks, float damage, int durationTicks) {
    private record Slot(UUID owner, String name) {}
    private static final Map<Slot, Active> ACTIVE = new HashMap<>();

    public FollowerSummon {
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(offset, "offset");
        if (!Double.isFinite(offset.x) || !Double.isFinite(offset.y) || !Double.isFinite(offset.z))
            throw new IllegalArgumentException("Hover offset must be finite");
        if (!Double.isFinite(range) || range <= 0 || !Float.isFinite(damage) || damage <= 0
                || cooldownTicks < 1 || durationTicks < 1)
            throw new IllegalArgumentException("Range, damage, cooldown and duration must be positive");
    }

    /** Keeps a drone just beside the caster, even while they sprint, turn or teleport. */
    public static FollowerSummon drone(ModelFx model) {
        return hover(model, new Vec3(0.65, 1.55, 0));
    }

    /** Fixed world-axis offset in blocks from the caster's feet. The client follows between server ticks. */
    public static FollowerSummon hover(ModelFx model, Vec3 offset) {
        return new FollowerSummon(model, offset, 3, 20, 4f, 200);
    }

    public FollowerSummon strikes(double range, int cooldownTicks, float damage) {
        return new FollowerSummon(model, offset, range, cooldownTicks, damage, durationTicks);
    }

    public FollowerSummon lasts(int ticks) {
        return new FollowerSummon(model, offset, range, cooldownTicks, damage, ticks);
    }

    /** A recast replaces only this owner's follower with the same name. */
    public void summon(LivingEntity owner, String name) {
        if (!(owner.level() instanceof ServerLevel level) || !Spells.casterStillHere(owner, level) || owner.isSpectator()) return;
        Slot slot = new Slot(owner.getUUID(), Objects.requireNonNull(name));
        dismiss(owner, name);
        Active summon = new Active(this, owner, level);
        ACTIVE.put(slot, summon);
        level.getChunkSource().sendToTrackingPlayersAndSelf(owner, summon.appearance());
    }

    public static void dismiss(LivingEntity owner, String name) {
        if (!(owner.level() instanceof ServerLevel)) return;
        Active summon = ACTIVE.remove(new Slot(owner.getUUID(), name));
        if (summon != null) summon.clearModel();
    }

    private static final class Active {
        final FollowerSummon spell;
        final LivingEntity owner;
        final ServerLevel level;
        final String tag = "follower_" + UUID.randomUUID();
        final long expires;
        final Runnable attack;
        long nextScan;

        Active(FollowerSummon spell, LivingEntity owner, ServerLevel level) {
            this.spell = spell;
            this.owner = owner;
            this.level = level;
            expires = level.getGameTime() + spell.durationTicks;
            attack = Echo.carry(this::strikeNearest);
        }

        Vec3 position() {
            return owner.position().add(spell.offset);
        }

        ClientboundLevelParticlesPacket packet(ModelFx fx) {
            Vec3 at = position();
            return new ClientboundLevelParticlesPacket(fx, true, true, at.x, at.y, at.z, 0, 0, 0, 0, 0);
        }

        ClientboundLevelParticlesPacket appearance() {
            int remaining = (int) (expires - level.getGameTime());
            return packet(spell.model.following(owner, spell.offset).turning().tag(tag).lifetime(remaining));
        }

        void clearModel() {
            level.getChunkSource().sendToTrackingPlayersAndSelf(owner, packet(ModelFx.clearing(owner, tag)));
        }

        void strikeNearest() {
            Vec3 at = position();
            LivingEntity nearest = null;
            double best = Double.POSITIVE_INFINITY;
            for (LivingEntity enemy : Spells.enemiesAround(owner, level, at, spell.range)) {
                if (enemy.isInvulnerable() || enemy instanceof Player target
                        && (target.isCreative() || owner instanceof Player caster && !caster.canHarmPlayer(target))) continue;
                double distance = Spells.distanceToBody(enemy, at);
                if (distance < best && Spells.canBeChosen(owner, enemy)
                        && Spells.clearLine(owner, level, at, enemy.getBoundingBox().getCenter())) {
                    nearest = enemy;
                    best = distance;
                }
            }
            if (nearest == null) {
                nextScan = level.getGameTime() + 4;
                return;
            }
            nextScan = level.getGameTime() + spell.cooldownTicks;
            Spells.strike(owner, nearest, spell.damage);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.LevelTickEvent.Post event) {
        if (!(event.level() instanceof ServerLevel level) || ACTIVE.isEmpty()) return;
        long now = level.getGameTime();
        // A hit can dismiss or replace a follower while damage events are running.
        for (var entry : List.copyOf(ACTIVE.entrySet())) {
            Active summon = entry.getValue();
            if (summon.level != level || ACTIVE.get(entry.getKey()) != summon) continue;
            if (now >= summon.expires || !Spells.casterStillHere(summon.owner, level) || summon.owner.isSpectator()) {
                ACTIVE.remove(entry.getKey());
                summon.clearModel();
            } else if (now >= summon.nextScan) {
                summon.attack.run();
            }
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer viewer) || ACTIVE.isEmpty()) return;
        for (Active summon : ACTIVE.values()) {
            if (summon.owner == event.getTarget() && summon.expires > summon.level.getGameTime())
                viewer.connection.send(summon.appearance());
        }
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) ACTIVE.values().removeIf(summon -> summon.level == level);
    }
}
