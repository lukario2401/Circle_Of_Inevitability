package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.apocalypse;

import net.lukario.circleofinevitability.combat.AbilityCharge;
import net.lukario.circleofinevitability.combat.AbilityCombos.Combo;
import net.lukario.circleofinevitability.combat.AbilityCombos.Input;
import net.lukario.circleofinevitability.combat.Spells;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.apocalypse.Apocalypse.PATHWAY;

public final class ApocalypseCombos {
    private static final AbilityCharge.Rule LANCE = new AbilityCharge.Rule(120, 40, 400);
    public static final List<Combo> ALL = List.of(
            new Combo(PATHWAY, "apocalypse_obsidian_lance", "Obsidian Lance", 7, Input.CHORD, 2, 3,
                    400, 160, LANCE, player -> DemonessFx.lanceCharge(player.level(), player), ApocalypseCombos::lance),
            new Combo(PATHWAY, "apocalypse_widows_crossing", "Widow's Crossing", 6, Input.CHORD, 1, 4,
                    1100, 200, ApocalypseCombos::crossing),
            new Combo(PATHWAY, "apocalypse_fever_thread", "Fever Thread", 5, Input.SEQUENCE, 4, 5,
                    2200, 240, ApocalypseCombos::fever),
            new Combo(PATHWAY, "apocalypse_mirror_wake", "Mirror Wake", 4, Input.CHORD, 3, 6,
                    3200, 200, ApocalypseCombos::wake),
            new Combo(PATHWAY, "apocalypse_omen_of_ruin", "Omen of Ruin", 3, Input.SEQUENCE, 5, 7,
                    6000, 360, ApocalypseCombos::omen),
            new Combo(PATHWAY, "apocalypse_false_footsteps", "False Footsteps", 7, Input.CHORD, 1, 3,
                    700, 200, DemonessTechniques::footsteps),
            new Combo(PATHWAY, "apocalypse_black_wick", "Black Wick", 6, Input.CHORD, 2, 4,
                    1200, 200, DemonessTechniques::wick),
            new Combo(PATHWAY, "apocalypse_cruel_accounting", "Cruel Accounting", 5, Input.SEQUENCE, 2, 5,
                    1800, 240, DemonessTechniques::accounting),
            new Combo(PATHWAY, "apocalypse_borrowed_tomorrow", "Borrowed Tomorrow", 4, Input.SEQUENCE, 1, 6,
                    3500, 480, DemonessTechniques::tomorrow),
            new Combo(PATHWAY, "apocalypse_eye_of_the_storm", "Eye of the Storm", 3, Input.CHORD, 6, 7,
                    5000, 400, DemonessTechniques::eye));

    private ApocalypseCombos() {}

    private static Runnable lance(ServerPlayer player, ServerLevel level, AbilityCharge charge) {
        int tier = charge.ticks() / LANCE.intervalTicks();
        Vec3 from = player.getEyePosition();
        Vec3 end = Spells.aimPoint(player, level, 28);
        List<LivingEntity> hits = Spells.enemiesOnLine(player, level, from, end, 0.18).stream().limit(tier + 1).toList();
        return () -> {
            Apocalypse.leaveMirrorWorld(player, level);
            Vec3 to = hits.size() == tier + 1 ? hits.getLast().getBoundingBox().getCenter() : end;
            DemonessFx.blackFlameBolt(level, from, to);
            Spells.sound(level, from, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.2f);
            for (LivingEntity hit : hits) {
                ApocalypseMechanics.afflict(player, level, hit, ApocalypseMechanics.Affliction.FLAME);
                Spells.strike(player, hit, 18 + 12 * tier);
                DemonessFx.blackFlameHit(level, hit.getBoundingBox().getCenter());
            }
        };
    }

    private static Runnable crossing(ServerPlayer player, ServerLevel level) {
        Apocalypse.Shard anchor = Apocalypse.shardsOf(player, level).stream()
                .min(Comparator.comparingDouble(shard -> shard.ground.distanceToSqr(player.position()))).orElse(null);
        Vec3 ground = Spells.groundNear(player, level, Spells.aimGround(player, level, 24), 1.5);
        if (anchor == null || ground == null) return refuse(player, "Aim at solid ground near one of your mirrors.");
        Vec3 start = anchor.ground.add(0, 0.6, 0);
        Vec3 end = ground.add(0, 0.6, 0);
        double length = start.distanceTo(end);
        if (length < 1 || length > 24 || !Spells.clearLine(player, level, start, end)) {
            return refuse(player, "The thread needs a clear span of 1 to 24 blocks.");
        }
        return () -> {
            Apocalypse.leaveMirrorWorld(player, level);
            Apocalypse.widowLine(player, level, anchor, end);
        };
    }

    private static Runnable fever(ServerPlayer player, ServerLevel level) {
        LivingEntity target = Spells.aimEnemy(player, level, Apocalypse.PLAGUE_RANGE);
        if (target == null || !ApocalypseMechanics.hasSeal(player, target, ApocalypseMechanics.Affliction.SILK)) {
            return refuse(player, "Aim at an enemy carrying your silk seal.");
        }
        return () -> {
            Apocalypse.leaveMirrorWorld(player, level);
            DemonessFx.plagueJump(level, player, target);
            Apocalypse.plagueOutbreak(player, level, 20).passesTo(1).reach(6).generations(2).maxVictims(2)
                    .start(player, level, target);
        };
    }

    private static Runnable wake(ServerPlayer player, ServerLevel level) {
        Vec3 from = player.getEyePosition();
        Vec3 ray = Spells.aimPoint(player, level, 24).subtract(from);
        Vec3 aim = ray.normalize();
        Apocalypse.Shard aimed = Apocalypse.shardsOf(player, level).stream().filter(shard -> {
            Vec3 delta = shard.ground.add(0, 0.9, 0).subtract(from);
            double along = delta.dot(aim);
            return along >= 0 && along <= ray.length() && delta.subtract(aim.scale(along)).lengthSqr() <= 0.9 * 0.9;
        }).min(Comparator.comparingDouble(shard -> shard.ground.distanceToSqr(from))).orElse(null);
        if (aimed == null || aimed.charge <= 0) return refuse(player, "Aim at one of your charged mirror shards.");
        return () -> {
            Apocalypse.leaveMirrorWorld(player, level);
            float spent = aimed.charge * 0.5f;
            aimed.charge -= spent;
            Vec3 center = aimed.ground.add(0, 0.9, 0);
            Vec3 direction = Vec3.directionFromRotation(0, aimed.yaw);
            DemonessFx.shardCharge(level, aimed.ground, aimed.yaw, aimed.charge, aimed.tag);
            DemonessFx.slivers(level, center, center.add(direction), 70, 16, 10);
            Spells.sound(level, center, SoundEvents.GLASS_BREAK, 0.8f, 1.2f);
            for (LivingEntity enemy : Spells.enemiesInCone(player, level, center, direction, 10, 35)) {
                if (Spells.clearLine(player, level, center, enemy.getEyePosition())) Spells.strike(player, enemy, 20 + 60 * spent);
            }
        };
    }

    private static Runnable omen(ServerPlayer player, ServerLevel level) {
        LivingEntity enemy = Spells.aimEnemy(player, level, 24);
        if (enemy == null || !ApocalypseMechanics.hasDespair(player, enemy)) return refuse(player, "Aim at your unharvested Despair.");
        return () -> {
            Apocalypse.leaveMirrorWorld(player, level);
            ApocalypseMechanics.claimOmen(player, level, enemy);
        };
    }

    static Runnable refuse(ServerPlayer player, String reason) {
        player.sendOverlayMessage(Component.literal(reason));
        return null;
    }
}
