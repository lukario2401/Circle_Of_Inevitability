package net.lukario.circleofinevitability.blasphemy_slate.beyonder;

import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.BeautyGoddess;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.hand_of_god.HandOfGod;
import net.lukario.circleofinevitability.blasphemy_slate.beyonder_characteristics.knight_of_misfortune.KnightOfMisfortune;
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
 *   custom("Key Of Stars", 2, player -> { ... });               // the same for one Pathway
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
        // Keen Senses' warning (7), Unliving Body's immunities and projectile cut (6), Weightless (5). Sequences 4 to 0
        // have no passives yet.
        effect("Abomination", 9, MobEffects.STRENGTH, 0);                                       // Prisoner: Strength I
        effect("Abomination", 7, MobEffects.SPEED, 0);                                          // Werewolf: Keen Senses, Speed I...
        effect("Abomination", 7, MobEffects.NIGHT_VISION, 0);                                   // ...and Night Vision
        effect("Abomination", 6, MobEffects.REGENERATION, 0);                                   // Zombie: Unliving Body, Regeneration I
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
