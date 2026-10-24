"""Synthesizes the Hand Of Order sounds and writes sounds.json.

python3 tools/model_gen/hand_of_order/sounds.py src/main/resources/assets/circle_of_inevitability [--wav dir]

Every sound is built from scratch (sine partials, filtered noise, formant choirs, a synthetic room), so nothing
here is copied from anywhere. The building blocks (tone, bell, choir, voice, room, thump, clank, drone, finish...)
are the ones in tools/model_gen/beauty_goddess/sounds.py, and the scholar's (whoosh, tick, rustle, scratch,
shimmer, crackle) those of tools/model_gen/knowledge_emperor/sounds.py, loaded from there; this file adds the court's
own: a gavel, a brass horn, a spoken word, psychic lightning, glass walls, a great bell. Needs numpy, scipy and ffmpeg
(with libvorbis).

The .ogg files go to sounds/hand_of_order/, mono 44.1 kHz (Minecraft only places mono sounds in the world), and each
one gets an event "hand_of_order.<name>" in sounds.json and a subtitle in lang/en_us.json. The same names are
registered in sounds/ModSounds.java (ORDER_...): a new sound here needs a line there too.

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
RNG = np.random.default_rng(1650)
silence, noise, place, env_adsr, decay, fade = kit.silence, kit.noise, kit.place, kit.env_adsr, kit.decay, kit.fade
lowpass, highpass, bandpass, sweep, tone, glide = kit.lowpass, kit.highpass, kit.bandpass, kit.sweep, kit.tone, kit.glide
bell, choir, voice, room, thump, finish = kit.bell, kit.choir, kit.voice, kit.room, kit.thump, kit.finish
sparkle, drone, clank, glass_shards, t_axis = kit.sparkle, kit.drone, kit.clank, kit.glass_shards, kit.t_axis
whoosh, tick, rustle, scratch, shimmer, crackle = (scholar.whoosh, scholar.tick, scholar.rustle, scholar.scratch,
                                                  scholar.shimmer, scholar.crackle)
reverse = scholar.reverse


def n_of(seconds):
    return int(seconds * SR)


# =====================================================================================================
# Building blocks of a court: a gavel, a brass horn, a spoken word, psychic lightning, glass, a great bell
# =====================================================================================================

def gavel(weight=1.0):
    """A gavel on its block: a hard wooden knock with a short hollow body, heavier for a bigger weight."""
    d = 0.5
    n = n_of(d)
    out = np.zeros(n)
    for f, a in ((180 / weight, 1.0), (410 / weight, 0.6), (760 / weight, 0.35), (1320, 0.2)):
        out += a * tone(f, d, RNG.uniform(0, 6.28)) * decay(n, 0.05 * weight)
    place(out, bandpass(noise(0.012), 800, 5000) * 1.2, 0)
    place(out, thump(70 / weight, 0.35, 0.6, 0.08) * 0.8 * weight, 0)
    return out


def horn(freq, seconds, swell=0.4, bright=1.0):
    """A brass horn: a buzzy harmonic tone that brightens as it swells."""
    n = n_of(seconds)
    tt = t_axis(seconds)
    env = env_adsr(n, swell, 0.2, 0, min(0.6, seconds * 0.4), 0.85)
    out = np.zeros(n)
    for k in range(1, 14):
        f = freq * k
        if f > SR * 0.45:
            break
        amp = (1.0 / k) * (0.4 + 0.6 * env * bright) ** (k * 0.35)
        out += amp * np.sin(2 * np.pi * f * tt * (1 + 0.003 * np.sin(2 * np.pi * 4.8 * tt)) + RNG.uniform(0, 6.28))
    return out * env


def brass_chord(freqs, seconds, swell=0.5):
    out = silence(seconds)
    for f in freqs:
        place(out, horn(f * 2 ** (RNG.uniform(-6, 6) / 1200), seconds, swell) / len(freqs), 0)
    return out


def word(seconds=0.55, pitch=82.0, vowels=('ah', 'oh')):
    """A word spoken with Authority: two low voiced syllables, a little rasp, no language at all."""
    out = silence(seconds + 0.1)
    half = seconds / len(vowels)
    for i, v in enumerate(vowels):
        syl = voice(pitch * (1.0 - 0.06 * i), half + 0.05, v, vibrato=3.0, depth=0.002, breath=0.08)
        syl *= env_adsr(len(syl), 0.03, 0.08, 0, 0.12, 0.8)
        place(out, syl, i * half)
    place(out, bandpass(noise(0.05), 1500, 5000) * decay(n_of(0.05), 0.015) * 0.6, 0)   # the hard first consonant
    return out


def zap(seconds, lo=1800, hi=6500, density=90):
    """Psychic lightning: a crackle of fine sparks over a buzzing hum."""
    out = crackle(seconds, int(seconds * density), lo, hi)
    n = n_of(seconds)
    buzz = np.sign(np.sin(2 * np.pi * 120 * t_axis(seconds))) * 0.12 + tone(240, seconds) * 0.08
    out += lowpass(buzz, 2400) * np.sin(np.linspace(0, np.pi, n)) ** 0.6
    return out


def crack(seconds=0.25, lo=2500):
    """A whip-crack of electricity: one sharp snap and its fizz."""
    out = silence(seconds)
    place(out, highpass(noise(0.006), lo) * 2.0, 0)
    place(out, bandpass(noise(seconds - 0.01), 2000, 9000) * decay(n_of(seconds - 0.01), 0.04) * 0.6, 0.004)
    place(out, crackle(seconds, 14, 3000, 8000) * 0.4, 0)
    return out


def great_bell(freq=98.0, seconds=4.0):
    return bell(freq, seconds, 0.7, (0.5, 1.0, 1.19, 1.5, 2.0, 2.52, 3.0, 4.07), 5.0)


def pane(freq):
    """A glass pane set down: a clear ring and a small clink."""
    d = 0.9
    out = bell(freq, d, 1.0, (1.0, 2.31, 3.9, 5.6), 0.6) * 0.5
    place(out, highpass(noise(0.008), 4000) * 0.7, 0)
    return out


def chains(seconds, count, start=0.0):
    out = silence(seconds)
    for _ in range(count):
        place(out, clank(RNG.uniform(500, 1200)) * RNG.uniform(0.15, 0.4), RNG.uniform(start, seconds - 0.35))
    return out


# =====================================================================================================
# Arbiter: Halt, Presence; Sheriff: Jurisdiction, Recognition, the Sheriff's Intuition
# =====================================================================================================

def s_halt():
    seconds = 1.4
    out = silence(seconds)
    place(out, word(0.45, 78, ('ah',)) * 0.9, 0.0)
    place(out, gavel(1.2) * 0.9, 0.12)
    place(out, horn(98, 0.6, 0.02) * env_adsr(n_of(0.6), 0.01, 0.1, 0, 0.4, 0.5) * 0.3, 0.12)
    return room(out, 1.3, 0.4, 3500)


def s_presence():
    seconds = 2.0
    out = silence(seconds)
    place(out, brass_chord((65.4, 98.0, 130.8), 1.4, 0.25) * 0.8, 0)
    place(out, sweep(noise(1.0), lambda p: 200 + 1600 * (1 - p), 0.6) * env_adsr(n_of(1.0), 0.05, 0.2, 0, 0.6, 0.7) * 0.4, 0.05)
    place(out, thump(45, 0.6, 0.5, 0.25) * 0.9, 0.05)
    return room(out, 1.8, 0.4, 3000)


def s_jurisdiction():
    seconds = 4.0
    out = silence(seconds)
    place(out, great_bell(98.0, 3.6) * 0.8, 0.0)
    place(out, great_bell(146.8, 3.0) * 0.3, 0.02)
    place(out, shimmer(1.6, 2000, 7000) * 0.15, 0.2)
    for i in range(6):                                                       # the boundary written round
        place(out, scholar.chime(1046.5 * 2 ** (i / 12 * 2), 0.8, 0.5) * 0.12, 0.4 + i * 0.12)
    return room(out, 2.6, 0.45, 3000)


def s_jurisdiction_enter():
    seconds = 1.2
    out = silence(seconds)
    place(out, scholar.chime(880.0, 1.0, 0.5) * 0.6, 0)
    place(out, scholar.chime(659.3, 1.0, 0.5) * 0.5, 0.12)
    return room(out, 1.0, 0.35, 5000)


def s_recognition():
    seconds = 1.5
    out = silence(seconds)
    for i in range(3):                                                       # it is looked over, piece by piece
        place(out, tick(2800 - i * 200) * 0.5, i * 0.09)
    place(out, tone(glide(330, 440, 0.7, 0.6), 0.7) * env_adsr(n_of(0.7), 0.05, 0.2, 0, 0.4, 0.6) * 0.3, 0.25)
    place(out, bell(1318.5, 1.0, 0.7, (1.0, 2.0, 3.0), 0.8) * 0.35, 0.3)
    return room(out, 1.2, 0.35, 5000)


def s_intuition():
    seconds = 1.2
    out = silence(seconds)
    place(out, bell(587.3, 1.0, 0.5, (1.0, 2.0, 2.76), 0.9) * 0.5, 0)
    place(out, bell(440.0, 1.0, 0.5, (1.0, 2.0, 2.76), 0.9) * 0.35, 0.18)
    return room(out, 1.0, 0.4, 4000)


# =====================================================================================================
# Interrogator: Psychic Lashing, Psychic Piercing, Whip of Pain
# =====================================================================================================

def s_lashing():
    seconds = 1.6
    n = n_of(seconds)
    out = silence(seconds)
    place(out, zap(1.4, 1500, 6000, 60) * env_adsr(n_of(1.4), 0.5, 0.2, 0, 0.5, 0.8) * 0.7, 0)
    place(out, tone(glide(90, 180, 1.2, 0.5), 1.2) * env_adsr(n_of(1.2), 0.6, 0.2, 0, 0.3, 0.7) * 0.25, 0)
    place(out, crack(0.3) * 0.7, 0.9)
    return room(out, 1.2, 0.3, 6000)


def s_lash_hit(variant):
    seconds = 0.6
    out = silence(seconds)
    place(out, crack(0.3, 2200 + variant * 600) * 0.9, 0)
    place(out, zap(0.35, 2000, 7000, 120) * decay(n_of(0.35), 0.1) * 0.5, 0.02)
    place(out, thump(90 + variant * 15, 0.2, 0.6, 0.05) * 0.5, 0)
    return room(out, 0.7, 0.25, 6000)


def s_piercing():
    seconds = 1.3
    n = n_of(seconds)
    out = silence(seconds)
    beam = sweep(noise(0.5), lambda p: 5000 - 3000 * p, 0.4) * env_adsr(n_of(0.5), 0.01, 0.1, 0, 0.3, 0.8)
    place(out, beam * 0.6, 0)
    place(out, zap(0.6, 2500, 8000, 160) * 0.6, 0)
    place(out, tone(glide(1600, 400, 0.4, 0.5), 0.4) * decay(n_of(0.4), 0.15) * 0.25, 0)
    return room(out, 1.0, 0.3, 6000)


def s_whip():
    seconds = 1.4
    out = silence(seconds)
    place(out, whoosh(0.4, 400, 3500, 0.7, 0.5) * 0.6, 0)
    place(out, crack(0.35, 2000) * 1.0, 0.36)
    place(out, zap(0.6, 1500, 6000, 70) * decay(n_of(0.6), 0.25) * 0.5, 0.38)
    place(out, word(0.4, 70, ('oh',)) * 0.25, 0.42)                         # a groan of the mind lashed
    return room(out, 1.2, 0.3, 5000)


# =====================================================================================================
# Judge: the Verdicts and the Prohibitions
# =====================================================================================================

def s_verdict():
    """The word itself, spoken as each Verdict is cast (SpellFx.speak)."""
    seconds = 1.1
    out = silence(seconds)
    place(out, word(0.5, 74, ('ah', 'oh')) * 1.0, 0)
    place(out, thump(52, 0.5, 0.6, 0.18) * 0.6, 0.02)
    return room(out, 1.2, 0.45, 3000)


def s_imprison():
    seconds = 1.6
    out = silence(seconds)
    for i in range(5):                                                       # layer on layer of transparent walls
        place(out, whoosh(0.2, 600, 3000, 0.8, 0.5) * 0.25, i * 0.08)
        place(out, pane(1400 + i * 180) * 0.6, 0.15 + i * 0.08)
    place(out, clank(520) * 0.5, 0.58)
    place(out, thump(60, 0.4, 0.6, 0.12) * 0.6, 0.58)
    return room(out, 1.3, 0.35, 5000)


def s_release():
    seconds = 1.4
    out = silence(seconds)
    for i in range(4):
        place(out, pane(2000 - i * 200) * 0.4, i * 0.06)
    place(out, shimmer(0.8, 3000, 9000, rise=False) * 0.2, 0.1)
    return room(out, 1.2, 0.35, 6000)


def s_exile():
    seconds = 1.6
    out = silence(seconds)
    place(out, word(0.3, 80, ('ee',)) * 0.5, 0)
    place(out, thump(40, 0.6, 0.4, 0.2) * 1.0, 0.12)
    place(out, whoosh(1.0, 200, 3000, 0.25, 0.8) * 0.8, 0.1)
    return room(out, 1.5, 0.35, 3500)


def s_exile_void():
    seconds = 1.8
    out = silence(seconds)
    implode = reverse(sweep(noise(0.8), lambda p: 300 + 3000 * p, 0.6) * decay(n_of(0.8), 0.25))
    place(out, implode * 0.8, 0)
    place(out, thump(36, 0.8, 0.5, 0.3) * 1.0, 0.78)
    place(out, drone(0.9, 55, (1, 1.41, 2.1), 1.3) * env_adsr(n_of(0.9), 0.05, 0.2, 0, 0.5, 0.6) * 0.4, 0.8)
    return room(out, 1.4, 0.4, 2500)


def s_death():
    seconds = 1.4
    out = silence(seconds)
    place(out, whoosh(0.35, 300, 3000, 0.6, 0.6) * 0.8, 0)                  # the rush
    place(out, thump(55, 0.5, 0.45, 0.14) * 1.2, 0.32)                       # the fist on the head
    place(out, bandpass(noise(0.08), 300, 2500) * decay(n_of(0.08), 0.02) * 1.0, 0.32)
    place(out, gavel(1.6) * 0.5, 0.33)
    return room(out, 1.1, 0.35, 3000)


def s_secrecy():
    seconds = 1.6
    out = silence(seconds)
    place(out, reverse(shimmer(0.6, 2500, 8000)) * 0.4, 0)
    place(out, bell(1567.98, 0.8, 0.8, (1.0, 2.4, 3.9), 0.5) * 0.3, 0.55)
    hush = lowpass(bandpass(noise(0.9), 1500, 6000), 4000) * env_adsr(n_of(0.9), 0.05, 0.3, 0, 0.5, 0.5) * 0.35
    place(out, hush, 0.6)                                                    # shh
    return room(out, 1.3, 0.4, 3500)


def s_deprivation():
    seconds = 1.6
    n = n_of(seconds)
    out = silence(seconds)
    suck = sweep(noise(1.0), lambda p: 3500 * (0.1 ** p), 0.5) * env_adsr(n_of(1.0), 0.2, 0.2, 0, 0.5, 0.7)
    place(out, suck * 0.7, 0)
    place(out, tone(glide(520, 110, 1.0, 0.7), 1.0) * env_adsr(n_of(1.0), 0.05, 0.2, 0, 0.6, 0.6) * 0.3, 0.05)
    place(out, clank(380) * 0.5, 0.95)                                       # the power locked away
    return room(out, 1.2, 0.35, 3500)


def s_execution_raise():
    seconds = 1.8
    n = n_of(seconds)
    out = silence(seconds)
    rise = drone(1.6, 55, (1, 1.5, 2, 3), 2.5) * env_adsr(n_of(1.6), 1.2, 0.1, 0, 0.3, 0.9)
    place(out, rise * 0.5, 0)
    for i in range(6):                                                       # the ring tightening
        place(out, tick(1800 + i * 250) * (0.3 + i * 0.08), 0.1 + i * 0.22)
    place(out, sweep(noise(1.5), lambda p: 400 + 2600 * p, 0.5) * env_adsr(n_of(1.5), 1.2, 0.1, 0, 0.2, 0.8) * 0.25, 0)
    return room(out, 1.0, 0.3, 4000)


def s_execution():
    seconds = 3.0
    out = silence(seconds)
    place(out, whoosh(0.3, 200, 2500, 0.7, 0.6) * 0.7, 0)
    place(out, thump(38, 1.0, 0.4, 0.35) * 1.4, 0.25)
    place(out, gavel(2.2) * 1.0, 0.25)
    place(out, great_bell(65.4, 2.5) * 0.4, 0.27)
    place(out, bandpass(noise(0.4), 80, 1200) * decay(n_of(0.4), 0.12) * 0.8, 0.25)
    return room(out, 2.4, 0.4, 2500)


def s_prohibition():
    seconds = 2.4
    out = silence(seconds)
    place(out, word(0.6, 76, ('oh', 'ah')) * 0.7, 0)
    place(out, clank(260) * 1.0, 0.5)                                        # iron set down
    place(out, clank(190) * 0.8, 0.52)
    place(out, chains(1.2, 10) * 0.8, 0.45)
    place(out, thump(48, 0.6, 0.5, 0.2) * 0.8, 0.5)
    place(out, great_bell(130.8, 1.6) * 0.25, 0.52)
    return room(out, 1.8, 0.4, 3000)


def s_refused():
    """A teleport, a flight, a hiding refused: a dull iron stop and a sour buzz."""
    seconds = 0.8
    out = silence(seconds)
    place(out, clank(230) * 0.8, 0)
    buzz = np.sign(np.sin(2 * np.pi * 92 * t_axis(0.35))) * decay(n_of(0.35), 0.12)
    place(out, lowpass(buzz, 1500) * 0.4, 0.01)
    return room(out, 0.8, 0.3, 3000)


def s_violation():
    seconds = 1.6
    out = silence(seconds)
    place(out, bell(207.65, 1.5, 0.9, (1.0, 1.41, 2.0, 2.83, 3.5), 1.2) * 0.7, 0)   # a sour gong
    place(out, bell(220.0, 1.5, 0.9, (1.0, 1.5, 2.0, 3.0), 1.2) * 0.5, 0.0)
    place(out, gavel(1.0) * 0.6, 0)
    return room(out, 1.4, 0.4, 3500)


# =====================================================================================================
# Disciplinary Paladin: Punishment; Imperative Mage: Imperative: Reality, Contract, the Brass Pillar
# =====================================================================================================

def s_punishment():
    seconds = 3.0
    out = silence(seconds)
    place(out, choir((196.0, 246.94, 293.66), 2.4, 'ah', 2, 0.9, 1.0) * 0.12, 0)   # dawn breaking
    place(out, brass_chord((98.0, 146.8, 196.0), 2.2, 0.8) * 0.5, 0.2)
    place(out, shimmer(2.0, 2500, 8000) * 0.12, 0.3)
    place(out, word(0.4, 72, ('ee',)) * 0.4, 0)                              # the Convict named
    return room(out, 2.2, 0.45, 3500)


def s_reality():
    seconds = 2.2
    out = silence(seconds)
    damp = sweep(noise(1.6), lambda p: 4000 * (0.06 ** p), 0.6) * env_adsr(n_of(1.6), 0.05, 0.3, 0, 0.8, 0.6)
    place(out, damp * 0.5, 0)                                                # mysticism dulled
    place(out, thump(42, 0.8, 0.6, 0.3) * 1.0, 0.02)
    place(out, clank(150) * 0.6, 0.02)
    place(out, word(0.6, 70, ('oh', 'ah')) * 0.4, 0.1)
    return room(out, 1.4, 0.3, 2000)


def s_contract():
    seconds = 2.2
    out = silence(seconds)
    place(out, rustle(0.5, 10) * 0.6, 0)                                      # the paper appears
    place(out, scratch(0.8, 6) * 0.6, 0.45)                                  # it is signed
    place(out, thump(80, 0.3, 0.7, 0.06) * 0.7, 1.3)                         # and sealed
    place(out, gavel(0.9) * 0.4, 1.3)
    place(out, bell(784.0, 0.9, 0.6, (1.0, 2.0, 3.0), 0.8) * 0.25, 1.32)
    return room(out, 1.2, 0.3, 5000)


def s_contract_tear():
    seconds = 0.9
    out = silence(seconds)
    tear = highpass(noise(0.35), 1500) * (0.5 + 0.5 * np.abs(np.sin(np.linspace(0, 40, n_of(0.35)))))
    place(out, tear * env_adsr(n_of(0.35), 0.01, 0.1, 0, 0.15, 0.8) * 0.8, 0)
    place(out, crackle(0.4, 18, 2500, 8000) * 0.5, 0.02)
    return room(out, 0.8, 0.25, 6000)


def s_pillar():
    seconds = 3.0
    out = silence(seconds)
    rumble = lowpass(noise(1.6), 140) * env_adsr(n_of(1.6), 0.6, 0.2, 0, 0.6, 0.8)
    place(out, rumble * 1.2, 0)
    place(out, great_bell(73.4, 2.4) * 0.6, 0.6)
    place(out, horn(146.8, 1.4, 0.5) * 0.3, 0.6)
    return room(out, 2.0, 0.4, 2500)


# =====================================================================================================
# Chaos Hunter: Sword of Judgment; Balancer: Balance, Divide the Battlefield, Law: No Return
# =====================================================================================================

def s_sword_of_judgment():
    seconds = 3.0
    out = silence(seconds)
    place(out, whoosh(0.3, 300, 4000, 0.7, 0.5) * 0.8, 0)                    # the single stride
    place(out, brass_chord((130.8, 196.0, 261.6), 1.6, 0.15) * 0.4, 0.25)
    place(out, sweep(noise(0.5), lambda p: 6000 - 5000 * p, 0.4) * env_adsr(n_of(0.5), 0.01, 0.1, 0, 0.3, 0.8) * 0.9, 0.3)
    for f in (1650, 2470, 3700):                                             # the blade's ring
        place(out, bell(f, 1.6, 1.0, (1.0, 2.76, 5.4), 0.9) * 0.15, 0.32)
    place(out, thump(45, 0.8, 0.5, 0.25) * 1.2, 0.4)
    place(out, choir((261.6, 392.0), 1.6, 'ah', 2, 0.05, 1.0) * 0.1, 0.35)
    return room(out, 2.2, 0.4, 3500)


def s_balance():
    seconds = 2.4
    out = silence(seconds)
    for i, at in enumerate((0.0, 0.35, 0.6, 0.8, 0.95)):                     # the pans tipping, then still
        place(out, chains(0.6, 3) * (0.5 - i * 0.07), at)
        place(out, bell(1046.5 if i % 2 else 987.8, 0.8, 0.8, (1.0, 2.4, 3.9), 0.6) * (0.3 - i * 0.04), at + 0.05)
    place(out, horn(110.0, 1.2, 0.4) * 0.25, 0.9)
    return room(out, 1.6, 0.4, 4000)


def s_divide():
    seconds = 2.6
    out = silence(seconds)
    place(out, sweep(noise(1.4), lambda p: 150 + 2500 * p, 0.6) * env_adsr(n_of(1.4), 1.0, 0.1, 0, 0.3, 0.9) * 0.6, 0)
    place(out, drone(1.5, 49, (1, 1.5, 2), 1.0) * env_adsr(n_of(1.5), 1.0, 0.2, 0, 0.4, 0.8) * 0.5, 0)
    for i in range(6):                                                       # the bars of the wall locking in
        place(out, clank(300 + i * 40) * 0.4, 1.2 + i * 0.05)
    place(out, thump(42, 0.7, 0.5, 0.25) * 1.0, 1.25)
    return room(out, 1.8, 0.4, 3000)


def s_no_return():
    seconds = 4.0
    out = silence(seconds)
    place(out, great_bell(55.0, 3.6) * 0.9, 0)                               # a knell
    place(out, great_bell(82.4, 3.0) * 0.3, 0.01)
    place(out, choir((110.0, 130.8), 2.6, 'oo', 2, 0.6, 1.5) * 0.1, 0.3)
    place(out, clank(170) * 0.5, 1.0)                                        # the door barred
    return room(out, 2.6, 0.45, 2500)


# =====================================================================================================
# Hand of Order: Order Proxy, Quell Disorder, Sense of Order; Justiciar: the Court of Judgment
# =====================================================================================================

def s_proxy():
    seconds = 3.6
    out = silence(seconds)
    place(out, lowpass(noise(2.0), 120) * env_adsr(n_of(2.0), 0.4, 0.3, 0, 1.0, 0.8) * 1.0, 0)
    place(out, brass_chord((65.4, 98.0, 130.8, 196.0), 2.8, 0.6) * 0.8, 0.1)
    place(out, choir((261.6, 329.6, 392.0), 2.6, 'ah', 2, 0.6, 1.2) * 0.12, 0.4)
    place(out, great_bell(65.4, 2.6) * 0.4, 0.3)
    return room(out, 2.4, 0.45, 3000)


def s_proxy_end():
    seconds = 2.0
    out = silence(seconds)
    place(out, tone(glide(196, 65, 1.4, 0.7), 1.4) * env_adsr(n_of(1.4), 0.05, 0.3, 0, 0.8, 0.6) * 0.4, 0)
    place(out, sweep(noise(1.2), lambda p: 2000 * (0.08 ** p), 0.6) * env_adsr(n_of(1.2), 0.05, 0.3, 0, 0.6, 0.6) * 0.4, 0)
    place(out, thump(40, 0.6, 0.6, 0.2) * 0.6, 1.0)
    return room(out, 1.4, 0.35, 2500)


def s_quell():
    seconds = 3.0
    out = silence(seconds)
    place(out, reverse(crackle(0.8, 50, 800, 6000)) * 0.4, 0)                # Disorder snuffed out
    place(out, choir((196.0, 293.66, 392.0), 2.2, 'oo', 2, 0.2, 1.4) * 0.15, 0.6)
    place(out, great_bell(196.0, 2.2) * 0.4, 0.7)
    place(out, shimmer(1.6, 2000, 6000, rise=False) * 0.15, 0.7)
    return room(out, 2.2, 0.45, 4000)


def s_sense_order():
    seconds = 1.2
    out = silence(seconds)
    place(out, bell(739.99, 1.0, 0.6, (1.0, 2.0, 3.0, 4.2), 0.9) * 0.5, 0)
    place(out, gavel(0.7) * 0.25, 0)
    return room(out, 1.0, 0.35, 5000)


def s_court():
    seconds = 6.0
    out = silence(seconds)
    for i, f in enumerate((65.4, 98.0, 130.8)):                              # bells of the Ancient Court
        place(out, great_bell(f, 4.0) * (0.6 - i * 0.12), i * 0.5)
    place(out, choir((130.8, 196.0, 261.6, 329.6), 4.0, 'ah', 2, 1.2, 1.6) * 0.12, 0.5)
    place(out, brass_chord((65.4, 98.0, 130.8), 3.0, 1.0) * 0.5, 1.0)
    for i in range(3):                                                       # Order in the court
        place(out, gavel(1.8) * 0.9, 2.6 + i * 0.45)
    return room(out, 3.0, 0.5, 3000)


def s_clause():
    seconds = 1.8
    out = silence(seconds)
    place(out, gavel(1.4) * 0.9, 0)
    place(out, great_bell(196.0, 1.6) * 0.35, 0.01)
    place(out, word(0.4, 76, ('oh',)) * 0.35, 0.15)
    return room(out, 1.6, 0.45, 3000)


def s_sentence():
    seconds = 2.6
    out = silence(seconds)
    place(out, gavel(2.4) * 1.0, 0)
    place(out, thump(36, 1.0, 0.45, 0.35) * 1.2, 0)
    place(out, great_bell(55.0, 2.2) * 0.5, 0.02)
    return room(out, 2.0, 0.45, 2500)


def s_adjourn():
    seconds = 2.6
    out = silence(seconds)
    place(out, gavel(1.6) * 0.8, 0)
    place(out, choir((196.0, 261.6, 329.6), 1.8, 'oo', 2, 0.1, 1.4) * 0.12, 0.1)
    place(out, shimmer(1.4, 1500, 5000, rise=False) * 0.12, 0.3)
    return room(out, 2.0, 0.45, 3500)


# =====================================================================================================
# The melee combo
# =====================================================================================================

def s_order_swing(variant):
    seconds = 0.7
    out = silence(seconds)
    place(out, whoosh(0.35, 400 + variant * 150, 2800, 0.5, 0.5) * 0.8, 0)
    place(out, bell(1318.5 + variant * 200, 0.5, 0.7, (1.0, 2.4), 0.4) * 0.15, 0.12)
    return room(out, 0.7, 0.25, 5000)


def s_gavel_slam():
    seconds = 1.4
    out = silence(seconds)
    place(out, whoosh(0.25, 300, 2000, 0.8, 0.5) * 0.6, 0)
    place(out, gavel(1.8) * 1.0, 0.22)
    place(out, thump(48, 0.6, 0.5, 0.2) * 0.9, 0.22)
    return room(out, 1.2, 0.35, 3000)


SOUNDS = {
    'halt': [s_halt],
    'presence': [s_presence],
    'jurisdiction': [s_jurisdiction],
    'jurisdiction_enter': [s_jurisdiction_enter],
    'recognition': [s_recognition],
    'intuition': [s_intuition],
    'lashing': [s_lashing],
    'lash_hit': [lambda: s_lash_hit(0), lambda: s_lash_hit(1), lambda: s_lash_hit(2)],
    'piercing': [s_piercing],
    'whip': [s_whip],
    'verdict': [s_verdict],
    'imprison': [s_imprison],
    'release': [s_release],
    'exile': [s_exile],
    'exile_void': [s_exile_void],
    'death': [s_death],
    'secrecy': [s_secrecy],
    'deprivation': [s_deprivation],
    'execution_raise': [s_execution_raise],
    'execution': [s_execution],
    'prohibition': [s_prohibition],
    'refused': [s_refused],
    'violation': [s_violation],
    'punishment': [s_punishment],
    'reality': [s_reality],
    'contract': [s_contract],
    'contract_tear': [s_contract_tear],
    'pillar': [s_pillar],
    'sword_of_judgment': [s_sword_of_judgment],
    'balance': [s_balance],
    'divide': [s_divide],
    'no_return': [s_no_return],
    'proxy': [s_proxy],
    'proxy_end': [s_proxy_end],
    'quell': [s_quell],
    'sense_order': [s_sense_order],
    'court': [s_court],
    'clause': [s_clause],
    'sentence': [s_sentence],
    'adjourn': [s_adjourn],
    'order_swing': [lambda: s_order_swing(0), lambda: s_order_swing(1)],
    'gavel_slam': [s_gavel_slam],
}

SUBTITLES = {
    'halt': 'A word: Halt', 'presence': 'A wave of Authority', 'jurisdiction': 'Ground is claimed',
    'jurisdiction_enter': 'Someone enters the Jurisdiction', 'recognition': 'Someone is memorized',
    'intuition': 'Something hunts from out of sight', 'lashing': 'An Illusory Torture Device ignites',
    'lash_hit': 'Psychic lightning lashes', 'piercing': 'Lightning leaps from the eyes', 'whip': 'A Whip of Pain cracks',
    'verdict': 'A Verdict is spoken', 'imprison': 'Transparent walls close', 'release': 'The walls open',
    'exile': 'Someone is exiled', 'exile_void': 'The void swallows someone', 'death': 'Death is declared',
    'secrecy': 'Secrecy is imposed', 'deprivation': 'A power is taken away', 'execution_raise': 'A fist is raised',
    'execution': 'An Execution', 'prohibition': 'Something is prohibited', 'refused': 'It is not allowed',
    'violation': 'A Rule is broken', 'punishment': 'A Convict is named', 'reality': 'Reality is enhanced',
    'contract': 'A Contract is signed', 'contract_tear': 'A Contract tears', 'pillar': 'The Brass Pillar rises',
    'sword_of_judgment': 'The Sword of Judgment falls', 'balance': 'The scales settle',
    'divide': 'The battlefield is divided', 'no_return': 'The dead shall not return', 'proxy': 'Order acts through someone',
    'proxy_end': 'The proxy is spent', 'quell': 'Order is restored', 'sense_order': 'An ability is sensed',
    'court': 'The Court of Judgment convenes', 'clause': 'A clause is written', 'sentence': 'A sentence is passed',
    'adjourn': 'The Court adjourns', 'order_swing': 'Order sweeps the air', 'gavel_slam': 'A gavel slams down',
}


def main(assets, wav_dir=None):
    folder = os.path.join(assets, 'sounds/hand_of_order')
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
            files.append({'name': f'circle_of_inevitability:hand_of_order/{file}'})
            print(f'sounds/hand_of_order/{file}.ogg  {len(samples) / SR:.2f} s')
        events['hand_of_order.' + name] = {
            'subtitle': f'subtitles.circle_of_inevitability.hand_of_order.{name}',
            'sounds': files,
        }
    with open(sounds_json, 'w') as f:
        json.dump(events, f, indent=2)
    lang_path = os.path.join(assets, 'lang/en_us.json')                # the subtitles
    if os.path.exists(lang_path):
        with open(lang_path, encoding='utf-8') as f:
            lang = json.load(f)
        for name, text in SUBTITLES.items():
            lang[f'subtitles.circle_of_inevitability.hand_of_order.{name}'] = text
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
