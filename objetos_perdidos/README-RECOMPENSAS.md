# HU: Canje de recompensas

> Como estudiante, quiero canjear mis puntos por recompensas reales cuando alcance el saldo, para recibir los beneficios de la aplicación.

## Qué se agregó
- `back/recompensas/RecompensasService.java` y `RecompensasController.java` (paquete nuevo).
  - `GET /api/recompensas`: devuelve `saldo`, `recompensas` (catálogo) y `canjes` (los del estudiante en sesión).
  - `POST /api/recompensas/{id}/canjear`: canjea la recompensa si el saldo alcanza. Devuelve `mensaje`, `canje` (con `codigo`) y el `saldo` nuevo.
- `static/puntos/PuntosView.js`: las recompensas ahora vienen del servidor; el botón se activa cuando el saldo alcanza el costo
  (si no, dice cuántos puntos faltan), pide confirmación, y al canjear muestra el código y actualiza el saldo. Abajo aparece la lista "Mis canjes".

## Cómo funciona
- Se usa `PuntosService.descontarPuntos`: valida el saldo y deja un movimiento negativo en `data/puntos.txt`
  (`Canje de recompensa: <nombre> (<código>)`), así que **el historial de puntos lo muestra como "gastado" sin cambios**.
- Cada canje genera un código (`FYB-XXXXXX`) que se guarda en `data/canjes.txt`: `codigo|correo|idRecompensa|nombre|costo|fecha`.
  El estudiante presenta ese código para reclamar el beneficio.
- Siempre se usa el correo de la sesión: nadie puede canjear ni ver canjes de otra persona.
- El canje es atómico (mismo candado que `PuntosService`): dos clics o dos pestañas no pueden gastar los mismos puntos dos veces.
  Si falla el guardado del canje, los puntos se devuelven con un movimiento "Reembolso".

## Catálogo de recompensas
Por defecto hay tres (100, 250 y 500 puntos). Para cambiarlas crea `data/recompensas.txt`, una por línea:

```
id|nombre|descripcion|costo
1|Bono de cafetería|Vale para una bebida o un snack en la cafetería del campus.|100
```
Las líneas que empiezan con `#` se ignoran. Si el archivo no existe o queda vacío se usa el catálogo por defecto.

## Cambios en archivos existentes (mínimos)
- `static/puntos/PuntosView.js` (reescrito para usar la API).
- `.gitignore`: `data/canjes.txt` (tiene correos de estudiantes).
- `README.md`: lista de historias y la nota sobre `descontarPuntos`.

## Cómo probarlo
1. Entra con un usuario con puntos (por ejemplo `julian.prueba@usa.edu.co`, 400 puntos en `data/puntos.txt`).
2. `#/puntos`: las recompensas de 100 y 250 puntos quedan activas; la de 500 muestra "Te faltan 100 puntos".
3. Canjea una: aparece el código, el saldo baja y el canje sale en "Mis canjes" y en `#/puntos/historial` como gasto.
