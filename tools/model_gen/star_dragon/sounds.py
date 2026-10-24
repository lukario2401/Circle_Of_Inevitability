"""Synthesizes the Star Dragon (Condenser pathway) sounds and writes sounds.json.

python3 tools/model_gen/star_dragon/sounds.py src/main/resources/assets/circle_of_inevitability [--wav dir]

Every sound is built from scratch (sine partials, filtered noise, formant choirs, a synthetic room), so nothing here is
copied from anywhere. The building blocks come from tools/model_gen/beauty_goddess/sounds.py (tone, bell, choir, room,
thump, drone, finish...), tools/model_gen/knowledge_emperor/sounds.py (whoosh, chime, shimmer, crackle...) and
tools/model_gen/illuminator/sounds.py (boom, hiss, strings, brass, timpani); this file adds the sky's own: the hum of
gravity, rays at light speed, the void's breath, electric arcs, a falling star, a black hole. Needs numpy, scipy and
ffmpeg (with libvorbis).

The .ogg files go to sounds/star_dragon/, mono 44.1 kHz (Minecraft only places mono sounds in the world), and each
one gets an event "star_dragon.<name>" in sounds.json and a subtitle in lang/en_us.json. The same names are
registered in sounds/ModSounds.java (STAR_DRAGON_...): a new sound here needs a line there too.

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
work = _load('illuminator_sounds', os.path.join(_here, '..', 'illuminator', 'sounds.py'))

SR = kit.SR
RNG = np.random.default_rng(1601)
silence, noise, place, env_adsr, decay, fade = kit.silence, kit.noise, kit.place, kit.env_adsr, kit.decay, kit.fade
lowpass, highpass, bandpass, sweep, tone, glide = kit.lowpass, kit.highpass, kit.bandpass, kit.sweep, kit.tone, kit.glide
bell, choir, room, thump, finish, t_axis = kit.bell, kit.choir, kit.room, kit.thump, kit.finish, kit.t_axis
sparkle, drone, glass_shards = kit.sparkle, kit.drone, kit.glass_shards
whoosh, chime, shimmer, crackle, reverse, tick = (scholar.whoosh, scholar.chime, scholar.shimmer, scholar.crackle,
                                                  scholar.reverse, scholar.tick)
boom, hiss, strings, brass, timpani = work.boom, work.hiss, work.strings, work.brass, work.timpani


def n_of(seconds):
    return int(seconds * SR)


# =====================================================================================================
# Building blocks of the sky: gravity's hum, a ray, the void's breath, an arc, a falling star
# =====================================================================================================

def gravity_hum(seconds, base=38.0, wobble=0.6, level=1.0):
    """The weight of a field: a sub tone with a slow beating, felt more than heard."""
    n = n_of(seconds)
    tt = t_axis(seconds)
    out = tone(base, seconds) + 0.5 * tone(base * 1.5, seconds) + 0.3 * tone(base * 2.01, seconds)
    out *= 0.7 + 0.3 * np.sin(2 * np.pi * wobble * tt)
    out += lowpass(noise(seconds), 140, 2) * 0.6
    return out * env_adsr(n, min(0.4, seconds / 4), 0.1, 0, min(0.6, seconds / 3), 0.9) * level


def ray_zap(seconds=0.6, f0=2400.0, f1=900.0, grit=0.3):
    """A ray at light speed: a bright tone falling fast, a crack at the front and a sizzle behind."""
    n = n_of(seconds)
    out = silence(seconds)
    body = sum(tone(glide(f0 * k, f1 * k, seconds, 0.4), seconds) / k for k in (1, 2, 3)) * decay(n, seconds / 3)
    place(out, body * 0.6, 0)
    place(out, highpass(noise(0.03), 3000) * decay(n_of(0.03), 0.006) * 1.2, 0)
    place(out, bandpass(noise(seconds), 2500, 9000) * decay(n, seconds / 2.5) * grit, 0.01)
    return out


def void_breath(seconds, inward=True):
    """The void opening or closing: low air drawn in (or let out) through a hollow resonance."""
    n = n_of(seconds)
    x = sweep(noise(seconds), (lambda p: 900 * 0.2 ** p) if inward else (lambda p: 180 * 5 ** p), 0.5)
    hollow = sum(tone(f, seconds) * w for f, w in ((55, 0.5), (82.4, 0.3), (110, 0.2)))
    shape = np.linspace(0, 1, n) ** 1.5 if inward else np.linspace(1, 0, n) ** 1.2
    return (x * 1.4 + hollow * 0.5) * shape


def arc(seconds=0.25):
    """An electric arc: a burst of buzzing at the mains-like rate with a crack."""
    n = n_of(seconds)
    tt = t_axis(seconds)
    buzz = np.sign(np.sin(2 * np.pi * 120 * tt)) * 0.3 + bandpass(noise(seconds), 1500, 8000)
    out = buzz * decay(n, seconds / 3)
    place(out, highpass(noise(0.02), 2000) * decay(n_of(0.02), 0.004) * 1.5, 0)
    return out


def star_ping(freq=1760.0, seconds=1.2, level=1.0):
    """A star answering: a clear bell with a shimmer round it."""
    out = silence(seconds)
    place(out, bell(freq, seconds, 0.6, (1.0, 2.0, 3.01, 4.2), 1.0) * 0.6 * level, 0)
    place(out, sparkle(seconds * 0.8, 6, 4000, 10000) * 0.25 * level, 0.02)
    return out


def falling(seconds, f0=3000.0, f1=200.0, level=1.0):
    """Something falling from the sky: a descending roar of burning air."""
    n = n_of(seconds)
    roar = sweep(noise(seconds), lambda p: f0 * (f1 / f0) ** p, 0.7)
    whistle = tone(glide(f0 * 0.6, f1 * 1.5, seconds, 0.8), seconds) * 0.12
    return (roar + whistle) * np.linspace(0.1, 1, n) ** 1.6 * level


def rumble(seconds, level=1.0, bright=220):
    n = n_of(seconds)
    return lowpass(noise(seconds), bright, 2) * 2.0 * env_adsr(n, seconds * 0.2, 0.1, 0, seconds * 0.5, 0.9) * level


# =====================================================================================================
# Astronomy Aficionado / Star Worshipper: Star Chart, Fate Glimpse
# =====================================================================================================

def s_star_chart():
    seconds = 2.0
    out = silence(seconds)
    for i, f in enumerate((1318.5, 1567.98, 1975.5, 2637.0)):             # stars found, one after another
        place(out, star_ping(f, 1.2, 0.7), i * 0.12)
    place(out, shimmer(1.4, 3000, 9000) * 0.25, 0)
    place(out, scholar.scratch(0.5, 3) * 0.2, 0.15)                        # the chart drawn
    return room(out, 1.8, 0.45, 7000)


def s_fate_glimpse():
    seconds = 2.4
    out = silence(seconds)
    place(out, choir([440, 659.3], 1.8, 'oo', 3, 0.3, 1.0) * 0.4, 0)
    place(out, shimmer(2.0, 2000, 8000) * 0.3, 0.1)
    place(out, star_ping(2093.0, 1.4, 0.6), 0.2)
    return room(out, 2.0, 0.5, 6000)


def s_glimpse_dodge():
    seconds = 0.8
    out = silence(seconds)
    place(out, whoosh(0.35, 600, 4000, 0.4, 0.5) * 0.6, 0)
    place(out, chime(2637.0, 0.6, 1.0) * 0.3, 0.02)
    return room(out, 0.8, 0.3, 7000)


# =====================================================================================================
# Star Sacrificer: Gravity Abnormality, Electromagnetic Attraction, Cosmic Void, Weakening Ray
# =====================================================================================================

def s_gravity_press():
    seconds = 2.2
    out = silence(seconds)
    place(out, gravity_hum(2.0, 36, 0.8) * 1.0, 0)
    place(out, thump(60, 0.6, 0.4, 0.25) * 1.0, 0.05)
    place(out, sweep(noise(1.0), lambda p: 1200 * 0.2 ** p, 0.5) * np.linspace(1, 0, n_of(1.0)) * 0.5, 0.0)
    return room(out, 1.6, 0.35, 2500)


def s_gravity_lift():
    seconds = 2.0
    out = silence(seconds)
    place(out, gravity_hum(1.8, 46, 1.4) * 0.6, 0)
    place(out, sweep(noise(1.2), lambda p: 200 * 8 ** p, 0.5) * np.linspace(0.2, 1, n_of(1.2)) * 0.6, 0.0)
    place(out, shimmer(1.2, 2000, 8000) * 0.25, 0.4)
    return room(out, 1.6, 0.4, 5000)


def s_gravity_slam():
    seconds = 1.4
    out = silence(seconds)
    place(out, sweep(noise(0.25), lambda p: 300 + 2000 * p, 0.5) * np.linspace(0, 1, n_of(0.25)) * 0.6, 0)
    place(out, boom(1.2, 50, 1.0), 0.24)
    return room(out, 1.0, 0.25, 2500)


def s_magnet_pull():
    seconds = 1.2
    n = n_of(seconds)
    out = silence(seconds)
    hum = sum(tone(glide(110 * k, 220 * k, seconds, 1.4), seconds) / k for k in (1, 2, 3, 5)) * 0.3
    place(out, hum * np.linspace(0.2, 1, n), 0)
    place(out, arc(0.2) * 0.4, 0.3)
    place(out, arc(0.2) * 0.5, 0.8)
    return room(out, 1.0, 0.3, 5000)


def s_magnet_collide():
    seconds = 1.2
    out = silence(seconds)
    place(out, thump(80, 0.4, 0.5, 0.12) * 1.0, 0)
    place(out, kit.clank(900) * 0.5, 0)
    place(out, arc(0.35) * 0.6, 0.01)
    place(out, lowpass(noise(0.4), 1200) * decay(n_of(0.4), 0.08) * 0.6, 0)
    return room(out, 1.0, 0.3, 4000)


def s_void_open():
    seconds = 2.2
    out = silence(seconds)
    place(out, void_breath(1.4, True) * 0.8, 0)
    place(out, drone(1.8, 41.2) * 0.4 * np.linspace(0, 1, n_of(1.8)), 0.2)
    for i in range(4):                                                    # far glimmers
        place(out, chime(2000 + i * 400, 0.6, 0.6) * 0.08, 0.8 + i * 0.2)
    return room(out, 2.0, 0.55, 3000)


def s_void_close():
    seconds = 1.4
    out = silence(seconds)
    place(out, void_breath(0.8, False) * 0.8, 0)
    place(out, thump(50, 0.4, 0.6, 0.12) * 0.5, 0.0)
    return room(out, 1.2, 0.45, 3000)


def s_void_freeze():
    seconds = 1.2
    out = silence(seconds)
    place(out, glass_shards(0.5, 10, 0.0, 0.15) * 0.4, 0)
    place(out, crackle(0.8, 30, 3000, 10000) * 0.5, 0.02)
    place(out, sweep(noise(0.8), lambda p: 6000 * 0.4 ** p, 0.4) * np.linspace(1, 0, n_of(0.8)) * 0.4, 0)
    return room(out, 1.0, 0.35, 8000)


def s_ray_warn():
    seconds = 0.6
    n = n_of(seconds)
    out = tone(glide(800, 2400, seconds, 0.8), seconds) * np.linspace(0, 1, n) ** 2 * 0.3
    place(out, shimmer(0.5, 3000, 9000) * 0.25, 0)
    return room(out, 0.4, 0.2, 7000)


def s_ray_weakening():
    seconds = 1.2
    out = silence(seconds)
    place(out, ray_zap(0.7, 1800, 600, 0.35), 0)
    place(out, bandpass(noise(0.6), 300, 1200) * decay(n_of(0.6), 0.2) * 0.4, 0.05)  # flesh sizzling
    place(out, crackle(0.6, 18, 1000, 5000) * 0.3, 0.1)
    return room(out, 1.0, 0.3, 5000)


def s_ray_piercing():
    seconds = 1.0
    out = silence(seconds)
    place(out, ray_zap(0.5, 3200, 1400, 0.2), 0)
    place(out, ray_zap(0.5, 3300, 1450, 0.2) * 0.7, 0.03)                 # the second eye
    return room(out, 0.9, 0.3, 8000)


def s_weakened_melt():
    seconds = 1.6
    out = silence(seconds)
    place(out, hiss(1.4, 800, 4000, 0.1, 0.8) * 0.5, 0)
    place(out, kit.bubbles(1.2, 12, 150, 600, 0.05) * 0.4, 0.1)
    return room(out, 1.0, 0.3, 4000)


# =====================================================================================================
# Navigator: Void Passage, Piercing Ray (above); Tidal Scholar: Tidal Surge, Scaled Form, Sunlight Ray
# =====================================================================================================

def s_void_passage():
    seconds = 1.6
    out = silence(seconds)
    place(out, void_breath(0.5, True) * 0.7, 0)
    place(out, whoosh(0.6, 200, 1600, 0.6, 0.6) * 0.6, 0.35)
    place(out, void_breath(0.5, False) * 0.6, 0.6)
    place(out, star_ping(1318.5, 0.9, 0.4), 0.65)
    return room(out, 1.4, 0.45, 4000)


def s_tidal_surge():
    seconds = 2.6
    n = n_of(seconds)
    out = silence(seconds)
    swell = sweep(noise(2.0), lambda p: 300 + 900 * np.sin(p * np.pi), 0.8)  # water rising and rushing
    place(out, swell * np.sin(np.linspace(0, np.pi, n_of(2.0))) * 1.1, 0)
    place(out, lowpass(noise(1.4), 400) * decay(n_of(1.4), 0.5) * 0.8, 0.8)
    place(out, hiss(1.2, 3000, 10000, 0.05, 0.9) * 0.4, 0.9)               # foam
    place(out, gravity_hum(1.6, 42, 0.5) * 0.3, 0)
    return room(out[:n], 1.6, 0.35, 4000)


def s_surge_parry():
    seconds = 0.9
    out = silence(seconds)
    place(out, bandpass(noise(0.3), 600, 3000) * decay(n_of(0.3), 0.06) * 0.8, 0)  # a slap of water
    place(out, kit.clank(1300) * 0.3, 0.0)
    return room(out, 0.7, 0.3, 5000)


def s_scales_on():
    seconds = 2.0
    out = silence(seconds)
    t = 0.0
    while t < 1.2:                                                         # scales clicking into place
        place(out, tick(RNG.uniform(1800, 3600)) * RNG.uniform(0.3, 0.7), t)
        t += RNG.uniform(0.02, 0.06) * (1.2 - t / 1.5)
    place(out, kit.creak(1.0, 40, 90) * 0.3, 0.1)
    place(out, thump(70, 0.4, 0.6, 0.15) * 0.6, 1.1)
    place(out, star_ping(987.8, 0.9, 0.3), 1.15)
    return room(out, 1.4, 0.35, 5000)


def s_scales_off():
    seconds = 1.2
    out = silence(seconds)
    place(out, scholar.rustle(0.8, 14, 0.0, 0.8) * 0.5, 0)
    place(out, shimmer(0.8, 3000, 8000, False) * 0.25, 0.1)
    return room(out, 1.0, 0.3, 6000)


def s_sunlight_charge():
    seconds = 1.0
    n = n_of(seconds)
    out = sum(tone(glide(220 * k, 880 * k, seconds, 0.7), seconds) / k for k in (1, 2, 3, 4)) * 0.25
    out *= np.linspace(0, 1, n) ** 1.5
    place(out, shimmer(1.0, 2000, 10000) * 0.4, 0)
    return room(out, 0.6, 0.3, 8000)


def s_sunlight_ray():
    seconds = 1.8
    out = silence(seconds)
    place(out, ray_zap(1.0, 1400, 500, 0.5) * 1.0, 0)
    place(out, choir([523.25, 659.3, 783.99], 1.2, 'ah', 2, 0.02, 0.8) * 0.3, 0)
    place(out, lowpass(noise(1.0), 600) * decay(n_of(1.0), 0.3) * 0.6, 0)
    return room(out, 1.4, 0.35, 7000)


# =====================================================================================================
# Heavybringer: Heavybringing, Fate Convergence, Heavy Sphere, Series of Rays, Remote Sight
# =====================================================================================================

def s_heavybringing():
    seconds = 3.2
    out = silence(seconds)
    place(out, gravity_hum(3.0, 30, 0.5) * 1.2, 0)
    place(out, void_breath(2.0, True) * 0.5, 0.3)
    place(out, thump(45, 0.8, 0.5, 0.3) * 0.8, 0.0)
    return room(out, 2.0, 0.4, 2500)


def s_fate_convergence():
    seconds = 3.0
    out = silence(seconds)
    place(out, choir([146.83, 220.0, 293.66], 2.6, 'oo', 3, 0.6, 1.2) * 0.4, 0)
    for i, f in enumerate((2637.0, 2349.3, 2093.0, 1975.5, 1760.0)):       # threads drawn tight, one by one
        place(out, star_ping(f, 0.9, 0.45), 0.2 + i * 0.22)
    place(out, gravity_hum(2.6, 40, 0.3) * 0.4, 0)
    return room(out, 2.4, 0.5, 5000)


def s_misfortune():
    seconds = 1.0
    out = silence(seconds)
    place(out, tone(glide(880, 440, 0.6, 1.0), 0.6) * decay(n_of(0.6), 0.25) * 0.3, 0)  # a falling, wrong note
    place(out, tone(glide(932, 466, 0.6, 1.0), 0.6) * decay(n_of(0.6), 0.25) * 0.2, 0)
    place(out, thump(90, 0.3, 0.6, 0.08) * 0.4, 0.05)
    return room(out, 0.9, 0.35, 4000)


def s_heavy_sphere():
    seconds = 2.0
    out = silence(seconds)
    place(out, void_breath(1.0, True) * 0.6, 0)
    place(out, gravity_hum(1.6, 34, 1.0) * 0.6, 0.2)
    place(out, thump(55, 0.6, 0.7, 0.2) * 0.9, 0.9)                         # the shell shuts
    place(out, bell(220, 1.0, 0.3, (1.0, 1.5, 2.2), 0.6) * 0.3, 0.9)
    return room(out, 1.6, 0.45, 3000)


def s_sphere_block():
    seconds = 0.6
    out = silence(seconds)
    place(out, thump(120, 0.25, 0.7, 0.06) * 0.6, 0)
    place(out, lowpass(noise(0.2), 1500) * decay(n_of(0.2), 0.05) * 0.5, 0)
    return room(out, 0.5, 0.3, 3000)


def s_series_of_rays():
    seconds = 2.2
    out = silence(seconds)
    for i in range(6):
        place(out, ray_zap(0.45, 2000 + i * 120, 700, 0.25) * 0.7, i * 0.2)
    return room(out, 1.4, 0.3, 5000)


def s_remote_sight():
    seconds = 2.4
    out = silence(seconds)
    place(out, shimmer(2.0, 1500, 9000) * 0.4, 0)
    place(out, bell(659.3, 2.0, 0.5, (1.0, 2.0, 3.0), 1.4) * 0.3, 0)
    place(out, whoosh(1.6, 300, 3000, 0.3, 0.9) * 0.3, 0.1)
    return room(out, 2.4, 0.6, 6000)


# =====================================================================================================
# Star Shepherd: Star Grazing, Fission Pulse
# =====================================================================================================

def s_asteroid_fall():
    return room(falling(1.3, 3500, 300, 1.0), 0.8, 0.25, 4000)


def s_asteroid_impact():
    seconds = 2.4
    out = silence(seconds)
    place(out, boom(2.0, 38, 1.3), 0)
    place(out, crackle(1.0, 40, 400, 3000) * 0.6, 0.05)                    # stone and dust raining
    place(out, highpass(noise(0.15), 1200) * decay(n_of(0.15), 0.04) * 0.8, 0)
    return room(out, 1.6, 0.3, 2500)


def s_grazed_star():
    seconds = 2.4
    out = silence(seconds)
    place(out, star_ping(1046.5, 2.0, 0.7), 0)
    place(out, choir([523.25, 783.99], 2.0, 'oo', 2, 0.5, 1.0) * 0.25, 0.1)
    return room(out, 2.0, 0.5, 7000)


def s_fission_glow():
    seconds = 2.0
    n = n_of(seconds)
    tt = t_axis(seconds)
    rate = 4 + 20 * (tt / seconds) ** 2                                    # a counter ticking faster
    clicks = silence(seconds)
    t = 0.0
    while t < seconds - 0.03:
        place(clicks, tick(RNG.uniform(2500, 4500)) * 0.5, t)
        t += 1.0 / (4 + 30 * (t / seconds) ** 2) * RNG.uniform(0.6, 1.4)
    hum = tone(glide(120, 360, seconds, 1.5), seconds) * 0.2 * np.linspace(0.3, 1, n)
    return room(clicks + hum, 0.8, 0.25, 6000)


def s_fission_burst():
    seconds = 2.2
    out = silence(seconds)
    place(out, boom(1.6, 55, 1.0), 0)
    place(out, highpass(noise(1.6), 3000) * decay(n_of(1.6), 0.4) * 0.5, 0)
    place(out, crackle(1.4, 60, 2000, 9000) * 0.5, 0.05)                   # the radiation's crackle
    return room(out, 1.4, 0.3, 5000)


def s_radiation():
    seconds = 2.0
    out = crackle(seconds, 70, 2000, 9000) * 0.7
    place(out, tone(60, seconds) * 0.05, 0)
    return room(out, 0.6, 0.2, 7000)


def s_arc():
    seconds = 0.7
    out = silence(seconds)
    place(out, arc(0.3) * 0.9, 0)
    place(out, arc(0.15) * 0.5, 0.18)
    return room(out, 0.6, 0.25, 6000)


# =====================================================================================================
# Radiant Angel: Fusion Cluster, Radiant Angel Form
# =====================================================================================================

def s_fusion_condense():
    seconds = 1.8
    n = n_of(seconds)
    out = silence(seconds)
    place(out, sum(tone(glide(80 * k, 320 * k, 1.2, 0.6), 1.2) / k for k in (1, 2, 3, 4)) * 0.3
          * np.linspace(0, 1, n_of(1.2)), 0)
    place(out, arc(0.3) * 0.3, 0.5)
    place(out, shimmer(1.5, 2000, 9000) * 0.3, 0.1)
    return room(out[:n], 1.2, 0.3, 6000)


def s_fusion_launch():
    seconds = 1.0
    out = silence(seconds)
    place(out, whoosh(0.6, 400, 4000, 0.3, 0.6) * 0.8, 0)
    place(out, tone(glide(600, 1800, 0.4, 0.5), 0.4) * decay(n_of(0.4), 0.15) * 0.25, 0)
    return room(out, 0.8, 0.3, 6000)


def s_fusion_burst():
    seconds = 2.0
    out = silence(seconds)
    place(out, boom(1.5, 60, 1.0), 0)
    place(out, choir([783.99, 1046.5], 1.0, 'ah', 2, 0.01, 0.8) * 0.2, 0)  # light, sung
    place(out, highpass(noise(0.8), 2500) * decay(n_of(0.8), 0.2) * 0.4, 0)
    return room(out, 1.4, 0.35, 6000)


def s_radiant_angel():
    seconds = 4.0
    out = silence(seconds)
    place(out, choir([261.63, 329.63, 392.0, 523.25], 3.6, 'ah', 3, 1.2, 1.4) * 0.55, 0)
    place(out, brass([130.81, 196.0], 2.2, 0.6, 1.0) * 0.25, 1.2)
    for i in range(6):                                                     # six pairs of wings unfolding
        place(out, whoosh(0.5, 300, 2400, 0.5, 0.6) * 0.3, 0.6 + i * 0.25)
    place(out, shimmer(3.0, 2000, 10000) * 0.3, 0.4)
    return room(out, 3.0, 0.55, 7000)


def s_radiance():
    seconds = 1.0
    out = silence(seconds)
    place(out, hiss(0.8, 3000, 9000, 0.1, 0.5) * 0.3, 0)
    place(out, chime(3136.0, 0.6, 0.8) * 0.15, 0.05)
    return room(out, 0.6, 0.3, 8000)


def s_form_end():
    seconds = 2.0
    out = silence(seconds)
    place(out, choir([392.0, 293.66], 1.6, 'oo', 2, 0.1, 1.0) * np.linspace(1, 0, n_of(1.6)) * 0.4, 0)
    place(out, shimmer(1.4, 1500, 7000, False) * 0.3, 0)
    place(out, thump(60, 0.4, 0.6, 0.15) * 0.4, 0.3)
    return room(out, 1.6, 0.45, 5000)


# =====================================================================================================
# Star Dragon: Gravitational Time Dilation, Planetfall, Star Dragon Form
# =====================================================================================================

def s_time_dilation():
    seconds = 3.0
    out = silence(seconds)
    for i in range(8):                                                     # a clock that slows to nothing
        place(out, tick(2400) * 0.5 * (1 - i / 9), sum(0.12 * 1.35 ** k for k in range(i)))
    place(out, reverse(bell(392.0, 2.0, 0.4, (1.0, 2.0, 2.76), 1.2)) * 0.3, 0.3)
    place(out, gravity_hum(2.6, 32, 0.15) * 0.6, 0.2)
    return room(out, 2.4, 0.5, 4000)


def s_time_hang():
    seconds = 0.9
    out = silence(seconds)
    place(out, tone(glide(1200, 1100, 0.8, 1.0), 0.8) * np.linspace(1, 0, n_of(0.8)) * 0.2, 0)
    place(out, shimmer(0.7, 3000, 7000, False) * 0.2, 0)
    return room(out, 0.8, 0.4, 6000)


def s_planet_descend():
    seconds = 4.2
    n = n_of(seconds)
    out = silence(seconds)
    place(out, falling(4.0, 1600, 120, 0.9), 0)
    place(out, rumble(4.0, 1.2), 0)
    place(out, gravity_hum(4.0, 28, 0.25) * np.linspace(0.2, 1, n_of(4.0)) * 0.8, 0)
    place(out, strings([65.41, 98.0, 130.81], 4.0, 1.5, 0.3, 900) * 0.25, 0)
    return room(out[:n], 1.6, 0.3, 2500)


def s_planet_impact():
    seconds = 4.5
    out = silence(seconds)
    place(out, boom(3.5, 28, 1.6), 0)
    place(out, boom(2.0, 45, 0.8), 0.25)
    place(out, rumble(4.0, 1.4, 160), 0.1)
    place(out, highpass(noise(0.3), 1500) * decay(n_of(0.3), 0.06) * 1.0, 0)
    place(out, crackle(2.5, 80, 300, 2500) * 0.4, 0.2)
    return room(out, 2.6, 0.35, 2000)


def s_light_cracks():
    seconds = 1.6
    out = silence(seconds)
    place(out, crackle(1.2, 50, 2000, 9000) * 0.5, 0)
    place(out, hiss(1.4, 4000, 10000, 0.05, 0.9) * 0.2, 0)
    place(out, chime(2793.8, 0.8, 0.6) * 0.1, 0.1)
    return room(out, 1.0, 0.3, 8000)


def s_star_dragon():
    seconds = 4.6
    out = silence(seconds)
    place(out, shimmer(2.0, 2000, 10000) * 0.4, 0)
    place(out, gravity_hum(3.0, 34, 0.4) * 0.7, 0.2)
    roar_s = 2.4                                                            # the roar: a formant growl, falling
    roar = sum(tone(glide(110 * k, 70 * k, roar_s, 1.2), roar_s) / k for k in range(1, 12))
    roar = sweep(roar + bandpass(noise(roar_s), 200, 2000) * 1.5, lambda p: 900 - 500 * p, 0.9)
    roar *= env_adsr(n_of(roar_s), 0.15, 0.3, 0, 1.2, 0.8)
    place(out, roar * 1.1, 1.4)
    place(out, choir([146.83, 220.0, 329.63], 2.8, 'ah', 3, 0.8, 1.2) * 0.3, 1.2)
    for i in range(2):
        place(out, whoosh(0.8, 120, 900, 0.5, 0.7) * 0.6, 0.6 + i * 0.9)  # vast wings
    return room(out, 3.2, 0.5, 4000)


def s_dragon_wing():
    seconds = 1.2
    out = silence(seconds)
    place(out, whoosh(0.9, 90, 700, 0.45, 0.7) * 1.0, 0)
    place(out, lowpass(noise(0.5), 200) * decay(n_of(0.5), 0.15) * 0.6, 0.3)
    return room(out, 1.0, 0.3, 2500)


def s_dragon_shot():
    seconds = 1.2
    out = silence(seconds)
    place(out, whoosh(0.6, 300, 3000, 0.25, 0.6) * 0.8, 0)
    place(out, falling(0.6, 2400, 500, 0.5), 0.05)
    return room(out, 0.8, 0.25, 4000)


# =====================================================================================================
# Condenser: Neutron Star, Singularity, Quantum Uncertainty
# =====================================================================================================

def s_neutron_star():
    seconds = 3.6
    n = n_of(seconds)
    out = silence(seconds)
    place(out, sweep(noise(1.2), lambda p: 4000 * 0.05 ** p, 0.5) * np.linspace(0.3, 1, n_of(1.2)) * 0.8, 0)  # crushing in
    place(out, boom(1.6, 32, 1.2), 1.1)
    tt = t_axis(2.2)
    pulse = (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 3.0 * tt))) * tone(880, 2.2) * 0.12   # the pulsar's beat
    place(out, pulse * np.linspace(1, 0, n_of(2.2)), 1.3)
    place(out, gravity_hum(2.4, 26, 0.6) * 0.8, 1.1)
    return room(out[:n], 2.2, 0.4, 3000)


def s_neutron_crush():
    seconds = 0.8
    out = silence(seconds)
    place(out, thump(50, 0.5, 0.5, 0.15) * 1.0, 0)
    place(out, kit.creak(0.4, 30, 60) * 0.3, 0.02)
    return room(out, 0.6, 0.25, 2000)


def s_singularity_gather():
    seconds = 3.2
    n = n_of(seconds)
    out = silence(seconds)
    place(out, reverse(boom(2.6, 40, 1.0)) * 0.6, 0.4)                       # everything drawn back in
    place(out, sweep(noise(3.0), lambda p: 8000 * 0.02 ** p, 0.5) * np.linspace(0.2, 1, n_of(3.0)) * 0.6, 0)
    place(out, choir([110.0, 116.54], 3.0, 'oo', 3, 1.5, 0.3) * 0.3, 0)    # a beating, uneasy chord
    return room(out[:n], 1.6, 0.4, 3000)


def s_singularity_open():
    seconds = 4.0
    out = silence(seconds)
    place(out, boom(2.0, 30, 1.2), 0)
    place(out, void_breath(3.6, True) * 1.0, 0.1)
    place(out, gravity_hum(3.8, 24, 0.9) * 1.0, 0)
    place(out, drone(3.6, 36.7, (1, 1.5, 2.02, 3.03), 0.9) * 0.4, 0.2)
    return room(out, 3.0, 0.5, 2500)


def s_singularity_collapse():
    seconds = 3.0
    out = silence(seconds)
    place(out, reverse(void_breath(1.0, False)) * 0.8, 0)
    place(out, thump(36, 1.2, 0.3, 0.5) * 1.2, 0.95)
    place(out, star_ping(1318.5, 1.6, 0.5), 1.0)                            # light, let go
    return room(out, 2.4, 0.5, 4000)


def s_quantum():
    seconds = 2.6
    out = silence(seconds)
    for i in range(40):                                                    # notes that cannot decide where they are
        f = RNG.choice([523.25, 587.33, 659.3, 783.99, 880.0, 1046.5, 1174.7])
        place(out, chime(f, 0.4, 0.8) * RNG.uniform(0.04, 0.12), RNG.uniform(0, 2.0))
    place(out, shimmer(2.2, 2000, 9000) * 0.25, 0.1)
    place(out, sweep(noise(2.2), lambda p: 800 + 2000 * abs(np.sin(p * 9)), 0.4) * 0.2, 0.1)
    return room(out, 2.2, 0.5, 6000)


def s_quantum_slip():
    seconds = 0.8
    out = silence(seconds)
    place(out, tone(glide(2000, 800, 0.15, 1.0), 0.15) * 0.3, 0)
    place(out, tone(glide(700, 2400, 0.15, 1.0), 0.15) * 0.3, 0.12)
    place(out, sparkle(0.5, 6, 4000, 10000) * 0.3, 0.1)
    return room(out, 0.6, 0.3, 7000)


# =====================================================================================================
# Passives
# =====================================================================================================

def s_intuition():
    seconds = 1.6
    out = silence(seconds)
    place(out, chime(1567.98, 1.2, 0.5) * 0.4, 0)
    place(out, chime(2093.0, 1.0, 0.5) * 0.25, 0.08)
    return room(out, 1.6, 0.5, 6000)


def s_stargazing():
    return room(star_ping(2349.3, 1.4, 0.4), 1.6, 0.5, 8000)


def s_dense_step():
    seconds = 0.7
    out = silence(seconds)
    place(out, thump(42, 0.5, 0.6, 0.18) * 1.0, 0)
    place(out, lowpass(noise(0.3), 300) * decay(n_of(0.3), 0.08) * 0.5, 0)
    return room(out, 0.6, 0.2, 1500)


SOUNDS = {
    'star_chart': [s_star_chart],
    'fate_glimpse': [s_fate_glimpse],
    'glimpse_dodge': [s_glimpse_dodge],
    'gravity_press': [s_gravity_press],
    'gravity_lift': [s_gravity_lift],
    'gravity_slam': [s_gravity_slam],
    'magnet_pull': [s_magnet_pull],
    'magnet_collide': [s_magnet_collide],
    'void_open': [s_void_open],
    'void_close': [s_void_close],
    'void_freeze': [s_void_freeze],
    'ray_warn': [s_ray_warn],
    'ray_weakening': [s_ray_weakening],
    'ray_piercing': [s_ray_piercing],
    'weakened_melt': [s_weakened_melt],
    'void_passage': [s_void_passage],
    'tidal_surge': [s_tidal_surge],
    'surge_parry': [s_surge_parry],
    'scales_on': [s_scales_on],
    'scales_off': [s_scales_off],
    'sunlight_charge': [s_sunlight_charge],
    'sunlight_ray': [s_sunlight_ray],
    'heavybringing': [s_heavybringing],
    'fate_convergence': [s_fate_convergence],
    'misfortune': [s_misfortune],
    'heavy_sphere': [s_heavy_sphere],
    'sphere_block': [s_sphere_block],
    'series_of_rays': [s_series_of_rays],
    'remote_sight': [s_remote_sight],
    'asteroid_fall': [s_asteroid_fall],
    'asteroid_impact': [s_asteroid_impact],
    'grazed_star': [s_grazed_star],
    'fission_glow': [s_fission_glow],
    'fission_burst': [s_fission_burst],
    'radiation': [s_radiation],
    'arc': [s_arc],
    'fusion_condense': [s_fusion_condense],
    'fusion_launch': [s_fusion_launch],
    'fusion_burst': [s_fusion_burst],
    'radiant_angel': [s_radiant_angel],
    'radiance': [s_radiance],
    'form_end': [s_form_end],
    'time_dilation': [s_time_dilation],
    'time_hang': [s_time_hang],
    'planet_descend': [s_planet_descend],
    'planet_impact': [s_planet_impact],
    'light_cracks': [s_light_cracks],
    'star_dragon': [s_star_dragon],
    'dragon_wing': [s_dragon_wing],
    'dragon_shot': [s_dragon_shot],
    'neutron_star': [s_neutron_star],
    'neutron_crush': [s_neutron_crush],
    'singularity_gather': [s_singularity_gather],
    'singularity_open': [s_singularity_open],
    'singularity_collapse': [s_singularity_collapse],
    'quantum': [s_quantum],
    'quantum_slip': [s_quantum_slip],
    'intuition': [s_intuition],
    'stargazing': [s_stargazing],
    'dense_step': [s_dense_step],
}

SUBTITLES = {
    'star_chart': 'The stars are charted', 'fate_glimpse': 'Fate is glimpsed', 'glimpse_dodge': 'Someone slips aside',
    'gravity_press': 'Gravity presses down', 'gravity_lift': 'Gravity lets go', 'gravity_slam': 'Someone is slammed down',
    'magnet_pull': 'A magnetic pull', 'magnet_collide': 'Bodies collide', 'void_open': 'A void opens',
    'void_close': 'A void closes', 'void_freeze': 'The void freezes', 'ray_warn': 'A ray takes aim',
    'ray_weakening': 'A weakening ray fires', 'ray_piercing': 'Piercing rays fire', 'weakened_melt': 'Flesh melts',
    'void_passage': 'Someone passes through the void', 'tidal_surge': 'A wave surges', 'surge_parry': 'A blow is parried',
    'scales_on': 'Scales spread', 'scales_off': 'Scales fall away', 'sunlight_charge': 'Sunlight gathers',
    'sunlight_ray': 'A ray of sunlight fires', 'heavybringing': 'Gravity gathers', 'fate_convergence': 'Fate converges',
    'misfortune': 'Misfortune strikes', 'heavy_sphere': 'The air turns dense', 'sphere_block': 'Something stops at a shell',
    'series_of_rays': 'A series of rays fires', 'remote_sight': 'Sight reaches far', 'asteroid_fall': 'An asteroid falls',
    'asteroid_impact': 'An asteroid strikes', 'grazed_star': 'A grazed star hums', 'fission_glow': 'Nuclei loosen',
    'fission_burst': 'Fission bursts', 'radiation': 'Radiation crackles', 'arc': 'An electric arc',
    'fusion_condense': 'Fusion clusters condense', 'fusion_launch': 'A fusion cluster flies',
    'fusion_burst': 'A fusion cluster bursts', 'radiant_angel': 'A Radiant Angel unfolds', 'radiance': 'Radiance burns',
    'form_end': 'A form fades', 'time_dilation': 'Time slows', 'time_hang': 'Something hangs in time',
    'planet_descend': 'Something vast descends', 'planet_impact': 'Planetfall', 'light_cracks': 'Cracks of light burn',
    'star_dragon': 'A Star Dragon roars', 'dragon_wing': 'Vast wings beat', 'dragon_shot': 'An asteroid is launched',
    'neutron_star': 'A body grows impossibly dense', 'neutron_crush': 'Crushing weight',
    'singularity_gather': 'Light drains away', 'singularity_open': 'A black hole opens',
    'singularity_collapse': 'A black hole closes', 'quantum': 'Certainty dissolves', 'quantum_slip': 'Something slips',
    'intuition': 'A warning chime', 'stargazing': 'Starlight answers', 'dense_step': 'A heavy step',
}


def main(assets, wav_dir=None):
    folder = os.path.join(assets, 'sounds/star_dragon')
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
            files.append({'name': f'circle_of_inevitability:star_dragon/{file}'})
            print(f'sounds/star_dragon/{file}.ogg  {len(samples) / SR:.2f} s')
        events['star_dragon.' + name] = {
            'subtitle': f'subtitles.circle_of_inevitability.star_dragon.{name}',
            'sounds': files,
        }
    with open(sounds_json, 'w') as f:
        json.dump(events, f, indent=2)
    lang_path = os.path.join(assets, 'lang/en_us.json')                # the subtitles
    if os.path.exists(lang_path):
        with open(lang_path, encoding='utf-8') as f:
            lang = json.load(f)
        for name, text in SUBTITLES.items():
            lang[f'subtitles.circle_of_inevitability.star_dragon.{name}'] = text
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
