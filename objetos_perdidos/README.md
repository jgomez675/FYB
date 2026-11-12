# FYB - Objetos perdidos

Aplicación web para publicar y recuperar objetos perdidos entre estudiantes. El servidor es
**Spring Boot** (Java 17) y la interfaz es HTML, CSS y JavaScript servidos desde
`src/main/resources/static`.

## Cómo ejecutarlo
Necesitas **JDK 17 o superior** y **Maven**.

- Linux / Mac: `./run.sh`
- Windows: doble clic en `run.bat` (o `mvn spring-boot:run` en PowerShell)
- Empaquetado: `mvn package` y luego `java -jar target/fyb.jar`

Abre http://localhost:8080. Ejecútalo **siempre desde esta carpeta** (`objetos_perdidos`): la carpeta
`data/` se busca desde donde arranca el servidor.

## Configuración (variables de entorno)
| Variable | Qué cambia | Por defecto |
|---|---|---|
| `FYB_NAME` | Nombre de la app | `FYB` |
| `FYB_TAGLINE` | Eslogan | `Encuentra lo que creías perdido` |
| `FYB_EMAIL_DOMAIN` | Dominio de correo permitido | `usa.edu.co` |
| `FYB_ADMIN_EMAIL` | Correo administrador | `admin@usa.edu.co` |
| `FYB_COOKIE_SECURE` | `true` si la sirves por https | `false` |
| `PORT` | Puerto | `8080` |
| `FYB_DB_URL` / `FYB_DB_USER` / `FYB_DB_PASSWORD` | Base de datos | H2 en `data/fyb-db` |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_USERNAME`, ... | Envío del código de recuperación de contraseña | sin correo |

## Estructura
```
src/main/java/back/
├── FyBApplication.java      arranque de Spring Boot
├── config/                  nombre, eslogan, dominio, admin
├── model/                   Usuario, ObjetoPerdido, ReportePublicacion
├── service/                 AuthService, ObjetoPerdidoService, PuntosService, ...
├── controller/              API REST (/api/...)
├── api/                     sesiones, errores, imágenes
├── persistence/             base de datos (entidades, repositorios, sincronización)
├── historial/               historial de puntos
└── recompensas/             canje de puntos por recompensas
src/main/resources/
├── application.properties   puerto y base de datos
└── static/                  front (index.html, Main.js, vistas por carpeta)
data/                        archivos de datos (usuarios, objetos, puntos, categorías)
```

## Historias de usuario implementadas
- **Perfil:** editar nombre y contraseña (`#/perfil`, `/api/perfil`).
- **Base de datos:** usuarios, objetos, puntos y categorías también se guardan en una base de datos
  (ver `README-BASE-DATOS.md`).
- **Historial de puntos:** ver puntos ganados y gastados y cómo cambió el saldo
  (`#/puntos/historial`, `/api/puntos/historial`; ver `README-HISTORIAL-PUNTOS.md`).
- **Canje de recompensas:** canjear puntos por recompensas cuando el saldo alcanza, con código para reclamar
  (`#/puntos`, `/api/recompensas`; ver `README-RECOMPENSAS.md`).

## Puntos: cómo se guardan
- Cada movimiento es una línea de `data/puntos.txt`: `correo|cantidad|motivo|fecha`. Cantidad positiva =
  ganado, negativa = gastado. El saldo es la suma de las líneas.
- Al sumar o restar puntos **primero se escribe en el archivo y después se cambia el saldo en memoria**:
  si el disco falla, el saldo no cambia y queda un aviso en la consola, así que nunca se desincronizan.
- `PuntosService.descontarPuntos` descuenta puntos (valida que alcance el saldo) y la usa el canje de
  recompensas (`back/recompensas`).
- La devolución solo se confirma desde `/api/objetos/{id}/confirmar-devolucion` (la hace el dueño del
  objeto). El antiguo `/validar-devolucion`, que daba 20 puntos a cualquiera sin confirmación, se eliminó.
- Marcar un objeto como devuelto es atómico: dos confirmaciones simultáneas no pueden dar los puntos dos veces.

## Datos privados
`data/usuarios.txt`, `data/objetos.txt`, `data/objetos/*` y `data/fyb-db*` están en `.gitignore` y no
deben subirse: contienen correos y contraseñas con hash.
