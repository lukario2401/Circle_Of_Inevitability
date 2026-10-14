"""Synthesizes the Hand Of God sounds and writes sounds.json.

python3 tools/model_gen/hand_of_god/sounds.py src/main/resources/assets/circle_of_inevitability [--wav dir]

Every sound is built from scratch (sine partials, filtered noise, formant choirs, a synthetic room), so nothing
here is copied from anywhere. The building blocks (tone, bell, choir, room, thump, finish...) are the ones in
tools/model_gen/beauty_goddess/sounds.py, loaded from there. Needs numpy, scipy and ffmpeg (with libvorbis).

The .ogg files go to sounds/hand_of_god/, mono 44.1 kHz (Minecraft only places mono sounds in the world), and each
one gets an event "hand_of_god.<name>" in sounds.json and a subtitle in lang/en_us.json. The same names are
registered in sounds/ModSounds.java (HAND_...): a new sound here needs a line there too.

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
RNG = np.random.default_rng(4096)
silence, noise, place, env_adsr, decay, fade = kit.silence, kit.noise, kit.place, kit.env_adsr, kit.decay, kit.fade
lowpass, highpass, bandpass, sweep, tone, glide = kit.lowpass, kit.highpass, kit.bandpass, kit.sweep, kit.tone, kit.glide
bell, choir, room, thump, heartbeat, finish = kit.bell, kit.choir, kit.room, kit.thump, kit.heartbeat, kit.finish
sparkle, drone, creak, clank, bubbles = kit.sparkle, kit.drone, kit.creak, kit.clank, kit.bubbles


def n_of(seconds):
    return int(seconds * SR)


# =====================================================================================================
# Building blocks of a warrior: whooshes, blows, steel
# =====================================================================================================

def whoosh(seconds, lo=300, hi=2600, peak=0.5, width=0.7):
    """Air torn by something fast: a band of noise that sweeps up and back down, loudest at 'peak' (0..1)."""
    n = n_of(seconds)
    x = sweep(noise(seconds), lambda p: lo + (hi - lo) * np.exp(-((p - peak) / 0.25) ** 2), width)
    shape = np.exp(-((np.linspace(0, 1, n) - peak) / 0.28) ** 2)
    return x * shape


def blow(weight=1.0):
    """A heavy blow landing on a body: a low thump, a slap of noise and a short crunch."""
    out = silence(0.45)
    place(out, thump(60 * (1.3 - 0.3 * weight), 0.35, 0.55, 0.09) * (0.9 + 0.3 * weight), 0)
    place(out, bandpass(noise(0.05), 400, 2500) * decay(n_of(0.05), 0.012) * 0.8, 0)
    place(out, highpass(noise(0.08), 1500) * decay(n_of(0.08), 0.02) * 0.25 * weight, 0.005)
    return out


def ring_of_steel(freq=1450, seconds=1.4, bright=1.0):
    """A blade's ring: an inharmonic metal partial set that dies slowly."""
    return bell(freq, seconds, bright, (1.0, 2.76, 5.4, 8.93, 11.2), 0.9)


def shing(freq=2200, seconds=0.6):
    """Steel drawn or light forming: a bright scrape rising into a ring."""
    out = silence(seconds)
    n = n_of(0.25)
    scrape = sweep(noise(0.25), lambda p: 2500 + 5000 * p, 0.35) * np.linspace(0.2, 1, n) * decay(n, 0.2)
    place(out, scrape * 0.5, 0)
    place(out, ring_of_steel(freq, seconds - 0.08, 1.2) * 0.45, 0.08)
    return out


def rumble(seconds, base=38, rate=9):
    """Deep, uneven rumbling: stone, a Giant's weight, a great wind."""
    n = n_of(seconds)
    x = lowpass(noise(seconds), 140, 2)
    am = 0.6 + 0.4 * np.sin(2 * np.pi * rate * np.arange(n) / SR + RNG.uniform(0, 6.28))
    return x * am + 0.3 * tone(base, seconds) * am


def tick(freq=3200):
    d = 0.03
    return tone(freq, d) * decay(n_of(d), 0.004) + highpass(noise(d), 4000) * decay(n_of(d), 0.002) * 0.5


# =====================================================================================================
# Warrior, Pugilist, Weapon Master
# =====================================================================================================

def s_charge():
    seconds = 1.0
    out = silence(seconds)
    place(out, whoosh(0.55, 200, 1600, 0.7, 0.9) * 0.8, 0)
    for i in range(4):                                                      # heavy footfalls on the way
        place(out, thump(48, 0.18, 0.7, 0.05) * 0.4, 0.04 + i * 0.11)
    place(out, blow(1.2), 0.48)
    return room(out, 0.6, 0.18, 2500)


def s_impact():
    return room(blow(1.0), 0.4, 0.15, 2500)


def s_divine_blood():
    seconds = 2.0
    n = n_of(seconds)
    out = silence(seconds)
    place(out, heartbeat(0.9, 50), 0)
    place(out, heartbeat(0.9, 54) * 1.1, 0.55)
    swell = sweep(noise(1.6), lambda p: 200 + 900 * p, 0.6) * env_adsr(n_of(1.6), 1.0, 0.2, 0, 0.4, 0.8)
    place(out, swell * 0.4, 0.25)
    place(out, choir((130.81, 196.0, 261.63), 1.4, 'oh', 2, 0.6, 0.6, depth=0.004) * 0.18, 0.5)
    return room(out, 1.0, 0.25, 3000)


def s_flurry():
    seconds = 1.0
    out = silence(seconds)
    for i in range(5):
        at = 0.02 + i * 0.13
        place(out, whoosh(0.12, 600, 3000, 0.6, 0.5) * 0.35, at)
        place(out, blow(0.6 + 0.12 * i) * (0.5 + 0.1 * i), at + 0.08)
    return room(out, 0.5, 0.15, 2500)


def s_resist():
    seconds = 1.6
    n = n_of(seconds)
    out = silence(seconds)
    place(out, drone(1.6, 55, (1, 2, 3), 0.6) * env_adsr(n, 0.15, 0.3, 0, 0.8, 0.6) * 0.5, 0)
    place(out, ring_of_steel(880, 1.3, 0.6) * 0.3, 0.02)
    place(out, choir((110.0, 164.81), 1.3, 'oo', 2, 0.3, 0.8, depth=0.003) * 0.18, 0.05)
    return room(out, 1.2, 0.3, 3000)


def s_edge_swing(variant):
    seconds = 0.45
    out = silence(seconds)
    place(out, whoosh(0.35, 700 + 200 * variant, 4200, 0.45 + 0.1 * variant, 0.45), 0)
    place(out, sparkle(0.35, 3, 5000, 9000) * 0.12, 0.05)
    return room(out, 0.3, 0.1, 5000)


def s_finisher():
    seconds = 1.6
    out = silence(seconds)
    place(out, whoosh(0.4, 400, 3500, 0.75, 0.6) * 0.8, 0)
    place(out, blow(1.4), 0.3)
    place(out, ring_of_steel(1250, 1.2, 1.0) * 0.5, 0.3)
    return room(out, 1.0, 0.25, 4000)


def s_parry():
    seconds = 1.4
    out = silence(seconds)
    place(out, clank(1100) * 0.9, 0)
    place(out, ring_of_steel(1650, 1.3, 1.3) * 0.6, 0.005)
    place(out, highpass(noise(0.04), 3000) * decay(n_of(0.04), 0.008) * 0.6, 0)
    return room(out, 0.8, 0.22, 6000)


# =====================================================================================================
# Dawn Paladin: light, armor, the Sword of Dawn, the Hurricane of Light
# =====================================================================================================

def s_dawn_light():
    seconds = 3.0
    n = n_of(seconds)
    out = silence(seconds)
    place(out, choir((261.63, 329.63, 392.0, 523.25), 2.6, 'ah', 2, 0.9, 1.2, depth=0.006) * 0.24, 0)
    place(out, sweep(noise(seconds), lambda p: 1500 + 5000 * p, 0.5) * env_adsr(n, 1.2, 0.3, 0, 1.2, 0.5) * 0.12, 0)
    for i, f in enumerate((1046.5, 1318.5, 1568.0, 2093.0)):
        place(out, bell(f, 1.8, 0.6, (1.0, 2.0, 3.0, 4.07), 1.4) * 0.12, 0.5 + i * 0.18)
    place(out, sparkle(seconds, 26, 4000, 10000, 0.6, 2.6) * 0.12, 0)
    return room(out, 2.4, 0.45, 7000)


def s_dawn_armor():
    seconds = 1.8
    out = silence(seconds)
    for i in range(5):                                                      # plates locking into place
        place(out, clank(700 + i * 90) * 0.35, 0.1 + i * 0.12)
    place(out, shing(1800, 1.0) * 0.5, 0.7)
    place(out, choir((196.0, 293.66, 392.0), 1.0, 'oh', 2, 0.2, 0.6, depth=0.003) * 0.15, 0.6)
    return room(out, 1.3, 0.3, 5000)


def s_dawn_sword():
    seconds = 2.2
    n = n_of(seconds)
    out = silence(seconds)
    place(out, sweep(noise(0.9), lambda p: 400 + 6000 * p ** 1.5, 0.5) * np.linspace(0, 1, n_of(0.9)) ** 2 * 0.5, 0)
    place(out, shing(2350, 1.4) * 0.8, 0.8)
    hum = (tone(110, 1.4) + 0.5 * tone(220.6, 1.4) + 0.25 * tone(330, 1.4)) * env_adsr(n_of(1.4), 0.1, 0.3, 0, 0.8, 0.5)
    place(out, hum * 0.3, 0.85)
    return room(out, 1.4, 0.3, 6000)


def s_hurricane():
    seconds = 4.0
    n = n_of(seconds)
    out = silence(seconds)
    wind = sweep(noise(seconds), lambda p: 250 + 1400 * p + 300 * np.sin(p * 30), 0.7)
    place(out, wind * env_adsr(n, 0.9, 0.5, 0, 1.4, 0.8) * 0.8, 0)
    place(out, rumble(seconds, 36, 7) * env_adsr(n, 0.6, 0.4, 0, 1.4, 0.8) * 0.5, 0)
    place(out, choir((130.81, 196.0, 261.63, 392.0), 3.0, 'ah', 2, 1.2, 1.4, depth=0.01) * 0.2, 0.3)
    for i in range(10):
        place(out, shing(RNG.uniform(1800, 3200), 0.5) * 0.15, 0.6 + i * 0.25)
    return room(out, 2.0, 0.35, 4500)


# =====================================================================================================
# Guardian: Protection, Damage Transference
# =====================================================================================================

def s_protection():
    seconds = 3.0
    n = n_of(seconds)
    out = silence(seconds)
    place(out, blow(1.6), 0)                                                # the sword driven into the ground
    place(out, ring_of_steel(980, 1.6, 0.9) * 0.6, 0.0)
    place(out, rumble(0.6, 40, 12) * decay(n_of(0.6), 0.2) * 0.6, 0.02)
    wall = choir((146.83, 220.0, 293.66, 440.0), 2.4, 'oo', 2, 0.6, 1.0, depth=0.003) * 0.22
    place(out, wall, 0.35)
    place(out, sweep(noise(2.0), lambda p: 600 + 2500 * p, 0.4) * env_adsr(n_of(2.0), 0.6, 0.4, 0, 1.0, 0.4) * 0.1, 0.35)
    return room(out, 2.0, 0.4, 5000)


def s_dome_hit():
    seconds = 1.2
    out = silence(seconds)
    place(out, bell(330, 1.1, 0.4, (1.0, 1.5, 2.0, 2.98, 4.1), 1.0) * 0.6, 0)
    place(out, thump(70, 0.2, 0.8, 0.05) * 0.5, 0)
    return room(out, 1.0, 0.35, 3500)


def s_transference():
    seconds = 2.0
    n = n_of(seconds)
    out = silence(seconds)
    place(out, heartbeat(0.9, 46), 0)
    place(out, heartbeat(0.9, 46) * 0.7, 0.9)
    link = (tone(220, 1.8) * tone(3.0, 1.8) * 0.6 + tone(330, 1.8) * 0.3) * env_adsr(n_of(1.8), 0.3, 0.3, 0, 0.8, 0.7)
    place(out, link * 0.3, 0.1)
    place(out, sparkle(seconds, 8, 2500, 6000, 0.2, 1.6) * 0.12, 0)
    return room(out, 1.4, 0.35, 4000)


# =====================================================================================================
# Demon Hunter: the Eye, Alchemy, Mind Concealment, the Giant
# =====================================================================================================

def s_eye():
    seconds = 2.0
    n = n_of(seconds)
    out = silence(seconds)
    whisper = sweep(noise(seconds), lambda p: 1800 + 1200 * np.sin(p * 17) ** 2, 0.25) * env_adsr(n, 0.4, 0.3, 0, 0.9, 0.6)
    place(out, whisper * 0.35, 0)
    place(out, bell(659.25, 1.6, 0.4, (1.0, 2.4, 4.1), 1.2) * 0.4, 0.1)
    place(out, bell(987.77, 1.4, 0.4, (1.0, 2.4, 4.1), 1.0) * 0.25, 0.35)
    place(out, drone(1.6, 49, (1, 1.5), 0.3) * env_adsr(n_of(1.6), 0.3, 0.2, 0, 0.8, 0.6) * 0.3, 0.1)
    return room(out, 1.5, 0.4, 4000)


def s_alchemy():
    seconds = 1.8
    out = silence(seconds)
    place(out, bubbles(1.0, 30, 300, 1600, 0.04) * 0.6, 0)
    hiss = highpass(noise(0.9), 3000) * env_adsr(n_of(0.9), 0.05, 0.2, 0, 0.5, 0.5)
    place(out, hiss * 0.35, 0.7)
    place(out, shing(2600, 0.8) * 0.35, 0.9)
    return room(out, 0.8, 0.2, 5000)


def s_coat_hit():
    seconds = 0.7
    out = silence(seconds)
    sizzle = highpass(noise(0.6), 2500) * (0.5 + 0.5 * np.abs(np.sin(np.arange(n_of(0.6)) / SR * 70))) * decay(n_of(0.6), 0.2)
    place(out, sizzle * 0.6, 0)
    place(out, blow(0.5) * 0.6, 0)
    return room(out, 0.4, 0.15, 5000)


def s_conceal():
    seconds = 1.6
    n = n_of(seconds)
    out = silence(seconds)
    breath = sweep(noise(seconds), lambda p: 1200 * (1 - p) + 200, 0.5) * env_adsr(n, 0.2, 0.3, 0, 1.0, 0.7)
    place(out, breath * 0.5, 0)
    place(out, tone(glide(440, 220, 1.2, 0.6), 1.2) * env_adsr(n_of(1.2), 0.1, 0.3, 0, 0.7, 0.5) * 0.15, 0.1)
    return room(out, 1.2, 0.4, 2500)


def s_giant_form():
    seconds = 3.6
    n = n_of(seconds)
    out = silence(seconds)
    place(out, rumble(seconds, 30, 5) * env_adsr(n, 1.0, 0.4, 0, 1.4, 0.8) * 0.7, 0)
    for i in range(14):                                                     # bones and flesh stretching
        place(out, creak(0.25, 20, 60) * 0.25, 0.2 + i * 0.12)
    growl = drone(2.4, 36.7, (1, 2, 3, 4), 1.7) * env_adsr(n_of(2.4), 0.6, 0.4, 0, 0.9, 0.8)
    place(out, growl * 0.7, 0.8)
    place(out, choir((55.0, 82.41), 2.0, 'oh', 3, 0.8, 0.9, depth=0.02) * 0.2, 1.0)
    return room(out, 2.0, 0.35, 2000)


def s_giant_step():
    seconds = 1.0
    out = silence(seconds)
    place(out, thump(38, 0.6, 0.6, 0.15) * 1.2, 0)
    place(out, rumble(0.8, 32, 14) * decay(n_of(0.8), 0.25) * 0.6, 0.01)
    place(out, lowpass(noise(0.3), 900) * decay(n_of(0.3), 0.06) * 0.4, 0)
    return room(out, 1.0, 0.3, 1500)


# =====================================================================================================
# Silver Knight: Mercury Liquefaction, Silver Rapier, Light Concealment
# =====================================================================================================

def s_mercury():
    seconds = 1.8
    out = silence(seconds)
    for _ in range(14):                                                     # thick liquid metal: low, slow gloops
        f0 = RNG.uniform(150, 420)
        d = RNG.uniform(0.06, 0.14)
        g = tone(glide(f0, f0 * 2.2, d, 0.6), d) * decay(n_of(d), d / 2.5)
        place(out, g * RNG.uniform(0.4, 1.0), RNG.uniform(0, 1.2))
    shimmer = bell(1975, 1.4, 0.8, (1.0, 1.41, 2.0, 2.83), 1.0) * 0.18
    place(out, shimmer, 0.2)
    place(out, sweep(noise(1.0), lambda p: 600 + 300 * np.sin(p * 20), 0.5) * env_adsr(n_of(1.0), 0.2, 0.2, 0, 0.6, 0.5) * 0.2, 0)
    return room(out, 1.0, 0.3, 4000)


def s_rapier():
    seconds = 1.2
    out = silence(seconds)
    for i in range(3):
        at = i * 0.07
        place(out, tone(glide(5200, 2600, 0.12, 0.5), 0.12) * decay(n_of(0.12), 0.05) * 0.4, at)
        place(out, highpass(noise(0.06), 4000) * decay(n_of(0.06), 0.015) * 0.5, at)
        place(out, blow(0.5) * 0.4, at + 0.02)
    place(out, ring_of_steel(2600, 1.0, 1.4) * 0.35, 0.03)
    return room(out, 0.8, 0.25, 7000)


def s_light_step():
    seconds = 1.4
    out = silence(seconds)
    place(out, whoosh(0.5, 900, 6000, 0.5, 0.5) * 0.5, 0)
    for i, f in enumerate((1568.0, 2093.0, 2637.0)):
        place(out, bell(f, 0.9, 0.7, (1.0, 2.0, 3.0), 0.8) * 0.22, 0.2 + i * 0.06)
    place(out, sparkle(seconds, 14, 5000, 11000, 0.1, 1.0) * 0.15, 0)
    return room(out, 1.2, 0.4, 8000)


# =====================================================================================================
# Glory, Hand of God, Twilight Giant
# =====================================================================================================

def s_twilight_sword():
    seconds = 3.0
    n = n_of(seconds)
    out = silence(seconds)
    rip = sweep(noise(0.7), lambda p: 3000 * (1 - p) + 200, 0.6) * np.linspace(0.3, 1, n_of(0.7)) * 0.7
    place(out, rip, 0)                                                      # the void tears open
    place(out, blow(1.8) * 1.1, 0.62)
    place(out, rumble(2.0, 34, 6) * decay(n_of(2.0), 0.6) * 0.7, 0.62)
    place(out, choir((98.0, 146.83, 196.0), 2.0, 'ah', 2, 0.1, 1.4, depth=0.012) * 0.22, 0.6)
    place(out, ring_of_steel(740, 2.0, 0.6) * 0.4, 0.62)
    return room(out, 2.0, 0.4, 3500)


def s_divine_hand():
    seconds = 2.8
    out = silence(seconds)
    place(out, sweep(noise(0.8), lambda p: 200 + 1600 * p, 0.6) * np.linspace(0, 1, n_of(0.8)) * 0.4, 0)
    for i in range(10):                                                     # the grip tightening
        place(out, creak(0.2, 30, 90) * 0.35, 0.55 + i * 0.07)
    place(out, thump(42, 0.8, 0.5, 0.25) * 1.2, 1.3)                       # the squeeze, then the throw
    place(out, rumble(1.2, 30, 9) * decay(n_of(1.2), 0.35) * 0.6, 1.3)
    place(out, choir((73.42, 110.0, 146.83), 1.4, 'oh', 2, 0.1, 0.9, depth=0.01) * 0.2, 1.3)
    return room(out, 1.8, 0.35, 3000)


def s_time_stop():
    seconds = 3.2
    n = n_of(seconds)
    out = silence(seconds)
    t, gap = 0.0, 0.12
    while t < 2.4:                                                          # a clock ticking slower and slower
        place(out, tick(3000 - t * 600) * (1 - t / 3), t)
        t += gap
        gap *= 1.18
    reverse = sweep(noise(1.4), lambda p: 300 + 3000 * p, 0.5) * np.linspace(0, 1, n_of(1.4)) ** 3 * 0.35
    place(out, reverse, 0.4)
    place(out, bell(523.25, 2.0, 0.5, (1.0, 2.0, 2.99, 4.1), 1.8) * 0.4, 1.75)
    place(out, choir((130.81, 196.0, 261.63), 1.4, 'oo', 2, 0.05, 1.2, depth=0.0) * 0.18, 1.75)
    return room(out, 2.2, 0.5, 4000)


def s_time_resume():
    seconds = 1.2
    out = silence(seconds)
    place(out, highpass(noise(0.03), 1500) * 0.8, 0)                        # a snap
    place(out, sweep(noise(0.6), lambda p: 4000 * (1 - p) + 300, 0.5) * decay(n_of(0.6), 0.2) * 0.5, 0)
    for i in range(6):
        place(out, tick(3200) * 0.5, 0.05 + i * 0.08)
    return room(out, 1.0, 0.3, 5000)


def s_decay():
    seconds = 3.0
    n = n_of(seconds)
    out = silence(seconds)
    crackle = silence(seconds)
    for _ in range(160):
        d = RNG.uniform(0.003, 0.02)
        place(crackle, bandpass(noise(d), 800, 5000) * decay(n_of(d), d / 3) * RNG.uniform(0.2, 1), RNG.uniform(0, seconds - 0.05))
    place(out, crackle * env_adsr(n, 0.5, 0.3, 0, 1.0, 0.8) * 0.6, 0)
    wind = sweep(noise(seconds), lambda p: 300 + 250 * np.sin(p * 9), 0.5) * env_adsr(n, 0.8, 0.3, 0, 1.2, 0.7)
    place(out, wind * 0.45, 0)
    place(out, drone(2.6, 41.2, (1, 1.06, 1.5), 0.25) * env_adsr(n_of(2.6), 0.6, 0.3, 0, 1.0, 0.6) * 0.35, 0.2)
    return room(out, 1.8, 0.35, 2500)


def s_kingdom():
    seconds = 5.0
    n = n_of(seconds)
    out = silence(seconds)
    place(out, choir((65.41, 98.0, 130.81, 164.81, 196.0, 261.63), 4.6, 'ah', 2, 1.6, 1.8, depth=0.006) * 0.22, 0)
    for i, f in enumerate((130.81, 196.0, 261.63, 392.0)):                  # great bells
        place(out, bell(f, 3.0, 0.4, decay_s=2.6) * 0.3, 0.8 + i * 0.6)
    place(out, rumble(seconds, 30, 3) * env_adsr(n, 1.5, 0.4, 0, 1.6, 0.6) * 0.35, 0)
    place(out, sweep(noise(seconds), lambda p: 400 + 1200 * p, 0.6) * env_adsr(n, 2.0, 0.4, 0, 1.6, 0.5) * 0.12, 0)
    return room(out, 3.4, 0.55, 3500)


def s_cage():
    seconds = 2.4
    n = n_of(seconds)
    out = silence(seconds)
    place(out, sweep(noise(0.8), lambda p: 5000 * (1 - p) + 300, 0.5) * np.linspace(1, 0.2, n_of(0.8)) * 0.4, 0)
    place(out, bell(196.0, 2.2, 0.5, (1.0, 1.5, 2.0, 2.98, 4.1), 2.0) * 0.6, 0.55)
    place(out, choir((98.0, 146.83, 196.0, 293.66), 1.8, 'oo', 2, 0.1, 1.2, depth=0.0) * 0.2, 0.55)
    return room(out, 2.0, 0.4, 3500)


# =====================================================================================================
# The list: event name -> the functions that make its variants
# =====================================================================================================

SOUNDS = {
    'charge': [s_charge],
    'impact': [s_impact],
    'divine_blood': [s_divine_blood],
    'flurry': [s_flurry],
    'resist': [s_resist],
    'edge_swing': [lambda: s_edge_swing(0), lambda: s_edge_swing(1)],
    'finisher': [s_finisher],
    'parry': [s_parry],
    'dawn_light': [s_dawn_light],
    'dawn_armor': [s_dawn_armor],
    'dawn_sword': [s_dawn_sword],
    'hurricane': [s_hurricane],
    'protection': [s_protection],
    'dome_hit': [s_dome_hit],
    'transference': [s_transference],
    'eye': [s_eye],
    'alchemy': [s_alchemy],
    'coat_hit': [s_coat_hit],
    'conceal': [s_conceal],
    'giant_form': [s_giant_form],
    'giant_step': [s_giant_step],
    'mercury': [s_mercury],
    'rapier': [s_rapier],
    'light_step': [s_light_step],
    'twilight_sword': [s_twilight_sword],
    'divine_hand': [s_divine_hand],
    'time_stop': [s_time_stop],
    'time_resume': [s_time_resume],
    'decay': [s_decay],
    'kingdom': [s_kingdom],
    'cage': [s_cage],
}

SUBTITLES = {
    'charge': 'A warrior charges', 'impact': 'A heavy blow lands', 'divine_blood': 'Divine blood surges',
    'flurry': 'A flurry of blows', 'resist': 'Supernatural power is shrugged off', 'edge_swing': 'A blade sweeps',
    'finisher': 'A finishing cut', 'parry': 'A blow is parried', 'dawn_light': 'The light of dawn breaks',
    'dawn_armor': 'Armor of light forms', 'dawn_sword': 'The Sword of Dawn forms', 'hurricane': 'A Hurricane of Light roars',
    'protection': 'A wall of dawn rises', 'dome_hit': 'Protection holds', 'transference': 'Wounds are taken on',
    'eye': 'The Eye of Demon Hunting opens', 'alchemy': 'A concoction is brewed', 'coat_hit': 'A coated blade sizzles',
    'conceal': 'A mind is hidden', 'giant_form': 'A Giant awakens', 'giant_step': 'A Giant\'s step',
    'mercury': 'Liquid silver flows', 'rapier': 'Silver rapiers strike', 'light_step': 'Someone steps through light',
    'twilight_sword': 'The Twilight Sword tears through', 'divine_hand': 'The Divine Right Hand grips',
    'time_stop': 'Time stands still', 'time_resume': 'Time resumes', 'decay': 'Everything decays',
    'kingdom': 'A Divine Kingdom descends', 'cage': 'A cage of light closes',
}


def encode(samples, path):
    kit.encode(samples, path)


def main(assets, wav_dir=None):
    folder = os.path.join(assets, 'sounds/hand_of_god')
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
            files.append({'name': f'circle_of_inevitability:hand_of_god/{file}'})
            print(f'sounds/hand_of_god/{file}.ogg  {len(samples) / SR:.2f} s')
        events['hand_of_god.' + name] = {
            'subtitle': f'subtitles.circle_of_inevitability.hand_of_god.{name}',
            'sounds': files,
        }
    with open(sounds_json, 'w') as f:
        json.dump(events, f, indent=2)
    lang_path = os.path.join(assets, 'lang/en_us.json')                # the subtitles
    if os.path.exists(lang_path):
        with open(lang_path, encoding='utf-8') as f:
            lang = json.load(f)
        for name, text in SUBTITLES.items():
            lang[f'subtitles.circle_of_inevitability.hand_of_god.{name}'] = text
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
