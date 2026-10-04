"""Banda sonora sintetizada para intro_v2 (sin samples externos, determinista).

python3 synth.py audio.wav
Sincronizada con la línea de tiempo de intro_v2.html (mismos segundos).
"""
import sys
import numpy as np
from scipy import signal
from scipy.io import wavfile

SR = 48000
DUR = 58.4
N = int(SR * DUR)
T = np.arange(N) / SR
rng = np.random.default_rng(20261003)

L = np.zeros(N)
Rch = np.zeros(N)
RevL = np.zeros(N)   # envío a reverb
RevR = np.zeros(N)


def idx(t):
    return int(np.clip(t * SR, 0, N))


def pan_gains(p):
    a = (np.clip(p, -1, 1) + 1) * np.pi / 4
    return np.cos(a), np.sin(a)


def add(sig, t0, gain=1.0, pan=0.0, rev=0.0):
    """Suma `sig` (mono, o array de pan por muestra) desde t0."""
    i0 = idx(t0)
    n = min(len(sig), N - i0)
    if n <= 0:
        return
    s = sig[:n] * gain
    gl, gr = pan_gains(pan if np.isscalar(pan) else pan[:n])
    L[i0:i0 + n] += s * gl
    Rch[i0:i0 + n] += s * gr
    if rev:
        RevL[i0:i0 + n] += s * gl * rev
        RevR[i0:i0 + n] += s * gr * rev


def env_ad(n, a, d):
    """Envolvente ataque-caída exponencial (segundos)."""
    t = np.arange(n) / SR
    e = np.where(t < a, t / max(a, 1e-4), np.exp(-(t - a) / d))
    return e


def ramp(t, a, b):
    return np.clip((t - a) / (b - a), 0, 1)


def smooth(x):
    return x * x * (3 - 2 * x)


def lowpass(x, fc, order=2):
    sos = signal.butter(order, fc, 'low', fs=SR, output='sos')
    return signal.sosfilt(sos, x)


def bandpass(x, lo, hi, order=2):
    sos = signal.butter(order, [lo, hi], 'band', fs=SR, output='sos')
    return signal.sosfilt(sos, x)


def highpass(x, fc, order=2):
    sos = signal.butter(order, fc, 'high', fs=SR, output='sos')
    return signal.sosfilt(sos, x)


def sweep(f0, f1, dur, curve=1.0):
    n = int(dur * SR)
    tt = np.linspace(0, 1, n) ** curve
    f = f0 + (f1 - f0) * tt
    return np.sin(2 * np.pi * np.cumsum(f) / SR)


def bell(freq, dur=2.5, decay=0.9):
    n = int(dur * SR)
    t = np.arange(n) / SR
    s = (np.sin(2 * np.pi * freq * t) * np.exp(-t / decay)
         + 0.35 * np.sin(2 * np.pi * freq * 2.76 * t) * np.exp(-t / (decay * 0.35))
         + 0.15 * np.sin(2 * np.pi * freq * 5.4 * t) * np.exp(-t / (decay * 0.15)))
    s *= np.minimum(1, t / 0.004)
    return s


def pluck(freq, dur=0.6, decay=0.22):
    n = int(dur * SR)
    t = np.arange(n) / SR
    s = (np.sin(2 * np.pi * freq * t) + 0.25 * np.sin(2 * np.pi * freq * 4 * t) * np.exp(-t / 0.05))
    return s * np.exp(-t / decay) * np.minimum(1, t / 0.003)


def whoosh(dur, lo=200, hi=3000, peak=0.6):
    n = int(dur * SR)
    noise = rng.standard_normal(n)
    x = bandpass(noise, lo, hi)
    t = np.linspace(0, 1, n)
    e = np.where(t < peak, (t / peak) ** 2, ((1 - t) / (1 - peak)) ** 1.5)
    return x * e


def thump(f0=85, f1=42, dur=0.6, decay=0.22):
    s = sweep(f0, f1, dur, 0.5)
    n = len(s)
    t = np.arange(n) / SR
    click = rng.standard_normal(n) * np.exp(-t / 0.004) * 0.3
    return (s + click) * np.exp(-t / decay)


def pad(freqs, amps, detune=0.25):
    out = np.zeros(N)
    for f, a in zip(freqs, amps):
        for d in (-detune, detune):
            ph = rng.uniform(0, 2 * np.pi)
            out += a * 0.5 * (np.sin(2 * np.pi * (f + d) * T + ph) + 0.18 * np.sin(2 * np.pi * 2 * (f + d) * T + ph))
    return out


# ---------------------------------------------------------------- ACTO I (oscuro)
dark_env = smooth(ramp(T, 0.3, 4.0)) * (1 - smooth(ramp(T, 30.4, 31.5)))
drone = pad([55, 82.41, 110], [0.55, 0.35, 0.25], 0.18)
lfo = 0.8 + 0.2 * np.sin(2 * np.pi * 0.11 * T)
tension = pad([116.54, 174.61], [0.18, 0.12], 0.3) * smooth(ramp(T, 17.5, 22)) * (1 - smooth(ramp(T, 28.5, 30.5)))
dark = lowpass((drone * lfo * (0.16 + 0.07 * smooth(ramp(T, 17, 26))) + tension * 0.16), 900) * dark_env
L += dark; Rch += dark
air = lowpass(np.cumsum(rng.standard_normal(N)) * 0.002, 300)
air = highpass(air, 30) * 0.5 * dark_env
L += air; Rch += air * 0.9

# lluvia
rain_n = int(5 * SR)
rain = highpass(rng.standard_normal(rain_n), 2500) * 0.035
rain *= smooth(np.clip(np.arange(rain_n) / SR / 1.2, 0, 1)) * (1 - smooth(np.clip((np.arange(rain_n) / SR - 3.6) / 1.2, 0, 1)))
add(rain, 0.0, pan=-0.2)
for k in range(40):
    tk = rng.uniform(0.3, 4.2)
    add(pluck(rng.uniform(2500, 4500), 0.05, 0.008), tk, 0.03, rng.uniform(-0.9, 0.9), rev=0.4)

# la gota
add(sweep(1500, 480, 0.12, 0.6) * env_ad(int(0.12 * SR), 0.002, 0.06), 4.0, 0.55, 0.0, rev=1.0)
add(sweep(900, 380, 0.15, 0.5) * env_ad(int(0.15 * SR), 0.002, 0.07), 4.17, 0.22, 0.05, rev=1.0)
add(pluck(170, 0.8, 0.25), 4.0, 0.25, 0.0, rev=0.6)
for k in range(8):
    add(pluck(rng.uniform(1800, 3200), 0.08, 0.02), 4.05 + rng.uniform(0, 0.5), 0.05, rng.uniform(-0.6, 0.6), rev=0.8)

# zambullida
add(lowpass(whoosh(1.9, 100, 2500, 0.7), 1800), 7.9, 0.35, 0.0, rev=0.3)
# bajo el agua
uw_n = int(6.8 * SR)
uw = lowpass(np.cumsum(rng.standard_normal(uw_n)) * 0.01, 380)
uw = highpass(uw, 25)
uw /= np.abs(uw).max() + 1e-9
tt = np.arange(uw_n) / SR
uw *= smooth(np.clip(tt / 0.9, 0, 1)) * (1 - smooth(np.clip((tt - 5.6) / 1.0, 0, 1)))
add(uw, 8.6, 0.16, -0.1)
for k in range(46):
    tk = rng.uniform(9.0, 14.8)
    add(sweep(rng.uniform(250, 450), rng.uniform(900, 1500), 0.05, 2.0) * env_ad(int(0.05 * SR), 0.004, 0.02), tk, 0.08, rng.uniform(-0.8, 0.8), rev=0.7)
# transformación
for f in (1980, 2640, 3300):
    add(bell(f, 1.5, 0.35), 13.95, 0.025, rng.uniform(-0.4, 0.4), rev=1.0)
add(whoosh(1.6, 300, 4000, 0.4), 13.7, 0.18, 0.0, rev=0.4)

# zumbido del mosquito (sigue su vuelo)
MP = np.array([[960, 650], [760, 430], [1010, 250], [1390, 320], [1520, 560], [1240, 720], [2160, 430]], float)


def catmull(u):
    n = len(MP) - 1
    x = np.clip(u, 0, 1) * n
    i = np.minimum(np.floor(x).astype(int), n - 1)
    f = x - i
    p0 = MP[np.maximum(i - 1, 0)]; p1 = MP[i]; p2 = MP[i + 1]; p3 = MP[np.minimum(i + 2, n)]
    f = f[:, None]
    return 0.5 * ((2 * p1) + (-p0 + p2) * f + (2 * p0 - 5 * p1 + 4 * p2 - p3) * f ** 2 + (-p0 + 3 * p1 - 3 * p2 + p3) * f ** 3)


bz0, bz1 = 14.4, 17.95
bn = int((bz1 - bz0) * SR)
bt = np.arange(bn) / SR
pos = catmull(bt / (bz1 - bz0))
freq = 560 + 28 * np.sin(2 * np.pi * 7 * bt) + 12 * np.sin(2 * np.pi * 1.3 * bt)
ph = 2 * np.pi * np.cumsum(freq) / SR
saw = 2 * ((ph / (2 * np.pi)) % 1) - 1
buzz = bandpass(saw, 450, 5000) * (0.7 + 0.3 * np.sin(2 * np.pi * 90 * bt))
buzz *= smooth(np.clip(bt / 0.4, 0, 1)) * (1 - smooth(np.clip((bt - 3.1) / 0.45, 0, 1)))
add(buzz, bz0, 0.07, pan=(pos[:, 0] - 960) / 960 * 0.9)
# chispas de los puntos
for k in range(140):
    tk = 15.0 + (k / 140) * 2.6 + rng.uniform(-0.03, 0.03)
    add(pluck(rng.uniform(3000, 5200), 0.06, 0.012), tk, 0.018, rng.uniform(-0.7, 0.7), rev=0.7)

# datos
add(whoosh(1.5, 200, 2500, 0.5), 17.6, 0.16, 0.0, rev=0.3)
cnt = sweep(180, 420, 3.0, 0.6) * smooth(np.clip(np.linspace(0, 1, int(3.0 * SR)) * 4, 0, 1)) * np.linspace(1, 0.2, int(3.0 * SR))
add(cnt, 18.0, 0.035, 0.0, rev=0.2)
add(pluck(220, 1.2, 0.4), 21.05, 0.16, 0.25, rev=0.5)
add(pluck(330, 1.2, 0.4), 21.05, 0.10, 0.25, rev=0.5)
add(pluck(233.1, 1.4, 0.45), 21.5, 0.14, 0.35, rev=0.6)
# anillo
add(whoosh(1.6, 150, 2000, 0.5), 24.2, 0.14, 0.0, rev=0.4)
# vórtice
vn = int(2.1 * SR)
swirl = bandpass(rng.standard_normal(vn), 300, 3500) * np.linspace(0, 1, vn) ** 2
add(swirl, 28.6, 0.22, pan=np.sin(np.linspace(0, 14, vn)) * 0.6, rev=0.4)
add(sweep(140, 620, 2.0, 1.6) * np.linspace(0, 1, int(2.0 * SR)) ** 2, 28.65, 0.06, 0.0, rev=0.5)
# la gota se forma
add(bell(880, 2.2, 0.7), 30.55, 0.12, 0.0, rev=1.0)
add(bell(1318.5, 2.0, 0.5), 30.6, 0.06, 0.0, rev=1.0)

# ---------------------------------------------------------------- TRANSICIÓN Y ACTO II (claro)
for i, f in enumerate([523.25, 659.25, 783.99, 1046.5]):
    add(bell(f, 3.0, 1.1), 31.25 + i * 0.05, 0.07, (i - 1.5) * 0.3, rev=1.0)
add(whoosh(1.2, 400, 6000, 0.6), 31.0, 0.14, 0.0, rev=0.5)

light_env = smooth(ramp(T, 31.3, 34.5)) * (1 - smooth(ramp(T, 52.2, 53.4)))
lp = pad([130.81, 196.0, 293.66, 329.63], [0.5, 0.35, 0.22, 0.18], 0.35)
lp = lowpass(lp, 2200) * (0.85 + 0.15 * np.sin(2 * np.pi * 0.25 * T)) * light_env * 0.13
L += lp; Rch += lp

# logo: letras
for i in range(8):
    add(pluck(783.99 * (1 + i * 0.0), 0.3, 0.06), 33.0 + i * 0.05, 0.015, (i - 3.5) * 0.12, rev=0.6)
# arpegio de la demo
notes = [523.25, 392.0, 659.25, 587.33, 783.99, 659.25, 587.33, 493.88]
k = 0
t = 36.4
while t < 45.9:
    fade = min(1, (t - 36.4) / 1.0) * min(1, (45.9 - t) / 0.8)
    add(pluck(notes[k % len(notes)], 0.7, 0.18), t, 0.045 * fade, ((k % 4) - 1.5) * 0.25, rev=0.55)
    k += 1
    t += 0.3
add(whoosh(0.9, 200, 3000, 0.5), 36.2, 0.16, -0.3, rev=0.3)        # entra el teléfono
add(sweep(600, 1800, 1.2, 1.0) * np.hanning(int(1.2 * SR)), 37.5, 0.03, 0.0, rev=0.4)  # escaneo
for i, tb in enumerate([38.65, 39.05, 39.45]):                      # detecciones
    add(pluck(1318.5, 0.25, 0.04), tb, 0.10, -0.3 + i * 0.2, rev=0.4)
    add(pluck(1760.0, 0.25, 0.05), tb + 0.035, 0.07, -0.3 + i * 0.2, rev=0.4)
add(whoosh(0.5, 300, 3000, 0.4), 40.0, 0.08, -0.2)                 # hoja inferior
pn = int(1.0 * SR)                                                   # vaciar el agua
pour = bandpass(rng.standard_normal(pn), 400, 3500) * (0.6 + 0.4 * np.abs(np.sin(np.linspace(0, 60, pn)))) * np.hanning(pn)
add(pour, 41.6, 0.16, -0.25, rev=0.4)
add(lowpass(rng.standard_normal(int(0.4 * SR)), 1800) * env_ad(int(0.4 * SR), 0.005, 0.1), 42.35, 0.18, -0.2, rev=0.5)
add(thump(110, 60, 0.3, 0.09), 43.0, 0.30, -0.25, rev=0.2)          # balde volteado
for i, f in enumerate([659.25, 783.99, 1046.5]):                    # éxito
    add(bell(f, 1.8, 0.6), 43.35 + i * 0.09, 0.10, -0.2 + i * 0.2, rev=0.8)
for k in range(30):
    add(pluck(rng.uniform(2500, 5000), 0.08, 0.015), 43.4 + rng.uniform(0, 1.2), 0.025, rng.uniform(-0.9, 0.9), rev=0.8)
add(whoosh(0.8, 200, 3000, 0.4), 45.9, 0.12, 0.3, rev=0.3)          # sale el teléfono
for i, f in enumerate([523.25, 659.25, 783.99, 1046.5]):            # por qué IA
    tb = 46.95 + i * 1.32
    add(whoosh(0.5, 400, 5000, 0.5), tb - 0.12, 0.09, 0.4 - i * 0.25, rev=0.3)
    add(bell(f, 1.6, 0.5), tb + 0.05, 0.08, 0.0, rev=0.8)

# ---------------------------------------------------------------- CIERRE
add(lowpass(whoosh(1.4, 80, 2500, 0.75), 2200), 52.2, 0.32, 0.0, rev=0.4)
for i, tb in enumerate([53.4, 53.9, 54.4]):
    add(thump(90, 45, 0.9, 0.28), tb, 0.55, 0.0, rev=0.25)
    add(pluck([261.63, 329.63, 392.0][i], 1.4, 0.5), tb, 0.10, (i - 1) * 0.4, rev=0.7)
for i, f in enumerate([523.25, 783.99, 1046.5, 1318.5]):
    add(bell(f, 3.4, 1.4), 55.0 + i * 0.06, 0.06, (i - 1.5) * 0.3, rev=1.0)
end_env = smooth(ramp(T, 53.3, 54.6)) * (1 - smooth(ramp(T, 57.0, 58.35)))
ep = lowpass(pad([65.41, 98.0, 130.81, 164.81, 196.0], [0.5, 0.35, 0.3, 0.22, 0.15], 0.3), 1800) * end_env * 0.15
L += ep; Rch += ep

# ---------------------------------------------------------------- reverb y master
ir_n = int(2.6 * SR)
it = np.arange(ir_n) / SR
irL = lowpass(rng.standard_normal(ir_n), 5000) * np.exp(-it / 0.55)
irR = lowpass(rng.standard_normal(ir_n), 5000) * np.exp(-it / 0.55)
irL /= np.sqrt((irL ** 2).sum()); irR /= np.sqrt((irR ** 2).sum())
wetL = signal.fftconvolve(RevL, irL)[:N]
wetR = signal.fftconvolve(RevR, irR)[:N]
outL = L + 0.55 * wetL
outR = Rch + 0.55 * wetR
out = np.stack([outL, outR], axis=1)
out = highpass(out.T, 25).T
peak = np.abs(out).max()
out = np.tanh(out / peak * 1.25) / np.tanh(1.25) * 0.89
fade = np.ones(N); fi = int(0.05 * SR); fade[:fi] = np.linspace(0, 1, fi)
out *= fade[:, None]
wavfile.write(sys.argv[1] if len(sys.argv) > 1 else "audio.wav", SR, (out * 32767).astype(np.int16))
print("ok", out.shape, float(np.abs(out).max()))
