/*
 * Espacio de nombres del front y valores por defecto.
 * El nombre de la app, el eslogan y el dominio de correo los define el servidor
 * (back/config/AppConfig.java) y se cargan al arrancar desde GET /api/config;
 * estos valores solo se usan si el servidor no responde.
 */
window.App = {
  config: {
    name: "FYB",
    tagline: "Encuentra lo que creías perdido",
    emailDomain: "usa.edu.co",

    locale: "es-CO",

    // Lado más largo (px) al que se reduce cada foto antes de subirla.
    maxImageSide: 900,
  },

  front: { api: {}, components: {}, login: {}, register: {}, objetos: {} },
};
