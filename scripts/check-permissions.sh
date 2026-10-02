#!/usr/bin/env bash
# Verifica que el APK respeta el diseño offline-first / privacy-first del PRD (§13, §37, AC-10):
#   - NO declara el permiso INTERNET (la app no puede abrir conexiones).
#   - Solo declara los permisos de la lista permitida.
#
# Uso:
#   scripts/check-permissions.sh <app.apk>        # usa aapt2 del Android SDK
#   scripts/check-permissions.sh --from-file <f>  # lee un volcado `aapt2 dump permissions` ya hecho (tests)
set -euo pipefail

ALLOWED_REGEX='^(android\.permission\.CAMERA|android\.permission\.ACCESS_NETWORK_STATE|[A-Za-z0-9_.]+\.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION)$'

if [[ "${1:-}" == "--from-file" ]]; then
  dump="$(cat "${2:?falta el archivo}")"
else
  apk="${1:?uso: $0 <app.apk> | --from-file <volcado>}"
  sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  aapt2="$(ls -1 "$sdk"/build-tools/*/aapt2 2>/dev/null | sort -V | tail -1 || true)"
  if [[ -z "$aapt2" ]]; then
    echo "No se encontró aapt2 (define ANDROID_HOME)." >&2
    exit 2
  fi
  dump="$("$aapt2" dump permissions "$apk")"
fi

perms="$(printf '%s\n' "$dump" | grep -oE "name='[^']+'" | sed -E "s/name='([^']+)'/\1/" | sort -u || true)"

echo "Permisos declarados:"
printf '%s\n' "${perms:-(ninguno)}" | sed 's/^/  - /'

status=0
if printf '%s\n' "$perms" | grep -qx 'android.permission.INTERNET'; then
  echo "ERROR: el APK declara android.permission.INTERNET (viola AC-10)." >&2
  status=1
fi
while IFS= read -r p; do
  [[ -z "$p" ]] && continue
  if ! [[ "$p" =~ $ALLOWED_REGEX ]]; then
    echo "ERROR: permiso no permitido: $p" >&2
    status=1
  fi
done <<< "$perms"

[[ $status -eq 0 ]] && echo "OK: sin INTERNET y solo permisos permitidos."
exit $status
