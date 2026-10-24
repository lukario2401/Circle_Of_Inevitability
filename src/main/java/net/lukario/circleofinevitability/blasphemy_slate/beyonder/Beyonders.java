package net.lukario.circleofinevitability.blasphemy_slate.beyonder;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.PathwayAbilities;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_mobs.BeyonderMobs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Who is a Beyonder, of which Pathway and Sequence, and which abilities that gives them: for players and Beyonder mobs
 * alike (blasphemy_slate/beyonder_mobs). Any ability that reads, steals, copies or judges someone else's powers should
 * ask here instead of SoulCore, so it works on both.
 *
 *   if (Beyonders.isBeyonder(target)) { ... }
 *   String pathway = Beyonders.pathwayOf(target);              // "none" when not a Beyonder
 *   int sequence = Beyonders.sequenceOf(target);               // NO_SEQUENCE (10) when not a Beyonder
 *   List<PathwayAbilities.Ability> theirs = Beyonders.abilitiesOf(target);       // what their Sequence gives them
 *   PathwayAbilities.Ability one = Beyonders.randomTakeable(target, random, null); // a stealable one, or null
 *   Beyonders.describe(target)                                 // "Hand Of Order, Sequence 7" (or "")
 *
 * For a player who casts, keep using Beyonder.hasSequence: it knows about borrowed power (Beyonder.castUnbound).
 * Here the Sequence is always the real one.
 */
public final class Beyonders {

    /** The Pathway of one who has none. */
    public static final String NO_PATHWAY = "none";
    /** The "Sequence" of one who has no Pathway: one weaker than 9. */
    public static final int NO_SEQUENCE = Beyonder.WEAKEST_SEQUENCE + 1;

    private Beyonders() {}

    /** True for a player who follows a Pathway, or a Beyonder mob. */
    public static boolean isBeyonder(LivingEntity entity) {
        return !pathwayOf(entity).equals(NO_PATHWAY);
    }

    /** Their Pathway's exact name, or NO_PATHWAY. Never null. */
    public static String pathwayOf(LivingEntity entity) {
        if (entity == null) return NO_PATHWAY;
        if (entity instanceof Player player) {
            String pathway = SoulCore.getPathway(player);
            return pathway == null || pathway.isEmpty() ? NO_PATHWAY : pathway;
        }
        String pathway = BeyonderMobs.pathway(entity);
        return pathway == null ? NO_PATHWAY : pathway;
    }

    /** Their Sequence (9 weakest .. 0 strongest), or NO_SEQUENCE when they are no Beyonder. */
    public static int sequenceOf(LivingEntity entity) {
        if (!isBeyonder(entity)) return NO_SEQUENCE;
        if (entity instanceof Player player) return SoulCore.getSequence(player);
        return BeyonderMobs.sequence(entity);
    }

    /** True if this ability is theirs: their Pathway, and their Sequence reaches it. */
    public static boolean has(LivingEntity entity, PathwayAbilities.Ability ability) {
        return ability != null && isBeyonder(entity) && ability.pathway().equals(pathwayOf(entity))
                && sequenceOf(entity) <= ability.sequence();
    }

    /** Every ability their Pathway and Sequence give them, in key order. Empty for one who is no Beyonder. */
    public static List<PathwayAbilities.Ability> abilitiesOf(LivingEntity entity) {
        List<PathwayAbilities.Ability> found = new ArrayList<>();
        if (!isBeyonder(entity)) return found;
        int sequence = sequenceOf(entity);
        for (PathwayAbilities.Ability ability : PathwayAbilities.of(pathwayOf(entity))) {
            if (sequence <= ability.sequence()) found.add(ability);
        }
        return found;
    }

    /** The ones of abilitiesOf that can be stolen or copied (PathwayAbilities' STEALABLE). */
    public static List<PathwayAbilities.Ability> takeableFrom(LivingEntity entity) {
        List<PathwayAbilities.Ability> found = abilitiesOf(entity);
        found.removeIf(ability -> !ability.stealable());
        return found;
    }

    /** A random one of takeableFrom that 'also' accepts (null = any), or null if there is none. */
    public static PathwayAbilities.Ability randomTakeable(LivingEntity entity, RandomSource random,
                                                          Predicate<PathwayAbilities.Ability> also) {
        List<PathwayAbilities.Ability> choices = takeableFrom(entity);
        if (also != null) choices.removeIf(also.negate());
        return choices.isEmpty() ? null : choices.get(random.nextInt(choices.size()));
    }

    /** "Hand Of Order, Sequence 7", or "" for one who is no Beyonder. For the abilities that read someone. */
    public static String describe(LivingEntity entity) {
        if (!isBeyonder(entity)) return "";
        return pathwayOf(entity) + ", Sequence " + sequenceOf(entity);
    }
}
