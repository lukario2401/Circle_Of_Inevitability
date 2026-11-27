package net.lukario.circleofinevitability.item.artifacts.uniqueness;

import net.lukario.circleofinevitability.combat.AbilityCatalog;
import net.lukario.circleofinevitability.combat.Casts;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * One ability a Uniqueness lends. Either a Pathway ability from the catalog (fromCatalog: cast as a free, unbound
 * copy, exactly as if its Pathway's Beyonder cast it) or one written for the uniqueness (of: any method that returns
 * whether it went off).
 *
 * ownCooldown: ticks this ability waits on top of the uniqueness's own cooldown (0 = none). A heal that could be
 * cast every 0.2 s would make its bearer unkillable, so it waits longer.
 */
public record UniquePower(String id, String name, String icon, int sequence, Caster caster, int ownCooldown,
                          String description) {

    @FunctionalInterface
    public interface Caster {
        /** Casts it now. Returns false if it found nothing to act on (no cooldown, no price). */
        boolean cast(ServerPlayer player, ServerLevel level);
    }

    public static UniquePower fromCatalog(AbilityCatalog.Power power) {
        return new UniquePower(power.id(), power.name(), power.icon(), power.sequence(),
                (player, level) -> Casts.recast(player, power.cast()), 0, "");
    }

    /** An ability written for a uniqueness: 'id' must be unique ("red_priest/storm_of_war"). */
    public static UniquePower of(String id, String name, String icon, int sequence, int ownCooldown, String description,
                                 Caster caster) {
        return new UniquePower(id, name, icon, sequence, caster, ownCooldown, description);
    }

    public boolean cast(ServerPlayer player, ServerLevel level) {
        return caster.cast(player, level);
    }
}
