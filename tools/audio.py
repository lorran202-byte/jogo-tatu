"""Gera todos os efeitos sonoros e a música do jogo (sem nenhum arquivo de terceiros).

Uso: python3 tools/audio.py assets
"""
import os
import subprocess
import sys
import wave

import numpy as np

SR = 22050
OUT = sys.argv[1] if len(sys.argv) > 1 else "assets"
rng = np.random.default_rng(7)


def t_of(dur):
    return np.arange(int(SR * dur)) / SR


def env(n, a=0.005, r=0.08, sustain=1.0):
    e = np.ones(n) * sustain
    na, nr = int(SR * a), int(SR * r)
    if na:
        e[:na] = np.linspace(0, 1, na)
    if nr and nr < n:
        e[-nr:] *= np.linspace(1, 0, nr)
    return e


def square(freq, t, duty=0.5):
    phase = np.cumsum(np.broadcast_to(freq, t.shape) / SR) % 1.0
    return np.where(phase < duty, 1.0, -1.0)


def tri(freq, t):
    phase = np.cumsum(np.broadcast_to(freq, t.shape) / SR) % 1.0
    return 4 * np.abs(phase - 0.5) - 1


def sine(freq, t):
    phase = np.cumsum(np.broadcast_to(freq, t.shape) / SR)
    return np.sin(2 * np.pi * phase)


def noise(n):
    return rng.uniform(-1, 1, n)


def lowpass(x, k=0.2):
    y = np.zeros_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc += k * (v - acc)
        y[i] = acc
    return y


def save_wav(path, x, vol=0.8):
    x = np.clip(x / max(1e-9, np.max(np.abs(x))) * vol, -1, 1)
    data = (x * 32767).astype(np.int16)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data.tobytes())


def sweep(f0, f1, dur, wave_fn=square, duty=0.5, curve=1.0):
    t = t_of(dur)
    f = f0 + (f1 - f0) * (t / dur) ** curve
    if wave_fn is square:
        return square(f, t, duty)
    return wave_fn(f, t)


def notes(seq, dur_each, wave_fn=square, duty=0.25):
    parts = []
    for f in seq:
        t = t_of(dur_each)
        s = square(f, t, duty) if wave_fn is square else wave_fn(f, t)
        parts.append(s * env(len(t), 0.002, dur_each * 0.4))
    return np.concatenate(parts)


def sfx():
    d = os.path.join(OUT, "sfx")
    x = sweep(260, 620, 0.16, square, 0.3, 0.6)
    save_wav(f"{d}/pulo.wav", x * env(len(x), 0.002, 0.08), 0.55)

    t = t_of(0.42)
    f = 300 + 180 * np.sin(2 * np.pi * 14 * t)
    x = tri(f, t) * 0.7 + noise(len(t)) * 0.25
    save_wav(f"{d}/giro.wav", lowpass(x, 0.35) * env(len(t), 0.01, 0.15), 0.5)

    n = noise(int(SR * 0.22))
    x = lowpass(n, 0.45) * np.exp(-np.arange(len(n)) / (SR * 0.05))
    x += sweep(180, 70, 0.22, square, 0.5) * 0.4 * np.exp(-np.arange(len(n)) / (SR * 0.06))
    save_wav(f"{d}/caixa.wav", x, 0.7)

    x = notes([1320, 1760], 0.055, square, 0.25)
    save_wav(f"{d}/acai.wav", x, 0.4)

    x = sweep(200, 900, 0.3, square, 0.4, 0.5)
    save_wav(f"{d}/quique.wav", x * env(len(x), 0.002, 0.12), 0.5)

    x = notes([523, 659, 784, 1047], 0.08, square, 0.5)
    save_wav(f"{d}/checkpoint.wav", x, 0.5)

    x = notes([392, 523, 659, 784, 1047, 1319], 0.07, square, 0.25)
    save_wav(f"{d}/vida.wav", x, 0.5)

    x = sweep(500, 90, 0.6, square, 0.5, 0.7)
    x = x * env(len(x), 0.002, 0.2) + lowpass(noise(len(x)), 0.2) * 0.3 * env(len(x), 0.002, 0.5)
    save_wav(f"{d}/dano.wav", x, 0.6)

    x = sweep(700, 150, 0.14, square, 0.5)
    x = x * env(len(x), 0.002, 0.06) + lowpass(noise(len(x)), 0.5) * 0.4 * env(len(x), 0.001, 0.1)
    save_wav(f"{d}/esmaga.wav", x, 0.55)

    x = notes([523, 659, 784, 659, 784, 1047, 1047, 1319], 0.11, square, 0.25)
    save_wav(f"{d}/vitoria.wav", x, 0.5)

    x = notes([880], 0.04, square, 0.5)
    save_wav(f"{d}/clique.wav", x, 0.35)


def midi(n):
    return 440.0 * 2 ** ((n - 69) / 12)


def music():
    bpm = 128
    beat = 60 / bpm
    step = beat / 4  # semicolcheia
    bars = 16
    total = int(SR * step * 16 * bars)
    mix = np.zeros(total)

    def place(sig, start_step, gain):
        s = int(round(start_step * step * SR))
        e = min(total, s + len(sig))
        mix[s:e] += sig[: e - s] * gain

    # Progressão (C, Am, F, G) x4 com variação no fim
    chords = [(48, [60, 64, 67]), (45, [57, 60, 64]), (41, [57, 60, 65]), (43, [55, 59, 62])] * 3 + \
             [(41, [57, 60, 65]), (43, [55, 59, 62]), (48, [60, 64, 67]), (43, [55, 59, 62])]

    # Baixo sincopado (balanço de carimbó, padrão próprio): passos 0, 3, 6, 8, 11, 14
    for bar, (root, _) in enumerate(chords):
        for k, (st, iv) in enumerate([(0, 0), (3, 7), (6, 12), (8, 0), (11, 7), (14, 10 if bar % 4 == 3 else 12)]):
            dur = step * 2.2
            t = t_of(dur)
            sig = tri(midi(root + iv - 12), t) * env(len(t), 0.003, dur * 0.5)
            place(sig, bar * 16 + st, 0.55)

    # Acordes curtos (contratempo) com onda quadrada fina
    for bar, (_, ch) in enumerate(chords):
        for st in (2, 6, 10, 14):
            dur = step * 1.2
            t = t_of(dur)
            sig = sum(square(midi(n), t, 0.125) for n in ch) / 3 * env(len(t), 0.002, dur * 0.6)
            place(sig, bar * 16 + st, 0.16)

    # Melodia: (passo, nota MIDI, duração em passos) por compasso de 2 compassos
    motif_a = [(0, 72, 3), (3, 74, 3), (6, 76, 2), (8, 79, 4), (12, 76, 2), (14, 74, 2),
               (16, 72, 3), (19, 69, 3), (22, 72, 2), (24, 74, 6), (30, 72, 2)]
    motif_b = [(0, 77, 3), (3, 76, 3), (6, 74, 2), (8, 72, 4), (12, 74, 2), (14, 76, 2),
               (16, 74, 3), (19, 71, 3), (22, 74, 2), (24, 79, 6), (30, 77, 2)]
    motif_c = [(0, 81, 2), (2, 79, 2), (4, 76, 2), (6, 79, 4), (10, 76, 2), (12, 74, 4),
               (16, 72, 2), (18, 74, 2), (20, 76, 4), (24, 72, 8)]
    song = [motif_a, motif_b, motif_a, motif_b, motif_a, motif_b, motif_c, motif_c]
    for i, mot in enumerate(song):
        base = i * 32
        for st, n, ln in mot:
            dur = step * ln
            t = t_of(dur)
            vib = 1 + 0.006 * np.sin(2 * np.pi * 5.5 * t) * (t > 0.12)
            sig = square(midi(n) * vib, t, 0.25) * env(len(t), 0.004, min(dur * 0.5, 0.12), 0.85)
            place(sig, base + st, 0.30)
            place(sig, base + st + 3, 0.08)  # eco

    # Percussão: bumbo, ganzá (chocalho), clave
    kick_t = t_of(0.16)
    kick = sine(110 * np.exp(-kick_t * 25) + 45, kick_t) * np.exp(-kick_t * 18)
    shaker = lowpass(noise(int(SR * 0.05)), 0.9) * np.exp(-np.arange(int(SR * 0.05)) / (SR * 0.012))
    clave_t = t_of(0.07)
    clave = sine(2100, clave_t) * np.exp(-clave_t * 60)
    for bar in range(bars):
        for st in (0, 8):
            place(kick, bar * 16 + st, 0.8)
        for st in range(0, 16, 2):
            place(shaker, bar * 16 + st, 0.22 if st % 4 else 0.12)
        for st in (0, 3, 7, 10, 12):
            place(clave, bar * 16 + st, 0.18)

    d = os.path.join(OUT, "music")
    os.makedirs(d, exist_ok=True)
    tmp = os.path.join(d, "tema.wav")
    save_wav(tmp, mix, 0.7)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp, "-c:a", "libvorbis", "-q:a", "3",
                    os.path.join(d, "tema.ogg")], check=True)
    os.remove(tmp)


if __name__ == "__main__":
    sfx()
    music()
    print("ok")
