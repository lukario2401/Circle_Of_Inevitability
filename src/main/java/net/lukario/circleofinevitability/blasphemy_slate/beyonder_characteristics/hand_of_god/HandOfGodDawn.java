package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGodFx.Status;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.combat.Zone;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.status.Blind;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.ConjuredArmor;
import net.lukario.circleofinevitability.status.ConjuredWeapon;
import net.lukario.circleofinevitability.status.Decay;
import net.lukario.circleofinevitability.status.Taunt;
import net.lukario.circleofinevitability.status.Unnoticed;
import net.lukario.circleofinevitability.status.Vulnerable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Hand Of God, key 4: the Dawn wheel (Dawn Paladin, Sequence 6). docs/HAND_OF_GOD.md, "Key 4 wheel".
 *
 *   Light of Dawn       dawn floods the ground round you: hidden enemies are forced out, evil burns, allies are
 *                       cleansed and healed
 *   Dawn Armor          weightless plates of light: less damage, no knockback, a layer of absorption. From Silver
 *                       Knight (3) the Silver Armor: it stays until you cast it again and its layer regrows
 *   Sword of Dawn       a greatsword of light (status/ConjuredWeapon): every hit purifies (the target takes more
 *                       damage, stacking) and burns evil; a full swing throws a crescent. From Silver Knight (3) it
 *                       stays until you cast it again, with no upkeep
 *   Hurricane of Light  the sword gathers the dawn into a hurricane that rolls 20 blocks; allies in its way take a
 *                       quarter. From Glory (2) twice as long and wide, and it decays
 *
 * Sneak casts the last pick again without the wheel (no cooldown, double the cost), as for Beauty Goddess.
 * The looks are HandOfGodFx's; every number is right below.
 */
public final class HandOfGodDawn {

    private HandOfGodDawn() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int DAWN_SEQUENCE = 6;                          // the key
    static final float QUICK_CAST_COST = 2f;            // a sneak recast: no cooldown, this many times the cost

    // Light of Dawn
    static final int LIGHT_SEQUENCE = 6;
    static final int LIGHT_COST = 1500;
    static final int LIGHT_COOLDOWN = 200;
    static final int LIGHT_TICKS = 160;                 // from the moment it is bright (HandOfGodFx.LIGHT_RISEN)
    static final double LIGHT_RADIUS = 12.0;
    static final double LIGHT_HEIGHT = 6.0;
    static final float LIGHT_EVIL_DAMAGE = 6f;          // each second, to evil and undead in it...
    static final int LIGHT_WEAKNESS_TICKS = 60;         // ...which are weakened while they stay
    static final float LIGHT_HEAL = 1f;                 // each second, to you and your allies in it

    // Dawn Armor (Silver Armor from Silver Knight)
    static final int ARMOR_SEQUENCE = 6;
    static final int ARMOR_COST = 1500;
    static final int ARMOR_COOLDOWN = 300;
    static final int ARMOR_TICKS = 900;
    static final float ARMOR_CUT = 0.4f;                // 40% less damage
    static final float ARMOR_ABSORPTION = 8f;           // four hearts of absorption
    static final int SILVER_SEQUENCE = 3;               // no timer: until cast again...
    static final float SILVER_ABSORPTION = 16f;         // ...twice the layer...
    static final int SILVER_REGROW = 40;                // ...and it regrows a point every 2 s without a hit

    // Sword of Dawn
    static final int SWORD_SEQUENCE = 6;
    static final int SWORD_COST = 2000;
    static final int SWORD_COOLDOWN = 200;
    static final int SWORD_TICKS = 800;
    static final float SWORD_UPKEEP = 20f;              // spirituality each second it is out (none from Silver Knight)
    static final float SWORD_DAMAGE = 6f;               // more on every full hit
    static final double SWORD_REACH = 1.0;
    static final float SWORD_PURIFY = 0.05f;            // each hit: the target takes 5% more damage...
    static final float SWORD_PURIFY_MOST = 0.3f;        // ...stacking up to 30%...
    static final int SWORD_PURIFY_TICKS = 200;          // ...for 10 seconds after the last hit
    static final float SWORD_EVIL_DAMAGE = 8f;          // evil and undead burn this much more
    static final float CRESCENT_CHARGE = 0.9f;          // a swing with this much of the attack bar...
    static final double CRESCENT_RANGE = 6.0;           // ...throws a crescent this far...
    static final float CRESCENT_SHARE = 0.6f;           // ...that cuts for this much of the hit
    static final int LASTING_SEQUENCE = 3;              // Silver Knight: no timer, no upkeep

    // Hurricane of Light
    static final int HURRICANE_SEQUENCE = 6;
    static final int HURRICANE_COST = 4500;
    static final int HURRICANE_COOLDOWN = 2400;         // two minutes
    static final double HURRICANE_LENGTH = 20.0;
    static final double HURRICANE_WIDTH = 4.0;
    static final double HURRICANE_HEIGHT = 4.0;
    static final int HURRICANE_TICKS = 24;              // to roll its whole length
    static final float HURRICANE_DAMAGE = 60f;
    static final float HURRICANE_EVIL_DAMAGE = 90f;     // evil and undead
    static final float HURRICANE_ALLY_SHARE = 0.25f;    // it is hard to control: allies in its way take this (0 = none)
    static final double HURRICANE_THROW = 0.9;
    static final int HURRICANE_GLORY_SEQUENCE = 2;      // Glory: twice as long and wide, and it decays
    static final int HURRICANE_DECAY_TICKS = 200;
    static final float HURRICANE_DECAY_DEPTH = 0.2f;

    // =========================
    // Names and what is saved on the player
    // =========================
    static final String LIGHT_ZONE = "hand_of_god_light_of_dawn";
    static final String ARMOR = "hand_of_god_dawn_armor";
    static final String SWORD = "hand_of_god_sword_of_dawn";
    static final String LIGHT_TAG = "hand_of_god_light_tag";         // the newest Light of Dawn
    static final String ARMOR_TAG = "hand_of_god_armor_tag";
    static final String ARMOR_SHOWN = "hand_of_god_armor_shown";       // a lasting armor is shown again after this
    static final String ARMOR_LAST_HIT = "hand_of_god_armor_last_hit";
    static final String SWORD_TAG = "hand_of_god_sword_tag";
    static final String SWORD_SHOWN = "hand_of_god_sword_shown";

    // =========================
    // The key
    // =========================

    private static final AbilityWheel WHEEL = AbilityWheel.create("hand_of_god_dawn", "Dawn")
            .radius(72)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .entry("light_of_dawn", HandOfGodFx.WHEEL_LIGHT_OF_DAWN, "Light of Dawn", LIGHT_SEQUENCE,
                    LIGHT_COST, LIGHT_COOLDOWN, HandOfGodDawn::lightOfDawn)
            .entry("dawn_armor", HandOfGodFx.WHEEL_DAWN_ARMOR, "Dawn Armor", ARMOR_SEQUENCE,
                    ARMOR_COST, ARMOR_COOLDOWN, HandOfGodDawn::dawnArmor)
            .free(player -> lastingArmor(player) && ConjuredArmor.isWorn(player, ARMOR))         // taking it off
            .entry("sword_of_dawn", HandOfGodFx.WHEEL_SWORD_OF_DAWN, "Sword of Dawn", SWORD_SEQUENCE,
                    SWORD_COST, SWORD_COOLDOWN, HandOfGodDawn::swordOfDawn)
            .free(player -> lastingSword(player) && ConjuredWeapon.isDrawn(player, SWORD))       // putting it away
            .entry("hurricane_of_light", HandOfGodFx.WHEEL_HURRICANE, "Hurricane of Light", HURRICANE_SEQUENCE,
                    HURRICANE_COST, HURRICANE_COOLDOWN, HandOfGodDawn::hurricaneOfLight);

    // =========================
    // Ability 4 - Dawn
    // Normal: the Dawn wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void handOfGodDawn(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!HandOfGod.ready(player, bypassClassCheck, DAWN_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Light of Dawn: LIGHT_RADIUS round where you stand, for LIGHT_TICKS once it is bright. Every second: enemies who
    // are hidden (Concealment) or unnoticed are forced out, evil and undead take LIGHT_EVIL_DAMAGE and are weakened;
    // you and your allies lose blindness and taunts, are cleansed and healed LIGHT_HEAL.
    // =========================
    static AbilityWheel.Result lightOfDawn(LivingEntity player, ServerLevel sl) {
        Vec3 ground = Spells.groundUnder(player, sl, player);
        String tag = "hog_dawn_" + player.getId() + "_" + sl.getGameTime();
        player.getPersistentData().putString(LIGHT_TAG, tag);
        int risen = HandOfGodFx.LIGHT_RISEN;
        HandOfGodFx.lightOfDawn(sl, ground, LIGHT_RADIUS, LIGHT_TICKS + risen, tag);
        HandOfGodFx.status(player, Status.LIGHT_OF_DAWN, LIGHT_TICKS + risen);
        Later.run(sl, risen, () -> {
            if (!Spells.casterStillHere(player, sl)) {
                HandOfGodFx.lightOfDawnEnd(sl, ground, tag);
                return;
            }
            Zone.at(ground, LIGHT_RADIUS).height(LIGHT_HEIGHT).lasts(LIGHT_TICKS).every(20)
                    .affects(one -> true)                                       // friend and foe alike: dawnTouches sorts them
                    .onInside((one, inside) -> dawnTouches(player, sl, one))
                    .onPulse(left -> {
                        HandOfGodFx.lightOfDawnPulse(sl, ground, LIGHT_RADIUS);
                        if (player.position().distanceTo(ground) <= LIGHT_RADIUS) dawnTouches(player, sl, player);
                    })
                    .onEnd(() -> {
                        HandOfGodFx.lightOfDawnEnd(sl, ground, tag);
                        // a newer dawn replaced this one: its clock stays
                        if (player.getPersistentData().getStringOr(LIGHT_TAG, "").equals(tag)) HandOfGodFx.statusEnd(player, Status.LIGHT_OF_DAWN);
                    })
                    .open(player, sl, LIGHT_ZONE);
        });
        return AbilityWheel.Result.DONE;
    }

    /** Once a second for each one in the light (the caster included). */
    private static void dawnTouches(LivingEntity player, ServerLevel sl, LivingEntity one) {
        if (!Spells.isFriend(player, one)) {
            if (Concealment.isHidden(one)) {                                    // illusions and hiding break
                Concealment.reveal(one);
                HandOfGodFx.lightOfDawnReveal(sl, one);
            }
            if (Unnoticed.isUnnoticed(one)) {
                Unnoticed.end(one);
                HandOfGodFx.lightOfDawnReveal(sl, one);
            }
            if (Spells.isEvil(one) && Spells.isEnemy(player, one)) {
                Spells.strike(player, one, LIGHT_EVIL_DAMAGE);
                Spells.keepEffect(one, MobEffects.WEAKNESS, 0, LIGHT_WEAKNESS_TICKS);
                HandOfGodFx.lightOfDawnBurn(sl, one);
            }
            return;
        }
        one.removeEffect(MobEffects.BLINDNESS);
        if (Blind.isBlind(one)) Blind.remove(one);
        if (Taunt.isTaunted(one)) Taunt.remove(one);
        Spells.cleanse(one);
        if (one.getHealth() < one.getMaxHealth()) one.heal(LIGHT_HEAL);
        HandOfGodFx.lightOfDawnHeal(sl, one);
    }

    // =========================
    // Dawn Armor: ARMOR_TICKS of plates of light (status/ConjuredArmor): ARMOR_CUT less damage, no knockback, no
    // slowness, ARMOR_ABSORPTION of absorption. Cast again: fresh plates. Silver Knight (3): the Silver Armor stays
    // until cast again (which takes it off, free), with SILVER_ABSORPTION that regrows.
    // =========================
    private static boolean lastingArmor(LivingEntity player) {
        return Beyonder.hasSequence(player, SILVER_SEQUENCE);
    }

    static AbilityWheel.Result dawnArmor(LivingEntity player, ServerLevel sl) {
        boolean silver = lastingArmor(player);
        if (ConjuredArmor.isWorn(player, ARMOR)) {
            ConjuredArmor.takeOff(player, ARMOR);                               // its onEnd clears the look
            if (silver) return AbilityWheel.Result.FREE;                        // the Silver Armor: the key takes it off
        }
        ConjuredArmor.of(ARMOR).lasts(silver ? 0 : ARMOR_TICKS).cut(ARMOR_CUT).steady()
                .absorption(silver ? SILVER_ABSORPTION : ARMOR_ABSORPTION).regrow(silver ? SILVER_REGROW : 0)
                .onHit((wearer, amount) -> armorStruck(wearer, silver))
                .onEnd(wearer -> armorOff(wearer, silver))
                .wear(player);
        showArmor(player, sl, silver);
        return AbilityWheel.Result.DONE;
    }

    private static void showArmor(LivingEntity player, ServerLevel sl, boolean silver) {
        String tag = "hog_armor_" + player.getId() + "_" + sl.getGameTime();
        CompoundTag data = player.getPersistentData();
        data.putString(ARMOR_TAG, tag);
        int ticks = silver ? HandOfGodFx.FOREVER : ARMOR_TICKS;
        data.putLong(ARMOR_SHOWN, sl.getGameTime() + ticks);
        HandOfGodFx.dawnArmor(sl, player, ticks, tag, silver);
        if (silver) HandOfGodFx.statusForever(player, Status.DAWN_ARMOR);
        else HandOfGodFx.status(player, Status.DAWN_ARMOR, ARMOR_TICKS);
    }

    /** A blow lands on the armor: sparks, at most a few times a second. */
    private static void armorStruck(LivingEntity wearer, boolean silver) {
        if (!(wearer instanceof Player player) || !(player.level() instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        if (now - player.getPersistentData().getLongOr(ARMOR_LAST_HIT, 0L) < 5) return;
        player.getPersistentData().putLong(ARMOR_LAST_HIT, now);
        HandOfGodFx.armorHit(sl, player, silver);
    }

    private static void armorOff(LivingEntity wearer, boolean silver) {
        if (!(wearer instanceof Player player)) return;
        CompoundTag data = player.getPersistentData();
        if (player.level() instanceof ServerLevel sl) HandOfGodFx.dawnArmorEnd(sl, player, data.getStringOr(ARMOR_TAG, ""), silver);
        HandOfGodFx.statusEnd(player, Status.DAWN_ARMOR);
        data.remove(ARMOR_TAG);
        data.remove(ARMOR_SHOWN);
    }

    // =========================
    // Sword of Dawn: a greatsword of light for SWORD_TICKS (status/ConjuredWeapon; SWORD_UPKEEP each second). Every
    // hit does SWORD_DAMAGE more and purifies: the target takes SWORD_PURIFY more damage (stacking), evil and undead
    // SWORD_EVIL_DAMAGE more; a full swing also throws a crescent CRESCENT_RANGE ahead. Cast again: drawn afresh.
    // Silver Knight (3): no timer, no upkeep; cast again to put it away (free).
    // =========================
    private static boolean lastingSword(LivingEntity player) {
        return Beyonder.hasSequence(player, LASTING_SEQUENCE);
    }

    static AbilityWheel.Result swordOfDawn(LivingEntity player, ServerLevel sl) {
        if (lastingSword(player) && ConjuredWeapon.isDrawn(player, SWORD)) {
            ConjuredWeapon.putAway(player, SWORD);
            return AbilityWheel.Result.FREE;
        }
        drawSword(player, sl);
        return AbilityWheel.Result.DONE;
    }

    /** The sword forms in the hand (drawn afresh if it is out). Also used by the Hurricane, which needs it. */
    static void drawSword(LivingEntity player, ServerLevel sl) {
        boolean lasting = lastingSword(player);
        ConjuredWeapon sword = ConjuredWeapon.of(SWORD).lasts(lasting ? 0 : SWORD_TICKS)
                .damage(SWORD_DAMAGE).reach(SWORD_REACH)
                .onHit(HandOfGodDawn::purifyingHit)
                .onEnd((wielder, why) -> swordGone(wielder));
        if (!lasting) sword = sword.upkeep(20, SWORD_UPKEEP);
        sword.draw(player);                                                     // an old one goes first (its onEnd clears it)
        showSword(player, sl, lasting);
    }

    private static void showSword(LivingEntity player, ServerLevel sl, boolean lasting) {
        String tag = "hog_sword_" + player.getId() + "_" + sl.getGameTime();
        CompoundTag data = player.getPersistentData();
        data.putString(SWORD_TAG, tag);
        int ticks = lasting ? HandOfGodFx.FOREVER : SWORD_TICKS;
        data.putLong(SWORD_SHOWN, sl.getGameTime() + ticks);
        HandOfGodFx.swordOfDawn(sl, player, ticks, tag);
        if (lasting) HandOfGodFx.statusForever(player, Status.SWORD_OF_DAWN);
        else HandOfGodFx.status(player, Status.SWORD_OF_DAWN, SWORD_TICKS);
    }

    private static float purifyingHit(LivingEntity attacker, LivingEntity target, float amount, float charge, ServerLevel sl) {
        Vulnerable.add(target, SWORD_PURIFY, SWORD_PURIFY_MOST, SWORD_PURIFY_TICKS);
        if (Spells.isEvil(target)) amount += SWORD_EVIL_DAMAGE;
        HandOfGodFx.purify(sl, target);
        boolean crescent = charge >= CRESCENT_CHARGE;
        HandOfGodFx.dawnSlash(sl, attacker, crescent);
        if (crescent) {
            float cut = amount * CRESCENT_SHARE;
            UUID struck = target.getUUID();
            Later.run(sl, 1, () -> {                                            // a tick later: not taken for the swing
                if (!Spells.casterStillHere(attacker, sl)) return;
                Vec3 chest = attacker.position().add(0, attacker.getBbHeight() * 0.6, 0);
                Vec3 end = chest.add(Spells.flatLook(attacker).scale(CRESCENT_RANGE));
                for (LivingEntity enemy : Spells.enemiesOnLine(attacker, sl, chest, end, 0.8)) {
                    if (enemy.getUUID().equals(struck)) continue;
                    Spells.strike(attacker, enemy, cut);
                    HandOfGodFx.purify(sl, enemy);
                }
            });
        }
        return amount;
    }

    private static void swordGone(LivingEntity wielder) {
        CompoundTag data = wielder.getPersistentData();
        if (wielder.level() instanceof ServerLevel sl) HandOfGodFx.swordOfDawnEnd(sl, wielder, data.getStringOr(SWORD_TAG, ""));
        HandOfGodFx.statusEnd(wielder, Status.SWORD_OF_DAWN);
        data.remove(SWORD_TAG);
        data.remove(SWORD_SHOWN);
    }

    // =========================
    // Hurricane of Light: the Sword of Dawn (drawn for it if it is not out) gathers the light for
    // HandOfGodFx.HURRICANE_GATHERS, then a hurricane rolls HURRICANE_LENGTH ahead, HURRICANE_WIDTH wide: each enemy it
    // reaches takes HURRICANE_DAMAGE (HURRICANE_EVIL_DAMAGE if evil) and is thrown along; each ally takes
    // HURRICANE_ALLY_SHARE of it. Glory (2): twice as long and wide, and everything hit is decayed.
    // =========================
    static AbilityWheel.Result hurricaneOfLight(LivingEntity player, ServerLevel sl) {
        if (!ConjuredWeapon.isDrawn(player, SWORD)) drawSword(player, sl);     // it forms first, for nothing more
        boolean glory = Beyonder.hasSequence(player, HURRICANE_GLORY_SEQUENCE);
        HandOfGodFx.hurricaneGather(sl, player);
        Later.run(sl, HandOfGodFx.HURRICANE_GATHERS, () -> {
            if (Spells.casterStillHere(player, sl)) releaseHurricane(player, sl, glory);
        });
        return AbilityWheel.Result.DONE;
    }

    private static void releaseHurricane(LivingEntity player, ServerLevel sl, boolean glory) {
        double length = glory ? HURRICANE_LENGTH * 2 : HURRICANE_LENGTH;
        double half = (glory ? HURRICANE_WIDTH * 2 : HURRICANE_WIDTH) / 2.0;
        int ticks = glory ? HURRICANE_TICKS * 2 : HURRICANE_TICKS;           // the same speed, twice as far
        Vec3 way = Spells.flatLook(player);
        Vec3 start = Spells.groundUnder(player, sl, player).add(way);
        HandOfGodFx.hurricane(sl, start, player.getYRot(), length, ticks, glory);

        Set<UUID> reached = new HashSet<>();
        for (int t = 0; t <= ticks; t++) {
            Vec3 at = start.add(way.scale(length * t / ticks));
            Later.run(sl, t, () -> {
                if (!Spells.casterStillHere(player, sl)) return;
                List<LivingEntity> near = Spells.livingAround(player, sl, at.add(0, HURRICANE_HEIGHT / 2, 0), half + HURRICANE_HEIGHT / 2);
                for (LivingEntity one : near) {
                    double dx = one.getX() - at.x, dz = one.getZ() - at.z;
                    if (Math.sqrt(dx * dx + dz * dz) - one.getBbWidth() / 2 > half) continue;
                    if (one.getY() + one.getBbHeight() < at.y - 1 || one.getY() > at.y + HURRICANE_HEIGHT) continue;
                    if (!reached.add(one.getUUID())) continue;
                    hurricaneReaches(player, sl, one, at.subtract(way.scale(2)), glory);
                }
            });
        }
    }

    private static void hurricaneReaches(LivingEntity player, ServerLevel sl, LivingEntity one, Vec3 behind, boolean glory) {
        if (Spells.isEnemy(player, one)) {
            Spells.strike(player, one, Spells.isEvil(one) ? HURRICANE_EVIL_DAMAGE : HURRICANE_DAMAGE);
            if (one.isAlive()) Spells.push(one, behind, HURRICANE_THROW, 0.4);
        } else if (Spells.isFriend(player, one)) {
            if (HURRICANE_ALLY_SHARE <= 0f) return;
            Spells.strike(player, one, HURRICANE_DAMAGE * HURRICANE_ALLY_SHARE);
        } else {
            return;                                                             // hidden: the light passes it by
        }
        if (glory && one.isAlive()) Decay.apply(one, HURRICANE_DECAY_TICKS, HURRICANE_DECAY_DEPTH);
        HandOfGodFx.hurricaneHit(sl, one, glory);
    }

    // =========================
    // Called from HandOfGod
    // =========================

    /** Once a second: a lasting armor or sword is shown again before its look runs out (after an hour). */
    static void tick(LivingEntity player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        long now = sl.getGameTime();
        if (data.contains(ARMOR_SHOWN) && now >= data.getLongOr(ARMOR_SHOWN, 0L) - 20 && ConjuredArmor.isWorn(player, ARMOR)
                && ConjuredArmor.ticksLeft(player, ARMOR) == Integer.MAX_VALUE) {
            ParticleShapes.clearModels(sl, player, data.getStringOr(ARMOR_TAG, ""));
            showArmor(player, sl, true);
        }
        if (data.contains(SWORD_SHOWN) && now >= data.getLongOr(SWORD_SHOWN, 0L) - 20 && ConjuredWeapon.isDrawn(player, SWORD)
                && ConjuredWeapon.ticksLeft(player, SWORD) == Integer.MAX_VALUE) {
            ParticleShapes.clearModels(sl, player, data.getStringOr(SWORD_TAG, ""));
            showSword(player, sl, true);
        }
    }

    /** After a relog or a death: the armor and sword are gone already (they are not saved); forget their looks. */
    static void clearState(Player player) {
        CompoundTag data = player.getPersistentData();
        for (String key : List.of(LIGHT_TAG, ARMOR_TAG, ARMOR_SHOWN, ARMOR_LAST_HIT, SWORD_TAG, SWORD_SHOWN)) data.remove(key);
    }
}
