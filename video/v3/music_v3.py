"""Música + efectos para intro_v3 (100 BPM, compás 2,4 s). Síntesis pura, determinista, nunca en silencio.

python3 music_v3.py out.wav
"""
import sys
import numpy as np
from scipy import signal
from scipy.io import wavfile

SR = 48000
DUR = 73.0
N = int(SR * DUR)
T = np.arange(N) / SR
BEAT, BAR, STEP = 0.6, 2.4, 0.15
rng = np.random.default_rng(73)

bus = {k: np.zeros((2, N)) for k in ("pad", "bass", "drums", "arp", "lead", "fx")}
rev_send = np.zeros((2, N))


def midi(n):
    return 440.0 * 2 ** ((n - 69) / 12)


NOTE = {'C': 0, 'C#': 1, 'D': 2, 'D#': 3, 'E': 4, 'F': 5, 'F#': 6, 'G': 7, 'G#': 8, 'A': 9, 'A#': 10, 'B': 11}


def nm(name):  # 'A3' -> midi
    return NOTE[name[:-1]] + 12 * (int(name[-1]) + 1)


CH = {'Am': ['A', 'C', 'E'], 'F': ['F', 'A', 'C'], 'C': ['C', 'E', 'G'], 'G': ['G', 'B', 'D'], 'E': ['E', 'G#', 'B'], 'Dm': ['D', 'F', 'A']}
PROG = ['Am', 'Am', 'Am', 'Am', 'F', 'Am', 'F', 'Am', 'F', 'C', 'G', 'Am', 'F', 'E',
        'C', 'G', 'Am', 'F', 'C', 'G', 'Am', 'F', 'C', 'G', 'Am', 'F', 'G', 'C', 'Am', 'G', 'C']


def lp(x, fc, o=2):
    return signal.sosfilt(signal.butter(o, fc, 'low', fs=SR, output='sos'), x)


def hp(x, fc, o=2):
    return signal.sosfilt(signal.butter(o, fc, 'high', fs=SR, output='sos'), x)


def bp(x, lo, hi, o=2):
    return signal.sosfilt(signal.butter(o, [lo, hi], 'band', fs=SR, output='sos'), x)


def put(name, sig, t0, gain=1.0, pan=0.0, rev=0.0):
    i0 = int(round(t0 * SR))
    if i0 >= N:
        return
    if i0 < 0:
        sig = sig[-i0:]; i0 = 0
    n = min(len(sig), N - i0)
    s = sig[:n] * gain
    p = pan if np.isscalar(pan) else pan[:n]
    a = (np.clip(p, -1, 1) + 1) * np.pi / 4
    gl, gr = np.cos(a), np.sin(a)
    bus[name][0, i0:i0 + n] += s * gl
    bus[name][1, i0:i0 + n] += s * gr
    if rev:
        rev_send[0, i0:i0 + n] += s * gl * rev
        rev_send[1, i0:i0 + n] += s * gr * rev


def adsr(n, a=0.005, d=0.1, s=0.7, r=0.1):
    t = np.arange(n) / SR
    dur = n / SR
    e = np.where(t < a, t / a, np.where(t < a + d, 1 - (1 - s) * (t - a) / d, s))
    rel = np.clip((dur - t) / r, 0, 1)
    return e * rel


# ---------------------------------------------------------------- instrumentos
def warm(freq, dur, harms=(1, .5, .25, .12), detune=0.0):
    n = int(dur * SR); t = np.arange(n) / SR
    ph = rng.uniform(0, 6.28)
    return sum(a * np.sin(2 * np.pi * freq * (k + 1) * (1 + detune) * t + ph * (k + 1)) for k, a in enumerate(harms))


def pluck(freq, dur=0.5, decay=0.18, bright=0.3):
    n = int(dur * SR); t = np.arange(n) / SR
    s = np.sin(2 * np.pi * freq * t) + bright * np.sin(2 * np.pi * freq * 2 * t) * np.exp(-t / 0.06) + 0.15 * np.sin(2 * np.pi * freq * 3 * t) * np.exp(-t / 0.03)
    return s * np.exp(-t / decay) * np.minimum(1, t / 0.002)


def bell(freq, dur=2.0, decay=0.8):
    n = int(dur * SR); t = np.arange(n) / SR
    s = (np.sin(2 * np.pi * freq * t) * np.exp(-t / decay) + 0.35 * np.sin(2 * np.pi * freq * 2.76 * t) * np.exp(-t / (decay * .35))
         + 0.15 * np.sin(2 * np.pi * freq * 5.4 * t) * np.exp(-t / (decay * .15)))
    return s * np.minimum(1, t / 0.003)


def kick(dur=0.45, f0=150, f1=45, decay=0.16, click=0.35):
    n = int(dur * SR); t = np.arange(n) / SR
    f = f1 + (f0 - f1) * np.exp(-t / 0.04)
    s = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / decay)
    s += click * rng.standard_normal(n) * np.exp(-t / 0.003)
    return np.tanh(s * 1.6)


def snare(dur=0.3):
    n = int(dur * SR); t = np.arange(n) / SR
    noise = bp(rng.standard_normal(n), 1200, 7000) * np.exp(-t / 0.09)
    body = np.sin(2 * np.pi * 190 * t) * np.exp(-t / 0.05)
    return noise * 0.8 + body * 0.5


def clap(dur=0.3):
    n = int(dur * SR); t = np.arange(n) / SR
    env = sum(np.exp(-np.clip(t - d, 0, None) / 0.012) * (t >= d) for d in (0, 0.011, 0.023)) + 0.6 * np.exp(-np.clip(t - 0.03, 0, None) / 0.11) * (t >= 0.03)
    return bp(rng.standard_normal(n), 900, 5000) * env * 0.7


def hat(dur=0.06, open_=False):
    n = int((0.35 if open_ else dur) * SR); t = np.arange(n) / SR
    return hp(rng.standard_normal(n), 7000) * np.exp(-t / (0.12 if open_ else 0.018))


def crash(dur=2.5):
    n = int(dur * SR); t = np.arange(n) / SR
    return hp(rng.standard_normal(n), 3500) * np.exp(-t / 0.9)


def boom(dur=2.0, f=48):
    n = int(dur * SR); t = np.arange(n) / SR
    return np.sin(2 * np.pi * (f + 40 * np.exp(-t / 0.05)) * t) * np.exp(-t / 0.7)


def riser(dur, lo=200, hi=6000):
    n = int(dur * SR); x = np.linspace(0, 1, n)
    noise = rng.standard_normal(n)
    out = np.zeros(n)
    segs = 24
    for k in range(segs):  # filtro que sube por tramos
        a, b = k * n // segs, (k + 1) * n // segs
        fc = lo * (hi / lo) ** (k / (segs - 1))
        out[a:b] = bp(noise[max(0, a - 2000):b], fc * 0.6, min(fc * 1.6, 20000))[-(b - a):]
    tone = np.sin(2 * np.pi * np.cumsum(220 + 660 * x ** 2) / SR) * 0.25
    return (out + tone) * x ** 2


def whoosh(dur, lo=200, hi=3000, peak=0.6):
    n = int(dur * SR); x = np.linspace(0, 1, n)
    e = np.where(x < peak, (x / peak) ** 2, ((1 - x) / (1 - peak)) ** 1.5)
    return bp(rng.standard_normal(n), lo, hi) * e


def sweep(f0, f1, dur, curve=1.0):
    n = int(dur * SR); x = np.linspace(0, 1, n) ** curve
    return np.sin(2 * np.pi * np.cumsum(f0 + (f1 - f0) * x) / SR)


# ---------------------------------------------------------------- secciones
def bar_t(b):
    return b * BAR


def section(b):
    if b <= 2: return 'A'
    if b <= 6: return 'B'
    if b <= 9: return 'C'
    if b <= 13: return 'D'
    if b <= 20: return 'E'
    if b <= 23: return 'F'
    if b <= 26: return 'G'
    if b == 27: return 'H'
    if b <= 29: return 'I'
    return 'Z'


# PAD
for b, chn in enumerate(PROG):
    sec = section(b)
    t0 = bar_t(b) - 0.05
    dur = BAR + 0.5 if sec != 'Z' else 1.6
    gain = {'A': .10, 'B': .11, 'C': .10, 'D': .12, 'E': .075, 'F': .08, 'G': .08, 'H': .09, 'I': .11, 'Z': .13}[sec]
    if b == 0:
        gain *= 0.6
    fc = {'A': 700, 'B': 600, 'C': 1400, 'D': 1100, 'E': 2600, 'F': 2400, 'G': 3000, 'H': 3200, 'I': 900, 'Z': 3000}[sec]
    root = CH[chn][0]
    voices = [nm(root + '3')] + [nm(n + '4') if NOTE[n] >= NOTE[root] or n == root else nm(n + '4') for n in CH[chn][1:]] + [nm(CH[chn][0] + '4')]
    n = int(dur * SR)
    env = adsr(n, a=0.35 if sec in 'ABDI' else 0.08, d=0.3, s=0.85, r=0.45)
    s = sum(warm(midi(v), dur, detune=d) for v in voices for d in (-0.002, 0.002))
    s = lp(s * env, fc) * gain / len(voices)
    put('pad', s, t0, 1.0, 0.0, rev=0.25)

# BAJO
for b, chn in enumerate(PROG):
    sec = section(b)
    r = midi(nm(CH[chn][0] + '1')) if CH[chn][0] in ('A', 'G', 'G#', 'B') else midi(nm(CH[chn][0] + '2'))
    if sec in 'ABD' or sec == 'I':
        n = int((BAR + 0.1) * SR)
        s = lp(np.tanh(warm(r, BAR + 0.1, (1, .3, .1)) * 1.5), 250) * adsr(n, 0.2, 0.2, 0.9, 0.3)
        g = {'A': .16, 'B': .2, 'D': .22, 'I': .22}[sec] * (0.3 if b == 0 else 1)
        put('bass', s, bar_t(b), g)
    elif sec in 'CEFG':
        steps = [0, 3, 6, 8, 11, 14] if sec in 'EG' else [0, 6, 8, 14]
        for st in steps:
            n = int(STEP * 2.2 * SR)
            s = lp(np.tanh(warm(r, STEP * 2.2, (1, .5, .2)) * 2.0), 420) * adsr(n, 0.004, 0.08, 0.6, 0.06)
            put('bass', s, bar_t(b) + st * STEP, .26)
    elif sec in 'HZ':
        n = int(1.5 * SR)
        put('bass', lp(np.tanh(warm(r, 1.5, (1, .4)) * 2), 300) * adsr(n, 0.005, 0.3, 0.7, 0.6), bar_t(b), .3)

# BATERÍA
kick_times = []
def k_at(t, g=1.0, **kw):
    kick_times.append(t); put('drums', kick(**kw), t, 0.55 * g)

for b in range(31):
    sec = section(b); t0 = bar_t(b)
    if sec in 'ABDI' and not (b == 0) and not (sec == 'D' and b >= 12):
        # latido: lub-dub en los tiempos 1 y 3
        for bt in (0, 2):
            k_at(t0 + bt * BEAT, 0.55 if sec != 'I' else 0.6, f0=110, f1=42, decay=0.2, click=0.1)
            k_at(t0 + bt * BEAT + 0.2, 0.32, f0=95, f1=40, decay=0.16, click=0.05)
    if sec == 'D' and b >= 12:  # construcción hacia el drop
        for st in range(0, 16, 4):
            k_at(t0 + st * STEP, 0.7)
    if sec in 'CEFG':
        kpat = {'C': [0, 8, 10], 'E': [0, 7, 8, 10], 'F': [0, 10], 'G': [0, 4, 8, 12]}[sec]
        for st in kpat:
            k_at(t0 + st * STEP, 0.95 if sec != 'C' else 0.75)
        spat = [4, 12] if sec != 'F' else [8]
        for st in spat:
            put('drums', snare(), t0 + st * STEP, 0.22 if sec == 'C' else 0.28, 0.05, rev=0.25)
            if sec in 'EG':
                put('drums', clap(), t0 + st * STEP + 0.005, 0.25, -0.05, rev=0.3)
        for st in range(0, 16, 1 if sec in 'EG' else 2):
            acc = 1.0 if st % 4 == 2 else 0.55
            put('drums', hat(), t0 + st * STEP, 0.07 * acc * (0.8 if sec == 'C' else 1), 0.35 if st % 2 else -0.25)
        if sec == 'G':
            put('drums', hat(open_=True), t0 + 14 * STEP, 0.08, 0.3)
    if sec == 'B':  # hats suaves bajo el agua
        for st in range(2, 16, 4):
            put('drums', lp(hat(), 5000), t0 + st * STEP, 0.035, 0.4)

# redoble hacia el drop (compás 13) y antes del clímax (compás 26)
for (a, b_, g) in [(32.4, 33.6, 0.22), (63.6, 64.8, 0.22), (71.4, 72.0, 0.18)]:
    tt = a; k = 0
    while tt < b_ - 0.01:
        frac = (tt - a) / (b_ - a)
        put('drums', snare(0.15), tt, g * (0.4 + 0.6 * frac), 0.0, rev=0.2)
        tt += STEP if frac < 0.5 else STEP / 2
        k += 1

# ARPEGIOS
for b, chn in enumerate(PROG):
    sec = section(b); t0 = bar_t(b)
    tones = [midi(nm(n + '4')) for n in CH[chn]] + [midi(nm(CH[chn][0] + '5'))]
    if sec == 'B':  # caja de música bajo el agua
        for st in range(0, 16, 2):
            f = tones[(st // 2) % 4] * 2
            put('arp', lp(pluck(f, 0.5, 0.25, 0.1), 1400), t0 + st * STEP, 0.045, -0.3 if st % 4 else 0.3, rev=0.6)
    if sec == 'C':
        for st in range(0, 16, 2):
            put('arp', pluck(tones[(st // 2) % 4], 0.4, 0.14), t0 + st * STEP, 0.04, -0.4 if st % 4 else 0.4, rev=0.4)
    if sec in 'EG':
        order = [0, 1, 2, 3, 2, 1, 3, 2]
        for st in range(16):
            f = tones[order[st % 8]] * (2 if st % 4 == 3 else 1)
            put('arp', pluck(f, 0.35, 0.11, 0.4), t0 + st * STEP, 0.032 if sec == 'E' else 0.04, -0.45 if st % 2 else 0.45, rev=0.35)
    if sec == 'F':
        for st in range(0, 16, 4):
            put('arp', pluck(tones[(st // 4) % 4] * 2, 0.6, 0.25, 0.2), t0 + st * STEP, 0.04, 0.0, rev=0.6)

# MELODÍA (campana) — gancho de 4 compases sobre C G Am F
HOOK = [[('E5', 0, 1), ('G5', 1, 1), ('E5', 2, 1), ('D5', 3, 1)],
        [('D5', 0, 2), ('B4', 2, 1), ('G4', 3, 1)],
        [('C5', 0, 1), ('E5', 1, 1), ('A5', 2, 2)],
        [('G5', 0, 1), ('F5', 1, 1), ('E5', 2, 2)]]
HOOK_OF = {'C': 0, 'G': 1, 'Am': 2, 'F': 3}
for bars, g in [(range(15, 19), 0.030), (range(24, 27), 0.045)]:
    for b in bars:
        for (n_, bt, ln) in HOOK[HOOK_OF[PROG[b]]]:
            put('lead', bell(midi(nm(n_)), ln * BEAT + 0.8, 0.5 + 0.2 * ln), bar_t(b) + bt * BEAT, g, 0.15, rev=0.7)

# ---------------------------------------------------------------- efectos sincronizados con la imagen
fx = lambda *a, **k: put('fx', *a, **k)
rain_n = int(6 * SR); x = np.arange(rain_n) / SR
fx(hp(rng.standard_normal(rain_n), 2500) * np.clip(x / 1.2, 0, 1) * (1 - np.clip((x - 4.4) / 1.4, 0, 1)), 0.0, 0.03, -0.2)
for k in range(40):
    fx(pluck(rng.uniform(2500, 4500), 0.05, 0.008, 0), rng.uniform(0.3, 5.0), 0.025, rng.uniform(-0.9, 0.9), rev=0.4)
# gota
fx(sweep(1500, 480, 0.12, 0.6) * np.exp(-np.arange(int(.12 * SR)) / SR / 0.06), 4.8, 0.5, 0.0, rev=1.0)
fx(sweep(900, 380, 0.15, 0.5) * np.exp(-np.arange(int(.15 * SR)) / SR / 0.07), 4.97, 0.2, 0.05, rev=1.0)
fx(boom(2.2, 46), 4.8, 0.45); fx(crash(2.0)[::-1] * np.linspace(0, 1, int(2.0 * SR)) ** 3, 2.8, 0.05, rev=0.3)  # cimbal invertido hacia el impacto
# zambullida + bajo el agua
fx(lp(whoosh(2.2, 100, 2500, 0.7), 1800), 7.6, 0.3, 0.0, rev=0.3)
uw_n = int(7.2 * SR); u = hp(lp(np.cumsum(rng.standard_normal(uw_n)) * 0.01, 380), 25); u /= np.abs(u).max()
xx = np.arange(uw_n) / SR; fx(u * np.clip(xx / 0.9, 0, 1) * (1 - np.clip((xx - 6.0) / 1.2, 0, 1)), 8.4, 0.12, -0.1)
for k in range(40):
    fx(sweep(rng.uniform(250, 450), rng.uniform(900, 1500), 0.05, 2.0) * np.exp(-np.arange(int(.05 * SR)) / SR / 0.02), rng.uniform(9.0, 14.6), 0.06, rng.uniform(-0.8, 0.8), rev=0.7)
for f in (1980, 2640, 3300):
    fx(bell(f, 1.5, 0.35), 13.85, 0.025, rng.uniform(-0.4, 0.4), rev=1.0)
fx(whoosh(1.6, 300, 4000, 0.4), 13.8, 0.16, 0.0, rev=0.4)
# mosquito
MP = np.array([[960, 650], [760, 430], [1010, 250], [1390, 320], [1520, 560], [1240, 720], [2160, 430]], float)
def catmull(u):
    n = len(MP) - 1; x = np.clip(u, 0, 1) * n; i = np.minimum(np.floor(x).astype(int), n - 1); f = (x - i)[:, None]
    p0, p1, p2, p3 = MP[np.maximum(i - 1, 0)], MP[i], MP[i + 1], MP[np.minimum(i + 2, n)]
    return 0.5 * ((2 * p1) + (-p0 + p2) * f + (2 * p0 - 5 * p1 + 4 * p2 - p3) * f ** 2 + (-p0 + 3 * p1 - 3 * p2 + p3) * f ** 3)
bz0, bz1 = 14.4, 17.95; bn = int((bz1 - bz0) * SR); bt = np.arange(bn) / SR
pos = catmull(bt / (bz1 - bz0)); fr = 560 + 28 * np.sin(2 * np.pi * 7 * bt) + 12 * np.sin(2 * np.pi * 1.3 * bt)
ph = 2 * np.pi * np.cumsum(fr) / SR; saw = 2 * ((ph / (2 * np.pi)) % 1) - 1
buzz = bp(saw, 450, 5000) * (0.7 + 0.3 * np.sin(2 * np.pi * 90 * bt)) * np.clip(bt / 0.4, 0, 1) * (1 - np.clip((bt - 3.1) / 0.45, 0, 1))
fx(buzz, bz0, 0.05, pan=(pos[:, 0] - 960) / 960 * 0.9)
for k in range(110):
    fx(pluck(rng.uniform(3000, 5200), 0.06, 0.012, 0), 15.0 + (k / 110) * 2.4 + rng.uniform(-.03, .03), 0.016, rng.uniform(-0.7, 0.7), rev=0.7)
# datos
fx(whoosh(1.4, 200, 2500, 0.5), 17.3, 0.12, rev=0.3)
fx(boom(1.6, 55), 21.6, 0.3); fx(crash(2.0), 21.6, 0.06, 0.2, rev=0.4)
fx(whoosh(1.6, 150, 2000, 0.5), 24.5, 0.12, rev=0.4)
# vórtice + riser hacia el drop
vn = int(2.6 * SR); fx(bp(rng.standard_normal(vn), 300, 3500) * np.linspace(0, 1, vn) ** 2, 31.0, 0.14, pan=np.sin(np.linspace(0, 16, vn)) * 0.6, rev=0.4)
fx(riser(2.4), 31.2, 0.12, rev=0.3)
fx(bell(880, 2.2, 0.7), 32.95, 0.08, rev=1.0)
# DROP 33.6: boom + crash + campanas
fx(boom(2.5, 44), 33.6, 0.5); fx(crash(3.0), 33.6, 0.09, rev=0.5)
for i, f in enumerate([523.25, 659.25, 783.99, 1046.5]):
    fx(bell(f, 3.0, 1.1), 33.6 + i * 0.05, 0.05, (i - 1.5) * 0.3, rev=1.0)
for i in range(8):
    fx(pluck(783.99 * 2 ** ((i % 3) / 12), 0.25, 0.05, 0.2), 35.2 + i * 0.05, 0.012, (i - 3.5) * 0.12, rev=0.6)
# demo en el teléfono
fx(whoosh(1.0, 200, 3000, 0.5), 38.1, 0.12, -0.3, rev=0.3)
fx(pluck(1046.5, 0.3, 0.05, 0.4), 39.5, 0.08, -0.2, rev=0.3)                         # toque en Analizar
fx(sweep(600, 1800, 1.1, 1.0) * np.hanning(int(1.1 * SR)), 39.7, 0.025, 0.0, rev=0.4)  # escaneo
for i, tb in enumerate([40.8, 41.1, 41.4]):
    fx(pluck(1318.5, 0.25, 0.04, 0.3), tb, 0.08, -0.3 + i * 0.2, rev=0.4)
    fx(pluck(1760.0, 0.25, 0.05, 0.3), tb + 0.035, 0.05, -0.3 + i * 0.2, rev=0.4)
fx(whoosh(0.5, 300, 3000, 0.4), 42.0, 0.07, -0.2)
fx(pluck(987.77, 0.3, 0.06, 0.3), 44.3, 0.07, -0.2, rev=0.3)                           # toque "No"
fx(pluck(1318.5, 0.6, 0.15, 0.2), 44.6, 0.05, -0.2, rev=0.6)
pn = int(1.0 * SR); fx(bp(rng.standard_normal(pn), 400, 3500) * (0.6 + 0.4 * np.abs(np.sin(np.linspace(0, 60, pn)))) * np.hanning(pn), 45.2, 0.12, -0.25, rev=0.4)
fx(kick(0.3, 130, 60, 0.08, 0.2), 46.8, 0.3, -0.25)                                     # balde volteado
for i, f in enumerate([659.25, 783.99, 1046.5, 1318.5]):
    fx(bell(f, 1.8, 0.6), 47.2 + i * 0.075, 0.07, -0.2 + i * 0.15, rev=0.8)
for k in range(30):
    fx(pluck(rng.uniform(2500, 5000), 0.08, 0.015, 0), 47.25 + rng.uniform(0, 1.2), 0.02, rng.uniform(-0.9, 0.9), rev=0.8)
fx(whoosh(0.9, 200, 3000, 0.4), 49.8, 0.1, 0.3, rev=0.3)
# comparación
fx(whoosh(0.6, 300, 4000, 0.5), 50.8, 0.07, -0.5); fx(whoosh(0.6, 300, 4000, 0.5), 52.3, 0.08, 0.5)
for i, tb in enumerate([52.8, 53.1, 53.4]):
    fx(pluck([1046.5, 1174.66, 1318.5][i], 0.4, 0.08, 0.3), tb, 0.05, 0.4, rev=0.5)
# viabilidad: contador y fichas
fx(sweep(300, 900, 1.7, 0.6) * np.linspace(1, 0, int(1.7 * SR)) ** 0.5, 57.9, 0.03, -0.3, rev=0.4)
for i in range(6):
    fx(pluck([1046.5, 1174.66, 1318.5, 1567.98, 1760.0, 2093.0][i], 0.3, 0.06, 0.3), 60.0 + i * 0.3, 0.05, 0.4 if i % 2 else 0.1, rev=0.5)
for i in range(10):
    fx(pluck(2093.0 * 2 ** ((i % 5) * 2 / 12), 0.12, 0.03, 0), 62.6 + i * 0.08, 0.02, 0.2 + i * 0.06, rev=0.6)
fx(riser(1.2, 300, 7000), 63.6, 0.1, rev=0.3)
fx(lp(whoosh(1.0, 80, 2500, 0.75), 2200), 64.0, 0.22, rev=0.4)
# clímax: tres golpes con acorde
for i, tb in enumerate([64.8, 65.4, 66.0]):
    fx(kick(0.6, 140, 42, 0.3, 0.3), tb, 0.55)
    for nt in ['C4', 'E4', 'G4', 'C5']:
        fx(lp(warm(midi(nm(nt)), 0.9, (1, .6, .4, .3, .2)) * adsr(int(0.9 * SR), 0.003, 0.15, 0.35, 0.4), 4000), tb, 0.035, (i - 1) * 0.35, rev=0.5)
fx(crash(2.5), 64.8, 0.08, rev=0.5)
for i, f in enumerate([523.25, 783.99, 1046.5, 1318.5]):
    fx(bell(f, 3.0, 1.2), 66.5 + i * 0.05, 0.045, (i - 1.5) * 0.3, rev=1.0)
# breakdown → "ya es una realidad"
dn = int(1.6 * SR); fx(lp(rng.standard_normal(dn), 3000) * np.linspace(1, 0, dn) ** 2, 67.2, 0.08, rev=0.6)   # caída
fx(riser(2.4, 200, 8000), 69.6, 0.13, rev=0.4)
fx(bell(659.25, 2.5, 1.0), 69.8, 0.05, rev=1.0); fx(bell(1318.5, 2.5, 0.8), 69.85, 0.03, rev=1.0)
fx(pluck(1046.5, 0.5, 0.15, 0.3), 71.0, 0.06, rev=0.6)
# golpe final
fx(boom(1.5, 44), 72.0, 0.55); fx(crash(1.2), 72.0, 0.09, rev=0.5)
for nt in ['C3', 'G3', 'C4', 'E4', 'G4', 'C5']:
    fx(lp(warm(midi(nm(nt)), 1.0, (1, .5, .3, .2)) * adsr(int(1.0 * SR), 0.003, 0.2, 0.5, 0.6), 5000), 72.0, 0.04, 0.0, rev=0.6)

# ---------------------------------------------------------------- mezcla
duck = np.ones(N)
for tk in kick_times:
    i0 = int(tk * SR); n = min(int(0.3 * SR), N - i0)
    if n > 0:
        duck[i0:i0 + n] = np.minimum(duck[i0:i0 + n], 1 - 0.45 * np.exp(-np.arange(n) / SR / 0.09))
# filtro del breakdown sobre la música (67,2–69,6): paso bajo que se abre en el riser
music = bus['pad'] * duck + bus['bass'] * duck + bus['arp'] + bus['lead'] + bus['drums']
i_a, i_b, i_c = int(67.2 * SR), int(69.6 * SR), int(72.0 * SR)
seg_ = music[:, i_a:i_c]
dark = np.stack([lp(seg_[0], 500), lp(seg_[1], 500)])
mixw = np.ones(i_c - i_a); ramp = np.linspace(0, 1, i_c - i_b) ** 2; mixw[i_b - i_a:] = 1 - ramp
music[:, i_a:i_c] = dark * mixw + seg_ * (1 - mixw)

ir_n = int(2.4 * SR); it = np.arange(ir_n) / SR
irs = [lp(rng.standard_normal(ir_n), 6000) * np.exp(-it / 0.5) for _ in range(2)]
irs = [i / np.sqrt((i ** 2).sum()) for i in irs]
wet = np.stack([signal.fftconvolve(rev_send[c], irs[c])[:N] for c in range(2)])
out = music + bus['fx'] + 0.5 * wet
out = np.stack([hp(out[0], 28), hp(out[1], 28)])
peak = np.abs(out).max()
out = np.tanh(out / peak * 1.6) / np.tanh(1.6)
fade = np.ones(N); fi = int(0.4 * SR); fade[:fi] = np.linspace(0, 1, fi) ** 2
fo0 = int(72.4 * SR); fade[fo0:] = np.linspace(1, 0, N - fo0) ** 1.5
out *= fade * 0.9
wavfile.write(sys.argv[1] if len(sys.argv) > 1 else 'music.wav', SR, (out.T * 32767).astype(np.int16))
print('ok', out.shape)
