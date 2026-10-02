# Privacidad

- La app no declara el permiso `INTERNET`; CI lo verifica (`scripts/check-permissions.sh`) y bloquea código de red (`scripts/check-no-network-code.sh`).
- Los fotogramas se procesan solo en RAM. Mientras se muestran resultados se conserva el fotograma analizado en memoria (nunca en disco) y se descarta al salir.
- El historial (Room) guarda solo clase, estado, confianza, condición, acción y hora. Nunca imágenes.
- `allowBackup=false`.
- Permisos: `CAMERA` y `ACCESS_NETWORK_STATE` (solo lectura, para mostrar "sin conexión").
