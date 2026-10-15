# Privacidad

- La app no declara el permiso `INTERNET`; CI lo verifica (`scripts/check-permissions.sh`) y bloquea código de red (`scripts/check-no-network-code.sh`).
- Los fotogramas se procesan solo en RAM. Mientras se muestran resultados se conserva el fotograma analizado en memoria (nunca en disco) y se descarta al salir.
- La **galería del barrido de zona** (5 s) también vive solo en RAM y se pierde al cerrar la revisión.
- El historial (Room) guarda solo clase, estado, confianza, condición, acción y hora. Nunca imágenes.
- La **gamificación** (racha, alertas atendidas, insignias) se calcula de ese mismo historial: no hay tablas nuevas, cuentas, ni envío de datos.
- Se puede borrar el historial desde la app (*Mis revisiones → Borrar historial*), lo que reinicia también la racha y las insignias.
- `allowBackup=false`.
- Permisos: `CAMERA` y `ACCESS_NETWORK_STATE` (solo lectura, para mostrar "sin conexión").
- Sin analítica, sin SDK de terceros que se conecte a la red y sin Play Services (TFLite directo).
