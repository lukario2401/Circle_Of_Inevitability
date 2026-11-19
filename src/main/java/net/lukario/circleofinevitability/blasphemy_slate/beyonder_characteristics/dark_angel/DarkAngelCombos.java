package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.dark_angel;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.pathway_fx.HangedManFx;
import net.lukario.circleofinevitability.combat.AbilityCombos.Combo;
import net.lukario.circleofinevitability.combat.AbilityCombos.Input;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.Comparator;
import java.util.List;

public final class DarkAngelCombos {
    public static final List<Combo> ALL = List.of(
            new Combo(DarkAngel.PATHWAY, "dark_angel_hangmans_snare", "Hangman's Snare", 7,
                    Input.CHORD, 1, 3, 900, 120, DarkAngelCombos::snare),
            new Combo(DarkAngel.PATHWAY, "dark_angel_choir_of_flesh", "Choir of Flesh", 6,
                    Input.SEQUENCE, 2, 4, 1500, 160, DarkAngelCombos::choir));

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
}
