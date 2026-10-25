package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGodFx.Status;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Reel;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Burial;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.ConjuredArmor;
import net.lukario.circleofinevitability.status.Decay;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.NoHeal;
import net.lukario.circleofinevitability.status.StoredDamage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Hand Of God, key 7: the Silver and Twilight wheel (Silver Knight, Sequence 3, up to Twilight Giant, Sequence 0).
 * docs/HAND_OF_GOD.md, "Key 7 wheel". Every entry is cast where you aim.
 *
 *   Mercury Liquefaction (3)  melt into silver: slide to where you aim, wrapping enemies you pass; aimed at an ally
 *                             you become their Silver Armor, aimed at an enemy a coat that holds and chokes it
 *   Silver Rapier (3)         three rapiers out of thin air stab the enemy you look at from every side
 *   Light Concealment (3)     step into a light near you and out of the light you aim at (sunlight counts by day).
 *                             Glory (2): with no light, you wither away and re-form wherever you aim
 *   Twilight Sword (2)        a sword of Glory falls out of the void where the enemy stands: nothing shields from it
 *   Divine Right Hand (1)     a hand of dusk seizes the enemy you aim at, squeezes and flings it; aimed at the
 *                             ground (or a boss) it slams down and presses all round into the earth
 *   Passage of Time (0)       every enemy round you is frozen in time; what they take meanwhile lands again, doubled
 *   Decay (0)                 Twilight rots everything in a circle where you aim, worse the longer they stay
 *   Divine Kingdom (0)        a kingdom of Twilight 40 blocks round you: enemies weak, slow and unhealing (and
 *                             their blows weaker), allies acknowledged. Cast again to end it
 *
 * Sneak casts the last pick again without the wheel (no cooldown, double the cost).
 */
public final class HandOfGodTwilight {

    private HandOfGodTwilight() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int TWILIGHT_SEQUENCE = 3;                      // the key
    static final float QUICK_CAST_COST = 2f;

    // Mercury Liquefaction
    static final int MERCURY_SEQUENCE = 3;
    static final int MERCURY_COST = 10000;
    static final int MERCURY_COOLDOWN = 400;
    static final int MERCURY_TICKS = 120;                // liquid at most this long (cast again to re-form early)
    static final double MERCURY_RANGE = 20.0;
    static final double MERCURY_SPEED = 0.6;             // blocks per tick along the ground
    static final double MERCURY_WRAP_REACH = 1.5;        // enemies this close as you slide past...
    static final int MERCURY_WRAP_TICKS = 40;            // ...are wrapped and held this long
    static final float MERCURY_ARMOR_CUT = 0.4f;         // as an ally's Silver Armor: this much less damage...
    static final float MERCURY_ARMOR_ABSORPTION = 8f;    // ...and this layer of absorption
    static final float MERCURY_CHOKE = 5f;               // as an enemy's coat: it is held and takes this each second

    // Silver Rapier
    static final int RAPIER_SEQUENCE = 3;
    static final int RAPIER_COST = 8000;
    static final int RAPIER_COOLDOWN = 200;
    static final double RAPIER_RANGE = 32.0;
    static final int RAPIER_COUNT = 3;
    static final float RAPIER_DAMAGE = 20f;              // each

    // Light Concealment
    static final int LIGHT_SEQUENCE = 3;
    static final int LIGHT_COST = 6000;
    static final int LIGHT_COOLDOWN = 300;
    static final double LIGHT_RANGE = 32.0;              // the light you come out of: this far...
    static final double LIGHT_AROUND = 4.0;              // ...and within this of the crosshair
    static final double LIGHT_ENTRY_REACH = 6.0;         // the light you step into: this close to you
    static final int LIGHT_MIN = 10;                     // a light source this bright counts (torch 14, lantern 15)
    static final int LIGHT_TRAVEL = 20;                  // hidden on the way this long
    static final int LIGHT_HIDDEN_AFTER = 120;           // and this long after it with Mind Concealment up
    static final int WITHERING_SEQUENCE = 2;            // Glory: no light needed, you wither away instead

    // Twilight Sword
    static final int SWORD_SEQUENCE = 2;
    static final int SWORD_COST = 20000;
    static final int SWORD_COOLDOWN = 600;
    static final double SWORD_RANGE = 48.0;
    static final double SWORD_RADIUS = 5.0;
    static final float SWORD_DAMAGE = 90f;               // through shields, substitutes and death wards
    static final int SWORD_DECAY_TICKS = 200;
    static final float SWORD_DECAY_DEPTH = 0.3f;

    // Divine Right Hand
    static final int HAND_SEQUENCE = 1;
    static final int HAND_COST = 30000;
    static final int HAND_COOLDOWN = 1200;
    static final double HAND_RANGE = 32.0;
    static final int HAND_HOLD = 60;                     // seized and squeezed this long...
    static final float HAND_SQUEEZE = 15f;               // ...this much each second...
    static final float HAND_FLING = 35f;                 // ...then flung with this much (80 in all)
    static final double HAND_FLING_STRENGTH = 2.6;
    static final double HAND_FLING_LIFT = 0.9;
    static final float BOSS_HEALTH = 150f;               // more than this, or a boss: the hand slams instead
    static final double HAND_SLAM_RADIUS = 6.0;
    static final float HAND_SLAM_DAMAGE = 80f;
    static final int HAND_SLAM_HOLD = 40;                // pressed into the ground this long
    static final int HAND_DECAY_TICKS = 120;
    static final float HAND_DECAY_DEPTH = 0.2f;

    // Passage of Time
    static final int TIME_SEQUENCE = 0;
    static final int TIME_COST = 40000;
    static final int TIME_COOLDOWN = 2400;
    static final double TIME_RADIUS = 24.0;
    static final int TIME_TICKS = 160;
    static final float TIME_DUE = 2f;                    // what they took while frozen lands again this many times

    // Decay
    static final int DECAY_SEQUENCE = 0;
    static final int DECAY_COST = 35000;
    static final int DECAY_COOLDOWN = 1800;
    static final double DECAY_RANGE = 32.0;
    static final double DECAY_RADIUS = 12.0;
    static final int DECAY_TICKS = 240;
    static final float DECAY_DEPTH = 0.1f;               // health lost to it at first...
    static final float DECAY_DEEPER = 0.05f;             // ...and this much more for every second they stay
    static final float DECAY_DEEPEST = 0.5f;

    // Divine Kingdom
    static final int KINGDOM_SEQUENCE = 0;
    static final int KINGDOM_COST = 50000;
    static final int KINGDOM_COOLDOWN = 3600;
    static final double KINGDOM_RADIUS = 40.0;
    static final double KINGDOM_HEIGHT = 24.0;
    static final int KINGDOM_TICKS = 1200;
    static final float KINGDOM_ENEMY_DAMAGE = 0.7f;      // what enemies inside deal (their blows and abilities)

    // =========================
    // Names and what is kept
    // =========================
    static final String MERCURY_ARMOR = "hand_of_god_mercury_armor";
    static final String DECAY_ZONE = "hand_of_god_decay";
    static final String KINGDOM_ZONE = "hand_of_god_divine_kingdom";

    /** One knight melted into silver. Not saved. */
    private enum MercuryMode { SLIDE, ARMOR, COAT }

    private static final class Mercury {
        final MercuryMode mode;
        final long until;
        final String tag;
        final LivingEntity host;                                 // the ally or enemy it coats (null when sliding)
        final Set<UUID> wrapped = new HashSet<>();
        boolean ended;

        Mercury(MercuryMode mode, long until, String tag, LivingEntity host) {
            this.mode = mode;
            this.until = until;
            this.tag = tag;
            this.host = host;
        }
    }
    private static final Map<UUID, Mercury> MERCURY = new HashMap<>();

    /** True while it is melted into silver (Mercury Liquefaction). */
    static boolean isLiquid(LivingEntity one) {
        return MERCURY.containsKey(one.getUUID());
    }

    /** Every Divine Kingdom that stands: owner -> where. Not saved (the zone is not either). */
    private record Kingdom(LivingEntity owner, Level level, Vec3 center) {}
    private static final Map<UUID, Kingdom> KINGDOMS = new HashMap<>();

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("hand_of_god_twilight", "Silver and Twilight")
            .radius(92)                                                         // eight entries
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .entry("mercury", HandOfGodFx.WHEEL_MERCURY, "Mercury Liquefaction", MERCURY_SEQUENCE,
                    MERCURY_COST, MERCURY_COOLDOWN, HandOfGodTwilight::mercuryLiquefaction)
            .free(player -> MERCURY.containsKey(player.getUUID()))             // re-forming early
            .entry("silver_rapier", HandOfGodFx.WHEEL_RAPIER, "Silver Rapier", RAPIER_SEQUENCE,
                    RAPIER_COST, RAPIER_COOLDOWN, HandOfGodTwilight::silverRapier)
            .entry("light_concealment", HandOfGodFx.WHEEL_LIGHT_CONCEALMENT, "Light Concealment", LIGHT_SEQUENCE,
                    LIGHT_COST, LIGHT_COOLDOWN, HandOfGodTwilight::lightConcealment)
            .entry("twilight_sword", HandOfGodFx.WHEEL_TWILIGHT_SWORD, "Twilight Sword", SWORD_SEQUENCE,
                    SWORD_COST, SWORD_COOLDOWN, HandOfGodTwilight::twilightSword)
            .entry("divine_right_hand", HandOfGodFx.WHEEL_DIVINE_HAND, "Divine Right Hand", HAND_SEQUENCE,
                    HAND_COST, HAND_COOLDOWN, HandOfGodTwilight::divineRightHand)
            .entry("passage_of_time", HandOfGodFx.WHEEL_PASSAGE_OF_TIME, "Passage of Time", TIME_SEQUENCE,
                    TIME_COST, TIME_COOLDOWN, HandOfGodTwilight::passageOfTime)
            .entry("decay", HandOfGodFx.WHEEL_DECAY, "Decay", DECAY_SEQUENCE,
                    DECAY_COST, DECAY_COOLDOWN, HandOfGodTwilight::decay)
            .entry("divine_kingdom", HandOfGodFx.WHEEL_DIVINE_KINGDOM, "Divine Kingdom", KINGDOM_SEQUENCE,
                    KINGDOM_COST, KINGDOM_COOLDOWN, HandOfGodTwilight::divineKingdom)
            .free(player -> Zone.isOpen(player, KINGDOM_ZONE));                // ending it early

    // =========================
    // Ability 7 - Silver and Twilight
    // Normal: the Silver and Twilight wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void handOfGodTwilight(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!HandOfGod.ready(player, bypassClassCheck, TWILIGHT_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Mercury Liquefaction: you melt into silver (Concealment: unseen and untouchable) for at most MERCURY_TICKS.
    //   aimed at an ally:   you become their Silver Armor (MERCURY_ARMOR_CUT, MERCURY_ARMOR_ABSORPTION) and go with them
    //   aimed at an enemy:  you coat it: it is held fast and takes MERCURY_CHOKE each second
    //   otherwise:          you slide (MERCURY_SPEED) to where you aim, MERCURY_RANGE at most, and every enemy you
    //                       pass is wrapped and held MERCURY_WRAP_TICKS; you re-form when you get there
    // Cast again while liquid: you re-form at once (free).
    // =========================
    static AbilityWheel.Result mercuryLiquefaction(LivingEntity player, ServerLevel sl) {
        Mercury running = MERCURY.get(player.getUUID());
        if (running != null) {
            reform(player, sl, running);
            return AbilityWheel.Result.FREE;
        }
        LivingEntity ally = aimAlly(player, sl, MERCURY_RANGE);
        LivingEntity enemy = ally == null ? Spells.aimEnemy(player, sl, MERCURY_RANGE) : null;
        Vec3 there = ally == null && enemy == null ? Spells.blinkSpot(player, sl, MERCURY_RANGE) : null;
        if (ally == null && enemy == null && there == null) {
            player.sendSystemMessage(Component.literal("There is no room where you look."));
            return AbilityWheel.Result.REFUSED;
        }
        MercuryMode mode = ally != null ? MercuryMode.ARMOR : enemy != null ? MercuryMode.COAT : MercuryMode.SLIDE;
        LivingEntity host = ally != null ? ally : enemy;
        String tag = "hog_mercury_" + player.getId() + "_" + sl.getGameTime();
        Mercury mercury = new Mercury(mode, sl.getGameTime() + MERCURY_TICKS, tag, host);
        MERCURY.put(player.getUUID(), mercury);

        Concealment.hide(player, MERCURY_TICKS + 5);                            // reform() brings them back
        HandOfGodFx.mercury(sl, player, MERCURY_TICKS, tag);
        HandOfGodFx.status(player, Status.MERCURY, MERCURY_TICKS);
        switch (mode) {
            case ARMOR -> {
                ConjuredArmor.of(MERCURY_ARMOR).lasts(MERCURY_TICKS).cut(MERCURY_ARMOR_CUT).absorption(MERCURY_ARMOR_ABSORPTION).steady()
                        .onEnd(wearer -> {
                            if (wearer.level() instanceof ServerLevel level) ParticleShapes.clearModels(level, wearer, tag);
                        })
                        .wear(ally);
                HandOfGodFx.mercuryArmor(sl, ally, MERCURY_TICKS, tag);
            }
            case COAT -> {
                Root.apply(enemy, MERCURY_TICKS, RootRestriction.EVERYTHING);
                HandOfGodFx.mercuryWrap(sl, enemy, MERCURY_TICKS);
            }
            case SLIDE -> {
                Vec3 to = there;
                Reel.in(player, () -> mercury.ended ? null : to)
                        .speed(MERCURY_SPEED).stopAt(0.6).lasts(MERCURY_TICKS)
                        .onEnd(() -> reform(player, sl, mercury))
                        .start(sl);
            }
        }
        mercuryStep(player, sl, mercury, 0);
        return AbilityWheel.Result.DONE;
    }

    /** Every tick while liquid: the drops, the wrapping, going along with the host, the end. */
    private static void mercuryStep(LivingEntity player, ServerLevel sl, Mercury mercury, int step) {
        if (mercury.ended) return;
        if (!Spells.casterStillHere(player, sl) || sl.getGameTime() >= mercury.until) {
            reform(player, sl, mercury);
            return;
        }
        if (step % 2 == 0) HandOfGodFx.mercuryTick(sl, player);
        switch (mercury.mode) {
            case SLIDE -> {
                for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), MERCURY_WRAP_REACH)) {
                    if (!mercury.wrapped.add(enemy.getUUID())) continue;
                    Root.apply(enemy, MERCURY_WRAP_TICKS, RootRestriction.MOVE, RootRestriction.JUMP, RootRestriction.ATTACK);
                    HandOfGodFx.mercuryWrap(sl, enemy, MERCURY_WRAP_TICKS);
                }
            }
            case ARMOR, COAT -> {
                LivingEntity host = mercury.host;
                if (!host.isAlive() || host.level() != sl) {
                    reform(player, sl, mercury);
                    return;
                }
                if (step % 2 == 0) player.teleportTo(host.getX(), host.getY(), host.getZ());    // the silver goes where it goes
                if (mercury.mode == MercuryMode.COAT && step > 0 && step % 20 == 0) Spells.strike(player, host, MERCURY_CHOKE);
            }
        }
        Later.run(sl, 1, () -> mercuryStep(player, sl, mercury, step + 1));
    }

    /** The silver draws together into the knight again (beside the host, if it coated one). */
    private static void reform(LivingEntity player, ServerLevel sl, Mercury mercury) {
        if (mercury.ended) return;
        mercury.ended = true;
        MERCURY.remove(player.getUUID(), mercury);
        if (mercury.host != null) {
            if (mercury.mode == MercuryMode.ARMOR) ConjuredArmor.takeOff(mercury.host, MERCURY_ARMOR);
            if (mercury.mode == MercuryMode.COAT) Root.remove(mercury.host);
            if (player.level() == sl && mercury.host.level() == sl) {
                Vec3 beside = Spells.freeSpotNear(player, sl, mercury.host.position(), player.position().subtract(mercury.host.position()), 1.5);
                if (beside != null) player.teleportTo(beside.x, beside.y, beside.z);
            }
        }
        if (Concealment.isHidden(player)) Concealment.reveal(player);
        FallGuard.protect(player, 60);
        HandOfGodFx.statusEnd(player, Status.MERCURY);
        if (player.level() instanceof ServerLevel level) HandOfGodFx.mercuryReform(level, player, mercury.tag);
    }

    /** The ally under the crosshair (not the caster), or null. */
    private static LivingEntity aimAlly(LivingEntity player, ServerLevel sl, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 point = Spells.aimPoint(player, sl, range);
        return sl.getEntitiesOfClass(LivingEntity.class, new AABB(eye, point).inflate(1.0),
                        one -> one != player && one.isAlive() && Spells.isFriend(player, one)
                                && one.getBoundingBox().inflate(0.4).clip(eye, point).isPresent())
                .stream().min(Comparator.comparingDouble(one -> one.distanceToSqr(player))).orElse(null);
    }

    // =========================
    // Silver Rapier: RAPIER_COUNT rapiers form round the enemy you look at (RAPIER_RANGE) and stab it at
    // HandOfGodFx.RAPIER_STRIKES, RAPIER_DAMAGE each: nowhere to dodge to.
    // =========================
    static AbilityWheel.Result silverRapier(LivingEntity player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, RAPIER_RANGE);
        if (target == null) {
            player.sendSystemMessage(Component.literal("There is no enemy before your eyes."));
            return AbilityWheel.Result.REFUSED;
        }
        HandOfGodFx.silverRapiers(sl, target, RAPIER_COUNT);
        Later.run(sl, HandOfGodFx.RAPIER_STRIKES, () -> {
            if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) return;
            for (int i = 0; i < RAPIER_COUNT && target.isAlive(); i++) Spells.strike(player, target, RAPIER_DAMAGE);
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Light Concealment: step into a light within LIGHT_ENTRY_REACH of you and out of the light you aim at (within
    // LIGHT_AROUND of the crosshair, LIGHT_RANGE away), hidden for LIGHT_TRAVEL on the way. By day the open sky is a
    // light. With Mind Concealment up you stay hidden LIGHT_HIDDEN_AFTER after it.
    // Glory (2): with no light to go in or out by, you wither away instead and re-form wherever you aim.
    // =========================
    static AbilityWheel.Result lightConcealment(LivingEntity player, ServerLevel sl) {
        boolean glory = Beyonder.hasSequence(player, WITHERING_SEQUENCE);
        List<BlockPos> near = Spells.lightNear(sl, player.getEyePosition(), LIGHT_ENTRY_REACH, LIGHT_MIN);
        Vec3 entry = !near.isEmpty()
                ? new Vec3(
                near.get(0).getX() + 0.5,
                near.get(0).getY() + 0.5,
                near.get(0).getZ() + 0.5
        )
                : Spells.inDaylight(sl, BlockPos.containing(player.getEyePosition())) ? player.getEyePosition().add(Spells.flatLook(player)) : null;
        Vec3 exit = null;
        BlockPos aimedLight = Spells.lightAimed(player, sl, LIGHT_RANGE, LIGHT_AROUND, LIGHT_MIN);
        if (aimedLight != null) exit = standBeside(player, sl, aimedLight);
        if (exit == null) {
            Vec3 spot = Spells.blinkSpot(player, sl, LIGHT_RANGE);
            if (spot != null && Spells.inDaylight(sl, BlockPos.containing(spot.x, spot.y + 1, spot.z))) exit = spot;
        }
        boolean withering = entry == null || exit == null;
        if (withering) {
            if (!glory) {
                player.sendSystemMessage(Component.literal(entry == null ? "There is no light near you to step into."
                        : "There is no light where you look to step out of."));
                return AbilityWheel.Result.REFUSED;
            }
            exit = Spells.blinkSpot(player, sl, LIGHT_RANGE);
            if (exit == null) {
                player.sendSystemMessage(Component.literal("There is no room where you look."));
                return AbilityWheel.Result.REFUSED;
            }
        }

        Concealment.hide(player, LIGHT_TRAVEL + 5);
        if (withering) HandOfGodFx.withering(sl, player);
        else HandOfGodFx.lightEnter(sl, player, entry);
        HandOfGodFx.status(player, Status.LIGHT_CONCEALMENT, LIGHT_TRAVEL);
        Vec3 out = exit;
        Later.run(sl, LIGHT_TRAVEL, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            player.teleportTo(out.x, out.y, out.z);
            FallGuard.protect(player, 60);
            HandOfGodFx.lightExit(sl, player, out);
            if (HandOfGodDemonHunting.mindConcealed(player)) Concealment.hide(player, LIGHT_HIDDEN_AFTER);
            else if (Concealment.isHidden(player)) Concealment.reveal(player);
        });
        return AbilityWheel.Result.DONE;
    }

    private static Vec3 blockCenter(BlockPos pos) {
        return new Vec3(
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5
        );
    }

    /** Where to stand to step out of a light: beside it, on the ground, with room for the body. Null if nowhere. */
    private static Vec3 standBeside(LivingEntity player, ServerLevel sl, BlockPos light) {
        Vec3 middle = new Vec3(
                light.getX() + 0.5,
                light.getY() + 0.5,
                light.getZ() + 0.5
        );
        Vec3 toPlayer = player.position().subtract(middle);
        Vec3 side = new Vec3(toPlayer.x, 0, toPlayer.z);
        side = side.lengthSqr() < 1.0E-4 ? Spells.flatLook(player).scale(-1) : side.normalize();
        for (Vec3 offset : new Vec3[]{Vec3.ZERO, side, Spells.turned(side, 90), Spells.turned(side, -90), side.scale(-1)}) {
            Vec3 ground = Spells.groundNear(player, sl, middle.add(offset), 2.0);
            if (ground != null && Spells.fits(player, sl, ground)) return ground;
        }
        return Spells.freeSpotNear(player, sl, middle, toPlayer, 1.5);
    }

    // =========================
    // Twilight Sword: a sword of Glory falls out of the void onto the enemy you aim at (or the ground you aim at,
    // SWORD_RANGE): at HandOfGodFx.TWILIGHT_SWORD_HITS every enemy within SWORD_RADIUS takes SWORD_DAMAGE, through
    // Protection, Substitutes and death wards (Spells.strikeThrough), and decays.
    // =========================
    static AbilityWheel.Result twilightSword(LivingEntity player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, SWORD_RANGE);
        Vec3 ground = target != null ? Spells.groundUnder(player, sl, target) : Spells.aimGround(player, sl, SWORD_RANGE);
        HandOfGodFx.twilightSword(sl, ground, SWORD_RADIUS);
        Later.run(sl, HandOfGodFx.TWILIGHT_SWORD_HITS, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            for (LivingEntity enemy : Spells.enemiesAround(player, sl, ground.add(0, 0.5, 0), SWORD_RADIUS)) {
                Spells.strikeThrough(player, enemy, SWORD_DAMAGE);
                if (!enemy.isAlive()) continue;
                Decay.apply(enemy, SWORD_DECAY_TICKS, SWORD_DECAY_DEPTH);
                Spells.push(enemy, ground, 0.6, 0.35);
            }
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Divine Right Hand: a hand of Twilight seizes the enemy you aim at (HAND_RANGE; not a boss) at
    // HandOfGodFx.HAND_GRIPS, holds it HAND_HOLD (HAND_SQUEEZE each second), then flings it away for HAND_FLING and
    // decays it. Aimed at the ground or a boss: it slams down there at HandOfGodFx.HAND_SLAMS, and everything within
    // HAND_SLAM_RADIUS takes HAND_SLAM_DAMAGE, is pressed into the ground (status/Burial) and decays.
    // =========================
    static AbilityWheel.Result divineRightHand(LivingEntity player, ServerLevel sl) {
        LivingEntity target = Spells.aimEnemy(player, sl, HAND_RANGE);
        if (target != null && !isBoss(target)) seize(player, sl, target);
        else slam(player, sl, target != null ? Spells.groundUnder(player, sl, target) : Spells.aimGround(player, sl, HAND_RANGE));
        return AbilityWheel.Result.DONE;
    }

    static boolean isBoss(LivingEntity one) {
        return one.getType() == EntityTypes.ENDER_DRAGON || one.getType() == EntityTypes.WITHER
                || one.getType() == EntityTypes.WARDEN || one.getType() == EntityTypes.ELDER_GUARDIAN
                || one.getMaxHealth() > BOSS_HEALTH;
    }

    private static void seize(LivingEntity player, ServerLevel sl, LivingEntity target) {
        int grips = HandOfGodFx.HAND_GRIPS;
        String tag = "hog_hand_" + player.getId() + "_" + sl.getGameTime();
        HandOfGodFx.divineHand(sl, player, target, grips + HAND_HOLD + 6, tag);
        HandOfGodFx.status(player, Status.DIVINE_HAND, grips + HAND_HOLD);
        Later.run(sl, grips, () -> {
            if (target.isAlive() && target.level() == sl) Root.apply(target, HAND_HOLD, RootRestriction.EVERYTHING);
        });
        for (int second = 1; second <= HAND_HOLD / 20; second++) {
            boolean last = second == HAND_HOLD / 20;
            Later.run(sl, grips + second * 20, () -> {
                if (!Spells.casterStillHere(player, sl) || !target.isAlive() || target.level() != sl) {
                    if (target.level() == sl) HandOfGodFx.divineHandFling(sl, target, tag);
                    return;
                }
                if (!last) {
                    Spells.strike(player, target, HAND_SQUEEZE);
                    return;
                }
                Root.remove(target);
                Spells.strike(player, target, HAND_SQUEEZE + HAND_FLING);
                HandOfGodFx.divineHandFling(sl, target, tag);
                if (!target.isAlive()) return;
                Spells.push(target, player.position(), HAND_FLING_STRENGTH, HAND_FLING_LIFT);
                Decay.apply(target, HAND_DECAY_TICKS, HAND_DECAY_DEPTH);
            });
        }
    }

    private static void slam(LivingEntity player, ServerLevel sl, Vec3 ground) {
        HandOfGodFx.divineHandSlam(sl, player, ground, HAND_SLAM_RADIUS);
        Later.run(sl, HandOfGodFx.HAND_SLAMS, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            for (LivingEntity enemy : Spells.enemiesAround(player, sl, ground.add(0, 0.5, 0), HAND_SLAM_RADIUS)) {
                Spells.strike(player, enemy, HAND_SLAM_DAMAGE);
                if (!enemy.isAlive()) continue;
                Decay.apply(enemy, HAND_DECAY_TICKS, HAND_DECAY_DEPTH);
                if (Burial.canBury(enemy)) Burial.bury(enemy, 10, HAND_SLAM_HOLD);
                else Root.apply(enemy, HAND_SLAM_HOLD, RootRestriction.EVERYTHING);
            }
        });
    }

    // =========================
    // Passage of Time: a clock spreads TIME_RADIUS round you; at HandOfGodFx.TIME_FROZEN every enemy in it stands
    // still for TIME_TICKS (nothing they do, nothing they think). What they take meanwhile is written down
    // (status/StoredDamage) and lands again, TIME_DUE times over, when time resumes.
    // =========================
    static AbilityWheel.Result passageOfTime(LivingEntity player, ServerLevel sl) {
        Vec3 center = Spells.groundUnder(player, sl, player);
        String tag = "hog_time_" + player.getId() + "_" + sl.getGameTime();
        int frozen = HandOfGodFx.TIME_FROZEN;
        HandOfGodFx.passageOfTime(sl, center, TIME_RADIUS, frozen + TIME_TICKS, tag);
        HandOfGodFx.status(player, Status.TIME_STOP, frozen + TIME_TICKS);
        Later.run(sl, frozen, () -> {
            if (!Spells.casterStillHere(player, sl)) return;
            for (LivingEntity enemy : Spells.enemiesAround(player, sl, center, TIME_RADIUS)) {
                String mine = tag + "_" + enemy.getId();
                Root.apply(enemy, TIME_TICKS, RootRestriction.EVERYTHING);
                HandOfGodFx.timeFrozen(sl, enemy, TIME_TICKS, mine);
                StoredDamage.start(enemy, player, TIME_TICKS, null, (victim, total, level) -> {
                    HandOfGodFx.timeResume(level, victim, mine);
                    if (total > 0f) StoredDamage.deal(victim, player, total * TIME_DUE);
                });
            }
        });
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Decay: Twilight shines over DECAY_RADIUS where you aim (DECAY_RANGE) for DECAY_TICKS. Every second each enemy
    // in it rots (status/Decay: no healing, wither, weakness, its gear wears out), its health lower by DECAY_DEPTH and
    // DECAY_DEEPER more for every second it stays, DECAY_DEEPEST at most.
    // =========================
    static AbilityWheel.Result decay(LivingEntity player, ServerLevel sl) {
        Vec3 center = Spells.aimGround(player, sl, DECAY_RANGE);
        String tag = "hog_decay_" + player.getId() + "_" + sl.getGameTime();
        HandOfGodFx.decay(sl, center, DECAY_RADIUS, DECAY_TICKS, tag);
        HandOfGodFx.status(player, Status.DECAY, DECAY_TICKS);
        Zone.at(center, DECAY_RADIUS).height(6).lasts(DECAY_TICKS).every(20)
                .onInside((enemy, inside) -> {
                    float depth = Math.min(DECAY_DEEPEST, DECAY_DEPTH + DECAY_DEEPER * (inside / 20f));
                    if (Decay.apply(enemy, 40, depth)) HandOfGodFx.decayOn(sl, enemy);
                })
                .onPulse(left -> HandOfGodFx.decayTick(sl, center, DECAY_RADIUS))
                .onEnd(() -> HandOfGodFx.decayEnd(sl, center, tag))
                .open(player, sl, DECAY_ZONE);
        return AbilityWheel.Result.DONE;
    }

    // =========================
    // Divine Kingdom: a frozen kingdom of Twilight KINGDOM_RADIUS round where you stand, for KINGDOM_TICKS. Every
    // second, enemies in it are slowed, weakened and cannot heal, and all they deal is KINGDOM_ENEMY_DAMAGE as much;
    // you and your allies are acknowledged: Strength, Resistance and Regeneration. Cast again: it ends (free).
    // =========================
    static AbilityWheel.Result divineKingdom(LivingEntity player, ServerLevel sl) {
        if (Zone.isOpen(player, KINGDOM_ZONE)) {
            Zone.close(player, KINGDOM_ZONE);
            return AbilityWheel.Result.FREE;
        }
        Vec3 center = Spells.groundUnder(player, sl, player);
        String tag = "hog_kingdom_" + player.getId() + "_" + sl.getGameTime();
        HandOfGodFx.divineKingdom(sl, player, KINGDOM_RADIUS, KINGDOM_TICKS, tag);
        HandOfGodFx.status(player, Status.KINGDOM, KINGDOM_TICKS);
        KINGDOMS.put(player.getUUID(), new Kingdom(player, sl, center));
        Zone.at(center, KINGDOM_RADIUS).height(KINGDOM_HEIGHT).lasts(KINGDOM_TICKS).every(20)
                .affects(one -> true)                                           // friend and foe: kingdomTouches sorts them
                .onEnter(one -> {
                    if (Spells.isFriend(player, one)) HandOfGodFx.kingdomAcknowledged(sl, one);
                })
                .onInside((one, inside) -> kingdomTouches(player, one))
                .onPulse(left -> {
                    HandOfGodFx.kingdomTick(sl, center, KINGDOM_RADIUS);
                    if (player.position().distanceTo(center) <= KINGDOM_RADIUS) kingdomTouches(player, player);
                })
                .onEnd(() -> {
                    KINGDOMS.remove(player.getUUID());
                    HandOfGodFx.kingdomEnd(sl, player, center, tag);
                    HandOfGodFx.statusEnd(player, Status.KINGDOM);
                })
                .open(player, sl, KINGDOM_ZONE);
        return AbilityWheel.Result.DONE;
    }

    private static void kingdomTouches(LivingEntity player, LivingEntity one) {
        if (Spells.isFriend(player, one)) {
            Spells.keepEffect(one, MobEffects.STRENGTH, 0, 40);
            Spells.keepEffect(one, MobEffects.RESISTANCE, 0, 40);
            Spells.keepEffect(one, MobEffects.REGENERATION, 0, 60);
        } else if (Spells.isEnemy(player, one)) {
            Spells.keepEffect(one, MobEffects.SLOWNESS, 0, 40);
            Spells.keepEffect(one, MobEffects.WEAKNESS, 0, 40);
            NoHeal.apply(one, 40);
        }
    }

    // =========================
    // Called from HandOfGod
    // =========================

    /** A hit is landing (any hit): an enemy of a Divine Kingdom's owner, standing in it, deals less. */
    static void kingdomCut(LivingHurtEvent event) {
        if (KINGDOMS.isEmpty() || !(event.getSource().getEntity() instanceof LivingEntity attacker)) return;
        for (Kingdom kingdom : KINGDOMS.values()) {
            if (kingdom.level() != attacker.level() || attacker == kingdom.owner()) continue;
            if (attacker.position().distanceTo(kingdom.center()) > KINGDOM_RADIUS) continue;
            if (!Spells.isEnemy(kingdom.owner(), attacker)) continue;
            event.setAmount(event.getAmount() * KINGDOM_ENEMY_DAMAGE);
            return;
        }
    }

    /** Logging out or dying while liquid: the silver lets go of its host. */
    static void clearState(LivingEntity player) {
        Mercury mercury = MERCURY.remove(player.getUUID());
        if (mercury == null) return;
        mercury.ended = true;
        if (mercury.host != null && mercury.mode == MercuryMode.ARMOR) ConjuredArmor.takeOff(mercury.host, MERCURY_ARMOR);
        if (mercury.host != null && mercury.mode == MercuryMode.COAT) Root.remove(mercury.host);
    }

    /** Logging out: a kingdom with no king falls (its zone closes by itself; this forgets where it stood). */
    static void forget(LivingEntity player) {
        clearState(player);
        KINGDOMS.remove(player.getUUID());
    }
}
