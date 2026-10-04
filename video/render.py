"""Renderiza intro.html a PNG/JPEG por fotograma (tiempo determinista) y lo une con ffmpeg.
Uso:  python3 render.py stills 3 9.5 14       -> PNGs de prueba en frames_test/
      python3 render.py frames START END      -> JPEG de los fotogramas [START, END) en frames/
      python3 render.py encode out.mp4        -> une frames/ en MP4
"""
import subprocess, sys
from pathlib import Path
from playwright.sync_api import sync_playwright

FPS = 30
HERE = Path(__file__).parent
URL = (HERE / "intro.html").resolve().as_uri()

def session(p):
    b = p.chromium.launch(args=["--font-render-hinting=none"])
    pg = b.new_page(viewport={"width": 1920, "height": 1080})
    pg.goto(URL); pg.wait_for_function("window.setTime !== undefined")
    pg.evaluate("document.fonts.ready")
    return b, pg

def main():
    cmd = sys.argv[1]
    if cmd == "stills":
        out = HERE / "frames_test"; out.mkdir(exist_ok=True)
        with sync_playwright() as p:
            b, pg = session(p)
            for t in map(float, sys.argv[2:]):
                pg.evaluate(f"setTime({t})"); pg.screenshot(path=str(out / f"t{t:06.2f}.png"))
            b.close()
    elif cmd == "frames":
        a, z = int(sys.argv[2]), int(sys.argv[3])
        out = HERE / "frames"; out.mkdir(exist_ok=True)
        with sync_playwright() as p:
            b, pg = session(p)
            for i in range(a, z):
                pg.evaluate(f"setTime({i / FPS})")
                pg.screenshot(path=str(out / f"f{i:05d}.jpg"), type="jpeg", quality=94)
            b.close()
    elif cmd == "encode":
        subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-framerate", str(FPS), "-i", str(HERE / "frames" / "f%05d.jpg"),
                        "-c:v", "libx264", "-preset", "slow", "-crf", "18", "-pix_fmt", "yuv420p", "-movflags", "+faststart", sys.argv[2]], check=True)

main()
