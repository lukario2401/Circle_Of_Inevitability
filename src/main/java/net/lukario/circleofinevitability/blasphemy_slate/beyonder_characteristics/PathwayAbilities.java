package net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.Abomination;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.AbominationChains;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.AbominationCurses;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.AbominationWraith;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.AbominationZombie;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGod;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrderDecrees;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrderInterrogation;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrderOrder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrderVerdicts;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGodDawn;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGodDemonHunting;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGodTwilight;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.Illuminator;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorAlchemy;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorAstronomy;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorCivilization;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.IlluminatorWorkshop;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.*;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperor;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperorKnowledge;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperorRetellings;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperorStars;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolition;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionBribes;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionFallen;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolitionSovereign;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragon;
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

            // ---- Hand Of Order (docs/HAND_OF_ORDER.md) ----
            new Ability("Hand Of Order", 1, "Halt", 9, SNEAK, STEALABLE,
                    (player, level, sl) -> HandOfOrder.handOfOrderAuthority(player, level, sl, true)),
            new Ability("Hand Of Order", 2, "Jurisdiction", 8, SNEAK, STEALABLE,
                    (player, level, sl) -> HandOfOrder.handOfOrderJurisdiction(player, level, sl, true)),
            new Ability("Hand Of Order", 3, "Interrogation", 7, SNEAK, STEALABLE,               // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> HandOfOrderInterrogation.handOfOrderInterrogation(player, level, sl, true)),
            new Ability("Hand Of Order", 4, "Verdict", 6, SNEAK, STEALABLE,                     // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> HandOfOrderVerdicts.handOfOrderVerdict(player, level, sl, true)),
            new Ability("Hand Of Order", 5, "Decree", 6, SNEAK, STEALABLE,                      // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> HandOfOrderDecrees.handOfOrderDecree(player, level, sl, true)),
            new Ability("Hand Of Order", 6, "Punishment", 5, SNEAK, STEALABLE,
                    (player, level, sl) -> HandOfOrder.handOfOrderJudgment(player, level, sl, true)),
            new Ability("Hand Of Order", 7, "Order", 2, SNEAK, OWN_PATHWAY_ONLY,                // a power too great to steal
                    (player, level, sl) -> HandOfOrderOrder.handOfOrderOrder(player, level, sl, true)),

            // ---- Frenzied Mage ----
            new Ability("Frenzied Mage", 1, "Spear", 9, SNEAK, STEALABLE,
                    (player, level, sl) -> FrenziedMage.frenziedMageSpear(player, level, sl, true)),
            new Ability("Frenzied Mage", 2, "Flame", 8, SNEAK, STEALABLE,
                    (player, level, sl) -> FrenziedMage.frenziedMageFlame(player, level, sl, true)),
            new Ability("Frenzied Mage", 3, "Frost", 7, SNEAK, STEALABLE,
                    (player, level, sl) -> FrenziedMage.frenziedMageFrost(player, level, sl, true)),
            new Ability("Frenzied Mage", 4, "Hammer", 6, SNEAK, STEALABLE,
                    (player, level, sl) -> FrenziedMage.frenziedMageHammer(player, level, sl, true)),
            new Ability("Frenzied Mage", 5, "Meteor", 5, SNEAK, STEALABLE,
                    (player, level, sl) -> FrenziedMage.frenziedMageMeteor(player, level, sl, true)),
            new Ability("Frenzied Mage", 6, "Entropy", 4, SNEAK, STEALABLE,
                    (player, level, sl) -> FrenziedMage.frenziedMageEntropy(player, level, sl, true)),
            new Ability("Frenzied Mage", 7, "Abolition", 3, SNEAK, STEALABLE,
                    (player, level, sl) -> FrenziedMage.frenziedMageAbolition(player, level, sl, true)),

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

            // ---- Apocalypse ----
            new Ability("Apocalypse", 1, "Fatal Step", 9, SNEAK, STEALABLE,
                    (player, level, sl) -> Apocalypse.apocalypseFatalStep(player, level, sl, true)),
            new Ability("Apocalypse", 2, "Black Flame", 8, SNEAK, STEALABLE,
                    (player, level, sl) -> Apocalypse.apocalypseBlackFlame(player, level, sl, true)),
            new Ability("Apocalypse", 3, "Mirror World", 7, SNEAK, OWN_PATHWAY_ONLY,      // the same key is the way back out
                    (player, level, sl) -> Apocalypse.apocalypseMirrorWorld(player, level, sl, true)),
            new Ability("Apocalypse", 4, "Silk Weaver", 6, SNEAK, STEALABLE,            // stolen: no wheel, its last (or a random) silk
                    (player, level, sl) -> Apocalypse.apocalypseSilkWeaver(player, level, sl, true)),
            new Ability("Apocalypse", 5, "Plague", 5, SNEAK, STEALABLE,
                    (player, level, sl) -> Apocalypse.apocalypsePlague(player, level, sl, true)),
            new Ability("Apocalypse", 6, "Unaging", 4, SNEAK, STEALABLE,
                    (player, level, sl) -> Apocalypse.apocalypseUnaging(player, level, sl, true)),
            new Ability("Apocalypse", 7, "Catastrophe", 3, SNEAK, STEALABLE,            // stolen: no wheel, its last (or a random) disaster
                    (player, level, sl) -> Apocalypse.apocalypseCatastrophe(player, level, sl, true)),

            // ---- Dark Angel ----
            new Ability("Dark Angel", 1, "Divination", 9, SNEAK, STEALABLE,
                    (player, level, sl) -> DarkAngel.hangedManDivination(player, level, sl, true)),
            new Ability("Dark Angel", 2, "Whispers", 8, SNEAK, STEALABLE,
                    (player, level, sl) -> DarkAngel.hangedManWhispers(player, level, sl, true)),
            new Ability("Dark Angel", 3, "Shadow Binding", 7, SNEAK, OWN_PATHWAY_ONLY,  // the same key is the way back out of the shadow
                    (player, level, sl) -> DarkAngel.hangedManShadowBinding(player, level, sl, true)),
            new Ability("Dark Angel", 4, "Flesh Bomb", 6, SNEAK, STEALABLE,
                    (player, level, sl) -> DarkAngel.hangedManFleshBomb(player, level, sl, true)),
            new Ability("Dark Angel", 5, "Grazing", 5, SNEAK, OWN_PATHWAY_ONLY,         // its slots and wheel are the Hanged Man's own
                    (player, level, sl) -> DarkAngel.hangedManGrazing(player, level, sl, true)),
            new Ability("Dark Angel", 6, "Cull of Spiritual Flesh", 4, SNEAK, STEALABLE,
                    (player, level, sl) -> DarkAngel.hangedManCull(player, level, sl, true)),
            new Ability("Dark Angel", 7, "Degeneration", 3, SNEAK, STEALABLE,
                    (player, level, sl) -> DarkAngel.hangedManDegeneration(player, level, sl, true)),

            // ---- Beauty Goddess ----
            new Ability("Beauty Goddess", 1, "Medicine Concoction", 9, SNEAK, STEALABLE,
                    (player, level, sl) -> BeautyGoddess.beautyGoddessMedicine(player, level, sl, true)),
            new Ability("Beauty Goddess", 2, "Taming", 8, SNEAK, STEALABLE,
                    (player, level, sl) -> BeautyGoddess.beautyGoddessTaming(player, level, sl, true)),
            new Ability("Beauty Goddess", 3, "Wings of Darkness", 7, SNEAK, STEALABLE,
                    (player, level, sl) -> BeautyGoddess.beautyGoddessWings(player, level, sl, true)),
            new Ability("Beauty Goddess", 4, "Potions", 6, SNEAK, STEALABLE,                 // stolen: no wheel, its last (or a random) potion
                    (player, level, sl) -> BeautyGoddess.beautyGoddessPotions(player, level, sl, true)),
            new Ability("Beauty Goddess", 5, "Lunar Battlefield", 5, SNEAK, STEALABLE,
                    (player, level, sl) -> BeautyGoddess.beautyGoddessLunarBattlefield(player, level, sl, true)),
            new Ability("Beauty Goddess", 6, "Gaze of Darkness", 4, SNEAK, STEALABLE,
                    (player, level, sl) -> BeautyGoddess.beautyGoddessGaze(player, level, sl, true)),
            new Ability("Beauty Goddess", 7, "Moon", 3, SNEAK, STEALABLE,                   // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> BeautyGoddess.beautyGoddessMoonWheel(player, level, sl, true)),

            // ---- Knight Of Misfortune (docs/KNIGHT_OF_MISFORTUNE.md) ----
            new Ability("Knight Of Misfortune", 1, "Night Sight", 9, SNEAK, STEALABLE,
                    (player, level, sl) -> KnightOfMisfortune.knightOfMisfortuneNightSight(player, level, sl, true)),
            new Ability("Knight Of Misfortune", 2, "Midnight Poem", 8, SNEAK, STEALABLE,       // stolen: no wheel, its last (or a random) poem
                    (player, level, sl) -> KnightOfMisfortunePoems.knightOfMisfortuneMidnightPoem(player, level, sl, true)),
            new Ability("Knight Of Misfortune", 3, "Nightmare", 7, SNEAK, OWN_PATHWAY_ONLY,     // the Nightmare's own flesh and soul
                    (player, level, sl) -> KnightOfMisfortune.knightOfMisfortuneNightmare(player, level, sl, true)),
            new Ability("Knight Of Misfortune", 4, "Requiem", 6, SNEAK, STEALABLE,
                    (player, level, sl) -> KnightOfMisfortune.knightOfMisfortuneRequiem(player, level, sl, true)),
            new Ability("Knight Of Misfortune", 5, "Spirit Commanding", 5, SNEAK, OWN_PATHWAY_ONLY,   // the spirits live in its teeth
                    (player, level, sl) -> KnightOfMisfortuneSpirits.knightOfMisfortuneSpiritCommanding(player, level, sl, true)),
            new Ability("Knight Of Misfortune", 6, "Nightwatcher", 4, SNEAK, STEALABLE,         // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> KnightOfMisfortuneNightwatcher.knightOfMisfortuneNightwatcher(player, level, sl, true)),
            new Ability("Knight Of Misfortune", 7, "Darkness and Concealment", 3, SNEAK, STEALABLE,   // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> KnightOfMisfortuneDarkness.knightOfMisfortuneDarkness(player, level, sl, true)),

            // ---- Hand Of God (docs/HAND_OF_GOD.md) ----
            new Ability("Hand Of God", 1, "Warrior's Charge", 9, SNEAK, STEALABLE,
                    (player, level, sl) -> HandOfGod.handOfGodWarriorsCharge(player, level, sl, true)),
            new Ability("Hand Of God", 2, "Gladiator's Flurry", 8, SNEAK, STEALABLE,
                    (player, level, sl) -> HandOfGod.handOfGodGladiatorsFlurry(player, level, sl, true)),
            new Ability("Hand Of God", 3, "Grandmaster's Edge", 7, SNEAK, STEALABLE,
                    (player, level, sl) -> HandOfGod.handOfGodGrandmastersEdge(player, level, sl, true)),
            new Ability("Hand Of God", 4, "Dawn", 6, SNEAK, STEALABLE,                       // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> HandOfGodDawn.handOfGodDawn(player, level, sl, true)),
            new Ability("Hand Of God", 5, "Protection", 5, SNEAK, OWN_PATHWAY_ONLY,          // the Guardian's own sword and body
                    (player, level, sl) -> HandOfGod.handOfGodProtection(player, level, sl, true)),
            new Ability("Hand Of God", 6, "Demon Hunting", 4, SNEAK, STEALABLE,              // stolen: no wheel; Giant Form refuses a thief
                    (player, level, sl) -> HandOfGodDemonHunting.handOfGodDemonHunting(player, level, sl, true)),
            new Ability("Hand Of God", 7, "Silver and Twilight", 3, SNEAK, STEALABLE,        // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> HandOfGodTwilight.handOfGodTwilight(player, level, sl, true)),

            // ---- Abomination (docs/ABOMINATION.md) ----
            new Ability("Abomination", 1, "Blood Chain", 9, SNEAK, STEALABLE,
                    (player, level, sl) -> Abomination.abominationBloodChain(player, level, sl, true)),
            new Ability("Abomination", 2, "Moonstruck", 8, SNEAK, STEALABLE,
                    (player, level, sl) -> Abomination.abominationMoonstruck(player, level, sl, true)),
            new Ability("Abomination", 3, "Rending Pounce", 7, SNEAK, STEALABLE,              // Werewolf Form refuses a thief
                    (player, level, sl) -> Abomination.abominationRendingPounce(player, level, sl, true)),
            new Ability("Abomination", 4, "Zombie", 6, SNEAK, STEALABLE,                      // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> AbominationZombie.abominationZombie(player, level, sl, true)),
            new Ability("Abomination", 5, "Wraith", 5, SNEAK, OWN_PATHWAY_ONLY,               // Possession needs this key to step out
                    (player, level, sl) -> AbominationWraith.abominationWraith(player, level, sl, true)),
            new Ability("Abomination", 6, "Chains", 4, SNEAK, STEALABLE,                      // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> AbominationChains.abominationChains(player, level, sl, true)),
            new Ability("Abomination", 7, "Curses", 3, SNEAK, STEALABLE,                      // stolen: no wheel; Abomination Form refuses a thief
                    (player, level, sl) -> AbominationCurses.abominationCurses(player, level, sl, true)),

            // ---- Prince Of Abolition (docs/PRINCE_OF_ABOLITION.md). Its wheels open on sneak; a plain press recasts.
            new Ability("Prince Of Abolition", 1, "Verdict", 9, SNEAK, STEALABLE,
                    (player, level, sl) -> PrinceOfAbolition.princeOfAbolitionVerdict(player, level, sl, true)),
            new Ability("Prince Of Abolition", 2, "Brute Force", 8, SNEAK, STEALABLE,
                    (player, level, sl) -> PrinceOfAbolition.princeOfAbolitionBruteForce(player, level, sl, true)),
            new Ability("Prince Of Abolition", 3, "Bribe", 7, SNEAK, STEALABLE,                // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> PrinceOfAbolitionBribes.princeOfAbolitionBribe(player, level, sl, true)),
            new Ability("Prince Of Abolition", 4, "Distort", 6, SNEAK, STEALABLE,
                    (player, level, sl) -> PrinceOfAbolition.princeOfAbolitionDistort(player, level, sl, true)),
            new Ability("Prince Of Abolition", 5, "Disorder", 5, SNEAK, STEALABLE,
                    (player, level, sl) -> PrinceOfAbolition.princeOfAbolitionDisorder(player, level, sl, true)),
            new Ability("Prince Of Abolition", 6, "The Fallen", 8, SNEAK, OWN_PATHWAY_ONLY,    // the mausoleums are the Black Emperor's own
                    (player, level, sl) -> PrinceOfAbolitionFallen.princeOfAbolitionFallen(player, level, sl, true)),
            new Ability("Prince Of Abolition", 7, "Sovereign", 6, SNEAK, STEALABLE,            // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> PrinceOfAbolitionSovereign.princeOfAbolitionSovereign(player, level, sl, true)),

            // ---- Knowledge Emperor (docs/KNOWLEDGE_EMPEROR.md) ----
            new Ability("Knowledge Emperor", 1, "Mystery Prying", 9, SNEAK, STEALABLE,
                    (player, level, sl) -> KnowledgeEmperor.knowledgeEmperorMysteryPrying(player, level, sl, true)),
            new Ability("Knowledge Emperor", 2, "Weak-Point Strike", 8, SNEAK, STEALABLE,
                    (player, level, sl) -> KnowledgeEmperor.knowledgeEmperorWeakPointStrike(player, level, sl, true)),
            new Ability("Knowledge Emperor", 3, "Hex", 7, SNEAK, STEALABLE,
                    (player, level, sl) -> KnowledgeEmperor.knowledgeEmperorSpells(player, level, sl, true)),
            new Ability("Knowledge Emperor", 4, "Read Scroll", 6, SNEAK, OWN_PATHWAY_ONLY,     // reads the Professor's own pouch
                    (player, level, sl) -> KnowledgeEmperor.knowledgeEmperorScrolls(player, level, sl, true)),
            new Ability("Knowledge Emperor", 5, "Stars", 5, SNEAK, STEALABLE,                  // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> KnowledgeEmperorStars.knowledgeEmperorStars(player, level, sl, true)),
            new Ability("Knowledge Emperor", 6, "Retellings and Prophecy", 4, SNEAK, STEALABLE,  // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> KnowledgeEmperorRetellings.knowledgeEmperorRetellings(player, level, sl, true)),
            new Ability("Knowledge Emperor", 7, "Knowledge", 2, SNEAK, STEALABLE,                // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> KnowledgeEmperorKnowledge.knowledgeEmperorKnowledge(player, level, sl, true)),

            // ---- Illuminator (docs/ILLUMINATOR.md) ----
            new Ability("Illuminator", 1, "Compound Flask", 9, SNEAK, STEALABLE,
                    (player, level, sl) -> Illuminator.illuminatorCompoundFlask(player, level, sl, true)),
            new Ability("Illuminator", 2, "Survey", 8, SNEAK, STEALABLE,
                    (player, level, sl) -> Illuminator.illuminatorSurvey(player, level, sl, true)),
            new Ability("Illuminator", 3, "Appraise", 7, SNEAK, STEALABLE,
                    (player, level, sl) -> Illuminator.illuminatorAppraise(player, level, sl, true)),
            new Ability("Illuminator", 4, "Workshop", 6, SNEAK, STEALABLE,                     // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> IlluminatorWorkshop.illuminatorWorkshop(player, level, sl, true)),
            new Ability("Illuminator", 5, "Astronomy", 5, SNEAK, STEALABLE,                    // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> IlluminatorAstronomy.illuminatorAstronomy(player, level, sl, true)),
            new Ability("Illuminator", 6, "Alchemy and Arcana", 4, SNEAK, STEALABLE,           // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> IlluminatorAlchemy.illuminatorAlchemy(player, level, sl, true)),
            new Ability("Illuminator", 7, "Civilization", 2, SNEAK, STEALABLE,                 // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> IlluminatorCivilization.illuminatorCivilization(player, level, sl, true)),
            // ---- Star Dragon (docs/STAR_DRAGON.md) ----
            new Ability("Star Dragon", 1, "Star Chart", 9, SNEAK, STEALABLE,                       // sneak: Fate Glimpse (8)
                    (player, level, sl) -> StarDragon.starDragonAstronomy(player, level, sl, true)),
            new Ability("Star Dragon", 2, "Star Sacrifice", 7, SNEAK, STEALABLE,                   // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> StarDragon.starDragonStarSacrifice(player, level, sl, true)),
            new Ability("Star Dragon", 3, "Void Passage", 6, SNEAK, STEALABLE,                     // sneak: Piercing Ray
                    (player, level, sl) -> StarDragon.starDragonNavigator(player, level, sl, true)),
            new Ability("Star Dragon", 4, "Tidal Surge", 5, SNEAK, STEALABLE,                      // sneak: Scaled Form
                    (player, level, sl) -> StarDragon.starDragonTidalScholar(player, level, sl, true)),
            new Ability("Star Dragon", 5, "Heavybringing", 4, SNEAK, STEALABLE,                    // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> StarDragon.starDragonHeavybringer(player, level, sl, true)),
            new Ability("Star Dragon", 6, "Star Grazing", 3, SNEAK, STEALABLE,                     // sneak: Fission Pulse
                    (player, level, sl) -> StarDragon.starDragonStarShepherd(player, level, sl, true)),
            new Ability("Star Dragon", 7, "Cosmos", 2, SNEAK, STEALABLE,                           // stolen: no wheel, its last (or a random) entry
                    (player, level, sl) -> StarDragon.starDragonCosmos(player, level, sl, true)));




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
