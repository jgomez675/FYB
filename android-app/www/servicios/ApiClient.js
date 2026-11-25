/* Cliente API de FYB para web y Android */
(function (App) {
  "use strict";

  const CLAVE_SERVIDOR = "fyb.apiBaseUrl";
  const SERVIDOR_PREDETERMINADO = "";

  class ErrorApi extends Error {
    constructor(mensaje, estado) {
      super(mensaje);
      this.name = "ErrorApi";
      this.estado = estado;
    }
  }

  const esAndroid = !!(
    window.Capacitor &&
    window.Capacitor.getPlatform() === "android"
  );

  function obtenerBaseUrl() {
    if (!esAndroid) return "";
    return (localStorage.getItem(CLAVE_SERVIDOR) || SERVIDOR_PREDETERMINADO).replace(/\/+$/, "");
  }

  function tieneConfiguracion() {
    return !esAndroid || !!localStorage.getItem(CLAVE_SERVIDOR);
  }

  function normalizarBaseUrl(valor) {
    const texto = String(valor || "").trim().replace(/\/+$/, "");
    const url = new URL(texto);
    if (!["http:", "https:"].includes(url.protocol)) {
      throw new Error("La dirección debe comenzar con http:// o https://.");
    }
    if (url.pathname !== "/" || url.search || url.hash) {
      throw new Error("Escribe solo la dirección y el puerto, sin rutas adicionales.");
    }
    return texto;
  }

  class ApiClient {
    static obtenerBaseUrl() {
      return obtenerBaseUrl();
    }

    static tieneConfiguracion() {
      return tieneConfiguracion();
    }

    static guardarBaseUrl(valor) {
      const base = normalizarBaseUrl(valor);
      localStorage.setItem(CLAVE_SERVIDOR, base);
      return base;
    }

    static async probarConexion(valor) {
      const base = normalizarBaseUrl(valor);
      let respuesta;
      try {
        respuesta = await fetch(base + "/api/config", {
          method: "GET",
          credentials: "include",
          headers: { "Accept": "application/json" }
        });
      } catch (e) {
        throw new ErrorApi("No se pudo conectar. Verifica la IP, el puerto, la red Wi-Fi y que Spring Boot esté iniciado.", 0);
      }
      if (!respuesta.ok) {
        throw new ErrorApi("El servidor respondió con HTTP " + respuesta.status + ". Verifica que la dirección corresponda al backend FYB.", respuesta.status);
      }
      let datos = null;
      try { datos = await respuesta.json(); } catch (e) { /* respuesta no JSON */ }
      if (!datos) {
        throw new ErrorApi("El servidor respondió, pero no devolvió la configuración esperada de FYB.", respuesta.status);
      }
      return datos;
    }

    static async solicitar(metodo, ruta, cuerpo) {
      const url = ruta.startsWith("http") ? ruta : obtenerBaseUrl() + ruta;
      let respuesta;

      try {
        console.log("[FYB] Solicitud:", metodo, url);

        respuesta = await fetch(url, {
          method: metodo,
          credentials: "include",
          headers: cuerpo === undefined ? {} : {
            "Content-Type": "application/json"
          },
          body: cuerpo === undefined ? undefined : JSON.stringify(cuerpo)
        });
      } catch (e) {
        console.error("[FYB] Error de conexión:", e);
        throw new ErrorApi("No se pudo conectar con el servidor. Revisa la configuración de conexión.", 0);
      }

      let datos = null;
      try {
        datos = await respuesta.json();
      } catch (e) {
        console.error("[FYB] Respuesta JSON inválida:", e);
      }

      console.log("[FYB] Respuesta:", respuesta.status, datos);
      return { estado: respuesta.status, datos };
    }

    static mensajeDe(respuesta) {
      return (respuesta.datos && respuesta.datos.mensaje) ||
        "Ocurrió un error inesperado. Inténtalo de nuevo.";
    }
  }

  App.front.api.ErrorApi = ErrorApi;
  App.front.api.ApiClient = ApiClient;
})(window.App);
