"""Synthesizes the Beauty Goddess sounds and writes sounds.json.

python3 tools/model_gen/beauty_goddess/sounds.py src/main/resources/assets/circle_of_inevitability [--wav dir]

Every sound is built from scratch (sine partials, filtered noise, formant choirs, a synthetic room), so nothing
here is copied from anywhere. Needs numpy, scipy and ffmpeg (with libvorbis). The .ogg files go to
sounds/beauty_goddess/, mono 44.1 kHz (Minecraft only places mono sounds in the world), and each one gets an
event "beauty_goddess.<name>" in sounds.json and a subtitle in lang/en_us.json. The same names are registered in
sounds/ModSounds.java: a new sound here needs a line there too.

To change one: edit its function, run this again, press F3+T in game (sounds reload with the resources).
--wav dir also writes .wav copies there, to listen to them outside the game.
"""
import json
import math
import os
import subprocess
import sys
import tempfile

import numpy as np
from scipy import signal

SR = 44100
RNG = np.random.default_rng(1729)


# =====================================================================================================
# Building blocks
# =====================================================================================================

def t_axis(seconds):
    return np.arange(int(seconds * SR)) / SR


def silence(seconds):
    return np.zeros(int(seconds * SR))


def noise(seconds):
    return RNG.standard_normal(int(seconds * SR))


def place(out, sound, at):
    """Adds 'sound' into 'out' starting 'at' seconds in (cut at the end of out)."""
    i = int(at * SR)
    if i >= len(out):
        return out
    n = min(len(sound), len(out) - i)
    out[i:i + n] += sound[:n]
    return out


def env_adsr(n, a, d, s, r, sustain=0.7):
    """a, d, r in seconds; the rest of n is the sustain."""
    a_n, d_n, r_n = int(a * SR), int(d * SR), int(r * SR)
    s_n = max(0, n - a_n - d_n - r_n)
    e = np.concatenate([
        np.linspace(0, 1, max(1, a_n), endpoint=False),
        np.linspace(1, sustain, max(1, d_n), endpoint=False),
        np.full(s_n, sustain),
        np.linspace(sustain, 0, max(1, r_n)),
    ])
    return np.pad(e, (0, max(0, n - len(e))))[:n]


def decay(n, seconds):
    return np.exp(-np.arange(n) / (seconds * SR))


def fade(x, fin=0.01, fout=0.05):
    x = x.copy()
    a, b = int(fin * SR), int(fout * SR)
    if a:
        x[:a] *= np.linspace(0, 1, a)
    if b:
        x[-b:] *= np.linspace(1, 0, b)
    return x


def sos(kind, freq, order=2):
    nyq = SR / 2
    if isinstance(freq, (tuple, list)):
        freq = [min(f, nyq * 0.98) / nyq for f in freq]
    else:
        freq = min(freq, nyq * 0.98) / nyq
    return signal.butter(order, freq, btype=kind, output='sos')


def lowpass(x, f, order=2):
    return signal.sosfilt(sos('lowpass', f, order), x)


def highpass(x, f, order=2):
    return signal.sosfilt(sos('highpass', f, order), x)


def bandpass(x, lo, hi, order=2):
    return signal.sosfilt(sos('bandpass', (lo, hi), order), x)


def sweep(x, centers, width=0.6, floor=0.0):
    """A band-pass whose middle moves: 'centers' is a function of time (0..1 over x) -> Hz. 'width' in octaves."""
    f, tt, z = signal.stft(x, SR, nperseg=1024, noverlap=768)
    freqs = f[:, None]
    for j, tj in enumerate(tt):
        c = centers(min(1.0, tj / max(1e-9, len(x) / SR)))
        octaves = np.log2(np.maximum(freqs[:, 0], 1.0) / c)
        z[:, j] *= floor + (1 - floor) * np.exp(-(octaves / width) ** 2)
    _, y = signal.istft(z, SR, nperseg=1024, noverlap=768)
    return np.pad(y, (0, max(0, len(x) - len(y))))[:len(x)]


def tone(freq, seconds, phase=0.0):
    """A sine; freq may be a number or an array (a glide) as long as the sound."""
    n = int(seconds * SR)
    if np.isscalar(freq):
        return np.sin(2 * np.pi * freq * np.arange(n) / SR + phase)
    f = np.asarray(freq)[:n]
    return np.sin(np.cumsum(2 * np.pi * f / SR) + phase)


def glide(f0, f1, seconds, curve=1.0):
    t = np.linspace(0, 1, int(seconds * SR)) ** curve
    return f0 * (f1 / f0) ** t


def bell(freq, seconds, bright=1.0, ratios=(0.5, 1.0, 1.19, 1.56, 2.0, 2.51, 2.66, 3.01, 4.1), decay_s=1.6):
    """An inharmonic bell or chime: partials that die away, the high ones faster."""
    n = int(seconds * SR)
    out = np.zeros(n)
    for k, r in enumerate(ratios):
        f = freq * r
        if f > SR * 0.45:
            continue
        amp = (1.0 / (1 + k * 0.6)) * (bright if r > 2 else 1.0)
        out += amp * tone(f * (1 + RNG.uniform(-0.002, 0.002)), seconds, RNG.uniform(0, 6.28)) * decay(n, decay_s / (0.6 + r * 0.5))
    out[:int(0.002 * SR)] *= np.linspace(0, 1, int(0.002 * SR))
    return out


FORMANTS = {
    'ah': ((800, 80, 1.0), (1150, 90, 0.5), (2900, 120, 0.25), (3900, 130, 0.12)),
    'oo': ((350, 60, 1.0), (600, 80, 0.35), (2400, 100, 0.1), (3300, 120, 0.05)),
    'ee': ((270, 60, 1.0), (2300, 100, 0.4), (3000, 120, 0.3), (3700, 130, 0.12)),
    'oh': ((450, 70, 1.0), (800, 80, 0.45), (2830, 100, 0.12), (3500, 120, 0.06)),
}


def voice(freq, seconds, vowel='ah', vibrato=5.2, depth=0.006, breath=0.04):
    """One sung voice: harmonics shaped by the vowel's formants, a little vibrato and breath."""
    n = int(seconds * SR)
    t = np.arange(n) / SR
    rate = vibrato * (1 + RNG.uniform(-0.1, 0.1))
    vib = 1 + depth * np.sin(2 * np.pi * rate * t + RNG.uniform(0, 6.28)) * np.clip(t / 0.6, 0, 1)
    drift = 1 + 0.002 * np.sin(2 * np.pi * 0.3 * t + RNG.uniform(0, 6.28))
    inst = freq * vib * drift
    phase = np.cumsum(2 * np.pi * inst / SR)
    out = np.zeros(n)
    k = 1
    while freq * k < 6000:
        fk = freq * k
        gain = 0.0
        for fc, bw, g in FORMANTS[vowel]:
            gain += g / (1 + ((fk - fc) / bw) ** 2)
        gain = gain * (1.0 / k ** 0.5) + 0.002
        out += gain * np.sin(k * phase + RNG.uniform(0, 6.28))
        k += 1
    br = bandpass(noise(seconds), 600, 4000) * breath
    return out / max(1e-9, np.max(np.abs(out))) + br


def choir(freqs, seconds, vowel='ah', voices=3, attack=0.8, release=1.2, **kw):
    """Several voices on every note, a little out of tune with each other: a choir."""
    n = int(seconds * SR)
    out = np.zeros(n)
    for f in freqs:
        for v in range(voices):
            detune = 2 ** (RNG.uniform(-12, 12) / 1200)
            out += voice(f * detune, seconds, vowel, **kw)
    return out * env_adsr(n, attack, 0.3, 0, release, 0.85)


def harp(freq, seconds):
    """A plucked string: a few harmonics decaying fast, the high ones faster."""
    n = int(seconds * SR)
    out = np.zeros(n)
    for k in range(1, 8):
        out += (1 / k) * tone(freq * k, seconds) * decay(n, 1.2 / k)
    out[:int(0.003 * SR)] *= np.linspace(0, 1, int(0.003 * SR))
    return out


def room(x, seconds=1.8, mix=0.35, bright=4000, predelay=0.012):
    """A synthetic room: the dry sound plus a tail of decaying, darkening noise."""
    n = int(seconds * SR)
    ir = RNG.standard_normal(n) * decay(n, seconds / 6.0)
    ir = lowpass(ir, bright, 1)
    ir = np.concatenate([np.zeros(int(predelay * SR)), ir])
    ir /= np.sqrt(np.sum(ir ** 2))
    wet = signal.fftconvolve(x, ir)[:len(x) + n]
    dry = np.pad(x, (0, len(wet) - len(x)))
    return dry * (1 - mix) + wet * mix * 1.6


def thump(freq=55, seconds=0.25, drop=0.6, body=0.12):
    n = int(seconds * SR)
    out = tone(glide(freq, freq * drop, seconds, 0.5), seconds) * decay(n, body)
    click = lowpass(noise(0.01), 1200) * 0.3
    return place(out, click, 0)


def heartbeat(seconds=0.9, freq=52):
    out = silence(seconds)
    place(out, thump(freq, 0.3), 0.0)
    place(out, thump(freq * 0.9, 0.3) * 0.7, 0.26)
    return out


def finish(x, peak=0.89):
    x = x - np.mean(x)
    m = np.max(np.abs(x))
    if m > 0:
        x = x / m
    x = np.tanh(x * 1.2) / np.tanh(1.2)                       # a gentle limiter
    loud = np.nonzero(np.abs(x) > 0.002)[0]                   # the room's tail is cut where it falls below -54 dB
    if len(loud):
        x = x[:min(len(x), loud[-1] + int(0.05 * SR))]
    return fade(x * peak, 0.002, 0.08)


# =====================================================================================================
# Glass, liquid and bubbles
# =====================================================================================================

def bubbles(seconds, count, lo=300, hi=1400, size=0.04):
    out = silence(seconds)
    for _ in range(count):
        f0 = RNG.uniform(lo, hi)
        d = RNG.uniform(0.6, 1.4) * size
        b = tone(glide(f0, f0 * 1.9, d, 0.7), d) * decay(int(d * SR), d / 3)
        place(out, b * RNG.uniform(0.3, 1.0), RNG.uniform(0, seconds - d))
    return out


def glass_shards(seconds, count, start=0.0, spread=0.25):
    out = silence(seconds)
    for _ in range(count):
        f = RNG.uniform(2500, 9500)
        d = RNG.uniform(0.04, 0.25)
        ping = tone(f, d) * decay(int(d * SR), d / 5) + 0.5 * tone(f * 2.37, d) * decay(int(d * SR), d / 8)
        place(out, ping * RNG.uniform(0.2, 1.0), start + abs(RNG.normal(0, spread)))
    return out


def s_flask_throw():
    seconds = 0.45
    w = sweep(noise(seconds), lambda p: 600 + 2600 * math.sin(p * math.pi), 0.7) * env_adsr(int(seconds * SR), 0.12, 0.1, 0, 0.2, 0.6)
    clink = bell(3100, 0.3, 1.0, (1.0, 2.71, 5.1), 0.4) * 0.25
    return room(w * 0.8 + place(silence(seconds), clink, 0.02), 0.6, 0.2)


def s_flask_shatter(variant):
    seconds = 1.1
    crack = highpass(noise(0.05), 2500) * decay(int(0.05 * SR), 0.012)
    out = silence(seconds)
    place(out, crack * 1.4, 0)
    place(out, glass_shards(seconds, 34 + variant * 8, 0.0, 0.08 + variant * 0.03) * 0.8, 0)
    splash = lowpass(noise(0.5), 1800) * decay(int(0.5 * SR), 0.09)
    place(out, splash * 0.9, 0.01)
    place(out, bubbles(0.9, 30, 500, 1800, 0.03) * 0.35, 0.08)
    fizz = highpass(noise(0.9), 4000) * decay(int(0.9 * SR), 0.3) * 0.12
    place(out, fizz, 0.05)
    return room(out, 1.0, 0.25)


def s_potion_bubble():
    seconds = 1.6
    out = bubbles(seconds, 55, 250, 900, 0.06)
    simmer = lowpass(noise(seconds), 500) * 0.15 * (0.6 + 0.4 * np.sin(2 * np.pi * 3 * t_axis(seconds)))
    return room(fade(out + simmer, 0.2, 0.3), 0.6, 0.25)


def s_potion_drink():
    seconds = 1.0
    out = silence(seconds)
    for i in range(3):                                                # three gulps
        d = 0.09
        n = int(d * SR)
        g = tone(glide(320, 150, d, 0.5), d) * np.sin(np.linspace(0, np.pi, n)) * 0.8
        g += bandpass(noise(d), 200, 900) * np.sin(np.linspace(0, np.pi, n)) * 0.5
        place(out, g * (1 - i * 0.15), 0.05 + i * 0.27)
        place(out, bubbles(0.15, 4, 400, 900, 0.03) * 0.3, 0.1 + i * 0.27)
    return room(out, 0.4, 0.12)


def s_heal_chime():
    seconds = 1.6
    out = silence(seconds)
    for i, f in enumerate((784, 988, 1175, 1568)):                  # G5 B5 D6 G6, rising
        place(out, bell(f, 1.2, 0.6, (1.0, 2.0, 3.0, 4.2), 1.2) * (0.9 - i * 0.1), i * 0.07)
    shimmer = highpass(noise(seconds), 6000) * env_adsr(int(seconds * SR), 0.2, 0.2, 0, 0.8, 0.3) * 0.08
    return room(out + shimmer, 1.6, 0.4, 6000)


# =====================================================================================================
# Moonlight, shimmer and blinking
# =====================================================================================================

def sparkle(seconds, count, lo=3000, hi=9000, start=0.0, end=None):
    out = silence(seconds)
    end = seconds if end is None else end
    for _ in range(count):
        f = RNG.uniform(lo, hi)
        d = RNG.uniform(0.08, 0.35)
        g = tone(f, d) * np.sin(np.linspace(0, np.pi, int(d * SR))) ** 2
        place(out, g * RNG.uniform(0.2, 1.0), RNG.uniform(start, max(start, end - d)))
    return out


def s_spirit_vision():
    seconds = 1.8
    n = int(seconds * SR)
    rise = tone(glide(220, 880, seconds, 0.6), seconds) * env_adsr(n, 0.5, 0.2, 0, 0.9, 0.5) * 0.35
    rise += tone(glide(330, 1320, seconds, 0.6), seconds) * env_adsr(n, 0.6, 0.2, 0, 0.9, 0.4) * 0.2
    air = sweep(noise(seconds), lambda p: 800 * 6 ** p, 0.5) * env_adsr(n, 0.4, 0.3, 0, 0.9, 0.5) * 0.6
    return room(rise + air + sparkle(seconds, 26, 3000, 8000, 0.3) * 0.25, 2.0, 0.45, 6000)


def s_moon_blink():
    seconds = 0.7
    n = int(seconds * SR)
    zap = tone(glide(4200, 380, 0.18, 0.4), 0.18) * decay(int(0.18 * SR), 0.08)
    out = silence(seconds)
    place(out, zap * 0.7, 0)
    place(out, sweep(noise(0.3), lambda p: 5000 * (0.15 ** p), 0.5) * decay(int(0.3 * SR), 0.1) * 0.8, 0)
    place(out, bell(1760, 0.6, 1.2, (1.0, 2.76, 5.4), 0.5) * 0.3, 0.05)
    place(out, sparkle(0.6, 14, 5000, 10000) * 0.25, 0.05)
    return room(out, 1.2, 0.4, 7000)


def s_moonlight_form():
    seconds = 2.2
    n = int(seconds * SR)
    pad = np.zeros(n)
    for f in (523.25, 659.25, 783.99, 987.77):                     # C5 E5 G5 B5, glassy
        pad += tone(f, seconds) * (0.6 + 0.4 * np.sin(2 * np.pi * RNG.uniform(3, 6) * t_axis(seconds)))
        pad += 0.3 * tone(f * 2.003, seconds)
    pad *= env_adsr(n, 0.25, 0.3, 0, 1.2, 0.6) * 0.2
    whoosh = sweep(noise(seconds), lambda p: 3000 * (0.3 + p), 0.6) * env_adsr(n, 0.05, 0.4, 0, 1.0, 0.2) * 0.4
    return room(pad + whoosh + sparkle(seconds, 30, 3500, 9000, 0, 1.6) * 0.3, 2.2, 0.5, 7000)


def s_moonlight_reform():
    seconds = 1.4
    out = silence(seconds)
    rev = s_moonlight_form()[:int(1.0 * SR)][::-1] * 0.6          # the shimmer, drawn back in
    place(out, rev, 0)
    place(out, glass_shards(0.6, 22, 0, 0.05) * 0.6, 0.95)
    place(out, bell(1046.5, 0.5, 1.0, (1.0, 2.0, 3.0), 0.6) * 0.4, 1.0)
    return room(out, 1.4, 0.35, 7000)


def s_invisibility():
    seconds = 1.2
    n = int(seconds * SR)
    down = tone(glide(1500, 300, seconds, 0.7), seconds) * env_adsr(n, 0.02, 0.2, 0, 0.9, 0.5) * 0.2
    hush = sweep(noise(seconds), lambda p: 6000 * (0.2 ** p), 0.6) * env_adsr(n, 0.02, 0.3, 0, 0.8, 0.4)
    return room(down + hush + sparkle(seconds, 18, 4000, 9000, 0, 0.8) * 0.3, 1.2, 0.4)


# =====================================================================================================
# Bats and wings
# =====================================================================================================

def flap(heavy=1.0):
    d = 0.16 + 0.06 * heavy
    n = int(d * SR)
    body = lowpass(noise(d), 700 + 400 * (1 - heavy)) * (np.linspace(0, 1, n) ** 0.3) * decay(n, d / 3.5)
    snap = bandpass(noise(0.03), 800, 3000) * decay(int(0.03 * SR), 0.006) * 0.6
    out = body
    place(out, snap, 0.01)
    place(out, thump(70, 0.15, 0.7, 0.05) * 0.5 * heavy, 0)
    return out


def squeak():
    d = RNG.uniform(0.02, 0.05)
    f0 = RNG.uniform(5500, 9000)
    s = tone(glide(f0, f0 * RNG.uniform(0.6, 0.85), d, 0.6), d) * np.sin(np.linspace(0, np.pi, int(d * SR)))
    return s


def s_wings_unfurl():
    seconds = 0.9
    out = silence(seconds)
    sw = sweep(noise(0.5), lambda p: 300 + 1500 * p, 0.8) * env_adsr(int(0.5 * SR), 0.25, 0.05, 0, 0.2, 0.7) * 0.5
    place(out, sw, 0)
    place(out, flap(1.4) * 1.1, 0.32)
    place(out, flap(0.8) * 0.6, 0.5)
    return room(out, 0.8, 0.2)


def s_wing_flap(variant):
    seconds = 0.5
    out = silence(seconds)
    place(out, flap(1.0 + 0.3 * variant), 0.0)
    whoosh = sweep(noise(0.35), lambda p: 900 - 500 * p, 0.7) * decay(int(0.35 * SR), 0.12) * 0.35
    place(out, whoosh, 0.02)
    return room(out, 0.5, 0.15)


def s_bat_swarm():
    seconds = 2.6
    out = silence(seconds)
    n = int(seconds * SR)
    for i in range(70):                                               # many wings, out of step
        place(out, flap(RNG.uniform(0.2, 0.6)) * RNG.uniform(0.2, 0.6), RNG.uniform(0, seconds - 0.25))
    for i in range(45):                                               # and their squeaks
        place(out, squeak() * RNG.uniform(0.06, 0.25), RNG.uniform(0, seconds - 0.06))
    flutter = bandpass(noise(seconds), 300, 2000) * (0.5 + 0.5 * np.sin(2 * np.pi * 18 * t_axis(seconds))) * 0.3
    out += flutter
    out *= env_adsr(n, 0.3, 0.2, 0, 0.6, 0.85)
    return room(out, 1.0, 0.3)


# =====================================================================================================
# Chains, darkness, the eye
# =====================================================================================================

def clank(freq):
    d = 0.35
    n = int(d * SR)
    out = np.zeros(n)
    for r, a in ((1.0, 1.0), (2.76, 0.6), (5.4, 0.4), (8.93, 0.25), (13.3, 0.12)):
        out += a * tone(freq * r, d, RNG.uniform(0, 6.28)) * decay(n, 0.08 / (1 + r * 0.15))
    place(out, highpass(noise(0.01), 3000) * 0.6, 0)
    return out


def s_shackles():
    seconds = 1.5
    out = silence(seconds)
    rise = sweep(noise(0.7), lambda p: 150 + 600 * p, 0.7) * env_adsr(int(0.7 * SR), 0.5, 0.05, 0, 0.15, 0.8) * 0.6
    place(out, rise, 0)
    for i in range(14):                                               # the chain rattling up
        place(out, clank(RNG.uniform(500, 1100)) * RNG.uniform(0.15, 0.4), 0.15 + i * 0.035 + RNG.uniform(0, 0.02))
    place(out, clank(420) * 1.0, 0.68)                                # and locking
    place(out, clank(310) * 0.8, 0.70)
    place(out, thump(48, 0.5, 0.6, 0.2) * 0.9, 0.68)
    return room(out, 1.6, 0.35, 3000)


def s_drain():
    seconds = 0.9
    n = int(seconds * SR)
    suck = sweep(noise(seconds), lambda p: 2500 * (0.12 ** p), 0.5) * env_adsr(n, 0.3, 0.2, 0, 0.3, 0.7)
    low = tone(glide(220, 90, seconds, 0.7), seconds) * env_adsr(n, 0.2, 0.2, 0, 0.4, 0.6) * 0.3
    whisper = bandpass(noise(seconds), 2000, 6000) * (0.5 + 0.5 * np.sin(2 * np.pi * 11 * t_axis(seconds))) * 0.2 * env_adsr(n, 0.2, 0.2, 0, 0.3, 0.6)
    return room(suck + low + whisper, 1.0, 0.3)


def drone(seconds, base=41.2, partials=(1, 1.5, 2, 3), beat=0.4):
    n = int(seconds * SR)
    out = np.zeros(n)
    tt = t_axis(seconds)
    for i, p in enumerate(partials):
        f = base * p
        sawish = sum((1 / k) * np.sin(2 * np.pi * f * k * tt + RNG.uniform(0, 6.28)) for k in range(1, 9))
        out += sawish * (0.8 / (1 + i)) * (0.75 + 0.25 * np.sin(2 * np.pi * beat * (i + 1) * tt))
    return lowpass(out, 600, 2)


def s_gaze():
    seconds = 2.6
    n = int(seconds * SR)
    out = drone(seconds, 36.7, (1, 1.06, 2, 2.12)) * env_adsr(n, 0.6, 0.3, 0, 1.0, 0.8) * 0.5
    place(out, heartbeat(0.9, 46) * 0.9, 0.3)
    place(out, heartbeat(0.9, 46) * 0.9, 1.2)
    squeal = tone(glide(2400, 2600, seconds), seconds) * env_adsr(n, 1.4, 0.2, 0, 1.0, 0.6) * 0.04
    return room(out + squeal, 2.0, 0.35, 2500)


def s_eye_crush():
    seconds = 0.9
    n = int(seconds * SR)
    squelch = sweep(noise(0.5), lambda p: 1800 * (0.15 ** p), 0.8) * env_adsr(int(0.5 * SR), 0.005, 0.1, 0, 0.3, 0.6)
    squelch *= 1 + 0.6 * np.sin(2 * np.pi * 34 * t_axis(0.5))
    out = silence(seconds)
    place(out, squelch, 0)
    place(out, highpass(noise(0.04), 1500) * decay(int(0.04 * SR), 0.01) * 1.2, 0.0)
    place(out, thump(90, 0.4, 0.4, 0.1), 0.0)
    place(out, lowpass(noise(0.3), 600) * decay(int(0.3 * SR), 0.06) * 0.6, 0.12)
    return room(out, 0.8, 0.25, 2500)


def s_darkness_close():
    seconds = 1.0
    out = silence(seconds)
    for i in range(40):                                               # black glass growing, crackling
        at = (i / 40) ** 0.7 * 0.6
        c = bandpass(noise(0.02), 1500, 6000) * decay(int(0.02 * SR), 0.004)
        place(out, c * RNG.uniform(0.3, 1.0), at)
    place(out, clank(180) * 0.5, 0.62)
    place(out, thump(60, 0.4, 0.6, 0.12) * 0.8, 0.62)
    low = drone(1.0, 55, (1, 2)) * env_adsr(int(SR), 0.3, 0.2, 0, 0.4, 0.6) * 0.3
    return room(out + low, 1.2, 0.3, 3000)


# =====================================================================================================
# The Door of Summoning and what comes through it
# =====================================================================================================

def creak(seconds, f_lo=25, f_hi=70):
    """Wood dragging on stone: a train of tiny catches whose rate wanders, each ringing the wood's body."""
    out = silence(seconds)
    t = 0.0
    body_d = 0.03
    nb = int(body_d * SR)
    body = np.zeros(nb)
    for fr, a in ((320, 1.0), (540, 0.6), (870, 0.4), (1400, 0.2)):
        body += a * tone(fr, body_d) * decay(nb, 0.008)
    while t < seconds:
        p = t / seconds
        rate = f_lo + (f_hi - f_lo) * (0.5 + 0.5 * math.sin(p * 9.0 + math.sin(p * 23.0)))
        place(out, body * RNG.uniform(0.5, 1.0), t)
        t += 1.0 / rate * RNG.uniform(0.8, 1.2)
    return out


def s_door_open():
    seconds = 3.6
    n = int(seconds * SR)
    out = silence(seconds)
    rumble = lowpass(noise(seconds), 90, 2) * env_adsr(n, 0.4, 0.4, 0, 1.8, 0.8) * 3.0
    place(out, rumble, 0)
    place(out, creak(1.8) * env_adsr(int(1.8 * SR), 0.3, 0.2, 0, 0.4, 0.8) * 0.6, 0.3)
    place(out, choir((110.0, 164.8, 220.0, 261.6), 3.0, 'oh', 2, 1.2, 1.4) * 0.12, 0.6)
    place(out, sweep(noise(1.8), lambda p: 300 + 2500 * p, 0.6) * env_adsr(int(1.8 * SR), 1.0, 0.2, 0, 0.6, 0.6) * 0.4, 1.5)
    place(out, thump(40, 0.8, 0.6, 0.3) * 1.0, 0.0)
    return room(out, 2.6, 0.45, 2500)


def s_summon_arrival():
    seconds = 1.6
    n = int(seconds * SR)
    whoosh = sweep(noise(seconds), lambda p: 200 + 3000 * math.sin(p * math.pi) ** 2, 0.6) * env_adsr(n, 0.3, 0.2, 0, 0.8, 0.6)
    growl = lowpass(noise(1.0), 300) * (0.5 + 0.5 * np.sin(2 * np.pi * 24 * t_axis(1.0)))
    growl = growl * env_adsr(int(SR), 0.15, 0.2, 0, 0.5, 0.7)
    out = whoosh * 0.7
    place(out, growl * 1.4, 0.35)
    place(out, sparkle(seconds, 12, 2000, 6000, 0.1, 0.8) * 0.2, 0)
    return room(out, 1.6, 0.4, 3500)


# =====================================================================================================
# Creation, Beauty and the Blood Moon
# =====================================================================================================

def s_creation():
    seconds = 3.4
    n = int(seconds * SR)
    out = silence(seconds)
    place(out, choir((261.63, 329.63, 392.0, 523.25), 3.0, 'oo', 2, 0.9, 1.4) * 0.22, 0.1)
    for i, f in enumerate((523.25, 659.25, 783.99, 1046.5, 1318.5, 1568.0)):
        place(out, bell(f, 2.0, 0.5, (1.0, 2.0, 3.0, 4.0), 1.6) * 0.3, 0.2 + i * 0.12)
    place(out, sweep(noise(2.0), lambda p: 1500 + 4000 * p, 0.5) * env_adsr(int(2 * SR), 0.6, 0.4, 0, 0.8, 0.5) * 0.15, 0.2)
    place(out, sparkle(seconds, 40, 3000, 8000, 0.4, 3.0) * 0.18, 0)
    place(out, thump(65, 0.6, 0.8, 0.25) * 0.4, 0)
    return room(out, 2.8, 0.5, 6000)


def s_beauty():
    seconds = 3.8
    out = silence(seconds)
    place(out, choir((349.23, 440.0, 523.25, 659.25, 880.0), 3.4, 'ah', 2, 1.0, 1.4, depth=0.008) * 0.25, 0.25)
    notes = (523.25, 587.33, 659.25, 783.99, 880.0, 1046.5, 1174.7, 1318.5, 1568.0, 1760.0)
    for i, f in enumerate(notes):                                      # a harp sweeping up
        place(out, harp(f, 1.6) * 0.25, i * 0.045)
    place(out, sparkle(seconds, 50, 3500, 9500, 0.2, 3.2) * 0.2, 0)
    place(out, bell(1046.5, 3.0, 0.8, (1.0, 2.0, 2.99, 4.1, 5.4), 2.4) * 0.25, 0.45)
    return room(out, 3.2, 0.55, 7000)


def s_blood_moon():
    seconds = 5.5
    n = int(seconds * SR)
    out = drone(seconds, 32.7, (1, 1.5, 2, 2.38, 3)) * env_adsr(n, 1.5, 0.5, 0, 2.0, 0.8) * 0.6
    place(out, choir((130.81, 138.59, 185.0, 196.0), 4.5, 'oo', 2, 1.6, 1.8, depth=0.012) * 0.16, 0.6)
    for at in (0.2, 2.2):                                              # a great bell, twice
        place(out, bell(73.4, 3.2, 0.8, (0.5, 1.0, 1.19, 1.56, 2.0, 2.51, 2.66, 3.01), 3.0) * 0.6, at)
    place(out, sweep(noise(seconds), lambda p: 200 + 300 * p, 0.8) * env_adsr(n, 2.0, 0.5, 0, 2.0, 0.6) * 0.35, 0)
    place(out, heartbeat(0.9, 40) * 0.6, 3.6)
    return room(out, 3.5, 0.5, 2000)


# =====================================================================================================
# The Potions wheel
# =====================================================================================================

def s_fire_breath():
    seconds = 1.6
    n = int(seconds * SR)
    roar = bandpass(noise(seconds), 150, 2200) * env_adsr(n, 0.08, 0.2, 0, 0.6, 0.8)
    roar *= 1 + 0.35 * lowpass(RNG.standard_normal(n), 12) * 30
    crackle = silence(seconds)
    for _ in range(60):
        c = highpass(noise(0.006), 2500) * decay(int(0.006 * SR), 0.0015)
        place(crackle, c * RNG.uniform(0.3, 1.0), RNG.uniform(0, seconds - 0.01))
    whoosh = sweep(noise(seconds), lambda p: 400 + 1800 * math.sin(p * math.pi * 0.7), 0.7) * env_adsr(n, 0.05, 0.3, 0, 0.6, 0.6) * 0.5
    return room(roar + crackle * 0.5 + whoosh, 0.9, 0.2)


def s_sun_water():
    seconds = 1.9
    n = int(seconds * SR)
    sizzle = highpass(noise(seconds), 3500) * decay(n, 0.5) * 0.4
    for _ in range(80):
        c = bandpass(noise(0.01), 2000, 8000) * decay(int(0.01 * SR), 0.002)
        place(sizzle, c * RNG.uniform(0.2, 0.9), RNG.uniform(0, 1.0))
    chord = silence(seconds)
    for f in (587.33, 739.99, 880.0, 1174.7):                          # D major, bright
        place(chord, bell(f, 1.6, 0.7, (1.0, 2.0, 3.0, 4.0), 1.4) * 0.3, 0.05)
    place(chord, choir((293.66, 440.0, 587.33), 1.6, 'ah', 1, 0.15, 1.0) * 0.12, 0.05)
    return room(sizzle + chord, 1.8, 0.4, 7000)


def s_shadow_melt():
    seconds = 1.2
    n = int(seconds * SR)
    rev = sweep(noise(seconds), lambda p: 300 + 2500 * p ** 2, 0.6) * np.linspace(0, 1, n) ** 2
    out = silence(seconds)
    place(out, rev[::-1] * 0.8, 0)
    place(out, drone(seconds, 49, (1, 2)) * env_adsr(n, 0.05, 0.3, 0, 0.6, 0.6) * 0.4, 0)
    place(out, thump(50, 0.4, 0.6, 0.15) * 0.6, 0.0)
    return room(out, 1.4, 0.35, 2500)


def s_blood_pact():
    seconds = 1.6
    n = int(seconds * SR)
    out = silence(seconds)
    place(out, drone(1.6, 55, (1, 2, 3)) * env_adsr(n, 0.2, 0.3, 0, 0.8, 0.6) * 0.4, 0)
    place(out, heartbeat(0.9, 55), 0.05)
    shing = bell(1318.5, 1.2, 1.4, (1.0, 2.76, 5.4, 8.9), 0.8) * 0.35
    place(out, shing, 0.0)
    place(out, sweep(noise(0.6), lambda p: 4000 * (0.2 ** p), 0.5) * decay(int(0.6 * SR), 0.2) * 0.4, 0.0)
    return room(out, 1.5, 0.35, 4000)


def s_moonrise():
    seconds = 4.2
    n = int(seconds * SR)
    out = silence(seconds)
    place(out, choir((196.0, 246.94, 293.66, 392.0, 493.88), 4.0, 'ah', 2, 1.8, 1.6, depth=0.007) * 0.24, 0.0)
    place(out, sweep(noise(seconds), lambda p: 600 + 4000 * p, 0.6) * env_adsr(n, 2.0, 0.5, 0, 1.4, 0.5) * 0.2, 0)
    for i, f in enumerate((783.99, 987.77, 1174.7, 1567.98)):
        place(out, bell(f, 2.0, 0.6, (1.0, 2.0, 3.0, 4.07), 1.8) * 0.18, 1.2 + i * 0.3)
    place(out, sparkle(seconds, 40, 4000, 9500, 1.0, 3.8) * 0.15, 0)
    return room(out, 3.0, 0.55, 6000)


# =====================================================================================================
# The list: event name -> the functions that make its variants
# =====================================================================================================

SOUNDS = {
    'flask_throw': [s_flask_throw],
    'flask_shatter': [lambda: s_flask_shatter(0), lambda: s_flask_shatter(1)],
    'potion_bubble': [s_potion_bubble],
    'potion_drink': [s_potion_drink],
    'heal_chime': [s_heal_chime],
    'spirit_vision': [s_spirit_vision],
    'blood_pact': [s_blood_pact],
    'wings_unfurl': [s_wings_unfurl],
    'wing_flap': [lambda: s_wing_flap(0), lambda: s_wing_flap(1)],
    'bat_swarm': [s_bat_swarm],
    'shackles': [s_shackles],
    'drain': [s_drain],
    'invisibility': [s_invisibility],
    'fire_breath': [s_fire_breath],
    'sun_water': [s_sun_water],
    'shadow_melt': [s_shadow_melt],
    'moonrise': [s_moonrise],
    'moon_blink': [s_moon_blink],
    'moonlight_form': [s_moonlight_form],
    'moonlight_reform': [s_moonlight_reform],
    'gaze': [s_gaze],
    'eye_crush': [s_eye_crush],
    'darkness_close': [s_darkness_close],
    'door_open': [s_door_open],
    'summon_arrival': [s_summon_arrival],
    'creation': [s_creation],
    'beauty': [s_beauty],
    'blood_moon': [s_blood_moon],
}

SUBTITLES = {
    'flask_throw': 'Flask thrown', 'flask_shatter': 'Flask shatters', 'potion_bubble': 'Potion bubbles',
    'potion_drink': 'Potion drunk', 'heal_chime': 'Healing chimes', 'spirit_vision': 'Spirit Vision opens', 'blood_pact': 'Blood pact sealed',
    'wings_unfurl': 'Wings of darkness unfurl', 'wing_flap': 'Wings beat', 'bat_swarm': 'Bats swarm',
    'shackles': 'Abyss shackles bind', 'drain': 'Life is drained', 'invisibility': 'Someone fades from sight',
    'fire_breath': 'Fire is breathed', 'sun_water': 'Sun Water bursts', 'shadow_melt': 'Someone melts into shadow',
    'moonrise': 'A full moon rises', 'moon_blink': 'Moonlight flashes', 'moonlight_form': 'Body turns to moonlight',
    'moonlight_reform': 'Moonlight re-forms', 'gaze': 'A dark gaze holds', 'eye_crush': 'An eye is crushed',
    'darkness_close': 'Darkness hardens', 'door_open': 'The Door of Summoning opens',
    'summon_arrival': 'Something comes through', 'creation': 'Life blooms', 'beauty': 'Divine beauty',
    'blood_moon': 'The Blood Moon rises',
}


def encode(samples, path):
    with tempfile.NamedTemporaryFile(suffix='.raw', delete=False) as f:
        f.write(samples.astype(np.float32).tobytes())
        raw = f.name
    try:
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-f', 'f32le', '-ar', str(SR), '-ac', '1', '-i', raw,
                        '-c:a', 'libvorbis', '-q:a', '5', path], check=True)
    finally:
        os.remove(raw)


def write_wav(samples, path):
    from scipy.io import wavfile
    wavfile.write(path, SR, (samples * 32767).astype(np.int16))


def main(assets, wav_dir=None):
    folder = os.path.join(assets, 'sounds/beauty_goddess')
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
                write_wav(samples, os.path.join(wav_dir, file + '.wav'))
            files.append({'name': f'circle_of_inevitability:beauty_goddess/{file}'})
            print(f'sounds/beauty_goddess/{file}.ogg  {len(samples) / SR:.2f} s')
        events['beauty_goddess.' + name] = {
            'subtitle': f'subtitles.circle_of_inevitability.beauty_goddess.{name}',
            'sounds': files,
        }
    with open(sounds_json, 'w') as f:
        json.dump(events, f, indent=2)
    lang_path = os.path.join(assets, 'lang/en_us.json')                # the subtitles
    if os.path.exists(lang_path):
        with open(lang_path, encoding='utf-8') as f:
            lang = json.load(f)
        for name, text in SUBTITLES.items():
            lang[f'subtitles.circle_of_inevitability.beauty_goddess.{name}'] = text
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
