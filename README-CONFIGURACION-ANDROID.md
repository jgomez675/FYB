# Configurar el servidor desde FYB Android

## Para estudiantes y usuarios

No necesitas instalar Android Studio, Node.js, Maven ni descargar el código fuente. El equipo de desarrollo te comparte un APK ya compilado.

### 1. Instala FYB
1. Descarga el archivo APK que te comparta el equipo.
2. Ábrelo desde tu celular Android.
3. Autoriza la instalación desde esa fuente si Android lo solicita.
4. Abre FYB.

### 2. Configura la conexión la primera vez
Al abrir la aplicación por primera vez aparecerá **Configurar servidor**.

1. En el computador que ejecuta el backend, abre PowerShell y ejecuta `ipconfig`.
2. Busca la dirección IPv4 del adaptador Wi-Fi o Ethernet activo. Ejemplo: `192.168.1.105`.
3. En FYB escribe la dirección completa con el puerto: `http://192.168.1.105:8080`.
4. Pulsa **Probar conexión y guardar**.
5. Cuando aparezca el mensaje de conexión correcta, vuelve al inicio de sesión.

La dirección queda guardada en el celular. No tendrás que configurarla cada vez que abras la aplicación.

### 3. Si cambia la IP o el computador servidor
Entra a **Configurar servidor** desde el enlace de la pantalla de inicio de sesión o desde el menú de FYB. Escribe la nueva dirección y vuelve a pulsar **Probar conexión y guardar**.

### 4. Requisitos para conectarse
- El backend FYB debe estar iniciado en el computador.
- El celular y el computador deben estar conectados a la misma red Wi-Fi (o a redes que permitan comunicación entre dispositivos).
- El firewall del computador debe permitir conexiones entrantes por TCP 8080.
- No uses `localhost` ni `127.0.0.1` en el celular: debes usar la IPv4 del computador.

## Para el integrante que prepara y distribuye el APK

Solo una persona del equipo necesita las herramientas de desarrollo (JDK 17, Node.js/npm y Android SDK). Desde la raíz del repositorio ejecuta:

```powershell
cd android-app
npm install
npx cap sync android
cd android
.\gradlew.bat assembleDebug
```

El APK de depuración se genera en:

```text
android-app/android/app/build/outputs/apk/debug/app-debug.apk
```

Comparte ese mismo APK con los demás integrantes. Cada usuario configura el servidor desde la aplicación; no debe recompilarlo.

> Esta modalidad está pensada para demostraciones y pruebas en una red local. Para acceder desde distintas redes o por Internet, el backend debe desplegarse en un servidor accesible mediante una dirección estable y HTTPS.
