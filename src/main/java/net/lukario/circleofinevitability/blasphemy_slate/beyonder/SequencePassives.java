package net.lukario.circleofinevitability.blasphemy_slate.beyonder;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.BeautyGoddess;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.abomination.Abomination;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGod;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.illuminator.Illuminator;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortune;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_order.HandOfOrder;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knowledge_emperor.KnowledgeEmperor;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.prince_of_abolition.PrinceOfAbolition;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.star_dragon.StarDragon;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Passives: what a Beyonder always has from a Sequence on (that Sequence and every stronger one),
 * with no key press and no spirituality cost. See docs/SEQUENCES.md, "Passives".
 *
 * To add one, add a line to the list right below. The forms:
 *   effect(4, MobEffects.SATURATION);                           // every Pathway, from Sequence 4, level I
 *   effect(6, MobEffects.NIGHT_VISION, 1);                      // the last number is the level: 0 = I, 1 = II
 *   effect("Pale Emperor", 5, MobEffects.FIRE_RESISTANCE, 0);   // one Pathway only (the exact Pathway name)
 *   custom(3, player -> player.clearFire());                    // anything that is not a potion effect
 *   custom("Wanderer", 2, player -> { ... });               // the same for one Pathway
 *
 * Every passive is checked once a second (CHECK_EVERY_TICKS), on the server, for every living player.
 * An effect is put back by the next check after milk, death or an ability removes it, and it runs out
 * on its own (at most EFFECT_TICKS later) when the player no longer has the Sequence or the Pathway.
 */
@Mod.EventBusSubscriber(modid = "circle_of_inevitability")
public final class SequencePassives {

    private static final List<Passive> PASSIVES = new ArrayList<>();

    // =========================
    // The passives: one line each
    // =========================
    static {
        effect(4, MobEffects.SATURATION); // Sequence 4 and stronger: never hungry

        // ---- Beauty Goddess (docs/BEAUTY_GODDESS.md). Also in BeautyGoddess itself: afterimages when you sprint
        // (Sequence 5), your potions and medicine last longer (Sequence 6).
        custom("Beauty Goddess", 9, player -> player.removeEffect(MobEffects.POISON));          // Apothecary: poison does not take
        effect("Beauty Goddess", 8, MobEffects.STRENGTH, 0);                                    // Beast Tamer: Strength I
        effect("Beauty Goddess", 7, MobEffects.NIGHT_VISION, 0);                                // Vampire: Night Vision...
        custom("Beauty Goddess", 7, player -> {                                                 // ...and wounds close faster
            if (player.getHealth() < player.getMaxHealth()) player.heal(0.5f);
        });
        custom("Beauty Goddess", 5, player -> {                                                 // Scarlet Scholar: Speed II at night
            if (player.level().isDarkOutside()) keepEffect(player, MobEffects.SPEED, 1);
        });
        effect("Beauty Goddess", 4, MobEffects.REGENERATION, 0);                                // Shaman King: strong regeneration
        custom("Beauty Goddess", 1, BeautyGoddess::hesitate);                                   // Beauty Goddess: mobs hesitate to hunt you

        // ---- Hand Of God (docs/HAND_OF_GOD.md). Also in HandOfGod itself: Weapon Mastery (Sequence 7), Giant's
        // Physique (6), half knockback (5), Tracking and Spiritual Intuition (4), wither does not take (2), Divine Touch (1).
        effect("Hand Of God", 9, MobEffects.STRENGTH, 0);                                       // Warrior: Strength I (Divine Blood)
        custom("Hand Of God", 8, HandOfGod::innateResistance);                                  // Pugilist: Supernatural Resistance
        effect("Hand Of God", 5, MobEffects.RESISTANCE, 0);                                     // Guardian: Resistance I
        custom("Hand Of God", 3, HandOfGod::dawnAura);                                          // Silver Knight: Dawn Aura burns evil
        custom("Hand Of God", 2, HandOfGod::twilightResistance);                                // Glory: nothing stops your healing
        custom("Hand Of God", 0, HandOfGod::twilightAura);                                      // Twilight Giant: Twilight Aura

        // ---- Knight Of Misfortune (docs/KNIGHT_OF_MISFORTUNE.md). Also in KnightOfMisfortune itself: the sense of hidden
        // danger (Sequence 9), Steady Hand's falls and bow (8), Spirit Teeth (5, in KnightOfMisfortuneSpirits), Presence of
        // Concealment (2), the Archangel's hearts and Ill Omen (1), what does not take on Darkness (0).
        effect("Knight Of Misfortune", 9, MobEffects.NIGHT_VISION, 0);                          // Sleepless: sees in the dark...
        custom("Knight Of Misfortune", 9, KnightOfMisfortune::nocturnality);                    // ...and is stronger in it
        custom("Knight Of Misfortune", 7, KnightOfMisfortune::resonance);                       // Nightmare: spirituality back faster
        custom("Knight Of Misfortune", 6, KnightOfMisfortune::spiritSight);                     // Soul Assurer: spirits are seen
        custom("Knight Of Misfortune", 4, KnightOfMisfortune::deepNight);                       // Nightwatcher: the sun weakens, the dark veils
        custom("Knight Of Misfortune", 3, KnightOfMisfortune::horrorAura);                      // Horror Bishop: the weak tremble
        custom("Knight Of Misfortune", 2, KnightOfMisfortune::requiemPresence);                 // Servant of Concealment: extreme peace
        effect("Knight Of Misfortune", 1, MobEffects.STRENGTH, 0);                              // Knight of Misfortune: the Archangel's
        effect("Knight Of Misfortune", 1, MobEffects.SPEED, 0);                                 // body (and its hearts, in KnightOfMisfortune)
        custom("Knight Of Misfortune", 0, KnightOfMisfortune::eternalSlumber);                  // Darkness: Eternal Slumber, Horror

        // ---- Abomination (docs/ABOMINATION.md). Also in Abomination itself: Lunatic Mind and the Full Moon (Sequence 8),
        // Keen Senses' warning (7), Unliving Body's immunities and projectile cut (6), Weightless (5), Puppeteer (4, in
        // the summons' times), Whispering Curse (3), the Bane's immunities (2), Collection of Curses (1), Chained's thorns (0).
        effect("Abomination", 9, MobEffects.STRENGTH, 0);                                       // Prisoner: Strength I
        effect("Abomination", 7, MobEffects.SPEED, 0);                                          // Werewolf: Keen Senses, Speed I...
        effect("Abomination", 7, MobEffects.NIGHT_VISION, 0);                                   // ...and Night Vision
        effect("Abomination", 6, MobEffects.REGENERATION, 0);                                   // Zombie: Unliving Body, Regeneration I
        custom("Abomination", 2, Abomination::baneAura);                                        // Ancient Bane: enemies near you lose their luck
        custom("Abomination", 0, Abomination::chainedAura);                                     // Chained: enemies near you are slowed and held to the ground

        // ---- Prince Of Abolition (docs/PRINCE_OF_ABOLITION.md). Also in PrinceOfAbolition itself: Sequence Might and Law &
        // Order (Sequence 9), Physical Enhancement's hearts and Mental Resistance (8), Everyone Has A Price (7), Weakness
        // Detection (6), Presence's hesitation (5); the mausoleums' Sanctuary (7) in PrinceOfAbolitionMausoleums.
        effect("Prince Of Abolition", 9, MobEffects.HERO_OF_THE_VILLAGE, 0);                     // Lawyer: Silver Tongue, better prices
        effect("Prince Of Abolition", 8, MobEffects.STRENGTH, 0);                                // Barbarian: Physical Enhancement, Strength I
        custom("Prince Of Abolition", 5, PrinceOfAbolition::presence);                           // Mentor of Disorder: weak hostile mobs hesitate

        // ---- Hand Of Order (docs/HAND_OF_ORDER.md). Also in HandOfOrder itself: Weapon Proficiency's speed and reach and
        // Sheriff's Intuition (Sequence 8), Demolition Proficiency (7), Judge's Aura (6), Self-Restraint's shorter mind
        // effects (5). Law of Order (0) and the rest of Sense of Order (1) come with the Decrees.
        effect("Hand Of Order", 9, MobEffects.STRENGTH, 0);                                      // Arbiter: Order, Strength I...
        custom("Hand Of Order", 9, HandOfOrder::order);                                          // ...and weak mobs keep their distance
        effect("Hand Of Order", 5, MobEffects.RESISTANCE, 0);                                    // Disciplinary Paladin: Self-Restraint, Resistance I
        custom("Hand Of Order", 4, HandOfOrder::beyonderSense);                                  // Imperative Mage: Beyonders are marked
        custom("Hand Of Order", 3, HandOfOrder::disorderSense);                                  // Chaos Hunter: the disorderly smoulder
        custom("Hand Of Order", 2, HandOfOrder::senseImbalance);                                 // Balancer: the concealed cannot hide
        custom("Hand Of Order", 1, HandOfOrder::senseOfOrder);                                   // Hand of Order: casts near chime

        // ---- Knowledge Emperor (docs/KNOWLEDGE_EMPEROR.md). Also in KnowledgeEmperor itself: Spiritual Intuition's chime
        // (Sequence 9), Fighting Knowledge's reach, speed and Study on every hit (8), Warlock's Mind (7), Information
        // Skin's footing (4), Clairaudience on a blow (3), Sage's Mind (2), Law of Three (1), the Hermit's slow fading (0).
        custom("Knowledge Emperor", 9, KnowledgeEmperor::mysteryPryerSenses);                    // Mystery Pryer: hidden things show faintly; the Whisper
        custom("Knowledge Emperor", 5, KnowledgeEmperor::starlight);                             // Constellations Master: Starlight
        custom("Knowledge Emperor", 4, KnowledgeEmperor::informationSkin);                       // Mysticologist: Information Skin
        custom("Knowledge Emperor", 3, KnowledgeEmperor::clairaudience);                         // Clairvoyant: Clairaudience
        custom("Knowledge Emperor", 0, KnowledgeEmperor::hermitPresence);                        // Hermit: enemies near come to be known
        custom("Knowledge Emperor", 9, KnowledgeEmperor::enlightenment);                         // an Indoctrination's blessing

        // ---- Illuminator (docs/ILLUMINATOR.md). Also in Illuminator itself: Quick Study's cheaper recasts (Sequence 9,
        // in costFor), Field Physique's hearts and lighter falls (8), Appraiser's Eye by the hotbar (7), Craftsman's
        // Hands in the Workshop wheel (6), the Enlightenment Aura's cheaper casts and shorter confusion for allies (1,
        // Illuminator.auraCost and its effect event), Reality Knowledge's marks twice as long and nothing reads it (0).
        custom("Illuminator", 5, Illuminator::starlight);                                        // Astronomer: Starlight
        custom("Illuminator", 4, Illuminator::constructorsBond);                                 // Alchemist: Constructor's Bond
        custom("Illuminator", 3, Illuminator::absorption);                                       // Arcane Scholar: Absorption
        custom("Illuminator", 2, Illuminator::analysis);                                         // Knowledge Magister: Analysis
        custom("Illuminator", 1, Illuminator::enlightenmentAura);                                // Illuminator: Enlightenment Aura

        // ---- Star Dragon (docs/STAR_DRAGON.md). Also in StarDragon itself: Spiritual Intuition's chime (Sequence 8), Star's
        // Embrace's hearts and half falls (7), Starlight Scales' footing (5), Dense Body's footing, no falls and nothing
        // moving it (4), Fission Touch on a blow (3), Gravity Authority's lightness and slowed projectiles (1), Weight and
        // Density's steps and nothing rooting or freezing it (0).
        effect("Star Dragon", 9, MobEffects.STRENGTH, 0);                                       // Astronomy Aficionado: Physical Enhancement
        custom("Star Dragon", 9, StarDragon::stargazing);                                       // ...and Stargazing
        custom("Star Dragon", 6, StarDragon::senseOfPassages);                                  // Navigator: Sense of Passages
        effect("Star Dragon", 5, MobEffects.RESISTANCE, 0);                                     // Tidal Scholar: Starlight Scales, Resistance I...
        custom("Star Dragon", 5, StarDragon::starlightScales);                                  // ...and fast swimming
        custom("Star Dragon", 2, StarDragon::radiance);                                         // Radiant Angel: Radiance
    }

    // =========================
    // Tuning
    // =========================

    /** How often the passives are checked, in ticks (20 = once a second). Custom passives run this often. */
    public static final int CHECK_EVERY_TICKS = 20;

    /**
     * How long an effect is given for, in ticks. It is given again when half of it is left, so the player never
     * runs out. Keep it at 600 or more: night vision flickers when it has under 200 ticks (10 seconds) left.
     */
    public static final int EFFECT_TICKS = 600;

    /** True: the effect's icon shows on the screen and in the inventory. The effects never show particles. */
    public static final boolean SHOW_ICON = true;

    private SequencePassives() {}

    // =========================
    // Adding passives
    // =========================

    /** A potion effect (level I) for every Pathway, from this Sequence on. */
    public static void effect(int sequence, Holder<MobEffect> effect) {
        effect(null, sequence, effect, 0);
    }

    /** A potion effect for every Pathway, from this Sequence on. amplifier: 0 = level I, 1 = level II... */
    public static void effect(int sequence, Holder<MobEffect> effect, int amplifier) {
        effect(null, sequence, effect, amplifier);
    }

    /** A potion effect for one Pathway (its exact name, as in SequenceCommands.PATHWAYS), from this Sequence on. */
    public static void effect(String pathway, int sequence, Holder<MobEffect> effect, int amplifier) {
        custom(pathway, sequence, player -> keepEffect(player, effect, amplifier));
    }

    /** Anything else, for every Pathway: the action runs every CHECK_EVERY_TICKS while the player has the Sequence. */
    public static void custom(int sequence, Consumer<Player> action) {
        custom(null, sequence, action);
    }

    /** Anything else, for one Pathway: the action runs every CHECK_EVERY_TICKS while the player has the Sequence. */
    public static void custom(String pathway, int sequence, Consumer<Player> action) {
        if (pathway != null && !SequenceCommands.PATHWAYS.contains(pathway)) {
            // a misspelt name would never match any player, so say it at once instead of doing nothing
            throw new IllegalArgumentException("SequencePassives: unknown Pathway \"" + pathway
                    + "\". Use one of " + SequenceCommands.PATHWAYS);
        }
        PASSIVES.add(new Passive(pathway, sequence, action));
    }

    // =========================
    // How they run
    // =========================

    /** pathway: null = every Pathway. sequence: the weakest Sequence that has it. */
    private record Passive(String pathway, int sequence, Consumer<Player> action) {

        boolean appliesTo(Player player) {
            return Beyonder.hasSequence(player, sequence)
                    && (pathway == null || pathway.equals(SoulCore.getPathway(player)));
        }
    }

    @SubscribeEvent
    public static void onSequencePassivesPlayerTick(TickEvent.PlayerTickEvent.Post event) {
        if (event.player().level().isClientSide()) return;
        Player player = event.player();
        if (player.tickCount % CHECK_EVERY_TICKS != 0 || !player.isAlive()) return;

        for (Passive passive : PASSIVES) {
            if (passive.appliesTo(player)) passive.action().accept(player);
        }
    }

    /**
     * Gives the effect for EFFECT_TICKS, without particles. Leaves it alone while the player still has it at this
     * level or a stronger one with more than half of the time left, so it is sent to the client only now and then.
     */
    private static void keepEffect(Player player, Holder<MobEffect> effect, int amplifier) {
        MobEffectInstance has = player.getEffect(effect);
        if (has != null && has.getAmplifier() >= amplifier
                && (has.getDuration() > EFFECT_TICKS / 2 || has.isInfiniteDuration())) return;
        player.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, amplifier, true, false, SHOW_ICON));
    }
}
