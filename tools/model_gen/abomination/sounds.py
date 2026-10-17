"""Synthesizes the Abomination sounds and writes sounds.json.

python3 tools/model_gen/abomination/sounds.py src/main/resources/assets/circle_of_inevitability [--wav dir]

Every sound is built from scratch (sine partials, filtered noise, formant voices, a synthetic room), so nothing
here is copied from anywhere. The building blocks (tone, bell, choir, room, thump, clank, creak, drone, finish...)
are the ones in tools/model_gen/beauty_goddess/sounds.py, and whoosh, blow and rumble are borrowed from
../hand_of_god/sounds.py. Needs numpy, scipy and ffmpeg (with libvorbis).

The .ogg files go to sounds/abomination/, mono 44.1 kHz (Minecraft only places mono sounds in the world), and each
one gets an event "abomination.<name>" in sounds.json and a subtitle in lang/en_us.json. The same names are
registered in sounds/ModSounds.java (ABOMINATION_...): a new sound here needs a line there too.

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
    spec = importlib.util.spec_from_file_location(name, os.path.join(_here, '..', path))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


kit = _load('beauty_sounds', 'beauty_goddess/sounds.py')
hog = _load('hand_sounds', 'hand_of_god/sounds.py')

SR = kit.SR
RNG = np.random.default_rng(1729)
silence, noise, place, env_adsr, decay, fade = kit.silence, kit.noise, kit.place, kit.env_adsr, kit.decay, kit.fade
lowpass, highpass, bandpass, sweep, tone, glide = kit.lowpass, kit.highpass, kit.bandpass, kit.sweep, kit.tone, kit.glide
bell, choir, room, thump, heartbeat, finish = kit.bell, kit.choir, kit.room, kit.thump, kit.heartbeat, kit.finish
sparkle, drone, creak, clank = kit.sparkle, kit.drone, kit.creak, kit.clank
whoosh, blow, rumble = hog.whoosh, hog.blow, hog.rumble
FORMANTS = kit.FORMANTS


def n_of(seconds):
    return int(seconds * SR)


# =====================================================================================================
# Building blocks: a voice that glides (howls, groans, wails), chains, wet sounds, cracking, whispers
# =====================================================================================================

def cry(curve, seconds, vowel='oo', rough=0.0, breath=0.05):
    """A voice whose pitch follows 'curve' (an array of Hz as long as the sound): a howl, a groan, a wail.
    rough > 0 shakes the pitch (a growl, a sob)."""
    n = n_of(seconds)
    f = np.asarray(curve)[:n]
    if rough:
        f = f * (1 + rough * lowpass(RNG.standard_normal(n), 30, 1) * 8)
    phase = np.cumsum(2 * np.pi * f / SR)
    mean = float(np.mean(f))
    out = np.zeros(n)
    k = 1
    while mean * k < 5000:
        gain = sum(g / (1 + ((mean * k - fc) / bw) ** 2) for fc, bw, g in FORMANTS[vowel]) / k ** 0.5 + 0.003
        out += gain * np.sin(k * phase + RNG.uniform(0, 6.28))
        k += 1
    out /= max(1e-9, np.max(np.abs(out)))
    return out + bandpass(noise(seconds), 500, 3500) * breath


def links(seconds, count, lo=600, hi=1600, start=0.0):
    """A chain rattling: many small clanks of links knocking together."""
    out = silence(seconds)
    for _ in range(count):
        place(out, clank(RNG.uniform(lo, hi)) * RNG.uniform(0.1, 0.35), start + RNG.uniform(0, max(0.01, seconds - start - 0.3)))
    return out


def squelch(seconds=0.35, low=1600):
    """Something wet: a falling band of noise with a wobble."""
    n = n_of(seconds)
    x = sweep(noise(seconds), lambda p: low * (0.2 ** p), 0.8) * env_adsr(n, 0.005, 0.1, 0, seconds * 0.6, 0.6)
    return x * (1 + 0.5 * np.sin(2 * np.pi * 30 * np.arange(n) / SR))


def crackle(seconds, count, lo=800, hi=5000):
    """Bones, ice or dry thorns cracking: short clicks of noise scattered over the sound."""
    out = silence(seconds)
    for _ in range(count):
        d = RNG.uniform(0.003, 0.02)
        place(out, bandpass(noise(d), lo, hi) * decay(n_of(d), d / 3) * RNG.uniform(0.2, 1), RNG.uniform(0, seconds - 0.03))
    return out


def whisper(seconds, rate=13, lo=1500, hi=5500):
    """Hissing, unvoiced words: noise in the voice's band, chopped into syllables."""
    n = n_of(seconds)
    t = np.arange(n) / SR
    syll = np.clip(np.sin(2 * np.pi * rate * t + 2 * np.sin(2 * np.pi * 1.7 * t)), 0, 1) ** 2
    return sweep(noise(seconds), lambda p: (lo + hi) / 2 + (hi - lo) / 2 * np.sin(p * 37), 0.4) * syll


def toll(freq=98.0, seconds=4.0):
    """A great funeral bell, low and slow to die."""
    return bell(freq, seconds, 0.5, (0.5, 1.0, 1.19, 1.5, 2.0, 2.51, 3.0, 4.1), seconds * 0.8)


def wood_slam(weight=1.0):
    """A heavy wooden lid slammed: a dull knock with the body of a box ringing under it."""
    out = silence(0.6)
    place(out, thump(70, 0.4, 0.5, 0.12) * weight, 0)
    body = sum(a * tone(f, 0.3) for f, a in ((180, 1.0), (310, 0.6), (520, 0.3))) * decay(n_of(0.3), 0.06)
    place(out, body * 0.6 * weight, 0)
    place(out, bandpass(noise(0.05), 300, 2500) * decay(n_of(0.05), 0.01) * 0.8, 0)
    return out


def rip(seconds=0.25, hi=6000):
    """Flesh or cloth torn: a fast falling band of rough noise."""
    n = n_of(seconds)
    x = sweep(noise(seconds), lambda p: hi * (0.15 ** p), 0.6) * decay(n, seconds * 0.4)
    return x * (1 + 0.7 * np.sign(np.sin(2 * np.pi * 90 * np.arange(n) / SR)))


# =====================================================================================================
# Prisoner: Blood Chain, Restraint
# =====================================================================================================

def s_chain_whip():
    seconds = 1.0
    out = silence(seconds)
    place(out, whoosh(0.35, 500, 3200, 0.6, 0.6) * 0.7, 0)
    place(out, links(0.6, 18, 700, 1700), 0.05)
    place(out, squelch(0.3, 1200) * 0.5, 0.0)
    place(out, clank(560) * 0.7, 0.34)
    return room(out, 0.8, 0.25, 4000)


def s_chain_bind():
    seconds = 1.3
    out = silence(seconds)
    place(out, links(0.8, 24, 500, 1300), 0)
    place(out, squelch(0.4, 900) * 0.6, 0.15)
    for i in range(3):                                                       # pulled taut, three tugs
        place(out, clank(380 + i * 40) * 0.6, 0.45 + i * 0.18)
        place(out, thump(60, 0.2, 0.7, 0.05) * 0.5, 0.45 + i * 0.18)
    return room(out, 1.0, 0.3, 3500)


def s_bleed():
    seconds = 0.6
    out = silence(seconds)
    for i in range(3):                                                       # drops on the ground
        f0 = RNG.uniform(700, 1100)
        d = 0.06
        place(out, tone(glide(f0, f0 * 2.2, d, 0.5), d) * decay(n_of(d), 0.02) * (0.6 - i * 0.15), i * 0.14)
    place(out, squelch(0.2, 800) * 0.3, 0)
    return room(out, 0.5, 0.2, 4000)


def s_restraint():
    seconds = 1.8
    out = silence(seconds)
    place(out, links(1.1, 30, 450, 1200), 0)                                  # winding round the body
    place(out, sweep(noise(1.0), lambda p: 300 + 900 * p, 0.6) * env_adsr(n_of(1.0), 0.3, 0.2, 0, 0.4, 0.6) * 0.3, 0)
    place(out, clank(900) * 0.8, 1.1)                                         # the padlock snaps shut
    place(out, highpass(noise(0.02), 3000) * 0.7, 1.1)
    place(out, thump(52, 0.4, 0.6, 0.12) * 0.7, 1.12)
    return room(out, 1.2, 0.3, 3500)


def s_unchain():
    seconds = 1.4
    out = silence(seconds)
    place(out, clank(1200) * 0.5, 0)                                          # the lock opens
    place(out, links(0.9, 26, 400, 1100), 0.08)
    for i in range(4):                                                        # coils falling to the ground
        place(out, thump(70, 0.15, 0.7, 0.04) * 0.4, 0.4 + i * 0.12)
    return room(out, 1.0, 0.25, 3500)


# =====================================================================================================
# Lunatic: Moonstruck, Lunar Frenzy, madness
# =====================================================================================================

def s_moonstruck():
    seconds = 2.6
    n = n_of(seconds)
    out = silence(seconds)
    warble = (tone(glide(523.25, 493.88, seconds), seconds) + 0.6 * tone(glide(554.37, 587.33, seconds), seconds))
    place(out, warble * env_adsr(n, 0.8, 0.3, 0, 1.0, 0.6) * 0.15, 0)            # two notes drifting out of tune
    place(out, whisper(2.2, 11) * env_adsr(n_of(2.2), 0.4, 0.3, 0, 0.8, 0.7) * 0.35, 0.2)
    place(out, bell(1318.5, 1.6, 0.8, (1.0, 2.3, 3.7), 1.2) * 0.25, 0.1)
    place(out, bell(1244.5, 1.6, 0.8, (1.0, 2.3, 3.7), 1.2) * 0.2, 0.55)
    place(out, choir((261.63, 277.18), 1.8, 'ee', 2, 0.6, 1.0, depth=0.02) * 0.12, 0.4)
    return room(out, 2.2, 0.45, 4500)


def s_madness():
    seconds = 1.4
    out = silence(seconds)
    t = 0.0
    while t < 1.1:                                                          # a giggle that will not stop
        d = RNG.uniform(0.07, 0.12)
        f0 = RNG.uniform(320, 460)
        place(out, cry(glide(f0, f0 * 0.8, d), d, 'ee', 0.002, 0.15) * env_adsr(n_of(d), 0.01, 0.02, 0, d * 0.5, 0.7) * 0.4, t)
        t += d + RNG.uniform(0.02, 0.06)
    return room(out, 1.4, 0.45, 3500)


def s_frenzy():
    seconds = 2.6
    n = n_of(seconds)
    out = silence(seconds)
    t, gap = 0.0, 0.55
    while t < 1.6:                                                          # the heart racing
        place(out, heartbeat(0.5, 50) * 0.8, t)
        t += gap
        gap = max(0.22, gap * 0.8)
    growl = cry(glide(90, 120, 1.4), 1.4, 'oh', 0.004, 0.2) * env_adsr(n_of(1.4), 0.3, 0.2, 0, 0.6, 0.8)
    place(out, lowpass(growl, 1800) * 0.5, 0.6)
    place(out, cry(glide(330, 520, 1.0, 0.6), 1.0, 'oo', 0.0, 0.05) * env_adsr(n_of(1.0), 0.2, 0.2, 0, 0.5, 0.7) * 0.35, 1.5)
    return room(out, 1.6, 0.35, 3500)


def s_frenzy_crash():
    seconds = 2.2
    out = silence(seconds)
    t, gap = 0.0, 0.3
    for i in range(5):                                                      # slowing, weaker
        place(out, heartbeat(0.6, 50 - i * 2) * (0.8 - i * 0.12), t)
        t += gap
        gap *= 1.35
    sigh = sweep(noise(1.4), lambda p: 1400 * (0.3 ** p), 0.6) * env_adsr(n_of(1.4), 0.1, 0.2, 0, 1.0, 0.6) * 0.35
    place(out, sigh, 0.1)
    place(out, tone(glide(220, 110, 1.4, 0.6), 1.4) * env_adsr(n_of(1.4), 0.1, 0.2, 0, 1.0, 0.5) * 0.15, 0.2)
    return room(out, 1.4, 0.35, 2500)


# =====================================================================================================
# Werewolf: Rending Pounce, the claws, the form, the howl
# =====================================================================================================

def snarl(seconds=0.6):
    x = cry(glide(110, 150, seconds, 0.5), seconds, 'ah', 0.006, 0.3) * env_adsr(n_of(seconds), 0.02, 0.1, 0, 0.3, 0.8)
    return lowpass(x * (1 + 0.6 * np.sin(2 * np.pi * 38 * np.arange(n_of(seconds)) / SR)), 2500)


def s_pounce():
    seconds = 1.0
    out = silence(seconds)
    place(out, thump(60, 0.2, 0.7, 0.05) * 0.6, 0)                            # the push off
    place(out, whoosh(0.5, 300, 2400, 0.5, 0.7) * 0.7, 0.03)
    place(out, snarl(0.55) * 0.7, 0.2)
    place(out, blow(1.0) * 0.8, 0.55)
    return room(out, 0.8, 0.25, 3000)


def s_claw(variant):
    seconds = 0.5
    out = silence(seconds)
    for i in range(3):                                                       # three claws, one after the other
        place(out, rip(0.18, 6500 - variant * 800) * (0.6 - i * 0.1), i * 0.025)
    place(out, squelch(0.25, 1400) * 0.4, 0.04)
    place(out, blow(0.6) * 0.4, 0.03)
    return room(out, 0.4, 0.15, 4500)


def s_werewolf_form():
    seconds = 3.4
    n = n_of(seconds)
    out = silence(seconds)
    place(out, crackle(1.6, 60, 300, 3000) * 0.9, 0)                           # bones breaking and setting
    for i in range(8):
        place(out, creak(0.2, 30, 80) * 0.25, 0.1 + i * 0.16)
    growl = cry(glide(70, 110, 1.6), 1.6, 'oh', 0.006, 0.25) * env_adsr(n_of(1.6), 0.4, 0.2, 0, 0.5, 0.8)
    place(out, lowpass(growl, 1500) * 0.6, 0.6)
    place(out, s_howl_raw(1.6) * 0.6, 1.7)
    place(out, rumble(seconds, 34, 6) * env_adsr(n, 0.5, 0.3, 0, 1.0, 0.5) * 0.2, 0)
    return room(out, 2.0, 0.35, 3000)


def s_howl_raw(seconds=2.6):
    curve = np.concatenate([glide(300, 560, seconds * 0.25, 0.6), glide(560, 600, seconds * 0.45),
                            glide(600, 380, seconds * 0.3 + 0.01, 1.4)])
    x = cry(curve, seconds, 'oo', 0.0006, 0.04)
    return x * env_adsr(n_of(seconds), 0.15, 0.2, 0, seconds * 0.4, 0.85)


def s_howl():
    seconds = 3.0
    out = silence(seconds)
    place(out, s_howl_raw(2.6) * 0.8, 0)
    place(out, s_howl_raw(2.2) * 0.3, 0.5)                                    # an echo of another wolf, far off
    return room(out, 2.6, 0.5, 3500)


# =====================================================================================================
# Zombie: Raise the Dead, Grave Chill, Undying Flesh, Death Sentence
# =====================================================================================================

def s_groan(variant):
    seconds = 1.6
    f0 = (95, 82)[variant]
    curve = np.concatenate([glide(f0, f0 * 1.25, 0.6), glide(f0 * 1.25, f0 * 0.8, 1.0 + 0.01, 1.3)])
    x = cry(curve, seconds, ('oh', 'ah')[variant], 0.004, 0.15) * env_adsr(n_of(seconds), 0.2, 0.2, 0, 0.7, 0.8)
    return room(lowpass(x, 2000) * 0.8, 1.0, 0.3, 2500)


def s_raise_dead():
    seconds = 3.0
    n = n_of(seconds)
    out = silence(seconds)
    place(out, rumble(1.6, 32, 9) * env_adsr(n_of(1.6), 0.2, 0.3, 0, 0.8, 0.6) * 0.6, 0)
    place(out, crackle(1.2, 70, 200, 1800) * 0.7, 0.2)                         # earth breaking open
    for i in range(3):
        place(out, lowpass(noise(0.3), 800) * decay(n_of(0.3), 0.08) * 0.5, 0.4 + i * 0.25)
    for i, f0 in enumerate((88, 104, 76)):                                    # three groans
        curve = glide(f0, f0 * 1.2, 1.2)
        g = cry(curve, 1.2, 'oh', 0.004, 0.12) * env_adsr(n_of(1.2), 0.3, 0.2, 0, 0.6, 0.8)
        place(out, lowpass(g, 1800) * 0.35, 1.0 + i * 0.35)
    place(out, choir((65.41, 69.3), 2.0, 'oo', 2, 0.6, 1.0, depth=0.0) * 0.1, 0.8)
    return room(out, 2.0, 0.35, 2500)


def s_grave_chill():
    seconds = 3.2
    n = n_of(seconds)
    out = silence(seconds)
    wind = sweep(noise(seconds), lambda p: 500 + 600 * p + 300 * np.sin(p * 13), 0.4) * env_adsr(n, 0.8, 0.3, 0, 1.2, 0.8)
    place(out, wind * 0.6, 0)
    place(out, crackle(seconds, 90, 3000, 9000) * np.linspace(0.2, 1, n) * 0.5, 0)       # frost spreading
    place(out, choir((196.0, 207.65), 2.4, 'oo', 2, 1.0, 1.0, depth=0.0) * 0.12, 0.3)
    place(out, sparkle(seconds, 16, 5000, 11000, 0.4, 2.8) * 0.12, 0)
    return room(out, 2.4, 0.45, 6000)


def s_freeze():
    seconds = 0.9
    out = silence(seconds)
    place(out, crackle(0.3, 40, 2500, 9000), 0)
    place(out, bell(2093, 0.8, 1.0, (1.0, 2.76, 5.4), 0.6) * 0.3, 0.2)
    place(out, highpass(noise(0.03), 2000) * 0.6, 0.22)
    return room(out, 0.8, 0.3, 7000)


def s_undying():
    seconds = 2.6
    n = n_of(seconds)
    out = silence(seconds)
    place(out, heartbeat(0.9, 46), 0)
    place(out, thump(40, 0.9, 0.6, 0.35) * 1.1, 0.9)                          # one last heavy beat
    flat = (tone(55, 1.6) + 0.4 * tone(110.4, 1.6)) * env_adsr(n_of(1.6), 0.2, 0.2, 0, 0.8, 0.6) * 0.3
    place(out, flat, 1.0)
    place(out, sweep(noise(1.6), lambda p: 300 + 200 * p, 0.5) * env_adsr(n_of(1.6), 0.4, 0.2, 0, 0.8, 0.5) * 0.25, 0.9)
    return room(out, 1.6, 0.35, 2500)


def s_sentence():
    seconds = 4.0
    out = silence(seconds)
    place(out, toll(98.0, 3.8) * 0.7, 0)
    place(out, choir((49.0, 73.42, 98.0), 2.8, 'oo', 2, 0.4, 1.4, depth=0.0) * 0.15, 0.1)
    place(out, whisper(1.6, 9) * env_adsr(n_of(1.6), 0.4, 0.2, 0, 0.8, 0.6) * 0.15, 0.4)
    return room(out, 3.0, 0.5, 3000)


def s_sentence_tick():
    seconds = 0.8
    out = silence(seconds)
    place(out, wood_slam(0.5) * 0.7, 0)
    place(out, bell(196.0, 0.7, 0.3, (1.0, 2.0, 3.01), 0.6) * 0.3, 0)
    return room(out, 0.8, 0.35, 2500)


def s_sentence_fall():
    seconds = 3.6
    n = n_of(seconds)
    out = silence(seconds)
    place(out, toll(73.42, 3.4) * 0.8, 0)
    place(out, blow(1.6), 0)
    place(out, rumble(1.6, 30, 6) * decay(n_of(1.6), 0.5) * 0.5, 0)
    place(out, choir((73.42, 77.78, 110.0), 2.6, 'ah', 2, 0.1, 1.6, depth=0.01) * 0.2, 0.05)
    return room(out, 2.6, 0.45, 3000)


# =====================================================================================================
# Wraith: Mirror Step, Possession, Ghost Host
# =====================================================================================================

def s_mirror_enter():
    seconds = 1.4
    n = n_of(seconds)
    out = silence(seconds)
    place(out, sweep(noise(0.8), lambda p: 7000 * (0.15 ** p), 0.5) * env_adsr(n_of(0.8), 0.05, 0.2, 0, 0.5, 0.6) * 0.5, 0)
    for i, f in enumerate((2637.0, 2349.3, 1975.5, 1568.0)):                   # a glassy fall
        place(out, bell(f, 0.9, 0.9, (1.0, 2.0, 3.0), 0.7) * 0.2, i * 0.07)
    place(out, tone(glide(880, 220, 0.8, 0.6), 0.8) * env_adsr(n_of(0.8), 0.05, 0.2, 0, 0.5, 0.5) * 0.15, 0.1)
    return room(out, 1.4, 0.45, 7000)


def s_mirror_exit():
    seconds = 1.2
    out = silence(seconds)
    place(out, sweep(noise(0.6), lambda p: 600 + 6000 * p ** 2, 0.5) * np.linspace(0, 1, n_of(0.6)) ** 2 * 0.5, 0)
    for i, f in enumerate((1568.0, 1975.5, 2637.0)):
        place(out, bell(f, 0.8, 0.9, (1.0, 2.0, 3.0), 0.6) * 0.2, 0.5 + i * 0.05)
    place(out, highpass(noise(0.05), 3000) * decay(n_of(0.05), 0.01) * 0.6, 0.55)
    return room(out, 1.2, 0.4, 7000)


def s_ghost_wail():
    seconds = 2.2
    curve = np.concatenate([glide(400, 640, 0.8, 0.6), glide(640, 300, 1.4 + 0.01, 1.2)])
    x = cry(curve, seconds, 'oo', 0.0015, 0.12) * env_adsr(n_of(seconds), 0.3, 0.2, 0, 1.0, 0.8)
    return room(x * 0.7, 2.0, 0.55, 4000)


def s_possession():
    seconds = 2.0
    out = silence(seconds)
    place(out, whoosh(0.8, 200, 3000, 0.75, 0.8) * 0.7, 0)                     # the dive
    place(out, s_ghost_wail()[:n_of(0.9)] * np.linspace(1, 0, n_of(0.9)) * 0.4, 0)
    muffled = lowpass(heartbeat(0.9, 46) + heartbeat(0.9, 46), 300)
    place(out, muffled * 0.8, 0.8)                                            # inside: only its heart
    place(out, lowpass(noise(1.2), 300) * env_adsr(n_of(1.2), 0.2, 0.2, 0, 0.6, 0.5) * 0.3, 0.8)
    return room(out, 1.4, 0.35, 2500)


def s_possession_exit():
    seconds = 1.2
    out = silence(seconds)
    place(out, sweep(noise(0.8), lambda p: 300 + 3000 * p, 0.6) * env_adsr(n_of(0.8), 0.1, 0.2, 0, 0.4, 0.7) * 0.5, 0)
    place(out, sparkle(0.8, 8, 3000, 8000, 0.1, 0.7) * 0.2, 0.2)
    place(out, sweep(noise(0.5), lambda p: 1500 * (0.4 ** p), 0.5) * decay(n_of(0.5), 0.2) * 0.4, 0.5)   # a breath out
    return room(out, 1.0, 0.35, 4000)


def s_ghost_host():
    seconds = 3.4
    out = silence(seconds)
    place(out, sweep(noise(1.0), lambda p: 200 + 1000 * p, 0.6) * env_adsr(n_of(1.0), 0.6, 0.2, 0, 0.3, 0.7) * 0.35, 0)
    for i, f0 in enumerate((360, 450, 300)):                                    # three of them rise
        curve = np.concatenate([glide(f0, f0 * 1.5, 0.7, 0.6), glide(f0 * 1.5, f0 * 0.8, 1.3 + 0.01, 1.2)])
        w = cry(curve, 2.0, 'oo', 0.0015, 0.1) * env_adsr(n_of(2.0), 0.3, 0.2, 0, 0.9, 0.8)
        place(out, w * 0.35, 0.5 + i * 0.35)
    place(out, choir((146.83, 155.56), 2.4, 'oo', 2, 0.8, 1.2, depth=0.01) * 0.1, 0.4)
    return room(out, 2.4, 0.5, 4000)


# =====================================================================================================
# Puppet: Animate Objects, Grave Shackles
# =====================================================================================================

def s_animate():
    seconds = 2.0
    n = n_of(seconds)
    out = silence(seconds)
    for i in range(12):                                                       # things knocking as they lift
        place(out, clank(RNG.uniform(300, 1200)) * RNG.uniform(0.15, 0.35), RNG.uniform(0, 0.9))
        place(out, wood_slam(0.3) * RNG.uniform(0.1, 0.3), RNG.uniform(0, 0.9))
    place(out, sweep(noise(seconds), lambda p: 300 + 900 * p, 0.5) * env_adsr(n, 0.5, 0.3, 0, 0.8, 0.6) * 0.3, 0)
    for i in range(4):                                                         # taut strings
        place(out, kit.harp(RNG.uniform(180, 260), 1.0) * 0.18, 0.3 + i * 0.2)
    return room(out, 1.4, 0.35, 4000)


def s_hurl():
    seconds = 0.6
    out = silence(seconds)
    place(out, whoosh(0.45, 600, 4000, 0.55, 0.5) * 0.9, 0)
    return room(out, 0.4, 0.15, 5000)


def s_shackles():
    seconds = 2.0
    out = silence(seconds)
    place(out, rumble(0.8, 32, 12) * decay(n_of(0.8), 0.3) * 0.5, 0)
    place(out, crackle(0.5, 40, 200, 1600) * 0.6, 0)                          # out of the earth
    place(out, links(1.0, 34, 400, 1200), 0.15)
    for i in range(3):                                                         # cuffs closing
        place(out, clank(360 + i * 60) * 0.8, 0.85 + i * 0.12)
        place(out, thump(50, 0.3, 0.6, 0.1) * 0.6, 0.85 + i * 0.12)
    return room(out, 1.6, 0.35, 3000)


def s_pain_link():
    seconds = 1.0
    out = silence(seconds)
    twang = sum(a * tone(f, 0.8) for f, a in ((147, 1.0), (294.6, 0.5), (442, 0.3))) * decay(n_of(0.8), 0.25)
    place(out, twang * 0.5, 0)
    place(out, lowpass(blow(0.7), 900) * 0.6, 0)
    place(out, clank(700) * 0.3, 0.01)
    return room(out, 0.8, 0.3, 3000)


# =====================================================================================================
# Disciple of Silence: curses, the relic, the Rite of Silence
# =====================================================================================================

def s_curse():
    seconds = 2.4
    n = n_of(seconds)
    out = silence(seconds)
    place(out, whisper(2.0, 12, 1200, 4500) * env_adsr(n_of(2.0), 0.3, 0.2, 0, 0.8, 0.8) * 0.5, 0)
    place(out, drone(seconds, 41.2, (1, 1.06, 1.5), 0.6) * env_adsr(n, 0.5, 0.3, 0, 1.0, 0.7) * 0.4, 0)
    place(out, bell(622.25, 1.6, 0.5, (1.0, 2.4, 4.1), 1.2) * 0.25, 0.9)
    place(out, bell(587.33, 1.6, 0.5, (1.0, 2.4, 4.1), 1.2) * 0.2, 0.95)
    return room(out, 2.0, 0.4, 3500)


def s_relic_set():
    seconds = 1.2
    out = silence(seconds)
    place(out, wood_slam(0.4) * 0.5, 0)
    place(out, bell(1046.5, 1.0, 0.6, (1.0, 2.4, 3.9), 0.8) * 0.3, 0.05)
    place(out, bell(987.77, 1.0, 0.6, (1.0, 2.4, 3.9), 0.8) * 0.25, 0.12)
    place(out, whisper(0.6, 14) * env_adsr(n_of(0.6), 0.1, 0.1, 0, 0.3, 0.6) * 0.2, 0.2)
    return room(out, 1.2, 0.4, 4000)


def s_relic_trigger():
    seconds = 1.8
    out = silence(seconds)
    sting = (tone(932.33, 0.8) + tone(987.77, 0.8) + 0.5 * tone(1396.9, 0.8)) * decay(n_of(0.8), 0.3)
    place(out, sting * 0.35, 0)
    place(out, highpass(noise(0.04), 2500) * 0.6, 0)
    place(out, whisper(1.4, 15) * env_adsr(n_of(1.4), 0.05, 0.2, 0, 0.7, 0.8) * 0.45, 0.05)
    place(out, s_madness()[:n_of(1.0)] * 0.3, 0.3)
    return room(out, 1.6, 0.4, 4000)


def s_silence():
    seconds = 2.6
    n = n_of(seconds)
    out = silence(seconds)
    shh = bandpass(noise(0.9), 2500, 7000) * env_adsr(n_of(0.9), 0.3, 0.1, 0, 0.5, 0.8)
    place(out, shh * 0.6, 0)                                                   # a long "hush"
    hollow = lowpass(noise(1.8), 180, 2) * env_adsr(n_of(1.8), 0.2, 0.2, 0, 1.2, 0.6)
    place(out, hollow * 0.7, 0.7)                                              # then the world goes dull
    place(out, tone(glide(220, 180, 1.8), 1.8) * env_adsr(n_of(1.8), 0.4, 0.2, 0, 1.0, 0.4) * 0.08, 0.7)
    return room(out, 2.2, 0.3, 1500)


# =====================================================================================================
# Ancient Bane: Bane Mist, Ancient Blight
# =====================================================================================================

def s_bane_mist():
    seconds = 4.2
    n = n_of(seconds)
    out = silence(seconds)
    roll = sweep(noise(seconds), lambda p: 250 + 150 * np.sin(p * 7), 0.6) * env_adsr(n, 1.2, 0.3, 0, 1.4, 0.8)
    place(out, roll * 0.7, 0)
    place(out, whisper(3.2, 10, 1200, 4000) * env_adsr(n_of(3.2), 1.0, 0.2, 0, 1.0, 0.7) * 0.3, 0.4)
    place(out, s_howl_raw(2.0) * 0.12, 1.6)                                     # something far off in it
    place(out, drone(seconds, 36.7, (1, 1.06, 1.5, 2.12), 0.3) * env_adsr(n, 1.4, 0.3, 0, 1.4, 0.7) * 0.35, 0)
    return room(out, 3.0, 0.5, 2500)


def s_blight_jump():
    seconds = 1.2
    out = silence(seconds)
    place(out, squelch(0.4, 1100) * 0.6, 0)
    place(out, whoosh(0.4, 300, 1800, 0.5, 0.6) * 0.4, 0.05)
    chord = (tone(311.13, 0.9) + tone(329.63, 0.9) + 0.6 * tone(466.16, 0.9)) * decay(n_of(0.9), 0.35)
    place(out, chord * 0.25, 0.25)
    return room(out, 1.0, 0.35, 3000)


# =====================================================================================================
# Abomination: the vessel and the calamity
# =====================================================================================================

def s_abomination_form():
    seconds = 4.6
    n = n_of(seconds)
    out = silence(seconds)
    for i in range(10):                                                        # thorns tightening round cloth
        place(out, creak(0.25, 25, 70) * 0.3, 0.1 + i * 0.12)
    place(out, rip(0.4, 4000) * 0.5, 0.6)
    place(out, rip(0.3, 3500) * 0.4, 0.9)
    place(out, crackle(1.4, 50, 600, 4000) * 0.5, 0.3)
    place(out, choir((61.74, 65.41, 92.5, 130.81, 138.59), 3.4, 'ah', 2, 1.0, 1.6, depth=0.015) * 0.25, 1.0)
    place(out, whisper(3.0, 14, 1200, 5000) * env_adsr(n_of(3.0), 0.8, 0.2, 0, 1.0, 0.7) * 0.25, 1.2)
    place(out, rumble(seconds, 30, 5) * env_adsr(n, 1.0, 0.3, 0, 1.4, 0.6) * 0.3, 0)
    return room(out, 3.0, 0.5, 3000)


def s_calamity():
    seconds = 4.4
    n = n_of(seconds)
    out = silence(seconds)
    place(out, blow(2.0) * 1.2, 0)
    place(out, rumble(3.0, 28, 4) * decay(n_of(3.0), 1.0) * 0.8, 0)
    place(out, toll(65.41, 3.6) * 0.6, 0.02)
    place(out, choir((65.41, 69.3, 98.0, 103.83), 3.2, 'ah', 2, 0.05, 2.0, depth=0.02) * 0.25, 0.02)
    place(out, crackle(2.0, 80, 300, 3000) * env_adsr(n_of(2.0), 0.05, 0.2, 0, 1.4, 0.6) * 0.4, 0.1)
    return room(out, 3.0, 0.5, 2500)


# =====================================================================================================
# Chained: the Mausoleum, the Coffin, the Thorn Garden
# =====================================================================================================

def s_thorn_burst():
    seconds = 1.4
    out = silence(seconds)
    place(out, thump(48, 0.4, 0.6, 0.12) * 0.8, 0)
    place(out, crackle(0.6, 60, 400, 3500) * 0.9, 0)
    for i in range(6):
        place(out, creak(0.15, 40, 90) * 0.3, 0.05 + i * 0.07)
    place(out, rip(0.3, 3000) * 0.4, 0.05)
    return room(out, 1.2, 0.3, 3000)


def s_thorn_hit():
    seconds = 0.45
    out = silence(seconds)
    place(out, rip(0.12, 7000) * 0.6, 0)
    place(out, squelch(0.2, 1500) * 0.4, 0.02)
    place(out, crackle(0.1, 8, 1500, 5000) * 0.5, 0)
    return room(out, 0.4, 0.15, 5000)


def s_mausoleum():
    seconds = 4.4
    n = n_of(seconds)
    out = silence(seconds)
    for i in range(18):                                                       # the vines climbing round
        place(out, creak(0.3, 20, 60) * 0.25, i * 0.13)
    place(out, crackle(2.4, 120, 400, 3500) * 0.5, 0)
    for i in range(5):                                                         # bandages snapping taut
        place(out, rip(0.15, 2500) * 0.3, 0.6 + i * 0.3)
    place(out, choir((55.0, 82.41, 87.31, 110.0), 3.0, 'oo', 2, 1.2, 1.4, depth=0.0) * 0.2, 1.0)
    place(out, toll(55.0, 2.8) * 0.5, 2.2)                                   # sealed
    place(out, wood_slam(1.2), 2.2)
    return room(out, 3.0, 0.5, 2500)


def s_coffin_close():
    seconds = 2.6
    out = silence(seconds)
    place(out, creak(0.7, 15, 40) * 0.6, 0)                                    # the lid swings
    place(out, wood_slam(1.4), 0.7)
    place(out, links(0.7, 22, 400, 1100), 0.8)                                 # chains round it
    place(out, clank(720) * 0.8, 1.5)                                          # locked
    place(out, toll(87.31, 1.8) * 0.35, 0.7)
    return room(out, 1.8, 0.4, 2500)


def s_coffin_open():
    seconds = 2.2
    out = silence(seconds)
    place(out, clank(900) * 0.5, 0)
    place(out, links(0.5, 14, 400, 1000), 0.05)
    place(out, creak(0.9, 12, 35) * 0.6, 0.4)
    place(out, wood_slam(0.8) * 0.7, 1.3)                                     # the lid falls open
    gasp = sweep(noise(0.6), lambda p: 600 + 2000 * p, 0.6) * env_adsr(n_of(0.6), 0.25, 0.1, 0, 0.25, 0.7)
    place(out, gasp * 0.4, 1.4)
    return room(out, 1.6, 0.35, 3000)


def s_thorn_garden():
    seconds = 4.0
    n = n_of(seconds)
    out = silence(seconds)
    for i in range(14):
        place(out, creak(0.3, 25, 70) * 0.2, i * 0.15)
    place(out, crackle(2.2, 60, 400, 3500) * 0.4, 0)
    for i, f in enumerate((783.99, 932.33, 1174.7, 1396.9, 1567.98)):          # roses opening, soft bells
        place(out, bell(f, 1.8, 0.6, (1.0, 2.0, 3.0, 4.07), 1.4) * 0.15, 1.0 + i * 0.3)
    place(out, choir((196.0, 233.08, 293.66), 2.4, 'ah', 2, 1.0, 1.2, depth=0.006) * 0.12, 1.2)
    place(out, sweep(noise(seconds), lambda p: 800 + 2000 * p, 0.5) * env_adsr(n, 1.0, 0.3, 0, 1.2, 0.5) * 0.1, 0)
    return room(out, 2.6, 0.45, 5000)


def s_rose_heal():
    seconds = 1.2
    out = silence(seconds)
    for i, f in enumerate((1174.7, 1396.9, 1760.0)):
        place(out, bell(f, 1.0, 0.6, (1.0, 2.0, 3.0), 0.9) * 0.2, i * 0.08)
    place(out, sparkle(1.0, 8, 4000, 9000, 0.0, 0.8) * 0.15, 0)
    return room(out, 1.2, 0.45, 7000)


# =====================================================================================================
# The list: event name -> the functions that make its variants
# =====================================================================================================

SOUNDS = {
    'chain_whip': [s_chain_whip],
    'chain_bind': [s_chain_bind],
    'bleed': [s_bleed],
    'restraint': [s_restraint],
    'unchain': [s_unchain],
    'moonstruck': [s_moonstruck],
    'madness': [s_madness],
    'frenzy': [s_frenzy],
    'frenzy_crash': [s_frenzy_crash],
    'pounce': [s_pounce],
    'claw': [lambda: s_claw(0), lambda: s_claw(1)],
    'werewolf_form': [s_werewolf_form],
    'howl': [s_howl],
    'raise_dead': [s_raise_dead],
    'groan': [lambda: s_groan(0), lambda: s_groan(1)],
    'grave_chill': [s_grave_chill],
    'freeze': [s_freeze],
    'undying': [s_undying],
    'sentence': [s_sentence],
    'sentence_tick': [s_sentence_tick],
    'sentence_fall': [s_sentence_fall],
    'mirror_enter': [s_mirror_enter],
    'mirror_exit': [s_mirror_exit],
    'possession': [s_possession],
    'possession_exit': [s_possession_exit],
    'ghost_host': [s_ghost_host],
    'ghost_wail': [s_ghost_wail],
    'animate': [s_animate],
    'hurl': [s_hurl],
    'shackles': [s_shackles],
    'pain_link': [s_pain_link],
    'curse': [s_curse],
    'relic_set': [s_relic_set],
    'relic_trigger': [s_relic_trigger],
    'silence': [s_silence],
    'bane_mist': [s_bane_mist],
    'blight_jump': [s_blight_jump],
    'abomination_form': [s_abomination_form],
    'calamity': [s_calamity],
    'thorn_burst': [s_thorn_burst],
    'thorn_hit': [s_thorn_hit],
    'mausoleum': [s_mausoleum],
    'coffin_close': [s_coffin_close],
    'coffin_open': [s_coffin_open],
    'thorn_garden': [s_thorn_garden],
    'rose_heal': [s_rose_heal],
}

SUBTITLES = {
    'chain_whip': 'A chain of blood lashes out', 'chain_bind': 'Chains bind tight', 'bleed': 'Blood drips',
    'restraint': 'Chains lock round a body', 'unchain': 'Chains fall away', 'moonstruck': 'A false moon stares',
    'madness': 'Mad laughter', 'frenzy': 'A heart races in frenzy', 'frenzy_crash': 'The frenzy fades',
    'pounce': 'A werewolf pounces', 'claw': 'Claws rend', 'werewolf_form': 'Bones twist into a wolf',
    'howl': 'A werewolf howls', 'raise_dead': 'The dead claw out of the ground', 'groan': 'A zombie groans',
    'grave_chill': 'A grave chill spreads', 'freeze': 'Ice locks a body', 'undying': 'A heart stops, yet goes on',
    'sentence': 'A death knell tolls', 'sentence_tick': 'The sentence counts down',
    'sentence_fall': 'A death sentence falls', 'mirror_enter': 'Someone steps into a reflection',
    'mirror_exit': 'Someone steps out of a reflection', 'possession': 'A spirit takes a body',
    'possession_exit': 'A spirit leaves a body', 'ghost_host': 'Ghosts rise', 'ghost_wail': 'A ghost wails',
    'animate': 'Objects rise on strings', 'hurl': 'Something is hurled', 'shackles': 'Grave shackles close',
    'pain_link': 'Shared pain', 'curse': 'A curse is laid', 'relic_set': 'A cursed relic is placed',
    'relic_trigger': 'A cursed relic wakes', 'silence': 'A hush falls', 'bane_mist': 'A baneful mist rolls in',
    'blight_jump': 'A blight spreads', 'abomination_form': 'An abomination takes form',
    'calamity': 'Calamity strikes', 'thorn_burst': 'Thorns burst from the ground', 'thorn_hit': 'Thorns pierce',
    'mausoleum': 'A thorned mausoleum seals', 'coffin_close': 'A coffin is sealed', 'coffin_open': 'A coffin opens',
    'thorn_garden': 'A garden of thorns grows', 'rose_heal': 'Roses heal',
}


def main(assets, wav_dir=None):
    folder = os.path.join(assets, 'sounds/abomination')
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
            files.append({'name': f'circle_of_inevitability:abomination/{file}'})
            print(f'sounds/abomination/{file}.ogg  {len(samples) / SR:.2f} s')
        events['abomination.' + name] = {
            'subtitle': f'subtitles.circle_of_inevitability.abomination.{name}',
            'sounds': files,
        }
    with open(sounds_json, 'w') as f:
        json.dump(events, f, indent=2)
    lang_path = os.path.join(assets, 'lang/en_us.json')                # the subtitles
    if os.path.exists(lang_path):
        with open(lang_path, encoding='utf-8') as f:
            lang = json.load(f)
        for name, text in SUBTITLES.items():
            lang[f'subtitles.circle_of_inevitability.abomination.{name}'] = text
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
