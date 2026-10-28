package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonders;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.Beyonder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SoulCore;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionFx.Status;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.combat.Spells;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.root.RootRestriction;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.DeathWard;
import net.lukario.circleofinevitability.status.Exile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Black Emperor's mausoleums (docs/PRINCE_OF_ABOLITION.md, "Mausoleums"). A mausoleum is a black stele trimmed in
 * gold, 3 blocks tall: a picture (PrinceOfAbolitionFx.raiseMausoleum) and a record here, never a block. They belong to
 * their owner alone and help, heal and revive no one else.
 *
 *   - Raise Mausoleum (the Fallen wheel) raises one. MAX at most; the next one lets the oldest crumble.
 *   - It stands LIFE ticks (2 minutes, longer with Sequence Might and Their Order), then crumbles.
 *   - It has HEALTH. A hostile mob hunting its owner that stands next to it strikes it once a second; an enemy player
 *     who swings at it within reach strikes it too. Untouched for REGEN_AFTER it mends REGEN a second.
 *   - Power: every standing one gives its owner MIGHT_PER_MAUSOLEUM more attack and defense (PrinceOfAbolition.attack,
 *     defense), and it is one standing order.
 *   - Rebirth: while one stands, a killing blow does not end its owner (status/DeathWard). They slip out of the world for
 *     REBIRTH_TICKS (hidden and held), then rise at half health from the nearest one, with a Majesty pulse round it;
 *     that mausoleum crumbles.
 *   - Sanctuary (Briber, 7): within SANCTUARY_RADIUS of one, the owner and their allies mend half a heart a second, the
 *     owner's spirituality comes back half as fast again, and enemies are slowed.
 *   - Step, Salvo, Mausoleum Blast and Exploit (PrinceOfAbolitionFallen) and Coronation (PrinceOfAbolitionSovereign)
 *     read the same records. Coronation doubles their bonus while it lasts.
 *   - Last Rebirth (Black Emperor, 0): with none left but LAST_REBIRTH_ORDERS other orders standing, a killing blow
 *     still does not end the owner, once every LAST_REBIRTH_COOLDOWN: out of the world (status/Exile), back where
 *     they fell at a quarter of their health.
 *
 * Only the mausoleums in the owner's own world count. They crumble when their owner logs out or loses the Pathway.
 * Not saved: a server restart forgets them (their pictures are gone with it).
 */
public final class PrinceOfAbolitionMausoleums {

    private PrinceOfAbolitionMausoleums() {}

    static final int MAX = 9;
    static final int LIFE = 2400;
    static final float HEALTH = 80f;
    static final float REGEN = 2f;                      // health a second...
    static final int REGEN_AFTER = 100;                 // ...once nobody struck it for 5 seconds
    static final double HEIGHT = 3.0;
    static final double MOB_REACH = 2.2;                // a mob hunting the owner this close strikes it...
    static final float MOB_BLOW = 3f;                   // ...with its own attack, or this if it has none
    static final double PLAYER_REACH = 3.5;             // an enemy player swinging at it from this close strikes it

    static final int SANCTUARY_SEQUENCE = 7;
    static final double SANCTUARY_RADIUS = 8.0;
    static final float SANCTUARY_HEAL = 1f;             // half a heart a second
    static final float SANCTUARY_SPIRIT = 0.5f;         // spirituality back half as fast again
    static final int SANCTUARY_SLOW = 0;                // Slowness I on enemies in it

    static final int REBIRTH_TICKS = 200;               // out of the world this long...
    static final float REBIRTH_HEALTH = 0.5f;           // ...and back at half health
    static final int WARD_TICKS = 1200;                 // the death ward is armed this long at a time, and armed again
    static final double REBIRTH_MAJESTY = 10.0;         // the rising makes enemies this close kneel...
    static final int REBIRTH_KNEEL = 30;                // ...this long

    // Last Rebirth (Black Emperor): no mausoleum left, but enough other orders standing
    static final int LAST_REBIRTH_SEQUENCE = 0;
    static final int LAST_REBIRTH_ORDERS = 3;           // at least this many orders must stand...
    static final int LAST_REBIRTH_TICKS = 200;          // ...out of the world this long...
    static final float LAST_REBIRTH_HEALTH = 0.25f;     // ...back where they fell at a quarter of their health...
    static final int LAST_REBIRTH_COOLDOWN = 12000;     // ...once every 10 minutes

    static final String WARD = "poa_rebirth_ward";
    static final String LAST_WARD = "poa_last_rebirth_ward";
    static final String LAST_READY = "poa_last_rebirth_ready";       // game time it can save them again
    static final String REBORN_UNTIL = "poa_reborn_until";
    static final String IN_SANCTUARY = "poa_in_sanctuary";

    /** One mausoleum. 'risen': the game time it stands complete (it counts from then). */
    static final class Mausoleum {
        final UUID owner;
        final ServerLevel level;
        final Vec3 ground;
        final float yaw;
        final long risen;
        final String tag;
        long until;
        float health = HEALTH;
        long lastHurt;

        private Mausoleum(UUID owner, ServerLevel level, Vec3 ground, float yaw, long risen, long until, String tag) {
            this.owner = owner;
            this.level = level;
            this.ground = ground;
            this.yaw = yaw;
            this.risen = risen;
            this.until = until;
            this.tag = tag;
        }

        boolean standing() {
            long now = level.getGameTime();
            return now >= risen && now < until;
        }

        /** Its top, where its sigil turns and its beam leaves from. */
        Vec3 top() {
            return ground.add(0, HEIGHT + 0.4, 0);
        }

        /** Where its owner steps out of it: in front of its door. */
        Vec3 door() {
            return Spells.spotAround(ground, yaw, 1.4);
        }

        AABB body() {
            return new AABB(ground.x - 0.6, ground.y, ground.z - 0.6, ground.x + 0.6, ground.y + HEIGHT, ground.z + 0.6);
        }

        String orderKey() {
            return "mausoleum_" + tag;
        }
    }

    static final Map<UUID, List<Mausoleum>> BY_OWNER = new HashMap<>();

    // =========================
    // Raising and losing them
    // =========================

    /** Raises one at 'ground', its door toward the owner. The tenth lets the oldest crumble first. */
    static Mausoleum raise(LivingEntity player, ServerLevel sl, Vec3 ground) {
        List<Mausoleum> owned = BY_OWNER.computeIfAbsent(player.getUUID(), id -> new ArrayList<>());
        while (owned.size() >= MAX) crumble(owned.get(0), "The oldest of your mausoleums crumbles to make room.");
        float yaw = Spells.yawOf(player.position().subtract(ground));
        int life = PrinceOfAbolition.duration(player, LIFE);
        long now = sl.getGameTime();
        String tag = "poa_mausoleum_" + player.getId() + "_" + now + "_" + owned.size();
        Mausoleum mausoleum = new Mausoleum(player.getUUID(), sl, ground, yaw, now + PrinceOfAbolitionFx.MAUSOLEUM_RISEN, now + life, tag);
        mausoleum.lastHurt = now;
        owned.add(mausoleum);
        PrinceOfAbolitionFx.raiseMausoleum(sl, ground, yaw, life, tag);
        Later.run(sl, PrinceOfAbolitionFx.MAUSOLEUM_RISEN, () -> {
            if (!owned.contains(mausoleum) || !Spells.casterStillHere(player, sl)) return;
            if (PrinceOfAbolition.isPrince(player, PrinceOfAbolition.ANY_SEQUENCE)) {
                PrinceOfAbolitionOrders.add(player, mausoleum.orderKey(), mausoleum.until, null);
            }
        });
        return mausoleum;
    }

    /** It crumbles now: destroyed, used for Rebirth, run out, pushed out. 'message' (may be null) goes to the owner. */
    static void crumble(Mausoleum mausoleum, String message) {
        List<Mausoleum> owned = BY_OWNER.get(mausoleum.owner);
        if (owned == null || !owned.remove(mausoleum)) return;
        if (owned.isEmpty()) BY_OWNER.remove(mausoleum.owner);
        PrinceOfAbolitionFx.mausoleumCrumble(mausoleum.level, mausoleum.ground, mausoleum.yaw, mausoleum.tag);
        Player owner = mausoleum.level.getPlayerByUUID(mausoleum.owner);
        if (owner != null) {
            PrinceOfAbolitionOrders.remove(owner, mausoleum.orderKey());
            if (message != null) owner.sendSystemMessage(Component.literal(message));
        }
    }

    /**
     * It goes off (Mausoleum Blast): taken out of the records and its order goes, with no crumbling look (the blast
     * is its look). False if it was gone already (struck down, run out) so nothing should go off.
     */
    static boolean detonate(Mausoleum mausoleum) {
        List<Mausoleum> owned = BY_OWNER.get(mausoleum.owner);
        if (owned == null || !owned.remove(mausoleum)) return false;
        if (owned.isEmpty()) BY_OWNER.remove(mausoleum.owner);
        Player owner = mausoleum.level.getPlayerByUUID(mausoleum.owner);
        if (owner != null) PrinceOfAbolitionOrders.remove(owner, mausoleum.orderKey());
        return true;
    }

    /**
     * Every standing one's remaining time is doubled, by at most 'most' ticks each (Exploit). Their pictures are
     * drawn again for the new time and their orders moved. Returns how many were stretched.
     */
    static int stretchAll(LivingEntity player, int most) {
        int stretched = 0;
        long now = player.level().getGameTime();
        for (Mausoleum mausoleum : standing(player)) {
            long extra = Math.min(most, mausoleum.until - now);
            if (extra <= 0) continue;
            mausoleum.until += extra;
            ParticleShapes.clearModels(mausoleum.level, mausoleum.ground, mausoleum.tag);
            PrinceOfAbolitionFx.mausoleumAgain(mausoleum.level, mausoleum.ground, mausoleum.yaw, (int) (mausoleum.until - now), mausoleum.tag);
            PrinceOfAbolitionOrders.add(player, mausoleum.orderKey(), mausoleum.until, null);
            stretched++;
        }
        return stretched;
    }

    /** All of the player's mausoleums crumble (logout, a lost Pathway). */
    static void crumbleAll(LivingEntity player, ServerLevel sl) {
        List<Mausoleum> owned = BY_OWNER.get(player.getUUID());
        if (owned == null) return;
        for (Mausoleum mausoleum : new ArrayList<>(owned)) crumble(mausoleum, null);
        BY_OWNER.remove(player.getUUID());
        if (player.getPersistentData().getBooleanOr(WARD, false) || player.getPersistentData().getBooleanOr(LAST_WARD, false)) {
            DeathWard.disarm(player);
            player.getPersistentData().remove(WARD);
            player.getPersistentData().remove(LAST_WARD);
        }
    }

    // =========================
    // Asking about them
    // =========================

    /** The ones standing complete in the player's world, oldest first. */
    static List<Mausoleum> standing(LivingEntity player) {
        List<Mausoleum> found = new ArrayList<>();
        List<Mausoleum> owned = BY_OWNER.get(player.getUUID());
        if (owned == null) return found;
        for (Mausoleum mausoleum : owned) {
            if (mausoleum.level == player.level() && mausoleum.standing()) found.add(mausoleum);
        }
        return found;
    }

    /** How many stand complete in the player's world. */
    public static int count(LivingEntity player) {
        return standing(player).size();
    }

    /** How many the player has at all, rising ones too (what the next one costs more for). */
    static int owned(LivingEntity player) {
        List<Mausoleum> owned = BY_OWNER.get(player.getUUID());
        return owned == null ? 0 : owned.size();
    }

    /** The extra attack and defense they give (0.05 for each, twice that while crowned). Only for a Prince Of Abolition. */
    static float might(LivingEntity player) {
        if (!Beyonders.pathwayOf(player).equals(PrinceOfAbolition.PATHWAY)) return 0f;
        float each = PrinceOfAbolition.MIGHT_PER_MAUSOLEUM * (PrinceOfAbolitionSovereign.isCrowned(player) ? PrinceOfAbolitionSovereign.CORONATION_MIGHT : 1f);
        return each * count(player);
    }

    /** The standing one nearest to a point, or null. */
    static Mausoleum nearest(LivingEntity player, Vec3 to) {
        Mausoleum best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Mausoleum mausoleum : standing(player)) {
            double distance = mausoleum.ground.distanceToSqr(to);
            if (distance < bestDistance) {
                best = mausoleum;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** The standing one nearest to the crosshair, at most 'degrees' off it and 'range' away, or null. */
    static Mausoleum aimed(LivingEntity player, double range, double degrees) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double narrowest = Math.cos(Math.toRadians(degrees));
        Mausoleum best = null;
        double bestDot = narrowest;
        for (Mausoleum mausoleum : standing(player)) {
            Vec3 to = mausoleum.ground.add(0, HEIGHT / 2, 0).subtract(eye);
            double distance = to.length();
            if (distance > range || distance < 0.01) continue;
            double dot = to.scale(1.0 / distance).dot(look);
            if (dot > bestDot) {
                best = mausoleum;
                bestDot = dot;
            }
        }
        return best;
    }

    /** True while the player is out of the world, waiting to rise from a mausoleum. */
    static boolean isReborn(LivingEntity player) {
        return PrinceOfAbolition.isActive(player, REBORN_UNTIL);
    }

    // =========================
    // Every second, for the owner
    // =========================

    static void ownerTick(LivingEntity player, ServerLevel sl) {
        List<Mausoleum> owned = BY_OWNER.get(player.getUUID());
        long now = sl.getGameTime();
        boolean inSanctuary = false;
        if (owned != null) {
            for (Mausoleum mausoleum : new ArrayList<>(owned)) {
                if (now >= mausoleum.until) {
                    crumble(mausoleum, null);                            // its time is up
                    continue;
                }
                if (!mausoleum.standing() || mausoleum.level != sl) continue;
                mobsStrike(player, sl, mausoleum);
                if (!owned.contains(mausoleum)) continue;                // struck down just now
                if (now - mausoleum.lastHurt >= REGEN_AFTER) mausoleum.health = Math.min(HEALTH, mausoleum.health + REGEN);
                if (PrinceOfAbolition.isPrince(player, SANCTUARY_SEQUENCE) && sanctuary(player, sl, mausoleum)) inSanctuary = true;
            }
        }
        CompoundTag data = player.getPersistentData();
        if (inSanctuary) {
            if (!data.getBooleanOr(IN_SANCTUARY, false)) PrinceOfAbolitionFx.statusForever(player, Status.SANCTUARY);
            data.putBoolean(IN_SANCTUARY, true);
        } else if (data.getBooleanOr(IN_SANCTUARY, false)) {
            data.remove(IN_SANCTUARY);
            PrinceOfAbolitionFx.statusEnd(player, Status.SANCTUARY);
        }
        rebirthWard(player, sl);
    }

    /** Hostile mobs hunting the owner (or their side) that stand next to it strike it. */
    private static void mobsStrike(LivingEntity player, ServerLevel sl, Mausoleum mausoleum) {
        AABB reach = mausoleum.body().inflate(MOB_REACH);
        for (Mob mob : sl.getEntitiesOfClass(Mob.class, reach, LivingEntity::isAlive)) {
            LivingEntity hunted = mob.getTarget();
            if (hunted == null || !Spells.isEnemy(player, mob)) continue;
            if (hunted != player && Spells.isEnemy(player, hunted)) continue;            // it hunts someone else
            float blow = MOB_BLOW;
            if (mob.getAttribute(Attributes.ATTACK_DAMAGE) != null) blow = (float) mob.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
            hurt(mausoleum, Math.max(1f, blow));
            if (!BY_OWNER.getOrDefault(player.getUUID(), List.of()).contains(mausoleum)) return;
        }
    }

    /** Every tick, for every player: one who swings at someone else's mausoleum within reach strikes it. */
    static void swingTick(Player player, ServerLevel sl) {
        if (BY_OWNER.isEmpty() || !player.swinging || player.swingTime != 0 || player.isSpectator()) return;
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        for (List<Mausoleum> owned : new ArrayList<>(BY_OWNER.values())) {
            for (Mausoleum mausoleum : new ArrayList<>(owned)) {
                if (mausoleum.level != sl || !mausoleum.standing() || mausoleum.owner.equals(player.getUUID())) continue;
                if (mausoleum.ground.distanceToSqr(eye) > (PLAYER_REACH + HEIGHT) * (PLAYER_REACH + HEIGHT)) continue;
                LivingEntity owner = sl.getEntity(mausoleum.owner) instanceof LivingEntity found ? found : null;   // a player or a Beyonder mob
                if (owner != null && !Spells.isEnemy(owner, player)) continue;           // a friend does not
                if (!looksAt(eye, look, mausoleum.body())) continue;
                float blow = 1f;
                if (player.getAttribute(Attributes.ATTACK_DAMAGE) != null) blow = (float) player.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
                hurt(mausoleum, Math.max(1f, blow * Math.max(0.2f, player.getAttackStrengthScale(0.5f))));
                return;                                                  // one swing, one mausoleum
            }
        }
    }

    /** True if the line of sight passes through the box within PLAYER_REACH. */
    private static boolean looksAt(Vec3 eye, Vec3 look, AABB box) {
        for (double along = 0.25; along <= PLAYER_REACH; along += 0.25) {
            if (box.contains(eye.add(look.scale(along)))) return true;
        }
        return false;
    }

    private static void hurt(Mausoleum mausoleum, float amount) {
        mausoleum.health -= amount;
        mausoleum.lastHurt = mausoleum.level.getGameTime();
        PrinceOfAbolitionFx.mausoleumHit(mausoleum.level, mausoleum.ground);
        if (mausoleum.health <= 0f) crumble(mausoleum, "One of your mausoleums was destroyed.");
    }

    /** Sanctuary round one mausoleum, once a second. True if the owner stands in it. */
    private static boolean sanctuary(LivingEntity player, ServerLevel sl, Mausoleum mausoleum) {
        double radius = PrinceOfAbolition.area(player, SANCTUARY_RADIUS);
        Vec3 middle = mausoleum.ground.add(0, 1, 0);
        boolean ownerIn = player.position().distanceTo(mausoleum.ground) <= radius;
        boolean anyone = ownerIn;
        if (ownerIn) {
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal(SANCTUARY_HEAL);
                PrinceOfAbolitionFx.sanctuaryMend(sl, player);
            }
            Spells.giveSpirituality(player, Spells.spiritualityRegenOf(player) * 20f * SANCTUARY_SPIRIT);
        }
        for (LivingEntity ally : Spells.alliesAround(player, sl, middle, radius)) {
            anyone = true;
            if (ally.getHealth() < ally.getMaxHealth()) {
                net.lukario.circleofinevitability.combat.Support.heal(player, ally, SANCTUARY_HEAL);
                PrinceOfAbolitionFx.sanctuaryMend(sl, ally);
            }
        }
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, middle, radius)) {
            Spells.keepEffect(enemy, MobEffects.SLOWNESS, SANCTUARY_SLOW, 30);
        }
        long now = sl.getGameTime();
        if (now % 40 < 20) PrinceOfAbolitionFx.sanctuaryPulse(sl, mausoleum.ground, radius);
        if (anyone && now % 80 < 20) PrinceOfAbolitionFx.sanctuaryHum(sl, mausoleum.ground);
        return ownerIn;
    }

    // =========================
    // Rebirth
    // =========================

    /**
     * Once a second: the death ward is armed while a mausoleum stands (Rebirth), or, for a Black Emperor with none
     * left, while at least LAST_REBIRTH_ORDERS orders stand and Last Rebirth is ready; it is taken off otherwise.
     */
    private static void rebirthWard(LivingEntity player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        boolean ours = data.getBooleanOr(WARD, false);
        boolean last = data.getBooleanOr(LAST_WARD, false);
        if (isReborn(player) || Exile.isExiled(player)) return;                  // out of the world right now
        if (DeathWard.isArmed(player) && !ours && !last) return;                 // another Pathway's ward is on
        boolean rebirth = count(player) > 0;
        boolean lastRebirth = !rebirth && PrinceOfAbolition.isPrince(player, LAST_REBIRTH_SEQUENCE)
                && PrinceOfAbolitionOrders.count(player) >= LAST_REBIRTH_ORDERS
                && sl.getGameTime() >= data.getLongOr(LAST_READY, 0L);
        if (rebirth) {
            if (last || !DeathWard.isArmed(player) || DeathWard.ticksLeft(player) < 40) {
                data.remove(LAST_WARD);
                DeathWard.arm(player, WARD_TICKS, REBIRTH_HEALTH, (saved, source, level) -> {
                    if (saved instanceof Player reborn) fall(reborn, level);
                });
                data.putBoolean(WARD, true);
            }
        } else if (lastRebirth) {
            if (ours || !DeathWard.isArmed(player) || DeathWard.ticksLeft(player) < 40) {
                data.remove(WARD);
                DeathWard.arm(player, WARD_TICKS, LAST_REBIRTH_HEALTH, (saved, source, level) -> {
                    if (saved instanceof Player reborn) lastFall(reborn, level);
                });
                data.putBoolean(LAST_WARD, true);
            }
        } else if (ours || last) {
            data.remove(WARD);
            data.remove(LAST_WARD);
            DeathWard.disarm(player);
        }
    }

    /**
     * Last Rebirth: the killing blow did not take though no mausoleum stands; the orders hold the Black Emperor up.
     * Out of the world for LAST_REBIRTH_TICKS (status/Exile), then back where they fell.
     */
    private static void lastFall(LivingEntity player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        data.remove(LAST_WARD);
        data.putLong(LAST_READY, sl.getGameTime() + LAST_REBIRTH_COOLDOWN);
        PrinceOfAbolitionFx.rebirthFall(sl, player);
        PrinceOfAbolitionFx.status(player, Status.REBIRTH, LAST_REBIRTH_TICKS);
        Spells.tell(player, Component.literal("Your orders still stand, and they will not let you fall. You slip out of the world..."));
        Exile.banish(player, LAST_REBIRTH_TICKS, player.position(), (who, level) -> {
            if (!(who instanceof Player back)) return;
            PrinceOfAbolitionFx.statusEnd(back, Status.REBIRTH);
            PrinceOfAbolitionFx.lastRebirth(level, back);
            back.sendSystemMessage(Component.literal("You rise again where you fell."));
        });
    }

    /** The killing blow did not take: the owner slips out of the world, and rises later from a mausoleum. */
    private static void fall(LivingEntity player, ServerLevel sl) {
        CompoundTag data = player.getPersistentData();
        data.remove(WARD);
        long until = sl.getGameTime() + REBIRTH_TICKS;
        data.putLong(REBORN_UNTIL, until);
        Concealment.hide(player, REBIRTH_TICKS);
        Root.apply(player, REBIRTH_TICKS, RootRestriction.EVERYTHING);
        PrinceOfAbolitionFx.rebirthFall(sl, player);
        PrinceOfAbolitionFx.status(player, Status.REBIRTH, REBIRTH_TICKS);
        Spells.tell(player, Component.literal("Your mausoleums will not let you go. You slip out of the world..."));
        Later.run(sl, REBIRTH_TICKS, () -> {
            if (player.getPersistentData().getLongOr(REBORN_UNTIL, 0L) != until || !Spells.casterStillHere(player, sl)) return;
            rise(player, sl);
        });
    }

    /** Ten seconds later: up again from the nearest mausoleum (where they fell, if none is left), which crumbles. */
    private static void rise(LivingEntity player, ServerLevel sl) {
        player.getPersistentData().remove(REBORN_UNTIL);
        Root.remove(player);
        Concealment.reveal(player);
        PrinceOfAbolitionFx.statusEnd(player, Status.REBIRTH);
        Mausoleum from = nearest(player, player.position());
        Vec3 ground = player.position();
        if (from != null) {
            Vec3 door = from.door();
            Vec3 there = Spells.fits(player, sl, door) ? door
                    : Spells.freeSpotNear(player, sl, from.ground, door.subtract(from.ground), 1.6);
            if (there != null) {
                player.teleportTo(there.x, there.y, there.z);
                player.fallDistance = 0;
            }
            ground = from.ground;
        }
        PrinceOfAbolitionFx.rebirthRise(sl, player, ground);
        if (from != null) crumble(from, null);
        // a Majesty pulse: the enemies round the rising kneel
        int kneel = PrinceOfAbolition.duration(player, REBIRTH_KNEEL);
        for (LivingEntity enemy : Spells.enemiesAround(player, sl, ground, PrinceOfAbolition.area(player, REBIRTH_MAJESTY))) {
            PrinceOfAbolitionFallen.kneel(player, sl, enemy, kneel);
        }
        Spells.tell(player, Component.literal(from != null ? "You rise again from your mausoleum." : "You rise again where you fell."));
    }

    /** After a relog: a rising that never finished is over (its hold and its hiding were saved, its timer was not). */
    static boolean clearRebirth(LivingEntity player) {
        CompoundTag data = player.getPersistentData();
        boolean was = data.contains(REBORN_UNTIL);
        data.remove(REBORN_UNTIL);
        data.remove(WARD);
        data.remove(LAST_WARD);
        data.remove(IN_SANCTUARY);
        return was;
    }
}
