package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.dark_angel;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.Cooldowns;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.pathway_fx.HangedManFx;
import net.lukario.circleofinevitability.combat.AbilityCombos.Combo;
import net.lukario.circleofinevitability.combat.AbilityCombos.Input;
import net.lukario.circleofinevitability.combat.Echo;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Teleport;
import net.lukario.circleofinevitability.combat.Trap;
import net.lukario.circleofinevitability.status.Marks;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class DarkAngelCombos {
    static final String REFRAIN = "dark_angel_trinity_refrain";
    private static final String TITHE = "dark_angel_crimson_tithe";
    private static final String PASSAGE = "dark_angel_shepherds_passage";
    private static final String BELL = "dark_angel_carrion_bell";
    static final int ECHO_DELAY = 8;

    public static final List<Combo> ALL = List.of(
            new Combo(DarkAngel.PATHWAY, "dark_angel_hangmans_snare", "Hangman's Snare", 7,
                    Input.CHORD, 1, 3, 900, 120, DarkAngelCombos::snare),
            new Combo(DarkAngel.PATHWAY, "dark_angel_choir_of_flesh", "Choir of Flesh", 6,
                    Input.SEQUENCE, 2, 4, 1500, 160, DarkAngelCombos::choir),
            new Combo(DarkAngel.PATHWAY, BELL, "Carrion Bell", 7,
                    Input.CHORD, 2, 3, 800, 160, DarkAngelCombos::bell),
            new Combo(DarkAngel.PATHWAY, TITHE, "Crimson Tithe", 6,
                    Input.SEQUENCE, 1, 4, 900, 200, DarkAngelCombos::tithe),
            new Combo(DarkAngel.PATHWAY, PASSAGE, "Shepherd's Passage", 5,
                    Input.CHORD, 3, 5, 1800, 200, DarkAngelCombos::passage),
            new Combo(DarkAngel.PATHWAY, "dark_angel_black_procession", "Black Procession", 4,
                    Input.CHORD, 4, 6, 3600, 240, DarkAngelCombos::procession),
            new Combo(DarkAngel.PATHWAY, REFRAIN, "Trinity Refrain", 3,
                    Input.SEQUENCE, 5, 7, 7500, 360, DarkAngelCombos::refrain));

    private static final int SNARE_TICKS = 60;
    private static final int CHOIR_WARNING = 20;
    private static final int CHOIR_WEAKNESS_TICKS = 100;
    private static final double CHOIR_RADIUS = 6;

    private DarkAngelCombos() {}

    private static Runnable snare(ServerPlayer caster, ServerLevel level) {
        LivingEntity target = Spells.enemiesAround(caster, level, caster.position(), DarkAngel.DIVINATION_RADIUS).stream()
                .filter(enemy -> Spells.canBeChosen(caster, enemy))
                .max(Comparator.comparingDouble(LivingEntity::getMaxHealth)
                        .thenComparingDouble(enemy -> -caster.distanceToSqr(enemy))).orElse(null);
        if (target == null) {
            caster.sendOverlayMessage(Component.literal("The pendulum finds nothing."));
            return null;
        }
        return () -> {
            DarkAngel.riseFromShadow(caster, level);
            HangedManFx.divination(level, caster);
            HangedManFx.divinationReveal(level, target, SNARE_TICKS + HangedManFx.BIND_GRIPS);
            HangedManFx.bind(level, target, SNARE_TICKS + HangedManFx.BIND_GRIPS);
            Later.run(level, HangedManFx.BIND_GRIPS, () -> {
                if (target.level() == level && Spells.casterStillHere(caster, level) && Spells.isEnemy(caster, target)) {
                    Root.apply(target, SNARE_TICKS, RootRestriction.MOVEMENT);
                }
            });
        };
    }

    private static Runnable choir(ServerPlayer caster, ServerLevel level) {
        LivingEntity source = Spells.aimEnemy(caster, level, DarkAngel.WHISPERS_RANGE);
        if (source == null) {
            caster.sendOverlayMessage(Component.literal("The choir needs a voice."));
            return null;
        }
        return () -> {
            DarkAngel.riseFromShadow(caster, level);
            HangedManFx.choirVoice(level, caster, source);
            Later.run(level, CHOIR_WARNING, () -> {
                if (!Spells.casterStillHere(caster, level) || source.level() != level || !Spells.isEnemy(caster, source)) return;
                for (LivingEntity enemy : Spells.enemiesAround(caster, level, source.getBoundingBox().getCenter(), CHOIR_RADIUS)) {
                    Spells.keepEffect(enemy, MobEffects.WEAKNESS, 1, CHOIR_WEAKNESS_TICKS);
                    if (enemy != source) HangedManFx.choirVoice(level, source, enemy);
                }
            });
        };
    }

    private static Runnable bell(ServerPlayer caster, ServerLevel level) {
        Vec3 ground = Spells.groundNear(caster, level, Spells.aimGround(caster, level, 20), 1);
        if (ground == null) return refuse(caster, "The bell needs solid ground.");
        return () -> {
            DarkAngel.riseFromShadow(caster, level);
            HangedManFx.bellSet(level, ground);
            Trap.at(ground, 1.5).armsAfter(2).lasts(400).onSpring(first -> {
                HangedManFx.bellRing(level, ground);
                for (LivingEntity enemy : Spells.enemiesAround(caster, level, ground, 6)) {
                    HangedManFx.divinationReveal(level, enemy, 60);
                    Spells.pull(enemy, ground, 0.85, 0.15);
                }
            }).set(caster, level, BELL, 1);
        };
    }

    private static Runnable tithe(ServerPlayer caster, ServerLevel level) {
        LivingEntity target = Spells.aimEnemy(caster, level, DarkAngel.BOMB_RANGE);
        if (target == null) return refuse(caster, "Aim at someone to mark for the tithe.");
        return () -> {
            DarkAngel.riseFromShadow(caster, level);
            Marks.add(target, caster, TITHE, 1, 100);
            HangedManFx.titheMark(level, target, caster, 100);
        };
    }

    static void collectTithe(ServerLevel level, LivingEntity caster, LivingEntity target, float damageDealt) {
        if (damageDealt > 0 && Marks.take(target, caster, TITHE) > 0) {
            HangedManFx.titheEnd(level, target, caster);
            caster.heal(Math.min(12f, damageDealt * 0.25f));
        }
    }

    private static Runnable passage(ServerPlayer caster, ServerLevel level) {
        LivingEntity target = Spells.aimEnemy(caster, level, 16);
        if (target == null) return refuse(caster, "Aim at a shadow to enter.");
        Vec3 feet = landing(caster, level, target);
        if (feet == null) return refuse(caster, "There is no passage behind that target.");
        float before = SoulCore.getSpirituality(caster);
        return () -> {
            float paid = before - SoulCore.getSpirituality(caster);
            DarkAngel.riseFromShadow(caster, level);
            HangedManFx.passageWhisper(level, feet);
            Later.run(level, 4, () -> {
                if (!Spells.casterStillHere(caster, level)) return;
                if (target.level() == level && Spells.isEnemy(caster, target) && caster.distanceTo(target) <= 16
                        && Teleport.of(caster).go(level, landing(caster, level, target))) return;
                Spells.giveSpirituality(caster, paid);
                Cooldowns.reset(caster, PASSAGE);
                caster.sendOverlayMessage(Component.literal("The shadow closed before you could enter."));
            });
        };
    }

    private static Vec3 landing(ServerPlayer caster, ServerLevel level, LivingEntity target) {
        Vec3 behind = Spells.flatLook(target).scale(-1.25 - target.getBbWidth() * 0.5);
        Vec3 feet = Spells.groundNear(caster, level, target.position().add(behind), 1.5);
        if (feet == null || !Teleport.canGo(caster) || !Teleport.fits(caster, level, feet)
                || !level.getWorldBorder().isWithinBounds(BlockPos.containing(feet))) return null;
        return feet;
    }

    private static Runnable procession(ServerPlayer caster, ServerLevel level) {
        Vec3 start = caster.position();
        Vec3 direction = Spells.flatLook(caster);
        float yaw = caster.getYRot();
        List<Vec3> ribs = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Vec3 ground = Spells.groundNear(caster, level, start.add(direction.scale(i * 3)), 2);
            if (ground != null) ribs.add(ground);
        }
        if (ribs.isEmpty()) return refuse(caster, "There is no ground for the procession.");
        return () -> {
            DarkAngel.riseFromShadow(caster, level);
            Set<UUID> struck = new HashSet<>();
            for (int i = 0; i < ribs.size(); i++) {
                Vec3 ground = ribs.get(i);
                Later.run(level, 1 + i * 5, () -> {
                    if (!Spells.casterStillHere(caster, level)) return;
                    HangedManFx.blackRib(level, ground, yaw);
                    for (LivingEntity enemy : Spells.enemiesAround(caster, level, ground.add(0, 0.7, 0), 2.2)) {
                        if (struck.add(enemy.getUUID())) Spells.strike(caster, enemy, 32);
                        Spells.push(enemy, ground, 0, 0.38);
                    }
                });
            }
        };
    }

    private static Runnable refrain(ServerPlayer caster, ServerLevel level) {
        return () -> {
            DarkAngel.riseFromShadow(caster, level);
            Echo.armNext(caster, REFRAIN, 100);
            HangedManFx.trinityEcho(level, caster);
        };
    }

    static void repeat(LivingEntity caster, ServerLevel level, Runnable effect) {
        if (!(caster instanceof ServerPlayer player) || Beyonder.isCastingUnbound(player)) return;
        if (Echo.repeatNext(caster, level, REFRAIN, ECHO_DELAY, 0.5f, effect)) {
            HangedManFx.refrainImage(level, caster);
        }
    }

    private static Runnable refuse(ServerPlayer caster, String reason) {
        caster.sendOverlayMessage(Component.literal(reason));
        return null;
    }
}
