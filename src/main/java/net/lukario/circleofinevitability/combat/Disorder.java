package net.lukario.circleofinevitability.combat;

import net.lukario.circleofinevitability.root.Root;
import net.lukario.circleofinevitability.status.Concealment;
import net.lukario.circleofinevitability.status.DamageLink;
import net.lukario.circleofinevitability.status.DeathWard;
import net.lukario.circleofinevitability.status.Marionette;
import net.lukario.circleofinevitability.status.Substitute;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Disorder: how much an entity is bending the order of things right now, as a score from 0 up. What the Hand Of
 * Order's Sword of Judgment strikes hardest, and what Disorder Sense and Recognition show. Ask it from anywhere:
 *
 *   int disorder = Disorder.of(entity);
 *
 * +1 for each of these on it: Concealment, a Substitute armed, a DeathWard armed, a DamageLink (redirected or shared),
 * a root (Root) of any kind, being a marionette or summon, and for a player each marionette they hold; +1 for each
 * ability a player cast in the last 10 seconds (combat/Residue), at most CASTS_MAX. More can be counted once, in a
 * static block (a Violation of a Decree, a curse of your own):
 *
 *   Disorder.addSource(entity -> MyStatus.isOn(entity) ? 1 : 0);
 *
 * Nothing is stored; it is worked out each time it is asked.
 *
 * The guide with examples for all of these tools: docs/SPELL_KIT.md
 */
public final class Disorder {

    /** Casts counted from this far back... */
    public static int CASTS_WINDOW = 200;
    /** ...and at most this many of them. */
    public static int CASTS_MAX = 3;

    private static final List<ToIntFunction<LivingEntity>> SOURCES = new ArrayList<>();

    private Disorder() {}

    /** One more thing that counts toward Disorder (it returns how much, 0 for none). */
    public static void addSource(ToIntFunction<LivingEntity> source) {
        SOURCES.add(source);
    }

    /** The Disorder of this entity right now (0 = in perfect order). */
    public static int of(LivingEntity entity) {
        if (entity == null || !(entity.level() instanceof ServerLevel sl)) return 0;
        int score = 0;
        if (Concealment.isHidden(entity)) score++;
        if (Substitute.isArmed(entity)) score++;
        if (DeathWard.isArmed(entity)) score++;
        if (DamageLink.redirectTarget(entity) != null || DamageLink.isShared(entity)) score++;
        if (Root.isRooted(entity)) score++;
        if (Marionette.isMarionette(entity) || Marionette.isSummoned(entity)) score++;
        if (entity instanceof Player player) {
            score += Marionette.of(player).size();
            score += recentCasts(sl, player);
        }
        for (ToIntFunction<LivingEntity> source : SOURCES) score += Math.max(0, source.applyAsInt(entity));
        return score;
    }

    /** How many abilities this player cast in the last CASTS_WINDOW ticks, at most CASTS_MAX. */
    private static int recentCasts(ServerLevel sl, Player player) {
        int casts = 0;
        for (Residue.Cast cast : Residue.near(sl, player.position(), 128, CASTS_WINDOW)) {
            if (cast.caster().equals(player.getUUID()) && ++casts >= CASTS_MAX) break;
        }
        return casts;
    }
}
