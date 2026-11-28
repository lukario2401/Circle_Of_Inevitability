"""Synthesizes the Observer (Sublunary Eye pathway) sounds and writes sounds.json.

python3 tools/model_gen/observer/sounds.py src/main/resources/assets/circle_of_inevitability [--wav dir]

Every sound is built from scratch (sine partials, filtered noise, formant choirs, a synthetic room), so nothing here is
copied from anywhere. The building blocks come from tools/model_gen/beauty_goddess/sounds.py (tone, bell, choir, harp,
room, thump, creak, drone, finish...), tools/model_gen/knowledge_emperor/sounds.py (whoosh, chime, shimmer, scratch,
rustle, whispers...), tools/model_gen/illuminator/sounds.py (boom, hiss, strings) and
tools/model_gen/star_dragon/sounds.py (void_breath, rumble, arc); this file adds the Observer's own: a brush on
canvas, a totem drum, a plucked and a taut string, a dragonfly's wings, paper folding, an eraser. Needs numpy, scipy
and ffmpeg (with libvorbis).

The .ogg files go to sounds/observer/, mono 44.1 kHz (Minecraft only places mono sounds in the world), and each one
gets an event "observer.<name>" in sounds.json and a subtitle in lang/en_us.json. The same names are in
sounds/ModSounds.java (the OBSERVER map, ModSounds.observer("name")): a new sound here needs its name added there too.

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


sky = _load('star_dragon_sounds', os.path.join(_here, '..', 'star_dragon', 'sounds.py'))
kit, scholar, work = sky.kit, sky.scholar, sky.work

SR = kit.SR
RNG = np.random.default_rng(2601)
silence, noise, place, env_adsr, decay, fade = kit.silence, kit.noise, kit.place, kit.env_adsr, kit.decay, kit.fade
lowpass, highpass, bandpass, sweep, tone, glide = kit.lowpass, kit.highpass, kit.bandpass, kit.sweep, kit.tone, kit.glide
bell, choir, room, thump, finish, t_axis = kit.bell, kit.choir, kit.room, kit.thump, kit.finish, kit.t_axis
sparkle, drone, harp, creak, bubbles = kit.sparkle, kit.drone, kit.harp, kit.creak, kit.bubbles
whoosh, chime, shimmer, crackle, reverse, tick = (scholar.whoosh, scholar.chime, scholar.shimmer, scholar.crackle,
                                                  scholar.reverse, scholar.tick)
scratch, rustle, whispers, music_box = scholar.scratch, scholar.rustle, scholar.whispers, scholar.music_box
boom, hiss, strings = work.boom, work.hiss, work.strings
void_breath, rumble, arc = sky.void_breath, sky.rumble, sky.arc


def n_of(seconds):
    return int(seconds * SR)


def rnd(lo, hi):
    return float(RNG.uniform(lo, hi))


# =====================================================================================================
# The Observer's own building blocks
# =====================================================================================================

def brush_stroke(seconds=0.35, bright=2200.0):
    """A loaded brush dragged across canvas: soft band of noise with the bristles' grain, swelling and lifting."""
    n = n_of(seconds)
    x = sweep(noise(seconds), lambda p: bright * (0.8 + 0.4 * np.sin(p * np.pi)), 0.6)
    grain = 1 + 0.35 * np.sin(2 * np.pi * rnd(40, 70) * t_axis(seconds))
    return x * grain * np.sin(np.linspace(0, np.pi, n)) ** 0.8


def painting(seconds=1.0, strokes=5):
    """Painting in the air: several brush strokes overlapping, quick then slower."""
    out = silence(seconds)
    t = 0.0
    for i in range(strokes):
        d = rnd(0.18, 0.34)
        place(out, brush_stroke(d, rnd(1600, 3000)) * rnd(0.5, 1.0), t)
        t += d * rnd(0.45, 0.8)
        if t > seconds - 0.2:
            break
    return out


def wood_knock(freq=180.0, level=1.0):
    """A knock on hollow wood: a short resonant body with a click."""
    d = 0.25
    n = n_of(d)
    body = sum(tone(freq * r, d) * a for r, a in ((1, 1.0), (2.3, 0.5), (3.9, 0.25))) * decay(n, 0.04)
    click = bandpass(noise(0.01), 1500, 6000) * decay(n_of(0.01), 0.002) * 0.5
    out = body * 0.8
    place(out, click, 0)
    return out * level


def drum(freq=70.0, level=1.0):
    """A hide drum: a falling thump with a skin slap."""
    out = thump(freq, 0.5, 0.75, 0.16) * 1.0
    place(out, bandpass(noise(0.05), 300, 1800) * decay(n_of(0.05), 0.012) * 0.5, 0)
    return out * level


def pluck(freq, seconds=1.4, level=1.0):
    """A plucked string: a harp's note with a little buzz at the start."""
    out = harp(freq, seconds) * 0.7
    place(out, bandpass(noise(0.02), freq * 2, freq * 6) * decay(n_of(0.02), 0.005) * 0.3, 0)
    return out * level


def taut_hum(freq, seconds, level=1.0, wobble=5.0):
    """A string drawn taut and humming: bowed partials with a fast tremble."""
    tt = t_axis(seconds)
    vib = 1 + 0.004 * np.sin(2 * np.pi * wobble * tt)
    out = sum(tone(freq * k * vib, seconds) / k ** 1.2 for k in range(1, 7))
    out *= 1 + 0.25 * np.sin(2 * np.pi * (wobble * 2.1) * tt)
    return out * env_adsr(n_of(seconds), min(0.3, seconds / 3), 0.1, 0, min(0.5, seconds / 3), 0.85) * level


def buzz_wings(seconds, rate=36.0, level=1.0):
    """A dragonfly's wings: a fast flutter of airy noise."""
    tt = t_axis(seconds)
    x = bandpass(noise(seconds), 500, 3200) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * rate * tt))) ** 2
    hum = tone(rate * 4, seconds) * 0.15
    return (x + hum) * env_adsr(n_of(seconds), 0.05, 0.1, 0, 0.15, 0.8) * level


def fold(seconds=0.5):
    """Paper folding: a crisp crackle that runs along a crease, then a soft press."""
    out = silence(seconds)
    for i in range(18):
        at = i / 18 * seconds * 0.6
        place(out, highpass(noise(0.01), rnd(2500, 5000)) * decay(n_of(0.01), 0.003) * rnd(0.3, 1.0), at)
    place(out, lowpass(noise(0.12), 900) * decay(n_of(0.12), 0.04) * 0.6, seconds * 0.62)
    return out


def rub(seconds=0.6, strokes=4):
    """An eraser rubbing: short back-and-forth scrubs of low-mid noise."""
    out = silence(seconds)
    d = seconds / strokes
    for i in range(strokes):
        s = bandpass(noise(d * 0.9), 400, 2400) * np.sin(np.linspace(0, np.pi, n_of(d * 0.9))) ** 1.4
        place(out, s * (0.6 + 0.4 * (i % 2)), i * d)
    return out


def eye_open(seconds=0.8, level=1.0):
    """An eye opening: a breath drawn in and a clear chime as it settles."""
    out = silence(seconds)
    place(out, sweep(noise(0.4), lambda p: 600 + 2400 * p, 0.6) * np.linspace(0, 1, n_of(0.4)) ** 1.5 * 0.5, 0)
    place(out, chime(1567.98, 0.6, 0.7) * 0.5, 0.32)
    return out * level


# =====================================================================================================
# Key 1: Territory
# =====================================================================================================

def s_totem_ritual():
    seconds = 3.2
    out = silence(seconds)
    for i in range(12):                                                   # the drum, quickening
        at = 0.02 + sum(0.34 * 0.92 ** k for k in range(i))
        if at < seconds - 0.3:
            place(out, drum(68 if i % 2 == 0 else 82, 0.8 + 0.02 * i), at)
    place(out, choir([110, 164.8], 3.0, 'oo', 2, 1.2, 0.8) * 0.25, 0.1)
    place(out, rustle(2.6, 12) * 0.15, 0.3)
    return room(out, 1.6, 0.35, 3000)


def s_totem_rise():
    seconds = 1.8
    out = silence(seconds)
    place(out, creak(0.8, 18, 40) * 0.5, 0)
    place(out, rumble(0.9, 0.6, 260), 0)
    place(out, drum(56, 1.0), 0.75)
    place(out, eye_open(0.9, 0.8), 0.85)
    return room(out, 1.6, 0.35, 3500)


def s_territory_chime():
    seconds = 1.0
    out = silence(seconds)
    place(out, chime(1046.5, 0.8, 0.6) * 0.5, 0)
    place(out, wood_knock(260, 0.4), 0)
    return room(out, 1.0, 0.3, 5000)


def s_territory_end():
    seconds = 1.2
    out = silence(seconds)
    place(out, creak(0.5, 30, 60) * 0.3, 0)
    place(out, reverse(chime(880.0, 0.7, 0.5)) * 0.4, 0.2)
    return room(out, 1.0, 0.3, 3000)


def s_borrow():
    seconds = 1.0
    out = silence(seconds)
    place(out, whispers(0.8, 14, 900, 3000) * 0.4, 0)
    place(out, sweep(noise(0.7), lambda p: 300 + 1500 * p, 0.6) * np.linspace(0, 1, n_of(0.7)) * 0.4, 0.1)
    return room(out, 1.0, 0.3, 4000)


def s_gale():
    seconds = 1.2
    out = silence(seconds)
    place(out, whoosh(1.0, 200, 2200, 0.3, 0.9) * 1.2, 0)
    place(out, whoosh(0.6, 400, 3000, 0.4, 0.6) * 0.6, 0.15)
    return room(out, 0.8, 0.2, 4000)


def s_earth_spike():
    seconds = 1.2
    out = silence(seconds)
    for i in range(5):
        place(out, thump(90 - i * 6, 0.3, 0.5, 0.08) * 0.8, i * 0.08)
        place(out, lowpass(noise(0.15), 1400) * decay(n_of(0.15), 0.04) * 0.6, i * 0.08)
    place(out, rumble(0.8, 0.5, 300), 0.0)
    return room(out, 1.0, 0.25, 3000)


def s_water_lash():
    seconds = 1.0
    out = silence(seconds)
    place(out, sweep(noise(0.6), lambda p: 3000 - 1800 * p, 0.8) * decay(n_of(0.6), 0.25) * 1.0, 0)
    place(out, bubbles(0.6, 20, 400, 1600) * 0.5, 0.1)
    return room(out, 0.8, 0.25, 5000)


def s_thorn_snare():
    seconds = 1.2
    out = silence(seconds)
    place(out, creak(0.9, 40, 90) * 0.5, 0)
    place(out, crackle(0.9, 40, 1200, 5000) * 0.6, 0.05)
    return room(out, 1.0, 0.3, 3500)


def s_beast_call():
    seconds = 1.6
    out = silence(seconds)
    call = tone(glide(520, 880, 0.5, 0.6), 0.5) * env_adsr(n_of(0.5), 0.05, 0.1, 0, 0.2, 0.8)
    place(out, call * 0.5, 0)
    place(out, tone(glide(880, 660, 0.5), 0.5) * env_adsr(n_of(0.5), 0.05, 0.1, 0, 0.2, 0.8) * 0.4, 0.45)
    place(out, drum(74, 0.6), 0.0)
    return room(out, 1.4, 0.4, 3500)


def s_ink_servants():
    seconds = 1.4
    out = silence(seconds)
    place(out, bubbles(1.0, 30, 180, 700, 0.06) * 0.7, 0)
    place(out, whispers(1.2, 18, 600, 2000, 0.1) * 0.35, 0.1)
    place(out, reverse(chime(659.3, 0.6, 0.5)) * 0.3, 0.6)
    return room(out, 1.2, 0.35, 2500)


def s_servant_strike():
    seconds = 0.5
    out = silence(seconds)
    place(out, whoosh(0.2, 600, 2500, 0.6, 0.5) * 0.6, 0)
    place(out, lowpass(noise(0.08), 900) * decay(n_of(0.08), 0.02) * 0.8, 0.16)
    place(out, bubbles(0.2, 6, 200, 600) * 0.4, 0.16)
    return room(out, 0.5, 0.2, 3000)


def s_painting_world_open():
    seconds = 2.6
    out = silence(seconds)
    place(out, rustle(0.6, 14) * 0.6, 0)                                   # the canvas unrolled
    place(out, void_breath(1.4, True) * 0.8, 0.3)
    place(out, boom(1.4, 38, 0.8), 1.2)
    place(out, shimmer(1.4, 1500, 6000, False) * 0.25, 1.2)
    return room(out, 2.2, 0.45, 2500)


def s_painting_world_close():
    seconds = 1.6
    out = silence(seconds)
    place(out, void_breath(1.0, False) * 0.7, 0)
    place(out, fold(0.5) * 0.6, 0.8)
    return room(out, 1.4, 0.35, 3000)


def s_world_barrier():
    seconds = 0.8
    out = silence(seconds)
    place(out, tone(glide(220, 330, 0.5, 0.5), 0.5) * decay(n_of(0.5), 0.2) * 0.5, 0)
    place(out, shimmer(0.6, 2000, 7000) * 0.4, 0)
    return room(out, 0.8, 0.3, 4000)


# =====================================================================================================
# Key 2: Sight
# =====================================================================================================

def s_true_sight():
    seconds = 1.8
    out = silence(seconds)
    place(out, eye_open(0.9, 1.0), 0)
    place(out, shimmer(1.4, 2500, 9000) * 0.3, 0.2)
    place(out, choir([523.25, 783.99], 1.2, 'ee', 2, 0.3, 0.6) * 0.15, 0.3)
    return room(out, 1.6, 0.4, 6000)


def s_many_angles():
    seconds = 2.0
    out = silence(seconds)
    for i, f in enumerate((1318.5, 1567.98, 1760.0, 2093.0, 2349.3)):     # eyes opening all round
        place(out, chime(f, 0.5, 0.6) * 0.25, 0.08 + i * 0.09)
    place(out, whispers(1.4, 26, 1200, 4200) * 0.3, 0.1)
    return room(out, 1.6, 0.4, 6000)


def s_read_intent():
    seconds = 1.4
    out = silence(seconds)
    place(out, rustle(0.5, 10) * 0.6, 0)
    place(out, scholar.patter(0.7, 40, 1200, 3800) * 0.4, 0.2)
    place(out, chime(1975.5, 0.7, 0.7) * 0.35, 0.65)
    return room(out, 1.2, 0.3, 6000)


def s_observed():
    seconds = 0.6
    out = silence(seconds)
    place(out, tick(2600) * 0.6, 0)
    place(out, chime(2637.0, 0.4, 0.5) * 0.2, 0.01)
    return room(out, 0.6, 0.25, 7000)


def s_seen_through():
    seconds = 1.6
    out = silence(seconds)
    place(out, bell(392.0, 1.4, 0.7) * 0.4, 0)
    place(out, eye_open(0.8, 0.6), 0.05)
    return room(out, 1.4, 0.4, 4000)


# =====================================================================================================
# Key 3: Canvas (and every Painting)
# =====================================================================================================

def s_painting():
    return room(painting(1.0, 6), 0.8, 0.25, 5000)


def s_painting_quick():
    return room(painting(0.45, 3), 0.6, 0.2, 5000)


def s_canvas_hang():
    seconds = 0.6
    out = silence(seconds)
    place(out, wood_knock(170, 0.9), 0)
    place(out, rustle(0.25, 4) * 0.3, 0.03)
    return room(out, 0.6, 0.25, 4000)


def s_painting_seen():
    seconds = 1.2
    out = silence(seconds)
    place(out, sweep(noise(0.6), lambda p: 800 + 4000 * p, 0.5) * np.linspace(0, 1, n_of(0.6)) ** 2 * 0.5, 0)
    place(out, chime(1318.5, 0.8, 0.8) * 0.4, 0.55)
    place(out, sparkle(0.5, 8, 3000, 9000) * 0.25, 0.55)
    return room(out, 1.0, 0.35, 6000)


def s_painting_fade():
    seconds = 1.2
    out = silence(seconds)
    place(out, reverse(brush_stroke(0.5, 1800)) * 0.5, 0)
    place(out, shimmer(0.8, 1500, 6000, False) * 0.3, 0.3)
    return room(out, 1.0, 0.3, 4000)


def s_riot():
    seconds = 1.8
    n = n_of(seconds)
    out = silence(seconds)
    for f in (311.1, 370.0, 415.3, 523.25, 587.3):                          # a swirl of clashing tones, rising
        place(out, tone(glide(f, f * 1.5, seconds, 0.7) * (1 + 0.02 * np.sin(2 * np.pi * rnd(3, 7) * t_axis(seconds))),
                        seconds) * 0.12, 0)
    out *= env_adsr(n, 0.2, 0.2, 0, 0.6, 0.8)
    place(out, sparkle(1.5, 20, 2000, 9000) * 0.3, 0.1)
    place(out, whispers(1.2, 16, 1500, 4000) * 0.25, 0.3)
    return room(out, 1.4, 0.35, 5000)


def s_madness_collapse():
    seconds = 1.4
    out = silence(seconds)
    place(out, tone(glide(600, 120, 1.0, 0.6), 1.0) * decay(n_of(1.0), 0.4) * 0.4, 0)
    place(out, thump(50, 0.5, 0.6, 0.15) * 0.8, 0.7)
    return room(out, 1.2, 0.35, 2500)


def s_itching():
    seconds = 1.4
    out = silence(seconds)
    for i in range(14):                                                    # frantic scratching
        d = rnd(0.04, 0.09)
        s = bandpass(noise(d), 1500, 5000) * np.sin(np.linspace(0, np.pi, n_of(d)))
        place(out, s * rnd(0.4, 1.0), i * 0.085 + rnd(0, 0.02))
    place(out, crackle(1.2, 30, 3000, 8000) * 0.3, 0)
    return room(out, 0.8, 0.2, 5000)


def s_golden_sun():
    seconds = 2.4
    out = silence(seconds)
    place(out, choir([261.6, 329.6, 392.0], 2.2, 'ah', 3, 0.5, 1.0) * 0.4, 0)
    place(out, bell(523.25, 2.0, 0.6) * 0.3, 0.1)
    place(out, shimmer(1.8, 2000, 7000) * 0.2, 0.3)
    return room(out, 2.0, 0.45, 5000)


def s_sun_burn():
    seconds = 0.8
    out = silence(seconds)
    place(out, hiss(0.6, 2500, 8000, 0.01, 0.3) * 0.6, 0)
    place(out, crackle(0.5, 14, 1000, 5000) * 0.5, 0)
    return room(out, 0.6, 0.2, 4000)


def s_exorcise():
    seconds = 1.4
    out = silence(seconds)
    place(out, bell(784.0, 1.2, 1.0) * 0.5, 0)
    place(out, reverse(shimmer(0.6, 2000, 8000)) * 0.4, 0)
    place(out, whoosh(0.6, 1000, 6000, 0.3, 0.6) * 0.4, 0.2)
    return room(out, 1.4, 0.4, 6000)


def s_boat_storm():
    seconds = 2.6
    n = n_of(seconds)
    out = silence(seconds)
    surf = lowpass(noise(seconds), 1200) * (0.5 + 0.5 * np.sin(2 * np.pi * 0.6 * t_axis(seconds)) ** 2)
    place(out, surf * env_adsr(n, 0.3, 0.2, 0, 0.8, 0.8) * 1.0, 0)
    place(out, rumble(1.4, 0.5, 200), 0.6)
    place(out, creak(0.8, 12, 26) * 0.3, 0.4)
    return room(out, 1.6, 0.35, 3000)


def s_wave():
    seconds = 1.0
    out = silence(seconds)
    place(out, sweep(noise(0.8), lambda p: 400 + 1800 * np.sin(p * np.pi), 0.8) * np.sin(np.linspace(0, np.pi, n_of(0.8))) * 1.0, 0)
    return room(out, 0.8, 0.25, 3500)


def s_self_portrait():
    seconds = 2.0
    out = silence(seconds)
    for i, f in enumerate((392.0, 493.9, 587.3, 740.0)):
        place(out, pluck(f, 1.4, 0.5), i * 0.16)
    place(out, painting(0.8, 4) * 0.3, 0)
    return room(out, 1.6, 0.4, 5000)


def s_portrait_echo():
    seconds = 1.0
    out = silence(seconds)
    place(out, reverse(pluck(587.3, 0.5, 0.5)), 0)
    place(out, shimmer(0.6, 2500, 8000) * 0.25, 0.3)
    return room(out, 1.0, 0.45, 5000)


def s_giant_fall():
    seconds = 1.2
    out = silence(seconds)
    place(out, whoosh(1.1, 80, 900, 0.8, 1.0) * 1.4, 0)
    place(out, rustle(0.5, 10) * 0.3, 0)
    return room(out, 1.0, 0.25, 2500)


def s_giant_impact():
    seconds = 2.0
    out = silence(seconds)
    place(out, boom(1.8, 34, 1.2), 0)
    place(out, thump(48, 0.6, 0.5, 0.2) * 1.0, 0)
    place(out, lowpass(noise(0.6), 2000) * decay(n_of(0.6), 0.12) * 0.7, 0.02)
    return room(out, 1.6, 0.3, 2000)


# =====================================================================================================
# Key 4: Fiction
# =====================================================================================================

def s_painted_mark():
    seconds = 0.9
    out = silence(seconds)
    place(out, brush_stroke(0.35, 2600) * 0.8, 0)
    place(out, chime(1174.7, 0.6, 0.6) * 0.35, 0.3)
    return room(out, 0.8, 0.3, 5000)


def s_mark_drive():
    seconds = 1.0
    out = silence(seconds)
    place(out, taut_hum(220, 0.7, 0.4, 7), 0)
    place(out, whoosh(0.5, 300, 2000, 0.5, 0.6) * 0.5, 0.2)
    return room(out, 0.9, 0.3, 4000)


def s_hide_enter():
    seconds = 1.2
    out = silence(seconds)
    place(out, reverse(brush_stroke(0.5, 2200)) * 0.7, 0)
    place(out, sweep(noise(0.4), lambda p: 3000 * 0.2 ** p, 0.5) * np.linspace(1, 0, n_of(0.4)) * 0.5, 0.45)
    place(out, wood_knock(220, 0.3), 0.85)
    return room(out, 1.0, 0.3, 4000)


def s_hide_exit():
    seconds = 1.0
    out = silence(seconds)
    place(out, sweep(noise(0.4), lambda p: 600 * 5 ** p, 0.5) * np.linspace(0, 1, n_of(0.4)) * 0.5, 0)
    place(out, rustle(0.4, 8) * 0.5, 0.35)
    return room(out, 0.9, 0.3, 4000)


def s_painted_face():
    seconds = 1.0
    out = silence(seconds)
    for i in range(3):
        place(out, brush_stroke(0.18, 3200) * 0.5, i * 0.12)
    place(out, chime(1567.98, 0.6, 0.6) * 0.3, 0.38)
    return room(out, 0.9, 0.3, 6000)


def s_manifest():
    seconds = 1.8
    out = silence(seconds)
    place(out, rustle(1.0, 24) * 0.7, 0)
    place(out, bubbles(0.8, 18, 150, 500, 0.07) * 0.5, 0.2)
    place(out, choir([146.8, 220.0], 1.2, 'oh', 2, 0.4, 0.6) * 0.2, 0.5)
    place(out, thump(80, 0.4, 0.6, 0.12) * 0.7, 1.1)
    return room(out, 1.4, 0.35, 3500)


def s_manifest_fade():
    seconds = 1.2
    out = silence(seconds)
    place(out, rustle(0.8, 16, 0.0, 0.8) * 0.5, 0)
    place(out, shimmer(0.8, 1200, 5000, False) * 0.3, 0.2)
    return room(out, 1.0, 0.3, 3500)


def s_knight_strike():
    seconds = 0.6
    out = silence(seconds)
    place(out, whoosh(0.25, 500, 3000, 0.5, 0.5) * 0.7, 0)
    place(out, kit.clank(700) * 0.5, 0.2)
    place(out, rustle(0.2, 4) * 0.3, 0.2)
    return room(out, 0.6, 0.25, 4000)


def s_archer_shot():
    seconds = 0.6
    out = silence(seconds)
    place(out, pluck(196.0, 0.4, 0.6), 0)
    place(out, whoosh(0.3, 1200, 5000, 0.3, 0.4) * 0.5, 0.02)
    return room(out, 0.5, 0.2, 5000)


def s_fantasy_double():
    seconds = 1.8
    out = silence(seconds)
    place(out, choir([329.6, 349.2], 1.4, 'ee', 2, 0.3, 0.8) * 0.3, 0)     # one voice and its slightly wrong twin
    place(out, shimmer(1.2, 2000, 8000) * 0.3, 0.1)
    place(out, rustle(0.5, 8) * 0.3, 0.6)
    return room(out, 1.6, 0.45, 5000)


# =====================================================================================================
# Key 5: Dimension
# =====================================================================================================

def s_phase_in():
    seconds = 1.2
    out = silence(seconds)
    place(out, sweep(noise(0.8), lambda p: 4000 * 0.15 ** p, 0.5) * np.linspace(1, 0, n_of(0.8)) ** 0.6 * 0.6, 0)
    place(out, reverse(chime(1046.5, 0.6, 0.5)) * 0.4, 0.1)
    place(out, tone(glide(440, 330, 0.8), 0.8) * decay(n_of(0.8), 0.3) * 0.15, 0.2)
    return room(out, 1.4, 0.5, 5000)


def s_phase_out():
    seconds = 1.0
    out = silence(seconds)
    place(out, sweep(noise(0.6), lambda p: 500 * 8 ** p, 0.5) * np.linspace(0, 1, n_of(0.6)) * 0.5, 0)
    place(out, chime(1046.5, 0.5, 0.5) * 0.4, 0.5)
    return room(out, 1.0, 0.35, 5000)


def s_wings_unfold():
    seconds = 1.2
    out = silence(seconds)
    place(out, rustle(0.4, 10) * 0.4, 0)
    place(out, buzz_wings(0.9, 34, 0.7), 0.25)
    place(out, sparkle(0.6, 10, 3000, 9000) * 0.25, 0.3)
    return room(out, 0.9, 0.3, 6000)


def s_wings_buzz():
    return room(buzz_wings(0.9, 38, 0.8), 0.6, 0.2, 5000)


def s_wings_fold():
    seconds = 0.7
    out = silence(seconds)
    place(out, buzz_wings(0.3, 30, 0.5), 0)
    place(out, rustle(0.3, 6) * 0.3, 0.25)
    return room(out, 0.6, 0.2, 5000)


def s_painted_lightning():
    seconds = 1.2
    out = silence(seconds)
    place(out, brush_stroke(0.15, 3500) * 0.6, 0)
    for i in range(3):
        place(out, arc(0.25) * 0.6, 0.08 + i * 0.03)
    place(out, boom(0.9, 70, 0.6), 0.1)
    place(out, crackle(0.6, 30, 2000, 8000) * 0.5, 0.1)
    return room(out, 1.0, 0.25, 4000)


def s_passage():
    seconds = 1.4
    out = silence(seconds)
    place(out, highpass(noise(0.3), 1500) * np.linspace(0, 1, n_of(0.3)) * 0.4, 0)   # the tear
    place(out, sweep(noise(0.25), lambda p: 2000 + 3000 * p, 0.3) * decay(n_of(0.25), 0.06) * 0.6, 0.28)
    place(out, void_breath(0.6, True) * 0.6, 0.3)
    place(out, whoosh(0.5, 300, 2400, 0.5, 0.7) * 0.6, 0.6)
    return room(out, 1.2, 0.35, 4000)


def s_hostel_lodge():
    seconds = 1.4
    out = silence(seconds)
    place(out, void_breath(0.8, True) * 0.8, 0)
    place(out, tone(glide(600, 90, 0.6, 0.6), 0.6) * decay(n_of(0.6), 0.25) * 0.4, 0.5)
    place(out, wood_knock(140, 0.6), 1.0)                                 # a door shut far inside
    return room(out, 1.2, 0.45, 3000)


def s_hostel_release():
    seconds = 1.2
    out = silence(seconds)
    place(out, wood_knock(160, 0.6), 0)
    place(out, void_breath(0.7, False) * 0.7, 0.1)
    place(out, tone(glide(120, 500, 0.5, 0.6), 0.5) * decay(n_of(0.5), 0.2) * 0.3, 0.2)
    return room(out, 1.1, 0.4, 3500)


def s_shadow_form():
    seconds = 3.0
    n = n_of(seconds)
    out = silence(seconds)
    place(out, drone(2.6, 41.2, (1, 1.5, 2.01, 3)) * env_adsr(n_of(2.6), 0.6, 0.3, 0, 1.0, 0.8) * 0.8, 0)
    place(out, void_breath(1.4, True) * 0.6, 0)
    place(out, choir([82.4, 87.3], 2.4, 'oo', 2, 0.8, 1.0) * 0.3, 0.4)
    place(out, boom(1.4, 32, 0.8), 1.2)
    return room(out[:n], 2.0, 0.45, 2000)


def s_shadow_form_end():
    seconds = 1.6
    out = silence(seconds)
    place(out, reverse(void_breath(1.0, True)) * 0.6, 0)
    place(out, tone(glide(220, 60, 1.0), 1.0) * decay(n_of(1.0), 0.4) * 0.3, 0.3)
    return room(out, 1.4, 0.4, 2500)


def s_ascension():
    seconds = 1.2
    out = silence(seconds)
    place(out, tone(glide(330, 440, 0.4, 0.5), 0.4) * env_adsr(n_of(0.4), 0.02, 0.1, 0, 0.2, 0.7) * 0.3, 0)
    place(out, chime(1760.0, 0.6, 0.6) * 0.3, 0.3)
    place(out, shimmer(0.6, 3000, 9000) * 0.2, 0.2)
    return room(out, 1.0, 0.4, 6000)


# =====================================================================================================
# Key 6: Strings
# =====================================================================================================

def s_string_pluck():
    return room(pluck(rnd(330, 440), 1.2, 1.0), 1.0, 0.3, 5000)


def s_taut_chord_draw():
    seconds = 1.2
    out = silence(seconds)
    for i, f in enumerate((110.0, 164.8, 220.0)):
        place(out, taut_hum(f, 1.1, 0.35, 5 + i), 0.05 * i)
    place(out, creak(0.6, 60, 140) * 0.2, 0)
    return room(out, 1.0, 0.3, 4000)


def s_taut_chord_play():
    seconds = 2.2
    out = silence(seconds)
    for i, f in enumerate((220.0, 277.2, 329.6, 440.0, 554.4, 659.3)):    # strummed, low to high
        place(out, pluck(f, 1.8, 0.7), i * 0.035)
    place(out, thump(60, 0.4, 0.6, 0.12) * 0.7, 0)
    return room(out, 1.6, 0.35, 5000)


def s_resonance():
    seconds = 1.6
    n = n_of(seconds)
    out = silence(seconds)
    hum = taut_hum(196.0, 1.5, 0.5, 9)
    place(out, hum, 0)
    place(out, pluck(392.0, 1.0, 0.5), 0)
    return room(out[:n], 1.2, 0.35, 4000)


def s_resonance_add():
    seconds = 0.6
    out = silence(seconds)
    place(out, pluck(rnd(500, 700), 0.5, 0.5), 0)
    return room(out, 0.6, 0.3, 5000)


def s_resonance_burst():
    seconds = 1.6
    out = silence(seconds)
    for f in (196.0, 246.9, 293.7, 392.0):
        place(out, pluck(f, 1.2, 0.5), 0)
    place(out, boom(1.2, 60, 0.7), 0)
    place(out, highpass(noise(0.05), 2000) * decay(n_of(0.05), 0.01) * 0.8, 0)
    return room(out, 1.4, 0.35, 4000)


def s_harmony():
    seconds = 2.6
    out = silence(seconds)
    for i, f in enumerate((261.6, 329.6, 392.0, 523.25)):
        place(out, pluck(f, 2.0, 0.5), i * 0.12)
    place(out, strings([261.6, 329.6, 392.0], 2.2, 0.4, 1.0) * 0.3, 0.2)
    place(out, choir([523.25, 659.3], 1.8, 'ah', 2, 0.5, 1.0) * 0.15, 0.4)
    return room(out, 2.0, 0.45, 5000)


def s_dissonance():
    seconds = 2.0
    out = silence(seconds)
    for i, f in enumerate((233.1, 246.9, 349.2, 370.0, 415.3)):          # a broken chord: seconds and tritones
        place(out, pluck(f, 1.6, 0.5), i * 0.05)
    place(out, strings([233.1, 246.9, 329.6, 349.2], 1.6, 0.1, 0.6) * 0.3, 0.1)
    place(out, crackle(0.4, 12, 2000, 7000) * 0.5, 0.25)                  # strings snapping
    return room(out, 1.6, 0.35, 4000)


def s_shadow_blade():
    seconds = 1.2
    out = silence(seconds)
    place(out, highpass(noise(0.12), 2500) * decay(n_of(0.12), 0.03) * 1.0, 0)   # no warning: the cut itself
    place(out, tone(glide(1800, 300, 0.4, 0.5), 0.4) * decay(n_of(0.4), 0.12) * 0.4, 0)
    place(out, thump(70, 0.4, 0.5, 0.1) * 0.8, 0.02)
    place(out, void_breath(0.6, False) * 0.4, 0.15)
    return room(out, 1.0, 0.3, 3500)


def s_shadow_rise():
    seconds = 1.4
    out = silence(seconds)
    place(out, rumble(0.6, 0.6, 300), 0)
    for i in range(5):
        place(out, highpass(noise(0.08), 2500) * decay(n_of(0.08), 0.02) * 0.7, 0.3 + i * 0.07)
    place(out, thump(60, 0.4, 0.5, 0.12) * 0.8, 0.3)
    return room(out, 1.2, 0.3, 3500)


def s_fold_space():
    seconds = 1.4
    out = silence(seconds)
    place(out, fold(0.6) * 1.0, 0)
    place(out, whoosh(0.4, 200, 4000, 0.5, 0.8) * 0.8, 0.4)
    place(out, fold(0.5) * 0.7, 0.75)
    place(out, thump(80, 0.3, 0.6, 0.08) * 0.6, 0.78)
    return room(out, 1.2, 0.3, 4000)


# =====================================================================================================
# Key 7: Sublunary
# =====================================================================================================

def s_foregone_write():
    seconds = 2.2
    out = silence(seconds)
    place(out, scratch(1.4, 9) * 0.9, 0)
    place(out, bell(146.8, 1.8, 0.5) * 0.5, 0.5)
    place(out, choir([146.8, 155.6], 1.4, 'oo', 2, 0.5, 0.8) * 0.15, 0.5)
    return room(out, 2.0, 0.45, 3000)


def s_foregone_fall():
    seconds = 2.4
    out = silence(seconds)
    place(out, bell(98.0, 2.2, 0.6) * 0.9, 0)                             # a toll
    place(out, boom(1.6, 36, 0.6), 0)
    return room(out, 2.0, 0.45, 2500)


def s_foregone_survival():
    seconds = 2.2
    out = silence(seconds)
    place(out, scratch(0.9, 6) * 0.6, 0)
    place(out, bell(523.25, 1.8, 0.8) * 0.4, 0.4)
    place(out, choir([392.0, 523.25, 659.3], 1.6, 'ah', 2, 0.4, 0.9) * 0.25, 0.4)
    return room(out, 2.0, 0.45, 5000)


def s_ward_spent():
    seconds = 2.0
    out = silence(seconds)
    place(out, reverse(shimmer(0.6, 2000, 8000)) * 0.4, 0)
    place(out, bell(659.3, 1.6, 1.0) * 0.6, 0.55)
    place(out, choir([329.6, 493.9, 659.3], 1.2, 'ah', 2, 0.1, 0.9) * 0.3, 0.55)
    return room(out, 1.8, 0.45, 6000)


def s_revival_ritual():
    seconds = 3.0
    n = n_of(seconds)
    out = silence(seconds)
    place(out, drone(2.8, 55, (1, 1.5, 2, 3), 0.25) * env_adsr(n_of(2.8), 1.0, 0.2, 0, 1.0, 0.7) * 0.5, 0)
    place(out, choir([220.0, 329.6], 2.6, 'oo', 2, 1.2, 1.0) * 0.3, 0.2)
    for i in range(4):
        place(out, chime(880.0 * (1.25 ** i), 0.6, 0.5) * 0.2, 0.6 + i * 0.5)
    return room(out[:n], 2.0, 0.45, 4000)


def s_higher_revival():
    seconds = 2.4
    out = silence(seconds)
    place(out, void_breath(0.9, False) * 0.6, 0)
    place(out, sweep(noise(1.0), lambda p: 300 * 10 ** p, 0.5) * np.linspace(0, 1, n_of(1.0)) * 0.5, 0.2)
    place(out, bell(392.0, 1.6, 0.8) * 0.6, 1.0)
    place(out, choir([392.0, 587.3, 784.0], 1.4, 'ah', 2, 0.1, 1.0) * 0.3, 1.0)
    return room(out, 2.0, 0.45, 5000)


def s_bloom():
    seconds = 3.0
    out = silence(seconds)
    place(out, eye_open(1.0, 0.8), 0)
    place(out, choir([523.25, 659.3, 784.0, 1046.5], 2.6, 'ah', 3, 0.8, 1.2) * 0.35, 0.2)
    place(out, music_box([1046.5, 1318.5, 1567.98, 2093.0, 1567.98, 2637.0], 0.18, 2.4) * 0.25, 0.5)
    place(out, sparkle(2.2, 24, 3000, 10000) * 0.25, 0.4)
    return room(out, 2.4, 0.5, 7000)


def s_bloom_flower():
    seconds = 0.8
    out = silence(seconds)
    place(out, chime(rnd(1800, 2600), 0.6, 0.6) * 0.3, 0)
    place(out, sparkle(0.4, 4, 4000, 9000) * 0.2, 0)
    return room(out, 0.8, 0.35, 7000)


def s_fall_silent():
    seconds = 3.0
    n = n_of(seconds)
    out = silence(seconds)
    place(out, choir([523.25, 659.3], 0.8, 'ah', 2, 0.05, 0.6) * 0.3, 0)
    place(out, lowpass(noise(2.6), 400) * np.linspace(0.8, 0, n_of(2.6)) * 0.4, 0.3)
    place(out, kit.heartbeat(0.9, 44) * 0.6, 0.6)
    place(out, kit.heartbeat(0.9, 40) * 0.35, 1.7)                        # the heart, falling silent too
    out *= np.linspace(1, 0.4, n)
    return room(out, 2.4, 0.5, 1500)


def s_silenced():
    seconds = 0.8
    out = silence(seconds)
    place(out, lowpass(noise(0.4), 500) * np.linspace(1, 0, n_of(0.4)) * 0.6, 0)
    place(out, thump(50, 0.4, 0.7, 0.1) * 0.5, 0)
    return room(out, 0.8, 0.4, 1500)


def s_wake():
    seconds = 1.0
    out = silence(seconds)
    place(out, sweep(noise(0.4), lambda p: 400 * 8 ** p, 0.5) * np.linspace(0, 1, n_of(0.4)) * 0.4, 0)
    place(out, chime(1567.98, 0.6, 0.7) * 0.4, 0.35)
    return room(out, 0.9, 0.35, 6000)


def s_erase():
    seconds = 1.8
    out = silence(seconds)
    place(out, rub(0.7, 5) * 0.9, 0)
    place(out, sweep(noise(0.8), lambda p: 6000 * 0.1 ** p, 0.5) * np.linspace(1, 0, n_of(0.8)) * 0.6, 0.6)
    place(out, reverse(bell(220.0, 0.8, 0.5)) * 0.4, 0.5)
    return room(out, 1.4, 0.4, 3000)


def s_erase_return():
    seconds = 1.2
    out = silence(seconds)
    place(out, sweep(noise(0.6), lambda p: 600 * 10 ** p, 0.5) * np.linspace(0, 1, n_of(0.6)) * 0.5, 0)
    place(out, painting(0.5, 3) * 0.5, 0.4)
    place(out, thump(70, 0.3, 0.6, 0.1) * 0.5, 0.85)
    return room(out, 1.0, 0.35, 4000)


def s_unmade():
    seconds = 1.6
    out = silence(seconds)
    place(out, rub(0.5, 4) * 0.6, 0)
    place(out, rustle(0.8, 20) * 0.5, 0.2)
    place(out, sweep(noise(0.8), lambda p: 5000 * 0.1 ** p, 0.5) * np.linspace(1, 0, n_of(0.8)) * 0.5, 0.4)
    return room(out, 1.4, 0.4, 3000)


# =====================================================================================================
# Passives
# =====================================================================================================

def s_spirit_chime():
    seconds = 1.4
    out = silence(seconds)
    place(out, chime(1396.9, 1.0, 0.5) * 0.4, 0)
    place(out, chime(1046.5, 1.0, 0.5) * 0.3, 0.18)
    place(out, whispers(0.6, 6, 900, 2400) * 0.2, 0.1)
    return room(out, 1.2, 0.45, 4000)


def s_prayer():
    seconds = 1.6
    out = silence(seconds)
    place(out, choir([440.0], 1.2, 'oo', 2, 0.3, 0.8) * 0.3, 0)
    place(out, chime(1760.0, 1.0, 0.6) * 0.3, 0.1)
    return room(out, 1.4, 0.45, 5000)


SOUNDS = {name[2:]: [fn] for name, fn in list(globals().items()) if name.startswith('s_') and callable(fn)}

SUBTITLES = {
    'totem_ritual': 'A totem ritual drums', 'totem_rise': 'A totem rises', 'territory_chime': 'Something enters the Territory',
    'territory_end': 'A Territory fades', 'borrow': 'Power is borrowed', 'gale': 'A gale blows', 'earth_spike': 'Earth spikes erupt',
    'water_lash': 'Water lashes', 'thorn_snare': 'Thorns snare', 'beast_call': 'A beast is called',
    'ink_servants': 'Ink Servants gather', 'servant_strike': 'An Ink Servant strikes',
    'painting_world_open': 'A Painting World opens', 'painting_world_close': 'A Painting World closes',
    'world_barrier': 'Something meets a painted edge', 'true_sight': 'True Sight opens', 'many_angles': 'Many eyes open',
    'read_intent': 'Intent is read', 'observed': 'Something is watched', 'seen_through': 'Something is seen through',
    'painting': 'Brush strokes', 'painting_quick': 'A quick brush stroke', 'canvas_hang': 'A canvas is hung',
    'painting_seen': 'A Painting takes hold', 'painting_fade': 'A Painting fades', 'riot': 'Colours riot',
    'madness_collapse': 'Someone collapses', 'itching': 'Frantic scratching', 'golden_sun': 'A golden sun warms',
    'sun_burn': 'Evil burns', 'exorcise': 'Something is unmade', 'boat_storm': 'A painted storm rolls',
    'wave': 'A wave rolls out', 'self_portrait': 'A Self Portrait is painted', 'portrait_echo': 'A portrait repeats a cast',
    'giant_fall': 'A giant fist falls', 'giant_impact': 'A giant fist strikes', 'painted_mark': 'A mark is painted',
    'mark_drive': 'A marked one is driven', 'hide_enter': 'Someone steps into a Painting',
    'hide_exit': 'Someone steps out of a Painting', 'painted_face': 'A face is painted',
    'manifest': 'Fiction becomes real', 'manifest_fade': 'A figure of fiction fades', 'knight_strike': 'A painted knight strikes',
    'archer_shot': 'A painted archer shoots', 'fantasy_double': 'A Fantasy Double appears', 'phase_in': 'Someone slips between worlds',
    'phase_out': 'Someone returns to reality', 'wings_unfold': 'Pixie wings unfold', 'wings_buzz': 'Wings buzz',
    'wings_fold': 'Pixie wings fold', 'painted_lightning': 'Painted lightning strikes', 'passage': 'A Dimensional Passage tears open',
    'hostel_lodge': 'A guest is lodged', 'hostel_release': 'A guest is released', 'shadow_form': 'A Dimensional Shadow rises',
    'shadow_form_end': 'A Dimensional Shadow fades', 'ascension': 'A Dimension rises', 'string_pluck': 'A string is plucked',
    'taut_chord_draw': 'Strings draw taut', 'taut_chord_play': 'Strings are played', 'resonance': 'A string resonates',
    'resonance_add': 'Resonance builds', 'resonance_burst': 'Resonance bursts', 'harmony': 'A chord of Harmony',
    'dissonance': 'A broken chord', 'shadow_blade': 'A shadow cuts', 'shadow_rise': 'Shadows rise from below',
    'fold_space': 'Space folds', 'foregone_write': 'An ending is written', 'foregone_fall': 'A foregone end',
    'foregone_survival': 'A survival is written', 'ward_spent': 'A death does not happen',
    'revival_ritual': 'A revival is arranged', 'higher_revival': 'Someone returns', 'bloom': 'Flowers bloom',
    'bloom_flower': 'A flower blooms', 'fall_silent': 'Everything falls silent', 'silenced': 'A heart falls silent',
    'wake': 'Someone wakes', 'erase': 'Something is erased', 'erase_return': 'Something returns',
    'unmade': 'Conjured things are unmade', 'spirit_chime': 'A spirit chime warns', 'prayer': 'A prayer is heard',
}


def main(assets, wav_dir=None):
    folder = os.path.join(assets, 'sounds/observer')
    os.makedirs(folder, exist_ok=True)
    events = {}
    sounds_json = os.path.join(assets, 'sounds.json')
    if os.path.exists(sounds_json):
        with open(sounds_json) as f:
            events = json.load(f)
    missing = set(SOUNDS) - set(SUBTITLES)
    if missing:
        raise ValueError(f'no subtitle for: {sorted(missing)}')
    written = []
    for name, makers in SOUNDS.items():
        files = []
        for i, make in enumerate(makers):
            file = name if len(makers) == 1 else f'{name}_{i + 1}'
            samples = finish(make())
            kit.encode(samples, os.path.join(folder, file + '.ogg'))
            if wav_dir:
                os.makedirs(wav_dir, exist_ok=True)
                kit.write_wav(samples, os.path.join(wav_dir, file + '.wav'))
            files.append({'name': f'circle_of_inevitability:observer/{file}'})
            written.append(f'sounds/observer/{file}.ogg  {len(samples) / SR:.2f} s')
        events['observer.' + name] = {
            'subtitle': f'subtitles.circle_of_inevitability.observer.{name}',
            'sounds': files,
        }
    with open(sounds_json, 'w') as f:
        json.dump(events, f, indent=2)
    lang_path = os.path.join(assets, 'lang/en_us.json')                # the subtitles
    if os.path.exists(lang_path):
        with open(lang_path, encoding='utf-8') as f:
            lang = json.load(f)
        for name, text in SUBTITLES.items():
            lang[f'subtitles.circle_of_inevitability.observer.{name}'] = text
        with open(lang_path, 'w', encoding='utf-8') as f:
            f.write(json.dumps(lang, indent=2, ensure_ascii=False) + '\n')
    return written


if __name__ == '__main__':
    args = sys.argv[1:]
    wav = None
    if '--wav' in args:
        wav = args[args.index('--wav') + 1]
        args = args[:args.index('--wav')]
    for line in main(args[0], wav):
        print(line)
