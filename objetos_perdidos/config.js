/*
 * Configuración central y espacio de nombres de la app.
 * Cambia `name` y todo el sitio se actualiza solo: título de la pestaña, cabecera,
 * pantallas de acceso, textos y claves de almacenamiento.
 *
 * La estructura de carpetas replica la del proyecto Java (src/back y src/front),
 * y `App.back.*` / `App.front.*` replica sus paquetes.
 */
window.App = {
  config: {
    name: "FYB",
    tagline: "Encuentra lo que creías perdido",

    // Dominio permitido para registrarse (sin la @).
    emailDomain: "usa.edu.co",

    // Prefijo de las claves de localStorage. Cámbialo si publicas dos versiones
    // en el mismo dominio para que no compartan datos.
    storagePrefix: "fyb",

    locale: "es-CO",

    // Lado más largo (px) al que se reduce cada foto antes de guardarla.
    maxImageSide: 900,
  },

  back: { model: {}, service: {}, validation: {} },
  front: { components: {}, login: {}, register: {}, objetos: {} },
};
