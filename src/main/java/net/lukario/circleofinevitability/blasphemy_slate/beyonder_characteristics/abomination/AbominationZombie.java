package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.AbominationFx.Status;
import net.lukario.circleofinevitability.combat.Countdown;
import net.lukario.circleofinevitability.combat.Fallen;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.root.Freeze;
import net.lukario.circleofinevitability.status.Bleeding;
import net.lukario.circleofinevitability.status.Connection;
import net.lukario.circleofinevitability.status.ConjuredArmor;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.StoredDamage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Abomination, key 4: the Zombie wheel (Zombie, Sequence 6). docs/ABOMINATION.md, "Key 4 wheel".
 *
 *   Raise the Dead   three zombies claw out of the ground where you aim and fight for you; the enemies killed near
 *                    there in the last 10 seconds rise too, as what they were (combat/Fallen). Six from Puppet (4),
 *                    ten from Ancient Bane (2), and then what they hit carries Misfortune
 *   Grave Chill      a cold mist round you: enemies in it slow more the longer they stay, and freeze once
 *   Undying Flesh    20 s: much less damage (projectiles most), no knockback, no bleeding or poison, and the body
 *                    knits 2% of its health a second (status/ConjuredArmor)
 *   Death Sentence   a countdown of 5 on the enemy you look at; at 0 it loses a share of its health, and if that
 *                    kills it, it rises for you. From Ancient Bane (2) up to three enemies in sight at once
 *
 * Sneak casts the last pick again without the wheel (no cooldown, double the cost).
 */
public final class AbominationZombie {

    private AbominationZombie() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int ZOMBIE_SEQUENCE = 6;                        // the key
    private static final float QUICK_CAST_COST = 2f;

    // Raise the Dead
    private static final int RAISE_SEQUENCE = 6;
    private static final int RAISE_COST = 1500;
    private static final int RAISE_COOLDOWN = 200;
    private static final double RAISE_RANGE = 24.0;
    private static final double RAISE_SPREAD = 2.5;              // the zombies come up this far round the spot
    private static final int RAISE_COUNT = 3;
    private static final int RAISE_PUPPET_SEQUENCE = 4;          // Puppet: six
    private static final int RAISE_PUPPET_COUNT = 6;
    private static final int RAISE_BANE_SEQUENCE = 2;            // Ancient Bane: ten, and their blows carry Misfortune
    private static final int RAISE_BANE_COUNT = 10;
    private static final int RAISE_TICKS = 600;                  // they serve this long (longer with Puppeteer)
    private static final int RAISE_STAGGER = 3;                  // ticks between two hands coming up
    private static final double FALLEN_RADIUS = 10.0;            // the dead that fell this close...
    private static final int FALLEN_AGE = 200;                   // ...this lately...
    private static final int FALLEN_MOST = 3;                    // ...rise too, this many at most

    // Grave Chill
    private static final int CHILL_SEQUENCE = 6;
    private static final int CHILL_COST = 2000;
    private static final int CHILL_COOLDOWN = 300;
    private static final double CHILL_RADIUS = 10.0;
    private static final int CHILL_TICKS = 160;
    private static final int CHILL_EVERY = 10;
    private static final int CHILL_DEEPER = 20;                  // one level of slowness more every second inside...
    private static final int CHILL_MOST_SLOW = 3;                // ...up to Slowness IV
    private static final int CHILL_FREEZE_AFTER = 60;            // after 3 seconds inside: frozen...
    private static final int CHILL_FREEZE_TICKS = 20;            // ...for a second, once each

    // Undying Flesh
    private static final int UNDYING_SEQUENCE = 6;
    private static final int UNDYING_COST = 1800;
    private static final int UNDYING_COOLDOWN = 300;
    private static final int UNDYING_TICKS = 400;
    private static final float UNDYING_CUT = 0.35f;              // 35% less from everything...
    private static final float UNDYING_PROJECTILE_CUT = 0.6f;    // ...60% less from projectiles
    private static final float UNDYING_HEAL = 0.02f;             // of the maximum health, every second

    // Death Sentence
    private static final int SENTENCE_SEQUENCE = 6;
    private static final int SENTENCE_COST = 2500;
    private static final int SENTENCE_COOLDOWN = 600;
    private static final double SENTENCE_RANGE = 24.0;
    private static final double SENTENCE_LEASH = 32.0;           // further than this at 0: the sentence is lifted
    private static final int SENTENCE_SECONDS = 5;
    private static final float SENTENCE_SHARE = 0.3f;            // of its maximum health...
    private static final float SENTENCE_LOW_SHARE = 0.5f;        // ...this much if it was below half health
    private static final int SENTENCE_BANE_SEQUENCE = 2;         // Ancient Bane: up to three in sight
    private static final int SENTENCE_BANE_MOST = 3;
    private static final double SENTENCE_SIGHT = 30.0;           // degrees from the crosshair
    private static final int SENTENCE_RISEN_TICKS = 600;         // one it kills serves this long (longer with Puppeteer)

    // =========================
    // Names and what is saved on the player
    // =========================
    private static final String CHILL_ZONE = "abomination_grave_chill";
    private static final String CHILL_TAG = "abomination_chill_tag";
    private static final String UNDYING = "abomination_undying_flesh";
    private static final String UNDYING_TAG = "abomination_undying_tag";

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("abomination_zombie", "Zombie")
            .radius(72)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .castsWhileSilenced(Abomination::castsThroughHush)
            .entry("raise_the_dead", AbominationFx.WHEEL_RAISE_THE_DEAD, "Raise the Dead", RAISE_SEQUENCE,
                    RAISE_COST, RAISE_COOLDOWN, AbominationZombie::raiseTheDead)
            .entry("grave_chill", AbominationFx.WHEEL_GRAVE_CHILL, "Grave Chill", CHILL_SEQUENCE,
                    CHILL_COST, CHILL_COOLDOWN, AbominationZombie::graveChill)
            .entry("undying_flesh", AbominationFx.WHEEL_UNDYING_FLESH, "Undying Flesh", UNDYING_SEQUENCE,
                    UNDYING_COST, UNDYING_COOLDOWN, AbominationZombie::undyingFlesh)
            .entry("death_sentence", AbominationFx.WHEEL_DEATH_SENTENCE, "Death Sentence", SENTENCE_SEQUENCE,
                    SENTENCE_COST, SENTENCE_COOLDOWN, AbominationZombie::deathSentence);

    // =========================
    // Ability 4 - Zombie
    // Normal: the Zombie wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void abominationZombie(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!Abomination.ready(player, bypassClassCheck, ZOMBIE_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Raise the Dead: where you aim (RAISE_RANGE) RAISE_COUNT zombies claw out of the ground (husks under the sun,
    // which does not burn them) and fight for you for RAISE_TICKS (status/Marionette summons: they vanish after,
    // with no drops). The enemies that fell within FALLEN_RADIUS of the spot in the last FALLEN_AGE rise too, up to
    // FALLEN_MOST, as what they were (combat/Fallen; bosses never rise). Puppet (4): RAISE_PUPPET_COUNT. Ancient
    // Bane (2): RAISE_BANE_COUNT, and what they hit carries a level of Misfortune.
    // =========================
    private static AbilityWheel.Result raiseTheDead(ServerPlayer player, ServerLevel sl) {
        Vec3 ground = Spells.aimGround(player, sl, RAISE_RANGE);
        boolean bane = Beyonder.hasSequence(player, RAISE_BANE_SEQUENCE);
        int count = bane ? RAISE_BANE_COUNT : Beyonder.hasSequence(player, RAISE_PUPPET_SEQUENCE) ? RAISE_PUPPET_COUNT : RAISE_COUNT;
        int ticks = Abomination.servantTicks(player, RAISE_TICKS);
        AbominationFx.raiseTheDead(sl, ground, RAISE_SPREAD + 0.5);
        AbominationFx.status(player, Status.RISEN, ticks);

        float turn = player.getRandom().nextFloat() * 360f;
        for (int i = 0; i < count; i++) {
            float yaw = turn + i * 360f / count;
            double out = count == 1 ? 0 : RAISE_SPREAD * (0.45 + 0.55 * player.getRandom().nextDouble());
            Vec3 spot = Spells.groundNear(player, sl, Spells.spotAround(ground, yaw, out), 3);
            Vec3 at = spot != null ? spot : ground;
            Later.run(sl, i * RAISE_STAGGER, () -> {
                if (!Spells.casterStillHere(player, sl)) return;
                AbominationFx.zombieRises(sl, at);
                Later.run(sl, AbominationFx.HANDS_UP, () -> {
                    if (!Spells.casterStillHere(player, sl)) return;
                    // a zombie burns under the sun; the desert's dead do not
                    EntityType<? extends Mob> type = EntityTypes.ZOMBIE;
                    if (Spells.inDaylight(sl, BlockPos.containing(at.add(0, 1, 0)))) type = EntityTypes.HUSK;
                    Mob zombie = Marionette.spawnSummon(type, player, sl, at, player.getYRot(), ticks);
                    if (zombie != null) risen(player, sl, zombie, ticks, bane);
                });
            });
        }

        int rose = 0;
        for (Fallen.Body body : Fallen.near(sl, ground, FALLEN_RADIUS, FALLEN_AGE)) {
            if (rose >= FALLEN_MOST) break;
            if (!Fallen.take(body)) continue;                            // another cast is raising it already
            rose++;
            int delay = (count + rose) * RAISE_STAGGER;
            Later.run(sl, delay, () -> {
                if (!Spells.casterStillHere(player, sl)) return;
                AbominationFx.fallenRises(sl, body.where());
                Later.run(sl, AbominationFx.HANDS_UP, () -> {
                    if (!Spells.casterStillHere(player, sl)) return;
                    Mob again = Fallen.rise(body, player, sl, ticks);
                    if (again != null) risen(player, sl, again, ticks, bane);
                });
            });
        }
        return AbilityWheel.Result.DONE;
    }

    /** One of the dead is up and serving: its mark, no burning, and the Bane's touch. */
    static void risen(Player player, ServerLevel sl, Mob servant, int ticks, boolean bane) {
        AbominationFx.servantMark(sl, servant, ticks, "ab_risen_" + servant.getId());
        servant.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, ticks, 0, false, false));
        if (bane) servant.getPersistentData().putBoolean(Abomination.BANE_TOUCH, true);
    }

    // =========================
    // Grave Chill: a cold, lifeless mist rolls out round you, CHILL_RADIUS, for CHILL_TICKS (a Zone that goes with
    // you, from the moment the frost has spread: AbominationFx.CHILL_SPREAD). Enemies in it are slowed one level more
    // every CHILL_DEEPER they stay (up to CHILL_MOST_SLOW), and after CHILL_FREEZE_AFTER they are frozen in ice for
    // CHILL_FREEZE_TICKS, once each.
    // =========================
    private static AbilityWheel.Result graveChill(ServerPlayer player, ServerLevel sl) {
        String tag = "ab_chill_" + player.getId() + "_" + sl.getGameTime();
        player.getPersistentData().putString(CHILL_TAG, tag);
        AbominationFx.graveChill(sl, player, CHILL_RADIUS, CHILL_TICKS, tag);
        AbominationFx.status(player, Status.GRAVE_CHILL, CHILL_TICKS);
        Set<UUID> frozen = new HashSet<>();
        Later.run(sl, AbominationFx.CHILL_SPREAD, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            Zone.around(player, CHILL_RADIUS)
                    .lasts(CHILL_TICKS - AbominationFx.CHILL_SPREAD)
                    .every(CHILL_EVERY)
                    .onInside((enemy, inside) -> {
                        int depth = Math.min(CHILL_MOST_SLOW, inside / CHILL_DEEPER);
                        Spells.keepEffect(enemy, MobEffects.SLOWNESS, depth, 30);
                        AbominationFx.chilled(sl, enemy, depth / (float) CHILL_MOST_SLOW);
                        if (inside >= CHILL_FREEZE_AFTER && frozen.add(enemy.getUUID())) {
                            Freeze.apply(enemy, CHILL_FREEZE_TICKS);
                            AbominationFx.chillFreeze(sl, enemy);
                        }
                    })
                    .onPulse(left -> AbominationFx.graveChillTick(sl, player, CHILL_RADIUS))
                    .onEnd(() -> {
                        AbominationFx.graveChillEnd(sl, player, tag);
                        // cast again meanwhile: the new chill's picture stays
                        if (player.getPersistentData().getStringOr(CHILL_TAG, "").equals(tag)) {
                            player.getPersistentData().remove(CHILL_TAG);
                            AbominationFx.statusEnd(player, Status.GRAVE_CHILL);
                        }
                    })
                    .open(player, sl, CHILL_ZONE);
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Undying Flesh: for UNDYING_TICKS your body forgets pain (status/ConjuredArmor): UNDYING_CUT less damage from
    // everything and UNDYING_PROJECTILE_CUT less from projectiles, no knockback, bleeding and poison do not take (and
    // end at once), and you regenerate UNDYING_HEAL of your health every second.
    // =========================
    private static AbilityWheel.Result undyingFlesh(ServerPlayer player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        AbominationFx.undyingEnd(sl, player, data.getStringOr(UNDYING_TAG, ""));       // cast again: one mark, not two
        String tag = "ab_undying_" + player.getId() + "_" + sl.getGameTime();
        data.putString(UNDYING_TAG, tag);
        Bleeding.remove(player);
        player.removeEffect(MobEffects.POISON);
        ConjuredArmor.of(UNDYING)
                .lasts(UNDYING_TICKS)
                .cut(UNDYING_CUT).projectileCut(UNDYING_PROJECTILE_CUT)
                .steady()
                .onHit((wearer, amount) -> AbominationFx.undyingHit(sl, player))
                .onEnd(wearer -> {
                    if (player.isAlive() && player.level() == sl) AbominationFx.undyingEnd(sl, player, tag);
                    if (player.getPersistentData().getStringOr(UNDYING_TAG, "").equals(tag)) player.getPersistentData().remove(UNDYING_TAG);
                    AbominationFx.statusEnd(player, Status.UNDYING);
                })
                .wear(player);
        AbominationFx.undyingFlesh(sl, player, UNDYING_TICKS, tag);
        AbominationFx.status(player, Status.UNDYING, UNDYING_TICKS);
        return AbilityWheel.Result.DONE;
    }

    /** True while Undying Flesh is on the player. */
    static boolean isUndying(Player player) {
        return ConjuredArmor.isWorn(player, UNDYING);
    }

    // =========================
    // Death Sentence: the enemy you look at (SENTENCE_RANGE) is sentenced. A countdown of SENTENCE_SECONDS runs on
    // your screen; if it is still alive and within SENTENCE_LEASH of you at 0, it takes SENTENCE_SHARE of its maximum
    // health as magic damage (SENTENCE_LOW_SHARE if it was below half health). If that kills it, it rises for you
    // (combat/Fallen) for SENTENCE_RISEN_TICKS. Sentencing someone else ends the first (one countdown at a time).
    // Ancient Bane (2): up to SENTENCE_BANE_MOST enemies in sight are sentenced at once.
    // =========================
    private static AbilityWheel.Result deathSentence(ServerPlayer player, ServerLevel sl) {
        List<LivingEntity> sentenced = new ArrayList<>();
        if (Beyonder.hasSequence(player, SENTENCE_BANE_SEQUENCE)) {
            for (LivingEntity one : Spells.enemiesInSight(player, sl, SENTENCE_RANGE, SENTENCE_SIGHT)) {
                if (sentenced.size() >= SENTENCE_BANE_MOST) break;
                sentenced.add(one);
            }
        } else {
            LivingEntity one = Spells.aimEnemy(player, sl, SENTENCE_RANGE);
            if (one != null) sentenced.add(one);
        }
        if (sentenced.isEmpty()) {
            player.sendSystemMessage(Component.literal("There is no one before you to sentence."));
            return AbilityWheel.Result.REFUSED;
        }

        String tag = "ab_sentence_" + player.getId() + "_" + sl.getGameTime();
        int ticks = SENTENCE_SECONDS * 20;
        // started first: a sentence still running is lifted now (its onCancel), before the new one is shown
        Countdown.seconds(SENTENCE_SECONDS)
                .colors(0xB070FF, 0xFF3060)
                .onSecond(left -> {
                    for (LivingEntity one : sentenced) if (stillSentenced(player, sl, one)) AbominationFx.sentenceTick(sl, one, left);
                })
                .keepWhile(() -> {
                    for (LivingEntity one : sentenced) if (stillSentenced(player, sl, one)) return true;
                    return false;
                })
                .onFinish(() -> {
                    for (LivingEntity one : sentenced) {
                        if (stillSentenced(player, sl, one)) sentenceFalls(player, sl, one, tag);
                        else if (one.isAlive()) AbominationFx.sentenceLifted(sl, one, tag);
                    }
                })
                .onCancel(() -> {
                    for (LivingEntity one : sentenced) if (one.isAlive()) AbominationFx.sentenceLifted(sl, one, tag);
                    AbominationFx.statusEnd(player, Status.SENTENCE);
                })
                .start(player);
        for (LivingEntity one : sentenced) {
            AbominationFx.deathSentence(sl, player, one, ticks, tag);
            Connection.touch(player, one);                               // the sentenced are your connections
        }
        AbominationFx.status(player, Status.SENTENCE, ticks);
        return AbilityWheel.Result.DONE;
    }

    private static boolean stillSentenced(Player player, ServerLevel sl, LivingEntity one) {
        return one.isAlive() && one.level() == sl && player.distanceTo(one) <= SENTENCE_LEASH;
    }

    /** The count reached 0: the mark falls, and the blow lands when it gets there. */
    private static void sentenceFalls(Player player, ServerLevel sl, LivingEntity one, String tag) {
        AbominationFx.sentenceFalls(sl, one, tag);
        Later.run(sl, AbominationFx.SENTENCE_LANDS, () -> {
            if (!Spells.casterStillHere(player, sl) || !one.isAlive() || one.level() != sl) return;
            float share = one.getHealth() < one.getMaxHealth() / 2f ? SENTENCE_LOW_SHARE : SENTENCE_SHARE;
            StoredDamage.deal(one, player, one.getMaxHealth() * share);
            if (one.isAlive()) return;
            // the sentence was carried out: it gets up again, for you (combat/Fallen wrote its death down just now)
            for (Fallen.Body body : Fallen.near(sl, one.position(), 2.0, 20)) {
                if (!body.id().equals(one.getUUID()) || !Fallen.take(body)) continue;
                AbominationFx.fallenRises(sl, body.where());
                int ticks = Abomination.servantTicks(player, SENTENCE_RISEN_TICKS);
                Later.run(sl, AbominationFx.HANDS_UP, () -> {
                    if (!Spells.casterStillHere(player, sl)) return;
                    Mob again = Fallen.rise(body, player, sl, ticks);
                    if (again != null) risen(player, sl, again, ticks, Beyonder.hasSequence(player, RAISE_BANE_SEQUENCE));
                });
                break;
            }
        });
    }

    // =========================
    // Called from Abomination
    // =========================

    /** Every tick. */
    static void tick(Player player, ServerLevel sl) {
        if (player.tickCount % 20 != 0 || !isUndying(player)) return;
        if (player.getHealth() < player.getMaxHealth()) player.heal(player.getMaxHealth() * UNDYING_HEAL);
        AbominationFx.undyingTick(sl, player);
    }

    /** After a relog or a death: start clean (the chill and the armor do not survive either). */
    static void clearState(Player player) {
        CompoundTag data = player.getPersistentData();
        data.remove(CHILL_TAG);
        data.remove(UNDYING_TAG);
    }
}
