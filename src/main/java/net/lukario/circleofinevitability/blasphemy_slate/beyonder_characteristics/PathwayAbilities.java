package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Every ability of every Pathway as a list, for code that has to pick one or cast one it does not know by name
 * (Worm Of Time steals them). The ability keys themselves do not use this list: they still call the methods
 * directly from network/SKeyPressAbility...Used.
 *
 *   for (PathwayAbilities.Ability ability : PathwayAbilities.of("Key Of Stars")) { ... }
 *   ability.cast().run(player, level, sl);        // casts it for this player, whatever their Pathway is
 *
 * cast() skips the "is this your Pathway" check and nothing else: the ability still needs its Sequence and
 * its spirituality. To cast it without those, wrap it: Beyonder.castUnbound(player, () -> ability.cast().run(...)).
 * Whether the normal or the sneak form comes out depends on whether the player is sneaking, as always.
 *
 * A new ability gets a line here (and one in docs/SEQUENCES.md). The numbers must be the ones in its method.
 */
public final class PathwayAbilities {

    @FunctionalInterface
    public interface Cast {
        void run(Player player, Level level, ServerLevel sl);
    }

    /**
     * pathway:   the exact Pathway name.
     * key:       1..7 = the ability keys G H J K L N M.
     * name:      what players are told it is called.
     * sequence:  the weakest Sequence that can use it.
     * sneakForm: true if it does something else when cast while sneaking.
     * stealable: false if it only works for its own Pathway (it needs that Pathway's stars, opens its menu, needs its key to end) or does nothing yet.
     */
    public record Ability(String pathway, int key, String name, int sequence, boolean sneakForm, boolean stealable, Cast cast) {}

    private static final boolean SNEAK = true, NO_SNEAK = false;
    private static final boolean STEALABLE = true, OWN_PATHWAY_ONLY = false;

    public static final List<Ability> ALL = List.of(
            // ---- Key Of Stars ----
            new Ability("Key Of Stars", 1, "Star Burst", 9, SNEAK, OWN_PATHWAY_ONLY,      // spends the Pathway's stars
                    (player, level, sl) -> KeyOfStars.keyOfStarsStarBurst(player, level, sl, true)),
            new Ability("Key Of Stars", 2, "Dash", 8, SNEAK, STEALABLE,
                    (player, level, sl) -> KeyOfStars.keyOfStarsDash(player, level, sl, true)),
            new Ability("Key Of Stars", 3, "Buff", 7, SNEAK, STEALABLE,
                    (player, level, sl) -> KeyOfStars.keyOfStarsBuff(player, level, sl, true)),
            new Ability("Key Of Stars", 4, "Seal", 7, SNEAK, STEALABLE,
                    (player, level, sl) -> KeyOfStars.keyOfStarsSeal(player, level, sl, true)),
            new Ability("Key Of Stars", 5, "Transport", 7, SNEAK, STEALABLE,
                    (player, level, sl) -> KeyOfStars.keyOfStarsTransport(player, level, sl, true)),
            new Ability("Key Of Stars", 6, "Orbital", 5, SNEAK, OWN_PATHWAY_ONLY,         // throws the Pathway's stars
                    (player, level, sl) -> KeyOfStars.keyOfStarsOrbital(player, level, sl, true)),
            new Ability("Key Of Stars", 7, "Cosmic Plague", 3, SNEAK, STEALABLE,
                    (player, level, sl) -> KeyOfStars.keyOfStarsCosmicPlague(player, level, sl, true)),

            // ---- Attendant Of Mysteries ----
            new Ability("Attendant Of Mysteries", 1, "Paper Dagger", 9, SNEAK, STEALABLE,
                    (player, level, sl) -> AttendantOfMysteries.attendantOfMysteriesPaperDagger(player, level, sl, true)),
            new Ability("Attendant Of Mysteries", 2, "Flaming Jump", 8, SNEAK, STEALABLE,
                    (player, level, sl) -> AttendantOfMysteries.attendantOfMysteriesFlamingJump(player, level, sl, true)),
            new Ability("Attendant Of Mysteries", 3, "Damage Transfer", 7, SNEAK, STEALABLE,
                    (player, level, sl) -> AttendantOfMysteries.attendantOfMysteriesDamageTransfer(player, level, sl, true)),
            new Ability("Attendant Of Mysteries", 4, "Air Cannon", 6, SNEAK, STEALABLE,
                    (player, level, sl) -> AttendantOfMysteries.attendantOfMysteriesAirCannon(player, level, sl, true)),
            new Ability("Attendant Of Mysteries", 5, "Spirit Body Threads", 5, SNEAK, STEALABLE,
                    (player, level, sl) -> AttendantOfMysteries.attendantOfMysteriesCountdown(player, level, sl, true)),
            new Ability("Attendant Of Mysteries", 6, "Historical Void", 4, SNEAK, STEALABLE,
                    (player, level, sl) -> AttendantOfMysteries.attendantOfMysteriesHistoricalVoid(player, level, sl, true)),
            new Ability("Attendant Of Mysteries", 7, "Grafting", 3, SNEAK, STEALABLE,
                    (player, level, sl) -> AttendantOfMysteries.attendantOfMysteriesGrafting(player, level, sl, true)),

            // ---- Hand Of Order ----
            new Ability("Hand Of Order", 1, "Buff", 9, SNEAK, STEALABLE,
                    (player, level, sl) -> HandOfOrder.handOfOrderBuff(player, level, sl, true)),
            new Ability("Hand Of Order", 2, "Decree", 8, NO_SNEAK, OWN_PATHWAY_ONLY,      // opens the Pathway's menu
                    (player, level, sl) -> HandOfOrder.handOfOrderDecree(player, level, sl, true)),
            new Ability("Hand Of Order", 3, "Edict", 7, SNEAK, STEALABLE,
                    (player, level, sl) -> HandOfOrder.handOfOrderEdict(player, level, sl, true)),
            new Ability("Hand Of Order", 4, "Ability 4", 6, SNEAK, OWN_PATHWAY_ONLY,      // only particles so far
                    (player, level, sl) -> HandOfOrder.handOfOrderAbility4(player, level, sl, true)),
            new Ability("Hand Of Order", 5, "Ability 5", 5, NO_SNEAK, OWN_PATHWAY_ONLY,   // opens the Pathway's menu
                    (player, level, sl) -> HandOfOrder.handOfOrderAbility5(player, level, sl, true)),
            new Ability("Hand Of Order", 6, "Words", 4, NO_SNEAK, OWN_PATHWAY_ONLY,       // opens the Pathway's text prompt
                    (player, level, sl) -> HandOfOrder.handOfOrderWords(player, level, sl, true)),
            new Ability("Hand Of Order", 7, "Bind", 3, NO_SNEAK, STEALABLE,
                    (player, level, sl) -> HandOfOrder.handOfOrderBind(player, level, sl, true)),

            // ---- Prince Of Abolition ----
            new Ability("Prince Of Abolition", 1, "Spear", 9, SNEAK, STEALABLE,
                    (player, level, sl) -> PrinceOfAbolition.princeOfAbolitionSpear(player, level, sl, true)),
            new Ability("Prince Of Abolition", 2, "Flame", 8, SNEAK, STEALABLE,
                    (player, level, sl) -> PrinceOfAbolition.princeOfAbolitionFlame(player, level, sl, true)),
            new Ability("Prince Of Abolition", 3, "Frost", 7, SNEAK, STEALABLE,
                    (player, level, sl) -> PrinceOfAbolition.princeOfAbolitionFrost(player, level, sl, true)),
            new Ability("Prince Of Abolition", 4, "Hammer", 6, SNEAK, STEALABLE,
                    (player, level, sl) -> PrinceOfAbolition.princeOfAbolitionHammer(player, level, sl, true)),
            new Ability("Prince Of Abolition", 5, "Meteor", 5, SNEAK, STEALABLE,
                    (player, level, sl) -> PrinceOfAbolition.princeOfAbolitionMeteor(player, level, sl, true)),
            new Ability("Prince Of Abolition", 6, "Entropy", 4, SNEAK, STEALABLE,
                    (player, level, sl) -> PrinceOfAbolition.princeOfAbolitionEntropy(player, level, sl, true)),
            new Ability("Prince Of Abolition", 7, "Abolition", 3, SNEAK, STEALABLE,
                    (player, level, sl) -> PrinceOfAbolition.princeOfAbolitionAbolition(player, level, sl, true)),

            // ---- Pale Emperor ----
            new Ability("Pale Emperor", 1, "Root", 9, SNEAK, STEALABLE,
                    (player, level, sl) -> PaleEmperor.paleEmperorRoot(player, sl, true)),
            new Ability("Pale Emperor", 2, "Door", 8, SNEAK, STEALABLE,
                    (player, level, sl) -> PaleEmperor.paleEmperorDoor(player, sl, true)),
            new Ability("Pale Emperor", 3, "Serpent", 7, SNEAK, STEALABLE,
                    (player, level, sl) -> PaleEmperor.paleEmperorSerpent(player, level, sl, true)),
            new Ability("Pale Emperor", 4, "Spear Skull", 7, SNEAK, STEALABLE,
                    (player, level, sl) -> PaleEmperor.paleEmperorSpearSkull(player, level, sl, true)),
            new Ability("Pale Emperor", 5, "Spikes", 6, SNEAK, STEALABLE,
                    (player, level, sl) -> PaleEmperor.paleEmperorSpikes(player, level, sl, true)),
            new Ability("Pale Emperor", 6, "Wings", 5, SNEAK, STEALABLE,
                    (player, level, sl) -> PaleEmperor.paleEmperorWings(player, level, sl, true)),
            new Ability("Pale Emperor", 7, "Wraiths", 4, SNEAK, STEALABLE,
                    (player, level, sl) -> PaleEmperor.paleEmperorWraiths(player, level, sl, true)),

            // ---- Worm Of Time ----
            new Ability("Worm Of Time", 1, "Life Theft", 9, SNEAK, STEALABLE,
                    (player, level, sl) -> WormOfTime.wormOfTimeAbility1(player, level, sl, true)),
            new Ability("Worm Of Time", 2, "Theft", 8, SNEAK, OWN_PATHWAY_ONLY,           // the theft itself is not stolen
                    (player, level, sl) -> WormOfTime.wormOfTimeAbility2(player, level, sl, true)),
            new Ability("Worm Of Time", 3, "Parasite", 4, NO_SNEAK, OWN_PATHWAY_ONLY,     // the same key is the way back out, and only this Pathway has it
                    (player, level, sl) -> WormOfTime.wormOfTimeAbility3(player, level, sl, true)),

            // ---- Conqueror ----
            new Ability("Conqueror", 1, "Hunter's Trap", 9, SNEAK, STEALABLE,
                    (player, level, sl) -> Conqueror.conquerorHuntersTrap(player, level, sl, true)),
            new Ability("Conqueror", 2, "Provocation", 8, SNEAK, STEALABLE,
                    (player, level, sl) -> Conqueror.conquerorProvocation(player, level, sl, true)),
            new Ability("Conqueror", 3, "Fire Ravens", 7, SNEAK, STEALABLE,
                    (player, level, sl) -> Conqueror.conquerorFireRavens(player, level, sl, true)),
            new Ability("Conqueror", 4, "Flame Transformation", 6, SNEAK, STEALABLE,
                    (player, level, sl) -> Conqueror.conquerorFlameTransformation(player, level, sl, true)),
            new Ability("Conqueror", 5, "Cull", 5, SNEAK, STEALABLE,
                    (player, level, sl) -> Conqueror.conquerorCull(player, level, sl, true)),
            new Ability("Conqueror", 6, "Galvanization", 4, SNEAK, STEALABLE,
                    (player, level, sl) -> Conqueror.conquerorGalvanization(player, level, sl, true)),
            new Ability("Conqueror", 7, "Fog of War", 3, SNEAK, STEALABLE,
                    (player, level, sl) -> Conqueror.conquerorFogOfWar(player, level, sl, true)),

            // ---- Apocalypse (keys 4 and 7, the silk and catastrophe wheels, are not made yet) ----
            new Ability("Apocalypse", 1, "Fatal Step", 9, SNEAK, STEALABLE,
                    (player, level, sl) -> Apocalypse.apocalypseFatalStep(player, level, sl, true)),
            new Ability("Apocalypse", 2, "Black Flame", 8, SNEAK, STEALABLE,
                    (player, level, sl) -> Apocalypse.apocalypseBlackFlame(player, level, sl, true)),
            new Ability("Apocalypse", 3, "Mirror World", 7, SNEAK, OWN_PATHWAY_ONLY,      // the same key is the way back out
                    (player, level, sl) -> Apocalypse.apocalypseMirrorWorld(player, level, sl, true)),
            new Ability("Apocalypse", 5, "Plague", 5, SNEAK, STEALABLE,
                    (player, level, sl) -> Apocalypse.apocalypsePlague(player, level, sl, true)),
            new Ability("Apocalypse", 6, "Unaging", 4, SNEAK, STEALABLE,
                    (player, level, sl) -> Apocalypse.apocalypseUnaging(player, level, sl, true)));

    private PathwayAbilities() {}

    /** The abilities of one Pathway, in key order. Empty for "none" or a name that is not a Pathway. */
    public static List<Ability> of(String pathway) {
        List<Ability> found = new ArrayList<>();
        for (Ability ability : ALL) {
            if (ability.pathway().equals(pathway)) found.add(ability);
        }
        return found;
    }

    /** The ability of that Pathway on that key (1..7), or null. */
    public static Ability of(String pathway, int key) {
        for (Ability ability : ALL) {
            if (ability.key() == key && ability.pathway().equals(pathway)) return ability;
        }
        return null;
    }
}
