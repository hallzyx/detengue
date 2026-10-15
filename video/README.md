# Video de presentación

El video entregado (problema + solución con IA + demo en el teléfono) está enlazado en el [README](../README.md).
Esta carpeta contiene solo la **parte animada del intro**, que se generó con código para poder regenerarla:

| Carpeta | Contenido |
|---|---|
| `v3/` | versión final del intro (73 s, 60 fps): `intro_v3.html` (animación determinista `draw(t)`), `music_v3.py` (música y efectos sintetizados), `render.py` |
| `v2/`, `./intro.html` | iteraciones anteriores |
| `detengue-intro-v3.mp4` | render del intro v3 |

## Regenerar

```bash
pip install playwright numpy scipy && playwright install chromium   # además de ffmpeg
cd video/v3
python3 render.py frames 0 4380         # 73 s x 60 fps: captura cada fotograma con Chromium (frames/f%05d.jpg)
python3 music_v3.py music.wav           # música y efectos sintetizados
python3 render.py encode intro.mp4 music.wav
```

El audio de `music.wav` se normaliza con `ffmpeg loudnorm` antes de codificar. `python3 render.py stills 3 9.5` saca cuadros sueltos para revisar.
Los cuadros y el audio intermedios están en `.gitignore`. La animación es determinista: el mismo código produce el mismo video.

## Datos del intro

- CDC-Minsa, hasta la semana epidemiológica 33 de 2026: 46 669 casos y 57 fallecidos (+53 % frente a 30 587 en el mismo periodo de 2025). Datos provisionales.
- INEI, IV trimestre de 2025: 95,7 % de hogares con celular (88,1 % en zonas rurales).

El teléfono, el patio y los baldes del intro son **ilustraciones**; el demo real (capturas del teléfono) va aparte, en el video final.
