"""Synthesizes the Key Of Stars sounds and writes sounds.json.

python3 tools/model_gen/key_of_stars/sounds.py src/main/resources/assets/circle_of_inevitability [--wav dir]

Every sound is built from scratch (sine partials, filtered noise, formant choirs, a synthetic room), so nothing
here is copied from anywhere. The building blocks (tone, bell, choir, room, thump, clank, creak, sparkle...) are the
ones in tools/model_gen/beauty_goddess/sounds.py, and the scholar's (whoosh, tick, rustle, scratch, shimmer,
crackle, chime, music_box) those of tools/model_gen/knowledge_emperor/sounds.py, loaded from there; this file adds
the Door's own: an illusory door swinging, a lock springing, starlight ringing, a fold in space, clockwork.
Needs numpy, scipy and ffmpeg (with libvorbis).

The .ogg files go to sounds/key_of_stars/, mono 44.1 kHz (Minecraft only places mono sounds in the world), and each
one gets an event "key_of_stars.<name>" in sounds.json and a subtitle in lang/en_us.json. The same names are
registered in sounds/ModSounds.java (STARS_...): a new sound here needs a line there too.

To change one: edit its function, run this again, press F3+T in game (sounds reload with the resources).
--wav dir also writes .wav copies there, to listen to them outside the game.
"""
import importlib.util
import json
import os
import sys

import numpy as np

_here = os.path.dirname(os.path.abspath(__file__))


def _load(name, rel):
    spec = importlib.util.spec_from_file_location(name, os.path.join(_here, '..', rel))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


kit = _load('beauty_sounds', 'beauty_goddess/sounds.py')
scholar = _load('knowledge_sounds', 'knowledge_emperor/sounds.py')

SR = kit.SR
RNG = np.random.default_rng(2401)
silence, noise, place, env_adsr, decay, fade = kit.silence, kit.noise, kit.place, kit.env_adsr, kit.decay, kit.fade
lowpass, highpass, bandpass, sweep, tone, glide = kit.lowpass, kit.highpass, kit.bandpass, kit.sweep, kit.tone, kit.glide
bell, choir, voice, room, thump, finish = kit.bell, kit.choir, kit.voice, kit.room, kit.thump, kit.finish
sparkle, drone, clank, creak, t_axis = kit.sparkle, kit.drone, kit.clank, kit.creak, kit.t_axis
whoosh, tick, rustle, scratch, shimmer, crackle = (scholar.whoosh, scholar.tick, scholar.rustle, scholar.scratch,
                                                  scholar.shimmer, scholar.crackle)
chime, music_box, reverse = scholar.chime, scholar.music_box, scholar.reverse


def n_of(seconds):
    return int(seconds * SR)


def env(seconds, a, r, sustain=0.8):
    return env_adsr(n_of(seconds), a, 0.05, 0, r, sustain)


# =====================================================================================================
# Building blocks of the Door: starlight, an illusory door, a lock, a fold in space, clockwork
# =====================================================================================================

def starlight(seconds, count=18, lo=3500, hi=9500, level=0.3):
    """Starlight ringing: a veil of high chimes and shimmer."""
    out = sparkle(seconds, count, lo, hi) * level
    out += shimmer(seconds, lo * 0.8, hi) * level * 0.25
    return out


def star_chord(root, seconds, intervals=(0, 7, 12, 16, 19), level=0.25):
    """A glassy chord of chimes, one after another like stars coming out."""
    out = silence(seconds)
    for i, semi in enumerate(intervals):
        place(out, chime(root * 2 ** (semi / 12), min(seconds, 1.6), 0.7) * level, i * 0.05)
    return out


def ghost_door(seconds=1.0, pitch=1.0, heavy=0.5):
    """An illusory door swinging: a faint glassy creak, the air it moves and a soft hollow knock."""
    out = silence(seconds)
    c = creak(seconds * 0.6, 40 * pitch, 110 * pitch)
    c = highpass(c, 600) * env(seconds * 0.6, 0.1, 0.3, 0.7) * 0.5
    place(out, c, 0.02)
    place(out, whoosh(seconds * 0.6, 300, 2200, 0.55, 0.6) * 0.5, 0.05)
    place(out, thump(70 * pitch, 0.3, 0.7, 0.08) * heavy, seconds * 0.55)
    return out


def lock_spring(freq=1800):
    """A lock springing open: a dry click, the bolt sliding back, a small bright ring."""
    out = silence(0.6)
    place(out, tick(freq) * 1.0, 0)
    place(out, bandpass(noise(0.06), 1500, 6000) * decay(n_of(0.06), 0.02) * 0.5, 0.03)
    place(out, clank(freq * 0.5) * 0.5, 0.07)
    place(out, chime(freq * 1.2, 0.5, 0.8) * 0.25, 0.08)
    return out


def fold(seconds=0.7, down=True):
    """Space folding: a deep sucking sweep with a hollow tone gliding under it."""
    n = n_of(seconds)
    f = (lambda p: 3000 * (0.08 ** p)) if down else (lambda p: 240 * (12 ** p))
    out = sweep(noise(seconds), f, 0.5) * env_adsr(n, 0.15, 0.1, 0, seconds * 0.4, 0.8) * 0.8
    out += tone(glide(260, 70, seconds, 0.6) if down else glide(70, 260, seconds, 0.6), seconds) \
        * env_adsr(n, 0.1, 0.1, 0, seconds * 0.5, 0.7) * 0.35
    return out


def clockwork(seconds, clicks, freq=2600):
    """A wind-up key turning: even clicks of a ratchet, each with a tiny spring ring."""
    out = silence(seconds)
    for i in range(clicks):
        place(out, tick(freq * RNG.uniform(0.95, 1.05)) * 0.8, i * seconds / max(1, clicks))
        place(out, tone(freq * 1.6, 0.05) * decay(n_of(0.05), 0.01) * 0.15, i * seconds / max(1, clicks) + 0.005)
    return out


def bang(seconds=1.2):
    """A loud bang: a sharp crack, a low boom and a rattling tail."""
    out = silence(seconds)
    place(out, highpass(noise(0.01), 1500) * 2.0, 0)
    place(out, lowpass(noise(0.4), 300, 2) * decay(n_of(0.4), 0.12) * 2.5, 0)
    place(out, thump(42, 0.7, 0.5, 0.25) * 1.2, 0)
    place(out, crackle(0.8, 40, 600, 4000) * 0.3, 0.05)
    return out


def gust(seconds, lo=200, hi=1800):
    """Wind: noise sweeping through a broad band, rising and falling twice."""
    n = n_of(seconds)
    x = sweep(noise(seconds), lambda p: lo + (hi - lo) * (0.5 + 0.5 * np.sin(p * np.pi * 3)), 0.9)
    return x * env_adsr(n, seconds * 0.15, 0.1, 0, seconds * 0.5, 0.9)


def frost(seconds):
    """Frost: brittle high crackling and a cold glassy ring."""
    out = crackle(seconds, int(seconds * 120), 3000, 9000) * 0.6
    out += bell(2600, seconds, 1.0, (1.0, 2.7, 4.9), 0.4) * 0.15
    return out


# =====================================================================================================
# Apprentice: Door Opening, Slip Free
# =====================================================================================================

def s_door_open():
    seconds = 1.6
    out = silence(seconds)
    place(out, ghost_door(1.1, 1.2, 0.4), 0)
    place(out, star_chord(784, 1.4, (0, 4, 7, 12), 0.25), 0.15)
    place(out, starlight(1.2, 14) * 0.6, 0.1)
    return room(out, 1.4, 0.4, 6000)


def s_door_step():
    seconds = 1.0
    out = silence(seconds)
    place(out, fold(0.45, True) * 0.7, 0)
    place(out, chime(1568, 0.8, 0.8) * 0.3, 0.3)
    place(out, thump(60, 0.25, 0.7, 0.06) * 0.6, 0.35)
    return room(out, 1.0, 0.35, 5000)


def s_slip_free():
    seconds = 1.1
    out = silence(seconds)
    place(out, lock_spring(1900) * 0.9, 0)
    for i in range(6):                                                       # bonds falling away
        place(out, clank(RNG.uniform(600, 1200)) * RNG.uniform(0.1, 0.25), 0.12 + i * 0.05)
    place(out, star_chord(1046.5, 0.9, (0, 7, 12), 0.2), 0.15)
    return room(out, 1.0, 0.35, 6000)


# =====================================================================================================
# Trickmaster: the tricks
# =====================================================================================================

def s_flash():
    seconds = 0.8
    out = silence(seconds)
    place(out, highpass(noise(0.02), 3000) * 1.5, 0)                         # the pop of the flash
    place(out, tone(glide(5000, 1200, 0.3, 0.5), 0.3) * env(0.3, 0.001, 0.25) * 0.4, 0)
    place(out, sparkle(0.6, 14, 5000, 10000) * 0.3, 0.02)
    return room(out, 0.8, 0.25, 7000)


def s_black_screen():
    seconds = 1.4
    out = silence(seconds)
    place(out, rustle(1.0, 14) * 0.7, 0)                                     # heavy cloth unfurling
    place(out, whoosh(0.8, 120, 900, 0.4, 0.7) * 0.7, 0)
    place(out, thump(55, 0.35, 0.7, 0.1) * 0.5, 0.6)
    return room(out, 1.2, 0.3, 2500)


def s_loud_noise():
    return room(bang(1.4), 1.8, 0.35, 3500)


def s_frost_ray():
    seconds = 1.0
    out = silence(seconds)
    place(out, whoosh(0.35, 1500, 7000, 0.3, 0.5) * 0.7, 0)
    place(out, frost(0.8) * 0.8, 0.08)
    place(out, tone(glide(1800, 2600, 0.5), 0.5) * env(0.5, 0.01, 0.4) * 0.15, 0)
    return room(out, 0.9, 0.3, 7000)


def s_gale():
    seconds = 1.6
    out = silence(seconds)
    place(out, gust(1.4, 150, 2400) * 1.0, 0)
    place(out, whoosh(0.5, 400, 3000, 0.4, 0.6) * 0.6, 0.1)
    return room(out, 1.0, 0.3, 4000)


def s_tumble():
    seconds = 1.2
    out = silence(seconds)
    for i in range(10):                                                      # wet, slick squelches
        f = RNG.uniform(300, 900)
        d = RNG.uniform(0.04, 0.09)
        blob = tone(glide(f * 1.5, f, d), d) * np.sin(np.linspace(0, np.pi, n_of(d))) * 0.4
        place(out, blob, i * 0.08 + RNG.uniform(0, 0.03))
    place(out, lowpass(noise(0.6), 800) * env(0.6, 0.05, 0.4) * 0.6, 0)
    place(out, star_chord(523.3, 0.8, (0, 3, 7), 0.12), 0.3)
    return room(out, 1.0, 0.3, 3500)


def s_escape_trick():
    seconds = 1.0
    out = silence(seconds)
    place(out, crackle(0.5, 30, 400, 2500) * 0.6, 0)                         # the door left behind crumbling
    place(out, whoosh(0.4, 600, 4000, 0.5, 0.5) * 0.8, 0.05)
    place(out, chime(2093, 0.6, 0.8) * 0.3, 0.3)
    return room(out, 0.9, 0.3, 6000)


# =====================================================================================================
# Astrologer: Star Divination, Interference; the Starlit mark
# =====================================================================================================

def s_divination():
    seconds = 2.2
    out = silence(seconds)
    n = n_of(1.8)
    hum = (tone(220, 1.8) + tone(330, 1.8) * 0.5 + tone(441, 1.8) * 0.3) * env_adsr(n, 0.5, 0.2, 0, 0.8, 0.6) * 0.3
    place(out, hum, 0)
    place(out, shimmer(1.6, 2500, 8000) * 0.4, 0.2)
    place(out, star_chord(659.3, 1.6, (0, 5, 9, 12, 16), 0.2), 0.5)
    return room(out, 1.8, 0.45, 6000)


def s_starlit():
    seconds = 1.0
    out = silence(seconds)
    place(out, chime(2637, 0.9, 0.9) * 0.6, 0)
    place(out, chime(3520, 0.7, 0.9) * 0.35, 0.06)
    place(out, sparkle(0.5, 8, 5000, 9000) * 0.2, 0)
    return room(out, 1.0, 0.4, 7000)


def s_interference():
    seconds = 1.8
    out = silence(seconds)
    n = n_of(1.6)
    static = bandpass(noise(1.6), 1200, 6000) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 9 * t_axis(1.6)))) \
        * env_adsr(n, 0.2, 0.2, 0, 0.8, 0.5) * 0.35
    place(out, static, 0)
    place(out, tone(glide(880, 440, 1.4, 0.7), 1.4) * env(1.4, 0.3, 0.8, 0.5) * 0.15, 0.1)
    place(out, reverse(chime(1318.5, 0.8, 0.8)) * 0.3, 0.6)
    return room(out, 1.4, 0.4, 5000)


def s_interference_reflect():
    seconds = 1.0
    out = silence(seconds)
    place(out, reverse(fold(0.4, True)) * 0.6, 0)
    place(out, chime(1760, 0.7, 1.0) * 0.4, 0.38)
    place(out, crackle(0.3, 14, 2000, 7000) * 0.4, 0.38)
    return room(out, 1.0, 0.35, 6000)


# =====================================================================================================
# Scribe: Record, Replay; Replication (Planeswalker)
# =====================================================================================================

def s_record():
    seconds = 1.6
    out = silence(seconds)
    place(out, rustle(0.5, 8) * 0.6, 0)                                      # the book opening
    place(out, scratch(1.0, 5) * 0.5, 0.35)                                  # the quill, ready
    place(out, shimmer(1.2, 3000, 8000) * 0.2, 0.2)
    return room(out, 1.2, 0.35, 5000)


def s_recorded():
    seconds = 1.6
    out = silence(seconds)
    place(out, scratch(0.6, 6) * 0.6, 0)                                     # written down quickly
    place(out, rustle(0.3, 5) * 0.5, 0.55)                                   # the page turned
    place(out, star_chord(880, 1.0, (0, 4, 7, 12), 0.25), 0.6)
    return room(out, 1.2, 0.35, 6000)


def s_replay():
    seconds = 1.2
    out = silence(seconds)
    place(out, rustle(0.25, 6) * 0.7, 0)                                     # a page torn out
    place(out, bandpass(noise(0.12), 1500, 7000) * env(0.12, 0.005, 0.1) * 0.7, 0.05)
    place(out, whoosh(0.4, 800, 5000, 0.4, 0.5) * 0.5, 0.15)
    place(out, chime(1975.5, 0.8, 0.9) * 0.3, 0.2)
    return room(out, 1.0, 0.3, 6000)


def s_replication():
    seconds = 1.6
    out = silence(seconds)
    for i in range(3):                                                       # an echo, copied
        place(out, chime(1318.5, 0.8, 0.8) * (0.5 - i * 0.12), i * 0.12)
        place(out, chime(1760.0, 0.8, 0.8) * (0.35 - i * 0.08), i * 0.12 + 0.04)
    place(out, scratch(0.4, 3) * 0.4, 0.05)
    place(out, shimmer(1.0, 3000, 9000) * 0.25, 0.2)
    return room(out, 1.4, 0.4, 6000)


# =====================================================================================================
# Traveler: Traveler's Door, Blink, Invisible Hand, Star Anchor; Wanderer: Wandering
# =====================================================================================================

def s_travelers_door():
    seconds = 2.4
    out = silence(seconds)
    place(out, ghost_door(1.4, 0.8, 0.7), 0)
    place(out, choir((146.8, 220.0, 293.7), 1.8, 'oh', 2, 0.6, 1.0) * 0.12, 0.2)
    place(out, fold(0.9, True) * 0.6, 0.9)
    place(out, starlight(1.4, 16) * 0.5, 0.3)
    return room(out, 2.0, 0.45, 5000)


def s_travelers_arrive():
    seconds = 1.4
    out = silence(seconds)
    place(out, fold(0.6, False) * 0.6, 0)
    place(out, ghost_door(0.8, 1.0, 0.5), 0.4)
    place(out, chime(1174.7, 0.9, 0.8) * 0.3, 0.55)
    return room(out, 1.4, 0.4, 5000)


def s_blink(variant):
    seconds = 0.7
    out = silence(seconds)
    pitch = (1.0, 1.12, 1.26)[variant]
    place(out, whoosh(0.22, 1200 * pitch, 7000, 0.4, 0.4) * 0.7, 0)
    place(out, tone(glide(1600 * pitch, 3200 * pitch, 0.12), 0.12) * env(0.12, 0.005, 0.1) * 0.3, 0)
    place(out, chime(2349 * pitch, 0.5, 0.9) * 0.3, 0.1)
    return room(out, 0.7, 0.25, 7000)


def s_invisible_hand():
    seconds = 1.0
    out = silence(seconds)
    place(out, whoosh(0.35, 300, 2400, 0.6, 0.6) * 0.7, 0)                   # it reaches out
    place(out, thump(90, 0.2, 0.7, 0.05) * 0.6, 0.32)                        # and closes
    place(out, whoosh(0.3, 2400, 400, 0.4, 0.6) * 0.5, 0.36)                 # and yanks back
    place(out, chime(1567.98, 0.5, 0.8) * 0.2, 0.34)
    return room(out, 0.9, 0.3, 5000)


def s_star_anchor():
    seconds = 1.6
    out = silence(seconds)
    place(out, thump(58, 0.4, 0.7, 0.12) * 0.7, 0)
    place(out, bell(587.3, 1.4, 0.7, (1.0, 2.0, 2.76, 4.1), 1.0) * 0.45, 0.02)
    place(out, starlight(1.0, 10) * 0.4, 0.1)
    return room(out, 1.4, 0.4, 5000)


def s_anchor_lift():
    seconds = 1.0
    out = silence(seconds)
    place(out, reverse(bell(587.3, 0.7, 0.7, (1.0, 2.0, 2.76), 0.6)) * 0.4, 0)
    place(out, sparkle(0.6, 8, 4000, 9000) * 0.3, 0.3)
    return room(out, 1.0, 0.35, 6000)


def s_wandering():
    seconds = 2.4
    out = silence(seconds)
    place(out, fold(0.8, False) * 0.6, 0)
    n = n_of(2.0)
    air = sweep(noise(2.0), lambda p: 600 + 3000 * p, 0.5) * env_adsr(n, 0.4, 0.2, 0, 1.0, 0.7) * 0.5
    place(out, air, 0.2)
    place(out, choir((261.6, 392.0, 523.3), 1.8, 'ah', 2, 0.6, 1.0) * 0.1, 0.3)
    place(out, starlight(2.0, 26) * 0.5, 0.2)
    return room(out, 2.0, 0.5, 6000)


def s_wandering_end():
    seconds = 1.2
    out = silence(seconds)
    place(out, fold(0.5, True) * 0.6, 0)
    place(out, thump(60, 0.3, 0.7, 0.08) * 0.5, 0.4)
    place(out, chime(1046.5, 0.8, 0.8) * 0.3, 0.42)
    return room(out, 1.1, 0.35, 5000)


# =====================================================================================================
# Secrets Sorcerer: Space Concealment, Transfiguration, Exile, Pocket Space; Wanderer: Space Tearing
# =====================================================================================================

def s_space_concealment():
    seconds = 2.4
    out = silence(seconds)
    place(out, fold(1.0, True) * 0.8, 0)
    n = n_of(1.6)
    hum = (tone(110, 1.6) + tone(164.8, 1.6) * 0.6) * env_adsr(n, 0.3, 0.2, 0, 0.9, 0.6) * 0.3
    place(out, hum, 0.3)
    for i in range(6):                                                       # the shell setting, pane by pane
        place(out, chime(1046.5 * 2 ** (i / 12 * 2), 0.7, 0.6) * 0.15, 0.5 + i * 0.08)
    return room(out, 2.0, 0.45, 4500)


def s_concealment_shut():
    seconds = 1.2
    out = silence(seconds)
    place(out, ghost_door(0.7, 0.9, 0.9), 0)
    place(out, lock_spring(1200) * 0.6, 0.38)
    place(out, thump(48, 0.5, 0.6, 0.18) * 0.7, 0.38)
    return room(out, 1.4, 0.4, 3500)


def s_transfiguration():
    seconds = 1.8
    out = silence(seconds)
    for i in range(5):                                                       # the layers of Doors unfold
        place(out, ghost_door(0.6, 1.0 + i * 0.12, 0.25) * 0.5, i * 0.14)
    place(out, star_chord(659.3, 1.4, (0, 7, 12, 19), 0.2), 0.4)
    return room(out, 1.6, 0.4, 5500)


def s_layer_tear(variant):
    seconds = 0.8
    out = silence(seconds)
    place(out, bandpass(noise(0.15), 800 + variant * 400, 6000) * env(0.15, 0.003, 0.12) * 0.9, 0)
    place(out, crackle(0.3, 18, 1500, 7000) * 0.5, 0.02)
    place(out, whoosh(0.3, 700, 3500, 0.4, 0.5) * 0.5, 0.05)
    place(out, chime(1760 * (1 + variant * 0.12), 0.5, 0.9) * 0.25, 0.1)
    return room(out, 0.8, 0.3, 6000)


def s_exile():
    seconds = 2.0
    out = silence(seconds)
    place(out, ghost_door(0.8, 0.7, 0.6), 0)
    place(out, fold(1.1, True) * 0.8, 0.3)
    n = n_of(1.2)
    storm = sweep(noise(1.2), lambda p: 300 + 900 * np.sin(p * np.pi * 4) ** 2, 0.6) * env_adsr(n, 0.2, 0.2, 0, 0.6, 0.7)
    place(out, storm * 0.5, 0.5)
    return room(out, 1.6, 0.45, 3500)


def s_exile_return():
    seconds = 1.3
    out = silence(seconds)
    place(out, fold(0.5, False) * 0.6, 0)
    place(out, ghost_door(0.6, 0.8, 0.9) * 0.8, 0.3)
    place(out, thump(45, 0.5, 0.6, 0.15) * 0.8, 0.5)
    return room(out, 1.3, 0.4, 3500)


def s_pocket_space():
    seconds = 1.2
    out = silence(seconds)
    place(out, ghost_door(0.7, 1.4, 0.3) * 0.8, 0)
    place(out, fold(0.4, True) * 0.4, 0.25)
    place(out, chime(1318.5, 0.6, 0.8) * 0.25, 0.3)
    return room(out, 1.0, 0.35, 5000)


def s_tearing_warn():
    seconds = 0.9
    out = silence(seconds)
    n = n_of(0.8)
    strain = tone(glide(90, 180, 0.8, 1.5), 0.8) * env_adsr(n, 0.3, 0.1, 0, 0.2, 0.9) * 0.5
    strain += crackle(0.8, 24, 500, 3000) * np.linspace(0.1, 1, n) * 0.4
    place(out, strain, 0)
    return room(out, 0.8, 0.3, 4000)


def s_space_tearing():
    seconds = 2.0
    out = silence(seconds)
    place(out, bandpass(noise(0.3), 400, 8000) * env(0.3, 0.002, 0.25) * 1.4, 0)   # the rip
    place(out, crackle(0.8, 50, 800, 7000) * 0.6, 0.02)
    place(out, thump(36, 0.9, 0.5, 0.35) * 1.0, 0)
    place(out, reverse(fold(0.6, True)) * 0.5, 0.15)
    place(out, sparkle(1.0, 16, 3000, 9000) * 0.3, 0.2)
    return room(out, 2.0, 0.45, 3500)


# =====================================================================================================
# Planeswalker: Toy Box; Key of Stars: Space Fragmentation, Starlight Seal, Space-Time Labyrinth
# =====================================================================================================

def s_toy_box():
    seconds = 2.6
    out = silence(seconds)
    place(out, clockwork(0.5, 6) * 0.6, 0)
    notes = [1046.5, 1318.5, 1568.0, 1318.5, 1174.7, 1046.5, 1568.0, None, 2093.0]
    place(out, music_box(notes, 0.18) * 0.6, 0.35)
    place(out, shimmer(1.6, 3000, 9000) * 0.15, 0.4)
    return room(out, 1.6, 0.4, 6000)


def s_toy_wind():
    seconds = 0.7
    return room(clockwork(0.6, 8, 3000) * 0.9, 0.5, 0.2, 7000)


def s_fragmentation():
    seconds = 3.0
    out = silence(seconds)
    n = n_of(2.6)
    hum = drone(2.6, 55.0, (1, 1.5, 2, 3), 0.6) * env_adsr(n, 0.8, 0.2, 0, 1.0, 0.8) * 0.5
    place(out, hum, 0)
    for i in range(9):                                                       # nine planets, each its own note
        f = 330.0 * 2 ** ((0, 2, 4, 7, 9, 12, 14, 16, 19)[i] / 12)
        place(out, bell(f, 1.6, 0.6, (1.0, 2.0, 2.9, 4.1), 1.4) * 0.15, 0.2 + i * 0.12)
    place(out, sweep(noise(2.0), lambda p: 200 + 600 * p, 0.6) * env(2.0, 0.6, 0.8, 0.6) * 0.3, 0.4)
    return room(out, 2.4, 0.5, 4000)


def s_fragmentation_collapse():
    seconds = 2.6
    out = silence(seconds)
    place(out, reverse(fold(0.6, True)) * 0.6, 0)
    place(out, thump(32, 1.2, 0.5, 0.45) * 1.2, 0.5)
    place(out, lowpass(noise(1.4), 400, 2) * decay(n_of(1.4), 0.4) * 2.0, 0.5)
    for i in range(9):                                                       # the planets shattering
        place(out, chime(RNG.uniform(800, 3000), 0.8, 1.0) * 0.2, 0.5 + RNG.uniform(0, 0.25))
    place(out, crackle(1.2, 60, 600, 6000) * 0.4, 0.52)
    return room(out, 2.4, 0.45, 3500)


def s_starlight_seal():
    seconds = 2.2
    out = silence(seconds)
    for i in range(10):                                                      # drops of starlight falling
        f = RNG.uniform(1800, 4200)
        place(out, chime(f, 0.6, 1.0) * 0.18, i * 0.07)
    place(out, lock_spring(1500) * 0.7, 0.8)
    place(out, bell(392.0, 1.4, 0.7, (1.0, 2.0, 2.76, 3.9), 1.0) * 0.4, 0.82)
    place(out, thump(50, 0.5, 0.6, 0.18) * 0.6, 0.82)
    return room(out, 1.8, 0.45, 5000)


def s_labyrinth():
    seconds = 2.6
    out = silence(seconds)
    for i in range(12):                                                      # walls of starlight rising
        place(out, chime(523.3 * 2 ** (RNG.integers(0, 14) / 12), 0.7, 0.6) * 0.12, i * 0.1)
    n = n_of(2.2)
    place(out, drone(2.2, 73.4, (1, 2, 3), 0.3) * env_adsr(n, 0.5, 0.2, 0, 1.0, 0.6) * 0.35, 0.1)
    place(out, shimmer(2.0, 2000, 7000) * 0.2, 0.3)
    return room(out, 2.4, 0.5, 4500)


def s_labyrinth_loop():
    seconds = 0.9
    out = silence(seconds)
    place(out, fold(0.3, True) * 0.5, 0)
    place(out, fold(0.3, False) * 0.5, 0.25)
    place(out, chime(1318.5, 0.5, 0.8) * 0.2, 0.3)
    return room(out, 0.9, 0.35, 5000)


# =====================================================================================================
# Door: Alternate World, Omnipresence, Close the Door
# =====================================================================================================

def s_alternate_world():
    seconds = 4.0
    out = silence(seconds)
    place(out, thump(36, 1.2, 0.5, 0.45) * 0.9, 0)
    for i in range(8):                                                       # a kingdom of Doors opening
        place(out, ghost_door(0.8, 0.8 + i * 0.07, 0.2) * 0.35, 0.2 + i * 0.16)
    place(out, choir((130.8, 196.0, 261.6, 329.6, 392.0), 3.0, 'ah', 3, 1.0, 1.4) * 0.15, 0.3)
    place(out, starlight(3.0, 40) * 0.5, 0.5)
    return room(out, 3.0, 0.55, 5000)


def s_omnipresence():
    seconds = 2.2
    out = silence(seconds)
    for i in range(7):                                                       # seven of you, one after another
        place(out, s_blink(i % 3) * (0.6 - i * 0.05), i * 0.11)
    place(out, star_chord(587.3, 1.6, (0, 4, 7, 11, 14), 0.2), 0.5)
    return room(out, 1.8, 0.45, 6000)


def s_close_the_door():
    seconds = 3.0
    out = silence(seconds)
    for i in range(6):                                                       # door after door slamming shut
        place(out, thump(50 + i * 4, 0.5, 0.6, 0.15) * (0.9 - i * 0.1), i * 0.12)
        place(out, clank(RNG.uniform(400, 700)) * 0.25, i * 0.12 + 0.01)
    place(out, lock_spring(1000) * 0.8, 0.85)
    place(out, bell(130.8, 2.4, 0.6, (0.5, 1.0, 1.19, 1.5, 2.0, 2.52), 2.2) * 0.5, 0.86)
    return room(out, 2.6, 0.5, 3000)


# =====================================================================================================
# The passives
# =====================================================================================================

def s_intuition():
    seconds = 1.2
    out = silence(seconds)
    place(out, chime(1567.98, 1.0, 0.6) * 0.5, 0)
    place(out, chime(1174.66, 1.0, 0.6) * 0.35, 0.16)
    return room(out, 1.0, 0.4, 6000)


def s_quick_study():
    seconds = 0.8
    out = silence(seconds)
    place(out, tick(4200) * 0.6, 0)
    place(out, chime(3135.96, 0.6, 1.0) * 0.35, 0.02)
    return room(out, 0.7, 0.3, 8000)


def s_key_turn():
    seconds = 1.0
    out = silence(seconds)
    place(out, clockwork(0.25, 3, 1800) * 0.7, 0)
    place(out, lock_spring(1400) * 0.8, 0.28)
    place(out, chime(2093.0, 0.6, 0.9) * 0.25, 0.32)
    return room(out, 0.9, 0.3, 6000)


SOUNDS = {
    'door_open': [s_door_open],
    'door_step': [s_door_step],
    'slip_free': [s_slip_free],
    'flash': [s_flash],
    'black_screen': [s_black_screen],
    'loud_noise': [s_loud_noise],
    'frost_ray': [s_frost_ray],
    'gale': [s_gale],
    'tumble': [s_tumble],
    'escape_trick': [s_escape_trick],
    'divination': [s_divination],
    'starlit': [s_starlit],
    'interference': [s_interference],
    'interference_reflect': [s_interference_reflect],
    'record': [s_record],
    'recorded': [s_recorded],
    'replay': [s_replay],
    'replication': [s_replication],
    'travelers_door': [s_travelers_door],
    'travelers_arrive': [s_travelers_arrive],
    'blink': [lambda: s_blink(0), lambda: s_blink(1), lambda: s_blink(2)],
    'invisible_hand': [s_invisible_hand],
    'star_anchor': [s_star_anchor],
    'anchor_lift': [s_anchor_lift],
    'wandering': [s_wandering],
    'wandering_end': [s_wandering_end],
    'space_concealment': [s_space_concealment],
    'concealment_shut': [s_concealment_shut],
    'transfiguration': [s_transfiguration],
    'layer_tear': [lambda: s_layer_tear(0), lambda: s_layer_tear(1)],
    'exile': [s_exile],
    'exile_return': [s_exile_return],
    'pocket_space': [s_pocket_space],
    'tearing_warn': [s_tearing_warn],
    'space_tearing': [s_space_tearing],
    'toy_box': [s_toy_box],
    'toy_wind': [s_toy_wind],
    'fragmentation': [s_fragmentation],
    'fragmentation_collapse': [s_fragmentation_collapse],
    'starlight_seal': [s_starlight_seal],
    'labyrinth': [s_labyrinth],
    'labyrinth_loop': [s_labyrinth_loop],
    'alternate_world': [s_alternate_world],
    'omnipresence': [s_omnipresence],
    'close_the_door': [s_close_the_door],
    'intuition': [s_intuition],
    'quick_study': [s_quick_study],
    'key_turn': [s_key_turn],
}

SUBTITLES = {
    'door_open': 'An illusory Door opens', 'door_step': 'Someone steps through a Door', 'slip_free': 'A lock springs open',
    'flash': 'A blinding flash', 'black_screen': 'A dark curtain falls', 'loud_noise': 'A deafening bang',
    'frost_ray': 'A ray of frost', 'gale': 'A sudden gale', 'tumble': 'The ground turns slick',
    'escape_trick': 'A trickster slips away', 'divination': 'A crystal ball glows', 'starlit': 'A star marks someone',
    'interference': 'Someone becomes unreadable', 'interference_reflect': 'A search is turned back',
    'record': 'The Scribe watches', 'recorded': 'A power is recorded', 'replay': 'A recorded power is replayed',
    'replication': 'A power is copied', 'travelers_door': 'A Door to the Spirit World opens',
    'travelers_arrive': 'A traveler steps out', 'blink': 'Someone blinks away', 'invisible_hand': 'An unseen hand grabs',
    'star_anchor': 'A Star Anchor is planted', 'anchor_lift': 'A Star Anchor is lifted',
    'wandering': 'Someone enters the Astral World', 'wandering_end': 'Someone steps out of the Astral World',
    'space_concealment': 'Space is split off', 'concealment_shut': 'The Door in space shuts',
    'transfiguration': 'Layers of Doors unfold', 'layer_tear': 'A layer of Doors tears', 'exile': 'Doors swallow someone',
    'exile_return': 'Someone is thrown back out', 'pocket_space': 'A pocket of space opens',
    'tearing_warn': 'Space strains', 'space_tearing': 'Space tears', 'toy_box': 'A Toy Box opens',
    'toy_wind': 'Clockwork winds', 'fragmentation': 'Planets of starlight gather',
    'fragmentation_collapse': 'The planets collapse', 'starlight_seal': 'A Starlight Seal closes',
    'labyrinth': 'A labyrinth of stars rises', 'labyrinth_loop': 'Someone is turned back into the maze',
    'alternate_world': 'The Alternate World unfolds', 'omnipresence': 'Someone is everywhere at once',
    'close_the_door': 'Every Door closes', 'intuition': 'Something is aimed this way',
    'quick_study': 'A power could be recorded', 'key_turn': 'A Key of Stars turns',
}


def main(assets, wav_dir=None):
    folder = os.path.join(assets, 'sounds/key_of_stars')
    os.makedirs(folder, exist_ok=True)
    events = {}
    sounds_json = os.path.join(assets, 'sounds.json')
    if os.path.exists(sounds_json):
        with open(sounds_json) as f:
            events = json.load(f)
    for name, makers in SOUNDS.items():
        files = []
        for i, make in enumerate(makers):
            file = name if len(makers) == 1 else f'{name}_{i + 1}'
            samples = finish(make())
            kit.encode(samples, os.path.join(folder, file + '.ogg'))
            if wav_dir:
                os.makedirs(wav_dir, exist_ok=True)
                kit.write_wav(samples, os.path.join(wav_dir, file + '.wav'))
            files.append({'name': f'circle_of_inevitability:key_of_stars/{file}'})
            print(f'sounds/key_of_stars/{file}.ogg  {len(samples) / SR:.2f} s')
        events['key_of_stars.' + name] = {
            'subtitle': f'subtitles.circle_of_inevitability.key_of_stars.{name}',
            'sounds': files,
        }
    with open(sounds_json, 'w') as f:
        json.dump(events, f, indent=2)
    lang_path = os.path.join(assets, 'lang/en_us.json')                # the subtitles
    if os.path.exists(lang_path):
        with open(lang_path, encoding='utf-8') as f:
            lang = json.load(f)
        for name, text in SUBTITLES.items():
            lang[f'subtitles.circle_of_inevitability.key_of_stars.{name}'] = text
        with open(lang_path, 'w', encoding='utf-8') as f:
            f.write(json.dumps(lang, indent=2, ensure_ascii=False) + '\n')
    return events


if __name__ == '__main__':
    args = sys.argv[1:]
    wav = None
    if '--wav' in args:
        wav = args[args.index('--wav') + 1]
        args = args[:args.index('--wav')]
    main(args[0], wav)
