package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.apocalypse;

import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.SpellFx;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.status.DeathWard;
import net.lukario.circleofinevitability.status.Decree;
import net.lukario.circleofinevitability.status.StoredDamage;
import net.lukario.circleofinevitability.status.Taunt;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class DemonessTechniques {
    private DemonessTechniques() {}

    static Runnable footsteps(ServerPlayer player, ServerLevel level) {
        Vec3 stride = Vec3.directionFromRotation(0, player.getYRot()).scale(0.16);
        return () -> {
            Apocalypse.leaveMirrorWorld(player, level);
            Mannequin image = new Mannequin(EntityTypes.MANNEQUIN, level) {
                @Override public boolean shouldBeSaved() { return false; }
            };
            image.setComponent(DataComponents.PROFILE, ResolvableProfile.createResolved(player.getGameProfile()));
            image.snapTo(player.position(), player.getYRot(), 0);
            image.setInvulnerable(true);
            level.addFreshEntity(image);
            Spells.sound(level, image.position(), SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 1.6f);
            walk(player, level, image, stride, 60);
        };
    }

    private static void walk(ServerPlayer owner, ServerLevel level, Mannequin image, Vec3 stride, int left) {
        if (left == 0 || !Spells.casterStillHere(owner, level) || !image.isAlive()) {
            image.discard();
            return;
        }
        image.move(MoverType.SELF, stride);
        if (left % 5 == 0) {
            ParticleShapes.burst(level, DemonessFx.GLASS_GLINT, image.position(), 4, 0.02, 0.1);
            for (LivingEntity enemy : Spells.enemiesAround(owner, level, image.position(), 8)) {
                if (enemy instanceof Enemy) Taunt.apply(enemy, image, Math.min(6, left));
            }
        }
        Later.run(level, 1, () -> walk(owner, level, image, stride, left - 1));
    }

    static Runnable wick(ServerPlayer player, ServerLevel level) {
        Vec3 origin = player.getEyePosition();
        var wire = Apocalypse.TRIP_LINES.stream()
                .filter(line -> line.owner == player && line.level == level && !line.snapped && line.expires > level.getGameTime())
                .min(Comparator.comparingDouble(line -> Math.min(origin.distanceToSqr(line.a), origin.distanceToSqr(line.b))))
                .orElse(null);
        if (wire == null) return ApocalypseCombos.refuse(player, "Tie a tripwire first.");
        boolean fromA = origin.distanceToSqr(wire.a) <= origin.distanceToSqr(wire.b);
        Vec3 start = fromA ? wire.a : wire.b;
        Vec3 end = fromA ? wire.b : wire.a;
        if (origin.distanceToSqr(start) > 24 * 24) return ApocalypseCombos.refuse(player, "Move within 24 blocks of a wire end.");
        return () -> {
            Apocalypse.leaveMirrorWorld(player, level);
            int steps = Math.max(1, (int) Math.ceil(start.distanceTo(end)));
            Spells.sound(level, start, SoundEvents.FIRECHARGE_USE, 0.8f, 0.7f);
            Set<UUID> struck = new HashSet<>();
            for (int i = 0; i < steps; i++) {
                Vec3 from = start.lerp(end, (double) i / steps);
                Vec3 to = start.lerp(end, (double) (i + 1) / steps);
                Later.run(level, i, () -> {
                    if (!Spells.casterStillHere(player, level)) return;
                    ParticleShapes.slashBetween(level, DemonessFx.FLAME_STREAK.width(0.25f).lifetime(8), from, to);
                    for (LivingEntity enemy : Spells.enemiesOnLine(player, level, from, to, 0.8)) {
                        if (!struck.add(enemy.getUUID())) continue;
                        ApocalypseMechanics.afflict(player, level, enemy, ApocalypseMechanics.Affliction.FLAME);
                        Spells.strike(player, enemy, 18);
                    }
                });
            }
        };
    }

    static Runnable accounting(ServerPlayer player, ServerLevel level) {
        LivingEntity target = Spells.aimEnemy(player, level, Apocalypse.CURSE_RANGE);
        if (target == null || !StoredDamage.belongsTo(target, player, Apocalypse.CURSE_TAG)) {
            return ApocalypseCombos.refuse(player, "Aim at an enemy carrying your Curse.");
        }
        return () -> {
            Apocalypse.leaveMirrorWorld(player, level);
            float damage = StoredDamage.total(target) * 0.75f;
            StoredDamage.cancel(target);
            DemonessFx.curseRelease(level, target, Apocalypse.CURSE_TAG);
            Spells.sound(level, target.position(), SoundEvents.GLASS_BREAK, 1f, 0.6f);
            StoredDamage.deal(target, player, damage);
        };
    }

    static Runnable tomorrow(ServerPlayer player, ServerLevel level) {
        LivingEntity ally = Spells.aimAlly(player, level, 24);
        if (!DeathWard.isArmedBy(player, Apocalypse.WARD_TAG)) return ApocalypseCombos.refuse(player, "Prepare Unaging first.");
        if (ally == null || DeathWard.isArmed(ally)) return ApocalypseCombos.refuse(player, "Aim at an ally without a death ward.");
        if (Decree.violate(ally, Decree.Rule.RESURRECTION)) return ApocalypseCombos.refuse(player, "Resurrection is forbidden there.");
        return () -> {
            int ticks = Math.min(160, DeathWard.ticksLeft(player));
            if (DeathWard.armWhen(ally, ticks, Apocalypse.UNAGING_HEALTH, DeathWard.GRACE_TICKS,
                    () -> true, Apocalypse::unagingReturn) == null) return;
            DeathWard.disarm(player);
            Apocalypse.leaveMirrorWorld(player, level);
            ParticleShapes.clearModels(level, player, Apocalypse.WARD_TAG);
            DemonessFx.unagingWard(level, ally, ticks, Apocalypse.WARD_TAG);
            Spells.sound(level, ally.position(), SoundEvents.BEACON_ACTIVATE, 0.8f, 1.4f);
        };
    }

    private record Eye(ServerLevel level, Vec3 center, long until, String tag) {}
    private static final Map<UUID, Eye> EYES = new HashMap<>();

    static Runnable eye(ServerPlayer player, ServerLevel level) {
        Vec3 point = Spells.groundNear(player, level, Spells.aimGround(player, level, Apocalypse.DISASTER_RANGE), 2);
        if (point == null) return ApocalypseCombos.refuse(player, "Aim at solid ground for the eye.");
        return () -> {
            Apocalypse.leaveMirrorWorld(player, level);
            clearEye(player);
            String tag = "apocalypse_eye_" + player.getUUID();
            Eye eye = new Eye(level, point, level.getGameTime() + 100, tag);
            EYES.put(player.getUUID(), eye);
            ParticleShapes.model(level, SpellFx.RUNE_CIRCLE.color(0xDE86E8E0).scale(6f).spin(1).lifetime(100).tag(tag),
                    point.add(0, 0.05, 0), 0, 0, 0);
            Spells.sound(level, point, SoundEvents.AMETHYST_BLOCK_RESONATE, 1f, 1.2f);
            Later.run(level, 100, () -> EYES.remove(player.getUUID(), eye));
        };
    }

    static Set<UUID> claimEye(LivingEntity owner, ServerLevel level) {
        Eye eye = clearEye(owner);
        if (eye == null || eye.level != level || eye.until <= level.getGameTime()) return Set.of();
        Set<UUID> protectedIds = new HashSet<>();
        for (LivingEntity entity : Spells.livingAround(owner, level, eye.center, 3)) {
            if (Spells.isFriend(owner, entity) || entity instanceof Player && entity.isShiftKeyDown()) {
                protectedIds.add(entity.getUUID());
                SpellFx.runeCircle(level, entity.position(), 0.7, 0xDE86E8E0, 15, 2);
            }
        }
        return Set.copyOf(protectedIds);
    }

    static Eye clearEye(LivingEntity owner) {
        Eye eye = EYES.remove(owner.getUUID());
        if (eye != null) ParticleShapes.clearModels(eye.level, eye.center, eye.tag);
        return eye;
    }

    static void clearLevel(ServerLevel level) {
        EYES.values().removeIf(eye -> eye.level == level);
    }
}
