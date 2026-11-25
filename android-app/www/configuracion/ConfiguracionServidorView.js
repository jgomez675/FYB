/* Configuración local del servidor para instalaciones Android de FYB. */
(function (App) {
  "use strict";

  const { h, campo, mostrarMensaje, ocupado } = App.front.components.dom;
  const { tarjetaAcceso, logo } = App.front.components.layout;
  const { ponerTitulo, irA } = App.front.components.navigation;
  const { ApiClient } = App.front.api;

  class ConfiguracionServidorView {
    async mostrar() {
      ponerTitulo("Configurar servidor");
      const actual = ApiClient.obtenerBaseUrl();
      const direccion = campo({
        id: "servidor",
        etiqueta: "Dirección del servidor",
        type: "url",
        inputmode: "url",
        autocomplete: "url",
        placeholder: "http://192.168.1.100:8080",
        value: actual
      });
      const mensaje = h("p", { class: "msg", role: "status" });
      const probar = h("button", { class: "btn btn-primary", type: "button" }, "Probar conexión y guardar");
      const volver = h("a", { class: "btn btn-secondary", href: "#/login" }, "Volver al inicio de sesión");
      const ayuda = h("div", { class: "server-help" },
        h("p", {}, "Escribe la dirección del computador donde está ejecutándose Spring Boot."),
        h("p", {}, "El celular y el computador deben estar conectados a la misma red Wi-Fi. Ejemplo: http://192.168.1.105:8080.")
      );

      probar.addEventListener("click", async () => {
        let base;
        try {
          base = ApiClient.guardarBaseUrl(direccion.control.value);
        } catch (e) {
          return mostrarMensaje(mensaje, e.message || "Escribe una dirección válida.", "error");
        }
        ocupado(probar, true);
        mostrarMensaje(mensaje, "Comprobando conexión...", "");
        try {
          await ApiClient.probarConexion(base);
          mostrarMensaje(mensaje, "Conexión correcta. La dirección quedó guardada en este celular.", "ok");
          setTimeout(() => irA("#/login"), 900);
        } catch (e) {
          mostrarMensaje(mensaje, e.message, "error");
        } finally {
          ocupado(probar, false);
        }
      });

      return tarjetaAcceso(
        logo("h1"),
        h("h2", { class: "auth-title" }, "Configurar servidor"),
        h("p", { class: "tagline" }, "Conecta FYB con el computador que ejecuta el backend."),
        ayuda,
        h("form", { class: "form" }, direccion.wrap, probar, mensaje),
        volver
      );
    }
  }

  App.front.configuracion = App.front.configuracion || {};
  App.front.configuracion.ConfiguracionServidorView = ConfiguracionServidorView;
})(window.App);
