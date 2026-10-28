package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.combat.*;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import java.util.EnumMap;
import java.util.Map;
import static net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.god_of_decay.GodOfDecayFx.Ability.*;

/** Seven keys, 26 abilities. Wheel accounting is exclusively AbilityWheel's responsibility. */
public final class GodOfDecay {
    public static final String PATHWAY = "God Of Decay";
    static final String PREFIX = "god_of_decay_";
    public static final float RECAST_COST = 3f;
    public record Spec(int sequence, int cost, int cooldown) {}
    static final Map<GodOfDecayFx.Ability, Spec> SPECS = new EnumMap<>(GodOfDecayFx.Ability.class);
    static {
        SPECS.put(ULCEROUS_BURST, new Spec(9, 250, 160));
        SPECS.put(CARRIERS_TOUCH, new Spec(9, 250, 400));
        SPECS.put(RED_TAPE, new Spec(8, 600, 240));
        SPECS.put(OFFICIAL_CAPACITY, new Spec(8, 600, 600));
        SPECS.put(CORRUPTION, new Spec(7, 1250, 500));
        SPECS.put(HOLLOW_OUT, new Spec(7, 1250, 360));
        SPECS.put(LAZINESS, new Spec(6, 2450, 280));
        SPECS.put(OBESITY, new Spec(6, 2450, 280));
        SPECS.put(DEGENERATION, new Spec(6, 2450, 280));
        SPECS.put(INCUBATION, new Spec(6, 2450, 400));
        SPECS.put(ROT, new Spec(5, 3000, 360));
        SPECS.put(DIVISION, new Spec(5, 3000, 900));
        SPECS.put(PATHOGEN_FORM, new Spec(4, 6000, 400));
        SPECS.put(CERTAIN_DEATH, new Spec(4, 6000, 800));
        SPECS.put(SURE_HIT, new Spec(4, 6000, 240));
        SPECS.put(DECAY, new Spec(4, 6000, 600));
        SPECS.put(SWOLLEN_FORM, new Spec(4, 6000, 1800));
        SPECS.put(LEFT_HAND_OF_DECAY, new Spec(3, 12000, 1200));
        SPECS.put(WITHERING_PALM, new Spec(3, 9000, 300));
        SPECS.put(GRAVEL_SHOT, new Spec(3, 8000, 200));
        SPECS.put(SLOW_TIME, new Spec(2, 18000, 700));
        SPECS.put(SEGMENTED_TIME, new Spec(2, 18000, 600));
        SPECS.put(SENESCENCE, new Spec(1, 28000, 1200));
        SPECS.put(ENTROPY, new Spec(0, 40000, 1800));
        SPECS.put(VIRUS_INCARNATION, new Spec(0, 40000, 2400));
        SPECS.put(RAGNAROK, new Spec(0, 60000, 4800));
    }
    public static final AbilityWheel DISEASE = wheel("disease", "Disease", LAZINESS, OBESITY, DEGENERATION, INCUBATION, ROT);
    public static final AbilityWheel DOOM = wheel("doom", "Doom", CERTAIN_DEATH, SURE_HIT, DECAY, SWOLLEN_FORM, LEFT_HAND_OF_DECAY, WITHERING_PALM, GRAVEL_SHOT);
    public static final AbilityWheel TIME = wheel("time", "Time and Entropy", SLOW_TIME, SEGMENTED_TIME, SENESCENCE, ENTROPY, VIRUS_INCARNATION, RAGNAROK);
    public static final MeleeCombo COMBO = MeleeCombo.forPathway(PATHWAY)
            .step((p,t,sl) -> GodOfDecayFx.infectedHit(sl,t))
            .step((p,t,sl) -> GodOfDecayFx.weathering(sl,p.position()))
            .step((p,t,sl) -> GodOfDecayMarks.put(p,t,GodOfDecayMarks.Kind.INFECTED,120));
    private GodOfDecay() {}

    private static AbilityWheel wheel(String id, String title, GodOfDecayFx.Ability... abilities) {
        AbilityWheel wheel = AbilityWheel.create(PREFIX + id, title).radius(abilities.length > 6 ? 92 : 80)
                .quickCastCooldown(false).quickCastCost(RECAST_COST);
        for (GodOfDecayFx.Ability ability : abilities) {
            Spec spec = SPECS.get(ability);
            wheel.entry(ability.id(), ability.icon(), label(ability), spec.sequence, spec.cost, spec.cooldown,
                    (p,sl) -> ready(p,spec.sequence,Casts.isCopying(p)) ? GodOfDecayPowers.cast(p,sl,ability) : AbilityWheel.Result.REFUSED)
                    .description(GodOfDecayPowers.description(ability) + " Sneak repeats the last pick for 3x spirituality, ignoring cooldowns.");
            if (ability == SWOLLEN_FORM || ability == VIRUS_INCARNATION || ability == RAGNAROK)
                wheel.free(p -> GodOfDecayPowers.active(p,ability)).notCopyable();
            if (ability == ENTROPY || ability == DECAY || ability == SLOW_TIME) wheel.notCopyable();
        }
        return wheel;
    }
    static String label(GodOfDecayFx.Ability ability) {
        return java.util.Arrays.stream(ability.id().split("_")).map(s -> Character.toUpperCase(s.charAt(0))+s.substring(1))
                .collect(java.util.stream.Collectors.joining(" "));
    }
    public static boolean is(LivingEntity entity, int sequence) {
        return entity instanceof Player p && PATHWAY.equals(SoulCore.getPathway(p)) && Beyonder.hasSequence(p, sequence);
    }
    static boolean ready(Player p, int sequence, boolean borrowed) {
        return !p.level().isClientSide() && p.isAlive() && Beyonder.hasSequence(p,sequence)
                && (borrowed || PATHWAY.equals(SoulCore.getPathway(p)));
    }
    public static void key(Player p, Level level, ServerLevel sl, int key, boolean borrowed) {
        if (!(p instanceof ServerPlayer player)) return;
        GodOfDecayEvents.initialize();
        if (key == 4 || key == 6 || key == 7) {
            if (!ready(p,key == 4 ? 6 : key == 6 ? 4 : 2,borrowed)) return;
            AbilityWheel wheel = key == 4 ? DISEASE : key == 6 ? DOOM : TIME;
            if (p.isShiftKeyDown()) wheel.quickCast(p); else wheel.open(p);
            return;
        }
        GodOfDecayFx.Ability ability = switch(key) {
            case 1 -> p.isShiftKeyDown() ? CARRIERS_TOUCH : ULCEROUS_BURST;
            case 2 -> p.isShiftKeyDown() ? OFFICIAL_CAPACITY : RED_TAPE;
            case 3 -> p.isShiftKeyDown() ? HOLLOW_OUT : CORRUPTION;
            case 5 -> p.isShiftKeyDown() ? PATHOGEN_FORM : DIVISION;
            default -> null;
        };
        if (ability == null) return;
        Spec spec = SPECS.get(ability);
        if (!ready(p,spec.sequence,borrowed)) return;
        // Recasting Division while its doubles stand is a true no-op, even during cooldown.
        if (ability == DIVISION && GodOfDecayPowers.hasDoubles(player)) return;
        String id = PREFIX + ability.id();
        if (!Cooldowns.ready(p,id,label(ability))) return;
        if (SoulCore.getSpirituality(p) < Spells.costFor(p,spec.cost)) { Spells.paySpirituality(p,spec.cost); return; }
        AbilityWheel.Result result = GodOfDecayPowers.cast(player,sl,ability);
        if (result == AbilityWheel.Result.DONE) {
            Spells.paySpirituality(p,spec.cost);
            Cooldowns.start(p,id,spec.cooldown);
        }
    }
}
