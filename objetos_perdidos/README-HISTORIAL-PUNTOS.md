# HU: Historial de puntos

> Como estudiante, quiero ver el historial de los puntos que gané y gasté, para entender cómo llegué a mi saldo.

## Qué se agregó
- `back/historial/HistorialPuntosService.java` y `HistorialPuntosController.java` (paquete nuevo).
  `GET /api/puntos/historial` devuelve, solo para quien tiene la sesión iniciada:
  `saldo`, `totalGanado`, `totalGastado` y `movimientos` (más reciente primero), cada uno con
  `cantidad`, `tipo` (`GANADO`/`GASTADO`), `motivo`, `fecha` y `saldoDespues`.
- `static/puntos/HistorialPuntosView.js`: pantalla en `#/puntos/historial` con resumen y tabla.

## Cambios en archivos existentes (mínimos)
- `static/Main.js`: la ruta `#/puntos/historial`.
- `static/index.html`: el `<script>` de la vista nueva.
- `static/puntos/PuntosView.js`: el botón "Ver historial de puntos".

## Cómo se calcula
Lee `data/puntos.txt` (lo que ya escribe `PuntosService`), sin modificarlo. Un movimiento positivo es
"ganado" y uno negativo es "gastado". El saldo coincide con el de "Mis puntos".

## Nota
Hoy la app no tiene una pantalla que reste puntos (el botón "Canjear recompensa" aún no hace nada), así que
por ahora solo aparecerán movimientos ganados. `PuntosService.descontarPuntos` ya está lista: cuando el canje
la use, el gasto queda guardado en `puntos.txt` con cantidad negativa y el historial lo mostrará solo.
