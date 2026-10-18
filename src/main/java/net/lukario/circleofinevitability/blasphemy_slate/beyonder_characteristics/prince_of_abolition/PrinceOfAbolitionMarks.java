package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionFx.Status;
import net.lukario.circleofinevitability.combat.Later;
import net.lukario.circleofinevitability.particles.fx.ParticleShapes;
import net.lukario.circleofinevitability.status.Falter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * The small statuses the Prince Of Abolition puts on others: Compromised, Brainwashed, the Bribes, Distorted,
 * Disordered, Corrupted (Corrosion's greed), Kneeling and Bestowment's five feelings. One kind, who put it there,
 * when it ends; saved with the entity. What each one DOES is read by the events in PrinceOfAbolition (less damage,
 * a mob that will not pick its briber); this class only keeps them, and hands the misses to status/Falter (a kind
 * with a 'miss' share makes that share of the bearer's attacks go wide while it lasts).
 *
 * A new kind is one line in Kind (its picture, mind or not, order or not, its miss share) plus whatever it does in
 * the events. The rot of Corrosion and Ruin is not a mark: it is status/Corrosion, which any Pathway can use.
 *
 *   int ticks = PrinceOfAbolitionMarks.put(player, target, Kind.COMPROMISED, 200);   // 0: it did not take
 *   PrinceOfAbolitionFx.compromised(sl, target, ticks, PrinceOfAbolitionMarks.tag(target, Kind.COMPROMISED));
 *   PrinceOfAbolitionMarks.has(target, Kind.COMPROMISED);                            // anyone's
 *   PrinceOfAbolitionMarks.from(target, Kind.COMPROMISED, player);                   // this player's
 *
 * put(...) does the shared rules: a mind mark does not take through Iron Will and lasts half as long on a Barbarian
 * of this Pathway (Mental Resistance); a Bribe or Disorder lasts half as long again on someone the same player has
 * Compromised; a player who carries a mark sees its picture and clock; a mark that is a standing order counts toward
 * the owner's Order. When it ends (time, end(...), or put again) the models shown with tag(...) are cleared.
 */
public final class PrinceOfAbolitionMarks {

    private PrinceOfAbolitionMarks() {}

    public enum Kind {
        //           shows on a player       mind   order  miss (status/Falter)
        COMPROMISED(Status.COMPROMISED, false, true, 0f),
        BRAINWASHED(Status.BRAINWASHED, true, false, 0f),
        WEAKENED(Status.WEAKENED, false, true, 0f),
        CHARMED(Status.CHARMED, true, true, 0f),
        ARROGANT(Status.ARROGANT, true, true, PrinceOfAbolitionBribes.ARROGANCE_MISS),
        CONNECTED(null, false, true, 0f),               // the briber shows Status.CONNECT instead
        DISTORTED(Status.DISTORTED, false, false, 0f),
        DISORDERED(Status.DISORDERED, false, false, PrinceOfAbolition.DISORDER_MISS),
        CORRUPTED(Status.CORROSION, true, false, PrinceOfAbolitionFallen.CORROSION_MISS),   // Corrosion's greed (its rot is status/Corrosion)
        KNEELING(Status.KNEELING, true, false, 0f),
        // Bestowment's feelings (PrinceOfAbolitionFallen)
        SLUGGISH(Status.SLUGGISH, true, true, 0f),
        ANXIOUS(Status.ANXIOUS, true, true, PrinceOfAbolitionFallen.ANXIOUS_MISS),
        DEFEATED(Status.DEFEATED, true, true, 0f),
        RASH(Status.RASH, true, true, 0f),
        AVARICIOUS(Status.AVARICIOUS, true, true, 0f);

        final Status status;
        /** A mind effect: Iron Will shakes it off, Mental Resistance halves it. */
        public final boolean mind;
        /** One of the owner's standing orders while it lasts. */
        public final boolean order;
        /** The share of the bearer's attacks that go wide while it lasts (0: none). */
        public final float miss;

        Kind(Status status, boolean mind, boolean order, float miss) {
            this.status = status;
            this.mind = mind;
            this.order = order;
            this.miss = miss;
        }

        String key() {
            return "poa_mark_" + name().toLowerCase();
        }
    }

    /** A Bribe or Disorder lasts this much longer on someone its owner has Compromised (Verdict). */
    static final float COMPROMISED_LONGER = 1.5f;
    /** Mental Resistance (Barbarian): mind marks last this share as long on a Prince Of Abolition. */
    static final float MENTAL_RESISTANCE = 0.5f;

    /**
     * Puts the mark on 'target' from 'owner' for 'ticks' (or longer, see above). Returns how long it really lasts:
     * 0 if it did not take (Iron Will, dead, the owner itself). Putting it again starts it over.
     */
    public static int put(Player owner, LivingEntity target, Kind kind, int ticks) {
        if (owner == null || target == null || target == owner || !target.isAlive() || ticks <= 0) return 0;
        if (!(target.level() instanceof ServerLevel sl)) return 0;
        if (kind.mind && target instanceof Player victim && PrinceOfAbolition.shrugsMind(victim)) {
            PrinceOfAbolitionFx.mindShrug(sl, victim);
            return 0;
        }
        if (kind.mind && target instanceof Player victim && PrinceOfAbolition.isPrince(victim, PrinceOfAbolition.MENTAL_RESISTANCE_SEQUENCE)) {
            ticks = Math.max(1, Math.round(ticks * MENTAL_RESISTANCE));
        }
        boolean bribeOrDisorder = kind == Kind.WEAKENED || kind == Kind.CHARMED || kind == Kind.ARROGANT
                || kind == Kind.CONNECTED || kind == Kind.DISORDERED;
        if (bribeOrDisorder && from(target, Kind.COMPROMISED, owner)) ticks = Math.round(ticks * COMPROMISED_LONGER);

        ParticleShapes.clearModels(sl, target, tag(target, kind));         // put again: one picture, not two
        CompoundTag data = target.getPersistentData();
        long until = sl.getGameTime() + ticks;
        data.putLong(kind.key() + "_until", until);
        data.putString(kind.key() + "_by", owner.getUUID().toString());
        if (kind.status != null && target instanceof Player victim) PrinceOfAbolitionFx.status(victim, kind.status, ticks);
        if (kind.miss > 0f) Falter.apply(target, kind.key(), kind.miss, ticks);
        if (kind.order && PrinceOfAbolition.isPrince(owner, PrinceOfAbolition.ANY_SEQUENCE)) {
            PrinceOfAbolitionOrders.add(owner, orderKey(target, kind), until, target);
        }
        Later.run(sl, ticks, () -> {
            if (target.getPersistentData().getLongOr(kind.key() + "_until", 0L) == until) end(target, kind);
        });
        return ticks;
    }

    /** True while anyone's mark of this kind is on it. */
    public static boolean has(Entity entity, Kind kind) {
        return entity != null && entity.getPersistentData().getLongOr(kind.key() + "_until", 0L) > entity.level().getGameTime();
    }

    /** True while this player's mark of this kind is on it. */
    public static boolean from(Entity entity, Kind kind, Entity owner) {
        return owner != null && has(entity, kind) && owner.getUUID().toString().equals(byId(entity, kind));
    }

    /** Who put the mark there (a UUID as text), or "" when there is none. */
    public static String byId(Entity entity, Kind kind) {
        return has(entity, kind) ? entity.getPersistentData().getStringOr(kind.key() + "_by", "") : "";
    }

    /** The player who put it there, if they are in this level; else null. */
    public static Player owner(Entity entity, Kind kind) {
        if (!(entity.level() instanceof ServerLevel sl)) return null;
        String id = byId(entity, kind);
        if (id.isEmpty()) return null;
        try {
            return sl.getPlayerByUUID(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Ticks it still lasts (0 = none). */
    public static int ticksLeft(Entity entity, Kind kind) {
        if (!has(entity, kind)) return 0;
        return (int) (entity.getPersistentData().getLongOr(kind.key() + "_until", 0L) - entity.level().getGameTime());
    }

    /** Takes it off now: its models, its picture on a player, its standing order. */
    public static void end(LivingEntity target, Kind kind) {
        CompoundTag data = target.getPersistentData();
        String by = data.getStringOr(kind.key() + "_by", "");
        data.remove(kind.key() + "_until");
        data.remove(kind.key() + "_by");
        if (kind.miss > 0f) Falter.remove(target, kind.key());
        if (!(target.level() instanceof ServerLevel sl)) return;
        ParticleShapes.clearModels(sl, target, tag(target, kind));
        if (kind.status != null && target instanceof Player victim) PrinceOfAbolitionFx.statusEnd(victim, kind.status);
        if (kind.order && !by.isEmpty()) {
            try {
                PrinceOfAbolitionOrders.remove(sl, UUID.fromString(by), orderKey(target, kind));
            } catch (IllegalArgumentException ignored) {
                // an edited save: there is no order to take back
            }
        }
    }

    /** Every mind mark on it ends (Iron Will). Returns true if there was one. */
    static boolean endMind(LivingEntity target) {
        boolean any = false;
        for (Kind kind : Kind.values()) {
            if (kind.mind && has(target, kind)) {
                end(target, kind);
                any = true;
            }
        }
        return any;
    }

    /** The tag the pictures of this mark on this entity are shown with (so they can be cleared). */
    public static String tag(Entity target, Kind kind) {
        return "poa_" + kind.name().toLowerCase() + "_" + target.getId();
    }

    private static String orderKey(Entity target, Kind kind) {
        return "mark_" + kind.name().toLowerCase() + "_" + target.getUUID();
    }
}
