"""Synthesizes the Knowledge Emperor sounds and writes sounds.json.

python3 tools/model_gen/knowledge_emperor/sounds.py src/main/resources/assets/circle_of_inevitability [--wav dir]

Every sound is built from scratch (sine partials, filtered noise, formant choirs, a synthetic room), so nothing
here is copied from anywhere. The building blocks (tone, bell, choir, room, thump, finish...) are the ones in
tools/model_gen/beauty_goddess/sounds.py, loaded from there; this file adds the scholar's own: whispers, a page's
rustle, a quill's scratch, a music box, the patter of information. Needs numpy, scipy and ffmpeg (with libvorbis).

The .ogg files go to sounds/knowledge_emperor/, mono 44.1 kHz (Minecraft only places mono sounds in the world), and
each one gets an event "knowledge_emperor.<name>" in sounds.json and a subtitle in lang/en_us.json. The same names are
registered in sounds/ModSounds.java (KNOWLEDGE_...): a new sound here needs a line there too.

To change one: edit its function, run this again, press F3+T in game (sounds reload with the resources).
--wav dir also writes .wav copies there, to listen to them outside the game.
"""
import importlib.util
import json
import os
import sys

import numpy as np

_here = os.path.dirname(os.path.abspath(__file__))
_spec = importlib.util.spec_from_file_location('beauty_sounds', os.path.join(_here, '..', 'beauty_goddess', 'sounds.py'))
kit = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(kit)

SR = kit.SR
RNG = np.random.default_rng(1891)
silence, noise, place, env_adsr, decay, fade = kit.silence, kit.noise, kit.place, kit.env_adsr, kit.decay, kit.fade
lowpass, highpass, bandpass, sweep, tone, glide = kit.lowpass, kit.highpass, kit.bandpass, kit.sweep, kit.tone, kit.glide
bell, choir, room, thump, heartbeat, finish = kit.bell, kit.choir, kit.room, kit.thump, kit.heartbeat, kit.finish
sparkle, drone, creak, clank, harp, glass_shards = kit.sparkle, kit.drone, kit.creak, kit.clank, kit.harp, kit.glass_shards


def n_of(seconds):
    return int(seconds * SR)


# =====================================================================================================
# Building blocks of a scholar: whispers, pages, quills, chimes, information
# =====================================================================================================

def whispers(seconds, count=24, lo=1200, hi=4200, start=0.0, end=None, level=1.0):
    """Many voices murmuring at once: short syllables of band-passed breath, each with its own vowel colour."""
    out = silence(seconds)
    end = seconds if end is None else end
    for _ in range(count):
        d = RNG.uniform(0.06, 0.22)
        n = n_of(d)
        c = RNG.uniform(lo, hi)
        syl = bandpass(noise(d), c * 0.7, c * 1.3) * np.sin(np.linspace(0, np.pi, n)) ** 1.5
        syl += bandpass(noise(d), 300, 900) * np.sin(np.linspace(0, np.pi, n)) ** 2 * 0.25   # the throat behind it
        place(out, syl * RNG.uniform(0.3, 1.0) * level, RNG.uniform(start, max(start, end - d)))
    return out


def rustle(seconds, count=10, start=0.0, end=None):
    """Paper: a page turned or a scroll unrolled, short crisp crackles of high noise."""
    out = silence(seconds)
    end = seconds if end is None else end
    for _ in range(count):
        d = RNG.uniform(0.01, 0.06)
        crack = highpass(noise(d), RNG.uniform(2500, 5000)) * decay(n_of(d), d / 2.5)
        place(out, crack * RNG.uniform(0.3, 1.0), RNG.uniform(start, max(start, end - d)))
    swish = bandpass(noise(end - start), 1500, 6000) * np.sin(np.linspace(0, np.pi, n_of(end - start))) * 0.25
    place(out, swish, start)
    return out


def scratch(seconds, strokes=7):
    """A quill writing: short strokes of narrow noise, each sliding a little in pitch, the nib catching."""
    out = silence(seconds)
    t = 0.02
    for _ in range(strokes):
        d = RNG.uniform(0.05, 0.14)
        f0 = RNG.uniform(2500, 4200)
        stroke = sweep(noise(d), lambda p, f0=f0: f0 * (1 + 0.25 * np.sin(p * np.pi)), 0.2)
        stroke *= np.sin(np.linspace(0, np.pi, n_of(d))) ** 0.7
        for k in range(RNG.integers(1, 4)):                                       # the nib catching on the fibres
            place(stroke, highpass(noise(0.004), 4000) * 0.6, RNG.uniform(0, d - 0.005))
        place(out, stroke * RNG.uniform(0.5, 1.0), t)
        t += d + RNG.uniform(0.01, 0.06)
        if t > seconds - 0.15:
            break
    return out


def chime(freq, seconds=1.2, bright=0.8):
    """A small clear chime: near-harmonic partials, the glassy ones dying fast."""
    return bell(freq, seconds, bright, (1.0, 2.0, 3.01, 4.2, 5.4), 1.2)


def music_box(notes, step=0.28, seconds=None):
    """A music box: plucked steel tines (bright, inharmonic, quick to die), one note after another."""
    seconds = seconds or step * len(notes) + 1.4
    out = silence(seconds)
    for i, f in enumerate(notes):
        if f is None:
            continue
        tine = bell(f, 1.3, 1.2, (1.0, 3.9, 9.6, 12.1), 0.9) * 0.6 + tone(f, 1.3) * decay(n_of(1.3), 0.5) * 0.4
        place(out, tine, i * step)
    return out


def patter(seconds, count=60, lo=900, hi=4000, start=0.0, end=None):
    """Information: a rain of tiny clean blips at scattered pitches, like letters being read too fast."""
    out = silence(seconds)
    end = seconds if end is None else end
    for _ in range(count):
        d = RNG.uniform(0.008, 0.03)
        f = RNG.choice([lo, (lo + hi) / 2, hi]) * RNG.uniform(0.9, 1.3)
        blip = tone(f, d) * np.sin(np.linspace(0, np.pi, n_of(d)))
        place(out, blip * RNG.uniform(0.2, 0.8), RNG.uniform(start, max(start, end - d)))
    return out


def whoosh(seconds, lo=300, hi=2600, peak=0.5, width=0.7):
    """Air moved by something fast: a band of noise that sweeps up and back down, loudest at 'peak' (0..1)."""
    n = n_of(seconds)
    x = sweep(noise(seconds), lambda p: lo + (hi - lo) * np.exp(-((p - peak) / 0.25) ** 2), width)
    shape = np.exp(-((np.linspace(0, 1, n) - peak) / 0.28) ** 2)
    return x * shape


def tick(freq=3200):
    d = 0.03
    return tone(freq, d) * decay(n_of(d), 0.004) + highpass(noise(d), 4000) * decay(n_of(d), 0.002) * 0.5


def shimmer(seconds, lo=2500, hi=9000, rise=True):
    """Starlight: a veil of high noise that brightens (or dims) over the sound."""
    n = n_of(seconds)
    return sweep(noise(seconds), (lambda p: lo + (hi - lo) * p) if rise else (lambda p: hi - (hi - lo) * p), 0.4) \
        * np.sin(np.linspace(0, np.pi, n)) ** 1.2


def reverse(x):
    return x[::-1].copy()


def crackle(seconds, count, lo=800, hi=6000):
    out = silence(seconds)
    for _ in range(count):
        d = RNG.uniform(0.003, 0.02)
        place(out, bandpass(noise(d), lo, hi) * decay(n_of(d), d / 3) * RNG.uniform(0.2, 1), RNG.uniform(0, seconds - 0.03))
    return out


# =====================================================================================================
# Mystery Pryer: Mystery Prying, Dowsing; the passives' chime and whisper
# =====================================================================================================

def s_pry():
    seconds = 2.2
    n = n_of(seconds)
    out = silence(seconds)
    place(out, whispers(1.6, 26, 1500, 4500, 0.0, 1.4, 0.6) * env_adsr(n_of(1.6), 0.5, 0.2, 0, 0.6, 0.8), 0)
    place(out, shimmer(1.4, 3000, 10000) * 0.25, 0.15)
    place(out, chime(1318.5, 1.6, 0.6) * 0.35, 0.45)                          # the eye opens
    place(out, chime(1975.5, 1.4, 0.5) * 0.25, 0.6)
    place(out, drone(1.8, 61.7, (1, 1.5, 2), 0.25) * env_adsr(n_of(1.8), 0.4, 0.2, 0, 0.9, 0.6) * 0.25, 0.2)
    return room(out, 1.6, 0.42, 5000)


def s_dowse():
    seconds = 2.4
    out = silence(seconds)
    for i in range(12):                                                       # the fine chain settling
        place(out, chime(RNG.uniform(4200, 7000), 0.25, 1.2) * 0.12, 0.02 + i * 0.025 + RNG.uniform(0, 0.02))
    for i, at in enumerate((0.4, 0.95, 1.45)):                                # swinging: tick ... tock
        place(out, tick(2600 if i % 2 == 0 else 2200) * 0.5, at)
        place(out, whoosh(0.35, 500, 1800, 0.5, 0.5) * 0.12, at - 0.15)
    place(out, bell(1567.98, 1.6, 0.8, (1.0, 2.32, 4.25, 6.63), 1.4) * 0.35, 1.5)   # it finds something
    return room(out, 1.4, 0.38, 6000)


def s_whisper():
    seconds = 2.4
    n = n_of(seconds)
    out = silence(seconds)
    place(out, whispers(2.2, 18, 1100, 3600, 0.1, 2.0, 0.7) * env_adsr(n_of(2.2), 0.6, 0.3, 0, 0.9, 0.7), 0)
    place(out, choir((392.0, 587.33), 1.8, 'oo', 1, 0.8, 0.9, depth=0.002) * 0.08, 0.2)
    place(out, chime(2349.3, 1.0, 0.5) * 0.15, 1.5)
    return room(out, 1.8, 0.5, 4000)


def s_chime():
    seconds = 1.4
    out = silence(seconds)
    place(out, chime(1760.0, 1.2, 0.6) * 0.6, 0)
    place(out, chime(2637.0, 1.0, 0.6) * 0.3, 0.07)
    return room(out, 1.2, 0.4, 7000)


# =====================================================================================================
# Melee Scholar: Weak-Point Strike, Sidestep; the melee combo
# =====================================================================================================

def s_weak_point():
    seconds = 1.0
    out = silence(seconds)
    place(out, whoosh(0.22, 700, 3800, 0.7, 0.5) * 0.6, 0)
    place(out, thump(70, 0.25, 0.6, 0.06) * 0.9, 0.18)
    place(out, bandpass(noise(0.04), 600, 3000) * decay(n_of(0.04), 0.01) * 0.8, 0.18)
    place(out, chime(3135.96, 0.7, 1.0) * 0.3, 0.19)                          # the glassy note of the weak point
    return room(out, 0.6, 0.2, 5000)


def s_sidestep():
    seconds = 0.8
    out = silence(seconds)
    place(out, whoosh(0.3, 900, 5000, 0.4, 0.45) * 0.7, 0)
    place(out, rustle(0.4, 6, 0.0, 0.3) * 0.5, 0.02)
    return room(out, 0.5, 0.2, 6000)


def s_glyph_swing(variant):
    seconds = 0.6
    out = silence(seconds)
    place(out, whoosh(0.35, 600 + 250 * variant, 4000, 0.45, 0.5) * 0.7, 0)
    for i in range(4):                                                        # glyphs tinkling round the sweep
        place(out, chime(RNG.uniform(2800, 5200), 0.3, 1.0) * 0.08, 0.05 + i * 0.05)
    return room(out, 0.4, 0.15, 6000)


# =====================================================================================================
# Warlock: Hex, Spellbolts
# =====================================================================================================

def s_hex():
    seconds = 2.2
    n = n_of(seconds)
    out = silence(seconds)
    place(out, reverse(whispers(0.9, 20, 1200, 3600, 0, 0.9, 0.8)) * np.linspace(0, 1, n_of(0.9)), 0)
    curse = choir((146.83, 155.56, 220.0), 1.6, 'oo', 2, 0.15, 1.0, depth=0.01) * 0.25    # a minor-second rub
    place(out, curse, 0.75)
    place(out, bell(293.66, 1.4, 0.5, (1.0, 1.19, 2.0, 2.4, 3.0), 1.3) * 0.35, 0.8)
    place(out, thump(52, 0.35, 0.6, 0.12) * 0.6, 0.8)
    return room(out, 1.6, 0.4, 3000)


def s_hex_jump():
    seconds = 1.0
    out = silence(seconds)
    place(out, whoosh(0.4, 800, 3500, 0.6, 0.5) * 0.4, 0)
    place(out, whispers(0.4, 8, 1500, 3500, 0, 0.35, 0.6), 0.05)
    place(out, bell(392.0, 0.8, 0.5, (1.0, 1.19, 2.0, 3.0), 0.8) * 0.3, 0.35)
    return room(out, 1.0, 0.35, 3500)


def s_bolt_frost():
    seconds = 1.0
    out = silence(seconds)
    place(out, whoosh(0.3, 1500, 6000, 0.6, 0.4) * 0.5, 0)
    place(out, crackle(0.5, 40, 3000, 9000) * 0.5, 0.2)                      # ice forming
    place(out, chime(2793.8, 0.8, 1.2) * 0.3, 0.25)
    place(out, chime(3729.3, 0.7, 1.2) * 0.2, 0.3)
    return room(out, 0.8, 0.3, 8000)


def s_bolt_lightning():
    seconds = 0.9
    out = silence(seconds)
    for i in range(5):                                                        # the crackle of the arc
        d = RNG.uniform(0.02, 0.06)
        zap = tone(glide(RNG.uniform(1800, 3200), RNG.uniform(300, 600), d, 0.5), d) * decay(n_of(d), d / 2)
        place(out, zap * 0.5 + highpass(noise(d), 2500) * decay(n_of(d), d / 3) * 0.7, i * 0.035)
    place(out, thump(90, 0.2, 0.5, 0.05) * 0.6, 0.15)
    place(out, crackle(0.5, 30, 2000, 8000) * 0.4, 0.15)
    return room(out, 0.6, 0.25, 6000)


def s_bolt_fire():
    seconds = 1.0
    n = n_of(seconds)
    out = silence(seconds)
    roar = sweep(noise(0.6), lambda p: 300 + 1200 * np.sin(p * np.pi), 0.8) * np.sin(np.linspace(0, np.pi, n_of(0.6)))
    place(out, roar * 0.8, 0)
    place(out, crackle(0.7, 50, 900, 5000) * 0.45, 0.15)
    place(out, thump(60, 0.3, 0.6, 0.08) * 0.5, 0.3)
    return room(out, 0.7, 0.25, 3500)


# =====================================================================================================
# Scrolls Professor: the pouch, Read Scroll, Inscribe Scroll, the two free scrolls
# =====================================================================================================

def s_scroll_read():
    seconds = 1.4
    out = silence(seconds)
    place(out, rustle(0.45, 14, 0.0, 0.4) * 0.8, 0)                          # unrolled
    place(out, whoosh(0.5, 500, 4000, 0.65, 0.6) * 0.5, 0.25)                # the spell leaps off the page
    place(out, chime(1567.98, 1.0, 0.8) * 0.3, 0.55)
    place(out, chime(2349.3, 0.9, 0.8) * 0.2, 0.6)
    place(out, crackle(0.5, 30, 1500, 6000) * 0.25, 0.6)                     # the page burns away
    return room(out, 1.0, 0.3, 5000)


def s_scroll_inscribe():
    seconds = 2.0
    out = silence(seconds)
    place(out, scratch(1.3, 9) * 0.7, 0)
    place(out, rustle(0.3, 5, 0, 0.25) * 0.4, 1.3)
    place(out, thump(110, 0.15, 0.7, 0.04) * 0.5, 1.45)                      # the seal pressed
    place(out, chime(1174.66, 1.0, 0.5) * 0.3, 1.47)
    return room(out, 1.0, 0.28, 5000)


def s_scroll_ready():
    seconds = 0.9
    out = silence(seconds)
    place(out, rustle(0.25, 5, 0, 0.2) * 0.5, 0)
    place(out, chime(2093.0, 0.7, 0.6) * 0.3, 0.12)
    return room(out, 0.6, 0.3, 6000)


def s_freeze_ray():
    seconds = 1.6
    n = n_of(seconds)
    out = silence(seconds)
    place(out, shimmer(0.7, 9000, 2500, rise=False) * 0.5, 0)                # it falls
    place(out, crackle(0.8, 70, 3000, 10000) * env_adsr(n_of(0.8), 0.05, 0.2, 0, 0.5, 0.7) * 0.6, 0.55)
    for i, f in enumerate((2637.0, 3135.96, 3951.07)):
        place(out, chime(f, 0.9, 1.2) * 0.18, 0.6 + i * 0.05)
    place(out, thump(80, 0.2, 0.5, 0.05) * 0.4, 0.55)
    return room(out, 1.2, 0.35, 8000)


def s_paralysis_ray():
    seconds = 1.2
    n = n_of(seconds)
    out = silence(seconds)
    t = np.arange(n_of(0.9)) / SR
    buzz = (tone(220, 0.9) + 0.5 * tone(331, 0.9)) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 37 * t)))
    place(out, lowpass(buzz, 2400) * env_adsr(n_of(0.9), 0.05, 0.2, 0, 0.4, 0.7) * 0.35, 0)
    place(out, whoosh(0.35, 1200, 5000, 0.4, 0.4) * 0.4, 0)
    place(out, thump(90, 0.2, 0.5, 0.05) * 0.5, 0.3)
    place(out, tone(glide(1800, 900, 0.4, 0.6), 0.4) * decay(n_of(0.4), 0.15) * 0.2, 0.3)
    return room(out, 0.8, 0.25, 5000)


# =====================================================================================================
# Constellations Master: the Stars wheel
# =====================================================================================================

def s_starlight_gather():
    seconds = 1.6
    n = n_of(seconds)
    out = silence(seconds)
    place(out, sparkle(1.4, 24, 3000, 10000, 0.0, 1.3) * np.linspace(0.3, 1, n_of(1.4)) * 0.4, 0)
    place(out, choir((523.25, 659.25, 783.99), 1.2, 'ee', 2, 0.9, 0.4, depth=0.003) * 0.12, 0.1)
    place(out, shimmer(1.2, 2000, 9000) * 0.25, 0.1)
    return room(out, 1.4, 0.4, 7000)


def s_amber_set():
    seconds = 1.8
    out = silence(seconds)
    for i in range(10):                                                       # crystal growing over the body
        place(out, chime(RNG.uniform(1500, 3500), 0.4, 1.4) * 0.12, i * 0.03)
    place(out, crackle(0.35, 40, 1500, 7000) * 0.5, 0)
    place(out, thump(65, 0.35, 0.6, 0.1) * 0.6, 0.3)
    place(out, bell(659.25, 1.4, 0.4, (1.0, 1.5, 2.0, 2.98), 1.4) * 0.35, 0.3)   # it sets
    return room(out, 1.4, 0.4, 5000)


def s_amber_break():
    seconds = 1.2
    out = silence(seconds)
    place(out, glass_shards(1.0, 26, 0.0, 0.2) * 0.8, 0)
    place(out, thump(80, 0.2, 0.5, 0.05) * 0.5, 0)
    return room(out, 1.0, 0.3, 7000)


def s_stellar_self():
    seconds = 1.6
    out = silence(seconds)
    place(out, sweep(noise(0.6), lambda p: 1500 + 7000 * p, 0.4) * np.linspace(1, 0, n_of(0.6)) * 0.4, 0)   # the body breaks up
    place(out, sparkle(1.0, 30, 4000, 11000, 0.0, 0.9) * 0.35, 0)
    place(out, whoosh(0.6, 1000, 6000, 0.5, 0.5) * 0.3, 0.3)
    place(out, reverse(sparkle(0.5, 14, 4000, 10000)) * 0.4, 0.8)            # and gathers again
    place(out, chime(1975.5, 0.8, 0.6) * 0.3, 1.25)
    return room(out, 1.2, 0.4, 8000)


def s_star_fall():
    seconds = 1.2
    n = n_of(seconds)
    out = silence(seconds)
    place(out, tone(glide(3800, 900, 1.0, 1.4), 1.0) * env_adsr(n_of(1.0), 0.1, 0.2, 0, 0.1, 0.8) * 0.2, 0)   # the whistle
    place(out, sweep(noise(1.0), lambda p: 6000 - 4500 * p, 0.4) * np.linspace(0.2, 1, n_of(1.0)) * 0.3, 0)
    place(out, sparkle(1.0, 10, 4000, 9000) * 0.15, 0)
    return room(out, 0.6, 0.3, 6000)


def s_star_burst():
    seconds = 1.4
    out = silence(seconds)
    place(out, thump(55, 0.4, 0.5, 0.12) * 1.0, 0)
    place(out, bandpass(noise(0.08), 500, 4000) * decay(n_of(0.08), 0.02) * 0.6, 0)
    place(out, bell(1046.5, 1.2, 1.0, (1.0, 2.0, 3.0, 4.2), 1.0) * 0.3, 0)
    place(out, sparkle(1.0, 18, 4000, 10000, 0.0, 0.6) * 0.3, 0.02)
    return room(out, 1.2, 0.35, 6000)


def s_star_guard():
    seconds = 2.2
    out = silence(seconds)
    for i, f in enumerate((1318.5, 1567.98, 1975.5, 2637.0)):                # four stars taking their places
        place(out, chime(f, 1.2, 0.7) * 0.25, 0.1 + i * 0.22)
    place(out, shimmer(1.8, 3000, 9000) * 0.15, 0.1)
    return room(out, 1.6, 0.45, 7000)


def s_star_block():
    seconds = 0.8
    out = silence(seconds)
    place(out, clank(1900) * 0.4, 0)
    place(out, chime(2793.8, 0.6, 1.2) * 0.3, 0)
    place(out, sparkle(0.4, 6, 5000, 10000) * 0.2, 0)
    return room(out, 0.6, 0.25, 8000)


# =====================================================================================================
# Mysticologist: the Retellings
# =====================================================================================================

def s_tale():
    seconds = 2.4
    n = n_of(seconds)
    out = silence(seconds)
    place(out, rustle(0.6, 16, 0, 0.55) * 0.6, 0)                            # the book falls open
    place(out, thump(90, 0.2, 0.7, 0.05) * 0.35, 0.05)
    place(out, choir((196.0, 246.94, 293.66), 1.8, 'ah', 2, 0.6, 1.0, depth=0.004) * 0.16, 0.4)   # once upon a time
    place(out, harp(392.0, 1.2) * 0.25, 0.5)
    place(out, harp(493.88, 1.2) * 0.2, 0.62)
    place(out, harp(587.33, 1.2) * 0.2, 0.74)
    return room(out, 1.8, 0.4, 5000)


def s_sleeping_beauty():
    # a lullaby on a music box (G major, slowing), under a hush of voices
    notes = (783.99, 659.25, 587.33, 659.25, 783.99, None, 659.25, 587.33, 523.25)
    seconds = 3.6
    out = silence(seconds)
    place(out, music_box(notes, 0.3, 3.4) * 0.5, 0)
    place(out, choir((261.63, 392.0), 3.0, 'oo', 2, 1.0, 1.4, depth=0.003) * 0.1, 0.2)
    hush = highpass(noise(2.4), 2500) * env_adsr(n_of(2.4), 1.0, 0.3, 0, 1.2, 0.5) * 0.06
    place(out, hush, 0.8)
    return room(out, 2.0, 0.45, 5000)


def s_awaken():
    seconds = 0.9
    out = silence(seconds)
    place(out, tone(glide(392.0, 784.0, 0.18, 0.5), 0.18) * decay(n_of(0.18), 0.12) * 0.3, 0)
    place(out, chime(1567.98, 0.7, 0.8) * 0.35, 0.12)
    place(out, thump(100, 0.15, 0.6, 0.04) * 0.4, 0.1)
    return room(out, 0.6, 0.3, 6000)


def s_avalon():
    seconds = 3.6
    n = n_of(seconds)
    out = silence(seconds)
    mist = sweep(noise(seconds), lambda p: 500 + 300 * np.sin(p * 6), 0.7) * env_adsr(n, 1.2, 0.3, 0, 1.4, 0.7)
    place(out, mist * 0.35, 0)
    for i, f in enumerate((293.66, 369.99, 440.0, 587.33, 739.99, 880.0)):  # a harp arpeggio over the water
        place(out, harp(f, 1.6) * 0.22, 0.4 + i * 0.13)
    place(out, choir((293.66, 440.0, 587.33), 2.6, 'ah', 2, 1.2, 1.4, depth=0.004) * 0.12, 0.6)
    place(out, sparkle(2.0, 10, 3000, 7000, 1.0, 3.0) * 0.1, 0)
    return room(out, 2.4, 0.5, 4500)


def s_achilles():
    seconds = 1.8
    out = silence(seconds)
    place(out, whoosh(0.25, 1500, 6000, 0.7, 0.4) * 0.5, 0)                 # the arrow
    place(out, thump(110, 0.18, 0.5, 0.04) * 0.6, 0.2)
    place(out, highpass(noise(0.03), 3000) * decay(n_of(0.03), 0.008) * 0.6, 0.2)
    place(out, heartbeat(0.9, 48) * 0.7, 0.45)                               # the weak spot, laid bare
    place(out, bell(493.88, 1.0, 0.5, (1.0, 2.0, 2.4, 3.0), 1.0) * 0.2, 0.45)
    return room(out, 1.2, 0.35, 4000)


def s_pandora():
    seconds = 3.6
    n = n_of(seconds)
    out = silence(seconds)
    place(out, creak(0.6, 18, 45) * 0.45, 0)                                 # the lid
    rise = whispers(1.6, 50, 900, 4500, 0, 1.5, 1.0) * np.linspace(0.2, 1.0, n_of(1.6)) ** 1.5
    place(out, rise * 0.6, 0.45)                                             # every misfortune at once
    place(out, choir((116.54, 123.47, 174.61), 1.4, 'ah', 2, 0.6, 0.4, depth=0.02) * 0.2, 0.6)
    place(out, thump(48, 0.4, 0.6, 0.15) * 0.5, 2.0)
    for i, f in enumerate((1046.5, 1318.5, 1567.98)):                        # and the last thing out: hope
        place(out, chime(f, 1.4, 0.4) * 0.3, 2.15 + i * 0.12)
    return room(out, 2.0, 0.45, 4500)


def s_foresight():
    seconds = 2.2
    out = silence(seconds)
    for i, f in enumerate((783.99, 1174.66)):                                 # bells heard before they ring
        place(out, reverse(bell(f, 1.0, 0.5, (1.0, 2.0, 3.0, 4.1), 1.0)) * 0.3, i * 0.15)
    place(out, shimmer(1.2, 2000, 8000) * 0.25, 0.3)
    place(out, chime(1567.98, 1.0, 0.6) * 0.3, 1.05)
    return room(out, 1.6, 0.45, 6000)


def s_dodge():
    seconds = 0.6
    out = silence(seconds)
    place(out, whoosh(0.25, 1200, 6000, 0.35, 0.4) * 0.6, 0)
    place(out, chime(2637.0, 0.4, 0.8) * 0.15, 0.05)
    return room(out, 0.4, 0.2, 7000)


def s_prophecy():
    seconds = 3.0
    n = n_of(seconds)
    out = silence(seconds)
    place(out, choir((65.41, 98.0, 103.83), 2.4, 'oh', 3, 0.3, 1.4, depth=0.01) * 0.25, 0)   # the fate is spoken
    place(out, bell(130.81, 2.6, 0.4, decay_s=2.4) * 0.5, 0.15)                               # and tolls
    place(out, whispers(1.4, 20, 800, 2600, 0, 1.3, 0.5), 0.1)
    place(out, drone(2.4, 32.7, (1, 1.06, 2), 0.3) * env_adsr(n_of(2.4), 0.4, 0.3, 0, 1.0, 0.6) * 0.3, 0.1)
    return room(out, 2.4, 0.5, 2800)


def s_prophecy_tick():
    seconds = 1.2
    out = silence(seconds)
    place(out, bell(196.0, 1.1, 0.3, (1.0, 2.0, 2.4, 3.0), 1.0) * 0.5, 0)
    place(out, thump(50, 0.3, 0.6, 0.1) * 0.4, 0)
    return room(out, 1.4, 0.45, 2500)


def s_ruin():
    seconds = 3.0
    out = silence(seconds)
    place(out, thump(36, 0.9, 0.5, 0.3) * 1.3, 0)
    place(out, bandpass(noise(0.15), 200, 2000) * decay(n_of(0.15), 0.05) * 0.8, 0)
    place(out, bell(98.0, 2.6, 0.5, decay_s=2.2) * 0.6, 0)
    place(out, choir((49.0, 73.42, 98.0), 1.6, 'ah', 2, 0.02, 1.4, depth=0.01) * 0.25, 0)
    place(out, glass_shards(1.2, 14, 0.05, 0.3) * 0.3, 0)
    return room(out, 2.4, 0.45, 2500)


# =====================================================================================================
# Sage, Knowledge Emperor, Hermit: the Knowledge wheel and the passives
# =====================================================================================================

def s_informatize():
    seconds = 1.8
    n = n_of(seconds)
    out = silence(seconds)
    place(out, patter(1.4, 140, 700, 4200, 0, 1.3) * np.linspace(1, 0.3, n_of(1.4)) * 0.6, 0)   # the body becomes text
    place(out, sweep(noise(1.0), lambda p: 4000 * (1 - p) + 400, 0.5) * decay(n_of(1.0), 0.4) * 0.4, 0)
    place(out, whispers(1.2, 16, 1500, 4000, 0.1, 1.1, 0.5), 0.1)
    place(out, tone(glide(880, 220, 0.8, 0.6), 0.8) * env_adsr(n_of(0.8), 0.05, 0.2, 0, 0.5, 0.6) * 0.12, 0)
    return room(out, 1.2, 0.4, 5000)


def s_reform():
    seconds = 1.4
    out = silence(seconds)
    place(out, reverse(patter(0.9, 90, 700, 4200)) * np.linspace(0.2, 1, n_of(0.9)) * 0.6, 0)
    place(out, thump(80, 0.25, 0.6, 0.06) * 0.6, 0.9)
    place(out, chime(1318.5, 0.8, 0.6) * 0.3, 0.9)
    return room(out, 1.0, 0.3, 6000)


def s_decoy_burst():
    seconds = 1.2
    out = silence(seconds)
    place(out, thump(60, 0.35, 0.5, 0.1) * 0.8, 0)
    place(out, patter(0.6, 80, 900, 5000, 0, 0.5) * 0.6, 0)
    place(out, rustle(0.7, 20, 0, 0.6) * 0.6, 0.02)                          # torn into pages of text
    return room(out, 1.0, 0.3, 5000)


def s_erase():
    seconds = 1.4
    n = n_of(seconds)
    out = silence(seconds)
    sweep_down = sweep(noise(0.7), lambda p: 7000 * (1 - p) ** 2 + 200, 0.4) * np.linspace(0.3, 1, n_of(0.7))
    place(out, sweep_down * 0.6, 0)
    place(out, patter(0.6, 60, 800, 3500) * np.linspace(1, 0, n_of(0.6)) * 0.5, 0.05)
    place(out, highpass(noise(0.006), 2000) * 0.9, 0.7)                      # a cut: then nothing
    place(out, tone(110, 0.08) * decay(n_of(0.08), 0.02) * 0.4, 0.7)
    out[n_of(0.71):] *= 0.0
    return room(out, 0.9, 0.18, 3000)


def s_overload():
    seconds = 2.6
    n = n_of(seconds)
    out = silence(seconds)
    ramp = np.linspace(0.1, 1.0, n_of(1.8)) ** 2
    place(out, whispers(1.8, 120, 900, 5000, 0, 1.8, 1.0) * ramp * 0.8, 0)   # a flood of useless knowledge
    place(out, patter(1.8, 220, 600, 5000, 0, 1.8) * ramp * 0.5, 0)
    place(out, sweep(noise(1.8), lambda p: 300 + 6000 * p ** 2, 0.5) * ramp * 0.3, 0)
    place(out, thump(48, 0.5, 0.5, 0.15) * 1.0, 1.8)                         # thought stops
    place(out, bell(146.83, 1.4, 0.5, (1.0, 1.06, 2.0, 2.98), 1.2) * 0.4, 1.8)
    return room(out, 1.6, 0.4, 3500)


def s_mind_burst():
    seconds = 2.0
    out = silence(seconds)
    place(out, thump(42, 0.7, 0.5, 0.2) * 1.2, 0)
    place(out, glass_shards(1.0, 30, 0.0, 0.25) * 0.5, 0)
    place(out, choir((123.47, 130.81, 185.0), 0.9, 'ah', 2, 0.01, 0.8, depth=0.02) * 0.3, 0)
    place(out, rustle(0.9, 26, 0.05, 0.8) * 0.5, 0.05)                       # knowledge scattered like pages
    return room(out, 1.6, 0.4, 4000)


def s_lance_form():
    seconds = 1.6
    n = n_of(seconds)
    out = silence(seconds)
    hum = (tone(glide(110, 440, 1.0, 0.6), 1.0) + 0.5 * tone(glide(220, 880, 1.0, 0.6), 1.0)) * np.linspace(0.1, 1, n_of(1.0))
    place(out, hum * 0.2, 0)
    place(out, whispers(1.0, 30, 2000, 5000, 0, 1.0, 0.6) * np.linspace(0.2, 1, n_of(1.0)), 0)
    place(out, bell(1760.0, 1.0, 1.2, (1.0, 2.76, 5.4, 8.93), 0.9) * 0.35, 0.95)   # condensed: the spear rings
    place(out, whoosh(0.5, 600, 5000, 0.4, 0.5) * 0.5, 1.0)                        # and is thrown
    return room(out, 1.2, 0.35, 6000)


def s_lance_impact():
    seconds = 2.4
    out = silence(seconds)
    place(out, thump(40, 0.8, 0.45, 0.25) * 1.3, 0)
    place(out, bandpass(noise(0.2), 300, 4000) * decay(n_of(0.2), 0.06) * 0.8, 0)
    place(out, bell(880.0, 1.8, 1.0, (1.0, 2.0, 3.0, 4.07), 1.6) * 0.35, 0)
    place(out, whispers(1.4, 50, 1000, 4500, 0.05, 1.3, 0.8) * np.linspace(1, 0, n_of(1.4)), 0.05)  # forced into minds
    place(out, sparkle(1.2, 20, 4000, 10000, 0.0, 0.8) * 0.25, 0)
    return room(out, 1.8, 0.4, 4500)


def s_condense():
    seconds = 2.2
    n = n_of(seconds)
    out = silence(seconds)
    suck = sweep(noise(1.0), lambda p: 6000 * (0.1 ** p) + 300, 0.5) * np.linspace(0.2, 1, n_of(1.0)) ** 2
    place(out, suck * 0.5, 0)                                                 # drawn in from all around
    place(out, reverse(sparkle(1.0, 24, 3000, 9000)) * 0.3, 0)
    warm = choir((261.63, 329.63, 392.0, 523.25), 1.2, 'oo', 2, 0.1, 0.9, depth=0.003) * 0.18
    place(out, warm, 0.95)                                                    # a ball of light, warm in the hands
    place(out, bell(523.25, 1.2, 0.6, (1.0, 2.0, 3.0, 4.2), 1.2) * 0.3, 0.95)
    return room(out, 1.6, 0.4, 5000)


def s_embodiment():
    seconds = 2.2
    n = n_of(seconds)
    out = silence(seconds)
    breath = sweep(noise(seconds), lambda p: 2500 * (1 - p) + 300, 0.6) * env_adsr(n, 0.2, 0.3, 0, 1.4, 0.6)
    place(out, breath * 0.4, 0)
    place(out, patter(1.2, 50, 800, 3000) * np.linspace(1, 0, n_of(1.2)) * 0.35, 0.1)
    place(out, tone(glide(1320, 2640, 1.6, 0.8), 1.6) * env_adsr(n_of(1.6), 0.1, 0.3, 0, 1.2, 0.5) * 0.06, 0.2)  # gone
    return room(out, 1.6, 0.5, 3000)


def s_indoctrination():
    seconds = 2.6
    n = n_of(seconds)
    out = silence(seconds)
    place(out, thump(44, 0.6, 0.5, 0.2) * 1.0, 0)                            # the pulse
    place(out, sweep(noise(1.2), lambda p: 300 + 3000 * p, 0.6) * decay(n_of(1.2), 0.5) * 0.4, 0)
    chant = choir((110.0, 164.81, 220.0), 2.0, 'oh', 3, 0.05, 1.4, depth=0.004)
    am = 0.6 + 0.4 * np.abs(np.sin(2 * np.pi * 2.5 * np.arange(len(chant)) / SR))    # voices reciting in time
    place(out, chant * am * 0.25, 0.05)
    place(out, whispers(1.6, 40, 1200, 4000, 0.1, 1.6, 0.6), 0.05)
    return room(out, 2.0, 0.45, 4000)


def s_kingdom():
    seconds = 5.0
    n = n_of(seconds)
    out = silence(seconds)
    place(out, choir((65.41, 98.0, 130.81, 196.0, 261.63, 329.63), 4.6, 'oo', 2, 1.8, 1.8, depth=0.005) * 0.2, 0)
    for i, f in enumerate((261.63, 329.63, 392.0, 523.25)):                  # great bells of the library
        place(out, bell(f, 3.0, 0.5, decay_s=2.6) * 0.25, 0.7 + i * 0.55)
    for k in range(6):                                                        # countless pages turning far away
        place(out, rustle(0.5, 10, 0, 0.45) * 0.25, 0.5 + k * 0.6 + RNG.uniform(0, 0.2))
    place(out, whispers(seconds, 60, 1000, 4000, 0.5, 4.5, 0.3), 0)
    place(out, drone(seconds, 32.7, (1, 1.5, 2, 3), 0.2) * env_adsr(n, 1.8, 0.4, 0, 1.6, 0.6) * 0.3, 0)
    place(out, sparkle(seconds, 30, 4000, 10000, 1.0, 4.5) * 0.1, 0)
    return room(out, 3.4, 0.55, 3500)


def s_law_of_three():
    seconds = 1.6
    out = silence(seconds)
    for i, f in enumerate((783.99, 987.77, 1174.66)):                         # three rising notes
        place(out, chime(f, 1.0, 0.6) * (0.3 + 0.08 * i), i * 0.12)
    place(out, shimmer(0.8, 3000, 9000) * 0.15, 0.3)
    return room(out, 1.2, 0.4, 7000)


# =====================================================================================================
# The list: event name -> the functions that make its variants
# =====================================================================================================

SOUNDS = {
    'pry': [s_pry],
    'dowse': [s_dowse],
    'whisper': [s_whisper],
    'chime': [s_chime],
    'weak_point': [s_weak_point],
    'sidestep': [s_sidestep],
    'glyph_swing': [lambda: s_glyph_swing(0), lambda: s_glyph_swing(1)],
    'hex': [s_hex],
    'hex_jump': [s_hex_jump],
    'bolt_frost': [s_bolt_frost],
    'bolt_lightning': [s_bolt_lightning],
    'bolt_fire': [s_bolt_fire],
    'scroll_read': [s_scroll_read],
    'scroll_inscribe': [s_scroll_inscribe],
    'scroll_ready': [s_scroll_ready],
    'freeze_ray': [s_freeze_ray],
    'paralysis_ray': [s_paralysis_ray],
    'starlight_gather': [s_starlight_gather],
    'amber_set': [s_amber_set],
    'amber_break': [s_amber_break],
    'stellar_self': [s_stellar_self],
    'star_fall': [s_star_fall],
    'star_burst': [s_star_burst],
    'star_guard': [s_star_guard],
    'star_block': [s_star_block],
    'tale': [s_tale],
    'sleeping_beauty': [s_sleeping_beauty],
    'awaken': [s_awaken],
    'avalon': [s_avalon],
    'achilles': [s_achilles],
    'pandora': [s_pandora],
    'foresight': [s_foresight],
    'dodge': [s_dodge],
    'prophecy': [s_prophecy],
    'prophecy_tick': [s_prophecy_tick],
    'ruin': [s_ruin],
    'informatize': [s_informatize],
    'reform': [s_reform],
    'decoy_burst': [s_decoy_burst],
    'erase': [s_erase],
    'overload': [s_overload],
    'mind_burst': [s_mind_burst],
    'lance_form': [s_lance_form],
    'lance_impact': [s_lance_impact],
    'condense': [s_condense],
    'embodiment': [s_embodiment],
    'indoctrination': [s_indoctrination],
    'kingdom': [s_kingdom],
    'law_of_three': [s_law_of_three],
}

SUBTITLES = {
    'pry': 'Hidden things are pried open', 'dowse': 'A pendulum swings', 'whisper': 'The Hidden Sage whispers',
    'chime': 'Something stirs out of sight', 'weak_point': 'A weak point is struck', 'sidestep': 'Someone slips aside',
    'glyph_swing': 'Glyphs sweep the air', 'hex': 'A hex is laid', 'hex_jump': 'A hex passes on',
    'bolt_frost': 'A frost bolt flies', 'bolt_lightning': 'A lightning bolt crackles', 'bolt_fire': 'A fire bolt roars',
    'scroll_read': 'A scroll is read', 'scroll_inscribe': 'A scroll is inscribed', 'scroll_ready': 'A scroll is ready',
    'freeze_ray': 'A freezing ray falls', 'paralysis_ray': 'A paralysing ray strikes',
    'starlight_gather': 'Starlight gathers', 'amber_set': 'Amber sets', 'amber_break': 'Amber shatters',
    'stellar_self': 'A body flows as starlight', 'star_fall': 'A star falls', 'star_burst': 'A star bursts',
    'star_guard': 'Stars take their places', 'star_block': 'A star turns a shot aside', 'tale': 'A tale is told',
    'sleeping_beauty': 'A lullaby plays', 'awaken': 'A sleeper wakes', 'avalon': 'Avalon rises from the mist',
    'achilles': 'A weak spot is laid bare', 'pandora': 'Pandora\'s box opens', 'foresight': 'The future is glimpsed',
    'dodge': 'A blow is foreseen and dodged', 'prophecy': 'A fate is pronounced', 'prophecy_tick': 'Fate draws nearer',
    'ruin': 'The prophecy comes true', 'informatize': 'A body becomes information',
    'reform': 'Information takes shape again', 'decoy_burst': 'A false copy bursts into text', 'erase': 'Something is erased',
    'overload': 'Knowledge floods a mind', 'mind_burst': 'A mind bursts', 'lance_form': 'A Lance of Knowledge forms',
    'lance_impact': 'A Lance of Knowledge strikes', 'condense': 'Knowledge is condensed',
    'embodiment': 'Someone vanishes into knowledge', 'indoctrination': 'A pulse of knowledge',
    'kingdom': 'The Knowledge Moor opens', 'law_of_three': 'The Law of Three',
}


def main(assets, wav_dir=None):
    folder = os.path.join(assets, 'sounds/knowledge_emperor')
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
            files.append({'name': f'circle_of_inevitability:knowledge_emperor/{file}'})
            print(f'sounds/knowledge_emperor/{file}.ogg  {len(samples) / SR:.2f} s')
        events['knowledge_emperor.' + name] = {
            'subtitle': f'subtitles.circle_of_inevitability.knowledge_emperor.{name}',
            'sounds': files,
        }
    with open(sounds_json, 'w') as f:
        json.dump(events, f, indent=2)
    lang_path = os.path.join(assets, 'lang/en_us.json')                # the subtitles
    if os.path.exists(lang_path):
        with open(lang_path, encoding='utf-8') as f:
            lang = json.load(f)
        for name, text in SUBTITLES.items():
            lang[f'subtitles.circle_of_inevitability.knowledge_emperor.{name}'] = text
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
