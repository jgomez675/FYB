/*
 * Piezas de pantalla que se repiten: el logo, la tarjeta de acceso, la barra superior
 * con la sesión y la foto de un objeto. (En Java se repetían copiadas en cada vista.)
 */
(function (App) {
  "use strict";

  const { h, iconoMarca } = App.front.components.dom;
  const { irA } = App.front.components.navigation;
  const { AuthApi } = App.front.api;

  // Logo con el nombre de la app. `tag` es "h1" en el login y "div" donde el título es otro.
  function logo(tag) {
    return h("div", { class: "wordmark" }, iconoMarca(), h(tag, { class: "wm" }, App.config.name));
  }

  // Tarjeta blanca centrada sobre el fondo azul (login y registro).
  function tarjetaAcceso(...hijos) {
    return h("div", { class: "auth" }, h("section", { class: "card auth-card" }, ...hijos));
  }

  // Pantalla con sesión iniciada: barra superior + contenido.
  function pantalla(usuario, ...contenido) {
    const cerrarSesion = async () => {
      await AuthApi.cerrarSesion();
      irA("#/login");
    };

    const barra = h(
      "header",
      { class: "topbar" },
      h(
        "div",
        { class: "topbar-inner" },
        h("a", { class: "brand-link", href: "#/objetos", "aria-label": App.config.name + ", ir al inicio" }, iconoMarca(), h("span", {}, App.config.name)),
        h(
          "div",
          { class: "topbar-user" },
          h("span", { class: "user-name", text: usuario.nombre }),
          h("button", { class: "btn btn-ghost", type: "button", onclick: cerrarSesion }, "Cerrar sesión")
        )
      )
    );

    return h("div", {}, barra, h("main", { class: "container" }, ...contenido));
  }

  function imagenObjeto(objeto, clase) {
    const valida = typeof objeto.imagenUrl === "string" && objeto.imagenUrl.startsWith("/api/imagenes/");
    return h(
      "div",
      { class: clase },
      valida ? h("img", { src: objeto.imagenUrl, alt: "Foto de " + objeto.nombre, loading: "lazy" }) : h("span", { class: "no-img" }, "Sin imagen")
    );
  }

  App.front.components.layout = { logo, tarjetaAcceso, pantalla, imagenObjeto };
})(window.App);
