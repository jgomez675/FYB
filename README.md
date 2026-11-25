# FYB - Encuentra lo que creías perdido

FYB es una aplicación para publicar, consultar y recuperar objetos perdidos dentro de la comunidad universitaria. El proyecto incluye un backend desarrollado con Spring Boot, una interfaz web y una aplicación Android construida con Capacitor.

## Componentes

- **Backend:** Java 17 y Spring Boot. Expone los servicios REST y sirve la interfaz web.
- **Interfaz web:** HTML, CSS y JavaScript, ubicada en `src/main/resources/static`.
- **Aplicación Android:** proyecto nativo de Android con Capacitor, ubicado en `android-app/`. Sus archivos web empaquetados están en `android-app/www/`.
- **Persistencia:** H2 en archivo local, configurada por defecto en `data/fyb-db.mv.db`. También se conservan archivos de datos `.txt` utilizados por los servicios actuales.

## Requisitos

### Para ejecutar el backend y la web

- JDK 17.
- Maven 3.9 o compatible.
- Puerto 8080 disponible (o configurar otro mediante `PORT`).

### Para generar la aplicación Android

- Node.js y npm.
- JDK 17.
- Android Studio y Android SDK (incluidas las herramientas de compilación).
- Un dispositivo Android o emulador para probar la aplicación.

## Clonar el repositorio

```bash
git clone https://github.com/jgomez675/FYB.git
cd FYB
```

Si se trabaja desde un fork, sustituye la URL por la dirección de ese fork.

## Ejecutar el backend y la aplicación web

Desde la carpeta raíz del repositorio:

**Windows (PowerShell):**

```powershell
mvn spring-boot:run
```

También se puede ejecutar `run.bat`.

**Linux / macOS:**

```bash
./run.sh
```

Abre [http://localhost:8080](http://localhost:8080) en el navegador. Para generar el JAR:

```bash
mvn clean package
java -jar target/fyb.jar
```

Ejecuta los comandos desde la raíz del repositorio, ya que la ubicación de `data/` se resuelve respecto al directorio desde el que arranca el servidor.

## Ejecutar FYB en Android

La aplicación Android se conecta al backend que se ejecuta en un computador. Por eso, cada persona que prepare su propio APK debe configurar la dirección IP de su computador antes de compilarlo.

Sigue la guía completa en **[README-ANDROID.md](README-ANDROID.md)**. Para usuarios que solo van a instalar el APK y configurar la IP desde el celular, consulta también **[README-CONFIGURACION-ANDROID.md](README-CONFIGURACION-ANDROID.md)**. En resumen:

1. Conecta el celular y el computador a la misma red local.
2. Configura la IP del computador en `android-app/www/servicios/ApiClient.js`.
3. Inicia el backend en el computador.
4. Sincroniza los recursos de Capacitor y compila el APK.
5. Instala el APK en el celular.

La dirección IP incluida en el repositorio es un valor de ejemplo del entorno de desarrollo original; no funcionará necesariamente en otra red. No publiques una IP privada como si fuera una dirección universal.

## Configuración del backend

Las principales variables de entorno se definen en `src/main/resources/application.properties`:

| Variable | Función | Valor predeterminado |
|---|---|---|
| `PORT` | Puerto HTTP del servidor | `8080` |
| `FYB_DB_URL` | URL JDBC de la base de datos | `jdbc:h2:file:./data/fyb-db;DB_CLOSE_ON_EXIT=FALSE` |
| `FYB_DB_USER` | Usuario de la base de datos | `sa` |
| `FYB_DB_PASSWORD` | Contraseña de la base de datos | Vacía |

El archivo H2 se crea localmente al iniciar el backend. No es necesario instalar un servidor de base de datos adicional para la configuración predeterminada.

Para la configuración y el funcionamiento de la persistencia, consulta [README-BASE-DATOS.md](README-BASE-DATOS.md).

## Estructura del repositorio

```text
FYB/
├── android-app/
│   ├── android/                 Proyecto Android / Gradle
│   ├── www/                     Recursos web empaquetados para Android
│   ├── package.json
│   └── capacitor.config.json
├── data/                        Datos iniciales no sensibles
├── src/main/java/back/           Código Java del backend
├── src/main/resources/
│   ├── application.properties   Configuración de Spring
│   └── static/                  Interfaz web
├── README.md
├── README-ANDROID.md
├── README-BASE-DATOS.md
├── README-HISTORIAL-PUNTOS.md
├── README-RECOMPENSAS.md
├── pom.xml
├── run.bat
└── run.sh
```

## Funcionalidades documentadas

- Gestión de usuarios, perfiles y autenticación.
- Publicación y consulta de objetos perdidos.
- Categorías y puntos por devolución.
- Historial de puntos: [README-HISTORIAL-PUNTOS.md](README-HISTORIAL-PUNTOS.md).
- Canje de recompensas: [README-RECOMPENSAS.md](README-RECOMPENSAS.md).
- Persistencia H2: [README-BASE-DATOS.md](README-BASE-DATOS.md).

## Datos locales y seguridad

Los archivos de usuarios, objetos, puntos, canjes, imágenes cargadas y la base H2 pueden contener información personal o datos de prueba. Se excluyen del control de versiones mediante `.gitignore`.

No subas al repositorio:

- Bases de datos locales (`*.mv.db`, `*.trace.db`, `*.lock.db`).
- Archivos de datos de usuarios, objetos, puntos o canjes.
- Imágenes de objetos cargadas durante las pruebas.
- Contraseñas, tokens, claves o archivos `.env` con credenciales.

Cada integrante debe utilizar sus propios datos locales. La base de datos no se comparte automáticamente entre computadores: cada instancia del backend mantiene su propia persistencia local.

## Notas para desarrollo en equipo

- El backend debe estar iniciado para que la aplicación móvil pueda consultar los servicios.
- El celular debe tener conectividad de red con el computador que ejecuta Spring Boot.
- Si se cambia la IP o el puerto del backend, hay que actualizar la configuración Android y volver a generar el APK.
- La carpeta `android-app/www/` es el contenido web empaquetado de Android. No asumir que se actualiza automáticamente al modificar `src/main/resources/static/`; sincroniza los recursos de Capacitor después de actualizar la versión móvil.
