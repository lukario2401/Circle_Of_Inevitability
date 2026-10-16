"""Synthesizes the Knight Of Misfortune sounds and writes sounds.json.

python3 tools/model_gen/knight_of_misfortune/sounds.py src/main/resources/assets/circle_of_inevitability [--wav dir]

Every sound is built from scratch (sine partials, filtered noise, formant voices and whispers, a synthetic room), so
nothing here is copied from anywhere. The building blocks (tone, bell, choir, room, thump, drone, finish...) are the
ones in tools/model_gen/beauty_goddess/sounds.py, loaded from there, as ../hand_of_god/sounds.py does. Needs numpy,
scipy and ffmpeg (with libvorbis).

The .ogg files go to sounds/knight_of_misfortune/, mono 44.1 kHz (Minecraft only places mono sounds in the world),
and each one gets an event "knight_of_misfortune.<name>" in sounds.json and a subtitle in lang/en_us.json. The same
names are registered in sounds/ModSounds.java (KNIGHT_...): a new sound here needs a line there too.

The sound of the Pathway: night, sleep and silence. Whispers instead of shouts, low drones, a music box for the
lullaby, wet flesh, old bone, a wolf, a banshee, and under the top Sequences a pull into nothing.

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
RNG = np.random.default_rng(1717)
silence, noise, place, env_adsr, decay, fade = kit.silence, kit.noise, kit.place, kit.env_adsr, kit.decay, kit.fade
lowpass, highpass, bandpass, sweep, tone, glide = kit.lowpass, kit.highpass, kit.bandpass, kit.sweep, kit.tone, kit.glide
bell, choir, room, thump, heartbeat, finish = kit.bell, kit.choir, kit.room, kit.thump, kit.heartbeat, kit.finish
sparkle, drone, creak, clank, harp = kit.sparkle, kit.drone, kit.creak, kit.clank, kit.harp
FORMANTS = kit.FORMANTS


def n_of(seconds):
    return int(seconds * SR)


def t_of(seconds):
    return np.arange(n_of(seconds)) / SR


# =====================================================================================================
# Building blocks of the night: whispers, a gliding voice, wet flesh, wind
# =====================================================================================================

def whisper(seconds, syllables=6, lo=900, hi=3800, seed=0):
    """Breath shaped into syllables: noise through a moving band, opening and closing like soft speech."""
    rnd = np.random.default_rng(seed)
    n = n_of(seconds)
    env = np.zeros(n)
    at = 0.02
    for _ in range(syllables):
        length = rnd.uniform(0.07, 0.2)
        a, b = n_of(at), n_of(at + length)
        if a >= n:
            break
        seg = np.sin(np.linspace(0, np.pi, max(1, min(b, n) - a))) ** 1.5
        env[a:a + len(seg)] = np.maximum(env[a:a + len(seg)], seg * rnd.uniform(0.6, 1.0))
        at += length + rnd.uniform(0.01, 0.06)
    centers = rnd.uniform(lo, hi, 8)
    voiced = sweep(noise(seconds), lambda p: float(np.interp(p, np.linspace(0, 1, 8), centers)), 0.45)
    hiss = highpass(noise(seconds), 5000) * 0.25
    return (voiced + hiss) * env


def sung(f0, f1, seconds, vowel='oo', vibrato=5.0, depth=0.01, curve=1.0):
    """One voice gliding from f0 to f1 (a howl, a wail): harmonics shaped by the vowel's formants."""
    n = n_of(seconds)
    t = t_of(seconds)
    f = glide(f0, f1, seconds, curve) * (1 + depth * np.sin(2 * np.pi * vibrato * t) * np.clip(t / 0.3, 0, 1))
    phase = np.cumsum(2 * np.pi * f / SR)
    out = np.zeros(n)
    for k in range(1, 30):
        fk = np.mean(f) * k
        if fk > 7000:
            break
        gain = sum(g / (1 + ((fk - fc) / bw) ** 2) for fc, bw, g in FORMANTS[vowel])
        out += (gain / k ** 0.5 + 0.002) * np.sin(k * phase)
    return out / max(1e-9, np.max(np.abs(out)))


def sung_path(points, seconds, vowel='oo', vibrato=5.0, depth=0.01):
    """One voice following a pitch path without a break: points = [(time 0..1, Hz), ...], eased between them."""
    t = np.linspace(0, 1, n_of(seconds))
    ts = [p[0] for p in points]
    fs = np.log([p[1] for p in points])
    f = np.exp(np.interp(t, ts, fs))
    f = np.convolve(f, np.ones(2001) / 2001, mode='same')                    # round the corners
    f[:1000], f[-1000:] = f[1000], f[-1001]
    tt = t_of(seconds)
    f = f * (1 + depth * np.sin(2 * np.pi * vibrato * tt) * np.clip(tt / 0.3, 0, 1))
    phase = np.cumsum(2 * np.pi * f / SR)
    out = np.zeros(len(t))
    for k in range(1, 30):
        fk = np.mean(f) * k
        if fk > 7000:
            break
        gain = sum(g / (1 + ((fk - fc) / bw) ** 2) for fc, bw, g in FORMANTS[vowel])
        out += (gain / k ** 0.5 + 0.002) * np.sin(k * phase)
    return out / max(1e-9, np.max(np.abs(out)))


def squelch(seconds=0.4, start=1600, end=200, wet=34):
    """Wet flesh: a band of noise falling in pitch, fluttering."""
    n = n_of(seconds)
    x = sweep(noise(seconds), lambda p: start * (end / start) ** p, 0.8) * env_adsr(n, 0.005, 0.08, 0, seconds * 0.6, 0.6)
    return x * (1 + 0.6 * np.sin(2 * np.pi * wet * t_of(seconds)))


def whoosh(seconds, lo=300, hi=2600, peak=0.5, width=0.7):
    n = n_of(seconds)
    x = sweep(noise(seconds), lambda p: lo + (hi - lo) * np.exp(-((p - peak) / 0.25) ** 2), width)
    return x * np.exp(-((np.linspace(0, 1, n) - peak) / 0.28) ** 2)


def wind(seconds, base=500, rate=0.6):
    """Dark wind: a slow-moving band of noise."""
    n = n_of(seconds)
    x = sweep(noise(seconds), lambda p: base * (1 + 0.6 * np.sin(p * 6.28 * rate * seconds)), 0.6)
    return x * env_adsr(n, seconds * 0.3, 0.2, 0, seconds * 0.4, 0.8)


def sub(seconds, freq=36, attack=0.3, release=0.8):
    """A sub-bass swell: felt more than heard."""
    n = n_of(seconds)
    return tone(freq, seconds) * env_adsr(n, attack, 0.2, 0, release, 0.8)


def music_box(notes, step=0.28, ring=1.6):
    """A tinkling music box: high plucked tines, a little out of tune, ringing on."""
    out = silence(step * len(notes) + ring)
    for i, f in enumerate(notes):
        tine = bell(f, ring, 1.3, (1.0, 3.9, 9.8), 1.2) * 0.6 + harp(f, ring) * 0.4
        place(out, tine * (0.9 if i % 2 == 0 else 0.7), i * step)
    return out


def bone_ring(freq=820, seconds=1.0):
    """Old bone struck: dull and hollow, dying quickly."""
    return bell(freq, seconds, 0.4, (1.0, 2.3, 3.6, 5.1), 0.5)


def tick(freq=3200):
    d = 0.03
    return tone(freq, d) * decay(n_of(d), 0.004) + highpass(noise(d), 4000) * decay(n_of(d), 0.002) * 0.5


# Low-ish minor notes for the poems (D minor, an octave of night)
D3, F3, A3, C4, D4, F4, A4, D5, F5, A5, D6 = 146.83, 174.61, 220.0, 261.63, 293.66, 349.23, 440.0, 587.33, 698.46, 880.0, 1174.66


# =====================================================================================================
# Sleepless: Spirit Vision, Dowsing
# =====================================================================================================

def s_spirit_vision():
    seconds = 2.0
    out = silence(seconds)
    place(out, sweep(noise(1.2), lambda p: 300 + 3000 * p, 0.5) * env_adsr(n_of(1.2), 0.6, 0.2, 0, 0.4, 0.7) * 0.25, 0)
    place(out, choir((D4, A4), 1.6, 'ee', 2, 0.5, 1.0, depth=0.003) * 0.25, 0.2)
    place(out, sparkle(1.2, 10, 3000, 7000) * 0.3, 0.4)
    return room(out, 1.6, 0.4, 3500)


def s_dowsing():
    seconds = 1.8
    out = silence(seconds)
    for i in range(3):                                                       # the pendulum swings, chain ticking
        place(out, tick(2600 + i * 200) * 0.5, 0.1 + i * 0.45)
    place(out, bell(A5, 1.4, 0.9, (1.0, 2.0, 3.01), 1.2) * 0.4, 1.1)
    return room(out, 1.2, 0.35, 5000)


# =====================================================================================================
# Midnight Poet: the poems
# =====================================================================================================

def s_poem(variant):
    seconds = 1.6
    out = silence(seconds)
    place(out, whisper(1.3, 7, 900, 3600, seed=variant) * 0.7, 0)
    place(out, choir(((D4, F4), (A3, C4), (F3, A3))[variant], 1.4, 'oo', 2, 0.4, 0.8, depth=0.003) * 0.12, 0.1)
    return room(out, 1.4, 0.45, 3000)


def s_tranquilize():
    seconds = 2.4
    out = silence(seconds)
    place(out, choir((D4, A4, D5), 2.2, 'ah', 2, 0.6, 1.2, depth=0.002) * 0.3, 0)
    place(out, bell(D5, 2.0, 0.6, (1.0, 2.0, 3.0), 2.0) * 0.25, 0.3)
    place(out, lowpass(noise(2.0), 700) * env_adsr(n_of(2.0), 0.8, 0.2, 0, 0.8, 0.5) * 0.1, 0)
    return room(out, 2.2, 0.45, 2800)


def s_lullaby():
    out = music_box([A5, F5, D5, F5, A5, D6, A5])
    place(out, choir((D4, F4), 1.8, 'oo', 2, 0.8, 1.0, depth=0.004) * 0.12, 0.3)
    return room(out, 1.8, 0.45, 4000)


def s_pacify():
    seconds = 2.2
    n = n_of(seconds)
    out = sung(D4, A3, seconds, 'oo', 4.0, 0.006, 0.6) * env_adsr(n, 0.4, 0.2, 0, 1.0, 0.6) * 0.35
    out += lowpass(noise(seconds), 400) * env_adsr(n, 0.8, 0.2, 0, 1.0, 0.5) * 0.12
    place(out, whisper(1.0, 4, 600, 2000, seed=9) * 0.35, 0.3)
    return room(out, 2.0, 0.5, 2400)


def s_fear():
    seconds = 1.8
    n = n_of(seconds)
    out = silence(seconds)
    place(out, sung(A4, D6, 0.9, 'ee', 9.0, 0.03, 2.0) * env_adsr(n_of(0.9), 0.2, 0.1, 0, 0.4, 0.7) * 0.25, 0.2)
    place(out, drone(seconds, 36.7, (1, 1.06, 2.03)) * env_adsr(n, 0.05, 0.2, 0, 1.0, 0.8) * 0.5, 0)
    place(out, heartbeat(0.9, 48) * 0.8, 0.1)
    place(out, heartbeat(0.7, 50) * 0.8, 0.75)
    return room(out, 1.4, 0.35, 2600)


def s_agitate():
    seconds = 1.4
    n = n_of(seconds)
    out = whisper(1.0, 9, 1200, 4500, seed=4) * 0.6
    out = np.pad(out, (0, n - len(out)))
    growl = sweep(noise(seconds), lambda p: 300 + 500 * p, 0.4) * (1 + 0.8 * np.sin(2 * np.pi * 28 * t_of(seconds)))
    out += growl * env_adsr(n, 0.3, 0.2, 0, 0.5, 0.7) * 0.35
    out += tone(glide(110, 165, seconds), seconds) * env_adsr(n, 0.4, 0.2, 0, 0.4, 0.6) * 0.15
    return room(out, 1.0, 0.3, 3000)


# =====================================================================================================
# Nightmare: limbs, the soul out of the body, dreams
# =====================================================================================================

def s_limbs():
    seconds = 1.4
    out = silence(seconds)
    for i in range(4):                                                       # four limbs tearing out of the back
        place(out, squelch(0.45, 1400 + i * 150, 180) * 0.6, i * 0.14)
    place(out, thump(70, 0.4, 0.6, 0.1) * 0.5, 0.0)
    place(out, sub(1.0, 40, 0.2, 0.6) * 0.3, 0.2)
    return room(out, 1.0, 0.3, 2400)


def s_lash(variant):
    seconds = 0.6
    out = silence(seconds)
    place(out, whoosh(0.3, 200, 1800 + variant * 300, 0.7, 0.8) * 0.7, 0)
    place(out, squelch(0.25, 900, 250, 50) * 0.6, 0.22)
    place(out, thump(90, 0.2, 0.6, 0.05) * 0.5, 0.22)
    return room(out, 0.6, 0.2, 3000)


def s_limbs_tear():
    seconds = 1.2
    out = silence(seconds)
    place(out, squelch(0.6, 2500, 120, 60), 0)
    place(out, highpass(noise(0.1), 1200) * decay(n_of(0.1), 0.03) * 0.8, 0)
    place(out, sung(F4, D3, 0.8, 'ah', 6, 0.02, 0.5) * env_adsr(n_of(0.8), 0.02, 0.1, 0, 0.5, 0.7) * 0.25, 0.05)
    return room(out, 1.0, 0.3, 2400)


def s_soul_out():
    seconds = 2.2
    n = n_of(seconds)
    out = choir((D4, F4, A4), seconds, 'oo', 2, 0.6, 1.0, depth=0.004) * 0.25
    out += sweep(noise(seconds), lambda p: 400 + 3000 * p, 0.5) * env_adsr(n, 1.0, 0.2, 0, 0.8, 0.6) * 0.2
    place(out, bell(D6, 1.4, 1.0, (1.0, 2.0, 2.76), 1.2) * 0.2, 0.9)
    return room(out, 2.2, 0.5, 3500)


def s_soul_return():
    seconds = 1.4
    n = n_of(seconds)
    out = sweep(noise(seconds), lambda p: 3500 - 3000 * p, 0.5) * env_adsr(n, 0.2, 0.2, 0, 0.6, 0.6) * 0.3
    place(out, heartbeat(0.9, 52), 0.7)
    return room(out, 1.2, 0.3, 3000)


def s_dream_enter():
    seconds = 2.4
    n = n_of(seconds)
    out = silence(seconds)
    sh = tone(glide(D5, D4, 1.6, 0.6), 1.6) * env_adsr(n_of(1.6), 0.1, 0.2, 0, 1.0, 0.6) * 0.15
    place(out, sh, 0)
    place(out, choir((D4, F4, A4, C4 * 2), 2.0, 'oo', 2, 0.8, 1.0, depth=0.008) * 0.2, 0.2)
    place(out, sparkle(1.6, 14, 2500, 6000) * 0.2, 0.2)
    out += lowpass(noise(seconds), 300) * env_adsr(n, 0.8, 0.2, 0, 1.0, 0.6) * 0.1
    return room(out, 2.6, 0.55, 2600)


def s_dream_shape():
    seconds = 1.6
    out = silence(seconds)
    for i, f in enumerate((D5, A4, F4, D4)):                                # the dream bends
        place(out, tone(glide(f, f * 0.94, 0.6), 0.6) * env_adsr(n_of(0.6), 0.05, 0.1, 0, 0.4, 0.6) * 0.2, i * 0.18)
    place(out, whisper(0.9, 5, 700, 2500, seed=21) * 0.4, 0.3)
    return room(out, 2.0, 0.5, 2600)


# =====================================================================================================
# Soul Assurer: Requiem
# =====================================================================================================

def s_requiem():
    seconds = 3.2
    out = silence(seconds)
    place(out, choir((D3, A3, D4, F4), 3.0, 'ah', 2, 0.8, 1.6, depth=0.003) * 0.35, 0)
    place(out, music_box([D5, A4, F4, D4], 0.5, 1.6) * 0.3, 0.6)
    place(out, sub(2.6, 37, 0.8, 1.2) * 0.25, 0.2)
    return room(out, 3.0, 0.55, 2400)


def s_soothe():
    seconds = 2.0
    out = silence(seconds)
    place(out, choir((F4, A4, C4 * 2), 1.8, 'oo', 2, 0.4, 1.0, depth=0.003) * 0.3, 0)
    for i, f in enumerate((A5, F5, D6)):
        place(out, bell(f, 1.2, 0.8, (1.0, 2.0, 3.01), 1.4) * 0.2, 0.2 + i * 0.22)
    return room(out, 2.0, 0.45, 4000)


# =====================================================================================================
# Spirit Warlock: spirits in the teeth
# =====================================================================================================

def s_teeth():
    seconds = 0.9
    out = silence(seconds)
    for i in range(5):                                                       # teeth clacking, a spirit let out
        place(out, tick(1800 + RNG.uniform(-300, 300)) * 0.7, i * 0.05)
    place(out, sweep(noise(0.6), lambda p: 600 + 2400 * p, 0.6) * env_adsr(n_of(0.6), 0.05, 0.1, 0, 0.4, 0.6) * 0.4, 0.25)
    return room(out, 1.0, 0.4, 3500)


def s_banshee():
    seconds = 2.6
    n = n_of(seconds)
    out = silence(seconds)
    wail = sung_path([(0, A4), (0.35, D6), (0.55, D6 * 0.97), (1, F5)], 2.4, 'ee', 7, 0.028)
    place(out, wail * env_adsr(len(wail), 0.15, 0.2, 0, 0.8, 0.85) * 0.45, 0.1)
    place(out, sung(A4 * 1.02, D6 * 0.98, 1.2, 'ee', 6, 0.02, 0.5) * 0.15, 0.15)
    out += highpass(noise(seconds), 2500) * env_adsr(n, 0.3, 0.2, 0, 1.0, 0.6) * 0.12
    return room(out, 2.4, 0.5, 3500)


def s_earth():
    seconds = 1.6
    n = n_of(seconds)
    out = lowpass(noise(seconds), 220, 2) * env_adsr(n, 0.15, 0.2, 0, 0.8, 0.8)
    out *= 0.7 + 0.3 * np.sin(2 * np.pi * 7 * t_of(seconds))
    for i in range(10):                                                      # pebbles
        place(out, bandpass(noise(0.03), 900, 3000) * decay(n_of(0.03), 0.006) * RNG.uniform(0.2, 0.5), RNG.uniform(0, 1.2))
    place(out, thump(50, 0.4, 0.6, 0.12) * 0.6, 0)
    return room(out, 1.0, 0.25, 2000)


def s_gale():
    seconds = 2.2
    out = wind(seconds, 700, 0.5) * 0.7
    place(out, whoosh(0.8, 400, 3000, 0.4, 0.8) * 0.6, 0.1)
    place(out, sung(F4, A4, 1.2, 'oo', 3, 0.01) * env_adsr(n_of(1.2), 0.4, 0.2, 0, 0.6, 0.5) * 0.1, 0.4)
    return room(out, 1.6, 0.4, 3500)


def s_command_spirits():
    seconds = 3.0
    out = silence(seconds)
    place(out, choir((D3, A3, D4), 2.8, 'oh', 3, 0.5, 1.4, depth=0.005) * 0.35, 0)
    for i in range(6):                                                       # spirits answering, one by one
        place(out, whisper(0.5, 3, 800, 3000, seed=40 + i) * 0.3, 0.4 + i * 0.3)
    place(out, sub(2.0, 34, 0.4, 1.0) * 0.3, 0.2)
    return room(out, 2.8, 0.55, 2400)


# =====================================================================================================
# Nightwatcher: the Night Domain, hair, Misfortune, the Demonic Wolf
# =====================================================================================================

def s_night_domain():
    seconds = 4.0
    n = n_of(seconds)
    out = drone(seconds, 36.7, (1, 1.5, 2, 3)) * env_adsr(n, 1.2, 0.4, 0, 1.4, 0.8) * 0.5
    out += sweep(noise(seconds), lambda p: 200 + 900 * np.sin(p * np.pi), 0.6) * env_adsr(n, 1.4, 0.2, 0, 1.4, 0.7) * 0.4
    place(out, choir((D3, A3, F4), 3.0, 'oo', 2, 1.2, 1.2, depth=0.003) * 0.2, 0.6)
    return room(out, 3.0, 0.45, 2000)


def s_hair():
    seconds = 1.2
    n = n_of(seconds)
    out = highpass(noise(seconds), 3000) * env_adsr(n, 0.05, 0.2, 0, 0.6, 0.6) * 0.5
    out *= 0.6 + 0.4 * np.sin(2 * np.pi * 40 * t_of(seconds)) ** 2
    place(out, whoosh(0.5, 1500, 6000, 0.3, 0.6) * 0.5, 0)
    place(out, tone(glide(A5, D5, 0.8), 0.8) * env_adsr(n_of(0.8), 0.05, 0.1, 0, 0.5, 0.5) * 0.05, 0.3)
    return room(out, 1.0, 0.3, 4500)


def s_curse():
    seconds = 2.0
    out = silence(seconds)
    place(out, whisper(0.8, 4, 600, 2200, seed=13) * 0.5, 0)
    place(out, bell(F3 * 2, 1.6, 0.5, (1.0, 1.21, 2.0, 2.37, 3.1), 1.2) * 0.35, 0.5)     # a cracked, sour bell
    place(out, tone(glide(220, 196, 1.4), 1.4) * env_adsr(n_of(1.4), 0.05, 0.2, 0, 0.8, 0.6) * 0.12, 0.5)
    return room(out, 1.8, 0.4, 2600)


def s_mishap(variant):
    seconds = 0.8
    out = silence(seconds)
    if variant == 0:                                                         # a stumble
        place(out, thump(80, 0.3, 0.5, 0.08) * 0.8, 0)
        place(out, bandpass(noise(0.15), 300, 1500) * decay(n_of(0.15), 0.05) * 0.5, 0.02)
    elif variant == 1:                                                       # something breaks
        place(out, clank(900) * 0.6, 0)
        place(out, highpass(noise(0.2), 2500) * decay(n_of(0.2), 0.05) * 0.4, 0.01)
    else:                                                                    # a crack
        place(out, highpass(noise(0.05), 1500) * decay(n_of(0.05), 0.01), 0)
        place(out, bone_ring(600, 0.6) * 0.4, 0.01)
    place(out, tone(glide(440, 415, 0.5), 0.5) * env_adsr(n_of(0.5), 0.02, 0.1, 0, 0.3, 0.5) * 0.08, 0.05)
    return room(out, 0.8, 0.25, 3000)


def s_catastrophe():
    seconds = 2.6
    n = n_of(seconds)
    out = silence(seconds)
    crack = highpass(noise(0.12), 800) * decay(n_of(0.12), 0.03)
    place(out, crack * 1.2, 0.05)
    boom = lowpass(noise(2.4), 260, 2) * env_adsr(n_of(2.4), 0.01, 0.3, 0, 1.6, 0.6)
    place(out, boom, 0.06)
    place(out, thump(42, 1.0, 0.5, 0.4), 0.06)
    out += drone(seconds, 32.7, (1, 1.06)) * env_adsr(n, 0.4, 0.2, 0, 1.0, 0.6) * 0.2
    return room(out, 2.4, 0.4, 2200)


def s_wolf_form():
    seconds = 3.2
    n = n_of(seconds)
    out = silence(seconds)
    for i in range(5):                                                       # bones cracking as the body changes
        place(out, highpass(noise(0.04), 1200) * decay(n_of(0.04), 0.01) * 0.8, 0.1 + i * 0.12 + RNG.uniform(0, 0.04))
    place(out, squelch(0.7, 1200, 100, 30) * 0.5, 0.2)
    growl = lowpass(noise(2.0), 400) * (1 + 0.9 * np.sin(2 * np.pi * 26 * t_of(2.0)))
    place(out, growl * env_adsr(n_of(2.0), 0.4, 0.3, 0, 0.8, 0.8) * 0.6, 0.8)
    place(out, sub(2.4, 38, 0.5, 1.0) * 0.4, 0.6)
    out += drone(seconds, 36.7, (1, 2, 3)) * env_adsr(n, 1.0, 0.3, 0, 1.0, 0.6) * 0.2
    return room(out, 2.4, 0.35, 2000)


def s_wolf_howl():
    seconds = 3.0
    out = silence(seconds)
    howl = sung_path([(0, A3), (0.22, A4), (0.7, A4 * 0.96), (1, D4)], 2.7, 'oo', 5, 0.01)
    place(out, howl * env_adsr(len(howl), 0.15, 0.3, 0, 0.6, 0.85) * 0.5, 0)
    place(out, sung(A3 * 1.5, A4 * 1.5, 0.6, 'oo', 5, 0.006, 0.5) * env_adsr(n_of(0.6), 0.2, 0.1, 0, 0.3, 0.7) * 0.1, 0.05)
    return room(out, 3.0, 0.5, 2400)


def s_wolf_bite():
    seconds = 0.6
    out = silence(seconds)
    place(out, whoosh(0.2, 300, 2000, 0.6, 0.8) * 0.4, 0)
    place(out, tick(1400) * 1.0 + tick(1100) * 0.8, 0.18)
    place(out, squelch(0.3, 1000, 200, 40) * 0.6, 0.19)
    place(out, thump(70, 0.25, 0.6, 0.06) * 0.6, 0.18)
    return room(out, 0.6, 0.25, 2600)


def s_wolf_step():
    seconds = 0.5
    out = silence(seconds)
    place(out, thump(46, 0.4, 0.5, 0.12) * 0.9, 0)
    place(out, lowpass(noise(0.2), 400) * decay(n_of(0.2), 0.05) * 0.4, 0)
    return out


# =====================================================================================================
# Horror Bishop: the Sword of Darkness and its tide, the Nightmare World
# =====================================================================================================

def s_dark_sword():
    seconds = 2.2
    n = n_of(seconds)
    out = silence(seconds)
    for i in range(6):                                                       # bone knitting together
        place(out, bone_ring(700 + i * 90, 0.4) * 0.3, i * 0.08)
    place(out, choir((D3, A3, D4), 1.8, 'oh', 2, 0.3, 1.0, depth=0.004) * 0.25, 0.3)
    place(out, sub(1.6, 36, 0.3, 0.8) * 0.35, 0.3)
    out += whisper(seconds, 8, 700, 2200, seed=33) * env_adsr(n, 0.4, 0.2, 0, 1.0, 0.6) * 0.15
    return room(out, 2.0, 0.45, 2400)


def s_dark_tide():
    seconds = 2.6
    n = n_of(seconds)
    surge = sweep(noise(seconds), lambda p: 200 + 900 * np.exp(-((p - 0.35) / 0.3) ** 2), 0.8)
    out = surge * env_adsr(n, 0.25, 0.3, 0, 1.4, 0.75) * 0.8
    out += lowpass(noise(seconds), 150) * env_adsr(n, 0.3, 0.2, 0, 1.2, 0.8) * 0.5
    place(out, choir((D3, F3), 1.6, 'ah', 2, 0.2, 1.0, depth=0.01) * 0.15, 0.3)
    return room(out, 2.2, 0.4, 1800)


def s_edict(variant):
    seconds = 1.6
    out = silence(seconds)
    place(out, whoosh(0.35, 300, 2400, 0.6, 0.7) * 0.6, 0)
    if variant == 0:                                                         # Eternal Sleep
        place(out, music_box([A5, D5], 0.15, 1.0) * 0.4, 0.3)
    elif variant == 1:                                                       # Despair
        place(out, sung(D4, A3, 1.0, 'oh', 4, 0.01) * env_adsr(n_of(1.0), 0.05, 0.2, 0, 0.6, 0.6) * 0.35, 0.3)
    else:                                                                    # Destruction
        place(out, thump(40, 0.8, 0.5, 0.25), 0.3)
        place(out, highpass(noise(0.3), 800) * decay(n_of(0.3), 0.08) * 0.8, 0.3)
    return room(out, 1.4, 0.35, 2600)


def s_nightmare_world():
    seconds = 4.0
    n = n_of(seconds)
    out = silence(seconds)
    sh = tone(glide(D6, D3, 2.0, 0.5), 2.0) * env_adsr(n_of(2.0), 0.1, 0.3, 0, 1.0, 0.5) * 0.12
    place(out, sh, 0)
    out += drone(seconds, 36.7, (1, 1.06, 1.5, 2.12)) * env_adsr(n, 1.0, 0.3, 0, 1.4, 0.8) * 0.4
    place(out, choir((D3, F3, A3, C4), 3.0, 'oo', 2, 1.0, 1.4, depth=0.012) * 0.25, 0.5)
    place(out, music_box([D6, A5, F5, D5], 0.6, 1.2) * 0.12, 1.6)
    return room(out, 3.0, 0.55, 2200)


def s_horror():
    seconds = 1.6
    n = n_of(seconds)
    out = silence(seconds)
    place(out, heartbeat(0.7, 44), 0)
    place(out, sweep(noise(1.2), lambda p: 3000 * (0.2 ** p) + 200, 0.5) * env_adsr(n_of(1.2), 0.02, 0.2, 0, 0.8, 0.6) * 0.4, 0)
    out += tone(glide(2600, 2900, seconds), seconds) * env_adsr(n, 0.4, 0.2, 0, 0.6, 0.5) * 0.03
    return room(out, 1.4, 0.4, 2600)


# =====================================================================================================
# Servant of Concealment and up: Concealment, the Mantle, Eternal Rest, the Destination of All Things
# =====================================================================================================

def s_conceal():
    seconds = 1.6
    n = n_of(seconds)
    out = sweep(noise(seconds), lambda p: 2500 * (0.1 ** p) + 150, 0.6) * env_adsr(n, 0.1, 0.2, 0, 1.0, 0.7) * 0.6
    place(out, tone(glide(F5, F3, 0.9, 0.5), 0.9) * env_adsr(n_of(0.9), 0.02, 0.1, 0, 0.6, 0.6) * 0.1, 0.05)
    return room(out, 1.4, 0.4, 2400)


def s_reveal():
    seconds = 1.2
    n = n_of(seconds)
    out = sweep(noise(seconds), lambda p: 150 + 4000 * p, 0.5) * env_adsr(n, 0.4, 0.1, 0, 0.3, 0.7) * 0.5
    place(out, bell(D6, 0.9, 1.2, (1.0, 2.0, 2.76, 5.4), 0.9) * 0.35, 0.45)
    return room(out, 1.2, 0.35, 5000)


def s_secret():
    seconds = 2.2
    out = silence(seconds)
    place(out, whisper(1.6, 8, 600, 2500, seed=55) * 0.5, 0)
    place(out, sweep(noise(0.6), lambda p: 3000 * (1 - p) + 200, 0.4) * env_adsr(n_of(0.6), 0.05, 0.1, 0, 0.4, 0.6) * 0.3, 1.4)
    place(out, thump(60, 0.3, 0.7, 0.06) * 0.4, 1.9)                         # a book closing
    return room(out, 1.8, 0.5, 2200)


def s_concealed_world():
    seconds = 3.2
    n = n_of(seconds)
    out = sweep(noise(seconds), lambda p: 3000 * (0.08 ** p) + 120, 0.6) * env_adsr(n, 0.4, 0.3, 0, 1.2, 0.7) * 0.5
    out += drone(seconds, 32.7, (1, 2, 2.5)) * env_adsr(n, 0.8, 0.3, 0, 1.2, 0.7) * 0.35
    place(out, thump(36, 1.0, 0.6, 0.3) * 0.7, 1.4)                          # the world shuts
    return room(out, 2.6, 0.45, 1800)


def s_mantle():
    seconds = 2.4
    n = n_of(seconds)
    out = wind(seconds, 300, 0.4) * 0.6
    out += drone(seconds, 41.2, (1, 1.5, 2)) * env_adsr(n, 0.5, 0.3, 0, 1.0, 0.8) * 0.4
    place(out, choir((D3, A3, D4, F4), 1.8, 'ah', 2, 0.4, 1.2, depth=0.003) * 0.25, 0.4)
    place(out, sub(1.8, 36, 0.2, 1.0) * 0.4, 0.3)
    return room(out, 2.2, 0.45, 2000)


def s_shadow_swing():
    seconds = 0.6
    out = silence(seconds)
    place(out, whoosh(0.45, 150, 1200, 0.55, 0.9) * 0.8, 0)
    place(out, lowpass(noise(0.3), 300) * env_adsr(n_of(0.3), 0.05, 0.1, 0, 0.15, 0.6) * 0.4, 0.15)
    return room(out, 0.7, 0.3, 2400)


def s_eternal_rest():
    seconds = 3.6
    n = n_of(seconds)
    out = silence(seconds)
    place(out, choir((D3, A3, D4, F4, A4), 3.2, 'oo', 2, 0.6, 2.0, depth=0.002) * 0.35, 0)
    place(out, bell(D4, 3.0, 0.4, (1.0, 2.0, 3.0, 4.2), 3.0) * 0.3, 0.2)       # a funeral bell, once
    place(out, heartbeat(0.9, 46) * 0.6, 0.6)
    place(out, heartbeat(0.9, 42) * 0.3, 1.8)                                # the last, fainter
    out += lowpass(noise(seconds), 200) * env_adsr(n, 1.0, 0.3, 0, 1.6, 0.5) * 0.1
    return room(out, 3.2, 0.55, 2000)


def s_destination():
    seconds = 4.4
    n = n_of(seconds)
    out = drone(seconds, 27.5, (1, 1.06, 1.5, 2)) * env_adsr(n, 1.0, 0.3, 0, 1.6, 0.9) * 0.5
    out += sweep(noise(seconds), lambda p: 3000 * (0.04 ** p) + 80, 0.7) * env_adsr(n, 0.8, 0.2, 0, 1.6, 0.8) * 0.5
    out += sub(seconds, 30, 1.0, 1.6) * 0.5
    place(out, choir((D3, F3, A3), 3.0, 'oh', 3, 1.2, 1.4, depth=0.02) * 0.15, 0.8)
    return room(out, 3.4, 0.5, 1600)


def s_dissolve():
    seconds = 1.2
    n = n_of(seconds)
    out = sweep(noise(seconds), lambda p: 2500 * (0.1 ** p) + 100, 0.5) * env_adsr(n, 0.02, 0.2, 0, 0.8, 0.7) * 0.6
    for i in range(14):
        place(out, bandpass(noise(0.02), 1500, 5000) * decay(n_of(0.02), 0.004) * RNG.uniform(0.2, 0.5), RNG.uniform(0, 0.8))
    return room(out, 1.0, 0.3, 2600)


# =====================================================================================================
# The list: event name -> the functions that make its variants
# =====================================================================================================

SOUNDS = {
    'spirit_vision': [s_spirit_vision],
    'dowsing': [s_dowsing],
    'poem': [lambda: s_poem(0), lambda: s_poem(1), lambda: s_poem(2)],
    'tranquilize': [s_tranquilize],
    'lullaby': [s_lullaby],
    'pacify': [s_pacify],
    'fear': [s_fear],
    'agitate': [s_agitate],
    'limbs': [s_limbs],
    'lash': [lambda: s_lash(0), lambda: s_lash(1)],
    'limbs_tear': [s_limbs_tear],
    'soul_out': [s_soul_out],
    'soul_return': [s_soul_return],
    'dream_enter': [s_dream_enter],
    'dream_shape': [s_dream_shape],
    'requiem': [s_requiem],
    'soothe': [s_soothe],
    'teeth': [s_teeth],
    'banshee': [s_banshee],
    'earth': [s_earth],
    'gale': [s_gale],
    'command_spirits': [s_command_spirits],
    'night_domain': [s_night_domain],
    'hair': [s_hair],
    'curse': [s_curse],
    'mishap': [lambda: s_mishap(0), lambda: s_mishap(1), lambda: s_mishap(2)],
    'catastrophe': [s_catastrophe],
    'wolf_form': [s_wolf_form],
    'wolf_howl': [s_wolf_howl],
    'wolf_bite': [s_wolf_bite],
    'wolf_step': [s_wolf_step],
    'dark_sword': [s_dark_sword],
    'dark_tide': [s_dark_tide],
    'edict_sleep': [lambda: s_edict(0)],
    'edict_despair': [lambda: s_edict(1)],
    'edict_destruction': [lambda: s_edict(2)],
    'nightmare_world': [s_nightmare_world],
    'horror': [s_horror],
    'conceal': [s_conceal],
    'reveal': [s_reveal],
    'secret': [s_secret],
    'concealed_world': [s_concealed_world],
    'mantle': [s_mantle],
    'shadow_swing': [s_shadow_swing],
    'eternal_rest': [s_eternal_rest],
    'destination': [s_destination],
    'dissolve': [s_dissolve],
}

SUBTITLES = {
    'spirit_vision': 'Spirit Vision opens', 'dowsing': 'A pendulum swings', 'poem': 'A Midnight Poem is whispered',
    'tranquilize': 'All goes still', 'lullaby': 'A lullaby plays', 'pacify': 'The will to live fades',
    'fear': 'Fear takes hold', 'agitate': 'Fury is stirred', 'limbs': 'Nightmare limbs tear free',
    'lash': 'A limb lashes', 'limbs_tear': 'A limb is torn off', 'soul_out': 'A soul leaves its body',
    'soul_return': 'A soul returns', 'dream_enter': 'A dream is entered', 'dream_shape': 'A dream bends',
    'requiem': 'A Requiem is sung', 'soothe': 'Souls are soothed', 'teeth': 'A spirit leaves the teeth',
    'banshee': 'A banshee wails', 'earth': 'Something moves through the earth', 'gale': 'A gale spirit lifts',
    'command_spirits': 'The spirits answer', 'night_domain': 'The night spreads', 'hair': 'Hair lashes out',
    'curse': 'Misfortune is bestowed', 'mishap': 'Something goes wrong', 'catastrophe': 'Catastrophe strikes',
    'wolf_form': 'A demonic wolf awakens', 'wolf_howl': 'A demonic wolf howls', 'wolf_bite': 'A wolf bites',
    'wolf_step': 'A wolf\'s step', 'dark_sword': 'The Sword of Darkness forms', 'dark_tide': 'A tide of Darkness surges',
    'edict_sleep': 'Eternal Sleep', 'edict_despair': 'Despair', 'edict_destruction': 'Destruction',
    'nightmare_world': 'A Nightmare World opens', 'horror': 'Horror', 'conceal': 'Something is concealed',
    'reveal': 'Something is revealed', 'secret': 'A secret is hidden', 'concealed_world': 'A Concealed World closes',
    'mantle': 'Darkness gathers', 'shadow_swing': 'Shadows strike', 'eternal_rest': 'Eternal Rest',
    'destination': 'The Destination of All Things opens', 'dissolve': 'Something dissolves into darkness',
}


def encode(samples, path):
    kit.encode(samples, path)


def main(assets, wav_dir=None):
    folder = os.path.join(assets, 'sounds/knight_of_misfortune')
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
            files.append({'name': f'circle_of_inevitability:knight_of_misfortune/{file}'})
            print(f'sounds/knight_of_misfortune/{file}.ogg  {len(samples) / SR:.2f} s')
        events['knight_of_misfortune.' + name] = {
            'subtitle': f'subtitles.circle_of_inevitability.knight_of_misfortune.{name}',
            'sounds': files,
        }
    with open(sounds_json, 'w') as f:
        json.dump(events, f, indent=2)
    lang_path = os.path.join(assets, 'lang/en_us.json')                # the subtitles
    if os.path.exists(lang_path):
        with open(lang_path, encoding='utf-8') as f:
            lang = json.load(f)
        for name, text in SUBTITLES.items():
            lang[f'subtitles.circle_of_inevitability.knight_of_misfortune.{name}'] = text
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
