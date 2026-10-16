package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortuneFx.Poem;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.Blind;
import net.lukario.circleofinevitability.status.Fear;
import net.lukario.circleofinevitability.status.GiantForm;
import net.lukario.circleofinevitability.status.Marks;
import net.lukario.circleofinevitability.status.Silence;
import net.lukario.circleofinevitability.status.Slumber;
import net.lukario.circleofinevitability.status.Taunt;
import net.lukario.circleofinevitability.status.Vulnerable;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Knight Of Misfortune, key 2: the Midnight Poem wheel (Midnight Poet, Sequence 8). docs/KNIGHT_OF_MISFORTUNE.md,
 * "Key 2 wheel".
 *
 *   Tranquilize  you and your allies are calmed (fear, nausea, blindness, darkness and taunts end); the one you look
 *                at shows what is wrong with it (its effects, its health, a Giant's madness)
 *   Lullaby      a cone of sleep: enemies in it fall asleep (status/Slumber) and wake when hurt; a player who
 *                sneaks (cogitates) stays awake but blank: no abilities, no sprint
 *   Pacify       the one you look at loses the will to fight: it cannot attack or use abilities and crawls
 *                (Slumber LIMP); blows do not end it
 *   Fear         a cone of dread (status/Fear): mobs run, players are slowed and cannot sprint toward you
 *   Agitating    (Soul Assurer, 6) the one you look at is stirred: a mob goes after whatever is nearest, friend or
 *                foe; it takes more damage from everything; what is wrong with it shows
 *
 * Every poem is heard: whoever heard a poem from you in the last HEARD_TICKS is held POEM_REPEAT longer by the next one
 * (up to HEARD_MOST times). A player who sneaks when it lands shakes it off in half the time. Bosses only hear Fear
 * and Tranquilize. From Nightmare (7) a poem costs RESONANCE_REFUND less (given back after the cast) and needs no voice:
 * it can be cast while silenced (Silence.addCastsThrough, in KnightOfMisfortune, and the wheel's castsWhileSilenced).
 * From Horror Bishop (3) Lullaby and Pacify reach CITY_RANGE all round you.
 *
 * Sneak casts the last poem again without the wheel (no cooldown, double the cost), as for the other wheels.
 * The looks are KnightOfMisfortuneFx's; every number is right below.
 */
public final class KnightOfMisfortunePoems {

    private KnightOfMisfortunePoems() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int POEM_SEQUENCE = 8;                          // the key
    private static final float QUICK_CAST_COST = 2f;

    private static final int TRANQUILIZE_SEQUENCE = 8;
    private static final int TRANQUILIZE_COST = 300;
    private static final int TRANQUILIZE_COOLDOWN = 200;
    private static final double TRANQUILIZE_RADIUS = 10.0;
    private static final double TRANQUILIZE_LOOK = 16.0;

    private static final int LULLABY_SEQUENCE = 8;
    private static final int LULLABY_COST = 300;
    private static final int LULLABY_COOLDOWN = 600;
    private static final double LULLABY_RANGE = 8.0;
    private static final double LULLABY_ANGLE = 30.0;
    private static final int LULLABY_TICKS = 160;
    private static final int LULLABY_RESISTED = 60;              // a player who shakes it off: blank this long

    private static final int PACIFY_SEQUENCE = 8;
    private static final int PACIFY_COST = 300;
    private static final int PACIFY_COOLDOWN = 600;
    private static final double PACIFY_RANGE = 16.0;
    private static final int PACIFY_TICKS = 160;

    private static final int FEAR_SEQUENCE = 8;
    private static final int FEAR_COST = 300;
    private static final int FEAR_COOLDOWN = 400;
    private static final double FEAR_RANGE = 12.0;
    private static final double FEAR_ANGLE = 35.0;
    private static final int FEAR_TICKS = 120;

    private static final int AGITATE_SEQUENCE = 6;
    private static final int AGITATE_COST = 1500;
    private static final int AGITATE_COOLDOWN = 600;
    private static final double AGITATE_RANGE = 16.0;
    private static final int AGITATE_TICKS = 200;
    private static final float AGITATE_VULNERABLE = 0.15f;       // it takes 15% more from everything
    private static final double AGITATE_NEAREST = 12.0;          // a mob goes after the nearest thing this close

    private static final int RESONANCE_SEQUENCE = 7;             // Nightmare: poems need no voice (cast while silenced)
    private static final int CITY_SEQUENCE = 3;                  // Horror Bishop: Lullaby and Pacify reach a whole city...
    private static final double CITY_RANGE = 40.0;               // ...all round you

    private static final int HEARD_TICKS = 600;                  // a poem heard within this long...
    private static final float POEM_REPEAT = 0.25f;              // ...makes the next one hold this much longer...
    private static final int HEARD_MOST = 4;                     // ...up to this many times over
    private static final String HEARD = "knight_of_misfortune_poem_heard";

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("knight_of_misfortune_poems", "Midnight Poem")
            .radius(72)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .entry("tranquilize", KnightOfMisfortuneFx.WHEEL_TRANQUILIZE, "Tranquilize", TRANQUILIZE_SEQUENCE,
                    TRANQUILIZE_COST, TRANQUILIZE_COOLDOWN, KnightOfMisfortunePoems::tranquilize)
            .entry("lullaby", KnightOfMisfortuneFx.WHEEL_LULLABY, "Lullaby", LULLABY_SEQUENCE,
                    LULLABY_COST, LULLABY_COOLDOWN, KnightOfMisfortunePoems::lullaby)
            .entry("pacify", KnightOfMisfortuneFx.WHEEL_PACIFY, "Pacify", PACIFY_SEQUENCE,
                    PACIFY_COST, PACIFY_COOLDOWN, KnightOfMisfortunePoems::pacify)
            .entry("fear", KnightOfMisfortuneFx.WHEEL_FEAR, "Fear", FEAR_SEQUENCE,
                    FEAR_COST, FEAR_COOLDOWN, KnightOfMisfortunePoems::fear)
            .entry("agitating", KnightOfMisfortuneFx.WHEEL_AGITATING, "Agitating", AGITATE_SEQUENCE,
                    AGITATE_COST, AGITATE_COOLDOWN, KnightOfMisfortunePoems::agitating)
            .castsWhileSilenced(player -> KnightOfMisfortune.isKnight(player, RESONANCE_SEQUENCE))   // resonated: no voice
            .onCast(KnightOfMisfortunePoems::resonance);

    // =========================
    // Ability 2 - Midnight Poem
    // Normal: the poem wheel. Sneak: the last poem again, without the wheel.
    // =========================
    public static void knightOfMisfortuneMidnightPoem(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!KnightOfMisfortune.ready(player, bypassClassCheck, POEM_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    /** Resonance (Nightmare, 7): a part of every poem's price comes back. */
    private static void resonance(ServerPlayer player, ServerLevel sl, AbilityWheel.Entry entry) {
        if (!KnightOfMisfortune.isKnight(player, RESONANCE_SEQUENCE)) return;
        float max = Beyonder.maxSpirituality(player);
        float back = entry.cost() * KnightOfMisfortune.RESONANCE_REFUND;
        SoulCore.setSpiritualityQuietly(player, Math.min(max, SoulCore.getSpirituality(player) + back));
    }

    // =========================
    // The poems
    // =========================

    private static AbilityWheel.Result tranquilize(ServerPlayer player, ServerLevel sl) {
        KnightOfMisfortuneFx.recite(sl, player, Poem.TRANQUILIZE, null);
        LivingEntity looked = Spells.aimEnemy(player, sl, TRANQUILIZE_LOOK);
        Later.run(sl, KnightOfMisfortuneFx.POEM_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            KnightOfMisfortuneFx.tranquilize(sl, player, TRANQUILIZE_RADIUS);
            List<LivingEntity> calmed = new ArrayList<>(Spells.alliesAround(player, sl, player.position(), TRANQUILIZE_RADIUS));
            calmed.add(player);
            for (LivingEntity ally : calmed) {
                ally.removeEffect(MobEffects.NAUSEA);
                ally.removeEffect(MobEffects.BLINDNESS);
                ally.removeEffect(MobEffects.DARKNESS);
                Blind.remove(ally);
                Fear.end(ally);
                if (ally instanceof Mob mob) Taunt.remove(mob);
                KnightOfMisfortuneFx.tranquilized(sl, ally);
            }
            if (looked != null && looked.isAlive()) {
                KnightOfMisfortuneFx.tranquilizeReveal(sl, looked);
                reveal(player, looked);
            }
        });
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result lullaby(ServerPlayer player, ServerLevel sl) {
        KnightOfMisfortuneFx.recite(sl, player, Poem.LULLABY, null);
        KnightOfMisfortuneFx.lullaby(sl, player, LULLABY_RANGE);
        Vec3 eyes = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        boolean city = Beyonder.hasSequence(player, CITY_SEQUENCE);
        Later.run(sl, KnightOfMisfortuneFx.POEM_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            List<LivingEntity> hearers = city ? Spells.enemiesAround(player, sl, player.position(), CITY_RANGE)
                    : Spells.enemiesInCone(player, sl, eyes, look, LULLABY_RANGE, LULLABY_ANGLE);
            for (LivingEntity hearer : hearers) {
                if (KnightOfMisfortune.isBoss(hearer)) continue;
                int ticks = heard(hearer, LULLABY_TICKS);
                if (hearer instanceof Player listener && listener.isShiftKeyDown()) {     // cogitating: awake, but blank
                    Silence.apply(hearer, LULLABY_RESISTED);
                    hearer.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, LULLABY_RESISTED, 0, false, false));
                    hearer.setSprinting(false);
                    continue;
                }
                if (Slumber.put(hearer, ticks, Slumber.Kind.SLEEP)) KnightOfMisfortune.showSlumber(sl, hearer, ticks);
            }
        });
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result pacify(ServerPlayer player, ServerLevel sl) {
        if (Beyonder.hasSequence(player, CITY_SEQUENCE)) return pacifyCity(player, sl);
        LivingEntity target = Spells.aimEnemy(player, sl, PACIFY_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("No one hears you."));
            return AbilityWheel.Result.REFUSED;
        }
        if (KnightOfMisfortune.isBoss(target)) {
            player.sendSystemMessage(Component.literal(target.getName().getString() + " is too great to be pacified."));
            return AbilityWheel.Result.REFUSED;
        }
        KnightOfMisfortuneFx.recite(sl, player, Poem.PACIFY, null);
        int ticks = heard(target, PACIFY_TICKS);
        KnightOfMisfortuneFx.pacify(sl, player, target, ticks, "km_pacify_" + target.getId());
        Later.run(sl, KnightOfMisfortuneFx.POEM_LANDS, () -> {
            if (!target.isAlive()) return;
            int held = target instanceof Player listener && listener.isShiftKeyDown() ? ticks / 2 : ticks;
            if (Slumber.put(target, held, Slumber.Kind.LIMP)) KnightOfMisfortune.showSlumber(sl, target, held);
        });
        return AbilityWheel.Result.DONE;
    }

    /** Horror Bishop (3): every enemy within CITY_RANGE (not bosses) is pacified. */
    private static AbilityWheel.Result pacifyCity(ServerPlayer player, ServerLevel sl) {
        List<LivingEntity> hearers = Spells.enemiesAround(player, sl, player.position(), CITY_RANGE);
        hearers.removeIf(KnightOfMisfortune::isBoss);
        if (hearers.isEmpty()) {
            player.sendSystemMessage(Component.literal("No one hears you."));
            return AbilityWheel.Result.REFUSED;
        }
        KnightOfMisfortuneFx.recite(sl, player, Poem.PACIFY, null);
        for (LivingEntity target : hearers) {
            int ticks = heard(target, PACIFY_TICKS);
            KnightOfMisfortuneFx.pacify(sl, player, target, ticks, "km_pacify_" + target.getId());
            Later.run(sl, KnightOfMisfortuneFx.POEM_LANDS, () -> {
                if (!target.isAlive()) return;
                int held = target instanceof Player listener && listener.isShiftKeyDown() ? ticks / 2 : ticks;
                if (Slumber.put(target, held, Slumber.Kind.LIMP)) KnightOfMisfortune.showSlumber(sl, target, held);
            });
        }
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result fear(ServerPlayer player, ServerLevel sl) {
        KnightOfMisfortuneFx.recite(sl, player, Poem.FEAR, null);
        KnightOfMisfortuneFx.fear(sl, player, FEAR_RANGE);
        Vec3 eyes = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Later.run(sl, KnightOfMisfortuneFx.POEM_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            for (LivingEntity hearer : Spells.enemiesInCone(player, sl, eyes, look, FEAR_RANGE, FEAR_ANGLE)) {
                int ticks = heard(hearer, FEAR_TICKS);
                if (KnightOfMisfortune.isBoss(hearer)) ticks /= 2;
                if (hearer instanceof Player listener && listener.isShiftKeyDown()) ticks /= 2;
                if (Fear.apply(hearer, player, ticks)) KnightOfMisfortune.showFear(sl, hearer, ticks);
            }
        });
        return AbilityWheel.Result.DONE;
    }

    private static AbilityWheel.Result agitating(ServerPlayer player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, AGITATE_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("No one hears you."));
            return AbilityWheel.Result.REFUSED;
        }
        if (KnightOfMisfortune.isBoss(target)) {
            player.sendSystemMessage(Component.literal(target.getName().getString() + " does not hear such small words."));
            return AbilityWheel.Result.REFUSED;
        }
        KnightOfMisfortuneFx.recite(sl, player, Poem.AGITATING, null);
        int ticks = heard(target, AGITATE_TICKS);
        KnightOfMisfortuneFx.agitate(sl, player, target, ticks, "km_agitate_" + target.getId());
        Later.run(sl, KnightOfMisfortuneFx.POEM_LANDS, () -> {
            if (!target.isAlive() || !Spells.casterStillHere(player, sl)) return;
            int held = target instanceof Player listener && listener.isShiftKeyDown() ? ticks / 2 : ticks;
            Vulnerable.add(target, AGITATE_VULNERABLE, AGITATE_VULNERABLE, held);
            if (target instanceof Mob mob) {
                LivingEntity nearest = null;
                double best = AGITATE_NEAREST * AGITATE_NEAREST;
                for (LivingEntity other : sl.getEntitiesOfClass(LivingEntity.class, mob.getBoundingBox().inflate(AGITATE_NEAREST))) {
                    if (other == mob || other == player || !other.isAlive() || other.isSpectator()) continue;
                    double d = other.distanceToSqr(mob);
                    if (d < best) {
                        best = d;
                        nearest = other;
                    }
                }
                if (nearest != null) Taunt.apply(mob, nearest, held);
            }
            reveal(player, target);
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Helpers
    // =========================

    /** How long a poem holds 'hearer' (longer for each poem it heard lately), and it has heard one more now. */
    private static int heard(LivingEntity hearer, int ticks) {
        int times = Marks.count(hearer, HEARD);
        Marks.add(hearer, HEARD, HEARD_MOST, HEARD_TICKS);
        return Math.round(ticks * (1f + POEM_REPEAT * Math.min(times, HEARD_MOST)));
    }

    /** What is wrong with it, in chat: its health, every effect on it, a Giant's madness. */
    private static void reveal(Player player, LivingEntity target) {
        StringBuilder line = new StringBuilder(target.getName().getString())
                .append(": health ").append(Math.round(target.getHealth())).append(" of ").append(Math.round(target.getMaxHealth()));
        for (MobEffectInstance effect : target.getActiveEffects()) {
            line.append(", ").append(effect.getEffect().value().getDisplayName().getString());
            if (effect.getAmplifier() > 0) line.append(" ").append(effect.getAmplifier() + 1);
        }
        if (GiantForm.isGiant(target)) line.append(", madness ").append(Math.round(GiantForm.madness(target) * 100)).append("%");
        if (Slumber.kind(target) != null) line.append(", asleep");
        if (Fear.isAfraid(target)) line.append(", afraid");
        player.sendSystemMessage(Component.literal(line.toString()));
    }
}
