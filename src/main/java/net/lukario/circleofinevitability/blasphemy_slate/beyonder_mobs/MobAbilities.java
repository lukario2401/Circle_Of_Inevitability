package net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder.SequenceCommands;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.AbominationMobs;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGodMobs;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrderMobs;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.key_of_stars.KeyOfStarsMobs;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragonMobs;
import net.lukario.circleofinevitability.combat.Casts;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Every ability a Beyonder mob can cast (MobAbility), by Pathway. A mob casts one only if it is of that Pathway and
 * at that Sequence or stronger (and has the spirituality: MobCasting).
 *
 * Each Pathway keeps its mob versions in one class next to its player abilities (hand_of_order/HandOfOrderMobs,
 * key_of_stars/KeyOfStarsMobs, star_dragon/StarDragonMobs), with a register() that this class calls once.
 *
 * To give mobs another ability of a Pathway that already has a ...Mobs class: one more MobAbilities.register(...)
 * in its register(). To give mobs a Pathway that has none yet: write its ...Mobs class the same way, add one line
 * to the static block below, and add the Pathway to BeyonderMobRules.MOB_PATHWAYS so mobs are rolled as it.
 */
public final class MobAbilities {

    private static final List<MobAbility> ALL = new ArrayList<>();      // (before the static block: it fills this)
    private static final Map<String, Float> CHEAPEST = new HashMap<>(); // cheapestCost, by "Pathway/sequence"

    static {
        HandOfOrderMobs.register();
        KeyOfStarsMobs.register();
        StarDragonMobs.register();
        AbominationMobs.register();
        HandOfGodMobs.register();
    }

    private MobAbilities() {}

    /**
     * Adds a mob ability (one with the same Pathway, key and form is replaced). Refused, with a line in the log, if
     * it has no cast() or its Pathway does not exist.
     */
    public static void register(MobAbility ability) {
        if (ability == null || ability.cast() == null) {
            CircleOfInevitability.LOGGER.error("A mob ability without a cast() was not registered: {}", ability);
            return;
        }
        if (!SequenceCommands.PATHWAYS.contains(ability.pathway())) {
            CircleOfInevitability.LOGGER.error("A mob ability of a Pathway that does not exist was not registered: {}", ability);
            return;
        }
        ALL.removeIf(one -> one.id().equals(ability.id()));
        ALL.add(ability);
        CHEAPEST.clear();
    }

    /** The cost of the cheapest mob ability that Pathway knows at that Sequence (0 if none). Remembered. */
    public static float cheapestCost(String pathway, int sequence) {
        if (pathway == null || sequence < 0) return 0f;
        return CHEAPEST.computeIfAbsent(pathway + "/" + sequence, key -> {
            float cheapest = Float.MAX_VALUE;
            for (MobAbility ability : ALL) {
                if (ability.pathway().equals(pathway) && sequence <= ability.sequence() && ability.cost() > 0f) {
                    cheapest = Math.min(cheapest, ability.cost());
                }
            }
            return cheapest == Float.MAX_VALUE ? 0f : cheapest;
        });
    }

    /** Every mob ability, in the order they were registered. */
    public static List<MobAbility> all() {
        return List.copyOf(ALL);
    }

    /** The mob abilities of one Pathway (empty for one that has none). */
    public static List<MobAbility> of(String pathway) {
        List<MobAbility> found = new ArrayList<>();
        for (MobAbility ability : ALL) {
            if (ability.pathway().equals(pathway)) found.add(ability);
        }
        return found;
    }

    /** The one with that id ("Hand Of Order/1/sneak", MobAbility.id), or null. */
    public static MobAbility byId(String id) {
        for (MobAbility ability : ALL) {
            if (ability.id().equals(id)) return ability;
        }
        return null;
    }

    /**
     * The mob version of what was cast (combat/Casts), for the abilities that copy a power seen (Key Of Stars'
     * Replication, Record): a wheel pick by its entry, a key by its key and form; a key whose mob versions are wheel
     * entries (a mob's own cast of one) gives one of them. Null when that power has no mob version.
     */
    public static MobAbility forCast(Casts.Cast cast) {
        if (cast == null || cast.pathway() == null) return null;
        if (cast.entry() != null) {
            for (MobAbility ability : ALL) {
                if (ability.pathway().equals(cast.pathway()) && ability.entry().equals(cast.entry().key())) return ability;
            }
            return null;
        }
        MobAbility exact = byId(cast.pathway() + "/" + cast.key() + (cast.sneak() ? "/sneak" : ""));
        if (exact != null) return exact;
        for (MobAbility ability : ALL) {
            if (ability.pathway().equals(cast.pathway()) && ability.key() == cast.key() && !ability.entry().isEmpty()) return ability;
        }
        return null;
    }

    /** What this mob's Pathway and Sequence let it cast (spirituality, cooldowns and range aside). Empty for no Beyonder. */
    public static List<MobAbility> knownBy(LivingEntity mob) {
        List<MobAbility> found = new ArrayList<>();
        String pathway = BeyonderMobs.pathway(mob);
        int sequence = BeyonderMobs.sequence(mob);
        if (pathway == null || sequence < 0) return found;
        for (MobAbility ability : ALL) {
            if (ability.pathway().equals(pathway) && sequence <= ability.sequence()) found.add(ability);
        }
        return found;
    }
}
