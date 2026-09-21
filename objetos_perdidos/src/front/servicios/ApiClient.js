/* Todas las llamadas al servidor Java pasan por aquí. */
(function (App) {
  "use strict";

  // Error con el código HTTP (0 = no hubo conexión) para que las pantallas decidan qué hacer.
  class ErrorApi extends Error {
    constructor(mensaje, estado) {
      super(mensaje);
      this.estado = estado;
    }
  }

  class ApiClient {
    // Devuelve { estado, datos }. Solo lanza si no hay conexión con el servidor.
    static async solicitar(metodo, ruta, cuerpo) {
      let respuesta;
      try {
        respuesta = await fetch(ruta, {
          method: metodo,
          credentials: "same-origin",
          headers: cuerpo === undefined ? {} : { "Content-Type": "application/json" },
          body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo),
        });
      } catch (e) {
        throw new ErrorApi("No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.", 0);
      }

      let datos = null;
      try {
        datos = await respuesta.json();
      } catch (e) {
        /* respuesta sin cuerpo JSON */
      }
      return { estado: respuesta.status, datos };
    }

    static mensajeDe(respuesta) {
      return (respuesta.datos && respuesta.datos.mensaje) || "Ocurrió un error inesperado. Inténtalo de nuevo.";
    }
  }

  App.front.api.ErrorApi = ErrorApi;
  App.front.api.ApiClient = ApiClient;
})(window.App);
