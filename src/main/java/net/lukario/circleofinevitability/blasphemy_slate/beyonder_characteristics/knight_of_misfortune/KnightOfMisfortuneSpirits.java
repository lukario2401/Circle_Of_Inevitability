package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortuneFx.Status;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.menu.AbilityWheel;
import net.lukario.circleofinevitability.status.Blind;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.FallGuard;
import net.lukario.circleofinevitability.status.Fear;
import net.lukario.circleofinevitability.status.Flight;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Slumber;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Knight Of Misfortune, key 5: the Spirit Commanding wheel (Spirit Warlock, Sequence 5). docs/KNIGHT_OF_MISFORTUNE.md,
 * "Key 5 wheel".
 *
 *   Terror Banshee   a wailing spirit floats at your shoulder for BANSHEE_TICKS; every BANSHEE_WAIL_EVERY it shrieks:
 *                    enemies round you are frightened (status/Fear), blinded and slowed. It has no body to strike.
 *   Earthen Spirit   you fuse with the earth: hidden and untouchable (status/Concealment), very fast, while you stay on
 *                    earth (dirt, stone, sand). Pick it again to rise where you aim (up to EARTH_RISE_RANGE, onto earth).
 *   Gale Spirit      a wind spirit lifts you: you fly (status/Flight) for GALE_TICKS, but cannot strike with a weapon
 *                    while you do. Pick it again to come down.
 *   Command Spirits  (Knight of Misfortune, 1) the undead and the animals round you fall asleep, then the nearest
 *                    COMMAND_MOST wake as yours for COMMAND_TICKS (Marionette.summon: when it ends they return to the
 *                    darkness, with no drops).
 *
 * The spirits live in your teeth: TEETH (TEETH_NIGHTWATCHER from Nightwatcher) may be out at once (a banshee, the
 * earth, the gale); a spirit that ends frees its tooth.
 *
 * Sneak casts the last pick again without the wheel (no cooldown, double the cost), as for the other wheels.
 * The looks are KnightOfMisfortuneFx's; every number is right below.
 */
public final class KnightOfMisfortuneSpirits {

    private KnightOfMisfortuneSpirits() {}

    // =========================
    // Settings (cost = spirituality, ticks: 20 = 1 second, distances in blocks)
    // =========================

    static final int SPIRIT_SEQUENCE = 5;                        // the key
    static final float QUICK_CAST_COST = 2f;
    static final int TEETH = 4;
    static final int TEETH_NIGHTWATCHER = 8;             // Nightwatcher (4): each tooth holds one more

    static final int BANSHEE_SEQUENCE = 5;
    static final int BANSHEE_COST = 3000;
    static final int BANSHEE_COOLDOWN = 600;
    static final int BANSHEE_TICKS = 600;
    static final int BANSHEE_WAIL_EVERY = 60;
    static final double BANSHEE_RADIUS = 10.0;
    static final int BANSHEE_FEAR = 60;
    static final int BANSHEE_BLIND = 40;

    static final int EARTH_SEQUENCE = 5;
    static final int EARTH_COST = 3000;
    static final int EARTH_COOLDOWN = 600;
    static final int EARTH_TICKS = 160;
    static final int EARTH_SPEED = 3;                    // Speed IV under the ground
    static final double EARTH_RISE_RANGE = 24.0;

    static final int GALE_SEQUENCE = 5;
    static final int GALE_COST = 3000;
    static final int GALE_COOLDOWN = 900;
    static final int GALE_TICKS = 400;

    static final int COMMAND_SEQUENCE = 1;
    static final int COMMAND_COST = 30000;
    static final int COMMAND_COOLDOWN = 2400;
    static final double COMMAND_RADIUS = 40.0;
    static final int COMMAND_SLEEP = 60;                 // they all sleep this long...
    static final int COMMAND_MOST = 8;                   // ...then the nearest this many wake as yours...
    static final float COMMAND_HEALTH = 80f;             // ...if their maximum health is at most this...
    static final int COMMAND_TICKS = 800;                // ...for this long

    // =========================
    // Saved on the player
    // =========================
    static final String EARTH_UNTIL = "knight_of_misfortune_earth_until";
    static final String EARTH_TAG = "knight_of_misfortune_earth_tag";
    static final String GALE_UNTIL = "knight_of_misfortune_gale_until";
    static final String GALE_TAG = "knight_of_misfortune_gale_tag";

    /** The spirits each player has out (when each one ends), for the teeth. Not saved. */
    static final Map<UUID, List<Long>> OUT = new HashMap<>();

    // =========================
    // The key
    // =========================

    static final AbilityWheel WHEEL = AbilityWheel.create("knight_of_misfortune_spirits", "Spirit Commanding")
            .radius(64)
            .quickCastCooldown(false).quickCastCost(QUICK_CAST_COST)
            .entry("terror_banshee", KnightOfMisfortuneFx.WHEEL_TERROR_BANSHEE, "Terror Banshee", BANSHEE_SEQUENCE,
                    BANSHEE_COST, BANSHEE_COOLDOWN, KnightOfMisfortuneSpirits::terrorBanshee)
            .entry("earthen_spirit", KnightOfMisfortuneFx.WHEEL_EARTHEN_SPIRIT, "Earthen Spirit", EARTH_SEQUENCE,
                    EARTH_COST, EARTH_COOLDOWN, KnightOfMisfortuneSpirits::earthenSpirit)
            .free(player -> KnightOfMisfortune.isActive(player, EARTH_UNTIL))                // rising out of it
            .entry("gale_spirit", KnightOfMisfortuneFx.WHEEL_GALE_SPIRIT, "Gale Spirit", GALE_SEQUENCE,
                    GALE_COST, GALE_COOLDOWN, KnightOfMisfortuneSpirits::galeSpirit)
            .free(player -> KnightOfMisfortune.isActive(player, GALE_UNTIL))                 // coming down
            .entry("command_spirits", KnightOfMisfortuneFx.WHEEL_COMMAND_SPIRITS, "Command Spirits", COMMAND_SEQUENCE,
                    COMMAND_COST, COMMAND_COOLDOWN, KnightOfMisfortuneSpirits::commandSpirits);

    // =========================
    // Ability 5 - Spirit Commanding
    // Normal: the spirit wheel. Sneak: the last pick again, without the wheel.
    // =========================
    public static void knightOfMisfortuneSpiritCommanding(Player player, Level level, ServerLevel sl, boolean bypassClassCheck) {
        if (!KnightOfMisfortune.ready(player, bypassClassCheck, SPIRIT_SEQUENCE)) return;
        if (player.isShiftKeyDown()) WHEEL.quickCast(player);
        else WHEEL.open(player);
    }

    // =========================
    // Terror Banshee
    // =========================

    static AbilityWheel.Result terrorBanshee(LivingEntity player, ServerLevel sl) {
        if (!freeTooth(player, sl)) return AbilityWheel.Result.REFUSED;
        long now = sl.getGameTime();
        String tag = "km_banshee_" + player.getId() + "_" + now;
        out(player, now + BANSHEE_TICKS);
        KnightOfMisfortuneFx.spiritRelease(sl, player, player.position().add(0, 1.6, 0));
        KnightOfMisfortuneFx.banshee(sl, player, BANSHEE_TICKS, tag);
        KnightOfMisfortuneFx.status(player, Status.BANSHEE, BANSHEE_TICKS);
        for (int t = 20; t < BANSHEE_TICKS; t += BANSHEE_WAIL_EVERY) {
            Later.run(sl, t, () -> {
                if (!Spells.casterStillHere(player, sl)) return;
                wail(player, sl);
            });
        }
        Later.run(sl, BANSHEE_TICKS, () -> {
            if (Spells.casterStillHere(player, sl)) KnightOfMisfortuneFx.bansheeEnd(sl, player, tag);
        });
        return AbilityWheel.Result.DONE;
    }

    private static void wail(LivingEntity player, ServerLevel sl) {
        KnightOfMisfortuneFx.bansheeWail(sl, player, BANSHEE_RADIUS);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, player.position(), BANSHEE_RADIUS)) {
            if (Fear.apply(enemy, player, BANSHEE_FEAR)) KnightOfMisfortune.showFear(sl, enemy, BANSHEE_FEAR);
            Blind.apply(enemy, BANSHEE_BLIND);
            Spells.keepEffect(enemy, MobEffects.SLOWNESS, 1, BANSHEE_BLIND);
        }
    }

    // =========================
    // Earthen Spirit
    // =========================

    static AbilityWheel.Result earthenSpirit(LivingEntity player, ServerLevel sl) {
        if (KnightOfMisfortune.isActive(player, EARTH_UNTIL)) {           // the second pick: rise where you aim
            rise(player, sl, true);
            return AbilityWheel.Result.FREE;
        }
        if (!onEarth(sl, player.blockPosition().below())) {
            player.sendSystemMessage(Component.literal("There is no earth under you to sink into."));
            return AbilityWheel.Result.REFUSED;
        }
        if (!freeTooth(player, sl)) return AbilityWheel.Result.REFUSED;
        long now = sl.getGameTime();
        String tag = "km_earth_" + player.getId() + "_" + now;
        CompoundTag data = player.getPersistentData();
        data.putLong(EARTH_UNTIL, now + EARTH_TICKS);
        data.putString(EARTH_TAG, tag);
        out(player, now + EARTH_TICKS);
        KnightOfMisfortuneFx.spiritRelease(sl, player, player.position());
        Concealment.hide(player, EARTH_TICKS);
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, EARTH_TICKS, EARTH_SPEED, false, false));
        KnightOfMisfortuneFx.earthMerge(sl, player, EARTH_TICKS, tag);
        KnightOfMisfortuneFx.status(player, Status.EARTH, EARTH_TICKS);
        return AbilityWheel.Result.DONE;
    }

    /** Out of the earth: where you aim (aimed, onto earth within EARTH_RISE_RANGE) or where you are. */
    private static void rise(LivingEntity player, ServerLevel sl, boolean aimed) {
        CompoundTag data = player.getPersistentData();
        String tag = data.getStringOr(EARTH_TAG, "");
        data.remove(EARTH_UNTIL);
        data.remove(EARTH_TAG);
        if (aimed) {
            Vec3 spot = Spells.blinkSpot(player, sl, EARTH_RISE_RANGE);
            if (spot != null && onEarth(sl, BlockPos.containing(spot).below())) player.teleportTo(spot.x, spot.y, spot.z);
        }
        Concealment.reveal(player);
        MobEffectInstance speed = player.getEffect(MobEffects.SPEED);
        if (speed != null && speed.getAmplifier() == EARTH_SPEED) player.removeEffect(MobEffects.SPEED);
        FallGuard.protect(player, 40);
        KnightOfMisfortuneFx.earthEmerge(sl, player, tag);
        KnightOfMisfortuneFx.statusEnd(player, Status.EARTH);
    }

    /** Earth an Earthen Spirit can move through: dirt, grass, stone, sand and the like. */
    private static boolean onEarth(ServerLevel sl, BlockPos below) {
        BlockState state = sl.getBlockState(below);
        return state.is(BlockTags.DIRT) || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.SAND)
                || state.is(BlockTags.BASE_STONE_NETHER);
    }

    // =========================
    // Gale Spirit
    // =========================

    private static AbilityWheel.Result galeSpirit(ServerPlayer player, ServerLevel sl) {
        if (KnightOfMisfortune.isActive(player, GALE_UNTIL)) {            // the second pick: come down
            Flight.end(player);
            galeEnd(player, sl);
            return AbilityWheel.Result.FREE;
        }
        if (!freeTooth(player, sl)) return AbilityWheel.Result.REFUSED;
        long now = sl.getGameTime();
        String tag = "km_gale_" + player.getId() + "_" + now;
        CompoundTag data = player.getPersistentData();
        data.putLong(GALE_UNTIL, now + GALE_TICKS);
        data.putString(GALE_TAG, tag);
        out(player, now + GALE_TICKS);
        KnightOfMisfortuneFx.spiritRelease(sl, player, player.position().add(0, 1, 0));
        Flight.grant(player, GALE_TICKS);
        KnightOfMisfortuneFx.gale(sl, player, GALE_TICKS, tag);
        KnightOfMisfortuneFx.status(player, Status.GALE, GALE_TICKS);
        return AbilityWheel.Result.DONE;
    }

    private static void galeEnd(LivingEntity player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        String tag = data.getStringOr(GALE_TAG, "");
        data.remove(GALE_UNTIL);
        data.remove(GALE_TAG);
        KnightOfMisfortuneFx.galeEnd(sl, player, tag);
        KnightOfMisfortuneFx.statusEnd(player, Status.GALE);
    }

    /** True while a gale spirit carries the player: they cannot strike with a weapon. */
    static boolean gliding(LivingEntity player) {
        return KnightOfMisfortune.isActive(player, GALE_UNTIL);
    }

    // =========================
    // Command Spirits
    // =========================

    static AbilityWheel.Result commandSpirits(LivingEntity player, ServerLevel sl) {
        List<Mob> answer = sl.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(COMMAND_RADIUS),
                mob -> commandable(player, mob));
        if (answer.isEmpty()) {
            player.sendSystemMessage(Component.literal("No spirit is near enough to answer."));
            return AbilityWheel.Result.REFUSED;
        }
        answer.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(player)));
        KnightOfMisfortuneFx.commandSpirits(sl, player, COMMAND_RADIUS);
        int taken = 0;
        for (Mob mob : answer) {
            if (Slumber.put(mob, COMMAND_SLEEP, Slumber.Kind.SLEEP)) KnightOfMisfortune.showSlumber(sl, mob, COMMAND_SLEEP);
            if (taken >= COMMAND_MOST || mob.getMaxHealth() > COMMAND_HEALTH) continue;
            taken++;
            Later.run(sl, COMMAND_SLEEP, () -> {
                if (!mob.isAlive() || !Spells.casterStillHere(player, sl)) return;
                Slumber.wake(mob);
                if (Marionette.summon(mob, player, COMMAND_TICKS)) {
                    KnightOfMisfortuneFx.spiritBound(sl, mob, COMMAND_TICKS, "km_bound_" + mob.getId());
                }
            });
        }
        player.sendSystemMessage(Component.literal(taken + " spirits answer your command."));
        return AbilityWheel.Result.DONE;
    }

    /** The undead and the animals, not yet anyone's, not a boss, not on the caster's side. */
    private static boolean commandable(LivingEntity player, Mob mob) {
        if (!mob.isAlive() || Marionette.isMarionette(mob) || Spells.isFriend(player, mob) || KnightOfMisfortune.isBoss(mob)) return false;
        return mob.is(EntityTypeTags.UNDEAD) || mob.getType().getCategory() == MobCategory.CREATURE;
    }

    // =========================
    // The teeth
    // =========================

    /** True when a tooth is free (and says so when none is). */
    private static boolean freeTooth(LivingEntity player, ServerLevel sl) {
        int teeth = Beyonder.hasSequence(player, 4) ? TEETH_NIGHTWATCHER : TEETH;
        List<Long> mine = OUT.get(player.getUUID());
        long now = sl.getGameTime();
        if (mine != null) mine.removeIf(until -> until <= now);
        int used = mine == null ? 0 : mine.size();
        if (used < teeth) return true;
        player.sendSystemMessage(Component.literal("Every tooth is empty: no spirit is left to call."));
        return false;
    }

    private static void out(LivingEntity player, long until) {
        OUT.computeIfAbsent(player.getUUID(), id -> new ArrayList<>()).add(until);
    }

    // =========================
    // Called by KnightOfMisfortune
    // =========================

    /** Every tick for every player: the earth and the gale, while they last, and their end. */
    static void tick(LivingEntity player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(EARTH_TAG)) {
            if (!KnightOfMisfortune.isActive(player, EARTH_UNTIL)) {
                rise(player, sl, false);
            } else if (player.tickCount % 10 == 0 && !player.isSpectator()
                    && player.onGround() && !onEarth(sl, player.blockPosition().below())) {
                player.sendSystemMessage(Component.literal("The earth ends: you rise out of it."));
                rise(player, sl, false);
            } else if (player.tickCount % 4 == 0) {
                KnightOfMisfortuneFx.earthTick(sl, player);
            }
        }
        if (data.contains(GALE_TAG)) {
            if (!KnightOfMisfortune.isActive(player, GALE_UNTIL) || !Flight.has(player)) {
                galeEnd(player, sl);
            } else if (player.tickCount % 6 == 0) {
                KnightOfMisfortuneFx.galeTick(sl, player);
            }
        }
    }

    static void clearState(Player player) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(EARTH_TAG)) Concealment.reveal(player);
        for (String key : List.of(EARTH_UNTIL, EARTH_TAG, GALE_UNTIL, GALE_TAG)) data.remove(key);
        OUT.remove(player.getUUID());
    }

    static void forget(LivingEntity player) {
        OUT.remove(player.getUUID());
    }
}
