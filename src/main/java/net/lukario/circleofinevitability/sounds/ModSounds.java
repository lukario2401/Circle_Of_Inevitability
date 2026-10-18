package net.lukario.circleofinevitability.sounds;

import net.lukario.circleofinevitability.CircleOfInevitability;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * The mod's own sounds. Each one is an event in assets/circle_of_inevitability/sounds.json, whose files are in
 * assets/circle_of_inevitability/sounds/. Play one from server code like any vanilla sound:
 *
 *   Spells.sound(sl, at, ModSounds.BEAUTY_MOONRISE.get(), 1.0f, 1.0f);
 *
 * A new sound: its .ogg (mono, so it is placed in the world) in sounds/, an event in sounds.json with the same name
 * as here, a subtitle in lang/en_us.json, and a line below. The Beauty Goddess ones are made by
 * tools/model_gen/beauty_goddess/sounds.py, the Hand Of God ones by tools/model_gen/hand_of_god/sounds.py, the Abomination ones by
 * tools/model_gen/abomination/sounds.py and the Prince Of Abolition ones by tools/model_gen/prince_of_abolition/sounds.py, which write
 * the .ogg files, sounds.json and the subtitles' names.
 */
public final class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, CircleOfInevitability.MOD_ID);

    private ModSounds() {}

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                Identifier.fromNamespaceAndPath(CircleOfInevitability.MOD_ID, name)));
    }

    // ---------- Beauty Goddess (tools/model_gen/beauty_goddess/sounds.py) ----------

    /** Flask thrown. */
    public static final RegistryObject<SoundEvent> BEAUTY_FLASK_THROW = sound("beauty_goddess.flask_throw");
    /** Flask shatters. */
    public static final RegistryObject<SoundEvent> BEAUTY_FLASK_SHATTER = sound("beauty_goddess.flask_shatter");
    /** Potion bubbles. */
    public static final RegistryObject<SoundEvent> BEAUTY_POTION_BUBBLE = sound("beauty_goddess.potion_bubble");
    /** Potion drunk. */
    public static final RegistryObject<SoundEvent> BEAUTY_POTION_DRINK = sound("beauty_goddess.potion_drink");
    /** Healing chimes. */
    public static final RegistryObject<SoundEvent> BEAUTY_HEAL_CHIME = sound("beauty_goddess.heal_chime");
    /** Spirit Vision opens. */
    public static final RegistryObject<SoundEvent> BEAUTY_SPIRIT_VISION = sound("beauty_goddess.spirit_vision");
    /** Blood pact sealed. */
    public static final RegistryObject<SoundEvent> BEAUTY_BLOOD_PACT = sound("beauty_goddess.blood_pact");
    /** Wings of darkness unfurl. */
    public static final RegistryObject<SoundEvent> BEAUTY_WINGS_UNFURL = sound("beauty_goddess.wings_unfurl");
    /** Wings beat. */
    public static final RegistryObject<SoundEvent> BEAUTY_WING_FLAP = sound("beauty_goddess.wing_flap");
    /** Bats swarm. */
    public static final RegistryObject<SoundEvent> BEAUTY_BAT_SWARM = sound("beauty_goddess.bat_swarm");
    /** Abyss shackles bind. */
    public static final RegistryObject<SoundEvent> BEAUTY_SHACKLES = sound("beauty_goddess.shackles");
    /** Life is drained. */
    public static final RegistryObject<SoundEvent> BEAUTY_DRAIN = sound("beauty_goddess.drain");
    /** Someone fades from sight. */
    public static final RegistryObject<SoundEvent> BEAUTY_INVISIBILITY = sound("beauty_goddess.invisibility");
    /** Fire is breathed. */
    public static final RegistryObject<SoundEvent> BEAUTY_FIRE_BREATH = sound("beauty_goddess.fire_breath");
    /** Sun Water bursts. */
    public static final RegistryObject<SoundEvent> BEAUTY_SUN_WATER = sound("beauty_goddess.sun_water");
    /** Someone melts into shadow. */
    public static final RegistryObject<SoundEvent> BEAUTY_SHADOW_MELT = sound("beauty_goddess.shadow_melt");
    /** A full moon rises. */
    public static final RegistryObject<SoundEvent> BEAUTY_MOONRISE = sound("beauty_goddess.moonrise");
    /** Moonlight flashes. */
    public static final RegistryObject<SoundEvent> BEAUTY_MOON_BLINK = sound("beauty_goddess.moon_blink");
    /** Body turns to moonlight. */
    public static final RegistryObject<SoundEvent> BEAUTY_MOONLIGHT_FORM = sound("beauty_goddess.moonlight_form");
    /** Moonlight re-forms. */
    public static final RegistryObject<SoundEvent> BEAUTY_MOONLIGHT_REFORM = sound("beauty_goddess.moonlight_reform");
    /** A dark gaze holds. */
    public static final RegistryObject<SoundEvent> BEAUTY_GAZE = sound("beauty_goddess.gaze");
    /** An eye is crushed. */
    public static final RegistryObject<SoundEvent> BEAUTY_EYE_CRUSH = sound("beauty_goddess.eye_crush");
    /** Darkness hardens. */
    public static final RegistryObject<SoundEvent> BEAUTY_DARKNESS_CLOSE = sound("beauty_goddess.darkness_close");
    /** The Door of Summoning opens. */
    public static final RegistryObject<SoundEvent> BEAUTY_DOOR_OPEN = sound("beauty_goddess.door_open");
    /** Something comes through. */
    public static final RegistryObject<SoundEvent> BEAUTY_SUMMON_ARRIVAL = sound("beauty_goddess.summon_arrival");
    /** Life blooms. */
    public static final RegistryObject<SoundEvent> BEAUTY_CREATION = sound("beauty_goddess.creation");
    /** Divine beauty. */
    public static final RegistryObject<SoundEvent> BEAUTY_BEAUTY = sound("beauty_goddess.beauty");
    /** The Blood Moon rises. */
    public static final RegistryObject<SoundEvent> BEAUTY_BLOOD_MOON = sound("beauty_goddess.blood_moon");

    // ---------- Hand Of God (tools/model_gen/hand_of_god/sounds.py) ----------

    /** A warrior charges. */
    public static final RegistryObject<SoundEvent> HAND_CHARGE = sound("hand_of_god.charge");
    /** A heavy blow lands. */
    public static final RegistryObject<SoundEvent> HAND_IMPACT = sound("hand_of_god.impact");
    /** Divine blood surges. */
    public static final RegistryObject<SoundEvent> HAND_DIVINE_BLOOD = sound("hand_of_god.divine_blood");
    /** A flurry of blows. */
    public static final RegistryObject<SoundEvent> HAND_FLURRY = sound("hand_of_god.flurry");
    /** Supernatural power is shrugged off. */
    public static final RegistryObject<SoundEvent> HAND_RESIST = sound("hand_of_god.resist");
    /** A blade sweeps. */
    public static final RegistryObject<SoundEvent> HAND_EDGE_SWING = sound("hand_of_god.edge_swing");
    /** A finishing cut. */
    public static final RegistryObject<SoundEvent> HAND_FINISHER = sound("hand_of_god.finisher");
    /** A blow is parried. */
    public static final RegistryObject<SoundEvent> HAND_PARRY = sound("hand_of_god.parry");
    /** The light of dawn breaks. */
    public static final RegistryObject<SoundEvent> HAND_DAWN_LIGHT = sound("hand_of_god.dawn_light");
    /** Armor of light forms. */
    public static final RegistryObject<SoundEvent> HAND_DAWN_ARMOR = sound("hand_of_god.dawn_armor");
    /** The Sword of Dawn forms. */
    public static final RegistryObject<SoundEvent> HAND_DAWN_SWORD = sound("hand_of_god.dawn_sword");
    /** A Hurricane of Light roars. */
    public static final RegistryObject<SoundEvent> HAND_HURRICANE = sound("hand_of_god.hurricane");
    /** A wall of dawn rises. */
    public static final RegistryObject<SoundEvent> HAND_PROTECTION = sound("hand_of_god.protection");
    /** Protection holds. */
    public static final RegistryObject<SoundEvent> HAND_DOME_HIT = sound("hand_of_god.dome_hit");
    /** Wounds are taken on. */
    public static final RegistryObject<SoundEvent> HAND_TRANSFERENCE = sound("hand_of_god.transference");
    /** The Eye of Demon Hunting opens. */
    public static final RegistryObject<SoundEvent> HAND_EYE = sound("hand_of_god.eye");
    /** A concoction is brewed. */
    public static final RegistryObject<SoundEvent> HAND_ALCHEMY = sound("hand_of_god.alchemy");
    /** A coated blade sizzles. */
    public static final RegistryObject<SoundEvent> HAND_COAT_HIT = sound("hand_of_god.coat_hit");
    /** A mind is hidden. */
    public static final RegistryObject<SoundEvent> HAND_CONCEAL = sound("hand_of_god.conceal");
    /** A Giant awakens. */
    public static final RegistryObject<SoundEvent> HAND_GIANT_FORM = sound("hand_of_god.giant_form");
    /** A Giant's step. */
    public static final RegistryObject<SoundEvent> HAND_GIANT_STEP = sound("hand_of_god.giant_step");
    /** Liquid silver flows. */
    public static final RegistryObject<SoundEvent> HAND_MERCURY = sound("hand_of_god.mercury");
    /** Silver rapiers strike. */
    public static final RegistryObject<SoundEvent> HAND_RAPIER = sound("hand_of_god.rapier");
    /** Someone steps through light. */
    public static final RegistryObject<SoundEvent> HAND_LIGHT_STEP = sound("hand_of_god.light_step");
    /** The Twilight Sword tears through. */
    public static final RegistryObject<SoundEvent> HAND_TWILIGHT_SWORD = sound("hand_of_god.twilight_sword");
    /** The Divine Right Hand grips. */
    public static final RegistryObject<SoundEvent> HAND_DIVINE_HAND = sound("hand_of_god.divine_hand");
    /** Time stands still. */
    public static final RegistryObject<SoundEvent> HAND_TIME_STOP = sound("hand_of_god.time_stop");
    /** Time resumes. */
    public static final RegistryObject<SoundEvent> HAND_TIME_RESUME = sound("hand_of_god.time_resume");
    /** Everything decays. */
    public static final RegistryObject<SoundEvent> HAND_DECAY = sound("hand_of_god.decay");
    /** A Divine Kingdom descends. */
    public static final RegistryObject<SoundEvent> HAND_KINGDOM = sound("hand_of_god.kingdom");
    /** A cage of light closes. */
    public static final RegistryObject<SoundEvent> HAND_CAGE = sound("hand_of_god.cage");

    // ---------- Knight Of Misfortune (tools/model_gen/knight_of_misfortune/sounds.py) ----------

    /** Spirit Vision opens. */
    public static final RegistryObject<SoundEvent> KNIGHT_SPIRIT_VISION = sound("knight_of_misfortune.spirit_vision");
    /** A pendulum swings. */
    public static final RegistryObject<SoundEvent> KNIGHT_DOWSING = sound("knight_of_misfortune.dowsing");
    /** A Midnight Poem is whispered. */
    public static final RegistryObject<SoundEvent> KNIGHT_POEM = sound("knight_of_misfortune.poem");
    /** All goes still. */
    public static final RegistryObject<SoundEvent> KNIGHT_TRANQUILIZE = sound("knight_of_misfortune.tranquilize");
    /** A lullaby plays. */
    public static final RegistryObject<SoundEvent> KNIGHT_LULLABY = sound("knight_of_misfortune.lullaby");
    /** The will to live fades. */
    public static final RegistryObject<SoundEvent> KNIGHT_PACIFY = sound("knight_of_misfortune.pacify");
    /** Fear takes hold. */
    public static final RegistryObject<SoundEvent> KNIGHT_FEAR = sound("knight_of_misfortune.fear");
    /** Fury is stirred. */
    public static final RegistryObject<SoundEvent> KNIGHT_AGITATE = sound("knight_of_misfortune.agitate");
    /** Nightmare limbs tear free. */
    public static final RegistryObject<SoundEvent> KNIGHT_LIMBS = sound("knight_of_misfortune.limbs");
    /** A limb lashes. */
    public static final RegistryObject<SoundEvent> KNIGHT_LASH = sound("knight_of_misfortune.lash");
    /** A limb is torn off. */
    public static final RegistryObject<SoundEvent> KNIGHT_LIMBS_TEAR = sound("knight_of_misfortune.limbs_tear");
    /** A soul leaves its body. */
    public static final RegistryObject<SoundEvent> KNIGHT_SOUL_OUT = sound("knight_of_misfortune.soul_out");
    /** A soul returns. */
    public static final RegistryObject<SoundEvent> KNIGHT_SOUL_RETURN = sound("knight_of_misfortune.soul_return");
    /** A dream is entered. */
    public static final RegistryObject<SoundEvent> KNIGHT_DREAM_ENTER = sound("knight_of_misfortune.dream_enter");
    /** A dream bends. */
    public static final RegistryObject<SoundEvent> KNIGHT_DREAM_SHAPE = sound("knight_of_misfortune.dream_shape");
    /** A Requiem is sung. */
    public static final RegistryObject<SoundEvent> KNIGHT_REQUIEM = sound("knight_of_misfortune.requiem");
    /** Souls are soothed. */
    public static final RegistryObject<SoundEvent> KNIGHT_SOOTHE = sound("knight_of_misfortune.soothe");
    /** A spirit leaves the teeth. */
    public static final RegistryObject<SoundEvent> KNIGHT_TEETH = sound("knight_of_misfortune.teeth");
    /** A banshee wails. */
    public static final RegistryObject<SoundEvent> KNIGHT_BANSHEE = sound("knight_of_misfortune.banshee");
    /** Something moves through the earth. */
    public static final RegistryObject<SoundEvent> KNIGHT_EARTH = sound("knight_of_misfortune.earth");
    /** A gale spirit lifts. */
    public static final RegistryObject<SoundEvent> KNIGHT_GALE = sound("knight_of_misfortune.gale");
    /** The spirits answer. */
    public static final RegistryObject<SoundEvent> KNIGHT_COMMAND_SPIRITS = sound("knight_of_misfortune.command_spirits");
    /** The night spreads. */
    public static final RegistryObject<SoundEvent> KNIGHT_NIGHT_DOMAIN = sound("knight_of_misfortune.night_domain");
    /** Hair lashes out. */
    public static final RegistryObject<SoundEvent> KNIGHT_HAIR = sound("knight_of_misfortune.hair");
    /** Misfortune is bestowed. */
    public static final RegistryObject<SoundEvent> KNIGHT_CURSE = sound("knight_of_misfortune.curse");
    /** Something goes wrong. */
    public static final RegistryObject<SoundEvent> KNIGHT_MISHAP = sound("knight_of_misfortune.mishap");
    /** Catastrophe strikes. */
    public static final RegistryObject<SoundEvent> KNIGHT_CATASTROPHE = sound("knight_of_misfortune.catastrophe");
    /** A demonic wolf awakens. */
    public static final RegistryObject<SoundEvent> KNIGHT_WOLF_FORM = sound("knight_of_misfortune.wolf_form");
    /** A demonic wolf howls. */
    public static final RegistryObject<SoundEvent> KNIGHT_WOLF_HOWL = sound("knight_of_misfortune.wolf_howl");
    /** A wolf bites. */
    public static final RegistryObject<SoundEvent> KNIGHT_WOLF_BITE = sound("knight_of_misfortune.wolf_bite");
    /** A wolf's step. */
    public static final RegistryObject<SoundEvent> KNIGHT_WOLF_STEP = sound("knight_of_misfortune.wolf_step");
    /** The Sword of Darkness forms. */
    public static final RegistryObject<SoundEvent> KNIGHT_DARK_SWORD = sound("knight_of_misfortune.dark_sword");
    /** A tide of Darkness surges. */
    public static final RegistryObject<SoundEvent> KNIGHT_DARK_TIDE = sound("knight_of_misfortune.dark_tide");
    /** Eternal Sleep. */
    public static final RegistryObject<SoundEvent> KNIGHT_EDICT_SLEEP = sound("knight_of_misfortune.edict_sleep");
    /** Despair. */
    public static final RegistryObject<SoundEvent> KNIGHT_EDICT_DESPAIR = sound("knight_of_misfortune.edict_despair");
    /** Destruction. */
    public static final RegistryObject<SoundEvent> KNIGHT_EDICT_DESTRUCTION = sound("knight_of_misfortune.edict_destruction");
    /** A Nightmare World opens. */
    public static final RegistryObject<SoundEvent> KNIGHT_NIGHTMARE_WORLD = sound("knight_of_misfortune.nightmare_world");
    /** Horror. */
    public static final RegistryObject<SoundEvent> KNIGHT_HORROR = sound("knight_of_misfortune.horror");
    /** Something is concealed. */
    public static final RegistryObject<SoundEvent> KNIGHT_CONCEAL = sound("knight_of_misfortune.conceal");
    /** Something is revealed. */
    public static final RegistryObject<SoundEvent> KNIGHT_REVEAL = sound("knight_of_misfortune.reveal");
    /** A secret is hidden. */
    public static final RegistryObject<SoundEvent> KNIGHT_SECRET = sound("knight_of_misfortune.secret");
    /** A Concealed World closes. */
    public static final RegistryObject<SoundEvent> KNIGHT_CONCEALED_WORLD = sound("knight_of_misfortune.concealed_world");
    /** Darkness gathers. */
    public static final RegistryObject<SoundEvent> KNIGHT_MANTLE = sound("knight_of_misfortune.mantle");
    /** Shadows strike. */
    public static final RegistryObject<SoundEvent> KNIGHT_SHADOW_SWING = sound("knight_of_misfortune.shadow_swing");
    /** Eternal Rest. */
    public static final RegistryObject<SoundEvent> KNIGHT_ETERNAL_REST = sound("knight_of_misfortune.eternal_rest");
    /** The Destination of All Things opens. */
    public static final RegistryObject<SoundEvent> KNIGHT_DESTINATION = sound("knight_of_misfortune.destination");
    /** Something dissolves into darkness. */
    public static final RegistryObject<SoundEvent> KNIGHT_DISSOLVE = sound("knight_of_misfortune.dissolve");

    // ---- Abomination (made by tools/model_gen/abomination/sounds.py, played by AbominationFx)
    /** A chain of blood lashes out. */
    public static final RegistryObject<SoundEvent> ABOMINATION_CHAIN_WHIP = sound("abomination.chain_whip");
    /** Chains bind tight. */
    public static final RegistryObject<SoundEvent> ABOMINATION_CHAIN_BIND = sound("abomination.chain_bind");
    /** Blood drips. */
    public static final RegistryObject<SoundEvent> ABOMINATION_BLEED = sound("abomination.bleed");
    /** Chains lock round a body. */
    public static final RegistryObject<SoundEvent> ABOMINATION_RESTRAINT = sound("abomination.restraint");
    /** Chains fall away. */
    public static final RegistryObject<SoundEvent> ABOMINATION_UNCHAIN = sound("abomination.unchain");
    /** A false moon stares. */
    public static final RegistryObject<SoundEvent> ABOMINATION_MOONSTRUCK = sound("abomination.moonstruck");
    /** Mad laughter. */
    public static final RegistryObject<SoundEvent> ABOMINATION_MADNESS = sound("abomination.madness");
    /** A heart races in frenzy. */
    public static final RegistryObject<SoundEvent> ABOMINATION_FRENZY = sound("abomination.frenzy");
    /** The frenzy fades. */
    public static final RegistryObject<SoundEvent> ABOMINATION_FRENZY_CRASH = sound("abomination.frenzy_crash");
    /** A werewolf pounces. */
    public static final RegistryObject<SoundEvent> ABOMINATION_POUNCE = sound("abomination.pounce");
    /** Claws rend. */
    public static final RegistryObject<SoundEvent> ABOMINATION_CLAW = sound("abomination.claw");
    /** Bones twist into a wolf. */
    public static final RegistryObject<SoundEvent> ABOMINATION_WEREWOLF_FORM = sound("abomination.werewolf_form");
    /** A werewolf howls. */
    public static final RegistryObject<SoundEvent> ABOMINATION_HOWL = sound("abomination.howl");
    /** The dead claw out of the ground. */
    public static final RegistryObject<SoundEvent> ABOMINATION_RAISE_DEAD = sound("abomination.raise_dead");
    /** A zombie groans. */
    public static final RegistryObject<SoundEvent> ABOMINATION_GROAN = sound("abomination.groan");
    /** A grave chill spreads. */
    public static final RegistryObject<SoundEvent> ABOMINATION_GRAVE_CHILL = sound("abomination.grave_chill");
    /** Ice locks a body. */
    public static final RegistryObject<SoundEvent> ABOMINATION_FREEZE = sound("abomination.freeze");
    /** A heart stops, yet goes on. */
    public static final RegistryObject<SoundEvent> ABOMINATION_UNDYING = sound("abomination.undying");
    /** A death knell tolls. */
    public static final RegistryObject<SoundEvent> ABOMINATION_SENTENCE = sound("abomination.sentence");
    /** The sentence counts down. */
    public static final RegistryObject<SoundEvent> ABOMINATION_SENTENCE_TICK = sound("abomination.sentence_tick");
    /** A death sentence falls. */
    public static final RegistryObject<SoundEvent> ABOMINATION_SENTENCE_FALL = sound("abomination.sentence_fall");
    /** Someone steps into a reflection. */
    public static final RegistryObject<SoundEvent> ABOMINATION_MIRROR_ENTER = sound("abomination.mirror_enter");
    /** Someone steps out of a reflection. */
    public static final RegistryObject<SoundEvent> ABOMINATION_MIRROR_EXIT = sound("abomination.mirror_exit");
    /** A spirit takes a body. */
    public static final RegistryObject<SoundEvent> ABOMINATION_POSSESSION = sound("abomination.possession");
    /** A spirit leaves a body. */
    public static final RegistryObject<SoundEvent> ABOMINATION_POSSESSION_EXIT = sound("abomination.possession_exit");
    /** Ghosts rise. */
    public static final RegistryObject<SoundEvent> ABOMINATION_GHOST_HOST = sound("abomination.ghost_host");
    /** A ghost wails. */
    public static final RegistryObject<SoundEvent> ABOMINATION_GHOST_WAIL = sound("abomination.ghost_wail");
    /** Objects rise on strings. */
    public static final RegistryObject<SoundEvent> ABOMINATION_ANIMATE = sound("abomination.animate");
    /** Something is hurled. */
    public static final RegistryObject<SoundEvent> ABOMINATION_HURL = sound("abomination.hurl");
    /** Grave shackles close. */
    public static final RegistryObject<SoundEvent> ABOMINATION_SHACKLES = sound("abomination.shackles");
    /** Shared pain. */
    public static final RegistryObject<SoundEvent> ABOMINATION_PAIN_LINK = sound("abomination.pain_link");
    /** A curse is laid. */
    public static final RegistryObject<SoundEvent> ABOMINATION_CURSE = sound("abomination.curse");
    /** A cursed relic is placed. */
    public static final RegistryObject<SoundEvent> ABOMINATION_RELIC_SET = sound("abomination.relic_set");
    /** A cursed relic wakes. */
    public static final RegistryObject<SoundEvent> ABOMINATION_RELIC_TRIGGER = sound("abomination.relic_trigger");
    /** A hush falls. */
    public static final RegistryObject<SoundEvent> ABOMINATION_SILENCE = sound("abomination.silence");
    /** A baneful mist rolls in. */
    public static final RegistryObject<SoundEvent> ABOMINATION_BANE_MIST = sound("abomination.bane_mist");
    /** A blight spreads. */
    public static final RegistryObject<SoundEvent> ABOMINATION_BLIGHT_JUMP = sound("abomination.blight_jump");
    /** An abomination takes form. */
    public static final RegistryObject<SoundEvent> ABOMINATION_ABOMINATION_FORM = sound("abomination.abomination_form");
    /** Calamity strikes. */
    public static final RegistryObject<SoundEvent> ABOMINATION_CALAMITY = sound("abomination.calamity");
    /** Thorns burst from the ground. */
    public static final RegistryObject<SoundEvent> ABOMINATION_THORN_BURST = sound("abomination.thorn_burst");
    /** Thorns pierce. */
    public static final RegistryObject<SoundEvent> ABOMINATION_THORN_HIT = sound("abomination.thorn_hit");
    /** A thorned mausoleum seals. */
    public static final RegistryObject<SoundEvent> ABOMINATION_MAUSOLEUM = sound("abomination.mausoleum");
    /** A coffin is sealed. */
    public static final RegistryObject<SoundEvent> ABOMINATION_COFFIN_CLOSE = sound("abomination.coffin_close");
    /** A coffin opens. */
    public static final RegistryObject<SoundEvent> ABOMINATION_COFFIN_OPEN = sound("abomination.coffin_open");
    /** A garden of thorns grows. */
    public static final RegistryObject<SoundEvent> ABOMINATION_THORN_GARDEN = sound("abomination.thorn_garden");
    /** Roses heal. */
    public static final RegistryObject<SoundEvent> ABOMINATION_ROSE_HEAL = sound("abomination.rose_heal");

    // ---------- Prince Of Abolition (tools/model_gen/prince_of_abolition/sounds.py) ----------

    /** A gavel falls. */
    public static final RegistryObject<SoundEvent> ABOLITION_VERDICT = sound("prince_of_abolition.verdict");
    /** Quiet words are spoken. */
    public static final RegistryObject<SoundEvent> ABOLITION_BRAINWASH = sound("prince_of_abolition.brainwash");
    /** A crushing blow. */
    public static final RegistryObject<SoundEvent> ABOLITION_BRUTE_FORCE = sound("prince_of_abolition.brute_force");
    /** A will hardens. */
    public static final RegistryObject<SoundEvent> ABOLITION_IRON_WILL = sound("prince_of_abolition.iron_will");
    /** Coins change hands. */
    public static final RegistryObject<SoundEvent> ABOLITION_BRIBE = sound("prince_of_abolition.bribe");
    /** Strength drains away. */
    public static final RegistryObject<SoundEvent> ABOLITION_WEAKEN = sound("prince_of_abolition.weaken");
    /** Someone is won over. */
    public static final RegistryObject<SoundEvent> ABOLITION_CHARM = sound("prince_of_abolition.charm");
    /** Pride swells. */
    public static final RegistryObject<SoundEvent> ABOLITION_ARROGANCE = sound("prince_of_abolition.arrogance");
    /** A thread pulls taut. */
    public static final RegistryObject<SoundEvent> ABOLITION_CONNECT = sound("prince_of_abolition.connect");
    /** A coin is flicked. */
    public static final RegistryObject<SoundEvent> ABOLITION_COIN_SHOT = sound("prince_of_abolition.coin_shot");
    /** A coin ricochets. */
    public static final RegistryObject<SoundEvent> ABOLITION_RICOCHET = sound("prince_of_abolition.ricochet");
    /** Something is distorted. */
    public static final RegistryObject<SoundEvent> ABOLITION_DISTORT = sound("prince_of_abolition.distort");
    /** A shot is turned back. */
    public static final RegistryObject<SoundEvent> ABOLITION_REFLECT = sound("prince_of_abolition.reflect");
    /** An illusory door appears. */
    public static final RegistryObject<SoundEvent> ABOLITION_DOOR_RISE = sound("prince_of_abolition.door_rise");
    /** A door slams shut. */
    public static final RegistryObject<SoundEvent> ABOLITION_SEAL = sound("prince_of_abolition.seal");
    /** A Seal breaks. */
    public static final RegistryObject<SoundEvent> ABOLITION_SEAL_BREAK = sound("prince_of_abolition.seal_break");
    /** Greed spreads. */
    public static final RegistryObject<SoundEvent> ABOLITION_CORROSION = sound("prince_of_abolition.corrosion");
    /** A beam of ruin. */
    public static final RegistryObject<SoundEvent> ABOLITION_RUIN = sound("prince_of_abolition.ruin");
    /** Someone steps through a mausoleum. */
    public static final RegistryObject<SoundEvent> ABOLITION_STEP = sound("prince_of_abolition.step");
    /** Order comes apart. */
    public static final RegistryObject<SoundEvent> ABOLITION_DISORDER = sound("prince_of_abolition.disorder");
    /** Distance is disordered. */
    public static final RegistryObject<SoundEvent> ABOLITION_PACES = sound("prince_of_abolition.paces");
    /** A false self bursts. */
    public static final RegistryObject<SoundEvent> ABOLITION_DECOY_BURST = sound("prince_of_abolition.decoy_burst");
    /** Majesty presses down. */
    public static final RegistryObject<SoundEvent> ABOLITION_MAJESTY = sound("prince_of_abolition.majesty");
    /** A feeling is bestowed. */
    public static final RegistryObject<SoundEvent> ABOLITION_BESTOWMENT = sound("prince_of_abolition.bestowment");
    /** A rule gives way. */
    public static final RegistryObject<SoundEvent> ABOLITION_EXPLOIT = sound("prince_of_abolition.exploit");
    /** Something is magnified. */
    public static final RegistryObject<SoundEvent> ABOLITION_MAGNIFY = sound("prince_of_abolition.magnify");
    /** The ground quakes. */
    public static final RegistryObject<SoundEvent> ABOLITION_QUAKE = sound("prince_of_abolition.quake");
    /** A mausoleum rises. */
    public static final RegistryObject<SoundEvent> ABOLITION_MAUSOLEUM_RAISE = sound("prince_of_abolition.mausoleum_raise");
    /** A mausoleum crumbles. */
    public static final RegistryObject<SoundEvent> ABOLITION_MAUSOLEUM_CRUMBLE = sound("prince_of_abolition.mausoleum_crumble");
    /** A mausoleum fires. */
    public static final RegistryObject<SoundEvent> ABOLITION_SALVO = sound("prince_of_abolition.salvo");
    /** A sanctuary hums. */
    public static final RegistryObject<SoundEvent> ABOLITION_SANCTUARY = sound("prince_of_abolition.sanctuary");
    /** The Emperor rises again. */
    public static final RegistryObject<SoundEvent> ABOLITION_REBIRTH = sound("prince_of_abolition.rebirth");
    /** Black bolts burst out. */
    public static final RegistryObject<SoundEvent> ABOLITION_FRENZY = sound("prince_of_abolition.frenzy");
    /** Order unravels. */
    public static final RegistryObject<SoundEvent> ABOLITION_ENTROPY = sound("prince_of_abolition.entropy");
    /** The world falls silent. */
    public static final RegistryObject<SoundEvent> ABOLITION_ABOLITION = sound("prince_of_abolition.abolition");
    /** Something is abolished. */
    public static final RegistryObject<SoundEvent> ABOLITION_EXILE = sound("prince_of_abolition.exile");
    /** Something returns. */
    public static final RegistryObject<SoundEvent> ABOLITION_EXILE_RETURN = sound("prince_of_abolition.exile_return");
    /** Everything turns over. */
    public static final RegistryObject<SoundEvent> ABOLITION_INVERSION = sound("prince_of_abolition.inversion");
    /** A coronation. */
    public static final RegistryObject<SoundEvent> ABOLITION_CORONATION = sound("prince_of_abolition.coronation");
    /** A Decree is spoken. */
    public static final RegistryObject<SoundEvent> ABOLITION_DECREE = sound("prince_of_abolition.decree");
    /** An order stands. */
    public static final RegistryObject<SoundEvent> ABOLITION_ORDER = sound("prince_of_abolition.order");
}
