"""Synthesizes the Prince Of Abolition sounds and writes sounds.json.

python3 tools/model_gen/prince_of_abolition/sounds.py src/main/resources/assets/circle_of_inevitability [--wav dir]

Every sound is built from scratch (sine partials, filtered noise, formant voices, a synthetic room), so nothing here
is copied from anywhere. The building blocks are the ones in tools/model_gen/beauty_goddess/sounds.py and the whispers,
voices and whooshes of ../knight_of_misfortune/sounds.py, loaded from there. Needs numpy, scipy and ffmpeg (with
libvorbis).

The .ogg files go to sounds/prince_of_abolition/, mono 44.1 kHz (Minecraft only places mono sounds in the world),
and each one gets an event "prince_of_abolition.<name>" in sounds.json and a subtitle in lang/en_us.json. The same
names are registered in sounds/ModSounds.java (ABOLITION_...): a new sound here needs a line there too.

The sound of the Pathway: a court and a throne room. Gavels on iron, coins, low brass and a choir in a stone hall for
Majesty; for Disorder and Entropy, tones that slip out of tune and time; for Abolition, the world's sound drained away.

To change one: edit its function, run this again, press F3+T in game (sounds reload with the resources).
--wav dir also writes .wav copies there, to listen to them outside the game.
"""
import importlib.util
import json
import os
import sys

import numpy as np

_here = os.path.dirname(os.path.abspath(__file__))


def _load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


kit = _load('beauty_sounds', os.path.join(_here, '..', 'beauty_goddess', 'sounds.py'))
night = _load('knight_sounds', os.path.join(_here, '..', 'knight_of_misfortune', 'sounds.py'))

SR = kit.SR
RNG = np.random.default_rng(1919)
silence, noise, place, env_adsr, decay, fade = kit.silence, kit.noise, kit.place, kit.env_adsr, kit.decay, kit.fade
lowpass, highpass, bandpass, sweep, tone, glide = kit.lowpass, kit.highpass, kit.bandpass, kit.sweep, kit.tone, kit.glide
bell, choir, room, thump, heartbeat, finish = kit.bell, kit.choir, kit.room, kit.thump, kit.heartbeat, kit.finish
sparkle, drone, creak, clank, harp, voice = kit.sparkle, kit.drone, kit.creak, kit.clank, kit.harp, kit.voice
whisper, sung, whoosh, wind, sub, tick = night.whisper, night.sung, night.whoosh, night.wind, night.sub, night.tick


def n_of(seconds):
    return int(seconds * SR)


def t_of(seconds):
    return np.arange(n_of(seconds)) / SR


# =====================================================================================================
# Building blocks of the court: a gavel, coins, brass, stone, a voice that gives orders
# =====================================================================================================

def gavel_strike(weight=1.0):
    """Hardwood struck on iron: a sharp crack, a dull block ring, a low thud under it."""
    d = 0.6
    n = n_of(d)
    out = np.zeros(n)
    for r, a in ((1.0, 1.0), (2.41, 0.5), (3.9, 0.3)):                      # the block's body
        out += a * tone(560 * r / weight, d) * decay(n, 0.05 / r)
    place(out, highpass(noise(0.012), 2500) * decay(n_of(0.012), 0.003) * 1.4, 0)
    place(out, thump(70 / weight, 0.5, 0.5, 0.12) * 1.2 * weight, 0)
    return out


def coin_clink(freq=None):
    """One coin on another: a bright, short, inharmonic ring."""
    f = freq or RNG.uniform(3800, 5200)
    return bell(f, 0.35, 1.2, (1.0, 2.76, 5.4, 8.9), 0.25) * 0.5


def coins(seconds, count, start=0.0, spread=0.3):
    out = silence(seconds)
    for _ in range(count):
        place(out, coin_clink() * RNG.uniform(0.3, 1.0), start + abs(RNG.normal(0, spread)))
    return out


def brass(freqs, seconds, attack=0.15, release=0.6, bright=2600):
    """A low brass chord: saw-like partials through a closing filter, swelling in."""
    n = n_of(seconds)
    tt = t_of(seconds)
    out = np.zeros(n)
    for f in freqs:
        for k in range(1, 14):
            out += (1 / k) * np.sin(2 * np.pi * f * k * tt * (1 + RNG.uniform(-0.001, 0.001)) + RNG.uniform(0, 6.28))
    return lowpass(out, bright, 2) * env_adsr(n, attack, 0.2, 0, release, 0.85)


def grind(seconds, lo=40, hi=110):
    """Stone dragged on stone: a rough low rumble with grains of grit in it."""
    n = n_of(seconds)
    out = lowpass(noise(seconds), 160, 2) * 3.0 * (0.7 + 0.3 * np.sin(2 * np.pi * 7 * t_of(seconds)))
    out += creak(seconds, lo, hi) * 0.5
    for _ in range(int(seconds * 40)):
        place(out, bandpass(noise(0.01), 800, 3000) * decay(n_of(0.01), 0.003) * RNG.uniform(0.1, 0.4),
              RNG.uniform(0, seconds))
    return out * env_adsr(n, 0.15, 0.1, 0, 0.3, 0.9)


def crumble(seconds, start=0.0):
    """Stone breaking up and falling: cracks, then a patter of pieces."""
    out = silence(seconds)
    for i in range(6):
        place(out, highpass(noise(0.03), 1200) * decay(n_of(0.03), 0.008) * RNG.uniform(0.5, 1.0), start + i * 0.05)
    for _ in range(30):
        place(out, bandpass(noise(0.04), 300, 2000) * decay(n_of(0.04), 0.012) * RNG.uniform(0.2, 0.7),
              start + 0.2 + abs(RNG.normal(0, 0.35)))
    place(out, thump(60, 0.5, 0.6, 0.15) * 0.8, start + 0.1)
    return out


def command(word_freq=110.0, seconds=0.9, vowel='oh'):
    """One word spoken with authority: a low voice, a hard start, falling at the end."""
    n = n_of(seconds)
    v = sung(word_freq * 1.15, word_freq * 0.85, seconds, vowel, 4.0, 0.004, 1.5)
    v += sung(word_freq * 2.3, word_freq * 1.7, seconds, 'ah', 4.0, 0.004, 1.5) * 0.25
    return v * env_adsr(n, 0.02, 0.1, 0, seconds * 0.4, 0.8)


def warble(x, rate=6.0, depth=0.004):
    """Pitch made unsteady (a tape pulled): the sound read at a wobbling speed."""
    n = len(x)
    t = np.arange(n) / SR
    pos = np.arange(n) + depth * SR * np.sin(2 * np.pi * rate * t)
    pos = np.clip(pos, 0, n - 1)
    return np.interp(pos, np.arange(n), x)


def reverse(x):
    return x[::-1].copy()


def muffle(x, start, end, low=180):
    """The world's sound drained: from start to end (seconds) it falls into a low murmur."""
    out = x.copy()
    a, b = n_of(start), n_of(end)
    dulled = lowpass(x, low, 2) * 0.6
    ramp = np.zeros(len(x))
    ramp[a:b] = np.linspace(0, 1, max(1, b - a))
    ramp[b:] = 1
    return out * (1 - ramp) + dulled * ramp


# Notes of the court: a low minor key with a bright major top for the crown (C minor, E flat major)
C2, G2, C3, EB3, G3, BB3, C4, EB4, G4, BB4, C5, EB5, G5 = (65.41, 98.0, 130.81, 155.56, 196.0, 233.08, 261.63,
                                                           311.13, 392.0, 466.16, 523.25, 622.25, 783.99)


# =====================================================================================================
# Lawyer: Verdict, Brainwash
# =====================================================================================================

def s_verdict():
    seconds = 2.0
    out = silence(seconds)
    place(out, whoosh(0.4, 200, 1400, 0.85, 0.7) * 0.6, 0)                   # it falls...
    place(out, gavel_strike(1.4) * 1.0, 0.36)                                # ...strikes once
    place(out, bell(C3, 1.4, 0.4, (1.0, 2.0, 3.0), 1.2) * 0.25, 0.38)
    place(out, choir((C3, G3), 1.2, 'oh', 2, 0.05, 0.9) * 0.08, 0.38)
    return room(out, 2.0, 0.45, 2600)                                         # in a court's hall


def s_brainwash():
    seconds = 1.8
    out = silence(seconds)
    place(out, whisper(1.4, 8, 700, 3000, seed=3) * 0.7, 0)
    place(out, warble(tone(glide(G4, EB4, 1.4), 1.4) * env_adsr(n_of(1.4), 0.4, 0.2, 0, 0.6, 0.6), 4.5, 0.01) * 0.12, 0.1)
    place(out, sparkle(0.8, 6, 2500, 5000) * 0.15, 0.6)
    return room(out, 1.6, 0.4, 3000)


# =====================================================================================================
# Barbarian: Brute Force, Iron Will
# =====================================================================================================

def s_brute_force():
    seconds = 1.0
    out = silence(seconds)
    place(out, whoosh(0.22, 150, 900, 0.8, 0.8) * 0.6, 0)
    place(out, thump(48, 0.6, 0.5, 0.18) * 1.6, 0.18)
    place(out, lowpass(noise(0.25), 900) * decay(n_of(0.25), 0.05) * 1.2, 0.18)
    place(out, clank(260) * 0.5, 0.19)                                         # the shield giving way
    place(out, sub(0.6, 38, 0.02, 0.4) * 0.6, 0.18)
    return room(out, 0.9, 0.25, 2200)


def s_iron_will():
    seconds = 1.6
    n = n_of(seconds)
    out = brass((C3, G3), seconds, 0.3, 0.8, 1800) * 0.4
    out += tone(glide(C2, C3, seconds, 0.4), seconds) * env_adsr(n, 0.4, 0.2, 0, 0.6, 0.6) * 0.3
    place(out, clank(620) * 0.5, 0.05)
    place(out, clank(410) * 0.4, 0.12)
    return room(out, 1.4, 0.3, 2400)


# =====================================================================================================
# Briber: the Bribes and Coin Shot
# =====================================================================================================

def s_bribe(variant):
    seconds = 1.0
    out = coins(seconds, 5 + variant * 3, 0.0, 0.18)
    place(out, whisper(0.5, 3, 900, 2800, seed=11 + variant) * 0.35, 0.15)
    return room(out, 0.8, 0.3, 5000)


def s_weaken():
    seconds = 1.4
    n = n_of(seconds)
    out = coins(seconds, 4, 0.0, 0.08)
    out += tone(glide(G3, C3, seconds, 0.6), seconds) * env_adsr(n, 0.05, 0.2, 0, 0.8, 0.6) * 0.3   # sinking
    out += sweep(noise(seconds), lambda p: 2000 * (0.15 ** p), 0.6) * env_adsr(n, 0.05, 0.2, 0, 0.8, 0.5) * 0.25
    return room(out, 1.2, 0.3, 3000)


def s_charm():
    seconds = 1.6
    out = coins(seconds, 3, 0.0, 0.05)
    for i, f in enumerate((EB4, G4, BB4, EB5)):
        place(out, bell(f, 1.2, 0.8, (1.0, 2.0, 3.0), 1.0) * 0.22, 0.1 + i * 0.09)
    place(out, choir((EB4, G4), 1.2, 'ah', 2, 0.3, 0.8) * 0.08, 0.2)
    return room(out, 1.6, 0.4, 4500)


def s_arrogance():
    seconds = 1.6
    n = n_of(seconds)
    out = brass((C3, G3, C4), seconds, 0.4, 0.6, 2400) * 0.35 * np.linspace(0.6, 1.0, n)        # swelling, proud
    out += tone(glide(C4, G4, seconds, 0.5), seconds) * env_adsr(n, 0.3, 0.2, 0, 0.5, 0.6) * 0.15
    place(out, coins(0.6, 3, 0, 0.05), 0.0)
    return room(out, 1.4, 0.35, 3000)


def s_connect():
    seconds = 1.2
    out = silence(seconds)
    place(out, whoosh(0.35, 400, 3000, 0.6, 0.5) * 0.35, 0)
    f = 880.0                                                                    # a thin thread pulled taut
    pluck = harp(f, 1.0) * 0.5
    place(pluck, tone(glide(f * 0.97, f, 0.2), 0.2) * decay(n_of(0.2), 0.1) * 0.2, 0)
    place(out, pluck, 0.32)
    return room(out, 1.0, 0.3, 5000)


def s_coin_shot(variant):
    seconds = 0.8
    out = silence(seconds)
    place(out, tick(4200 + variant * 400) * 0.8, 0)                              # the flick
    place(out, coin_clink(5600 + variant * 300) * 0.7, 0.0)
    place(out, whoosh(0.18, 1500, 6000, 0.3, 0.4) * 0.5, 0.02)                   # it flies like a shot
    place(out, thump(110, 0.2, 0.5, 0.04) * 0.6, 0.16)                            # and hits
    return room(out, 0.6, 0.2, 5000)


def s_ricochet():
    seconds = 0.7
    n = n_of(seconds)
    out = tone(glide(5200, 1800, 0.4, 0.6), 0.4) * decay(n_of(0.4), 0.12) * 0.4
    out = np.pad(out, (0, n - len(out)))
    place(out, coin_clink(4800) * 0.6, 0)
    return room(out, 0.6, 0.25, 5000)


# =====================================================================================================
# Baron of Corruption: Distort, Seal (the illusory door), Corrosion, Ruin, Step
# =====================================================================================================

def s_distort():
    seconds = 1.2
    n = n_of(seconds)
    out = tone(glide(EB4, G3, seconds, 0.5), seconds) * env_adsr(n, 0.05, 0.2, 0, 0.6, 0.6) * 0.3
    out += tone(glide(G4, BB4, seconds, 0.5), seconds) * env_adsr(n, 0.05, 0.2, 0, 0.6, 0.6) * 0.2     # bent apart
    out += sweep(noise(seconds), lambda p: 600 + 2400 * abs(np.sin(p * 9.0)), 0.4) * env_adsr(n, 0.05, 0.2, 0, 0.6, 0.6) * 0.4
    return room(warble(out, 7.0, 0.006), 1.0, 0.3, 3000)


def s_reflect():
    seconds = 0.8
    out = reverse(whoosh(0.4, 300, 2600, 0.7, 0.7)) * 0.6
    out = np.pad(out, (0, n_of(seconds) - len(out)))
    place(out, bell(BB4, 0.5, 1.0, (1.0, 2.7, 5.1), 0.4) * 0.35, 0.36)
    return room(out, 0.7, 0.25, 4000)


def s_door_rise():
    seconds = 1.4
    n = n_of(seconds)
    out = sweep(noise(seconds), lambda p: 200 + 1800 * p, 0.5) * env_adsr(n, 0.6, 0.2, 0, 0.4, 0.7) * 0.4
    place(out, creak(0.8, 20, 60) * 0.5, 0.45)                                    # it swings open a heartbeat
    place(out, choir((C4, G4), 1.0, 'oo', 2, 0.4, 0.5) * 0.06, 0.3)
    return room(out, 1.4, 0.35, 3000)


def s_seal():
    seconds = 2.2
    out = silence(seconds)
    place(out, whoosh(0.25, 200, 1200, 0.85, 0.7) * 0.5, 0)
    place(out, thump(42, 0.9, 0.5, 0.25) * 1.6, 0.22)                             # slammed shut
    place(out, lowpass(noise(0.4), 500) * decay(n_of(0.4), 0.08) * 1.0, 0.22)
    place(out, clank(520) * 0.6, 0.34)                                             # and locked
    place(out, clank(360) * 0.6, 0.38)
    place(out, bell(C3, 1.6, 0.3, (1.0, 2.0, 2.9), 1.4) * 0.3, 0.24)
    return room(out, 2.2, 0.45, 2200)


def s_seal_break():
    seconds = 1.2
    out = silence(seconds)
    for i in range(18):
        place(out, bandpass(noise(0.02), 2000, 7000) * decay(n_of(0.02), 0.004) * RNG.uniform(0.3, 1.0), (i / 18) ** 0.6 * 0.4)
    place(out, sweep(noise(0.8), lambda p: 3000 * (0.1 ** p) + 100, 0.6) * env_adsr(n_of(0.8), 0.02, 0.2, 0, 0.5, 0.6) * 0.5, 0.1)
    return room(out, 1.0, 0.3, 4000)


def s_corrosion():
    seconds = 2.0
    n = n_of(seconds)
    out = whisper(1.6, 10, 600, 2600, seed=21) * 0.6
    out = np.pad(out, (0, n - len(out)))
    out += sweep(noise(seconds), lambda p: 300 + 200 * np.sin(p * 12), 0.5) * env_adsr(n, 0.5, 0.2, 0, 0.8, 0.6) * 0.35
    place(out, coins(1.2, 6, 0.2, 0.3) * 0.4, 0.2)
    out += drone(seconds, 49.0, (1, 1.06, 2)) * env_adsr(n, 0.5, 0.2, 0, 0.8, 0.7) * 0.25
    return room(out, 1.6, 0.4, 2400)


def s_ruin():
    seconds = 1.4
    n = n_of(seconds)
    out = silence(seconds)
    place(out, sweep(noise(0.3), lambda p: 300 + 4000 * p, 0.5) * env_adsr(n_of(0.3), 0.25, 0.02, 0, 0.03, 0.8) * 0.5, 0)
    beam = brass((C3, G3), 1.0, 0.01, 0.6, 3200) * 0.4
    beam += sweep(noise(1.0), lambda p: 1800 - 1200 * p, 0.6) * env_adsr(n_of(1.0), 0.01, 0.2, 0, 0.6, 0.7) * 0.4
    beam += tone(glide(C5, C4, 1.0, 0.7), 1.0) * env_adsr(n_of(1.0), 0.01, 0.2, 0, 0.6, 0.6) * 0.12
    place(out, beam, 0.28)
    place(out, thump(60, 0.5, 0.6, 0.15) * 0.8, 0.28)
    return room(out, 1.2, 0.3, 3200)


def s_step():
    seconds = 1.0
    out = silence(seconds)
    place(out, reverse(whoosh(0.35, 300, 2200, 0.6, 0.6)) * 0.5, 0)               # into one door...
    place(out, thump(80, 0.25, 0.6, 0.06) * 0.4, 0.34)
    place(out, whoosh(0.35, 300, 2200, 0.3, 0.6) * 0.5, 0.38)                       # ...out of the other
    place(out, bell(G4, 0.6, 0.8, (1.0, 2.0, 3.0), 0.5) * 0.25, 0.38)
    return room(out, 0.9, 0.3, 3500)


# =====================================================================================================
# Mentor of Disorder: Disorder, Paces, Majesty
# =====================================================================================================

def s_disorder():
    seconds = 1.8
    out = silence(seconds)
    notes = [C4, EB4, G4, BB4, C5, EB5]
    order = [3, 0, 5, 1, 4, 2, 5, 0]                                               # out of order
    for i, k in enumerate(order):
        at = i * 0.15 + RNG.uniform(-0.04, 0.06)                                    # and out of time
        place(out, bell(notes[k] * RNG.uniform(0.97, 1.03), 0.5, 0.8, (1.0, 2.0, 3.0), 0.4) * 0.25, max(0.0, at))
    place(out, reverse(bell(G4, 0.8, 0.8, (1.0, 2.0, 3.0), 0.6)) * 0.3, 0.6)
    for i in range(8):                                                              # a clock ticking wrong
        place(out, tick(2400 + RNG.uniform(-300, 300)) * 0.4, RNG.uniform(0, 1.4))
    return room(warble(out, 3.0, 0.01), 1.6, 0.35, 3000)


def s_paces():
    seconds = 1.0
    out = silence(seconds)
    for i in range(4):                                                               # a few long strides...
        at = (i / 4) ** 0.7 * 0.5
        place(out, thump(120 - i * 10, 0.12, 0.6, 0.03) * 0.5, at)
        place(out, whoosh(0.15, 600, 3000, 0.5, 0.6) * 0.3, at)
    place(out, reverse(bell(EB5, 0.4, 1.0, (1.0, 2.0, 3.0), 0.3)) * 0.25, 0.4)        # ...covering far too much
    return room(out, 0.8, 0.3, 3500)


def s_decoy_burst():
    seconds = 1.0
    out = silence(seconds)
    place(out, sweep(noise(0.5), lambda p: 3000 * (0.12 ** p) + 150, 0.6) * env_adsr(n_of(0.5), 0.005, 0.1, 0, 0.35, 0.6) * 0.7, 0)
    place(out, thump(90, 0.3, 0.5, 0.08) * 0.6, 0)
    for _ in range(10):
        place(out, tick(RNG.uniform(1800, 3400)) * 0.3, RNG.uniform(0.05, 0.5))
    return room(warble(out, 9.0, 0.01), 0.9, 0.3, 3000)


def s_majesty():
    seconds = 2.6
    n = n_of(seconds)
    out = brass((C2, G2, C3, G3), seconds, 0.08, 1.4, 1600) * 0.5                    # the throne room
    out += choir((C3, G3, C4, EB4), seconds, 'oh', 2, 0.1, 1.4) * 0.12
    place(out, thump(36, 1.0, 0.5, 0.3) * 1.4, 0.02)                                  # and everyone drops to a knee
    out += sub(seconds, 32, 0.05, 1.4) * 0.5
    return room(out, 2.4, 0.5, 2200)


# =====================================================================================================
# Earl of the Fallen: Bestowment, Exploit, Magnify and its quake
# =====================================================================================================

def s_bestowment():
    seconds = 1.6
    n = n_of(seconds)
    out = whisper(1.2, 6, 500, 2200, seed=31) * 0.5
    out = np.pad(out, (0, n - len(out)))
    out += sung(EB4, C4, seconds, 'oo', 3.0, 0.02, 0.8) * env_adsr(n, 0.3, 0.2, 0, 0.7, 0.6) * 0.2
    place(out, bell(G3, 1.0, 0.4, (1.0, 1.5, 2.0), 0.8) * 0.25, 0.4)
    return room(out, 1.4, 0.4, 2600)


def s_exploit():
    seconds = 1.4
    n = n_of(seconds)
    out = tone(glide(C4, C5, seconds, 0.5), seconds) * env_adsr(n, 0.1, 0.2, 0, 0.6, 0.6) * 0.25   # a rule stretched...
    out += tone(glide(G4, G5, seconds, 0.5), seconds) * env_adsr(n, 0.1, 0.2, 0, 0.6, 0.6) * 0.12
    out += sparkle(seconds, 10, 3000, 8000) * 0.25
    place(out, tick(5000) * 0.6, 0.8)                                                    # ...until it gives
    return room(out, 1.2, 0.35, 5000)


def s_magnify():
    seconds = 1.8
    n = n_of(seconds)
    swell = sweep(noise(1.2), lambda p: 200 + 3000 * p ** 2, 0.5) * env_adsr(n_of(1.2), 1.0, 0.05, 0, 0.15, 0.8) * 0.5
    out = silence(seconds)
    place(out, swell, 0)
    place(out, brass((C3, G3, C4), 0.8, 0.6, 0.2, 3000) * 0.3, 0.4)
    place(out, bell(C5, 1.0, 1.2, (1.0, 2.0, 3.0, 4.2), 0.8) * 0.35, 1.15)              # the moment it locks in
    place(out, thump(55, 0.6, 0.5, 0.2) * 0.8, 1.15)
    return room(out, 1.6, 0.35, 3500)


def s_quake():
    seconds = 2.0
    n = n_of(seconds)
    out = lowpass(noise(seconds), 120, 2) * env_adsr(n, 0.02, 0.3, 0, 1.4, 0.8) * 4.0
    place(out, thump(36, 1.2, 0.5, 0.35) * 1.4, 0)
    place(out, crumble(1.6, 0.1) * 0.5, 0)
    place(out, coins(1.2, 8, 0.05, 0.25) * 0.4, 0)                                         # the flower was a bribe
    return room(out, 1.8, 0.35, 2000)


# =====================================================================================================
# Mausoleums: raised, crumbling, Salvo, Sanctuary, Rebirth
# =====================================================================================================

def s_mausoleum_raise():
    seconds = 2.6
    out = silence(seconds)
    place(out, grind(1.4, 30, 90) * 0.7, 0)                                               # stone rising out of the ground
    place(out, thump(40, 0.8, 0.6, 0.25) * 1.2, 1.35)                                      # set in place
    place(out, bell(C3, 2.0, 0.4, (1.0, 2.0, 2.4, 3.0), 2.0) * 0.4, 1.4)                   # a funeral bell, once
    place(out, choir((C3, G3, C4), 1.2, 'oo', 2, 0.3, 0.9) * 0.08, 1.4)
    return room(out, 2.6, 0.45, 2200)


def s_mausoleum_crumble():
    seconds = 2.0
    out = silence(seconds)
    place(out, crumble(1.8, 0.0) * 1.0, 0)
    place(out, grind(0.6, 50, 120) * 0.4, 0.1)
    place(out, sweep(noise(1.0), lambda p: 1200 * (0.2 ** p) + 80, 0.6) * env_adsr(n_of(1.0), 0.1, 0.2, 0, 0.6, 0.6) * 0.3, 0.6)
    return room(out, 1.6, 0.35, 2600)


def s_salvo():
    seconds = 0.8
    out = silence(seconds)
    place(out, tone(glide(G5, G4, 0.3, 0.6), 0.3) * decay(n_of(0.3), 0.1) * 0.3, 0)
    place(out, sweep(noise(0.4), lambda p: 4000 - 3000 * p, 0.5) * env_adsr(n_of(0.4), 0.005, 0.1, 0, 0.25, 0.6) * 0.5, 0)
    place(out, thump(90, 0.25, 0.5, 0.06) * 0.6, 0.06)
    return room(out, 0.7, 0.25, 4000)


def s_sanctuary():
    seconds = 2.4
    n = n_of(seconds)
    out = choir((C4, EB4, G4), seconds, 'oo', 2, 0.8, 1.0) * 0.18
    out += drone(seconds, 65.41, (1, 1.5, 2)) * env_adsr(n, 0.6, 0.2, 0, 1.0, 0.6) * 0.2
    out += sparkle(seconds, 8, 2500, 6000) * 0.15
    return room(out, 2.0, 0.5, 3000)


def s_rebirth():
    seconds = 3.6
    n = n_of(seconds)
    out = silence(seconds)
    place(out, reverse(bell(C3, 1.4, 0.4, (1.0, 2.0, 2.4, 3.0), 1.4)) * 0.5, 0)           # pulled back...
    place(out, sweep(noise(1.4), lambda p: 100 + 3000 * p ** 2, 0.5) * env_adsr(n_of(1.4), 1.2, 0.05, 0, 0.15, 0.8) * 0.4, 0)
    place(out, crumble(1.2, 0.0) * 0.6, 1.35)                                               # ...out of the stele, which breaks
    place(out, thump(34, 1.2, 0.5, 0.35) * 1.5, 1.38)
    place(out, choir((C3, G3, C4, EB4, G4), 2.0, 'ah', 2, 0.05, 1.4) * 0.18, 1.4)
    place(out, brass((C2, G2, C3), 2.0, 0.05, 1.2, 1800) * 0.35, 1.4)
    out += sub(seconds, 30, 1.2, 1.4) * 0.4
    return room(out, 3.0, 0.5, 2200)


# =====================================================================================================
# The top Sequences: Frenzy, Entropy, Abolition, Exile, Inversion, Coronation, Decree
# =====================================================================================================

def s_frenzy():
    seconds = 2.0
    out = silence(seconds)
    place(out, whoosh(0.4, 200, 2000, 0.8, 0.6) * 0.5, 0)                                  # arms thrown open
    for i in range(12):                                                                     # twelve bolts
        at = 0.35 + i * 0.07 + RNG.uniform(0, 0.03)
        crack = highpass(noise(0.08), 1500) * decay(n_of(0.08), 0.02) * RNG.uniform(0.5, 1.0)
        zap = tone(glide(RNG.uniform(1200, 2400), RNG.uniform(200, 400), 0.15, 0.5), 0.15) * decay(n_of(0.15), 0.05) * 0.3
        place(out, crack, at)
        place(out, zap, at)
    place(out, sub(1.4, 40, 0.1, 1.0) * 0.4, 0.35)
    return room(out, 1.6, 0.3, 3500)


def s_entropy():
    seconds = 3.2
    n = n_of(seconds)
    out = silence(seconds)
    for i, f in enumerate((C3, EB3, G3, C4)):                                                # a chord coming apart
        drift = glide(f, f * RNG.uniform(0.86, 1.14), seconds, 1.4)
        place(out, tone(drift, seconds) * env_adsr(n, 0.4, 0.2, 0, 1.2, 0.6) * 0.18, 0)
    out += sweep(noise(seconds), lambda p: 300 + 2000 * np.abs(np.sin(p * 17)), 0.5) * env_adsr(n, 0.6, 0.2, 0, 1.2, 0.6) * 0.3
    for _ in range(14):
        place(out, tick(RNG.uniform(1200, 4000)) * 0.35, RNG.uniform(0.3, 2.8))
    return room(warble(out, 0.8, 0.02), 2.6, 0.45, 2600)


def s_abolition():
    seconds = 3.4
    n = n_of(seconds)
    world = lowpass(noise(seconds), 2500) * 0.25 + wind(seconds, 700, 0.4) * 0.4             # the world, as it was...
    world += bell(G4, seconds, 0.6, (1.0, 2.0, 3.0), 2.0) * 0.2
    out = muffle(world, 0.4, 1.4, 160)                                                     # ...drained of its sound
    out += sub(seconds, 28, 0.6, 1.4) * 0.4
    out += lowpass(noise(seconds), 90) * env_adsr(n, 1.2, 0.2, 0, 1.4, 0.7) * 1.5             # the pressure of a pocket
    return room(out, 2.6, 0.35, 1400)


def s_exile():
    seconds = 1.2
    n = n_of(seconds)
    out = sweep(noise(seconds), lambda p: 4000 * (0.05 ** p) + 60, 0.5) * env_adsr(n, 0.01, 0.2, 0, 0.8, 0.6) * 0.6
    fall = tone(glide(G4, C3, 0.8, 0.5), 0.8) * env_adsr(n_of(0.8), 0.01, 0.2, 0, 0.6, 0.6) * 0.2   # out of the world
    place(out, fall, 0)
    return room(muffle(out, 0.2, 0.6, 200), 0.8, 0.25, 1600)


def s_exile_return():
    return reverse(finish(s_exile()))


def s_inversion():
    seconds = 3.0
    n = n_of(seconds)
    swell = brass((C3, G3, C4, EB4), seconds, 0.05, 0.3, 2200) * 0.4 + choir((C4, G4), seconds, 'ah', 2, 0.1, 0.5) * 0.12
    out = reverse(swell * env_adsr(n, 0.02, 0.2, 0, 2.4, 0.7))                                # a chord heard backward
    place(out, thump(36, 1.0, 0.5, 0.3) * 1.2, seconds - 0.6)
    place(out, reverse(bell(C4, 1.4, 0.6, (1.0, 2.0, 3.0), 1.2)) * 0.4, seconds - 1.6)
    return room(out, 2.2, 0.4, 2400)


def s_coronation():
    seconds = 3.6
    n = n_of(seconds)
    out = silence(seconds)
    for i, f in enumerate((G3, C4, EB4, G4)):                                                # a fanfare rising
        place(out, brass((f,), 0.5, 0.03, 0.3, 3200) * 0.35, i * 0.18)
    place(out, brass((C3, G3, C4, EB4, G4), 2.6, 0.05, 1.6, 3000) * 0.4, 0.75)
    place(out, choir((C4, EB4, G4, C5), 2.6, 'ah', 2, 0.2, 1.6) * 0.16, 0.75)
    for i in range(5):                                                                       # bells ringing it in
        place(out, bell([C5, G4, EB5, C5, G5][i], 1.6, 1.0, (1.0, 2.0, 3.0, 4.2), 1.4) * 0.22, 0.75 + i * 0.22)
    place(out, thump(40, 1.0, 0.5, 0.3) * 1.0, 0.75)
    out += sub(seconds, 32, 0.8, 1.6) * 0.3
    return room(out, 3.0, 0.5, 3000)


def s_decree():
    seconds = 2.8
    n = n_of(seconds)
    out = silence(seconds)
    place(out, command(98.0, 0.9, 'oh') * 0.8, 0)                                             # one word...
    place(out, command(196.0, 0.9, 'oh') * 0.25, 0)
    place(out, sweep(noise(0.3), lambda p: 400 + 5000 * p, 0.5) * env_adsr(n_of(0.3), 0.25, 0.02, 0, 0.03, 0.8) * 0.4, 0.6)
    place(out, thump(32, 1.4, 0.5, 0.4) * 1.8, 0.9)                                           # ...and it is so
    place(out, brass((C2, C3, G3), 1.6, 0.01, 1.2, 2000) * 0.45, 0.9)
    place(out, bell(C3, 1.8, 0.5, (1.0, 2.0, 2.4, 3.0), 1.6) * 0.4, 0.9)
    out += sub(seconds, 28, 0.8, 1.6) * 0.4
    return room(out, 2.6, 0.5, 2000)


def s_order():
    seconds = 0.9
    out = silence(seconds)
    place(out, bell(G5, 0.7, 0.8, (1.0, 2.0, 3.0), 0.5) * 0.35, 0)
    place(out, coin_clink(4400) * 0.25, 0.04)
    return room(out, 0.7, 0.3, 5000)


# =====================================================================================================
# The list: event name -> the functions that make its variants
# =====================================================================================================

SOUNDS = {
    'verdict': [s_verdict],
    'brainwash': [s_brainwash],
    'brute_force': [s_brute_force],
    'iron_will': [s_iron_will],
    'bribe': [lambda: s_bribe(0), lambda: s_bribe(1)],
    'weaken': [s_weaken],
    'charm': [s_charm],
    'arrogance': [s_arrogance],
    'connect': [s_connect],
    'coin_shot': [lambda: s_coin_shot(0), lambda: s_coin_shot(1)],
    'ricochet': [s_ricochet],
    'distort': [s_distort],
    'reflect': [s_reflect],
    'door_rise': [s_door_rise],
    'seal': [s_seal],
    'seal_break': [s_seal_break],
    'corrosion': [s_corrosion],
    'ruin': [s_ruin],
    'step': [s_step],
    'disorder': [s_disorder],
    'paces': [s_paces],
    'decoy_burst': [s_decoy_burst],
    'majesty': [s_majesty],
    'bestowment': [s_bestowment],
    'exploit': [s_exploit],
    'magnify': [s_magnify],
    'quake': [s_quake],
    'mausoleum_raise': [s_mausoleum_raise],
    'mausoleum_crumble': [s_mausoleum_crumble],
    'salvo': [s_salvo],
    'sanctuary': [s_sanctuary],
    'rebirth': [s_rebirth],
    'frenzy': [s_frenzy],
    'entropy': [s_entropy],
    'abolition': [s_abolition],
    'exile': [s_exile],
    'exile_return': [s_exile_return],
    'inversion': [s_inversion],
    'coronation': [s_coronation],
    'decree': [s_decree],
    'order': [s_order],
}

SUBTITLES = {
    'verdict': 'A gavel falls', 'brainwash': 'Quiet words are spoken', 'brute_force': 'A crushing blow',
    'iron_will': 'A will hardens', 'bribe': 'Coins change hands', 'weaken': 'Strength drains away',
    'charm': 'Someone is won over', 'arrogance': 'Pride swells', 'connect': 'A thread pulls taut',
    'coin_shot': 'A coin is flicked', 'ricochet': 'A coin ricochets', 'distort': 'Something is distorted',
    'reflect': 'A shot is turned back', 'door_rise': 'An illusory door appears', 'seal': 'A door slams shut',
    'seal_break': 'A Seal breaks', 'corrosion': 'Greed spreads', 'ruin': 'A beam of ruin',
    'step': 'Someone steps through a mausoleum', 'disorder': 'Order comes apart', 'paces': 'Distance is disordered',
    'decoy_burst': 'A false self bursts', 'majesty': 'Majesty presses down', 'bestowment': 'A feeling is bestowed',
    'exploit': 'A rule gives way', 'magnify': 'Something is magnified', 'quake': 'The ground quakes',
    'mausoleum_raise': 'A mausoleum rises', 'mausoleum_crumble': 'A mausoleum crumbles',
    'salvo': 'A mausoleum fires', 'sanctuary': 'A sanctuary hums', 'rebirth': 'The Emperor rises again',
    'frenzy': 'Black bolts burst out', 'entropy': 'Order unravels', 'abolition': 'The world falls silent',
    'exile': 'Something is abolished', 'exile_return': 'Something returns', 'inversion': 'Everything turns over',
    'coronation': 'A coronation', 'decree': 'A Decree is spoken', 'order': 'An order stands',
}


def encode(samples, path):
    kit.encode(samples, path)


def main(assets, wav_dir=None):
    folder = os.path.join(assets, 'sounds/prince_of_abolition')
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
            encode(samples, os.path.join(folder, file + '.ogg'))
            if wav_dir:
                os.makedirs(wav_dir, exist_ok=True)
                kit.write_wav(samples, os.path.join(wav_dir, file + '.wav'))
            files.append({'name': f'circle_of_inevitability:prince_of_abolition/{file}'})
            print(f'sounds/prince_of_abolition/{file}.ogg  {len(samples) / SR:.2f} s')
        events['prince_of_abolition.' + name] = {
            'subtitle': f'subtitles.circle_of_inevitability.prince_of_abolition.{name}',
            'sounds': files,
        }
    with open(sounds_json, 'w') as f:
        json.dump(events, f, indent=2)
    lang_path = os.path.join(assets, 'lang/en_us.json')                # the subtitles
    if os.path.exists(lang_path):
        with open(lang_path, encoding='utf-8') as f:
            lang = json.load(f)
        for name, text in SUBTITLES.items():
            lang[f'subtitles.circle_of_inevitability.prince_of_abolition.{name}'] = text
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
