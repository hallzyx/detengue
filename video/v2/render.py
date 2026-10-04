"""Render determinista de intro_v2.html.
  python3 render.py stills 3 9.5        -> frames_test/*.png
  python3 render.py frames A B          -> frames/f%05d.jpg para los índices [A,B) a FPS
  python3 render.py encode out.mp4 [audio.wav]
"""
import subprocess, sys
from pathlib import Path
from playwright.sync_api import sync_playwright

FPS = 60
HERE = Path(__file__).parent
URL = (HERE / "intro_v2.html").resolve().as_uri()

def session(p):
    b = p.chromium.launch()
    pg = b.new_page(viewport={"width": 1920, "height": 1080})
    pg.goto(URL); pg.wait_for_function("window.ready !== undefined"); pg.evaluate("window.ready")
    return b, pg

cmd = sys.argv[1]
if cmd == "stills":
    out = HERE / "frames_test"; out.mkdir(exist_ok=True)
    with sync_playwright() as p:
        b, pg = session(p)
        for t in map(float, sys.argv[2:]):
            pg.evaluate(f"setTime({t})"); pg.locator("canvas").screenshot(path=str(out / f"t{t:06.2f}.png"))
        b.close()
elif cmd == "frames":
    a, z = int(sys.argv[2]), int(sys.argv[3])
    out = HERE / "frames"; out.mkdir(exist_ok=True)
    with sync_playwright() as p:
        b, pg = session(p)
        for i in range(a, z):
            pg.evaluate(f"setTime({i / FPS})")
            pg.screenshot(path=str(out / f"f{i:05d}.jpg"), type="jpeg", quality=93)
        b.close()
elif cmd == "encode":
    args = ["ffmpeg", "-loglevel", "error", "-y", "-framerate", str(FPS), "-i", str(HERE / "frames" / "f%05d.jpg")]
    if len(sys.argv) > 3: args += ["-i", sys.argv[3]]
    args += ["-c:v", "libx264", "-preset", "slow", "-crf", "18", "-pix_fmt", "yuv420p", "-movflags", "+faststart"]
    if len(sys.argv) > 3: args += ["-c:a", "aac", "-b:a", "192k", "-shortest"]
    subprocess.run(args + [sys.argv[2]], check=True)
