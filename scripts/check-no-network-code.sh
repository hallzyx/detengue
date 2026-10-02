#!/usr/bin/env bash
# Guardia de código (PRD §0.2, AC-10): el código de la app no debe usar APIs de red.
# Complementa a check-permissions.sh: aunque alguien añadiera código de red, sin permiso INTERNET fallaría,
# pero preferimos detectarlo en CI antes de que llegue a un teléfono.
set -euo pipefail

root="${1:-android/app/src/main}"
pattern='java\.net\.(URL|HttpURLConnection|Socket)|javax\.net|okhttp|retrofit|ktor|android\.net\.http|WebView|HttpClient'

if matches="$(grep -RInE "$pattern" "$root" --include='*.kt' --include='*.java' 2>/dev/null)"; then
  echo "ERROR: se encontró código que usa APIs de red:" >&2
  echo "$matches" >&2
  exit 1
fi
echo "OK: ningún uso de APIs de red en $root"
