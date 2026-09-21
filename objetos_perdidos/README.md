# FYB - Objetos perdidos (versión web)

La lógica sigue en **Java** (`src/back`) y ahora corre como servidor. La interfaz (`src/front`)
es una página en HTML, CSS y JavaScript que habla con ese servidor. Mantiene la división
back / front del proyecto original.

## Cómo ejecutarlo
Necesitas un **JDK 17 o superior** (no hay que descargar librerías).

- Linux / Mac: `./run.sh`
- Windows: doble clic en `run.bat`
- Con Maven: `mvn package` y luego `java -jar target/fyb.jar` (ejecútalo desde esta carpeta)

Abre http://localhost:8080. Siempre se ejecuta desde esta carpeta, porque busca `data/` y `src/front/` desde aquí.

## Cambiar el nombre de la app
El nombre vive en un solo lugar: `src/back/config/AppConfig.java` (`NOMBRE`). El front lo pide al
servidor al arrancar, así que el título de la pestaña, la cabecera y los textos cambian solos.
También puedes cambiarlo sin tocar código con variables de entorno:

| Variable | Qué cambia | Por defecto |
|---|---|---|
| `FYB_NAME` | Nombre de la app | `FYB` |
| `FYB_TAGLINE` | Eslogan | `Encuentra lo que creías perdido` |
| `FYB_EMAIL_DOMAIN` | Dominio de correo permitido | `usa.edu.co` |
| `PORT` | Puerto | `8080` |
| `FYB_FRONT_DIR` | Carpeta del front | `src/front` |
| `FYB_COOKIE_SECURE` | `true` si la sirves por https | `false` |

## Estructura
```
src/
├── Main.java                         arranca el servidor
├── back/
│   ├── config/AppConfig.java         nombre, eslogan, dominio, puerto
│   ├── model/                        Usuario, ObjetoPerdido (sin cambios)
│   ├── service/                      AuthService, ObjetoPerdidoService, Archivos
│   ├── validation/EmailValidator.java
│   └── api/                          capa HTTP (ApiServer y ayudas)
└── front/
    ├── index.html, config.js, Main.js
    ├── servicios/                    llamadas al servidor (ApiClient, AuthApi, ObjetosApi)
    ├── components/                   piezas compartidas
    ├── login/  register/  objetos/   las pantallas
    └── styles/styles.css
data/                                 usuarios.txt, objetos.txt y fotos (se crean solos)
```

## API
| Método y ruta | Qué hace | Sesión |
|---|---|---|
| `GET /api/config` | Nombre, eslogan y dominio | no |
| `POST /api/registro` | Crea una cuenta | no |
| `POST /api/login` | Inicia sesión (cookie) | no |
| `POST /api/logout` | Cierra sesión | no |
| `GET /api/sesion` | Usuario actual o `null` | no |
| `GET /api/objetos` | Lista de objetos | sí |
| `GET /api/objetos/{id}` | Detalle de un objeto | sí |
| `POST /api/objetos` | Publica un objeto (foto en base64) | sí |
| `GET /api/imagenes/{archivo}` | Foto de un objeto | sí |

## Qué cambió respecto a la versión de escritorio
- Se quitaron las vistas JavaFX (`src/front/*.java`) y la dependencia de JavaFX del `pom.xml`. Siguen en el historial de Git.
- Los modelos y la estructura de `data/` no cambian; `usuarios.txt` y `objetos.txt` mantienen su formato.
- **Contraseñas:** ahora se guardan con hash (PBKDF2). Las cuentas antiguas en texto plano siguen funcionando y se convierten solas la primera vez que inician sesión.
- **Servidor:** las validaciones se repiten en Java (tamaños máximos, fecha válida, la foto se verifica por su contenido); el autor de cada objeto sale de la sesión; cinco intentos fallidos de inicio de sesión bloquean diez minutos.
- Se añadió cerrar sesión, un buscador en la lista y un botón para contactar a quien publicó el objeto.

## Para publicarla en internet
El servidor incluido habla HTTP simple. Ponle delante un proxy con HTTPS (Caddy, nginx...), que
reenvíe la cabecera `X-Forwarded-Proto: https`, o arranca con `FYB_COOKIE_SECURE=true`.
Las sesiones viven en memoria: al reiniciar el servidor, todos deben volver a iniciar sesión.
