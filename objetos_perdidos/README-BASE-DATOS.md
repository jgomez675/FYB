# HU: Base de datos para usuarios, objetos, puntos y categorías

> Como equipo de desarrollo, quiero guardar usuarios, objetos, puntos y categorías en una base
> de datos, para que no se pierdan al reiniciar y el sistema pueda crecer.

## Qué se agregó (todo lo nuevo vive en `back.persistence`)

```
src/main/java/back/persistence/
├── entity/        UsuarioEntity, CategoriaEntity, ObjetoEntity, MovimientoPuntosEntity
├── repository/    UsuarioRepository, CategoriaRepository, ObjetoRepository, MovimientoPuntosRepository
├── sync/          SincronizacionService, SincronizacionProgramada, PersistenciaConfig
└── BaseDatosController.java    GET /api/db/resumen (solo admin)
src/main/resources/application.properties    conexión a la base (H2 en archivo)
```

Cambios en archivos que ya existían: solo `pom.xml` (2 dependencias: `spring-boot-starter-data-jpa`
y `h2`) y `.gitignore` (ignora `data/fyb-db*`). **Ningún `.java` existente se modificó.**

## Cómo funciona
- La base es un archivo H2 en `data/fyb-db.mv.db`: no hay que instalar nada y sobrevive a los reinicios.
- Las tablas se crean solas al arrancar (`ddl-auto=update`): `usuarios`, `categorias`, `objetos`, `movimientos_puntos`.
- Los servicios actuales (`AuthService`, `ObjetoPerdidoService`, `PuntosService`) siguen escribiendo en `data/*.txt`.
  La base se llena a partir de ellos: al arrancar, cada 30 s (`fyb.db.sync-ms`) y al apagar.
- La copia es "upsert": no duplica y **nunca borra** de la base, así que también sirve de respaldo.
- Las categorías guardan además `puntos_devolucion` (se siembra con los valores de `PuntosService`).

## Cómo probarlo
1. `mvn spring-boot:run` (o `run.bat`) desde la carpeta `objetos_perdidos`.
2. Registra un usuario o publica un objeto y espera ~30 s (o reinicia la app).
3. Inicia sesión con el correo admin y abre `http://localhost:8080/api/db/resumen`:
   devuelve cuántos usuarios, categorías, objetos y movimientos de puntos hay en la base.

## Variables de entorno
| Variable | Qué cambia | Por defecto |
|---|---|---|
| `FYB_DB_URL` | URL JDBC | `jdbc:h2:file:./data/fyb-db;DB_CLOSE_ON_EXIT=FALSE` |
| `FYB_DB_USER` | Usuario | `sa` |
| `FYB_DB_PASSWORD` | Contraseña | (vacía) |

Para PostgreSQL/MySQL: cambia la dependencia `h2` por el driver y define `FYB_DB_URL`, `FYB_DB_USER`, `FYB_DB_PASSWORD`.

## Siguiente paso sugerido (otra historia)
Hoy la base es un espejo. Cuando quieras que sea la fuente principal, se migran los servicios
para leer/escribir con los repositorios y se retiran los `.txt`.

## Notas
- `objetos.correo_usuario` y los correos de `movimientos_puntos` no tienen llave foránea a `usuarios`
  a propósito: `puntos.txt` ya trae correos de prueba sin cuenta registrada.
- La base contiene contraseñas (con hash) y por eso queda fuera de git.
