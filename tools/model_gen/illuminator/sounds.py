"""Synthesizes the Illuminator (Paragon pathway) sounds and writes sounds.json.

python3 tools/model_gen/illuminator/sounds.py src/main/resources/assets/circle_of_inevitability [--wav dir]

Every sound is built from scratch (sine partials, filtered noise, formant choirs, a synthetic room), so nothing here is
copied from anywhere. The building blocks come from tools/model_gen/beauty_goddess/sounds.py (tone, bell, choir, room,
thump, clank, glass_shards, finish...) and tools/model_gen/knowledge_emperor/sounds.py (whispers, rustle, scratch,
chime, patter, whoosh, tick, shimmer); this file adds the workshop's own: clockwork ratchets, meshing gears, steam,
pistons, gunshots, an orchestra. Needs numpy, scipy and ffmpeg (with libvorbis).

The .ogg files go to sounds/illuminator/, mono 44.1 kHz (Minecraft only places mono sounds in the world), and each
one gets an event "illuminator.<name>" in sounds.json and a subtitle in lang/en_us.json. The same names are
registered in sounds/ModSounds.java (ILLUMINATOR_...): a new sound here needs a line there too.

To change one: edit its function, run this again, press F3+T in game.
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
scholar = _load('knowledge_sounds', os.path.join(_here, '..', 'knowledge_emperor', 'sounds.py'))

SR = kit.SR
RNG = np.random.default_rng(2401)
silence, noise, place, env_adsr, decay, fade = kit.silence, kit.noise, kit.place, kit.env_adsr, kit.decay, kit.fade
lowpass, highpass, bandpass, sweep, tone, glide = kit.lowpass, kit.highpass, kit.bandpass, kit.sweep, kit.tone, kit.glide
bell, choir, room, thump, finish, t_axis = kit.bell, kit.choir, kit.room, kit.thump, kit.finish, kit.t_axis
sparkle, drone, creak, clank, glass_shards = kit.sparkle, kit.drone, kit.creak, kit.clank, kit.glass_shards
whispers, rustle, scratch, chime = scholar.whispers, scholar.rustle, scholar.scratch, scholar.chime
patter, whoosh, tick, shimmer, crackle, reverse = (scholar.patter, scholar.whoosh, scholar.tick, scholar.shimmer,
                                                   scholar.crackle, scholar.reverse)


def n_of(seconds):
    return int(seconds * SR)


# =====================================================================================================
# Building blocks of a workshop: clockwork, gears, steam, pistons, gunfire, an orchestra
# =====================================================================================================

def ratchet(seconds, rate0=8, rate1=30, freq=2600, level=0.6):
    """Clockwork being wound: a pawl clicking over a ratchet, faster and faster."""
    out = silence(seconds)
    t = 0.0
    while t < seconds - 0.03:
        p = t / seconds
        place(out, tick(freq * RNG.uniform(0.92, 1.08)) * level * RNG.uniform(0.6, 1.0), t)
        t += 1.0 / (rate0 + (rate1 - rate0) * p)
    return out


def gears(seconds, teeth_rate=40, hum=110, level=1.0):
    """Gears meshing: a fine rattle of teeth over a low hum of the shaft."""
    n = n_of(seconds)
    out = silence(seconds)
    t = 0.0
    while t < seconds - 0.02:
        place(out, bandpass(noise(0.006), 1500, 5000) * decay(n_of(0.006), 0.002) * RNG.uniform(0.2, 0.6), t)
        t += 1.0 / teeth_rate * RNG.uniform(0.85, 1.15)
    shaft = sum(tone(hum * k, seconds) / k for k in (1, 2, 3)) * 0.12
    return (out + shaft) * env_adsr(n, 0.08, 0.1, 0, 0.15, 0.9) * level


def hiss(seconds, lo=2500, hi=9000, attack=0.02, release=0.3):
    """Steam escaping."""
    n = n_of(seconds)
    return bandpass(noise(seconds), lo, hi) * env_adsr(n, attack, 0.05, 0, release, 0.8)


def piston(freq=70):
    """A piston slamming home and venting."""
    out = silence(0.6)
    place(out, thump(freq, 0.3, 0.7, 0.08) * 0.9, 0)
    place(out, clank(freq * 8) * 0.25, 0.0)
    place(out, hiss(0.4, 3000, 8000, 0.01, 0.3) * 0.3, 0.05)
    return out


def gunshot(body=1.0):
    """A crack of powder: a hard noise burst, a low punch, the brass ringing after."""
    out = silence(0.8)
    crack = highpass(noise(0.05), 800) * decay(n_of(0.05), 0.012)
    place(out, crack * 1.2, 0)
    place(out, thump(90, 0.25, 0.5, 0.05) * body, 0)
    place(out, lowpass(noise(0.4), 900) * decay(n_of(0.4), 0.08) * 0.5 * body, 0.005)
    place(out, clank(2200) * 0.12, 0.01)
    return out


def boom(seconds=1.6, freq=42, level=1.0):
    """A heavy explosion: a sub drop and a long rumble."""
    n = n_of(seconds)
    out = silence(seconds)
    place(out, thump(freq, 0.8, 0.45, 0.25) * 1.2, 0)
    place(out, lowpass(noise(seconds), 300, 2) * decay(n, 0.35) * 2.0, 0)
    place(out, highpass(noise(0.12), 1500) * decay(n_of(0.12), 0.03) * 0.8, 0)
    return out * level


def strings(freqs, seconds, attack=0.4, release=0.8, bright=1800):
    """A section of bowed strings: detuned saw-like voices, softened."""
    n = n_of(seconds)
    tt = t_axis(seconds)
    out = np.zeros(n)
    for f in freqs:
        for v in range(3):
            fv = f * 2 ** (RNG.uniform(-8, 8) / 1200)
            vib = 1 + 0.004 * np.sin(2 * np.pi * 5.5 * tt + RNG.uniform(0, 6.28))
            ph = np.cumsum(2 * np.pi * fv * vib / SR)
            out += sum(np.sin(k * ph) / k for k in range(1, 10)) * 0.3
    return lowpass(out, bright, 2) * env_adsr(n, attack, 0.2, 0, release, 0.85)


def brass(freqs, seconds, attack=0.08, release=0.4):
    """Horns: bright harmonic stacks that open up as they swell."""
    n = n_of(seconds)
    out = np.zeros(n)
    for f in freqs:
        ph = 2 * np.pi * f * t_axis(seconds)
        out += sum(np.sin(k * ph + RNG.uniform(0, 6.28)) * (0.9 ** k) for k in range(1, 12)) * 0.25
    return sweep(out, lambda p: 600 + 2400 * min(1.0, p * 3), 1.4, 0.15) * env_adsr(n, attack, 0.2, 0, release, 0.8)


def timpani(freq=65):
    out = silence(1.2)
    place(out, thump(freq, 1.0, 0.9, 0.35) * 1.0, 0)
    place(out, lowpass(noise(0.08), 600) * decay(n_of(0.08), 0.02) * 0.6, 0)
    return out


# =====================================================================================================
# Savant: Compound Flask, Recall; Archaeologist: Survey, Ritual Circle; Appraiser: Appraise, Exploit Flaw
# =====================================================================================================

def s_flask_throw():
    seconds = 0.8
    out = silence(seconds)
    place(out, whoosh(0.4, 500, 2400, 0.5, 0.5) * 0.6, 0)
    for i in range(4):                                                    # the liquid sloshing in the glass
        place(out, bandpass(noise(0.05), 400, 1400) * np.sin(np.linspace(0, np.pi, n_of(0.05))) * 0.3, 0.05 + i * 0.07)
    place(out, chime(2600, 0.5, 1.0) * 0.1, 0.02)
    return room(out, 0.6, 0.2, 5000)


def s_flask_burst(variant):
    seconds = 1.8
    out = silence(seconds)
    place(out, glass_shards(0.9, 24, 0.0, 0.25) * 0.8, 0)
    place(out, boom(1.2, 60 + variant * 8, 0.8), 0.01)                   # nitroglycerin
    place(out, hiss(1.2, 2000, 7000, 0.02, 0.9) * 0.35, 0.05)             # and the acid eating
    place(out, crackle(1.0, 30, 2000, 8000) * 0.4, 0.15)
    return room(out, 1.2, 0.3, 4000)


def s_acid_hiss():
    seconds = 1.6
    out = silence(seconds)
    place(out, hiss(1.5, 1800, 7000, 0.1, 0.8) * 0.5, 0)
    place(out, crackle(1.5, 50, 1500, 6000) * 0.5, 0)
    return room(out, 0.8, 0.25, 5000)


def s_recall():
    seconds = 1.6
    n = n_of(seconds)
    out = silence(seconds)
    for i in range(10):                                                   # a clock running backward, quickening
        place(out, tick(3000 if i % 2 else 2400) * 0.5, 0.05 + 0.12 * (1 - (i / 10) ** 0.6) * i)
    rewind = reverse(rustle(0.8, 10, 0.0, 0.7)) * 0.6                     # pages turned back
    place(out, rewind, 0.3)
    place(out, sweep(noise(0.9), lambda p: 4000 * 0.15 ** p, 0.5) * np.linspace(1, 0, n_of(0.9)) * 0.3, 0.4)
    place(out, chime(1567.98, 1.0, 0.5) * 0.3, 1.1)
    return room(out, 1.4, 0.35, 5000)


def s_survey():
    seconds = 2.2
    out = silence(seconds)
    ping = bell(1318.5, 1.8, 0.6, (1.0, 2.0, 3.01), 1.6) * 0.5            # a sounding pulse going out
    place(out, ping, 0)
    place(out, bell(1318.5, 1.4, 0.4, (1.0, 2.0), 1.2) * 0.18, 0.45)      # and its echo coming back
    place(out, whoosh(1.0, 300, 3000, 0.25, 0.8) * 0.3, 0)
    place(out, scratch(0.6, 3) * 0.25, 0.1)                                # pencil on paper: the map is drawn
    return room(out, 1.8, 0.45, 6000)


def s_ritual_circle():
    seconds = 2.4
    n = n_of(seconds)
    out = silence(seconds)
    chalk = silence(1.2)
    for i in range(5):                                                    # chalk dragged round in strokes
        d = 0.2
        stroke = bandpass(noise(d), 1800, 5000) * (0.6 + 0.4 * np.sin(2 * np.pi * 35 * t_axis(d))) * \
            np.sin(np.linspace(0, np.pi, n_of(d)))
        place(chalk, stroke * 0.5, i * 0.2)
    place(out, chalk, 0)
    place(out, drone(1.6, 65.4, (1, 1.5, 2), 0.3) * env_adsr(n_of(1.6), 0.6, 0.2, 0, 0.6, 0.7) * 0.4, 0.6)
    place(out, chime(987.77, 1.2, 0.5) * 0.35, 1.0)
    return room(out, 1.6, 0.4, 4000)


def s_ritual_pulse():
    seconds = 1.0
    out = silence(seconds)
    place(out, chime(1318.5, 0.9, 0.4) * 0.35, 0)
    place(out, sparkle(0.8, 6, 3000, 7000) * 0.15, 0.05)
    return room(out, 1.0, 0.4, 5000)


def s_appraise():
    seconds = 1.4
    out = silence(seconds)
    place(out, clank(3400) * 0.3, 0)                                      # a jeweller's lens snapped down
    for i in range(6):                                                    # measuring ticks
        place(out, tick(3600 + i * 120) * 0.3, 0.12 + i * 0.05)
    place(out, chime(1760.0, 1.0, 0.6) * 0.45, 0.45)                      # known
    place(out, chime(2637.0, 0.8, 0.6) * 0.25, 0.52)
    return room(out, 1.2, 0.35, 6000)


def s_exploit_flaw():
    seconds = 1.2
    out = silence(seconds)
    place(out, sweep(noise(0.12), lambda p: 3000 + 5000 * p, 0.3) * 0.6, 0)   # the hairline drawn
    place(out, chime(4186.0, 0.5, 1.2) * 0.4, 0.1)
    place(out, thump(60, 0.3, 0.6, 0.08) * 0.9, 0.13)
    place(out, crackle(0.4, 20, 800, 5000) * 0.8, 0.13)                   # and it gives
    return room(out, 0.8, 0.25, 5000)


def s_construct_break():
    seconds = 1.6
    out = silence(seconds)
    for i in range(16):                                                   # springs, screws and gears falling apart
        place(out, clank(RNG.uniform(900, 3200)) * RNG.uniform(0.15, 0.4), RNG.uniform(0.0, 0.8))
    place(out, tone(glide(900, 300, 0.4, 0.5), 0.4) * decay(n_of(0.4), 0.15) * 0.25, 0.05)   # a spring let go
    place(out, thump(55, 0.4, 0.6, 0.12) * 0.8, 0)
    return room(out, 1.0, 0.3, 4000)


# =====================================================================================================
# Artisan: Clockwork Rifle, Spirit Cannon, Mystic Brooch
# =====================================================================================================

def s_rifle_wind():
    seconds = 0.9
    out = silence(seconds)
    place(out, ratchet(0.6, 10, 34, 2800, 0.6), 0)
    place(out, clank(1900) * 0.35, 0.62)                                  # the catch locks
    place(out, chime(2093.0, 0.5, 0.8) * 0.15, 0.62)
    return room(out, 0.6, 0.2, 6000)


def s_rifle_shot(variant):
    seconds = 1.2
    out = silence(seconds)
    place(out, gunshot(1.0 - variant * 0.1), 0)
    place(out, tick(2400) * 0.4, 0.16)                                    # the clockwork feeding the next round
    place(out, tick(2700) * 0.3, 0.2)
    return room(out, 1.0, 0.25, 4000)


def s_cannon_unfold():
    seconds = 1.8
    out = silence(seconds)
    for i, at in enumerate((0.0, 0.25, 0.5, 0.8)):                        # plates and legs clanking into place
        place(out, clank(RNG.uniform(500, 900)) * 0.5, at)
        place(out, thump(80 - i * 6, 0.25, 0.6, 0.06) * 0.4, at)
    place(out, gears(0.6, 50, 90, 0.6), 0.3)
    place(out, hiss(0.7, 3000, 9000, 0.05, 0.5) * 0.35, 1.0)              # the boiler comes up
    return room(out, 1.2, 0.3, 4000)


def s_cannon_fire():
    seconds = 2.0
    out = silence(seconds)
    place(out, boom(1.6, 48, 1.0), 0)
    place(out, gunshot(0.6), 0)
    place(out, hiss(0.8, 2500, 8000, 0.02, 0.6) * 0.35, 0.1)             # steam venting from the breech
    return room(out, 1.6, 0.3, 3000)


def s_cannon_blast():
    seconds = 1.6
    out = silence(seconds)
    place(out, boom(1.4, 56, 0.9), 0)
    place(out, crackle(0.8, 30, 600, 5000) * 0.5, 0.02)
    place(out, chime(1975.5, 0.6, 1.0) * 0.12, 0.02)                      # the rune's light
    return room(out, 1.3, 0.3, 3500)


def s_brooch_pin():
    seconds = 1.4
    out = silence(seconds)
    place(out, clank(3000) * 0.3, 0)                                      # a clasp
    place(out, bell(1567.98, 1.2, 0.8, (1.0, 2.0, 2.76, 4.1), 1.0) * 0.45, 0.04)
    place(out, sparkle(1.0, 10, 4000, 9000, 0.05) * 0.2, 0)
    place(out, tone(110, 1.2) * env_adsr(n_of(1.2), 0.3, 0.2, 0, 0.6, 0.6) * 0.15, 0.1)   # a hum of the ward
    return room(out, 1.2, 0.35, 6000)


def s_brooch_burst():
    seconds = 1.4
    out = silence(seconds)
    place(out, glass_shards(0.7, 16, 0.0, 0.15) * 0.6, 0)
    place(out, thump(70, 0.4, 0.5, 0.1) * 0.8, 0)
    place(out, whoosh(0.5, 400, 3000, 0.2, 0.6) * 0.5, 0)
    return room(out, 1.0, 0.3, 4500)


# =====================================================================================================
# Astronomer: Star of Curses, Blessing of Stars, False Constellations, Meteor Shower
# =====================================================================================================

def s_star_curse():
    seconds = 1.6
    n = n_of(seconds)
    out = silence(seconds)
    ray = tone(glide(1800, 2400, 0.4, 0.5), 0.4) * env_adsr(n_of(0.4), 0.02, 0.05, 0, 0.2, 0.7) * 0.3
    place(out, ray, 0)
    place(out, shimmer(0.5, 3000, 10000) * 0.3, 0)
    curse = choir((138.6, 146.83), 1.2, 'oo', 2, 0.05, 0.8, depth=0.012) * 0.2   # a minor-second rub
    place(out, curse, 0.25)
    place(out, thump(52, 0.3, 0.6, 0.1) * 0.6, 0.25)
    return room(out, 1.4, 0.4, 4000)


def s_curse_step():
    seconds = 1.0
    out = silence(seconds)
    place(out, bell(220.0, 0.9, 0.5, (1.0, 1.19, 2.0, 2.4), 0.8) * 0.4, 0)
    place(out, whispers(0.5, 8, 1200, 3600, 0, 0.45, 0.5), 0.05)
    return room(out, 1.0, 0.35, 3500)


def s_blessing_stars():
    seconds = 2.6
    n = n_of(seconds)
    out = silence(seconds)
    place(out, shimmer(2.0, 2500, 9000) * 0.3, 0)
    place(out, sparkle(2.2, 30, 3000, 9000) * 0.25, 0)
    weight = tone(glide(90, 45, 2.0, 0.7), 2.0) * env_adsr(n_of(2.0), 0.6, 0.3, 0, 0.8, 0.8) * 0.5   # pressing down
    place(out, weight, 0.2)
    place(out, choir((523.25, 659.25, 783.99), 1.8, 'ah', 1, 0.6, 0.9, depth=0.002) * 0.06, 0.4)   # and lifting up
    return room(out, 2.0, 0.45, 6000)


def s_false_constellations():
    seconds = 3.0
    out = silence(seconds)
    for i, f in enumerate((1046.5, 1318.5, 1567.98, 2093.0, 1760.0)):    # stars lighting one by one
        place(out, chime(f, 1.4, 0.5) * 0.3, 0.2 + i * 0.28)
    place(out, sweep(noise(2.4), lambda p: 600 + 3000 * p, 0.7) * env_adsr(n_of(2.4), 0.8, 0.3, 0, 1.0, 0.6) * 0.25, 0)
    place(out, drone(2.4, 98.0, (1, 1.5, 2.0), 0.2) * env_adsr(n_of(2.4), 0.8, 0.2, 0, 0.9, 0.6) * 0.2, 0.2)
    return room(out, 2.2, 0.5, 6000)


def s_meteor_fall():
    seconds = 1.2
    out = silence(seconds)
    place(out, sweep(noise(1.0), lambda p: 7000 * 0.15 ** p, 0.5) * np.linspace(0.2, 1.0, n_of(1.0)) * 0.6, 0)
    place(out, tone(glide(2400, 600, 1.0, 0.8), 1.0) * np.linspace(0.05, 0.25, n_of(1.0)), 0)
    return room(out, 0.6, 0.2, 6000)


def s_meteor_impact():
    seconds = 1.4
    out = silence(seconds)
    place(out, thump(64, 0.5, 0.5, 0.15) * 1.0, 0)
    place(out, sparkle(0.9, 18, 3000, 9000) * 0.4, 0)                     # a burst of light, no fire
    place(out, bell(2093.0, 0.9, 1.0, (1.0, 2.0, 3.01), 0.7) * 0.25, 0)
    place(out, lowpass(noise(0.6), 500) * decay(n_of(0.6), 0.15) * 0.8, 0)
    return room(out, 1.0, 0.3, 5000)


# =====================================================================================================
# Alchemist: Living Doll, Golem, Mechanical Body
# =====================================================================================================

def s_doll_rise():
    seconds = 1.8
    out = silence(seconds)
    place(out, creak(1.0, 30, 80) * 0.6, 0.0)                              # matter grinding up out of the ground
    place(out, lowpass(noise(1.2), 400) * env_adsr(n_of(1.2), 0.3, 0.2, 0, 0.6, 0.8) * 0.8, 0)
    for i in range(4):                                                    # joints clicking into place
        place(out, clank(RNG.uniform(1200, 2400)) * 0.25, 0.9 + i * 0.12)
    place(out, chime(1174.66, 0.8, 0.6) * 0.25, 1.4)                      # its eyes open
    return room(out, 1.2, 0.3, 4000)


def s_doll_fall():
    seconds = 1.2
    out = silence(seconds)
    place(out, thump(70, 0.4, 0.6, 0.1) * 0.8, 0)
    place(out, crackle(0.8, 40, 400, 3000) * 0.8, 0.02)                   # crumbling back to what it was made of
    place(out, rustle(0.6, 8, 0.0, 0.5) * 0.4, 0.05)
    return room(out, 0.9, 0.25, 3500)


def s_golem_assemble():
    seconds = 4.4
    n = n_of(seconds)
    out = silence(seconds)
    place(out, gears(4.0, 30, 70, 0.6) * np.linspace(0.3, 1.0, n_of(4.0)), 0)
    for i in range(18):                                                   # brass and iron flying in and fitting
        at = 0.2 + i * 0.21 + RNG.uniform(-0.05, 0.05)
        place(out, whoosh(0.25, 500, 2500, 0.7, 0.5) * 0.2, max(0.0, at - 0.18))
        place(out, clank(RNG.uniform(300, 900)) * 0.45, at)
    place(out, piston(60) * 1.0, 3.9)                                     # the last plate, and it stands
    place(out, hiss(0.6, 2500, 8000, 0.01, 0.5) * 0.4, 3.95)
    place(out, bell(130.81, 0.6, 0.3, (1.0, 2.0, 3.0), 0.6) * 0.3, 4.0)
    return room(out, 1.6, 0.3, 3500)


def s_golem_strike():
    seconds = 1.0
    out = silence(seconds)
    place(out, whoosh(0.3, 200, 1400, 0.6, 0.6) * 0.5, 0)
    place(out, piston(55), 0.18)
    place(out, clank(420) * 0.6, 0.2)
    return room(out, 0.8, 0.25, 3000)


def s_golem_taunt():
    seconds = 1.8
    out = silence(seconds)
    horn = sum(tone(98.0 * k, 1.4) / k for k in (1, 2, 3, 4)) * env_adsr(n_of(1.4), 0.1, 0.2, 0, 0.5, 0.8) * 0.3
    place(out, lowpass(horn, 900), 0)                                      # a steam whistle, low
    place(out, hiss(1.4, 1500, 6000, 0.05, 0.4) * 0.4, 0)
    place(out, piston(50) * 0.8, 0.0)
    return room(out, 1.4, 0.35, 3000)


def s_mech_on():
    seconds = 1.8
    out = silence(seconds)
    for i in range(8):                                                    # plates clamping on one by one
        place(out, clank(RNG.uniform(600, 1400)) * 0.4, 0.05 + i * 0.12)
        place(out, tick(2600) * 0.3, 0.08 + i * 0.12)
    place(out, gears(0.8, 60, 120, 0.5), 0.2)
    place(out, hiss(0.6, 3000, 9000, 0.02, 0.5) * 0.4, 1.05)
    place(out, chime(1567.98, 0.7, 0.6) * 0.25, 1.2)                      # the eyes come on
    return room(out, 1.0, 0.25, 5000)


def s_jetpack():
    seconds = 1.2
    n = n_of(seconds)
    roar = bandpass(noise(seconds), 200, 3000) * (0.8 + 0.2 * np.sin(2 * np.pi * 23 * t_axis(seconds)))
    roar = roar * env_adsr(n, 0.05, 0.1, 0, 0.4, 0.85)
    burn = lowpass(noise(seconds), 200) * env_adsr(n, 0.05, 0.1, 0, 0.4, 0.8) * 1.4
    return room(roar * 0.6 + burn + hiss(seconds, 4000, 10000, 0.02, 0.5) * 0.3, 0.6, 0.2, 4000)


def s_mech_off():
    seconds = 1.4
    out = silence(seconds)
    place(out, hiss(1.0, 2000, 8000, 0.01, 0.8) * 0.5, 0)
    for i in range(6):
        place(out, clank(RNG.uniform(500, 1200)) * 0.3, 0.1 + i * 0.1)
    place(out, tone(glide(140, 60, 0.8, 0.5), 0.8) * decay(n_of(0.8), 0.3) * 0.25, 0.1)   # gears winding down
    return room(out, 1.0, 0.25, 4000)


# =====================================================================================================
# Arcane Scholar: Dissociation, Knowledge Spirit; the Absorption passive
# =====================================================================================================

def s_dissociation():
    seconds = 2.2
    n = n_of(seconds)
    out = silence(seconds)
    beam = sweep(noise(1.4), lambda p: 1500 + 600 * np.sin(p * 12), 0.6) * env_adsr(n_of(1.4), 0.05, 0.1, 0, 0.6, 0.8)
    place(out, beam * 0.6, 0)
    place(out, choir((659.25, 987.77), 1.4, 'ee', 1, 0.05, 0.6, depth=0.003) * 0.06, 0)
    for i in range(30):                                                   # what it touches comes apart in flakes
        place(out, bandpass(noise(0.02), 4000, 9000) * decay(n_of(0.02), 0.006) * RNG.uniform(0.1, 0.4),
              RNG.uniform(0.1, 1.6))
    place(out, tone(glide(3000, 1200, 1.2, 0.5), 1.2) * env_adsr(n_of(1.2), 0.05, 0.1, 0, 0.8, 0.5) * 0.08, 0)
    return room(out, 1.8, 0.4, 6000)


def s_knowledge_spirit():
    seconds = 1.8
    out = silence(seconds)
    place(out, rustle(1.4, 30, 0.0, 1.3) * 0.8, 0)                         # a body turning into pages
    place(out, whoosh(1.2, 600, 4000, 0.5, 0.7) * 0.5, 0.2)
    place(out, whispers(1.2, 14, 1500, 4000, 0.2, 1.3, 0.4), 0.2)
    return room(out, 1.2, 0.35, 5000)


def s_mind_enter():
    seconds = 1.8
    out = silence(seconds)
    place(out, reverse(rustle(0.6, 12, 0.0, 0.5)) * 0.7, 0)
    place(out, choir((185.0, 196.0), 1.2, 'oo', 2, 0.1, 0.8, depth=0.02) * 0.15, 0.4)   # a mind made to reel
    place(out, patter(1.0, 40, 700, 2600, 0.5, 1.4) * 0.35, 0.3)
    place(out, thump(50, 0.4, 0.6, 0.15) * 0.6, 0.45)
    return room(out, 1.4, 0.4, 3500)


def s_absorption():
    seconds = 1.0
    n = n_of(seconds)
    suck = sweep(noise(seconds), lambda p: 3000 * (0.15 ** p), 0.5) * env_adsr(n, 0.3, 0.2, 0, 0.3, 0.6)
    return room(suck * 0.4 + tone(glide(600, 300, seconds, 0.5), seconds) * env_adsr(n, 0.2, 0.2, 0, 0.4, 0.5) * 0.1,
                0.8, 0.3, 4000)


# =====================================================================================================
# Knowledge Magister: Rewrite Principles, Colossus, the Fusion Beam, the steam form; the Analysis passive
# =====================================================================================================

def s_rewrite_principles():
    seconds = 3.0
    n = n_of(seconds)
    out = silence(seconds)
    place(out, scratch(0.9, 8) * 0.5, 0)                                   # the old law crossed out
    place(out, tone(glide(55, 220, 2.2, 0.6), 2.2) * env_adsr(n_of(2.2), 0.4, 0.3, 0, 1.0, 0.7) * 0.4, 0.6)
    place(out, choir((261.63, 329.63, 392.0, 523.25), 2.0, 'ah', 1, 0.8, 1.0, depth=0.002) * 0.07, 0.8)
    place(out, shimmer(2.0, 2000, 8000, True) * 0.25, 0.8)
    return room(out, 2.2, 0.45, 5000)


def s_colossus_assemble():
    seconds = 4.0
    out = silence(seconds)
    place(out, gears(3.4, 24, 55, 0.8) * np.linspace(0.3, 1.0, n_of(3.4)), 0)
    for i in range(12):
        place(out, piston(RNG.uniform(45, 70)) * 0.6, 0.15 + i * 0.25)
    place(out, hiss(1.4, 2000, 8000, 0.1, 1.0) * 0.5, 2.5)
    place(out, boom(1.2, 38, 0.8), 3.1)                                    # it rises to its full height
    return room(out, 1.8, 0.35, 3000)


def s_colossus_step():
    seconds = 1.2
    out = silence(seconds)
    place(out, thump(40, 0.7, 0.6, 0.2) * 1.2, 0)
    place(out, clank(260) * 0.4, 0.02)
    place(out, hiss(0.4, 2500, 7000, 0.01, 0.3) * 0.25, 0.1)
    place(out, lowpass(noise(0.6), 200) * decay(n_of(0.6), 0.2) * 1.0, 0)
    return room(out, 1.0, 0.3, 2500)


def s_fusion_charge():
    seconds = 1.4
    n = n_of(seconds)
    whine = tone(glide(200, 3200, seconds, 1.6), seconds) * env_adsr(n, 0.2, 0.0, 0, 0.05, 1.0) * 0.25
    whine += tone(glide(300, 4800, seconds, 1.6), seconds) * env_adsr(n, 0.2, 0.0, 0, 0.05, 1.0) * 0.12
    return room(whine + hiss(seconds, 3000, 9000, 0.6, 0.1) * 0.3, 0.6, 0.2, 7000)


def s_fusion_beam():
    seconds = 2.2
    n = n_of(seconds)
    out = silence(seconds)
    place(out, boom(1.6, 45, 0.9), 0)
    roar = bandpass(noise(1.6), 300, 6000) * env_adsr(n_of(1.6), 0.02, 0.2, 0, 1.0, 0.8)
    place(out, roar * 0.7, 0)
    place(out, choir((130.81, 196.0, 261.63), 1.4, 'ah', 1, 0.05, 1.0, depth=0.002) * 0.08, 0)
    place(out, sparkle(1.6, 20, 4000, 10000) * 0.2, 0.1)
    return room(out, 2.0, 0.4, 4000)


def s_steam_form():
    seconds = 1.6
    out = silence(seconds)
    place(out, hiss(1.4, 1200, 9000, 0.02, 1.0) * 0.8, 0)
    place(out, rustle(0.8, 12, 0.0, 0.7) * 0.4, 0.1)                       # the scroll of civilizations unrolling
    place(out, chime(1046.5, 0.8, 0.5) * 0.15, 0.2)
    return room(out, 1.2, 0.35, 6000)


def s_steam_reform():
    seconds = 1.2
    out = silence(seconds)
    place(out, reverse(hiss(0.8, 1500, 8000, 0.02, 0.6)) * 0.6, 0)
    place(out, piston(65) * 0.5, 0.75)
    return room(out, 0.9, 0.25, 5000)


def s_analysis():
    seconds = 1.2
    out = silence(seconds)
    place(out, patter(0.5, 24, 1200, 4000) * 0.4, 0)
    place(out, chime(2349.3, 0.9, 0.6) * 0.35, 0.3)
    return room(out, 1.0, 0.4, 6000)


# =====================================================================================================
# Illuminator: Scrolls of Civilization, Mathematical Maze, Masterwork
# =====================================================================================================

def s_scrolls_unfold():
    seconds = 2.6
    out = silence(seconds)
    place(out, rustle(1.4, 40, 0.0, 1.3) * 0.9, 0)                         # great scrolls unrolling
    place(out, choir((196.0, 293.66, 392.0), 2.0, 'oh', 2, 0.6, 1.0, depth=0.004) * 0.08, 0.3)
    place(out, bell(98.0, 2.0, 0.4, (1.0, 2.0, 2.76, 4.1), 2.0) * 0.3, 0.2)
    return room(out, 2.0, 0.45, 4000)


def s_scrolls_release():
    seconds = 1.6
    out = silence(seconds)
    place(out, whoosh(0.8, 300, 3000, 0.3, 0.7) * 0.8, 0)
    place(out, rustle(0.8, 20, 0.0, 0.7) * 0.6, 0)
    place(out, thump(60, 0.4, 0.6, 0.12) * 0.5, 0.1)
    return room(out, 1.4, 0.35, 4000)


def s_immersion():
    seconds = 2.4
    out = silence(seconds)
    place(out, choir((146.83, 220.0, 293.66, 349.23), 2.0, 'ah', 2, 0.3, 1.2, depth=0.006) * 0.1, 0)   # a lost age
    place(out, bell(73.42, 2.2, 0.3, (1.0, 2.0, 3.0), 2.0) * 0.4, 0)
    place(out, whispers(1.6, 16, 900, 3000, 0.3, 2.0, 0.35), 0)
    return room(out, 2.2, 0.55, 3000)


def s_maze_close():
    seconds = 1.8
    out = silence(seconds)
    for i in range(12):                                                   # bars of the lattice locking
        place(out, clank(RNG.uniform(1800, 3200)) * 0.25, 0.05 + i * 0.06)
    place(out, patter(1.2, 50, 1000, 4000, 0.0, 1.2) * 0.35, 0.1)          # equations
    place(out, bell(523.25, 1.0, 0.6, (1.0, 2.0, 3.0, 4.0), 0.8) * 0.3, 0.8)
    return room(out, 1.4, 0.4, 5000)


def s_enlightenment():
    seconds = 2.4
    n = n_of(seconds)
    out = silence(seconds)
    place(out, choir((523.25, 659.25, 783.99, 1046.5), 2.0, 'ah', 1, 0.3, 1.0, depth=0.002) * 0.08, 0)
    for i, f in enumerate((1046.5, 1318.5, 1567.98, 2093.0)):
        place(out, chime(f, 1.2, 0.5) * 0.25, 0.1 + i * 0.1)
    place(out, sparkle(2.0, 16, 3000, 8000) * 0.2, 0)
    return room(out, 2.0, 0.45, 7000)


def s_masterwork():
    """Ten seconds of a symphony: strings, horns and timpani through I - vi - IV - V - I in D."""
    seconds = 10.4
    out = silence(seconds)
    d4, fs4, a4 = 293.66, 369.99, 440.0
    chords = [((146.83, 220.0, 293.66, 369.99), (293.66, 440.0)),         # D
              ((123.47, 246.94, 293.66, 369.99), (246.94, 369.99)),       # Bm
              ((98.0, 196.0, 246.94, 293.66), (196.0, 293.66)),            # G
              ((110.0, 220.0, 277.18, 329.63), (220.0, 329.63)),           # A
              ((146.83, 220.0, 293.66, 369.99), (293.66, 440.0))]          # D
    for i, (low, horns) in enumerate(chords):
        at = i * 2.0
        place(out, strings(low, 2.4, 0.3, 0.6) * 0.18, at)
        place(out, brass(horns, 1.6, 0.06, 0.5) * 0.08, at + 0.1)
        place(out, timpani(low[0] / 2 if low[0] > 120 else low[0]) * 0.6, at)
        if i % 2 == 0:
            place(out, timpani(low[0] / 2 if low[0] > 120 else low[0]) * 0.35, at + 1.0)
    melody = [a4, fs4, d4, fs4, a4, 493.88, 440.0, 392.0, 369.99, 329.63, 293.66, 329.63, 369.99, 293.66]
    for i, f in enumerate(melody):                                         # a violin line over it
        place(out, strings((f * 2,), 0.75, 0.05, 0.3, 3000) * 0.1, 0.2 + i * 0.7)
    place(out, sparkle(seconds, 20, 4000, 9000) * 0.08, 0)
    return room(out, 2.4, 0.4, 5000)


# =====================================================================================================
# Paragon: Reality Knowledge, Illusory Book, Mechanical Heaven
# =====================================================================================================

def s_reality_knowledge():
    seconds = 2.0
    out = silence(seconds)
    place(out, scratch(0.5, 5) * 0.5, 0)
    place(out, thump(40, 0.8, 0.5, 0.3) * 1.0, 0.3)                        # a heavy book slammed shut
    place(out, clank(180) * 0.4, 0.3)
    place(out, reverse(choir((110.0, 116.54), 1.0, 'oo', 2, 0.6, 0.1, depth=0.01)) * 0.15, 0.6)
    place(out, patter(0.6, 30, 800, 3000, 0.0, 0.5) * np.linspace(0.6, 0, n_of(0.6)), 0.4)   # thoughts lost
    return room(out, 1.8, 0.4, 3000)


def s_book_write():
    seconds = 2.4
    out = silence(seconds)
    place(out, rustle(0.8, 16, 0.0, 0.7) * 0.6, 0)                         # the book opens
    place(out, scratch(1.2, 10) * 0.6, 0.5)                                 # they are written in
    place(out, patter(1.4, 60, 900, 3500, 0.5, 1.9) * 0.35, 0)
    place(out, thump(48, 0.5, 0.6, 0.2) * 0.8, 1.9)                         # and it closes on them
    return room(out, 1.6, 0.4, 4000)


def s_book_release():
    seconds = 1.6
    out = silence(seconds)
    place(out, rustle(1.0, 24, 0.0, 0.9) * 0.7, 0)
    place(out, reverse(scratch(0.8, 6)) * 0.4, 0.1)
    place(out, whoosh(0.6, 400, 3000, 0.4, 0.6) * 0.4, 0.5)
    return room(out, 1.2, 0.35, 4500)


def s_mechanical_heaven():
    seconds = 6.0
    n = n_of(seconds)
    out = silence(seconds)
    place(out, gears(5.4, 14, 41.2, 1.0) * env_adsr(n_of(5.4), 1.5, 0.3, 0, 1.8, 0.9), 0)   # great slow gears
    for i in range(6):
        place(out, clank(RNG.uniform(120, 200)) * 0.5, 0.4 + i * 0.8)
        place(out, piston(42) * 0.5, 0.4 + i * 0.8)
    place(out, choir((73.42, 110.0, 146.83, 220.0, 293.66), 4.6, 'ah', 2, 1.6, 1.8, depth=0.003) * 0.08, 0.6)
    place(out, brass((146.83, 220.0, 293.66), 3.0, 1.0, 1.4) * 0.06, 1.8)
    place(out, hiss(4.0, 1500, 8000, 1.0, 1.5) * 0.25, 1.0)
    place(out, bell(146.83, 3.0, 0.5, (1.0, 2.0, 2.76, 4.1), 3.0) * 0.4, 1.6)
    return room(out, 3.0, 0.45, 3500)


# =====================================================================================================
# The melee combo
# =====================================================================================================

def s_gear_swing(variant):
    seconds = 0.6
    out = silence(seconds)
    place(out, whoosh(0.35, 500 + 200 * variant, 3600, 0.45, 0.5) * 0.7, 0)
    place(out, ratchet(0.25, 30, 50, 2400 + 300 * variant, 0.25), 0.05)
    return room(out, 0.4, 0.15, 6000)


def s_page_swing():
    seconds = 0.6
    out = silence(seconds)
    place(out, whoosh(0.35, 700, 4200, 0.45, 0.5) * 0.6, 0)
    place(out, rustle(0.35, 10, 0.0, 0.3) * 0.6, 0.02)
    return room(out, 0.4, 0.15, 6000)


def s_blueprint_strike():
    seconds = 0.9
    out = silence(seconds)
    place(out, whoosh(0.25, 800, 4500, 0.6, 0.5) * 0.6, 0)
    place(out, thump(70, 0.25, 0.6, 0.06) * 0.8, 0.15)
    place(out, chime(3135.96, 0.6, 1.0) * 0.25, 0.16)
    place(out, scratch(0.3, 2) * 0.3, 0.0)
    return room(out, 0.6, 0.2, 5000)


# =====================================================================================================
# The list: event name -> the functions that make its variants
# =====================================================================================================

SOUNDS = {
    'flask_throw': [s_flask_throw],
    'flask_burst': [lambda: s_flask_burst(0), lambda: s_flask_burst(1)],
    'acid_hiss': [s_acid_hiss],
    'recall': [s_recall],
    'survey': [s_survey],
    'ritual_circle': [s_ritual_circle],
    'ritual_pulse': [s_ritual_pulse],
    'appraise': [s_appraise],
    'exploit_flaw': [s_exploit_flaw],
    'construct_break': [s_construct_break],
    'rifle_wind': [s_rifle_wind],
    'rifle_shot': [lambda: s_rifle_shot(0), lambda: s_rifle_shot(1)],
    'cannon_unfold': [s_cannon_unfold],
    'cannon_fire': [s_cannon_fire],
    'cannon_blast': [s_cannon_blast],
    'brooch_pin': [s_brooch_pin],
    'brooch_burst': [s_brooch_burst],
    'star_curse': [s_star_curse],
    'curse_step': [s_curse_step],
    'blessing_stars': [s_blessing_stars],
    'false_constellations': [s_false_constellations],
    'meteor_fall': [s_meteor_fall],
    'meteor_impact': [s_meteor_impact],
    'doll_rise': [s_doll_rise],
    'doll_fall': [s_doll_fall],
    'golem_assemble': [s_golem_assemble],
    'golem_strike': [s_golem_strike],
    'golem_taunt': [s_golem_taunt],
    'mech_on': [s_mech_on],
    'jetpack': [s_jetpack],
    'mech_off': [s_mech_off],
    'dissociation': [s_dissociation],
    'knowledge_spirit': [s_knowledge_spirit],
    'mind_enter': [s_mind_enter],
    'absorption': [s_absorption],
    'rewrite_principles': [s_rewrite_principles],
    'colossus_assemble': [s_colossus_assemble],
    'colossus_step': [s_colossus_step],
    'fusion_charge': [s_fusion_charge],
    'fusion_beam': [s_fusion_beam],
    'steam_form': [s_steam_form],
    'steam_reform': [s_steam_reform],
    'analysis': [s_analysis],
    'scrolls_unfold': [s_scrolls_unfold],
    'scrolls_release': [s_scrolls_release],
    'immersion': [s_immersion],
    'maze_close': [s_maze_close],
    'enlightenment': [s_enlightenment],
    'masterwork': [s_masterwork],
    'reality_knowledge': [s_reality_knowledge],
    'book_write': [s_book_write],
    'book_release': [s_book_release],
    'mechanical_heaven': [s_mechanical_heaven],
    'gear_swing': [lambda: s_gear_swing(0), lambda: s_gear_swing(1)],
    'page_swing': [s_page_swing],
    'blueprint_strike': [s_blueprint_strike],
}

SUBTITLES = {
    'flask_throw': 'A flask is thrown', 'flask_burst': 'A flask bursts', 'acid_hiss': 'Acid hisses',
    'recall': 'Someone steps back in time', 'survey': 'A survey pulse spreads', 'ritual_circle': 'A chalk circle is drawn',
    'ritual_pulse': 'The ritual circle hums', 'appraise': 'Something is appraised', 'exploit_flaw': 'A flaw is struck',
    'construct_break': 'A construct falls apart', 'rifle_wind': 'Clockwork winds', 'rifle_shot': 'A clockwork rifle fires',
    'cannon_unfold': 'A cannon unfolds', 'cannon_fire': 'A spirit cannon fires', 'cannon_blast': 'A cannonball bursts',
    'brooch_pin': 'A brooch is pinned', 'brooch_burst': 'A brooch shatters', 'star_curse': 'A ray of cursed starlight',
    'curse_step': 'A curse deepens', 'blessing_stars': 'Starlight presses down', 'false_constellations': 'A false sky opens',
    'meteor_fall': 'A meteor falls', 'meteor_impact': 'A meteor strikes', 'doll_rise': 'A doll rises from the ground',
    'doll_fall': 'A doll crumbles', 'golem_assemble': 'A golem assembles', 'golem_strike': 'A golem strikes',
    'golem_taunt': 'A golem calls out', 'mech_on': 'Mechanical plates clamp on', 'jetpack': 'A jetpack roars',
    'mech_off': 'Mechanical plates come away', 'dissociation': 'Dissociation beams out',
    'knowledge_spirit': 'Someone turns into pages', 'mind_enter': 'A mind is entered', 'absorption': 'Life is absorbed',
    'rewrite_principles': 'The laws are rewritten', 'colossus_assemble': 'A colossus assembles',
    'colossus_step': 'A colossus steps', 'fusion_charge': 'A fusion beam charges', 'fusion_beam': 'A fusion beam fires',
    'steam_form': 'Someone dissolves into steam', 'steam_reform': 'Steam takes shape again',
    'analysis': 'Something is seen through', 'scrolls_unfold': 'Scrolls of civilization unfold',
    'scrolls_release': 'Scrolls fly outward', 'immersion': 'A vision of a lost age', 'maze_close': 'A lattice of equations closes',
    'enlightenment': 'Enlightenment', 'masterwork': 'A symphony plays', 'reality_knowledge': 'Knowledge is rejected',
    'book_write': 'Someone is written into a book', 'book_release': 'Someone steps out of a book',
    'mechanical_heaven': 'Mechanical Heaven descends', 'gear_swing': 'Gears sweep the air', 'page_swing': 'Pages sweep the air',
    'blueprint_strike': 'A blueprint strike',
}


def main(assets, wav_dir=None):
    folder = os.path.join(assets, 'sounds/illuminator')
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
            files.append({'name': f'circle_of_inevitability:illuminator/{file}'})
            print(f'sounds/illuminator/{file}.ogg  {len(samples) / SR:.2f} s')
        events['illuminator.' + name] = {
            'subtitle': f'subtitles.circle_of_inevitability.illuminator.{name}',
            'sounds': files,
        }
    with open(sounds_json, 'w') as f:
        json.dump(events, f, indent=2)
    lang_path = os.path.join(assets, 'lang/en_us.json')                # the subtitles
    if os.path.exists(lang_path):
        with open(lang_path, encoding='utf-8') as f:
            lang = json.load(f)
        for name, text in SUBTITLES.items():
            lang[f'subtitles.circle_of_inevitability.illuminator.{name}'] = text
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
