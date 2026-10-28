/*
 * Navegación entre pantallas (en Java: stage.setScene). Aquí una "pantalla" es una ruta
 * del hash de la URL. Incluye el aviso de una sola vez ("Objeto publicado...") y el
 * manejo del nombre de la app en el título de la pestaña.
 */
(function (App) {
  "use strict";

  const { h } = App.front.components.dom;

  let aviso = null;

  // Deja un mensaje para mostrarlo en la próxima pantalla.
  function guardarAviso(texto, tipo) {
    aviso = { texto, tipo: tipo || "ok" };
  }

  // Devuelve el aviso pendiente (o null) y lo borra: solo se muestra una vez.
  function tomarAviso() {
    if (!aviso) return null;
    const a = aviso;
    aviso = null;
    return h("p", { class: "flash " + a.tipo, role: "status" }, a.texto);
  }

  // Cambia de pantalla sin dejar la anterior en el historial y avisa al router.
  function irA(hash) {
    history.replaceState(null, "", hash);
    window.dispatchEvent(new Event("hashchange"));
  }

  function ponerTitulo(seccion) {
    document.title = seccion ? seccion + " - " + App.config.name : App.config.name;
  }

  // Rellena todo lo que lleva el nombre de la app en el HTML estático.
  function aplicarMarca() {
    document.querySelectorAll("[data-app-name]").forEach((el) => {
      el.textContent = App.config.name;
    });
    const desc = document.querySelector('meta[name="description"]');
    if (desc) desc.setAttribute("content", App.config.name + ": " + App.config.tagline);
    document.title = App.config.name;
  }

  App.front.components.navigation = { guardarAviso, tomarAviso, irA, ponerTitulo, aplicarMarca };
})(window.App);
