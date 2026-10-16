package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortuneFx.Status;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.ConjuredWeapon;
import net.lukario.circleofinevitability.status.Decay;
import net.lukario.circleofinevitability.status.GiantForm;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.NoHeal;
import net.lukario.circleofinevitability.status.Nocturnality;
import net.lukario.circleofinevitability.status.Slumber;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Knight Of Misfortune, key 6: the Nightwatcher wheel (Nightwatcher, Sequence 4). docs/KNIGHT_OF_MISFORTUNE.md,
 * "Key 6 wheel".
 *
 *   Night Domain        a tide of Darkness DOMAIN_RADIUS round where you stand, DOMAIN_TICKS: it is night inside
 *                       (status/Nocturnality), you and your allies are strengthened, your spirits fight harder; enemies
 *                       rot (status/Decay: corroded gear, less vitality the longer they stay) and gain a level of
 *                       Misfortune every DOMAIN_CURSE_EVERY (up to DOMAIN_CURSE_MOST). Pick it again to lift it.
 *   Hair Entanglement   a cone of hair: enemies caught are bound (root/Root) for HAIR_BIND; whoever is not hurt
 *                       while held falls into Slumber when the hairs let go.
 *   Curse of Misfortune the one you look at carries CURSE_LEVEL levels of Misfortune (status/Misfortune).
 *                       From Knight of Misfortune (1) it is Misfortune: an area where you aim, every enemy in it gets
 *                       MISFORTUNE_START levels at once and one more every MISFORTUNE_EVERY. From Darkness (0) it is
 *                       Catastrophe: the worst mishap is a disaster. (Both cost more, and have a cooldown of their own.)
 *   Demonic Wolf Form   your Mythical Creature Form (status/GiantForm): an eight-legged wolf, stronger, longer reach, a
 *                       tearing bite; WOLF_UPKEEP a second and madness rising. Bigger at Horror Bishop (3), where allies
 *                       who sneak near you are healed; the complete wolf, with no madness, at Servant of Concealment (2).
 *                       Pick it again to leave it.
 *
 * Sneak casts the last pick again without the wheel (no cooldown, double the cost), as for the other wheels.
 * The looks are KnightOfMisfortuneFx's; every number is right below.
 */
public final class KnightOfMisfortuneNightwatcher {

    private KnightOfMisfortuneNightwatcher() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int NIGHTWATCHER_SEQUENCE = 4;                  // the key
    private static final float QUICK_CAST_COST = 2f;

    private static final int DOMAIN_SEQUENCE = 4;
    private static final int DOMAIN_COST = 8000;
    private static final int DOMAIN_COOLDOWN = 1800;
    private static final double DOMAIN_RADIUS = 20.0;
    private static final int DOMAIN_TICKS = 800;
    private static final float DOMAIN_ROT = 0.05f;               // enemies lose this much maximum health at once...
    private static final float DOMAIN_ROT_PER_SECOND = 0.01f;    // ...and this much more each second they stay
    private static final int DOMAIN_CURSE_EVERY = 60;
    private static final int DOMAIN_CURSE_MOST = 3;
    private static final int DOMAIN_CURSE_TICKS = 400;

    private static final int HAIR_SEQUENCE = 4;
    private static final int HAIR_COST = 4500;
    private static final int HAIR_COOLDOWN = 400;
    private static final double HAIR_RANGE = 8.0;
    private static final double HAIR_ANGLE = 35.0;
    private static final int HAIR_BIND = 80;
    private static final int HAIR_SLEEP = 80;

    private static final int CURSE_SEQUENCE = 4;
    private static final int CURSE_COST = 4000;
    private static final int CURSE_COOLDOWN = 400;
    private static final double CURSE_RANGE = 32.0;
    private static final int CURSE_LEVEL = 3;
    private static final int CURSE_TICKS = 400;
    // Knight of Misfortune (1): Misfortune; Darkness (0): Catastrophe
    private static final int MISFORTUNE_COST = 25000;            // in all (the wheel's CURSE_COST is a part of it)
    private static final int MISFORTUNE_COOLDOWN = 1200;
    private static final int CATASTROPHE_COST = 35000;
    private static final int CATASTROPHE_COOLDOWN = 1800;
    private static final double MISFORTUNE_RANGE = 48.0;
    private static final double MISFORTUNE_RADIUS = 12.0;
    private static final int MISFORTUNE_TICKS = 400;
    private static final int MISFORTUNE_START = 2;
    private static final int MISFORTUNE_EVERY = 100;
    private static final int MISFORTUNE_MOST = 5;

    private static final int WOLF_SEQUENCE = 4;
    private static final int WOLF_COST = 8000;
    private static final int WOLF_COOLDOWN = 3600;
    private static final int WOLF_TICKS = 600;
    private static final float WOLF_UPKEEP = 400f;               // each second
    private static final double WOLF_HEIGHT = 3.0;               // blocks: Nightwatcher...
    private static final double WOLF_HEIGHT_BISHOP = 4.5;        // ...Horror Bishop (3)...
    private static final double WOLF_HEIGHT_COMPLETE = 7.0;      // ...the complete wolf, Servant of Concealment (2)
    private static final float WOLF_MADNESS = 1f / 25f;          // each second: mad before its time is up...
    private static final float WOLF_MADNESS_BISHOP = 1f / 40f;   // ...not quite, at Horror Bishop; none for the complete wolf
    private static final int WOLF_SPENT = 200;                   // gone mad: weakened this long after
    private static final float WOLF_BITE = 1.5f;                 // more damage on every bite, times the power level
    private static final int WOLF_TEAR = 60;                     // a bitten wound does not heal this long
    private static final double WOLF_PRAYERS = 8.0;              // Horror Bishop: allies who sneak this close are healed
    private static final float WOLF_PRAYER_HEAL = 1f;            // each second

    // =========================
    // Saved on the player, and names
    // =========================
    private static final String DOMAIN_TAG = "knight_of_misfortune_domain_tag";
    private static final String MISFORTUNE_READY = "knight_of_misfortune_misfortune_ready";
    private static final String WOLF_TAG = "knight_of_misfortune_wolf_tag";
    private static final String DOMAIN_ZONE = "knight_of_misfortune_night_domain";
    private static final String MISFORTUNE_ZONE = "knight_of_misfortune_misfortune";
    private static final String WOLF_BITE_WEAPON = "knight_of_misfortune_wolf_bite";

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("knight_of_misfortune_nightwatcher", "Nightwatcher")
            .radius(64)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .entry("night_domain", KnightOfMisfortuneFx.WHEEL_NIGHT_DOMAIN, "Night Domain", DOMAIN_SEQUENCE,
                    DOMAIN_COST, DOMAIN_COOLDOWN, KnightOfMisfortuneNightwatcher::nightDomain)
            .free(player -> Zone.isOpen(player, DOMAIN_ZONE))                               // lifting it
            .entry("hair_entanglement", KnightOfMisfortuneFx.WHEEL_HAIR_ENTANGLEMENT, "Hair Entanglement", HAIR_SEQUENCE,
                    HAIR_COST, HAIR_COOLDOWN, KnightOfMisfortuneNightwatcher::hairEntanglement)
            .entry("curse_of_misfortune", KnightOfMisfortuneFx.WHEEL_CURSE_OF_MISFORTUNE, "Curse of Misfortune", CURSE_SEQUENCE,
                    CURSE_COST, CURSE_COOLDOWN, KnightOfMisfortuneNightwatcher::curseOfMisfortune)
            .entry("demonic_wolf", KnightOfMisfortuneFx.WHEEL_DEMONIC_WOLF, "Demonic Wolf Form", WOLF_SEQUENCE,
                    WOLF_COST, WOLF_COOLDOWN, KnightOfMisfortuneNightwatcher::demonicWolf)
            .free(KnightOfMisfortuneNightwatcher::isWolf);                                  // leaving it

    // =========================
    // Ability 6 - Nightwatcher
    // Normal: the Nightwatcher wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void knightOfMisfortuneNightwatcher(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!KnightOfMisfortune.ready(player, bypassClassCheck, NIGHTWATCHER_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Night Domain
    // =========================

    private static AbilityWheel.Result nightDomain(ServerPlayer player, ServerLevel sl) {
        if (Zone.isOpen(player, DOMAIN_ZONE)) {                           // the second pick: lift it
            Zone.close(player, DOMAIN_ZONE);
            return AbilityWheel.Result.FREE;
        }
        Vec3 ground = Spells.groundUnder(player, sl, player);
        String tag = "km_domain_" + player.getId() + "_" + sl.getGameTime();
        int risen = KnightOfMisfortuneFx.DOMAIN_RISEN;
        player.getPersistentData().putString(DOMAIN_TAG, tag);
        KnightOfMisfortuneFx.nightDomain(sl, ground, DOMAIN_RADIUS, DOMAIN_TICKS + risen, tag);
        KnightOfMisfortuneFx.status(player, Status.NIGHT_DOMAIN, DOMAIN_TICKS + risen);
        Nocturnality.forceNight(sl, ground, DOMAIN_RADIUS, DOMAIN_TICKS + risen);
        Later.run(sl, risen, () -> {
            if (!Spells.casterStillHere(player, sl)) {
                KnightOfMisfortuneFx.domainEnd(sl, ground, tag);
                return;
            }
            Zone.at(ground, DOMAIN_RADIUS).height(8).lasts(DOMAIN_TICKS).every(20)
                    .onInside((enemy, inside) -> domainTouches(player, sl, enemy, inside))
                    .onPulse(left -> domainPulse(player, sl, ground))
                    .onEnd(() -> {
                        KnightOfMisfortuneFx.domainEnd(sl, ground, tag);
                        KnightOfMisfortuneFx.statusEnd(player, Status.NIGHT_DOMAIN);
                        player.getPersistentData().remove(DOMAIN_TAG);
                    })
                    .open(player, sl, DOMAIN_ZONE);
        });
        return AbilityWheel.Result.DONE;
    }

    /** Once a second for each enemy in the domain: it rots, and its luck runs out. */
    private static void domainTouches(Player player, ServerLevel sl, LivingEntity enemy, int inside) {
        Decay.apply(enemy, 40, DOMAIN_ROT + DOMAIN_ROT_PER_SECOND * inside / 20f);
        KnightOfMisfortuneFx.domainCorrode(sl, enemy);
        if ((inside + 20) % DOMAIN_CURSE_EVERY == 0) {
            KnightOfMisfortune.curse(player, sl, enemy, 1, DOMAIN_CURSE_MOST, DOMAIN_CURSE_TICKS, false);
        }
    }

    /** Once a second: the night shows, and you, your allies and your spirits are strengthened in it. */
    private static void domainPulse(Player player, ServerLevel sl, Vec3 ground) {
        KnightOfMisfortuneFx.domainPulse(sl, ground, DOMAIN_RADIUS);
        List<LivingEntity> allies = new ArrayList<>(Spells.alliesAround(player, sl, ground, DOMAIN_RADIUS));
        if (player.position().distanceTo(ground) <= DOMAIN_RADIUS + 1) allies.add(player);
        for (LivingEntity ally : allies) {
            boolean spirit = Marionette.isMarionetteOf(ally, player);
            Spells.keepEffect(ally, MobEffects.STRENGTH, spirit ? 1 : 0, 40);
            Spells.keepEffect(ally, MobEffects.RESISTANCE, 0, 40);
            Spells.keepEffect(ally, MobEffects.REGENERATION, 0, 60);
        }
    }

    // =========================
    // Hair Entanglement
    // =========================

    private static AbilityWheel.Result hairEntanglement(ServerPlayer player, ServerLevel sl) {
        List<LivingEntity> caught = Spells.enemiesInCone(player, sl, player.getEyePosition(), player.getLookAngle(),
                HAIR_RANGE, HAIR_ANGLE);
        if (caught.isEmpty()) {
            player.sendSystemMessage(Component.literal("Your hair finds no one to bind."));
            return AbilityWheel.Result.REFUSED;
        }
        KnightOfMisfortuneFx.hairEntangle(sl, player, caught);
        Later.run(sl, KnightOfMisfortuneFx.HAIR_BINDS, () -> {
            for (LivingEntity victim : caught) {
                if (!victim.isAlive()) continue;
                int bind = KnightOfMisfortune.isBoss(victim) ? HAIR_BIND / 2 : HAIR_BIND;
                String tag = "km_hair_" + victim.getId();
                float health = victim.getHealth();
                Root.apply(victim, bind, RootRestriction.MOVE, RootRestriction.JUMP, RootRestriction.TELEPORT, RootRestriction.ATTACK);
                KnightOfMisfortuneFx.hairBind(sl, victim, bind, tag);
                Later.run(sl, bind, () -> {
                    ParticleShapes.clearModels(sl, victim, tag);
                    if (!victim.isAlive() || KnightOfMisfortune.isBoss(victim)) return;
                    if (victim.getHealth() < health - 0.01f) return;             // hurt while held: wide awake
                    if (Slumber.put(victim, HAIR_SLEEP, Slumber.Kind.SLEEP)) KnightOfMisfortune.showSlumber(sl, victim, HAIR_SLEEP);
                });
            }
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Curse of Misfortune / Misfortune / Catastrophe
    // =========================

    private static AbilityWheel.Result curseOfMisfortune(ServerPlayer player, ServerLevel sl) {
        if (Beyonder.hasSequence(player, 1)) return misfortuneArea(player, sl, Beyonder.hasSequence(player, 0));
        LivingEntity target = Spells.aimEnemy(player, sl, CURSE_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no one there to curse."));
            return AbilityWheel.Result.REFUSED;
        }
        KnightOfMisfortuneFx.curse(sl, player, target);
        Later.run(sl, KnightOfMisfortuneFx.CURSE_LANDS, () -> {
            if (!target.isAlive() || !Spells.casterStillHere(player, sl)) return;
            int level = KnightOfMisfortune.curse(player, sl, target, CURSE_LEVEL, CURSE_LEVEL, CURSE_TICKS, false);
            if (level > 0) player.sendSystemMessage(Component.literal(target.getName().getString() + " carries Misfortune " + level + "."));
            else player.sendSystemMessage(Component.literal(target.getName().getString() + " shrugs the curse off."));
        });
        return AbilityWheel.Result.DONE;
    }

    /** Knight of Misfortune (1) and Darkness (0): the curse falls on an area. */
    private static AbilityWheel.Result misfortuneArea(ServerPlayer player, ServerLevel sl, boolean catastrophe) {
        int cost = catastrophe ? CATASTROPHE_COST : MISFORTUNE_COST;
        String name = catastrophe ? "Catastrophe" : "Misfortune";
        if (KnightOfMisfortune.coolingDown(player, sl, MISFORTUNE_READY, name)) return AbilityWheel.Result.REFUSED;
        if (SoulCore.getSpirituality(player) < cost) {
            Spells.paySpirituality(player, cost);                       // takes nothing, only says so
            return AbilityWheel.Result.REFUSED;
        }
        Spells.paySpirituality(player, cost - CURSE_COST);             // the wheel takes the rest
        player.getPersistentData().putLong(MISFORTUNE_READY, sl.getGameTime() + (catastrophe ? CATASTROPHE_COOLDOWN : MISFORTUNE_COOLDOWN));
        Vec3 ground = Spells.aimGround(player, sl, MISFORTUNE_RANGE);
        String tag = "km_misfortune_area_" + player.getId() + "_" + sl.getGameTime();
        KnightOfMisfortuneFx.misfortuneArea(sl, ground, MISFORTUNE_RADIUS, MISFORTUNE_TICKS, tag);
        Zone.at(ground, MISFORTUNE_RADIUS).height(6).lasts(MISFORTUNE_TICKS).every(20)
                .onEnter(enemy -> KnightOfMisfortune.curse(player, sl, enemy, MISFORTUNE_START, MISFORTUNE_MOST, MISFORTUNE_TICKS, catastrophe))
                .onInside((enemy, inside) -> {
                    if (inside > 0 && inside % MISFORTUNE_EVERY == 0) {
                        KnightOfMisfortune.curse(player, sl, enemy, 1, MISFORTUNE_MOST, MISFORTUNE_TICKS, catastrophe);
                    }
                })
                .onEnd(() -> ParticleShapes.clearModels(sl, ground.add(0, 0.05, 0), tag))
                .open(player, sl, MISFORTUNE_ZONE);
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Demonic Wolf Form
    // =========================

    private static AbilityWheel.Result demonicWolf(ServerPlayer player, ServerLevel sl) {
        if (isWolf(player)) {                                             // the second pick: leave the form
            GiantForm.end(player);
            return AbilityWheel.Result.FREE;
        }
        if (GiantForm.isGiant(player)) {
            player.sendSystemMessage(Component.literal("Your body already wears another form."));
            return AbilityWheel.Result.REFUSED;
        }
        boolean complete = Beyonder.hasSequence(player, 2);
        boolean bishop = Beyonder.hasSequence(player, 3);
        double height = complete ? WOLF_HEIGHT_COMPLETE : bishop ? WOLF_HEIGHT_BISHOP : WOLF_HEIGHT;
        float madness = complete ? 0f : bishop ? WOLF_MADNESS_BISHOP : WOLF_MADNESS;
        double scale = height / 1.8;
        String tag = "km_wolf_" + player.getId() + "_" + sl.getGameTime();
        player.getPersistentData().putString(WOLF_TAG, tag);

        KnightOfMisfortuneFx.wolfForm(sl, player, height, WOLF_TICKS, tag, complete);
        KnightOfMisfortuneFx.status(player, Status.WOLF, WOLF_TICKS);
        int strength = complete ? 2 : 1;
        wolfBody(player, strength);
        GiantForm.of(scale)
                .reach(scale - 1.0)
                .stepHeight(Math.min(2.0, scale - 0.5))
                .lasts(WOLF_TICKS)
                .madness(madness)
                .everySecond((giant, mad) -> wolfSecond(player, sl, height, mad, strength, bishop))
                .onMad(giant -> {
                    player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, WOLF_SPENT, 0));
                    player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, WOLF_SPENT, 0));
                    player.sendSystemMessage(Component.literal("Madness takes the wolf: the form breaks and leaves you spent."));
                })
                .onEnd((giant, why) -> {
                    String old = player.getPersistentData().getStringOr(WOLF_TAG, tag);
                    player.getPersistentData().remove(WOLF_TAG);
                    ConjuredWeapon.putAway(player, WOLF_BITE_WEAPON);
                    KnightOfMisfortuneFx.wolfFormEnd(sl, player, height, old);
                    KnightOfMisfortuneFx.statusEnd(player, Status.WOLF);
                })
                .grow(player);
        float bite = WOLF_BITE * Beyonder.powerLevel(player);
        ConjuredWeapon.of(WOLF_BITE_WEAPON).lasts(WOLF_TICKS).damage(bite)
                .onHit((attacker, target, amount, charge, level) -> {
                    KnightOfMisfortuneFx.wolfBite(level, attacker, target);
                    if (charge > 0.8f) NoHeal.apply(target, WOLF_TEAR);
                    return amount;
                })
                .draw(player);
        return AbilityWheel.Result.DONE;
    }

    private static void wolfBody(Player player, int strength) {
        Spells.keepEffect(player, MobEffects.STRENGTH, strength, 60);
        Spells.keepEffect(player, MobEffects.SPEED, 1, 60);
        Spells.keepEffect(player, MobEffects.RESISTANCE, 0, 60);
    }

    /** Once a second in the form: its price, its strength, its madness; at Horror Bishop the prayers of allies. */
    private static void wolfSecond(Player player, ServerLevel sl, double height, float madness, int strength, boolean bishop) {
        if (!Spells.paySpirituality(player, WOLF_UPKEEP)) {
            GiantForm.end(player);
            return;
        }
        wolfBody(player, strength);
        if (madness > 0f) {
            KnightOfMisfortuneFx.wolfMadness(sl, player, madness, height);
            player.sendOverlayMessage(Component.literal("Madness " + Math.round(madness * 100) + "%"));
        }
        if (!bishop) return;
        for (LivingEntity ally : Spells.alliesAround(player, sl, player.position(), WOLF_PRAYERS)) {
            if (ally.isShiftKeyDown() && ally.getHealth() < ally.getMaxHealth()) {
                ally.heal(WOLF_PRAYER_HEAL);
                KnightOfMisfortuneFx.soothed(sl, ally);
            }
        }
    }

    /** True while the player is the Demonic Wolf. */
    static boolean isWolf(Player player) {
        return GiantForm.isGiant(player) && player.getPersistentData().contains(WOLF_TAG);
    }

    /** Every tick for every player: the wolf's steps. */
    static void tick(Player player, ServerLevel sl) {
        if (player.tickCount % 10 != 0 || !isWolf(player) || !player.onGround()) return;
        if (player.getDeltaMovement().horizontalDistanceSqr() < 0.004) return;
        KnightOfMisfortuneFx.wolfStep(sl, player, GiantForm.scale(player) * 1.8);
    }

    static void clearState(Player player) {
        CompoundTag data = player.getPersistentData();
        data.remove(DOMAIN_TAG);
        data.remove(WOLF_TAG);
    }
}
