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
 * tools/model_gen/beauty_goddess/sounds.py, which writes the .ogg files, sounds.json and the subtitles' names.
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
}
